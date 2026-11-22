import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import javafx.animation.AnimationTimer;
import javafx.application.Platform;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import ru.inversion.fx.form.controls.IStateControl.State;
import ru.inversion.fx.form.controls.JInvCustomValueField;
import ru.inversion.fx.form.controls.skin.JInvTextFieldButtonSkin;

public final class LegacyValueLifecycleSmoke {
    private static final class Custom implements JInvCustomValueField.ICustom<String> {
        private final String id;
        private final List<String> calls;
        private final Property<String> value = new SimpleObjectProperty<>();

        Custom(String id, List<String> calls) {
            this.id = id;
            this.calls = calls;
        }

        @Override
        @SuppressWarnings("unchecked")
        public <P extends Property<String>> P valueProperty() {
            return (P) value;
        }

        @Override public Class<String> getClassValue() {
            return String.class;
        }

        @Override public State setText(String text) {
            calls.add(id + ":" + text);
            value.setValue(id + ":" + text);
            return id.equals("A") ? State.VALUE : State.ERROR;
        }
    }

    private final CompletableFuture<List<String>> verdict = new CompletableFuture<>();
    private final List<String> failures = new ArrayList<>();
    private final List<String> calls = new ArrayList<>();
    private final Custom a = new Custom("A", calls);
    private final Custom b = new Custom("B", calls);
    private final SimpleObjectProperty<String> model = new SimpleObjectProperty<>();

    public static void main(String[] args) {
        var probe = new LegacyValueLifecycleSmoke();
        int exit = 2;
        try {
            require(Runtime.version().feature() == 25, "setup: Java 25 required");
            Platform.startup(() -> probe.checked(probe::show));
            var failures = probe.verdict.get(30, TimeUnit.SECONDS);
            failures.forEach(failure -> System.out.println("FINDING " + failure));
            System.out.println(failures.isEmpty() ? "VF-002 PASS (custom listener ownership)" : "VF-002 RED");
            exit = failures.isEmpty() ? 0 : 1;
        } catch (Throwable failure) {
            System.err.println("VF-002 incomplete: see setup or FX exception");
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
        var field = new JInvCustomValueField<String>("initial");
        var scene = new Scene(new StackPane(field), 420, 120);
        scene.getStylesheets().add(Objects.requireNonNull(getClass().getResource("/css/general.css"),
                "setup: production stylesheet missing").toExternalForm());
        var stage = new Stage();
        stage.setTitle("VF-002 Legacy custom value listener lifecycle");
        stage.setScene(scene);
        stage.show();
        afterPulse(() -> {
            require(stage.isShowing() && field.getSkin() instanceof JInvTextFieldButtonSkin
                            && field.getWidth() > 0 && field.getHeight() > 0,
                    "setup: laid-out visible Legacy field required");
            exercise(field);
            stage.close();
            verdict.complete(List.copyOf(failures));
        });
    }

    private void exercise(JInvCustomValueField<String> field) {
        field.setCustomImpl(a);
        var original = field.valueProperty();
        original.bindBidirectional(model);
        try {
            snapshot("initial install", field, original);
            check(calls.isEmpty() && "initial".equals(field.getText()) && field.getValue() == null
                    && field.getState() == State.NULL, "initial install unexpectedly processed existing text");
            change(field, original, "one", a, State.VALUE, List.of("A:one"));
            check("A:one".equals(model.get()), "first implementation did not update model");

            field.setCustomImpl(b);
            change(field, original, "two", b, State.ERROR, List.of("B:two"));
            System.out.println("BINDING replacement sameProperty=" + (field.valueProperty() == original)
                    + " field=" + field.getValue() + " original=" + original.getValue() + " model=" + model.get());

            model.set("model-only");
            snapshot("external model write while B active", field, original);
            field.setCustomImpl(b);
            change(field, original, "three", b, State.ERROR, List.of("B:three"));

            field.setCustomImpl(a);
            snapshot("reinstall A before edit", field, original);
            check(field.valueProperty() == original && "model-only".equals(field.getValue()),
                    "original property or model value lost on A reinstallation");
            change(field, original, "four", a, State.VALUE, List.of("A:four"));
            check("A:four".equals(model.get()), "reinstalled original did not update model");

            calls.clear();
            field.setCustomImpl(null);
            field.setText("detached");
            System.out.println("DETACH text=[" + field.getText() + "] calls=" + calls
                    + " state=" + field.getState() + " model=" + model.get());
            check(calls.isEmpty() && "detached".equals(field.getText()) && "A:four".equals(model.get())
                            && field.getState() == State.VALUE,
                    "detached implementation changed callbacks, state or model");
        } finally {
            original.unbindBidirectional(model);
        }
    }

    private void change(JInvCustomValueField<String> field, Property<String> original,
                        String text, Custom active, State expectedState, List<String> expectedCalls) {
        calls.clear();
        field.setText(text);
        snapshot("edit " + text, field, original);
        check(calls.equals(expectedCalls), text + " callbacks expected=" + expectedCalls + " actual=" + calls);
        check(field.valueProperty() == active.value && expectedCalls.getFirst().equals(field.getValue())
                && field.getState() == expectedState && text.equals(field.getText()),
                text + " active value, property or state mismatch");
    }

    private void snapshot(String phase, JInvCustomValueField<String> field, Property<String> original) {
        System.out.println(phase + " text=[" + field.getText() + "] calls=" + calls
                + " state=" + field.getState() + " field=" + field.getValue()
                + " A=" + a.value.getValue() + " B=" + b.value.getValue() + " model=" + model.get()
                + " originalProperty=" + (field.valueProperty() == original));
    }

    private void check(boolean condition, String diagnostic) {
        if (!condition) failures.add(diagnostic);
    }

    private void checked(Runnable action) {
        try {
            action.run();
        } catch (Throwable failure) {
            verdict.completeExceptionally(failure);
        }
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

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
