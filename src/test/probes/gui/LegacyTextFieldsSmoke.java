import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import javafx.animation.AnimationTimer;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import ru.inversion.fx.form.controls.IStateControl.State;
import ru.inversion.fx.form.controls.JInvBigDecimalField;
import ru.inversion.fx.form.controls.JInvIntegerField;
import ru.inversion.fx.form.controls.JInvLongField;
import ru.inversion.fx.form.controls.JInvMoneyField;
import ru.inversion.fx.form.controls.JInvNumberField;
import ru.inversion.fx.form.controls.JInvPercentField;
import ru.inversion.fx.form.controls.JInvTextField;
import ru.inversion.fx.form.controls.skin.JInvTextFieldButtonSkin;

public final class LegacyTextFieldsSmoke {
    private final CompletableFuture<List<String>> verdict = new CompletableFuture<>();
    private final List<String> findings = new ArrayList<>();
    private Stage stage;
    private JInvTextField text;
    private JInvIntegerField integer;
    private JInvLongField longField;
    private JInvBigDecimalField decimal;
    private JInvMoneyField money;
    private boolean started;

    public static void main(String[] args) {
        var smoke = new LegacyTextFieldsSmoke();
        int exit = 2;
        try {
            require(Runtime.version().feature() == 25, "setup: Java 25 required");
            Platform.startup(() -> smoke.checked(smoke::show));
            var findings = smoke.verdict.get(30, TimeUnit.SECONDS);
            findings.forEach(finding -> System.out.println("FINDING " + finding));
            System.out.println(findings.isEmpty() ? "GUI-012 PASS (tested matrix)" : "GUI-012 RED (see findings)");
            exit = findings.isEmpty() ? 0 : 1;
        } catch (Throwable failure) {
            System.err.println("GUI-012 UNKNOWN: setup or runtime did not complete");
            failure.printStackTrace(System.err);
        } finally {
            Platform.exit();
        }
        System.exit(exit);
    }

    private void show() {
        Platform.setImplicitExit(false);
        Thread.currentThread().setUncaughtExceptionHandler((thread, failure) -> verdict.completeExceptionally(failure));
        String version = System.getProperty("javafx.version", "UNKNOWN");
        require(version.equals("25") || version.startsWith("25."), "setup: JavaFX 25 required: " + version);
        System.out.println("Java=" + Runtime.version() + " JavaFX=" + version);
        text = new JInvTextField();
        integer = new JInvIntegerField();
        longField = new JInvLongField();
        decimal = new JInvBigDecimalField();
        money = new JInvMoneyField();
        var scene = new Scene(new VBox(8, text, integer, longField, decimal, money), 440, 240);
        scene.getStylesheets().add(Objects.requireNonNull(getClass().getResource("/css/general.css"),
                "setup: production stylesheet missing").toExternalForm());
        stage = new Stage();
        stage.setTitle("GUI-012 Legacy text and numeric fields");
        stage.setScene(scene);
        stage.focusedProperty().addListener((property, before, focused) -> {
            if (focused && !started) afterPulse(() -> {
                if (stage.isFocused() && !started) {
                    started = true;
                    checkFields();
                }
            });
        });
        stage.show();
        stage.requestFocus();
    }

    private void checkFields() {
        for (var field : List.of(text, integer, longField, decimal, money)) {
            require(field.getSkin() instanceof JInvTextFieldButtonSkin && field.getWidth() > 0
                            && field.getHeight() > 0 && stage.isShowing(),
                    "layout: visible Legacy field/skin required for " + field.getClass().getSimpleName());
            check(empty(field.getText()), "default field has text: " + describe(field));
        }
        check(empty(new JInvTextField("").getText()) && "Alpha".equals(new JInvTextField("Alpha").getText()),
                "base: empty and normal String constructors");
        text.requestFocus();
        require(text.isFocused(), "focus: plain TextField did not acquire focus");
        text.setText("Alpha Beta");
        text.selectRange(6, 10);
        check("Beta".equals(text.getSelectedText()), "base: selected text mismatch");
        text.replaceSelection("Delta");
        check("Alpha Delta".equals(text.getText()), "base: replaceSelection lost text");
        text.setText(null);
        check(empty(text.getText()), "base: setText(null) unexpectedly produced text");

        check(text.getContextMenu() == null, "control: no framework context menu before a request");
        var anchor = text.localToScreen(5, 5);
        text.fireEvent(new javafx.scene.input.ContextMenuEvent(
                javafx.scene.input.ContextMenuEvent.CONTEXT_MENU_REQUESTED,
                5, 5, anchor.getX(), anchor.getY(), false, null));
        ContextMenu own = text.getContextMenu();
        check(own != null && own.isShowing() && !own.getItems().isEmpty(), "control: default context menu missing");
        own.hide();
        text.setOnContextMenuRequested(event -> event.consume());
        check(text.getContextMenu() == null, "skin: menu did not yield to application handler");
        var custom = new ContextMenu(new MenuItem("Custom"));
        text.setContextMenu(custom);
        text.setOnContextMenuRequested(null);
        check(text.getContextMenu() == custom, "skin: custom context menu replaced");
        text.setContextMenu(null);
        check(text.getContextMenu() == own, "skin: own context menu not restored");

        checkNumber(integer, Integer.MIN_VALUE, "2147483647", Integer.MAX_VALUE, "2147483648");
        checkNumber(longField, Long.MIN_VALUE, "9223372036854775807", Long.MAX_VALUE, "9223372036854775808");
        decimal.setShowGroups(true);
        checkNumber(decimal, new BigDecimal("12345.675"), "12 345,685", new BigDecimal("12345.69"), "1,2.3");
        checkNumber(money, new BigDecimal("12345.675"), "12 345,685", new BigDecimal("12345.69"), "1,2.3");
        checkFocus(List.of(integer, longField, decimal, money), 0);
    }

