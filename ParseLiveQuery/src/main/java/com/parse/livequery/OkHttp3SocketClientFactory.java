package com.parse.livequery;

import android.util.Log;

import java.net.URI;
import java.util.Locale;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okio.ByteString;

public class OkHttp3SocketClientFactory implements WebSocketClientFactory {

    OkHttpClient mClient;

    public OkHttp3SocketClientFactory(OkHttpClient client) {
        mClient = client;
    }

    public OkHttp3SocketClientFactory() {
        // Optimized for LiveQuery WebSocket: 0 read timeout (long-lived), 15s ping, tuned pool
        mClient = new OkHttpClient.Builder()
                .connectTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
                .readTimeout(0, java.util.concurrent.TimeUnit.MILLISECONDS)
                .writeTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
                .pingInterval(15, java.util.concurrent.TimeUnit.SECONDS)
                .retryOnConnectionFailure(true)
                .connectionPool(new okhttp3.ConnectionPool(5, 5, java.util.concurrent.TimeUnit.MINUTES))
                .build();
    }

    @Override
    public WebSocketClient createInstance(WebSocketClient.WebSocketClientCallback webSocketClientCallback, URI hostUrl) {
        return new OkHttp3WebSocketClient(mClient, webSocketClientCallback, hostUrl);
    }

    static class OkHttp3WebSocketClient implements WebSocketClient {

        private static final String LOG_TAG = "OkHttpWebSocketClient";

        private final WebSocketClientCallback webSocketClientCallback;
        private WebSocket webSocket;
        private volatile State state = State.NONE;
        private final OkHttpClient client;
        private final String url;
        private final int STATUS_CODE = 1000;
        private final String CLOSING_MSG = "User invoked close";

        private final WebSocketListener handler = new WebSocketListener() {
            @Override
            public void onOpen(WebSocket webSocket, Response response) {
                setState(State.CONNECTED);
                webSocketClientCallback.onOpen();
            }

            @Override
            public void onMessage(WebSocket webSocket, String text) {
                webSocketClientCallback.onMessage(text);
            }

            @Override
            public void onMessage(WebSocket webSocket, ByteString bytes) {
                Log.w(LOG_TAG, String.format(Locale.US,
                        "Socket got into inconsistent state and received %s instead.",
                        bytes.toString()));
            }

            @Override
            public void onClosed(WebSocket webSocket, int code, String reason) {
                setState(State.DISCONNECTED);
                webSocketClientCallback.onClose();
            }

            @Override
            public void onFailure(okhttp3.WebSocket webSocket, Throwable t, Response response) {
                setState(State.DISCONNECTED);
                webSocketClientCallback.onError(t);
            }
        };

        private OkHttp3WebSocketClient(OkHttpClient okHttpClient,
                                       WebSocketClientCallback webSocketClientCallback, URI hostUrl) {
            client = okHttpClient;
            this.webSocketClientCallback = webSocketClientCallback;
            url = hostUrl.toString();
        }

        @Override
        public synchronized void open() {
            if (State.NONE == state) {
                // OkHttp3 connects as soon as the socket is created so do it here.
                Request request = new Request.Builder()
                        .url(url)
                        .build();

                webSocket = client.newWebSocket(request, handler);
                setState(State.CONNECTING);
            }
        }

        @Override
        public synchronized void close() {
            if (State.NONE != state) {
                setState(State.DISCONNECTING);
                webSocket.close(STATUS_CODE, CLOSING_MSG);
            }
        }

        @Override
        public void send(String message) {
            // Fix lock order: snapshot state under monitor, invoke callbacks outside it.
            // Calling onError while holding the socket monitor deadlocks against
            // ParseLiveQueryClientImpl (socket -> client vs client -> socket).
            final WebSocketClientCallback callback;
            final State observed;
            final WebSocket socket;
            synchronized (this) {
                observed = state;
                socket = webSocket;
                callback = webSocketClientCallback;
            }
            if (observed == State.CONNECTED && socket != null) {
                try {
                    // Fix Step 4: Check boolean return — false means queue full or socket closed
                    boolean sent = socket.send(message);
                    if (!sent) {
                        Log.w(LOG_TAG, "WebSocket.send() returned false — queue full or socket closed");
                        java.io.IOException failure =
                                new java.io.IOException("WebSocket queue full or socket closed");
                        callback.onError(failure);
                        throw new IllegalStateException(failure);
                    }
                } catch (RuntimeException e) {
                    throw e;
                } catch (Exception e) {
                    Log.e(LOG_TAG, "Failed to send WebSocket message", e);
                    callback.onError(e);
                    throw new IllegalStateException(e);
                }
            } else {
                Log.w(LOG_TAG, "Cannot send message: state=" + observed + ", webSocket=" + (socket != null ? "valid" : "null"));
                java.io.IOException failure = new java.io.IOException(
                        "WebSocket not connected: state=" + observed);
                callback.onError(failure);
                throw new IllegalStateException(failure);
            }
        }

        @Override
        public State getState() {
            return state;
        }

        private void setState(State newState) {
            synchronized (this) {
                this.state = newState;
            }
            this.webSocketClientCallback.stateChanged();
        }
    }

}
