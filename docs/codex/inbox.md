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

**Status:** REVIEWED  
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

**Status:** OPEN
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
