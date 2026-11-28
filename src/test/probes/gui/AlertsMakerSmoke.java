import java.util.List;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import javafx.application.Platform;
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
import ru.inversion.fx.form.Alerts;

public final class AlertsMakerSmoke {
    private static void require(boolean condition, String description) {
        if (!condition) {
            throw new AssertionError(description);
        }
    }

    private static FutureTask<Void> onDialog(Window owner, Consumer<DialogPane> inspect, ButtonType result) {
        var task = new FutureTask<Void>(() -> {
            var dialog = Window.getWindows().stream()
                    .filter(window -> window.isShowing() && window != owner)
                    .filter(window -> window.getScene().getRoot() instanceof DialogPane)
                    .findFirst().orElseThrow();
            var pane = (DialogPane) dialog.getScene().getRoot();
            try {
                inspect.accept(pane);
            } finally {
                ((Button) pane.lookupButton(result)).fire();
            }
            return null;
        });
        Platform.runLater(task);
        return task;
    }

    private static void exercise() throws Exception {
        var owner = new Stage();
        owner.setScene(new Scene(new Pane(), 280, 150));
        owner.show();
        try {
            var before = onDialog(owner, pane -> {
                require(pane.getButtonTypes().equals(List.of(ButtonType.OK)), "default OK");
                require(pane.getContent() == null, "default content");
            }, ButtonType.OK);
            require(Alerts.maker().showButtonAndWait().orElseThrow() == ButtonType.OK, "typed OK");
            before.get();

            var message = new Label("message");
            var maker = Alerts.maker().window(owner).type(Alert.AlertType.CONFIRMATION)
                    .title("Maker").headerText("Header").content(message).detail("Detail")
                    .modality(Modality.WINDOW_MODAL).buttons(ButtonType.YES, ButtonType.NO);
            var first = onDialog(owner, pane -> {
                var dialog = (Stage) pane.getScene().getWindow();
                require(dialog.getOwner() == owner, "resolved owner");
                require(dialog.getModality() == Modality.WINDOW_MODAL, "window modal");
                require("Maker".equals(dialog.getTitle()) && "Header".equals(pane.getHeaderText()), "texts");
                require(((VBox) pane.getContent()).getChildren().getFirst() == message, "node identity");
                require(pane.getExpandableContent() != null, "details");
            }, ButtonType.NO);
            require(maker.showButtonAndWait().orElseThrow() == ButtonType.NO, "typed NO");
            first.get();

            var next = onDialog(owner, pane -> {
                require("second".equals(pane.getContentText()), "updated content");
                require(pane.getButtonTypes().equals(List.of(ButtonType.YES, ButtonType.NO)), "same choices");
            }, ButtonType.YES);
            require(maker.contentText("second").<ButtonType>showAndWait().orElseThrow() == ButtonType.YES,
                    "legacy generic route");
            next.get();
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
            task.get(15, TimeUnit.SECONDS);
        } finally {
            Platform.exit();
        }
        System.out.println("ALERT-002 PASS");
    }
}
