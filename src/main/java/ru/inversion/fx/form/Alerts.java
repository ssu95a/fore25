package ru.inversion.fx.form;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TextArea;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.Modality;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.Window;
import org.slf4j.Logger;
import ru.inversion.fx.app.BaseApp;
import ru.inversion.fx.app.sound.SoundPlayer;
import ru.inversion.utils.S;
import ru.inversion.utils.U;

import java.lang.invoke.MethodHandles;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

import static org.slf4j.LoggerFactory.getLogger;
import static ru.inversion.fx.app.service.ViewPrefAppService.localizeDialog;

/**
 *
 * @author ssu
 */
public class Alerts {

    //private final static Logger logger = getLogger(MethodHandles.lookup().lookupClass());

    final static private ResourceBundle g_bundle = ResourceBundle.getBundle("alerts");

    static public ButtonType showAlert(
        Object windowContainer,
        Alert.AlertType alertType,
        String title,
        String headerText,
        String contentText,
        Node graphic,
        ButtonType... buttons
    ) {
        return showAlert( windowContainer, alertType, title, headerText, (Object) contentText, graphic, buttons );
    }

    /**
     @param detailText String / List< String > / String[]
     */
    static public ButtonType showAlert(
            Object windowContainer,
            Alert.AlertType alertType,
            String title,
            String headerText,
            String contentText,
            Object detailText,
            Node graphic,
            ButtonType... buttons
    ) {
        return showAlert(windowContainer, alertType, title, headerText, (Object) contentText, detailText, graphic, buttons);
    }

    /**
     @param contentText String или Node, в случае Node оный подставляется вместо исходной TextArea
     */
    static public ButtonType showAlert(
            Object windowContainer,
            Alert.AlertType alertType,
            String title,
            String headerText,
            Object contentText,
            Node graphic,
            ButtonType... buttons
    ) {
        return showAlert(windowContainer, alertType, title, headerText, contentText, null, graphic, buttons);
    }

    /**
     @param contentText String или Node, в случае Node оный подставляется вместо исходной TextArea
     @param detailText String / List< String > / String[]
     */
    static public ButtonType showAlert(
            Object windowContainer,
            Alert.AlertType alertType,
            String title,
            String headerText,
            Object contentText,
            Object detailText,
            Node graphic,
            ButtonType... buttons
    ) {
        Modality defaultModality = Modality.WINDOW_MODAL;

        Alert alert = getAlert( windowContainer, alertType, title, headerText, contentText, detailText, graphic, defaultModality, buttons );

        return alert.showAndWait().orElse(ButtonType.CANCEL);
    }

    static JInvAlert getAlert(
            final Object windowContainer,
            final Alert.AlertType alertType,
            final String title,
            final String headerText,
            final Object contentText,
            final Object detailText,
            final Node graphic,
            final Modality modality,
            final ButtonType... buttons
    ) {
        requireFxThread();
        JInvAlert alert = new JInvAlert( alertType, "", buttons );
        Window window = configure(alert, windowContainer, alertType, title, headerText,
                contentText, detailText, graphic, modality);
        initBeforeShow(alertType, alert, window);
        return alert;
    }

    private static Window configure(
            Alert alert, Object windowContainer, Alert.AlertType alertType, String title,
            String headerText, Object contentText, Object detailText, Node graphic, Modality modality
    ) {
        alert.setResizable(true);
        Window window = ViewContext.tryGetWindow(windowContainer);

        //if( BaseApp.APP().isAfterLogin() )
        {
            //Защита от бага FX (HeavyweightDialog.java:329) - JAVAKERNEL-1572
            if (!(window instanceof Stage) || window.getScene() != null)
            {
                U.callIfNotNull(alert::initOwner, window);
            }
        }
        alert.setTitle(U.nvl(title, g_bundle.getString("TITLE_" + alertType.name())));
        alert.setHeaderText(headerText);
        U.callIfNotNull(alert::setGraphic, graphic);
        if (contentText != null)
        {
            if (contentText instanceof Node) {
                setNode4Content((Node) contentText, alert);
            } else {
                setTextArea4Content(contentText.toString(), alert, screenFor(window));
            }
        }
        String expandText = null;
        if( detailText != null )
        {
            if (detailText instanceof String) {
                expandText = (String) detailText;
            } else if (detailText instanceof String[]) {
                List<String> sa = Arrays.asList((String[]) detailText);
                expandText = String.join("\n", sa);
            } else if (detailText instanceof List) {
                List<String> sa = (List<String>) detailText;
                expandText = String.join("\n", sa);
            }
            else if( detailText instanceof Node ) {
                alert.getDialogPane().setExpandableContent( (Node)detailText );
                alert.getDialogPane().setExpanded(true);
            }
        }//end if
        if (expandText != null) {
            //Label label = new Label("Подробности:");

            TextArea textArea = messageArea(screenFor(window));
            textArea.setText(expandText);
            GridPane.setVgrow(textArea, Priority.ALWAYS);
            GridPane.setHgrow(textArea, Priority.ALWAYS);

            GridPane expContent = new GridPane();
            expContent.setMaxWidth(Double.MAX_VALUE);
            //expContent.add( label,    0, 0 );
            expContent.add(textArea, 0, 1);
            alert.getDialogPane().setExpandableContent(expContent);
        }//end if
        alert.initModality(modality);
        localizeDialog(alert.getDialogPane());
        installEnterHandler(alert);
        return window;
    }

