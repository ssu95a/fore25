/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ru.inversion.fx.form.valid;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.scene.AccessibleAttribute;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.controlsfx.control.PopOver;
import org.slf4j.Logger;
import ru.inversion.fx.app.sound.SoundPlayer;
import ru.inversion.fx.app.sound.SoundType;
import ru.inversion.fx.form.Alerts;
import ru.inversion.fx.form.controls.Controls;
import ru.inversion.utils.S;

import java.lang.invoke.MethodHandles;

import static org.slf4j.LoggerFactory.getLogger;
import static ru.inversion.fx.form.controls.Controls.scrollToNode;

/**
 *
 * @author antonovdi
 */
public class JInvValidTooltip {

    private static boolean showed;
    public static PopOver currentToolTip;

    private final static Logger logger = getLogger( MethodHandles.lookup().lookupClass() );

    /** Скрываем тултип, если он ещё не скрыт */
    public static void closeValidTooltip() {
        if (isShowed()) {
            currentToolTip.hide();
        }
    }

    public static void showValidTooltip(Control control, String text) {
//        logger.info("showValidTooltip");
        closeValidTooltip();

        if( !control.isVisible() || control.getScene() == null || !control.getScene().getRoot().isVisible()) {

            String details = null;

            final Label l = (Label)control.queryAccessibleAttribute(AccessibleAttribute.LABELED_BY);
            if( l != null )
                details = l.getText();

            if( !S.isNullOrEmpty(details) )
                details = details + " = " + S.nvl( Controls.getValue( control ) );
            else
                details = S.nvl( Controls.getValue( control ) );

            Alerts.error( control.getParent(), null, text, details );

            return;
        }
        SoundPlayer.playSound( SoundType.ERROR );

        PopOver pop = new PopOver();
        currentToolTip = pop;
        Label errorIcon = new Label("\uf05e");
        errorIcon.setStyle("    -fx-font-family:'FontAwesome';\n"
            + "    -fx-text-fill:#c91e1e;\n"
            + "    -fx-font-size:32;");
        Label errorLabel = new Label(text);

        HBox box = new HBox(errorIcon, errorLabel);
        box.setStyle("-fx-padding:5");
        box.setAlignment(Pos.CENTER);
        box.setSpacing(10);
        box.setFillHeight(true);

        Timeline timeline = new Timeline();
        ChangeListener<Boolean> focusChangeListener = (v, o, n) -> {
            if (!n) {
                timeline.play();
            }
        };
        //Если с контрола уходит фокус и не возвращается через 500мс, закрываем напоминание
        EventHandler<ActionEvent> afterDelay = (ActionEvent ae) -> {
            if (!control.isFocused()) {
//                logger.trace("Closing tooltip due to not being focused after 500ms");
                closeValidTooltip();
                control.focusedProperty().removeListener(focusChangeListener);
            }
        };
        timeline.getKeyFrames().add(new KeyFrame(Duration.millis(500)));
        timeline.setOnFinished(afterDelay);
        control.focusedProperty().addListener(focusChangeListener);

//         Если выходим за границы окна, то закрываем напоминание
        PopOverListener windowFocusListener = new PopOverListener(control);
        if (control.getScene().getWindow() != null && control.getScene().getWindow() instanceof Stage) {
            control.getScene().getWindow().focusedProperty().addListener(windowFocusListener);
        }

        pop.setOnHidden(e-> setShowed(false));
        pop.setStyle(" -fx-background-color:white;");
        pop.setArrowLocation(PopOver.ArrowLocation.BOTTOM_CENTER);
        pop.setContentNode(box);
        pop.setFadeOutDuration(Duration.ZERO);

        pop.setAutoHide(true);
        pop.setHideOnEscape(true);

        Platform.runLater(() -> {
            if (control.isVisible() && control.getScene().getWindow().focusedProperty().get())
            {
                // Если ошибка произошла на компоненте,
                // расположенном на другой вкладке, то активизируем вкладку
                ValidViewDecorator.enableTabPane(control);
                // Скролируем ScrollPane до компонента,
                // если она есть
                scrollToNode(control);

                pop.show(control);
                setShowed(true);
            }
        });
    }

    public static boolean isShowed() {
        return showed;
    }

    private static void setShowed(boolean showed) {
        JInvValidTooltip.showed = showed;
    }
}
