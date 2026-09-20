package ru.inversion.fx.form.controls;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.StringProperty;
import javafx.css.PseudoClass;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.util.Duration;
import ru.inversion.dataset.fx.DSFXAdapter;
import ru.inversion.fx.app.Tags;
import ru.inversion.fx.form.AbstractBaseController;
import ru.inversion.fx.form.JInvFXFormController;
import ru.inversion.utils.S;
import ru.inversion.utils.U;
import ru.inversion.utils.converter.TypeConverter;
import ru.inversion.utils.scheck.JInvStringWorker;
import ru.inversion.utils.scheck.JInvStringWorkerException;

import java.io.ObjectStreamClass;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static ru.inversion.dataset.fx.DSFXAdapter.PROPERTY_DATA_SET_ADAPTER;
import static ru.inversion.fx.form.controls.IJInvControl.CONTROL_VALUE_CHANGED;

/**
 Содержит вспомогательные методы фреймворка связанные с контролами
 @author ssu @ */
public class Controls {
    public static final String CONTROL_FIELD_NAME = "ru.inversion.field_name";
    public static final String CONTROL_PARENT = "ru.inversion.parent_control";
    public static final String CONTROL_ID = "ru.inversion.id";
    public static final String F7FILTER_GROUP_ID = "ru.inversion.f7filter.group.id";
    public static final String F7FILTER_ORDER_IN_GROUP = "ru.inversion.f7filter.order_in_group";
    public static final String F7FILTER_IGNORE_95_SYMB_IN_LIKE = "r.i.column.ignore_95_symb";

    public static final String LIST_NON_CONTROL_COMPONENT = "ru.inversion.list_non_control_component";
    /** Ключ свойства для успешно отвалидированного контрола */
    private static final String CONTROL_VALIDATED = "ru.inversion.control_validated";
    private static final ResourceBundle bundle = ResourceBundle.getBundle( "fore" );
    public static final PseudoClass DISABLED_GREY_STATE = PseudoClass.getPseudoClass( "disabled" );
    private static final EventHandler<MouseEvent> consumeFilter = Event::consume;

    /** */
    public static void setProperty( Control control, String property, Object value ) {
        if ( U.containsNull( control, property ) ) {
            return;
        }
        if ( control instanceof IJInvControl ) {
            IJInvControl ic = (IJInvControl) control;
            ic.setProperty( property, value );
        } else {
            control.getProperties().put( property, value );
        }
    }

    /** */
    public static boolean hasProperty( Control control, String property ) {
        if ( U.containsNull( control, property ) ) {
            return false;
        }
        if ( control instanceof IJInvControl ) {
            IJInvControl ic = (IJInvControl) control;
            return ic.hasProperty( property );
        } else {
            return control.getProperties().containsKey( property );
        }
    }

    /** */
    public static <T> T getProperty( Control control, String property ) {
        return getProperty( control, property, null );
    }

    /** */
    public static <T> T getProperty( Control control, String property, T defaultValue ) {
        if ( U.containsNull( control, property ) ) {
            return defaultValue;
        }
        if ( control instanceof IJInvControl ) {
            IJInvControl ic = (IJInvControl) control;
            return ic.getProperty( property, defaultValue );
        } else {
            return (T) control.getProperties().getOrDefault( property, defaultValue );
        }
    }

    public static void setValue( Node control, Object value ) {
        setValue( (Object) control, value );
    }

    /**
     Метод для установки значения в контрол.
     @param control контрол, куда будет установлено значение.
     Если контрол по совместительству ICustomValueControl, значение будет установлено через setCustomValue
     @param value значение, которое будет установлено в контрол
     */
    public static void setValue( Object control, Object value ) {
        if ( control == null ) {
            return;
        }
        if ( control instanceof JInvRadioGroup ) {
            ( (JInvRadioGroup) control ).valueProperty().set( TypeConverter.convertToString( value, null ) );
        }
        else if ( control instanceof ICustomValueControl ) {
            ( (ICustomValueControl) control ).setCustomValue( value );
        } else if ( control instanceof Label ) {
            ( (Label) control ).setText( U.nvl( value, S.EMPTY_STRING ).toString() );
        } else if ( control instanceof JInvValueField ) {
            ( (JInvValueField) control ).setValue( TypeConverter.convert( value, ( (JInvValueField) control ).getClassValue() ) );
        } else if ( control instanceof TextInputControl ) {
            ( (TextInputControl) control ).setText( TypeConverter.convert( value, String.class ) );
        } else if ( control instanceof CheckBox ) {
            if ( control instanceof JInvCheckBoxString ) {
                ( (JInvCheckBoxString) control ).valueProperty()
                        .set( TypeConverter.convert( value, String.class ) );
            } else {
                ( (CheckBox) control ).setSelected( U.nvl( TypeConverter.convert( value, Boolean.class ), Boolean.FALSE ) );
            }
        } else if ( control instanceof DatePicker ) {
            if ( control instanceof JInvCalendarTime ) {
                ( (JInvCalendarTime) control ).setDateTimeValue( TypeConverter.convert( value, LocalDateTime.class ) );
            } else if ( control instanceof JInvCalendar ) {
                ( (DatePicker) control ).setValue( TypeConverter.convert( value, LocalDate.class ) );
            }
        } else if ( control instanceof JInvComboBox ) {
            ( (JInvComboBox) control ).setSelectedValue( value );
        } else if ( control instanceof ComboBox ) {
            ( (ComboBox) control ).setValue( value );
        } else {
            throw new RuntimeException( "Unsupported class control: " + control.getClass().getName() );
        }
    }

