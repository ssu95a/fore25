/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ru.inversion.fx.form.controls;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.AccessibleAttribute;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import ru.inversion.dataset.fx.DSFXAdapter;
import ru.inversion.fx.app.AppException;
import ru.inversion.fx.app.BaseApp;
import ru.inversion.fx.app.es.JInvErrorService;
import ru.inversion.fx.app.sec.SecurityStrategyEnum;
import ru.inversion.fx.app.service.view.ButtonIconView;
import ru.inversion.fx.form.ActionFactory;
import ru.inversion.fx.form.ActionFactory.ActionTypeEnum;
import ru.inversion.fx.form.ActionFactory.IconEnum;
import ru.inversion.fx.form.action.IAction;
import ru.inversion.fx.form.action.JInvAction;
import ru.inversion.fx.form.action.JInvKeyboardManager;
import ru.inversion.icons.IBaseIconDescriptor;
import ru.inversion.utils.S;

import java.util.Optional;
import java.util.ResourceBundle;

import static ru.inversion.fx.form.action.JInvAction.emptyIcon;

/**
 * @author antonovdi
 */
public class JInvButton extends Button implements IJInvControl {

    private static final String PROPERTY_ID_TEXTFIELD = "ru.inversion.button.idtextfield";
    private static final String PROPERTY_BLOCK_BY_TEXTFIELD = "ru.inversion.button.block_by_textfield";
    private static final String PROPERTY_SECURITY_ID = "ru.inversion.button.security_id";
    private static final String PROPERTY_TEXTFIELD = "ru.inversion.button.textfield";
    private static final String PROPERTY_ICON = "ru.inversion.button.icon";
    private static final String PROPERTY_ICON_NAME = "ru.inversion.button.icon_name";
    private static final String PROPERTY_TYPE = "ru.inversion.button.type";
    private static final String PROPERTY_TYPICAL_TYPE = "ru.inversion.button.typical_type";
    private static final String PROPERTY_BUTTON_NAME = "ru.inversion.button.name";

    private StringProperty textLabelProperty = new SimpleStringProperty("");
    private final static ResourceBundle bundle = ResourceBundle.getBundle("fore");

    public JInvButton() {
        super();
        init();
    }

    public JInvButton(String text) {
        super(text);
        init();
    }

    public JInvButton(String text, Node graphic) {
        super(text, graphic);
        init();
    }

    private void init() {
        readOnlyProperty().addListener( (obs, oldV, newV) -> Controls.disableControl( this, obs.getValue() ) );
        setMnemonicParsing(false);
        mnemonicParsingProperty().addListener( (v,o,n) -> {if (n) setMnemonicParsing(false);} );
        JInvKeyboardManager.initNaviationOnNode(this);
    }

    public String getName() {
        return (String) getProperties().getOrDefault(PROPERTY_BUTTON_NAME, null);
    }

    public void setName(String name) {
        getProperties().put(PROPERTY_BUTTON_NAME, name);
    }

    /**
     * @return
     */
    public String getIconName() {
        return (String) getProperties().getOrDefault(PROPERTY_ICON_NAME, null);
    }

    /**
     * @return
     */
    public void setIconName(String val) {
        try {
            if (val != null && !val.isEmpty() && IconEnum.valueOf(val) != null) {
                getProperties().put(PROPERTY_ICON_NAME, val);
            }
        } catch (Throwable ex) {
            JInvErrorService.handleException(null, new RuntimeException(bundle.getString("ERROR_ICON_CODE")));
        }

    }

    /**
     * @return
     */
    public ActionTypeEnum getType() {
        return (ActionTypeEnum) getProperties().getOrDefault(PROPERTY_TYPE, null);
    }



    /**
     * @param type
     */
    public void setType(ActionTypeEnum type) {
        getProperties().put(PROPERTY_TYPE, type);
    }


    @Override
    public String getFieldName() {
        return null;
    }

    @Override
    public void setFieldName(String fieldName) {
    }

