package ru.inversion.fx.help.controller;

import javafx.concurrent.Worker;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import netscape.javascript.JSObject;
import org.slf4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.w3c.dom.events.Event;
import org.w3c.dom.events.EventTarget;

import java.awt.*;
import java.io.IOException;
import java.net.URI;

import static java.lang.invoke.MethodHandles.lookup;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Внутренний класс.
 * Добавляет функциональность для компонента WebView
 *
 * @author perov
 * @version 1.0.0
 */
public class WebViewDecorator {

    private final static Logger LOGGER;

    private final static String JQUERY;

    private final static String JQUERY_MARK;

    private final static String SEARCH_JS;

    private final WebView webView;

    private final WebEngine webEngine;

    static {
        final Class<?> clazz = lookup().lookupClass();
        LOGGER = getLogger(clazz);
        JQUERY = clazz.getResource("/ru/inversion/fx/help/js/jquery-3.3.1.min.js").toExternalForm();
        JQUERY_MARK = clazz.getResource("/ru/inversion/fx/help/js/jquery.mark.min.js").toExternalForm();
        SEARCH_JS = clazz.getResource("/ru/inversion/fx/help/js/search.js").toExternalForm();
    }

    public WebViewDecorator(WebView webView) {
        this(webView, null, false, true);

    }

    public WebViewDecorator(WebView webView, Object clazz, boolean enableEdit, boolean enableLink) {
        this.webView = webView;
        this.webEngine = webView.getEngine();
        webEngine.setUserStyleSheetLocation(getClass().getResource("/ru/inversion/fx/help/css/mark.css").toString());

        webEngine.getLoadWorker().stateProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue == Worker.State.SUCCEEDED) {
                if (!enableEdit) {
                    editOff();
                }
                if (enableLink) {
                    linkOn(clazz);
                }
                loadScript(JQUERY);
                loadScript(JQUERY_MARK);
                loadScript(SEARCH_JS);
            }
        });
    }

    public int searchText(String text) {
        return searchText(text, false);
    }

    public int searchText(String text, boolean caseSensitive) {
        return (Integer) callFunc("searchText", text, caseSensitive);
    }

    public int searchTextRegEx(String regexp) {
        return searchTextRegEx(regexp, "g");
    }

    public int searchTextRegEx(String regexp, String flags) {
        return (Integer) callFunc("searchTextRegEx", regexp, flags);
    }

    public int searchWord(String text) {
        return searchWord(text, "g");
    }

    public int searchWord(String text, String flags) {
        final String regexp = "(^|[ \n\r\t.,'\"\\(+!?-]+)(" + text + ")([ \n\r\t.,'\\)\"+!?-]+|$)";
        return (Integer) callFunc("searchWord", regexp, flags);
    }

    public void clearTextMark() {
        callFunc("clearMark");
    }

    public void selectNextText() {
        callFunc("selectNext");
    }

    public void selectPrevText() {
        callFunc("selectPrev");
    }

    private void loadScript(String script) {
        webEngine.executeScript("var script = document.createElement(\"script\");\n" +
                "    script.type = \"text/javascript\";\n" +
                "    script.src = '" + script + "';\n" +
                "    document.documentElement.childNodes[0].appendChild(script);");
    }

    private Object callFunc(String funcName, Object... params) {
        JSObject jsObject = (JSObject) webEngine.executeScript("window");
        return jsObject.call(funcName, params);
    }

    private void editOff() {
        Document doc = webEngine.getDocument();
        NodeList bodyList = doc.getElementsByTagName("body");
        if (bodyList.getLength() > 0) {
            Element body = (Element) bodyList.item(0);
            body.setAttribute("contenteditable", "false");
        }
    }

    private void linkOn(Object clazz) {
        /*Добавляем ссылки */
        NodeList aList = webEngine.getDocument().getElementsByTagName("a");
        for (int i = 0; i < aList.getLength(); i++) {
            Element el = (Element) aList.item(i);
            String urlString = el.getAttribute("href");
            if (isImproperUrl(urlString)) {
                el.removeAttribute("href");
                continue;
            }
            ((EventTarget) el).addEventListener("click", WebViewDecorator::handleUrlClickEvent, false);
        }
    }

    private boolean isImproperUrl(final String urlString) {
        return urlString == null || urlString.equals("") || !urlString.startsWith("http") || !urlString.startsWith("https");
    }

    private static void handleUrlClickEvent(Event event) {
        final EventTarget target = event.getTarget();
        try {
            Desktop.getDesktop().browse(URI.create(target.toString()));
        } catch (IOException e) {
            LOGGER.warn("Bad link: {}", target);
        }
    }

    public WebView getWebView() {
        return webView;
    }
}