    public static <T> T getValue( Node control ) {
        return getValue( (Object) control );
    }

    /**
     Метод возвращает значение из контрола
     <p>
     @param control Контрол, из которого возвращается значение
     Если контрол по совместительству ICustomValueControl, значение будет взято через getCustomValue
     @return значение из переданного контрола
     */
    public static <T> T getValue( Object control ) {
        if ( control == null ) {
            return null;
        }
        Object result = null;
        if ( control instanceof ICustomValueControl ) {
            result = ( (ICustomValueControl) control ).getCustomValue();
        } else if ( control instanceof Label ) {
            result = ( (Label) control ).getText();
        } else if ( control instanceof JInvValueField ) {
            result = ( (JInvValueField) control ).getValue();
        } else if ( control instanceof TextInputControl ) {
            String text = ( (TextInputControl) control ).getText();
            if ( S.isNullOrEmpty( text ) ) {
                text = null;
            }
            result = text;
        } else if ( control instanceof CheckBox ) {
            if ( control instanceof JInvCheckBoxString ) {
                result = ( (JInvCheckBoxString) control ).getValue();
            } else {
                result = ( (CheckBox) control ).isSelected();
            }
        } else if ( control instanceof DatePicker ) {
            if(control instanceof JInvCalendarTime) {
                result = ((JInvCalendarTime)control).getDateTimeValue();
            } else {
                result = ((DatePicker)control).getValue();
            }
        } else if ( control instanceof ComboBox ) {
            result = ( (ComboBox) control ).getValue();
        } else if ( control instanceof JInvChoiceBox ) {
            result = ( (JInvChoiceBox) control ).getValue();
        } else if ( control instanceof JInvTable ) {
            result = ( (JInvTable) control ).getDataSetAdapter();
        } else {
            throw new RuntimeException( Tags.PRODUCT_LABEL +
                    "Unsupported class control: " +
                    control.getClass().getName() );
        }
        if ( result instanceof String && S.isNullOrEmpty( (String) result ) ) {
            return null;
        }
        return (T)result;
    }

    /**
     Метод возвращает значение из строковое значение контрола из поля ввода.
     При этом это значение может быть в сам контрол неустановлено из-за ошибок конверсии
     @param control Контрол, из которого возвращается значение
     @return значение из переданного контрола
     */
    public static String getStringValueFromEditor( Node control ) {
        if ( control == null ) {
            return null;
        }
        String result = null;
        if ( control instanceof Label ) {
            result = ( (Label) control ).getText();
        } else if ( control instanceof JInvValueField ) {
            result = ( (JInvValueField) control ).getText();
        } else if ( control instanceof TextInputControl ) {
            String text = ( (TextInputControl) control ).getText();
            if ( S.isNullOrEmpty( text ) ) {
                text = null;
            }
            result = text;
        } else if ( control instanceof CheckBox ) {
            result = ( (Boolean) ( (CheckBox) control ).isSelected() ).toString();
        } else if ( control instanceof DatePicker ) {
            result = ( (DatePicker) control ).getEditor().getText();
        } else if ( control instanceof ComboBox ) {
            Object value = ( (ComboBox) control ).getValue();
            if ( value != null && value != U.EMPTY ) {
                result = TypeConverter.convertToString( value, null );
            }
        } else {
            throw new RuntimeException( "Unsupported class control: " + control.getClass().getName() );
        }
        if ( result instanceof String && result.isEmpty() ) {
            return null;
        }
        return result;
    }

