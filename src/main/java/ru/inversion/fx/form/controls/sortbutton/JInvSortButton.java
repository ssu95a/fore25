package ru.inversion.fx.form.controls.sortbutton;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.input.MouseEvent;
import ru.inversion.fx.form.ActionFactory;
import ru.inversion.fx.form.controls.IJInvControl;
import ru.inversion.fx.form.controls.JInvButton;
import ru.inversion.icons.IconDescriptorBuilder;
import ru.inversion.icons.enums.FontAwesome;
import ru.inversion.utils.S;

import java.util.ResourceBundle;

import static ru.inversion.fx.form.controls.Controls.CONTROL_FIELD_NAME;

/**
 * Хрен знает чо за кнопка
 * <p>
 * @author antonovdi,
 *         sulimoff
 */
public class JInvSortButton extends JInvButton implements IJInvControl {

    public static final String SORT_BUTTON = "SORT_BUTTON";

    private static final ResourceBundle bundle = ResourceBundle.getBundle("fore");

    private Label arrowUp;
    private Label arrowDown;

    private String orderBy;
    private String idTable;

    private OrderByManager sortManager;

    public enum SortStateEnum {

        NONE,

        ASC,

        DESC;

        public SortStateEnum next() {
            return this.ordinal() == SortStateEnum.values().length - 1 ? SortStateEnum.values()[0] : SortStateEnum.values()[this.ordinal()+1];
        }

        public TableColumn.SortType toSortType() {
            return this == DESC ? TableColumn.SortType.DESCENDING : TableColumn.SortType.ASCENDING;
        }
    }

    private final ObjectProperty<SortStateEnum> sortModeProperty = new SimpleObjectProperty<>(SortStateEnum.NONE);

    public void setSortMode(SortStateEnum mode) {
        sortModeProperty.set(mode);
    }

    public SortStateEnum getSortMode() {
        return sortModeProperty.get();
    }

    public ObjectProperty<SortStateEnum> sortModeProperty() {
        return sortModeProperty;
    }

    public JInvSortButton() {
        super();
        init();
    }

    public JInvSortButton(String text) {
        super(text);
        init();
    }

    public JInvSortButton(String text, Node graphic) {
        super(text, graphic);
        init();
    }

    private void init() {
        initView();
        initBehavior();
    }

    private void initView( ) {

        setStyle( "-fx-content-display:right" );
        setText ( bundle.getString("LABEL_SORT") );

        sortModeProperty.addListener((ObservableValue<? extends SortStateEnum> observable, SortStateEnum oldValue, SortStateEnum newValue) -> {

            //toTableColumn().setSortType( newValue.toSortType() );

            switch (newValue) {
                case ASC: {
                    setGraphic( getUP() );
                    break;
                }
                case DESC: {
                    setGraphic( getDOWN() );
                    break;
                }
                default:
                    setGraphic(null);
                    break;
            }
        });
    }

    //lazy init
    private Label getDOWN()
    {
        if( arrowDown == null )
            arrowDown = ActionFactory.getLabel( new IconDescriptorBuilder<>( FontAwesome.fa_caret_down).build() );
        return arrowDown;
    }
    private Label getUP() {
        if ( arrowUp == null )
            arrowUp = ActionFactory.getLabel( new IconDescriptorBuilder<>( FontAwesome.fa_caret_up).build() );
        return arrowUp;
    }

    private boolean shiftDown = false;

    private void initBehavior() {

        setOnMouseClicked( (MouseEvent event) -> {

            shiftDown = event.isShiftDown();

            if( shiftDown )
            {
                fire();
            }
            else
            {
                event.consume();
            }
        });

        setOnAction( (ActionEvent event) -> {

            changeToNextSortMode();

            if( sortManager != null )
                sortManager.buttonHandle( this, shiftDown );

            shiftDown = false;

        });
    }

    private void changeToNextSortMode() {
        setSortMode( getSortMode().next() );
    }

    @Override
    public String getFieldName() {
        return getProperty( CONTROL_FIELD_NAME );
    }

    @Override
    public void setFieldName( String fieldName ) {

        setProperty( CONTROL_FIELD_NAME, fieldName );

        if( S.isNotNullOrEmpty(fieldName) && S.isNullOrEmpty(orderBy) )
            orderBy = fieldName;
    }

    public String getOrderBy() {
        return orderBy;
    }

    public void setOrderBy(String orderBy) {
        this.orderBy = orderBy;
    }

    public String getIdTable() {
        return idTable;
    }

    public void setIdTable(String idTable) {
        this.idTable = idTable;
    }

    public OrderByManager getSortManager() {
        return sortManager;
    }

    /** */
    public void setSortManager(OrderByManager sortManager) {
        this.sortManager = sortManager;
    }

    /** */
    private TableColumn tableColumn;

    TableColumn toTableColumn()
    {
        if( tableColumn == null ) {
            tableColumn = new TableColumn();
            tableColumn.setSortable( true );
            tableColumn.setVisible ( false );
            tableColumn.setSortType( getSortMode().toSortType() );
            tableColumn.getProperties().put( SORT_BUTTON, this );
        }

        return tableColumn;
    }

}
