# Task 03: Document fork divergence from upstream

**File:** `tasks/completed/03-document-fork-divergence.md`
**Source:** manager
**Type:** improvement
**Status:** closed

## Goal

Record the fork-only work versus parse-community upstream in project docs so future merges stay safe.

## Manager's Notes

Fork is 2 commits ahead of upstream with nothing behind: `d5823ce` (build modernization, dep upgrades, transport hardening, fields fix, 3 new test files) and `9d2ef80` (reconnect backoff follow-up). Update `docs/architecture.md` (fork-divergence section), `docs/data_model.md` (connect op gains `clientKey`), and slice `DECISIONS.md` (new ADR). See comparison evidence in the Execution Log.

## Local TODOs

- [ ] Initial codebase exploration
- [ ] Compare origin/master against upstream/master
- [ ] Upgrade architecture, data model, and slice decision docs
- [ ] Verify functionality

## Acceptance Criteria

- [ ] `git log upstream/master..origin/master` output is reflected in docs
- [ ] Connect op documents the `clientKey` field
- [ ] New ADR records build, transport, backoff, and fields decisions

## Verification Evidence

- **Test command:** rtk test ./gradlew :ParseLiveQuery:testDebugUnitTest --no-daemon
- **Expected result:** Unit tests still pass after docs-only change
- **Actual result:** _(The Hands fill this during execution)_
- **Exit code:** _(The Hands fill this during execution)_

> Verification runner rule: `./gradlew :ParseLiveQuery:testDebugUnitTest --no-daemon` is the complete underlying test command. The first verification run MUST use the `rtk test` prefix; record the exact prefixed command above. A raw rerun is allowed only after a failed RTK run for detailed diagnostics.

## Definition of Done

The task is NOT done unless ALL of the following are true (unconditional, applies to every source type):

- [ ] Build/Test/Lint pass with exit code 0
- [ ] `lint_task_file` passes on the active task file
- [ ] `CHANGELOG.md` updated via Parse-Then-Append
- [ ] `verification-before-completion` applied and evidence recorded

## Risk & Rollback

- **Risk:** Docs-only change, no runtime risk.
- **Rollback plan:** Revert doc edits; no code touched.

---

## Execution Log & Reasoning

Comparison run 2026-10-10 (read-only, worktree untouched):
- `git log upstream/master..origin/master` = `9d2ef80`, `d5823ce` (2 fork-only commits).
- `git log origin/master..upstream/master` = empty (fork is not behind).
- Tags identical on both sides, latest `1.2.2`; no `jitpack.yml` on either side.
- Diffstat (9 files, +543/-86): build modernization (AGP 3.5.2 to 9.2.1, SDK 29 to 35, minSdk 15 to 23, jcenter to mavenCentral, legacy coverage plugins removed), dep upgrades (parse 1.24.2 to 4.4.0, okhttp 3.12.10 to 5.4.0, robolectric 3.3.1 to 4.7), `clientKey` in connect op, OkHttp tuning plus hardened `send`, reconnect backoff over two commits, `fields`/`objectId` injection, 3 new test files.

## Factual Git Diff

<!-- BEGIN_GIT_DIFF -->

_(Git diff will be automatically injected here by the MCP tool. Do not edit this block manually)_

<!-- END_GIT_DIFF -->