    /**
     Метод дизаблирует переданный контрол: контролы Button делаются невидимыми,
     текстовые контролы делаются нередактируемыми,
     для всех других вызывается метод disableControlDefault
     @param control контрол для дизаблирования
     */
    public static void disableControl( Node control ) {
        disableControl( control, true );
    }
    public static void disableControl( Control control ) {
        disableControl( (Node)control, true );
    }
    /**
     Метод включает переданный контрол обратно после disableControl: контролы Button делаются видимыми, текстовые
     контролы делаются
     редактируемыми, для всех других вызывается метод disableControlDefault
 * @param control контрол для включения
     */
    public static void enableControl( Node control ) {
        disableControl( control, false );
    }
    public static void enableControl( Control control ) {
        disableControl( (Node)control, false );
    }

    /**
     Метод дизаблирует переданный контрол:
     <il>
     <li>Контролы Button делаются невидимыми</li>
     <li>Текстовые контролы делаются нередактируемыми</li>
     <li>Для всех других вызывается метод disableControlDefault</li>
     </il>
     <p>
     Сделана переделка под Control -> Node, из-за JInvXMLViewer и других компонентов построенных не на Control. Sulimoff, 01.12.25

     @param control контрол для дизаблирования
     @param disable {@code}true{@code} выключен, {@code}false{@code} включён
     */
    public static void disableControl( Node control, boolean disable )
    {
        if( control == null
            || control instanceof Label
            || control instanceof ScrollPane
            || control instanceof SplitPane
            || control instanceof TabPane
            || control instanceof TitledPane
            || control instanceof ScrollBar
            || control instanceof JInvTable
        )
            return;

        if( control instanceof JInvXMLViewer )
        {
            final JInvXMLViewer xv = (JInvXMLViewer)control;
            xv.setReadOnly( disable );
            return;
        }

        if( control instanceof Button )
        {
            if(!control.visibleProperty().isBound() ) {
                control.setVisible( !disable );
                return;
            }
        }
        else if ( control instanceof TextInputControl ) {
            ( (TextInputControl) control ).setEditable( !disable );
        }
        else if ( control instanceof DatePicker ) {
            ( (DatePicker) control ).setEditable( !disable );
        }
        else if ( control instanceof ChoiceBox || control instanceof ComboBoxBase )
        {
            control.setFocusTraversable( !disable );
            //Не даём кликать
            Stream.of( MouseEvent.MOUSE_CLICKED, MouseEvent.MOUSE_PRESSED, MouseEvent.MOUSE_RELEASED)
                  .forEach( et -> {
                      if( disable )
                      {
                        control.addEventFilter( et, consumeFilter );
                      } else {
                        control.removeEventFilter( et, consumeFilter );
                      }
                  } );
        }
        else
        {
            control.setFocusTraversable( !disable );
            control.setMouseTransparent( disable );
        }

        control.pseudoClassStateChanged( DISABLED_GREY_STATE, disable );
    }

    /** */
    public static void disableControl( Control control, boolean disable ) {
        disableControl( (Node)control, disable );
    }

        /**
         Возвращает экземлпяр {@link DSFXAdapter}, в случае если он установлен для контрола
         <p>
         @param <T> генерик-параметр для {@link DSFXAdapter}
         @param component контрол, в котором будет осуществляться поиск адаптера
         @return экземлпяр {@link DSFXAdapter}, установленный для переданного контролла
         */
    public static <T> DSFXAdapter<T> getDsAdapterFromControl( Object component ) {
        DSFXAdapter<T> result = null;
        if ( component != null ) {
            Object adapter = null;
            if ( component instanceof IJInvControl ) {
                IJInvControl ic = (IJInvControl) component;
                adapter = ic.getProperty( PROPERTY_DATA_SET_ADAPTER );
            } else if ( component instanceof Control ) {
                Control control = (Control) component;
                adapter = control.getProperties().getOrDefault( PROPERTY_DATA_SET_ADAPTER, null );
            }
//            } else if (component instanceof JInvRadioGroup) {
//                adapter = ((JInvRadioGroup) component).getProperties().getOrDefault(PROPERTY_DATA_SET_ADAPTER, null);
//            }
            if ( adapter != null && adapter instanceof DSFXAdapter ) {
                result = (DSFXAdapter<T>) adapter;
            }
        }
        return result;
    }

    /**
     Метод возвращает признак того, что фильтр по колонке допустим.
     <p>
     Не фильтруются колонки пометки и колонки с транзиентными полями. Данные признаки устанавливаются в момент подготовки модели столбцов.
     @param col Колонка таблицы
     @return допустим ли фильтр по колонке
     */
    public static boolean isColumnSupportFilter( TableColumn col ) {
        return !(Boolean) col.getProperties().getOrDefault( JInvTableColumn.COLUMN_MARK, Boolean.FALSE ) &&
                !(Boolean) col.getProperties().getOrDefault( JInvTableColumn.COLUMN_TRANSIENT, Boolean.FALSE );
    }

