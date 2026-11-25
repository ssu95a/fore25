import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import ru.inversion.fx.form.controls.Controls;
import ru.inversion.fx.form.controls.JInvButton;
import ru.inversion.fx.form.controls.JInvFEButton;
import ru.inversion.fx.form.controls.JInvWrapButton;
import ru.inversion.fx.form.lov.JInvLOVButton;

public final class WrapButtonOwnershipSmoke {
    private final CompletableFuture<Void> done = new CompletableFuture<>();
    private final List<String> failures = new ArrayList<>();
    private final List<String> routed = new ArrayList<>();

    private void expect(boolean passed, String label) {
        if (!passed) failures.add(label);
    }

    private void start() {
        var wrapper = new JInvWrapButton();
        var a = new JInvLOVButton();
        var b = new JInvButton();
        a.setOnAction(event -> routed.add("A"));
        b.setOnAction(event -> routed.add("B"));
        var stage = new Stage();
        stage.setScene(new Scene(new VBox(wrapper), 280, 120));
        stage.show();
        wrapper.setInnerButton(a);
        wrapper.setInnerButton(new JInvFEButton());
        require(wrapper.getInnerButton() == a, "LOV priority");
        later(() -> {
            require(wrapper.getOnAction() == a.getOnAction() && a.getProperties().get(Controls.CONTROL_PARENT) == wrapper,
                    "healthy initial A binding");
            wrapper.fire();
            require(routed.equals(List.of("A")), "A fires once");
            wrapper.setInnerButton(null);
            wrapper.fire();
            expect(routed.equals(List.of("A")) && wrapper.getOnAction() == null
                    && !a.getProperties().containsKey(Controls.CONTROL_PARENT), "settled detach retains A action/parent");
            System.out.printf("detach routed=%s, old action=%s, old parent=%s%n",
                    routed, wrapper.getOnAction() == a.getOnAction(), a.getProperties().get(Controls.CONTROL_PARENT) == wrapper);
            routed.clear();
            wrapper.setInnerButton(a);
            wrapper.setInnerButton(b);
            wrapper.setInnerButton(a);
            later(() -> {
                wrapper.fire();
                expect(routed.equals(List.of("A")) && !b.getProperties().containsKey(Controls.CONTROL_PARENT),
                        "same-turn ABA only routes A");
                wrapper.setInnerButton(b);
                wrapper.fire();
                expect(routed.equals(List.of("A")) && !a.getProperties().containsKey(Controls.CONTROL_PARENT),
                        "old A must not fire while B pending");
                later(() -> {
                    int before = routed.size();
                    wrapper.fire();
                    require(routed.size() == before + 1 && "B".equals(routed.getLast()), "settled B fires once");
                    var foreignParent = new VBox();
                    b.getProperties().put(Controls.CONTROL_PARENT, foreignParent);
                    wrapper.onActionProperty().unbind();
                    wrapper.setOnAction(event -> routed.add("caller"));
                    var caller = wrapper.getOnAction();
                    wrapper.setInnerButton(null);
                    wrapper.fire();
                    expect(wrapper.getOnAction() == caller && routed.size() == before + 2
                            && "caller".equals(routed.getLast())
                            && b.getProperties().get(Controls.CONTROL_PARENT) == foreignParent,
                            "caller-owned action and foreign parent preserved");
                    System.out.println("final routed=" + routed);
                    stage.hide();
                    require(failures.isEmpty(), "wrapper ownership: " + failures);
                    done.complete(null);
                });
            });
        });
    }

    private void later(Runnable step) {
        Platform.runLater(() -> {
            try { step.run(); } catch (Throwable failure) { done.completeExceptionally(failure); }
        });
    }

    public static void main(String[] args) {
        var smoke = new WrapButtonOwnershipSmoke();
        Platform.startup(() -> smoke.later(smoke::start));
        int exit = 1;
        try {
            smoke.done.get(25, TimeUnit.SECONDS);
            System.out.println("CTRL-055 PASS");
            exit = 0;
        } catch (Exception failure) {
            failure.printStackTrace(System.err);
        } finally {
            Platform.exit();
        }
        System.exit(exit);
    }

    private static void require(boolean ok, String label) {
        if (!ok) throw new AssertionError(label);
    }
}
