package ru.inversion.fx.form.controls;

import javafx.beans.property.StringProperty;
import ru.inversion.utils.U;

/**
 * @author fomishkin
 * @since 22.04.2022
 */
public interface IMnbItem {

    /** */
    StringProperty mnbItemProperty();

    /** */
    default String getMnbItem() {return mnbItemProperty() == null ? null : mnbItemProperty().get(); };

    /** */
    default void setMnbItem(String mnbItem) {
        if( mnbItemProperty() != null )
            mnbItemProperty().set(mnbItem);
    };
}
