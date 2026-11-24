import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import ru.inversion.fx.app.BaseApp;
import ru.inversion.fx.app.service.ViewPrefAppService;
import ru.inversion.fx.form.ViewContext;
import ru.inversion.fx.form.controls.JInvTextField;
import ru.inversion.fx.form.lov.AbstractLovBase;
import ru.inversion.fx.form.lov.ILov;
import ru.inversion.fx.form.lov.JInvLOVButton;
import ru.inversion.fx.form.valid.validators.LovValidator;

public final class LovIntegrationSmoke {
    private static final class TestApp extends BaseApp {
        static void install() { g_app = new TestApp(); }
        static void reset() { g_app = null; }
        @Override public ViewPrefAppService getViewPrefService() { return new ViewPrefAppService(); }
    }

    private static final class Provider extends AbstractLovBase<String> {
        private final Stage stage;
        private final List<String> notifications;
        private String choice;
        private boolean accept;

        Provider(Stage stage, List<String> notifications, String name) {
            this.stage = stage;
            this.notifications = notifications;
            valueProperty().addListener((p, before, after) -> notifications.add(name + "=" + after));
        }

        @Override public boolean isSmallLov() { return false; }

        @Override public void showChoiceList(ViewContext view, String filter, BiConsumer<Boolean, ILov<String>> callback) {
            require(view.getStage() == stage && stage.isShowing(), "active owner Stage");
            notifications.add("show");
            if (accept) setValue(choice);
            callback.accept(accept, this);
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
            System.out.println("CTRL-031 PASS");
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
        var events = new ArrayList<String>();
        var errors = new ArrayList<Throwable>();
        var thread = Thread.currentThread();
        var errorHandler = thread.getUncaughtExceptionHandler();
        thread.setUncaughtExceptionHandler((t, error) -> errors.add(error));
        try {
            var field = new JInvTextField();
            var button = new JInvLOVButton(field);
            var model = new SimpleStringProperty("seed");
            field.textProperty().bindBidirectional(model);
            model.addListener((p, before, after) -> events.add("model=" + after));
            field.textProperty().addListener((p, before, after) -> events.add("text=" + after));
            field.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
                if (event.getCode() == KeyCode.TAB) {
                    events.add("forward=" + model.get());
                    event.consume();
                }
            });
            stage.setScene(new Scene(new VBox(field, button), 320, 130));
            stage.show();
            var former = new Provider(stage, events, "former");
            var active = new Provider(stage, events, "active");
            field.setLOV(former, true);
            require(events.isEmpty() && "seed".equals(model.get()), "initial attachment");
            choose(field, former, true, "first", model, events);
            expect(events, "show", "model=first", "text=first", "former=first", "callback=true/first");
            choose(field, former, false, "ignored", model, events);
            expect(events, "show", "callback=false/first");
            choose(field, former, true, new String("first"), model, events);
            expect(events, "show", "callback=true/first");

            field.setLOV(active, true);
            former.setValue("stale");
            expect(events, "former=stale");
            field.setLOV(active, true);
            field.setAppendFromLOV(true);
            field.setText(",");
            events.clear();
            choose(field, active, true, "x,y", model, events);
            String appended = model.get();
            require(appended.equals(",\"x,y\""), "comma quoting: " + appended);
            expect(events, "show", "model=" + appended, "text=" + appended, "active=x,y", "callback=true/" + appended);

            field.setAppendFromLOV(false);
            var validator = new LovValidator(field);
            require(validator.validateOnOk("checked") == null && "x,y".equals(active.getValue())
                    && appended.equals(model.get()) && events.isEmpty(), "read-only validation");
            require(validator.validate("checked") == null && "checked".equals(model.get()), "normal validation");
            expect(events, "model=checked", "text=checked", "active=checked");

            active.accept = true;
            active.choice = "button";
            button.fire();
            expect(events, "show", "model=button", "text=button", "active=button", "forward=button");
            active.accept = false;
            button.fire();
            expect(events, "show");
            field.unbindLOV();
            active.setValue("detached");
            expect(events, "active=detached");
            require(errors.isEmpty() && "button".equals(model.get()) && "button".equals(field.getText()),
                    "unbind errors=" + errors);
        } finally {
            stage.hide();
            thread.setUncaughtExceptionHandler(errorHandler);
            TestApp.reset();
        }
    }

    private static void choose(JInvTextField field, Provider provider, boolean accept, String value,
                               SimpleStringProperty model, List<String> events) {
        provider.accept = accept;
        provider.choice = value;
        field.showLOV((ok, source) -> {
            require(ok == accept && source == provider && model.get().equals(field.getText()), "callback context");
            events.add("callback=" + ok + "/" + model.get());
        });
    }

    private static void expect(List<String> events, String... expected) {
        require(events.equals(List.of(expected)), "expected " + List.of(expected) + ", actual " + events);
        System.out.println(events);
        events.clear();
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
