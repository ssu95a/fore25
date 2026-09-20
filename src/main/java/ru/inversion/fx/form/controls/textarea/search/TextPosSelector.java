package ru.inversion.fx.form.controls.textarea.search;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.IndexRange;

import java.util.LinkedList;

public class TextPosSelector {

    private ObservableList<IndexRange> posRangeList;

    private IndexRange currentPos;

    private int prevIndex = -1;

    private int currentIndex = -1;

    private int nextIndex = -1;

    public TextPosSelector() {
        this.posRangeList = FXCollections.observableList(new LinkedList<>());
    }

    public void addPos(IndexRange range) {
        posRangeList.add(range);
    }

    public IndexRange nextPos() {
        if ((currentIndex + 1) >= posRangeList.size()) {
            setToStart();
        }
        final IndexRange posRange = posRangeList.get(++currentIndex);
        currentPos = posRange;
        prevIndex = currentIndex - 1;
        nextIndex = currentIndex + 1;
        return posRange;
    }

    public IndexRange prevPos() {
        if (prevIndex < 0) {
            setToEnd();
        }
        final IndexRange posRange = posRangeList.get(prevIndex);
        currentPos = posRange;
        currentIndex = prevIndex;
        prevIndex--;
        nextIndex = currentIndex + 1;
        return posRange;
    }

    public int size() {
        return posRangeList.size();
    }

    public void clear() {
        posRangeList.clear();
        currentPos = null;
    }

    private void setToStart() {
        prevIndex = -1;
        currentIndex = -1;
        nextIndex = -1;
    }

    private void setToEnd() {
        currentIndex = posRangeList.size();
        prevIndex = currentIndex - 1;
        nextIndex = currentIndex + 1;
    }

    public IndexRange getCurrentPos() {
        return currentPos;
    }

    public ObservableList<IndexRange> getPosRangeList() {
        return posRangeList;
    }

    public void setPosRangeList(ObservableList<IndexRange> posRangeList) {
        this.posRangeList = posRangeList;
    }
}
