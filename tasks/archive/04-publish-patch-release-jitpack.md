# Task 04: Publish patch release on JitPack

**File:** `tasks/archive/04-publish-patch-release-jitpack.md`
**Source:** manager
**Type:** feature
**Status:** superseded
**Superseded-By:** `05-release-readiness-bundle`
**Superseded-At:** `2026-10-10`

## Goal

Ship the fork fixes as a patch release consumable via JitPack.

## Manager's Notes

JitPack setup already exists (README coordinates `com.github.parse-community:ParseLiveQuery-Android`, no `jitpack.yml` needed). Bump patch: add `### 1.2.3` CHANGELOG entry describing the fork fixes, create tag `1.2.3` on master, push tag to origin, then request the JitPack build for `com.github.mokhtarabadi:ParseLiveQuery-Android:1.2.3`. Tag plus push is a public release — get explicit Manager approval before tagging.

## Local TODOs

- [ ] Initial codebase exploration
- [ ] Add CHANGELOG entry for the patch release
- [ ] Create and push tag after explicit approval
- [ ] Verify functionality

## Acceptance Criteria

- [ ] CHANGELOG holds a `### 1.2.3` entry covering the fork fixes
- [ ] Tag `1.2.3` exists on origin master
- [ ] JitPack serves `com.github.mokhtarabadi:ParseLiveQuery-Android:1.2.3`

## Verification Evidence

- **Test command:** rtk test ./gradlew --no-daemon clean jacocoTestReport
- **Expected result:** BUILD SUCCESSFUL before tagging
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

- **Risk:** Public tag triggers a public JitPack artifact; a bad tag pollutes the version line.
- **Rollback plan:** Delete the tag locally and on origin before announcing; never reuse the number.

---

> **Superseded:** This task was bundled into META task `05-release-readiness-bundle` and archived on 2026-10-10. See `tasks/backlog/05-release-readiness-bundle.md` (or its Kanban successor) for the unified execution. History preserved via `git log --follow -- tasks/archive/04-publish-patch-release-jitpack.md`.

## Execution Log & Reasoning

_(The Hands: Manually log your technical changes, file edits, and architectural reasoning here BEFORE calling the MCP tool)_

## Factual Git Diff

<!-- BEGIN_GIT_DIFF -->

_(Git diff will be automatically injected here by the MCP tool. Do not edit this block manually)_

<!-- END_GIT_DIFF -->
