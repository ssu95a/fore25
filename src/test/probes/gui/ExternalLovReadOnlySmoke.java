import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import ru.inversion.fx.app.BaseApp;
import ru.inversion.fx.app.service.ViewPrefAppService;
import ru.inversion.fx.form.ViewContext;
import ru.inversion.fx.form.controls.JInvTextArea;
import ru.inversion.fx.form.controls.JInvTextField;
import ru.inversion.fx.form.controls.skin.JInvTextFieldButtonSkin;
import ru.inversion.fx.form.lov.AbstractLovBase;
import ru.inversion.fx.form.lov.ILov;
import ru.inversion.fx.form.lov.JInvLOVButton;

public final class ExternalLovReadOnlySmoke {
    private static final class App extends BaseApp {
        static void install() { g_app = new App(); }
        @Override public ViewPrefAppService getViewPrefService() { return new ViewPrefAppService(); }
    }

    private static final class Field extends JInvTextField {
        int focusRequests;
        int copies;
        @Override public void requestFocus() { focusRequests++; super.requestFocus(); }
        @Override protected void setTextValueFromLov() { copies++; super.setTextValueFromLov(); }
    }

    private static final class Provider extends AbstractLovBase<String> {
        int opens;
        int callbacks;
        @Override public void showChoiceList(ViewContext view, String filter, BiConsumer<Boolean, ILov<String>> done) {
            require(view.getStage().isShowing(), "shown Stage");
            setValue("choice-" + ++opens);
            callbacks++;
            done.accept(true, this);
        }
    }

    private final CompletableFuture<Void> done = new CompletableFuture<>();
    private final List<String> failures = new ArrayList<>();

    private void expect(boolean ok, String label) {
        if (!ok) failures.add(label);
    }

