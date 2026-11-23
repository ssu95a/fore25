import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import javafx.scene.Scene;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import ru.inversion.dataset.ArrayDataSet;
import ru.inversion.dataset.DataSetNavigationEvent;
import ru.inversion.dataset.DataSetRowEvent;
import ru.inversion.dataset.IDataSet.InsertRowModeEnum;
import ru.inversion.dataset.IDataSetNavigationListener;
import ru.inversion.dataset.IDataSetRowListener;
import ru.inversion.fx.form.controls.JInvChoiceBox;
import ru.inversion.fx.form.controls.choicebox.DSChoiceBoxAdapter;

public final class ChoiceAdapterSmoke {
    private record Row(String label) {}
    private static final Row A = new Row("A");
    private static final Row B = new Row("B");
    private static final Row C = new Row("C");

    public static void main(String[] args) throws Exception {
        var task = new FutureTask<Void>(ChoiceAdapterSmoke::run, null);
        Platform.startup(() -> Platform.runLater(task));
        try {
            task.get(20, TimeUnit.SECONDS);
            System.out.println("CTRL-015 PASS");
        } finally {
            Platform.exit();
        }
    }

    private static void run() {
        String fx = System.getProperty("javafx.version", "");
        check(Runtime.version().feature() == 25 && fx.matches("25(?:\\..*)?"), "Java/JavaFX 25");
        System.out.println("Java=" + Runtime.version() + " JavaFX=" + fx);
        var data = new ObservedData(A, B);
        var box = new JInvChoiceBox<Row, String>();
        var adapter = new ObservedAdapter(data, box);
        var stage = new Stage();
        var root = new VBox(box);
        stage.setScene(new Scene(root, 280, 120));
        stage.show();
        try {
            var navigation = new ArrayList<Integer>();
            var indexes = new ArrayList<Integer>();
            var values = new ArrayList<Row>();
            var inserts = new ArrayList<Integer>();
            var added = new ArrayList<Row>();
            data.addNavigationListener(e -> navigation.add(e.getNewRowIndex()));
            data.addRowListener(e -> {
                if (e.getRowOperation() == DataSetRowEvent.RowOperationEnum.INSERT) {
                    inserts.add(e.getRowIndex());
                }
            });
            box.getSelectionModel().selectedIndexProperty().addListener((o, a, b) -> indexes.add(b.intValue()));
            box.valueProperty().addListener((o, a, b) -> values.add(b));
            adapter.addListener((ListChangeListener<Row>) change -> {
                while (change.next()) {
                    if (change.wasAdded()) added.addAll(change.getAddedSubList());
                }
            });
            data.setCurrentRowNum(0);
            box.setSelectedValue("B");
            box.setSelectedValue("B");
            data.setCurrentRowNum(0);
            state(data, box, 0, A);
            check(navigation.equals(List.of(0, 1, 0)) && indexes.equals(navigation)
                    && values.equals(List.of(A, B, A)) && adapter.navigationCalls == 3,
                    "normal navigation and repeat produce one callback per event");
            System.out.printf("adapter registration attempts row/nav=%d/%d, navigation callbacks=%d%n",
                    data.adapterRowAdds, data.adapterNavigationAdds, adapter.navigationCalls);

            data.insertRow(C, InsertRowModeEnum.AFTER_CURRENT, false);
            System.out.printf("insert row events=%s adapter callbacks=%d list additions=%s%n",
                    inserts, adapter.rowCalls, added);
            check(inserts.equals(List.of(1)) && adapter.rowCalls == 1 && added.equals(List.of(C)),
                    "one insert callback and one list addition");
            state(data, box, 0, A);

            var thread = Thread.currentThread();
            var previous = thread.getUncaughtExceptionHandler();
            var delivered = new ArrayList<Throwable>();
            thread.setUncaughtExceptionHandler((t, e) -> delivered.add(e));
            try {
                data.failNext = true;
                box.getSelectionModel().select(2);
            } finally {
                thread.setUncaughtExceptionHandler(previous);
            }
            check(delivered.equals(List.of(data.failure)), "original exception delivered through JavaFX");
            check(data.getCurrentRowNum() == 0 && box.getValue() == B, "failure occurred before cursor update");
            navigation.clear();
            indexes.clear();
            values.clear();
            data.setCurrentRowNum(1);
            boolean recovered = box.getSelectionModel().getSelectedIndex() == 1 && box.getValue() == C;
            System.out.printf("after deliberate failure: cursor=%d selection=%d value=%s navigation=%s selectionEvents=%s valueEvents=%s%n",
                    data.getCurrentRowNum(), box.getSelectionModel().getSelectedIndex(), box.getValue(),
                    navigation, indexes, values);
            check(navigation.equals(List.of(1)), "external navigation event delivered once after failure");
            if (recovered) {
                check(indexes.equals(List.of(1)) && values.equals(List.of(C)), "recovery changes selection/model once");
                box.setSelectedValue("A");
                state(data, box, 0, A);
            }
            nullDiagnostic(root);
            check(data.adapterRowAdds == 1 && data.adapterNavigationAdds == 1 && recovered,
                    "expected one registration per kind and navigation recovery");
        } finally {
            stage.close();
        }
    }

