import java.util.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.*;
import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import javafx.scene.Parent;
import javafx.stage.*;
import ru.inversion.fore.form.action.ActionMap;
import ru.inversion.fore.form.action.ForeActions;
import ru.inversion.fx.app.BaseApp;
import ru.inversion.fx.app.property.*;
import ru.inversion.fx.app.frame.JInvMainFrame.JInvFrameMode;
import ru.inversion.fx.app.service.ViewPrefAppService;
import ru.inversion.fx.form.*;
import ru.inversion.utils.SingletonInstance;
import static ru.inversion.fx.form.AbstractBaseController.FormModeEnum.VM_NONE;

public class SingletonCloseVetoSmoke {
    static final CompletableFuture<Void> done = new CompletableFuture<>();
    static final List<Controller> opened = new ArrayList<>();
    static final List<ResultForm<Object>> callbacks = new ArrayList<>();
    static boolean failNextInit;
    private static boolean actionMaps;
    private static final Map<Controller, ActionMap> maps = new IdentityHashMap<>();
    static class App extends BaseApp {
        static void install() { g_app = new App(); }
        @Override public void reportGuiLaunch() {}
        @Override public Stage getPrimaryStage() { return null; }
        @Override public IAppProperties getProperties(PropertiesTypeEnum type) {
            return new LOCAL_AppProperties(this, type) {
                @Override protected Object doLoadProperty(String name) { return null; }
                @Override public void setProperty(String name, Object value) { throw new AssertionError("Preference write: "+name); }
            };
        }
        @Override public ViewPrefAppService getViewPrefService() {
            return new ViewPrefAppService() {
                @Override public JInvFrameMode getFrameMode() { return JInvFrameMode.SDI; }
                @Override public void refreshViewSettingsRoot(Parent root) {}
            };
        }
    }
    @SingletonInstance
    public static final class Controller extends JInvFXFormController<Object> {
        boolean veto = true;
        int rejectedOkCalls;
        final AtomicInteger cleaned = new AtomicInteger();
        @Override protected void closeResources() { cleaned.incrementAndGet(); }
        @Override protected void beforeInit() {
            if (actionMaps) {
                require(peekActionMap() == null, "controller registry is lazy before init");
                var map = getActionMap();
                map.put("operation", ForeActions.custom("operation", event -> {}));
                maps.put(this, map);
            }
        }
        @Override protected void init() {
            if (failNextInit) {
                failNextInit = false;
                veto = false;
                throw new IllegalStateException("controlled initialization failure");
            }
        }
        @Override public ru.inversion.tc.TaskContext getTaskContext() { return null; }
        @Override protected boolean onCancel() { return super.onCancel() && !veto; }
        @Override protected boolean onOK() { rejectedOkCalls++; return false; }
        void useParent(ViewContext parent) { parentviewContext = parent; }
    }
    static void launch(Runnable check) {
        new FXFormLauncher<Object>(null,null,"singleton.fxml")
                .bundle(ResourceBundle.getBundle("fore")).dialogMode(VM_NONE).disableSavedDimensions(true).disableHandleInitException(true)
                .controllerCallback(ctrl -> opened.add((Controller)ctrl))
                .closeCallback(callbacks::add)
                .show();
        Platform.runLater(() -> checked(check));
    }
    static boolean showing(Controller controller) { return controller.getViewContext().getStage().isShowing(); }
    static void requestClose(Controller controller) {
        var stage = controller.getViewContext().getStage();
        stage.fireEvent(new WindowEvent(stage, WindowEvent.WINDOW_CLOSE_REQUEST));
    }
    static void require(boolean condition, String msg) {
        if (!condition) throw new AssertionError(msg);
    }
    static void checked(Runnable r) {
        try { r.run(); } catch (Throwable failure) { done.completeExceptionally(failure); }
    }
    static void start() {
        App.install();
        launch(() -> {
            require(opened.size()==1 && showing(opened.getFirst()), "first launch");
            var first=opened.getFirst();
            if (actionMaps) {
                var child = new Controller();
                child.useParent(first.getViewContext());
                var inherited = child.getActionMap();
                require(inherited.getParent() == first.getActionMap()
                        && inherited.get("operation") == first.getActionMap().get("operation"),
                        "parent resolves through the shown form's existing controller context");
            }
            launch(() -> {
                require(opened.size() == 1 && showing(first) && first.getException() == null
                        && first.getLifeCycleState() == LifeCycleStateEnum.RUNTIME, "duplicate blocked before veto");
                requestClose(first);
                require(showing(first) && callbacks.isEmpty() && first.cleaned.get() == 0
                        && first.getLifeCycleState() == LifeCycleStateEnum.RUNTIME, "veto remained open");
                if (actionMaps) {
                    require(first.getActionMap() == maps.get(first) && first.getActionMap().get("operation") != null,
                            "veto preserves operation identity");
                }
                launch(() -> {
                    long visible = opened.stream().filter(SingletonCloseVetoSmoke::showing).count();
                    System.out.printf("after veto: launched=%d visible=%d callbacks=%d%n", opened.size(), visible, callbacks.size());
                    require(opened.equals(List.of(first)) && visible == 1 && callbacks.isEmpty(), "veto must preserve registry owner");
                    Runnable closeAndReopen = () -> {
                        first.veto = false;
                        requestClose(first);
                        verifyClosed(first, 1);
                        launch(() -> {
                            require(opened.size() == 2 && showing(opened.getLast()), "reopen after accepted Cancel");
                            var next = opened.getLast();
                            if (actionMaps) {
                                require(next.getActionMap() != maps.get(first)
                                        && next.getActionMap().get("operation") != null,
                                        "reopened controller has its own operation registry");
                            }
                            afterProgressHidden(next, () -> {
                                verifyClosed(next, 2);
                                launch(() -> {
                                    require(opened.size() == 3 && showing(opened.getLast()), "reopen after closeNow");
                                    var last = opened.getLast();
                                    last.veto = false;
                                    requestClose(last);
                                    verifyClosed(last, 3);
                                    if (matrix) {
                                        checkFailedInit();
                                    } else {
                                        done.complete(null);
                                    }
                                });
                            });
                            next.closeNow(AbstractBaseController.FormReturnEnum.RET_CANCEL);
                        });
                    };
                    if (matrix) {
                        checkRejectedOk(first, closeAndReopen);
                    } else {
                        closeAndReopen.run();
                    }
                });
            });
        });
    }