    private <T extends Number> void checkNumber(JInvNumberField<T> field, T assigned, String edit,
                                               T expected, String invalid) {
        String label = field.getClass().getSimpleName();
        check(field.getValue() == null && field.getState() == State.NULL, label + ": default typed state");
        var values = new ArrayList<T>();
        var changes = new ArrayList<String>();
        field.valueProperty().addListener((property, before, after) -> values.add(after));
        field.textProperty().addListener((property, before, after) -> changes.add(after));
        field.setValue(assigned);
        Number assignedExpected = assigned instanceof BigDecimal ? new BigDecimal("12345.68") : assigned;
        check(assignedExpected.equals(field.getValue()) && field.getState() == State.VALUE
                        && values.size() == 1 && changes.size() == 1,
                label + ": assignment/state or notification count " + describe(field));
        field.selectAll();
        field.replaceSelection(edit);
        check(expected.equals(field.getValue()) && field.getState() == State.VALUE
                        && values.size() == 2 && changes.size() == 2,
                label + ": valid edit/state or notification count " + describe(field));
        field.setText(invalid);
        check(expected.equals(field.getValue()) && field.getState() == State.ERROR
                        && values.size() == 2 && changes.size() == 3,
                label + ": invalid input altered value/state or notification count " + describe(field));
        field.setText(edit);
        check(expected.equals(field.getValue()) && field.getState() == State.VALUE
                        && values.size() == 2 && changes.size() == 4,
                label + ": same-value recovery or notification count " + describe(field));
        field.setText("");
        check(field.getValue() == null && field.getState() == State.NULL
                        && values.size() == 3 && changes.size() == 5,
                label + ": clearing did not produce null/NULL exactly once " + describe(field));
        field.setText(edit);
        check(expected.equals(field.getValue()) && field.getState() == State.VALUE
                        && values.size() == 4 && changes.size() == 6,
                label + ": final valid edit could not recover " + describe(field));
        System.out.println("NUMERIC " + describe(field) + " valueEvents=" + values.size() + " textEvents=" + changes.size());
    }

    private void checkFocus(List<JInvNumberField<?>> fields, int index) {
        if (index == fields.size()) {
            checkConstructors();
            return;
        }
        var field = fields.get(index);
        field.requestFocus();
        afterPulse(() -> {
            require(field.isFocused(), "focus: numeric field did not acquire focus: " + describe(field));
            var previous = field.getValue();
            var valueEvents = new ArrayList<Number>();
            field.valueProperty().addListener((property, before, after) -> valueEvents.add(after));
            text.requestFocus();
            afterPulse(() -> {
                require(text.isFocused() && !field.isFocused(), "focus: numeric field did not lose focus");
                check(Objects.equals(previous, field.getValue()) && field.getState() == State.VALUE
                                && valueEvents.isEmpty(),
                        "focus: typed value/state mutated " + describe(field));
                System.out.println("FOCUS " + describe(field) + " valueEvents=" + valueEvents.size());
                checkFocus(fields, index + 1);
            });
        });
    }

