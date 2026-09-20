/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ru.inversion.fx.app.service.view;

import java.util.ResourceBundle;

/**
 * Энум способов отображения иконки на кнопке.
 * @author antonovdi
 */
public enum ButtonIconView {
    
    
    /**
     * Показывать иконку
     */
    ICON,
    
    /**
     * Показывать иконку и текст
     */
    ICON_WITH_TEXT,
    
    /**
     * Показывать только текст
     */
    TEXT;
    
    private static ResourceBundle bundle = ResourceBundle.getBundle("fore");

    @Override
    public String toString() {
        return bundle.getString("BUTTON_ICON_VIEW_" + this.name());
    }

}
