import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import javafx.animation.AnimationTimer;
import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import ru.inversion.fx.form.controls.IStateControl.State;
import ru.inversion.fx.form.controls.JInvIntegerField;
import ru.inversion.fx.form.controls.JInvMoneyField;
import ru.inversion.fx.form.controls.JInvNumberField;
import ru.inversion.fx.form.controls.skin.JInvTextFieldButtonSkin;

public final class LegacyValueFieldsSmoke {
    private record Change<T>(T before, T after) {}

    private static final class Binding<T extends Number> {
        final JInvNumberField<T> field;
        final SimpleObjectProperty<T> model = new SimpleObjectProperty<>();
        final List<Change<T>> fieldChanges = new ArrayList<>();
        final List<Change<T>> modelChanges = new ArrayList<>();
        final List<String> drafts = new ArrayList<>();

        Binding(JInvNumberField<T> field) {
            this.field = field;
            field.valueProperty().bindBidirectional(model);
            field.valueProperty().addListener((property, before, after) -> fieldChanges.add(new Change<>(before, after)));
            model.addListener((property, before, after) -> modelChanges.add(new Change<>(before, after)));
            field.textProperty().addListener((property, before, after) -> drafts.add(after));
        }

        void expect(T value, State state, String step) {
            require(Objects.equals(value, field.getValue()) && Objects.equals(value, model.get())
                            && field.getState() == state,
                    step + ": text=[" + field.getText() + "] field=" + field.getValue() + " model=" + model.get()
                            + " state=" + field.getState());
        }
    }

    private final CompletableFuture<List<String>> verdict = new CompletableFuture<>();
    private final List<String> clipboardFailures = new ArrayList<>();
    private Stage stage;
    private Button focusSink;
    private Binding<Integer> integer;
    private Binding<BigDecimal> money;
    private boolean started;

