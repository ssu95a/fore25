import java.util.List;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.ContextMenuEvent;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.Window;
import ru.inversion.fx.app.BaseApp;
import ru.inversion.fx.app.service.ViewPrefAppService;
import ru.inversion.fx.form.F7FilterTextField;
import ru.inversion.fx.form.controls.*;
import ru.inversion.fx.form.controls.skin.JInvTextAreaSkin;

public final class TextAreaContextMenuLifecycleSmoke {
    static final class App extends BaseApp {
        static void install() { g_app = new App(); }
        @Override public ViewPrefAppService getViewPrefService() { return new ViewPrefAppService(); }
    }

    static void require(boolean condition, String label) {
        if (!condition) throw new AssertionError(label);
    }

    static void request(Node node, boolean keyboard) {
        var point = node.localToScreen(10, 10);
        node.fireEvent(new ContextMenuEvent(ContextMenuEvent.CONTEXT_MENU_REQUESTED,
                10, 10, point == null ? 10 : point.getX(), point == null ? 10 : point.getY(), keyboard, null));
    }

    static List<ContextMenu> popups() {
        return Window.getWindows().stream().filter(ContextMenu.class::isInstance)
                .map(ContextMenu.class::cast).filter(Window::isShowing).toList();
    }

    static ContextMenu popup() {
        var menus = popups();
        require(menus.size() == 1, "expected one popup, got " + menus.size());
        return menus.getFirst();
    }

    static void editFromMenu(Stage owner, VBox root) {
        var field = new JInvTextArea("original\nline two");
        var sentinel = new JInvTextArea("sentinel");
        root.getChildren().setAll(field, sentinel);
        root.applyCss();
        root.layout();
        for (boolean accept : new boolean[]{false, true}) {
            request(field, true);
            var menu = popup();
            var editItem = field.getListItemsToPopulateContextMenu().get(1);
            require(menu.getItems().contains(editItem), "FE action is installed in popup");
            menu.hide();
            var check = new FutureTask<Void>(() -> {
                var dialog = Window.getWindows().stream().filter(w -> w instanceof Stage stage
                                && stage.getOwner() == owner && w.getScene().getRoot() instanceof DialogPane)
                        .findFirst().orElseThrow();
                try {
                    var pane = (DialogPane) dialog.getScene().getRoot();
                    var editor = (TextArea) pane.getContent().lookup(".text-area");
                    require(editor != null && editor.getText().equals("original\nline two"), "FE dialog original target");
                    editor.setText("draft");
                    if (accept) ((Button) pane.lookupButton(ButtonType.OK)).fire();
                    else dialog.hide();
                } finally {
                    if (dialog.isShowing()) dialog.hide();
                }
                return null;
            });
            Platform.runLater(check);
            editItem.fire();
            try {
                check.get(2, TimeUnit.SECONDS);
            } catch (Exception failure) {
                throw new AssertionError("FE dialog completion", failure);
            }
            require(field.getText().equals(accept ? "draft" : "original\nline two") && sentinel.getText().equals("sentinel"),
                    "FE dialog changes only the original target after accept");
        }
    }