    /**
     *
     */
    public Control setLabel(Label label) {
        if (label != null) {
            label.setLabelFor(this);
            textLabelProperty.bind(label.textProperty());
        }
        return this;
    }

    @Override
    public Label getLabel() {
        return (Label) this.queryAccessibleAttribute(AccessibleAttribute.LABELED_BY);
    }

    @Override
    public void setAction(IAction action) {

        if( action != null)
        {
            initGraphic( action );

            if( S.isNullOrEmpty(getToolTipText()) && !S.isNullOrEmpty(action.getToolTip()) )
            {
                setTooltip( new Tooltip( action.getToolTip()) );
            }

            if (action.getActionType() != null) {
                setType(action.getActionType());
            }

            if (action.getId() == null && getSecurityId() != null) {
                action.setId(getSecurityId());
            }

            if (action.getId() != null) {
                internalSetOnAction(action);
            } else if (getOnAction() == null ||
                    (getOnAction() instanceof JInvAction
                            && (((JInvAction) getOnAction()).getHandler()==null))) {

                internalSetOnAction(action);

                if (getScene() != null) {
                    JInvKeyboardManager.addAction(this, action);
                } else {

                    sceneProperty().addListener(new ChangeListener<Scene>() {
                        //Сработали разок – и хватит
                        boolean triggeredOnce = false;

                        @Override
                        public void changed(ObservableValue<? extends Scene> observable, Scene oldValue, Scene newValue) {
                            if ( triggeredOnce ){
                                return;
                            }

                            JInvKeyboardManager.addAction(JInvButton.this, action);
                            triggeredOnce = true;
                        }
                    });
                }
            }

            //серим кнопку, если привязанный экшн отключают
            action.enabledProperty().ifPresent(actionEnabledProperty -> {

                final boolean disabledOnInit = isDisabled();

                actionEnabledProperty.addListener((v,o,n)->{
                    if (disabledOnInit){
                        //избегаем включения изначально выключенной кнопки
                        if (!n) setDisable(true);
                    } else {
                        setDisable(!n);
                    }
                });
            });

            if(!action.isEnabled()){
                setDisable(true);
            }

        }
    }

    /**
     * @see  JInvMenuItem#internalSetOnAction(IAction)
     */
    private void internalSetOnAction(IAction action) {
        SecurityStrategyEnum strategy = action.getSecurityStrategy();

        boolean isEnabledOnInit = action.isEnabledBySecurity();

        if (getId() != null && !isEnabledOnInit){
            switch (strategy) {
                case DISABLE: {
                    setDisable(true);
                    break;
                }
                case HIDE: {
                    setVisible(false);
                    break;
                }
                default:
                    //do nothing for ERROR_ON_ACTION, check will be performed in runtime
                    break;
            }
        }

        setOnAction(action);
    }

    /** */
    private void initGraphic(IAction action) {

//        if( action.getIcon() == null )
//        {
//            if( S.isNullOrEmpty( action.getTitle() ) )
//                setText( action.getTitle() );
//            return;
//        }

        if (BaseApp.APP() != null && BaseApp.APP().isAfterLogin()) {

            try {

                final IBaseIconDescriptor icon = action.getIcon();

                Node graphicNode = action.getIcon() == null ? null : ActionFactory.getLabel( icon );

                ButtonIconView view = BaseApp.APP().getViewPrefService().getButtonIconView();

                switch (view) {
                    case ICON: {
                        if( graphicNode != null && icon != emptyIcon )
                            setGraphic( graphicNode );
                        else
                            if( S.isNotNullOrEmpty( action.getTitle() ) )
                                setText( action.getTitle());//setGraphic( new Label(action.getTitle()) );
                        break;
                    }
                    case ICON_WITH_TEXT:
                    {
                        HBox box = new HBox();
                        box.setSpacing(5);
                        box.setFillHeight(true);
                        if (graphicNode != null) {
                            box.getChildren().add(graphicNode);
                        }
                        if (S.isNotNullOrEmpty(action.getTitle())) {
                            box.getChildren().add(new Label(action.getTitle()));
                        }
                        setGraphic(box);
                    }
                    break;
                    case TEXT: {
                        if( S.isNotNullOrEmpty(action.getTitle()) ) {
                            setGraphic(new Label(action.getTitle()));
                        }
                    }
                    break;
                }

            } catch (AppException ex) {
                throw new RuntimeException(ex);
            }
        }
    }

