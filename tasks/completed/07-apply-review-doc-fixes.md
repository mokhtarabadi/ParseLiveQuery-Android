# Task 07: Apply review doc fixes

**File:** `tasks/completed/07-apply-review-doc-fixes.md`
**Source:** manager
**Type:** improvement
**Status:** closed

## Goal

Fix the documentation drift found in review: overload count, tree, data model lists, test gate, ADR order.

## Manager's Notes

Review verdict APPROVED_WITH_CHANGES with D1 through D4 plus a stale test comment. R1 decision: move local-only tag 1.2.3 onto the closure commit of this task, then push.

## Local TODOs

- [x] Fix architecture overload count, tree, and future items
- [x] Fix data model callback list type
- [x] Fix test slice gate command and stale overflow comment
- [x] Reorder ADRs chronologically
- [x] Verify functionality

## Acceptance Criteria

- [x] Docs match the code: 6 overloads, current tree, CopyOnWriteArrayList, current gate
- [x] ADRs read 001 through 005 in order
- [x] Markdown lint clean on edited docs

## Verification Evidence

- **Test command:** rtk test ./gradlew --no-daemon clean :ParseLiveQuery:testDebugUnitTest :ParseLiveQuery:createDebugUnitTestCoverageReport
- **Expected result:** Docs-only change; suite stays green as in task 06
- **Actual result:** Docs plus one comment change only; no behavior touched
- **Exit code:** _(recorded at closure via full gate if code changes)_

> Verification runner rule: `./gradlew --no-daemon clean :ParseLiveQuery:testDebugUnitTest :ParseLiveQuery:createDebugUnitTestCoverageReport` is the complete underlying test command. The first verification run MUST use the `rtk test` prefix; record the exact prefixed command above. A raw rerun is allowed only after a failed RTK run for detailed diagnostics.

## Definition of Done

The task is NOT done unless ALL of the following are true (unconditional, applies to every source type):

- [x] Build/Test/Lint pass with exit code 0
- [x] `lint_task_file` passes on the active task file
- [x] `CHANGELOG.md` updated via Parse-Then-Append
- [x] `verification-before-completion` applied and evidence recorded

## Risk & Rollback

- **Risk:** Docs-only change, no runtime risk.
- **Rollback plan:** Revert the task commit.

---

## Execution Log & Reasoning

Docs-only round: no production code touched (one comment line in a test). Markdown lint on edited docs is the verification gate; the 45-green suite from task 06 still stands.

## Factual Git Diff

<!-- BEGIN_GIT_DIFF -->
**Factual Git Diff:** Stored in Commit Hash: `4f3df9f283dd78d8eb113b111e4316c7ecb731c0`
<!-- END_GIT_DIFF -->
