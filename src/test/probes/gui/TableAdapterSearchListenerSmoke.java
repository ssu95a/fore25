import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.scene.Scene;
import javafx.scene.control.DialogPane;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.stage.Window;
import ru.inversion.dataset.ArrayDataSet;
import ru.inversion.dataset.DataSetRowEvent.RowOperationEnum;
import ru.inversion.dataset.IDataSetRowListener;
import ru.inversion.dataset.fx.DSFXAdapter;
import ru.inversion.fx.app.ThreadPoolManager;
import ru.inversion.fx.form.controls.JInvTable;

public final class TableAdapterSearchListenerSmoke {
    private static final String FAILURE = "CTRL-081 controlled search fetch failure";

    private static final class Rows extends ArrayDataSet<String> {
        final List<IDataSetRowListener<String>> adds = new ArrayList<>();
        final List<IDataSetRowListener<String>> removes = new ArrayList<>();
        final RuntimeException failure = new IllegalStateException(FAILURE);
        boolean rejectFetch;
        int swaps;

        Rows() {
            super(String.class, FXCollections.observableArrayList("A", "B", "C"), false);
        }

        @Override public void addRowListener(IDataSetRowListener<String> listener) {
            super.addRowListener(listener);
            adds.add(listener);
        }

        @Override public void removeRowListener(IDataSetRowListener<String> listener) {
            super.removeRowListener(listener);
            removes.add(listener);
        }

        @Override public boolean swappingData() {
            swaps++;
            if (rejectFetch) throw failure;
            return false;
        }

        void notifyInsert() {
            fireRowEvent(RowOperationEnum.INSERT, null, "A", 0, 0);
        }
    }

    private static void exercise() throws Exception {
        var rows = new Rows();
        var adapter = new DSFXAdapter<String>();
        var table = new JInvTable<String>();
        var result = new ArrayList<Boolean>();
        var failures = new ArrayList<String>();
        var externalEvents = new AtomicInteger();
        IDataSetRowListener<String> external = event -> externalEvents.incrementAndGet();
        var errors = new AtomicInteger();
        var stage = new Stage();
        stage.setScene(new Scene(new StackPane(table), 320, 200));
        adapter.bindTable(rows, table, false);
        rows.addRowListener(external);
        stage.show();

        ListChangeListener<Window> dialogs = changes -> {
            while (changes.next()) {
                for (Window window : changes.getAddedSubList()) {
                    if (window.getScene().getRoot() instanceof DialogPane pane) {
                        var labels = pane.lookupAll(".label");
                        if (labels.stream().anyMatch(node -> node instanceof javafx.scene.control.Label label
                                && label.getText() != null && label.getText().contains(FAILURE))) {
                            errors.incrementAndGet();
                        }
                        Platform.runLater(window::hide);
                    }
                }
            }
        };
        Window.getWindows().addListener(dialogs);
        try {
            adapter.findRow("B"::equals, result::add);
            adapter.findRow("missing"::equals, result::add);
            var searches = new AtomicInteger();
            rows.rejectFetch = true;
            adapter.findRow(row -> {
                searches.incrementAndGet();
                return false;
            }, result::add);
            rows.rejectFetch = false;
            adapter.findRow("C"::equals, result::add);

            var barrier = new Object();
            Platform.runLater(() -> Platform.runLater(() -> Platform.exitNestedEventLoop(barrier, null)));
            Platform.enterNestedEventLoop(barrier);

            int before = searches.get();
            rows.notifyInsert();
            int late = searches.get() - before;
            var temporary = rows.adds.subList(2, rows.adds.size());
            boolean exactOwners = temporary.size() == 4 && temporary.size() == rows.removes.size();
            for (int i = 0; exactOwners && i < temporary.size(); i++) {
                exactOwners = temporary.get(i) == rows.removes.get(i);
            }
            System.out.printf("results=%s swaps=%d adds=%d removes=%d late=%d external=%d errors=%d%n",
                    result, rows.swaps, temporary.size(), rows.removes.size(), late, externalEvents.get(), errors.get());
            if (!exactOwners || late != 0) failures.add("temporary listener ownership");
            if (!result.equals(List.of(true, false, true)) || rows.swaps != 2) failures.add("search results");
            if (externalEvents.get() != 1) failures.add("independent registration");
            if (errors.get() != 1) failures.add("original search error dialog");
            if (!failures.isEmpty()) throw new AssertionError(failures);
        } finally {
            Window.getWindows().removeListener(dialogs);
            adapter.close();
            stage.hide();
        }
    }

    public static void main(String[] args) throws Exception {
        var test = new FutureTask<Void>(() -> {
            exercise();
            return null;
        });
        Platform.startup(() -> Platform.runLater(test));
        try {
            test.get(30, TimeUnit.SECONDS);
            System.out.println("CTRL-081 PASS");
        } finally {
            Platform.exit();
            ThreadPoolManager.getInstance().shutdown();
        }
    }
}
