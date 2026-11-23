import java.util.List;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableView;
import javafx.scene.layout.Pane;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import ru.inversion.fx.form.AbstractBaseController;
import ru.inversion.fx.form.JInvFXFormController;
import ru.inversion.fx.form.controls.Controls;
import ru.inversion.fx.form.controls.JInvTable;
import ru.inversion.fx.form.controls.JInvTableColumn;

public final class ControlsFollowupSmoke {
    public static void main(String[] args) throws Exception {
        var task = new FutureTask<Void>(() -> {
            check(Runtime.version().feature() == 25
                    && System.getProperty("javafx.version", "").matches("25(?:\\..*)?"), "Java and JavaFX 25");
            titles();
            collections();
            lookup();
            scrolling();
            return null;
        });
        Platform.startup(() -> Platform.runLater(task));
        try {
            task.get(20, TimeUnit.SECONDS);
            System.out.println("CTRL-CONTROLS-FOLLOWUP PASS");
        } finally {
            Platform.exit();
        }
    }

    private static void titles() {
        var column = new JInvTableColumn<Object, String>("Caption");
        check("Caption".equals(Controls.getTitleFromTableColumn(column)), "detached title");
        var stock = new TableView<Object>();
        stock.getColumns().add(column);
        check("Caption".equals(Controls.getTitleFromTableColumn(column)), "stock table title");
        stock.getColumns().remove(column);
        column.setText("");
        column.setGraphic(new Label("Graphic caption"));
        check("Graphic caption".equals(Controls.getTitleFromTableColumn(column)), "graphic fallback");
        column.setGraphic(null);
        column.setToolTipText("Tooltip fallback");
        check("Tooltip fallback".equals(Controls.getTitleFromTableColumn(column)), "tooltip fallback");
        new JInvTable<Object>().getColumns().add(column);
        column.setText("Long header");
        check("Long header".equals(Controls.getTitleFromTableColumn(column)), "legacy header precedence");
    }

    private static void collections() {
        var pane = new Pane();
        check(Controls.findNonControlComponents(pane).isEmpty() && !pane.hasProperties(), "pristine lookup");
        pane.getProperties().put("other", true);
        check(Controls.findNonControlComponents(pane).isEmpty() && pane.getProperties().size() == 1,
                "unrelated metadata preserved");
        var list = Controls.getNonControlComponentFromContainer(pane);
        list.add("one");
        Controls.addNonControlComponentToContainer("two", pane);
        check(Controls.findNonControlComponents(pane) == list
                && Controls.getNonControlComponentFromContainer(pane) == list
                && list.equals(List.of("one", "two")), "stored list identity and mutability");
    }

    private static void lookup() {
        var leaf = new Rectangle();
        var button = new Button();
        var near = new Pane(leaf);
        var middle = new Pane(near, button);
        var root = new Pane(middle);
        var closeProperty = new ReadOnlyObjectWrapper<AbstractBaseController<?>>().getReadOnlyProperty();
        var farProperty = new ReadOnlyObjectWrapper<AbstractBaseController<?>>().getReadOnlyProperty();
        String key = JInvFXFormController.PROPERTY_CONTROLLER;
        check(Controls.getControllerProperty(leaf) == null
                && !near.hasProperties() && !middle.hasProperties() && !root.hasProperties(), "missing controller");
        root.getProperties().put(key, farProperty);
        near.getProperties().put(key, closeProperty);
        check(Controls.getControllerProperty(leaf) == closeProperty
                && Controls.getControllerProperty(near) == closeProperty
                && Controls.getControllerProperty(button) == farProperty, "nearest controller property identity");
        check(Controls.getParentStreamOfControl(leaf).toList().equals(List.of(near, middle, root)),
                "parent stream order");
        var nodes = Controls.getNodeList(root, null);
        var controls = Controls.getControlList(root, null);
        check(nodes.equals(List.of(root, middle, near, leaf, button)) && controls.equals(List.of(button)),
                "traversal order and filtering");
        nodes.clear();
        controls.clear();
        check(near.getChildren().contains(leaf) && middle.getChildren().contains(button), "mutable result snapshots");
    }

    private static void scrolling() {
        var child = new Pane();
        child.setPrefSize(40, 40);
        var inner = new Pane(child);
        inner.relocate(0, 420);
        var content = new Pane(inner);
        content.setPrefSize(300, 700);
        var scroll = new ScrollPane(content);
        scroll.setVmin(10);
        scroll.setVmax(30);
        scroll.setPrefSize(280, 175);
        var outer = new ScrollPane(new Pane(scroll));
        var stage = new Stage();
        stage.setScene(new Scene(outer, 300, 190));
        stage.show();
        try {
            scroll.applyCss();
            scroll.layout();
            var bounds = content.getBoundsInLocal();
            check(Controls.getRecursiveBoundsOfParent(content, bounds, content) == bounds, "identity bounds");
            try {
                Controls.getRecursiveBoundsOfParent(child, child.getBoundsInLocal(), new Pane());
                throw new AssertionError("unrelated ancestor accepted");
            } catch (IllegalArgumentException expected) {
                check(expected.getMessage() != null, "unrelated ancestor has a diagnostic");
            }
            scroll.setVvalue(10);
            Controls.scrollToNode(child);
            double position = scroll.getVvalue();
            var target = Controls.getRecursiveBoundsOfParent(child, child.getBoundsInLocal(), content);
            double height = scroll.getViewportBounds().getHeight();
            double top = (position - 10) / 20 * (content.getLayoutBounds().getHeight() - height);
            check(position > 10 && target.getMinY() >= top - .01 && target.getMaxY() <= top + height + .01,
                    "scroll target into viewport");
            check(outer.getVvalue() == outer.getVmin(), "outer viewport unchanged");
            Controls.scrollToNode(child);
            Controls.scrollToNode(content, scroll);
            check(Math.abs(scroll.getVvalue() - position) < .0001, "visible target and content root unchanged");
            scroll.setVvalue(30);
            Controls.scrollToNode(child);
            check(scroll.getVvalue() < 30, "scroll upward");
            Controls.scrollToNode(outer);
        } finally {
            stage.close();
        }
    }

    private static void check(boolean condition, String label) {
        if (!condition) {
            throw new AssertionError(label);
        }
    }
}
