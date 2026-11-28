import java.util.List;
import java.util.ResourceBundle;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.event.Event;
import javafx.scene.Scene;
import javafx.scene.control.ButtonBase;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import ru.inversion.dataset.XXIDataSet;
import ru.inversion.dataset.fx.DSFXAdapter;
import ru.inversion.dataset.mark.IMarkable;
import ru.inversion.fx.app.BaseApp;
import ru.inversion.fx.app.service.ViewPrefAppService;
import ru.inversion.fx.form.action.JInvAction;
import ru.inversion.fx.form.controls.JInvTable;
import ru.inversion.fx.form.controls.dsbar.Part_MarkButtons;

public final class ActionMapMarkViewSmoke {
    private static final class App extends BaseApp {
        static void install() { g_app = new App(); }

        @Override
        public ViewPrefAppService getViewPrefService() {
            return new ViewPrefAppService() {
                @Override
                public Boolean isMarkLeft() { return true; }
            };
        }
    }

    private static final class Row implements IMarkable {
        boolean marked;
        @Override public boolean isMark() { return marked; }
        @Override public void setMark(boolean marked) { this.marked = marked; }
        @Override public Object getMarkID1() { return 1; }
    }

    private static final class Rows extends XXIDataSet<Row> {
        final List<Row> data = FXCollections.observableArrayList(new Row());
        int toggles;
        @Override protected List<Row> rows() { return data; }
        @Override public List<Row> getRows() { return data; }
        @Override public Class<Row> getRowClass() { return Row.class; }
        @Override public boolean isSupportMark() { return true; }
        @Override public boolean isMarkCurrentRow() { return data.getFirst().isMark(); }
        @Override public void markCurrentRow() { data.getFirst().setMark(true); toggles++; }
        @Override public void unMarkCurrentRow() { data.getFirst().setMark(false); toggles++; }
    }

    private static final class Adapter extends DSFXAdapter<Row> {
        int calls;

        @Override
        public void revertMarkCurrentRow() {
            calls++;
            super.revertMarkCurrentRow();
        }
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private static void exercise() throws Exception {
        App.install();
        var toolbarBundle = ResourceBundle.getBundle("fore");
        var table = new JInvTable<Row>();
        var rows = new Rows();
        var adapter = new Adapter();
        var stage = new Stage();
        var root = new VBox(table);
        stage.setScene(new Scene(root, 450, 300));
        adapter.bindTable(rows, table, true);
        table.initMouse();
        var first = new Part_MarkButtons(adapter, toolbarBundle);
        var second = new Part_MarkButtons(adapter, toolbarBundle);
        root.getChildren().addAll(first.createControlPane(), second.createControlPane());
        stage.show();
        try {
            var firstButton = (ButtonBase) first.getControlPane().getChildren().getFirst();
            var secondButton = (ButtonBase) second.getControlPane().getChildren().getFirst();
            var legacyHandler = firstButton.getOnAction();
            require(legacyHandler instanceof JInvAction legacy && legacy.getIcon() != null && firstButton.getGraphic() == null
                    && firstButton.getTooltip().getText().equals(toolbarBundle.getString("MARK")),
                    "legacy button handler, tooltip, icon descriptor and pre-login graphic");
            require(adapter.peekActionMap() == null, "view construction does not fill registry");
            firstButton.fire();
            var map = adapter.peekActionMap();
            var operation = map.getLocal(DSFXAdapter.ACTION_MARK_CURRENT_ROW);
            require(rows.isMarkCurrentRow() && rows.toggles == 1 && adapter.calls == 1, "first view marks once");
            secondButton.fire();
            require(!rows.isMarkCurrentRow() && rows.toggles == 2 && adapter.calls == 2
                    && map.getLocal(DSFXAdapter.ACTION_MARK_CURRENT_ROW) == operation, "same operation in second view");
            Event.fireEvent(table, new MouseEvent(MouseEvent.MOUSE_CLICKED, 5, 5, 5, 5, MouseButton.PRIMARY, 1,
                    false, true, false, false, false, false, false, false, false, true, null));
            require(rows.isMarkCurrentRow() && rows.toggles == 3 && adapter.calls == 3, "table path marks once");
            operation.setDisabled(true);
            firstButton.fire();
            require(rows.toggles == 3 && adapter.calls == 3, "disabled operation rejects either view");
            operation.setDisabled(false);
            adapter.close();
            operation.handle(null);
            secondButton.fire();
            require(rows.toggles == 3 && adapter.calls == 3 && firstButton.getOnAction() == legacyHandler,
                    "retained operation and view cannot mutate closed adapter");
        } finally {
            adapter.close();
            stage.close();
        }
    }

    public static void main(String[] args) throws Exception {
        Platform.startup(() -> Platform.setImplicitExit(false));
        var test = new FutureTask<Void>(() -> {
            exercise();
            return null;
        });
        Platform.runLater(test);
        try {
            test.get(20, TimeUnit.SECONDS);
        } finally {
            Platform.exit();
        }
        System.out.println("ACT-017 PASS");
    }
}
