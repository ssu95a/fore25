package ru.inversion.fx.app.frame.menu;
import javafx.beans.value.ObservableValue;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import org.controlsfx.control.PropertySheet;
import ru.inversion.fx.app.property.PropertiesTypeEnum;
import ru.inversion.fx.app.service.ViewPrefAppService;
import ru.inversion.fx.app.service.view.ButtonIconView;
import ru.inversion.fx.app.sound.SoundLevel;
import ru.inversion.fx.form.controls.sheet.SettingsPage;
import ru.inversion.fx.form.valid.ValidViewDecorator.LabelStyleEnum;
import ru.inversion.fx.log.LogManager;
import ru.inversion.fx.log.LoggerLevelEnum;
import ru.inversion.fx.log.LoggerTimingEnum;
import ru.inversion.icons.IconDescriptorBuilder;
import ru.inversion.icons.enums.FontAwesome;
import ru.inversion.utils.U;

import java.io.File;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import static ru.inversion.fx.app.service.ViewPrefAppService.DEFAULT_FX_FONT_CODE;
import static ru.inversion.fx.app.service.exteditor.ExternalEditorManager.ASSOC;
import static ru.inversion.fx.app.property.PropertiesTypeEnum.*;


/** */
public enum PropertyItemEnum implements PropertySheet.Item {

// ┌─────────────────┬────────────────────────────┬─────────────────────────────────┬─────────────────────────────────────────────────────┐
// │     PROPERTY    |      NAME / DATA TYPE      |          PROPERTY TYPE          |          DEFAULT VALUE / REQUIRES RESTART           |
// ├─────────────────┼────────────────────────────┼─────────────────────────────────┼─────────────────────────────────────────────────────┤
// LOGIN
    LOGIN_SKIP_BRANCH_DIALOG( "SKIP_BRANCH_DIALOG",
                              Boolean.class         , DB_USER     ),

