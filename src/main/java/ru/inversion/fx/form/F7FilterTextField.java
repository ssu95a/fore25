package ru.inversion.fx.form;

import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.control.IndexRange;
import javafx.scene.paint.Color;
import ru.inversion.dataset.IFilterItem;
import ru.inversion.dataset.fx.F7FilterItem;
import ru.inversion.fx.app.BaseApp;
import ru.inversion.fx.app.Tags;
import ru.inversion.fx.form.controls.JInvCheckBox;
import ru.inversion.fx.form.controls.JInvTextField;
import ru.inversion.icons.IconDescriptorBuilder;
import ru.inversion.icons.IconFactory;
import ru.inversion.icons.enums.FontAwesome;
import ru.inversion.icons.enums.IconSize;
import ru.inversion.utils.S;
import ru.inversion.utils.U;
import ru.inversion.utils.converter.TypeConverter;

import java.math.BigDecimal;
import java.sql.Types;
import java.time.temporal.Temporal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static ru.inversion.dataset.IFilterItem.ValueTypeEnum.*;
import static ru.inversion.fx.form.controls.Controls.CONTROL_FIELD_NAME;

/** */
public class F7FilterTextField extends JInvTextField {

    static private final ResourceBundle bundle = ResourceBundle.getBundle("fore");

    static final private Color colorIndexBkg = Color.valueOf( BaseApp.APP().viewPrefService( ).getColorIndexSearch());
    static final private Color colorListBkg  = Color.valueOf( BaseApp.APP().viewPrefService( ).getColorListSearch() );
    static final private Color colorExprBkg  = Color.valueOf( BaseApp.APP().viewPrefService( ).getColorExprSearch() );

    /** Иконки на кнопы
    final private Node[] icons = new Node[] {
        IconFactory.getLabel( new IconDescriptorBuilder().iconId(FontAwesome.fa_vimeo   ).iconSize(IconSize.SMALL).color(Color.BLACK ).build() ),
        IconFactory.getLabel( new IconDescriptorBuilder().iconId(FontAwesome.fa_bars    ).iconSize(IconSize.SMALL).color(colorListBkg.darker()).build() ),
        IconFactory.getLabel( new IconDescriptorBuilder().iconId(FontAwesome.fa_database).iconSize(IconSize.SMALL).color(colorExprBkg.darker()).build() )
    };
    */

    static final private Pattern DECPattern = Pattern.compile("(^(([<> ]?-?\\d+\\.?\\d*)|([\\d_%\\.]+))$)");
    static final private Pattern INTPattern = Pattern.compile("(^(([<> ]?-?\\d+)|([\\d_%]+))$)"           );

    static final public  String indexSearchStyle = "-fx-control-inner-background: " + BaseApp.APP().viewPrefService( ).getColorIndexSearch() + ";";
    static final private String listStyle = "-fx-control-inner-background: "+ BaseApp.APP().viewPrefService( ).getColorListSearch() +";";
    static final private String exprStyle = "-fx-control-inner-background: "+ BaseApp.APP().viewPrefService( ).getColorExprSearch() +";";

//    final static private Background indexBkg = new Background( new BackgroundFill( colorIndexBkg, null, null ));
//    final static private Background listBkg  = new Background( new BackgroundFill( colorListBkg, null, null ));
//    final static private Background exprBkg  = new Background( new BackgroundFill( colorExprBkg, null, null ));

    /** */
    final private ChangeListener<String> valueListener = new ChangeListener< String >() {
        @Override
        public void changed( ObservableValue< ? extends String > o, String oldValue, String newText ) {

            if( S.isNullOrEmpty(newText ) )
            {
                setState( State.NULL );
            }
            else
            {
                if( externalValueControl )
                {
                    setState( State.VALUE );
                }
                else
                {
                    if( containsListDlmtr(newText) )
                        setValueType(LIST);
                    else if( containsExprSymb(newText) )
                        setValueType(EXPRESSION);
                    else
                    {
                        if( value_checkText(newText) )
                            setState( State.VALUE );
                        else
                            setState( State.ERROR );
                    }
                }
            }
        }
    };

    final private ChangeListener<String> listListener = new ChangeListener< String >() {
        @Override
        public void changed( ObservableValue< ? extends String > o, String oldText, String newText ) {

            if( containsListDlmtr(newText) )
                setState( State.VALUE );
            else
                setValueType(VALUE);
        }
    };
    final private ChangeListener<String> exprListener = new ChangeListener< String >() {
        @Override
        public void changed( ObservableValue< ? extends String > o, String oldValue, String newText ) {
            setState( S.isNullOrEmpty(newText) ? State.NULL : State.VALUE );   
        }
    };

