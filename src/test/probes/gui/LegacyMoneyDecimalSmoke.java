import java.lang.ref.Reference;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import javafx.animation.AnimationTimer;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import ru.inversion.fx.form.controls.AbstractBigDecimalField;
import ru.inversion.fx.form.controls.IStateControl.State;
import ru.inversion.fx.form.controls.JInvBigDecimalField;
import ru.inversion.fx.form.controls.JInvMoneyField;
import ru.inversion.fx.form.controls.JInvNumberField;
import ru.inversion.fx.form.controls.MoneyStringConverter;
import ru.inversion.fx.form.controls.skin.JInvTextFieldButtonSkin;

public final class LegacyMoneyDecimalSmoke {
    private static final int BATCH = 100;
    private record Counts(int text, int value) {}

    private static final class Watched {
        final AbstractBigDecimalField field;
        int textEvents;
        int valueEvents;

        Watched(AbstractBigDecimalField field) {
            this.field = field;
            field.textProperty().addListener((property, before, after) -> textEvents++);
            field.valueProperty().addListener((property, before, after) -> valueEvents++);
        }

        Counts counts() { return new Counts(textEvents, valueEvents); }

        String snapshot(Counts before) {
            return "text=[" + field.getText() + "] value=" + field.getValue() + " state=" + field.getState()
                    + " textEvents=" + (textEvents - before.text()) + " valueEvents=" + (valueEvents - before.value());
        }
    }

    private final CompletableFuture<List<String>> verdict = new CompletableFuture<>();
    private final List<String> failures = new ArrayList<>();
    private final MoneyStringConverter parser = new MoneyStringConverter();
    private Locale originalLocale;
    private DecimalFormat originalMoneyFormat;
    private Stage stage;
    private Watched money;
    private Watched decimal;
    private JInvBigDecimalField peer;
    private Button sink;
    private boolean started;

    public static void main(String[] args) {
        int exit = 2;
        try {
            require(Runtime.version().feature() == 25, "setup: Java 25 required");
            var probe = new LegacyMoneyDecimalSmoke();
            Platform.startup(() -> probe.checked(probe::show));
            var failures = probe.verdict.get(30, TimeUnit.SECONDS);
            System.out.println(failures.isEmpty() ? "NUM-001 PASS (tested invariants)" : "NUM-001 RED findings=" + failures.size());
            exit = failures.isEmpty() ? 0 : 1;
        } catch (Throwable failure) {
            System.err.println("NUM-001 UNKNOWN: setup or FX scenario did not complete");
            failure.printStackTrace(System.err);
        } finally {
            Platform.exit();
        }
        System.exit(exit);
    }

    private void show() {
        Platform.setImplicitExit(false);
        Thread.currentThread().setUncaughtExceptionHandler((thread, failure) -> {
            failure.printStackTrace(System.err);
            check(false, "uncaught FX exception: " + failure);
        });
        String version = System.getProperty("javafx.version", "UNKNOWN");
        require(version.equals("25") || version.startsWith("25."), "setup: JavaFX 25 required");
        System.out.println("Java=" + Runtime.version() + " JavaFX=" + version);
        originalMoneyFormat = JInvMoneyField.g_moneyFormat;
        originalLocale = Locale.getDefault(Locale.Category.FORMAT);
        Locale.setDefault(Locale.Category.FORMAT, Locale.US);
        JInvMoneyField.initDefaultMoneyFormatter(2, '.', true);
        money = new Watched(new JInvMoneyField());
        decimal = new Watched(new JInvBigDecimalField());
        peer = new JInvBigDecimalField();
        sink = new Button("Move focus");
        var scene = new Scene(new VBox(8, money.field, decimal.field, peer, sink), 450, 190);
        scene.getStylesheets().add(Objects.requireNonNull(getClass().getResource("/css/general.css"),
                "setup: production stylesheet missing").toExternalForm());
        stage = new Stage();
        stage.setScene(scene);
        stage.setTitle("NUM-001 Legacy money and decimal fields");
        stage.focusedProperty().addListener((observable, before, focused) -> {
            if (focused && !started) afterPulse(() -> {
                if (stage.isFocused() && !started) {
                    started = true;
                    runCases();
                }
            });
        });
        stage.show();
        stage.requestFocus();
    }

    private void runCases() {
        for (var field : List.of(money.field, decimal.field, peer)) {
            require(field.getSkin() instanceof JInvTextFieldButtonSkin && field.getWidth() > 0
                            && field.getHeight() > 0 && stage.isShowing(),
                    "setup: visible Legacy fields and skins required");
        }
        directCases();
        fieldCases(money);
        fieldCases(decimal);
        checkSignEdit("-", "-12.35");
        checkSignEdit("+", "+12.36");
        step("money recover for focus", money, () -> money.field.setText("12345,67"));
        step("decimal recover for focus", decimal, () -> decimal.field.setText("12345,67"));
        checkFocus(money, () -> checkFocus(decimal, this::settingsCases));
    }

