/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ru.inversion.fx.app.frame.menu;

import javafx.beans.value.ObservableValue;
import org.controlsfx.control.PropertySheet;
import ru.inversion.fx.app.property.PropertiesTypeEnum;
import ru.inversion.utils.U;

import java.beans.PropertyChangeSupport;
import java.util.Optional;

/**
 *
 * @author antonovdi
 */
public class SimpleBeanInfo implements PropertySheet.Item {

    private Class          clazz;
    private String         category;
    private String         name;
    private String         desc;
    // Модель, откуда читать
    private Object         sourceObject;
    // из модели свойств
    private String         propertyName;
    // Поставщик данных
    private IItemsProvider itemProvider;
    // Признак что был изменен
    private boolean        changed;

    public boolean isChanged() {
        return changed;
    }

    public void setChanged(boolean changed) {
        this.changed = changed;
    }

    public IItemsProvider getItemProvider() {
        return itemProvider;
    }

    public void setItemProvider(IItemsProvider itemProvider) {
        this.itemProvider = itemProvider;
    }

    public String getPropertyName() {
        return propertyName;
    }

    public void setPropertyName(String propertyName) {
        this.propertyName = propertyName;
    }
    private PropertiesTypeEnum typeProp;

    private PropertyChangeSupport propertyChangeSupport = new PropertyChangeSupport(this);

    public void addPropertyChangeListener(java.beans.PropertyChangeListener listener) {
        propertyChangeSupport.addPropertyChangeListener(listener);
    }

    public void removePropertyChangeListener(java.beans.PropertyChangeListener listener) {
        propertyChangeSupport.removePropertyChangeListener(listener);
    }

    public PropertiesTypeEnum getTypeProp() {
        return typeProp;
    }

    public void setTypeProp(PropertiesTypeEnum typeProp) {
        this.typeProp = typeProp;
    }

    private boolean editable = true;

    public SimpleBeanInfo(Class clazz, String category, String name, String desc, String propertyName, Object sourceObject, PropertiesTypeEnum typeProp) {
        this.clazz = clazz;
        this.category = category;
        this.name = name;
        this.desc = desc;
        this.propertyName = propertyName;
        this.sourceObject = sourceObject;
        this.typeProp = typeProp;
    }

    @Override
    public boolean isEditable() {
        return editable;
    }

    public void setEditable(boolean editable) {
        this.editable = editable;
    }

    @Override
    public Class<?> getType() {
        return clazz;
    }

    @Override
    public String getCategory() {
        return category;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getDescription() {
        return desc;
    }

    @Override
    public Object getValue() {

        try {
            Object result = null;
            if (clazz == Boolean.class) {
                result = sourceObject.getClass().getMethod("is" + propertyName.substring(0, 1).toUpperCase() + propertyName.substring(1)).invoke(sourceObject);
                if (result == null) {
                    result = Boolean.FALSE;
                }
            } else {
                result = sourceObject.getClass().getMethod("get" + propertyName.substring(0, 1).toUpperCase() + propertyName.substring(1)).invoke(sourceObject);
            }
            return result;
        } catch (Throwable ex) {

        }
        return null;
    }

    public void initValue(Object value) {
        try {
            setVal(value);
        } catch (Throwable ex) {
        }
    }

    @Override
    public void setValue(Object value) {

        try {

            if( !U.equals( value, getValue() ) )
            {
            //if (value!=null && !value.equals(getValue())) {
                setChanged( true );
            }
            
            setVal( value );

        } catch (Throwable ex) {
        }
    }

    private void setVal(Object value) throws Exception {
        sourceObject.getClass().getMethod("set" + propertyName.substring(0, 1).toUpperCase() + propertyName.substring(1), clazz).invoke(sourceObject, value);
    }

    @Override
    public Optional<ObservableValue<? extends Object>> getObservableValue() {
        return Optional.empty(); // Optional.of( new SimpleObjectProperty( getValue() ) );

    }
}
