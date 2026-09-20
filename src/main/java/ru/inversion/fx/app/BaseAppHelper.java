package ru.inversion.fx.app;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.inversion.db.DBUniqueResult;
import ru.inversion.fx.app.property.PRP_AppProperties;
import ru.inversion.fx.service.module.ModuleService;
import ru.inversion.tc.TaskContext;
import ru.inversion.utils.*;

import javax.persistence.NamedNativeQuery;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.events.Characters;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import java.io.*;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static ru.inversion.fx.app.property.PropertiesTypeEnum.PRP;

/**
 *
 * @author ssu
 */
public class BaseAppHelper {

    final static Logger appLogger = LoggerFactory.getLogger( "ru.inversion." + BaseApp.APP().getAppID() );
    private static final ResourceBundle fore = ResourceBundle.getBundle("fore");

    //
    static private String getCharactersData ( XMLEventReader er, Characters chrs ) throws Exception
    {
        StringBuilder sb = new StringBuilder ( chrs.getData() );

        XMLEvent ev;
        while ((ev = er.peek ()) != null)
        {
            if (! ev.isCharacters ())
                break;

            sb.append (ev.asCharacters ().getData ());
            er.nextEvent ();
        }

        return sb.toString ();
    }

    //
    static private Map<String, Object > parseParametersReader( Reader fr ) {

        Map<String, Object> retMap = new HashMap<>();
        
        try {

            String  currentName = null;
            boolean doGetValue = false;

            XMLEventReader xmlReader = null;
                    
            try {

                xmlReader = XMLInputFactory.newInstance( ).createXMLEventReader( fr );
            
                // выход на начало документа
                while( xmlReader.hasNext() ) {
                    XMLEvent event = xmlReader.nextEvent();
                    if( event.isStartElement() ) {
                        if( event.asStartElement().getName().getLocalPart().equals("Paramlist") )
                            break;
                    }
                }

                while( xmlReader.hasNext() ) {

                    XMLEvent event = xmlReader.nextEvent();

                    //выход из разбора, по достижению конца документа
                    if( event.isEndElement() ) {

                        String name = event.asEndElement().getName().getLocalPart();

                        if( name.equals("Paramlist") )
                            break;
                    }

                    if( event.isStartElement() ) 
                    {

                        StartElement se = event.asStartElement();

                        String name = se.getName().getLocalPart();

                        if( name.equals("Parameter") ) {
                            currentName = se.getAttributeByName( new QName("Name") ).getValue();
                            if( !S.isNullOrEmpty(currentName) )
                                doGetValue = true;
                        }
                    }

                    if( event.isCharacters() && doGetValue ) {
//                        retMap.put( currentName, event.asCharacters().getData() );
                        retMap.put( currentName, getCharactersData( xmlReader, event.asCharacters() ) );
                        currentName= null;
                        doGetValue = false;
                    }
                }//end while
            }
            finally {
                if( xmlReader != null )
                    xmlReader.close();
            }
            return retMap;
        }
        catch( Throwable th ) {
            throw new RuntimeException( Tags.PRODUCT_LABEL + "Error on read XML parameters", th );
        }
    }

    //
    static public Map<String, Object > parseParametersString( String xmlData ) {

        if( S.isNullOrEmpty(xmlData) )
            return new HashMap<>();
        try {
            return parseParametersReader( new StringReader(xmlData) );
        }
        catch( Throwable th ) {
            throw new RuntimeException( Tags.PRODUCT_LABEL + "Error on read string XML parameters", th );
        }
    }

