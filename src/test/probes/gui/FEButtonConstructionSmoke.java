import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ResourceBundle;
import java.lang.management.ManagementFactory;
import java.util.Arrays;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.*;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.util.StringConverter;
import ru.inversion.fx.app.BaseApp;
import ru.inversion.fx.app.service.ViewPrefAppService;
import ru.inversion.fx.form.ViewContext;
import ru.inversion.fx.form.controls.*;
import ru.inversion.fx.form.controls.skin.JInvTextFieldButtonSkin;
import ru.inversion.fx.form.lov.AbstractLovBase;
import ru.inversion.fx.form.lov.ILov;
import ru.inversion.icons.IconFactory;
import ru.inversion.icons.enums.*;

public final class FEButtonConstructionSmoke {
    static boolean baseline;
    static boolean measure;
    static final String LONG = "abcdefghij".repeat(30);

    static final class App extends BaseApp {
        static void install() { g_app = new App(); }
        @Override public ViewPrefAppService getViewPrefService() { return new ViewPrefAppService(); }
    }

    static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    static JInvFEButton overflow(JInvTextField field) {
        var node = field.lookup(".transparentButton");
        check(node != null && node.getClass() == JInvFEButton.class, "exact overflow class");
        var button = (JInvFEButton) node;
        check(button.getTextField() == field && button.getTooltip() == null, "overflow target and absent tooltip");
        check(!button.isManaged() && !button.isFocusTraversable() && !button.isDisabled()
                && button.getMaxWidth() == 12 && button.getOnAction() != null, "overflow flags and action");
        var expected = (Label) IconFactory.getLabel(FontAwesome.fa_ellipsis_h, IconSize.SMALL,
                Color.DODGERBLUE.deriveColor(1,1,1,0.5));
        var actual = (Label) button.getGraphic();
        check(expected.getText().equals(actual.getText()) && expected.getStyle().equals(actual.getStyle())
                && expected.getFont().equals(actual.getFont()) && expected.getTextFill().equals(actual.getTextFill()),
                "overflow colored graphic");
        return button;
    }

    static void defaults(JInvFEButton button, TextInputControl target) {
        String tooltip = ResourceBundle.getBundle("fore").getString("OTKRYT_V_DIALOGOVOM_OKNE");
        var expected = (Label) IconFactory.getLabel(FontAwesome.fa_ellipsis_h);
        check(button.getTextField() == target && button.getTooltip().getText().equals(tooltip)
                && ((Label)button.getGraphic()).getText().equals(expected.getText())
                && !button.isFocusTraversable() && button.getOnAction() != null, "legacy defaults");
    }

    static void dialog(Stage owner, Runnable action, String initial, String draft, boolean accept, boolean editable) {
        var result = new CompletableResult();
        Platform.runLater(() -> {
            Window window = null;
            try {
                window = Window.getWindows().stream().filter(w -> w != owner && w.isShowing())
                        .filter(w -> w.getScene().getRoot() instanceof DialogPane).findFirst().orElseThrow();
                check(window instanceof Stage stage && stage.getOwner() == owner, "real dialog owner");
                var pane = (DialogPane)window.getScene().getRoot();
                var area = (TextArea)pane.getContent().lookup(".text-area");
                check(area != null && area.getText().equals(initial) && area.isEditable() == editable,
                        "real dialog initial text and editable state");
                if (draft != null) area.setText(draft);
                if (accept) ((Button)pane.lookupButton(ButtonType.OK)).fire();
                else window.hide();
                result.completed = true;
            } catch (Throwable failure) {
                result.failure = failure;
                if (window != null) window.hide();
            }
        });
        action.run();
        if (result.failure != null) throw new AssertionError("dialog verification", result.failure);
        check(result.completed, "dialog runs a real nested event loop");
    }

    static final class CompletableResult {
        boolean completed;
        Throwable failure;
    }

