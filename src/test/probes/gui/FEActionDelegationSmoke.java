import java.lang.management.ManagementFactory;
import java.util.Arrays;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.util.StringConverter;
import ru.inversion.fx.app.BaseApp;
import ru.inversion.fx.app.property.CACHE_AppProperties;
import ru.inversion.fx.app.property.IAppProperties;
import ru.inversion.fx.app.property.PropertiesTypeEnum;
import ru.inversion.fx.app.service.ViewPrefAppService;
import ru.inversion.fx.form.action.JInvKeyboardManager;
import ru.inversion.fx.form.controls.*;
import ru.inversion.fx.form.controls.skin.JInvTextFieldButtonSkin;

public final class FEActionDelegationSmoke {
    static class App extends BaseApp {
        private final IAppProperties offlineProperties = new CACHE_AppProperties(this);
        static void install() { g_app = new App(); }
        @Override public ViewPrefAppService getViewPrefService() { return new ViewPrefAppService(); }
        @Override public IAppProperties getProperties(PropertiesTypeEnum type) { return offlineProperties; }
    }

    static class ObservedField extends JInvTextField {
        int buttonTooltipReads;
        @Override public String getButtonToolTipText() {
            buttonTooltipReads++;
            return super.getButtonToolTipText();
        }
    }

    static void require(boolean condition, String what) {
        if (!condition) throw new AssertionError(what);
    }

    static long dialog(Stage owner, Runnable trigger, String initial, String draft, boolean accept, boolean editable)
            throws Exception {
        var panel = new FutureTask<Void>(() -> {
            var window = Window.getWindows().stream().filter(w -> w instanceof Stage stage
                    && stage.getOwner() == owner && stage.getScene().getRoot() instanceof DialogPane)
                    .findFirst().orElseThrow();
            try {
                var pane = (DialogPane) window.getScene().getRoot();
                var editor = (TextArea) pane.getContent().lookup(".text-area");
                require(editor != null && editor.getText().equals(initial) && editor.isEditable() == editable,
                        "dialog value, owner and editability");
                if (draft != null) editor.setText(draft);
                if (accept) ((Button) pane.lookupButton(ButtonType.OK)).fire();
                else window.hide();
            } finally {
                if (window.isShowing()) window.hide();
            }
            return null;
        });
        var memory = (com.sun.management.ThreadMXBean) ManagementFactory.getThreadMXBean();
        Platform.runLater(panel);
        long before = memory.getCurrentThreadAllocatedBytes();
        trigger.run();
        long allocated = memory.getCurrentThreadAllocatedBytes() - before;
        panel.get(2, TimeUnit.SECONDS);
        return allocated;
    }

    static void run(boolean baseline, boolean measure) throws Exception {
        App.install();
        var field = new ObservedField();
        var sentinel = new JInvTextField("untouched");
        field.setText("seed");
        int[] writes = {0};
        field.textProperty().addListener((o, oldValue, newValue) -> writes[0]++);
        var stage = new Stage();
        var root = new VBox(field, sentinel);
        var scene = new Scene(root, 440, 170);
        stage.setScene(scene);
        JInvKeyboardManager.initKeyBoard(scene);
        stage.show();
        try {
            root.applyCss();
            root.layout();
            Runnable direct = () -> field.getEditDialogAction().orElseThrow().handle(new ActionEvent());
            if (measure) {
                var samples = new long[9];
                for (int i = -5; i < 9; i++) {
                    long bytes = dialog(stage, direct, "seed", null, false, true);
                    if (i >= 0) samples[i] = bytes;
                }
                Arrays.sort(samples);
                System.out.printf("direct dialog FX bytes: median=%d range=%d..%d%n", samples[4], samples[0], samples[8]);
            }
            field.buttonTooltipReads = 0;
            dialog(stage, direct, "seed", "discarded", false, true);
            require(writes[0] == 0 && field.getText().equals("seed"), "cancel no write");
            require(field.buttonTooltipReads == (baseline ? 1 : 0), "temporary visual-target configuration count");

            var point = field.localToScreen(10, 10);
            field.fireEvent(new ContextMenuEvent(ContextMenuEvent.CONTEXT_MENU_REQUESTED,
                    10, 10, point.getX(), point.getY(), true, null));
            var menu = field.getContextMenu();
            require(menu != null && menu.isShowing(), "real menu popup");
            var action = field.getListItemsToPopulateContextMenu().get(1);
            menu.hide();
            dialog(stage, action::fire, "seed", "menu", true, true);
            require(writes[0] == 1 && field.getText().equals("menu"), "one menu write");
            dialog(stage, () -> field.fireEvent(new KeyEvent(KeyEvent.KEY_PRESSED, "", "", KeyCode.E,
                    false, true, false, false)), "menu", "keyboard", true, true);
            require(writes[0] == 2 && field.getText().equals("keyboard"), "one keyboard write");

            field.setReadOnly(true);
            dialog(stage, direct, "keyboard", null, true, false);
            require(writes[0] == 2, "read-only no write");
            field.setReadOnly(false);

            var configured = new JInvFEButton();
            field.setInnerButton(configured);
            int[] converted = {0};
            configured.setStringConverter(new StringConverter<>() {
                @Override public String toString(String s) { return "shown:" + s; }
                @Override public String fromString(String s) { converted[0]++; return "converted:" + s; }
            });
            dialog(stage, direct, "shown:keyboard", "custom", true, true);
            require(converted[0] == 1 && field.getText().equals("converted:custom") && writes[0] == 3,
                    "configured converter is used once");
            int[] custom = {0};
            configured.setOnAction(e -> custom[0]++);
            direct.run();
            require(custom[0] == 1, "configured custom handler");
            configured.disableProperty().unbind();
            configured.setDisable(true);
            direct.run();
            require(custom[0] == 1 && writes[0] == 3, "configured disabled blocks fire");
            field.removeInnerButton();

            field.setSkin(null);
            field.setSkin(new JInvTextFieldButtonSkin(field));
            dialog(stage, direct, "converted:custom", "new-skin", true, true);
            require(writes[0] == 4 && field.getText().equals("new-skin") && sentinel.getText().equals("untouched"),
                    "replacement skin and no foreign writes");
        } finally {
            stage.hide();
        }
    }

    public static void main(String[] args) throws Exception {
        var work = new FutureTask<Void>(() -> {
            run(Arrays.asList(args).contains("baseline"), Arrays.asList(args).contains("measure"));
            return null;
        });
        Platform.startup(() -> { Platform.setImplicitExit(false); Platform.runLater(work); });
        try {
            work.get(45, TimeUnit.SECONDS);
            System.out.println("ARCH-001 PASS (real dialog, FE routing and legacy configured button)");
        } finally {
            Platform.exit();
        }
    }
}
