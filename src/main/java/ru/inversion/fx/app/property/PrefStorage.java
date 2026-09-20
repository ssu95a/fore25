package ru.inversion.fx.app.property;


import ru.inversion.datacall.IDataCall;
import ru.inversion.datacall.SQLCallBuilder;
import ru.inversion.fx.app.BaseApp;
import ru.inversion.fx.app.frame.menu.PropertyItemEnum;
import ru.inversion.fx.form.ViewContext;
import ru.inversion.tc.TaskContext;
import ru.inversion.utils.U;
import ru.inversion.utils.converter.TypeConverter;

import java.net.URL;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static ru.inversion.fx.app.property.PropertiesTypeEnum.*;

/** */
public class PrefStorage {

    /** */
    final static URL defXml = PrefStorage.class.getResource( "plsql/def.xml" );

    /** */
    private static class PrefData {
        /** */
        public PrefData( Map< String, Object > values ) {
            this.values = values;
        }
        /** */
        public IDataCall call_getProperty;
        public IDataCall call_setProperty;
        /** */
        public Map<String,Object> values;
    };

    /** */
    final private Map< PropertiesTypeEnum, PrefData > storageValues = new EnumMap<>(PropertiesTypeEnum.class);

    /** */
    private IDataCall call_getProperties;
    /** */
    private IDataCall call_setProperties;

    /** */
    private void initCall_Get( PropertiesTypeEnum type, PrefData prefData )
    {
        if( prefData.call_getProperty == null )
        {
            final TaskContext tc = BaseApp.APP().getCommonTaskContext();
            prefData.call_getProperty =
                SQLCallBuilder.NEW(tc)
                    .url(defXml).name( U.decode( type, DB_USER, "get.user", DB_GLOBAL, "get.global", DB_UNIVERSAL, "get.universal" ) )
                        .build();
        }
    }

    /** */
    private void initCall_Set( PropertiesTypeEnum type, PrefData prefData )
    {
        if( prefData.call_setProperty == null ) {
            final TaskContext tc = BaseApp.APP().getCommonTaskContext();
            prefData.call_setProperty =
                SQLCallBuilder.NEW(tc)
                    .url(defXml).name( U.decode( type, DB_USER, "set.user", DB_GLOBAL, "set.global", DB_UNIVERSAL, "set.universal" )) //type == DB_USER ? "get.user" : "get" )
                        .build();
        }
    }

    /** */
    private Map<String,Object> call_getProperties( PropertiesTypeEnum type, Collection<String> names )
    {
        if( call_getProperties == null ) {
            final TaskContext tc = BaseApp.APP().getCommonTaskContext();
            call_getProperties =
                SQLCallBuilder.NEW(tc)
                    .url(defXml).name( "load" )
                        .build();
        }
        // CALL JF_PKG_UTIL.GET_PROPERTIES(:DB_USER,:DB_GLOBAL,:DB_UNIVERSAL,:O_DB_USER,:O_DB_GLOBAL,:O_DB_UNIVERSAL)
        call_getProperties.set("DB_USER", type == DB_USER ? names : null )
                          .set("DB_GLOBAL", type == DB_GLOBAL ? names : null )
                          .set("DB_UNIVERSAL", type == DB_UNIVERSAL ? names : null )
            .execute();

        final List<Object> values = call_getProperties.get( U.decode( type, DB_USER, "O_DB_USER", DB_GLOBAL, "O_DB_GLOBAL", DB_UNIVERSAL, "O_DB_UNIVERSAL" ));
        return readValuesImpl( names, values );
    }

    /** */
    private void call_setProperties( PropertiesTypeEnum type, Map<String,Object> valuesMap )
    {
        if( call_setProperties == null ) {
            final TaskContext tc = BaseApp.APP().getCommonTaskContext();
            call_setProperties =
                SQLCallBuilder.NEW(tc)
                    .url(defXml).name( "save" )
                        .build();
        }

        // throw NPE if need
        final Character prefix = U.<PropertiesTypeEnum,Character>decode( type, DB_USER, 'S', DB_GLOBAL, 'G', DB_UNIVERSAL, 'U' );
        Objects.requireNonNull( prefix, "Для данного типа свойств, массовая установка не поддерживается" );

        final List<String> names = valuesMap.keySet().stream().map(s->prefix + s ).collect(Collectors.toList());

        call_setProperties
            .set("PREF_KEY", names ).set( "PREF_VAL", valuesMap.values() )
        .execute();
    }

    /** */
    public PrefStorage()
    {
        initialLoad( );

        BaseApp.APP().addAfterLoginCallback( new Consumer< ViewContext >() {
            @Override
            public void accept( ViewContext viewContext ) {
                // удаляем все значения свойств, которые editable + не требуют рестарт
                for( PropertiesTypeEnum e : U.toIterable( DB_GLOBAL, DB_UNIVERSAL ) )
                {
                    final Map< String, Object > m = storageValues.get(e).values;
                    makeNames(e,false).forEach( m::remove);
                }
            }
        }, 1000 );
    }

    /** */
    private List<String> makeNames( PropertiesTypeEnum type, boolean all )
    {
        return
            Arrays.stream( PropertyItemEnum.values() )
               .filter( pi->pi.getPropertyType() == type && ( all || ( pi.isEditable() && !pi.isRestartRequired() ) ) )
                  .map( PropertyItemEnum::getName ).collect( Collectors.toList() );
    }

