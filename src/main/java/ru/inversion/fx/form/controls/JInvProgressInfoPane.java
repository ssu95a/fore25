package ru.inversion.fx.form.controls;

import javafx.beans.binding.Bindings;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.Separator;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.*;
import javafx.scene.paint.Paint;

import java.util.ResourceBundle;

public class JInvProgressInfoPane extends AnchorPane implements IDynamicProgressControl, IMultiProgressControl {

    private static final ResourceBundle FORE = ResourceBundle.getBundle("fore");

    private VBox vBox;

    private JInvButton cancelBtn;

    private TitledPane titlePane;

    private VBox bottomVBox;

    private ProgressIndicator progressIndicator;

    private ProgressBar innerProgressBar;

    private JInvLabel progressText;

    private JInvLabel innerProgressText;

    private final boolean innerProgress;

    public JInvProgressInfoPane() {
        this(false);
    }

    public JInvProgressInfoPane(boolean innerProgress) {
        setPrefWidth(300);
        setPrefHeight(150);

        setStyle("-fx-background-color: transparent;");

        this.progressText = new JInvLabel("...");
        progressText.setTextFill(Paint.valueOf("#565656"));
        this.innerProgressText = new JInvLabel();
        innerProgressText.setTextFill(Paint.valueOf("#565656"));
        innerProgressText.managedProperty().bind(Bindings.isNotEmpty(innerProgressText.textProperty()));
        this.cancelBtn = new JInvButton(FORE.getString("CANCEL"));
        this.innerProgress = innerProgress;

        init();
    }

    @Override
    public void toIndicator() {
        final StackPane progressBarPane = buildProgressIndicatorPane();
        replaceProgressBar(progressBarPane);
    }

    @Override
    public void toProgress() {
        final AnchorPane progressIndicatorPane = buildProgressBarPane();
        replaceProgressBar(progressIndicatorPane);
    }

    @Override
    public DoubleProperty progressProperty() {
        return progressIndicator.progressProperty();
    }

    @Override
    public StringProperty textProperty() {
        return progressText.textProperty();
    }

    @Override
    public DoubleProperty innerProgressProperty() {
        return innerProgressBar.progressProperty();
    }

    @Override
    public StringProperty innerTextProperty() {
        return innerProgressText.textProperty();
    }

    public void setTitleText(String text) {
        titlePane.setText(text);
    }

    public void showCancelButton() {
        cancelBtn.setVisible(true);
        cancelBtn.setManaged(true);
    }

    public void hideCancelButton() {
        cancelBtn.setVisible(false);
        cancelBtn.setManaged(false);
    }

    public JInvButton getCancelBtn() {
        return cancelBtn;
    }

    private void init() {
        final ObservableList<Node> rootChildren = getChildren();

        if (rootChildren.contains(vBox)) rootChildren.remove(vBox);

        this.vBox = new VBox();
        vBox.setSpacing(10);
        vBox.setPrefWidth(100);
        vBox.setPrefHeight(200);
        AnchorPane.setTopAnchor(vBox, 5.0);
        AnchorPane.setRightAnchor(vBox, 5.0);
        AnchorPane.setBottomAnchor(vBox, 5.0);
        AnchorPane.setLeftAnchor(vBox, 5.0);
        vBox.setStyle("-fx-effect: dropshadow(gaussian, rgba(0, 0, 0, 0.2), 10, 0.5, 0.0, 0.0);" +
                " -fx-background-color: white;");

        titlePane = buildTitledPane();

        VBox.setVgrow(titlePane, Priority.ALWAYS);
        vBox.getChildren().addAll(titlePane);

        rootChildren.add(vBox);
    }

    private AnchorPane buildProgressBarPane() {
        this.progressIndicator = new ProgressBar();
        progressIndicator.setId("mainProgress");
        progressIndicator.setMinHeight(25);
        AnchorPane.setTopAnchor(progressIndicator, 0.0);
        AnchorPane.setRightAnchor(progressIndicator, 0.0);
        AnchorPane.setBottomAnchor(progressIndicator, 0.0);
        AnchorPane.setLeftAnchor(progressIndicator, 0.0);
        AnchorPane progressPane = new AnchorPane();
        progressPane.getChildren().add(progressIndicator);
        return progressPane;
    }

    private AnchorPane buildInnerProgressBarPane() {
        this.innerProgressBar = new ProgressBar();
        innerProgressBar.setId("innerProgress");
        innerProgressBar.setMinHeight(12);
        innerProgressBar.setMaxHeight(12);
        AnchorPane.setTopAnchor(innerProgressBar, 0.0);
        AnchorPane.setRightAnchor(innerProgressBar, 0.0);
        AnchorPane.setBottomAnchor(innerProgressBar, 0.0);
        AnchorPane.setLeftAnchor(innerProgressBar, 0.0);
        AnchorPane progressPane = new AnchorPane();
        progressPane.getChildren().add(innerProgressBar);
        return progressPane;
    }

    private StackPane buildProgressIndicatorPane() {
        this.progressIndicator = new ProgressIndicator();
        progressIndicator.setId("mainProgress");
        progressIndicator.setMinHeight(50);
        progressIndicator.setMinHeight(50);
        StackPane progressIndicatorPane = new StackPane();
        progressIndicatorPane.getChildren().add(progressIndicator);
        return progressIndicatorPane;
    }

    /**
     * Панель индикатора загрузки
     */
    private TitledPane buildTitledPane() {
        final AnchorPane progressPane = buildProgressBarPane();
        StackPane textPane = new StackPane();
        textPane.setAlignment(Pos.CENTER);
        textPane.getChildren().add(progressText);

        VBox contentBox = new VBox();
        contentBox.setAlignment(Pos.CENTER);
        contentBox.setSpacing(10);

        final Separator separator = new Separator();
        separator.visibleProperty().bind(cancelBtn.visibleProperty());
        separator.managedProperty().bind(cancelBtn.managedProperty());
        if (innerProgress) {
            final AnchorPane innerProgressPane = buildInnerProgressBarPane();
            contentBox.getChildren().addAll(progressPane, textPane, innerProgressPane,
                    innerProgressText, separator, cancelBtn);
        } else {
            contentBox.getChildren().addAll(progressPane, textPane, separator, cancelBtn);
        }

        TitledPane titledPane = new TitledPane();
        titledPane.setMinHeight(100);
        titledPane.setMaxHeight(500);
        titledPane.setCollapsible(false);
        titledPane.setContent(contentBox);
        titledPane.getStyleClass().add("progress_title_pane");

        return titledPane;
    }

    private void replaceProgressBar(Node progressPane) {
        final VBox topPaneContent = (VBox) titlePane.getContent();
        final ObservableList<Node> vBoxChildren = topPaneContent.getChildren();
        vBoxChildren.stream()
                .filter(node -> {
                    if (node instanceof AnchorPane || node instanceof StackPane) {
                        final Node progressIndicator = ((Pane) node).getChildren().get(0);
                        return progressIndicator.getId().equals("mainProgress");
                    }
                    return false;
                })
                .findFirst()
                .ifPresent(node -> {
                    final int progressIndex = vBoxChildren.indexOf(node);
                    vBoxChildren.set(progressIndex, progressPane);
                });
    }

    public boolean isInnerProgress() {
        return innerProgress;
    }

    public TitledPane getTitlePane() {
        return titlePane;
    }
}
