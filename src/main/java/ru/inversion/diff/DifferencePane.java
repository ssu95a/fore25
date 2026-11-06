package ru.inversion.diff;

import javafx.geometry.Orientation;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.AnchorPane;
import ru.inversion.fx.form.controls.JInvTextArea;

import java.util.ResourceBundle;

/**
 * Простая панель для детального отображения нового и старого значения
 */
public class DifferencePane extends AnchorPane {

    private static final ResourceBundle FORE = ResourceBundle.getBundle("fore");

    private final SplitPane splitPane;

    private final JInvTextArea oldValueTextArea;

    private final JInvTextArea newValueTextArea;

    public DifferencePane(String oldValue, String newValue) {
        this.splitPane = new SplitPane();
        AnchorPane.setTopAnchor(splitPane, 0.0);
        AnchorPane.setRightAnchor(splitPane, 0.0);
        AnchorPane.setBottomAnchor(splitPane, 0.0);
        AnchorPane.setLeftAnchor(splitPane, 0.0);
        splitPane.setDividerPositions(0.5);
        splitPane.setOrientation(Orientation.HORIZONTAL);

        this.oldValueTextArea = new JInvTextArea(oldValue);
        final TitledPane oldValueTitledPane = new TitledPane(FORE.getString("DIFF_OLD_VALUE_COLUMN"), oldValueTextArea);
        oldValueTitledPane.setCollapsible(false);
        AnchorPane.setTopAnchor(oldValueTitledPane, 0.0);
        AnchorPane.setRightAnchor(oldValueTitledPane, 0.0);
        AnchorPane.setBottomAnchor(oldValueTitledPane, 0.0);
        AnchorPane.setLeftAnchor(oldValueTitledPane, 0.0);
        this.newValueTextArea = new JInvTextArea(newValue);
        final TitledPane newValueTitledPane = new TitledPane(FORE.getString("DIFF_NEW_VALUE_COLUMN"), newValueTextArea);
        newValueTitledPane.setCollapsible(false);
        AnchorPane.setTopAnchor(newValueTitledPane, 0.0);
        AnchorPane.setRightAnchor(newValueTitledPane, 0.0);
        AnchorPane.setBottomAnchor(newValueTitledPane, 0.0);
        AnchorPane.setLeftAnchor(newValueTitledPane, 0.0);

        final AnchorPane leftPane = new AnchorPane(oldValueTitledPane);
        final AnchorPane rightPane = new AnchorPane(newValueTitledPane);
        splitPane.getItems().addAll(leftPane, rightPane);
        getChildren().add(splitPane);
    }
}
