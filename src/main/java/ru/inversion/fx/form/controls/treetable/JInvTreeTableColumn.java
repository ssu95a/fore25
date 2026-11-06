/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ru.inversion.fx.form.controls.treetable;
import javafx.scene.control.Control;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.control.TreeTableView;
import ru.inversion.fx.app.BaseApp;
import ru.inversion.fx.app.es.JInvErrorService;
import ru.inversion.fx.app.service.IViewPrefSaver;
import ru.inversion.fx.app.service.PPrefComponent;
import ru.inversion.fx.form.ViewContext;
import ru.inversion.fx.form.controls.IViewChangeable;

import java.util.ResourceBundle;

import static ru.inversion.fx.app.service.ViewPrefAppService.DIMENSION_FRACTIONAL_FACTOR;

/**
 *
 * @author perov
 * @param <S>
 * @param <T>
 */
public class JInvTreeTableColumn<S, T> extends TreeTableColumn<S, T> implements IViewChangeable {

    private static final ResourceBundle FORE = ResourceBundle.getBundle("fore");

    public static final String COLUMN_FIELD_NAME = "ru.inversion.field_name";
    public static final String COLUMN_TRANSIENT = "ru.inversion.column.transient";
    public static final String COLUMN_ORDERBY = "ru.inversion.order_by";
    private IViewPrefSaver prefSaver;

    public JInvTreeTableColumn() {
        super();
    }

    public JInvTreeTableColumn(String text) {
        super(text);
    }

    public String getFieldName() {
        return (String) getProperties().getOrDefault(COLUMN_FIELD_NAME, "");
    }

    public void setFieldName(String fieldName) {
        getProperties().put(COLUMN_FIELD_NAME, fieldName);
    }

    public Boolean getTransientColumn() {
        return (Boolean) getProperties().getOrDefault(COLUMN_TRANSIENT, Boolean.FALSE);
    }

    public void setTransientColumn(Boolean val) {
        if (val != null) {
            getProperties().put(COLUMN_TRANSIENT, val);
        }
    }

    @Override
    public void setViewPrefSaver( final IViewPrefSaver saver ) {
        this.prefSaver = saver;
    }

    private String getElementName() {

        if (this.getFieldName() != null && !this.getFieldName().isEmpty()) {
            return this.getFieldName();
        }
        if (this.getId() != null && !this.getId().isEmpty()) {
            return this.getId();
        }
        if (!this.getColumns().isEmpty()) {
            return null;
        }
        throw new RuntimeException(FORE.getString("ERROR_LOAD_COLUMN_SETTINGS") + " " + this.getText());
    }

    private String getFormName() {
        TreeTableView<S> treeTable = this.getTreeTableView();
        if ( treeTable instanceof JInvTreeTable) {
            ViewContext vc = ((JInvTreeTable) treeTable).getController().getViewContext();
            if ( vc.getFormName() != null && !vc.getFormName().isEmpty()) {
                return vc.getFormName();
            }
        }
        throw new RuntimeException(FORE.getString("ERROR_LOAD_COLUMN_SETTINGS") + " " + this.getText());
    }

    private String getComponentName() {
        TreeTableView<S> treeTable = this.getTreeTableView();
        if (treeTable.getId() != null && !treeTable.getId().isEmpty()) {
            return treeTable.getId();
        }
        throw new RuntimeException(FORE.getString("ERROR_LOAD_COLUMN_SETTINGS") + " " + this.getText());
    }

    @Override
    public void applyViewPrefs() {
        if (prefSaver != null) {

            final TreeTableView<S> table = this.getTreeTableView();
            //ежели понадобится отключать сохранение/восстановление
//            if (table instanceof JInvTreeTable ) {
//                if (!((JInvTreeTable) table).isEnableColumnManager()) {
//                    return;
//                }
//            }
            try {
                PPrefComponent p = prefSaver.getInitialPrefs().stream()
                        .filter((PPrefComponent pref) -> pref.getFORM_NAME().equals(getFormName()))
                        .filter((PPrefComponent pref) -> pref.getCOMPONENT() != null && pref.getCOMPONENT().equals(getComponentName()))
                        .filter((PPrefComponent pref) -> pref.getELEMENT() != null && pref.getELEMENT().equals(getElementName()))
                        .findFirst().orElse(null);
                if (p != null) {
                    //если ширина уже свзяна, то пропускаем
                    if( !this.prefWidthProperty().isBound() )
                    {
                        if ( table.getColumnResizePolicy() != TreeTableView.CONSTRAINED_RESIZE_POLICY ) {
                            if (p.getWIDTH() <= 0) {
                                this.setPrefWidth( Control.USE_PREF_SIZE);
                            }
                            this.setPrefWidth(p.getWIDTH() / DIMENSION_FRACTIONAL_FACTOR);
                        }
                        table.refresh();
                    }

                    this.setVisible(p.getVISIBLE() == 1L);
                    if (p.getORDBY() != null && p.getORDBY() != -1 && table != null) {
                        int totalColumns = table.getColumns().size();
                        if (totalColumns > 0) {
                            table.getColumns().remove(this);
                            totalColumns = table.getColumns().size();

                            if (BaseApp.APP().getViewPrefService().isMarkLeft()) {
                                table.getColumns().add(Math.min(p.getORDBY() + 1, totalColumns), this);
                            } else {
                                table.getColumns().add(p.getORDBY(), this);
                            }
                        }
                    }
                }
            } catch (Exception ex) {
                JInvErrorService.handleException(null, ex);
            }

        }
    }
}
