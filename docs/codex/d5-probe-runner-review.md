# D5-003 probe sources

Complete source from commit 692ed672b6215dd5845491b7bb818f36ae324b10.

## src/test/probes/Test-D5ExecuteQuery.ps1

```powershell
[CmdletBinding(DefaultParameterSetName = 'Extraction')]
param(
    [Parameter(ParameterSetName = 'Extraction')][switch] $Before,
    [Parameter(ParameterSetName = 'Corporate', Mandatory)][switch] $Corporate
)

$ErrorActionPreference = 'Stop'
$root = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '../../..'))
$java = (Get-Command java -ErrorAction Stop).Source
if ($Corporate) {
    $maven = (Get-Command mvn -ErrorAction Stop).Source
    $dependencyFile = Join-Path $root ('target/d5-dependencies-' + [Guid]::NewGuid().ToString('N') + '.txt')
    Push-Location $root
    try {
        & $maven '-o' '-Pbuild' '-Dmaven.buildNumber.skip=true' '-DskipTests' 'compile' 'dependency:build-classpath' "-Dmdep.outputFile=$dependencyFile" '-DincludeScope=test'
        if ($LASTEXITCODE -ne 0) { throw 'D5 corporate Maven compile/classpath failed' }
    } finally {
        Pop-Location
    }
    $dependencies = [IO.File]::ReadAllText($dependencyFile).Trim()
    if (!$dependencies) { throw 'Maven returned an empty dependency classpath' }
    $classpath = (Join-Path $root 'target/classes') + [IO.Path]::PathSeparator + $dependencies
    & $java '--enable-native-access=ALL-UNNAMED' '-cp' $classpath (Join-Path $PSScriptRoot 'd5/D5ExecuteQueryProbe.java')
    if ($LASTEXITCODE -ne 0) { throw 'D5 corporate probe failed' }
    return
}

$relativePath = 'src/main/java/ru/inversion/dataset/fx/DSFXAdapter.java'
if ($Before) {
    $baseline = '0dd94764ac4d75eccc75a57c85ad005676875d19'
    $source = (& git -C $root show "${baseline}:$relativePath") -join "`n"
    if ($LASTEXITCODE -ne 0) { throw 'D5 baseline source unavailable' }
} else {
    $source = [IO.File]::ReadAllText((Join-Path $root $relativePath))
}
$method = [regex]::Match($source, '(?s)public void executeQuery\(BiConsumer< Boolean, Throwable> clb, IFormStateListener stateListener, boolean parallel, boolean readAllRecords \) \{(.*?)\r?\n    \}\r?\n')
if (!$method.Success) { throw 'Production method not found' }
$body = $method.Groups[1].Value
$asyncStart = $body.IndexOf('        Runnable executeRun')
$syncStart = [regex]::Match($body, '(?m)^            synchronized\( DATA_SET_LOCK \)')
if ($asyncStart -lt 0 -or !$syncStart.Success -or $syncStart.Index -le $asyncStart) {
    throw 'Production sync boundaries not found'
}
# Извлекаем пролог и синхронную ветку без переписывания их логики.
$sync = $body.Substring(0, $asyncStart) + '        if (!parallel) {' + "`n" + $body.Substring($syncStart.Index)
$output = Join-Path $root ('target/d5-execute-query-probe/' + [Guid]::NewGuid().ToString('N'))
New-Item -ItemType Directory -Force -Path $output | Out-Null
$probe = Join-Path $output 'D5Extraction.java'
$javaSource = @'
import java.util.*;
import java.util.function.BiConsumer;

class D5Extraction {
    interface IFormStateListener {}
    static class JInvTable {
        IFormStateListener getController() { return null; }
    }
    static class Platform {
        static void runLater(Runnable run) { throw new AssertionError("Unexpected default callback"); }
    }
    static class JInvErrorService {
        static void handleException(Object owner, Throwable error) {}
    }

    final Object DATA_SET_LOCK = new Object();
    final List<String> events = new ArrayList<>();
    final Thread caller = Thread.currentThread();
    final RuntimeException queryError = new RuntimeException("query");
    final RuntimeException callbackError = new RuntimeException("callback");
    final RuntimeException positionError = new RuntimeException("position");
    Object tableView = new JInvTable();
    Dataset dataset = new Dataset();
    boolean allow = true;
    boolean guardFails;
    boolean queryFails;
    boolean callbackFails;
    boolean positionFails;

    class Dataset {
        void executeQuery(boolean all) {
            events.add("query(all=" + all + ")");
            if (queryFails) throw queryError;
        }
    }