    private static void nullDiagnostic(VBox root) {
        var data = new ObservedData(A, B, null);
        var box = new JInvChoiceBox<Row, String>();
        root.getChildren().setAll(box);
        new ObservedAdapter(data, box);
        data.setCurrentRowNum(1);
        state(data, box, 1, B);
        var navigation = new ArrayList<Integer>();
        data.addNavigationListener(e -> navigation.add(e.getNewRowIndex()));
        box.setValue(null);
        System.out.printf("literal null diagnostic selected=%d cursor=%d navigation=%s%n",
                box.getSelectionModel().getSelectedIndex(), data.getCurrentRowNum(), navigation);
        state(data, box, 2, null);
        check(navigation.equals(List.of(2)), "existing literal-null behavior");
    }

    private static void state(ObservedData data, JInvChoiceBox<Row, String> box, int index, Row row) {
        check(data.getCurrentRowNum() == index && box.getSelectionModel().getSelectedIndex() == index
                && box.getValue() == row, "cursor/selection/value expected at " + index);
    }

    private static final class ObservedData extends ArrayDataSet<Row> {
        int adapterRowAdds, adapterNavigationAdds;
        boolean failNext;
        final RuntimeException failure = new IllegalStateException("controlled cursor failure");

        ObservedData(Row... rows) {
            super(Row.class, new ArrayList<>(Arrays.asList(rows)), false);
        }

        @Override
        public void addRowListener(IDataSetRowListener<Row> listener) {
            if (listener instanceof DSChoiceBoxAdapter<?, ?>) adapterRowAdds++;
            super.addRowListener(listener);
        }

        @Override
        public void addNavigationListener(IDataSetNavigationListener<Row> listener) {
            if (listener instanceof DSChoiceBoxAdapter<?, ?>) adapterNavigationAdds++;
            super.addNavigationListener(listener);
        }

        @Override
        public boolean setCurrentRowNum(int row) {
            if (failNext) {
                failNext = false;
                throw failure;
            }
            return super.setCurrentRowNum(row);
        }
    }

    private static final class ObservedAdapter extends DSChoiceBoxAdapter<Row, String> {
        int rowCalls, navigationCalls;

        ObservedAdapter(ObservedData data, JInvChoiceBox<Row, String> box) {
            super(data, box, null, Row::label);
        }

        @Override
        public void rowOperation(DataSetRowEvent event) {
            rowCalls++;
            super.rowOperation(event);
        }

        @Override
        public void navigated(DataSetNavigationEvent event) {
            navigationCalls++;
            super.navigated(event);
        }
    }

    private static void check(boolean ok, String description) {
        if (!ok) throw new AssertionError(description);
    }
}
