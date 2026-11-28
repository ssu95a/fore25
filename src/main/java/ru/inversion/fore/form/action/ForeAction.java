package ru.inversion.fore.form.action;

import javafx.event.ActionEvent;
import javafx.scene.input.KeyCombination;
import org.controlsfx.control.action.Action;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/** Состояние одной операции и её представление для ControlsFX. */
public final class ForeAction extends Action {
    private final StandardAction standardType;
    private IconSpec icon;
    private List<KeyCombination> alternatives = List.of();

    ForeAction(StandardAction type, String text, Consumer<ActionEvent> handler) {
        super(text, adapt(handler));
        standardType = type;
        if (type != null) {
            setLongText(type.tooltip());
            setIcon(type.icon());
            setHotkeys(type.hotkeys());
        }
    }

    /** Заменяет функциональный обработчик, не меняя состояние представления. */
    public void setHandler(Consumer<ActionEvent> handler) {
        setEventHandler(adapt(handler));
    }

    private static Consumer<ActionEvent> adapt(Consumer<ActionEvent> handler) {
        Objects.requireNonNull(handler, "handler");
        return event -> ActionInvocation.invoke(handler, Consumer::accept, event);
    }

    /** Возвращает стандартный тип или null для пользовательской операции. */
    public StandardAction standardType() {
        return standardType;
    }

    public IconSpec icon() {
        return icon;
    }

    /** null убирает графику операции. */
    public void setIcon(IconSpec icon) {
        setGraphic(icon == null ? null : icon.newGraphic());
        this.icon = icon;
    }

    /** Основная клавиша берётся из accelerator, остальные хранятся отдельно. */
    public List<KeyCombination> hotkeys() {
        KeyCombination primary = getAccelerator();
        if (primary == null) {
            return alternatives;
        }
        if (alternatives.isEmpty()) {
            return List.of(primary);
        }
        var keys = new ArrayList<KeyCombination>(alternatives.size() + 1);
        keys.add(primary);
        keys.addAll(alternatives);
        return List.copyOf(keys);
    }

    /** Первая клавиша задаёт accelerator. Прямая запись accelerator меняет только первую клавишу. */
    public void setHotkeys(List<? extends KeyCombination> hotkeys) {
        List<KeyCombination> keys = List.copyOf(hotkeys);
        List<KeyCombination> previous = alternatives;
        alternatives = keys.size() < 2 ? List.of() : List.copyOf(keys.subList(1, keys.size()));
        try {
            setAccelerator(keys.isEmpty() ? null : keys.getFirst());
        } catch (RuntimeException | Error failure) {
            alternatives = previous;
            throw failure;
        }
    }
}
