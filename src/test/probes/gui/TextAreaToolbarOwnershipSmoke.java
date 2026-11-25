import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import javafx.application.Platform;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.css.PseudoClass;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import ru.inversion.fx.app.BaseApp;
import ru.inversion.fx.app.service.ViewPrefAppService;
import ru.inversion.fx.form.ActionFactory;
import ru.inversion.fx.form.action.JInvAction;
import ru.inversion.fx.form.controls.JInvTextArea;
import ru.inversion.fx.form.controls.skin.JInvTextAreaSkin;

public final class TextAreaToolbarOwnershipSmoke {
    private static final class App extends BaseApp {
        static void install() { g_app = new App(); }
        @Override public ViewPrefAppService getViewPrefService() { return new ViewPrefAppService(); }
    }

    private static final class Scenario {
        final JInvTextArea first = new JInvTextArea("first draft");
        final JInvTextArea second = new JInvTextArea("second draft");
        final Button caller = new Button("Caller");
        final List<String> failures = new ArrayList<>();
        int callerActions;

        void exercise() {
            var oldSkin = (JInvTextAreaSkin) first.getSkin();
            var secondSkin = (JInvTextAreaSkin) second.getSkin();
            var oldToolbar = oldSkin.getToolBar();
            var secondToolbar = secondSkin.getToolBar();
            var oldFe = oldSkin.getFeButton();
            var oldEditor = externalButton(first, oldSkin);
            first.setToolBarVisible(true);
            require(oldToolbar.isVisible(), "active toolbar visibility");
            first.buttonFEVisibleProperty().set(false);
            require(!oldFe.isVisible(), "active FE visibility");
            first.buttonFEVisibleProperty().set(true);
            var oldFeAction = oldFe.getOnAction();
            require(oldFeAction != null && oldFe.getTextField() == first, "FE initially associated with area");

            var oldAction = (JInvAction) oldEditor.getOnAction();
            var originalHandler = oldAction.getHandler();
            require(originalHandler != null, "installed external editor handler");
            int[] safeActions = {0};
            oldAction.setHandler(event -> safeActions[0]++);
            oldEditor.fire();
            oldAction.setHandler(originalHandler);
            require(safeActions[0] == 1, "safe active external editor dispatch");

            var callerVisibility = new SimpleBooleanProperty(true);
            caller.visibleProperty().bind(callerVisibility);
            caller.setOnAction(event -> callerActions++);
            var callerHandler = caller.getOnAction();
            oldToolbar.getItems().add(caller);
            require(caller.getPseudoClassStates().contains(PseudoClass.getPseudoClass("toolbar-vertical"))
                    && caller.getMaxWidth() == Double.MAX_VALUE, "active vertical toolbar styling");
            widthEffects("active caller", caller, oldFe, oldEditor, 1);
            for (int i = 0; i < 2; i++) {
                oldToolbar.getItems().remove(caller);
                widthEffects("removed caller " + i, caller, oldFe, oldEditor, 0);
                oldToolbar.getItems().add(caller);
                widthEffects("readded caller " + i, caller, oldFe, oldEditor, 1);
            }
            oldToolbar.getItems().remove(caller);
            secondToolbar.getItems().add(caller);
            widthEffects("moved caller on old toolbar", caller, oldFe, oldEditor, 0);
            var secondEditor = externalButton(second, secondSkin);
            widthEffects("moved caller on active toolbar", caller, secondSkin.getFeButton(), secondEditor, 1);
            caller.fire();
            require(callerActions == 1 && caller.getOnAction() == callerHandler && caller.visibleProperty().isBound(),
                    "caller identity, handler and binding after move");

            first.selectRange(0, 5);
            first.setSkin(null);
            var replacement = new JInvTextAreaSkin(first);
            first.setSkin(replacement);
            require(oldSkin.getSkinnable() == null && first.getSelectedText().equals("first")
                    && replacement.getToolBar().isVisible(), "old skin disposed, selection and visibility preserved");
            var replacementEditor = externalButton(first, replacement);
            widthEffects("retired FE", oldFe, oldEditor, oldFe, 0);
            widthEffects("retired external editor", oldEditor, oldFe, oldEditor, 0);
            widthEffects("active replacement FE", replacement.getFeButton(), replacementEditor, replacement.getFeButton(), 1);
            boolean oldCallbackRetained = oldAction.getHandler() == originalHandler;
            expect(!oldCallbackRetained, "old external editor action retains the skin callback");
            require(oldFe.getOnAction() == oldFeAction && oldFe.getTextField() == first,
                    "public FE button retains its own area association");

            // A retained caller item can stay in the old toolbar through disposal.
            var retained = new Button("Retained");
            var retainedAction = new int[1];
            retained.setOnAction(event -> retainedAction[0]++);
            replacement.getToolBar().getItems().add(retained);
            var replacementAction = (JInvAction) replacementEditor.getOnAction();
            replacementAction.setHandler(event -> safeActions[0]++);
            first.setSkin(null);
            require(replacement.getSkinnable() == null, "replacement disposed");
            widthEffects("caller retained at dispose", retained, replacement.getFeButton(), replacementEditor, 0);
            replacement.getToolBar().getItems().remove(retained);
            secondToolbar.getItems().add(retained);
            retained.fire();
            replacementEditor.fire();
            require(safeActions[0] == 2 && retainedAction[0] == 1 && secondToolbar.getItems().contains(retained),
                    "caller replacement callback and moved button action survive");

            System.out.printf("retired editor callback retained=%s safe editor dispatches=%d caller dispatches=%d/%d%n",
                    oldCallbackRetained, safeActions[0], callerActions, retainedAction[0]);
            require(failures.isEmpty(), "toolbar ownership violations: " + failures);
        }

