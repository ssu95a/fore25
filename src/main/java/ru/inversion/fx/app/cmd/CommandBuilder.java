package ru.inversion.fx.app.cmd;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.inversion.fx.app.Tags;
import ru.inversion.fx.form.ViewContext;
import ru.inversion.tc.TaskContext;
import ru.inversion.utils.S;
import ru.inversion.utils.U;
import ru.inversion.utils.lstn.IListenerManConsumer;
import ru.inversion.utils.lstn.ListenerManFactory;

import java.io.File;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

import static ru.inversion.fx.app.cmd.StandardArgEnum.USERID;


/**
 * Базовый класс для внешней команды
 *
 * @param <B>   impl type
 * @param <R>   тип возвращаемого значения в результате выполнения команды
 * @param <LO>  тип данных о содержании который передается слушателю в момент вызова команды
 */
public abstract class CommandBuilder<B extends CommandBuilder, R, LO> implements Callable<R>, BiFunction< ViewContext, TaskContext, R> {

    /** Поставщик стандартных параметров */
    static private volatile Function<Object,Object> g_standardArgSupplier = new StandardArgSupplierImpl();

    /** */
    public static Function< Object, Object > getStandardArgSupplier() {
        return g_standardArgSupplier;
    }

    /** */
    public static void setStandardArgSupplier( Function< Object, Object > sas ) {
        g_standardArgSupplier = sas;
    }

    /** */
    static final protected Logger logger = LoggerFactory.getLogger("ru.inversion.cmd");

    /** Рабочая папка в которой происходит выполнение, используется не всеми видами команд */
    protected File workDir;

    /** Параметры окружения выполнения команды */
    protected Map<String,String> environment;

   /**
    *  Параметры используемые в команде
    *  в мапе хранятся как именованные так и неименованные параметры
    *  для неименованных, хранится только название - значение устанавливается в {@code null}
    */
    protected Map<String,Object> nuArgs;

    /** Параметры переданные команде в виде строки */
    protected String stringArgs;

    /** Параметры переданные в виде внешнего файла */
    protected File fileArgs;

    /** Признак, что нужно ожидать завершения выполнения команды */
    protected boolean waitFor = false;

    /** Объект для вывода результата работы внешней команды  */
    protected Object out;

    /** Слушатели выполнения команды */
    private IListenerManConsumer< ICmdListener<B, LO> > listeners;

    /** */
    public CommandBuilder( )
    { }

    /** Тип команды */
    public abstract CmdTypeEnum getType();

    /** Добавить слушателя */
    public void addListener( ICmdListener<B, LO> l ) {

        if( listeners == null )
            listeners = ListenerManFactory.createListenerManConsumer();

        listeners.addListener(l);
    }

    /** Удалить слушателя */
    public void removeListener( ICmdListener<B, LO> l ) {
        if( listeners != null )
            listeners.removeListener(l);
    }

    /** */
    protected void fireOnRun( Supplier<LO> lobj ) {
        if( listeners != null )
            listeners.fire( (l)->l.onRun((B)CommandBuilder.this, lobj.get()) );
    }

    /** */
    protected void fireOnFinish( ) {
        if( listeners != null )
            listeners.fire( (l)->l.onFinish((B)CommandBuilder.this) );
    }

    /** Поставщик значений стандартных аргументов */
    protected Function<Object,Object> standardArgSupplier()
    {
        return g_standardArgSupplier;
    }

    /** Установить команду которую необходимо выполнить */
    abstract public B command( String cmdStr );

    /**
     *  Установить параметры в виде строки
     *  добавляются к существующим
     * */
    public B stringArgs( String args )
    {
        this.stringArgs = args;
        return (B)this;
    }

    /** Установить файл с параметрами команды*/
    public B fileArgs( File fileArgs )
    {
        this.fileArgs = fileArgs;
        return (B)this;
    }

    /**
     * Добавить 'стандартный' параметр,
     * значение будет установлено классом команды
     *
     *  @see StandardArgSupplierImpl
     */
    public B standardArg( StandardArgEnum ... argTypes )
    {
        if( g_standardArgSupplier == null )
            throw new IllegalStateException( Tags.PRODUCT_LABEL + "'standardArgSupplier' is null" );

        for( StandardArgEnum sae : argTypes )
        {
            if( sae != null )
            {
                Object v = g_standardArgSupplier.apply(sae);
                if( v != null )
                    namedArg( sae.name(), v.toString() );
            }
        }
        return (B)this;
    }

