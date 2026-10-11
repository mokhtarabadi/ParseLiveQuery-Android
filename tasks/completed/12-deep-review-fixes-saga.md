# Task 12: Deep-review fixes saga

**File:** `tasks/completed/12-deep-review-fixes-saga.md`
**Source:** manager
**Type:** bug
**Status:** closed

## Goal

Apply the deep-review findings on the cumulative fork diff: redact session-token logging, route global errors, break transport lock-order, de-brittle adversarial tests, document infinite reconnect, bump stale CI actions.

## Manager's Notes

Autopilot locked. Brain creates the plan, reviews the pasted cumulative diff first, then Hands fixes everything without further prompts. Mode stays locked until QA move. ZAC holds: no commits, no pushes, no tagging. Closure only on explicit approval words.

## Local TODOs

- [x] Brain review of pasted cumulative diff
- [x] Redact sessionToken from send-path logging
- [x] Route global error events to dispatchServerError
- [x] Break send/onError lock ordering in OkHttp transport
- [x] De-brittle adversarial tests toward behavioral coverage
- [x] Bump stale CI actions plus document reconnect policy
- [x] Upgrade fork Parse SDK 4.4.3 to 4.4.4 and re-verify gate (rolled back: 4.4.4 not on JitPack, kept 4.4.3)
- [x] Verify functionality

## Acceptance Criteria

- [x] No sessionToken content in any log output
- [x] Global error events reach server-error listeners
- [x] Transport error callback fires outside the socket monitor
- [ ] Fork SDK resolves to 4.4.4 from JitPack with gate green (unavailable: kept 4.4.3, resolve failed)
- [x] Full gate green plus spotless green

## Verification Evidence

- **Test command:** rtk test ./gradlew --no-daemon clean :ParseLiveQuery:testDebugUnitTest :ParseLiveQuery:createDebugUnitTestCoverageReport
- **Expected result:** BUILD SUCCESSFUL, all tests green, spotless green
- **Actual result:** BUILD SUCCESSFUL; tests=43 failures=0 errors=0; spotlessCheck BUILD SUCCESSFUL; 4.4.4 resolve failed so SDK kept at 4.4.3
- **Exit code:** 0

> Verification runner rule: `./gradlew :ParseLiveQuery:testDebugUnitTest --no-daemon` is the complete underlying test command. The first verification run MUST use the `rtk test` prefix; record the exact prefixed command above. A raw rerun is allowed only after a failed RTK run for detailed diagnostics.

## Definition of Done

The task is NOT done unless ALL of the following are true (unconditional, applies to every source type):

- [x] Build/Test/Lint pass with exit code 0
- [x] `lint_task_file` passes on the active task file
- [x] `CHANGELOG.md` updated via Parse-Then-Append
- [x] `verification-before-completion` applied and evidence recorded

## Risk & Rollback

- **Risk:** Transport threading change alters error-delivery timing.
- **Rollback plan:** Revert the task commit; suite guards regressions.

---

## Execution Log & Reasoning

Autopilot locked for end-to-end saga. Seat Check: backend/concurrency plus build/CI domains → Senior Programmer plus Software Architect; UI/UX Designer skipped (no user-visible surface), Planner/Strategist/QA/Reviewer skipped (planning gate, not QA stage). Trigger words fired: race, deadlock, silent-fail (Programmer); contract (data_model wire contract). Cross-disciplinary so multi-seat planning call. Brainstorm: not required — fixes are individually reversible behind suite plus ADR updates.

Brain plan verdict (same task_id 12): confirmed F1-F6 with F5 adjustment (jitter required, not optional). Plan steps: 1 redact token in sendOperationAsync, 2 route global errors to dispatchServerError, 3 break transport lock order (invoke callback outside monitor), 4 de-brittle adversarial tests to behavioral, 5 document infinite-retry plus add clock-driven jitter, 6 bump CI majors keeping JDK17 plus fetch-depth 0. Seats: 1-4 Senior Programmer, 3 joint with Architect, 5-6 Architect. Awaiting plan approval gate.

Manager approved with added scope: fork SDK 4.4.3 to 4.4.4. Brain emitted bare implementation XML F1-F7. Implemented: F1 redactForLog masks sessionToken in debug logs; F2 global errors dispatch to server-error listeners plus reconnect; F3 send snapshots state under monitor and invokes onError outside it; F4 TestAdversarialFixes rewritten to 3 behavioral tests (bounds, global-error delivery, capped exponential); F5 capped backoff plus 0-249ms clock jitter plus architecture/data_model/README/ADR-007 updates; F6 CI to checkout v4, cache v4, wrapper-validation v3; F7 SDK 4.4.4 attempted, JitPack resolve failed (Could not GET parse-4.4.4.pom), rolled back to 4.4.3 with docs noting the miss. Assumption A1: 4.4.4 tag simply unpublished, so retry later rather than code change. Verified: rtk full gate BUILD SUCCESSFUL, 43 tests 0 failures, spotlessCheck BUILD SUCCESSFUL. Test count 43 vs 45 reflects the 5-to-3 adversarial rewrite.

Reviewer verdict (Code Reviewer, same task_id 12): APPROVED → PO_REVIEW_PENDING. Technical approval with low-severity notes (backoff math-mirror test, empty-catch style, Math.abs on modulus). File stays in tasks/qa/ pending explicit closure words.

Closure: Manager accept quote "Approved for closure". Reviewer PO_REVIEW_PENDING accepted. Header set to completed and closed. Zero code changes in closure turn.

## Factual Git Diff

<!-- BEGIN_GIT_DIFF -->
**Factual Git Diff:** Stored in Commit Hash: `d534b367b33bbe6cb06053cf71358d6599e0516d`
<!-- END_GIT_DIFF -->
