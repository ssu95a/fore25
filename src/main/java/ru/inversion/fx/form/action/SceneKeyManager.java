/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ru.inversion.fx.form.action;

import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Control;
import javafx.scene.input.KeyEvent;
import org.slf4j.Logger;
import ru.inversion.dataset.fx.DSFXAdapter;
import ru.inversion.fx.app.BaseApp;
import ru.inversion.fx.form.action.JInvEvent.PlaceType;
import ru.inversion.fx.form.controls.Controls;
import ru.inversion.fx.form.controls.IJInvControl;
import ru.inversion.utils.Pair;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;

import static java.lang.invoke.MethodHandles.lookup;
import static org.slf4j.LoggerFactory.getLogger;

/**
 *
 * @author antonovdi
 */
public class SceneKeyManager {
    private final static Logger logger = getLogger(lookup().lookupClass());
    static final public String KEYBOARD_CALLBACK_FILTER = "ru.inversion.ActionMapFilter";
    static final public String KEYBOARD_CALLBACK_HANDLER = "ru.inversion.ActionMapHandler";

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

    private static IAction getActionRecursive(Node node, KeyEvent event, List<Pair<Node, IAction>> map) {

        IAction action = map.stream()
                .filter( ( Pair<Node, IAction> t ) -> t.first.equals( node ) )
                .map( ( Pair<Node, IAction> t ) -> t.second )
                .filter(actionFilter(event))
                .findFirst()
                .orElse( null );

        if( action == null )
        {
            if( node.getProperties().getOrDefault( Controls.CONTROL_PARENT, null) != null )
            {
                return getActionRecursive( (Node) node.getProperties().get(Controls.CONTROL_PARENT), event, map );
            }
            else if( node.getParent() != null )
            {
                return getActionRecursive( node.getParent(), event, map );
            }
            else
            {
                return null;
            }
        }
        else
        {
            return action;
        }
    }

    /**
     Для экшнов, не зависящих от конкретного контрола
     */
    private static IAction getActionNonRecursive( final KeyEvent event, final List<Pair<Node, IAction>> map ) {
        return map.stream()
                .map( ( Pair<Node, IAction> t ) -> t.second )
                .filter(actionFilter(event))
                .findFirst()
                .orElse( null );
    }

    private static Predicate<IAction> actionFilter(KeyEvent event) {
        return (IAction t) -> {
            return t.getHotKey()
                    .stream()
                    .anyMatch(keyCodeCombination -> keyCodeCombination != null && keyCodeCombination.match(event))
                    && t.getKeyEventType() == event.getEventType();
        };
    }

    static protected void doAction(KeyEvent event, PlaceType placeType) {
        if (event.isConsumed()){
            return;
        }
//        logger.trace("doAction:\n{}/{}", event, placeType);
        Object source = event.getSource();

        if (source instanceof Scene)
        {
            List<Pair<Node, IAction>> map = getActionMap((Scene) source, placeType);

            if (((Scene) source).getFocusOwner() instanceof Control) {

                Node control = (Control) ((Scene) source).getFocusOwner();

                IAction action = getActionRecursive(control, event, map);

                if ( action == null )
                {
                    if (control instanceof IJInvControl)
                    {
                        DSFXAdapter<Object> dsfx = ((IJInvControl) control).getDataSetAdapter();
                        if (dsfx != null && dsfx.getTable() != null){
                            action = getActionRecursive(dsfx.getTable(), event, map);
                        }
                    }
                }

                if( action == null ){
                    action = getActionNonRecursive( event, map );
                }
                if (action != null && action.isEnabled() && action.isEnabledBySecurity()) {
//                    logger.trace("eventType={}, actionKeyEventType={}",event.getEventType(),action.getKeyEventType());
                    if (event.getEventType().equals(action.getKeyEventType())){
                        action.handle(new JInvEvent(control, placeType, event));
                        event.consume();
//                        logger.trace("consumed event {}", event);
                    }
                }
            }
        }
    }

    static public void filterHandler(KeyEvent event) {
        doAction(event, PlaceType.FILTER);
    }

    static public void eventHandler(KeyEvent event) {
        doAction(event, PlaceType.HANDLER);
    }

    static public List<Pair<Node, IAction>> getActionMap(Scene scene, PlaceType type) {

        List<Pair<Node, IAction>> result = null;
        if (type == PlaceType.FILTER) {
            result = (List<Pair<Node, IAction>>) scene.getProperties().get(KEYBOARD_CALLBACK_FILTER);
            if (result == null) {
//                // установка обработчиков клавиатуры
                scene.addEventFilter(KeyEvent.ANY, (event) -> filterHandler(event));
                scene.addEventFilter(KeyEvent.KEY_PRESSED, (event) -> filterHandler(event));
                scene.addEventFilter(KeyEvent.KEY_RELEASED, (event) -> filterHandler(event));
                scene.addEventFilter(KeyEvent.KEY_TYPED, (event) -> filterHandler(event));
                result = new ArrayList<>();
                scene.getProperties().put(KEYBOARD_CALLBACK_FILTER, result);
            }
        } else {
            result = (List<Pair<Node, IAction>>) scene.getProperties().get(KEYBOARD_CALLBACK_HANDLER);
            if (result == null) {
//                // установка обработчиков клавиатуры
                scene.addEventHandler(KeyEvent.ANY, (event) -> eventHandler(event));
                scene.addEventHandler(KeyEvent.KEY_PRESSED, (event) -> eventHandler(event));
                scene.addEventHandler(KeyEvent.KEY_RELEASED, (event) -> eventHandler(event));
                scene.addEventHandler(KeyEvent.KEY_TYPED, (event) -> eventHandler(event));
                result = new ArrayList<>();
                scene.getProperties().put(KEYBOARD_CALLBACK_HANDLER, result);
            }
        }
        return result;
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
