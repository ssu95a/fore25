param(
    [Parameter(Mandatory = $true)][string[]] $JavafxJars,
    [Parameter(Mandatory = $true)][string] $CommonJar,
    [string] $SourcePath = "$PSScriptRoot/../../main/java/ru/inversion/dataset/fx/DSFXAdapter.java"
)

$ErrorActionPreference = 'Stop'
$source = Get-Content -LiteralPath $SourcePath -Raw -Encoding UTF8
$startMarker = '        this.tableView.addEventFilter ( MouseEvent.DRAG_DETECTED, ( MouseEvent event ) -> refreshCurrentRow() );'
$endMarker = '        final SwappingScrollBinding scrollBinding = new SwappingScrollBinding();'
$start = $source.IndexOf($startMarker)
$end = $source.IndexOf($endMarker, [Math]::Max(0, $start))
if ($start -lt 0 -or $end -lt 0) { throw 'Cannot locate column tracking initialization' }
$tracking = $source.Substring($start + $startMarker.Length, $end - $start - $startMarker.Length)
$lookup = ''
$fields = 'return columnFieldsSet;'
if ($source -notmatch 'final private Set<String> columnFieldsSet') {
    $lookupStart = $source.IndexOf('    private boolean checkColumnWithFieldNameExists(')
    $lookupEnd = $source.IndexOf('    @Deprecated', $lookupStart)
    $lookup = $source.Substring($lookupStart, $lookupEnd - $lookupStart)
    if ($source -notmatch '!checkColumnWithFieldNameExists\(pd.getColumnName\(\)\)') {
        throw 'Duplicate protection does not use the tested lookup'
    }
    $fields = @'
        Set<String> result = new HashSet<>();
        for (String name : Arrays.asList(null, "", "INITIAL", "ADDED", "REPLACED", "A", "B", "GROUP"))
            if (checkColumnWithFieldNameExists(name)) result.add(name);
        return result;
'@
}
$controls = Get-Content -LiteralPath "$PSScriptRoot/../../main/java/ru/inversion/fx/form/controls/Controls.java" -Raw -Encoding UTF8
$helperStart = $controls.IndexOf('    private static <T> void helperGetColumns(')
$helperEnd = $controls.IndexOf('    public static <T> List<TableColumn<T, ?>> getAllColumnsFromTable(', $helperStart)
$helpers = $controls.Substring($helperStart, $helperEnd - $helperStart).Replace('TableView<T> table', 'Columns<T> table')

# Компилируются исходные блоки адаптера и обхода дерева Controls.
# JavaFX и корпоративные классы берутся из переданных jar без подмены.
# Вместо TableView используется контейнер списка: проверка не создаёт окно TableView.
$template = @'
import java.util.*;
import javafx.collections.*;
import javafx.scene.control.TableColumn;
import ru.inversion.utils.S;