    /**
     Метод возвращает привязанное имя поля из запроса к колонке, если оно пустое возвращает id колонки
     @param column Колонка таблицы
     @return fieldName из колонки, если оно пустое то fxId колонки
     */
    public static String getFieldNameFromTableColumn( TableColumn column ) {
        String fieldName = null;
        if ( column instanceof JInvTableColumn ) {
            fieldName = ( (JInvTableColumn) column ).getFieldName();
        }
        if ( fieldName == null || fieldName.isEmpty() ) {
            fieldName = column.getId();
        }
        return fieldName;
    }

    /**
     Возвращает наиболее подходящее название столбца.
     Если название слишком короткое – пробует взять тултип
     */
    public static String getTitleFromTableColumn(TableColumn column) {

        String title = Controls.getFieldNameFromTableColumn(column); //SQL имя столбца или fx:id

        String columnHeader = column.getText();

        if( S.isNotNullOrEmpty(columnHeader) ) {
            title = columnHeader; //имя столбца из textProperty
        }

        if (columnHeader == null && column.getGraphic() != null && column.getGraphic() instanceof Label) {
            columnHeader = ((Label) column.getGraphic()).getText();
        }

        if ( column instanceof JInvTableColumn ) {
            JInvTableColumn invColumn = (JInvTableColumn) column;
            JInvFXFormController<?> controller = ( (JInvTable) invColumn.getTableView() ).getController();

            if ( S.isNotNullOrEmpty(columnHeader) ) {
                title = columnHeader; //если кто-то присвоил лейбл с текстом в качестве заголовка
            } else if ( controller != null && S.isNotNullOrEmpty(invColumn.getFieldName()) ) {
                String fromBundle = controller.getBundleString(invColumn.getFieldName());
                if (S.isNotNullOrEmpty(fromBundle)) {
                    title = fromBundle; //имя из бандла по ключу
                }
            }

            //Если название слишком короткое, стараемся взять тултип
            if ( S.isNotNullOrEmpty(invColumn.getToolTipText()) && title.length() < 6 ) {
                title = invColumn.getToolTipText(); //тултип
            }
        } else {
            if ( S.isNotNullOrEmpty(columnHeader) ) {
                title = columnHeader; //если кто-то присвоил лейбл с текстом в качестве заголовка
            }
        }

        return title;
    }

    /**
     * Редактируемый ли переданный контрол
     * @return по умолчанию true
     */
    public static boolean isEditable( Control control ){
        Optional<BooleanProperty> editableProperty = editableProperty(control);
        if (editableProperty.isPresent()) {
            return editableProperty.get().getValue();
        }
        return true;
    }

    /**
     * Возвращает editable проперть у контрола, если она есть
     */
    public static Optional<BooleanProperty> editableProperty( Control control ){
        if (control instanceof JInvTextField){
            return Optional.of(((JInvTextField) control).editableOrLovEditableProperty());
        }
        if (control instanceof TextInputControl){
            return Optional.of(((TextInputControl) control).editableProperty());
        }
        return Optional.empty();
    }

    /**
     Метод возвращает привязанное имя поля из запроса к компоненту
     @param component Контрол, в котором ищется имя поля
     @return имя поля привязанное к компоненту
     */
    public static String getFieldNameFromControl( Object component ) {

        String fieldName = null;

        if( component instanceof Control )
        {
            if( component instanceof IJInvControl ) {
                fieldName = ( (IJInvControl) component ).getFieldName();
            }

            if ( fieldName == null ) {
                fieldName = (String) ( (Control) component ).getProperties().get( Controls.CONTROL_FIELD_NAME );
            }

            if ( fieldName == null && component instanceof JInvLabel) {
                fieldName = ((JInvLabel) component).getLinkFieldName();
            }

            if ( fieldName == null ) {

                try {
                    fieldName = getPropertyNameFromControl( (Control) component );
                } catch ( JInvStringWorkerException ex ) {
                    fieldName = null;
                }

                if ( fieldName != null ) {
                    throw new UnsupportedOperationException( bundle.getString( "OSHIBKA_IMYA_POLYA_V_POLE_PROPMT_TEXT" ) );
                }
//                fieldName = getPropertyNameAndClearFromControl((Control) component);
            }
        }
        else
            if ( component instanceof IJInvControl )
            {
                fieldName = ( (IJInvControl) component ).getFieldName();
            }

        if( fieldName != null && fieldName.isEmpty() ) {
            fieldName = null;
        }

        return fieldName;
    }

