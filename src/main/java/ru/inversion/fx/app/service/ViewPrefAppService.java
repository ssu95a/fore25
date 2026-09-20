package ru.inversion.fx.app.service;

import com.sun.javafx.css.StyleManager;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Dimension2D;
import javafx.scene.ImageCursor;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.control.SplitPane.Divider;
import javafx.scene.image.Image;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontPosture;
import javafx.scene.text.FontWeight;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.inversion.dataset.fx.DSFXAdapter;
import ru.inversion.fx.app.AppConstants;
import ru.inversion.fx.app.AppException;
import ru.inversion.fx.app.BaseApp;
import ru.inversion.fx.app.es.JInvErrorService;
import ru.inversion.fx.app.frame.JInvMainFrame.JInvFrameMode;
import ru.inversion.fx.app.frame.menu.ConnectionIndicators;
import ru.inversion.fx.app.frame.menu.PropertyItemEnum;
import ru.inversion.fx.app.property.IAppProperties;
import ru.inversion.fx.app.property.PropertiesTypeEnum;
import ru.inversion.fx.app.service.view.ButtonIconView;
import ru.inversion.fx.form.AbstractBaseController;
import ru.inversion.fx.form.JInvFXFormController;
import ru.inversion.fx.form.ViewContext;
import ru.inversion.fx.form.controls.*;
import ru.inversion.fx.form.controls.table.TripleBoolValueEnum;
import ru.inversion.fx.form.controls.treetable.JInvTreeTable;
import ru.inversion.fx.form.controls.treetable.JInvTreeTableColumn;
import ru.inversion.fx.form.mdi.JInvWindowMdi;
import ru.inversion.fx.form.valid.ValidViewDecorator.LabelStyleEnum;
import ru.inversion.tc.TaskContext;
import ru.inversion.utils.Holder;
import ru.inversion.utils.S;
import ru.inversion.utils.U;
import ru.inversion.utils.converter.TypeConverter;

import java.io.File;
import java.io.FileWriter;
import java.io.Writer;
import java.nio.file.Files;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static ru.inversion.fx.app.frame.JInvMainFrame.NAME;
import static ru.inversion.fx.app.frame.menu.PropertyItemEnum.*;
import static ru.inversion.fx.app.property.PropertiesTypeEnum.DB_USER;
import static ru.inversion.fx.form.action.decorator.ProgressFormStateDecorator.PROPERTY_DECORATOR;
import static ru.inversion.fx.form.controls.JInvTableColumn.COLUMN_MARK;
import static ru.inversion.fx.form.controls.table.TableSettingItemEnum.*;
import static ru.inversion.fx.form.valid.ValidViewDecorator.DEFAULT_VALID_LABEL_STYLE;

/**
 * @author antonovdi
 */
public class ViewPrefAppService implements IAppService {
    public static final Dimension2D DEFAULT_WINDOW_SIZE = new Dimension2D(800,600);
    //Подробная отладка параметров формы при загрузке и сохранении
    public static final boolean DEBUG_FORM_PARAMETERS = false;
//    public static final String PROPERTY_FX_COLOR_VALIDATABLE = "FX_COLOR_VALIDATABLE";
    public static final String PROPERTY_FX_SAVE_MARK = "PROPERTY_FX_SAVE_MARK";
    public static final String  DEFAULT_FX_COLOR_MARK = "#ffffcc";
    public static final String  DEFAULT_FX_COLOR_REQUIRED = "#BAE4CA";
    public static final String  DEFAULT_FX_COLOR_VALIDATABLE = "#66ffff66";
    public static final Font    DEFAULT_FX_FONT = Font.getDefault();
    public static final Font    DEFAULT_FX_FONT_CODE = Font.font("Monospaced", 13);
    public static final Boolean DEFAULT_FX_LEFT_MARK_POSITION = Boolean.TRUE;
    public static final Boolean DEFAULT_FX_VECTOR_ICONS = Boolean.TRUE;
    public static final Boolean DEFAULT_FX_SAVE_MARK = Boolean.TRUE;
    public static final double DEFAULT_FONT_SIZE = 12.0;
    public static JInvFrameMode DEFAULT_FX_FRAME_MODE = JInvFrameMode.SDI;
    public static final int     DEFAULT_FX_LOV_SMALL_ROW_COUNT = 25;
    public static final String  DEFAULT_FX_COLOR_TOOLTIP_BACKGROUND = "#ffffcc";
    // F7 filter
    public static final String  DEFAULT_FX_COLOR_FILTER_INDEX_SEARCH = "#ffffcc";
    public static final String  DEFAULT_FX_COLOR_FILTER_LIST = "#99CCCC";
    public static final String  DEFAULT_FX_COLOR_FILTER_EXPR = "#E6CCFF";

    public static final String  DEFAULT_FX_COLOR_TOOLTIP_TEXT = "black";
    public static final ButtonIconView DEFAULT_FX_BUTTON_ICON_VIEW = ButtonIconView.ICON;
    public static final String PROPERTY_ELEMENT_SPLIT_DIVIDER = "divider";
    public static final Double DIMENSION_FRACTIONAL_FACTOR = 100000000.0;
    public static final String PROPERTY_UI_PREFS = "ru.inversion.ui_prefs";
    public static final String FORM_START_POINT = "FORM_START_POINT";
    public static final String SERVICE_ID = "APPSERVICE_VIEW_PREF";

    public static double widthWindowBorder;
    public static double heightWindowTitle;
    private static JInvFrameMode frameMode = JInvFrameMode.SDI;
    private static ImageCursor helpCursor = new ImageCursor(new Image( "img/help.png" ));
    private static Image appIcon;
    private static ResourceBundle bundle = ResourceBundle.getBundle("fore");
    private static ResourceBundle buttonBundle = ResourceBundle.getBundle("alerts");
    private static Logger logger = LoggerFactory.getLogger("ru.inversion");
    private static Font font;
    private static Font codeFont;
    static private ConnectionIndicators indicators;

    public void refreshViewSettingsRoot(Parent root) {
        refreshViewSettingsRoot(root, true);
    }

