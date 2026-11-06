package ru.inversion.fx.app.es;

import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Window;
import javafx.util.Callback;
import javafx.util.Pair;
import ru.inversion.fx.app.service.ViewPrefAppService;
import ru.inversion.fx.form.ActionFactory;
import ru.inversion.fx.form.Alerts;
import ru.inversion.fx.form.controls.JInvToolBar;
import ru.inversion.icons.IconFactory;
import ru.inversion.icons.enums.FontAwesome;
import ru.inversion.utils.IExceptionInfo;
import ru.inversion.utils.S;
import ru.inversion.utils.U;

import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.ResourceBundle;

/**
 *
 * @author ssu
 */
public class DialogError extends Dialog<Throwable> {

    private static final ResourceBundle fore = ResourceBundle.getBundle("fore");

    final private Throwable      throwable;
    final private IExceptionInfo exceptionInfo;
    private TableView<Throwable> table;

    private static String getThrowableTitle( Throwable th )
    {
        String text   = U.nvl( th.getMessage(), S.EMPTY_STRING );
        String l_text = U.nvl( th.getLocalizedMessage(), S.EMPTY_STRING );

        if(!text.equals(l_text) )
            text = new StringBuilder(text).append(" (").append(l_text).append(')').toString();

        if( th instanceof SQLException )
        {
            SQLException se = (SQLException)th;
            text = new StringBuilder(text).append(" (").append(se.getErrorCode()).append(')').toString();//.append(se.getSQLState()).toString();
        }

        if( S.isNullOrEmpty(text) )
            text  = "Exception: " + th.getClass().getName();

        return text;
    }

    /** */
    private static class WrapExceptionInfo extends Throwable implements IExceptionInfo {

        final private Throwable th;
        final private String    title,
                                message;
        /** */
        public WrapExceptionInfo( Throwable th ) {

            this.th = th;

            String text = getThrowableTitle(th);

            if( S.isNullOrEmpty(th.getMessage()) )
            {
                title   = text;
                message = fore.getString("SISTEMNAYA_OSHIBKA");
            }
            else
            {
                int pointIndex = text.indexOf(". ");

                if( pointIndex == -1 )
                {
                    title   = text;
                    message = null;
                }
                else
                {
                    int postPointIndex = pointIndex + 1; //не переносим первую точку в тело
                    String s1 = text.substring( 0, postPointIndex ).trim();
                    String s2 = text.substring( postPointIndex ).trim();

                    if (S.isNullOrEmpty(s1)) {
                        s1 = s2;
                        s2 = null;
                    }

                    title   = s1;
                    message = s2;
                }//end if
            }
        }

