# Slice: com.parse.livequery

Realtime subscription engine for Parse queries over OkHttp WebSocket.

## Duties

- Own the public subscribe/unsubscribe API (`ParseLiveQueryClient`, `SubscriptionHandling`).
- Own the protocol state machine (`ParseLiveQueryClientImpl`): connect handshake, requestId correlation, event dispatch, reconnect backoff.
- Own wire encoding (`ClientOperation` family) and transport abstraction (`WebSocketClient`, `OkHttp3SocketClientFactory`).

## Files

- `ParseLiveQueryClient.java` — Public interface + `Factory.getClient` overloads.
- `ParseLiveQueryClientImpl.java` — Protocol engine; thread-safe subscriptions + reconnect.
- `ParseLiveQueryClock.java` — Injectable wall-clock plus scheduler; `SYSTEM` default, fakes in tests.
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
- `sessionToken` is sensitive — never log it; send-path logs mask it as `***`.
- Global server errors (no `requestId`) still reach `dispatchServerError`.
- Reconnect retries forever with capped backoff plus clock jitter unless the user disconnects.