    private static String getPropertyNameFromControl( Control control ) throws JInvStringWorkerException {
        String promptText = getPromptTextFromControl( control );
        String fieldName = null;
        if ( promptText != null ) {
            fieldName = JInvStringWorker.INSTANCE().matchCaseRegExpOrNull( "\\{(.*)\\}", 1, promptText );
        }
        return fieldName;
    }

    private static String getPromptTextFromControl( Control control ) {
        String promptText = null;
        if ( control instanceof TextInputControl ) {
            TextInputControl c = (TextInputControl) control;
            promptText = c.getPromptText();
        } else if ( control instanceof DatePicker ) {
            DatePicker cal = (DatePicker) control;
            promptText = cal.getPromptText();
        } else if ( control instanceof CheckBox ) {
            CheckBox ch = (CheckBox) control;
            promptText = ch.getText();
        }
        return promptText;
    }

    private static String getPropertyNameAndClearFromControl( Control control ) {
        String result = null;
        try {
            result = getPropertyNameFromControl( control );
            clearPropertyNameInControl( control );
            control.getProperties().put( Controls.CONTROL_FIELD_NAME, result );
        } catch ( Throwable ex ) {
            result = null;
        }
        return result;
    }

    public static void clearPropertyNameInControl( Control control ) {
        String promptText = getPromptTextFromControl( control );
        if ( control instanceof TextInputControl ) {
            TextInputControl c = (TextInputControl) control;
            c.setPromptText( promptText.replaceAll( "\\{(.*)\\}", "" ) );
        } else if ( control instanceof DatePicker ) {
            DatePicker cal = (DatePicker) control;
            cal.setPromptText( promptText.replaceAll( "\\{(.*)\\}", "" ) );
        } else if ( control instanceof CheckBox ) {
            CheckBox ch = (CheckBox) control;
            ch.setText( promptText.replaceAll( "\\{(.*)\\}", "" ) );
        }
    }

    /**
     Метод поиска экземпляра контроллера {@link AbstractBaseController} в переданном <code>Control</code>.
     Если у текущего компонента в свойствах явно не указан контроллер, производится поиск по всем его
     родителям до тех пор, пока контроллер не будет найден, либо родитель не будет пустым
     @param control Контрол, в котором ищется контроллер
     @return Контроллер, привязанный к контролу, либо к его родителю
     */
    public static <C extends AbstractBaseController<?>> C getControllerFromControl( Node control ) {

        ReadOnlyObjectProperty<C> property = getControllerProperty( control );

        if( property != null )
            return property.get();

        return null;
    }

    public static <C extends AbstractBaseController<?>> ReadOnlyObjectProperty<C> getControllerProperty( Node control )
    {
        final Stream<Parent> streamOfParent = getParentStreamOfControl(control);

        Optional<ReadOnlyObjectProperty<C>> opt
            = streamOfParent
                .filter(t -> t.getProperties().getOrDefault(JInvFXFormController.PROPERTY_CONTROLLER, null) != null)
                    .findFirst().map(t -> (ReadOnlyObjectProperty<C>) t.getProperties().get(JInvFXFormController.PROPERTY_CONTROLLER));

        return opt.orElse(null);

        /*
        Parent parent = getParentStreamOfControl( control ).filter( ( Parent t ) -> (t instanceof Pane || t instanceof SplitPane) &&
                        t.getProperties().getOrDefault( JInvFXFormController.PROPERTY_CONTROLLER, null ) != null ).
                findFirst().orElse( null );
        if ( parent != null ) {
            return (ReadOnlyObjectProperty<C>) parent.getProperties().get( JInvFXFormController.PROPERTY_CONTROLLER );
        } else {
            return null;
        }
        */
    }

    /**
     Возвращается {@link Stream] родителей {@link Parent} переданного компонента
    @param control компонент для поиска всех его родителей
    @return {@link Stream] родителей компонента
     */
    public static Stream<Parent> getParentStreamOfControl( Node control ) {

        if( control == null )
            return Stream.empty();

        final List<Parent> list = new ArrayList<>();

        if( control instanceof Parent )
            list.add( (Parent) control );

        fillParentList( control.getParent(), list );

        return list.stream();
    }

    /**
     Рекурсивный метод поиска родителей компонента
     */
    private static void fillParentList( Parent parent, List<Parent> list ) {

        if( parent != null )
        {
            list.add( parent );
            fillParentList( parent.getParent(), list );
        }
    }

