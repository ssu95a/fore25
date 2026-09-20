package ru.inversion.fx.app.cmd;

import ru.inversion.fx.form.ViewContext;
import ru.inversion.tc.TaskContext;
import ru.inversion.utils.Pair;
import ru.inversion.utils.S;
import ru.inversion.utils.U;

import java.util.Collections;
import java.util.Map;
import java.lang.reflect.Method;
import java.util.Optional;

import static ru.inversion.fx.app.cmd.CmdTypeEnum.BICOMP_ACTION;

/** */
public class BICompAction extends CommandBuilder< BICompAction, Map<String,Object>, Pair<String, Map<String,Object>> >  {

    final static String runClassName  = "ru.inversion.bicomp.action.ActionBiComp";
    final static String runMethodName = "run";

    private static Method runMethod = null;

    private String actionClassName;
    /** */
    synchronized private void check4Init( ) {

        if( runMethod == null )
        {
            try {
                final Class<?> runClass = Class.forName( runClassName );
                runMethod = runClass.getDeclaredMethod ( runMethodName, ViewContext.class, TaskContext.class, Map.class );
            }
            catch( Throwable th ) {
                throw new CmdException( "Ошибка при инициализации класса и метода выполнения action", th );
            }
        }
    }

    /** */
    public BICompAction( ) {
        check4Init();
    }

    /** */
    @Override
    public CmdTypeEnum getType( ) {
        return BICOMP_ACTION;
    }

    /** */
    public BICompAction actionClassName( String ac ) {
        this.actionClassName = ac;
        return this;
    }

    /** */
    @Override
    public BICompAction command( String cmdStr ) {

        final Map<String,Object> map = U.parseArgsStr(cmdStr);

        boolean hasByName = true;
        actionClassName = (String)map.get("BiCompActionName");

        if( S.isNullOrEmpty(actionClassName) )
        {
            hasByName = false;

            final Optional<Map.Entry<String,Object>> oe = map.entrySet().stream().filter(e -> e.getValue() == null).findFirst();
            oe.ifPresent(e -> actionClassName = e.getKey() );
        }

        if(!hasByName && !S.isNullOrEmpty(actionClassName) ) {
            map.put( "BiCompActionName", actionClassName);
            map.remove(actionClassName);
        }

        initArgs(map);

        return this;
    }

    /** */
    @Override
    public Map<String, Object> call( ) throws Exception {
        return apply( null, null );
    }

    /** */
    @Override
    public Map<String, Object> apply( ViewContext vc, TaskContext tc ) {

        try {

            if( S.isNullOrEmpty(actionClassName) )
                throw new IllegalStateException("В параметрах не задано название 'BiCompActionName'");

            if( !S.isNullOrEmpty(stringArgs) )
            {
                if( nuArgs == null )
                    nuArgs = U.parseArgsStr(stringArgs);
                else
                    nuArgs.putAll( U.parseArgsStr(stringArgs) );
            }

            fireOnRun( ()->Pair.makePair( actionClassName, nuArgs ));

            runMethod.invoke( null, vc, tc, nuArgs );

            fireOnFinish( );

            return Collections.emptyMap();
        }
        catch( Throwable th ) {
            throw new CmdException( "Ошибка при вызове BIAction ", th );
        }
    }
}
