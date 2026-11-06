package ru.inversion.fx.form.controls.treetableex.column;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Pos;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TreeTableCell;
import javafx.scene.control.TreeTableRow;
import javafx.scene.control.cell.CheckBoxTreeTableCell;
import javafx.util.Callback;
import javafx.util.StringConverter;
import org.controlsfx.control.ToggleSwitch;
import ru.inversion.dataset.fx.ICellValueChangeListener;
import ru.inversion.dataset.fx.StubBooleanObservableValue;
import ru.inversion.fx.form.controls.renderer.Colorizer;
import ru.inversion.fx.form.controls.renderer.IColoredCell;
import ru.inversion.fx.form.controls.treetableex.JInvTreeTableColumnEx;
import ru.inversion.fx.form.controls.treetableex.JInvTreeTableEx;
import ru.inversion.meta.IEntityProperty;
import ru.inversion.utils.Tags;

import java.util.Objects;
import java.util.function.Function;

public class JInvTreeTableColumnEx_CheckBox<P> extends JInvTreeTableColumnEx<P, Boolean> {


    private static final int FIXED_WIDTH = 40;

    /** */
    private static class C<T> extends CheckBoxTreeTableCell<T,Boolean> {

        /** */
        public C( final Callback<Integer, ObservableValue<Boolean>> getSelectedProperty ) {
            super(getSelectedProperty);
        }

        private boolean isLeaf()
        {
            final TreeTableRow<T> row = getTreeTableRow();

            return row != null && row.getTreeItem() != null && row.getTreeItem().isLeaf();
        }

        @Override
        public void updateItem( Boolean item, boolean empty )
        {
            final JInvTreeTableColumnEx_CheckBox<T> tableColumn =
                    (JInvTreeTableColumnEx_CheckBox<T>) getTableColumn();

            if( !empty && tableColumn.isLeafOnly() && !isLeaf() )
                empty = true;

            super.updateItem( item, empty );

            unDisable();
        }

        private void unDisable()
        {
            final CheckBox ch = (CheckBox)getGraphic();

            if( ch != null && ch.disableProperty().isBound() ) {
                ch.disableProperty().unbind();
                ch.setDisable(false);
            }
        }
    }

    /** */
    private static class G<T> extends TreeTableCell<T,Boolean> {

        final private ToggleSwitch toggleSwitch;
        private boolean showLabel;

        private ObservableValue<Boolean> booleanProperty;

        /** */
        public G( final Callback<Integer, ObservableValue<Boolean>> getSelectedProperty,
                  final StringConverter<Boolean> converter )
        {
            super();

            this.toggleSwitch = new ToggleSwitch();

            setSelectedStateCallback(getSelectedProperty);
            setConverter(converter);

            setGraphic(null);
        }

        /** */
        public G( final Callback<Integer, ObservableValue<Boolean>> getSelectedProperty )
        {
            this(getSelectedProperty, null);
        }

        /** */
        private boolean isLeaf()
        {
            final JInvTreeTableColumnEx_CheckBox<T> tableColumn = (JInvTreeTableColumnEx_CheckBox<T>) getTableColumn();

            if( !tableColumn.isLeafOnly() )
                return true;

            final TreeTableRow<T> row = getTreeTableRow();

            return row != null && row.getTreeItem() != null && row.getTreeItem().isLeaf();
        }

        @Override
        public void updateItem(Boolean item, boolean empty) {

            super.updateItem(item, empty);

            if( booleanProperty instanceof BooleanProperty )
            {
                toggleSwitch.selectedProperty().unbindBidirectional((BooleanProperty) booleanProperty);
                booleanProperty = null;
            }

            if( empty || !isLeaf() ) {
                setText(null);
                setGraphic(null);
                return;
            }

            StringConverter<Boolean> c = getConverter();

            if( showLabel && c != null )
                setText(c.toString(item));
            else
                setText(null);

            setGraphic(toggleSwitch);

            ObservableValue<?> obsValue = getSelectedProperty();

            if( obsValue instanceof BooleanProperty )
            {
                booleanProperty = (ObservableValue<Boolean>)obsValue;
                toggleSwitch.selectedProperty().bindBidirectional((BooleanProperty) booleanProperty);
            }
            else
            {
                toggleSwitch.setSelected( Boolean.TRUE.equals(item) );
            }
        }

