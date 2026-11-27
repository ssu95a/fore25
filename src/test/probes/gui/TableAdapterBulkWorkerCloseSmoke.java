import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.scene.Scene;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.stage.Window;
import ru.inversion.dataset.ArrayDataSet;
import ru.inversion.dataset.IDataSetNavigationListener;
import ru.inversion.dataset.fx.DSFXAdapter;
import ru.inversion.fx.app.ThreadPoolManager;
import ru.inversion.fx.form.controls.JInvTable;
import ru.inversion.fx.form.controls.JInvProgressInfoPane;

public final class TableAdapterBulkWorkerCloseSmoke {
    private enum Route { NORMAL, BEFORE_ENTRY, BEFORE_FIRST_SWAP, FIRST_SWAP, SECOND_SWAP, CANCEL, ERROR }

    private static final class Rows extends ArrayDataSet<String> {
        final AtomicInteger totals = new AtomicInteger();
        final AtomicInteger swaps = new AtomicInteger();
        final AtomicInteger reports = new AtomicInteger();
        final CountDownLatch release = new CountDownLatch(1);
        final CountDownLatch finishedSwap = new CountDownLatch(1);
        volatile Runnable nestedAction;
        volatile Route route;
        int navigationAdds;
        int cursorRequests;

        Rows() {
            super(String.class, FXCollections.observableArrayList("A", "B", "C"), false);
        }

        @Override public int getTotalRowCount() {
            if (!Platform.isFxApplicationThread()) {
                totals.incrementAndGet();
                if (route == Route.BEFORE_FIRST_SWAP) barrier();
            }
            return super.getTotalRowCount();
        }

        @Override public int getLoadedRowCount() {
            if (!Platform.isFxApplicationThread()) reports.incrementAndGet();
            return super.getLoadedRowCount();
        }

        @Override public boolean swappingData() {
            int i = swaps.incrementAndGet();
            if (route == Route.ERROR) throw new IllegalStateException("CTRL-092 controlled swap error");
            if (route == Route.CANCEL || route == Route.FIRST_SWAP && i == 1
                    || route == Route.SECOND_SWAP && i == 2) {
                try {
                    barrier();
                    return true;
                } finally {
                    finishedSwap.countDown();
                }
            }
            return i == 1;
        }

        private void barrier() {
            Platform.runLater(() -> {
                try {
                    nestedAction.run();
                } finally {
                    release.countDown();
                }
            });
            try {
                if (!release.await(10, TimeUnit.SECONDS)) throw new AssertionError("fixture barrier");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        @Override public void addNavigationListener(IDataSetNavigationListener<String> listener) {
            super.addNavigationListener(listener);
            if (listener instanceof DSFXAdapter<?>) navigationAdds++;
        }

        @Override public boolean setCurrentRow(String row) {
            cursorRequests++;
            return super.setCurrentRow(row);
        }
    }

    private static void exercise(Route route, List<String> failures) throws Exception {
        var rows = new Rows();
        var adapter = new DSFXAdapter<String>();
        var table = new JInvTable<String>();
        var stage = new Stage();
        var column = new TableColumn<String, String>("Row");
        column.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue()));
        table.getColumns().add(column);
        stage.setScene(new Scene(new StackPane(table), 340, 240));
        adapter.bindTable(rows, table, false);
        stage.show();

        var nav = new ArrayList<Integer>();
        rows.addNavigationListener(event -> nav.add(event.getNewRowIndex()));
        var errors = new ArrayList<String>();
        ListChangeListener<Window> errorDialogs = change -> {
            while (change.next()) {
                for (Window window : change.getAddedSubList()) {
                    if (window.getScene().getRoot() instanceof DialogPane pane) {
                        errors.add(pane.lookupAll(".label").stream().filter(Label.class::isInstance)
                                .map(Label.class::cast).map(Label::getText).toList().toString());
                        Platform.runLater(window::hide);
                    }
                }
            }
        };
        Window.getWindows().addListener(errorDialogs);
        rows.route = route;
        rows.nestedAction = () -> {
            if (route == Route.CANCEL) {
                Window.getWindows().stream().map(w -> w.getScene().getRoot())
                        .filter(JInvProgressInfoPane.class::isInstance).map(JInvProgressInfoPane.class::cast)
                        .findFirst().orElseThrow().getCancelBtn().fire();
            } else {
                adapter.close();
                adapter.close();
            }
        };

