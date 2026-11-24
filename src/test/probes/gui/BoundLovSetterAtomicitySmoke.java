import java.util.*;
import java.util.concurrent.*;
import java.util.function.*;
import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.scene.Scene;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import ru.inversion.fx.app.BaseApp;
import ru.inversion.fx.app.service.ViewPrefAppService;
import ru.inversion.fx.form.ViewContext;
import ru.inversion.fx.form.controls.JInvTextField;
import ru.inversion.fx.form.controls.skin.JInvTextFieldButtonSkin;
import ru.inversion.fx.form.lov.*;

public class BoundLovSetterAtomicitySmoke {
    static class App extends BaseApp {
        static void install() { g_app = new App(); }
        @Override public ViewPrefAppService getViewPrefService() { return new ViewPrefAppService(); }
    }
    static class Provider extends AbstractLovBase<String> {
        int opens;
        @Override public void showChoiceList(ViewContext view,String filter,BiConsumer<Boolean,ILov<String>> cb) {
            setValue("choice-" + ++opens);
            cb.accept(true,this);
        }
    }
    static void check(boolean ok,String label) { if (!ok) throw new AssertionError(label); }
    static void run() throws ReflectiveOperationException {
        App.install();
        var field=new JInvTextField();
        var model=new SimpleStringProperty("seed");
        field.textProperty().bindBidirectional(model);
        var events=new ArrayList<String>();
        model.addListener((p,o,n)->events.add(n));
        var stage=new Stage();
        stage.setScene(new Scene(new VBox(field)));
        stage.show();
        try {
            field.applyCss();
            var wrap=((JInvTextFieldButtonSkin)field.getSkin()).getButton();
            var unused = JInvTextField.class.getDeclaredField("lovValueListener");
            unused.setAccessible(true);
            check(unused.get(new JInvTextField()) == null, "unused field has no listener");
            boolean setPreserved = scenario(field, model, events, wrap, false);
            boolean unbindPreserved = scenario(field, model, events, wrap, true);
            check(setPreserved && unbindPreserved, "bound request lost active listener");
        } finally { stage.hide(); }
    }
    private static boolean scenario(JInvTextField field, SimpleStringProperty model, List<String> events,
                                    ru.inversion.fx.form.controls.JInvWrapButton wrap, boolean detach) {
        var old = new Provider();
        var next = new Provider();
        var providerChanges = new ArrayList<String>();
        var propertyChanges = new ArrayList<AbstractLovBase>();
        old.valueProperty().addListener((p,before,after) -> providerChanges.add(after));
        field.setLOV(old, true);
        old.setValue("initial");
        check(model.get().equals("initial"), "positive provider");
        var button = wrap.getInnerButton();
        var source = new SimpleObjectProperty<AbstractLovBase>(old);
        field.lovProperty().bind(source);
        javafx.beans.value.ChangeListener<AbstractLovBase> changes = (p,before,after) -> propertyChanges.add(after);
        field.lovProperty().addListener(changes);
        events.clear();
        var nativeFailure = rejects(() -> field.lovProperty().set(next));
        var actual = rejects(detach ? field::unbindLOV : () -> field.setLOV(next, false));
        check(actual.getClass() == nativeFailure.getClass() && actual.getMessage().equals(nativeFailure.getMessage()),
                "native exception preserved: " + actual);
        check(field.lovProperty().isBound() && field.getLOV() == old && wrap.getInnerButton() == button
                && model.get().equals("initial") && field.getText().equals("initial")
                && field.isValidateFromLOV() && propertyChanges.isEmpty() && events.isEmpty(), "failure preserves UI");
        old.setValue("after failure");
        boolean active = events.equals(List.of("after failure")) && model.get().equals("after failure");
        check(providerChanges.equals(List.of("initial","after failure")), "other provider listeners preserved");
        System.out.printf("%s: provider=%s model=%s sameButton=%s propertyEvents=%d%n",
                detach ? "unbind" : "replace", providerChanges, events, wrap.getInnerButton() == button, propertyChanges.size());

        field.lovProperty().removeListener(changes);
        field.lovProperty().unbind();
        events.clear();
        field.setLOV(next, false);
        field.setLOV(next, false);
        old.setValue("former");
        next.setValue("recovered");
        check(!field.isValidateFromLOV() && events.equals(List.of("recovered")), "replacement and reattachment");
        field.unbindLOV();
        next.setValue("detached");
        check(wrap.getInnerButton() == null && events.equals(List.of("recovered")), "unbound detach");
        return active;
    }

    private static RuntimeException rejects(Runnable action) {
        try { action.run(); }
        catch (RuntimeException failure) { return failure; }
        throw new AssertionError("expected bound-property exception");
    }

    public static void main(String[] args) throws Exception {
        var test=new FutureTask<Void>(() -> {run(); return null;});
        Platform.startup(() -> Platform.runLater(test));
        try { test.get(20,TimeUnit.SECONDS); System.out.println("CTRL-044 PASS"); } finally { Platform.exit(); }
    }
}
