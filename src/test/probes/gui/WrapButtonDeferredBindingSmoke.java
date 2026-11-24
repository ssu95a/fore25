import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
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
import ru.inversion.fx.form.controls.Controls;
import ru.inversion.fx.form.controls.JInvButton;
import ru.inversion.fx.form.controls.JInvFEButton;
import ru.inversion.fx.form.controls.JInvTextField;
import ru.inversion.fx.form.controls.JInvWrapButton;
import ru.inversion.fx.form.controls.skin.JInvTextFieldButtonSkin;
import ru.inversion.fx.form.lov.AbstractLovBase;
import ru.inversion.fx.form.lov.ILov;

public final class WrapButtonDeferredBindingSmoke {
    private static final class App extends BaseApp {
        static void install() { g_app = new App(); }
        @Override public ViewPrefAppService getViewPrefService() { return new ViewPrefAppService(); }
    }

    private static final class Provider extends AbstractLovBase<String> {
        int opens;
        @Override public void showChoiceList(ViewContext view, String text, BiConsumer<Boolean, ILov<String>> callback) {
            setValue("choice-" + ++opens);
            callback.accept(true, this);
        }
    }

    private record Sample(String route, JInvTextField field, JInvWrapButton wrapper,
                          JInvButton first, JInvButton second, Provider active, List<String> writes) {
        boolean verify() {
            var expected = active == null ? null : second == null ? first : second;
            var obsolete = expected == first ? List.<JInvButton>of()
                    : expected == null && second != null ? List.of(first, second) : List.of(first);
            boolean noStaleParent = obsolete.stream().noneMatch(b -> b.getProperties().containsKey(Controls.CONTROL_PARENT));
            boolean bindings = wrapper.onActionProperty().isBound() && wrapper.textProperty().isBound()
                    && wrapper.tooltipProperty().isBound() && wrapper.graphicProperty().isBound()
                    && wrapper.disableProperty().isBound() && wrapper.visibleProperty().isBound();
            boolean unbound = !wrapper.onActionProperty().isBound() && !wrapper.textProperty().isBound()
                    && !wrapper.tooltipProperty().isBound() && !wrapper.graphicProperty().isBound()
                    && !wrapper.disableProperty().isBound() && !wrapper.visibleProperty().isBound();
            boolean values;
            if (expected == null) {
                values = unbound && !wrapper.isVisible() && wrapper.getOnAction() == null;
            } else {
                expected.setText("active");
                values = bindings && wrapper.isVisible() && !wrapper.isDisabled()
                        && wrapper.getOnAction() == expected.getOnAction() && wrapper.getTooltip() == expected.getTooltip()
                        && "active".equals(wrapper.getText()) && expected.getProperties().get(Controls.CONTROL_PARENT) == wrapper;
                field.setReadOnly(true);
                values &= wrapper.isDisabled() && !wrapper.isVisible();
                field.setReadOnly(false);
                wrapper.fire();
                values &= active.opens == 1 && writes.equals(List.of("active", "choice-1"));
            }
            String before = wrapper.getText();
            obsolete.forEach(button -> button.setText("obsolete"));
            values &= before.equals(wrapper.getText());
            System.out.printf("%s inner=%s bindings=%s visible=%s obsoleteDetached=%s writes=%s%n",
                    route, wrapper.getInnerButton() == expected, bindings, wrapper.isVisible(), noStaleParent, writes);
            return wrapper.getInnerButton() == expected && noStaleParent && values;
        }
    }

    private static Sample setup(VBox root, String route) {
        var field = new JInvTextField();
        root.getChildren().add(field);
        field.applyCss();
        var wrapper = ((JInvTextFieldButtonSkin) field.getSkin()).getButton();
        var model = new SimpleStringProperty("seed");
        field.textProperty().bindBidirectional(model);
        var writes = new ArrayList<String>();
        model.addListener((p, before, after) -> writes.add(after));
        var a = new Provider();
        var b = new Provider();
        field.setLOV(a);
        var first = wrapper.getInnerButton();
        JInvButton second = null;
        if (route.startsWith("A-null")) field.unbindLOV();
        if (route.contains("B")) {
            field.setLOV(b);
            second = wrapper.getInnerButton();
        }
        if ("A-B-null".equals(route)) field.unbindLOV();
        if ("A".equals(route)) {
            wrapper.setInnerButton(new JInvFEButton());
            require(wrapper.getInnerButton() == first, "LOV priority over FE button");
        }
        var active = field.getLOV();
        if (a != active) a.setValue("former");
        if (b != active) b.setValue("former");
        require(writes.isEmpty(), "former providers must not write");
        if (active != null) active.setValue("active");
        return new Sample(route, field, wrapper, first, second, (Provider) active, writes);
    }

    public static void main(String[] args) {
        var done = new CompletableFuture<Void>();
        Platform.startup(() -> later(done, () -> {
            App.install();
            var root = new VBox();
            var stage = new Stage();
            stage.setScene(new Scene(root, 400, 200));
            stage.show();
            var samples = List.of("A", "A-null", "A-B", "A-null-B", "A-B-null").stream()
                    .map(route -> setup(root, route)).toList();
            later(done, () -> {
                var failed = samples.stream().filter(sample -> !sample.verify()).map(Sample::route).toList();
                stage.hide();
                require(failed.isEmpty(), "stale deferred attachment: " + failed);
                done.complete(null);
            });
        }));
        int exit = 1;
        try {
            done.get(20, TimeUnit.SECONDS);
            System.out.println("CTRL-043 PASS");
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

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
