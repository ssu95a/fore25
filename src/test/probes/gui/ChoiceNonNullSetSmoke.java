import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.scene.Node;
import ru.inversion.dataset.ArrayDataSet;
import ru.inversion.dataset.fx.DSControlAdapter;
import ru.inversion.fx.form.Binder;
import ru.inversion.fx.form.controls.Controls;
import ru.inversion.fx.form.controls.JInvChoiceBox;

public final class ChoiceNonNullSetSmoke {
    private record Row(Integer key) {}
    private static final Row A = new Row(1), B = new Row(2), NO_KEY = new Row(null);
    private static final String UNSUPPORTED = "Unsupported class control: " + JInvChoiceBox.class.getName();

    public static void main(String[] args) throws Exception {
        Platform.startup(() -> {});
        try {
            onFx(() -> { run(); return null; });
            var choice = new JInvChoiceBoxReference();
            onFx(choice::setup);
            onFx(() -> choice.data.setCurrentRowNum(0));
            onFx(() -> choice.fixture.expect(B, 1, 1));
            onFx(() -> choice.data.setCurrentRowNum(1));
            onFx(() -> choice.fixture.expect(A, 0, 2));
            onFx(choice.adapter::close);
            System.out.println("CTRL-019 PASS");
        } finally {
            Platform.exit();
        }
    }

    private static void run() throws Exception {
        String fx = System.getProperty("javafx.version", "");
        check(Runtime.version().feature() == 25 && fx.matches("25(?:\\..*)?"), "Java/JavaFX 25");
        System.out.println("Java=" + Runtime.version() + " JavaFX=" + fx);
        var failures = new ArrayList<String>();
        var plain = new JInvChoiceBox<String, String>();
        plain.getItems().setAll("A", "B");
        exercise(new Fixture<>(plain), "A", "B", "A", "B", "missing", 1, failures);
        var mapped = new JInvChoiceBox<Row, Integer>(FXCollections.observableArrayList(A, B));
        mapped.setValueFactory(Row::key);
        exercise(new Fixture<>(mapped), A, B, 1, 2, 99, 1, failures);
        var mappedWithNull = new JInvChoiceBox<Row, Integer>(FXCollections.observableArrayList(A, null, NO_KEY, B));
        mappedWithNull.setValueFactory(Row::key);
        exercise(new Fixture<>(mappedWithNull), A, B, 1, 2, 99, 3, failures);
        for (var rows : List.of(List.of(A, B), Arrays.asList(A, B, null))) {
            var data = new ArrayDataSet<>(Row.class, new ArrayList<>(rows), false);
            var box = new JInvChoiceBox<Row, Integer>();
            Binder.bindChoiceBox(box, data, null, Row::key);
            data.setCurrentRowNum(1);
            var fixture = new Fixture<>(box);
            var navigation = new ArrayList<Integer>();
            data.addNavigationListener(e -> navigation.add(e.getNewRowIndex()));
            expectUnsupported(() -> Controls.setValue((Node) box, null));
            expectUnsupported(() -> Controls.setValue((Object) box, null));
            box.setSelectedValue(null);
            fixture.expect(B, 1, 0);
            check(data.getCurrentRowNum() == 1 && navigation.isEmpty(), "null must not navigate");
            System.out.println("null overloads rejected; rows=" + rows.size() + ", cursor=1, zero events");
        }
        check(failures.isEmpty(), failures.toString());
    }

    private static <T, R> void exercise(Fixture<T, R> f, T a, T b, R aKey, R bKey, R missing,
                                        int bIndex, List<String> failures) {
        f.expect(null, -1, 0);
        f.box.setSelectedValue(aKey);
        f.expect(a, 0, 1);
        for (boolean nodeOverload : new boolean[]{true, false}) {
            f.box.setSelectedValue(aKey);
            f.reset();
            try {
                if (nodeOverload) Controls.setValue((Node) f.box, bKey);
                else Controls.setValue((Object) f.box, bKey);
            } catch (RuntimeException ex) {
                check(ex.getClass() == RuntimeException.class && UNSUPPORTED.equals(ex.getMessage()), ex.toString());
                f.expect(a, 0, 0);
                String red = (nodeOverload ? "Node" : "Object") + " key=" + bKey + ": " + ex.getMessage();
                System.out.println(red + "; zero events");
                failures.add(red);
                continue;
            }
            f.expect(b, bIndex, 1);
            Controls.setValue(f.box, bKey);
            Controls.setValue(f.box, missing);
            f.expect(b, bIndex, 1);
            System.out.println((nodeOverload ? "Node" : "Object") + " key=" + bKey
                    + ": one index/item/value/model event; repeat/unmatched zero");
        }
    }

    private static void expectUnsupported(Runnable write) {
        try {
            write.run();
            throw new AssertionError("null write unexpectedly succeeded");
        } catch (RuntimeException ex) {
            check(ex.getClass() == RuntimeException.class && UNSUPPORTED.equals(ex.getMessage()), ex.toString());
        }
    }

    private static final class JInvChoiceBoxReference {
        Fixture<Row, Integer> fixture;
        ArrayDataSet<Row> data;
        DSControlAdapter<Row> adapter;

        void setup() {
            var box = new JInvChoiceBox<Row, Integer>(FXCollections.observableArrayList(A, B));
            box.setValueFactory(Row::key);
            fixture = new Fixture<>(box);
            data = new ArrayDataSet<>(Row.class, new ArrayList<>(List.of(B, A)), false);
            adapter = new DSControlAdapter<>(data);
            adapter.bindControl(box, null, Row::key);
        }
    }

    private static final class Fixture<T, R> {
        final JInvChoiceBox<T, R> box;
        final SimpleObjectProperty<T> model = new SimpleObjectProperty<>();
        final int[] events = new int[4];

        Fixture(JInvChoiceBox<T, R> box) {
            this.box = box;
            model.set(box.getValue());
            box.valueProperty().bindBidirectional(model);
            box.getSelectionModel().selectedIndexProperty().addListener((o, a, b) -> events[0]++);
            box.getSelectionModel().selectedItemProperty().addListener((o, a, b) -> events[1]++);
            box.valueProperty().addListener((o, a, b) -> events[2]++);
            model.addListener((o, a, b) -> events[3]++);
        }

        void reset() { Arrays.fill(events, 0); }

        void expect(T item, int index, int count) {
            check(box.getSelectionModel().getSelectedIndex() == index
                    && box.getSelectionModel().getSelectedItem() == item && box.getValue() == item
                    && Controls.getValue(box) == item && model.get() == item
                    && Arrays.equals(events, new int[]{count, count, count, count}),
                    "selection/model/events: index=" + index + " count=" + count + " actual=" + Arrays.toString(events));
        }
    }

    private static void onFx(java.util.concurrent.Callable<Void> action) throws Exception {
        var task = new FutureTask<>(action);
        Platform.runLater(task);
        task.get(15, TimeUnit.SECONDS);
    }

    private static void onFx(Runnable action) throws Exception {
        onFx(() -> { action.run(); return null; });
    }

    private static void check(boolean ok, String label) {
        if (!ok) throw new AssertionError(label);
    }
}
