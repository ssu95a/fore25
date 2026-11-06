package ru.inversion.fx.app.cmd;

import javafx.scene.control.Label;
import javafx.stage.Stage;
import ru.inversion.fx.app.BaseApp;
import ru.inversion.fx.app.BaseAppHelper;
import ru.inversion.fx.form.ViewContext;
import ru.inversion.icons.IconFactory;
import ru.inversion.tc.TaskContext;
import ru.inversion.utils.S;
import ru.inversion.utils.Triplet;
import ru.inversion.utils.U;
import ru.inversion.utils.dco.Dco;
import ru.inversion.utils.dco.IDco;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.*;

import static ru.inversion.fx.app.cmd.CmdTypeEnum.FORE_FX_MODULE;

/**
 * Класс для запуска приложений реализованных по методике JInvFore
 * Запуск производится в той же jvm, откуда происходит вызов
 */
public class ForeFxModule extends CommandBuilder< ForeFxModule, Map<String,Object>, Triplet<String,String, Map> > {

    private String methodName;
    private String className;

    /** */
    private Object icon;

    public ForeFxModule() { }

    /** */
    public ForeFxModule className( String className )
    {
        this.className = className;
        return this;
    }

    /** */
    public ForeFxModule methodName( String methodName )
    {
        this.methodName = methodName;
        return this;
    }

    /** */
    @Override
    public CmdTypeEnum getType() {
        return FORE_FX_MODULE;
    }

    /** */
    private List<String> cmdSplit( String cmd )
    {
        String[] a = cmd.split("\\s+");
        List<String> retLst = new ArrayList<>(3);
        retLst.add(a[0]);

        if( a.length > 1 )
            retLst.add(a[1]);

        if( a.length > 2 )
        {
            a[0] = S.EMPTY_STRING;
            a[1] = S.EMPTY_STRING;
            retLst.add( String.join( " ", a ).trim() );
        }

        return retLst;
    }

    /**
     *  Команда для запуска модуля Fore FX
     *  формат:
     *      class-name static-method-name params (pairs or xml)
     */
    @Override
    public ForeFxModule command( String cmdStr ) {

        if( S.isNotNullOrEmpty(cmdStr) )
        {
            List<String> a = cmdSplit( cmdStr );
            this.className = a.get(0);
            if( a.size() > 1 )
                this.methodName = a.get(1);
            if( a.size() > 2 )
            {
                String ps = a.get(2).trim();
                if( ps.startsWith("<") )
                {
                    this.initArgs( BaseAppHelper.parseParametersString(ps) );
                }
                else
                {
                    this.initArgs( U.parseArgsStr(ps) );
                }
            }
        }
        return this;
    }

    /** Иконка для нового приложения */
    public ForeFxModule icon( Object icon )
    {
        this.icon = icon;
        return this;
    }

    /** */
    private Map<String,Object> initParameters() throws Exception
    {
        if( fileArgs != null && fileArgs.isFile() && fileArgs.exists() )
        {
            //MDom mDom = new MDom();

            try( FileInputStream fis = new FileInputStream( fileArgs ) )
            {
                final IDco dco = Dco.parseXml(fis);

                //MDomList element = mDom.list("*");

                for( IDco param : dco.select("*") )
                {
                    String name = param.a("name").value();

                    // Если не находит атрибут name, попробуем найти Name
                    if( S.isNullOrEmpty(name) )
                        name = param.a("Name").value();

                    // Параметр из файла имеет более низкий приоритет
                    if( this.nuArgs == null )
                        this.nuArgs = new LinkedHashMap<>();

                    nuArgs.putIfAbsent( name, param.value() );
                }
            } catch (FileNotFoundException ignored) {
            }
        }

        if( !S.isNullOrEmpty(stringArgs) )
        {
            if( nuArgs == null )
                nuArgs = U.parseArgsStr(stringArgs);
            else
                nuArgs.putAll( U.parseArgsStr(stringArgs) );
        }

        return nuArgs;
    }

    /** */
    @Override
    public Map<String,Object> call( ) throws CmdException {
        return apply(null,null);
    }

    @Override
    public Map< String, Object > apply( ViewContext viewContext, TaskContext taskContext ) {
        try {

            final Map<String, Object> parameters = initParameters();

            /*
            if (!ignoreMinimize) {
                final Boolean minimizeOnStart = SettingsController.SETTINGS.isMinimizeOnStart();
                System.setProperty("fx_minimize_on_start", String.valueOf(minimizeOnStart));
            }
            */

            System.setProperty( "on_front", "false"); // JDESK-45

            // Инициализируем иконку для запускаемого приложения (только JavaFX в текущей JVM)
            BaseApp.APP().getPrimaryViewContext().setIcon( null ); // Сбрасываем текущую иконку

            if( icon != null ) {

                final Label iconLabel;

                if( icon instanceof Label )
                    iconLabel = (Label)icon;
                else if( icon instanceof String )
                    iconLabel = IconFactory.getLabel(icon);
                else
                    iconLabel = null;

                BaseApp.APP().getPrimaryViewContext().setIcon( iconLabel ); // Устанавливаем текущую иконку
            }

            final Stage primaryStage = BaseApp.APP().getPrimaryStage();

            fireOnRun( ()->Triplet.makeTriplet( this.className, this.methodName, parameters ));

            BaseApp.APP().runClass( this.className, this.methodName, parameters );

            fireOnFinish( );

            // Заново выставляем stage JInvDesktop в качестве основного (JDESK-54)
            BaseApp.APP().getPrimaryViewContext().setStage(primaryStage);

            return parameters;

        } catch ( Throwable e ) {
            throw new CmdException(e);
            //JInvErrorService.handleException( BaseApp.APP().getPrimaryViewContext(), e );
            //appLog.error(String.format("Can't run application. Class-%s, method-%s", runCommand.getClassName(), runCommand.getMethodName()), e);
        }
    }


    /*
    public static ForeFxModule fromXml( InputStream is ) throws CmdException
    {
        MDom mDom = new MDom();
        mDom.load(is);

        DCont element = mDom.list("/*").first();
        String startClass     = element.a("startclass"    ).asStr();
        String startMethod    = element.a("startmethod"   ).asStr();
        String startFileParam = element.a("startfileparam").asStr();

        File fileArgs = null;

        if( S.isNullOrEmpty(startFileParam) ) {

            fileArgs = new File(startFileParam);

            if(!fileArgs.isFile() || !fileArgs.exists() )
                fileArgs = null;
        }

        return new ForeFxModule().className(startClass).methodName(startMethod).fileArgs(fileArgs);
    }
    */
}
