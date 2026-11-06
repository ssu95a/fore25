/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ru.inversion.fx.form;

/**
 * Энум состояний жизненного цикла контроллера
 * @author antonovdi
 */
public enum LifeCycleStateEnum {
    
    START, 
    BEFORE_INIT,
    INIT,
    AFTER_INIT,
    RUNTIME,
    ON_OK,
    ON_CLOSE,
    ON_CANCEL;
}
