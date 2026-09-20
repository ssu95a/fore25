package ru.inversion.fx.help.controller;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Worker;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebErrorEvent;
import javafx.scene.web.WebEvent;
import javafx.scene.web.WebView;
import netscape.javascript.JSObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import ru.inversion.fx.app.es.JInvErrorService;
import ru.inversion.fx.form.JInvFXFormController;

import java.net.URL;
import java.util.ResourceBundle;
import ru.inversion.utils.S;

/**
 * Внутренний класс. Реализация Tab - отображения информации "Горячие клавиши".
 *
 * @author perov
 * @version 1.0.0
 */
class HotKeyTab extends AbstractTab {
    public static final String HOTKEYS_URL = "ru/inversion/fx/help/hotkey/keys.html";
    public static final String CSS_URL = "ru/inversion/fx/help/hotkey/css/keys.css";
    private final WebView webView = new WebView();
    private static Logger logger = LoggerFactory.getLogger( MethodHandles.lookup().lookupClass() );
    private static String content;

    public HotKeyTab(String text, JInvFXFormController controller, ResourceBundle bundle) {
        super(text, controller, bundle);
        init();
    }

    @Override
    public void init() {
        VBox vb = new VBox();
        vb.getChildren().add(webView);
        VBox.setVgrow(webView, Priority.ALWAYS);
        this.setContent(vb);
        initWebView();
        initWebViewDisabler(webView);
    }

    @Override
    public void draw(String formName) {
        setTitle(getBundle().getString("HELP_TITLE_HOT_KEY"));
        //CSS
        final WebEngine engine = webView.getEngine();
        engine.setUserStyleSheetLocation( getClass().getClassLoader().getResource( CSS_URL ).toString());
        engine.loadContent( content );
    }

    private static boolean started;

    private void initWebView() {
        if (!started) {
            WebEngine engine = webView.getEngine();

            engine.setOnError((WebErrorEvent event) -> {
        //        JInvErrorService.handleException(getViewContext(), event.getException());
                logger.warn( "WebView error: {}", event.getMessage() );
            });

            engine.setOnAlert((WebEvent<String> event) -> {
            logger.info( "HotKeyTab WebView alert: {}", event.getData() );
        });
            engine.getLoadWorker().exceptionProperty().addListener((ObservableValue<? extends Throwable> ob,
                    Throwable oV, Throwable nV) -> JInvErrorService.handleException(getViewContext(), nV) );

            URL url = getClass().getClassLoader().getResource( HOTKEYS_URL );

            engine.load(url.toExternalForm());
            //При полной загрузке страницы
            engine.getLoadWorker().stateProperty().addListener( ( observable, oldValue, newValue ) -> {
                if ( S.isNullOrEmpty(content) && newValue != oldValue && newValue == Worker.State.SUCCEEDED ){
                    //Даём дёргать методы HotKeyHelper из-под JS
                    JSObject jsobj = (JSObject) engine.executeScript("window");
                    jsobj.setMember("keyHelper", new HotKeyHelper());

                    //Получаем готовый HTML-код страницы
                    content = (String) engine.executeScript( "generatePage()" );

                }
            } );
            started = true;
        } else {
            logger.warn( "WebView already started!" );
        }
    }


}
