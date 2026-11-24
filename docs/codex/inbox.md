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

**Status:** ASSIGNED — HIGH / RED→GREEN TextArea LOV ownership, timebox approximately 60 minutes
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