    public static void main(String[] args) {
        var smoke = new LegacyValueFieldsSmoke();
        int exit = 2;
        try {
            require(Runtime.version().feature() == 25, "setup: Java 25 required");
            Platform.startup(() -> smoke.checked(smoke::show));
            var failures = smoke.verdict.get(30, TimeUnit.SECONDS);
            failures.forEach(failure -> System.out.println("FINDING " + failure));
            System.out.println(failures.isEmpty() ? "VF-001 PASS" : "VF-001 RED");
            exit = failures.isEmpty() ? 0 : 1;
        } catch (Throwable failure) {
            System.err.println("VF-001 incomplete: see setup, focus or typed binding assertion");
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
        require(version.equals("25") || version.startsWith("25."), "setup: JavaFX 25 required");
        System.out.println("Java=" + Runtime.version() + " JavaFX=" + version);
        integer = new Binding<>(new JInvIntegerField());
        money = new Binding<>(new JInvMoneyField());
        focusSink = new Button("Move focus");
        var scene = new Scene(new VBox(8, integer.field, money.field, focusSink), 440, 170);
        scene.getStylesheets().add(Objects.requireNonNull(getClass().getResource("/css/general.css"),
                "setup: production stylesheet missing").toExternalForm());
        stage = new Stage();
        stage.setTitle("VF-001 Legacy typed value and clipboard");
        stage.setScene(scene);
        stage.focusedProperty().addListener((property, before, focused) -> {
            if (focused && !started) afterPulse(() -> {
                if (stage.isFocused() && !started) {
                    started = true;
                    checkBindings();
                }
            });
        });
        stage.show();
        stage.requestFocus();
    }

    private void checkBindings() {
        require(stage.isShowing() && integer.field.getSkin() instanceof JInvTextFieldButtonSkin
                        && money.field.getSkin() instanceof JInvTextFieldButtonSkin
                        && integer.field.getWidth() > 0 && money.field.getWidth() > 0,
                "setup: visible Legacy fields and skins required");
        checkEdits(integer, 41, "42", 42, "bad integer", "43", 43, null);
        checkEdits(money, new BigDecimal("12345.60"), "12 346,70", new BigDecimal("12346.70"),
                "bad money", "12 347,80", new BigDecimal("12347.80"), "12346");
        checkFocus(integer, () -> checkFocus(money, () -> {
            integer.field.valueProperty().unbindBidirectional(integer.model);
            money.field.valueProperty().unbindBidirectional(money.model);
            stage.close();
            verdict.complete(List.copyOf(clipboardFailures));
        }));
    }

    private <T extends Number> void checkEdits(Binding<T> binding, T initial, String validText, T valid,
                                                String invalid, String recoveredText, T recovered, String partial) {
        var field = binding.field;
        String label = field.getClass().getSimpleName();
        binding.expect(null, State.NULL, label + " initial");
        expectCopy(field, label + " empty", null);
        binding.model.set(initial);
        binding.expect(initial, State.VALUE, label + " model assignment");
        require(field.getText() != null && !field.getText().isEmpty(), label + ": assigned value not rendered");
        int formattingChanges = binding.drafts.size();
        require(formattingChanges == 1, label + ": expected exactly one text change from model assignment");

        field.selectAll();
        field.replaceSelection(validText);
        binding.expect(valid, State.VALUE, label + " valid edit");
        require(validText.equals(field.getText()), label + ": valid draft was replaced");
        field.selectAll();
        expectCopy(field, label + " valid full", valid.toString());
        if (partial != null) {
            field.selectRange(0, validText.indexOf(','));
            expectCopy(field, label + " grouped partial", partial);
        }

        field.setText(invalid);
        binding.expect(valid, State.ERROR, label + " invalid edit");
        require(invalid.equals(field.getText()), label + ": invalid draft not visible");
        expectChanges(binding, List.of(new Change<>(null, initial), new Change<>(initial, valid)), label + " invalid");
        field.selectAll();
        expectCopy(field, label + " invalid full", invalid);
        field.selectRange(0, 3);
        expectCopy(field, label + " invalid partial", "bad");

        field.setText(recoveredText);
        binding.expect(recovered, State.VALUE, label + " recovery");
        field.setText("");
        binding.expect(null, State.NULL, label + " clear");
        expectCopy(field, label + " cleared", null);
        field.setText(recoveredText);
        binding.expect(recovered, State.VALUE, label + " recovery after clear");
        expectChanges(binding, List.of(new Change<>(null, initial), new Change<>(initial, valid),
                new Change<>(valid, recovered), new Change<>(recovered, null), new Change<>(null, recovered)), label);
        require(binding.drafts.subList(formattingChanges, binding.drafts.size())
                        .equals(List.of(validText, invalid, recoveredText, "", recoveredText)),
                label + ": unexpected draft notifications " + binding.drafts);
        System.out.println("BINDING " + label + " field/model=" + binding.modelChanges + " textEvents=" + binding.drafts.size());
    }

    private static <T extends Number> void expectChanges(Binding<T> binding, List<Change<T>> expected, String label) {
        require(binding.fieldChanges.equals(expected) && binding.modelChanges.equals(expected),
                label + ": field=" + binding.fieldChanges + " model=" + binding.modelChanges + " expected=" + expected);
    }

    private void expectCopy(JInvNumberField<?> field, String label, String expected) {
        String actual = field.getClipboardContent().getString();
        System.out.println("COPY " + label + " expected=[" + expected + "] actual=[" + actual + "]");
        if (!Objects.equals(expected, actual)) {
            clipboardFailures.add(label + " expected=[" + expected + "] actual=[" + actual + "]");
        }
    }

    private <T extends Number> void checkFocus(Binding<T> binding, Runnable next) {
        var field = binding.field;
        T before = binding.model.get();
        int fieldEvents = binding.fieldChanges.size();
        int modelEvents = binding.modelChanges.size();
        int textEvents = binding.drafts.size();
        field.requestFocus();
        afterPulse(() -> {
            require(field.isFocused(), "focus: numeric field did not acquire focus");
            focusSink.requestFocus();
            afterPulse(() -> {
                require(focusSink.isFocused() && !field.isFocused(), "focus: numeric field did not lose focus");
                binding.expect(before, State.VALUE, "focus loss");
                require(binding.fieldChanges.size() == fieldEvents && binding.modelChanges.size() == modelEvents
                                && binding.drafts.size() - textEvents <= 1,
                        "focus: unexpected typed, model or text notifications");
                System.out.println("FOCUS " + field.getClass().getSimpleName() + " value=" + before
                        + " fieldEvents=0 modelEvents=0 textEvents=" + (binding.drafts.size() - textEvents));
                next.run();
            });
        });
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

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
