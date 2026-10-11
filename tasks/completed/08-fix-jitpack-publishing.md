# Task 08: Fix JitPack publishing

**File:** `tasks/completed/08-fix-jitpack-publishing.md`
**Source:** manager
**Type:** bug
**Status:** closed

## Goal

Make the JitPack 1.2.3 build succeed by adding the missing Maven publication.

## Manager's Notes

JitPack 1.2.3 errored: no `publishToMavenLocal` task. Diagnosis plus fixes: maven-publish with explicit `release` publication from `components.release` plus artifactId override (singleVariant alone ships pom-only); OkHttp capped at 5.4.0 because okhttp-android 5.5.0 requires compileSdk 37; compileSdk moved 35 to 36. Verified locally with JitPack flags: AAR plus sources plus pom under correct coordinates. Open decision: move tag 1.2.3 onto the closure commit (nothing consumed the errored version) or ship 1.2.4.

## Local TODOs

- [x] Diagnose JitPack build log
- [x] Add working Maven publication
- [x] Cap OkHttp and raise compileSdk to satisfy AAR metadata
- [x] Verify locally with JitPack flags plus full gate
- [x] Verify functionality

## Acceptance Criteria

- [x] `:ParseLiveQuery:publishToMavenLocal` succeeds with JitPack flags
- [x] Local repo holds AAR plus sources plus pom under `com.github.mokhtarabadi:ParseLiveQuery-Android:1.2.3`
- [x] Full gate green: 45 tests plus spotless
- [ ] JitPack serves the release after tag push

## Verification Evidence

- **Test command:** rtk test ./gradlew --no-daemon clean :ParseLiveQuery:testDebugUnitTest :ParseLiveQuery:createDebugUnitTestCoverageReport
- **Expected result:** BUILD SUCCESSFUL, 45 tests green, publish verified
- **Actual result:** BUILD SUCCESSFUL; tests=45 failures=0 errors=0; spotless exit 0; AAR plus sources plus pom published locally
- **Exit code:** 0

> Verification runner rule: `./gradlew --no-daemon clean :ParseLiveQuery:testDebugUnitTest :ParseLiveQuery:createDebugUnitTestCoverageReport` is the complete underlying test command. The first verification run MUST use the `rtk test` prefix; record the exact prefixed command above. A raw rerun is allowed only after a failed RTK run for detailed diagnostics.

## Definition of Done

The task is NOT done unless ALL of the following are true (unconditional, applies to every source type):

- [x] Build/Test/Lint pass with exit code 0
- [x] `lint_task_file` passes on the active task file
- [x] `CHANGELOG.md` updated via Parse-Then-Append
- [x] `verification-before-completion` applied and evidence recorded

## Risk & Rollback

- **Risk:** JitPack image may lack API 36 platform; the build log will tell.
- **Rollback plan:** Revert the task commit; republish under a new version.

---

## Execution Log & Reasoning

JitPack ran `clean -Pgroup=... -Pversion=1.2.3 -xtest -xlint publishToMavenLocal` and failed on the missing task. Added maven-publish with explicit component publication; learned singleVariant alone yields pom-only output. OkHttp 5.5.0 forced a choice between compileSdk 37 and a cap; chose 5.4.0 with compileSdk 36 as the conservative pair JitPack is most likely to satisfy.

## Factual Git Diff

