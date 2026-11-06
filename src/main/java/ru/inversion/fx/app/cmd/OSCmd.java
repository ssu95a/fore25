package ru.inversion.fx.app.cmd;

import ru.inversion.fx.app.Tags;
import ru.inversion.fx.app.ThreadPoolManager;
import ru.inversion.fx.form.ViewContext;
import ru.inversion.tc.TaskContext;
import ru.inversion.utils.S;
import ru.inversion.utils.U;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static ru.inversion.fx.app.cmd.OSCmd.PrepEnum.*;

/**
 * Класс для запуска приложений операционной системы
 */
public class OSCmd extends CommandBuilder< OSCmd, Object, String > {

    /** */
    protected enum PrepEnum {
        CMD, UNMD, NMD, STR, RUN
    }

    /** */
    protected String command;

    /** Префиксы для именованных и неименованных параметров */
    protected String namedPrefix,
                     unnamedPrefix;
    /** */
    public OSCmd()
    {
//        addListener(new ICmdListener< OSCmd, String >() {
//            @Override
//            public void onRun( OSCmd cmd, String objRun ) {
//                System.out.println(objRun);
//            }
//        });
    }

    /** */
    protected void onPrepare( PrepEnum prep, boolean after,  List<String> cmdItems )
    { }

    /** */
    @Override
    public CmdTypeEnum getType() {
        return CmdTypeEnum.OS_COMMAND;
    }

    /** */
    public OSCmd command( String cmdStr )
    {
        this.command = cmdStr;
        return this;
    }

    /**
     * Префикс для именованных параметров
     */
    public OSCmd namedPrefix( String namedPrefix )
    {
        this.namedPrefix = namedPrefix;
        return this;
    }

    /**
     * Префикс для неименованных параметров
     */
    public OSCmd unNamedPrefix( String unnamedPrefix )
    {
        this.unnamedPrefix = unnamedPrefix;
        return this;
    }

    /**
     * Объект для вывода результатов работы команды,
     * используется в основном для команды ОС
     * Out м.б:
     - Appendable
     - OutputStream
     - List<String>
     - File
     */
    public OSCmd outputTo( Object out )
    {
        this.out = out;
        return this;
    }

    /** */
    private Callable<Object> initOSHandler( InputStream is )
    {
        if( out == null )
            return null;

        if( out instanceof File)
        {
            return () -> {
                try( InputStream isLocal = is) {
                     Files.copy(isLocal, ((File) out).toPath(), StandardCopyOption.REPLACE_EXISTING);
                     return null;
                } catch (IOException e) {
                    logger.error("Error writing to File", e);
                    throw new CmdException( Tags.PRODUCT_LABEL + "Error processing command output", e);
                }
            };
        }

        if( out instanceof Appendable )
        {
            return () -> {
                try( BufferedReader reader = new BufferedReader(new InputStreamReader(is)))
                {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        ((Appendable) out).append(line).append('\n');
                    }
                } catch (IOException e) {
                    logger.error("Error writing to Appendable", e);
                    throw new CmdException(Tags.PRODUCT_LABEL + "Error processing command output", e);
                }
                return null;
            };
        }

        if( out instanceof List )
        {
            return new Callable< Object >() {
                @Override
                public Object call() throws Exception {
                    new BufferedReader( new InputStreamReader(is) ).lines().collect( Collectors.toCollection( ()->(List<String>)out ));
                    return null;
                }
            };
        }

        if( out instanceof OutputStream )
        {
            return () -> {
                try( InputStream isLocal = is )
                {
                    U.copyStreams( isLocal, (OutputStream)out );
                    return null;
                }
            };
        }

