package ru.inversion.fore.form.action;

import javafx.event.ActionEvent;

import java.util.function.BiConsumer;
import java.util.function.Predicate;

/** Вызов операции с исходным событием; допуск и обработка ошибок остаются у вызывающей стороны. */
public final class ActionInvocation {
    public enum Admission {
        DISABLED, DENIED, ALLOWED
    }

    private ActionInvocation() {
    }

    public static <H> void invoke(H handler, BiConsumer<H, ActionEvent> operation, ActionEvent event) {
        if (handler != null) {
            operation.accept(handler, event);
        }
    }

    /** Отключённая операция не запрашивает проверку прав. */
    public static <T> Admission admit(T action, Predicate<T> enabled, Predicate<T> authorized) {
        if (!enabled.test(action)) {
            return Admission.DISABLED;
        }
        return authorized.test(action) ? Admission.ALLOWED : Admission.DENIED;
    }
}
