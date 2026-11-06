package ru.inversion.fx.form.controls.dsbar;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.util.StringConverter;
import ru.inversion.dataset.aggr.AggrFuncEnum;
import ru.inversion.fx.form.controls.JInvLongField;
import ru.inversion.fx.form.controls.JInvMoneyField;
import ru.inversion.icons.IconFactory;
import ru.inversion.icons.enums.FontAwesome;
import ru.inversion.utils.U;

import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

import static ru.inversion.fx.form.controls.dsbar.DSInfoBar.PartEnum.*;

/**
 *
 * @author ssu
 */
public abstract class AbstractPartBase implements IDSBarPart {

    /** */
    final protected static ResourceBundle g_bundle = ResourceBundle.getBundle("fore");

    /** */
    final public static String SORT_INDEX = "ds.bar.index";

    /** */
    final public static String PART = "ds.bar.part";

    /** */
    final public static String PART_STYLE = "-fx-border-width:  1;-fx-border-color: lightgray;-fx-border-radius: 5;";

    protected ResourceBundle customBundle;

    protected AbstractPartBase( ResourceBundle customBundle ) {
        this.customBundle = customBundle;
    }

    /** */
    protected String getBundleString( String key ) {

        if( customBundle != null && customBundle.containsKey(key) ) {
            return customBundle.getString(key);
        }

        if( g_bundle.containsKey(key) ) {
           return g_bundle.getString(key);
        }

        return key;
    }

    /**
     Конвертер для DSInfoBar по умолчанию (Лонговый или денежный)
     */
    protected StringConverter getDefaultAggrConverter( final AggrFuncEnum func ) {
        return func == AggrFuncEnum.COUNT ? JInvLongField.stringConverter : JInvMoneyField.stringConverter;
    }

    private String wherePredicate;
    /** */
    public void setWherePredicate( String predicate )
    {
        wherePredicate = predicate;
    }
    public String getWherePredicate( )
    {
        return wherePredicate;
    }

    @Override
    public void onVisible( ) {
    }

    /** */
    @Override
    public void onHide( ) {
    }

    /** */
    @Override
    public void recalculate() {
    }

    public Object getValue( String column ) { return null; }

    public List<? extends Control> getControls() { return new ArrayList<>(); }
    public List<? extends Control> getControls(AggrFuncEnum funcEnum) { return getControls(); }

    protected Label getAggregatorLabel() {
        DSInfoBar.PartEnum type = getType();

        String nameKey = "DS_INFO_BAR_" + type.name().toUpperCase();
        String tooltipKey = "DS_INFO_BAR_TOOLTIP_" + type.name().toUpperCase();
        Label label = null;
        boolean isCustom = customBundle != null;
        if ( isCustom )
        {
            label = new Label( customBundle.getString( nameKey ) );
        }
        else
        {
            if( type == FindById )
                label = new Label("ID:");
            else
            // Default: icons
            label = IconFactory.getLabel( U.decode( type,
                    Mark, FontAwesome.fa_check_square_o,
                    Filter, FontAwesome.fa_filter,
                    Total, FontAwesome.fa_database )
            );
        }
        label.setTooltip( new Tooltip( getBundleString( tooltipKey ) ) );
        return label;
    }

}
