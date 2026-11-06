package ru.inversion.fx.form;

import javafx.beans.property.SimpleStringProperty;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBase;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import ru.inversion.fx.app.AppException;
import ru.inversion.fx.app.BaseApp;
import ru.inversion.fx.app.sec.SecurityStrategyEnum;
import ru.inversion.fx.form.action.ActionBuilder;
import ru.inversion.fx.form.action.IAction;
import ru.inversion.fx.form.controls.JInvButton;
import ru.inversion.icons.IBaseIconDescriptor;
import ru.inversion.icons.IconDescriptor;
import ru.inversion.icons.IconDescriptorBuilder;
import ru.inversion.icons.IconFactory;
import ru.inversion.icons.enums.FontAwesome;
import ru.inversion.icons.enums.MaterialDesign;
import ru.inversion.icons.utils.EnumUtils;
import ru.inversion.tc.TaskContext;
import ru.inversion.utils.S;

import java.util.Arrays;
import java.util.List;
import java.util.ResourceBundle;
import java.util.concurrent.Callable;

/**
 * Фабрика стандартных компонентов
 *
 * @author ssu @
 */
public class ActionFactory {

    final private static ResourceBundle bundle = ResourceBundle.getBundle("actions");

    private static final String CSS_CLASS_BUTTON_DEFAULT = "button_default";
    private static final String CSS_CLASS_BUTTON_INVERSION = "button_inversion";

    @Deprecated
    final public static String defaultButtonStyle = "-fx-font-family:'FontAwesome';-fx-font-size:1.16666em;-fx-margin-right:1;"; //-fx-text-fill:#1A66AC;
    @Deprecated
    final public static String inversinButtonStyle = "-fx-font-family:'Font-Inversion';-fx-font-size:1.16666em;-fx-margin-right:1;";


    public enum ActionTypeEnum {

        CREATE( new IconDescriptorBuilder<>( FontAwesome.fa_file_o, null ).build(),
            new KeyCodeCombination(KeyCode.F2), new KeyCodeCombination(KeyCode.F6)),
        CREATE_BY( new IconDescriptorBuilder<>( FontAwesome.fa_file_text_o, null ).build(),
            new KeyCodeCombination(KeyCode.F2, KeyCombination.ALT_DOWN),
            new KeyCodeCombination(KeyCode.F6, KeyCombination.ALT_DOWN) ),
        UPDATE( new IconDescriptorBuilder<>( FontAwesome.fa_edit, null ).build(),
            new KeyCodeCombination(KeyCode.F4)),
        DELETE( new IconDescriptorBuilder<>( FontAwesome.fa_close, "fmx:ap_del" ).build(),
            new KeyCodeCombination(KeyCode.F6, KeyCombination.SHIFT_DOWN) ),
        BIND(   new IconDescriptorBuilder<>( FontAwesome.empty, null ).build() ),
        UNBIND( new IconDescriptorBuilder<>( FontAwesome.empty, null ).build() ),
        REFRESH(new IconDescriptorBuilder<>( FontAwesome.fa_refresh, null ).build() ),
        ACCEPT( new IconDescriptorBuilder<>( FontAwesome.fa_check, null ).build() ),
        STATUS( new IconDescriptorBuilder<>( FontAwesome.empty, null ).build() ),
        UP(     new IconDescriptorBuilder<>( FontAwesome.fa_chevron_up, null ).build() ),
        DOWN(   new IconDescriptorBuilder<>( FontAwesome.fa_chevron_down, null ).build() ),
        STOP(   new IconDescriptorBuilder<>( FontAwesome.fa_stop, null ).build() ),
        EXPAND( new IconDescriptorBuilder<>( FontAwesome.fa_expand, null ).build() ),
        COLLAPSE( new IconDescriptorBuilder<>( FontAwesome.fa_compress, null ).build() ),
        PRINT(  new IconDescriptorBuilder<>( FontAwesome.fa_print, null ).build() ),
        ALT_PRINT( new IconDescriptorBuilder<>( FontAwesome.fa_print, null ).build() ),
        ALT_PRINT_LIST ( new IconDescriptorBuilder<>( FontAwesome.fa_print, null ).build() ),
        CLEAR ( new IconDescriptorBuilder<>( FontAwesome.fa_eraser, null ).build(),
            new KeyCodeCombination(KeyCode.F5, KeyCombination.SHIFT_DOWN) ),
        SEARCH( new IconDescriptorBuilder<>( FontAwesome.fa_search, null ).build() ),
        SAVE_FILE( new IconDescriptorBuilder<>( FontAwesome.fa_floppy_o, null ).build() ),
        OPEN_FILE( new IconDescriptorBuilder<>( FontAwesome.fa_folder_open_o, null ).build() ),
        RUN(    new IconDescriptorBuilder<>( FontAwesome.fa_play, null ).build() ),
        EXIT(   new IconDescriptorBuilder<>( FontAwesome.fa_sign_out, null ).build() ),
        SETTINGS( new IconDescriptorBuilder<>( FontAwesome.fa_cog, null ).build() ),
        RESTORE( new IconDescriptorBuilder<>( FontAwesome.empty, null ).build() ),
        LEFT(   new IconDescriptorBuilder<>( FontAwesome.fa_chevron_left, null ).build() ),
        FILTER_EXEC( new IconDescriptorBuilder<>( FontAwesome.fa_bolt, "fmx:f_copy" ).build() ),
        MARK(   new IconDescriptorBuilder<>( FontAwesome.fa_check_square_o, "fmx:f_mark" ).build() ),
        RIGHT(  new IconDescriptorBuilder<>( FontAwesome.fa_chevron_right, null ).build() ),
        VIEW(   new IconDescriptorBuilder<>( FontAwesome.fa_info_circle, null ).build(),
            new KeyCodeCombination(KeyCode.F3)),
        SELECT_FOLDER( new IconDescriptorBuilder<>( FontAwesome.fa_folder_open_o, null ).build() ),
        CHOOSE_DIRECTORY( new IconDescriptorBuilder<>( FontAwesome.fa_folder_open_o, null ).build() ), // аналог SELECT_FOLDER
        EMAIL(  new IconDescriptorBuilder<>( FontAwesome.fa_envelope_o, null ).build() ),
        DETAILS( new IconDescriptorBuilder<>( FontAwesome.fa_bars, null ).build() ),
        ADD(    new IconDescriptorBuilder<>( FontAwesome.fa_plus, null ).build() ),
        EDIT_EXTERNAL ( new IconDescriptorBuilder<>( FontAwesome.fa_edit, null ).build(),
            new KeyCodeCombination(KeyCode.F4) ),
        FE ( new IconDescriptorBuilder<>( FontAwesome.fa_ellipsis_h, null ).build() ),
        BOOK_PEN( new IconDescriptorBuilder<>( FontAwesome.fa_list, "fmx:book_pen" ).build(),
            new KeyCodeCombination(KeyCode.R, KeyCombination.ALT_DOWN)), //Вызов формы просмотра реестра операций по доступным счетам -- book_pen
        OP_USER( new IconDescriptorBuilder<>( FontAwesome.fa_smile_o, "fmx:op_user3" ).build(),
            new KeyCodeCombination(KeyCode.U, KeyCombination.ALT_DOWN)), //Вызов формы просмотра реестра операций пользователя -- op_user3
        CLIENT_INFO( new IconDescriptorBuilder<>( FontAwesome.fa_file_image_o, "fmx:image" ).build(),
            new KeyCodeCombination(KeyCode.K, KeyCombination.ALT_DOWN)), //Карточка образцов подписей
        ACC_INFO( new IconDescriptorBuilder<>( FontAwesome.fa_info, "fmx:buc_hlp" ).build(),
            new KeyCodeCombination(KeyCode.O, KeyCombination.ALT_DOWN)), //Информация по счёту -- buc_hlp
        ACC_EXTRACT( new IconDescriptorBuilder<>( FontAwesome.fa_dollar, "fmx:bal" ).build(),
            new KeyCodeCombination(KeyCode.T, KeyCombination.ALT_DOWN) ), //Получение выписки по лицевому счету -- bal
        PRINT_ALT(null), //Альтернативная печать документа
        PRINT_ALT_LIST(null), //Альтернативная печать списка
        CHANGE_FILIAL( new IconDescriptorBuilder<>( FontAwesome.fa_bank, null ).build() ), // Смена текущего филиала
        DOWNLOAD  ( new IconDescriptorBuilder<>( FontAwesome.fa_download, null ).build() ),
        IMPORT    ( new IconDescriptorBuilder<>( FontAwesome.fa_download, null).build() ),
        EXPORT    ( new IconDescriptorBuilder<>( FontAwesome.fa_upload, null).build() ),
        PAUSE     ( new IconDescriptorBuilder<>( FontAwesome.fa_pause, null ).build() ),
        PLAY      ( new IconDescriptorBuilder<>( FontAwesome.fa_play, null ).build() ),
        PLAY_CIRLE( new IconDescriptorBuilder<>( FontAwesome.fa_play_circle, null ).build() ),
        PAPER_CLIP( new IconDescriptorBuilder<>( FontAwesome.fa_paperclip, null ).build() ),
        COPY      ( new IconDescriptorBuilder<>( FontAwesome.fa_copy, null ).build(),
            new KeyCodeCombination( KeyCode.C, KeyCombination.CONTROL_DOWN ) ),
        CLONE     ( new IconDescriptorBuilder<>( FontAwesome.fa_clone, null ).build() ),
        FILTER    ( new IconDescriptorBuilder<>( FontAwesome.fa_filter, null ).build() ),
        CONFIG    ( new IconDescriptorBuilder<>( FontAwesome.fa_wrench, null ).build() ),
        AUDIT    ( new IconDescriptorBuilder<>( FontAwesome.fa_user_secret, null ).build() )
        ;

        final private IAction action;

        ActionTypeEnum(
                IBaseIconDescriptor icon,
                KeyCodeCombination... keys) {

            String bk = this.name(), title = name(), toolTip = null;

            if (bundle.containsKey(bk)) {
                title = bundle.getString(bk);
            }
            if (bundle.containsKey(bk + "_TOOLTIP")) {
                toolTip = bundle.getString(bk + "_TOOLTIP");
            }

            ActionBuilder builder = new ActionBuilder()
                    .setTitle(title)
                    .setToolTip(toolTip)
                    .icon(icon).setActionType(this);
            if(keys!=null){
                builder.setListKeyCombination(Arrays.<KeyCodeCombination>asList(keys));
            }

            action  = builder.build();
        }

        /**
         *
         */
        public IAction getAction() {
            return action;
        }

        public List<KeyCodeCombination> getHotKey() {
            return getAction().getHotKey();
        }

        /**
         *
         */
        public String getName() {
            return getAction().getTitle();
        }

        /**
         *
         */
        public String getTooltip() {
            return getAction().getToolTip();
        }

        /**
         *
         */
        public String getIconFontCode() {
            return ((FontAwesome) ((IconDescriptor) getAction().getIcon()).getIconID()).name();
        }
    }

    /**
     * Получить нужную иконку у шрифта
     *
     * @param actionFontCode
     * @return
     */
    @Deprecated
    public static Label getLabel(String actionFontCode) {
        SimpleStringProperty d;
        if (S.isNotNullOrEmpty(actionFontCode) && !actionFontCode.startsWith("fa")) {
            FontAwesome font = EnumUtils.<FontAwesome>getEnumByCode(FontAwesome.class, actionFontCode);
            if (font != null) {
                return IconFactory.getLabel(font);
            }
        } else {
            return IconFactory.getLabel("fa:" + actionFontCode);
        }
        return null;
    }

    @Deprecated
    public static Label getLabel(IconEnum iconCode) {

        if (iconCode != null) {
            return IconFactory.getLabel( iconCode.getNewEnum() );
        } else {
            return null;
        }
    }

    /**
     *
     */
    @Deprecated
    public static Button createButton(String actionFontCode, EventHandler<ActionEvent> eventHandler) {

        Button button = new JInvButton(null, getLabel(actionFontCode));

        if (eventHandler != null) {
            button.setOnAction(eventHandler);
        }

        button.setFocusTraversable(false);
        return button;
    }

    /**
     *
     */
    @Deprecated
    public static Button createButton(String actionFontCode, EventHandler<ActionEvent> eventHandler, String toolTipText) {

        Button button = createButton(actionFontCode, eventHandler);

        if (toolTipText != null && !toolTipText.isEmpty()) {
            button.setTooltip(new Tooltip(toolTipText));
        }
        button.setFocusTraversable(false);
        return button;
    }

