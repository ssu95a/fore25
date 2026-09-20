package ru.inversion.fx.form.controls.sheet;

import com.sun.javafx.scene.control.behavior.BehaviorBase;
import com.sun.javafx.scene.control.skin.BehaviorSkinBase;
import com.sun.javafx.tk.FontLoader;
import com.sun.javafx.tk.Toolkit;
import impl.org.controlsfx.skin.PropertySheetSkin;
import javafx.beans.value.ObservableValue;
import javafx.collections.ListChangeListener;
import javafx.collections.MapChangeListener;
import javafx.collections.ObservableMap;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.cell.TextFieldTreeCell;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.util.Callback;
import javafx.util.StringConverter;
import org.controlsfx.control.PropertySheet;
import org.controlsfx.control.SegmentedButton;
import org.controlsfx.control.action.Action;
import org.controlsfx.control.action.ActionUtils;
import org.controlsfx.control.textfield.TextFields;
import org.controlsfx.property.editor.AbstractPropertyEditor;
import org.controlsfx.property.editor.PropertyEditor;
import org.slf4j.Logger;
import ru.inversion.fx.app.BaseApp;
import ru.inversion.fx.app.frame.menu.PropertyItemValue;
import ru.inversion.fx.form.controls.IReadOnlyControl;
import ru.inversion.fx.form.controls.JInvTextField;
import ru.inversion.fx.form.lov.JInvEntityLov;
import ru.inversion.icons.IconFactory;
import ru.inversion.icons.enums.FontAwesome;
import ru.inversion.utils.U;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

import static impl.org.controlsfx.i18n.Localization.asKey;
import static impl.org.controlsfx.i18n.Localization.localize;
import static java.lang.invoke.MethodHandles.lookup;
import static org.slf4j.LoggerFactory.getLogger;

/**
 *
 * @author antonovdi
 */
public class JInvPropertySheetSkin extends BehaviorSkinBase<PropertySheet, BehaviorBase<PropertySheet>> {
    /**
     * ************************************************************************
     *
     * Static fields
     *
     *************************************************************************
     */
    private static final int MIN_COLUMN_WIDTH = 75;
    final static ResourceBundle fore = ResourceBundle.getBundle("fore");
    private static final Logger logger = getLogger(lookup().lookupClass());
    public static final char ICON_MARKER = '`';
    /**
     * ************************************************************************
     *
     * fields
     *
     *************************************************************************
     */
    private final BorderPane content;
    private final ScrollPane scroller;
    private final ToolBar toolbar;
    private final SegmentedButton modeButton = ActionUtils.createSegmentedButton(
        new ActionChangeMode(PropertySheet.Mode.NAME),
        new ActionChangeMode(PropertySheet.Mode.CATEGORY)
    );
    private final TextField searchField = TextFields.createClearableTextField();
    private final TreeItem<SettingsPage> pageTreeRoot = new TreeItem<>(null);
    private final FontLoader fontLoader = Toolkit.getToolkit().getFontLoader();
    //Показывать ли список страниц слева
    private boolean showPages;
    private TreeView<SettingsPage> tree;

    //Текущая максимальная ширина левой части (описание свойства). Обнуляется после использования.
    private double maxWidth = MIN_COLUMN_WIDTH;
    private final HashMap<PropertySheet.Item, Node> itemEditorMap = new HashMap<>();
    private Map<String,Boolean> securityMap;

