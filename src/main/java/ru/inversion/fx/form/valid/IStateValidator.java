/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ru.inversion.fx.form.valid;

/**
 *
 * @author antonovdi
 */
public interface IStateValidator<T> extends Validator<T> {

    public void setDisable( boolean val );

    public boolean isDisabled( );
}
