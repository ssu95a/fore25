package ru.inversion.fx.form.controls.renderer;

import javafx.scene.control.TableColumn;
import javafx.util.StringConverter;
import ru.inversion.fx.form.controls.JInvTableColumn;
import ru.inversion.fx.form.controls.JInvTableColumnDate;
import ru.inversion.utils.S;
import ru.inversion.utils.Triplet;
import ru.inversion.utils.U;
import ru.inversion.utils.converter.IConverter;
import ru.inversion.utils.converter.TypeConverter;

import java.util.ArrayList;
import java.util.List;

/**
 * Ячейка TableView для вывода даты
 *
 * @author antonovdi
 * @author Sulimoff
 */
public class JInvTableCellDate extends JInvTableCell
{
    final private static List<Triplet<Class,String, StringConverter >> fmtCache = new ArrayList<>();
    public JInvTableCellDate(String mask) {
        super(mask);
    }

    private StringConverter<?> getConverter( Class clazz, JInvTableColumn fxColumn )
    {
        if( S.isNullOrEmpty( this.mask ) )
        {
            if( fxColumn instanceof JInvTableColumnDate )
            {
                final JInvTableColumnDate invColumn = (JInvTableColumnDate) fxColumn;

                if( invColumn.getDateFormat() != null )
                    this.mask = invColumn.getDateFormat().getMask();
                else
                    this.mask = "-";
            }
        }

        for( Triplet< Class, String, StringConverter > t : fmtCache )
        {
            if( clazz == t.first && U.equals( mask, t.second ) )
                return t.third;
        }

        Triplet<Class,String, StringConverter > t;

        if( "-".equals( mask ) )
        {
            t = Triplet.makeTriplet(clazz, "-", new StringConverter() {
                @Override
                public String toString( Object o ) { return o.toString(); }
                @Override
                public Object fromString( String string ) { throw new UnsupportedOperationException("fromString");}
            });
        }
        else
        {
            t = Triplet.makeTriplet(clazz, mask, new StringConverter() {
                final IConverter<Object,String> c = TypeConverter.getFormatConverter( clazz, mask );
                @Override
                public String toString( Object o ) {
                    return c.to(o);
                }
                @Override
                public Object fromString( String s ) {
                    return c.from(s);
                }
            });
        }

        fmtCache.add(t);

        return t.third;
    }

    @Override
    protected void updateItem(Object item, boolean empty) {

        super.updateItem( item, empty );

        if( item == null || empty )
        {
            setText(null);
        }
        else
        {
            final TableColumn fxColumn = getTableColumn( );
            final StringConverter sc;
            if( fxColumn instanceof JInvTableColumn )
            {
                final JInvTableColumn invColumn = (JInvTableColumn) fxColumn;
                setAlignment( invColumn.getAlignment() );
                sc = getConverter(item.getClass(), invColumn );
            }
            else
                sc = getConverter( item.getClass(), null );

            setText( sc.toString(item) );
        }

        applyRenderer(item, empty);
    }

}
