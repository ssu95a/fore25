package ru.inversion.fx.form.controls.textarea.search;

import javafx.scene.control.IndexRange;
import org.fxmisc.richtext.GenericStyledArea;
import org.fxmisc.richtext.model.StyleSpansBuilder;
import ru.inversion.fx.form.controls.textarea.model.TextAreaSearchResult;

import java.util.Collection;
import java.util.Collections;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class RegExSearchTextArea extends SearchTextArea {

    protected StyleSpansBuilder<Collection<String>> spansBuilder;
    protected int lastKwEnd;
    protected final boolean matchCase;

    public RegExSearchTextArea(GenericStyledArea textArea, boolean matchCase) {
        super(textArea);
        this.matchCase = matchCase;
    }

    @Override
    public TextAreaSearchResult search(String searchText) {
        spansBuilder = new StyleSpansBuilder<>();
        lastKwEnd = 0;
        try {
            final Pattern pattern;
            if (matchCase) {
                pattern = Pattern.compile(searchText);
            } else {
                pattern = Pattern.compile(searchText, Pattern.CASE_INSENSITIVE);
            }
            final TextPosSelector textPosSelector = checkMatch(pattern, super.textArea.getText());
            if (!textPosSelector.getPosRangeList().isEmpty()) {
                return new TextAreaSearchResult(textPosSelector, spansBuilder.create());
            }
        } catch (PatternSyntaxException ignored) {
        }

        return null;
    }

    /**
     * Проходится по списку всего найденного исходя из текущего pattern'а
     * А также формирует стиль, для возможности дальнейшего подсвечивания найденного текста
     *
     * @return список промежутков, в которых был найден текст
     */
    protected TextPosSelector checkMatch(Pattern pattern, String text) {
        final TextPosSelector textPosSelector = new TextPosSelector();
        final Matcher matcher = pattern.matcher(text);

        while (matcher.find()) {
            spansBuilder.add(Collections.singleton("highlightText_transparent"), matcher.start() - lastKwEnd);
            spansBuilder.add(Collections.singleton("highlightText"), matcher.end() - matcher.start());
            lastKwEnd = matcher.end();
            textPosSelector.addPos(new IndexRange(matcher.start(), matcher.end()));
        }
        spansBuilder.add(Collections.singleton("highlightText_transparent"), textArea.getText().length() - lastKwEnd);
        return textPosSelector;
    }

}
