package ru.inversion.fx.app.es;

import ru.inversion.utils.IExceptionInfo;

import java.net.ConnectException;

/*
* @author Shchapov
* Обертка для исключения ConnectException.
* При передаче экземпляра данного исключения в JInvErrorService.handleException,
* проверит дополнительно доступность сервиса указанного в url по средству ping
* см. JInvErrorService.handleConnectUrlException
* */
public class ConnectUrlException extends ConnectException implements IExceptionInfo {
    private String url;
    private Object object;

    public ConnectUrlException( String message, String url, String object ) {
        super( message );
        this.object = object;
        this.url = url;
    }
    public ConnectUrlException( String message, String url ) {
        super( message );
        this.url = url;
    }
    public ConnectUrlException( String message ) {
        super( message );
    }

    @Override
    public String getCategory() {
        return "connect";
    }

    public String getUrl() {
        return url;
    }

    public Object getObject() {
        return object;
    }

    public static ConnectUrlException throwConnectUrlException( String message, String url, String object ) {
        return new ConnectUrlException( message, url, object );
    }

    public static ConnectUrlException throwConnectUrlException( String message, String url ) {
        return new ConnectUrlException( message, url, null );
    }
}