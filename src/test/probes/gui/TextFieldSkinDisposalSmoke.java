import java.util.*;
import java.util.concurrent.*;
import java.util.function.*;
import javafx.animation.AnimationTimer;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.css.PseudoClass;
import javafx.scene.Scene;
import javafx.scene.input.*;
import javafx.scene.control.TextField;
import javafx.scene.layout.Pane;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import ru.inversion.fx.app.BaseApp;
import ru.inversion.fx.app.service.ViewPrefAppService;
import ru.inversion.fx.form.ViewContext;
import ru.inversion.fx.form.controls.*;
import ru.inversion.fx.form.controls.skin.JInvTextFieldButtonSkin;
import ru.inversion.fx.form.lov.*;

public class TextFieldSkinDisposalSmoke {
    static class App extends BaseApp {
        static void install() { g_app = new App(); }
        @Override public ViewPrefAppService getViewPrefService() { return new ViewPrefAppService(); }
    }
    static class Field extends JInvTextField {
        int overflowCalls;
        @Override protected javafx.scene.control.Skin<?> createDefaultSkin() { return new Skin(this); }
        @Override public boolean checkExcessTextLength() {
            overflowCalls++;
            return super.checkExcessTextLength();
        }
    }
    static class Skin extends JInvTextFieldButtonSkin {
        int disposals;
        Skin(TextField field) { super(field); }
        @Override public void dispose() { disposals++; super.dispose(); }
        JInvFEButton overflow() {
            return getChildren().stream().filter(JInvFEButton.class::isInstance)
                    .map(JInvFEButton.class::cast).findFirst().orElseThrow();
        }
    }
    static class Provider extends AbstractLovBase<String> {
        int calls;
        @Override public void showChoiceList(ViewContext view,String filter,BiConsumer<Boolean,ILov<String>> done) {
            setValue("pick-" + ++calls);
            done.accept(true,this);
        }
    }
    static final List<String> lifecycleFailures = new ArrayList<>();
    static final CompletableFuture<Void> result = new CompletableFuture<>();
    static final String LONG = "0123456789".repeat(8);
    static final PseudoClass FOCUSED = PseudoClass.getPseudoClass("focused");
    static void check(boolean ok,String label) { if (!ok) throw new AssertionError(label); }
    static void later(Runnable action) {
        Platform.runLater(() -> {
            try { action.run(); } catch (Throwable failure) { result.completeExceptionally(failure); }
        });
    }
    static void lifecycle(boolean ok, String label) {
        if (!ok) lifecycleFailures.add(label);
    }
    static void counts(Field field, int expected, String what, Runnable action) {
        int before = field.overflowCalls;
        action.run();
        int count = field.overflowCalls - before;
        System.out.printf("%s=%d%n",what,count);
        lifecycle(count == expected, what + " expected " + expected + ", got " + count);
    }
    static void start() {
        App.install();
        var field = new Field();
        var sentinel = new Field();
        field.setManaged(false);
        sentinel.setManaged(false);
        var external = new JInvLOVButton();
        external.setManaged(false);
        var stage = new Stage();
        stage.setScene(new Scene(new Pane(field,sentinel,external),450,130));
        stage.show();
        field.resizeRelocate(0,0,110,30);
        sentinel.resizeRelocate(0,50,110,30);
        external.resizeRelocate(140,0,90,30);
        field.requestFocus();
        new AnimationTimer() {
            @Override public void handle(long now) {
                if (field.isFocused()) {
                    stop();
                    later(() -> baseline(stage,field,sentinel,external));
                }
            }
        }.start();
    }
    static void baseline(Stage stage,Field field,Field sentinel,JInvLOVButton external) {
        var old = (Skin)field.getSkin();
        var oldWrap = old.getButton();
        var provider = new Provider();
        var model = new SimpleStringProperty("seed");
        field.textProperty().bindBidirectional(model);
        var writes = new ArrayList<String>();
        model.addListener((p,before,after) -> writes.add(after));
        counts(field,1,"baseline text",() -> field.setText(LONG));
        check(old.overflow().isVisible(),"baseline overflow");
        counts(field,1,"baseline width",() -> field.resize(115,30));
        counts(field,1,"baseline managed font",() -> field.setFont(Font.font(16)));
        counts(field,1,"baseline external attach",() -> external.setTextField(field));
        check(!old.overflow().isVisible(),"external suppresses overflow");
        field.setLOV(provider);
        external.fire();
        check(provider.calls==1 && writes.getLast().equals("pick-1"),"external positive");
        field.externalButtonProperty().set(null);
        later(() -> {
            check(oldWrap.isVisible() && oldWrap.onActionProperty().isBound(),"baseline wrapper");
            oldWrap.fire();
            check(provider.calls==2 && writes.getLast().equals("pick-2"),"inner positive");
            var oldButton = oldWrap.getInnerButton();
            var retiredOverflow = old.overflow();
            check(oldWrap.getOnAction() == oldButton.getOnAction()
                    && oldWrap.getTooltip() == oldButton.getTooltip()
                    && oldWrap.tooltipProperty().isBound(), "owned action and tooltip before disposal");

            field.setSkin(null);
            var current = new Skin(field);
            field.setSkin(current);
            check(old.disposals==1 && old.getSkinnable()==null && field.getSkin()==current
                    && current.getButton().getInnerButton()==oldButton && field.getLOV()==provider,
                    "installed new skin and retained functional button and provider");
            lifecycle(detached(oldWrap) && oldWrap.getOnAction() == null, "disposed wrapper still owns an action");
            boolean retiredOverflowVisible = retiredOverflow.isVisible();
            counts(field,1,"replaced text",() -> field.setText(LONG));
            check(current.overflow().isVisible(), "active overflow still responds");
            counts(field,1,"replaced width",() -> field.resize(120,30));
            counts(field,1,"replaced managed font",() -> field.setFont(Font.font(17)));
            counts(field,1,"replaced external toggle",() -> field.externalButtonProperty().set(external));
            check(!current.overflow().isVisible(), "active external suppression");
            field.externalButtonProperty().set(null);
            lifecycle(retiredOverflow.isVisible() == retiredOverflowVisible, "old overflow changed");
            boolean retiredFocused = oldWrap.getPseudoClassStates().contains(FOCUSED);
            sentinel.requestFocus();
            check(sentinel.isFocused(),"focus moved");
            lifecycle(oldWrap.getPseudoClassStates().contains(FOCUSED) == retiredFocused, "disposed wrapper receives blur");
            field.requestFocus();
            check(field.isFocused(),"focus returned");
            lifecycle(oldWrap.getPseudoClassStates().contains(FOCUSED) == retiredFocused, "disposed wrapper receives focus");
            oldButton.setText("obsolete");
            lifecycle(!oldWrap.getText().equals("obsolete"), "retired wrapper has a live text binding");

            for (int i = 1; i <= 3; i++) {
                var oldSentinel = (Skin) sentinel.getSkin();
                sentinel.setSkin(null);
                var newSentinel = new Skin(sentinel);
                sentinel.setSkin(newSentinel);
                check(oldSentinel.disposals == 1 && oldSentinel.getSkinnable() == null
                        && sentinel.getSkin() == newSentinel && newSentinel.getButton().getInnerButton() == null,
                        "no-LOV replacement identity " + i);
                String text = i % 2 == 0 ? "short" : LONG;
                counts(sentinel,1,"no-LOV replacement " + i,() -> sentinel.setText(text));
            }
            verifyNoLovListener(sentinel);
            field.requestFocus();
            int before = writes.size();
            field.fireEvent(new KeyEvent(KeyEvent.KEY_RELEASED,"","",KeyCode.F9,false,false,false,false));
            provider.setValue("ordinary");
            check(provider.calls==3 && writes.subList(before,writes.size()).equals(List.of("pick-3","ordinary")),"no duplicate model writes");
            field.setLOV(provider);
            var pendingWrapper = current.getButton();
            var pendingButton = pendingWrapper.getInnerButton();
            var callerMenu = new javafx.scene.control.ContextMenu();
            javafx.event.EventHandler<javafx.event.ActionEvent> callerAction = event -> {};
            field.setContextMenu(callerMenu);
            field.setOnAction(callerAction);
            field.setSkin(null);
            var finalSkin = new Skin(field);
            field.setSkin(finalSkin);
            check(field.getContextMenu() == callerMenu && field.getOnAction() == callerAction && field.getLOV() == provider
                    && current.disposals == 1 && field.getSkin() == finalSkin, "preserve caller menu and provider");
            field.setLOV(provider);
            later(() -> {
                lifecycle(detached(pendingWrapper) && pendingWrapper.getOnAction() == null
                        && !pendingButton.getProperties().containsKey(Controls.CONTROL_PARENT), "queued disposed attachment revived");
                check(finalSkin.getButton().isVisible(), "current wrapper visible");
                sentinel.requestFocus();
                field.requestFocus();
                check(finalSkin.getButton().getPseudoClassStates().contains(FOCUSED), "current wrapper receives focus");
                sentinel.requestFocus();
                check(!finalSkin.getButton().getPseudoClassStates().contains(FOCUSED), "current wrapper receives blur");
                field.setLOV(provider);
                field.unbindLOV();
                provider.setValue("detached");
                later(() -> {
                    check(detached(finalSkin.getButton()), "same-turn unbind on current skin");
                    check(writes.equals(List.of(LONG, "pick-1", "pick-2", LONG, "pick-3", "ordinary")),
                            "exact model writes " + writes);
                    System.out.println("dispose=" + old.disposals + " provider choices=" + provider.calls + " model writes=" + writes.size());
                    check(lifecycleFailures.isEmpty(), "skin lifecycle failures: " + lifecycleFailures);
                    stage.hide();
                    result.complete(null);
                });
            });
        });
    }
    private static boolean detached(JInvWrapButton wrap) {
        return wrap.getInnerButton() == null && !wrap.isVisible() && !wrap.onActionProperty().isBound()
                && !wrap.tooltipProperty().isBound() && !wrap.textProperty().isBound()
                && !wrap.disableProperty().isBound() && !wrap.visibleProperty().isBound()
                && !wrap.graphicProperty().isBound();
    }

    private static void verifyNoLovListener(JInvTextField field) {
        try {
            var listener = JInvTextField.class.getDeclaredField("lovValueListener");
            listener.setAccessible(true);
            check(listener.get(field) == null, "unused field must not allocate a LOV listener");
        } catch (ReflectiveOperationException failure) {
            throw new AssertionError(failure);
        }
    }
    public static void main(String[] args) {
        Platform.startup(() -> later(TextFieldSkinDisposalSmoke::start));
        int exit = 1;
        try { result.get(20,TimeUnit.SECONDS); System.out.println("CTRL-049 PASS"); exit=0; }
        catch (Exception error) { error.printStackTrace(); }
        finally { Platform.exit(); }
        System.exit(exit);
    }
}
