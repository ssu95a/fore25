package ru.inversion.fx.form.controls;

import javafx.animation.FadeTransition;
import javafx.beans.binding.Bindings;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.StringProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Paint;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.util.Duration;
import ru.inversion.icons.IconFactory;
import ru.inversion.icons.enums.FontAwesome;

import java.util.ResourceBundle;

public class JInvMultiProgressInfoPane extends AnchorPane {

    private static final ResourceBundle FORE = ResourceBundle.getBundle("fore");

    private final VBox generalVBox;

    private final VBox secondaryVBox;

    private final GeneralProgress generalProgress;

    public JInvMultiProgressInfoPane() {
        this.generalVBox = new VBox();
        generalVBox.setSpacing(10);
        AnchorPane.setTopAnchor(generalVBox, 5.0);
        AnchorPane.setRightAnchor(generalVBox, 5.0);
        AnchorPane.setBottomAnchor(generalVBox, 5.0);
        AnchorPane.setLeftAnchor(generalVBox, 5.0);
        this.generalProgress = new GeneralProgress();

        final AnchorPane secondaryProgressPane = new AnchorPane();
        secondaryProgressPane.setPadding(new Insets(0, 5, 0, 0));
        final ScrollPane scrollPane = new ScrollPane();
        VBox.setVgrow(scrollPane, Priority.ALWAYS);
        scrollPane.setStyle("-fx-background-color:transparent;");
        scrollPane.setFitToWidth(true);
        scrollPane.setContent(secondaryProgressPane);
        this.secondaryVBox = new VBox();
        secondaryVBox.setSpacing(10);
        AnchorPane.setTopAnchor(secondaryVBox, 0.0);
        AnchorPane.setRightAnchor(secondaryVBox, 0.0);
        AnchorPane.setBottomAnchor(secondaryVBox, 0.0);
        AnchorPane.setLeftAnchor(secondaryVBox, 0.0);
        secondaryProgressPane.getChildren().add(secondaryVBox);

        this.generalVBox.getChildren().addAll(scrollPane, new Separator(), generalProgress);

        getChildren().add(generalVBox);
    }

    public SecondaryProgress addSecondaryProgress(boolean indicator) {
        return addSecondaryProgress(indicator, false);
    }

    public SecondaryProgress addSecondaryProgress(boolean indicator, boolean allowCancel) {
        final AnchorPane secondaryProgress = createSecondaryProgress(indicator, allowCancel);
        secondaryVBox.getChildren().add(secondaryProgress);
        return (SecondaryProgress) secondaryProgress.getChildren().get(0);
    }

    private AnchorPane createSecondaryProgress(boolean indicator, boolean allowCancel) {
        final AnchorPane rootPane = new AnchorPane();
        rootPane.getStyleClass().add("multitask_pane");
        final SecondaryProgress secondaryProgress = new SecondaryProgress(indicator, allowCancel);
        secondaryProgress.getProgressIndicator().setProgress(-1);
        AnchorPane.setTopAnchor(secondaryProgress, 5.0);
        AnchorPane.setRightAnchor(secondaryProgress, 5.0);
        AnchorPane.setBottomAnchor(secondaryProgress, 5.0);
        AnchorPane.setLeftAnchor(secondaryProgress, 5.0);
        rootPane.getChildren().add(secondaryProgress);
        return rootPane;
    }

    public GeneralProgress getGeneralProgress() {
        return generalProgress;
    }

    public final class GeneralProgress extends VBox implements IProgressControl {

        private final JInvLabel text;

        private final HBox hBox;

        private final ProgressBar progressBar;

        private final JInvButton cancelBtn;
        private final JInvButton okBtn;      

        GeneralProgress() {
            VBox.setMargin(this, new Insets(5, 0, 0, 0));
            setSpacing(5);

            this.text = new JInvLabel();
            text.setWrapText(true);

            this.hBox = new HBox();
            this.hBox.setSpacing(10);

            this.progressBar = new ProgressBar();
            this.progressBar.setMinHeight(25);
            AnchorPane.setTopAnchor(this.progressBar, 0.0);
            AnchorPane.setRightAnchor(this.progressBar, 0.0);
            AnchorPane.setBottomAnchor(this.progressBar, 0.0);
            AnchorPane.setLeftAnchor(this.progressBar, 0.0);

            final AnchorPane anchorPane = new AnchorPane();
            anchorPane.getChildren().add(this.progressBar);
            HBox.setHgrow(anchorPane, Priority.ALWAYS);

            this.cancelBtn = new JInvButton(FORE.getString("CANCEL"));
            this.okBtn = new JInvButton(FORE.getString("OK"));
            hideOkButton();

            hBox.getChildren().addAll(anchorPane, this.okBtn, this.cancelBtn);

            getChildren().addAll(text, hBox);
        }
        
