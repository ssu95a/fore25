import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Supplier;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.event.Event;
import javafx.scene.AccessibleAction;
import javafx.scene.Scene;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.PickResult;
import javafx.scene.layout.VBox;
import javafx.scene.robot.Robot;
import javafx.stage.Stage;
import ru.inversion.fx.form.controls.Controls;
import ru.inversion.fx.form.controls.JInvRadioButton;
import ru.inversion.fx.form.controls.JInvRadioGroup;

public final class RadioReadOnlySmoke {
    private final List<String> failures = new ArrayList<>();
    private final AtomicInteger changes = new AtomicInteger();
    private final boolean useRobot;
    private VBox root;
    private Stage stage;
    private Robot robot;
    private JInvRadioButton a;
    private JInvRadioButton b;
    private JInvRadioGroup group;
    private SimpleStringProperty model;

    private RadioReadOnlySmoke(boolean useRobot) {
        this.useRobot = useRobot;
    }

    public static void main(String[] args) throws Exception {
        Platform.startup(() -> {});
        var probe = new RadioReadOnlySmoke(args.length > 0 && args[0].equals("robot"));
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
                throw new AssertionError("CTRL-006 failures: " + probe.failures);
            }
            System.out.println("CTRL-006 PASS");
        } finally {
            Platform.exit();
        }
    }

    private void setup() {
        a = new JInvRadioButton("A");
        b = new JInvRadioButton("B");
        a.setValue("A");
        b.setValue("B");
        root = new VBox(12, a, b);
        group = new JInvRadioGroup("RADIO", root);
        a.setToggleGroup(group);
        b.setToggleGroup(group);
        group.setInitialized(true);
        group.setValue("A");
        model = new SimpleStringProperty("A");
        group.valueProperty().bindBidirectional(model);
        model.addListener((obs, old, value) -> changes.incrementAndGet());
        stage = new Stage();
        stage.setTitle("CTRL-006 radio readOnly");
        stage.setScene(new Scene(root, 300, 160));
        stage.show();
        stage.toFront();
        stage.requestFocus();
        if (useRobot) {
            robot = new Robot();
        }
    }

    private void run() throws Exception {
        gesture("unlocked Space", false, r -> {}, KeyCode.SPACE, false);
        gesture("unlocked click", false, r -> {}, null, true);
        gesture("unlocked Right", false, r -> {}, KeyCode.RIGHT, false);
        if (!failures.isEmpty()) {
            throw new AssertionError("Invalid unlocked positive control: " + failures);
        }
        checkLocked("both", r -> { group.setReadOnly(true); r.setReadOnly(true); });
        checkLocked("group only", r -> group.setReadOnly(true));
        checkLocked("radio only", r -> r.setReadOnly(true));
        checkLocked("release group", r -> { group.setReadOnly(true); r.setReadOnly(true); group.setReadOnly(false); });
        checkLocked("release radio", r -> { group.setReadOnly(true); r.setReadOnly(true); r.setReadOnly(false); });
        gesture("fully unlocked Space", false, r -> {
            r.setReadOnly(true); group.setReadOnly(true);
            r.setReadOnly(false); group.setReadOnly(false);
        }, KeyCode.SPACE, false);
        fx(this::programmatic);
        fx(this::groupLifecycle);
        mixedGroup();
    }

    private void checkLocked(String name, Consumer<JInvRadioButton> lock) throws Exception {
        gesture(name + " Space", true, lock, KeyCode.SPACE, false);
        gesture(name + " Right from A", true, lock, KeyCode.RIGHT, false);
        gesture(name + " click", true, lock, null, true);
        prepare(b, lock);
        fx(() -> b.executeAccessibleAction(AccessibleAction.FIRE));
        fx(() -> verify(name + " accessibility FIRE", a, 0));
    }

    private void gesture(String name, boolean locked, Consumer<JInvRadioButton> lock,
                         KeyCode key, boolean click) throws Exception {
        var target = key == KeyCode.RIGHT ? a : b;
        prepare(target, lock);
        if (get(() -> !target.isFocused()) && !click) {
            failures.add(name + " did not retain focus");
        }
        fx(() -> {
            if (click && useRobot) {
                var bounds = b.localToScreen(b.getBoundsInLocal());
                robot.mouseMove(bounds.getCenterX(), bounds.getCenterY());
                robot.mouseClick(MouseButton.PRIMARY);
            } else if (click) {
                targetedClick();
            } else if (useRobot) {
                robot.keyType(key);
            } else {
                Event.fireEvent(target, new KeyEvent(KeyEvent.KEY_PRESSED, "", "", key, false, false, false, false));
                Event.fireEvent(target, new KeyEvent(KeyEvent.KEY_RELEASED, "", "", key, false, false, false, false));
            }
        });
        if (useRobot) {
            Thread.sleep(180);
        }
        fx(() -> verify(name, locked ? a : b, locked ? 0 : 1));
    }

    private void targetedClick() {
        var center = b.localToScene(b.getWidth() / 2, b.getHeight() / 2);
        for (var type : List.of(MouseEvent.MOUSE_PRESSED, MouseEvent.MOUSE_RELEASED)) {
            Event.fireEvent(b, new MouseEvent(type, center.getX(), center.getY(), center.getX(), center.getY(),
                    MouseButton.PRIMARY, 1, false, false, false, false, type == MouseEvent.MOUSE_PRESSED,
                    false, false, false, false, true, new PickResult(b, center.getX(), center.getY())));
        }
    }

    private void prepare(JInvRadioButton target, Consumer<JInvRadioButton> lock) throws Exception {
        fx(() -> {
            group.setReadOnly(false);
            b.setReadOnly(false);
            group.selectToggle(a);
            target.requestFocus();
        });
        if (useRobot) {
            Thread.sleep(180);
        }
        fx(() -> { lock.accept(b); changes.set(0); });
    }

    private void programmatic() {
        group.setReadOnly(true);
        b.setReadOnly(true);
        program("selectToggle", () -> group.selectToggle(b));
        program("model write", () -> model.set("B"));
        program("group.setValue", () -> group.setValue("B"));
        program("direct selected write", () -> b.setSelected(true));
        program("explicit fire", b::fire);
    }

    private void program(String name, Runnable write) {
        group.selectToggle(a);
        changes.set(0);
        write.run();
        verify(name, b, 1);
    }

    private void groupLifecycle() {
        var other = new JInvRadioGroup("OTHER", root);
        b.setReadOnly(false);
        b.setToggleGroup(other);
        verifyFlags("reassign from locked group", false);
        group.setReadOnly(false);
        group.setReadOnly(true);
        verifyFlags("old group toggle", false);
        other.setReadOnly(true);
        verifyFlags("new group lock", true);
        b.setToggleGroup(null);
        verifyFlags("detached", false);
        other.setReadOnly(false);
        other.setReadOnly(true);
        verifyFlags("detached group toggle", false);
        b.setToggleGroup(new ToggleGroup());
        verifyFlags("plain group", false);
        b.setToggleGroup(group);
        verifyFlags("reattach to locked group", true);
        b.setReadOnly(true);
        b.setToggleGroup(null);
        verifyFlags("detach with own lock", true);
        root.setDisable(true);
        b.setReadOnly(false);
        check("native ancestor disabled", b.isDisabled()
                && b.getPseudoClassStates().contains(Controls.DISABLED_GREY_STATE));
        root.setDisable(false);
    }

    private void mixedGroup() throws Exception {
        var plain = get(ToggleGroup::new);
        var stock = get(() -> new RadioButton("Stock"));
        var legacy = get(() -> new JInvRadioButton("Legacy"));
        fx(() -> {
            root.getChildren().addAll(stock, legacy);
            legacy.setToggleGroup(plain);
            stock.setToggleGroup(plain);
            root.applyCss();
            check("mixed group skins installed", stock.getSkin() != null && legacy.getSkin() != null);
            plain.selectToggle(stock);
            stock.requestFocus();
            legacy.setReadOnly(true);
        });
        rightFrom(stock);
        fx(() -> check("mixed group blocks arrow into locked Legacy radio", plain.getSelectedToggle() == stock));
        fx(() -> {
            legacy.setToggleGroup(null);
            legacy.setToggleGroup(plain);
            plain.selectToggle(stock);
            stock.requestFocus();
        });
        rightFrom(stock);
        fx(() -> check("mixed group blocks after detach and reattach", plain.getSelectedToggle() == stock));
        fx(() -> { legacy.setReadOnly(false); plain.selectToggle(stock); stock.requestFocus(); });
        rightFrom(stock);
        fx(() -> check("mixed group unlocked arrow", plain.getSelectedToggle() == legacy));
        fx(() -> {
            var bulk = new ToggleGroup();
            var another = new JInvRadioButton("Another Legacy");
            root.getChildren().add(another);
            bulk.getToggles().addAll(stock, legacy, another);
            legacy.setReadOnly(true);
            bulk.selectToggle(stock);
            root.applyCss();
            stock.requestFocus();
        });
        rightFrom(stock);
        fx(() -> check("mixed group blocks after bulk attachment", stock.isSelected()));
    }

    private void rightFrom(RadioButton button) throws Exception {
        if (useRobot) {
            Thread.sleep(180);
            fx(() -> robot.keyType(KeyCode.RIGHT));
            Thread.sleep(180);
        } else {
            fx(() -> Event.fireEvent(button, new KeyEvent(KeyEvent.KEY_PRESSED, "", "", KeyCode.RIGHT,
                    false, false, false, false)));
        }
    }

    private void verifyFlags(String name, boolean locked) {
        check(name, b.isFocusTraversable() != locked && b.isMouseTransparent() == locked
                && b.getPseudoClassStates().contains(Controls.DISABLED_GREY_STATE) == locked);
    }

    private void verify(String name, JInvRadioButton expected, int count) {
        check(name + " selected=" + (group.getSelectedToggle() == b ? "B" : "A")
                + " group=" + group.getValue() + " model=" + model.get() + " changes=" + changes.get(),
                group.getSelectedToggle() == expected && expected.getValue().equals(group.getValue())
                && expected.getValue().equals(model.get()) && changes.get() == count);
    }

    private void check(String description, boolean condition) {
        System.out.println((condition ? "PASS " : "FAIL ") + description);
        if (!condition) {
            failures.add(description);
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
