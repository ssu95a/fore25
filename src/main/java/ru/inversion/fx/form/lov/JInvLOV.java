package ru.inversion.fx.form.lov;

import javafx.beans.property.Property;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import ru.inversion.dataset.parser.SQLParser;
import ru.inversion.fx.app.BaseApp;
import ru.inversion.fx.app.es.JInvErrorService;
import ru.inversion.fx.form.AbstractBaseController;
import ru.inversion.fx.form.FXFormLauncher;
import ru.inversion.fx.form.ViewContext;
import ru.inversion.fx.form.lov.exceptions.JInvLovException;
import ru.inversion.tc.TaskContext;
import ru.inversion.utils.U;
import ru.inversion.utils.converter.TypeConverter;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.ResourceBundle;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 *
 * @author antonovdi
 */
public class JInvLOV extends AbstractLovBase<Object> implements AutoCloseable {

    protected static final ResourceBundle fore = ResourceBundle.getBundle("fore");

	private final StringProperty sqlSelect	= new SimpleStringProperty( );

	private TaskContext	taskContext;

	//private IParameters	parameters;

	private List< JInvLOVColumn > columnList;

	/**
     * @param taskContext */
	public void setTaskContext( TaskContext taskContext ) {
		this.taskContext = taskContext;
	}
	public TaskContext getTaskContext( ) {
		return taskContext;
	}

	/**
     * @return  */
	public final String getSqlSelect( ) {
		return sqlSelect.get();
	}
	public final void setSqlSelect( String sqlSelect ) {
		this.sqlSelect.set(sqlSelect);
	}
	public final StringProperty sqlSelectProperty() {
		return sqlSelect;
	}

	/**
     * @param columnName
     * @param columnClass
     * @param title
     * @param size
     * @param bindProperty */
	public void addColumn( String columnName, Class columnClass, String title, int size, Property<?> bindProperty ) {
		if( columnList == null )
			columnList = new ArrayList<>();
		columnList.add( new JInvLOVColumn( columnName, columnClass, title, size, bindProperty ));
	}

    /** */
	public JInvLOVColumn getValueColumn() {

		for( JInvLOVColumn c : getColumnList() ) {
			if( c.getWidth() != 0 ) {
                return c;
            }
		}
		return null;
	}

	public List<JInvLOVColumn> getColumnList() {
		return U.nvl( columnList, Collections.<JInvLOVColumn>emptyList() );
	}


    @Override
    public void showChoiceList(ViewContext vc, String filterString, BiConsumer<Boolean,ILov<Object>> clb) {
        try {
            if (getColumnList().isEmpty()) {
                throw new IllegalStateException(fore.getString("NE_OPREDELENY_STOLBCY_DLYA_LOV"));
            }

			if( isSkipFilterString ())
				filterString = "%";

			final PLovParam lovParam = new PLovParam(this, filterString);

			new FXFormLauncher<PLovParam>(getTaskContext(), vc, "ru/inversion/fx/form/lov/fxml/ViewLov.fxml")
					.bundle(BaseApp.APP().getCommonResourceBundle())
					.dataObject(lovParam)
					.dialogMode(AbstractBaseController.FormModeEnum.VM_CHOICE)
					.callback((formReturnEnum, controller) -> {
						if (formReturnEnum == AbstractBaseController.FormReturnEnum.RET_OK) {
							setValue(TypeConverter.convert(controller.getDataObject().getResult(),
									getColumnList().get(0).getColumnClass()));
							clb.accept(true, this);
						}
					})
					.modal(true)
					.show();

//            JInvLOVChoiceDialog lovDialog = new JInvLOVChoiceDialog(vc.getStage(), this, filterString);
//
//            /**
//             * если маленький лов устанавливаем позицию окна
//             */
//            if (this.isSmallLov()) {
//                Rectangle2D primaryScreenBounds = Screen.getPrimary().getVisualBounds();
//                lovDialog.setX(primaryScreenBounds.getMinX() + this.getPosition().getKey());
//                lovDialog.setY(primaryScreenBounds.getMinY() + this.getPosition().getValue()+1);
//            }
//
//            Optional<Object> op = lovDialog.showAndWait();
//			op.ifPresent(o -> setValue(TypeConverter.convert(o, getColumnList().get(0).getColumnClass())));
//            clb.accept(op.isPresent(), this);

        } catch (Throwable th) {
            JInvErrorService.handleException(vc.getStage(), th);
        }

    }


	private PreparedStatement checkValueStatement;
	private List<Function>	  checkValueParameter;

	/** */
	private void initCheckValueStatement() throws Exception {

		if( getValueColumn() == null )
			throw new IllegalStateException(fore.getString("NE_OPREDELENY_STOLBCY_DLYA_LOV"));

		SQLParser parser = new SQLParser( getSqlSelect() );
		checkValueParameter = new ArrayList<>();

		String strPreparedSQL = parser.prepareActualSqlEx( getParameters(), checkValueParameter );

		StringBuilder sb = new StringBuilder( );
		sb	.append("select 1 from dual where exists ( select 1 from (")
			.append( strPreparedSQL )
			.append(") q where q.")
			.append( getValueColumn().getColumnName() )
			.append("= ?)");

		checkValueStatement = getTaskContext().getConnection().prepareStatement( sb.toString() );

	}
	/** */
    @Override
	public boolean checkValue( Object v )  {

        try {

		if( checkValueStatement == null )
			initCheckValueStatement();

        Object value = TypeConverter.convert( v, getValueColumn().getColumnClass() );

		//int nParam = checkValueStatement.getParameterMetaData().getParameterCount();
		int i = 1;
		for( Function f : checkValueParameter ) {
			checkValueStatement.setObject( i++, f.apply(null) );
		}

			checkValueStatement.setObject( i, value );

		try( ResultSet rs = checkValueStatement.executeQuery() ) {
			 return rs.next();
		}
        }
        catch( Throwable th ) {
            throw new JInvLovException( fore.getString("OSHIBKA_PRI_PROVERKE_ZNACHENIYA"), th );
        }
	}
    /** */
	@Override
	public void close() throws Exception {
		if( checkValueStatement != null )
			checkValueStatement.close();
	}
}
