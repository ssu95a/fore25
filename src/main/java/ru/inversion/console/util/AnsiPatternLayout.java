package ru.inversion.console.util;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.PatternLayout;
import ch.qos.logback.classic.spi.ILoggingEvent;

public class AnsiPatternLayout extends PatternLayout {

    private static final boolean USE_COLOR = System.console() != null;

    @Override
    public String doLayout(ILoggingEvent event) {
        String line = super.doLayout(event);

        if (!USE_COLOR) {
            return line;
        }

        final Ansi ansi = colorFor(event);
        return ansi != null ? ansi.colorize(line) : line;
    }

    private Ansi colorFor( ILoggingEvent e) {
        Level l = e.getLevel();

        switch (l.toInt()) {
            case Level.ERROR_INT:
                return Ansi.Red.and(Ansi.Bold);
            case Level.WARN_INT:
                return Ansi.Yellow;
            case Level.INFO_INT:
                return Ansi.Green;
            case Level.DEBUG_INT:
                return Ansi.Cyan;
            case Level.TRACE_INT:
                return Ansi.White;
            default:
                return null;
        }
    }
}
