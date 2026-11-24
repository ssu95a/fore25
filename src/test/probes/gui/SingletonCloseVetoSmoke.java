import java.util.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.*;
import javafx.application.Platform;
import javafx.scene.Parent;
import javafx.stage.*;
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
        final AtomicInteger cleaned = new AtomicInteger();
        @Override protected void closeResources() { cleaned.incrementAndGet(); }
        @Override protected void beforeInit() {}
        @Override public ru.inversion.tc.TaskContext getTaskContext() { return null; }
        @Override protected boolean onCancel() { return super.onCancel() && !veto; }
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
            launch(() -> {
                require(opened.size() == 1 && showing(first) && first.getException() == null
                        && first.getLifeCycleState() == LifeCycleStateEnum.RUNTIME, "duplicate blocked before veto");
                requestClose(first);
                require(showing(first) && callbacks.isEmpty() && first.cleaned.get() == 0
                        && first.getLifeCycleState() == LifeCycleStateEnum.RUNTIME, "veto remained open");
                launch(() -> {
                    long visible = opened.stream().filter(SingletonCloseVetoSmoke::showing).count();
                    System.out.printf("after veto: launched=%d visible=%d callbacks=%d%n", opened.size(), visible, callbacks.size());
                    require(opened.equals(List.of(first)) && visible == 1 && callbacks.isEmpty(), "veto must preserve registry owner");
                    first.veto = false;
                    requestClose(first);
                    verifyClosed(first, 1);
                    launch(() -> {
                        require(opened.size() == 2 && showing(opened.getLast()), "reopen after accepted Cancel");
                        var next = opened.getLast();
                        next.closeNow(AbstractBaseController.FormReturnEnum.RET_CANCEL);
                        Platform.runLater(() -> checked(() -> {
                            verifyClosed(next, 2);
                            launch(() -> {
                                require(opened.size() == 3 && showing(opened.getLast()), "reopen after closeNow");
                                var last = opened.getLast();
                                last.veto = false;
                                requestClose(last);
                                verifyClosed(last, 3);
                                done.complete(null);
                            });
                        }));
                    });
                });
            });
        });
    }
    static void verifyClosed(Controller controller, int count) {
        require(!showing(controller) && callbacks.size() == count && controller.cleaned.get() == 1
                && callbacks.getLast().getController() == controller
                && callbacks.getLast().getFormReturn() == AbstractBaseController.FormReturnEnum.RET_CANCEL
                && callbacks.getLast().getException() == null, "one callback and cleanup for matching owner " + count);
    }

    public static void main(String[] args) throws Exception {
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
        try { done.get(20,TimeUnit.SECONDS); System.out.println("CTRL-032 PASS"); code=0; }
        catch (Exception error) { error.printStackTrace(); }
        finally { Platform.exit(); }
        System.exit(code);
    }
}
