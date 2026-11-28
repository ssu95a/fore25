import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Supplier;
import javafx.application.Platform;
import javafx.event.Event;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.stage.WindowEvent;
import ru.inversion.fx.app.BaseApp;
import ru.inversion.fx.form.AlertBuilder;
import ru.inversion.fx.form.Alerts;
import ru.inversion.fx.form.JInvAlert;

public final class AlertsFacadeSmoke {
    private static final class OfflineApp extends BaseApp {
        static void install() {
            g_app = new OfflineApp();
        }

        @Override
        public boolean isAfterLogin() {
            return false;
        }
    }

    private static void require(boolean condition, String description) {
        if (!condition) {
            throw new AssertionError(description);
        }
    }

    private static <T> T inspect(Stage owner, Supplier<T> show, Consumer<DialogPane> inspect,
                                 ButtonType result) throws Exception {
        var pending = new FutureTask<Void>(() -> {
            var stage = Window.getWindows().stream().filter(Window::isShowing)
                    .filter(window -> window != owner && window.getScene().getRoot() instanceof DialogPane)
                    .map(Stage.class::cast).findFirst().orElseThrow();
            var pane = (DialogPane) stage.getScene().getRoot();
            try {
                require(stage.getOwner() == owner && stage.getModality() == Modality.WINDOW_MODAL,
                        "static owner and modality");
                inspect.accept(pane);
                if (result == null) {
                    Event.fireEvent(stage, new WindowEvent(stage, WindowEvent.WINDOW_CLOSE_REQUEST));
                    require(!stage.isShowing(), "window close request closes the dialog");
                }
            } finally {
                if (stage.isShowing()) {
                    ButtonType button = result != null ? result : ButtonType.CANCEL;
                    ((Button) pane.lookupButton(button)).fire();
                }
            }
            return null;
        });
        Platform.runLater(pending);
        T returned = show.get();
        pending.get();
        return returned;
    }

    private static void exercise() throws Exception {
        OfflineApp.install();
        var owner = new Stage();
        owner.setScene(new Scene(new Pane(), 280, 150));
        owner.show();
        try {
            ResourceBundle titles = ResourceBundle.getBundle("alerts");
            inspect(owner, () -> {
                Alerts.info(owner, "information header");
                return null;
            }, pane -> {
                require("information header".equals(pane.getHeaderText()), "info header");
                require(pane.getButtonTypes().equals(List.of(ButtonType.OK)), "info OK");
                require(((Stage) pane.getScene().getWindow()).getTitle().equals(titles.getString("TITLE_INFORMATION")),
                        "bundle title");
            }, ButtonType.OK);
            inspect(owner, () -> {
                Alerts.error(owner, "Error", "Header", "message", List.of("one", "two"));
                return null;
            }, pane -> {
                require("message".equals(pane.getContentText()), "error text");
                require(pane.getExpandableContent() != null && !pane.isExpanded(), "error detail");
            }, ButtonType.OK);

            Consumer<DialogPane> yesNo = pane -> require(
                    pane.getButtonTypes().equals(List.of(ButtonType.YES, ButtonType.NO)), "yesNo choices");
            require(inspect(owner, () -> Alerts.yesNo(owner, "yes or no"), yesNo, ButtonType.YES), "YES true");
            require(!inspect(owner, () -> Alerts.yesNo(owner, "yes or no"), yesNo, ButtonType.NO), "NO false");
            Consumer<DialogPane> yesNoCancel = pane -> require(
                    pane.getButtonTypes().equals(List.of(ButtonType.YES, ButtonType.NO, ButtonType.CANCEL)),
                    "yesNoCancel choices");
            require(inspect(owner, () -> Alerts.yesNoCancel(owner, "choose"), yesNoCancel, ButtonType.YES)
                    .equals(Optional.of(true)), "YES optional true");
            require(inspect(owner, () -> Alerts.yesNoCancel(owner, "choose"), yesNoCancel, ButtonType.NO)
                    .equals(Optional.of(false)), "NO optional false");
            require(inspect(owner, () -> Alerts.yesNoCancel(owner, "choose"), yesNoCancel, null).isEmpty(),
                    "window-close request yields no Boolean");

            var message = new Label("message");
            var detail = new Label("detail");
            var graphic = new Label("graphic");
            var selected = inspect(owner,
                    () -> Alerts.showAlert(owner.getScene(), Alert.AlertType.CONFIRMATION, "Title", "",
                            message, detail, graphic, ButtonType.OK, ButtonType.CANCEL),
                    pane -> {
                        require(((VBox) pane.getContent()).getChildren().getFirst() == message, "message Node identity");
                        require(pane.getExpandableContent() == detail && pane.isExpanded(), "detail Node expanded");
                        require(pane.getGraphic() == graphic, "graphic identity");
                        require(pane.getHeaderText().isEmpty(), "empty header");
                    }, ButtonType.OK);
            require(selected == ButtonType.OK, "static exact ButtonType");
            require(inspect(owner,
                    () -> Alerts.showAlert(owner, Alert.AlertType.INFORMATION, null, null, "", null),
                    pane -> {
                        require(pane.getContent() == null && pane.getHeaderText() == null, "empty content and null header");
                        require(pane.getButtonTypes().equals(List.of(ButtonType.OK)), "native default list");
                    }, ButtonType.OK) == ButtonType.OK, "default button result");

            JInvAlert legacy = new AlertBuilder(owner, Alert.AlertType.INFORMATION)
                    .modality(Modality.APPLICATION_MODAL).build();
            require(legacy.getOwner() == owner && legacy.getModality() == Modality.APPLICATION_MODAL,
                    "binary legacy build owner and modality");
            Platform.runLater(() -> ((Button) legacy.getDialogPane().lookupButton(ButtonType.OK)).fire());
            require(legacy.showAndWait().orElseThrow() == ButtonType.OK, "binary legacy builder result");
        } finally {
            owner.close();
        }
    }

    public static void main(String[] args) throws Exception {
        Platform.startup(() -> Platform.setImplicitExit(false));
        var task = new FutureTask<Void>(() -> {
            exercise();
            return null;
        });
        Platform.runLater(task);
        try {
            task.get(20, TimeUnit.SECONDS);
        } finally {
            Platform.exit();
        }
        System.out.println("ALERT-007 PASS");
    }
}