    /**
     * Применение/сброс настроек отображения для панели, которая располагается на Stage.
     * @param root
     */
    public void refreshViewSettingsRoot(Parent root, boolean iconRefresh) {
        try {

            if( BaseApp.APP().isAfterLogin() )
            {

                font = null;
                codeFont = null;

                initUIPrefs(root);

                // Шрифт
                refreshFontNode(root, getFont());

                // Цвета
                refreshColors(root);

                // Размеры таблиц и колонок
                refreshTableSize(root);
                refreshTreeTableSize(root);

                // Размеры окна
                refreshStageSize(root);

                // Иконка в левом верхнем углу окна
                if (iconRefresh) {
                    refreshIcon(root);
                }

                indicators = null;

                // Расположение сплитов
                root.getScene().getWindow().showingProperty().addListener(new ChangeListener<Boolean>() {
                    //Сработали разок – и хватит
                    boolean triggeredOnce = false;

                    @Override
                    public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
                        if ( triggeredOnce ){
                            return;
                        }

                        if (newValue) {
                            refreshSplitPosition(root);
//                            refreshTableColumnPosition(root);
                            triggeredOnce = true;
                        }
                    }
                });

            }
        } catch (Throwable ex) {
            JInvErrorService.handleException(null, ex);
        }
    }

    private void initUIPrefs(Parent root) throws AppException {

        String formName = null;
        JInvFXFormController controller = Controls.getControllerFromControl(root);

        if (controller != null) {
            formName = controller.getViewContext().getFormName();
        } else if (root.getId() != null && root.getId().equals(NAME)) {
            formName = NAME;
        }

        if (formName != null && !formName.isEmpty()) {
            //load
            List<PPrefComponent> listPref = ViewPrefDao.loadPreferences(formName);
            if (DEBUG_FORM_PARAMETERS){
                logger.info("Loaded form parameters:\n{}",
                        listPref.stream().map(String::valueOf).collect(Collectors.joining("\n","[","]")));
            }
            if (controller != null) {
                ((ViewPrefFormService) controller.getViewContext().getViewPrefSaver()).setInitialPrefs(listPref);
            }
            root.getProperties().put(PROPERTY_UI_PREFS, listPref);
        }

    }

    public void refreshFontNode(Node root) {
        refreshFontNode(root, getFont());
    }

    public void refreshFontNode(Node root, Font font) {
        String fontStyle = font.getStyle();
        fontStyle = fontStyle.replaceAll("[Rr]egular", "normal");
        String fontString = String.format("-fx-font:%s %dpx \"%s\";", fontStyle, (int) font.getSize(), font.getFamily());
        root.setStyle(fontString);
    }

    private void refreshColors(Parent root) {
        root.setStyle(root.getStyle().concat("-fx-inversion-mark-color:" + getColorMark() + ";"));
        root.setStyle(root.getStyle().concat("-fx-inversion-required-color:" + getColorRequired() + ";"));
        root.setStyle(root.getStyle().concat("-fx-inversion-validatable-color:" + getColorValidatable() + ";"));
        root.setStyle(root.getStyle().concat("-fx-inversion-tooltip-text-color:" + getColorTooltipText() + ";"));

        String colorTooltipBackground = getColorTooltipBackground();
        if (!colorTooltipBackground.equals(DEFAULT_FX_COLOR_TOOLTIP_BACKGROUND)) {
            root.setStyle(root.getStyle().concat("-fx-inversion-tooltip-background-color:" + colorTooltipBackground + ";"));
        }
    }

    /** */
    public void initXMLViewer( )
    {

        final Function<Color, String> toRGBCode = new Function< Color, String >() {
            @Override
            public String apply( Color color ) {
                return String.format("#%02X%02X%02X", (int) (color.getRed() * 255), (int) (color.getGreen() * 255), (int) (color.getBlue() * 255));
            }
        };

        final IAppProperties ap = BaseApp.APP().getProperties(DB_USER);

        StringBuilder sb = new StringBuilder();

        final int sl = "XML_STYLE_".length();

        for( PropertyItemEnum p : U.iterable (
            U.toIterator (
                XML_STYLE_BRACKET, XML_STYLE_ANYTAG, XML_STYLE_ATTRIBUTE_NAME, XML_STYLE_ATTRIBUTE_VALUE,
                XML_STYLE_NS_NAME, XML_STYLE_NS_VALUE, XML_STYLE_CDATA_WORD,
                XML_STYLE_CDATA_VALUE, XML_STYLE_EQUAL,
                XML_STYLE_COMMENT, XML_STYLE_HIGHLIGHT,
                XML_STYLE_BACKGROUND
            )
        ))
        {
            if( p == XML_STYLE_HIGHLIGHT )
                sb.append(".highlight {-rtfx-background-color: ")
                        .append( ap.getStringProperty( p.getName(), toRGBCode.apply( (Color)p.getValue() ) ) )
                        .append(";}\n");
            else if( p == XML_STYLE_BACKGROUND )
                sb.append(".code-area {-fx-background-color: ")
                        .append( ap.getStringProperty( p.getName(), toRGBCode.apply( (Color)p.getValue() ) ) )
                        .append(";}\n");
            else
                sb.append('.').append( p.getName().substring(sl).toLowerCase() )
                        .append(" {-fx-fill:")
                        .append( ap.getStringProperty( p.getName(), toRGBCode.apply( (Color)p.getValue() ) ) )
                        .append(";}\n");
        }

        String styleData = sb.toString();

        try {

            final File tempFile = Files.createTempFile( "xvr", ".css" ).toFile();
            tempFile.deleteOnExit();

            try( Writer w = new FileWriter( tempFile ) )
            {
                w.write( styleData );
            }

            StyleManager.getInstance().addUserAgentStylesheet( tempFile.toURL().toExternalForm() );
        }
        catch( Exception ignored )
        {
            ignored.printStackTrace();
        }
    }

    /** */
    private void refreshStageSize( Parent root ) {

        if( root instanceof Pane )
        {
            javafx.stage.Stage stage = (javafx.stage.Stage) root.getScene().getWindow();
            Pane   pane = (Pane ) root;

            // Если окно расширено на весь экран ничего не делаем
            if( stage.isMaximized() ) {
                return;
            }

            StringBuilder sb = new StringBuilder("refreshStageSize:\n");
            sb.append("original heightWindowTitle = ").append(heightWindowTitle)
              .append("; widthWindowBorder = ").append(widthWindowBorder).append('\n');

            // Если не определили высоту заголовка при открытии главного меню по какой-то причине, пробуем это вновь
            if( heightWindowTitle == 0 && !Double.isNaN(stage.getHeight()) && !Double.isNaN(pane.getScene().getHeight()) ) {
                heightWindowTitle = stage.getHeight() - pane.getScene().getHeight();
                sb.append("new heightWindowTitle = ").append(heightWindowTitle).append('\n');
            }
            if( widthWindowBorder == 0 && !Double.isNaN(stage.getWidth()) && !Double.isNaN(pane.getScene().getWidth()) ) {
                widthWindowBorder = stage.getWidth() - pane.getScene().getWidth();
                sb.append("new widthWindowBorder = ").append(widthWindowBorder).append('\n');
            }

            Double width = null;
            Double height = null;
            Double titleHeight = heightWindowTitle;
            Double borderWidth = widthWindowBorder;

            // Предпочитаемый размер
            // Если имеется сохраненный размер, то берем его. Если нет, то берем preferredSize у панели
            Dimension2D savedDimensions = null;

            JInvFXFormController controller = Controls.getControllerFromControl(root);
            if (controller == null || !controller.isDisableSavedDimensions()) {
                savedDimensions = getSavedDimensions(root);
                sb.append("savedDimensions = ").append(savedDimensions).append('\n');
            }

            //Пытаемся узнать предпочтительные размеры формы
            double prefWidth;
            double prefHeight;
            boolean changeDimensions = true;

            {
                if (pane.getPrefHeight() != 0 && pane.getPrefHeight() != -1.0) {
                    prefHeight = pane.getPrefHeight();
                    sb.append("prefHeight normal = ").append(prefHeight).append('\n');
                } else if (!Double.isNaN(stage.getHeight())) {
                    prefHeight = stage.getHeight();
                    titleHeight = 0.0;
                    sb.append("prefHeight stage = ").append(prefHeight).append('\n');
                } else if (pane.getPrefHeight() == -1.0) {
                    //некоторые формы не указывают высоту, и размер их заранее непонятен.
                    //не присваиваем размеры таким формам
                    changeDimensions = false;
                    prefHeight = pane.getPrefHeight();
                    sb.append("prefHeight unknown or maximised = ").append(prefHeight).append('\n');
                } else {
                    prefHeight = DEFAULT_WINDOW_SIZE.getHeight();
                    sb.append("prefHeight default = ").append(prefHeight).append('\n');
                }

                if (pane.getPrefWidth() != 0 && pane.getPrefWidth() != -1.0) {
                    prefWidth = pane.getPrefWidth();
                    sb.append("prefWidth normal = ").append(prefWidth).append('\n');
                } else if (!Double.isNaN(stage.getWidth())) {
                    prefWidth = stage.getWidth();
                    sb.append("prefWidth stage = ").append(prefWidth).append('\n');
                } else if (pane.getPrefWidth() == -1.0) {
                    //некоторые формы не указывают ширину, и размер их заранее непонятен.
                    //не присваиваем размеры таким формам
                    changeDimensions = false;
                    prefWidth = pane.getPrefWidth();
                    sb.append("prefWidth unknown = ").append(prefWidth).append('\n');
                } else {
                    prefWidth = DEFAULT_WINDOW_SIZE.getWidth();
                    sb.append("prefWidth default = ").append(prefWidth).append('\n');
                }

                if (changeDimensions){
                    // Рассчитаем размер относительно текущего шрифта
                    height = computeSize(prefHeight) + titleHeight;
                    width = computeSize(prefWidth) + borderWidth;
                }
            }

            //не используем сохранённые размеры, если они меньше предпочтительных
            //в случае с полноэкраном сравниваем со стандартными размерами окна
            if ( (prefHeight != -1 && isSameOrLarger(savedDimensions, prefWidth, prefHeight))
              || (prefHeight == -1 && isSameOrLarger(savedDimensions, DEFAULT_WINDOW_SIZE.getWidth(), DEFAULT_WINDOW_SIZE.getHeight())) )
            {
                changeDimensions = true;
                sb.append("using savedDimensions. changeDimensions = true").append('\n');
                prefWidth = savedDimensions.getWidth();
                prefHeight = savedDimensions.getHeight();
                width     = prefWidth;
                height    = prefHeight;
            } else {
                sb.append("not using savedDimensions").append('\n');
            }

            sb.append("prefWidth = ").append(prefWidth).append('\n');
            sb.append("prefHeight = ").append(prefHeight).append('\n');
            sb.append("width = ").append(width).append('\n');
            sb.append("height = ").append(height).append('\n');
            sb.append("changeDimensions = ").append(changeDimensions).append('\n');

            if (changeDimensions){

                stage.setWidth (width);
                stage.setHeight(height);

                // Разворачиваем окно, если ранее сохраняли с высотой -1 или форма такая по умолчанию
                boolean isMaximised = isMaximised(savedDimensions)
                        || (savedDimensions == null && prefHeight == -1);
                if (isMaximised) {
                    sb.append("using maximised window").append('\n');
                    Platform.runLater(() -> {
                        ((javafx.stage.Stage) root.getScene().getWindow()).setMaximized(true);
                    });
                } else {
                    sb.append("not maximising window").append('\n');
                }

                // Минимальный размер
                if (isFiniteSize(pane.getMinHeight())) {
                    double minHeight = computeSize(pane.getMinHeight()) + titleHeight;
                    stage.setMinHeight(minHeight);
                    sb.append("minHeight = ").append(minHeight).append('\n');
                }
                if (isFiniteSize(pane.getMinWidth())) {
                    double minWidth = computeSize(pane.getMinWidth()) + borderWidth;
                    stage.setMinWidth(minWidth);
                    sb.append("minWidth = ").append(minWidth).append('\n');
                }

                // Максимальный размер
                if (isFiniteSize(pane.getMaxHeight())) {
                    double maxHeight = computeSize(pane.getMaxHeight()) + titleHeight;
                    stage.setMaxHeight(maxHeight);
                    sb.append("maxHeight = ").append(maxHeight).append('\n');
                }
                if (isFiniteSize(pane.getMaxWidth())) {
                    double maxWidth = computeSize(pane.getMaxWidth()) + borderWidth;
                    stage.setMaxWidth(maxWidth);
                    sb.append("maxWidth = ").append(maxWidth).append('\n');
                }
            }

            if (DEBUG_FORM_PARAMETERS){
                logger.info(sb.toString());
            }
        }
    }

    /** */
    private boolean isSameOrLarger(Dimension2D dimensions, double thanWidth, double thanHeight) {
        return dimensions != null && dimensions.getWidth() >= thanWidth && dimensions.getHeight() >= thanHeight;
    }

    private boolean isMaximised(Dimension2D dimensions) {
        return dimensions != null && dimensions.getHeight() == -1;
    }

    public static String getStringFromFont( Font font ) {
        String result = null;
        try {
            result = String.format( "%s&%s&%d", font.getFamily(), font.getStyle(), (int) font.getSize() );
        } catch ( Throwable ignored ) {}
        return result;
    }

    private static Double computeAppIconSize(double fontSize) {
        return fontSize * 1.4 * (fontSize / DEFAULT_FONT_SIZE);
    }

    public static Image getAppIcon(double fontSize) {

        double size = computeAppIconSize(fontSize);

        if (appIcon == null) {
            try {
                appIcon = new Image("/img/app_icon.png", size, size, false, false);
            } catch (IllegalArgumentException e) {
                try {
                    appIcon = new Image("/img/app_icon.gif", size, size, false, false);
                } catch (IllegalArgumentException ex) {
                    appIcon = new Image("/img/inversia.png", size, size, false, false);
                }
            }
        }
        return appIcon;
    }

    /** */
    public static void deleteDimensions(String formName)
    {
        final Connection con = BaseApp.APP().getCommonTaskContext().getConnection();
        final String sql = "DELETE FROM V_JF_PREF_COMPONENT WHERE form_name = ?";

        try ( CallableStatement cs = con.prepareCall( sql ) ) {
            cs.setString(1, formName);
            cs.execute();
            //con.commit(); - autoCommit - true
        } catch (Throwable ex) {
            JInvErrorService.handleException(null, ex);
        }
    }

    public static void saveFormParameters(ViewContext viewContext, String formName, boolean saveDimension) throws Exception {
        List<PPrefComponent> list = new ArrayList<>();

        if (saveDimension) {
            fillSplitPositions(viewContext, formName, list);
            fillFormDimension(viewContext, formName, list);
            //fillLeftUpPoint(viewContext, formName, list);
            fillTableColumns(viewContext, formName, list);
            fillTreeTableColumns(viewContext, formName, list);
            fillTabPanes(viewContext, formName, list);
        }

        fillEnableFilter(viewContext, formName, list);

        if (viewContext != null) {
            IViewPrefSaver saver = viewContext.getViewPrefSaver();
            if (DEBUG_FORM_PARAMETERS){
                logger.info("Saving form parameters:\n{}",
                        list.stream().map(String::valueOf).collect(Collectors.joining("\n","[","]")));
            }
            saver.addAll(list);
            saver.save();
        }
    }

    private static void fillTabPanes( final ViewContext viewContext, final String formName, final List<PPrefComponent> list ) {
        if (viewContext != null && viewContext.getContentPane() != null) {

            List<JInvTabPane> listTabPane = Controls.getNodeList(viewContext.getContentPane(),
                    (Node t) -> t instanceof JInvTabPane)
                    .stream()
                    .map((Node t) -> (JInvTabPane) t)
                    .collect(Collectors.toList());

            for (JInvTabPane tabPane : listTabPane) {
                String tabPaneId = tabPane.getId();
                if (tabPaneId == null || tabPaneId.isEmpty()) {
                    logger.error("Save pref. TabPane name is null");
                    continue;
                }

                logger.debug(String.format("Save pref for form %s, tabPane %s ", formName, tabPaneId));

                // Сохраняем serialUID класса пожо датасета у tabPane, если такой есть
                if (tabPane.getDataSetAdapter() != null
                        && tabPane.getDataSetAdapter().getDataSet() != null
                        && tabPane.getDataSetAdapter().getDataSet().getRowClass() != null) {
                    PPrefComponent tableSerialUID = new PPrefComponent();
                    tableSerialUID.setFORM_NAME(formName);
                    tableSerialUID.setCOMPONENT(tabPane.getId());
                    tableSerialUID.setSERIAL_UID(Controls.getSerialVersionUID(tabPane.getDataSetAdapter().getDataSet().getRowClass()));
                    list.add(tableSerialUID);
                }

                    List<Tab> allTabs = tabPane.getTabs();
                    allTabs.forEach( tab -> {
                        PPrefComponent pref = new PPrefComponent();

                        //Стандартное: имя формы и компонента
                        pref.setFORM_NAME(formName);
                        pref.setCOMPONENT(tabPaneId);

                        //Название таба.
                        //Для JInvTabPane текст находится в лейбле и устанавливается через setGraphic
                        String fieldName;
                        if ( JInvTabPane.tabHasGraphicText( tab ) ){
                            fieldName = ( (Label) tab.getGraphic() ).getText();
                        } else {
                            //Пробуем найти fxid
                            final String tabId = tab.getId();
                            if ( tabId != null ){
                                fieldName = tabId;
                            } else throw new RuntimeException(bundle.getString("ERROR_LOAD_COLUMN_SETTINGS") + tab.getText());
                        }
                        pref.setELEMENT(fieldName);

                        //Порядок табов
                        pref.setORDBY(tabPane.getTabs().indexOf(tab));

                        list.add(pref);
                    });

            }
        }
    }

    private static void fillSplitPositions(ViewContext viewContext, String formName, List<PPrefComponent> list) {

        if (viewContext != null && viewContext.getContentPane() != null) {

            List<SplitPane> listSplitPane = Controls.getNodeList(viewContext.getContentPane(), (Node t) -> t instanceof SplitPane).
                    stream().map((Node t) -> (SplitPane) t).collect(Collectors.toList());

            if (!listSplitPane.isEmpty() && listSplitPane.size() == 1) {

                list.addAll(getPrefSplitDividerList(formName, listSplitPane.get(0), true));
            } else {
                listSplitPane.forEach((SplitPane t) -> {
                    list.addAll(getPrefSplitDividerList(formName, t, false));
                });
            }
        }
    }

    private static List<PPrefComponent> getPrefSplitDividerList(String formName, SplitPane pane, boolean onlyOne) {

        List<PPrefComponent> list = new ArrayList<>();
        int i = 0;
        for (double position : pane.getDividerPositions()) {

            PPrefComponent pref = new PPrefComponent();
            if (onlyOne) {
                pref.setCOMPONENT("oneSplitPane");
            } else if (pane.getId() != null) {
                pref.setCOMPONENT(pane.getId());
            } else {
                continue;
            }
            pref.setELEMENT(PROPERTY_ELEMENT_SPLIT_DIVIDER + i);
            pref.setFORM_NAME(formName);
            pref.setORDBY(i);
            pref.setHEIGHT( new Double(position * DIMENSION_FRACTIONAL_FACTOR).longValue());

            logger.trace("position " + position);

            list.add(pref);
            i++;
        }
        return list;
    }

    private static void fillFormDimension(ViewContext viewContext, String formName, List<PPrefComponent> list) {

        if (viewContext != null) {

            Double height = viewContext.getHeight();
            Double width = viewContext.getWidth();
            if (viewContext.isMaximized()) {
                height = -1.0;
                width = -1.0;
            }

            PPrefComponent pref = new PPrefComponent();
            pref.setFORM_NAME(formName);
            pref.setWIDTH(width.longValue());
            pref.setHEIGHT(height.longValue());
            list.add(pref);
        }
    }

