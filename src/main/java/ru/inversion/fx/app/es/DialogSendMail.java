package ru.inversion.fx.app.es;

import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.geometry.HPos;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.stage.Modality;
import javafx.util.Callback;
import ru.inversion.fx.app.BaseApp;
import ru.inversion.fx.app.es.JInvErrorService.IErrorSendProperties;
import ru.inversion.fx.app.property.PropertiesTypeEnum;
import ru.inversion.fx.app.service.ViewPrefAppService;
import ru.inversion.fx.form.controls.JInvCheckBox;
import ru.inversion.fx.form.controls.JInvLabel;
import ru.inversion.fx.form.controls.JInvTextField;
import ru.inversion.icons.IconDescriptorBuilder;
import ru.inversion.icons.IconFactory;
import ru.inversion.icons.enums.FontAwesome;
import ru.inversion.icons.enums.IconSize;

import java.util.ResourceBundle;

/**
 *
 * @author ssu
 */
public class DialogSendMail extends Dialog<IErrorSendProperties> {

    private static final ResourceBundle fore = ResourceBundle.getBundle("fore");

    private class SendProperties implements IErrorSendProperties {

        public final BooleanProperty makeScreenShort = new SimpleBooleanProperty (
            BaseApp.APP().getProperties(PropertiesTypeEnum.DB_USER).getBooleanProperty("MAIL_ERR_ATTACH_SCREENSHOT",false)
        );
        public final BooleanProperty attachLog       = new SimpleBooleanProperty(
            BaseApp.APP().getProperties(PropertiesTypeEnum.DB_USER).getBooleanProperty("MAIL_ERR_ATTACH_LOG",false)
        );
        public final StringProperty  subject         = new SimpleStringProperty ( );
        public final StringProperty  info            = new SimpleStringProperty ( );
        public final StringProperty  recipient       = new SimpleStringProperty (
            BaseApp.APP().getProperties(PropertiesTypeEnum.DB_GLOBAL).getStringProperty("DEFAULT_SUPPORT_MAIL")
        );
        public final StringProperty  sender       = new SimpleStringProperty (
            BaseApp.APP().getProperties(PropertiesTypeEnum.DB_GLOBAL).getStringProperty("DEFAULT_SENDER_MAIL")
        );

        /** */
        @Override
        public boolean isMakeScreenShort() {
            return makeScreenShort.get();
        }

        /** */
        @Override
        public boolean isAttachLog() {
            return attachLog.get();
        }

        /** */
        @Override
        public String getSubject() {
            return subject.get();
        }

        /** */
        @Override
        public String getInfo() {
            return info.get();
        }
        
        /** */
        @Override
        public String getRecipient() {
            return recipient.get();
        }

        /** */
        @Override
        public String getSender() {
            return sender.get();
        }
    }
    
    private SendProperties sendProperties = new SendProperties();
    
    public DialogSendMail( ) {

        this.setTitle("Отправка информации об ошибке по e-mail");

        final DialogPane dialogPane = getDialogPane();
        
        this.setHeaderText( "Отправка сообщения об ошибке" );
        this.setGraphic   ( IconFactory.getLabel( new IconDescriptorBuilder().iconId(FontAwesome.fa_envelope_o).iconSize(IconSize.LARGE).build() ) );
        //this.setContentText( "" );

        //dialogPane.getStyleClass().addAll("alert","info");

        this.initModality( Modality.APPLICATION_MODAL );
        this.setResizable( true );

        dialogPane.getButtonTypes().addAll( ButtonType.OK, ButtonType.CANCEL );
        dialogPane.setExpandableContent( createDetailsNode() );
        
//        try {
//            BaseApp.APP().getViewPrefService().refreshViewSettingsRoot(dialogPane);
//        } catch (Throwable ex) {
//        }

        this.setResultConverter( new Callback<ButtonType, IErrorSendProperties>() {
            @Override
            public IErrorSendProperties call(ButtonType param) {
                return param == ButtonType.OK ? sendProperties : null;
            }
        } );
        
        Platform.runLater(() -> dialogPane.setExpanded(true));
        ViewPrefAppService.localizeDialog(dialogPane);
    }

