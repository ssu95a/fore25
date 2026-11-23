import java.util.ArrayList;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import javafx.application.Platform;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.MapChangeListener;
import javafx.scene.layout.Pane;
import ru.inversion.fx.form.controls.Controls;

import static ru.inversion.fx.form.controls.IJInvControl.CONTROL_VALUE_CHANGED;

public final class ValueChangedFlagSmoke {
    public static void main(String[] args) throws Exception {
        var task = new FutureTask<>(ValueChangedFlagSmoke::run, null);
        Platform.startup(task);
        try {
            task.get(15, TimeUnit.SECONDS);
        } finally {
            Platform.exit();
        }
    }

    private static void run() {
        String javafx = System.getProperty("javafx.version", "");
        require(Runtime.version().feature() == 25 && javafx.matches("25(?:\\..*)?"), "Java and JavaFX 25");
        System.out.println("Java=" + Runtime.version() + " JavaFX=" + javafx);
        var pristine = new Pane();
        require(!pristine.hasProperties(), "pristine fixture has no properties");
        require(!Controls.isControlValueChanged(null), "null reads false");
        require(!Controls.isControlValueChanged(pristine), "pristine reads false");
        boolean retainedAfterFirstRead = pristine.hasProperties();
        require(!Controls.isControlValueChanged(pristine), "repeated pristine read is false");
        boolean retainedAfterRepeatedRead = pristine.hasProperties();
        System.out.printf("pristine hasProperties before/first/repeated=false/%s/%s%n",
                retainedAfterFirstRead, retainedAfterRepeatedRead);

        var decorated = new Pane();
        var metadata = new Object();
        decorated.getProperties().put("metadata", metadata);
        var mapWrites = new AtomicInteger();
        decorated.getProperties().addListener((MapChangeListener<Object, Object>) change -> mapWrites.incrementAndGet());
        require(!Controls.isControlValueChanged(decorated), "metadata-only reads false");
        require(!Controls.isControlValueChanged(decorated), "metadata-only repeat reads false");
        boolean insertedFlag = decorated.getProperties().containsKey(CONTROL_VALUE_CHANGED);
        int writesOnRead = mapWrites.get();
        require(decorated.getProperties().get("metadata") == metadata, "unrelated metadata identity");
        System.out.printf("metadata flag inserted=%s map changes on reads=%d%n", insertedFlag, writesOnRead);

        var writable = new Pane();
        var flag = Controls.getValueChangedProperty(writable);
        require(writable.getProperties().get(CONTROL_VALUE_CHANGED) == flag && !flag.get(),
                "explicit getter creates and stores false property");
        var transitions = new ArrayList<Boolean>();
        flag.addListener((obs, oldValue, newValue) -> transitions.add(newValue));
        require(Controls.getValueChangedProperty(writable) == flag, "repeated getter preserves identity");
        Controls.setControlValueChanged(writable, true);
        require(Controls.isControlValueChanged(writable), "setter true observed");
        Controls.setControlValueChanged(writable, true);
        Controls.setControlValueChanged(writable, false);
        require(!Controls.isControlValueChanged(writable), "setter false observed");
        require(Controls.getValueChangedProperty(writable) == flag, "reads and writes preserve identity");
        require(transitions.equals(java.util.List.of(true, false)), "one notification per actual transition");
        var setterFirst = new Pane();
        Controls.setControlValueChanged(setterFirst, false);
        require(setterFirst.getProperties().get(CONTROL_VALUE_CHANGED) instanceof javafx.beans.property.BooleanProperty
                && !Controls.isControlValueChanged(setterFirst), "explicit false setter still creates property");
        Controls.setControlValueChanged(null, true);

        var source = new SimpleBooleanProperty(true);
        var bound = new SimpleBooleanProperty();
        bound.bind(source);
        decorated.getProperties().put(CONTROL_VALUE_CHANGED, bound);
        var boundChanges = new AtomicInteger();
        bound.addListener((obs, oldValue, newValue) -> boundChanges.incrementAndGet());
        int beforeBoundReads = mapWrites.get();
        require(Controls.isControlValueChanged(decorated), "pre-existing bound true observed");
        source.set(false);
        require(!Controls.isControlValueChanged(decorated), "live bound false observed");
        source.set(true);
        require(Controls.isControlValueChanged(decorated), "live bound true observed again");
        require(Controls.getValueChangedProperty(decorated) == bound && bound.isBound()
                && mapWrites.get() == beforeBoundReads && boundChanges.get() == 2,
                "bound identity, binding and notifications preserved");
        System.out.printf("explicit transitions=%s bound transitions=%d%n", transitions, boundChanges.get());
        require(!retainedAfterFirstRead && !retainedAfterRepeatedRead && !insertedFlag && writesOnRead == 0,
                "predicate must not retain metadata or notify the map");
        System.out.println("CTRL-013 PASS");
    }

    private static void require(boolean condition, String label) {
        if (!condition) throw new AssertionError(label);
    }
}
