// Sibling Docs: ./README.md | Decisions: ./DECISIONS.md
package com.parse.livequery;

import android.util.Log;

import com.parse.PLog;
import com.parse.Parse;
import com.parse.ParseDecoder;
import com.parse.ParseObject;
import com.parse.ParsePlugins;
import com.parse.ParseQuery;
import com.parse.ParseUser;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executor;

import com.parse.boltsinternal.Continuation;
import com.parse.boltsinternal.Task;
import okhttp3.OkHttpClient;

class ParseLiveQueryClientImpl implements ParseLiveQueryClient {

    private static final String LOG_TAG = "ParseLiveQueryClient";

    private final Executor taskExecutor;
    private final String applicationId;
    private final String clientKey;
    private final ConcurrentHashMap<Integer, Subscription<? extends ParseObject>> subscriptions = new ConcurrentHashMap<>();
    private final URI uri;
    private final WebSocketClientFactory webSocketClientFactory;
    private final WebSocketClient.WebSocketClientCallback webSocketClientCallback;
    private final ParseLiveQueryClock clock;

    private final List<ParseLiveQueryClientCallbacks> mCallbacks = new CopyOnWriteArrayList<>();

    private volatile WebSocketClient webSocketClient;
    private volatile int requestIdCount = 1;
    private volatile boolean userInitiatedDisconnect = false;
    private volatile boolean hasReceivedConnected = false;

    /* package */ ParseLiveQueryClientImpl() {
        this(getDefaultUri());
    }

    /* package */ ParseLiveQueryClientImpl(URI uri) {
        this(uri, new OkHttp3SocketClientFactory(new OkHttpClient()), Task.BACKGROUND_EXECUTOR);
    }

    /* package */ ParseLiveQueryClientImpl(URI uri, WebSocketClientFactory webSocketClientFactory) {
        this(uri, webSocketClientFactory, Task.BACKGROUND_EXECUTOR);
    }

    /* package */ ParseLiveQueryClientImpl(WebSocketClientFactory webSocketClientFactory) {
        this(getDefaultUri(), webSocketClientFactory, Task.BACKGROUND_EXECUTOR);
    }

    /* package */ ParseLiveQueryClientImpl(URI uri, WebSocketClientFactory webSocketClientFactory, Executor taskExecutor) {
        this(uri, webSocketClientFactory, taskExecutor, ParseLiveQueryClock.SYSTEM);
    }

    /* package */ ParseLiveQueryClientImpl(URI uri, WebSocketClientFactory webSocketClientFactory, Executor taskExecutor, ParseLiveQueryClock clock) {
        Parse.checkInit();
        this.uri = uri;
        this.applicationId = ParsePlugins.get().applicationId();
        this.clientKey = ParsePlugins.get().clientKey();
        this.webSocketClientFactory = webSocketClientFactory;
        this.taskExecutor = taskExecutor;
        this.clock = clock;
        this.webSocketClientCallback = callbackForGeneration(connectionGeneration);
    }

    private static URI getDefaultUri() {
        String url = ParsePlugins.get().server();
        if (url.contains("https")) {
            url = url.replaceFirst("https", "wss");
        } else {
            url = url.replaceFirst("http", "ws");
        }
        try {
            return new URI(url);
        } catch (URISyntaxException e) {
            e.printStackTrace();
            throw new RuntimeException(e.getMessage());
        }
    }

    @Override
    public <T extends ParseObject> SubscriptionHandling<T> subscribe(ParseQuery<T> query) {
        int requestId = requestIdGenerator();
        Subscription<T> subscription = new Subscription<>(requestId, query);
        subscriptions.put(requestId, subscription);

        Log.d(LOG_TAG, "subscribe() called for query: " + query.getClassName() + " (requestId=" + requestId + ")");

        // Fix Issue #70: Only send immediately if WebSocket is connected AND we received op=connected
        // This prevents duplicate subscriptions when subscribing before op=connected is received
        if (isConnected() && hasReceivedConnected) {
            Log.d(LOG_TAG, "WebSocket connected and ready, sending subscription immediately");
            sendSubscription(subscription);
        } else if (userInitiatedDisconnect) {
            Log.w(LOG_TAG, "Warning: The client was explicitly disconnected! You must explicitly call .reconnect() in order to process your subscriptions.");
        } else {
            Log.d(LOG_TAG, "WebSocket not ready yet (state=" + getWebSocketState() + ", hasReceivedConnected=" + hasReceivedConnected + "), queuing subscription");
            connectIfNeeded();
        }

        return subscription;
    }

