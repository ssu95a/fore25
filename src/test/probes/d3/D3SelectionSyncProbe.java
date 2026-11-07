package ru.inversion.probes.d3;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.scene.Scene;
import javafx.scene.control.TableView;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import ru.inversion.dataset.ArrayDataSet;
import ru.inversion.dataset.fx.DSFXAdapter;

public final class D3SelectionSyncProbe {
    private static final int TIMEOUT_SECONDS = 30;
    private static final AtomicReference<Throwable> ASYNC_FAILURE = new AtomicReference<>();

    private record Row(String name) {
        @Override public String toString() { return name; }
    }

    private record State(int tableIndex, Row tableItem, int datasetIndex, Row datasetRow,
                         int navigationCallbacks, int selectionCallbacks, int inputEvents) { }

    private static final class Fixture {
        final List<Row> rows = List.of(new Row("A"), new Row("B"), new Row("C"));
        final ArrayDataSet<Row> dataset = new ArrayDataSet<>(
                Row.class, FXCollections.observableArrayList(rows), false);
        final TableView<Row> table = new TableView<>();
        final DSFXAdapter<Row> adapter = new DSFXAdapter<>();
        final Stage stage = new Stage();
        int navigationCallbacks;
        int selectionCallbacks;
        int inputEvents;
        String phase = "INITIAL";

        Fixture() throws Exception {
            stage.setScene(new Scene(new StackPane(table), 480, 240));
            dataset.setCurrentRowNum(2);
            dataset.addNavigationListener(event -> {
                navigationCallbacks++;
                System.out.printf("%s NAV %d -> %d thread=%s%n", phase,
                        event.getOldRowIndex(), event.getNewRowIndex(), Thread.currentThread().getName());
            });
            table.getSelectionModel().selectedIndexProperty().addListener((observable, oldValue, newValue) -> {
                selectionCallbacks++;
                System.out.printf("%s SEL %s -> %s thread=%s%n", phase,
                        oldValue, newValue, Thread.currentThread().getName());
            });
            table.addEventFilter(KeyEvent.ANY, event -> inputEvents++);
            table.addEventFilter(MouseEvent.ANY, event -> inputEvents++);
            snapshot("INITIAL BEFORE BIND");
            // Настоящий binding целиком; окно не показывается, business UI не запускается.
            // Без колонок bindColumns штатно завершается до metadata/controller/mark setup.
            System.out.println("SETUP bindTable: real DSFXAdapter + ArrayDataSet + TableView; Scene/Stage attached, Stage not shown");
            adapter.bindTable(dataset, table, null, false);
            System.out.println("SETUP bindTable=RETURNED");
            require(adapter.getDataSet() == dataset && adapter.getTable() == table, "Real binding not established");
            require(table.getItems().size() == 3, "Binding did not expose Dataset rows");
            for (int i = 0; i < rows.size(); i++)
                require(table.getItems().get(i) == dataset.getRows().get(i), "Items/rows identity mismatch");
        }

        State snapshot(String label) {
            State state = new State(table.getSelectionModel().getSelectedIndex(),
                    table.getSelectionModel().getSelectedItem(), dataset.getCurrentRowNum(),
                    dataset.getCurrentRow(), navigationCallbacks, selectionCallbacks, inputEvents);
            System.out.printf("%s table.index=%d table.item=%s dataset.index=%d dataset.row=%s NAV=%d SEL=%d INPUT=%d%n",
                    label, state.tableIndex(), state.tableItem(), state.datasetIndex(), state.datasetRow(),
                    state.navigationCallbacks(), state.selectionCallbacks(), state.inputEvents());
            return state;
        }
    }

    private static <T> T onFx(Callable<T> operation) throws Exception {
        require(!Platform.isFxApplicationThread(), "onFx must be called from the coordinator thread");
        checkAsyncFailure();
        FutureTask<T> task = new FutureTask<>(operation);
        Platform.runLater(task);
        T result = task.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        checkAsyncFailure();
        return result;
    }

    private static void flushFx() throws Exception {
        // Каждый barrier ставится после завершения предыдущего: учитываются вложенные runLater.
        // Для известных путей positionRow/super.navigated/rowLoader достаточно четырёх циклов.
        // Это ограниченная глубина изолированного probe, а не обещание глобального idle.
        for (int cycle = 0; cycle < 4; cycle++) onFx(() -> null);
    }

    private static State baseline(Fixture fixture, String phase, int index) throws Exception {
        onFx(() -> {
            fixture.phase = phase + " RESET";
            if (fixture.dataset.getCurrentRowNum() != index)
                require(fixture.dataset.setCurrentRowNum(index), "Dataset baseline setter failed");
            fixture.table.getSelectionModel().clearAndSelect(index);
            return null;
        });
        flushFx();
        return onFx(() -> {
            fixture.phase = phase;
            State state = fixture.snapshot(phase + " BEFORE");
            require(aligned(fixture, state, index), phase + " baseline is not aligned");
            return state;
        });
    }

    private static boolean aligned(Fixture fixture, State state, int index) {
        return state.tableIndex() == index && state.datasetIndex() == index
                && state.tableItem() == fixture.rows.get(index) && state.datasetRow() == fixture.rows.get(index);
    }

