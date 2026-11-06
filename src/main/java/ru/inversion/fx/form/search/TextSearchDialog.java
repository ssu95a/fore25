package ru.inversion.fx.form.search;

import javafx.application.Platform;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Control;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import ru.inversion.fx.app.Tags;
import ru.inversion.utils.S;

import java.util.*;
import java.util.function.Predicate;
import java.util.regex.Pattern;

import static java.util.regex.Pattern.CASE_INSENSITIVE;

/** */
public class TextSearchDialog extends BaseSearchDialog<String> {
    private final Control textField;
    private final CheckBox[] checkBoxes = new CheckBox[6];

    public TextSearchDialog( ) {
        this( null, S.EMPTY_STRING );
    }
    public TextSearchDialog( Comparable objId, String defaultValue )
    {
        super( objId, defaultValue );

        if( this.objId != null && g_searchHistory.containsKey(objId) )
        {
            final ComboBox<String> comboBox = new ComboBox<>();
            comboBox.setEditable(true);
            comboBox.getEditor().setText(defaultValue);

            if( S.isNullOrEmpty(defaultValue) )
            {
                final Map< String, Integer > historyMap = initSearchHistory();
                if( historyMap != null ) {
                    final Set<String> keys = historyMap.keySet();
                    //for( int i = keys.size() - 1; i <=0; i-- )
                        comboBox.getItems().addAll( keys );
                }

                if( comboBox.getItems().size() > 0)
                    comboBox.setValue( comboBox.getItems().get( comboBox.getItems().size() - 1 ) );
            }
            textField = comboBox;
        }
        else
        {
            this.textField = new TextField(defaultValue);
        }

        textField.setMaxWidth(Double.MAX_VALUE);
        GridPane.setHgrow     ( textField, Priority.ALWAYS);
        GridPane.setFillWidth ( textField, true );
        GridPane.setColumnSpan( textField, 2 );


        checkBoxes[0] = new CheckBox("Учитывать регистр");
        checkBoxes[1] = new CheckBox("Слово целиком");
        checkBoxes[2] = new CheckBox("Regexp");
        checkBoxes[3] = new CheckBox("Искать с начала");
        checkBoxes[4] = new CheckBox("Только в листовых");
        checkBoxes[5] = new CheckBox("При достижении конца продолжить с начала.");

        checkBoxes[1].disableProperty().bind( checkBoxes[2].selectedProperty() );
        checkBoxes[5].disableProperty().bind( checkBoxes[4].selectedProperty() );

        final Integer value = g_searchHistory.computeIfAbsent("#bits", k -> new HashMap<>()).get("value");
        if( value != null )
        {
            final BitSet bitSet = BitSet.valueOf(new long[]{value.longValue()});
            int i = 0;
            for( CheckBox cb : checkBoxes ) {
                 if( bitSet.get(i++) )
                     if(cb!=null)
                        cb.setSelected(true);
            }
        }

        updateGrid();
    }

    /** */
    protected Map<String, Integer> initSearchHistory( ) {

        if( objId == null )
            return null;

        Map<String, Integer> retMap = (Map< String, Integer >)g_searchHistory.get(objId);

        if( retMap == null ) {

            retMap = new LinkedHashMap<String,Integer>() {
                @Override
                protected boolean removeEldestEntry( Map.Entry< String, Integer > eldest ) {
                    return size() >= MAX_HISTORY_SIZE;
                }
            };

            g_searchHistory.put( objId, retMap );
        }

        return retMap;
    }

    @Override
    protected void updateGrid() {
        grid.getChildren().clear();
        grid.add( label, 0, 0 );
        grid.add( textField, 0, 1);

        grid.add( checkBoxes[0], 0, 2 );
        grid.add( checkBoxes[1], 0, 3 );
        grid.add( checkBoxes[2], 1, 2 );
        grid.add( checkBoxes[3], 1, 3 );
        grid.add( checkBoxes[4], 0, 4 );
        grid.add( checkBoxes[5], 1, 4 );

        getDialogPane().setContent(grid);

        Platform.runLater( textField::requestFocus );
    }

    @Override
    protected SearchParam<String> makeParam() {

        final TextField tf = textField instanceof TextField ? (TextField)textField : ((ComboBox)textField).getEditor();

        String text = tf.getText();

        if( S.isNullOrEmpty(text) )
            throw new IllegalStateException( Tags.PRODUCT + "Не заполнена строка поиска" );

        final int caseInsensitive = checkBoxes[0].isSelected() ? ( CASE_INSENSITIVE | Pattern.UNICODE_CASE ) : 0;

        Predicate<String> predicate;

        if( checkBoxes[2].isSelected() )
        {
            predicate = Pattern.compile( text, caseInsensitive ).asPredicate();
        }
        else
        {
            if( S.contains( text, '%','_') )
            {
                final String t = S.likeToRegexp(text);
                predicate = Pattern.compile( t, caseInsensitive ).asPredicate();
            }
            else
            {
                //слово целиком
                if( checkBoxes[1].isSelected() )
                {
                    predicate = caseInsensitive == 0 ? text::equals : text::equalsIgnoreCase;
                }
                else
                {
                    if( caseInsensitive == 0 )
                        predicate =  s -> s != null && s.startsWith(text);
                    else
                        predicate = s -> s.regionMatches( true, 0, text, 0, text.length() );
                }
            }
        }

        if( objId != null ) {
            final Map< String, Integer > historyMap = (Map< String, Integer >)g_searchHistory.computeIfAbsent(objId, k->new LinkedHashMap<>());
            historyMap.compute(text, ( s, i ) -> i == null ? 0 : i + 1);
        }

        final BitSet bitSet = BitSet.valueOf(new long[]{0L});
        int i = 0;
        for( CheckBox cb : checkBoxes )
             if( cb != null )
                 bitSet.set( i++,cb.isSelected() );

        final Map< String, Integer > bitsMap = (Map< String, Integer >)g_searchHistory.computeIfAbsent("#bits", k -> new HashMap< String, Integer >());
        bitsMap.put( "value", bitSet.isEmpty() ? 0 :(int)bitSet.toLongArray()[0] );

        return new SearchParam<>( predicate, !checkBoxes[1].isSelected(), checkBoxes[3].isSelected(), checkBoxes[4].isSelected(), !checkBoxes[5].isDisable() && checkBoxes[5].isSelected() );
    }
}