    /** Элемент фильтра для которого сделано поле ввода значения */
    final private F7FilterItem filterItem;

    /** Слушатель для проверки того что ввели */
    private ChangeListener<String> currentTextListener = valueListener;

    /** Текущий тип данных в поле  */
    final private ReadOnlyObjectWrapper<IFilterItem.ValueTypeEnum> valueTypeProperty = new ReadOnlyObjectWrapper<>( this, "valueType" );

    /** Признак что в режиме заведения значения будет использоваться другой контрол */
    final private boolean externalValueControl;

    /** */
    public F7FilterTextField( F7FilterItem filterItem, boolean externalValueControl ) {
        this.filterItem           = filterItem;
        this.externalValueControl = externalValueControl;
        getProperties().put  ( CONTROL_FIELD_NAME, filterItem.getColumn() );
        valueTypeProperty.addListener(( o, old, newType ) -> onChangeValueType(newType));
        valueTypeProperty.set( VALUE );
    }

    /** */
    public void setValue( IFilterItem.ValueTypeEnum vt, String value ) {
        setText     ( value );
        setValueType( vt );
    }

    /** */
    private boolean isLastDlmtr( String s ) {

        if( S.isNullOrEmpty(s) )
            return false;

        for( int i = s.length() - 1; i >=0; i--)
        {
            char ch = s.charAt(i);

            if( Character.isSpaceChar(ch) )
                continue;

            if( ch == ',' || ch == ';' )
                return true;

            break;
        }

        return false;
    }

    /** */
    transient private IndexRange selection;

    /** */
    @Override
    protected String getTextForLov( )
    {
        selection = getSelection();
        return getValueType() == VALUE ? super.getTextForLov() : "%";
    }

    /** */
    @Override
    protected void setTextValueFromLov( ) {

        if( U.in( getValueType(), LIST, EXPRESSION ) )
        {
            String txt = getText();

            if( S.isNullOrEmpty(txt) )
                super.setTextValueFromLov( );
            else
            {
                final String s = TypeConverter.convert( getLOV().getValue(), String.class );

                if( selection != null && selection.getLength() > 0 )
                {
                    replaceText( selection, s );

                    selection = null;
                }
                else
                {
                    if( isLastDlmtr(txt) )
                        setText( txt + s );
                    else
                        setText( txt + "," + s );
                }
                positionCaret( getText().length() );
            }
        }
        else
            super.setTextValueFromLov( );
    }

    /** */
    public ReadOnlyObjectProperty<IFilterItem.ValueTypeEnum> valueTypeProperty() {
        return valueTypeProperty.getReadOnlyProperty();
    }

    /** */
    public IFilterItem.ValueTypeEnum getValueType( ) {
        return valueTypeProperty.get();
    }

    /** */
    public void setValueType( IFilterItem.ValueTypeEnum vt ) {

        if( !isSupportListMode() && vt == LIST )
            vt = VALUE;

        valueTypeProperty.set(vt);
    }

    /** */
    public F7FilterItem getFilterItem( ) {
        return filterItem;
    }

    /** */
    private transient String lastStyle = null;

    /** */
    private transient boolean noCheck = false;

    /** */
    private void onChangeValueType( IFilterItem.ValueTypeEnum vt ) {

        filterItem.setValueType( vt );
        filterItem.setGeneratedExpression(false);

        textProperty( ).removeListener(currentTextListener);

        currentTextListener = U.<IFilterItem.ValueTypeEnum, ChangeListener<String>>decode (
            vt,
                VALUE, valueListener,
                LIST, listListener,
                EXPRESSION, exprListener
        );

        textProperty( ).addListener(currentTextListener);

        if( lastStyle != null ) {
            setStyle(S.EMPTY_STRING);
            lastStyle = null;
        }

        if( vt != VALUE ) {

            lastStyle =
                U.decode (
                    vt,
                        LIST, listStyle,
                        EXPRESSION, exprStyle
                );
        }
        else
            if( getFilterItem().isIndexSearchAllowed() )
                lastStyle = indexSearchStyle;

        if( lastStyle != null )
            setStyle( lastStyle );

        if( checkExpression != null)
        {
            noCheck = true;

            try {
                checkExpression.setSelected( vt == EXPRESSION );
            }finally {
                noCheck = false;
            }
        }

        currentTextListener.changed( null, null, getText() );
    }