    private static Screen screenFor(Window window) {
        if (window != null) {
            var screens = Screen.getScreensForRectangle(
                    window.getX(), window.getY(), window.getWidth(), window.getHeight());
            if (!screens.isEmpty()) {
                return screens.getFirst();
            }
        }
        return Screen.getPrimary();
    }

    private static TextArea messageArea(Screen screen) {
        var area = new TextArea();
        area.setWrapText(true);
        area.setEditable(false);
        var bounds = screen.getVisualBounds();
        // Оставляем место для второй текстовой области, заголовка и кнопок.
        double heightLimit = bounds.getHeight() / 3;
        double widthLimit = bounds.getWidth() * 0.8;
        area.setMaxHeight(heightLimit);
        area.setMaxWidth(widthLimit);
        var measure = new Text();
        measure.fontProperty().bind(area.fontProperty());
        measure.textProperty().bind(area.textProperty());
        measure.wrappingWidthProperty().bind(Bindings.createDoubleBinding(() -> {
            double width = area.getWidth() > 0 ? area.getWidth() : Math.min(area.prefWidth(-1), widthLimit);
            return Math.max(1, width - area.getInsets().getLeft() - area.getInsets().getRight()
                    - 2 * area.getFont().getSize());
        }, area.widthProperty(), area.insetsProperty(), area.fontProperty()));
        area.prefHeightProperty().bind(Bindings.createDoubleBinding(
                () -> Math.min(heightLimit, measure.getLayoutBounds().getHeight()
                        + 2 * area.getFont().getSize()),
                measure.layoutBoundsProperty(), area.fontProperty()));
        return area;
    }

