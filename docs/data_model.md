# Data Model

Wire protocol + in-memory model for ParseLiveQuery-Android. Field names are exact server contract — do not rename without a server-compat ADR.

## 1. Client → Server operations

### connect (`ConnectClientOperation`)

```json
{ "op": "connect", "applicationId": "<id>", "clientKey": "<key|omitted when null>", "sessionToken": "<token>" }
```

Sent once after socket `onOpen`. Server replies `{"op":"connected"}` — client must wait for it before sending subscribes (race fix, see CHANGELOG 1.0.3). Fork addition: `clientKey` is sent only when non-null.

### subscribe (`SubscribeClientOperation.java:24-58`)

```json
{
  "op": "subscribe",
  "requestId": 7,
  "sessionToken": "<token>",
  "query": {
    "className": "Message",
    "where": { "<encoded constraints via PointerEncoder>" : {} },
    "fields": ["title", "objectId"]
  }
}
```

- `requestId`: synchronized int from `requestIdGenerator`, also the `Subscription.getRequestId` key.
- `fields`: from `ParseQuery.State.selectedKeys()`; `objectId` is force-injected when missing (V5 fix — without it the server silently drops events).
- `where`: `PointerEncoder.encode(state.constraints())`.

### unsubscribe (`UnsubscribeClientOperation`)

```json
{ "op": "unsubscribe", "requestId": 7 }
```

`didUnsubscribe` fires only on explicit `unregister()` — not on socket close.

## 2. Server → Client messages (`ParseLiveQueryClientImpl.parseMessage`)

| op | Meaning | Handling |
|----|---------|----------|
| `connected` | Handshake done | `dispatchConnected`, flush pending subscribes |
| `subscribed` | `{requestId}` accepted | `handleSubscribedEvent` → `didSubscribe` |
| `unsubscribed` | `{requestId}` removed | `handleUnsubscribedEvent` → `didUnsubscribe` |
| `create` / `update` / `enter` / `leave` / `delete` | Object event + `{requestId, object}` | `handleObjectEvent(Event, json)` → decode `ParseObject` → `didReceive` |
| `error` | `{requestId?, code, message}` | `handleErrorEvent` → `didEncounter(LiveQueryException)`; no requestId → `dispatchServerError` |

`Subscription.Event` enum mirrors the five object ops.

## 3. In-memory entities

### Subscription (`Subscription.java:9-24`)

- `requestId: int` — wire correlation key.
- `query: ParseQuery<T>` + frozen `state: ParseQuery.State<T>` (built at construction; later query mutation does not affect the wire payload).
- Callback lists: `handleEventsCallbacks`, `handleErrorCallbacks`, `handleSubscribeCallbacks`, `handleUnsubscribeCallbacks` (ArrayList — accessed on callback executor).

### Client session state (`ParseLiveQueryClientImpl`)

- `subscriptions: ConcurrentHashMap<Integer, Subscription>` (thread-safe since 1.0.5).
- `webSocketClient: WebSocketClient` + `State` machine (`NONE → CONNECTING → CONNECTED → DISCONNECTING → DISCONNECTED`).
- Reconnect: `scheduleReconnect` with backoff + `cancelPendingReconnect` (synchronized); `close` on the socket client is synchronized (1.0.6 fix).
- Listeners: `ParseLiveQueryClientCallbacks` set for `onConnected/onDisconnected/onServerError/onSocketError`.

## 4. Nullability & datetime rules

- `sessionToken`: nullable — omitted when logged out; never send literal `"null"`.
- `fields`: nullable — absent key means "all fields"; empty array is not sent.
- Timestamps inside event objects are server Parse dates (`{"__type":"Date","iso":"..."}`) — transport as ISO-8601 with offset at the boundary, store UTC at rest per `docs/conventions.md`. No client clock reads except backoff timer (migrate to injected `Clock`).
