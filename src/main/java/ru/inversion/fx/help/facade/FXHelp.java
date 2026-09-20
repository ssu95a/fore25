package ru.inversion.fx.help.facade;
import static java.lang.invoke.MethodHandles.lookup;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.ResourceBundle;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.Cursor;
import javafx.scene.control.Control;
import javafx.scene.control.Tooltip;
import javafx.stage.WindowEvent;
import org.slf4j.Logger;
import static org.slf4j.LoggerFactory.getLogger;
import ru.inversion.fx.app.sec.JInvSecurityService;
import ru.inversion.fx.form.AbstractBaseController;
import ru.inversion.fx.form.Alerts;
import ru.inversion.fx.form.FXFormLauncher;
import ru.inversion.fx.form.JInvFXFormController;
import ru.inversion.fx.form.ViewContext;
import ru.inversion.fx.form.controls.Controls;
import ru.inversion.fx.form.controls.IJInvControl;
import ru.inversion.fx.help.dao.HelpTooltips;
import ru.inversion.fx.help.entity.PHelpBundle;
import ru.inversion.tc.TaskContext;
import ru.inversion.utils.S;

/**
 * Класс обеспечивает унифицированный интерфейс для Справочной системы.
 * <p>
 * @author perov
 * @version 1.0.0
 */
public class FXHelp {
    private final static Logger logger = getLogger(lookup().lookupClass());
    private static final ResourceBundle BNDL = ResourceBundle.getBundle("ru.inversion.fx.help.fxml.help");
    public static final String HELP_FORM_CONTROLLSER_CLASS = "HELP_CONTROLLSER_CLASS";
    public static final String HELP_FORM_NAME_PROPERTY = "FORM_NAME";
    public static final String TOOLTIP_HELP_TEXT = "TOOLTIP_HELP_TEXT";
    public static final String TOOLTIP_TEXT = "TOOLTIP_TEXT";

    /**
     * Метод показывает справочную информацию о форме.
     */
    public static void showHelp(TaskContext taskContext, ViewContext viewContext, String formName, Class clazz) {
        Map param = new HashMap();
        param.put(HELP_FORM_NAME_PROPERTY, formName);
        param.put(HELP_FORM_CONTROLLSER_CLASS, clazz);
        new FXFormLauncher(taskContext, viewContext, "ru/inversion/fx/help/fxml/HelpPane.fxml").modal(true).bundle(BNDL).initProperties(param).show();
    }

    /**
     * Метод показывает всплывающее окно содержащее справочную информацию о компоненте.
     *
     * @param control компонента
     * @param x x - координата окна
     * @param y у - координата окна
     */
    public static void showTooltipHelp( Control control, double x, double y ) {
        if ( control == null ) {
            return;
        }
        JInvFXFormController controller = Controls.getControllerFromControl( control );
        if ( controller == null ) {
            return;
        }
        Tooltip tooltip = control.getTooltip();
        final String textHelp = getTextHelp( controller.getTaskContext(), control, controller.getViewContext().getFormName() );
        if ( isSuitable( tooltip ) ) {
            tooltip.getProperties().put( TOOLTIP_TEXT, tooltip.getText() );
        } else {
            tooltip = new Tooltip( textHelp );
        }
//        logger.info( "setting text to {}", textHelp );
        applyAndShow( control, x, y, tooltip );
        //Меняем текст тултипа после начала показа, иначе что-то другое его перезаписывает
        if ( isSuitable( tooltip ) )  {
            tooltip.setText( textHelp );
        }
    }

    private static boolean isSuitable( final Tooltip tooltip ) {
        return tooltip != null && !tooltip.getText().isEmpty();
    }

    private static void applyAndShow( final Control control, final double x, final double y, final Tooltip tooltip ) {
        tooltip.setWrapText(true);
        tooltip.setOnShown((WindowEvent event) -> tooltip.getScene().setCursor( Cursor.DEFAULT) );
        tooltip.autoHideProperty().set(true);
        tooltip.focusedProperty().addListener(new ChangeListenerImpl(tooltip));
        tooltip.show(control.getScene().getWindow(), x, y);
//        logger.info( "showing text {}", tooltip.getText() );
    }

