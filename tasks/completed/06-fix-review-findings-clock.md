# Task 06: Fix brain review findings and complete clock work

**File:** `tasks/completed/06-fix-review-findings-clock.md`
**Source:** manager
**Type:** bug
**Status:** closed

## Goal

Fix remaining bugs found by Brain review and complete the clock injection work.

## Manager's Notes

Autopilot round ordered by Manager after the release bundle: rebuild graph, review with Brain, fix bugs, complete the clock part. QA findings from prior rounds are fixed and verified (43 green); this task covers whatever the fresh review still reports.

## Local TODOs

- [x] Initial codebase exploration
- [x] Brain review with fresh graph
- [x] Fix reported bugs and complete clock work
- [x] Verify functionality

## Acceptance Criteria

- [x] Brain review reports no blocking findings
- [x] Full gate green: unit tests plus coverage plus spotless
- [x] No direct clock reads remain in main sources

## Verification Evidence

- **Test command:** rtk test ./gradlew --no-daemon clean :ParseLiveQuery:testDebugUnitTest :ParseLiveQuery:createDebugUnitTestCoverageReport
- **Expected result:** BUILD SUCCESSFUL, all tests green, spotless green
- **Actual result:** BUILD SUCCESSFUL; tests=43 failures=0 errors=0; spotlessCheck exit 0; coverage report generated
- **Exit code:** 0

> Verification runner rule: `./gradlew --no-daemon clean :ParseLiveQuery:testDebugUnitTest :ParseLiveQuery:createDebugUnitTestCoverageReport` is the complete underlying test command. The first verification run MUST use the `rtk test` prefix; record the exact prefixed command above. A raw rerun is allowed only after a failed RTK run for detailed diagnostics.

## Definition of Done

The task is NOT done unless ALL of the following are true (unconditional, applies to every source type):

- [x] Build/Test/Lint pass with exit code 0
- [x] `lint_task_file` passes on the active task file
- [x] `CHANGELOG.md` updated via Parse-Then-Append
- [x] `verification-before-completion` applied and evidence recorded

## Risk & Rollback

- **Risk:** Small scoped fixes; full suite guards regressions.
- **Rollback plan:** Revert the task commit.

---

## Execution Log & Reasoning

Autopilot locked 2026-10-10 for this fix round. Graph rebuilt: 434 nodes, 758 links.

Brain QA_REJECTED with F1-F8 plus clock-complete confirmation. All fixed: F1 dropped the userInitiatedDisconnect reset in dispatchSocketError; F2 mCallbacks to CopyOnWriteArrayList; F3 callback monitors unified on the Impl instance; F4 onFailure sets DISCONNECTED first; F5 send failures fault the Task instead of vanishing; F6 token-fetch failures route to didEncounter plus dispatchServerError plus reconnect; F7 subscription callback lists to CopyOnWriteArrayList; F8 error event uses opt getters with early return. Re-verified: rtk full gate exit 0, 43 tests 0 failures, spotless exit 0.

Dep upgrade round (Brain plan S1-S5, verified stepwise): jsonassert 1.5.3, OkHttp 5.5.0, Mockito 5.24.0, Robolectric 4.17 with @Config sdk 28, AGP 9.4.1. Kept parse 4.4.0 and JUnit 4.13.2 (both latest/final). Final gate: rtk exit 0, 43 tests 0 failures, spotless exit 0.

Round 2 duo review (F1-F10, all fixed): synchronized handshake writes; checked send errors always reported; server reconnect flag honored; transport send fails loudly with rethrow; volatile reconnect state cleared under monitor; generation counter drops stale-socket callbacks (reconnect only — disconnect keeps current-socket delivery); per-listener guards on all dispatch paths; connectIfNeeded synchronized; clock drives postDelayed/removeCallbacks with deterministic and stale-callback tests. Final gate: rtk exit 0, 45 tests 0 failures, spotless exit 0.

## Factual Git Diff

