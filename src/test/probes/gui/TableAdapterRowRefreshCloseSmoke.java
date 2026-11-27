import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import com.sun.management.ThreadMXBean;
import javafx.application.Platform;
import javafx.beans.Observable;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.event.Event;
import javafx.scene.Scene;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.util.converter.DefaultStringConverter;
import ru.inversion.dataset.DataSetEvent;
import ru.inversion.dataset.XXIDataSet;
import ru.inversion.dataset.fx.DSFXAdapter;
import ru.inversion.dataset.mark.IMarkable;
import ru.inversion.fx.form.controls.JInvTable;
import ru.inversion.fx.form.controls.JInvTableColumn;
import ru.inversion.fx.form.controls.renderer.JInvTableCellMark;
import ru.inversion.fx.form.controls.renderer.JInvTextFieldTableCell;

public final class TableAdapterRowRefreshCloseSmoke {
    static final class App extends ru.inversion.fx.app.BaseApp {
        static void install(){g_app=new App();}
        @Override public ru.inversion.fx.app.service.ViewPrefAppService getViewPrefService(){
            return new ru.inversion.fx.app.service.ViewPrefAppService(){
                @Override public Boolean isMarkLeft(){return true;}
                @Override public void refreshTableColumn(javafx.scene.control.TableColumnBase<?,?> c){}
            };
        }
    }
    private static final ThreadMXBean ALLOC=(ThreadMXBean)ManagementFactory.getThreadMXBean();
    private enum Mode { FULL, ROW_REPLACE, EXTRACTOR, LAYOUT_ONLY }

    private static final class Row implements IMarkable {
        final int id;
        final SimpleIntegerProperty signal=new SimpleIntegerProperty();
        String value;
        boolean mark;
        Row(int id){this.id=id;value="row-"+id;}
        @Override public boolean isMark(){return mark;}
        @Override public void setMark(boolean mark){this.mark=mark;}
        @Override public Object getMarkID1(){return id;}
        @Override public boolean equals(Object x){return x instanceof Row r && r.id==id;}
        @Override public int hashCode(){return id;}
    }

    private static final class Rows extends XXIDataSet<Row> {
        final ObservableList<Row> fixture=FXCollections.observableArrayList(r->new Observable[]{r.signal});
        Rows(int n){for(int i=0;i<n;i++)fixture.add(new Row(i));}
        @Override protected List<Row> rows(){return fixture;}
        @Override public List<Row> getRows(){return fixture;}
        @Override public Class<Row> getRowClass(){return Row.class;}
        @Override public boolean isSupportMark(){return true;}
        @Override public void markRow(int index){
            var r=fixture.get(index);
            r.mark=true;
            fireMarkDataSetEvent(ru.inversion.dataset.DataSetMarkEvent.MarkActionEnum.MARK_ROW,index,r);
        }
        void beforeAfterExecute(){fireDataSetEvent(DataSetEvent.DataSetEventType.EXECUTE,false);}
    }

    private record Sample(long micros,long bytes,int cells,int values,int pulse,int calls) {}

    private static final class Fixture implements AutoCloseable {
        final Rows rows;
        final Stage stage=new Stage();
        final JInvTable<Row> table=new JInvTable<>();
        final JInvTableColumn<Row,String> first=new JInvTableColumn<>("C0");
        final Scene scene;
        final List<Row> committed=new ArrayList<>();
        final List<String> listSignals=new ArrayList<>();
        final IllegalArgumentException invalid=new IllegalArgumentException("invalid draft");
        int cells,values,pulses,refreshCalls,edits,cancels,checks,markValues;
        boolean countList;
        final DSFXAdapter<Row> adapter=new DSFXAdapter<>(){
            @Override public void refresh(){refreshCalls++;super.refresh();}
        };

