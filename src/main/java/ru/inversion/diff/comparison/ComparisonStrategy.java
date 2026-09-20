package ru.inversion.diff.comparison;

import ru.inversion.diff.DiffItem;
import ru.inversion.diff.DiffState;

public interface ComparisonStrategy {

    DiffState compare(DiffItem item);

}
