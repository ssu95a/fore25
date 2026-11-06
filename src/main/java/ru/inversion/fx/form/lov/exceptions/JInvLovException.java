package ru.inversion.fx.form.lov.exceptions;

import ru.inversion.utils.IExceptionInfo;

/**
 *
 * @author ssu
 */
public class JInvLovException extends RuntimeException implements IExceptionInfo  {
    
    final private String contentText;
    
    /** */
    public JInvLovException() {
        contentText = null;
    }
    
    /** */
    public JInvLovException( String message ) {
        super(message);
        contentText = null;
    }
    
    /** */
    public JInvLovException( String message, Throwable cause ) {
        super(message, cause);
        contentText = null;
    }
    
    /** */
    public JInvLovException( Throwable cause ) {
        super(cause);
        contentText = null;
    }
    
    /** */
    public JInvLovException( String title, String contentText ) {
        super(title);
        this.contentText = contentText;
    }
    
    /** */
    public JInvLovException( String title, String contentText, Throwable cause ) {
        super(title, cause);
        this.contentText = contentText;
    }
    
    /** */
    @Override
    public String getContentText( ) {
        return contentText;
    }

    /** */
    @Override
    public String getCategory() {
        return "LOV";
    }
}
