import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.FutureTask;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.scene.Scene;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.stage.Window;
import ru.inversion.dataset.ArrayDataSet;
import ru.inversion.dataset.DataSetRowEvent.RowOperationEnum;
import ru.inversion.dataset.IDataSetRowListener;
import ru.inversion.dataset.fx.DSFXAdapter;
import ru.inversion.fx.app.ThreadPoolManager;
import ru.inversion.fx.form.controls.JInvProgressInfoPane;
import ru.inversion.fx.form.controls.JInvTable;

public final class TableAdapterSearchWorkerCloseSmoke {
    private enum Route { HIT, MISS, BEFORE_ENTRY, INITIAL_SCAN, REGISTRATION, SWAP_MORE, SWAP_LAST, CANCEL, ERROR }
    private static final String FAILURE = "CTRL-093 controlled search failure";

    private static final class Rows extends ArrayDataSet<String> {
        final CountDownLatch release = new CountDownLatch(1);
        final CountDownLatch removed = new CountDownLatch(1);
        final AtomicInteger totals = new AtomicInteger();
        final AtomicInteger scans = new AtomicInteger();
        final AtomicInteger swaps = new AtomicInteger();
        final AtomicInteger calls = new AtomicInteger();
        final AtomicInteger externalEvents = new AtomicInteger();
        final List<IDataSetRowListener<String>> adds = new CopyOnWriteArrayList<>();
        final List<IDataSetRowListener<String>> removes = new CopyOnWriteArrayList<>();
        final IDataSetRowListener<String> external = e -> externalEvents.incrementAndGet();
        volatile Runnable nestedAction;
        volatile Route route;

        Rows() { super(String.class, FXCollections.observableArrayList("A", "B", "C"), false); }

        @Override public int getTotalRowCount() {
            if (!Platform.isFxApplicationThread()) totals.incrementAndGet();
            return super.getTotalRowCount();
        }

        @Override public boolean findRow(int start, Predicate<String> predicate) {
            int scan = scans.incrementAndGet();
            boolean found = super.findRow(start, predicate);
            if (scan == 1 && route == Route.INITIAL_SCAN) block();
            return found;
        }

        @Override public void addRowListener(IDataSetRowListener<String> listener) {
            super.addRowListener(listener);
            if (!(listener instanceof DSFXAdapter<?>) && listener != external) {
                adds.add(listener);
                if (route == Route.REGISTRATION) block();
            }
        }

        @Override public void removeRowListener(IDataSetRowListener<String> listener) {
            super.removeRowListener(listener);
            if (!(listener instanceof DSFXAdapter<?>) && listener != external) {
                removes.add(listener);
                removed.countDown();
            }
        }

        @Override public boolean swappingData() {
            swaps.incrementAndGet();
            if (route == Route.ERROR) throw new IllegalStateException(FAILURE);
            if (route == Route.SWAP_MORE || route == Route.SWAP_LAST || route == Route.CANCEL) block();
            return route == Route.SWAP_MORE;
        }

        void emit() { fireRowEvent(RowOperationEnum.INSERT, null, "A", 0, 0); }

