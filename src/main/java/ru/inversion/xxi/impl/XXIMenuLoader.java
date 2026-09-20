package ru.inversion.xxi.impl;

import ru.inversion.fx.app.frame.menu.IMenuItemData;
import ru.inversion.fx.app.frame.menu.IMenuLoader;
import ru.inversion.tc.TaskContext;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.LinkedList;
import java.util.List;
import java.util.ResourceBundle;

/**
 *
 * @author ssu @
 */
public class XXIMenuLoader implements IMenuLoader {

    private static final ResourceBundle fore = ResourceBundle.getBundle("fore");

	private final String mna, parentMenu;
	private final TaskContext tc;
	
	public XXIMenuLoader( TaskContext tc, String mna ) {
		this(tc, mna, null);
	}

	public XXIMenuLoader( TaskContext tc, String mna, String parentMenu ) {
		this.mna = mna;
		this.tc  = tc;
		this.parentMenu = parentMenu;
	}

	static final private String strSQL
			= "SELECT a.imnbid, a.cmnbname, NULL, iGrNum, iOrdBy, cJava_Class, cJava_Method, cParams, a.imnbref, a.cmnbitem" +
			"  FROM xmnb a WHERE a.cmnbname is not null and a.cmnbmna = ? /*parent*/ " +
			" ORDER BY a.imnbref NULLS FIRST, iMnbNum, iGrNum, iOrdBy";
	static final private String strSQLPrnt_Ora =
			"AND a.iMnbId IN ( SELECT x.iMnbId FROM mnb x CONNECT BY PRIOR x.imnbid = x.imnbRef START WITH x.cmnbitem = ? AND a.cmnbmna = x.cmnbmna )";

	static final private String strSQLPrnt_Pg =
			"   AND A.IMNBID IN (\n" +
					"with recursive t( IMNBID ) as (\n" +
					"	select X.IMNBID, X.IMNBREF from MNB X where X.CMNBITEM = ? AND A.CMNBMNA = X.CMNBMNA\n" +
					"	union all\n" +
					"      select Y.IMNBID, Y.IMNBREF from MNB Y, t where Y.IMNBREF = t.IMNBID AND A.CMNBMNA = Y.CMNBMNA\n" +
					")" +
					"select IMNBID from t)";

	public List<IMenuItemData> getMenuItemList() {

		try {

			final List<IMenuItemData> mil = new LinkedList<>();

			final String sql;

			if( parentMenu != null )
				sql = strSQL.replace("/*parent*/", tc.isOracle() ? strSQLPrnt_Ora : strSQLPrnt_Pg );
			else
				sql = strSQL;

            try ( PreparedStatement ps = tc.getConnection().prepareStatement(sql) ) {

                ps.setString( 1, mna );
				if( parentMenu != null )
					ps.setString( 2, parentMenu );

                try( ResultSet rs = ps.executeQuery() ) {

                    int size = rs.getMetaData().getColumnCount();

                    while( rs.next() ) {

                        Object a[] = new Object[size];

                        for( int i = 0; i < a.length; i++ )
                            a[i] = rs.getObject(i+1);

                        mil.add( new MenuItemData(a) );
                    }
                }
            }

			return mil;
		}
		catch( Throwable ex ) {
			throw new RuntimeException( fore.getString("ERROR_ON_LOAD_MENU_FROM_DB"), ex );
		}
	}
}