    /**
     * @param iconCode
     * @param eventHandler
     * @return
     */
    @Deprecated
    public static Button createButton(IconEnum iconCode, EventHandler<ActionEvent> eventHandler) {

        return createButton(iconCode, eventHandler, null, false);
    }

    /**
     * @param iconCode
     * @param eventHandler
     * @param toolTipText
     * @return
     */
    @Deprecated
    public static Button createButton(IconEnum iconCode, EventHandler<ActionEvent> eventHandler, String toolTipText) {

        return createButton(iconCode, eventHandler, toolTipText, false);
    }

    @Deprecated
    public static Button createButton(IconEnum iconCode, EventHandler<ActionEvent> eventHandler, String toolTipText, boolean parallel) {

        return createButton(iconCode, eventHandler, toolTipText, parallel, null, null);
    }

    /**
     *
     */
    @Deprecated
    public static Button createButton(ActionTypeEnum actionType, EventHandler<ActionEvent> eventHandler, boolean parallel) {

        if (actionType != null) {
            return createButton(actionType, eventHandler, parallel, null, null);
        } else {
            return null;
        }
    }

    @Deprecated
    public static Button createButton(IconEnum iconCode, EventHandler<ActionEvent> eventHandler, String toolTipText, boolean parallel, Integer id, SecurityStrategyEnum strategy) {

        return createButtonInternal(null, iconCode, null, toolTipText, eventHandler, parallel, id, strategy);
    }

    /**
     *
     */
    @Deprecated
    public static Button createButton(ActionTypeEnum actionType, EventHandler<ActionEvent> eventHandler, boolean parallel, Integer id, SecurityStrategyEnum strategy) {

        if (actionType != null) {
            return createButtonInternal(actionType, getOldEnumFromActionTypeEnum(actionType), actionType.getName(), actionType.getTooltip(), eventHandler, parallel, id, strategy);
        } else {
            return null;
        }
    }

    private static Button createButtonInternal(
            ActionTypeEnum actionType,
            IconEnum iconCode,
            String name,
            String toolTipText,
            EventHandler<ActionEvent> eventHandler,
            boolean parallel,
            Integer id,
            SecurityStrategyEnum strategy) {

        if (iconCode != null) {
            return (Button) createButton(actionType, new IconDescriptorBuilder<>( iconCode.getNewEnum(), null ).build(),
                    name, toolTipText, eventHandler, parallel, id, strategy);
        } else {
            return (Button) createButton(actionType,
                    null, name, toolTipText, eventHandler, parallel, id, strategy);
        }
    }

    /**
     *
     */
    @Deprecated
    public static Button createLOButton(ActionTypeEnum actionType, Callable cl, TaskContext tc, ViewContext vc) {

        Button bt = createButton(actionType, null, false);

        if (cl != null) {

        }

        bt.setText("NOT IMPL");

        return bt;
    }

    /**
     *
     */
    @Deprecated
    public static ButtonBase assignButtonStyle(String actionFontCode, ButtonBase button) {

        if (button != null && !S.isNullOrEmpty(actionFontCode)) {
            button.setGraphic(getLabel(actionFontCode));
        }
        return button;
    }

    @Deprecated
    public static Button assignButtonStyle(String actionFontCode, Button button) {
        return (Button) assignButtonStyle(actionFontCode, (ButtonBase) button);
    }

    /**
     *
     */
    @Deprecated
    public static ButtonBase assignButtonStyleFromString(String iconCodeString, String name, ButtonBase button) {

        try {
            IconEnum icon = IconEnum.valueOf(iconCodeString);
            assignButtonStyle(button, icon, name);
        } catch (Throwable ex) {
            throw new RuntimeException(BaseApp.APP().getCommonResourceBundle().getString("ERROR_ICON_CODE"));
        }

        return button;
    }

    @Deprecated
    public static Button assignButtonStyleFromString(String iconCodeString, String name, Button button) {
        return (Button) assignButtonStyleFromString(iconCodeString, name, (ButtonBase) button);
    }

    /**
     *
     */
    @Deprecated
    public static ButtonBase assignButtonStyle(IconEnum iconCode, ButtonBase button) {
        assignButtonStyle(button, iconCode, iconCode.toString());
        return button;
    }

    @Deprecated
    public static Button assignButtonStyle(IconEnum iconCode, Button button) {
        return (Button) assignButtonStyle(iconCode, (ButtonBase) button);
    }

    private static IconEnum getOldEnumFromActionTypeEnum(ActionTypeEnum type) {

        if (type != null) {
            try {
                return IconEnum.valueOf(((FontAwesome) (((IconDescriptor) type.getAction().getIcon()).getIconID())).name());
            } catch (Throwable ex) {
                return null;
            }
        } else {
            return null;
        }
    }

    /**
     *
     */
    @Deprecated
    public static ButtonBase assignButtonStyle(ActionTypeEnum actionType, ButtonBase button) {

        if (actionType != null) {
            assignButtonStyle(button, getOldEnumFromActionTypeEnum(actionType), actionType.getName());
        }
        return button;
    }

    @Deprecated
    public static Button assignButtonStyle(ActionTypeEnum actionType, Button button) {
        return (Button) assignButtonStyle(actionType, (ButtonBase) button);
    }

    /**
     * Декорация кнопки без обращения к базе данных
     * <p>
     *
     * @param actionType
     * @param button
     * @return
     */
    @Deprecated
    public static ButtonBase assignButtonStyleSilent(ActionTypeEnum actionType, ButtonBase button) {

        if (actionType != null && button != null) {
            button.setGraphic(getLabel(getOldEnumFromActionTypeEnum(actionType)));
        }

        return button;
    }

    @Deprecated
    public static Button assignButtonStyleSilent(ActionTypeEnum actionType, Button button) {
        return (Button) assignButtonStyleSilent(actionType, (ButtonBase) button);
    }

    /**
     * Задача метода
     *
     * @param button
     * @param iconName
     * @param name
     */
    @Deprecated
    public static ButtonBase assignButtonStyle(ButtonBase button, IconEnum iconName, String name) {

        if (iconName != null) {
            return initButton(button,
                    new ActionBuilder().icon(new IconDescriptorBuilder(iconName.getNewEnum(), null).build()).setTitle(name).build());
        } else {
            return initButton(button,
                    new ActionBuilder().setTitle(name).build());
        }
    }

    public static Button assignButtonStyle(Button button, IconEnum iconName, String name) {
        return (Button) assignButtonStyle((ButtonBase) button, iconName, name);
    }

    /**
     @deprecated использовать new ActionBuilder().setActionType( type ).handler( handler ).setParallel( parallel )
     .build()
     */
    @Deprecated
    public static IAction getAction(ActionTypeEnum type, EventHandler<ActionEvent> handler, boolean parallel) {
        return getAction(type, handler, parallel, null, null);
    }

    /**
     * @param type
     * @param handler
     * @param parallel
     * @param id
     * @param strategy
     * @return
     */
    @Deprecated
    public static IAction getAction(ActionTypeEnum type, EventHandler<ActionEvent> handler, boolean parallel, Integer id, SecurityStrategyEnum strategy) {

        ActionTypeEnum typical = null;
        if (type != null) {
            typical = type;
        }

        if (type == null) {

            if (parallel) {
                return createParallelAction(typical,  null, null,
                        null, handler, null, id, strategy);
            } else {
                return createAction(typical,  null, null,
                        null, handler, null, id, strategy);
            }
        } else if (parallel) {

            return createParallelAction(typical,  null, null,
                    null, handler, type.getHotKey(), id, strategy);
        } else {
            return createAction(typical, null,  null,
                    null, handler, type.getHotKey(), id, strategy);
        }
    }

    @Deprecated
    public static IAction getAction(EventHandler<ActionEvent> handler) {
        return createAction(null, handler);
    }

