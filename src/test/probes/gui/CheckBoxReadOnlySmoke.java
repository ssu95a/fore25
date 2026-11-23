import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import javafx.application.Platform;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.css.PseudoClass;
import javafx.event.Event;
import javafx.event.EventType;
import javafx.scene.AccessibleAction;
import javafx.scene.Scene;
import javafx.scene.control.CheckBox;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.PickResult;
import javafx.scene.layout.VBox;
import javafx.scene.robot.Robot;
import javafx.stage.Stage;
import ru.inversion.dataset.ArrayDataSet;
import ru.inversion.dataset.fx.DSControlAdapter;
import ru.inversion.fx.form.controls.JInvCheckBox;
import ru.inversion.fx.form.controls.JInvCheckBoxString;

public final class CheckBoxReadOnlySmoke {
    private record Row(boolean selected) {}
    private record DataBinding(ArrayDataSet<Row> data, DSControlAdapter<Row> adapter) {}

    private final List<String> failures = new ArrayList<>();
    private final VBox root = new VBox(12);
    private final SimpleBooleanProperty model = new SimpleBooleanProperty();
    private final JInvCheckBox box = new JInvCheckBox("Boolean");
    private final Robot robot;
    private int selectedChanges;
    private int modelChanges;
    private int actions;
    private int listenerErrors;

    private CheckBoxReadOnlySmoke(boolean useRobot) {
        robot = useRobot ? new Robot() : null;
        Thread.currentThread().setUncaughtExceptionHandler((thread, error) -> {
            listenerErrors++;
            System.out.println("FX listener: " + error);
        });
        box.selectedProperty().bindBidirectional(model);
        box.selectedProperty().addListener((obs, old, value) -> selectedChanges++);
        model.addListener((obs, old, value) -> modelChanges++);
        box.setOnAction(event -> actions++);
        root.getChildren().add(box);
        var stage = new Stage();
        stage.setTitle("CTRL-009 CheckBox readOnly");
        stage.setScene(new Scene(root, 320, 160));
        stage.show();
        stage.toFront();
        stage.requestFocus();
    }

    public static void main(String[] args) throws Exception {
        Platform.startup(() -> {});
        try {
            boolean useRobot = args.length > 0 && args[0].equals("robot");
            var probe = get(() -> new CheckBoxReadOnlySmoke(useRobot));
            String javafx = System.getProperty("javafx.version", "UNKNOWN");
            if (Runtime.version().feature() != 25 || !javafx.matches("25(?:\\..*)?")) {
                throw new AssertionError("Java 25 and JavaFX 25 required");
            }
            System.out.println("Java=" + Runtime.version() + " JavaFX=" + javafx + " input="
                    + (useRobot ? "Windows Robot keyboard/mouse" : "targeted JavaFX keyboard/mouse"));
            probe.run();
            if (!probe.failures.isEmpty()) {
                throw new AssertionError("CTRL-009 failures: " + probe.failures);
            }
            System.out.println("CTRL-009 PASS");
        } finally {
            Platform.exit();
        }
    }

