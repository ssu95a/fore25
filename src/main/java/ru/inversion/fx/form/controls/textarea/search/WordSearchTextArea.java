package ru.inversion.fx.form.controls.textarea.search;

import org.fxmisc.richtext.GenericStyledArea;
import org.fxmisc.richtext.model.StyleSpansBuilder;
import ru.inversion.fx.form.controls.textarea.model.TextAreaSearchResult;

import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class WordSearchTextArea extends RegExSearchTextArea {

    public WordSearchTextArea(GenericStyledArea textArea, boolean matchCase) {
        super(textArea, matchCase);
    }

    @Override
    public TextAreaSearchResult search(String searchText) {
        spansBuilder = new StyleSpansBuilder<>();
        lastKwEnd = 0;
        try {
            final Pattern pattern;
            if (matchCase) {
                pattern = Pattern.compile("\\b" + searchText + "\\b");
            } else {
                pattern = Pattern.compile("(?i)\\b" + searchText + "\\b");
            }
            final TextPosSelector textPosSelector = super.checkMatch(pattern, super.textArea.getText());
            if (!textPosSelector.getPosRangeList().isEmpty()) {
                return new TextAreaSearchResult(textPosSelector, super.spansBuilder.create());
            }
        } catch (PatternSyntaxException ignored) {
        }

        return null;
    }
}
