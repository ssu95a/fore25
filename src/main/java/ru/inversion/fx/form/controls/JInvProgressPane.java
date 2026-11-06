package ru.inversion.fx.form.controls;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.text.Text;

/**
 *
 * @author banin
 */
public class JInvProgressPane extends AnchorPane implements IDynamicProgressControl {

    private ProgressIndicator progressIndicator = new ProgressBar();

    private Text progressText = new Text("...");

    private HBox hBox;

    public JInvProgressPane() {
        init();
    }

    private void init() {
        final ObservableList<Node> rootChildren = getChildren();

        if (rootChildren.contains(hBox)) rootChildren.remove(hBox);

        this.hBox = new HBox();
        hBox.setSpacing(5);
        hBox.setAlignment(Pos.CENTER);
        hBox.getChildren().addAll(progressIndicator, progressText);
        rootChildren.add(hBox);

        AnchorPane.setTopAnchor(hBox, 5.0);
        AnchorPane.setRightAnchor(hBox, 5.0);
        AnchorPane.setBottomAnchor(hBox, 5.0);
        AnchorPane.setLeftAnchor(hBox, 5.0);
    }

    @Override
    public void toIndicator() {
        this.progressIndicator = new ProgressIndicator();
        init();
    }

    @Override
    public void toProgress() {
        this.progressIndicator = new ProgressBar();
        init();
    }

    @Override
    public DoubleProperty progressProperty() {
        return progressIndicator.progressProperty();
    }

    @Override
    public StringProperty textProperty() {
        return progressText.textProperty();
    }

    public ProgressIndicator getProgressIndicator() {
        return progressIndicator;
    }

    public Text getProgressText() {
        return progressText;
    }

    public HBox gethBox() {
        return hBox;
    }
}
