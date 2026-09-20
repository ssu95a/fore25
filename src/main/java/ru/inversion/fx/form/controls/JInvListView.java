package ru.inversion.fx.form.controls;

import java.util.function.Function;
import javafx.beans.InvalidationListener;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ObjectPropertyBase;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.property.StringPropertyBase;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.cell.TextFieldListCell;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseEvent;
import javafx.util.Callback;
import javafx.util.StringConverter;
import ru.inversion.fx.form.AbstractBaseController;
import ru.inversion.fx.form.IChoiceControl;
import static ru.inversion.fx.form.controls.Controls.CONTROL_FIELD_NAME;
import static ru.inversion.fx.form.controls.IStateControl.State.NULL;
import ru.inversion.utils.S;
import ru.inversion.utils.U;
import ru.inversion.utils.converter.TypeConverter;

/**
 *
 * @author sulimoff
 */
public class JInvListView<T> extends ListView<T> implements IJInvControl, IChoiceControl, IStateControl, IValidatableByChangeValueControl, ICustomValueControl<T> {

    private ObjectProperty< Callback<T, ?> > valueGetter;
    private final RequiredState requiredState = new RequiredState(this);
    private Function<T, T> valueFactory;
    private StringProperty formatMask;
    private ObjectProperty<StringConverter<T>> converter;
    private ObjectProperty<T> valueProperty;
    final private ObjectProperty<State> stateProperty = new ReadOnlyObjectWrapper<>(this, "stateProperty", NULL);
    private BooleanProperty nullable = new SimpleBooleanProperty();

    /** */
    public JInvListView() {
        super();
        init();
    }

    /** */
    public JInvListView( ObservableList<T> items ) {
        super(items);
        init();
    }

    private void init() {
        converterProperty();
        valueProperty();
    }

    public BooleanProperty nullableProperty() {
        return nullable;
    }

    public boolean isNullable() {
        return nullable.get();
    }

    public void setSelectedValue(T value) {
        if (value != null) {
            boolean nofFound = true;
            for (T entry : getItems()) {
                if (entry == U.EMPTY || (isNullable() && entry == null)) {
                    getSelectionModel().select(entry);
                    continue;
                }
                if (valueFactory != null) {
                    T currentEntryValue = valueFactory.apply(entry);
                    if (currentEntryValue != null && currentEntryValue.equals(value)) {
                        getSelectionModel().select(entry);
                        nofFound = false;
                        break;
                    }
                } else if (value.equals(entry)) {
                    getSelectionModel().select(entry);
                    nofFound = false;
                    break;
                }
            }
            if (nofFound && isEditable()) {
                this.setValue((T) value);
            }
        } else {
            if (isEditable()) {
                this.setValue(null);
            } else {
                getSelectionModel().clearSelection();
            }
        }
    }

    // JAVAKERNEL-1753
    public T getValue() {
        return getSelectionModel().getSelectedItem();
    }

    public void setValue(T t) {
        if (this.getItems().contains(t)) {
            getSelectionModel().select(t);
        } else {
            throw new IllegalArgumentException("value - " + t + " not allowed");
        }
    }

    public Function<T, T> getValueFactory() {
        return valueFactory;
    }

    public void setValueFactory(Function<T, T> valueFactory) {
        this.valueFactory = valueFactory;
    }

    @Override
    public T getCustomValue(){
        return getValue();
    }

    @Override
    public void setCustomValue(T t) {
        setValue(t);
    }

    public String getFieldName() {
        return (String) getProperties().getOrDefault(CONTROL_FIELD_NAME, null);
    }

    public void setFieldName(String fieldName) {
        getProperties().put(CONTROL_FIELD_NAME, fieldName);
    }

    /** */
    public ObjectProperty<StringConverter<T>> converterProperty( ) {

        if( converter == null )
            converter = new ObjectPropertyBase( ) {
                @Override
                public Object getBean() {
                    return JInvListView.this;
                }

                @Override
                public String getName() {
                    return "converter";
                }

                @Override
                protected void invalidated() {
                    updateCellFactory( );
                }
            };

        return converter;
    }
    /** */
    public void setConverter( StringConverter<T> c ) {
        if( c == null ) {
            if( converter != null )
                converter.set(c);
        }
        else {
            converterProperty( ).set(c);
        }
    }
    /** */
    public StringConverter<T> getConverter( ) {
        return converter == null ? null : converter.get();
    }

    //Реализация valueProperty для осуществления поведенияк JInvListView, как JInvComboBox
    public ObjectProperty<T> valueProperty() {
        if (valueProperty == null) {
            valueProperty = new ObjectProperty<T>() {
                @Override
                public T get() {
                    return getSelectionModel().selectedItemProperty().get();
                }

                @Override
                public void addListener(ChangeListener<? super T> listener) {
                    getSelectionModel().selectedItemProperty().addListener(listener);
                }

                @Override
                public void removeListener(ChangeListener<? super T> listener) {
                    getSelectionModel().selectedItemProperty().removeListener(listener);
                }

                @Override
                public void addListener(InvalidationListener listener) {
                    getSelectionModel().selectedItemProperty().addListener(listener);
                }

                @Override
                public void removeListener(InvalidationListener listener) {
                    getSelectionModel().selectedItemProperty().addListener(listener);
                }

                @Override
                public Object getBean() {
                    return getSelectionModel().selectedItemProperty().getBean();
                }

                @Override
                public String getName() {
                    return getSelectionModel().selectedItemProperty().getName();
                }

                @Override
                public void bind(ObservableValue<? extends T> observable) {
                    this.bind(observable);
                }

                @Override
                public void unbind() {
                    this.unbind();
                }

                @Override
                public boolean isBound() {
                    return this.isBound();
                }

                @Override
                public void set(T value) {
                    setSelectedValue(value);
                }
            };
        }
        return valueProperty;
    }

