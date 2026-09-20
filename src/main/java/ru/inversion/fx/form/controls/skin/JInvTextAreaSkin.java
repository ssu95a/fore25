/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ru.inversion.fx.form.controls.skin;
import com.sun.javafx.scene.control.skin.TextAreaSkin;
import java.util.ResourceBundle;
import java.util.function.Consumer;
import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.css.PseudoClass;
import javafx.event.ActionEvent;
import javafx.geometry.HPos;
import javafx.geometry.Orientation;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBase;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ToolBar;
import javafx.scene.control.Tooltip;
import ru.inversion.fx.app.es.JInvErrorService;
import ru.inversion.fx.app.service.exteditor.ExtensionType;
import ru.inversion.fx.app.service.exteditor.ExternalEditorManager;
import ru.inversion.fx.form.ActionFactory;
import ru.inversion.fx.form.action.JInvKeyboardManager;
import ru.inversion.fx.form.action.JInvParallelAction;
import ru.inversion.fx.form.controls.Controls;
import ru.inversion.fx.form.controls.JInvFEButton;
import ru.inversion.fx.form.controls.JInvTextArea;

/**
 *
 * @author antonovdi
 */
public class JInvTextAreaSkin extends TextAreaSkin implements ISkinPopulatable {

    private ToolBar toolBar = new ToolBar();
    private static ResourceBundle bundle = ResourceBundle.getBundle("fore");
    private final ObservableList<MenuItem> listPopulateContextMenu = FXCollections.observableArrayList();
    private Consumer<ContextMenu> onPopulateContextMenu;
    private JInvFEButton feButton;

    public JInvFEButton getFeButton() {
        return feButton;
    }

    public JInvTextAreaSkin(JInvTextArea textArea) {
        super(textArea);
        textArea.sceneProperty().addListener(new ChangeListener<Scene>() {
            //Сработали разок – и хватит
            boolean triggeredOnce = false;

            @Override
            public void changed(ObservableValue<? extends Scene> observable, Scene oldValue, Scene newValue) {
                if ( triggeredOnce ){
                    return;
                }

                if (newValue != null) {
                    JInvKeyboardManager.addAction( textArea,
                            ActionFactory.createAction( ActionFactory.ActionTypeEnum.EDIT_EXTERNAL,
                                e -> {
                                    if ( JInvTextAreaSkin.this.getSkinnable().isFocused() ) {
                                        editInExtEditor( e );
                                    }
                                } ) );
                    triggeredOnce = true;
                }
            }
        });

        initToolBar( textArea );
    }

    public void initToolBar( final JInvTextArea textArea ) {

        toolBar.visibleProperty().bind(textArea.toolBarVisibleProperty());
        toolBar.setOrientation( Orientation.VERTICAL);
        toolBar.setManaged(false);
        toolBar.applyCss();

        //Применение стиля к добавленным кнопкам
        toolBar.getItems().addListener((ListChangeListener<? super Node>) c -> c.getList().forEach( this::setButtonStyle ));

        feButton = new JInvFEButton(textArea);
        feButton.visibleProperty().bind( textArea.buttonFEVisibleProperty() );

        toolBar.getItems().add(feButton);

        textArea.editableProperty().addListener(new ChangeListener< Boolean >() {

            final private Button edButton;
            {
                edButton = ActionFactory.createButton (
                                ActionFactory.ActionTypeEnum.EDIT_EXTERNAL,
                                JInvTextAreaSkin.this::editInExtEditor
                );

                edButton.setTooltip(new Tooltip(bundle.getString("TOOLTIP_EDIT_IN_EXTERNAL_EDITOR")));
            }
            @Override
            public void changed( ObservableValue< ? extends Boolean > observable, Boolean oldValue, Boolean newValue ) {
                if( newValue )
                    toolBar.getItems().add(edButton);
                else
                    toolBar.getItems().remove(edButton);
            }
        });

        getChildren().addAll(toolBar);
    }

    private double getMaxItemWidth() {
        double size = -1;
        for (Node b : toolBar.getItems()) {
            if ( b instanceof ButtonBase ){
                size = Math.max(size, ( (ButtonBase) b ).getPrefWidth());
            }
        }
        return size;
    }

    @Override
    protected void layoutChildren(double x, double y, double w, double h) {

        if (toolBar.isVisible()) {
            super.layoutInArea(
                    toolBar,
                    x + 2,
                    y + 2,
                    toolBar.getWidth()- 2,
                    h - 4,
                    -1,
                    HPos.CENTER,
                    VPos.TOP
            );
            super.layoutChildren(getX(x), y, getW(w), h);

            toolBar.applyCss();

        } else {
            super.layoutChildren(x, y, w, h);
        }
    }

    private void setButtonStyle(Node b) {
        if (b instanceof Button) {
            final InvalidationListener listener = observable -> {
                double size = getMaxItemWidth();
                for (Node node : toolBar.getItems()) {
                    if( node instanceof ButtonBase ){
                        ( (ButtonBase) node ).setPrefWidth(size);
                    }
                }
            };
            ( (ButtonBase) b ).widthProperty().addListener( listener );
            b.pseudoClassStateChanged( PseudoClass.getPseudoClass("toolbar-vertical"), true );
            ((Button) b).setMaxWidth(Double.MAX_VALUE);
        }
    }

    public ToolBar getToolBar() {
        return toolBar;
    }

    double getX(double x) {
        if (toolBar.isVisible()) {
            return x + toolBar.getWidth();
        }
        return x;
    }

    double getY(double y) {
        return y;
    }

    double getW(double w) {
        if (toolBar.isVisible()) {
            return w - toolBar.getWidth();
        }
        return w;
    }

    private void editInExtEditor(ActionEvent event) {
        ExtensionType type = ((JInvTextArea) getSkinnable()).getExtensionType();
        ExternalEditorManager manager = ExternalEditorManager.INSTANCE();
        if (manager.checkExistencePathToEditor(Controls.getControllerFromControl(getSkinnable()).getViewContext(), type)) {
            new JInvParallelAction((ActionEvent event1) -> {

                try {
                    String newValue = manager.editInExternalEditor(getSkinnable().getText(), type);
                    Controls.setValue(getSkinnable(), newValue);
                } catch (Throwable ex) {
                    Platform.runLater(() -> JInvErrorService.handleException(null, ex) );
                }

            }, null).handle(event);
        }
    }
    @Override
    public void populateContextMenu(ContextMenu contextMenu) {
        super.populateContextMenu(contextMenu);
        if (!(listPopulateContextMenu.isEmpty())) {
            listPopulateContextMenu.forEach((MenuItem items) -> contextMenu.getItems().add(0, items));
        }
        if ( onPopulateContextMenu != null ){
            onPopulateContextMenu.accept( contextMenu );
        }
    }

    @Override
    public ObservableList<MenuItem> getListItemsToPopulateContextMenu() {
        return listPopulateContextMenu;
    }

    @Override
    public void onPopulateContextMenu( Consumer<ContextMenu> action ) {
        onPopulateContextMenu = action;
    }
}
