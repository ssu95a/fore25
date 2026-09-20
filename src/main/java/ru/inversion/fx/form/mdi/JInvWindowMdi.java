/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ru.inversion.fx.form.mdi;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Region;
import ru.inversion.fx.app.BaseApp;
import ru.inversion.fx.app.es.JInvErrorService;

/**
 *
 * @author antonovdi
 */
public class JInvWindowMdi extends javafx.stage.Stage {

    private JInvWindowMdi parentWindow;
    private final ObservableList<Node> rightButtons = FXCollections.observableArrayList();
    private final ObservableList<Node> leftButtons = FXCollections.observableArrayList();

    private BooleanProperty maximizeAllowedProperty = new SimpleBooleanProperty(true);
    private BooleanProperty minimizeAllowedProperty = new SimpleBooleanProperty(true);

    public static final String CSS_MDI_ICON_CLASS = "mdi-icon";
    public static final String CSS_CLOSED_ICON_CLASS = "closed-icon";
//    public static final String CSS_MINIMIZE_ICON_CLASS = "minimize-icon";

    public boolean isMaximizeAllowed() {
        return maximizeAllowedProperty.get();
    }

    public void setMaximizeAllowed(boolean val) {
        maximizeAllowedProperty.set(val);
    }

    public BooleanProperty maximizeAllowedProperty() {
        return maximizeAllowedProperty;
    }

    public boolean isMinimizeAllowed() {
        return minimizeAllowedProperty.get();
    }

    public void setMinimizeAllowed(boolean val) {
        minimizeAllowedProperty.set(val);
    }

    public BooleanProperty minimizeAllowedProperty() {
        return minimizeAllowedProperty;
    }

    public ObservableList<Node> getRightButtons() {
        return rightButtons;
    }

    public ObservableList<Node> getLeftButtons() {
        return leftButtons;
    }

    public Button getCloseButton() {

        switch (getRightButtons().size()) {
            case 2:
                return (Button) getRightButtons().get(1);
            case 3:
                return (Button) getRightButtons().get(2);
            default:
                return null;
        }
    }

    public JInvWindowMdi getParentWindow() {
        return parentWindow;
    }

    public void setParentWindow( JInvWindowMdi parentWindow) {
        this.parentWindow = parentWindow;
    }

    public JInvWindowMdi( String title, Region content) {
//        super(title);
        BaseApp.APP().reportGuiLaunch();
        init(content);
    }

    public JInvWindowMdi( String title) {
//        super(title);
        BaseApp.APP().reportGuiLaunch();
    }

    public void addRegion(Region content) {
        init(content);
//        getContentPane().getChildren().add(content);
//        applyCss();
    }

    private void init(Region content) {

        try {

            // У некоторых компонентов почему-то по дефолту минимальные и максимальные размеры ставятся -Infinity
            // из-за этого отображение размеров некорректное. Приходится делать такое преобразование
            if (Double.isInfinite(content.getMinHeight())) {
                content.setMinHeight(-1.0);
            }
            if (Double.isInfinite(content.getMinWidth())) {
                content.setMinWidth(-1.0);
            }
            if (Double.isInfinite(content.getMaxHeight())) {
                content.setMaxHeight(-1.0);
            }
            if (Double.isInfinite(content.getMaxWidth())) {
                content.setMaxWidth(-1.0);
            }

//            ButtonBase closeButton =
//                    ActionFactory.createButton( new IconDescriptorBuilder<>( MaterialDesign.mdi_window_close ).build(),
//                    (ActionEvent event) -> {
//                close();
//            });
//            closeButton.getStyleClass().add(CSS_MDI_ICON_CLASS);
//            closeButton.getStyleClass().add(CSS_CLOSED_ICON_CLASS);
//
//            if (closeButton.getGraphic() instanceof Label) {
//                Label lbClose = (Label) closeButton.getGraphic();
////                lbClose.setStyle(lbClose.getStyle().concat("-fx-text-fill:white;-fx-font-size:1.3em;"));
////                lbClose.setEffect(new InnerShadow(BlurType.ONE_PASS_BOX, Color.BLACK, 2, 1, 0, 0));
//            }
//
//            ButtonBase minimizeButton =
//                    ActionFactory.createButton(new IconDescriptorBuilder<>(MaterialDesign.mdi_window_minimize).build(),
//                    (ActionEvent event) -> {
//                        setMinimized(!isMinimized());
//                    });
//
//            if (minimizeButton.getGraphic() instanceof Label) {
//                Label lbMimimize = (Label) minimizeButton.getGraphic();
//                minimizeButton.getStyleClass().add(CSS_MDI_ICON_CLASS);
////                minimizeButton.getStyleClass().add(CSS_MINIMIZE_ICON_CLASS);
////                lbMimimize.setStyle(lbMimimize.getStyle().concat("-fx-text-fill:white;-fx-font-size:1.3em;"));
////                lbMimimize.setEffect(new InnerShadow(BlurType.ONE_PASS_BOX, Color.BLACK, 2, 1, 0, 0));
//            }
//            minimizeButton.disableProperty().bind(minimizeAllowedProperty.not());
//            Label maximizeIcon = IconFactory.getLabel( new IconDescriptorBuilder<>( MaterialDesign.mdi_window_maximize ).build() );
//            Label restoreIcon = IconFactory.getLabel( new IconDescriptorBuilder<>( MaterialDesign.mdi_window_restore ).build() );
//            ButtonBase expandButton =
//                    ActionFactory.createButton( new IconDescriptorBuilder<>( MaterialDesign.mdi_window_maximize ).build(),
//                    (ActionEvent event) -> {
//                        try {
//                            setMaximized(!isMaximized());
//                            ( (ButtonBase) event.getTarget() ).setGraphic( isMaximized() ? restoreIcon : maximizeIcon );
//                        } catch (Throwable ex) {
//                            JInvErrorService.handleException(null, ex);
//                        }
//                    });
//
//            if (expandButton.getGraphic() instanceof Label) {
//                Label lbExpand = (Label) expandButton.getGraphic();
//                expandButton.getStyleClass().add(CSS_MDI_ICON_CLASS);
////                expandButton.getStyleClass().add(CSS_MINIMIZE_ICON_CLASS);
////                lbExpand.setStyle(lbExpand.getStyle().concat("-fx-text-fill:white;-fx-font-size:1.3em;"));
////                lbExpand.setEffect(new InnerShadow( BlurType.THREE_PASS_BOX, Color.BLACK, 2, 1, 0, 0));
//            }
//
//            expandButton.disableProperty().bind(resizeableWindowProperty().not().or(maximizeAllowedProperty).not());
//
//            getRightButtons().add(minimizeButton);
//
//            if (content.getMaxHeight() == -1.0 && content.getMaxWidth() == -1.0) {
//                getRightButtons().add(expandButton);
//            }
//            getRightButtons().add(closeButton);
//            getLeftButtons().add(new Label("", new ImageView(BaseApp.APP().getViewPrefService().getAppIcon())));
//
//            setOnClosedAction((ActionEvent event) -> {
//                BaseApp.APP().getMainFrame().getWindowManager().removeWindow(JInvWindowMdi.this);
//            });

        } catch (Throwable ex) {
            JInvErrorService.handleException(null, ex);
        }
    }

