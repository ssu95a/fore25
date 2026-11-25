import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import javafx.animation.AnimationTimer;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.ContextMenu;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import ru.inversion.fx.app.BaseApp;
import ru.inversion.fx.app.service.ViewPrefAppService;
import ru.inversion.fx.form.ActionFactory;
import ru.inversion.fx.form.action.JInvAction;
import ru.inversion.fx.form.action.JInvEvent.PlaceType;
import ru.inversion.fx.form.action.JInvKeyboardManager;
import ru.inversion.fx.form.action.SceneKeyManager;
import ru.inversion.fx.form.controls.JInvTextArea;
import ru.inversion.fx.form.controls.skin.JInvTextAreaSkin;

public final class TextAreaSkinDisposalSmoke {
    private static final class App extends BaseApp {
        static void install() { g_app = new App(); }
        @Override public ViewPrefAppService getViewPrefService() { return new ViewPrefAppService(); }
    }

    private static final class Skin extends JInvTextAreaSkin {
        int disposals;
        Skin(JInvTextArea area) { super(area); }
        @Override public void dispose() { disposals++; super.dispose(); }
    }

    private final CompletableFuture<Void> done = new CompletableFuture<>();
    private final List<String> failures = new ArrayList<>();

    private void expect(boolean ok, String label) {
        if (!ok) failures.add(label);
    }

    private static List<JInvAction> actions(Scene scene, JInvTextArea area) {
        return SceneKeyManager.getActionMap(scene, PlaceType.FILTER).stream()
                .filter(pair -> pair.first == area && pair.second.getActionType() == ActionFactory.ActionTypeEnum.EDIT_EXTERNAL)
                .map(pair -> (JInvAction) pair.second).toList();
    }

    private static Skin replace(JInvTextArea area) {
        area.setSkin(null);
        var skin = new Skin(area);
        area.setSkin(skin);
        return skin;
    }

    private void start() {
        App.install();
        var area = new JInvTextArea();
        var preScene = replace(area);
        var initial = replace(area);
        var callerMenu = new ContextMenu();
        area.setContextMenu(callerMenu);
        var root = new VBox(area);
        var stage = new Stage();
        var first = new Scene(root, 340, 180);
        stage.setScene(first);
        stage.show();
        area.requestFocus();
        new AnimationTimer() {
            @Override public void handle(long now) {
                if (area.isFocused()) {
                    stop();
                    Platform.runLater(() -> checked(() -> exercise(area, preScene, initial, callerMenu, root, first, stage)));
                }
            }
        }.start();
    }

    private void exercise(JInvTextArea area, Skin preScene, Skin initial, ContextMenu menu, VBox root, Scene first, Stage stage) {
        require(area.isFocused() && area.getSkin() == initial && preScene.disposals == 1 && area.getLOV() == null,
                "actual focused skin setup");
        var firstActions = actions(first, area);
        expect(firstActions.size() == 1, "before-scene disposal retained actions: " + firstActions.size());
        int[] retiredCalls = {0};
        firstActions.forEach(action -> action.setHandler(event -> retiredCalls[0]++));
        var caller = (JInvAction) ActionFactory.createAction(ActionFactory.ActionTypeEnum.EDIT_EXTERNAL, event -> {});
        JInvKeyboardManager.addAction(area, caller);
        area.setEditable(false);
        area.setEditable(true);
        require(initial.getToolBar().getItems().size() == 2, "external-editor toolbar positive");
        int initialItems = initial.getToolBar().getItems().size();
        var next = replace(area);
        expect(initial.disposals == 1 && initial.getSkinnable() == null, "post-scene dispose");
        area.setEditable(false);
        area.setEditable(true);
        expect(initial.getToolBar().getItems().size() == initialItems, "retired editable listener fired");
        area.setEditable(false);
        expect(initial.getToolBar().getItems().size() == initialItems, "retired editable listener removed a button");
        area.setEditable(true);
        require(next.getToolBar().getItems().size() == 2 && area.getContextMenu() == menu, "active toolbar and caller menu");
        expect(!initial.getToolBar().visibleProperty().isBound() && !initial.getFeButton().visibleProperty().isBound(),
                "retired toolbar/FE bindings");
        var latest = replace(area);
        root.getChildren().remove(area);
        var second = new Scene(new VBox(area), 340, 180);
        stage.setScene(second);
        area.requestFocus();
        Platform.runLater(() -> checked(() -> {
            var actual = actions(second, area);
            expect(actions(first, area).equals(List.of(caller)), "first scene keeps only its caller action");
            expect(next.disposals == 1 && latest == area.getSkin() && actual.size() == 1, "one action in new scene: " + actual.size());
            int[] routed = {0};
            actual.forEach(action -> action.setHandler(event -> routed[0]++));
            require(area.isFocused(), "current control focused before synthetic F4");
            var f4 = new KeyEvent(second, area, KeyEvent.KEY_PRESSED, "", "", KeyCode.F4,
                    false, false, false, false);
            SceneKeyManager.filterHandler(f4);
            expect(f4.isConsumed() && routed[0] == 1 && retiredCalls[0] == 0, "current shortcut routing");
            System.out.printf("first initial actions=%d, first remaining=%d, second actions=%d, dispatch=%d, retired dispatch=%d%n",
                    firstActions.size(), actions(first, area).size(), actual.size(), routed[0], retiredCalls[0]);
            stage.hide();
            require(failures.isEmpty(), "TextArea skin ownership: " + failures);
            done.complete(null);
        }));
    }

    private void checked(Runnable action) {
        try { action.run(); } catch (Throwable failure) { done.completeExceptionally(failure); }
    }

    public static void main(String[] args) {
        var probe = new TextAreaSkinDisposalSmoke();
        Platform.startup(() -> Platform.runLater(() -> probe.checked(probe::start)));
        int exit = 1;
        try {
            probe.done.get(25, TimeUnit.SECONDS);
            System.out.println("CTRL-053 PASS");
            exit = 0;
        } catch (Exception failure) {
            failure.printStackTrace(System.err);
        } finally {
            Platform.exit();
        }
        System.exit(exit);
    }

    private static void require(boolean condition, String label) {
        if (!condition) throw new AssertionError(label);
    }
}