    Dataset getDataSet() { return dataset; }
    boolean checkForFullScan() {
        events.add("guard");
        if (guardFails) throw queryError;
        return allow;
    }

    void positionRow(int index) {
        events.add("position(" + index + ")");
        if (positionFails) throw positionError;
    }

    void executeQuery(BiConsumer<Boolean, Throwable> clb, IFormStateListener stateListener,
                      boolean parallel, boolean readAllRecords) {
__SYNC__
    }

    void check(String name, Class<? extends Throwable> escaped, String... expected) {
        Throwable observed = null;
        try {
            executeQuery((ok, error) -> {
                if (error != null && error != queryError) throw new AssertionError("Wrong error identity", error);
                events.add("callback(" + ok + "," + (error == null ? "null" : error.getMessage()) + ")");
                if (Thread.currentThread() != caller || !Thread.holdsLock(DATA_SET_LOCK))
                    throw new AssertionError("Wrong callback thread or lock");
                if (callbackFails) throw callbackError;
            }, null, false, true);
        } catch (Throwable error) {
            observed = error;
        }
        if ((observed == null ? null : observed.getClass()) != escaped
                || (observed != null && (positionFails ? observed != positionError
                   : callbackFails && observed != callbackError))
                || !events.equals(List.of(expected)))
            throw new AssertionError(name + " " + events, observed);
        System.out.println(name + " " + events + " escaped=" + observed);
    }

    public static void main(String[] args) {
        boolean before = args.length == 1 && args[0].equals("before");
        System.out.println((before ? "BEFORE" : "AFTER") + ": source extraction + doubles, no JavaFX or DB");
        String[] successTrace = before
                ? new String[]{"guard", "query(all=true)", "position(0)"}
                : new String[]{"guard", "query(all=true)", "callback(true,null)", "position(0)"};
        new D5Extraction().check("success", null, successTrace);
        var successCallback = new D5Extraction();
        successCallback.callbackFails = true;
        successCallback.check("throwing-success-callback", before ? null : RuntimeException.class, successTrace);
        successCallback.events.clear();
        successCallback.positionFails = true;
        successCallback.check("position-masks-success-callback-error", RuntimeException.class, successTrace);
        successCallback.events.clear();
        successCallback.callbackFails = false;
        successCallback.check("success-with-position-enqueue-error", RuntimeException.class, successTrace);
        var failure = new D5Extraction();
        failure.queryFails = true;
        failure.check("query-error", null, "guard", "query(all=true)", "callback(false,query)", "position(0)");
        failure.events.clear();
        failure.callbackFails = true;
        failure.check("throwing-failure-callback", RuntimeException.class,
                "guard", "query(all=true)", "callback(false,query)", "position(0)");
        failure.events.clear();
        failure.positionFails = true;
        failure.check("position-masks-callback-error", RuntimeException.class,
                "guard", "query(all=true)", "callback(false,query)", "position(0)");
        failure.events.clear();
        failure.callbackFails = false;
        failure.check("query-error-with-position-enqueue-error", RuntimeException.class,
                "guard", "query(all=true)", "callback(false,query)", "position(0)");
        var declined = new D5Extraction();
        declined.allow = false;
        declined.check("declined", null, "guard", "position(0)");
        var failedGuard = new D5Extraction();
        failedGuard.guardFails = true;
        failedGuard.check("guard-error", null, "guard", "callback(false,query)", "position(0)");
        var missing = new D5Extraction();
        missing.dataset = null;
        missing.check("null-dataset", null);
        var badTable = new D5Extraction();
        badTable.tableView = new Object();
        badTable.check("pre-try-cast", ClassCastException.class);
    }
}
'@
[IO.File]::WriteAllText($probe, $javaSource.Replace('__SYNC__', $sync))
$mode = if ($Before) { 'before' } else { 'after' }
& $java $probe $mode
if ($LASTEXITCODE -ne 0) { throw 'D5 extraction probe failed' }
```

## src/test/probes/d5/D5ExecuteQueryProbe.java

```java
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.scene.Scene;
import javafx.scene.control.TableView;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import ru.inversion.dataset.ArrayDataSet;
import ru.inversion.dataset.IDataSet;
import ru.inversion.dataset.fx.DSFXAdapter;
import ru.inversion.fx.form.IFormStateListener;
import ru.inversion.fx.form.StateEnum;

class D5ExecuteQueryProbe {
    record Row(String name) {}
    record Result(boolean ok, Throwable error) {}

