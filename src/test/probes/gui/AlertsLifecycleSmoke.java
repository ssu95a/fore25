import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.DialogPane;
import javafx.scene.layout.Pane;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;
import ru.inversion.fx.app.BaseApp;
import ru.inversion.fx.form.AlertBuilder;
import ru.inversion.fx.form.Alerts;

public final class AlertsLifecycleSmoke {
    private static final class OfflineApp extends BaseApp {
        private int checks;

        static OfflineApp install() {
            var app = new OfflineApp();
            g_app = app;
            return app;
        }

        @Override
        public boolean isAfterLogin() {
            checks++;
            return false;
        }
    }

    private static void require(boolean condition, String description) {
        if (!condition) {
            throw new AssertionError(description);
        }
    }

    private static void inspectMaker(Alerts.IAlertMaker maker, Stage expectedOwner,
                                     Modality expectedModality) throws Exception {
        var inspect = new FutureTask<Void>(() -> {
            Stage dialog = Window.getWindows().stream().filter(Window::isShowing)
                    .filter(window -> window.getScene().getRoot() instanceof DialogPane)
                    .map(Stage.class::cast).findFirst().orElseThrow();
            var pane = (DialogPane) dialog.getScene().getRoot();
            try {
                require(dialog.getOwner() == expectedOwner, "maker owner");
                require(dialog.getModality() == expectedModality, "maker modality");
            } finally {
                ((Button) pane.lookupButton(ButtonType.OK)).fire();
            }
            return null;
        });
        Platform.runLater(inspect);
        require(maker.showButtonAndWait().orElseThrow() == ButtonType.OK, "maker OK");
        inspect.get();
    }

    private static void exercise() throws Exception {
        require(BaseApp.APP() == null, "no BaseApp");
        var sceneLess = new Stage();
        var noApp = new AlertBuilder(sceneLess, Alert.AlertType.INFORMATION).build();
        require(noApp.getOwner() == null, "scene-less Stage skipped");
        Platform.runLater(() -> ((Button) noApp.getDialogPane().lookupButton(ButtonType.OK)).fire());
        require(noApp.showAndWait().orElseThrow() == ButtonType.OK, "no-app builder result");
        inspectMaker(Alerts.maker().window(new Pane()), null, Modality.WINDOW_MODAL);

        var app = OfflineApp.install();
        var root = new Pane();
        var owner = new Stage();
        owner.setScene(new Scene(root, 280, 150));
        owner.show();
        try {
            owner.setAlwaysOnTop(true);
            owner.setIconified(true);
            var builder = new AlertBuilder(root, Alert.AlertType.ERROR).modality(Modality.APPLICATION_MODAL);
            var legacy = builder.build();
            require(app.checks == 1, "legacy build consults login state");
            require(owner.isAlwaysOnTop() && !owner.isIconified(), "legacy owner restored without flag mutation");
            require(legacy.getOwner() == owner && legacy.getModality() == Modality.APPLICATION_MODAL,
                    "legacy owner and application modality");
            var maker = Alerts.maker().window(root).sound(false).modality(Modality.APPLICATION_MODAL);
            inspectMaker(maker, owner, Modality.APPLICATION_MODAL);
            require(app.checks == 1, "muted maker skips sound lifecycle");
            maker.sound(true);
            require(app.checks == 1, "configuration has no sound effect");
            inspectMaker(maker, owner, Modality.APPLICATION_MODAL);
            inspectMaker(maker, owner, Modality.APPLICATION_MODAL);
            require(app.checks == 3, "pre-login gate once per actual show");
            require(owner.isAlwaysOnTop(), "maker preserves owner flag");
        } finally {
            owner.close();
        }
    }

    public static void main(String[] args) throws Exception {
        Platform.startup(() -> Platform.setImplicitExit(false));
        try {
            try {
                Alerts.maker().showButtonAndWait();
                throw new AssertionError("worker thread show should fail");
            } catch (IllegalStateException e) {
                require(e.getMessage().contains("JavaFX Application Thread"), "explicit FX thread precondition");
            }
            var task = new FutureTask<Void>(() -> {
                exercise();
                return null;
            });
            Platform.runLater(task);
            task.get(15, TimeUnit.SECONDS);
        } finally {
            Platform.exit();
        }
        System.out.println("ALERT-004 PASS");
    }
}