    @Deprecated
    public enum IconEnum {
        /*
        empty(""),
        fa_500px("\uf26e"),
        fa_adjust("\uf042"),
        fa_adn("\uf170"),
        fa_align_center("\uf037"),
        fa_align_justify("\uf039"),
        fa_align_left("\uf036"),
        fa_align_right("\uf038"),
        fa_amazon("\uf270"),
        fa_ambulance("\uf0f9"),
        fa_anchor("\uf13d"),
        fa_android("\uf17b"),
        fa_angellist("\uf209"),
        fa_angle_double_down("\uf103"),
        fa_angle_double_left("\uf100"),
        fa_angle_double_right("\uf101"),
        fa_angle_double_up("\uf102"),
        fa_angle_down("\uf107"),
        fa_angle_left("\uf104"),
        fa_angle_right("\uf105"),
        fa_angle_up("\uf106"),
        fa_apple("\uf179"),
        fa_archive("\uf187"),
        fa_area_chart("\uf1fe"),
        fa_arrow_circle_down("\uf0ab"),
        fa_arrow_circle_left("\uf0a8"),
        fa_arrow_circle_o_down("\uf01a"),
        fa_arrow_circle_o_left("\uf190"),
        fa_arrow_circle_o_right("\uf18e"),
        fa_arrow_circle_o_up("\uf01b"),
        fa_arrow_circle_right("\uf0a9"),
        fa_arrow_circle_up("\uf0aa"),
        fa_arrow_down("\uf063"),
        fa_arrow_left("\uf060"),
        fa_arrow_right("\uf061"),
        fa_arrow_up("\uf062"),
        fa_arrows("\uf047"),
        fa_arrows_alt("\uf0b2"),
        fa_arrows_h("\uf07e"),
        fa_arrows_v("\uf07d"),
        fa_asterisk("\uf069"),
        fa_at("\uf1fa"),
        fa_automobile("\uf1b9"),
        fa_backward("\uf04a"),
        fa_balance_scale("\uf24e"),
        fa_ban("\uf05e"),
        fa_bank("\uf19c"),
        fa_bar_chart("\uf080"),
        fa_bar_chart_o("\uf080"),
        fa_barcode("\uf02a"),
        fa_bars("\uf0c9"),
        fa_battery_0("\uf244"),
        fa_battery_1("\uf243"),
        fa_battery_2("\uf242"),
        fa_battery_3("\uf241"),
        fa_battery_4("\uf240"),
        fa_battery_empty("\uf244"),
        fa_battery_full("\uf240"),
        fa_battery_half("\uf242"),
        fa_battery_quarter("\uf243"),
        fa_battery_three_quarters("\uf241"),
        fa_bed("\uf236"),
        fa_beer("\uf0fc"),
        fa_behance("\uf1b4"),
        fa_behance_square("\uf1b5"),
        fa_bell("\uf0f3"),
        fa_bell_o("\uf0a2"),
        fa_bell_slash("\uf1f6"),
        fa_bell_slash_o("\uf1f7"),
        fa_bicycle("\uf206"),
        fa_binoculars("\uf1e5"),
        fa_birthday_cake("\uf1fd"),
        fa_bitbucket("\uf171"),
        fa_bitbucket_square("\uf172"),
        fa_bitcoin("\uf15a"),
        fa_black_tie("\uf27e"),
        fa_bluetooth("\uf293"),
        fa_bluetooth_b("\uf294"),
        fa_bold("\uf032"),
        fa_bolt("\uf0e7"),
        fa_bomb("\uf1e2"),
        fa_book("\uf02d"),
        fa_bookmark("\uf02e"),
        fa_bookmark_o("\uf097"),
        fa_briefcase("\uf0b1"),
        fa_btc("\uf15a"),
        fa_bug("\uf188"),
        fa_building("\uf1ad"),
        fa_building_o("\uf0f7"),
        fa_bullhorn("\uf0a1"),
        fa_bullseye("\uf140"),
        fa_bus("\uf207"),
        fa_buysellads("\uf20d"),
        fa_cab("\uf1ba"),
        fa_calculator("\uf1ec"),
        fa_calendar("\uf073"),
        fa_calendar_check_o("\uf274"),
        fa_calendar_minus_o("\uf272"),
        fa_calendar_o("\uf133"),
        fa_calendar_plus_o("\uf271"),
        fa_calendar_times_o("\uf273"),
        fa_camera("\uf030"),
        fa_camera_retro("\uf083"),
        fa_car("\uf1b9"),
        fa_caret_down("\uf0d7"),
        fa_caret_left("\uf0d9"),
        fa_caret_right("\uf0da"),
        fa_caret_square_o_down("\uf150"),
        fa_caret_square_o_left("\uf191"),
        fa_caret_square_o_right("\uf152"),
        fa_caret_square_o_up("\uf151"),
        fa_caret_up("\uf0d8"),
        fa_cart_arrow_down("\uf218"),
        fa_cart_plus("\uf217"),
        fa_cc("\uf20a"),
        fa_cc_amex("\uf1f3"),
        fa_cc_diners_club("\uf24c"),
        fa_cc_discover("\uf1f2"),
        fa_cc_jcb("\uf24b"),
        fa_cc_mastercard("\uf1f1"),
        fa_cc_paypal("\uf1f4"),
        fa_cc_stripe("\uf1f5"),
        fa_cc_visa("\uf1f0"),
        fa_certificate("\uf0a3"),
        fa_chain("\uf0c1"),
        fa_chain_broken("\uf127"),
        fa_check("\uf00c"),
        fa_check_circle("\uf058"),
        fa_check_circle_o("\uf05d"),
        fa_check_square("\uf14a"),
        fa_check_square_o("\uf046"),
        fa_chevron_circle_down("\uf13a"),
        fa_chevron_circle_left("\uf137"),
        fa_chevron_circle_right("\uf138"),
        fa_chevron_circle_up("\uf139"),
        fa_chevron_down("\uf078"),
        fa_chevron_left("\uf053"),
        fa_chevron_right("\uf054"),
        fa_chevron_up("\uf077"),
        fa_child("\uf1ae"),
        fa_chrome("\uf268"),
        fa_circle("\uf111"),
        fa_circle_o("\uf10c"),
        fa_circle_o_notch("\uf1ce"),
        fa_circle_thin("\uf1db"),
        fa_clipboard("\uf0ea"),
        fa_clock_o("\uf017"),
        fa_clone("\uf24d"),
        fa_close("\uf00d"),
        fa_cloud("\uf0c2"),
        fa_cloud_download("\uf0ed"),
        fa_cloud_upload("\uf0ee"),
        fa_cny("\uf157"),
        fa_code("\uf121"),
        fa_code_fork("\uf126"),
        fa_codepen("\uf1cb"),
        fa_codiepie("\uf284"),
        fa_coffee("\uf0f4"),
        fa_cog("\uf013"),
        fa_cogs("\uf085"),
        fa_columns("\uf0db"),
        fa_comment("\uf075"),
        fa_comment_o("\uf0e5"),
        fa_commenting("\uf27a"),
        fa_commenting_o("\uf27b"),
        fa_comments("\uf086"),
        fa_comments_o("\uf0e6"),
        fa_compass("\uf14e"),
        fa_compress("\uf066"),
        fa_connectdevelop("\uf20e"),
        fa_contao("\uf26d"),
        fa_copy("\uf0c5"),
        fa_copyright("\uf1f9"),
        fa_creative_commons("\uf25e"),
        fa_credit_card("\uf09d"),
        fa_credit_card_alt("\uf283"),
        fa_crop("\uf125"),
        fa_crosshairs("\uf05b"),
        fa_css3("\uf13c"),
        fa_cube("\uf1b2"),
        fa_cubes("\uf1b3"),
        fa_cut("\uf0c4"),
        fa_cutlery("\uf0f5"),
        fa_dashboard("\uf0e4"),
        fa_dashcube("\uf210"),
        fa_database("\uf1c0"),
        fa_dedent("\uf03b"),
        fa_delicious("\uf1a5"),
        fa_desktop("\uf108"),
        fa_deviantart("\uf1bd"),
        fa_diamond("\uf219"),
        fa_digg("\uf1a6"),
        fa_dollar("\uf155"),
        fa_dot_circle_o("\uf192"),
        fa_download("\uf019"),
        fa_dribbble("\uf17d"),
        fa_dropbox("\uf16b"),
        fa_drupal("\uf1a9"),
        fa_edge("\uf282"),
        fa_edit("\uf044"),
        fa_eject("\uf052"),
        fa_ellipsis_h("\uf141"),
        fa_ellipsis_v("\uf142"),
        fa_empire("\uf1d1"),
        fa_envelope("\uf0e0"),
        fa_envelope_o("\uf003"),
        fa_envelope_square("\uf199"),
        fa_eraser("\uf12d"),
        fa_eur("\uf153"),
        fa_euro("\uf153"),
        fa_exchange("\uf0ec"),
        fa_exclamation("\uf12a"),
        fa_exclamation_circle("\uf06a"),
        fa_exclamation_triangle("\uf071"),
        fa_expand("\uf065"),
        fa_expeditedssl("\uf23e"),
        fa_external_link("\uf08e"),
        fa_external_link_square("\uf14c"),
        fa_eye("\uf06e"),
        fa_eye_slash("\uf070"),
        fa_eyedropper("\uf1fb"),
        fa_facebook("\uf09a"),
        fa_facebook_f("\uf09a"),
        fa_facebook_official("\uf230"),
        fa_facebook_square("\uf082"),
        fa_fast_backward("\uf049"),
        fa_fast_forward("\uf050"),
        fa_fax("\uf1ac"),
        fa_feed("\uf09e"),
        fa_female("\uf182"),
        fa_fighter_jet("\uf0fb"),
        fa_file("\uf15b"),
        fa_file_archive_o("\uf1c6"),
        fa_file_audio_o("\uf1c7"),
        fa_file_code_o("\uf1c9"),
        fa_file_excel_o("\uf1c3"),
        fa_file_image_o("\uf1c5"),
        fa_file_movie_o("\uf1c8"),
        fa_file_o("\uf016"),
        fa_file_pdf_o("\uf1c1"),
        fa_file_photo_o("\uf1c5"),
        fa_file_picture_o("\uf1c5"),
        fa_file_powerpoint_o("\uf1c4"),
        fa_file_sound_o("\uf1c7"),
        fa_file_text("\uf15c"),
        fa_file_text_o("\uf0f6"),
        fa_file_video_o("\uf1c8"),
        fa_file_word_o("\uf1c2"),
        fa_file_zip_o("\uf1c6"),
        fa_files_o("\uf0c5"),
        fa_film("\uf008"),
        fa_filter("\uf0b0"),
        fa_fire("\uf06d"),
        fa_fire_extinguisher("\uf134"),
        fa_firefox("\uf269"),
        fa_flag("\uf024"),
        fa_flag_checkered("\uf11e"),
        fa_flag_o("\uf11d"),
        fa_flash("\uf0e7"),
        fa_flask("\uf0c3"),
        fa_flickr("\uf16e"),
        fa_floppy_o("\uf0c7"),
        fa_folder("\uf07b"),
        fa_folder_o("\uf114"),
        fa_folder_open("\uf07c"),
        fa_folder_open_o("\uf115"),
        fa_font("\uf031"),
        fa_fonticons("\uf280"),
        fa_fort_awesome("\uf286"),
        fa_forumbee("\uf211"),
        fa_forward("\uf04e"),
        fa_foursquare("\uf180"),
        fa_frown_o("\uf119"),
        fa_futbol_o("\uf1e3"),
        fa_gamepad("\uf11b"),
        fa_gavel("\uf0e3"),
        fa_gbp("\uf154"),
        fa_ge("\uf1d1"),
        fa_gear("\uf013"),
        fa_gears("\uf085"),
        fa_genderless("\uf22d"),
        fa_get_pocket("\uf265"),
        fa_gg("\uf260"),
        fa_gg_circle("\uf261"),
        fa_gift("\uf06b"),
        fa_git("\uf1d3"),
        fa_git_square("\uf1d2"),
        fa_github("\uf09b"),
        fa_github_alt("\uf113"),
        fa_github_square("\uf092"),
        fa_gittip("\uf184"),
        fa_glass("\uf000"),
        fa_globe("\uf0ac"),
        fa_google("\uf1a0"),
        fa_google_plus("\uf0d5"),
        fa_google_plus_square("\uf0d4"),
        fa_google_wallet("\uf1ee"),
        fa_graduation_cap("\uf19d"),
        fa_gratipay("\uf184"),
        fa_group("\uf0c0"),
        fa_h_square("\uf0fd"),
        fa_hacker_news("\uf1d4"),
        fa_hand_grab_o("\uf255"),
        fa_hand_lizard_o("\uf258"),
        fa_hand_o_down("\uf0a7"),
        fa_hand_o_left("\uf0a5"),
        fa_hand_o_right("\uf0a4"),
        fa_hand_o_up("\uf0a6"),
        fa_hand_paper_o("\uf256"),
        fa_hand_peace_o("\uf25b"),
        fa_hand_pointer_o("\uf25a"),
        fa_hand_rock_o("\uf255"),
        fa_hand_scissors_o("\uf257"),
        fa_hand_spock_o("\uf259"),
        fa_hand_stop_o("\uf256"),
        fa_hashtag("\uf292"),
        fa_hdd_o("\uf0a0"),
        fa_header("\uf1dc"),
        fa_headphones("\uf025"),
        fa_heart("\uf004"),
        fa_heart_o("\uf08a"),
        fa_heartbeat("\uf21e"),
        fa_history("\uf1da"),
        fa_home("\uf015"),
        fa_hospital_o("\uf0f8"),
        fa_hotel("\uf236"),
        fa_hourglass("\uf254"),
        fa_hourglass_1("\uf251"),
        fa_hourglass_2("\uf252"),
        fa_hourglass_3("\uf253"),
        fa_hourglass_end("\uf253"),
        fa_hourglass_half("\uf252"),
        fa_hourglass_o("\uf250"),
        fa_hourglass_start("\uf251"),
        fa_houzz("\uf27c"),
        fa_html5("\uf13b"),
        fa_i_cursor("\uf246"),
        fa_ils("\uf20b"),
        fa_image("\uf03e"),
        fa_inbox("\uf01c"),
        fa_indent("\uf03c"),
        fa_industry("\uf275"),
        fa_info("\uf129"),
        fa_info_circle("\uf05a"),
        fa_inr("\uf156"),
        fa_instagram("\uf16d"),
        fa_institution("\uf19c"),
        fa_internet_explorer("\uf26b"),
        fa_intersex("\uf224"),
        fa_ioxhost("\uf208"),
        fa_italic("\uf033"),
        fa_joomla("\uf1aa"),
        fa_jpy("\uf157"),
        fa_jsfiddle("\uf1cc"),
        fa_key("\uf084"),
        fa_keyboard_o("\uf11c"),
        fa_krw("\uf159"),
        fa_language("\uf1ab"),
        fa_laptop("\uf109"),
        fa_lastfm("\uf202"),
        fa_lastfm_square("\uf203"),
        fa_leaf("\uf06c"),
        fa_leanpub("\uf212"),
        fa_legal("\uf0e3"),
        fa_lemon_o("\uf094"),
        fa_level_down("\uf149"),
        fa_level_up("\uf148"),
        fa_life_bouy("\uf1cd"),
        fa_life_buoy("\uf1cd"),
        fa_life_ring("\uf1cd"),
        fa_life_saver("\uf1cd"),
        fa_lightbulb_o("\uf0eb"),
        fa_line_chart("\uf201"),
        fa_link("\uf0c1"),
        fa_linkedin("\uf0e1"),
        fa_linkedin_square("\uf08c"),
        fa_linux("\uf17c"),
        fa_list("\uf03a"),
        fa_list_alt("\uf022"),
        fa_list_ol("\uf0cb"),
        fa_list_ul("\uf0ca"),
        fa_location_arrow("\uf124"),
        fa_lock("\uf023"),
        fa_long_arrow_down("\uf175"),
        fa_long_arrow_left("\uf177"),
        fa_long_arrow_right("\uf178"),
        fa_long_arrow_up("\uf176"),
        fa_magic("\uf0d0"),
        fa_magnet("\uf076"),
        fa_mail_forward("\uf064"),
        fa_mail_reply("\uf112"),
        fa_mail_reply_all("\uf122"),
        fa_male("\uf183"),
        fa_map("\uf279"),
        fa_map_marker("\uf041"),
        fa_map_o("\uf278"),
        fa_map_pin("\uf276"),
        fa_map_signs("\uf277"),
        fa_mars("\uf222"),
        fa_mars_double("\uf227"),
        fa_mars_stroke("\uf229"),
        fa_mars_stroke_h("\uf22b"),
        fa_mars_stroke_v("\uf22a"),
        fa_maxcdn("\uf136"),
        fa_meanpath("\uf20c"),
        fa_medium("\uf23a"),
        fa_medkit("\uf0fa"),
        fa_meh_o("\uf11a"),
        fa_mercury("\uf223"),
        fa_microphone("\uf130"),
        fa_microphone_slash("\uf131"),
        fa_minus("\uf068"),
        fa_minus_circle("\uf056"),
        fa_minus_square("\uf146"),
        fa_minus_square_o("\uf147"),
        fa_mixcloud("\uf289"),
        fa_mobile("\uf10b"),
        fa_mobile_phone("\uf10b"),
        fa_modx("\uf285"),
        fa_money("\uf0d6"),
        fa_moon_o("\uf186"),
        fa_mortar_board("\uf19d"),
        fa_motorcycle("\uf21c"),
        fa_mouse_pointer("\uf245"),
        fa_music("\uf001"),
        fa_navicon("\uf0c9"),
        fa_neuter("\uf22c"),
        fa_newspaper_o("\uf1ea"),
        fa_object_group("\uf247"),
        fa_object_ungroup("\uf248"),
        fa_odnoklassniki("\uf263"),
        fa_odnoklassniki_square("\uf264"),
        fa_opencart("\uf23d"),
        fa_openid("\uf19b"),
        fa_opera("\uf26a"),
        fa_optin_monster("\uf23c"),
        fa_outdent("\uf03b"),
        fa_pagelines("\uf18c"),
        fa_paint_brush("\uf1fc"),
        fa_paper_plane("\uf1d8"),
        fa_paper_plane_o("\uf1d9"),
        fa_paperclip("\uf0c6"),
        fa_paragraph("\uf1dd"),
        fa_paste("\uf0ea"),
        fa_pause("\uf04c"),
        fa_pause_circle("\uf28b"),
        fa_pause_circle_o("\uf28c"),
        fa_paw("\uf1b0"),
        fa_paypal("\uf1ed"),
        fa_pencil("\uf040"),
        fa_pencil_square("\uf14b"),
        fa_pencil_square_o("\uf044"),
        fa_percent("\uf295"),
        fa_phone("\uf095"),
        fa_phone_square("\uf098"),
        fa_photo("\uf03e"),
        fa_picture_o("\uf03e"),
        fa_pie_chart("\uf200"),
        fa_pied_piper("\uf1a7"),
        fa_pied_piper_alt("\uf1a8"),
        fa_pinterest("\uf0d2"),
        fa_pinterest_p("\uf231"),
        fa_pinterest_square("\uf0d3"),
        fa_plane("\uf072"),
        fa_play("\uf04b"),
        fa_play_circle("\uf144"),
        fa_play_circle_o("\uf01d"),
        fa_plug("\uf1e6"),
        fa_plus("\uf067"),
        fa_plus_circle("\uf055"),
        fa_plus_square("\uf0fe"),
        fa_plus_square_o("\uf196"),
        fa_power_off("\uf011"),
        fa_print("\uf02f"),
        fa_product_hunt("\uf288"),
        fa_puzzle_piece("\uf12e"),
        fa_qq("\uf1d6"),
        fa_qrcode("\uf029"),
        fa_question("\uf128"),
        fa_question_circle("\uf059"),
        fa_quote_left("\uf10d"),
        fa_quote_right("\uf10e"),
        fa_ra("\uf1d0"),
        fa_random("\uf074"),
        fa_rebel("\uf1d0"),
        fa_recycle("\uf1b8"),
        fa_reddit("\uf1a1"),
        fa_reddit_alien("\uf281"),
        fa_reddit_square("\uf1a2"),
        fa_refresh("\uf021"),
        fa_registered("\uf25d"),
        fa_remove("\uf00d"),
        fa_renren("\uf18b"),
        fa_reorder("\uf0c9"),
        fa_repeat("\uf01e"),
        fa_reply("\uf112"),
        fa_reply_all("\uf122"),
        fa_retweet("\uf079"),
        fa_rmb("\uf157"),
        fa_road("\uf018"),
        fa_rocket("\uf135"),
        fa_rotate_left("\uf0e2"),
        fa_rotate_right("\uf01e"),
        fa_rouble("\uf158"),
        fa_rss("\uf09e"),
        fa_rss_square("\uf143"),
        fa_rub("\uf158"),
        fa_ruble("\uf158"),
        fa_rupee("\uf156"),
        fa_safari("\uf267"),
        fa_save("\uf0c7"),
        fa_scissors("\uf0c4"),
        fa_scribd("\uf28a"),
        fa_search("\uf002"),
        fa_search_minus("\uf010"),
        fa_search_plus("\uf00e"),
        fa_sellsy("\uf213"),
        fa_send("\uf1d8"),
        fa_send_o("\uf1d9"),
        fa_server("\uf233"),
        fa_share("\uf064"),
        fa_share_alt("\uf1e0"),
        fa_share_alt_square("\uf1e1"),
        fa_share_square("\uf14d"),
        fa_share_square_o("\uf045"),
        fa_shekel("\uf20b"),
        fa_sheqel("\uf20b"),
        fa_shield("\uf132"),
        fa_ship("\uf21a"),
        fa_shirtsinbulk("\uf214"),
        fa_shopping_bag("\uf290"),
        fa_shopping_basket("\uf291"),
        fa_shopping_cart("\uf07a"),
        fa_sign_in("\uf090"),
        fa_sign_out("\uf08b"),
        fa_signal("\uf012"),
        fa_simplybuilt("\uf215"),
        fa_sitemap("\uf0e8"),
        fa_skyatlas("\uf216"),
        fa_skype("\uf17e"),
        fa_slack("\uf198"),
        fa_sliders("\uf1de"),
        fa_slideshare("\uf1e7"),
        fa_smile_o("\uf118"),
        fa_soccer_ball_o("\uf1e3"),
        fa_sort("\uf0dc"),
        fa_sort_alpha_asc("\uf15d"),
        fa_sort_alpha_desc("\uf15e"),
        fa_sort_amount_asc("\uf160"),
        fa_sort_amount_desc("\uf161"),
        fa_sort_asc("\uf0de"),
        fa_sort_desc("\uf0dd"),
        fa_sort_down("\uf0dd"),
        fa_sort_numeric_asc("\uf162"),
        fa_sort_numeric_desc("\uf163"),
        fa_sort_up("\uf0de"),
        fa_soundcloud("\uf1be"),
        fa_space_shuttle("\uf197"),
        fa_spinner("\uf110"),
        fa_spoon("\uf1b1"),
        fa_spotify("\uf1bc"),
        fa_square("\uf0c8"),
        fa_square_o("\uf096"),
        fa_stack_exchange("\uf18d"),
        fa_stack_overflow("\uf16c"),
        fa_star("\uf005"),
        fa_star_half("\uf089"),
        fa_star_half_empty("\uf123"),
        fa_star_half_full("\uf123"),
        fa_star_half_o("\uf123"),
        fa_star_o("\uf006"),
        fa_steam("\uf1b6"),
        fa_steam_square("\uf1b7"),
        fa_step_backward("\uf048"),
        fa_step_forward("\uf051"),
        fa_stethoscope("\uf0f1"),
        fa_sticky_note("\uf249"),
        fa_sticky_note_o("\uf24a"),
        fa_stop("\uf04d"),
        fa_stop_circle("\uf28d"),
        fa_stop_circle_o("\uf28e"),
        fa_street_view("\uf21d"),
        fa_strikethrough("\uf0cc"),
        fa_stumbleupon("\uf1a4"),
        fa_stumbleupon_circle("\uf1a3"),
        fa_subscript("\uf12c"),
        fa_subway("\uf239"),
        fa_suitcase("\uf0f2"),
        fa_sun_o("\uf185"),
        fa_superscript("\uf12b"),
        fa_support("\uf1cd"),
        fa_table("\uf0ce"),
        fa_tablet("\uf10a"),
        fa_tachometer("\uf0e4"),
        fa_tag("\uf02b"),
        fa_tags("\uf02c"),
        fa_tasks("\uf0ae"),
        fa_taxi("\uf1ba"),
        fa_television("\uf26c"),
        fa_tencent_weibo("\uf1d5"),
        fa_terminal("\uf120"),
        fa_text_height("\uf034"),
        fa_text_width("\uf035"),
        fa_th("\uf00a"),
        fa_th_large("\uf009"),
        fa_th_list("\uf00b"),
        fa_thumb_tack("\uf08d"),
        fa_thumbs_down("\uf165"),
        fa_thumbs_o_down("\uf088"),
        fa_thumbs_o_up("\uf087"),
        fa_thumbs_up("\uf164"),
        fa_ticket("\uf145"),
        fa_times("\uf00d"),
        fa_times_circle("\uf057"),
        fa_times_circle_o("\uf05c"),
        fa_tint("\uf043"),
        fa_toggle_down("\uf150"),
        fa_toggle_left("\uf191"),
        fa_toggle_off("\uf204"),
        fa_toggle_on("\uf205"),
        fa_toggle_right("\uf152"),
        fa_toggle_up("\uf151"),
        fa_trademark("\uf25c"),
        fa_train("\uf238"),
        fa_transgender("\uf224"),
        fa_transgender_alt("\uf225"),
        fa_trash("\uf1f8"),
        fa_trash_o("\uf014"),
        fa_tree("\uf1bb"),
        fa_trello("\uf181"),
        fa_tripadvisor("\uf262"),
        fa_trophy("\uf091"),
        fa_truck("\uf0d1"),
        fa_try("\uf195"),
        fa_tty("\uf1e4"),
        fa_tumblr("\uf173"),
        fa_tumblr_square("\uf174"),
        fa_turkish_lira("\uf195"),
        fa_twitch("\uf1e8"),
        fa_twitter("\uf099"),
        fa_twitter_square("\uf081"),
        fa_umbrella("\uf0e9"),
        fa_underline("\uf0cd"),
        fa_undo("\uf0e2"),
        fa_university("\uf19c"),
        fa_unlink("\uf127"),
        fa_unlock("\uf09c"),
        fa_unlock_alt("\uf13e"),
        fa_unsorted("\uf0dc"),
        fa_upload("\uf093"),
        fa_usb("\uf287"),
        fa_usd("\uf155"),
        fa_user("\uf007"),
        fa_user_md("\uf0f0"),
        fa_user_plus("\uf234"),
        fa_user_secret("\uf21b"),
        fa_user_times("\uf235"),
        fa_users("\uf0c0"),
        fa_venus("\uf221"),
        fa_venus_double("\uf226"),
        fa_venus_mars("\uf228"),
        fa_viacoin("\uf237"),
        fa_video_camera("\uf03d"),
        fa_vimeo("\uf27d"),
        fa_vimeo_square("\uf194"),
        fa_vine("\uf1ca"),
        fa_vk("\uf189"),
        fa_volume_down("\uf027"),
        fa_volume_off("\uf026"),
        fa_volume_up("\uf028"),
        fa_warning("\uf071"),
        fa_wechat("\uf1d7"),
        fa_weibo("\uf18a"),
        fa_weixin("\uf1d7"),
        fa_whatsapp("\uf232"),
        fa_wheelchair("\uf193"),
        fa_wifi("\uf1eb"),
        fa_wikipedia_w("\uf266"),
        fa_windows("\uf17a"),
        fa_won("\uf159"),
        fa_wordpress("\uf19a"),
        fa_wrench("\uf0ad"),
        fa_xing("\uf168"),
        fa_xing_square("\uf169"),
        fa_y_combinator("\uf23b"),
        fa_y_combinator_square("\uf1d4"),
        fa_yahoo("\uf19e"),
        fa_yc("\uf23b"),
        fa_yc_square("\uf1d4"),
        fa_yelp("\uf1e9"),
        fa_yen("\uf157"),
        fa_youtube("\uf167"),
        fa_youtube_play("\uf16a"),
        fa_youtube_square("\uf166"),
        fi_print_alt("c"),
        fi_print_alt_list("a");
         */

