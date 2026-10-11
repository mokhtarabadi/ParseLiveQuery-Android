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
**Factual Git Diff:** Stored in Commit Hash: `f3ae0e235468fde71947c115672c2a1a2fa6de9a`
<!-- END_GIT_DIFF -->
