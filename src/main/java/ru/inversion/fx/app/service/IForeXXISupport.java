package ru.inversion.fx.app.service;

public interface IForeXXISupport extends IAppService {
    public static final String SERVICE_ID = "FORE_XXI_SUPPORT";

    <T> T getFeature( Object featureId, Object param );
}
