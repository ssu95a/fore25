import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import ru.inversion.fx.app.BaseApp;
import ru.inversion.fx.app.service.ViewPrefAppService;
import ru.inversion.fx.form.ViewContext;
import ru.inversion.fx.form.controls.IJInvControl;
import ru.inversion.fx.form.controls.JInvTextArea;
import ru.inversion.fx.form.lov.AbstractLovBase;
import ru.inversion.fx.form.lov.ILov;
import ru.inversion.fx.form.lov.JInvLOVButton;

public final class TextAreaLovLifecycleSmoke {
    private static final class App extends BaseApp {
        static void install() { g_app = new App(); }
        @Override public ViewPrefAppService getViewPrefService() { return new ViewPrefAppService(); }
    }

    private static final class Area extends JInvTextArea {
        int copies;
        @Override protected void setTextValueFromLov() {
            copies++;
            super.setTextValueFromLov();
        }
    }

    private static final class Provider extends AbstractLovBase<String> {
        final List<String> selected = new ArrayList<>();
        final String name;
        boolean accept = true;
        int setupCalls;

        Provider(String name) { this.name = name; }

        @Override public void onSetLov(IJInvControl control) { setupCalls++; }

        @Override public void showChoiceList(ViewContext view, String filter, BiConsumer<Boolean, ILov<String>> callback) {
            require(view.getStage().isShowing(), "provider has an actual showing owner");
            selected.add(accept ? "accept" : "cancel");
            if (accept) setValue(name + "-choice-" + selected.size());
            callback.accept(accept, this);
        }
    }

    private static final class Scenario {
        final Area area = new Area();
        final Area unused = new Area();
        final Provider a = new Provider("A");
        final Provider b = new Provider("B");
        final List<Throwable> fxErrors = new ArrayList<>();
        final List<String> writes = new ArrayList<>();
        final List<String> failures = new ArrayList<>();
        final List<Node> original;
        final SimpleStringProperty model = new SimpleStringProperty("seed");

        Scenario() {
            area.getToolBar().getItems().add(new Button("Caller"));
            original = List.copyOf(area.getToolBar().getItems());
            require(!original.isEmpty() && original.stream().noneMatch(JInvLOVButton.class::isInstance),
                    "existing editor and caller buttons");
            area.textProperty().bindBidirectional(model);
            model.addListener((p, before, after) -> writes.add(after));
        }

        void expect(boolean ok, String message) {
            if (!ok) failures.add(message);
        }

        List<JInvLOVButton> lovButtons() {
            return area.getToolBar().getItems().stream().filter(JInvLOVButton.class::isInstance)
                    .map(JInvLOVButton.class::cast).toList();
        }

        void toolbar(int expected) {
            toolbar(expected, original);
        }

        List<Node> otherButtons() {
            return area.getToolBar().getItems().stream().filter(n -> !(n instanceof JInvLOVButton)).toList();
        }

        void toolbar(int expected, List<Node> expectedOthers) {
            var buttons = lovButtons();
            expect(buttons.size() == expected && otherButtons().equals(expectedOthers),
                    "toolbar LOV count " + buttons.size() + "/" + expected);
        }

        void update(Provider provider, String value, int expectedCopies, int expectedWrites) {
            int beforeCopies = area.copies;
            int beforeWrites = writes.size();
            int beforeErrors = fxErrors.size();
            provider.getValue();
            provider.setValue(value);
            int copies = area.copies - beforeCopies;
            int updates = writes.size() - beforeWrites;
            int errors = fxErrors.size() - beforeErrors;
            System.out.printf("%s %s copies=%d writes=%d exceptions=%d%n", provider.name, value, copies, updates, errors);
            expect(copies == expectedCopies && updates == expectedWrites && errors == 0, "provider update " + value);
        }

