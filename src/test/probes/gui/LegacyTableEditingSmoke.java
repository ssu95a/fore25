import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.event.Event;
import javafx.scene.Scene;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import ru.inversion.fx.form.controls.JInvTable;
import ru.inversion.fx.form.controls.JInvTableColumn;
import ru.inversion.fx.form.controls.renderer.JInvTextFieldTableCell;
import ru.inversion.fx.form.controls.skin.JInvTableViewSkin;

@SuppressWarnings("deprecation")
public final class LegacyTableEditingSmoke {
    private record Row(StringProperty name) {
        Row(String name) {
            this(new SimpleStringProperty(name));
        }
    }

    private final CompletableFuture<Void> verdict = new CompletableFuture<>();
    private final Row first = new Row("First");
    private final Row second = new Row("Second");
    private Stage stage;
    private JInvTable<Row> table;
    private JInvTableColumn<Row, String> name;
    private boolean started;
    private int firstChanges;
    private int secondChanges;

    public static void main(String[] args) {
        var smoke = new LegacyTableEditingSmoke();
        int exit = 1;
        try {
            require(Runtime.version().feature() == 25, "setup: Java 25 required");
            Platform.startup(() -> smoke.runChecked(smoke::show));
            smoke.verdict.get(30, TimeUnit.SECONDS);
            System.out.println("GUI-005 PASS");
            exit = 0;
        } catch (TimeoutException failure) {
            System.err.println("GUI-005 UNKNOWN: timed out waiting for Stage focus or FX queue. Use an interactive desktop.");
        } catch (Throwable failure) {
            System.err.println("GUI-005 failed; see the setup, focus, or editing diagnostic below.");
            failure.printStackTrace(System.err);
        } finally {
            Platform.exit();
        }
        System.exit(exit);
    }

    private void show() {
        Platform.setImplicitExit(false);
        Thread.currentThread().setUncaughtExceptionHandler((thread, failure) -> verdict.completeExceptionally(failure));
        String version = System.getProperty("javafx.version", "UNKNOWN");
        require(version.equals("25") || version.startsWith("25."), "setup: JavaFX 25 required: " + version);
        System.out.println("Java=" + Runtime.version() + " JavaFX=" + version);
        System.out.println("Legacy controls=" + JInvTable.class.getProtectionDomain().getCodeSource().getLocation());

        table = new JInvTable<>();
        name = new JInvTableColumn<>("Name");
        require(!name.isEditable(), "setup: Legacy column should start non-editable");
        table.setEditable(true);
        name.setEditable(true);
        name.setCellValueFactory(cell -> cell.getValue().name());
        name.setCellFactory(JInvTextFieldTableCell.forTableColumn());
        first.name().addListener((property, before, after) -> firstChanges++);
        second.name().addListener((property, before, after) -> secondChanges++);
        table.getColumns().add(name);
        table.getItems().setAll(first, second);
        table.getSelectionModel().setCellSelectionEnabled(true);

        var scene = new Scene(new StackPane(table), 420, 240);
        scene.getStylesheets().add(Objects.requireNonNull(getClass().getResource("/css/general.css"),
                "setup: production CSS missing").toExternalForm());
        stage = new Stage();
        stage.setTitle("GUI-005 Legacy table editing smoke");
        stage.setScene(scene);
        stage.focusedProperty().addListener((property, before, focused) -> {
            if (focused) later(() -> {
                if (stage.isFocused() && !started) {
                    started = true;
                    startEdit();
                }
            });
        });
        stage.show();
        stage.requestFocus();
    }

    private void startEdit() {
        stage.getScene().getRoot().applyCss();
        stage.getScene().getRoot().layout();
        require(stage.isShowing() && table.getSkin() instanceof JInvTableViewSkin<?>
                        && table.getWidth() > 0 && name.getWidth() > 0,
                "layout: visible Legacy table skin and column required");
        require(table.isEditable() && name.isEditable() && cellAt(0).isEditable() && cellAt(1).isEditable(),
                "setup: table, column and both realized Legacy cells must be editable");
        table.requestFocus();
        require(stage.isFocused() && table.isFocused(), "focus: table did not receive focus");
        table.getSelectionModel().clearAndSelect(0, name);
        table.edit(0, name);
        later(this::commitAndMove);
    }

    private void commitAndMove() {
        TextField editor = requireEditor(0);
        editor.setText("Updated first");
        Event.fireEvent(editor, new KeyEvent(KeyEvent.KEY_RELEASED, "", "", KeyCode.ENTER,
                false, false, false, false));
        requireModel();
        later(this::cancelSecond);
    }

    private void cancelSecond() {
        TextField editor = requireEditor(1);
        require(!cellAt(0).isEditing() && cellAt(0).getGraphic() == null,
                "navigation: first cell still has an editor after Enter");
        editor.setText("Discard this");
        table.edit(-1, null);
        later(() -> {
            require(table.getEditingCell() == null && !cellAt(1).isEditing() && cellAt(1).getGraphic() == null,
                    "cancel: second cell or table remains in editing state");
            requireSelected(1);
            requireModel();
            System.out.println("Legacy table edit, single-property commit, Enter navigation/focus and cancel passed");
            stage.close();
            verdict.complete(null);
        });
    }

    private TextField requireEditor(int row) {
        requireSelected(row);
        var position = table.getEditingCell();
        var cell = cellAt(row);
        require(position != null && position.getRow() == row && position.getTableColumn() == name
                        && cell.isEditing() && cell.getGraphic() instanceof TextField,
                "editing: Legacy editor not active at row " + row);
        var editor = (TextField) cell.getGraphic();
        require(stage.isFocused() && stage.getScene().getFocusOwner() == editor && editor.isFocused(),
                "focus: Legacy cell editor did not receive focus at row " + row);
        return editor;
    }

    private void requireSelected(int row) {
        var selection = table.getSelectionModel();
        require(selection.isSelected(row, name) && selection.getSelectedCells().size() == 1
                        && selection.getSelectedItem() == table.getItems().get(row),
                "selection: expected only row " + row + " in Name");
    }

    private void requireModel() {
        require("Updated first".equals(first.name().get()) && firstChanges == 1
                        && "Second".equals(second.name().get()) && secondChanges == 0,
                "model: expected one change to first row and zero to second; got "
                        + first.name().get() + "/" + firstChanges + ", " + second.name().get() + "/" + secondChanges);
    }

    private JInvTextFieldTableCell<?, ?> cellAt(int row) {
        return table.lookupAll(".table-cell").stream()
                .filter(node -> node instanceof JInvTextFieldTableCell<?, ?> cell
                        && cell.getTableColumn() == name && cell.getIndex() == row && !cell.isEmpty()
                        && cell.getWidth() > 0 && cell.getHeight() > 0)
                .map(node -> (JInvTextFieldTableCell<?, ?>) node)
                .findFirst()
                .orElseThrow(() -> new AssertionError("layout: no realized Legacy cell for row " + row));
    }

    private void later(Runnable action) {
        Platform.runLater(() -> runChecked(action));
    }

    private void runChecked(Runnable action) {
        if (verdict.isDone()) return;
        try {
            action.run();
        } catch (Throwable failure) {
            verdict.completeExceptionally(failure);
        }
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
