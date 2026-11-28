package ru.inversion.fx.form;

import java.util.ArrayList;
import ru.inversion.fx.form.action.JInvAction;

/** Состояние автоматического WAIT одной формы. Вызовы выполняются в FX-потоке. */
public final class FormWait {
    private static final ScopedValue<Registration> HOOK = ScopedValue.newInstance();

    private final IFormStateListener owner;
    private ArrayList<Registration> active;
    private boolean automatic;
    private boolean retired;

    public FormWait(IFormStateListener owner) {
        this.owner = java.util.Objects.requireNonNull(owner);
    }

    /** Сохраняет виртуальный вызов старых хуков и связывает их с данной операцией. */
    public static Runnable acquire(IFormStateListener owner, JInvAction action, Runnable retireProgress) {
        var registration = new Registration(owner, action, retireProgress);
        try {
            ScopedValue.where(HOOK, registration).run(() -> owner.addWaitAction(action));
        } catch (RuntimeException | Error failure) {
            try {
                registration.finish();
            } catch (RuntimeException | Error cleanup) {
                if (cleanup != failure) {
                    failure.addSuppressed(cleanup);
                }
            }
            throw failure;
        }
        return registration::finish;
    }

    void add(JInvAction action) {
        if (action == null) {
            return;
        }
        var registration = current(action);
        if (retired) {
            if (registration != null) {
                registration.retire();
            }
            return;
        }
        if (registration == null) {
            if (legacy(action) != null) {
                return;
            }
            registration = new Registration(owner, action, null);
        }
        if (registration.form != null) {
            return;
        }
        if (active == null) {
            active = new ArrayList<>();
        }
        if (active.isEmpty()) {
            automatic = owner.getState() == StateEnum.ACTIVE;
        }
        active.add(registration);
        registration.form = this;
        var captionSource = action.getStateMessageProperty();
        registration.caption = new ProgressDelivery(owner.stateTextProperty(), captionSource,
                () -> action.getStateMessageProperty() == captionSource);
        registration.caption.attach();
        if (registration.form == this && automatic && owner.getState() == StateEnum.ACTIVE) {
            owner.setState(StateEnum.WAIT);
        }
    }

    void remove(JInvAction action) {
        var registration = current(action);
        if (registration == null) {
            registration = legacy(action);
        }
        if (registration != null) {
            release(registration);
        }
    }

    private Registration current(JInvAction action) {
        if (HOOK.isBound()) {
            var registration = HOOK.get();
            if (registration.owner == owner && registration.action == action) {
                return registration;
            }
        }
        return null;
    }

    private Registration legacy(JInvAction action) {
        if (active != null) {
            for (var registration : active) {
                if (registration.retireProgress == null && registration.action == action) {
                    return registration;
                }
            }
        }
        return null;
    }

    private void release(Registration registration) {
        if (registration.form != this) {
            return;
        }
        active.remove(registration);
        registration.form = null;
        try {
            registration.retire();
        } finally {
            if (active != null && active.isEmpty() && automatic) {
                automatic = false;
                if (owner.getState() == StateEnum.WAIT) {
                    owner.setState(StateEnum.ACTIVE);
                    if (owner.getState() == StateEnum.ACTIVE) {
                        ProgressDelivery.clearIdle(owner.stateTextProperty());
                    }
                }
            }
        }
    }

    /** Завершает текущую сессию WAIT, включая доставку ее сообщений. */
    public void reset() {
        automatic = false;
        if (active != null) {
            var previous = active;
            active = null;
            for (var registration : previous) {
                registration.form = null;
            }
            RuntimeException failure = null;
            for (var registration : previous) {
                try {
                    registration.retire();
                } catch (RuntimeException error) {
                    if (failure == null) {
                        failure = error;
                    } else if (failure != error) {
                        failure.addSuppressed(error);
                    }
                }
            }
            if (failure != null) {
                throw failure;
            }
        }
    }

    void retire() {
        retired = true;
        reset();
    }

    private static final class Registration {
        private final IFormStateListener owner;
        private final JInvAction action;
        private final Runnable retireProgress;
        private FormWait form;
        private ProgressDelivery caption;
        private boolean finished;

        private Registration(IFormStateListener owner, JInvAction action, Runnable retireProgress) {
            this.owner = owner;
            this.action = action;
            this.retireProgress = retireProgress;
        }

        private void retire() {
            try {
                if (caption != null) {
                    caption.close();
                }
            } finally {
                if (retireProgress != null) {
                    retireProgress.run();
                }
            }
        }

        private void finish() {
            if (finished) {
                return;
            }
            finished = true;
            try {
                ScopedValue.where(HOOK, this).run(() -> owner.removeWaitAction(action));
            } finally {
                if (form != null) {
                    form.release(this);
                } else {
                    retire();
                }
            }
        }
    }
}
