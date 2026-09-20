/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ru.inversion.fx.form.action;

import javafx.scene.Scene;
import javafx.scene.control.Control;
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
    
    static final public String KEYBOARD_CALLBACK_FILTER = "ru.inversion.KeyBoardCallBackFilter";
    static final public String KEYBOARD_CALLBACK_HANDLER = "ru.inversion.KeyBoardCallBackHandler";
    
    
    
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
        Object obj = event.getSource();
        Callback clbk = null;
        
        
        if(obj instanceof Scene) {
            HashMap<KeyCodeCombination,Callback<JInvActionEvent<KeyEvent>,Object>> hm = getCallBackMap((Scene)obj,placeType);
            
            for(KeyCodeCombination kcc:hm.keySet()) {
                if(kcc.match(event)) {
                    Control control = (Control)((Scene)obj).getFocusOwner();
                    clbk = hm.get(kcc);
                    if(clbk != null) {
                        clbk.call(new JInvActionEvent(control,placeType, event));
                    }
                    break;
                }
            }
        }
    }
    
    static public void filterHandler(KeyEvent event) {
        doAction(event,PlaceType.FILTER);
    }
    
    static public void eventHandler(KeyEvent event) {
        doAction(event,PlaceType.HANDLER);
    }
    
    static public HashMap<KeyCodeCombination,Callback<JInvActionEvent<KeyEvent>,Object>> getCallBackMap(Scene scene, PlaceType type) {
        
        HashMap<KeyCodeCombination,Callback<JInvActionEvent<KeyEvent>,Object>> callbackMap = null;
        if(type == PlaceType.FILTER) {
            callbackMap = (HashMap<KeyCodeCombination, Callback<JInvActionEvent<KeyEvent>,Object>>) scene.getProperties().get(KEYBOARD_CALLBACK_FILTER);
            if(callbackMap == null) {
                // установка обработчиков клавиатуры
                scene.addEventFilter(KeyEvent.KEY_PRESSED, (event)->filterHandler(event));
                
                callbackMap = new HashMap<>();
                scene.getProperties().put(KEYBOARD_CALLBACK_FILTER, callbackMap);
            }
        } else {
            callbackMap = (HashMap<KeyCodeCombination, Callback<JInvActionEvent<KeyEvent>,Object>>) scene.getProperties().get(KEYBOARD_CALLBACK_HANDLER);
            if(callbackMap == null) {
                // установка обработчиков клавиатуры
                
                scene.addEventHandler(KeyEvent.KEY_PRESSED, (event)->eventHandler(event));
                callbackMap = new HashMap<>();
                scene.getProperties().put(KEYBOARD_CALLBACK_HANDLER, callbackMap);
            }
        }
        
        return callbackMap;
    }
    
}
