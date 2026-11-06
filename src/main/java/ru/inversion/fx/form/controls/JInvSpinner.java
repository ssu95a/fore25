package ru.inversion.fx.form.controls;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.ObservableList;
import javafx.scene.AccessibleAttribute;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import ru.inversion.dataset.fx.DSFXAdapter;
import ru.inversion.fx.form.action.IAction;
import ru.inversion.fx.form.action.JInvKeyboardManager;
import ru.inversion.utils.S;
import ru.inversion.utils.U;

import static ru.inversion.fx.form.controls.Controls.*;
import static ru.inversion.fx.form.controls.Controls.CONTROL_FIELD_NAME;


/** */
public class JInvSpinner<T> extends Spinner<T> implements IJInvControl, IValidatableByChangeValueControl, IFilterControl {

    private final RequiredState requiredState = new RequiredState(this);
    //private final StringProperty textLabelProperty = new SimpleStringProperty(this,"textLabelProperty", S.EMPTY_STRING );

    public JInvSpinner() {
    }

    public JInvSpinner( int min, int max, int initialValue ) {
        super(min, max, initialValue);
    }

    public JInvSpinner( int min, int max, int initialValue, int amountToStepBy ) {
        super(min, max, initialValue, amountToStepBy);
    }

    public JInvSpinner( double min, double max, double initialValue ) {
        super(min, max, initialValue);
    }

    public JInvSpinner( double min, double max, double initialValue, double amountToStepBy ) {
        super(min, max, initialValue, amountToStepBy);
    }

    public JInvSpinner( ObservableList< T > items ) {
        super(items);
    }

    public JInvSpinner( SpinnerValueFactory< T > valueFactory ) {
        super(valueFactory);
    }

    public Control setLabel( Label label ) {

        if( label != null )
        {
            label.setLabelFor( this );
            //textLabelProperty.bind( label.textProperty() );
        }
        return this;
    }

    /** {@inheritDoc } */
    @Override
    public String getFieldName() {
        return IJInvControl.super.getFieldName();
    }

    /** {@inheritDoc } */
    @Override
    public void setFieldName(String fieldName) { IJInvControl.super.setFieldName(fieldName); }

    /** */
    @Override
    public Label getLabel() {
        return (Label) this.queryAccessibleAttribute(AccessibleAttribute.LABELED_BY);
    }
    /** */
    @Override
    public StringProperty labelTextProperty() { return U.callIfNotNull( getLabel(), Label::textProperty ); }

    @Override
    public void setAction( IAction action ) {
        JInvKeyboardManager.addAction(this, action);
    }

    @Override
    public <T> DSFXAdapter< T > getDataSetAdapter() {
        return null;
    }

    /**
     * Возвращает Идентификатор группы в F7 фильтре
     * @return
     */
    public String getIdF7FilterGroup() {
        return getProperty( F7FILTER_GROUP_ID );
    }

    /**
     * Устанавливает Идентификатор группы в F7 фильтре
     * @param idF7FilterGroup
     */
    public void setIdF7FilterGroup( String idF7FilterGroup ) {
        setProperty( F7FILTER_GROUP_ID, idF7FilterGroup );
    }

    /**  Возвращает порядок следования в группе F7 фильтра */
    public Integer getOrderInF7FilterGroup() { return getProperty(F7FILTER_ORDER_IN_GROUP); }

    /** Устанавливает порядок следования в группе F7 фильтра */
    public void setOrderInF7FilterGroup(Integer orderInF7FilterGroup) {
        setProperty(F7FILTER_ORDER_IN_GROUP, orderInF7FilterGroup );
    }

    public void setIndexSearchAllowed(boolean value) {
        setProperty(INDEX_SEARCH_ALLOWED, value);
    }

    public boolean isIndexSearchAllowed() {
        return getProperty(INDEX_SEARCH_ALLOWED, Boolean.FALSE);
    }

    @Override
    public void setRequiredState(RequiredStateEnum state) {
        requiredState.setState(state);
    }

    @Override
    public RequiredStateEnum getRequiredState() {
        return requiredState.getState();
    }

    @Override
    public ObjectProperty<RequiredStateEnum> requiredStateProperty() {
        return requiredState.stateProperty();
    }

    @Override
    public void setRequired(boolean val) {
        setRequiredState( val ? RequiredStateEnum.REQUIRED : RequiredStateEnum.NOT_REQUIRED );
    }

    @Override
    public boolean isRequired() {
        return requiredState.isRequired();
    }
}
