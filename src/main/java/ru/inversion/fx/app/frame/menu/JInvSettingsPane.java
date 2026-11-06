package ru.inversion.fx.app.frame.menu;

import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableSet;
import javafx.collections.SetChangeListener;
import javafx.event.ActionEvent;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.VPos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.WindowEvent;
import ru.inversion.utils.Pair;
import org.apache.commons.lang.LocaleUtils;
import org.controlsfx.control.PropertySheet;
import org.controlsfx.control.PropertySheet.Item;
import ru.inversion.fx.app.AppException;
import ru.inversion.fx.app.BaseApp;
import ru.inversion.fx.app.es.JInvErrorService;
import ru.inversion.fx.app.property.IAppProperties;
import ru.inversion.fx.app.property.PropertiesTypeEnum;
import ru.inversion.fx.app.sec.JInvSecurityService;
import ru.inversion.fx.app.service.ViewPrefAppService;
import ru.inversion.fx.app.service.exteditor.ExternalEditorManager;
import ru.inversion.fx.form.Alerts;
import ru.inversion.fx.form.controls.sheet.JInvPropertySheet;
import ru.inversion.fx.form.mdi.JInvWindowMdi;
import ru.inversion.fx.log.LogManager;
import ru.inversion.fx.log.LoggerTimingEnum;
import ru.inversion.icons.IconDescriptorBuilder;
import ru.inversion.icons.IconFactory;
import ru.inversion.icons.enums.FontAwesome;
import ru.inversion.utils.S;
import ru.inversion.utils.U;
import ru.inversion.utils.converter.TypeConverter;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

import static ru.inversion.fx.app.property.PropertiesTypeEnum.*;
import static ru.inversion.fx.form.controls.sheet.JInvPropertyEditorFactory.FILE_TO_STRING;

/**
 *
 * @author antonovdi, sulimoff
 */
public class JInvSettingsPane extends GridPane {

    public static final String DEFAULT_FX_TRACE_PROC = "VIEWSTAT.BAT";

    //public static final String PROPERTY_FX_TRACE_PROC = "FX_TRACE_PROC";

    static private ResourceBundle bundle = ResourceBundle.getBundle("fore");

    //static private ResourceBundle bundleLanguage = ResourceBundle.getBundle("languages");

    // Компонент
    private JInvPropertySheet sheet;

    // Накопленные изменения
    private final ObservableSet<PropertyItemValue> changeSet = FXCollections.observableSet( new HashSet<>() );

    private HBox warningBox = new HBox();

    public JInvSettingsPane() {

        initSheetPane();
        initWarningBox();
        initButtons();

        //loadValues();

        setVgap(5);
        setHgap(5);

        setPrefSize(800, 700 );
    }

    /** */
    private void initPropertyValue( PropertyItemValue pv, Map<Integer,Boolean> cacheGrants ) {

        if( pv.getDescriptor() == PropertyItemEnum.LOGGER_FOLDER ) {
            //pv.setValue   ( LogManager.DEFAULT_LOGGING_FOLDER_NAME );
            pv.setValue   ( LogManager.getCurrentLogFolder().toString() );
            pv.setEditable( false );
        }
        else
        {
            int grantId = pv.getDescriptor().getGrantId();

            if(grantId > 0) {

                if(!cacheGrants.containsKey(grantId))
                    cacheGrants.put(grantId, JInvSecurityService.isCanAccessIsAction(BaseApp.APP().getCommonTaskContext(), grantId));

                pv.setEditable(cacheGrants.get(grantId));

            }//end if

            String name = pv.getPropertyName();

            IAppProperties appProperties = BaseApp.APP().getProperties(pv.getPropertyType());

            Object value = appProperties.getProperty(name);

            if( value != null )
            {
                Class classOfProperty = pv.getType();

                if( classOfProperty == Boolean.class ) {
                    value = !value.equals("0");
                } else if(classOfProperty == Locale.class) {
                    value = LocaleUtils.toLocale((String)value);
                } else if(classOfProperty == File.class) {
                    value = FILE_TO_STRING.fromString( (String) value );
                } else if(classOfProperty == LoggerTimingEnum.class) {
                    value = LoggerTimingEnum.fromLogString((String)value);
                } else if(classOfProperty.isEnum()) {
                    value = Enum.valueOf(classOfProperty, ((String)value).toUpperCase());
                } else if(classOfProperty == Color.class) {
                    value = Color.web((String)value);
                } else if(classOfProperty == Font.class) {
                    value = ViewPrefAppService.getFontFromString((String)value);
                } else if(classOfProperty == ConnectionIndicators.class) {
                    value = ConnectionIndicators.fromXMLString( (String)value );
                } else
                    value = TypeConverter.convert(value, classOfProperty);

                pv.setValue( value );
            }
            else {
                if( pv.getType() == ConnectionIndicators.class )
                    pv.setValue( ConnectionIndicators.fromXMLString( null ) );
                else
                    pv.setValue( pv.getDescriptor().getValue() );
            }
        }

        if( pv.isEditable() ){
            pv.getObservableValue().ifPresent( i -> {
                ((ObservableValue)i).addListener( ( v, o, n ) -> {
                    changeSet.add( pv );
                } );
            } );
        }
    }