    private void run() throws Exception {
        prepare(false);
        space(box);
        fx(() -> verify("unlocked Space positive control", true, 1, 1));
        if (!failures.isEmpty()) {
            throw new AssertionError("Invalid unlocked positive control");
        }
        prepare(false);
        click(box);
        fx(() -> verify("unlocked primary click positive control", true, 1, 1));
        prepare(false);
        fx(() -> {
            box.executeAccessibleAction(AccessibleAction.FIRE);
            verify("unlocked accessibility FIRE positive control", true, 1, 1);
        });
        if (!failures.isEmpty()) {
            throw new AssertionError("Invalid mouse or accessibility positive control");
        }
        for (boolean initial : new boolean[]{false, true}) {
            prepare(initial);
            fx(() -> {
                box.setReadOnly(true);
                check("focus retained on lock", box.isFocused());
            });
            space(box);
            fx(() -> verify("locked Space from " + initial, initial, 0, 0));
            prepare(initial);
            key(box, KeyEvent.KEY_PRESSED);
            fx(() -> {
                check("Space press armed before lock", box.isArmed());
                box.setReadOnly(true);
            });
            key(box, KeyEvent.KEY_RELEASED);
            fx(() -> {
                verify("Space release across lock from " + initial, initial, 0, 0);
                box.setReadOnly(false);
            });
            space(box);
            fx(() -> verify("Space after unlock from " + initial, !initial, 1, 1));
        }
        boundProperties();
        programmaticAndDataSet();
        stringBox(get(JInvCheckBoxString::new), false);
        stringBox(get(() -> new JInvCheckBoxString("String")), true);
        fx(() -> check("no FX listener exceptions", listenerErrors == 0));
    }

    private void boundProperties() throws Exception {
        var focus = new SimpleBooleanProperty(true);
        var mouse = new SimpleBooleanProperty(false);
        var readOnly = new SimpleBooleanProperty();
        prepare(false);
        fx(() -> {
            box.focusTraversableProperty().bind(focus);
            box.mouseTransparentProperty().bind(mouse);
            box.readOnlyProperty().bind(readOnly);
            readOnly.set(true);
            check("bound readOnly retains focus and mouse hit eligibility",
                    box.isFocused() && box.isFocusTraversable() && !box.isMouseTransparent());
        });
        for (boolean source : new boolean[]{true, false, true}) {
            fx(() -> {
                focus.set(source);
                mouse.set(!source);
                check("bound focus/mouse sources remain live",
                        box.isFocusTraversable() == source && box.isMouseTransparent() == !source);
            });
            space(box);
            fx(() -> verify("Space with bound properties, focus=" + source, false, 0, 0));
        }
        click(box);
        fx(() -> verify("locked primary click with mouseTransparent bound false", false, 0, 0));
        fx(() -> {
            box.executeAccessibleAction(AccessibleAction.FIRE);
            verify("locked accessibility FIRE", false, 0, 0);
            check("readOnly/focus/mouse bindings intact", box.readOnlyProperty().isBound()
                    && box.focusTraversableProperty().isBound() && box.mouseTransparentProperty().isBound());
            readOnly.set(false);
            box.readOnlyProperty().unbind();
            box.focusTraversableProperty().unbind();
            box.mouseTransparentProperty().unbind();
        });
        prepare(false);
        mouse(box, MouseEvent.MOUSE_PRESSED);
        fx(() -> {
            check("primary press armed before lock", box.isArmed());
            box.setReadOnly(true);
        });
        mouse(box, MouseEvent.MOUSE_RELEASED);
        fx(() -> verify("primary release across lock", false, 0, 0));
        fx(() -> {
            box.setSelected(false);
            resetCounts();
            var disabled = PseudoClass.getPseudoClass("disabled");
            check("locked CSS without native disable", !box.isDisabled() && box.getPseudoClassStates().contains(disabled));
            root.setDisable(true);
            box.setReadOnly(false);
            check("unlock preserves ancestor disabled and CSS",
                    box.isDisabled() && box.getPseudoClassStates().contains(disabled));
            box.fire();
            verify("native disabled prevents fire", false, 0, 0);
            box.setReadOnly(true);
            root.setDisable(false);
            check("ancestor reenable preserves locked CSS",
                    !box.isDisabled() && box.getPseudoClassStates().contains(disabled));
            box.setReadOnly(false);
            check("unlock clears readOnly CSS", !box.getPseudoClassStates().contains(disabled));
        });
    }