    //
    static public Map<String, Object > parseParametersFile( String fileName ) {

        if( S.isNullOrEmpty(fileName) )
            return new HashMap<>();

        try ( Reader fr = new InputStreamReader(new FileInputStream(fileName), "UTF-8") )
        {
            return parseParametersReader(fr);
        }
        catch( Throwable th ) {
            appLogger.error( Tags.PRODUCT_LABEL + java.text.MessageFormat.format(java.util.ResourceBundle.getBundle("fore").getString("OSHIBKA_PRI_SCHITYVANII_PEREDANNYH_PARAMETROV_IZ_FAJLA"), new Object[] {fileName}), th );
        }
        return null;

/*
        Map<String, Object> retMap = new HashMap<>();

        try {

            String  currentName = null;
            boolean doGetValue = false;

            XMLEventReader xmlReader = null;

            try ( Reader fr = new InputStreamReader(new FileInputStream(fileName), "UTF-8") )
            {

                xmlReader = XMLInputFactory.newInstance( ).createXMLEventReader( fr );

                // выход на начало документа
                while( xmlReader.hasNext() ) {
                    XMLEvent event = xmlReader.nextEvent();
                    if( event.isStartElement() ) {
                        if( event.asStartElement().getName().getLocalPart().equals("Paramlist") )
                            break;
                    }
                }

                while( xmlReader.hasNext() ) {

                    XMLEvent event = xmlReader.nextEvent();

                    //выход из разбора, по достижению конца документа
                    if( event.isEndElement() ) {

                        String name = event.asEndElement().getName().getLocalPart();

                        if( name.equals("Paramlist") )
                            break;
                    }


                    if( event.isStartElement() )
                    {

                        StartElement se = event.asStartElement();

                        String name = se.getName().getLocalPart();

                        if( name.equals("Parameter") ) {
                            currentName = se.getAttributeByName( new QName("Name") ).getValue();
                            if( !S.isNullOrEmpty(currentName) )
                                doGetValue = true;
                        }
                    }

                    if( event.isCharacters() && doGetValue ) {
//                        retMap.put( currentName, event.asCharacters().getData() );
                        retMap.put( currentName, getCharactersData( xmlReader, event.asCharacters() ) );
                        currentName= null;
                        doGetValue = false;
                    }
                }//end while

            }
            finally {
                if( xmlReader != null )
                    xmlReader.close();
            }

            try {
                Files.deleteIfExists( (new File(fileName)).toPath()  );
            }
            catch( Throwable th ) {
                appLogger.error( Tags.PRODUCT_LABEL+"On delete XML parameters file: " + fileName, th );
            }

            return retMap;
        }
        catch( Throwable th ) {
            appLogger.error( Tags.PRODUCT_LABEL + java.text.MessageFormat.format(java.util.ResourceBundle.getBundle("fore").getString("OSHIBKA_PRI_SCHITYVANII_PEREDANNYH_PARAMETROV_IZ_FAJLA"), new Object[] {fileName}), th );
        }
        return null;
 */
    }

    /** */
    static public Map<String, Object > prepareParameters( BaseApp app ) {

        PRP_AppProperties appProperties = (PRP_AppProperties)app.getProperties(PRP);

        final Map< String, Object > parameters = parseParametersFile( appProperties.getStringProperty("ru.inversion.start_file_params") );

        appProperties.acceptForEach(new TriConsumer< Integer, String, Object >() {
            @Override
            public void accept( Integer integer, String s, Object o ) {
                if( s.startsWith("ru.inversion.parameter.") )
                    parameters.put( s.substring( "ru.inversion.parameter.".length() ), o );
            }
        });

        return parameters;
    }

    @NamedNativeQuery(name ="FrmVersion", query = "select cVersion from frm_version where cForm=?" ) 
    private static class FrmVersion extends DBUniqueResult<String> {
    }

    /** */
    static public void checkModuleVersion( TaskContext tc, String moduleVersion, String jarName ) throws AppException {
        
        if( U.containsNull( moduleVersion, jarName ) )
            return;
        
        FrmVersion fv         = new FrmVersion( );
        String     frmVersion = fv.execute( tc, false, jarName );
        if( frmVersion != null ) {
            if( !frmVersion.equalsIgnoreCase(moduleVersion) ) {
                throw new AppException( Tags.PRODUCT_LABEL + MessageFormat.format( fore.getString("INCORRECT_VERSION_FORM"), jarName, moduleVersion, frmVersion ));
            }//end if
        }
    }



