package ru.inversion.fx.form.controls.filter;

import ru.inversion.dataset.ISQLExpression;
import ru.inversion.utils.ParametersValues;

import java.util.Objects;

/**
 *
 * @author ssu @
 */
public class SQLFilter implements ISQLExpression {
    
    private String sql;
    private String name;

    /** */
    final private ParametersValues values = new ParametersValues();

    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }

    /** */
    @Override
    public void setParameter( String parameterName, Object parameterValue) {
        values.set(
            Objects.requireNonNull( parameterName, "parameterName is null" ),
            parameterValue 
        );
    }

    /** */
    @Override
    public void setParameter( int parameterIndex, Object parameterValue)
    {
        values.set(parameterIndex, parameterValue);
    }

    /** */
    @Override
    public Object getParameter(int parameterIndex) {
        return values.get(parameterIndex);
    }

    /** */
    public void setSQL(String sql) {
        this.sql = sql;
    }

    /** */
    @Override
    public String getSQL() {
        return sql;
    }

    /** */
    @Override
    public Object getParameter(String parameterName) {
        return parameterName == null ? null : values.get( parameterName.toUpperCase() );
    }

    /** */
    public boolean hasParameters() { return !values.isEmpty(); }
}
