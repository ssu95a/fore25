package ru.inversion.fx.app.property;

import ru.inversion.fx.app.BaseApp;
import ru.inversion.utils.S;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.util.Map;
import java.util.Properties;
import java.util.ResourceBundle;


import static ru.inversion.fx.app.property.PREF_AppProperties.STORAGE;
import static ru.inversion.fx.app.property.PropertiesTypeEnum.SMR;

/**
 *
 * @author ssu @
 */
public class SMR_AppProperties extends AbstractAppProperties {

    /** */
    private static final ResourceBundle fore = ResourceBundle.getBundle("fore");

    /** */
    private Map<String,Object> smrProperties;

    /** */
	public SMR_AppProperties( BaseApp app ) {
		super( app );
	}

	/** */
	@Override
	protected Object doLoadProperty( String property ) {

	    if ( S.in( property.toUpperCase(), "OGRN", "ОГРН" ) ){
	        property = "ccusKsiva";
        }
        //Если находим в кэше, выдаём оттуда
        String cachedProperty = (String)smrProperties.get( property );
        if ( S.isNotNullOrEmpty(cachedProperty) ){
            //g_logger.info("cached property: {} = {} ", property, nvl(cachedProperty, "null"));
            return cachedProperty;
        }
        //Если нет, делаем перезапрос
        reInit();
        String refreshedProperty = (String)smrProperties.get( property );
        //g_logger.info("refreshed property: {} = {} ", property, nvl(refreshedProperty, "null"));
        return refreshedProperty;
	}
	/** */
	@Override
	public void getProperties( Properties properties ) {
        properties.putAll( smrProperties );
	}

	@Override
	public PropertiesTypeEnum getType() {
		return SMR;
	}

    /** */
    public void init() {
        reInit();
    }

    /** */
    private void composeTitles()
    {
        String CSMRNAME = (String)smrProperties.get("CSMRNAME");
        String CSMRCUR  = (String)smrProperties.get("CSMRCUR");

        if( S.isNotNullOrEmpty(CSMRNAME) ) {
            smrProperties.put( "ru.inversion.app.org_name", CSMRNAME  );
            smrProperties.put( "ru.inversion.app.csmrcur", CSMRCUR );
        }

        IAppProperties p = getApp().getProperties(PropertiesTypeEnum.PRP);

        try {
            Connection c = BaseApp.APP().getCommonTaskContext().getConnection();
            //Собираем заголовки
            final String appID = getApp().getAppID();
            final DatabaseMetaData metaData = c.getMetaData();
            final String userName = metaData.getUserName();
            final String url = metaData.getURL().indexOf('@') == -1 ? metaData.getURL() : metaData.getURL().split( "@" )[1];

            smrProperties.put("ru.inversion.app.title",
                String.format("%s | v. %s | %s-%s@%s | %s",
                p.getStringProperty("ru.inversion.app.name", appID),
                p.getStringProperty("ru.inversion.app.version", "<unknown>"),
                p.getStringProperty("ru.inversion.app.user_full_name", " "),
                userName,
                url,
                getStringProperty("ru.inversion.app.org_name", " "))
            );
            smrProperties.put("ru.inversion.app.title_no_ver",
                String.format("%s | %s-%s@%s | %s",
                p.getStringProperty("ru.inversion.app.name", appID),
                p.getStringProperty("ru.inversion.app.user_full_name", " "),
                userName,
                url,
                getStringProperty("ru.inversion.app.org_name", " ")));
            smrProperties.put("ru.inversion.app.info",
                    String.format("%s-%s@%s | %s",
                            p.getStringProperty("ru.inversion.app.user_full_name", " "),
                            userName,
                            url,
                            getStringProperty("ru.inversion.app.org_name", " ")));
        } catch ( Throwable th ){
            throw new PropertyException( fore.getString("OSHIBKA_PRI_POLUCHENII_SMR_SVOJSTV"), th );
        }
    }

    /**
     Перерисовка заголовка в связи со сменой филиала
     */
    public void reInit() {

        //smrProperties.clear();
        //smrProperties.putAll( PREF_AppProperties.getSmrProperties( true ) );

        smrProperties = STORAGE.takeMap(SMR);
        composeTitles( );
    }
}
