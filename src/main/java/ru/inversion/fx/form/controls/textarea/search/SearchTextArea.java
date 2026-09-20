package ru.inversion.fx.form.controls.textarea.search;

import org.fxmisc.richtext.GenericStyledArea;
import ru.inversion.fx.form.controls.textarea.model.TextAreaSearchResult;

public abstract class SearchTextArea {

    protected final GenericStyledArea textArea;

    SearchTextArea(GenericStyledArea textArea) {
        this.textArea = textArea;
    }

    /**
     * Находит искомый текст в textArea
     *
     * @return результат поиска. null - если ничего не нашлось
     */
    public abstract TextAreaSearchResult search(String searchText);

}
