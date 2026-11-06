package ru.inversion.fx.form.lov;
import java.lang.invoke.MethodHandles;
import java.util.function.BiConsumer;
import javafx.application.Platform;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputControl;
import javafx.stage.Stage;
import javafx.util.Pair;
import org.slf4j.Logger;
import static org.slf4j.LoggerFactory.getLogger;
import ru.inversion.fx.app.es.JInvErrorService;
import ru.inversion.fx.form.ViewContext;
import ru.inversion.fx.form.action.JInvKeyboardManager;
import ru.inversion.fx.form.controls.ILovValueControl;
import ru.inversion.fx.form.controls.JInvChoiceButton;
import ru.inversion.fx.form.controls.JInvTextArea;
import ru.inversion.fx.form.controls.JInvTextField;
import ru.inversion.icons.IBaseIconDescriptor;
import ru.inversion.icons.enums.FontAwesome;
import ru.inversion.utils.converter.TypeConverter;

/**
 *
 * @author ssu @
 */
public class JInvLOVButton extends JInvChoiceButton {
    public static final String LOV_BUTTON_DATA = "ru.inversion.lov_button.lov_data";
    private final static Logger logger = getLogger( MethodHandles.lookup().lookupClass() );

    /**
     *
     */
    private class LOVHandler/*<T extends TextInputControl & ILovValueControl & IJInvControl>*/ implements EventHandler {

        @Override
        public void handle(Event event) {
            try {

                Pair<TextInputControl, ILov> p = (Pair<TextInputControl, ILov>) getProperties().get( LOV_BUTTON_DATA );

                if (p != null) {

                    TextInputControl ed = p.getKey();

                    if (ed != null) {
                        Platform.runLater( () -> ed.requestFocus() );
                        if (ed instanceof JInvTextField || ed instanceof JInvTextArea) {
                            ILovValueControl field = (ILovValueControl) ed;
                            field.showLOV( ( t, u ) -> {
                                if (t) {
                                    // Отказались от setDisableNextValidation из-за JAVAKERNEL-1075
                                    JInvKeyboardManager.fireForwardEvent(null, ed);
                                } else {
                                    ed.requestFocus();
                                }
                            } );
                        } else {

                            ILov lov = p.getValue();
                            if (lov != null) {
                                lov.showChoiceList(ViewContext.of(getScene().getWindow()), ed.getText(), new BiConsumer<Boolean, ILov>() {
                                    @Override
                                    public void accept(Boolean t, ILov u) {
                                        if (t) {
                                            ed.setText(TypeConverter.convert(getLOV().getValue(), String.class));
                                            JInvKeyboardManager.fireForwardEvent(null, ed);
                                        }
                                    }

                                });

                            } else {
                                ed.requestFocus();
                            }
                        }

                    } else {
                        ILov lov = p.getValue();
                        if (lov != null) {
                            lov.showChoiceList(new ViewContext((Stage) getScene().getWindow()), null, (t, u) -> {
                            });
                            //lov.showChoiceList(new ViewContext((Stage) getScene().getWindow()), null, null);
                        }
                    }

                }
            } catch (Throwable th) {
                JInvErrorService.handleException(ViewContext.of(getScene().getWindow()), th);
            }
        }
    }

    /** */
    public JInvLOVButton( final FontAwesome icon, ILov lov ) {
        super( icon );
        init();
        setLOV(lov);
    }

    /** */
    public JInvLOVButton( final FontAwesome icon ) {
        super( icon );
        init();
    }

    public JInvLOVButton( final IBaseIconDescriptor complexIcon ) {
        super( complexIcon );
        init();
    }

    /**
     *
     */
    public JInvLOVButton() {
        init();
    }

    /**
     *
     */
    public JInvLOVButton(TextField ed) {
        init();
        setTextField(ed);
    }

    /**
     *
     */
    public JInvLOVButton(ILov lov) {
        init();
        setLOV(lov);
    }

    private void init(){
        setOnAction(new LOVHandler());
        setFocusTraversable(false);
    }
    /**
     *
     */
    @Override
    public void setTextField(TextInputControl ed) {

        super.setTextField(ed);
        if (ed != null) {


            getProperties().put( LOV_BUTTON_DATA, new Pair<>(ed, null));

            if (ed instanceof JInvTextField && ((JInvTextField) ed).externalButtonProperty().get() == null) {
                //Отключён (но виден), если editable = false и buttonIgnoresEditable = false
                disableProperty().bind(ed.editableProperty().not().and(((JInvTextField) ed).buttonIgnoresEditableProperty().not()));
                //Не виден, если readOnly = true, или (editable = false и buttonIgnoresEditable = false)
                visibleProperty().bind( ( (JInvTextField) ed ).readOnlyProperty()
                        .not()
                        .and( ed.editableProperty()
                            .or( ( (JInvTextField) ed ).buttonIgnoresEditableProperty() )
                ) );
            }
        }

    }

    /**
     *
     */
    @Override
    public TextInputControl getTextField() {
        Pair<TextInputControl, ILov> p = (Pair<TextInputControl, ILov>) getProperties().get( LOV_BUTTON_DATA );
        if (p != null) {
            return p.getKey();
        }
        return null;
    }

    /**
     *
     */
    public void setLOV(ILov lov) {
        if (lov != null) {
            getProperties().put( LOV_BUTTON_DATA, new Pair<>(null, lov));
            //Иной раз в специфическом лове требуются знания о контроле к которому оный привязан
            lov.onSetLov(this);
        }
    }

    /**
     *
     */
    public ILov getLOV() {
        Pair<TextInputControl, ILov> p = (Pair<TextInputControl, ILov>) getProperties().get( LOV_BUTTON_DATA );
        if (p != null) {
            return p.getValue();
        }
        return null;
    }

}