        @Override
        public void printStackTrace(PrintWriter s) {
            th.printStackTrace(s); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public void printStackTrace(PrintStream s) {
            th.printStackTrace(s); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public void printStackTrace() {
            th.printStackTrace(); //To change body of generated methods, choose Tools | Templates.
        }
        /** */
        @Override
        public String toString() {
            return th.toString(); //To change body of generated methods, choose Tools | Templates.
        }
        /** */
        @Override
        public synchronized Throwable getCause() {
            return th.getCause(); //To change body of generated methods, choose Tools | Templates.
        }
        /** */
        @Override
        public String getLocalizedMessage() {
            return th.getLocalizedMessage(); //To change body of generated methods, choose Tools | Templates.
        }
        /** */
        @Override
        public String getMessage() {
            return th.getMessage(); //To change body of generated methods, choose Tools | Templates.
        }
        /** */
        @Override
        public String getTitle() {
            return title;
        }

        /** */
        @Override
        public String getContentText() {
            return message;
        }
    }

    /** */
    private static IExceptionInfo cast2Info( Throwable th ) {

        if( th == null )
            return null;

        if( th instanceof IExceptionInfo )
            return (IExceptionInfo)th;

        return new WrapExceptionInfo(th);
    }

    /** */
    public DialogError( Window parent, Throwable th ) {

        this.exceptionInfo = cast2Info(th);
        this.throwable     = th;

        this.setTitle(fore.getString("SOOBSHCHENIE_OB_OSHIBKE"));

        final DialogPane dialogPane = getDialogPane();

        if( exceptionInfo.getContentText() != null && !exceptionInfo.getContentText().isEmpty()){

            TextArea text = new TextArea();
            VBox.setVgrow(text, Priority.ALWAYS);
            text.setWrapText(true);
            text.setEditable(false);
            text.textProperty().bind(this.contentTextProperty());
            GridPane.setConstraints(text, 0, 0, 1, 1, HPos.LEFT, VPos.CENTER, Priority.ALWAYS, Priority.ALWAYS);

            GridPane gp = new GridPane();
            gp.setPadding( new Insets( 8,8,0,8 ) );
            gp.add( text, 0, 0 );


            dialogPane.setContent(gp);
        }

        this.setHeaderText ( exceptionInfo.getHeaderText() );
        this.setContentText( exceptionInfo.getContentText() );

        dialogPane.getStyleClass().addAll("alert", "error");

//        if( parent != null )
//            this.initOwner   ( parent );
        this.initModality(Modality.APPLICATION_MODAL);
        this.setResizable(true);

        dialogPane.getButtonTypes().add(ButtonType.OK);

        dialogPane.setExpandableContent(createDetailsNode());

//        try {
//            BaseApp.APP().getViewPrefService().refreshViewSettingsRoot(dialogPane);
//        } catch (Throwable ex) {
//        }

        if( th.getCause() != null && !( th instanceof JInvErrorService.OraUserDefException)) {
            Platform.runLater(() -> dialogPane.setExpanded(true));
        }

        ViewPrefAppService.localizeDialog(dialogPane);
    }

    private Throwable getCause( Throwable th )
    {
        if( th == null )
            return null;

        Throwable ret = th.getCause();

        if( ret == null && th instanceof SQLException )
        {
            SQLException se = (SQLException)th;

            ret = se.getNextException();
        }

        return ret;
    }

    /** */
    private Node createDetailsNode() {

        boolean hasCause = this.throwable.getCause() != null;

        GridPane gp = new GridPane();
        gp.setHgap(2.0);
        gp.setVgap(5.0);

        if (hasCause) {

            ObservableList<Throwable> errorList = FXCollections.observableArrayList(new ArrayList());

            for( Throwable e = throwable.getCause(); e != null; e = getCause(e) ) {
                errorList.add(e);
            }

            table = new TableView<>(errorList);
            TableColumn c = new TableColumn<Throwable, String>(fore.getString("DETALI"));
            table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
            c.setSortable(false);
            c.setCellValueFactory(new Callback<TableColumn.CellDataFeatures<Throwable, String>, ObservableValue<String>>() {
                @Override
                public ObservableValue<String> call(TableColumn.CellDataFeatures<Throwable, String> param) {
                    return new SimpleStringProperty (
                            param.getValue() instanceof IExceptionInfo ?
                                    ((IExceptionInfo)param.getValue()).getHeaderText()
                                    :
                                    getThrowableTitle(param.getValue())
                    );
                }
            });

            table.getColumns().add(c);

            gp.add(table, 0, 0);
            GridPane.setConstraints(table, 0, 0, 1, 1, HPos.LEFT, VPos.CENTER, Priority.ALWAYS, Priority.ALWAYS);
        }
        else
        {
            String exceptionText = exceptionInfo.getDetailedMessage();

            if( S.isNullOrEmpty(exceptionText) ) {
                StringWriter sw = new StringWriter();
                PrintWriter  pw = new PrintWriter(sw);
                throwable.printStackTrace(pw);
                exceptionText = sw.toString();
            }

            TextArea textArea = new TextArea(exceptionText);
            textArea.setEditable ( false );
            textArea.setWrapText ( true  );
            textArea.setMaxWidth ( Double.MAX_VALUE );
            textArea.setMaxHeight( Double.MAX_VALUE );
            gp.add(textArea, 0, 0);
            GridPane.setConstraints(textArea, 0, 0, 1, 1, HPos.LEFT, VPos.CENTER, Priority.ALWAYS, Priority.ALWAYS);
        }

        JInvToolBar tb = new JInvToolBar();

        Window parent = getDialogPane().getScene().getWindow();

        if( hasCause )
        {
            ButtonBase btTopDetail = new Button();
            btTopDetail.setGraphic ( IconFactory.getLabel( FontAwesome.fa_level_up )); //fa_caret_square_o_up
            btTopDetail.setOnAction( e -> showThrowable ( parent, throwable, true ) );
            btTopDetail.setTooltip ( new Tooltip(fore.getString( "DETAILS_TOPLEVEL" ) ));

            ButtonBase btDetail = new Button();
            btDetail.setGraphic ( IconFactory.getLabel( FontAwesome.fa_bars ));
            btDetail.setOnAction( e -> showThrowable( parent, table.getSelectionModel().getSelectedItem(), true) );
            btDetail.setTooltip ( new Tooltip(fore.getString( "DETAILS_SELECTED" ) ));


            tb.getItems().addAll(
                btTopDetail,
                btDetail
            );
        }

        Button btMail = new Button();
        ActionFactory.assignButtonStyleSilent(ActionFactory.ActionTypeEnum.EMAIL, btMail );
        btMail.setOnAction( (e)->sendMail( parent, this.throwable ) );

//        Button btnSave = new Button();
//        ActionFactory.assignButtonStyleSilent(ActionFactory.ActionTypeEnum.SAVE_FILE, btnSave );
//        tb.getItems().addAll( btnMail, btnSave );

        tb.getItems().add( btMail );

        gp.add(tb, 0, 1);
        GridPane.setConstraints(tb, 0, 1, 1, 1, HPos.LEFT, VPos.CENTER, Priority.NEVER, Priority.NEVER);

        return gp;
    }

    /** */
    static private void sendMail( Window parent, Throwable th )
    {
        try {

            final JInvErrorService.IErrorMailSender mailSender = new ErrorMessageSender();//JInvErrorService.getErrorMailSender();

            if( mailSender != null) {

                DialogSendMail d = new DialogSendMail();
                d.showAndWait().map((JInvErrorService.IErrorSendProperties p)->{ mailSender.send(th, p); return null; } );
            }
            else
            {
                Alerts.info(parent, fore.getString("POCHTA_NE_NASTROENA"));
            }
        } catch( Throwable ex ) {
            JInvErrorService.handleException( parent, ex );
        }
    }

    /** */
    static public void showThrowable(Window parent, Throwable th, boolean isDetail) {

        if (th == null) {
            return;
        }

        Alert alert = new Alert(AlertType.ERROR);
        alert.initOwner(parent);
        alert.setTitle(isDetail ? fore.getString("DETALI") : fore.getString("SOOBSHCHENIE_OB_OSHIBKE"));

        IExceptionInfo exceptionInfo = cast2Info(th);

        //Pair<String, String> p = getErrorText(th);

        alert.setHeaderText ( exceptionInfo.getHeaderText() );
        alert.setContentText( exceptionInfo.getContentText() );

        String detail = exceptionInfo.getDetailedMessage();

        if( S.isNullOrEmpty(detail) )
        {
            StringWriter sw = new StringWriter( );
            PrintWriter  pw = new PrintWriter(sw);
            th.printStackTrace(pw);
            detail = sw.toString();
        }

        TextArea textArea = new TextArea(detail);
        textArea.setEditable(false);
        textArea.setWrapText(true );

        textArea.setMaxWidth ( Double.MAX_VALUE );
        textArea.setMaxHeight( Double.MAX_VALUE );
        GridPane.setVgrow(textArea, Priority.ALWAYS);
        GridPane.setHgrow(textArea, Priority.ALWAYS);

        GridPane expContent = new GridPane();
        expContent.setMaxWidth(Double.MAX_VALUE);
        expContent.add(textArea, 0, 1);

        alert.getDialogPane().setExpandableContent(textArea);
        alert.getDialogPane().setExpanded(true);

        alert.showAndWait();
    }

    /**
     *
     */
    private static Pair<String, String> getErrorText(Throwable th) {

        Pair<String, String> pair = null;

        String text = th.getLocalizedMessage();

        if (S.isNullOrEmpty(text)) {
            pair = new Pair<>("EXCEPTION: " + th.getClass().getName(), fore.getString("SISTEMNAYA_OSHIBKA"));
        } else {

            int pointIndex = text.indexOf(". ");

            if (pointIndex == -1) {
                pair = new Pair<>(text, null);
            } else {
                String s1 = text.substring(0, pointIndex).trim();
                String s2 = text.substring(pointIndex).trim();

                if (S.isNullOrEmpty(s1)) {
                    s1 = s2;
                    s2 = S.EMPTY_STRING;
                }

                pair = new Pair<>(s1, s2);
            }
        }
        return pair;
    }
}
