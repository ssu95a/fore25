package ru.inversion.fx.form.controls.renderer;

import javafx.geometry.Pos;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;

/** */
public class JInvTableCellIcon<S> extends JInvTableCell<S,String> {
    //final private Label icon = new Label();

    public JInvTableCellIcon( String style ) {
        super(null);
        setAlignment( Pos.CENTER );
        /*
        icon.setContentDisplay( ContentDisplay.TEXT_ONLY );
        //icon.setStyle("-fx-font-family:'FontAwesome';-fx-font-size:1.5em;-fx-alignment:TOP_CENTER");
        icon.setStyle(style);
        */
        setStyle(style);
    }

    /** */
    @Override
    protected void updateItem( String item, boolean empty )
    {
        super.updateItem(item, empty);
//
//        setText(null);
//
//        if( empty )
//            setGraphic(null);
//        else
//        {
//            icon.setText(item);
//            setGraphic(icon);
//        }
    }
}