    public synchronized void connectIfNeeded() {
        switch (getWebSocketState()) {
            case CONNECTED:
                // nothing to do
                break;
            case CONNECTING:
                // just wait for it to finish connecting
                break;

            case NONE:
            case DISCONNECTING:
            case DISCONNECTED:
                reconnect();
                break;

            default:

                break;
        }
    }

    @Override
    public <T extends ParseObject> void unsubscribe(final ParseQuery<T> query) {
        if (query != null) {
            // Fix Issue #117: Create copy to avoid ConcurrentModificationException
            for (Subscription<? extends ParseObject> subscription : new java.util.ArrayList<>(subscriptions.values())) {
                if (query.equals(subscription.getQuery())) {
                    sendUnsubscription(subscription);
                }
            }
        }
    }

    @Override
    public <T extends ParseObject> void unsubscribe(final ParseQuery<T> query, final SubscriptionHandling<T> subscriptionHandling) {
        if (query != null && subscriptionHandling != null) {
            // Fix Issue #117: Create copy to avoid ConcurrentModificationException
            for (Subscription<? extends ParseObject> subscription : new java.util.ArrayList<>(subscriptions.values())) {
                if (query.equals(subscription.getQuery()) && subscriptionHandling.equals(subscription)) {
                    sendUnsubscription(subscription);
                }
            }
        }
    }

    @Override
    public synchronized void reconnect() {
        // Fix race condition: only reconnect if connection is not already active
        if (isConnected()) {
            Log.d(LOG_TAG, "reconnect() skipped: already connected");
            return;
        }
        cancelPendingReconnect();
        if (webSocketClient != null) {
            webSocketClient.close();
        }

        userInitiatedDisconnect = false;
        hasReceivedConnected = false;
        connectionGeneration++;
        webSocketClient = webSocketClientFactory.createInstance(
                callbackForGeneration(connectionGeneration), uri);
        webSocketClient.open();
    }

    @Override
    public synchronized void disconnect() {
        cancelPendingReconnect();
        // NOTE: no generation bump here — the current socket's own onClose
        // must still be delivered (expected-disconnect notification); only
        // reconnect() invalidates predecessors.
        // Fix Issue #82: Safely close even if connection was never opened
        if (webSocketClient != null) {
            try {
                webSocketClient.close();
            } catch (Exception e) {
                Log.w(LOG_TAG, "Error closing WebSocket (may have never been opened)", e);
            }
            webSocketClient = null;
        }

        userInitiatedDisconnect = true;
        hasReceivedConnected = false;
    }

    @Override
    public void registerListener(ParseLiveQueryClientCallbacks listener) {
        mCallbacks.add(listener);
    }

    @Override
    public void unregisterListener(ParseLiveQueryClientCallbacks listener) {
        mCallbacks.remove(listener);
    }

    // Private methods

    private synchronized int requestIdGenerator() {
        return requestIdCount++;
    }

    private WebSocketClient.State getWebSocketState() {
        WebSocketClient.State state = webSocketClient == null ? null : webSocketClient.getState();
        return state == null ? WebSocketClient.State.NONE : state;
    }

    private boolean isConnected() {
        return hasReceivedConnected && inAnyState(WebSocketClient.State.CONNECTED);
    }

    private boolean inAnyState(WebSocketClient.State... states) {
        return Arrays.asList(states).contains(getWebSocketState());
    }

    /** Masks sessionToken for logs; the wire payload is unchanged. */
    private static String redactForLog(JSONObject json) {
        try {
            if (json.has("sessionToken")) {
                JSONObject copy = new JSONObject(json.toString());
                copy.put("sessionToken", "***");
                return copy.toString();
            }
        } catch (JSONException ignored) {
        }
        return json.toString();
    }

