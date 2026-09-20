package ru.inversion.fx.form.controls.textarea;

import ru.inversion.fx.form.controls.SearchToolBarPane;

public class JInvSearchTextPane extends SearchToolBarPane<TextAreaSearchToolBar> {

    private final JInvVirtualizedScrollPane scrollPane;

    public JInvSearchTextPane() {
        super(new TextAreaSearchToolBar(new JInvStyleTextArea()));

        this.scrollPane = new JInvVirtualizedScrollPane<>(getToolBar().getStyleTextArea());
        addContentPane(scrollPane);
    }

    public JInvStyleTextArea getTextArea() {
        return getToolBar().getStyleTextArea();
    }

    public JInvVirtualizedScrollPane getScrollPane() {
        return scrollPane;
    }
}
