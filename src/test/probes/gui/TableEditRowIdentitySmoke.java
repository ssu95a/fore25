import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.event.Event;
import javafx.scene.Scene;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.PickResult;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import ru.inversion.dataset.ArrayDataSet;
import ru.inversion.dataset.fx.DSFXAdapter;
import ru.inversion.fx.form.controls.JInvTable;
import ru.inversion.fx.form.controls.JInvTableColumn;
import ru.inversion.fx.form.controls.renderer.JInvTextFieldTableCell;

public final class TableEditRowIdentitySmoke {
    private static final class Row {
        final String id;
        final int sortKey;
        final SimpleStringProperty value = new SimpleStringProperty("seed");

        Row(String id, int sortKey) {
            this.id = id;
            this.sortKey = sortKey;
        }

        @Override public boolean equals(Object other) {
            return other instanceof Row row && sortKey == row.sortKey;
        }

        @Override public int hashCode() { return sortKey; }
        @Override public String toString() { return id; }
    }

    private enum Action { REPLACE, REMOVE, PERMUTE, CANCEL, ESCAPE, F8, ENTER }
    private final List<String> failures = new ArrayList<>();

    private void exercise(Action action, boolean arm) throws Exception {
        var a = new Row("A", 1);
        var b = new Row("B", 1);
        var c = new Row("C", 2);
        var x = new Row("X", 1);
        if (a == x || !a.equals(x) || a.sortKey != b.sortKey) throw new AssertionError("fixture identity");
        var items = FXCollections.observableArrayList(a, b, c);
        var rows = new ArrayDataSet<>(Row.class, items, false);
        var table = new JInvTable<Row>();
        var column = new JInvTableColumn<Row, String>("Value");
        column.setCellFactory(JInvTextFieldTableCell.forTableColumn());
        column.setCellValueFactory(cell -> cell.getValue().value);
        var commits = new ArrayList<Row>();
        var writes = new ArrayList<Row>();
        column.addEventHandler(TableColumn.<Row, String>editCommitEvent(), event -> commits.add(event.getRowValue()));
        for (Row row : List.of(a, b, c, x)) {
            row.value.addListener((p, before, after) -> writes.add(row));
        }
        table.getColumns().add(column);
        var adapter = new DSFXAdapter<Row>();
        adapter.bindTable(rows, table, false);
        table.setEditable(true);
        column.setEditable(true);
        var stage = new Stage();
        stage.setScene(new Scene(new StackPane(table), 360, 240));
        stage.show();
        try {
            table.applyCss();
            table.layout();
            table.getSelectionModel().select(0);
            edit(table, column, "draft");
            var editor = (TextField) table.lookup(".text-field");
            if (arm) {
                Event.fireEvent(table, new MouseEvent(MouseEvent.MOUSE_PRESSED, 1, 1, 1, 1,
                        MouseButton.PRIMARY, 1, false, false, false, false, true, false,
                        false, false, false, true, new PickResult(table, 1, 1)));
            }
            switch (action) {
                case REPLACE -> items.set(0, x);
                case REMOVE -> items.remove(0);
                case PERMUTE -> FXCollections.sort(items, (left, right) -> Integer.compare(right.sortKey, left.sortKey));
                case CANCEL -> table.edit(-1, null);
                case ESCAPE -> {
                    key(editor, KeyEvent.KEY_PRESSED, KeyCode.ESCAPE);
                    table.edit(-1, null);
                }
                case F8 -> key(editor, KeyEvent.KEY_RELEASED, KeyCode.F8);
                case ENTER -> key(editor, KeyEvent.KEY_RELEASED, KeyCode.ENTER);
            }
            boolean accept = action == Action.F8 || action == Action.ENTER || action == Action.CANCEL && arm;
            check((accept ? singleTarget(commits, a) && singleTarget(writes, a)
                    : commits.isEmpty() && writes.isEmpty()), action + "/" + arm, commits, writes);
            System.out.printf("%s armed=%s commits=%s writes=%s cursor=%d%n", action, arm, commits, writes, rows.getCurrentRowNum());

            if (action == Action.REPLACE && arm) {
                commits.clear();
                writes.clear();
                edit(table, column, "next draft");
                table.edit(-1, null);
                check(commits.isEmpty() && writes.isEmpty(), "reentry unarmed cancel", commits, writes);
                edit(table, column, "next accepted");
                key((TextField) table.lookup(".text-field"), KeyEvent.KEY_RELEASED, KeyCode.F8);
                check(singleTarget(commits, x) && singleTarget(writes, x), "reentry F8", commits, writes);
            }
        } finally {
            adapter.close();
            stage.hide();
        }
    }

    private static void edit(JInvTable<Row> table, JInvTableColumn<Row, String> column, String draft) {
        table.edit(0, column);
        table.layout();
        var editor = (TextField) table.lookup(".text-field");
        if (editor == null || table.getEditingCell() == null) throw new AssertionError("editor inactive");
        editor.setText(draft);
    }

    private static void key(TextField editor, javafx.event.EventType<KeyEvent> type, KeyCode code) {
        Event.fireEvent(editor, new KeyEvent(type, "", "", code, false, false, false, false));
    }

    private static boolean singleTarget(List<Row> rows, Row target) {
        return rows.size() == 1 && rows.getFirst() == target;
    }

    private void check(boolean ok, String scenario, List<Row> commits, List<Row> writes) {
        if (!ok) failures.add(scenario + " commits=" + commits + " writes=" + writes);
    }

    public static void main(String[] args) throws Exception {
        var task = new FutureTask<Void>(() -> {
            var smoke = new TableEditRowIdentitySmoke();
            for (Action action : List.of(Action.REPLACE, Action.REMOVE, Action.PERMUTE)) {
                smoke.exercise(action, false);
                smoke.exercise(action, true);
            }
            smoke.exercise(Action.CANCEL, true);
            smoke.exercise(Action.ESCAPE, true);
            smoke.exercise(Action.F8, false);
            smoke.exercise(Action.ENTER, false);
            if (!smoke.failures.isEmpty()) throw new AssertionError(smoke.failures);
            return null;
        });
        Platform.startup(() -> Platform.runLater(task));
        try {
            task.get(30, TimeUnit.SECONDS);
            System.out.println("CTRL-075 PASS");
        } finally {
            Platform.exit();
        }
    }
}
