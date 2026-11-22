import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import javafx.animation.AnimationTimer;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.event.Event;
import javafx.scene.Scene;
import javafx.scene.control.TableCell;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import ru.inversion.dataset.ArrayDataSet;
import ru.inversion.dataset.DataSetEvent;
import ru.inversion.dataset.DataSetNavigationEvent;
import ru.inversion.dataset.DataSetRowEvent;
import ru.inversion.dataset.fx.DSFXAdapter;
import ru.inversion.fx.form.controls.JInvTable;
import ru.inversion.fx.form.controls.JInvTableColumn;
import ru.inversion.fx.form.controls.renderer.JInvTableCellString;
import ru.inversion.fx.form.controls.skin.JInvTableViewSkin;

public final class LegacyDataBindingSmoke {
    @Entity
    public static class Row {
        private String code;
        private String name;

        public Row() {}

        Row(String code, String name) {
            this.code = code;
            this.name = name;
        }

        @Id
        @Column(name = "CODE")
        public String getCode() { return code; }
        public void setCode(String code) { this.code = code; }

        @Column(name = "NAME")
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
    }

    private static final class ProbeDataSet extends ArrayDataSet<Row> {
        ProbeDataSet(List<Row> rows) {
            super(Row.class, FXCollections.observableArrayList(rows), false);
        }

        void emitExecuteNotification() {
            fireDataSetEvent(DataSetEvent.DataSetEventType.EXECUTE, false);
        }
    }

    private record Callbacks(int navigation, int rows, int dataset) {}

    private static final class ObservedAdapter extends DSFXAdapter<Row> {
        private int navigation;
        private int rows;
        private int dataset;

        @Override public void navigated(DataSetNavigationEvent<Row> event) {
            navigation++;
            super.navigated(event);
        }

        @Override public void rowOperation(DataSetRowEvent<Row> event) {
            rows++;
            super.rowOperation(event);
        }

        @Override public void dataSetChanged(DataSetEvent event) {
            dataset++;
            super.dataSetChanged(event);
        }

        Callbacks callbacks() {
            return new Callbacks(navigation, rows, dataset);
        }
    }

    private final CompletableFuture<Void> verdict = new CompletableFuture<>();
    private final List<Row> rows = List.of(new Row("A", "Alpha"), new Row("B", "Bravo"), new Row("C", "Charlie"));
    private final List<String> navigation = new ArrayList<>();
    private final List<String> selection = new ArrayList<>();
    private int datasetEvents;
    private int rowEvents;
    private boolean started;
    private ProbeDataSet dataset;
    private ObservedAdapter adapter;
    private JInvTable<Row> table;
    private JInvTableColumn<Row, String> code;
    private JInvTableColumn<Row, String> name;
    private Stage stage;

    public static void main(String[] args) {
        var smoke = new LegacyDataBindingSmoke();
        int exit = 1;
        try {
            require(Runtime.version().feature() == 25, "setup: Java 25 required");
            Platform.startup(() -> smoke.checked(smoke::show));
            smoke.verdict.get(30, TimeUnit.SECONDS);
            System.out.println("GUI-009 PASS");
            exit = 0;
        } catch (TimeoutException failure) {
            System.err.println("GUI-009 UNKNOWN: timed out waiting for Stage focus or FX pulses");
        } catch (Throwable failure) {
            System.err.println("GUI-009 failed; see binding, rendering, navigation or lifecycle diagnostic");
            failure.printStackTrace(System.err);
        } finally {
            Platform.exit();
        }
        System.exit(exit);
    }

