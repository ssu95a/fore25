import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.skin.TextFieldSkin;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import ru.inversion.fx.app.BaseApp;
import ru.inversion.fx.app.service.ViewPrefAppService;
import ru.inversion.fx.form.ViewContext;
import ru.inversion.fx.form.controls.*;
import ru.inversion.fx.form.controls.skin.JInvTextFieldButtonSkin;
import ru.inversion.fx.form.lov.*;

public final class TextFieldInnerButtonOwnershipSmoke {
    static class App extends BaseApp {
        static void install() { g_app = new App(); }
        @Override public ViewPrefAppService getViewPrefService() { return new ViewPrefAppService(); }
    }

    static class Provider extends AbstractLovBase<String> {
        int calls;
        @Override
        public void showChoiceList(ViewContext view, String filter, BiConsumer<Boolean, ILov<String>> done) {
            setValue("choice-" + ++calls);
            done.accept(true, this);
        }
    }

    static void require(boolean b, String name) {
        if (!b) throw new AssertionError(name);
    }

    static void tick() {
        var token = new Object();
        Platform.runLater(() -> Platform.exitNestedEventLoop(token, null));
        Platform.enterNestedEventLoop(token);
    }

    static JInvWrapButton wrapper(JInvTextField field) {
        return ((JInvTextFieldButtonSkin) field.getSkin()).getButton();
    }

    static void run(boolean baseline) {
        App.install();
        var field = new JInvTextField();
        var currency = new JInvTextFieldCUR();
        var original = wrapper(field);
        var realCurrency = wrapper(currency).getInnerButton();
        var provider = new Provider();
        field.setLOV(provider, false);
        var lov = original.getInnerButton();
        require(lov instanceof JInvLOVButton && lov.getTextField() == field, "initial LOV selection");
        field.setInnerButton(new JInvFEButton());
        require(original.getInnerButton() == lov, "LOV wins over FE");
        var root = new VBox(field, currency);
        var stage = new Stage();
        stage.setScene(new Scene(root, 430, 180));
        stage.show();
        try {
            tick();
            require(original.getOnAction() == lov.getOnAction(), "initial LOV action");
            original.fire();
            require(provider.calls == 1 && field.getText().equals("choice-1"), "initial action exact write");

            field.setSkin(null);
            field.setSkin(new TextFieldSkin(field));
            field.fireEvent(new KeyEvent(KeyEvent.KEY_RELEASED, "", "", KeyCode.F9, false, false, false, false));
            require(provider.calls == 2 && field.getText().equals("choice-2"), "F9 with custom skin");
            field.setSkin(null);
            field.setSkin(new JInvTextFieldButtonSkin(field));
            var replacement = wrapper(field);
            require(original.getInnerButton() == null && !original.onActionProperty().isBound()
                    && original.getOnAction() == null, "retired wrapper detached");
            require(replacement.getInnerButton() == (baseline ? null : lov), "replacement reattaches selected button");
            currency.setSkin(null);
            currency.setSkin(new JInvTextFieldButtonSkin(currency));
            require(wrapper(currency).getInnerButton() == (baseline ? null : realCurrency),
                    "currency functional button follows control");
            if (!baseline) {
                tick();
                replacement.fire();
                require(provider.calls == 3 && field.getText().equals("choice-3")
                        && lov.getProperties().get(Controls.CONTROL_PARENT) == replacement, "new wrapper exact action and ownership");
                field.setReadOnly(true);
                tick();
                require(!replacement.isVisible(), "read-only preserves LOV presentation rule");
                field.setReadOnly(false);
            }
            field.unbindLOV();
            var legacy = new JInvButton();
            int[] actions = {0};
            legacy.setOnAction(e -> actions[0]++);
            replacement.setInnerButton(legacy);
            tick();
            replacement.fire();
            require(actions[0] == 1 && provider.calls == (baseline ? 2 : 3), "legacy wrapper setter and independent action");
            field.setSkin(null);
            field.setSkin(new JInvTextFieldButtonSkin(field));
            tick();
            require(wrapper(field).getInnerButton() == (baseline ? null : legacy), "legacy selection reattaches");
            if (!baseline) {
                wrapper(field).fire();
                require(actions[0] == 2 && !replacement.onActionProperty().isBound(), "old wrapper action isolated");
                wrapper(field).setInnerButton(null);
                require(field.getConfiguredInnerButton() == null, "legacy clear reaches control");
            }
        } finally {
            stage.hide();
        }
    }

    public static void main(String[] args) throws Exception {
        var work = new FutureTask<Void>(() -> { run(args.length != 0 && args[0].equals("baseline")); return null; });
        Platform.startup(() -> { Platform.setImplicitExit(false); Platform.runLater(work); });
        try {
            work.get(40, TimeUnit.SECONDS);
            System.out.println("ARCH-003 PASS (control selection and wrapper lifecycle)");
        } finally {
            Platform.exit();
        }
    }
}
