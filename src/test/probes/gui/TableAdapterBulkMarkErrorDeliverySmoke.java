import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.scene.Scene;
import javafx.scene.control.DialogPane;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.stage.Window;
import ru.inversion.dataset.XXIDataSet;
import ru.inversion.dataset.fx.DSFXAdapter;
import ru.inversion.dataset.mark.IMarkable;
import ru.inversion.fx.app.BaseApp;
import ru.inversion.fx.app.ThreadPoolManager;
import ru.inversion.fx.app.es.JInvErrorService;
import ru.inversion.fx.app.service.ViewPrefAppService;
import ru.inversion.fx.form.controls.JInvTable;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.ThrowableProxy;
import ch.qos.logback.core.AppenderBase;

public final class TableAdapterBulkMarkErrorDeliverySmoke {
    enum State { ACTIVE, RETIRED, DETACHED, HIDDEN, REPARENTED, SUCCESS, BEFORE_ENTRY, AFTER_QUEUED, EMPTY }
    static final class App extends BaseApp {
        static void install(){g_app=new App();}
        @Override public ViewPrefAppService getViewPrefService(){
            return new ViewPrefAppService(){
                @Override public Boolean isMarkLeft(){return true;}
                @Override public void refreshTableColumn(javafx.scene.control.TableColumnBase<?,?> c){}
            };
        }
    }
    static final class Row implements IMarkable {
        boolean mark;
        public boolean isMark(){return mark;}
        public void setMark(boolean v){mark=v;}
        public Object getMarkID1(){return 1;}
    }
    static final class Rows extends XXIDataSet<Row> {
        final List<Row> items=FXCollections.observableArrayList(new Row());
        final RuntimeException error=new IllegalStateException("controlled mark worker failure");
        final CountDownLatch entered=new CountDownLatch(1), release=new CountDownLatch(1);
        final boolean success;
        int writes;
        final List<Boolean> saved = new ArrayList<>();
        Rows(boolean success){this.success=success;}
        @Override protected List<Row> rows(){return items;}
        @Override public List<Row> getRows(){return items;}
        @Override public Class<Row> getRowClass(){return Row.class;}
        @Override public boolean isSupportMark(){return true;}
        @Override public void markAllRows(boolean save){work(true,save);}
        @Override public void unMarkAllRows(boolean save){work(false,save);}
        void work(boolean value,boolean save){
            saved.add(save);
            entered.countDown();
            await(release);
            if(!success)throw error;
            items.getFirst().mark=value;
            writes++;
        }
    }
    static void await(CountDownLatch latch){
        try{if(!latch.await(10,TimeUnit.SECONDS))throw new AssertionError("worker barrier");}
        catch(InterruptedException e){Thread.currentThread().interrupt();throw new AssertionError(e);}
    }
    static void exercise(State state, boolean mark, boolean closeTest, List<String> failed)throws Exception{
        var rows=new Rows(state==State.SUCCESS || closeTest && state!=State.ACTIVE);
        rows.items.getFirst().mark=!mark;
        var table=new JInvTable<Row>();
        var root=new StackPane(table);
        var origin=new Stage();
        var fresh=new Stage();
        origin.setScene(new Scene(root,300,180));
        var freshRoot=new StackPane();
        fresh.setScene(new Scene(freshRoot,300,180));
        var refreshes=new int[1];
        var adapter=new DSFXAdapter<Row>(){@Override public void refresh(){refreshes[0]++;super.refresh();}};
        adapter.bindTable(rows,table,true);
        origin.show();fresh.show();
        var errors=new ArrayList<Throwable>();
        var owners=new ArrayList<Window>();
        var logger=(ch.qos.logback.classic.Logger)JInvErrorService.logger;
        var appender=new AppenderBase<ILoggingEvent>(){
            @Override protected void append(ILoggingEvent e){
                if(e.getThrowableProxy() instanceof ThrowableProxy p)errors.add(p.getThrowable());
            }
        };
        ListChangeListener<Window> dialogs=change->{
            while(change.next())for(var window:change.getAddedSubList()){
                if(window.getScene().getRoot() instanceof DialogPane && window instanceof Stage dialog){
                    owners.add(dialog.getOwner());
                    Platform.runLater(window::hide);
                }
            }
        };
        var fxErrors=new ArrayList<Throwable>();
        var thread=Thread.currentThread();
        var previous=thread.getUncaughtExceptionHandler();
        var pool=(ThreadPoolExecutor)ThreadPoolManager.getInstance().fixedExecutor();
        var occupied=new CountDownLatch(pool.getMaximumPoolSize()-1);
        var releasePool=new CountDownLatch(1);
        for(int i=0;i<pool.getMaximumPoolSize()-1;i++)pool.execute(()->{occupied.countDown();await(releasePool);});
        await(occupied);
        appender.start();logger.addAppender(appender);
        Window.getWindows().addListener(dialogs);
        thread.setUncaughtExceptionHandler((t,e)->fxErrors.add(e));
        var entryRelease=new CountDownLatch(1);
        try{
            if(state==State.EMPTY){
                rows.items.clear();
                table.setBigTableMode(true);
            }
            if(state==State.BEFORE_ENTRY){
                var entryOccupied=new CountDownLatch(1);
                pool.execute(()->{entryOccupied.countDown();await(entryRelease);});
                await(entryOccupied);
            }
            if(mark)adapter.markAll();else adapter.unMarkAll();
            if(state==State.BEFORE_ENTRY){
                adapter.close();adapter.close();
                entryRelease.countDown();
            }
            if(state!=State.EMPTY)await(rows.entered);
            switch(state){
                case RETIRED -> {adapter.close();adapter.close();}
                case DETACHED -> root.getChildren().clear();
                case HIDDEN -> origin.hide();
                case REPARENTED -> {root.getChildren().clear();freshRoot.getChildren().add(table);}
                default -> {}
            }
            rows.release.countDown();
            pool.submit(()->{}).get(10,TimeUnit.SECONDS);
            if(state==State.AFTER_QUEUED){adapter.close();adapter.close();}
            var gate=new Object();
            Platform.runLater(()->Platform.runLater(()->Platform.exitNestedEventLoop(gate,null)));
            Platform.enterNestedEventLoop(gate);
            boolean success=state==State.SUCCESS;
            boolean good=fxErrors.isEmpty() && (success
                    ? errors.isEmpty()&&owners.isEmpty()&&refreshes[0]==1&&rows.writes==1&&rows.items.getFirst().mark==mark
                    : errors.equals(List.of(rows.error))&&owners.size()==1
                        &&owners.getFirst()==null
                        &&refreshes[0]==0&&rows.writes==0&&rows.items.getFirst().mark!=mark);
            if(closeTest) {
                if(state==State.EMPTY) good=rows.writes==0&&refreshes[0]==0&&fxErrors.isEmpty()&&errors.isEmpty()&&rows.saved.isEmpty();
                else if(state==State.SUCCESS){
                    table.getFilterToolbar().savePrevMarkedRowsProperty().set(false);
                    if(mark)adapter.markAll();else adapter.unMarkAll();
                    pool.submit(()->{}).get(10,TimeUnit.SECONDS);
                    Platform.runLater(()->Platform.runLater(()->Platform.exitNestedEventLoop(gate,null)));
                    Platform.enterNestedEventLoop(gate);
                    good=rows.writes==2&&refreshes[0]==2&&rows.saved.equals(List.of(true,false))
                            &&errors.isEmpty()&&fxErrors.isEmpty();
                } else if(state!=State.ACTIVE) {
                    good=rows.writes==1&&rows.items.getFirst().mark==mark&&refreshes[0]==0
                            &&errors.isEmpty()&&owners.isEmpty()&&fxErrors.isEmpty();
                }
            }
            if(!good)failed.add(state+"/"+mark);
            System.out.printf("%s mark=%s errorIdentity=%s dialogs=%s secondary=%s refresh=%d writes=%d%n",state,mark,
                    errors.equals(List.of(rows.error)),owners.stream().map(o->o==origin?"origin":o==fresh?"fresh":o==null?"neutral":"other").toList(),
                    fxErrors,refreshes[0],rows.writes);
        }finally{
            entryRelease.countDown();rows.release.countDown();releasePool.countDown();
            thread.setUncaughtExceptionHandler(previous);
            Window.getWindows().removeListener(dialogs);logger.detachAppender(appender);appender.stop();
            adapter.close();origin.hide();fresh.hide();
        }
    }
    public static void main(String[] args)throws Exception{
        var test=new FutureTask<Void>(()->{
            App.install();
            var failed=new ArrayList<String>();
            boolean closeTest=args.length>0&&args[0].equals("close");
            var states=closeTest
                    ? List.of(State.ACTIVE,State.SUCCESS,State.EMPTY,State.BEFORE_ENTRY,State.RETIRED,State.AFTER_QUEUED,State.DETACHED,State.HIDDEN,State.REPARENTED)
                    : List.of(State.ACTIVE,State.RETIRED,State.DETACHED,State.HIDDEN,State.REPARENTED,State.SUCCESS);
            for(var state:states)for(boolean mark:new boolean[]{true,false})exercise(state,mark,closeTest,failed);
            if(!failed.isEmpty())throw new AssertionError("bulk mark error delivery: "+failed);
            return null;
        });
        Platform.startup(()->{Platform.setImplicitExit(false);Platform.runLater(test);});
        try{test.get(60,TimeUnit.SECONDS);System.out.println(args.length==0?"CTRL-104 PASS":"CTRL-105 PASS");}
        finally{Platform.exit();ThreadPoolManager.getInstance().shutdown();}
    }
}
