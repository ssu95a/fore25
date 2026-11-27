import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.scene.Scene;
import javafx.scene.control.DialogPane;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.stage.WindowEvent;
import ru.inversion.dataset.ArrayDataSet;
import ru.inversion.dataset.fx.DSFXAdapter;
import ru.inversion.fx.app.ThreadPoolManager;
import ru.inversion.fx.app.es.JInvErrorService;
import ru.inversion.fx.form.controls.JInvProgressInfoPane;
import ru.inversion.fx.form.controls.JInvTable;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.ThrowableProxy;
import ch.qos.logback.core.AppenderBase;

public final class TableAdapterSearchMissDeliverySmoke {
    static final class App extends ru.inversion.fx.app.BaseApp {
        static void install() { g_app = new App(); }
    }

    enum Route { ACTIVE_HIT, ACTIVE_MISS_ALERT, ACTIVE_ERROR, ACTIVE_CANCEL, RETIRED_EXTERNAL,
        RETIRED_ALERT, RETIRED_DETACHED_ALERT, NO_WINDOW, REPARENT, HIDDEN, ERROR_BEFORE_RETIRE, ERROR_AFTER_RETIRE, BULK_ERROR_AFTER_RETIRE }

    static final class Rows extends ArrayDataSet<String> {
        final RuntimeException failure = new IllegalStateException("CTRL-098 fixture failure");
        final List<String> events;
        final Route route;
        volatile Runnable action;
        final CountDownLatch swapEnded = new CountDownLatch(1);
        int swaps;
        Rows(Route route,List<String> events) {
            super(String.class,FXCollections.observableArrayList("A","B","C"),false);
            this.route=route;this.events=events;
        }
        @Override public boolean swappingData() {
            swaps++;
            events.add("swap-enter");
            try {
                switch(route){
                    case ACTIVE_ERROR, ERROR_BEFORE_RETIRE -> throw failure;
                    case ACTIVE_CANCEL, RETIRED_EXTERNAL, RETIRED_ALERT, RETIRED_DETACHED_ALERT, NO_WINDOW, REPARENT, HIDDEN,
                            ERROR_AFTER_RETIRE, BULK_ERROR_AFTER_RETIRE -> block();
                    default -> { }
                }
                if(route==Route.ERROR_AFTER_RETIRE||route==Route.BULK_ERROR_AFTER_RETIRE)throw failure;
                events.add("swap-return:false");
                return false;
            } catch(RuntimeException problem) {
                events.add("swap-throw");
                throw problem;
            } finally {swapEnded.countDown();}
        }
        void block(){
            var gate=new CountDownLatch(1);
            Platform.runLater(()->{
                try{action.run();}
                finally{gate.countDown();}
            });
            try{if(!gate.await(10,TimeUnit.SECONDS))throw new AssertionError("swap barrier");}
            catch(InterruptedException e){events.add("swap-interrupt");Thread.currentThread().interrupt();}
        }
    }

