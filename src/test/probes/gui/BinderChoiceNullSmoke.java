import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.scene.Scene;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import ru.inversion.dataset.ArrayDataSet;
import ru.inversion.fx.form.Binder;
import ru.inversion.fx.form.FXEntity;
import ru.inversion.fx.form.controls.JInvChoiceBox;

public final class BinderChoiceNullSmoke {
    private record Row(Integer key) {}
    private static final Row A = new Row(1);
    private static final Row B = new Row(2);

    @Entity
    public static class Bean {
        private Integer key = 1;
        @Id @Column(name = "ID")
        public Integer getId() { return 10; }
        public void setId(Integer id) {}
        @Column(name = "CHOICE_KEY")
        public Integer getKey() { return key; }
        public void setKey(Integer value) { key = value; }
    }

    public static void main(String[] args) throws Exception {
        var task = new FutureTask<Void>(() -> {
            String fx = System.getProperty("javafx.version", "");
            check(Runtime.version().feature() == 25 && fx.matches("25(?:\\..*)?"), "Java/JavaFX 25");
            System.out.println("Java=" + Runtime.version() + " JavaFX=" + fx);
            var failures = new ArrayList<String>();
            exercise(false, false, failures);
            exercise(false, true, failures);
            exercise(true, false, failures);
            check(failures.isEmpty(), failures.toString());
            return null;
        });
        Platform.startup(() -> Platform.runLater(task));
        try {
            task.get(20, TimeUnit.SECONDS);
            System.out.println("CTRL-017 PASS");
        } finally {
            Platform.exit();
        }
    }

    private static void exercise(boolean specialized, boolean clear, List<String> failures) throws Exception {
        var box = new JInvChoiceBox<Row, Integer>(FXCollections.observableArrayList(A, B));
        var entity = new FXEntity<>(new Bean());
        var key = entity.<Integer>getProperty("key");
        int[] nullCalls = {0};
        var writeProjections = new ArrayList<Row>();
        Function<Row, Integer> strict = row -> {
            if (row == null) nullCalls[0]++;
            if (row != null && row == box.getValue() && !Objects.equals(row.key(), key.getValue())) {
                writeProjections.add(row);
            }
            return row.key();
        };
        var data = specialized ? new ArrayDataSet<>(Row.class, new ArrayList<>(Arrays.asList(A, B, null)), false) : null;
        if (data == null) box.setValueFactory(strict);
        else Binder.bindChoiceBox(box, data, null, strict);
        box.setFieldName("key");
        Binder.bind(entity, List.of(box), null);
        var stage = new Stage();
        stage.setScene(new Scene(new VBox(box), 220, 100));
        stage.show();
        try {
            var index = new ArrayList<Integer>();
            var item = new ArrayList<Row>();
            var value = new ArrayList<Row>();
            var model = new ArrayList<Integer>();
            var navigation = new ArrayList<Integer>();
            box.getSelectionModel().selectedIndexProperty().addListener((o, a, b) -> index.add(b.intValue()));
            box.getSelectionModel().selectedItemProperty().addListener((o, a, b) -> item.add(b));
            box.valueProperty().addListener((o, a, b) -> value.add(b));
            key.addListener((o, a, b) -> model.add(b));
            if (data != null) data.addNavigationListener(e -> navigation.add(e.getNewRowIndex()));
            check(box.getValue() == A && key.getValue() == 1, "initial binding");
            writeProjections.clear();
            box.getSelectionModel().select(1);
            check(box.getValue() == B && key.getValue() == 2 && writeProjections.equals(List.of(B)),
                    "genuine non-null projection exactly once");
            key.setValue(1);
            check(box.getValue() == A, "non-null model-to-control");
            key.setValue(null);
            check(box.getValue() == A, "model null retains selection");
            key.setValue(2);
            if (!specialized) box.setSelectedValue(99);
            check(index.equals(List.of(1, 0, 1)) && item.equals(List.of(B, A, B))
                    && value.equals(item) && model.equals(Arrays.asList(2, 1, null, 2)),
                    "non-null positive counts and legacy model null no-op");
            index.clear(); item.clear(); value.clear(); model.clear(); navigation.clear(); writeProjections.clear();

            var errors = new ArrayList<Throwable>();
            var thread = Thread.currentThread();
            var oldHandler = thread.getUncaughtExceptionHandler();
            thread.setUncaughtExceptionHandler((t, ex) -> errors.add(ex));
            try {
                if (data != null) data.setCurrentRowNum(2);
                else if (clear) box.getSelectionModel().clearSelection();
                else box.setValue(null);
                box.setValue(null);
            } finally {
                thread.setUncaughtExceptionHandler(oldHandler);
            }
            String label = specialized ? "specialized null row" : clear ? "clearSelection" : "setValue(null)";
            System.out.printf("%s index=%s item=%s value=%s model=%s key=%s nullMapperCalls=%d nav=%s errors=%s%n",
                    label, index, item, value, model, key.getValue(), nullCalls[0], navigation, errors);
            check(box.getValue() == null && item.equals(Arrays.asList((Row) null)) && value.equals(item),
                    "one item/value change");
            check(index.equals(specialized ? List.of(2, -1, 2) : List.of(-1))
                    && navigation.equals(specialized ? List.of(2) : List.of())
                    && (data == null || data.getCurrentRowNum() == 2), "existing cursor/index sequence");
            if (!errors.isEmpty() || nullCalls[0] != 0 || key.getValue() != null
                    || !model.equals(Arrays.asList((Integer) null)) || !writeProjections.isEmpty()) {
                failures.add(label + ": null must synchronize once without invoking mapper");
            }
            key.setValue(1);
            check(box.getValue() == A && Objects.equals(key.getValue(), 1), "recovery to non-null key");
        } finally {
            stage.hide();
        }
    }

    private static void check(boolean ok, String label) {
        if (!ok) throw new AssertionError(label);
    }
}