    @Override
    public <T> DSFXAdapter<T> getDataSetAdapter() {

        return Controls.<T>getDsAdapterFromControl(this);
    }

    @Override
    public void setToolTipText(String toolTipText) {
        if (toolTipText != null) {

            if (getTooltip() != null) {
                getTooltip().setText(toolTipText);
            } else {
                setTooltip(new Tooltip(toolTipText));
            }
        }
    }

    @Override
    public String getToolTipText( ) {
        return IJInvControl.super.getToolTipText();
    }

/*
    @Override
    public Optional<String> getToolTipText() {
        if (getTooltip() != null && !getTooltip().getText().isEmpty()) {
            return Optional.ofNullable(getTooltip().getText());
        } else {
            return Optional.empty();
        }
    }
*/
    @Override
    public StringProperty labelTextProperty() {
        return textLabelProperty;
    }

    /**
     * Возвращает fx:id привязанного к кнопке текстового поля
     *
     * @return
     */
    public String getIdTextField() {
        return (String) getProperties().getOrDefault(PROPERTY_ID_TEXTFIELD, null);
    }

    /**
     * Устанавливает fx:id текстового поля, которое будет автоматически связано с текущей кнопкой
     */
    public void setIdTextField(String idTextField) {
        getProperties().put(PROPERTY_ID_TEXTFIELD, idTextField);
    }

    /**
     * Устанавливает текстовое поле
     *
     * @param ed
     */
    public void setTextField(TextInputControl ed) {
        if (ed != null) {

            getProperties().put(PROPERTY_TEXTFIELD, ed);

            if (getBindBlockFromTextField()) {
                disableProperty().bind(ed.editableProperty().not());
            }

            if (ed instanceof JInvTextField) {

                JInvTextField field = (JInvTextField) ed;

                setToolTipText(field.getButtonToolTipText());

                JInvButton parentButton = (JInvButton) getProperties().getOrDefault(Controls.CONTROL_PARENT, null);
                if ((parentButton == null || !(parentButton instanceof JInvWrapButton))
                        && this.getParent() != null //не учитываем кнопки, созданные кодом и никуда не пристроенные
                ) {
                    field.externalButtonProperty().set(this);
                }

                if (!field.getButtonIgnoresEditable()) {
                    setDisable(!field.isEditable());
                }
            }

        }
    }

    /**
     * Возвращает текстовое поле
     */
    public TextInputControl getTextField() {
        return (TextInputControl) getProperties().getOrDefault(PROPERTY_TEXTFIELD, null);
    }

    /**
     * Возвращает признак отвечающий за согласное дизаблирование кнопки совместно с привязанным текстовым полем
     *
     * @return
     */
    public Boolean getBindBlockFromTextField() {
        return (Boolean) getProperties().getOrDefault(PROPERTY_BLOCK_BY_TEXTFIELD, Boolean.FALSE);
    }

    /**
     * Устанавливает признак отвечающий за согласное дизаблирование кнопки совместно с привязанным текстовым полем
     *
     * @param bindBlockFromTextField
     */
    public void setBindBlockFromTextField(Boolean bindBlockFromTextField) {
        getProperties().put(PROPERTY_BLOCK_BY_TEXTFIELD, bindBlockFromTextField);
    }

    /**
     * @return
     */
    public Integer getSecurityId() {
        return (Integer) getProperties().getOrDefault(PROPERTY_SECURITY_ID, null);
    }

    /**
     */
    public void setSecurityId(Integer id) {
        getProperties().put(PROPERTY_SECURITY_ID, id);
    }
}
