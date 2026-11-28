package ru.inversion.fore.form.action;

import javafx.event.ActionEvent;

import java.util.function.BiConsumer;

/** Вызов операции с исходным событием; допуск и обработка ошибок остаются у вызывающей стороны. */
public final class ActionInvocation {
    private ActionInvocation() {
    }

    public static <H> void invoke(H handler, BiConsumer<H, ActionEvent> operation, ActionEvent event) {
        if (handler != null) {
            operation.accept(handler, event);
        }
    }
}