    private static void helperGetListControlsInternal( List<Control> controlList, Node n, Predicate<Control> predicat ) {
        if ( n instanceof ButtonBar || n instanceof Pane ) {
        } else if ( n instanceof Control && predicat.test( (Control) n ) ) {
            controlList.add( (Control) n );
        }
        if ( n instanceof SplitPane ) {
            SplitPane pane = (SplitPane) n;
            pane.getItems().stream().forEach( item -> {
                helperGetListControlsInternal( controlList, item, predicat );
            } );
        } else if ( n instanceof TabPane ) {
            ( (TabPane) n ).getTabs().forEach( ( Tab t ) -> {
                helperGetListControlsInternal( controlList, t.getContent(), predicat );
            } );
        } else if ( n instanceof ScrollPane ) {
            helperGetListControlsInternal( controlList, ( (ScrollPane) n ).getContent(), predicat );
        } else if ( n instanceof TitledPane ) {
            helperGetListControlsInternal( controlList, ( (TitledPane) n ).getContent(), predicat );
        } else if ( n instanceof Parent && !( n instanceof JInvCalendar ) &&
                !( n instanceof Spinner ) &&
                !( n instanceof JInvXMLViewer ) &&
                !( n instanceof JInvTable ) &&
                !( n instanceof JInvTextField ) ) {
            ( (Parent) n ).getChildrenUnmodifiable().stream().forEach( child -> {
                helperGetListControlsInternal( controlList, child, predicat );
            } );
        }
    }

    private static void helperGetListNodeInternal( List<Node> nodeList, Node n, Predicate<Node> predicat ) {
        if ( predicat.test( n ) ) {
            nodeList.add( n );
        }
        if ( n instanceof SplitPane ) {
            SplitPane pane = (SplitPane) n;
            pane.getItems().stream().forEach( item -> {
                helperGetListNodeInternal( nodeList, item, predicat );
            } );
        } else if ( n instanceof TabPane ) {
            ( (TabPane) n ).getTabs().forEach( ( Tab t ) -> {
                helperGetListNodeInternal( nodeList, t.getContent(), predicat );
            } );
        } else if ( n instanceof ScrollPane ) {
            helperGetListNodeInternal( nodeList, ( (ScrollPane) n ).getContent(), predicat );
        } else if ( n instanceof TitledPane ) {
            helperGetListNodeInternal( nodeList, ( (TitledPane) n ).getContent(), predicat );
        } else if ( n instanceof Parent && !( n instanceof JInvCalendar ) &&
                !( n instanceof Spinner ) &&
                !( n instanceof JInvXMLViewer ) &&
                !( n instanceof JInvTable ) &&
                !( n instanceof JInvTextField ) ) {
            ( (Parent) n ).getChildrenUnmodifiable().stream().forEach( child -> {
                helperGetListNodeInternal( nodeList, child, predicat );
            } );
        }
    }

    /**
     Метод поиска контролов в компоненте родителе (например в Pane).
     <p>
     Получив список всех контролов происходит фильтрация по переданному предикату. Если условие предиката выполняется контрол добавляется в результирующий лист. Есть особенности реализации: не
     происходит поиск внутри компонентов JInvCalendr Spinner JInvTable, также в результирующий лист не добавляются компоненты: ButtonBar
     */
    public static List<Control> getControlList( Parent parent, Predicate<Control> predicat ) {
        List<Control> controlList = new LinkedList<>();
        helperGetListControlsInternal( controlList, parent, predicat == null ? c -> {
            return true;
        } : predicat );
        return controlList;
    }

    /**
     См. {@link#getControlList(javafx.scene.Parent, java.util.function.Predicate) }
     @param clazz Если аргумент не пустой, то генерится предикат на основе него, при этом другие предикаты перезатираются
     */
    public static List<Control> getControlList( Parent parent, Predicate<Control> predicat, Class clazz ) {
        if ( predicat == null ) {
            return getControlList( parent, ( Control t ) -> clazz.isAssignableFrom( t.getClass() ) );
        } else {
            return getControlList( parent, predicat );
        }
    }

    /**
     Метод поиска компонентов в компоненте родителе (например в Pane).
     */
    public static List<Node> getNodeList( Parent parent, Predicate<Node> predicat ) {
        List<Node> controlList = new LinkedList<>();
        helperGetListNodeInternal( controlList, parent, predicat == null ? c -> {
            return true;
        } : predicat );
        return controlList;
    }

    /**
     Метод поиска компонента в компоненте родителе по имени поля fieldName (например в Pane).
     */
    public static Node getNodeByFieldName( Parent parent, String fieldName ) {
        if ( S.isNullOrEmpty( fieldName ) ) {
            throw new RuntimeException( "field name in null" );
        }
        List<Node> controlList = getNodeList( parent, new Predicate<Node>() {
            @Override
            public boolean test( Node t ) {
                return t instanceof IJInvControl &&
                        S.isNotNullOrEmpty( ( (IJInvControl) t ).getFieldName() ) &&
                        ( (IJInvControl) t ).getFieldName().equals( fieldName );
            }
        } );
        if ( controlList.size() > 1 ) {
            throw new RuntimeException( "There is several controls with textField " + fieldName );
        } else if ( controlList.isEmpty() ) {
            return null;
        } else {
            return controlList.get( 0 );
        }
    }

