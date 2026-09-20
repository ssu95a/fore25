/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ru.inversion.fx.form.controls.renderer;
import javafx.application.Platform;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TableColumn;
import javafx.scene.control.cell.CheckBoxTableCell;
import ru.inversion.fx.app.es.JInvErrorService;
import ru.inversion.fx.form.controls.JInvTableColumn;
import ru.inversion.meta.EntityMetadataFactory;
import ru.inversion.meta.IEntityMetaData;
import ru.inversion.meta.IEntityProperty;
import ru.inversion.utils.ConsumerWithException;
import ru.inversion.utils.S;
import ru.inversion.utils.U;

/**
 Редактор чекбоксов в ячейке.
 Своб реализацию коммита после изменения можно задать через
 {@link ru.inversion.fx.form.controls.JInvTableColumn#setUserCommit(ConsumerWithException)}
 или
 {@link ru.inversion.fx.form.controls.JInvTable#setColumnUserCommit(String, ConsumerWithException)}
 @author mik, foma
 */
public class JInvCheckBoxCellEditor<T> extends CheckBoxTableCell<T, Boolean> {
    private final ValueProperty valueProperty = new ValueProperty();
    private ConsumerWithException<CellEditorInfo<T,Boolean>> onCommitEdit = null;
    private IEntityProperty<T, ?> entityProperty = null;

    public JInvCheckBoxCellEditor() {
        selectedStateCallbackProperty().set( p -> valueProperty );

        ensureNoFocus();
    }

//    public JInvCheckBoxCellEditor( Callback<Integer, ObservableValue<Boolean>> clbck ) {
//        super( clbck );
//    }
//
//    public JInvCheckBoxCellEditor( Callback<Integer, ObservableValue<Boolean>> clbck, StringConverter<Boolean> sc ) {
//        super( clbck, sc );
//    }

    private void ensureNoFocus() {
        Platform.runLater( () -> {
            Node graphic = getGraphic();
            if ( graphic instanceof CheckBox ){
                graphic.setFocusTraversable( false );
            }
        } );
    }

    @Override
    public void commitEdit( Boolean t ) {
        Boolean result = valueProperty.trySet( t );
        super.commitEdit( result );
    }

    public class ValueProperty extends SimpleBooleanProperty {
        public ValueProperty() {}

        @Override
        public Object getBean() {
            if ( getTableRow() == null ){
                return null;
            }
            return getTableRow().getItem();
        }

        T getBeanCast() {
            try {
                return (T)getBean();
            } catch ( Throwable ex ){
                JInvErrorService.handleException( null, ex );
            }
            return null;
        }

        @Override
        public Boolean getValue() {
            Boolean ret = null;
            try {
                T item = getBeanCast();
                ret = (Boolean) getEntityProperty( item ).invokeGetter( item );
            } catch ( Throwable ignored ) {}
            return ret;
        }

        protected IEntityProperty<T, ?> getEntityProperty( T row ) throws Exception {
            if ( entityProperty != null ) {
                return entityProperty;
            }

            if ( row == null ){
                throw new Exception( "provided row is null" );
            }

            IEntityMetaData md = EntityMetadataFactory.getEntityMetaData( row.getClass() );
            entityProperty = md.getProperty( getFieldName() );
            return entityProperty;
        }

        protected String getFieldName() {
            String name = null;
            TableColumn col = getTableColumn();

            if ( col instanceof JInvTableColumn ) {

                name = ( (JInvTableColumn) col ).getFieldName();
                if ( name == null || name.length() == 0 ) {
                    name = col.getId();
                }
            }
            return U.nvl( name, S.EMPTY_STRING );
        }

        @Override
        public void set( boolean bln ) {
            trySet( bln );
        }

        public Boolean trySet( Boolean bln ) {
            Boolean oldValue = getValue();
            T pojo = getBeanCast();

            try {
//                oops();
                userEditCommit( pojo, bln );
                super.set( bln );

                return bln;
            } catch ( Throwable e ) {

                JInvErrorService.handleException( null, e );

                if ( oldValue != null ) {
                    //Rollback
                    try {
                        super.set( oldValue );
                        getEntityProperty( pojo ).invokeSetter( pojo, oldValue );
                        updateItem( oldValue, false );
                    } catch ( Exception ignored ) {}
                }
            }

            return oldValue;
        }

//        private void oops() throws Exception {
//            throw new IOException( "oops" );
//        }

        protected void userEditCommit( T row, Boolean value ) throws Exception {
            getEntityProperty( row ).invokeSetter( row, value );
            TableColumn col = getTableColumn();

            Object customOnCommit = col.getProperties().getOrDefault( JInvTableColumn.COLUMN_USER_COMMIT, null );
            onCommitEdit = U.nvl((ConsumerWithException<CellEditorInfo<T, Boolean>>) customOnCommit, onCommitEdit);
            if ( onCommitEdit != null && col instanceof JInvTableColumn ) {
                CellEditorInfo<T, Boolean> info = new CellEditorInfo<>( getBeanCast(), getFieldName(), null, value );
                onCommitEdit.accept( info );
            }
        }
    }
}