        empty(FontAwesome.empty),
        fa_500px(FontAwesome.fa_500px),
        fa_adjust(FontAwesome.fa_adjust),
        fa_adn(FontAwesome.fa_adn),
        fa_align_center(FontAwesome.fa_align_center),
        fa_align_justify(FontAwesome.fa_align_justify),
        fa_align_left(FontAwesome.fa_align_left),
        fa_align_right(FontAwesome.fa_align_right),
        fa_amazon(FontAwesome.fa_amazon),
        fa_ambulance(FontAwesome.fa_ambulance),
        fa_anchor(FontAwesome.fa_anchor),
        fa_android(FontAwesome.fa_android),
        fa_angellist(FontAwesome.fa_angellist),
        fa_angle_double_down(FontAwesome.fa_angle_double_down),
        fa_angle_double_left(FontAwesome.fa_angle_double_left),
        fa_angle_double_right(FontAwesome.fa_angle_double_right),
        fa_angle_double_up(FontAwesome.fa_angle_double_up),
        fa_angle_down(FontAwesome.fa_angle_down),
        fa_angle_left(FontAwesome.fa_angle_left),
        fa_angle_right(FontAwesome.fa_angle_right),
        fa_angle_up(FontAwesome.fa_angle_up),
        fa_apple(FontAwesome.fa_apple),
        fa_archive(FontAwesome.fa_archive),
        fa_area_chart(FontAwesome.fa_area_chart),
        fa_arrow_circle_down(FontAwesome.fa_arrow_circle_down),
        fa_arrow_circle_left(FontAwesome.fa_arrow_circle_left),
        fa_arrow_circle_o_down(FontAwesome.fa_arrow_circle_o_down),
        fa_arrow_circle_o_left(FontAwesome.fa_arrow_circle_o_left),
        fa_arrow_circle_o_right(FontAwesome.fa_arrow_circle_o_right),
        fa_arrow_circle_o_up(FontAwesome.fa_arrow_circle_o_up),
        fa_arrow_circle_right(FontAwesome.fa_arrow_circle_right),
        fa_arrow_circle_up(FontAwesome.fa_arrow_circle_up),
        fa_arrow_down(FontAwesome.fa_arrow_down),
        fa_arrow_left(FontAwesome.fa_arrow_left),
        fa_arrow_right(FontAwesome.fa_arrow_right),
        fa_arrow_up(FontAwesome.fa_arrow_up),
        fa_arrows(FontAwesome.fa_arrows),
        fa_arrows_alt(FontAwesome.fa_arrows_alt),
        fa_arrows_h(FontAwesome.fa_arrows_h),
        fa_arrows_v(FontAwesome.fa_arrows_v),
        fa_asterisk(FontAwesome.fa_asterisk),
        fa_at(FontAwesome.fa_at),
        fa_automobile(FontAwesome.fa_automobile),
        fa_backward(FontAwesome.fa_backward),
        fa_balance_scale(FontAwesome.fa_balance_scale),
        fa_ban(FontAwesome.fa_ban),
        fa_bank(FontAwesome.fa_bank),
        fa_bar_chart(FontAwesome.fa_bar_chart),
        fa_bar_chart_o(FontAwesome.fa_bar_chart_o),
        fa_barcode(FontAwesome.fa_barcode),
        fa_bars(FontAwesome.fa_bars),
        fa_battery_0(FontAwesome.fa_battery_0),
        fa_battery_1(FontAwesome.fa_battery_1),
        fa_battery_2(FontAwesome.fa_battery_2),
        fa_battery_3(FontAwesome.fa_battery_3),
        fa_battery_4(FontAwesome.fa_battery_4),
        fa_battery_empty(FontAwesome.fa_battery_empty),
        fa_battery_full(FontAwesome.fa_battery_full),
        fa_battery_half(FontAwesome.fa_battery_half),
        fa_battery_quarter(FontAwesome.fa_battery_quarter),
        fa_battery_three_quarters(FontAwesome.fa_battery_three_quarters),
        fa_bed(FontAwesome.fa_bed),
        fa_beer(FontAwesome.fa_beer),
        fa_behance(FontAwesome.fa_behance),
        fa_behance_square(FontAwesome.fa_behance_square),
        fa_bell(FontAwesome.fa_bell),
        fa_bell_o(FontAwesome.fa_bell_o),
        fa_bell_slash(FontAwesome.fa_bell_slash),
        fa_bell_slash_o(FontAwesome.fa_bell_slash_o),
        fa_bicycle(FontAwesome.fa_bicycle),
        fa_binoculars(FontAwesome.fa_binoculars),
        fa_birthday_cake(FontAwesome.fa_birthday_cake),
        fa_bitbucket(FontAwesome.fa_bitbucket),
        fa_bitbucket_square(FontAwesome.fa_bitbucket_square),
        fa_bitcoin(FontAwesome.fa_bitcoin),
        fa_black_tie(FontAwesome.fa_black_tie),
        fa_bluetooth(FontAwesome.fa_bluetooth),
        fa_bluetooth_b(FontAwesome.fa_bluetooth_b),
        fa_bold(FontAwesome.fa_bold),
        fa_bolt(FontAwesome.fa_bolt),
        fa_bomb(FontAwesome.fa_bomb),
        fa_book(FontAwesome.fa_book),
        fa_bookmark(FontAwesome.fa_bookmark),
        fa_bookmark_o(FontAwesome.fa_bookmark_o),
        fa_briefcase(FontAwesome.fa_briefcase),
        fa_btc(FontAwesome.fa_btc),
        fa_bug(FontAwesome.fa_bug),
        fa_building(FontAwesome.fa_building),
        fa_building_o(FontAwesome.fa_building_o),
        fa_bullhorn(FontAwesome.fa_bullhorn),
        fa_bullseye(FontAwesome.fa_bullseye),
        fa_bus(FontAwesome.fa_bus),
        fa_buysellads(FontAwesome.fa_buysellads),
        fa_cab(FontAwesome.fa_cab),
        fa_calculator(FontAwesome.fa_calculator),
        fa_calendar(FontAwesome.fa_calendar),
        fa_calendar_check_o(FontAwesome.fa_calendar_check_o),
        fa_calendar_minus_o(FontAwesome.fa_calendar_minus_o),
        fa_calendar_o(FontAwesome.fa_calendar_o),
        fa_calendar_plus_o(FontAwesome.fa_calendar_plus_o),
        fa_calendar_times_o(FontAwesome.fa_calendar_times_o),
        fa_camera(FontAwesome.fa_camera),
        fa_camera_retro(FontAwesome.fa_camera_retro),
        fa_car(FontAwesome.fa_car),
        fa_caret_down(FontAwesome.fa_caret_down),
        fa_caret_left(FontAwesome.fa_caret_left),
        fa_caret_right(FontAwesome.fa_caret_right),
        fa_caret_square_o_down(FontAwesome.fa_caret_square_o_down),
        fa_caret_square_o_left(FontAwesome.fa_caret_square_o_left),
        fa_caret_square_o_right(FontAwesome.fa_caret_square_o_right),
        fa_caret_square_o_up(FontAwesome.fa_caret_square_o_up),
        fa_caret_up(FontAwesome.fa_caret_up),
        fa_cart_arrow_down(FontAwesome.fa_cart_arrow_down),
        fa_cart_plus(FontAwesome.fa_cart_plus),
        fa_cc(FontAwesome.fa_cc),
        fa_cc_amex(FontAwesome.fa_cc_amex),
        fa_cc_diners_club(FontAwesome.fa_cc_diners_club),
        fa_cc_discover(FontAwesome.fa_cc_discover),
        fa_cc_jcb(FontAwesome.fa_cc_jcb),
        fa_cc_mastercard(FontAwesome.fa_cc_mastercard),
        fa_cc_paypal(FontAwesome.fa_cc_paypal),
        fa_cc_stripe(FontAwesome.fa_cc_stripe),
        fa_cc_visa(FontAwesome.fa_cc_visa),
        fa_certificate(FontAwesome.fa_certificate),
        fa_chain(FontAwesome.fa_chain),
        fa_chain_broken(FontAwesome.fa_chain_broken),
        fa_check(FontAwesome.fa_check),
        fa_check_circle(FontAwesome.fa_check_circle),
        fa_check_circle_o(FontAwesome.fa_check_circle_o),
        fa_check_square(FontAwesome.fa_check_square),
        fa_check_square_o(FontAwesome.fa_check_square_o),
        fa_chevron_circle_down(FontAwesome.fa_chevron_circle_down),
        fa_chevron_circle_left(FontAwesome.fa_chevron_circle_left),
        fa_chevron_circle_right(FontAwesome.fa_chevron_circle_right),
        fa_chevron_circle_up(FontAwesome.fa_chevron_circle_up),
        fa_chevron_down(FontAwesome.fa_chevron_down),
        fa_chevron_left(FontAwesome.fa_chevron_left),
        fa_chevron_right(FontAwesome.fa_chevron_right),
        fa_chevron_up(FontAwesome.fa_chevron_up),
        fa_child(FontAwesome.fa_child),
        fa_chrome(FontAwesome.fa_chrome),
        fa_circle(FontAwesome.fa_circle),
        fa_circle_o(FontAwesome.fa_circle_o),
        fa_circle_o_notch(FontAwesome.fa_circle_o_notch),
        fa_circle_thin(FontAwesome.fa_circle_thin),
        fa_clipboard(FontAwesome.fa_clipboard),
        fa_clock_o(FontAwesome.fa_clock_o),
        fa_clone(FontAwesome.fa_clone),
        fa_close(FontAwesome.fa_close),
        fa_cloud(FontAwesome.fa_cloud),
        fa_cloud_download(FontAwesome.fa_cloud_download),
        fa_cloud_upload(FontAwesome.fa_cloud_upload),
        fa_cny(FontAwesome.fa_cny),
        fa_code(FontAwesome.fa_code),
        fa_code_fork(FontAwesome.fa_code_fork),
        fa_codepen(FontAwesome.fa_codepen),
        fa_codiepie(FontAwesome.fa_codiepie),
        fa_coffee(FontAwesome.fa_coffee),
        fa_cog(FontAwesome.fa_cog),
        fa_cogs(FontAwesome.fa_cogs),
        fa_columns(FontAwesome.fa_columns),
        fa_comment(FontAwesome.fa_comment),
        fa_comment_o(FontAwesome.fa_comment_o),
        fa_commenting(FontAwesome.fa_commenting),
        fa_commenting_o(FontAwesome.fa_commenting_o),
        fa_comments(FontAwesome.fa_comments),
        fa_comments_o(FontAwesome.fa_comments_o),
        fa_compass(FontAwesome.fa_compass),
        fa_compress(FontAwesome.fa_compress),
        fa_connectdevelop(FontAwesome.fa_connectdevelop),
        fa_contao(FontAwesome.fa_contao),
        fa_copy(FontAwesome.fa_copy),
        fa_copyright(FontAwesome.fa_copyright),
        fa_creative_commons(FontAwesome.fa_creative_commons),
        fa_credit_card(FontAwesome.fa_credit_card),
        fa_credit_card_alt(FontAwesome.fa_credit_card_alt),
        fa_crop(FontAwesome.fa_crop),
        fa_crosshairs(FontAwesome.fa_crosshairs),
        fa_css3(FontAwesome.fa_css3),
        fa_cube(FontAwesome.fa_cube),
        fa_cubes(FontAwesome.fa_cubes),
        fa_cut(FontAwesome.fa_cut),
        fa_cutlery(FontAwesome.fa_cutlery),
        fa_dashboard(FontAwesome.fa_dashboard),
        fa_dashcube(FontAwesome.fa_dashcube),
        fa_database(FontAwesome.fa_database),
        fa_dedent(FontAwesome.fa_dedent),
        fa_delicious(FontAwesome.fa_delicious),
        fa_desktop(FontAwesome.fa_desktop),
        fa_deviantart(FontAwesome.fa_deviantart),
        fa_diamond(FontAwesome.fa_diamond),
        fa_digg(FontAwesome.fa_digg),
        fa_dollar(FontAwesome.fa_dollar),
        fa_dot_circle_o(FontAwesome.fa_dot_circle_o),
        fa_download(FontAwesome.fa_download),
        fa_dribbble(FontAwesome.fa_dribbble),
        fa_dropbox(FontAwesome.fa_dropbox),
        fa_drupal(FontAwesome.fa_drupal),
        fa_edge(FontAwesome.fa_edge),
        fa_edit(FontAwesome.fa_edit),
        fa_eject(FontAwesome.fa_eject),
        fa_ellipsis_h(FontAwesome.fa_ellipsis_h),
        fa_ellipsis_v(FontAwesome.fa_ellipsis_v),
        fa_empire(FontAwesome.fa_empire),
        fa_envelope(FontAwesome.fa_envelope),
        fa_envelope_o(FontAwesome.fa_envelope_o),
        fa_envelope_square(FontAwesome.fa_envelope_square),
        fa_eraser(FontAwesome.fa_eraser),
        fa_eur(FontAwesome.fa_eur),
        fa_euro(FontAwesome.fa_euro),
        fa_exchange(FontAwesome.fa_exchange),
        fa_exclamation(FontAwesome.fa_exclamation),
        fa_exclamation_circle(FontAwesome.fa_exclamation_circle),
        fa_exclamation_triangle(FontAwesome.fa_exclamation_triangle),
        fa_expand(FontAwesome.fa_expand),
        fa_expeditedssl(FontAwesome.fa_expeditedssl),
        fa_external_link(FontAwesome.fa_external_link),
        fa_external_link_square(FontAwesome.fa_external_link_square),
        fa_eye(FontAwesome.fa_eye),
        fa_eye_slash(FontAwesome.fa_eye_slash),
        fa_eyedropper(FontAwesome.fa_eyedropper),
        fa_facebook(FontAwesome.fa_facebook),
        fa_facebook_f(FontAwesome.fa_facebook_f),
        fa_facebook_official(FontAwesome.fa_facebook_official),
        fa_facebook_square(FontAwesome.fa_facebook_square),
        fa_fast_backward(FontAwesome.fa_fast_backward),
        fa_fast_forward(FontAwesome.fa_fast_forward),
        fa_fax(FontAwesome.fa_fax),
        fa_feed(FontAwesome.fa_feed),
        fa_female(FontAwesome.fa_female),
        fa_fighter_jet(FontAwesome.fa_fighter_jet),
        fa_file(FontAwesome.fa_file),
        fa_file_archive_o(FontAwesome.fa_file_archive_o),
        fa_file_audio_o(FontAwesome.fa_file_audio_o),
        fa_file_code_o(FontAwesome.fa_file_code_o),
        fa_file_excel_o(FontAwesome.fa_file_excel_o),
        fa_file_image_o(FontAwesome.fa_file_image_o),
        fa_file_movie_o(FontAwesome.fa_file_movie_o),
        fa_file_o(FontAwesome.fa_file_o),
        fa_file_pdf_o(FontAwesome.fa_file_pdf_o),
        fa_file_photo_o(FontAwesome.fa_file_photo_o),
        fa_file_picture_o(FontAwesome.fa_file_picture_o),
        fa_file_powerpoint_o(FontAwesome.fa_file_powerpoint_o),
        fa_file_sound_o(FontAwesome.fa_file_sound_o),
        fa_file_text(FontAwesome.fa_file_text),
        fa_file_text_o(FontAwesome.fa_file_text_o),
        fa_file_video_o(FontAwesome.fa_file_video_o),
        fa_file_word_o(FontAwesome.fa_file_word_o),
        fa_file_zip_o(FontAwesome.fa_file_zip_o),
        fa_files_o(FontAwesome.fa_files_o),
        fa_film(FontAwesome.fa_film),
        fa_filter(FontAwesome.fa_filter),
        fa_fire(FontAwesome.fa_fire),
        fa_fire_extinguisher(FontAwesome.fa_fire_extinguisher),
        fa_firefox(FontAwesome.fa_firefox),
        fa_flag(FontAwesome.fa_flag),
        fa_flag_checkered(FontAwesome.fa_flag_checkered),
        fa_flag_o(FontAwesome.fa_flag_o),
        fa_flash(FontAwesome.fa_flash),
        fa_flask(FontAwesome.fa_flask),
        fa_flickr(FontAwesome.fa_flickr),
        fa_floppy_o(FontAwesome.fa_floppy_o),
        fa_folder(FontAwesome.fa_folder),
        fa_folder_o(FontAwesome.fa_folder_o),
        fa_folder_open(FontAwesome.fa_folder_open),
        fa_folder_open_o(FontAwesome.fa_folder_open_o),
        fa_font(FontAwesome.fa_font),
        fa_fonticons(FontAwesome.fa_fonticons),
        fa_fort_awesome(FontAwesome.fa_fort_awesome),
        fa_forumbee(FontAwesome.fa_forumbee),
        fa_forward(FontAwesome.fa_forward),
        fa_foursquare(FontAwesome.fa_foursquare),
        fa_frown_o(FontAwesome.fa_frown_o),
        fa_futbol_o(FontAwesome.fa_futbol_o),
        fa_gamepad(FontAwesome.fa_gamepad),
        fa_gavel(FontAwesome.fa_gavel),
        fa_gbp(FontAwesome.fa_gbp),
        fa_ge(FontAwesome.fa_ge),
        fa_gear(FontAwesome.fa_gear),
        fa_gears(FontAwesome.fa_gears),
        fa_genderless(FontAwesome.fa_genderless),
        fa_get_pocket(FontAwesome.fa_get_pocket),
        fa_gg(FontAwesome.fa_gg),
        fa_gg_circle(FontAwesome.fa_gg_circle),
        fa_gift(FontAwesome.fa_gift),
        fa_git(FontAwesome.fa_git),
        fa_git_square(FontAwesome.fa_git_square),
        fa_github(FontAwesome.fa_github),
        fa_github_alt(FontAwesome.fa_github_alt),
        fa_github_square(FontAwesome.fa_github_square),
        fa_gittip(FontAwesome.fa_gittip),
        fa_glass(FontAwesome.fa_glass),
        fa_globe(FontAwesome.fa_globe),
        fa_google(FontAwesome.fa_google),
        fa_google_plus(FontAwesome.fa_google_plus),
        fa_google_plus_square(FontAwesome.fa_google_plus_square),
        fa_google_wallet(FontAwesome.fa_google_wallet),
        fa_graduation_cap(FontAwesome.fa_graduation_cap),
        fa_gratipay(FontAwesome.fa_gratipay),
        fa_group(FontAwesome.fa_group),
        fa_h_square(FontAwesome.fa_h_square),
        fa_hacker_news(FontAwesome.fa_hacker_news),
        fa_hand_grab_o(FontAwesome.fa_hand_grab_o),
        fa_hand_lizard_o(FontAwesome.fa_hand_lizard_o),
        fa_hand_o_down(FontAwesome.fa_hand_o_down),
        fa_hand_o_left(FontAwesome.fa_hand_o_left),
        fa_hand_o_right(FontAwesome.fa_hand_o_right),
        fa_hand_o_up(FontAwesome.fa_hand_o_up),
        fa_hand_paper_o(FontAwesome.fa_hand_paper_o),
        fa_hand_peace_o(FontAwesome.fa_hand_peace_o),
        fa_hand_pointer_o(FontAwesome.fa_hand_pointer_o),
        fa_hand_rock_o(FontAwesome.fa_hand_rock_o),
        fa_hand_scissors_o(FontAwesome.fa_hand_scissors_o),
        fa_hand_spock_o(FontAwesome.fa_hand_spock_o),
        fa_hand_stop_o(FontAwesome.fa_hand_stop_o),
        fa_hashtag(FontAwesome.fa_hashtag),
        fa_hdd_o(FontAwesome.fa_hdd_o),
        fa_header(FontAwesome.fa_header),
        fa_headphones(FontAwesome.fa_headphones),
        fa_heart(FontAwesome.fa_heart),
        fa_heart_o(FontAwesome.fa_heart_o),
        fa_heartbeat(FontAwesome.fa_heartbeat),
        fa_history(FontAwesome.fa_history),
        fa_home(FontAwesome.fa_home),
        fa_hospital_o(FontAwesome.fa_hospital_o),
        fa_hotel(FontAwesome.fa_hotel),
        fa_hourglass(FontAwesome.fa_hourglass),
        fa_hourglass_1(FontAwesome.fa_hourglass_1),
        fa_hourglass_2(FontAwesome.fa_hourglass_2),
        fa_hourglass_3(FontAwesome.fa_hourglass_3),
        fa_hourglass_end(FontAwesome.fa_hourglass_end),
        fa_hourglass_half(FontAwesome.fa_hourglass_half),
        fa_hourglass_o(FontAwesome.fa_hourglass_o),
        fa_hourglass_start(FontAwesome.fa_hourglass_start),
        fa_houzz(FontAwesome.fa_houzz),
        fa_html5(FontAwesome.fa_html5),
        fa_i_cursor(FontAwesome.fa_i_cursor),
        fa_ils(FontAwesome.fa_ils),
        fa_image(FontAwesome.fa_image),
        fa_inbox(FontAwesome.fa_inbox),
        fa_indent(FontAwesome.fa_indent),
        fa_industry(FontAwesome.fa_industry),
        fa_info(FontAwesome.fa_info),
        fa_info_circle(FontAwesome.fa_info_circle),
        fa_inr(FontAwesome.fa_inr),
        fa_instagram(FontAwesome.fa_instagram),
        fa_institution(FontAwesome.fa_institution),
        fa_internet_explorer(FontAwesome.fa_internet_explorer),
        fa_intersex(FontAwesome.fa_intersex),
        fa_ioxhost(FontAwesome.fa_ioxhost),
        fa_italic(FontAwesome.fa_italic),
        fa_joomla(FontAwesome.fa_joomla),
        fa_jpy(FontAwesome.fa_jpy),
        fa_jsfiddle(FontAwesome.fa_jsfiddle),
        fa_key(FontAwesome.fa_key),
        fa_keyboard_o(FontAwesome.fa_keyboard_o),
        fa_krw(FontAwesome.fa_krw),
        fa_language(FontAwesome.fa_language),
        fa_laptop(FontAwesome.fa_laptop),
        fa_lastfm(FontAwesome.fa_lastfm),
        fa_lastfm_square(FontAwesome.fa_lastfm_square),
        fa_leaf(FontAwesome.fa_leaf),
        fa_leanpub(FontAwesome.fa_leanpub),
        fa_legal(FontAwesome.fa_legal),
        fa_lemon_o(FontAwesome.fa_lemon_o),
        fa_level_down(FontAwesome.fa_level_down),
        fa_level_up(FontAwesome.fa_level_up),
        fa_life_bouy(FontAwesome.fa_life_bouy),
        fa_life_buoy(FontAwesome.fa_life_buoy),
        fa_life_ring(FontAwesome.fa_life_ring),
        fa_life_saver(FontAwesome.fa_life_saver),
        fa_lightbulb_o(FontAwesome.fa_lightbulb_o),
        fa_line_chart(FontAwesome.fa_line_chart),
        fa_link(FontAwesome.fa_link),
        fa_linkedin(FontAwesome.fa_linkedin),
        fa_linkedin_square(FontAwesome.fa_linkedin_square),
        fa_linux(FontAwesome.fa_linux),
        fa_list(FontAwesome.fa_list),
        fa_list_alt(FontAwesome.fa_list_alt),
        fa_list_ol(FontAwesome.fa_list_ol),
        fa_list_ul(FontAwesome.fa_list_ul),
        fa_location_arrow(FontAwesome.fa_location_arrow),
        fa_lock(FontAwesome.fa_lock),
        fa_long_arrow_down(FontAwesome.fa_long_arrow_down),
        fa_long_arrow_left(FontAwesome.fa_long_arrow_left),
        fa_long_arrow_right(FontAwesome.fa_long_arrow_right),
        fa_long_arrow_up(FontAwesome.fa_long_arrow_up),
        fa_magic(FontAwesome.fa_magic),
        fa_magnet(FontAwesome.fa_magnet),
        fa_mail_forward(FontAwesome.fa_mail_forward),
        fa_mail_reply(FontAwesome.fa_mail_reply),
        fa_mail_reply_all(FontAwesome.fa_mail_reply_all),
        fa_male(FontAwesome.fa_male),
        fa_map(FontAwesome.fa_map),
        fa_map_marker(FontAwesome.fa_map_marker),
        fa_map_o(FontAwesome.fa_map_o),
        fa_map_pin(FontAwesome.fa_map_pin),
        fa_map_signs(FontAwesome.fa_map_signs),
        fa_mars(FontAwesome.fa_mars),
        fa_mars_double(FontAwesome.fa_mars_double),
        fa_mars_stroke(FontAwesome.fa_mars_stroke),
        fa_mars_stroke_h(FontAwesome.fa_mars_stroke_h),
        fa_mars_stroke_v(FontAwesome.fa_mars_stroke_v),
        fa_maxcdn(FontAwesome.fa_maxcdn),
        fa_meanpath(FontAwesome.fa_meanpath),
        fa_medium(FontAwesome.fa_medium),
        fa_medkit(FontAwesome.fa_medkit),
        fa_meh_o(FontAwesome.fa_meh_o),
        fa_mercury(FontAwesome.fa_mercury),
        fa_microphone(FontAwesome.fa_microphone),
        fa_microphone_slash(FontAwesome.fa_microphone_slash),
        fa_minus(FontAwesome.fa_minus),
        fa_minus_circle(FontAwesome.fa_minus_circle),
        fa_minus_square(FontAwesome.fa_minus_square),
        fa_minus_square_o(FontAwesome.fa_minus_square_o),
        fa_mixcloud(FontAwesome.fa_mixcloud),
        fa_mobile(FontAwesome.fa_mobile),
        fa_mobile_phone(FontAwesome.fa_mobile_phone),
        fa_modx(FontAwesome.fa_modx),
        fa_money(FontAwesome.fa_money),
        fa_moon_o(FontAwesome.fa_moon_o),
        fa_mortar_board(FontAwesome.fa_mortar_board),
        fa_motorcycle(FontAwesome.fa_motorcycle),
        fa_mouse_pointer(FontAwesome.fa_mouse_pointer),
        fa_music(FontAwesome.fa_music),
        fa_navicon(FontAwesome.fa_navicon),
        fa_neuter(FontAwesome.fa_neuter),
        fa_newspaper_o(FontAwesome.fa_newspaper_o),
        fa_object_group(FontAwesome.fa_object_group),
        fa_object_ungroup(FontAwesome.fa_object_ungroup),
        fa_odnoklassniki(FontAwesome.fa_odnoklassniki),
        fa_odnoklassniki_square(FontAwesome.fa_odnoklassniki_square),
        fa_opencart(FontAwesome.fa_opencart),
        fa_openid(FontAwesome.fa_openid),
        fa_opera(FontAwesome.fa_opera),
        fa_optin_monster(FontAwesome.fa_optin_monster),
        fa_outdent(FontAwesome.fa_outdent),
        fa_pagelines(FontAwesome.fa_pagelines),
        fa_paint_brush(FontAwesome.fa_paint_brush),
        fa_paper_plane(FontAwesome.fa_paper_plane),
        fa_paper_plane_o(FontAwesome.fa_paper_plane_o),
        fa_paperclip(FontAwesome.fa_paperclip),
        fa_paragraph(FontAwesome.fa_paragraph),
        fa_paste(FontAwesome.fa_paste),
        fa_pause(FontAwesome.fa_pause),
        fa_pause_circle(FontAwesome.fa_pause_circle),
        fa_pause_circle_o(FontAwesome.fa_pause_circle_o),
        fa_paw(FontAwesome.fa_paw),
        fa_paypal(FontAwesome.fa_paypal),
        fa_pencil(FontAwesome.fa_pencil),
        fa_pencil_square(FontAwesome.fa_pencil_square),
        fa_pencil_square_o(FontAwesome.fa_pencil_square_o),
        fa_percent(FontAwesome.fa_percent),
        fa_phone(FontAwesome.fa_phone),
        fa_phone_square(FontAwesome.fa_phone_square),
        fa_photo(FontAwesome.fa_photo),
        fa_picture_o(FontAwesome.fa_picture_o),
        fa_pie_chart(FontAwesome.fa_pie_chart),
        fa_pied_piper(FontAwesome.fa_pied_piper),
        fa_pied_piper_alt(FontAwesome.fa_pied_piper_alt),
        fa_pinterest(FontAwesome.fa_pinterest),
        fa_pinterest_p(FontAwesome.fa_pinterest_p),
        fa_pinterest_square(FontAwesome.fa_pinterest_square),
        fa_plane(FontAwesome.fa_plane),
        fa_play(FontAwesome.fa_play),
        fa_play_circle(FontAwesome.fa_play_circle),
        fa_play_circle_o(FontAwesome.fa_play_circle_o),
        fa_plug(FontAwesome.fa_plug),
        fa_plus(FontAwesome.fa_plus),
        fa_plus_circle(FontAwesome.fa_plus_circle),
        fa_plus_square(FontAwesome.fa_plus_square),
        fa_plus_square_o(FontAwesome.fa_plus_square_o),
        fa_power_off(FontAwesome.fa_power_off),
        fa_print(FontAwesome.fa_print),
        fa_product_hunt(FontAwesome.fa_product_hunt),
        fa_puzzle_piece(FontAwesome.fa_puzzle_piece),
        fa_qq(FontAwesome.fa_qq),
        fa_qrcode(FontAwesome.fa_qrcode),
        fa_question(FontAwesome.fa_question),
        fa_question_circle(FontAwesome.fa_question_circle),
        fa_quote_left(FontAwesome.fa_quote_left),
        fa_quote_right(FontAwesome.fa_quote_right),
        fa_ra(FontAwesome.fa_ra),
        fa_random(FontAwesome.fa_random),
        fa_rebel(FontAwesome.fa_rebel),
        fa_recycle(FontAwesome.fa_recycle),
        fa_reddit(FontAwesome.fa_reddit),
        fa_reddit_alien(FontAwesome.fa_reddit_alien),
        fa_reddit_square(FontAwesome.fa_reddit_square),
        fa_refresh(FontAwesome.fa_refresh),
        fa_registered(FontAwesome.fa_registered),
        fa_remove(FontAwesome.fa_remove),
        fa_renren(FontAwesome.fa_renren),
        fa_reorder(FontAwesome.fa_reorder),
        fa_repeat(FontAwesome.fa_repeat),
        fa_reply(FontAwesome.fa_reply),
        fa_reply_all(FontAwesome.fa_reply_all),
        fa_retweet(FontAwesome.fa_retweet),
        fa_rmb(FontAwesome.fa_rmb),
        fa_road(FontAwesome.fa_road),
        fa_rocket(FontAwesome.fa_rocket),
        fa_rotate_left(FontAwesome.fa_rotate_left),
        fa_rotate_right(FontAwesome.fa_rotate_right),
        fa_rouble(FontAwesome.fa_rouble),
        fa_rss(FontAwesome.fa_rss),
        fa_rss_square(FontAwesome.fa_rss_square),
        fa_rub(FontAwesome.fa_rub),
        fa_ruble(FontAwesome.fa_ruble),
        fa_rupee(FontAwesome.fa_rupee),
        fa_safari(FontAwesome.fa_safari),
        fa_save(FontAwesome.fa_save),
        fa_scissors(FontAwesome.fa_scissors),
        fa_scribd(FontAwesome.fa_scribd),
        fa_search(FontAwesome.fa_search),
        fa_search_minus(FontAwesome.fa_search_minus),
        fa_search_plus(FontAwesome.fa_search_plus),
        fa_sellsy(FontAwesome.fa_sellsy),
        fa_send(FontAwesome.fa_send),
        fa_send_o(FontAwesome.fa_send_o),
        fa_server(FontAwesome.fa_server),
        fa_share(FontAwesome.fa_share),
        fa_share_alt(FontAwesome.fa_share_alt),
        fa_share_alt_square(FontAwesome.fa_share_alt_square),
        fa_share_square(FontAwesome.fa_share_square),
        fa_share_square_o(FontAwesome.fa_share_square_o),
        fa_shekel(FontAwesome.fa_shekel),
        fa_sheqel(FontAwesome.fa_sheqel),
        fa_shield(FontAwesome.fa_shield),
        fa_ship(FontAwesome.fa_ship),
        fa_shirtsinbulk(FontAwesome.fa_shirtsinbulk),
        fa_shopping_bag(FontAwesome.fa_shopping_bag),
        fa_shopping_basket(FontAwesome.fa_shopping_basket),
        fa_shopping_cart(FontAwesome.fa_shopping_cart),
        fa_sign_in(FontAwesome.fa_sign_in),
        fa_sign_out(FontAwesome.fa_sign_out),
        fa_signal(FontAwesome.fa_signal),
        fa_simplybuilt(FontAwesome.fa_simplybuilt),
        fa_sitemap(FontAwesome.fa_sitemap),
        fa_skyatlas(FontAwesome.fa_skyatlas),
        fa_skype(FontAwesome.fa_skype),
        fa_slack(FontAwesome.fa_slack),
        fa_sliders(FontAwesome.fa_sliders),
        fa_slideshare(FontAwesome.fa_slideshare),
        fa_smile_o(FontAwesome.fa_smile_o),
        fa_soccer_ball_o(FontAwesome.fa_soccer_ball_o),
        fa_sort(FontAwesome.fa_sort),
        fa_sort_alpha_asc(FontAwesome.fa_sort_alpha_asc),
        fa_sort_alpha_desc(FontAwesome.fa_sort_alpha_desc),
        fa_sort_amount_asc(FontAwesome.fa_sort_amount_asc),
        fa_sort_amount_desc(FontAwesome.fa_sort_amount_desc),
        fa_sort_asc(FontAwesome.fa_sort_asc),
        fa_sort_desc(FontAwesome.fa_sort_desc),
        fa_sort_down(FontAwesome.fa_sort_down),
        fa_sort_numeric_asc(FontAwesome.fa_sort_numeric_asc),
        fa_sort_numeric_desc(FontAwesome.fa_sort_numeric_desc),
        fa_sort_up(FontAwesome.fa_sort_up),
        fa_soundcloud(FontAwesome.fa_soundcloud),
        fa_space_shuttle(FontAwesome.fa_space_shuttle),
        fa_spinner(FontAwesome.fa_spinner),
        fa_spoon(FontAwesome.fa_spoon),
        fa_spotify(FontAwesome.fa_spotify),
        fa_square(FontAwesome.fa_square),
        fa_square_o(FontAwesome.fa_square_o),
        fa_stack_exchange(FontAwesome.fa_stack_exchange),
        fa_stack_overflow(FontAwesome.fa_stack_overflow),
        fa_star(FontAwesome.fa_star),
        fa_star_half(FontAwesome.fa_star_half),
        fa_star_half_empty(FontAwesome.fa_star_half_empty),
        fa_star_half_full(FontAwesome.fa_star_half_full),
        fa_star_half_o(FontAwesome.fa_star_half_o),
        fa_star_o(FontAwesome.fa_star_o),
        fa_steam(FontAwesome.fa_steam),
        fa_steam_square(FontAwesome.fa_steam_square),
        fa_step_backward(FontAwesome.fa_step_backward),
        fa_step_forward(FontAwesome.fa_step_forward),
        fa_stethoscope(FontAwesome.fa_stethoscope),
        fa_sticky_note(FontAwesome.fa_sticky_note),
        fa_sticky_note_o(FontAwesome.fa_sticky_note_o),
        fa_stop(FontAwesome.fa_stop),
        fa_stop_circle(FontAwesome.fa_stop_circle),
        fa_stop_circle_o(FontAwesome.fa_stop_circle_o),
        fa_street_view(FontAwesome.fa_street_view),
        fa_strikethrough(FontAwesome.fa_strikethrough),
        fa_stumbleupon(FontAwesome.fa_stumbleupon),
        fa_stumbleupon_circle(FontAwesome.fa_stumbleupon_circle),
        fa_subscript(FontAwesome.fa_subscript),
        fa_subway(FontAwesome.fa_subway),
        fa_suitcase(FontAwesome.fa_suitcase),
        fa_sun_o(FontAwesome.fa_sun_o),
        fa_superscript(FontAwesome.fa_superscript),
        fa_support(FontAwesome.fa_support),
        fa_table(FontAwesome.fa_table),
        fa_tablet(FontAwesome.fa_tablet),
        fa_tachometer(FontAwesome.fa_tachometer),
        fa_tag(FontAwesome.fa_tag),
        fa_tags(FontAwesome.fa_tags),
        fa_tasks(FontAwesome.fa_tasks),
        fa_taxi(FontAwesome.fa_taxi),
        fa_television(FontAwesome.fa_television),
        fa_tencent_weibo(FontAwesome.fa_tencent_weibo),
        fa_terminal(FontAwesome.fa_terminal),
        fa_text_height(FontAwesome.fa_text_height),
        fa_text_width(FontAwesome.fa_text_width),
        fa_th(FontAwesome.fa_th),
        fa_th_large(FontAwesome.fa_th_large),
        fa_th_list(FontAwesome.fa_th_list),
        fa_thumb_tack(FontAwesome.fa_thumb_tack),
        fa_thumbs_down(FontAwesome.fa_thumbs_down),
        fa_thumbs_o_down(FontAwesome.fa_thumbs_o_down),
        fa_thumbs_o_up(FontAwesome.fa_thumbs_o_up),
        fa_thumbs_up(FontAwesome.fa_thumbs_up),
        fa_ticket(FontAwesome.fa_ticket),
        fa_times(FontAwesome.fa_times),
        fa_times_circle(FontAwesome.fa_times_circle),
        fa_times_circle_o(FontAwesome.fa_times_circle_o),
        fa_tint(FontAwesome.fa_tint),
        fa_toggle_down(FontAwesome.fa_toggle_down),
        fa_toggle_left(FontAwesome.fa_toggle_left),
        fa_toggle_off(FontAwesome.fa_toggle_off),
        fa_toggle_on(FontAwesome.fa_toggle_on),
        fa_toggle_right(FontAwesome.fa_toggle_right),
        fa_toggle_up(FontAwesome.fa_toggle_up),
        fa_trademark(FontAwesome.fa_trademark),
        fa_train(FontAwesome.fa_train),
        fa_transgender(FontAwesome.fa_transgender),
        fa_transgender_alt(FontAwesome.fa_transgender_alt),
        fa_trash(FontAwesome.fa_trash),
        fa_trash_o(FontAwesome.fa_trash_o),
        fa_tree(FontAwesome.fa_tree),
        fa_trello(FontAwesome.fa_trello),
        fa_tripadvisor(FontAwesome.fa_tripadvisor),
        fa_trophy(FontAwesome.fa_trophy),
        fa_truck(FontAwesome.fa_truck),
        fa_try(FontAwesome.fa_try),
        fa_tty(FontAwesome.fa_tty),
        fa_tumblr(FontAwesome.fa_tumblr),
        fa_tumblr_square(FontAwesome.fa_tumblr_square),
        fa_turkish_lira(FontAwesome.fa_turkish_lira),
        fa_twitch(FontAwesome.fa_twitch),
        fa_twitter(FontAwesome.fa_twitter),
        fa_twitter_square(FontAwesome.fa_twitter_square),
        fa_umbrella(FontAwesome.fa_umbrella),
        fa_underline(FontAwesome.fa_underline),
        fa_undo(FontAwesome.fa_undo),
        fa_university(FontAwesome.fa_university),
        fa_unlink(FontAwesome.fa_unlink),
        fa_unlock(FontAwesome.fa_unlock),
        fa_unlock_alt(FontAwesome.fa_unlock_alt),
        fa_unsorted(FontAwesome.fa_unsorted),
        fa_upload(FontAwesome.fa_upload),
        fa_usb(FontAwesome.fa_usb),
        fa_usd(FontAwesome.fa_usd),
        fa_user(FontAwesome.fa_user),
        fa_user_md(FontAwesome.fa_user_md),
        fa_user_plus(FontAwesome.fa_user_plus),
        fa_user_secret(FontAwesome.fa_user_secret),
        fa_user_times(FontAwesome.fa_user_times),
        fa_users(FontAwesome.fa_users),
        fa_venus(FontAwesome.fa_venus),
        fa_venus_double(FontAwesome.fa_venus_double),
        fa_venus_mars(FontAwesome.fa_venus_mars),
        fa_viacoin(FontAwesome.fa_viacoin),
        fa_video_camera(FontAwesome.fa_video_camera),
        fa_vimeo(FontAwesome.fa_vimeo),
        fa_vimeo_square(FontAwesome.fa_vimeo_square),
        fa_vine(FontAwesome.fa_vine),
        fa_vk(FontAwesome.fa_vk),
        fa_volume_down(FontAwesome.fa_volume_down),
        fa_volume_off(FontAwesome.fa_volume_off),
        fa_volume_up(FontAwesome.fa_volume_up),
        fa_warning(FontAwesome.fa_warning),
        fa_wechat(FontAwesome.fa_wechat),
        fa_weibo(FontAwesome.fa_weibo),
        fa_weixin(FontAwesome.fa_weixin),
        fa_whatsapp(FontAwesome.fa_whatsapp),
        fa_wheelchair(FontAwesome.fa_wheelchair),
        fa_wifi(FontAwesome.fa_wifi),
        fa_wikipedia_w(FontAwesome.fa_wikipedia_w),
        fa_windows(FontAwesome.fa_windows),
        fa_won(FontAwesome.fa_won),
        fa_wordpress(FontAwesome.fa_wordpress),
        fa_wrench(FontAwesome.fa_wrench),
        fa_xing(FontAwesome.fa_xing),
        fa_xing_square(FontAwesome.fa_xing_square),
        fa_y_combinator(FontAwesome.fa_y_combinator),
        fa_y_combinator_square(FontAwesome.fa_y_combinator_square),
        fa_yahoo(FontAwesome.fa_yahoo),
        fa_yc(FontAwesome.fa_yc),
        fa_yc_square(FontAwesome.fa_yc_square),
        fa_yelp(FontAwesome.fa_yelp),
        fa_yen(FontAwesome.fa_yen),
        fa_youtube(FontAwesome.fa_youtube),
        fa_youtube_play(FontAwesome.fa_youtube_play),
        fa_youtube_square(FontAwesome.fa_youtube_square),
        fi_print_alt(FontAwesome.fa_print),
        fi_print_alt_list(FontAwesome.fa_print);

