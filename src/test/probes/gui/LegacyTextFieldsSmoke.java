import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
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

        ContextMenu own = text.getContextMenu();
        check(own != null && !own.getItems().isEmpty(), "skin: default context menu missing");
        text.setOnContextMenuRequested(event -> event.consume());
        check(text.getContextMenu() == null, "skin: menu did not yield to application handler");
        var custom = new ContextMenu(new MenuItem("Custom"));
        text.setContextMenu(custom);
        text.setOnContextMenuRequested(null);
        check(text.getContextMenu() == custom, "skin: custom context menu replaced");
        text.setContextMenu(null);
        check(text.getContextMenu() == own, "skin: own context menu not restored");

        observeConstructor(new JInvIntegerField("42"));
        observeConstructor(new JInvLongField("42"));
        observeConstructor(new JInvBigDecimalField("42"));
        observeConstructor(new JInvMoneyField("42"));
        checkNumber(integer, Integer.MIN_VALUE, "2147483647", Integer.MAX_VALUE, "2147483648");
        checkNumber(longField, Long.MIN_VALUE, "9223372036854775807", Long.MAX_VALUE, "9223372036854775808");
        decimal.setShowGroups(true);
        checkNumber(decimal, new BigDecimal("12345.675"), "12 345,685", new BigDecimal("12345.69"), "1,2.3");
        checkNumber(money, new BigDecimal("12345.675"), "12 345,685", new BigDecimal("12345.69"), "1,2.3");
        checkFocus(List.of(integer, longField, decimal, money), 0);
    }

    private void observeConstructor(JInvNumberField<?> field) {
        System.out.println("OBSERVED String constructor " + describe(field));
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
            stage.close();
            verdict.complete(List.copyOf(findings));
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
