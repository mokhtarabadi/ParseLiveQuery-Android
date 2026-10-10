# Slice: tests (com.parse)

Unit verification for the LiveQuery client via Robolectric + JUnit4 + Mockito.

## Duties

- Cover subscribe wire encoding including the `fields`/`objectId` invariant.
- Cover client state machine: connect handshake ordering, reconnect backoff, adversarial disconnects.
- Cover transport factory behavior with fakes (`TestOkHttpClientFactory`, `ImmediateExecutor`).

## Files

- `livequery/TestSubscribeOperationFields.java` — Subscribe JSON shape + objectId injection.
- `TestParseLiveQueryClient.java` — Core client behavior (god-node adjacency: onError/createInstance).
- `TestAdversarialFixes.java` — Race/edge fixes.
- `TestReconnectBackoff.java` — Backoff scheduling/cancel discipline.
- `TestOkHttpClientFactory.java` — Transport factory.
- `ImmediateExecutor.java` — Synchronous executor for deterministic tests.

## Key Risks & Invariants

- Tests must run under `./gradlew :ParseLiveQuery:testDebugUnitTest`; full gate is `clean jacocoTestReport` on JDK 11.
- Backoff tests are timing-sensitive — prefer injected executors over sleeps.
- Do not assert on `sessionToken` values in logs.
