package ru.inversion.xxi.impl;

import org.slf4j.Logger;
import ru.inversion.fx.app.frame.menu.IMenuItemData;
import ru.inversion.fx.app.frame.menu.IMenuLoader;
import ru.inversion.fx.form.controls.IMnbItem;
import ru.inversion.fx.form.controls.JInvMenuBar;
import ru.inversion.tc.TaskContext;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

/**
 * Загрузка пунктов меню по содержимому JInvMenuBar в FXML
 * @author fomishkin
 * @since 22.04.2022
 */
public class FxmlMenuLoader implements IMenuLoader {

    private static final Logger log = org.slf4j.LoggerFactory.getLogger(FxmlMenuLoader.class);
    private static final ResourceBundle fore = ResourceBundle.getBundle("fore");

    private final JInvMenuBar menuBar;
    private final TaskContext tc;
    static final private String strSQL
            = "SELECT a.imnbid, a.cmnbname, NULL, iGrNum, iOrdBy, cJava_Class, cJava_Method, cParams, a.imnbref, a.cmnbitem" +
            "  FROM xmnb a WHERE a.cmnbmna = ? and a.CMNBNAME is not null and cMnbItem in (";
    //" ORDER BY a.imnbref NULLS FIRST, iMnbNum, iGrNum, iOrdBy";


    public FxmlMenuLoader(TaskContext tc, JInvMenuBar menuBar) {
        this.menuBar = menuBar;
        this.tc  = tc;
    }

    @Override
    public List<IMenuItemData> getMenuItemList() {
        String mnaMenu = menuBar.getMnaMenu();
        List<String> mnbList = new LinkedList<>();
        menuBar.getAllItems().stream()
                .map(item -> (IMnbItem)item)
                .forEach(item -> mnbList.add(item.getMnbItem()));

        try {
            final String sql;
            StringBuilder sb = new StringBuilder(strSQL);

            int ncount = 0;

            for( String s : mnbList ) {
                if( ncount > 0 )
                    sb.append(',');
                sb.append("'").append(s).append("'");
                ncount++;
            }
            sb.append(')').append(" ORDER BY a.imnbref NULLS FIRST, iMnbNum, iGrNum, iOrdBy");

            sql = sb.toString();

            final List<IMenuItemData> mil = new LinkedList<>();

            try ( PreparedStatement ps = tc.getConnection().prepareStatement( sql ) ) {

                ps.setString( 1, mnaMenu );

                try( ResultSet rs = ps.executeQuery() ) {

                    int size = rs.getMetaData().getColumnCount();

                    while( rs.next() ) {

                        Object[] a = new Object[size];

                        for( int i = 0; i < a.length; i++ )
                            a[i] = rs.getObject(i+1);

                        mil.add( new MenuItemData(a) );
                    }
                }
            }

            return mil;
        }
        catch( Throwable ex ) {
            throw new RuntimeException( fore.getString("OSHIBKA_ZAGRUZKI_MENYU"), ex );
        }
    }

    @Override
    public Optional<JInvMenuBar> getMenuBar() {
        return Optional.of(menuBar);
    }
}
