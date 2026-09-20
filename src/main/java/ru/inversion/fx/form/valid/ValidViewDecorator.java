package ru.inversion.fx.form.valid;
import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

import javafx.beans.property.BooleanProperty;
import javafx.collections.ObservableList;
import javafx.scene.Parent;
import javafx.scene.control.ComboBoxBase;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextInputControl;
import javafx.stage.Stage;
import org.slf4j.Logger;
import ru.inversion.fx.app.AppException;
import ru.inversion.fx.app.BaseApp;
import ru.inversion.fx.app.es.JInvErrorService;
import ru.inversion.fx.app.service.ViewPrefAppService;
import ru.inversion.fx.form.controls.Controls;

import static org.slf4j.LoggerFactory.getLogger;
import static ru.inversion.fx.form.controls.Controls.scrollToNode;
import ru.inversion.fx.form.controls.IJInvControl;

/**
 *
 * @author ssu @
 */
public class ValidViewDecorator {
    private static ValidViewDecorator instance;
     /** Синглтончик */
    public static ValidViewDecorator INSTANCE() {
        if (instance == null) {
            instance = new ValidViewDecorator();
        }
        return instance;
    }

    private final static String PROPERTY_MARK_TRACKING_TYPE = "ru.inversion.mark_tracking_type";
    private final static Logger logger = getLogger( MethodHandles.lookup().lookupClass() );

    public static final LabelStyleEnum DEFAULT_VALID_LABEL_STYLE = LabelStyleEnum.PLAIN;

    public enum LabelStyleEnum {

        PLAIN, BOLD, ITALIC, UNDERLINED, BOLD_ITALIC, BOLD_UNDERLINED, ITALIC_UNDERLINED, BOLD_ITALIC_UNDERLINED;

        static ResourceBundle bundle = ResourceBundle.getBundle("fore");

        @Override
        public String toString() {

            switch (this) {
                case BOLD:
                    return bundle.getString("SETTINGS_VALID_LABEL_BOLD");
                case ITALIC:
                    return bundle.getString("SETTINGS_VALID_LABEL_ITALIC");
                case UNDERLINED:
                    return bundle.getString("SETTINGS_VALID_LABEL_UNDERLINED");
                case BOLD_ITALIC:
                    return bundle.getString("SETTINGS_VALID_LABEL_BOLD_ITALIC");
                case BOLD_UNDERLINED:
                    return bundle.getString("SETTINGS_VALID_LABEL_BOLD_UNDERLINED");
                case ITALIC_UNDERLINED:
                    return bundle.getString("SETTINGS_VALID_LABEL_ITALIC_UNDERLINED");
                case BOLD_ITALIC_UNDERLINED:
                    return bundle.getString("SETTINGS_VALID_LABEL_BOLD_ITALIC_UNDERLINED");
                case PLAIN:
                    return bundle.getString("SETTINGS_VALID_LABEL_PLAIN");
                default:
                    return null;
            }
        }

        public String getStyle() {
            switch (this) {
                case BOLD:
                    return "-fx-font-weight: bold;";
                case ITALIC:
                    return "-fx-font-style: italic;";
                case UNDERLINED:
                    return "-fx-underline: true;";
                case BOLD_ITALIC:
                    return "-fx-font-weight: bold; -fx-font-style: italic;";
                case BOLD_UNDERLINED:
                    return "-fx-font-weight: bold; -fx-underline: true;";
                case ITALIC_UNDERLINED:
                    return "-fx-font-style: italic; -fx-underline: true;";
                case BOLD_ITALIC_UNDERLINED:
                    return "-fx-font-weight: bold; -fx-font-style: italic; -fx-underline: true;";
                case PLAIN:
                    return "";
                default:
                    return null;
            }
        }
    }

    private ViewPrefAppService prefService;
    private ViewPrefAppService getPrefService(){
        if ( prefService == null ){
            try {
                prefService = BaseApp.APP().getViewPrefService();
            } catch ( AppException e ) {
                JInvErrorService.handleException( BaseApp.APP().getPrimaryViewContext(), e );
            }
        }
        return prefService;
    }

    /**
     * Метод маркировки компонентов для валидации
     * Отслеживает изменения editable у контрола и соответственно убирает/возвращает раскраску
     * @param c компонент для маркировки
     * @param type значение из класса перечисления MarkType
     */
    public void markAndTrack(Control control, MarkType markType){
        if (getMarkTrackingType(control) == markType){
            return;
        }
        control.getProperties().put(PROPERTY_MARK_TRACKING_TYPE, markType);
        markControl(control, markType);
        addEditableUnmarkListener(control);
    }

    /**
     * Убрать раскраску и перестать отслеживать изменения editable
     */
    public void unmarkAndUntrack(MarkType type, Control... controls) {
        for (Control control : controls) {
            control.getProperties().put(PROPERTY_MARK_TRACKING_TYPE, MarkType.NONE);
        }
        unmarkControls(type,controls);
    }