    private Task<Void> handleOperationAsync(final String message) {
        return Task.call(new Callable<Void>() {
            public Void call() throws Exception {
                parseMessage(message);
                return null;
            }
        }, taskExecutor);
    }

    /**
     * Enqueues a {@link ClientOperation} for transmission over the WebSocket.
     *
     * <p><b>Thread-safety (V1 fix):</b> The field {@code webSocketClient} can be nulled by
     * {@link #disconnect()} on a different thread at any moment. To avoid a TOCTOU race
     * where we null-check the field then read it again for {@code send()}, we capture the
     * reference into a {@code final} local variable exactly once and branch on that local
     * for the entire call body. The local may point to a closing socket (send throws) but
     * will never be {@code null} after the initial capture.</p>
     */
    private Task<Void> sendOperationAsync(final ClientOperation clientOperation) {
        return Task.call(new Callable<Void>() {
            public Void call() throws Exception {
                // V1 FIX: Snapshot the volatile field exactly once to prevent TOCTOU race.
                final WebSocketClient clientSnapshot = webSocketClient;
                if (clientSnapshot == null) {
                    Log.w(LOG_TAG, "sendOperationAsync: WebSocket is null, cannot send operation");
                    return null;
                }
                JSONObject jsonEncoded = clientOperation.getJSONObjectRepresentation();
                String jsonString = jsonEncoded.toString();
                if (Parse.getLogLevel() <= Parse.LOG_LEVEL_DEBUG) {
                    Log.d(LOG_TAG, "Sending over websocket: " + redactForLog(jsonEncoded));
                }
                try {
                    clientSnapshot.send(jsonString);
                } catch (Exception e) {
                    Log.e(LOG_TAG, "Failed to send operation over WebSocket", e);
                    throw e;
                }
                return null;
            }
        }, taskExecutor);
    }

    private void parseMessage(String message) throws LiveQueryException {
        try {
            JSONObject jsonObject = new JSONObject(message);
            String rawOperation = jsonObject.getString("op");

            switch (rawOperation) {
                case "connected":
                    synchronized (ParseLiveQueryClientImpl.this) {
                        hasReceivedConnected = true;
                        reconnectAttempts = 0; // F4: Reset only after successful LiveQuery handshake
                    }
                    long wsDuration = clock.currentTimeMillis() - wsConnectStartMs;
                    Log.v(LOG_TAG, "[WS-TIMING] Connected in " + wsDuration + "ms, sending pending subscription");
                    dispatchConnected();
                    // Fix Issue #117: Create copy to avoid ConcurrentModificationException
                    for (Subscription<? extends ParseObject> subscription : new java.util.ArrayList<>(subscriptions.values())) {
                        sendSubscription(subscription);
                    }
                    break;
                case "redirect":
                    String url = jsonObject.getString("url");
                    // TODO: Handle redirect.
                    Log.d(LOG_TAG, "Redirect is not yet handled");
                    break;
                case "subscribed":
                    handleSubscribedEvent(jsonObject);
                    break;
                case "unsubscribed":
                    handleUnsubscribedEvent(jsonObject);
                    break;
                case "enter":
                    handleObjectEvent(Subscription.Event.ENTER, jsonObject);
                    break;
                case "leave":
                    handleObjectEvent(Subscription.Event.LEAVE, jsonObject);
                    break;
                case "update":
                    handleObjectEvent(Subscription.Event.UPDATE, jsonObject);
                    break;
                case "create":
                    handleObjectEvent(Subscription.Event.CREATE, jsonObject);
                    break;
                case "delete":
                    handleObjectEvent(Subscription.Event.DELETE, jsonObject);
                    break;
                case "error":
                    handleErrorEvent(jsonObject);
                    break;
                default:
                    throw new LiveQueryException.InvalidResponseException(message);
            }
        } catch (JSONException e) {
            throw new LiveQueryException.InvalidResponseException(message);
        }
    }

