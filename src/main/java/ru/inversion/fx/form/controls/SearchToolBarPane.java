package ru.inversion.fx.form.controls;

import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import ru.inversion.icons.IconFactory;
import ru.inversion.icons.enums.FontAwesome;

public abstract class SearchToolBarPane<T extends JInvSearchToolBar> extends AnchorPane implements ISearchPane {

    private final VBox rootBox;
    private final T toolBar;

    public SearchToolBarPane(T toolBar) {
        this.rootBox = new VBox();
        this.toolBar = toolBar;
        hideSearchBar();

        initPane();

        final KeyCodeCombination ctrlF = new KeyCodeCombination(KeyCode.F, KeyCombination.CONTROL_DOWN);
        setOnKeyPressed(event -> {
            if (ctrlF.match(event)) {
                showSearchBar();
                toolBar.requestFocus();
            } else if (event.getCode() == KeyCode.ESCAPE) {
                hideSearchBar();
            }
        });

        // Кнопка закрытия toolBar'а
        final Pane rightSpace = new Pane();
        HBox.setHgrow(rightSpace, Priority.SOMETIMES);
        final Label faClose = IconFactory.getLabel(FontAwesome.fa_close, Color.GRAY);
        final JInvButton closeBtn = new JInvButton();
        closeBtn.setFocusTraversable(false);
        closeBtn.getStyleClass().add("transparentButton");
        closeBtn.setGraphic(faClose);
        closeBtn.setOnAction(event -> {
            hideSearchBar();
        });
        toolBar.getItems().addAll(rightSpace, closeBtn);
    }

    protected final void addContentPane(Node content) {
        VBox.setVgrow(content, Priority.ALWAYS);
        rootBox.getChildren().add(content);
    }

    private void initPane() {
        AnchorPane.setTopAnchor(rootBox, 0.0);
        AnchorPane.setRightAnchor(rootBox, 0.0);
        AnchorPane.setBottomAnchor(rootBox, 0.0);
        AnchorPane.setLeftAnchor(rootBox, 0.0);

        rootBox.getChildren().add(toolBar);

        getChildren().add(rootBox);
    }

    @Override
    public void showSearchBar() {
        toolBar.setManaged(true);
        toolBar.setVisible(true);
    }

    @Override
    public void hideSearchBar() {
        toolBar.setManaged(false);
        toolBar.setVisible(false);
    }

    protected VBox getRootBox() {
        return rootBox;
    }

    public T getToolBar() {
        return toolBar;
    }

}
