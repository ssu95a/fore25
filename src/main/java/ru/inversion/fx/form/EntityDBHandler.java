package ru.inversion.fx.form;

import javafx.stage.Stage;
import ru.inversion.db.DbNoDataFoundException;
import ru.inversion.fx.app.es.JInvErrorService;
import ru.inversion.fx.form.JInvFXDialogController.DialogModeEnum;
import ru.inversion.meta.EntityMetadataFactory;
import ru.inversion.meta.IEntityDao;
import ru.inversion.meta.IEntityMetaData;
import ru.inversion.meta.IEntityProperty;
import ru.inversion.tc.TaskContext;
import ru.inversion.utils.U;

import java.sql.SQLException;
import java.util.*;

import static ru.inversion.fx.form.AbstractBaseController.FormModeEnum;
import static ru.inversion.fx.form.AbstractBaseController.FormModeEnum.VM_DEL;
import static ru.inversion.fx.form.AbstractBaseController.FormModeEnum.VM_EDIT;

/**
 *
 * @author ssu
 */
public class EntityDBHandler<P> implements AutoCloseable {

    private static final ResourceBundle fore = ResourceBundle.getBundle("fore");

    final private IFXEntity<P>  fxEntity;
    final private Stage         stage;
    final private TaskContext   tc;
    final private FormModeEnum  dbOperation;
    final private IEntityDao<P,?> entityDao;
    final private Map<String,IEntityProperty<P,?>> columnMap;

    /** */
    public EntityDBHandler( IFXEntity<P> fxEntity, Stage stage, TaskContext tc, DialogModeEnum dbOperation ) throws Exception {
        this(fxEntity, stage, tc, DialogModeEnum.toFormMode(dbOperation));
    }

    /** */
    public EntityDBHandler( IFXEntity<P> fxEntity, Stage stage, TaskContext tc, FormModeEnum dbOperation ) {

        Objects.requireNonNull( fxEntity );

        this.fxEntity    = fxEntity;
        this.stage       = stage;
        this.tc          = tc;
        this.dbOperation = dbOperation;

        final IEntityMetaData<P> entityMetaData = EntityMetadataFactory.getEntityMetaData((Class< P >)fxEntity.getBaseEntity().getClass());

        entityDao = entityMetaData.getEntityDao( tc );

        if( U.in( dbOperation, VM_EDIT, VM_DEL ) )
            columnMap = entityMetaData.getColumnsMap();
        else
            columnMap = null;
    }

    /** */
    public boolean handle( ) throws Exception {

        boolean retValue = false;

        try {

        P entity = fxEntity.getBaseEntity();

        if( dbOperation == FormModeEnum.VM_INS ) {

            fxEntity.commit( );

            entityDao.insert( entity );

            retValue = true;
        }
        else
        {
            P       copyEntity = null;
            boolean tryLock    = false;

            do {

                tryLock = false;

                try {

                    copyEntity = entityDao.get( entity, true );

                    if( copyEntity == null )
                        throw new DbNoDataFoundException( fore.getString("RECORD_NOT_FOUND_DEL") ) ;
                }
                catch( Throwable th ) {

                    SQLException sqlEx = JInvErrorService.findThrowable(th, SQLException.class); //FIXME

                    if( sqlEx == null || U.notInInt( sqlEx.getErrorCode(), 30006, 2014 ) )
                        throw th;

                    if( sqlEx.getErrorCode() == 30006 ) // Если запись заблокирована другим пользователем
                    {
                        if( Alerts.yesNo( stage, fore.getString("RECORD_LOCK_TRY")) ) //попробовать еще раз?
                            tryLock = true;
                        else
//return
                            return false;
                    }

                    if( sqlEx.getErrorCode() == 2014 ) { // Если невозможно заблокировать запись

                        tryLock    = false;

                        copyEntity = entityDao.get( entity, false );
                    }
                }
            } while( tryLock );

            boolean equals = true;

            int max_count = 3;
            List<String> msgs = new ArrayList<>(max_count);

            //compare
            {
                Object v1, v2;

                for( IEntityProperty<P,?> p : columnMap.values() ) {

                     if( p.getColumnInfo() == null || !p.getColumnInfo().isUpdatable() )
                         continue;

                    v1 = p.invokeGetter( entity     );
                    v2 = p.invokeGetter( copyEntity );

                    if( !U.equals( v1, v2 ) )
                    {
                        msgs.add( java.text.MessageFormat.format( fore.getString("DB_HANDLER_WARN_CHANGE"), p.getPropertyName(), v2, v1 ) );
                        equals = false;
                        if( -- max_count <= 0)
                            break;
                    }
                }
            }

            if( equals || Alerts.yesNo( stage, null, fore.getString("DB_HANDLER_WARN_CHANGE_TITLE"), String.join( "\n", msgs ) ) ) {

                if( dbOperation == VM_EDIT ) {

                    try {
                        entityDao.update( entity, fxEntity.commit( ) );
                    }
                    catch( Throwable th ) {
                        fxEntity.rollback();
                        throw th;
                    }
                }
                else
                    if( dbOperation == VM_DEL )
                        entityDao.delete( copyEntity );

                retValue = true;
            }
        }
        return  retValue;
        }
        finally
        {
            if(fxEntity.isAutoCommit() )
            {
                if( retValue )
                    tc.commit();
                else
                    tc.rollback();
            }
        }
    }

    /** */
    @Override
    public void close( ) throws Exception {
        //tc.getSession( ).clear();
    }
}
