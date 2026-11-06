/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ru.inversion.fx.form.action;

import javafx.scene.control.Control;

/**
 *
 * @author antonovdi
 */
public class JInvActionEvent<T> {

    protected Control control;
    protected PlaceType placeType = PlaceType.FILTER;
    protected T event;

    public JInvActionEvent(Control control, PlaceType placeType, T event) {
        this.control = control;
        this.placeType = placeType;
        this.event = event;
    }

    public T getEvent() {
        return event;
    }

    public void setEvent(T event) {
        this.event = event;
    }

    public Control getControl() {
        return control;
    }

    public void setControl(Control control) {
        this.control = control;
    }

    public PlaceType getPlaceType() {
        return placeType;
    }

    public void setPlaceType(PlaceType placeType) {
        this.placeType = placeType;
    }

    static public enum PlaceType {
        FILTER,
        HANDLER
    }
}
