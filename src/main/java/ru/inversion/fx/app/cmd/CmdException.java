package ru.inversion.fx.app.cmd;

import ru.inversion.utils.IExceptionInfo;

public class CmdException extends RuntimeException implements IExceptionInfo {

    public CmdException( String message ) {
        super(message);
    }

    public CmdException( String message, Throwable cause ) {
        super(message, cause);
    }

    public CmdException( Throwable cause ) {
        super(cause);
    }

    @Override
    public String getCategory() {
        return "cmd";
    }
}