    RUN_FX_CALL_PL_MON   ( "RUN_FX_CALL_PL_MON",
                            Boolean.class,           DB_UNIVERSAL,                   false ),
// i18n
     I18N_LOCALE         ( "LANGUAGE",
                           Locale.class,            DB_USER,     Locale.getDefault(), true ),
// LOG
     LOGGER_LEVEL        ( "LOGGER_LEVEL",
                            LoggerLevelEnum.class,  DB_USER,     LoggerLevelEnum.fromLevel(LogManager.DAFAULT_ROOTLOGGER_LEVEL)  ),
     LOGGER_HISTORY      ( "LOGGER_HISTORY",
                            Integer.class,          DB_USER,     LogManager.DAFAULT_ROOTLOGGER_HISTORY ),
     LOGGER_TIMING       ( "LOGGER_TIMING",
                            LoggerTimingEnum.class, DB_USER,     LoggerTimingEnum.fromLogString(LogManager.DEFAULT_ROOTLOGGER_TIMING) ),
     LOGGER_FILE_PATTERN ( "LOGGER_FILE_PATTERN",
                            String.class,           DB_USER,     LogManager.DEFAULT_ROOTLOGGER_FILE_PATTERN ),
     LOGGER_FOLDER       ( "LOGGER_FOLDER",
                            String.class,           DB_USER      ),
// MAIL
     MAILS_SMTP_SERVER    ( "DEFAULT_SMTP_SERVER",
                            String.class,           DB_GLOBAL    ),
     MAILS_SMTP_PORT      ( "DEFAULT_SMTP_PORT",
                            Integer.class,          DB_GLOBAL,       25 ),
     MAILE_DEFAULT_TO_ADDR ( "DEFAULT_SUPPORT_MAIL",
                            String.class,           DB_GLOBAL    ),
     MAILF_FROM_ADDR      ( "DEFAULT_SENDER_MAIL",
                            String.class,           DB_GLOBAL    ),
     MAILF_FROM_NAME      ( "DEFAULT_SENDER_NAME",
                            String.class,           DB_GLOBAL    ),
     MAILE_ENABLE_DEBUG    ( "DEFAULT_SMTP_DEBUG",
                            Boolean.class,          DB_USER,         false ),
     MAILE_ERR_ATTACH_LOG ( "MAIL_ERR_ATTACH_LOG",
                            Boolean.class,          DB_USER,         true  ),
     MAILE_ERR_ATTACH_SCREENSHOT( "MAIL_ERR_ATTACH_SCREENSHOT",
                            Boolean.class,          DB_USER,         false ),
     MAILU_USR_LOGIN      ( "MAIL_USR_LOGIN",
                            String.class,           DB_GLOBAL    ),
     MAILU_USR_PASSWORD   ( "MAIL_USR_PASSWORD",
                            String.class,           DB_GLOBAL    ),
    // GUI
     GUI_COLOR_REQUIRED   ( "FX_COLOR_REQUIRED",
                            Color.class           , DB_USER,         Color.web(ViewPrefAppService.DEFAULT_FX_COLOR_REQUIRED) ),
     GUI_LABEL_REQ_STYLE  ( "FX_VALID_LABEL_STYLE",
                            LabelStyleEnum.class  , DB_USER,         LabelStyleEnum.PLAIN ),
     GUI_SHOW_VALIDATABLE ( "FX_SHOW_VALIDATABLE",
                            Boolean.class,          DB_USER,         false, true ),
     GUI_COLOR_VALIDATABLE( "FX_COLOR_VALIDATABLE", //Раскраска полей с валидаторами
                            Color.class           , DB_USER,         Color.web(ViewPrefAppService.DEFAULT_FX_COLOR_VALIDATABLE) ),
     GUI_COLOR_MARKED     ( "FX_COLOR_MARK",
                            Color.class           , DB_USER,         Color.web(ViewPrefAppService.DEFAULT_FX_COLOR_MARK    ) ),
     GUI_COLOR_TOOLTIP_FG ( "FX_COLOR_TOOLTIP_TEXT",
                            Color.class           , DB_USER,         Color.web(ViewPrefAppService.DEFAULT_FX_COLOR_TOOLTIP_TEXT ) ),
     GUI_COLOR_TOOLTIP_BG ( "FX_COLOR_TOOLTIP_BACKGROUND",
                            Color.class           , DB_USER,         Color.web(ViewPrefAppService.DEFAULT_FX_COLOR_TOOLTIP_BACKGROUND ) ),
     GUI_COLOR_INDEX_SEARCH( "FX_COLOR_FILTER_INDEX_SEARCH",
                             Color.class          , DB_USER,         Color.web(ViewPrefAppService.DEFAULT_FX_COLOR_FILTER_INDEX_SEARCH ), true ),
     GUI_COLOR_LIST_SEARCH( "FX_COLOR_FILTER_LIST_SEARCH",
                            Color.class           , DB_USER,         Color.web(ViewPrefAppService.DEFAULT_FX_COLOR_FILTER_LIST ), true ),
     GUI_COLOR_EXPR_SEARCH( "FX_COLOR_FILTER_EXPR_SEARCH",
                            Color.class           , DB_USER,         Color.web(ViewPrefAppService.DEFAULT_FX_COLOR_FILTER_EXPR ), true ),
     GUI_FONT             ( "FX_FONT",
                            Font.class            , DB_USER,         Font.getDefault(), true ),
     GUI_FONT_CODE        ( "FX_FONT_CODE",
                            Font.class            , DB_USER,         DEFAULT_FX_FONT_CODE, true ),
     GUI_VECTOR_ICONS     ( "FX_VECTOR_ICONS",
                            Boolean.class         , DB_USER,         Boolean.TRUE, true ),
     GUI_TOOLBAR_BTN_MODE ( "FX_BUTTON_ICON_VIEW",
                            ButtonIconView.class  , DB_USER,         ButtonIconView.ICON, true  ),
     GUI_INPUT_SHORT_DATE ( "FX_CALENDAR_DATE_PRIORITET",
                            Boolean.class         , DB_USER     ),
     GUI_IS_MAXIMIZED_FRAME( "IS_MAXIMIZED_FRAME",  //Увы, теперь распространяется на все приложения
                            Boolean.class         , DB_USER     ),
// DB
     DB_TRACE_PROC       ( "FX_TRACE_PROC",
                            String.class          , DB_USER     ,    JInvSettingsPane.DEFAULT_FX_TRACE_PROC ),
     DB_MAX_RECORDS       ( "MAXIMUM_RECORDS_FETCHED",
                            Long.class          , DB_GLOBAL, 0L, true ),
// CONNECTION
     CON_BIG_T_MAX_RECORD( 3694, "BIG_T_MAX_RECORD",
                            Integer.class,          DB_GLOBAL   ,    1000, false  ),
     CON_BIG_T_MAX_SORTED( 3694, "BIG_T_MAX_SORTED",
                            Integer.class,          DB_GLOBAL   ,    1000, false  ),
// SOUND
     SOUND_LEVEL        ( "SOUND_LEVEL",
                            SoundLevel.class,        DB_USER,         SoundLevel.ALL  ),
// CONNECTION INDICATORS
     IND_ALIAS_LIST      ( null,
                           ConnectionIndicators.class, DB_USER  ),
     IND_BORDER_WIDTH    ( null,
                           Integer.class             , DB_USER   ,   2 ),
// ASSOC
     ASSOC_ASSOC_TXT    ( ASSOC +   "TXT",
                            File.class            , LOCAL_USER   ),
     ASSOC_ASSOC_SQL    ( ASSOC +   "SQL",
                            File.class            , LOCAL_USER   ),
     ASSOC_ASSOC_XML    ( ASSOC +   "XML",
                            File.class            , LOCAL_USER   ),
     ASSOC_ASSOC_DOCX    ( ASSOC +  "DOCX",
                            File.class            , LOCAL_USER   ),
     ASSOC_ASSOC_DOC    ( ASSOC +   "DOC",
                            File.class            , LOCAL_USER   ),
     ASSOC_ASSOC_CSS    ( ASSOC +   "CSS",
                            File.class            , LOCAL_USER   ),
     ASSOC_ASSOC_LOG    ( ASSOC +   "LOG",
                            File.class            , LOCAL_USER   ),
     ASSOC_ASSOC_XLS    ( ASSOC +   "XLS",
                            File.class            , LOCAL_USER   ),
     ASSOC_ASSOC_XLSX    ( ASSOC +  "XLSX",
                            File.class            , LOCAL_USER   ),
     ASSOC_ASSOC_TMP    ( ASSOC +   "TMP",
                            File.class            , LOCAL_USER   ),
     ASSOC_ASSOC_PDF    ( ASSOC +   "PDF",
                            File.class            , LOCAL_USER   ),
     ASSOC_ASSOC_TIF    ( ASSOC +   "TIF",
                            File.class            , LOCAL_USER   ),
     ASSOC_ASSOC_PCX    ( ASSOC +   "PCX",
                          File.class            , LOCAL_USER   ),
     ASSOC_ASSOC_JS    ( ASSOC +   "JS",
                          File.class            , LOCAL_USER   ),
     ASSOC_ASSOC_JPG    ( ASSOC +   "JPG",
                          File.class            , LOCAL_USER   ),

