/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ru.inversion.fx.form.controls.skin;

import com.sun.javafx.scene.control.skin.TextFieldSkin;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.css.PseudoClass;
import javafx.scene.Cursor;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextField;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import ru.inversion.fx.form.controls.JInvFEButton;
import ru.inversion.fx.form.controls.JInvTextField;
import ru.inversion.fx.form.controls.JInvWrapButton;
import ru.inversion.icons.IconFactory;
import ru.inversion.icons.enums.FontAwesome;
import ru.inversion.icons.enums.IconSize;

import java.util.function.Consumer;

import static javafx.scene.control.TextField.DEFAULT_PREF_COLUMN_COUNT;

/**
 *
 * @author antonovdi
 */
public class JInvTextFieldButtonSkin extends TextFieldSkin implements ISkinPopulatable {

    private final TextField textField;
    private JInvWrapButton wrapButton;
    private JInvFEButton overflowButton;

    final static private PseudoClass focusedButton = PseudoClass.getPseudoClass("focused");

    //Список элементов меню PopulateContextMenu
    private final ObservableList<MenuItem> listPopulateContextMenu = FXCollections.observableArrayList();
    private Consumer<ContextMenu> onPopulateContextMenu;

    public JInvTextFieldButtonSkin(TextField textField) {
        super(textField);
        this.textField = textField;

        overflowButton = new JInvFEButton();
        overflowButton.setGraphic(
                IconFactory.getLabel( FontAwesome.fa_ellipsis_h, IconSize.SMALL, Color.DODGERBLUE.deriveColor(1,1,1,0.5) ));
        overflowButton.setTextField(textField);
        overflowButton.setManaged(false);
        overflowButton.setFocusTraversable(false);
        overflowButton.getStyleClass().add("transparentButton");
        overflowButton.setTooltip(null);
        overflowButton.setMaxWidth(12);

        wrapButton = new JInvWrapButton();
        wrapButton.setStyle("");
        wrapButton.getStyleClass().add("button_in_textfield");
        wrapButton.setCursor(Cursor.DEFAULT);
        wrapButton.setVisible(false);
        wrapButton.setManaged(false);
        wrapButton.setFocusTraversable(false);
        wrapButton.visibleProperty().addListener((ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) -> {
            textField.requestLayout();
        });

        if (textField instanceof JInvTextField) {
            ((JInvTextField) textField)
                .externalButtonProperty()
                .addListener((v,o,n) -> {
                    checkOverflow();
                });
        }

        textField.focusedProperty().addListener((ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) -> {
            if (wrapButton.isVisible()) {
                if (newValue) {
                    wrapButton.pseudoClassStateChanged(focusedButton, true);
                } else {
                    wrapButton.pseudoClassStateChanged(focusedButton, false);
                }
            }
        });

        getChildren().addAll(overflowButton,wrapButton);
        initOverflowListener();
    }

    private void initOverflowListener() {
        textField.textProperty().addListener((ObservableValue<? extends String> observable, String oldValue, String newValue) -> {
            checkOverflow();
        });

        textField.widthProperty().addListener( ( observableValue, oldWidth, newWidth ) -> {
//            logger.info("textField '{}': width changed {}->{}",this.getId(),oldWidth,newWidth);
            checkOverflow();
        } );

    }

    private void checkOverflow() {
        boolean showOverflowButton = false;
        if (textField instanceof JInvTextField){
            JInvTextField jInvTextField = (JInvTextField) this.textField;
            showOverflowButton = jInvTextField.checkExcessTextLength()
                              && jInvTextField.externalButtonProperty().get() == null;
        }

        overflowButton.setVisible(showOverflowButton);
    }

    @Override protected void layoutChildren(double x, double y, double w, double h) {
        final double fullHeight = h + snappedTopInset() + snappedBottomInset();

        final double rightWidth = wrapButton.isVisible() ? snapSize(wrapButton.prefWidth(fullHeight)) : 0.0;

        final double textFieldStartX = snapPosition(x);
        final double textFieldWidth = w - snapSize(rightWidth);

        if (overflowButton.isVisible()){
            overflowButton.relocate(w+overflowButton.getMaxWidth(),fullHeight/2);
        }

        if (wrapButton.isVisible()) {
            final double rightStartX = w - rightWidth + snappedLeftInset() + snappedRightInset();
            wrapButton.resizeRelocate(rightStartX, 0, rightWidth, fullHeight);
            super.layoutChildren(textFieldStartX, y, textFieldWidth, h);
        } else {
            super.layoutChildren(x, y, w, h);
        }
    }

    @Override
    protected double computePrefWidth(double h, double topInset, double rightInset, double bottomInset, double leftInset) {
        final double pw = super.computePrefWidth(h, topInset, rightInset, bottomInset, leftInset);

        boolean hasPrefWidth = (textField.getPrefWidth() != Region.USE_COMPUTED_SIZE && textField.getPrefWidth() != Region.USE_PREF_SIZE)
                              || (textField.getMaxWidth() != Region.USE_COMPUTED_SIZE && textField.getMaxWidth() != Region.USE_PREF_SIZE);
        boolean hasPrefColumns = textField.getPrefColumnCount() != DEFAULT_PREF_COLUMN_COUNT;

        if (wrapButton.isVisible() && (hasPrefColumns || !hasPrefWidth)) {
            final double rightWidth = snapSize(wrapButton.prefWidth(h));
            return pw + rightWidth; //поле увеличивается на ширину кнопки
        }
        return pw; //кнопка использует пространство текстового поля и не влияет на общую ширину
    }

    @Override
    protected double computePrefHeight(double w, double topInset, double rightInset, double bottomInset, double leftInset) {
        final double ph = super.computePrefHeight(w, topInset, rightInset, bottomInset, leftInset);
        if (wrapButton.isVisible()) {
            final double buttonHeight = snapSize(wrapButton.prefHeight(-1));
            return Math.max(ph, buttonHeight);
        }
        return ph;
    }


    public JInvWrapButton getButton() {
        return wrapButton;
    }

    /**
     *
     * @param contextMenu
     */
    @Override
    public void populateContextMenu(ContextMenu contextMenu) {
        super.populateContextMenu(contextMenu);
        if ( !listPopulateContextMenu.isEmpty() ) {
            listPopulateContextMenu.forEach((MenuItem items) -> contextMenu.getItems().add(0, items));
        }
        if ( onPopulateContextMenu != null ){
            onPopulateContextMenu.accept( contextMenu );
        }
    }

    public ObservableList<MenuItem> getListItemsToPopulateContextMenu() {
        return listPopulateContextMenu;
    }

    @Override
    public void onPopulateContextMenu( Consumer<ContextMenu> action ) {
        onPopulateContextMenu = action;
    }
}