        Fixture(int n,int height,int columns,boolean marks)throws Exception{
            rows=new Rows(n);
            table.setFixedCellSize(24);
            table.setEditable(true);
            for(int j=0;j<columns;j++){
                var col=j==0?first:new JInvTableColumn<Row,String>("C"+j);
                col.setEditable(true);
                col.setCellValueFactory(c->{values++;return new ReadOnlyStringWrapper(c.getValue().value);});
                col.setCellFactory(c->{
                    cells++;
                    return new JInvTextFieldTableCell<Row,String>(new DefaultStringConverter(){
                        @Override public String fromString(String text){
                            checks++;
                            if(text.equals("bad"))throw invalid;
                            return text;
                        }
                    });
                });
                col.addEventHandler(TableColumn.editStartEvent(),e->edits++);
                col.addEventHandler(TableColumn.editCancelEvent(),e->cancels++);
                col.addEventHandler(TableColumn.<Row,String>editCommitEvent(),e->{
                    Row row=e.getRowValue();
                    committed.add(row);
                    row.value=e.getNewValue();
                });
                table.getColumns().add(col);
            }
            scene=new Scene(new StackPane(table),500,height);
            stage.setScene(scene);
            adapter.bindTable(rows,table,marks);
            adapter.setEnd(true);
            if(marks){
                @SuppressWarnings("unchecked") var mark=(TableColumn<Row,Boolean>)table.getMarkColumn();
                var cf=mark.getCellFactory();var vf=mark.getCellValueFactory();
                mark.setCellFactory(c->{cells++;return cf.call(c);});
                mark.setCellValueFactory(c->{values++;markValues++;return vf.call(c);});
            }
            rows.fixture.addListener((ListChangeListener<Row>)ch->{
                while(ch.next()){
                    if(countList)listSignals.add(ch.wasUpdated()?"UPDATE":ch.wasReplaced()?"REPLACE":"OTHER");
                }
            });
            stage.show();
            table.getSelectionModel().select(0);
            pulse(()->{});
        }

        void invalidate(Mode mode,int row) {
            switch(mode){
                case FULL -> rows.beforeAfterExecute();
                case ROW_REPLACE -> adapter.refreshRow(row);
                case EXTRACTOR -> rows.fixture.get(row).signal.set(rows.fixture.get(row).signal.get()+1);
                case LAYOUT_ONLY -> table.requestLayout();
            }
        }

        Sample pulse(Runnable action){
            int initialCells=cells,initialValues=values,initialPulses=pulses,initialRefresh=refreshCalls;
            long begin=System.nanoTime(), initialBytes=ALLOC.getCurrentThreadAllocatedBytes();
            var key=new Object();
            Sample[] outcome={null};
            boolean[] drained={false};
            Runnable listener=()->{
                pulses++;
                if(drained[0]&&outcome[0]==null){
                    outcome[0]=new Sample((System.nanoTime()-begin)/1000,
                            ALLOC.getCurrentThreadAllocatedBytes()-initialBytes,cells-initialCells,values-initialValues,
                            pulses-initialPulses,refreshCalls-initialRefresh);
                    Platform.runLater(()->Platform.exitNestedEventLoop(key,null));
                }
            };
            scene.addPostLayoutPulseListener(listener);
            try{
                action.run();
                Platform.runLater(()->{drained[0]=true;Platform.requestNextPulse();});
                Platform.enterNestedEventLoop(key);
                return outcome[0];
            }finally{scene.removePostLayoutPulseListener(listener);}
        }

        String text(){
            return table.lookupAll(".table-cell").stream().filter(TableCell.class::isInstance)
                    .map(TableCell.class::cast).filter(c->c.getIndex()==0&&c.getTableColumn()==first)
                    .findFirst().orElseThrow().getText();
        }
        boolean mark(){
            return table.lookupAll(".check-box-table-cell").stream().filter(JInvTableCellMark.class::isInstance)
                    .map(JInvTableCellMark.class::cast).filter(c->c.getIndex()==0).findFirst()
                    .map(c->((CheckBox)c.getGraphic()).isSelected()).orElse(false);
        }
        TextField editor(){
            return table.lookupAll(".table-cell").stream().filter(TableCell.class::isInstance)
                    .map(TableCell.class::cast).filter(c->c.getIndex()==0&&c.getTableColumn()==first&&c.isEditing())
                    .map(TableCell::getGraphic).filter(TextField.class::isInstance).map(TextField.class::cast)
                    .findFirst().orElse(null);
        }
        void key(KeyCode code,boolean press){
            Event.fireEvent(editor(),new KeyEvent(press?KeyEvent.KEY_PRESSED:KeyEvent.KEY_RELEASED,
                    "","",code,false,false,false,false));
        }
        void arm(){
            Event.fireEvent(table,new MouseEvent(MouseEvent.MOUSE_PRESSED,1,1,1,1,
                    MouseButton.PRIMARY,1,false,false,false,false,true,false,false,false,false,true,null));
        }

