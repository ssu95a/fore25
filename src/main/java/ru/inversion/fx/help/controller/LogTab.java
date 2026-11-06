package ru.inversion.fx.help.controller;

import ch.qos.logback.classic.Logger;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import org.slf4j.LoggerFactory;
import ru.inversion.fx.app.es.JInvErrorService;
import ru.inversion.fx.form.JInvFXFormController;
import ru.inversion.fx.form.controls.textarea.JInvSearchTextPane;
import ru.inversion.fx.log.LogManager;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ResourceBundle;

/**
 * @author perov
 */
public class LogTab extends AbstractTab {

    private Logger logger = (Logger) LoggerFactory.getLogger(LogManager.DEFAULT_ROOTLOGGER_PATH);

    private JInvSearchTextPane textAreaPane;
    private boolean initDone;

    public LogTab(String text, JInvFXFormController controller, ResourceBundle bundle) {
        super(text, controller, bundle);
    }

    @Override
    public void init() {
        textAreaPane = new JInvSearchTextPane();
        textAreaPane.getTextArea().setEditable(false);
        textAreaPane.getTextArea().appendText(loadLogFromFile());

        //зачем
//        if (logger.getAppender(TextAreaAppender.APPNDER_NAME) == null) {
//            TextAreaAppender appender = new TextAreaAppender(textAreaPane);
//            appender.setContext(logger.getLoggerContext());
//            logger.addAppender(appender);
//        }
//        if (!logger.getAppender(TextAreaAppender.APPNDER_NAME).isStarted()) {
//            logger.getAppender(TextAreaAppender.APPNDER_NAME).start();
//        }

        VBox vb = new VBox();
        vb.getChildren().add(textAreaPane);
        VBox.setVgrow(textAreaPane, Priority.ALWAYS);
        this.setContent(vb);

    }

    @Override
    public void draw(String formName) {
        setTitle(getBundle().getString("HELP_TITLE_LOG"));

        logger.debug("Start appender {} \n", TextAreaAppender.APPNDER_NAME);
        if (!initDone) {
            init();
            initDone = true;
        }

    }

    @Override
    public void preDestroy() {
        if (logger.getAppender(TextAreaAppender.APPNDER_NAME) != null) {
            logger.debug("Stop appender {} ", TextAreaAppender.APPNDER_NAME);
            logger.getAppender(TextAreaAppender.APPNDER_NAME).stop();
        }
    }

    private String loadLogFromFile() {
        final File currentLogFile = LogManager.getCurrentLogFile();
        if (currentLogFile != null && currentLogFile.canRead()) {
            try {
                final FileInputStream fileInputStream = new FileInputStream(currentLogFile);
                ByteArrayOutputStream result = new ByteArrayOutputStream();
                byte[] buffer = new byte[1024];
                int length;
                while ((length = fileInputStream.read(buffer)) != -1) {
                    result.write(buffer, 0, length);
                }
                return result.toString("UTF-8");
            } catch (IOException ex) {
                JInvErrorService.handleException(null, ex);
            }
        }
        return null;
    }

}
