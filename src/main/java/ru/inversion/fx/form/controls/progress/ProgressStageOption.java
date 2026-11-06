package ru.inversion.fx.form.controls.progress;

import javafx.stage.Stage;

final class ProgressStageOption {

    private final Stage parentStage;

    private final String title;

    private final boolean showSuccess;

    ProgressStageOption(Stage parentStage, String title) {
        this(parentStage, title, false);
    }

    ProgressStageOption(Stage parentStage, String title, boolean showSuccess) {
        this.parentStage = parentStage;
        this.title = title;
        this.showSuccess = showSuccess;
    }

    public Stage getParentStage() {
        return parentStage;
    }

    public String getTitle() {
        return title;
    }

    public boolean isShowSuccess() {
        return showSuccess;
    }
}