        @Override public void close(){adapter.close();stage.hide();}
    }

    private static String range(long[] a){
        Arrays.sort(a);
        return a[4]+"("+a[0]+".."+a[8]+")";
    }

    private enum Route { INVALID, SAME, CLOSED, MARK_CLOSED, CLOSE_DURING, DETACHED, EQUAL_REPLACE, REMOVE, PERMUTE, OFFSCREEN, MARK, MARK_REPLACE,
        DATASET_UPDATE, EDIT_SAME, EDIT_OTHER, EDIT_F8, EDIT_ENTER, EDIT_ESCAPE, EDIT_REJECT }
    private static void audit(Route route,List<Route> failures)throws Exception{
        try(var f=new Fixture(200,240,1,true)){
            var list=f.rows.fixture;
            var a=list.get(0);
            var b=list.get(1);
            var x=new Row(a.id);
            x.value="equal-X";
            var log=new ArrayList<String>();
            var fx=Thread.currentThread();
            var previous=fx.getUncaughtExceptionHandler();
            fx.setUncaughtExceptionHandler((t,e)->log.add("error:"+e.getClass().getSimpleName()));
            try{
                list.addListener((ListChangeListener<Row>)c->{
                    while(c.next()){
                        if (route==Route.CLOSE_DURING && c.wasReplaced()) {
                            f.adapter.close();
                            f.adapter.close();
                            f.table.getFocusModel().focus(1);
                        }
                        if(c.wasPermutated())log.add("PERMUTE");
                        else log.add((c.wasReplaced()?"REPLACE":c.wasRemoved()?"REMOVE":"UPDATE")+":"+c.getFrom()
                            +":"+c.getRemoved().stream().map(r->tag(r,a,b,x)).toList()
                            +"->"+c.getAddedSubList().stream().map(r->tag(r,a,b,x)).toList());
                    }
                });
                f.rows.addRowListener(e->log.add("dataset:"+e.getRowOperation()+":"+e.getRowIndex()));
                f.rows.addMarkDataSetListener(e->log.add("mark:"+e.getIndex()));
                boolean edit=route.name().startsWith("EDIT");
                if(edit){
                    f.table.edit(0,f.first);
                    f.table.layout();
                    if(f.editor()==null)throw new AssertionError("edit fixture inactive");
                    f.editor().setText(route==Route.EDIT_REJECT?"bad":"draft");
                    if(route==Route.EDIT_F8||route==Route.EDIT_ENTER){
                        f.key(route==Route.EDIT_F8?KeyCode.F8:KeyCode.ENTER,false);
                    } else if(route==Route.EDIT_ESCAPE){
                        f.arm();
                        f.key(KeyCode.ESCAPE,true);
                        f.table.edit(-1,null);
                    } else if(route==Route.EDIT_REJECT) {
                        f.arm();
                        try{f.table.edit(-1,null);}catch(IllegalArgumentException expected){log.add("error:"+expected.getClass().getSimpleName());}
                    }
                }
                var sample=f.pulse(()->{
                    switch(route){
                        case MARK, MARK_REPLACE, MARK_CLOSED -> f.rows.markRow(0);
                        case INVALID -> f.adapter.refreshRow(-1);
                        case DATASET_UPDATE -> f.rows.updateCurrentRow(x);
                        case OFFSCREEN -> f.adapter.refreshRow(150);
                        case EDIT_OTHER -> f.adapter.refreshRow(1);
                        default -> f.adapter.refreshRow(0);
                    }
                    switch(route){
                        case CLOSED, MARK_CLOSED -> {f.adapter.close();f.adapter.close(); if(route==Route.MARK_CLOSED) f.rows.markRow(0);}
                        case DETACHED -> ((StackPane)f.scene.getRoot()).getChildren().clear();
                        case EQUAL_REPLACE, MARK_REPLACE -> list.set(0,x);
                        case REMOVE -> list.remove(0);
                        case PERMUTE -> FXCollections.sort(list,(left,right)->Integer.compare(right.id,left.id));
                        default -> {}
                    }
                    log.add("enqueue-end");
                });
                var selected=tag(f.table.getSelectionModel().getSelectedItem(),a,b,x);
                var occupant=tag(list.getFirst(),a,b,x);
                boolean edited=f.table.getEditingCell()!=null;
                var editor=f.editor();
                System.out.printf("%s occupant=%s selected=%s focus=%d cursor=%d editor=%s draft=%s commits=%s checks=%d a=%s x=%s markA=%s markFirst=%s cell=%d value=%d full=%d log=%s%n",
                    route,occupant,selected,f.table.getFocusModel().getFocusedIndex(),f.rows.getCurrentRowNum(),
                    edited,editor==null?"none":editor.getText(),f.committed.stream().map(r->tag(r,a,b,x)).toList(),
                    f.checks,a.value,x.value,a.mark,f.mark(),sample.cells(),sample.values(),sample.calls(),log);
                var expected=switch(route){
                    case CLOSED, MARK_CLOSED, INVALID, DATASET_UPDATE -> List.<String>of();
                    case EQUAL_REPLACE, MARK_REPLACE -> List.of("REPLACE:0:[X]->[X]");
                    case REMOVE -> List.of("REPLACE:0:[B]->[B]");
                    case PERMUTE -> List.of("REPLACE:0:[R199]->[R199]");
                    case OFFSCREEN -> List.of("REPLACE:150:[R150]->[R150]");
                    case EDIT_OTHER -> List.of("REPLACE:1:[B]->[B]");
                    default -> List.of("REPLACE:0:[A]->[A]");
                };
                var deferred=log.subList(log.indexOf("enqueue-end")+1,log.size());
                var commits=f.committed.stream().map(r->tag(r,a,b,x)).toList();
                boolean accept=route==Route.EDIT_F8||route==Route.EDIT_ENTER;
                boolean rejection=route==Route.EDIT_REJECT;
                var errors=log.stream().filter(e->e.startsWith("error:")).toList();
                int focus = route==Route.CLOSE_DURING||route==Route.EDIT_ENTER?1:route==Route.PERMUTE?199:0;
                int cursor = route==Route.EDIT_ENTER?1:route==Route.PERMUTE?199:0;
                if(!deferred.equals(expected)||!commits.equals(accept?List.of("A"):List.of())||edited
                        ||!errors.equals(rejection?List.of("error:IllegalArgumentException"):List.of())
                        ||f.table.getFocusModel().getFocusedIndex()!=focus||f.rows.getCurrentRowNum()!=cursor
                        ||(route==Route.MARK_CLOSED&&log.stream().filter("mark:0"::equals).count()!=2)
                        ||f.checks!=(accept||rejection?1:0))failures.add(route);
            }finally{fx.setUncaughtExceptionHandler(previous);}
        }
    }
    private static String tag(Row r,Row a,Row b,Row x){
        return r==a?"A": r==b?"B": r==x?"X":r==null?"null":"R"+r.id;
    }

    public static void main(String[] args)throws Exception{
        var test=new FutureTask<Void>(()->{
            App.install();
            var failures=new ArrayList<Route>();
            for(Route route:Route.values())audit(route,failures);
            if(!failures.isEmpty())throw new AssertionError("row refresh retirement: "+failures);
            return null;
        });
        Platform.startup(()->{Platform.setImplicitExit(false);Platform.runLater(test);});
        try{test.get(80,TimeUnit.SECONDS);System.out.println("CTRL-106 PASS");}
        finally{Platform.exit();}
    }
}