    private void show() throws Exception {
        Platform.setImplicitExit(false);
        Thread.currentThread().setUncaughtExceptionHandler((thread, failure) -> verdict.completeExceptionally(failure));
        String version = System.getProperty("javafx.version", "UNKNOWN");
        require(version.equals("25") || version.startsWith("25."), "setup: JavaFX 25 required");
        System.out.println("Java=" + Runtime.version() + " JavaFX=" + version);
        dataset = new ProbeDataSet(rows);
        adapter = new ObservedAdapter();
        table = new JInvTable<>();
        code = column("Code", "CODE");
        name = column("Name", "NAME");
        table.getColumns().addAll(List.of(code, name));

        var scene = new Scene(new StackPane(table), 520, 280);
        scene.getStylesheets().add(Objects.requireNonNull(getClass().getResource("/css/general.css"),
                "setup: production CSS missing").toExternalForm());
        stage = new Stage();
        stage.setTitle("GUI-009 Legacy data binding smoke");
        stage.setScene(scene);
        adapter.bindTable(dataset, table, false);
        require(adapter.getTable() == table && adapter.getDataSet() == dataset && table.getController() == null,
                "binding: expected standalone adapter and table without a form controller");
        require(code.getCellValueFactory() != null && name.getCellValueFactory() != null,
                "binding: metadata-derived column factories missing");
        dataset.setCurrentRowNum(0);
        table.getSelectionModel().clearAndSelect(0);

        dataset.addNavigationListener(event -> navigation.add(event.getOldRowIndex() + "->" + event.getNewRowIndex()));
        dataset.addDataSetListener(event -> datasetEvents++);
        dataset.addRowListener(event -> rowEvents++);
        table.getSelectionModel().selectedIndexProperty().addListener(
                (value, before, after) -> selection.add(before + "->" + after));
        stage.focusedProperty().addListener((value, before, focused) -> {
            if (focused && !started) afterPulse(() -> {
                if (stage.isFocused() && !started) {
                    started = true;
                    verifyAndNavigate();
                }
            });
        });
        stage.show();
        stage.requestFocus();
    }

    private static JInvTableColumn<Row, String> column(String title, String field) {
        var column = new JInvTableColumn<Row, String>(title);
        column.setFieldName(field);
        column.setPrefWidth(155);
        return column;
    }

    private void verifyAndNavigate() {
        require(stage.isShowing() && table.getSkin() instanceof JInvTableViewSkin<?>,
                "rendering: visible Legacy table skin required");
        require(table.getItems().size() == rows.size(), "binding: expected three table items");
        for (int i = 0; i < rows.size(); i++) {
            require(table.getItems().get(i) == dataset.getRow(i) && dataset.getRow(i) == rows.get(i),
                    "binding: row object identity lost at " + i);
            requireText(i, code, rows.get(i).getCode());
            requireText(i, name, rows.get(i).getName());
        }
        System.out.println("RENDER A/Alpha B/Bravo C/Charlie; dataset row identity preserved");
        requireAligned(0);
        navigation.clear();
        selection.clear();
        int callbacksBefore = adapter.callbacks().navigation();
        table.requestFocus();
        require(table.isFocused(), "focus: table did not receive focus");
        Event.fireEvent(table, new KeyEvent(KeyEvent.KEY_PRESSED, "", "", KeyCode.DOWN, false, false, false, false));
        Event.fireEvent(table, new KeyEvent(KeyEvent.KEY_RELEASED, "", "", KeyCode.DOWN, false, false, false, false));
        afterPulse(() -> {
            requireAligned(1);
            requireTransitions(List.of("0->1"), callbacksBefore + 1);
            require(dataset.setCurrentRowNum(2), "navigation: Dataset cursor change failed");
            afterPulse(() -> {
                requireAligned(2);
                requireTransitions(List.of("0->1", "1->2"), callbacksBefore + 2);
                updateValue();
            });
        });
    }

    private void updateValue() {
        rows.get(2).setName("Charlie updated");
        afterPulse(() -> {
            String beforeRefresh = renderedCell(2, name).getText();
            adapter.refresh();
            afterPulse(() -> {
                requireText(2, name, "Charlie updated");
                requireText(1, name, "Bravo");
                requireAligned(2);
                System.out.println("UPDATE before refresh=[" + beforeRefresh + "] after adapter.refresh=[Charlie updated]");
                checkClose();
            });
        });
    }

