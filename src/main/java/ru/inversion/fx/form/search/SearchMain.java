package ru.inversion.fx.form.search;

import ru.inversion.annotation.StartMethod;
import ru.inversion.fx.app.BaseApp;
import ru.inversion.fx.app.cmd.OSCmd;
import ru.inversion.fx.app.cmd.StandardArgEnum;
import ru.inversion.fx.app.es.JInvErrorService;
import ru.inversion.fx.form.ViewContext;
import ru.inversion.tc.TaskContext;

import java.io.File;
import java.util.Map;

public class SearchMain extends BaseApp
{

    @Override
    protected void showMainWindow ()
    {
        //showAndWait( getPrimaryViewContext (), new TaskContext(), Collections.emptyMap() );
        TextSearchDialog tsd = new TextSearchDialog("TextSearch", "");
        tsd.showAndWait();
        tsd = null;
        tsd = new TextSearchDialog("TextSearch", "");
        tsd.showAndWait();
    }

    @Override
    public String getAppID ()
    {
        return "XXI.Test";
    }

    public static void main (String[] args)
    {
//        System.out.println( String.format( "%02d", 1 ) );
//        System.out.println( String.format( "%02d", 11 ) );
//        System.out.println( String.format( "%02d", 111 ) );
        launch (args);

    }

    @StartMethod(description = "showAndWait")
    public static void showAndWait ( ViewContext vc, TaskContext tc, Map<String, Object> map )
    {
        //ForeFxModule ffm = new ForeFxModule();
        //ffm.className("ru.inversion.fxsmev.sm001.Sm001App").methodName("showViewSm001").call();
        try {

            OSCmd osb = new OSCmd();
            osb.standardArg(StandardArgEnum.USERID).command("loadmain.exe").workDir(new File("\\\\DS4\\pereliv\\GAN\\cli_rur")).waitFor(true).outputTo(System.out).call();

        } catch( Exception e) {
            JInvErrorService.handleException( vc, e );
        }
    }
}