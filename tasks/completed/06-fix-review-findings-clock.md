# Task 06: Fix brain review findings and complete clock work

**File:** `tasks/completed/06-fix-review-findings-clock.md`
**Source:** manager
**Type:** bug
**Status:** closed

## Goal

Fix remaining bugs found by Brain review and complete the clock injection work.

## Manager's Notes

Autopilot round ordered by Manager after the release bundle: rebuild graph, review with Brain, fix bugs, complete the clock part. QA findings from prior rounds are fixed and verified (43 green); this task covers whatever the fresh review still reports.

## Local TODOs

- [x] Initial codebase exploration
- [x] Brain review with fresh graph
- [x] Fix reported bugs and complete clock work
- [x] Verify functionality

## Acceptance Criteria

- [x] Brain review reports no blocking findings
- [x] Full gate green: unit tests plus coverage plus spotless
- [x] No direct clock reads remain in main sources

## Verification Evidence

- **Test command:** rtk test ./gradlew --no-daemon clean :ParseLiveQuery:testDebugUnitTest :ParseLiveQuery:createDebugUnitTestCoverageReport
- **Expected result:** BUILD SUCCESSFUL, all tests green, spotless green
- **Actual result:** BUILD SUCCESSFUL; tests=43 failures=0 errors=0; spotlessCheck exit 0; coverage report generated
- **Exit code:** 0

> Verification runner rule: `./gradlew --no-daemon clean :ParseLiveQuery:testDebugUnitTest :ParseLiveQuery:createDebugUnitTestCoverageReport` is the complete underlying test command. The first verification run MUST use the `rtk test` prefix; record the exact prefixed command above. A raw rerun is allowed only after a failed RTK run for detailed diagnostics.

## Definition of Done

The task is NOT done unless ALL of the following are true (unconditional, applies to every source type):

- [x] Build/Test/Lint pass with exit code 0
- [x] `lint_task_file` passes on the active task file
- [x] `CHANGELOG.md` updated via Parse-Then-Append
- [x] `verification-before-completion` applied and evidence recorded

## Risk & Rollback

- **Risk:** Small scoped fixes; full suite guards regressions.
- **Rollback plan:** Revert the task commit.

---

## Execution Log & Reasoning

Autopilot locked 2026-10-10 for this fix round. Graph rebuilt: 434 nodes, 758 links.

Brain QA_REJECTED with F1-F8 plus clock-complete confirmation. All fixed: F1 dropped the userInitiatedDisconnect reset in dispatchSocketError; F2 mCallbacks to CopyOnWriteArrayList; F3 callback monitors unified on the Impl instance; F4 onFailure sets DISCONNECTED first; F5 send failures fault the Task instead of vanishing; F6 token-fetch failures route to didEncounter plus dispatchServerError plus reconnect; F7 subscription callback lists to CopyOnWriteArrayList; F8 error event uses opt getters with early return. Re-verified: rtk full gate exit 0, 43 tests 0 failures, spotless exit 0.

Dep upgrade round (Brain plan S1-S5, verified stepwise): jsonassert 1.5.3, OkHttp 5.5.0, Mockito 5.24.0, Robolectric 4.17 with @Config sdk 28, AGP 9.4.1. Kept parse 4.4.0 and JUnit 4.13.2 (both latest/final). Final gate: rtk exit 0, 43 tests 0 failures, spotless exit 0.

Round 2 duo review (F1-F10, all fixed): synchronized handshake writes; checked send errors always reported; server reconnect flag honored; transport send fails loudly with rethrow; volatile reconnect state cleared under monitor; generation counter drops stale-socket callbacks (reconnect only — disconnect keeps current-socket delivery); per-listener guards on all dispatch paths; connectIfNeeded synchronized; clock drives postDelayed/removeCallbacks with deterministic and stale-callback tests. Final gate: rtk exit 0, 45 tests 0 failures, spotless exit 0.

## Factual Git Diff

<!-- BEGIN_GIT_DIFF -->
**Factual Git Diff:** Stored in Commit Hash: `987dad74cc64c77459ec2fd81fb009feac55319d`
<!-- END_GIT_DIFF -->