    private void markControl(Control c, MarkType type) {
        if (type == MarkType.NONE) {
            return;
        }

        //Не делаем обводку валидируемых полей, коли настройка не велит
        if ( type == MarkType.VALIDATABLE && !getPrefService().isShowValidatable() ){
            return;
        }

        //Стандартная раскраска
        if ( canApplyStyle( c ) ) {
            final ObservableList<String> styleClass = c.getStyleClass();
            String cssClass = type.toString().toLowerCase();
            if ( !styleClass.contains( cssClass ) ) {
                styleClass.add( cssClass );
            }
        }
        //Доп. действия для "обязательных"
        if ( type == MarkType.REQUIRED ) {
            if ( c instanceof IJInvControl ) {
                Label l = ( (IJInvControl) c ).getLabel();
                if ( l != null ) {
                    try {
                        l.setStyle( BaseApp.APP().getViewPrefService().getStyleValidLabels().getStyle() );
                    } catch ( AppException e ) {
                        JInvErrorService.handleException( c.getScene().getWindow(), e );
                    }
                }
            }
        }
    }

    /** Вызывает markControlOnError(), если контрол в фокусе
     @see #markControlOnError(Control, Object)  */
    void markFocusedControlOnError( Control control, Object errorObject ) {
        if ( control.getScene() != null
          && control.getScene().getWindow() != null
          && control.getScene().getWindow() instanceof Stage
          && control.getScene().getWindow().focusedProperty().get() ){
            markControlOnError(control, errorObject);
        }
    }
    /** Метод маркировки ошибочных компонентов после валидации
     *
     * @param control компонент, где произошла ошибка
     * @param errorObject ошибка ( может быть Exception, Validator.Result or String )
     */
    public void markControlOnError(Control control, Object errorObject) {
//        logger.trace("markControlOnError: showing valid error tooltip");
        String errMessage = null;

        enableTabPane(control);
        scrollToNode(control);
        control.requestFocus();
        if (errorObject instanceof Validator.Result) {

            Validator.Result result = (Validator.Result) errorObject;
            if (result.getException() != null) {
                JInvErrorService.handleException(null, result.getException());
                return;
            } else {
                errMessage = result.getDescription();
            }
        } else if (errorObject instanceof Exception) {
            errMessage = ((Exception) errorObject).getLocalizedMessage();
        } else {
            errMessage = (String) errorObject;
        }

        JInvValidTooltip.showValidTooltip(control, errMessage);
    }

    private void unmarkControls(MarkType type, Control... controls) {
        if (type == MarkType.NONE) {
            return;
        }

        String cssClass = type.toString().toLowerCase();

        for ( final Control control : controls ) {
            //Стандартное снятие раскраски
            if ( canApplyStyle( control ) ) {
                control.getStyleClass().remove( cssClass );
            }
            //Доп. действия для "обязательных"
            if ( type == MarkType.REQUIRED ) {
                if ( control instanceof IJInvControl ) {
                    Label l = ( (IJInvControl) control ).getLabel();
                    if ( l != null ) {
                        l.setStyle( "" );
                    }
                }
            }
        }
    }

    private MarkType getMarkTrackingType(Control control){
        if (control == null){
            return MarkType.NONE;
        }
        return (MarkType) control.getProperties().getOrDefault(PROPERTY_MARK_TRACKING_TYPE, MarkType.NONE);
    }

    private void addEditableUnmarkListener( Control control ){
        Optional<BooleanProperty> optional = Controls.editableProperty(control);
        optional.ifPresent(editableProperty -> {
            editableProperty.addListener((v, o, n) -> {

                if (o == n) {
                    return;
                }

                onEditableChanged(control, n);
            });

            if (!editableProperty.get()) {
                onEditableChanged(control, false);
            }
        });
    }

    private void onEditableChanged(Control control, Boolean isEditable) {
        MarkType markType = getMarkTrackingType(control);

        if (markType == MarkType.NONE) {
            return;
        }

        if (isEditable) {
            markControl(control, markType);
        } else {
            unmarkControls(markType, control);
        }
    }

    private boolean canApplyStyle( final Control c ) {
        return c instanceof TextInputControl || c instanceof ComboBoxBase;
    }

    void unmarkControlOnError(Control control) {
        JInvValidTooltip.closeValidTooltip();
    }

    public static void enableTabPane(Control c) {

        List<Parent> listParent = Controls.getParentStreamOfControl(c).collect(Collectors.toList());

//         System.out.println("before loop " + listParent.size());
        for (int i = 0; i < listParent.size(); i++) {

            Parent parent = listParent.get(i);
            if (parent instanceof TabPane) {

                TabPane tabPane = (TabPane) parent;
                Parent content = listParent.get(i - 2);
                if (content != null) {

                    Tab tab = tabPane.getTabs().stream().filter((Tab t) -> t.getContent().equals(content)).findFirst().orElse(null);
                    if (tab != null) {
                        tabPane.getSelectionModel().select(tab);
//                        System.out.println("select Tab " + tab);
                    }
                }
            }
        }
    }
}
