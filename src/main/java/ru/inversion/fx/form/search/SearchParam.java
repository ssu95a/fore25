package ru.inversion.fx.form.search;

import java.util.function.Predicate;

/** */
public class SearchParam<T> {

    final private Predicate<T> predicate; // условие

    final boolean dir; // Направления поиска

    final boolean from; // С начала/с текущей позиции

    final boolean leafOnly;

    final boolean wrapAround;

    public SearchParam(Predicate< T > predicate, boolean dir, boolean from, boolean leafOnly, boolean wrapAround) {
        this.predicate = predicate;
        this.dir       = dir;
        this.from      = from;
        this.leafOnly  = leafOnly;
        this.wrapAround = wrapAround;
    }

    public Predicate< T > getPredicate() {
        return predicate;
    }

    public boolean isDown() {
        return !dir;
    }
    public boolean isUp  () {
        return  dir;
    }

    public boolean isCurrent() { return !from; }
    public boolean isBegin()   { return  from; }

    public boolean isLeafOnly(){ return leafOnly; }

    public boolean wrapAround() {
        return wrapAround;
    }
}
