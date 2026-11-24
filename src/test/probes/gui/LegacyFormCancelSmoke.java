package ru.inversion.fx.form;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import javafx.animation.AnimationTimer;
import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.stage.WindowEvent;
import ru.inversion.fx.app.BaseApp;
import ru.inversion.fx.form.AbstractBaseController.FormModeEnum;
import ru.inversion.fx.form.AbstractBaseController.FormReturnEnum;
import ru.inversion.fx.form.controls.JInvButton;
import ru.inversion.fx.form.controls.JInvTextArea;
import ru.inversion.fx.form.controls.skin.JInvTextAreaSkin;

public final class LegacyFormCancelSmoke {
    private enum Route { BUTTON, WINDOW_REQUEST, VETO_THEN_BUTTON, CLOSE_NOW }
    private final CompletableFuture<Void> verdict = new CompletableFuture<>();

    public static void main(String[] args) {
        var probe = new LegacyFormCancelSmoke();
        Platform.startup(() -> probe.checked(() -> {
            Platform.setImplicitExit(false);
            Thread.currentThread().setUncaughtExceptionHandler((thread, ex) -> probe.verdict.completeExceptionally(ex));
            String fx = System.getProperty("javafx.version", "");
            require(Runtime.version().feature() == 25 && fx.matches("25(?:\\..*)?") && BaseApp.APP() == null,
                    "standalone Java and JavaFX 25");
            System.out.println("Java=" + Runtime.version() + " JavaFX=" + fx);
            probe.show(0);
        }));
        int exit = 1;
        try {
            probe.verdict.get(40, TimeUnit.SECONDS);
            System.out.println("CTRL-022 PASS");
            exit = 0;
        } catch (Exception failure) {
            failure.printStackTrace(System.err);
        } finally {
            Platform.exit();
        }
        System.exit(exit);
    }