    /** */
    private void initDefaultSettings( ) {

        final Map<Integer,Boolean> cacheGrants = new TreeMap<>();
        changeSet.clear();
        sheet.getItems().forEach( item -> initPropertyValue( (PropertyItemValue)item, cacheGrants ) );
    }

    /** */
    private void initSheetPane( ) {

        sheet = new JInvPropertySheet(true);
        sheet.setMode( PropertySheet.Mode.CATEGORY );
        sheet.setSearchBoxVisible   ( true );
        sheet.setModeSwitcherVisible( true );

        Arrays.stream( PropertyItemEnum.values() )
              .map( PropertyItemValue::create ).filter( Objects::nonNull)
              .collect( Collectors.toCollection( sheet::getItems ) );

        initDefaultSettings( );

        // Init access
        Map<String, Boolean> map = new LinkedHashMap<>();
        map.put("MAIL", JInvSecurityService.isCanAccessIsAction( BaseApp.APP().getCommonTaskContext(), 4099));
        getProperties().put("r.i.securityMap", map );

        add( sheet, 0, 0 );

        GridPane.setConstraints( sheet, 0, 0, 1, 1, HPos.LEFT, VPos.CENTER, Priority.ALWAYS, Priority.ALWAYS );
    }

    /** */
    private void doClose( ) {

        try {

            if( window != null )
                BaseApp.APP().getMainFrame().closeWindow(window);
            else
                fireEvent( new WindowEvent( this.getScene().getWindow(), WindowEvent.WINDOW_CLOSE_REQUEST ));

        } catch (Throwable ex) {
            JInvErrorService.handleException(null, ex);
        }
    }

    private void initWarningBox() {
        final Label icon = IconFactory.getLabel( IconDescriptorBuilder.of( FontAwesome.fa_repeat ) );
        final Label text = new Label("  "+bundle.getString( "SETTINGS_RESTART_REQUIRED" ));
        text.setGraphic( icon );
        text.setStyle( "-fx-background-color: #ffff9999; -fx-background-radius:5; -fx-background-insets: -5;" );
        warningBox = new HBox(10, text);
        warningBox.setAlignment( Pos.CENTER_LEFT );
        warningBox.setVisible( false );
        add( warningBox, 0, 1 );
        GridPane.setConstraints ( warningBox, 0, 1, 1, 1,
                HPos.LEFT, VPos.CENTER, Priority.ALWAYS, Priority.NEVER, new Insets(0,10,5,10)
        );
        changeSet.addListener( (SetChangeListener<PropertyItemValue>) change -> {
            if ( change.wasAdded() && change.getElementAdded().isRestartRequired() ) {
                warningBox.setVisible( true );
            } else if ( changeSet.isEmpty() || changeSet.stream().noneMatch( PropertyItemValue::isRestartRequired ) ){
                warningBox.setVisible( false );
            }
        } );
    }

    private void initButtons() {

        Button okButton = new Button( bundle.getString("OK") );

        okButton.setOnAction( (ActionEvent event1) -> {
            if ( warningBox.isVisible() ){
                Alerts.info( this, bundle.getString( "SETTINGS_RESTART_REQUIRED_DETAILED" ) );
            }
            try {

                saveChangedSettings();

                BaseApp.APP().getDBTools().refreshAppPreferenceCache( BaseApp.APP().getCommonTaskContext() );

                changeLoggerParameters();
                changeViewPref();
                changeLocale();

                doClose();

            } catch (Throwable ex) {
                JInvErrorService.handleException(null, ex);
            }
        });

        Button cancelButton = new Button(bundle.getString("CANCEL"));

        cancelButton.setOnAction(event2 -> {
            doClose();
        });

        Button revertButton = new Button(bundle.getString("SETTINGS_BUTTON_REVERT"));

        revertButton.setOnAction(event2 -> {

            if( Alerts.yesNo(getScene().getWindow(), null, bundle.getString("SETTINGS_ALERT_REVERT")) )
            {
//                loadValues ();
//                initDefaultSettings();
                getChildren().remove(sheet);

                initSheetPane( );
            }
        });

        HBox buttonBox = new HBox(5, revertButton, okButton, cancelButton) ;

        buttonBox.setAlignment( Pos.CENTER_RIGHT );
        add( buttonBox, 0, 1 );
        GridPane.setConstraints (
            buttonBox, 0, 1, 1, 1,
                HPos.RIGHT, VPos.CENTER, Priority.ALWAYS, Priority.NEVER, new Insets(0,5,5,5)
        );
    }

