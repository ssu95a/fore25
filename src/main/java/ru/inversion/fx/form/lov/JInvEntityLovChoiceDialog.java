package ru.inversion.fx.form.lov;
import static java.lang.Boolean.TRUE;
import java.util.ResourceBundle;
import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.EventHandler;
import javafx.geometry.HPos;
import javafx.geometry.Pos;
import javafx.geometry.VPos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.SortEvent;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import static javafx.scene.control.TableView.CONSTRAINED_RESIZE_POLICY;
import static javafx.scene.control.TableView.UNCONSTRAINED_RESIZE_POLICY;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.util.Pair;
import ru.inversion.dataset.DataSetException;
import ru.inversion.dataset.SQLDataSet;
import ru.inversion.dataset.fx.DSFXAdapter;
import ru.inversion.dataset.fx.ICellValueChangeListener;
import ru.inversion.fx.app.BaseApp;
import ru.inversion.fx.app.Tags;
import ru.inversion.fx.app.es.JInvErrorService;
import ru.inversion.fx.app.service.ViewPrefAppService;
import ru.inversion.fx.form.IFormStateListener;
import ru.inversion.fx.form.StateEnum;
import ru.inversion.fx.form.ViewContext;
import ru.inversion.fx.form.action.JInvKeyboardManager;
import ru.inversion.fx.form.action.decorator.ProgressFormStateDecorator;
import ru.inversion.fx.form.controls.Controls;
import ru.inversion.fx.form.controls.JInvLovSearchPane;
import ru.inversion.fx.form.controls.JInvTable;
import ru.inversion.fx.form.controls.JInvTableColumn;
import ru.inversion.fx.form.lov.exceptions.JInvLovException;
import ru.inversion.tc.TaskContext;
import ru.inversion.utils.S;

/**
 *
 * @author ssu @
 */
public class JInvEntityLovChoiceDialog<P> extends Dialog<P>  implements IFormStateListener {

    private static final ResourceBundle fore = ResourceBundle.getBundle("fore");
    private final Integer WIDTH_HGAP = 15; //  15 вспомогательный столбец "+"
    private final GridPane		grid;
    private final TableView<P>  table;
	private final JInvEntityLov<P,?> lov;
	private final SQLDataSet<P> dataSet;

    protected ObjectProperty<StateEnum> state = new SimpleObjectProperty<>(StateEnum.ACTIVE);

    private final JInvLovSearchPane lovSearchPane = new JInvLovSearchPane();

