package ru.inversion.fore.form.action;

import javafx.event.ActionEvent;

import java.util.Objects;
import java.util.function.Consumer;

/** Общие настройки не разделяют изменяемое состояние операций. */
public final class ForeActions {
    private ForeActions() {
    }

    public static ForeAction create(StandardAction type, Consumer<ActionEvent> handler) {
        return new ForeAction(type, Objects.requireNonNull(type, "type").text(), handler);
    }

    public static ForeAction custom(String text, Consumer<ActionEvent> handler) {
        return new ForeAction(null, text, handler);
    }
}
