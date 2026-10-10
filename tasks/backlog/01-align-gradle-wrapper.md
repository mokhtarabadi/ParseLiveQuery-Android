# Task 01: Align Gradle wrapper with AGP and CI

**File:** `tasks/backlog/01-align-gradle-wrapper.md`
**Source:** manager
**Type:** improvement
**Status:** open

## Goal

Align the Gradle wrapper with the declared AGP version so local builds and CI use the same toolchain.

## Manager's Notes

Root declares AGP 9.2.1 but `gradle-wrapper.properties` pins Gradle 5.6.4; CI runs JDK 11. Decide: upgrade wrapper to 8.x (preferred) or pin AGP down. Keep `spotlessCheck` and `clean jacocoTestReport` green. See `docs/architecture.md` section 6.

## Local TODOs

- [ ] Initial codebase exploration
- [ ] Reproduce wrapper vs AGP mismatch locally
- [ ] Upgrade wrapper (or pin AGP) + update CI JDK if needed
- [ ] Verify functionality

## Acceptance Criteria

- [ ] `./gradlew --version` reports a Gradle compatible with AGP 9.2.1
- [ ] `./gradlew spotlessCheck --no-daemon` passes
- [ ] `./gradlew --no-daemon clean jacocoTestReport` passes

## Verification Evidence

- **Test command:** rtk test ./gradlew --no-daemon clean jacocoTestReport
- **Expected result:** BUILD SUCCESSFUL, tests pass, coverage report generated
- **Actual result:** _(The Hands fill this during execution)_
- **Exit code:** _(The Hands fill this during execution)_

> Verification runner rule: `./gradlew --no-daemon clean jacocoTestReport` is the complete underlying test command. The first verification run MUST use the `rtk test` prefix; record the exact prefixed command above. A raw rerun is allowed only after a failed RTK run for detailed diagnostics.

## Definition of Done

The task is NOT done unless ALL of the following are true (unconditional, applies to every source type):

- [ ] Build/Test/Lint pass with exit code 0
- [ ] `lint_task_file` passes on the active task file
- [ ] `CHANGELOG.md` updated via Parse-Then-Append
- [ ] `verification-before-completion` applied and evidence recorded

## Risk & Rollback

- **Risk:** Wrapper upgrade breaks CI cache keys or requires newer JDK.
- **Rollback plan:** Revert `gradle-wrapper.properties` + CI file; re-run fast loop.

---

## Execution Log & Reasoning

_(The Hands: Manually log your technical changes, file edits, and architectural reasoning here BEFORE calling the MCP tool)_

## Factual Git Diff

<!-- BEGIN_GIT_DIFF -->

_(Git diff will be automatically injected here by the MCP tool. Do not edit this block manually)_

<!-- END_GIT_DIFF -->
