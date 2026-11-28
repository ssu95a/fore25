import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
import javafx.event.ActionEvent;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import org.controlsfx.control.action.ActionUtils;
import ru.inversion.fore.form.action.ForeActions;
import ru.inversion.fore.form.action.StandardAction;
import ru.inversion.fx.form.ActionFactory;

public final class ForeActionModelSmoke {
    static void check(boolean condition, String name) {
        if (!condition) throw new AssertionError(name);
    }

    static void run() {
        var received = new ArrayList<ActionEvent>();
        var action = ForeActions.create(StandardAction.REFRESH, received::add);
        var independent = ForeActions.create(StandardAction.REFRESH, e -> {});
        check(action != independent && action.standardType() == StandardAction.REFRESH
                && action.getText().equals(StandardAction.REFRESH.text())
                && action.getLongText().equals(StandardAction.REFRESH.tooltip()), "independent standard metadata");
        check(action.getAccelerator().equals(new KeyCodeCombination(KeyCode.F8))
                && ActionFactory.ActionTypeEnum.REFRESH.getHotKey().isEmpty(), "native and legacy F8 defaults");
        var custom = ForeActions.custom("custom", received::add);
        check(custom.standardType() == null && custom.getGraphic() == null && custom.hotkeys().isEmpty(), "custom defaults");

        var first = new KeyCodeCombination(KeyCode.F2);
        var secondary = new KeyCodeCombination(KeyCode.F6);
        var replacement = new KeyCodeCombination(KeyCode.F4);
        var input = new ArrayList<>(List.of(first, secondary));
        custom.setHotkeys(input);
        input.clear();
        check(custom.hotkeys().equals(List.of(first, secondary)), "immutable snapshot");
        try {
            custom.hotkeys().clear();
            throw new AssertionError("mutable hotkeys");
        } catch (UnsupportedOperationException expected) {
            check(custom.hotkeys().size() == 2, "immutable hotkeys");
        }
        custom.setAccelerator(replacement);
        check(custom.hotkeys().equals(List.of(replacement, secondary)), "inherited primary setter");
        custom.setAccelerator(null);
        check(custom.hotkeys().equals(List.of(secondary)), "clear primary only");
        custom.acceleratorProperty().bind(new SimpleObjectProperty<>(replacement));
        try {
            custom.setHotkeys(List.of(first));
            throw new AssertionError("bound accelerator write");
        } catch (RuntimeException expected) {
            check(custom.hotkeys().equals(List.of(replacement, secondary)), "bound write rollback");
        }
        custom.acceleratorProperty().unbind();

        var button = ActionUtils.createButton(action);
        var item = ActionUtils.createMenuItem(action);
        check(button.getGraphic() != item.getGraphic() && button.getGraphic() != action.getGraphic(),
                "new graphics for views");
        var stage = new Stage();
        var scene = new Scene(new VBox(button), 260, 90);
        scene.getStylesheets().add(ForeActionModelSmoke.class.getResource("/style/style.css").toExternalForm());
        stage.setScene(scene);
        stage.show();
        try {
            scene.getRoot().applyCss();
            check(((Text) button.getGraphic()).getFont().getFamily().equals(action.icon().fontFamily()), "bundled font CSS");
            action.setText("updated");
            action.setLongText("hint");
            action.setDisabled(true);
            button.fire();
            item.fire();
            check(received.isEmpty() && button.isDisabled() && item.isDisable()
                    && independent.getText().equals(StandardAction.REFRESH.text()), "disabled and independent");
            action.setDisabled(false);
            button.fire();
            item.fire();
            var direct = new ActionEvent();
            custom.handle(direct);
            check(received.size() == 3 && received.get(0).getSource() == button
                    && received.get(1).getSource() == item && received.getLast() == direct, "view sources and direct identity");
            check(button.getTooltip().getText().equals("hint") && item.getText().equals("updated"), "observable view updates");
            ActionUtils.unconfigureButton(button);
            ActionUtils.unconfigureMenuItem(item);
            action.setText("detached");
            check(!button.getText().equals("detached") && !item.getText().equals("detached"), "view detach");
            action.setIcon(null);
            check(action.icon() == null && action.getGraphic() == null, "optional icon");
        } finally {
            stage.hide();
        }
    }

    public static void main(String[] args) throws Exception {
        var work = new FutureTask<Void>(() -> { run(); return null; });
        Platform.startup(() -> { Platform.setImplicitExit(false); Platform.runLater(work); });
        try {
            work.get(35, TimeUnit.SECONDS);
            System.out.println("ACT-003 PASS");
        } finally {
            Platform.exit();
        }
    }
}