        return null;
    }

    /** */
    private String prepareUnnamed( String p )
    {
        boolean needQuote  = p.indexOf(' ') > 0 || p.indexOf('=') > 0;
        boolean needPrefix = !( S.isNullOrEmpty(unnamedPrefix) || p.startsWith(unnamedPrefix) );

        if( needQuote || needPrefix )
        {
            StringBuilder sb = new StringBuilder();

            if( needPrefix )
                sb.append(unnamedPrefix);

            if( needQuote )
                sb.append('"');

            sb.append(p);

            if( needQuote )
                sb.append('"');

            p = sb.toString();
        }

        return p;
    }

    /** */
    private String prepareNamed( String k, String v )
    {
        boolean needQuoteK  = k.indexOf(' ') > 0;
        boolean needQuoteV  = v.indexOf(' ') > 0;
        boolean needPrefix = !( S.isNullOrEmpty(namedPrefix) || k.startsWith(namedPrefix) );

        StringBuilder sb = new StringBuilder( k.length() + v.length() + 5 );

        if( needPrefix )
            sb.append(namedPrefix);

        if( needQuoteK )
            sb.append('"');

        sb.append(k);

        if( needQuoteK )
            sb.append('"');

        sb.append('=');

        if( needQuoteV )
            sb.append('"');

        sb.append(v);

        if( needQuoteV )
            sb.append('"');

        return sb.toString();
    }

    /** */
    protected void preCall( )
    {
        if( S.isNotNullOrEmpty( command ) )
        {
            if( command.indexOf('{') != -1 )
            {
                if (command.contains("{U}"))
                    command = command.replace("{U}", (String)standardArgSupplier().apply(StandardArgEnum.USER_LOGIN));

                if (command.contains("{P}"))
                    command = command.replace("{P}", (String)standardArgSupplier().apply(StandardArgEnum.USER_PASSWORD));

                if( command.contains("{D}") )
                    command = command.replace("{D}", (String)standardArgSupplier().apply(StandardArgEnum.DB_ALIAS));
            }

            if( command.indexOf(' ') > 0 )
            {
                final Map<String,Object> mapCmd = U.parseArgsStr(command);

                if( mapCmd.size() > 1 )
                {
                    final String key = mapCmd.keySet().iterator().next();
                    mapCmd.remove( key );

                    if( nuArgs == null )
                        nuArgs = mapCmd;
                    else
                        nuArgs.putAll(mapCmd);

                    final List<String> keyList = mapCmd.keySet().stream().filter(s -> s.length() > 1 && s.charAt(0) == '{' && S.lastChar(s) == '}').collect(Collectors.toList());

                    for( String s1 : keyList )
                    {
                        String stArg = s1.substring( 1, s1.length() - 1 );
                        nuArgs.remove(s1);
                        standardArg(stArg);
                    }

                    command = key;
                }
            }
        }
    }

    @Override
    public Object apply( ViewContext viewContext, TaskContext taskContext ) {
        return call();
    }

    /** */
    @Override
    public Object call( ) throws CmdException {

        final List<String> cmdItems = new ArrayList<>();

        try {

            preCall( );
            //
            onPrepare( CMD, false, cmdItems );
            cmdItems.add( command );
            onPrepare( CMD, true, cmdItems );
            //
            onPrepare( UNMD, false, cmdItems );
            if( this.nuArgs != null )
            {
                this.nuArgs.entrySet().stream().filter( e->e.getValue() == null).forEach( (e)->cmdItems.add( prepareUnnamed(e.getKey())) );
            }
            onPrepare( UNMD, true, cmdItems );
            //
            onPrepare( NMD, false, cmdItems );
            if( this.nuArgs != null )
            {
                this.nuArgs.entrySet().stream().filter( e->e.getValue() != null).forEach( (e)->cmdItems.add( prepareNamed(e.getKey(),e.getValue().toString()) ) );
            }
            onPrepare( NMD, true, cmdItems );
            //
            onPrepare( STR, false, cmdItems );
            if( this.stringArgs != null )
                cmdItems.add(stringArgs);
            onPrepare( STR, true, cmdItems );

            final ProcessBuilder pb = new ProcessBuilder( cmdItems );
//pb.redirectError (new File("D:\\940_err_log.txt"));
//pb.redirectOutput(new File("D:\\940_out_log.txt"));

            if( this.environment != null )
                pb.environment().putAll( this.environment );

            if( this.workDir != null )
                pb.directory( workDir );
            else
            {
                File cmd = new File( cmdItems.get(0) );

                if( cmd.exists() && cmd.isFile() )
                    pb.directory( cmd.getParentFile() );
            }

            fireOnRun( ()->String.join( " ", cmdItems ) );

            final Process process = pb.start();

            final Callable<Object> stdoutHandler = out == null ? null : initOSHandler(process.getInputStream());
            final Callable<Object> stderrHandler = out == null ? null : initOSHandler(process.getErrorStream());

            if( stdoutHandler != null )
                ThreadPoolManager.getInstance().executeShortTask(stdoutHandler);
            if( stderrHandler != null )
                ThreadPoolManager.getInstance().executeShortTask(stderrHandler);

            if( this.waitFor )
            {
                process.waitFor();

                fireOnFinish();

                return process.exitValue();
            }

            fireOnFinish();

            return null;
        }
        catch( Throwable th ) {
            String commandStr = String.join(" ", cmdItems );
            logger.error("Failed to execute command: {}", commandStr, th);
            throw new CmdException( String.format("%sError executing command '%s': %s", Tags.PRODUCT_LABEL, command, th.getMessage()), th );
        }
    }
}
