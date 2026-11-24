import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.scene.Scene;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import ru.inversion.fx.app.BaseApp;
import ru.inversion.fx.app.service.ViewPrefAppService;
import ru.inversion.fx.form.ViewContext;
import ru.inversion.fx.form.controls.Controls;
import ru.inversion.fx.form.controls.JInvButton;
import ru.inversion.fx.form.controls.JInvTextField;
import ru.inversion.fx.form.controls.JInvWrapButton;
import ru.inversion.fx.form.controls.skin.JInvTextFieldButtonSkin;
import ru.inversion.fx.form.lov.AbstractLovBase;
import ru.inversion.fx.form.lov.ILov;
import ru.inversion.fx.form.lov.JInvLOVButton;

public final class LovBindingInterleavingSmoke {
    private static final class App extends BaseApp {
        static void install() { g_app = new App(); }
        @Override public ViewPrefAppService getViewPrefService() { return new ViewPrefAppService(); }
    }

    private static final class Provider extends AbstractLovBase<String> {
        int choices;
        @Override public void showChoiceList(ViewContext view, String text, BiConsumer<Boolean, ILov<String>> callback) {
            setValue("choice-" + ++choices);
            callback.accept(true, this);
        }
    }

    private record Row(JInvTextField field, JInvWrapButton wrap, List<String> model) {
        Row(VBox root) {
            this(new JInvTextField(), root);
        }

        private Row(JInvTextField field, VBox root) {
            this(field, install(root, field), new ArrayList<>());
            var value = new SimpleStringProperty("seed");
            field.textProperty().bindBidirectional(value);
            value.addListener((p, before, after) -> model.add(after));
        }

        private static JInvWrapButton install(VBox root, JInvTextField field) {
            root.getChildren().add(field);
            field.applyCss();
            return ((JInvTextFieldButtonSkin) field.getSkin()).getButton();
        }

        JInvButton set(Provider provider) {
            field.setLOV(provider, true);
            return wrap.getInnerButton();
        }

        void verifyDetached() {
            require(wrap.getInnerButton() == null && !wrap.isVisible() && !wrap.onActionProperty().isBound()
                    && !wrap.tooltipProperty().isBound() && !wrap.visibleProperty().isBound()
                    && !wrap.disableProperty().isBound() && model.isEmpty(), "detached wrapper and model");
        }

        void verifyActive(JInvButton current, JInvButton former) {
            require(wrap.getInnerButton() == current && wrap.getOnAction() == current.getOnAction()
                    && wrap.getTooltip() == current.getTooltip() && wrap.isVisible() && !wrap.isDisabled()
                    && wrap.onActionProperty().isBound() && wrap.tooltipProperty().isBound()
                    && wrap.visibleProperty().isBound() && wrap.disableProperty().isBound(), "active wrapper");
            require(current.getProperties().get(Controls.CONTROL_PARENT) == wrap
                    && !former.getProperties().containsKey(Controls.CONTROL_PARENT), "wrapper ownership");
            former.setText("obsolete");
            require(!"obsolete".equals(wrap.getText()), "wrapper detached from former text");
        }
    }

    private static void test(CompletableFuture<Void> done) {
        App.install();
        var stage = new Stage();
        var root = new VBox();
        stage.setScene(new Scene(root, 380, 220));
        stage.show();

        var first = new Row(root);
        var a = new Provider();
        first.set(a);
        first.field.unbindLOV();
        a.setValue("detached");
        var second = new Row(root);
        var b = new Provider();
        var c = new Provider();
        second.set(b);
        second.set(c);
        second.field.unbindLOV();
        b.setValue("detached");
        c.setValue("detached");

        var row = new Row(root);
        var old = new Provider();
        var current = new Provider();
        row.set(old);
        row.field.unbindLOV();
        JInvButton former = row.set(current);
        JInvButton active = row.set(current);
        old.setValue("obsolete");
        current.setValue("current");
        current.setValue(new String("current"));
        require(row.model.equals(List.of("current")), "one write, equal value silent");

        later(done, () -> {
            first.verifyDetached();
            second.verifyDetached();
            row.verifyActive(active, former);
            row.field.setReadOnly(true);
            require(row.wrap.isDisabled() && !row.wrap.isVisible(), "read-only wrapper");
            row.wrap.fire();
            require(current.choices == 0, "read-only blocks button action");
            row.field.setReadOnly(false);
            row.field.setEditable(false);
            require(row.wrap.isDisabled() && !row.wrap.isVisible(), "non-editable wrapper");
            row.field.setEditable(true);
            var source = new SimpleObjectProperty<AbstractLovBase>(current);
            row.field.lovProperty().bind(source);
            rejects(() -> row.field.setLOV(old, false));
            rejects(row.field::unbindLOV);
            require(row.field.getLOV() == current && row.field.isValidateFromLOV(), "bound failures preserve provider");
            row.verifyActive(active, former);
            row.wrap.fire();
            require(current.choices == 1 && row.model.equals(List.of("current", "choice-1")), "one active choice write");
            row.field.lovProperty().unbind();
            row.set(old);
            current.setValue("former");
            old.getValue();
            old.setValue("recovered");
            row.field.unbindLOV();
            old.setValue("detached");

            var standalone = new Provider();
            var providerButton = new JInvLOVButton(standalone);
            root.getChildren().add(providerButton);
            providerButton.fire();
            require(providerButton.getLOV() == standalone && providerButton.getTextField() == null
                    && standalone.choices == 1 && ((JInvLOVButton) active).getTextField() == row.field
                    && ((JInvLOVButton) active).getLOV() == null, "exclusive button modes");
            later(done, () -> {
                require(row.wrap.getInnerButton() == null && !row.wrap.isVisible()
                        && !row.wrap.onActionProperty().isBound() && !row.wrap.tooltipProperty().isBound()
                        && !row.wrap.disableProperty().isBound() && !row.wrap.visibleProperty().isBound(),
                        "recovery followed by same-turn detach");
                require(row.model.equals(List.of("current", "choice-1", "recovered")), "exact model sequence " + row.model);
                System.out.println("detached writes=0/0 active writes=" + row.model + " standalone choices=1");
                stage.hide();
                done.complete(null);
            });
        });
    }

    public static void main(String[] args) {
        var done = new CompletableFuture<Void>();
        Platform.startup(() -> later(done, () -> test(done)));
        int exit = 1;
        try {
            done.get(20, TimeUnit.SECONDS);
            System.out.println("CTRL-045 PASS");
            exit = 0;
        } catch (Exception error) {
            error.printStackTrace(System.err);
        } finally {
            Platform.exit();
        }
        System.exit(exit);
    }

    private static void later(CompletableFuture<Void> done, Runnable work) {
        Platform.runLater(() -> {
            try { work.run(); } catch (Throwable error) { done.completeExceptionally(error); }
        });
    }

    private static void rejects(Runnable mutation) {
        try {
            mutation.run();
        } catch (RuntimeException bound) {
            require(bound.getMessage().contains("A bound value cannot be set"), "unexpected exception " + bound);
            return;
        }
        throw new AssertionError("expected bound-property rejection");
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