        public void changeCancelToOkBtn(){
            hideCancelButton();
            showOkButton();
        }

        public void showCancelButton() {
            cancelBtn.setVisible(true);
            cancelBtn.setManaged(true);
        }

        public void hideCancelButton() {
            cancelBtn.setVisible(false);
            cancelBtn.setManaged(false);
        }
        
        public void showOkButton() {
            okBtn.setVisible(true);
            okBtn.setManaged(true);
        }

        public void hideOkButton() {
            okBtn.setVisible(false);
            okBtn.setManaged(false);
        }

        public JInvLabel getText() {
            return text;
        }

        public ProgressBar getProgressBar() {
            return progressBar;
        }

        public JInvButton getCancelBtn() {
            return cancelBtn;
        }
        
        public JInvButton getOkBtn(){
            return okBtn;
        }

        @Override
        public DoubleProperty progressProperty() {
            return progressBar.progressProperty();
        }

        @Override
        public StringProperty textProperty() {
            return text.textProperty();
        }
    }

    public final class SecondaryProgress extends GridPane implements IMultiProgressControl {

        private final JInvLabel title;

        private final JInvLabel text;

        private final JInvLabel innerProgressText;

        private final ProgressIndicator progressIndicator;

        private final ProgressBar innerProgressBar;

        private final VBox innerProgressBox;

        private final JInvButton actionButton;

        private final StackPane closeBtnPane;

        SecondaryProgress(boolean indicator) {
            this(indicator, false);
        }

        SecondaryProgress(boolean indicator, boolean showButton) {
            setHgap(5);
            setVgap(5);
            AnchorPane.setTopAnchor(this, 5.0);
            AnchorPane.setRightAnchor(this, 5.0);
            AnchorPane.setBottomAnchor(this, 5.0);
            AnchorPane.setLeftAnchor(this, 5.0);

            final ColumnConstraints columnConstraints = new ColumnConstraints();
            columnConstraints.setHgrow(Priority.ALWAYS);
            getColumnConstraints().add(columnConstraints);

            this.title = new JInvLabel();
            title.setFont(Font.font("System", FontWeight.BOLD, 12));

            this.text = new JInvLabel();
            text.setTextFill(Paint.valueOf("#565656"));
            VBox.setMargin(text, new Insets(0, 0, 0, 2));

            this.innerProgressText = new JInvLabel();
            innerProgressText.setTextFill(Paint.valueOf("#565656"));
            VBox.setMargin(innerProgressText, new Insets(0, 0, 0, 2));

            this.innerProgressBar = new ProgressBar();
            innerProgressBar.setMinHeight(12);
            innerProgressBar.setMaxHeight(12);
            AnchorPane.setTopAnchor(this.innerProgressBar, 0.0);
            AnchorPane.setRightAnchor(this.innerProgressBar, 0.0);
            AnchorPane.setBottomAnchor(this.innerProgressBar, 0.0);
            AnchorPane.setLeftAnchor(this.innerProgressBar, 0.0);

            if (indicator) {
                this.progressIndicator = new ProgressIndicator();
                final VBox progressBox = new VBox();
                progressBox.setAlignment(Pos.CENTER);
                progressBox.setSpacing(5.0);
                progressBox.getChildren().addAll(title, new Separator(), progressIndicator, text);
                getChildren().add(progressBox);
            } else {
                this.progressIndicator = new ProgressBar();
                AnchorPane.setTopAnchor(this.progressIndicator, 0.0);
                AnchorPane.setRightAnchor(this.progressIndicator, 0.0);
                AnchorPane.setBottomAnchor(this.progressIndicator, 0.0);
                AnchorPane.setLeftAnchor(this.progressIndicator, 0.0);

                final VBox progressBox = new VBox();
                progressBox.setSpacing(5);

                final AnchorPane anchorPane = new AnchorPane();
                HBox.setHgrow(anchorPane, Priority.ALWAYS);
                anchorPane.getChildren().add(this.progressIndicator);

                final HBox hBox = new HBox();
                hBox.setSpacing(5);
                hBox.getChildren().add(anchorPane);

                progressBox.getChildren().addAll(title, new Separator(), text, hBox);

                getChildren().add(progressBox);
            }

            this.actionButton = new JInvButton();
            actionButton.setMaxWidth(18);
            actionButton.setPrefWidth(18);
            actionButton.setMinWidth(18);
            actionButton.getStyleClass().addAll("transparentButton");
            final Label label = IconFactory.getLabel(FontAwesome.fa_close);
            label.setStyle(label.getStyle() + "-fx-font-size:1.26em;");
            label.getStyleClass().add("redTextFillHover");
            actionButton.setGraphic(label);
            actionButton.setFocusTraversable(false);
            closeBtnPane = new StackPane(actionButton);
            closeBtnPane.setAlignment(Pos.CENTER);
            GridPane.setRowSpan(closeBtnPane, REMAINING);
            this.addColumn(1, closeBtnPane);

            // Inner progress
            innerProgressBox = new VBox();
            innerProgressBox.setSpacing(5);
            innerProgressBox.getChildren().addAll(innerProgressText, new AnchorPane(innerProgressBar));
            add(innerProgressBox, 0, 1);

            if (!showButton) {
                hideButton();
            }

            innerProgressText.managedProperty().bind(Bindings.isNotEmpty(innerProgressText.textProperty()));
        }

