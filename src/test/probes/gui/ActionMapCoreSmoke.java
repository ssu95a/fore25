import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.UUID;
import javafx.application.Platform;
import ru.inversion.fore.form.action.ActionMap;
import ru.inversion.fore.form.action.ForeAction;
import ru.inversion.fore.form.action.ForeActions;
import ru.inversion.fore.form.action.StandardAction;

public final class ActionMapCoreSmoke {
    private enum Custom {
        CREATE {
            @Override
            public String toString() {
                return "custom";
            }
        },
        DELETE
    }

    private static void require(boolean condition, String description) {
        if (!condition) {
            throw new AssertionError(description);
        }
    }

    private static void rejected(Runnable attempt) {
        try {
            attempt.run();
            throw new AssertionError("expected IllegalArgumentException");
        } catch (IllegalArgumentException expected) {
            // Проверка отказа без изменения реестра.
        }
    }

    private static final class TextKey {
        private final String text;
        private int calls;

        private TextKey(String text) {
            this.text = text;
        }

        @Override
        public String toString() {
            calls++;
            return text;
        }
    }

    public static void main(String[] args) {
        var parent = new ActionMap();
        var child = new ActionMap();
        var unrelated = new ActionMap();
        var a = ForeActions.custom("parent", event -> {});
        var b = ForeActions.custom("child", event -> {});
        b.setDisabled(true);
        require(child.size() == 0 && child.get(null) == null && child.getLocal(null) == null, "empty map");
        parent.put(StandardAction.CREATE, a);
        child.setParent(parent);
        require(child.get(StandardAction.CREATE) == a && child.getLocal(StandardAction.CREATE) == null,
                "parent fallback");
        child.put(StandardAction.CREATE, b);
        require(child.get(StandardAction.CREATE) == b, "disabled local action shadows ancestor");
        child.put(Custom.CREATE, a);
        child.put(Custom.DELETE, b);
        require(child.get(Custom.CREATE) == a && child.get(Custom.DELETE) == b, "declaring enum class");
        child.put("create", a);
        require(child.put("CREATE", b) == a && child.get("Create") == b, "case-insensitive identity");
        require(child.get("custom") == null, "enum toString does not define its key");
        exerciseNormalization(parent, child, a, b);
        require(child.size() == 4 && parent.size() == 1, "local counts");
        rejected(() -> parent.setParent(child));
        rejected(() -> child.setParent(child));
        require(child.getParent() == parent && parent.getParent() == null && child.size() == 4,
                "rejection atomicity");
        require(child.put("cREaTe", null) == b && child.get("create") == null, "put null removes local");
        require(child.remove(StandardAction.CREATE) == b && child.get(StandardAction.CREATE) == a,
                "remove restores fallback");
        child.clear();
        require(child.size() == 0 && child.get(StandardAction.CREATE) == a, "clear is local");
        child.setParent(unrelated);
        require(child.get(StandardAction.CREATE) == null && parent.get(StandardAction.CREATE) == a,
                "replace parent");
        child.setParent(null);
        child.put("local", a);
        child.setParent(parent);
        child.retire();
        require(child.getParent() == null && child.size() == 0 && child.get("local") == null
                && parent.get(StandardAction.CREATE) == a, "retirement remains local");
        try {
            child.put(new TextKey("retired"), a);
            throw new AssertionError("retired registry accepts keys");
        } catch (IllegalStateException expected) {
            require(child.size() == 0, "retirement prevents repopulation");
        }
        Platform.exit();
        System.out.println("ACT-012 PASS");
    }

    private static void exerciseNormalization(ActionMap parent, ActionMap child, ForeAction a, ForeAction b) {
        parent.put("1", a);
        require(child.get(1L) == a, "Long aliases inherited String");
        child.put(1, a);
        child.put(1L, b);
        require(child.get("1") == b && child.get(BigInteger.ONE) == b && child.get(1) == a,
                "integer and normalized string domains");
        require(child.put(new BigDecimal("1.0"), a) == null && child.get(new BigDecimal("1.00")) == null,
                "decimal text retains scale");
        var uuid = new UUID(0, 10);
        child.put(uuid, a);
        require(child.get(uuid.toString().toUpperCase(java.util.Locale.ROOT)) == a, "UUID normalizes as text");

        var key = new TextKey("INHERITED");
        parent.put("inherited", a);
        require(child.get(key) == a && key.calls == 1, "normalize once across parents");
        require(child.getLocal(key) == null && key.calls == 2, "normalize once for local miss");
        require(child.put(key, b) == null && key.calls == 3, "normalize once on put");
        require(child.getLocal(key) == b && key.calls == 4, "normalize once for local hit");
        require(child.put(key, null) == b && key.calls == 5, "normalize once for put-null");
        child.put(key, b);
        require(child.remove(key) == b && key.calls == 7, "normalize once for remove");

        var nullText = new TextKey(null);
        int size = child.size();
        try {
            child.put(nullText, a);
            throw new AssertionError("null toString accepted");
        } catch (NullPointerException expected) {
            require(nullText.calls == 1 && child.size() == size, "null toString is atomic");
        }
        var failure = new IllegalArgumentException("toString failed");
        Object broken = new Object() {
            @Override public String toString() { throw failure; }
        };
        try {
            child.put(broken, null);
            throw new AssertionError("throwing toString accepted");
        } catch (IllegalArgumentException expected) {
            require(expected == failure && child.size() == size && child.get("1") == b,
                    "original failure propagated without mutation");
        }
        require(child.remove(null) == null && child.remove((Object) null) == null, "null removal");
        require(child.remove(1L) == b && child.get("1") == a && child.remove(1) == a,
                "remove normalized ID restores parent");
        require(child.remove("1.0") == a && child.remove(uuid) == a, "remove arbitrary keys via aliases");
        parent.remove("1");
        parent.remove("inherited");
    }
}
