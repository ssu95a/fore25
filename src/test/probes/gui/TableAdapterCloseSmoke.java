import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import ru.inversion.dataset.ArrayDataSet;
import ru.inversion.dataset.DataSetEvent;
import ru.inversion.dataset.DataSetNavigationEvent;
import ru.inversion.dataset.DataSetRowEvent;
import ru.inversion.dataset.fx.DSFXAdapter;
import ru.inversion.fx.form.controls.JInvTable;

public final class TableAdapterCloseSmoke {
    private static final class Rows extends ArrayDataSet<String> {
        int cursorRequests;

        Rows() { super(String.class, FXCollections.observableArrayList("A", "B", "C"), false); }

        @Override public boolean setCurrentRowNum(int row) {
            cursorRequests++;
            return super.setCurrentRowNum(row);
        }

        void executeNotification() {
            fireDataSetEvent(DataSetEvent.DataSetEventType.EXECUTE, false);
        }
    }

    private static final class Adapter extends DSFXAdapter<String> {
        int navigation;
        int rows;
        int executions;

        @Override public void navigated(DataSetNavigationEvent<String> event) {
            navigation++;
            super.navigated(event);
        }

        @Override public void rowOperation(DataSetRowEvent<String> event) {
            rows++;
            super.rowOperation(event);
        }

        @Override public void dataSetChanged(DataSetEvent event) {
            executions++;
            super.dataSetChanged(event);
        }

        List<Integer> counts() { return List.of(navigation, rows, executions); }
    }

    private static void exercise() throws Exception {
        require(Runtime.version().feature() == 25 && System.getProperty("javafx.version", "").startsWith("25"),
                "Java/JavaFX 25");
        var rows = new Rows();
        var adapter = new Adapter();
        var table = new JInvTable<String>();
        var stage = new Stage();
        stage.setScene(new Scene(new StackPane(table), 320, 200));
        adapter.bindTable(rows, table, false);
        var homeConsumed = new ArrayList<Boolean>();
        var releases = new ArrayList<KeyCode>();
        table.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.HOME) {
                homeConsumed.add(event.isConsumed());
                event.consume();
            }
        });
        table.addEventHandler(KeyEvent.KEY_RELEASED, event -> releases.add(event.getCode()));
        stage.show();
        var errors = new ArrayList<Throwable>();
        var thread = Thread.currentThread();
        var previousHandler = thread.getUncaughtExceptionHandler();
        thread.setUncaughtExceptionHandler((t, failure) -> errors.add(failure));
        try {
            rows.setCurrentRowNum(1);
            table.getSelectionModel().clearAndSelect(1);
            int before = rows.cursorRequests;
            key(table, KeyEvent.KEY_RELEASED, KeyCode.DOWN, errors);
            key(table, KeyEvent.KEY_PRESSED, KeyCode.HOME, errors);
            require(errors.isEmpty() && rows.cursorRequests == before + 1 && rows.getCurrentRowNum() == 1
                    && releases.equals(List.of(KeyCode.DOWN)) && homeConsumed.equals(List.of(true)), "pre-close key paths");
            rows.executeNotification();
            require(adapter.executions == 1, "pre-close dataset notification");
            var adapterBefore = adapter.counts();
            adapter.close();
            adapter.close();
            require(stage.isShowing() && adapter.getDataSet() == null, "adapter closed with visible table");
            before = rows.cursorRequests;
            key(table, KeyEvent.KEY_RELEASED, KeyCode.DOWN, errors);
            key(table, KeyEvent.KEY_PRESSED, KeyCode.HOME, errors);
            int postCloseRefreshes = rows.cursorRequests - before;
            var independent = new ArrayList<String>();
            rows.addNavigationListener(event -> independent.add("navigation"));
            rows.addRowListener(event -> independent.add("row"));
            rows.addDataSetListener(event -> independent.add("execute"));
            rows.setCurrentRowNum(2);
            rows.updateCurrentRow("C updated");
            rows.executeNotification();
            require(independent.equals(List.of("navigation", "row", "execute"))
                    && adapter.counts().equals(adapterBefore) && table.getSelectionModel().getSelectedIndex() == 1,
                    "late dataset events did not reach closed adapter or move table cursor");
            System.out.printf("pre-close refresh=1; post-close refresh=%d, caller releases=%s, HOME consumed=%s, adapter=%s, FX errors=%s%n",
                    postCloseRefreshes, releases, homeConsumed, adapter.counts(), errors);
            require(postCloseRefreshes == 0 && errors.isEmpty() && releases.equals(List.of(KeyCode.DOWN, KeyCode.DOWN))
                    && homeConsumed.equals(List.of(true, false)), "closed adapter still handles key input");
        } finally {
            thread.setUncaughtExceptionHandler(previousHandler);
            stage.hide();
        }
    }

    private static void key(JInvTable<String> table, javafx.event.EventType<KeyEvent> type,
                            KeyCode code, List<Throwable> errors) {
        try {
            table.fireEvent(new KeyEvent(type, "", "", code, false, false, false, false));
        } catch (RuntimeException failure) {
            errors.add(failure);
        }
    }

    public static void main(String[] args) {
        var test = new FutureTask<Void>(() -> { exercise(); return null; });
        Platform.startup(() -> Platform.runLater(test));
        int exit = 1;
        try {
            test.get(30, TimeUnit.SECONDS);
            System.out.println("CTRL-051 PASS");
            exit = 0;
        } catch (Exception failure) {
            failure.printStackTrace(System.err);
        } finally {
            Platform.exit();
        }
        System.exit(exit);
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
