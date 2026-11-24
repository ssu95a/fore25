import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.scene.Scene;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import ru.inversion.fx.app.BaseApp;
import ru.inversion.fx.app.service.ViewPrefAppService;
import ru.inversion.fx.form.ViewContext;
import ru.inversion.fx.form.controls.JInvTextField;
import ru.inversion.fx.form.lov.AbstractLovBase;
import ru.inversion.fx.form.lov.ILov;
import ru.inversion.fx.form.valid.validators.LovValidator;

public final class LovDefaultValidationSmoke {
    private static final class TestApp extends BaseApp {
        static void install() { g_app = new TestApp(); }
        static void reset() { g_app = null; }
        @Override public ViewPrefAppService getViewPrefService() { return new ViewPrefAppService(); }
    }

    private static final class Provider extends AbstractLovBase<String> {
        @Override public void showChoiceList(ViewContext view, String text, BiConsumer<Boolean, ILov<String>> callback) {
            throw new AssertionError("Validation opened a choice list");
        }
    }

    public static void main(String[] args) {
        var test = new FutureTask<Void>(() -> {
            run();
            return null;
        });
        Platform.startup(() -> { Platform.setImplicitExit(false); Platform.runLater(test); });
        int exit = 1;
        try {
            test.get(20, TimeUnit.SECONDS);
            System.out.println("CTRL-027 PASS");
            exit = 0;
        } catch (Exception error) {
            error.printStackTrace(System.err);
        } finally {
            Platform.exit();
        }
        System.exit(exit);
    }

    private static void run() {
        require(Runtime.version().feature() == 25
                && System.getProperty("javafx.version", "").matches("25(?:\\..*)?"), "Java/JavaFX 25");
        TestApp.install();
        var stage = new Stage();
        try {
            var field = new JInvTextField();
            stage.setScene(new Scene(new VBox(field)));
            stage.show();
            var model = new SimpleStringProperty("seed");
            field.textProperty().bindBidirectional(model);
            var provider = new Provider();
            field.setLOV(provider, true);
            var validator = new LovValidator(field);
            var events = new ArrayList<String>();
            provider.valueProperty().addListener((p, a, b) -> events.add("lov"));
            field.textProperty().addListener((p, a, b) -> events.add("text"));
            model.addListener((p, a, b) -> events.add("model"));
            require(provider.checkValue("direct", true) && "direct".equals(model.get()), "direct true");
            assertWrites(events);
            events.clear();
            require(validator.validate("normal") == null && "normal".equals(model.get()), "validator true");
            assertWrites(events);
            events.clear();
            require(provider.checkValue(new String("normal"), true)
                    && validator.validateOnOk(new String("normal")) == null && events.isEmpty(), "equal no-op");

            var failures = new ArrayList<String>();
            for (boolean direct : new boolean[]{true, false}) {
                for (String candidate : new String[]{"different", null}) {
                    provider.setValue("normal");
                    events.clear();
                    boolean valid = direct ? provider.checkValue(candidate, false)
                            : validator.validateOnOk(candidate) == null;
                    String route = (direct ? "direct" : "validateOnOk") + " candidate=" + candidate;
                    System.out.printf("%s lov=%s text=%s model=%s events=%s%n",
                            route, provider.getValue(), field.getText(), model.get(), events);
                    if (!valid || !events.isEmpty() || !"normal".equals(provider.getValue())
                            || !"normal".equals(field.getText()) || !"normal".equals(model.get())) {
                        failures.add(route);
                    }
                }
            }

            var next = new Provider();
            field.setLOV(next, true);
            events.clear();
            provider.setValue("old");
            require(events.equals(List.of("lov")), "previous provider updates only its own listeners");
            require(validator.validate("next") == null && Objects.equals("next", model.get()), "validator uses new LOV");
            field.unbindLOV();
            events.clear();
            next.setValue("detached");
            require(events.isEmpty() && "next".equals(model.get()), "detachment");
            require(failures.isEmpty(), "false validation mutated state: " + failures);
        } finally {
            stage.hide();
            TestApp.reset();
        }
    }

    private static void assertWrites(List<String> events) {
        require(events.size() == 3 && events.containsAll(List.of("lov", "text", "model")), "one write each " + events);
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
