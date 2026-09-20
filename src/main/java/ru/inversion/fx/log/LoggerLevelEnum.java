/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ru.inversion.fx.log;

import ch.qos.logback.classic.Level;

import java.util.ResourceBundle;

/**
 *
 * @author antonovdi
 */
public enum LoggerLevelEnum {

    TRACE, DEBUG, INFO, WARN, ERROR, OFF, ALL;

    private static final ResourceBundle fore = ResourceBundle.getBundle("fore");

    public String toString() {
        switch (this) {
            case ALL:
                return fore.getString("VSE");
            default:
                return super.toString();
        }
    }

    public static LoggerLevelEnum fromLevel(Level level) {

        if (level.equals(Level.ALL)) {
            return ALL;
        } else {
            return null;
        }
    }

}