    private static boolean canHaveHelp( final Control control ) {
        return control != null && control.getId() != null;
    }

    /**
     * Метод показывает окно редактирования справочной информации компоненты.
     *
     * @param control компонента
     */
    public static void showDialogTooltipHelp(Control control) {
        //Если у контрола нет fx:id, показывать диалог бессмысленно
        if ( !canHaveHelp( control ) ) {
            Alerts.info( control.getScene().getWindow(), BNDL.getString( "NO_FX_ID" ));
            return;
        }
        JInvFXFormController controller = Controls.getControllerFromControl(control);
        if (controller != null) {
            //Проверяем права
            if ( JInvSecurityService.isCanAccessIsAction( controller.getTaskContext(), 3454 ) ) {
                final String id = control.getId();
                PHelpBundle pojo = HelpTooltips.getControlTooltip( controller.getTaskContext().getConnection(), controller.getViewContext().getFormName(), id );
                if ( pojo == null ) {
                    pojo = new PHelpBundle();
                    pojo.setFORM( controller.getViewContext().getFormName() );
                    pojo.setCNTR_NAME( id );
                    showTooltipDialog( controller, control, pojo, AbstractBaseController.FormModeEnum.VM_INS );
                    return;
                }
                showTooltipDialog( controller, control, pojo, AbstractBaseController.FormModeEnum.VM_EDIT );
            } else {
               logger.info("Tooltip edit access denied");
            }
        }
    }

    private static void showTooltipDialog( final JInvFXFormController controller, final Control control,
            final PHelpBundle pHelpBundle, AbstractBaseController.FormModeEnum formMode ) {
        new FXFormLauncher<>(controller.getTaskContext(),
                controller.getViewContext(),
                "ru/inversion/fx/help/fxml/DialogEditTooltip.fxml")
                .dataObject( pHelpBundle )
                .bundle(BNDL)
                .dialogMode( formMode )
                .callback( ( formReturnEnum, objectJInvFXFormController ) -> {
                    if ( formReturnEnum == AbstractBaseController.FormReturnEnum.RET_OK ){
                        tryCacheTooltip( control.getTooltip(), pHelpBundle.getHTML_TEXT() );
                    }
                } )
                .show();
    }

    private static String getTextHelp(TaskContext tc, Control control, String name) {

        if ( !canHaveHelp( control ) && control instanceof IJInvControl ) {
            final Optional<String> toolTipText = Optional.ofNullable(  ((IJInvControl)control).getToolTipText() );
            if (toolTipText.isPresent()){
                return toolTipText.get();
            }
        }
        Tooltip tooltip = control.getTooltip();
        //From properties
        if (tooltip != null) {
            final Object helpText = tooltip.getProperties().get( TOOLTIP_HELP_TEXT );
            if ( helpText != null) {
                return helpText.toString();
            }
        }
        //From DB
        String helpText = HelpTooltips.getControlHelpText(tc.getConnection(), name, control.getId());
        tryCacheTooltip( tooltip, helpText );
        return helpText == null ? BNDL.getString("TEXT_NOT_SET") : helpText;
    }

    private static void tryCacheTooltip( final Tooltip tooltip, final String helpText ) {
        if (tooltip == null) {
            return;
        }
        if ( S.isNullOrEmpty(helpText) ) {
            tooltip.getProperties().remove( TOOLTIP_HELP_TEXT );
        } else {
            tooltip.getProperties().put( TOOLTIP_HELP_TEXT, helpText);
        }
    }

    private static class ChangeListenerImpl implements ChangeListener<Boolean> {

        private final Tooltip tooltip;

        public ChangeListenerImpl(Tooltip tooltip) {
            this.tooltip = tooltip;
        }

        //Сработали разок – и хватит
        boolean triggeredOnce = false;

        @Override
        public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
            if ( triggeredOnce ){
                return;
            }

            if (!newValue && tooltip != null) {
                final Object tooltipProperty = tooltip.getProperties().get( TOOLTIP_TEXT );
                if ( tooltipProperty != null) {
//                    logger.info( "changing from {} to {}", tooltip.getText(), tooltipProperty.toString() );
                    tooltip.setText( tooltipProperty.toString());
                }

                triggeredOnce = true;
            }
        }
    }

}
