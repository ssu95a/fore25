package ru.inversion.fx.help.controller;

import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.inversion.fx.app.BaseApp;
import ru.inversion.fx.app.property.PropertiesTypeEnum;
import ru.inversion.fx.form.JInvFXFormController;
import ru.inversion.fx.log.LogManager;
import ru.inversion.fx.service.module.ModuleContext;
import ru.inversion.fx.service.module.ModuleService;

import java.util.Enumeration;
import java.util.Properties;
import java.util.ResourceBundle;

/**
 *
 * @author perov
 */
public class InfoTab extends AbstractTab {

    private final WebView webView = new WebView();
    private String formName;

    private static Logger logger = LoggerFactory.getLogger(InfoTab.class);

    public InfoTab(String text, JInvFXFormController controller, ResourceBundle bundle) {
        super(text, controller, bundle);
        init();
    }

    @Override
    public void init() {
        VBox vb = new VBox();
        vb.getChildren().add(webView);
        VBox.setVgrow(webView, Priority.ALWAYS);
        this.setContent(vb);
        webView.getEngine().setUserStyleSheetLocation(this.getClass().getClassLoader()
                .getResource("ru/inversion/fx/help/info/css/info.css")
                .toString());
        initWebViewDisabler(webView);
    }

    @Override
    public void draw(String formName) {
        this.formName = formName;
        StringBuilder content = new StringBuilder();
        content.append(getInversionInfo());
        content.append(getSystemProperty());
        webView.getEngine().loadContent(content.toString());
        setTitle(getBundle().getString("HELP_TITLE_INFO"));
    }

    private String getSystemProperty() {
        Properties p = System.getProperties();
        Enumeration propertyNames = p.propertyNames();
        StringBuilder sb = new StringBuilder("<h3>Java</h3>");
        for (; propertyNames.hasMoreElements();) {
            String key = propertyNames.nextElement().toString();
            sb.append("<p><span>").append(key).append(" : </span>").append(p.getProperty(key)).append("</p>");
        }
        return sb.toString();
    }

    private String getInversionInfo() {
        StringBuilder sb = new StringBuilder();
        sb.append("<h3>").append(getBundle().getString("INFO_INVERSIA")).append("</h3>");

        // Имя формы
        sb.append("<p><span>").append(getBundle().getString("INFO_FORM_NAME")).append("</span>").append(formName).append("</p>");

        // Заголовок родительской формы
        String parentName = null;
        parentName = BaseApp.APP().getProperties(PropertiesTypeEnum.PRP)
            .getStringProperty("ru.inversion.app.form_mdi_title");
        if (parentName == null) {
            parentName = BaseApp.APP().getProperties(PropertiesTypeEnum.SMR)
                .getStringProperty("ru.inversion.app.title");
        }
        sb.append("<p><span>").append(getBundle().getString("INFO_PARENT_TITLE")).append("</span>").append(parentName).append("</p>");

        try {

            ModuleContext context = ((ModuleService) BaseApp.APP().getAppService(ModuleService.SERVICE_ID)).
                getModuleContextOrCreate(((HelpController) getController()).getClassController());

            if (context != null) {
                // Имя jar
                sb.append("<p><span>").append(getBundle().getString("INFO_JAR_NAME")).append("</span>").append(context.getJarName()).append("</p>");
                // Версия ядра модуля
                sb.append("<p><span>").append(getBundle().getString("INFO_CORE_VERSION")).append("</span>")
                        .append("(")
                        .append(BaseApp.APP().getProperties(PropertiesTypeEnum.PRP).getStringProperty("ru.inversion.app.core_scm_version"))
                        .append(") ")
                        .append(BaseApp.APP().getProperties(PropertiesTypeEnum.PRP).getStringProperty("ru.inversion.app.core_impl_version"))
                        .append("</p>");
                // Версия модуля
                sb.append("<p><span>").append(getBundle().getString("INFO_MODULE_VERSION")).append("</span>").append(context.getFullVersion()).append("</p>");
            } else {
                // заглушки
                // Имя jar
                sb.append("<p><span>").append(getBundle().getString("INFO_JAR_NAME")).
                    append("</span>").append(getBundle().getString("INFO_ERROR")).append("</p>");
                // Версия ядра модуля
                sb.append("<p><span>").append(getBundle().getString("INFO_CORE_VERSION")).append("</span>").
                    append(getBundle().getString("INFO_ERROR")).append("</p>");
                // Версия модуля
                sb.append("<p><span>").append(getBundle().getString("INFO_MODULE_VERSION")).append("</span>").
                    append(getBundle().getString("INFO_ERROR")).append("</p>");
            }

            // Контроллер
            sb.append("<p><span>").append(getBundle().getString("INFO_CONTROLLER")).append("</span>").
                append(((HelpController) getController()).getClassController().getCanonicalName()).append("</p>");
            // Лог файл
            sb.append("<p><span>").append(getBundle().getString("INFO_LOGFILE")).append("</span>")
                    .append(LogManager.getCurrentLogFile()).append("</p>");

        } catch (Throwable ex) {
            logger.error("Error help ", ex);
        }

        return sb.toString();
    }

}
