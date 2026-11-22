import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
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
import ru.inversion.fx.form.controls.JInvTable;
import ru.inversion.fx.form.controls.JInvTableColumn;
import ru.inversion.fx.form.controls.renderer.JInvTextFieldTableCell;
import ru.inversion.fx.form.controls.skin.JInvTableViewSkin;

@SuppressWarnings("deprecation")
public final class LegacyTableCancelSmoke {
    private enum Scenario { PROGRAMMATIC, MOUSE_THEN_ESCAPE, MOUSE_THEN_PROGRAMMATIC }

    private record Row(StringProperty value) {}
    private record Change(Row row, String before, String after) {}
    private record Commit(int row, TableColumn<Row, String> column, String value) {}

    private final CompletableFuture<Boolean> verdict = new CompletableFuture<>();
    private final List<Change> changes = new ArrayList<>();
    private final List<Commit> commits = new ArrayList<>();
    private final Row edited = new Row(new SimpleStringProperty("Original"));
    private final Row other = new Row(new SimpleStringProperty("Other"));
    private Stage stage;
    private JInvTable<Row> table;
    private JInvTableColumn<Row, String> column;
    private boolean started;

    public static void main(String[] args) {
        var smoke = new LegacyTableCancelSmoke();
        int exit = 1;
        try {
            require(Runtime.version().feature() == 25, "setup: Java 25 required");
            Platform.startup(() -> smoke.checked(smoke::show));
            boolean mousePathObserved = smoke.verdict.get(30, TimeUnit.SECONDS);
            System.out.println(mousePathObserved ? "GUI-006 PASS" : "GUI-006 UNKNOWN: mouse-capture path unconfirmed");
            exit = mousePathObserved ? 0 : 2;
        } catch (TimeoutException failure) {
            System.err.println("GUI-006 UNKNOWN: timed out waiting for Stage focus or FX queue. Use an interactive desktop.");
        } catch (Throwable failure) {
            System.err.println("GUI-006 failed; see setup, focus or scenario diagnostics.");
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
        table.setEditable(true);
        column = new JInvTableColumn<>("Value");
        column.setEditable(true);
        column.setCellValueFactory(cell -> cell.getValue().value());
        column.setCellFactory(JInvTextFieldTableCell.forTableColumn());
        column.addEventHandler(TableColumn.<Row, String>editCommitEvent(), event ->
                commits.add(new Commit(event.getTablePosition().getRow(), event.getTableColumn(), event.getNewValue())));
        for (Row row : List.of(edited, other)) {
            row.value().addListener((property, before, after) -> changes.add(new Change(row, before, after)));
        }
        table.getColumns().add(column);
        table.getItems().setAll(edited, other);
        table.getSelectionModel().setCellSelectionEnabled(true);

        var scene = new Scene(new StackPane(table), 420, 240);
        scene.getStylesheets().add(Objects.requireNonNull(getClass().getResource("/css/general.css"),
                "setup: production CSS missing").toExternalForm());
        stage = new Stage();
        stage.setTitle("GUI-006 Legacy table cancellation smoke");
        stage.setScene(scene);
        stage.focusedProperty().addListener((property, before, focused) -> {
            if (focused) later(() -> {
                if (stage.isFocused() && !started) {
                    started = true;
                    scene.getRoot().applyCss();
                    scene.getRoot().layout();
                    require(stage.isShowing() && table.getSkin() instanceof JInvTableViewSkin<?>,
                            "setup: visible Legacy table skin required");
                    start(Scenario.PROGRAMMATIC);
                }
            });
        });
        stage.show();
        stage.requestFocus();
    }

    private void start(Scenario scenario) {
        table.requestFocus();
        table.getSelectionModel().clearAndSelect(0, column);
        table.edit(0, column);
        later(() -> exercise(scenario));
    }

    private void exercise(Scenario scenario) {
        var cell = table.lookupAll(".table-cell").stream()
                .filter(node -> node instanceof JInvTextFieldTableCell<?, ?> candidate
                        && candidate.getIndex() == 0 && candidate.getTableColumn() == column
                        && !candidate.isEmpty() && candidate.getWidth() > 0 && candidate.getHeight() > 0)
                .map(node -> (JInvTextFieldTableCell<?, ?>) node)
                .findFirst()
                .orElseThrow(() -> new AssertionError("layout: no realized Legacy cell at row 0"));
        require(cell.isEditing() && cell.getGraphic() instanceof TextField, scenario + ": editor did not open");
        var editor = (TextField) cell.getGraphic();
        require(stage.isFocused() && editor.isFocused(), scenario + ": editor did not receive focus");

        String draft = "Draft " + scenario;
        System.out.println(scenario + " BEFORE " + snapshot(editor));
        editor.setText(draft);
        if (scenario != Scenario.PROGRAMMATIC) {
            var pick = new PickResult(editor, 4, 4);
            Event.fireEvent(editor, new MouseEvent(MouseEvent.MOUSE_PRESSED, 4, 4, 4, 4,
                    MouseButton.PRIMARY, 1, false, false, false, false, true, false, false,
                    true, false, true, pick));
            require(cell.isEditing() && editor.isFocused() && commits.isEmpty() && changes.isEmpty(),
                    scenario + ": press within editor changed state before cancellation");
        }
        require(draft.equals(editor.getText()), scenario + ": editor draft was replaced before cancellation");
        System.out.println(scenario + " ACTION " + snapshot(editor));
        if (scenario == Scenario.MOUSE_THEN_ESCAPE) {
            Event.fireEvent(editor, new KeyEvent(KeyEvent.KEY_PRESSED, "", "", KeyCode.ESCAPE,
                    false, false, false, false));
        } else {
            table.edit(-1, null);
        }
        later(() -> finish(scenario, cell, editor, draft));
    }

    private void finish(Scenario scenario, JInvTextFieldTableCell<?, ?> cell, TextField editor, String draft) {
        System.out.println(scenario + " AFTER " + snapshot(editor));
        require(table.getEditingCell() == null && !cell.isEditing() && cell.getGraphic() == null,
                scenario + ": cancellation did not clear the active editor");
        require("Other".equals(other.value().get()) && changes.stream().noneMatch(change -> change.row() == other),
                scenario + ": changed the other row");
        require(commits.size() <= 1 && changes.size() <= 1, scenario + ": duplicate commits or changes");
        if (scenario == Scenario.MOUSE_THEN_PROGRAMMATIC && !commits.isEmpty()) {
            require(commits.equals(List.of(new Commit(0, column, draft)))
                            && changes.equals(List.of(new Change(edited, "Original", draft)))
                            && draft.equals(edited.value().get()),
                    scenario + ": commit event and edited property disagree");
            System.out.println("OBSERVED: routed mouse press armed Legacy cancel-time commit of the draft");
        } else {
            require(commits.isEmpty() && changes.isEmpty() && "Original".equals(edited.value().get()),
                    scenario + ": cancellation mutated the model or fired a commit");
            if (scenario == Scenario.MOUSE_THEN_PROGRAMMATIC) {
                System.out.println("UNKNOWN: no cancel-time commit observed; mouse-capture branch coverage unconfirmed");
            }
        }

        switch (scenario) {
            case PROGRAMMATIC -> start(Scenario.MOUSE_THEN_ESCAPE);
            case MOUSE_THEN_ESCAPE -> start(Scenario.MOUSE_THEN_PROGRAMMATIC);
            case MOUSE_THEN_PROGRAMMATIC -> {
                stage.close();
                verdict.complete(!commits.isEmpty());
            }
        }
    }

    private String snapshot(TextField editor) {
        var selection = table.getSelectionModel().getSelectedCells().stream()
                .map(position -> position.getRow() + "/" + position.getColumn()).toList();
        var owner = stage.getScene().getFocusOwner();
        String focus = owner == editor ? "editor" : owner == table ? "table"
                : owner == null ? "none" : owner.getClass().getSimpleName();
        return "draft=[" + editor.getText() + "] model=[" + edited.value().get() + ", " + other.value().get()
                + "] commitEvents=" + commits + " propertyChanges=" + changes + " focus=" + focus
                + " stageFocused=" + stage.isFocused() + " selection=" + selection;
    }

    private void later(Runnable action) {
        Platform.runLater(() -> checked(action));
    }

    private void checked(Runnable action) {
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
