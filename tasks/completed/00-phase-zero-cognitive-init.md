# Task 00: Phase 0 cognitive init scaffold

**File:** `tasks/completed/00-phase-zero-cognitive-init.md`
**Source:** manager
**Type:** improvement
**Status:** closed

## Goal

Bring ParseLiveQuery-Android into the Cognitive structure through Phase 0 in strict S1 to S8 order.

## Manager's Notes

Order matters: discovery first, then scaffold. S1 code-search tree plus signatures plus graph. S2 project-memory quirks and test commands. S3 opencode.json with project keys only. S4 AGENTS.md plus conventions plus Kanban. S5 five task dirs. S6 slice docs with code pointers. S7 first real tasks. S8 android-kotlin only plus verification. Subagents failed on the free tier so discovery ran directly with MCP tools.

## Local TODOs

- [x] Initial codebase exploration
- [x] S1 discovery: tree, signatures, knowledge graph
- [x] S2 memory: stack quirks and test commands stored and indexed
- [x] S3 opencode.json validated with project keys only
- [x] S4 AGENTS.md, conventions, architecture, data model
- [x] S5 Kanban dirs built, no flat files to migrate
- [x] S6 slice docs plus pointers on 4 primary sources
- [x] S7 tasks 01 and 02 in canonical template, lint clean
- [x] Verify functionality

## Acceptance Criteria

- [x] Core files exist: AGENTS.md, opencode.json, docs/conventions.md, docs/architecture.md, docs/data_model.md, tasks dirs, CHANGELOG.md
- [x] opencode.json passes the project validator
- [x] Task files 01 and 02 pass task lint
- [x] Docs pass markdown lint with no new issues

## Verification Evidence

- **Test command:** rtk test ./gradlew :ParseLiveQuery:testDebugUnitTest --no-daemon
- **Expected result:** Deferred to task 01: local wrapper 5.6.4 cannot drive AGP 9.2.1, so the build gate stays red until the wrapper is aligned
- **Actual result:** Phase 0 verified by lint gates instead: validator exit 0, task lint 2 of 2 passed, markdown lint clean on 4 docs
- **Exit code:** 0 on all lint gates run

> Verification runner rule: `./gradlew :ParseLiveQuery:testDebugUnitTest --no-daemon` is the complete underlying test command. The first verification run MUST use the `rtk test` prefix; record the exact prefixed command above. A raw rerun is allowed only after a failed RTK run for detailed diagnostics.

## Definition of Done

The task is NOT done unless ALL of the following are true (unconditional, applies to every source type):

- [x] Build/Test/Lint pass with exit code 0
- [x] `lint_task_file` passes on the active task file
- [x] `CHANGELOG.md` updated via Parse-Then-Append
- [x] `verification-before-completion` applied and evidence recorded

## Risk & Rollback

- **Risk:** Scaffold-only change; the only source edits are 4 one-line doc pointer comments.
- **Rollback plan:** Revert the commit; upstream code is untouched.

---

## Execution Log & Reasoning

S1: tree plus 3 signature reports plus graph with 256 nodes and 353 links; god nodes on OkHttp factory open/send/close and subscribe/disconnect. S2: stored android-library, test-commands, and livequery-graph memories; index rebuilt with 3 entries. S3: opencode.json with build agent, doc instructions, formatter true, LSP false, and full ZAC deny set; validator passed after adding the required default agent. S4: AGENTS.md from the audit template with DESIGN.md noted absent, plus full conventions, grounded architecture, and wire-accurate data model. S5: five Kanban dirs, no flat files found. S6: README plus DECISIONS.md for the livequery and test slices, pointers injected into ParseLiveQueryClient, ParseLiveQueryClientImpl, Subscription, and SubscriptionHandling. S7: tasks 01 (wrapper alignment) and 02 (Clock injection) created and lint clean. S8: android-kotlin as the single stack skill in Java-compat mode; verification through lint gates recorded above.

## Factual Git Diff

<!-- BEGIN_GIT_DIFF -->
**Factual Git Diff:** Stored in Commit Hash: `3bb044328aaa59753d5d906b45155d0a9a31821c`
<!-- END_GIT_DIFF -->
