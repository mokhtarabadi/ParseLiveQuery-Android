---
created_at: '2026-10-10T19:40:42.159001+00:00'
status: active
tags: []
updated_at: '2026-10-10T19:40:42.159022+00:00'
---

# LiveQuery graph quirks
- God nodes: OkHttp3SocketClientFactory open/send/close, ParseLiveQueryClientImpl subscribe/disconnect, ClientOperation.getJSONObjectRepresentation.
- Core flow: ParseLiveQueryClient.Factory.getClient -> ParseLiveQueryClientImpl.connectIfNeeded -> WebSocketClient -> handleOperationAsync/parseMessage -> Subscription dispatch (CREATE/UPDATE/ENTER/LEAVE/DELETE).
- Reconnect: scheduleReconnect/cancelPendingReconnect with backoff (see TestReconnectBackoff). Threading: background executor for connect/disconnect.