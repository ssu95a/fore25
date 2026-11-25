package ru.inversion.fx.form.controls.skin;
import static java.lang.invoke.MethodHandles.lookup;
import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.css.PseudoClass;
import javafx.css.Styleable;
import javafx.geometry.Side;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.control.Control;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.skin.TabPaneSkin;
import javafx.scene.layout.StackPane;
import org.slf4j.Logger;
import static org.slf4j.LoggerFactory.getLogger;
import ru.inversion.fx.form.controls.JInvTabPane;

/**
 @author fomishkin on 31.03.2020. */
public class JInvTabPaneSkin extends TabPaneSkin {
    private static final String HORIZONTAL_LEFT_TAB = "ru.inversion.horizontal_left_tab";
    private static final String STYLECLASS_TAB_LEFT = "tab-left";
    private static final Number DEFAULT_LEFT_WIDTH = 150;
    private final PseudoClass left = PseudoClass.getPseudoClass( "left" );

    private final static Logger logger = getLogger( lookup().lookupClass() );
    private Number prefWidth;
    private boolean leftLookInstalled;

    /**
     Creates a new TabPaneSkin instance, installing the necessary child
     nodes into the Control {@link Control#getChildren() children} list, as
     well as the necessary input mappings for handling key, mouse, etc events.

     @param control The control that this skin should be installed onto. */
    public JInvTabPaneSkin( final JInvTabPane control ) {
        super( control );
        initialise(control);
    }

    private void initialise( final JInvTabPane control ) {
        control.setTabClosingPolicy( TabPane.TabClosingPolicy.UNAVAILABLE );

        registerChangeListener(control.prefWidthProperty(), observable -> {
            if (control.getPrefWidth() > 0) {
                this.prefWidth = control.getPrefWidth();
            }
        });
        registerChangeListener(control.sideProperty(), observable -> updateLeftLook(control));
        updateLeftLook(control);
    }

    private void updateLeftLook(JInvTabPane control) {
        if (control.getSide() != Side.LEFT) {
            return;
        }
        control.pseudoClassStateChanged(left, true);
        pseudoClassStateChanged(left, true);
        control.getTabs().forEach(this::initLeftTabLook);

        if (!leftLookInstalled) {
            leftLookInstalled = true;
            registerListChangeListener(control.getTabs(), change -> {
                while (change.next()) {
                    if (change.wasAdded()) {
                        control.getTabs().subList(change.getFrom(), change.getTo()).forEach(this::initLeftTabLook);
                    }
                }
            });
        }
        Platform.runLater(() -> {
            if (getSkinnable() != null) {
                control.setRotateGraphic(true);
            }
        });
    }

    private void initLeftTabLook( final Tab tab ) {
        //Срабатывает ПОСЛЕ JInvTabPane.addDragSupport
        Platform.runLater( () -> {

        if (getSkinnable() == null) {
            return;
        }

        Boolean isInitialised = (Boolean) tab.getProperties().getOrDefault( HORIZONTAL_LEFT_TAB, false );
        if ( isInitialised ){
            return;
        }

        tab.setStyle( "-fx-pref-height: " + (prefWidth == null ? DEFAULT_LEFT_WIDTH : prefWidth) );

        Node graphic = tab.getGraphic();
//        logger.info( "initTabLook. {} tab's graphic: {}, text: {}", tab, graphic, tab.getText() );

        if ( graphic != null ){
            graphic.setRotate(90);
            StackPane stp = new StackPane(new Group(graphic));
            tab.setGraphic(stp);
        }
        addStyleClassIfAbsent( tab, STYLECLASS_TAB_LEFT );

        tab.getProperties().put( HORIZONTAL_LEFT_TAB, true );

        } );
    }

    //util candidate
    private static void addStyleClassIfAbsent( Styleable styleable, String styleClass) {
        ObservableList<String> styleClasses = styleable.getStyleClass();
        if (!styleClasses.contains(styleClass)) {
            styleClasses.add(styleClass);
        }
    }
}