        var queuedWorker = new CountDownLatch(1);
        try {
            if (route == Route.BEFORE_ENTRY) {
                var executor = (java.util.concurrent.ThreadPoolExecutor) ThreadPoolManager.getInstance().fixedExecutor();
                var occupied = new CountDownLatch(executor.getMaximumPoolSize());
                for (int i = 0; i < executor.getMaximumPoolSize(); i++) {
                    executor.execute(() -> {
                        occupied.countDown();
                        try {
                            if (!queuedWorker.await(10, TimeUnit.SECONDS)) throw new AssertionError("worker queue gate");
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                    });
                }
                if (!occupied.await(10, TimeUnit.SECONDS)) throw new AssertionError("occupy executor");
                Platform.runLater(() -> {
                    try {
                        adapter.close();
                        adapter.close();
                    } finally {
                        queuedWorker.countDown();
                    }
                });
            }
            adapter.moveToLast();
            if (route == Route.CANCEL && !rows.finishedSwap.await(10, TimeUnit.SECONDS))
                throw new AssertionError("cancelled worker did not leave the swap");
            var first = new Object();
            Platform.runLater(() -> Platform.runLater(() -> Platform.exitNestedEventLoop(first, null)));
            Platform.enterNestedEventLoop(first);
            int expectedSwaps = switch (route) {
                case BEFORE_ENTRY, BEFORE_FIRST_SWAP -> 0;
                case FIRST_SWAP, CANCEL, ERROR -> 1;
                case NORMAL, SECOND_SWAP -> 2;
            };
            boolean retired = route == Route.BEFORE_ENTRY || route == Route.BEFORE_FIRST_SWAP
                    || route == Route.FIRST_SWAP || route == Route.SECOND_SWAP;
            String selected = table.getSelectionModel().getSelectedItem();
            int cursor = rows.getCurrentRowNum();
            int recordedErrors = errors.size();
            boolean end = adapter.isEnd();
            boolean valid = rows.swaps.get() == expectedSwaps
                    && rows.totals.get() == (route == Route.BEFORE_ENTRY ? 0 : 1)
                    && (retired
                    ? selected == null && cursor == -1 && rows.cursorRequests == 0 && rows.navigationAdds == 1
                        && !end && recordedErrors == 0 && nav.isEmpty()
                    : "C".equals(selected) && rows.navigationAdds == 2 && (route == Route.ERROR
                        ? cursor == -1 && rows.cursorRequests == 0 && recordedErrors == 1
                            && errors.getFirst().contains("CTRL-092 controlled swap error") && !end
                        : cursor == 2 && rows.cursorRequests == 1 && recordedErrors == 0
                            && nav.equals(List.of(2)) && end == (route == Route.NORMAL)));
            if (!valid) failures.add(route.toString());
            int before = nav.size();
            rows.setCurrentRowNum(1);
            if (nav.size() != before + 1 || nav.getLast() != 1) failures.add(route + " late independent event");
            System.out.printf("%s totals=%d swaps=%d progressCountReads=%d navigationAdds=%d selection=%s cursor=%d "
                            + "cursorRequests=%d observer=%s errors=%s%n",
                    route, rows.totals.get(), rows.swaps.get(), rows.reports.get(), rows.navigationAdds,
                    selected, cursor, rows.cursorRequests, nav, errors);
        } finally {
            queuedWorker.countDown();
            Window.getWindows().removeListener(errorDialogs);
            adapter.close();
            stage.hide();
        }
    }

    public static void main(String[] args) throws Exception {
        var test = new FutureTask<Void>(() -> {
            var failures = new ArrayList<String>();
            for (Route route : Route.values()) exercise(route, failures);
            if (!failures.isEmpty()) throw new AssertionError("bulk worker ownership: " + failures);
            return null;
        });
        Platform.startup(() -> {
            Platform.setImplicitExit(false);
            Platform.runLater(test);
        });
        try {
            test.get(50, TimeUnit.SECONDS);
            System.out.println("CTRL-092 PASS");
        } finally {
            Platform.exit();
            ThreadPoolManager.getInstance().shutdown();
        }
    }
}
