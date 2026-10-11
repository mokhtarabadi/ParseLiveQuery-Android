## Changelog

### master
- Audit round two: slice README coverage for clock files plus ADR-006 for JitPack publishing
- Fix JitPack publishing: maven-publish release publication with artifactId override; OkHttp capped at 5.4.0 with compileSdk 36
- Review follow-ups: sync architecture tree and overload count, CopyOnWriteArrayList in data model, current gate in test docs, chronological ADRs
- Upgrade deps: OkHttp 5.5.0, Robolectric 4.17, Mockito 5.24.0, jsonassert 1.5.3, AGP 9.4.1; Robolectric configs moved to sdk 28
- Harden reconnect lifecycle: generation-guarded callbacks, clock-driven scheduling, loud send failures, guarded listeners
- Fix Brain review races: unified reconnect monitor, CopyOnWrite callback lists, faulted send tasks, token-failure routing, tolerant error parsing, DISCONNECTED on transport failure
- Cognitive-lead audit: staging and index-isolation guardrails plus deprecated-section purge rule in AGENTS.md; sibling-docs pointer on clock test
- Align wrapper with AGP 9.2.1: Gradle 5.6.4 to 9.8.1; CI JDK 11 to 17; add Spotless check with upstream ratchet; pin JitPack JDK 17
- Inject ParseLiveQueryClock into the reconnect path with a backward-compatible Factory overload plus deterministic clock tests

### 1.2.3
- Modernize build: AGP 9.2.1, SDK 35, minSdk 23, mavenCentral; drop legacy coverage plugins
- Upgrade deps: Parse SDK 4.4.0, OkHttp 5.4.0, Robolectric 4.7, JUnit 4.13.2, Mockito 4.6.1
- Add clientKey to connect operation; harden OkHttp send path and reconnect backoff
- Guarantee objectId in subscribe fields so events are never silently dropped
- Phase 0 Cognitive init: AGENTS.md, opencode.json (build agent, ZAC), docs/conventions.md, docs/architecture.md, docs/data_model.md, tasks/ Kanban (5 dirs) with 01-align-gradle-wrapper + 02-inject-clock-reconnect-backoff, living docs for livequery + tests slices with code pointers on 4 primary sources.

### 1.2.2
- CHANGE: Change OkHttp dependency to allow for Android versions < 5.0

### 1.2.1
> NOTE:
>
> This release requires Parse Android SDK >=`1.24.2` as dependency because of the transient depencency on the internalized bolts module.
- CHANGE: Changed bolts references to use internalized bolts depedency. See [#1036](https://github.com/parse-community/Parse-SDK-Android/issues/1036) for details. Thanks to [Manuel Trezza](https://github.com/mtrezza)

### 1.2.0
- Upgrade to avoid depending on outdated Bolts (#107)

### 1.1.0
- Repackage from com.parse to com.parse.livequery
- Bumps to use the latest dependency on JitPack for compatibility

### 1.0.6
- Safely call close on the websocket client with synchronized calls
thanks to @mmimeault (#83)

### 1.0.5
- Back the subscriptions by a thread safe collection map thanks to @mmimeault (#80)

### 1.0.4
- Change package name thanks to @hermanliang (#64)

### 1.0.3
- Fix race condition by ensuring that op=connected has been received before sending a new subscribe event thanks to @jhansche (#48)

### 1.0.2
- Dispatch better disconnect events thanks to @jhansche (#39)

### 1.0.1
- getClient() method can get URL inferred from Parse.initialize() call thanks to @hermanliang (#30)
- Bump to support Android API 25 (#32)
- Bump to Parse Android 1.14.1 dependency (#32).
- Switch from TubeSock library to OkHttp3 web scokets (#28)
- Fix Locale lint errors thanks to @jhansche (#23)
- Connect/disconnect on background executor thanks to @jhansche (#22)
- Refactor ParseLiveQueryClient not to be typed thanks to @jhansche (#27)

### 1.0.0
- Initial 1.0.0 release