    private void dispatchConnected() {
        for (ParseLiveQueryClientCallbacks callback : mCallbacks) {
            try {
                callback.onLiveQueryClientConnected(this);
            } catch (Exception e) {
                Log.e(LOG_TAG, "Listener threw in onLiveQueryClientConnected", e);
            }
        }
    }

    private void dispatchDisconnected() {
        for (ParseLiveQueryClientCallbacks callback : mCallbacks) {
            try {
                callback.onLiveQueryClientDisconnected(this, userInitiatedDisconnect);
            } catch (Exception e) {
                Log.e(LOG_TAG, "Listener threw in onLiveQueryClientDisconnected", e);
            }
        }
    }


    private void dispatchServerError(LiveQueryException exc) {
        for (ParseLiveQueryClientCallbacks callback : mCallbacks) {
            try {
                callback.onLiveQueryError(this, exc);
            } catch (Exception e) {
                Log.e(LOG_TAG, "Listener threw in onLiveQueryError", e);
            }
        }
    }

    private void dispatchSocketError(Throwable reason) {
        for (ParseLiveQueryClientCallbacks callback : mCallbacks) {
            try {
                callback.onSocketError(this, reason);
            } catch (Exception e) {
                Log.e(LOG_TAG, "Listener threw in onSocketError", e);
            }
        }

        dispatchDisconnected();
    }

    private <T extends ParseObject> void handleSubscribedEvent(JSONObject jsonObject) throws JSONException {
        final int requestId = jsonObject.getInt("requestId");
        final Subscription<T> subscription = subscriptionForRequestId(requestId);
        if (subscription != null) {
            subscription.didSubscribe(subscription.getQuery());
        }
    }

    private <T extends ParseObject> void handleUnsubscribedEvent(JSONObject jsonObject) throws JSONException {
        final int requestId = jsonObject.getInt("requestId");
        final Subscription<T> subscription = subscriptionForRequestId(requestId);
        if (subscription != null) {
            subscription.didUnsubscribe(subscription.getQuery());
            subscriptions.remove(requestId);
        }
    }

    private <T extends ParseObject> void handleObjectEvent(Subscription.Event event, JSONObject jsonObject) throws JSONException {
        final int requestId = jsonObject.getInt("requestId");
        final Subscription<T> subscription = subscriptionForRequestId(requestId);
        if (subscription != null) {
            T object = ParseObject.fromJSON(jsonObject.getJSONObject("object"), subscription.getQueryState().className(), ParseDecoder.get(), subscription.getQueryState().selectedKeys());
            subscription.didReceive(event, subscription.getQuery(), object);
        }
    }

    private <T extends ParseObject> void handleErrorEvent(JSONObject jsonObject) throws JSONException {
        int requestId = jsonObject.optInt("requestId", -1);
        int code = jsonObject.optInt("code", -1);
        String error = jsonObject.optString("error", "");
        boolean reconnect = jsonObject.optBoolean("reconnect", false);
        LiveQueryException exc = new LiveQueryException.ServerReportedException(code, error, reconnect);
        if (requestId < 0) {
            Log.w(LOG_TAG, "Error event without requestId: code=" + code + " error=" + error);
            dispatchServerError(exc);
            if (reconnect && !userInitiatedDisconnect) {
                scheduleReconnect();
            }
            return;
        }
        final Subscription<T> subscription = subscriptionForRequestId(requestId);

        if (subscription != null) {
            subscription.didEncounter(exc, subscription.getQuery());
        }

        dispatchServerError(exc);

        if (reconnect && !userInitiatedDisconnect) {
            scheduleReconnect();
        }
    }

    private <T extends ParseObject> Subscription<T> subscriptionForRequestId(int requestId) {
        //noinspection unchecked
        return (Subscription<T>) subscriptions.get(requestId);
    }