    /**
     * ************************************************************************
     *
     * Constructors
     *
     *************************************************************************
     */
    public JInvPropertySheetSkin(final PropertySheet control) {

        super(control, new BehaviorBase<>(control, Collections.emptyList()));

        scroller = new ScrollPane();
        scroller.setFitToWidth(true);

        toolbar = new ToolBar();
        toolbar.managedProperty().bind(toolbar.visibleProperty());
        toolbar.setFocusTraversable(true);

        // property sheet mode
        modeButton.managedProperty().bind(modeButton.visibleProperty());
        modeButton.getButtons().get(getSkinnable().modeProperty().get().ordinal()).setSelected(true);
        toolbar.getItems().add(modeButton);

        // property sheet search
        searchField.setPromptText(localize(asKey("property.sheet.search.field.prompt"))); //$NON-NLS-1$
        searchField.setMinWidth(0);
        HBox.setHgrow(searchField, Priority.SOMETIMES);
        searchField.managedProperty().bind(searchField.visibleProperty());
        toolbar.getItems().add(searchField);

        // layout controls
        content = new BorderPane();
        content.setTop(toolbar);
        content.setCenter(scroller);
        getChildren().add(content);

        showPages = control instanceof JInvPropertySheet && ( (JInvPropertySheet) control ).isShowPages();

        if ( showPages ){
            tree = new TreeView<>();

            AnchorPane pageList = new AnchorPane();
            pageList.setPrefWidth( 160 );
            pageList.getChildren().add( tree );

            AnchorPane.setTopAnchor( tree, 0d );
            AnchorPane.setBottomAnchor( tree, 0d );
            AnchorPane.setLeftAnchor( tree, 0d );
            AnchorPane.setRightAnchor( tree, 0d );

            content.setLeft( pageList );

            tree.setCellFactory( new Callback<TreeView<SettingsPage>, TreeCell<SettingsPage>>() {
                private final StringConverter<SettingsPage> converter = new StringConverter<SettingsPage>() {
                    @Override
                    public String toString( final SettingsPage id ) {
                        return id == null ? "" : "  " + fore.getString( "SETTINGSPAGE_" + id );
                    }
                    @Override
                    public SettingsPage fromString( final String string ) {
                        throw new UnsupportedOperationException();
                    }
                };
                @Override
                public TreeCell<SettingsPage> call( TreeView<SettingsPage> param ) {
                    return new TextFieldTreeCell<SettingsPage>( converter ) {
                        @Override
                        public void updateItem( SettingsPage item, boolean empty ) {
                            //Убираем стрелку-индикатор раскрытого дерева
                            setDisclosureNode( null );
                            super.updateItem( item, empty );
                            if ( item != null ) {
                                setGraphic( item.getGraphic() );
                                setPadding( new Insets( 5, 5, 5, -10 ) );
                            }
                        }
                    };
                }
            } );
            pageTreeRoot.setExpanded( true );
            tree.setRoot( pageTreeRoot );
            tree.setShowRoot( false );
            tree.setFocusTraversable( false );
            tree.getSelectionModel().selectedItemProperty()
                    .addListener( ( v, o, n ) -> {
                        if ( n != null && n.getValue() != null ){
                            ( (JInvPropertySheet) control ).pageProperty().set( n.getValue().getName() );
                        }
                    } );
            registerChangeListener(( (JInvPropertySheet) control ).pageProperty(), "PAGE");
        }

        // setup listeners
        registerChangeListener(control.modeProperty(), "MODE"); //$NON-NLS-1$
        registerChangeListener(control.propertyEditorFactory(), "EDITOR-FACTORY"); //$NON-NLS-1$
        registerChangeListener(control.titleFilter(), "FILTER"); //$NON-NLS-1$
        registerChangeListener(searchField.textProperty(), "FILTER-UI"); //$NON-NLS-1$
        registerChangeListener(control.modeSwitcherVisibleProperty(), "TOOLBAR-MODE"); //$NON-NLS-1$
        registerChangeListener(control.searchBoxVisibleProperty(), "TOOLBAR-SEARCH"); //$NON-NLS-1$

        control.getItems().addListener((ListChangeListener<PropertySheet.Item>) change -> refreshProperties());

        final ObservableMap<Object, Object> properties = control.getProperties();
        properties.remove("REFRESH");
        properties.addListener(propertiesMapListener);

        // initialize properly
        refreshProperties();
        updateToolbar();
    }

    MapChangeListener<Object, Object> propertiesMapListener = c -> {

        if (! c.wasAdded())
            return;

        if ("REFRESH".equals(c.getKey())) {
            refreshView();
            getSkinnable().getProperties().remove("REFRESH");
        }
    };


    /** */
    private void refreshView() {
        refreshProperties();
    }
    @Override public void dispose() {
        getSkinnable().getProperties().removeListener(propertiesMapListener);
        super.dispose();
    }

    /**
     * ************************************************************************
     *
     * Overriding public API
     *
     *************************************************************************
     */
    @Override
    protected void handleControlPropertyChanged(String p) {
        super.handleControlPropertyChanged(p);

        if (p == "MODE" || p == "EDITOR-FACTORY" || p == "FILTER" || p == "PAGE" ) {
            refreshProperties();
        } else if (p == "FILTER-UI") { //$NON-NLS-1$
            getSkinnable().setTitleFilter(searchField.getText());
        } else if (p == "TOOLBAR-MODE") { //$NON-NLS-1$
            updateToolbar();
        } else if (p == "TOOLBAR-SEARCH") { //$NON-NLS-1$
            updateToolbar();
        }
    }