    /**
     Иногда возникает необходимость хранить прикладные компоненты, не являющиеся составными частями сцены.
     Метод добавляет component в коллекцию компонентов contentPane. При отсутствии таковой создает ее.
     */
    public static void addNonControlComponentToContainer( Object component, Parent contentPane ) {
        if ( component != null && contentPane != null ) {
            List<Object> list = getNonControlComponentFromContainer( contentPane );
            list.add( component );
        }
    }

    /**
     Возвращает коллекцию прикладных компонентов из контейнера(панели).
     */
    public static List<Object> getNonControlComponentFromContainer( Parent contentPane ) {
        List<Object> list = new ArrayList<>();
        List<Object> listNonControlComponent = (List<Object>) contentPane.getProperties()
                .getOrDefault( LIST_NON_CONTROL_COMPONENT, null );
        if ( listNonControlComponent == null ) {
            contentPane.getProperties().put( LIST_NON_CONTROL_COMPONENT, list );
        } else {
            list = listNonControlComponent;
        }
        return list;
    }

    /**
     Возвращает текст тултипа из bundle файла по указанному fieldName
     @param fieldName Имя поля привязанного к компоненту, тултип для которого необходимо найти
     @param bundle Bundle-файл для поиска тултипа
     @return Optional<String> результат поиска тултипа
     */
    public static Optional<String> getTooltipFromBundleByFieldName( String fieldName, ResourceBundle bundle ) {
        Optional<String> result = Optional.empty();
        String toolTipKey = fieldName + "_TOOLTIP";
        if ( !U.containsNull( fieldName, bundle ) && bundle.containsKey( toolTipKey ) ) {
            result = Optional.ofNullable( bundle.getString( toolTipKey ) );
        }
        return result;
    }

    /** */
    public static Node getControlByClass( Class clazz, String fieldName ) {

        IJInvControl result = null;

        if ( clazz != null )
        {
            if( clazz.equals( String.class ) ) {
                result = new JInvTextField();
            } else if ( clazz.equals( LocalDate.class ) ) {
                result = new JInvCalendar();
            } else if ( clazz.equals( Boolean.class ) ) {
                result = new JInvCheckBox();
            } else if ( clazz.equals( LocalDateTime.class ) ) {
                result = new JInvCalendarTime();
            } else if ( clazz.equals( BigDecimal.class ) ) {
                result = new JInvBigDecimalField();
            } else if ( clazz.equals( Long.class ) ) {
                result = new JInvLongField();
            } else if ( clazz.equals( Integer.class ) ) {
                result = new JInvIntegerField();
            }
            else if ( clazz.equals( LocalTime.class ) ) {
                result = new JInvTimeFieldNew();
            }
        }

        if( result != null )
            result.setFieldName( fieldName );

        return (Node) result;
    }

    public static JInvTableColumn getColumnByClass( Class clazz, String fieldName, StringProperty titleProperty ) {
        JInvTableColumn result = null;
        if ( clazz != null ) {
            if ( clazz.equals( String.class ) ) {
                result = new JInvTableColumn();
            } else if ( clazz.equals( LocalDate.class ) ) {
                result = new JInvTableColumnDate();
            } else if ( clazz.equals( LocalDateTime.class ) ) {
                result = new JInvTableColumnDate();
            } else if ( clazz.equals( Boolean.class ) ) {
                result = new JInvTableColumnBoolean();
            } else if ( clazz.equals( BigDecimal.class ) ) {
                result = new JInvTableColumnBigDecimal();
            } else {
                result = new JInvTableColumn();
            }
            result.setFieldName( fieldName );
            result.textProperty().bind( titleProperty );
        }
        return result;
    }

    /**
     *
     */
    private static <T> void helperGetColumns( List<TableColumn<T, ?>> tableColumns, List<TableColumn<T, ?>> listColumns, boolean all ) {
        tableColumns.stream().filter( c -> !( c == null ) ).forEach( c -> {
            if ( c.getColumns().isEmpty() ) {
                listColumns.add( c );
            } else {
                if ( all ) {
                    listColumns.add( c );
                }
                helperGetColumns( c.getColumns(), listColumns, all );
            }
        } );
    }

