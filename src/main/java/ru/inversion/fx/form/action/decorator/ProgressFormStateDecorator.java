/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ru.inversion.fx.form.action.decorator;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.Event;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextInputControl;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import ru.inversion.fx.form.IFormStateListener;
import ru.inversion.fx.form.StateEnum;
import ru.inversion.fx.form.ViewContext;
import ru.inversion.utils.S;

import static javafx.event.Event.ANY;

/**
 * @author antonovdi
 */
public class ProgressFormStateDecorator {

    public static final String PROPERTY_DECORATOR = "ru.inversion.wait_decorator";

    private Parent rootPane;
    private final ViewContext vc;
    private final IFormStateListener stateListener;
    private final StackPane sp = new StackPane();

    private Node focusNode = null;

    public ProgressFormStateDecorator( IFormStateListener stateListener, ViewContext vc) {
        this.vc = vc;
        this.stateListener = stateListener;
        sp.getProperties().put( PROPERTY_DECORATOR, true );
        init();
    }

    /** */
    public void changeDecorator( StateEnum newValue ) {

        if( newValue == StateEnum.WAIT )
        {
            // Если состояние не активное.
            // Ставим прозрачную панель, чтобы пользователь ничего не нажимал
            final Pane glassPane = new Pane();
            glassPane.setBackground( new Background( new BackgroundFill( Color.WHITE, CornerRadii.EMPTY, Insets.EMPTY ) ) );
            glassPane.setOpacity   ( 0.6 );

            glassPane.setMouseTransparent(false);
            glassPane.setPickOnBounds( true );
            glassPane.addEventFilter ( Event.ANY, Event::consume );

            // просто крутящегося червяка
            final ProgressIndicator pi = new ProgressIndicator();
            final VBox wormPane = new VBox();

            if (
                stateListener.stateTextProperty() != null
            )
            {
                //wormPane with text
                final Label label = new Label( stateListener.getStateText() );
                stateListener
                    .stateTextProperty()
                        .addListener( (v,o,n) -> {
                            if(S.isNullOrEmpty(n) )
                            {
                                Platform.runLater( () -> wormPane.getChildren().remove(label) );
                            }
                            else
                            {
                                Platform.runLater(
                                    () -> {

                                        if( wormPane.getChildren().get(0) != label )
                                            wormPane.getChildren().add(0, label);

                                        label.setText( n );
                                    }
                                );
                            }
                } );

                label.setFont( Font.font( Font.getDefault().getFamily(), 24 ) );

                if( S.isNullOrEmpty( stateListener.getStateText() ) )
                    wormPane.getChildren().addAll( pi );
                else
                    wormPane.getChildren().addAll( label, pi );
            }
            else
            {
                wormPane.getChildren().add( pi );
            }

            final Node fn = rootPane.getScene().focusOwnerProperty().get();

            wormPane.setAlignment(Pos.CENTER);
            pi.setPrefSize( 50.0, 50.0 );
            pi.setMinSize ( 50.0, 50.0 );

            sp.getChildren().addAll( rootPane, glassPane, wormPane );

            vc.getStage().getScene().setRoot(sp);
            vc.getStage().getScene().setCursor(Cursor.WAIT);

            if( fn instanceof TextInputControl )
            {
                pi.requestFocus();
                pi.focusedProperty().addListener(new ChangeListener< Boolean >() {
                    @Override
                    public void changed( ObservableValue< ? extends Boolean > observable, Boolean oldValue, Boolean newValue ) {
                        if( !newValue )
                            pi.requestFocus();
                    }
                });

                focusNode = fn;
            }
        }
        else if (newValue == StateEnum.ACTIVE)
        {
            sp.getChildren().clear();
            vc.getStage().getScene().setCursor(Cursor.DEFAULT);
            vc.getStage().getScene().setRoot  (rootPane);
            if( focusNode == null )
                ;//rootPane.requestFocus();
            else
                focusNode.requestFocus();

            focusNode = null;
        }
    }

    /** */
    private void init( )
    {
        final Parent parent = vc.getStage().getScene().getRoot();

        // Отображается неправильно, если максимальный размер задан как минус бесконечность
        if( parent instanceof Pane )
        {
            final Pane pane = (Pane)parent;
            if( pane.getMaxHeight() == Double.NEGATIVE_INFINITY ) pane.setMaxHeight( Double.MAX_VALUE );
            if( pane.getMaxWidth()  == Double.NEGATIVE_INFINITY ) pane.setMaxWidth ( Double.MAX_VALUE );
            if( pane.getMinHeight() == Double.NEGATIVE_INFINITY ) pane.setMinHeight( 0 );
            if( pane.getMinWidth()  == Double.NEGATIVE_INFINITY ) pane.setMinWidth ( 0 );

        }

        rootPane = parent;

        stateListener.stateProperty().addListener((ObservableValue<? extends StateEnum> observable, StateEnum oldValue, StateEnum newValue) -> {
            changeDecorator(newValue);
        });

    }

    public Parent getRootPane() { return rootPane; }
}