    public boolean isWindowInViewPort() {
        ScrollPane scroll = BaseApp.APP().getMainFrame().getScrollPane();
        return false;// scroll.localToScene(scroll.getBoundsInLocal()).intersects(this.localToScene(this.getBoundsInLocal()));
    }

    public void centerOnScroll() {
        ScrollPane scroll = BaseApp.APP().getMainFrame().getScrollPane();
//
        Bounds boundsAnchorPane = ((AnchorPane) scroll.getContent()).getBoundsInLocal();
//        Bounds boundsWindow = this.getBoundsInParent();
        Bounds boundsWindow = null;//this.get;

        scroll.setVvalue(0);
        scroll.setHvalue(0);

        double xMax = boundsWindow.getMaxX();
        double right = scroll.getViewportBounds().getWidth();
        double width = boundsAnchorPane.getWidth() - right;

//        System.out.println("width "+width);
        for (double i = 0; i <= 1; i += 0.1) {
            scroll.setHvalue(i);

//            System.out.println("right "+right + " xMax "+xMax);
            if (right > xMax) {
//                System.out.println("right found");
                break;
            } else {
                right = right + 0.1 * width;
            }
        }

        double yMax = boundsWindow.getMaxY();
        double bottom = scroll.getViewportBounds().getHeight();
        double height = boundsAnchorPane.getHeight() - bottom;

        for (double i = 0; i <= 1; i += 0.1) {
            scroll.setVvalue(i);
//            System.out.println("bottom "+bottom + " yMax "+yMax);
            if (bottom > yMax) {
//                System.out.println("bottom found");
                break;
            } else {
                bottom = bottom + 0.1 * height;
            }
        }
    }

    //@Override
    public void toFront() {
        //super.toFront(); //To change body of generated methods, choose Tools | Templates.
        //BaseApp.APP().getMainFrame().getWindowManager().selectWindow(this);
    }

    public void toFrontSuper() {
        //super.toFront();
    }

    //@Override
    public String getUserAgentStylesheet() {
        return this.getClass().getClassLoader().getResource("css/general.css").toExternalForm();
    }

    public static void switchBlockParentWindow( JInvWindowMdi window, boolean val) {
//        if (window != null) {
//            window.disableProperty().set(val);
//            window.requestFocus();
//        }
    }

    /**
     * Восстанавливает окно, если оно свернуто
     */
    public void restore() {
//        setMinimized(false);
//        if (isWindowInViewPort()) {
//            centerOnScroll();
//        }
//        toFront();
    }

    /**
     * Если окно свернуто, разворачиваем его. Если окно размернуто сворачиваем его. Если окно не видно, то центрируем его
     */
    public void restoreOrMinimizeOrCenter() {
//        if (isMinimized()) {
//            restore();
//        } else if (isWindowInViewPort()) {
//            centerOnScroll();
//            toFront();
//        } else if (isSelected()) {
//            setMinimized(true);
//            toFront();
//        }

    }
}
