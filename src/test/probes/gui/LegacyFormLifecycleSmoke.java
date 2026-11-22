package ru.inversion.fx.form;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import javafx.animation.AnimationTimer;
import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.Region;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.stage.WindowEvent;
import ru.inversion.fx.app.BaseApp;
import ru.inversion.fx.form.AbstractBaseController.FormModeEnum;
import ru.inversion.fx.form.AbstractBaseController.FormReturnEnum;
import ru.inversion.fx.form.controls.JInvButton;
import ru.inversion.fx.form.controls.JInvTextArea;
import ru.inversion.fx.form.controls.skin.JInvTextAreaSkin;

public final class LegacyFormLifecycleSmoke {
    private static final String TEXT = "GUI-010 controller payload";
    private final CompletableFuture<Void> verdict = new CompletableFuture<>();
    private final List<LifeCycleStateEnum> states = new ArrayList<>();
    private final List<ResultForm<String>> results = new ArrayList<>();
    private Stage stage;
    private JInvTextAreaFormController controller;
    private int closeRequests;
    private int hiddenEvents;
    private int progressShown;
    private int progressHidden;

    public static void main(String[] args) {
        var smoke = new LegacyFormLifecycleSmoke();
        int exit = 1;
        try {
            require(Runtime.version().feature() == 25, "setup: Java 25 required");
            Platform.startup(() -> smoke.checked(smoke::show));
            smoke.verdict.get(30, TimeUnit.SECONDS);
            System.out.println("GUI-010 PASS (standalone production FXML/controller and SDI close)");
            exit = 0;
        } catch (TimeoutException failure) {
            System.err.println("GUI-010 UNKNOWN: timed out waiting for layout or close completion");
        } catch (Throwable failure) {
            failure.printStackTrace(System.err);
        } finally {
            Platform.exit();
        }
        System.exit(exit);
    }

    private void show() throws Exception {
        Platform.setImplicitExit(false);
        Thread.currentThread().setUncaughtExceptionHandler((thread, failure) -> verdict.completeExceptionally(failure));
        String version = System.getProperty("javafx.version", "UNKNOWN");
        require(version.equals("25") || version.startsWith("25."), "setup: JavaFX 25 required");
        require(BaseApp.APP() == null, "setup: expected standalone execution without BaseApp");
        System.out.println("Java=" + Runtime.version() + " JavaFX=" + version);

        var loader = new FXMLLoader(Objects.requireNonNull(getClass().getResource("fxml/JInvTextAreaForm.fxml"),
                "setup: production form FXML missing"));
        Region root = loader.load();
        controller = loader.getController();
        require(controller.getClass() == JInvTextAreaFormController.class,
                "FXML: production controller was not instantiated");
        var editor = (JInvTextArea) loader.getNamespace().get("edText");
        var ok = (JInvButton) loader.getNamespace().get("btOK");
        require(editor != null && ok != null, "FXML: Legacy editor or button missing");

        var scene = new Scene(root);
        scene.getStylesheets().add(Objects.requireNonNull(getClass().getResource("/css/general.css"),
                "setup: production stylesheet missing").toExternalForm());
        stage = new Stage();
        stage.setTitle("GUI-010 Legacy form lifecycle smoke");
        stage.setScene(scene);
        controller.setViewContext(ViewContext.of(stage));
        controller.setDataObject(TEXT);
        controller.initProperties = Map.of();
        controller.formMode = FormModeEnum.VM_EDIT;
        controller.setDisableSavedDimensions(true);
        controller.returnCallback = results::add;
        controller.lifeCycleStateProperty().addListener((value, before, after) -> states.add(after));
        stage.addEventFilter(WindowEvent.WINDOW_CLOSE_REQUEST, event -> closeRequests++);
        stage.addEventHandler(WindowEvent.WINDOW_HIDDEN, event -> hiddenEvents++);
        stage.setOnCloseRequest(controller::internalOnClose);

        controller.init();
        require(TEXT.equals(editor.getText()) && editor.isEditable(),
                "controller init: injected editor did not receive form data or edit mode");
        controller.configStandartButtons();
        require("BUTTON_OK".equals(ok.getId()) && ok.getOnAction() != null,
                "controller: standard OK button was not configured");
        require(controller.getViewContext().getStage() == stage && controller.getContentPane() == root,
                "controller: incorrect ViewContext attachment");
        require(controller.returnPropery.getValue() == FormReturnEnum.RET_CANCEL,
                "controller: incorrect initial result");
        stage.show();
        afterPulse(() -> checkShown(editor, ok));
    }

    private void checkShown(JInvTextArea editor, JInvButton ok) {
        require(stage.isShowing() && editor.getSkin() instanceof JInvTextAreaSkin
                        && editor.getWidth() > 0 && editor.getHeight() > 0
                        && ok.isVisible() && ok.getWidth() > 0 && ok.getHeight() > 0,
                "layout: visible Legacy editor skin and OK button required");
        System.out.println("FXML production controller initialized; CSS, TextArea skin and standard OK laid out");
        Window.getWindows().addListener((ListChangeListener<Window>) changes -> {
            while (changes.next()) {
                for (Window window : changes.getAddedSubList()) {
                    if (window instanceof Stage child && child.getOwner() == stage) {
                        progressShown++;
                        child.addEventHandler(WindowEvent.WINDOW_HIDDEN, event -> {
                            progressHidden++;
                            Platform.runLater(() -> checked(this::checkClosed));
                        });
                    }
                }
            }
        });
        ok.fire();
    }

    private void checkClosed() {
        require(!stage.isShowing() && closeRequests == 1 && hiddenEvents == 1,
                "close: expected one production close request and one hidden notification");
        require(states.equals(List.of(LifeCycleStateEnum.ON_OK, LifeCycleStateEnum.ON_CLOSE)),
                "close: incorrect controller lifecycle " + states);
        require(controller.returnPropery.getValue() == FormReturnEnum.RET_OK
                        && results.size() == 1 && results.getFirst().getController() == controller
                        && results.getFirst().getFormReturn() == FormReturnEnum.RET_OK
                        && results.getFirst().getException() == null,
                "close: result binding or callback mismatch");
        require(progressShown == 1 && progressHidden == 1 && Window.getWindows().stream().noneMatch(Window::isShowing),
                "close: resource progress or form window still showing");
        System.out.println("OK -> WINDOW_CLOSE_REQUEST -> " + states + " -> RET_OK; cleanup window closed");
        verdict.complete(null);
    }

    private void afterPulse(CheckedAction action) {
        new AnimationTimer() {
            private boolean first = true;

            @Override public void handle(long now) {
                if (first) {
                    first = false;
                    return;
                }
                stop();
                checked(action);
            }
        }.start();
    }

    @FunctionalInterface
    private interface CheckedAction {
        void run() throws Exception;
    }

    private void checked(CheckedAction action) {
        if (verdict.isDone()) return;
        try {
            action.run();
        } catch (Throwable failure) {
            verdict.completeExceptionally(failure);
        }
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
