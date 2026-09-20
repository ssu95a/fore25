package ru.inversion.fx.form.controls.treetable;

import javafx.scene.control.TreeItem;

import java.util.function.Predicate;

@FunctionalInterface
public interface ITreeItemPredicate<T> {

    boolean test(TreeItem<T> parent, T value);

    static <T> ITreeItemPredicate<T> create(Predicate<T> predicate) {
        return (parent, value) -> predicate.test(value);
    }

}