    private static void installEnterHandler(Alert alert) {
        var pane = alert.getDialogPane();
        // Enter на кнопке диалога должен выбрать именно эту кнопку.
        pane.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() != KeyCode.ENTER || !(event.getTarget() instanceof Button button)) {
                return;
            }
            for (ButtonType type : pane.getButtonTypes()) {
                if (pane.lookupButton(type) == button) {
                    event.consume();
                    button.fire();
                    return;
                }
            }
        });
    }

    public class Builder{

    }


    /**
     Общий код инициализации алертов перед показом
     */
    private static void initBeforeShow( final Alert.AlertType alertType, final Alert alert, final Window window ) {
        playSoundIfLoggedIn(alertType);
        bringOwnerForward(window);
    }

    private static void playSoundIfLoggedIn(Alert.AlertType type) {
        BaseApp app = BaseApp.APP();
        if (app != null && app.isAfterLogin()) {
            SoundPlayer.playSound(type);
        }
    }

    private static void bringOwnerForward(Window window) {
        if (window instanceof Stage stage) {
            stage.setIconified(false);
            if (stage.isShowing()) {
                stage.toFront();
            }
        }
    }

    private static void requireFxThread() {
        if (!Platform.isFxApplicationThread()) {
            throw new IllegalStateException("Alerts must be shown on the JavaFX Application Thread");
        }
    }

    /**
     * Устанавливаем текстарея для текста сообщения
     *
     * @param contentText
     * @param alert
     */
    private static void setTextArea4Content(String contentText, Alert alert, Screen screen) {

        if( contentText != null && !contentText.isEmpty() ) {

            alert.setContentText(contentText);

            VBox vbox = new VBox();
            vbox.setPadding(new Insets(5));

            TextArea textArea = messageArea(screen);
            vbox.getChildren().add(textArea);
            VBox.setVgrow(textArea, Priority.ALWAYS);
            alert.getDialogPane().setContent(vbox);
            textArea.textProperty().bind(alert.contentTextProperty());
        }
    }

    /**
     *
     */
    private static void setNode4Content(Node contentNode, Alert alert) {

        if (contentNode != null) {
            VBox vbox = new VBox();
            vbox.setPadding(new Insets(5));
            vbox.getChildren().add(contentNode);
            VBox.setVgrow(contentNode, Priority.ALWAYS);

            alert.getDialogPane().setContent(vbox);
        }
    }

    /**
     *
     */
    static private int getLineCount( String str ) {
        int len = 2;

        if (!S.isNullOrEmpty(str)) {

            if (str.contains("\n")) {
                len = str.split("\n").length;
            } else {
                len = str.length() / 60;
            }
        }

        return Math.max(len, 2);
    }

    /**
     *
     */
    static public Optional<Boolean> yesNoCancel( Object parentWindow, String title, String headerText, String contentText, Object detailText, Node graphic) {

        ButtonType bt = showAlert(
            parentWindow,
            Alert.AlertType.CONFIRMATION,
            title,
            headerText,
            contentText,
            detailText,
            graphic,
            ButtonType.YES, ButtonType.NO, ButtonType.CANCEL
        );

        if (bt == ButtonType.YES) {
            return Optional.of(Boolean.TRUE);
        }
        if (bt == ButtonType.NO) {
            return Optional.of(Boolean.FALSE);
        }
        return Optional.empty();
    }

    static public Optional<Boolean> yesNoCancel(Object parentWindow, String title, String headerText, String contentText, Object detailText) {
        return yesNoCancel(parentWindow, title, headerText, contentText, detailText, null);
    }

    static public Optional<Boolean> yesNoCancel(Object parentWindow, String title, String headerText, String contentText) {
        return yesNoCancel(parentWindow, title, headerText, contentText, null);
    }

    static public Optional<Boolean> yesNoCancel(Object parentWindow, String title, String headerText) {
        return yesNoCancel(parentWindow, title, headerText, null);
    }

    static public Optional<Boolean> yesNoCancel(Object parentWindow, String headerText) {
        return yesNoCancel(parentWindow, null, headerText);
    }

    /**
     *
     */
    static public boolean yesNo(Object parentWindow, String title, String headerText, String contentText, Object detailText, Node graphic) {
        return showAlert(parentWindow, Alert.AlertType.CONFIRMATION, title, headerText, contentText, detailText, graphic, ButtonType.YES, ButtonType.NO) == ButtonType.YES;
    }

    static public boolean yesNo(Object parentWindow, String title, String headerText, String contentText, Object detailText) {
        return yesNo(parentWindow, title, headerText, contentText, detailText, null);
    }

    static public boolean yesNo(Object parentWindow, String title, String headerText, String contentText) {
        return yesNo(parentWindow, title, headerText, contentText, null);
    }

    static public boolean yesNo(Object parentWindow, String title, String headerText) {
        return yesNo(parentWindow, title, headerText, null);
    }

    static public boolean yesNo(Object parentWindow, String headerText) {
        return yesNo(parentWindow, null, headerText);
    }

    /**
     *
     */
    static public void info(Object parentWindow, String title, String headerText, String contentText, Object detailText, Node graphic) {
        showAlert(parentWindow, Alert.AlertType.INFORMATION, title, headerText, contentText, detailText, graphic, ButtonType.OK);
    }

    static public void info(Object parentWindow, String title, String headerText, String contentText, Object detailText) {
        info(parentWindow, title, headerText, contentText, detailText, null);
    }

    static public void info(Object parentWindow, String title, String headerText, String contentText) {
        info(parentWindow, title, headerText, contentText, null);
    }

    static public void info(Object parentWindow, String title, String headerText) {
        info(parentWindow, title, headerText, null);
    }

    static public void info(Object parentWindow, String headerText) {
        info(parentWindow, null, headerText);
    }

    /** */
    static public void error( Object parentWindow, String title, String headerText, String contentText, Object detailText, Node graphic) {
        showAlert(parentWindow, Alert.AlertType.ERROR, title, headerText, contentText, detailText, graphic, ButtonType.OK);
    }
    /** */
    static public void error( Object parentWindow, String title, String headerText, String contentText, Object detailText) {
        error(parentWindow, title, headerText, contentText, detailText, null);
    }
    /** */
    static public void error( Object parentWindow, String title, String headerText, String contentText) {
        error( parentWindow, title, headerText, contentText, null);
    }
    /** */
    static public void error( Object parentWindow, String title, String headerText) {
        error( parentWindow, title, headerText, null);
    }
    /** */
    static public void error( Object parentWindow, String headerText) {
        error(parentWindow, null, headerText);
    }

    /** fluent zone */
    public static IAlertMaker maker() {
        return new AlertMaker();
    }

    public interface IAlertMaker {
        /** */
        IAlertMaker window( Object w );
        /** */
        IAlertMaker type  ( Alert.AlertType type );
        /** */
        IAlertMaker title ( String title );
        /** */
        IAlertMaker headerText  ( String headerText );
        /** */
        IAlertMaker content     ( Object content );
        /** */
        IAlertMaker contentText ( String contentText );
        /** */
        IAlertMaker detail      ( Object detail );
        /** */
        IAlertMaker checkBoxText( String detailText );
        /** */
        IAlertMaker graphic( Node graphic );
        /** */
        IAlertMaker modality( Modality modality );
        /** */
        IAlertMaker buttons( ButtonType... buttons );
        /** */
        <T> Optional<T> showAndWait();

        /** Возвращает фактически выбранную кнопку. */
        default Optional<ButtonType> showButtonAndWait() {
            throw new UnsupportedOperationException();
        }

        /** Назначает кнопку по умолчанию из списка кнопок диалога. */
        default IAlertMaker defaultButton(ButtonType button) {
            throw new UnsupportedOperationException();
        }

        /** Управляет воспроизведением звука с учетом настроек приложения. */
        default IAlertMaker sound(boolean enabled) {
            throw new UnsupportedOperationException();
        }

        /** Задает начальное состояние области подробностей. */
        default IAlertMaker expanded(boolean expanded) {
            throw new UnsupportedOperationException();
        }

        /** Разрешает изменение размеров окна пользователем. */
        default IAlertMaker resizable(boolean resizable) {
            throw new UnsupportedOperationException();
        }
    }

    private static final class AlertMaker implements IAlertMaker {
        private Object window;
        private Alert.AlertType type = Alert.AlertType.INFORMATION;
        private String title;
        private String headerText;
        private Object content;
        private Object detail;
        private Node graphic;
        private Modality modality = Modality.WINDOW_MODAL;
        private ButtonType[] buttons;
        private ButtonType defaultButton;
        private boolean sound = true;
        private Boolean expanded;
        private boolean resizable = true;

        @Override
        public IAlertMaker window(Object window) {
            this.window = window;
            return this;
        }

        @Override
        public IAlertMaker type(Alert.AlertType type) {
            this.type = java.util.Objects.requireNonNull(type);
            return this;
        }

        @Override
        public IAlertMaker title(String title) {
            this.title = title;
            return this;
        }

        @Override
        public IAlertMaker headerText(String headerText) {
            this.headerText = headerText;
            return this;
        }

        @Override
        public IAlertMaker content(Object content) {
            this.content = content;
            return this;
        }

        @Override
        public IAlertMaker contentText(String contentText) {
            return content(contentText);
        }

        @Override
        public IAlertMaker detail(Object detail) {
            this.detail = detail;
            return this;
        }

        @Override
        public IAlertMaker checkBoxText(String text) {
            throw new UnsupportedOperationException();
        }

        @Override
        public IAlertMaker graphic(Node graphic) {
            this.graphic = graphic;
            return this;
        }

        @Override
        public IAlertMaker modality(Modality modality) {
            this.modality = java.util.Objects.requireNonNull(modality);
            return this;
        }

        @Override
        public IAlertMaker buttons(ButtonType... buttons) {
            this.buttons = buttons == null ? null : buttons.clone();
            return this;
        }

        @Override
        public IAlertMaker defaultButton(ButtonType button) {
            defaultButton = java.util.Objects.requireNonNull(button);
            return this;
        }

        @Override
        public IAlertMaker sound(boolean enabled) {
            sound = enabled;
            return this;
        }

        @Override
        public IAlertMaker expanded(boolean expanded) {
            this.expanded = expanded;
            return this;
        }

        @Override
        public IAlertMaker resizable(boolean resizable) {
            this.resizable = resizable;
            return this;
        }

        /** Результат совместимого обобщенного метода имеет тип ButtonType. */
        @Override
        @SuppressWarnings("unchecked")
        public <T> Optional<T> showAndWait() {
            return (Optional<T>) showButtonAndWait();
        }

        @Override
        public Optional<ButtonType> showButtonAndWait() {
            requireFxThread();
            Alert alert = new Alert(type, "", buttons);
            Window owner = configure(alert, window, type, title, headerText,
                    content, detail, graphic, modality);
            alert.setResizable(resizable);
            if (expanded != null) {
                alert.getDialogPane().setExpanded(expanded);
            }
            if (defaultButton != null) {
                var pane = alert.getDialogPane();
                if (!pane.getButtonTypes().contains(defaultButton)) {
                    throw new IllegalArgumentException("Default button is absent from the dialog");
                }
                for (ButtonType choice : pane.getButtonTypes()) {
                    ((Button) pane.lookupButton(choice)).setDefaultButton(choice == defaultButton);
                }
            }
            if (sound) {
                alert.setOnShown(event -> playSoundIfLoggedIn(alert.getAlertType()));
            }
            bringOwnerForward(owner);
            return alert.showAndWait();
        }
    }
}
