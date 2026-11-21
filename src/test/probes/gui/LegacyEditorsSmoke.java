import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import javafx.animation.AnimationTimer;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.layout.VBox;
import javafx.stage.PopupWindow;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import ru.inversion.fx.form.controls.JInvComboBox;
import ru.inversion.fx.form.controls.JInvTextArea;
import ru.inversion.fx.form.controls.skin.JInvComboBoxListViewSkin;
import ru.inversion.fx.form.controls.skin.JInvTextAreaSkin;

public final class LegacyEditorsSmoke {
    private final CompletableFuture<Void> verdict = new CompletableFuture<>();
    private Stage stage;
    private JInvTextArea area;
    private JInvComboBox<String, String> combo;
    private boolean started;

    public static void main(String[] args) {
        var smoke = new LegacyEditorsSmoke();
        int exit = 1;
        try {
            require(Runtime.version().feature() == 25, "setup: Java 25 required");
            Platform.startup(() -> smoke.runChecked(smoke::show));
            smoke.verdict.get(30, TimeUnit.SECONDS);
            System.out.println("GUI-004 PASS");
            exit = 0;
        } catch (TimeoutException failure) {
            System.err.println("GUI-004 UNKNOWN: timed out waiting for focus or FX pulses. Use an interactive desktop.");
        } catch (Exception | AssertionError failure) {
            System.err.println("GUI-004 failed; see the setup, focus, or control diagnostic below.");
            failure.printStackTrace(System.err);
        } finally {
            Platform.exit();
        }
        System.exit(exit);
    }

    private void show() {
        Platform.setImplicitExit(false);
        Thread.currentThread().setUncaughtExceptionHandler((thread, failure) -> verdict.completeExceptionally(failure));
        String version = System.getProperty("javafx.version", "UNKNOWN");
        require(version.equals("25") || version.startsWith("25."), "setup: JavaFX 25 required: " + version);
        System.out.println("Java=" + Runtime.version() + " JavaFX=" + version);
        System.out.println("Legacy controls=" + JInvTextArea.class.getProtectionDomain().getCodeSource().getLocation());

        area = new JInvTextArea("first\nsecond");
        combo = new JInvComboBox<>();
        combo.setEditable(true);
        combo.getItems().setAll("ALPHA", "BETA");
        combo.setConverter(new StringConverter<>() {
            @Override public String toString(String value) { return value == null ? "" : value; }
            @Override public String fromString(String text) { return text.trim().toUpperCase(Locale.ROOT); }
        });

        var scene = new Scene(new VBox(8, area, combo), 420, 250);
        scene.getStylesheets().add(Objects.requireNonNull(getClass().getResource("/css/general.css"),
                "setup: production CSS missing").toExternalForm());
        stage = new Stage();
        stage.setTitle("GUI-004 Legacy editors smoke");
        stage.setScene(scene);
        stage.focusedProperty().addListener((property, oldValue, focused) -> {
            if (focused && !started) {
                afterPulse(() -> {
                    if (stage.isFocused() && !started) {
                        started = true;
                        checkEditors();
                    }
                });
            }
        });
        stage.show();
        stage.requestFocus();
    }

    private void checkEditors() {
        require(stage.isShowing() && area.getWidth() > 0 && area.getHeight() > 0
                && combo.getWidth() > 0 && combo.getHeight() > 0, "layout: controls are not laid out");
        require(area.getSkin() instanceof JInvTextAreaSkin, "setup: Legacy TextArea skin missing");
        require(combo.getSkin() instanceof JInvComboBoxListViewSkin<?>, "setup: Legacy ComboBox skin missing");

        area.requestFocus();
        require(area.isFocused(), "focus: TextArea did not receive focus");
        area.selectRange(6, 12);
        require("second".equals(area.getSelectedText()), "TextArea: unexpected selection");
        area.replaceSelection("edited");
        require("first\nedited".equals(area.getText()), "TextArea: text replacement failed");

        ContextMenu skinMenu = area.getContextMenu();
        require(skinMenu != null && !skinMenu.getItems().isEmpty(), "TextArea menu: skin menu unavailable");
        area.setOnContextMenuRequested(event -> event.consume());
        require(area.getContextMenu() == null, "TextArea menu: skin menu did not yield to request handler");
        var customMenu = new ContextMenu(new MenuItem("Custom"));
        area.setContextMenu(customMenu);
        area.setOnContextMenuRequested(null);
        require(area.getContextMenu() == customMenu, "TextArea menu: custom menu was replaced");
        area.setContextMenu(null);
        require(area.getContextMenu() == skinMenu, "TextArea menu: skin menu was not restored");

        combo.requestFocus();
        require(comboHasFocus() && !area.isFocused(), "focus: focus did not move to ComboBox");
        combo.getSelectionModel().select(0);
        requireComboValue(0, "ALPHA");
        combo.getEditor().setText(" beta ");
        combo.forceCommitValue();
        requireComboValue(1, "BETA");
        combo.getEditor().setText(" beta ");
        combo.forceCommitValue();
        requireComboValue(1, "BETA");
        System.out.println("TextArea edit/menu ownership and ComboBox selection, commit and same-value normalization passed");

        var skin = (JInvComboBoxListViewSkin<?>) combo.getSkin();
        combo.show();
        afterPulse(() -> checkPopup(skin));
    }

    private void checkPopup(JInvComboBoxListViewSkin<?> skin) {
        var content = skin.getPopupContent();
        require(combo.isShowing() && content.getScene() != null
                        && content.getScene().getWindow() instanceof PopupWindow popup
                        && popup.isShowing() && popup.getOwnerWindow() == stage,
                "ComboBox popup: no actual showing popup owned by smoke Stage after pulse");
        var popup = content.getScene().getWindow();
        combo.hide();
        afterPulse(() -> {
            require(!combo.isShowing() && !popup.isShowing(), "ComboBox popup: window did not hide");
            require(stage.isFocused() && comboHasFocus(), "focus: ComboBox did not retain focus after hide");
            area.requestFocus();
            require(area.isFocused() && !comboHasFocus(), "focus: could not return to TextArea after popup");
            System.out.println("ComboBox popup window shown and hidden; focus returned to TextArea");
            stage.close();
            verdict.complete(null);
        });
    }

    private boolean comboHasFocus() {
        var owner = stage.getScene().getFocusOwner();
        return owner == combo || owner == combo.getEditor();
    }

    private void requireComboValue(int index, String expected) {
        var selection = combo.getSelectionModel();
        require(selection.getSelectedIndex() == index && expected.equals(selection.getSelectedItem())
                        && expected.equals(combo.getValue()) && expected.equals(combo.getEditor().getText()),
                "ComboBox: expected index=" + index + ", value/item/editor=" + expected + "; got "
                        + selection.getSelectedIndex() + "/" + combo.getValue() + "/"
                        + selection.getSelectedItem() + "/" + combo.getEditor().getText());
    }

    private void afterPulse(Runnable action) {
        new AnimationTimer() {
            private boolean firstFrame = true;
            @Override public void handle(long now) {
                if (firstFrame) {
                    firstFrame = false;
                    return;
                }
                stop();
                if (!verdict.isDone()) runChecked(action);
            }
        }.start();
    }

    private void runChecked(Runnable action) {
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