    private static void checkRejectedOk(Controller first, Runnable next) {
        first.close(AbstractBaseController.FormReturnEnum.RET_OK);
        Platform.runLater(() -> checked(() -> {
            require(first.rejectedOkCalls == 1 && showing(first) && callbacks.isEmpty()
                    && first.cleaned.get() == 0, "rejected OK must keep singleton open");
            launch(() -> {
                require(opened.equals(List.of(first)) && showing(first), "rejected OK preserves registry owner");
                System.out.println("Rejected OK: one visible owner, zero callbacks or cleanup");
                next.run();
            });
        }));
    }

    private static void checkFailedInit() {
        failNextInit = true;
        launch(() -> {
            var failed = opened.getLast();
            afterProgressHidden(failed, () -> {
                require(opened.size() == 4 && !showing(failed) && failed.cleaned.get() == 1 && callbacks.size() == 4
                        && callbacks.getLast().getController() == failed
                        && callbacks.getLast().getException() instanceof IllegalStateException
                        && "controlled initialization failure".equals(callbacks.getLast().getException().getMessage()),
                        "failed initialization closes and reports the original error");
                if (actionMaps) {
                    checkRetired(failed);
                }
                launch(() -> {
                    require(opened.size() == 5 && showing(opened.getLast()), "reopen after initialization failure");
                    var last = opened.getLast();
                    last.veto = false;
                    requestClose(last);
                    verifyClosed(last, 5);
                    System.out.println("Initialization failure: one error callback and cleanup, later reopen succeeds");
                    done.complete(null);
                });
            });
        });
    }

    private static void afterProgressHidden(Controller controller, Runnable continuation) {
        Stage owner = controller.getViewContext().getStage();
        Window.getWindows().addListener(new ListChangeListener<Window>() {
            @Override public void onChanged(Change<? extends Window> change) {
                while (change.next()) {
                    for (Window window : change.getAddedSubList()) {
                        if (window instanceof Stage progress && progress.getOwner() == owner) {
                            Window.getWindows().removeListener(this);
                            // WINDOW_HIDDEN приходит до выхода из вложенного цикла showAndWait().
                            progress.addEventHandler(WindowEvent.WINDOW_HIDDEN, event ->
                                    Platform.runLater(() -> checked(() -> {
                                        require(!Platform.isNestedLoopRunning() && !progress.isShowing(),
                                                "resource progress window and nested loop must be closed");
                                        continuation.run();
                                    })));
                            return;
                        }
                    }
                }
            }
        });
    }

    private static boolean matrix;
    static void verifyClosed(Controller controller, int count) {
        require(!showing(controller) && callbacks.size() == count && controller.cleaned.get() == 1
                && callbacks.getLast().getController() == controller
                && callbacks.getLast().getFormReturn() == AbstractBaseController.FormReturnEnum.RET_CANCEL
                && callbacks.getLast().getException() == null, "one callback and cleanup for matching owner " + count);
        if (actionMaps) {
            checkRetired(controller);
        }
    }

    private static void checkRetired(Controller owner) {
        var map = maps.get(owner);
        require(map == owner.peekActionMap() && map.get("operation") == null && map.getParent() == null,
                "retained reference loses registrations and parent");
        try {
            map.put("operation", ForeActions.custom("retired", event -> {}));
            throw new AssertionError("retired registry accepts registration");
        } catch (IllegalStateException expected) {
            // Закрытый владелец больше не принимает операции.
        }
    }

    public static void main(String[] args) throws Exception {
        actionMaps = args.length == 1 && "action-map".equals(args[0]);
        matrix = actionMaps || (args.length == 1 && "matrix".equals(args[0]));
        Path output = Path.of(SingletonCloseVetoSmoke.class.getProtectionDomain().getCodeSource().getLocation().toURI());
        Files.writeString(output.resolve("singleton.fxml"), """
                <?import javafx.scene.layout.VBox?>
                <VBox xmlns:fx="http://javafx.com/fxml/1" fx:controller="SingletonCloseVetoSmoke$Controller"
                      prefWidth="260" prefHeight="100"/>
                """);
        Platform.startup(() -> {
            Platform.setImplicitExit(false);
            Platform.runLater(() -> checked(SingletonCloseVetoSmoke::start));
        });
        int code=1;
        try { done.get(20,TimeUnit.SECONDS); System.out.println(actionMaps ? "ACT-013 PASS" : matrix ? "CTRL-035 PASS" : "CTRL-032 PASS"); code=0; }
        catch (Exception error) { error.printStackTrace(); }
        finally { Platform.exit(); }
        System.exit(code);
    }
}
