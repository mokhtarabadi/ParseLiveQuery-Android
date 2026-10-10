# Slice: com.parse.livequery

Realtime subscription engine for Parse queries over OkHttp WebSocket.

## Duties

- Own the public subscribe/unsubscribe API (`ParseLiveQueryClient`, `SubscriptionHandling`).
- Own the protocol state machine (`ParseLiveQueryClientImpl`): connect handshake, requestId correlation, event dispatch, reconnect backoff.
- Own wire encoding (`ClientOperation` family) and transport abstraction (`WebSocketClient`, `OkHttp3SocketClientFactory`).

## Files

- `ParseLiveQueryClient.java` — Public interface + `Factory.getClient` overloads.
- `ParseLiveQueryClientImpl.java` — Protocol engine; thread-safe subscriptions + reconnect.
- `Subscription.java` — requestId + frozen query state + callback lists.
- `SubscriptionHandling.java` — Handler interface + `Event` enum (CREATE/UPDATE/ENTER/LEAVE/DELETE).
- `ClientOperation.java` / `ConnectClientOperation.java` / `SubscribeClientOperation.java` / `UnsubscribeClientOperation.java` — Wire JSON encoders.
- `WebSocketClient.java` / `WebSocketClientFactory.java` / `OkHttp3SocketClientFactory.java` — Transport SPI + OkHttp impl.
- `ParseLiveQueryClientCallbacks.java` — Connection-level listener.
- `LiveQueryException.java` — Protocol error type.

## Key Risks & Invariants

- Must wait for server `connected` before sending subscribes (race fix).
- Subscription map must stay thread-safe; `close` on socket is synchronized.
- `SubscribeClientOperation` must always include `objectId` in `fields` or events drop silently.
- `didUnsubscribe` fires only on explicit unregister, never on socket close.
- `sessionToken` is sensitive — never log it.
