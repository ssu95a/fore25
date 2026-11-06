package ru.inversion.diff.comparison;

import ru.inversion.diff.DiffItem;
import ru.inversion.diff.DiffState;

public class ComparableComparisonStrategy implements ComparisonStrategy {

    @Override
    public DiffState compare(DiffItem item) {
        if (item.getOldValue() instanceof Comparable && item.getNewValue() instanceof Comparable) {
            if (isEqualByComparison(((Comparable) item.getOldValue()), ((Comparable) item.getNewValue()))) {
                return DiffState.UNCHANGED;
            } else {
                return DiffState.CHANGED;
            }
        } else if (item.getOldValue() == null && item.getNewValue() != null) {
            return DiffState.CHANGED;
        }
        return DiffState.UNCHANGED;
    }

    private <T extends Comparable<T>> boolean isEqualByComparison(T a, T b) {
        if (a == null && b == null) {
            return true;
        } else if (a != null && b != null) {
            return a.compareTo(b) == 0 || b.compareTo(a) == 0;
        }
        return false;
    }
}
