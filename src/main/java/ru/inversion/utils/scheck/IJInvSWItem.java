/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ru.inversion.utils.scheck;

import java.util.regex.Pattern;

/**
 *
 * @author antonovdi
 */
public interface IJInvSWItem {
    
    public Pattern getRegExp();
    
    public String getId();
    
    public String getErrorDescription();
    
    public String getDescription();
    
    public int getGroupRegExp();
}