    private void checkClose() {
        int eventsBefore = datasetEvents;
        int callbacksBefore = adapter.callbacks().dataset();
        dataset.emitExecuteNotification();
        afterPulse(() -> {
            require(datasetEvents == eventsBefore + 1 && adapter.callbacks().dataset() == callbacksBefore + 1,
                    "lifecycle: pre-close Dataset event did not reach both listeners exactly once");
            Callbacks before = adapter.callbacks();
            int navigationBefore = navigation.size();
            int rowsBefore = rowEvents;
            adapter.close();
            require(adapter.getDataSet() == null, "lifecycle: closed adapter still holds Dataset");
            int selected = table.getSelectionModel().getSelectedIndex();
            require(dataset.setCurrentRowNum(0), "lifecycle: post-close Dataset navigation failed");
            dataset.emitExecuteNotification();
            afterPulse(() -> {
                require(table.getSelectionModel().getSelectedIndex() == selected,
                        "lifecycle: Dataset still changes table selection after close");
                dataset.updateCurrentRow(rows.get(0));
                afterPulse(() -> {
                    require(navigation.size() == navigationBefore + 1 && datasetEvents == eventsBefore + 2
                                    && rowEvents == rowsBefore + 1,
                            "lifecycle: independent listeners did not observe post-close events");
                    require(adapter.callbacks().equals(before), "lifecycle: late callback to closed adapter");
                    System.out.println("CLOSE Dataset, navigation and row notifications observed independently; adapter=" + before);
                    stage.close();
                    verdict.complete(null);
                });
            });
        });
    }

    private void requireAligned(int index) {
        require(table.getSelectionModel().getSelectedIndex() == index && dataset.getCurrentRowNum() == index
                        && table.getSelectionModel().getSelectedItem() == rows.get(index)
                        && dataset.getCurrentRow() == rows.get(index),
                "navigation: expected both cursors and identities at " + index + " table="
                        + table.getSelectionModel().getSelectedIndex() + " dataset=" + dataset.getCurrentRowNum());
    }

    private void requireTransitions(List<String> expected, int adapterNavigation) {
        System.out.println("NAV " + navigation + " SEL " + selection + " adapterNav=" + adapter.callbacks().navigation());
        require(navigation.equals(expected) && selection.equals(expected)
                        && adapter.callbacks().navigation() == adapterNavigation,
                "navigation: missing or duplicate transition, expected " + expected);
    }

    private void requireText(int row, JInvTableColumn<Row, String> column, String expected) {
        String actual = renderedCell(row, column).getText();
        require(expected.equals(actual), "rendering: row " + row + " column " + column.getText()
                + " expected [" + expected + "], got [" + actual + "]");
    }

    private TableCell<?, ?> renderedCell(int row, JInvTableColumn<Row, String> column) {
        return table.lookupAll(".table-cell").stream()
                .filter(node -> node instanceof JInvTableCellString cell && cell.getIndex() == row
                        && cell.getTableColumn() == column && !cell.isEmpty() && cell.isVisible()
                        && cell.getWidth() > 0 && cell.getHeight() > 0)
                .map(node -> (TableCell<?, ?>) node)
                .findFirst()
                .orElseThrow(() -> new AssertionError("rendering: no realized Legacy cell at " + row + "/" + column.getText()));
    }

    private void afterPulse(CheckedAction action) {
        new AnimationTimer() {
            private boolean first = true;

            @Override public void handle(long now) {
                if (first) {
                    first = false;
                    return;
                }
                stop();
                checked(action);
            }
        }.start();
    }

    @FunctionalInterface
    private interface CheckedAction {
        void run() throws Exception;
    }

    private void checked(CheckedAction action) {
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
