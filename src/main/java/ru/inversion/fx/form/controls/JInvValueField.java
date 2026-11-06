package ru.inversion.fx.form.controls;
import java.util.function.BiConsumer;
import javafx.beans.property.Property;
import javafx.geometry.Bounds;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import ru.inversion.fx.form.ViewContext;
import ru.inversion.fx.form.lov.ILov;
import ru.inversion.utils.S;
import ru.inversion.utils.converter.TypeConverter;

/**
 *
 * @author antonovdi
 * @param <T>
 */
public abstract class JInvValueField<T> extends JInvTextField  {


    /**
     *
     */
    public JInvValueField() {
        this(null);
    }

    /**
     *
     * @param arg0
     */
    public JInvValueField(String arg0) {
        super( arg0 );
    }

    /**
     *
     * @param <T>
     * @return
     */
    abstract public Property<T> valueProperty();

    /**
     *
     * @return
     */
    public T getValue() {
        return valueProperty().getValue();
    }

    /**
     *
     * @param value
     */
    public void setValue(T value) {
        valueProperty().setValue(value);
    }


    /**
     *
     * @return  */
    public abstract Class<T> getClassValue( );

    @Override
    public ClipboardContent getClipboardContent() {
        final String selectedText = getSelectedText();
        final String text = getText();
        final T value = getValue();
        final ClipboardContent content = new ClipboardContent();

        if ( allTextSelected( selectedText, text ) ) {
            if ( value != null ){
                //Кладём Value в буфер обмена, если выделено всё
                content.putString( value.toString() );
            }
        } else {
            //Если выделена часть текста, удаляем пробелы и вставляем её
            final String cleanText = selectedText.replace( " ", "" );
            if ( S.isNotNullOrEmpty( cleanText ) ){
                content.putString( cleanText );
            }
        }
        return content;
    }

    private static boolean allTextSelected( final String selectedText, final String allText ) {
        return S.isNotNullOrEmpty( allText ) && selectedText.length() == allText.length();
    }

    /**
     *      */
//    @Override
//    @Deprecated
//    public boolean showLOV() {
//
//        boolean result = false;
//        if (getLOV() != null) {
//            if (getLOV().showChoiceList(ViewContext.of(getScene().getWindow()), getText())) {
//                setValue(TypeConverter.convert(getLOV().getValue(), getClassValue()));
//                result = true;
//            }
//
//        }
//        return result;
//    }



    @Override
    public void showLOV(BiConsumer<Boolean, ILov> clb){

        if (getLOV() != null) {

            if (getLOV().isSmallLov()) {
                    Bounds boundsInLocal = this.getBoundsInLocal();
                    Bounds localToScreen = this.localToScreen(boundsInLocal);
                    getLOV().setPosition((int) localToScreen.getMinX(), (int) localToScreen.getMinY());
            }

            getLOV().showChoiceList(ViewContext.of( getScene().getWindow() ), TypeConverter.convert(getValue(), String.class), new BiConsumer<Boolean,ILov>(){
                @Override
                public void accept(Boolean t, ILov u) {
                    if(t){
                        //setValue(TypeConverter.convert(getLOV().getValue(), getClassValue()));
                        setTextValueFromLov();
                    }
                    if (clb!=null){
                            clb.accept(t, u);
                    }
                }
            });
        }

    }

    @Override
    protected void setTextValueFromLov( ) {
        setValue( TypeConverter.convert( getLOV().getValue(), getClassValue() ) );
    }

}
