package ru.inversion.fore.form.action;

import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import ru.inversion.icons.enums.FontAwesome;

import java.util.List;
import java.util.ResourceBundle;

/** Неизменяемые настройки стандартных операций. */
public enum StandardAction {
    CREATE(FontAwesome.fa_file_o, new KeyCodeCombination(KeyCode.F2), new KeyCodeCombination(KeyCode.F6)),
    UPDATE(FontAwesome.fa_edit, new KeyCodeCombination(KeyCode.F4)),
    DELETE(FontAwesome.fa_close, new KeyCodeCombination(KeyCode.F6, KeyCombination.SHIFT_DOWN)),
    VIEW(FontAwesome.fa_info_circle, new KeyCodeCombination(KeyCode.F3)),
    REFRESH(FontAwesome.fa_refresh, new KeyCodeCombination(KeyCode.F8));

    private static final ResourceBundle BUNDLE = ResourceBundle.getBundle("ru.inversion.fore.form.action.actions");

    private final IconSpec icon;
    private final List<KeyCombination> hotkeys;

    StandardAction(FontAwesome icon, KeyCombination... hotkeys) {
        this.icon = IconSpec.of(icon);
        this.hotkeys = List.of(hotkeys);
    }

    public IconSpec icon() {
        return icon;
    }

    public List<KeyCombination> hotkeys() {
        return hotkeys;
    }

    public String text() {
        return BUNDLE.getString(name());
    }

    public String tooltip() {
        String key = name() + "_TOOLTIP";
        return BUNDLE.containsKey(key) ? BUNDLE.getString(key) : text();
    }
}
