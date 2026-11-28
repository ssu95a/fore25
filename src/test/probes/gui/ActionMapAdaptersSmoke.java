import java.util.ArrayList;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.Scene;
import javafx.scene.control.TableView;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import ru.inversion.dataset.ArrayDataSet;
import ru.inversion.dataset.fx.DSFXAdapter;
import ru.inversion.fore.form.action.ForeActions;
import ru.inversion.fore.form.action.StandardAction;
import ru.inversion.fx.form.AbstractBaseController;
import ru.inversion.fx.form.JInvFXFormController;
import ru.inversion.fx.form.controls.JInvTextField;
import ru.inversion.fx.form.controls.treetableex.JInvTreeTableEx;
import ru.inversion.fx.form.controls.treetableex.TSFXAdapter;
import ru.inversion.tds.XXITreeDataSet;

public final class ActionMapAdaptersSmoke {
    private static final class OfflineController extends JInvFXFormController<Object> {
        @Override
        public ru.inversion.tc.TaskContext getTaskContext() {
            return null;
        }
    }

    @Entity
    public static final class Row {
        @Id @Column(name = "ID")
        public String getId() { return "row"; }
        public void setId(String id) {}
        @Column(name = "NAME")
        public String getName() { return "name"; }
        public void setName(String name) {}
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private static DSFXAdapter<Row> bind(TableView<Row> view) throws Exception {
        var rows = new ArrayDataSet<>(Row.class, new ArrayList<Row>(), false);
        var adapter = new DSFXAdapter<Row>();
        adapter.bindTable(rows, view, null, false);
        return adapter;
    }

    private static TSFXAdapter<Row> bindTree(JInvTreeTableEx<Row> tree) {
        return TSFXAdapter.<Row>newBuilder().treeTable(tree).treeDataSet(new XXITreeDataSet<>(Row.class))
                .markMode(TSFXAdapter.ItemMarkMode.NONE).bind();
    }

    private static void exercise() throws Exception {
        var form = new OfflineController();
        var other = new OfflineController();
        var refresh = ForeActions.custom("form refresh", event -> {});
        var left = ForeActions.custom("first data refresh", event -> {});
        var right = ForeActions.custom("second data refresh", event -> {});
        var treeRefresh = ForeActions.custom("tree refresh", event -> {});
        var first = new TableView<Row>();
        var second = new TableView<Row>();
        var tree = new JInvTreeTableEx<Row>();
        var field = new JInvTextField();
        var root = new VBox(first, second, tree, field);
        root.getProperties().put(AbstractBaseController.PROPERTY_CONTROLLER, new SimpleObjectProperty<>(form));
        var otherRoot = new VBox();
        otherRoot.getProperties().put(AbstractBaseController.PROPERTY_CONTROLLER, new SimpleObjectProperty<>(other));
        var stage = new Stage();
        stage.setScene(new Scene(root, 500, 420));
        var otherStage = new Stage();
        otherStage.setScene(new Scene(otherRoot, 340, 220));
        stage.show();
        otherStage.show();
        var one = bind(first);
        var two = bind(second);
        var ts = bindTree(tree);
        DSFXAdapter<Row> replacement = null;
        TSFXAdapter<Row> treeReplacement = null;
        try {
            require(one.peekActionMap() == null && two.peekActionMap() == null
                    && ts.peekActionMap() == null && form.peekActionMap() == null, "all scopes lazy");
            one.bindControl(field, "name", Row::getName);
            require(field.<Row>getDataSetAdapter() == one, "field resolves exact dataset adapter");
            var oneActions = one.getActionMap();
            var twoActions = two.getActionMap();
            var treeActions = ts.getActionMap();
            form.getActionMap().put(StandardAction.REFRESH, refresh);
            require(oneActions.get(StandardAction.REFRESH) == refresh && treeActions.get(StandardAction.REFRESH) == refresh,
                    "both adapter types inherit the same form");
            oneActions.put(StandardAction.REFRESH, left);
            twoActions.put(StandardAction.REFRESH, right);
            treeActions.put(StandardAction.REFRESH, treeRefresh);
            require(oneActions.get(StandardAction.REFRESH) == left && twoActions.get(StandardAction.REFRESH) == right
                    && tree.getTSFXAdapter() == ts && treeActions.get(StandardAction.REFRESH) == treeRefresh,
                    "same ID resolves each mini-controller");
            root.getChildren().remove(second);
            require(twoActions.getParent() == null && twoActions.get(StandardAction.REFRESH) == right, "detached view");
            otherRoot.getChildren().add(second);
            require(two.getActionMap().getParent() == other.getActionMap(), "reattached to different controller");
            replacement = bind(first);
            treeReplacement = bindTree(tree);
            require(oneActions.size() == 0 && oneActions.getParent() == null
                    && treeActions.size() == 0 && treeActions.getParent() == null, "replaced maps retired");
            one.close();
            ts.close();
            require(replacement.getActionMap().get(StandardAction.REFRESH) == refresh
                    && treeReplacement.getActionMap().get(StandardAction.REFRESH) == refresh,
                    "closing old adapters cannot retire replacement maps");
            var replacementTreeActions = treeReplacement.getActionMap();
            treeReplacement.close();
            require(replacementTreeActions.get(StandardAction.REFRESH) == null, "tree close retires map");
            two.close();
            require(twoActions.get(StandardAction.REFRESH) == null && twoActions.getParent() == null,
                    "table close retires map");
            require(form.getActionMap().get(StandardAction.REFRESH) == refresh, "form registry survives adapter close");
        } finally {
            if (replacement != null) {
                replacement.close();
            }
            if (treeReplacement != null) {
                treeReplacement.close();
            }
            one.close();
            two.close();
            ts.close();
            otherStage.close();
            stage.close();
        }
    }

    public static void main(String[] args) throws Exception {
        var asynchronous = new AtomicReference<Throwable>();
        Platform.startup(() -> {
            Platform.setImplicitExit(false);
            Thread.currentThread().setUncaughtExceptionHandler((thread, error) -> asynchronous.set(error));
        });
        var test = new FutureTask<Void>(() -> {
            exercise();
            return null;
        });
        Platform.runLater(test);
        try {
            test.get(20, TimeUnit.SECONDS);
            require(asynchronous.get() == null, "unexpected JavaFX callback failure: " + asynchronous.get());
        } finally {
            Platform.exit();
        }
        System.out.println("ACT-014 PASS");
    }
}