        private final FontAwesome fa;

        IconEnum(FontAwesome fa) {
            this.fa = fa;
        }

        public String getCode() {
            return fa.getCode();
        }

        public FontAwesome getNewEnum() {
            return fa;
        }
    }


    public static Label getLabel( IBaseIconDescriptor iconDescriptor ) {
        try {
            if ( BaseApp.APP().getViewPrefService().isVectorIcons() ) {
                return IconFactory.getLabel( iconDescriptor, false );
            } else {

                Label fmxLabel = null;
                if ( iconDescriptor != null ){
                    fmxLabel = IconFactory.getLabel( iconDescriptor.getCompatibleID(), false );
                }
                if ( fmxLabel == null ) {
                    fmxLabel = IconFactory.getLabel( iconDescriptor, false );
                }
                return fmxLabel;
            }
        } catch (AppException ex) {
            throw new RuntimeException(ex);
        }
    }

    public static Label getLabel(ActionTypeEnum actionTypeEnum) {
        return getLabel( actionTypeEnum.action.getIcon() );
    }

    /**
     * Метод создания IAction
     *
     * @param typicalType  тип
     * @param title        название
     * @param toolTipText  тултип
     * @param eventHandler обработчик
     * @param id           id права безопасности
     * @param strategy     стратегия отображения в случае недопустимости запуска по безопасности
     * @return
     */
    public static IAction createAction(ActionTypeEnum typicalType,
                                       IBaseIconDescriptor icon,
                                       String title,
                                       String toolTipText,
                                       EventHandler<ActionEvent> eventHandler,
                                       List<KeyCodeCombination> keyCodeCombinations,
                                       Integer id,
                                       SecurityStrategyEnum strategy) {

        return createActionBuilder(typicalType,
                icon, title, toolTipText,
                eventHandler,
                id,
                strategy,
                keyCodeCombinations).build();
    }

