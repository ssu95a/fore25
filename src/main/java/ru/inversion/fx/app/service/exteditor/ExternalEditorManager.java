/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ru.inversion.fx.app.service.exteditor;
import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.event.ActionEvent;
import javafx.scene.Node;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.stage.FileChooser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.inversion.fx.app.BaseApp;
import ru.inversion.fx.app.es.JInvErrorService;
import ru.inversion.fx.app.property.IAppProperties;
import ru.inversion.fx.app.property.PropertiesTypeEnum;
import ru.inversion.fx.form.Alerts;
import ru.inversion.fx.form.JInvFXFormController;
import static ru.inversion.fx.form.JInvFileChooser.showOpenDialog;
import ru.inversion.fx.form.ViewContext;
import ru.inversion.fx.form.action.ActionBuilder;
import ru.inversion.fx.form.action.IAction;
import ru.inversion.fx.form.action.JInvEvent;
import ru.inversion.fx.form.controls.JInvTextArea;
import ru.inversion.utils.S;
import ru.inversion.utils.Tags;
import ru.inversion.utils.U;
import ru.inversion.utils.converter.TypeConverter;

/**
 *
 * @author antonovdi
 */
public class ExternalEditorManager {
    private final static Logger logger = LoggerFactory.getLogger( MethodHandles.lookup().lookupClass() );

    public static final String ASSOC = "LAUNCH_ON_";
    private static ExternalEditorManager instance;
    private Map<ExtensionType, String> mapExtensionType = new HashMap<>();

    private static final ResourceBundle bundle = ResourceBundle.getBundle("fore");
    private IAction removeExtEditorSettingsAction;

    private ObjectProperty<Object> settingsUpdatedEvent = new SimpleObjectProperty<>();

    private ExternalEditorManager() {
        initialize();
    }

    public static ExternalEditorManager INSTANCE() {
        if (instance == null) {
            instance = new ExternalEditorManager();
        }
        return instance;
    }

    public ObjectProperty<Object> settingsUpdatedEvent() {
        return settingsUpdatedEvent;
    }

    /** */
    public <T> T editInExternalEditor( T content, ExtensionType type ) throws IOException, InterruptedException {
        reloadExtensionSettings();

        ExtensionType extType = U.nvl( type, ExtensionType.TXT );

        String stringContent = S.EMPTY_STRING;
        Class clazz         = String.class;

        if( content != null ) {
            clazz         = content.getClass();
            stringContent = TypeConverter.convert( content, String.class);

            if( stringContent == null )
                stringContent = S.EMPTY_STRING;
        }

        File file = File.createTempFile( BaseApp.APP().getAppID(), "." + extType.toString() );

        if( !stringContent.isEmpty() ) {
            //JAVAKERNEL-1336 holy notepad
            String stringContentCrLf = stringContent.replaceAll( "(?<!\\r)\\n", "\r\n" );
            Files.write( file.toPath(), stringContentCrLf.getBytes() );
        }

        openFileAndWait(file);

        String newValue = new String( Files.readAllBytes(file.toPath()) );

        Files.delete( file.toPath() );

        return TypeConverter.convert( newValue, (Class<T>)clazz );
    }

    /**
     Открыть переданный файл
     @param file
     @throws IOException
     */
    public void openFile(File file) throws IOException {
        try {
            openFile( file, false );
        } catch ( InterruptedException ignored ) {} //never thrown if wait == false
    }

    /**
     Открыть переданный файл и дождаться окончания процесса
     @param file
     @throws IOException
     @throws InterruptedException
     */
    public void openFileAndWait(File file) throws IOException, InterruptedException {
        openFile(file, true);
    }

    private void openFile(File file, boolean wait) throws IOException, InterruptedException {

        //for registered extension types:
        for ( final ExtensionType extension : ExtensionType.values() ) {

            boolean isMatch = file.getName().toLowerCase().endsWith( '.' + extension.toString().toLowerCase() );
            if ( !isMatch ) {
                continue;
            }

            String pathToEditor = mapExtensionType.get( extension );
            if ( S.isNotNullOrEmpty( pathToEditor ) ) {
                logger.info( "trying to open {} as {}{}", file, extension, wait ? " - waiting!" : S.EMPTY_STRING );
                ProcessBuilder pb = new ProcessBuilder( pathToEditor, file.getAbsolutePath() );
                Process p = pb.start();
                if ( wait ){
                    p.waitFor();
                }
                return;
            } else {
                if ( wait ){
                    //
                    throw new IOException( Tags.PRODUCT + " Editor is not configured for '" + extension.name() + "' extension" );
                }
                //open in system
                break;
            }
        }

        logger.info( "trying to open {} in system", file );
        Desktop.getDesktop().open(file);
    }

