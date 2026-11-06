/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ru.inversion.fx.form.controls.renderer;

/**
 * Информация о произошедшем изменении в ячейке
 * @author mik, foma
 */
public class CellEditorInfo<T,S> {
    protected T entity;
    protected String fieldName;
    protected S oldValue;
    protected S newValue;

    public CellEditorInfo(T entity, String fieldName, S oldValue, S newValue) {

        this.entity = entity;
        this.fieldName = fieldName;
        this.oldValue = oldValue;
        this.newValue = newValue;

    }

    public CellEditorInfo() {
    }

    public T getEntity() {
        return entity;
    }

    public void setEntity(T entity) {
        this.entity = entity;
    }

    public String getFieldName() {
        return fieldName;
    }

    public void setFieldName(String fieldName) {
        this.fieldName = fieldName;
    }

    public S getOldValue() {
        return oldValue;
    }

    public void setOldValue(S oldValue) {
        this.oldValue = oldValue;
    }

    public S getNewValue() {
        return newValue;
    }

    public void setNewValue(S newValue) {
        this.newValue = newValue;
    }

}