    /**
     *
     */
    public static <T> List<TableColumn<T, ?>> getLeafColumnsFromTable( TableView<T> table ) {
        List<TableColumn<T, ?>> leafColumns = new ArrayList<>();
        helperGetColumns( table.getColumns(), leafColumns, false );
        return leafColumns;
    }

    /**
     *
     */
    public static <T> List<TableColumn<T, ?>> getAllColumnsFromTable( TableView<T> table ) {
        List<TableColumn<T, ?>> leafColumns = new ArrayList<>();
        helperGetColumns( table.getColumns(), leafColumns, true );
        return leafColumns;
    }

    public static <T> List<TreeTableColumn<T, ?>> getAllColumnsFromTreeTable( TreeTableView<T> table ) {
        //? do leaf columns exist for TreeTables?
        return new ArrayList<>( table.getColumns() );
    }

    public static Long getSerialVersionUID( Class c ) {
        if( c != null )
        {
            ObjectStreamClass l = ObjectStreamClass.lookup(c);
            if( l != null )
                return l.getSerialVersionUID();
        }
        return null;
    }

    public static void scrollToNode( Node node, ScrollPane scroll ) {
//        double hGap = 5;
        double topViewPort = scroll.getViewportBounds().getMinY() * -1;
        double bottomViewPort = topViewPort + scroll.getViewportBounds().getHeight();
//        double heightOfControl = node.getBoundsInLocal().getHeight();
//        double buttomPositionOfControl = (heightOfControl + hGap) * (rowIndex + 1);
        double buttomPositionOfControl = getRecursiveBoundsOfParent( node, node.getBoundsInLocal(), scroll.getContent() )
                .getMaxY();
        if ( buttomPositionOfControl < topViewPort || buttomPositionOfControl > bottomViewPort ) {
            scroll.setVvalue( buttomPositionOfControl / scroll.getContent().getBoundsInLocal().getHeight() );
//            System.out.println("невидимый");
        } else {
//            System.out.println("видимый");
        }
    }

    public static void scrollToNode( Node control ) {
        Controls.getParentStreamOfControl(control)
                .filter((Parent t) -> t instanceof ScrollPane)
                .findFirst().ifPresent(parent -> Controls.scrollToNode(control, (ScrollPane) parent));
    }

    public static Bounds getRecursiveBoundsOfParent( Node node, Bounds bounds, Node parent ) {
        Bounds boundsInParent = node.localToParent( bounds );
        Parent parentOfNode = node.getParent();
        if ( parentOfNode.equals( parent ) ) {
            return boundsInParent;
        } else {
            return getRecursiveBoundsOfParent( parentOfNode, boundsInParent, parent );
        }
    }

    public static void setTooltipStartTiming( Tooltip tooltip, int timeInMilisec ) throws IllegalArgumentException, IllegalAccessException, NoSuchFieldException {
        Field fieldBehavior = tooltip.getClass().getDeclaredField( "BEHAVIOR" );
        fieldBehavior.setAccessible( true );
        Object objBehavior = fieldBehavior.get( tooltip );
        Field fieldTimer = objBehavior.getClass().getDeclaredField( "activationTimer" );
        fieldTimer.setAccessible( true );
        Timeline objTimer = (Timeline) fieldTimer.get( objBehavior );
        objTimer.getKeyFrames().clear();
        objTimer.getKeyFrames().add( new KeyFrame( new Duration( timeInMilisec ) ) );
    }

    /** Возвращает признак проведенной валидации у компонента */
    public static boolean isControlValidated( Control control ) {
        return Controls.<Boolean>getProperty( control, CONTROL_VALIDATED, false );
    }

    /** Устанавливает признак успешно проведенной валидации на компоненте */
    public static void setControlValidated(Control control, boolean val) {
        if (control != null) {
            control.getProperties().put(CONTROL_VALIDATED, val);
        }
    }

    /** Возвращает свойство с признаком изменённого значения на переданной Node */
    public static BooleanProperty getValueChangedProperty(Node control ) {
        BooleanProperty bp = (BooleanProperty) control.getProperties().get( CONTROL_VALUE_CHANGED );

        if( bp == null ) {
            bp = new SimpleBooleanProperty( control, "controlValueChanged" );
            control.getProperties().put( CONTROL_VALUE_CHANGED, bp );
        }
        return bp;
    }

    /** Устанавливает признак измененного значение на переданной Node */
    public static void setControlValueChanged(Node control, boolean val) {
        if (control != null) {
            getValueChangedProperty( control ).setValue( val );
        }
    }

    /** Возвращает признак изменённого значения на переданной Node */
    public static boolean isControlValueChanged(Node control) {
        if (control != null) {
            return getValueChangedProperty( control ).getValue();
        } else {
            return false;
        }
    }
}
