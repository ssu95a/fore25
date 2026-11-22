package ru.inversion.fx.form.controls;

import javafx.beans.property.Property;
import javafx.beans.value.ChangeListener;

/**
 * Текстовое поле с поддержкой состояния.
 * Установка состояния реализуется в методе ICustom.setText
 * @author perov
 * @param <T>
 */
public class JInvCustomValueField<T> extends JInvValueField<T> {
    
    private ICustom custom = null;
    private final ChangeListener<String> customTextListener = (observable, oldValue, newValue) -> {
        if (custom != null) {
            setState(custom.setText(newValue));
        }
    };
    
    /**
     * Интерфейс установки состояния
     * @param <T> 
     */
    public interface ICustom<T> {
        <P extends Property<T>> P valueProperty() ;
        Class getClassValue();
        State setText(String text);
    }
    
    /**
     * Значение 
     * @param <P>
     * @return 
     */
    @Override
    public  Property<T>  valueProperty() {
        return  custom.valueProperty();
    }
    
    /**
     * Класс которым типизирован компонент
     * @return 
     */
    @Override
    public Class getClassValue() {
       return custom.getClassValue();
    }

    public JInvCustomValueField() {
        super(null);
    }
    
    public JInvCustomValueField(String arg0) {
        super(arg0);
    }
    
    /**
     * Метод установки реализации установки состояния
     * @param custom 
     */
    public void setCustomImpl(ICustom custom) {
        if (this.custom == custom) {
            return;
        }
        if (this.custom != null) {
            textProperty().removeListener(customTextListener);
        }
        this.custom = custom;
        if (custom != null) {
            textProperty().addListener(customTextListener);
        }
    }  
    
}