        private Button externalButton(JInvTextArea area, JInvTextAreaSkin skin) {
            area.setEditable(false);
            area.setEditable(true);
            return skin.getToolBar().getItems().stream().filter(Button.class::isInstance).map(Button.class::cast)
                    .filter(button -> button.getOnAction() instanceof JInvAction action
                            && action.getActionType() == ActionFactory.ActionTypeEnum.EDIT_EXTERNAL)
                    .findFirst().orElseThrow();
        }

        private void widthEffects(String label, Button source, Button observed, Button wide, int expected) {
            observed.setPrefWidth(31);
            wide.setPrefWidth(140);
            int[] changes = {0};
            javafx.beans.value.ChangeListener<Number> listener = (p, oldWidth, newWidth) -> changes[0]++;
            observed.prefWidthProperty().addListener(listener);
            try {
                source.resize(source.getWidth() + 3, 20);
            } finally {
                observed.prefWidthProperty().removeListener(listener);
            }
            System.out.printf("%s downstream width changes=%d%n", label, changes[0]);
            expect(changes[0] == expected && observed.getPrefWidth() == (expected == 0 ? 31 : 140), label);
        }

        private void expect(boolean result, String label) {
            if (!result) failures.add(label);
        }
    }

    public static void main(String[] args) throws Exception {
        var done = new CompletableFuture<Void>();
        Platform.startup(() -> Platform.runLater(() -> {
            try {
                App.install();
                var test = new Scenario();
                var stage = new Stage();
                stage.setScene(new Scene(new VBox(test.first, test.second), 440, 360));
                stage.show();
                Platform.runLater(() -> {
                    try {
                        test.exercise();
                        done.complete(null);
                    } catch (Throwable failure) {
                        done.completeExceptionally(failure);
                    } finally {
                        stage.hide();
                    }
                });
            } catch (Throwable failure) {
                done.completeExceptionally(failure);
            }
        }));
        try {
            done.get(25, TimeUnit.SECONDS);
            System.out.println("CTRL-066 PASS");
        } finally {
            Platform.exit();
        }
    }

    private static void require(boolean result, String message) {
        if (!result) throw new AssertionError(message);
    }
}