    private void checkSignEdit(String sign, String recovery) {
        BigDecimal previous = money.field.getValue();
        Counts before = money.counts();
        step("money sign-only edit [" + sign + "]", money, () -> replace(money.field, sign));
        check(sign.equals(money.field.getText()) && previous.equals(money.field.getValue())
                        && money.field.getState() == State.ERROR && money.valueEvents == before.value()
                        && money.textEvents == before.text() + 1,
                "money sign-only [" + sign + "] must preserve the amount and report ERROR: " + money.snapshot(before));
        int beforeRecovery = money.valueEvents;
        step("money signed recovery [" + recovery + "]", money, () -> replace(money.field, recovery));
        expectValue(money, recovery, State.VALUE);
        check(money.valueEvents == beforeRecovery + 1, "signed recovery must publish exactly one typed change");
    }

    private void directCases() {
        for (String text : List.of("1.005", "-1.005", "0", "12 345,67", "12 345.67",
                "123456789012345678901234567890.12345678901234567890")) {
            String canonical = text.replace(" ", "").replace(" ", "").replace(',', '.');
            BigDecimal parsed = parser.fromString(text);
            check(bd(canonical).equals(parsed), "direct parse lost precision for " + text + ": " + parsed);
            System.out.println("CONFIRMED direct [" + text + "] -> " + parsed + " display=[" + parser.toString(parsed) + "]");
        }
        for (String text : List.of("+1.25K", "-2T", "3M", "-1К", "+2Т", "3М", "12K34", "K")) {
            System.out.println("OBSERVED shorthand [" + text + "] -> " + parser.fromString(text));
        }
        for (String malformed : List.of("abc", "1.2,3", "+", "-", " + ", " - ",
                "\u00a0+\u00a0", "\u00a0-\u00a0", " \u00a0+ ", " -\u00a0 ")) {
            try {
                BigDecimal result = parser.fromString(malformed);
                check(false, "invalid numeric input [" + malformed + "] accepted as " + result);
            } catch (NumberFormatException expected) {
                System.out.println("CONFIRMED direct rejected [" + malformed + "]");
            }
        }
        check(bd("0").equals(parser.fromString("+0")) && bd("0").equals(parser.fromString("-0"))
                        && bd("1250.00").equals(parser.fromString("+1.25K"))
                        && bd("-2000").equals(parser.fromString("-2T"))
                        && bd("3000000").equals(parser.fromString("+3M")),
                "signed zero or signed shorthand conversion changed");
        BigDecimal amount = bd("-12345.67");
        roundTrip("default money", amount);
        check(parser.fromString("  ") == null && parser.toString(null) == null,
                "blank input or null output conversion");
    }

    private void fieldCases(Watched watched) {
        String name = watched.field.getClass().getSimpleName();
        expectValue(watched, null, State.NULL);
        step(name + " typed 1.005", watched, () -> watched.field.setValue(bd("1.005")));
        expectValue(watched, "1.01", State.VALUE);
        step(name + " edit -1.005", watched, () -> replace(watched.field, "-1.005"));
        expectValue(watched, "-1.01", State.VALUE);
        step(name + " edit excess digits", watched, () -> replace(watched.field, "12345678901234567890.12345"));
        expectValue(watched, "12345678901234567890.12", State.VALUE);
        step(name + " malformed edit", watched, () -> watched.field.setText("1.2,3"));
        expectValue(watched, "12345678901234567890.12", State.ERROR);
        step(name + " clear by typed null", watched, () -> watched.field.setValue(null));
        expectValue(watched, null, State.NULL);
        step(name + " recovery", watched, () -> replace(watched.field, "12,34"));
        expectValue(watched, "12.34", State.VALUE);
    }

    private void checkFocus(Watched watched, Runnable next) {
        var before = watched.counts();
        BigDecimal value = watched.field.getValue();
        watched.field.requestFocus();
        afterPulse(() -> {
            require(watched.field.isFocused(), "setup: numeric field did not receive focus");
            sink.requestFocus();
            afterPulse(() -> {
                require(sink.isFocused() && !watched.field.isFocused(), "setup: numeric field did not lose focus");
                check(value.equals(watched.field.getValue()) && watched.valueEvents == before.value(),
                        "focus changed typed amount: " + watched.snapshot(before));
                System.out.println("CONFIRMED focus " + watched.field.getClass().getSimpleName() + " " + watched.snapshot(before));
                next.run();
            });
        });
    }

