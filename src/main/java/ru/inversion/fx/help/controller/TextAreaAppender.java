package ru.inversion.fx.help.controller;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import javafx.application.Platform;
import org.slf4j.LoggerFactory;
import ru.inversion.fx.form.controls.JInvTextArea;
import ru.inversion.fx.log.LogManager;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 *
 * @author perov
 */
public class TextAreaAppender extends AppenderBase<ILoggingEvent> {
    
    private final org.slf4j.Logger logger = LoggerFactory.getLogger(LogManager.class);
    static final String APPNDER_NAME = "ru.inversion.help.textarea.appender"; 
    private final JInvTextArea textArea;

    public TextAreaAppender(JInvTextArea textArea) {
        this.textArea = textArea;
        this.setName(APPNDER_NAME);      
    }
    

    @Override
    protected void append(final ILoggingEvent event) {
        try {
            Platform.runLater(() -> {
                
                StringBuffer sb = new StringBuffer(128);
                sb.append(new SimpleDateFormat("dd-MM-yyyy HH:mm:ss").format(new Date(event.getTimeStamp())));
                sb.append(" ");
                sb.append(event.getLevel());
                sb.append(" [");
                sb.append(event.getThreadName());
                sb.append("] ");
                sb.append(event.getLoggerName());
                sb.append(" - ");
                sb.append(event.getFormattedMessage());
                sb.append(System.getProperty("line.separator"));                
                textArea.appendText(sb.toString());
            });
        } catch (Exception e) {
            logger.error("Error append", e);
        }

    }

    

}
