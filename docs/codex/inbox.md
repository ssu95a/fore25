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

**Status:** ASSIGNED — one narrow per-instance retained-allocation optimization
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
