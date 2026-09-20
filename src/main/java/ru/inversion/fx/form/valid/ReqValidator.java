package ru.inversion.fx.form.valid;

import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import org.slf4j.Logger;
import ru.inversion.fx.app.es.JInvErrorService;
import ru.inversion.fx.form.controls.Controls;
import ru.inversion.fx.form.controls.IJInvControl;

import java.util.*;
import java.util.function.Function;

import static java.lang.invoke.MethodHandles.lookup;
import static org.slf4j.LoggerFactory.getLogger;

/**
 *
 * @author ssu @
 */
public class ReqValidator {

    final static public String CUSTOM_CONTROL_VALIDATOR =  "r.i.v.r.c.cv";

    private static final Logger logger = getLogger(lookup().lookupClass());
    private static final Comparator<Control> comparator = Comparator
//          Сортируем по табам, если они есть
            .comparing( ( Control control ) -> {
                int tabIndex = findTabIndexForNode( control );
                return tabIndex;
            } )
//          Сортируем по высоте
            .thenComparing( control -> control.localToScene( control.getBoundsInLocal() ).getMinY() )
//          Сортируем по ширине
            .thenComparing( control -> control.localToScene( control.getBoundsInLocal() ).getMinX() )
//          Не теряем контролы, даже если всё остальное вдруг совпало
            .thenComparing( Object::hashCode );

    //Хитрая сортировка контролов по позиции на форме
    private final Set<Control> controls = new HashSet<>();

    /** */
    private static int findTabIndexForNode( Node node )
    {
        TabPane tabPane = null;
        Node tabContentRegion = null;
        int tabIndex = -1;

        for( Node n = node.getParent(); n != null && tabPane == null; n = n.getParent() )
        {
            if( n.getStyleClass().contains("tab-content-area") ) {
                tabContentRegion = n;
            }
            if (n instanceof TabPane) {
                tabPane = (TabPane) n;
            }
        }
        if (tabPane != null && tabContentRegion != null) {
            ObservableList<Tab> tabList = tabPane.getTabs();

            for (Tab t : tabList) {
                if (t.getContent().getParent().equals(tabContentRegion)) {
                    tabIndex = tabList.indexOf( t );
                    break;
                }
            }
        }
        return tabIndex;
    }

    /** Добавляет кастомную логику проверки, не заполненного поля */
    public boolean setCustomValidator( Control c, Function<Control, Boolean> vldtr )
    {
        if( c == null || !controls.contains(c) )
            return false;

        if( vldtr == null )
            c.getProperties().remove( CUSTOM_CONTROL_VALIDATOR );
        else
            c.getProperties().put( CUSTOM_CONTROL_VALIDATOR, vldtr );

        return true;
    }

    /** */
    public void addControls( Control ... c )
    {
        if( controls.isEmpty() )
        {
            if( c.length == 1 )
                controls.add( c[0] );
            else
                controls.addAll( Arrays.asList(c) );
        }
        else
        {
            for( Control control : c )
            {
                if( !controls.contains(control) )
                     controls.add(control);
            }// end for
        }
    }

    /** */
    public void removeControls(Control... c) {
        controls.removeAll(Arrays.asList(c));
    }

    /** */
    boolean isValidatable(Control c) {
        return !c.isDisable() && c.isVisible() && c.getScene() != null && Controls.isEditable(c);
    }

    /** */
    void markControls() {
        ValidViewDecorator decorator = ValidViewDecorator.INSTANCE();
        if (decorator != null) {
            controls.stream().filter( c -> !c.isDisable() ).forEach( c -> {
                try {
                    decorator.markAndTrack(c, MarkType.REQUIRED );
                } catch (Throwable ex) {
                    JInvErrorService.handleException("", ex);
                }
            });
        }
    }

    /** */
    private String composeErrorMessage( final Control c )
    {
        String errorMessage;

        if( c instanceof IJInvControl && ((IJInvControl) c).getLabel() != null )
        {
            Label label = ((IJInvControl) c).getLabel();
            String nameOfControl = label.getText();
            errorMessage = JInvValidLocalized.getLocalMessage( "REQVERRLABELED", nameOfControl );
        }
        else
        {
            errorMessage = JInvValidLocalized.getLocalMessage( "REQVERR" );
        }
        return errorMessage;
    }

    /** */
    public boolean validate(ValidViewDecorator decorator) {

        boolean ok = true;

        List<Control> sortedControls = new ArrayList<>( this.controls );
        sortedControls.sort( comparator );

        for( Control c : sortedControls ) {

            if (!isValidatable(c)) {
                continue;
            }

            c.requestFocus();
            Object value = Controls.getValue(c);

            if( value == null || ( value instanceof String && ((String) value).isEmpty() ) )
            {
                final Function<Control, Boolean> vldtr = (Function< Control, Boolean >)c.getProperties().get(CUSTOM_CONTROL_VALIDATOR);
                ok = (vldtr == null) ? false : vldtr.apply(c);
            }
            //
            if( !ok )
            {
                decorator.markControlOnError( c, composeErrorMessage( c ) );
                break;
            }
        }
        return ok;
    }

}
