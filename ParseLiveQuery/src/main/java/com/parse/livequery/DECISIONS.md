# Architectural Decisions: com.parse.livequery

## [2026-10-10] ADR-004: Injectable clock for reconnect timing

- **Context:** Two direct `System.currentTimeMillis()` reads lived on the reconnect path, violating the Clock Injection rule and blocking deterministic tests.
- **Decision:** New public `ParseLiveQueryClock` interface with a `SYSTEM` default; `ParseLiveQueryClientImpl` takes it via a new constructor overload and a new `Factory.getClient` overload. All prior overloads delegate to the system clock, so the public API stays backward compatible.
- **Consequences:** Tests can inject a fake clock; see `TestParseLiveQueryClock`.
- **Rollback:** Drop the overloads and restore direct reads; single-file revert.

## [2026-10-10] ADR-005: Keep reconnecting with zero subscriptions (disputed QA revert)

- **Context:** QA asked to restore the `subscriptions.isEmpty()` guard in `scheduleReconnect`. The guard was deliberately removed (see `9d2ef80` era notes) so the client keeps retrying even when the subscription map is momentarily empty, and reconnects on socket close regardless of map state.
- **Decision:** Keep the guard out; cover the empty path with `connectedWithEmptySubscriptions_sendsNothing` instead of reverting.
- **Consequences:** Reconnect attempts continue with zero subscriptions (by design); the connected handler iterates an empty map and sends nothing.
- **Rollback:** Re-add `|| subscriptions.isEmpty()` to the `scheduleReconnect` early return; no data impact.

## [2026-10-10] ADR-001: Initial Slice Baseline

- **Context:** Baseline documentation initialized via init-folder-docs during Phase 0.
- **Decision:** Adopting Living Folder Docs standard for this component.
- **Consequences:** All future structural or contract changes must be recorded here.
- **Rollback:** N/A (baseline adoption).

## [2026-10-10] ADR-002: Preserve OkHttp transport + Java-only surface

- **Context:** Discovery found 100% Java with OkHttp 5.4.0 transport and Parse SDK 4.4.0; god nodes cluster on `OkHttp3SocketClientFactory` open/send/close.
- **Decision:** Keep OkHttp transport SPI (`WebSocketClient`/`WebSocketClientFactory`) and Java-only public API; no Kotlin/Compose migration in Phase 0.
- **Consequences:** Stack skill runs in Java-compat mode; transport swaps stay behind the factory interface.
- **Rollback:** Revisit if minSdk or OkHttp major forces migration; record new ADR.

## [2026-10-10] ADR-003: Fork-ahead changelog (upstream comparison)

- **Context:** `upstream/master..origin/master` holds exactly `d5823ce` plus `9d2ef80`; reverse range is empty, tags match through `1.2.2`.
- **Decision:** Treat the fork as upstream plus: modernized build and deps, `clientKey` in connect, tuned and hardened OkHttp transport, two-pass reconnect backoff hardening, `objectId`-guaranteed subscribe fields, three new test files.
- **Consequences:** Any upstream merge must re-verify `TestReconnectBackoff` and `TestAdversarialFixes`; see `docs/architecture.md` section 10.
- **Rollback:** Cherry-pick revert per commit; transport and backoff changes are independent of the build changes.
