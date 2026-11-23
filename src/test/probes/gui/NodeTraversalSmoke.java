import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Spinner;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import ru.inversion.fx.form.controls.Controls;
import ru.inversion.fx.form.controls.JInvTextField;

public final class NodeTraversalSmoke {
    public static void main(String[] args) throws Exception {
        var task = new FutureTask<Void>(NodeTraversalSmoke::checkTraversal, null);
        Platform.startup(() -> Platform.runLater(task));
        try {
            task.get(15, TimeUnit.SECONDS);
            System.out.println("CTRL-003 PASS");
        } finally {
            Platform.exit();
        }
    }

    private static void checkTraversal() {
        String javafx = System.getProperty("javafx.version", "UNKNOWN");
        if (Runtime.version().feature() != 25 || !javafx.matches("25(?:\\..*)?")) {
            throw new AssertionError("Java 25 and JavaFX 25 required");
        }
        System.out.println("Java=" + Runtime.version() + " JavaFX=" + javafx);

        var emptyTabs = new TabPane(new Tab("Empty"));
        var emptyScroll = new ScrollPane();
        var emptyTitled = new TitledPane("Empty", null);
        var field = new JInvTextField();
        field.setFieldName("FIELD");
        field.setId("nested");
        var spinner = new Spinner<Integer>(0, 10, 1);
        spinner.getEditor().setId("excluded-editor");
        var scroll = new ScrollPane(field);
        var titled = new TitledPane("Nested", scroll);
        var tabs = new TabPane(new Tab("Nested", titled));
        var split = new SplitPane(tabs, spinner);
        split.setId("split");
        var root = new VBox(emptyTabs, emptyScroll, emptyTitled, split);
        var stage = new Stage();
        stage.setTitle("CTRL-003 Node traversal");
        stage.setScene(new Scene(root, 480, 360));
        try {
            stage.show();
            var failures = new ArrayList<String>();
            for (var empty : List.of(emptyTabs, emptyScroll, emptyTitled)) {
                var actual = Controls.getNodeList(empty, null);
                check(actual.equals(List.of(empty)), empty.getClass().getSimpleName()
                        + " null entries=" + Collections.frequency(actual, null), failures);
            }

            List<Node> expected = List.of(root, emptyTabs, emptyScroll, emptyTitled,
                    split, tabs, titled, scroll, field, spinner);
            check(Controls.getNodeList(root, null).equals(expected), "default traversal order", failures);
            var visited = new ArrayList<Node>();
            try {
                var filtered = Controls.getNodeList(root, node -> {
                    visited.add(node);
                    return node.getId() != null;
                });
                check(filtered.equals(List.of(split, field)), "filtered traversal order", failures);
                check(visited.equals(expected), "predicate visits each actual node once", failures);
            } catch (NullPointerException exception) {
                failures.add("node.getId() predicate received null=" + visited.contains(null));
            }

            check(Controls.getControlList(root, null).equals(expected.subList(1, expected.size())),
                    "control traversal order", failures);
            check(Controls.getNodeByFieldName(root, "FIELD") == field, "unique field lookup", failures);
            check(Controls.getNodeByFieldName(root, "ABSENT") == null, "missing field lookup", failures);
            var duplicate = new JInvTextField();
            duplicate.setFieldName("FIELD");
            root.getChildren().add(duplicate);
            try {
                Controls.getNodeByFieldName(root, "FIELD");
                failures.add("duplicate field accepted");
            } catch (RuntimeException exception) {
                check("There is several controls with textField FIELD".equals(exception.getMessage()),
                        "unexpected duplicate lookup error: " + exception, failures);
            }
            if (!failures.isEmpty()) {
                throw new AssertionError("CTRL-003 failures: " + failures);
            }
        } finally {
            stage.close();
        }
    }

    private static void check(boolean condition, String description, List<String> failures) {
        if (!condition) {
            failures.add(description);
        }
    }
}