//    private static void fillLeftUpPoint(ViewContext viewContext, String formName, List<PPrefComponent> list) {
//        if (viewContext != null && viewContext.getWindow() != null)
//        {
//
//            JInvWindowMdi window = viewContext.getWindow();
//            Bounds boundsParent = window.localToParent(window.getBoundsInLocal());
//            long x = new Double(boundsParent.getMinX() * DIMENSION_FRACTIONAL_FACTOR).longValue();
//            long y = new Double(boundsParent.getMinY() * DIMENSION_FRACTIONAL_FACTOR).longValue();
//            PPrefComponent pref = new PPrefComponent();
//            pref.setFORM_NAME(formName);
//            pref.setWIDTH(x);
//            pref.setHEIGHT(y);
//            pref.setCOMPONENT(FORM_START_POINT);
//            list.add(pref);
//        }
//    }

    private static void fillEnableFilter(ViewContext viewContext, String formName, List<PPrefComponent> list) {

        if (viewContext != null && viewContext.getContentPane() != null) {

            List<JInvTable> listTable = Controls.getNodeList(viewContext.getContentPane(), (Node t) -> t instanceof JInvTable).
                    stream().map((Node t) -> (JInvTable) t).collect(Collectors.toList());

            listTable.forEach(table -> {

                if (table.getDataSetAdapter() != null) {
                    PPrefComponent entry = new PPrefComponent();
                    entry.setFORM_NAME(formName);
                    entry.setCOMPONENT(table.getId());
                    entry.setELEMENT(DSFXAdapter.PROPERTY_ENABLE_FILTER);
                    entry.setVISIBLE(TypeConverter.convert(table.getDataSetAdapter().isEnableFilter(), Long.class));
                    list.add(entry);

                    TripleBoolValueEnum tbv = (TripleBoolValueEnum) table.getProperties().get(GUI_SHOW_STATUS_BAR.name());

                    if (tbv != null && tbv == TripleBoolValueEnum.LAST) {
                        entry = new PPrefComponent();
                        entry.setFORM_NAME(formName);
                        entry.setCOMPONENT(table.getId());
                        entry.setELEMENT(GUI_SHOW_STATUS_BAR.name());
                        entry.setVISIBLE(table.visibleStatusBarProperty().get() ? 1L : 0L);
                        list.add(entry);
                    }

                    tbv = (TripleBoolValueEnum) table.getProperties().get(MARK_SAVE_PREV_MARKED_ROWS.name());

                    if (tbv != null && tbv == TripleBoolValueEnum.LAST) {
                        entry = new PPrefComponent();
                        entry.setFORM_NAME(formName);
                        entry.setCOMPONENT(table.getId());
                        entry.setELEMENT(MARK_SAVE_PREV_MARKED_ROWS.name());
                        entry.setVISIBLE(table.savePrevMarkedRowsProperty().get() ? 1L : 0L);
                        list.add(entry);
                    }
                    
                    tbv = (TripleBoolValueEnum) table.getProperties().get(CHECK_NO_DATA_FOUND.name());

                    if (tbv != null && tbv == TripleBoolValueEnum.LAST) {
                        entry = new PPrefComponent();
                        entry.setFORM_NAME(formName);
                        entry.setCOMPONENT(table.getId());
                        entry.setELEMENT(CHECK_NO_DATA_FOUND.name());
                        entry.setVISIBLE(table.checkNoDataFound().get() ? 1L : 0L);
                        list.add(entry);
                    }
                }

            });
        }

    }

    /** */
    public static boolean isMarkColumn(TableColumn<?, ?> column)
    {
        return column != null && column.getProperties().containsKey(COLUMN_MARK) && Boolean.TRUE.equals( column.getProperties().get(COLUMN_MARK) );
    }

    /** Вызывается при сохранении размеров формы */
    private static void fillTableColumns( ViewContext viewContext, String formName, List<PPrefComponent> list )
    {
        if( viewContext != null && viewContext.getContentPane() != null )
        {
            List<JInvTable<?>> listTable = Controls.getNodeList(viewContext.getContentPane(), (Node t) -> t instanceof JInvTable)
                    .stream()
                    .map((Node t) -> (JInvTable<?>) t)
                    .collect(Collectors.toList());

            for( JInvTable<?> table : listTable )
            {
                if( !table.isEnableColumnManager() ) {
                    logger.debug("Save pref. Table isEnableColumnManager false");
                    continue;
                }

                if( table.getId() == null || table.getId().isEmpty()) {
                    logger.error("Save pref. Table name is null");
                    continue;
                }

                final String tableName = table.getId( );

                logger.debug( String.format("Save pref for form %s, table %s ", formName, tableName) );

                // Сохраняем serialUID класса пожо датасета у таблицы, если такой есть
                if (table.getDataSetAdapter() != null
                        && table.getDataSetAdapter().getDataSet() != null
                        && table.getDataSetAdapter().getDataSet().getRowClass() != null) {
                    PPrefComponent tableSerialUID = new PPrefComponent();
                    tableSerialUID.setFORM_NAME ( formName );
                    tableSerialUID.setCOMPONENT ( table.getId() );
                    tableSerialUID.setSERIAL_UID( Controls.getSerialVersionUID(table.getDataSetAdapter().getDataSet().getRowClass()) );
                    list.add(tableSerialUID);
                    logger.debug("Table '{}' SERIAL_UID={}",
                            table.getId(), Controls.getSerialVersionUID(table.getDataSetAdapter().getDataSet().getRowClass()));
                }

                try {
                    List<? extends TableColumn<?, ?>> allColumnsFromTable = Controls.getAllColumnsFromTable(table);

                    allColumnsFromTable.stream()
                         .filter( c -> c instanceof JInvTableColumn<?,?> && !isMarkColumn(c))
                        .forEach( col -> {

                            final JInvTableColumn<?,?> column = (JInvTableColumn<?,?>) col;

                            final PPrefComponent pref = new PPrefComponent();
                            pref.setFORM_NAME( formName );
                            pref.setCOMPONENT( table.getId() );

                            String fieldName = column.getFieldName();

                            if( S.isNullOrEmpty(fieldName))
                                 fieldName = column.getId();

                            if( fieldName == null || fieldName.isEmpty() )
                            {
                                if( !column.getColumns().isEmpty() ) {
                                    logger.warn("Can't save column \"{}\" without fieldName or id. It will be ignored, because the column is parent", column.getText() );
                                    return;
                                }
                                throw new RuntimeException(bundle.getString("ERROR_LOAD_COLUMN_SETTINGS") + column.getText());
                            }

                            pref.setELEMENT(fieldName);

                            double width = column.getWidth();

//                            if( DEBUG_FORM_PARAMETERS  && "C_LCHN_OURREF".equals( fieldName ) ) {
//                                logger.info("{}: currentWidth: {}, prefWidth: {}", column.getId(), width, column.getPrefWidth());
//                            }

                            if( column.getProperties().get(JInvTableColumn.COLUMN_FONT_SIZE_ADJUST) != null )
                            {
                                if( column.isFixedSize() )
                                {
                                    //if( DEBUG_FORM_PARAMETERS && "C_LCHN_OURREF".equals( fieldName ) )
                                        ; // logger.info("{}: leaving fixed size {}", column.getId(), width);
                                }
                                else
                                {
//                                    double originalWidth = width;

                                    width = reverseComputeSize(width);

//                                    if( DEBUG_FORM_PARAMETERS  && "C_LCHN_OURREF".equals( fieldName ) )
//                                        logger.info("{}: restoring width {}->{} (prefWidth: {})", column.getId(), originalWidth, width, column.getPrefWidth());
                                }
                            }

                            pref.setWIDTH  ( new Double(width * DIMENSION_FRACTIONAL_FACTOR).longValue() );

                            pref.setVISIBLE( column.isVisible() ? 1L : 0L );

                            // Если вложенный столбец, то ordby =-1
                            int columnIndex = table.getColumns().indexOf(column);

                            if( columnIndex > 0 )
                            {
                                try {
                                    if ( BaseApp.APP().getViewPrefService().isMarkLeft() && table.getDataSetAdapter() != null && table.getDataSetAdapter().isEnableMark() && table.getMarkColumn() != null )
                                    {
                                        pref.setORDBY( columnIndex - 1 );
                                    }
                                    else
                                    {
                                        pref.setORDBY(columnIndex);
                                    }
                                } catch ( AppException ex ) {
                                    logger.error( "", ex );
                                }
                            }
                            else
                            {
                                pref.setORDBY(columnIndex);
                            }

                            list.add( pref );

                        });
                } catch (Exception ex) {
                    JInvErrorService.handleException(null, ex);
                }
            }
        }
    }
    private static void fillTreeTableColumns(ViewContext viewContext, String formName, List<PPrefComponent> list) {

        if( viewContext != null && viewContext.getContentPane() != null )
        {
            List<JInvTreeTable> listTable = Controls.getNodeList(viewContext.getContentPane(),
                    (Node t) -> t instanceof JInvTreeTable)
                    .stream()
                    .map((Node t) -> (JInvTreeTable) t)
                    .collect(Collectors.toList());

            for (JInvTreeTable table : listTable) {

                if (table.getId() == null || table.getId().isEmpty()) {
                    logger.error("Save pref. TreeTable name is null");
                    continue;
                }

                String tableName = table.getId();

                logger.debug(String.format("Save pref for form %s, treetable %s ", formName, tableName));

                try {

                    List allColumnsFromTable = Controls.getAllColumnsFromTreeTable(table);
                    allColumnsFromTable.stream()
                        .filter( c -> c instanceof JInvTreeTableColumn )
                        .forEach( col -> {
                            JInvTreeTableColumn column = (JInvTreeTableColumn) col;

                            PPrefComponent pref = new PPrefComponent();
                            pref.setFORM_NAME(formName);

                            if (tableName == null || tableName.isEmpty()) {
                                throw new RuntimeException(bundle.getString("ERROR_LOAD_COLUMN_SETTINGS") + column.getText());
                            }

                            pref.setCOMPONENT(table.getId());

                            String fieldName = column.getFieldName();

                            if (fieldName == null || column.getFieldName().isEmpty()) {
                                fieldName = column.getId();
                            }

                            if (fieldName == null || fieldName.isEmpty()) {
                                if (!column.getColumns().isEmpty()) {
                                    logger.warn("Can't save tree column \"{}\" without fieldName or id." +
                                            " It will be ignored, because the column is parent", column.getText());
                                    return;
                                }
                                throw new RuntimeException(bundle.getString("ERROR_LOAD_COLUMN_SETTINGS") + column.getText());
                            }
                            pref.setELEMENT(fieldName);

                            pref.setWIDTH(new Double(column.getWidth() * DIMENSION_FRACTIONAL_FACTOR).longValue());

                            pref.setVISIBLE(column.isVisible() ? 1L : 0L);
                            pref.setORDBY( table.getColumns().indexOf( column ) );

                            list.add( pref );
                        });
                } catch (Exception ex) {
                    JInvErrorService.handleException(null, ex);
                }
            }
        }
    }

    public Boolean isMarkLeft() {
        return DEFAULT_FX_LEFT_MARK_POSITION;
    }

    private static boolean hasNonEmptyLabel( final Node tabLabel ) {
        return tabLabel instanceof Label && !( (Label) tabLabel ).getText().isEmpty();
    }

    public static void saveDimensions(String formName, javafx.stage.Stage stage) throws Exception {

        Double height = stage.getHeight();
        Double width = stage.getWidth();
        if (stage.isMaximized()) {
            height = -1.0;
            width = -1.0;
        }
        PPrefComponent pref = new PPrefComponent();
        pref.setFORM_NAME(formName);
        pref.setWIDTH(width.longValue());
        pref.setHEIGHT(height.longValue());
        saveDimensions( Collections.singletonList( pref ) );
    }

    public static void saveDimensions(List<PPrefComponent> list) throws AppException {
        ViewPrefDao.savePreferences(list);
    }

    public static void refreshTabPanePosition(ViewContext viewContext) {
        if ( viewContext.getStage() == null ) {
            return;
        }
        Parent root = viewContext.getStage().getScene().getRoot();
        if (root instanceof StackPane) {
            root = getRootWhileWaiting((StackPane) root);
        }
        Controls.getControlList(root, (Control t) -> t instanceof JInvTabPane).forEach((Control t) -> {
            JInvTabPane tabPane = (JInvTabPane) t;
            tabPane.setViewPrefSaver( viewContext.getViewPrefSaver() );
            tabPane.applyViewPrefs();
        });
    }

    public static void localizeDialog(DialogPane pane) {
        if (pane != null) {
            pane.getButtonTypes().forEach((ButtonType t) -> {
                Button button = (Button) pane.lookupButton(t);
                if (button != null) {
                    localizeButton(button, t);
                }
            });
        }
    }

    public static void localizeButton(Button button, ButtonType type) {

        if (button != null && type != null) {
            if (type == ButtonType.APPLY) {
                button.setText(buttonBundle.getString("Dialog.apply.button"));
            } else if (type == ButtonType.CANCEL) {
                button.setText(buttonBundle.getString("Dialog.cancel.button"));
            } else if (type == ButtonType.CLOSE) {
                button.setText(buttonBundle.getString("Dialog.close.button"));
            } else if (type == ButtonType.FINISH) {
                button.setText(buttonBundle.getString("Dialog.finish.button"));
            } else if (type == ButtonType.NEXT) {
                button.setText(buttonBundle.getString("Dialog.next.button"));
            } else if (type == ButtonType.NO) {
                button.setText(buttonBundle.getString("Dialog.no.button"));
            } else if (type == ButtonType.OK) {
                button.setText(buttonBundle.getString("Dialog.ok.button"));
            } else if (type == ButtonType.PREVIOUS) {
                button.setText(buttonBundle.getString("Dialog.previous.button"));
            } else if (type == ButtonType.YES) {
                button.setText(buttonBundle.getString("Dialog.yes.button"));
            }
        }
    }

    public ImageCursor getHelpCursor() {
        return helpCursor;
    }

    @Override
    public String getServiceInfo() {
        return "ViewPrefAppService";
    }

    /** */
    public Optional<Color> getIndicatorColor( TaskContext tc ) {

        if( indicators == null ) {
            String xml =
                        BaseApp
                        .APP()
                        .getProperties    ( PropertyItemEnum.IND_ALIAS_LIST.getPropertyType() )
                        .getStringProperty( PropertyItemEnum.IND_ALIAS_LIST.getName() );


            indicators = ConnectionIndicators.fromXMLString( xml );
        }

        return indicators.getIndicatorColor( tc );
    }

    /** */
    public int getIndicatorBorderWidth() {
        return  BaseApp
                .APP()
                .getProperties     ( PropertyItemEnum.IND_BORDER_WIDTH.getPropertyType() )
                .getIntegerProperty( PropertyItemEnum.IND_BORDER_WIDTH.getName(), 2 );
    }

    /** */
    public ButtonIconView getButtonIconView() {
        String buttonIconViewString = BaseApp.APP().getProperties( PropertiesTypeEnum.DB_USER ).
               getStringProperty( PropertyItemEnum.GUI_TOOLBAR_BTN_MODE.getName(),DEFAULT_FX_BUTTON_ICON_VIEW.name() );
        ButtonIconView buttonIconView = ButtonIconView.valueOf( buttonIconViewString );
        return buttonIconView;
    }

    public Boolean getSaveMark() {
        Boolean saveMark = BaseApp.APP().getProperties( PropertiesTypeEnum.DB_USER ).
                getBooleanProperty( PROPERTY_FX_SAVE_MARK );
        return U.nvl( saveMark, DEFAULT_FX_SAVE_MARK );
    }

    public void setSaveMark( Boolean val ) {
        if ( val != null ) {
            BaseApp.APP().getProperties( PropertiesTypeEnum.DB_USER ).setProperty( PROPERTY_FX_SAVE_MARK, val );
        }
    }

    public Integer getFontSize() {
        return (int) getFont().getSize();
    }

    public Integer getLovSmallCountRows() {
        return DEFAULT_FX_LOV_SMALL_ROW_COUNT;
    }

    public String getColorMark() {
        return BaseApp.APP().getProperties( PropertiesTypeEnum.DB_USER )
                .getStringProperty( PropertyItemEnum.GUI_COLOR_MARKED.getName(), DEFAULT_FX_COLOR_MARK );
    }

    public String getColorIndexSearch() {
        return BaseApp.APP().getProperties( PropertiesTypeEnum.DB_USER )
                .getStringProperty( PropertyItemEnum.GUI_COLOR_INDEX_SEARCH.getName(), DEFAULT_FX_COLOR_FILTER_INDEX_SEARCH );
    }
    public String getColorListSearch() {
        return BaseApp.APP().getProperties( PropertiesTypeEnum.DB_USER )
                .getStringProperty( PropertyItemEnum.GUI_COLOR_LIST_SEARCH.getName(), DEFAULT_FX_COLOR_FILTER_LIST );
    }
    public String getColorExprSearch() {
        return BaseApp.APP().getProperties( PropertiesTypeEnum.DB_USER )
                .getStringProperty( PropertyItemEnum.GUI_COLOR_EXPR_SEARCH.getName(), DEFAULT_FX_COLOR_FILTER_EXPR );
    }

    public String getColorTooltipBackground() {
        return BaseApp.APP().getProperties( PropertiesTypeEnum.DB_USER )
                .getStringProperty( PropertyItemEnum.GUI_COLOR_TOOLTIP_BG.getName(), DEFAULT_FX_COLOR_TOOLTIP_BACKGROUND );
    }

    /** Стиль для названий обязательных полей */
    public LabelStyleEnum getStyleValidLabels() {
        String styleString = BaseApp.APP().getProperties( PropertiesTypeEnum.DB_USER )
                .getStringProperty( PropertyItemEnum.GUI_LABEL_REQ_STYLE.getName(), DEFAULT_VALID_LABEL_STYLE.name() );
        return LabelStyleEnum.valueOf( styleString );
    }

    public String getColorTooltipText() {
        return BaseApp.APP().getProperties( PropertiesTypeEnum.DB_USER ).
                getStringProperty( PropertyItemEnum.GUI_COLOR_TOOLTIP_FG.getName(),
                        DEFAULT_FX_COLOR_TOOLTIP_TEXT );
    }

    public String getColorRequired() {
        return BaseApp.APP()
                .getProperties( PropertiesTypeEnum.DB_USER )
                .getStringProperty( PropertyItemEnum.GUI_COLOR_REQUIRED.getName(), DEFAULT_FX_COLOR_REQUIRED );
    }

    public String getColorValidatable() {
        return BaseApp.APP()
                .getProperties( PropertiesTypeEnum.DB_USER )
                .getStringProperty( PropertyItemEnum.GUI_COLOR_VALIDATABLE.getName(), DEFAULT_FX_COLOR_VALIDATABLE );
    }

    public boolean isShowValidatable() {
        Boolean property = BaseApp.APP()
                .getProperties( PropertiesTypeEnum.DB_USER )
                .getBooleanProperty( PropertyItemEnum.GUI_SHOW_VALIDATABLE.getName() );
        return U.nvl( property, false );
    }

    public static void setFrameMode( final JInvFrameMode frameMode ) {
        ViewPrefAppService.frameMode = frameMode;
    }

    public JInvFrameMode getFrameMode() {
        if ( frameMode != null ) {
            return frameMode;
        }
        String frameModeString = BaseApp.APP()
                .getProperties( PropertiesTypeEnum.PRP )
                .getStringProperty( AppConstants.VIEW_MODE );
        if ( frameModeString != null ) {
            try {
                frameMode = JInvFrameMode.valueOf( frameModeString.toUpperCase() );
            } catch ( IllegalArgumentException ignored ) {}
        }
        if ( frameMode == null ) 
            frameMode = JInvFrameMode.SDI;
        return frameMode;
    }

    public Boolean getDatePrioritet() {
        Boolean datePrioritet = BaseApp.APP().getProperties( PropertiesTypeEnum.DB_USER ).
                getBooleanProperty( JInvCalendar.PROPERTY_FX_CALENDAR_DATE_PRIORITET );
        return U.nvl( datePrioritet, JInvCalendar.DEFAULT_FX_CALENDAR_DATE_PRIORITET );
    }

    public Boolean isVectorIcons() {
        Boolean property = BaseApp.APP()
                .getProperties( PropertyItemEnum.GUI_VECTOR_ICONS.getPropertyType() )
                .getBooleanProperty( PropertyItemEnum.GUI_VECTOR_ICONS.getName() );
        return U.nvl( property, DEFAULT_FX_VECTOR_ICONS );
    }

    /**
     Применение размеров для режима MDI
     */
    public void refreshViewSettingsWindow( JInvWindowMdi w) throws Exception {
/*
        if (!w.getContentPane().getChildren().isEmpty()) {

            Region content = (Region) w.getContentPane().getChildren().get(0);

            // Такое может произойти что в этот момент на панеле стоит декоратор долгой операции и поэтому вытащить
            // содержимое попытаемся таким образом
            if (content instanceof StackPane) {
                content = getRootWhileWaiting((StackPane) content);

            }

            if (content != null) {

                initUIPrefs(content);

                double sizeTitle = 24; // высота заголовка по умолчанию. 2 em см. general.css

                JInvFXFormController controller = Controls.getControllerFromControl(content);
                Dimension2D dimenstions = null;

                if (controller == null || !controller.isDisableSavedDimensions()) {
                    dimenstions = getSavedDimensions(content);
                }

                Double prefRelHeight = null;
                Double prefRelWidth = null;
                if (dimenstions != null) {

                    if (dimenstions.getHeight() == -1.0) {
                        prefRelHeight = -1.0;
                        prefRelWidth = -1.0;
                    } else {
                        w.setPrefSize(dimenstions.getWidth(), dimenstions.getHeight());
                    }
                } else {

                    logger.trace(Tags.PRODUCT_LABEL + bundle.getString("SOHRANENNYH_RAZMEROV_NE_NAJDENO"));
                    prefRelHeight = computeRelativeExSize(content.getPrefHeight() + sizeTitle);
                    prefRelWidth = computeRelativeExSize(content.getPrefWidth());

                    w.setStyle(w.getStyle().concat("-fx-pref-height:" + prefRelHeight + "ex; -fx-pref-width:" + prefRelWidth + "ex;"));
                    logger.trace(Tags.PRODUCT_LABEL + "pane prefSize " + content.getPrefHeight() + " : " + content.getPrefHeight() + " relPrefSize " + prefRelHeight + " : " + prefRelWidth);
                    w.setPrefSize(content.getPrefWidth(), content.getPrefHeight() + sizeTitle);
                }

                if (prefRelHeight != null && prefRelHeight == -1.0) {
                    w.visibleProperty().addListener(new ChangeListener<Boolean>() {
                        //Сработали разок – и хватит
                        boolean triggeredOnce = false;

                        @Override
                        public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
                            if ( triggeredOnce ){
                                return;
                            }

                            if ( newValue ) {
                                try {
                                    w.setMaximized(true);
                                } catch (Throwable ex) {
                                    JInvErrorService.handleException(null, ex);
                                } finally {
                                    triggeredOnce = true;
                                }
                            }
                        }
                    });
                }

                // Если установлены минимальные размеры масштабируем их под текущий шрифт и ставим стиль на содержимое и на само окно
                if (isFiniteSize(content.getMinHeight()) && isFiniteSize(content.getMinWidth())) {
                    Double minRelHeightWithTitle = computeRelativeExSize(content.getMinHeight() + sizeTitle);
                    Double minRelHeight = computeRelativeExSize(content.getMinHeight());

                    Double minRelWidth = computeRelativeExSize(content.getMinWidth());
                    w.setStyle(w.getStyle().concat("-fx-min-height:" + minRelHeightWithTitle + "ex; -fx-min-width:" + minRelWidth + "ex;"));
                    content.setStyle(content.getStyle().concat("-fx-min-height:" + minRelHeight + "ex; -fx-min-width:" + minRelWidth + "ex;"));
                    logger.trace(Tags.PRODUCT_LABEL + "pane minSize " + content.getMinHeight() + " : " + content.getMinHeight() + " relMinSize " + minRelHeightWithTitle + " : " + minRelHeight + " : " + minRelWidth);
                }

                // Если установлены максимальныеразмеры масштабируем их под текущий шрифт и ставим стиль на содержимое и на само окно
                if (isFiniteSize(content.getMaxHeight()) && isFiniteSize(content.getMaxWidth())) {
                    Double maxRelHeight = computeRelativeExSize(content.getMaxHeight());
                    Double maxRelHeightWithTitle = computeRelativeExSize(content.getMaxHeight() + sizeTitle);

                    Double maxRelWidth = computeRelativeExSize(content.getMaxWidth());
                    w.setStyle(w.getStyle().concat("-fx-max-height:" + maxRelHeightWithTitle + "ex; -fx-max-width:" + maxRelWidth + "ex;"));
                    content.setStyle(content.getStyle().concat("-fx-max-height:" + maxRelHeight + "ex; -fx-max-width:" + maxRelWidth + "ex;"));

                    logger.trace(Tags.PRODUCT_LABEL + "pane maxSize " + content.getMaxHeight() + " : " + content.getMaxHeight() + " relMaxSize " + maxRelHeight + " : " + maxRelWidth);
                }

                refreshTableSize( content );
                refreshTreeTableSize( content );
                refreshSplitPosition( content );
                refreshTableColumnPosition( content );

            }
        }
 */
    }

    private static Region getRootWhileWaiting( StackPane content ) {
        Region result = null;
        Boolean isDecorator = (Boolean) content.getProperties().getOrDefault( PROPERTY_DECORATOR, false );
        if ( isDecorator ) {
            result = (Region) content.getChildrenUnmodifiable().get( 0 );
        }
        return result;
    }

    /**
     * Ищем все таблицы на панели, далее бежим по всем колонкам и вызываем метод масштабирования размеров для каждой колонки
     *
     * @param root
     */
    public void refreshTableSize(Parent root) {

        Controls.getControlList(root, null, TableView.class).stream().
                map((Control t) -> (TableView) t).
                filter((TableView t) -> t != null && t.getColumnResizePolicy().equals(TableView.UNCONSTRAINED_RESIZE_POLICY) ).
                forEach((TableView t) -> {

                    t.getColumns().forEach((Object col) -> {
                        TableColumn column = (TableColumn) col;

                        refreshTableColumn(column);

                        if (!column.getColumns().isEmpty()) {
                            column.getColumns().forEach((Object childCol) -> {
                                TableColumn childColumn = (TableColumn) childCol;
                                refreshTableColumn(childColumn);
                            });
                        }
                    });
                });
    }
    public void refreshTreeTableSize(Parent root) {

        Controls.getControlList(root, null, TreeTableView.class).stream().
                map((Control t) -> (TreeTableView) t).
                filter((TreeTableView t) -> t != null && t.getColumnResizePolicy().equals(TreeTableView.UNCONSTRAINED_RESIZE_POLICY) ).
                forEach((TreeTableView t) -> {

                    t.getColumns().forEach(col -> {
                        refreshTableColumn((TableColumnBase) col);
                    });
                });
    }

    /**
     * Устанавливаем новый размер колонки в пропорции соотношения текущего размера шрифта к дефолтному.
     * Изначальный размер сохраняем в свойства один раз.
     * <p>
     * @param column
     */
    public void refreshTableColumn( TableColumnBase<?,?> column)
    {
        if( !column.getProperties().containsKey(JInvTableColumn.COLUMN_PREF_WIDTH) )
        {
            if( column.getPrefWidth() != -1.0 && column.getPrefWidth() != 0.0 && !column.getProperties().containsKey(JInvTableColumn.COLUMN_LOV))
                column.getProperties().put(JInvTableColumn.COLUMN_PREF_WIDTH, column.getPrefWidth());
            else
                column.getProperties().put(JInvTableColumn.COLUMN_PREF_WIDTH, column.getWidth());
        }

        Double beginPrefWidth = (Double) column.getProperties().get(JInvTableColumn.COLUMN_PREF_WIDTH);
        Double newPrefWidth   = beginPrefWidth;

        boolean adjustColumnWidth = getFont().getSize() != DEFAULT_FONT_SIZE
                                    && !column.getProperties().containsKey(JInvTableColumn.COLUMN_FONT_SIZE_ADJUST);
        if( adjustColumnWidth )
        {
            newPrefWidth = computeSize(beginPrefWidth);
            if (DEBUG_FORM_PARAMETERS){
                logger.info("refreshTC: {} adjusted {}->{}", column.getId(), beginPrefWidth, newPrefWidth);
            }
            column.getProperties().put(JInvTableColumn.COLUMN_FONT_SIZE_ADJUST, "stage1");
        }

        if ( !column.prefWidthProperty().isBound() ) {
            column.setPrefWidth(newPrefWidth);
        }

        if (column.getProperties().containsKey(JInvTableColumn.COLUMN_MARK)) {
            if ((Boolean) column.getProperties().get(JInvTableColumn.COLUMN_MARK)) {
                column.setMinWidth(newPrefWidth);
                column.setMaxWidth(newPrefWidth);
            }
        }
    }

    public Font getFont() {
        if ( font == null ) {
            String fontString = BaseApp.APP().getProperties( PropertiesTypeEnum.DB_USER )
                    .getStringProperty( PropertyItemEnum.GUI_FONT.getName() );
            font = fontString == null ? DEFAULT_FX_FONT : getFontFromString( fontString );
        }
        return font;
    }

    public Font getCodeFont() {
        if ( codeFont == null ) {
            String fontString = BaseApp.APP().getProperties( PropertiesTypeEnum.DB_USER )
                    .getStringProperty( PropertyItemEnum.GUI_FONT_CODE.getName() );
            codeFont = fontString == null ? DEFAULT_FX_FONT_CODE : getFontFromString( fontString );
        }
        return codeFont;
    }

    public static Font getFontFromString( String val ) {
        Font font = null;
        try {
            String[] fontParts = val.split( "&" );
            if ( fontParts.length == 3 ) {
                FontWeight weight = FontWeight.NORMAL;
                FontPosture posture = FontPosture.REGULAR;
                String[] fontStyles = fontParts[1].trim().toUpperCase().split( "\\s" );
                for ( String style : fontStyles ) {
                    FontWeight w = FontWeight.findByName( style );
                    if ( w != null ) {
                        weight = w;
                    } else {
                        FontPosture p = FontPosture.findByName( style );
                        if ( p != null ) {
                            posture = p;
                        }
                    }
                }
                font = Font.font( fontParts[0], weight, posture, Double.valueOf( fontParts[2] ) );
            }
        } catch ( Throwable ignored ) {}
        return font;
    }

    private boolean isFiniteSize(double size) {
        return size != 0 && size != -1.0 && Double.isFinite(size);
    }

    public static Dimension2D getSavedDimensions(Parent root) {

        List<PPrefComponent> list = (List<PPrefComponent>) root.getProperties().get(PROPERTY_UI_PREFS);
        if (list != null && !list.isEmpty()) {

            PPrefComponent pref = list.stream().
                    filter((PPrefComponent t) -> t.getFORM_NAME() != null && t.getCOMPONENT() == null && t.getELEMENT() == null).
                    findAny().orElse(null);
            if (pref != null && pref.getWIDTH() != null && pref.getHEIGHT() != null ) {
                return new Dimension2D(pref.getWIDTH(), pref.getHEIGHT());
            }
        }

        return null;
    }

    private void refreshSplitPosition(Parent root) {

        if (root.getProperties().get(PROPERTY_UI_PREFS) != null) {

            List<SplitPane> listSplitPane =
                    Controls.getNodeList(root, (Node t) -> t instanceof SplitPane)
                        .stream()
                        .map((Node t) -> (SplitPane) t)
                        .collect(Collectors.toList());

            if (!listSplitPane.isEmpty()) {

                List<PPrefComponent> list = (List<PPrefComponent>) root.getProperties().get(PROPERTY_UI_PREFS);
                List<PPrefComponent> listDividers = list.stream().
                        filter((PPrefComponent t) -> t.getELEMENT() != null && t.getELEMENT().startsWith(PROPERTY_ELEMENT_SPLIT_DIVIDER)).
                        sorted( Comparator.comparingInt( PPrefComponent::getORDBY ) ).
                        collect(Collectors.toList());

                if (!listDividers.isEmpty()) {

                    listDividers.forEach((PPrefComponent t) -> {
                        logger.trace(t.toString());
                    });
                    if ( listSplitPane.size() == 1 && listDividers.size() == listSplitPane.get( 0 ).getDividers().size() ) {

                        SplitPane pane = listSplitPane.get(0);

                        int i = 0;
                        for (Divider divider : pane.getDividers()) {
                            divider.setPosition(listDividers.get(i).getHEIGHT() / DIMENSION_FRACTIONAL_FACTOR);

                            logger.trace("when refresh " + listDividers.get(i).getHEIGHT() / DIMENSION_FRACTIONAL_FACTOR);

                            i++;
                        }

                    } else {
                        Consumer<SplitPane> restoreSplitPosition = ( SplitPane splitPane ) -> {
                            String idPane = splitPane.getId();
                            if ( idPane != null ) {
                                List<PPrefComponent> listDividersForPane = listDividers.stream().
                                        filter( ( PPrefComponent t1 ) -> t1.getCOMPONENT().equalsIgnoreCase( idPane ) ).
                                        collect( Collectors.toList() );
                                Iterator<PPrefComponent> iterator = listDividersForPane.iterator();
                                for ( Divider divider : splitPane.getDividers() ) {
                                    if ( !iterator.hasNext() ) {
                                        return;
                                    }
                                    divider.setPosition( iterator.next().getHEIGHT() / DIMENSION_FRACTIONAL_FACTOR );
                                }
                            }
                        };

                        //Делаем два прохода: в обратном порядке, затем в прямом
                        Collections.reverse( listSplitPane );

                        listSplitPane.forEach( restoreSplitPosition );

                        Collections.reverse( listSplitPane );

                        Platform.runLater( () -> listSplitPane.forEach( restoreSplitPosition ) );
                    }
                }
            }
        }
    }

    /** */
    public static void refreshTableColumnPosition(Parent root)
    {
        if( root instanceof StackPane )
            root = getRootWhileWaiting((StackPane) root);

        final Parent content = root;

        Controls.getControlList( root, (Control t) -> t instanceof JInvTable).forEach((Control t) -> {

            JInvTable table = (JInvTable) t;

            if( checkSerialUIDTable( table, content ))
            {
                final AbstractBaseController< ? > ctrl = Controls.getControllerFromControl(table);
                if( ctrl == null )
                    return;

                IViewPrefSaver prefSaver = Controls.getControllerFromControl( table ).getViewContext().getViewPrefSaver();

                List<TableColumn> listColumn = Controls.getAllColumnsFromTable( table );

                final Holder<String> prefId = new Holder<>();

                final Predicate<TableColumn> colFinder = new Predicate< TableColumn >() {
                    @Override
                    public boolean test( TableColumn tc ) {

                        if( tc instanceof JInvTableColumn )
                        {
                            JInvTableColumn jtc = (JInvTableColumn)tc;
                            if( !S.isNullOrEmpty( jtc.getFieldName() ) )
                                return prefId.get().equals( jtc.getFieldName() );
                        }
                        return prefId.get().equals( tc.getId() );
                    }
                };

                //
                final String component = table.getId();

                if( !S.isNullOrEmpty(component) )
                {
                    final Iterable<PPrefComponent> prefsIter = U.iterable(prefSaver.getInitialPrefs(component));
                    //final Stream<TableColumn> columnStream = listColumn.stream();

                    for( PPrefComponent pref : prefsIter )
                    {
                        if( pref.getORDBY() == null )
                            continue;

                        prefId.set( pref.getELEMENT() );
                        listColumn.stream().filter(colFinder).findAny().map( c->c.getProperties().put("order_by", pref.getORDBY() ) );

                    }//end for

                    table.getColumns().sort(new Comparator<TableColumn>() {
                        @Override
                        public int compare( TableColumn c1, TableColumn c2 ) {

                            Integer ord1 = (Integer)c1.getProperties().get("order_by");
                            Integer ord2 = (Integer)c2.getProperties().get("order_by");

                            if( ord1 == ord2 )
                                return 0;

                            if( ord1 == null )
                                return 1;

                            if( ord2 == null )
                                return -1;

                            return Integer.compare( ord1,ord2 );
                        }
                    });

                }

                listColumn.forEach((TableColumn column) -> {
                    if (column instanceof IViewChangeable && column.getColumns().isEmpty() ) {
                        ((IViewChangeable) column).applyViewPrefs();
                    }
                });
            }

        });

        /*
        Controls.getControlList(root, (Control t) -> t instanceof JInvTreeTable).forEach((Control t) -> {
            JInvTreeTable table = (JInvTreeTable) t;
//            if (checkSerialUIDTable(table, content)) { // "Not supported yet."
                List<TreeTableColumn> listColumn = Controls.getAllColumnsFromTreeTable(table);
                IViewPrefSaver prefSaver = Controls.getControllerFromControl( table )
                        .getViewContext()
                        .getViewPrefSaver();
                listColumn.forEach((TreeTableColumn column) -> {
                    if (column instanceof IViewChangeable) {
                        ((IViewChangeable) column).setViewPrefSaver( prefSaver );
                        ((IViewChangeable) column).applyViewPrefs();
                    }
                });
//            }
        });
         */
    }

    private static <T extends Node & IJInvControl> boolean checkSerialUIDTable(T table, Parent root) {

        boolean result = true;

        Long serialUID = null;
        if (table == null || root == null
                || table.getId() == null
                || table.getDataSetAdapter() == null
                || table.getDataSetAdapter().getDataSet() == null
                || table.getDataSetAdapter().getDataSet().getRowClass() == null) {
            return result;
        } else {
            serialUID = U.getSerialVersionUID(table.getDataSetAdapter().getDataSet().getRowClass());
        }
        List<PPrefComponent> list = (List<PPrefComponent>) root.getProperties().getOrDefault( PROPERTY_UI_PREFS, null );
        if ( list != null ) {
            PPrefComponent prefSerial = list.stream()
                    .filter( ( PPrefComponent t ) -> t.getCOMPONENT() != null &&
                            t.getCOMPONENT().equals( table.getId() ) &&
                            t.getSERIAL_UID() != null )
                    .findFirst()
                    .orElse( null );
            if ( prefSerial != null ) {
                result = serialUID.equals( prefSerial.getSERIAL_UID() );
            }
        }
        return result;
    }

    private Double computeSize(double val) {
        return val * (getFont().getSize() / DEFAULT_FONT_SIZE);
    }

    /** */
    private static Double reverseComputeSize(double val)
    {
        double fontSize = DEFAULT_FONT_SIZE;

        if( font != null && font.getSize() > 0f )
            fontSize = font.getSize();

        return val * (DEFAULT_FONT_SIZE / fontSize);
    }

    public Image getAppIcon() {
        return getAppIcon(getFontSize());
    }

    private void refreshIcon(Parent root) {
        javafx.stage.Stage stage = (javafx.stage.Stage) root.getScene().getWindow();
        stage.getIcons().clear();
        stage.getIcons().add(getAppIcon());
    }
}
