import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import ru.inversion.fx.app.BaseApp;
import ru.inversion.fx.app.service.ViewPrefAppService;
import ru.inversion.fx.form.ViewContext;
import ru.inversion.fx.form.controls.JInvTextField;
import ru.inversion.fx.form.lov.AbstractLovBase;
import ru.inversion.fx.form.lov.ILov;
import ru.inversion.fx.form.lov.JInvLOVButton;

public final class TextFieldLovDelayedCompletionSmoke {
    private enum Route { DIRECT, F9, BUTTON }
    private enum Transition { KEEP, REPLACE, DETACH, REATTACH }

    private static final class App extends BaseApp {
        static void install() { g_app = new App(); }
        @Override public ViewPrefAppService getViewPrefService() { return new ViewPrefAppService(); }
    }

    private static final class Field extends JInvTextField {
        int focusRequests;
        @Override public void requestFocus() { focusRequests++; super.requestFocus(); }
    }

    private static final class Provider extends AbstractLovBase<String> {
        final ArrayDeque<BiConsumer<Boolean, ILov<String>>> pending = new ArrayDeque<>();
        @Override public void showChoiceList(ViewContext view, String filter, BiConsumer<Boolean, ILov<String>> callback) {
            require(view.getStage().isShowing(), "shown owner");
            pending.addLast(callback);
        }
        void complete(boolean accepted) { pending.removeFirst().accept(accepted, this); }
    }

    private record Outcome(Boolean accepted, ILov provider) {}

    private final Field field = new Field();
    private final JInvLOVButton button = new JInvLOVButton();
    private final Provider a = new Provider();
    private final Provider b = new Provider();
    private final SimpleStringProperty model = new SimpleStringProperty("seed");
    private final List<Outcome> outcomes = new ArrayList<>();
    private final List<String> failures = new ArrayList<>();
    private int writes;
    private int tabs;

    private void open(Route route) {
        switch (route) {
            case DIRECT -> field.showLOV((accepted, provider) -> outcomes.add(new Outcome(accepted, provider)));
            case F9 -> field.fireEvent(new KeyEvent(KeyEvent.KEY_RELEASED, "", "", KeyCode.F9, false, false, false, false));
            case BUTTON -> button.fire();
        }
    }

    private void complete(String name, Route route, boolean providerResult, boolean expected) {
        String draft = "draft " + name;
        field.setText(draft);
        int oldFocus = field.focusRequests;
        int oldTabs = tabs;
        int oldWrites = writes;
        int oldOutcomes = outcomes.size();
        a.complete(providerResult);
        int tabDelta = tabs - oldTabs;
        int focusDelta = field.focusRequests - oldFocus;
        int writeDelta = writes - oldWrites;
        var results = outcomes.subList(oldOutcomes, outcomes.size());
        boolean direct = route == Route.DIRECT;
        boolean callbackOk = direct
                ? results.size() == 1 && results.getFirst().provider() == a && results.getFirst().accepted() == expected
                : results.isEmpty();
        if (!callbackOk || tabDelta != (!direct && expected ? 1 : 0)
                || focusDelta != (direct ? 0 : 1) || writeDelta != 0 || !draft.equals(model.get())) {
            failures.add(name);
        }
        System.out.printf("%s providerResult=%s callback=%s TAB=%d focus=%d writes=%d draft=%s%n",
                name, providerResult, results.stream().map(Outcome::accepted).toList(),
                tabDelta, focusDelta, writeDelta, model.get().equals(draft));
    }

    private void run() {
        var stage = new Stage();
        stage.setScene(new Scene(new VBox(field, button), 360, 180));
        stage.show();
        try {
            button.setTextField(field);
            field.textProperty().bindBidirectional(model);
            model.addListener((p, before, after) -> writes++);
            field.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
                if (event.getCode() == KeyCode.TAB) {
                    tabs++;
                    event.consume();
                }
            });

            for (Route route : Route.values()) {
                for (Transition transition : Transition.values()) {
                    for (boolean accepted : List.of(true, false)) {
                        field.setLOV(a, false);
                        open(route);
                        require(a.pending.size() == 1, "open count " + route);
                        switch (transition) {
                            case KEEP -> field.setLOV(a, false);
                            case REPLACE -> field.setLOV(b, false);
                            case DETACH -> field.setLOV(null, false);
                            case REATTACH -> {
                                field.setLOV(b, false);
                                field.setLOV(a, false);
                            }
                        }
                        complete(route + "/" + transition + "/" + accepted, route,
                                accepted, accepted && transition == Transition.KEEP);
                    }
                }
            }

            field.setLOV(a, false);
            open(Route.DIRECT);
            field.setLOV(a, false);
            open(Route.DIRECT);
            require(a.pending.size() == 2, "overlapping current requests");
            complete("first concurrent current request", Route.DIRECT, true, true);
            complete("second concurrent current request", Route.DIRECT, true, true);

            open(Route.DIRECT);
            field.lovProperty().bind(new SimpleObjectProperty<AbstractLovBase>(a));
            try {
                expectBoundFailure(() -> field.setLOV(b, false));
                expectBoundFailure(field::unbindLOV);
                require(field.getLOV() == a, "bound failure keeps original provider");
            } finally {
                field.lovProperty().unbind();
            }
            complete("after two bound setter rejections", Route.DIRECT, true, true);
            int before = writes;
            a.setValue("live update");
            require(writes == before + 1 && model.get().equals("live update"), "active notification");
            field.setLOV(b, false);
            before = writes;
            a.setValue("retired update");
            require(writes == before, "retired notification");
            b.setValue("replacement update");
            require(writes == before + 1 && model.get().equals("replacement update"), "replacement notification");

            field.setLOV(a, false);
            field.setReadOnly(true);
            open(Route.F9);
            open(Route.BUTTON);
            require(a.pending.isEmpty(), "readOnly user routes blocked");
            open(Route.DIRECT);
            require(a.pending.size() == 1, "programmatic readOnly route allowed");
            complete("programmatic readOnly", Route.DIRECT, true, true);
            require(failures.isEmpty(), "incorrect completions: " + failures);
        } finally {
            stage.hide();
        }
    }

    private static void expectBoundFailure(Runnable setter) {
        try {
            setter.run();
            throw new AssertionError("expected bound setter rejection");
        } catch (RuntimeException failure) {
            require(failure.getMessage().contains("A bound value cannot be set"), "bound failure");
        }
    }

    private static void require(boolean ok, String label) {
        if (!ok) throw new AssertionError(label);
    }

    public static void main(String[] args) throws Exception {
        var task = new FutureTask<Void>(() -> {
            App.install();
            new TextFieldLovDelayedCompletionSmoke().run();
            return null;
        });
        Platform.startup(() -> Platform.runLater(task));
        try {
            task.get(20, TimeUnit.SECONDS);
            System.out.println("CTRL-078 PASS");
        } finally {
            Platform.exit();
        }
    }
}
