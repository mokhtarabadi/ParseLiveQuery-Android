# Task 10: Safe dependency upgrade round

**File:** `tasks/completed/10-safe-dependency-upgrade.md`
**Source:** manager
**Type:** improvement
**Status:** closed

## Goal

Upgrade dependencies only where safe: available, compatible, and green.

## Manager's Notes

No Brain needed. Full completion ordered: create, implement, verify, stage, close. Keep compileSdk and OkHttp paired (okhttp-android metadata enforces it); do not break JitPack compatibility.

## Local TODOs

- [x] Check latest versions of all direct deps plus toolchain
- [x] Apply safe upgrades only
- [x] Verify full gate plus publish dry-run
- [x] Verify functionality

## Acceptance Criteria

- [x] Every direct dep resolved from an available repository
- [x] Full gate green: 45 tests plus spotless plus local publish
- [x] No downgrade or pinned workaround left behind

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

- **Risk:** Newer dep breaks API compat or JitPack image coverage.
- **Rollback plan:** Revert the dep bump; suite guards regressions.

---

## Execution Log & Reasoning

Checked upstream for every direct dep and the toolchain: AGP 9.4.1, Gradle 9.8.1, Mockito 5.24.0, Robolectric 4.17, fork SDK 4.4.2, jsonassert 1.5.3, JUnit 4.13.2 final — all already latest, so no bump was safe to make. OkHttp 5.5.0 deliberately skipped: its android artifact requires compileSdk 37 and JitPack image coverage is unverifiable. All repos (google, central, JitPack) resolve. Repos clean: no jcenter remnants.

## Factual Git Diff

<!-- BEGIN_GIT_DIFF -->

_(Git diff will be automatically injected here by the MCP tool. Do not edit this block manually)_

<!-- END_GIT_DIFF -->
