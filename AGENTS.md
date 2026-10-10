# ParseLiveQuery-Android — Project Context Hub

## Project Overview

Parse LiveQuery Client for Android. Java Android library (`ParseLiveQuery` module, namespace `com.parse.livequery`) that subscribes to `ParseQuery` over WebSocket (OkHttp3) and dispatches realtime events (CREATE/UPDATE/ENTER/LEAVE/DELETE). 100% Java, no Kotlin/Compose. Core classes: `ParseLiveQueryClient` (interface + Factory), `ParseLiveQueryClientImpl`, `Subscription`/`SubscriptionHandling`, `ClientOperation` family, `WebSocketClient` + `OkHttp3SocketClientFactory`. Tests: Robolectric + JUnit4 + Mockito under `ParseLiveQuery/src/test`.

## Setup & Dev Commands

- Build: `./gradlew :ParseLiveQuery:assembleDebug --no-daemon` (full gate: `./gradlew --no-daemon clean jacocoTestReport`)
- Test: `./gradlew :ParseLiveQuery:testDebugUnitTest --no-daemon` (fast loop); CI gate: `./gradlew --no-daemon clean jacocoTestReport` (JDK 11)
- Lint: `./gradlew spotlessCheck --no-daemon`
- Dev: N/A (library — no dev server; verify via unit tests)

## Actionable Guardrails (Do's & Don'ts)

- **Don't** bypass the reconnect backoff (`scheduleReconnect`/`cancelPendingReconnect`) or call `new Date()`/direct clocks in business logic.
  -> **Do** route time via injectable `Clock` and keep reconnect discipline (see `docs/conventions.md`).
- **Don't** mutate subscription state without thread-safe handling (`ConcurrentHashMap`-backed subscriptions).
  -> **Do** keep subscribe/unsubscribe paths synchronized as in `ParseLiveQueryClientImpl`.
- **Don't** read `context-reports/` markdown files yourself.
  -> **Do** generate them using the MCP server — context reports via `custom_context_read_source_files`, tree reports via `custom_context_create_tree_report` ("create a tree of the project") — and hand the file path to the Manager.
- **Don't** execute Git commands like `git add`, `git commit`, or `git mv` autonomously or try to guess when to stage code.
  -> **Do** execute Git commands ONLY when explicitly instructed by an Orchestrator task block. Otherwise, rely on the `custom_context_stage_and_inject_diff` MCP tool.
  -> **Exception:** `git mv` is permitted autonomously for moving task files between Kanban directories.
- **Don't** guess blindly when facing complex bugs, deadlocks, or silent timeouts.
  -> **Do** utilize the `debug-instrumentation` skill to inject strategic logs and trace the runtime execution path.
- **Don't** write bash scripts without strict mode or mask errors with `2>/dev/null` on data commands.
  -> **Do** follow the Defensive Shell Protocol: `set -euo pipefail`, ban error masking, sidecar isolation for Docker backups. See `docs/conventions.md`.
- **Don't** perform financial mutations without snapshotting the prior state or allow nulls in monetary aggregations.
  -> **Do** follow the Universal Financial Ledger Standard: snapshot-on-write, `$ifNull` precedence, discrepancy alerting, deep config merging. See `docs/conventions.md`.
- **Don't** leave task numbers in visible prompt prose, section headings, or skill instructions.
  -> **Do** keep task-number references in code comments, CHANGELOG entries, task files, history archives, and HTML comments only. See `docs/conventions.md`.
- **Don't** carry over assumptions, partial results, or architectural hypotheses from a previous task.
  -> **Do** flush context and treat every task as contextually independent (Buffer Isolation directive in validation-phase).
- **Don't** execute raw, informal, or non-English prompts directly.
  -> **Do** load the `prompt-refactor` skill to translate and expand the intent into an elite English spec first. (Note: If you receive a standard XML task block, skip this and execute normally).
- **Don't** attempt to resolve cross-disciplinary ambiguity within a single persona.
  -> **Do** trigger the Multi-Agent Brainstorming Loop if the Manager explicitly requests brainstorming or a task exhibits cross-disciplinary ambiguity. Interpret the `<brainstorming_session>` results in backlog tasks as non-functional guidelines that govern execution.
- **Don't** silently alter slice architecture or domain invariants without updating the sibling README.md and appending a dated ADR to DECISIONS.md.
  -> **Do** inspect sibling README.md and DECISIONS.md before modifying code, and keep them in sync with implementation changes.

## Documentation Sync Rules

When modifying this repository, you must keep these files synchronized:

