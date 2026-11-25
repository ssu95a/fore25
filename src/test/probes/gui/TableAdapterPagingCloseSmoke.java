import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;
import javafx.animation.AnimationTimer;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.scene.Scene;
import javafx.scene.control.ScrollBar;
import javafx.scene.control.TableColumn;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import ru.inversion.dataset.ArrayDataSet;
import ru.inversion.dataset.DataSetEvent;
import ru.inversion.dataset.fx.DSFXAdapter;
import ru.inversion.fx.form.controls.JInvTable;
import ru.inversion.fx.form.controls.skin.JInvTableViewSkin;

public final class TableAdapterPagingCloseSmoke {
    private static final class Rows extends ArrayDataSet<String> {
        int fetches;
        int cursorRequests;
        boolean pageAvailable;

        Rows() {
            super(String.class, FXCollections.observableArrayList(
                    IntStream.range(0, 40).mapToObj(i -> "Row " + i).toList()), false);
        }

        @Override public boolean swappingData() {
            fetches++;
            if (!pageAvailable) return false;
            pageAvailable = false;
            getRows().add("Loaded " + fetches);
            return true;
        }

        @Override public boolean setCurrentRowNum(int row) {
            cursorRequests++;
            return super.setCurrentRowNum(row);
        }

        void executeNotification() { fireDataSetEvent(DataSetEvent.DataSetEventType.EXECUTE, false); }
    }

    private final CompletableFuture<Void> done = new CompletableFuture<>();
    private final List<Throwable> errors = new ArrayList<>();
    private final List<Integer> selections = new ArrayList<>();
    private final Rows rows = new Rows();
    private final DSFXAdapter<String> adapter = new DSFXAdapter<>();
    private final JInvTable<String> table = new JInvTable<>();
    private Stage stage;
    private Thread.UncaughtExceptionHandler previousHandler;

