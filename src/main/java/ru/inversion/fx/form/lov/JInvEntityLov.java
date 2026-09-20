package ru.inversion.fx.form.lov;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.util.Callback;
import ru.inversion.fx.app.es.JInvErrorService;
import ru.inversion.fx.form.AbstractBaseController;
import ru.inversion.fx.form.FXFormLauncher;
import ru.inversion.fx.form.ViewContext;
import ru.inversion.fx.form.controls.Controls;
import ru.inversion.fx.form.controls.IBaseControl;
import ru.inversion.fx.form.controls.JInvTextField;
import ru.inversion.fx.form.lov.exceptions.JInvLovException;
import ru.inversion.meta.EntityMetadataFactory;
import ru.inversion.meta.IEntityProperty;
import ru.inversion.tc.TaskContext;
import ru.inversion.utils.S;
import ru.inversion.utils.U;

import java.util.*;
import java.util.function.BiConsumer;

/**
 *
 * @author ssu
 * @param <P> класс сущности
 * @param <T> класс первичного ключа
 */
public class JInvEntityLov<P, T> extends AbstractEntityLovBase<P, T> {

    public static final String LOV_CLASS_NAME = "ru.inversion.lov.class_name";
    public static final String LOV_VALIDATE_FROM_LOV = "ru.inversion.lov.validate_from_lov";

    final private List< JInvEntityLovColumn<P>> columnList = new ArrayList<>();

    private JInvEntityLovColumn<P> columnValue;

    public JInvEntityLov(Class<? extends P> entityClass) {
        super(entityClass, null, null);
    }

    public JInvEntityLov(Class<? extends P> entityClass, String valueColumnName) {
        super(entityClass, valueColumnName, null);
    }

    public JInvEntityLov(Class<? extends P> entityClass, String valueColumnName, Callback<P, T> valueCallback) {
        super(entityClass, valueColumnName, valueCallback);
    }

