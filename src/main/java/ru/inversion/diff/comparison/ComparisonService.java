package ru.inversion.diff.comparison;

import ru.inversion.diff.DiffItem;
import ru.inversion.diff.helper.DiffClassHelper;

public class ComparisonService implements ComparisonResolver {

    private static final ComparisonStrategy COMPARABLE_COMPARISON_STRATEGY = new ComparableComparisonStrategy();
    private static final ComparisonStrategy EQUALS_ONLY_COMPARISON_STRATEGY = new EqualsComparisonStrategy();

    @Override
    public ComparisonStrategy resolveComparisonStrategy(DiffItem item) {
        final Class valueType = item.getValueType();
        if (DiffClassHelper.isSimpleType(valueType)) {
            if (Comparable.class.isAssignableFrom(valueType)) {
                return COMPARABLE_COMPARISON_STRATEGY;
            } else {
                return EQUALS_ONLY_COMPARISON_STRATEGY;
            }
        }

        if (valueType == Object.class) {
            return EQUALS_ONLY_COMPARISON_STRATEGY;
        }

        return null;
    }
}
