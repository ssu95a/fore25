import java.util.*;
import java.util.concurrent.*;
import java.util.function.*;
import javafx.application.Platform;
import javafx.animation.AnimationTimer;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import ru.inversion.fx.app.BaseApp;
import ru.inversion.fx.app.service.ViewPrefAppService;
import ru.inversion.fx.form.*;
import ru.inversion.fx.form.controls.*;
import ru.inversion.fx.form.lov.*;
import ru.inversion.fx.form.valid.*;
import ru.inversion.fx.form.valid.validators.LovValidator;

public class ValidatorMultiControlSmoke {
    static class App extends BaseApp {
        static void install() { g_app = new App(); }
        @Override public ViewPrefAppService getViewPrefService() { return new ViewPrefAppService() {
            @Override public boolean isShowValidatable() { return true; }
        }; }
    }
    static class Manager extends ValidMan {
        int focusDispatch;
        Manager(AbstractBaseController<?> ctrl) { super(ctrl); }
        @Override public void validateControl(Control c) {
            focusDispatch++;
            super.validateControl(c);
        }
    }
    static class Controller extends JInvFXFormController<Object> {
        final Manager manager = new Manager(this);
        @Override public ValidMan getValidMan() { return manager; }
    }
    static class Provider extends AbstractLovBase<String> {
        @Override public void showChoiceList(ViewContext view, String filter, BiConsumer<Boolean,ILov<String>> cb) {
            throw new AssertionError();
        }
    }
    static void check(boolean ok, String detail) { if (!ok) throw new AssertionError(detail); }
    static void blur(JInvTextField field, Button next, String text) {
        field.requestFocus();
        check(field.isFocused(),"focus field");
        field.setText(text);
        check(ValidMan.isControlValueChanged(field),"text listener marks dirty");
        next.requestFocus();
        check(next.isFocused(),"focus next");
    }
    static void run(Stage stage, JInvTextField field, JInvTextField other, Button next) {
        var ctrl = new Controller();
        ctrl.setViewContext(ViewContext.of(stage));
        ctrl.lifeCycleStateProperty().set(LifeCycleStateEnum.RUNTIME);
        stage.getScene().getRoot().getProperties().put(AbstractBaseController.PROPERTY_CONTROLLER,new SimpleObjectProperty<>(ctrl));
        var first = ctrl.manager;
        int[] hits = new int[2];
        var modelEvents = new ArrayList<String>();
        var model = new SimpleStringProperty("seed");
        var otherModel = new SimpleStringProperty("other");
        field.textProperty().bindBidirectional(model);
        other.textProperty().bindBidirectional(otherModel);
        model.addListener((p, old, value) -> modelEvents.add("A:" + value));
        otherModel.addListener((p, old, value) -> modelEvents.add("B:" + value));
        Validator<String> shared = value -> { hits[0]++; return null; };
        Validator<String> distinct = value -> { hits[1]++; return null; };
        first.bindValidators2Control(field, shared, shared);
        first.bindValidators2Control(other, shared, distinct);

        blur(field, next, "duplicates");
        check(hits[0] == 2 && hits[1] == 0, "two same-field registrations, one blur");
        first.unbindValidators(field, shared);
        blur(other, next, "B retained");
        check(hits[0] == 3 && hits[1] == 1, "A unbind preserves both B validators");
        first.bindValidators2Control(field, shared);
        first.unbindValidators(other, shared);
        blur(field, next, "A retained");
        blur(other, next, "B distinct");
        check(hits[0] == 4 && hits[1] == 2, "B unbind preserves A shared and B distinct");
        first.unbindValidators(field, shared);
        first.unbindValidators(other, distinct);
        blur(field, next, "unbound");
        check(hits[0] == 4 && hits[1] == 2 && first.getValidators(field).isEmpty()
                && first.getValidators(other).isEmpty(), "unbound no stale validator calls");

        var old = new Provider();
        field.setLOV(old,true);
        var active = new Provider();
        field.setLOV(active,true);
        field.setLOV(active,true);
        check(first.getValidators(field).stream().filter(LovValidator.class::isInstance).count()==1,"one LOV validator");
        old.setValue("stale");
        check(model.get().equals("unbound"),"no stale LOV write");
        blur(field,next,"lov validation");
        check("lov validation".equals(active.getValue()),"active provider normal validation");
        var v=(LovValidator)first.getValidators(field).stream().filter(LovValidator.class::isInstance).findFirst().orElseThrow();
        v.validateOnOk("read only");
        check(model.get().equals("lov validation") && active.getValue().equals("lov validation"),"default no mutation on OK");
        check(modelEvents.equals(List.of("A:duplicates", "B:B retained", "A:A retained",
                "B:B distinct", "A:unbound", "A:lov validation")), "one model event per edit " + modelEvents);
        check(first.focusDispatch == 6, "one dispatch per blur");
        field.unbindLOV();
        active.setValue("detached");
        check(model.get().equals("lov validation") && modelEvents.size() == 6, "no stale LOV update");
        System.out.printf("validators=%s blur dispatches=%d model events=%s%n",
                Arrays.toString(hits), first.focusDispatch, modelEvents);
    }
    public static void main(String[] args) throws Exception {
        var done = new CompletableFuture<Void>();
        Platform.startup(() -> {
            try {
                App.install();
                var stage = new Stage();
                var field = new JInvTextField();
                var other = new JInvTextField();

                var next = new Button("Next");
                stage.setScene(new Scene(new VBox(field, other, next),320,180));
                stage.show();
                new AnimationTimer() {
                    @Override public void handle(long time) {
                        if (!stage.isFocused()) return;
                        stop();
                        Platform.runLater(() -> {
                            try { run(stage, field, other, next); done.complete(null); }
                            catch (Throwable error) { done.completeExceptionally(error); }
                            finally { stage.hide(); }
                        });
                    }
                }.start();
            } catch (Throwable error) { done.completeExceptionally(error); }
        });
        int code=1;
        try { done.get(20,TimeUnit.SECONDS); System.out.println("CTRL-036 PASS"); code=0; }
        catch (Exception failure) { failure.printStackTrace(); }
        finally { Platform.exit(); }
        System.exit(code);
    }
}