    private void programmaticAndDataSet() throws Exception {
        prepare(false);
        fx(() -> {
            box.setReadOnly(true);
            box.setSelected(true);
            verify("locked setSelected", true, 1, 0);
            box.setSelected(true);
            verify("same selected value", true, 1, 0);
            resetCounts();
            box.selectedProperty().set(false);
            verify("locked selectedProperty.set", false, 1, 0);
            resetCounts();
            model.set(true);
            verify("locked Boolean model update", true, 1, 0);
            resetCounts();
            box.fire();
            verify("locked direct programmatic fire", false, 1, 1);
            box.selectedProperty().unbindBidirectional(model);
            box.selectedProperty().bind(model);
            model.set(true);
            resetCounts();
        });
        try {
            space(box);
        } catch (ExecutionException ex) {
            check("locked Space threw with unidirectionally bound selected: " + ex.getCause(), false);
        }
        fx(() -> {
            verify("locked Space with unidirectional selected binding", true, 0, 0);
            check("selected binding retained", box.selectedProperty().isBound());
            model.set(false);
            verify("unidirectional selected updates while locked", false, 1, 0);
            box.selectedProperty().unbind();
            box.selectedProperty().bindBidirectional(model);
        });
        var binding = get(() -> {
            var data = new ArrayDataSet<>(Row.class, FXCollections.observableArrayList(
                    new Row(false), new Row(true), new Row(false)), false);
            var adapter = new DSControlAdapter<>(data);
            adapter.bindControl(box, null, Row::selected);
            return new DataBinding(data, adapter);
        });
        try {
            for (int row : new int[]{1, 2}) {
                fx(() -> {
                    resetCounts();
                    binding.data().setCurrentRowNum(row);
                });
                fx(() -> verify("locked DataSet row " + row, row == 1, 1, 0));
            }
        } finally {
            fx(binding.adapter()::close);
        }
    }

    private void stringBox(JInvCheckBoxString control, boolean withText) throws Exception {
        var stringModel = new SimpleStringProperty("N");
        int[] counts = new int[4];
        String label = "String text constructor=" + withText;
        fx(() -> {
            control.setValueChecked("Y");
            control.setValueUnchecked("N");
            control.setValue("N");
            control.valueProperty().bindBidirectional(stringModel);
            control.selectedProperty().addListener((o, a, b) -> counts[0]++);
            control.indeterminateProperty().addListener((o, a, b) -> counts[1]++);
            control.valueProperty().addListener((o, a, b) -> counts[2]++);
            stringModel.addListener((o, a, b) -> counts[3]++);
            root.getChildren().setAll(control);
            control.requestFocus();
            check("String retained focus before lock, text constructor=" + withText, control.isFocused());
            control.setReadOnly(true);
        });
        space(control);
        fx(() -> {
            verifyString(label + " locked Space", control, stringModel, counts, false, false, "N", 0, 0, 0);
            control.setValue("N");
            Arrays.fill(counts, 0);
            control.setSelected(true);
            verifyString(label + " selected", control, stringModel, counts, true, false, "Y", 1, 0, 1);
            control.setValue("N");
            verifyString(label + " setValue", control, stringModel, counts, false, false, "N", 2, 0, 2);
            stringModel.set("Y");
            verifyString(label + " model write", control, stringModel, counts, true, false, "Y", 3, 0, 3);
        });
        space(control);
        fx(() -> {
            verifyString(label + " locked checked Space", control, stringModel, counts, true, false, "Y", 3, 0, 3);
            control.setValue("Y");
            Arrays.fill(counts, 0);
            control.setIndeterminate(true);
            verifyString(label + " direct indeterminate", control, stringModel, counts, true, true,
                    withText ? "Y" : null, 0, 1, withText ? 0 : 1);
            control.setValue(null);
            verifyString(label + " setValue(null)", control, stringModel, counts, true, true, null, 0, 1, 1);
            Arrays.fill(counts, 0);
        });
        space(control);
        fx(() -> verifyString("String locked indeterminate Space, text constructor=" + withText,
                control, stringModel, counts, true, true, null, 0, 0, 0));
    }

