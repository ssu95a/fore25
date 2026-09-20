/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ru.inversion.fx.form.controls;

import javafx.beans.property.*;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Pos;
import ru.inversion.utils.Pair;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author antonovdi
 */
public class JInvTableColumnBigDecimal<S,V> extends JInvTableColumn<S, BigDecimal > {

    // Количество знаков после запятой
    //public Integer precision = JInvMoneyField.g_moneyFormat.getMaximumFractionDigits();

    public JInvTableColumnBigDecimal() {
        alignment = Pos.CENTER_RIGHT;
    }

    private IntegerProperty precisionProperty;

    public IntegerProperty precisionProperty( ) {
        if( precisionProperty == null ) {
            precisionProperty = new IntegerPropertyBase( JInvMoneyField.g_moneyFormat.getMaximumFractionDigits() ) {
                @Override
                public Object getBean() {
                    return JInvTableColumnBigDecimal.this;
                }

                @Override
                public String getName() {
                    return "precision";
                }

                @Override
                public void setValue( Number v ) {
                    set( v == null ? 0 : v.intValue() );
                }
            };
            precisionProperty.addListener(new ChangeListener< Number >() {
                @Override
                public void changed( ObservableValue< ? extends Number > observable, Number oldValue, Number newValue ) {
                    decimalFormat = null;
                    if( getTableView() != null )
                        getTableView().refresh();
                }
            });
        }
        return precisionProperty;
    }

    /** */
    public Integer getPrecision() {
        return precisionProperty == null ? JInvMoneyField.g_moneyFormat.getMaximumFractionDigits() : precisionProperty.getValue() ;
    }
    /** */
    public void setPrecision( Integer v ) {
        precisionProperty().setValue(v);
    }

    // Разделять на группы
    //public Boolean showGroups = Boolean.FALSE;

    public BooleanProperty showGroupsProperty;

    public BooleanProperty showGroupsProperty()
    {
        if( showGroupsProperty == null ) {
            showGroupsProperty = new BooleanPropertyBase(true) {
                @Override
                public Object getBean() {
                    return JInvTableColumnBigDecimal.this;
                }

                @Override
                public String getName() {
                    return "showGroups";
                }

                @Override
                public void setValue( Boolean v ) {
                    super.set( v != null && v );
                }
            };
            showGroupsProperty.addListener(new ChangeListener< Boolean >() {
                @Override
                public void changed( ObservableValue< ? extends Boolean > observable, Boolean oldValue, Boolean newValue ) {
                    decimalFormat = null;
                    if (getTableView() != null)
                        getTableView().refresh();
                }
            });
        }

        return showGroupsProperty;
    }

    public boolean getShowGroups() {
        return showGroupsProperty == null || showGroupsProperty.get();
    }
    public void setShowGroups(boolean v) {
        showGroupsProperty().set(v);
    }

    private DecimalFormat decimalFormat;

    public DecimalFormat getDecimalFormat() {
        if( decimalFormat == null )
            createDecimalFormat();
        return decimalFormat;
    }

    static private List< Pair<String,DecimalFormat> > g_dfCache = new ArrayList<>(2);

    /** */
    private void createDecimalFormat()
    {
        String key = String.join( "-", Integer.toString( getPrecision() ), Boolean.toString( getShowGroups() ) );

        for( int i = 0; i < g_dfCache.size(); i++ )
        {
             if( g_dfCache.get(i).first.equals(key) )
             {
                 decimalFormat = g_dfCache.get(i).second;
                 return;
             }
        }

        final char decimalSeparator = '.';
        final DecimalFormatSymbols ds = new DecimalFormatSymbols();
        ds.setMonetaryDecimalSeparator(decimalSeparator);
        ds.setDecimalSeparator(decimalSeparator);
        ds.setGroupingSeparator(' ');

        final DecimalFormat df = new DecimalFormat();
        df.setDecimalFormatSymbols(ds);
        df.setDecimalSeparatorAlwaysShown( getPrecision() != 0 );
        df.setGroupingSize( 3 );
        df.setGroupingUsed( getShowGroups() );

        df.setParseBigDecimal(true);

        df.setMaximumFractionDigits( getPrecision() );
        df.setMinimumFractionDigits( getPrecision() );

        g_dfCache.add( Pair.makePair(key,df) );

        decimalFormat = df;
    }

}
