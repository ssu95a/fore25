package ru.inversion.fx.form.controls.table;

import javafx.geometry.HPos;
import javafx.geometry.Pos;
import javafx.geometry.VPos;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.paint.Color;
import org.controlsfx.control.PropertySheet;
import ru.inversion.fx.app.AppException;
import ru.inversion.fx.app.Tags;
import ru.inversion.fx.app.es.JInvErrorService;
import ru.inversion.fx.app.service.PPrefComponent;
import ru.inversion.fx.app.service.ViewPrefDao;
import ru.inversion.fx.form.controls.JInvTable;
import ru.inversion.fx.form.controls.sheet.JInvPropertySheet;
import ru.inversion.icons.IconFactory;
import ru.inversion.icons.enums.FontAwesome;
import ru.inversion.icons.enums.IconSize;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.function.Function;
import java.util.stream.Collectors;

import static ru.inversion.fx.app.service.ViewPrefDao.loadPreferences;
import static ru.inversion.fx.form.controls.table.TableSettingItemEnum.MARK_SAVE_PREV_MARKED_ROWS;
import static ru.inversion.fx.form.controls.table.TableSettingItemValue.BUNDLE_PREFIX;

/** */
public class JInvTableSettingDialog extends Dialog<Boolean> {

    /**
     * Объект для доступа к строковым локализованным ресурсам фреймворка
     */
    protected static final ResourceBundle foreBundle = ResourceBundle.getBundle("fore");

    private final JInvPropertySheet sheet;
    private final JInvTable table;
    private final String formName;

    /** */
    public JInvTableSettingDialog( JInvTable table ) {

        this.formName = table.getController().getClass().getName();
        this.table    = table;

        this.setResizable(true);

        final DialogPane dialogPane = getDialogPane();

        GridPane grid = new GridPane();
        grid.setHgap     ( 10 );
        grid.setVgap     ( 10 );
        grid.setMaxWidth ( Double.MAX_VALUE);
        grid.setAlignment( Pos.CENTER_LEFT );

        setTitle( foreBundle.getString(BUNDLE_PREFIX + "TITLE") );
        dialogPane.setHeaderText ( foreBundle.getString(BUNDLE_PREFIX + "HEADER") );
        dialogPane.getButtonTypes( ).addAll( ButtonType.OK, ButtonType.CANCEL );

        Label l = IconFactory.getLabel( FontAwesome.fa_table, IconSize.LARGE, Color.LIGHTSKYBLUE );
        l.setStyle( l.getStyle() + ";-fx-font-size:2em;");
        dialogPane.graphicProperty().setValue( l );

        sheet = new JInvPropertySheet( );
        sheet.setMode( PropertySheet.Mode.CATEGORY );
        sheet.setSearchBoxVisible( false );

        try {

            Map< String, Object > valuesMap = loadSettingsValues( );

            Arrays
                .asList( TableSettingItemEnum.values() )
                .stream( )
                .filter(
                    (sd)->sd != MARK_SAVE_PREV_MARKED_ROWS || table.getDataSetAdapter().isEnableMark()
                )
                .map( (sd)-> TableSettingItemValue.create( sd, valuesMap ) )
                .collect(
                    Collectors.toCollection( sheet::getItems )
                );

        } catch( AppException e ) {
            throw new RuntimeException( Tags.PRODUCT_LABEL + "Error on init 'JInvTableSettingDialog'", e );
        }

        grid.add( sheet, 0, 0 );
        GridPane.setConstraints( sheet, 0, 0, 1, 1, HPos.LEFT, VPos.CENTER, Priority.ALWAYS, Priority.ALWAYS );

        dialogPane.setContent( grid );

        setResultConverter(( dialogButton ) -> {
            ButtonBar.ButtonData data = dialogButton == null ? null : dialogButton.getButtonData();
            return data == ButtonBar.ButtonData.OK_DONE ? saveSettingsValues() : Boolean.FALSE;
        });
    }

    /** */
    private boolean saveSettingsValues(  ) {

        Function<TableSettingItemValue,PPrefComponent> cp = tsv -> {
            PPrefComponent p = new PPrefComponent();
            p.setFORM_NAME( formName );
            p.setCOMPONENT( table.getId() );
            p.setELEMENT  ( tsv.getSettingName() );
            p.setVISIBLE  ( (long)((TripleBoolValueEnum)tsv.getValue()).toInt() );

            return p;
        };

        try {

            List< PPrefComponent > list
             = sheet.getItems()
                    .stream()
                    .map(( i ) -> (TableSettingItemValue)i)
                    .map( cp::apply )
                    .collect( Collectors.toList() );

            ViewPrefDao.savePreferences( list );

            list.forEach(
                (p)->table.getProperties()
                          .put( p.getELEMENT(), TripleBoolValueEnum.fromInt( p.getVISIBLE().intValue() ) )
            );

            return true;
        }
        catch( Throwable th ) {
            JInvErrorService.handleException( getDialogPane().getScene().getWindow(), th );
        }

        return false;
    }

    /** */
    private Map<String,Object> loadSettingsValues( ) throws AppException {

        final Map< String, Object > map = loadPreferences(
            formName,
            table.getId(),
            Arrays.asList( TableSettingItemEnum.values() )
                  .stream()
                  .map (
                       ( t ) -> t.name()).collect(Collectors.toList()
                  )
        ).stream  ( )
         .collect (
            Collectors.toMap(p -> p.getELEMENT(), p -> p.getVISIBLE())
         );

        return map;
    }
}

