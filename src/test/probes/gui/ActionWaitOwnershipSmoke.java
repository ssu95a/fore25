import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.FutureTask;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.concurrent.Worker;
import javafx.event.ActionEvent;
import ru.inversion.fx.app.ThreadPoolManager;
import ru.inversion.fx.form.AbstractBaseController;
import ru.inversion.fx.form.FormWait;
import ru.inversion.fx.form.IFormStateListener;
import ru.inversion.fx.form.ProgressDelivery;
import ru.inversion.fx.form.StateEnum;
import ru.inversion.fx.form.action.JInvAction;
import ru.inversion.fx.form.action.TaskHandler;

public final class ActionWaitOwnershipSmoke {
    private static class PassiveForm implements IFormStateListener {
        final ObjectProperty<StateEnum> state = new SimpleObjectProperty<>(StateEnum.ACTIVE);
        final StringProperty caption = new SimpleStringProperty();

        @Override
        public ObjectProperty<StateEnum> stateProperty() {
            return state;
        }

        @Override
        public StringProperty stateTextProperty() {
            return caption;
        }
    }

    private static final class ManagedForm extends PassiveForm {
        private final FormWait wait = new FormWait(this);
        int admissions;
        int releases;

        @Override
        public FormWait getFormWait() {
            return wait;
        }

        @Override
        public void addWaitAction(JInvAction action) {
            admissions++;
            super.addWaitAction(action);
        }

        @Override
        public void removeWaitAction(JInvAction action) {
            releases++;
            super.removeWaitAction(action);
        }
    }

    private static final class Run {
        final ActionEvent event = new ActionEvent();
        final CountDownLatch started = new CountDownLatch(1);
        final CountDownLatch release = new CountDownLatch(1);
        final CountDownLatch terminal = new CountDownLatch(1);
        volatile JInvAction.JInvTask task;

        void execute(ActionEvent actual, JInvAction.JInvTask invocation, String text) {
            require(actual == event && !Platform.isFxApplicationThread(), "worker and event identity");
            task = invocation;
            Platform.runLater(() -> invocation.stateProperty().addListener((source, previous, state) -> {
                if (state == Worker.State.SUCCEEDED || state == Worker.State.FAILED
                        || state == Worker.State.CANCELLED) {
                    terminal.countDown();
                }
            }));
            invocation.setMessage(text);
            started.countDown();
            await(release, "worker release");
        }
    }

    private static void require(boolean value, String description) {
        if (!value) {
            throw new AssertionError(description);
        }
    }

    private static void await(CountDownLatch latch, String description) {
        try {
            require(latch.await(12, TimeUnit.SECONDS), description);
        } catch (InterruptedException failure) {
            Thread.currentThread().interrupt();
            throw new AssertionError(description, failure);
        }
    }

    private static <T> T fx(Callable<T> action) throws Exception {
        var task = new FutureTask<>(action);
        Platform.runLater(task);
        return task.get(15, TimeUnit.SECONDS);
    }

    private static JInvAction command(IFormStateListener form, StringProperty target, TaskHandler body) {
        var action = new JInvAction();
        action.setParallel(true);
        action.setFormStateListener(form);
        action.setMessageProperty(target);
        action.setHandler(body);
        return action;
    }

    private static void overlap() throws Exception {
        var first = new Run();
        var second = new Run();
        var third = new Run();
        var form = new ManagedForm();
        var text = new SimpleStringProperty();
        var exception = new AtomicReference<Throwable>();
        var action = command(form, text, (event, task) -> {
            var invocation = (JInvAction.JInvTask) task;
            if (event == first.event) {
                first.execute(event, invocation, "first");
            } else if (event == second.event) {
                second.execute(event, invocation, "second");
            } else {
                third.execute(event, invocation, "third");
            }
        });
        action.setExceptionHandler(exception::set);
        try {
            fx(() -> {
                action.handle(first.event);
                require(form.getState() == StateEnum.WAIT && !text.isBound(), "first admission");
                return null;
            });
            await(first.started, "first began");
            fx(() -> {
                action.handle(second.event);
                return null;
            });
            await(second.started, "second began");
            fx(() -> {
                require("second".equals(text.get()), "last admitted source owns progress");
                return null;
            });
            first.release.countDown();
            await(first.terminal, "first completion");
            fx(() -> {
                require(form.getState() == StateEnum.WAIT && "second".equals(text.get()),
                        "first completion leaves second WAIT/progress");
                form.setState(StateEnum.ACTIVE);
                action.handle(third.event);
                return null;
            });
            await(third.started, "third began");
            var callerSource = new SimpleStringProperty("foreign");
            fx(() -> {
                require("third".equals(text.get()), "new generation has its own progress");
                text.bind(callerSource);
                return null;
            });
            second.release.countDown();
            await(second.terminal, "second completion");
            fx(() -> {
                require(form.getState() == StateEnum.WAIT && text.isBound(), "stale generation cannot release WAIT");
                require(third.task.cancel(false), "cancel accepted");
                return null;
            });
            await(third.terminal, "third cancellation");
            fx(() -> {
                callerSource.set("replacement remains live");
                require(text.isBound() && "replacement remains live".equals(text.get()), "caller binding survives");
                require(form.getState() == StateEnum.ACTIVE && third.release.getCount() == 1,
                        "cancel releases GUI while worker still held");
                require(form.admissions == 3 && form.releases == 3 && exception.get() == null,
                        "virtual hooks once per admitted invocation");
                return null;
            });
        } finally {
            first.release.countDown();
            second.release.countDown();
            third.release.countDown();
        }
    }

