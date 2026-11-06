package ru.inversion.fx.form.controls.dsbar;
import javafx.util.Pair;
import ru.inversion.dataset.aggr.AggrFuncEnum;
import ru.inversion.fx.form.controls.JInvNumberField;

import java.util.*;
import java.util.stream.Collectors;

/**
 @author fomishkin on 08.04.2019. */
public abstract class AbstractAggregatorPart extends AbstractPartBase {
    private final Map<Pair<AggrFuncEnum,String>, List<JInvNumberField>> fieldList = new HashMap<>();

    public AbstractAggregatorPart( final ResourceBundle bundle ) {
        super( bundle );
    }

    @Override
    public List<JInvNumberField> getControls() {
        return fieldList.values().stream().flatMap( Collection::stream ).collect( Collectors.toList() );
    }

    @Override
    public List<JInvNumberField> getControls( final AggrFuncEnum funcEnum ) {
        return fieldList.get( funcEnum );
    }

    /**
     Получить первый контрол по колонке и функции
     */
    public JInvNumberField getControl( AggrFuncEnum func, String column ){

        Pair<AggrFuncEnum, String> pair = new Pair<>( func, column );
        List<JInvNumberField> fields = fieldList.get( pair );

        if ( fields == null || fields.isEmpty() ) {
            return null;
        }
        return fields.get(0);
    }

    /**
     Добавить поле в fieldList по колонке и функции
     */
    protected void addField( final AggrFuncEnum func, final String column, final JInvNumberField textField ) {

        Pair<AggrFuncEnum, String> pair = new Pair<>( func, column );
        fieldList.computeIfAbsent( pair, k -> new LinkedList<>() );
        fieldList.get( pair ).add( textField );
    }

}
