package ru.inversion.fx.form;

import ru.inversion.fx.app.Tags;
import ru.inversion.fx.app.es.JInvErrorService;
import ru.inversion.tc.TaskContext;
import ru.inversion.utils.S;
import ru.inversion.utils.U;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;
//import java.util.ServiceLoader;
import java.util.function.Consumer;

import static ru.inversion.fx.form.AbstractBaseController.FormReturnEnum.RET_OK;

/** */
public class FXClassLauncher extends FXFormLauncher<Void> {

    /** */
    final public static String DEFAULT_METHOD_NAME = "entryPoint";

    private String className,
                   methodName;

    /** */
    public FXClassLauncher( TaskContext tc, ViewContext vc ) {
        super( tc, vc );
    }

    /** */
    public String getClassName() {
        return className;
    }

    /** */
    public FXClassLauncher className( String className ) {
        this.className = className;
        return this;
    }

    /** */
    public String getMethodName( ) {
        return methodName;
    }

    /** */
    public FXClassLauncher methodName( String methodName ) {
        this.methodName = methodName;
        return this;
    }

    /** */
    public FXClassLauncher commandForRun( String command ) {

        if( S.isNullOrEmpty(command) ) {
            this.methodName = null;
            this.className  = null;
        }
        else {

            int index = command.indexOf(':');

            if( index == -1 )
                this.methodName = command;
            else {
                this.className  = command.substring( 0, index );
                this.methodName = command.substring( index + 1 );
            }
        }
        return this;
    }

    /** */
    final private static Consumer<ResultForm<Void>> g_defCalback = resultForm -> {
        if( resultForm.getException() != null  )
            JInvErrorService.handleException(
                    resultForm.getController().getViewContext(),
                    resultForm.getException()
            );
    };

    /** */
    private Consumer<ResultForm<Void>> getCallBack() {

        if( closeCallback != null )
            return closeCallback;

        if( clb != null ) {
            return tResultForm -> {
                if( tResultForm.getFormReturn() == RET_OK ) {
                    clb.accept( tResultForm.getFormReturn(), tResultForm.getController() );
                }
                else {

                    if( tResultForm.getException() != null )
                        g_defCalback.accept( tResultForm );

                }
            };
        }

        return g_defCalback;
    }

    /** */
    @Override
    protected void showInternal( Map< String, Object > innerProperties ) {

        try {

            if(S.isNullOrEmpty(className))
                throw new IllegalArgumentException("'className' is null");

            String mn = S.isNullOrEmpty(methodName) ? DEFAULT_METHOD_NAME : methodName;

            Class runClass = null;

            try {
                runClass = Class.forName(className);
            } catch(ClassNotFoundException e) {
                throw new IllegalArgumentException( Tags.PRODUCT_LABEL + "'" + className + "' is bad class name", e );
            }

            //ServiceLoader.load()

            Method runMethod = null;

            try {

                runMethod = runClass.getDeclaredMethod(
                        mn,
                        ViewContext.class,  //
                        TaskContext.class,  //
                        Map.class,          // Входные параметры
                        Consumer.class      // Close callBack
                );

            } catch(NoSuchMethodException e) {
                throw new IllegalArgumentException( Tags.PRODUCT_LABEL + "No method found with name '" + mn + "' and signature ( ... )", e );
            }

            if( !Modifier.isStatic(runMethod.getModifiers()) )
                throw new IllegalArgumentException(Tags.PRODUCT_LABEL + "Method '" + mn + "' must be static");

            Object params[] = new Object[runMethod.getParameterCount()];

            Map< String, Object > p = U.nvl( getInitProperties(), new HashMap() );

            if( innerProperties != null )
                p.putAll( innerProperties );

            params[0] = vc;
            params[1] = tc;
            params[2] = p;
            params[3] = getCallBack();

            try {
                runMethod.invoke(null, params );
            } catch( IllegalAccessException e ) {
                throw new RuntimeException( Tags.PRODUCT_LABEL + "Error inside call method '" + mn + "'", e );
            }
        }
        catch( Throwable th ) {
            throw new RuntimeException( Tags.PRODUCT_LABEL + "Error on call class", th );
        }
    }
}