    // XML style
    XML_STYLE_BRACKET ( null, Color.class, DB_USER, Color.RED, true  ),

    XML_STYLE_ANYTAG  ( null, Color.class, DB_USER, Color.BLUE, true  ),

    XML_STYLE_ATTRIBUTE_NAME ( null, Color.class, DB_USER, Color.VIOLET, true  ),

    XML_STYLE_ATTRIBUTE_VALUE( null, Color.class, DB_USER, Color.GREEN, true  ),

    XML_STYLE_NS_NAME    ( null, Color.class, DB_USER, Color.ORANGE, true ),

    XML_STYLE_NS_VALUE   ( null, Color.class, DB_USER, Color.ORANGERED, true  ),

    XML_STYLE_CDATA_WORD ( null, Color.class, DB_USER, Color.BLUE, true  ),

    XML_STYLE_CDATA_VALUE( null, Color.class, DB_USER, Color.GREEN, true  ),

    XML_STYLE_EQUAL      ( null, Color.class, DB_USER, Color.RED, true  ),

    XML_STYLE_COMMENT    ( null, Color.class, DB_USER, Color.GRAY, true ),

    XML_STYLE_HIGHLIGHT  ( null, Color.class, DB_USER, Color.YELLOW, true ),

    XML_STYLE_BACKGROUND ( null, Color.class, DB_USER, Color.WHITESMOKE, true )
    ;

    /** */
    PropertyItemEnum( String name, Class dataType, PropertiesTypeEnum type ) {
        this.dataType        = dataType;
        this.defaultValue    = null;
        this.type            = type;
        this.propertyName    = U.nvl( name, this.name() );
        this.grantId         = 0;
        this.restartRequired = false;
    }