    /** */
    public static Consumer<Pair<Object,String>> runFxCallMonitor( )
    {
        final LinkedBlockingQueue<Pair<Object,String>> q = new LinkedBlockingQueue<>();

        final Runnable run = new Runnable() {

            final private Pattern getJarPattern_Url = Pattern.compile( "ru/inversion/(?<jar>\\w+)", Pattern.DOTALL | Pattern.CASE_INSENSITIVE );
            final private Pattern getJarPattern_Cls = Pattern.compile( "ru\\.inversion\\.(?<jar>\\w+)", Pattern.DOTALL | Pattern.CASE_INSENSITIVE );
            final private Pattern getFilePattern    = Pattern.compile( "(?<file>ru/inversion/[\\w/.]+$)", Pattern.DOTALL | Pattern.CASE_INSENSITIVE );

            @Override
            public void run() {

                try {

                    Pair<Object,String> t;
                    int i;

                    while( (t = q.take()) != Pair.NullPair )
                    {
                        //System.out.println(t);
                        try {

                            if( t.getFirst() == null )
                                continue;

                            final Object source = t.getFirst();
                            String sourceName;
                            final String jarName;

                            if( source instanceof URL ) {

                                sourceName = ((URL)source).toExternalForm();

                                final Matcher matcher = getJarPattern_Url.matcher(sourceName);
                                if( matcher.find() )
                                    jarName = matcher.group("jar");
                                else
                                    jarName = null;

                                final Matcher matcher1 = getFilePattern.matcher(sourceName);
                                if( matcher1.find() )
                                    sourceName = matcher1.group("file");
                            }
                            else if( source instanceof Class<?> )
                            {
                                sourceName = ((Class<?>)source).getName();

                                File file = ModuleService.getJarFile( (Class<?>)source );
                                if( file != null )
                                    jarName = file.getName();
                                else
                                {
                                    final Matcher matcher = getJarPattern_Cls.matcher(sourceName);
                                    if( matcher.find() )
                                        jarName = matcher.group("jar");
                                    else
                                        jarName = null;
                                }
                            }
                            else {
                                sourceName = t.getFirst().toString();
                                jarName    = null;
                            }

                            if( Thread.currentThread().isInterrupted() ) {
                                appLogger.info( Tags.PRODUCT_LABEL + "runFxCallMonitor: exit from monitor - thread interrupted!" );
                                return;
                            }

                            final Connection c;

                            try {

                                c = BaseApp.APP().getCommonTaskContext().getConnection();

                                if( c.isClosed() )
                                    throw new IllegalStateException("Connection is closed");

                                if( !c.getAutoCommit() ) {
                                    appLogger.error ( Tags.PRODUCT_LABEL + "runFxCallMonitor: exit from monitor - ALARM! commonTaskContext.autoCommit = false! Find him and humiliate him!" );
                                    return;
                                }
                            }
                            catch( Throwable th ) {
                                appLogger.info( Tags.PRODUCT_LABEL + "runFxCallMonitor: exit from monitor - DB common connection is closed!" );
                                return;
                            }

                            try( PreparedStatement ps = c.prepareStatement("insert into XXI_FX_PLSQL_MON (JAR_NAME, SRC_NAME, BLK_NAME) select ?,?,? from dual where not exists ( select null from XXI_FX_PLSQL_MON where SRC_NAME = ? and BLK_NAME = ? and run_date = trunc(LOCALTIMESTAMP) )\n--lti") ) {
                                 ps.setString( 1, jarName );
                                 ps.setString( 2, sourceName );
                                 ps.setString( 3, t.getSecond() );
                                 ps.setString( 4, sourceName );
                                 ps.setString( 5, t.getSecond() );

                                 ps.execute();

                                // no commit !!! autoCommit mode ON
                            }
                            catch( Throwable th ) {
                                appLogger.info( Tags.PRODUCT_LABEL + "runFxCallMonitor: exit from monitor - execute DB statement error!: " + th.getLocalizedMessage() );
                                return;
                            }
                        }
                        catch( Throwable th ) {
                            appLogger.error( Tags.PRODUCT_LABEL + "runFxCallMonitor: exit from monitor - error handle Pair", th );
                            return;
                        }
                    }//end while
                    appLogger.debug( Tags.PRODUCT_LABEL + "runFxCallMonitor: exit from monitor - exit on take NullPair" );

                } catch( InterruptedException e ) {

                    appLogger.debug( Tags.PRODUCT_LABEL + "runFxCallMonitor: exit from monitor - exit on InterruptedException" );

                    Thread.currentThread().interrupt();
                }
            }
        };

        ThreadPoolManager.getInstance().executeTask(run);

        return new Consumer<Pair<Object,String>>() {
            @Override
            public void accept( Pair<Object,String> t ) {
                try {
                    q.put(t);
                } catch( InterruptedException e ) {
                    appLogger.error( Tags.PRODUCT_LABEL + "Interrupt error on call Consumer 4 runFxCallMonitor" );
                }
            }
        };
    }
}
