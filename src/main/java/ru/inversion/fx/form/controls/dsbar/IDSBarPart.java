package ru.inversion.fx.form.controls.dsbar;

import javafx.scene.control.Control;
import javafx.scene.layout.Pane;
import ru.inversion.dataset.aggr.AggrFuncEnum;
import ru.inversion.fx.form.controls.dsbar.DSInfoBar.PartEnum;

import java.util.List;

/**
 *
 * @author ssu
 */
public interface IDSBarPart {
    
    /** */
    PartEnum getType( );
    
    /** */
    Pane createControlPane( );

    void setWherePredicate( String predicate );

    /** */
    void onVisible( );
    
    /** */
    void onHide( );
    
    /** */
    Pane getControlPane();
    
    /** */
    void recalculate();

    /** */
    List<? extends Control> getControls();

    /** */
    List<? extends Control> getControls( AggrFuncEnum funcEnum);

    Object getValue( String column );

}
