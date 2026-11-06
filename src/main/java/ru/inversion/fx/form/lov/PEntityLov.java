/*
 * ЦАБС Банк XXI Век
 * Компания ИНВЕРСИЯ
 */
package ru.inversion.fx.form.lov;

import ru.inversion.fx.form.lov.JInvEntityLov;

/**
 *
 * @author psh
 * @param <T>
 */
public class PEntityLov<T> 
{    
    private final JInvEntityLov<T, ?> lovObject;
    private final String filterSting;

    public PEntityLov ( JInvEntityLov<T, ?> lovObject, String filterSting ) 
    {
        this.lovObject = lovObject;
        this.filterSting = filterSting;
    }
    
    public String getFilterSting () {
        return filterSting;
    }

    public JInvEntityLov<T, ?> getLovObject () {
        return lovObject;
    }

    private T choiceValue;

    public T getChoiceValue () {
        return choiceValue;
    }

    public void setChoiceValue ( T choiceValue ) {
        this.choiceValue = choiceValue;
    }    
}
