package ru.inversion.fx.form.controls.treetableex.infobar;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Separator;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import ru.inversion.fx.form.ActionFactory;
import ru.inversion.fx.form.controls.JInvLongField;
import ru.inversion.fx.form.controls.dsbar.AbstractPartBase;
import ru.inversion.fx.form.controls.dsbar.DSInfoBar;
import ru.inversion.fx.form.controls.treetableex.TSFXAdapter;
import ru.inversion.meta.EntityMetadataFactory;
import ru.inversion.meta.IEntityProperty;
import ru.inversion.tds.CompositeAdapter;
import ru.inversion.tds.ITreeDataSet;
import ru.inversion.utils.U;

import java.util.List;
import java.util.ResourceBundle;

/** */
public class Part_FindById extends AbstractPartBase {

    /** */
    final private HBox pane = new HBox(5.0d);
    /** */
    final private TSFXAdapter adapter;

    /** */
    public Part_FindById( TSFXAdapter adapter, ResourceBundle customBundle ) {
        super(customBundle);
        this.adapter = adapter;

        pane.setAlignment(Pos.CENTER_LEFT);
        pane.getProperties( ).put( PART, this );
        pane.getChildren().addAll( new Separator( Orientation.VERTICAL ), getAggregatorLabel() );

        final List<IEntityProperty> idList = EntityMetadataFactory.getEntityMetaData(adapter.getTreeDataSet().getRowClass()).getIDList();

        final TextField textField;
        final Button    btnSearch;

        final EventHandler<ActionEvent> handler;

        if( idList.get(0).getType() == String.class )
        {
            textField = new TextField( );
            textField.setAlignment ( Pos.CENTER_RIGHT );
            textField.setEditable  ( true );
            handler = event ->doSearch(textField.getText());
        }
        else
        {
            final JInvLongField lf = new JInvLongField();
            textField = lf;
            handler = event ->doSearch(lf.getValue());
        }
        btnSearch = ActionFactory.createButton(ActionFactory.ActionTypeEnum.SEARCH, handler );

        textField.addEventHandler( KeyEvent.KEY_PRESSED, ( KeyEvent event) -> {
            if (event.getCode() == KeyCode.ENTER) {
                handler.handle(null);
            }
        });
        textField.setPrefColumnCount( 5 );
        textField.setStyle ( PART_STYLE );

        pane.getChildren().addAll( textField, btnSearch );
    }

    //
    private void doSearch( Object value )
    {
        final ITreeDataSet treeDataSet = adapter.getTreeDataSet();
        final CompositeAdapter ca = new CompositeAdapter( treeDataSet.getRowClass() );
        treeDataSet.findItem(o -> U.equals( ca.getId(o), value ));

        //TODO: Если узел не найден, то вывести алерт - или моргнуть контролом ?
    }

    /** */
    @Override
    public DSInfoBar.PartEnum getType() {
        return DSInfoBar.PartEnum.FindById;
    }

    /** */
    @Override
    public Pane createControlPane() {
        return pane;
    }

    /** */
    @Override
    public Pane getControlPane() {
        return pane;
    }
}
