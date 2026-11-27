/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ru.inversion.fx.form.controls.skin;
import javafx.scene.control.skin.TextAreaSkin;
import java.util.List;
import java.util.ResourceBundle;
import java.util.function.Consumer;
import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.css.PseudoClass;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Orientation;
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
import ru.inversion.fx.form.action.IAction;
import ru.inversion.fx.form.action.JInvAction;
import ru.inversion.fx.form.action.JInvEvent.PlaceType;
import ru.inversion.fx.form.action.JInvKeyboardManager;
import ru.inversion.fx.form.action.JInvParallelAction;
import ru.inversion.fx.form.action.SceneKeyManager;
import ru.inversion.fx.form.controls.Controls;
import ru.inversion.fx.form.controls.JInvFEButton;
import ru.inversion.fx.form.controls.JInvTextArea;
import ru.inversion.utils.Pair;

/**
 *
 * @author antonovdi
 */
public class JInvTextAreaSkin extends TextAreaSkin implements ISkinPopulatable {

    private final JInvTextArea textArea;
    private ToolBar toolBar = new ToolBar();
    private static ResourceBundle bundle = ResourceBundle.getBundle("fore");
    private JInvFEButton feButton;
    private boolean disposed;
    private IAction externalEditorAction;
    private List<Pair<Node, IAction>> registeredActions;
    private final EventHandler<ActionEvent> editorButtonHandler = this::editInExtEditor;
    private JInvAction editorButtonAction;
    private final InvalidationListener buttonWidthListener = observable -> {
        double size = getMaxItemWidth();
        for (Node node : toolBar.getItems()) {
            if (node instanceof ButtonBase button) {
                button.setPrefWidth(size);
            }
        }
    };
    private final ListChangeListener<Node> toolbarItemsListener = this::toolbarItemsChanged;


    public JInvFEButton getFeButton() {
        return feButton;
    }

    public JInvTextAreaSkin(JInvTextArea textArea) {
        super(textArea);
        this.textArea = textArea;
        registerChangeListener(textArea.sceneProperty(), observable -> updateExternalEditorAction(textArea.getScene()));
        updateExternalEditorAction(textArea.getScene());

        initToolBar( textArea );
        registerChangeListener(toolBar.visibleProperty(), observable -> textArea.requestLayout());
    }

    private void updateExternalEditorAction(Scene scene) {
        if (registeredActions != null) {
            registeredActions.removeIf(pair -> pair.first == getSkinnable() && pair.second == externalEditorAction);
            registeredActions = null;
        }
        if (scene != null) {
            if (externalEditorAction == null) {
                externalEditorAction = ActionFactory.createAction(ActionFactory.ActionTypeEnum.EDIT_EXTERNAL, event -> {
                    if (getSkinnable().isFocused()) {
                        editInExtEditor(event);
                    }
                });
            }
            JInvKeyboardManager.addAction(getSkinnable(), externalEditorAction);
            registeredActions = SceneKeyManager.getActionMap(scene, PlaceType.FILTER);
        }
    }

    @Override
    public void dispose() {
        if (disposed)
            return;
        disposed = true;
        updateExternalEditorAction(null);
        externalEditorAction = null;
        toolBar.getItems().removeListener(toolbarItemsListener);
        toolBar.getItems().forEach(this::stopTrackingButtonWidth);
        if (editorButtonAction != null && editorButtonAction.getHandler() == editorButtonHandler) {
            editorButtonAction.setHandler(null);
        }
        editorButtonAction = null;
        toolBar.visibleProperty().unbind();
        feButton.visibleProperty().unbind();
        super.dispose();
    }

    public void initToolBar( final JInvTextArea textArea ) {

        toolBar.visibleProperty().bind(textArea.toolBarVisibleProperty());
        toolBar.setOrientation( Orientation.VERTICAL);
        toolBar.setManaged(false);
        toolBar.applyCss();

        //Применение стиля к добавленным кнопкам
        toolBar.getItems().addListener(toolbarItemsListener);

        feButton = new JInvFEButton(textArea);
        feButton.visibleProperty().bind( textArea.buttonFEVisibleProperty() );

        toolBar.getItems().add(feButton);

        Button edButton = ActionFactory.createButton(ActionFactory.ActionTypeEnum.EDIT_EXTERNAL, editorButtonHandler);
        if (edButton.getOnAction() instanceof JInvAction action) {
            editorButtonAction = action;
        }
        edButton.setTooltip(new Tooltip(bundle.getString("TOOLTIP_EDIT_IN_EXTERNAL_EDITOR")));
        registerChangeListener(textArea.editableProperty(), observable -> {
            if (textArea.isEditable()) {
                toolBar.getItems().add(edButton);
            } else {
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
            double width = Math.min(Math.max(0, w), Math.max(0, toolBar.prefWidth(-1)));
            toolBar.resizeRelocate(x + 2, y + 2, Math.max(0, width - 2), Math.max(0, h - 4));
            super.layoutChildren(x + width, y, Math.max(0, w - width), Math.max(0, h));
        } else {
            super.layoutChildren(x, y, Math.max(0, w), Math.max(0, h));
        }
    }

    private void toolbarItemsChanged(ListChangeListener.Change<? extends Node> change) {
        while (change.next()) {
            change.getRemoved().forEach(this::stopTrackingButtonWidth);
            change.getAddedSubList().forEach(this::setButtonStyle);
        }
    }

    private void stopTrackingButtonWidth(Node item) {
        if (item instanceof Button button) {
            button.widthProperty().removeListener(buttonWidthListener);
        }
    }

    private void setButtonStyle(Node item) {
        if (item instanceof Button button) {
            button.widthProperty().addListener(buttonWidthListener);
            button.pseudoClassStateChanged(PseudoClass.getPseudoClass("toolbar-vertical"), true);
            button.setMaxWidth(Double.MAX_VALUE);
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

    public void populateContextMenu(ContextMenu contextMenu) {
        textArea.populateContextMenu(contextMenu);
    }

    @Override
    public ObservableList<MenuItem> getListItemsToPopulateContextMenu() {
        return textArea.getListItemsToPopulateContextMenu();
    }

    @Override
    public void onPopulateContextMenu(Consumer<ContextMenu> action) {
        textArea.onPopulateContextMenu(action);
    }
}
