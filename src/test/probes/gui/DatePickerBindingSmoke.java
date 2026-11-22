import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.scene.Scene;
import javafx.scene.control.DatePicker;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import ru.inversion.dataset.ArrayDataSet;
import ru.inversion.dataset.fx.DSControlAdapter;
import ru.inversion.fx.form.controls.Controls;
import ru.inversion.fx.form.controls.JInvCalendar;
import ru.inversion.fx.form.controls.JInvCalendarTime;

public final class DatePickerBindingSmoke {
    private static final LocalDate FIRST = LocalDate.of(2025, 1, 2);
    private static final LocalDate SECOND = LocalDate.of(2025, 3, 4);

    private record Row(LocalDate date) {}

    private final List<String> failures = new ArrayList<>();
    private DatePicker direct;
    private DatePicker bound;
    private JInvCalendar calendar;
    private JInvCalendarTime calendarTime;
    private ArrayDataSet<Row> dataset;
    private DSControlAdapter<Row> adapter;
    private Stage stage;

    public static void main(String[] args) throws Exception {
        var ready = new CountDownLatch(1);
        Platform.startup(ready::countDown);
        try {
            if (!ready.await(15, TimeUnit.SECONDS)) {
                throw new AssertionError("JavaFX startup timed out");
            }
            String javafx = System.getProperty("javafx.version", "UNKNOWN");
            if (Runtime.version().feature() != 25 || !javafx.matches("25(?:\\..*)?")) {
                throw new AssertionError("Java 25 and JavaFX 25 required");
            }
            System.out.println("Java=" + Runtime.version() + " JavaFX=" + javafx);
            var probe = new DatePickerBindingSmoke();
            try {
                onFx(probe::setup);
                onFx(probe::checkDirect);
                onFx(probe::checkLegacy);
                var first = new Row(FIRST);
                var second = new Row(SECOND);
                var blank = new Row(null);
                onFx(() -> probe.setUpBinding(first, second, blank));
                probe.navigate(0, first);
                probe.navigate(1, second);
                probe.navigate(2, blank);
            } finally {
                onFx(probe::close);
            }
            if (!probe.failures.isEmpty()) {
                throw new AssertionError("CTRL-002 failures: " + probe.failures);
            }
            System.out.println("CTRL-002 PASS");
        } finally {
            Platform.exit();
        }
    }

    private static void onFx(Runnable action) throws Exception {
        var task = new FutureTask<Void>(action, null);
        Platform.runLater(task);
        task.get(15, TimeUnit.SECONDS);
    }

    private void setup() {
        direct = new DatePicker(FIRST);
        bound = new DatePicker(FIRST);
        calendar = new JInvCalendar();
        calendarTime = new JInvCalendarTime();
        stage = new Stage();
        stage.setTitle("CTRL-002 DatePicker binding");
        stage.setScene(new Scene(new VBox(direct, bound, calendar, calendarTime), 360, 220));
        stage.show();
    }

    private void checkDirect() {
        var events = new AtomicInteger();
        direct.valueProperty().addListener((property, oldValue, newValue) -> events.incrementAndGet());
        Controls.setValue(direct, SECOND);
        checkValue("direct write", direct, SECOND);
        int beforeRepeat = events.get();
        Controls.setValue(direct, LocalDate.of(2025, 3, 4));
        check("same-value events", beforeRepeat, events.get());

        // Независимая проверка: исходное значение установлено напрямую.
        direct.setValue(SECOND);
        int beforeInvalid = events.get();
        try {
            Controls.setValue(direct, "invalid-date");
            check("invalid date rejected", true, false);
        } catch (RuntimeException expected) {
            System.out.println("Invalid conversion rejected: " + expected.getClass().getName());
        }
        checkValue("invalid date preserves value", direct, SECOND);
        check("invalid date events", beforeInvalid, events.get());
        Controls.setValue(direct, null);
        checkValue("direct clear", direct, null);
    }

    private void checkLegacy() {
        Controls.setValue(calendar, SECOND);
        checkValue("JInvCalendar write", calendar, SECOND);
        Controls.setValue(calendar, null);
        checkValue("JInvCalendar clear", calendar, null);

        var dateTime = SECOND.atTime(13, 42, 17);
        Controls.setValue(calendarTime, dateTime);
        check("JInvCalendarTime write", dateTime, Controls.getValue(calendarTime));
        Controls.setValue(calendarTime, null);
        check("JInvCalendarTime clear", null, Controls.getValue(calendarTime));
    }

    private void setUpBinding(Row first, Row second, Row blank) {
        dataset = new ArrayDataSet<>(Row.class, FXCollections.observableArrayList(first, second, blank), false);
        adapter = new DSControlAdapter<>(dataset);
        adapter.bindControl(bound, null, Row::date);
    }

    private void navigate(int rowIndex, Row expected) throws Exception {
        // Адаптер ставит обновление в очередь JavaFX перед проверкой.
        onFx(() -> dataset.setCurrentRowNum(rowIndex));
        onFx(() -> {
            check("DataSet cursor " + rowIndex, expected, dataset.getCurrentRow());
            checkValue("DataSet value " + rowIndex, bound, expected.date());
        });
    }

    private void checkValue(String label, DatePicker picker, LocalDate expected) {
        check(label, expected, picker.getValue());
        check(label + " via Controls", expected, Controls.getValue(picker));
    }

    private void check(String label, Object expected, Object actual) {
        if (Objects.equals(expected, actual)) {
            System.out.println("PASS " + label + " = " + actual);
        } else {
            String failure = label + " expected=" + expected + " actual=" + actual;
            failures.add(failure);
            System.out.println("FAIL " + failure);
        }
    }

    private void close() {
        if (adapter != null) {
            adapter.close();
        }
        if (stage != null) {
            stage.close();
        }
    }
}