    /**
     *
     */
    private Node createDetailsNode() {

        GridPane gp = new GridPane( );
        gp.setHgap( 2.0 );
        gp.setVgap( 5.0 );

        int y = 0;

        JInvLabel label = new JInvLabel("Адрес отправителя");
        gp.add( label, 0, y );
        GridPane.setConstraints( label, 0, y++, 1, 1, HPos.LEFT, VPos.CENTER, Priority.NEVER, Priority.NEVER );

        JInvTextField edSender = new JInvTextField();
        edSender.setLabel(label);
        gp.add( edSender, 0, y );
        GridPane.setConstraints( edSender, 0, y++, 1, 1, HPos.LEFT, VPos.CENTER, Priority.ALWAYS, Priority.NEVER );
        edSender.textProperty().bindBidirectional( sendProperties.sender );

        label = new JInvLabel("Адрес(а) получателя, через ','");
        gp.add( label, 0, y );
        GridPane.setConstraints( label, 0, y++, 1, 1, HPos.LEFT, VPos.CENTER, Priority.NEVER, Priority.NEVER );
        
        JInvTextField edAddr = new JInvTextField();
        edAddr.setLabel(label);
        gp.add( edAddr, 0, y );
        GridPane.setConstraints( edAddr, 0, y++, 1, 1, HPos.LEFT, VPos.CENTER, Priority.ALWAYS, Priority.NEVER );
        edAddr.textProperty().bindBidirectional( sendProperties.recipient );
        
        label = new JInvLabel("Текст сообщения");
        gp.add( label, 0, y );
        GridPane.setConstraints( label, 0, y++, 1, 1, HPos.LEFT, VPos.CENTER, Priority.NEVER, Priority.NEVER );
        
        TextArea textArea = new TextArea( );
        textArea.setEditable ( true );
        textArea.setWrapText ( true );
        textArea.setMaxWidth ( Double.MAX_VALUE );
        textArea.setMaxHeight( Double.MAX_VALUE );
        
        textArea.textProperty().bindBidirectional( sendProperties.info );
        
        gp.add( textArea, 0, y );
        
        GridPane.setConstraints( textArea, 0, y++, 1, 1, HPos.LEFT, VPos.CENTER, Priority.ALWAYS, Priority.ALWAYS );

        JInvCheckBox chLog = new JInvCheckBox("Отправить лог-файл вместе с письмом");
        //sendProperties.attachLog.bindBidirectional( chLog.selectedProperty() );
        chLog.selectedProperty().bindBidirectional( sendProperties.attachLog );
        gp.add( chLog, 0, y );
        GridPane.setConstraints( chLog, 0, y++, 1, 1, HPos.LEFT, VPos.CENTER, Priority.ALWAYS, Priority.NEVER );

        GridPane gpScreenShort = new GridPane( );
        gpScreenShort.setHgap( 2.0 );
        gpScreenShort.setVgap( 5.0 );
        
        JInvCheckBox chSend1 = new JInvCheckBox("Отправить снимок экрана вместе с письмом");

        JInvCheckBox chSend2 = new JInvCheckBox("Все равно отправить снимок с экрана");
        chSend2.disableProperty().bind( chSend1.selectedProperty().not() );

        if( sendProperties.makeScreenShort.get() ) {
            chSend1.setSelected(true);
            chSend2.setSelected(true);
        }

        sendProperties.makeScreenShort.bind ( 
                chSend1.selectedProperty().and ( 
                            chSend2.selectedProperty() 
                        ) 
        );
        
        JInvLabel lbSend = new JInvLabel("На снимок попадут все видимые данные, которые присутствуют на экране компьютера");
        
        gpScreenShort.add( chSend1, 0, 0 );
        GridPane.setConstraints( chSend1, 0, 0, 2, 1, HPos.LEFT, VPos.CENTER, Priority.ALWAYS, Priority.NEVER );
        gpScreenShort.add( lbSend, 0, 1 );
        GridPane.setConstraints( lbSend , 0, 1, 2, 1, HPos.LEFT, VPos.CENTER, Priority.ALWAYS, Priority.NEVER );
        Label stub = new Label("      ");
        gpScreenShort.add( stub, 0, 2 );
        GridPane.setConstraints( stub , 0, 2, 1, 1, HPos.LEFT, VPos.CENTER, Priority.NEVER, Priority.NEVER );
        gpScreenShort.add( chSend2, 1, 2 );
        GridPane.setConstraints( chSend2 , 1, 2, 1, 1, HPos.LEFT, VPos.CENTER, Priority.ALWAYS, Priority.NEVER );
        
        TitledPane titledPane = new TitledPane("Снимок с экрана", gpScreenShort );
        titledPane.setCollapsible(false);

        gp.add( titledPane, 0, y );
        GridPane.setConstraints( titledPane, 0, y++, 1, 1, HPos.LEFT, VPos.CENTER, Priority.ALWAYS, Priority.ALWAYS );
        
        return gp;
    }    
}