    private static void exercise(Route route, List<Route> failures) throws Exception {
        var events=new CopyOnWriteArrayList<String>();
        var rows=new Rows(route,events);
        var oldStage=new Stage();
        var freshStage=new Stage();
        var oldTable=new JInvTable<String>();
        var freshTable=new JInvTable<String>();
        var oldRoot=new StackPane(oldTable);
        var oldAdapter=new DSFXAdapter<String>();
        var freshAdapter=new DSFXAdapter<String>();
        var freshRows=new ArrayDataSet<>(String.class,FXCollections.observableArrayList("fresh"),false);
        oldStage.setScene(new Scene(oldRoot,300,200));
        freshStage.setScene(new Scene(new StackPane(freshTable),300,200));
        oldAdapter.bindTable(rows,oldTable,false);
        freshAdapter.bindTable(freshRows,freshTable,false);
        oldStage.show(); freshStage.show();
        freshTable.getSelectionModel().select(0);
        Runnable retire=()->{
            oldAdapter.close();oldAdapter.close();
            if(route==Route.RETIRED_DETACHED_ALERT)oldRoot.getChildren().clear();
            events.add("retired");
        };
        rows.action=()->{
            if(route==Route.NO_WINDOW || route==Route.REPARENT || route==Route.HIDDEN){
                if(route==Route.HIDDEN) oldStage.hide();
                else {
                    oldRoot.getChildren().clear();
                    if(route==Route.NO_WINDOW) new Scene(new StackPane(oldTable));
                    else ((StackPane)freshStage.getScene().getRoot()).getChildren().add(oldTable);
                }
                events.add("owner-changed");
                return;
            }
            if(route==Route.ACTIVE_CANCEL){
                events.add("cancel");
                Window.getWindows().stream().map(w->w.getScene().getRoot())
                        .filter(JInvProgressInfoPane.class::isInstance).map(JInvProgressInfoPane.class::cast)
                        .findFirst().orElseThrow().getCancelBtn().fire();
            } else retire.run();
        };
        ListChangeListener<Window> windows=change->{
            while(change.next()){
                for(var window:change.getAddedSubList()){
                    var root=window.getScene().getRoot();
                    if(root instanceof JInvProgressInfoPane){
                        events.add("progress-show");
                        window.addEventHandler(WindowEvent.WINDOW_HIDDEN,e->{
                            events.add("progress-hidden");
                            if(route==Route.ERROR_BEFORE_RETIRE)retire.run();
                        });
                    } else if(root instanceof DialogPane){
                        events.add("dialog-shown");
                        if(window instanceof Stage stage)events.add("dialog-owner:"+(stage.getOwner()==oldStage ? "old" : stage.getOwner()==freshStage ? "fresh" : "other"));
                        window.addEventHandler(WindowEvent.WINDOW_HIDDEN,e->events.add("dialog-hidden"));
                        Platform.runLater(window::hide);
                    }
                }
            }
        };
        var logger=(ch.qos.logback.classic.Logger)JInvErrorService.logger;
        var appender=new AppenderBase<ILoggingEvent>(){
            @Override protected void append(ILoggingEvent event){
                if(event.getThrowableProxy() instanceof ThrowableProxy proxy && proxy.getThrowable()==rows.failure)
                    events.add("original-error-identity");
            }
        };
        appender.start();logger.addAppender(appender);
        Window.getWindows().addListener(windows);
        var fx=Thread.currentThread();
        var previous=fx.getUncaughtExceptionHandler();
        fx.setUncaughtExceptionHandler((thread,error)->events.add("FX-error:"+error));
        try{
            if(route==Route.BULK_ERROR_AFTER_RETIRE)oldAdapter.moveToLast();
            else if(route==Route.ACTIVE_MISS_ALERT||route==Route.RETIRED_ALERT||route==Route.RETIRED_DETACHED_ALERT||route==Route.NO_WINDOW||route==Route.REPARENT||route==Route.HIDDEN)oldAdapter.findRow(r->false);
            else oldAdapter.findRow(r->route==Route.ACTIVE_HIT&&r.equals("B"),
                    result->events.add("consumer:"+result+":freshShowing="+freshStage.isShowing()));
            if(route==Route.ACTIVE_CANCEL && !rows.swapEnded.await(10,TimeUnit.SECONDS))
                throw new AssertionError("cancelled swap termination");
            var turn=new Object();
            Platform.runLater(()->Platform.runLater(()->Platform.runLater(()->Platform.exitNestedEventLoop(turn,null))));
            Platform.enterNestedEventLoop(turn);
            long shown = events.stream().filter("dialog-shown"::equals).count();
            long fxErrors = events.stream().filter(e->e.startsWith("FX-error:")).count();
            long genuine = events.stream().filter("original-error-identity"::equals).count();
            var consumers=events.stream().filter(e->e.startsWith("consumer:")).toList();
            boolean errorRoute = route==Route.ACTIVE_ERROR||route==Route.ERROR_BEFORE_RETIRE
                    ||route==Route.ERROR_AFTER_RETIRE||route==Route.BULK_ERROR_AFTER_RETIRE;
            var expectedConsumers=switch(route){
                case ACTIVE_HIT -> List.of("consumer:true:freshShowing=true");
                case RETIRED_EXTERNAL, ACTIVE_CANCEL -> List.of("consumer:false:freshShowing=true");
                default -> List.<String>of();
            };
            if(shown!=(errorRoute||route==Route.ACTIVE_MISS_ALERT?1:0)||genuine!=(errorRoute?1:0)
                    ||fxErrors!=0||!consumers.equals(expectedConsumers)
                    ||(route==Route.ACTIVE_MISS_ALERT&&!events.contains("dialog-owner:old"))
                    ||!"fresh".equals(freshTable.getSelectionModel().getSelectedItem())||freshRows.getCurrentRowNum()!=0)
                failures.add(route);
            System.out.printf("%s swaps=%d timeline=%s oldSel=%s oldCursor=%d freshSel=%s freshCursor=%d%n",
                    route,rows.swaps,events,oldTable.getSelectionModel().getSelectedItem(),rows.getCurrentRowNum(),
                    freshTable.getSelectionModel().getSelectedItem(),freshRows.getCurrentRowNum());
        }finally{
            fx.setUncaughtExceptionHandler(previous);
            Window.getWindows().removeListener(windows);
            logger.detachAppender(appender);appender.stop();
            oldAdapter.close();freshAdapter.close();
            oldStage.hide();freshStage.hide();
        }
    }

    public static void main(String[] args)throws Exception{
        var test=new FutureTask<Void>(()->{
            App.install();
            var failures=new ArrayList<Route>();
            for(Route route:Route.values())exercise(route,failures);
            if(!failures.isEmpty())throw new AssertionError("miss delivery ownership: "+failures);
            return null;
        });
        Platform.startup(()->{Platform.setImplicitExit(false);Platform.runLater(test);});
        try{test.get(60,TimeUnit.SECONDS);System.out.println("CTRL-098 PASS");}
        finally{Platform.exit();ThreadPoolManager.getInstance().shutdown();}
    }
}
