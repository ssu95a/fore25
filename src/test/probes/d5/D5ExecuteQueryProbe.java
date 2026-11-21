import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.scene.Scene;
import javafx.scene.control.TableView;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import ru.inversion.dataset.ArrayDataSet;
import ru.inversion.dataset.IDataSet;
import ru.inversion.dataset.fx.DSFXAdapter;
import ru.inversion.fx.form.IFormStateListener;
import ru.inversion.fx.form.StateEnum;

class D5ExecuteQueryProbe {
    record Row(String name) {}
    record Result(boolean ok, Throwable error) {}

    public static void main(String[] args) throws Exception {
        require(Runtime.version().feature() == 25, "Java 25 required");
        var asyncFailure = new AtomicReference<Throwable>();
        var test = new FutureTask<Void>(() -> {
            Thread.currentThread().setUncaughtExceptionHandler((thread, error) -> asyncFailure.compareAndSet(null, error));
            return run();
        });
        Platform.startup(test);
        try {
            test.get(30, TimeUnit.SECONDS);
            var barrier = new FutureTask<Void>(() -> null);
            Platform.runLater(barrier);
            barrier.get(30, TimeUnit.SECONDS);
            if (asyncFailure.get() != null) {
                throw new AssertionError("Queued JavaFX operation failed", asyncFailure.get());
            }
            System.out.println("AFTER FIX PASS (real adapter and JavaFX; full-scan dialog excluded)");
        } finally {
            Platform.exit();
        }
    }

    @SuppressWarnings("unchecked")
    static Void run() throws Exception {
        var adapter = new DSFXAdapter<Row>();
        var calls = new ArrayList<Result>();
        var queries = new ArrayList<Boolean>();
        var injectedError = new IllegalStateException("controlled query error");
        BiConsumer<Boolean, Throwable> callback = (ok, error) -> {
            require(Platform.isFxApplicationThread(), "Unexpected callback thread");
            calls.add(new Result(ok, error));
        };
        adapter.executeQuery(callback, null, false, true);
        require(calls.isEmpty(), "Null Dataset callback");
        System.out.println("null-dataset callbacks=" + calls);

        var real = new ArrayDataSet<>(Row.class,
                FXCollections.observableArrayList(new Row("A")), false);
        // Вызовы Dataset делегируются реальному ArrayDataSet. Ошибка вводится на границе запроса.
        var dataset = (IDataSet<Row>) Proxy.newProxyInstance(IDataSet.class.getClassLoader(),
                new Class<?>[]{IDataSet.class}, (proxy, method, args) -> {
                    if (method.getName().equals("executeQuery") && method.getParameterCount() == 1) {
                        boolean readAll = (boolean) args[0];
                        queries.add(readAll);
                        if (!readAll) throw injectedError;
                    }
                    try {
                        return method.invoke(real, args);
                    } catch (InvocationTargetException error) {
                        throw error.getCause();
                    }
                });
        var table = new TableView<Row>();
        var stage = new Stage();
        stage.setScene(new Scene(new StackPane(table), 320, 200));
        adapter.bindTable(dataset, table, null, false);
        var listener = new IFormStateListener() {
            private final SimpleObjectProperty<StateEnum> state = new SimpleObjectProperty<>();
            private final SimpleStringProperty text = new SimpleStringProperty();
            public SimpleObjectProperty<StateEnum> stateProperty() { return state; }
            public SimpleStringProperty stateTextProperty() { return text; }
        };

        adapter.executeQuery(callback, listener, false, true);
        require(queries.equals(List.of(true)) && calls.equals(List.of(new Result(true, null))),
                "Unexpected success trace: " + queries + " " + calls);
        System.out.println("success queries=" + queries + " callbacks=" + calls);

        adapter.executeQuery(callback, listener, false, false);
        require(queries.equals(List.of(true, false))
                        && calls.equals(List.of(new Result(true, null), new Result(false, injectedError))),
                "Unexpected error trace: " + queries + " " + calls);
        System.out.println("query-error queries=" + queries + " callbacks=" + calls);

        try {
            adapter.executeQuery(callback, null, false, true);
            throw new AssertionError("Expected controller cast failure");
        } catch (ClassCastException expected) {
            require(queries.size() == 2 && calls.size() == 2, "Pre-try failure ran query/callback");
            System.out.println("pre-try-cast escaped=ClassCastException");
        }
        var callbackError = new IllegalStateException("controlled callback error");
        BiConsumer<Boolean, Throwable> throwingCallback = (ok, error) -> {
            callback.accept(ok, error);
            throw callbackError;
        };
        for (boolean readAll : new boolean[]{true, false}) {
            calls.clear();
            queries.clear();
            try {
                adapter.executeQuery(throwingCallback, listener, false, readAll);
                throw new AssertionError("Callback exception did not escape");
            } catch (IllegalStateException error) {
                require(error == callbackError, "Unexpected escaped exception: " + error);
            }
            var expected = new Result(readAll, readAll ? null : injectedError);
            require(queries.equals(List.of(readAll)) && calls.equals(List.of(expected)),
                    "Unexpected throwing-callback trace: " + queries + " " + calls);
            System.out.println("throwing-callback queries=" + queries + " callbacks=" + calls);
        }
        return null;
    }

    static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
