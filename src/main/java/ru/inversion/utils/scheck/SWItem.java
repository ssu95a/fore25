package ru.inversion.utils.scheck;

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */


import java.util.regex.Pattern;

/**
 *
 * @author antonovdi
 */
public class SWItem implements IJInvSWItem {

    private String id;
    private Pattern regExp;
    private int groupRegExp;
    private String description;
    private String errorDescription;

    public SWItem(String id, Pattern regExp, int groupRegExp, String description, String errorDescription) {
        this.id = id;
        this.regExp = regExp;
        this.groupRegExp = groupRegExp;
        this.description = description;
        this.errorDescription = errorDescription;
    }

    public String getId() {
        return id;
    }

    public Pattern getRegExp() {
        return regExp;
    }

    public int getGroupRegExp(){
        return groupRegExp;
    }

    public String getErrorDescription() {

        return errorDescription;
    }

    public String getDescription() {
        return description;
    }

    @Override
    public String toString() {
        return super.toString(); //To change body of generated methods, choose Tools | Templates.
    }


}