        private ObjectProperty<StringConverter<Boolean>> converter =new SimpleObjectProperty<StringConverter<Boolean>>( this, "converter") { protected void invalidated() { updateShowLabel();}};

        public final ObjectProperty<StringConverter<Boolean>> converterProperty() {
            return converter;
        }
        public final void setConverter(StringConverter<Boolean> value) {
            converterProperty().set(value);
        }
        public final StringConverter<Boolean> getConverter() {
            return converterProperty().get();
        }

        private ObjectProperty<Callback<Integer, ObservableValue<Boolean>>> selectedStateCallback = new SimpleObjectProperty<Callback<Integer, ObservableValue<Boolean>>>( this, "selectedStateCallback");

        public final ObjectProperty<Callback<Integer, ObservableValue<Boolean>>> selectedStateCallbackProperty() {
            return selectedStateCallback;
        }
        public final void setSelectedStateCallback(Callback<Integer, ObservableValue<Boolean>> value) {
            selectedStateCallbackProperty().set(value);
        }
        public final Callback<Integer, ObservableValue<Boolean>> getSelectedStateCallback() {
            return selectedStateCallbackProperty().get();
        }
        private void updateShowLabel() {
            this.showLabel = getConverter() != null;
            this.toggleSwitch.setAlignment( showLabel ? Pos.CENTER_LEFT : Pos.CENTER );
        }
        /** */
        private ObservableValue<?> getSelectedProperty() {
            return getSelectedStateCallback() != null ? getSelectedStateCallback().call(getIndex()) : getTableColumn().getCellObservableValue(getIndex());
        }
    }

    final private BooleanProperty leafOnlyProperty = new SimpleBooleanProperty( this, "leafOnly", false );
    final private BooleanProperty toggleProperty   = new SimpleBooleanProperty  ( this, "toggle", false );

    public JInvTreeTableColumnEx_CheckBox() {
        super();
        init();
    }

    public JInvTreeTableColumnEx_CheckBox( final String name ) {
        super( name );
        init();
    }

    private void init()
    {
        maxWidthProperty().bind(minWidthProperty());
        setMinWidth( FIXED_WIDTH);
    }

    public boolean isLeafOnly() { return leafOnlyProperty.get(); }
    public void setLeafOnly( boolean v ) { leafOnlyProperty.set(v); }

    public boolean isToggle() { return toggleProperty.get(); }
    public void setToggle( boolean v ) { toggleProperty.set(v); }

    @Override
    public void addColor( Function<IColoredCell<P>, Colorizer> styleExpr ) {
        // do nothing
    }

    @Override
    public void clearColor() {
        // do nothing
    }

    @Override
    public void bind( IEntityProperty<P, Boolean> ep, ICellValueChangeListener<P> cellValueChangeListener )
    {
        if( this.entityProperty != null )
            throw new IllegalStateException( Tags.PRODUCT_LABEL + "Column '" + getFieldName() + "' already bind" );

        final IEntityProperty<P, Boolean> property = Objects.requireNonNull( ep, "'entityProperty' is null" );

        final Class<?> propertyType = property.getType();

        if( propertyType != boolean.class && propertyType != Boolean.class )
            throw new IllegalStateException( Tags.PRODUCT_LABEL + "Column '" + getFieldName() + "' is not boolean type" );

        this.entityProperty = property;

        setCellFactory( f -> !isToggle() ? new C<>( null ) : new G<>( null ) );

        setCellValueFactory(param -> new StubBooleanObservableValue<>( property, cellValueChangeListener ).setPojoInstance( param.getValue() == null ? null : param.getValue().getValue() ) );
    }}
