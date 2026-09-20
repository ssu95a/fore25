package ru.inversion.fx.form.controls.treetable;

import javafx.beans.binding.Bindings;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.scene.control.TreeItem;

public class FilterTreeItem<T> extends TreeItem<T> {

    private final ObservableList<TreeItem<T>> sourceList;
    private final FilteredList<TreeItem<T>> filteredList;
    private final ObjectProperty<ITreeItemPredicate<T>> predicate;

    public FilterTreeItem(T value) {
        super(value);
        this.sourceList = FXCollections.observableArrayList();
        this.filteredList = new FilteredList<>(this.sourceList);
        this.predicate = new SimpleObjectProperty<>();
        this.filteredList.predicateProperty().bind(Bindings.createObjectBinding(() -> {
            return child -> {
                // Set the predicate of child items to force filtering
                if (child instanceof FilterTreeItem) {
                    FilterTreeItem<T> filterableChild = (FilterTreeItem<T>) child;
                    filterableChild.predicateProperty().set(this.predicate.get());
                }
                // If there is no predicate, keep this tree item
                if (this.predicate.get() == null || !child.getChildren().isEmpty())
                    return true;

                return this.predicate.get().test(this, child.getValue());
            };
        }, this.predicate));

        filteredList.addListener((ListChangeListener<TreeItem<T>>) c -> {
            while (c.next()) {
                getChildren().removeAll(c.getRemoved());
                getChildren().addAll(c.getAddedSubList());
            }
        });
    }

    public ObservableList<TreeItem<T>> getSourceList() {
        return sourceList;
    }

    public ObjectProperty<ITreeItemPredicate<T>> predicateProperty() {
        return predicate;
    }
}
