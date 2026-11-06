package ru.inversion.fx.form.controls;

import javafx.beans.property.StringProperty;
import javafx.scene.Node;
import javafx.scene.control.*;
import ru.inversion.dataset.fx.DSFXAdapter;
import ru.inversion.fx.form.ActionFactory;
import ru.inversion.fx.form.JInvFXFormController;
import ru.inversion.fx.form.action.IAction;
import ru.inversion.icons.IBaseIconDescriptor;
import ru.inversion.icons.enums.FontAwesome;
import ru.inversion.utils.S;
import ru.inversion.utils.U;

import java.util.List;
import java.util.Optional;

/**
 *
 * @author ssu
 */
public class JInvMenuButton extends MenuButton implements IJInvControl {

    /** */
    public JInvMenuButton( ) {
        readOnlyProperty().addListener( (obs, oldV, newV) -> Controls.disableControl( this, obs.getValue() ) );
    }

    /** */
    public JInvMenuButton(String text) {
        super( text );
        readOnlyProperty().addListener( (obs, oldV, newV) -> Controls.disableControl( this, obs.getValue() ) );
    }

    /** */
    public JInvMenuButton( String text, Node graphic ) {
        super( text, graphic );
        readOnlyProperty().addListener( (obs, oldV, newV) -> Controls.disableControl( this, obs.getValue() ) );
    }

    /** */
    public JInvMenuButton( String text, Node graphic, MenuItem... items ) {
        super( text, graphic, items );
        readOnlyProperty().addListener( (obs, oldV, newV) -> Controls.disableControl( this, obs.getValue() ) );
    }

    /** */
    public void init( String text, Node graphic, String tooltip, List<? extends IAction> actionList ) {

        this.setFocusTraversable(false);

        if( S.isNotNullOrEmpty(tooltip) )
            this.setTooltip( new Tooltip(tooltip) );

        if( graphic != null )
            this.setGraphic(graphic);
        else
            this.setText(text);

        getItems().clear();

//        JInvFXFormController<?> c = this.getController();

        if( actionList != null && !actionList.isEmpty() ) {

            actionList.forEach((a) -> {

                MenuItem mi;

                if( S.isNullOrEmpty( a.getIconCode() ) )
                    mi = new JInvMenuItem( a.getTitle() ) {
                        @Override
                        public JInvFXFormController<?> getController() {
                            return JInvMenuButton.this.getController();
                        }
                    };
                else
                    mi = new JInvMenuItem( a.getTitle(), ActionFactory.getLabel( a.getIconCode() ) ) {
                        @Override
                        public JInvFXFormController<?> getController() {
                            return JInvMenuButton.this.getController();
                        }
                    };

                mi.setOnAction(a);

                getItems().add(mi);
            }); //end for

        }//end if
    }

    /** */
    public void init( ActionFactory.IconEnum ai, List<? extends IAction> actionList ) {
        init( ai.getCode(), ActionFactory.getLabel( ai ), null, actionList );
    }

    /** */
    public void init(IBaseIconDescriptor icon, String tooltip, List<? extends IAction> actionList ) {
        init(null, icon.getLabel(), tooltip, actionList);
    }

    /** */
    public void init( ActionFactory.ActionTypeEnum au, List<? extends IAction> actionList ) {

        // init( au.getName(), ActionFactory.getLabel( au.getIconFontCode() ), au.getTooltip(), actionList );

        String text    = au.getName();
        String toolTip = au.getTooltip();
        Node   graphic = ActionFactory.getLabel( au.getIconFontCode() );

        this.setFocusTraversable(false);

        if( S.isNotNullOrEmpty(toolTip) )
            this.setTooltip( new Tooltip(toolTip) );

        if( graphic != null )
            this.setGraphic(graphic);
        else
            this.setText(text);

        getItems().clear();

        //JInvFXFormController<?> c = this.getController();

        if( actionList != null && !actionList.isEmpty() ) {

            actionList.forEach((a) -> {

                MenuItem mi;

                if(  U.equals( a.getIconCode(), au.getIconFontCode() ) || S.isNullOrEmpty( a.getIconCode() ) )
                    mi = new JInvMenuItem( a.getTitle() ) {
                        @Override
                        public JInvFXFormController<?> getController() {
                            return JInvMenuButton.this.getController();
                        }
                    };
                else
                    mi = new JInvMenuItem( a.getTitle(), ActionFactory.getLabel( a.getIconCode() ) ) {
                        @Override
                        public JInvFXFormController<?> getController() {
                            return JInvMenuButton.this.getController();
                        }
                    };

                mi.setOnAction(a);

                getItems().add(mi);
            }); //end for

        }//end if


    }

    @Override
    public Control setLabel(Label label) {
        return this;
    }

    @Override
    public Label getLabel() {
        return null;
    }

    @Override
    public StringProperty labelTextProperty() {
        return textProperty();
    }

    @Override
    public void setAction(IAction action) {
    }

    @Override
    public <T> DSFXAdapter<T> getDataSetAdapter() {
        return null;
    }

    @Override
    public String getFieldName() {
        return null;
    }

    @Override
    public void setFieldName(String fieldName) {
    }

    @Override
    public void setToolTipText(String toolTipText) {
        this.setTooltip( new Tooltip(toolTipText) );
    }

    /*
    @Override
    public Optional<String> getToolTipText() {
        Tooltip t = getTooltip();
        return t == null ? Optional.empty() : Optional.of( t.getText() );
    }
    */
}