    private void start() throws Exception {
        var column = new TableColumn<String, String>("Row");
        column.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue()));
        table.getColumns().add(column);
        table.setFixedCellSize(24);
        stage = new Stage();
        stage.setScene(new Scene(new StackPane(table), 330, 220));
        adapter.bindTable(rows, table, false);
        table.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.PAGE_DOWN) event.consume();
        });
        table.getSelectionModel().selectedIndexProperty().addListener((p, oldIndex, index) -> selections.add(index.intValue()));
        previousHandler = Thread.currentThread().getUncaughtExceptionHandler();
        Thread.currentThread().setUncaughtExceptionHandler((t, failure) -> errors.add(failure));
        stage.show();
        stage.requestFocus();
        table.requestFocus();
        new AnimationTimer() {
            @Override public void handle(long now) {
                var bar = table.getScrollVertical();
                if (table.isFocused() && bar != null && bar.isVisible() && bar.getMax() > bar.getMin()) {
                    stop();
                    run(() -> pagingPositive(bar));
                }
            }
        }.start();
    }

    private void pagingPositive(ScrollBar bar) {
        table.getSelectionModel().clearAndSelect(0);
        bar.setValue(bar.getMin());
        int fetches = rows.fetches;
        bar.setValue(bar.getMax());
        require(rows.fetches == fetches + 1 && selections.getLast() == 0 && errors.isEmpty(),
                "active scrollbar requests one fetch without moving selection; fetches=" + (rows.fetches - fetches));
        rows.pageAvailable = true;
        fetches = rows.fetches;
        int size = rows.getRows().size();
        pageDown();
        require(rows.fetches == fetches + 1 && rows.getRows().size() == size + 1 && selections.getLast() == 0,
                "active PageDown requests one page and queues selection");
        Platform.runLater(() -> run(() -> closeBeforeSelection(bar)));
    }

    private void closeBeforeSelection(ScrollBar bar) {
        int selected = table.getSelectionModel().getSelectedIndex();
        require(selected == rows.getRows().size() - 1 && rows.getCurrentRowNum() == selected,
                "active queued selectLast changed the table and Dataset cursor");
        System.out.println("active scrollbar fetch=1, active PageDown fetch=1, queued selection/cursor=" + selected);
        table.getSelectionModel().clearAndSelect(0);
        bar.setValue(bar.getMax());
        rows.pageAvailable = true;
        int fetches = rows.fetches;
        pageDown();
        require(rows.fetches == fetches + 1 && table.getSelectionModel().getSelectedIndex() == 0,
                "second request queued before close");
        adapter.close();
        adapter.close();
        int beforeFetch = rows.fetches;
        int beforeCursor = rows.cursorRequests;
        int beforeSelection = selections.size();
        int beforeSize = rows.getRows().size();
        fireScrollbarTransition(bar);
        Platform.runLater(() -> run(() -> verifyClosed(bar, beforeFetch, beforeCursor, beforeSelection, beforeSize)));
    }

    private void verifyClosed(ScrollBar first, int fetches, int cursor, int selectionEvents, int size) {
        int queuedSelection = table.getSelectionModel().getSelectedIndex();
        int queuedEvents = selections.size() - selectionEvents;
        var oldErrors = List.copyOf(errors);
        table.setSkin(new JInvTableViewSkin<String>(table) {});
        table.applyCss();
        table.layout();
        var next = table.getScrollVertical();
        require(next != null && next != first, "replacement scrollbar installed");
        fireScrollbarTransition(first);
        fireScrollbarTransition(next);

        var late = new ArrayList<String>();
        rows.addNavigationListener(event -> late.add("navigation"));
        rows.addRowListener(event -> late.add("row"));
        rows.addDataSetListener(event -> late.add("execute"));
        require(rows.fetches == fetches && rows.cursorRequests == cursor && rows.getRows().size() == size,
                "no closed paging fetch, cursor request or row insertion");
        rows.setCurrentRowNum(1);
        rows.updateCurrentRow("Updated");
        rows.executeNotification();
        require(late.equals(List.of("navigation", "row", "execute"))
                && table.getSelectionModel().getSelectedIndex() == queuedSelection,
                "late Dataset events remain independent");
        System.out.printf("closed queued selection=%d changes=%d original scrollbar errors=%d total errors=%d late=%s%n",
                queuedSelection, queuedEvents, oldErrors.size(), errors.size(), late);
        require(queuedSelection == 0 && queuedEvents == 0 && errors.isEmpty(),
                "closed adapter still owns paging effects: " + errors);
        finish(null);
    }

    private static void fireScrollbarTransition(ScrollBar bar) {
        bar.setValue(bar.getMin());
        bar.setValue(bar.getMax());
    }

    private void pageDown() {
        table.fireEvent(new KeyEvent(KeyEvent.KEY_PRESSED, "", "", KeyCode.PAGE_DOWN, false, false, false, false));
    }

    private void run(CheckedAction step) {
        try { step.run(); }
        catch (Throwable failure) { finish(failure); }
    }

    private void finish(Throwable failure) {
        if (previousHandler != null) Thread.currentThread().setUncaughtExceptionHandler(previousHandler);
        if (stage != null) stage.hide();
        if (failure == null) done.complete(null);
        else done.completeExceptionally(failure);
    }

    @FunctionalInterface
    private interface CheckedAction { void run() throws Exception; }

    public static void main(String[] args) throws Exception {
        Platform.startup(() -> Platform.runLater(() -> {
            var test = new TableAdapterPagingCloseSmoke();
            test.run(test::start);
            test.done.whenComplete((unused, failure) -> {
                if (failure == null) verdict.complete(null);
                else verdict.completeExceptionally(failure);
            });
        }));
        try {
            verdict.get(30, TimeUnit.SECONDS);
            System.out.println("CTRL-065 PASS");
        } finally {
            Platform.exit();
        }
    }

    private static final CompletableFuture<Void> verdict = new CompletableFuture<>();

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
