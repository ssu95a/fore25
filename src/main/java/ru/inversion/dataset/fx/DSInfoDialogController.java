package ru.inversion.dataset.fx;
import javafx.application.Platform;
import javafx.fxml.FXML;
import ru.inversion.fx.form.JInvFXFormController;
import ru.inversion.fx.form.controls.JInvTextArea;
import ru.inversion.utils.IDumpable;
import ru.inversion.utils.S;
import ru.inversion.utils.U;

import java.util.Map;
import java.util.TreeMap;

/**
 *
 * @author ssu
 */
public class DSInfoDialogController extends JInvFXFormController<Void> {

    @FXML JInvTextArea edQUERY;

    @FXML JInvTextArea edQUERY_WITH_PARAMS;

    @FXML JInvTextArea edPROPERTIES;

    /** */
    @Override
    protected void init( ) throws Exception {

        titleProperty().set( getBundleString("DS_INFO_DIALOG_TITLE") );

        Platform.runLater( () -> initTextAreas() );

        Map<String, Object> m = this.getInitProperties();

        IDumpable d = (IDumpable)m.get("dataSet");

        if( d != null )
        {
            Map<String, Object> prop = new TreeMap<>();
            d.dump( prop );

            if( !prop.isEmpty() ) {

                StringBuilder sb = new StringBuilder();

                String q = (String)prop.get( "ds.sql.query" );

                if( S.isNotNullOrEmpty(q) ) {

                    sb.append(q);

                    q = (String)prop.get( "ds.parameterIndexMap" );

                    if( S.isNotNullOrEmpty(q) )
                        sb.append('\n').append(q);

                    q = (String)prop.get( "ds.parameterNamedMap" );

                    if( S.isNotNullOrEmpty(q) )
                        sb.append('\n').append(q);

                    edQUERY.setText( sb.toString() );
                }//end if

                q = (String)prop.get( "ds.sql.query_with_parameters" );
                if( S.isNotNullOrEmpty(q) )
                    edQUERY_WITH_PARAMS.setText(q);

                final StringBuilder sb1 = new StringBuilder();

                q = (String)prop.get( "ds.properties" );
                if( S.isNotNullOrEmpty(q) )
                    sb1.append(q);

                prop.entrySet( )
                    .stream( )
                    .filter(
                            (e)->!U.in( e.getKey(),
                                          "ds.sql.query",
                                          "ds.sql.query_with_parameters",
                                          "ds.parameterIndexMap",
                                          "ds.parameterNamedMap",
                                          "ds.properties"
                            )
                    )
                    .forEach((e)->sb1.append( e.getKey()).append(" = ").append(e.getValue()).append('\n'));


                edPROPERTIES.setText( sb1.toString() );
            }
        }
    }

    /** */
    private void initTextAreas( ) {
        edQUERY.lookup(".content").setStyle("-fx-background-color:#FFFFE0");
        edQUERY_WITH_PARAMS.lookup(".content").setStyle("-fx-background-color:#FFFFE0");
        edPROPERTIES.lookup(".content").setStyle("-fx-background-color:#FFFFE0");
    }

}