        void start() {
            toolbar(0);
            area.setLOV(a, true);
            update(a, "first", 1, 1);
            require(model.get().equals("first"), "unlocked provider positive");
            var first = lovButtons().getFirst();
            area.setLOV(a, false);
            toolbar(1);
            expect(lovButtons().getFirst() == first && a.setupCalls == 2, "repeat retains button and setup contract");
            update(a, "repeat", 1, 1);
            area.setLOV(b, true);
            toolbar(1);
            update(b, "active", 1, 1);
            area.setText("distinct draft");
            update(a, "retired A", 0, 0);
            expect(model.get().equals("distinct draft") && area.getText().equals("distinct draft"), "former provider overwrote draft");
            var beforeBoundFailure = List.copyOf(area.getToolBar().getItems());
            var beforeBoundModel = model.get();
            var source = new SimpleObjectProperty<AbstractLovBase>(b);
            area.lovProperty().bind(source);
            rejects(() -> area.setLOV(a, false));
            rejects(() -> area.setLOV(null));
            expect(area.getLOV() == b && area.lovProperty().isBound(), "bound-call identity");
            expect(area.getToolBar().getItems().equals(beforeBoundFailure) && model.get().equals(beforeBoundModel),
                    "bound failures preserve toolbar identity and model");
            update(b, "after bound failure", 1, 1);
            area.lovProperty().unbind();
            area.setLOV(null);
            area.setLOV(null);
            toolbar(0);
            update(a, "detached A", 0, 0);
            update(b, "detached B", 0, 0);
            area.setLOV(a);
            toolbar(1);
            update(a, "reattach A", 1, 1);
        }

        void settled() throws ReflectiveOperationException {
            int beforeCopies = area.copies;
            int beforeWrites = writes.size();
            lovButtons().getFirst().fire();
            area.fireEvent(new KeyEvent(KeyEvent.KEY_RELEASED, "", "", KeyCode.F9, false, false, false, false));
            expect(a.selected.equals(List.of("accept", "accept")), "toolbar and F9 use current provider");
            expect(area.copies - beforeCopies == 4 && writes.size() - beforeWrites == 2
                    && writes.subList(beforeWrites, writes.size()).equals(List.of("A-choice-1", "A-choice-2")),
                    "accepted choice keeps two copy attempts and one model event per action");
            area.setReadOnly(true);
            area.fireEvent(new KeyEvent(KeyEvent.KEY_RELEASED, "", "", KeyCode.F9, false, false, false, false));
            expect(a.selected.size() == 2, "read-only F9 must stay blocked");
            area.setReadOnly(false);
            a.accept = false;
            var outcomes = new ArrayList<Boolean>();
            beforeCopies = area.copies;
            beforeWrites = writes.size();
            area.showLOV((ok, provider) -> {
                require(provider == a, "cancel callback provider");
                outcomes.add(ok);
            });
            expect(outcomes.equals(List.of(false)) && area.copies == beforeCopies && writes.size() == beforeWrites,
                    "cancellation reports once without copying");
            // Переключение editable может изменить набор кнопок внешнего редактора.
            var afterEditableChange = otherButtons();
            require(afterEditableChange.containsAll(original), "original toolbar items survive editable toggle");
            area.setLOV(null);
            toolbar(0, afterEditableChange);
            update(a, "after settled detach", 0, 0);
            require(unused.getLOV() == null && unused.getToolBar().getItems().stream().noneMatch(JInvLOVButton.class::isInstance),
                    "unused area has no LOV button");
            System.out.printf("choices=%s model=%s uncaught FX errors=%d%n", a.selected, writes, fxErrors.size());
            require(failures.isEmpty(), "LOV lifecycle failures: " + failures);
            var attachment = JInvTextArea.class.getDeclaredField("lovAttachment");
            attachment.setAccessible(true);
            require(attachment.get(unused) == null, "unused area has no LOV attachment");
        }
    }

    private static void rejects(Runnable request) {
        try { request.run(); }
        catch (RuntimeException error) {
            require(error.getMessage().contains("A bound value cannot be set"), "original bound-property rejection");
            return;
        }
        throw new AssertionError("expected a bound-property rejection");
    }

    public static void main(String[] args) {
        var done = new CompletableFuture<Void>();
        Platform.startup(() -> Platform.runLater(() -> {
            try {
                App.install();
                var test = new Scenario();
                var stage = new Stage();
                stage.setScene(new Scene(new VBox(test.area, test.unused), 360, 260));
                stage.show();
                var thread = Thread.currentThread();
                var handler = thread.getUncaughtExceptionHandler();
                thread.setUncaughtExceptionHandler((t, failure) -> test.fxErrors.add(failure));
                test.start();
                Platform.runLater(() -> {
                    try { test.settled(); done.complete(null); }
                    catch (Throwable failure) { done.completeExceptionally(failure); }
                    finally { thread.setUncaughtExceptionHandler(handler); stage.hide(); }
                });
            } catch (Throwable failure) { done.completeExceptionally(failure); }
        }));
        int exit = 1;
        try {
            done.get(20, TimeUnit.SECONDS);
            System.out.println("CTRL-050 PASS");
            exit = 0;
        } catch (Exception error) {
            error.printStackTrace(System.err);
        } finally {
            Platform.exit();
        }
        System.exit(exit);
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
