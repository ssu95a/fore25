package ru.inversion.fx.help.controller.search;

import javafx.scene.input.KeyCode;
import ru.inversion.fx.form.controls.JInvSearchToolBar;
import ru.inversion.fx.help.controller.WebViewDecorator;

public class WebViewSearchToolBar extends JInvSearchToolBar {

    private final WebViewDecorator webViewDecorator;

    public WebViewSearchToolBar(WebViewDecorator webViewDecorator) {
        this.webViewDecorator = webViewDecorator;
        super.textField.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) {
                webViewDecorator.selectNextText();
            }
        });
    }

    @Override
    public int searchAndMark(String searchText) {
        clearMark();
        int count = 0;
        if (regExCB.isSelected()) {
            final String flags = matchCaseCB.isSelected() ? "g" : "ig";
            count = webViewDecorator.searchTextRegEx(searchText, flags);
        } else if (matchWord.isSelected()) {
            final String flags = matchCaseCB.isSelected() ? "g" : "ig";
            count = webViewDecorator.searchWord(searchText, flags);
        } else {
            count = webViewDecorator.searchText(searchText, matchCaseCB.isSelected());
        }
        return count;
    }

    @Override
    public void clearMark() {
        webViewDecorator.clearTextMark();
    }

    public WebViewDecorator getWebViewDecorator() {
        return webViewDecorator;
    }

    @Override
    protected void searchUp() {
        webViewDecorator.selectPrevText();
    }

    @Override
    protected void searchDown() {
        webViewDecorator.selectNextText();
    }
}