    private static void contracts() throws Exception {
        var nextCalled = new CountDownLatch(1);
        var failurePassed = new CountDownLatch(1);
        var form = new ManagedForm();
        var original = new IllegalStateException("offline worker failure");
        var failure = new AtomicReference<Throwable>();
        fx(() -> {
            var passive = new PassiveForm();
            var action = new JInvAction();
            passive.setState(StateEnum.WAIT);
            passive.addWaitAction(action);
            passive.removeWaitAction(action);
            require(passive.getState() == StateEnum.WAIT, "passive manual WAIT");
            form.setState(StateEnum.WAIT);
            form.addWaitAction(action);
            form.removeWaitAction(action);
            require(form.getState() == StateEnum.WAIT, "managed manual WAIT");
            form.setState(StateEnum.ACTIVE);
            var controller = new AbstractBaseController<>();
            controller.addWaitAction(action);
            require(controller.getState() == StateEnum.WAIT, "controller opts in");
            controller.removeWaitAction(action);
            require(controller.getState() == StateEnum.ACTIVE, "controller releases WAIT");

            var next = new JInvAction();
            action.setNextAction(next, true);
            action.setHandlerWithNextAction((event, target) -> {
                require(Platform.isFxApplicationThread() && target == next && event == null, "legacy next callback");
                nextCalled.countDown();
            });
            action.handle(null);
            require(nextCalled.getCount() == 1, "forced next is deferred");

            var failing = command(form, new SimpleStringProperty(), (event, task) -> { throw original; });
            failing.setExceptionHandler(error -> {
                failure.set(error);
                failurePassed.countDown();
            });
            failing.handle(null);
            return null;
        });
        await(nextCalled, "deferred next");
        await(failurePassed, "failure delivered");
        fx(() -> {
            require(failure.get() == original && form.getState() == StateEnum.ACTIVE, "failure identity and cleanup");
            return null;
        });
    }

    private static void rejection() throws Exception {
        ThreadPoolManager.getInstance().shutdown();
        fx(() -> {
            var form = new ManagedForm();
            var target = new SimpleStringProperty();
            var rejected = new AtomicReference<Throwable>();
            var command = command(form, target, (event, task) -> {
                throw new AssertionError("rejected action ran");
            });
            command.setExceptionHandler(rejected::set);
            command.handle(null);
            require(rejected.get() instanceof RejectedExecutionException, "original rejection delivered");
            require(form.getState() == StateEnum.ACTIVE && !target.isBound(), "rejection releases WAIT and progress");
            return null;
        });
    }

    private static void exceptionalOwnership() throws Exception {
        fx(() -> {
            var failures = new java.util.ArrayList<String>();
            var form = new AbstractBaseController<>();
            var original = new IllegalStateException("caption getter failed");
            var command = new JInvAction() {
                private boolean first = true;

                @Override
                public StringProperty getStateMessageProperty() {
                    if (first) {
                        first = false;
                        throw original;
                    }
                    return super.getStateMessageProperty();
                }
            };
            try {
                form.addWaitAction(command);
                throw new AssertionError("caption failure was swallowed");
            } catch (IllegalStateException error) {
                require(error == original, "caption failure identity");
            }
            command.getStateMessageProperty().set("recovered");
            form.addWaitAction(command);
            if (form.getState() != StateEnum.WAIT || !"recovered".equals(form.getStateText())) {
                failures.add("legacy WAIT admission cannot recover");
            }
            form.removeWaitAction(command);

            var target = new SimpleStringProperty();
            var first = new ProgressDelivery(target, new SimpleStringProperty("first"), () -> true);
            first.attach();
            var rejected = new ProgressDelivery(target, new SimpleStringProperty("rejected"), () -> {
                throw original;
            });
            try {
                rejected.attach();
                throw new AssertionError("progress failure was swallowed");
            } catch (IllegalStateException error) {
                require(error == original, "progress failure identity");
            }
            var replacement = new ProgressDelivery(target, new SimpleStringProperty("replacement"), () -> true);
            replacement.attach();
            try {
                replacement.close();
                if (!"first".equals(target.get())) {
                    failures.add("failed attach shadows previous source");
                }
            } catch (IllegalStateException error) {
                require(error == original, "restoration failure identity");
                failures.add("failed attach shadows previous source");
            }
            rejected.close();
            first.close();

            var newer = new ProgressDelivery(target, new SimpleStringProperty("newer"), () -> true);
            try (var older = new ProgressDelivery(target, new SimpleStringProperty("older"), () -> {
                newer.attach();
                return true;
            })) {
                older.attach();
                if (!"newer".equals(target.get())) {
                    failures.add("reentrant current predicate permits stale write");
                }
            } finally {
                newer.close();
            }

            var releases = new java.util.concurrent.atomic.AtomicInteger();
            Runnable finish = FormWait.acquire(form, new JInvAction(), releases::incrementAndGet);
            finish.run();
            finish.run();
            if (releases.get() != 1 || form.getState() != StateEnum.ACTIVE) {
                failures.add("WAIT retirement callback runs twice");
            }
            require(failures.isEmpty(), String.join("; ", failures));
            return null;
        });
    }

    public static void main(String[] args) throws Exception {
        Platform.startup(() -> Platform.setImplicitExit(false));
        try {
            contracts();
            overlap();
            exceptionalOwnership();
            rejection();
        } finally {
            if (!ThreadPoolManager.getInstance().isShutdown()) {
                ThreadPoolManager.getInstance().shutdown();
            }
            Platform.exit();
        }
        System.out.println("ACT-007A PASS");
    }
}