    /**
     * Добавить 'стандартный' параметр,
     * значение будет установлено классом команды
     */
    public B standardArg( String ... argNames )
    {
        for( String s : argNames )
        {
            if( s != null )
            {
                Object v = g_standardArgSupplier.apply(s);
                if( v != null )
                    //namedArg( s, v.toString() );
                    unnamedArgs( v.toString()  );
            }
        }
        return (B)this;
    }

    /** Параметр окружения */
    public B environment( String name, String value )
    {
        if( environment == null )
            environment = new HashMap<>();

        environment.put( name, value );

        return (B)this;
    }

    /** Параметры окружения */
    public B environments( Map< String, String > envMap )
    {
        if( environment == null )
            environment = new HashMap<>(envMap);
        else
            environment.putAll( envMap );

        return (B)this;
    }

    /** Инициализировать параметры окружения,
     *  предыдущие будут затерты
     */
    public B initEnvironments( Map< String, String > envMap )
    {
        environment = envMap;
        return (B)this;
    }

    /**
     * Инициализация именованных и неименованных параметров
     * неименованные должны в качестве значение в мапе содержать null
     * */
    public B initArgs( Map< String, Object > naMap )
    {
        nuArgs = naMap;
        return (B)this;
    }

    /** Присваивание именованного параметра в команду*/
    public B namedArg( String name, Object value )
    {
        if( nuArgs == null )
            nuArgs = new LinkedHashMap<>();

        nuArgs.put( Objects.requireNonNull( name, "Argument name cannot be null"), value );

        return (B)this;
    }

    /** Присваивание именованных параметров в команду*/
    public B namedArgs( Map< String, Object > args )
    {
        if( nuArgs == null )
            nuArgs = new LinkedHashMap<>(args);
        else
            nuArgs.putAll( args );

        return (B)this;
    }

    /** Присваивание неименованных параметров в команду*/
    public B unnamedArgs( String ... args )
    {
        return unnamedArgs( U.toIterable(args) );
    }

    /** Присваивание неименованных параметров в команду*/
    public B unnamedArgs( Iterable<String> list )
    {
        if( nuArgs == null )
            nuArgs = new LinkedHashMap<>();

        list.forEach( (s)->nuArgs.put(s,null) );

        return (B)this;
    }

    /** Установлен ли аргумент */
    public boolean containsArg( String arg )
    {
        if( S.isNullOrEmpty(arg) )
            return false;

        return nuArgs != null && nuArgs.containsKey(arg);
    }

    /** Признак, что при выполнении, необходимо дождаться завершения работы команды */
    public B waitFor( boolean wf )
    {
        this.waitFor = wf;
        return (B)this;
    }

    /** Рабочая папка где будет происходить работа команды */
    public B workDir( File dir )
    {
        this.workDir = dir;
        return (B)this;
    }

    public static void main( String[] args ) {
        try {

            //"cmd.exe", "/c", "dir"
            /*
            OSCommandBuilder oscb = new OSCommandBuilder(new Function< Object, Object >() {
                @Override
                public Object apply( Object o ) {
                    return "xxi/casper@dev8i";
                }
            });
            */



            JavaFXApp oscb = new JavaFXApp();
            oscb.jvmOptions("-XX:+UseG1GC -Xms256m -Xmx1536m -XX:MaxHeapFreeRatio=30 -XX:MinHeapFreeRatio=10 -Dlog_system_properties=true");
            oscb.namedArg("param1", 100 );
            oscb.addListener(new ICmdListener< OSCmd, String >() {
                @Override
                public void onRun( OSCmd cmd, String objRun ) {
                    System.out.println(objRun);
                }
            });
            oscb.jar("d:\\Java Projects\\XXI\\FX\\FXSmev\\target\\FXSmev.jar").mainClass("ru.inversion.fxsmev.sm008.Sm008App").standardArg(USERID);



//            oscb.command ("java.exe");
//            oscb.addUnnamedArgs("-version");
            //oscb.standardArg(USERID);
//            oscb.command("cmd.exe");
//            oscb.unnamedArgs( "/C", "start","/D", "P:\\GAN\\cli_rur", "loadmain.exe" );
            //start /D  loadmain.exe
            //oscb.addUnnamedArgs("ya.ru");
//            oscb.workDir( new File("x:\\B21\\Cd") );
//            oscb.fmx("cdbrowse");
            //oscb.addUnnamedArgs("/c", "dir");
//            StringBuilder sb = new StringBuilder();
            oscb.outputTo( System.out );
            //oscb.waitFor (true);

            oscb.call();
/*
            ForeFxModule ffm = new ForeFxModule();
            ffm.className ("ru.inversion.fxsmev.sm001.Sm001App");
            ffm.methodName("showViewSm001");

            ffm.call();

            //System.out.println(sb.toString());
*/
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

}
