package ru.inversion.fx.form.controls.progress;

public class MultiProgressConfig {

    private final ProgressStageOption stageOption;

    private final boolean stopAllIfFailed;

    private final boolean closeOnCancel;
    
    private final boolean closeIfAllSuccess;

    private final boolean hideInnerProgressOnFinish;

    private final ProgressHandler<GeneralProgressCounter> finishGeneralProgressHandler;

    private MultiProgressConfig(Builder builder) {
        this.stageOption = builder.getStageOption();
        this.stopAllIfFailed = builder.isStopAllIfFailed();
        this.hideInnerProgressOnFinish = builder.isHideInnerProgressOnFinish();
        this.finishGeneralProgressHandler = builder.getFinishGeneralProgressHandler();
        this.closeOnCancel = builder.isCloseOnCancel();
        this.closeIfAllSuccess = builder.isCloseIfAllSuccess();
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public ProgressStageOption getStageOption() {
        return stageOption;
    }

    public boolean isStopAllIfFailed() {
        return stopAllIfFailed;
    }

    public boolean isHideInnerProgressOnFinish() {
        return hideInnerProgressOnFinish;
    }

    public ProgressHandler<GeneralProgressCounter> getFinishGeneralProgressHandler() {
        return finishGeneralProgressHandler;
    }

    public boolean isCloseOnCancel() {
        return closeOnCancel;
    }
    
    public boolean isCloseIfAllSuccess() {
        return closeIfAllSuccess;
    }

    public static final class Builder {

        private ProgressStageOption stageOption;

        private boolean stopAllIfFailed;

        private boolean closeOnCancel;
        
        private boolean closeIfAllSuccess;

        private boolean hideInnerProgressOnFinish;

        private ProgressHandler<GeneralProgressCounter> finishGeneralProgressHandler;

        private Builder() {
        }

        public ProgressStageOption getStageOption() {
            return stageOption;
        }

        public Builder setStageOption(ProgressStageOption stageOption) {
            this.stageOption = stageOption;
            return this;
        }

        public boolean isStopAllIfFailed() {
            return stopAllIfFailed;
        }

        public Builder setStopAllIfFailed(boolean stopAllIfFailed) {
            this.stopAllIfFailed = stopAllIfFailed;
            return this;
        }

        public boolean isCloseOnCancel() {
            return closeOnCancel;
        }

        public Builder setCloseOnCancel(boolean yep) {
            this.closeOnCancel = yep;
            return this;
        }
        
        public boolean isCloseIfAllSuccess() {
            return closeIfAllSuccess;
        }

        public Builder setCloseIfAllSuccess(boolean yep) {
            this.closeIfAllSuccess = yep;
            return this;
        }

        public boolean isHideInnerProgressOnFinish() {
            return hideInnerProgressOnFinish;
        }

        public Builder setHideInnerProgressOnFinish(boolean hideInnerProgressOnFinish) {
            this.hideInnerProgressOnFinish = hideInnerProgressOnFinish;
            return this;
        }

        public ProgressHandler<GeneralProgressCounter> getFinishGeneralProgressHandler() {
            return finishGeneralProgressHandler;
        }

        public Builder setFinishGeneralProgressHandler(ProgressHandler<GeneralProgressCounter> finishGeneralProgressHandler) {
            this.finishGeneralProgressHandler = finishGeneralProgressHandler;
            return this;
        }

        public MultiProgressConfig build() {
            return new MultiProgressConfig(this);
        }
    }
}
