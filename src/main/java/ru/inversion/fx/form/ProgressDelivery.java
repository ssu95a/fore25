package ru.inversion.fx.form;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.function.BooleanSupplier;
import javafx.application.Platform;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableStringValue;

/** Публикация сообщений активных операций в свойства представления. Только FX-поток. */
public final class ProgressDelivery implements AutoCloseable {
    private static final IdentityHashMap<StringProperty, ArrayList<ProgressDelivery>> ACTIVE =
            new IdentityHashMap<>();

    private final StringProperty target;
    private final ObservableStringValue source;
    private final BooleanSupplier current;
    private final ChangeListener<String> change = (observable, oldValue, value) -> {
        if (Platform.isFxApplicationThread()) {
            publish();
        } else {
            Platform.runLater(this::publish);
        }
    };
    private boolean attached;

    public ProgressDelivery(StringProperty target, ObservableStringValue source, BooleanSupplier current) {
        this.target = target;
        this.source = source;
        this.current = current;
    }

    public void attach() {
        if (attached || target == null || source == null || target == source) {
            return;
        }
        ACTIVE.computeIfAbsent(target, ignored -> new ArrayList<>()).add(this);
        attached = true;
        try {
            source.addListener(change);
            publish();
        } catch (RuntimeException | Error failure) {
            try {
                close();
            } catch (RuntimeException | Error cleanup) {
                if (cleanup != failure) {
                    failure.addSuppressed(cleanup);
                }
            }
            throw failure;
        }
    }

    private void publish() {
        if (ownsTarget() && current.getAsBoolean() && ownsTarget() && !target.isBound() && ownsTarget()) {
            String message = source.get();
            if (ownsTarget() && !target.isBound() && ownsTarget()) {
                target.set(message);
            }
        }
    }

    private boolean ownsTarget() {
        return attached && ACTIVE.get(target).getLast() == this;
    }

    static void clearIdle(StringProperty target) {
        if (target != null && !target.isBound() && !ACTIVE.containsKey(target)) {
            target.set("");
        }
    }

    @Override
    public void close() {
        if (!attached) {
            return;
        }
        attached = false;
        var deliveries = ACTIVE.get(target);
        boolean latest = deliveries.getLast() == this;
        deliveries.remove(this);
        if (deliveries.isEmpty()) {
            ACTIVE.remove(target);
        }
        var previous = latest && !deliveries.isEmpty() ? deliveries.getLast() : null;
        try {
            source.removeListener(change);
        } catch (RuntimeException | Error failure) {
            if (previous != null) {
                try {
                    previous.publish();
                } catch (RuntimeException | Error cleanup) {
                    if (cleanup != failure) {
                        failure.addSuppressed(cleanup);
                    }
                }
            }
            throw failure;
        }
        if (previous != null) {
            previous.publish();
        }
    }
}