    static void run(JInvTextArea field, VBox root, int ownCount) {
        require(field.getContextMenu() == null, "pre-request menu must be absent");
        try {
            request(field, false);
        } catch (IllegalArgumentException expected) {
            require(expected.getMessage().contains("associated with a window"), "detached JavaFX popup failure");
        }
        require(field.getContextMenu() == null && popups().isEmpty(), "detached request does not open menu");
        var oldSkin = (JInvTextAreaSkin)field.getSkin();
        require(oldSkin.getToolBar().getItems().contains(oldSkin.getFeButton()), "toolbar FE available");
        var extensions = field.getListItemsToPopulateContextMenu();
        require(extensions == oldSkin.getListItemsToPopulateContextMenu(), "legacy delegate list identity");
        var custom = new MenuItem("custom first");
        extensions.add(custom);
        int[] populates = {0};
        field.onPopulateContextMenu(menu -> {
            require(menu.getItems().getFirst() == custom, "custom order and identity");
            populates[0]++;
        });
        root.getChildren().setAll(field);
        root.applyCss();
        root.layout();
        require(field.getContextMenu() == null, "CSS/layout do not initialize menu");
        EventHandler<ContextMenuEvent> consumer = event -> event.consume();
        field.addEventFilter(ContextMenuEvent.CONTEXT_MENU_REQUESTED, consumer);
        request(field, false);
        field.removeEventFilter(ContextMenuEvent.CONTEXT_MENU_REQUESTED, consumer);
        require(field.getContextMenu() == null && popups().isEmpty() && populates[0] == 0, "consumed request");
        var external = new ContextMenu(new MenuItem("external"));
        var property = new SimpleObjectProperty<ContextMenu>(external);
        field.contextMenuProperty().bind(property);
        request(field, true);
        require(popup() == external && populates[0] == 0, "bound external wins before first use");
        external.hide();
        property.set(null);
        request(field, true);
        var fallback = popup();
        require(field.getContextMenu() == null && populates[0] == 0, "bound null uses FX fallback");
        fallback.hide();
        field.contextMenuProperty().unbind();
        field.setOnContextMenuRequested(consumer);
        request(field, false);
        require(popups().isEmpty() && field.getContextMenu() == null, "application handler first");
        field.setOnContextMenuRequested(null);
        field.setText("alpha");
        field.selectRange(0, 3);
        request(field.lookup(".text"), false);
        var own = popup();
        require(own == field.getContextMenu() && own != fallback, "child request opens control-owned popup");
        require(own.getItems().size() == ownCount && populates[0] == 1, "first show exactly one population");
        own.hide();
        field.setEditable(false);
        request(field, true);
        require(popup() == own && own.getItems().size() == ownCount - 5 && populates[0] == 2, "read-only keyboard reopening");
        own.hide();
        field.setOnContextMenuRequested(consumer);
        require(field.getContextMenu() == null, "owned menu yields");
        field.setContextMenu(external);
        field.setOnContextMenuRequested(null);
        require(field.getContextMenu() == external, "external menu retains ownership");
        field.setContextMenu(null);
        require(field.getContextMenu() == own, "own restored after external removal");
        oldSkin.onPopulateContextMenu(menu -> populates[0]++);
        field.setSkin(null);
        field.setSkin(new JInvTextAreaSkin(field));
        require(field.getContextMenu() == own && field.getListItemsToPopulateContextMenu() == extensions
                && extensions == oldSkin.getListItemsToPopulateContextMenu(), "skin replacement retains control menu state");
        own.show(field, javafx.geometry.Side.BOTTOM, 0, 0);
        require(popup() == own && populates[0] == 3, "programmatic reopening after replacement");
        own.hide();
    }

    public static void main(String[] args) throws Exception {
        var work = new FutureTask<Void>(() -> {
            App.install();
            var stage = new Stage();
            var root = new VBox();
            stage.setScene(new Scene(root, 500, 180));
            stage.show();
            try {
                run(new JInvTextArea(), root, 11);
                run(new JInvTextArea() {
                    @Override public java.util.Optional<EventHandler<javafx.event.ActionEvent>> getEditDialogAction() {
                        return java.util.Optional.empty();
                    }
                }, root, 9);
                editFromMenu(stage, root);
            } finally {
                popups().forEach(ContextMenu::hide);
                stage.hide();
            }
            return null;
        });
        Platform.startup(() -> { Platform.setImplicitExit(false); Platform.runLater(work); });
        try {
            work.get(40, TimeUnit.SECONDS);
            System.out.println("PERF-015 PASS (control-owned context menu lifecycle)");
        } finally {
            Platform.exit();
        }
    }
}
