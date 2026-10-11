---
created_at: '2026-10-11T06:13:34.079824+00:00'
status: active
tags: []
updated_at: '2026-10-11T06:13:34.079838+00:00'
---

# ParseLiveQuery-Android stack
- Android library module `ParseLiveQuery`, namespace com.parse.livequery, compileSdk 36, minSdk 23, targetSdk 35, Java 8.
- Root AGP 9.4.1, Gradle wrapper 9.8.1, CI JDK 17. Dependencies: parse SDK 4.4.0 (api, latest), okhttp 5.4.0 (api; 5.5.0 needs compileSdk 37), robolectric 4.17 + junit 4.13.2 (final) + mockito 5.24.0 + jsonassert 1.5.3 (test).
- JitPack: maven-publish `release` publication from components.release with artifactId override; singleVariant alone ships pom-only.
- 100% Java (no Kotlin), WebSocket via OkHttp3, no Jetpack Compose. Stack skill: android-kotlin applied in Java-compat mode.