    /**
     Выбрать, а затем открыть выбранный файл в системе
     */
    public static void selectAndOpen( ViewContext vc ) {
        selectAndOpen( vc, null );
    }
    /**
     Выбрать, а затем открыть выбранный файл в системе
     */
    public static void selectAndOpen( ViewContext vc, File initialDirectory ) {
        if ( initialDirectory == null ){
            initialDirectory = Paths.get(System.getProperty("user.home")).toFile();
        }
        try {
            File selectedFile = showOpenDialog( vc.getStageOrPrimaryStage(), initialDirectory );
            if ( selectedFile != null ){
                INSTANCE().openFile( selectedFile );
            }
        } catch ( IOException e ) {
            JInvErrorService.handleException( vc, e );
        }
    }

    private void initialize() {
        reloadExtensionSettings();
        settingsUpdatedEvent.addListener( (v,o,n) -> {
            reloadExtensionSettings();
        } );

        removeExtEditorSettingsAction = new ActionBuilder().setKeyCombination(new KeyCodeCombination(KeyCode.F4, KeyCombination.CONTROL_DOWN)).
            setHandler((ActionEvent event) -> {

                if (event instanceof JInvEvent) {
                    JInvEvent<KeyEvent> param = (JInvEvent<KeyEvent>) event;
                    Node node = param.getControl();
                    if (node instanceof JInvTextArea) {
                        ExtensionType type = ((JInvTextArea) node).getExtensionType();
                        ViewContext vc = null;
                        JInvFXFormController controller = ((JInvTextArea) node).getController();
                        if(controller!=null){
                            vc = controller.getViewContext();
                        }
                        removeEditorPreference(type, vc);
                    }
                }
            }).build();
    }
    public IAction getRemoveExtEditorSettingsAction(){
        return removeExtEditorSettingsAction;
    }

    private void reloadExtensionSettings() {
        mapExtensionType.clear();

        IAppProperties prefs = BaseApp.APP().getProperties(PropertiesTypeEnum.LOCAL_USER);
        for (ExtensionType type : ExtensionType.values()) {
            String value = prefs.getStringProperty(ASSOC + type.name(), null);
            if (value != null && !value.isEmpty()) {
                mapExtensionType.put(type, value);
            }
        }
    }

    public boolean checkExistencePathToEditor(ViewContext vc, ExtensionType type) {
        reloadExtensionSettings();

        boolean result = false;

        if (!mapExtensionType.containsKey(type)) {
            File file = askUserToChoiceEditorPath(vc, type);
            if (file != null) {
                mapExtensionType.put(type, file.getAbsolutePath());
                result = true;
                savePreferences();
            }
        } else {
            result = true;
        }

        return result;
    }

    /**
     * Необходимо запускать из потока отрисовки
     */
    private File askUserToChoiceEditorPath(ViewContext vc,  ExtensionType type) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle(MessageFormat.format(bundle.getString("CHOOSE_EXTERNAL_EDITOR_TITLE"), type));
        return chooser.showOpenDialog(vc.getStageOrPrimaryStage());
    }

    public void savePreferences() {

        if (!mapExtensionType.isEmpty()) {
            IAppProperties prefs = BaseApp.APP().getProperties(PropertiesTypeEnum.LOCAL_USER);
            mapExtensionType.keySet().forEach((type) -> {
                prefs.setProperty(ASSOC + type.name(), mapExtensionType.get(type));
            });
        }
    }

    public void removeEditorPreference(ExtensionType type, ViewContext vc) {

        IAppProperties prefs = BaseApp.APP().getProperties(PropertiesTypeEnum.LOCAL_USER);
        // Приходится ставить пустую строчку ибо иначе летит ислючение
        prefs.setProperty(ASSOC + type.name(), "");
        mapExtensionType.remove(type);
        Alerts.info(vc, MessageFormat.format(bundle.getString("REMOVE_EXTERNAL_EDITOR"), type));
    }
}