    /** */
    private Map<String,Object> readValuesImpl( Collection<String> names, Collection<Object> values )
    {
        assert names.size() != values.size() : "Размер коллекции имен переменных и коллекции значений не совпадает";

        final Map<String,Object> map = new HashMap<>();
        final Iterator<Object> iterVal = values.iterator();

        for( String s : names )
             map.put( s, iterVal.next() );

        return map;
    }

    /** */
    private void readValues( PropertiesTypeEnum type, List<String> names, List<Object> values )
    {
        storageValues.putIfAbsent( type, new PrefData(readValuesImpl(names,values)));
    }

    /** */
    private void initialLoad( )
    {
        try {

            final List<String> dbUserNames      = makeNames( DB_USER,   true );
            final List<String> dbGlobalNames    = makeNames( DB_GLOBAL, true );
                               dbGlobalNames.addAll( Arrays.asList( SyslogProperties.PORT, SyslogProperties.FACILITY, SyslogProperties.SUFFIX_PATTERN, SyslogProperties.SYSLOG_HOST, SyslogProperties.USE_SYSLOG, SyslogProperties.USE_STDLOG ) );
            final List<String> dbUniversalNames = makeNames( DB_UNIVERSAL, true );

            final List<Object> dbUserValues;
            final List<Object> dbGlobalValues;
            final List<Object> dbUniversalValues;
            final List<Object> smrValues;
            final List<Object> prpValues;

            final IDataCall
                call_getPropertiesAll =
                    SQLCallBuilder.NEW(BaseApp.APP().getCommonTaskContext())
                        .url(defXml).name( "loadAll" )
                    .build()
                        .set("DB_USER",      dbUserNames )
                        .set("DB_GLOBAL",    dbGlobalNames )
                        .set("DB_UNIVERSAL", dbUniversalNames )
                    .execute();

            prpValues         = call_getPropertiesAll.get("O_PRP");
            smrValues         = call_getPropertiesAll.get("O_SMR");

            dbUserValues      = call_getPropertiesAll.get("O_DB_USER");
            dbGlobalValues    = call_getPropertiesAll.get("O_DB_GLOBAL");
            dbUniversalValues = call_getPropertiesAll.get("O_DB_UNIVERSAL");

            readValues( SMR, Arrays.asList("CSMRBIC", "CSMRCUR", "CSMRNAME", "ccusKsiva"), smrValues );
            readValues( PRP, Arrays.asList( "ru.inversion.app.allow_change_filial", "ru.inversion.app.user_full_name", "ru.inversion.form.table.allow_change_filter"), prpValues );
            readValues( DB_USER,      dbUserNames,      dbUserValues      );
            readValues( DB_GLOBAL,    dbGlobalNames,    dbGlobalValues    );
            readValues( DB_UNIVERSAL, dbUniversalNames, dbUniversalValues );

        }
        catch( Throwable th ) {
            throw new PropertyException( th );
        }
    }

    /** */
    private Map<String,Object> loadSmr( )
    {
        final IDataCall
            call_getSmr =
                SQLCallBuilder.NEW( BaseApp.APP().getCommonTaskContext() )
                    .url(defXml).name( "loadSmr" )
                .build()
                    .execute();

        final List<Object> smrValues = call_getSmr.get("O_SMR");

        return readValuesImpl( Arrays.asList("CSMRBIC", "CSMRCUR", "CSMRNAME", "ccusKsiva"), smrValues );
    }

    /** Забираем мапу после загрузки */
    Map<String,Object> takeMap( PropertiesTypeEnum type )
    {
        PrefData data = storageValues.remove(type);

        if( data == null && type == SMR )
            data = new PrefData( loadSmr() );

        return data == null ? null : data.values;
    }

    /** */
    public void set( PropertiesTypeEnum type, String property, Object value )
    {
        final PrefData data = storageValues.get(type);

        if( data != null )
        {
            if( data.values.containsKey(property) )
                data.values.put( property, value );

            initCall_Set( type, data );

            data.call_setProperty.set("PREF_KEY",property).set("PREF_VAL",TypeConverter.convert(value,String.class)).execute();
        }
    }

    /** */
    public <T> T get( PropertiesTypeEnum type, String property )
    {
        final PrefData data = storageValues.get(type);

        Object value = null;

        if( data != null )
        {
            value = data.values.get(property);

            if( value == null && !data.values.containsKey(property) ) {

                initCall_Get( type, data );
                value = data.call_getProperty.set(0,property).execute().getReturnValue();

                if( value != null && type == DB_USER )
                    data.values.put(property,value);
            }
        }

        return (T)value;
    }

    /** */
    public Map<String,Object> gets( PropertiesTypeEnum type, Collection<String> names )
    {
        if( names == null || names.isEmpty() )
            return new HashMap<>();

        final PrefData data = storageValues.get(type);

        Map<String,Object> retMap = new HashMap<>();

        if( data != null )
        {
            for( String n : names ) {
                 if( data.values.containsKey(n) )
                     retMap.put( n,data.values.get(n) );
            }

            if( retMap.size() < names.size() )
            {
                if(!retMap.isEmpty() ) {
                    final Set<String> k = data.values.keySet();
                    names = names.stream().filter(s->!k.contains(s)).collect(Collectors.toList());
                }

                if(!retMap.isEmpty())
                    retMap.putAll( call_getProperties(type, names ) );
                else
                    retMap = call_getProperties(type, names );
            }
        }
        return retMap;
    }

    /** */
    public void sets( PropertiesTypeEnum type, Map<String,Object> valuesMap )
    {
        final PrefData data = storageValues.get(type);

        if( data != null )
        {
            valuesMap.forEach( data.values::replace );
            call_setProperties( type, valuesMap );
        }
    }
}
