package ru.inversion.fx.app.sec;

import javafx.event.ActionEvent;
import ru.inversion.datacall.IDataCall;
import ru.inversion.datacall.SQLCallBuilder;
import ru.inversion.fx.app.Tags;
import ru.inversion.fx.form.action.IAction;
import ru.inversion.tc.TaskContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 *
 * @author ssu
 */
public class JInvSecurityService {
    public static final SecurityStrategyEnum DEFAULT_SECURITY_STRATEGY = SecurityStrategyEnum.DISABLE;
    private static boolean g_checkStrategy = false;
    private static StubAction g_stubAction = new StubAction();
    /**
     * Заглушка
     */
    private static class StubAction implements IAction {
        /** */
        public StubAction() { }
        /** */
        @Override
        public void setEnabled(boolean val) {
            ;
        }
        /** */
        @Override
        public boolean isEnabled() {
            return false;
        }
        /** */
        @Override
        public void handle(ActionEvent event) {
            ;
        }
        /** */
        @Override
        public void handle() { }
        /** */
        @Override
        public IAction getNextAction() { return null; }
        /** */
        @Override
        public void setNextAction(IAction actionAfter) { }
    }

    private static class WrapAction<E extends Throwable> implements IAction {
        private final IAction     baseAction;
        private final Integer     actionId;
        private final TaskContext tc;
        private final Consumer<E> onError;

        /** */
        public WrapAction( IAction baseAction, Integer actionId, TaskContext tc, Consumer<E> onError ) {
            this.baseAction = baseAction;
            this.actionId = actionId;
            this.tc         = tc;
            this.onError    = onError;
        }

        /** */
        @Override
        public void setEnabled(boolean b) {
            baseAction.setEnabled(b);
        }

        /** */
        @Override
        public boolean isEnabled() {
            return baseAction.isEnabled();
        }

        /** */
        @Override
        public void handle( ActionEvent e)
        {
            try {

                if( isCanAccessIsAction( tc, actionId ) )
                    baseAction.handle(e);
                else
                {
                    try {

                        throw new SecurityException (
                            SQLCallBuilder.NEW(tc).url(JInvSecurityService.class.getResource("plsql/def.xml"))
                                .name("getActErr")
                            .build()
                                .set("ACT_ID", actionId )
                            .execute()
                                .<String>getReturnValue()
                        );
                    }
                    catch( Throwable th ) {
                        throw new RuntimeException( Tags.PRODUCT_LABEL + "Error on call 'JF_pkg_Util.odb_Access_Get_Act_Err'", th );
                    }
                }
            /*
                try(CallableStatement cs = tc.getConnection().prepareCall("{?=call ODB_ACCESS_1.IS_ACT(?)}")) {

                    cs.registerOutParameter(1, Types.INTEGER);
                    cs.setInt(2, actionID);
                    cs.execute();

                    doRun = cs.getInt(1) == 1;

                } catch(SQLException ex) {
                    throw new JInvDbException(fore.getString("OSHIBKA_PRI_VYZOVE_F-CII_ODB_ACCESS_1_IS_ACT"), ex);
                }

                if(doRun) {
                    baseAction.handle(e);
                } else {
                    try(CallableStatement cs = tc.getConnection().prepareCall("{?=call ODB_ACCESS_1.get_act_err(?)}")) {

                        cs.registerOutParameter(1, Types.VARCHAR);
                        cs.setInt(2, actionID);
                        cs.execute();

                        throw new SecurityException(cs.getString(1));

                    } catch(SQLException ex) {
                        throw new JInvDbException(fore.getString("OSHIBKA_PRI_VYZOVE_F-CII_ODB_ACCESS_1_GET_ACT_ERR"), ex);
                    }
                }
             */
            }
            catch( Throwable th ) {
                if( onError != null )
                    onError.accept( (E)th );
                else
                    throw th;
            }
        }

        @Override
        public void handle() {
            handle(null);
        }

        @Override
        public IAction getNextAction() {
            return null;
        }

        @Override
        public void setNextAction(IAction actionAfter) {
        }
    }

    /** */
    //private static final ResourceBundle fore = ResourceBundle.getBundle("fore");

    /** */
    static public IAction wrapSecAction( TaskContext tc, Integer actID, IAction baseAction ) {
        return wrapSecAction( tc, actID, baseAction, null );
    }

    /** */
    static public <E extends Throwable> IAction wrapSecAction( TaskContext tc, Integer actId, IAction baseAction, Consumer<E> onError ) {

        if( actId == null )
            return baseAction;

        if( g_checkStrategy )
        {
            if( isCanAccessIsAction(tc, actId) )
                return baseAction;
            else
                return g_stubAction;

        } else {
            return new WrapAction<>( baseAction, actId, tc, onError );
        }
    }

    /** */
    static public boolean isCanAccessIsAction( TaskContext tc, Integer actId )
    {
        try {

            return SQLCallBuilder.NEW(tc)
                   .url (JInvSecurityService.class.getResource("plsql/def.xml"))
                   .name("isAct")
                .build()
                   .set("ACT_ID", actId)
                .execute()
                   .<Integer>getReturnValue() != 0;
        }
        catch( Throwable th ) {
            throw new RuntimeException( Tags.PRODUCT_LABEL + "Error on call 'JF_pkg_Util.odb_Access_Is_Act'", th );
        }

        /*
        try (CallableStatement cs = tc.getConnection().prepareCall("{?=call ODB_ACCESS_1.IS_ACT(?)}")) {

            cs.registerOutParameter(1, Types.NUMERIC);
            cs.setInt(2, actID);
            cs.execute();

            return TypeConverter.convert( cs.getObject(1), Integer.class ) != 0;

        } catch (SQLException ex) {
            throw new JInvDbException(fore.getString("OSHIBKA_PRI_VYZOVE_F-CII_ODB_ACCESS_1_IS_ACT"), ex);
        }
        */
    }

    /** */
    static public void isCanAccessList( TaskContext tc, Map<Integer,Boolean> actIDlist) {

        try {

            final List< Integer > idList = new ArrayList<>(actIDlist.keySet());

            final IDataCall call_ActList = SQLCallBuilder.NEW(tc).url(JInvSecurityService.class.getResource("plsql/def.xml"))
                  .name("isActList")
                  .build()
                  .set("actList", idList)
                  .execute();

            final List<Integer> retList = call_ActList.get("retList");

            for( int i = 0; i < idList.size(); i++ )
                 actIDlist.put( idList.get(i), retList.get(i) != 0 );
        }
        catch( Throwable th ) {
            throw new RuntimeException( Tags.PRODUCT_LABEL + "Error on call 'isActList'", th );
        }
    }

    /** UTIL.In_Role (tabRole (i)) */
    static public boolean hasRole( TaskContext tc, String role ) {

        try {

            return
                SQLCallBuilder.NEW(tc)
                    .url (JInvSecurityService.class.getResource("plsql/def.xml"))
                    .name( "hasRole" )
                  .build()
                    .set ( "CROLE", role )
                  .execute()
                    .<Integer>getReturnValue() == 1;
        }
        catch( Throwable th ) {
            throw new RuntimeException( Tags.PRODUCT_LABEL + "Error on call 'JF_pkg_Util.Util_In_Role'", th );
        }
    }
}