public class ColumnFieldsProbe<T> {
    static final String COLUMN_FIELD_NAME = "ru.inversion.field_name";
    final Set<String> columnFieldsSet = new HashSet<>();
    final Columns<T> tableView = new Columns<>();
    static class Columns<T> {
        final ObservableList<TableColumn<T, ?>> columns = FXCollections.observableArrayList();
        ObservableList<TableColumn<T, ?>> getColumns() { return columns; }
    }
    void initialize() {
__TRACKING__
    }
__LOOKUP__
    Set<String> fields() {
__FIELDS__
    }
    static class Controls {
__HELPERS__
    }
    static TableColumn<Object, Object> column(String name) {
        TableColumn<Object, Object> column = new TableColumn<>();
        column.getProperties().put(COLUMN_FIELD_NAME, name);
        return column;
    }
    static int failures;
    static void check(String name, boolean condition) {
        System.out.println((condition ? "PASS " : "FAIL ") + name);
        if (!condition) failures++;
    }
    public static void main(String[] args) {
        ColumnFieldsProbe<Object> p = new ColumnFieldsProbe<>();
        TableColumn<Object, Object> initial = column("INITIAL");
        p.tableView.columns.addAll(List.of(initial, column(null), column("")));
        p.initialize();
        check("initial non-empty field blocks duplicate", p.columnFieldsSet.contains("INITIAL"));
        check("initial null ignored", !p.columnFieldsSet.contains(null));
        check("initial empty ignored", !p.columnFieldsSet.contains(""));
        TableColumn<Object, Object> added = column("ADDED");
        p.tableView.columns.add(added);
        check("added non-empty field blocks duplicate", p.columnFieldsSet.contains("ADDED"));
        TableColumn<Object, Object> nullColumn = column(null);
        TableColumn<Object, Object> emptyColumn = column("");
        p.tableView.columns.addAll(List.of(nullColumn, emptyColumn));
        check("added null ignored", !p.columnFieldsSet.contains(null));
        check("added empty ignored", !p.columnFieldsSet.contains(""));
        p.tableView.columns.removeAll(List.of(nullColumn, emptyColumn));
        check("removing unnamed columns preserves named fields",
            p.columnFieldsSet.equals(Set.of("INITIAL", "ADDED")));
        p.tableView.columns.remove(initial);
        check("removed initial field allows regeneration", !p.columnFieldsSet.contains("INITIAL"));
        p.tableView.columns.remove(added);
        check("removed added field allows regeneration", !p.columnFieldsSet.contains("ADDED"));
        p.tableView.columns.add(added);
        check("re-added field tracked", p.columnFieldsSet.contains("ADDED"));
        TableColumn<Object, Object> duplicate = column("ADDED");
        p.tableView.columns.add(duplicate);
        p.tableView.columns.remove(added);
        check("removing one duplicate retains field", p.columnFieldsSet.contains("ADDED"));
        p.tableView.columns.remove(duplicate);
        check("removing last duplicate clears field", !p.columnFieldsSet.contains("ADDED"));
        TableColumn<Object, Object> replacement = column("REPLACED");
        p.tableView.columns.setAll(List.of(replacement));
        check("replacement tracked", p.columnFieldsSet.equals(Set.of("REPLACED")));
        p.tableView.columns.setAll(List.of(column("REPLACED")));
        check("replacement with same field retained", p.columnFieldsSet.equals(Set.of("REPLACED")));
        p.tableView.columns.clear();
        check("clear removes all fields", p.columnFieldsSet.isEmpty());

        ColumnFieldsProbe<Object> n = new ColumnFieldsProbe<>();
        TableColumn<Object, Object> group = column("GROUP");
        TableColumn<Object, Object> leafA = column("A");
        group.getColumns().add(leafA);
        n.tableView.columns.add(group);
        n.initialize();
        check("initial nested A blocks duplicate", n.fields().contains("A"));
        check("non-leaf group field ignored", !n.fields().contains("GROUP"));
        n.tableView.columns.clear();
        check("removed initial group clears nested A", !n.fields().contains("A"));
        n.tableView.columns.add(group);
        check("added group tracks nested A", n.fields().contains("A"));
        n.tableView.columns.remove(group);
        check("removed added group clears nested A", !n.fields().contains("A"));
        TableColumn<Object, Object> topA = column("A");
        n.tableView.columns.addAll(List.of(topA, group));
        n.tableView.columns.remove(topA);
        check("remove top-level A retains nested A", n.fields().contains("A"));
        n.tableView.columns.add(topA);
        n.tableView.columns.remove(group);
        check("remove nested A retains top-level A", n.fields().contains("A"));
        n.tableView.columns.setAll(List.of(group));
        TableColumn<Object, Object> secondA = column("A");
        group.getColumns().add(secondA);
        group.getColumns().remove(leafA);
        check("remove first nested duplicate retains A", n.fields().contains("A"));
        group.getColumns().remove(secondA);
        check("remove last nested duplicate clears A", !n.fields().contains("A"));
        int[] topChanges = {0};
        n.tableView.columns.addListener((ListChangeListener<TableColumn<Object, ?>>) c -> topChanges[0]++);
        group.getColumns().add(leafA);
        check("direct nested add tracks A", n.fields().contains("A"));
        group.getColumns().remove(leafA);
        check("direct nested remove clears A", !n.fields().contains("A"));
        check("top-level listener receives no direct nested changes", topChanges[0] == 0);
        group.getColumns().add(leafA);
        TableColumn<Object, Object> replacementGroup = column(null);
        TableColumn<Object, Object> innerGroup = column(null);
        innerGroup.getColumns().add(column("B"));
        replacementGroup.getColumns().add(innerGroup);
        n.tableView.columns.setAll(List.of(replacementGroup));
        check("group replacement removes A and tracks deep B", n.fields().equals(Set.of("B")));
        innerGroup.getColumns().setAll(List.of(column("A")));
        check("direct subtree replacement removes B and tracks A", n.fields().equals(Set.of("A")));
        innerGroup.getColumns().setAll(List.of(column(null), column("")));
        check("nested null and empty ignored", n.fields().isEmpty());
        n.tableView.columns.clear();
        check("nested clear leaves no tracked fields", n.fields().isEmpty());
        if (failures != 0) throw new AssertionError(failures + " failed checks");
    }
}
'@

$probeDir = Join-Path ([IO.Path]::GetTempPath()) ('fore25-column-fields-' + [Guid]::NewGuid().ToString('N'))
New-Item -ItemType Directory -Path $probeDir | Out-Null
$probeFile = Join-Path $probeDir 'ColumnFieldsProbe.java'
$generated = $template.Replace('p.columnFieldsSet', 'p.fields()').Replace('__TRACKING__', $tracking).Replace('__LOOKUP__', $lookup).Replace('__FIELDS__', $fields).Replace('__HELPERS__', $helpers)
[IO.File]::WriteAllText($probeFile, $generated, [Text.UTF8Encoding]::new($false))
$classpath = (@($JavafxJars) + @($CommonJar)) -join [IO.Path]::PathSeparator
& javac -encoding UTF-8 -cp $classpath -d $probeDir $probeFile
if ($LASTEXITCODE -ne 0) { throw 'Probe compilation failed' }
& java "-Duser.home=$probeDir" "-Djavafx.cachedir=$probeDir" '-Dprism.order=sw' -cp ($probeDir + [IO.Path]::PathSeparator + $classpath) ColumnFieldsProbe
if ($LASTEXITCODE -ne 0) { throw 'Column field regression probe failed' }
