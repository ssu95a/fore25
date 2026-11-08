param(
    [Parameter(Mandatory = $true)][string[]] $JavafxJars,
    [Parameter(Mandatory = $true)][string] $CommonJar,
    [string[]] $AdditionalJars = @(),
    [string] $SourcePath = "$PSScriptRoot/../../main/java/ru/inversion/dataset/fx/DSFXAdapter.java"
)

$ErrorActionPreference = 'Stop'
$source = Get-Content -LiteralPath $SourcePath -Raw -Encoding UTF8
function Get-SourceBetween([string] $startMarker, [string] $endMarker) {
    $start = $source.IndexOf($startMarker)
    if ($start -lt 0) { throw "Missing source marker: $startMarker" }
    $end = $source.IndexOf($endMarker, $start + $startMarker.Length)
    if ($end -lt 0) { throw "Missing source marker: $endMarker" }
    return $source.Substring($start, $end - $start)
}

# Проверяются исходные блоки property, подписки bindTable и начала обработчиков событий.
# UI-действия обработчиков не исполняются; Dataset и события берутся из настоящего JInvCommon.
$property = Get-SourceBetween '    private ReadOnlyBooleanWrapper emptyProperty;' '    public BooleanProperty enableMarkProperty()'
$binding = Get-SourceBetween '        this.dataSet = dataSet;' '        this.tableView = tableView;'
$row = Get-SourceBetween '    public void rowOperation( DataSetRowEvent<T> event ) {' '        if( event.getRowOperation() == UPDATE )'
$data = Get-SourceBetween '    public void dataSetChanged( DataSetEvent e ) {' '        if( e.isAfter() && e.getEventType() == DataSetEvent.DataSetEventType.EXECUTE )'
$template = @'
import java.util.*;
import javafx.beans.property.*;
import ru.inversion.dataset.*;

public class DSFXAdapter<T> implements IDataSetRowListener<T>, IDataSetListener, IDataSetNavigationListener<T> {
    private IDataSet<T> dataSet;
    public IDataSet<T> getDataSet() { return dataSet; }
    void bind(IDataSet<T> dataSet) {
__BINDING__
    }
__PROPERTY__
__ROW__
    }
__DATA__
    }
    public void navigated(DataSetNavigationEvent<T> e) { }

    static class Data extends ArrayDataSet<String> {
        int rowRegistrations, datasetRegistrations;
        Data(int count) {
            super(String.class, new ArrayList<>(), false);
            for (int i = 0; i < count; i++) rows().add("row" + i);
            if (count != 0) setCurrentRowNum(0);
        }
        @Override public void addRowListener(IDataSetRowListener<String> listener) {
            rowRegistrations++;
            super.addRowListener(listener);
        }
        @Override public void addDataSetListener(IDataSetListener listener) {
            datasetRegistrations++;
            super.addDataSetListener(listener);
        }
        void replaceRows(int count) {
            rows().clear();
            for (int i = 0; i < count; i++) rows().add("result" + i);
        }
        void event(DataSetEvent.DataSetEventType type, boolean before) {
            fireDataSetEvent(type, before);
        }
    }
    static int checks, failures;
    static void check(String name, boolean result) {
        checks++;
        if (!result) failures++;
        System.out.println((result ? "PASS " : "FAIL ") + name);
    }
    static void scenario(boolean beforeBinding, int initialCount) {
        String label = (beforeBinding ? "before-bind" : "after-bind") + "/" + initialCount + ": ";
        DSFXAdapter<String> adapter = new DSFXAdapter<>();
        ReadOnlyBooleanProperty property = null;
        if (beforeBinding) {
            property = adapter.emptyProperty();
            check(label + "no Dataset means empty", property.get());
        }
        Data ds = new Data(initialCount);
        adapter.bind(ds);
        if (property == null) property = adapter.emptyProperty();
        check(label + "initial state", property.get() == ds.isEmpty());
        check(label + "bean and name", property.getBean() == adapter && property.getName().equals("emptyProperty"));
        int rowCount = ds.rowRegistrations, datasetCount = ds.datasetRegistrations;
        boolean sameProperty = true;
        for (int i = 0; i < 10; i++)
            sameProperty &= adapter.emptyProperty() == property;
        check(label + "repeated reads return same property", sameProperty);
        check(label + "repeated reads do not add listeners",
            ds.rowRegistrations == rowCount && ds.datasetRegistrations == datasetCount);
        check(label + "only adapter listeners", ds.rowRegistrations == 1 && ds.datasetRegistrations == 1);

        ds.clear();
        check(label + "clear leaves empty", property.get());
        ds.insertRow("first", IDataSet.InsertRowModeEnum.FIRST, true);
        check(label + "insert first row", !property.get());
        ds.insertRow("second", IDataSet.InsertRowModeEnum.LAST, true);
        ds.updateCurrentRow("updated");
        check(label + "update", !property.get());
        ds.removeCurrentRow();
        check(label + "delete not-last row", !property.get() && ds.getLoadedRowCount() == 1);
        ds.removeCurrentRow();
        check(label + "delete last row", property.get() && ds.isEmpty());

        for (DataSetEvent.DataSetEventType type : DataSetEvent.DataSetEventType.values()) {
            ds.replaceRows(2);
            ds.event(type, true);
            check(label + type + " BEFORE ignored", property.get());
            ds.event(type, false);
            check(label + type + " AFTER non-empty", !property.get());
            ds.replaceRows(0);
            ds.event(type, true);
            check(label + type + " BEFORE ignored on empty result", !property.get());
            ds.event(type, false);
            check(label + type + " AFTER empty", property.get());
        }
    }
    public static void main(String[] args) {
        scenario(false, 0);
        scenario(false, 2);
        scenario(true, 0);
        scenario(true, 2);
        System.out.println("CHECKS=" + checks + " FAILURES=" + failures);
        if (failures != 0) throw new AssertionError(failures + " failed checks");
    }
}
'@
$generated = $template.Replace('__BINDING__', $binding).Replace('__PROPERTY__', $property).Replace('__ROW__', $row).Replace('__DATA__', $data)
$probeDir = Join-Path ([IO.Path]::GetTempPath()) ('fore25-empty-property-' + [Guid]::NewGuid().ToString('N'))
New-Item -ItemType Directory -Path $probeDir | Out-Null
$probeFile = Join-Path $probeDir 'DSFXAdapter.java'
[IO.File]::WriteAllText($probeFile, $generated, [Text.UTF8Encoding]::new($false))
$classpath = (@($JavafxJars) + @($CommonJar) + @($AdditionalJars)) -join [IO.Path]::PathSeparator
& javac -encoding UTF-8 -cp $classpath -d $probeDir $probeFile
if ($LASTEXITCODE -ne 0) { throw 'Probe compilation failed' }
& java -cp ($probeDir + [IO.Path]::PathSeparator + $classpath) DSFXAdapter
if ($LASTEXITCODE -ne 0) { throw 'Empty property regression probe failed' }
