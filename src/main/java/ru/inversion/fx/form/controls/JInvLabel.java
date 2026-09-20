package ru.inversion.fx.form.controls;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;

/**
 *
 * @author antonovdi
 */
public class JInvLabel extends Label {

    private StringProperty linkFieldNameProperty;

    public JInvLabel() {
        init();
    }

    /** */
    public JInvLabel(String text) {
        super(text);
        init();
    }

    /** */
    public JInvLabel(String text, Node graphic) {
        super(text, graphic);
        init();
    }

    /** */
    private void init()
    {
        setMnemonicParsing(false);
        mnemonicParsingProperty().addListener( (v,o,n) -> {if (n) setMnemonicParsing(false);} );

        //setAlignment(Pos.CENTER_RIGHT);
    }

    /** */
    public String getLinkFieldName() {
        return linkFieldNameProperty == null ? null : linkFieldNameProperty.get();
    }

    /** */
    public void setLinkFieldName( String linkFieldName ) {
        this.linkFieldNameProperty().set( linkFieldName );
    }

    /** */
    public StringProperty linkFieldNameProperty() {
        if( this.linkFieldNameProperty == null )
            this.linkFieldNameProperty = new SimpleStringProperty(this, "linkFieldName");
        return linkFieldNameProperty;
    }

}
