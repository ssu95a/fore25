/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ru.inversion.fx.form.valid;

import javafx.scene.control.Control;

/**
 * Интерфейс калькулятора. Принимает на вход значение, внутри метода calculate делает какие-то вычисления
 * 
 * @author antonovdi
 */
public interface Calculator<T> {
    
    void calculate(T value, Control control);
    
    
    
    
    default void calculate(){
        calculate(null, null);
    }
    
    public default boolean runOnStartForm(){
        return true;
    }
}
