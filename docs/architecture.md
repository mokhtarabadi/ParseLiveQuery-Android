# Architecture Overview

Living overview of ParseLiveQuery-Android. Update as the codebase evolves. Grounded in S1 discovery: tree + signatures + graph (256 nodes, 353 links).

## 1. Project Structure

```
ParseLiveQuery-Android/
├── ParseLiveQuery/src/main/java/com/parse/livequery/
│   ├── ParseLiveQueryClient.java          # Public interface + Factory.getClient overloads
│   ├── ParseLiveQueryClientImpl.java      # Protocol state machine, reconnect, dispatch
│   ├── Subscription.java                  # requestId + query + callback lists
│   ├── SubscriptionHandling.java           # Public handler interface + Event enum
│   ├── ClientOperation.java               # Abstract getJSONObjectRepresentation
│   ├── ConnectClientOperation.java        # op=connect
│   ├── SubscribeClientOperation.java      # op=subscribe (className/where/fields/sessionToken)
│   ├── UnsubscribeClientOperation.java    # op=unsubscribe
│   ├── WebSocketClient.java               # Transport interface (open/close/send/State)
│   ├── WebSocketClientFactory.java        # Transport factory
│   ├── OkHttp3SocketClientFactory.java    # OkHttp3 transport impl (god node)
│   ├── ParseLiveQueryClientCallbacks.java # Connection-level listener
│   └── LiveQueryException.java            # Protocol error type
├── ParseLiveQuery/src/test/java/com/parse/
│   ├── livequery/TestSubscribeOperationFields.java
│   ├── TestParseLiveQueryClient.java
│   ├── TestAdversarialFixes.java
│   ├── TestReconnectBackoff.java
│   ├── TestOkHttpClientFactory.java
│   └── ImmediateExecutor.java
├── docs/ (conventions.md, architecture.md, data_model.md)
├── tasks/ (backlog, in-progress, qa, completed, archive)
├── AGENTS.md, opencode.json, CHANGELOG.md
└── .github/workflows/ci.yml
```

## 2. High-Level System Diagram

```
[Android App] --subscribe(ParseQuery)--> [ParseLiveQueryClientImpl]
    <--dispatch events (CREATE/UPDATE/ENTER/LEAVE/DELETE)--+
[ParseLiveQueryClientImpl] --ClientOperation JSON--> [OkHttp3 WebSocket] <--> [Parse Server LiveQuery]
    --scheduleReconnect/cancelPendingReconnect on close/error-->
```

Flow: `Factory.getClient()` → `connectIfNeeded()` → `ConnectClientOperation` → server `connected` → `sendSubscription` per `Subscription` → server `subscribed` → object events → `parseMessage`/`handleOperationAsync` → `Subscription.didReceive` → app callbacks.

## 3. Core Components

### 3.1 Public API surface

- `ParseLiveQueryClient` (`ParseLiveQueryClient.java`): `subscribe`, `unsubscribe` (x2), `connectIfNeeded`, `reconnect`, `disconnect`, `registerListener`/`unregisterListener`, `Factory.getClient` (5 overloads: default URI from `Parse.initialize`, custom `URI`, custom `WebSocketClientFactory`, custom `Executor`).
- `SubscriptionHandling` (`SubscriptionHandling.java`): `handleEvents`, `handleEvent(Event, cb)`, `handleError`, `handleSubscribe`, `handleUnsubscribe`, `getRequestId`.

### 3.2 Protocol engine

- `ParseLiveQueryClientImpl` (`ParseLiveQueryClientImpl.java`): owns `requestIdGenerator` (synchronized int), `ConcurrentHashMap` subscriptions, `getWebSocketState`/`isConnected`/`inAnyState`, `handleOperationAsync`, `sendOperationAsync`, `parseMessage`, `dispatchConnected/Disconnected/ServerError/SocketError`, `handleSubscribed/Unsubscribed/Object/ErrorEvent`, `subscriptionForRequestId`, `sendSubscription`/`sendUnsubscription`, `scheduleReconnect`/`cancelPendingReconnect`, `getWebSocketClientCallback`. God-node methods: `subscribe`, `disconnect`.

### 3.3 Transport

