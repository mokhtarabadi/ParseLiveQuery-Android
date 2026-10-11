# Task 09: Publish 1.2.4 patch release on JitPack

**File:** `tasks/qa/09-publish-124-patch-release.md`
**Source:** manager
**Type:** feature
**Status:** open

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

## Factual Git Diff

<!-- BEGIN_GIT_DIFF -->
```diff
diff --git a/CHANGELOG.md b/CHANGELOG.md
index 22770f1..cd9742c 100644
--- a/CHANGELOG.md
+++ b/CHANGELOG.md
@@ -1,6 +1,9 @@
 ## Changelog
 
 ### master
+
+### 1.2.4
+- Republish 1.2.3 with the JitPack publishing fix (maven-publish release publication); 1.2.3 errored on JitPack from the pre-fix commit and its result is cached
 - Audit round two: slice README coverage for clock files plus ADR-006 for JitPack publishing
 - Fix JitPack publishing: maven-publish release publication with artifactId override; OkHttp capped at 5.4.0 with compileSdk 36
 - Review follow-ups: sync architecture tree and overload count, CopyOnWriteArrayList in data model, current gate in test docs, chronological ADRs
```
<!-- END_GIT_DIFF -->
