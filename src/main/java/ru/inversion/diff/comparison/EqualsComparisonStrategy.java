package ru.inversion.diff.comparison;

import ru.inversion.diff.DiffItem;
import ru.inversion.diff.DiffState;

public class EqualsComparisonStrategy implements ComparisonStrategy {

    @Override
    public DiffState compare(DiffItem item) {
        if (item.getOldValue() == null && item.getNewValue() == null) {
            return DiffState.UNCHANGED;
        } else if (item.getOldValue() == null && item.getNewValue() != null) {
            return DiffState.CHANGED;
        } else if (item.getOldValue().equals(item.getNewValue())) {
            return DiffState.UNCHANGED;
        } else {
            return DiffState.CHANGED;
        }
    }
}
