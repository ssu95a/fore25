package ru.inversion.diff.comparison;

import ru.inversion.diff.DiffItem;

public interface ComparisonResolver {

    ComparisonStrategy resolveComparisonStrategy(DiffItem item);

}