        void block() {
            Platform.runLater(() -> {
                try { nestedAction.run(); }
                finally { release.countDown(); }
            });
            try {
                if (!release.await(10, TimeUnit.SECONDS)) throw new AssertionError("search barrier");
            } catch (InterruptedException error) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private static void exercise(Route route, List<String> failures) throws Exception {
        var rows = new Rows();
        var adapter = new DSFXAdapter<String>();
        var table = new JInvTable<String>();
        var stage = new Stage();
        var callbacks = new ArrayList<Boolean>();
        var errors = new ArrayList<String>();
        stage.setScene(new Scene(new StackPane(table), 340, 240));
        adapter.bindTable(rows, table, false);
        rows.addRowListener(rows.external);
        stage.show();
        rows.route = route;
        rows.nestedAction = () -> {
            if (route == Route.CANCEL) {
                Window.getWindows().stream().map(w -> w.getScene().getRoot())
                        .filter(JInvProgressInfoPane.class::isInstance).map(JInvProgressInfoPane.class::cast)
                        .findFirst().orElseThrow().getCancelBtn().fire();
            } else {
                adapter.close();
                adapter.close();
                rows.emit();
            }
        };
        ListChangeListener<Window> errorDialogs = changes -> {
            while (changes.next()) {
                for (Window window : changes.getAddedSubList()) {
                    if (window.getScene().getRoot() instanceof DialogPane pane) {
                        errors.add(pane.lookupAll(".label").stream().filter(Label.class::isInstance)
                                .map(Label.class::cast).map(Label::getText).toList().toString());
                        Platform.runLater(window::hide);
                    }
                }
            }
        };
        Window.getWindows().addListener(errorDialogs);
        var workerRelease = new CountDownLatch(1);
        try {
            if (route == Route.BEFORE_ENTRY || route == Route.CANCEL) {
                var executor = (ThreadPoolExecutor) ThreadPoolManager.getInstance().fixedExecutor();
                int occupants = executor.getMaximumPoolSize() - (route == Route.CANCEL ? 1 : 0);
                var occupied = new CountDownLatch(occupants);
                for (int i = 0; i < occupants; i++) executor.execute(() -> {
                    occupied.countDown();
                    try { workerRelease.await(10, TimeUnit.SECONDS); }
                    catch (InterruptedException e) { Thread.currentThread().interrupt(); }
                });
                if (!occupied.await(10, TimeUnit.SECONDS)) throw new AssertionError("worker slots");
                if (route == Route.BEFORE_ENTRY) Platform.runLater(() -> {
                    try { rows.nestedAction.run(); }
                    finally { workerRelease.countDown(); }
                });
            }
            adapter.findRow(row -> {
                rows.calls.incrementAndGet();
                return route == Route.HIT && "B".equals(row);
            }, callbacks::add);
            if (route == Route.CANCEL) {
                var marker = ThreadPoolManager.getInstance().fixedExecutor().submit(() -> {});
                marker.get(10, TimeUnit.SECONDS);
            }
            var gate = new Object();
            Platform.runLater(() -> Platform.runLater(() -> Platform.exitNestedEventLoop(gate, null)));
            Platform.enterNestedEventLoop(gate);

            int adds = rows.adds.size();
            int removes = rows.removes.size();
            int scans = rows.scans.get();
            int calls = rows.calls.get();
            rows.emit();
            int swaps = rows.swaps.get();
            int totals = rows.totals.get();
            var expected = switch (route) {
                case HIT -> List.of(true);
                case MISS, SWAP_LAST, CANCEL -> List.of(false);
                default -> List.of();
            };
            boolean registered = route != Route.BEFORE_ENTRY && route != Route.INITIAL_SCAN;
            boolean expectedClose = switch (route) {
                case BEFORE_ENTRY, INITIAL_SCAN, REGISTRATION, SWAP_MORE, SWAP_LAST -> true;
                default -> false;
            };
            boolean correct = totals == (route == Route.BEFORE_ENTRY ? 0 : 1)
                    && scans == (route == Route.BEFORE_ENTRY ? 0 : 1)
                    && calls == rows.calls.get() && adds == (registered ? 1 : 0) && removes == adds
                    && (!registered || rows.adds.getFirst() == rows.removes.getFirst())
                    && callbacks.equals(expected)
                    && swaps == switch (route) {
                        case MISS, SWAP_MORE, SWAP_LAST, CANCEL, ERROR -> 1;
                        default -> 0;
                    }
                    && rows.externalEvents.get() == (expectedClose ? 2 : 1)
                    && (route == Route.ERROR
                        ? errors.size() == 1 && errors.getFirst().contains(FAILURE)
                        : errors.isEmpty());
            if (!correct) failures.add(route.toString());
            System.out.printf("%s totals=%d scans=%d predicateCalls=%d swaps=%d adds=%d removes=%d callbacks=%s external=%d errors=%s%n",
                    route, totals, scans, calls, swaps, adds, removes, callbacks, rows.externalEvents.get(), errors);
        } finally {
            workerRelease.countDown();
            Window.getWindows().removeListener(errorDialogs);
            adapter.close();
            stage.hide();
        }
    }

    public static void main(String[] args) throws Exception {
        var test = new FutureTask<Void>(() -> {
            var failures = new ArrayList<String>();
            for (Route route : Route.values()) exercise(route, failures);
            if (!failures.isEmpty()) throw new AssertionError("search worker ownership: " + failures);
            return null;
        });
        Platform.startup(() -> {
            Platform.setImplicitExit(false);
            Platform.runLater(test);
        });
        try {
            test.get(70, TimeUnit.SECONDS);
            System.out.println("CTRL-093 PASS");
        } finally {
            Platform.exit();
            ThreadPoolManager.getInstance().shutdown();
        }
    }
}
