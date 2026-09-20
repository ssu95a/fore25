package ru.inversion.fx.app.property;

import ru.inversion.utils.Pair;
import ru.inversion.fx.app.BaseApp;
import ru.inversion.fx.app.IDBTools;
import ru.inversion.fx.app.frame.menu.IMenuLoader;
import ru.inversion.tc.TaskContext;
import ru.inversion.utils.U;
import ru.inversion.xxi.impl.XXIMenuLoader;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

/** */
public class PREF_AppProperties extends AbstractAppProperties {

    /** */
    static final public PrefStorage STORAGE = new PrefStorage();

    /** */
    final private PropertiesTypeEnum type;

    /** */
    public PREF_AppProperties( BaseApp app, PropertiesTypeEnum type ) {
        super(app);
        this.type = type;
    }

    /** */
    @Override
    public PropertiesTypeEnum getType() {
        return type;
    }

    /** */
    @Override
    protected <P> P doLoadProperty( String property ) {
        return STORAGE.get(getType(),property);
    }

    /** */
    @Override
    public void getProperties( Properties properties ) {
        getProperties((Map)properties);
    }

    /** */
    public Map<String,Object> getProperties( String ... names ) {
        return getProperties(Arrays.asList(names));
    }

    /** */
    public Map<String,Object> getProperties( List<String> names ) {
        return STORAGE.gets( getType(), names );
    }

    /** */
    public void getProperties( Map<String,Object> properties )
    {
        STORAGE.gets( getType(), properties.keySet() );
    }

    /** */
    @Override
    public void setProperty( String property, Object value ) {
        STORAGE.set( getType(), property, value );
    }

    /** */
    public void setProperties( Map<String,Object> valuesMap ) {
        STORAGE.sets( getType(), valuesMap );
    }

    public static IDBTools getDBTools() {
        return XXITools.g_instance;
    }

    /** */
    private static class XXITools implements IDBTools {
        private static XXITools g_instance = new XXITools();
        @Override
        public IMenuLoader getMenuLoader( TaskContext tc, String menuID ) {
            return new XXIMenuLoader( tc, menuID );
        }

        @Override
        public void getAppProperties( TaskContext tc, Properties properties ) {
        }

        @Override
        public String getPreference( TaskContext tc, PropertiesTypeEnum type, String preference, String defValue ) {
            return U.nvl( STORAGE.get(type,preference), defValue ) ;
        }

        @Override
        public void setPreference( TaskContext tc, PropertiesTypeEnum type, String property, String value ) {
            STORAGE.set(type,property,value );
        }

        @Override
        public void setPreference( TaskContext tc, Map< PropertiesTypeEnum, List<Pair<String,String> > > map ) {
            map.forEach(new BiConsumer< PropertiesTypeEnum, List< Pair< String, String > > >() {
                @Override
                public void accept( PropertiesTypeEnum t, List<Pair<String,String> > prefs ) {
                    STORAGE.sets(
                        t,
                        prefs.stream().collect (
                            Collectors.toMap (
                            p->p.first, p->p.second
                            )
                        )
                    );
                }
            });
        }

        @Override
        public Map< PropertiesTypeEnum, Properties > initPreferences( TaskContext tc, Map< PropertiesTypeEnum, List< String > > prefNames ) {
            //return Collections.emptyMap();
            throw new UnsupportedOperationException("initPreferences");
        }

        @Override
        public void refreshAppPreferenceCache( TaskContext tc ) {
        }
    }
}
