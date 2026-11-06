package ru.inversion.fx.form.controls;

/**
 *
 * @author antonovdi
 */
public class JInvTableColumnBoolean<S, T>  extends JInvTableColumn<S, Boolean> {

    private static final int FIXED_WIDTH = 25;

    public JInvTableColumnBoolean() {
        super();
        limitWidth();
    }

    public JInvTableColumnBoolean( final String name ) {
        super( name );
        limitWidth();
    }

    private void limitWidth( ) {
        setMinWidth ( FIXED_WIDTH );
        setPrefWidth( FIXED_WIDTH );
        //setMaxWidth ( 40 );
    }
}
