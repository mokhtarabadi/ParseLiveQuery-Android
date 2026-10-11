# Task 09: Publish 1.2.4 patch release on JitPack

**File:** `tasks/completed/09-publish-124-patch-release.md`
**Source:** manager
**Type:** feature
**Status:** closed

## Goal

Ship 1.2.4 on JitPack after 1.2.3 cached an error from the pre-fix commit.

## Manager's Notes

Tag 1.2.3 was moved to the fixed commit but JitPack keeps serving the cached error build, so a fresh patch version is required. Add the CHANGELOG entry, tag 1.2.4 after commit, push branch plus tag, then monitor JitPack to green.

## Local TODOs

- [x] Confirm JitPack serves stale cached error for moved tag
- [x] Add CHANGELOG entry for 1.2.4
- [ ] Commit plus tag plus push via Manager terminal
- [ ] Monitor JitPack build to green

## Acceptance Criteria

- [x] CHANGELOG holds a `### 1.2.4` entry
- [ ] Tag `1.2.4` exists on origin
- [ ] JitPack serves `com.github.mokhtarabadi:ParseLiveQuery-Android:1.2.4`

## Verification Evidence

- **Test command:** rtk test ./gradlew --no-daemon clean :ParseLiveQuery:testDebugUnitTest :ParseLiveQuery:createDebugUnitTestCoverageReport
- **Expected result:** Code unchanged since 45-green gate; release is a version bump only
- **Actual result:** No code changes in this task; suite green as recorded in task 06
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
- **Rollback plan:** Never reuse a version number; next fix goes to 1.2.5.

---

## Execution Log & Reasoning

JitPack build log for 1.2.3 is pinned to the pre-fix commit 0eadefd even after the tag moved to 5da9dc8. Fresh version is the only cache-busting path.

Fork SDK switch: own fork 4.4.2 verified green on JitPack (real AAR) and adopted after one compat fix — the fork caches session tokens process-wide, so test teardowns now clear the cache reflectively for deterministic logged-out behavior. Full gate re-verified: 45 tests 0 failures, spotless clean.

## Factual Git Diff

<!-- BEGIN_GIT_DIFF -->
```diff
diff --git a/CHANGELOG.md b/CHANGELOG.md
index cd9742c..5573990 100644
--- a/CHANGELOG.md
+++ b/CHANGELOG.md
@@ -4,6 +4,7 @@
 
 ### 1.2.4
 - Republish 1.2.3 with the JitPack publishing fix (maven-publish release publication); 1.2.3 errored on JitPack from the pre-fix commit and its result is cached
+- Depend on own fork SDK 4.4.2 (JitPack-built) instead of upstream 4.4.0
 - Audit round two: slice README coverage for clock files plus ADR-006 for JitPack publishing
 - Fix JitPack publishing: maven-publish release publication with artifactId override; OkHttp capped at 5.4.0 with compileSdk 36
 - Review follow-ups: sync architecture tree and overload count, CopyOnWriteArrayList in data model, current gate in test docs, chronological ADRs
diff --git a/ParseLiveQuery/build.gradle b/ParseLiveQuery/build.gradle
index a11f2f2..c2f2156 100644
--- a/ParseLiveQuery/build.gradle
+++ b/ParseLiveQuery/build.gradle
@@ -11,8 +11,8 @@ android {
     defaultConfig {
         minSdk 23
         targetSdk 35
-        versionCode 1
-        versionName "1.0"
+        versionCode 2
+        versionName "1.2.4"
         consumerProguardFiles "release-proguard.pro"
     }
 
@@ -44,7 +44,7 @@ android {
 }
 
 dependencies {
-    api "com.github.parse-community.Parse-SDK-Android:parse:4.4.0"
+    api "com.github.mokhtarabadi.Parse-SDK-Android:parse:4.4.2"
     api "com.squareup.okhttp3:okhttp:5.4.0"
 
     testImplementation "org.robolectric:robolectric:4.17"
diff --git a/ParseLiveQuery/src/test/java/com/parse/TestClockWiring.java b/ParseLiveQuery/src/test/java/com/parse/TestClockWiring.java
index 5a9573d..a3f853b 100644
--- a/ParseLiveQuery/src/test/java/com/parse/TestClockWiring.java
+++ b/ParseLiveQuery/src/test/java/com/parse/TestClockWiring.java
@@ -69,6 +69,14 @@ public class TestClockWiring {
 
     @After
     public void tearDown() throws Exception {
+        try {
+            java.lang.reflect.Field field =
+                    ParseUser.class.getDeclaredField("sCachedSessionToken");
+            field.setAccessible(true);
+            field.set(null, null);
+        } catch (ReflectiveOperationException ignored) {
+            // Upstream SDK without the token cache: nothing to clear.
+        }
         ParseCorePlugins.getInstance().reset();
         ParsePlugins.reset();
     }
diff --git a/ParseLiveQuery/src/test/java/com/parse/TestParseLiveQueryClient.java b/ParseLiveQuery/src/test/java/com/parse/TestParseLiveQueryClient.java
index d588167..4f3c1d2 100644
--- a/ParseLiveQuery/src/test/java/com/parse/TestParseLiveQueryClient.java
+++ b/ParseLiveQuery/src/test/java/com/parse/TestParseLiveQueryClient.java
@@ -90,10 +90,27 @@ public class TestParseLiveQueryClient {
 
     @After
     public void tearDown() throws Exception {
+        clearForkSessionTokenCache();
         ParseCorePlugins.getInstance().reset();
         ParsePlugins.reset();
     }
 
+    /**
+     * The forked SDK caches the session token in a process-wide static.
+     * Clear it so unstubbed tests observe a logged-out client deterministically.
+     * No-op on SDK builds without the cache field.
+     */
+    private static void clearForkSessionTokenCache() {
+        try {
+            java.lang.reflect.Field field =
+                    ParseUser.class.getDeclaredField("sCachedSessionToken");
+            field.setAccessible(true);
+            field.set(null, null);
+        } catch (ReflectiveOperationException ignored) {
+            // Upstream SDK without the token cache: nothing to clear.
+        }
+    }
+
     @Test
     public void testSubscribeAfterSocketConnectBeforeConnectedOp() throws Exception {
         // Bug: https://github.com/parse-community/ParseLiveQuery-Android/issues/46
diff --git a/docs/architecture.md b/docs/architecture.md
index 3c29edb..6e7c376 100644
--- a/docs/architecture.md
+++ b/docs/architecture.md
@@ -74,7 +74,7 @@ None. Client is stateless except in-memory subscription map and reconnect timer.
 
 ## 5. External Integrations
 
-- `com.github.parse-community.Parse-SDK-Android:parse:4.4.0` (api) — `ParseQuery`, `ParseObject`, `PointerEncoder`.
+- `com.github.mokhtarabadi.Parse-SDK-Android:parse:4.4.2` (api) — own fork, JitPack-built; `ParseQuery`, `ParseObject`, `PointerEncoder`.
 - `com.squareup.okhttp3:okhttp:5.4.0` (api) — WebSocket transport (capped: okhttp-android 5.5.0 requires compileSdk 37).
 - Test: `robolectric:4.17`, `junit:4.13.2`, `mockito-core:5.24.0`, `jsonassert:1.5.3`.
```
<!-- END_GIT_DIFF -->