    @Override
    protected void layoutChildren(double x, double y, double w, double h) {
        content.resizeRelocate(x, y, w, h);
    }

    /**
     * ************************************************************************
     *
     * Implementation
     *
     *************************************************************************
     */
    private void updateToolbar() {
        modeButton.setVisible(getSkinnable().isModeSwitcherVisible());
        searchField.setVisible(getSkinnable().isSearchBoxVisible());

        toolbar.setVisible(modeButton.isVisible() || searchField.isVisible());
    }

    private void refreshProperties() {
        scroller.setContent(buildPropertySheetContainer());
    }


    private Node buildPropertySheetContainer() {
        if ( showPages && pageTreeRoot.getChildren().isEmpty() ){
            initPageList();
        }

        switch (getSkinnable().modeProperty().get()) {
            case CATEGORY: {
                // group by category

                Map<String, List<PropertySheet.Item>> categoryMap = new TreeMap<>();
                if (getSkinnable() instanceof JInvPropertySheet) {
                    Comparator<? super String> categoryComparator = ((JInvPropertySheet) getSkinnable()).getCategoryComparator();
                    if (categoryComparator != null) {
                        categoryMap = new TreeMap<>(categoryComparator);
                    }
                }

                for (PropertySheet.Item p : getSkinnable().getItems()) {
                    String category = p.getCategory();
                    List<PropertySheet.Item> list = categoryMap.get(category);
                    if (list == null) {
                        list = new ArrayList<>();
                        categoryMap.put(category, list);
                    }
                    list.add(p);
                }

                VBox box = new VBox();
                for (String category : categoryMap.keySet()) {

                    PropertyPane props = new PropertyPane(categoryMap.get(category));
//                    // Only show non-empty categories
                    if (props.getChildrenUnmodifiable().size() > 0) {
                        TitledPane pane = new TitledPane(category, props);
                        pane.setExpanded(true);
                        box.getChildren().add(pane);
                    }
                }
                box.setFillWidth(true);
//                logger.info( "CATEGORY MAX WIDTH = {}", maxWidth );
                setCommonWidth(box);

                return box;
            }

            default:
//                logger.info( "default MAX WIDTH = {}", maxWidth );
                return new PropertyPane(getSkinnable().getItems());
        }

    }

    /** Список категорий с проверкой доступа по БД */
    private Map<String,Boolean> getSecurityMap(){
        if (securityMap == null){
            //Map<String, Boolean> map = new LinkedHashMap<>();
            //map.put("MAIL", JInvSecurityService.isCanAccessIsAction( BaseApp.APP().getCommonTaskContext(), 4099));
            Map<String, Boolean> map = (Map< String, Boolean >)getSkinnable().getProperties().get("r.i.securityMap");
            securityMap = U.nvl( map, Collections.emptyMap() );
        }
        return securityMap;
    }

    /** Устанавливает общую, наибольшую ширину лэйблов в режиме отображения категорий */
    private void setCommonWidth( final VBox box ) {
        box.getChildrenUnmodifiable()
           .stream()
           .filter( node -> node instanceof TitledPane )
           .map( node -> ( (TitledPane) node ).getContent() )
           .filter( node -> node instanceof GridPane )
           .map( node -> (GridPane) node )
           .forEach( gridPane -> {
            for (int colIndex = 0; colIndex < 2; colIndex++) {
                ColumnConstraints cc = new ColumnConstraints();
                cc.setHgrow( Priority.ALWAYS ); // allow column to grow
                cc.setFillWidth( true ); // ask nodes to fill space for column

                double commonWidth = this.maxWidth + 10; // some more space
                if ( colIndex == 0 ){
                    cc.setMaxWidth( commonWidth );
                }
                cc.setPrefWidth( commonWidth );
                gridPane.getColumnConstraints().add(cc);
            }
        } );
        maxWidth = MIN_COLUMN_WIDTH;
    }

