package ru.inversion.fx.app.service;

import ru.inversion.fx.app.AppException;
import ru.inversion.fx.service.module.ModuleService;

import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;

/**
 *
 * @author ssu @
 */
public class AppServiceFactory {

    private static final ResourceBundle fore = ResourceBundle.getBundle("fore");
	private static AppServiceFactory INSTANCE = new AppServiceFactory();
	private Map<String,IAppService> serviceMap = new HashMap<>();
	private AppServiceFactory() {
	}

	public static AppServiceFactory getInstance() {
		return INSTANCE;
	}

	/* */
	private IAppService createService( String serviceID ) {
		switch( serviceID ) {
			case ViewPrefAppService.SERVICE_ID:
				return new ViewPrefAppService();
            case ModuleService.SERVICE_ID:
                return new ModuleService();
			case IForeXXISupport.SERVICE_ID:
			{
				IForeXXISupport foreXXISupport = null;
				try {

					final Class<?> aClass = Class.forName("ru.inversion.bicomp.fore_support.BiCompForeXXISupport");
					foreXXISupport = (IForeXXISupport)aClass.newInstance();

				} catch(Throwable ignored) {
				}

				if( foreXXISupport == null )
					foreXXISupport = new IForeXXISupport() {
						@Override
						public < T > T getFeature( Object featureId, Object param ) {
							return null;
						}

						@Override
						public String getServiceInfo() {
							return "Service stub, no XXI impl";
						}
					};

				return foreXXISupport;
			}
		}
		return null;
	}

	/** */
	public IAppService getService( String serviceID ) throws AppException {

		IAppService s = serviceMap.get(serviceID);

		if( s == null ) {
			s = createService(serviceID);
			if( s == null )
				throw new AppException(java.text.MessageFormat.format(fore.getString("NE_IZVESTNYJ_SERVIS_S_ID"), new Object[] {serviceID }));
			serviceMap.put(serviceID, s );
		}
		return s;
	}
}
