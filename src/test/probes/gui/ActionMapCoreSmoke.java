import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.UUID;
import javafx.application.Platform;
import ru.inversion.fore.form.action.ActionMap;
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
        child.put(1, a);
        child.put(1L, b);
        child.put(BigInteger.ONE, b);
        child.put(new BigDecimal("1.0"), a);
        require(child.get(1) == a && child.get(1L) == b && child.get(BigInteger.ONE) == b
                && child.get(new BigDecimal("1.00")) == a, "numeric type boundaries and compareTo identity");
        require(child.size() == 8 && parent.size() == 1, "local counts");
        rejected(() -> parent.setParent(child));
        rejected(() -> child.setParent(child));
        rejected(() -> child.put(new UUID(0, 0), a));
        require(child.getParent() == parent && parent.getParent() == null && child.size() == 8,
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
        Platform.exit();
        System.out.println("ACT-012 PASS");
    }
}