1. Active task file in `tasks/` (single source of truth for current work items)
2. `CHANGELOG.md` (Keep a Changelog format)
3. `DESIGN.md` (absent — library with no UI; skipped per Absent-File Policy)
4. `docs/conventions.md` (syntax rules, datetime standard, SOLID guidelines)
5. Relevant `SKILL.md` files (if structural patterns were altered)

## 🛑 GATEKEEPER VALIDATION (HALT PROTOCOL)

You (the Hands) are the final gatekeeper. Before executing any implementation task, you MUST evaluate the Orchestrator's instructions against this file and any referenced specs (`docs/architecture.md`, `docs/data_model.md`, etc.). If the instructions violate project rules, ignore them. HALT immediately and output a `⚠️ RULE VIOLATION WARNING` back to the Manager explaining exactly what the Orchestrator got wrong, forcing it to self-correct.

## 🛑 CORE FILE LOCATIONS

You MUST strictly adhere to these exact paths. Do not create duplicates elsewhere:

- **Global Rules:** `AGENTS.md` (Root)
- **UI/UX Specs:** `DESIGN.md` (absent — no UI surface in this library; skipped gracefully)
- **Agent Skills:** `.opencode/skills/<skill-name>/SKILL.md` (Local workspace — optional; only include if project utilizes OpenCode)
- **Active Tasks:** `tasks/backlog/<task-number>-<name>.md` (backlog), `tasks/in-progress/`, `tasks/qa/`, `tasks/completed/`, `tasks/archive/`
- **Conventions:** `docs/conventions.md`
- **Architecture:** `docs/architecture.md`
- **Data Model:** `docs/data_model.md`

## 🛑 SKILL LOADING RULES

You MUST follow these skill loading rules in every session:

- **Task-Generator Skill:** Before creating any new task file, you MUST load the `task-generator` skill using the `skill` tool to ensure the correct template format with `<!-- BEGIN_GIT_DIFF -->` / `<!-- END_GIT_DIFF -->` markers.
- **Project Skills:** Before implementing any task, you MUST load every available skill matching the project's tech stack (e.g., `android-kotlin`, `spring-boot`, `react-vite`). If a relevant skill exists, it MUST be loaded — this enforces framework-specific conventions and architectural rules. For this repo: `android-kotlin` (Java-compat mode, no Compose).

## 🛑 CONTEXT BOOTSTRAPPING

At the start of every task, you MUST call `search_memory` or `list_namespaces` to load any hidden project quirks relevant to your domain before implementing.

## 🛑 MANDATORY END-OF-TASK SEQUENCE

When finishing a task, you MUST execute these exact steps in order:

1. **Update Changelog:** You MUST insert a formal entry into CHANGELOG.md logging your modifications.
2. **Write your Summary:** Manually write your architectural reasoning, local TODO checks, and execution notes into the active `tasks/XX-task.md` file under "Execution Log & Reasoning".
3. **Call MCP Tool & QA Transition:** Call the `custom_context_stage_and_inject_diff` MCP tool. After injection, you MUST move the task file to `tasks/qa/` via `git mv` before notifying the Manager (implementation tasks only — discovery tasks stay in place). DO NOT execute any `git commit` commands. Closure to `tasks/completed/` happens ONLY after the Manager explicitly says "Approved for closure" or "Close task".
4. **Kanban Metadata Synchronization (mandatory after ANY authorized `git mv`):** After the move, update the task file's `**File:**` metadata header to the new path. If the move happened AFTER staging, re-run `lint_task_file` and call `custom_context_stage_and_inject_diff` AGAIN with the NEW task path and the full `modified_files` array before notifying the Manager — the re-stage keeps the injected diff and staging state in sync with the final path. Never notify the Manager with a stale `**File:**` header.
5. **Notify Manager:** Output exactly: "Task ready. Manager, please copy the contents of `tasks/XX-task.md` and send it back to the Orchestrator Brain for review."

## 🛑 LITE MODE PROTOCOL (`<lite_mode_protocol>`)

When eligible (single-file, no security/financial impact, obvious simplicity), the full 9-step production line can be bypassed with a `[LITE]` justification in the task's `## Execution Log & Reasoning` section. Escalation to Full Mode is mandatory if hidden complexity is discovered. Lite-eligible changes still run Seat Check + verification; login/auth, money, and security-surface changes are never Lite.

## 🛑 BUFFER ISOLATION

Treat every task as contextually independent: flush working execution assumptions between tasks; do not carry over hypotheses or partial results. Session threads persist under the active session ID until sprint completion — flushing covers working assumptions only.

## Living Folder Docs

Before editing code inside any component directory, inspect sibling `README.md` and `DECISIONS.md`; update them in the same task when contracts or decisions change. Primary source files carry a top-line sibling-docs pointer.