    /** */
    private boolean value_checkText( String text ) {

        try {

            Class n = filterItem.getType();

            if( Number.class.isAssignableFrom( n ) ) {
                if( U.in( n, Integer.class, Long.class, Short.class ) ) {
                    Matcher matcher = INTPattern.matcher(text);
                    return matcher.find() && matcher.group(0) != null;
                }
                else
                {
                    if( U.in( n, BigDecimal.class, Double.class, Float.class ) ) {
                        Matcher matcher = DECPattern.matcher(text);
                        return matcher.find() && matcher.group(0) != null;
                    }
                }
            }

            return true;
        }
        catch( Exception e ) {
            return false;
        }
    }

    /** */
    private JInvCheckBox checkExpression;

    /** Создает компонент */
    public Node createToggleNode( ) {

        if( checkExpression != null )
            throw new IllegalStateException( Tags.PRODUCT_LABEL + "Expression check already initialized!" );

        checkExpression = new JInvCheckBox();
        checkExpression.setFocusTraversable( false );
        checkExpression.selectedProperty().addListener( new ChangeListener< Boolean >() {
            @Override
            public void changed( ObservableValue< ? extends Boolean > o, Boolean old, Boolean newValue ) {
                if( !noCheck )
                    setValueType( newValue ? EXPRESSION : determineValueType( getText() ) );
            }
        });

        return checkExpression;
    }

    /** */
    public Optional<JInvCheckBox> getCheckExpression() {
        return Optional.ofNullable(checkExpression);
    }

    /** */
    public static Object toF7List( String s, int type ) {

        if( S.isNullOrEmpty(s) )
            return s;

        boolean wasDelim = true;
        boolean inQuotas = false;
        boolean hasSpecCharacters = false;

        StringBuilder si = new StringBuilder();
        final List<String> items = new ArrayList<>();

        char ch;

        for( int i = 0; i < s.length(); i++ )
        {
            ch = s.charAt(i);

            if( ch == '"' )
            {
                inQuotas = !inQuotas;
                continue;
            }

            if( !inQuotas )
            {
                if( U.inChar( ch, ',', ';', '\n' ) )
                {
                    if( wasDelim )
                        continue;

                    wasDelim = true;
                }
                else
                    wasDelim = false;
            }

            if( wasDelim )
            {
                if( si.length() > 0 )
                {
                    String s1 = si.toString().trim();

                    if( !s1.isEmpty() )
                    {
                        boolean hsc = S.contains( s1, '_', '%' );

                        if(!hasSpecCharacters )
                            hasSpecCharacters = hsc;

                        if( hsc || type == Types.VARCHAR )
                            s1 = String.join( S.EMPTY_STRING, "'", s1, "'" );

                        items.add( s1 );
                    }

                    si = new StringBuilder();
                }

                continue;
            }

            si.append(ch);

        }//end for

        if( si.length() != 0 )
        {
            String s1 = si.toString().trim();

            if( !s1.isEmpty() )
            {
                boolean hsc = S.contains( s1, '_', '%' );

                if(!hasSpecCharacters )
                    hasSpecCharacters = hsc;

                if( hsc || type == Types.VARCHAR )
                    s1 = String.join( S.EMPTY_STRING, "'", s1, "'" );

                items.add( s1 );
            }
        }//end if

        return hasSpecCharacters ? items : String.join( ",", items );
    }

    /** */
    private boolean containsExprSymb( String s ) {

        if( S.isNullOrEmpty(s) )
            return false;

        // Проверяем наличие в '!='
        if( U.in(s.charAt(0),'!'))
        {
            if( s.length() > 1 )
                return U.in(s.charAt(1), '=');

            return false;
        }
        
        return U.in( s.charAt(0), '#', '>', '<','=' );

    }

    /** */
    private boolean containsListDlmtr( String s ) {

        if( S.isNullOrEmpty(s) )
            return false;

        for( int i = 0; i < s.length(); i++ )
        {
            switch( s.charAt(i) ) {
                case ',': case ';': case '\n': case '\t':
                    return true;
            }
        }

        return false;
    }


    /** */
    private boolean isSupportListMode() {

        Class<?> type = this.getFilterItem().getType();

        if( type.isEnum() )
            return false;

       return !Temporal.class.isAssignableFrom(this.getFilterItem().getType());
    }


    /** */
    private IFilterItem.ValueTypeEnum determineValueType( String s ) {

        if( S.isNullOrEmpty(s) )
            return VALUE;

        return ( isSupportListMode() && containsListDlmtr(s) ) ? LIST : VALUE;
    }