    /** */
    public StringProperty formatMaskProperty( ) {
        if( formatMask == null )
            formatMask = new StringPropertyBase( ) {
                @Override
                public Object getBean() {
                    return JInvListView.this;
                }

                @Override
                public String getName() {
                    return "mask";
                }
                @Override
                protected void invalidated() {
                    updateCellFactory( );
                }
            };
        return formatMask;
    }

    /** */
    public void setFormatMask( String fm ) {
        if( S.isNullOrEmpty(fm) ) {
            if( formatMask != null )
                formatMask.set(fm);
        }
        else {
            formatMaskProperty( ).set(fm);
        }
    }
    /** */
    public String getFormatMask( ) {
        return formatMask == null ? null : formatMask.get();
    }

    /** */
    public ObjectProperty<Callback<T, ?>> valueGetterProperty() {
        if( valueGetter == null )
            valueGetter = new ObjectPropertyBase( ) {
                @Override
                public Object getBean() {
                    return JInvListView.this;
                }

                @Override
                public String getName() {
                    return "valueGetter";
                }
                @Override
                protected void invalidated() {
                    updateCellFactory( );
                }
            };
        return valueGetter;
    }

    /** */
    public void setValueGetter( Callback<T,?> vg ) {
        if( vg != null ) {
            if( valueGetter != null )
                valueGetter.set(vg);
        }
        else {
            valueGetterProperty( ).set(vg);
        }
    }
    /** */
    public Callback<T,?> getValueGetter( ) {
        return valueGetter == null ? null : valueGetter.get();
    }

    /** */
    private void updateCellFactory( ) {

        Callback<ListView<T>, ListCell<T>> cellFactory = null;

        if( converter != null && converter.get() != null ) {
            cellFactory = TextFieldListCell.forListView( converter.get() );
        }
        else {

            final Callback<T,?> vg = getValueGetter();

            if( vg != null ) {

                final String fm = getFormatMask();

                StringConverter<T> c = new StringConverter<T>() {
                    @Override
                    public String toString( T t ) {
                        return TypeConverter.convertToString( vg.call(t), fm );
                    }
                    @Override
                    public T fromString( String string ) {
                        throw new UnsupportedOperationException("Not supported yet.");
                    }
                };

                cellFactory = TextFieldListCell.forListView( c );
            }
        }

        if( cellFactory == null )
        {

            StringConverter<T> c = new StringConverter<T>() {
                @Override
                public String toString( T t ) {
                    return t.toString();
                }
                @Override
                public T fromString( String string ) {
                    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
                }
            };

            cellFactory = TextFieldListCell.forListView( c );
        }

        this.setCellFactory(cellFactory);

    }

    /** */
    @Override
    public void initChoiceBehavior(AbstractBaseController<?> controller) {
        //Выбор значений в LOV таблице
        setOnMousePressed( (MouseEvent event) -> {
            if (event.isPrimaryButtonDown() && event.getClickCount() == 2) {
                chooseAndClose( controller );
            }
        });
        setOnKeyPressed( event -> {
            if (event.getCode() == KeyCode.F9 || event.getCode() == KeyCode.ENTER && event.isControlDown()) {
                chooseAndClose( controller );
            }
        } );
    }

    private void chooseAndClose( final AbstractBaseController<?> controller ) {
        if (controller.isChoiceEnabled()) {
            controller.close(AbstractBaseController.FormReturnEnum.RET_OK);
        }
    }

    @Override
    public void setRequiredState(IValidatableControl.RequiredStateEnum state) {
        requiredState.setState(state);
    }

    @Override
    public IValidatableControl.RequiredStateEnum getRequiredState() {
        return requiredState.getState();
    }

    @Override
    public ObjectProperty<IValidatableControl.RequiredStateEnum> requiredStateProperty() {
        return requiredState.stateProperty();
    }

    @Override
    public void setRequired(boolean val) {
        if (val) {
            setRequiredState(IValidatableControl.RequiredStateEnum.REQUIRED);
        } else {
            setRequiredState(IValidatableControl.RequiredStateEnum.NOT_REQUIRED);
        }
    }

    @Override
    public boolean isRequired() {
        return requiredState.isRequired();
    }

    //    /** */
//    static private class AbstractListCellBase<T> extends ListCell<T> {
//
//        public AbstractListCellBase( Pos alignment ) {
//            super();
//            setAlignment(alignment);
//        }
//
//        @Override
//        public void updateItem( T item, boolean empty) {
//            super.updateItem( item, empty );
//        }
//    }
//
//    /** */
//    static private class ListCellFactory {
//        public <R> ListCell<R> getListCell( Class clazz, String mask ) {
//            return null;
//        }
//    }
    @Override
    public ObjectProperty<State> stateProperty() {
        return stateProperty;
    }
}
