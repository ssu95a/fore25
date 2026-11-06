package ru.inversion.fx.form.controls;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Pos;
import javafx.util.StringConverter;
import ru.inversion.db.entity.ContentTypeEnum;
import ru.inversion.fx.form.controls.renderer.ContentTypeManager;
import ru.inversion.utils.converter.IConverter;
import ru.inversion.utils.converter.TypeConverter;

/**
 *
 * @author Sulimoff,
 *         antonovdi
 */
public class JInvTableColumnDate<S, T> extends JInvTableColumn<S, T> {

    private ObjectProperty<DateContentType> dateFormatProperty = null;

    {
        alignment = Pos.CENTER;
        setPrefWidth(120);
    }
    
    public enum DateContentType {
        DATE, // dd.MM.yyyy
        TIME, // mm:ss
        TIME_NO_SECONDS,
        DATE_TIME,// dd.MM.yyyy hh:mm
        TIME_DATE,// hh:mm dd.MM.yyyy
        DATE_TIME_DETAIL, // dd.MM.yyyy hh:mm:ss
        TIME_DATE_DETAIL; // hh:mm:ss dd.MM.yyyy
        
        public String getMask(){
            switch(this){
                case DATE:return ContentTypeManager.getFormatMask(ContentTypeEnum.DATE);
                case TIME:return ContentTypeManager.getFormatMask(ContentTypeEnum.TIME);
                case TIME_NO_SECONDS:return ContentTypeManager.getFormatMask(ContentTypeEnum.TIME_NO_SECONDS);
                case DATE_TIME:return ContentTypeManager.getFormatMask(ContentTypeEnum.DATE_TIME);
                case TIME_DATE:return ContentTypeManager.getFormatMask(ContentTypeEnum.TIME_DATE);
                case DATE_TIME_DETAIL:return ContentTypeManager.getFormatMask(ContentTypeEnum.DATE_TIME_DETAIL);
                case TIME_DATE_DETAIL:return ContentTypeManager.getFormatMask(ContentTypeEnum.TIME_DATE_DETAIL);
                default:return null;
            }
        }

        /** */
        public StringConverter getStringConverter( Class clazz ) {
            return new StringConverter() {
                final IConverter<Object,String> c = TypeConverter.getFormatConverter( clazz, getMask() );
                @Override
                public String toString( Object o ) {
                    return c.to(o);
                }
                @Override
                public Object fromString( String s ) {
                    return c.from(s);
                }
            };
        }
    }

    public ObjectProperty<DateContentType> dateFormatProperty() {

        if( dateFormatProperty == null )
        {
            dateFormatProperty = new SimpleObjectProperty<>(this, "dateFormat", DateContentType.DATE_TIME_DETAIL );
            dateFormatProperty.addListener(new ChangeListener< DateContentType >() {
                @Override
                public void changed( ObservableValue< ? extends DateContentType > observable, DateContentType oldValue, DateContentType newValue ) {
                    setPrefWidth( newValue.ordinal() < 3 ? 80 : 120 );
                }
            });
        }
        return dateFormatProperty;
    }
    final public DateContentType getDateFormat() {
        return dateFormatProperty == null ? DateContentType.DATE_TIME_DETAIL : dateFormatProperty.get();
    }
    final public void setDateFormat(DateContentType v) {
        dateFormatProperty().set(v);
    }
}
