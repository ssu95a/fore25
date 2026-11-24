import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import ru.inversion.fx.app.BaseApp;
import ru.inversion.fx.app.service.ViewPrefAppService;
import ru.inversion.fx.form.JInvFXFormController;
import ru.inversion.fx.form.controls.JInvTextField;
import ru.inversion.fx.form.valid.Validator;

public final class ValidatorScopedUnbindSmoke {
    private static final class App extends BaseApp {
        static void install() { g_app = new App(); }
        @Override public ViewPrefAppService getViewPrefService() {
            return new ViewPrefAppService() {
                @Override public boolean isShowValidatable() { return false; }
            };
        }
    }

    private static boolean exercise(boolean reverse) {
        var manager = new JInvFXFormController<>().getValidMan();
        var removed = new JInvTextField();
        var kept = new JInvTextField();
        var a = reverse ? kept : removed;
        var b = reverse ? removed : kept;
        var av = new SimpleStringProperty("A");
        var bv = new SimpleStringProperty("B");
        a.textProperty().bindBidirectional(av);
        b.textProperty().bindBidirectional(bv);
        var validations = new ArrayList<String>();
        var model = new ArrayList<String>();
        av.addListener((p, old, value) -> model.add("A=" + value));
        bv.addListener((p, old, value) -> model.add("B=" + value));
        Validator<String> shared = value -> { validations.add("shared=" + value); return null; };
        Validator<String> distinct = value -> { validations.add("distinct=" + value); return null; };
        manager.bindValidators2Control(a, shared);
        manager.bindValidators2Control(b, shared);
        manager.bindValidators2Control(removed, shared, distinct);
        String removedValue = removed.getText();
        String keptValue = kept.getText();
        manager.validateControl(removed);
        manager.validateControl(kept);
        require(validations.equals(List.of("shared=" + removedValue, "shared=" + removedValue,
                "distinct=" + removedValue, "shared=" + keptValue)) && model.isEmpty(), "positive controls");
        validations.clear();
        manager.unbindValidators(removed, shared);
        removed.setText("removed");
        kept.setText("kept");
        manager.validateControl(removed);
        manager.validateControl(kept);
        require(model.equals(reverse ? List.of("B=removed", "A=kept") : List.of("A=removed", "B=kept")),
                "exactly one notification per edit " + model);
        System.out.printf("remove %s: validators=%s model=%s%n", removedValue, validations, model);
        return manager.getValidators(removed).equals(List.of(distinct))
                && manager.getValidators(kept).equals(List.of(shared))
                && validations.equals(List.of("distinct=removed", "shared=kept"));
    }

    public static void main(String[] args) {
        var test = new FutureTask<Void>(() -> {
            require(Runtime.version().feature() == 25
                    && System.getProperty("javafx.version", "").matches("25(?:\\..*)?"), "Java/JavaFX 25");
            App.install();
            boolean forward = exercise(false);
            boolean reverse = exercise(true);
            require(forward && reverse, "unbind must retain shared validator on the other control");
            return null;
        });
        Platform.startup(() -> Platform.runLater(test));
        int exit = 1;
        try {
            test.get(20, TimeUnit.SECONDS);
            System.out.println("CTRL-033 PASS");
            exit = 0;
        } catch (Exception failure) {
            failure.printStackTrace(System.err);
        } finally {
            Platform.exit();
        }
        System.exit(exit);
    }

    private static void require(boolean condition, String label) {
        if (!condition) throw new AssertionError(label);
    }
}
