package ru.inversion.fx.help.controller;

import javafx.scene.control.Tab;
import javafx.scene.web.WebView;
import org.slf4j.Logger;
import ru.inversion.fx.app.sec.JInvSecurityService;
import ru.inversion.fx.form.JInvFXFormController;
import ru.inversion.fx.form.ViewContext;
import ru.inversion.tc.TaskContext;

import java.lang.invoke.MethodHandles;
import java.util.ResourceBundle;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Внутренний класс.
 * Реализует общую функциональность
 * @author perov
 * @version 1.0.0
 */
abstract class AbstractTab extends Tab implements TabState {
    private final static Logger logger = getLogger(MethodHandles.lookup().lookupClass());
    private final ResourceBundle bundle;
    private ViewContext viewContext;
    private final JInvFXFormController  controller;


    public JInvFXFormController  getController() {
        return controller;
    }

    public ResourceBundle getBundle() {
        return bundle;
    }


    public ViewContext getViewContext() {
        return viewContext;
    }

    public void setViewContext(ViewContext viewContext) {
        this.viewContext = viewContext;
    }

    public AbstractTab(String text, JInvFXFormController  controller, ResourceBundle bundle) {
        super(text);
        this.controller = controller;
        this.bundle = bundle;
    }

    /**
     * Инициализация компонент
     */
    public void init(){

    }

    public void lossSelect() {
    }

    /**
     Отключение WebView при переходе на другой таб
     @param webView
     */
    void initWebViewDisabler(WebView webView) {
        if ( webView == null ) {
            logger.info( "WebView for {} is null", getClass().getSimpleName() );
            return;
        }
        logger.info( "WebView {} on {} armed to be disabled", webView, getClass().getSimpleName());
        this.selectedProperty().addListener( ( observable, oldValue, newValue ) -> {
            logger.info( "{} tab now selected = {}, webview disabled = {} ", getClass().getSimpleName(),
                    newValue, !newValue );
            webView.setDisable( !newValue );
        } );
    }

    @Override
    public boolean checkAccess(TaskContext tc, int code) {
        return JInvSecurityService.isCanAccessIsAction(tc, code);
    }

//    /**
//     * Обработка клика на ссылке
//     */
//    public class JavaScriptUtil {
//
//        public void onClick(String link) {
//            /*draw(form);*/
//            try {
//                Desktop.getDesktop().browse(URI.create(link));
//            } catch (IOException e) {
//                logger.warn("bad link: {}", link);
//            }
//        }
//
//    }

    @Override
    public void preDestroy() {

    }

    protected void setTitle(String partOfTitle) {

        StringBuilder sb = new StringBuilder();
        sb.append(partOfTitle);
        sb.append(((HelpController) getController()).getConstantPartOfTitle());
        getController().setTitle(sb.toString());
    }

}
