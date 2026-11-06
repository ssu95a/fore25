package ru.inversion.fx.form.controls.textarea;

import javafx.scene.control.IndexRange;
import javafx.scene.input.KeyCode;
import org.fxmisc.richtext.Caret;
import ru.inversion.fx.form.controls.JInvSearchToolBar;
import ru.inversion.fx.form.controls.textarea.model.TextAreaSearchResult;
import ru.inversion.fx.form.controls.textarea.search.*;
import ru.inversion.utils.S;

public class TextAreaSearchToolBar extends JInvSearchToolBar {

    private final JInvStyleTextArea styleTextArea;

    private TextPosSelector textPosSelector;

    TextAreaSearchToolBar(JInvStyleTextArea styleTextArea) {
        this.styleTextArea = styleTextArea;
        this.styleTextArea.focusedProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue) {
                this.styleTextArea.setShowCaret(Caret.CaretVisibility.ON);
            } else {
                this.styleTextArea.setShowCaret(Caret.CaretVisibility.OFF);
            }
        });
        super.textField.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) {
                if (textPosSelector != null && textPosSelector.size() > 0) {
                    selectRange(textPosSelector.nextPos());
                }
            }
        });
        super.textField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (S.isNullOrEmpty(newValue)) {
                textPosSelector = null;
            }
        });
    }

    @Override
    public int searchAndMark(String searchText) {
        final SearchTextArea searchTextArea;
        if (regExCB.isSelected()) {
            searchTextArea = new RegExSearchTextArea(styleTextArea, matchCaseCB.isSelected());
        } else if (matchWord.isSelected()) {
            searchTextArea = new WordSearchTextArea(styleTextArea, matchCaseCB.isSelected());
        } else {
            searchTextArea = new CharSearchTextArea(styleTextArea, matchCaseCB.isSelected());
        }

        final TextAreaSearchResult textAreaSearchResult = searchTextArea.search(searchText);
        if (textAreaSearchResult != null) {
            textPosSelector = textAreaSearchResult.getPosSelector(); // Объект для "хождения" по позициям
            styleTextArea.setStyleSpans(0, textAreaSearchResult.getStyleSpans());
        } else {
            textPosSelector = null;
            clearMark();
        }
        return textAreaSearchResult != null ? textAreaSearchResult.getMatchCount() : 0;
    }

    @Override
    public void clearMark() {
        styleTextArea.clearStyle(0, styleTextArea.getText().length());
    }

    /**
     * Выделить область
     *
     * @param posRange объект хранящий в себе начало и конец позиции
     */
    private void selectRange(IndexRange posRange) {
        styleTextArea.moveTo(posRange.getStart()); // Передвигаем caret
        styleTextArea.requestFollowCaret(); // Следуем за новой позицией caret
        styleTextArea.selectRange(posRange.getStart(), posRange.getEnd()); // Выделяем текст
    }

    public JInvStyleTextArea getStyleTextArea() {
        return styleTextArea;
    }

    @Override
    protected void searchUp() {
        if (textPosSelector != null && textPosSelector.size() > 0) {
            selectRange(textPosSelector.prevPos());
        }
    }

    @Override
    protected void searchDown() {
        if (textPosSelector != null && textPosSelector.size() > 0) {
            selectRange(textPosSelector.nextPos());
        }
    }
}