    private <T extends ParseObject> void sendSubscription(final Subscription<T> subscription) {
        ParseUser.getCurrentSessionTokenAsync().continueWith(new Continuation<String, Void>() {
            @Override
            public Void then(Task<String> task) throws Exception {
                if (task.getError() != null) {
                    Exception tokenError = task.getError();
                    RuntimeException cause = tokenError instanceof RuntimeException
                            ? (RuntimeException) tokenError
                            : new RuntimeException(tokenError);
                    subscription.didEncounter(new LiveQueryException.UnknownException(
                            "Error fetching session token", cause),
                            subscription.getQuery());
                    return null;
                }
                String sessionToken = task.getResult();
                SubscribeClientOperation<T> op = new SubscribeClientOperation<>(subscription.getRequestId(), subscription.getQueryState(), sessionToken);

                // dispatch errors
                sendOperationAsync(op).continueWith(new Continuation<Void, Void>() {
                    public Void then(Task<Void> task) {
                        Exception error = task.getError();
                        if (error != null) {
                            RuntimeException cause = error instanceof RuntimeException
                                    ? (RuntimeException) error
                                    : new RuntimeException(error);
                            subscription.didEncounter(new LiveQueryException.UnknownException(
                                    "Error when subscribing", cause), subscription.getQuery());
                        }
                        return null;
                    }
                });
                return null;
            }
        });
    }

    private void sendUnsubscription(Subscription subscription) {
        sendOperationAsync(new UnsubscribeClientOperation(subscription.getRequestId()));
    }

    // Auto-reconnect state (Issue #6)
    // Task 720: Removed MAX_RECONNECT_ATTEMPTS cap — infinite retry with exponential backoff
    private static final int BASE_RECONNECT_DELAY_MS = 1000;
    private static final int MAX_RECONNECT_DELAY_MS = 30000;
    private volatile int reconnectAttempts = 0;
    private volatile boolean isReconnecting = false;
    private volatile Runnable pendingReconnectRunnable;
    private volatile long connectionGeneration = 0;
    // Task 720: WebSocket timing for 10k visibility
    private volatile long wsConnectStartMs = 0;

    private synchronized void cancelPendingReconnect() {
        if (pendingReconnectRunnable != null) {
            clock.removeCallbacks(pendingReconnectRunnable);
            pendingReconnectRunnable = null;
        }
        isReconnecting = false;
    }

    /**
     * Wraps the shared protocol callback so events from a superseded socket
     * (older generation) are dropped instead of driving a new connection.
     */
    private WebSocketClient.WebSocketClientCallback callbackForGeneration(final long generation) {
        final WebSocketClient.WebSocketClientCallback delegate = getWebSocketClientCallback();
        return new WebSocketClient.WebSocketClientCallback() {
            private boolean stale() {
                return generation != connectionGeneration;
            }

            @Override
            public void onOpen() {
                if (!stale()) {
                    delegate.onOpen();
                }
            }

            @Override
            public void onMessage(String message) {
                if (!stale()) {
                    delegate.onMessage(message);
                }
            }

            @Override
            public void onClose() {
                if (!stale()) {
                    delegate.onClose();
                }
            }

            @Override
            public void onError(Throwable exception) {
                if (!stale()) {
                    delegate.onError(exception);
                }
            }

            @Override
            public void stateChanged() {
                delegate.stateChanged();
            }
        };
    }