    static void run() throws Exception {
        App.install();
        var field = new JInvTextField();
        var currency = new JInvTextFieldCUR();
        var legacyTarget = new TextField("legacy-target");
        var direct = new JInvFEButton(legacyTarget);
        var noArg = new JInvFEButton();
        defaults(direct, legacyTarget);
        defaults(noArg, null);
        check(direct.getGraphic() != noArg.getGraphic() && direct.getTooltip() != noArg.getTooltip(), "legacy ownership");
        var xml = "<?import ru.inversion.fx.form.controls.JInvFEButton?><JInvFEButton xmlns:fx='http://javafx.com/fxml/1'/>";
        defaults(new FXMLLoader().load(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8))), null);

        if (!baseline) {
            var graphic = new Label("custom");
            var tooltip = new Tooltip("custom");
            var custom = new JInvFEButton(graphic, tooltip);
            check(custom.getGraphic() == graphic && custom.getTooltip() == tooltip
                    && !custom.isFocusTraversable() && custom.getOnAction() != null, "custom identities");
            var absent = new JInvFEButton(null, null);
            check(absent.getGraphic() == null && absent.getTooltip() == null
                    && absent.getOnAction() != null && !absent.isFocusTraversable(), "null means absent");
            custom.setTextField(legacyTarget);
            check(custom.getTextField() == legacyTarget, "custom target");
        }

        var button = overflow(field);
        var currencyButton = overflow(currency);
        var skin = (JInvTextFieldButtonSkin)field.getSkin();
        check(button.isVisible() && currencyButton.isVisible() && button.getGraphic() != currencyButton.getGraphic(),
                "pre-scene visibility and separate graphics");
        var children = field.getChildrenUnmodifiable();
        check(children.indexOf(button) >= 0 && children.indexOf(skin.getButton()) == children.indexOf(button) + 1,
                "immediate child order");
        var root = new VBox(field, currency, legacyTarget);
        var stage = new Stage();
        var scene = new Scene(root, 500, 160);
        var css = FEButtonConstructionSmoke.class.getResource("/css/general.css");
        if (css != null) scene.getStylesheets().add(css.toExternalForm());
        stage.setScene(scene);
        field.setMaxWidth(220);
        try {
            stage.show();
            field.setText("a");
            root.applyCss();
            root.layout();
            check(!button.isVisible() && overflow(field) == button, "short hidden");
            if (measure) {
                var mem=(com.sun.management.ThreadMXBean)ManagementFactory.getThreadMXBean();
                long[] alloc=new long[9], time=new long[9];
                for(int i=-5;i<9;i++){
                    field.setText("a");
                    root.layout();
                    long bytes=mem.getCurrentThreadAllocatedBytes(), t=System.nanoTime();
                    field.setText(LONG);
                    root.layout();
                    long ns=System.nanoTime()-t, delta=mem.getCurrentThreadAllocatedBytes()-bytes;
                    check(button.isVisible(),"first show after short");
                    if(i>=0){alloc[i]=delta;time[i]=ns/1000;}
                }
                Arrays.sort(alloc); Arrays.sort(time);
                System.out.printf("short to overflow B=%d(%d..%d) us=%d(%d..%d)%n",alloc[4],alloc[0],alloc[8],time[4],time[0],time[8]);
            }
            field.setText(LONG);
            root.layout();
            check(button.isVisible() && overflow(field) == button, "long visible");
            double longWidth = field.getWidth(), longX = button.getLayoutX();
            field.setMaxWidth(150);
            root.layout();
            check(button.isVisible() && field.getWidth() < longWidth && button.getLayoutX() < longX, "resize geometry");
            field.setFont(Font.font("System", 18));
            root.layout();
            check(button.isVisible(), "font change");
            field.externalButtonProperty().set(new Button("external"));
            root.layout();
            check(!button.isVisible(), "external button suppression");
            field.externalButtonProperty().set(null);
            root.layout();
            check(button.isVisible(), "external button removal");
            int[] lovRequests = {0};
            field.setLOV(new AbstractLovBase<String>() {
                @Override
                public void showChoiceList(ViewContext view, String filter, BiConsumer<Boolean, ILov<String>> done) {
                    lovRequests[0]++;
                    done.accept(false, this);
                }
            }, false);
            var lovBindingReady = new Object();
            Platform.runLater(() -> Platform.exitNestedEventLoop(lovBindingReady, null));
            Platform.enterNestedEventLoop(lovBindingReady);
            root.layout();
            check(skin.getButton().isVisible() && skin.getButton().getInnerButton() != null
                    && button.isVisible(), "internal LOV and overflow coexist");
            field.fireEvent(new KeyEvent(KeyEvent.KEY_RELEASED, "", "", KeyCode.F9, false, false, false, false));
            check(lovRequests[0] == 1, "F9 still reaches LOV");
            field.unbindLOV();
            field.setText(LONG);

            int[] reads = {0}, writes = {0};
            button.setStringConverter(new StringConverter<>() {
                @Override public String toString(String value) { reads[0]++; return "show:" + value; }
                @Override public String fromString(String value) { writes[0]++; return "accepted:" + value; }
            });
            var configured = new JInvFEButton();
            int[] configuredCalls = {0};
            configured.setOnAction(event -> configuredCalls[0]++);
            field.setInnerButton(configured);
            dialog(stage, button::fire, "show:" + LONG, "discarded", false, true);
            check(field.getText().equals(LONG) && reads[0] == 1 && writes[0] == 0, "dismiss does not commit the draft");
            check(configuredCalls[0] == 0, "overflow does not invoke configured inner FE");
            field.getEditDialogAction().orElseThrow().handle(new ActionEvent());
            check(configuredCalls[0] == 1, "configured FE still handles ordinary edit action");
            field.removeInnerButton();
            dialog(stage, button::fire, "show:" + LONG, "draft", true, true);
            check(field.getText().equals("accepted:draft") && reads[0] == 2 && writes[0] == 1, "accept writes converted target");
            check(legacyTarget.getText().equals("legacy-target"), "no foreign write");
            field.setText(LONG);
            field.setReadOnly(true);
            check(!button.isDisabled(), "read-only retains overflow action");
            dialog(stage, button::fire, "show:" + LONG, null, true, false);
            check(field.getText().equals(LONG) && writes[0] == 1, "read-only dialog does not write");

            field.setReadOnly(false);
            dialog(stage, () -> field.getEditDialogAction().orElseThrow().handle(new ActionEvent()), LONG, "fallback", true, true);
            check(field.getText().equals("fallback"), "direct fallback action");
            var customMenuItem = new MenuItem("fixture custom dialog");
            customMenuItem.setOnAction(field.getEditDialogAction().orElseThrow());
            skin.getListItemsToPopulateContextMenu().add(customMenuItem);
            var anchor = field.localToScreen(5, 5);
            field.fireEvent(new ContextMenuEvent(ContextMenuEvent.CONTEXT_MENU_REQUESTED,
                    5, 5, anchor.getX(), anchor.getY(), false, null));
            var menu = field.getContextMenu();
            check(menu != null && menu.isShowing() && menu.getItems().contains(customMenuItem),
                    "custom menu item is populated");
            menu.hide();
            dialog(stage, customMenuItem::fire, "fallback", "menu-draft", false, true);
            check(field.getText().equals("fallback"), "custom menu dismiss");
            dialog(stage, direct::fire, "legacy-target", "legacy-accepted", true, true);
            check(legacyTarget.getText().equals("legacy-accepted") && field.getText().equals("fallback"), "legacy target isolation");
            field.setText("a");
            root.layout();
            check(!button.isVisible() && overflow(field) == button, "short again");
            field.setSkin(null);
            check(field.getContextMenu() == menu, "control-owned menu survives skin disposal");
            field.setSkin(new JInvTextFieldButtonSkin(field));
            check(overflow(field) != button && button.getParent() == null, "overflow lifetime follows skin");
        } finally {
            stage.hide();
        }
    }

    public static void main(String[] args) throws Exception {
        baseline=Arrays.asList(args).contains("baseline");
        measure=Arrays.asList(args).contains("measure");
        var work = new FutureTask<Void>(() -> { run(); return null; });
        Platform.startup(() -> {
            Platform.setImplicitExit(false);
            Platform.runLater(work);
        });
        try {
            work.get(45, TimeUnit.SECONDS);
            System.out.println("ARCH-006 PASS (construction, dialog, overflow and skin lifecycle)");
        } finally {
            Platform.exit();
        }
    }
}