    /** Начитываем список страниц */
    private void initPageList() {
        Map<String, Boolean> securityMap = getSecurityMap();

        pageTreeRoot.getChildren().addAll( getSkinnable().getItems().stream()
                .filter( i -> i instanceof PropertyItemValue )
                .map( i -> ((PropertyItemValue) i).getDescriptor().getPage(securityMap))
                .filter( i -> !i.equals( SettingsPage.NO_ACCESS ) )
                .distinct()
                .map( TreeItem::new )
                .collect( Collectors.toList() ));
        //Выбираем первую страницу
        if ( !pageTreeRoot.getChildren().isEmpty() && tree != null ){
            tree.getSelectionModel().selectFirst();
        }
    }

    private Node firstEditor;

    public Node getFirstEditor() {
        return firstEditor;
    }

    public void setFirstEditor(Node firstEditor) {
        this.firstEditor = firstEditor;
    }

    /**
     * ************************************************************************
     *
     * Support classes / enums
     *
     *************************************************************************
     */
    private class ActionChangeMode extends Action {

        private final Image CATEGORY_IMAGE = new Image(PropertySheetSkin.class.getResource("/org/controlsfx/control/format-indent-more.png").toExternalForm()); //$NON-NLS-1$
        private final Image NAME_IMAGE = new Image(PropertySheetSkin.class.getResource("/org/controlsfx/control/format-line-spacing-triple.png").toExternalForm()); //$NON-NLS-1$

        public ActionChangeMode(PropertySheet.Mode mode) {
            super(""); //$NON-NLS-1$
            setEventHandler(ae -> getSkinnable().modeProperty().set(mode));

            if (mode == PropertySheet.Mode.CATEGORY) {
                setGraphic(new ImageView(CATEGORY_IMAGE));
                setLongText(localize(asKey("property.sheet.group.mode.bycategory"))); //$NON-NLS-1$
            } else if (mode == PropertySheet.Mode.NAME) {
                setGraphic(new ImageView(NAME_IMAGE));
                setLongText(localize(asKey("property.sheet.group.mode.byname"))); //$NON-NLS-1$
            } else {
                setText("???"); //$NON-NLS-1$
            }
        }

    }

    private class PropertyPane extends GridPane {

        public PropertyPane(List<PropertySheet.Item> properties) {
            this(properties, 0);
        }

        public PropertyPane(List<PropertySheet.Item> properties, int nestingLevel) {
            setVgap(5);
            setHgap(5);
            setPadding(new Insets(5, 15, 5, 15 + nestingLevel * 10));
            getStyleClass().add("property-pane"); //$NON-NLS-1$
            setItems(properties);
        }

        public void setItems(List<PropertySheet.Item> properties) {
            getChildren().clear();

            String currentPage = "";
            if ( showPages ){
                currentPage = ( (JInvPropertySheet) getSkinnable() ).pageProperty().get();
            }

            String filter = getSkinnable().titleFilter().get();
            filter = filter == null ? "" : filter.trim().toLowerCase(); //$NON-NLS-1$

            int row = 0;

            Map<String, Boolean> securityMap = getSecurityMap();

            for (PropertySheet.Item item : properties) {
                if ( !currentPage.isEmpty() && item instanceof PropertyItemValue &&
                     !currentPage.toLowerCase().contains( ((PropertyItemValue)item ).getDescriptor().getPage(securityMap).getName().toLowerCase() )){
                    continue;
                }

                // filter properties
                String title = item.getName();

                if (!filter.isEmpty() && title.toLowerCase().indexOf(filter) < 0) {
                    continue;
                }

                // setup property label
                Label label = new Label(title);
                label.setMinWidth( MIN_COLUMN_WIDTH );
                label.setWrapText( true );

                // add icon if marker is detected
                if ( title.startsWith( String.valueOf(ICON_MARKER) ) ){
                    addEntryIcon( title, label );
                }

                // show description as a tooltip
                String description = item.getDescription();
                if (description != null && !description.trim().isEmpty()) {
                    label.setTooltip(new Tooltip(description));
                }

                // setup property editor
                Node editor = getEditor(item);

                if (editor instanceof Region) {
                    ((Region) editor).setMinWidth( MIN_COLUMN_WIDTH );
                    ((Region) editor).setMaxWidth(Double.MAX_VALUE);
                }
                label.setLabelFor(editor);

                if (firstEditor == null) {
                    setFirstEditor(editor);
                }

                double labelWidth = 0d;

                //Придаём привычный вид чекбоксам
                if ( editor instanceof CheckBox ){
                    ( (CheckBox) editor ).setGraphic( label );
                    add(editor, 1, row);
                } else {
                    labelWidth = fontLoader.computeStringWidth(label.getText(), label.getFont());

                    AnchorPane labelAnchor = new AnchorPane();
                    labelAnchor.getChildren().add( label );
                    AnchorPane.setTopAnchor( label,0d );
                    AnchorPane.setBottomAnchor( label,0d );
                    AnchorPane.setLeftAnchor( label,0d );
                    AnchorPane.setRightAnchor( label,0d );
                    label.setAlignment( Pos.CENTER_RIGHT );
                    add(labelAnchor, 0, row);
                    add(editor, 1, row);
                }

                itemEditorMap.put( item, editor );

                GridPane.setHgrow(editor, Priority.ALWAYS);

                row++;

                if ( labelWidth > maxWidth ){
                    maxWidth = labelWidth;
                }
            }
        }