    private void start() {
        App.install();
        var field = new Field();
        var external = new JInvLOVButton();
        var area = new JInvTextArea();
        var provider = new Provider();
        var areaProvider = new Provider();
        var standalone = new Provider();
        var standaloneButton = new JInvLOVButton(standalone);
        var stage = new Stage();
        stage.setScene(new Scene(new VBox(field, external, area, standaloneButton), 390, 290));
        int[] bubbledF9 = {0};
        stage.getScene().getRoot().addEventHandler(KeyEvent.KEY_RELEASED, event -> {
            if (event.getCode() == KeyCode.F9) bubbledF9[0]++;
        });
        stage.show();
        external.setTextField(field);
        field.setLOV(provider, true);
        area.setLOV(areaProvider, true);
        var areaButton = area.getToolBar().getItems().stream().filter(JInvLOVButton.class::isInstance)
                .map(JInvLOVButton.class::cast).findFirst().orElseThrow();
        var fieldModel = new SimpleStringProperty("seed");
        var areaModel = new SimpleStringProperty("area seed");
        field.textProperty().bindBidirectional(fieldModel);
        area.textProperty().bindBidirectional(areaModel);
        var writes = new ArrayList<String>();
        var areaWrites = new ArrayList<String>();
        fieldModel.addListener((p, before, after) -> writes.add(after));
        areaModel.addListener((p, before, after) -> areaWrites.add(after));
        int[] tabs = {0};
        field.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.TAB) {
                tabs[0]++;
                event.consume();
            }
        });

        later(() -> {
            var inner = ((JInvTextFieldButtonSkin) field.getSkin()).getButton();
            require(field.externalButtonProperty().get() == external && inner.isVisible()
                    && !external.isDisabled(), "external-first editable setup");
            external.fire();
            inner.fire();
            f9(field);
            require(provider.opens == 3 && provider.callbacks == 3 && field.copies == 3 && tabs[0] == 3
                    && bubbledF9[0] == 0 && writes.equals(List.of("choice-1", "choice-2", "choice-3")), "three editable routes");
            field.setEditable(false);
            external.fire();
            f9(field);
            require(provider.opens == 4 && bubbledF9[0] == 1, "editable=false still permits legacy external activation");
            field.setButtonIgnoresEditable(true);
            inner.fire();
            f9(field);
            external.fire();
            require(provider.opens == 7 && writes.size() == 7 && tabs[0] == 7 && bubbledF9[0] == 1,
                    "ignoreEditable preserves all three routes");
            standaloneButton.fire();
            areaButton.fire();
            require(standalone.opens == 1 && standalone.callbacks == 1 && areaProvider.opens == 1
                    && areaWrites.equals(List.of("choice-1")), "standalone and editable TextArea");
            area.setReadOnly(true);
            int areaBefore = areaProvider.opens;
            areaButton.fire();
            System.out.printf("TextArea locked disabled=%s opens=%d%n", areaButton.isDisabled(), areaProvider.opens - areaBefore);
            expect(areaProvider.opens == areaBefore && areaWrites.size() == 1, "read-only TextArea activation");
            areaProvider.setValue("program area");
            require(areaModel.get().equals("program area"), "TextArea programmatic update");
            field.setEditable(true);
            later(() -> checkLocked(field, provider, external, inner, writes, tabs, bubbledF9, stage));
        });
    }

    private void checkLocked(Field field, Provider provider, JInvLOVButton external,
                             javafx.scene.control.Button inner, List<String> writes, int[] tabs, int[] bubbledF9, Stage stage) {
        field.setReadOnly(true);
        int opens = provider.opens;
        int callbacks = provider.callbacks;
        int copies = field.copies;
        int changes = writes.size();
        int navigation = tabs[0];
        int focus = field.focusRequests;
        for (boolean ignore : new boolean[]{false, true}) {
            field.setButtonIgnoresEditable(ignore);
            require(!external.isDisabled() && inner.isDisabled(), "external still enabled; inner disabled");
            external.fire();
            inner.fire();
            if (!ignore) f9(field);
        }
        later(() -> {
            System.out.printf("locked external opens=%d callbacks=%d copies=%d writes=%d TAB=%d focus requests=%d%n",
                    provider.opens - opens, provider.callbacks - callbacks, field.copies - copies, writes.size() - changes,
                    tabs[0] - navigation, field.focusRequests - focus);
            expect(provider.opens == opens && provider.callbacks == callbacks && field.copies == copies
                    && writes.size() == changes && tabs[0] == navigation && field.focusRequests == focus, "locked external activation");
            provider.setValue("program field");
            require(writes.getLast().equals("program field"), "programmatic update still works");
            int beforeProgrammatic = provider.opens;
            field.showLOV(null);
            require(provider.opens == beforeProgrammatic + 1, "programmatic show remains allowed");
            later(() -> checkLockedF9(field, provider, external, writes, tabs, bubbledF9, stage));
        });
    }

    private void checkLockedF9(Field field, Provider provider, JInvLOVButton external,
                               List<String> writes, int[] tabs, int[] bubbledF9, Stage stage) {
        int focus = field.focusRequests;
        for (boolean editable : new boolean[]{false, true}) {
            field.setEditable(editable);
            for (boolean ignore : new boolean[]{false, true}) {
                field.setButtonIgnoresEditable(ignore);
                int opens = provider.opens;
                int callbacks = provider.callbacks;
                int copies = field.copies;
                int changes = writes.size();
                int navigation = tabs[0];
                int bubbled = bubbledF9[0];
                f9(field);
                System.out.printf("locked F9 editable=%s ignore=%s opens=%d callbacks=%d copies=%d writes=%d TAB=%d bubbled=%d%n",
                        editable, ignore, provider.opens - opens, provider.callbacks - callbacks, field.copies - copies,
                        writes.size() - changes, tabs[0] - navigation, bubbledF9[0] - bubbled);
                expect(provider.opens == opens && provider.callbacks == callbacks && field.copies == copies
                        && writes.size() == changes && tabs[0] == navigation && bubbledF9[0] == bubbled + 1,
                        "locked F9 editable=" + editable + " ignore=" + ignore);
            }
        }
        later(() -> {
            expect(field.focusRequests == focus, "locked F9 focus requests");
            field.setReadOnly(false);
            int before = provider.opens;
            int navigation = tabs[0];
            external.fire();
            require(provider.opens == before + 1 && tabs[0] == navigation + 1, "unlock restores external action");
            f9(field);
            require(provider.opens == before + 2 && tabs[0] == navigation + 2, "unlock restores F9 action");
            stage.hide();
            require(failures.isEmpty(), "external and keyboard LOV readOnly: " + failures);
            done.complete(null);
        });
    }

    private static void f9(Field field) {
        field.fireEvent(new KeyEvent(KeyEvent.KEY_RELEASED, "", "", KeyCode.F9, false, false, false, false));
    }

    private void later(Runnable step) {
        Platform.runLater(() -> {
            try { step.run(); } catch (Throwable failure) { done.completeExceptionally(failure); }
        });
    }

    public static void main(String[] args) {
        var smoke = new ExternalLovReadOnlySmoke();
        Platform.startup(() -> smoke.later(smoke::start));
        int exit = 1;
        try {
            smoke.done.get(25, TimeUnit.SECONDS);
            System.out.println("CTRL-061 PASS");
            exit = 0;
        } catch (Exception failure) {
            failure.printStackTrace(System.err);
        } finally {
            Platform.exit();
        }
        System.exit(exit);
    }

    private static void require(boolean ok, String label) {
        if (!ok) throw new AssertionError(label);
    }
}
