package ru.inversion.diff;

import java.util.Objects;

public final class DiffItemResult implements Comparable<DiffItemResult> {

    private final DiffItem item;

    private final DiffState state;

    private final DiffItemInfo itemInfo;

    DiffItemResult(DiffItem item, DiffState state, DiffItemInfo itemInfo) {
        this.item = Objects.requireNonNull(item, "item can't be null");
        this.state = Objects.requireNonNull(state, "state can't be null");
        this.itemInfo = Objects.requireNonNull(itemInfo, "itemInfo can't be null");
    }

    public DiffItem getItem() {
        return item;
    }

    public DiffState getState() {
        return state;
    }

    public DiffItemInfo getItemInfo() {
        return itemInfo;
    }

    @Override
    public String toString() {
        return "DiffItemResult{" +
                "oldValue=" + item.getOldValue() +
                ", newValue=" + item.getNewValue() +
                ", state=" + state +
                '}';
    }

    @Override
    public int compareTo(DiffItemResult o) {
        return this.getItem().getCategory().compareTo(this.getItem().getCategory());
    }
}