    private WebSocketClient.WebSocketClientCallback getWebSocketClientCallback() {
        return new WebSocketClient.WebSocketClientCallback() {
            @Override
            public void onOpen() {
                synchronized (ParseLiveQueryClientImpl.this) {
                    cancelPendingReconnect();
                    hasReceivedConnected = false;
                    // F4: reconnectAttempts reset moved to "connected" case in handleOperation
                    // Only reset after successful LiveQuery handshake, not just socket open
                    wsConnectStartMs = clock.currentTimeMillis();
                }
                Log.v(LOG_TAG, "[WS-TIMING] Socket opened at " + wsConnectStartMs);
                ParseUser.getCurrentSessionTokenAsync().continueWithTask(new Continuation<String, Task<Void>>() {
                    @Override
                    public Task<Void> then(Task<String> task) throws Exception {
                        if (task.getError() != null) {
                            return Task.forError(task.getError());
                        }
                        String sessionToken = task.getResult();
                        return sendOperationAsync(new ConnectClientOperation(applicationId, clientKey, sessionToken));
                    }
                }).continueWith(new Continuation<Void, Void>() {
                    public Void then(Task<Void> task) {
                        Exception error = task.getError();
                        if (error != null) {
                            Log.e(LOG_TAG, "Error when connection client", error);
                            RuntimeException cause = error instanceof RuntimeException
                                    ? (RuntimeException) error
                                    : new RuntimeException(error);
                            dispatchServerError(new LiveQueryException.UnknownException(
                                    "Error when connecting client", cause));
                            synchronized (ParseLiveQueryClientImpl.this) {
                                if (!userInitiatedDisconnect) {
                                    scheduleReconnect();
                                }
                            }
                        }
                        return null;
                    }
                });
            }

            @Override
            public void onMessage(String message) {
                Log.v(LOG_TAG, "Socket onMessage " + message);
                handleOperationAsync(message).continueWith(new Continuation<Void, Void>() {
                    public Void then(Task<Void> task) {
                        Exception error = task.getError();
                        if (error != null) {
                            Log.e(LOG_TAG, "Error handling message", error);
                        }
                        return null;
                    }
                });
            }

            @Override
            public void onClose() {
                synchronized (ParseLiveQueryClientImpl.this) {
                    Log.v(LOG_TAG, "Socket onClose");
                    hasReceivedConnected = false;
                    dispatchDisconnected();
                    // Fix Issue #6: Auto-reconnect if not user-initiated
                    if (!userInitiatedDisconnect) {
                        scheduleReconnect();
                    }
                }
            }

            @Override
            public void onError(Throwable exception) {
                synchronized (ParseLiveQueryClientImpl.this) {
                    PLog.e(LOG_TAG, "Socket onError", exception);
                    hasReceivedConnected = false;
                    dispatchSocketError(exception);
                    // Fix Issue #6: Auto-reconnect on error if not user-initiated
                    if (!userInitiatedDisconnect) {
                        scheduleReconnect();
                    }
                }
            }

            @Override
            public void stateChanged() {
                PLog.v(LOG_TAG, "Socket stateChanged");
            }
        };
    }

    // Fix Issue #6: Exponential backoff reconnect
    // Task 720: Removed MAX_RECONNECT_ATTEMPTS cap — infinite retry
    // Task 720: Removed subscriptions.isEmpty() guard — reconnect even if subscriptions were GC'd
    // Task 720: Added detailed logging for every reconnect attempt
    private synchronized void scheduleReconnect() {
        if (isReconnecting || userInitiatedDisconnect) {
            return;
        }
        // Cancel any previously scheduled reconnect to prevent duplicate timers
        cancelPendingReconnect();
        isReconnecting = true;

        // F4: Clamp reconnectAttempts safely to prevent negative arithmetic on overflow
        int clampedAttempts = Math.max(0, Math.min(reconnectAttempts, 30));
        // Use long arithmetic: 1000L * 2^clampedAttempts — safe up to 2^30
        long baseDelay = Math.min(BASE_RECONNECT_DELAY_MS * (long) Math.pow(2, clampedAttempts), MAX_RECONNECT_DELAY_MS);
        // Clock-driven jitter (0-249ms) to avoid retry storms; deterministic under fake clocks.
        long jitter = Math.abs(clock.currentTimeMillis() % 250);
        long delay = Math.min(baseDelay + jitter, MAX_RECONNECT_DELAY_MS);
        reconnectAttempts++;

        Log.d(LOG_TAG, "[RECONNECT] Scheduling reconnect in " + delay + "ms (Attempt " + reconnectAttempts
                + ", subscriptions=" + subscriptions.size() + ")");

        pendingReconnectRunnable = new Runnable() {
            @Override
            public void run() {
                synchronized (ParseLiveQueryClientImpl.this) {
                    isReconnecting = false;
                    pendingReconnectRunnable = null;
                    if (!userInitiatedDisconnect) {
                        Log.d(LOG_TAG, "[RECONNECT] Executing reconnect attempt " + reconnectAttempts);
                        reconnect();
                    }
                }
            }
        };
        clock.postDelayed(pendingReconnectRunnable, delay);
    }
}
