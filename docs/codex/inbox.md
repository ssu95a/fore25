# Codex mailbox inbox

## Protocol

- Mailbox branch: `agent/codex-mailbox`.
- Working implementation branch: `migration/java25-correctness`.
- Do not implement work on the mailbox branch.
- Read this file after `git fetch origin`.
- For a reply, update `docs/codex/outbox.md` on the mailbox branch.
- Keep production commits on the working implementation branch.
- **Standing user approval (2026-11-22): ALL Git/GitHub operations are PRE-APPROVED within the current assigned task.** Do not ask the user for authorization to use Git or GitHub. You may independently fetch, pull, create/switch task branches and worktrees, commit, cherry-pick, rebase/merge task branches, push, create/update PRs, tags, and update the mailbox when needed to accomplish the task. Publish completed, verified task commits to `migration/java25-correctness` without waiting for a separate push approval.
- This standing approval supersedes **all earlier procedural requirements** to wait for permission before commit/push/merge/rebase, including task-local "NO push before review" and "one-time push approval" clauses. It does **not** expand the scope of code changes, authorize work on `main`, or waive correctness, tests, and reporting.
- Before any write, verify the correct repository and target branch; preserve user changes and others' commits. Prefer fast-forward pushes. For a non-fast-forward or potentially destructive history operation, verify ancestry and exactly what would be overwritten; never silently discard someone else's work. If state is ambiguous, STOP and report BLOCKED for a technical reason, not to request routine Git permission.
- `main` remains STRICTLY read-only, unless the user expressly changes this restriction. `JInvCommon` remains read-only. Do not push to another repository or branch unrelated to the assigned task.
- Maintain one logical change per commit where practical, run available validation, and verify the remote commit SHA after push; report what was pushed and what remains UNKNOWN.
- Public/protected API compatibility remains mandatory unless the task explicitly says otherwise.
- JInvCommon is read-only and MUST NOT be changed.
- New source comments must be in Russian.

A safe way to reply without switching the working checkout:

```powershell
git fetch origin
git worktree add --detach ..\fore25-mailbox origin/agent/codex-mailbox
# edit ..\fore25-mailbox\docs\codex\outbox.md
git -C ..\fore25-mailbox add docs/codex/outbox.md
git -C ..\fore25-mailbox commit -m "mailbox: reply <MESSAGE-ID>"
git -C ..\fore25-mailbox push origin HEAD:agent/codex-mailbox
git worktree remove ..\fore25-mailbox
```

---

## MESSAGE D4-001

**Status:** CLOSED  
**Target branch:** `migration/java25-correctness`  
**Expected starting production commit:** `475b24f935aa0f38e1ed6993809ec18d71f4ac5f`

### Task: D4 listener lifecycle, narrowed by architecture decision

The user made an explicit architecture decision:

> Runtime rebinding of an already bound `DSFXAdapter` to another Dataset is unsupported and considered a bad design for a TableView adapter.

### D4.1 `DSFXAdapter.setDataSet(...)`

Keep the existing public signature for compatibility, but:

- add/retain `@Override`;
- mark the method `@Deprecated`;
- make every call fail fast with `UnsupportedOperationException`;
- do not perform partial rebinding;
- do not change the superclass API;
- do not add replacement public API.

The method remains present so source/binary API shape is preserved as far as Java permits, but its runtime use is deliberately rejected.

Choose a concise exception message describing that changing Dataset for a bound TableView adapter is unsupported.

### D4.2 Dataset listener cleanup on `close()`

Investigate the exact subscriptions installed by `DSFXAdapter.bindTable(...)`.

Confirmed current bind path includes:

```java
dataSet.addRowListener(this);
dataSet.addNavigationListener(this);
dataSet.addDataSetListener(this);
```

The inherited `DSControlAdapter.close()` removes Row and Navigation listeners but does not remove the DataSet listener.

Prove the lifecycle bug with a targeted test/probe, then implement the minimal symmetric cleanup required by `DSFXAdapter`.

Also inspect the mark-listener path:

```java
xxiDs.addMarkDataSetListener(this);
```

Determine whether a matching public removal API exists and whether `DSFXAdapter.close()` must remove this subscription when it was installed.

Do not guess. Report CONFIRMED / REJECTED / UNKNOWN separately.

### Important scope limits

Do NOT turn D4 into a general listener cleanup/refactor.

In particular:

- do not redesign TableView event handler lifecycle;
- do not refactor the D3 selection synchronization;
- do not change initial binding behavior;
- do not change `clearSelection()` semantics;
- do not change D5-D7;
- do not modify JInvCommon.

The D3 selection listener and TableView mouse/key handlers may be mentioned as follow-up observations if relevant, but do not expand scope without a reproduced correctness problem.

### Deferred, explicitly out of current scope

Do not change or refactor:

- logback-related work;
- `JInvXMLViewer` beyond already accepted Stage 1 migration;
- the entire package `ru.inversion.diff`.

If an issue is found there, record it only as `DEFERRED`.

### Verification

Need targeted evidence for at least:

1. bind DS1, close adapter, then DS1 must no longer deliver DataSet-level callbacks to adapter;
2. Row/Navigation cleanup remains correct;
3. if mark listener cleanup is implemented, prove it is detached;
4. `setDataSet(...)` always throws the intended `UnsupportedOperationException`;
5. public method signature remains unchanged.

Run all locally available checks and clearly mark corporate JavaFX 25 runtime/build as UNKNOWN if unavailable.

### Git

One logical local commit after reviewable fix.

Suggested message:

```text
fix: close dataset listener lifecycle
```

If D4.1 deserves mention, choose a better single message covering both changes.

**Do not push production branch without explicit permission.**

### Reply requirements

Write a concise reply to `docs/codex/outbox.md` with:

- MESSAGE-ID: D4-001
- status: DONE / BLOCKED
- branch and HEAD
- CONFIRMED root causes
- exact production changes
- tests/probes and results
- public API impact
- remaining UNKNOWN / follow-ups
- local commit SHA if created
- PUSH status


---

## MESSAGE D4-002

**Status:** CLOSED  
**Purpose:** review request for local production commit from D4-001.

Do not change code. Do not amend. Do not push production branch.

Local production commit reported by D4-001:

`0dd94764ac4d75eccc75a57c85ad005676875d19`

I cannot inspect that commit through GitHub because production push is correctly still disabled.

Please publish to `docs/codex/outbox.md` the exact review material for this commit:

1. Full production diff only:

```text
git diff <parent-of-0dd94764>..0dd94764 -- src/main/java/ru/inversion/dataset/fx/DSFXAdapter.java
```

2. Full current code of:
   - `DSFXAdapter.setDataSet(...)`
   - `DSFXAdapter.close()`
   - any helper/state added specifically for D4.

3. Exact probe diff for:
   - `src/test/probes/Test-D4Lifecycle.ps1`
   - modifications to `src/test/probes/Test-EmptyProperty.ps1`

4. Explain why changing D2 probe was necessary after the architectural decision that `DSFXAdapter.setDataSet(...)` is unsupported.

5. Confirm whether `close()` removes:
   - DataSet listener
   - mark listener, only when applicable
   - Row/Navigation listeners via `super.close()`

6. Confirm repeated `close()` behavior and null safety.

7. Report parent SHA of `0dd94764...`.

No code changes.
No production push.

Reply under:

`## Reply D4-002`


---

## MESSAGE D4-003

**Status:** CLOSED  
**Purpose:** verify intermediate D3 documentation commit before allowing D4 production push.

Do not change code.
Do not amend existing commits.
Do not push production branch.

Commit to review:

`f08248e4a4d90ae9423316e29f2197fee83fa9ba`

This commit is the parent of D4 production commit:

`0dd94764ac4d75eccc75a57c85ad005676875d19`

### Required review material

Please publish to `docs/codex/outbox.md`:

1. Parent SHA of `f08248e4a4d90ae9423316e29f2197fee83fa9ba`.

2. Full commit summary:
   - commit message;
   - changed files;
   - additions/deletions per file.

3. Full exact diff of `f08248e...` against its parent.

4. Explicitly confirm whether the commit changes:
   - production Java source;
   - probe/test code;
   - public/protected API;
   - JInvCommon;
   - only documentation.

5. If it is documentation-only, summarize exactly what was added to the D3 report and confirm that it matches the corporate Java 25 / JavaFX 25 runtime results already recorded:
   - P2 PASS
   - P1 REJECTED_GAP
   - P1 additional methods PASS
   - P3 PASS
   - P4 PASS with Dataset remaining on B
   - P5 PASS with NAV=[0->1, 1->2]
   - overall probe VALID EXIT 0
   - initial binding unchanged
   - clearSelection semantics unchanged
   - no feedback loop observed

6. Confirm no changes to deferred areas:
   - logback
   - JInvXMLViewer
   - package ru.inversion.diff

### Reply format

Append:

`## Reply D4-003`

with:

- MESSAGE-ID: D4-003
- STATUS: DONE / BLOCKED
- COMMIT: f08248e...
- PARENT: ...
- DIFF SUMMARY
- FULL DIFF
- PRODUCTION CODE CHANGES: YES / NO
- PROBE CODE CHANGES: YES / NO
- PUBLIC API CHANGES: YES / NO
- JInvCommon CHANGES: YES / NO
- DEFERRED AREAS CHANGED: YES / NO
- VERDICT

No code changes.
No production push.


---

## MESSAGE D5-001

**Status:** CLOSED  
**Mode:** READ-ONLY INVESTIGATION + targeted reproduction; NO production changes.  
**Target branch:** `migration/java25-correctness`  
**Expected GitHub HEAD:** `0dd94764ac4d75eccc75a57c85ad005676875d19` (D4 accepted, corporate build green).

### Task: D5 synchronous executeQuery callback contract

Inspect `DSFXAdapter.executeQuery(BiConsumer<Boolean,Throwable> clb, IFormStateListener stateListener, boolean parallel, boolean readAllRecords)` and its public overloads, call sites, relevant full-scan guards and callback consumers.

**Confirmed static observation:** in the `parallel=false` branch, after successful `getDataSet().executeQuery(readAllRecords)` there is no `callBack.accept(true,null)`. The catch invokes `callBack.accept(false,th)`; the finally invokes `positionRow(0)`.

This is strong evidence of missing success notification, but do NOT assume a complete callback contract without inspecting usages.

#### Required investigation

1. Find all relevant callers of `executeQuery(..., parallel=false)` and consumers of `clb`; determine expected success/error semantics and callback count, documenting concrete call sites.
2. Analyze `parallel=true` as a comparison only, including its asynchronous full-scan decision path, not as refactoring scope.
3. **Distinguish the two overloaded `checkForFullScan` methods.** Trace `boolean checkForFullScan()` in the sync branch and its `false`/cancel/skip path. Determine if `false` means user-declined, query not run, or something else. Do not treat cancellation as successful execution.
4. Analyze `getDataSet()==null` early return and any exception before entering the `try` block (e.g. controller/cast). Define what is CONFIRMED vs UNKNOWN. Do not broaden scope without proof.
5. Map all outcomes: executed successfully, execution throws, full-scan declined, Dataset null, callback itself throws, positioning errors. Check exactly-once callback and whether it runs under `DATA_SET_LOCK`; record thread-affinity assumptions. Do NOT automatically refactor lock/callback threading in this task.
6. Create a minimal regression probe (prefer `src/test/probes/`) using real project code/dependencies where feasible. Demonstrate **BEFORE FIX** callback counts/values; if full `DSFXAdapter` cannot be instantiated locally, clearly label extraction probe as limited (not integration). If JavaFX 25 needed, prepare exact one-command corporate runner rather than simulating production as proven.
7. Propose one minimal internal fix preserving public/protected signatures and legacy behavior; identify decisions requiring user approval, especially the cancellation contract. **Do not implement the fix yet.**

#### Guardrails

- Production files unchanged; do not modify the public/protected API.
- No change to `JInvCommon`.
- Do not touch D1–D4, D6, D7 or other unrelated behavior.
- DEFERRED: logback, `JInvXMLViewer`, whole `ru.inversion.diff`.
- New code comments in Russian.
- Do not run unsafe modifications of any external DB.
- No production commit; no production push.
- Report CONFIRMED / HYPOTHESIS / UNKNOWN separately.

#### Reply

Publish `## Reply D5-001` to `docs/codex/outbox.md` on `agent/codex-mailbox`, including:
- branch + HEAD, status, production changes NONE;
- exact contract inferred from call sites;
- outcome matrix (success/error/cancel/null);
- before-fix probe evidence and any limitations;
- recommended minimal fix and open cancellation decision;
- files created locally and corporate execution command if needed;
- git status;
- no production push.

This is an investigation task, so local uncommitted probe files may be left pending review. Only the mailbox reply may be pushed.


---

## MESSAGE D5-002

**Status:** CLOSED  
**Task type:** narrowly scoped correctness fix + regression tests, local commit only.  
**Working branch:** `migration/java25-correctness`  
**Expected baseline:** `0dd94764ac4d75eccc75a57c85ad005676875d19`

### Decision following D5-001

Investigation D5-001 accepted. The synchronous branch of
`DSFXAdapter.executeQuery(BiConsumer<Boolean,Throwable>, IFormStateListener, boolean, boolean)`
omits success callback after `getDataSet().executeQuery(readAllRecords)` returns normally.

Implement a **minimal internal fix** and a reproducible regression probe.

#### Required semantics (explicit user-facing engineering decision)

- If the synchronous Dataset query **actually ran and returned normally**, invoke exactly once `callBack.accept(true, null)`.
- If query or full-scan guard throws, preserve existing exactly-one attempted `callBack.accept(false, th)`, passing the original throwable.
- If `checkForFullScan()` returns false (user declines / dismisses), **do not invoke success or failure callback**; preserve silent skip.
- If `getDataSet()==null` on entry, preserve early return without callback.
- If pre-try listener resolution fails, do not silently change that behavior.
- If success callback itself throws, do **not** catch it as a query failure and invoke a second callback. Propagate its exception naturally, while preserving existing `positionRow(0)` attempt in the finally block.
- Preserve positioning attempt and order in all cases where the original synchronous block did so, including declined guard, query errors and callback exceptions.
- Preserve synchronous caller thread, `DATA_SET_LOCK` ownership, existing callback execution timing relative to queued positioning, and method signatures. Do not introduce asynchronous callback delivery.
- Do not refactor parallel=true or full-scan dialog logic, even if separate flaws were observed.

Investigate simplest boolean success flag scoped to sync path, with success callback **outside the query catch but before `positionRow(0)`**. Ensure exception behavior and exactly-once attempts are covered. Avoid code duplication.

### Tests and regression

Update existing untracked D5 probe files from D5-001:

- `src/test/probes/Test-D5ExecuteQuery.ps1`
- `src/test/probes/d5/D5ExecuteQueryProbe.java`

Test both BEFORE and AFTER evidence where practical:
1. success: one query, exactly one `(true,null)`;
2. query exception: exactly one `(false, original throwable)`;
3. guard exception: no query, one failure notification;
4. guard refusal: no query, zero callbacks;
5. Dataset null: zero callbacks;
6. throwing success callback: only one callback attempt and no false retry; positioning still attempted;
7. throwing failure callback: no retry; positioning still attempted;
8. positioning enqueue failure: document masking/propagation behavior, no fake extra callback.

If using source-extraction tests, explicitly state they are not equivalent to real `DSFXAdapter` integration. Ideally support a **single PowerShell command** for corporate Java 25 / JavaFX 25, deriving Maven dependencies from existing project instead of requiring manually filled environment variables. Do not download or substitute JavaFX/JInvCommon.

### Compatibility and guardrails

- Keep all public/protected signatures and runtime defaults unchanged.
- `JInvCommon`: read-only; absolutely no modifications.
- No D1-D4 or D6-D7 unrelated changes.
- DEFERRED: logback, `JInvXMLViewer`, package `ru.inversion.diff`.
- Source comments only in Russian.
- No external DB operations.
- `positionRow`, `clearSelection`, `setDataSet` and other lifecycle semantics outside this sync callback path must not change.

### Git/review

Start with `git status`, current branch, and HEAD. Check that only the two untracked D5 probes exist before modifications (or report deviations).

Make **one local commit** with narrow production fix and tests after passing all available checks.

Suggested commit message:

`fix: notify synchronous query success`

**Do not push production branch.**

Reply through GitHub mailbox `docs/codex/outbox.md` as:

`## Reply D5-002`

Include:
- exact commit SHA + parent;
- complete production diff (not just summary);
- exact changed file list and probe commands;
- test matrix and results;
- remaining UNKNOWN, especially corporate JavaFX25 integration;
- public API compatibility;
- production PUSH: NO.

Do not start D6 or other work.


---

## MESSAGE D5-003

**Status:** CLOSED  
**Mode:** REVIEW ONLY: do not change files, do not amend, do not push production branch.

D5-002 local commit: `692ed672b6215dd5845491b7bb818f36ae324b10`.
Its production diff was reviewed and is preliminarily approved. We now need independently review test quality and corporate runner before authorizing a production push.

Please publish a review-only response as `## Reply D5-003` in mailbox `docs/codex/outbox.md` on branch `agent/codex-mailbox`.

### Required exact evidence

1. Complete source of `src/test/probes/Test-D5ExecuteQuery.ps1` from local D5 commit, with no omissions. If very long, put complete source instead in a separate **mailbox-branch only** file `docs/codex/d5-probe-runner-review.md` and link its repository path from outbox.
2. Complete source of `src/test/probes/d5/D5ExecuteQueryProbe.java`, same rules (separate mailbox file allowed).
3. Explain precisely how BEFORE and AFTER source variants are selected; ensure BEFORE extracts parent `0dd94764...` without changing working checkout and AFTER extracts actual D5 code.
4. Confirm the D5 extraction probe really executes the actual changed sync block rather than an independently reimplemented version. Identify any stubs/rewrites and what they limit.
5. Explain precisely how `-Corporate` mode compiles and runs a real `DSFXAdapter` with the actual JavaFX 25 and JInvCommon from this project; whether any proxy/test-double changes semantics, and which outcomes it can/cannot prove.
6. State the exact expected command to run on the corporate Windows checkout with JAVA_HOME=JDK25, including `mvnw.cmd` support, and how failed Maven/type-check is distinguished from test failure.
7. Confirm git status, current local HEAD, and production PUSH: NO.

Do not modify production, tests, docs on the production branch, the existing commit, or any external DB. Do not push production. Write only the mailbox reply (and optional review attachments on mailbox branch).


---

## MESSAGE D5-004

**Status:** CLOSED  
**Mode:** Narrow TEST-RUNNER reliability fix ONLY. No production code changes.
**Working branch:** `migration/java25-correctness`.
**Local accepted production commit:** `692ed672b6215dd5845491b7bb818f36ae324b10`.
**Production PUSH:** still forbidden until review.

### D5-003 review verdict

The production change in `DSFXAdapter` is APPROVED.
The extraction probe copies the real synchronous branch from source, and the BEFORE/AFTER comparison is credible within its documented limitations.
The real JavaFX25 probe is appropriate for callback integration, but its launcher is not ready for our standard corporate workflow.

### Task

Improve **only** `src/test/probes/Test-D5ExecuteQuery.ps1` in `-Corporate` mode, preserving `-Before`/AFTER extraction behavior.

1. Java/Javac must prefer tools under `JAVA_HOME\bin` when `JAVA_HOME` is set; otherwise use PATH. Validate JDK 25 before proceeding and print a clear error if not running under 25.
2. Prefer `mvn` when it is available, otherwise fall back to the project's existing `mvnw.cmd`. If neither exists, report the missing executable clearly. Do not change Maven wrapper files, Java build POM, Nexus configuration or corporate dependencies.
3. Do not force Maven offline mode (`-o`) by default. The corporate Maven execution may need Nexus. An explicit optional offline switch is acceptable if useful.
4. Distinguish Maven project compile/classpath failure, Java probe type-check/compilation failure, and JavaFX/runtime/assertion failure. These must be distinct errors with nonzero exit.
5. Prefer compiling the Java probe explicitly with JDK25 `javac` into a unique `target/d5-...` directory and running its compiled main class. This avoids ambiguous Java source-file mode and enables separate compile/run diagnostics. Preserve the real `target/classes` + Maven dependencies classpath. Account for Windows paths containing spaces (argument files if needed).
6. Keep the integration test logic, test assertions, and production sync callback behavior unchanged.
7. Print an exact one-command corporate invocation from the repo root; no manual environment-variable dependency classpath requirement.
8. Validate PowerShell syntax and locally available `-Before` and AFTER extraction tests; `git diff --check`. Mark real corporate JavaFX 25 execution UNKNOWN until run by user.

### Git

Make a separate local test-only commit, e.g. `test: harden D5 corporate probe runner`.
Do NOT amend previously accepted local production commit.
Do NOT push `migration/java25-correctness`.
Do NOT modify JInvCommon, D1-D4, D6-D7, logback, JInvXMLViewer, or ru.inversion.diff.

### Reply

Publish `## Reply D5-004` in mailbox outbox with:
- local test-runner commit SHA and its parent;
- exact PowerShell diff;
- tests and results;
- intended corporate command;
- working-tree status;
- production changes NONE;
- production PUSH NO.

Then stop and wait for review.


---

## MESSAGE GUI-001

**Status:** CLOSED  
**Priority:** HIGH, top-level project goal: Java 8/JavaFX 8 -> Java 25/JavaFX 25 Legacy GUI migration.  
**Target implementation branch:** `migration/java25-correctness`. Never write to `main`.

### Step 0: publish accepted D5 work, then switch focus

D5 production fix and corporate test runner were independently reviewed and approved.

Local commits:
- `692ed672b6215dd5845491b7bb818f36ae324b10` — sync query success callback
- `60e70689f7eeed990c64f19669b35b817d3f82b8` — D5 corporate runner

**Explicit permission:** push `migration/java25-correctness` up to and including `60e70689f7eeed990c64f19669b35b817d3f82b8` ONLY, provided `git status` is clean and the branch HEAD and linear ancestry match those SHAs. Never force push. Do not push any later commits.

Report the pushed SHA in mailbox. D5 corporate JavaFX25 runtime remains UNKNOWN pending the user's corporate execution.

**D6/D7 are postponed**, not cancelled. Do not continue `DSFXAdapter` audits now.

### Step 1: prioritize real JavaFX25 GUI behavior

Main mission is now the *original Legacy JavaFX GUI layer*, not framework redesign.

Stage 1 source migration reached a corporate Maven Java25 compile milestone. The missing evidence is behavior at runtime: application controls, skins, CSS/FXML and interactions actually functioning in JavaFX 25.

Conduct a narrowly scoped reconnaissance of current repository entrypoints and test paths:

- `JInvTable`, `JInvTableViewSkin`, `JInvTableColumn` (selection, editing, sizing, focus).
- TextField/TextArea custom skins, editor behavior, context menu ownership.
- ComboBox editor/list custom skin and popup behavior.
- Minimal FXML/SceneBuilder integration and style loading if present.
- ControlsFX PropertySheet and toolbar interoperability as next tier.

Find the smallest realistic **JavaFX25 GUI smoke path** that executes actual production Legacy controls, not replicas, with a visible Stage/Scene and no external business DB. Inspect existing test/demo launchers first; reuse them if practical.

### Step 2: deliver one runnable GUI smoke test, not a huge report

Implement **at most one narrow smoke harness** under `src/test/probes/gui/` (and an optional PowerShell launcher under `src/test/probes/`) to exercise:

1. At least a real `JInvTable` or production table subclass if feasible, including actual skin/layout and selection;
2. At least one real editable Legacy control (TextField/TextArea/ComboBox based on minimal dependencies) and its focus/edit interaction;
3. A real FX toolkit/Stage/Scene shown where possible, not merely calling JavaFX constructors headlessly;
4. No production code changes on this first GUI-001 iteration.

Prefer corporate Maven dependency resolution + Java 25 + JavaFX25 and a one-command Windows PowerShell runner. Handle both `mvn` and `mvnw.cmd`, and avoid hard-coded home paths or unrelated downloads.

If `BaseApp` or corporate application infrastructure is strictly required, explain the blocker and propose the smallest valid launcher. Do not work around required real behavior using fake controls.

Crucial: a compiler-only probe or `-ValidateOnly` is NOT evidence of GUI runtime correctness. Separate local `UNKNOWN` from corporate runtime results.

### Required report

Publish `## Reply GUI-001` in `docs/codex/outbox.md` on `agent/codex-mailbox` with:

- D5 push status / SHA;
- GUI inventory and minimal real runtime path;
- runnable harness source/command or exact blocker;
- which GUI behaviors are actually tested versus still UNKNOWN;
- local checks;
- files changed;
- whether a local test-only commit was created (optional), SHA and parent;
- production changes NONE and GUI-001 push NONE.

No general refactor. No UI public/protected API changes. No changes to `JInvCommon`.
New source comments in Russian.

### Explicit deferred exclusions

Do not touch:
- logback;
- `JInvXMLViewer` legacy regex/formatting/security beyond accepted Stage1;
- all `ru.inversion.diff`.

D6 (rowFinder listener finally) and D7 (bulkAppend reset) remain backlog, not priority.

After GUI-001, stop for review. Do not begin GUI-002 without a new mailbox instruction.


---

## MESSAGE GUI-002

**Status:** CLOSED  
**Mode:** READ-ONLY REVIEW MATERIAL, no code modifications, no commits/amends, no production push.  
**Working branch:** `migration/java25-correctness`.  
**Local test-only GUI commit:** `74fa142a5af7722809e6ce842be0581d0fe7cd8c`.  
**Expected parent:** `60e70689f7eeed990c64f19669b35b817d3f82b8`.

### Task

GUI-001 has completed a local harness, but the commit has not been pushed and cannot yet be independently inspected via GitHub. Provide exact evidence for one-pass final review before publishing the GUI harness.

1. Full exact diff of the local GUI commit against its parent (including new untracked-then-committed files). Report file list, counts and current HEAD/status.
2. Provide COMPLETE contents of `src/test/probes/gui/LegacyGuiSmoke.java` and `src/test/probes/Test-LegacyGuiSmoke.ps1`. If long, place complete copies in a single `docs/codex/gui-001-review.md` file **on mailbox branch only**, referenced from outbox. Do not edit the production branch.
3. Verify the smoke really creates/shows a visible Stage, instantiates REAL Legacy `JInvTable`, `JInvTextField` and their production skins, and asserts useful row/column selection, layout, focus and text replacement. Distinguish programmatic manipulation from actual physical input. Report if the use of an inline FXMLLoader/production CSS is faithful and where it might fail.
4. Check for invalid assumptions about `Stage.show`, focus ownership when OS denies focus, FX queue barriers, toolkit shutdown, timeouts and JavaFX classpath/native module setup. Failures should say GUI UNKNOWN, not falsely PASS.
5. Verify launcher Maven wrapper fallback and JDK selection, Windows paths with spaces, target output directory, and separation of Maven/build, javac and runtime errors.
6. Ensure no production, public/protected API, JInvCommon or deferred area changes.
7. Give exact corporate Windows PowerShell command. No external business database required.

Reply with `## Reply GUI-002` in mailbox `docs/codex/outbox.md` linking review source if applicable.

**No changes to production or local GUI commit. No production push.** Then stop.


---

## MESSAGE GUI-003

**Status:** CLOSED  
**Purpose:** publish accepted GUI-001 smoke harness, no other production work.  
**Working branch:** `migration/java25-correctness`.

### Review verdict and authorization

GUI-002 code review accepted:
- local test-only commit `74fa142a5af7722809e6ce842be0581d0fe7cd8c`;
- parent `60e70689f7eeed990c64f19669b35b817d3f82b8`;
- two newly added files only:
  `src/test/probes/gui/LegacyGuiSmoke.java`,
  `src/test/probes/Test-LegacyGuiSmoke.ps1`;
- production/public API/JInvCommon changes NONE.

**Explicit one-time permission:** if local HEAD is exactly `74fa142a5af7722809e6ce842be0581d0fe7cd8c`, working tree is clean, origin `migration/java25-correctness` is at `60e70689f7eeed990c64f19669b35b817d3f82b8`, and history is linear, push the working branch **only to this accepted commit**. Do not force-push and do not include later commits. If conditions differ, STOP and report BLOCKED without guessing.

After push, verify remote branch SHA using `git ls-remote origin refs/heads/migration/java25-correctness`.

### Corporate handoff

Provide concise commands from corporate Windows interactive desktop checkout:
1. `git pull`
2. `powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\src\test\probes\Test-LegacyGuiSmoke.ps1`

Remember corporate JDK25 `JAVA_HOME` and Maven settings. Do not claim GUI PASS until that command actually runs successfully with `GUI-001 PASS`/exit 0. If failure, classify Maven/classpath, javac, toolkit/native, application infrastructure, focus/layout/assertion; avoid guessing the root cause.

D5 JavaFX25 corporate probe remains pending separately.

### Post back

Publish `## Reply GUI-003` in `docs/codex/outbox.md`:
- push status;
- local and remote SHA;
- exact next corporate command;
- GUI runtime status UNKNOWN;
- confirmation no production source/API changes.

Then stop; no GUI-004 implementation, no D6/D7. Exclude logback, `JInvXMLViewer`, `ru.inversion.diff`.


---

## MESSAGE GUI-004

**Status:** CLOSED
**Priority:** High; actual Legacy JavaFX 25 GUI behavior, not DSFXAdapter audit.
**Working branch:** `migration/java25-correctness`
**Expected current GitHub HEAD:** `504f94d6fa4331ce3fe5e561afb8a96e510ebacd` (SVN-only pom.xml cleanup, corporate BUILD SUCCESS).

### Confirmed GUI-001 result supplied by the user

The user executed `Test-LegacyGuiSmoke.ps1` on Windows using Oracle Java 25.0.1 / JavaFX 25 / the real project's compiled classes with a working custom Maven settings.xml and obtained:

```text
BUILD SUCCESS
Stage shown; FXML/CSS loaded; Legacy table skin, layout,
cell selection and editor focus/edit passed
GUI-001 PASS
```

The visible window appeared. Warnings:
- JavaFX unnamed module (classpath-based launch), nonblocking for this smoke.
- `URIFacade - No icon path found!`, nonblocking for tested controls.

Treat GUI-001 as **CLOSED for its tested matrix only**. Do not claim full form or component equivalence. No production modifications are necessary as a result of this smoke.

### Task: GUI-004, one focused Legacy editor GUI smoke

Next priority is behavioral verification of **real production Legacy TextArea and ComboBox** components and custom skins after migration JavaFX8 -> JavaFX25.

Inspect actual classes first, especially `JInvTextArea` custom skin, `JInvComboBox`, `JInvComboBoxListViewSkin`, and focus/context-menu ownership code. Find the smallest realistic visible Stage + Scene test, using real control classes, no fake skins.

Create at most one narrowly scoped new or extended GUI smoke harness under `src/test/probes/gui/`, with a PowerShell one-command launcher. Reuse GUI-001 infrastructure when practical; don't break existing GUI-001.

Mandatory checks, if supported by real public API without app bootstrap:
1. Java 25 and JavaFX 25, actual production controls and custom skins.
2. Visible Scene/Stage and laid-out controls.
3. TextArea programmatic focus, selection and text edit and meaningful context-menu ownership/availability assertion (do not invent context menu semantics).
4. Editable ComboBox: editor text normalization, selection/value relationship, popup show/hide and focus behavior. Check at least one **actual popup shown** state after an FX pulse, not just invoking show().
5. Record precise failures and distinguish setup/focus limitations from confirmed control bugs.

Prefer deterministic FX barriers/timeouts, avoid relying on arbitrary sleeps. If CSS/resource setup requires BaseApp or an icon path, explain the minimal real dependency; do not replace production control with a standard JavaFX stand-in.

### Constraints

- First iteration TEST-ONLY. Do not change any production Java source, public/protected API, POM, Maven wrapper or JInvCommon.
- Keep D6/D7 in backlog.
- Do not touch DEFERRED logback, `JInvXMLViewer`, or `ru.inversion.diff`.
- New source comments only in Russian.
- Use current Maven with custom settings file (environment may have `MAVEN_ARGS` set to `-s <path>`). The runner should not forcibly replace the user's Maven settings or assume a home path.
- No external DB, no full application login.

### Deliverable and review

- Produce a runnable test harness and exact corporate/home Windows PowerShell command.
- Run PowerShell/java syntax checks and available checks locally.
- Corporate JavaFX 25 runtime status UNKNOWN until user actually runs it.
- One test-only **local commit** allowed, but **do not push** production branch pending review.
- Reply in `docs/codex/outbox.md` on mailbox branch as `## Reply GUI-004`: files, commit SHA, exact source/diff for review (full sources may be attached on mailbox branch), tested behavior, limitations/UNKNOWN, working tree status, no production changes/push.

Do not proceed to GUI-005 or wider GUI refactor. Stop for review.


---

## MESSAGE GUI-005

**Status:** CLOSED  
**Priority:** Legacy JavaFX25 GUI correctness, Stage 1 behavioral smoke  
**Working branch:** `migration/java25-correctness`  
**Expected current remote HEAD:** `504f94d6fa4331ce3fe5e561afb8a96e510ebacd`

### Step A: publish accepted GUI-004 test-only commit

GUI-004 is approved for its tested matrix. User ran the real visible GUI smoke successfully and reported:

```text
TextArea edit/menu ownership and ComboBox selection, commit and same-value normalization passed
ComboBox popup window shown and hidden; focus returned to TextArea
GUI-004 PASS
```

Local GUI-004 commit:
`22d2dffff9d48558193a3c95184fe1ed94b51244`  
Parent:
`504f94d6fa4331ce3fe5e561afb8a96e510ebacd`

Exact diff was independently reviewed: ONLY
`src/test/probes/gui/LegacyEditorsSmoke.java`
and `src/test/probes/Test-LegacyGuiSmoke.ps1` test-runner extension. No production/POM/API/JInvCommon changes.

**Already accepted GUI-004 push (also covered by standing Git approval):** If local HEAD is exactly the above commit, tree clean, remote branch HEAD exactly the above parent, and ancestry is linear, fast-forward push `migration/java25-correctness` **only to 22d2dffff9d48558193a3c95184fe1ed94b51244**. Never force push. If any discrepancy, stop as BLOCKED and report. Verify remote SHA.

### Step B: GUI-005 investigate real Legacy table cell editing

Main mission remains original Legacy GUI migration JavaFX8 -> JavaFX25, not a redesign.

GUI-001 has already demonstrated visible JInvTable/JInvTextField, table layout, selection, editor focus. GUI-004 demonstrated TextArea/ComboBox skin, popup, text and context menu behavior.

Next untested high-risk area is **editing inside real JInvTable cells**. Inspect actual production:
- `JInvTable`, `JInvTableColumn`, `JInvTableViewSkin`;
- existing Legacy cell factories/editors and their appropriate activation/commit/cancel contracts;
- column read-only/editability defaults, custom skin focus and navigation, current editing semantics.

Choose the smallest representative *real* editable Legacy column/editor, with an in-memory record and observable property, running in a visible Stage/Scene on JavaFX 25 without business DB or BaseApp if feasible.

At most one **test-only** GUI harness under `src/test/probes/gui/` and a minimal extension to the existing GUI PowerShell runner, preserving GUI-001 and GUI-004 default/flag behavior.

Aim to assert (according to actual production contract, not invented expectations):
1. Legacy table and cell factory/skin are instantiated, laid out and editable;
2. enter editing via real TableView edit path;
3. value commit updates exactly the intended target model/property, without losing focus/selection unexpectedly;
4. cancel edit leaves model value unchanged;
5. moving between rows/cells gives coherent editing/selection state.
Differentiate simulated programmatic edit/commit from physical key/mouse events. Do not assert persistence to DB.

If the Legacy editor requires application infrastructure or the available cell classes are not directly usable without Dataset, explain exact blocker and produce minimal realistic alternative rather than faking the editor. Do not change production code to make a smoke pass.

### Constraints and verification

- NO production code, public/protected API, POM, Maven wrapper or JInvCommon modifications.
- NO D6/D7 changes.
- DEFERRED: logback, `JInvXMLViewer` and entire `ru.inversion.diff`.
- New source comments Russian only.
- Use the user's custom Maven settings via existing environment (`MAVEN_ARGS` if set); do not hard-code personal paths.
- Run locally available syntax/compile checks and report limitations.
- Corporate/actual JavaFX25 GUI result UNKNOWN until user runs test.
- One local test-only commit allowed for GUI-005. Git/GitHub push is pre-approved by the standing user instruction: once locally checked, push the task commit to the working branch and report its verified remote SHA. Do not wait for an additional approval.

### Reply

Publish `## Reply GUI-005` to `agent/codex-mailbox:docs/codex/outbox.md` with:
- GUI-004 fast-forward push status + verified SHA;
- GUI-005 code inventory and exact test coverage;
- new files, local commit SHA (if created) and parent;
- full exact diff or mailbox-branch review attachment;
- single Windows PowerShell command;
- actual results vs UNKNOWN and failure diagnostics;
- confirmation no production/API/JInvCommon/deferred changes.

Stop after reporting results and the push. No GUI-006 work.


---

## MESSAGE GUI-005-PUBLISH

**Status:** CLOSED
**Working branch:** `migration/java25-correctness`

Independent review of GUI-005 exact diff in outbox complete. The test-only change is accepted for a **narrow programmatic-edit/cancel smoke**.

Commit `13ceedc419941bb53b701707e2d0a737af9edeba`, parent `22d2dffff9d48558193a3c95184fe1ed94b51244`. Files only:
- `src/test/probes/gui/LegacyTableEditingSmoke.java`;
- `src/test/probes/Test-LegacyGuiSmoke.ps1` (`-TableEditing`).

The user explicitly pre-approved all Git/GitHub operations. Push this validated local commit to `migration/java25-correctness` **without asking for additional permission**, only if tree clean, local SHA as above, current remote SHA equals parent, and fast-forward. Verify remote after push. No force push or changes to main/JInvCommon. Do not modify production to force the test to pass.

### Review caveat (must retain in report)

Production `JInvTextFieldTableCell.cancelEdit()` contains an alternative branch: when its private/protected `editingCell` field is non-null (set via table mouse press filter), it emits a `TableColumn.editCommitEvent()` before `super.cancelEdit()`. The smoke intentionally avoids mouse events and exercises ONLY programmatic cancel, not the mouse-populated `editingCell` path, actual ESC keyboard interaction, or real user cancellation. Do not claim all cancellation semantics are verified. This branch is a **specific potential data-integrity risk**, not yet proven to be a bug; record as a follow-up probe after GUI-005 runtime result. Do not quietly change this behavior or broaden this task.

The smoke also uses a **synthetic Enter KEY_RELEASED** and an observable in-memory JavaFX property, not physical typing or Dataset persistence.

### Next step

Once pushed, provide user command from interactive Windows checkout with custom Maven settings inherited:

```powershell
git pull --ff-only
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\src\test\probes\Test-LegacyGuiSmoke.ps1 -TableEditing
```

Report runtime UNKNOWN until the user sends actual result; don't start GUI-006 or additional fixes.

Post `## Reply GUI-005-PUBLISH` in outbox with remote SHA, checks and review caveat; mark this message CLOSED after verified publication.


---

## MESSAGE GUI-006

**Status:** CLOSED
**Working branch:** `migration/java25-correctness`
**Starting remote HEAD (verified):** `13ceedc419941bb53b701707e2d0a737af9edeba`
**Priority:** correctness and data integrity of real Legacy TableCell cancellation; make GUI runner reproducible with custom Maven settings.

### Confirmed by user and GitHub

On interactive Windows Java 25.0.1 / JavaFX 25 user ran `Test-LegacyGuiSmoke.ps1 -TableEditing` and reported:

```text
Java=25.0.1+8-LTS-27 JavaFX=25
Legacy controls=file:/D:/Java/Projects/fore25/target/classes/
Legacy table edit, single-property commit, Enter navigation/focus and cancel passed
GUI-005 PASS
```

The commit `13ceedc4...` is already published to `migration/java25-correctness`. Treat GUI-005 as CLOSED **only for programmatic edit/synthetic Enter/programmatic cancel**.

### Task A: runner correctness, own minimal commit

The GUI runner repeatedly fails on the user's home Windows machine because Maven uses a *custom* `settings.xml` path. Current working workaround: session `$env:MAVEN_ARGS = "-s `"<settings.xml path>`""`, but this is fragile across fresh PowerShell sessions.

Make a minimal backwards-compatible enhancement to `src/test/probes/Test-LegacyGuiSmoke.ps1` to support optional explicit `-SettingsFile <path>` (and optional `-LocalRepository <path>` only if truly needed), passing `-s <path>` as **separate native arguments** to the Maven call and validating existence; if omitted, preserve the existing `MAVEN_ARGS`/defaults. Handle spaces in Windows paths robustly, no hardcoded user-specific paths. Preserve GUI-001/004/005 launch flags and compatibility. For tests, verify PS parser and argument construction; no Maven credentials in logs. Do not change `pom.xml`, wrapper, project parent/repository definitions.

### Task B: GUI-006 targeted cancel correctness smoke, separate minimal commit

Inspect actual `src/main/java/ru/inversion/fx/form/controls/renderer/JInvTextFieldTableCell.java`, especially:
- `tableMouseHandle` assigns `editingCell = getTableView().getEditingCell()` on mouse press;
- `tableFieldKeyHandler` clears `editingCell` when ESC is pressed;
- `cancelEdit()` emits `TableColumn.editCommitEvent()` with the editor's text if `editingCell != null`, before `super.cancelEdit()`.

This conditional commit-on-cancel is a **confirmed code path**, but whether or when it violates intended UX semantics is UNKNOWN. Distinguish actual user cancellation via ESC, mouse-click-away committing, and pure `table.edit(-1,null)` from GUI-005. Do not assume every cancel should discard changes if Legacy contract intentionally commits on focus loss/mouse elsewhere.

Create a focused test-only JavaFX 25 visible-Stage probe under `src/test/probes/gui/`, reusing the production Legacy `JInvTable`, `JInvTableColumn`, and `JInvTextFieldTableCell`; extend the runner with a distinct `-TableCancel` switch. Use observable in-memory properties to count **actual change events** and monitor TableColumn.editCommitEvent; ensure tests prove the mouse-handler field was set through real/synthetic mouse event through appropriate event routing, not reflection or private fields; capture before/after editor drafts, event count, focus and selected position. Verify scenarios separately:
1. true ESC cancellation (actual key event handling path): no unintended model mutation;
2. mouse press while editing, then cancel: does current code perform a commit? Record precise actual behavior and assess vs Legacy intent; fail only if an agreed invariant is violated;
3. no mouse and programmatic cancel: same as GUI-005 control case;
4. no double-commit or cross-row mutations.

If it is not feasible to reproduce original mouse semantics without native pointer or BaseApp, do not pretend coverage; report UNKNOWN and explain minimal reproduction. Scope is **test-only** for now. **Do not alter production editor behavior** and do not change public/protected APIs until evidence and explicit correctness rationale are reviewed.

Use Java25 existing project classes and custom Maven settings. Keep GUI-001/004/005 available. Separate code vs hypothesis, use reproducible diagnostics.

### Git and reporting

All Git and GitHub operations are PRE-APPROVED by user. Work only on `migration/java25-correctness` and mailbox branch, not `main` or JInvCommon. Once checks pass, commit task A and task B separately and **push fast-forward without asking**; verify remote SHA. Preserve any existing user changes.

Write `## Reply GUI-006` in `docs/codex/outbox.md` with per-commit SHAs, code/diff for review, exactly what was verified locally, precise Windows commands including optional `-SettingsFile`, interactive runtime UNKNOWN until user report, and confirmed/hypothesized cancellation behavior. Stop after report. D6/D7, logback, XMLViewer, ru.inversion.diff all out of scope.


---

## MESSAGE GUI-007

**Status:** CLOSED
**Working branch:** `migration/java25-correctness`
**Expected remote HEAD:** `8ead483ae4eba6a972b474ea969aa783e953b33e`
**Priority:** data integrity: stale editor draft across edit sessions in Legacy `JInvTextFieldTableCell`.

### Evidence from user's actual interactive Windows Java25/JavaFX25 GUI-006 PASS

```text
PROGRAMMATIC BEFORE draft=[] model=[Original, Other]
PROGRAMMATIC ACTION draft=[Draft PROGRAMMATIC] model=[Original, Other]
PROGRAMMATIC AFTER draft=[Draft PROGRAMMATIC] model=[Original, Other] commitEvents=[] propertyChanges=[]
MOUSE_THEN_ESCAPE BEFORE draft=[Draft PROGRAMMATIC] model=[Original, Other]
MOUSE_THEN_ESCAPE ACTION draft=[Draft MOUSE_THEN_ESCAPE] model=[Original, Other]
MOUSE_THEN_ESCAPE AFTER draft=[Draft MOUSE_THEN_ESCAPE] model=[Original, Other] commitEvents=[] propertyChanges=[]
MOUSE_THEN_PROGRAMMATIC BEFORE draft=[Draft MOUSE_THEN_ESCAPE] model=[Original, Other]
MOUSE_THEN_PROGRAMMATIC AFTER draft=[Draft MOUSE_THEN_PROGRAMMATIC]
  model=[Draft MOUSE_THEN_PROGRAMMATIC, Other]
  commitEvents=[...value=Draft MOUSE_THEN_PROGRAMMATIC]
  propertyChanges=[...before=Original, after=Draft MOUSE_THEN_PROGRAMMATIC]
GUI-006 PASS
```

Conclusions: no commit for unarmed programmatic cancel or mouse+ESC; single commit and model change for mouse-armed programmatic cancel. GUI-006 PASS means the existing code path was observed, NOT that cancellation is defect-free. Synthetic events used, physical click-away semantics remain UNKNOWN.

**Additional confirmed observation:** after cancel, the reopened editor starts with the previous session's *discarded draft*, even while the model still reads `Original`. It is very likely because `startEdit()` reuses `textField` and sets cell label `setText(...)` but does not refresh the editor `textField.setText(...)` except when `updateItem(...)` happens.

### Task: isolate stale draft data integrity scenario

1. Inspect `JInvTextFieldTableCell.startEdit/updateItem/cancelEdit` and JavaFX25 TableCell lifecycle. Identify precise lifecycle conditions where a TextField draft is reused, whether intended initial edit text is blank or model current value, and whether deferred FX updates could repair it.
2. Create a **test-only** GUI-007 targeted regression/reproducer in visible Stage with the production Legacy cell/skin, no DB. The key scenario should be:
   - edit row 0, change draft, ESC to discard, assert model unchanged;
   - reopen the same cell and *do not type any new text*;
   - capture initial editor text and model value, and optionally mouse-armed click-away / `table.edit(-1,null)` to test whether the *previous, canceled draft* gets committed without fresh typing.
   - Count commit events and property changes; verify other row untouched. Clearly distinguish synthetic event behavior from real mouse clicks.
   - Include a control case of fresh edit as needed, and characterize whether a rendering/pulse event resets the text.
3. The desired invariant for analysis is **a canceled draft must not silently become a committed model value on a later untouched edit**. Do not overconstrain exact initial text if Legacy intentionally starts edits blank. Report reproduced/unsupported/UNKNOWN precisely.
4. Preserve GUI-001/004/005/006 runner behavior, add a distinct switch `-EditorReentry` with optional existing `-SettingsFile`; no hardcoded paths. The probe should produce intelligible reproduction output; a failure of an intended invariant should be labeled clearly. Reproducible known-bug diagnostics are acceptable, but don't pretend PASS means fixed.
5. **TEST/INVESTIGATION ONLY** this iteration. Do NOT modify production Java editor, public/protected API, POM, JInvCommon, or deferred modules. Do not make speculative repair to pass smoke. No D6/D7.

### Git/process

The user pre-approved all Git/GitHub operations. Once syntax/compilation and test harness checks complete, commit this narrow test-only probe and push fast-forward to the working branch, verify remote SHA, without asking permission. Stop if user changes/conflicting history present. Source comments only in Russian.

Reply `## Reply GUI-007` in `docs/codex/outbox.md` with complete source/diff, commit and verified SHA, code-level findings, exact user's PowerShell command with `-SettingsFile`, local validation, and interactive JavaFX25 result UNKNOWN pending user execution. No follow-up implementation or GUI-008 until evidence reviewed.


---

## MESSAGE GUI-008

**Status:** CLOSED
**Priority:** CRITICAL, confirmed data-integrity defect, minimal production correctness fix
**Repository:** `ssu95a/fore25`, **branch:** `migration/java25-correctness`
**Expected remote HEAD:** `0b4b2d65b05ec637959ca3a81a81df5d4d650cd3`

### Confirmed RED on real Java25/JavaFX25

The user ran GUI-007 via `Test-LegacyGuiSmoke.ps1 -EditorReentry` (visible real JavaFX 25 Stage and production cell), yielding:

```text
FRESH draft=[] model=[Original, Other]
AFTER ESCAPE draft=[GUI-007 discarded draft] model=[Original, Other]
  commitEvents=[] propertyChanges=[]
REOPEN IMMEDIATE reusedEditor=true
  draft=[GUI-007 discarded draft] model=[Original, Other]
REOPEN AFTER PULSE draft=[GUI-007 discarded draft] model=[Original, Other]
AFTER UNTOUCHED CANCEL draft=[GUI-007 discarded draft]
  model=[GUI-007 discarded draft, Other]
  commitEvents=[Commit[row=0, value=GUI-007 discarded draft]]
  propertyChanges=[Change[row=0, before=Original, after=GUI-007 discarded draft]]
GUI-007 REPRODUCED: Escape-discarded draft was committed on untouched reentry
```

This is **CONFIRMED data-integrity defect**; GUI-007 test failing is an intentional red regression, not an infrastructure issue. JavaFX unnamed-module warning is nonblocking.

### Root mechanism (source inspected)

`src/main/java/ru/inversion/fx/form/controls/renderer/JInvTextFieldTableCell.java`:
- `startEdit()` reuses `textField`; on an existing editor it updates the cell label via `setText(...)` but does **not** reset `textField.setText(...)` from model/current item.
- `cancelEdit()` tears down graphic and filters but leaves stale draft in the reusable `textField`.
- `updateItem()` updates editor only in limited item-refresh conditions, not guaranteed at reentry.
- `tableMouseHandle` arms a separate Legacy commit-on-cancel behavior that may be intentional for clicking away; **do not remove it or redefine Escape semantics without a separate evidenced rationale.**
- Freshly allocated editor is explicitly cleared in `startEdit()` (`textField.setText("")`), so original initial-input UX may intentionally differ from the expected standard TableCell editor. Do not silently assume that initial blank means current item should always be inserted; assess its contract.

### Objective

Make a targeted production correctness fix so an **Escape-discarded draft can never be committed on a later untouched reentry**. Keep compatibility (no public/protected signature changes).

1. Analyze precise startEdit / updateItem / cancelEdit lifecycle and decide the minimal reliable initialization point for each edit session. Consider first allocation vs reused editor and null/converter handling. Avoid introducing re-entrant commit/focus loops.
2. Strengthen an existing targeted smoke (or narrowly add an assertion) to distinguish **cold first edit untouched** from **reentry after Escape**, both with mouse-armed cancel. We must establish whether the existing blank-first-editor UX can itself silently change a model value without new typing. If it does, do not call the main issue fully fixed until that risk is classified; fix only when justified by same correctness invariant and explain any UX compatibility impact.
3. Keep and **do not weaken** GUI-007 stale-draft invariant: `-EditorReentry` should no longer print `GUI-007 REPRODUCED`; it should PASS after red/green verification on actual desktop. Do not mask wrong behavior by merely changing test exit code or event collection.
4. Preserve GUI-006 previously observed semantics: ESC discards model changes; mouse press plus programmatic cancel can produce one commit (the current Legacy mechanism), no duplicates or cross-row writes. Preserve GUI-001/004/005.
5. Minimal source change confined to `JInvTextFieldTableCell` and focused tests. Russian comments for new source. No unrelated refactors.

### Verification and Git

Run available JDK25 tests, Maven `-Pbuild` compile, explicit checks of known GUI probes where locally feasible. The user's Maven settings are custom: do not hardcode path; GUI runner supports `-SettingsFile`. If no visible interactive desktop, mark GUI-007/008 runtime UNKNOWN, not PASS. Explain before/after code-level evidence and risks.

All Git and GitHub operations are PRE-APPROVED. Commit small, validated changes and fast-forward push only `migration/java25-correctness`, verify SHA. Never change `main`, JInvCommon or API signatures. Do not touch D6/D7, logback, JInvXMLViewer, ru.inversion.diff. No speculative cleanup.

Reply `## Reply GUI-008` in mailbox outbox including:
- confirmed root cause and chosen fix, why alternative approaches were rejected;
- exact diff/source and commit SHA, verified remote SHA;
- exact regression matrix and commands with custom `settings.xml`;
- test results split into local confirmed and interactive UNKNOWN until user reports;
- any remaining limitations, particularly physical click-away vs synthetic MouseEvent.

Stop after GUI-008. Do not begin next task.


---

## MESSAGE GUI-009

**Status:** CLOSED
**Priority:** Legacy JavaFX25 integration milestone, real data-bound visible table
**Working repo/branch:** `ssu95a/fore25` / `migration/java25-correctness`
**Expected remote starting HEAD:** `fa4af1b688b4e20662663a110bad2b86fe1fe48c`

### Baseline and latest user acceptance

User ran GUI-007 `-EditorReentry` against GUI-008 production fix on **Java 25.0.1 / JavaFX 25**:
- FRESH editor: `Original`, other row `Other`;
- Escape: original model unchanged, zero commit events;
- reentry immediately and after pulse: editor reset to `Original`, not discarded draft;
- untouched mouse-armed cancel: event `Original`, zero property changes;
- first edit of row 1: editor `Other`, untouched cancel event `Other`, zero property changes;
- `GUI-007 PASS`.

Thus GUI-008 production defect is CLOSED for the user-run regression. Codex also reported local GUI-001/004/005/006 PASS after the fix. Separate physical input, DB persistence, and custom converters remain beyond this milestone.

### Goal: go beyond isolated controls without broad refactoring

Build a minimal **visible JavaFX25 integration smoke** that uses together:
- real production `JInvTable`, `JInvTableColumn`, `JInvTableViewSkin`;
- real production `DSFXAdapter.bindTable(...)`;
- real JInvCommon `ArrayDataSet` with 3 distinct in-memory records (not a mock replacement for `IDataSet`);
- a visible Stage with actual column values rendered.

D3 already verified `DSFXAdapter` + `ArrayDataSet` selection sync on a **plain TableView with no columns and not-shown Stage**. GUI-009 must add materially new coverage: production Legacy Table/skin/columns on-screen, with real binding. No SQL database or network is needed here. Be precise: this is **not** `SQLDataSet` integration and not a full business form.

Minimum checks, only if supported by real APIs:
1. After bind, visible rows/columns render expected values, and table items reference the actual Dataset rows (no copy).
2. UI-driven selection/navigation moves Dataset current row exactly once and keeps row identity, and Dataset-driven cursor change updates TableView selection with no feedback loop. Record listener counts for duplicate/recursive events.
3. An in-memory row value update refreshes visible presentation according to the actual documented binding contract; if refresh is required, invoke the legitimate production mechanism and explain why.
4. Adapter lifecycle `close()` detaches Dataset-level and navigation subscriptions as established in D4; no late callbacks to closed adapter. Do not assume closing adapter disposes the entire TableView.

Before implementation, inspect `DSFXAdapter.bindColumns` and `JInvTable` / `JInvTableColumn` metadata/controller/skin dependencies. If a controller or BaseApp bootstrap is truly necessary, report exact minimal setup and whether user can run on corporate Windows. Do not smuggle in fake controllers, stub internals, or production changes merely to make a test pass. Prefer a smaller accurate proof and an explicit UNKNOWN rather than a misleading PASS.

### Execution

Prefer extending `Test-LegacyGuiSmoke.ps1` with one new mutually exclusive `-DataBinding` flag, reusing its `-SettingsFile` and Maven implementation, while preserving GUI-001/004/005/006/007 flags. Add one test-only GUI harness under `src/test/probes/gui/` if feasible. JDK 25 + JavaFX 25, no credentials/DB, visible Stage, production CSS, deterministic FX pulse barriers, useful failure diagnostics. Synthetic events vs physical interaction distinguished.

No change to production Java, public/protected APIs, POM, Maven wrapper, JInvCommon or main. Avoid D6/D7, logback, JInvXMLViewer and ru.inversion.diff. New comments Russian only.

### Git, verification and report

Git/GitHub operations within task pre-approved. If a valid, locally type-checked **test-only** harness is achieved, commit and fast-forward push only working branch; verify remote SHA. If blocked by real application infrastructure, do **not** push speculative/incomplete smoke. Describe precisely what dependency is missing and smallest next step.

Run source/PowerShell parse checks, Maven compile and javac when available. If GUI cannot be run on a real interactive desktop, mark GUI-009 runtime UNKNOWN. Do not claim SQLDataSet/business form support.

Publish `## Reply GUI-009` to mailbox outbox with design findings, exact diff/commit, command including `-SettingsFile`, source vs hypothesis, local verification, and outstanding blockers. Stop after GUI-009 for review.


---

## MESSAGE GUI-010

**Status:** CLOSED
**Priority:** Stage 4 JavaFX25 Legacy form lifecycle integration
**Repository and branch:** `ssu95a/fore25` / `migration/java25-correctness`
**Remote baseline SHA (verified):** `0a328b9f1ec960837862db2cd785abf2a42309ec`

### User-confirmed milestone, GUI-009 CLOSED

User independently executed GUI-009 on Windows, actual JDK25.0.1/JavaFX25:
```text
RENDER A/Alpha B/Bravo C/Charlie; dataset row identity preserved
NAV [0->1] SEL [0->1] adapterNav=2
NAV [0->1, 1->2] SEL [0->1, 1->2] adapterNav=3
UPDATE before refresh=[Charlie] after adapter.refresh=[Charlie updated]
CLOSE Dataset, navigation and row notifications observed independently; adapter=Callbacks[navigation=3, rows=0, dataset=1]
GUI-009 PASS
```
The `sun.misc.Unsafe::objectFieldOffset` warning comes from `com.github.ben-manes.caffeine:caffeine:2.9.3` under JDK25; this is nonblocking for GUI-009. Record as **DEFERRED third-party compatibility debt**. Don't upgrade Caffeine without dependency-tree/root/compatibility evidence.

### Main task: first real Legacy form with a controller

Advance beyond the GUI-001/004/005/006/007/009 isolated controls and bare data-bound table. Investigate and, **only if feasible with real APIs**, construct a visible minimal true Legacy form using actual:
- `JInvFXFormController` or a concrete subclass of `AbstractBaseController`,
- JavaFX `FXMLLoader`, actual form FXML/FX declarations and production CSS,
- `FXFormLauncher` / `ViewContext` / appropriate Stage lifecycle when possible,
- real Legacy controls and optional `ArrayDataSet` without any database.

Read and map actual initialization/bootstrap requirements, including `AbstractBaseController.initialize(...)`, form initialize/close handling, `FXFormLauncher.showInternal`, `ViewContext`, and any `BaseApp.APP()` / `TaskContext` assumptions. Do not assume GUI-009's controller-less binding proves form lifecycle. Inspect existing in-repo form samples/tests/resources.

Build a **test-only** visible smoke for genuine form lifecycle, if achievable:
1. FXML loader creates/attaches a real controller and injected `@FXML` Legacy controls.
2. Real form stage appears with CSS and form component layout; record actual controller lifecycle entry points and conditions.
3. At least one form-level behavior is exercised (e.g., controller action/validation/form state, or real controller-driven table dataset integration) through **production** paths, not just direct control manipulation.
4. Form closes normally and releases/reconciles resources, without invoking DB.
5. Document which full-launcher semantics, login, TaskContext, BaseApp services, prefs, and SQLDataSet still remain unverified.

Prefer an `-FormLifecycle` mutually exclusive switch in `src/test/probes/Test-LegacyGuiSmoke.ps1`, preserving `-SettingsFile` and all prior switches. Add minimal Java/FXML under test/probe or test resources only; no fake production controller, reflection or stubbing of BaseApp globals. Use `AnimationTimer`/runLater barriers rather than sleeps. In-memory `ArrayDataSet` only if it naturally fits real form contract. If full `FXFormLauncher` genuinely cannot run without BaseApp/TaskContext, **do not rename bare FXMLLoader smoke a full launcher proof**: show exact blocker and choose an honestly scoped FXML/controller test, or stop with an evidence-based investigation.

### Limits

- FIRST PASS is TEST-ONLY, no production Java, public/protected API, Maven POM, wrapper, JInvCommon, or main changes.
- Do not disturb D6/D7 or deferred logback, XMLViewer, diff and Caffeine.
- New comments in Russian.
- No credentials, SQL connections, login or dependence on corporate system services.
- Custom `settings.xml` supported using existing optional `-SettingsFile`; do not hardcode any user path.
- No overclaiming real business form lifecycle if only partial coverage.
- Maintain behavior/API compatibility.

### Process

All Git/GitHub operations already PRE-APPROVED by the user. Commit only a reviewable, locally checked **test-only** result and fast-forward push to `migration/java25-correctness`; verify remote SHA. If the real controller/launcher cannot run under these constraints, report a specific blocked dependency and minimal next step rather than introducing fake shims. Avoid changing someone else's work, and never change main/JInvCommon.

Send `## Reply GUI-010` in mailbox outbox, including:
- lifecycle architecture findings (CONFIRMED / UNKNOWN),
- exact diff and commit SHA if produced,
- local verification and real GUI runtime status (UNKNOWN unless actually executed),
- exact Windows command with `-SettingsFile`,
- remaining limitations/risks and recommended next move.

Stop at GUI-010, no automatic GUI-011.


---

## MESSAGE GUI-011

**Status:** CLOSED (investigation done; full-launcher runtime BLOCKED)
**Priority:** Full real Legacy form launcher lifecycle / JavaFX25 migration
**Repository:** `ssu95a/fore25`
**Working branch:** `migration/java25-correctness` (never modify `main`)
**Expected remote HEAD:** `789c2e59de58a963701f72fa46fe5f02e3de3df8`

### Confirmed GUI-010 and scope boundary

The user independently ran `Test-LegacyGuiSmoke.ps1 -FormLifecycle` on Java 25 / JavaFX 25 with their custom Maven settings and obtained:

```text
FXML production controller initialized; CSS, TextArea skin and standard OK laid out
OK -> WINDOW_CLOSE_REQUEST -> [ON_OK, ON_CLOSE] -> RET_OK; cleanup window closed
GUI-010 PASS (standalone production FXML/controller and SDI close)
```

GUI-010 is CLOSED for **standalone real FXML/controller plus SDI close**, not for full launcher initialization, login, TaskContext or full BaseApp services.

### Objective

Advance to exercising the **real production `FXFormLauncher.show()` -> `AbstractBaseController.internalShow()` -> `initFormController()` -> `internalInit()`** path under JavaFX25. We want a *meaningful integration proof*, not an artificial PASS.

### First: evidence-based architecture/feasibility analysis

Investigate in actual source (not assumptions):
- `FXFormLauncher.showInternal()` delegates to `AbstractBaseController.internalShow()`.
- `BaseApp.APP().reportGuiLaunch()`, frame mode and view preference service prerequisites.
- `BaseApp.start(Stage)` invokes `LoginManager.login()`, after-login work and main-frame startup; distinguish full application from a safe test entry point.
- `beforeInit()`: `checkLicense()`, `ModuleService`, `initFormStateDecorator()`, data object/FXEntity setup.
- `initFormController()`: TaskContext, ViewContext, session title action, keyboard, form initialization, show/close.
- `ViewContext.of(Stage)` and actual production `JInvTextAreaForm.fxml` / controller.
- Any direct/indirect DB connections, current user or licensing dependencies even with null or non-owned TaskContext.
- Existing app bootstrap code or in-repository test/smoke fixtures; whether a *real configured BaseApp instance* can be started without login/DB, or requires a sanctioned corporate runtime.

Explicitly distinguish: CONFIRMED requirements, hypothesized requirements, UNKNOWN.

### Then: minimum safe executable test, ONLY if possible without fake application environment

If feasible using **real production API and configured services**, construct a test-only visible JavaFX25 `-FullFormLauncher` scenario that:
1. Starts a genuinely initialized BaseApp/service context, or documents the authentic minimal bootstrap.
2. Invokes actual `FXFormLauncher` (not direct `FXMLLoader`, manual `controller.init()`, reflection/private-field mutation, fake `BaseApp.APP()`, or bypassing `beforeInit`).
3. Observes lifecycle `BEFORE_INIT -> INIT -> AFTER_INIT -> RUNTIME`, real Stage, user action, `ON_OK -> ON_CLOSE`, callback and resource cleanup.
4. No hidden network/DB/login/licensing bypass. No credentials included in repo, logs, or mailbox.
5. Preserves GUI-010 and all prior smoke modes. Optional `-SettingsFile` remains supported (user path: `D:\Java\.m2\settings.xml`), but do not hardcode user path in source.

If **full launcher is not safely runnable in an isolated context** because it really requires BaseApp services/DB/license/login, STOP and report the precise blocking dependency chain, and propose a minimal supervised GUI-011 corporate interactive experiment or an explicitly *smaller* no-login test. Do not create fake services, bypass checks, or label bare FXMLLoader coverage as a full launcher test.

### Safety, scope, Git

- Phase one is **read-only investigation**. Only create a test-only harness and launcher switch when a real, supported execution route is established.
- Do not change production Java, Maven POM, public/protected APIs, BaseApp/LoginManager/TaskContext implementation, JInvCommon or `main`.
- Do not touch D6/D7, deferred logback, JInvXMLViewer, ru.inversion.diff, Caffeine upgrade.
- Do not require or expose corporate credentials or commit settings.xml.
- Real JavaFX25 GUI results must reflect actual execution, not inferred code inspection.
- Source comments (if added): Russian.
- All Git/GitHub operations pre-approved by user; test-only commit and fast-forward push permitted after validation, verify SHA. Do not push an incomplete/speculative harness. Preserve user work.

### Deliverable

Write `## Reply GUI-011` in mailbox outbox with:
- exact full-launcher initialization chain and evidence for each blocker;
- minimal real safe plan / executable smoke, if feasible;
- exact diff, commit SHA and verified remote SHA if anything published;
- command for the user's PowerShell including `-SettingsFile "D:\Java\.m2\settings.xml"` if an executable probe exists;
- validation results, explicit UNKNOWN for untested runtime;
- precise narrowest next decision for subsequent work.

Stop at GUI-011, no automatic GUI-012.


---

## MESSAGE GUI-012

**Status:** CLOSED
**Priority:** Critical field-value correctness and JavaFX25 GUI compatibility
**Working repository/branch:** `ssu95a/fore25` / `migration/java25-correctness`
**Expected remote HEAD:** `789c2e59de58a963701f72fa46fe5f02e3de3df8`

### Scope and priority change (explicit user decision)

The user has **deferred full `FXFormLauncher` and BaseApp integration**. Do not continue GUI-011, real app/login/TaskContext/ModuleService or create GUI-011 launcher tests. New focus: **`JInvTextField` and its descendants**, preserving Legacy business behavior on Java25/JavaFX25. Main objective is value correctness, input normalization, focus, validation state, LOV integration semantics, skin and listener lifecycle. No large refactor.

### Actual hierarchy (independently inspected)

```text
javafx.scene.control.TextField
└─ JInvTextField
   ├─ JInvRegExpTextField
   ├─ JInvTextFieldCUR ─ JInvCURTextField
   └─ JInvValueField<T>
      ├─ JInvNumberField<T extends Number>
      │  ├─ JInvIntegerField
      │  ├─ JInvLongField
      │  └─ AbstractBigDecimalField
      │     ├─ JInvBigDecimalField ─ JInvPercentField
      │     └─ JInvMoneyField
      ├─ JInvTimeField
      ├─ JInvTimeFieldNew
      ├─ JInvTextFieldFormatted
      └─ JInvCustomValueField<T>
```

`JInvPasswordField` extends JavaFX PasswordField, not JInvTextField. Treat it as an adjacent but separate branch; assess inventory only for now. Include JInvTextArea only for shared ITextFieldBase boundary and avoid redoing GUI-004.

Inspect independently all classes and actual inheritance; correct the inventory if needed, don't assume this list is exhaustive.

### Confirmed code observations to investigate, not yet runtime bugs

- `JInvTextField` constructs a real `JInvTextFieldButtonSkin`, installs case-sensitive `TextFormatter`, focus/selection cache, LOV hooks, custom context menu and tooltip. A subclass can replace this formatter.
- `JInvNumberField` has two-way `textProperty`/typed `valueProperty` listeners with `fromSetText`/`fromSetValue` guards; conversion failures set `State.ERROR`; focus loss re-renders typed values unless state ERROR.
- `AbstractBigDecimalField` scales BigDecimal on assignment with HALF_UP; `JInvMoneyField` has a static `DecimalFormat`, so consider shared format/concurrency and loss of precision/rounding in the appropriate future test.
- `JInvTimeFieldNew` text-change listener accesses `newValue.charAt(0)` without empty guard, and calls `LocalTime.parse(newValue)`. This is a concrete candidate defect but has NOT been runtime confirmed. Include in inventory and isolate separately; do not fix speculatively or combine time work with numeric fixes.
- `JInvTextFieldCUR` involves LOV and form initialization; likely needs controller/services for some cases. Do not assume database-free end-to-end LOV behavior.

### Task A: focused architecture/correctness inventory

Map each production subclass to: `textProperty`, typed `valueProperty`, `TextFormatter`/converter, focus normalization, validation states, null/empty handling, skin/LOV infrastructure dependencies. Rank risks by **data integrity / correctness > lifecycle > performance > cosmetic**. Identify what is safely testable with a visible Stage and no BaseApp/DB, and what remains UNKNOWN. Distinguish confirmed suspicious paths from real observed bugs.

### Task B: first high-value GUI smoke, baseline and numeric family ONLY

If feasible, implement **test-only** `-TextFields` flag on existing `src/test/probes/Test-LegacyGuiSmoke.ps1` preserving all existing flags and `-SettingsFile`. Create one minimal visible JavaFX25 smoke in `src/test/probes/gui/` using production:
- `JInvTextField` and its actual `JInvTextFieldButtonSkin`;
- `JInvIntegerField`, `JInvLongField`, `JInvBigDecimalField` and `JInvMoneyField` only when safe standalone constructors work.

Prefer a small matrix with:
1. initial null/empty/normal text values; `setText`, select/replace and focus changes;
2. `textProperty` and typed `valueProperty` consistency after programmatic set, valid edits, invalid edits and recovery, with explicit state checks;
3. integer/long limits, BigDecimal grouping/decimal separator/scale/rounding only where production contract is clear; no locale assumptions; ensure value does not silently mutate due to mere focus change;
4. skin identity, context-menu ownership, optional external/inner button and orderly dispose/recreate if meaningful;
5. detect unexpected exception on FX thread, recursive text/value updates, duplicate listeners/changes.

Use exact expected behavior derived from source/contracts; no invented UX assumptions. Test can report precise OBSERVED vs UNKNOWN where behavior is unspecified, and separately fail on clearly violated invariants. Use deterministic FX pulses/timeout instead of arbitrary sleeps. Do not introduce tests that require login, real database, LOV data provider, credentials or stubbed BaseApp. No physical input claims for synthetic events.

### Strict constraints

- **FIRST PASS IS READ/TEST ONLY:** do not change production Java, public/protected APIs, POM/wrapper, JInvCommon, main, or deferred modules.
- No work on FXFormLauncher, D6/D7, logback, XMLViewer, ru.inversion.diff, Caffeine.
- New source comments must be Russian.
- User's custom Maven settings path is `D:\Java\.m2\settings.xml`; test runner accepts `-SettingsFile`. Do not hardcode local path in code.
- Preserve the already green GUI-001/004/005/006/007/009/010 switches.
- If a standalone constructor relies on external infrastructure, report exact blocker and skip that subclass rather than fake a PASS.

### Git/reply

All Git and GitHub operations are pre-approved. Commit and fast-forward push only tested/reviewable **test-only** code on `migration/java25-correctness`, verify remote SHA, no force pushes or lost user changes. If implementation blocked, send inventory only and no speculative commit.

Publish `## Reply GUI-012` to `agent/codex-mailbox:docs/codex/outbox.md` with:
- full hierarchy and per-class behavior/risk matrix,
- explicit confirmed vs hypotheses vs UNKNOWN,
- exact source diff/commit SHA and remote SHA if code published,
- Java25/local checks, interactive GUI status (UNKNOWN if not actually launched),
- PowerShell command with `-SettingsFile "D:\Java\.m2\settings.xml"`,
- recommendation for a separate GUI-013 time/masked-field probe.

Stop after GUI-012, no production fixes or follow-on task.


---

## MESSAGE GUI-012A

**Status:** CLOSED
**Priority:** Data integrity / constructor semantics of numeric Legacy JavaFX25 fields
**Repository/branch:** `ssu95a/fore25`, `migration/java25-correctness`
**Expected remote baseline:** `2077722f0fb27b807dfa9bbac4bf9f43cf1d3dbc`

### User verified GUI-012 on actual JavaFX25

The user independently ran `-TextFields` with Java 25.0.1, using `D:\Java\.m2\settings.xml`:

```text
OBSERVED String constructor JInvIntegerField text=[42] value=null state=NULL
OBSERVED String constructor JInvLongField text=[42] value=null state=NULL
OBSERVED String constructor JInvBigDecimalField text=[42] value=null state=NULL
OBSERVED String constructor JInvMoneyField text=[42] value=null state=NULL
NUMERIC ... integer/long boundary, decimal/money rounding, valid/invalid/recovery: passed
FOCUS ... valueEvents=0 for all four fields
GUI-012 PASS (tested matrix)
```

GUI-012 is now CLOSED for its *tested matrix*, but the String-constructor inconsistency is CONFIRMED and remains unresolved.

### Code-level mechanism verified

`JInvNumberField(String arg0)` calls `super(arg0)`, thereby setting JavaFX TextField's initial text **before** the numeric field installs `textProperty` -> `valueProperty` conversion listener. Numeric type remains `null` and `State.NULL` even when the field visibly contains a valid string. `JInvIntegerField(String)`, `JInvLongField(String)`, `JInvBigDecimalField(String)`, `JInvMoneyField(String)` all forward their String argument. `JInvPercentField(String)` also forwards to decimal superclass and must be considered.

### Task A: contract/usage analysis, mandatory before edits

Review all public numeric constructors and in-repo instantiation sites (including compiled FXML usage if any). Determine whether String argument is intended to denote an initial numerical value, or display-only text, and whether caller code expects unparsed initial state. Search actual source, don't infer from name alone. Identify external usage as UNKNOWN.

Inspect Java constructor ordering and subclass converter overrides, specifically `getConverter()`, `valueProperty()`, `getFractionDigits()` for Integer, Long, BigDecimal, Money, Percent, and generic JInvNumberField. Do not introduce unsafe virtual calls to uninitialized subclass fields. Consider whether empty/null and invalid String arguments need explicit semantic handling.

### Task B: targeted regression and minimal fix if justified by a consistent numeric contract

Prove as RED: `new JInvIntegerField("42")`, Long, BigDecimal, Money and Percent on real FX toolkit leave typed value `null` despite valid text, **including no edit and focus loss**. Check what happens to text on focus loss: a displayed value may disappear due to `State.NULL`. Distinguish null and empty String constructors, invalid values, and typed-value constructors. Record precise expected behavior and any disagreements with actual Legacy contract.

If the intended invariant is clear (valid numeric initial text must produce matching typed value/state), make the narrowest production fix possible, preserving existing public signatures and runtime input semantics. Prefer registering the converter listener before assigning initial text at the right initialization phase; avoid `setText(null); setText(original)` hack, hardcoding leaf-specific parsers, spurious notification loops, or changes to value rounding/converter behavior. Review virtual dispatch into subclass properties/converters during super constructors carefully. If behavior cannot be established safely, stay TEST/INVESTIGATION ONLY and report BLOCKED with smallest needed contract decision.

Keep a dedicated regression including: valid "42" on Integer/Long/BigDecimal/Money/Percent, null and empty, invalid input and ERROR/no silent numeric value, precision/scale, valid no-input focus loss, and no extra unexpected value notifications. Preserve existing GUI-012 numeric scenarios and base GUI modes.

### Limits

- No broad refactor of `JInvTextField`, skins, LOV, validator system or time/mask controls.
- No public/protected method or constructor signature changes.
- No JInvCommon, main, pom.xml, wrapper, BaseApp/FXFormLauncher/SQLDataSet changes.
- Deferred: D6/D7, logback, XMLViewer, ru.inversion.diff, Caffeine.
- Only focused code and test, all new comments Russian.
- Preserve user custom settings via existing `-SettingsFile` (actual path `D:\Java\.m2\settings.xml`).
- Do not weaken tests to make GREEN; separate source findings vs real FX evidence.

### Execution/Git/Reply

All Git/GitHub operations pre-approved by the user. Local validation with JDK25 and Maven; interactive FX test if available, otherwise mark UNKNOWN. If justified fix and test are checked, commit minimal changes and push fast-forward to `migration/java25-correctness`, verify remote SHA. If no justified fix, report findings without speculative production commit. Never change main/JInvCommon.

Reply `## Reply GUI-012A` to `docs/codex/outbox.md` on mailbox branch with source usage/contract evidence, exact diff and commit, RED/GREEN results, effects of constructor ordering, remaining uncertainty and user's command including `-SettingsFile "D:\Java\.m2\settings.xml"`. Stop after this, do not start GUI-013 until this issue is classified.


---

## MESSAGE VF-001

**Status:** CLOSED
**Priority:** Core Legacy typed-field data integrity; `JInvValueField<T>`
**Repo/branch:** `ssu95a/fore25` / `migration/java25-correctness`
**Expected remote HEAD:** `0ee740be4ff8a314a0a3b08f280b0bbda3cde770`

### User decision: finish JInvValueField correctness before broadening components

The user confirmed GUI-012A on real Java25/JavaFX25:

```text
CONSTRUCTOR initial JInvIntegerField text=[42] value=42 state=VALUE
CONSTRUCTOR initial JInvLongField text=[42] value=42 state=VALUE
CONSTRUCTOR initial JInvBigDecimalField text=[42] value=42.00 state=VALUE
CONSTRUCTOR initial JInvMoneyField text=[42] value=42.00 state=VALUE
CONSTRUCTOR initial JInvPercentField text=[42] value=42.00 state=VALUE
All invalid String constructors: value=null state=ERROR
All untouched focus loss: value unchanged, valueEvents=0
GUI-012 PASS (tested matrix)
```

GUI-012A CLOSED. **Defer GUI-013 time/mask testing and FXFormLauncher** until the JInvValueField core-value work is decided.

### Grounded architecture findings (already independently checked)

- `JInvValueField<T>` extends `JInvTextField`, defines abstract `valueProperty()`, `getClassValue()`, public `getValue()/setValue()`, `getClipboardContent()`, `showLOV()`, `setTextValueFromLov()`.
- `Binder.bindValueControl` bidirectionally binds `valueProperty()` directly to business `fxEntity` property. `Controls.getValue` returns `getValue()` for JInvValueField.
- `JInvNumberField` retains last valid typed value while malformed draft has `state=ERROR`. This alone is NOT a proven save bug.
- Actual standard form gate: `AbstractBaseController.configControls()` calls `getValidMan().initStateValidator(getContentPane())` in VM_INS/VM_EDIT/VM_NONE. `ValidMan.validate()` runs `stateValidator.validate()` first. `AbstractBaseController.internalOnClose()` calls `validMan.validate()` on standard OK in those modes, so an ERROR blocks that specific path. **Other custom actions directly reading business properties may bypass validation**; do not claim they do without evidence.
- `JInvValueField.getClipboardContent()` copies `value.toString()` when all visible text is selected and typed value nonnull; for a partial selection it copies selected text with spaces removed. This creates a concrete candidate when visible text is invalid and `state=ERROR` while typed value is stale.
- `JInvTextField.setLOV()` calls `addLovValueListener`, whose listener calls `setTextValueFromLov()` using the **current** `getLOV()`. `unbindLOV()` clears the LOV reference and removes its button but does not unregister that listener. Replacing LOV may retain old listeners. This is a separate listener-ownership candidate, not yet a runtime-proven bug.
- `JInvCustomValueField.setCustomImpl()` adds a text listener on every call, capturing the passed implementation; potential duplicate callbacks when implementation replaced. Separate investigation.

### Phase 1, mandatory: test the typed-field contract before changing production

Create one narrow **visible JavaFX25 Stage** regression under `src/test/probes/gui`, one new mutually exclusive `-ValueFields` option in `Test-LegacyGuiSmoke.ps1`, preserving all existing modes and `-SettingsFile`.

Use real `JInvNumberField` descendants at minimum `JInvIntegerField` and `JInvMoneyField` (optionally BigDecimal), production skin, no application/login/DB. Use an actual `SimpleObjectProperty<T>` bound **bidirectionally by the same JavaFX API** as Binder (call this a contract simulation, NOT a fully exercised Binder/FXEntity/form).

Minimum observable matrix:
1. Start null; set valid typed or text value; verify text/value/state/model and exact listener events.
2. Enter invalid text after a valid value; verify `state=ERROR`, draft visible, last valid typed value preserved, business-property surrogate retains last valid value, **no silent new typed commit**, and no callback storm.
3. Recover valid, clear to null and recover, including programmatic focus transitions. Count property notifications and confirm no unintended model mutation on mere focus changes.
4. Copy **all selected text** via actual `getClipboardContent()` in both valid and invalid state; also partial selection with grouping spaces. Distinguish returned ClipboardContent from writing to OS Clipboard. Confirm whether INVALID visible draft copies stale numeric typed value. Source says yes, but run actual case.
5. Inspect actual validation gate source, optionally exercise `ValidMan.StateValidator` with real controls if safe without BaseApp. Do not create fake BaseApp/TaskContext or call a broken mock form "real validation". Explicitly report standard gate proof vs custom-action UNKNOWN.
6. No real DB/persistence assertion.

### Phase 2: only a confirmed clipboard correction, if unambiguous

If RED confirms invalid draft copied as old typed value, adopt this narrowly scoped invariant:

> For `state=ERROR`, a full-text copy must NOT silently export the last valid typed value in place of the visible invalid draft.

Prefer copying the selected visible text in this case, while preserving existing behavior for full valid-selection (`value.toString()`) and partial selections. Implement smallest possible fix in `JInvValueField.getClipboardContent()`, with RED/GREEN regression and null handling. **Do not change `JInvNumberField` numeric sync or standard validation behavior; don't overhaul `TextFormatter<T>`.** If impact on the historical clipboard contract cannot be safely determined, report the proof and proposed contract as UNKNOWN/NEEDS DESIGN instead of guessing.

### Phase 3: source-level LOV and custom-value lifecycle audit only

Record exact ownership and replacement issues in `setLOV()/unbindLOV()` and `JInvCustomValueField.setCustomImpl()`. If easy, use no-DB probes, but **DO NOT modify LOV or custom-value production in VF-001**. Propose VF-002 separately. Don't induce a BaseApp bootstrap bypass.

### Limits and Git

- No public/protected API signature changes.
- No POM, Maven wrapper, JInvCommon or main writes.
- No FXFormLauncher, SQLDataSet, D6/D7, time/masked fields, logback, XMLViewer, ru.inversion.diff or Caffeine changes.
- Source comments Russian only.
- The user's custom Maven path: `D:\Java\.m2\settings.xml`; runner must accept `-SettingsFile`, not hard-code it.
- Local JDK25 compile/PS parse and real GUI runtime if available. Distinguish no-runtime UNKNOWN.
- All Git/GitHub operations pre-approved. After verified focused changes, commit and fast-forward push only `migration/java25-correctness`; verify remote SHA, do not lose other work.

Reply `## Reply VF-001` to outbox with:
- confirmed source contracts, simulated Binder binding and validation gate limitations;
- RED/GREEN reproduction including exact clipboard content;
- exact diff/commit and remote SHA if published;
- exact Windows command `-ValueFields -SettingsFile "D:\Java\.m2\settings.xml"`;
- what remains UNKNOWN; specific recommendation for VF-002 LOV/custom listener lifecycle.

Stop after VF-001; no automatic next task.


---

## MESSAGE VF-002

**Status:** CLOSED
**Priority:** typed-field correctness / lifecycle, stale listeners and property-binding identity
**Repository:** `ssu95a/fore25`
**Working branch:** `migration/java25-correctness` (never modify `main`)
**Expected remote HEAD:** `44b7fed91ebe394cc691f553d74847df043c7c54`

### Completed independent user milestone: VF-001 CLOSED

User ran on actual Java 25 / JavaFX 25 with custom Maven settings `D:\Java\.m2\settings.xml` and reported:

```text
COPY JInvIntegerField invalid full expected=[bad integer] actual=[bad integer]
COPY JInvMoneyField invalid full expected=[bad money] actual=[bad money]
COPY valid full and partial selections all as expected
BINDING JInvIntegerField ... five field/model changes, six text events
BINDING JInvMoneyField ... five field/model changes, six text events
FOCUS JInvIntegerField ... fieldEvents=0 modelEvents=0 textEvents=0
FOCUS JInvMoneyField ... fieldEvents=0 modelEvents=0 textEvents=1
VF-001 PASS
```

Keep this tested typed-value and clipboard contract. Do not reopen VF-001 or undermine its regression. `JInvValueField` remains the common abstraction; no broad TextFormatter<T> redesign.

### Focus A: `JInvTextField.setLOV()/unbindLOV()` listener ownership

Production source inspected:
- `setLOV(lovObj,...)` sets `this.lov` to the new provider, calls `onSetLov(this)`, then `addLovValueListener(lovObj)`.
- `addLovValueListener()` installs anonymous `lov.valueProperty().addListener(o -> setTextValueFromLov())` and the callback dereferences **current** `getLOV()`, not necessarily the owner that fired the notification.
- `unbindLOV()` clears the reference and removes the inner button but **does not detach** the previous listener. Repeated same LOV assignment can add duplicate callbacks.
- `JInvValueField.setTextValueFromLov()` does `setValue(TypeConverter.convert(getLOV().getValue(), getClassValue()))`; the stale callback could mutate the new typed value or throw on null.
- `AbstractLovBase` has static initialization accessing `BaseApp.APP().getViewPrefService()`; assess whether a real no-DB LOV subclass can be instantiated in standalone GUI probe without fatal BaseApp dependencies. Do not bypass production bootstrap, fake global BaseApp or alter license/login.

Prove, if feasible on real JavaFX25, this sequence: install provider A, A value change; replace with B, mutate A, mutate B; install B again, mutate B once; unbind, mutate B and A; count actual field text, typed value changes and callbacks. Classify stale notifications, duplicate listeners, retained owner references and exceptions separately. Do not assert that the same LOV instance's reinstallation is an intended supported feature without checking the contract.

Prefer a minimal test-only provider implementing the real `AbstractLovBase` API using public abstract methods with in-memory values only. If this is infeasible because genuine BaseApp is mandatory, report exact blocker and keep this LOV phase read-only; don't disguise stand-in behavior as production.

### Focus B: `JInvCustomValueField<T>.setCustomImpl()` lifecycle and property identity

Actual source:
```java
public void setCustomImpl(ICustom custom) {
    this.custom = custom;
    textProperty().addListener((observable, oldValue, newValue) -> {
        if (custom != null) setState(custom.setText(newValue));
    });
}
```
- Each call registers a new text listener capturing that specific implementation. Old implementations may receive callbacks after replacement and duplicate calls when reinstalled.
- `valueProperty()` delegates to `custom.valueProperty()`. Replacing `custom` might change property identity while a previous `Property.bindBidirectional(model)` remains bound to the old implementation, silently stranding the model.
- `getClassValue()` also delegates and `custom == null` may be unsupported; don't invent semantics for setting null.

Implement a standalone test-only `ICustom` counter with real JavaFX properties, no fake JInv controls. Record the exact observed callback sequence, current property identity, model identity/bidirectional synchronization, and state after A -> B -> B -> A (or smaller supported sequence), plus initial nonempty text behavior. Prefer a visible Stage and production skin. Treat runtime replacement support as UNKNOWN until evidence; if it is unsupported, report that rather than silently redesign it.

### Approach and constraints

**Phase 1 is RED diagnosis, not speculative repair.** Diagnose each candidate independently. If the cause and intended contract are clear and a narrow fix preserves public/protected methods and existing semantics, produce separate minimal production commits (LOV and custom), with targeted RED/GREEN tests. Otherwise keep test-only and report the required contract decision. No production changes solely for cosmetic cleanup.

Key invariants (where feature is supported):
- Mutating a detached/obsolete LOV must never update the active field.
- Each active provider value change must produce at most one expected typed/text update; unbound providers must have no effect.
- Replacing an ICustom must not continue invoking retired implementations; repeated setting must not duplicate callbacks.
- If dynamic ICustom replacement is supported, its externally bound typed property must remain correctly synchronized; if impossible without API/contract redesign, flag BLOCKED and request explicit design decision rather than manufacturing a green result.

Preserve `JInvValueField` core and its callback/clipboard rules; do not change public/protected API signatures or underlying business semantics. Only `JInvTextField`, `JInvCustomValueField` and a minimal dedicated test harness may be touched by justified fixes.

### Verification

Use user-established GUI runner `src/test/probes/Test-LegacyGuiSmoke.ps1`, add distinct mutually exclusive `-ValueLifecycle` flag (or two independent flags if a real LOV needs separate setup), preserving all existing GUI/VF probes and `-SettingsFile`. Production CSS, Java 25 and JavaFX 25, visible Stage, stable pulse barriers, no arbitrary sleeps. Test actual errors without swallowing FX-thread exceptions. Do not require external DB, credentials, a full FXFormLauncher, or JInvCommon changes.

Existing Maven settings path is `D:\Java\.m2\settings.xml`. Pass as a runner parameter, NEVER hard-code into source. Run available local Maven compile, javac, parser, probe and relevant existing regressions. Distinguish source inspection, runtime confirmation and UNKNOWN. If no interactive desktop, mark GUI runtime UNKNOWN.

No changes to `main`, JInvCommon, POM, wrapper, FXFormLauncher, D6/D7, GUI-013 time/mask fields, Caffeine, logback, JInvXMLViewer, ru.inversion.diff.

All Git/GitHub operations user-approved: commit and push verified focused work to `migration/java25-correctness` without seeking additional approval, only fast-forward, verify remote SHA. Do not overwrite user changes. New comments in Russian.

### Reply contract

Publish `## Reply VF-002` to `agent/codex-mailbox:docs/codex/outbox.md` with:
- source contracts/usage evidence for LOV replacement and dynamic ICustom;
- RED repro per scenario, plus GREEN if fixed, with exact callback count/state/model/field values;
- whether original bound value property identity remains valid;
- exact diff, commit(s) and verified remote SHA;
- exact PowerShell command `-ValueLifecycle -SettingsFile "D:\Java\.m2\settings.xml"`;
- residual blockers and per-part status;
- no full form/SQL claims.

Stop after VF-002, do not automatically start the next task.


---

## MESSAGE VF-003

**Status:** DEFERRED (user confirms JInvCustomValueField unused)
**Priority:** `JInvCustomValueField<T>` data integrity / stable typed-property contract
**Repository:** `ssu95a/fore25`
**Working branch:** `migration/java25-correctness` (main read-only)
**Expected remote HEAD:** `e5e9032d3d455efb92f7cfabda0cdb4fbee86648`

### User confirmed VF-002 on interactive Java25/JavaFX25

The user ran the production `-ValueLifecycle` smoke and obtained `VF-002 PASS (custom listener ownership)`.

Confirmed runtime sequence:
- A edit one -> A:one, model=A:one, state VALUE.
- Replace A with B, edit two -> B:two, model remains A:one, `originalProperty=false`, state ERROR; no stale A callbacks.
- Model writes "model-only" while B active -> old A property/model updated but current field is B:two.
- Reinstall B, edit three -> single B callback, no duplicate.
- Reinstall A -> field reads A's model-only value again, but `state=ERROR` remains until next text edit. Subsequent edit four -> A:four and model A:four, state VALUE.
- Detach with `setCustomImpl(null)`, edit detached -> no callbacks; state remains VALUE although no implementation and `valueProperty()`/getValue() cannot safely be called.
- `VF-002 PASS`.

The previous focused fix `e5e9032d` is accepted for listener ownership ONLY. Dynamic value-property replacement and state lifecycle are NOT fixed.

### Objective: choose a safe architectural contract before changing production

Make an evidence-based read-only investigation of how to handle `JInvCustomValueField.setCustomImpl` and its typed value property.

1. Inspect actual source of `JInvCustomValueField`, `JInvValueField`, `Binder.bindValueControl`, `Controls.getValue/setValue`, and relevant `ICustom<T>` implementations, FXML declarations, all uses of `setCustomImpl()` reachable in this repo. Check whether post-binding replacement is advertised, used, assumed or undocumented. External downstream use UNKNOWN.
2. Trace exact JavaFX property identity and bidirectional binding behavior through A -> B -> A and null; distinguish displayed text, active implementation value, `State`, current `Property<T>`, and original business model binding.
3. Evaluate at least these options with correctness/API/compatibility consequences:
   - **Immutable implementation after initial configuration**: forbid replacement (fail fast) while retaining existing public method; determine how to handle same-instance repeat, null/unset, and whether this breaks legitimate reconfiguration of forms.
   - **Stable field-owned valueProperty**: `JInvCustomValueField<T>` exposes one stable `Property<T>` regardless of implementation; evaluate safe synchronization with `ICustom.valueProperty()`, change ordering, generic type changes, reentrancy, `null`, existing external property-identity assumptions, and `Binder` compatibility.
   - **Explicit rebinding contract**: caller must unbind old and bind new property after replacement, but consider API ergonomics and lack of reliable detection for existing bidirectional binding.
4. Report a clear recommendation that prioritizes avoiding silent model divergence. If no single option is supported by current usage/contract, identify EXACTLY the one design decision needed from the user.
5. Assess whether stale `State.ERROR` on A reinstall and retained state after `setCustomImpl(null)` are independent correctness defects, and whether they can be addressed without silently imposing a new contract.
6. Distinguish CONFIRMED runtime behavior, source-level mechanisms, and UNKNOWN external caller impact. Preserve existing accepted VF-001 and VF-002 regressions.

### Strict scope

**READ-ONLY investigation only this turn. Do not alter production, tests, public/protected API or publish a code commit.** You may add an evidence report to mailbox branch (not implementation branch). Do not treat GUI-013 time/mask or LOV full bootstrap as part of this task. Do not work on FXFormLauncher, BaseApp/login, JInvCommon, main, POM, D6/D7, logback, JInvXMLViewer, ru.inversion.diff or Caffeine.

All Git/GitHub operations approved for mailbox reporting. No changes in working branch. Comments in Russian for any code samples.

### Deliverable

Post `## Reply VF-003` to `agent/codex-mailbox:docs/codex/outbox.md` with:
- exact contract/usage evidence and ambiguity;
- one recommendation and its compatibility risks;
- concrete behavioral examples for after-bind replacement and null;
- smallest next implementation or test task once design contract is chosen;
- confirmation working branch unchanged and remote SHA.

Stop for review; do not begin VF-004 or implement a speculative fix.


---

## MESSAGE GUI-013

**Status:** DEFERRED (superseded by NUM-001, user prioritizes money and decimal fields)
**Priority:** Java25 / JavaFX25 typed time and masked-field data integrity
**Repository/branch:** `ssu95a/fore25` / `migration/java25-correctness`
**Expected remote HEAD:** `e5e9032d3d455efb92f7cfabda0cdb4fbee86648`

### User decision and handoff

User explicitly confirms: **`JInvCustomValueField` is not used. Move on.** VF-003 configure-once/stable-property decision and any further work on CustomValueField are DEFERRED, **not approved for implementation**. VF-002 listener fix stays accepted. Do not revisit or request a CustomValueField design decision.

GUI-012 and 012A numeric fields passed on user Windows Java25/JavaFX25; VF-001 and VF-002 passed. Next planned components:
- `JInvTimeField` (`LocalTime`, own `JInvTimeSkin`, with/without seconds)
- `JInvTimeFieldNew` (`LocalTime`, `TextFormatter` masking, timeWithoutSeconds)
- `JInvTextFieldFormatted` (`String`, mask property, enableMaskValue).

LOV still requires real BaseApp context for a concrete `AbstractLovBase`; defer full LOV lifecycle runtime. `FXFormLauncher` deferred by user.

### Source-level candidates, not yet runtime-proven defects

1. `JInvTimeFieldNew`: text listener calls `newValue.charAt(0)` without guarding null/empty, then `LocalTime.parse(newValue)`. Its formatter rewrites deletion into underscores; check whether actual `setText("")`, paste, clear, backspace, `setValue(null)`, or toggling seconds can surface exceptions. Catch/report FX thread failures. Do NOT assume a source candidate is an observed bug.
2. `JInvTimeFieldNew` formatter and value listeners can perform nested text/value updates, partial/incomplete inputs, and mask switch; ensure no stale valid value is accidentally committed, no unhandled NumberFormatException/DateTimeParseException, and no unsafe feedback loops.
3. `JInvTimeField` uses guarded text/value conversion and a custom `JInvTimeSkin`. Validate null/empty, invalid hours/minutes/seconds, converter modes, focus reformat, caret-based increase/decrease and boundaries, preserving actual intended behavior.
4. `JInvTextFieldFormatted` installs a mask `TextFormatter` when `formatMask` changes; its `textProperty` listener writes `valueProperty` and state; its `valueProperty` listener writes text. Potential duplication, partial masks and accepted value policy need evidence. Study `# ? A U L *` and enableMaskValue semantics before assertions.

### Phase 1: source contract map and deterministic RED smoke, TEST-ONLY

Review exact current production source, existing uses/FXML and converter/skin dependencies. Produce one small **visible JavaFX 25 Stage** harness using all three production controls and production CSS, not standard JavaFX stand-ins, no BaseApp/DB. Add one mutually exclusive `-TimeMasks` switch to `src/test/probes/Test-LegacyGuiSmoke.ps1`; preserve existing GUI and VF flags and `-SettingsFile`.

Test high-value cases with exact observable `text/value/state`, typed/model changes and exceptions:
- Fresh fields and typed assignment, then focus loss and reentry.
- Clearing and `setValue(null)`, including empty edit/delete paths and recovery.
- Complete valid 24-hour time in HH:mm:ss and HH:mm where contract supports, plus invalid 25:xx, minute/second overflow, malformed/partial text, precision and seconds-mode toggle. No timezone/date assumptions.
- `JInvTimeField` caret-based increment/decrement including boundary wrap only if backed by actual API semantics.
- `JInvTextFieldFormatted` one simple stable mask: complete vs incomplete values, invalid character, paste/replacement, mask change and enableMaskValue true/false; verify model value and display relationship.
- Event counts, no hidden typed mutation when text is ERROR, no recursive storms, no exceptions.
- Explicitly note test-driven programmatic and synthetic interaction vs real physical keyboard.

Probe should emit **clear per-scenario OBSERVED**, `PASS` only for supported invariants, `RED` for demonstrated data corruption/exception, `UNKNOWN` for untestable contract. If initial construction, skin or invalid operation throws, classify real reproduction vs flawed setup rather than weakening the test. Prefer pulse barriers to sleeps.

### Phase 2: report results, NO production fix yet

Do not change any production Java in GUI-013, even if RED. We want one focused RED/GREEN fix per distinct root cause in a later task after evidence. Don't quietly alter behavior to make the test green. Source comments Russian only.

Do not change public/protected APIs, `main`, JInvCommon, POM/wrapper, JInvValueField/VF fixes, or any unrelated area. No FXFormLauncher, CustomValueField, LOV, D6/D7, logback, XMLViewer, ru.inversion.diff or Caffeine.

### Git/process/return

Git/GitHub operations pre-approved. Commit and fast-forward push test-only harness once checked, verify remote SHA. Preserve others' work. If blocked by infrastructure, don't create deceptive harness; report exact blocker.

Custom Maven settings for user: `D:\Java\.m2\settings.xml` (pass as `-SettingsFile`, never hardcode).

Post `## Reply GUI-013` in mailbox outbox with:
- class-level behavior/risks, source vs actual runtime evidence,
- exact test coverage, RED/PASS/UNKNOWN, any reproducible exception stack,
- exact commit/diff and verified remote SHA,
- local Maven/JDK25/PowerShell verification,
- concrete user command `-TimeMasks -SettingsFile "D:\Java\.m2\settings.xml"`,
- recommended *single* next correctness fix based on highest-risk confirmed result.

Stop after GUI-013, don't begin GUI-014 or production edits.


---

## MESSAGE NUM-001

**Status:** CLOSED — user independently reproduced `NUM-001 RED findings=4` on Windows JavaFX 25 (2026-11-22). Follow-up: NUM-002.
**Priority:** CRITICAL financial data correctness, conversion/rounding, AND memory footprint per GUI field
**Repo:** `ssu95a/fore25`
**Work branch:** `migration/java25-correctness` (never touch `main`)
**Expected remote HEAD:** `e5e9032d3d455efb92f7cfabda0cdb4fbee86648`

### User's explicit priority change

The user now wants **`JInvMoneyField` and `JInvBigDecimalField`**. Previous `GUI-013` (time/masks) is DEFERRED, and `JInvCustomValueField` remains unused/deferred. Do not work on GUI-013, FXFormLauncher, CustomValueField or LOV. If you already started GUI-013 locally, preserve nonconflicting user work, stop that task, report status, and switch to this assignment before making further production changes.

GUI-012, GUI-012A, VF-001, VF-002 have user-confirmed Java25/JavaFX25 PASS for their *tested* matrices. Do not undo accepted fixes.

### USER CLARIFICATION: MEMORY OVER GUI CONCURRENCY (2026-11-22)

The user confirms ordinary UI controls run on the JavaFX application thread, so GUI-field multithreading is **NOT a priority**. By contrast, **extra retained memory/allocations per control instance IS a priority**: there may be many fields in forms. Do not spend time on multithreaded stress or redesign shared caches for thread-safety unless actual supported off-FX usage is independently evidenced. Code may note conditional concurrency exposure as LOW PRIORITY / UNKNOWN.

Treat memory assessment as first-class alongside data correctness:
- Distinguish **per-instance retained allocations** from static/global caches and one-off temporary allocations.
- Inventory actual per-field retained allocations: `JInvBigDecimalField.formatter`, lazy `g_decConverter`, inherited value/text/skin/listeners/properties, `JInvMoneyField` static converter/global formatter, `AbstractBigDecimalField` property and formatter interactions. Focus *incremental cost* relevant to Money/BigDecimal vs common JInvTextField; avoid claiming entire JavaFX skin cost belongs to numeric subclass.
- Evaluate whether WeakReference<DecimalFormat> cache genuinely reduces per-field formatter allocations, whether it causes a growing list of stale weak refs (index/count loop), and when formats are shared vs recreated on preference change/GC.
- Assess shared MoneyStringConverter cache memory bounds (250 strong key/value pairs) vs per-component cost, and whether it offers measurable benefit. Do not infer a leak without evidence.
- If practical, add reproducible memory diagnostic: count allocations / retained identities over a representative batch of 100 or 1000 fields, with realistic JVM warmed up, class static init excluded and JavaFX thread discipline; avoid unreliable `Runtime.totalMemory()-freeMemory()` claims and arbitrary exact-size numbers. Use JOL/JFR only if already available in environment; no new dependencies/POM edits. Otherwise report qualitative inventory and UNKNOWN numerical bytes.
- Prioritize **numerical correctness first, then retained memory per field**, then secondary throughput; shared mutable formatters matter for isolation/formatting correctness, not speculative GUI races.
- NO production optimization in NUM-001. Quantify before proposing a memory optimization.

### Confirmed source-level facts and candidates (must verify independently)

- `JInvMoneyField` uses a **public static mutable `g_moneyFormat: DecimalFormat`**, initialized with 2 fractional digits, decimal '.', grouping spaces; `initDefaultMoneyFormatter(...)` can replace it. All MoneyStringConverter instances format via this global.
- `MoneyStringConverter` has a shared static access-order `LinkedHashMap<String, BigDecimal>` parse cache, unsynchronized; access-order `get()` changes internal order, and writes evict after 250 entries. The converter parses spaces, comma/dot separator, +/- and K/К/T/Т (thousands) and M/М (millions). It calls `retValue.round(new MathContext(nFration));` **without using the result**. `MathContext` also limits significant digits, not digits after decimal, so don't “fix” by simply assigning this result.
- `AbstractBigDecimalField.valueProperty()` overrides `set()` to apply `setScale(getFractionDigits(), HALF_UP)`; UI behavior may therefore conceal parsing/rounding issues in the converter. Test converter directly and fields separately.
- `JInvBigDecimalField` caches `WeakReference<DecimalFormat>` by scale and grouping and returns the same **mutable `DecimalFormat` object** across instances; cache list access is synchronized only during lookup. Formatting occurs outside this lock.
- `JInvBigDecimalField` converter replaces comma with dot, strips regex whitespace, and uses BigDecimalStringConverter. It also supports dynamic `setFractionDigits()/setShowGroups()`. `JInvPercentField` extends it with a default scale policy; include its dependencies in the assessment, but don't broaden scope unnecessarily.
- Numeric `String` constructor initialization was fixed in `0ee740be` and tested. Typed sync, error recovery, and focus normalization were already covered by GUI-012. Extend rather than repeat those base cases.

### Task A: source/usage analysis, correctness before implementation

Map actual contracts and usage of `JInvMoneyField`, `JInvBigDecimalField`, `AbstractBigDecimalField`, `MoneyStringConverter`, and `JInvPercentField`. Focus on retained heap footprint per field and UI-thread semantics; note off-FX access only if real call sites establish it. Identify public API, initialization order, shared global settings, external/corporate usage UNKNOWN.

Prioritize possible **silent numerical corruption**, stale text vs typed value, rounding/scale, locale/grouping, **memory retained per instantiated field**, shared formatter isolation, then performance. Cache races are not a GUI priority absent supported off-FX evidence. Don't conflate display-only format rounding with actual BigDecimal rounding.

### Task B: focused RED regression harness (test-only first)

Prefer one new visible JavaFX25 `-MoneyDecimal` mode in existing `src/test/probes/Test-LegacyGuiSmoke.ps1`, keeping all existing flags and `-SettingsFile`. Use production `JInvMoneyField`, `JInvBigDecimalField` and skin, CSS; no DB/login/BaseApp.

Validate with explicit `text`, `value`, `State`, error and notification counts:
1. Direct `MoneyStringConverter.fromString/toString` behavior, separately from field `setScale`. Cases 1.005, -1.005, 0, grouped strings, comma/dot, nonbreaking spaces, shorthand K/T/M (including signs), malformed input, very long precisions, zero, and roundtrip invariants where supported.
2. `JInvMoneyField` and `JInvBigDecimalField` typed `BigDecimal` assignment vs text edit, scale HALF_UP, extra input fraction digits, focus loss, `null`, invalid/recovery, and non-default locale presentation (without global default mutations unless restored).
3. Dynamic `setFractionDigits` and `setShowGroups`: do existing instances refresh when settings change? Whether value changes without explicit user edit; when formatting occurs; stale formatters due to cache sharing.
4. Two simultaneous fields using the same fraction/grouping settings: assess independence/identity and any cross-field mutable formatter mutation. Test `JInvMoneyField.initDefaultMoneyFormatter` as global reconfiguration and its impact on existing fields, restoring defaults after test. Check whether converter cache distinguishes global format settings.
5. **Memory footprint**: produce per-instance vs global allocation inventory; inspect formatter sharing, cache entry retention and weak-reference lifecycle. If feasible measure with trustworthy JVM allocation/heap tooling already available, otherwise mark bytes UNKNOWN. NO speculative concurrency stress: the UI is single-threaded per the user's architecture clarification.
6. Capture FX uncaught exceptions and keep sensitive settings/credentials out of logs.

Produce per-case CONFIRMED / REJECTED / UNKNOWN. **Do not alter production code in NUM-001**, even on RED. One demonstrated issue per follow-up fix with RED/GREEN and compatibility review. If there is a clear deterministic correctness defect, make probe report it as RED with nonzero exit, not silently pass. If a historical conversion contract is unclear (e.g. `M` suffix or grouping), report observed behavior without inventing a rejection policy.

### Constraints / Git

TEST ONLY, no production changes, public/protected API or ctor signature modifications, POM, Maven wrapper, JInvCommon or main writes. No work on other deferred: D6/D7, logback, JInvXMLViewer, ru.inversion.diff, Caffeine. All source comments Russian.

Git/GitHub operations pre-approved: after local JDK25 javac/PowerShell/Maven verification, commit only test changes and fast-forward push to `migration/java25-correctness`, verify remote SHA. Preserve user work and no force push. If a complete test cannot be made safe, report blocker and do not push incomplete code.

### Reply

Publish `## Reply NUM-001` in mailbox outbox with:
- source and call-site evidence, per-component retained allocation inventory and memory evidence/UNKNOWN (off-FX use only if found);
- explicit observations vs hypotheses for rounding, formatter and cache behavior;
- exact test diff and verified SHA, local JavaFX25 status;
- Windows command `-MoneyDecimal -SettingsFile "D:\Java\.m2\settings.xml"`;
- ONE prioritized next fix if a data-integrity problem is observed.

Stop after NUM-001. Do not proceed to GUI-013 or NUM-002 without a new assignment.


---

## MESSAGE NUM-002

**Status:** CLOSED — Windows independent validation GREEN on sign-only, full NUM-001 RED findings=1 (grouping only), 2026-11-23
**Priority:** CRITICAL — preserve money typed value on incomplete sign-only user input
**Repo:** `ssu95a/fore25`
**Implementation branch:** `migration/java25-correctness` (never `main`)
**Expected remote HEAD:** `4c75d9943927aebe9c91e1ec235afce1c6a63f7d`
**Baseline probe:** `src/test/probes/gui/LegacyMoneyDecimalSmoke.java`, runner `-MoneyDecimal`
**Reply ID:** `NUM-002`

### Independent user confirmation (not only agent run)

User ran visible JavaFX 25 NUM-001 on Windows. Observed:
- `MoneyStringConverter.fromString("+")` and `fromString("-")` accepted invalid signs as `BigDecimal.ZERO`.
- After valid `JInvMoneyField` amount `12.34`, editing text to `-` yielded `text="-" value=0.00 state=VALUE`, with `textEvents=1, valueEvents=1`. Thus a non-final draft overwrote the last valid amount.
- Independent `grouping=false` roundtrip also RED; deliberately **not in NUM-002**.
- Overall `NUM-001 RED findings=4`. PowerShell throws at runner line 123 because probe exit code is nonzero. This is a correct RED result, not a JavaFX launcher or infrastructure failure.
- Valid direct parsing `1.005`, `-1.005`, long decimals, `0`, comma/dot, spaces/NBSP, verified signed K/T/M suffixes worked. Preserve those observations.

### Confirmed root cause and existing recovery path

`MoneyStringConverter.fromString(String)` initializes `retValue = BigDecimal.ZERO` and accumulates parsed numeric token items. A lone `+` or `-` consumes the sign, produces zero items and returns zero. That then drives a **real typed property change** in `JInvMoneyField`. This is not a rounding problem.

`JInvNumberField.updateValueFromText()` already catches conversion exceptions, sets `State.ERROR` and preserves the previous typed value. Use this existing path, rather than redesigning field binding/listeners or adding a second validation mechanism.

### Assignment: ONE narrow fix

1. Make `MoneyStringConverter.fromString` throw `NumberFormatException` for sign-only input `+` and `-`, including surrounding parser-tolerated spaces (normal and NBSP), **before a value can be read from the parse cache**. An incomplete sign must never be interpreted as zero. Keep blank/whitespace-only input semantics unchanged (`null`). Avoid unnecessary regex / allocations in the per-keystroke hot path.
2. Keep the existing parsing grammar, special suffix behavior and exact `BigDecimal` arithmetic. **Do not** change `MathContext`, BigDecimal scale/rounding, public API, converter caching design, grouping, locale behavior, formatter, or code unrelated to this defect. The current `K -> 0` and `12K34 -> 12034` behavior is observed but its intended contract is UNKNOWN. Do not silently change it in NUM-002.
3. Tighten the existing focused GUI regression to assert separately:
   - Direct parser rejects `+`, `-`, with tolerated spaces around them, with `NumberFormatException`.
   - After a valid money amount, replacing text with either sign yields `State.ERROR`, retains the exact previous typed value, and produces **zero typed-value change notifications** (text event expected).
   - Recovery from ERROR via a valid signed amount works; real zero and valid signed numeric/shorthand input remain valid.
   - Existing `JInvBigDecimalField` cases and unrelated NUM-001 matrix remain intact.
4. **Expected status of full `-MoneyDecimal` run after fix:** three sign-related RED assertions turn GREEN; **one remaining RED for `grouping=false` is expected and acceptable**, so full runner may still exit 1. Do not turn off that check, hide it, or claim overall PASS. Report focused sign invariants GREEN separately from the overall `NUM-001 RED findings=1` if it remains exactly one.

### Scope and quality gates

- Production edits restricted to `src/main/java/ru/inversion/fx/form/controls/MoneyStringConverter.java`. Test/runner edit only when needed for assertions. No POM, Maven wrapper, `JInvMoneyField`, `JInvNumberField`, other controls, `JInvCommon`, or `main` changes.
- Preserve Java 25 / JavaFX 25, source/binary-compatible public and protected API. Comments in new source must be Russian. Prefer tiny diff and no new dependencies.
- Do not spend time on GUI multi-thread safety or memory optimization. Per-instance retained memory remains separate follow-up. Financial data correctness dominates.
- Verify Maven compilation using available settings (on user machine: `D:\Java\.m2\settings.xml`), PowerShell probe script parsing, and visible JavaFX 25 GUI where possible. If GUI cannot be launched in agent environment, report UNKNOWN and use the strongest available isolated regression. Preserve user local work.
- Git operations are pre-approved under project protocol. Verify repo/branch and expected HEAD before write; commit and fast-forward push verified changes to `migration/java25-correctness`, then re-read remote SHA. No force pushes. If diverged, STOP and report.
- Reply in `docs/codex/outbox.md` on `agent/codex-mailbox` as `## Reply NUM-002`, with exact diff/SHA, test matrix, separately named focused GREEN / remaining grouping RED / UNKNOWN, any blockers, and Windows recheck commands:
  ```powershell
  git pull --ff-only origin migration/java25-correctness
  git rev-parse HEAD
  powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\src\test\probes\Test-LegacyGuiSmoke.ps1 -MoneyDecimal -SettingsFile "D:\Java\.m2\settings.xml"
  ```
- **Stop after NUM-002**. Do not fix grouping, rewrite the parser, pursue memory optimizations, or begin NUM-003 until separately assigned.

---

## MESSAGE NUM-003

**Status:** CLOSED — user independently confirmed Windows Java 25 / JavaFX 25 `NUM-001 PASS (tested invariants)`, `$LASTEXITCODE=0` (2026-11-23). Implementation commit `2f88c30e8b46f0eeb51433265b1d9faaaf2d2a54` accepted.
**Priority:** HIGH — formatter/parser roundtrip and financial correctness
**Repo:** `ssu95a/fore25`
**Implementation branch:** `migration/java25-correctness` (never `main`)
**Expected remote HEAD:** `13b60790a434a9d261dde3392342d6184823ac6b`
**Reply ID:** `NUM-003`

### User-confirmed regression, Windows JavaFX 25

- NUM-002 is independently confirmed GREEN: direct sign-only input (including spaces/NBSP) rejected; field preserves prior typed values with `State.ERROR` and zero typed-value events; signed recovery passes.
- The sole outstanding RED in the unchanged MoneyDecimal matrix: `JInvMoneyField.initDefaultMoneyFormatter(2, '.', false)` in `Locale.US` still formats `12345.67` as `12,345.67`. `MoneyStringConverter` parses comma as decimal separator, so formatted string fails at the second separator. Full run: `NUM-001 RED findings=1`; failure in PowerShell runner line 123 is only the expected consequence of nonzero probe exit.
- NUM-002 production commit is accepted. Preserve it.

### Source-level root cause candidate, verify before editing

`JInvMoneyField.initDefaultMoneyFormatter(int fraction, char decimalSeparator, boolean grouping)` creates a new `DecimalFormat`, sets grouping separator to space only when `grouping=true`, and sets grouping size 3, but does not call `setGroupingUsed(grouping)`. Thus `grouping=false` inherits the formatter's locale-specific default grouping behavior. The direct candidate is `g_moneyFormat.setGroupingUsed(grouping)` during formatter construction. Verify it actually removes grouping before accepting the fix.

### One narrow production fix

1. Ensure `grouping=false` disables thousands separators in the shared money formatter, regardless of locale's default grouping choice. Ensure `grouping=true` preserves existing space-separated formatting, scale and decimal separator behavior.
2. Keep `MoneyStringConverter` grammar/cache, exact arithmetic, `JInvMoneyField` static formatter API, rounding, `JInvBigDecimalField` and other field semantics unchanged. Do not rewrite or generalize parsing to accommodate ambiguous comma grouping. No new dependencies.
3. Extend the existing visible `-MoneyDecimal` regression with focused formatter/parser roundtrips for both grouping modes, including `Locale.US`, a comma decimal separator, negative values and a value with at least five integral digits. Preserve existing sign-only, high-precision, suffix, typed-property event and locale assertions. If testing another locale, restore global FORMAT locale and money formatter under `finally`/existing teardown. Make no claim about untested locale output.
4. Confirm on a **previously existing field** that replacing global formatter settings alone does not alter its typed value or generate typed notifications. Avoid introducing an unsolicited text refresh or scale change. Explicitly distinguish formatted display behavior from underlying `BigDecimal`.
5. Target result: **all assertions in `NUM-001` GUI smoke PASS, `NUM-001 PASS (tested invariants)` and exit code 0**. If any unexpected regression appears, do not mask or relax checks; report RED/UNKNOWN.

### Scope, verification and publication

- Production edits only in `src/main/java/ru/inversion/fx/form/controls/JInvMoneyField.java`; test edits may touch `src/test/probes/gui/LegacyMoneyDecimalSmoke.java` only if required. Do not touch `MoneyStringConverter.java`, runner, POM, Maven wrapper, `main` or `JInvCommon`.
- Preserve public/protected API and constructors; source comments in Russian. GUI concurrency stress and per-instance memory tuning are **out of scope**. Do not begin NUM-004 or deferred GUI/LOV tasks.
- Run available Java 25 / JavaFX 25 compilation and visible GUI smoke using existing Maven settings. If no real GUI session, mark runtime UNKNOWN and give an independent user command; no deceptive PASS.
- All ordinary Git/GitHub operations are APPROVED under the mailbox protocol. Verify repo, expected branch HEAD, and clean relevant changes. Commit a minimal tested diff, fast-forward push to `migration/java25-correctness`, verify remote SHA; never force or overwrite user changes.
- Publish `## Reply NUM-003` into `docs/codex/outbox.md` on `agent/codex-mailbox` with exact commit/SHA, source evidence, comparison of old/new behavior, test summary and clear PASS/RED/UNKNOWN. Include user rerun command:

```powershell
git pull --ff-only origin migration/java25-correctness
git rev-parse HEAD
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\src\test\probes\Test-LegacyGuiSmoke.ps1 -MoneyDecimal -SettingsFile "D:\Java\.m2\settings.xml"
```

**Stop after NUM-003.** Retained per-field memory work is a separate future assignment.

---

## MESSAGE NUM-004

**Status:** CLOSED — user independently confirmed Windows NUM-001 PASS (tested invariants) after ca05ae3c991973f48269542309041fd3eaf426ec (2026-11-23); code review accepted. Retained-byte reduction UNKNOWN.
**Priority:** MEMORY — reduce unused objects per GUI numeric field; preserve correctness
**Repo:** `ssu95a/fore25`
**Work branch:** `migration/java25-correctness` (never `main`)
**Expected remote HEAD:** `2f88c30e8b46f0eeb51433265b1d9faaaf2d2a54`
**Reply ID:** `NUM-004`

### Confirmed baseline and scope decision

The user independently reran Java 25 / JavaFX 25 MoneyDecimal after NUM-003: `NUM-001 PASS (tested invariants)`, PowerShell `$LASTEXITCODE=0`. All tested sign-only, money grouping, rounding, long precision, locale, property notification and recovery scenarios remain GREEN. Do not undo this baseline.

The user explicitly prioritizes **retained memory per GUI control** over speculative UI concurrency. Source and NUM-001 warm-batch probe observed 100 distinct *inherited default* `JInvNumberField.stringConverter` objects across 100 `JInvBigDecimalField` and also 100 across 100 `JInvMoneyField`, although these numeric subclasses override `getConverter()` and use their own converters. In `JInvNumberField`, the field is initialized with `new TypeStringConverter<>(Number.class)` once per instance.

`TypeStringConverter<P>` stores only a final `Class<P> clazz`; `toString`, `fromString` delegate to `TypeConverter`, and `getType` returns the class. No mutable per-converter state is evident in its source. This makes sharing the default converter a **candidate**, not a proven safe change until actual behavior and usage are checked.

Do not mistake `AbstractBigDecimalField.valueProperty` shadowing for a second allocated value property: the parent slot is not allocated in the observed numeric subclasses. Likewise the `JInvBigDecimalField` converter is nonstatic because formatting uses its owning field's settings; **do not** replace it with a global converter in this task.

### Assignment: one measured, compatible optimization

1. Establish a reproducible baseline using the existing `-MoneyDecimal` GUI smoke and read-only code/usage review. Verify actual converter identity, concrete `JInvNumberField` access and public `setConverter` semantics. Search for any in-repo consumer that depends on the distinct identity or mutability of the default converter. External corporate callers UNKNOWN.
2. If the source/usage evidence supports it, replace per-instance construction of the **default** `TypeStringConverter<>(Number.class)` in `JInvNumberField` with a shared immutable default (for example, one private static final instance), while preserving the per-field converter slot and `setConverter` override semantics. The singleton must not override custom converters, change access modifiers or API signatures, or change subclass dispatch.
3. Extend the same visible JavaFX 25 regression with memory *identity* assertions: 100 used Decimal and 100 Money fields should now refer to **one** inherited default converter, not 100; the independently required actual decimal converter and typed property instances remain distinct; actual Money converter remains shared. Cover two ordinary `JInvNumberField` instances and `setConverter` isolation (custom on A does not change B), without assuming exact retained byte sizes.
4. Keep all existing MoneyDecimal correctness tests. Compare baseline versus changed GUI probe on Java 25 / JavaFX 25 and report original/optimized inherited converter identity counts, overall PASS and exit 0. Report retained bytes as UNKNOWN unless measured with suitable tooling. Do not claim overall footprint savings in exact byte counts from identities alone.
5. If any relevant consumer relies on converter identity or mutation, or a behavior-preserving solution is not established, **STOP before production edit**; report BLOCKED with evidence rather than changing contract silently.

### Strict boundaries

- Only permitted production file: `src/main/java/ru/inversion/fx/form/controls/JInvNumberField.java`; test expansion only in `src/test/probes/gui/LegacyMoneyDecimalSmoke.java` as needed. Do not change `TypeStringConverter`, other field implementations, wrapper/POM, parser/formatters/caches, `main` or `JInvCommon`.
- No change to public/protected signatures or behavior, numeric parse/rounding semantics, constructor init/late initialization, binding, events, display, error recovery or custom-converter assignment. Comments in new source must be Russian.
- GUI concurrency, unrelated stale weak references, locale cache key, memory leak claims, and general refactors are outside NUM-004.
- Keep all existing smoke flags and original assertions. Verify PowerShell parsing, Maven `-Pbuild` compile, probe javac and **visible** JavaFX 25 smoke if possible. If no real GUI session, report runtime UNKNOWN, don't claim GREEN.
- All ordinary Git/GitHub operations are pre-approved. Verify repo/branch/HEAD and preserve local user work. Commit/push only verified minimal change via fast-forward to `migration/java25-correctness`; verify remote SHA, no force.
- Publish `## Reply NUM-004` to `docs/codex/outbox.md` on `agent/codex-mailbox` with exact diff/commit, baseline/after memory identities, compatibility considerations, PASS/UNKNOWN, and reproducible user command:

```powershell
git pull --ff-only origin migration/java25-correctness
git rev-parse HEAD
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\src\test\probes\Test-LegacyGuiSmoke.ps1 -MoneyDecimal -SettingsFile "D:\Java\.m2\settings.xml"
$LASTEXITCODE
```

**Stop after NUM-004.** Do not begin NUM-005 or additional memory optimization without a new assignment.

---

## MESSAGE NUM-005

**Status:** CLOSED — read-only investigation completed: retain lazy per-instance JInvBigDecimalConverter; no safe retained object reduction proven. Locale-key cache reuse was demonstrated under runtime FORMAT locale change; first dead WeakReference index bug REJECTED; remote production remains ca05ae3c991973f48269542309041fd3eaf426ec.
**Priority:** MEMORY — per-field allocations in JInvBigDecimalField
**Repo:** ssu95a/fore25
**Read-only production branch:** migration/java25-correctness
**Expected production HEAD:** ca05ae3c991973f48269542309041fd3eaf426ec
**Reply ID:** NUM-005

### Starting point

NUM-004 user-confirmed GREEN. Sharing the unused inherited default TypeStringConverter reduced its identities to one across 100 Money and 100 BigDecimal instances. Do not change this baseline.
NUM-001/004 memory probe still shows 100 actual JInvBigDecimalConverter instances for 100 used BigDecimal fields, sharing one DecimalFormat under common settings. The converter is a nonstatic inner class whose toString calls its owning field's formatter(). It therefore depends on field-specific configuration. JInvMoneyField uses a static converter; Percent extends BigDecimalField with a separate scale default.

### Assignment: read-only proof, not a speculative optimization

1. Audit actual retained objects and public/protected contracts in JInvBigDecimalField, AbstractBigDecimalField, JInvNumberField, JInvPercentField and TypeStringConverter. Distinguish own converter, formatter, fractionDigitsProperty/listeners from shared JavaFX TextField baseline; identify concrete call sites and overrides.
2. Establish why the actual decimal converter must access its owner under dynamic fractionDigits/grouping/locale. Check two live fields with differing settings and getConverter().toString(value), and effects of changing those settings. Preserve Percent semantics. Do not conflate sharing a converter with sharing a DecimalFormat.
3. Compare realistic alternatives, including KEEP per-instance converter. A stateless shared converter is not a viable fix if field-specific formatting and getConverter() behavior cannot be preserved. Specify risk, compatibility and expected retained-memory benefit for each.
4. Quantify per-field retained bytes only with suitable instrumentation (JOL/JFR/heap tools) if already available, no new dependencies. Identity counts are evidence of object count, NOT bytes. No Runtime.totalMemory()-freeMemory() retained-memory claims; no aggressive GC on live controls.
5. Separately assess weak DecimalFormat cache source-level risks: cache lookup key omits FORMAT locale; dead weak-reference slot indexing may be incorrect. Clearly distinguish demonstrated defect, hypothesis and UNKNOWN. Do not implement fixes or broaden task to cache redesign or concurrency.
6. Propose ONE narrowly scoped follow-up experiment or change with RED/GREEN acceptance criteria, or recommend retaining current per-instance converter if no safe memory gain is established.

### Constraints and reporting

READ ONLY production branch. No production edits, test commits, POM/wrapper changes, or changes to main or JInvCommon. Existing MoneyDecimal tests must remain GREEN if run. All standard Git/GitHub operations approved only for reading and publishing the mailbox reply.
Publish ## Reply NUM-005 to docs/codex/outbox.md on agent/codex-mailbox with source and usage evidence, per-field object analysis, confirmed vs UNKNOWN, proposed smallest next step. Do not push to implementation branch. Stop after report; no NUM-006 without a new assignment.

---

## MESSAGE CTRL-001

**Status:** CLOSED — agent read-only audit complete (Java 25 / JavaFX 25); plain DatePicker silent write failure independently reproduced by agent on actual DSControlAdapter + ArrayDataSet, production unchanged. Report published in outbox. First follow-up: CTRL-002.
**Priority:** CORRECTNESS / DATA INTEGRITY > LIFECYCLE / MEMORY > PERFORMANCE > JavaFX 25 COMPATIBILITY
**Repo:** ssu95a/fore25
**Working code branch (READ ONLY):** migration/java25-correctness
**Expected production HEAD:** ca05ae3c991973f48269542309041fd3eaf426ec
**Reply ID:** CTRL-001

### Reason and boundaries

User requests assessment of Controls after JInvMoneyField and JInvBigDecimalField. NUM-001 through NUM-004 independently user-confirmed GREEN; NUM-005 recommends retaining the owner-bound lazy decimal converter. Do not broaden this task back into the numeric fields. A separate confirmed cache locale-key mismatch from NUM-005 is recorded for a later task, not a Controls fix.

Audit the existing almost-1000-line static helper `src/main/java/ru/inversion/fx/form/controls/Controls.java` at the current HEAD. The purpose is to identify actual correctness, integrity, lifecycle, memory and JavaFX 25 migration risks, with evidence and the smallest next targeted test. Do NOT refactor, optimize or edit code in this task.

### Analysis axes and candidates to investigate (hypotheses, NOT accepted defects)

1. **Values and data integrity:** trace `Controls.setValue(Object,Object)`, `getValue(Object)`, `getStringValueFromEditor(Node)` with `JInvValueField`, generic FX controls, `ICustomValueControl`, `Binder`, `TypeConverter`, null, ERROR state and typed property. Identify asymmetries, unsupported cases, type conversions and observed runtime vs source-only suspicion. Preserve distinction between typed value and invalid editor draft. Include call sites and untested external usage as UNKNOWN.
2. **Read-side effects and memory:** `isControlValueChanged(Node)` delegates to `getValueChangedProperty(Node)` which may allocate and attach a `SimpleBooleanProperty` on first read. `getNonControlComponentFromContainer(Parent)` creates a new ArrayList and installs a property even when only reading. Verify lazy allocation semantics, actual call patterns, retention/lifecycle and cost per control; do not infer leaks or exact bytes from object count.
3. **Disable/enable lifecycle:** inspect `disableControl` routing and event-filter add/remove, repeat operations, normal mode and bound visible/editable properties. Check whether toggling restores preexisting user settings versus overwriting them. Do not assume an error without a realistic supported scenario.
4. **Traversal and performance:** `getControlList`, `getNodeList`, `getNodeByFieldName`, `getParentStreamOfControl` and column helpers; inspect nested TabPane/SplitPane/ScrollPane/TitledPane, excluded composite controls, null children, visit order, duplicate nodes, linked-list vs array-list and per-call allocations. Test suspected correctness problems before proposing speed refactors. Preserve intentionally excluded controls unless confirmed wrong.
5. **JavaFX 25:** `setTooltipStartTiming` uses reflection into private Tooltip BEHAVIOR and activationTimer; assess exact supported JavaFX 25 API/options, actual in-repo usages and real reproducible failures (module access) vs unproven risk. Do not broaden to general JavaFX internal API migration or change module settings.
6. **Other high-risk call paths:** type→control construction (`getControlByClass`), controller lookup, property name parsing / clear logic, title resolution in TableView, simple get/set property. Prioritize any strong evidence of loss of data, wrong type or premature allocations, but do not attempt an exhaustive cosmetic rewrite.

### Evidence and report

- First map public static methods into functional groups and their in-repo callers. Distinguish CONFIRMED via executing actual JavaFX 25 scenario, SOURCE-PROVEN (path follows directly from code), HYPOTHESIS and UNKNOWN. Mark missing BaseApp/DB-dependent runtime explicitly.
- If practical, run **untracked** small probes under ignored target with JDK25/JavaFX25 and actual FX controls (no fake business framework). Avoid new tracked files, POM changes or external dependencies. Existing GUI regression must remain unchanged.
- Report prioritized findings in a compact table: method(s), severity, concrete input/call path, expected vs actual, impact on data/memory/performance, evidence quality, and the **one smallest next regression/fix task**. For memory, separate per-control retained references, global/static caches, and temporary allocations; do not use JVM free-memory deltas as measurements.
- Keep public/protected API compatibility and FX thread assumptions. Do not choose stylistic cleanup over correctness and memory. Existing deliberate behavior may look unusual: distinguish contractual intent and UNKNOWN from bugs.

### Git and stop conditions

READ ONLY on migration/java25-correctness, main and JInvCommon. No implementation or test commits, no POM/wrapper changes, no code write to ANY production branch. Standard Git/GitHub operations APPROVED for reading and the mailbox report only.
Publish `## Reply CTRL-001` into docs/codex/outbox.md on agent/codex-mailbox with exact source commit, scope, findings, evidence, and recommended next narrow task. Verify mailbox remote SHA. **STOP after CTRL-001**; no implementation, no CTRL-002 until assigned.

---

## MESSAGE CTRL-002

**Status:** CLOSED — user independently confirmed Windows visible JavaFX 25 `CTRL-002 PASS` (2026-11-23). Implementation commit `dafb93d64e2a1a2d8cd47589aa6c672e574c35ca` accepted; full BaseApp/DB persistence outside tested scope.
**Priority:** HIGH — prevent stale DatePicker value on DataSet navigation / null clear
**Repo:** ssu95a/fore25
**Implementation branch:** migration/java25-correctness (never main)
**Expected remote HEAD:** ca05ae3c991973f48269542309041fd3eaf426ec
**Reply ID:** CTRL-002

### Confirmed defect, not a hypothetical root cause

CTRL-001 read-only GUI probe on Java 25.0.1 / JavaFX 25 reproduced a plain `javafx.scene.control.DatePicker` write omission in `Controls.setValue(Object,Object)`: after an initial 2025-01-02, write 2025-03-04 and subsequent null clear returned normally but retained 2025-01-02. A real `DSControlAdapter.bindControl(picker, null, row -> row)` and in-memory ArrayDataSet navigated twice; the mapping callback ran but the DatePicker showed the old date. Actual DB save and third-party plain DatePicker usage remain UNKNOWN. Agent's exact report is `## Reply CTRL-001` in mailbox outbox.

Source-level cause: `Controls.setValue` accepts `control instanceof DatePicker` but writes only if also `JInvCalendarTime` or `JInvCalendar`; ordinary DatePicker falls out silently. `Controls.getValue` already reads ordinary DatePicker. This is a correctness/integrity concern, not a presentation-only problem.

### One narrow fix

1. Verify the starting HEAD and inspect current source/actual `DSControlAdapter` call path. Create a focused automated **RED** Java 25 / JavaFX 25 regression for an ordinary DatePicker through both `Controls.setValue` and real `DSControlAdapter` with in-memory ArrayDataSet. Assert typed `LocalDate` after row navigation and `null` clear. Prove failure before production edit.
2. In `Controls.setValue` DatePicker branch only: retain the `JInvCalendarTime` path first, preserving `LocalDateTime` and its time component; for other DatePicker instances use existing `TypeConverter.convert(value, LocalDate.class)` and `DatePicker.setValue(...)` (thereby preserving JInvCalendar), with the narrowest readable diff. No new public API or unnecessary cast/branch rewriting.
3. Add sentinels for `JInvCalendar` LocalDate and `JInvCalendarTime` LocalDateTime (nonzero time, null clear if supported) where a legitimate standalone fixture can construct them. If a Legacy calendar needs BaseApp/bootstrap, mark that part UNKNOWN and do not fabricate a fake application context.
4. Check conversion failure cannot silently clear/replace a previously valid typed value, and that same-value assignment does not create a spurious value event. Keep `Controls.getValue` typed read behavior, editor-string distinction, untouched.
5. Retain broad correctness coverage: MoneyDecimal current user-confirmed GREEN, and any existing controls/GUI regressions compatible with the environment. Do not loosen old assertions or alter their baselines.

### Scope / acceptance

- Production modification allowed ONLY in `src/main/java/ru/inversion/fx/form/controls/Controls.java`. Test-only changes may extend `src/test/probes/Test-LegacyGuiSmoke.ps1` and add/modify a dedicated GUI probe under `src/test/probes/gui/`. No changes to other production, POM, wrapper, dependencies, main or JInvCommon.
- Preserve all public/protected signatures, constructor and DatePicker/Legacy semantics. New source comments in Russian. Keep GUI memory, getNodeList, disableControl, tooltip reflection, column metadata out of this task.
- On real visible JavaFX 25 Stage if available: targeted RED before and GREEN after on direct + ArrayDataSet/DSControlAdapter date/null scenarios. Compile with existing Maven custom settings; keep PowerShell parsing and all previous dispatch modes working. If full visible GUI unavailable, report exactly what is UNKNOWN, not a claimed PASS.
- All ordinary Git/GitHub operations APPROVED under project protocol: verify repo and branch SHA, preserve unrelated local changes, make small reviewed commit and fast-forward push to migration/java25-correctness, verify remote SHA, no force or destructive update.
- Publish `## Reply CTRL-002` in docs/codex/outbox.md on agent/codex-mailbox: failure mechanism, exact diff/commit/SHA, direct and Dataset RED/GREEN, Legacy sentinels PASS/UNKNOWN, Windows recheck command with `-SettingsFile "D:\Java\.m2\settings.xml"`, plus any limitations.
- Stop after CTRL-002. Do NOT start CTRL-003 or unrelated fixes.

---

## MESSAGE CTRL-003

**Status:** CLOSED — independent user Windows JavaFX 25 regression `CTRL-003 PASS`, exit code `0` (2026-11-23). Exact implementation `c21a17e0b2ba8305a02d405449052f9aa0b5b607` accepted; only tested traversal matrix confirmed.
**Priority:** MED-HIGH — null Node leakage and predicate exception in Controls.getNodeList
**Repo:** ssu95a/fore25
**Implementation branch:** migration/java25-correctness (never main)
**Expected remote HEAD:** dafb93d64e2a1a2d8cd47589aa6c672e574c35ca
**Reply ID:** CTRL-003

### Independent prerequisite

CTRL-002 has user-confirmed `CTRL-002 PASS` on Windows. Preserve the DatePicker change, and existing MoneyDecimal, DataBinding and other accepted GUI behavior. No changes beyond this task's scope.

### Confirmed defect (CTRL-001)

`Controls.helperGetListNodeInternal(List<Node>, Node, Predicate<Node>)` invokes predicat.test(n) before any null guard. In a live JavaFX 25 fixture, an empty TabPane Tab and an empty ScrollPane content caused `Controls.getNodeList` with the default/all predicate to return null elements. A predicate like `node -> node.getId() != null` threw NullPointerException on those absent contents. Source paths include Tab.getContent(), ScrollPane.getContent() and TitledPane.getContent(), which may legitimately be null. No production business crash was proven, but the public helper violates expected list-of-existing-nodes behavior.

### One narrow fix

1. Verify repo, work branch, current HEAD, and reproduce the exact source issue as **RED** in a focused Java 25 / JavaFX 25 probe with real controls. Cover an empty TabPane tab, an empty ScrollPane, an empty TitledPane, plus a nonempty nested node; include both null/default predicate and a predicate that dereferences node.getId(). Demonstrate what happens before any production edit.
2. Modify ONLY `helperGetListNodeInternal` in `Controls.java` to skip null `Node` before applying predicate or adding to results. This is the smallest candidate. Preserve depth-first walk, visit order, container inclusion and existing exclusions (calendar/spinner/table/editor internals), explicit custom predicate behavior on actual nodes, and public signatures. Avoid a general rewrite of traversal or collection type.
3. Regression GREEN: no null elements, no NPE on empty container branches, real nested nodes visited once in the fixture with their established order, predicate filters only actual nodes, `getNodeByFieldName` still works including duplicate-match detection. Compare with `getControlList` on same fixture where relevant. If a broader issue is observed, document it separately rather than broadening the fix.
4. Retain unrelated `CTRL-002 -DatePickerBinding` and `NUM-001 -MoneyDecimal` PASS. Use existing GUI runner conventions; a new focused runner mode (`-NodeTraversal`) and standalone test under src/test/probes/gui are permitted. No BaseApp/DB required. Do not fabricate coverage of full forms or dynamic FXML trees.
5. If an existing consumer demonstrably relies on null entries, stop and report compatibility risk rather than changing its contract silently. No evidence currently establishes that null nodes are intentional.

### Strict scope / Git

- Allowed production file: `src/main/java/ru/inversion/fx/form/controls/Controls.java`, only targeted node traversal helper. Test changes restricted to `src/test/probes/Test-LegacyGuiSmoke.ps1` and new/focused `src/test/probes/gui/*` regression. No other production, Maven POM/wrapper, dependencies, main or JInvCommon modifications.
- No performance redesign, caching, retained memory optimization, `disableControl`, `Tooltip` reflection or other CTRL-001 findings in this task. All source comments added must be Russian.
- Require independent RED/GREEN with Java 25 / JavaFX 25 (visible Stage if available), Maven -Pbuild compile with existing Maven settings, PowerShell runner parsing and no regression in existing `DatePickerBinding` and `MoneyDecimal`. Document environment/runtime UNKNOWN honestly if blocked.
- Git/GitHub actions are APPROVED under project protocol. Commit and fast-forward push only verified, minimal code/test changes to migration/java25-correctness after verifying expected HEAD; no force. Verify remote SHA.
- Publish `## Reply CTRL-003` in docs/codex/outbox.md on agent/codex-mailbox with exact commit and diff, RED/GREEN cases, verified branch SHAs, caveats, and reproducible Windows command with SettingsFile `D:\Java\.m2\settings.xml`.
- Stop after CTRL-003. Do not begin CTRL-004 without separate assignment.

---

## MESSAGE CTRL-004

**Status:** CLOSED — read-only Windows Java 25 / JavaFX 25 investigation completed; found bound-property exceptions, potentially ineffective readOnly transitions and overlapping JInvRadioGroup/JInvRadioButton ownership. Production unchanged at c21a17e0b2ba8305a02d405449052f9aa0b5b607. User clarified: readOnly=true forbids user-originated value edits, not programmatic updates.
**Priority:** CORRECTNESS / GUI STATE / BINDINGS and lifecycle
**Repo:** ssu95a/fore25
**Code branch:** migration/java25-correctness (READ ONLY)
**Expected production HEAD:** c21a17e0b2ba8305a02d405449052f9aa0b5b607
**Reply ID:** CTRL-004

### Context

CTRL-003 accepted after independent Windows JavaFX 25 `CTRL-003 PASS`, exit 0. We are continuing the Controls audit. CTRL-001 reported that `Controls.disableControl(Node,boolean)` throws `A bound value cannot be set` for `TextInputControl` with bound editable property, while a Button with bound visible stays visible. More subtly, `enableControl` currently forces editable/visible/focusTraversable rather than restoring prior state. Whether this is an intentional contract is unresolved. Do not silently turn this into a general preservation/restoration feature.

### Read-only assignment

1. Trace all in-repository uses of `Controls.disableControl`, `enableControl` and `disableControl(Node, boolean)` and callers that own multiple disable reasons. Trace `JInvTextField` special behavior, `TextInputControl`, `DatePicker`, `Button`, `ChoiceBox`, `ComboBoxBase`, JInvXMLViewer and bypassed containers. Record source-level intended semantics and public API dependencies, not just class branches.
2. With actual Java 25 / JavaFX 25 controls on the FX thread, reproduce four independent dimensions: (a) unbound initially editable/visible/focusTraversable false states, (b) corresponding bound properties, (c) repeated disable/enable and event filters, (d) nested or overlapping disable reasons if supported by real call sites. Observe exact property values, binding state, exceptions, and actual click/editability behavior where possible. Do not invent unsupported application-level ownership protocols.
3. Distinguish `setDisable(true)` from the existing specialized editable/visible/focus policy. Do NOT recommend blindly replacing it with Node.disableProperty; applications might rely on the current visual and keyboard semantics.
4. Compare a minimal fail-fast or skip-bound-property handling policy with preserving previous state or caller-managed restoration. Discuss integrity/UX/compatibility and per-control retained allocations. In particular a cache of original state on every node has memory consequences. If a safe narrow change can be proven, propose one RED/GREEN task; otherwise state the single decision that requires user approval.
5. Check for JavaFX 25 API limitations on bound properties; avoid general concurrency stress. Distinguish CONFIRMED runtime findings, SOURCE-PROVEN call paths, HYPOTHESIS and UNKNOWN. Any standard form/DB validation not runnable is UNKNOWN.

### Constraints and report

READ ONLY production and tests. Temporary untracked experiments under ignored target are allowed. No POM/wrapper/dependency changes, no commits to production branch, main or JInvCommon. All GitHub/Git operations APPROVED only for reading and posting the mailbox reply. Do not begin CTRL-005.
Publish `## Reply CTRL-004` in docs/codex/outbox.md on agent/codex-mailbox, with method/caller map, reproduction matrix, state ownership alternatives, priority and ONE narrow next step with exact testing criteria. Verify mailbox SHA, working branch SHA and unchanged production.

---

## MESSAGE CTRL-005

**Status:** CLOSED — user contract confirmed and agent Windows Java 25 / JavaFX 25 Robot/targeted-event audit complete. Focused radio Space and partial release modified bound model during readOnly; programmatic model-to-radio updates worked. Production unchanged at c21a17e0b2ba8305a02d405449052f9aa0b5b607. Follow-up CTRL-006 assigned.
**Priority:** HIGH — user-input suppression and DataSet/model value integrity
**Repo:** ssu95a/fore25
**Production code branch:** migration/java25-correctness (READ ONLY)
**Expected production HEAD:** c21a17e0b2ba8305a02d405449052f9aa0b5b607
**Reply ID:** CTRL-005

### User's explicit architectural contract

**`readOnly=true` suppresses USER-ORIGINATED VALUE MODIFICATION.** Programmatic writes (setValue, model binding, DataSet row navigation / refresh, programmatic radio selection) must remain available. Do NOT interpret readOnly as blanket native Node.disable: keyboard focus, selection, clipboard copy, navigation, and ability to inspect data may remain available. Do not confuse disabling a button with invalidating all programmatic actions; programmatic Button.fire is NOT proof of a physical user click.

An effective `readOnly=true` must prevent interaction paths that change the value, not merely set a cosmetic pseudo-class. If two distinct, supported owners impose readOnly (e.g. radio's own property and JInvRadioGroup), release of one owner's restriction must not give the user the ability to modify a value while the other owner remains readOnly. Whether this is currently represented by the API, and minimal safe composition, require evidence. Existing enable forcing state rather than restoring historical state is documented: do NOT invent a general snapshot/restore contract.

### Source + runtime investigation (no production edit)

1. Trace actual production readOnly paths and public behavior in Controls.disableControl, JInvTextField/JInvTextArea, JInvCalendar/CalendarTime, JInvComboBox/JInvChoiceBox, JInvRadioButton/JInvRadioGroup, JInvCheckBox and button types. Identify actual user interaction entry points (text editing, menu/popup selection, shortcut keys, space/enter, mouse, toggle action). State call sites and any relevant bound-property ownership.
2. In a visible JavaFX 25 Stage on FX application thread, build a focused behavior matrix with representative controls. For each, compare normal input and `readOnly=true` for mouse, keyboard, and popup selection. Use genuine user-style event routing where possible; distinguish precisely `Event.fireEvent`, `Button.fire()`, `ToggleGroup.selectToggle`, direct setters, and physical input. **Never claim a physical key/mouse was tested if only synthetic events or direct method calls were used.**
3. Test programmatic value change after readOnly=true without disabling value/model propagation, with event/value counts. Include a real standalone in-memory DataSet/adapter when practical. Avoid requiring full BaseApp/DB; report those pathways UNKNOWN if unavailable.
4. Reproduce the concrete CTRL-004 bound editable/focus/visible findings and their observable impact on effective input suppression. A thrown bound-property exception and effective unblocked edit are different findings. Test a binding whose source changes dynamically, record whether bindings are intact and whether input is still accepted.
5. Prioritize overlapping `JInvRadioGroup` + `JInvRadioButton` readOnly cases: both true, release group only, release radio only, programmatic selection while locked, then full unlock. Check effective user input separately from readOnly property, focusTraversable, mouseTransparent and pseudo-class. Record whether group binding/FXEntity value changes. Inspect realistic overlapping owner call sites; do not assume reference counting is part of existing contract.
6. Where native JavaFX skins or accessibility paths make true keyboard/mouse simulation unreliable, document the limitation and recommend an independent manual Windows physical-input matrix rather than claiming PASS. Explicitly keep focus/copy/selection behaviors distinct from value editing.
7. Report precise CONFIRMED runtime facts, SOURCE-PROVEN paths, HYPOTHESES and UNKNOWN. Rank by likely silent user value corruption / programmatic regression first, then UX and memory overhead. Recommend ONE minimal RED/GREEN correction task with acceptance criteria and compatibility impact; if a policy decision on multiple owners is essential, state the decision explicitly before any production work.

### Scope / Git / reporting

- This is a READ-ONLY audit. Do NOT modify production, tracked tests, API, module flags, POM, wrappers, dependencies, main or JInvCommon. Temporary untracked probes under ignored target are allowed. Do not optimize memory or rewrite Controls, nor begin CTRL-006.
- Reuse JDK 25.0.1 / JavaFX 25 and existing Maven settings. Do not fabricate coverage of full business forms, DB saves or physical interaction. Keep all previously accepted regression baselines unchanged.
- All normal Git/GitHub operations are APPROVED only for reading and posting the mailbox report. Verify exact branch HEADs. Post `## Reply CTRL-005` in docs/codex/outbox.md on agent/codex-mailbox with full matrix, source/call-site evidence, risks, UNKNOWN, and one recommended narrow next action. No implementation branch push. STOP after report.

---

## MESSAGE CTRL-006

**Status:** CLOSED — independent user Windows CTRL-006 PASS after commit 6797a24554d40026025e49cd26d73f8503f93cd5 (2026-11-23). Agent also ran exclusive Windows Robot baseline/fix RED/GREEN; independent user reported PASS without identifying Robot mode. Programmatic selection invariants passed in focused tests.
**Priority:** CRITICAL — prevent user-originated radio edits and bound model writes under readOnly
**Repo:** ssu95a/fore25
**Implementation branch:** migration/java25-correctness (never main)
**Expected remote HEAD:** c21a17e0b2ba8305a02d405449052f9aa0b5b607
**Reply ID:** CTRL-006

### Established user contract

readOnly=true suppresses USER-originated value changes while preserving programmatic control/model updates (group.valueProperty, ToggleGroup.selectToggle, selected property via model, DataSet navigation). Do not substitute native Node.disable globally, block model propagation, or treat programmatic selectToggle as a forbidden user edit. Focus, navigation and copy are not automatically prohibited.

### Confirmed CTRL-005 RED

Real JavaFX 25 shown Stage on Windows, targeted input and exclusive Robot rerun: when radio B was focused before both its own and its JInvRadioGroup readOnly became true, Robot Space selected B and updated the bidirectionally bound SimpleStringProperty model once; direct mouse click remained blocked in both-locked state, showing independent keyboard defect. If group readOnly was released while radio remained readOnly, OR radio readOnly released while group remained readOnly, Robot click and Space each selected B and wrote the bound model. Explicit programmatic selectToggle(B) and group model writes did work while locked, as they must continue to do. No physical hand-operated input or real DB persistence was tested.

Source: JInvRadioButton's readOnly listener calls Controls.disableControl(this, flag); JInvRadioGroup's readOnly listener separately iterates toggles and calls Controls.disableControl on the same radio. Thus last-writer GUI state conflicts. Controls changes focusTraversable and mouseTransparent but focused Space still activates a radio.

### Scope and acceptance

1. Reproduce RED with a focused, **tracked** Java 25/JavaFX25 GUI regression before production modification. Use actual JInvRadioButton, JInvRadioGroup, a bidirectionally bound SimpleStringProperty model, shown Stage, and input delivered through realistic JavaFX/Robot paths. The first positive unlocked control must work; distinguish targeted Event.fireEvent, Robot and any manual physical input. Do not infer real mouse hit testing from an event dispatched directly to a mouseTransparent Node.
2. Define effectiveReadOnly(B) = B.readOnly OR (B.current ToggleGroup instanceof JInvRadioGroup AND group.readOnly). Ensure the effective state recomputes on either property transition, attach/detach/reassign of group, and on existing toggles when group readOnly changes. When one owner releases while the other is still locked, effective readOnly stays true. No stale listeners or retention of detached group; avoid unnecessary per-control objects or broad snapshot/restore. Preserve the documented Controls enable semantics outside this targeted composition.
3. Block **user-originated value-changing activation before selection and model notifications**, including focused Space, mouse and other supported activation keys. A cosmetic CSS state, focusTraversable=false, mouseTransparent alone, or reverting selection after an emitted value event is insufficient. Preserve programmatic ToggleGroup.selectToggle, group.setValue/FXEntity model-to-GUI updates and direct selected property writes while locked. Programmatic RadioButton.fire() vs real user action may be ambiguous; investigate that distinction and do not silently change it without documenting compatibility implications. Avoid indiscriminately swallowing unrelated navigation/focus events.
4. Acceptance matrix: A initially selected, B focused before lock; both locked, group-only locked, radio-only locked, each partial release, fully unlocked; for every locked variant no user-originated change to selected toggle, group value and model property, zero model notifications. Full unlock: user activation selects B once, exactly one model change. Model→radio and selectToggle(B) while locked must change selection as requested without false ERROR or suppressed update. Include reassign/detach to a new/old group and repeat toggles for listener lifecycle; native ancestor-disabled state must remain effective.
5. If a safe user-action gate respecting programmatic writes or group lifecycle cannot be proven, STOP before production commit, report BLOCKED with exact evidence. Do not weaken RED assertions merely to obtain GREEN.
6. Retain earlier accepted -NodeTraversal (CTRL-003), -DatePickerBinding (CTRL-002) and -MoneyDecimal (NUM-001) regression PASS. Check any relevant existing radio/GUI-009 fixture. Mark actual business form/DB and untested physical input as UNKNOWN.

### Strict limits and Git

- Production modifications ONLY in src/main/java/ru/inversion/fx/form/controls/JInvRadioButton.java and JInvRadioGroup.java if both needed. Do NOT change Controls.java, JInvCheckBox, ComboBox, ChoiceBox, Calendar, Button, Binder, other production, POM/wrapper/dependencies, main or JInvCommon. Test-only change may extend Test-LegacyGuiSmoke.ps1 and add a focused probe in src/test/probes/gui.
- Preserve public/protected API signatures, constructors, bindings, property identity and JavaFX 25 compatibility. New source comments in Russian. Keep the smallest compatible diff; if extra owner/listener state increases retained memory, account for it and justify it.
- Maven/JDK25 GUI RED/GREEN on Windows if available; PowerShell parse and compilation. No speculative concurrency stress. Verify repo, current SHA, and clean relevant changes. ALL regular Git/GitHub operations APPROVED within task: commit verified code, fast-forward push to migration/java25-correctness, verify remote SHA; no force push.
- Publish ## Reply CTRL-006 in docs/codex/outbox.md on agent/codex-mailbox: root cause, exact diff/commit/SHA, RED/GREEN matrix, keyboard/Robot vs physical evidence, programmatic selection invariants, group listener lifecycle, byte-memory UNKNOWN, and independent user command with -SettingsFile D:\Java\.m2\settings.xml.
- STOP after CTRL-006; no broader readOnly fixes until separately assigned.

---

## MESSAGE CTRL-007

**Status:** CLOSED — independent Windows Java 25 / JavaFX 25 user-confirmed `CTRL-007 PASS`, `$LASTEXITCODE=0` (2026-11-23). Production `cf7b05c45319624f3f0134a78d59410484829af9` accepted for tested invariants. Robot post-fix, external bound inner-editor editable and programmatic show contract remain separately UNKNOWN.
**Priority:** HIGH — prevent user-originated value updates while readOnly=true
**Repo:** ssu95a/fore25
**Implementation branch:** migration/java25-correctness (never main)
**Expected remote HEAD:** 6797a24554d40026025e49cd26d73f8503f93cd5
**Reply ID:** CTRL-007

### Contract and confirmed evidence

User-confirmed: readOnly=true MUST suppress USER-ORIGINATED changes to value/model, but not programmatic setValue, setSelectedValue, selection or DataSet/Binder updates. Keyboard focus, viewing, and navigation need not be globally blocked. Do not replace this with native Node.setDisable(true), and do not block model-to-GUI writes.

CTRL-005 real Windows JavaFX 25 Robot showed noneditable JInvComboBox with A selected, focus retained before readOnly=true: Down selected B and updated value under readOnly. Its Controls.disableControl branch sets focusTraversable=false and installs mouse filters, but that does not prevent keyboard selection with retained focus. When focusTraversable is bound, setting readOnly throws before mouse filters are installed; Robot click and Down could still select B. The agent validated the unlocked positive control. The same audit reported locked ChoiceBox and other controls, outside this task. CTRL-006 radio correctness is now independently user-confirmed PASS; preserve its code.

### Narrow production assignment

1. Establish RED in a **tracked**, focused JDK 25/JavaFX 25 GUI smoke with the real JInvComboBox and its real skin, visible Stage, items A/B and bound SimpleStringProperty model or equivalent value observer. Test unlocked positive control first. Focus before locking, then deliver Down and other supported value-changing keyboard actions via targeted JavaFX and, where exclusive input exists, Robot. Verify selection, value, model and change count. Distinguish synthetic event paths, Robot input and physical manual interaction.
2. Fix input suppression **before changing selection/model**. At minimum cover focused Down and Up, and other keyboard selection keys actually handled by the skin. If a popup is already open and can still commit a new selection under readOnly, document and cover it if a safe minimal gate in JInvComboBox can enforce the same contract without changes outside authorized production files. Avoid after-the-fact selection rollback that emits transient model notifications.
3. Preserve all programming paths: ComboBox.setValue, selectionModel.select, JInvComboBox.setSelectedValue, Controls.setValue, binder/data adapter navigation and null clear where supported. No spurious typed/value events on rejected user input. Preserve editable and noneditable mode contracts; if a new keyboard gate blocks legitimate non-value operations, report and revise.
4. Specifically test a focusTraversableProperty bound source: do not try to set the bound property. readOnly must still suppress user-originated selection even if the binding source remains true and must not throw a bound-value exception. Test source switching true/false and unlock, preserving the binding. Examine actual popup mouse filtering and existing Controls behavior; do not silently claim full readOnly coverage when only keyboard tested.
5. Make compatibility and lifecycle checks: repeated lock/unlock, continued focused input, normal unlocked keyboard and mouse, no duplicate event filters, no lingering listener references and no unnecessary per-instance allocation. Public/protected API, converter/valueFactory/nullable semantics, state, skin and programmatic selection must remain unchanged. If correct suppression would require broad skin redesign or cannot distinguish user vs programmatic selection reliably, STOP and report BLOCKED before any production commit.

### Strict scope / validation / Git

- Only permitted production path: src/main/java/ru/inversion/fx/form/controls/JInvComboBox.java. A minimal edit to its custom skin may only be proposed, NOT silently made; report a blocker if genuinely needed. Test-only changes allowed in src/test/probes/Test-LegacyGuiSmoke.ps1 and new focused GUI probe under src/test/probes/gui.
- No changes to Controls.java, JInvRadioButton, JInvRadioGroup, JInvChoiceBox, CheckBox, Calendar, Button, Binder, POM/wrapper, dependencies, main or JInvCommon. New comments in production Russian.
- Run targeted RED/GREEN on Windows Java 25 / JavaFX 25, Maven -Pbuild compile and PowerShell parsing. Preserve existing -RadioReadOnly (CTRL-006), -NodeTraversal (CTRL-003), -DatePickerBinding (CTRL-002), -MoneyDecimal (NUM-001) and -DataBinding (GUI-009) PASS; explicitly list any unrun cases as UNKNOWN.
- Ordinary Git/GitHub operations APPROVED under mailbox protocol: verify current HEAD and preserve user work, publish a verified minimal commit by fast-forward push to migration/java25-correctness, verify exact remote SHA. Never force push.
- Publish ## Reply CTRL-007 to docs/codex/outbox.md on agent/codex-mailbox with RED/GREEN results, exact production/test diff and SHA, input provenance, bound-property and model-update invariants, known remaining readOnly gaps and Windows recheck command with -SettingsFile D:\Java\.m2\settings.xml.
- Stop after CTRL-007. Do not begin CTRL-008 or fix other controls.

---

## MESSAGE CTRL-008

**Status:** CLOSED — agent read-only audit reported in outbox; explicit user architecture decision (2026-11-23): do NOT pursue full JInvComboBox readOnly support. Existing CTRL-007 remains accepted, but readOnly must NOT be relied upon to prohibit user-originated value changes in business forms. No CTRL-009 production task.
**Priority:** HIGH — JInvComboBox readOnly completeness and programmatic API compatibility
**Repo:** ssu95a/fore25
**Implementation branch:** migration/java25-correctness (READ ONLY)
**Expected production HEAD:** cf7b05c45319624f3f0134a78d59410484829af9
**Reply ID:** CTRL-008

### Baseline and contract

CTRL-007 is independently user-confirmed PASS on Windows (`CTRL-007 PASS`, exit 0). Do not undo or expand that accepted production fix.
User contract: `readOnly=true` prohibits **user-originated value modification**, but programmatic setValue, setSelectedValue, selection, Binder and DataSet updates must remain operational. Focus, viewing and read-only navigation are not categorically forbidden. There is no pre-approved rule that `readOnly` forbids opening a popup through an explicit programmatic show() call if user changes remain blocked.

CTRL-007 currently overrides `JInvComboBox.show()` to return when readOnly. It also locks a standard inner TextField editor if editable and writable, but does not override an externally bound inner editor editable property. Agent reported the latter path UNKNOWN. A helper `forceCommitValue()` delegates to its custom skin. Scope the review to real semantics of these two boundaries and any closely related value commit path; do not broaden into other controls.

### Read-only investigation

1. Trace all in-repository callers of JInvComboBox.show(), hide(), getEditor().editableProperty(), `forceCommitValue()`, `cancelEdit()`, skin commit path, and `ComboBoxBase` popup/user event handlers. Check whether any business code intentionally opens a read-only combo for inspection, or whether popup opening leads to editable selection. External consumers UNKNOWN.
2. On a visible JavaFX25 Stage using production JInvComboBoxListViewSkin and a valid unlocked positive control, reproduce an editable ComboBox with `getEditor().editableProperty()` bound to a true/false source. Set readOnly=true, then use targeted events and JavaFX Robot with exclusive input if available to attempt typing, focus loss, Enter, popup selection and other documented commit paths. Record draft text, committed value, selection and bound SimpleStringProperty/model notifications. Vary bound source while locked. Compare with unbound editor and standard CTRL-007 sentinel. Distinguish Robot vs targeted events vs direct programmatic calls.
3. Investigate `show()` semantic regression: readOnly=true and an explicit programmatic combo.show(), while keeping popup selection blocked. Separate **opening for viewing** from **committing a new value**. Test the current actual behavior and any expected use cases from source. Do not decide the API contract by assumption or silently rewrite it.
4. Check direct programmatic `forceCommitValue()`, direct setValue and DataSet navigation under readOnly. Determine which actions are protected user input, programmatic requests, or ambiguous; do not block legitimate programmatic model updates.
5. If needed, inspect possible inaccessible/skin-specific editor event routes and the consequences for event consumption, copied text, focus and readOnly. Do not claim manual physical input or full BaseApp/DB coverage when not run. Evaluate smallest behavior-preserving correction and its memory/lifecycle impact.
6. Report CONFIRMED runtime, SOURCE-PROVEN, HYPOTHESIS and UNKNOWN separately. Recommend ONE minimal follow-up RED/GREEN fix only if evidence establishes an integrity issue; otherwise specify precisely which policy decision is needed. Keep the existing -ComboReadOnly baseline GREEN if re-run.

### Constraints and reply

READ ONLY. No production, tracked-test, POM, Maven-wrapper, dependency, module or other repository changes. Temporary untracked probes under ignored target are allowed. main and JInvCommon remain read-only. Do not start CTRL-009 or unrelated JInvChoiceBox/CheckBox/Calendar work.
All routine Git/GitHub operations APPROVED for reading and publishing the mailbox report only. Post `## Reply CTRL-008` in docs/codex/outbox.md on agent/codex-mailbox, including exact branch SHA, test matrix, proof of user vs programmatic origin, policy implications and ONE recommended next task. No implementation branch push.

### Architecture decision after CTRL-008 (user, 2026-11-23)

**NO FURTHER COMBOBOX READONLY REWORK.** The user chose to stop adding interception logic to JInvComboBox. Its `readOnly` property/API remains for compatibility and CTRL-007 is NOT reverted, but **complete suppression of user-originated edits is NOT guaranteed** and consuming applications must not rely on it to protect mutable business-model values.

Documented reason: real JavaFX 25 inner-editor focus-loss implicit commit can write a model even when readOnly is active, especially with a caller-bound editable property; a diagnostic asymmetric converter also wrote without a new user draft. Other routes include popup, skin, and validation callbacks. Fully blocking every route risks extensive skin/control redesign, additional state/listeners and API incompatibility. The default popup `show()` behavior introduced in CTRL-007 is unchanged; programmatic show compatibility remains an acknowledged limitation.

Usage guidance: do not use `JInvComboBox.setReadOnly(true)` as the mechanism of enforcing a business-form read-only state. For a noninteractive control, prefer native `Node.setDisable(true)` after verifying that programmatic DataSet/Binder updates remain functional in that particular form. For a visible inspection-only value, use a separate display control such as Label if its semantics fit. Retain validation at the business/model boundary independently of UI mode.

No production changes, no rollback, no new tests or CTRL-009 assignment authorized by this decision. The remaining findings are explicitly ACCEPTED LIMITATIONS, not resolved defects. The next Controls-audit task should target a different component or issue and requires separate assignment.

---

## MESSAGE CTRL-009

**Status:** CLOSED — user independently confirmed Windows Java 25 / JavaFX 25 `CTRL-009 PASS` (2026-11-23). Production `5fc050d452e32adf55407512c8e9b5e9cc13fa75` accepted for the tested matrix. User supplied no separate exit code or Robot run; physical gestures and full business forms/DB persist remain UNKNOWN.
**Priority:** HIGH — prevent user-originated selected/model changes
**Repo:** `ssu95a/fore25`
**Implementation branch:** `migration/java25-correctness` (never main)
**Expected remote HEAD:** `cf7b05c45319624f3f0134a78d59410484829af9`
**Reply ID:** `CTRL-009`

### Scope distinction

Previous suggestion to continue a *ComboBox* fix under CTRL-009 was explicitly cancelled. The existing CTRL-008 decision not to rework JInvComboBox remains binding. THIS new CTRL-009 is only about JInvCheckBox; do not reopen ComboBox, edit Controls, or change the accepted limitations.

### User's established contract and confirmed RED

`readOnly=true` blocks USER-originated value changes, but DOES NOT block programmatic setSelected, selectedProperty writes, model binding, DataSet navigation, nor programmatic value update in `JInvCheckBoxString`. ReadOnly must not automatically forbid focus or inspection. A prior CTRL-005 visible JavaFX25/Windows Robot audit, with a passing unlocked positive control, confirmed that focused Space toggles JInvCheckBox from false to true while readOnly=true. Targeted mouse also toggled, but bypassed normal mouseTransparent hit testing, so it does not prove physical mouse bypass. This is a confirmed keyboard integrity defect.

Current JInvCheckBox.init() registers a readOnly listener that calls Controls.disableControl(this, flag). The generic Controls path only changes focusTraversable, mouseTransparent and CSS; it does not prevent Space activation of an already-focused CheckBox. JInvCheckBoxString extends this class and synchronizes selected/indeterminate to its string valueProperty. Keep this mapping intact.

### One focused implementation task

1. Verify repo, correct branch and HEAD. Create a **tracked RED regression first** using a shown Stage, actual JInvCheckBox and a bidirectionally bound BooleanProperty model or equivalent listener. Assert positive unlocked Space toggle and one value/model notification. Request focus BEFORE setting readOnly=true, then use Space; demonstrate original selected/model changes, not merely state flags. Include focused keyboard activation through JavaFX event dispatch and, if exclusive Windows desktop input is available, JavaFX Robot; label provenance accurately.
2. Implement the smallest user-activation gate in `JInvCheckBox` only, before native selection and model propagation. Cover the actual supported activation keyboard gesture(s), primary mouse activation where applicable, and accessibility FIRE if it is a genuine user route. Preserve focused inspection and do not use after-the-fact rollback. Preserve programmatic CheckBox.fire() behavior unless a verified contract explicitly requires a change; report any ambiguity.
3. Respect bound focusTraversable or mouseTransparent: do not throw trying to write a unidirectionally bound property. A bound property must not bypass readOnly input gating. Avoid broad Controls or Native Node.disable rewrites. Preserve native disabled and CSS state on lock/unlock, plus repeated enable/disable without accumulating handlers.
4. In the same probe cover JInvCheckBoxString (both constructors where feasible): checked/unchecked/indeterminate mapping, selected and string-model notifications; blocked user Space must leave values unchanged, while programmatic selected, setValue, indeterminate and bound model updates still work. If a String subclass defect requires modifications outside JInvCheckBox, do not silently expand scope: report BLOCKED with evidence.
5. Check the plain JInvCheckBox selectedProperty direct updates, value initial true/false, repeated lock/unlock, focus retained, and bound selected/focus property behavior where meaningful. Test programmatic setSelected while locked and one notification per actual change. No new per-control cache or per-field object unless justified; exact retained bytes UNKNOWN.
6. Keep previous accepted `-ComboReadOnly`, `-RadioReadOnly`, `-NodeTraversal`, `-DatePickerBinding`, `-MoneyDecimal` and `-DataBinding` regressions GREEN where available. No change to JInvCheckBoxCellEditor or table-cell behavior unless a concrete regression is discovered; report instead.

### Constraints / verification / Git

- ONLY production edit permitted: `src/main/java/ru/inversion/fx/form/controls/JInvCheckBox.java`. Test-only edits may add a focused JavaFX probe under `src/test/probes/gui/` and extend `src/test/probes/Test-LegacyGuiSmoke.ps1` with a new `-CheckBoxReadOnly` mode. Do not change JInvCheckBoxString, Controls, ComboBox, other controls, skin, POM/wrapper, dependencies, main or JInvCommon.
- Preserve public/protected signatures and behavior, constructor semantics and bindings. New source comments in Russian. Prefer shared static event handlers over per-instance closures when feasible; do not introduce unsupported asynchronous work or threading stress.
- Run RED on original production classes, GREEN after fix with actual JDK25/JavaFX25 Stage, Maven -Pbuild compile and PowerShell parsing. If Robot not run, mark mouse hit-testing / native gesture UNKNOWN. Do not call diagnostic green if unlocked positive control fails.
- Git/GitHub operations APPROVED within task. Commit only verified minimal code/test changes, fast-forward push to migration/java25-correctness, confirm exact remote SHA; never force or discard user changes.
- Publish `## Reply CTRL-009` to `docs/codex/outbox.md` on `agent/codex-mailbox`, including code/source cause, exact diff/SHA, RED/GREEN event/model counts, user vs programmatic distinctions, String subclass checks, retained-memory effect, and independent Windows rerun command (`-CheckBoxReadOnly -SettingsFile "D:\Java\.m2\settings.xml"`).
- STOP after CTRL-009; no wider readOnly rework without new assignment.

---

## MESSAGE CTRL-010

**Status:** CLOSED — read-only investigation reported in `Reply CTRL-010` and user accepted the documented `JInvChoiceBox.readOnly` limitation (2026-11-23). No production changes or ChoiceBox skin rewrite authorized. User input can still select through a live popup; do not rely on readOnly for business-data integrity.
**Priority:** HIGH — prevent user-driven selection/model change without a large JavaFX skin reimplementation
**Repo:** ssu95a/fore25
**Implementation branch:** migration/java25-correctness (READ ONLY)
**Expected HEAD:** 5fc050d452e32adf55407512c8e9b5e9cc13fa75
**Reply ID:** CTRL-010

### Context and architectural guardrails

The previous CTRL-005 read-only audit reproduced JInvChoiceBox already-open popup selection (value A → B) while readOnly=true. The same probe FAILED to open a closed ChoiceBox popup even when UNLOCKED, so do not claim a closed-popup user route without a fresh positive control. Agent must establish positive controls BEFORE negative assertions. The user previously chose to stop extensive JInvComboBox readOnly rework. For this ChoiceBox investigation, FIRST decide whether minimal input prevention is possible; do not silently expand into skin redesign.

User contract: readOnly blocks USER-originated selection changes and bound model notifications, while ChoiceBox.setValue, selectionModel.select, JInvChoiceBox.setSelectedValue, Binder/DSControlAdapter navigation and other programmatic model writes must still work. ReadOnly need not block focus, viewing or programmatic API calls unless a narrow, explicitly justified choice is made. Never rely on temporary model rollback after notification.

Source-confirmed: JInvChoiceBox constructors delegate readOnly to Controls.disableControl; Controls branch uses setFocusTraversable and mouse filters, and can throw on bound focusTraversable before installing filters. JavaFX 25 ChoiceBoxSkin has an independent ContextMenu popup and its RadioMenuItem action calls selectionModel.select(index) directly; filtering only the ChoiceBox node cannot intercept all already-open popup selections.

### READ ONLY investigation tasks

1. Inspect current JInvChoiceBox.java, Controls.disableControl, upstream JavaFX25 ChoiceBox, ChoiceBoxSkin, ChoiceBoxBehavior and ContextMenu input routes. Identify actual keyboard selection and popup opening/closing mechanisms, including input while control is already focused, F4/Alt keys/Space where supported, accessibility SHOW, popup item action and programmatic show()/hide(). Distinguish SOURCE-PROVEN from runtime.
2. Build a temporary ignored-target Java 25 / JavaFX 25 visible Stage fixture with production JInvChoiceBox, at least items A/B, bidirectional SimpleStringProperty model, selection/value/model notification counters. Establish *working unlocked* positive controls separately for keyboard open/select and pointer popup selection. Exercise popup opened before locking, lock while showing, popup item select B, locked closed-popup open attempts, focused keyboard selection, and native disabled state. Targeted JavaFX event dispatch is valid for routing tests but bypasses mouse picking. Use JavaFX Robot only if exclusive Windows input is actually available and explicitly label its provenance; otherwise say NOT RUN.
3. Specifically test readOnly/focusTraversable bound properties, source transitions after lock, exception propagation, and repeat lock/unlock. Test popup show() invoked explicitly by application under readOnly and any accessibility show/selection effects. Assess whether closing on lock plus guarding user-originated reopening (not blanket API mutation) can be minimal/compatible; do not assume all show() calls represent user input.
4. Check real JInvChoiceBox `setSelectedValue(valueFactory)` and programmatic `setValue`, `getSelectionModel().select` while locked, binding/DSControlAdapter navigation (A/B/null if supported). No suppression of model-to-GUI flow, and no spurious event count from blocked user input. Search actual app usages for explicit show(), popup listeners, and any custom skins or declarative state expectations.
5. Propose ONE narrow correction only if scope is demonstrably small, with exact entry-point strategy, proof that popup menu actions cannot write the model while locked, public API/compatibility effects, and estimated instance-state/listener/memory overhead. If a custom skin rewrite, broad Controls redesign or change to programmatic show() semantics is required, report BLOCKED/POLICY DECISION and recommend documentation instead. Avoid the unapproved JInvComboBox redesign scenario.
6. Keep existing CTRL-009 -CheckBoxReadOnly and other accepted regression tests GREEN if rerun. Separate CONFIRMED runtime, SOURCE-PROVEN, HYPOTHESIS, UNKNOWN and what still requires Robot/real application validation.

### Strict limits / report

READ-ONLY investigation only. No production, tracked tests, POM, Maven wrapper, dependency, schema, or source changes; temporary probes only in ignored target. No commits to migration/java25-correctness, main or JInvCommon. No work on ComboBox, radio, calendar or unrelated controls. ONLY Git/GitHub write allowed is append `## Reply CTRL-010` to docs/codex/outbox.md on agent/codex-mailbox (mailbox protocol approved), with branch SHA, exact Java/JavaFX versions, positive/negative matrix, any minimal-proposal vs reason for rejecting a fix, and concrete next action. Do not assign or start CTRL-011.

---

## MESSAGE CTRL-011

**Status:** CLOSED — read-only audit completed in Reply CTRL-011, and user explicitly accepted documented JInvTextField.readOnly limitation (2026-11-23). No production changes or broad input/skin rework authorized. No CTRL-012 assigned.
**Priority:** CRITICAL — input integrity and unexpected model writes in common text controls
**Repo:** ssu95a/fore25
**Implementation branch:** migration/java25-correctness (READ ONLY)
**Expected HEAD:** 5fc050d452e32adf55407512c8e9b5e9cc13fa75
**Reply ID:** CTRL-011

### Confirmed baseline, source and contract

Prior CTRL-005 Windows JDK25/JavaFX25 probe with a real JInvTextField and valid unlocked positive control confirmed: `readOnly=true` with `editableProperty().bind(sourceTrue)` causes the Controls.disableControl listener to raise a bound-value exception, while focused user-style typed input and Backspace continue changing text. Changing the editable source to false blocks those edits and permits copy. Native readOnly assignment itself stays true. Audit used real Stage, source flags and targeted JavaFX input; physical/Robot not demonstrated for this TextField binding path.

Current production source: JInvTextField(String) registers readOnly listener -> `Controls.disableControl(this, flag)`. The TextInputControl branch of Controls sets `editable = !disable` without checking `editableProperty().isBound()`. JInvTextField additionally installs a per-field CaseSensitiveFormatter, context-menu population, F9/LOV handling (`isEditable() || getButtonIgnoresEditable()`), a `commitValue` path and a custom skin. It has selection caching, tooltip and external LOV button bindings. Do NOT assume TextField's native `editable=false` is the same as `readOnly`, or that simply adding a listener to block text changes preserves programmatic model writes.

User contract: `readOnly=true` must prevent USER-originated text/value changes and any resulting Binder/DataSet/FXEntity model modifications; programmatic setText/textProperty changes, formatter/model-to-GUI normalization, DataSet navigation and LOV-driven **authorized programmatic** updates must remain allowed. Selection/copy/focus should remain available if they already work. No model-write rollback after emitting notifications. User agreed to accept documented limitations rather than complex control/skin reimplementations when scope is disproportionate (ComboBox and ChoiceBox precedents).

### READ-ONLY investigation

1. Verify exact production HEAD; inspect JInvTextField.java, custom skin, CaseSensitiveFormatter, Controls.disableControl, `ITextFieldBase`/validation/LOV and real Binder/DataSet binding route. Separate SOURCE-PROVEN, CONFIRMED runtime, HYPOTHESIS and UNKNOWN. Check subclass impact if any, but do not edit other controls.
2. Build a *temporary ignored target* Java 25/JavaFX25 visible Stage audit with real JInvTextField skin and `SimpleStringProperty` bidirectionally bound model, text/model notification counters and tested unlocked positive controls. Establish normal typed text, Backspace, paste/cut/context menu, copy, selection and F9/LOV route as available. Then set readOnly on focused field with externally bound editable=true; record exceptions, text changes and model notifications. Dynamically change bound source true→false→true while locked; verify identity/liveness. Compare ordinary writable editable mode (CTRL-005 baseline) and repeated lock/unlock.
3. Include KEY_TYPED, KEY_PRESSED/KEY_RELEASED, deletion, clipboard paste/cut, context menu, IME/undo/redo/drag-drop where feasible. Explicitly mark unrun paths UNKNOWN. Synthetic Event.fireEvent bypasses native hit testing; use Robot only if exclusive keyboard/mouse access is established; do not claim physical input without it. Disabled ancestor, focus retention, focused selection and Ctrl+C should be included if supported.
4. Verify explicit programmatic setText, textProperty/model writes, actual ArrayDataSet + applicable DSControlAdapter and `Controls.setValue` while locked. Never confuse legitimate programmatic normalization with user edits; avoid a blanket property listener or value rollback that would block DataSet updates or produce transient notifications.
5. Audit F9/LOV and contextual actions for a bound editable=true field locked by readOnly. Do not construct unsupported full LOV dependencies or claim DB behavior without a real fixture. Determine if `getButtonIgnoresEditable()` permits a user to mutate field value while readOnly, and whether current buttons' disableProperty bindings already prevent it. Check bound visible/disable/other critical properties only where they directly affect user editability.
6. Explore the **smallest possible** compatible gate that prevents user-originated edits with editable externally bound TRUE, while preserving native editing API, copy and programmatic updates. Critically assess TextFormatter (application-owned per control), clipboard paths, input methods, and JavaFX TextInputControl internals. Estimate extra instance handlers, allocation and lifecycle risk. If straightforward and complete, propose one narrow RED/GREEN task; if not, recommend documented accepted limitation. Do not implement now.

### Constraints and reporting

READ ONLY. No modifications to production, tracked tests, POM, modules/dependencies, JavaFX controls, main or JInvCommon. Only ignored target temporary probe permitted; no commits to `migration/java25-correctness`. Scope limited to JInvTextField and its direct code paths. Do not start CTRL-012 or unrelated calendar/button fixes.
All routine Git/GitHub actions APPROVED for reading and publishing mailbox report ONLY. Publish `## Reply CTRL-011` to `docs/codex/outbox.md` on `agent/codex-mailbox` with exact SHA/JavaFX versions, RED evidence/positive controls, user vs programmatic event/model counts, binding exception details, compatibility/per-instance-memory implications, and a SINGLE recommended next decision/task. Clearly say which input provenance and full app/DB cases remain UNKNOWN. Do not push implementation branch.

### Architecture decision after CTRL-011 (user approved, 2026-11-23)

**ACCEPTED LIMITATION:** `JInvTextField.readOnly` must NOT be treated as a data-integrity enforcement boundary, especially when `editableProperty` is externally bound. The agent confirmed that `readOnly=true` with `editableProperty.bind(true)` raises a bound-property exception on `Controls.disableControl` and permits user-style text mutations to reach a bidirectional model. Separate confirmed routes include already-open context-menu actions, direct accessibility SET_TEXT, and F9/LOV under editable or buttonIgnoresEditable conditions. Unsupported physical/Robot and full app/DB cases remain UNKNOWN.

**NO JInvTextField READONLY REWORK** is authorized from CTRL-011. Keep existing production and compatibility API unchanged. Do not add blanket TextFormatter vetoes, input filters, state snapshots, rollback or skin changes on this task. A future independently scoped fix for bound-property exception must be evaluated separately and must not be presented as complete readOnly support.

Usage guidance: do not rely solely on the control's readOnly flag to protect business-model writes. Enforce authorization and validation at model/action/persistence boundaries. For display-only interaction use a suitable view; native disable may be an option after checking retained context-menu/LOV state and programmatic model-to-control updates in the actual form. This is the same policy category as accepted JInvComboBox and JInvChoiceBox limitations. No CTRL-012 is being assigned.

---

## MESSAGE CTRL-012

**Status:** CLOSED — read-only audit completed in Reply CTRL-012; user explicitly accepted documented JInvCalendar/JInvCalendarTime readOnly limitations (2026-11-24). No production/skin rework authorized. Do not rely on calendar readOnly for data-integrity enforcement; already-open popups and bound editable can write models. Native disable must not be assumed to close a popup.
**Priority:** HIGH — date and datetime model integrity under readOnly
**Repo:** ssu95a/fore25
**Implementation branch:** migration/java25-correctness (READ ONLY)
**Expected remote HEAD:** 5fc050d452e32adf55407512c8e9b5e9cc13fa75
**Reply ID:** CTRL-012

### Goal / accepted architecture

The user requested progression to the next Controls audit. CTRL-010 JInvChoiceBox and CTRL-011 JInvTextField were closed with explicit ACCEPTED LIMITATIONS because broad skin/input interception would be disproportionate. DO NOT resume either workstream or quietly redesign calendar controls. Analyze first whether a MINIMAL, correctness-preserving change exists; do not modify production in CTRL-012.

Contract: readOnly=true means USER-originated modifications must not alter date/datetime or bound business model, but programmatic `setValue`, `dateTimeValue` updates, `Controls.setValue`, Binder, DSControlAdapter and DataSet navigation must continue working. In inspection state, copying/selection/focus may be useful. No after-the-fact value rollback that transiently notifies model.

CTRL-005 Windows Java25/JavaFX25 audit already established: closed JInvCalendar/JInvCalendarTime popup did not open via tested F9 and arrow pointer gestures while locked. However an already-open JInvCalendar popup remained open after switching readOnly=true; targeted day-cell selection changed March 10 to March 12, and the live control value changed. Those targeted events bypass OS pointer picking. Real Robot or physical input post-change UNKNOWN. Original readOnly lock delegates to Controls.disableControl(DatePicker), which changes editable but doesn't hide existing popup. Source: JInvCalendar.show() gates on editable; F9 also checks editable; JInvCalendarTime synchronizes LocalDate and LocalDateTime and can preserve the time component during date change.

### READ-ONLY investigation matrix

1. Verify exact production HEAD and inspect BOTH JInvCalendar constructors (JInvCalendar() vs JInvCalendar(LocalDate)), JInvCalendarTime, factory skin feature `JInvDataPickerSkin` (when BaseApp.APP() exists), JavaFX25 DatePicker/DatePickerSkin/DatePickerContent and Controls.disableControl. Identify all ordinary user input routes: keyboard F9/F4/Alt arrows, arrow click, typing into editor, focused key events, date cell selection, popup already open, accessibility expand/commit and context menu. Distinguish JavaFX source analysis from actual runtime. Do not silently treat the default skin as custom BaseApp skin.
2. Build ignored-target, untracked visible Stage fixture with actual JInvCalendar and JInvCalendarTime, unlocked positive controls for popup and day selection, bound LocalDate/LocalDateTime or real adapter where supported, and counters for date/value/model notifications. Include date change March 10 -> March 12 and CalendarTime fixed time 13:42; verify time preservation. Test lock before opening and lock after popup is showing, then day cell input. Test popup show/hide programmatically while locked and any native disabled interaction.
3. Bound editableProperty or other bound writable flags: verify that readOnly listener does not throw and, if it does, whether a user can still modify the date/datetime and notify bound model. Test dynamic source true/false while locked and round-trip lock/unlock. Native `setDisable` and ancestor disable must not be assumed to automatically close existing popups.
4. Test user-oriented text entry/commit (including focus loss and forced commit) and calendars' editor converter, formatting/invalid drafts, null clearing, and value/dateTimeValue propagation. Distinguish direct PROGRAMMATIC forceCommitValue/setValue from user-originated triggers such as validation mouse filters. Do not confuse normal model updates with user input. If any setup paths fail even unlocked, mark UNKNOWN instead of counting a locked PASS.
5. Include explicit `LocalDate` and `LocalDateTime` programmatic writes under lock, actual `ArrayDataSet`/`DSControlAdapter` and any specialized adapter or Binder path relevant to each control, including null if supported. Check exact notification counts and absence of value/time loss. Note JInvCalendarTime's inherited readOnly and two-way date/datetime listeners.
6. Assess if a single popup close on locking and narrowly gating reopening could solve the confirmed already-open popup case without breaking programmatic show() and current public API. Consider the default JavaFX skin and BaseApp-provided custom skin separately. If the popup can still commit through independent child handlers, or strict support requires skin redesign/formatters, report ACCEPTED-LIMITATION recommendation with evidence rather than proposing incomplete hardening. Any proposed future edit must be scoped and include per-instance state/listener allocations and lifecycle risks; exact retained bytes UNKNOWN unless measured.
7. Use targeted JavaFX dispatch for diagnostic routing but explicitly label it; Robot only if exclusive Windows input is available. Establish positive controls first. Distinguish CONFIRMED runtime, SOURCE-PROVEN, HYPOTHESIS, UNKNOWN. Full business forms, DB persistence, externally supplied skin are UNKNOWN unless genuinely tested.

### Strict scope and report

READ ONLY. No production or tracked test edits, commits to implementation branch, dependency/POM/module/skin changes, no JInvCommon or main writes. Temporary probes may reside only in ignored target. Do NOT start CTRL-013 or unrelated controls. Ordinary Git/GitHub reading and mailbox report publication APPROVED.
Publish `## Reply CTRL-012` to docs/codex/outbox.md on agent/codex-mailbox. Include exact SHA, Java/JavaFX versions, unlocked and locked matrix, input provenance, binding/model notification counts, CalendarTime date-time integrity, risk/compatibility and ONE recommended decision or minimal separate future task. Confirm production remained at baseline SHA.

---

## BACKLOG CAL-TIME-001 — JInvCalendarTime implicit seconds mutation

**Status:** DEFERRED / ACCEPTED FOR BACKLOG (user decision 2026-11-24); NOT ASSIGNED. No agent work or implementation authorized yet.
**Priority when scheduled:** correctness / data integrity, independent of CTRL-012 readOnly policy.
**Component:** `src/main/java/ru/inversion/fx/form/controls/JInvCalendarTime.java`, converter `InternalConverter.fromString`.
**Baseline SHA:** `5fc050d452e32adf55407512c8e9b5e9cc13fa75` on `migration/java25-correctness`.

**CONFIRMED in CTRL-012 Windows Java 25.0.1/JavaFX 25 runtime:** unchanged displayed `10.03.2026 13:42` (input mask only minute precision), locked `editableProperty` externally bound true, focus lost without user draft edit; business `dateTimeValue` changed `2026-03-10T13:42:14` → `2026-03-10T13:42:44`. Date event 0, datetime/model events 1/1. Source confirms minute-resolution `InternalConverter` parses then `.plusSeconds(LocalTime.now().getSecond())` instead of preserving prior hidden seconds. Full applications and persistence remain UNKNOWN.

**Future RED/GREEN scope (only when separately assigned):** establish deterministic test for losing focus/committing untouched displayed value without changing datetime or emitting model notifications; test genuine newly entered minute-precision values, explicit seconds input, date-only mode, rangeable mode, null/invalid drafts, masked formatter semantics and `LocalDateTime` binding/DSControlAdapter navigation. Specify the policy for seconds on an actual new user edit before implementing it. Preserve all programmatic updates and user-visible converter behavior where required. Avoid work on full `readOnly` interception or calendar skin changes.

**No assignment number CTRL-013**, no production change, no GitHub issue created in this step.

---

## MESSAGE CTRL-013

**Status:** CLOSED — independent user Windows `CTRL-013 PASS`, `$LASTEXITCODE=0` (2026-11-24); implementation `2e8c11556690e49968c575fd13d00e0b998dbacc` accepted. No further production edits authorized in this task.
**Priority:** HIGH — prevent retained Node property/map allocation on a read-only query
**Repo:** ssu95a/fore25
**Implementation branch:** migration/java25-correctness (never main)
**Expected production HEAD:** 5fc050d452e32adf55407512c8e9b5e9cc13fa75
**Reply ID:** CTRL-013

### Confirmed source issue

`Controls.isControlValueChanged(Node)` currently calls `getValueChangedProperty(control).getValue()` for non-null nodes. This helper lazily creates a `SimpleBooleanProperty(control, "controlValueChanged")` and stores it under `IJInvControl.CONTROL_VALUE_CHANGED` in `node.getProperties()`. Hence a predicate READ on a pristine node creates a persisted BooleanProperty and can create the lazily allocated Node properties observable map. Upstream OpenJFX 25 Node.java confirms `getProperties()` lazily initializes `FXCollections.observableMap(new HashMap<>())`, whereas public `hasProperties()` returns `properties != null && !properties.isEmpty()` without initialization. This is significant in many GUI controls; direct caller chain `F7FilterDialog -> ValidMan.isControlValueChanged -> Controls.isControlValueChanged` exists.

User preference: correctness and minimizing retained memory per control are higher priority than style; no unnecessary per-instance fields, listeners or cache. This is not a request to change unrelated Controls methods.

### Narrow fix and RED/GREEN tests

1. Verify repository/branch HEAD; create a tracked minimal test under `src/test/probes/gui/` and add a dedicated `-ValueChangedFlag` mode to `src/test/probes/Test-LegacyGuiSmoke.ps1` (or a comparably existing official GUI runner). Establish RED on original source: `Controls.isControlValueChanged(pristineNode)` returns false BUT changes `node.hasProperties()` to true or inserts `IJInvControl.CONTROL_VALUE_CHANGED`; the test must assert no retained mutation/metadata even on the first read. Use an actual Node with initially `hasProperties()==false`, e.g. `Pane`, `Group`, or stock suitable Node; establish unlocked/valid fixture positive control.
2. Modify **only `Controls.isControlValueChanged` production implementation** to use public Node.hasProperties guard and lookup existing optional property *without calling the creating getter*. False for null, pristine, and unrelated Node properties; no map or BooleanProperty should be created on a read. Check `IJInvControl.CONTROL_VALUE_CHANGED` only when properties actually exist. Preserve value semantics and any existing cast/behavior for an already stored BooleanProperty. Avoid changing explicit property API, visibility or mutability.
3. Assert explicit `Controls.getValueChangedProperty` still creates and stores the same writable property on demand, repeated calls preserve identity and listeners, `setControlValueChanged(node,true/false)` still writes and reports correct values, and a pre-existing bound/value property is observed without replacement. Access to other unrelated node.getProperties keys must remain unchanged. Null reads return false.
4. Check that newly created/unset controls with metadata follow the same behavior; no redundant property instance is introduced on read, no per-instance listener registration. If duplicate property key has a custom or unexpected type, document existing contract rather than silently redesigning it.
5. Run Java 25 / JavaFX 25 targeted test RED -> GREEN; Maven `-Pbuild` compile, PowerShell parsing and `git diff --check`; keep prior accepted `-CheckBoxReadOnly`, `-ComboReadOnly`, `-RadioReadOnly`, `-NodeTraversal`, `-DatePickerBinding`, `-MoneyDecimal`, `-DataBinding` green where available. If a probe requires a Windows Stage/toolkit, use actual JavaFX 25 and report exact provenance. A headless pure-Node test is acceptable if it is sufficient to establish properties-map allocation semantics. State any unrun test as UNKNOWN.

### Strict scope / Git / mailbox

- ONLY production modification permitted: `src/main/java/ru/inversion/fx/form/controls/Controls.java`, specifically `isControlValueChanged`. No change to `getValueChangedProperty` or `setControlValueChanged`, no F7FilterDialog/ValidMan edits, no broad Controls refactor, dependency/POM/wrapper, main or JInvCommon changes. Test only in `src/test/probes/gui` and dedicated runner mode.
- New source comments in Russian. Preserve API and avoid additional retained memory; exact bytes UNKNOWN unless independently measured. Do NOT change the ComboBox/ChoiceBox/TextField/Calendar accepted readOnly limitations or deferred CAL-TIME-001 seconds finding.
- Ordinary Git/GitHub actions APPROVED: commit verified minimal implementation on migration/java25-correctness, fast-forward push, confirm exact remote SHA; never force push. Publish `## Reply CTRL-013` in `docs/codex/outbox.md` on agent/codex-mailbox with baseline/final SHA, exact diff, test evidence, RED/GREEN allocation/property identity counts, and Windows recheck command with `-ValueChangedFlag -SettingsFile "D:\Java\.m2\settings.xml"`.
- STOP after CTRL-013. Do not start CTRL-014 or edit other Controls helpers. Future `getTitleFromTableColumn` detached-column correctness and `getNonControlComponentFromContainer` allocation/API semantics are separate potential tasks.

---

## MESSAGE CTRL-014

**Status:** BLOCKED — agent Reply CTRL-014 proved null-clearing can navigate specialized DSChoiceBoxAdapter cursor to literal null row (1→-1→2). Provisional Controls.setValue patch was discarded. Requires explicit null-item/clear-selection policy before production implementation. Do not resume CTRL-014 as part of CTRL-015.
**Priority:** HIGH — programmatic value setting and data integrity, not readOnly
**Repo:** ssu95a/fore25
**Implementation branch:** migration/java25-correctness (never main)
**Expected production HEAD:** 2e8c11556690e49968c575fd13d00e0b998dbacc
**Reply ID:** CTRL-014

### Confirmed source facts

`Controls.getValue(Object)` explicitly supports `JInvChoiceBox`, returning the selected item T. `Controls.setValue(Object,Object)` does NOT support JInvChoiceBox and throws `Unsupported class control`. This is source-proven and the previous CTRL-010 audit also observed the exception with a generic DSControlAdapter. `JInvChoiceBox<T,R>.setSelectedValue(R)` selects by mapped valueFactory R when configured, by raw item equality otherwise, and currently does nothing on null. Specialized Binder.bindChoiceBox/DSChoiceBoxAdapter is distinct; it must not be modified.

User approved closing remaining Controls issues as narrow fixes with correctness/data integrity first. Earlier JInvChoiceBox.readOnly implementation limitations were ACCEPTED; DO NOT reopen that work or touch the popup/skin.

### Work and acceptance criteria

1. Verify expected branch, remote HEAD and clean working tree. Build a tracked JavaFX25 smoke under src/test/probes/gui and a dedicated -ChoiceSetValue mode in src/test/probes/Test-LegacyGuiSmoke.ps1. Establish positive control: direct `JInvChoiceBox.setSelectedValue("B")` works. Then RED: `Controls.setValue(choice,"B")` throws (record exact type/message, selection, model counts). Include Controls.setValue(Node) and Controls.setValue(Object) overloads.
2. Test both JInvChoiceBox constructors, simple String items A/B, and a distinct T-record/R-key valueFactory case. Validate selected item T, Controls.getValue result T, mapped R selection, exactly one selection/value/model notification for a real change, zero on identical repeats. Include unmatched non-null key: preserve existing JInvChoiceBox.setSelectedValue no-change behavior. Distinguish R from T, and do NOT silently change getValue API.
3. Test null explicitly: start with selected B. `Controls.setValue(choice,null)` should clear value/selection if consistent with generic Controls.setValue null meaning and model-to-GUI behavior. Compare `ChoiceBox.setValue(null)`, `selectionModel.clearSelection`, current `JInvChoiceBox.setSelectedValue(null)` (known no-op) and selection-to-dataset navigation. Do NOT change JInvChoiceBox's public setSelectedValue(null) semantics; handle null locally in Controls.setValue if safe. If clear invokes an unsafe dataset navigation or would violate existing required invariants, STOP and report BLOCKED with evidence rather than guessing.
4. Implement the smallest production branch in `Controls.setValue(Object,Object)` only. For non-null call existing JInvChoiceBox.setSelectedValue with the projected R. If null semantics are verified safe, clear appropriately without extra model-change events. Preserve dispatch precedence and APIs for ICustomValueControl, DatePicker, JInvComboBox, ComboBox, CheckBox, RadioGroup etc. No new per-instance allocations, caches, listeners, or wide refactor.
5. Verify generic DSControlAdapter navigation/update path where feasible, including non-null and null/missing rows. Do not modify adapter, Binder, or specialized DSChoiceBoxAdapter. Explicit programmatic updates during `readOnly=true` must continue to succeed; no change to user-origin input policy. Annotate tests with correct provenance and use actual Java 25.0.1 / JavaFX 25 Stage and FX thread when required.
6. Run real Windows RED before production edit and GREEN after. Maven -Pbuild compile, PowerShell syntax, git diff --check; rerun relevant existing `-ValueChangedFlag`, `-CheckBoxReadOnly`, `-ComboReadOnly`, `-RadioReadOnly`, `-NodeTraversal`, `-DatePickerBinding`, `-MoneyDecimal`, `-DataBinding` smoke modes as practical. Any unrun cases remain UNKNOWN. Robot unnecessary as this is programmatic API behavior.

### Strict scope, Git, reply

- ONLY production edit: src/main/java/ru/inversion/fx/form/controls/Controls.java, specifically setValue(Object,Object). Test-only addition in src/test/probes/gui and optional -ChoiceSetValue runner. Do not edit JInvChoiceBox, adapters, Binder, other methods of Controls, dependencies/POM/wrapper, main or JInvCommon. Comments newly added in production in Russian.
- Git/GitHub operations APPROVED within this task: minimal verified commit, fast-forward push to migration/java25-correctness, confirm exact remote SHA. NEVER force push or discard user changes. If blocker discovered, no production commit.
- Publish `## Reply CTRL-014` on docs/codex/outbox.md in agent/codex-mailbox with exact baseline/final SHA, RED/GREEN matrix, null/valueFactory semantics, event counts, exact diff, and Windows independent rerun command `-ChoiceSetValue -SettingsFile "D:\Java\.m2\settings.xml"`.
- Stop after CTRL-014; next detached-column title and non-control collection allocation cases are separate potential tasks.

---

## MESSAGE CTRL-015

**Status:** CLOSED — user independently confirmed Windows CTRL-015 PASS (2026-11-24) after agent RED/GREEN; production commit 47c31310fac24692dc1b7b3118a127825de52a01 accepted for tested invariants. Robot, full business forms and DB persistence remain UNKNOWN.
**Priority:** HIGH — dataset correctness, duplicate events and stuck navigation state
**Repo:** ssu95a/fore25
**Implementation branch:** migration/java25-correctness (never main)
**Expected production HEAD:** 8a993e3e4aa7b24611902ac8fa8380288586b939
**Reply ID:** CTRL-015

### Evidence and strict separation

CTRL-014 is BLOCKED pending explicit policy for literal null items vs clearing selection. DO NOT add Controls.setValue(JInvChoiceBox), change null policy, or alter JInvChoiceBox/ChoiceBoxSkin/Binder. The recent ControlsFollowup production commit `8a993e3e` has been pushed and its reported smoke tests passed, but user-independent acceptance remains UNKNOWN. Do not mix that code with this task.

Source inspection on the above production SHA: `DSChoiceBoxAdapter` constructor executes `dataSet.addRowListener(this)` and `dataSet.addNavigationListener(this)` TWICE each. Whether the DataSet deduplicates these registrations, and whether callbacks duplicate in practice, are UNKNOWN until tested. Its `selectedIndexProperty` listener sets `insideInSetCurrentRow = true`, calls `dataSet.setCurrentRowNum(newIndex)` and then resets the flag without `finally`. If that call throws, subsequent navigation can be suppressed. This is a source-proven exception-safety risk.

### RED/GREEN task

1. Verify working tree, expected branch and remote HEAD. Read the actual `IDataSet`, listener registration/dispatch implementations and `ArrayDataSet` behavior before changing code. Establish whether double registration creates two entries or whether the DataSet deduplicates. Record source facts separately from runtime evidence.
2. Create a TRACKED focused JavaFX 25/Java 25 GUI smoke with real JInvChoiceBox + real DSChoiceBoxAdapter, a small ArrayDataSet and counters for row/navigation events, selected index/value, and DataSet cursor. Establish positive controls with A/B and valid navigation. Reproduce duplicate callback behavior, if possible, using row INSERT and navigation events; do not equate registration counts with duplicate externally-visible events without proof. Distinguish second registration's effect from reentrant UI selection.
3. Test a controlled `setCurrentRowNum` failure originating from selected-index change. Use a minimal real DataSet scenario or a strictly local test double, preserving actual listener dispatch semantics. Check whether `insideInSetCurrentRow` remains true and causes later programmatic DataSet navigation to fail to update selection. If a failure cannot be reproduced, explicitly state UNKNOWN and still assess source-level try/finally correctness.
4. Only after establishing evidence, make the smallest production change in `src/main/java/ru/inversion/fx/form/controls/choicebox/DSChoiceBoxAdapter.java`: remove redundant listener registrations if they are indeed redundant, and use `try/finally` around `setCurrentRowNum()` to restore the reentrancy guard on exceptions. Preserve original exception propagation, listener order, synchronous selection/data cursor semantics and public API. Do not change null clearing, index -1 handling, weak/strong listener ownership or lifecycle in this task unless a blocker is demonstrated and separately reported. No new instance fields/allocations/handlers.
5. Test normal UI→DataSet selection A→B and DataSet→UI navigation B→A, repeated selection, row INSERT, exception + recovery and stable event counts. Add a diagnostic for literal null row A/B/null and explicitly document cursor changes; DO NOT change its semantics or characterize an intentional null-row selection as an error without an approved policy.
6. Maven `-Pbuild` compile, PowerShell syntax, `git diff --check`. Rerun accepted GUI smoke modes, including `-ControlsFollowup`, `-ValueChangedFlag`, `-CheckBoxReadOnly`, `-ComboReadOnly`, `-RadioReadOnly`, `-NodeTraversal`, `-DatePickerBinding`, `-MoneyDecimal`, `-DataBinding` where available. Report all unrun suites as UNKNOWN. Windows Java 25/JavaFX 25 targeted programmatic event tests are sufficient; Robot optional only with exclusive desktop access.

### Boundaries and reporting

- Production edit ONLY `src/main/java/ru/inversion/fx/form/controls/choicebox/DSChoiceBoxAdapter.java`. Tests ONLY under `src/test/probes/gui/` and `src/test/probes/Test-LegacyGuiSmoke.ps1` with a new `-ChoiceAdapter` mode. No changes to Controls, JInvChoiceBox, Binder, other adapters, POM/dependencies, main or JInvCommon. Comments in new source code Russian.
- If there is an unresolvable contract conflict or the test demonstrates that even this narrow change would disturb correct navigation, STOP and report BLOCKED, no production commit.
- Git/GitHub operations APPROVED for narrow verified fix: commit minimal changes, fast-forward push to `migration/java25-correctness`, verify remote SHA; do not force push or discard user changes. Publish `## Reply CTRL-015` in docs/codex/outbox.md on `agent/codex-mailbox` with RED/GREEN, duplicates evidence, exception recovery, exact SHA/diff, status of literal-null uncertainty, and a reproducible Windows command (`-ChoiceAdapter -SettingsFile "D:\Java\.m2\settings.xml"`).
- STOP after CTRL-015; do not self-assign CTRL-016. Monitoring is hourly and will report your new outbox reply, but it does not approve future implementation steps.

---

## MESSAGE CTRL-016

**Status:** CLOSED — READ-ONLY investigation completed in Reply CTRL-016. User authorized separate narrow fixes CTRL-017 and CTRL-018 (2026-11-24). The CTRL-014 generic null-clear contract remains BLOCKED and unchanged.
**Type:** ~1-hour bounded READ-ONLY forensic audit with local ignored-target runtime experiments; NO production or tracked-test modifications
**Priority:** HIGH — establish ChoiceBox/FXEntity/DataSet null semantics to resolve CTRL-014 safely
**Repo:** ssu95a/fore25
**Branch:** migration/java25-correctness (READ ONLY; use the verified HEAD after CTRL-015)
**Reply ID:** CTRL-016

### Background, evidence and invariant

CTRL-014 is BLOCKED, not implemented. The generic `Controls.setValue(JInvChoiceBox, value)` lacks a branch. Provisional support for null via `choice.setValue(null)` caused JavaFX selection index `1 -> -1 -> 2` for real specialized Binder.bindChoiceBox + DSChoiceBoxAdapter with list `[A, B, null]`, moving the DataSet cursor 1 -> 2. `selectionModel.clearSelection()` had the same outcome. Meanwhile `JInvChoiceBox.setSelectedValue(null)` currently is a NO-OP. Thus `null` as a real item, `null` as a model value, and absence of selected row cannot be equated. Never silently change user data, cursor position or the public null contract.

Source review also found `Binder.bindChoiceBox(IFXEntity, JInvChoiceBox)` with `valueFactory` installs `cb.valueProperty().addListener` calling `valueFactory.apply((T) cb.getValue())` with no null guard. Verify whether a null selection can throw or cause data/model inconsistency. Do NOT assume all valueFactory functions accept null. `DSChoiceBoxAdapter` is concurrently being investigated by CTRL-015 for duplicate listener registration and `insideInSetCurrentRow` exception safety; this assignment MUST NOT overlap or rewrite that work.

### Time-boxed tasks (around one hour, stop at evidence, not exhaustive refactor)

1. PRECONDITION: read fresh GitHub inbox/outbox, confirm `Reply CTRL-015` says DONE, production branch SHA matches the published final SHA (no unexpected advances), and local worktree is clean. If not, write a short WAITING/BLOCKED response only and stop. Never modify the in-progress CTRL-015 checkout.
2. Trace and document current contracts among `Controls.getValue/setValue`, `JInvChoiceBox.setSelectedValue`, `Binder.bindChoiceBox` both FXEntity and DataSet overloads, `DSChoiceBoxAdapter`, JavaFX ChoiceBox value/list index semantics, and ArrayDataSet cursor behavior. Distinguish value T vs projected key R, `null item` vs no selection, and index `-1` vs a valid null-row index.
3. Create temporary, UNTRACKED probes under ignored `target/ctrl016` only, using Java 25/JavaFX 25 actual classes on FX thread. Establish positive controls first. Compare at least these fixtures: `['A','B']`, `['A','B',null]`, and structured rows `[Row('A',1),Row('B',2),null]` (if row-null supported). Run native `setValue(null)`, `clearSelection()`, explicit select of null-row index, DataSet navigation to a valid row, and missing/invalid index. Record exact selected-index transitions, value/item/model change counts, cursor old/new indices and exceptions. Do not use UI synth dispatch to claim physical behavior; this is a programmatic API audit.
4. Independently test FXEntity `valueFactory` binder when control value becomes null through both programmatic clear and DataSet navigation, verifying if `valueFactory.apply(null)` fails. Use a real minimal FXEntity if practical; otherwise mark as SOURCE-PROVEN risk and runtime UNKNOWN. Verify non-null key mapping and changes without duplicate notifications.
5. Evaluate these policies without implementing: (A) forbid literal null items in specialized DataSet; (B) treat null item as a legitimate selectable DataSet row and specify what 'clear' means; (C) support clearing value only when no literal null item is present; (D) use an explicit 'no selection' operation at the adapter boundary. For each, give impact on legacy behavior, cursor correctness, model events, public API and migration risk. Do not invent a business rule or choose policy silently.
6. Produce ONE recommended smallest next RED/GREEN task and one precise architectural question for the user if policy remains ambiguous. Recommend stop/accepted limitation rather than broad changes if no safe contract is demonstrable. Mark CONFIRMED runtime, SOURCE-PROVEN, HYPOTHESIS, UNKNOWN distinctly. At timebox limit, stop with findings so far; no unbounded research.

### Hard constraints / reporting

READ ONLY: no production commits, no tracked tests, no push to migration branch, no modification of main/JInvCommon/Controls/JInvChoiceBox/Binder/DSChoiceBoxAdapter or unrelated controls, no shell background scheduler or new automation. Local ignored-target probes permitted. Report exact Java/JavaFX versions, base SHA and input provenance.
Only permitted write is `## Reply CTRL-016` appended to docs/codex/outbox.md on agent/codex-mailbox, after verifying SHA. Limit the reply to a compact matrix, source-cause, concrete recommendations and blockers; include enough detail for the architect to decide. Do not start CTRL-017. The hourly ChatGPT monitoring is notification-only, NOT authorization to merge or approve new production code.

---

## MESSAGE CTRL-017

**Status:** DONE / AWAITING INDEPENDENT USER PASS — agent Reply CTRL-017 reports RED/GREEN and published production 4e7ee03b6a85fac0e09b4a2d73d7cfce36e88eca. Do not rerun this task; independent Windows acceptance UNKNOWN.
**Priority:** CRITICAL — prevent NPE and stale FXEntity key when JInvChoiceBox value becomes null
**Repo:** ssu95a/fore25
**Implementation branch:** migration/java25-correctness (never main)
**Expected production HEAD:** 47c31310fac24692dc1b7b3118a127825de52a01
**Reply ID:** CTRL-017

### Confirmed failure / contract

CTRL-016 used a real FXEntity + `Binder.bindChoiceBox(IFXEntity,JInvChoiceBox)` with `valueFactory=Row::key` and reproduced this sequence: choice selection/value becomes null; the listener calls `valueFactory.apply(null)`; a NullPointerException reaches the FX event handler and FXEntity retains the previous key (2). This violates model synchronization. In `Binder.java` the code is exactly `fxEntity.getProperty(fieldName).setValue(valueFactory.apply((T)((ObjectProperty) observable).get()))` with no null handling. JavaFX ChoiceBox may briefly transition index 1 → -1 → 2 if a literal null item exists. Existing `DSChoiceBoxAdapter` and FXEntity binding may coexist, but must not be silently changed.

Acceptance: when control.getValue() becomes null, mapped FXEntity field must receive null with exactly one model notification for actual change, without exception. When control value is non-null, existing mapper still runs normally and exactly once for a genuine change. No new cursor navigation or model writes from the guard. `FXEntity model key -> control` existing `setSelectedValue(null)` NO-OP must remain unchanged; do not claim it now clears selection.

### RED/GREEN steps

1. Verify remote HEAD, branch, working tree. Create tracked real Java 25/JavaFX 25 GUI smoke with `JInvChoiceBox<Row,Integer>`, real annotated FXEntity and production Binder auto-binding path, `Row::key` STRICT on null (method reference throws), A/B items, model property observer. Establish non-null positive key A↔B, then RED with explicit programmatic `choice.setValue(null)` or `clearSelection` and assert old model key is improperly retained and NPE emitted. Run against baseline before editing, record exact value/model/index notifications and exception route. Use a fixture without literal null entries first.
2. Implement ONLY the minimal null check in `Binder.bindChoiceBox(IFXEntity,JInvChoiceBox)` on the control→FXEntity mapper path; for null selected value set entity property null without calling mapper; for non-null preserve mapping. Do not catch/ignore real mapper exceptions for non-null rows. Avoid unnecessary per-change allocations or new state/listeners. Do not alter initialization logic or model→control null policy in this task.
3. GREEN on A/B (no literal null): set value B→null via native calls, verify FXEntity key 2→null, one model change, no NPE; repeated null zero notifications; non-null A↔B, valueFactory mapping and direct programmatic model writes remain intact. If a model→control null write produces a pre-existing stale selection, document this as distinct and do NOT broaden fix.
4. Additional diagnostic: specialized Binder.bindChoiceBox DataSet with [A,B,null] and combined FXEntity mapping. Verify selection/cursor transitions and key consistency when value becomes null. It is ACCEPTABLE if the existing JavaFX 1→-1→2 intermediate transition still occurs; do not change or mask that behavior here. New guard must prevent NPE and stale key, but if it creates a new data-integrity regression or unexpected extra navigation, STOP and report BLOCKED rather than commit.
5. Test both exact and unmatched keys as appropriate. Keep the behavior of `Controls.setValue(JInvChoiceBox)`, `JInvChoiceBox.setSelectedValue(null)`, `DSChoiceBoxAdapter`, readOnly and literal-null policy unchanged. Verify existing smoke suite (at minimum ChoiceAdapter, ControlsFollowup, ValueChangedFlag, DataBinding, CheckBoxReadOnly, ComboReadOnly, RadioReadOnly, MoneyDecimal) when feasible; report unrun UNKNOWN. Maven -Pbuild compile, PowerShell syntax and git diff --check.

### Changes / Git

ONLY production edit allowed: `src/main/java/ru/inversion/fx/form/Binder.java` in `bindChoiceBox(IFXEntity, JInvChoiceBox)` null-mapper handling. Tracked test-only: new `src/test/probes/gui/BinderChoiceNullSmoke.java` (or equivalent) and new mode `-BinderChoiceNull` in `src/test/probes/Test-LegacyGuiSmoke.ps1`. No other production files, no POM/build changes. New comments in Russian.
Normal Git/GitHub operations APPROVED: commit narrow verified diff, fast-forward push to migration/java25-correctness, check remote SHA, never force-push or overwrite user work. Publish `## Reply CTRL-017` in docs/codex/outbox.md (agent/codex-mailbox) with RED/GREEN counts, exact SHA, regression list, and reproducible Windows command with -SettingsFile `D:\Java\.m2\settings.xml`. If BLOCKED, no production commit and STOP; do not start CTRL-018.

---

## MESSAGE CTRL-018

**Status:** DONE / AWAITING INDEPENDENT USER PASS — agent Reply CTRL-018 reports RED/GREEN and published production 11b1641e12efaa45cb82a8ec58f530b028f5acf0. Do not rerun this task; independent Windows acceptance UNKNOWN.
**Priority:** HIGH — null elements must not crash JInvChoiceBox.setSelectedValue scans
**Repo:** ssu95a/fore25
**Implementation branch:** migration/java25-correctness
**Expected base:** exact published final SHA from successful CTRL-017, not the old 47c31310 baseline
**Reply ID:** CTRL-018

### Confirmed failure / contract

CTRL-016 reproduced: with items `[A, null, B]` and `valueFactory=Row::key`, calling `setSelectedValue(2)` or unmatched key `99` throws when the loop reaches literal null (`valueFactory.apply(null)`), before reaching B. For valueFactory==null, `entry.equals(value)` also dereferences literal null. Unlike a projected null R from a non-null item, a literal null entry is a list element and must not be fed into ordinary row-key converters during a non-null key lookup.

Acceptance: for NON-NULL search key, skip literal-null list entries and continue scanning; choose matching non-null entry by projected R or equals(T) exactly as before. On unmatched key retain current selection/value/model, zero events, no exception. For `setSelectedValue(null)`, preserve the existing NO-OP contract. Do NOT automatically select a literal-null entry, clear selection or change its cursor policy; CTRL-014 remains BLOCKED.

### RED/GREEN steps

1. PRECONDITION: read fresh mailbox and implementation branch, confirm CTRL-017 DONE and remote SHA equals reported final SHA, local clean. If CTRL-017 BLOCKED or SHA differs unexpectedly, do not edit and report WAITING/BLOCKED.
2. New tracked JavaFX25 smoke: real JInvChoiceBox with data `[Row(A,1), null, Row(B,2)]` and strict Row::key; establish positive without-null list, then RED on key 2 and 99. Include plain `["A",null,"B"]` with no valueFactory, null-before-match and unmatched sentinel. Observe selectedIndex/item/value/model counts. Run original behavior before edit.
3. Implement ONLY smallest null guard in `JInvChoiceBox.setSelectedValue(R)` before invoking mapper OR entry.equals. Preserve loops, equality, projection behavior for non-null entries, public signatures and null-key no-op. Avoid allocation and per-instance state.
4. GREEN: B selected once for projected key 2; repeated key 2 no extra notifications; unmatched key 99 retains selection and emits zero notifications; mapper may return null for non-null entry and must not be treated as a matching non-null key; `setSelectedValue(null)` remains no-op; programmatic explicit select of literal-null index and existing DataSet cursor behavior are unchanged. Include both constructors and, if practical, combined FXEntity Binder after CTRL-017 to confirm its null handling remains GREEN.
5. Run Maven -Pbuild compile, PowerShell parsing, git diff --check and both `-BinderChoiceNull` / `-ChoiceNullLookup` plus existing accepted suite as feasible. Report all unrun UNKNOWN. No Robot needed for programmatic operations.

### Changes / Git

ONLY production edit allowed: `src/main/java/ru/inversion/fx/form/controls/JInvChoiceBox.java` method `setSelectedValue(R)`. Tracked test-only: new `src/test/probes/gui/ChoiceNullLookupSmoke.java` and `-ChoiceNullLookup` runner mode. Do NOT touch Binder (except the already published CTRL-017 commit), Controls, DSChoiceBoxAdapter, model/popup/readOnly, build, main or JInvCommon. New comments in Russian.
Normal Git/GitHub operations APPROVED: commit separate minimal change, fast-forward push to migration/java25-correctness and verify exact SHA, never force-push/discard work. Publish `## Reply CTRL-018` in docs/codex/outbox.md with RED/GREEN, diff/SHA, observed model/cursor effects and independent Windows command. Stop after CTRL-018: no CTRL-019 or CTRL-014 resumption without new architecture decision.

### Overall workflow

Both tasks are approved now, to be executed SEQUENTIALLY as two distinct production commits with two independent mailbox replies, without an additional user prompt between them. If any blocker arises in CTRL-017 or CTRL-018, STOP rather than guessing null/cursor semantics. Do NOT modify `Controls.setValue(JInvChoiceBox)` or resolve CTRL-014 by assumption.

---

## MESSAGE CTRL-019

**Status:** CLOSED — user independently confirmed Windows `CTRL-019 PASS`, exit 0 (2026-11-24). Production commit `a4edd28f2d68e461c676e01a5a72880d8929cd54` accepted for tested non-null writes. Generic null-clear remains BLOCKED under CTRL-014.
**Priority:** HIGH — make generic programmatic value writes work while preserving legacy null failure
**Repo:** ssu95a/fore25
**Implementation branch:** migration/java25-correctness (never main)
**Expected baseline remote HEAD:** 11b1641e12efaa45cb82a8ec58f530b028f5acf0
**Reply ID:** CTRL-019

### Source-evidenced issue and hard policy

`Controls.getValue(Object)` recognizes JInvChoiceBox and exposes the selected item T, but `Controls.setValue(Object,Object)` currently falls through to RuntimeException("Unsupported class control: ..."). CTRL-014 reproduced RED but STOPPED because `setValue(choice,null)` could inadvertently navigate a specialized DataSet cursor from B index 1 to literal null row index 2. CTRL-016 established further null/FXEntity distinction. Subsequent CTRL-017 and CTRL-018 now guard Binder null projection and skip literal null items during non-null lookup; NEITHER approved a generic clear policy.

**APPROVED ONLY:** support `Controls.setValue(choice, NON_NULL_R)` via existing `JInvChoiceBox.setSelectedValue(R)` semantics; explicitly preserve the old `Unsupported class control` exception for `Controls.setValue(choice,null)` (both overloads), without touching selection, value, model, cursor or events. Do not interpret or silently accept null. The method `JInvChoiceBox.setSelectedValue(null)` must remain its previous NO-OP. `Controls.getValue` still returns T, not R.

### RED/GREEN and production limits

1. Verify mailbox state, exact baseline SHA and a clean worktree. Compile a tracked Java 25/JavaFX 25 focused probe under `src/test/probes/gui` plus `-ChoiceNonNullSet` runner mode. Establish unlocked native `setSelectedValue` positive control. RED: `Controls.setValue((Node)choice,keyR)` and `(Object)` both throw unsupported; record selected-index/item/value/model counts and unchanged model/cursor.
2. Include both ChoiceBox constructors, A/B plain Strings, structured non-null rows T with projected R (e.g. Row::key), both null-free and `[A,null,B]` where CTRL-018 now permits scanning. Test valid R, repeats, unmatched R, a non-null item projecting a null R, initial null selection and a linked bidirectional T-valued model. Do not change how T and R map.
3. Explicit null regression: use `[A,B]` and `[A,B,null]` plus real specialized Binder.bindChoiceBox/DSChoiceBoxAdapter and monitor cursor. `Controls.setValue(choice,null)` must fail in the same unsupported manner as baseline and must not move cursor or emit selection/model notifications. If existing code cannot safely distinguish null, STOP without committing.
4. Implement ONLY one small conditional branch in `Controls.setValue(Object,Object)` in `src/main/java/ru/inversion/fx/form/controls/Controls.java`. It may delegate for non-null to `JInvChoiceBox.setSelectedValue`, and must retain the exact previous null exception behavior rather than selecting/clearing a literal-null row. Preserve precedence for ICustomValueControl, DatePicker, JInvComboBox, standard ComboBox etc. No new state, listeners or allocations.
5. Test generic DSControlAdapter non-null row-to-control path, initial/update values and unchanged unsupported null, plus legacy `-BinderChoiceNull`, `-ChoiceNullLookup`, `-ChoiceAdapter` and broader accepted smoke suite. Note separately that generic DSControlAdapter null navigation remains unsupported. Maven `-Pbuild compile`, PowerShell parse, `git diff --check`; no Robot needed.

### Git and report

Production edit ONLY Controls.java method setValue(Object,Object). Test-only edits in src/test/probes/gui and Test-LegacyGuiSmoke.ps1. Do not change JInvChoiceBox/Binder/DSChoiceBoxAdapter, FXEntity, null policy, readOnly, main/JInvCommon, POM/dependencies or other Controls methods. Russian production comments.
Git/GitHub commit + fast-forward push APPROVED if tests GREEN, verify remote SHA. Publish `## Reply CTRL-019` in docs/codex/outbox.md with separate RED/GREEN counts, exact SHA/diff and Windows command with `-ChoiceNonNullSet -SettingsFile "D:\Java\.m2\settings.xml"`. If BLOCKED, no production commit; stop all queued tasks and report.

---

## MESSAGE CTRL-020

**Status:** CLOSED — agent completed READ-ONLY F7 route investigation, Reply CTRL-020 `DONE / NO CHANGE` (2026-11-24). Current production call path not established; full application F7 runtime remains UNKNOWN; no implementation authorized.
**Priority:** MEDIUM — avoid unsupported-control failures or wrong search expressions in filters
**Repo:** ssu95a/fore25
**Implementation branch:** migration/java25-correctness (READ ONLY)
**Expected base:** final verified production HEAD from CTRL-019
**Reply ID:** CTRL-020

### Scope and evidence

`Controls.getStringValueFromEditor(Node)` supports Label, JInvValueField, TextInputControl, CheckBox, DatePicker and ComboBox, but not JInvChoiceBox; a direct ChoiceBox invocation reaches `Unsupported class control`. `F7FilterDialog.ExpressionChangeListener.changed` calls this method on `baseControl` / an HBox's `baseControlInner` when switching a filter to expression mode, then forwards text using `Controls.setValue`. However, `F7FilterDialog.getComboBoxFromControl` builds its own JInvComboBox wrappers for some types. **UNKNOWN** whether a real JInvChoiceBox ever reaches this exact expression path. Do not assume an Unsupported exception is a business-form bug until a real call chain is demonstrated.

### Bounded READ-ONLY investigation

1. Read F7FilterDialog control-construction/iteration and ExpressionChangeListener, Controls.getStringValueFromEditor and relevant ValueConverters. Trace where JInvChoiceBox is accepted, cloned, skipped or replaced by JInvComboBox. Confirm the actually reachable control type and semantics of editor string vs selected object T vs projected key R. Search concrete application call sites and filter subclasses; distinguish SOURCE-PROVEN, CONFIRMED runtime, HYPOTHESIS, UNKNOWN.
2. With temporary ignored-target probes (no tracked changes), establish a working positive control for F7 expression switching from a supported String/ComboBox/DatePicker case, then attempt a real JInvChoiceBox path only if constructible under stock frontend. If full F7 dialog needs unavailable BaseApp/DB/context, say runtime UNKNOWN rather than inventing a fake route.
3. Check whether supported JInvCheckBoxString is rendered as plain Boolean in getStringValueFromEditor while the actual F7 wiring bypasses that call; determine if this is intentionally existing behavior or a genuine incorrect conversion. Do not redesign filter semantics.
4. Evaluate one minimal *future* fix only if a reachable bug is demonstrated. Compare `StringConverter.toString(selected T)`, `TypeConverter.convertToString(T)` and valueFactory projected R; do not choose representation without an observed F7 expectation. No production edits; if the issue is unreachable, recommend NO CHANGE and close.
5. Report a concise reproduction matrix with source paths, baseline/final SHA (unchanged), user-visible failure if any, no duplicate regression notifications, and a single recommendation for user decision if needed. Stop after `## Reply CTRL-020`, without a production commit.

Hard scope: read only everywhere except appending `## Reply CTRL-020` to docs/codex/outbox.md on agent/codex-mailbox, temporary untracked probes only in ignored target. Never edit Controls, F7FilterDialog, JInvChoiceBox, Binder, build, main/JInvCommon. If CTRL-019 BLOCKED or production SHA unexpected, report WAITING and do not start.

---

## MESSAGE CTRL-021

**Status:** CLOSED — user independently confirmed Windows `CTRL-006 PASS`, exit 0, using fixed RadioReadOnlySmoke from CTRL-021 (2026-11-24). Test-only commit `ce32f72ccc4a03ccc09f959da9b9d2e77d00f993` accepted. No production radio control code changed.
**Priority:** HIGH — credible GUI regression signal for JavaFX25 legacy radio groups
**Repo:** ssu95a/fore25
**Implementation branch:** migration/java25-correctness
**Expected base:** final CTRL-019 production SHA, unchanged by read-only CTRL-020
**Reply ID:** CTRL-021

### Evidence / limits

Repeated past agent reports for CTRL-015/017/018 and ControlsFollowup described intermittent failures of `-RadioReadOnly` at the 'mixed group unlocked arrow' positive control, including initial direct batch/standalone failures followed by an official paired rerun PASS. The `RadioReadOnlySmoke` invokes targeted `Event.fireEvent` keys and optional Robot; it changes focus and group membership between cases. Root cause currently UNKNOWN: do not presume a timing issue or dismiss a real regression.

### Focused investigation and conditional test-only fix

1. Verify CTRL-020 published DONE and production HEAD unchanged from CTRL-019; verify clean workspace. Read real `RadioReadOnlySmoke`, `JInvRadioButton` + JInvRadioGroup, JavaFX25 RadioButton/ToggleGroup behavior and test runner. Identify who owns arrow traversal and selection for mixed Legacy/stock groups.
2. Run the official `-RadioReadOnly -SettingsFile "D:\Java\.m2\settings.xml"` probe on Windows Java25/JavaFX25 across multiple fresh processes (bounded 10 runs, stop earlier after a credible reproduced failure). Establish whether the unlocked-arrow positive fails under targeted dispatch; record each run's exit code and selected/focused toggles, whether group readOnly and native disabled are false, and event order. Do not use Robot unless exclusive input is available.
3. Add temporary ignored-target diagnostics for focus owner, scene/window activation, selected toggle, readOnly flags, handler installation, toggles membership, event consumption, pending Platform.runLater work, and asynchronous skin CSS if relevant. Distinguish (A) test setup/timing false failure, (B) real production race/correctness bug, (C) environment/RDP interference. Check paired original/fixed input baseline if necessary.
4. If test-only cause is proven, make the SMALLEST test-only correction in `src/test/probes/gui/RadioReadOnlySmoke.java` and optionally its existing runner; preserve genuine unlocked positive control and all locked negative assertions. Do not reduce checks, add unconditional sleeps to hide failures, or turn an expected user gesture into direct programmatic selection to manufacture PASS. Rerun bounded fresh-process loop and relevant radio/databinding regression modes.
5. If a production bug is implicated or root cause UNKNOWN after bounded investigation, do NOT edit production or weaken tests. Report BLOCKED / needs standalone production task with reproduction and targeted recommendations. No broad Controls or RadioButton redesign.

### Git and report

Production source READ ONLY: JInvRadioButton, JInvRadioGroup, Controls, JInvChoiceBox, Binder and all other production classes MUST NOT change under CTRL-021. Test-only changes allowed to RadioReadOnlySmoke.java and relevant test runner if proven necessary. No new per-instance fields or listeners in production.
Git/GitHub operations APPROVED ONLY for a verified test-only fix: fast-forward commit/push to migration/java25-correctness, verify remote SHA. If no reproducible test-only bug, no production/test commit. Publish `## Reply CTRL-021` in docs/codex/outbox.md with per-run matrix, exact failure evidence, scope of any test-only diff, PASS/BLOCKED and final SHA. Stop: do not assign CTRL-022.

### Queue and safety invariants

Execute CTRL-019 → CTRL-020 → CTRL-021 SEQUENTIALLY with separate outbox replies. Stop the queue on any BLOCKED (including policy ambiguity, failing positive control, unexpected branch advance, or regression). Do not implement CTRL-014 null-clearing, rework accepted readOnly limitations, or pull deferred CAL-TIME-001 seconds into these tasks. Existing CTRL-017/018 DONE reports are not independent user acceptance; preserve their tested invariants and surface any new failures.

---

## MESSAGE CTRL-022

**Status:** CLOSED — user independently confirmed Windows `CTRL-022 PASS`, exit 0, on 2026-11-24. Agent's test-only commit `a5901c251ce76a122a31e8c832b3199f33735c24` accepted. Full corporate/MDI execution UNKNOWN.
**Priority:** HIGH — form return status and single cleanup/callback correctness
**Repo:** ssu95a/fore25
**Implementation branch:** migration/java25-correctness; baseline remote HEAD `ce32f72ccc4a03ccc09f959da9b9d2e77d00f993`
**Reply ID:** CTRL-022

### Grounding and narrow scope

Existing tracked `src/test/probes/gui/LegacyFormLifecycleSmoke.java` (GUI-010) proves the positive OK route with real production FXML/controller, stage, `ON_OK → ON_CLOSE`, one RET_OK callback and resource-progress cleanup. Production `AbstractBaseController.configStandartButtons()` wires btCancel to `internalCancel`, while `internalOnClose` calls onCancel for RET_CANCEL, may consume close requests if onCancel refuses/WAIT state, and eventually calls `internalActionsOnClose` on WINDOW_HIDDEN. `closeNow()` intentionally bypasses onOK/onCancel and should not be conflated with normal cancel. A singleton-map removal exists in `internalOnClose` finally even on cancelled close; this is source observation, not a proven runtime bug. Do not change production by speculation.

### Test-only execution

1. Verify branch, exact HEAD and clean worktree. Reuse real `JInvTextAreaForm.fxml` / `JInvTextAreaFormController`, ViewContext and production standard buttons with JavaFX25 Stage in a tracked *new* `LegacyFormCancelSmoke` probe; do not modify the accepted GUI-010 positive test.
2. Positive Cancel: show and initialize form in no-DB VM_NONE or equivalent supported mode; fire btCancel, assert exactly one `WINDOW_CLOSE_REQUEST`, `WINDOW_HIDDEN`, RET_CANCEL callback, lifecycle `ON_CANCEL`/`ON_CLOSE` in the real order, no lingering Stage/progress windows, no duplicate callback. Test window X separately. Do not infer lifecycle states from code alone; observe them.
3. Explicit veto: use a controlled test controller overriding onCancel to return false (only if achievable without rewriting production/FXML), assert close request is consumed, Stage remains open and no `onCloseResources`/return callback fires; subsequent accepted cancel closes once. Test `closeNow(RET_CANCEL)` separately only if its intended bypass can be verified safely. If singleton registry is accessible by normal form launcher, verify no premature removal on veto; otherwise mark UNKNOWN.
4. Establish valid positive controls and end-to-end FX pulsing; no unconditional sleeps, no reflection into private JavaFX internals. If a scenario requires BaseApp, DB session or unsupported MDI environment, mark UNKNOWN, do not fabricate PASS. Record exact callback and event counts.
5. ONLY tracked test changes: new `src/test/probes/gui/LegacyFormCancelSmoke.java` and `-FormCancel` runner mode in `src/test/probes/Test-LegacyGuiSmoke.ps1`. Production read-only. If test reveals actual lifecycle bug, STOP and report BLOCKED with reproducible RED, not a broad controller rewrite.
6. Run Maven `-Pbuild compile`, PowerShell syntax, `git diff --check`, new probe and existing GUI-010 `-FormLifecycle`, GUI-009 `-DataBinding`, plus relevant controls regressions. If test-only GREEN, commit/push to implementation branch by fast-forward, verify remote SHA. Publish `## Reply CTRL-022` in mailbox with exact test diff/SHA, event matrix, UNKNOWN and Windows rerun command. If no useful test feasible, report NO_CHANGE with reason; no commit.

---

## MESSAGE CTRL-023

**Status:** CLOSED — completed READ-ONLY `Reply CTRL-023`: partial bindings confirmed after error; no approved rollback semantics. No production changes; follow-up only if actual caller reuse proves a defect.
**Priority:** HIGH — avoid partially attached control/model listeners after binding failure
**Repo:** ssu95a/fore25
**Implementation branch:** migration/java25-correctness (READ ONLY)
**Expected base:** final verified branch SHA from CTRL-022, not fixed to initial baseline
**Reply ID:** CTRL-023

### Source-confirmed risk

`src/main/java/ru/inversion/fx/form/Binder.java` public `bind(IFXEntity,List<Node>,AbstractBaseController)` iterates controls, catches individual exceptions into `listException`, continues binding other controls, calls `bindNonControlComponent`, and then throws an exception if any failed. Already-successful bidirectional/listener bindings may remain installed after the exception. Correctness/rollback contract UNKNOWN: do NOT assume partial binding is always a bug. Binder has different strategies for TextInputControl, JInvValueField, CheckBox, DatePicker, ComboBox, ChoiceBox, and non-control radio groups.

### READ-ONLY investigation

1. Verify successful CTRL-022 handoff, clean worktree and current exact production SHA. Trace Binder.bind control order, listener ownership, lifecycle cleanup and caller behavior after exceptions; identify which bindings are reversible and which are deliberately lazy. Review actual FXEntity and controller integration. Do not propose a naive transaction over JavaFX property bindings.
2. In ignored `target/ctrl023` local diagnostics, build minimal annotated FXEntity, real TextField/CheckBox (or another well-grounded pair), one valid field binding followed by an invalid binding that actually throws, plus a third valid control. Capture whether Binder attaches first/third, exact exception, control↔model propagation and listeners after thrown bind. Establish all-valid positive controls first. Prefer no BaseApp/DB, no native GUI input required.
3. If possible, exercise re-running Binder.bind on same controls after fixing bad field metadata; determine whether duplicate listeners or bidirectional bindings appear and whether events are emitted more than once. For each observed case record text/model notification counts and data integrity. Confirm whether the form caller aborts/reuses partially bound controls.
4. Report CONFIRMED runtime, SOURCE-PROVEN, HYPOTHESIS, UNKNOWN separately. Evaluate candidate policies (fail-fast before any binding, best-effort with explicit partial state, or rollback) and their compatibility/per-instance memory costs. Give ONE narrow next task only if root cause and safe contract are proven.
5. READ-ONLY: no tracked tests, production, Binder/controller/FXEntity changes, no POM/dependencies, main/JInvCommon read-only. Only ignored target diagnostics and `## Reply CTRL-023` appended in mailbox. Report DONE/NO_CHANGE with evidence even if it reveals an open risk; STOP the queue only if tests unexpectedly prove a regression that makes continuing unsafe or if prerequisites fail. No production commit.

---

## MESSAGE CTRL-024

**Status:** CLOSED — user independently confirmed Windows `CTRL-024 PASS`, exit 0, on 2026-11-24. Agent production commit `7282d378cf144d15bfb61b0cf944def96cbb577b` accepted; no unrelated changes.
**Priority:** MEDIUM/HIGH — avoid retained Node.properties allocations during hotkey dispatch while preserving action precedence
**Repo:** ssu95a/fore25
**Implementation branch:** migration/java25-correctness
**Expected base:** verified SHA after CTRL-023 (unchanged if READ-ONLY),
**Reply ID:** CTRL-024

### Confirmed source and contract

`src/main/java/ru/inversion/fx/form/action/SceneKeyManager.java` `getActionRecursive` calls `node.getProperties().getOrDefault(Controls.CONTROL_PARENT, null)` for each visited node when no matching action. JavaFX25 `Node.getProperties()` lazily creates an observable map and backing HashMap. A normal F7/F8 or other key search can therefore leave metadata allocations in controls/parents that previously had none. This is source-proven; runtime materiality/route must be RED-tested. The method must preserve `CONTROL_PARENT` overrides, ordinary nearest-parent recursion, first matching enabled action selection and exact consumption.

### RED/GREEN

1. Verify current HEAD and clean working tree. Create tracked `SceneKeyLookupSmoke.java` under src/test/probes/gui and `-SceneKeyLookup` runner. Use attached JavaFX 25 Stage/Scene, stock focused Control and nested Parent nodes without preexisting property maps, plus actual `SceneKeyManager.addAction` on an ancestor and a scene/global action. Establish working action positive control (exactly one invocation/key press, expected focus/target/consumed). Capture `Node.hasProperties()` for pristine visited nodes BEFORE and AFTER key dispatch; RED must prove persisted mutation during lookup without manually causing properties allocation in probe.
2. Verify nearest-context action beats global fallback, CONTROL_PARENT logical override takes effect, nonmatching key does not execute, disabled/security-blocked action does not execute, event type KEY_PRESSED vs KEY_RELEASED, filter/handler dispatch precedence as supported. Be careful: JavaFX event filters for `KeyEvent.ANY` plus concrete types are registered; do not assume duplicate invocation without observing it. Distinguish expected map on Scene root from unused nested Nodes.
3. If RED and positive controls are credible, implement ONLY a minimal `node.hasProperties()` guard in `getActionRecursive` to avoid `node.getProperties()` on nodes without metadata. Must still look up an existing CONTROL_PARENT and keep recursive traversal unchanged; no new per-Node fields, listeners or collections. Do not redesign action maps, manager initialization, event scheduling, or fix other unrelated issues.
4. GREEN checks same action counts/order/consumption, zero properties-map creation from read-only lookup on pristine nodes, existing property map object identity preserved, and `CONTROL_PARENT` behavior identical. Run new probe across several fresh JavaFX25 processes, Maven `-Pbuild compile`, PowerShell syntax, diff check and relevant GUI-009/010, RadioReadOnly and ControlsFollowup regressions. If no reliable RED or change would alter semantics, STOP/BLOCKED without production commit.
5. ONLY production edit: SceneKeyManager.java method getActionRecursive. Tests only new focused src/test/probes/gui and runner mode. Comments newly added in Russian. No modifications to Controllers/Controls/Binder/ChoiceBox/Radio, JInvCommon, main, modules or dependencies. Commit verified minimum, fast-forward push and verify SHA. Publish `## Reply CTRL-024` with before/after property maps and action counts, exact diff/SHA, Windows rerun command `-SceneKeyLookup -SettingsFile "D:\Java\.m2\settings.xml"`.

---

## MESSAGE CTRL-025

**Status:** CLOSED / FINDINGS ACCEPTED — READ-ONLY `Reply CTRL-025` proved stale and duplicate JInvTextField LOV listeners plus default check `doSetValue=false` mutation; follow-ups CTRL-026 and CTRL-027 assigned separately; original task not to rerun.
**Priority:** HIGH — business field integrity on LOV accepted/cancelled selection
**Repo:** ssu95a/fore25
**Implementation branch:** migration/java25-correctness (READ ONLY)
**Expected base:** final verified SHA from CTRL-024
**Reply ID:** CTRL-025

### Existing source path / no implied new LOV architecture

Legacy `JInvTextField.setLOV` listens to AbstractLovBase.valueProperty and calls `setTextValueFromLov`, which can replace or append to editor text; `showLOV` delegates to ILov.showChoiceList and its callback intentionally avoids repeating setText because of historical double notifications. `JInvLOVButton` delegates to the field and on accepted choice may forward keyboard navigation. `JInvEntityLov.showChoiceList` launches a modal ViewEntityLovController and its choiceResult calls `setEntityValue` on RET_OK and invokes callback. This is auditing current legacy behavior, NOT implementing declarative XML lxml LOV from scratch.

### Bounded READ-ONLY audit

1. Verify CTRL-024 status and current HEAD; no overlapping work. Trace actual `JInvTextField.setLOV`, `addLovValueListener`, `showLOV`, `setTextValueFromLov`, `AbstractLovBase.checkValue`, `JInvLOVButton` action, `JInvEntityLov.choiceResult`, `LovValidator` and `ButtonValidationListener`. Source trace event and callback ordering for selected result vs cancel and for repeated invocation.
2. Use ignored `target/ctrl025` temporary JavaFX25 fixture with real JInvTextField, bound SimpleStringProperty model, a minimal test AbstractLovBase/ILov subclass returning chosen value via contract and callback, and actual attached Stage if needed. Establish positive controls (programmatic initial LOV attachment and accepted single selection) and record exact text, model, LOV value, validation and callback notifications. Test CANCEL (no new value or model change), ACCEPT (one change), repeated equal value (zero redundant notification), replacement vs appendFromLOV and selected-text replacement where feasible.
3. Test replacing LOV instance on same field, old vs new LOV value changes, and repeated show/cancel, to identify potential stale listeners or unexpectedly retained references. Verify whether a double write is actual event count or just two same-value attempts. Do not claim memory leak without a grounded strong-reference path. If actual modal ViewEntityLovController needs BaseApp/DB and is unavailable, mark real dialog integration UNKNOWN instead of substituting fake result for full E2E.
4. Give a concise classification of CONFIRMED runtime, SOURCE-PROVEN, HYPOTHESIS, UNKNOWN. Recommend ONE smallest correctness fix if required, otherwise NO_CHANGE. Do not change selection/validation/append semantics without separate approval.
5. READ-ONLY: no production or tracked test changes, no XML parser prototype, no changes to `JInvEntityLov`, `JInvTextField`, `LovValidator`, `ButtonValidationListener`, main/JInvCommon, POM/dependencies. Only ignored target probes and `## Reply CTRL-025` mailbox publication; no implementation branch push. Stop after CTRL-025. No CTRL-026 self-assignment.

### Queue discipline for all four

Execute CTRL-022 → CTRL-023 → CTRL-024 → CTRL-025 sequentially. A task may finish DONE (including test-only change), DONE/NO_CHANGE (READ-ONLY analysis), or BLOCKED. On BLOCKED stop the ENTIRE remaining queue and report. Before every task verify the prior Reply and current remote production SHA. Never assume a future commit SHA. Every task has a separate outbox Reply with exact evidence and a concise architect-facing summary. Do not touch blocked CTRL-014 generic ChoiceBox null clearing or deferred CAL-TIME-001. Remote writing requires ordinary fast-forward push only. No force-push, no main/JInvCommon writes. Independent Windows acceptance is not implied by agent PASS.

---

## MESSAGE CTRL-026

**Status:** DONE BY AGENT / USER PASS PENDING — Reply CTRL-026, production 0c3af0c77f4c056314e268764184e857b4c423b8; do not rerun
**Priority:** CRITICAL — stale listeners duplicate model writes / retention / NPE
**Repository:** ssu95a/fore25
**Branch:** migration/java25-correctness (NEVER main)
**Baseline:** 7282d378cf144d15bfb61b0cf944def96cbb577b
**Reply:** CTRL-026

### Reproduced in prior READ-ONLY CTRL-025

Production JInvTextField.setLOV(AbstractLovBase,boolean) calls `this.lov.set(lovObj)` then `addLovValueListener(lovObj)`; listener is an inline lambda on `lov.valueProperty` that captures the field and reads current `getLOV()`. Neither replacement nor `unbindLOV()` detaches old registrations. On old LOV write after replacement active LOV value was appended again; setting same LOV twice caused two appends; after unbind, old LOV write caused NPE. These are CONFIRMED JavaFX25 and source-proven, not hypothetical.

### Contract and RED/GREEN

1. Verify expected SHA, branch, clean worktree. Add tracked real JavaFX25 fixture using JInvTextField, `SimpleStringProperty` bidirectional model, minimal concrete AbstractLovBase provider and stage only as necessary. RED on old LOV replaced by new: writing former LOV MUST NOT update current field/model; attaching same LOV twice MUST NOT duplicate writes; unbind LOV then external old provider write MUST NOT trigger field NPE. Establish positive single-LOV update and unchanged initial-binding behavior first.
2. Implement smallest ownership fix confined to `src/main/java/ru/inversion/fx/form/controls/JInvTextField.java`: at most ONE active value listener owned by field and associated with current LOV; detach on replacement/unbind, attach on new instance, do not multiply on same LOV reattachment. Prefer lazily allocating callback/state only once a LOV is first used; no eager memory overhead on every JInvTextField instance. Do not use weak listeners that silently disappear without strong references, and do not allocate a fresh listener per LOV update.
3. GREEN with selected text replacement, appendFromLOV (including comma quoting), repeated equal value, distinct replacement and unbind, repeated show/cancel callbacks where available. Preserve current `setTextValueFromLov`, validation, `onSetLov`, button creation, and properties semantics; explicitly test that detachment does not remove *other consumers'* listeners on the LOV. Avoid rewriting dialog or fixing unrelated `doSetValue` contract (CTRL-027). Do not force null text on detach.
4. Check alternative entry path public `lovProperty().set(...)` as diagnostic: if it bypasses setter and breaks invariant, document UNKNOWN/API limitation or STOP if contract requires property binding; do not broaden silently. If concurrent ownership of same LOV across different fields creates new regressions, STOP BLOCKED.
5. Maven `-Pbuild compile`, PowerShell parse and `git diff --check`; `-LovListenerLifecycle` RED/GREEN + existing TextFields, DataBinding, FormCancel, BinderChoiceNull and other applicable smoke. Include no extra production fields for controls with no LOV in baseline; record per-instance memory cost.

**Git approval:** ONLY JInvTextField.java production, a NEW tracked src/test/probes/gui/LovListenerLifecycleSmoke.java, and `-LovListenerLifecycle` runner parameter. Russian source comments. Commit+fast-forward push ONLY with GREEN, verify remote and publish `## Reply CTRL-026` with RED/GREEN numbers, SHA, exact files, and independent Windows command. On BLOCKED no production commit, stop queue.

---

## MESSAGE CTRL-027

**Status:** DONE BY AGENT / USER PASS PENDING — Reply CTRL-027, production 2390e7cdac4a463b1be24082b6502df0e54a4aa7; do not rerun
**Priority:** HIGH — `validateOnOk` must not mutate field/model by default check
**Repository:** ssu95a/fore25
**Branch:** migration/java25-correctness
**Expected baseline:** exact final SHA published by CTRL-026
**Reply:** CTRL-027

### Confirmed evidence / intended limited contract

`LovValidator.validateOnOk(value)` passes `doSetValue=false` to `AbstractLovBase.checkValue(value, false)`; the **base-class** `AbstractLovBase.createDefaultCheck()` returns `(t,u)->{setValue(t); return true;}` and ignores u. CTRL-025 reproduced that validateOnOk with default predicate mutates LOV, field text and model. Other providers (especially `AbstractEntityLovBase.createDefaultCheck()` and custom `setCheck`) can use distinct semantics and MUST NOT be changed in this task.

### RED/GREEN

1. Confirm remote HEAD == CTRL-026 reported SHA and clean tree; establish real direct base `checkValue(T,false)` and attached JInvTextField/LovValidator.validateOnOk smoke, plus positive `checkValue(T,true)` and `LovValidator.validate` with defaults. RED for false current LOV value, control text and model are mutated. Include equal-value no-op, true positive, and false checks for both null and non-null when non-equivalent.
2. Change ONLY `AbstractLovBase.createDefaultCheck()` (or minimal base default predicate) so default `doSetValue=false` means validate/return true without setting LOV value; `doSetValue=true` still sets exactly once for actual change. No modifications to checkValue's equality shortcut, custom predicates, EntityLov, SQLDataSet, validators, other LOV fields.
3. GREEN single active LOV listener, zero field/model/LOV writes on read-only validation, one on true actual change, no NPE after replacement/unbind. Run both focused `-LovDefaultValidation` and prior `-LovListenerLifecycle`, relevant `-TextFields`, `-FormCancel`, `-DataBinding` and accepted smoke. Ensure both direct AbstractLovBase and LovValidator contract. If base predicate false mutation is required by proven legacy contract, STOP/BLOCKED with evidence rather than changing semantics.
4. Production edit ONLY `src/main/java/ru/inversion/fx/form/lov/AbstractLovBase.java`; tests new src/test/probes/gui/LovDefaultValidationSmoke.java + runner `-LovDefaultValidation`. No extra per-instance state. Maven compile, PowerShell syntax and git diff --check. Russian comments if needed.

**Git:** independent commit, fast-forward push, verify exact HEAD, publish Reply CTRL-027 with before/after mutation counts + SHA. On BLOCKED stop all remaining tasks.

---

## MESSAGE CTRL-028

**Status:** CLOSED / ARCHITECTURE FINDINGS — Reply CTRL-028; same-mode button reattachment issue, see CTRL-034
**Priority:** HIGH — prevent wrong LOV or missing editor reference in dual setter usage
**Repository:** ssu95a/fore25
**Branch:** migration/java25-correctness (READ ONLY)
**Expected baseline:** latest CTRL-027 SHA
**Reply:** CTRL-028

### Source evidence and UNKNOWN contract

`JInvLOVButton` keeps a `Pair<TextInputControl,ILov>` in node property LOV_BUTTON_DATA. `setTextField` writes `(ed,null)`, while `setLOV` writes `(null,lov)`; both setters overwrite previous pair. In `LOVHandler`, control-present flow differs from standalone LOV button. Whether both setters are intended to coexist or mutually exclusive is UNKNOWN. Treat this as an architectural question, not permission for speculative 'fix'.

### Investigation

1. Inspect JInvChoiceButton and all callers, JInvTextField.setLOV (creates a button and setTextField) and direct JInvLOVButton(ILov) constructors. Separate actual supported modes: field-bound button delegates field.showLOV, standalone button uses own ILov. Trace field binding and ButtonValidationListener focus/keyboard behavior, including cancel handling.
2. With ignored target-only JavaFX25 fixture establish positive field-only mode, provider-only mode, and sequential `setTextField→setLOV` and reverse order; capture getTextField/getLOV and button action/callback. Do not invoke real modal without controlled fake ILov, BaseApp/DB limitations -> UNKNOWN.
3. Examine setTextField repeated attachment and Boolean disable/visible binding: check potential double binding or IllegalStateException on reuse and orphaned old field references. Record actual exceptions/retained maps; do not extrapolate an app-level leak without evidence.
4. Recommend whether API modes should stay exclusive or require combined support, with smallest possible future correction and safety/compatibility implications. **NO production or tracked edits**, no commits on implementation branch. Only ignored target diagnostics and `## Reply CTRL-028` in outbox; record source+runtime, unknowns.
5. If contract ambiguity is material, report DONE / ARCHITECTURE DECISION NEEDED without calling the task BLOCKED; continue to independent CTRL-029. Never change JInvLOVButton, JInvChoiceButton, Binder, JInvTextField or other production code.

---

## MESSAGE CTRL-029

**Status:** CLOSED / CONFIRMED BUG — Reply CTRL-029; duplicate Singleton window after veto, see CTRL-032
**Priority:** HIGH — lifecycle correctness: prevent opening duplicate SingletonInstance forms after veto
**Repository:** ssu95a/fore25
**Branch:** migration/java25-correctness (READ ONLY)
**Expected baseline:** final verified CTRL-027 HEAD, unchanged by CTRL-028
**Reply:** CTRL-029

### Source-proven risk

`AbstractBaseController.internalShow()` registers `@SingletonInstance` controllers in `g_controllerInstanceMap`; `internalOnClose()` has a `finally` block that removes the instance after try, even when a normal Cancel close is vetoed and window remains open. CTRL-022 confirmed veto keeps Stage open and returns no callback but did not inspect singleton map. Potential duplicate-controller opening is HYPOTHESIS pending genuine launcher path.

### Bounded forensic task

1. Verify prior report, remote SHA, clean worktree. Source-trace singleton registration, stage creation, internalOnClose return paths (veto, validation failure, exception, closeNow, SDI/MDI), map removal, and existing tests/SingletonInstance annotation.
2. Attempt ignored target-only runtime probe using real production launcher and minimal annotated controller to register singleton, veto close, open second instance and observe whether second is suppressed/brought to front; after successful close re-open and verify acceptance. If BaseApp/DB/main frame prevents real launcher, use read-only isolated introspection to verify map state only, report end-to-end duplicate as UNKNOWN, do not fabricate a real popup.
3. Distinguish ERROR cleanup from ordinary Cancel/veto and pending asynchronous finalization. Consider exception and return paths; no naive move of map.remove that causes a stale singleton on error.
4. Deliver CONFIRMED vs SOURCE-PROVEN vs HYPOTHESIS vs UNKNOWN, detailed reproduction and smallest proposed fix in *separate future task* if warranted. No production/test tracked changes or implementation branch commits. Only ignored target diagnostic and Reply CTRL-029. If unresolved, DONE/NO CHANGE with recommendation, do not hold independent CTRL-030.

---

## MESSAGE CTRL-030

**Status:** CLOSED / CONFIRMED BUG — Reply CTRL-030; cross-control validator unbind, see CTRL-033
**Priority:** HIGH — correctness and allocation/retention when a control is revalidated/rebound
**Repository:** ssu95a/fore25
**Branch:** migration/java25-correctness (READ ONLY)
**Expected base:** final CTRL-027 SHA, unchanged by CTRL-028/029
**Reply:** CTRL-030

### Source grounding

`ValidMan.bindValidators2Control` adds validators via `getValidators(c).addAll`, `unbindValidators` removes; `initListenersOnControl` guards focus listener through Controls.CONTROL_FOCUS_LISTENER, then installs change listeners through `initChangeListener`; `JInvTextField.setLOV` installs LovValidator if missing. Existing `ButtonValidationListener` replays focus/click after validation. Whether unbind/rebind or controller recreation leaves duplicated validations and listener references is UNKNOWN.

### Audit

1. Trace ValidMan initialization, observer lists, getValidators(c), bind/unbind and focus/changed listeners including potential leaks when controller is destroyed or control moved to another form. Distinguish initial bind vs repeated same `ValidMan` and NEW ValidMan, and enumerate real lifecycle caller paths.
2. Use ignored JavaFX25 probe with actual field and a counting Validator, single/duplicate add/remove, focus change, programmatic text edits. Record validation invocations and GUI notifications. Include no-LOV control as allocation/memory sentinel and a LOV-validated field after CTRL-026/027. Count whether repeated bind registers one or more focus listeners and whether stale managers still validate or retain control.
3. Do NOT silently deduplicate validator instances or change focus semantics: repeat bindings may be intentionally supported. Identify one verified data-integrity issue if any and an isolated next step. Source evidence alone about listener existence is not enough to assert a leak. SQL/DB integration and production corporate app remain UNKNOWN unless actually run.
4. READ-ONLY; no implementation branch edits, no tracked tests, no ValidMan/JInvTextField production code, no new dependencies. Publish Reply CTRL-030 with evidence and recommendation. If standalone fixture cannot run reliably, report UNKNOWN rather than failing adjacent tasks.

---

## MESSAGE CTRL-031

**Status:** DONE BY AGENT / USER PASS PENDING — Reply CTRL-031, test-only 3805ac5ba9096de48fc9697014d1d8063805dd29; do not rerun
**Priority:** HIGH — safeguard corrected LOV lifecycle against actual field/model regressions
**Repository:** ssu95a/fore25
**Branch:** migration/java25-correctness
**Expected base:** final CTRL-027 SHA (READ-ONLY CTRL-028/029/030 must not alter production branch)
**Reply:** CTRL-031

### Integration regression goals

1. Verify exact HEAD and completed predecessor replies; clean worktree. Add ONE tracked JavaFX25 smoke `LovIntegrationSmoke.java` under src/test/probes/gui, `-LovIntegration` PowerShell runner mode, reusing production JInvTextField + AbstractLovBase + LovValidator and a model `SimpleStringProperty` in a real Stage. Prefer constructing an isolated provider that implements a deterministic chosen result via ILov.showChoiceList callback, NOT a fake full DB-backed ViewEntityLovController. If BaseApp is necessary, use a minimal test subclass only to meet real control preferences.
2. Cover accepted choose (once updates value/text/model), cancel (zero writes), repeated equal choices (zero events), old LOV writes after replacement (zero writes), same LOV reattached (one write), unbind (no NPE and zero stale writes), append delimiter quoting, and base `validateOnOk` nonmutating vs normal validation one write. Test only accepted semantics, no change to generic ChoiceBox null clearing.
3. Test actual `JInvLOVButton` click and keyboard forwarding only if a real scene is available and paths are deterministic; report unsupported interaction UNKNOWN without fabricating pass. No Robot required. Do not modify JInvLOVButton semantics based on CTRL-028 research without separate policy approval. Test exact event counts, callback order, model values and no duplicate listeners.
4. If a real regression appears from accepted CTRL-026/027 implementation, STOP and report BLOCKED with RED; do NOT silently change production in this test-only task. If GREEN: run new probe and existing `-LovListenerLifecycle`, `-LovDefaultValidation`, `-FormCancel`, `-FormLifecycle`, `-TextFields`, `-DataBinding`, `-SceneKeyLookup`, `-ControlsFollowup` where practical; report unrun UNKNOWN.
5. Allowed tracked edits ONLY new LovIntegrationSmoke.java and adding `-LovIntegration` runner parameter. No production edits, POM changes, main/JInvCommon writes. Maven -Pbuild compile, PowerShell syntax, diff check. Commit/push fast-forward only after GREEN, verify SHA, publish Reply CTRL-031 with independent Windows command and an overall six-task status table. Then STOP; no self-assigned CTRL-032.

### Global queue rules

Execute CTRL-026 → CTRL-027 → CTRL-028 → CTRL-029 → CTRL-030 → CTRL-031 **strictly sequentially**. On any actual BLOCKED test/production defect, stop entire remainder rather than guessing; READ-ONLY audits may conclude DONE/NO_CHANGE or DONE/ARCHITECTURE DECISION NEEDED and continue where no blocking regression exists. Verify exact remote production SHA, working tree, mailbox prior reply before each task. Separate outbox replies; separate commits for 026, 027, 031 only when GREEN. No force push, no main/JInvCommon writes. Existing CTRL-014 generic ChoiceBox null-clear stays BLOCKED and CAL-TIME-001 seconds stays deferred; do NOT modify either. Comments in Russian, avoid additional per-component allocations. Independent Windows user acceptance must not be claimed before reported.

---

## AUTONOMY PROTOCOL — FORE25 CTRL-032…041

**Approved autonomous queue:** CTRL-032 through CTRL-041, in order, without prompting user between successful tasks. This is a bounded permission to perform exactly these tasks, not a blank check to modify arbitrary production code or launch background execution. Run only while local Codex is actively invoked.

**Git boundaries:** `ssu95a/fore25` only. Production writes to `migration/java25-correctness` only. Mailbox reports to `agent/codex-mailbox/docs/codex/outbox.md` only. Never write `main`, `JInvCommon` or any unrelated repository; no force-push, destructive reset, rebase over user commits, schema/DB writes, POM changes, new dependency or public API break. Preserve existing Java 8 business behavior under Java/JavaFX 25. Production comments in Russian. GUI concurrency is not the issue; correctness, data integrity, model events, resource lifetime and **per-control memory** are.

**Each task gate:** fetch current production/mailbox HEAD and prior Reply, verify clean workspace, move by fast-forward only. For every permitted fix: positive baseline, reproducible RED against original implementation, smallest scoped code, GREEN with actual event/cursor/model counts, Maven `-Pbuild compile`, PowerShell parsing, `git diff --check`, focused and nearby regressions. Separate fast-forward commit and Reply per task, verify pushed remote SHA. For READ-ONLY use ignored `target/` probes only; no tracked or production changes. Never invent a GUI/DB result; mark UNKNOWN.

**Blockers and autonomy:** A systemic build failure, branch divergence, unclean worktree, or data-integrity regression blocks **all writes**. If a task has a local architectural ambiguity, report `BLOCKED`, do not patch; **skip only its dependent tasks**, but continue explicitly independent READ-ONLY investigations and independent tests with the verified stable HEAD. CTRL-033 may continue after isolated CTRL-032 BLOCKED only if baseline is green and code scopes are disjoint. Do not use failing expected checks as a reason to weaken tests. CTRL-014 ChoiceBox-null policy and CAL-TIME-001 seconds remain out of scope. No self-assigned production fixes or CTRL-042.

**Final output:** publish individual `## Reply CTRL-032`…`041` for DONE / NO_CHANGE / BLOCKED / WAITING as appropriate, then `## Mission Summary CTRL-032–041` with SHA, tested counts, decisions needed, and three best next engineering priorities. User-independent PASS is UNKNOWN unless reported by user. STOP after mission.

---

## MESSAGE CTRL-032

**Status:** CLOSED — user independently confirmed Windows `CTRL-032 PASS`, exit 0 after test-only synchronization fix CTRL-042 (2026-11-24). Original production fix `cad4dfd39cda4cb1f23bb8610d7ab24c00123bb5` accepted for tested singleton veto/close behavior. Corporate DB, MDI, WAIT UNKNOWN.
**Repo:** ssu95a/fore25
**Branch:** migration/java25-correctness
**Baseline:** 3805ac5ba9096de48fc9697014d1d8063805dd29
**Reply:** CTRL-032

**PROVEN (CTRL-029):** a real FXFormLauncher+annotated SingletonInstance form stays visible after onCancel() veto; launcher then creates a second window. AbstractBaseController.internalOnClose removes g_controllerInstanceMap entry unconditionally in finally even when WINDOW_CLOSE_REQUEST is consumed. The launcher catch also removes by class. This is form uniqueness and potentially task context integrity.
**RED:** new tracked SingletonCloseVetoSmoke.java and `-SingletonCloseVeto` runner. Run real Stage/FXML/launcher; verify duplicate suppressed before veto, veto leaves one Stage/no callback, *next* launch erroneously opens a second; assert callback count, identity, lifecycle. Positive accepted Cancel/closeNow/reopen.
**FIX:** only AbstractBaseController.java, minimal registry lifetime change: don't remove mapping on veto or validation/onOK rejection; remove only matching owner on accepted/finalized close, including SDI WINDOW_HIDDEN and no-Stage branch; identity-safe launcher failure cleanup. Don't leave stale singleton on exception. No new per-controller fields or MDI redesign.
**GREEN:** veto suppresses second open, accepted close eventually permits reopening, cleanup and return callback once; init/validation errors tested if safely constructible; WAIT/MDI unavailable => UNKNOWN. Run -FormCancel, -FormLifecycle, -DataBinding; Maven/diff/PowerShell. Separate verified commit and Reply; if safe error handling cannot be determined, BLOCKED with evidence.

---

## MESSAGE CTRL-033

**Status:** CLOSED — user independently confirmed Windows `CTRL-033 PASS`, exit 0 as part of the four-mode acceptance (2026-11-24). Verified agent production commit `562b9a917a6c708dd6f18cb10112a6aca3d7653f` accepted for scoped validator unbind.
**Repo:** ssu95a/fore25
**Branch:** migration/java25-correctness
**Baseline:** verified latest predecessor production SHA
**Reply:** CTRL-033

**PROVEN (CTRL-030):** same Validator instance bound to controls A and B, unbindValidators(A,v) removes it also from B. Source ValidMan.getValidators(control) list-removal callback does cvList.removeIf comparing only validator, not control. Duplicate additions to SAME control currently validate twice: preserve that separate behavior.
**RED:** new tracked ValidatorScopedUnbindSmoke.java with -ValidatorScopedUnbind. Real controls A/B, shared validator, unbind A then B still validates; reverse ordering; two validators; duplicate registration same control. Count validation and model events. Use public ValidMan methods.
**FIX:** only ValidMan.java getValidators list removal, scope removal to BOTH requested control and validator (preserve existing equality and same-control duplicate semantics); no new listener/state or different focus-manager handling.
**GREEN:** B stays registered after A removed, A remains after B removed; no cross-control collateral; no duplicate model writes introduced. Run -LovIntegration, -LovDefaultValidation, -FormCancel, -DataBinding. Separate commit + Reply. Independent of CTRL-032 if baseline clean.

---

## MESSAGE CTRL-034

**Status:** BLOCKED — Reply CTRL-034 reproduced same/new JInvLOVButton field reattachment with externally owned bindings. Ownership contract unresolved; no implementation authorized in CTRL-043–047
**Repo:** ssu95a/fore25
**Branch:** migration/java25-correctness
**Baseline:** verified latest predecessor production SHA
**Reply:** CTRL-034

**PROVEN (CTRL-028):** real JInvLOVButton.setTextField(F1) then reattach F1/F2 can throw bound-disable exception; after failed new-field attach, inherited metadata and action pair may refer to different fields, retaining previous field. Two existing EXCLUSIVE modes: field-delegated and provider-only. Mixed setTextField()+setLOV() contract UNKNOWN, DO NOT merge.
**RED:** new LovButtonReattachSmoke.java + -LovButtonReattach. Test field-only F1→F1/F2, old field editable/readOnly changes, correct editor target and exactly one accepted model write/TAB, cancel zero; provider-only positive. Count disable/visible bindings, getTextField and inherited metadata. 
**FIX only if safe:** smallest JInvLOVButton.java change, release old disable/visible bindings BEFORE superclass may setDisable, detach field metadata, install exactly one active binding and correct action pair. Avoid altering generic JInvButton shared behavior; if required, BLOCKED requesting architecture decision instead of rewriting. Do not redefine mixed-mode setter semantics or create per-button state unnecessarily.
**GREEN:** repeat and F1→F2 work, no old-field mutation, provider-only untouched, no duplicate actions. Run -LovIntegration, -LovListenerLifecycle, -TextFields, -FormCancel. Separate commit/Reply or BLOCKED.

---

## MESSAGE CTRL-035

**Status:** CLOSED — user independently confirmed Windows `CTRL-035 PASS`, exit 0 after CTRL-042 (2026-11-24). Test-only matrix commit `aba018ab2e14bab5c774668d3ab9b74d32ae7fa1` accepted for tested lifecycle scenarios.
**Repo:** ssu95a/fore25
**Branch:** migration/java25-correctness
**Baseline:** verified latest predecessor production SHA
**Reply:** CTRL-035

New real FXFormLauncher SingletonLifecycleMatrixSmoke.java and -SingletonLifecycleMatrix, or extend 032 probe only if it avoids duplicate fixtures. Verify single visible instance during veto and rejected OK, subsequent accepted Cancel/closeNow allows reopen, callback/cleanup exactly once, owner identity preserved; exercise initialization failure if possible. No production edits, no fake DB/MDI success. Run -SingletonCloseVeto, -FormCancel, -FormLifecycle, Maven/diff. Commit test-only if new invariant and GREEN; if CTRL-032 BLOCKED reply WAITING and skip without stopping independent work.

---

## MESSAGE CTRL-036

**Status:** DONE BY AGENT / USER PASS PENDING — Reply CTRL-036 test-only commit dce04163498221af2f86c8eaec84d77d3974dfc6
**Repo:** ssu95a/fore25
**Branch:** migration/java25-correctness
**Baseline:** verified latest predecessor production SHA
**Reply:** CTRL-036

Tracked ValidatorMultiControlSmoke.java + -ValidatorMultiControl (or extend 033 test if separate source redundant). Shared Validator on A/B with alternating bind/unbind, opposite-order removal, repeated same-control registration, different validator instances, real focus changes and no extra model notifications. Include LOV-validated field from CTRL-026/027 and unbinding without stale effects. No production edits; do not deduplicate the repeated same-control registrations. Run -ValidatorScopedUnbind and -LovIntegration. If CTRL-033 BLOCKED reply WAITING; continue independent research.

---

## MESSAGE CTRL-037

**Status:** CLOSED / READ-ONLY — Reply CTRL-037; reused control focus events remain with first ValidMan, supported cross-controller control reuse UNKNOWN
**Repo:** ssu95a/fore25
**Branch:** migration/java25-correctness
**Baseline:** verified latest predecessor production SHA
**Reply:** CTRL-037

CTRL-030 established first ValidMan captures focus/change listeners, CONTROL_FOCUS_LISTENER marker prevents later manager from installing them; new manager can manually validate but receives no automatic blur events. UNKNOWN whether framework ever reuses one Control across form controller instances. Trace actual control/controller ownership and cleanup. Ignored target probe: same control M1→M2 across scene/form reattachment, record focus validation calls, retained references, marker and model changes. Avoid calling this a production bug without supported caller reuse. Assess memory/per-instance cost of repairs; only Reply with confirmed/source/unknown and a future scoped proposal.

---

## MESSAGE CTRL-038

**Status:** CLOSED / READ-ONLY — Reply CTRL-038; direct lovProperty writes and bound setter exception side effects proven; see narrow CTRL-044
**Repo:** ssu95a/fore25
**Branch:** migration/java25-correctness
**Baseline:** verified latest predecessor production SHA
**Reply:** CTRL-038

CTRL-026 confirmed JInvTextField.lovProperty().set(...) bypasses setLOV listener/button lifecycle. Trace actual callers and FXML/property use, setter vs external binding. Ignored FX25 probe direct set, swap, null, bind/unbind, then setLOV: report model/text, LOV callbacks, button state and exceptions. Do not add automatic property listener to all fields without approved contract, and do not rewrite existing getters. Only Reply with source-confirmed/observed/UNKNOWN and proposed policy.

---

## MESSAGE CTRL-039

**Status:** CLOSED / CONFIRMED DEFECT — Reply CTRL-039; same-turn unbindLOV can reattach stale JInvWrapButton bindings; approved scoped CTRL-043
**Repo:** ssu95a/fore25
**Branch:** migration/java25-correctness
**Baseline:** verified latest predecessor production SHA
**Reply:** CTRL-039

Source JInvTextField.setLOV constructs a new JInvLOVButton each time; its tooltip/disable/visible bound to field; skin may retain former buttons. CTRL-026 fixed provider value listeners, NOT necessarily button retention. With real Stage+skin, perform repeated same-LOV / replacement / unbind, check inner button reference, stable event/model counts, old-button reachability through strongly held bindings. No nondeterministic GC assertions or claims of leak from allocations alone. Track extra memory in controls WITHOUT LOV. Only ignored probes and Reply; recommend next fix if proven.

---

## MESSAGE CTRL-040

**Status:** CLOSED / NO_CHANGE — Reply CTRL-040; table DataSet replacement unsupported, late callbacks after adapter close did not affect it
**Repo:** ssu95a/fore25
**Branch:** migration/java25-correctness
**Baseline:** verified latest predecessor production SHA
**Reply:** CTRL-040

Existing GUI-009 DataBinding covers JInvTable row identity, navigation, refresh and close. Inspect production DSFXAdapter + DSControlAdapter lifecycle to see if replacing dataset on same table is SUPPORTED. If yes, ignored JavaFX25 fixture A→B: late A navigation/row events must not mutate B table; count selection/cursor identity, duplicated handlers and refreshed rows. If rebind is unsupported, do not manufacture a defect: report UNKNOWN/unavailable contract. No changes to JInvCommon or production. Only Reply with actual events/source facts and one bounded recommendation.

---

## MESSAGE CTRL-041

**Status:** DONE BY AGENT / REGRESSION BATTERY — Reply CTRL-041, 15 FX tests PASS at dce04163498221af2f86c8eaec84d77d3974dfc6; no independent acceptance implied
**Repo:** ssu95a/fore25
**Branch:** migration/java25-correctness
**Baseline:** verified latest predecessor production SHA
**Reply:** CTRL-041

Verify predecessor task replies, current remote HEAD and clean worktree. Run fresh Windows Java/FX25 processes where appropriate: -SingletonCloseVeto, -ValidatorScopedUnbind, -LovButtonReattach IF their fixes succeeded, plus existing -FormCancel, -FormLifecycle, -DataBinding, -LovListenerLifecycle, -LovDefaultValidation, -LovIntegration, -SceneKeyLookup, -ChoiceNonNullSet, -BinderChoiceNull, -RadioReadOnly, -ControlsFollowup; Maven -Pbuild compile, diff check. Record each exit code and UNKNOWN for unavailable physical input/DB/forms. Do not add a new test unless an actually uncovered accepted invariant needs it; no production edits. Publish Reply CTRL-041 and '## Mission Summary CTRL-032–041' with SHA matrix, blockers, recommendations. Stop; no CTRL-042.

---

## MESSAGE CTRL-042

**Status:** CLOSED — agent Reply CTRL-042 confirmed JavaFX nested event-loop caused premature test assertion; user independently confirmed both `CTRL-032 PASS` and `CTRL-035 PASS`, exit 0 (2026-11-24). Test-only fix commit `f4f2903181be6b4c2ff0dd19d002a87fe6a99afb` accepted. No production change.
**Priority:** CRITICAL — restore truthful, deterministic singleton regression signal; do NOT weaken production invariants
**Repo:** ssu95a/fore25
**Production branch:** migration/java25-correctness
**Expected baseline HEAD:** dce04163498221af2f86c8eaec84d77d3974dfc6
**Reply:** CTRL-042

### Independent Windows failure (2026-11-24)

User ran `-SingletonCloseVeto` with Windows Java25/FX25. Test printed:
```text
after veto: launched=1 visible=1 callbacks=0
...
java.lang.AssertionError: one callback and cleanup for matching owner 2
  at SingletonCloseVetoSmoke.verifyClosed(...:162)
  at SingletonCloseVetoSmoke.lambda$start$6(...:100)
  ...
  at javafx.stage.Stage.showAndWait(...)
  at ru.inversion.fx.form.controls.progress.ProgressBinder.showStageProcess(...:209)
  at ru.inversion.fx.form.AbstractBaseController.onCloseResources(...:1960)
```
CTRL-032 did NOT PASS independently; do not mark it accepted. The positive main invariant (veto prevented second stage) **did** pass. The failure occurs immediately after second controller `next.closeNow(RET_CANCEL)`, not directly inside singleton registration.

### Source-proven timing hazard; root cause to verify

In `src/test/probes/gui/SingletonCloseVetoSmoke.java`, after `next.closeNow(...)`, the test schedules `Platform.runLater(() -> verifyClosed(next,2))`. Production `AbstractBaseController.close()` itself uses `Platform.runLater` to fire WINDOW_CLOSE_REQUEST. The accepted close hides Stage and `internalActionsOnClose` first emits return callback and then calls `onCloseResources`, which uses `ProgressTaskExecutor` and `ProgressBinder.showStageProcess()->Stage.showAndWait()`. This opens a nested JavaFX event loop that may run the test's next `runLater` BEFORE `closeResources()` and singleton deregistration have completed. **HYPOTHESIS:** premature test verification, not production regression. The stack and source establish the timing path, but the actual failing subcondition among stage showing/callback count/cleaned count/return status is still UNKNOWN.

`LegacyFormCancelSmoke` already implements an independent observer for a progress Stage owned by the closing form and waits for its WINDOW_HIDDEN before checking resource completion and callback count. Use it as an established regression pattern, not as a guarantee.

### Required RED/diagnosis

1. Verify exact HEAD, clean worktree and current mailbox. Reproduce the user failure in repeated FRESH JavaFX processes with existing `-SingletonCloseVeto` and, separately, `-SingletonLifecycleMatrix`. If not reproduced immediately, instrument the original test in ignored `target/ctrl042` copy, not production.
2. Before `verifyClosed(next,2)` record `stage.isShowing`, callbacks count and last result, `next.cleaned.get()`, status/lifecycle, progress Stage showing/hidden, and observed singleton owner using public launcher behavior where feasible. Add observation when cleanup completes and when progress stage hides; confirm whether premature assertion precedes later successful cleanup. An actual permanent cleanup failure or incorrect result is a PRODUCTION BUG and must be reported BLOCKED, not papered over by timing changes.
3. Distinguish test harness race from production: stage hidden, return callback published, resources completed, singleton entry released are separate observable events. Do not treat `Platform.runLater` (one tick) or Stage hidden alone as a completion guarantee when nested modal event loops exist.

### Conditional TEST-ONLY fix

4. If the evidence supports test-race root cause, modify ONLY `src/test/probes/gui/SingletonCloseVetoSmoke.java` (runner only if necessary). Replace the premature `runLater` check after `closeNow` with an event-driven barrier that observes the actual resource-progress Stage owned by `next` and its completion, then verifies cleanup/callback/registration. Ensure final callback executes on FX thread after the relevant completion event; account for nested event-loop unwind. No arbitrary sleeps, indefinite polling/busy loop, swallowed exceptions, disabling checks or direct registry mutation. Preserve exact stage and owner identity, callbacks==2, cleanup==1, RET_CANCEL, no duplicate windows and successful reopen. Also inspect other `verifyClosed` calls and the matrix init-failure path for the same hazard; adjust only when equivalent evidence exists.
5. RED/GREEN under Windows Java 25.0.1 / JavaFX25 on `-SingletonCloseVeto` and `-SingletonLifecycleMatrix`. At least 10 fresh-process successful runs each (or report actual count if limited); also run `-FormCancel`, `-FormLifecycle`, `-ValidatorScopedUnbind`, `-DataBinding`, Maven `-Pbuild compile`, PowerShell syntax and `git diff --check`. Preserve all existing assertions, do not weaken the test to force PASS. Report before/after event counts and any non-determinism.

### Authorized Git and STOP

ONLY test-file modifications permitted to `SingletonCloseVetoSmoke.java` and optionally its existing PowerShell runner; **NO production edits** to AbstractBaseController/ProgressBinder/ProgressTaskExecutor, no main/JInvCommon changes, no new dependencies. If test-race diagnosis and GREEN demonstrated, create an isolated TEST-ONLY commit on migration/java25-correctness, fast-forward push and verify SHA. Publish `## Reply CTRL-042` to docs/codex/outbox.md on agent/codex-mailbox with exact evidence, SHA, commands. If actual production defect or unresolved cause, report BLOCKED with no production commit and STOP. No self-assigned CTRL-043. Independent user rerun required to accept CTRL-032/035 and CTRL-042.

---

## MISSION PROTOCOL FORE25 CTRL-043…047

**Autonomous but bounded:** Execute these FIVE assigned tasks in order after local Codex is launched; no additional user prompt between successful steps. Do not self-assign CTRL-048, modify other code under pretext of cleanup or silently resolve unrelated architectural questions. Read previous Replies CTRL-026/027/031/033/034/038/039/042 first.

**Git boundaries:** only `ssu95a/fore25`; production commits only on `migration/java25-correctness`, reports only to `agent/codex-mailbox/docs/codex/outbox.md`. Never touch `main`, `JInvCommon`, another repo/DB, CTRL-014 ChoiceBox null-clearing or CAL-TIME-001 seconds. No force push, destructive reset, rebase of user work, POM/dependency changes or public API breaking changes. Existing JInvLOVButton binding-ownership issue CTRL-034 stays BLOCKED. GUI interactions on FX thread; no per-instance heap allocation on unused components. Russian source comments.

**For every approved fix:** verify latest remote HEAD/clean tree, exact baseline + positive control, local RED on original code with real JavaFX 25 fixture, isolated minimal production edit, GREEN with callbacks, values, model/selection/visibility and property binding counts, diff check, Maven `-Pbuild compile`, PowerShell syntax and relevant regressions, separate commit/fast-forward push, verify remote SHA, separate `## Reply CTRL-XXX`. Tests user did not run remain UNKNOWN. No monkeypatch/reflection into JavaFX internals to manufacture GREEN.

**Dependencies:** CTRL-043 is high priority. CTRL-044 is independent of 043 if the stable baseline still compiles after a local BLOCKED and worktree remains clean. CTRL-045 depends on successful 043 and 044, otherwise report WAITING. CTRL-046 is READ-ONLY and may proceed independently. CTRL-047 runs only tests of implemented scopes plus accepted previous tests; it reports blocked modes as SKIPPED, never PASS. On systemic build/tree/branch blocker stop all changes. On scoped design ambiguity publish BLOCKED rather than improvising API semantics; continue only independent READ-ONLY scopes. No long background promises without an actually running Codex agent.

**End:** publish Reply for each ID, then `## Mission Summary CTRL-043–047` with SHA/test matrix, independent-user acceptance unknown, unresolved CTRL-034 and future priorities. STOP.


---

## MESSAGE CTRL-043

**Status:** CLOSED — user independently confirmed Windows `CTRL-043 PASS`, exit 0 (2026-11-24); production commit `a62b145212b12455fe8cc3a6fa66c82f3a047be1` accepted. No new per-instance state.
**Repo:** ssu95a/fore25
**Implementation branch:** migration/java25-correctness
**Baseline:** f4f2903181be6b4c2ff0dd19d002a87fe6a99afb
**Report ID:** Reply CTRL-043

**Proven in CTRL-039:** real `JInvTextField` and `JInvTextFieldButtonSkin` with same-turn `setLOV(provider)` then `unbindLOV()`: `JInvWrapButton.setInnerButton(new LOVButton)` schedules `Platform.runLater` to bind wrapper action/text/tooltip/visible/disable to captured button. `unbindLOV` sets innerButton=null, unbinds, hides wrapper, but old scheduled callback can later rebind old button and show stale UI. Source: `JInvWrapButton.java`, lines ~23–49. This is confirmed retention and ghost UI; no claim about GC lifetime.

**RED:** new tracked `WrapButtonDeferredBindingSmoke.java` and `-WrapButtonDeferred` runner. Real attached Stage, JInvTextField skin and wrapper. Test immediate set A→null before FX queue drains (stale callback must NOT reattach), A→B same turn (only B active), A→null→B and A→B→null, and stable A alone positive. Capture `getInnerButton`, property bound/unbound statuses, wrapper visibility, inherited onAction/text/tooltip/disable values, interaction with former button after drain; model writes, events and listeners. Preserve JInvLOVButton-over-JInvFEButton priority, and existing `Controls.CONTROL_PARENT` for active button. No physical Robot.

**Minimal production fix ONLY `src/main/java/ru/inversion/fx/form/controls/JInvWrapButton.java`:** queued callback must verify captured button is still current before writing CONTROL_PARENT or binding any wrapper property; avoid new fields, state, listeners, extra allocations or reworking scheduling. A simple identity guard is a candidate, not an assumption: test overlapping cases first. Do not weaken existing no-inner-button behavior or alter `JInvTextField`, `JInvLOVButton`, `JInvButton`. If apparent failure persists beyond stale callback or guard changes ownership, STOP BLOCKED.

**GREEN:** previously deferred callback no longer resurrects removed button, current button correctly bound once after pulse; zero stale model writes, no NPE. Run `-WrapButtonDeferred`, `-LovListenerLifecycle`, `-LovIntegration`, `-TextFields`, `-FormCancel`, `-ControlsFollowup` and Maven/diff checks. At least 5 fresh-process focused passes. Separate production+test commit, push, Reply SHA/counts/Windows command with `-SettingsFile "D:\Java\.m2\settings.xml"`.


---

## MESSAGE CTRL-044

**Status:** CLOSED — user independently confirmed Windows `CTRL-044 PASS`, exit 0 (2026-11-24); production commit `31de47899b2b22e0dddbe374bfd02fd297236f3c` accepted.
**Repo:** ssu95a/fore25
**Implementation branch:** migration/java25-correctness
**Baseline:** verified latest predecessor production HEAD
**Report ID:** Reply CTRL-044

**Proven in CTRL-038:** after normal `JInvTextField.setLOV(old)`, external `lovProperty().bind(source)` to current old provider, calling `setLOV(new)` attempts to detach active value listener BEFORE `this.lov.set(new)` throws for bound property. Same risk in `unbindLOV()`: detaches listener before `lov.set(null)`. If caller catches exception, old LOV remains current but stopped updating field/model. Direct `lovProperty().set` also bypasses setup by legacy design; this separate contract remains OUT OF SCOPE.

**RED:** new tracked `BoundLovSetterAtomicitySmoke.java`, runner `-BoundLovSetter`. Actual attached JInvTextField and AbstractLovBase providers. Positive: unbound setter replacement/unbind works; bind lovProperty to currently active provider; attempts to `setLOV(other)` and `unbindLOV()` must fail with existing bound-property exception while preserving initial field/model value, still-active listener, old provider, button identity and validation flag. Prove baseline old listener loss after failed call by mutating active provider. Count model and provider events, no duplicate registration. Test unbind property and subsequent valid setter recovery, replacement to new provider, repeat same provider and no-LOV memory sentinel.

**Scope:** ONLY `JInvTextField.java` inside `setLOV(AbstractLovBase,boolean)` and `unbindLOV`, plus a new focused smoke and runner. Arrange operations to preserve *failed setter* state without altering JavaFX's existing bound-property prohibition or its exception type. Prefer safe mutation ordering or a proven preflight; no automatic listener on writable `lovProperty`, no new per-control state, no change to public signatures, validation, append, button creation or provider ownership. Beware reentrant property observers. If preserving complete failed-call behavior requires defining externally bound property lifecycle, report BLOCKED rather than changing API.

**GREEN:** bound mutation attempts throw but leave active old provider updates functional and UI intact; unbound normal path unchanged; no stale events. Re-run `-LovListenerLifecycle`, `-LovDefaultValidation`, `-LovIntegration`, `-WrapButtonDeferred` if 043 GREEN, `-DataBinding`, `-FormCancel`; Maven/diff/PowerShell. Commit/push only if proven, Reply separate SHA and counts.


---

## MESSAGE CTRL-045

**Status:** CLOSED — user independently confirmed Windows `CTRL-045 PASS`, exit 0 (2026-11-24); test-only commit `4de3d8a680bb2820be733d89c8cee0dfe3c1e80f` accepted.
**Repo:** ssu95a/fore25
**Implementation branch:** migration/java25-correctness
**Baseline:** verified latest predecessor production HEAD
**Report ID:** Reply CTRL-045

After confirming CTRL-043/044 DONE and their exact remote SHAs, add ONE test-only `LovBindingInterleavingSmoke.java` and `-LovBindingInterleaving` runner. Exercise real JavaFX25 Stage + JInvTextFieldButtonSkin with these supported sequences: set A→unbind before pulse, set A→B→unbind before pulse, set A→unbind→B, same provider repeated, external lovProperty bound to active A then rejected set B/unbind and recovery after property unbind, disabled/readOnly control, equal values and old provider changes. Verify wrapper inner button identity, tooltip/onAction/visible/disable binding, model once per accepted LOV value and zero after detach, repeated calls zero additional events, exceptions only for existing bound-property prohibition. Ensure no NPE and no strong path from wrapper to obsolete button via active property binding.

Also preserve `JInvLOVButton` field vs provider exclusive-mode behavior. **Do NOT use field reattachment F1→F2 as a passing expectation**: CTRL-034 is BLOCKED and not patched. Do not force asynchronous tests with arbitrary sleep; observe settled FX queue or real events. Add only test source+runner, no production. Run prior two focused modes, LovIntegration, TextFields, ControlsFollowup. Fast-forward test-only commit and Reply. If 043/044 BLOCKED, Reply WAITING, no fabricated PASS.


---

## MESSAGE CTRL-046

**Status:** CLOSED / READ-ONLY — Reply CTRL-046 found concurrent external + inner LOV visibility in real AddLinkPane order; skin lifecycle follow-up CTRL-048; button precedence contract remains undecided.
**Repo:** ssu95a/fore25
**Implementation branch:** migration/java25-correctness
**Baseline:** verified latest predecessor production HEAD
**Report ID:** Reply CTRL-046

Investigate a NEW distinct question from blocked CTRL-034. `JInvTextField` listens to `externalButtonProperty` and removes inner button when non-null; `setLOV` can subsequently create/install a new inner JInvLOVButton even while externalButton remains assigned. `JInvTextFieldButtonSkin` owns JInvWrapButton and overflow button. In business forms both FXML-internal and external buttons may be present. **UNKNOWN** whether order of operations can display wrong button or duplicate actions.

Trace real production call sites/FXML and `JInvButton.setTextField`, `JInvTextField.externalButton`, skin/wrapper behavior. Use ignored `target/ctrl046` JavaFX25 fixture with actual Stage and focus, controlled external JInvButton and minimal LOV provider. Try external→setLOV and setLOV→external, repeated attach/detach, Skin creation/replacement if possible. Observe wrapper visible/managed state, externalButton references, F9 or button action routing, field/model writes, old button binding retention. Distinguish SUPPORTED contract from questionable combinations; don't invent an external button that cannot be wired in real forms.

No production or tracked changes; report CONFIRMED, SOURCE-PROVEN, HYPOTHESIS, UNKNOWN and safest proposed scope if real defect found. Do not resolve CTRL-034 binding ownership, do not change button modes. Can proceed even if 043/044 local BLOCKED if baseline clean.


---

## MESSAGE CTRL-047

**Status:** CLOSED / READ-ONLY — Reply CTRL-047 final battery 17/17 PASS on agent Windows; CTRL-034 intentionally skipped. Independent acceptance for CTRL-033/043/044/045 subsequently supplied by user.
**Repo:** ssu95a/fore25
**Implementation branch:** migration/java25-correctness
**Baseline:** verified latest predecessor production HEAD
**Report ID:** Reply CTRL-047

Check all task Replies and remote HEAD. Run Maven `-Pbuild compile`, diff check and relevant JavaFX25 Windows smoke modes in fresh processes: `-WrapButtonDeferred` and `-BoundLovSetter` only if implemented, `-LovBindingInterleaving` if 045 DONE; plus existing `-LovListenerLifecycle`, `-LovDefaultValidation`, `-LovIntegration`, `-TextFields`, `-FormCancel`, `-FormLifecycle`, `-SingletonCloseVeto`, `-SingletonLifecycleMatrix`, `-ValidatorScopedUnbind`, `-ValidatorMultiControl`, `-DataBinding`, `-SceneKeyLookup`, `-ControlsFollowup`, and `-RadioReadOnly`. Record explicit exit codes, skipped modes, event counts and flaky outcomes. Do not weaken tests or conceal failures. If regressions fail, report BLOCKED with exact reproduction and stop production writes.

Output ONLY a detailed `## Reply CTRL-047` and `## Mission Summary CTRL-043–047` on mailbox branch (no production changes); add a tracked TEST-ONLY assertion only if a proven accepted invariant lacks coverage and such a change is explicitly within the test-only scope. Summarize no-database/physical-input/MDI UNKNOWN, CTRL-034 remains BLOCKED, CTRL-033 independent user PASS still pending. Recommend three highest-value next tasks. STOP after CTRL-047, no autonomous CTRL-048.

---

## MESSAGE CTRL-048

**Status:** ASSIGNED — timebox 60 minutes, READ-ONLY JavaFX 25 JInvTextFieldButtonSkin disposal/retained-listener forensic audit
**Priority:** HIGH — memory retention and stale UI callbacks after skin replacement
**Repo:** ssu95a/fore25
**Branch:** migration/java25-correctness (production READ-ONLY)
**Baseline remote HEAD:** 4de3d8a680bb2820be733d89c8cee0dfe3c1e80f
**Reply ID:** CTRL-048
**Expected effort:** approximately 60 minutes; stop at timebox with the best grounded findings and UNKNOWNs, do not extend indefinitely

### Evidence and questions

CTRL-046 observed an older JInvTextFieldButtonSkin/wrapper still reacting after skin replacement and noted user-visible external vs inner LOV-button conflicts. Current production `src/main/java/ru/inversion/fx/form/controls/skin/JInvTextFieldButtonSkin.java` constructor registers DIRECT callbacks on:
- `JInvTextField.externalButtonProperty()` with lambda calling `checkOverflow()`;
- `textField.focusedProperty()` capturing `wrapButton` and pseudo-class logic;
- `textField.textProperty()` and `widthProperty()` via `initOverflowListener()`, each calling `checkOverflow()`;
- `wrapButton.visibleProperty()` -> `textField.requestLayout()`.
Skin also has registerChangeListener(...) bindings whose cleanup JavaFX superclass likely owns. `dispose()` hides/clears context menu and calls super.dispose(), but does not explicitly detach above direct listeners. This establishes a potential strong reference path from live control properties to old skin, but actual disposal semantics, recurrence and affected GUI output must be experimentally verified. Do not label every allocation a leak.

### READ-ONLY investigation

1. Verify HEAD, mailbox and clean tree; inspect constructor, `dispose()`, `JInvTextField.setSkin`, `JInvWrapButton.setInnerButton`, externalButton, JInvFEButton/LOV, and skin lifecycle call sites. Establish whether skin replacement is a supported/reachable case in current forms. If production only creates one skin per field, characterize failure impact accordingly.
2. In ignored `target/ctrl048` JavaFX25 fixture with actual shown Stage and real JInvTextField, establish positive baseline with one skin, LOV provider and (separately) external button. Replace skin using the supported JavaFX setSkin API with a new JInvTextFieldButtonSkin, confirm former skin dispose invoked and new skin active; pulse correctly (no sleeps or fabricated focus). Exercise focus transitions, text edits, width/layout change, externalButton toggles, ordinary LOV changes and same-turn unbind. Record old vs new wrapper text/visibility/pseudo-class/tooltip/handlers and whether old skin methods still run. Use controlled diagnostic subclass or temporary reflection only in ignored diagnostics, never reflect into JavaFX internals for an official test.
3. Distinguish direct unmanaged listeners from registerChangeListener managed by SkinBase. Establish a grounded strong-reference ownership path or retained callbacks, but do NOT rely on nondeterministic GC as the sole correctness test. If actual skin disposal already detaches a callback via a supported mechanism, record that fact rather than assuming.
4. Determine whether stale skin can affect business value/model or only stale UI/resources; count model notifications and action routing, including old JInvWrapButton retained bindings. Test field without LOV as a low-memory sentinel. Check whether old skin can retain an old inner button after new skin becomes active; new wrapper may start empty.
5. Provide one *minimal future fix* if confirmed: explicit listener ownership/removal on dispose, possibly detach old wrap bindings, using lazy handles and no extra per-JInvTextField instance memory. Do NOT change external-vs-inner button precedence contract or CTRL-034 ownership. Distinguish correctness issue from memory-only issue. If impact is limited to unsupported skin replacement, recommend NO CHANGE.
6. Keep investigation strictly within one-hour timebox. Publish `## Reply CTRL-048` in `docs/codex/outbox.md` on mailbox branch with SOURCE-PROVEN, CONFIRMED runtime, HYPOTHESIS, UNKNOWN, concrete code paths, event counts, whether reproduction is deterministic, actual HEAD, and one narrow next task or NO_CHANGE. No commit, no production changes, no tracked test changes. No self-assigned CTRL-049.

### Global boundaries
GitHub writes allowed only to mailbox outbox for the reply. Do not write production, `main`, `JInvCommon`, CTRL-014, CTRL-034 or CAL-TIME-001. Do not introduce eager listener/state allocation on all fields. If a branch conflict, dirty worktree, broken baseline, or required environment prevents test, report BLOCKED with evidence, not invented PASS. The architect will review the report and conditionally assign the next task.

---

## MESSAGE CTRL-049

**Status:** DONE / ARCHITECT APPROVED / USER ACCEPTED — independent Windows `CTRL-049 PASS`, exit code `0`; production `582a7899172c3beef845dbb2393bfc3e5d02e75f`.
**Repo:** ssu95a/fore25
**Production branch:** migration/java25-correctness
**Baseline remote HEAD:** 4de3d8a680bb2820be733d89c8cee0dfe3c1e80f
**Reply ID:** CTRL-049
**Dependency:** architect-reviewed DONE/READ-ONLY Reply CTRL-048 (mailbox HEAD 4d8ad74d1758444469e3e499fe047f34c405cf54).

### Confirmed facts and unknowns
CTRL-048 used real Windows Java 25 / JavaFX 25 Stage, public setSkin(null) followed by replacement, and one exploratory plus three fresh JVM runs. Direct field text, width and externalButton changes triggered overflow calculation twice instead of once after replacement; former dispose was called once, getSkinnable() null, yet former overflow visibility and focus pseudo-class changed. The managed font callback stayed at one. No-LOV field also reproduced doubled text callbacks. Source path: live field property → direct listener → former skin → former wrapper/button. The old wrapper retained its inner button and bound properties. Normal LOV/provider/F9 model writes remained exactly one per accepted action/change and zero after unbind. No duplicate business writes demonstrated. Routine in-repo skin replacement use, actual retained bytes, GC lifetime, corporate DB and physical input remain UNKNOWN. This is a confirmed public lifecycle correctness/ownership issue, not proof of an application-wide leak.

### Scope and implementation
1. Verify current remote production HEAD and mailbox, clean tree, read CTRL-048 and CTRL-046 fully. If unexpected advancement/conflict or dirty tree, STOP BLOCKED without writes. Recover the ignored CTRL-048 probe if available.
2. Add tracked `src/test/probes/gui/TextFieldSkinDisposalSmoke.java` and official `-TextFieldSkinDisposal` mode in `src/test/probes/Test-LegacyGuiSmoke.ps1`. RED on unchanged production before edit. Real shown Stage, actual JInvTextField and skin, supported replacement using setSkin(null) then new skin (also direct replacement if supported by JavaFX). Distinguish old/new callbacks using observable public effects or instrumented test subclasses. Record initial positive controls and deterministic settled FX transitions without arbitrary sleeps or reflection into JavaFX internals. For isolated text, width, externalButton mutations verify count two before fix, one afterward, with managed font as control; old focus pseudo-class/overflow state must stop responding while new skin continues. Exercise multiple replacements (at least three), no-LOV control, active LOV and same-turn pending wrapper attachment during disposal. Capture old wrapper bindings and handler/tooltip identity where relevant; assert former skin no longer participates while current field still works. Explicitly verify actual disposals, current skin identity, current focus behavior, model and provider notification counts. Do not manufacture GC assertions; show ownership edges removed by code and deterministic callbacks.
3. **Only permitted production file:** `src/main/java/ru/inversion/fx/form/controls/skin/JInvTextFieldButtonSkin.java`. Make smallest correction to register/unregister skin-owned listeners with lifetime of that skin: externalButton, focus, text, width and wrap visibility. Prefer existing supported SkinBase managed registration (already used for font) when equivalent callback timing/semantics are demonstrated. Otherwise retain exact listener handles on the skin and remove them during dispose; avoid additional allocations/state on JInvTextField, avoid duplicate listener objects/registrations, no eager per-field helper/collection. A disposed guard alone is not a root-cause fix for retained subscriptions. Preserve normal focus pseudo-class and overflow semantics and idempotent dispose; do not alter user-owned field callbacks/bindings or context-menu ownership. Do not detach active LOV provider or change field value. If old skin-owned wrapper bindings/queued attach survive, detach only the disposed skin's owned wrapper using existing APIs (CTRL-043 guard should prevent pending rebind); clear old cached action only if proven skin-owned and safe. If ownership or restoring a new skin's LOV button requires an unapproved contract, leave that portion READ-ONLY and report exactly what remains UNKNOWN; if the core correction itself cannot be isolated, STOP BLOCKED with no production changes. No attempt to restore LOV button in replacement skin, change external/inner priority or rework JInvWrapButton/JInvLOVButton.
4. GREEN: previous RED, old callbacks gone, current callbacks exactly one, old pseudo-class/overflow frozen after disposal, no accumulating callbacks after repeated replacement. If wrapper cleanup included, verify obsolete bindings detached and same-turn queued callbacks cannot resurrect wrapper; old UI cannot route an obsolete owned action. Provider/model writes exactly once when valid, zero after unbind; unchanged normal current-skin behavior including focus, font, context-menu and external-button paths; no LOV listener allocated for unused field. Explicitly account for memory/handle allocations in old/new implementation; no new per-JInvTextField fields or allocations. Test positive controls and preserve legitimate caller callbacks.
5. Run focused official mode and at least 3 fresh JVM passes, then existing -TextFields, -ControlsFollowup, -WrapButtonDeferred, -BoundLovSetter, -LovBindingInterleaving, -LovListenerLifecycle, -LovDefaultValidation, -LovIntegration, -FormCancel, -FormLifecycle, Maven -Pbuild compile, PowerShell parse and diff check. If regression fails, STOP BLOCKED; do not weaken assertions or hide flaky outcomes. Within timebox, publish only a validated isolated fix+test commit (fast-forward push, verify remote SHA). If timebox expires before reliable GREEN, no incomplete production push; report PARTIAL/BLOCKED with exact remaining obstacle and clean baseline. No claims of independent user acceptance.
6. Publish exactly one `## Reply CTRL-049` in docs/codex/outbox.md on mailbox branch: DONE/NO_CHANGE/BLOCKED with RED/GREEN evidence, old/new callback counts, model counts, ownership review, precise allowed-file diff, allocation comparison, production SHA, Windows command with -SettingsFile "D:\Java\.m2\settings.xml", results/UNKNOWN and any residual wrapper/skin-contract question. Stop after 60-minute bounded effort, no self-assigned CTRL-050.

### Boundaries
Only repo ssu95a/fore25. Production writes only on migration/java25-correctness, mailbox reply only agent/codex-mailbox. No main, JInvCommon, CTRL-014, CTRL-034 or CAL-TIME-001 changes, no unrelated files, dependency/API changes, destructive Git commands or force push. Russian source comments. External-vs-inner LOV precedence and direct writable lovProperty lifecycle remain outside scope. Do not treat absent duplicate model writes as proof of absent UI lifecycle defect. Do not expand the scope without architect approval.

---

## MESSAGE CTRL-050

**Status:** DONE / ARCHITECT APPROVED / USER ACCEPTED — independent Windows `CTRL-050 PASS` reported by user; numeric exit code not separately reported. Production `6d36746616ce26fee2e42acefce57571131ffb6e`.
**Repo:** ssu95a/fore25
**Production branch:** migration/java25-correctness
**Baseline remote HEAD:** 582a7899172c3beef845dbb2393bfc3e5d02e75f
**Reply ID:** CTRL-050
**Dependency:** CTRL-049 DONE, architect diff review approved; independent user Windows acceptance pending, not a prerequisite for this isolated task.

### Evidence
FORE25-TRIAGE priority 1 reproduced former provider overwriting draft with current provider value, three attachments adding three buttons, two NPEs on one former-provider mutation after detach. Architect verified source: JInvTextArea.setLOV sets property then adds untracked invalidation listener and new toolbar button each non-null call; listener reads current getLOV rather than event source. Null returns without cleanup. Recover ignored target/fore25-triage probe if available. Actual corporate prevalence and heap bytes UNKNOWN.

### Scope
1. Verify clean checkout and both remote refs before changes. Unexpected production advancement or dirty tree: STOP BLOCKED. Read JInvTextArea, JInvLOVButton and relevant JInvTextField lifecycle precedent (CTRL-044/045). Add tracked src/test/probes/gui/TextAreaLovLifecycleSmoke.java and official -TextAreaLovLifecycle runner mode. RED on unchanged production. Shown Stage, real control, distinguishable providers A/B, exact callback and model counts, toolbar identity/size, deterministic FX turns, capture uncaught FX exceptions. Matrix null→A→A→B→null→A and repeated null; former A mutation while B active must preserve distinct draft and model; after detach mutate A and B independently. Include same-turn attach/detach and settled path, legitimate toolbar action and synthetic F9, ordinary current-provider update, cancellation and no-LOV control. Preserve caller-added toolbar item and existing external-editor button identity/order. Ensure RED is defect assertion not setup failure.
2. Only permitted production file src/main/java/ru/inversion/fx/form/controls/JInvTextArea.java. Remove exact owned subscription from previous provider and avoid duplicates on repeat. Track/remove only internally created LOV button; preserve unrelated toolbar items, title initialization, accepted-choice semantics, onSetLov contract and readOnly behavior. Inspect existing button detach API before using; no changes to JInvLOVButton/JInvWrapButton or skin. No eager per-control listener/helper/collection allocation; prefer lazy reusable handle and minimal nullable state, explain footprint. A null guard or source guard alone is not sufficient to remove stale subscription. Preserve public writable lovProperty contract (direct writes out of scope). If bound lov property rejects setter, do not dismantle working old attachment before successful property update; test failure atomicity if supported by existing setter path. Do not change validateFromLOV ignored semantics, append semantics, provider callback contract or keyboard navigation. If delayed choice completion from a retired provider or button detach ownership cannot be isolated, report separately with evidence rather than redesign shared APIs. No skin replacement work.
3. GREEN same probe, former providers zero text/model writes and zero owned callback invocations, no NPE, exactly one owned toolbar LOV button while attached and zero on detach, unrelated items intact. Valid choice/ordinary update preserve baseline exact model notifications (also inspect duplicate actual setter callbacks; do not equate equal-value property suppression with absence of callbacks). Positive controls; no weakening assertions. Verify no LOV listener/button allocations on unused control.
4. Official focused mode plus 3 fresh JVM runs; relevant existing -TextFields, -ControlsFollowup, -LovListenerLifecycle, -LovDefaultValidation, -LovIntegration, -BoundLovSetter, -LovBindingInterleaving, -TextFieldSkinDisposal and applicable TextArea modes identified from runner. Maven -Pbuild compile, PowerShell parse, diff check. Report exact commands, exit codes, RED/GREEN counts, allowed-file diff, allocation delta and UNKNOWN. If reliable GREEN not achieved within timebox or unexpected regression, STOP PARTIAL/BLOCKED, no incomplete production push. Otherwise isolated validated commit and fast-forward push, verify remote SHA.
5. Publish exactly one Reply CTRL-050 in docs/codex/outbox.md on mailbox branch, include independent Windows rerun command with -SettingsFile "D:\Java\.m2\settings.xml". No claim of user acceptance. Stop; no self-assignment of CTRL-051.

### Boundaries
Only ssu95a/fore25, production migration/java25-correctness, mailbox agent/codex-mailbox. main and JInvCommon read-only. No destructive commands, force push, dependencies/public API changes, CTRL-014/034/CAL-TIME-001, skin disposal, generic LOV redesign or unrelated cleanup. Russian Java comments. Timebox 60 minutes.

---

## MISSION CTRL-051…CTRL-060 — architect-authorized 8-hour sequential queue

**Status:** BACKLOG / AUTONOMOUS MISSION CANCELLED. No task in CTRL-051…060 is authorized to start without a separate explicit architect assignment.
**Repo:** ssu95a/fore25
**Production branch:** migration/java25-correctness
**Mailbox:** agent/codex-mailbox
**Budget:** 480 minutes total, additional to CTRL-050. Ten ordered tasks, no parallel work.
**Baseline:** actual verified clean remote production HEAD after CTRL-050; 582a7899172c3beef845dbb2393bfc3e5d02e75f is the architect's pre-050 observation, not permission to reset. Record exact baseline for every task.

### Execution contract (applies to every task)
The user revoked autonomous execution. These ten task descriptions are backlog only. Execute exactly one separately assigned task, publish its reply, and STOP. Await architect review and independent user acceptance before the architect assigns the next task. If an earlier instruction conflicts with this rule, this rule takes precedence. If already executing a mission task, do not start another; report current state and stop safely. Verify remote refs and clean checkout; fetch current mailbox before every reply and production HEAD before every task. Sequential only. No production writes on unexpected remote advancement, dirty tree, scope conflict or regression. Fast-forward only; no force push/destructive commands. Main and JInvCommon read-only. No CTRL-014, CTRL-034, CAL-TIME-001, dependency or public API changes. New Java comments in Russian.

For FIX/AUDIT→FIX: read actual source at task baseline, recover ignored triage probes if available, add one tracked focused smoke test and runner mode where justified. Deterministic RED before production edit and GREEN on same assertions, positive controls, actual callback/model counts and exceptions, official focused mode plus 3 fresh JVMs where feasible within timebox, relevant existing regressions, Maven compile, PowerShell parse, diff check. No arbitrary sleeps or JavaFX internal reflection. Do not weaken assertions. One isolated validated commit per completed fix, fast-forward publish and verify remote SHA. If unable to finish safely, no incomplete push; report PARTIAL/BLOCKED with exact obstacle. Keep test changes isolated. No new eager per-control helper/collection/listener allocations; account for instance footprint. Only named production class allowed per task. AUDIT and VERIFY are read-only in production; temporary ignored probes allowed, evidence in outbox. No production fix for unknown semantic contract. Report exact baseline/final SHA, commands/exits, RED/GREEN evidence, scope, unknowns and Windows rerun with -SettingsFile "D:\Java\.m2\settings.xml". Publish exactly one Reply CTRL-0XX per task. No claims of user acceptance.

If a task is BLOCKED by an unresolved contract, report and STOP after restoring/verifying clean baseline without destructive operations. If tree cannot be made clean safely, remote conflict, failed shared regression, or infrastructure prevents reliable validation, stop mission and report remaining NOT_RUN. Do not let timeboxes expand or use freed time for unscheduled work; total maximum 480 min, reserve CTRL-060 budget where possible. No new tasks after CTRL-060. 

---

## MESSAGE CTRL-051

**Status:** DONE / ARCHITECT APPROVED / USER ACCEPTED — independent Windows `CTRL-051 PASS` reported by user; numeric exit code not separately reported. Production `171cb8c6347cae958907debcf0774ba6d3b85d11`. Keyboard detach scope only; mouse filters and pending paging remain outside scope.
**Repo:** ssu95a/fore25
**Production branch:** migration/java25-correctness
**Baseline remote HEAD:** 6d36746616ce26fee2e42acefce57571131ffb6e
**Dependency:** CTRL-050 architect approved and user PASS confirmed.
**Execution:** Explicit architect assignment of CTRL-051 only. Follow shared per-task execution contract above; after publishing Reply CTRL-051 STOP, even if time remains. Do not start any other task in the CTRL-051 session. CTRL-052…055 have a separately authorized conditional batch below; CTRL-056…060 remain BACKLOG / NOT ASSIGNED.
**Timebox:** 60 minutes
**Reply ID:** CTRL-051
**Task:** FIX — DSFXAdapter.close / refreshCurrentRow

FORE25-TRIAGE priority 2: reproduced KEY_RELEASED after close on still shown table reaching obsolete handler and NPE. Recover ignored probe if available. RED on actual shown table and adapter: demonstrate legitimate pre-close key behavior, close, late key event, late dataset events, repeated close. Count owned handler executions and refreshes, capture FX uncaught exceptions. Fix only DSFXAdapter: unregister exact owned keyboard handler(s) on close; keep caller-owned handlers, existing dataset unsubscribe and close semantics, do not rely on null guard alone. No same-table rebind contract changes. GREEN: obsolete handler never invoked, no NPE/refresh after close, legitimate pre-close and unrelated input preserved; run previous adapter close/late-event probes and new focused mode. If required handler ownership is unclear, STOP this task BLOCKED.

---

## BATCH CTRL-052…CTRL-055 — four-task sequential assignment

**Status:** DONE / ARCHITECT APPROVED / USER ACCEPTED — all four tasks accepted by user after independent batch verification; individual numeric exit codes not separately supplied. Final production HEAD `ed5533d8d03c1ca1500b65660060d47ce336e3d3`. Batch closed; CTRL-056…060 remain backlog.
**Repo:** ssu95a/fore25
**Production branch:** migration/java25-correctness
**Mailbox:** agent/codex-mailbox
**Budget:** 195 minutes total (45 + 60 + 45 + 45), additional to standalone CTRL-051.
**Baseline remote HEAD:** 171cb8c6347cae958907debcf0774ba6d3b85d11. Verify clean checkout and remote ref at launch; no reset. Subsequent tasks start from verified HEAD of preceding completed task.

### Latest authorization — overrides earlier manual-only rule for these four tasks only
The user explicitly requested a new batch of four tasks. CTRL-051 remains a standalone assignment; finish it, publish Reply CTRL-051, and STOP that session. CTRL-051 has published DONE and architect diff review is approved. The user requested batch execution now; this release supersedes the former conditional start. Start with CTRL-052 in a new local session. Do not repeat CTRL-051. Independent Windows acceptance of CTRL-051 is pending but not a prerequisite for this isolated batch. Stop on unexpected remote state.

On batch launch, execute CTRL-052, CTRL-053, CTRL-054, CTRL-055 in that exact order, sequentially, with one isolated validated fast-forward commit per fix, one Reply per task and a verified clean remote baseline before each. No need for intermediate user acceptance or architect approval for fixes explicitly within the individual scopes; user acceptance remains separate and pending. This is not permission to change an ambiguous contract. On a task-specific unresolved semantic question, publish READ-ONLY/BLOCKED or PARTIAL with evidence; proceed only if the next task is independent, no regression is failing and the working baseline is clean. Stop the batch on dirty/conflicting state, failed regression or unreliable infrastructure; report remaining NOT_RUN. After CTRL-055 STOP. CTRL-056…060 remain BACKLOG / NOT ASSIGNED. No other tasks, no autonomous eight-hour mission.

All earlier per-task RED/GREEN, ownership, allocation, API, Git and testing constraints remain. No eager per-control objects. For CTRL-053, preserve accepted CTRL-050 LOV attachment behavior; neither restoration of LOV toolbar on replacement skin nor retired provider delayed completion is authorized. For CTRL-055, account for CTRL-049's existing skin-specific cleanup and CTRL-050's button association; no common LOV redesign. Report these separately if relevant. If no deterministic RED, do not edit production.

---

## MESSAGE CTRL-052

**Status:** DONE / ARCHITECT APPROVED / USER ACCEPTED — user confirmed all four independent checks OK. Production `e708fdda5d8669e52a52e628e332216ef737c6fc`.
**Timebox:** 45 minutes
**Reply ID:** CTRL-052
**Task:** FIX — ValidMan multi-subchange removals

FORE25-TRIAGE priority 3 reproduced removeAll(v1,v3) leaving v3 active though list contains only v2. RED real control and three distinguishable validators; assert list, exact validation calls and automatic blur if applicable; disjoint removals in one change, contiguous removal, additions/replacement, clear, duplicate instances and shared Validator across controls. Fix only ValidMan list-change processing (all change.next subchanges, preserve relevant event ordering), no redesign. GREEN list and actual registrations agree; CTRL-033 per-control unbind and established duplicate semantics preserved. If a test reveals ambiguous duplicate behavior, record baseline and do not change it.

---

## MESSAGE CTRL-053

**Status:** DONE / ARCHITECT APPROVED / USER ACCEPTED — user confirmed all four independent checks OK. Production `2d18941e3fd7911f3df27c65ec26061062e3c6d4`.
**Timebox:** 60 minutes
**Reply ID:** CTRL-053
**Task:** AUDIT → FIX — JInvTextAreaSkin.dispose

Current source inspected by architect at 582a7899: direct scene and editable listeners capture skin; scene listener installs EDIT_EXTERNAL action that dereferences getSkinnable. Toolbar visible/FE visible bindings also exist. Runtime defect not yet proven. After CTRL-050, test no-LOV shown control with public unset/replacement, dispose count, focus/scene reattachment/editable changes, old/new toolbar effects and keyboard routing using safe stub action; no real external editor. Include replacement before first scene and after first scene, repeated replacements, caller context menu/actions. Trace ownership of registrations/bindings. Only if deterministic RED and existing action ownership can be preserved, fix JInvTextAreaSkin alone using managed registrations or exact cleanup; no CTRL-050 LOV restoration, no changes to keyboard manager/other controls, no eager per-field objects. If shortcut ownership ambiguous, isolate proven listener cleanup only or report BLOCKED/AUDIT. GREEN normal toolbar and external-editor action path, old callbacks stop, no duplicate current actions; run CTRL-050 and relevant TextArea/menu tests.

---

## MESSAGE CTRL-054

**Status:** DONE / ARCHITECT APPROVED / USER ACCEPTED — user confirmed all four independent checks OK. Production `5bc3c8bd02a22cb13cd5a7d45b941b86d29061e5`.
**Timebox:** 45 minutes
**Reply ID:** CTRL-054
**Task:** AUDIT → FIX — JInvTabPaneSkin listener accumulation

Architect inspected current source: sideProperty listener adds another tabs-list listener each transition to LEFT, plus direct prefWidth/side listeners. Prove actual repeated processing: shown Stage, TOP→LEFT→TOP→LEFT cycles at least three, then add/remove tab; count effects with observable instrumentation, compare first LEFT and non-LEFT behavior, test initially LEFT and normal rendering/selection. No claims based only on listener count inferred from source. Fix only JInvTabPaneSkin if clear RED and contract: one lifetime-managed tab-list registration or exact attach/detach, correct all subchanges; preserve existing non-LEFT behavior unless explicitly proven defect, no new tab state or public API. If time permits demonstrate disposal; avoid unrelated skin redesign. GREEN same probe and existing tab/layout tests.

---

## MESSAGE CTRL-055

**Status:** DONE / ARCHITECT APPROVED / USER ACCEPTED — user confirmed all four independent checks OK. Production `ed5533d8d03c1ca1500b65660060d47ce336e3d3`.
**Timebox:** 45 minutes
**Reply ID:** CTRL-055
**Task:** AUDIT → FIX — JInvWrapButton retired onAction ownership

CTRL-046 reproduced cached action after settled detach; CTRL-043 fixed deferred bind race; CTRL-049 clears matching bound action in retired TextField skin, not generic wrapper. Inspect existing setInnerButton and CONTROL_PARENT ownership. RED only if detached/replaced wrapper can route obsolete owned action or strong ownership edge is proven and safe contract established. Exercise A→null, A→B, A→B→A same-turn/settled, caller-defined unbound action, LOV-vs-FE priority, old button CONTROL_PARENT, no firing after detach; positive current button. If safe narrow fix, only JInvWrapButton; remove owned cached action/parent edge only if identity/ownership demonstrated, preserve caller-owned action and CTRL-043 behavior; do not change JInvLOVButton or CTRL-034 reattachment contract. Otherwise READ-ONLY with exact residual question. Run WrapButtonDeferred, BoundLovSetter, LovBindingInterleaving, TextFieldSkinDisposal and CTRL-050 if available.

---

## BATCH CTRL-056…CTRL-060 — five-task sequential audit and verification

**Status:** DONE / ARCHITECT REVIEWED — five read-only replies received, 23/23 agent regression modes exit 0, production unchanged. No independent Windows rerun claimed.
**Repo:** ssu95a/fore25
**Production branch:** migration/java25-correctness
**Mailbox:** agent/codex-mailbox
**Baseline remote HEAD:** ed5533d8d03c1ca1500b65660060d47ce336e3d3
**Budget:** 225 minutes total, 45 minutes per task.
**Dependency:** CTRL-052…055 DONE / ARCHITECT APPROVED / USER ACCEPTED.

### Latest authorization
The user approved this five-task batch. This overrides older manual-only and cancelled mission restrictions ONLY for CTRL-056…060. Execute in order, without waiting for intermediate architect/user confirmations. Do not repeat CTRL-051…055. All five tasks are strictly READ-ONLY for production and tracked test code. Ignored temporary probes allowed. No implementation commits, no dependency or public API changes. Publish exactly one Reply for each task in docs/codex/outbox.md on agent/codex-mailbox, with actual baseline SHA, confirmed evidence vs hypotheses/UNKNOWN, exact commands/exits, and narrow proposed follow-up where justified. Use existing tests and deterministic real JavaFX fixtures. Preserve established contracts and security; no speculative fixes. Verify clean local checkout and fresh remote refs before starting and before each task/reply. Unexpected production advancement, dirty tree or shared infrastructure failure: STOP safely and report remaining NOT_RUN. Task-specific unknown contract: report evidence and continue to next independent task. CTRL-060 is final regression and ownership audit on accepted HEAD, including all focused modes CTRL-049…055 (TextFieldSkinDisposal, TextAreaLovLifecycle, TableAdapterClose, ValidatorSubchanges, TextAreaSkinDisposal, TabPaneSkinListeners, WrapButtonOwnership), relevant earlier regressions and compile within its timebox. Include exact skipped modes and reasons. Do not claim user acceptance of new evidence or actual heap measurements without data. Keep earlier per-task boundaries. Stop after CTRL-060, no self-assignment of CTRL-061 or any other task. Production HEAD should remain unchanged throughout.

---

## MESSAGE CTRL-056

**Status:** DONE / READ-ONLY / ARCHITECT REVIEWED
**Timebox:** 45 minutes
**Reply ID:** CTRL-056
**Task:** AUDIT — ValidMan reparenting / ownership

CTRL-037 runtime: moved control still dispatches blur to former manager. Read actual registration/removal and supported caller paths. Real Stage with two managers, shared/distinct validators, move control between parents, blur before/after, remove/readd, explicit unbind if supported. Separate scene-graph reparenting from supported transfer of validation ownership. Count old/new callbacks and validation outcomes, check CTRL-033/052. READ-ONLY: no production change, report callers/evidence, contract alternatives and minimal proposed scope or NO_CHANGE.

---

## MESSAGE CTRL-057

**Status:** DONE / READ-ONLY / ARCHITECT REVIEWED
**Timebox:** 45 minutes
**Reply ID:** CTRL-057
**Task:** AUDIT — Binder partial bind failure

CTRL-023 runtime: exception leaves successful controls bidirectionally attached. Inspect Binder.bind callers and cleanup, exception paths and retry/reuse patterns. Real fixture with successful first binding and controlled failing later binding; observe model/control writes both directions, repeated bind/close/unbind, clean success. Distinguish intentional partial binding from leak/atomicity requirement. READ-ONLY; no transaction or generic rollback changes. Report exact failure, ownership ledger, supported caller evidence, rollback alternatives and data-loss risk. No corporate DB assumption.

---

## MESSAGE CTRL-058

**Status:** DONE / READ-ONLY / ARCHITECT REVIEWED
**Timebox:** 45 minutes
**Reply ID:** CTRL-058
**Task:** AUDIT — SceneKeyManager action selection

CTRL-024 plus triage: candidate picked before enabled/security checks. Inspect action registration/order and security semantics, exercise two same-key actions: first disabled or denied, second eligible; reverse registration; nested parent/child scopes; no eligible action; event consumption and actual invocation counts. Use synthetic key events on real Stage and controlled security fixture; never weaken authorization or invoke denied action. READ-ONLY: distinguish intentional key reservation/precedence from starvation, propose explicit contract options with evidence. No action-dispatch change.

---

## MESSAGE CTRL-059

**Status:** DONE / READ-ONLY / ARCHITECT REVIEWED
**Timebox:** 45 minutes
**Reply ID:** CTRL-059
**Task:** AUDIT — AddLinkPane external/inner LOV precedence

CTRL-046 runtime and actual AddLinkPane external-first order: both buttons visible, external/inner/F9 each one write. Inspect actual AddLinkPane construction and relevant callers, reproduce external→LOV and reverse, detach permutations, validate/readOnly and F9 routes, counts and layout. Compare CTRL-045/049/050 contracts; no changes to public LOV property, priority, CTRL-034, or production. Report real caller evidence, user-visible consequences, competing precedence options and architect decision required. No corporate form/DB claims unless actually tested.

---

## MESSAGE CTRL-060

**Status:** DONE / READ-ONLY / ARCHITECT REVIEWED
**Timebox:** 45 minutes
**Reply ID:** CTRL-060
**Task:** VERIFY — cross-task regression and ownership audit

Read-only on final verified production HEAD. Run official focused modes for all newly pushed fixes plus existing TextFields, ControlsFollowup, WrapButtonDeferred, BoundLovSetter, LovBindingInterleaving, LovListenerLifecycle, LovDefaultValidation, LovIntegration, FormCancel, FormLifecycle, TextFieldSkinDisposal, TextAreaLovLifecycle if available, adapter/ValidMan relevant modes, Maven -Pbuild compile, PowerShell parse and diff checks within remaining time. Report actual exit codes, skipped modes/reasons (CTRL-034 unsupported scenario excluded), failures without hiding, exact SHAs and per-file scope; no production changes. Compare added instance fields, eager allocations and registration counts per control/skin from diffs; no invented retained-byte metrics. Summarize 10 tasks with DONE/NO_CHANGE/BLOCKED/PARTIAL/NOT_RUN, unresolved contracts, next triage. Stop after mission.

---

## MESSAGE CTRL-061

**Status:** DONE / ARCHITECT APPROVED / USER ACCEPTED — independent Windows `CTRL-061 PASS`, exit code `0`. Production `1db820ca0c7c0c3d2e32f1dbbfe92bc780485b1e`.
**Repo:** ssu95a/fore25
**Production branch:** migration/java25-correctness
**Baseline remote HEAD:** ed5533d8d03c1ca1500b65660060d47ce336e3d3
**Reply ID:** CTRL-061
**Dependency:** CTRL-059 reproduced defect, CTRL-060 23/23 GREEN, CTRL-049…055 user accepted.

### Evidence and scope
CTRL-059 reproduced actual AddLinkPane external-first setup: external.setTextField(field), field.setLOV(provider,true), real shown Stage. With readOnly=true inner and F9 opened zero, external Button.fire opened once and wrote model once. Source reviewed: JInvLOVButton.LOVHandler.handle delegates to field.showLOV without readOnly check. Native physical input and corporate DB UNKNOWN. Do not change external/inner precedence, restore hidden buttons, or redefine readOnly as prohibition on programmatic provider updates.

1. Verify clean checkout, current remote refs and read CTRL-059 fully. Recover ignored ExternalLovAudit if available. Read JInvLOVButton, JInvChoiceButton, JInvTextField/JInvTextArea readOnly contracts and actual AddLinkPane setup. Add tracked focused ExternalLovReadOnlySmoke.java and official -ExternalLovReadOnly runner. RED on unchanged baseline: real shown Stage, external-first arrangement, count opens/copies/model events, confirm normal editable external/inner/F9 positive controls. Test explicit readOnly=true vs editable=false, both values of buttonIgnoresEditable, readOnly toggling back, ordinary provider updates while readOnly, standalone provider-only button, and existing TextArea field-delegated button behavior. Distinguish native disabled state from handler-level guard. Include callback/navigation/focus side effects where observable; blocked activation must not queue focus or navigation or open provider. Record existing behavior separately for editable=false without readOnly; preserve it. Synthetic fire is sufficient for deterministic probe, do not claim native physical clicks.
2. Only permitted production file src/main/java/ru/inversion/fx/form/lov/JInvLOVButton.java. Minimal early activation guard for JInvTextField/JInvTextArea field-delegated LOV when explicit readOnly=true, before requestFocus/runLater/showLOV. Confirm appropriate existing isReadOnly API. Do not change ordinary TextInputControl or standalone ILov routing, direct programmatic showLOV/provider writes, buttonIgnoresEditable semantics, disable/visible bindings or common button reattachment. If TextArea has different established contract, report and restrict fix to proven TextField defect. No new fields/listeners/bindings/helper allocation. Do not tackle delayed accepted completion from retired provider (separate high priority candidate), CTRL-034, generic LOV ownership, external/inner priority, or security semantics.
3. GREEN same assertions: blocked external activation zero opens/model writes and no navigation/focus scheduling; allowed routes preserve original positive counts, standalone and provider-only behavior, readOnly toggle restores permitted activation, ordinary programmatic provider updates retain legacy contract. Preserve CTRL-049/050/055 ownership. Report if visual enabled appearance remains unchanged (handler guard addresses data integrity, visual policy separate).
4. Official focused plus 3 fresh JVM runs; existing TextFields, ControlsFollowup, LovIntegration, LovDefaultValidation, LovListenerLifecycle, BoundLovSetter, LovBindingInterleaving, TextFieldSkinDisposal, TextAreaLovLifecycle, WrapButtonOwnership, and relevant readOnly modes discovered in runner. Maven -Pbuild compile, PowerShell parse, diff check. One isolated validated fast-forward commit only, verify remote SHA. If reliable GREEN not achieved, BLOCKED/PARTIAL with no incomplete production push. No assertion weakening.
5. Publish exactly one Reply CTRL-061 in docs/codex/outbox.md on mailbox branch: exact baseline/final SHA, RED/GREEN counts and exits, scope and allocation comparison, residual questions and independent Windows command with -SettingsFile "D:\Java\.m2\settings.xml". Stop after CTRL-061; no self-assignment of CTRL-062.

### Boundaries
Only ssu95a/fore25. Production migration/java25-correctness, mailbox agent/codex-mailbox. main and JInvCommon read-only. No force push/destructive reset, dependency/public API changes or unrelated files. Russian source comments. Timebox 60 minutes.

---

## BATCH CTRL-062…CTRL-066 — five-task sequential correctness assignment

**Status:** DONE / ARCHITECT APPROVED / USER ACCEPTED — user confirmed all five independent checks OK; individual exit codes not separately supplied. Final accepted production HEAD `6ccb4403d9322c3d4160f6513f6498fe003f9a5e`.
**Repo:** ssu95a/fore25
**Production branch:** migration/java25-correctness
**Mailbox branch:** agent/codex-mailbox
**Baseline remote HEAD:** 1db820ca0c7c0c3d2e32f1dbbfe92bc780485b1e
**Budget:** 270 minutes total (60+45+60+60+45).
**Dependency:** CTRL-061 DONE / ARCHITECT APPROVED / USER ACCEPTED, exit 0.

### Latest execution authorization
Execute CTRL-062…066 in exact order, autonomously, no intermediate architect/user confirmations needed for explicitly scoped fixes. This overrides previous standalone/manual-only restrictions for these five tasks only. Before each task/reply verify remote refs, clean checkout, read latest mailbox and relevant prior replies. Each task starts from the actual verified clean preceding HEAD, never reset to historical baseline. For each FIX or AUDIT→FIX use real deterministic fixtures, tracked focused smoke and runner mode where justified, RED before production edit, same assertions GREEN afterward, positive controls, exact callback/model counts, official focused plus three fresh JVM runs, relevant existing regressions, Maven -Pbuild compile, PowerShell parse, diff check. One isolated validated fast-forward production commit per fix, verify remote SHA; one separate Reply per task. No assertion weakening, arbitrary sleeps or JavaFX internal reflection. No eager per-control allocations; account for new fields/objects and allocation frequency. No fix based solely on source suspicion. On unresolved task-specific contract, report READ-ONLY/BLOCKED/PARTIAL and proceed only if next task independent and baseline clean with no regression failures. Stop entire batch on dirty/conflicting state, failed shared regression or unreliable infrastructure; report remaining NOT_RUN. No incomplete production pushes or destructive cleanup. Main and JInvCommon read-only, no force push, destructive reset, dependency/public API changes, unrelated cleanup, CTRL-014/034/CAL-TIME-001 or unapproved contract changes. Russian Java comments. Report baseline/final SHA, exact RED/GREEN commands/exits, counts, scope, allocation delta, unknowns, Windows rerun with -SettingsFile "D:\Java\.m2\settings.xml". Independent user acceptance remains separate. Stop after CTRL-066. Do not self-assign further work. Total timebox is a ceiling, not permission to expand scope.

---

## MESSAGE CTRL-062

**Status:** DONE / ARCHITECT APPROVED / USER ACCEPTED — user confirmed package checks OK. Production `f6a73c0d583b25012b2132fe643a89bea471b024`.
**Timebox:** 60 minutes
**Reply ID:** CTRL-062
**Task:** FIX — JInvTextArea retired LOV delayed completion

CTRL-050 report reproduced delayed accepted callback from former A copying current B value onto draft, independently of fixed valueProperty subscription. Read current JInvTextArea.showLOV/setLOV, AbstractLovBase, ILov and JInvLOVButton callback contracts. Recover ignored diagnostic if available. RED on real shown Stage with controllable deferred provider completion (no sleep): A→B, A→null, A→B→A, current-provider accepted/cancelled completion, distinct draft/model, counts of copies/model writes and navigation/focus/caller callbacks, late cancelled completion. Distinguish stale attachment from same-provider repeated setter and overlapping choices: document actual supported semantics, don't invent latest-wins contract. For stale completion neither copy nor accepted navigation should occur; preserve legitimate caller completion protocol (if cancellation notification required, preserve exactly once without accepted navigation; if contract unclear, report rather than silently change it). Check provider value notifications separately: direct current provider programmatic updates remain legitimate; a provider publishing a value when reattached is not distinguishable solely by completion callback. Do not claim ABA value-notification suppression unless proven separately. Only production JInvTextArea.java. Prefer existing lazy LovAttachment state and minimal attachment identity/epoch captured per request, no new eager per-control objects, no generic token allocation per field or global manager, no new state unless justified. Ensure bound-property setter failure does not invalidate current valid attachment; preserve CTRL-050 onSetLov/toolbar and programmatic behavior. Direct writable lovProperty mutations out of scope unless already covered contract. Inspect analogous JInvTextField path read-only and report if reproduced; do not edit it in this task. No readOnly policy change or provider API change. New focused -TextAreaLovDelayedCompletion mode. Regressions TextAreaLovLifecycle, ExternalLovReadOnly, TextAreaSkinDisposal, LovIntegration, LovListenerLifecycle, BoundLovSetter, LovBindingInterleaving, Editors, FormLifecycle. If ABA or caller completion contract cannot be safely resolved in scope, report residual precisely or BLOCKED; do not silently weaken tests.

---

## MESSAGE CTRL-063

**Status:** DONE / ARCHITECT APPROVED / USER ACCEPTED — user confirmed package checks OK. Production `33a73ce5ccfe77eb7a5bcdd9e65c060cce681e5c`.
**Timebox:** 45 minutes
**Reply ID:** CTRL-063
**Task:** FIX — JInvTextField F9 explicit readOnly with buttonIgnoresEditable

CTRL-061 explicitly reproduced locked TextField F9 opening once when buttonIgnoresEditable=true. Existing ExternalLovReadOnlySmoke positively asserts this old out-of-scope behavior; update only that explicitly superseded assertion as part of this task, retaining all other CTRL-061 coverage. RED verify actual keyboard handler with both ignoreEditable settings, readOnly true/false and editable false/true, normal external/inner positive controls, provider opens/copies/model writes, TAB/focus and event consumption where observable. Only production JInvTextField.java. Minimal explicit isReadOnly guard on F9 user activation before LOV/commit/navigation/focus side effects, preserve normal editable=false+ignoreEditable=true behavior, direct showLOV and provider updates. No fields/listeners/allocations, no change to external button visual policy or generic key precedence/security. Preserve normal event propagation/consumption unless existing readOnly path proves required behavior. GREEN extend existing ExternalLovReadOnly mode to prove locked F9 zero opens/writes under both settings; optional separate focused mode -LovKeyboardReadOnly if necessary. Report exactly which old assertion was deliberately superseded by this approved contract; do not weaken other assertions. Regressions ExternalLovReadOnly, TextFields, ControlsFollowup, LovIntegration, LovDefaultValidation, LovListenerLifecycle, TextFieldSkinDisposal, BoundLovSetter, LovBindingInterleaving, TextAreaLovLifecycle and new CTRL-062 mode if available.

---

## MESSAGE CTRL-064

**Status:** DONE / ARCHITECT APPROVED / USER ACCEPTED — user confirmed package checks OK. Production `5b4032851f6677dc6b5905147b2accb93d27e5a0`.
**Timebox:** 60 minutes
**Reply ID:** CTRL-064
**Task:** AUDIT → FIX — DSFXAdapter mouse event ownership after close

CTRL-051 removed KEY_RELEASED handler and KEY_PRESSED filter; current source at reviewed diff still registers MOUSE_RELEASED, MOUSE_PRESSED and DRAG_DETECTED filters calling refreshCurrentRow. Runtime post-close mouse failure not yet proven. Read actual current DSFXAdapter, recover TableAdapterClose fixture. Real shown table, deterministic synthetic mouse event delivery to actual installed filters; count refreshes and exceptions, caller-owned handlers, pre-close positive paths, repeated close, late Dataset events. If RED, only DSFXAdapter production: use one shared owned mouse handler if behavior equivalent, retain exact handle and remove each owned registration before teardown; no extra object per mouse event, minimize nullable references, preserve caller handlers, no blanket remove/filter null guard instead of unsubscribe. Keep CTRL-051 keyboard cleanup, no same-table rebind support, no paging changes. GREEN zero former callbacks/exceptions and correct pre-close behavior. New focused mode -TableAdapterMouseClose or extend existing TableAdapterClose with separate assertions and explicit report. Regressions TableAdapterClose, DataBinding, ChoiceAdapter, ChoiceNullLookup and relevant table modes. If source ownership or actual event dispatch cannot be verified, READ-ONLY/BLOCKED.

---

## MESSAGE CTRL-065

**Status:** DONE / ARCHITECT APPROVED / USER ACCEPTED — user confirmed package checks OK. Production `424dc86b9feeed9c0cc0ba1e0c383ae06384596e`.
**Timebox:** 60 minutes
**Reply ID:** CTRL-065
**Task:** AUDIT → FIX — DSFXAdapter pending paging / scroll binding after close

CTRL-051 report left pending paging and scroll paths outside scope. No confirmed runtime failure yet. Read current DSFXAdapter SwappingScrollBinding and actual paging/scroll scheduling, Dataset lifecycle and close ordering. Identify precise owned subscription/queued work, not generic concurrency (FX thread normal). Reproduce real table with deterministic queued callback or scroll/paging transition followed by close before execution; positive control normal paging, close repeated, late events, replaced scrollbars if applicable, counts of fetch/cursor/model effects and exceptions. Use controlled in-memory Dataset/test double where possible, no real corporate DB assumption, no arbitrary sleeps. If confirmed, only DSFXAdapter production: exact owner teardown for scroll listeners/owned pending callbacks; cancel or invalidate truly obsolete queued work with minimum existing lifecycle state, no global executor, no unnecessary per-row/per-control allocations, no changes to paging sizes, selection or same-table rebind contract. A guard can invalidate already queued work but is not substitute for removing live retained subscriptions. Keep keyboard and mouse cleanup from CTRL-051/064. If multiple independent defects, isolate smallest proven fix; report other candidates separately. New focused -TableAdapterPagingClose mode if fix. Regressions TableAdapterClose, CTRL-064 focused if available, DataBinding and relevant existing paging/table modes. If paging requires unavailable infrastructure or contractual behavior is unknown, READ-ONLY/BLOCKED, no speculative fix.

---

## MESSAGE CTRL-066

**Status:** DONE / ARCHITECT APPROVED / USER ACCEPTED — user confirmed package checks OK. Production `6ccb4403d9322c3d4160f6513f6498fe003f9a5e`.
**Timebox:** 45 minutes
**Reply ID:** CTRL-066
**Task:** AUDIT → FIX — JInvTextAreaSkin retained toolbar button ownership

CTRL-053 fixed managed scene/editable subscriptions, action registration and toolbar/FE visibility bindings; separately retained toolbar button width callbacks and owned handlers remained untested. Read current JInvTextAreaSkin.initToolBar/setButtonStyle/dispose and existing CTRL-053/050. Prove actual post-dispose effect or strong ownership edge in supported scenario: real Stage, caller-owned toolbar item retained and moved/reused if supported, old skin-owned external-editor/FE buttons separately, repeated add/remove/readd and skin replacement, width changes, callback counts, style/selection/visibility positive controls. Do not infer leak from mere cycles or externally held objects; distinguish caller-owned node retention of old skin versus intentionally retained skin-owned button. No native editor launch; stub safe action paths. Only JInvTextAreaSkin production if deterministic RED and ownership clear. Remove exact owned registrations on item removal/dispose as appropriate; prefer existing lifetime management or stateless handler where possible, no eager per-button helper maps/collections without clear necessity, preserve toolbar item identity and caller-owned actions/bindings. Do not alter CTRL-050 LOV attachment/replacement restoration or generic JInvLOVButton association, keyboard manager contract, toolbar styling semantics. If ambiguous, READ-ONLY/BLOCKED. New focused -TextAreaToolbarOwnership mode if fix; regressions TextAreaSkinDisposal, TextAreaLovLifecycle, CTRL-062 focused if available, ExternalLovReadOnly, Editors, ControlsFollowup, SceneKeyLookup. Final reply includes batch summary of all five task statuses, exact final HEAD, residual issues and stop. No further self-assignment.

---

## BATCH CTRL-067…CTRL-074 — System correctness and performance research

**Status:** CLOSED / ARCHITECT REVIEWED — all eight PARTIAL / READ-ONLY replies received; remaining unknowns retained. Production unchanged. Follow-up fixes authorized separately.
**Repo:** ssu95a/fore25
**Production branch:** migration/java25-correctness
**Mailbox:** agent/codex-mailbox
**Baseline remote HEAD:** 6ccb4403d9322c3d4160f6513f6498fe003f9a5e
**Budget:** 465 minutes (7 hours 45 minutes), ceiling, not target duration.
**Dependency:** CTRL-049…055 and CTRL-061…066 architect approved and user accepted; CTRL-056…060 audit completed, previous 23/23 battery agent GREEN.

### Latest authorization and execution contract
The architect explicitly authorizes CTRL-067…074 only. Execute in exact order autonomously, no intermediate approvals. This is a strictly READ-ONLY research batch for production and tracked test code, even when a bug is proven. No production commits, no tracked test additions or changes, no dependency or public API changes. Ignored temporary diagnostic probes allowed; do not overwrite user files. All Git/GitHub operations necessary within these boundaries approved; writes only separate Reply messages in docs/codex/outbox.md on agent/codex-mailbox. Main and JInvCommon read-only, no other repo writes, no destructive Git commands or force push. Verify clean local checkout and remote production/mailbox refs at launch, before each task and before publishing each reply; production HEAD must remain 6ccb4403d9322c3d4160f6513f6498fe003f9a5e throughout. Unexpected production advancement, dirty tracked tree, conflicting mailbox write, or shared infrastructure failure: STOP safely and report remaining NOT_RUN. Do not reset or discard user work. Task-local lack of evidence/unsupported contract: report UNKNOWN/BLOCKED and continue independent tasks if baseline remains clean. Do not repeat completed tasks or expand scope. Stop after CTRL-074.

For each task read actual implementation and relevant prior replies/tests; distinguish observed facts, source-derived paths, hypotheses and unknown contracts. Use actual Java 25/JavaFX 25 where available, deterministic FX fixtures with actual shown Stage when GUI behavior is under test, no arbitrary sleeps, no JavaFX internal reflection. Preserve legacy first → correctness → targeted optimization, prioritize data integrity/transactions/lifecycle over cosmetics, minimal state and memory per component. Ordinary GUI on FX thread; investigate workers only where actual asynchronous work exists. No real corporate DB writes, native external editor launch or security-policy changes. Positive controls, exact counts, actual exceptions and event order; report failed experiments honestly. Record commands/exit codes, runtime versions, baseline SHA, probe path and reproducibility, scope and limitations. Diagnostic exit 0 does not mean defect absent. A reproduced defect does not establish a supported contract; separate proposed fix from workaround. No automatic production fix. Where feasible run relevant existing smoke modes using src/test/probes/Test-LegacyGuiSmoke.ps1 and -SettingsFile "D:\Java\.m2\settings.xml"; no need to rerun full regression per read-only task. Respect CTRL-014/034, CAL-TIME-001, existing LOV precedence, ValidMan transfer, Binder rollback and keyboard reservation open contracts. New Russian comments for temporary Java probes where needed.

Publish exactly one Reply CTRL-0XX after each task with DONE/READ-ONLY, NO_DEFECT, BLOCKED or PARTIAL as appropriate, ranked confirmed findings, proposed minimum next task and explicit UNKNOWN. Include exact baseline and verify no production/tracked changes. No claim of independent user acceptance. CTRL-074 final reply includes full batch summary and ranked next steps. No self-assignment after CTRL-074.

---

## MESSAGE CTRL-067

**Status:** PARTIAL / READ-ONLY / ARCHITECT REVIEWED — report received, residual scope not closed
**Timebox:** 60 minutes
**Reply ID:** CTRL-067
**Task:** AUDIT — DSFXAdapter bulk fetch/search lifecycle after close

CTRL-051/064/065 closed keyboard, mouse, scroll subscriptions and deferred PageDown selection, but bulk fetch/search workers remained untested. Inspect actual task/executor/FX callbacks, dataset ownership, close ordering and caller paths. Map worker start, cancellation, completion, error and FX delivery. Use controlled in-memory dataset or test double and explicit synchronization barriers (no sleeps), real shown table: normal completion positive control, close before completion delivery, close during work where supported, repeated close, late success/error/cancel. Measure cursor/model/selection changes, fetches, error dispatch, callbacks, pending jobs. Distinguish work already accepted by DB from stale UI completion; do not claim that closing rolls back external work. No real corporate SQL unless already authorized and available; no destructive DB operations. Record supported lifecycle and ownership, exact reproduction or UNKNOWN. Do not alter threading model or production.

---

## MESSAGE CTRL-068

**Status:** PARTIAL / READ-ONLY / ARCHITECT REVIEWED — report received, residual scope not closed
**Timebox:** 60 minutes
**Reply ID:** CTRL-068
**Task:** AUDIT — Table editing commit/cancel on row navigation

Read actual JInvTable, cell editor/adapter implementations and existing TableEditing, TableCancel, EditorReentry and DataBinding probes; locate actual production editor paths rather than guessing class names. Real Stage with identifiable rows A/B/C and bound in-memory DataSet. Exercise start edit A, modify draft, select B by keyboard/mouse synthetic events or model cursor change, commit/cancel, failed validation/conversion, repeated editor entry. Compare UI cell, editing position, DataSet cursor, original row identity, actual write target, values and write counts, validation and callback order. Establish baseline legal behavior before asserting defect; distinguish text draft vs committed value and row index vs entity identity. Confirm whether accepted edits target original row and cancellation does not commit or undo unrelated model changes. No change to navigation or transaction contracts. Report minimal reproducer and proposed scope only.

---

## MESSAGE CTRL-069

**Status:** PARTIAL / READ-ONLY / ARCHITECT REVIEWED — report received, residual scope not closed
**Timebox:** 60 minutes
**Reply ID:** CTRL-069
**Task:** AUDIT — Table editing refresh/sort/replacement during active editor

Inspect supported refresh, sort, execute, row update/removal and editor paths; use actual stable row keys, in-memory dataset where equivalent. Start edit with distinct draft, change row ordering or refresh data, then commit/cancel via supported actions. Test removed/replaced original row, unchanged index with changed identity, duplicate sort keys, legitimate re-entry, validation error. Record original entity, current cursor, editing cell, selected item, actual write destination, model write counts and selection events. Do not assume sort/refresh is legal during edit; document guards and callers. Distinguish server-side sorting from simulated local ordering and report unsupported DB paths UNKNOWN. Prioritize wrong-row writes and lost drafts over presentation. No production or tracked test edits.

---

## MESSAGE CTRL-070

**Status:** PARTIAL / READ-ONLY / ARCHITECT REVIEWED — report received, residual scope not closed
**Timebox:** 60 minutes
**Reply ID:** CTRL-070
**Task:** AUDIT — Binder successful binding lifecycle and ownership

Extend CTRL-057 evidence (partial bind confirmed, rollback contract unknown) by studying successful bindings and normal form lifecycle. Inspect Binder, actual bindControls callers, converter bidirectional routes, validators/adapters/LOV and corresponding release paths. On real Stage and supported controller/form fixture where feasible, establish positive two-way updates and exact callback counts, then supported close/dispose/unbind and reopening fresh instance; test repeated bind only if supported, compare explicit unbind overloads including Object/Object converter route. Retain distinguishable old controls/model deliberately to observe stale updates, label artificial retention; do not infer leaks from cycles or ordinary Stage.hide alone. Identify actual ownership and whether normal full form close releases it. No global rollback, new cleanup contract or production changes. Report supported paths, strong reference chain if proven, counts, narrow next task.

---

## MESSAGE CTRL-071

**Status:** PARTIAL / READ-ONLY / ARCHITECT REVIEWED — report received, residual scope not closed
**Timebox:** 60 minutes
**Reply ID:** CTRL-071
**Task:** AUDIT — Business form partial initialization and close lifecycle

Inspect AbstractBaseController launch/init/bindControls and actual lifecycle paths, accepted Singleton close veto/CTRL-032/035/042 and FormLifecycle/FormCancel tests. Reproduce via safe fixture at supported initialization failure points (before/after binding or registration), close veto then successful close, reopen, nested form or progress Stage only if supported. Count registration, visible forms, close callbacks, owned resources, listener/model effects and queued completion; verify no orphan singleton or invalid reuse. Distinguish failed initialization from ordinary cancellation, and request close from actual WINDOW_HIDDEN. No corporate DB, no real external editor, no changes to modal/MDI/WAIT contracts. Report actual launcher coverage vs simplified fixture and unresolved risks, not speculative rollback.

---

## MESSAGE CTRL-072

**Status:** PARTIAL / READ-ONLY / ARCHITECT REVIEWED — report received, residual scope not closed
**Timebox:** 60 minutes
**Reply ID:** CTRL-072
**Task:** AUDIT — DataSet adapter repeated execute/refresh and cursor integrity

Read actual DataSet event/adapter paths in fore25 and available dependency contracts (JInvCommon read-only). Real attached table and in-memory dataset with distinct stable row identities. Exercise execute/refresh repeated, empty→nonempty→empty, row insertion/removal/update around cursor, late navigation/row notifications and sort where supported. Observe exact dataset cursor vs table selected identity, selection/cursor propagation counts, list changes, model writes, recursion/re-entry, stale events after close and fresh adapter on fresh table. Preserve intentional no-selection/null semantics; do not assume same-table rebind or DB-side sort contract. Compare CTRL-051/064/065 and existing DataBinding/table probes. Report confirmed mismatch/redundant work with exact sequence and counts, otherwise NO_DEFECT/UNKNOWN. No production changes.

---

## MESSAGE CTRL-073

**Status:** PARTIAL / READ-ONLY / ARCHITECT REVIEWED — report received, residual scope not closed
**Timebox:** 45 minutes
**Reply ID:** CTRL-073
**Task:** AUDIT — LOV residual callback and attachment paths

Read accepted CTRL-050/061/062/063 and source. CTRL-062 diagnostic TextField forwarded stale accepted result but preserved draft; investigate whether actual caller navigation or other side effects follow. Matrix delayed completion A→B, A→null, A→B→A; accepted/cancel, real caller routes F9 and button and direct programmatic showLOV, distinct provider value notification vs completion; current provider update and same-provider repeat positive controls. Inspect direct writable lovProperty callers and actual supported contract, not a proposed universal redesign. Test reattached-provider notifications only with clear distinction between legitimate updates and stale choice; report if indistinguishable without new provider contract. Count model writes, accepted/cancel callbacks, focus/navigation and exceptions. No change to CTRL-034, button precedence, readOnly policy, generic provider API or production.

---

## MESSAGE CTRL-074

**Status:** PARTIAL / READ-ONLY / ARCHITECT REVIEWED — report received, residual scope not closed
**Timebox:** 60 minutes
**Reply ID:** CTRL-074
**Task:** AUDIT — Performance and memory audit — DataSet/Binder/table hot paths

Read actual code at verified HEAD; rank candidates by correctness first, then CPU/allocation/retention impact. Inspect navigation/refresh/sort/event dispatch, collection traversal complexity, duplicate data/cache, per-row/per-cell/per-control allocation, repeated binding/converter/editor construction, redundant notifications and form initialization. For top 2–3 candidates use reproducible controlled fixture and available JDK tools (JFR or allocation counters if installed and practical), warmed repeat measurements, record JDK/FX versions, heap/fixture sizes, iterations, workload, median/range, allocation units and measurement limitations; vary N to assess scaling, distinguish source-derived complexity from measured data. FX thread normal; no generic synchronization optimization. Do not confuse allocated bytes with retained heap or infer byte savings from field counts. No external dependencies, profiler install, production code or tracked test changes. If measurement unavailable, give source evidence and precise proposed benchmark, not invented numbers. Deliver ranked candidate table with location, evidence, CPU/memory tradeoff, estimated scope/risk, correctness prerequisites and next smallest experiment. Include final batch summary of all eight replies, unchanged SHA, blockers and recommended next 3–5 RED→GREEN tasks ranked by actual evidence.

---

## BATCH CTRL-075…CTRL-078 — data integrity and stale delivery corrections

**Status:** DONE / ARCHITECT APPROVED / USER ACCEPTED — user confirmed package verification OK; separate numeric exit codes not supplied. Final accepted HEAD `70b139ed54d00c46b4c2ae597535d82ddbe5a2ab`.
**Repo:** ssu95a/fore25
**Production branch:** migration/java25-correctness
**Mailbox branch:** agent/codex-mailbox
**Baseline remote HEAD:** 6ccb4403d9322c3d4160f6513f6498fe003f9a5e
**Budget:** 270 minutes (90+60+60+60), ceiling.
**Dependency:** CTRL-067…074 read-only reports at mailbox 7e3d6fe2 reviewed; CTRL-062…066 user accepted.

### Latest authorization and common execution contract
Execute only CTRL-075…078 sequentially, no intermediate confirmations needed within the explicit scopes. Highest priority CTRL-075. Before each task and reply verify clean checkout, actual production HEAD and fresh mailbox; each next task uses preceding verified HEAD. No reset to historical baseline. Recover ignored research fixtures where available, but reproduce tracked deterministic RED on unchanged production before edits; use real Java 25/FX25 shown Stage for UI, controlled in-memory Dataset and FX barriers, positive controls, exact entity identity and model/callback counts, no arbitrary sleeps, JavaFX internals reflection or test-only production hooks. Same assertions GREEN after fix; official runner mode, three fresh JVMs, relevant existing regression modes, Maven -Pbuild compile, PowerShell parse, diff check. One isolated validated fast-forward production commit per fix and one Reply per task, verify remote SHA. No assertion weakening or incomplete production push. If CTRL-075 cannot be reliably fixed, report BLOCKED/PARTIAL, do not claim wrong-row correctness, and continue independent tasks only if clean baseline and no shared regressions; stop entire batch on dirty/conflicting state, failed shared regression or unreliable infrastructure. No speculative fixes. Main and JInvCommon read-only, no force push/destructive reset, dependency/public API changes or unrelated cleanup. No new eager per-control objects; minimize instance state and account for allocations. Russian Java comments. Preserve legacy contracts, transactions, FX thread policy, CTRL-014/034 and CAL-TIME-001. No SQL rollback/cancellation guarantees, no real corporate DB writes. Report exact baseline/final SHA, RED/GREEN commands/exits, actual counts, scope, allocation changes, unknowns and Windows rerun with -SettingsFile "D:\Java\.m2\settings.xml". User acceptance remains separate. Stop after CTRL-078, no further self-assignment.

---

## MESSAGE CTRL-075

**Status:** DONE / ARCHITECT APPROVED / USER ACCEPTED — user confirmed package checks OK. Production `eaf0bed0be6a888ce259a69b2899b49da8ad7f57`.
**Timebox:** 90 minutes
**Reply ID:** CTRL-075
**Task:** FIX / CRITICAL — JInvTextFieldTableCell synthetic cancel commit writes wrong row

CTRL-069 reproduced wrong-entity writes on actual JInvTable, DSFXAdapter, JInvTextFieldTableCell and shared ObservableList ArrayDataSet: edit A at index 0, enter draft, arm via synthetic MOUSE_PRESSED, replace A with X/remove A/reorder descending. Synchronous cancelEdit commits to X/B/C respectively instead of A. Unarmed mutations produce zero commits. CTRL-068 normal F8 writes A once. Recover target/ctrl-table-research/EditAudit.java. Read actual renderer and adapter commit path, prior GUI-006/TableCancel/EditorReentry contracts. RED tracked probe on same actual production paths; require original row object identity, distinguish equal-valued distinct entities and duplicate sort keys, track actual model write target and event row identity, exact commit counts. Test armed replacement/removal/permutation, unarmed variants, valid same-row mouse-armed commit, ordinary F8/ENTER and Escape as supported, same-index cell reuse/reentry and conversion rejection where feasible. Test reentry after invalidated edit and subsequent legitimate edit. Do not substitute equality/index/current cursor for original entity identity. Only permitted production class JInvTextFieldTableCell. Minimal guard of synthetic cancel commit against original edit identity and valid position/column/table as applicable; capture identity at correct lifecycle boundary and clear marker/reference on cancel/commit/reentry; don't retain row on idle virtualized cells. Do not silently redirect commit to another index or write original entity after removal. If position no longer identifies original row, suppress synthetic commit; draft recovery/UI notification policy remains separate UNKNOWN, report clearly that invalidated draft may be discarded under existing cancel behavior. Preserve same-row commit and Escape, no changes to normal commit semantics, sorting, DataSet/Binder or transactions. GREEN zero wrong-row writes and zero synthetic commit for invalidated row, positive valid commit exactly once; verify replacement with equal-valued distinct object. If a different ordinary commit path is also defective, report separately rather than expanding production scope. New focused -TableEditRowIdentity mode. Regressions TableEditing, TableCancel, EditorReentry, DataBinding, TableAdapterClose, TableAdapterPagingClose and relevant conversion/validation modes. Record actual test coverage, no claim of server-side sort/SQL reproduction.

---

## MESSAGE CTRL-076

**Status:** DONE / ARCHITECT APPROVED / USER ACCEPTED — user confirmed package checks OK. Production `07a51fda7407161b9f1a1eb2bed04930bc6fc0b9`.
**Timebox:** 60 minutes
**Reply ID:** CTRL-076
**Task:** FIX — DSFXAdapter bulk moveToLast/rowLoader after close

CTRL-067 reproduced two NPE and one stale selection after adapter close during nested progress loop: moveToLast restores navigation listener through null getDataSet; rowLoader queues selection and cursor positioning after close. Active bulk selects C and sets cursor 2. Recover target/ctrl-067-research/BulkLifecycleAudit.java; read actual DSFXAdapter/DSControlAdapter/ProgressTaskExecutor paths. Deterministic tracked RED with controlled in-memory Dataset, real shown table, progress lifecycle and explicit ordering barriers; active normal completion and close twice before queued delivery, late Dataset observers; capture errors and exact listener ownership, selection/cursor/model changes. Where feasible exercise close between separate queued deliveries and verify active listener restoration, not just null guards. Only DSFXAdapter production. Capture originating attachment, ensure same still-live attachment before restoring navigation listener or delivering each selection/cursor action. Never re-register adapter to retired dataset; preserve active normal behavior and independent listener registrations, do not simply replace null with captured Dataset and mutate retired data. Minimize state, reuse existing lifecycle identity if appropriate and proven. No changes to worker scheduling, fetch loop, SQL transaction/cancellation semantics, ProgressTaskExecutor/Service or search callback policy; separate stale search result/exception-safe search listener candidate. No guarantee of stopping already-running SQL. GREEN closed bulk zero NPE, zero obsolete selection/cursor requests, no restored retired adapter listener; active normal selection and cursor preserved. New focused -TableAdapterBulkClose mode. Regressions TableAdapterClose, TableAdapterPagingClose, DataBinding, TableEditing, TableCancel, EditorReentry and CTRL-075 if available. Report untested blocking SQL, cancellation/error routes.

---

## MESSAGE CTRL-077

**Status:** DONE / ARCHITECT APPROVED / USER ACCEPTED — user confirmed package checks OK. Production `f6bfd8bb2a0ca38cf36ea538140bff1f49744c7d`.
**Timebox:** 60 minutes
**Reply ID:** CTRL-077
**Task:** FIX — DSFXAdapter queued positionRow/dataSetChanged deliveries after close

CTRL-072 showed queued navigation and AFTER EXECUTE refresh issued while open survived close twice: one obsolete selection X and one extra refresh; notifications raised after close already correctly detached. Active navigation and three ordered refresh deliveries work. Recover target/ctrl-072-research/RefreshAudit.java. Read actual latest adapter after CTRL-076. RED on real shown table with genuine Dataset notifications and deterministic queue barriers; active cursor movement, active refresh burst, pending positionRow and dataSetChanged then close in same turn, each pending route isolated and combined, repeated close, late external observer notifications. Test fresh adapter on fresh table where supported, no same-table rebind contract. Only DSFXAdapter production. Guard each queued delivery against originating still-live attachment, before any table/data access; use existing identity/lifecycle state where suitable, no extra per-event wrapper object beyond existing queued callback unless justified. Preserve active event order/count and cursor/selection behavior. No coalescing, refresh optimization, sorting or general Dataset mutation changes. Do not infer SQL corruption from direct shared list changes bypassing Dataset APIs. GREEN zero stale refresh/selection/cursor/model effects, active positive counts unchanged, external observers unaffected. New focused -TableAdapterQueuedClose mode. Regressions TableAdapterClose, TableAdapterPagingClose, TableAdapterBulkClose if available, DataBinding, TableEditing, TableCancel, EditorReentry and CTRL-075. Report remaining SQL/cursor contract limitations.

---

## MESSAGE CTRL-078

**Status:** DONE / ARCHITECT APPROVED / USER ACCEPTED — user confirmed package checks OK. Production `70b139ed54d00c46b4c2ae597535d82ddbe5a2ab`.
**Timebox:** 60 minutes
**Reply ID:** CTRL-078
**Task:** FIX — JInvTextField retired LOV completion and stale navigation

CTRL-073 24-case diagnostic: direct/F9/external button × keep/replace/detach/ABA × accepted/cancelled. Completion alone zero model writes, draft preserved; stale accepted result nevertheless forwarded true and F9/button emitted TAB. Architect approves matching CTRL-062 policy: a completion from retired attachment is delivered as false/cancelled exactly once per provider completion with original reporting provider, never as accepted; no accepted focus/navigation side effect. Existing cancellation branch may request focus as before; do not claim zero cancellation focus. Recover target/ctrl-073-research/ResidualLovAudit.java. Read current JInvTextField.showLOV/setLOV/unbindLOV, CTRL-044/045/062 contracts. RED actual shown Stage, controllable deferred provider, A→B, A→null, A→B→A, current acceptance/cancel, repeated same-provider setter, overlapping current requests, bound setter rejection, ordinary current-provider updates, readOnly behavior, direct caller and actual F9/button callback routes. Count actual callback Boolean/provider, TAB intent, focus requests and model writes separately; retain distinct drafts. Only JInvTextField production. Use attachment lifetime/epoch, not provider identity alone; prefer existing lazy state if suitable, no eager object allocation on unused control, justify minimal primitive/reference cost if necessary. Invalidate only successful attachment transitions, preserve same-provider repeat, bound failure atomicity and existing callback semantics for current choices. Do not introduce latest-wins for overlapping current requests or provider API change. Direct writable lovProperty mutations, reattached-provider value notifications (no request identity), readOnly policy, button precedence, CTRL-034 and actual focus traversal outside scope. GREEN stale accepted converted to false, zero stale TAB/accepted focus branch and unchanged draft, current valid routes preserve counts. New focused -TextFieldLovDelayedCompletion mode. Regressions TextAreaLovDelayedCompletion, ExternalLovReadOnly, LovListenerLifecycle, LovIntegration, LovDefaultValidation, BoundLovSetter, LovBindingInterleaving, TextFieldSkinDisposal, WrapButtonOwnership, TextAreaLovLifecycle. Final reply includes batch summary of four statuses, exact final HEAD, confirmed fixes, residual UNKNOWN and stop. No performance optimization tasks authorized.

---

## BATCH CTRL-079…CTRL-090 — regression, residual integrity and measured performance

**Status:** CLOSED / ARCHITECT REVIEWED — all eleven resumed replies received; final agent regression 50/50 child and runner exit 0. CTRL-080/081 architecture approved, independent user rerun not explicitly reported. CTRL-083 remains BLOCKED; follow-up separately assigned. Original CTRL-079 incident UNKNOWN / NOT_REPRODUCED.
**Repo:** ssu95a/fore25
**Production branch:** migration/java25-correctness
**Mailbox branch:** agent/codex-mailbox
**Baseline remote HEAD:** 70b139ed54d00c46b4c2ae597535d82ddbe5a2ab
**Budget:** 720 minutes total (12 hours), ceiling not required elapsed duration.
**Dependency:** CTRL-075…078 architect approved and user accepted, prior accepted fixes retained.

### Latest authorization and execution contract
Execute CTRL-079…090 only, in exact order, autonomously, no intermediate approval for explicitly permitted changes. CTRL-079 is a mandatory regression gate; unexplained supported regression stops implementation and batch, report remaining NOT_RUN. READ-ONLY/AUDIT/PERF AUDIT/VERIFY tasks prohibit production and tracked test edits; ignored temporary probes allowed. AUDIT→FIX permits one isolated fix only after deterministic RED and proven contract. PERF permits one narrow measured optimization only after proving semantic equivalence and meaningful benefit using the same workload; no invented defect RED for pure optimization, use baseline correctness plus before/after measurement. If no benefit/unclear contract, report NO_CHANGE or READ-ONLY; do not force a commit. No speculative or broad refactors.

Verify clean local checkout and fresh production/mailbox refs before each task and reply. Each task uses preceding verified clean HEAD. Main and JInvCommon read-only, no other repo writes, force push, destructive reset, dependency/public API changes, user file overwrite, unsupported same-table rebind, CTRL-014/034/CAL-TIME-001 or unapproved LOV/keyboard/transaction contracts. No actual corporate DB writes, no native external editor launches. Ordinary GUI on FX thread; investigate concurrency only in actual worker paths. For fixes use deterministic actual Java25/FX25 fixtures, shown Stage where relevant, positive controls, RED before edit and GREEN same assertions, exact counts, focused tracked smoke + official runner mapping, three fresh JVMs, relevant regressions, Maven -Pbuild compile, PowerShell parse, diff check. For optimizations preserve all correctness and lifecycle semantics, no unnecessary persistent caches, no new eager per-control/per-row objects; measure time/allocations with warmed repeat samples, record environment and ranges, do not confuse allocations and retained heap. No arbitrary sleeps, JavaFX internal reflection, test-only production hooks or assertion weakening. Russian Java comments. One isolated validated fast-forward production commit per implemented task; verify remote SHA. No incomplete push. Publish exactly one Reply per task in docs/codex/outbox.md on agent/codex-mailbox. Include baseline/final SHA, actual commands/exits, scope, confirmed facts vs hypotheses/UNKNOWN, allocation/state delta and independent Windows rerun with -SettingsFile "D:\Java\.m2\settings.xml" for fixes. No claim of user acceptance. On local unresolved contract report and continue only independent task on verified clean baseline with no failed regression. On shared regression, unexpected remote advancement, dirty/conflicting state, unreliable infrastructure, STOP safely; no destructive cleanup. Timeboxes are ceilings; do not expand scope or spend freed time on unscheduled tasks. Stop after CTRL-090. All Git/GitHub operations within these boundaries approved.

---

## MESSAGE CTRL-079

**Status:** GATE ACCEPTED / READ-ONLY — initial unexplained process exit retained as evidence; subsequent CTRL-079A full matrix 49/49 child and runner exits 0, architect accepts residual risk. Original cause UNKNOWN, not a claimed fix.
**Timebox:** 60 minutes
**Reply ID:** CTRL-079
**Task:** VERIFY — full supported baseline regression

READ-ONLY. Enumerate actual ParameterSetName switches and mappings from current Test-LegacyGuiSmoke.ps1, deduplicate parameter attributes and distinguish default invocation. Run all supported smoke modes within timebox, prioritizing every accepted fix CTRL-049…078 (some share modes), table/edit/binding/form, numeric/date, navigation/readOnly/choice/validation/LOV and singleton lifecycle. Include default if supported. Run Maven -Pbuild compile, PowerShell parse and git diff --check. Record exact mode, probe ID, exit, runtime, elapsed, failures and exclusions. Do not enable unresolved CTRL-014/034 or other explicitly unsupported scenarios; if existing mode contains unresolved case, report exact reason. Never silently skip or weaken failing tests. Distinguish infrastructure failure from production failure with evidence. Any unexplained supported regression blocks production changes in subsequent tasks; STOP and report remaining NOT_RUN. No test or production edits, no claim of independent user acceptance.

---

## MESSAGE CTRL-080

**Status:** DONE / ARCHITECT APPROVED / USER ACCEPTED — 85a4b1f8aeef5558e32430da1f6e2c7061f42cb8
**Timebox:** 60 minutes
**Reply ID:** CTRL-080
**Task:** AUDIT → FIX — TableCell converter rejection / edit lifecycle

CTRL-075 guarded synthetic cancel commit against original row identity before and after conversion; converter rejection on that route remains untested. Read actual JInvTextFieldTableCell and existing TableEditRowIdentity/TableEditing/TableCancel/EditorReentry probes. Shown Stage with actual adapter and stable distinct rows, controlled converter throws/rejects input, positive valid same-row mouse-armed cancellation commit, ordinary F8/ENTER, Escape, row invalidation and equal-valued replacement. Observe exceptions, actual model writes, row/cursor/editor state, handler registration, row reference and reentry after failure. Separate expected validation rejection from unintended exception or abandoned edit lifecycle. If deterministic defect and clear established contract, only JInvTextFieldTableCell production; smallest cleanup/ordering fix preserving original row guard and draft/validation semantics, no automatic commit after rejected conversion, no silent swallowed errors or new draft recovery policy. No additional idle-cell row retention or eager allocation. Otherwise READ-ONLY/BLOCKED. New or extended focused probe plus TableEditRowIdentity, TableEditing, TableCancel, EditorReentry, DataBinding, MoneyDecimal and relevant validation tests.

---

## MESSAGE CTRL-081

**Status:** DONE / ARCHITECT APPROVED / USER ACCEPTED — 3976625111c448f00f07837ca987da7da9a0a1d7
**Timebox:** 60 minutes
**Reply ID:** CTRL-081
**Task:** AUDIT → FIX — DSFXAdapter search temporary row listener

CTRL-067 source candidate: search registers temporary row listener and removes it on straight-line completion; stale result policy unresolved. Inspect current actual find/search implementation and ProgressTaskExecutor boundary. Recover BulkLifecycleAudit if available. Controlled in-memory Dataset with distinguishable independent listener, real search positive hit/miss, deterministic exception after successful listener registration, cancellation if reachable, repeated searches. Count exact add/remove and notifications; preserve original exception/error dispatch, caller result semantics and independent listeners. If confirmed missing removal on exceptional exit, only DSFXAdapter production, exact owned listener cleanup in proper finally around registration lifetime; avoid masking original exception, do not remove caller listeners or change search result delivery/cancellation/SQL. No new eager fields/objects; no blanket listener clearing. GREEN no retained owned listener after failure and normal paths intact. New focused search-listener probe or existing extension. Regressions DataBinding, TableAdapterBulkClose, TableAdapterQueuedClose, TableAdapterClose and actual search-related modes. If no reproducible exception leak, READ-ONLY with evidence.

---

## MESSAGE CTRL-082

**Status:** DONE / READ-ONLY / ARCHITECT REVIEWED — worker NPE reproduced
**Timebox:** 60 minutes
**Reply ID:** CTRL-082
**Task:** AUDIT — bulk worker close during blocking fetch

READ-ONLY. CTRL-076 guarded UI delivery and listener restoration but current rowLoader still accesses mutable getDataSet()/dataSet in worker fetch loop. Verify actual source and supported owner threading semantics, ProgressTaskExecutor nested FX loop, cancellation and error dispatch. Use controlled blocking in-memory swap with latches/barriers, not arbitrary sleeps or real SQL writes; close before initial work, during first swap, between swaps, then release; normal, cancellation/error controls if available. Count fetch attempts, progress callbacks, stale errors, navigation registration, selection/cursor/model changes, worker termination and original exception. Distinguish already accepted work from subsequent obsolete access, report memory visibility/ownership only as source evidence unless tested. No cancellation/rollback guarantee, no changes to worker scheduling or ProgressTaskExecutor. Propose minimum scoped fix or UNKNOWN; avoid expanding CTRL-076 contract by assumption.

---

## MESSAGE CTRL-083

**Status:** BLOCKED / READ-ONLY / ARCHITECT REVIEWED — currency stale writes reproduced; superseded by expanded CTRL-091 scope
**Timebox:** 45 minutes
**Reply ID:** CTRL-083
**Task:** AUDIT → FIX — specialized TextFieldCUR delayed LOV completion

CTRL-078 excluded specialized TextFieldCUR override. Locate actual class and caller paths, inspect override contract and compare accepted TextField/TextArea stale completion policy. Real controlled delayed provider, shown Stage, A→B/null/ABA, repeated same provider, overlapping current requests, accepted/cancelled results; actual supported direct and user routes, model writes, TAB/focus and any specialized currency semantics. If reproducible stale acceptance or wrong update and existing contract equivalent to CTRL-078, only actual specialized TextFieldCUR class production; use existing lifetime state/accessors where possible without public API change or adding eager objects; preserve specialized current provider conversion/callback behavior. No generic provider API, direct lovProperty or reattached-notification redesign; if base private state makes safe isolated fix impossible, report BLOCKED with proposed minimal contract, no base edit. Focused test plus TextFieldLovDelayedCompletion, TextAreaLovDelayedCompletion, ExternalLovReadOnly and relevant numeric/currency modes.

---

## MESSAGE CTRL-084

**Status:** DONE / READ-ONLY / ARCHITECT REVIEWED — ownership contract UNKNOWN
**Timebox:** 75 minutes
**Reply ID:** CTRL-084
**Task:** AUDIT — full launched form Binder ownership on normal close

READ-ONLY. Resolve CTRL-070 limitation: prior Stage.hide fixture was not actual controller launcher. Build ignored real SDI FXML/controller fixture through FXFormLauncher, externally retained FXEntity and at least one real converter-bound text control. Verify actual bindControls, two-way writes, close request/veto, WINDOW_HIDDEN and closeResources/TaskContext completion gates; retain old control and external model deliberately, then mutate each direction and reopen fresh form with same model. Count model/control changes, callbacks, old/new bindings and resource cleanup. Test explicit owned converter unlink with Object/Object overload separately, not as production policy. Establish whether supported real callers reuse external entities and whether cleanup is contractually expected. No global Binder rollback, no automatic unbind change, no heap leak conclusion from intentional retention. Report actual launcher coverage and minimal next fix if proven.

---

## MESSAGE CTRL-085

**Status:** DONE / READ-ONLY / ARCHITECT REVIEWED — ownership contract UNKNOWN
**Timebox:** 60 minutes
**Reply ID:** CTRL-085
**Task:** AUDIT — initialization failure after real model bind

READ-ONLY. Extend CTRL-071 real launcher test which had no actual entity fields. Reuse CTRL-084 fixture if available, inject failure immediately after successful real converter binding; externally retained model and old control, normal successful launch positive control, exact failure/callback/close-resource/hidden order, reopen fresh form, check old versus new writes in both directions and singleton registration. Test veto/failure interaction only if supported, use deterministic WINDOW_HIDDEN and cleanup gates, not sleep or early assertions mistaken for final result. Distinguish error callback from resource teardown and model ownership. No rollback/transaction/MDI/WAIT policy edits, no changes to Binder/controller production. Report whether actual post-failure binding remains and whether supported caller reuse makes it a defect; propose precise smallest follow-up.

---

## MESSAGE CTRL-086

**Status:** NO_CHANGE / ARCHITECT REVIEWED — equivalence failed
**Timebox:** 60 minutes
**Reply ID:** CTRL-086
**Task:** PERF — reduce redundant form control traversal if equivalent

CTRL-074 measured Controls.getControlList flat traversal; architect verified AbstractBaseController.bindControls scans IJInvControl then initLabels then getBindingFilter scan, and initLabels itself scans labels. Inspect actual traversal exclusions, overrideable getBindingFilter/initLabels/setLabel and supported subclass graph mutations. Benchmark representative real nested FXML/form or equivalent shown graph; distinguish full initialization from traversal-only, same fixtures and warm-up, at least 5 warmup/9 measurement batches where practical, exact identity/order/filter results and FX-thread allocated bytes + time ranges. Candidate: eliminate one redundant traversal or share only local temporary results IF proven equivalent including custom predicates, label-induced mutations, traversal order/exclusions, non-IJInvControl binding targets and no-entity path. Do not globally cache control lists or change public API. Only AbstractBaseController and/or Controls if strictly necessary, one narrow optimization with explicit file scope and correctness fixture. If equivalent optimization cannot be proven or measurable benefit is absent/noisy, READ-ONLY/NO_CHANGE. No implementation based on flat benchmark alone; preserve init order and all caller semantics. If fix, tracked correctness smoke, before/after same workload and relevant NodeTraversal, DataBinding, FormLifecycle, FormCancel, singleton, ControlsFollowup tests; report median/range and allocated bytes, not retained heap.

---

## MESSAGE CTRL-087

**Status:** NO_CHANGE / ARCHITECT REVIEWED — equivalence failed
**Timebox:** 60 minutes
**Reply ID:** CTRL-087
**Task:** PERF — label matching complexity in initLabels

Architect source confirmed each eligible control searches listLabel via stream.filter/findFirst, candidate O(C×L) worst-case. Inspect actual JInvLabel.getLinkFieldName/getLabelFor and IJInvControl.setLabel, overrides and order. Create representative nested fixture with unmatched/matched labels, duplicate field names, multiple free labels with same key, pre-associated labels, already-labeled controls, null/empty names and user-defined setter behavior; exact selected label identity, order and bindings before/after. Measure varying C and L, allocations/time with same warmed fixture and reset state outside measurement; separate matching cost from FX node construction/CSS. Consider local lookup/index of eligible labels preserving first currently unassociated match, rechecking eligibility and mutation behavior; no persistent cache, no per-control field. Only AbstractBaseController.initLabels production scope. If graph/label changes during setters prevent equivalent indexing, NO_CHANGE/READ-ONLY. Optimize only with demonstrated correctness and meaningful measured improvement; report small-form overhead/memory tradeoff and duplicate behavior. Tracked correctness probe and relevant NodeTraversal, DataBinding, FormLifecycle, ControlsFollowup and CTRL-086 tests if implemented. No broad traversal refactor.

---

## MESSAGE CTRL-088

**Status:** DONE / READ-ONLY / ARCHITECT REVIEWED
**Timebox:** 60 minutes
**Reply ID:** CTRL-088
**Task:** PERF AUDIT — table cell/editor allocation and reuse

READ-ONLY. CTRL-074 identified lazy editor construction, per-edit filters and markColumn value factory SimpleBooleanProperty allocations. Inspect current DSFXAdapter, renderers, StubObservableValue/StubCallbackValue sharing and mark renderer behavior; first check correctness of row identity/notification and reuse, not generic caching. Real shown virtualized table, vary loaded row count independently of visible rows, normal scroll/refresh and repeated edit/cancel/reentry; count cell factory, value factory, editor creation, handler registrations and mark value accesses using safe fixture instrumentation. Measure warmed FX-thread allocated bytes/time where practical, baseline exact selection/mark/model semantics; compare mark-enabled/disabled and repeated edits. No property cache per row, no production edits, no change to mark semantics. Distinguish allocation from retained memory, queue from layout. Rank measured hotspots and minimal next task, report limits.

---

## MESSAGE CTRL-089

**Status:** DONE / READ-ONLY / ARCHITECT REVIEWED
**Timebox:** 60 minutes
**Reply ID:** CTRL-089
**Task:** PERF AUDIT — end-to-end DataSet refresh and FX layout

READ-ONLY. CTRL-074 measured AFTER EXECUTE dispatch/queue drain ~45–49 µs, 201.2 allocated B/call, not full layout/paint. Use actual shown JInvTable/adapter, preloaded rows and genuine notifications, vary loaded N and visible row count independently. Compare single refresh vs burst of 3/10/100, count actual refresh calls, selection/cursor order, cell factory/value callbacks, pulses and elapsed to completed layout with deterministic observable barriers/AnimationTimer where valid, not arbitrary sleeps. Warm repeat, report FX-thread allocations and median/ranges, separate setup and SQL (not measured). Investigate whether intermediate refresh effects are observable/required by active editing, selection and validation; no coalescing or production edits. Propose optimization only if measured impact and compatible event ordering established. No claim of paint completion if only layout observed.

---

## MESSAGE CTRL-090

**Status:** DONE / READ-ONLY / ARCHITECT REVIEWED — 50/50 agent GREEN
**Timebox:** 60 minutes
**Reply ID:** CTRL-090
**Task:** VERIFY — final regression, performance evidence and readiness triage

READ-ONLY on final verified HEAD. Run supported modes affected by any batch fixes plus all CTRL-075…078 and core DataBinding/TableEditing/TableCancel/EditorReentry/TableAdapterClose/TableAdapterPagingClose, LOV/readOnly/lifecycle, NodeTraversal/Forms and new tracked modes; prioritize full CTRL-079 battery if time permits. Exact exit matrix, Maven compile, PowerShell parse, diff check. Verify commit chain/scope and clean remote, no hidden modifications. Summarize all 12 tasks DONE/NO_CHANGE/READ-ONLY/BLOCKED/PARTIAL/NOT_RUN with evidence, production SHA and independent Windows rerun commands for fixes, performance before/after measured where available, memory/state delta, no invented savings. Rank next 3–5 tasks by confirmed data risk then measured cost. Separate production readiness from local smoke success; list actual corporate SQL/forms/native input/MDI/WAIT and open contracts needing user environment. Stop after CTRL-090, no self-assignment.

---

## MESSAGE CTRL-079A

**Status:** DONE / READ-ONLY / ARCHITECT REVIEWED — original failure NOT_REPRODUCED; 30 completed focused PASS markers, 28 recorded JVM exits 0, 7 recorded focused runner exits 0, full instrumented gate 49/49 child and runner exits 0. Original cause UNKNOWN. No production or tracked test edits.
**Repo:** ssu95a/fore25
**Production branch:** migration/java25-correctness
**Mailbox branch:** agent/codex-mailbox
**Baseline remote HEAD:** 70b139ed54d00c46b4c2ae597535d82ddbe5a2ab
**Reply ID:** CTRL-079A
**Timebox:** 45 minutes, ceiling.
**Dependency:** Reply CTRL-079 at f669409b06028d734d61790c17e9bc099a997224.

### Evidence and goal
CTRL-079 ran all 48 named supported runner modes plus default: all 49 printed PASS; initial SceneKeyLookup runner exit 1 after CTRL-024 PASS, 48 other exits 0. Subsequent fresh official runner and direct JVM reruns both PASS/exit 0. Original child exit code not captured, no observed assertion failure or hs_err. Cause UNKNOWN. Architect reviewed SceneKeyLookupSmoke: PASS printed before Platform.exit() in finally. Runner invokes java and throws generic message on LASTEXITCODE != 0. These facts do not prove shutdown error, native crash or runner error. Goal is characterize reproducibility and separate assertion result, actual child-process exit, and runner exit; do not conceal an unexplained failure.

### Execution
1. Verify clean tracked checkout and fresh production/mailbox refs; recover original CTRL-079 output, java.args, dependencies and timestamps if still available. Inspect exact tracked SceneKeyLookupSmoke and runner at baseline. Record OS build, PowerShell version, actual java/javac path and versions, FX version, Maven/settings/offline environment, relevant JAVA_TOOL_OPTIONS/JDK_JAVA_OPTIONS without exposing secrets. No dependency upgrades or environment-wide setting changes.
2. Use ignored diagnostic directory under target/ctrl-079a. Run 20–30 fresh JVMs total, targeting 30 if time permits, with unchanged original tracked assertions and original runner classpath/arguments. Include both official runner invocation and direct JVM invocation with generated java.args (e.g. 10 official and 20 direct). Sequential, no concurrent GUI launches. Capture run ID, PID when available, start/end timestamps, stdout and stderr separately, PASS marker, immediate native child exit code (signed decimal and unsigned hex when relevant), outer PowerShell exit, timeout/termination and elapsed time. A diagnostic copy of runner in ignored storage or equivalent temporary wrapper may log immediate child exit before throwing; preserve actual invocation semantics, root resolution, argument files and assertions. Do not mislabel instrumented runner as unchanged official runner. Use per-run JVM error-file location if appropriate, disclose diagnostic flags. On timeout collect thread/process evidence where possible, terminate only owned diagnostic process if needed, report as timeout not PASS. Preserve every failure, no retry-until-green.
3. Inspect Windows Application event log around original/reproduced failure timestamps (Application Error/WER when available), JVM hs_err/error output and relevant process diagnostics. Correlate PID/time, report access-denied/missing evidence as UNKNOWN; no system configuration or dump-policy changes. Avoid reporting sensitive unrelated event log data. If failure recurs, capture exact native exit, phase and stack/crash evidence, compare runner/direct and identify smallest next discriminating experiment. If no failure, explicitly report NOT_REPRODUCED, not root cause fixed. Do not infer cause from PASS marker or lack of hs_err.
4. Re-run full supported baseline gate: all 48 named modes plus default, same exclusions as CTRL-079, exact matrix and original assertions, record actual runner exits and child exit evidence for failures. Maven -Pbuild compile, PowerShell parse, git diff --check, verify clean unchanged production HEAD. Reserve time for full battery; report incomplete coverage explicitly if ceiling reached. A PASS marker alone is insufficient. If any nonzero/timeout, preserve evidence and mark gate BLOCKED; if all 49 exit 0 and focused repetitions clean, recommend architect decision whether to accept residual one-off UNKNOWN risk. Do not declare original cause resolved merely from successful repetitions.
5. Publish exactly one Reply CTRL-079A in docs/codex/outbox.md on agent/codex-mailbox: actual repetition matrix (counts and per-run exit details or concise table), original/reproduced failure evidence, full gate exit matrix, commands, diagnostics paths, confirmed vs hypothesis/UNKNOWN, recommendation and unchanged SHA. STOP. This diagnostic session has ended. The subsequent explicit architect release below authorizes CTRL-080…090 in a new session.

### Boundaries
READ-ONLY for production and tracked tests/runner. Ignored diagnostic scripts/logs only. No System.exit(0) workaround, no assertion suppression, no changing shutdown semantics, no retry masking, no speculative SceneKeyManager or JavaFX fixes. No new reflective access beyond original tracked test. Main and JInvCommon read-only, no force push/destructive commands. Only mailbox reply write authorized. Timebox 45 minutes. All necessary Git/GitHub operations within scope approved.

---

## RELEASE CTRL-080…CTRL-090 — resume remaining eleven tasks

**Status:** CLOSED / ARCHITECT REVIEWED — resumed session ended after CTRL-090.
**Repo:** ssu95a/fore25
**Production branch:** migration/java25-correctness
**Mailbox branch:** agent/codex-mailbox
**Baseline remote HEAD:** 70b139ed54d00c46b4c2ae597535d82ddbe5a2ab
**Remaining timebox:** 660 minutes (11 hours), individual original task limits unchanged.
**Evidence:** Reply CTRL-079A at e84e1110f4b37cef463ba2c8f30d319c8ecc7875.

### Gate decision and latest execution authorization
Architect accepts the baseline regression gate on the evidence of all 49 modes passing with actual child JVM and outer runner exits 0 in the instrumented repeat, plus 30 completed focused PASS results (28 recorded JVM exits and seven recorded outer runner exits all 0). The first incident remains UNKNOWN / NOT_REPRODUCED, not fixed or classified as a production defect. Preserve original failure, missing exit records and interrupted recorder attempt as documented. No changes to SceneKeyManager, shutdown semantics, assertions, or tracked runner are authorized by this release alone.

Resume CTRL-080 through CTRL-090 in exact order in one new session. Do not repeat CTRL-079 or CTRL-079A. All individual original scopes, regression requirements, measurement/equivalence gates, memory constraints and timeboxes remain in force. No intermediate confirmations needed for explicitly permitted changes. Before each task and reply verify clean tracked checkout, fresh remote refs, actual preceding verified HEAD. For each production fix deterministic RED then GREEN, isolated fast-forward commit; for PERF prove equivalence and measured benefit, otherwise NO_CHANGE. Read-only tasks remain read-only. Preserve all exits, not just PASS markers. Use existing ignored diagnostic recorder from CTRL-079A or equivalent faithful instrumentation where appropriate to retain immediate child exit, runner exit, separate output and timestamps; do not claim instrumented runner is unchanged official runner. No masking failures, retry-until-green or forced successful shutdown. If a similar nonzero exit or timeout recurs, collect precise diagnostics and stop under the existing gate rule; no automatic waiver of a new failure. Shared unexplained regressions, dirty/conflicting state or unreliable infrastructure also stop the batch. Task-local unknown contracts may be reported and independent work continued on a clean baseline as previously authorized. Main and JInvCommon read-only; no dependency/public API changes, destructive Git commands, force push or unscheduled tasks. All Git/GitHub operations within original approved scopes authorized. Publish one Reply per remaining task; final CTRL-090 summary includes original residual gate risk, final actual exit matrix, exact production SHAs and measured performance evidence. Stop after CTRL-090.

---

## BATCH CTRL-091…CTRL-097 — currency integrity, worker ownership and table performance

**Status:** CLOSED / ARCHITECT REVIEWED / USER ACCEPTED FIXES — all seven replies received, final agent regression 53/53 JVM and runner exits 0; user reported ALL FIVE TESTS PASSED on independent acceptance battery covering CTRL-080/081/091/092/093.
**Repo:** ssu95a/fore25
**Production branch:** migration/java25-correctness
**Mailbox branch:** agent/codex-mailbox
**Baseline remote HEAD:** 3976625111c448f00f07837ca987da7da9a0a1d7
**Budget:** 495 minutes (90+75+60+60+60+90+60), ceiling.
**Dependency:** CTRL-090 final 50/50 actual JVM/runner exits 0; CTRL-080/081 architect approved; subsequently user accepted with five-mode battery.

### Latest authorization
Execute only CTRL-091…097 in exact order, autonomously. First correct proven currency data corruption, then actual asynchronous worker lifetime; performance tasks are research only. Each task starts from preceding verified clean HEAD. Verify fresh mailbox and production refs before task/reply. All Git/GitHub operations within explicit scopes approved. Main and JInvCommon read-only, no other repo writes, force push/destructive reset, dependency/public API changes or unrelated refactoring. Only CTRL-091 expressly authorizes minimal non-public base-class helper contract; no broad API expansion. No CTRL-014/034/CAL-TIME-001, speculative Binder rollback, keyboard reservation or external/inner LOV precedence changes. Ordinary GUI FX thread; actual worker paths require explicit visibility/admission reasoning. No locks across blocking SQL or nested FX loops, no corporate DB writes, no claim of SQL cancellation or rollback.

FIX/AUDIT→FIX: deterministic tracked RED on unchanged production, GREEN same assertions, real shown Stage and controlled in-memory dataset/provider, positive controls, exact model/callback/row identity/error counts. Official focused plus three fresh JVMs, relevant regressions, Maven -Pbuild compile, PowerShell parse, diff check; actual child JVM and outer runner exits recorded, not just PASS. No arbitrary sleeps, JavaFX internal reflection, test-only production hooks, weakening assertions or forced success shutdown. One isolated validated fast-forward commit per implemented task and verified remote SHA. No incomplete pushes. No eager per-control objects, minimize persistent state and allocations; actual async lifecycle state may be added only if needed and justified. Russian Java comments. READ-ONLY/PERF AUDIT tasks allow ignored temporary probes only, no tracked production/test changes. Measure with warmed repeat identical fixtures, document units/ranges and limitations, no retained heap estimates from allocation counters.

Task-local unresolved contract: report BLOCKED/PARTIAL/NO_CHANGE and proceed only to independent task with clean baseline and no regression failure. Shared unexplained exit/timeout, regression, remote conflict, dirty state or unreliable infrastructure: STOP safely and report remaining NOT_RUN. Preserve evidence; original CTRL-079 incident UNKNOWN / NOT_REPRODUCED, not blanket waiver for new failures. Exactly one Reply per task in docs/codex/outbox.md on agent/codex-mailbox. Include baseline/final SHA, commands/exits, confirmed vs UNKNOWN, scope, memory/state delta, Windows rerun with -SettingsFile "D:\Java\.m2\settings.xml". User acceptance separate. Timeboxes ceilings, no scope expansion. Stop after CTRL-097, no self-assignment.

---

## MESSAGE CTRL-091

**Status:** DONE / ARCHITECT APPROVED / USER ACCEPTED — de95eefc66700425f46720f43119c74298adf0d4
**Timebox:** 90 minutes
**Reply ID:** CTRL-091
**Task:** FIX / CRITICAL — Currency LOV completion ownership

CTRL-083 reproduced nine wrong currency writes on JInvTextFieldCUR, inherited by JInvCURTextField, direct/F9/button × A→B/null/ABA. Architect inspected actual override: reads getLOV().getValue() on callback, getLOV falls back to lazy internal JInvEntityLov when base property null. Base JInvTextField CTRL-078 has private lazy listener revision; final unbindLOV. Explicitly expanded scope: ONLY JInvTextField.java and JInvTextFieldCUR.java production, one isolated commit. Permit minimal non-public final reusable base helper(s) for snapshot/check of existing attachment lifetime (package-private preferred for same-package subclass if sufficient; protected only if necessary and justified). No public API change, no duplicated generation, no extra field on ordinary controls, no eager internal provider creation or callback wrapper beyond existing request allocation if avoidable. Preserve base behavior and fallback provider semantics: capture actual effective provider and base attachment epoch at request, verify epoch and provider before accepting/copying; handle default internal provider→explicit→default ABA and inherited unbind without evaluating unnecessary SQL-backed fallback on stale path. Preserve skipFilter, getText filtering, current accepted currency conversion/updateProperty order, original source, cancellation callback once, overlapping current requests and same-provider setter. Stale accepted completion becomes false without text/curValue writes or accepted navigation. RED on unchanged baseline using recovered CurrencyLovAudit or tracked equivalent, all nine stale cases, positive current/cancel routes, bound setter failures, fallback identity without actual SQL, programmatic updates, readOnly; test default internal provider path only with safe fixture, disclose genuine SQL LOV untested. Do not replace override with super.showLOV if popup/filter/error semantics differ. New focused -CurrencyLovDelayedCompletion; regressions TextFieldLovDelayedCompletion, TextAreaLovDelayedCompletion, ExternalLovReadOnly, LovBindingInterleaving, BoundLovSetter, LovIntegration, numeric/currency existing modes. No provider value event attribution redesign or direct writable property contract changes.

---

## MESSAGE CTRL-092

**Status:** DONE / ARCHITECT APPROVED / USER ACCEPTED — 8914be08e3399bed7fabbb55e016412a03da9e72
**Timebox:** 75 minutes
**Reply ID:** CTRL-092
**Task:** FIX — bulk worker attachment lifetime

CTRL-082 reproduced three obsolete NPE error dialogs: close before first swap (paused initial count), during first or second swap returning true. CTRL-076/077 UI delivery guards already work. Actual rowLoader still dereferences mutable dataSet/getDataSet across worker/FX close. Only DSFXAdapter production unless separate authorization; establish source visibility and supported FX close/worker ownership. Reuse existing captured origin for worker operations and a minimal explicit safely published retirement state for actual async operation/attachment if necessary (volatile/atomic or equivalent with defined happens-before; no unsynchronized equality polling as correctness proof). Define precise boundary: a fetch already admitted before retirement may return; no subsequent obsolete work admitted after retirement is observed, no post-retirement reads of cleared/replaced adapter field, no stale progress/state/selection updates. Do not hold locks across blocking fetch or FX nested loop, do not promise cancellation of in-flight SQL/rollback. Deterministic latch fixtures for close before worker entry, paused initial count, during swap(s), active success/cancel/error, repeat close, late independent listeners; record exact admission/fetch counts, errors, state/cursor/selection and cleanup. Preserve genuine active failure and cancellation semantics, do not swallow unrelated exception merely because closed; stale error delivery separately CTRL-094. Minimize per-adapter/per-operation state, justify worker visibility and race boundary; preserve current paging and no same-table rebind support. New focused -TableAdapterBulkWorkerClose if fix; regressions TableAdapterBulkClose, TableAdapterPagingClose, TableAdapterQueuedClose, TableAdapterClose, TableAdapterSearchListener, DataBinding, TableEditing. No ProgressTaskExecutor/Service edit here; if impossible in scope report BLOCKED.

---

## MESSAGE CTRL-093

**Status:** DONE / ARCHITECT APPROVED / USER ACCEPTED — 60685aad5e7f564dffdf32e3327754ff333b5fc9
**Timebox:** 60 minutes
**Reply ID:** CTRL-093
**Task:** AUDIT → FIX — search worker attachment lifetime

CTRL-067 observed post-close result callback; CTRL-081 fixed exact temporary listener removal on exceptions, current rowFinder still uses this.dataSet for swap/count and captures adapter at worker entry. Reuse CTRL-092 proven worker ownership mechanism if suitable; no parallel redundant infrastructure. Establish actual source and deterministic RED with shown table, controlled search hit/miss, close before entry/registration/during swap/after registration, active exception/cancel, external observer. Only DSFXAdapter production if confirmed. Capture origin at launch, safely stop admitting obsolete work on retirement as defined by CTRL-092, exact removal from original Dataset in finally preserving primary failure/suppressed cleanup; no additional search predicate calls after observed retirement where owned listener can guard safely. Preserve legitimate active result/cancel/error behavior. Do NOT silently suppress public Consumer<Boolean> after close or convert success into false unless supported caller contract established; report stale result and no-consumer alert separately for CTRL-094. No blocking locks across fetch, no SQL cancellation/rollback guarantees, no rebind contract changes. New focused -TableAdapterSearchWorkerClose if implemented, existing TableAdapterSearchListener, TableAdapterBulkWorkerClose if available, TableAdapterBulkClose, TableAdapterQueuedClose, DataBinding, TableAdapterClose. If no safe isolated contract, READ-ONLY/BLOCKED.

---

## MESSAGE CTRL-094

**Status:** DONE / READ-ONLY / ARCHITECT REVIEWED — stale table-owned miss alert and detached-scene NPE reproduced; fixed in CTRL-098, user accepted
**Timebox:** 60 minutes
**Reply ID:** CTRL-094
**Task:** AUDIT — stale progress/error/result delivery ownership

Strict READ-ONLY initially. Inspect actual ProgressTaskExecutor/ProgressService and DSFXAdapter call sites at current HEAD; distinguish progress window teardown from business callback and error notification. Reuse CTRL-082/092/093 controlled fixtures. Matrix active success/failure/cancel, close before FX error/result dispatch, closed owner but external caller still active, no-consumer search miss alert, real exceptions occurring before versus after close, old vs fresh form. Record actual callback order, error identity, progress WINDOW_HIDDEN, task terminal states, navigation/model effects, ownership and caller evidence. Do not suppress genuine SQL failures, alter cancellation/rollback, change public Consumer contract or generic progress manager. If remaining defect is deterministic, propose precise separately scoped fix with caller contract and evidence; no production/ tracked test edit in this task. Report whether CTRL-092/093 already eliminated obsolete NPE paths; avoid counting those twice.

---

## MESSAGE CTRL-095

**Status:** DONE / READ-ONLY / ARCHITECT REVIEWED — marker property allocation low priority; no optimization authorized
**Timebox:** 60 minutes
**Reply ID:** CTRL-095
**Task:** PERF AUDIT — isolate mark value factory allocation

READ-ONLY. CTRL-088 measured 1.296 vs 3.148 MB FX allocations at 2000 rows/19 visible with mark disabled/enabled, includes whole cell/checkbox/CSS/layout. Current DSFXAdapter markColumn returns new SimpleBooleanProperty per IMarkable read. Isolate value-factory-only cost from full table construction, with warmed identical workloads, count allocations and independent observable identity/notifications across rows, retained references to A/B returned properties, sorting/selection/mark/unmark/mark-all where in-memory supported, actual JInvTableCellMark rendering and row pseudoclass. Compare candidate stateless/snapshot alternatives ONLY in ignored probes, no shared mutable proxy unless independent observers proven equivalent, no persistent per-row cache or changes to model. Quantify absolute contribution to full refresh allocation; report median/ranges, FX thread only, no retained heap claim. Rank whether worth implementing, exact scope and contract tests. No production edits.

---

## MESSAGE CTRL-096

**Status:** NO_CHANGE / ARCHITECT REVIEWED — full refresh alternatives fail equivalence
**Timebox:** 90 minutes
**Reply ID:** CTRL-096
**Task:** PERF AUDIT — avoid refresh-induced cell reconstruction without changing semantics

READ-ONLY. CTRL-089 measured one layout pulse/reconstruction for same-turn burst despite N observable refresh deliveries; 9/19 recreated cells, ~0.6/1.27 MB FX allocations. Read current JInvTable/DSFXAdapter refresh/refreshRow and public JavaFX 25 API implementation available locally, no private VirtualFlow/Skin internals or unsupported access. Distinguish explicit full refresh, row updates, mark changes, AFTER EXECUTE and new data, preserve observable order of active navigation/refresh and editing/cancel. Compare minimal public targeted invalidation/update mechanisms in ignored fixtures (no production patch) with exact cell/value factory counts, row identity, model write/validation counts, marks, selection/cursor, active draft/commit/cancel/invalid conversion and same-index replacements. Vary loaded N, visible rows, columns and marked mode; warmed repeat and elapsed-to-layout, allocation median/ranges, not paint/retained heap. Do not coalesce events or cache row values in production, avoid relying on mutable per-column proxy without equivalence. If no equivalent public mechanism, NO_CHANGE and explain why. Produce recommended next experiment or narrow implementation proposal only with demonstrated compatibility and measured benefit.

---

## MESSAGE CTRL-097

**Status:** DONE / READ-ONLY / ARCHITECT REVIEWED — agent final 53/53 GREEN; independent five-mode acceptance passed
**Timebox:** 60 minutes
**Reply ID:** CTRL-097
**Task:** VERIFY — final regression and release readiness triage

READ-ONLY. Enumerate actual supported runner modes at final HEAD (previous 50 plus new modes). Full battery within timebox, prioritize all new fixes and accepted CTRL-075…081, existing table/LOV/readOnly/numeric/form/data binding tests. Capture actual JVM and outer exit codes separately using faithful ignored diagnostic recorder from CTRL-079A/090, separate output/timestamps, preserve any failed/timeout attempt. No opt-in Robot or unresolved CTRL-014/034/CAL-TIME-001. Maven -Pbuild compile, PowerShell parse, diff check, verify remote chain and per-task scope. Any unexplained nonzero stops, not retry-until-green. Final summary all seven statuses, exact SHAs, RED/GREEN evidence, allocation/state deltas, measured performance audit limits, independent Windows reruns for implemented fixes, residual unknown contracts and original CTRL-079 incident. No claim of production readiness from local tests alone; corporate SQL/transaction behavior, native input, real forms/MDI/WAIT remain separately unverified. Rank next tasks by confirmed data integrity then measured benefit. STOP after CTRL-097.

---

## BATCH CTRL-098…CTRL-103 — final search delivery correction and targeted performance

**Status:** CLOSED / ARCHITECT REVIEWED / USER ACCEPTED FIX — all six replies received; CTRL-098 independently verified by user: CTRL-098 PASS, exit 0; final agent regression 54/54 child and runner exits 0.
**Repo:** ssu95a/fore25
**Production branch:** migration/java25-correctness
**Mailbox branch:** agent/codex-mailbox
**Baseline remote HEAD:** 60685aad5e7f564dffdf32e3327754ff333b5fc9
**Budget:** 450 minutes (7 h 30 min), ceiling.
**Dependency:** accepted fixes CTRL-080/081/091/092/093; user ALL FIVE TESTS PASSED; CTRL-097 agent 53/53 JVM and runner exits 0. Acceptance mailbox 5c2bfa4e7b7f41a28e921023f29c7d67fc5e53d2.

### Latest execution contract
Execute only CTRL-098…103 in exact order, autonomously; no intermediate approval for explicitly permitted scope. Verify clean local tracked checkout and fresh production/mailbox refs before each task/reply. Each starts from preceding verified HEAD. Main and JInvCommon read-only; no other repo writes, destructive Git, force push, dependency/public API changes, unrelated refactor, unapproved Binder/LOV/keyboard contracts or CTRL-014/034/CAL-TIME-001. No actual corporate DB writes or SQL cancellation/rollback guarantees. Ordinary GUI on FX thread; actual workers require explicit admission/visibility reasoning. No locks spanning blocking work/nested FX loops.

Only CTRL-098 is mandatory scoped correctness implementation; CTRL-100 conditional measured optimization, not obligation to produce a commit. All other tasks READ-ONLY, ignored probes/logs allowed, no tracked production/test edits. Fix: deterministic tracked RED on unchanged baseline, same assertions GREEN, positive controls, real shown fixtures and exact writes/identity/errors, official runner plus three fresh JVMs, relevant regressions, Maven -Pbuild compile, PowerShell parse, diff check. PERF: prove behavioral equivalence before production edit, actual meaningful absolute improvement on identical warmed workloads, report ranges, no false RED for pure optimization; if not proven NO_CHANGE. No speculative caching, eager per-control/per-row objects, private JavaFX skin/VirtualFlow access, arbitrary sleeps, test-only production hooks, assertion weakening or forced JVM success. Russian Java comments. Minimize state and allocations; distinguish allocations/retained heap, post-layout/paint. One isolated validated fast-forward production commit per implemented task; verify remote SHA, no incomplete push.

Record actual child and runner exits, preserve failed attempts and diagnostic evidence. Original CTRL-079 exit UNKNOWN / NOT_REPRODUCED, accepted residual risk, not a waiver of new failures. Shared unexplained regression/nonzero/timeout, dirty/conflicting checkout, unexpected remote advancement or unreliable infrastructure: STOP safely, remaining NOT_RUN. Task-local unresolved contract or no measured benefit: report BLOCKED/PARTIAL/NO_CHANGE and proceed only independent task on clean baseline. Exactly one Reply per task in docs/codex/outbox.md on agent/codex-mailbox. Include scope, baseline/final SHA, commands/exits, confirmed facts vs UNKNOWN, memory/state delta and independent Windows rerun with -SettingsFile "D:\Java\.m2\settings.xml" for fixes. No claim of user acceptance. Timeboxes ceilings, no expansion or self-assignment. STOP after CTRL-103. All Git/GitHub operations within explicit scopes approved.

---

## MESSAGE CTRL-098

**Status:** DONE / ARCHITECT APPROVED / USER ACCEPTED — df95a931230b57e87e7bef4bfba6267c3e9a395d; user CTRL-098 PASS, exit 0
**Timebox:** 45 minutes
**Reply ID:** CTRL-098
**Task:** FIX — retired table-owned search miss alert

CTRL-094 confirmed old-owner RECORD_NOT_FOUND information dialog and detached-table FX NPE after search's admitted final swap returns false. Inspect current DSFXAdapter.rowFinder queued completion at baseline. Scope production ONLY DSFXAdapter. Guard only no-consumer miss alert at actual FX delivery with existing originating attachment/revision and valid scene/window ownership; consider old hidden window and reparented table separately, use launch-owner identity if needed without adding per-instance state. Recheck after any observable cancellation-state publication which may synchronously retire/detach the owner. Active shown owner miss still one correct alert. Completed independent Consumer<Boolean> still gets original true/false once even when old table retired; incomplete retired search no invented result. Genuine admitted worker exceptions retain original error identity and existing error dispatch regardless of retirement. Preserve cancellation semantics, listener finally, cursor/selection, fresh independent table. Deterministic RED/GREEN shown real table + worker using recovered ProgressDeliveryAudit or focused equivalent; retired still-shown owner, detached scene, closed/hidden owner, active miss/hit, external consumer, genuine error before/after close, no duplicate alerts/errors, actual FX uncaught exceptions collected. Distinguish no-scene from no-window. No generic progress/error-service changes, no SQL cancellation/rollback claims. New focused -TableAdapterSearchMissDelivery. Regressions TableAdapterSearchWorkerClose, TableAdapterSearchListener, TableAdapterBulkWorkerClose, TableAdapterQueuedClose, DataBinding, TableAdapterClose.

---

## MESSAGE CTRL-099

**Status:** DONE / READ-ONLY / ARCHITECT REVIEWED — deferred refreshRow stale list event reproduced; index vs identity contract unresolved
**Timebox:** 75 minutes
**Reply ID:** CTRL-099
**Task:** AUDIT — precise row-only update and deferred identity contract

READ-ONLY. CTRL-096 found same-index refreshRow can preserve cell objects but changes edit/list semantics vs full refresh. Inspect actual DSFXAdapter.refreshRow, rowOperation UPDATE, markAction, refreshCurrentRowFromDB, row list implementation and callers; inspect available actual JInvCommon contracts read-only, not guessed. Architect verified refreshRow queues getItems().set(index,getItems().get(index)) plus focus restore without retirement or captured row identity; markAction forwards event index. Test real shown table/in-memory fixture normal explicit refreshRow, mark notification and genuine row UPDATE separately; queued close/detach, replace/remove/permutation between enqueue/delivery, duplicate equals/different identities, same object current index, hidden row, active same/other row edit, F8/ENTER/Escape/rejected conversion. Count exact list changes, row identity, selection/focus/cursor, converter/commit/model writes, marker/pseudoclass, cell factory and FX errors. Distinguish index-based public contract from entity-based event ownership; don't assume changed occupant refresh is a data write. Establish one actual supported row-only operation currently causing redundant full reconstruction, or explicitly conclude none found. Propose scoped next fix/optimization, baseline contract, deterministic assertions and measurements; no tracked changes, no invented guarantee about corporate DB refresh. Binder external model ownership remains outside scope.

---

## MESSAGE CTRL-100

**Status:** NO_CHANGE / ARCHITECT REVIEWED — no qualifying row-only full refresh caller, alternatives fail equivalence
**Timebox:** 90 minutes
**Reply ID:** CTRL-100
**Task:** PERF — narrow row-only refresh optimization, conditional

Use CTRL-099 evidence, not automatic replacement of TableView.refresh. Authorize one isolated production optimization ONLY DSFXAdapter and if strictly necessary JInvTable for one confirmed row-only caller that currently performs unnecessary full reconstruction; preserve public refresh()/AFTER EXECUTE semantics and all listener order. If CTRL-099 finds no such caller or behavior equivalence cannot be proven, NO_CHANGE. A deterministic independently confirmed queued row-refresh retirement NPE/wrong-target side effect may instead be reported for separate correctness assignment, not silently bundled into PERF. Compare unchanged source vs candidate using same real shown fixture, stable identity and exact edit/mark/list/navigation/focus behavior including duplicate equal rows, permutation, active/rejected conversion, valid commits, offscreen changes and closed owner. No full list replacement, coalescing, per-row cached properties, shared mutable observables or private skin/VirtualFlow API. Public JavaFX 25 mechanism only; no extra eager per-control/per-row objects. Warm 5 batches / 9 measured where practical, vary loaded N 200/2000 and viewport 9/19 rows, 1/4 columns, mark off/on; record exact cell/value calls, FX allocated bytes, median/range elapsed to post-layout (not paint), small-case overhead. Require reproducible material allocation/time improvement and contract equivalence; no optimization for hypothetical hot path. Tracked correctness probe and runner mode only if implemented; existing TableEditRowIdentity, TableEditing, TableCancel, EditorReentry, DataBinding, TableAdapterClose, TableAdapterQueuedClose and relevant marker tests. Do not expand scope if unable.

---

## MESSAGE CTRL-101

**Status:** DONE / READ-ONLY / ARCHITECT REVIEWED — retired bulk refresh and detached error delivery NPE reproduced
**Timebox:** 90 minutes
**Reply ID:** CTRL-101
**Task:** PERF AUDIT — marker-only and bulk mark/unmark lifecycle

READ-ONLY. CTRL-095 isolated property snapshot cost 40 B/read (~760 B at 19 rows) negligible; do not optimize property identity/writability. Inspect actual DSFXAdapter.markRow/unMarkRow, markAction → refreshRow and markAll/unMarkAll → JInvParallelAction → queued full refresh/error, JInvTableCellMark, in-memory XXIDataSet event behavior. Single mark already targeted; don't claim it uses full refresh. Measure real supported mark/unmark and bulk mark/unmark separately (in-memory overrides only, no DB writes); savePrevMarkedRows, empty/big table guard, duplicate keys/equals, reordered rows, offscreen/visible marks, pseudoclass, selection/cursor/focus, active same/other row editor, callbacks/list notification order. Test close while parallel mark worker pending and after result queued, deterministic latches; identify stale dialog/refresh/worker behavior and genuine error routing, no new cancellation guarantees. Measure actual counts/FX allocation/time for visible and loaded sizes, distinguish mark-all logical data changes from visual rendering cost and per-row notification cost. Candidate public targeted marker invalidation only in ignored fixtures, preserve snapshot observable semantics, no per-row cache or private JavaFX internals. If bulk route cannot run offline with real contract, report exact limitation. Recommend narrow next task only with evidence; no production or tracked edits.

---

## MESSAGE CTRL-102

**Status:** DONE / READ-ONLY / ARCHITECT REVIEWED — synthetic form startup phase measurements; no retained heap claim
**Timebox:** 90 minutes
**Reply ID:** CTRL-102
**Task:** PERF AUDIT — representative form startup and retained state

READ-ONLY. Use actual FXFormLauncher/AbstractBaseController, shown SDI FXML fixture with realistic nested layout, fields/labels/table, real Binder/FXEntity and offline app seam where necessary; inspect actual repository form resources and select one representative or clearly label synthetic fixture. No corporate DB/session writes, no claims about production forms if unavailable. CTRL-086/087 rejected traversal sharing and label index due supported hook mutation; do not retry same non-equivalent changes. Measure full launch phases FXML load, controller initialization/bindControls/initLabels/configControls, CSS/layout/first observable post-layout, close/reopen; avoid attributing total to one phase, separate FX thread bytes/time, include warmups/ranges, sizes e.g. 20/100/400 fields only where practical. Use existing hooks/logging or ignored instrumentation with disclosed overhead, no tracked edits. Inspect allocation hotspots with available JFR or ThreadMXBean; distinguish sampled allocations vs exact counters, retained heap requires actual evidence (optional local heap histogram/weak-reference test with bounded GC caveat, not proof of absence). Identify actual persistent per-control/skin state, no speculative caching, no broad refactor. Check close/veto/reopen and ordinary ownership; intentionally retained external FXEntity binding from CTRL-084/085 is separate contract, do not infer leak or auto-unbind. Report top 3 hotspots by measured absolute cost, candidate scope, reproducible commands, limitations and smallest next experiment.

---

## MESSAGE CTRL-103

**Status:** DONE / READ-ONLY / ARCHITECT REVIEWED — 54/54 agent GREEN
**Timebox:** 60 minutes
**Reply ID:** CTRL-103
**Task:** VERIFY — production baseline regression and readiness

READ-ONLY. Enumerate actual supported smoke modes at final HEAD (53 previous + any new). Run full battery, prioritize CTRL-098 and any implemented CTRL-100, all CTRL-075…093 fixes and form/table/LOV/marker/numeric/navigation. Record actual child JVM and runner exits separately with faithful ignored diagnostic recorder, exact PASS matrix, timestamps, stdout/stderr and timeouts. No opt-in Robot or unresolved CTRL-014/034/CAL-TIME-001. Maven -Pbuild compile, PowerShell parse, git diff --check, verify fast-forward chain and clean remote. Report each of six task statuses, exact SHAs and scopes, RED/GREEN, state/allocations, performance measurements and rejected candidates, independent Windows rerun commands, residual risks and next 3–5 priorities. Original CTRL-079 incident UNKNOWN / NOT_REPRODUCED; new unexplained exit stops, no retry masking. Local test baseline not certification of corporate SQL/transaction, real TaskContext/currency lookup, native focus/input or MDI/WAIT. Stop after CTRL-103.

---

## BATCH CTRL-104…CTRL-107 / PERF-001…PERF-006 — finish confirmed lifecycle fixes and profile control construction

**Status:** CLOSED / ARCHITECT REVIEWED — ten replies received, final agent regression 57/57 child and runner exits 0. CTRL-104/105/106 architecture approved; user approved next package, independent three-mode test result not explicitly supplied. No production performance change.
**Repo:** ssu95a/fore25
**Production branch:** migration/java25-correctness
**Mailbox branch:** agent/codex-mailbox
**Baseline remote HEAD:** df95a931230b57e87e7bef4bfba6267c3e9a395d
**Budget:** 630 minutes (10 h 30 min), ceiling.
**Dependency:** CTRL-098 user PASS/exit 0; acceptance fff0dde7d9fd0f1db472846e7ad685f63e7612d0; CTRL-103 agent 54/54 child and runner exits 0.

### Latest execution contract
Execute only CTRL-104,105,106,107 then PERF-001,002,003,004,005,006, in exact order, autonomously. Each starts from preceding verified clean HEAD. Verify fresh production/mailbox refs and clean tracked checkout before task/reply. All Git/GitHub operations within explicit scope approved. Main and JInvCommon read-only; no other repository writes, force push/destructive commands, dependency/public API changes, broad refactor, unapproved Binder/keyboard/LOV contracts, CTRL-014/034/CAL-TIME-001. No corporate DB writes, SQL cancellation/rollback guarantee, locks across blocking operation or nested FX loop. GUI ordinary FX thread, real worker paths require explicit publication/admission reasoning.

CTRL-104/105/106 permit isolated scoped correctness fixes after deterministic tracked RED on unchanged baseline and GREEN same assertions; CTRL-107 and PERF-001/002/003/005/006 READ-ONLY, ignored probes/logs only. PERF-004 sole conditional production optimization, at most one isolated change if measured meaningful benefit and semantic equivalence; NO_CHANGE is acceptable. No speculative cache, per-row state, eager compensating per-control objects, private JavaFX skin/VirtualFlow access or reflection, test-only production hooks, arbitrary sleeps, assertion weakening, forced JVM success. Preserve FXML/property/skin/LOV/validation/navigation lifecycle contracts. Russian Java comments. Fix/optimization tracked focused correctness smoke + runner mode, three fresh JVMs and unchanged official runner, relevant regressions, Maven -Pbuild compile, PowerShell parse, git diff --check. One validated fast-forward production commit per implemented task; verify remote SHA, no incomplete push. Profile actual absolute allocations and time with matched warmed workloads/ranges; distinguish allocation, live shallow bytes, retained heap, post-layout and paint; no downloads or dependency changes for profiler. Keep measurement overhead and fixture limitations explicit.

Record real native JVM and outer runner exits and preserve failed attempts. Original CTRL-079 failure UNKNOWN / NOT_REPRODUCED, accepted residual risk not waiver for new failures. Unexplained shared nonzero/timeout, regression, dirty/conflict/unexpected remote advancement/unreliable infrastructure: STOP safely, remaining NOT_RUN. Task-local unknown contract/no benefit: report BLOCKED/PARTIAL/NO_CHANGE and proceed only independent task on clean baseline with no regression. Exactly one Reply per task in docs/codex/outbox.md on agent/codex-mailbox; report scope, baseline/final SHA, actual commands/exits, confirmed facts vs UNKNOWN, state/allocations and Windows rerun with -SettingsFile "D:\Java\.m2\settings.xml" for fixes. User acceptance separate. Timeboxes ceilings; no expansion or self-assignment. STOP after PERF-006.

---

## MESSAGE CTRL-104

**Status:** DONE / ARCHITECT APPROVED / USER RERUN PENDING — 17cabb0b11b7b414fab811bcd59650f02a483561
**Timebox:** 45 minutes
**Reply ID:** CTRL-104
**Task:** FIX — bulk mark original error delivery

CTRL-101 proved genuine failure in actual JInvParallelAction bulk mark with detached table becomes FX NPE at getScene().getWindow(), original Throwable not delivered. Scope production ONLY DSFXAdapter; inspect markAll/unMarkAll async and outer catch, existing JInvErrorService.handleException(Object,Throwable), ViewContext.tryGetWindow and DialogError null-owner behavior. Architect authorizes original Throwable delivery exactly once via existing service: if original owner remains appropriate use it; if retired/detached/hidden/reparented use existing neutral/null context routing, not arbitrary stale/new table owner, and verify that existing neutral route is supported. Do not suppress genuine exceptions or change generic error service. Avoid off-FX access to scene/window at delivery; preserve existing active error behavior. If existing service cannot safely support neutral context, BLOCKED rather than redesign. Deterministic real shown table, actual parallel action and in-memory worker failure, mark AND unmark, active, closed, detached, hidden/reparented where applicable, original error identity, one delivery and no secondary FX NPE, no success refresh/write invented; active success controls. Existing admitted operations remain unchanged; no SQL cancellation/rollback. New focused -TableAdapterBulkMarkErrorDelivery; relevant close, search miss, bulk worker, mark/data binding regressions. No persistent fields if avoidable.

---

## MESSAGE CTRL-105

**Status:** DONE / ARCHITECT APPROVED / USER RERUN PENDING — 72f92bab82063aa873af9d9086d03022dfe2a7d8
**Timebox:** 60 minutes
**Reply ID:** CTRL-105
**Task:** FIX — bulk mark retired refresh ownership

CTRL-101 reproduced one retired full refresh after close during pending mark worker and after queued successful result. Only DSFXAdapter production. Reuse existing workerAttachmentRevision and captured original Dataset at admission, account for cached markAllAction/unMarkAllAction capturing original Dataset: inspect actual JInvParallelAction handle/scheduling and supported one-shot attachment lifecycle; no unsupported same-table rebind contract. Guard table-owned queued refresh at FX delivery, no obsolete refresh after close, no incorrect target on reparent, preserve active one refresh and logical mark/unmark counts/flags/order, existing CTRL-104 genuine error routing. Work already admitted may finish; no worker cancellation redesign or transaction change. Real mark and unmark parallel fixture, close before entry where deterministic, during admitted operation and after completion queued, double close, active success/empty big-table guard/repeated invocations/savePrev true/false, independent observers, fresh table, active failure. Explicitly distinguish whether unstarted work admission needs retirement gate under existing contract; do not add a new cancellation policy by assumption. New focused -TableAdapterBulkMarkClose. Existing bulk mark error/worker, queued close, adapter close, DataBinding modes. Minimize state, no new per-row objects.

---

## MESSAGE CTRL-106

**Status:** DONE / ARCHITECT APPROVED / USER RERUN PENDING — 0982d2112b8f8020d16f917adaab77caee29005d
**Timebox:** 60 minutes
**Reply ID:** CTRL-106
**Task:** FIX — deferred refreshRow retirement

CTRL-099 confirmed queued refreshRow still emits REPLACE after adapter close even when detached. Only DSFXAdapter production. Capture origin attachment and revision (reuse existing lifecycle state), validate before touching list/focus in existing FX callback and recheck before subsequent focus restore if list event synchronously closes owner. Preserve public index-at-delivery semantics; do NOT convert to entity identity, redirect indices, suppress active list events or change markAction contract in this task. Inspect legitimate detached-but-live caller semantics; close retirement is sufficient independently of visibility, don't require shown Stage if active detached table supported. Real ArrayDataSet/JInvTable plus observers: RED one obsolete REPLACE after close, GREEN zero, explicit and mark-driven queued delivery, close twice, late event, close during list notification, active same index replacement, invalid index, replace/remove/permutation still documented legacy index behavior, exact focus/selection/cursor/edit/commit counts. Positive active offscreen update, active detached case if supported. No transaction, full refresh, row cache or new callback count. New focused -TableAdapterRowRefreshClose; TableAdapterQueuedClose, TableAdapterClose, TableAdapterPagingClose, TableEditRowIdentity, TableEditing, TableCancel, DataBinding and bulk mark regression. Report row identity separately CTRL-107.

---

## MESSAGE CTRL-107

**Status:** DONE / READ-ONLY / ARCHITECT REVIEWED — single mark event carries original row identity; wrong-slot notification reproduced, no foreign writes
**Timebox:** 45 minutes
**Reply ID:** CTRL-107
**Task:** AUDIT — mark event identity ownership

READ-ONLY. CTRL-099 marked original A then delayed index refresh targeted X after replacement; zero wrong model writes. Inspect actual installed JInvCommon DataSetMarkEvent API/bytecode or read-only source, event production/dispatch, row identity availability, index meaning for single/all mark, sort/filter/update, live list and adapter markAction callers. Reuse deterministic shown fixture with equal-valued distinct A/X, remove, permutation, same identity and offscreen, independent observers. Distinguish event original identity vs explicitly index-based public refreshRow contract, verify visual mark/pseudoclass after moving A to another index and actual data mark values, commit/write/focus counts. Propose narrow next contract only if proven; no identity scan over all loaded rows or cache by assumption, no edits to JInvCommon or Fore25. Confirm whether mark event even exposes origin identity; if unavailable or ambiguous report UNKNOWN. Do not conflate wrong slot refresh with wrong DB write.

---

## MESSAGE PERF-001

**Status:** DONE / READ-ONLY / ARCHITECT REVIEWED — matched baseline
**Timebox:** 60 minutes
**Reply ID:** PERF-001
**Task:** BENCHMARK — matched stock TextField versus JInvTextField

READ-ONLY. Build reproducible ignored Windows Java25/FX25 benchmark on FX thread, identical text/font/width/parent/CSS/layout/pulse and count; compare stock TextField vs JInvTextField and (optional) stock TextField with actual JInvTextFieldButtonSkin only if compatible, distinguish functionality difference not claim equal features. Actual JInvTextField constructor calls setSkin(createDefaultSkin()) then initContextMenu/tooltips/keyboard/focus; stock generally installs default skin later. Separate fresh construction, attachment, applyCss/skin install and first layout, also total through comparable lifecycle. Do not compare stock bare constructor to framework constructor with eager skin as if same work. Same heap settings/process isolation, sizes e.g. 1/20/100/400 within budget, warmup >=5 batches/9 measured where practical, no retained references to closed graphs across runs, exact FX ThreadMXBean allocated bytes and elapsed ranges per control/batch, cold first-use separately. Verify real normal field behavior, no LOV, later LOV setup, readOnly and existing skin access before scene. Record baseline environment and source paths; raw logs and reproducible commands. Do not modify production/tracked tests. Select matched fixture for PERF-002/003/004/005/006.

---

## MESSAGE PERF-002

**Status:** DONE / READ-ONLY / ARCHITECT REVIEWED — nested skin creation dominates constructor allocations
**Timebox:** 60 minutes
**Reply ID:** PERF-002
**Task:** PROFILE — constructor allocation attribution

READ-ONLY. On PERF-001 matched fixture use existing local JDK JFR allocation sampling/available profiler (no downloads/new dependencies) plus exact FX ThreadMXBean deltas for phases; inspect JInvTextField fields, RequiredState, default properties, editableOrLovEditable initialization, external button properties, focus/keyboard/tooltip, skin eager call. Separate superclass, framework own setup and skin-related allocation using safe public/protected seams only where semantics preserved; do not override away behavior and call it equivalent. Report sampled weights/stacks as estimates, not exact per-class sizes, distinguish first class initialization vs steady state; rank top 5 allocation stacks/classes by measured absolute contribution where available. If JFR unavailable or sampling inconclusive, report UNKNOWN and use exact phase totals, no invented attribution. No private JavaFX reflection/test hooks. Use ignored local profiles/logs, avoid external app or secret data. Provide candidate only where measured and API/lifecycle can be preserved.

---

## MESSAGE PERF-003

**Status:** DONE / READ-ONLY / ARCHITECT REVIEWED — isolated component costs and compatibility constraints
**Timebox:** 75 minutes
**Reply ID:** PERF-003
**Task:** PROFILE — skin construction, CSS and context menu/overflow/wrapper costs

READ-ONLY. Inspect actual JInvTextFieldButtonSkin constructor and JInvWrapButton/JInvFEButton, IconFactory, ContextMenu population and dispose; eager ContextMenu populated before install and again on showing, wrapper/getButton used by subclasses, LOV/button and layout paths. Isolate construction and first CSS/skin/layout costs in PERF-001 matched workloads, including no LOV/no custom menu, overflow text, with internal/external button, context-menu custom items/populate callback, readOnly and bound/external menu/handler; preserve CTRL-049/055/061/063/066/078/091 behavior. Measure counts/allocations using existing public observation/profiler, not speculative object size; evaluate whether repeated menu item construction, icons, skin-owned children or CSS parsing dominate, first-use versus warm. Prototype only ignored variants (not tracked) with explicit compatibility tests: immediate getSkin/getButton/getContextMenu/items access before scene, FXML injection/property binding, custom menu override and dispose/recreate, keyboard/mouse activation, overflow layout and actual menu open. No shared mutable menu or node across controls, no production edit. Rank candidates with absolute benefit and blockers. No assumption delaying skin itself is safe.

---

## MESSAGE PERF-004

**Status:** NO_CHANGE / ARCHITECT REVIEWED — immediate menu contract prevents deferral; isolated binding saving below threshold
**Timebox:** 90 minutes
**Reply ID:** PERF-004
**Task:** PERF — one measured lazy initialization optimization, conditional

Depends PERF-001…003. Only if a materially costly eager object/work is proven unnecessary on common paths AND existing observable contract preserved. Authorize one narrow production change in JInvTextField and/or JInvTextFieldButtonSkin (only these two files) with explicit exact scope; do not rewrite all properties, lazify whole skin, remove immediate getButton/getContextMenu behavior, change FXML/default/property metadata or share Nodes/Menus. Candidate may defer unused internal work or avoid redundant allocation while preserving observed API; do not choose ContextMenu/wrapper/overflow laziness by guess. Prove existing behavior including immediate constructor and property access/binding/default/null semantics, public getListItemsToPopulateContextMenu and onPopulateContextMenu order/count, user custom menu and handlers, editable/readOnly, LOV/provider attachment, F9/button/overflow geometry, dispose and skin replacement, specialized JInvTextFieldCUR and descendants. No eager per-control compensating object/state; justify any added nullable reference vs saved objects and quantify retained delta separately. Compare identical warmed matched fixture before/after, N=20/100/400 same heap, exact allocated bytes/time/median/ranges both first-use and ordinary no-feature path, feature activation cost, no moved cost hidden; require meaningful absolute benefit and no material correctness regression. If no equivalent proven candidate or time insufficient, NO_CHANGE. One isolated validated commit only if conditions met, tracked focused smoke + official mode -TextFieldAllocationLifecycle or appropriate named mode; regressions TextFieldSkinDisposal, TextFields, ValueFields, MoneyDecimal, LovIntegration, BoundLovSetter, LovBindingInterleaving, TextFieldLovDelayedCompletion, CurrencyLovDelayedCompletion, ExternalLovReadOnly, WrapButtonOwnership, FormLifecycle. Do not use artificial performance assertions dependent on wall-clock. No public API changes.

---

## MESSAGE PERF-005

**Status:** DONE / READ-ONLY / ARCHITECT REVIEWED — global live shallow histogram delta, retained heap UNKNOWN
**Timebox:** 75 minutes
**Reply ID:** PERF-005
**Task:** AUDIT — retained/live heap per control

READ-ONLY. Exact allocation during launch is not retained memory. Use matched stock/framework fixtures from PERF-001 with identical heap flags, controlled strongly held live groups and detached/disposed/released groups, several N and repeated independent processes. Use local JDK jcmd GC.class_histogram or heap dumps/dominator analysis only if existing tools available and controlled synthetic data, never corporate heap; no uploads of raw dumps or secrets. Distinguish class histogram live shallow bytes/count vs true retained/dominator graph; report retained heap UNKNOWN unless dominator measurement exists. If using explicit GC as diagnostic, document not deterministic reclamation proof; weak-reference check bounded observations only. Account for shared static CSS/icon/font caches, scene/window roots and base controls, avoid retaining controller graphs in harness or pooling measurement objects. Separate bare constructor/skin-installed/shown/post-close states and optional features (LOV/ContextMenu/overflow). Compare stock/framework and PERF-004 if implemented, same fixture/heap/roots; record class counts and incremental live bytes per N where defensible, no claim of per-object retained size from global heap delta. Identify avoidable persistent state and verify no functional ownership conclusions from deliberate strong references. Binder shared FXEntity remains separately UNKNOWN. Reproducible commands, limitations, top 3 follow-ups.

---

## MESSAGE PERF-006

**Status:** DONE / READ-ONLY / ARCHITECT REVIEWED — agent regression 57/57 GREEN
**Timebox:** 60 minutes
**Reply ID:** PERF-006
**Task:** VERIFY — final regression and repeated matched profiling

READ-ONLY. Enumerate actual supported smoke modes at final HEAD (54 previous + new fix modes and any optimization mode); full battery within ceiling, prioritize CTRL-104/105/106, CTRL-098, currency/bulk/search, text/skin/LOV/numeric/controls/forms and table editing. Faithful ignored diagnostic runner immediate native and outer exits, exact PASS matrix, preserve all failures/timeouts and raw output. Maven -Pbuild compile, PowerShell parse, diff check, clean remote and isolated chain/scope. Repeat PERF-001 matched benchmark at same heap/settings before/after if PERF-004 implemented; if no production performance change, report baseline only, no claimed savings. Record all ten tasks dispositions, exact SHAs, RED/GREEN, per-instance and per-operation state/allocation delta, phase/profile/live-heap evidence and limitations; independent Windows rerun commands for fixes, rank next candidates by confirmed risk then measured absolute cost. Original CTRL-079 UNKNOWN / NOT_REPRODUCED remains; no blanket waiver. No claim of corporate SQL/TaskContext/MDI/WAIT/native input/paint readiness from local tests. Stop after PERF-006.

---

## BATCH CTRL-108 / PERF-007…PERF-012 — marker identity and measured skin memory reduction

**Status:** CLOSED / ARCHITECT REVIEWED — all seven replies received, final agent regression 57/57 child and runner exits 0; production unchanged. CTRL-108 blocked by equal-distinct row rendering counterexample; PERF-008/010 NO_CHANGE. User explicitly approved new public constructor for separately scoped PERF-013.
**Repo:** ssu95a/fore25
**Production branch:** migration/java25-correctness
**Mailbox branch:** agent/codex-mailbox
**Baseline remote HEAD:** 0982d2112b8f8020d16f917adaab77caee29005d
**Budget:** 510 minutes (8 h 30 min), ceiling.
**Dependency:** PERF-006 agent regression 57/57 child and runner exits 0; CTRL-104/105/106 architecture approved, independent user rerun not explicitly supplied; continuation authorized.

### Latest execution contract
Execute CTRL-108 then PERF-007…012 in exact order, autonomously. Each starts from preceding verified clean HEAD; fresh production/mailbox refs and clean tracked checkout before each task/reply. Main and JInvCommon read-only, no other repo writes, destructive Git, force push, dependency/public API changes or unapproved broad refactor. No CTRL-014/034/CAL-TIME-001 or speculative Binder/LOV/keyboard contract changes. GUI ordinary FX thread; actual worker paths explicit publication/admission reasoning. No corporate DB writes, blocking locks or SQL cancellation/rollback guarantee.

CTRL-108 conditional narrow correctness fix with deterministic RED/GREEN. PERF-008 and PERF-010 each permit at most one isolated measured optimization only with demonstrated semantic equivalence and meaningful absolute benefit. All other tasks READ-ONLY, ignored probes/logs only. No obligation to create production commit: NO_CHANGE if compatibility/measurement fails. Public/protected constructor, FXML, CSS, property, immediate skin/wrapper/menu, control identity, LOV/validation/navigation, disposal contracts preserved. No private JavaFX APIs or reflective internal access, shared mutable nodes/menu, per-row/per-control cache or eager compensating allocation, test-only hooks, arbitrary sleeps, assertion weakening or forced success shutdown. Minimize instance fields and allocations; distinguish exact allocated bytes, sampled estimates, global live shallow histogram delta, retained/dominator sizes, observable post-layout and paint. No new tools/dependencies/downloads, raw heap uploads or corporate data. Russian Java comments. Tracked correctness probe + runner mapping for implemented changes, three fresh JVM and official runner, relevant regressions, Maven -Pbuild compile, PowerShell parse, git diff --check. One validated isolated fast-forward production commit per implemented task, verify remote SHA, no incomplete push.

Record actual child JVM and outer runner exits, preserve failed attempts. Original CTRL-079 UNKNOWN / NOT_REPRODUCED is accepted residual risk, not waiver of new failure. Unexplained regression/nonzero/timeout, dirty/conflicting state, unexpected remote advancement or unreliable infrastructure: STOP safely, remaining NOT_RUN. Task-local unclear contract/no benefit: report BLOCKED/PARTIAL/NO_CHANGE and proceed only independent task on clean baseline. One Reply per task in docs/codex/outbox.md on agent/codex-mailbox; scope, baseline/final SHA, exact commands/exits, facts vs UNKNOWN, persistent and transient state delta, Windows rerun with -SettingsFile "D:\Java\.m2\settings.xml". User acceptance separate. Timeboxes ceilings, no expansion/self-assignment. STOP after PERF-012. All Git/GitHub operations within explicit scopes approved.

---

## MESSAGE CTRL-108

**Status:** BLOCKED / READ-ONLY / ARCHITECT REVIEWED — removing old-slot refresh breaks visible identities/marks for equal-distinct permutation; no compliant fix
**Timebox:** 60 minutes
**Reply ID:** CTRL-108
**Task:** AUDIT → FIX — single-marker notification row identity

CTRL-107 installed JInvCommon bytecode and fixture established DataSetMarkEvent.getRow exact original object for MARK_ROW/UNMARK_ROW, getIndex original position; MARK_ALL/UNMARK_ALL/REFRESH row null/index -1. Current markAction forwards index to public refreshRow, which is intentionally index-at-delivery and protected by CTRL-106 retirement guard. Authorize narrow entity-owned single-mark delivery ONLY DSFXAdapter production. Verify actual source/event operation and origin contract; compare object identity (==), not equals. Keep public refreshRow(index) behavior unchanged. Prefer shared existing scheduling code without extra callback or per-row state. Same row still at old index must receive exact existing REPLACE/focus behavior; removed/replaced row must not generate REPLACE for unrelated occupant. For moved original row, suppress old-slot refresh ONLY if real supported permutation/sort/filter/update paths already update moved row's visible mark/pseudoclass (including reuse/scroll and multiple queued marks). If moved row needs targeted invalidation and identity lookup contract/cost not proven, report BLOCKED/PARTIAL rather than inventing scan or fallback full refresh; no blanket O(N) search/cache. Guard original source/attachment/epoch and reentry close during list event. Deterministic RED/GREEN both directions with A/B/equal-distinct X, removal/permutation, offscreen/visible, multiple marks and cursor/focus/edit state, independent observer counts, active detached and retired cases, index API positive, bulk index -1 unchanged. No wrong DB write claimed. New focused -TableAdapterMarkRowIdentity if implemented; regressions TableAdapterRowRefreshClose, BulkMarkClose, BulkMarkErrorDelivery, TableAdapterQueuedClose, TableEditRowIdentity, TableEditing, TableCancel, DataBinding. Corporate SQL untested; JInvCommon read-only.

---

## MESSAGE PERF-007

**Status:** DONE / READ-ONLY / ARCHITECT REVIEWED — distinct JavaFX fallback and framework menu responsibilities
**Timebox:** 60 minutes
**Reply ID:** PERF-007
**Task:** AUDIT — ownership and actual use of the two ContextMenus

READ-ONLY. PERF-005 histogram at N=400 showed 800 ContextMenus vs stock 400, but global histogram does not prove exact ownership. Trace actual installed JavaFX25 TextFieldSkin/TextInputControlSkin public-source/bytecode and framework JInvTextFieldButtonSkin source, including inherited JavaFX default behavior, touch/keyboard/context request, property replacement and custom app handlers. Identify both objects/roots with available supported public inspection and local heap/dominator tooling if available; no new private reflection or unsupported JavaFX APIs, no downloads. Determine which is framework menu and whether other is superclass-owned/otherwise created; do not assume duplicate or unused. Actual pre-scene immediate menu and items, edit/readOnly/clipboard action semantics, onPopulateContextMenu/custom item order/identity, external/bound menu, custom request handler, replacement/disposal, repeated opening, specialized currency and other actual subclasses. Test root ownership/GC only as bounded evidence. If removing one requires unsupported superclass surgery or changes public observable behavior, NO_CHANGE; no shared mutable menu. Quantify class counts, live shallow delta and relevant allocation stacks, no retained-size claim without dominators. Report exact feasible next candidate or UNKNOWN. No tracked edits.

---

## MESSAGE PERF-008

**Status:** NO_CHANGE / ARCHITECT REVIEWED — immediately observable overflow node and identity contract
**Timebox:** 90 minutes
**Reply ID:** PERF-008
**Task:** PERF — lazy overflow button, conditional implementation

PERF-003 isolated overflow constructor 34,824 B and icon 4,128 B; PERF-005 saw 400 overflow buttons with 400 fields. Architect inspected current JInvTextFieldButtonSkin: overflowButton private, constructed eagerly, added before wrapper; checkOverflow called on text/width/font/external property and layout; JInvTextField.checkExcessTextLength returns false at width 0 but allocates Text. No public overflow getter in this class; lookup/children/CSS/initial visible behavior may be observable. Inspect all actual in-repo references/subclasses and public FXML/CSS/skin contracts; no assumption external callers absent. Authorize one narrow conditional production change ONLY JInvTextFieldButtonSkin: lazily create the same configured overflow button on first actual need, retain same identity after activation, no recreation on hide/show, no new eager fields/holder per skin, preserve order relative to wrapper when present, no deferred wrapper/skin/menu. Preserve actual checkOverflow override invocation semantics and geometry; do not silently change measured-text logic, initial focus/visibility/event semantics or initial first layout; if pre-layout visible/lookup difference is incompatible, NO_CHANGE or request explicit contract decision. Test short no-overflow across actual shown CSS/layout and pre-scene, initial long text, font/width transitions, later text/resize, external button suppress/remove, internal LOV/wrapper affects width, readOnly and specialized JInvTextFieldCUR descendants, genuine first overflow action through safe offline dialog fixture (cancel/accept and correct field target if feasible), custom menu and F9, disposal before/after first activation and skin replacement, no retired callbacks. Node count/order, exact geometry/visibility, layout stabilization and CSS/lookup, selection/model writes. Matched PERF-001 stock/framework fixture before/after same JVM settings N=20/100/400, 5/9 warmed repeats, short-only/overflow subset/all-overflow and first activation separate, measure exact FX bytes/time, live class counts with PERF-005 method, no shifted cost concealed. Meaningful absolute improvement and compatibility required; no forced optimization. Tracked correctness probe -TextFieldOverflowLifecycle if implemented, three fresh JVM plus official, TextFieldSkinDisposal, TextFields, LovIntegration, BoundLovSetter, LovBindingInterleaving, ExternalLovReadOnly, WrapButtonOwnership, CurrencyLovDelayedCompletion, MoneyDecimal, FormLifecycle. One isolated commit if proven.

---

## MESSAGE PERF-009

**Status:** DONE / READ-ONLY / ARCHITECT REVIEWED — decoration-only analogue delta 12,275,200 B/400, not actual production savings
**Timebox:** 75 minutes
**Reply ID:** PERF-009
**Task:** PROFILE — CSS/handler allocation stacks and overflow measurement

READ-ONLY. PERF-002 exact nested skin creation ~75.45 MB/400, JFR sampled CSS selector/iterator and event handler registration dominate. On current HEAD repeat actual matched fixture, compare with original accepted baseline if PERF-008 changed, source stack attribution using local JFR and exact FX ThreadMXBean phase totals. Separate default JavaFX skin work, framework additional nodes/menu/icon/button CSS, early constructor vs first applyCss/layout, cold vs warm. Inspect JInvFEButton constructor creates icon and Tooltip then overflow skin overwrites graphic and sets tooltip null; quantify redundant work via ignored equivalent prototype, distinguish transient allocation from live memory, no assuming all constructor operations redundant. Inspect checkExcessTextLength new Text on every call; measure actual call counts and allocation in layout/text/width/font/external-button changes (including tooltip), verify width/font/wrapper geometry and overridable call semantics. Use public supported API/seams; no private CSS engine or JavaFX internals, no global stylesheet changes, no caching Text/measure per field or shared mutable Node. Rank by absolute contribution to common workload and live cost; report reproducible experiments, ranges, sampled weights not exact allocation. No tracked changes.

---

## MESSAGE PERF-010

**Status:** NO_CHANGE / ARCHITECT REVIEWED — cross-package constructor access blocked by old API scope; superseded by PERF-013 explicit authorization
**Timebox:** 90 minutes
**Reply ID:** PERF-010
**Task:** PERF — one additional measured skin construction improvement, conditional

Depends PERF-007…009. Scope ONLY JInvTextFieldButtonSkin and JInvFEButton; one isolated candidate, no broad refactor or public API change. Prefer eliminating confirmed redundant overflow construction work (default graphic/Tooltip immediately replaced) ONLY if it remains material on actual current workload and compatible with existing constructors, CSS classes, action target, readOnly semantics, subclass and getTextField behavior; minimal non-public creation path if needed and source compatibility verified, otherwise NO_CHANGE. If more valuable proven CSS/skin-local candidate from audits fits exact scope, choose one and justify. Do NOT replace superclass-owned ContextMenu, defer immediate default menu population, whole skin or wrapper, alter app stylesheet/visual geometry, add global cache/per-field measurement node or share mutable Nodes/Menus, use private JavaFX APIs or change generic button semantics. Preserve existing public constructor behavior and all direct/subclass/FXML access, first-open/overflow actions and disposal; same matched correctness fixtures and performance gates as PERF-008. Measure absolute whole-lifecycle allocations/time and activation cost on N=20/100/400, short/mixed/overflow, same heap and 5/9 repeat; live counts where useful, no tiny isolated percentage optimization sold as whole-form savings. If PERF-008 already removed common-path cost and remaining gain is negligible, explicitly NO_CHANGE. Focused tracked correctness smoke/runner only if implemented; regressions including PERF-008 mode if present, TextFieldSkinDisposal, TextFields, ValueFields, LovIntegration, WrapButtonOwnership, CurrencyLovDelayedCompletion, MoneyDecimal, FormLifecycle. One commit at most.

---

## MESSAGE PERF-011

**Status:** DONE / READ-ONLY / ARCHITECT REVIEWED — same-SHA live shallow baseline reproduced; retained heap UNKNOWN
**Timebox:** 75 minutes
**Reply ID:** PERF-011
**Task:** VERIFY — matched live heap comparison and bounded ownership

READ-ONLY. Use PERF-005 FieldHeapAudit methodology, matched stock, original framework baseline 0982d211 and final framework with any approved PERF changes, same JVM heap flags, N=20/100/400 and two or more separate processes per size/variant where time permits, same scene roots/features and warm baseline. Need clean isolated baseline build/probes without altering current worktree (separate safe worktree or already verified baseline artifacts; no destructive checkout; if unavailable report limitation). Short no-overflow vs mixed/activated overflow, custom menu/LOV scenario separately; actual node counts, ContextMenu, wrapper, overflow, tooltip/menu item/class counts, global live shallow bytes after diagnostic GC at constructed/skin/shown/detached/disposed/released states. Drop actual fixture roots, bounded weak-reference observations, no memory leak assertion merely from histogram or GC. True retained/dominator size UNKNOWN unless valid local tool available. Report absolute and normalized global delta, not falsely exact retained per control; compare total allocations separately via PERF-001, including first activation costs. If no production PERF commits, record baseline verification and do not claim savings. No raw heap uploads, no corporate data, no tracked edits.

---

## MESSAGE PERF-012

**Status:** DONE / READ-ONLY / ARCHITECT REVIEWED — 57/57 agent GREEN
**Timebox:** 60 minutes
**Reply ID:** PERF-012
**Task:** VERIFY — final full regression and performance summary

READ-ONLY. Enumerate supported runner modes at final HEAD (57 previous + new ones), full battery, prioritize CTRL-104/105/106/108, all new PERF correctness modes, text/skin/LOV/currency/numeric, table/mark/editing/binding/form/lifecycle. Faithful ignored diagnostic runner actual native JVM/outer exit codes, timestamps, stdout/stderr/timeouts, preserve every failure. Maven -Pbuild compile, PowerShell parse, diff check, exact fast-forward chain and scope, clean refs. No opt-in Robot or unresolved CTRL-014/034/CAL-TIME-001. Report all seven dispositions, SHAs, RED/GREEN for correctness, equivalence and measured savings for PERF only if implemented, actual before/after same-workload FX allocations/time/live counts, per-instance state delta and activation costs, rejected alternatives/UNKNOWN. Independent Windows rerun commands for new modes and pending CTRL-104/105/106; do not assert user acceptance. Original CTRL-079 UNKNOWN / NOT_REPRODUCED stays recorded; new unexplained failure stops. Corporate SQL/TaskContext, real currency, native input/focus, MDI/WAIT, paint and retained heap limitations explicit. Rank next 3–5 by confirmed risk and absolute measured benefit. STOP after PERF-012.

---

## MESSAGE PERF-013

**Status:** NO_CHANGE / ARCHITECT REVIEWED — corrected candidate passed 58/58 locally but saved only 0.50–1.05% shown-lifecycle allocation; setTextField creates another Tooltip. No production or tracked changes. Follow-up ARCH-002/006.
**Repo:** ssu95a/fore25
**Production branch:** migration/java25-correctness
**Mailbox branch:** agent/codex-mailbox
**Baseline remote HEAD:** 0982d2112b8f8020d16f917adaab77caee29005d
**Reply ID:** PERF-013
**Timebox:** 90 minutes, ceiling.
**Dependency:** PERF-009/010/012 replies at dc818a7af890cb3fd89ac2168e781a63999b9e0e; agent regression 57/57 child and runner exits 0. CTRL-104/105/106 architecture approved, independent user reruns still pending.

### Approved architectural decision
Keep both classes in their original packages/FQNs. Add exactly one additive public overload to JInvFEButton:

    public JInvFEButton(Node graphic, Tooltip tooltip)

The user rejected a factory and approved this constructor. No static factory, subclass, alternate button class, package move or new persistent fields. Existing public JInvFEButton() and JInvFEButton(TextInputControl) remain source/binary compatible with original default graphic, tooltip, action, focus and target behavior. New overload installs caller-supplied graphic and tooltip; null explicitly means absent, no fallback creation. Same FEButtonHandler, focusTraversable=false, setTextField behavior, StringConverter and dialog accept/cancel semantics. Reuse small private initialization if beneficial. The architect's illustrative delegating default constructor is not mandatory if argument evaluation before super() would change initialization order; preserve legacy behavior and ordering of existing constructors. Document the new public overload with Russian Javadoc.

Production scope ONLY:
- src/main/java/ru/inversion/fx/form/controls/JInvFEButton.java
- src/main/java/ru/inversion/fx/form/controls/skin/JInvTextFieldButtonSkin.java

Replace the skin's eager default construction followed by graphic replacement and tooltip removal with construction using the existing final SMALL colored ellipsis graphic and null Tooltip. Preserve all other skin settings, immediate lookup/children order/exact class and identity, geometry, managed/focus/visibility flags, actual target and action. No lazy overflow policy change; no ContextMenu/CSS/text measurement optimization in this task. No unrelated constructor caller changes, public additions other than this overload, new dependencies, cache, extra instance objects or fields.

### Evidence and acceptance
PERF-009 isolated decoration analogue: 15,251,200 versus 2,976,000 B at N=400 (delta 12,275,200 B); actual end-to-end savings UNKNOWN. Compare unmodified baseline versus actual candidate, not analogue. Reuse PERF-001/009 matched FieldCostAudit with same JDK25/FX25, G1, Xms256m/Xmx1024m, identical stylesheet, text/font/width, N=20/100/400, >=5 warmups/9 samples where practical, separate fresh JVMs for baseline/candidate using safe isolated build or verified existing baseline artifacts. Record constructor and nested skin, full shown through post-layout, exact FX-thread allocated bytes/time medians/ranges, cold vs warm, short and overflow activated cases and first dialog activation separately if practical. No retained-heap claim from allocation delta. Measure actual absolute whole-lifecycle gain and no shifted cost concealed; only publish if meaningful reproducible benefit and compatibility demonstrated. If not, NO_CHANGE with evidence.

Tracked focused correctness smoke and runner mapping -FEButtonConstruction (or clearly named equivalent) if change implemented. Baseline behavior characterization and candidate equivalence, not fabricated RED for optimization. Existing no-arg and TextInputControl constructors, direct caller and FXML/default construction, custom graphic/tooltip including null, exact button class, no shared node/tooltip between controls, pre-scene lookup and child order, actual configured colored graphic and null tooltip in skin, short/long/short and resize/font/external/LOV behavior, readOnly and JInvTextFieldCUR, action target, StringConverter and actual offline JInvFEDialog cancel/accept if fixture supports it, no foreign writes, custom menu/F9/direct fallback, dispose/replacement. Distinguish real dialog evidence from synthetic event dispatch; unsupported cases explicitly UNKNOWN; do not publish if critical action equivalence unproven. Preserve original public constructors' initialization and observable defaults. No private FX APIs/reflection, arbitrary sleeps, test-only production hooks or forced JVM success.

Three fresh GREEN JVMs plus unchanged official focused runner, actual child/outer exits, relevant TextFieldSkinDisposal, TextFields, ValueFields, LovIntegration, BoundLovSetter, LovBindingInterleaving, ExternalLovReadOnly, WrapButtonOwnership, CurrencyLovDelayedCompletion, MoneyDecimal, FormLifecycle and TextArea paths using legacy constructors. Maven -Pbuild compile, PowerShell parse, git diff --check. Full supported regression battery within ceiling, or report explicit coverage and pending cases; any unexplained failure stops. One isolated validated fast-forward production commit if implemented, verify remote SHA and clean tracked tree. No incomplete push. Preserve all failed attempts and exact exit codes; original CTRL-079 UNKNOWN / NOT_REPRODUCED is not waiver of new failures. Main and JInvCommon read-only, no destructive commands or other repo writes. All necessary Git/GitHub operations within scope approved.

Publish exactly one Reply PERF-013 in docs/codex/outbox.md on agent/codex-mailbox, with baseline/final SHA, actual before/after metrics and ranges, compatibility evidence, exact tests/exits, state delta, residual UNKNOWN, independent Windows rerun commands using -SettingsFile "D:\Java\.m2\settings.xml". User acceptance separate. STOP after PERF-013. CTRL-109 and PERF-014…017 remain proposals, not assigned by this decision.

---

## ARCHITECTURE DECISION — thin skins and control-owned context menu

**Status:** USER APPROVED — intentional migration of legacy behavior to control ownership.

Skin is a visual adapter: layout, geometry, rendering, CSS, skin-owned visual nodes, visual listener lifecycle, and delegation to control. The control owns behavioral configuration, actions, menu composition and lifecycle, and feature state. Skin must not configure behavioral constituents independently of the control. Do not move visual layout into the control or introduce a large new manager per instance. Do not broaden this decision into a global skin rewrite. Context menu is the first migration slice.

The user explicitly authorizes the intentional contract change: internal framework ContextMenu is created on demand for ContextMenuEvent.CONTEXT_MENU_REQUESTED; its presence and populated items before the first request are no longer guaranteed. Existing application-provided ContextMenu, bound properties and onContextMenuRequested handlers retain precedence. No private JavaFX API, no attempt to remove inherited JavaFX fallback menu. Preserve editing, readOnly, action/LOV/FE semantics and custom menu extension order. Control-owned menu and extensions must survive skin replacement. Existing ISkinPopulatable signatures remain compatible as thin delegations for framework controls during migration; do not delete the interface or rename FQNs without separate authorization. Inventory consumers and report external compatibility UNKNOWN. A small shared stateless helper or lazily created feature state is allowed if justified; no eager per-instance manager, no shared mutable MenuItem/ContextMenu across controls.

## BATCH PERF-014…PERF-016 — event-driven control-owned context menu migration

**Status:** ACTIVE / ASSIGNED — PERF-013 reported NO_CHANGE; verified unchanged production 0982d211. Execute PERF-014…016 once, then continue ARCH-001…008 under the latest combined authorization below.
**Repo:** ssu95a/fore25
**Production branch:** migration/java25-correctness
**Mailbox branch:** agent/codex-mailbox
**Starting baseline:** 0982d2112b8f8020d16f917adaab77caee29005d (verified PERF-013 NO_CHANGE).
**Budget:** 270 minutes (4 h 30 min), ceilings.
**Order:** PERF-014, PERF-015, PERF-016.

---

## MESSAGE PERF-014

**Status:** ASSIGNED / ACTIVE COMBINED BATCH
**Timebox:** 105 minutes
**Reply ID:** PERF-014
**Task:** Migrate JInvTextField menu ownership and event-driven lazy construction.

Inspect actual current branch sources and all consumers with rg (ISkinPopulatable, getListItemsToPopulateContextMenu, onPopulateContextMenu, populateContextMenu, getContextMenu, setContextMenu, CONTEXT_MENU_REQUESTED, custom skins/subclasses/FXML). Existing JInvTextField constructor eagerly installs skin then initContextMenu; skin allocates own menu, populates defaults before control adds custom items, repopulates on show. PERF-007 established separate superclass fallback menu. Use actual installed JavaFX 25 sources/bytecode and deterministic shown fixture to establish event dispatch ordering and supported handling for mouse, keyboard Shift+F10/context key, touch if feasible, consumed requests, bubbling/target (child/skin nodes), app handler/filter, bound null/non-null, custom menu, detached/no window. Do not assume addEventHandler or filter order. Prefer a control-owned event handler/filter preserving application's property; choose either lazy install allowing native dispatch or explicit show/consume ONLY after proving exactly one popup and no double dispatch/fallback, correct anchor coordinates for keyboard/mouse. Respect consumed events and application-owned menus/handlers, do not overwrite bound property or steal external menu. If property remains installed after first use, ensure correct dynamic refresh on later openings including programmatic show, do not populate twice per opening. Handle replacement/null/binding transitions and app handler installed after first use without stale own popup/ownership; cleanup exact own handlers only. Preserve standard items, disabled states at actual opening, clipboard/selection, custom order and callback count, FE action and original target. Do not create default menu/items/FE decoration on ordinary construction except compatibility-demanded explicit access; design lazy extension state so no unnecessary per-control object. Capture old virtual getEditDialogAction timing/subclass contract; if changed disclose and prove current supported behavior, do not silently drop overrides. New control-level methods for extension permitted exactly to replace existing skin-level extension contract (same signatures or clearly documented minimal additive equivalent), no other public API expansion. Existing skin methods delegate to owning JInvTextField; preserve public populateContextMenu entry point as delegation if present. If skin is publicly used with stock TextField, inventory and preserve supported behavior with minimal shared behavior delegate, or report blocker rather than silently break; no duplicated eager fallback path for framework controls. Do not migrate TextArea yet. Preserve PERF-013 if implemented. No change to overflow/wrapper/layout logic in this task.

Production scope: JInvTextField, JInvTextFieldButtonSkin, one shared small context-menu support class/interface if necessary (justify stateless/lazy state; intended reuse for TextArea); no other class edits absent explicit necessity/architect approval. ISkinPopulatable stays. Document intentional pre-request getContextMenu()/items contract change in Russian source comments/Javadoc. Track actual caller migration and compatibility exceptions; no claim about external applications not available. Deterministic tracked test -TextFieldContextMenuLifecycle (or equivalent) covering no framework menu/default items on construction, first/second popup, actual popup show/hide and exact count, menu actions and disabled state, app-owned and bound properties, custom callback and item identity/order, control-level and legacy skin delegates, specialized JInvTextFieldCUR/JInvRegExpTextField, skin dispose/replace including held old skin, no retired callbacks, no duplicate handler registration, actual FE action target/cancel/accept via safe fixture if supported. Actual native touch/clipboard writes only if safe, otherwise UNKNOWN. Existing tests expecting eager framework menu may be updated ONLY for explicitly approved new contract; preserve all ownership/disposal/custom assertions and document every changed assertion. No assertion weakening beyond changed contract. Compare same matched PERF-001/009 fixture before/after with 20/100/400, same heap/JDK/FX/styles, 5/9 warm, exact FX allocations/time/ranges and live menu/item counts, short no-menu-use and first/repeated open separately. No hidden shifted cost or retained heap claim. Publish isolated validated fast-forward commit if migration correctness passes; report material benefit or lack thereof, not a requirement to fabricate savings.

---

## MESSAGE PERF-015

**Status:** ASSIGNED / ACTIVE COMBINED BATCH
**Timebox:** 90 minutes
**Reply ID:** PERF-015
**Task:** Migrate JInvTextArea context menu to the same control-owned behavior.

Current JInvTextArea.initContextMenu and JInvTextAreaSkin duplicate menu ownership and extension machinery (verified sources). Depend on PERF-014 proven event path and compatible design; reuse shared stateless/lazy support rather than duplicate framework implementation. If PERF-014 BLOCKED and no proven common event route, report BLOCKED; do not invent second architecture. Only JInvTextArea, JInvTextAreaSkin and existing shared menu helper plus minimal control-level extension methods analogous to TextField. Keep ISkinPopulatable delegation compatibility. Preserve actual TextArea menu item resources/order, edit/readOnly/clipboard/FE actions, app overrides/binding/custom list and callback, FXML and subclass behavior. Leave toolbar, external editor action/LOV lifecycle and visual layout alone; verify prior CTRL-050/053/062/066, no duplicate registrations on skin replacement. Control-owned menu survives skin disposal, exact owner and no stale skin callbacks, no eager per-control helper or shared mutable menu. Test actual mouse/keyboard popup, first/repeat, bound/external/custom handlers, control-level vs skin delegation, FE action target, TextArea-specific multiline and toolbar/LOV behavior. Existing eager-menu assertions may change only as expressly authorized; preserve ownership assertions. Focused tracked -TextAreaContextMenuLifecycle; before/after matched warmed fixture N=20/100 (400 if practical), exact FX allocations/time and live object counts, first/repeated open separately. One isolated validated commit if proven.

---

## MESSAGE PERF-016

**Status:** ASSIGNED / ACTIVE COMBINED BATCH
**Timebox:** 75 minutes
**Reply ID:** PERF-016
**Task:** Verify the control-owned menu architecture and full regression; inventory remaining thin-skin candidates.

READ-ONLY. Run full supported runner battery at actual final HEAD, including PERF-013 if implemented, prior 57 modes and new menu modes. Record real native JVM and runner exits separately, all stdout/stderr, timestamps and timeouts via faithful ignored diagnostic copy, preserve failures. Maven -Pbuild compile, PowerShell parse, diff check, clean remote and exact isolated fast-forward chain. Verify new ownership/control-skin boundary in actual source and tests, inventory remaining skin-side action/feature configuration (overflow, wrapper, TextArea toolbar and editor actions), rank proposed follow-up slices without production changes. Verify no unused eager manager/state or duplicate default menu population; measure comparative total FX allocations and live shallow counts of actual no-menu-use and first-use workloads on same JVM/heap, do not claim retained heap or paint. Report intentional API/behavior changes and exact external compatibility UNKNOWN, programmatic getContextMenu().show() callers before first use need documented migration; report supported explicit way to request/open framework menu if required, no new API without approval. Include test coverage for public stock-control skins if supported, skin replacement, actual dialog and external application precedence. Report all three task statuses, exact SHAs, user rerun commands for every new mode, remaining migration work including possible later retirement of ISkinPopulatable, no global rewrite. PERF-016 is an intermediate mandatory regression gate; continue ARCH-001 only if clean and all supported modes exit 0, under the latest combined authorization below.

### Execution boundaries
Each task starts from preceding verified clean HEAD. Verify current production/mailbox refs before task/reply. Main and JInvCommon read-only, no other repository writes, destructive Git, force push, dependency changes, private JavaFX API/reflection, global caches, shared mutable nodes/menus, arbitrary sleeps, forced JVM success or test-only production hooks. New public API restricted to explicitly authorized menu-extension entry points; preserve other contracts. No corporate SQL/transaction changes. For implemented changes deterministic baseline characterization and candidate assertions (intentional new lazy/ownership behavior expected to differ), three fresh JVMs plus unchanged official runner, focused tracked smoke, relevant existing regressions, compile/parse/diff check and exact exit codes. One isolated validated fast-forward production commit per implemented migration step. Russian Java comments. No incomplete push. Record failed attempts; original CTRL-079 UNKNOWN / NOT_REPRODUCED not waiver for new failure. Shared unexplained nonzero/timeout/regression, dirty/conflict/unexpected remote advancement: stop safely, remaining NOT_RUN. Task-local unresolved contract: BLOCKED/PARTIAL and proceed only independent tasks on clean baseline. One Reply per task in docs/codex/outbox.md with exact baseline/final SHA, scope, changed assertions/contract, actual commands/exits, allocation/state measurements, facts/UNKNOWN, independent Windows rerun with -SettingsFile "D:\Java\.m2\settings.xml". User acceptance separate. No self-assignment. All Git/GitHub operations within scope approved.

---

## COMBINED ARCHITECTURE BATCH — PERF-014…016 + ARCH-001…008

**Status:** ACTIVE / ASSIGNED — latest user request authorizes all identified text-control/skin refactoring in sequential slices.
**Repo:** ssu95a/fore25
**Production branch:** migration/java25-correctness
**Mailbox branch:** agent/codex-mailbox
**Baseline remote HEAD:** 0982d2112b8f8020d16f917adaab77caee29005d
**Budget:** 1020 minutes (17 hours), ceilings: PERF-014…016 270 + ARCH-001…008 750.
**Dependency:** PERF-013 NO_CHANGE, Reply 527d633ff37c6a9989f34db618cb9bc2e45575f0; production unchanged, candidate-only 58 modes not published. Prior actual production regression 57/57. CTRL-104/105/106 independent user rerun not reported.
**Order:** PERF-014, PERF-015, PERF-016 (mandatory intermediate gate), ARCH-001, ARCH-002, ARCH-003, ARCH-004, ARCH-005, ARCH-006, ARCH-007, ARCH-008.

### Latest authorization and design boundaries
This is the latest execution contract. Do not repeat completed tasks or PERF-013. If PERF-014…016 already in progress, finish original scopes and continue only after verified intermediate gate; do not interrupt or overwrite a running task. PERF-016 no longer requires a session stop if gate fully green; STOP only after ARCH-008 or stop rule. If task-local blocked, document dependencies and continue independent tasks only on clean baseline; PERF-016 failure prevents subsequent implementation.

Control owns behavior, actions, target associations, menu/functional button configuration and lifecycle; skin owns presentation, layout, visual nodes/visual listeners and delegation. Shared behavior may be stateless helper, lazy state only if necessary. No new manager/action/property per control merely to implement pattern; quantify permanent state and temporary allocations. Preserve public FQNs, existing FXML/constructors/signatures and behavior except the approved explicit lazy framework ContextMenu contract; additive minimal control menu/target/behavior entry points and approved JInvFEButton overload permitted within task scope, no factory. No deletion of ISkinPopulatable, JInvWrapButton or wholesale inheritance hierarchy refactor; compatibility adapters delegate rather than own behavior. External caller compatibility UNKNOWN must be recorded. No unapproved removal of immediately observable overflow Node; no arbitrary lifecycle/cancellation/LOV/FE/security/keyboard/transaction semantics changes. Main/JInvCommon read-only. No other repo writes, force push/destructive Git, dependency changes, private JavaFX APIs/reflection, shared mutable Nodes/Menus, global cache, per-row cache, new eager per-control objects, test-only production hooks, arbitrary sleeps, forced JVM success. No real corporate DB writes or external editor process launch. GUI ordinary FX thread, real worker needs explicit safe FX publication/ownership; no lock across blocking I/O/nested loop, no SQL cancellation/rollback guarantees. Russian Java comments/Javadoc.

Each implemented slice: characterize baseline behavior, deterministic RED/GREEN for confirmed defects, equivalent positive tests for refactors, explicit approved intentional contract differences, no weakened assertions outside those. Three fresh JVMs and unchanged official focused runner, actual native and outer exits, relevant regression modes, Maven -Pbuild compile, PowerShell parse, git diff --check. One isolated validated fast-forward production commit per implemented task, verified remote SHA, no incomplete push. Performance changes conditional on meaningful absolute measured benefit and equivalence; architectural ownership migration justified by approved design even if no material performance gain, report that honestly. Read-only tasks allow ignored probes only. Preserve previous accepted fixes; no broad cleanup. If external compatibility cannot be safely established for a specific API, preserve legacy delegate or BLOCKED instead of guessing. Timeboxes ceilings not minimum work duration.

Verify clean tracked checkout and fresh production/mailbox refs before each task and reply. Shared unexplained nonzero/timeout/regression, dirty/conflict/unexpected advancement, unreliable infrastructure: STOP safely, report remaining NOT_RUN. Original CTRL-079 UNKNOWN / NOT_REPRODUCED not waiver. One Reply per task in docs/codex/outbox.md on agent/codex-mailbox. Include baseline/final SHA, scope, actual commands/exits, intentional API change, confirmed vs UNKNOWN, allocations/live-state delta and independent Windows commands -SettingsFile "D:\Java\.m2\settings.xml". All Git/GitHub operations within explicit scopes APPROVED. User acceptance separate. No self-assignment.

---

## MESSAGE ARCH-001

**Status:** ASSIGNED / ACTIVE COMBINED BATCH
**Timebox:** 90 minutes
**Reply ID:** ARCH-001
**Task:** REFACTOR — FE action independent of visual button

Actual JInvFEButton.FEButtonHandler creates JInvFEDialog(target, stringConverter), showAndWait and optional setText. JInvTextField.getEditDialogAction fires existing inner FE button if present, else allocates temporary new JInvFEButton(this). JInvTextArea.getEditDialogAction fires skin.getFeButton. Introduce ONE shared direct FE action execution path, no mandatory Action/manager per field, preferably existing JInvFEDialog or narrowly scoped shared helper with minimal additive API if cross-package requires. Preserve exact existing button public constructors, custom onAction and disabled/fire semantics, configured converter, dialog ownership/title/clipboard/selection, cancel/accept/readOnly and error routing. Existing explicitly configured FE button with overridden action/converter must keep that customization (do not bypass button.fire when its behavior is intentionally supplied). Replace only temporary fallback allocation with direct action; ordinary menu/keyboard path must not create a button just to invoke a dialog. TextArea delegation may remain until ARCH-005 where toolbar owner migrates. Scope JInvFEButton, JInvTextField, JInvTextArea only if needed, JInvFEDialog or one small helper. No generic action redesign. Actual offline dialog shown for direct/button/menu/keyboard path, correct original target vs sentinel, converter to/from, cancellation zero writes, accepted exactly one intended write, readonly zero, configured disabled/custom handler, no temporary visual allocation measured (benchmark separate dialog overhead), skin replacement. Review FXML/subclass callers. Track focused -FEActionDelegation; existing menu, text/skin/LOV/currency, MoneyDecimal and FormLifecycle regressions. One isolated commit if equivalent.

---

## MESSAGE ARCH-002

**Status:** ASSIGNED / ACTIVE COMBINED BATCH
**Timebox:** 90 minutes
**Reply ID:** ARCH-002
**Task:** REFACTOR — target association distinct from button configuration

Actual JInvButton.setTextField(ed) stores PROPERTY_TEXTFIELD, optional disable binding to editable.not, copies JInvTextField button tooltip text (empty string creates Tooltip), recognizes external via CONTROL_PARENT and parent, writes externalButtonProperty, sets disable according to ignoresEditable. JInvFEButton overrides and setsDisable(false), public legacy contract. Inventory all callers including FXML/idTextField linking in controllers/Controls, JInvLOVButton/JInvChoiceButton, special subclasses, external/custom buttons. Introduce minimal explicit target-only association path, ideally package-private/protected within same controls package; additive public API only if justified cross-package use and name/contract clear, no boolean flag matrix. Keep public legacy setTextField and existing FXML/setIdTextField behavior as compatibility delegation; move config decisions to actual control-owned attachment path for migrated internal buttons, do not globally change null/detach/readOnly/security/disable semantics. Preserve custom tooltip/bound tooltip, bound disable rejection behavior, legacy external registration, FE always accessible per JAVAKERNEL-542 where supported, LOV readOnly guard, target identity and reattachment cleanup exact ownership. No automatic external precedence change, no new fields or unnecessary allocation, existing properties map may be reused. If clean separation requires wider unsupported contract, BLOCKED with exact call path. Scope JInvButton, JInvFEButton, JInvTextField/JInvTextArea where necessary for internal configuration; do not edit skin visual layout or global controller binding. Actual internal and external fixtures, pre-scene/parented, FXML linking, repeated attach/detach, bound properties and exception atomicity, custom converters/actions, readOnly and external/LOV. New focused -ButtonTargetAssociation, relevant keyboard, external LOV, WrapButtonOwnership, currency, text/area lifecycle. Measure discarded Tooltip path and actual no-new-object association; optimize overflow specifically ARCH-006. One isolated commit.

---

## MESSAGE ARCH-003

**Status:** ASSIGNED / ACTIVE COMBINED BATCH
**Timebox:** 120 minutes
**Reply ID:** ARCH-003
**Task:** REFACTOR — control-owned innerButton, explicit LOV/FE precedence

Current JInvTextField setLOV/setInnerButton/removeInnerButton/getInnerButton route through concrete skin.getButton wrapper. JInvTextFieldCUR constructor directly installs LOV in wrapper; inventory every actual in-repo caller/subclass and public skin.getButton/getInnerButton consumers at current HEAD. JInvWrapButton currently enforces JAVAKERNEL-1550 LOV > FE. Migrate authoritative functional inner-button state to JInvTextField. Use existing nullable/lazy state if practical; no eager ObjectProperty/manager on every field; justify nullable reference/observable notification and no per-instance duplication if avoidable. Control configures target, tooltip, enable/disable/readOnly/ignoreEditable, LOV-vs-FE precedence, external priority exactly as existing accepted behavior. Skin attaches/render/delegates, no authority on business choice. Retain public setInnerButton/removeInnerButton, setLOV/unbindLOV, skin.getButton and wrapper compatibility where existing callers use it; legacy skin access must not silently desynchronize control ownership, either thin delegation/adaptation with cycle guard or explicit BLOCKED for unsupported contract. Preserve bound LOV setter failure atomicity, attachment revision/ABA delayed completion, model values, F9/external/inner precedence (CTRL-046 external+inner coexistence remains unresolved; do not redefine), custom buttons/converters, currency fallback and specialized init. Correct evident setInnerButton condition (getSkin()!=null || instanceof) only within proven new ownership route, no incidental broad refactor. Skin replacement/no skin/custom skin should retain control-owned behavior; old skin removes only its visual bindings, not control state; no two Nodes with same parent, no stale CONTROL_PARENT or action. Scope JInvTextField, JInvTextFieldButtonSkin, JInvTextFieldCUR and directly identified subclasses using old skin path (list actual files first), JInvWrapButton only minimal delegation needed; no other control families. Test actual skin replacement while LOV/FE/custom attached, before/after scene, direct legacy wrapper callers, FXML, external attach/remove, same-provider/detach/ABA, readOnly/F9/action identity, old skin disposed, exact listener/model writes, no retired actions. New focused -TextFieldInnerButtonOwnership; previous LOV/skin/wrapper/currency/numeric/form regression matrix. One isolated commit if proven, otherwise BLOCKED. Explicitly report compatibility of external callers UNKNOWN.

---

## MESSAGE ARCH-004

**Status:** ASSIGNED / ACTIVE COMBINED BATCH
**Timebox:** 90 minutes
**Reply ID:** ARCH-004
**Task:** REFACTOR/AUDIT — thin JInvWrapButton and direct visual placement feasibility

After ARCH-003, move LOV-vs-FE selection rule out of framework textfield visual path into owning control. Inventory generic standalone JInvWrapButton uses and public methods; don't remove legacy priority for other actual consumers without proven migration. Wrapper currently binds onAction, visible, graphic, text, disable, tooltip and stores CONTROL_PARENT in deferred callback; preserve CTRL-043/055 deferred guard, exact ownership cleanup, logical parent navigation, action/security dispatch, CSS, pref/min/max widths, layout, and active bindings. Make wrapper pure visual adapter for migrated controls; if old standalone contract must stay, compatibility path delegates, document. Evaluate placing actual functional button directly instead of duplicate wrapper in ignored prototypes: actual parent/child, CSS lookup and exact identity, focus/mouse/key behavior, graphic Node reparent, public skin.getButton compatibility and custom FXML. DO NOT delete JInvWrapButton or change public FQN/getButton return type; removal remains conditional separate future contract if compatibility cannot be proven. One narrow implementation only for proven delegation simplification after ARCH-003; scope JInvWrapButton/JInvTextField/JInvTextFieldButtonSkin. No per-field additional wrapper, duplicate state, global cache. New or expanded focused smoke -TextFieldWrapperDelegation, exact active/stale callback, order and identity counters; WrapButtonDeferred/Ownership, TextFieldSkinDisposal, ExternalLovReadOnly, BoundLovSetter, delayed LOV/currency tests. Measure actual allocations and live counts before/after on matched workload, no invented savings. If no safe simplification, NO_CHANGE with explicit blockers.

---

## MESSAGE ARCH-005

**Status:** ASSIGNED / ACTIVE COMBINED BATCH
**Timebox:** 120 minutes
**Reply ID:** ARCH-005
**Task:** REFACTOR — control-owned TextArea toolbar and external editor action

Verified JInvTextArea.getToolBar returns skin toolbar else new disconnected ToolBar; getEditDialogAction calls skin FE button. JInvTextAreaSkin owns initToolBar, FE and external editor buttons, editable-state membership, Scene EDIT_EXTERNAL registration, ExternalEditorManager and parallel action which reads getSkinnable().getText and Controls.setValue(getSkinnable(), result). Migrate functional toolbar composition, target/availability, FE and external editor actions and Scene registration ownership to JInvTextArea. Skin only renders/layouts/stylizes toolbar and delegates, owns visual listeners and releases them. Preserve public getToolBar/getFeButton and supported customization access; no disconnected new toolbar per getter, stable control-owned lazy toolbar if compatible with immediate public access, no eager extra Action/manager per area beyond necessary existing state. Keep custom toolbar items/order, FE/LOV visibility and exact owned buttons, external editor availability/readOnly/editable, keyboard registration security/scene ownership, CTRL-050/053/062/066 and current menu contract. On replacement remove old visual parent/bindings safely before installing new (actual Node can have one parent), control configuration survives. Worker captures original target and input/extension on FX before dispatch where appropriate; test actual JInvParallelAction/ExternalEditorManager API, deliver result on FX with explicit ownership and admission reasoning, no access to disposed skin or foreign control, preserve original error identity. Do not invent external editor cancellation/overwrite policy: test lifecycle/late result, if target remains valid but text changed policy UNKNOWN report separately; do not silently discard or overwrite under new semantics. Use controlled offline external editor fixture/seam already supported; no actual external process launch or file writes outside ignored fixture, if cannot prove action path report BLOCKED/PARTIAL rather than speculative rewrite. Scope JInvTextArea, JInvTextAreaSkin, existing shared FE helper only if needed. No generic SceneKeyManager/parallel service rewrite. Actual scene detach/reattach, skin replacement while worker pending, old/fresh targets, custom toolbar and width listeners, FE dialog, LOV delayed completion, F9/EDIT_EXTERNAL, hide/show, readonly, no duplicate listeners/action registration, exactly one model write or established cancellation. Focused -TextAreaControlOwnedToolbar; existing TextAreaSkinDisposal, TextAreaToolbarOwnership, TextAreaLovLifecycle, TextAreaLovDelayedCompletion, ExternalLovReadOnly, FormLifecycle/Cancel, menu probes. One isolated commit only if proven.

---

## MESSAGE ARCH-006

**Status:** ASSIGNED / ACTIVE COMBINED BATCH
**Timebox:** 90 minutes
**Reply ID:** ARCH-006
**Task:** PERF — overflow feature initialization after target separation

Revisit PERF-008/013 using preceding verified code. PERF-013 candidate locally 58/58 but only 0.50–1.05% whole-lifecycle savings, discarded Tooltip created by legacy setTextField. User previously approved public JInvFEButton(Node graphic, Tooltip tooltip) with explicit null semantics, no factory/package move. Authorize this exact overload if still useful in new architecture and actually skips discarded decoration via ARCH-002 target-only path. Preserve existing constructors/target/custom actions, exact public overflow class and immediate node identity/order unless explicit lazy behavior contract established. PERF-008 public lookup/children pre-layout exposes eager node; user approved laziness for ContextMenu specifically, NOT blanket permission to remove observable overflow node. Audit real references and external UNKNOWN, evaluate lazy creation in ignored prototype, no production change to immediate-node contract without separately documented user decision. May implement eager-but-cheaper exact class path or safe lazy internal resources preserving public node/behavior; no wrapper/menu reversal, new per-field state or private APIs. Control owns behavioral config/action, skin visual layout. Scope JInvFEButton, JInvTextField, JInvTextFieldButtonSkin and minimal reuse of approved target association (JInvButton only if strictly necessary with unchanged legacy API). Matched baseline/candidate N=20/100/400 same G1/heap/CSS/fonts, 5/9 warm, no-overflow/mixed/all and first activation, exact FX allocation/time/ranges/live shallow counts; prove real absolute improvement, no cost shifted. Actual dialog accept/cancel, custom converter, readonly, external/LOV, pre-scene lookup, geometry, identity, skin replace/dispose, tooltip null. If small savings or contract unresolved, NO_CHANGE. New focused -FEButtonConstruction or appropriate mode if implemented, all affected text/area/LOV/menu/skin/FE/toolbar regressions. At most one commit.

---

## MESSAGE ARCH-007

**Status:** ASSIGNED / ACTIVE COMBINED BATCH
**Timebox:** 60 minutes
**Reply ID:** ARCH-007
**Task:** AUDIT — remaining thin-skin and table identity candidates

READ-ONLY. Inventory actual all in-repo skins with source paths/methods: action creation/registration, business services, model writes, configuration owned by skin, control reads concrete skin, eager feature nodes/properties. Classify legitimate visual work versus behavioral ownership, prioritize by data risk and measured absolute allocations. Include actual text measurement checkExcessTextLength allocation 2.3–3 kB/check and CSS stacks from PERF-009; do not add cache, change stylesheet or measurement semantics, propose only supported next experiment. CTRL-108 equal-distinct marker permutation counterexample separate correctness issue: reproduce original visible identities, establish whether underlying JavaFX TableRow/Cell isItemChanged/equals/list update or framework proxy is responsible using public API/source/bytecode, no private access, no edits, no O(N) lookup or global refresh workaround. Distinguish production current behavior from experimental suppression and no foreign writes. Binder external FXEntity ownership remains UNKNOWN, no changes. Report remaining ISkinPopulatable consumers (legacy external compatibility not proven), whether future removal possible only with explicit API decision. Provide small prioritized follow-up slices, no global rewrite.

---

## MESSAGE ARCH-008

**Status:** ASSIGNED / ACTIVE COMBINED BATCH
**Timebox:** 90 minutes
**Reply ID:** ARCH-008
**Task:** VERIFY — complete architecture migration, measurements and regression

READ-ONLY. Verify actual final HEAD and chain of isolated commits across PERF-014…016 and ARCH-001…007. Full supported smoke battery at final SHA, 57 original plus each actual new mode; no opt-in Robot/unresolved CTRL-014/034/CAL-TIME-001. Record true native JVM and runner exits, all failures/timeouts, Maven -Pbuild compile, PowerShell parse, diff check. Use faithful ignored diagnostic recorder. Repeat matched stock/framework baseline 0982d211 vs final with actual build artifacts, same JVM flags, styles and 20/100/400, 5/9 where practical. Total FX allocations/time, nested skin/CSS/layout, no-menu-use vs first/repeated menu, overflow and toolbar activation; live shallow object counts and global deltas from controlled histograms if available, distinguish retained/dominators UNKNOWN. Quantify overall and individual changes only from matched evidence, not adding isolated costs. Confirm ownership, absence of duplicate handlers/menus/temporary action buttons, exact instance-state delta, supported existing API and intentionally changed pre-menu contract, explicit external UNKNOWN. List all eleven dispositions, exact SHAs, evidence, regressions, user rerun commands incl pending CTRL-104/105/106, performance results and unresolved blockers. No corporate SQL/transaction/native input/paint/MDI/WAIT readiness claims. STOP after ARCH-008.


---

## MESSAGE PERF-014A — native JVM failure differential diagnosis

**Status:** USER APPROVED / ASSIGNED — ONLY ACTIVE DIAGNOSTIC TASK.
**Timebox:** 60 minutes ceiling.
**Reply ID:** PERF-014A.
**Repo:** ssu95a/fore25.
**Production branch:** migration/java25-correctness.
**Baseline:** 0982d2112b8f8020d16f917adaab77caee29005d.
**Mailbox:** agent/codex-mailbox.
**Incident:** Reply PERF-014 at 1d99d96fa3210143bb36b68d90cb2e8f4deddf3f.

**Latest authorization overrides all earlier ACTIVE labels for execution:** combined assignment 98856c3c remains STOPPED. PERF-014 remains BLOCKED; PERF-015/016 and ARCH-001…008 remain NOT_RUN. Do not resume, publish the candidate, or modify production even if every diagnostic run passes. Separate architect/user regression gate decision required. CTRL-079 waiver does not apply.

READ-ONLY production diagnosis. Before running, verify remote refs, clean tracked baseline and local artifact availability. Preserve original target/perf-014 evidence without overwriting: candidate patch, added sources, compiled classes, matrix.csv, run-06 stdout/stderr, win-events.xml, original args/runner and hashes. Record exact candidate identity and compare source/class hashes with saved artifacts; if exact reconstruction or isolated execution cannot be established, report BLOCKED and STOP. Use separate ignored diagnostic directories/artifacts; do not contaminate baseline or candidate classpaths. No production commit, tracked test changes, dependency upgrades, system configuration changes, other repo writes, destructive Git or force push.

Incident: WrapButtonOwnership printed CTRL-055 PASS then native JVM exit -1073740791 / 0xC0000409, outer runner 1, timeout false. Windows Application Error JVM PID 15248, Java 25.0.1, ucrtbase.dll offset 0xA527E, WER BEX64 parameter 9=7, report ID 34e53bcc-5329-4b3e-8071-81fe33a32f33. Root cause UNKNOWN. Do not infer stack overflow or menu/wrapper/shutdown causality from marker or module.

Run at most 10 fresh JVMs per variant, serial alternating A1,B1,A2,B2… (A=exact production baseline, B=exact saved final PERF-014 candidate), mode WrapButtonOwnership, same original assertions, Java arguments (only necessary isolated artifact/log path substitutions, document them), JavaFX/JDK, environment, runner behavior and settings D:\Java\.m2\settings.xml. Use faithful ignored recorder preserving actual child exit immediately, and actual outer process exit. Baseline official command reference: powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\src\test\probes\Test-LegacyGuiSmoke.ps1 -WrapButtonOwnership -SettingsFile "D:\Java\.m2\settings.xml". Verify both variants use correct classes and probe; record unavoidable differences. No parallel runs, arbitrary sleeps, modified shutdown, forced exit, suppressed errors, retry-until-green or weakened assertions.

For each invocation preserve variant, ordinal, artifact hashes/classpath, exact command/args, child PID and runner PID, start/end timestamps with timezone, stdout/stderr, PASS marker, native signed/hex exit, outer exit, timeout and matching Windows Application Error/WER/hs_err if present. Absence of hs_err does not prove absence of native failure. Inspect available original WER evidence read-only; no enabling dumps or changing registry without separate approval. On first repeated native failure OR other unexplained nonzero/timeout, capture available evidence, stop immediately (remaining runs NOT_RUN), do not retry. If all 20 focused runs finish 0/0, report NOT_REPRODUCED, not FIXED; next proposed step is replay of original regression sequence and full regression under separate gate authorization, not part of this assignment. If timebox expires, report actual counts and incomplete scope without calling it green. Distinguish failure on baseline versus candidate, neither alone proves root cause. Restore/verify clean tracked baseline and unchanged remote production at completion.

Publish exactly one Reply PERF-014A in docs/codex/outbox.md on mailbox branch, with actual matrix, native/outer exits, artifact verification, environment and commands, original and new evidence locations, confirmed facts vs hypotheses/UNKNOWN, remaining NOT_RUN, recommendation and final production SHA. Git/GitHub operations necessary within this diagnostic/mailbox scope approved. STOP after reply; no self-assignment, no production push, no regression gate waiver.


---

## MESSAGE PERF-014B — replay and full differential regression gate evidence

**Status:** USER APPROVED / ASSIGNED — ONLY ACTIVE DIAGNOSTIC TASK.
**Timebox:** 90 minutes ceiling, not a minimum.
**Reply ID:** PERF-014B.
**Repo:** ssu95a/fore25.
**Production branch:** migration/java25-correctness.
**Baseline:** 0982d2112b8f8020d16f917adaab77caee29005d.
**Mailbox:** agent/codex-mailbox.
**Prior evidence:** Reply PERF-014 at 1d99d96fa3210143bb36b68d90cb2e8f4deddf3f; Reply PERF-014A at 31105963e3170cc45aca56071e02c28b01747a41.

**Latest authorization:** PERF-014A architect APPROVED as diagnostic evidence only, NOT_REPRODUCED (A 10/10 and B 10/10 native/outer 0/0). Root cause UNKNOWN. Combined assignment 98856c3c remains STOPPED, PERF-014 BLOCKED, PERF-015/016 and ARCH-001…008 NOT_RUN. This task collects gate evidence only; no automatic gate waiver or production publication/resumption even if entirely GREEN.

READ-ONLY production. Verify fresh refs and clean tracked baseline, exact frozen A/B class trees and original candidate identity against PERF-014A manifests, original incident artifacts preserved. A manifest SHA-256 a63c199e296be6972ebd523f41966b4f3081f1aa50f85c5bb536acb1493a35b9; B 010b980d40a014e55692c539ca7f24a75e6087ddd40576a6b026a7207798fe61; saved candidate.patch SHA-256 2fa7e590c16b3178cfedda393f78265ffd069be234d1749c6fcf0a0a20cd9b93. If exact artifact identity or isolation cannot be established, BLOCKED and STOP. Reuse the proven faithful ignored recorder and archived candidate-specific probe sources/runner. Keep all new evidence in separate target/perf-014b directories; no overwrite of PERF-014 or PERF-014A. Do not apply candidate sources to tracked production or rebuild/replace the frozen candidate class tree. Compile test probes in isolated output as needed using their exact applicable sources; preserve all assertions, including intentional candidate-only lazy menu contract. Explicitly document hashes, source/probe differences and classpath substitutions. Same JDK/FX/Windows/Maven/settings as original. No dependency/system/shutdown changes, arbitrary sleeps, forced exit, error suppression or retry-until-green.

Execute in order, serial fresh JVMs:
1. Replay the original PERF-014 final regression invocation on B with the same focus, runner semantics and mode ordering: TextFields, TextFieldSkinDisposal, ValueFields, LovIntegration, ExternalLovReadOnly, WrapButtonOwnership, CurrencyLovDelayedCompletion, FormLifecycle. Inspect archived regression.ps1 and original args first; preserve any original focused preamble and distinguish it from eight modes. Record exact command and any necessary isolated path substitutions.
2. Full A regression: the 57 supported original modes from PERF-012 production gate, same assertions and mode order. Verify actual manifest against historical report/runner; no opt-in Robot or unresolved CTRL-014/034/CAL-TIME-001.
3. Full B regression: same 57 modes plus TextFieldContextMenuLifecycle (58 total). Use exact final candidate probe versions, document any authorized pre-request menu contract differences versus baseline, no assertion weakening. Verify mode inventory and counts before execution. Candidate-specific focused mode must actually run.

Each invocation records variant, stage/mode, exact args/command, dependency/probe/class identity, child/runner PID, start/end timestamps with timezone, stdout/stderr, assertion marker, true native signed/hex exit captured immediately, independently captured outer exit, timeout, matching Windows Application Error/WER and hs_err evidence. Zero matching events means no observed matched event, not proof of no native issue. No parallel runs. Any unexplained nonzero/timeout/assertion failure or infrastructure/identity issue: preserve evidence, STOP immediately, all remaining runs NOT_RUN. No automatic retries. If 90-minute ceiling expires, report actual coverage and pending modes, not full green. If all stages pass, report regression evidence GREEN and original incident NOT_REPRODUCED / root cause UNKNOWN; recommend separate architecture/user gate decision only. Original CTRL-079 waiver not applicable.

Verify unchanged baseline and remote production HEAD, clean tracked checkout, original artifacts unchanged. Publish exactly one Reply PERF-014B in docs/codex/outbox.md on mailbox branch with complete per-stage matrix or explicit incomplete coverage, actual child/outer exits, artifact checks, original sequence fidelity, commands and raw evidence locations, confirmed facts vs UNKNOWN, remaining NOT_RUN and proposed gate disposition. Git/GitHub operations necessary within diagnostic/mailbox scope APPROVED. No production commit/push, no other repo writes, no destructive commands. STOP after reply regardless of result; no self-assignment or resume.


---

## DECISION PERF-014 — resume combined architecture migration

**Status:** ARCHITECT APPROVED / USER APPROVED — regression gate cleared for the original PERF-014 incident only; combined assignment 98856c3c RESUMED.
**Production baseline:** 0982d2112b8f8020d16f917adaab77caee29005d.
**Evidence:** Reply PERF-014A 31105963e3170cc45aca56071e02c28b01747a41 (20/20 native/outer 0/0); Reply PERF-014B 00ba8ebd6b3e703afa1ae39e3d4716650727ab5c (13/13 original replay including five focused invocations, 57/57 A, 58/58 B; 128/128 native/outer 0/0, no timeout or matched Windows crash event). Total 148/148 diagnostic invocations after the original incident. Original 0xC0000409 failure CONFIRMED, root cause UNKNOWN, NOT_REPRODUCED; not FIXED. Agent verified frozen artifact identity and original evidence preservation. User explicitly accepts residual risk and authorizes publication and continuation. Preserve original evidence; no claim that native defect is resolved or blanket waiver for any new failure.

**Latest authorization supersedes diagnostic STOP only:** complete PERF-014 from the exact saved final candidate; then PERF-015, PERF-016 (mandatory GREEN intermediate gate), ARCH-001…008 in previously approved order and task scopes, ceilings and stop rules. No new confirmation for these already approved slices. No restart of PERF-013 or completed diagnostics. Before PERF-014 publication verify fresh refs and clean tracked baseline; verify saved patch SHA-256 2fa7e590c16b3178cfedda393f78265ffd069be234d1749c6fcf0a0a20cd9b93, B class manifest 010b980d40a014e55692c539ca7f24a75e6087ddd40576a6b026a7207798fe61, source post-images, added sources and exact tested candidate probe/runner versions. Restore only the intended verified candidate production/test changes from archives; review all tracked diff, scope, Russian contract comments, and assertion changes against original PERF-014 authorization. Rebuild from restored sources with original environment, verify resulting classes match frozen tested artifacts (if any mismatch establish exact benign reproducible compiler metadata difference or STOP and report; no untested functional delta). Maven compile, PowerShell parse, git diff --check, three fresh focused JVMs plus original official focused runner with actual child/outer exit evidence and relevant affected regression per original assignment. Do not repeat 148 diagnostic invocations merely as a ritual; rely on archived full regression only for verified identical candidate. Any new unexplained nonzero/timeout or artifact mismatch/dirty tree/unexpected remote advancement: STOP; no waiver. Publish one isolated fast-forward PERF-014 production commit only after validation, verify remote SHA, clean tree, publish separate Reply PERF-014-FINAL (do not overwrite original PERF-014 or diagnostic reports), include exact scope, SHA, tests/exits, artifact equivalence, approved residual risk and known candidate performance tradeoffs. User independent acceptance remains separate. Continue PERF-015 only after successful publication; follow original per-task Reply and commit rules. PERF-016 must pass before ARCH-001. STOP after ARCH-008 or any shared failure.

**User-approved architecture clarification for ARCH-001/003/004/006 (no scope expansion): Control owns behavior. Skin owns presentation.** Two TextField button roles are distinct. Functional innerButton (LOV/FE/custom) is configured and selected by Control; Skin attaches/renders/delegates. Overflow button is a visual affordance for insufficient text viewport, not a second functional innerButton. Keep overflow visual node and geometry in Skin; Skin evaluates available visual width/overflow using actual text/font/insets/functional button layout and reacts to changes; actual full-editor action belongs to Control and Skin delegates to it. Preserve distinct existing behavior, including external button suppressing overflow, simultaneous LOV and overflow, readOnly full-text access and custom FE action semantics as established by tests. Do not merge buttons or silently replace public observable node class/identity. Do not assume overflow click and configured inner FE action are interchangeable: characterize actual paths and preserve their supported differences. Remove concrete-skin internals dependency in JInvTextField.checkExcessTextLength() within relevant existing slices if compatible; preserve public API and tooltip behavior, use minimal presentation query/delegation or equivalent without new eager per-instance state; do not move visual measurement/layout into Control. Preserve stock TextField skin compatibility. If a compatibility issue needs intentional contract change, report separately; no unauthorized change to immediate overflow node existence/class, wrapper public API, CSS/lookup or external/inner precedence. ARCH-006 can optimize exact class path under prior approved constructor contract; alternative lightweight visual node is investigation only absent separate API approval. Keep performance claims measured and distinct from retained heap. No additional task or parallel production changes.

All original constraints remain in force: main/JInvCommon read-only, no other repo writes, no private JavaFX API/reflection, no destructive Git, no dependency upgrades, no blanket regression waiver. Agent owns production implementation and tests; architect mailbox only.


---

## USER ACCEPTANCE — ARCH-008 final package independent rerun

**Status:** USER ACCEPTED — nine focused modes reported by user as "ALL NINE TESTS PASSED" in response to the exact nine-mode acceptance procedure for final production 515d01fd20ba491bc39044f939191b85229b7a08. Remote production verified at that SHA, final agent Reply ARCH-008 at 418d4d8993517b23ebaaf52fad70faa4b24ec652. User did not separately provide local git SHA or individual process logs; do not fabricate those.

Accepted focused scope: PERF-014 TextFieldContextMenuLifecycle; PERF-015 TextAreaContextMenuLifecycle; ARCH-001 FEActionDelegation; ARCH-002 ButtonTargetAssociation; ARCH-003 TextFieldInnerButtonOwnership; ARCH-006 FEButtonConstruction; CTRL-104 TableAdapterBulkMarkErrorDelivery; CTRL-105 TableAdapterBulkMarkClose; CTRL-106 TableAdapterRowRefreshClose. Final agent regression 63/63 native/outer 0/0 remains separate evidence. ARCH-004 NO_CHANGE, ARCH-005 BLOCKED, ARCH-007 read-only findings remain as reported. Original native incident UNKNOWN / NOT_REPRODUCED, residual risk accepted previously; no waiver for new failures. No claim of corporate integration, retained heap, or independent full 63-mode user rerun.

Combined package finished with its recorded dispositions. STOP remains in force. No new task assigned, no production authorization beyond completed package. Action architecture migration is a future proposal, not an assignment.


---

## ARCHITECTURE DIRECTION — Action Engine migration

**Status:** USER APPROVED — direction and initial ACT-001/002 audit/characterization only. Implementation ACT-003 onward NOT ASSIGNED.
**Repo:** ssu95a/fore25.
**Production branch:** migration/java25-correctness.
**Baseline:** 515d01fd20ba491bc39044f939191b85229b7a08 (ARCH-008 agent GREEN 63/63; user independent nine-mode acceptance recorded at 93a172f0).
**Reference repository READ-ONLY:** ssu95a/fore26_Arch, main pinned to c34fffdc7f48c791cd2ec47a14b76915aa61565c, src/main/java/ru/inversion/fore/form/action (ForeAction, ForeActions, StandardAction, ActionKeyBinder, IconSpec, IconFont). Read actual reference sources and relevant tests/resources, not just this summary. Do not edit or push to reference repo. ControlsFX 11.2.6 already present in Fore25 pom; inspect actual locally resolved artifact/source/bytecode, no dependency upgrade.

User intends to replace legacy action internals with the new architecture inspired by Swing Action and ControlsFX, retaining existing public/protected interfaces as compatibility facades. One eventual execution/registration authority, not permanently duplicated engines. New core must not depend on legacy facade. Action is a SMALL container/bridge between GUI and functionality: operation description, observable state, presentation rules and invocation entry point, not UI component owner, controller/task/WAIT manager or a service implementation. JavaFX properties are acceptable; actual GUI nodes must not become general action state. Existing ForeAction extends ControlsFX Action and uses setGraphic(icon.newGraphic()); user explicitly leaves icon materialization unresolved and tolerates current Node graphic as pragmatic exception pending later decision. Do not replace inheritance with composition or introduce icon/Node cache, manager, extra per-action/per-control state or graphic mappings in this batch. Preserve immutable standard descriptors versus independent runtime instances. Action is optional where a simple control method suffices. Distinguish responsibilities without assuming each needs an allocated object. Control owns behavior; Skin owns presentation remains in force.

Compatibility includes existing source and binary clients, external subclasses/overrides, constructors, JVM descriptors, public/protected fields and nested types, FXML/reflection where actually used, mutable properties/collections, event semantics, real security and lifecycle. Preserve current supported behavior by default; characterize surprising behavior separately, never promote every incidental defect into an approved future contract. No blanket permission to change shortcut precedence, security, thread admission, nextAction sequencing, mutable catalog semantics, or public API. External corporate consumers UNKNOWN.

## BATCH ACT-001…ACT-002 — Action subsystem contract discovery

**Status:** ACTIVE / ASSIGNED; only these two tasks authorized.
**Budget:** ACT-001 90 minutes; ACT-002 120 minutes; ceilings, not minimum durations.
**Order:** ACT-001 then ACT-002. Both READ-ONLY production; ignored local probes/reports only under target/act-001 and target/act-002. No tracked source/test/build/resource modifications or production commits. No other repository writes; main, JInvCommon and fore26_Arch read-only. Git/GitHub operations necessary for reading and mailbox replies approved. Never change corporate DB/security state or launch real external editor processes. No private JavaFX APIs/reflection for implementation, no dependency upgrades, no destructive Git. Verify fresh refs and clean tracked checkout before/after each task. Stop on dirty/conflict/unexpected remote advancement, unexplained nonzero/timeout, or unreliable shared infrastructure; preserve evidence; remaining tasks NOT_RUN. Expected isolated characterization differences or explicit unsupported offline setup must be documented, not hidden or called green; continue independent cases only on verified clean baseline with reliable runner. No blanket native incident waiver. One Reply per task in docs/codex/outbox.md. STOP after ACT-002; no ACT-003 implementation or self-assignment.

---

## MESSAGE ACT-001

**Status:** ASSIGNED / ACTIVE.
**Timebox:** 90 minutes.
**Reply ID:** ACT-001.
**Task:** READ-ONLY full legacy action API, consumers and new-core gap inventory.

Use actual source at 515d01fd and compiled baseline classes/jar where available. Inventory ru.inversion.fx.form.action including IAction, JInvAction, public inner JInvAction.JInvTask, ActionBuilder, JInvParallelAction, TaskHandler, JInvEvent/JInvActionEvent, decorators, SceneKeyManager, SceneKeyBinder, JInvKeyboardManager; ActionFactory including ActionTypeEnum/IconEnum and all public/protected overloads, legacy icon descriptors, and actual action-consuming controls, controllers, dataset adapters, toolbar/menu/keyboard bindings, app services, security, ThreadPoolManager and WAIT lifecycle. Follow actual calls with rg; list path/method/line and categorized consumer counts, direct implementations/subclasses, anonymous overrides, concrete casts, retained identities, direct collection mutation, public/protected hooks, inherited signatures and FXML/config usage. Record public/protected API with exact javap -s/-protected (or equivalent descriptor inventory), nested classes, fields, enum names/order and constructor signatures; distinguish bytecode evidence vs source-only evidence. Preserve API inventory locally for future binary checks. Include existing third-party/custom IAction implementations, optional enabledProperty, default methods, direct handle()/handle(null), handler replacement and decorators. Do not treat only JInvAction as all IAction.

Map existing contracts and actual paths:
- metadata title/description/tooltip/icon including deprecated/Oracle Forms mappings, custom and standard actions, catalog ActionTypeEnum.getAction shared mutable identity, default hotkeys and localization, ActionBuilder reuse/copy and overload behavior, nullability;
- requested enabled vs security admission vs visual disabled/hidden behavior; JInvSecurityService strategies, security IDs, cached/rechecked decisions, direct vs keyboard vs button/menu paths, event source/target/JInvEvent payload and error routing;
- synchronous/parallel paths, TaskHandler/JInvTask public API, invocation event and actual thread, handlerWithNextAction/nextAction/forceFxThread, WAIT add/remove, message binding, success/fail/cancel/rejected submission, overlapping invocation and form close ownership; distinguish observed source risk from reproduced defect;
- keyboard resolution Node/CONTROL_PARENT/parent/DataSet/table/global fallback, focus owner, filter vs handler, key event types and consumption, disabled/security-denied shadowing, duplicate hotkeys and registration ordering, mutable lists/maps returned publicly, null/default keys, add/remove/detach/scene movement, existing accelerators and double dispatch, multiple scenes/MDI;
- action presentation factory/decorator binding ownership, existing JInvButton/legacy control return types, multiple views sharing action state, graphics/tooltip construction and release, no unnecessary actions for simple control behaviors.

Read pinned fore26_Arch action sources, relevant tests/resources/FormTools and actual ControlsFX 11.2.6 API/bytecode as necessary. Produce explicit mapping legacy contract -> new implementation present/missing/incompatible/UNKNOWN. Include confirmed differences: five standard actions vs legacy catalog; REFRESH F8 vs no legacy catalog default; mandatory StandardAction vs arbitrary custom action; immutable hotkey list vs legacy exposed mutable list; setAccelerator vs setHotkeys authority; writable Optional<BooleanProperty> enabled vs ControlsFX disabled; ActionKeyBinder scene-wide overlap rejection/KEY_PRESSED/filter/consumes disabled matches vs contextual legacy behavior; event target/source; graphic Node compromise; no new security/parallel/WAIT contract yet. Verify actual code before asserting. Identify candidate facade boundaries and minimal experiments needed, not final design or implementation. Note new engine cannot require every legacy operation to have StandardAction and cannot silently alter external subclass dispatch.

Deliver Reply ACT-001 with prioritized contract matrix, exact source and bytecode evidence, compatibility traps, actual unresolved policy decisions, proposed ACT-002 probe coverage, local API inventory location, and confirmed production unchanged. Large detailed inventories may stay in ignored files but report sufficient decision-critical detail in mailbox, not just local paths. Do not assign further tasks.

---

## MESSAGE ACT-002

**Status:** ASSIGNED / ACTIVE after ACT-001.
**Timebox:** 120 minutes.
**Reply ID:** ACT-002.
**Task:** READ-ONLY characterization and compatibility feasibility experiments.

At same verified baseline use existing tracked smoke runner where applicable and isolated ignored minimal probes compiled against actual production classes and ControlsFX. No source edits even to fix discovered defects. Build with existing JDK25/FX25, Maven offline settings D:\Java\.m2\settings.xml, original runtime options. Reuse faithful recorder with immediate native JVM signed/hex exit, actual outer runner exit, PID/timestamps, stdout/stderr, timeout and matching WER on abnormal exit; no retries to green, no forced System.exit(0), arbitrary sleeps, or weakened assertions. Use deterministic FX barriers/latches for worker scenarios, no blocking FX thread. Keep failed attempts and characterize unsupported cases explicitly. Focused new probes may live under target/act-002; save source and commands. Prefer a compact set of independently runnable focused fixtures over running whole 63-mode suite without code changes. Baseline Maven compile and existing SceneKeyLookup, FormLifecycle, FormCancel, WrapButtonOwnership and other directly applicable modes as time permits; record actual coverage.

Prioritize executable evidence:
1. Legacy runtime metadata/defaults and mutation: independent instances versus catalog shared identity, custom IAction and subclass override calls, ActionBuilder reuse/copy/null behavior, mutable hotkeys, constructor/event defaults; preserve title/tooltip/description distinction, direct invocation and original event identity. Verify whether each is actual observable contract or just a surprising implementation detail.
2. Enabled/disabled property bridge experiment (ignored only): ControlsFX actual API, legacy writable BooleanProperty identity, bind/unbind, bidirectional binding, change/invalidation notifications and write rejection under binding; no two independent authorities or recursive listener loop. Report if lossless bridge needs legacy property to remain canonical and why. Verify requested enabled separate from security state. No API decision on bridge until review.
3. Keyboard dispatch matrix on shown offline scene: same F4 in different scopes, child/focus/CONTROL_PARENT/table and global fallback, duplicate registrations, filter/handler, pressed/released, disabled or denied first candidate vs eligible second, existing accelerators, dynamic keys and direct public map/list removal, exact execution and consumption counts, original JInvEvent payload, scene detach/close. Reuse SceneKeyLookup. Contrast new binder behavior using actual reference sources in isolated fixture only if feasible without tracked changes or dependency changes; otherwise report source-derived gap. Do not impose strict Scene conflict rule on legacy.
4. Security and execution: verify actual strategies, no-handler or disabled paths, exception routing, synchronous vs worker thread, original Task/event/nextAction behavior, WAIT/message binding cleanup, success/failure/cancel/rejection and overlapping calls, form close/late delivery. Use existing supported offline fixtures/seams only, no actual corporate auth, SQL or external editor process; do not bypass security to claim coverage. If no valid offline seam, identify smallest required future boundary and mark UNKNOWN/BLOCKED per case.
5. Presentation: one action used by two real visual views, enabled/text/tooltip/icon updates, event source, detach/replacement, duplicate graphic ownership and existing action factory binding semantics. Measure selected representative action/view allocations or shallow state only if useful and time permits; no new cache and no icon contract decision. A small precompiled legacy consumer fixture (including subclass/overrides, public inner class use if applicable) is desirable for future binary regression; preserve baseline .class files and exact ABI inventory, not evidence of future binary compatibility.

Use ACT-001 priorities to fit ceiling, report per-case observed results, true exits, source-only vs runtime evidence and explicit NOT_RUN/UNKNOWN. No need to prove every historical behavior correct; distinguish bugs/desired changes from compatibility constraints requiring user decision. Deliver Reply ACT-002 with actual characterization matrix, raw paths and reproducible PowerShell commands, findings on bridge feasibility, proposed single-core migration boundaries, highest-risk contract gaps and minimal next implementation slice. No production changes, no new tracked tests, no new dependency, no action core rewrite, no ARCH-005/CTRL-108 work. Verify remote baseline unchanged and clean tracked tree. STOP after reply regardless of outcome; wait for architect/user decision.


---

## DECISION — Action Engine implementation combined package ACT-003…ACT-011

**Status:** USER APPROVED / ACTIVE — execute entire nine-task package sequentially; no routine confirmation between scoped tasks.
**Repo:** ssu95a/fore25.
**Production branch:** migration/java25-correctness.
**Mailbox:** agent/codex-mailbox.
**Starting production:** 515d01fd20ba491bc39044f939191b85229b7a08.
**Discovery evidence:** ACT-001/002, latest Reply 3f69f5e3ab42c2709273eb62dfe051df753801f1, architect accepts discovery scope. Baseline API/probe evidence under target/act-001 and target/act-002. Preserve exact original ABI and precompiled consumer artifacts; copy them to separate ignored evidence directory before builds if necessary. ACT-002's seven focused cases and five existing modes finished 0/0, with documented initial fixture errors and recorder collision; corporate security and other specified gaps remain UNKNOWN.
**Reference READ-ONLY:** ssu95a/fore26_Arch, pinned c34fffdc7f48c791cd2ec47a14b76915aa61565c, package ru.inversion.fore.form.action. ControlsFX 11.2.6 already resolved; no dependency upgrade.
**Order:** ACT-003, 004, 005, 006, 007, 008, 009, 010, 011.
**Budget ceilings:** 90 + 90 + 120 + 120 + 150 + 180 + 120 + 120 + 90 = 1080 minutes total (18h). Timeboxes are ceilings, not targets. Report incomplete coverage rather than invent success.

### Architecture and compatibility invariants (binding across all slices)

Action is a small bridge between functional operation and GUI, with description, observable requested state, presentation rules and invocation entry point. It is not the worker, WAIT controller, Scene registry, view owner or business service. ForeAction extends actual ControlsFX Action as in the reference; icon descriptor and graphic Node are the currently tolerated pragmatic exception, materialization question explicitly deferred. No new icon Node cache or composition rewrite. Immutable standard descriptors; independent mutable runtime actions; support custom operations without a StandardAction. No compulsory per-control Action, executor object, context or manager for a simple method. Stateless executor methods and per-invocation context only where needed; avoid global retention. Core must not import or depend on legacy IAction/JInvAction, ActionFactory or compatibility facade. Keep Control owns behavior / Skin owns presentation. Prefer reuse of existing icon/service infrastructure without dragging legacy public facade types into core; avoid duplicate font infrastructure without evidence.

One eventual execution and shortcut resolution authority; incremental legacy entry points can remain until migrated and validated, explicitly list transitional routes and remove duplicate framework dispatch before claiming completed migration. Arbitrary external IAction implementations and virtual override bodies remain legitimate extensions, not forced to become ForeAction or silently bypassed. Preserve public/protected FQNs, signatures, JVM descriptors, constructors, superclass/interface relationships, fields, enum names/order, nested JInvAction.JInvTask and TaskHandler, public mutable collection identities/semantics where supported, legacy event payload and subclass dispatch. Do not add abstract interface methods or alter overload resolution for old clients. Use compiled ACT-002 legacy consumer fixture unchanged and compare ACT-001 ABI (including inherited members) at each relevant step. New API need not copy legacy quirks. No sweeping renames, removal of deprecated methods, or silent source/binary incompatibility. Actual external application compatibility remains UNKNOWN even with passing fixture. Legacy API preserved as facade, not a permanently separate engine.

Preserve legacy requested enabled separately from security admission and effective view presentation. Legacy writable BooleanProperty remains canonical where exposed; ControlsFX disabled may be a bound read projection for facade-owned instances, but do not expose two independently writable authorities for same state. Native new actions retain ordinary ControlsFX state semantics. No mandatory extra state per action without measured/functional justification. Preserve direct handle(null), event source/target, custom handler identity/virtual dispatch, existing synchronous caller/thread contract, error routing, keyboard filter/handler and event phase. Do not change legacy default keys, catalog mutation, builder reuse, disabled/denied shadowing, tooltip snapshot behavior, or custom button/scene ownership merely to make new API cleaner. For new API establish coherent accelerator/hotkey policy, characterize inherited setter limitations; do not silently claim they are synchronized.

Security fail closed; preserve real service/TaskContext calls, IDs, strategy and error/visibility semantics. No fake corporate authorization, permissive fallback or swallowing denial. Parallel execution: correct reproduced WAIT/message premature release and rejection leak with per-invocation ownership if safely possible; preserve admission of overlapping invocations unless contract explicitly changed by user. Do not introduce global serialization, silent drop, task cancellation, transaction/rollback promise, or change nextAction sequencing. Preserve original Throwable, cancellation semantics, task identity and FX/worker publication boundaries. Ambiguous concurrent progress display, late result after form retirement or deferred chaining policy: characterize and keep existing supported policy where safe, report BLOCKED and request separate decision if essential behavior cannot be determined; no invented overwrite/cancel policy. No broad ARCH-005 external-editor, CTRL-108, Binder, lazy skin or unrelated work.

### Execution protocol and gates

Before each task verify fresh remote production/mailbox refs, expected predecessor and clean tracked checkout. Main, JInvCommon and fore26_Arch READ-ONLY; no other repo writes, destructive Git/force push, private JavaFX access, dependency upgrades, environment/shutdown changes, corporate DB/security mutations or external editor process. No arbitrary sleeps, suppressed errors, forced successful exit or weakened assertions. Russian production comments. Use existing Maven offline settings D:\Java\.m2\settings.xml, JDK25/FX25, actual ControlsFX. Preserve original evidence and failed attempts. Keep diagnostic probes in ignored target; add narrow tracked regression modes for implemented slices. For defects prove baseline RED with same meaningful assertions then candidate GREEN; refactors characterize equivalence, record intentional differences. Three fresh focused JVMs plus unchanged official focused runner, real child signed/hex and outer exit, timeout, PIDs/timestamps/streams; relevant regression, Maven -Pbuild compile, PowerShell parse, git diff --check. Use faithful ignored recorder; distinguish official runner outer-only from instrumented child exit. Any new unexplained native/nonzero/timeout, shared regression, dirty/conflict/unexpected advancement or unreliable gate: STOP, remaining NOT_RUN, preserve evidence, no retry until green. Prior CTRL-079/PERF-014 residual-risk decisions do not apply to new failure.

One isolated validated fast-forward production commit per implemented task; no partial/unvalidated push. Verify remote SHA and clean tracked tree, separate Reply ACT-nnn in docs/codex/outbox.md after each task, with scope, exact commands/exits, ABI delta, contract changes, evidence, allocation/state delta where relevant, residual UNKNOWN, independent Windows rerun command(s). User acceptance separate. All Git/GitHub operations within explicitly approved scope authorized. Read-only or NO_CHANGE results no production commit. Task-local contract BLOCKED may continue only an independent slice on unchanged clean green baseline with no dependency on unresolved decision; otherwise STOP dependent work. Never invent a policy just to finish the package. Mandatory intermediate full supported suite after ACT-005 and ACT-008, including 63 existing modes plus actually published new ones, ABI check against original baseline and precompiled fixture; gate failure blocks subsequent tasks. ACT-011 full final gate. No repeated whole-suite run merely as ritual outside required gates; do not skip gate due to timebox, report incomplete and stop. STOP after ACT-011 or earlier blocking condition; no self-assignment.

---

## MESSAGE ACT-003

**Status:** ASSIGNED / ACTIVE.
**Timebox:** 90 minutes.
**Reply ID:** ACT-003.
**Task:** Introduce compact new action model from pinned reference (additive, no legacy consumer switch).

Read six reference classes and actual ACT-001/002 evidence. Establish coherent new namespace (recommend reference ru.inversion.fore.form.action for forward compatibility if no collision; justify exact choice, no legacy package moves). Bring ForeAction, ForeActions, StandardAction and required descriptor/icon support, use existing ControlsFX 11.2.6. Keep current inheritance, no new cache, no eager execution machinery. Support nonstandard/custom action without dummy StandardAction; define clear defaults, independent runtime state, immutable metadata/default lists, optional graphics. Preserve reference standard five descriptors including new API REFRESH F8, but do not apply that to legacy default. Reuse existing font/resources if feasible; do not touch legacy catalog/icon behavior. Do not force legacy actions to adopt immutable list or newer caption defaults. Investigate inherited accelerator setter vs hotkeys; report supported new contract rather than falsely guaranteeing consistency, no private hacks. New API names/signatures deliberately minimal; no generic abstract policy hierarchy. Add focused tests for defaults, custom action, independent runtime instances, properties, graphic ownership/duplicated views and normal ControlsFX execution/disabled behavior; verify actual Button/MenuItem views and unconfigure if applicable. Track instance footprint and avoid unnecessary eager validators or extra manager. No keyboard binder migration until ACT-008. Preserve old compiled consumer fixture/ABI; baseline regression smoke applicable. One additive commit if validated, otherwise BLOCKED.

---

## MESSAGE ACT-004

**Status:** ASSIGNED / ACTIVE after ACT-003.
**Timebox:** 90 minutes.
**Reply ID:** ACT-004.
**Task:** Establish one ordinary synchronous execution path, independent of legacy types.

Extract only genuinely shared execution responsibilities into small stateless executor or equivalent, use ForeAction invocation as adapter to this path. Preserve ControlsFX native disabled guard and original ActionEvent semantics; no conversion of null to synthetic event unless new API explicitly specifies it, legacy null preserved. Keep functional handler separate from presentation; no Task/WAIT/Scene/Controller fields in Action. Define narrow admission/error hook only when supported by actual subsequent consumers, not hypothetical framework. Prove direct, real button/menu, repeated invocation, disabled, handler replacement/exception behavior and correct event identity. No extra context allocation on ordinary execution if not needed. Keep legacy routes unchanged until ACT-005, explicitly record transitional routes. Focused test plus related modes and original ABI fixture. No security/parallel/keyboard policy changes. One isolated commit if meaningful; if ACT-003 already provides equivalent common path, NO_CHANGE with evidence and proceed to ACT-005.

---

## MESSAGE ACT-005

**Status:** ASSIGNED / ACTIVE after ACT-004.
**Timebox:** 120 minutes.
**Reply ID:** ACT-005.
**Task:** First legacy facade slice and mandatory intermediate gate.

Use actual JInvAction/IAction and ACT-002 custom/override fixture. Preserve public/protected ABI, existing superclass relationship and public nested task. Route ordinary synchronous non-secured built-in legacy path into the shared invocation core without requiring external callers to opt in or change source; transition only paths proved equivalent. Preserve override dispatch, handler identity, null event, requested enabled, exception routing and nextAction behavior as characterized; leave secured/parallel/chaining variants on existing adapter temporarily if not proved safe. No permanent duplicate execution implementation; report remaining route inventory. Do not eagerly allocate a second ForeAction per JInvAction just to satisfy facade pattern; prefer compact shared invocation/state abstraction and lazily adapted presentation if needed. If wrapping actual native Action, legacy enabled canonical projection from ACT-002; bound/write rejection, listener notification ordering and property identity must survive, no independent writable disabled handle leaked. Avoid mirroring through two mutable properties or bypassing external custom IAction. Compare original ABI and run original precompiled consumer classes without recompiling, plus direct, custom, disabled, null, handler replacement, builder reuse and property characterization. Add focused tracked mode. Gate: full 63 original modes + all newly added, compile/parse/diff and ABI checks, record matrix. Publish only validated scoped change, then verify mandatory gate at exact candidate/published SHA. No ACT-006 until GREEN and ABI-compatible. No unrelated change.

---

## MESSAGE ACT-006

**Status:** ASSIGNED / ACTIVE after ACT-005 GREEN.
**Timebox:** 120 minutes.
**Reply ID:** ACT-006.
**Task:** Separate security admission and presentation policy from Action model.

Inventory and follow actual JInvSecurityService StubAction/WrapAction, JInvButton, ActionFactory, JInvAction, SecurityStrategyEnum and TaskContext usage. Centralize shared admission/dispatch without legacy types in core; existing security service and strategy mapping remain in facade or narrow adapter. Requested enabled is distinct from authorization and DISABLE/HIDE presentation. Preserve cached/recheck timing, denied behavior, error identity and routing, ID/default strategy behavior, custom IAction overrides, direct vs keyboard semantics (selection/consumption unchanged until ACT-008), visibility/disable bindings, genuine checks for every route. No new bypass, no fake security context; actual production authentication remains UNKNOWN. Use existing offline supported synthetic deny/allow fixtures and explicitly differentiate observed real code from stubbed service policy. Test enabled false, allow/deny, strategy variants where available, recheck/ID change and original event, same action in multiple views, no execution when denied, no accidental permission escalation. If essential real service contract cannot be characterized safely, keep existing real admission route through adapter and report limitations; no speculative security rewrite. One narrow commit if safe. ABI and relevant security/control/keyboard/form regression.

---

## MESSAGE ACT-007

**Status:** ASSIGNED / ACTIVE after ACT-006.
**Timebox:** 150 minutes.
**Reply ID:** ACT-007.
**Task:** Per-invocation parallel execution, WAIT/progress ownership and cleanup.

Use actual ThreadPoolManager, JInvAction.JInvTask, TaskHandler, IFormStateListener and controller WAIT collections. Preserve existing JInvParallelAction constructors, public Task type, original event, handler/nextAction/forceFxThread semantics, message properties and real executor/TaskContext contract. Move orchestration outside small Action model, only allocate invocation state for admitted worker where required. Reproduce ACT-002 defects with deterministic actual worker/latch fixtures: overlapping same Action prematurely removes WAIT/unbinds message while second active; submission rejection leaves WAIT/message despite no work. Correct exact ownership and submission failure cleanup (including original Throwable and suppressed cleanup if applicable). Preserve overlapping admissions rather than silently serializing/dropping; WAIT must remain until all associated active invocations have relinquished it. Respect actual form WAIT API and external observers; avoid changing public wait-action collection identity/contents without characterization. Cleanup must not unbind another invocation's property; if overlapping progress-selection semantics unclear, preserve demonstrable existing latest binding behavior where possible without stale cleanup, report unresolved policy separately, no invented global progress queue. Capture per-invocation ownership, success/fail/cancel/rejected submission, FX delivery, reentrant observable changes, no FX blocking or locks across worker I/O. Do not claim worker is stopped merely because Task.cancelled callback occurs; preserve cancellation and business completion contracts. No automatic rollback or late overwrite/cancel policy. Characterize nextAction deferred ordering, failure routing and actual form close with pending action where supported; if unsafe/unknown, retain compatible boundary and BLOCKED rather than broad fix. Actual offline local executor rejection fixture only, never shut down corporate runtime. Preserve public nested class ABI and precompiled TaskHandler consumers. RED/GREEN both confirmed defects, exact wait/message/error counts and threading, original and custom handlers, success/fail/cancel/reject/overlap, existing worker and form regression. One validated commit or safe independent partial disposition with explicit unimplemented defects; do not declare completed executor migration while legacy path unaccounted.

---

## MESSAGE ACT-008

**Status:** ASSIGNED / ACTIVE after ACT-007.
**Timebox:** 180 minutes.
**Reply ID:** ACT-008.
**Task:** One contextual shortcut resolution authority, legacy keyboard facades, second mandatory gate.

Adapt reference ActionKeyBinder principles (exact own registrations, FX thread/lifecycle, close) without copying its incompatible Scene-wide conflict rule onto legacy. Build minimal unified resolution/dispatch path, separate lookup from admission, adapt existing SceneKeyManager/SceneKeyBinder/JInvKeyboardManager API. Preserve actual legacy ordered mutable Pair lists/HashMaps and public constants/accessors, direct mutations visible immediately; do not copy into stale registry or install duplicate competing filter sets. Preserve Node/CONTROL_PARENT/visual parent/DataSet table/global fallback order, first matching action before eligibility, disabled/denied shadowing and unconsumed event for legacy, default RELEASED vs builder PRESSED, explicit PRESSED/RELEASED/TYPED and FILTER/HANDLER, JInvEvent/JInvActionEvent payload, source/target, scene/global fallback and navigation. New API may use reference conflict validation in explicitly appropriate new scope but must define interaction with legacy without stealing commands or double fire. No unapproved change of reserved-vs-fallback policy. Preserve native accelerators unrelated to engine; distinguish consumed new disabled matches vs legacy unconsumed denied matches. Handle registration removal/detach/reparent/scene close and held actions without stale refs, no new global scene cache. Check externally exposed mutable map/list behavior (including in-place edits, iterator removal, same keys different scopes, insertion order, direct map update) and existing two legacy callback APIs; if impossible to unify without breaking supported contract, report exact blocker and do not install a second permanent dispatcher. Characterize native accelerator plus filter/handler ordering with actual shown Scene; multiple scenes/MDI and dataset table fallback where supported. Add focused tests for exact selection/consumption/event payload, dynamic keys, duplicates, disabled/denied shadowing, logical parent, attach/detach/close, native accelerator coexistence, callback maps, custom IAction. Use existing SceneKeyLookup, WrapButtonOwnership, TextAreaToolbarOwnership, form, LOV/navigation and table regression. ABI/precompiled fixture and full 63 original + published new modes mandatory gate. No ACT-009 until GREEN and ABI-compatible. One scoped commit if supported.

---

## MESSAGE ACT-009

**Status:** ASSIGNED / ACTIVE after ACT-008 GREEN.
**Timebox:** 120 minutes.
**Reply ID:** ACT-009.
**Task:** ActionFactory and ActionBuilder compatibility facades.

Map all legacy overloads, exact return types/classes, public/protected signatures, ActionTypeEnum/IconEnum order/names, deprecated Forms icons, resources and custom metadata. Keep legacy shared mutable catalog getAction identity and downstream default mutation semantics at facade boundary, not as core runtime standard descriptors. Preserve builder reuse and concrete-copy/default/no-op constructor observations pending explicit change approval; do not silently fix unrelated surprising metadata behavior. Legacy default REFRESH keys remain as before, not new F8. Preserve custom nulls, tooltip suffix/snapshot, existing enabled binding/visibility/security policy, specialized controls and FXML linking, exact caller event source, graphic ownership. Internally delegate common operation creation and presentation where compatible to new architecture/ControlsFX, not blindly replace JInvButton with stock Button or legacy snapshot with live bindings. Do not remove legacy icon machinery or settle deferred icon-materialization question. Characterize all actual factory families and decorators, representative precompiled consumers, custom overrides, independent runtime instances, shared catalog identity, null paths, two views and lifecycle unconfiguration. Keep legacy facade free of duplicated core policy; document unavoidable legacy adapters. Measure representative action/button/menu allocations, no claim of savings absent evidence. One validated scoped commit; no broader API changes.

---

## MESSAGE ACT-010

**Status:** ASSIGNED / ACTIVE after ACT-009.
**Timebox:** 120 minutes.
**Reply ID:** ACT-010.
**Task:** Migrate actual internal consumers to new engine and eliminate redundant framework paths.

Use ACT-001 64-file inventory updated against current HEAD. Classify each consumer: native new API, compatible facade required, external/custom extension, or unresolved. Move only concrete internal callers where functional and binary contract preserved; do not rewrite all files just to remove the string IAction. Public/protected return/parameter types remain legacy as necessary. Keep old API entry points as delegators; preserve custom implementations/virtual dispatch and source/FXML consumers. Review JInvButton, table/tree context menus, controllers, DSFXAdapter, toolbar/menu and keyboard registrations, decorators, security wrappers, worker paths. No separate new Action for a simple FE/LOV/control method, no per-view duplicate operation state, no additional managers. Remove only verified redundant internal execution or registration implementation with no public contract loss, no deleting legacy facade types. Report exact remaining routes, dependency direction and whether one authority achieved; partial migration explicitly PARTIAL/BLOCKED where necessary. ARCH-005 external editor, CTRL-108 and other unrelated backlog remain outside scope. Focused tests on each actual migrated path, precompiled ABI fixture, relevant lifecycle regressions. One scoped commit if safe, otherwise no unvalidated push.

---

## MESSAGE ACT-011

**Status:** ASSIGNED / ACTIVE after ACT-010.
**Timebox:** 90 minutes.
**Reply ID:** ACT-011.
**Task:** Final ABI, correctness, architecture and measured footprint gate.

READ-ONLY final verification. Verify remote exact chain from 515d01fd, scope of every commit and clean tracked tree. Full supported 63 original + all new tracked modes at final SHA with actual child signed/hex and outer exits, timeouts, assertion markers, compile/parse/diff. Compare public/protected ABI inventory with original including method descriptors, fields, nested types, enum names/order, inheritance and constructors. Execute original precompiled baseline legacy consumers unchanged; add representative compatibility checks as ignored fixtures if needed, no claim of complete external compatibility. Verify actual legacy and native action paths, security admission, worker ownership/cleanup, keyboard resolution and native accelerator coexistence, no accidental double dispatch, lifecycle/unbind. Audit dependency direction and one execution/registration authority versus residual adapters, explicitly list any not yet migrated paths and blockers; do not mark architecture complete if two independent core paths persist. Matched representative baseline/final action construction, no-view and Button/MenuItem views, multiple views, bind/unbind/registration, execution (sync/worker separately) allocation and live shallow state if practical with same JDK/FX/ControlsFX/CSS/heap, medians/ranges and actual mode counts; no retained heap or corporate throughput claims. Report missing/negative performance evidence honestly, no artificial requirement for improvements. List all nine task dispositions and exact SHAs, gate matrices, intentional contract differences/remaining UNKNOWN, actual security/MDI/TaskContext/real app limits, independent Windows rerun commands for new modes with settings D:\Java\.m2\settings.xml, final production SHA. Previous class/skin/editor/table backlog not silently closed. STOP after Reply ACT-011, no self-assignment.


---

## QUEUED PACKAGE ALERT-001…ALERT-007 — Alerts / IAlertMaker evolution

**Status:** USER APPROVED DIRECTION / QUEUED / NOT ACTIVE.
**IMPORTANT:** ACT-003…011 remains the only active implementation package. This append is an architect-approved mailbox-only queue announcement; it does not alter ACT scope, stop conditions, production authorization or expected production chain. Do not interrupt, parallelize, or start ALERT tasks while ACT is active. STOP after ACT-011 as previously ordered. ALERT execution requires a subsequent architect activation with exact verified baseline after ACT final report/review. If ACT stops early, ALERT stays queued. This known append is not an unexpected production advancement. No production authorization from this queue entry alone.
**Repo:** ssu95a/fore25; production migration/java25-correctness; mailbox agent/codex-mailbox.
**Baseline:** to be pinned upon activation; source review was at 515d01fd before Action migration. Reopen actual current source and ABI at activation.
**Order:** ALERT-001…007. Proposed timebox ceilings 60/90/75/75/90/90/90 minutes (570 total); no time estimate or minimum.
**User direction:** do not break old API, add capabilities. Static Alerts method signatures MUST NOT change; preserve result mapping, default modality, ownership, localization, sync behavior and other supported semantics unless explicit approved correction. Focus final showAlert implementation. Evolve IAlertMaker. Ignore JInvAlert as development target; do not edit/remove/rewrite that class. Existing AlertBuilder public build() return type and package-private Alerts.getAlert route are compatibility constraints; retain existing legacy creation bridge if needed, use shared configuration implementation, no duplicate engine. No forced migration of external callers.

### Common architecture / scope

One compact implementation for static Alerts facade and fluent maker, using public JavaFX25 Alert/Dialog/DialogPane facilities. No large AlertManager, service locator, global dialog cache, generic forms framework or per-control validation infrastructure. Keep show lifecycle distinct from configuration. Preserve synchronous showAndWait contract; no silent asynchronous conversion or FX-thread blocking wait from worker. New optional nonblocking show/result delivery may use native lifecycle; document exact semantics. Do not invent cross-thread marshalling semantics. User-approved feature intent: content/detail/graphic, expanded, resizable, modality, default button, sound, service checkbox, functional checkbox, initial selection, optional validator. Candidate result API and method names below are design guidance; preserve existing IAlertMaker methods/erased descriptors and source compatibility where applicable. Do not introduce incompatible generic interface change without separate approval. Existing <T> Optional<T> showAndWait() cannot provide arbitrary runtime T safely; inventory implementations and consumers, preserve its existing signature if needed, define actual supported result explicitly and prefer an additive typed terminal/result converter API for new callers. No misleading arbitrary type cast in new typed API. Proposed AlertResult(ButtonType button, boolean checkBoxSelected), Optional empty on actual no-result per native contract, explicit close/cancel characterization; converter may map typed result; choose one minimal path, avoid overload ambiguity. Existing static ButtonType.CANCEL fallback and yesNo/yesNoCancel mappings unchanged. Do not use static facade result fallback to erase actual new fluent result distinctions.

**Checkbox contract USER APPROVED:** checkBoxText(String) is service checkbox, independent of buttons; initial selection default false, checkBoxSelected(boolean) optional. Return its state to caller; no persistence/settings side effect. requireCheckBox(ButtonType...) gates ONLY explicitly selected actual buttons, including YES/APPLY/OK if requested. No enum modes or separate checkbox managers. No checkbox means no implicit gating; invalid configuration (gating without checkbox or missing button) must have explicit deterministic behavior, prefer fail-fast for new API; no legacy behavior changes. CANCEL/ESC/close remain available under native close policy unless explicitly gated by supported contract; do not turn window close into affirmative operation. Functional acknowledgement cannot be bypassed by keyboard/default button or alternate normal UI invocation.

**Validator contract USER APPROVED:** exactly validator(Supplier<Boolean>), no additional validator overloads, no arbitrary button-specific validation, no validation contexts/severity/chain/events/framework. It affects ONLY ButtonType.OK; if no OK, does not affect other buttons. No validator = no extra restriction. Boolean.TRUE permits; false blocks. Evaluate on FX thread and recheck immediately before accepting OK; combine checkbox condition and other existing OK restrictions without clobbering them. Establish explicit null/exception handling (never accept invalid/failed validation; surface failure appropriately, no silent swallowing). Supplier is not observable: establish supported input-triggered reevaluation after actual user changes and initial evaluation; no polling/Timeline, no listeners on every child, no claim of universal observation of arbitrary programmatic supplier dependencies. If event-driven refresh cannot cover a needed contract, report limitation/propose minimal explicit refresh API separately, do not add a validation subsystem. Test mouse/keyboard/default-button, input text/paste/selection where relevant, state transitions, no OK, other buttons unaffected, checkbox AND, close/cancel, exception, reuse and exact lifecycle cleanup. No direct business writes from validation.

### Execution protocol (upon later activation only)

Fresh refs and clean tracked baseline, exact source/ABI inventory, isolated scoped commits on production, one Reply ALERT-nnn per task. Existing public/protected API and static signatures frozen; inspect binary descriptors, compile old consumer and run precompiled fixture unchanged. Main, JInvCommon, fore26_Arch read-only; no other repo writes, dependency upgrades, destructive Git, private JavaFX APIs, corporate DB/security changes, or native system configuration modifications. Use existing JDK25/FX25/Maven offline settings D:\Java\.m2\settings.xml, Russian Java comments, actual shown offline dialogs with deterministic FX callbacks and real ButtonType results. Three fresh focused JVMs + official focused runner, true native/outer exits, timeouts/PIDs/streams; relevant regressions, Maven -Pbuild compile, PowerShell parse, git diff --check. Defects baseline RED/candidate GREEN; refactors positive equivalence. Preserve original evidence and failed attempts; unexplained native/nonzero/timeout/shared failure stops all dependent work, no retry-to-green. No blanket prior waiver. Task-local unresolved contract BLOCKED and report, independent slices only on clean green baseline where safe. Final full suite all supported modes at activated baseline plus new tracked modes, ABI verification and independent user rerun commands. No publication of incomplete unvalidated changes.

---

## MESSAGE ALERT-001

**Status:** QUEUED, not assigned for execution.
**Timebox:** 60 min.
**Task:** READ-ONLY characterization of Alerts and consumers. Inventory all static overloads and descriptors, IAlertMaker, AlertBuilder (including build() ABI), actual calls with rg, ViewContext.tryGetWindow, JInvErrorService and LoginManager, Dialog localization, SoundPlayer, existing AlertException paths, actual thread/owner/modality use. Do not inspect/refactor JInvAlert internals. Check actual default/native ButtonType semantics for null/empty arrays, NONE/default type, title bundle, header/content null/empty/custom Node, detail String/String[]/List/Node, expanded state, custom graphics, YES/NO/CANCEL mapping, ESC/X/default ENTER, Stage without scene and null owner, no BaseApp/login, builder build vs show sound/owner effects, repeated show. Preserve supported contracts; label confirmed source hazards versus reproduced defects. Baseline ABI and precompiled legacy fixture. Use ignored probes, no production edit. Reply ALERT-001 with contract matrix, test evidence and decisions needed.

---

## MESSAGE ALERT-002

**Status:** QUEUED.
**Timebox:** 90 min.
**Task:** Compact IAlertMaker implementation, factory and common dialog preparation/show path. Introduce minimal additive fluent API, preserve legacy static signatures and old AlertBuilder bridge. Use standard JavaFX Alert/Dialog internally, no JInvAlert changes. Refactor shared owner/content/detail/button/localization setup without duplicating actual show implementation; static showAlert integration staged in ALERT-007 if necessary. Keep legacy configuration and show side effects characterized until relevant correction. Typed result design must not break existing IAlertMaker API; report exact proposed method signatures and compatibility evidence; prefer additive typed entry point, preserve old generic signature as compatibility surface with explicit supported behavior. New basic maker returns selected ButtonType through a sound typed route. Add initial focused tests for maker configuration, isolation of successive dialogs, native result, content and button defaults; no unsupported new result semantics.

---

## MESSAGE ALERT-003

**Status:** QUEUED.
**Timebox:** 75 min.
**Task:** Keyboard and default button correctness. Existing onShown adds Scene KEY_PRESSED filter for ENTER, casts arbitrary event target to ButtonBase and consumes all ENTER. Establish actual JavaFX25 dispatch/default-button behavior and historical requirement focused NO must not trigger YES. Preserve focused button activation, nonbutton/custom content (TextArea multiline, CheckBox, TextField etc.) native editing semantics and disabled/default button behavior; avoid global interception if public native route suffices. No ClassCastException on nonbutton target, exactly one execution, no repeated-show filter accumulation, exact handler ownership and removal, no override of caller event property unnecessarily. New defaultButton(ButtonType) only for explicit maker configuration; legacy default behavior not silently changed. Test actual shown dialogs, mouse-style/button action, keyboard-style event through real dispatch, ESC/X and repeated show. Physical input UNKNOWN if not tested. Reproduce source hazards before fixing, no private APIs.

---

## MESSAGE ALERT-004

**Status:** QUEUED.
**Timebox:** 75 min.
**Task:** Owner, modality, FX lifecycle, sound. Current owner resolution via ViewContext, scene-less Stage JAVAKERNEL-1572 workaround, alwaysOnTop true/false mutation and deiconify, BaseApp.APP() null exposure, SoundPlayer at construction. Characterize actual behavior and preserve valid owner/modality (legacy WINDOW_MODAL and error builder APPLICATION_MODAL) and intended bring-forward/deiconify without changing persistent alwaysOnTop flag. Do not remove historical workaround without JavaFX25 evidence. New maker sound(boolean) respects existing sound settings and plays at actual show, not mere config. Preserve legacy builder pre-show contract unless approved correction justified; distinguish build side effects from show lifecycle. Handle no-app/pre-login safely without triggering corporate settings or fake login. Define FX-thread precondition, no silent asynchronous change to static methods. Test no owner, detached Node, actual Stage/Scene, pre-login/no app, repeated show, correct owner and modal result, lifecycle/cleanup. No system audio config modifications. If actual sound playback unavailable, report source-only/offline evidence.

---

## MESSAGE ALERT-005

**Status:** QUEUED.
**Timebox:** 90 min.
**Task:** Content/details layout and sizing. Preserve selectable/copyable message text, wrapping, custom Node identity and ownership, String/String[]/List and custom Node details, initial expandable state (legacy Node currently expanded), localization and resizable defaults. Improve actual width/font-aware measurement and bounded dialog size via public JavaFX layout, no hidden text truncation, no arbitrary fixed magic line counts or eager caches. Characterize long unbroken text, multiline, Cyrillic, font/width changes, small window, expandable detail, external node reuse, existing content binding behavior. Do not claim pixel/paint/native DPI evidence absent measurement. New expanded/resizable configuration additive. Unsupported legacy detail types remain as before unless explicit extension approved; no generic exception disclosure policy change.

---

## MESSAGE ALERT-006

**Status:** QUEUED.
**Timebox:** 90 min.
**Task:** Service/functional checkbox, typed result and OK-only validator. Implement agreed contracts above using minimal public API, no persisted service-checkbox preference, no arbitrary validation framework. Shared gate conditions AND, preserve other buttons and native close behavior. Actual shown controls, initial false/true, user toggling, checkbox state returned on result, service no button gating, functional specified buttons only, dynamic Supplier<Boolean> affects exclusively ButtonType.OK, absent OK, invalid/exception cases, input reevaluation and final confirmation check, disabled button keyboard/default path and lifecycle cleanup. Result conversion typed without breaking static or existing fluent signatures; document exact choices, ambiguous legacy generic behavior and limitations. No change to business operation invocation. Focused tracked regression mode(s).

---

## MESSAGE ALERT-007

**Status:** QUEUED.
**Timebox:** 90 min.
**Task:** Complete static Alerts facade delegation and final gate. Confirm all original static signatures and JVM descriptors identical; all overloads converge on shared implementation with same return/fallback, synchronous semantics and supported owner/content/localization/modal contracts. Preserve AlertBuilder build() binary compatibility and JInvAlert untouched. No duplicate configuration/show engine; legacy bridge only where necessary. Run old compiled fixture unchanged, actual static info/error/yesNo/yesNoCancel/showAlert paths and new maker tests. Full supported regression at final exact SHA (all baseline Action/GUI modes plus new Alerts modes), actual native/outer exits, compile/parse/diff, ABI comparison, fast-forward chain and clean tree. Report all seven dispositions, exact commits, tests, approved intentional differences, remaining UNKNOWN and independent Windows commands. STOP after Reply ALERT-007.


---

## USER ACCEPTANCE — ACT-011 intermediate Action Engine checkpoint

**Status:** USER ACCEPTED, scoped independent four-mode rerun. Production checkpoint 62cb0ec44f0e958455f775d6c26e20ed208c7bb4; final Reply ACT-011 at fb2c8c1452953b9de5ad6a39827ef2e1f40958eb. User supplied ACT-006 PASS with three expected disabled-action WARN messages and subsequently confirmed the other three requested tests passed ("да! все отлично! фиксируем и идем дальше!"). Accepted scope: ForeActionModel (ACT-003), ForeActionInvocation (ACT-004), LegacyActionFacade (ACT-005), ActionSecurityAdmission (ACT-006). No separately supplied local SHA, complete streams or individual numeric process exits; do not fabricate. Agent final 67/67 JVM/runner 0/0 and six frozen precompiled fixture modes 0/0, original scoped ABI match remain separate evidence. Five production commits verified as a fast-forward chain by architect. Full per-diff review and corporate integration remain outstanding.

ACT package is closed at its actual intermediate disposition, not declared completely migrated: ACT-007 BLOCKED (same-action overlapping WAIT/message ownership, executor rejection, foreign rebind); ACT-008 BLOCKED (observable third-party filter interleaving and legacy dispatcher order); ACT-010 PARTIAL/BLOCKED (remaining consumer/worker/keyboard routes). ACT-009 implemented, agent verified, not a separately named user rerun. No new authority to fix worker or keyboard policies or migrate remaining consumers. Keep these issues in backlog for separate architectural decisions. ARCH-005 and CTRL-108 also remain open.

## DECISION — activate ALERT-001…ALERT-007

**Status:** USER APPROVED / ACTIVE — execute all seven previously queued tasks in order. This decision supersedes their QUEUED execution status and the previous completed Action-package STOP only for the Alerts scope.
**Production baseline:** 62cb0ec44f0e958455f775d6c26e20ed208c7bb4.
**Production branch:** migration/java25-correctness.
**Mailbox branch:** agent/codex-mailbox.
**Queued assignment:** dbecbe0af25865e0df20d89b00757f5236a371a6.
**Order / ceilings:** ALERT-001 (60 min), ALERT-002 (90), ALERT-003 (75), ALERT-004 (75), ALERT-005 (90), ALERT-006 (90), ALERT-007 (90); 570 min total ceiling. No routine interim confirmation for already scoped changes. Execute exactly the existing seven task descriptions and common architecture/protocol, not a new interpretation. Verify fresh refs and clean tracked baseline before starting; preserve all earlier Action evidence. Use the 67 supported final ACT-011 regression modes as baseline for final full gate, plus all actually introduced Alerts modes. Preserve the entire existing public/protected API, especially every static Alerts signature and synchronous result mapping. Do not modify JInvAlert or remove the legacy AlertBuilder build() ABI. Extend IAlertMaker additively; type-safe new result path without breaking old generic method. One shared dialog configuration/show implementation with minimal legacy bridge. Checkbox service and functional scenarios as agreed; validator(Supplier<Boolean>) affects ONLY OK, no other validation infrastructure, no polling or arbitrary child listener proliferation. No new dependency or action architecture changes.

For each task publish separate Reply ALERT-nnn; separate validated fast-forward production commit only for implemented changes. Preserve existing stop conditions, actual native/outer exit evidence, ABI and precompiled consumer verification, scoped correctness and final full regression gate. If a task needs an unapproved contract change, report exact blocker and do not invent policy; only independent work on a clean green baseline may continue. Original native incident waivers do not apply to any new failure. All necessary Git/GitHub operations within this specific approved scope are authorized. Main, JInvCommon and fore26_Arch read-only; no other repo writes. STOP after ALERT-007 or earlier blocking condition. No self-assignment or parallel production work.


---

## USER ACCEPTANCE — ALERT-007 final checkpoint

**Status:** USER ACCEPTED — independent six-mode rerun reported as "ALL SIX TESTS PASSED" in response to the exact six-mode acceptance procedure for final production 259f9887b95ee9cd7761a231e86bc57610325b88. Remote production verified at that SHA; final Reply ALERT-007 at 8a6c2e0c88ba5e0970a9560f97b32b4015eac56f. User did not separately supply local SHA, individual logs or numeric exit codes; do not fabricate.

Accepted focused scope: AlertsMaker (ALERT-002), AlertsKeyboard (ALERT-003), AlertsLifecycle (ALERT-004), AlertsLayout (ALERT-005), AlertsCheckbox (ALERT-006), AlertsFacade (ALERT-007). Agent final full regression 73/73 native/outer 0/0, scoped original ABI compatibility (24 static signatures), unchanged precompiled consumer and six fast-forward production commits remain separate evidence. JInvAlert and AlertBuilder source unchanged. Existing synchronous static signatures/results retained; IAlertMaker additive API, typed ButtonType and AlertResult terminals, service/functional checkbox, Supplier<Boolean> OK-only validator. Arbitrary programmatic supplier dependency changes are not observable; no polling or additional validation framework. Nonblocking show and general resultConverter not implemented; physical input, corporate sound/login, OS stacking, DPI/small-screen rendering remain unverified. ALERT-001 partial characterization, ALERT-002…007 implemented. Acceptance is bounded to tested scope.

Package completed; STOP remains in force. ACT-007 continuation (WAIT/progress ownership) and ACT-012…019 (ActionMap/contextual keyboard architecture) are separate future assignments, not activated by this acceptance. Other unresolved backlog unchanged. No production authorization or new work from this entry.


---

## DECISION — WAIT/Progress and ActionMap combined package ACT-007A + ACT-012…ACT-019

**Status:** USER APPROVED / ACTIVE. Execute all nine tasks in order autonomously within approved scope, not parallel production edits.
**Repo:** ssu95a/fore25; production migration/java25-correctness; mailbox agent/codex-mailbox.
**Starting baseline:** 259f9887b95ee9cd7761a231e86bc57610325b88 (ALERT-007 final 73/73 agent GREEN, six focused tests user accepted at f3d8009e).
**Prior evidence:** ACT-001/002, ACT-007/008/010/011 (final Action Reply fb2c8c1452953b9de5ad6a39827ef2e1f40958eb); preserve target/act-001 ABI, target/act-002 compiled consumer fixtures, target/act-007 WorkerOwnershipAudit and target/act-008 ShortcutOrderAudit. Reference fore26_Arch c34fffdc7f48c791cd2ec47a14b76915aa61565c READ-ONLY. Reopen current source at actual baseline. All necessary Git/GitHub operations within this approved package authorized.
**Order/ceilings:** ACT-007A 150; ACT-012 60; ACT-013 90; ACT-014 120; ACT-015 150; ACT-016 150; ACT-017 120; ACT-018 90; ACT-019 90 minutes. Total 1020 minutes (17h), ceilings not estimates/minimums. STOP after ACT-019 or blocking condition.

### Binding architectural decisions

Control owns behavior. Skin owns presentation. ForeAction is a compact operation container/bridge, not execution orchestration or GUI owner. Retain ControlsFX 11.2.6 inheritance and existing icon compromise; no icon cache/composition rewrite. New core ru.inversion.fore.form.action does not import legacy facade. Existing ActionInvocation remains shared ordinary execution/admission primitive. Do not add eager Action/ActionMap/manager per simple control, duplicate state per view or global Scene/Task registries.

WAIT belongs to the form; progress belongs to a particular execution. User EXPLICITLY authorizes deletion of public IFormStateListener.getWaitActions() and AbstractBaseController's implementation/backing Set<JInvAction> as the intentional exception to original ABI preservation. No fake deprecated empty Set. Existing addWaitAction(JInvAction)/removeWaitAction(JInvAction) signatures not separately authorized for deletion; inventory actual consumers and retain compatible facade where feasible; internal execution must not use action identity as sole WAIT ownership. If exact semantics of these legacy hooks cannot be maintained without separate decision, report blocker; do not invent no-op or remove more API. Independent overlapping executions of same Action permitted, each owns its own WAIT lifetime. Explicit setState(ACTIVE) resets old wait session; late old releases must not affect a new session. No mandatory extra token object if existing task/invocation can own identity. Preserve form close/veto lifecycle, distinguish GUI WAIT release from actual worker/transaction completion.

Progress USER APPROVED: external StringProperty belongs to caller. Framework publishes values on FX thread only while target is unbound; NEVER bind/unbind caller-owned property. Only detach exact listeners owned by execution. Intentional observable change: supplied property no longer becomes bound during framework execution. Do not overwrite or remove caller's replacement binding. Capture exact target/owner per invocation, retire obsolete delivery on replacement/close/reset; no stale write to a new target. Latest started still-active execution owns progress; when it ends, resume previous still-active source; when none, stop publishing. Apply to shared actual presentation sink, including stateTextProperty, without allowing an older completion to clear newer WAIT/progress. Preserve distinct legacy messageProperty (Task progress target) and stateMessageProperty (WAIT caption source); do not silently equate their semantics. If target already bound, leave untouched; report any incompatible external usage. No global serialization, cancellation or rollback promise. Preserve ThreadPoolManager submission policy incl non-FX CallerRuns and FX saturation rejection, no blocking work under lifecycle lock or on FX. New ForeAction excludes nextAction/chaining entirely; existing legacy methods and virtual callback semantics retained via facade. No unapproved change to deferred sequence or admitted handler semantics.

ActionMap owns operation registry; ActionKeyBinder resolves keyboard invocation; ActionInvocation executes operation. ActionMap has no Node/Scene/KeyEvent/security/Task/WAIT responsibilities. Each form controller owns its map (lazy materialization allowed), DSFXAdapter and TSFXAdapter are mini-controllers and own their data-operation maps; tables/trees are views, not owners of CRUD/REFRESH/MARK. Optional map only for complex controls' own operations, never mandatory map per simple field. Parent hierarchy: adapter -> owning form controller -> optional application scope; complex control local scope only as needed. Reuse existing Controls.CONTROL_PARENT, PROPERTY_DATA_SET_ADAPTER, IJInvControl.getController/getDataSetAdapter, actual TSFXAdapter attachment, controller lifecycle; no parallel GUI context tree or table button search for data operation. No new application-global mutable registry unless real ownership/lifecycle already supported. Do not create ActionMap merely for lookup on unused control.

User-provided ActionMap.java (ru.vmls.view.ActionMap) design reference: Map<Comparable,BgAction> backed by TreeMap comparator, same runtime type compared naturally except String compareToIgnoreCase; different types separated by class name; nullable parent; put(key,null) removes local; get(null) returns null; get(key) local then parent; remove/clear/size local; setParent/getParent. Adapt to ForeAction, not BgAction. Proposed new API Comparable<?> keys including own enum/String/numeric types, case-insensitive strings as source; retain semantics for intended supported keys, document comparator identity (including enum declaring class and compareTo equality) and reject unsupported/colliding type contracts rather than unsafe casts. User approved package/direction; exact key domain was proposed, not separately mandated for arbitrary Comparable implementations. Keep small, do not add type hierarchy for ActionId. Add getLocal, iterative parent traversal, cycle rejection and lazy backing collection; no synchronization for FX-owned usage. No second InputMap unless measured/functional necessity established and separately approved. Native key matching should not allocate full hotkeys list per event; current ForeAction primary accelerator + alternatives contract preserved.

New dispatcher design target: most specific functional context wins; a local ActionId shadows ancestor same ID entirely even if hotkey changed or action disabled. For new/native registrations, selected matching action reserves its key even when disabled or denied; consume and do not fall back to another action/native accelerator. No match -> normal JavaFX dispatch. Same hotkey allowed across scopes; within same effective scope ambiguous matches must have explicit deterministic conflict behavior (prefer reject configuration where possible; dynamic changes must not execute arbitrary conflicting action). Do not globally capture native editing keys unless explicit command/navigation contract applies. One dispatcher owner per Scene, potentially own FILTER/HANDLER hooks, no competing permanent engines. Legacy keyboard behavior: ACT-008 showed observable callback-vs-action registration interleaving with external filters. User approved direction toward unified ActionMap but did NOT separately approve breaking those observed legacy phase/consumption/priority contracts or deleting public getActionMap/getCallBackMap. Preserve existing legacy behavior or report exact blocker requiring decision; don't silently impose native reserve policy on legacy or declare consolidation complete if residual routes remain. Preserve public mutable collection identity/updates, event payload, registered KEY_PRESSED/RELEASED/TYPED, logical owner, dataset fallback, existing accelerators and external filters. No new native dispatcher competing with active old registrars on same Scene. Explicit attach/close ownership; detached Node must not silently create a Scene or be registered to wrong main Scene for new API.

### Common execution protocol

Verify fresh refs and clean tracked checkout before each task/reply. Preserve all original public/protected API/ABI except specifically approved getWaitActions removal and approved progress binding behavior; document intentional changes, compare exact original and package-baseline descriptors, run unchanged precompiled fixtures where applicable. If fixture directly calls intentionally removed method, explicitly identify expected incompatibility and use targeted new characterization only for removed contract; do not weaken unrelated ABI assertions. Existing test assertions expecting old erroneous WAIT/progress behavior may be changed only for these approved corrections, show exact before/after. No other public signature deletions, silent security or behavioral changes. Main, JInvCommon, fore26_Arch read-only. No destructive Git/force push, dependency upgrade, private JavaFX access/reflection, actual corporate DB/security changes, external editor processes, arbitrary sleeps, forced successful exit or retry-to-green. Russian Java comments. JDK25/FX25, offline Maven settings D:\Java\.m2\settings.xml. Use exact old probes and faithful ignored recorder for true native child signed/hex, independent outer exit, timeout, PID/timestamps/stdout/stderr; preserve all failed attempts. For defects deterministic baseline RED and candidate GREEN; refactor characterization/equivalence. Three fresh focused JVMs + unchanged official focused runner, applicable scoped regression, Maven -Pbuild compile, PowerShell parse, git diff --check. One isolated validated fast-forward commit per implemented task, remote SHA verified, clean tree and separate Reply per task. No incomplete/unvalidated push. Read-only/NO_CHANGE no production commit. Mandatory intermediate full gate after ACT-007A (73 original modes plus new ones), another after ACT-016 (all supported modes), final ACT-019. Any shared unexplained nonzero/native/timeout, dirty/conflict/unexpected advancement or unreliable evidence: STOP, remaining NOT_RUN. No prior native incident waiver applies. Task-local BLOCKED: continue only independent work on clean verified green baseline; dependent tasks NOT_RUN/BLOCKED, never invent a contract. Timebox expiry report exact incomplete coverage; mandatory gate not skipped. User acceptance separate. No ARCH-005/CTRL-108/Alerts redesign or unrelated tasks. STOP after ACT-019.

---

## MESSAGE ACT-007A

**Status:** ASSIGNED / ACTIVE.
**Timebox:** 150 min.
**Reply ID:** ACT-007A.
**Task:** Implement approved WAIT/Progress ownership corrections and remove getWaitActions.

Inventory actual IFormStateListener implementations, getWaitActions/addWaitAction/removeWaitAction/setState/stateTextProperty usages, JInvAction/JInvParallelAction/JInvTask/TaskHandler, ThreadPoolManager, AbstractBaseController, WAIT view callbacks and externally constructed public tasks. Preserve public nested JInvTask and legacy add/remove hooks unless separate explicit decision; remove getWaitActions and Set<JInvAction> only as approved. Implement compact form-owned waiting state with exact per-invocation identity, idempotent release, reset generation/retirement, no permanent global registry. Prefer existing execution/task state as token or minimal lazy form state. Retain separate explicit form state transitions/close/veto and independent manual WAIT semantics, no accidental transition to ACTIVE from other states. Capture form owner and progress target; no mutable action field lookup at late completion that affects new owner. Framework never binds/unbinds external StringProperty incl stateTextProperty; publish unbound values on FX, detach own source listeners, resume most recent remaining active source, preserve caller's binding and original exception. Handle rejection after partial acquisition, success/fail/cancel exactly once, reentrant observable callback/failure, actual Task cancellation vs worker still alive, directly constructed public JInvTask with no acquired token, same-action overlap, different actions/shared target, explicit ACTIVE reset then new run, target replacement/rebind, close/reopen. Preserve actual FX/non-FX caller admission and ThreadPoolManager CallerRuns (no lock across submit, no blocking FX wait) and legacy nextAction semantics. No new ForeAction chaining. Reproduce all three ACT-007 defects baseline (overlap, rejection, foreign binding), GREEN on exact meaningful assertions plus positive worker/TaskHandler/nextAction/cancel tests. If external custom form or direct task compatibility cannot safely be resolved, report exact bounded blocker; no unauthorized broad changes. ABI exception inventory; full 73+new mode mandatory gate on exact validated candidate and verify published SHA. No advancement after gate failure.

---

## MESSAGE ACT-012

**Status:** ASSIGNED / ACTIVE after ACT-007A green or clean independent baseline.
**Timebox:** 60 min.
**Reply ID:** ACT-012.
**Task:** Compact ActionMap core.

Implement source-inspired ActionMap in ru.inversion.fore.form.action with ForeAction values, parent hierarchy, local lookup/removal/clear/size, put(key,null) removal, get(null) null, getLocal, cycle-safe setParent, lazy backing map. Exact minimal public API and documented key equality/case rules; support StandardAction, custom enum, String and numeric keys, avoid raw Comparable warnings/unsafe cross-type compare. No GUI imports except ForeAction dependency, no keyboard logic, no listeners/extra property objects. Choose simple collection preserving intended comparison semantics; do not claim HashMap equivalent to compareTo without proof. Parent lookup iterative, no temporary collection on ordinary get. Provide only necessary internal iteration for later dispatcher, don't expose writable backing map gratuitously. Test local shadowing/disabled no fallback, parent changes, cycle rejection atomicity, null/remove/clear/size, custom enum/type separation/case collisions, empty footprint. Measure only relevant object/lookup allocations. No consumer switch. One scoped commit.

---

## MESSAGE ACT-013

**Status:** ASSIGNED / ACTIVE after ACT-012.
**Timebox:** 90 min.
**Reply ID:** ACT-013.
**Task:** Controller-owned ActionMap.

Integrate stable lazily created map in AbstractBaseController with minimal additive getActionMap/access path; avoid materialization during passive lookup if unused. Actual functional form ownership including controller construction, FXML before/after init, SDI close/veto/reopen, nested controllers/MDI as feasible. Parent linkage only to verified functional owner; optional application scope if actual lifecycle warrants, not new global cache. Close retires registrations and parent references only for owned context, no stale action invocation or clearing other owner's map. Preserve legacy action wiring unchanged until migration. Test independent controllers/maps, same IDs different forms, parent replacement/cycle, close/veto/failure initialization, old references and exact identity, no eager maps on simple controls. Scope controller and minimal new core bridges, no Binder redesign. One commit if verified.

---

## MESSAGE ACT-014

**Status:** ASSIGNED / ACTIVE after ACT-013.
**Timebox:** 120 min.
**Reply ID:** ACT-014.
**Task:** DSFXAdapter / TSFXAdapter mini-controller ActionMap ownership.

Use actual adapter bind/close/afterInit and controller linkage; adapter owns data actions (CREATE/UPDATE/DELETE/REFRESH/MARK as applicable), table/tree is View, optional local complex control map only for its own functional view operations. Add stable lazy adapter map, parent to actual owning controller, use existing PROPERTY_DATA_SET_ADAPTER and bound controls lookup; do not create parallel context graph, eager per-field maps or call getTable() solely to resolve a data action. Do not invent CRUD implementations or bypass app security/transaction policy; register existing operations only when equivalent, otherwise infrastructure only with fixtures. Retire exact map/parent registration on close/rebind, guard original attachment, no stale actions after worker/queued delivery, preserve CTRL-051/064/065/076/077/092/093/098/104/105/106 and tree behavior. Test two adapters same form/different IDs and same IDs, field bound to correct dataset, table/tree, detached/replaced/closed adapter, parent fallback, independent controller scope, retained old reference and no foreign writes. Corporate DB writes forbidden. Verify TSFXAdapter actual lifecycle separately, no assumption it behaves identically to DSFXAdapter. One scoped commit.

---

## MESSAGE ACT-015

**Status:** ASSIGNED / ACTIVE after ACT-012…014 as applicable.
**Timebox:** 150 min.
**Reply ID:** ACT-015.
**Task:** New contextual ActionKeyBinder, no competing dispatchers.

Build minimal per-Scene owned resolver/registration lifecycle on ActionMap, use existing context relationships and actual ForeAction primary accelerator/alternatives (avoid allocating hotkeys list for each key match). Native policy as above: exact local ID shadows parent even with different hotkeys; most-specific matching scope, explicit ambiguity handling within scope, reserved disabled/denied key consumes and does not fall back; no match passes native. Security via existing safe admission boundary; no bypass. Determine focus/control/logical owner/adapter/form context from actual relationships, guard loops/detached and no wrong Scene fallback. Preserve native text/clipboard/selection/accelerators and correct event type/target. One owner, exact filter/handler detach, no global map/cache. Can test isolated shown native Scene without legacy registrars; do not install competing production handler where legacy active until ACT-016 integration proven. Dynamic hotkey/accelerator change, duplicate/overlap modifiers, local override changed key, unrelated native accelerator, disabled, two sibling scopes, attached/reparent/close, repeated registration/no duplicate invocation, controller and adapter resolution, synthetic dispatch and real shown controls. No separate InputMap without decision. Report proposed vs verified scope order. One isolated additive commit if safe; no claim legacy unified yet.

---

## MESSAGE ACT-016

**Status:** ASSIGNED / ACTIVE after ACT-015.
**Timebox:** 150 min.
**Reply ID:** ACT-016.
**Task:** Legacy keyboard facade, dispatcher integration and mandatory gate.

Inventory SceneKeyManager/SceneKeyBinder/JInvKeyboardManager public/protected and actual external-facing maps/constants, callbacks, virtual event phases. Reuse shared resolver/admission where compatible, adapt registration and dispatch without duplicate authority, preserve exact legacy observable behavior unless expressly approved otherwise. ACT-008 original blocker: application filter between legacy registrars sees action/application(true)/callback vs callback/application(false)/action depending registration order. Reproduce same fixtures; don't change ordering, consume semantics or public map identity silently. If one filter position cannot preserve third-party interleaving, propose minimal explicit policy change with exact impact, retain compatible existing dispatch positions or report BLOCKED; do not enable new competing binder or discard legacy map API. Distinguish shared resolution authority vs multiple compatibility hooks. Preserve original JInvEvent/JInvActionEvent control/place/key payload, disabled/denied shadowing, native accelerator interaction, logical parent, DataSet/table fallback, mutable collections, pressed/released/typed, filter/handler, no duplicate action or security recheck regression. Verify real shown offline cases, direct collection edits, callback identity/order, detached nodes, multiple scenes/MDI where feasible. Full 73+all new modes, original ABI except approved WAIT removal and precompiled fixture. Mandatory GREEN gate at exact candidate/current SHA even if integration BLOCKED; dependent migration cannot pretend completed. One scoped commit if safe.

---

## MESSAGE ACT-017

**Status:** ASSIGNED / ACTIVE after relevant ActionMap/dispatcher gates.
**Timebox:** 120 min.
**Reply ID:** ACT-017.
**Task:** Migrate standard functional operation consumers and presentation.

Use updated ACT-010 inventory and actual new ActionMap ownership; select concrete representative internal routes (form and adapter/table/tree where supported, toolbar/menu/keyboard) that can share one functional operation without duplicate Action state, preserve legacy exact handler identity/custom overrides/return types/FXML/security/tooltip and legacy icon contract. Native ForeAction for new/internal supported use, compatibility facade for existing callers; do not wrap each IAction into eager ForeAction or manufacture unused map entries. Controller/adapter owns functional operation, view projects it. No broad migration where ACT-016 unresolved keyboard integration is required; independent programmatic/toolbar/menu routes only if exact compatibility evidence. Test actual operation identity, event/source, two views, lifecycle/replacement/close, adapter scope, denied operation, no duplicate invocations or retired delivery; no real DB writes. Quantify actual migrated vs remaining consumers and residual authorities. One scoped commit if safe, otherwise explicit PARTIAL/BLOCKED.

---

## MESSAGE ACT-018

**Status:** ASSIGNED / ACTIVE after prior independent gate.
**Timebox:** 90 min.
**Reply ID:** ACT-018.
**Task:** Investigate navigation simplification using JavaFX requestFocusTraversal.

Read actual JInvKeyboardManager.initNaviationOnNode/fireForwardEvent/fireBackwardEvent and table/LOV/ValidMan commit/focus contracts. Existing TextArea uses public requestFocusTraversal(NEXT/PREVIOUS), others synthesize TAB; compare real shown traversal, focus order, validation veto/commit, text editing, Ctrl+Tab/Shift+Tab, table boundaries and editable cells, ComboBox popup, LOV completion, disabled/hidden controls, nested forms. No sweeping replacement, no native editing key interception or navigation semantic change. Use baseline/candidate characterization; implement only narrow proven equivalent public traversal path if it materially simplifies behavior, preserve old public API and event consumption; if external synthetic-event observers or validation behavior differ, NO_CHANGE/BLOCKED with exact evidence, no workaround by forced focus. Scope navigation only, no key registry rewrite or table model changes. Relevant regression and one validated commit at most.

---

## MESSAGE ACT-019

**Status:** ASSIGNED / ACTIVE after previous dispositions.
**Timebox:** 90 min.
**Reply ID:** ACT-019.
**Task:** Final read-only architecture, compatibility, lifecycle, correctness and footprint gate.

Verify exact fast-forward production chain from 259f9887, clean tree, scope and disposition of all nine tasks including independent/blocked cases. Full 73 original + actual new supported modes, real child/outer exits, compile/parse/diff, scoped original and starting ABI checks with exactly documented getWaitActions removal and intentional progress change; unchanged precompiled fixtures except explicitly incompatible removed call/old erroneous assertions, no blanket waiver. Confirm actual WAIT overlap/reject/rebind/reset/cancel/close behavior and progress FX ownership; ActionMap context/parent cycle/lifecycle, controller/adapter ownership, real dispatcher/legacy hooks, native accelerator behavior and remaining duplicate authorities; no claim fully migrated if unresolved. Measure matched action/map construction, empty owner vs active map, lookup/dispatch and representative view lifecycle allocation/shallow state with same environment and ranges where practical; avoid per-event list allocations and eager per-simple-control state. Retained heap/corporate integration UNKNOWN unless measured. List exact SHAs, all task statuses, approved intentional incompatibilities, unresolved keyboard policy, external API impact and remaining backlog; independent Windows commands for new modes -SettingsFile D:\Java\.m2\settings.xml. STOP after Reply ACT-019; no self-assignment.


---

## DECISION ACT-007A — explicit WAIT opt-in

**Status: USER APPROVED / BLOCKER RESOLVED / RESUME ACTIVE.**
In response to Reply ACT-007A at mailbox 4cbb80922aa333b20f09e4d7cc09f09a0c7c79b7, the user expressly approved replacing the old implicit WAIT opt-in (override of getWaitActions) with explicit opt-in to automatic WAIT management.

Binding delta to combined package f03d66b14f9cd62418ee9fe6d9a7ac42b5036688:
- Remove getWaitActions() from interface and controller, including backing Set, as previously approved. Do not preserve hidden dependency, reflective detection, or compatibility getter.
- IFormStateListener is the basic form state/stateText contract. External implementors remain passive by default. AbstractBaseController explicitly enables the new automatic WAIT lifecycle.
- Existing external implementations whose sole opt-in was overriding getWaitActions() intentionally become passive until migrated to the new explicit mechanism. User accepts this precise behavioral compatibility change. No attempt to dynamically recognize old overrides. External classes not previously opted in must remain passive; explicitly set WAIT on a passive form remains under its own control.
- Provide a minimal explicit, additive opt-in path suitable for external implementations, with ownership of per-form execution state, not merely an unconditional global registry or boolean that cannot supply state. Choose smallest workable API, document exact migration example and why chosen. No eager infrastructure on passive forms or simple controls.
- Preserve existing addWaitAction(JInvAction)/removeWaitAction(JInvAction) signatures and compatible behavior for explicitly managed forms where feasible; default passive behavior remains no-op. Do not delete these signatures or change custom overrides silently. Internal execution uses per-invocation identity, not legacy action-only pairing. Report separately any remaining actual contract incompatibility rather than expanding the waiver.
- Extend frozen ExternalWaitContract evidence: unchanged precompiled passive and former-getter-opt-in forms both passive under new API (former opt-in change intentional); migrated explicit-opt-in external fixture and AbstractBaseController managed, with independent execution identities. Test manual WAIT, reset and late releases, external binding ownership, rejection, overlap, close and original worker/nextAction contracts. Document exact original vs candidate assertions and approved ABI/behavior delta, preserve unrelated frozen checks.
- No other changes to combined package or stop rules. Resume ACT-007A from verified unchanged production 259f9887b95ee9cd7761a231e86bc57610325b88. Publish separate continuation Reply ACT-007A and validated production commit, mandatory full gate, then execute ACT-012…019 in original order without further confirmation for already approved scope. Remaining tasks NOT_RUN status in previous Reply superseded by this activation. STOP after ACT-019 or new blocking condition. All necessary Git/GitHub operations within original approved scope authorized. No new external API deletion or blanket ABI waiver. Actual corporate external implementors remain UNKNOWN.


---

## USER ACCEPTANCE — ACT-015 checkpoint

**Status:** USER ACCEPTED five published focused modes. User reported "ALL FIVE TESTS PASSED" after the prescribed independent rerun of ActionWaitOwnership, ActionMapCore, ActionMapController, ActionMapAdapters and ActionKeyContext against the intended production checkpoint 655b7287728b8f2ab4d4a6adafc7844f066cee1d. Remote SHA independently verified; local SHA and per-process exit codes not separately supplied. GitHub remote in user's fore25 checkout D:\Java\Projects\fore25 is explicitly confirmed as origin; branch migration/java25-correctness. Agent full gate 78/78 on published SHA and previous 74/74 after ACT-007A are separate evidence. ACT-007A, ACT-012…015 accepted within tested scope; native contextual binder remains isolated, not activated by production consumers. ACT-016 integration BLOCKED pending explicit legacy keyboard ordering policy. ACT-017 candidate remains local, uncommitted and unpublished following GUI-005 focus/FX-queue timeout (child/outer 1/1); root cause UNKNOWN. ACT-018/019 NOT_RUN. Acceptance of published checkpoint is NOT a waiver for the ACT-017 failure, authorization to retry, or activation of any pending tasks. Preserve candidate and all original diagnostic artifacts; STOP remains in force pending separate authorization. Source review observations (static active sink index cleanup/reentrancy, per-event Match allocation, new binder imports legacy registrars) are follow-up review concerns, not confirmed runtime defects or assigned changes.


---

## DECISION ACT-017 — GUI-005 diagnosis

**Status: USER APPROVED / DIAGNOSIS ACTIVE.** The user authorizes the specifically proposed baseline/candidate diagnosis of the TableEditing (GUI-005) failure in Reply ACT-017, mailbox cf93ec2a31b5302e2b09e7a5fb7a6cdcc387b779. Production checkpoint 655b7287728b8f2ab4d4a6adafc7844f066cee1d independently accepted (five focused modes); the previous failure is NOT waived. ACT-016 keyboard ordering policy is still BLOCKED and not authorized to change by this decision.

**Timebox:** 90 min diagnostic ceiling, not an estimate.
**Reply:** ACT-017-DIAG.

1. Verify fresh refs, exact published baseline and original local uncommitted candidate status/diff. Preserve original candidate patch including untracked test source and hashes and all target/act-017 failure evidence (stdout/stderr/exit/argfile/recorder, original timestamps and PIDs). Do not overwrite the original artifacts. No reset/clean/stash of unknown user work, no destructive checkout, no production push during diagnosis. Use isolated checkout/worktree/build outputs for the comparison without modifying original candidate; if impossible, STOP with exact blocker.
2. Inspect actual GUI-005 probe and runner, identify which wait/check timed out and whether Stage focus, FX queue, or another cause is evidenced. Original child/outer exit 1/1, timeout false at recorder, probe reports focus/FX queue timeout before edit assertions. Root cause UNKNOWN until evidence. Check interactive desktop/session state, window visibility/focus and competing process/window conditions; preserve same JDK25/FX25, dependency/classpath, Maven settings, runner and timeout across baseline/candidate. Capture focused diagnostics of Stage lifecycle/focus/FX heartbeat, thread dump if feasible and relevant, without altering the original assertion contract.
3. Execute controlled paired A/B diagnosis against exact published baseline and preserved candidate in comparable conditions, preferably two paired fresh runs in reversed order (A/B and B/A), actual native/outer exit, timeout, PID, timestamps, stdout/stderr, assertion marker and focus/FX diagnostics. Run only when interactive desktop suitable; otherwise report environment blocker. The purpose is discrimination, not retry-to-green. If any run exhibits additional unexplained failure, preserve evidence and STOP the diagnostic matrix unless specifically justified as a necessary controlled comparison; no repeated arbitrary retries. If instrumentation needed, use isolated diagnostic probes and distinguish from unchanged official runner evidence.
4. Interpret evidence: baseline failure as well does not by itself prove environment cause; candidate-only reproducible failure requires root cause analysis; passing pairs do not erase original incident. Classify confirmed facts vs hypotheses, explain whether root cause is established, and identify residual uncertainty. No relaxing timeout/assertions, forcing focus to make a test green, swallowing failure, modifying production to hide a probe defect, or applying previous unrelated native failure waiver.
5. Publish separate Reply ACT-017-DIAG with exact baseline/candidate identity, diagnostic commands/results, preserved artifacts, conclusion and minimum next step. If actual source regression established, propose exact correction for review; if environment/probe issue established, propose exact corrective condition and fresh gate procedure. If unresolved, report UNKNOWN and STOP. No production change/push, no automatic activation of ACT-017 implementation/ACT-018/019 or ACT-016 policy changes from diagnosis alone; further gate/restart decision based on results. Existing approved package constraints and independent user acceptance remain intact.


---

## DECISION ACT-016 — deterministic keyboard dispatch

**Status: USER APPROVED CONTRACT / QUEUED, NOT ACTIVE.** User approved the proposed unified flow. This resolves the ACT-016 ordering-policy question, but does not clear the ACT-017 regression stop or authorize simultaneous work. Latest Reply ACT-017-DIAG at 1a205703ab4e55292851c997844d5f59fd279075 reports current desktop cannot focus a shown canary despite 127 FX pulses, zero A/B GUI-005 launches, original cause UNKNOWN. Production remains 655b7287728b8f2ab4d4a6adafc7844f066cee1d. Preserve original dirty candidate and all evidence. STOP remains in force pending separate diagnostic gate/restart decision.

Approved dispatch delta for eventual ACT-016 continuation:
- One ActionKeyBinder owner per Scene; one controlled hook per required FILTER/HANDLER phase, not independent competing registrars. Within each phase, search in this deterministic order: contextual native ForeAction -> legacy IAction -> legacy Callback. Select at most one functional recipient; do not execute a chain of handlers. New native shortcuts default KEY_PRESSED; preserve existing legacy event types and phases.
- Respect consumed events, including at dispatcher entry. Do not invoke a legacy callback after an event has already been consumed. No match leaves the event untouched for normal JavaFX dispatch; do not intercept unrelated native editing/navigation.
- Found native action reserves key even disabled/denied/ambiguous, consumes without fallback as previously agreed. Found legacy IAction still shadows lower-priority actions/callbacks when unavailable; preserve legacy unconsumed behavior for disabled/denied IAction (unless separately approved otherwise). Successful legacy action consumes as before. Legacy callback, when selected, retains its payload and control of consume(). No fallback within a phase after selection, regardless of admission. A callback leaving event unconsumed allows normal JavaFX dispatch; do not silently impose cross-phase deduplication where it changes unrelated legacy phase semantics. Test and report any cross-phase duplicate functional route requiring further decision.
- Intentional compatibility exception: old relative registration/interleaving of independent Fore action and callback filters versus external application filters is no longer guaranteed; both Fore paths occupy the unified phase hook. Existing order of third-party filters relative to each other remains JavaFX behavior. Callback observing an already consumed event or executing alongside a matching higher-priority action is intentionally no longer supported. Document exact before/after fixture outcomes; preserve other legacy payload, map identity/mutability, live edits, registration replacement, logical context, admission and public signatures. No additional ABI waiver.
- SceneKeyManager and SceneKeyBinder are registration compatibility facades, new dispatcher owns resolution. Retain public getActionMap/getCallBackMap collections without synchronized copies. Remove direct imports of legacy registrars from new ActionKeyBinder; facade depends on core, not reverse. Do not introduce a new global registry, eager per-field objects, or separate InputMap. Navigation remains a separate concern.
- Update original ACT-008/016 ordering characterization only for the expressly approved deltas; retain unchanged precompiled consumers where compatible, test both registration orders, third-party filters before/after, consumed input, enabled/disabled/denied/missing action, callback consume/no-consume, native text/accelerators, live map edits, phases and lifecycle. Mandatory full gate and original stop rules after implementation. Exact mechanism and internal result type left to Codex; no public DispatchResult requirement.

This entry records architecture only. Do not resume ACT-016, ACT-017, ACT-018, ACT-019, run further GUI diagnostics or change production until separately authorized. No self-assignment.


---

## DECISION ACT-017-DIAG — interactive desktop continuation

**Status: DIAGNOSTIC RESUME ACTIVE, conditional on preflight.** User clarified that Codex was previously working in the background; now the user is deliberately yielding keyboard/mouse and leaving Codex in the foreground. This is a new test condition, not proof of the original failure cause or of current focus availability.

Resume only the previously authorized ACT-017-DIAG (original decision f583a436cb3bf80191fe5587c0e71910f696d84c) from the exact preserved A/B worktrees and artifacts in Reply ACT-017-DIAG at 1a205703ab4e55292851c997844d5f59fd279075. Verify original candidate hashes and unchanged production 655b7287728b8f2ab4d4a6adafc7844f066cee1d. Repeat independent focus canary under the new interactive condition; record current foreground/focus/FX pulses and actual child/outer exit. If it obtains focus and other preconditions pass, immediately execute the already prepared controlled A/B/B/A GUI-005 sequence with unchanged assertions/timeout and matching environments, no further confirmation required. If preflight fails, STOP and report, do not run GUI-005. Use remaining diagnostic budget or a fresh ceiling of 90 min for this continuation; no arbitrary retries. No forced focus workaround or simulated user input to manufacture a passing test. Preserve all previous and new evidence in separate outputs. Passing pairs do not by themselves establish the original cause. Publish continuation Reply ACT-017-DIAG with results and recommended next gate; STOP. No production edits or push, no regression waiver, no activation of ACT-016/017 implementation/018/019 by this entry. Approved keyboard contract remains queued.


---

## DECISION ACT-017 — validation and remaining package resume

**Status: USER APPROVED / ACTIVE.** In response to Reply ACT-017-DIAG interactive continuation at f8d84462e0bbc857bf3f973766621e7f1e735a1e, user accepts the bounded residual uncertainty of the original GUI-005 incident and explicitly authorizes validation and resumption of the remaining package. This resolves the historical stop for that incident only. The original GUI-005 root cause remains UNKNOWN; do not reclassify it as a proven desktop problem or erase its 1/1 child/outer failure record.

Evidence: original preflight canary showed a live FX loop but no focus; subsequent user-yielded interactive desktop canary focused on first FX pulse, child/outer 0/0. Controlled A/B/B/A matrix on exact production 655b7287728b8f2ab4d4a6adafc7844f066cee1d and preserved candidate returned GUI-005 PASS, 0/0, no timeout in all four runs, unchanged assertions and timeout, identical probe/dependency hashes. No reproducible candidate-only failure under this condition. Preserve all original and diagnostic artifacts.

**Execution order:** ACT-017 validation/publication -> ACT-016 continuation -> ACT-018 -> ACT-019. Sequential only, no competing production edits. This explicitly activates the queued deterministic keyboard contract from c27657b19dff7cacc1aa3df0672c4de7ad0d3a54. Existing combined assignment f03d66b14f9cd62418ee9fe6d9a7ac42b5036688 and ACT-007A decision remain binding except the approved order and compatibility deltas. No new API waiver.

1. ACT-017: verify exact remote baseline, original dirty candidate, tracked-candidate.patch SHA-256 B529F722DB67E5DC267CF2051A3BF4D4961D953F7748B6B1D24FEDA3CC133D76 and untracked ActionMapMarkViewSmoke.java SHA-256 8679EE8A4EA400668C9BDC6FCBAF9DA0AA1C33C0689672C59F11ABF35567CD2A. Preserve hashes and evidence; only the known candidate modifications are authorized, any additional unknown changes STOP. No mixing with ACT-016. On verified focus-capable interactive desktop, run fresh unchanged official ActionMapMarkView and required focused fresh JVMs, complete scoped regression including TableEditing/GUI-005 and previously not-run SceneKeyLookup, ACT-015/014/013, CTRL-104/106, compile, parser, ABI, diff and clean publication checks according to original task. If GREEN, publish exact validated candidate as its own fast-forward production commit and separate continuation Reply ACT-017. No publication based on A/B alone.
2. ACT-016: on the new verified clean HEAD, implement deterministic unified keyboard dispatch exactly per approved queued decision c27657b19dff7cacc1aa3df0672c4de7ad0d3a54. ForeAction -> legacy IAction -> legacy Callback within each phase, respect consumed events, preserve specified native/legacy admission, payload, phase, map semantics; remove reverse dependency on legacy registrars. No guarantee of old relative interleaving between registrars and external filters; this is the only newly approved ordering/consumed-callback compatibility delta. Preserve unrelated contracts; do not invent cross-phase policy. Required full regression/ABI gate on exact candidate includes all previously supported modes and ACT-017 mode. Separate commit and continuation Reply ACT-016.
3. ACT-018: original navigation investigation and bounded equivalent requestFocusTraversal change only if evidence supports it, otherwise NO_CHANGE/BLOCKED with reason; no broad replacement or forced focus. Separate Reply and validated commit if applicable.
4. ACT-019: final full regression/ABI/lifecycle/architecture/performance gate, including all actually added supported modes, exact commit chain, remaining limitations, independent user PowerShell rerun commands (fore25 remote origin). Include previously noted ProgressDelivery static sink-index cleanup/reentrancy and per-key Match allocation as review findings, not automatic authorization for unrelated refactors. STOP after Reply ACT-019.

**Desktop preflight:** verify a focus-capable interactive desktop before every focus-sensitive test group and after any desktop/session change. User is yielding keyboard/mouse for tests; availability must still be measured, not presumed. If preflight fails, STOP without attempting focus-sensitive tests. No forced focus workaround, no synthetic user input to manufacture green, no timeout/assertion relaxation, no arbitrary retries. Any new unexplained JVM/runner nonzero, timeout, native crash, lost-focus failure, unreliable evidence, unexpected advancement or unknown dirty changes triggers original STOP rules. Neither this bounded incident acceptance nor earlier waivers apply to new failures.

Timebox ceilings for this continuation: ACT-017 120 min, ACT-016 150 min, ACT-018 90 min, ACT-019 90 min (450 min total ceilings, not estimates). All necessary scoped Git/GitHub operations authorized. No additional user confirmation for already approved work. Production writes only validated task-scoped fast-forward commits. Read-only other repos. Preserve separate Replies and original evidence. STOP after ACT-019 or earlier blocking condition.


---

## DECISION ACT-016 — remaining binary-fixture validation resume

**Status: USER APPROVED / ACTIVE.** In response to Reply ACT-016 at mailbox 784f1a6aa73891e1e0a2812a7a1e8bf5a3c84149, the user explicitly authorizes continuation and is yielding the interactive Windows desktop, keyboard and mouse again. This authorizes a fresh measured preflight under the new condition, not an assumption that focus is available, and not a waiver of any regression or binary compatibility requirement.

Resume on exact clean local candidate eabf4c6725fc984363c57d54ff8633a66ebef20d, with direct parent and expected remote production HEAD b5bb8fb10be4fd574f8f86eb8e77c4aec90ef563. Verify local branch, exact commit/tree, clean status, direct parent, remote HEAD and recorded evidence before proceeding. Preserve all prior artifacts under target/act-016-validation, especially gate/matrix.csv and frozen/canary.stdout. Previous full candidate gate 80/80, 0/0 native/outer, no timeout and all markers, focused and scoped tests and ABI evidence remain valid only for the same unchanged candidate with verified matching evidence/environment. Do not rebuild or rewrite candidate production source, amend, rebase or repeat implementation. Do not needlessly rerun the completed full gate; if its evidence or equivalence cannot be established, STOP and report instead of assuming it.

1. Run a fresh independent focus-capable desktop canary and capture actual Stage showing/focused state, FX pulses, foreground/session observations, native JVM and outer runner exits, timeout, raw streams. The previous failed preflight (visible, unfocused, 127 pulses, 0/0) remains recorded; cause UNKNOWN. If fresh preflight fails, STOP without starting focus-sensitive fixtures. Repeat preflight before each required focus-sensitive group and after desktop/session changes; no arbitrary retries, forced focus or simulated input to manufacture green.
2. Complete all outstanding ACT-016 checks on the exact candidate: adapted exact ACT-008 ShortcutOrderAudit with both original registration orders and specific KEY_PRESSED interposition, preserving the approved decision c27657b19dff7cacc1aa3df0672c4de7ad0d3a54 and testing the new deterministic per-phase selection/consumption contract, not requiring obsolete third-party interleaving. Execute frozen ACT-002 precompiled consumer fixtures without recompiling consumers, with only previously approved compatibility deltas (including ACT-007A worker/worker-edges expectations as already documented). Record each actual mode, PID, native signed/hex exit, outer exit, timeout, PASS marker and raw streams. Complete any still-outstanding publication checks, offline compile, PowerShell parse, diff/ABI and clean exact-commit verification required by original assignment. No assertions or timeout relaxation; no new compatibility waivers. If a genuine new failure, unexplained nonzero, timeout, unreliable evidence, unknown dirty change or unexpected remote advancement occurs, STOP.
3. If all required gates are GREEN, publish the exact existing ACT-016 commit by fast-forward to migration/java25-correctness, verify remote SHA, and append separate Reply ACT-016 continuation describing previous and new evidence separately, exact fixtures/modes, authorized deltas and limitations. Do not claim historical focus root cause resolved. No publication based on 80/80 alone.
4. After successful publication, continue already ACTIVE ACT-018 then ACT-019 sequentially, per DECISION ACT-017 at a5417483c0fbaeef87dafd51a6b1c7202ac5f10e and original assignments. Preserve scope, separate commits/Replies, navigation equivalence rules, desktop preflights, full final regression/ABI/lifecycle/architecture/performance gate and independent PowerShell reruns. STOP after ACT-019 or earlier blocking condition. No intermediate user approval needed within this scope.

Use remaining ACT-016 validation budget or a fresh ceiling of 90 min for this limited continuation; existing ACT-018/019 ceilings unchanged (90 min each). These are ceilings, not expected durations. All original STOP rules apply. No writes to other repositories or unrelated branches. Mailbox append-only.



---

## DECISION ACT-016-FIXTURE — isolate frozen binary fixtures and resume

**Status: USER APPROVED / ACTIVE.** Responding to Reply ACT-016 binary-fixture continuation at 6778d1378b41006ad0c285c77775ca3e04b87519. User authorizes a strictly test-harness-only correction for the diagnosed duplicate-FQCN linkage failure, followed by completion of ACT-016 validation and the already approved ACT-018/019 package. No production change, ABI waiver, assertion weakening, or assumption of desktop availability is authorized.

### Immutable checkpoints and evidence

Expected remote production: b5bb8fb10be4fd574f8f86eb8e77c4aec90ef563. Exact clean local ACT-016 candidate: eabf4c6725fc984363c57d54ff8633a66ebef20d, direct parent the expected production SHA. Verify both, branch/tree cleanliness, relevant class/dependency hashes, and previous evidence before work. Do not amend, rebase, regenerate production classes unnecessarily, or mix a harness change into this candidate. Preserve original frozen consumer classfiles, reference fixture bytes and original artifacts, including the keys JVM 19348 / runner 19384 failure 1/1, no timeout, key-edges NOT_RUN. Preserve original classpath and linkage error. Prior full regression 80/80, ABI, adapted ACT-008 and frozen meta/bridge results may be reused only with verified unchanged relevant inputs and environment; do not claim unrun or failed modes passed.

### ACT-016-FIXTURE: characterize and isolate

1. Inspect the original ACT-002 consumer bytecode (javap -p -c -s and constant-pool references as needed), reference binder and production binder, classpath, package-private access, transitive linkage, and any shared JavaFX/Fore types or callback identities. Record exact method descriptors, caller and defining loader/code source. The known collision is ru.inversion.fore.form.action.ActionKeyBinder in target/act-003/legacy-fixture versus target/classes; reference binder lacks legacyActions(Scene, boolean) invoked by production SceneKeyManager. Verify the actual failing linkage and class origin, rather than infer all compatibility from the duplicate name. Determine which paths are reference-behavior characterization and which are genuine frozen-consumer-to-production binary compatibility checks.
2. Select the smallest defensible harness-only isolation after this inspection. Consider narrowly scoped separate defining classloaders with explicitly shared parent-loaded JavaFX and public contract types, or separate JVM/fixture lanes only if they preserve the required original assertions and interactions. Preserve original consumer bytecode and assertions, original reference binder bytes and production class bytes; hash before/after. Do not blindly reorder the classpath, exclude a required reference implementation, shade/rename/rewrite frozen classes, add a shim/missing method to production or reference binder, silently replace a reference path with the production binder, or bypass linkage by mocks/reflection that changes the tested API. Same binary name in different loaders is different JVM type identity: demonstrate valid linkage, runtime package access, casts, callback/Scene identity and event interaction as applicable. Do not introduce a general plugin/classloading framework. Explain chosen isolation and rejected simpler alternatives. If original interactions cannot be preserved by honest harness isolation, STOP with the precise incompatible dependency edge and a proposed separately approvable alternative; do not manufacture a green verdict.
3. Prove isolation before accepting results: record class-loading diagnostics for frozen consumer, reference binder, production binder, SceneKeyManager/SceneKeyBinder, and relevant shared types; include loader identities, code sources, class hashes and actual call path. Show that production facade invokes the production binder and reference assertions still exercise the reference binder where required. If split lanes are used, demonstrate coverage equivalence for every original assertion, including cross-path interactions; reference-only success is NOT evidence of production binary compatibility. No recompilation of frozen consumer and no new compatibility exception. Only earlier explicitly approved ACT-007A worker/worker-edges and ACT-016 ordering/consumption contract deltas apply.
4. Work in isolated ignored harness/output directories, leaving the exact production candidate clean and immutable. Preserve original launchers and streams; record the harness delta, reproducible commands and hashes. If a tracked persistent harness correction is needed, report it separately for approval; do not include it in the preserved candidate or create an unrelated production commit under this authorization.
5. Before each focus-sensitive group and after any desktop/session change run the independent desktop preflight with actual showing/focus/pulses, foreground/session, native/outer exits and raw logs. If it fails, STOP before fixtures. On the corrected verified harness run the failed frozen keys mode and not-run key-edges, plus any frozen modes whose launch/class-loading environment changed, and all remaining required fixture/publication checks. Preserve original assertions, timeout, native signed/hex exit, outer exit, PID, PASS marker, raw streams and asynchronous failure evidence. Retain successful prior meta/bridge and ACT-008 only if their tested configuration is unaffected. A new unexpected nonzero, timeout, focus loss, unknown dirty state, evidence mismatch or unexpected remote advancement triggers STOP, not retries. The authorized rerun after the diagnosed harness correction is not a waiver or retry-to-green.

### Publication and continuation

If every mandatory gate is GREEN, original candidate/evidence equivalence established and isolation demonstrated, publish the exact existing ACT-016 commit eabf4c6725fc984363c57d54ff8633a66ebef20d by fast-forward; verify remote HEAD. Append a separate Reply ACT-016-FIXTURE documenting the original failure, dependency graph, chosen harness correction, isolation proof, frozen hashes, per-mode outcomes and remaining limitations, and a clear ACT-016 final publication verdict (separate Reply ACT-016). Distinguish reference characterization from production compatibility evidence. Include reproducible harness invocation and location. Do not claim the historical desktop-focus root cause resolved. If any required proof is missing, leave candidate unpublished and STOP.

Then continue ACT-018 followed by ACT-019 per a5417483c0fbaeef87dafd51a6b1c7202ac5f10e and e28cfdebfa572f14cf4069afb205bc3ee9f5d7b9, without intermediate approvals within the existing scope. Navigation changes only with proven equivalence; final full gate must include all supported modes, lifecycle/ABI/architecture/performance findings and independent user rerun commands. Separate task-scoped commits and Replies. STOP after ACT-019 or a new blocking condition. Keep all previous STOP rules, no writes to other repos, mailbox append-only.

Fresh ceiling for this bounded ACT-016-FIXTURE diagnosis/validation: 90 minutes; ACT-018 and ACT-019 original 90-minute ceilings unchanged. Ceilings are not estimates. No requirement to fill the budget.



---

## MAINT-001 — local target housekeeping

**Status: USER APPROVED / QUEUED for the next Codex entry at a safe checkpoint.** User reports local target exceeds 500 MB and asks to remove unnecessary artifacts. This is local housekeeping only, not a production task, not permission to interrupt the currently active ACT-018/019 sequence, and not permission to waive evidence retention. At the next session, inspect local state and this decision; if an active gate, worker, build, test process or unresolved STOP requires the artifacts, defer destructive cleanup until a safe checkpoint (prefer after ACT-019 final gate and independent acceptance if applicable). Inventory may be done read-only at any safe point. If already stopped, do not resume production tasks or remove anything needed to diagnose the stop under this maintenance authorization.

Scope: D:\Java\Projects\fore25\target only. Verify actual repository root and canonical target path, exclude junctions/reparse points and paths outside target; do not follow links, delete worktrees via raw filesystem removal, or touch source, tracked files, .git, Maven/Nexus caches, other repositories, or local configuration. No blanket mvn clean, git clean, or recursive target removal. Do not delete from active processes' output directories.

1. Measure target total and per top-level directory/file sizes before cleanup; identify largest contributors and classify as reproducible/disposable, required by pending work, unique evidence, or UNKNOWN. Inspect actual dependencies of active probes and frozen launchers rather than decide by age or directory name alone.
2. Preserve original frozen consumer/reference classfiles, ABI manifests, fixture isolation harness/launcher/hashes (target/act-016-fixture), original and resumed ACT-016 evidence, ACT-007A frozen dependencies, ACT-017 original incident/candidate and diagnostic evidence, all failure streams and native/outer exit records, final gate matrices and rerun requirements, any remaining unaccepted checkpoint and required classpaths. This list is illustrative, not exhaustive. In particular do not delete target/act-003/legacy-fixture or other archived fixture bytecode merely because it is old. Do not discard local artifacts needed to reproduce the approved compatibility evidence. Preserve original bytes of non-reproducible material; avoid compression/moving required files unless paths/dependencies are verified and original evidence is safely retained. Keep uncertain artifacts.
3. Remove only explicitly inventoried, confirmed disposable items: superseded reproducible compiler outputs, redundant identical copies, expired temporary build scratch or other artifacts demonstrably no longer referenced by pending tasks, final evidence or rerun scripts. Keep currently required target/classes and build inputs until safe. For duplicate evidence verify hashes and retention of one canonical copy and that paths are not needed before deletion; keep unique diagnostics. If no safe deletion is possible, report why rather than aim for an arbitrary size. Do not use cleanup to resolve a test failure.
4. Verify retained evidence and required harness paths/hashes, clean tracked Git status and unchanged production/branch state attributable to housekeeping. Record before/after bytes, reclaimed space, exact deleted paths with rationale, retained largest directories and reasons, any deferred work. Append Reply MAINT-001 to outbox; no production commit. If housekeeping cannot be done safely at the next entry, report DEFERRED and retain all necessary artifacts. Existing ACT-018/019 instructions and STOP rules remain unchanged.



---

## DECISION ACT-018/019 — reconcile maintenance-only mailbox advancement

**Status: ARCHITECT CONTINUATION within previously USER APPROVED scope.** The user relayed Codex's STOP after ACT-018 NO_CHANGE, with local reply at target/act-018-validation/reply.md and ACT-019 NOT_RUN. The architect caused the mailbox advancement by appending queued MAINT-001 while Codex was executing. Verified 82c6023fee05d92b0c5f83e857595c5fc3c1402c -> a60f50a03a84ffe56a0c0dd2c194f9e4ee31c147 is one fast-forward commit, inbox-only append (15 added lines, no deletions), outbox unchanged. Production remains eabf4c6725fc984363c57d54ff8633a66ebef20d. This is a known, nonconflicting administrative advancement, not a regression failure. The original STOP was correct. This entry explicitly clears that particular mailbox STOP only, within already approved ACT-018/019 execution scope. All other STOP rules remain.

On next entry fetch both branch heads, reconcile local mailbox by fast-forward or clean isolated checkout, inspect actual delta including this decision, verify exact production SHA, local cleanliness and preserved ACT-018 evidence. Publish the saved original ACT-018 Reply once, append-only, after confirming it matches actual results and checkpoint. Do not redo ACT-018 merely because the mailbox advanced; retain NO_CHANGE if evidence establishes direct traversal bypasses a legacy TAB veto filter. Do not modify navigation or assert untested equivalence. If evidence is incomplete or another unexplained failure/advancement exists, STOP with specifics.

Then execute the previously authorized ACT-019 full final gate on exact eabf4c6725fc984363c57d54ff8633a66ebef20d, including all required regression, binary/ABI, lifecycle, architecture and performance checks and explicit independent user rerun commands. Preserve raw evidence and mandatory focus-capable desktop preflights; do not assume desktop availability. Original 90 min ceiling applies. Publish separate ACT-019 Reply and STOP. MAINT-001 remains QUEUED; only perform read-only inventory if safe, defer deletion until final gate and independent acceptance are complete where applicable. No new production scope, no waiver, no blanket cleanup.

Architect will avoid mailbox writes during the active final gate to prevent another administrative interruption. This is not a blanket exemption for future unexpected remote advances.



---

## DECISION ACT-019 — final pending checks

**Status: USER APPROVED / ACTIVE.** Resume only the outstanding ACT-019 checks after Reply at cb2e39cc550db9ab354e8962f0deaab4ba2729fc. Exact production SHA eabf4c6725fc984363c57d54ff8633a66ebef20d; no production changes. Verify clean tree, remote SHA, unchanged relevant build/fixture inputs and saved evidence in target/act-019-validation. Reuse the completed final 80/80 regression and ABI results only if their equivalence is verified; do not repeat the full gate unnecessarily. Preserve the failed desktop preflight (JVM 17928 / runner 8896, visible, 127 pulses, unfocused, 0/0); cause UNKNOWN.

1. Fresh independent desktop preflight before each focus-sensitive group and after any session change. If it fails, STOP before that group. Do not assume the user has yielded desktop focus; no forced focus, retries-to-green or timeout/assertion relaxation.
2. Run the four frozen binary modes keys,key-edges,meta,bridge with the already validated isolated harness in target/act-016-fixture, unchanged frozen bytecode/assertions and verified loader identities/hashes. Run the prepared read-only footprint probe in target/act-019-validation. Record actual commands, conditions, native/outer exits, PID, timeout, markers, raw streams and measurements, distinguishing shallow size, allocation and retained heap; no unsupported claims. Preserve all previous evidence.
3. Publish one final Reply ACT-019 continuation with completed/pending gates, exact SHA, measured results and limitations, independent rerun commands and final verdict. Any new unexplained failure, evidence mismatch, dirty tree or unexpected remote advancement: STOP. No waiver, refactor, new task or production commit. MAINT-001 remains queued until final validation and independent user acceptance. STOP after the final reply.

Use the remaining ACT-019 budget or a fresh bounded ceiling of 45 minutes for this limited continuation (ceiling, not estimate). Architect mailbox remains read-only during this run.



---

## USER ACCEPTANCE — Action Engine Phase II

**Status: USER ACCEPTANCE / 7 of 7 PASS; ACT-019 FINAL GREEN; implementation package complete in tested scope.** User independently reported exactly "ALL SEVEN TESTS PASSED" after receiving the seven-mode rerun instructions for ActionWaitOwnership, ActionMapCore, ActionMapController, ActionMapAdapters, ActionKeyContext, ActionMapMarkView and ActionKeyUnified. This is user-reported acceptance, distinct from Codex evidence; no separately observed local user SHA, per-mode native exits or raw logs were supplied. Intended checkpoint and verified remote production: eabf4c6725fc984363c57d54ff8633a66ebef20d. Final ACT-019 Reply at mailbox 45b2c5a14bb5c441e47bf028ce5fc6b2918d3a32 reports 80/80 regression, 4/4 unchanged frozen modes with isolation proof, nine measurement JVMs/runner 0, ABI and other mandatory gates GREEN. ACT-018 NO_CHANGE. Architect verified remote fast-forward comparison from Alerts checkpoint 259f9887b95ee9cd7761a231e86bc57610325b88: seven commits ahead, zero behind. Historical incident causes and untested corporate integration/retained heap remain UNKNOWN; no additional waiver. This checkpoint is not a claim of independent raw-log or complete source review.

## MAINT-001 — activation at accepted checkpoint

Previously user-approved local target housekeeping may now proceed on the next Codex entry, as ACT-019 is FINAL GREEN/STOP and the user has reported independent acceptance. Follow original MAINT-001 instructions at a60f50a03a84ffe56a0c0dd2c194f9e4ee31c147 exactly, including safe path verification, pre-cleanup inventory, preservation of unique frozen bytecode, final isolated binary and measurement harnesses and evidence, archived failures, and all required rerun dependencies. No blanket clean, no tracked changes, no production commit, no unrelated task or refactor. Verify current state and no active processes; if unsafe defer. Append Reply MAINT-001 with before/after sizes, exact removed paths and retained material rationale; STOP.



---

## ACT-020 — simplify ActionMap key normalization

**Status: USER APPROVED / ACTIVE.** After accepted Action Engine Phase II and completed MAINT-001, simplify ActionMap key semantics. User explicitly wants String, Enum and int (Integer) as the only directly supported types; every other non-null object, including Long, becomes its toString() value. An Action ID should be deliberately chosen, not an arbitrary typed numeric domain.

Baseline exact production eabf4c6725fc984363c57d54ff8633a66ebef20d. Verify remote HEAD and clean working tree; preserve prior evidence. No competing work. Scope ActionMap normalization and necessary directly related callers/tests only.

Contract:
- Canonical types: String, Enum<?>, Integer. String identity remains case-insensitive; enum identity remains declaring class + ordinal (including constant-specific subclasses, not enum toString); Integer natural numeric identity. These three domains remain independent.
- All other Object keys, including Long, Byte, Short, BigInteger, BigDecimal, UUID, non-Comparable objects, are canonicalized using exactly one key.toString() call per public operation, then String semantics. Thus 1L and "1" are identical, but 1 (Integer) is distinct from both; BigDecimal("1.0") and BigDecimal("1.00") differ. Original objects are not retained. No implicit trimming, numeric parsing, type prefixes or extra wrappers. Null toString result fails before map mutation; thrown toString exceptions propagate without mutation.
- Add Object-accepting get/getLocal/put/remove. Preserve existing public Comparable<?> method descriptors as thin delegates to the Object path for ABI (architect default, no ABI removal authorized). Ensure one normalization even for put(key,null), and once before traversing parent hierarchy. Keep internal canonical keys Comparable<?> and existing forEachLocal contract unless concrete need dictates otherwise; comparator operates only on canonical values. Preserve null-key rules, lazy map allocation, put(key,null) local removal, return values, local-only clear/size, parent shadowing/cycles, retirement and current unsupported class-name collision policy. Keep valid-key lookup free of added allocation or wrappers.
- Update ActionMapCoreSmoke to replace explicitly superseded numeric/rejection expectations with new contract; retain unrelated assertions. Cover Object not Comparable, UUID, Long/String aliasing across local and parent, Integer/String and distinct enum domains, enum with custom toString, case-insensitive aliases, put/get/getLocal/remove and put-null, counting toString called once per operation including inherited lookup, null/throwing toString failure atomicity, lifecycle. Check ActionKeyBinder shadowing uses canonical IDs across scopes; no separate hotkey semantics changes.
- Run focused ActionMapCore in fresh JVMs and relevant ActionMapController, ActionMapAdapters, ActionKeyContext, ActionMapMarkView, ActionKeyUnified, then full supported regression gate, scoped ABI (old descriptors retained; Object overloads additions only), offline compile, parser and git diff --check. Reuse frozen fixtures only as appropriate; do not remove historical evidence or weaken unrelated assertions. Mandatory desktop preflight before focus-sensitive groups, actual native/outer exit, timeout, PASS markers, raw logs and normal STOP rules. No unrelated refactor, performance framework or production changes outside scope.

If GREEN, publish one task-scoped fast-forward commit and Reply ACT-020 with exact SHA, old/new identity semantics, evidence and independent rerun command(s); STOP. If blocked, preserve candidate/evidence and STOP. Ceiling 90 minutes, not an estimate.



---

## USER ACCEPTANCE — ACT-020

**Status: USER ACCEPTED / CLOSED in tested scope.** User independently reported exactly "ALL SIX TESTS PASSED" following the ACT-020 rerun instructions for ActionMapCore, ActionMapController, ActionMapAdapters, ActionKeyContext, ActionMapMarkView and ActionKeyUnified. Intended checkpoint and verified remote production SHA: 25ca2aa2fd3d5ebb679fdbf37da1c77f3863f9ec. User-reported acceptance is distinct from agent evidence; no independent local user SHA, per-mode exit codes or raw logs were supplied. Reply ACT-020 at 7bb4ed3fa8c41a09fc859abd91c544c9ed8b1b62 reports focused 8/8 and full 80/80 GREEN, original Comparable descriptors retained, four Object overloads added. Architect verified the one-commit fast-forward and scoped source/test diff. Canonical domains String (case-insensitive), Enum (declaring class + ordinal), Integer; other non-null objects normalize once via toString(). No new task activated by this checkpoint. STOP remains in force.



---

## DECISION — Action Engine Phase III, bounded integration package ACT-021 to ACT-023

**Status: USER APPROVED / ACTIVE.** The user approved the proposed correctness review and several consecutive integration tasks. Baseline accepted ACT-020 production 25ca2aa2fd3d5ebb679fdbf37da1c77f3863f9ec, mailbox acceptance 530cc92792032c6f467b21c9a8b28e1f4df466f0. Sequential execution only: ACT-021 -> ACT-022 -> ACT-023A (MARK) -> ACT-023B (REFRESH) -> ACT-023G (package gate). No intermediate user confirmation for work within this bounded scope. ACT-024 onward are NOT ACTIVE. Architect does not write mailbox while package runs.

Preserve agreed architecture: Control owns behavior, Skin owns presentation; DSFXAdapter/TSFXAdapter are mini-controllers, table/tree are views; ActionMap owns registry, ActionKeyBinder resolves shortcuts, ActionInvocation invokes; WAIT belongs to form, progress to execution. ACT-020 canonical key contract unchanged. Lazy state, minimal memory overhead; no additional global manager, command bus, duplicate registry, parallel GUI hierarchy or universal workflow engine. Public JavaFX API only. Preserve legacy ABI and virtual hooks; only existing explicitly approved compatibility changes allowed. Do not reinterpret a successful compile as GUI correctness. No writes to main, JInvCommon or reference repositories.

### ACT-021 — core correctness review and bounded fixes

Investigate actual ProgressDelivery/FormWait callers and worker submission/completion boundaries, not only isolated classes. Characterize existing contract with targeted tests on exact baseline before fixes. Source observations to verify, NOT preclassified bugs:
- ProgressDelivery.attach registers ACTIVE/attached before addListener/publish; inspect rollback when source registration, current predicate, source read or target publication fails.
- close marks detached before removeListener and registry cleanup; inspect exception safety, reentrant callbacks, idempotence, empty-bucket removal, stale/queued publishes and whether latest active valid source is restored after completion or retirement.
- publish checks ownership before invoking potentially user-controlled current/source/property methods; test reentrant replacement/close/reset during these calls and guard against stale publication without global locking or reusable mutable dispatch state.
- FormWait release/reset/retire and legacy hooks: nested acquire during caption delivery, final release or state transition, old generation completion after new session, throwing cleanup, duplicate finish and passive external listeners. Check retirement does not leave active sinks/listeners or prematurely restore ACTIVE for a newer live registration. Separate caller exceptions from JavaFX listener exceptions that JavaFX may report through uncaught handlers. Investigate actual supported observable/property contracts; do not invent behavior for arbitrary broken custom implementations.
- Keep FX confinement: background source changes queue delivery, no write or registry mutation off FX thread unless already explicitly supported. No framework bind/unbind of external targets; bound targets untouched. Preserve approved latest-started still-active ownership, exact target identity, late release and stateMessage/message distinction. No executor, SQL transaction, nextAction or thread pool policy changes.
- Verify idle index entries and retention separately from backing capacity; static map alone is not proof of leak. Measure or inspect before claiming memory improvements. Keep per-form lazy allocation. Do not replace index with WeakHashMap or broad synchronization as a speculative fix.
Add small deterministic probes that reproduce genuine defects; preserve prior overlap/rejection/cancel/reset tests. Fix only demonstrated defects with smallest coherent implementation, document before/after and limitations. If no defect demonstrated, NO_CHANGE with evidence. New unexplainable failure or needed contract/security change: STOP. Focused fresh JVMs, ActionWaitOwnership, relevant worker/form/lifecycle/ABI/frozen compatibility gates, offline compile/parser/diff; full gate if production changed. Separate Reply ACT-021 and task-scoped commit only if GREEN. Ceiling 90 min.

### ACT-022 — production activation of contextual ActionMap

On verified next clean HEAD, discover actual production Scene initialization/disposal in SDI, MDI and existing JInvKeyboardManager.initKeyBoard call sites; map real controller/adapter attach and authorization contracts. ActionKeyBinder currently uses explicit attach(Scene, Predicate) and only dispatches native KEY_PRESSED in FILTER when authorized != null. Legacy facades already share owner. Establish minimal real production activation path that respects existing scene owner lifecycle, reinitialization, close/reopen, shared MDI scene and exact authorization contract. No unconditional action -> true as a substitute for corporate/security policy; no guess about which native commands require authorization. If no existing sound authorization contract or safe activation owner exists, report BLOCKED with precise missing decision and STOP rather than enable globally. Preserve explicit attach/close and already-approved legacy FILTER/HANDLER ordering, consumed events and live collections. No mandatory map per simple control, no eager initialization for empty controllers/adapters, no registration of all standard operations. Avoid duplicate scene hooks, retained closed controllers, accidental early native consumption and cross-phase behavioral changes. Test through actual production initialization path, not just direct attach in probe. Cover sibling/nested forms, adapter precedence, detach/reparent/reopen, shared Scene lifetime and close isolation, existing legacy commands, disabled/denied/ambiguous and native fallback; characterize unavailable corporate/MDI evidence explicitly. If existing production path is already equivalent, NO_CHANGE with evidence; do not claim complete activation from synthetic explicit attach alone. Separate Reply ACT-022, task-scoped commit only after required focused and full regression/ABI gates. Ceiling 120 min.

### ACT-023A — current-row MARK, complete the narrow vertical slice

Use existing DSFXAdapter.ACTION_MARK_CURRENT_ROW and existing adapter-owned ForeAction from ACT-017; do not introduce a second ID/operation or rename into StandardAction without separate decision. Existing toolbar and JInvTable Ctrl-click already route via invokeMarkCurrentRow(); JInvKeyboardManager.markHandler still directly calls revertMarkCurrentRow(). Inspect all current-row MARK entrypoints including Scene HANDLER Insert/Space and per-table KEY_PRESSED ENTER/SPACE handlers. These have existing phase/consumption/selection/editing semantics; do not blindly register those hotkeys in native FILTER (would change priority and may steal text entry/navigation/edit commit). The immediate goal is one functional operation for current-row MARK, not migration of all keyboard shortcuts to FILTER. Preserve existing phase and consumption where necessary; legacy keyboard triggers may delegate to the common adapter operation. Ensure keyboard-first invocation works before any toolbar/mouse call; obtain/create only when relevant. Retain mark enablement, original virtual revertMarkCurrentRow, valid captured attachment, disabled behavior, admission/security and error handling. Test no double toggle when table and Scene handlers both observe the event, no mutation of replaced/closed attachment, actual table editing/selection/native fallback, disabled/unsupported mark, keyboard-first, toolbar/Ctrl-click shared identity, sibling adapters and non-table contexts. Do not change ENTER/TAB navigation, mark-all/unmark-all workers or tree semantics in this slice. If moving any key phase or consumption is necessary beyond already authorized contract, STOP for decision. If no safe unified path established, report exact blocker. Separate Reply ACT-023A and scoped commit after focused/full regression/ABI. Ceiling 90 min.

### ACT-023B — REFRESH vertical slice (bounded)

Only after successful ACT-021/022/023A gates. Characterize existing F8 and actual toolbar/menu refresh entrypoints for table and tree. Distinguish UI redraw refresh() from data reload/executeQuery()/refreshTree(), controller virtual onRefreshData(adapter) and any business authorization, filter-dialog enablement, worker/selection/transaction semantics. Use StandardAction.REFRESH only where semantics really match; operation belongs to appropriate adapter or form, not keyboard manager. Preserve controller overrides and existing invocation context; never move data reload directly into a view or bypass form callbacks. Identify real presentation consumers and unify at least one proven equivalent table path plus keyboard; tree only if separately characterized equivalence is clear within this slice. Do not invent tree behavior, force a unified REFRESH across unlike scopes, change F7/Shift+F8 or implement CRUD. Preserve existing keys, admission, error reporting, no-op/consumption behavior and current attachment validity. Avoid native FILTER vs legacy phase/priority changes not already authorized, duplicate accelerators/actions or execution. Add focused shown-scene fixture covering keyboard-first, actual selected presentation path, with/without controller override, two adapters, disabled/denied, detach/close, non-SQL/unsupported paths, exactly one reload and no extra transaction/worker effects. Real corporate DB integration is not inferred from offline fixtures. If equivalence of selected slice cannot be established without contract change, report NO_CHANGE for unmodified scope or BLOCKED if a decision is required; no speculative refactor. Separate Reply ACT-023B, task-scoped commit only with required gates GREEN. Ceiling 90 min.

### ACT-023G — final package verification

On final exact clean remote SHA, verify task chain, actual diff/ABI and approved behavior only. Full supported regression including any new official modes, offline compile/parser/diff, native/outer exits, PID/markers/timeouts, appropriate isolated frozen binary consumer compatibility without recompilation and unchanged assertions except explicit approved deltas, lifecycle/security findings and bounded memory impact (distinguish allocation/shallow/retained). Report actual production activation and which MARK/REFRESH entrypoints migrated versus legacy, all limitations and independent PowerShell reruns from D:\Java\Projects\fore25, remote origin, branch migration/java25-correctness. No speculative optimization of Match or unrelated refactor. Separate final Reply ACT-023G and STOP. Ceiling 90 min.

### Execution discipline

Per task verify exact HEAD, clean tree and expected remote ancestry before work/publication, separate task-scoped fast-forward commit and append-only Reply with evidence. Before each focus-sensitive test group and after any desktop/session change, independent focus-capable desktop preflight; do not assume input is yielded. If preflight fails, STOP before group. Any new unexplained native/runner failure, timeout, unexpected advancement, unknown dirty state, evidence mismatch, unresolved correctness/security decision triggers STOP; preserve candidate and original evidence. No forced focus, retry-to-green, assertions/timeout relaxation, force push, old waiver reuse. NO_CHANGE is legitimate if explicitly justified and does not hide a required blocker. Record actual native signed/hex exit and outer exit separately, raw logs, timeout and PASS markers. Keep historical fixtures and harnesses under target; reuse one safe mailbox checkout where possible rather than creating redundant full worktrees, no blanket clean. All necessary scoped test and Git operations authorized; no new production scope beyond this package. Total ceilings 480 min, not estimates; use only necessary budget.



---

## DECISION ACT-021-DIAG — controlled native-exit diagnosis

**Status: USER APPROVED / ACTIVE for diagnosis only.** In response to Reply ACT-021 at 3b20f5f12b83d318704781c93e45a492b67befc7, diagnose the unexpected native JVM termination in RadioReadOnly (CTRL-006), mode 59 of the full ACT-021 gate. Accepted production A: 25ca2aa2fd3d5ebb679fdbf37da1c77f3863f9ec. Preserved local candidate B: 2cabd4432ea2d30e2988514cc8266d31cea3a11c, direct parent A. The original run printed CTRL-006 PASS but JVM 6080 exited -1073740791 (0xC0000409), runner 18448 exited 1, timeout False. This is RED, not a successful test. Root cause UNKNOWN. Do not assume the crash is related or unrelated to ProgressDelivery/FormWait; earlier native incidents are not waivers.

1. Verify exact local and remote commits, candidate parent and clean state. Preserve candidate unchanged; record hashes and original evidence under target/act-021-validation, including matrix, run-59 stdout/stderr/PID/native exit, original argfile/classpath and runner output if present. Inspect available Windows Application/WER event records near the actual incident time and PID, faulting module/version, exception details, fault offset and any available JVM hs_err or crash dump references. Record positive findings and missing/unavailable evidence separately. Do not change Windows settings, install diagnostics, collect unrelated process data or treat 0xC0000409 alone as root cause.
2. Prepare isolated exact A/B worktrees and distinct build/output directories, reusing existing verified artifacts where appropriate. Verify equivalent JDK, JavaFX, ControlsFX, offline Maven settings, probe source/bytecode (RadioReadOnly must be unchanged or differences explicitly explained), runtime classpaths, arguments and desktop/session conditions. No production source changes, no candidate amend/rebase, no modifications to original probe/assertions, timeout or exit policy. Use actual child JVM and outer runner exit capture. Preserve original failure separately.
3. Predeclare bounded controlled comparison: 8 fresh JVMs per side (16 total), order A/B/B/A repeated four times, using identical official RadioReadOnly probe semantics. Verify focus-capable desktop before starting and before each focus-sensitive block, and after any desktop/session change. If preflight fails, STOP before tests. Record for each attempt exact revision, timestamps, JVM PID, runner PID, signed/hex exit, outer exit, timeout, PASS marker, stdout/stderr, available matched native fault event and foreground/session observations. The original known crash signature is the diagnostic endpoint, not a regression waiver: if the same native exit recurs, preserve it, inspect matched fault evidence and stop that comparison for reporting rather than repeatedly running until green. Any new unexpected failure, unreliable evidence, divergence, dirty state or remote advancement: STOP. If a comparable crash occurs on baseline, it proves occurrence on baseline under the observed conditions only; if on candidate, it does not alone prove causation. A zero-failure matrix means not reproduced in this bounded sample, not proven equivalence or cause. No retries beyond the fixed design, forced focus, synthetic user input to manufacture green, or suppression of failure.
4. Review actual dependencies and call paths of CTRL-006 relative to candidate changes, including JavaFX shutdown and test runner behavior; distinguish direct code involvement, indirect possibilities and UNKNOWN. Explain whether evidence supports a candidate-specific regression, common native/desktop failure, or unresolved result. Recommend the minimal subsequent gate with exact preserved artifacts and residual risk. Do not broaden diagnosis or implement speculative fixes.
5. Append Reply ACT-021-DIAG to outbox with exact SHA, evidence, full attempted matrix, any not-run steps and recommendation; STOP. No publication of ACT-021, no production edits, no continuation of remaining 80-mode gate/frozen fixtures or ACT-022/023A/023B/023G under this decision. A subsequent architect/user decision is required to resume the package. Preserve prior 58 green modes and expanded focused tests as historical candidate evidence, without treating them as completion.

Ceiling 90 minutes for this bounded diagnostic task, not an estimate. All unrelated original STOP rules remain in effect. Reuse safe mailbox checkout, preserve all original evidence; no target cleanup.



---

## DECISION ACT-021 — resume after controlled native-exit diagnosis

**Status: USER APPROVED / ACTIVE.** The user accepts the bounded residual uncertainty of the specific historical CTRL-006 native exit and explicitly authorizes continuation of the previously approved Phase III package, following Reply ACT-021-DIAG at 1096d97a0bafdf6a2b65eff7d65a2c6c97b4a141. This clears only that historical STOP; it does not reclassify the failed run as PASS, establish root cause, waive any gate or authorize future unexplained exits.

Verified remote baseline remains 25ca2aa2fd3d5ebb679fdbf37da1c77f3863f9ec. Resume from exact preserved local candidate 2cabd4432ea2d30e2988514cc8266d31cea3a11c, direct child of baseline. Preserve original CTRL-006 RED record (JVM 6080, signed -1073740791 / 0xC0000409, runner 18448 exit 1, no timeout), WER/Application Error records identifying ucrtbase.dll, and all original and diagnostic artifacts. A/B/B/A x4 yielded A 8/8 and B 8/8 with native/outer 0/0 and unchanged probe bytecode/dependencies; direct involvement of changed WAIT/progress paths was not found. Isolated matrix did not reproduce the preceding full-suite sequence. Root cause remains UNKNOWN; no native stack/dump available. No evidence of candidate-specific regression in the bounded sample, not proof of absence.

1. Verify exact clean local candidate, direct parent, current branch and remote SHA, relevant inputs and preserved hashes. Do not amend, rebase, rewrite or repeat implementation. Retain earlier four-defect reproductions, focused/ABI results and 58 green modes as historical evidence.
2. Run fresh independent focus-capable desktop preflight, then a **new complete official ACT-021 full regression gate from mode 1**, preserving existing order including CTRL-006 at its normal position, all supported modes, assertions, timeouts and actual child/outer exit capture. No splicing old 58 results into the new gate or treating diagnostic PASS as gate PASS. Preserve previous matrices separately. Repeat desktop preflight before each focus-sensitive group and after session changes; do not assume input is yielded. If preflight fails STOP before the group.
3. Complete the outstanding fresh frozen binary compatibility modes using the established isolated harness with unchanged frozen consumer/reference bytes, assertions and loader identity/hash verification; complete required focused, offline compile, PowerShell parse, ABI and git diff checks as prescribed by original ACT-021 assignment. Previously completed focused/ABI evidence may be reused only where exact candidate and input equivalence are verified. Record PID, signed/hex native exit, outer exit, timeout, PASS markers and raw streams for all new runs.
4. Any new unexplained nonzero exit, native crash (including the same historical signature), timeout, unreliable evidence, unexpected remote advance or dirty state: STOP immediately, preserve candidate and matched Windows/WER evidence if available. No arbitrary retries, forced focus, synthetic input to manufacture green, relaxation or automatic waiver.
5. Only when every mandatory ACT-021 gate is GREEN, publish the exact existing candidate by fast-forward, verify remote SHA and append separate Reply ACT-021 continuation with historical RED, diagnostic outcome, new full gate, frozen compatibility, actual changes and limitations clearly separated.
6. Then resume ACT-022 -> ACT-023A -> ACT-023B -> ACT-023G sequentially under the original approved Phase III assignment at 2deea5f24fdace7f7ae737a002bfdfa416597050. All original correctness, authorization, phase/consumption, lifecycle, ABI, regression and STOP rules remain binding. No intermediate confirmation for already approved scope; unresolved architectural/security changes require STOP. Separate task commits/Replies. ACT-024 onward NOT ACTIVE. STOP after ACT-023G or earlier blocker.

Fresh ceiling 90 minutes for ACT-021 validation continuation; subsequent ceilings unchanged (120/90/90/90 minutes). These are ceilings, not estimates. No cleanup, unrelated production work or mailbox history changes. Architect mailbox remains read-only while this package runs.

