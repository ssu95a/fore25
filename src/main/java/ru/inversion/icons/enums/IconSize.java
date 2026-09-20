package ru.inversion.icons.enums;

/**
 * @author fomishkin on 05.07.2017.
 */
public enum IconSize {
    TINY(8),
    SMALL(16),
    MEDIUM(24),
    LARGE(32);

    private final int size;

    IconSize(int s) {
        this.size = s;
    }

    public int getSize() {
        return size;
    }

}
