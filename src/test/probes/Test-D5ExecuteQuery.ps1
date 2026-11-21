[CmdletBinding(DefaultParameterSetName = 'Extraction')]
param(
    [Parameter(ParameterSetName = 'Extraction')][switch] $Before,
    [Parameter(ParameterSetName = 'Corporate', Mandatory)][switch] $Corporate
)

$ErrorActionPreference = 'Stop'
$root = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '../../..'))
if ($Corporate) {
    if ($env:JAVA_HOME) {
        $java = Join-Path $env:JAVA_HOME 'bin/java.exe'
        $javac = Join-Path $env:JAVA_HOME 'bin/javac.exe'
        if (!(Test-Path -LiteralPath $java -PathType Leaf) -or !(Test-Path -LiteralPath $javac -PathType Leaf)) {
            throw "JAVA_HOME must point to a JDK 25 with java.exe and javac.exe: $env:JAVA_HOME"
        }
    } else {
        $java = (Get-Command java -CommandType Application -ErrorAction SilentlyContinue).Source
        $javac = (Get-Command javac -CommandType Application -ErrorAction SilentlyContinue).Source
        if (!$java -or !$javac) { throw 'JDK 25 required: set JAVA_HOME or put java and javac on PATH' }
    }
    $javaVersion = & $java '--version'
    if ($LASTEXITCODE -ne 0 -or ($javaVersion -join "`n") -notmatch '^(?:openjdk|java) 25(?:[. +\-]|$)') {
        throw "D5 requires Java 25. $java reported: $javaVersion"
    }
    $javacVersion = & $javac '--version'
    if ($LASTEXITCODE -ne 0 -or ($javacVersion -join "`n") -notmatch '^javac 25(?:[. +\-]|$)') {
        throw "D5 requires javac 25. $javac reported: $javacVersion"
    }

    $maven = (Get-Command mvn -CommandType Application -ErrorAction SilentlyContinue).Source
    if (!$maven) {
        $maven = Join-Path $root 'mvnw.cmd'
        if (!(Test-Path -LiteralPath $maven -PathType Leaf)) {
            throw 'D5 requires mvn on PATH or mvnw.cmd in the project root'
        }
    }
    $output = Join-Path $root ('target/d5-corporate-' + [Guid]::NewGuid().ToString('N'))
    New-Item -ItemType Directory -Path $output | Out-Null
    $dependencyFile = Join-Path $output 'dependencies.txt'
    Push-Location $root
    try {
        & $maven '-Pbuild' '-Dmaven.buildNumber.skip=true' '-DskipTests' 'compile' 'dependency:build-classpath' "-Dmdep.outputFile=$dependencyFile" '-DincludeScope=test'
        if ($LASTEXITCODE -ne 0) { throw 'D5 corporate Maven compile/classpath failed' }
    } finally {
        Pop-Location
    }
    if (!(Test-Path -LiteralPath $dependencyFile -PathType Leaf)) {
        throw 'D5 corporate Maven compile/classpath failed: dependency file is missing'
    }
    $dependencies = [IO.File]::ReadAllText($dependencyFile).Trim()
    if (!$dependencies) { throw 'D5 corporate Maven compile/classpath failed: dependency classpath is empty' }
    $classpath = (Join-Path $root 'target/classes') + [IO.Path]::PathSeparator + $dependencies
    $probeSource = Join-Path $PSScriptRoot 'd5/D5ExecuteQueryProbe.java'
    $compileArgs = Join-Path $output 'javac.args'
    $runArgs = Join-Path $output 'java.args'
    $runClasspath = $output + [IO.Path]::PathSeparator + $classpath
    # Кавычки в файлах аргументов сохраняют пробелы, а прямые слеши исключают экранирование путей.
    [IO.File]::WriteAllLines($compileArgs, @('-cp', ('"' + $classpath.Replace('\', '/') + '"'),
        '-d', ('"' + $output.Replace('\', '/') + '"'), ('"' + $probeSource.Replace('\', '/') + '"')))
    [IO.File]::WriteAllLines($runArgs, @('--enable-native-access=ALL-UNNAMED',
        '-cp', ('"' + $runClasspath.Replace('\', '/') + '"'), 'D5ExecuteQueryProbe'))
    & $javac "@$compileArgs"
    if ($LASTEXITCODE -ne 0) { throw 'D5 corporate Java probe compilation/type-check failed' }
    & $java "@$runArgs"
    if ($LASTEXITCODE -ne 0) { throw 'D5 corporate JavaFX runtime/assertion failed' }
    return
}

$java = (Get-Command java -ErrorAction Stop).Source
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