    private void checkConstructors() {
        var decimal42 = new BigDecimal("42.00");
        var genericEdited = new JInvNumberField<Number>();
        genericEdited.setText("42");
        var generic = new JInvNumberField<Number>("42");
        check(genericEdited.getState() == State.VALUE && generic.getState() == State.VALUE
                        && Objects.equals(genericEdited.getValue(), generic.getValue()),
                "constructor: generic Number conversion differs from an edit " + describe(generic));
        List<JInvNumberField<?>> fields = List.of(
                checkConstructor(JInvIntegerField::new, 42, 42),
                checkConstructor(JInvLongField::new, 42L, 42L),
                checkConstructor(JInvBigDecimalField::new, decimal42, new BigDecimal("42.56")),
                checkConstructor(JInvMoneyField::new, decimal42, new BigDecimal("42.56")),
                checkConstructor(JInvPercentField::new, decimal42, new BigDecimal("42.56")));
        check(new JInvIntegerField(42).getValue().equals(42)
                        && new JInvLongField(42L).getValue().equals(42L),
                "constructor: typed integer or long value changed");
        for (var field : List.of(new JInvBigDecimalField(new BigDecimal("42.555")),
                new JInvPercentField(new BigDecimal("42.555")))) {
            check(new BigDecimal("42.56").equals(field.getValue()) && field.getState() == State.VALUE,
                    "constructor: typed decimal rounding/state " + describe(field));
        }
        var root = (VBox) stage.getScene().getRoot();
        root.getChildren().setAll(text);
        root.getChildren().addAll(fields);
        checkConstructorFocus(fields, 0);
    }

    private JInvNumberField<?> checkConstructor(Function<String, ? extends JInvNumberField<?>> create,
                                                Number expected, Number precision) {
        var valid = create.apply("42");
        var fractional = create.apply(precision instanceof BigDecimal ? "42.555" : "42");
        var invalid = create.apply("invalid");
        var missing = create.apply(null);
        var emptyText = create.apply("");
        System.out.println("CONSTRUCTOR initial " + describe(valid) + " invalid=" + describe(invalid));
        check("42".equals(valid.getText()) && expected.equals(valid.getValue()) && valid.getState() == State.VALUE,
                "constructor: initial valid text/value mismatch " + describe(valid));
        check(precision.equals(fractional.getValue()) && fractional.getState() == State.VALUE,
                "constructor: initial precision/state " + describe(fractional));
        check("invalid".equals(invalid.getText()) && invalid.getValue() == null && invalid.getState() == State.ERROR,
                "constructor: invalid initial text did not report ERROR " + describe(invalid));
        for (var field : List.of(missing, emptyText)) {
            check(empty(field.getText()) && field.getValue() == null && field.getState() == State.NULL,
                    "constructor: null or empty did not preserve NULL " + describe(field));
        }
        return valid;
    }

    private void checkConstructorFocus(List<JInvNumberField<?>> fields, int index) {
        if (index == fields.size()) {
            stage.close();
            verdict.complete(List.copyOf(findings));
            return;
        }
        var field = fields.get(index);
        var initial = field.getValue();
        var valueEvents = new ArrayList<Number>();
        field.valueProperty().addListener((property, before, after) -> valueEvents.add(after));
        field.requestFocus();
        afterPulse(() -> {
            require(field.isFocused(), "focus: constructor field did not acquire focus " + describe(field));
            text.requestFocus();
            afterPulse(() -> {
                require(text.isFocused() && !field.isFocused(), "focus: constructor field did not lose focus");
                System.out.println("CONSTRUCTOR after focus " + describe(field) + " valueEvents=" + valueEvents.size());
                check(initial != null && initial.equals(field.getValue()) && field.getState() == State.VALUE
                                && !empty(field.getText()) && valueEvents.isEmpty(),
                        "constructor: untouched focus loss did not preserve number " + describe(field));
                checkConstructorFocus(fields, index + 1);
            });
        });
    }

    private static boolean empty(String value) {
        return value == null || value.isEmpty();
    }

    private static String describe(JInvTextField field) {
        return field.getClass().getSimpleName() + " text=[" + field.getText() + "]"
                + (field instanceof JInvNumberField<?> number ? " value=" + number.getValue() : "")
                + " state=" + field.getState();
    }

    private void afterPulse(Runnable action) {
        new AnimationTimer() {
            private boolean first = true;
            @Override public void handle(long now) {
                if (first) {
                    first = false;
                    return;
                }
                stop();
                checked(action);
            }
        }.start();
    }

    private void checked(Runnable action) {
        if (verdict.isDone()) return;
        try {
            action.run();
        } catch (Throwable failure) {
            verdict.completeExceptionally(failure);
        }
    }

    private void check(boolean condition, String message) {
        if (!condition) findings.add(message);
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
