param([switch]$ExpectLeak, [string]$MavenRepository = 'D:/Java/.m2/repository')
$ErrorActionPreference = 'Stop'
$root = [IO.Path]::GetFullPath("$PSScriptRoot/../../..")
$fx = Get-Content "$root/src/main/java/ru/inversion/dataset/fx/DSFXAdapter.java" -Raw -Encoding UTF8
$base = Get-Content "$root/src/main/java/ru/inversion/dataset/fx/DSControlAdapter.java" -Raw -Encoding UTF8
if ($fx -notmatch '@Override\s+@Deprecated\s+public void setDataSet\( IDataSet<T> ds \)') {
    throw 'Public setter signature/annotations changed'
}
Write-Output 'PASS public void setDataSet(IDataSet<T>) with Override/Deprecated'
function Method([string]$source, [string]$signature) {
    $start = $source.IndexOf($signature)
    if ($start -lt 0) { throw "Missing method $signature" }
    $brace = $source.IndexOf('{', $start)
    $depth = 1; $end = $brace + 1
    while ($depth -gt 0) {
        if ($source[$end] -eq '{') { $depth++ }
        if ($source[$end] -eq '}') { $depth-- }
        $end++
    }
    return $source.Substring($start, $end - $start)
}
$close = if ($ExpectLeak) { '' } else { Method $fx 'public void close(' }
$setter = if ($ExpectLeak) { '' } else { Method $fx 'public void setDataSet(' }
$start = $fx.IndexOf('        this.dataSet = dataSet;')
$end = $fx.IndexOf('        this.tableView = tableView;', $start)
$bind = $fx.Substring($start, $end - $start).Replace('        updateEmptyProperty();', '')
$mark = [regex]::Match($fx, 'xxiDs\.addMarkDataSetListener\(this\);').Value
if (!$mark) { throw 'Missing mark registration' }
# Исполняются извлечённые lifecycle-блоки без UI; события доставляет настоящий JInvCommon.
$template = @'
import java.util.*;
import ru.inversion.dataset.*;
class Parent<T> implements IDataSetRowListener<T>, IDataSetNavigationListener<T> {
    protected IDataSet<T> dataSet;
    public void rowOperation(DataSetRowEvent<T> e) {}
    public void navigated(DataSetNavigationEvent<T> e) {}
    public void setDataSet(IDataSet<T> ds) {}
__BASE_CLOSE__
}
public class D4LifecycleProbe<T> extends Parent<T> implements IDataSetListener, IDataSetMarkListener<T> {
    int data, row, nav, mark;
    public void dataSetChanged(DataSetEvent e) { data++; }
    public void rowOperation(DataSetRowEvent<T> e) { row++; }
    public void navigated(DataSetNavigationEvent<T> e) { nav++; }
    public void markAction(DataSetMarkEvent<T> e) { mark++; }
    void bind(IDataSet<T> dataSet) {
__BIND__
    }
    void bindMark(XXIDataSet<T> xxiDs) { __MARK__ }
__CLOSE__
__SETTER__
    static class Data extends ArrayDataSet<String> {
        Data() { super(String.class, new ArrayList<>(), false); }
        void fire() {
            fireDataSetEvent(DataSetEvent.DataSetEventType.EXECUTE, false);
            fireNavigationEvent(null, null, 0, 1);
            fireRowEvent(DataSetRowEvent.RowOperationEnum.UPDATE, null, null, 0, 0);
        }
    }
    static class MarkData extends XXIDataSet<String> {
        @Override public boolean isSupportMark() { return true; }
        void fire() { fireMarkDataSetEvent(DataSetMarkEvent.MarkActionEnum.REFRESH); }
    }
    static void check(String name, boolean ok) {
        if (!ok) throw new AssertionError(name);
        System.out.println("PASS " + name);
    }
    public static void main(String[] args) throws Exception {
        boolean leak = Boolean.parseBoolean(args[0]);
        var a = new D4LifecycleProbe<String>();
        var ds = new Data();
        if (!leak) reject(a, ds);
        a.bind(ds); ds.fire();
        check("all three callbacks before close", a.data == 1 && a.row == 1 && a.nav == 1);
        if (!leak) { reject(a, ds); reject(a, new Data()); reject(a, null); }
        a.close(); ds.fire();
        check("Row/Navigation detached", a.row == 1 && a.nav == 1);
        check(leak ? "REPRODUCED DataSet callback after close" : "DataSet detached", a.data == (leak ? 2 : 1));
        check("dataSet cleared", a.dataSet == null);
        a.close();
        if (!leak) reject(a, ds);
        var m = new D4LifecycleProbe<String>();
        var md = new MarkData();
        m.bind(md); m.bindMark(md); md.fire();
        check("mark delivered before close", m.mark == 1);
        m.close(); md.fire();
        check(leak ? "REPRODUCED mark callback after close" : "mark detached", m.mark == (leak ? 2 : 1));
        m.close();
    }
    static void reject(D4LifecycleProbe<String> a, IDataSet<String> ds) {
        var before = a.dataSet;
        try { a.setDataSet(ds); throw new AssertionError("setter returned"); }
        catch (UnsupportedOperationException e) {
            check("setter rejects without rebinding", a.dataSet == before && e.getMessage().equals("Changing Dataset for a bound TableView adapter is unsupported"));
        }
    }
}
'@
$out = "$root/target/d4-lifecycle-probe".Replace('\','/')
New-Item -ItemType Directory -Force $out | Out-Null
$generated = $template.Replace('__BASE_CLOSE__', (Method $base 'public void close(')).Replace('__CLOSE__', $close).Replace('__SETTER__', $setter).Replace('__BIND__', $bind).Replace('__MARK__', $mark)
[IO.File]::WriteAllText("$out/D4LifecycleProbe.java", $generated, [Text.UTF8Encoding]::new($false))
$jars = Get-ChildItem $MavenRepository -Recurse -Filter *.jar | Select-Object -ExpandProperty FullName
$cp = $jars -join ';'
$compile = '-encoding UTF-8 -proc:none -cp "' + $cp.Replace('\','/') + '" -d "' + $out + '" "' + $out + '/D4LifecycleProbe.java"'
[IO.File]::WriteAllText("$out/compile.args", $compile)
& javac "@$out/compile.args"
if ($LASTEXITCODE) { throw 'Compile failed' }
$run = '-cp "' + $out + ';' + $cp.Replace('\','/') + '" D4LifecycleProbe ' + $ExpectLeak.IsPresent.ToString().ToLowerInvariant()
[IO.File]::WriteAllText("$out/run.args", $run)
& java "@$out/run.args"
if ($LASTEXITCODE) { throw 'Probe failed' }
