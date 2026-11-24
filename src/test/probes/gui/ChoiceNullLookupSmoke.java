import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.scene.Scene;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import ru.inversion.dataset.ArrayDataSet;
import ru.inversion.fx.form.Binder;
import ru.inversion.fx.form.controls.JInvChoiceBox;

public final class ChoiceNullLookupSmoke {
    private record Row(String name, Integer key) {}
    private static final Row A = new Row("A", 1);
    private static final Row B = new Row("B", 2);
    private static final Row UNMAPPED = new Row("No key", null);

    public static void main(String[] args) throws Exception {
        var task = new FutureTask<Void>(() -> {
            run();
            return null;
        });
        Platform.startup(() -> Platform.runLater(task));
        try {
            task.get(20, TimeUnit.SECONDS);
            System.out.println("CTRL-018 PASS");
        } finally {
            Platform.exit();
        }
    }

    private static void run() throws Exception {
        String fx = System.getProperty("javafx.version", "");
        check(Runtime.version().feature() == 25 && fx.matches("25(?:\\..*)?"), "Java/JavaFX 25");
        System.out.println("Java=" + Runtime.version() + " JavaFX=" + fx);
        var stage = new Stage();
        var root = new VBox();
        stage.setScene(new Scene(root, 250, 120));
        stage.show();
        try {
            var failures = new ArrayList<String>();
            var plain = new JInvChoiceBox<String, String>();
            root.getChildren().setAll(plain);
            exercise("plain no-arg", plain, "A", "B", "B", "missing", failures);
            var mapped = new JInvChoiceBox<Row, Integer>(FXCollections.observableArrayList(A, B));
            mapped.setValueFactory(Row::key);
            root.getChildren().setAll(mapped);
            exercise("mapped items constructor", mapped, A, B, 2, 99, failures);
            mapped.getItems().setAll(A, null, UNMAPPED, B);
            mapped.getSelectionModel().select(0);
            attempt("projected null is not a match", mapped, 2, B, failures);
            var data = new ArrayDataSet<>(Row.class, new ArrayList<>(Arrays.asList(A, null, B)), false);
            var viaData = new JInvChoiceBox<Row, Integer>();
            root.getChildren().setAll(viaData);
            Binder.bindChoiceBox(viaData, data, null, Row::key);
            var navigation = new ArrayList<Integer>();
            data.addNavigationListener(e -> navigation.add(e.getNewRowIndex()));
            data.setCurrentRowNum(2);
            check(viaData.getValue() == B && data.getCurrentRowNum() == 2, "valid DataSet navigation to B");
            navigation.clear();
            viaData.getSelectionModel().select(1);
            viaData.setSelectedValue(null);
            check(viaData.getValue() == null && viaData.getSelectionModel().getSelectedIndex() == 1
                    && data.getCurrentRowNum() == 1 && navigation.equals(List.of(1)),
                    "explicit literal-null selection and cursor unchanged");
            System.out.println("explicit literal-null selection index=1 cursor=1 navigation=" + navigation);
            check(failures.isEmpty(), failures.toString());
        } finally {
            stage.close();
        }
    }

    private static <T, R> void exercise(String label, JInvChoiceBox<T, R> box, T a, T b, R match, R missing,
                                        List<String> failures) {
        var model = new SimpleObjectProperty<T>();
        box.valueProperty().bindBidirectional(model);
        var counts = new int[4];
        box.getSelectionModel().selectedIndexProperty().addListener((o, x, y) -> counts[0]++);
        box.getSelectionModel().selectedItemProperty().addListener((o, x, y) -> counts[1]++);
        box.valueProperty().addListener((o, x, y) -> counts[2]++);
        model.addListener((o, x, y) -> counts[3]++);
        box.getItems().setAll(List.of(a, b));
        box.getSelectionModel().select(0);
        Arrays.fill(counts, 0);
        box.setSelectedValue(match);
        check(Objects.equals(model.get(), b) && Arrays.equals(counts, new int[]{1, 1, 1, 1}),
                label + " positive without null");
        box.getItems().setAll(Arrays.asList(a, null, b));
        box.getSelectionModel().select(0);
        Arrays.fill(counts, 0);
        boolean matched = attempt(label + " match through literal null", box, match, b, failures);
        System.out.printf("%s match index/item/value/model=%s%n", label, Arrays.toString(counts));
        if (matched) {
            check(box.getSelectionModel().getSelectedIndex() == 2 && Objects.equals(model.get(), b)
                    && Arrays.equals(counts, new int[]{1, 1, 1, 1}), label + " one match notification");
        } else {
            check(Objects.equals(model.get(), a) && Arrays.equals(counts, new int[4]), label + " RED state");
        }
        T retained = box.getValue();
        Arrays.fill(counts, 0);
        if (matched) attempt(label + " repeat", box, match, retained, failures);
        attempt(label + " unmatched", box, missing, retained, failures);
        box.setSelectedValue(null);
        System.out.printf("%s repeat/unmatched/null index/item/value/model=%s%n", label, Arrays.toString(counts));
        check(Objects.equals(model.get(), retained) && Arrays.equals(counts, new int[4]), label + " no extra events");
    }

    private static <T, R> boolean attempt(String label, JInvChoiceBox<T, R> box, R key, T expected,
                                          List<String> failures) {
        try {
            box.setSelectedValue(key);
            check(Objects.equals(box.getValue(), expected), label);
            return true;
        } catch (NullPointerException ex) {
            System.out.println(label + ": " + ex);
            failures.add(label);
            return false;
        }
    }

    private static void check(boolean ok, String label) {
        if (!ok) throw new AssertionError(label);
    }
}
