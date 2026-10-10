# Task 02: Inject Clock for reconnect backoff

**File:** `tasks/archive/02-inject-clock-reconnect-backoff.md`
**Source:** manager
**Type:** improvement
**Status:** superseded
**Superseded-By:** `05-release-readiness-bundle`
**Superseded-At:** `2026-10-10`

## Goal

Replace direct time reads in reconnect backoff with an injectable Clock for deterministic tests and datetime-standard compliance.

## Manager's Notes

`ParseLiveQueryClientImpl.scheduleReconnect`/`cancelPendingReconnect` drive reconnect timing; see `docs/conventions.md` Clock Injection rule and `TestReconnectBackoff`. Keep `OkHttp3SocketClientFactory` transport untouched. Update slice docs if the backoff contract changes.

## Local TODOs

- [ ] Initial codebase exploration
- [ ] Locate all direct time reads on the reconnect path
- [ ] Introduce Clock abstraction + inject via Factory/getClient overload
- [ ] Verify functionality

## Acceptance Criteria

- [ ] No direct `new Date()`/`System.currentTimeMillis()` remains in reconnect path
- [ ] `TestReconnectBackoff` passes deterministically with a fake Clock
- [ ] Public API stays backward compatible (new overloads only, no signature breaks)

## Verification Evidence

- **Test command:** rtk test ./gradlew :ParseLiveQuery:testDebugUnitTest --no-daemon
- **Expected result:** All unit tests pass including reconnect backoff tests
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

- **Risk:** Clock plumbing changes threading assumptions on connect/disconnect executor.
- **Rollback plan:** Revert to system clock; keep fake-Clock tests skipped.

---

> **Superseded:** This task was bundled into META task `05-release-readiness-bundle` and archived on 2026-10-10. See `tasks/backlog/05-release-readiness-bundle.md` (or its Kanban successor) for the unified execution. History preserved via `git log --follow -- tasks/archive/02-inject-clock-reconnect-backoff.md`.

## Execution Log & Reasoning

_(The Hands: Manually log your technical changes, file edits, and architectural reasoning here BEFORE calling the MCP tool)_

## Factual Git Diff

<!-- BEGIN_GIT_DIFF -->

_(Git diff will be automatically injected here by the MCP tool. Do not edit this block manually)_

<!-- END_GIT_DIFF -->
