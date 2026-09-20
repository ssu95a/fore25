package ru.inversion.fx.form.lov;

import ru.inversion.db.dialect.SqlDialect;
import ru.inversion.db.entity.ContentTypeEnum;
import ru.inversion.fx.form.controls.renderer.ContentTypeManager;
import ru.inversion.meta.IEntityProperty;
import ru.inversion.utils.S;
import ru.inversion.utils.U;

import java.util.ResourceBundle;

/**
 *
 * @author ssu
 */
public class JInvEntityLovColumn<P> {

	final private int    width;
    final private String title;
    final private String name;
    final private int    index;
    
    final private IEntityProperty<P,?> pd;

    /** */
    public JInvEntityLovColumn( IEntityProperty<P,?> pd, ResourceBundle rb ) {
        this( pd, rb, Integer.MAX_VALUE );
    }
    
    /** */
    public JInvEntityLovColumn( IEntityProperty<P,?> pd, ResourceBundle rb, int index ) {
        
        this.pd    = pd;
        this.index = index;
        String columnName   = pd.getColumnInfo() == null ? null : pd.getColumnName();
        String propertyName = pd.getPropertyName();

        int w = -1;
        
        if( rb == null ) {
            this.title = null;
            this.name  = propertyName;
        }
        else
        {
            final String key;

            if( rb.containsKey(propertyName) )
            {
                this.title = rb.getString(propertyName);
                this.name  = propertyName;
                key = propertyName + "_WIDTH";
            }
            else if( columnName != null && rb.containsKey(columnName) ) {
                this.title = rb.getString(columnName);
                this.name  = columnName;
                key = columnName + "_WIDTH";
            }
            else
            {
                key = null;
                this.title = null;
                this.name  = null;
            }

            if( key != null && rb.containsKey(key) )
                try { w = Integer.parseInt( rb.getString(key) ); } catch(Throwable th) {  }
        }
        
        if( w == -1 ) {
        
            int len = pd.getLength();

            if( len != -1 ) {

                if( len <= 3 )
                    len ++;

                else if ( len >= 180 )
                    len = 180;

                w = len * 9;            
            }//end if
        }
        
        this.width = w == -1 ? type2length(pd) * 9 : w;
    }

    /** */
    public int getWidth() {
        return width;
    }

    /** */
    public int getIndex() {
        return index;
    }
    
    /** */
    public String getName() {
        return name;
    }

    /** */
    public String getTitle() {
        return title;
    }
    
    /** */
    public Object getColumnValue( P pojo ) throws Exception {
        return pd.invokeGetter(pojo);
    }
    
    /** */
    public String getDBColumnName(SqlDialect dialect) {
//        if( pd.getColumnInfo() != null )
//            return pd.getColumnInfo().getName();
//        return null;
        return pd.getColumnName(dialect);
    }
    
    /** */
    private static int type2length( IEntityProperty pd ) {
        
        ContentTypeEnum content = pd.getContent();
        
        if( content != null )
            return ContentTypeManager.getDefaultLengthSyb(content);
        
        if( Number.class.isAssignableFrom(pd.getType()) )
            return 10;
        
        return -1;
    }

    /** */
    public boolean isSortable()
    {
        return !pd.isTransient();
    }
}
