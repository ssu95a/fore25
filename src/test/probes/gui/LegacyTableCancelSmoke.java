import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import javafx.animation.AnimationTimer;
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
    private final boolean reentry;
    private Stage stage;
    private JInvTable<Row> table;
    private JInvTableColumn<Row, String> column;
    private boolean started;

    private LegacyTableCancelSmoke(boolean reentry) {
        this.reentry = reentry;
    }

    public static void main(String[] args) {
        var smoke = new LegacyTableCancelSmoke(args.length == 1 && "reentry".equals(args[0]));
        String id = smoke.reentry ? "GUI-007" : "GUI-006";
        int exit = 1;
        try {
            require(Runtime.version().feature() == 25, "setup: Java 25 required");
            Platform.startup(() -> smoke.checked(smoke::show));
            boolean conclusive = smoke.verdict.get(30, TimeUnit.SECONDS);
            System.out.println(conclusive ? id + " PASS" : id + " UNKNOWN: mouse-capture path unconfirmed");
            exit = conclusive ? 0 : 2;
        } catch (TimeoutException failure) {
            System.err.println(id + " UNKNOWN: timed out waiting for Stage focus or FX queue. Use an interactive desktop.");
        } catch (Throwable failure) {
            System.err.println(id + " failed; see setup, focus or scenario diagnostics.");
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
        stage.setTitle(reentry ? "GUI-007 Legacy editor reentry" : "GUI-006 Legacy table cancellation smoke");
        stage.setScene(scene);
        stage.focusedProperty().addListener((property, before, focused) -> {
            if (focused) later(() -> {
                if (stage.isFocused() && !started) {
                    started = true;
                    scene.getRoot().applyCss();
                    scene.getRoot().layout();
                    require(stage.isShowing() && table.getSkin() instanceof JInvTableViewSkin<?>,
                            "setup: visible Legacy table skin required");
                    if (reentry) {
                        selectAndEdit(0);
                        later(this::discardDraftAndReopen);
                    } else {
                        start(Scenario.PROGRAMMATIC);
                    }
                }
            });
        });
        stage.show();
        stage.requestFocus();
    }

    private void start(Scenario scenario) {
        selectAndEdit(0);
        later(() -> exercise(scenario));
    }

    private void selectAndEdit(int row) {
        table.requestFocus();
        table.getSelectionModel().clearAndSelect(row, column);
        table.edit(row, column);
    }

    private JInvTextFieldTableCell<?, ?> editedCell(int row) {
        return table.lookupAll(".table-cell").stream()
                .filter(node -> node instanceof JInvTextFieldTableCell<?, ?> candidate
                        && candidate.getIndex() == row && candidate.getTableColumn() == column
                        && !candidate.isEmpty() && candidate.getWidth() > 0 && candidate.getHeight() > 0)
                .map(node -> (JInvTextFieldTableCell<?, ?>) node)
                .findFirst()
                .orElseThrow(() -> new AssertionError("layout: no realized Legacy cell at row " + row));
    }

    private void exercise(Scenario scenario) {
        var cell = editedCell(0);
        require(cell.isEditing() && cell.getGraphic() instanceof TextField, scenario + ": editor did not open");
        var editor = (TextField) cell.getGraphic();
        require(stage.isFocused() && editor.isFocused(), scenario + ": editor did not receive focus");

        String draft = "Draft " + scenario;
        System.out.println(scenario + " BEFORE " + snapshot(editor));
        editor.setText(draft);
        if (scenario != Scenario.PROGRAMMATIC) {
            pressEditor(editor);
            require(cell.isEditing() && editor.isFocused() && commits.isEmpty() && changes.isEmpty(),
                    scenario + ": press within editor changed state before cancellation");
        }
        require(draft.equals(editor.getText()), scenario + ": editor draft was replaced before cancellation");
        System.out.println(scenario + " ACTION " + snapshot(editor));
        if (scenario == Scenario.MOUSE_THEN_ESCAPE) {
            escape(editor);
        } else {
            table.edit(-1, null);
        }
        later(() -> finish(scenario, cell, editor, draft));
    }

    private void discardDraftAndReopen() {
        var cell = editedCell(0);
        TextField editor = activeEditor(cell);
        require(stage.isFocused() && editor.isFocused(), "focus: fresh editor did not receive focus");
        System.out.println("FRESH " + snapshot(editor));
        String discarded = "GUI-007 discarded draft";
        editor.setText(discarded);
        escape(editor);
        later(() -> {
            System.out.println("AFTER ESCAPE " + snapshot(editor));
            require(table.getEditingCell() == null && !cell.isEditing() && cell.getGraphic() == null,
                    "Escape did not close the editor");
            require(commits.isEmpty() && changes.isEmpty() && "Original".equals(edited.value().get())
                            && "Other".equals(other.value().get()),
                    "Escape committed or changed the model before reentry");
            selectAndEdit(0);
            require(editedCell(0) == cell, "setup: reentry did not use the same realized cell");
            TextField reopened = activeEditor(cell);
            System.out.println("REOPEN IMMEDIATE reusedEditor=" + (reopened == editor) + " " + snapshot(reopened));
            afterPulse(() -> cancelUntouched(cell, reopened, discarded));
        });
    }

    private void cancelUntouched(JInvTextFieldTableCell<?, ?> cell, TextField editor, String discarded) {
        require(activeEditor(cell) == editor && stage.isFocused() && editor.isFocused(),
                "focus: reopened editor did not stay active through layout pulses");
        String untouched = editor.getText();
        System.out.println("REOPEN AFTER PULSE " + snapshot(editor));
        pressEditor(editor);
        require(activeEditor(cell) == editor && untouched.equals(editor.getText())
                        && commits.isEmpty() && changes.isEmpty(),
                "untouched reentry: editor or model changed before programmatic cancel");
        table.edit(-1, null);
        later(() -> {
            System.out.println("AFTER UNTOUCHED CANCEL " + snapshot(editor));
            require(table.getEditingCell() == null && !cell.isEditing() && cell.getGraphic() == null,
                    "untouched reentry: cancellation did not close editor");
            require("Other".equals(other.value().get()) && changes.stream().noneMatch(change -> change.row() == other),
                    "untouched reentry: changed the other row");
            require(commits.size() <= 1 && changes.size() <= 1, "untouched reentry: duplicate commit or property change");
            if (!commits.isEmpty()) {
                require(commits.equals(List.of(new Commit(0, column, untouched)))
                                && Objects.equals(untouched, edited.value().get()),
                        "untouched reentry: commit event and model disagree");
                var expected = Objects.equals("Original", untouched) ? List.of()
                        : List.of(new Change(edited, "Original", untouched));
                require(changes.equals(expected), "untouched reentry: unexpected property changes");
                require(!discarded.equals(edited.value().get()),
                        "GUI-007 REPRODUCED: Escape-discarded draft was committed on untouched reentry");
                require("Original".equals(untouched) && changes.isEmpty(),
                        "untouched reentry: cancel changed the original model value");
            } else {
                require(changes.isEmpty() && "Original".equals(edited.value().get()),
                        "untouched reentry: model changed without a commit event");
            }
            boolean reentryCommitObserved = !commits.isEmpty();
            commits.clear();
            selectAndEdit(1);
            afterPulse(() -> checkColdUntouched(reentryCommitObserved));
        });
    }

    private void checkColdUntouched(boolean reentryCommitObserved) {
        var cell = editedCell(1);
        TextField editor = activeEditor(cell);
        require(stage.isFocused() && editor.isFocused(), "focus: cold editor did not receive focus");
        String untouched = editor.getText();
        System.out.println("COLD FIRST EDIT " + snapshot(editor));
        pressEditor(editor);
        require(activeEditor(cell) == editor && untouched.equals(editor.getText())
                        && commits.isEmpty() && changes.isEmpty(),
                "cold first edit: press changed the editor or model before cancellation");
        table.edit(-1, null);
        later(() -> {
            System.out.println("AFTER COLD UNTOUCHED CANCEL " + snapshot(editor));
            require(table.getEditingCell() == null && !cell.isEditing() && cell.getGraphic() == null,
                    "cold first edit: cancellation did not close editor");
            require("Other".equals(untouched) && "Other".equals(other.value().get())
                            && "Original".equals(edited.value().get()) && changes.isEmpty(),
                    "GUI-008: cold untouched editor did not preserve the original value");
            require(commits.isEmpty() || commits.equals(List.of(new Commit(1, column, "Other"))),
                    "cold first edit: unexpected or duplicate commit event");
            stage.close();
            verdict.complete(reentryCommitObserved && !commits.isEmpty());
        });
    }

    private static TextField activeEditor(JInvTextFieldTableCell<?, ?> cell) {
        require(cell.isEditing() && cell.getGraphic() instanceof TextField, "editing: Legacy editor not active");
        return (TextField) cell.getGraphic();
    }

    private static void pressEditor(TextField editor) {
        var pick = new PickResult(editor, 4, 4);
        Event.fireEvent(editor, new MouseEvent(MouseEvent.MOUSE_PRESSED, 4, 4, 4, 4,
                MouseButton.PRIMARY, 1, false, false, false, false, true, false, false,
                true, false, true, pick));
    }

    private static void escape(TextField editor) {
        Event.fireEvent(editor, new KeyEvent(KeyEvent.KEY_PRESSED, "", "", KeyCode.ESCAPE,
                false, false, false, false));
    }

    private void afterPulse(Runnable action) {
        new AnimationTimer() {
            private boolean firstFrame = true;

            @Override public void handle(long now) {
                if (firstFrame) {
                    firstFrame = false;
                    return;
                }
                stop();
                checked(action);
            }
        }.start();
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