        public void hideButton() {
            actionButton.setVisible(false);
            actionButton.setManaged(false);
        }

        public void showButton() {
            actionButton.setVisible(true);
            actionButton.setManaged(true);
        }

        public void hideProgress() {
            progressIndicator.setVisible(false);
            progressIndicator.setManaged(false);
        }

        public void showProgress() {
            progressIndicator.setVisible(true);
            progressIndicator.setManaged(true);
        }

        public void showInnerProgress() {
            showInnerProgress(false);
        }

        public void showInnerProgress(boolean animated) {
            if (animated) {
                FadeTransition ft = new FadeTransition(Duration.millis(700), innerProgressBox);
                ft.setFromValue(0.0);
                ft.setToValue(1);
                ft.play();
                ft.setOnFinished(event -> {
                    GridPane.setRowSpan(closeBtnPane, REMAINING);
                    innerProgressBox.setVisible(true);
                    if (!getChildren().contains(innerProgressBar)) {
                        add(innerProgressBox, 0, 1);
                    }
                });
            } else {
                GridPane.setRowSpan(closeBtnPane, REMAINING);
                innerProgressBox.setVisible(true);
                if (!getChildren().contains(innerProgressBar)) {
                    add(innerProgressBox, 0, 1);
                }
            }
        }

        public void hideInnerProgress() {
            hideInnerProgress(false);
        }

        public void hideInnerProgress(boolean animated) {
            if (animated) {
                FadeTransition ft = new FadeTransition(Duration.millis(700), innerProgressBox);
                ft.setFromValue(1);
                ft.setToValue(0);
                ft.play();
                ft.setOnFinished(event -> {
                    GridPane.setRowSpan(closeBtnPane, 1);
                    innerProgressBox.setVisible(false);
                    this.getChildren().remove(innerProgressBox);
                });
            } else {
                GridPane.setRowSpan(closeBtnPane, 1);
                innerProgressBox.setVisible(false);
                this.getChildren().remove(innerProgressBox);
            }
        }

        public DoubleProperty innerProgressProperty() {
            return innerProgressBar.progressProperty();
        }

        public StringProperty innerTextProperty() {
            return innerProgressText.textProperty();
        }

        public JInvLabel getTitle() {
            return title;
        }

        public JInvLabel getText() {
            return text;
        }

        public ProgressIndicator getProgressIndicator() {
            return progressIndicator;
        }

        /**
         * Кнопка отмены
         */
        public JInvButton getActionButton() {
            return actionButton;
        }

        public DoubleProperty progressProperty() {
            return progressIndicator.progressProperty();
        }

        public StringProperty textProperty() {
            return text.textProperty();
        }
    }
}