    private void settingsCases() {
        var field = (JInvBigDecimalField) decimal.field;
        peer.setValue(bd("76543.21"));
        Object shared = read(JInvBigDecimalField.class, "formatter", field);
        check(shared == read(JInvBigDecimalField.class, "formatter", peer), "same decimal setting did not share formatter");
        step("decimal grouping and scale change", decimal, () -> {
            field.setShowGroups(true);
            field.setFractionDigits(3);
        });
        check(bd("12345.67").equals(field.getValue()) && bd("76543.21").equals(peer.getValue())
                        && shared == read(JInvBigDecimalField.class, "formatter", peer),
                "settings changed typed amount or peer formatter");
        System.out.println("CONFIRMED settings reference isolated="
                + (shared != read(JInvBigDecimalField.class, "formatter", field))
                + " peerText=[" + peer.getText() + "] nextFormat=[" + field.getConverter().toString(field.getValue()) + "]");
        step("decimal typed 1.245 at scale 3", decimal, () -> field.setValue(bd("1.245")));
        step("decimal reduce scale to 2", decimal, () -> field.setFractionDigits(2));
        check(bd("1.245").equals(field.getValue()), "setting alone changed decimal model value");
        System.out.println("OBSERVED next decimal format at scale 2=[" + field.getConverter().toString(field.getValue()) + "]");

        BigDecimal cached = parser.fromString("71.005");
        step("money global scale=3, comma, grouping", money,
                () -> JInvMoneyField.initDefaultMoneyFormatter(3, ',', true));
        check(bd("12345.67").equals(money.field.getValue()), "global settings changed existing money model");
        System.out.println("OBSERVED cached raw parse reused across money settings=" + (cached == parser.fromString("71.005")));
        roundTrip("comma money at scale 3", bd("12345.678"));
        step("money typed under new scale", money, () -> money.field.setValue(bd("71.0054")));
        expectValue(money, "71.005", State.VALUE);

        checkGrouping('.', false, "12345.67");
        checkGrouping('.', true, "12 345.67");
        checkGrouping(',', false, "12345,67");
        checkGrouping(',', true, "12 345,67");

        Locale locale = Locale.forLanguageTag("ar-EG");
        Locale.setDefault(Locale.Category.FORMAT, locale);
        JInvMoneyField.initDefaultMoneyFormatter(2, '.', true);
        roundTrip("money under " + locale.toLanguageTag(), bd("12345.67"));
        var localized = new JInvBigDecimalField();
        localized.setFractionDigits(7);
        localized.setShowGroups(true);
        localized.setValue(bd("12345.6789012"));
        System.out.println("OBSERVED localized decimal locale=" + locale + " text=[" + localized.getText()
                + "] value=" + localized.getValue() + " parsed=" + localized.getConverter().fromString(localized.getText()));
        check(localized.getValue().equals(localized.getConverter().fromString(localized.getText())),
                "localized decimal round trip lost value");
        Locale.setDefault(Locale.Category.FORMAT, Locale.US);
        JInvMoneyField.initDefaultMoneyFormatter(2, '.', true);
        memoryCases();
        restoreSettings();
        stage.close();
        verdict.complete(List.copyOf(failures));
    }

    private void checkGrouping(char separator, boolean grouping, String expected) {
        String label = "US money, decimal=" + separator + ", grouping=" + grouping;
        BigDecimal previous = money.field.getValue();
        String previousText = money.field.getText();
        Counts before = money.counts();
        JInvMoneyField.initDefaultMoneyFormatter(2, separator, grouping);
        check(previous.equals(money.field.getValue()) && Objects.equals(previousText, money.field.getText())
                        && before.equals(money.counts()),
                label + " changed the existing field: " + money.snapshot(before));
        System.out.println("OBSERVED " + label + " existing field " + money.snapshot(before));
        roundTrip(label, bd("12345.67"), expected);
        roundTrip(label, bd("-12345.67"), "-" + expected);
    }

    private void roundTrip(String label, BigDecimal value) {
        roundTrip(label, value, null);
    }

    private void roundTrip(String label, BigDecimal value, String expectedDisplay) {
        String rendered = parser.toString(value);
        check(expectedDisplay == null || expectedDisplay.equals(rendered),
                label + " expected display [" + expectedDisplay + "], got [" + rendered + "]");
        try {
            BigDecimal parsed = parser.fromString(rendered);
            System.out.println("OBSERVED " + label + " format=[" + rendered + "] parsed=" + parsed);
            check(value.equals(parsed), label + " changed " + value + " to " + parsed);
        } catch (NumberFormatException failure) {
            check(false, label + " cannot parse its own format [" + rendered + "]: " + failure);
        }
    }

