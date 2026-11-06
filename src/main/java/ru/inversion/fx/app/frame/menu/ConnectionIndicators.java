package ru.inversion.fx.app.frame.menu;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.paint.Color;
import javafx.util.Pair;
//import ru.inversion.priv.tools.dcont.DCont;
//import ru.inversion.priv.tools.mdom.MDom;
//import ru.inversion.priv.tools.dcont.DCont;
import ru.inversion.tc.TaskContext;
import ru.inversion.utils.ConnectionStringFormatEnum;
import ru.inversion.utils.S;
import ru.inversion.utils.Triplet;
import ru.inversion.utils.dco.Dco;
import ru.inversion.utils.dco.IDco;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/** */
public class ConnectionIndicators {

    final static private String COLOR_PROPERTY = "ru.inversion.connection.indicator.color";

    private List<Triplet<String,Color,Pattern> > items = new ArrayList<>();
    /** */
    private ConnectionIndicators() {}

    /** */
    public Optional<Color> getIndicatorColor( TaskContext tc ) {

        if( tc == null )
            return Optional.empty();

        Object o = tc.getProperty( COLOR_PROPERTY );

        if( o != null )
            return Optional.of( (Color)o );

        final String conStr= tc.getConnectionString( ConnectionStringFormatEnum.SQL_SIMPLE );

        Optional< Triplet< String, Color, Pattern > > item
                = items.stream().filter(( t ) -> t.third.matcher(conStr).matches()).findFirst();

        if( !item.isPresent() ) {
            final String alias= tc.getConnectionString( ConnectionStringFormatEnum.SQL_DB_ALIS );
            item = items.stream().filter(( t ) -> t.third.matcher(alias).matches()).findFirst();
        }

        return item.flatMap( (t)->{tc.setProperty( COLOR_PROPERTY, t.second ); return Optional.of(t.second);} );
    }

    /** */
    public ObservableList<Pair<String,Color> > toObservableList( ) {

        ObservableList< Pair< String, Color > > list = FXCollections.observableArrayList();

        items.forEach( (e)->list.add( new Pair<>( e.first, e.second) ) );

        return list;
    }

    /** */
    public static ConnectionIndicators fromList( List<Pair<String,Color> > list ) {

        ConnectionIndicators indicators = new ConnectionIndicators();

        for( Pair<String,Color> p : list ) {

            try {

                String match = p.getKey();
                Color color = p.getValue();
                Pattern pattern = Pattern.compile(match, Pattern.CASE_INSENSITIVE);

                Triplet< String, Color, Pattern > t = new Triplet<>(match, color, pattern);

                indicators.items.add(t);
            }
            catch( Throwable th ) {
                System.err.println( th.getLocalizedMessage() );
            }
        }

        return indicators;
    }

    /** */
    private void toDC( Triplet<String,Color,Pattern> p, IDco dc ) {
        IDco item = dc.append("item");
        item.a("match").set( p.first );
        item.a("color").set( p.second.toString() );
    }

    /** */
    public String toXMLString() {

        if( items.isEmpty() )
            return S.EMPTY_STRING;

//        final DCont dc = new MDom().e("items");
//        items.forEach( (p)->toDC(p,dc) );
//        return dc.getXML();
        final IDco dco = new Dco("items");
        items.forEach( (p)->toDC(p,dco) );
        return dco.asXml();
    }

    /** */
    public static ConnectionIndicators fromXMLString( String xmlString ) {

        ConnectionIndicators indicators = new ConnectionIndicators();

        if( !S.isNullOrEmpty( xmlString ) ) {

//            DCont dc = new MDom();
//            dc.loadXml( xmlString );

            final IDco dco = Dco.parseXml(xmlString);

            for( IDco dc1 : dco.select("/items/item") ) {

                try {

                    String  match    = dc1.a("match").value();
                    Color   color    = Color.web( dc1.a("color").value() );
                    Pattern pattern  = Pattern.compile( match, Pattern.CASE_INSENSITIVE );

                    Triplet<String,Color,Pattern> t = new Triplet<>( match, color, pattern );

                    indicators.items.add( t );
                }
                catch( Throwable th ) {
                    System.err.println( th.getLocalizedMessage() );
                }
            }
        }
        return indicators;
    }

    /** */
    public String toString() {
        return items.stream().map( (t)->t.first ).collect( Collectors.joining(";") );
    }
}