    private static State action(Fixture fixture, String name, String description, Runnable operation) throws Exception {
        onFx(() -> {
            System.out.println(name + " ACTION " + description);
            operation.run();
            fixture.snapshot(name + " IMMEDIATE");
            return null;
        });
        flushFx();
        return onFx(() -> fixture.snapshot(name + " AFTER flush=4"));
    }

    private static String runProbe(Fixture fixture) throws Exception {
        flushFx();
        onFx(() -> fixture.snapshot("INITIAL AFTER BIND flush=4"));
        System.out.println("P2 control runs before the D3 verdict; no mouse/key events are generated.");

        baseline(fixture, "P2", 0);
        State p2 = action(fixture, "P2", "dataset.setCurrentRowNum(2)", () -> {
            require(fixture.dataset.setCurrentRowNum(2), "P2 Dataset setter failed");
        });
        require(aligned(fixture, p2, 2), "P2 FAIL: Dataset navigation did not select row 2; probe INVALID");
        System.out.println("P2 RESULT=PASS");

        State before = baseline(fixture, "P1", 0);
        State p1 = action(fixture, "P1", "table.select(2)", () -> fixture.table.getSelectionModel().select(2));
        require(p1.inputEvents() == before.inputEvents(), "P1 contaminated by mouse/key events");
        String verdict;
        if (p1.tableIndex() == 2 && p1.tableItem() == fixture.rows.get(2)
                && p1.datasetIndex() == 0 && p1.datasetRow() == fixture.rows.get(0)) {
            verdict = "CONFIRMED";
            System.out.println("P1 RESULT=CONFIRMED_GAP");
        } else if (aligned(fixture, p1, 2)) {
            verdict = "REJECTED";
            System.out.println("P1 RESULT=REJECTED_GAP");
        } else {
            throw new IllegalStateException("P1 unexpected state; verdict UNKNOWN");
        }

        baseline(fixture, "P3", 0);
        State p3 = action(fixture, "P3", "table.selectLast()", () -> fixture.table.getSelectionModel().selectLast());
        require(p3.tableIndex() == 2 && p3.tableItem() == fixture.rows.get(2), "P3 selection did not reach last row");
        System.out.println("P3 RESULT=OBSERVED dataset.index=" + p3.datasetIndex());

        baseline(fixture, "P4", 1);
        action(fixture, "P4", "table.clearSelection()", () -> fixture.table.getSelectionModel().clearSelection());
        System.out.println("P4 RESULT=OBSERVED (no Dataset expectation imposed)");

        baseline(fixture, "P5", 0);
        State p5 = action(fixture, "P5", "dataset baseline=0 -> 1 -> 2 in one FX task", () -> {
            // Пока эта FX task выполняется, queued positionRow не может исполниться.
            require(fixture.dataset.setCurrentRowNum(1), "P5 setter 1 failed");
            require(fixture.dataset.setCurrentRowNum(2), "P5 setter 2 failed");
        });
        require(p5.datasetIndex() == 2 && p5.datasetRow() == fixture.rows.get(2), "P5 Dataset navigation invalid");
        System.out.println("P5 RESULT=" + (aligned(fixture, p5, 2) ? "PASS" : "STALE_OR_MISMATCHED_SELECTION"));
        checkAsyncFailure();
        return verdict;
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new IllegalStateException(message);
    }

    private static void checkAsyncFailure() {
        Throwable failure = ASYNC_FAILURE.get();
        if (failure != null) throw new IllegalStateException("Uncaught FX task failure", failure);
    }

    public static void main(String[] args) {
        int exitCode = 2;
        try {
            require(Runtime.version().feature() == 25, "Java 25 required");
            CountDownLatch started = new CountDownLatch(1);
            Platform.startup(() -> {
                Thread.currentThread().setUncaughtExceptionHandler((thread, failure) -> ASYNC_FAILURE.compareAndSet(null, failure));
                Platform.setImplicitExit(false);
                started.countDown();
            });
            require(started.await(TIMEOUT_SECONDS, TimeUnit.SECONDS), "JavaFX startup timed out");
            String javafxVersion = System.getProperty("javafx.version", "UNKNOWN");
            System.out.println("Java=" + Runtime.version() + " JavaFX=" + javafxVersion);
            require(javafxVersion.equals("25") || javafxVersion.startsWith("25."), "JavaFX 25 required");
            System.out.println("DSFXAdapter=" + DSFXAdapter.class.getProtectionDomain().getCodeSource().getLocation());
            Fixture fixture = onFx(Fixture::new);
            String verdict = runProbe(fixture);
            onFx(() -> { fixture.stage.close(); return null; });
            checkAsyncFailure();
            System.out.println("SCENARIOS COMPLETED=P1,P2,P3,P4,P5,INITIAL_BINDING");
            System.out.println("D3 PROGRAMMATIC SELECTION GAP — " + verdict);
            System.out.println("PROBE STATUS=VALID EXIT=0");
            exitCode = 0;
        } catch (Throwable failure) {
            System.out.println("D3 PROGRAMMATIC SELECTION GAP — UNKNOWN");
            System.out.println("PROBE STATUS=INVALID EXIT=2");
            failure.printStackTrace(System.err);
        } finally {
            Platform.exit();
        }
        System.exit(exitCode);
    }
}
