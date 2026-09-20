package ru.inversion.diff;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

public final class DiffParamObject {

    private final Collection<DiffItemResult> diffItemCollection;

    private final Consumer<List<DiffItem>> resultItemCollection;

    public DiffParamObject(Collection<DiffItemResult> diffItemCollection, Consumer<List<DiffItem>> resultItemCollection) {
        this.diffItemCollection = Objects.requireNonNull(diffItemCollection, "diffItemCollection can't be null");
        this.resultItemCollection = Objects.requireNonNull(resultItemCollection, "resultItemCollection can't be null");
    }

    public Collection<DiffItemResult> getDiffItemCollection() {
        return diffItemCollection;
    }

    public Consumer<List<DiffItem>> getResultItemCollection() {
        return resultItemCollection;
    }
}
