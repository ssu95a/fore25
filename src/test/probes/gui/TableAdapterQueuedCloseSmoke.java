import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.scene.Scene;
import javafx.scene.control.TableColumn;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import ru.inversion.dataset.ArrayDataSet;
import ru.inversion.dataset.DataSetEvent;
import ru.inversion.dataset.fx.DSFXAdapter;
import ru.inversion.fx.form.controls.JInvTable;

public final class TableAdapterQueuedCloseSmoke {
    private enum Scenario { ACTIVE, POSITION, EXECUTE, BOTH }

    private static final class Rows extends ArrayDataSet<String> {
        int cursorRequests;
        Rows() {
            super(String.class, FXCollections.observableArrayList("A", "B", "C"), false);
        }
        void afterExecute() { fireDataSetEvent(DataSetEvent.DataSetEventType.EXECUTE, false); }
        @Override public boolean setCurrentRowNum(int row) {
            cursorRequests++;
            return super.setCurrentRowNum(row);
        }
    }

    private static final class Adapter extends DSFXAdapter<String> {
        final List<String> deliveries = new ArrayList<>();
        @Override public void refresh() {
            deliveries.add("refresh");
            super.refresh();
        }
    }

    private record Result(String selected, int cursor, int cursorRequests, List<String> deliveries,
                          List<String> observerEvents, List<Throwable> errors) {}

    private static final class Fixture {
        final CompletableFuture<Result> done = new CompletableFuture<>();
        final Rows rows = new Rows();
        final Adapter adapter = new Adapter();
        final Stage stage = new Stage();
        final JInvTable<String> table = new JInvTable<>();
        final List<String> observerEvents = new ArrayList<>();
        final List<Throwable> errors = new ArrayList<>();
        final Thread fx = Thread.currentThread();
        final Thread.UncaughtExceptionHandler previous = fx.getUncaughtExceptionHandler();
        final Scenario scenario;

        Fixture(Scenario scenario) { this.scenario = scenario; }

        void start() throws Exception {
            var column = new TableColumn<String, String>("Row");
            column.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue()));
            table.getColumns().add(column);
            stage.setScene(new Scene(new StackPane(table), 320, 200));
            adapter.bindTable(rows, table, false);
            table.getSelectionModel().selectedItemProperty().addListener(
                    (p, before, after) -> adapter.deliveries.add("selection:" + after));
            rows.addNavigationListener(event -> observerEvents.add("navigate:" + event.getNewRowIndex()));
            rows.addDataSetListener(event -> observerEvents.add("execute:" + event.isAfter()));
            fx.setUncaughtExceptionHandler((thread, error) -> errors.add(error));
            stage.show();
            table.getSelectionModel().select(0);
            Platform.runLater(() -> run(this::enqueue));
        }

        private void enqueue() {
            if (!"A".equals(table.getSelectionModel().getSelectedItem()) || rows.getCurrentRowNum() != 0) {
                throw new AssertionError("initial selection/cursor");
            }
            adapter.deliveries.clear();
            observerEvents.clear();
            rows.cursorRequests = 0;
            if (scenario != Scenario.EXECUTE) rows.setCurrentRowNum(1);
            if (scenario != Scenario.POSITION) rows.afterExecute();
            if (scenario == Scenario.ACTIVE) {
                rows.setCurrentRowNum(2);
                rows.afterExecute();
                rows.afterExecute();
            } else {
                adapter.close();
                adapter.close();
            }
            Platform.runLater(() -> run(this::observe));
        }

        private void observe() {
            if (scenario == Scenario.ACTIVE) {
                finish(snapshot());
                return;
            }
            rows.setCurrentRowNum(2);
            rows.afterExecute();
            Platform.runLater(() -> run(() -> finish(snapshot())));
        }

        private Result snapshot() {
            return new Result(table.getSelectionModel().getSelectedItem(), rows.getCurrentRowNum(),
                    rows.cursorRequests, List.copyOf(adapter.deliveries),
                    List.copyOf(observerEvents), List.copyOf(errors));
        }

        private void finish(Result result) {
            adapter.close();
            stage.hide();
            fx.setUncaughtExceptionHandler(previous);
            done.complete(result);
        }

        void run(CheckedAction action) {
            try {
                action.run();
            } catch (Throwable error) {
                done.completeExceptionally(error);
                fx.setUncaughtExceptionHandler(previous);
                adapter.close();
                stage.hide();
            }
        }
    }

    @FunctionalInterface
    private interface CheckedAction { void run() throws Exception; }

    private static Result exercise(Scenario scenario) throws Exception {
        var ready = new CompletableFuture<Fixture>();
        Platform.runLater(() -> {
            var fixture = new Fixture(scenario);
            ready.complete(fixture);
            fixture.run(fixture::start);
        });
        return ready.get(10, TimeUnit.SECONDS).done.get(10, TimeUnit.SECONDS);
    }

    public static void main(String[] args) throws Exception {
        Platform.startup(() -> Platform.setImplicitExit(false));
        try {
            var active = exercise(Scenario.ACTIVE);
            var position = exercise(Scenario.POSITION);
            var execute = exercise(Scenario.EXECUTE);
            var both = exercise(Scenario.BOTH);
            var fresh = exercise(Scenario.ACTIVE);
            System.out.printf("active=%s%nposition=%s%nexecute=%s%nboth=%s%nfresh=%s%n",
                    active, position, execute, both, fresh);
            var expectedActive = new Result("C", 2, 2,
                    List.of("selection:B", "refresh", "selection:C", "refresh", "refresh"),
                    List.of("navigate:1", "execute:true", "navigate:2", "execute:true", "execute:true"), List.of());
            if (!active.equals(expectedActive) || !fresh.equals(expectedActive)
                    || !position.equals(new Result("A", 2, 2, List.of(),
                            List.of("navigate:1", "navigate:2", "execute:true"), List.of()))
                    || !execute.equals(new Result("A", 2, 1, List.of(),
                            List.of("execute:true", "navigate:2", "execute:true"), List.of()))
                    || !both.equals(new Result("A", 2, 2, List.of(),
                            List.of("navigate:1", "execute:true", "navigate:2", "execute:true"), List.of()))) {
                throw new AssertionError("queued closure matrix failed");
            }
            System.out.println("CTRL-077 PASS");
        } finally {
            Platform.exit();
        }
    }
}
