package ru.inversion.fx.app.property;

import ru.inversion.fx.app.AppException;
import ru.inversion.fx.app.BaseApp;
import ru.inversion.fx.app.Tags;

import java.util.Properties;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

/**
 *
 * @author ssu @
 */
public class LOCAL_AppProperties extends AbstractAppProperties {

	private PropertiesTypeEnum propertiesType;

    public LOCAL_AppProperties(BaseApp app, PropertiesTypeEnum propertiesType) {
        super(app);
        this.propertiesType = propertiesType;
    }

    @Override
    public PropertiesTypeEnum getType() {
        return propertiesType;
    }

    @Override
    protected Object doLoadProperty(String property) {

        switch (getType()) {
            case LOCAL_USER:
            case LOCAL_APP_USER:
                return Preferences.userRoot().node( getApp().getAppID() ).get( property, null );
            case LOCAL_SYSTEM:
            case LOCAL_APP_SYSTEM:
                return Preferences.systemRoot().node( getApp().getAppID() ).get( property, null );
//                return Preferences.userRoot().get( getApp().getAppID() + "." + property, null );
//                return Preferences.systemRoot().get( getApp().getAppID() + "." + property, null );
            default:
                return null;
        }
    }

	@Override
    public void setProperty(String property, Object value) {
        switch( getType()) {
            case LOCAL_USER:
            case LOCAL_APP_USER:
            {
                final Preferences node = Preferences.userRoot().node( getApp().getAppID() );
                node.put( property, value == null ? null : value.toString() );
                try {
                    node.flush();
                } catch( BackingStoreException e) {
                    throw new RuntimeException(Tags.PRODUCT_LABEL + "Error on save property to Preferences.userRoot", e );
                }
            }
            break;
            case LOCAL_SYSTEM:
            case LOCAL_APP_SYSTEM:
                Preferences.systemRoot().put( property, value == null ? null : value.toString() );
			break;
//                Preferences.userRoot().put( getApp().getAppID() + "." + property, value == null ? null : value.toString() );
//			break;
//                Preferences.systemRoot().put( getApp().getAppID() + "." + property, value == null ? null : value.toString() );
//			break;
        }
    }

    @Override
    public void getProperties(Properties properties) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
}