<!-- BEGIN_GIT_DIFF -->
```diff
diff --git a/.opencode/memory/stack/android-library.md b/.opencode/memory/stack/android-library.md
index 92c9cb5..e3153e8 100644
--- a/.opencode/memory/stack/android-library.md
+++ b/.opencode/memory/stack/android-library.md
@@ -1,12 +1,12 @@
 ---
-created_at: '2026-10-10T20:54:12.201976+00:00'
+created_at: '2026-10-11T06:13:34.079824+00:00'
 status: active
 tags: []
-updated_at: '2026-10-10T20:54:12.201989+00:00'
+updated_at: '2026-10-11T06:13:34.079838+00:00'
 ---
 
 # ParseLiveQuery-Android stack
-- Android library module `ParseLiveQuery`, namespace com.parse.livequery, compileSdk 35, minSdk 23, targetSdk 35, Java 8.
-- Root AGP 9.4.1, Gradle wrapper 9.8.1, CI JDK 17. Dependencies: parse SDK 4.4.0 (api, latest), okhttp 5.5.0 (api), robolectric 4.17 + junit 4.13.2 (final) + mockito 5.24.0 + jsonassert 1.5.3 (test).
-- 100% Java (no Kotlin), WebSocket via OkHttp3, no Jetpack Compose. Stack skill: android-kotlin applied in Java-compat mode.
-- Robolectric 4.17 dropped old SDK levels: @Config uses sdk 28. Mockito 5 needs JVM 11+ (we run 17).
\ No newline at end of file
+- Android library module `ParseLiveQuery`, namespace com.parse.livequery, compileSdk 36, minSdk 23, targetSdk 35, Java 8.
+- Root AGP 9.4.1, Gradle wrapper 9.8.1, CI JDK 17. Dependencies: parse SDK 4.4.0 (api, latest), okhttp 5.4.0 (api; 5.5.0 needs compileSdk 37), robolectric 4.17 + junit 4.13.2 (final) + mockito 5.24.0 + jsonassert 1.5.3 (test).
+- JitPack: maven-publish `release` publication from components.release with artifactId override; singleVariant alone ships pom-only.
+- 100% Java (no Kotlin), WebSocket via OkHttp3, no Jetpack Compose. Stack skill: android-kotlin applied in Java-compat mode.
\ No newline at end of file
diff --git a/CHANGELOG.md b/CHANGELOG.md
index 810602f..da85e2f 100644
--- a/CHANGELOG.md
+++ b/CHANGELOG.md
@@ -1,6 +1,7 @@
 ## Changelog
 
 ### master
+- Fix JitPack publishing: maven-publish release publication with artifactId override; OkHttp capped at 5.4.0 with compileSdk 36
 - Review follow-ups: sync architecture tree and overload count, CopyOnWriteArrayList in data model, current gate in test docs, chronological ADRs
 - Upgrade deps: OkHttp 5.5.0, Robolectric 4.17, Mockito 5.24.0, jsonassert 1.5.3, AGP 9.4.1; Robolectric configs moved to sdk 28
 - Harden reconnect lifecycle: generation-guarded callbacks, clock-driven scheduling, loud send failures, guarded listeners
diff --git a/ParseLiveQuery/build.gradle b/ParseLiveQuery/build.gradle
index fe94f71..a11f2f2 100644
--- a/ParseLiveQuery/build.gradle
+++ b/ParseLiveQuery/build.gradle
@@ -5,7 +5,7 @@ plugins {
 apply plugin: "com.android.library"
 
 android {
-    compileSdk 35
+    compileSdk 36
     namespace "com.parse.livequery"
 
     defaultConfig {
@@ -20,6 +20,12 @@ android {
         buildConfig = true
     }
 
+    publishing {
+        singleVariant("release") {
+            withSourcesJar()
+        }
+    }
+
     lint {
         abortOnError false
     }
@@ -39,7 +45,7 @@ android {
 
 dependencies {
     api "com.github.parse-community.Parse-SDK-Android:parse:4.4.0"
-    api "com.squareup.okhttp3:okhttp:5.5.0"
+    api "com.squareup.okhttp3:okhttp:5.4.0"
 
     testImplementation "org.robolectric:robolectric:4.17"
     testImplementation "org.skyscreamer:jsonassert:1.5.3"
@@ -54,3 +60,23 @@ spotless {
         googleJavaFormat()
     }
 }
+
+// JitPack publishes via: ./gradlew clean -Pgroup=... -Pversion=... -xtest -xlint publishToMavenLocal
+apply plugin: "maven-publish"
+
+group = findProperty("group") ?: "com.github.mokhtarabadi"
+
+base {
+    archivesName = "ParseLiveQuery-Android"
+}
+
+afterEvaluate {
+    publishing {
+        publications {
+            release(MavenPublication) {
+                from components["release"]
+                artifactId = "ParseLiveQuery-Android"
+            }
+        }
+    }
+}
diff --git a/docs/architecture.md b/docs/architecture.md
index 1e2142c..3c29edb 100644
--- a/docs/architecture.md
+++ b/docs/architecture.md
@@ -75,14 +75,14 @@ None. Client is stateless except in-memory subscription map and reconnect timer.
 ## 5. External Integrations
 
 - `com.github.parse-community.Parse-SDK-Android:parse:4.4.0` (api) — `ParseQuery`, `ParseObject`, `PointerEncoder`.
-- `com.squareup.okhttp3:okhttp:5.5.0` (api) — WebSocket transport.
+- `com.squareup.okhttp3:okhttp:5.4.0` (api) — WebSocket transport (capped: okhttp-android 5.5.0 requires compileSdk 37).
 - Test: `robolectric:4.17`, `junit:4.13.2`, `mockito-core:5.24.0`, `jsonassert:1.5.3`.
 
 ## 6. Deployment & Infrastructure
 
 - JitPack artifact `com.github.parse-community:ParseLiveQuery-Android`.
 - CI (`.github/workflows/ci.yml`): `check-lint` (JDK 17 + `spotlessCheck` with upstream ratchet), `check-gradlewrapper`, `check-build` (`clean` + unit tests + AGP built-in coverage report + codecov). Checkout uses full history (`fetch-depth: 0`) so the Spotless ratchet can resolve `origin/master`.
-- Android: `compileSdk/targetSdk 35`, `minSdk 23`, Java 8, `abortOnError false`.
+- Android: `compileSdk` 36, `targetSdk` 35, `minSdk` 23, Java 8, `abortOnError false`.
 
 ## 7. Security Considerations
 
@@ -98,7 +98,8 @@ None. Client is stateless except in-memory subscription map and reconnect timer.
 
 ## 9. Future Considerations
 
-- Dependency refresh (2026-10-10): OkHttp 5.5.0, Robolectric 4.17, Mockito 5.24.0, jsonassert 1.5.3, AGP 9.4.1; parse SDK 4.4.0 and JUnit 4.13.2 already latest.
+- Dependency refresh (2026-10-10): OkHttp 5.4.0, Robolectric 4.17, Mockito 5.24.0, jsonassert 1.5.3, AGP 9.4.1, compileSdk 36; parse SDK 4.4.0 and JUnit 4.13.2 already latest.
+- JitPack publishing: `maven-publish` with explicit `release` publication (`components.release`, artifactId override); verified locally into mavenLocal with JitPack flags.
 - `Clock` injection done: `ParseLiveQueryClock` drives reconnect reads and scheduling, covered by deterministic tests.
 - Kotlin/Compose: out of scope — library stays Java.
```
<!-- END_GIT_DIFF -->
