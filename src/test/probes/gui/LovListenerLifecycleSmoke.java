import java.util.*;
import java.util.concurrent.*;
import java.util.function.*;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.scene.Scene;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import ru.inversion.fx.app.BaseApp;
import ru.inversion.fx.app.service.ViewPrefAppService;
import ru.inversion.fx.form.ViewContext;
import ru.inversion.fx.form.controls.JInvTextField;
import ru.inversion.fx.form.lov.*;


public final class LovListenerLifecycleSmoke {
    private static final List<String> failures = new ArrayList<>();
    static final class App extends BaseApp {
        static void install() { g_app = new App(); }
        static void reset() { g_app = null; }
        @Override public ViewPrefAppService getViewPrefService() { return new ViewPrefAppService(); }
    }
    static final class Lov extends AbstractLovBase<String> {
        final String name;
        final List<String> events;
        String chosen;
        boolean accept;
        Lov(String name, List<String> events) {
            this.name=name;
            this.events=events;
            valueProperty().addListener((p,o,n) -> events.add(name+"="+n));
            var original = getCheck();
            setCheck((text,set) -> { events.add("check="+text+"/"+set); return original.test(text,set); });
        }
        @Override public boolean isSmallLov() { return false; }
        @Override public void showChoiceList(ViewContext context, String filter, BiConsumer<Boolean,ILov<String>> done) {
            if (context.getStage() == null) throw new AssertionError("Stage missing");
            events.add("show="+filter);
            if (accept) setValue(chosen);
            done.accept(accept,this);
        }
    }
    static final class Field extends JInvTextField {
        int updates;
        @Override protected void setTextValueFromLov() { updates++; super.setTextValueFromLov(); }
    }
    static void check(boolean success, String label, List<String> events) {
        if (!success) throw new AssertionError(label+" "+events);
    }
    static void record(boolean success, String label, List<String> events) {
        if (!success) failures.add(label + " " + events);
    }
    static long count(List<String> events, String prefix) {
        return events.stream().filter(event -> event.startsWith(prefix)).count();
    }
    static void show(Field field, Lov lov, boolean accept, String value, List<String> events) {
        lov.accept = accept;
        lov.chosen = value;
        field.showLOV((ok, observed) -> {
            if (ok != accept || observed != lov) throw new AssertionError("callback payload");
            events.add("callback="+ok+"/"+field.getText());
        });
    }
    static void report(String name, List<String> events) {
        System.out.println(name+" "+events);
        events.clear();
    }
    static void run(Stage stage, Field field, List<String> events) {
        var model = new SimpleStringProperty("seed");
        field.textProperty().bindBidirectional(model);
        field.textProperty().addListener((p,o,n) -> events.add("text="+n));
        model.addListener((p,o,n) -> events.add("model="+n));
        var old = new Lov("old",events);
        old.setValue("preexisting");
        events.clear();
        field.setLOV(old,true);
        check(old.getValue().equals("preexisting") && field.getText().equals("seed") && events.isEmpty(),"attachment",events);
        old.setValue("initial");
        check(model.get().equals("initial") && count(events,"model=")==1 && count(events,"text=")==1,"programmatic attachment",events);
        report("ATTACH",events);
        show(field,old,false,"ignored",events);
        check(events.equals(List.of("show=initial","callback=false/initial")),"cancel",events);
        report("CANCEL",events);
        show(field,old,true,"pick",events);
        check(model.get().equals("pick") && count(events,"model=")==1 && count(events,"text=")==1
                && count(events,"old=")==1 && events.getLast().equals("callback=true/pick"),"single accepted",events);
        report("ACCEPT",events);
        int before=field.updates;
        show(field,old,true,old.getValue(),events);
        check(events.equals(List.of("show=pick","callback=true/pick")) && field.updates==before,"same object",events);
        report("SAME_REFERENCE",events);
        show(field,old,true,new String("pick"),events);
        check(count(events,"model=")==0 && count(events,"text=")==0 && count(events,"old=")==0
                && events.getLast().equals("callback=true/pick"),"equal distinct",events);
        System.out.println("equal distinct update attempts="+(field.updates-before));
        report("EQUAL_DISTINCT",events);
        field.setAppendFromLOV(true);
        field.setText("base,tail");
        field.requestFocus();
        boolean focused=field.isFocused();
        field.selectRange(5,9);
        events.clear();
        show(field,old,true,"chosen",events);
        System.out.println("selection fixture focused="+focused+" text="+field.getText());
        check(focused && field.getText().equals("base,chosen") && count(events,"model=")==1 && count(events,"text=")==1 && count(events,"callback=")==1,"append/selection",events);
        report("SELECTION",events);
        field.deselect();
        field.positionCaret(field.getLength());
        show(field,old,true,"x,y",events);
        check(field.getText().endsWith("\"x,y\"") && count(events,"model=")==1,"quoted append",events);
        report("APPEND",events);
        var latest = new Lov("new",events);
        field.setLOV(latest,true);
        latest.setValue("current");
        check(count(events,"model=")==1,"new active LOV",events);
        report("NEW_VALUE",events);
        before=field.updates;
        old.setValue("stale");
        record(field.updates==before && events.equals(List.of("old=stale")), "former LOV", events);
        report("STALE_OLD_VALUE",events);
        show(field,latest,false,"ignore",events);
        show(field,latest,false,"ignore",events);
        check(count(events,"callback=false")==2 && count(events,"model=")==0,"repeat cancel",events);
        report("REPEAT_CANCEL",events);
        field.setLOV(latest,true);
        latest.setValue("duplicated");
        record(count(events,"model=")==1 && count(events,"text=")==1,"one append on same LOV reattachment",events);
        report("REATTACH_SAME",events);
        field.unbindLOV();
        events.clear();
        var errors = new ArrayList<Throwable>();
        var thread = Thread.currentThread();
        var original = thread.getUncaughtExceptionHandler();
        thread.setUncaughtExceptionHandler((t,e) -> errors.add(e));
        try { latest.setValue("detached"); }
        finally { thread.setUncaughtExceptionHandler(original); }
        record(errors.isEmpty() && events.equals(List.of("new=detached")), "unbind preserves other listener only", events);
        System.out.println("UNBIND errors="+errors);
        report("DETACHED_NOTIFICATIONS", events);

        field.setAppendFromLOV(false);
        field.setLOV(old, false);
        var other = new JInvTextField();
        other.setLOV(old, false);
        old.setValue("shared");
        check(field.getText().equals("shared") && other.getText().equals("shared"), "shared provider", events);
        field.unbindLOV();
        old.setValue("other-only");
        check(field.getText().equals("shared") && other.getText().equals("other-only"), "only field detaches", events);
        other.unbindLOV();

        field.setLOV(old, false);
        field.lovProperty().set(latest);
        old.setValue("obsolete-direct");
        latest.setValue("property-only");
        check(field.getLOV() == latest && field.getText().equals("shared"), "direct property bypass", events);
        field.setLOV(latest, false);
        latest.setValue("setter-restored");
        check(field.getText().equals("setter-restored"), "supported setter restores attachment", events);
        field.unbindLOV();
        System.out.println("SHARED and DIRECT_PROPERTY diagnostics passed");
    }
    public static void main(String[] args) throws Exception {
        var future = new CompletableFuture<Void>();
        Platform.startup(() -> {
            try {
                App.install();
                var stage = new Stage();
                var field = new Field();
                stage.setScene(new Scene(new VBox(field),300,100));
                stage.show();
                field.requestFocus();
                Platform.runLater(() -> {
                    try { run(stage,field,new ArrayList<>()); if (!failures.isEmpty()) throw new AssertionError(failures); future.complete(null); }
                    catch (Throwable error) { future.completeExceptionally(error); }
                    finally { stage.hide(); App.reset(); }
                });
            } catch (Throwable error) { future.completeExceptionally(error); }
        });
        int code=1;
        try { future.get(20,TimeUnit.SECONDS); code=0; }
        catch (Exception e) { e.printStackTrace(); }
        finally { Platform.exit(); }
        if (code == 0) System.out.println("CTRL-026 PASS");
        System.exit(code);
    }
}
