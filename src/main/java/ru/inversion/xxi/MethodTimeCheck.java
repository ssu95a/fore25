package ru.inversion.xxi;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.inversion.utils.S;

import java.time.Duration;
import java.time.Instant;
import java.util.LinkedList;

public final class MethodTimeCheck {

    private final LinkedList<FixedTime> fixedTimeList = new LinkedList<>();

    private final String parentClass;

    private final String callerClass;

    private final String methodName;

    private static final Boolean ACTIVE = Boolean.valueOf(System.getProperty("performance_logging"));

    private static final Logger LOGGER = LoggerFactory.getLogger("ru.inversion.fx.performance");

    public MethodTimeCheck(String parentClass, String methodName) {
        this.parentClass = parentClass;
        this.callerClass = null;
        this.methodName  = methodName;
    }

    public MethodTimeCheck( String parentClass, String callerClass, String methodName) {
        this.parentClass = parentClass;
        this.callerClass = callerClass;
        this.methodName  = methodName;
    }

    public void fixTime(String timeName) {
        if (!ACTIVE) return;

        final FixedTime fixedTime = new FixedTime( timeName, Instant.now() );
        fixedTimeList.add(fixedTime);
    }

    public void printTime() {
        if (!ACTIVE) return;

        final StringBuilder sb = new StringBuilder();
        sb.append(System.lineSeparator());
        sb.append("###### ").append("PARENT_CLASS: ").append(parentClass).append(".").append(methodName).append(" ######");
        if (S.isNotNullOrEmpty(callerClass)) {
            sb.append(System.lineSeparator()).append("###### ").append("CALLER_CLASS: ").append(callerClass).append(" ######");
        }
        sb.append(System.lineSeparator());

        final FixedTime startFixedTime = fixedTimeList.getFirst();
        final FixedTime lastFixedTime = fixedTimeList.getLast();
        FixedTime prevFixedTime = null;

        for (final FixedTime fixedTime : fixedTimeList) {
            if (prevFixedTime == null) {
                prevFixedTime = fixedTime;
                continue;
            }

            sb.append("DURATION BETWEEN ").append(prevFixedTime.getTimeName())
                    .append(" AND ").append(fixedTime.getTimeName()).append(":");
            sb.append(getDurationInfo(prevFixedTime.getTimeInstant(), fixedTime.getTimeInstant()));
            sb.append(System.lineSeparator());

            prevFixedTime = fixedTime;
        }

        sb.append("GLOBAL TIME DURATION:")
                .append(getDurationInfo(startFixedTime.getTimeInstant(), lastFixedTime.getTimeInstant()));


        LOGGER.debug(methodName + ": " + "{}", sb.toString());
    }


    private String getDurationInfo(Instant start, Instant end) {
        final long seconds = Duration.between(start, end).getSeconds();
        final long millis  = Duration.between(start, end).toMillis();
        return System.lineSeparator() +
                " |- Seconds: " + seconds + System.lineSeparator() +
                " |- Milliseconds: " + millis + System.lineSeparator();
    }

    private final class FixedTime {

        private final String timeName;

        private final Instant timeInstant;

        FixedTime(String timeName, Instant timeInstant) {
            this.timeName = timeName;
            this.timeInstant = timeInstant;
        }

        String getTimeName() {
            return timeName;
        }

        Instant getTimeInstant() {
            return timeInstant;
        }
    }
}
