package ru.inversion.fx.form.controls.textarea.search;

import javafx.scene.control.IndexRange;
import org.fxmisc.richtext.StyledTextArea;
import org.fxmisc.richtext.model.StyleSpansBuilder;
import ru.inversion.fx.app.es.JInvErrorService;
import ru.inversion.fx.form.controls.textarea.model.TextAreaSearchResult;

import java.io.IOException;
import java.io.StringReader;
import java.util.Collection;
import java.util.Collections;

public class CharSearchTextArea extends SearchTextArea {
    private StringReader stringReader;
    private final boolean matchCase;

    private int ch = -1;
    private int count = 0; // текущая позиция

    private StyleSpansBuilder<Collection<String>> spansBuilder;
    private int lastKwEnd = 0;

    public CharSearchTextArea(StyledTextArea textArea) {
        this(textArea, false);
    }

    public CharSearchTextArea(StyledTextArea textArea, boolean matchCase) {
        super(textArea);
        this.matchCase = matchCase;
    }

    private int nextChar() throws IOException {
        ch = stringReader.read(); // Читаем следующий символ
        count++; // Счетчик текущей позиции
        return ch;
    }

    public TextAreaSearchResult search(String searchText) {
        stringReader = new StringReader(textArea.getText());
        spansBuilder = new StyleSpansBuilder<>();
        char[] searchChars = matchCase ? searchText.toCharArray() : searchText.toLowerCase().toCharArray();
        final TextPosSelector textPosSelector = new TextPosSelector();
        try {
            while (nextChar() != -1) {
                final IndexRange posRange = checkMatch(searchChars);
                if (posRange != null) {
                    textPosSelector.addPos(posRange);
                }
            }
            // Окрашиваем оставшиеся символы
            spansBuilder.add(Collections.singleton("highlightText_transparent"), textArea.getText().length() - lastKwEnd);
        } catch (IOException e) {
            JInvErrorService.handleException(null, e);
        }

        if (!textPosSelector.getPosRangeList().isEmpty()) {
            return new TextAreaSearchResult(textPosSelector, spansBuilder.create());
        }

        return null;
    }

    /**
     * Сверяет символ(ы) с искомыми символами. А также формирует стиль, для возможности
     * дальнейшего подсвечивания найденного текста в textArea
     *
     * @param chars искомые символы
     * @return возвращает позицию найденного текста
     * @throws IOException - если во время чтения символов произошла ошибка
     */
    private IndexRange checkMatch(char[] chars) throws IOException {
        int firstPos = count; // Перед началом поиска сохраняем позицию проверяемого символа
        for (int i = 0; i < chars.length; i++) {
            final char c = matchCase ? ((char) ch) : Character.toLowerCase(((char) ch));
            if ((i == (chars.length - 1)) && c == chars[i]) {

                spansBuilder.add(Collections.singleton("highlightText_transparent"), (firstPos - 1) - lastKwEnd);
                spansBuilder.add(Collections.singleton("highlightText"), count - (firstPos - 1));
                lastKwEnd = count;

                return new IndexRange(firstPos - 1, count);
            }
            if (c == chars[i]) {
                nextChar(); // Берем следующий символ
                continue;
            }
            break;
        }
        return null;
    }
}
