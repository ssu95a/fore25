import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Side;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import ru.inversion.fx.form.controls.JInvTabPane;
import ru.inversion.fx.form.controls.skin.JInvTabPaneSkin;

public final class TabPaneSkinListenersSmoke {
    private final CompletableFuture<Void> done = new CompletableFuture<>();
    private final List<Throwable> setterAttempts = new ArrayList<>();
    private final List<String> failures = new ArrayList<>();

    private static Tab tab(String name) { return new Tab(name, new Label("Content " + name)); }

    private Tab counted(JInvTabPane pane) {
        Tab tab = tab("Counter");
        // Связанное свойство позволяет посчитать каждую попытку установки стиля до отметки инициализации.
        tab.styleProperty().bind(new SimpleStringProperty(""));
        pane.getTabs().add(tab);
        return tab;
    }

    private void count(int previous, int expected, String stage) {
        int actual = setterAttempts.size() - previous;
        System.out.printf("%s attempts=%d%n", stage, actual);
        if (actual != expected) failures.add(stage + " expected " + expected + ", got " + actual);
    }

    private void start() {
        var initial = tab("Initial");
        var pane = new JInvTabPane(initial);
        var stage = new Stage();
        stage.setScene(new Scene(new StackPane(pane), 360, 220));
        stage.show();
        Thread thread = Thread.currentThread();
        var old = thread.getUncaughtExceptionHandler();
        thread.setUncaughtExceptionHandler((t, error) -> {
            if (error instanceof RuntimeException && error.getMessage().contains("A bound value cannot be set")
                    && java.util.Arrays.stream(error.getStackTrace())
                            .anyMatch(frame -> frame.getClassName().equals(JInvTabPaneSkin.class.getName()))) {
                setterAttempts.add(error);
            } else {
                done.completeExceptionally(error);
            }
        });
        var top = counted(pane);
        later(() -> {
            count(0, 0, "initial TOP");
            pane.getTabs().remove(top);
            pane.setSide(Side.LEFT);
            var first = counted(pane);
            later(() -> {
                count(0, 1, "first LEFT");
                pane.getTabs().remove(first);
                int before = setterAttempts.size();
                for (int i = 0; i < 3; i++) {
                    pane.setSide(Side.TOP);
                    pane.setSide(Side.LEFT);
                }
                var repeated = counted(pane);
                var normal = tab("Normal");
                pane.getTabs().add(normal);
                pane.getSelectionModel().select(normal);
                later(() -> {
                    count(before, 1, "fourth LEFT");
                    require(normal.getStyleClass().contains("tab-left") && pane.getSelectionModel().getSelectedItem() == normal
                            && normal.getContent().getScene() == stage.getScene(), "normal tab style, selection and attached content");
                    pane.getTabs().remove(repeated);
                    int afterLeft = setterAttempts.size();
                    pane.setSide(Side.TOP);
                    var historical = counted(pane);
                    later(() -> {
                        count(afterLeft, 1, "TOP after LEFT");
                        pane.getTabs().remove(historical);
                        pane.setSide(Side.LEFT);
                        pane.setSkin(null);
                        pane.setSkin(new JInvTabPaneSkin(pane));
                        int afterReplacement = setterAttempts.size();
                        var replacement = counted(pane);
                        later(() -> {
                            count(afterReplacement, 1, "replacement already LEFT");
                            pane.getTabs().remove(replacement);
                            thread.setUncaughtExceptionHandler(old);
                            stage.hide();
                            require(failures.isEmpty(), "tab listener lifecycle: " + failures);
                            done.complete(null);
                        });
                    });
                });
            });
        });
    }

    private void later(Runnable action) {
        Platform.runLater(() -> {
            try { action.run(); } catch (Throwable error) { done.completeExceptionally(error); }
        });
    }

    public static void main(String[] args) {
        var smoke = new TabPaneSkinListenersSmoke();
        Platform.startup(() -> smoke.later(smoke::start));
        int exit = 1;
        try {
            smoke.done.get(25, TimeUnit.SECONDS);
            System.out.println("CTRL-054 PASS");
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
