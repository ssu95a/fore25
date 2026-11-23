import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import javafx.application.Platform;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.event.Event;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ListCell;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.PickResult;
import javafx.scene.layout.VBox;
import javafx.scene.robot.Robot;
import javafx.stage.Stage;
import javafx.stage.Window;
import ru.inversion.dataset.ArrayDataSet;
import ru.inversion.dataset.fx.DSControlAdapter;
import ru.inversion.fx.form.controls.Controls;
import ru.inversion.fx.form.controls.JInvComboBox;
import ru.inversion.fx.form.controls.skin.JInvComboBoxListViewSkin;

public final class ComboReadOnlySmoke {
    private record Row(String value) {}

    private final List<String> failures = new ArrayList<>();
    private final boolean useRobot;
    private final SimpleStringProperty model = new SimpleStringProperty("A");
    private JInvComboBox<String, String> combo;
    private Button next;
    private Stage stage;
    private Robot robot;
    private int modelChanges;
    private int valueChanges;
    private int selectionChanges;
    private int listenerErrors;

    private ComboReadOnlySmoke(boolean useRobot) {
        this.useRobot = useRobot;
    }

    public static void main(String[] args) throws Exception {
        Platform.startup(() -> {});
        var probe = new ComboReadOnlySmoke(args.length > 0 && args[0].equals("robot"));
        try {
            fx(probe::setup);
            String javafx = System.getProperty("javafx.version", "UNKNOWN");
            if (Runtime.version().feature() != 25 || !javafx.matches("25(?:\\..*)?")) {
                throw new AssertionError("Java 25 and JavaFX 25 required");
            }
            System.out.println("Java=" + Runtime.version() + " JavaFX=" + javafx
                    + " input=" + (probe.useRobot ? "Windows Robot" : "targeted JavaFX events"));
            probe.run();
            if (!probe.failures.isEmpty()) {
                throw new AssertionError("CTRL-007 failures: " + probe.failures);
            }
            System.out.println("CTRL-007 PASS");
        } finally {
            Platform.exit();
        }
    }

    private void setup() {
        Thread.currentThread().setUncaughtExceptionHandler((thread, exception) -> {
            listenerErrors++;
            System.out.println("FX listener: " + exception);
        });
        combo = new JInvComboBox<>(FXCollections.observableArrayList("A", "B"));
        combo.setValue("A");
        combo.valueProperty().bindBidirectional(model);
        model.addListener((obs, old, value) -> modelChanges++);
        combo.valueProperty().addListener((obs, old, value) -> valueChanges++);
        combo.getSelectionModel().selectedItemProperty().addListener((obs, old, value) -> selectionChanges++);
        next = new Button("Focus target");
        stage = new Stage();
        stage.setTitle("CTRL-007 ComboBox readOnly");
        stage.setScene(new Scene(new VBox(12, combo, next), 320, 170));
        stage.show();
        stage.toFront();
        stage.requestFocus();
        check("production skin", combo.getSkin() instanceof JInvComboBoxListViewSkin<?>);
        if (useRobot) {
            robot = new Robot();
        }
    }

    private void run() throws Exception {
        repeatArrow("unlocked", false);
        prepare("A");
        clickArrow();
        fx(() -> check("unlocked arrow button opens popup", combo.isShowing()));
        choosePopupB();
        fx(() -> verify("unlocked popup mouse selection", "B", 1));
        if (!failures.isEmpty()) {
            throw new AssertionError("Invalid unlocked positive control: " + failures);
        }

        for (int cycle = 1; cycle <= 2; cycle++) {
            repeatArrow("lock cycle " + cycle, true);
            repeatArrow("unlock cycle " + cycle, false);
        }
        prepare("A");
        fx(combo::show);
        pause();
        fx(() -> check("popup open before lock", combo.isShowing()));
        fx(() -> combo.setReadOnly(true));
        pause();
        if (get(combo::isShowing)) {
            choosePopupB();
        }
        fx(() -> verify("popup open before lock cannot commit", "A", 0));
        fx(() -> check("locked popup closed", !combo.isShowing()));

        fx(() -> combo.setReadOnly(false));
        var focusSource = new SimpleBooleanProperty(true);
        fx(() -> combo.focusTraversableProperty().bind(focusSource));
        for (boolean source : new boolean[]{true, false}) {
            fx(() -> { combo.setReadOnly(false); focusSource.set(source); });
            repeatArrow("bound focus source=" + source, true);
            prepare("A");
            fx(() -> combo.setReadOnly(true));
            clickArrow();
            fx(() -> {
                check("bound locked click cannot show popup", !combo.isShowing());
                verify("bound locked click", "A", 0);
                check("focus binding preserved " + source, combo.focusTraversableProperty().isBound()
                        && combo.isFocusTraversable() == source);
            });
        }
        fx(() -> focusSource.set(true));
        key(combo, KeyCode.DOWN);
        fx(() -> {
            verify("bound source changed while locked", "A", 0);
            check("live focus binding retained", combo.focusTraversableProperty().isBound()
                    && combo.isFocusTraversable());
        });
        repeatArrow("bound unlock", false);
        prepare("A");
        clickArrow();
        choosePopupB();
        fx(() -> verify("bound unlock popup click", "B", 1));
        fx(() -> check("no listener exceptions", listenerErrors == 0));
        fx(() -> { combo.focusTraversableProperty().unbind(); combo.setReadOnly(true); });
        apiUpdates();
        editable();
        dataset();
    }

