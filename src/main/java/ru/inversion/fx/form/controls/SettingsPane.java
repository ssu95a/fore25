/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ru.inversion.fx.form.controls;

import javafx.scene.layout.GridPane;
import ru.inversion.fx.app.frame.menu.IItemsProvider;

import java.util.List;

/**
 * Компоненты на основе JInvPropertySheet, который умеет сохранять и загружать из базы настройки
 * @author antonovdi
 */
public class SettingsPane extends GridPane implements IItemsProvider {

    @Override
    public List<Object> getItems() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

////    private ResourceBundle foreBundle = ResourceBundle.getBundle("fore");
////    private PSettingsSystem forePojo = new PSettingsSystem();
//    protected JInvPropertySheet sheet;
//    
////    private ResourceBundle bundleLanguage = ResourceBundle.getBundle("languages");
//
//    public SettingsPane() throws AppException {
//
//        initSheetPane();
//        initButtons();
//        downloadParameters();
//        setVgap(5);
//        setHgap(5);
//        
////        if (BaseApp.APP().getViewPrefService().getFrameMode().equals(MDI)) {
//            setPrefSize(400, 650);
////        }
//
//        PropertyChangeListener listenerPojo = (PropertyChangeEvent evt) -> {
//            if (evt.getOldValue() != null && evt.getNewValue() != null) {
//                changedParams.add(evt.getPropertyName());
//            }
//        };
//        
//        forePojo.addPropertyChangeListener(listenerPojo);
//
//    }
//
//    public void setItems(List<SimpleBeanInfo> items){
//        sheet.getItems().addAll(items);
//    }
//    
//    private void initSheetPane() {
//
//        sheet = new JInvPropertySheet();
//        sheet.setMode(PropertySheet.Mode.CATEGORY);
//        sheet.setModeSwitcherVisible(false);
//        sheet.setSearchBoxVisible(false);
//        sheet.setModeSwitcherVisible(true);
//        add(sheet, 0, 0);
//        GridPane.setConstraints(sheet, 0, 0, 1, 1, HPos.LEFT, VPos.CENTER, Priority.ALWAYS, Priority.ALWAYS);
//
//    }
//
//    private void doClose() {
//        try {
//            if (window != null) {
//                BaseApp.APP().getMainFrame().closeWindow(window);
//            } else {
//                fireEvent(new WindowEvent(
//                    this.getScene().getWindow(),
//                    WindowEvent.WINDOW_CLOSE_REQUEST
//                ));
//            }
//        } catch (Throwable ex) {
//            JInvErrorService.handleException(null, ex);
//        }
//    }
//
//    private void initButtons() {
//
//        Button okButton = new Button(foreBundle.getString("OK"));
//        okButton.setOnAction((ActionEvent event1) -> {
//            try {
//
//                saveToDbChangedParams();
//                changeLoggerParameters();
//                changeViewPref();
//                changeLocale();
//                doClose();
//            } catch (Throwable ex) {
//                JInvErrorService.handleException(null, ex);
//            }
//        });
//
//        Button cancelButton = new Button(foreBundle.getString("CANCEL"));
//
//        cancelButton.setOnAction(event2 -> {
//            doClose();
//        });
//
//        Button revertButton = new Button(foreBundle.getString("SETTINGS_BUTTON_REVERT"));
//
//        revertButton.setOnAction(event2 -> {
//
//            if (Alerts.yesNo(getScene().getWindow(), null, foreBundle.getString("SETTINGS_ALERT_REVERT"))) {
//                downloadParameters();
//                initDefaultSettings();
//                getChildren().remove(sheet);
//                initSheetPane();
//            }
//        });
//
//        HBox buttonBox = new HBox(5, revertButton, okButton, cancelButton);
//
//        buttonBox.setAlignment(Pos.CENTER_RIGHT);
//        add(buttonBox, 0, 1);
//        GridPane.setConstraints(buttonBox, 0, 1, 1, 1, HPos.RIGHT, VPos.CENTER, Priority.ALWAYS, Priority.ALWAYS, new Insets(5));
//    }
//
//    private void downloadParameters() {
//
//        List<PropertySheet.Item> listItem = sheet.getItems();
//
//        listItem.stream().map((item) -> (SimpleBeanInfo) item).forEach((info) -> {
//            PropertiesTypeEnum typeProp = info.getTypeProp();
//            if (typeProp != null) {
//                String name = info.getPropertyName();
//                IAppProperties appProperties = BaseApp.APP().getProperties(typeProp);
//                if (appProperties != null) {
//                    Object value = appProperties.getProperty(name);
//                    if (value != null) {
//                        Class classOfProperty = info.getType();
//                        if (classOfProperty == Boolean.class) {
//                            if (value.equals("1")) {
//                                value = Boolean.TRUE;
//                            } else {
//                                value = Boolean.FALSE;
//                            }
//                        } else if (classOfProperty == LoggerTimingEnum.class) {
//                            value = LoggerTimingEnum.fromLogString((String) value);
//                        } else if (classOfProperty.isEnum()) {
//                            value = Enum.valueOf(classOfProperty, (String) value);
//                        } else if (classOfProperty == Color.class) {
//                            value = Color.web((String) value);
//                        } else if (classOfProperty == Font.class) {
////                            value = ViewPrefAppService.DEFAULT_FX_FONT;
//                            value = ViewPrefAppService.getFontFromString((String) value);
//                        } else {
//                            value = TypeConverter.convert(value, classOfProperty);
//                        }
//                        info.setValue(value);
//                    }
//                }
//            }
//        });
//    }
//
//    private void saveToDbChangedParams() {
//
//        //BaseApp.APP_CACHE().getProperties(PropertiesTypeEnum.DB_USER).setProperty( BaseApp.APP_CACHE().getAppID() + "_locale", locale.getLocale().getLanguage() );
//        for (String parameterName : changedParams) {
//            SimpleBeanInfo info = getInfoByName(parameterName);
//            String propertyName = info.getPropertyName();
//            PropertiesTypeEnum typeOfProperty = info.getTypeProp();
//            PREF_AppProperties pref = (PREF_AppProperties) BaseApp.APP().getProperties(typeOfProperty);
//            Object propertyValue = info.getValue();
//            String stringValue = null;
//            if (propertyValue instanceof File) {
//                stringValue = ((File) propertyValue).getPath();
//            } else if (propertyValue instanceof String) {
//                stringValue = (String) propertyValue;
//            } else if ((propertyValue instanceof Boolean)) {
//                {
//                    if ((Boolean) propertyValue.equals(Boolean.TRUE)) {
//                        stringValue = "1";
//                    } else {
//                        stringValue = "0";
//                    }
//                }
//            } else if (propertyValue instanceof Enum) {
//
//                if (propertyValue instanceof LoggerTimingEnum) {
//                    stringValue = ((LoggerTimingEnum) propertyValue).toLogString();
//                } else {
//                    stringValue = ((Enum) propertyValue).name();
//                }
//            } else if (propertyValue instanceof Color) {
//
//                Color color = ((Color) propertyValue);
//                stringValue = String.format("#%02X%02X%02X", (int) (color.getRed() * 255), (int) (color.getGreen() * 255), (int) (color.getBlue() * 255));
//
//            } else if (propertyValue instanceof NamedLocale) {
//                propertyName = BaseApp.APP().getAppID() + "_locale";
//                stringValue = ((NamedLocale) propertyValue).getLocale().getLanguage();
//            } else if (propertyValue instanceof Font) {
//                Font font = (Font) propertyValue;
//                stringValue = ViewPrefAppService.getStringFromFont(font);
//            } else if (propertyValue != null) {
//                stringValue = propertyValue.toString();
//            }
//            pref.setProperty(propertyName, stringValue);
//        }
//    }
//
//    private SimpleBeanInfo getInfoByName(String parameterName) {
//
//        for (PropertySheet.Item item : sheet.getItems()) {
//            if (((SimpleBeanInfo) item).getPropertyName().equals(parameterName)) {
//                return (SimpleBeanInfo) item;
//            }
//        }
//        return null;
//    }
//
//    @Override
//    public List<Object> getItems() {
//
//        List<Object> listLocales = new ArrayList<>();
//        Enumeration<String> enLanguages = bundleLanguage.getKeys();
//        for (String language : Collections.list(enLanguages)) {
//            Locale locale = LocaleUtils.toLocale(language);
//            listLocales.add(new NamedLocale(locale.getDisplayLanguage(), locale));
//        }
//        return listLocales;
////        cbLanguage.setSelectedItem(new NamedLocale(Locale.getDefault().getDisplayLanguage(), Locale.getDefault()));
//    }
//
//    private void changeLoggerParameters() {
//
//        if (changedParams.contains(LogManager.PROPERTY_DEFAULT_ROOTLOGGER_LEVEL) || changedParams.contains(LogManager.PROPERTY_DEFAULT_ROOTLOGGER_HISTORY)
//            || changedParams.contains(LogManager.PROPERTY_DEFAULT_ROOTLOGGER_TIMING) || changedParams.contains(LogManager.PROPERTY_DEFAULT_ROOTLOGGER_FILE_PATTERN)) {
//            LogManager.INSTANCE().refreshLoggerConfig();
//        }
//    }
//
//    private void changeLocale() {
//        if (changedParams.contains("LANGUAGE")) {
//            BaseApp.APP().refreshLocale();
//        }
//    }
//
//    private void changeViewPref() throws AppException {
//
//        ViewPrefAppService service = BaseApp.APP().getViewPrefService();
//        service.refresh();
//        if (BaseApp.APP().getMainFrame() != null) {
//            BaseApp.APP().getMainFrame().refreshViewSettings();
//        }
//    }
//
//    public void setWindow(Window w) {
//        this.window = w;
//    }
//
//    private Window window;

}
