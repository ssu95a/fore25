/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ru.inversion.fx.app.sec;

/**
 *
 * @author antonovdi
 */
public enum SecurityStrategyEnum {
    
    /**
     * Показывается ошибка о том что прав недостаточно при непосредственном действии
     */
    ERROR_ON_ACTION,

    /**
     * Компонент дизаблируется в случае недостаточности прав
     */
    DISABLE,
    
    /**
     * Компонент скрывается в случае недостаточности прав
     */
    HIDE;
}