    /** */
    public JInvEntityLovChoiceDialog( Window parent, JInvEntityLov<P,?> lov, String filterValue ) {

		this.lov = lov;

		this.initOwner(parent);

		this.setResizable(true);

		final DialogPane dialogPane = getDialogPane();

        setTitle( lov.getTitle() );

        this.grid = new GridPane();
        this.grid.setHgap( 10 );
        this.grid.setVgap( 10 );
        this.grid.setMaxWidth ( Double.MAX_VALUE );
        this.grid.setAlignment( Pos.CENTER_LEFT  );

        TaskContext tc = lov.getTaskContext();
        if( tc == null )
            throw new IllegalStateException( Tags.PRODUCT_LABEL + fore.getString("DLYA_LOV_NE_USTANOVLEN_TASKCONTEXT"));

        table = new JInvTable();
        table.setTableMenuButtonVisible(true);

        int width = -1, height = -1;

        ResourceBundle rs = lov.getResourceBundle();
        if( rs != null ) {
            if( rs.containsKey("LOV_WIDTH") )
                try { width = Integer.parseInt( rs.getString("LOV_WIDTH" ) );} catch(Throwable th) {}
            if( rs.containsKey("LOV_HEIGHT") )
                try { height= Integer.parseInt( rs.getString("LOV_HEIGHT") );} catch(Throwable th) {}
        }

        // если в LOV определено несколько столбцов,

        if( lov.getColumnList().size() > 1 ) {

            if( width == -1 )
                width = lov.getColumnList().stream().filter( (c)->c.getWidth() > 0 ).mapToInt( (c)->c.getWidth() ).sum();

            if( width > 0 ) {

                if( width > 1200 ) {
                    width = 1200;

                    table.setColumnResizePolicy( UNCONSTRAINED_RESIZE_POLICY );
                }

                getDialogPane( ).setPrefWidth((double)width + (this.grid.getHgap()*2) + WIDTH_HGAP);
            }

            if( height > 0 )
                getDialogPane( ).setPrefHeight((double)height);
        } else {
            table.setColumnResizePolicy( CONSTRAINED_RESIZE_POLICY );
        }

		dataSet = new SQLDataSet<>();
		dataSet.setRowClass     ( lov.getEntityClass() );
		dataSet.setCallbackParameters( lov.getParameters() );
		dataSet.setTaskContext  ( tc );

                if (lov.getProperty("ru.inversion.dataset.query_alias") != null){
                    dataSet.setQueryAlias((String) lov.getProperty("ru.inversion.dataset.query_alias"));
                }

        //dataSet.setFilter       ( lov.getWherePredicat(), true, true );
                dataSet.setWherePredicat(lov.getWherePredicat());
                dataSet.setFilter(lov.getFilter(), true, true);
        String orderBy = lov.getChoiceOrderBy( );

        if( S.isNotNullOrEmpty(orderBy) )
            dataSet.setOrderBy(orderBy);

        setResultConverter(
				(dialogButton) -> {
					ButtonBar.ButtonData data = dialogButton == null ? null : dialogButton.getButtonData();
					return data == ButtonBar.ButtonData.OK_DONE ? getSelectedItem( ) : null;
        });

		try {

			DSFXAdapter dsf = new DSFXAdapter( ) {

				@Override
				protected void bindColumns( ICellValueChangeListener cellValueChangeListener ) throws Exception {

					for( JInvEntityLovColumn<P> column : lov.getColumnList() )
                    {
                        String title = column.getTitle();
                        if( S.isNullOrEmpty(title) )
                            continue;

						TableColumn<P,Object> tableColumn = new TableColumn<>( title );
                        tableColumn.setId		( column.getName() );
						tableColumn.setUserData ( column );
                        tableColumn.getProperties().put( JInvTableColumn.COLUMN_LOV, TRUE );

                        if( column.getWidth() > 0 )
							tableColumn.setPrefWidth( column.getWidth() );

                        table.getColumns().add ( tableColumn );

					}//end for

                    super.bindColumns( cellValueChangeListener );
				}
			};

            table.setOnSort( new EventHandler<SortEvent<TableView<P>>>() {
                @Override
                public void handle(SortEvent<TableView<P>> event) {

                    try {

                        StringBuilder sb = new StringBuilder();
                        event.getSource()
                             .getSortOrder()
                             .stream ( )
                             //.filter ( (t)->!S.isNullOrEmpty( t.getId() ) )
                             .forEach( (t)->sb.append( t.getId() ).append( t.getSortType() == TableColumn.SortType.ASCENDING ? " ASC " : " DESC " ).append(',') );

                        if( sb.length() > 0 ) {
                            sb.deleteCharAt   ( sb.length()-1 );
                            dataSet.setOrderBy( sb.toString() );
                            dataSet.executeQuery( );
                        }
                        else
                            dataSet.setOrderBy( null );

                    } catch (DataSetException ex) {
                        JInvErrorService.handleException( table.getScene().getWindow(), ex);
                    }
                }
            } );

			dialogPane.getButtonTypes().addAll( ButtonType.OK, ButtonType.CANCEL );

			final Button btOK =  ( Button )dialogPane.lookupButton(ButtonType.OK);
			btOK.setDefaultButton( false  );

			dialogPane.addEventHandler( KeyEvent.KEY_RELEASED, new EventHandler<KeyEvent>() {
				@Override
				public void handle(KeyEvent event) {
					if( ( event.isControlDown() && event.getCode() == KeyCode.ENTER ) || event.getCode() == KeyCode.F9 ) {
                        event.consume();
						btOK.fire();
					}
				}
			});

			table.setOnMouseClicked(new EventHandler<MouseEvent>() {
				@Override
				public void handle(MouseEvent event) {
					if( event.getClickCount() > 1 ) {
						btOK.fire();
					}
				}//end if
			});

                        table.setOnKeyReleased(new EventHandler<KeyEvent>() {
                            @Override
                            public void handle(KeyEvent event) {
                                if (event.getCode().equals(KeyCode.ENTER)){
                                    btOK.fire();
                                }
                            }
                        });

			dsf.bindTable( dataSet, table, null );

            Pair<String, Boolean> choiceFilter = lov.getChoiceFilter();

            String strSQL = dataSet.getSQL();

            boolean addLovParameter = true;

            if( !S.isNullOrEmpty(strSQL) )
                 addLovParameter = !strSQL.contains(":lov_parameter");

            try {

                lovSearchPane.init( table,
                    addLovParameter,
                    choiceFilter == null ? null  : choiceFilter.getKey(),
                    choiceFilter == null ? false : choiceFilter.getValue(),
                    filterValue,
                    lov.getColumnValue().getDBColumnName(tc.dialect())
                );

                if (lov.isSmallLov()){
                    lovSearchPane.setMaxPageSize(lov.getMaxCountRow());
                }

                lovSearchPane.setListener(this);
            }
            catch( Throwable th ) {
                throw new JInvLovException( fore.getString("OSHIBKA_PRI_INICIALIZACII_PANELI_POISKA"), th );
            }

            BaseApp.APP().getViewPrefService().refreshViewSettingsRoot( dialogPane.getScene().getRoot() );

		} catch( Throwable th ) {
			throw new JInvLovException( fore.getString("NEVOZMOZHNO_OTOBRAZIT_DIALOG_VYBORA_DLYA_LOV"), th );
		}


        showingProperty().addListener( (ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) -> {
            if( newValue ) {
                new ProgressFormStateDecorator( this, new ViewContext((Stage)getDialogPane().getScene().getWindow()));
            }
        });

        updateGrid( );

        this.getDialogPane().sceneProperty().addListener(new ChangeListener<Scene>() {
            //Сработали разок – и хватит
            boolean triggeredOnce = false;

            @Override
            public void changed(ObservableValue<? extends Scene> observable, Scene oldValue, Scene newValue) {
                if ( triggeredOnce ){
                    return;
                }

                if (newValue != null) {

                    grid.getProperties().put(Controls.CONTROL_PARENT, newValue.getRoot());

                    JInvKeyboardManager.initKeyBoard(newValue);
                    triggeredOnce = true;
                }

            }
        });

        ViewPrefAppService.localizeDialog(dialogPane);

    }

    /**
     * Returns the currently selected item in the dialog.
     * <p>
     */
    public final P getSelectedItem() {

		if( dataSet.isEmpty() )
			return null;

		return dataSet.getCurrentRow();
    }

    /**
     *
     */
    private void updateGrid() {

        grid.getChildren().clear();
        grid.add( lovSearchPane, 0, 0 );
        GridPane.setConstraints( lovSearchPane, 0, 0, 1, 1, HPos.RIGHT, VPos.CENTER, Priority.NEVER, Priority.NEVER);
        grid.add( table, 0, 1);
        GridPane.setConstraints( table, 0, 1, 1, 1, HPos.RIGHT, VPos.CENTER, Priority.ALWAYS, Priority.ALWAYS);
        getDialogPane( ).setContent( grid );

		Platform.runLater( () -> table.requestFocus() );
    }

    /** */
    @Override
    public ObjectProperty<StateEnum> stateProperty() {
        return state;
    }

    protected StringProperty stateText = new SimpleStringProperty( S.EMPTY_STRING );

    @Override
    public StringProperty stateTextProperty() {
        return stateText;
    }
}
