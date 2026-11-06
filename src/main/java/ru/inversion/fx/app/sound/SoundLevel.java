package ru.inversion.fx.app.sound;
import java.util.ResourceBundle;

/**
 @author fomishkin on 06.03.2019. */
public enum SoundLevel {
    OFF,
    ERROR,
    WARNING,
    QUESTION,
    ALL;

    private static final ResourceBundle fore = ResourceBundle.getBundle("fore");
    private static final String sound_ = "SOUND_";

    public String toString() {
        return fore.getString( sound_ + this.name());
    }
}