    private void prepare(boolean initial) throws Exception {
        fx(() -> {
            box.setReadOnly(false);
            box.setSelected(initial);
            box.requestFocus();
            resetCounts();
        });
        Thread.sleep(100);
        fx(() -> check("focused before gesture", box.isFocused()));
    }

    private void space(CheckBox control) throws Exception {
        key(control, KeyEvent.KEY_PRESSED);
        key(control, KeyEvent.KEY_RELEASED);
    }

    private void key(CheckBox control, EventType<KeyEvent> type) throws Exception {
        fx(() -> {
            if (robot == null) {
                Event.fireEvent(control, new KeyEvent(type, "", "", KeyCode.SPACE, false, false, false, false));
            } else if (type == KeyEvent.KEY_PRESSED) {
                robot.keyPress(KeyCode.SPACE);
            } else {
                robot.keyRelease(KeyCode.SPACE);
            }
        });
        Thread.sleep(120);
    }

    private void click(CheckBox control) throws Exception {
        mouse(control, MouseEvent.MOUSE_PRESSED);
        mouse(control, MouseEvent.MOUSE_RELEASED);
    }

    private void mouse(CheckBox control, EventType<MouseEvent> type) throws Exception {
        fx(() -> {
            var bounds = control.getBoundsInLocal();
            double x = bounds.getCenterX();
            double y = bounds.getCenterY();
            boolean press = type == MouseEvent.MOUSE_PRESSED;
            if (robot == null) {
                var screen = control.localToScreen(x, y);
                var scene = control.localToScene(x, y);
                Event.fireEvent(control, new MouseEvent(type, scene.getX(), scene.getY(), screen.getX(),
                        screen.getY(), MouseButton.PRIMARY, 1, false, false, false, false, press,
                        false, false, false, false, true, new PickResult(control, scene.getX(), scene.getY())));
            } else if (press) {
                robot.mouseMove(control.localToScreen(x, y));
                robot.mousePress(MouseButton.PRIMARY);
            } else {
                robot.mouseRelease(MouseButton.PRIMARY);
            }
        });
        Thread.sleep(120);
    }

    private void resetCounts() {
        selectedChanges = modelChanges = actions = 0;
    }

    private void verify(String label, boolean value, int changes, int expectedActions) {
        check(label + " selected/model=" + box.isSelected() + "/" + model.get()
                        + " selected/model/action events=" + selectedChanges + "/" + modelChanges + "/" + actions,
                box.isSelected() == value && model.get() == value && selectedChanges == changes
                        && modelChanges == changes && actions == expectedActions);
    }

    private void verifyString(String label, JInvCheckBoxString control, SimpleStringProperty stringModel,
                              int[] counts, boolean selected, boolean indeterminate, String value,
                              int expectedSelected, int expectedIndeterminate, int expectedValue) {
        check(label + " selected/indeterminate/value/model=" + control.isSelected() + "/"
                        + control.isIndeterminate() + "/" + control.getValue() + "/" + stringModel.get()
                        + " selected/indeterminate/value/model events=" + Arrays.toString(counts),
                control.isSelected() == selected && control.isIndeterminate() == indeterminate
                        && Objects.equals(control.getValue(), value) && Objects.equals(stringModel.get(), value)
                        && counts[0] == expectedSelected && counts[1] == expectedIndeterminate
                        && counts[2] == expectedValue && counts[3] == expectedValue);
    }

    private void check(String label, boolean ok) {
        System.out.println((ok ? "PASS " : "FAIL ") + label);
        if (!ok) failures.add(label);
    }

    private static void fx(Runnable action) throws Exception {
        get(() -> {
            action.run();
            return null;
        });
    }

    private static <T> T get(Supplier<T> action) throws Exception {
        var task = new FutureTask<T>(action::get);
        Platform.runLater(task);
        return task.get(15, TimeUnit.SECONDS);
    }
}