    private void repeatArrow(String label, boolean locked) throws Exception {
        for (KeyCode key : List.of(KeyCode.DOWN, KeyCode.UP)) {
            String from = key == KeyCode.DOWN ? "A" : "B";
            String to = key == KeyCode.DOWN ? "B" : "A";
            prepare(from);
            fx(() -> combo.setReadOnly(locked));
            fx(() -> check(label + " retained focus " + key, combo.isFocused()));
            key(combo, key);
            fx(() -> verify(label + " " + key, locked ? from : to, locked ? 0 : 1));
        }
    }

    private void apiUpdates() throws Exception {
        fx(() -> {
            program("setValue", () -> combo.setValue("B"));
            program("selectionModel.select", () -> combo.getSelectionModel().select("B"));
            program("setSelectedValue", () -> combo.setSelectedValue("B"));
            program("Controls.setValue", () -> Controls.setValue(combo, "B"));
            program("bound model write", () -> model.set("B"));
        });
    }

    private void program(String label, Runnable write) {
        combo.setValue("A");
        resetCounts();
        write.run();
        verify(label, "B", 1);
    }

    private void editable() throws Exception {
        fx(() -> { combo.setReadOnly(false); combo.setEditable(true); });
        for (boolean lock : new boolean[]{false, true}) {
            prepare("A");
            fx(() -> { combo.getEditor().end(); combo.setReadOnly(lock); });
            key(get(combo::getEditor), KeyCode.BACK_SPACE);
            fx(() -> {
                String draft = combo.getEditor().getText();
                check("editable editor Backspace locked=" + lock, lock ? "A".equals(draft) : draft.isEmpty());
                check("combo editable mode survives lock change", combo.isEditable());
                next.requestFocus();
            });
            pause();
            if (lock) {
                fx(() -> verify("editable locked focus loss", "A", 0));
            }
        }
    }

    private void dataset() throws Exception {
        var data = get(() -> new ArrayDataSet<>(Row.class,
                FXCollections.observableArrayList(new Row("A"), new Row("B"), new Row(null)), false));
        DSControlAdapter<Row> adapter = get(() -> new DSControlAdapter<>(data));
        try {
            fx(() -> {
                adapter.bindControl(combo, null, Row::value);
                combo.setValue("B");
            });
            for (int index = 0; index < 3; index++) {
                final int row = index;
                fx(() -> { resetCounts(); data.setCurrentRowNum(row); });
                String expected = new String[]{"A", "B", null}[index];
                fx(() -> verify("DataSet row " + row, expected, 1));
            }
        } finally {
            fx(adapter::close);
        }
    }

    private void prepare(String value) throws Exception {
        fx(() -> {
            combo.hide();
            combo.setReadOnly(false);
            combo.setValue(value);
            combo.requestFocus();
        });
        pause();
        fx(this::resetCounts);
    }

    private void clickArrow() throws Exception {
        var arrow = get(() -> combo.lookup(".arrow-button"));
        click(arrow);
    }

    private void choosePopupB() throws Exception {
        var cell = get(() -> Window.getWindows().stream().filter(w -> w != stage && w.isShowing())
                .flatMap(w -> w.getScene().getRoot().lookupAll(".list-cell").stream())
                .filter(n -> n instanceof ListCell<?> item && "B".equals(item.getItem()))
                .findFirst().orElse(null));
        fx(() -> check("popup B cell available", cell != null));
        if (cell != null) {
            click(cell);
        }
    }

    private void click(Node node) throws Exception {
        fx(() -> {
            var bounds = node.getBoundsInLocal();
            if (useRobot) {
                var point = node.localToScreen(bounds.getCenterX(), bounds.getCenterY());
                robot.mouseMove(point);
                robot.mouseClick(MouseButton.PRIMARY);
            } else {
                var point = node.localToScene(bounds.getCenterX(), bounds.getCenterY());
                for (var type : List.of(MouseEvent.MOUSE_PRESSED, MouseEvent.MOUSE_RELEASED, MouseEvent.MOUSE_CLICKED)) {
                    Event.fireEvent(node, new MouseEvent(type, point.getX(), point.getY(), point.getX(), point.getY(),
                            MouseButton.PRIMARY, 1, false, false, false, false, type == MouseEvent.MOUSE_PRESSED,
                            false, false, false, false, true, new PickResult(node, point.getX(), point.getY())));
                }
            }
        });
        pause();
    }

    private void key(Node node, KeyCode code) throws Exception {
        fx(() -> {
            if (useRobot) {
                robot.keyType(code);
            } else {
                for (var type : List.of(KeyEvent.KEY_PRESSED, KeyEvent.KEY_RELEASED)) {
                    Event.fireEvent(node, new KeyEvent(type, "", "", code, false, false, false, false));
                }
            }
        });
        pause();
    }

    private void pause() throws InterruptedException {
        Thread.sleep(useRobot ? 180 : 100);
    }

    private void resetCounts() {
        modelChanges = 0;
        valueChanges = 0;
        selectionChanges = 0;
    }

    private void verify(String label, String value, int count) {
        var selected = combo.getSelectionModel().getSelectedItem();
        check(label + " selection=" + selected + " value=" + combo.getValue() + " model=" + model.get()
                + " selection/value/model events=" + selectionChanges + "/" + valueChanges + "/" + modelChanges,
                java.util.Objects.equals(selected, value) && java.util.Objects.equals(combo.getValue(), value)
                && java.util.Objects.equals(model.get(), value) && selectionChanges == count
                && valueChanges == count && modelChanges == count);
    }

    private void check(String label, boolean condition) {
        System.out.println((condition ? "PASS " : "FAIL ") + label);
        if (!condition) {
            failures.add(label);
        }
    }

    private static void fx(Runnable action) throws Exception {
        get(() -> { action.run(); return null; });
    }

    private static <T> T get(Supplier<T> action) throws Exception {
        var task = new FutureTask<T>(action::get);
        Platform.runLater(task);
        return task.get(15, TimeUnit.SECONDS);
    }
}