    private void memoryCases() {
        new JInvBigDecimalField(bd("42.00"));
        new JInvMoneyField("42");
        var decimals = new ArrayList<JInvBigDecimalField>(BATCH);
        var monies = new ArrayList<JInvMoneyField>(BATCH);
        int lazyConverters = 0;
        int lazyFormats = 0;
        for (int i = 0; i < BATCH; i++) {
            var decimalField = new JInvBigDecimalField();
            if (read(JInvBigDecimalField.class, "g_decConverter", decimalField) == null) lazyConverters++;
            if (read(JInvBigDecimalField.class, "formatter", decimalField) == null) lazyFormats++;
            decimalField.setValue(BigDecimal.valueOf(i + 1));
            decimals.add(decimalField);
            var moneyField = new JInvMoneyField();
            moneyField.setValue(BigDecimal.valueOf(i + 1));
            monies.add(moneyField);
        }
        System.out.println("MEMORY live batch per type=" + BATCH + " decimal lazy converters/formats="
                + lazyConverters + "/" + lazyFormats
                + " decimal converter/formatter identities="
                + identities(decimals, JInvBigDecimalField.class, "g_decConverter") + "/"
                + identities(decimals, JInvBigDecimalField.class, "formatter")
                + " decimal/money typed property identities="
                + identities(decimals, AbstractBigDecimalField.class, "valueProperty") + "/"
                + identities(monies, AbstractBigDecimalField.class, "valueProperty")
                + " decimal/money inherited converter identities="
                + identities(decimals, JInvNumberField.class, "stringConverter") + "/"
                + identities(monies, JInvNumberField.class, "stringConverter")
                + " actual money converter/formatter identities=1/1 (static)");
        var refs = (List<?>) read(JInvBigDecimalField.class, "g_decimalFormatList", null);
        long live = refs.stream().filter(reference -> ((Reference<?>) reference).get() != null).count();
        var cache = (Map<?, ?>) read(MoneyStringConverter.class, "g_valueCacheMap", null);
        String oldest = "314159.271828";
        parser.fromString(oldest);
        for (int i = 0; i < 300; i++) parser.fromString(Integer.toString(80000 + i));
        System.out.println("MEMORY cache weak slots/live=" + refs.size() + "/" + live
                + " money entries=" + cache.size() + " oldestRetained=" + cache.containsKey(oldest)
                + " retained bytes UNKNOWN");
        check(cache.size() == 250 && !cache.containsKey(oldest), "money cache did not evict old entry at expected bound");
        Reference.reachabilityFence(decimals);
        Reference.reachabilityFence(monies);
    }

    private static int identities(List<?> objects, Class<?> owner, String name) {
        var values = Collections.newSetFromMap(new IdentityHashMap<>());
        for (Object object : objects) values.add(read(owner, name, object));
        return values.size();
    }

    private static Object read(Class<?> owner, String name, Object receiver) {
        try {
            Field field = owner.getDeclaredField(name);
            field.setAccessible(true);
            return field.get(receiver);
        } catch (ReflectiveOperationException failure) {
            throw new IllegalStateException("diagnostic member unavailable: " + owner.getSimpleName() + "." + name, failure);
        }
    }

    private void expectValue(Watched watched, String value, State state) {
        check(Objects.equals(value == null ? null : bd(value), watched.field.getValue()) && state == watched.field.getState(),
                watched.field.getClass().getSimpleName() + " expected " + value + "/" + state
                        + ", got " + watched.field.getValue() + "/" + watched.field.getState());
    }

    private void step(String label, Watched watched, Runnable action) {
        var before = watched.counts();
        String text = watched.field.getText();
        action.run();
        System.out.println("OBSERVED " + label + " before=[" + text + "] " + watched.snapshot(before));
    }

    private static BigDecimal bd(String text) { return new BigDecimal(text); }

    private static void replace(AbstractBigDecimalField field, String text) {
        field.replaceText(0, field.getLength(), text);
    }

    private void check(boolean condition, String diagnostic) {
        if (!condition) {
            failures.add(diagnostic);
            System.out.println("FINDING " + diagnostic);
        }
    }

    private void restoreSettings() {
        if (originalLocale == null) return;
        Locale.setDefault(Locale.Category.FORMAT, originalLocale);
        JInvMoneyField.g_moneyFormat = originalMoneyFormat;
    }

    private void afterPulse(Runnable action) {
        new AnimationTimer() {
            private boolean first = true;

            @Override public void handle(long now) {
                if (first) {
                    first = false;
                    return;
                }
                stop();
                checked(action);
            }
        }.start();
    }

    private void checked(Runnable action) {
        if (verdict.isDone()) return;
        try {
            action.run();
        } catch (Throwable failure) {
            restoreSettings();
            verdict.completeExceptionally(failure);
        }
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
