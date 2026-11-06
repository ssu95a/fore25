package ru.inversion.fx.help.controller.search;

import javafx.scene.layout.VBox;
import ru.inversion.fx.form.controls.SearchToolBarPane;
import ru.inversion.fx.help.controller.WebViewDecorator;

public class WebViewSearchPane extends SearchToolBarPane<WebViewSearchToolBar> {

    public WebViewSearchPane(WebViewDecorator webViewDecorator) {
        super(new WebViewSearchToolBar(webViewDecorator));

        addContentPane(getToolBar().getWebViewDecorator().getWebView());
    }

    public WebViewDecorator getWebViewDecorator() {
        return getToolBar().getWebViewDecorator();
    }

    @Override
    public VBox getRootBox() {
        return super.getRootBox();
    }
}