    private void show(int ordinal) throws Exception {
        if (ordinal == Route.values().length) {
            verdict.complete(null);
            return;
        }
        Route route = Route.values()[ordinal];
        var stage = new Stage();
        var controller = new ObservedController();
        controller.veto = route == Route.VETO_THEN_BUTTON;
        var loader = new FXMLLoader(Objects.requireNonNull(getClass().getResource("fxml/JInvTextAreaForm.fxml")));
        loader.setControllerFactory(type -> {
            require(type == JInvTextAreaFormController.class, "production FXML controller type");
            return controller;
        });
        Region root = loader.load();
        var editor = (JInvTextArea) loader.getNamespace().get("edText");
        var ok = (JInvButton) loader.getNamespace().get("btOK");
        var cancel = new JInvButton();
        // В исходной форме есть только OK. Кнопку Cancel добавляет тест.
        cancel.setId("btCancel");
        ((VBox) ok.getParent()).getChildren().add(cancel);
        var scene = new Scene(root);
        scene.getStylesheets().add(Objects.requireNonNull(getClass().getResource("/css/general.css")).toExternalForm());
        stage.setScene(scene);
        stage.setTitle("CTRL-022 " + route);
        controller.setViewContext(ViewContext.of(stage));
        controller.setDataObject("CTRL-022 payload");
        controller.initProperties = Map.of();
        controller.formMode = FormModeEnum.VM_NONE;
        controller.setDisableSavedDimensions(true);
        var results = new ArrayList<ResultForm<String>>();
        controller.returnCallback = results::add;
        controller.init();
        controller.configStandartButtons();
        require(cancel.getOnAction() != null && "CTRL-022 payload".equals(editor.getText()), "initialized production form");

        var states = new ArrayList<LifeCycleStateEnum>();
        var requests = new ArrayList<Boolean>();
        int[] counts = new int[3];  // WINDOW_HIDDEN, progress shown, progress hidden
        controller.lifeCycleStateProperty().addListener((o, a, b) -> states.add(b));
        stage.setOnCloseRequest(event -> {
            controller.internalOnClose(event);
            requests.add(event.isConsumed());
        });
        stage.addEventHandler(WindowEvent.WINDOW_HIDDEN, event -> counts[0]++);
        ListChangeListener<Window> windows = change -> {
            while (change.next()) {
                for (Window window : change.getAddedSubList()) {
                    if (window instanceof Stage progress && progress.getOwner() == stage) {
                        counts[1]++;
                        progress.addEventHandler(WindowEvent.WINDOW_HIDDEN, event -> {
                            counts[2]++;
                            Platform.runLater(() -> checked(() -> {
                                require(!stage.isShowing() && counts[0] == 1 && counts[1] == 1 && counts[2] == 1,
                                        route + " expected one hidden and one progress cleanup");
                                require(Window.getWindows().stream().noneMatch(Window::isShowing), route + " lingering window");
                                require(requests.equals(route == Route.VETO_THEN_BUTTON ? List.of(true, false) : List.of(false)),
                                        route + " close request consumption " + requests);
                                var expected = switch (route) {
                                    case VETO_THEN_BUTTON -> List.of(LifeCycleStateEnum.ON_CANCEL, LifeCycleStateEnum.RUNTIME,
                                            LifeCycleStateEnum.ON_CANCEL, LifeCycleStateEnum.ON_CLOSE);
                                    case CLOSE_NOW -> List.of(LifeCycleStateEnum.ON_CLOSE);
                                    default -> List.of(LifeCycleStateEnum.ON_CANCEL, LifeCycleStateEnum.ON_CLOSE);
                                };
                                require(states.equals(expected), route + " lifecycle " + states);
                                require(controller.cancelCalls == (route == Route.CLOSE_NOW ? 0
                                        : route == Route.VETO_THEN_BUTTON ? 2 : 1)
                                        && controller.closeCalls == 1 && controller.resources.get() == 1,
                                        route + " lifecycle hook counts");
                                require(results.size() == 1 && results.getFirst().getController() == controller
                                        && results.getFirst().getFormReturn() == FormReturnEnum.RET_CANCEL
                                        && results.getFirst().getException() == null, route + " single RET_CANCEL");
                                System.out.printf("%s requests consumed=%s states=%s hidden=%d cleanup=%d callback=%d%n",
                                        route, requests, states, counts[0], controller.resources.get(), results.size());
                                show(ordinal + 1);
                            }));
                        });
                    }
                }
            }
        };
        Window.getWindows().addListener(windows);
        // Каждый слушатель учитывает только дочернее окно своего Stage.
        verdict.whenComplete((r, ex) -> Window.getWindows().removeListener(windows));
        stage.show();
        afterPulse(() -> {
            require(stage.isShowing() && editor.getSkin() instanceof JInvTextAreaSkin
                    && cancel.isVisible() && cancel.getWidth() > 0, "visible styled form and cancel button");
            switch (route) {
                case BUTTON -> cancel.fire();
                case WINDOW_REQUEST -> stage.fireEvent(new WindowEvent(stage, WindowEvent.WINDOW_CLOSE_REQUEST));
                case CLOSE_NOW -> controller.closeNow(FormReturnEnum.RET_CANCEL);
                case VETO_THEN_BUTTON -> {
                    cancel.fire();
                    afterPulse(() -> {
                        require(stage.isShowing() && requests.equals(List.of(true))
                                && results.isEmpty() && counts[0] == 0 && counts[1] == 0
                                && controller.closeCalls == 0 && controller.resources.get() == 0, "veto kept form open");
                        controller.veto = false;
                        cancel.fire();
                    });
                }
            }
        });
    }

    private static final class ObservedController extends JInvTextAreaFormController {
        boolean veto;
        int cancelCalls, closeCalls;
        final AtomicInteger resources = new AtomicInteger();

        @Override protected boolean onCancel() {
            cancelCalls++;
            return super.onCancel() && !veto;
        }
        @Override protected void onCloseWindow() { closeCalls++; super.onCloseWindow(); }
        @Override protected void closeResources() { resources.incrementAndGet(); }
    }

    private void afterPulse(Checked action) {
        new AnimationTimer() {
            private boolean first = true;
            @Override public void handle(long now) {
                if (first) { first = false; return; }
                stop();
                Platform.runLater(() -> checked(action));
            }
        }.start();
    }

    @FunctionalInterface private interface Checked { void run() throws Exception; }

    private void checked(Checked action) {
        if (verdict.isDone()) return;
        try { action.run(); }
        catch (Throwable ex) { verdict.completeExceptionally(ex); }
    }

    private static void require(boolean ok, String label) {
        if (!ok) throw new AssertionError(label);
    }
}
