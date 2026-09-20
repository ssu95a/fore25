package ru.inversion.fx.form.controls.filter.impl;

import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import ru.inversion.fx.form.action.JInvKeyboardManager;
import ru.inversion.fx.form.controls.IStateControl;
import ru.inversion.fx.form.controls.JInvTextField;
import ru.inversion.fx.form.controls.skin.ISkinPopulatable;

/**
 * Класс
 *
 * @author perov
 * @version 1.0.0
 */
abstract class ParamCellTextField extends JInvTextField {

    private String oldValue;

    public ParamCellTextField() {
        this(null);
    }

    ParamCellTextField(String string) {
        super(string);
        focusOnControl.set(false);
    }

    @Override
    protected void initKeyBoard() {

        /*
        this.setOnKeyReleased((KeyEvent event) -> {
            if (event.getCode() == KeyCode.ENTER) {
                commit();
            } else if (event.getCode() == KeyCode.ESCAPE) {
                cancel();
            }
        });
        */

        // Отслеживаем выход из редактирования ячейки таблицы, а затем commit'им изменения
        focusedProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue) {
                this.oldValue = getText();
                commit();
            }
        });

        addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.ENTER) {
                commit();
                event.consume();
            } else if (event.getCode() == KeyCode.ESCAPE) {
                cancel();
            }
        });

        addEventFilter(KeyEvent.KEY_RELEASED, (KeyEvent event) -> {
            if (event.getCode() == KeyCode.F9) {
                if (isEditable() && !((IStateControl) this).stateProperty().get().equals(State.ERROR)) {

                    if (getController() != null && getController().getValidMan() != null) {
                        getController().getValidMan().setFlagOnShowChoiceDialog(true);
                    }
                    event.consume();
                    showLOV(null);
                } else {
                    JInvKeyboardManager.fireForwardEvent(null, this);
                }
            }
        });
    }

    public abstract void commit();

    public abstract void cancel();

    void addItemToContextMenu( final MenuItem item) {
        if (getSkin() instanceof ISkinPopulatable) {

            if (( (ISkinPopulatable) getSkin() ).getListItemsToPopulateContextMenu().isEmpty()) {
                ( (ISkinPopulatable) getSkin() ).getListItemsToPopulateContextMenu().add(new SeparatorMenuItem());
            }

            if (!( (ISkinPopulatable) getSkin() ).getListItemsToPopulateContextMenu().contains(item) ) {
                ( (ISkinPopulatable) getSkin() ).getListItemsToPopulateContextMenu().add(item);
            }
        }
    }

}