    /** */
    PropertyItemEnum( String name, Class dataType, PropertiesTypeEnum type, Object defaultValue ) {
        this.dataType        = dataType;
        this.defaultValue    = defaultValue;
        this.type            = type;
        this.propertyName    = U.nvl( name, this.name() );
        this.grantId         = 0;
        this.restartRequired = false;
    }

    /** */
    PropertyItemEnum( int grantId, String name, Class dataType, PropertiesTypeEnum type, Object defaultValue, boolean restartRequired ) {
        this.dataType        = dataType;
        this.defaultValue    = defaultValue;
        this.type            = type;
        this.propertyName    = U.nvl( name, this.name() );
        this.grantId         = grantId;
        this.restartRequired = restartRequired;
    }

    /** */
    PropertyItemEnum( String name, Class dataType, PropertiesTypeEnum type, Object defaultValue, boolean restartRequired ) {
        this.dataType        = dataType;
        this.defaultValue    = defaultValue;
        this.type            = type;
        this.propertyName    = U.nvl( name, this.name() );
        this.grantId         = 0;
        this.restartRequired = restartRequired;
    }

    final private Class   dataType;
    final private Object  defaultValue;
    final private int     grantId;
    final private String  propertyName;
    final private boolean restartRequired;
    //final private String  category;

    final private PropertiesTypeEnum type;

    @Override
    public Class< ? > getType() {
        return dataType;
    }

    @Override
    public String getCategory() {

        int index = this.name().indexOf('_');

        if( index == -1 )
            return this.name();
        else
            return this.name().substring( 0, index );
    }

    /**
     * Получить имя страницы без проверки безопасности
     */
    public SettingsPage getPageInsecure() {
        return getPage(null);
    }

    /**
     * Получить имя страницы с проверкой безопасности.
     * В случае провала проверки возвращает SettingsPage.NO_ACCESS
     */
    public SettingsPage getPage(Map<String,Boolean> securityMap) {

        String cat = getCategory().toUpperCase();

        if (securityMap != null){
            for (Map.Entry<String, Boolean> entry : securityMap.entrySet()) {
                if ( (cat.equals(entry.getKey().toUpperCase()) || cat.startsWith(entry.getKey().toUpperCase()) )
                        && !entry.getValue()) {
                    return SettingsPage.NO_ACCESS;
                }
            }
        }

        if (   cat.equals( "GUI" )
            || cat.equals( "I18N")
            || cat.equals( "XML" )
            || cat.equals( "IND" )
            || cat.equals( "LOGIN" )
        )
        {
            return new SettingsPage( "INTERFACE", IconDescriptorBuilder.of( FontAwesome.fa_paint_brush ) );
        }
        if ( cat.equals( "DB" ) || cat.equals( "CON" ) )
        {
            return new SettingsPage("DB", IconDescriptorBuilder.of( FontAwesome.fa_database ) );
        }
        if ( cat.startsWith("MAIL") ){
            return new SettingsPage("MAIL", IconDescriptorBuilder.of( FontAwesome.fa_envelope ) );
        }
        if ( cat.equals( "LOGGER" ) ){
            return new SettingsPage("LOG", IconDescriptorBuilder.of( FontAwesome.fa_bug ) );
        }
        if ( cat.equals( "ASSOC" ) ){
            return new SettingsPage("ASSOC", IconDescriptorBuilder.of( FontAwesome.fa_external_link_square ) );
        }
        if ( cat.equals( "SOUND" ) ){
            return new SettingsPage("SOUND", IconDescriptorBuilder.of( FontAwesome.fa_volume_up ) );
        }
        return new SettingsPage("OTHER" );
    }

    @Override
    public String getName() {
        return propertyName;
    }

    @Override
    public String getDescription( ) {
        return null;
    }

    @Override
    public Object getValue() {
        return defaultValue;
    }

    @Override
    public void setValue( Object value ) {
    }

    @Override
    public Optional< ObservableValue< ? extends Object > > getObservableValue() {
        return null;
    }

    @Override
    public boolean isEditable() {
        return this != LOGGER_FOLDER;
    }

    /** */
    public PropertiesTypeEnum getPropertyType( ) { return type; }

    /** */
    public int getGrantId( ) { return grantId; }

    public boolean isRestartRequired() {
        return restartRequired;
    }
}