- `WebSocketClient` (`WebSocketClient.java`): `open/close/send/getState`, `State {NONE, CONNECTING, CONNECTED, DISCONNECTING, DISCONNECTED}`, `WebSocketClientCallback (onOpen/onMessage/onClose/onError/stateChanged)`.
- `OkHttp3SocketClientFactory` (`OkHttp3SocketClientFactory.java`): god nodes `open/send/close`; wraps OkHttp3 WebSocket.

### 3.4 Operations (wire encoding)

- `ClientOperation.getJSONObjectRepresentation` (god node, `ClientOperation.java:7`).
- `ConnectClientOperation`, `SubscribeClientOperation` (always injects `objectId` into `fields`), `UnsubscribeClientOperation`.

## 4. Data Stores

None. Client is stateless except in-memory subscription map and reconnect timer. No DB, no cache.

## 5. External Integrations

- `com.github.parse-community.Parse-SDK-Android:parse:4.4.0` (api) — `ParseQuery`, `ParseObject`, `PointerEncoder`.
- `com.squareup.okhttp3:okhttp:5.4.0` (api) — WebSocket transport.
- Test: `robolectric:4.7`, `junit:4.13.2`, `mockito-core:4.6.1`, `jsonassert:1.5.0`.

## 6. Deployment & Infrastructure

- JitPack artifact `com.github.parse-community:ParseLiveQuery-Android`.
- CI (`.github/workflows/ci.yml`): `check-lint` (JDK 11 + `spotlessCheck`), `check-gradlewrapper`, `check-build` (`clean jacocoTestReport` + codecov). Note: root AGP 9.2.1 vs wrapper 5.6.4 skew — local builds require modern Gradle; CI pins JDK 11.
- Android: `compileSdk/targetSdk 35`, `minSdk 23`, Java 8, `abortOnError false`.

## 7. Security Considerations

- `wss://` expected for custom URIs; default URI derived from `Parse.initialize` server URL (http→ws mapping).
- `sessionToken` forwarded in subscribe payload — never log it.
- No auth refresh logic in-client; token expiry surfaces as server error event.

## 8. Development & Testing Environment

- Fast loop: `./gradlew :ParseLiveQuery:testDebugUnitTest --no-daemon`.
- Full gate: `./gradlew --no-daemon clean jacocoTestReport` + `./gradlew spotlessCheck --no-daemon`.
- Key tests: `TestParseLiveQueryClient`, `TestSubscribeOperationFields` (fields/objectId invariant), `TestReconnectBackoff`, `TestAdversarialFixes`.

## 9. Future Considerations

- Gradle wrapper modernization (5.6.4 → 8.x) to match AGP 9.2.1.
- `Clock` injection for backoff to satisfy datetime standard and enable deterministic tests.
- Kotlin/Compose: out of scope — library stays Java.

## 10. Fork Divergence vs Upstream (parse-community)

Comparison date 2026-10-10, via `git log upstream/master..origin/master`. The fork is exactly 2 commits ahead; `origin/master..upstream/master` is empty (nothing to pull). Tags are identical through `1.2.2`; neither side carries `jitpack.yml`.

- `d5823ce` ("fix many bugs"): build modernization (AGP 3.5.2 → 9.2.1, compile/target SDK 29 → 35, minSdk 15 → 23, jcenter → mavenCentral, coveralls/legacy jacoco/javadocs plugins removed, Java 8 `compileOptions`, new `lint`/`testCoverage` DSL); dependency upgrades (parse 1.24.2 → 4.4.0, okhttp 3.12.10 → 5.4.0, robolectric 3.3.1 → 4.7, junit 4.12 → 4.13.2, mockito 1.10.19 → 4.6.1); `clientKey` added to the connect op; OkHttp tuned for long-lived sockets (0 read timeout, 15s ping, tuned pool) with hardened `send` (null guard, boolean-return check, error propagation); reconnect backoff plus thread-safety pass; `fields`/`objectId` injection in subscribe; 3 new test files (`TestAdversarialFixes`, `TestReconnectBackoff`, `TestSubscribeOperationFields`).
- `9d2ef80` ("reconnect backoff, thread safety"): follow-up hardening of `ParseLiveQueryClientImpl` backoff scheduling and synchronized paths (single file, +20/-13).

Merge caution: upstream `ParseLiveQueryClientImpl` and `OkHttp3SocketClientFactory` will conflict on any upstream touch of reconnect or `send` — always re-run `TestReconnectBackoff` plus `TestAdversarialFixes` after a merge.
