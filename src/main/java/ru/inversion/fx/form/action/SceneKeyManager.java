/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ru.inversion.fx.form.action;

import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.input.KeyEvent;
import org.slf4j.Logger;
import ru.inversion.fore.form.action.ActionKeyBinder;
import ru.inversion.fx.app.BaseApp;
import ru.inversion.fx.form.action.JInvEvent.PlaceType;
import ru.inversion.utils.Pair;

import java.util.Collections;
import java.util.List;

import static java.lang.invoke.MethodHandles.lookup;
import static org.slf4j.LoggerFactory.getLogger;

/**
 *
 * @author antonovdi
 */
public class SceneKeyManager {
    private final static Logger logger = getLogger(lookup().lookupClass());
    static final public String KEYBOARD_CALLBACK_FILTER = ActionKeyBinder.ACTION_FILTER;
    static final public String KEYBOARD_CALLBACK_HANDLER = ActionKeyBinder.ACTION_HANDLER;

    public static void addAction(Scene scene, IAction action, PlaceType type) {
        addAction(scene.getRoot(), action, type);
    }

    public static void addAction(Node node, IAction action, PlaceType placeType) {

        if (node == null) {
            return;
        }

        if (placeType == null) {
            placeType = PlaceType.FILTER;
        }

        Scene scene = getSceneFromNode(node);
        List<Pair<Node, IAction>> actions = getActionMap(scene, placeType);

        if (action instanceof JInvAction){

            // Если по каким то причинам кнопка не стоит, но тип есть и на тип настроена кнопка, то берем с нее
            if ((action.getHotKey() == null || action.getHotKey().isEmpty())
                && action.getActionType() != null
                && action.getActionType().getHotKey() != null
                && !action.getActionType().getHotKey().isEmpty()) {

                ((JInvAction) action).setHotKey(action.getActionType().getHotKey());
            }

            if ( action.getHotKey() == null ){
                ((JInvAction)action).setHotKey( Collections.emptyList() );
            }
        }

        actions.add(new Pair<>(node, action));
    }

    static protected void doAction(KeyEvent event, PlaceType placeType) {
        ActionKeyBinder.dispatchRegistered(event, placeType == PlaceType.FILTER);
    }

    static public void filterHandler(KeyEvent event) {
        doAction(event, PlaceType.FILTER);
    }

    static public void eventHandler(KeyEvent event) {
        doAction(event, PlaceType.HANDLER);
    }

    static public List<Pair<Node, IAction>> getActionMap(Scene scene, PlaceType type) {
        return ActionKeyBinder.legacyActions(scene, type == PlaceType.FILTER);
    }
    private static Scene getSceneFromNode(Node node) {

        Scene scene = node.getScene();

        if( scene == null )
        {
            if ( BaseApp.APP().getMainFrame() != null )
                scene =  BaseApp.APP().getMainFrame().getScene();

            if( scene == null )
            {
                Parent p = node.getParent();

                while( p != null ) {

                    scene = p.getScene();
                    if( scene != null )
                        break;

                    p = p.getParent();
                }

            }//end if
        }

        if( scene == null )
            scene =  new Scene( (Parent) node );

        return scene;
    }
}
