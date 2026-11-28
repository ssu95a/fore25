/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ru.inversion.fx.form.action;

import javafx.scene.Scene;
import ru.inversion.fore.form.action.ActionKeyBinder;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyEvent;
import javafx.util.Callback;
import ru.inversion.fx.form.action.JInvActionEvent.PlaceType;

import java.util.HashMap;

/**
 *
 * @author mik
 */
public class SceneKeyBinder{
    
    static final public String KEYBOARD_CALLBACK_FILTER = ActionKeyBinder.CALLBACK_FILTER;
    static final public String KEYBOARD_CALLBACK_HANDLER = ActionKeyBinder.CALLBACK_HANDLER;
    
    
    
    static public void addAction(Scene scene, KeyCodeCombination kcb, Callback<JInvActionEvent<KeyEvent>,Object> clbk, PlaceType...placeType) {
        PlaceType type = null;
        
        if(placeType != null && placeType.length > 0) {
            type = placeType[0];
        } else {
            type = PlaceType.FILTER;
        }
       
        HashMap<KeyCodeCombination,Callback<JInvActionEvent<KeyEvent>,Object>> actions = getCallBackMap(scene,type);
        actions.put(kcb, clbk);
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

    static public HashMap<KeyCodeCombination,Callback<JInvActionEvent<KeyEvent>,Object>> getCallBackMap(
            Scene scene, PlaceType type) {
        return ActionKeyBinder.legacyCallbacks(scene, type == PlaceType.FILTER);
    }
}