    /**
     * Создание IAction, код которого будет исполнятся в другом потоке
     *
     * @param typicalType  тип
     * @param title        название
     * @param toolTipText  тултип
     * @param eventHandler обработчик
     * @param id           id права безопасности
     * @param strategy     стратегия отображения в случае недопустимости запуска по безопасности
     * @return обьект IAction
     */
    public static IAction createParallelAction(ActionTypeEnum typicalType,
                                               IBaseIconDescriptor icon,
                                               String title,
                                               String toolTipText,
                                               EventHandler<ActionEvent> eventHandler, List<KeyCodeCombination> keyCodeCombinations,
                                               Integer id,
                                               SecurityStrategyEnum strategy
    ) {
        return createActionBuilder(typicalType,
                icon,
                title,
                toolTipText,
                eventHandler,
                id,
                strategy,
                keyCodeCombinations).
                setParallel(true).build();
    }

    /**
     * Создание Action на основе предустановленных типов
     *
     * @param typicalType  тип
     * @param eventHandler обработчик
     * @return кнопка
     */
    public static IAction createAction(ActionTypeEnum typicalType, EventHandler<ActionEvent> eventHandler) {
        if (typicalType != null) {
            IAction action = typicalType.getAction();
            if (action != null) {
                return createActionBuilder(typicalType,
                        action.getIcon(),
                        action.getTitle(),
                        action.getToolTip(),
                        eventHandler,
                        action.getId(),
                        action.getSecurityStrategy(),
                        action.getHotKey()).build();
            } else {
                return null;
            }
        } else {
            return new ActionBuilder().setHandler(eventHandler).build();
        }
    }