    @Override
    protected void init() {
        super.init();

        if(!columnList.isEmpty() )
            return;

        final ResourceBundle rb = getResourceBundle();

        final Map<String, Integer> columnsOrder = new HashMap<>();

        //columnOrder
        if( rb.containsKey("LOV_COLUMNS_ORDER") )
        {
            final String str = rb.getString("LOV_COLUMNS_ORDER");

            U.forEachWithIndex (
                Arrays.stream( str.split(",")).map(String::trim)
                    .filter(S::isNotNullOrEmpty)
                        .iterator(),
                    columnsOrder::put
            );
        }

        Map< String, IEntityProperty<P,?> > pdMap = EntityMetadataFactory.getEntityMetaData( (Class<P>)getEntityClass()) .getPropertiesMap();

        final List<IEntityProperty<P,?>> idPropertyList = EntityMetadataFactory.getEntityMetaData( (Class<P>)getEntityClass() ).getIDList();

        idPropertyList.forEach((pd) -> columnList.add (
            new JInvEntityLovColumn<> (
                pd, rb, columnsOrder.getOrDefault( pd.getPropertyName(), Integer.MAX_VALUE)
            )
        )
        );

        pdMap.values().forEach((pd) -> {

            if( !pd.isId() )
            {
                columnList.add (
                    new JInvEntityLovColumn<>( pd, rb, columnsOrder.getOrDefault( pd.getPropertyName(), Integer.MAX_VALUE ))
                );
            }
        }
        );

        if( columnList.isEmpty() )
            throw new JInvLovException(java.text.MessageFormat.format(fore.getString("ENTITYLOV_NE_OPREDELENY_STOLBCY_DLYA_ENTITY_KLASSA"), new Object[]{getEntityClass()}), fore.getString("NE_ZADANY_ANNOTACII_COLUMN_DLYA_GETXXX"));

        columnList.sort( Comparator.comparingInt( JInvEntityLovColumn::getIndex ) );

        if( !S.isNullOrEmpty(valueColumnName) ) {

            columnValue = columnList.stream().filter((c) -> valueColumnName.equals(c.getName())).findFirst().orElse( columnList.get(0)) ;

            if( columnValue != columnList.get(0)) {
                columnList.remove(columnValue);
                columnList.add(0, columnValue);
            }
        }
        else
        {
            columnValue = columnList.get(0);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public void showChoiceList(ViewContext vc, String filterString, BiConsumer<Boolean, ILov<T>> clb) {
        try {

            init( );

            if( isSkipFilterString() )
                filterString = "%";

            PEntityLov<P> plov = new PEntityLov<> (this, filterString);

            new FXFormLauncher (getTaskContext (), vc, ViewEntityLovController.class)
                .dataObject (plov)
                .dialogMode (AbstractBaseController.FormModeEnum.VM_CHOICE)
                .callback ((ok, ctrl)-> choiceResult (AbstractBaseController.FormReturnEnum.RET_OK.equals (ok), plov.getChoiceValue (), clb))
                .doModal ();

        } catch (Throwable th) {
            JInvErrorService.handleException (vc, th);
        }

    }

    private void choiceResult ( boolean ok, P val, BiConsumer<Boolean, ILov<T>> clb )
    {
        if (ok)
            setEntityValue (val);

        clb.accept (ok, this);
    }

    public List<JInvEntityLovColumn<P>> getColumnList() {
        init();
        return columnList;
    }

    JInvEntityLovColumn<P> getColumnValue() {
        init();
        return columnValue;
    }

    @Override
    public Callback<P, T> getValueCallback() {
        if (super.getValueCallback() == null) {
            super.setValueCallback( param -> {
                try {
                    return param == null ? null : (T) getColumnValue().getColumnValue(param);
                } catch (Throwable th) {
                    throw new JInvLovException(java.text.MessageFormat.format(fore.getString("OSHIBKA_PRI_POLUCHENII_ZNACHENIYA_STOLBCA"), new Object[]{getColumnValue().getName()}), th);
                }
            } );

        }
        return super.getValueCallback();
    }
    @Deprecated
    public static AbstractLovBase generateLov(String className, String valueColumnName, TaskContext tc, String title)
            throws ClassNotFoundException {
        return generateLov( className, tc, title );
    }

    /** */
    public static AbstractLovBase generateLov( String className, TaskContext tc, String title ) throws ClassNotFoundException {
        return generateLov( className, tc, title, null );
    }

    /**
        Генерирует новый LOV, используя текстовое представление класса как pojo, либо,
        если этот класс реализует AbstractLovBase, использует его как LOV
     */
    public static AbstractLovBase generateLov( String className, TaskContext tc, String title, IBaseControl controlFor ) throws ClassNotFoundException {

        if( S.isNullOrEmpty(className) )
            return null;

        Class clazz = Class.forName( className );

        if( AbstractLovBase.class.isAssignableFrom( clazz ) )
        {
            try {

                final AbstractLovBase customLov = (AbstractLovBase) clazz.newInstance();

                if( controlFor != null )
                    customLov.setControlFor(controlFor);

                if( customLov instanceof AbstractEntityLovBase )
                {
                    ( (AbstractEntityLovBase) customLov ).setTaskContext( tc );
                }

                return customLov;

            } catch ( InstantiationException | IllegalAccessException e ) {
                throw new IllegalStateException( "Error on create custom LOV",  e );
            }
        }

        JInvEntityLov lov;

        lov = new JInvEntityLov(clazz);
        if( controlFor != null )
            lov.setControlFor(controlFor);
        lov.setTaskContext(tc);

        if( canReplaceTitle( title, lov ) )
            lov.setTitle( title );

        return lov;
    }

    /**
     Генерирует новый LOV, используя текстовое представление класса как pojo, либо, если этот класс реализует
     AbstractLovBase, использует его как LOV.

     !!! Вешает одноразовый слушатель на значение, заданное при запуске формы (lovField.setValue(x) в init() и подобные)
     !!! Обращается в базу при непустом значении, позволяя обновить привязанные к ловополю другие элементы

     @param field поле, для которого генерится LOV
     @param tc TaskContext
     @param title Название
     @return LOV
     @throws ClassNotFoundException
     */
    public static AbstractLovBase generateLov( JInvTextField field, TaskContext tc, String title ) throws ClassNotFoundException {
       AbstractLovBase lov = generateLov( field.getLovClassName(), tc, title );
       
       final ChangeListener<String> listener = new ChangeListener<String>() {

            //Сработали разок – и хватит
            boolean triggeredOnce = false;

            @Override
            public void changed( final ObservableValue<? extends String> v, final String o, final String n ) {
                
                //Если lov уже имеет значения, то меняем флаг и не меняем позицию курсора
                if(S.isNotNullOrEmpty(o))
                    triggeredOnce=true;
                
                if(!triggeredOnce)
                {
                    //Если при инициализации lov был пустой, то отрабатываем при первом вводе значения.
                    if(S.isNotNullOrEmpty( field.getText() ) )
                    {
                        Platform.runLater(() -> {
                                lov.checkValue( Controls.getValue(field), true );
                                //Если при вводе значения в lov в нем есть такое значение,
                                //то перекидываем позицию каретки в конец, чтобы пользователь мог писать дальше
                                if( !S.isNullOrEmpty(field.getText()) )
                                     field.positionCaret( field.getText().length() );
                            }
                        );
                        triggeredOnce=true;
                    }  
                }

                if( triggeredOnce )
                    field.textProperty().removeListener(this);
            }
        };
        field.textProperty().addListener(listener);
        return lov;
    }

    private static boolean canReplaceTitle( final String title, final JInvEntityLov lov ) {
        return S.isNotNullOrEmpty(title) && S.isNullOrEmpty( lov.getTitle() );
    }
}
