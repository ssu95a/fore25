import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.scene.Scene;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import ru.inversion.fx.app.BaseApp;
import ru.inversion.fx.app.service.ViewPrefAppService;
import ru.inversion.fx.form.ViewContext;
import ru.inversion.fx.form.controls.JInvTextArea;
import ru.inversion.fx.form.controls.JInvTextField;
import ru.inversion.fx.form.lov.AbstractLovBase;
import ru.inversion.fx.form.lov.ILov;

public final class TextAreaLovDelayedCompletionSmoke {
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
        final List<BiConsumer<Boolean, ILov<String>>> pending = new ArrayList<>();
        @Override public void showChoiceList(ViewContext view, String filter, BiConsumer<Boolean, ILov<String>> callback) {
            require(view.getStage().isShowing(), "provider must have a showing owner");
            pending.add(callback);
        }
        void complete(boolean accepted) { pending.removeFirst().accept(accepted, this); }
    }

    private static final class Scenario {
        final Area area = new Area();
        final JInvTextField field = new JInvTextField();
        final Provider a = new Provider();
        final Provider b = new Provider();
        final SimpleStringProperty model = new SimpleStringProperty("seed");
        final List<String> writes = new ArrayList<>();
        final List<Boolean> outcomes = new ArrayList<>();
        final List<String> failures = new ArrayList<>();
        int focusRequests;
        int navigationRequests;

        void open(Provider expected) {
            int before = expected.pending.size();
            area.showLOV((accepted, provider) -> {
                require(provider == expected, "completion retains the reporting provider");
                outcomes.add(accepted);
                if (accepted) navigationRequests++;
                else focusRequests++;
            });
            require(expected.pending.size() == before + 1, "choice opened once");
        }

        void complete(String name, Provider provider, boolean result, boolean expectedResult, int expectedWrites) {
            area.setText("draft");
            int beforeCopies = area.copies;
            int beforeWrites = writes.size();
            int beforeOutcomes = outcomes.size();
            int beforeNavigation = navigationRequests;
            int beforeFocus = focusRequests;
            try {
                provider.complete(result);
            } catch (RuntimeException error) {
                failures.add(name + " threw " + error);
            }
            int copies = area.copies - beforeCopies;
            int changes = writes.size() - beforeWrites;
            var callbacks = outcomes.subList(beforeOutcomes, outcomes.size());
            int navigation = navigationRequests - beforeNavigation;
            int focus = focusRequests - beforeFocus;
            System.out.printf("%s copies=%d writes=%d callbacks=%s navigation=%d focus=%d%n",
                    name, copies, changes, callbacks, navigation, focus);
            if (copies != (expectedResult ? 1 : 0) || changes != expectedWrites
                    || !callbacks.equals(List.of(expectedResult)) || navigation != (expectedResult ? 1 : 0)
                    || focus != (expectedResult ? 0 : 1)
                    || !model.get().equals(expectedResult ? provider.getValue() : "draft")) {
                failures.add(name);
            }
        }

        void run() {
            area.textProperty().bindBidirectional(model);
            model.addListener((p, oldValue, value) -> writes.add(value));
            area.setLOV(a);
            open(a);
            a.setValue("active A");
            require(area.copies == 1 && writes.equals(List.of("active A")), "programmatic A update");
            complete("current accepted", a, true, true, 1);
            open(a);
            complete("current cancelled", a, false, false, 0);

            open(a);
            area.setLOV(b);
            b.setValue("active B");
            require(model.get().equals("active B"), "programmatic B update");
            int copies = area.copies;
            int changes = writes.size();
            a.setValue("retired A");
            require(area.copies == copies && writes.size() == changes, "retired value notification");
            complete("A to B accepted", a, true, false, 0);
            open(b);
            area.setLOV(null);
            complete("B to null accepted", b, true, false, 0);

            area.setLOV(a);
            open(a);
            area.setLOV(b);
            area.setLOV(a);
            complete("A to B to A accepted", a, true, false, 0);
            open(a);
            area.setLOV(b);
            complete("A to B cancelled", a, false, false, 0);

            open(b);
            area.setLOV(b);
            open(b);
            complete("older overlapping request", b, true, true, 1);
            complete("same provider overlapping cancellation", b, false, false, 0);
            open(b);
            area.lovProperty().bind(new SimpleObjectProperty<AbstractLovBase>(b));
            try {
                area.setLOV(a);
                throw new AssertionError("expected bound-property rejection");
            } catch (RuntimeException error) {
                require(error.getMessage().contains("A bound value cannot be set"), "bound-property error");
            } finally {
                area.lovProperty().unbind();
            }
            complete("after bound setter failure", b, true, true, 1);

            field.setLOV(a, false);
            var fieldCallbacks = new ArrayList<Boolean>();
            field.showLOV((ok, provider) -> fieldCallbacks.add(ok));
            field.setLOV(b, false);
            field.setText("field draft");
            a.complete(true);
            System.out.printf("TextField late completion callbacks=%s draft=%s%n", fieldCallbacks, field.getText());
            require(failures.isEmpty(), "delayed completion violations: " + failures);
        }
    }

    public static void main(String[] args) throws Exception {
        var task = new FutureTask<Void>(() -> {
            App.install();
            var test = new Scenario();
            var stage = new Stage();
            stage.setScene(new Scene(new VBox(test.area, test.field), 360, 260));
            stage.show();
            try { test.run(); }
            finally { stage.hide(); }
            return null;
        });
        Platform.startup(() -> Platform.runLater(task));
        try {
            task.get(20, TimeUnit.SECONDS);
            System.out.println("CTRL-062 PASS");
        } finally {
            Platform.exit();
        }
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
