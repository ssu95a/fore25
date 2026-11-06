package ru.inversion.fx.app.cmd;

import ru.inversion.utils.S;

import java.util.List;

import static ru.inversion.fx.app.cmd.CmdTypeEnum.FMX;
import static ru.inversion.fx.app.cmd.OSCmd.PrepEnum.CMD;
import static ru.inversion.fx.app.cmd.StandardArgEnum.USERID;

/**
 * Класс для запуска fmx модулей oracle forms
 * (пока только под Windows)
 */
public class FMXApp extends OSCmd {

    /** */
    private final static String IFRUN60EXE = "ifrun60.exe";

    /** */
    private String fmxName;

    /** */
    public FMXApp( )
    {
        super.command( IFRUN60EXE );
    }

    /** */
    @Override
    public FMXApp command( String cmdStr ) {
        return fmx( cmdStr );
    }

    /** */
    @Override
    public CmdTypeEnum getType() {
        return FMX;
    }

    /** имя файла fxm для запуска */
    public FMXApp fmx( String fmx )
    {
        if( S.isNotNullOrEmpty(fmx) )
        {
            fmx = fmx.toLowerCase();

            if( !fmx.endsWith(".fmx") && fmx.indexOf('.') == -1 )
                 fmx += ".fmx";
        }

        this.fmxName = fmx;

        return this;
    }

    /** */
    @Override
    protected void onPrepare( PrepEnum prep, boolean after, List<String> cmdItems ) {
        if( prep == CMD && after ) {
            cmdItems.add(fmxName);
            if( containsArg(USERID.name()) )
                ;//cmdItems.add("userid");
            else
                cmdItems.add("userid=" + standardArgSupplier().apply(USERID) );
        }
    }

    /** Под каким пользователем запускать, если не вызывать, то будет подставлен текущий*/
    public void userId( String userId )
    {
        namedArg( USERID.name(), userId );
    }
}