        private Node getEditor(PropertySheet.Item item) {

            PropertyEditor editor = getSkinnable().getPropertyEditorFactory().call(item);

            if (editor == null) {
                editor = new AbstractPropertyEditor<Object, JInvTextField>(item, new JInvTextField(), true) {
                    {
                        getEditor().setEditable(false);
                        getEditor().setDisable(true);
                        //Есди LOV
                        if (!item.getType().getClass().isEnum() && !item.getType().getClass().isPrimitive()&&!item.getType().isInstance(BigDecimal.class)) {

                            getEditor().setEditable(true);
                            getEditor().setDisable(false);
                            getEditor().setLovClassName(item.getType().getName());

                            JInvEntityLov lov = new JInvEntityLov(item.getType());
//                            lov.entityValueProperty().addListener((observable, o, n) -> {
//                                for (Method m : n.getClass().getMethods()) {
//                                    if (m.getAnnotation(javax.persistence.Id.class) != null) {
//                                        try {
//                                            item.setValue(m.invoke(n));
//                                            return;
//                                        } catch (IllegalAccessException ex) {
//                                            logger.error("IllegalAccessException\n{}", ex.getMessage());
//                                        } catch (IllegalArgumentException ex) {
//                                            logger.error("IllegalArgumentException\n{}", ex.getMessage());
//                                        } catch (InvocationTargetException ex) {
//                                            logger.error("InvocationTargetException\n{}", ex.getMessage());
//                                        }
//
//                                    }
//                                }
//                            });
                            lov.setTaskContext(BaseApp.APP().getCommonTaskContext());
                            getEditor().setLOV(lov);
                        }
                    }

                    /**
                     * {@inheritDoc}
                     */
                    @Override
                    protected ObservableValue<Object> getObservableValue() {
                        return (ObservableValue<Object>) (Object) getEditor().textProperty();
                    }

                    /**
                     * {@inheritDoc}
                     */
                    @Override
                    public void setValue(Object value) {
                        getEditor().setText(value == null ? "" : value.toString()); //$NON-NLS-1$
                    }
                };
            } else if (!item.isEditable()) {
                Node editorNode = editor.getEditor();
                if ( editorNode instanceof IReadOnlyControl ) {
                    ( (IReadOnlyControl) editorNode ).setReadOnly(true);
                } else {
                    editorNode.setDisable( true );
                }
            }
            Object itemValue = item.getValue();
            if ( itemValue != null ){
                editor.setValue( itemValue );
            }
            return editor.getEditor();
        }
    }

    /**
     Добавляет иконку FontAwesome, если перед текстом стоит её название в косых апострофах (`)
     */
    public void addEntryIcon( final String title, final Label label ) {
        String prefix = title.substring( 0, title.lastIndexOf( ICON_MARKER ) + 1 );
        label.setText(title.substring( prefix.length() ));
        try {
            FontAwesome icon = Enum.valueOf( FontAwesome.class, prefix.replace( String.valueOf( ICON_MARKER ), "" ) );
            Label glyph = IconFactory.getLabel( icon );
            label.setGraphic( new Group(label, glyph) );
        } catch ( Exception e ) {
            logger.error( "Faulty icon prefix!\n{}", e.getMessage() );
        }
    }

    public HashMap<PropertySheet.Item, Node> getItemEditorMap() {
        return itemEditorMap;
    }

}