    /**
     * Создание кнопки на основе IAction
     *
     * @param action
     * @return
     */
    public static ButtonBase createButton(IAction action) {

        JInvButton button = new JInvButton();
        button.setFocusTraversable(false);
        button.setAction(action);
        return button;
    }

    /**
     * Создание кнопки
     *
     * @param typicalType  тип
     * @param title        название
     * @param toolTipText  тултип
     * @param eventHandler обработчик
     * @param parallel     признак параллельности выполнения
     * @param id           id права безопасности
     * @param strategy     стратегия отображения в случае недопустимости запуска по безопасности
     * @return кнопка
     */
    public static ButtonBase createButton(
        ActionTypeEnum typicalType,
        IBaseIconDescriptor icon, String title,
        String toolTipText,
        EventHandler<ActionEvent> eventHandler,
        boolean parallel,
        Integer id,
        SecurityStrategyEnum strategy)
    {
        IAction action;
        if (parallel) {
            action = createParallelAction(typicalType,
                    icon,
                    title,
                    toolTipText,
                    eventHandler,
                    null,
                    id,
                    strategy);
        } else {
            action = createAction(typicalType,
                    icon,
                    title,
                    toolTipText,
                    eventHandler,
                    null,
                    id,
                    strategy);
        }
        return createButton(action);
    }

