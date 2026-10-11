# Task 11: Adopt fork SDK 4.4.3

**File:** `tasks/completed/11-adopt-fork-sdk-443.md`
**Source:** manager
**Type:** improvement
**Status:** closed

## Goal

Move the Parse SDK dependency to the fork 4.4.3 with full module publications.

## Manager's Notes

Sibling session proved 4.4.2 publishes only parse plus bolts-tasks while 4.4.3 carries all nine modules with correct coordinates. Bump and re-verify the gate.

## Local TODOs

- [x] Bump fork SDK to 4.4.3
- [x] Verify full gate plus docs sync
- [x] Verify functionality

## Acceptance Criteria

- [x] Dependency resolves to fork 4.4.3 from JitPack
- [x] Full gate green plus spotless

## Verification Evidence

- **Test command:** rtk test ./gradlew --no-daemon clean :ParseLiveQuery:testDebugUnitTest :ParseLiveQuery:createDebugUnitTestCoverageReport
- **Expected result:** BUILD SUCCESSFUL, all tests green
- **Actual result:** BUILD SUCCESSFUL; tests=45 failures=0 errors=0; spotless exit 0
- **Exit code:** 0

> Verification runner rule: `./gradlew --no-daemon clean :ParseLiveQuery:testDebugUnitTest :ParseLiveQuery:createDebugUnitTestCoverageReport` is the complete underlying test command. The first verification run MUST use the `rtk test` prefix; record the exact prefixed command above. A raw rerun is allowed only after a failed RTK run for detailed diagnostics.

## Definition of Done

The task is NOT done unless ALL of the following are true (unconditional, applies to every source type):

- [x] Build/Test/Lint pass with exit code 0
- [x] `lint_task_file` passes on the active task file
- [x] `CHANGELOG.md` updated via Parse-Then-Append
- [x] `verification-before-completion` applied and evidence recorded

## Risk & Rollback

- **Risk:** New SDK build changes behavior under test.
- **Rollback plan:** Revert to 4.4.2; suite guards regressions.

---

## Execution Log & Reasoning

Bumped parse 4.4.2 to 4.4.3 after sibling session proved full module publications with correct coordinates (AAR verified downloadable). Drop-in compatible: no test changes needed.

Release prep: AAR versionName 1.2.5 with versionCode 3 plus CHANGELOG entry; re-verified green after the bump.

## Factual Git Diff

<!-- BEGIN_GIT_DIFF -->
```diff
diff --git a/CHANGELOG.md b/CHANGELOG.md
index 9628d28..ceee557 100644
--- a/CHANGELOG.md
+++ b/CHANGELOG.md
@@ -1,6 +1,7 @@
 ## Changelog
 
 ### master
+- Adopt fork SDK 4.4.3 with full module publications
 - Verify dep round: all direct deps already latest-safe; OkHttp held at 5.4.0 pending compileSdk 37 coverage
 
 ### 1.2.4
diff --git a/ParseLiveQuery/build.gradle b/ParseLiveQuery/build.gradle
index c2f2156..1e7170d 100644
--- a/ParseLiveQuery/build.gradle
+++ b/ParseLiveQuery/build.gradle
@@ -44,7 +44,7 @@ android {
 }
 
 dependencies {
-    api "com.github.mokhtarabadi.Parse-SDK-Android:parse:4.4.2"
+    api "com.github.mokhtarabadi.Parse-SDK-Android:parse:4.4.3"
     api "com.squareup.okhttp3:okhttp:5.4.0"
 
     testImplementation "org.robolectric:robolectric:4.17"
diff --git a/docs/architecture.md b/docs/architecture.md
index 6e7c376..6c36926 100644
--- a/docs/architecture.md
+++ b/docs/architecture.md
@@ -74,7 +74,7 @@ None. Client is stateless except in-memory subscription map and reconnect timer.
 
 ## 5. External Integrations
 
-- `com.github.mokhtarabadi.Parse-SDK-Android:parse:4.4.2` (api) — own fork, JitPack-built; `ParseQuery`, `ParseObject`, `PointerEncoder`.
+- `com.github.mokhtarabadi.Parse-SDK-Android:parse:4.4.3` (api) — own fork, JitPack-built with full module publications; `ParseQuery`, `ParseObject`, `PointerEncoder`.
 - `com.squareup.okhttp3:okhttp:5.4.0` (api) — WebSocket transport (capped: okhttp-android 5.5.0 requires compileSdk 37).
 - Test: `robolectric:4.17`, `junit:4.13.2`, `mockito-core:5.24.0`, `jsonassert:1.5.3`.
```
<!-- END_GIT_DIFF -->