<!-- BEGIN_GIT_DIFF -->
```diff
diff --git a/CHANGELOG.md b/CHANGELOG.md
index 09cdd38..27b6569 100644
--- a/CHANGELOG.md
+++ b/CHANGELOG.md
@@ -1,6 +1,9 @@
 ## Changelog
 
 ### master
+- Upgrade deps: OkHttp 5.5.0, Robolectric 4.17, Mockito 5.24.0, jsonassert 1.5.3, AGP 9.4.1; Robolectric configs moved to sdk 28
+- Harden reconnect lifecycle: generation-guarded callbacks, clock-driven scheduling, loud send failures, guarded listeners
+- Fix Brain review races: unified reconnect monitor, CopyOnWrite callback lists, faulted send tasks, token-failure routing, tolerant error parsing, DISCONNECTED on transport failure
 - Cognitive-lead audit: staging and index-isolation guardrails plus deprecated-section purge rule in AGENTS.md; sibling-docs pointer on clock test
 - Align wrapper with AGP 9.2.1: Gradle 5.6.4 to 9.8.1; CI JDK 11 to 17; add Spotless check with upstream ratchet; pin JitPack JDK 17
 - Inject ParseLiveQueryClock into the reconnect path with a backward-compatible Factory overload plus deterministic clock tests
diff --git a/ParseLiveQuery/build.gradle b/ParseLiveQuery/build.gradle
index ece9360..fe94f71 100644
--- a/ParseLiveQuery/build.gradle
+++ b/ParseLiveQuery/build.gradle
@@ -39,12 +39,12 @@ android {
 
 dependencies {
     api "com.github.parse-community.Parse-SDK-Android:parse:4.4.0"
-    api "com.squareup.okhttp3:okhttp:5.4.0"
+    api "com.squareup.okhttp3:okhttp:5.5.0"
 
-    testImplementation "org.robolectric:robolectric:4.7"
-    testImplementation "org.skyscreamer:jsonassert:1.5.0"
+    testImplementation "org.robolectric:robolectric:4.17"
+    testImplementation "org.skyscreamer:jsonassert:1.5.3"
     testImplementation "junit:junit:4.13.2"
-    testImplementation "org.mockito:mockito-core:4.6.1"
+    testImplementation "org.mockito:mockito-core:5.24.0"
 }
 
 spotless {
diff --git a/ParseLiveQuery/src/main/java/com/parse/livequery/OkHttp3SocketClientFactory.java b/ParseLiveQuery/src/main/java/com/parse/livequery/OkHttp3SocketClientFactory.java
index 56fd954..d050ff2 100644
--- a/ParseLiveQuery/src/main/java/com/parse/livequery/OkHttp3SocketClientFactory.java
+++ b/ParseLiveQuery/src/main/java/com/parse/livequery/OkHttp3SocketClientFactory.java
@@ -76,6 +76,7 @@ public class OkHttp3SocketClientFactory implements WebSocketClientFactory {
 
             @Override
             public void onFailure(okhttp3.WebSocket webSocket, Throwable t, Response response) {
+                setState(State.DISCONNECTED);
                 webSocketClientCallback.onError(t);
             }
         };
@@ -117,15 +118,24 @@ public class OkHttp3SocketClientFactory implements WebSocketClientFactory {
                     boolean sent = webSocket.send(message);
                     if (!sent) {
                         Log.w(LOG_TAG, "WebSocket.send() returned false — queue full or socket closed");
-                        webSocketClientCallback.onError(
-                            new java.io.IOException("WebSocket queue full or socket closed"));
+                        java.io.IOException failure =
+                                new java.io.IOException("WebSocket queue full or socket closed");
+                        webSocketClientCallback.onError(failure);
+                        throw new IllegalStateException(failure);
                     }
+                } catch (RuntimeException e) {
+                    throw e;
                 } catch (Exception e) {
                     Log.e(LOG_TAG, "Failed to send WebSocket message", e);
                     webSocketClientCallback.onError(e);
+                    throw new IllegalStateException(e);
                 }
             } else {
                 Log.w(LOG_TAG, "Cannot send message: state=" + state + ", webSocket=" + (webSocket != null ? "valid" : "null"));
+                java.io.IOException failure = new java.io.IOException(
+                        "WebSocket not connected: state=" + state);
+                webSocketClientCallback.onError(failure);
+                throw new IllegalStateException(failure);
             }
         }
 
diff --git a/ParseLiveQuery/src/main/java/com/parse/livequery/ParseLiveQueryClientImpl.java b/ParseLiveQuery/src/main/java/com/parse/livequery/ParseLiveQueryClientImpl.java
index 1e29fc2..808dcdf 100644
--- a/ParseLiveQuery/src/main/java/com/parse/livequery/ParseLiveQueryClientImpl.java
+++ b/ParseLiveQuery/src/main/java/com/parse/livequery/ParseLiveQueryClientImpl.java
@@ -16,11 +16,11 @@ import org.json.JSONObject;
 
 import java.net.URI;
 import java.net.URISyntaxException;
-import java.util.ArrayList;
 import java.util.Arrays;
 import java.util.List;
 import java.util.concurrent.Callable;
 import java.util.concurrent.ConcurrentHashMap;
+import java.util.concurrent.CopyOnWriteArrayList;
 import java.util.concurrent.Executor;
 
 import com.parse.boltsinternal.Continuation;
@@ -40,7 +40,7 @@ class ParseLiveQueryClientImpl implements ParseLiveQueryClient {
     private final WebSocketClient.WebSocketClientCallback webSocketClientCallback;
     private final ParseLiveQueryClock clock;
 
-    private final List<ParseLiveQueryClientCallbacks> mCallbacks = new ArrayList<>();
+    private final List<ParseLiveQueryClientCallbacks> mCallbacks = new CopyOnWriteArrayList<>();
 
     private volatile WebSocketClient webSocketClient;
     private volatile int requestIdCount = 1;
@@ -75,7 +75,7 @@ class ParseLiveQueryClientImpl implements ParseLiveQueryClient {
         this.webSocketClientFactory = webSocketClientFactory;
         this.taskExecutor = taskExecutor;
         this.clock = clock;
-        this.webSocketClientCallback = getWebSocketClientCallback();
+        this.webSocketClientCallback = callbackForGeneration(connectionGeneration);
     }
 
     private static URI getDefaultUri() {
@@ -116,7 +116,7 @@ class ParseLiveQueryClientImpl implements ParseLiveQueryClient {
         return subscription;
     }
 
-    public void connectIfNeeded() {
+    public synchronized void connectIfNeeded() {
         switch (getWebSocketState()) {
             case CONNECTED:
                 // nothing to do
@@ -175,13 +175,18 @@ class ParseLiveQueryClientImpl implements ParseLiveQueryClient {
 
         userInitiatedDisconnect = false;
         hasReceivedConnected = false;
-        webSocketClient = webSocketClientFactory.createInstance(webSocketClientCallback, uri);
+        connectionGeneration++;
+        webSocketClient = webSocketClientFactory.createInstance(
+                callbackForGeneration(connectionGeneration), uri);
         webSocketClient.open();
     }
 
     @Override
     public synchronized void disconnect() {
         cancelPendingReconnect();
+        // NOTE: no generation bump here — the current socket's own onClose
+        // must still be delivered (expected-disconnect notification); only
+        // reconnect() invalidates predecessors.
         // Fix Issue #82: Safely close even if connection was never opened
         if (webSocketClient != null) {
             try {
@@ -262,6 +267,7 @@ class ParseLiveQueryClientImpl implements ParseLiveQueryClient {
                     clientSnapshot.send(jsonString);
                 } catch (Exception e) {
                     Log.e(LOG_TAG, "Failed to send operation over WebSocket", e);
+                    throw e;
                 }
                 return null;
             }
@@ -275,8 +281,10 @@ class ParseLiveQueryClientImpl implements ParseLiveQueryClient {
 
             switch (rawOperation) {
                 case "connected":
-                    hasReceivedConnected = true;
-                    reconnectAttempts = 0; // F4: Reset only after successful LiveQuery handshake
+                    synchronized (ParseLiveQueryClientImpl.this) {
+                        hasReceivedConnected = true;
+                        reconnectAttempts = 0; // F4: Reset only after successful LiveQuery handshake
+                    }
                     long wsDuration = clock.currentTimeMillis() - wsConnectStartMs;
                     Log.v(LOG_TAG, "[WS-TIMING] Connected in " + wsDuration + "ms, sending pending subscription");
                     dispatchConnected();
@@ -324,28 +332,42 @@ class ParseLiveQueryClientImpl implements ParseLiveQueryClient {
 
     private void dispatchConnected() {
         for (ParseLiveQueryClientCallbacks callback : mCallbacks) {
-            callback.onLiveQueryClientConnected(this);
+            try {
+                callback.onLiveQueryClientConnected(this);
+            } catch (Exception e) {
+                Log.e(LOG_TAG, "Listener threw in onLiveQueryClientConnected", e);
+            }
         }
     }
 
     private void dispatchDisconnected() {
         for (ParseLiveQueryClientCallbacks callback : mCallbacks) {
-            callback.onLiveQueryClientDisconnected(this, userInitiatedDisconnect);
+            try {
+                callback.onLiveQueryClientDisconnected(this, userInitiatedDisconnect);
+            } catch (Exception e) {
+                Log.e(LOG_TAG, "Listener threw in onLiveQueryClientDisconnected", e);
+            }
         }
     }
 
 
     private void dispatchServerError(LiveQueryException exc) {
         for (ParseLiveQueryClientCallbacks callback : mCallbacks) {
-            callback.onLiveQueryError(this, exc);
+            try {
+                callback.onLiveQueryError(this, exc);
+            } catch (Exception e) {
+                Log.e(LOG_TAG, "Listener threw in onLiveQueryError", e);
+            }
         }
     }
 
     private void dispatchSocketError(Throwable reason) {
-        userInitiatedDisconnect = false;
-
         for (ParseLiveQueryClientCallbacks callback : mCallbacks) {
-            callback.onSocketError(this, reason);
+            try {
+                callback.onSocketError(this, reason);
+            } catch (Exception e) {
+                Log.e(LOG_TAG, "Listener threw in onSocketError", e);
+            }
         }
 
         dispatchDisconnected();
@@ -378,10 +400,14 @@ class ParseLiveQueryClientImpl implements ParseLiveQueryClient {
     }
 
     private <T extends ParseObject> void handleErrorEvent(JSONObject jsonObject) throws JSONException {
-        int requestId = jsonObject.getInt("requestId");
-        int code = jsonObject.getInt("code");
-        String error = jsonObject.getString("error");
-        Boolean reconnect = jsonObject.getBoolean("reconnect");
+        int requestId = jsonObject.optInt("requestId", -1);
+        if (requestId < 0) {
+            Log.w(LOG_TAG, "Error event without requestId: " + jsonObject);
+            return;
+        }
+        int code = jsonObject.optInt("code", -1);
+        String error = jsonObject.optString("error", "");
+        boolean reconnect = jsonObject.optBoolean("reconnect", false);
         final Subscription<T> subscription = subscriptionForRequestId(requestId);
         LiveQueryException exc = new LiveQueryException.ServerReportedException(code, error, reconnect);
 
@@ -390,6 +416,10 @@ class ParseLiveQueryClientImpl implements ParseLiveQueryClient {
         }
 
         dispatchServerError(exc);
+
+        if (reconnect && !userInitiatedDisconnect) {
+            scheduleReconnect();
+        }
     }
 
     private <T extends ParseObject> Subscription<T> subscriptionForRequestId(int requestId) {
@@ -398,9 +428,19 @@ class ParseLiveQueryClientImpl implements ParseLiveQueryClient {
     }
 
     private <T extends ParseObject> void sendSubscription(final Subscription<T> subscription) {
-        ParseUser.getCurrentSessionTokenAsync().onSuccess(new Continuation<String, Void>() {
+        ParseUser.getCurrentSessionTokenAsync().continueWith(new Continuation<String, Void>() {
             @Override
             public Void then(Task<String> task) throws Exception {
+                if (task.getError() != null) {
+                    Exception tokenError = task.getError();
+                    RuntimeException cause = tokenError instanceof RuntimeException
+                            ? (RuntimeException) tokenError
+                            : new RuntimeException(tokenError);
+                    subscription.didEncounter(new LiveQueryException.UnknownException(
+                            "Error fetching session token", cause),
+                            subscription.getQuery());
+                    return null;
+                }
                 String sessionToken = task.getResult();
                 SubscribeClientOperation<T> op = new SubscribeClientOperation<>(subscription.getRequestId(), subscription.getQueryState(), sessionToken);
 
@@ -409,10 +449,11 @@ class ParseLiveQueryClientImpl implements ParseLiveQueryClient {
                     public Void then(Task<Void> task) {
                         Exception error = task.getError();
                         if (error != null) {
-                            if (error instanceof RuntimeException) {
-                                subscription.didEncounter(new LiveQueryException.UnknownException(
-                                        "Error when subscribing", (RuntimeException) error), subscription.getQuery());
-                            }
+                            RuntimeException cause = error instanceof RuntimeException
+                                    ? (RuntimeException) error
+                                    : new RuntimeException(error);
+                            subscription.didEncounter(new LiveQueryException.UnknownException(
+                                    "Error when subscribing", cause), subscription.getQuery());
                         }
                         return null;
                     }
@@ -432,32 +473,83 @@ class ParseLiveQueryClientImpl implements ParseLiveQueryClient {
     private static final int MAX_RECONNECT_DELAY_MS = 30000;
     private volatile int reconnectAttempts = 0;
     private volatile boolean isReconnecting = false;
-    private Runnable pendingReconnectRunnable;
-    private android.os.Handler reconnectHandler;
+    private volatile Runnable pendingReconnectRunnable;
+    private volatile long connectionGeneration = 0;
     // Task 720: WebSocket timing for 10k visibility
     private volatile long wsConnectStartMs = 0;
 
     private synchronized void cancelPendingReconnect() {
-        if (reconnectHandler != null && pendingReconnectRunnable != null) {
-            reconnectHandler.removeCallbacks(pendingReconnectRunnable);
+        if (pendingReconnectRunnable != null) {
+            clock.removeCallbacks(pendingReconnectRunnable);
             pendingReconnectRunnable = null;
         }
         isReconnecting = false;
     }
 
+    /**
+     * Wraps the shared protocol callback so events from a superseded socket
+     * (older generation) are dropped instead of driving a new connection.
+     */
+    private WebSocketClient.WebSocketClientCallback callbackForGeneration(final long generation) {
+        final WebSocketClient.WebSocketClientCallback delegate = getWebSocketClientCallback();
+        return new WebSocketClient.WebSocketClientCallback() {
+            private boolean stale() {
+                return generation != connectionGeneration;
+            }
+
+            @Override
+            public void onOpen() {
+                if (!stale()) {
+                    delegate.onOpen();
+                }
+            }
+
+            @Override
+            public void onMessage(String message) {
+                if (!stale()) {
+                    delegate.onMessage(message);
+                }
+            }
+
+            @Override
+            public void onClose() {
+                if (!stale()) {
+                    delegate.onClose();
+                }
+            }
+
+            @Override
+            public void onError(Throwable exception) {
+                if (!stale()) {
+                    delegate.onError(exception);
+                }
+            }
+
+            @Override
+            public void stateChanged() {
+                delegate.stateChanged();
+            }
+        };
+    }
+
     private WebSocketClient.WebSocketClientCallback getWebSocketClientCallback() {
         return new WebSocketClient.WebSocketClientCallback() {
             @Override
-            public synchronized void onOpen() {
-                cancelPendingReconnect();
-                hasReceivedConnected = false;
-                // F4: reconnectAttempts reset moved to "connected" case in handleOperation
-                // Only reset after successful LiveQuery handshake, not just socket open
-                wsConnectStartMs = clock.currentTimeMillis();
+            public void onOpen() {
+                synchronized (ParseLiveQueryClientImpl.this) {
+                    cancelPendingReconnect();
+                    hasReceivedConnected = false;
+                    // F4: reconnectAttempts reset moved to "connected" case in handleOperation
+                    // Only reset after successful LiveQuery handshake, not just socket open
+                    wsConnectStartMs = clock.currentTimeMillis();
+                }
                 Log.v(LOG_TAG, "[WS-TIMING] Socket opened at " + wsConnectStartMs);
-                ParseUser.getCurrentSessionTokenAsync().onSuccessTask(new Continuation<String, Task<Void>>() {
+                ParseUser.getCurrentSessionTokenAsync().continueWithTask(new Continuation<String, Task<Void>>() {
                     @Override
                     public Task<Void> then(Task<String> task) throws Exception {
+                        if (task.getError() != null) {
+                            return Task.forError(task.getError());
+                        }
                         String sessionToken = task.getResult();
                         return sendOperationAsync(new ConnectClientOperation(applicationId, clientKey, sessionToken));
                     }
@@ -466,6 +558,16 @@ class ParseLiveQueryClientImpl implements ParseLiveQueryClient {
                         Exception error = task.getError();
                         if (error != null) {
                             Log.e(LOG_TAG, "Error when connection client", error);
+                            RuntimeException cause = error instanceof RuntimeException
+                                    ? (RuntimeException) error
+                                    : new RuntimeException(error);
+                            dispatchServerError(new LiveQueryException.UnknownException(
+                                    "Error when connecting client", cause));
+                            synchronized (ParseLiveQueryClientImpl.this) {
+                                if (!userInitiatedDisconnect) {
+                                    scheduleReconnect();
+                                }
+                            }
                         }
                         return null;
                     }
@@ -487,24 +589,28 @@ class ParseLiveQueryClientImpl implements ParseLiveQueryClient {
             }
 
             @Override
-            public synchronized void onClose() {
-                Log.v(LOG_TAG, "Socket onClose");
-                hasReceivedConnected = false;
-                dispatchDisconnected();
-                // Fix Issue #6: Auto-reconnect if not user-initiated
-                if (!userInitiatedDisconnect) {
-                    scheduleReconnect();
+            public void onClose() {
+                synchronized (ParseLiveQueryClientImpl.this) {
+                    Log.v(LOG_TAG, "Socket onClose");
+                    hasReceivedConnected = false;
+                    dispatchDisconnected();
+                    // Fix Issue #6: Auto-reconnect if not user-initiated
+                    if (!userInitiatedDisconnect) {
+                        scheduleReconnect();
+                    }
                 }
             }
 
             @Override
-            public synchronized void onError(Throwable exception) {
-                PLog.e(LOG_TAG, "Socket onError", exception);
-                hasReceivedConnected = false;
-                dispatchSocketError(exception);
-                // Fix Issue #6: Auto-reconnect on error if not user-initiated
-                if (!userInitiatedDisconnect) {
-                    scheduleReconnect();
+            public void onError(Throwable exception) {
+                synchronized (ParseLiveQueryClientImpl.this) {
+                    PLog.e(LOG_TAG, "Socket onError", exception);
+                    hasReceivedConnected = false;
+                    dispatchSocketError(exception);
+                    // Fix Issue #6: Auto-reconnect on error if not user-initiated
+                    if (!userInitiatedDisconnect) {
+                        scheduleReconnect();
+                    }
                 }
             }
 
@@ -536,18 +642,19 @@ class ParseLiveQueryClientImpl implements ParseLiveQueryClient {
         Log.d(LOG_TAG, "[RECONNECT] Scheduling reconnect in " + delay + "ms (Attempt " + reconnectAttempts
                 + ", subscriptions=" + subscriptions.size() + ")");
 
-        reconnectHandler = new android.os.Handler(android.os.Looper.getMainLooper());
         pendingReconnectRunnable = new Runnable() {
             @Override
             public void run() {
-                isReconnecting = false;
-                pendingReconnectRunnable = null;
-                if (!userInitiatedDisconnect) {
-                    Log.d(LOG_TAG, "[RECONNECT] Executing reconnect attempt " + reconnectAttempts);
-                    reconnect();
+                synchronized (ParseLiveQueryClientImpl.this) {
+                    isReconnecting = false;
+                    pendingReconnectRunnable = null;
+                    if (!userInitiatedDisconnect) {
+                        Log.d(LOG_TAG, "[RECONNECT] Executing reconnect attempt " + reconnectAttempts);
+                        reconnect();
+                    }
                 }
             }
         };
-        reconnectHandler.postDelayed(pendingReconnectRunnable, delay);
+        clock.postDelayed(pendingReconnectRunnable, delay);
     }
 }
diff --git a/ParseLiveQuery/src/main/java/com/parse/livequery/ParseLiveQueryClock.java b/ParseLiveQuery/src/main/java/com/parse/livequery/ParseLiveQueryClock.java
index e9a5b94..4d489a6 100644
--- a/ParseLiveQuery/src/main/java/com/parse/livequery/ParseLiveQueryClock.java
+++ b/ParseLiveQuery/src/main/java/com/parse/livequery/ParseLiveQueryClock.java
@@ -7,10 +7,44 @@ package com.parse.livequery;
  * <p>Production code uses {@link #SYSTEM}; tests inject a fake to make
  * reconnect timing deterministic. Satisfies the Clock Injection rule in
  * {@code docs/conventions.md} — no direct clock reads in business logic.
+ * Scheduling also runs through the clock so fakes can drive backoff
+ * without real waiting.
  */
 public interface ParseLiveQueryClock {
 
     long currentTimeMillis();
 
-    ParseLiveQueryClock SYSTEM = System::currentTimeMillis;
+    void postDelayed(Runnable runnable, long delayMs);
+
+    void removeCallbacks(Runnable runnable);
+
+    ParseLiveQueryClock SYSTEM = new ParseLiveQueryClock() {
+        private android.os.Handler handler;
+
+        private synchronized android.os.Handler handler() {
+            if (handler == null) {
+                handler = new android.os.Handler(android.os.Looper.getMainLooper());
+            }
+            return handler;
+        }
+
+        @Override
+        public long currentTimeMillis() {
+            return System.currentTimeMillis();
+        }
+
+        @Override
+        public void postDelayed(Runnable runnable, long delayMs) {
+            handler().postDelayed(runnable, delayMs);
+        }
+
+        @Override
+        public void removeCallbacks(Runnable runnable) {
+            synchronized (this) {
+                if (handler != null && runnable != null) {
+                    handler.removeCallbacks(runnable);
+                }
+            }
+        }
+    };
 }
diff --git a/ParseLiveQuery/src/main/java/com/parse/livequery/Subscription.java b/ParseLiveQuery/src/main/java/com/parse/livequery/Subscription.java
index ad46861..bcebe17 100644
--- a/ParseLiveQuery/src/main/java/com/parse/livequery/Subscription.java
+++ b/ParseLiveQuery/src/main/java/com/parse/livequery/Subscription.java
@@ -1,18 +1,20 @@
 // Sibling Docs: ./README.md | Decisions: ./DECISIONS.md
 package com.parse.livequery;
 
+import android.util.Log;
+
 import com.parse.ParseObject;
 import com.parse.ParseQuery;
 
-import java.util.ArrayList;
 import java.util.List;
+import java.util.concurrent.CopyOnWriteArrayList;
 
 class Subscription<T extends ParseObject> implements SubscriptionHandling<T> {
 
-    private final List<HandleEventsCallback<T>> handleEventsCallbacks = new ArrayList<>();
-    private final List<HandleErrorCallback<T>> handleErrorCallbacks = new ArrayList<>();
-    private final List<HandleSubscribeCallback<T>> handleSubscribeCallbacks = new ArrayList<>();
-    private final List<HandleUnsubscribeCallback<T>> handleUnsubscribeCallbacks = new ArrayList<>();
+    private final List<HandleEventsCallback<T>> handleEventsCallbacks = new CopyOnWriteArrayList<>();
+    private final List<HandleErrorCallback<T>> handleErrorCallbacks = new CopyOnWriteArrayList<>();
+    private final List<HandleSubscribeCallback<T>> handleSubscribeCallbacks = new CopyOnWriteArrayList<>();
+    private final List<HandleUnsubscribeCallback<T>> handleUnsubscribeCallbacks = new CopyOnWriteArrayList<>();
 
     private final int requestId;
     private final ParseQuery<T> query;
@@ -81,7 +83,11 @@ class Subscription<T extends ParseObject> implements SubscriptionHandling<T> {
      */
     /* package */ void didReceive(Event event, ParseQuery<T> query, T object) {
         for (HandleEventsCallback<T> handleEventsCallback : handleEventsCallbacks) {
-            handleEventsCallback.onEvents(query, event, object);
+            try {
+                handleEventsCallback.onEvents(query, event, object);
+            } catch (Exception e) {
+                Log.e("Subscription", "Listener threw in onEvents", e);
+            }
         }
     }
 
@@ -93,7 +99,11 @@ class Subscription<T extends ParseObject> implements SubscriptionHandling<T> {
      */
     /* package */ void didEncounter(LiveQueryException error, ParseQuery<T> query) {
         for (HandleErrorCallback<T> handleErrorCallback : handleErrorCallbacks) {
-            handleErrorCallback.onError(query, error);
+            try {
+                handleErrorCallback.onError(query, error);
+            } catch (Exception e) {
+                Log.e("Subscription", "Listener threw in onError", e);
+            }
         }
     }
 
@@ -105,7 +115,11 @@ class Subscription<T extends ParseObject> implements SubscriptionHandling<T> {
      */
     /* package */ void didSubscribe(ParseQuery<T> query) {
         for (HandleSubscribeCallback<T> handleSubscribeCallback : handleSubscribeCallbacks) {
-            handleSubscribeCallback.onSubscribe(query);
+            try {
+                handleSubscribeCallback.onSubscribe(query);
+            } catch (Exception e) {
+                Log.e("Subscription", "Listener threw in onSubscribe", e);
+            }
         }
     }
 
@@ -117,7 +131,11 @@ class Subscription<T extends ParseObject> implements SubscriptionHandling<T> {
      */
     /* package */ void didUnsubscribe(ParseQuery<T> query) {
         for (HandleUnsubscribeCallback<T> handleUnsubscribeCallback : handleUnsubscribeCallbacks) {
-            handleUnsubscribeCallback.onUnsubscribe(query);
+            try {
+                handleUnsubscribeCallback.onUnsubscribe(query);
+            } catch (Exception e) {
+                Log.e("Subscription", "Listener threw in onUnsubscribe", e);
+            }
         }
     }
 
diff --git a/ParseLiveQuery/src/test/java/com/parse/TestClockWiring.java b/ParseLiveQuery/src/test/java/com/parse/TestClockWiring.java
index d8f88b6..5a9573d 100644
--- a/ParseLiveQuery/src/test/java/com/parse/TestClockWiring.java
+++ b/ParseLiveQuery/src/test/java/com/parse/TestClockWiring.java
@@ -2,6 +2,7 @@
 package com.parse;
 
 import static junit.framework.Assert.assertEquals;
+import static junit.framework.Assert.assertTrue;
 import static org.mockito.ArgumentMatchers.anyBoolean;
 import static org.mockito.ArgumentMatchers.contains;
 
@@ -35,7 +36,7 @@ import static org.mockito.Mockito.when;
  * TestParseLiveQueryClient.
  */
 @RunWith(RobolectricTestRunner.class)
-@Config(sdk = 21)
+@Config(sdk = 28)
 public class TestClockWiring {
 
     private WebSocketClient webSocketClient;
@@ -103,6 +104,14 @@ public class TestClockWiring {
             public long currentTimeMillis() {
                 return fakeNow;
             }
+
+            @Override
+            public void postDelayed(Runnable runnable, long delayMs) {
+            }
+
+            @Override
+            public void removeCallbacks(Runnable runnable) {
+            }
         });
         client.reconnect();
         webSocketClientCallback.onOpen();
@@ -120,4 +129,51 @@ public class TestClockWiring {
         webSocketClientCallback.onMessage(connected.toString());
         verify(webSocketClient, never()).send(contains("\"op\":\"subscribe\""));
     }
+
+    /** Recording fake clock: captures backoff delays and fires manually. */
+    private static class RecordingClock implements ParseLiveQueryClock {
+        final java.util.List<Long> delays = new java.util.ArrayList<>();
+        private long now = 1000L;
+
+        @Override
+        public long currentTimeMillis() {
+            return now;
+        }
+
+        @Override
+        public void postDelayed(Runnable runnable, long delayMs) {
+            delays.add(delayMs);
+        }
+
+        @Override
+        public void removeCallbacks(Runnable runnable) {
+        }
+    }
+
+    @Test
+    public void backoffDelay_isDeterministicThroughClock() throws Exception {
+        RecordingClock clock = new RecordingClock();
+        ParseLiveQueryClient client = newClientWithClock(clock);
+        client.reconnect();
+        // Socket drops before any handshake: first reconnect must wait exactly BASE delay.
+        webSocketClientCallback.onClose();
+        assertEquals(1, clock.delays.size());
+        assertEquals(Long.valueOf(1000L), clock.delays.get(0));
+    }
+
+    @Test
+    public void staleSocketCallback_doesNotScheduleReconnect() throws Exception {
+        RecordingClock clock = new RecordingClock();
+        ParseLiveQueryClient client = newClientWithClock(clock);
+        client.reconnect();
+        WebSocketClient.WebSocketClientCallback firstSocket = webSocketClientCallback;
+        client.reconnect();
+        WebSocketClient.WebSocketClientCallback secondSocket = webSocketClientCallback;
+        // Old socket close arriving late must not schedule anything.
+        firstSocket.onClose();
+        assertTrue(clock.delays.isEmpty());
+        // Current socket close schedules exactly one reconnect.
+        secondSocket.onClose();
+        assertEquals(1, clock.delays.size());
+    }
 }
diff --git a/ParseLiveQuery/src/test/java/com/parse/TestOkHttpClientFactory.java b/ParseLiveQuery/src/test/java/com/parse/TestOkHttpClientFactory.java
index bd022b6..723cf17 100644
--- a/ParseLiveQuery/src/test/java/com/parse/TestOkHttpClientFactory.java
+++ b/ParseLiveQuery/src/test/java/com/parse/TestOkHttpClientFactory.java
@@ -19,6 +19,9 @@ import java.net.URI;
 import okhttp3.OkHttpClient;
 
 import static com.parse.livequery.WebSocketClient.State.NONE;
+import static org.mockito.ArgumentMatchers.any;
+import static org.mockito.Mockito.times;
+import static org.mockito.Mockito.verify;
 
 @RunWith(RobolectricTestRunner.class)
 @Config(sdk = 28)
@@ -49,7 +52,13 @@ public class TestOkHttpClientFactory {
     public void testClientCloseWithoutOpenShouldBeNoOp()  {
         Assert.assertEquals(NONE, webSocketClient.getState());
         webSocketClient.close();
-        webSocketClient.send("test");
+        try {
+            webSocketClient.send("test");
+            Assert.fail("send() while not connected must fail loudly instead of dropping");
+        } catch (IllegalStateException expected) {
+            // Expected: message loss is reported, never silent.
+        }
+        verify(webSocketClientCallbackMock, times(1)).onError(any(Throwable.class));
         Assert.assertEquals(NONE, webSocketClient.getState());
     }
 
diff --git a/ParseLiveQuery/src/test/java/com/parse/TestParseLiveQueryClient.java b/ParseLiveQuery/src/test/java/com/parse/TestParseLiveQueryClient.java
index 0c06ae5..d588167 100644
--- a/ParseLiveQuery/src/test/java/com/parse/TestParseLiveQueryClient.java
+++ b/ParseLiveQuery/src/test/java/com/parse/TestParseLiveQueryClient.java
@@ -44,7 +44,7 @@ import static org.mockito.Mockito.verify;
 import static org.mockito.Mockito.when;
 
 @RunWith(RobolectricTestRunner.class)
-@Config(sdk = 21)
+@Config(sdk = 28)
 public class TestParseLiveQueryClient {
 
     private WebSocketClient webSocketClient;
diff --git a/build.gradle b/build.gradle
index ad24df7..cc3d6b7 100644
--- a/build.gradle
+++ b/build.gradle
@@ -4,7 +4,7 @@ buildscript {
         mavenCentral()
     }
     dependencies {
-        classpath "com.android.tools.build:gradle:9.2.1"
+        classpath "com.android.tools.build:gradle:9.4.1"
     }
 }
 
diff --git a/docs/architecture.md b/docs/architecture.md
index 9d5fde1..36f6663 100644
--- a/docs/architecture.md
+++ b/docs/architecture.md
@@ -72,8 +72,8 @@ None. Client is stateless except in-memory subscription map and reconnect timer.
 ## 5. External Integrations
 
 - `com.github.parse-community.Parse-SDK-Android:parse:4.4.0` (api) — `ParseQuery`, `ParseObject`, `PointerEncoder`.
-- `com.squareup.okhttp3:okhttp:5.4.0` (api) — WebSocket transport.
-- Test: `robolectric:4.7`, `junit:4.13.2`, `mockito-core:4.6.1`, `jsonassert:1.5.0`.
+- `com.squareup.okhttp3:okhttp:5.5.0` (api) — WebSocket transport.
+- Test: `robolectric:4.17`, `junit:4.13.2`, `mockito-core:5.24.0`, `jsonassert:1.5.3`.
 
 ## 6. Deployment & Infrastructure
 
@@ -95,7 +95,7 @@ None. Client is stateless except in-memory subscription map and reconnect timer.
 
 ## 9. Future Considerations
 
-- Gradle wrapper modernization (5.6.4 → 8.x) to match AGP 9.2.1.
+- Dependency refresh (2026-10-10): OkHttp 5.5.0, Robolectric 4.17, Mockito 5.24.0, jsonassert 1.5.3, AGP 9.4.1; parse SDK 4.4.0 and JUnit 4.13.2 already latest.
 - `Clock` injection for backoff to satisfy datetime standard and enable deterministic tests.
 - Kotlin/Compose: out of scope — library stays Java.
```
<!-- END_GIT_DIFF -->