    /**
     * Метод создания кнопки
     *
     * @param eventHandler обработчик
     * @return
     */
    public static ButtonBase createButton(IBaseIconDescriptor icon, EventHandler<ActionEvent> eventHandler
    ) {
        return createButton(icon,eventHandler,null);
    }


    /**
     * Метод создания кнопки для совместимости по просьбам трудящихся
     * @param enoom
     * @param eventHandler
     * @param tooltipText
     */
    @Deprecated
    public static ButtonBase createButton(FontAwesome enoom, EventHandler<ActionEvent> eventHandler, String tooltipText
    ) {
        return createButton(enoom, null, eventHandler, tooltipText);
    }

    /**
     * Метод создания кнопки для совместимости по просьбам трудящихся
     */
    public static ButtonBase createButton(FontAwesome enoom, String fmxIcoName, EventHandler<ActionEvent> eventHandler, String tooltipText
    ) {
        if(fmxIcoName != null && !fmxIcoName.equals("")) {
            return createButton(new IconDescriptorBuilder<>(enoom, fmxIcoName).build(), eventHandler, tooltipText);
        } else {
            return createButton(new IconDescriptorBuilder<>(enoom).build(), eventHandler, tooltipText);
        }
    }

    /**
     * Метод создания кнопки для совместимости по просьбам трудящихся
     */
    public static ButtonBase createButton(FontAwesome enoom, EventHandler<ActionEvent> eventHandler) {
        return createButton(new IconDescriptorBuilder<>(enoom).build(), eventHandler);
    }

    /**
     * Метод создания кнопки
     *
     * @param eventHandler обработчик
     * @param tooltipText
     * @return
     */
    public static ButtonBase createButton(IBaseIconDescriptor icon,
                                          EventHandler<ActionEvent> eventHandler, String tooltipText
    ) {
        return createButton(null,icon,null,tooltipText,eventHandler,false,null,null);
    }

    /**
     * Метод создания кнопки на основе предустановленного типа
     *
     * @param typical
     * @return
     */
    public static Button createButton(ActionTypeEnum typical, EventHandler<ActionEvent> eventHandler) {
        IAction action = createAction(typical, eventHandler);
        return (Button)createButton(action);
    }

    /**
     * Метод изменения отображения и поведение кнопки на основе переданного IAction action
     *
     * @param button кнопка для измененеия
     * @param action обьект IAction
     * @return
     */
    public static ButtonBase initButton(ButtonBase button, IAction action) {
        if (button != null && button instanceof JInvButton) {
            ((JInvButton) button).setAction(action);
        }
        return button;
    }

    /**
     * Метод изменения отображения и поведение кнопки на основе переданного типа и обработчика
     *
     * @param button       кнопка для измененеия
     * @param typical      тип
     * @param eventHandler обработчик
     * @return
     */
    public static ButtonBase initButton(ButtonBase button, ActionTypeEnum typical, EventHandler<ActionEvent> eventHandler) {

        if (button != null && typical != null) {

            IAction action = createAction(typical, eventHandler);
            return initButton(button, action);
        } else {
            return null;
        }

    }

    private static ActionBuilder createActionBuilder(ActionTypeEnum typicalType,
                                                     IBaseIconDescriptor icon,
                                                     String title, String toolTipText,
                                                     EventHandler<ActionEvent> eventHandler,
                                                     Integer id,
                                                     SecurityStrategyEnum strategy, List<KeyCodeCombination> keyCodeCombinations) {

        if (icon == null && typicalType != null) {
            icon = typicalType.getAction().getIcon();
        }
        if (S.isNullOrEmpty(title) && typicalType != null) {
            title = typicalType.getAction().getTitle();
        }
        return new ActionBuilder().icon(icon).
                title(title).
                toolTipText(toolTipText).
                handler(eventHandler).
                id(id).setActionType(typicalType).
                securityStrategy(strategy).setListKeyCombination(keyCodeCombinations);

    }

}
