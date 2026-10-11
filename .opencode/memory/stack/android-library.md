---
created_at: '2026-10-10T20:54:12.201976+00:00'
status: active
tags: []
updated_at: '2026-10-10T20:54:12.201989+00:00'
---

# ParseLiveQuery-Android stack
- Android library module `ParseLiveQuery`, namespace com.parse.livequery, compileSdk 35, minSdk 23, targetSdk 35, Java 8.
- Root AGP 9.4.1, Gradle wrapper 9.8.1, CI JDK 17. Dependencies: parse SDK 4.4.0 (api, latest), okhttp 5.5.0 (api), robolectric 4.17 + junit 4.13.2 (final) + mockito 5.24.0 + jsonassert 1.5.3 (test).
- 100% Java (no Kotlin), WebSocket via OkHttp3, no Jetpack Compose. Stack skill: android-kotlin applied in Java-compat mode.
- Robolectric 4.17 dropped old SDK levels: @Config uses sdk 28. Mockito 5 needs JVM 11+ (we run 17).