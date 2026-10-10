---
created_at: '2026-10-10T20:13:45.214921+00:00'
status: active
tags: []
updated_at: '2026-10-10T20:13:45.214938+00:00'
---

# Test commands
- CI: `./gradlew --no-daemon clean :ParseLiveQuery:testDebugUnitTest :ParseLiveQuery:createDebugUnitTestCoverageReport` (JDK 17, ubuntu-latest).
- Lint gate: `./gradlew :ParseLiveQuery:spotlessCheck --no-daemon` (Spotless 8.x, ratchetFrom origin/master).
- Local verify: `./gradlew :ParseLiveQuery:testDebugUnitTest --no-daemon` for fast unit loop (Gradle 9.8.1 wrapper).
- Legacy `jacocoTestReport` task no longer exists (custom task removed with AGP 9); use AGP built-in `createDebugUnitTestCoverageReport`.