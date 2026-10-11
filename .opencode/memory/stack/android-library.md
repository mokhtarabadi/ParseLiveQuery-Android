---
created_at: '2026-10-11T10:48:10.535401+00:00'
status: active
tags: []
updated_at: '2026-10-11T10:48:10.535438+00:00'
---

# ParseLiveQuery-Android stack
- Android library module `ParseLiveQuery`, namespace com.parse.livequery, compileSdk 36, minSdk 23, targetSdk 35, Java 8.
- Root AGP 9.4.1, Gradle wrapper 9.8.1, CI JDK 17. Dependencies: own fork SDK com.github.mokhtarabadi.Parse-SDK-Android:parse:4.4.3 (api, JitPack-built, full modules), okhttp 5.4.0 (api; 5.5.0 needs compileSdk 37), robolectric 4.17 + junit 4.13.2 (final) + mockito 5.24.0 + jsonassert 1.5.3 (test).
- Fork SDK adds a process-wide session-token cache: tests clear sCachedSessionToken reflectively in tearDown for determinism.