package ru.inversion.fx.form.mdi;

import javafx.scene.control.Button;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.stage.Window;
import ru.inversion.fx.app.AppException;
import ru.inversion.fx.form.ViewContext;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * @author antonovdi
 */
public class JInvFrameBar extends GridPane implements IWindowManager {

    private HBox windowBar;
    private Map< JInvWindowMdi, Button> mapButtonWindow = new LinkedHashMap<>();

    public JInvFrameBar() {
        super();
        windowBar = new HBox();
        add(windowBar, 0, 0);
    }

    public List< JInvWindowMdi > getWindows() {
        return new ArrayList(mapButtonWindow.keySet());
    }

    @Override
    public void addWindow(String title, JInvWindowMdi window) {

//        if (window != null) {
//            Button bt = new Button(title);
//            bt.textProperty().bind(window.titleProperty());
//            mapButtonWindow.put(window, bt);
//            bt.getStyleClass().add("bar-button");
//            bt.setOnAction((ActionEvent event) -> {
//
//                window.restoreOrMinimizeOrCenter();
//
//            });
//            window.selectedProperty().addListener((ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) -> {
//                bt.pseudoClassStateChanged(PseudoClass.getPseudoClass("current"), newValue);
//            });
//
//            bt.disableProperty().bind(window.disabledProperty());
//            windowBar.getChildren().add(bt);
//        }
    }

    @Override
    public void removeWindow(Window window) {
        if (window != null) {
            Button bt = mapButtonWindow.get(window);
            windowBar.getChildren().remove(bt);
            mapButtonWindow.remove(window);
        }
    }

    @Override
    public void selectWindow( Window window) {

//        mapButtonWindow.keySet().stream().forEach((Window t) -> {
//            t.requestSelection(false);
//        });
//        window.requestSelection(true);
    }

    public void cascadeWindows() {

//        if (!mapButtonWindow.isEmpty()) {
//            JInvWindowSkin skin = (JInvWindowSkin) mapButtonWindow.keySet().stream().findFirst().get().getSkin();
//            double offset = skin.getTitleBar().getHeight();
//            double x = 0;
//            double y = 0;
//
//            for (JInvWindowMdi w : mapButtonWindow.keySet()) {
//                w.relocate(x, y);
//                w.toFrontSuper();
//                x += offset;
//                y += offset;
//            }
//        }
    }

    public void tileWindows() {
//        if (!mapButtonWindow.isEmpty()) {
//
//            int amountRowInColumn = 3;
//            int amouuntColumn = (int) (Math.ceil(((double) mapButtonWindow.size()) / amountRowInColumn));
//
//            ScrollPane pane = BaseApp.APP().getMainFrame().getScrollPane();
//
//            double scrollBarWidth = ((ScrollBar) pane.lookup(".scroll-bar")).getWidth();
//
//            double widthOfMdiPane = pane.getWidth() - 5;
//            double heightOfMdiPane = pane.getHeight() - 5;
//
//            double widthColumn = widthOfMdiPane / amouuntColumn;
//            List<Window> listWindow = new ArrayList(mapButtonWindow.keySet());
//            int indexOfWindow = 0;
//            double x = 0;
//
//            for (int currentColumnIndex = 1; currentColumnIndex <= amouuntColumn; currentColumnIndex++) {
//
//                double heightOfRow = heightOfMdiPane / amountRowInColumn;
//                double y = 0;
//
//                if (listWindow.size() - indexOfWindow < amountRowInColumn) {
//                    heightOfRow = heightOfMdiPane / (listWindow.size() - indexOfWindow);
//                }
//
//                for (int rowNumber = 1; rowNumber <= amountRowInColumn; rowNumber++) {
//                    Window w = listWindow.get(indexOfWindow);
//                    w.setPrefSize(widthColumn, heightOfRow);
//                    w.relocate(x, y);
//                    if (indexOfWindow + 1 == listWindow.size()) {
//                        break;
//                    } else {
//                        indexOfWindow++;
//                        y += heightOfRow;
//                    }
//                }
//
//                x += widthColumn;
//            }
//        }
    }

    /**
     * Центрирует окно, относительно родительского окна
     *
     * @param window
     * @throws AppException
     */
    public void positionWindow( JInvWindowMdi window, ViewContext vc) throws AppException {
//
//        // Если в базе есть настройка точки верхнего левого угла, то позиционируем туда
//        // Иначе центрируем относительно родителя, если таковой имеется
//        PPrefComponent pref = vc.getViewPrefSaver().getInitialPrefs().stream().
//            filter((PPrefComponent t) -> t.getCOMPONENT()!=null && t.getCOMPONENT().equals(ViewPrefAppService.FORM_START_POINT)).findFirst().orElse(null);
//
//        if (window != null && pref != null && pref.getWIDTH() != null && pref.getHEIGHT() != null) {
//
//            window.relocate(pref.getWIDTH() > 0 ? pref.getWIDTH() / DIMENSION_FRACTIONAL_FACTOR : 0,
//                pref.getHEIGHT() > 0 ? pref.getHEIGHT() / DIMENSION_FRACTIONAL_FACTOR : 0);
//
//        } else if (window != null && window.getParentWindow() != null) {
//            JInvWindowMdi parentWindow = window.getParentWindow();
//            Bounds boundsParent = parentWindow.localToParent(parentWindow.getBoundsInLocal());
//            double centerXCorner = boundsParent.getMinX() + boundsParent.getWidth() / 2;
//            double centerYCorner = boundsParent.getMinY() + boundsParent.getHeight() / 2;
//
//            double fontSize = BaseApp.APP().getViewPrefService().getFont().getSize();
//
////
//            double centerX = centerXCorner - (window.getPrefWidth() * (fontSize / 12)) / 2;
//            double centerY = centerYCorner - (window.getPrefHeight() * (fontSize / 12)) / 2;
//
////
////                System.out.println(centerX + " " + centerY);
//            window.relocate(centerX > 0 ? centerX : 0, centerY > 0 ? centerY : 0);
//        }
    }

}
