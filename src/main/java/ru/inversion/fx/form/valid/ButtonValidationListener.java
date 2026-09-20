/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ru.inversion.fx.form.valid;

import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Control;
import javafx.scene.control.TextInputControl;
import javafx.scene.input.MouseEvent;
import ru.inversion.fx.form.controls.*;
import ru.inversion.fx.form.lov.JInvLOVButton;
import ru.inversion.utils.U;

import static ru.inversion.fx.form.controls.IStateControl.State.ERROR;

/**
 *
 * @author antonovdi
 */
public class ButtonValidationListener implements EventHandler<MouseEvent> {

    private final ValidMan validMan;

    public ButtonValidationListener(ValidMan validMan) {
        this.validMan = validMan;
    }

    @Override
    public void handle(MouseEvent event) {

        // Для того чтобы валидатор сработал до кнопки и кнопка не нажалась приходится отлавливать события мыши
        // причем сразу все три и поглощать, и ставить в validMan контрол для того, чтобы вызвать его, если валидация корретная
        // if (event.getEventType().equals(MouseEvent.MOUSE_CLICKED) || event.getEventType().equals(MouseEvent.MOUSE_PRESSED) || event.getEventType().equals(MouseEvent.MOUSE_RELEASED)) {

        if( U.in( event.getEventType(), MouseEvent.MOUSE_CLICKED, MouseEvent.MOUSE_PRESSED, MouseEvent.MOUSE_RELEASED ) ) {

            Node node = event.getPickResult().getIntersectedNode();

            if( node != null && node.getScene() != null ) {

                Node focusNode = node.getScene().getFocusOwner();

                Controls.getParentStreamOfControl(node)
                    .filter((Parent t) -> t instanceof IJInvControl && t instanceof Control && !t.equals(focusNode)).findAny()
                    .ifPresent((Parent t) -> {

                        Control control = (Control) t;

                        if (control != focusNode && control.getId() == null
                            || !control.getId().equalsIgnoreCase("btCancel")) {

                            if (validMan.isControlInValidation(focusNode)
                                && (control.getId() == null
                                || !control.getId().equalsIgnoreCase("btCancel"))) {
                                // Если мы нажимае на Lov button
                                // и при этом стоим на
                                // привязанном к нему
                                // текстфилде, то не запускаем
                                // валидацию

                                JInvLOVButton lovButton = null;

                                if (control instanceof JInvLOVButton) {
                                    lovButton = (JInvLOVButton) control;
                                } else if (control instanceof JInvWrapButton) {
                                    JInvWrapButton wrapButton = (JInvWrapButton) control;
                                    JInvButton innerButton = wrapButton.getInnerButton();
                                    if (innerButton != null && innerButton instanceof JInvLOVButton) {
                                        lovButton = (JInvLOVButton) innerButton;
                                    }
                                }

                                if (lovButton != null) {
                                    TextInputControl textField = lovButton.getTextField();
                                    if (textField != null && textField.equals(focusNode)) {
                                        validMan.setFlagOnShowChoiceDialog(true);
                                    }
                                }

                                if ( ( control instanceof Button || control instanceof JInvCalendar )
                                    //Исправление нажатий на кнопки из ридонли контрола в фокусе:
                                        && !isReadOnlyNode( focusNode ) ) {
                                    event.consume();
                                    validMan.setControlAfterValidation(control, event);
                                    control.requestFocus();
                                    //Не оставляем фокус на кнопках
                                    if (control instanceof Button) {
                                        focusNode.requestFocus();
                                    }
                                }

                                // Если стояли на контроле,
                                // который не валидируется, но
                                // имеет внутреннее ошибочное
                                // состояние, то не разрешаем
                                // уходить
                                // с него
                            } else if (focusNode instanceof IStateControl && ((IStateControl) focusNode)
                                .stateProperty().get().equals(ERROR)) {
                                event.consume();
                                t.requestFocus();
                            } else if (focusNode instanceof JInvCalendar) {
                                //Нужно выполнять в момент нажатия кнопки, конвертер не отрабатывает, если кнопка не фокусируемая
                                if (event.getEventType().equals(MouseEvent.MOUSE_PRESSED)) {
                                    ((JInvCalendar) focusNode).forceCommitValue();
//                                    System.out.println("listener");
                                }
                            }
                            else if (focusNode instanceof JInvComboBox) {
                                if (event.getEventType().equals(MouseEvent.MOUSE_PRESSED)) {
                                    ((JInvComboBox) focusNode).forceCommitValue();
    //                                    System.out.println("listener");
                                }
                            }
                        }
                    });
            }
        }
    }

    private static boolean isReadOnlyNode( final Node focusNode ) {
        return focusNode instanceof IReadOnlyControl && ( (IReadOnlyControl) focusNode ).isReadOnly();
    }
}
