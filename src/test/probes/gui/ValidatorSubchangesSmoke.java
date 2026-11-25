import java.util.*;
import java.util.concurrent.*;
import javafx.application.Platform;
import ru.inversion.fx.app.BaseApp;
import ru.inversion.fx.app.service.ViewPrefAppService;
import ru.inversion.fx.form.JInvFXFormController;
import ru.inversion.fx.form.controls.JInvTextField;
import ru.inversion.fx.form.valid.Validator;
public class ValidatorSubchangesSmoke {
    static class App extends BaseApp {
        static void install() { g_app = new App(); }
        @Override public ViewPrefAppService getViewPrefService() {
            return new ViewPrefAppService() {
                @Override public boolean isShowValidatable() { return false; }
            };
        }
    }
    static void run() {
        App.install();
        var valid = new JInvFXFormController<>().getValidMan();
        var field = new JInvTextField();
        var calls = new ArrayList<String>();
        Validator<String> one = v -> { calls.add("1"); return null; };
        Validator<String> two = v -> { calls.add("2"); return null; };
        Validator<String> three = v -> { calls.add("3"); return null; };
        var other = new JInvTextField();
        var view = valid.getValidators(field);
        valid.bindValidators2Control(other, three);
        view.addAll(one,two,three);
        field.setText("x");
        verify(valid, field, calls, List.of("1","2","3"));
        view.removeAll(one,three);
        if (!view.equals(List.of(two))) throw new AssertionError(view);
        valid.validateControl(field);
        boolean disjoint = calls.equals(List.of("2"));
        System.out.println("disjoint remove: visible=[2], invoked="+calls);
        calls.clear();
        valid.unbindValidators(field,three);
        verify(valid, other, calls, List.of("3"));
        view.set(0,one);
        verify(valid, field, calls, List.of("1"));
        view.addAll(two,three,two);
        verify(valid, field, calls, List.of("1","2","3","2"));
        view.remove(1,3);
        // Удаление одного экземпляра сохраняет историческое снятие всех его регистраций.
        verify(valid, field, calls, List.of("1"));
        view.clear();
        verify(valid, field, calls, List.of());
        verify(valid, other, calls, List.of("3"));
        if (!disjoint) throw new AssertionError("disjoint removals left validator registered");
    }
    static void verify(ru.inversion.fx.form.valid.ValidMan manager, JInvTextField field,
                       List<String> actual, List<String> expected) {
        manager.validateControl(field);
        if (!actual.equals(expected)) throw new AssertionError("expected "+expected+" actual="+actual);
        actual.clear();
    }
    public static void main(String[] args) throws Exception {
        var test = new FutureTask<Void>(() -> {run(); return null;});
        Platform.startup(() -> Platform.runLater(test));
        try { test.get(20,TimeUnit.SECONDS); System.out.println("CTRL-052 PASS"); } finally { Platform.exit(); }
    }
}