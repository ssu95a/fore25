package ru.inversion.fx.form.controls.textarea.model;

import org.fxmisc.richtext.model.StyleSpans;
import ru.inversion.fx.form.controls.textarea.search.TextPosSelector;

import java.util.Collection;

/** Результат поиска в TextArea */
public class TextAreaSearchResult {

    /** Содержит коллекцию и логику пермещений по тексту */
    private final TextPosSelector posSelector;

    /** Добавить стиль*/
    private final StyleSpans<Collection<String>> styleSpans;

    private final int matchCount;

    public TextAreaSearchResult(TextPosSelector posSelector, StyleSpans<Collection<String>> styleSpans) {
        this.posSelector = posSelector;
        this.styleSpans = styleSpans;
        this.matchCount = this.posSelector.getPosRangeList().size();
    }

    public int getMatchCount() {
        return matchCount;
    }

    public StyleSpans<Collection<String>> getStyleSpans() {
        return styleSpans;
    }

    public TextPosSelector getPosSelector() {
        return posSelector;
    }
}
