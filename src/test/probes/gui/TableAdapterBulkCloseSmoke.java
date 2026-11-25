import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.scene.Scene;
import javafx.scene.control.TableColumn;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import ru.inversion.dataset.ArrayDataSet;
import ru.inversion.dataset.IDataSetNavigationListener;
import ru.inversion.dataset.fx.DSFXAdapter;
import ru.inversion.fx.form.controls.JInvTable;

public final class TableAdapterBulkCloseSmoke {
    private enum CloseAt { NEVER, BEFORE_SELECTION, AFTER_SELECTION }

    private static final class Rows extends ArrayDataSet<String> {
        final AtomicInteger swaps = new AtomicInteger();
        Runnable beforeSwapReturns;
        int adapterNavigationAdds;
        int explicitCursorRequests;

        Rows() {
            super(String.class, FXCollections.observableArrayList("A", "B", "C"), false);
        }

        @Override public boolean swappingData() {
            swaps.incrementAndGet();
            if (beforeSwapReturns != null) Platform.runLater(beforeSwapReturns);
            return false;
        }

        @Override public void addNavigationListener(IDataSetNavigationListener<String> listener) {
            super.addNavigationListener(listener);
            if (listener instanceof DSFXAdapter<?>) adapterNavigationAdds++;
        }

        @Override public boolean setCurrentRow(String row) {
            explicitCursorRequests++;
            return super.setCurrentRow(row);
        }
    }

    private record Result(int cursor, int explicitRequests, int adds, List<String> selected,
                          List<Throwable> errors) {}

    private static CompletableFuture<Result> exercise(CloseAt when) {
        var done = new CompletableFuture<Result>();
        Platform.runLater(() -> {
            var stage = new Stage();
            var adapter = new DSFXAdapter<String>();
            var rows = new Rows();
            var table = new JInvTable<String>();
            var selected = new ArrayList<String>();
            var errors = new ArrayList<Throwable>();
            var externalNavigation = new ArrayList<Integer>();
            Thread fx = Thread.currentThread();
            var previous = fx.getUncaughtExceptionHandler();
            try {
                var column = new TableColumn<String, String>("Row");
                column.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue()));
                table.getColumns().add(column);
                stage.setScene(new Scene(new StackPane(table), 320, 200));
                adapter.bindTable(rows, table, false);
                rows.addNavigationListener(event -> externalNavigation.add(event.getNewRowIndex()));
                stage.show();
                fx.setUncaughtExceptionHandler((thread, error) -> errors.add(error));
                Runnable closeTwice = () -> {
                    adapter.close();
                    adapter.close();
                };
                if (when == CloseAt.BEFORE_SELECTION) rows.beforeSwapReturns = closeTwice;
                table.getSelectionModel().selectedItemProperty().addListener((p, before, after) -> {
                    selected.add(after);
                    if (when == CloseAt.AFTER_SELECTION && selected.size() == 1) closeTwice.run();
                });
                try {
                    adapter.moveToLast();
                } catch (RuntimeException error) {
                    errors.add(error);
                }
                Platform.runLater(() -> Platform.runLater(() -> {
                    try {
                        if (rows.swaps.get() != 1) throw new AssertionError("bulk worker swap count");
                        var result = new Result(rows.getCurrentRowNum(), rows.explicitCursorRequests,
                                rows.adapterNavigationAdds, new ArrayList<>(selected), List.copyOf(errors));
                        rows.setCurrentRowNum(1);
                        Platform.runLater(() -> {
                            try {
                                if (externalNavigation.isEmpty() || externalNavigation.getLast() != 1) {
                                    throw new AssertionError("independent listener received no navigation");
                                }
                                if (when == CloseAt.NEVER && !"B".equals(table.getSelectionModel().getSelectedItem())) {
                                    throw new AssertionError("active adapter navigation listener was not restored");
                                }
                                if (when != CloseAt.NEVER && selected.size() != result.selected.size()) {
                                    throw new AssertionError("late navigation reached retired adapter");
                                }
                                done.complete(result);
                            } catch (Throwable error) {
                                done.completeExceptionally(error);
                            } finally {
                                fx.setUncaughtExceptionHandler(previous);
                                closeTwice.run();
                                stage.hide();
                            }
                        });
                    } catch (Throwable error) {
                        fx.setUncaughtExceptionHandler(previous);
                        closeTwice.run();
                        stage.hide();
                        done.completeExceptionally(error);
                    }
                }));
            } catch (Throwable error) {
                fx.setUncaughtExceptionHandler(previous);
                adapter.close();
                stage.hide();
                done.completeExceptionally(error);
            }
        });
        return done;
    }

    public static void main(String[] args) throws Exception {
        Platform.startup(() -> Platform.setImplicitExit(false));
        int exit = 1;
        try {
            var active = exercise(CloseAt.NEVER).get(20, TimeUnit.SECONDS);
            var before = exercise(CloseAt.BEFORE_SELECTION).get(20, TimeUnit.SECONDS);
            var between = exercise(CloseAt.AFTER_SELECTION).get(20, TimeUnit.SECONDS);
            System.out.printf("active=%s%nclose before selection=%s%nclose after selection=%s%n", active, before, between);
            if (!active.equals(new Result(2, 1, 2, List.of("C"), List.of()))
                    || !before.equals(new Result(-1, 0, 1, List.of(), List.of()))
                    || between.explicitRequests != 0 || between.adds != 1
                    || !between.selected.equals(List.of("C")) || !between.errors.isEmpty()) {
                throw new AssertionError("bulk closure matrix failed");
            }
            System.out.println("CTRL-076 PASS");
            exit = 0;
        } catch (Throwable error) {
            error.printStackTrace();
        } finally {
            Platform.exit();
        }
        System.exit(exit);
    }
}