    /** */
    private String normalizeList(String s ) {

        if( S.isNullOrEmpty(s) )
            return s;

        boolean wasDelim = true;
        boolean inQuotas = false;

        StringBuilder sb = new StringBuilder();
        StringBuilder si = new StringBuilder();

        char ch;

        for( int i = 0; i < s.length(); i++ ) {

            ch = s.charAt(i);

            if( ch == '"' )
            {
                wasDelim = false;
                inQuotas = !inQuotas;
                //continue;
            }

            if( !inQuotas )
            {
               if( U.inChar( ch, ',', ';', '\n' ) )
               {
                   if( wasDelim )
                       continue;

                   wasDelim = true;
               }
               else
                   wasDelim = false;
            }

            if( wasDelim )
            {
                if( si.length() > 0 )
                {
                    String s1 = si.toString().trim();

                    if( !s1.isEmpty() )
                    {
                        if( sb.length() > 0 )
                            sb.append(", ");

                        sb.append( s1 );
                    }

                    si = new StringBuilder();
                }

                continue;
            }

            si.append(ch);
        }

        if( si.length() != 0 )
        {
            String s1 = si.toString().trim();

            if( !s1.isEmpty() )
            {
                if( sb.length() > 0 )
                    sb.append(", ");

                sb.append( s1 );
            }
        }

        return sb.toString();
    }

    /** */
    public void showListDialog( ) {

        if( !isSupportListMode() )
            return;

        NoteInputDialog nid = new NoteInputDialog( getText(), 5 );
        nid.setTitle      ( filterItem.getLabel() );
        nid.setHeaderText ( bundle.getString("F7_LIST_HEADER" ) );
        nid.setContentText( bundle.getString("F7_LIST_CONTEXT") );
        nid.setGraphic    (
            IconFactory.getLabel( new IconDescriptorBuilder().iconId(FontAwesome.fa_bars).iconSize(IconSize.LARGE).color(colorListBkg).build() )
        );
        nid.initOwner( getScene().getWindow() );
        nid.setResizable(true);

        final Optional< String > os = nid.showAndWait();

        if( os.isPresent() && !S.isNullOrEmpty( os.get() ) )
        {
            setValue( LIST, normalizeList( os.get() ) );
        }
    }

    /** */
    public void showExpressionDialog( ) {

        String columnName = filterItem.getProxyFor() == null ? filterItem.getColumn() : filterItem.getProxyFor().getColumn();

        NoteInputDialog nid = new NoteInputDialog( getText(), 5 );
        nid.setTitle      ( filterItem.getLabel() );
        nid.setHeaderText ( bundle.getString("F7_EXPR_HEADER" ) );
        nid.setGraphic    ( IconFactory.getLabel( new IconDescriptorBuilder().iconId(FontAwesome.fa_database).iconSize(IconSize.LARGE).color(colorExprBkg).build() ));
        nid.setContentText( "Введите часть SQL выражения для поля '" + filterItem.getLabel() + "', оно будет подставлено в запрос в виде: " + columnName + " <введенное выражение>");
        nid.initOwner     ( getScene().getWindow() );
        nid.setResizable  ( true );
        nid.getEditor().setFont( BaseApp.APP().viewPrefService().getCodeFont() );

        final Optional< String > os = nid.showAndWait();

        if( os.isPresent() && !S.isNullOrEmpty( os.get() ) )
        {
            setValue( EXPRESSION, os.get() );
        }
    }

    /** */
    public void showValueDialog( ) {
        /*
        NoteInputDialog nid = new NoteInputDialog( getText(), 5 );
        nid.setTitle     ( filterItem.getLabel() );
        nid.setHeaderText( "Ввод значения для поля" );
        nid.setGraphic   (
            IconFactory.getLabel( new IconDescriptorBuilder().iconId(FontAwesome.fa_vimeo).iconSize(IconSize.LARGE).build() )
        );
        nid.setContentText("Введите значение для поля '" + filterItem.getLabel() + "'");
        nid.initOwner( getScene().getWindow() );
        nid.setResizable(true);

        final Optional< String > os = nid.showAndWait();

        if( os.isPresent() && !S.isNullOrEmpty( os.get() ) )
        {
            setText( os.get() );
            switcher.selectToggle( switcher.getToggles().get(0) );
        }
        */
        setValueType(VALUE);
    }

    @Override
    public Optional<EventHandler<ActionEvent>> getEditDialogAction() {
        return Optional.empty(); //своя логика в F7FilterDialog.initKeyboard()
    }
}
