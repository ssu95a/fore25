package ru.inversion.fx.form.controls;

import ru.inversion.fx.app.sec.SecurityStrategyEnum;

/** Применение прежней стратегии к представлению, независимо от допуска вызова. */
final class ActionSecurityPresentation {
    enum Effect {
        NONE, DISABLE, HIDE
    }

    private ActionSecurityPresentation() {
    }

    static Effect effect(String viewId, boolean authorized, SecurityStrategyEnum strategy) {
        if (viewId == null || authorized) {
            return Effect.NONE;
        }
        return switch (strategy) {
            case DISABLE -> Effect.DISABLE;
            case HIDE -> Effect.HIDE;
            default -> Effect.NONE;
        };
    }
}
