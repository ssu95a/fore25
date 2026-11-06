/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ru.inversion.fx.log;

import java.util.ResourceBundle;

/**
 *
 * @author antonovdi
 */
public enum LoggerTimingEnum {

    MOUNTH, DAYS, HOURS, MINUTES, SECONDS;

    private static final ResourceBundle fore = ResourceBundle.getBundle("fore");

//          values[0] = new ComboBoxPropertyEditor.Value("yy-MM", "месяцы");
//        values[1] = new ComboBoxPropertyEditor.Value("yy-MM-dd", "дни");
//        values[2] = new ComboBoxPropertyEditor.Value("yy-MM-dd-HH", "часы");
//        values[3] = new ComboBoxPropertyEditor.Value("yy-MM-dd-HH-mm", "минуты");
//        values[4] = new ComboBoxPropertyEditor.Value("yy-MM-dd-HH-mm-ss", "секунды");
    @Override
    public String toString() {

        switch (this) {
            case MOUNTH:
                return fore.getString("MESYACY");
            case DAYS:
                return fore.getString("DNI");
            case HOURS:
                return fore.getString("CHASY");
            case MINUTES:
                return fore.getString("MINUTY");
            case SECONDS:
                return fore.getString("SEKUNDY");
            default:
                return null;
        }
    }

    public String toLogString() {
        switch (this) {
            case MOUNTH:
                return "yy-MM";
            case DAYS:
                return "yy-MM-dd";
            case HOURS:
                return "yy-MM-dd-HH";
            case MINUTES:
                return "yy-MM-dd-HH-mm";
            case SECONDS:
                return "yy-MM-dd-HH-mm-ss";
            default:
                return null;
        }
    }

    public static LoggerTimingEnum fromLogString(String st) {
        switch (st) {
            case "yy-MM":
                return MOUNTH;
            case "yy-MM-dd":
                return DAYS;
            case "yy-MM-dd-HH":
                return HOURS;
            case "yy-MM-dd-HH-mm":
                return MINUTES;
            case "yy-MM-dd-HH-mm-ss":
                return SECONDS;
            default:
                return null;
        }
    }
}