    /** */
    private void saveChangedSettings( ) {

        final Map<PropertiesTypeEnum, List<Pair<String, String>>> dbMapPrefs = new HashMap<>();
        final Map<PropertiesTypeEnum, List<Pair<String, String>>> localMapPrefs = new HashMap<>();

        for( Item item : sheet.getItems() )
        {
            PropertyItemValue piv = (PropertyItemValue)item;
            String propertyName   = piv.getPropertyName();

            if( !changeSet.contains( piv ) ){
                 continue;
            }

            PropertiesTypeEnum typeOfProperty = piv.getPropertyType();
            Object             propertyValue  = piv.getValue();
            String             stringValue    = null;
            PropertyItemEnum   descriptor = piv.getDescriptor();

            if( propertyValue instanceof File ) {
                stringValue = ((File) propertyValue).getPath();
            } else if (propertyValue instanceof String) {
                stringValue = (String) propertyValue;
            } else if ( propertyValue instanceof Boolean ) {
                stringValue = (Boolean)propertyValue ? "1" : "0";
            } else if (propertyValue instanceof Enum) {
                if (propertyValue instanceof LoggerTimingEnum) {
                    stringValue = ((LoggerTimingEnum) propertyValue).toLogString();
                } else {
                    stringValue = ((Enum) propertyValue).name();
                }
            } else if (propertyValue instanceof Color) {
                Color color = (Color) propertyValue;
                stringValue = String.format("#%02X%02X%02X%02X", (int) (color.getRed() * 255), (int) (color.getGreen() * 255), (int) (color.getBlue() * 255), (int) (color.getOpacity() * 255));
            } else if (propertyValue instanceof Locale) {
                stringValue = ((Locale) propertyValue).toString();
            } else if (propertyValue instanceof Font) {
                Font font = (Font) propertyValue;
                stringValue = ViewPrefAppService.getStringFromFont(font);
            } else if ( propertyValue instanceof ConnectionIndicators ) {
                ConnectionIndicators ind = (ConnectionIndicators) propertyValue;
                stringValue = ind.toXMLString();
            } else if (propertyValue != null) {
                stringValue = propertyValue.toString();
            }

            Map<PropertiesTypeEnum, List<Pair<String, String>>> maptoInsert =
                    U.in( descriptor.getPropertyType(), DB_USER, DB_GLOBAL, DB_UNIVERSAL )
                          ? dbMapPrefs
                          : localMapPrefs;

            if( !maptoInsert.containsKey(typeOfProperty) ) {
                maptoInsert.put(typeOfProperty, new ArrayList<>() );
            }
            maptoInsert.get(typeOfProperty).add(new Pair<>(propertyName, stringValue));
        }

        if (!dbMapPrefs.isEmpty()) {
            //Временная затычка для сброса кэшированного режима представления
            ViewPrefAppService.setFrameMode( null );
            BaseApp.APP().getDBTools().setPreference(BaseApp.APP().getCommonTaskContext(), dbMapPrefs);
        }
        if (!localMapPrefs.isEmpty()) {
            localMapPrefs.keySet().forEach( propertyType -> {
                IAppProperties propStorage = BaseApp.APP().getProperties( propertyType );

                List<Pair<String, String>> prefList = localMapPrefs.get( propertyType );

                prefList.forEach( p -> {
                    propStorage.setProperty(p.first, U.nvl( p.second, S.EMPTY_STRING ) );
                } );
            });
        }

        ExternalEditorManager.INSTANCE().settingsUpdatedEvent().setValue(new Object()); //aka fire/invoke
    }

    private boolean isChanged(String property){
        return changeSet.stream().anyMatch( i -> Objects.equals( i.getName(), property ) );
    }

    /** */
    private void changeLoggerParameters() {

        if(    isChanged(LogManager.PROPERTY_DEFAULT_ROOTLOGGER_LEVEL)
            || isChanged(LogManager.PROPERTY_DEFAULT_ROOTLOGGER_HISTORY)
            || isChanged(LogManager.PROPERTY_DEFAULT_ROOTLOGGER_TIMING)
            || isChanged(LogManager.PROPERTY_DEFAULT_ROOTLOGGER_FILE_PATTERN) )
        {

            LogManager.INSTANCE().refreshLoggerConfig();
        }
    }

    private void changeLocale() {

        if( isChanged("LANGUAGE") )
            BaseApp.APP().refreshLocale();
    }

    private void changeViewPref() throws AppException {

        if (BaseApp.APP().getMainFrame() != null) {
            BaseApp.APP().getMainFrame().refreshViewSettings();
        }
    }

    public void setWindow( JInvWindowMdi w) {
        this.window = w;
    }

    private JInvWindowMdi window;
}