    public static void main(String[] args) throws Exception {
        require(Runtime.version().feature() == 25, "Java 25 required");
        var asyncFailure = new AtomicReference<Throwable>();
        var test = new FutureTask<Void>(() -> {
            Thread.currentThread().setUncaughtExceptionHandler((thread, error) -> asyncFailure.compareAndSet(null, error));
            return run();
        });
        Platform.startup(test);
        try {
            test.get(30, TimeUnit.SECONDS);
            var barrier = new FutureTask<Void>(() -> null);
            Platform.runLater(barrier);
            barrier.get(30, TimeUnit.SECONDS);
            if (asyncFailure.get() != null) {
                throw new AssertionError("Queued JavaFX operation failed", asyncFailure.get());
            }
            System.out.println("AFTER FIX PASS (real adapter and JavaFX; full-scan dialog excluded)");
        } finally {
            Platform.exit();
        }
    }

    @SuppressWarnings("unchecked")
    static Void run() throws Exception {
        var adapter = new DSFXAdapter<Row>();
        var calls = new ArrayList<Result>();
        var queries = new ArrayList<Boolean>();
        var injectedError = new IllegalStateException("controlled query error");
        BiConsumer<Boolean, Throwable> callback = (ok, error) -> {
            require(Platform.isFxApplicationThread(), "Unexpected callback thread");
            calls.add(new Result(ok, error));
        };
        adapter.executeQuery(callback, null, false, true);
        require(calls.isEmpty(), "Null Dataset callback");
        System.out.println("null-dataset callbacks=" + calls);

        var real = new ArrayDataSet<>(Row.class,
                FXCollections.observableArrayList(new Row("A")), false);
        // Вызовы Dataset делегируются реальному ArrayDataSet. Ошибка вводится на границе запроса.
        var dataset = (IDataSet<Row>) Proxy.newProxyInstance(IDataSet.class.getClassLoader(),
                new Class<?>[]{IDataSet.class}, (proxy, method, args) -> {
                    if (method.getName().equals("executeQuery") && method.getParameterCount() == 1) {
                        boolean readAll = (boolean) args[0];
                        queries.add(readAll);
                        if (!readAll) throw injectedError;
                    }
                    try {
                        return method.invoke(real, args);
                    } catch (InvocationTargetException error) {
                        throw error.getCause();
                    }
                });
        var table = new TableView<Row>();
        var stage = new Stage();
        stage.setScene(new Scene(new StackPane(table), 320, 200));
        adapter.bindTable(dataset, table, null, false);
        var listener = new IFormStateListener() {
            private final SimpleObjectProperty<StateEnum> state = new SimpleObjectProperty<>();
            private final SimpleStringProperty text = new SimpleStringProperty();
            public SimpleObjectProperty<StateEnum> stateProperty() { return state; }
            public SimpleStringProperty stateTextProperty() { return text; }
        };

        adapter.executeQuery(callback, listener, false, true);
        require(queries.equals(List.of(true)) && calls.equals(List.of(new Result(true, null))),
                "Unexpected success trace: " + queries + " " + calls);
        System.out.println("success queries=" + queries + " callbacks=" + calls);

        adapter.executeQuery(callback, listener, false, false);
        require(queries.equals(List.of(true, false))
                        && calls.equals(List.of(new Result(true, null), new Result(false, injectedError))),
                "Unexpected error trace: " + queries + " " + calls);
        System.out.println("query-error queries=" + queries + " callbacks=" + calls);

        try {
            adapter.executeQuery(callback, null, false, true);
            throw new AssertionError("Expected controller cast failure");
        } catch (ClassCastException expected) {
            require(queries.size() == 2 && calls.size() == 2, "Pre-try failure ran query/callback");
            System.out.println("pre-try-cast escaped=ClassCastException");
        }
        var callbackError = new IllegalStateException("controlled callback error");
        BiConsumer<Boolean, Throwable> throwingCallback = (ok, error) -> {
            callback.accept(ok, error);
            throw callbackError;
        };
        for (boolean readAll : new boolean[]{true, false}) {
            calls.clear();
            queries.clear();
            try {
                adapter.executeQuery(throwingCallback, listener, false, readAll);
                throw new AssertionError("Callback exception did not escape");
            } catch (IllegalStateException error) {
                require(error == callbackError, "Unexpected escaped exception: " + error);
            }
            var expected = new Result(readAll, readAll ? null : injectedError);
            require(queries.equals(List.of(readAll)) && calls.equals(List.of(expected)),
                    "Unexpected throwing-callback trace: " + queries + " " + calls);
            System.out.println("throwing-callback queries=" + queries + " callbacks=" + calls);
        }
        return null;
    }

    static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
```
