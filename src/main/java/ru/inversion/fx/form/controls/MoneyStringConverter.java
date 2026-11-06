package ru.inversion.fx.form.controls;

import javafx.util.converter.BigDecimalStringConverter;
import ru.inversion.utils.Pair;
import ru.inversion.utils.S;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static ru.inversion.fx.form.controls.JInvMoneyField.g_moneyFormat;

/**
 *
 * @author ssu
 */
public class MoneyStringConverter extends BigDecimalStringConverter {

    static final int MAX_CACHE_SIZE = 1000 / 4;
    
    final static private Map<String,BigDecimal> g_valueCacheMap = new LinkedHashMap<String,BigDecimal>( MAX_CACHE_SIZE, 0.75f, true ) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, BigDecimal> eldest) {
            return size() > MAX_CACHE_SIZE;
        }
    };

    /** */
    public static String convertToString( BigDecimal value )
    {
        return value == null ? null : g_moneyFormat.format(value);
    }

    /** */
    @Override
    public String toString( BigDecimal value ) {
        return value == null ? null : g_moneyFormat.format(value);
        
    }
    
    /** */
    @Override
    public BigDecimal fromString( String value ) {

        value = value.trim();
        
        if( S.isNullOrEmpty(value) )
            return null;

        BigDecimal retValue = g_valueCacheMap.get(value);
        if( retValue != null )
            return retValue;

        List<Pair<StringBuilder,Character>> items = new ArrayList<>();

        retValue = BigDecimal.ZERO;
        
        char    ch;
        boolean afterSeparator = false;
        boolean sign           = true;

        int i = 0;

        int nFration = g_moneyFormat.getMaximumFractionDigits();
        
        for( ; i < value.length(); i++ ) {

            ch = value.charAt(i);

            if( Character.isSpaceChar(ch) || ch == (char)0xA0 )
                continue;
            
            if( ch == '-' || ch == '+' ) {
                if( ch == '-' ) {
                    sign = false;
                }
                i++;
            }
            break;
        }

        Pair<StringBuilder, Character> currentItem = Pair.makePair( new StringBuilder(), '_' );
        
        for( ; i < value.length(); i++ ) {

            ch = value.charAt(i);

            if( Character.isSpaceChar(ch) || ch == (char)0xA0 )
                continue;
            
            switch (ch) {
                
                case 'М': case 'м': case 'M': case 'm': 
                {
                    if( currentItem.first.length() > 0) {

                        currentItem.second = 'M';

                        items.add( currentItem );

                        currentItem    = Pair.makePair( new StringBuilder(), '_' );
                        afterSeparator = false;
                    }
                }
                break;
                case 'K': case 'k': case 'К': case 'к':
                case 'Т': case 'т': case 'T': case 't': 
                {
                    if( currentItem.first.length() > 0) {

                        currentItem.second = 'T';

                        items.add( currentItem );

                        currentItem    = Pair.makePair( new StringBuilder(), '_' );
                        afterSeparator = false;
                    }
                }
                break;
                case '.':
                case ',': {

                    if( afterSeparator )
                        throw new NumberFormatException("Error conversion string '" + value + "' to BigDecimal at position " + i);

                    currentItem.first.append('.');
                    afterSeparator = true;
                }
                break;
                default: {
                    
                    if( !Character.isDigit(ch) )
                        throw new NumberFormatException("Error conversion string '" + value + "' to BigDecimal at position " + i);

                    currentItem.first.append(ch);
                }
            }//end switch
            
        }//end for
        
        if( currentItem.first.length() > 0 )
            items.add( currentItem );
        
        for( Pair<StringBuilder,Character> p : items ) {
            
            BigDecimal d = new BigDecimal( p.first.toString() );
            
            switch( p.second ) {
                case 'M':
                    d = d.multiply( BigDecimal.valueOf( 1000000L ) );
                break;
                case 'T':
                    d = d.multiply( BigDecimal.valueOf( 1000L ) );
                break;
            }
            retValue = retValue.add(d);
        }

        retValue.round( new MathContext(nFration) );
        
        if( !sign )
            retValue = retValue.negate();
        
        g_valueCacheMap.put( value, retValue );
        
        return retValue;
    }
}
