package ru.inversion.fx.form.controls;

import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.css.PseudoClass;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.paint.Color;
import org.controlsfx.control.textfield.CustomTextField;
import org.controlsfx.control.textfield.TextFields;
import ru.inversion.fx.form.ActionFactory;
import ru.inversion.icons.IconDescriptorBuilder;
import ru.inversion.icons.IconFactory;
import ru.inversion.icons.enums.Entypo;
import ru.inversion.icons.enums.FontAwesome;
import ru.inversion.utils.S;

public abstract class JInvSearchToolBar extends ToolBar implements ISearchToolBar {

    protected final CustomTextField textField;

    protected final ButtonBase searchUp;

    protected final ButtonBase searchDown;

    protected final CheckBox matchCaseCB;

    protected final CheckBox matchWord;

    protected final CheckBox regExCB;

    protected final Label matchLabel;

    private final BooleanProperty found = new SimpleBooleanProperty();

    protected JInvSearchToolBar() {
        this.textField = (CustomTextField) TextFields.createClearableTextField();
        this.textField.getStyleClass().add("searchField");
        this.textField.setMinWidth(100);
        this.textField.setMaxWidth(400);
        HBox.setHgrow(textField, Priority.SOMETIMES);
        final Label searchIcon = IconFactory.getLabel(FontAwesome.fa_search, Color.GRAY);
        searchIcon.getStyleClass().add("textFieldButton");
        this.textField.setLeft(searchIcon);
        this.searchUp = ActionFactory.createButton(new IconDescriptorBuilder<>(Entypo.icon_up).build(), event -> searchUp());
        this.searchDown = ActionFactory.createButton(new IconDescriptorBuilder<>(Entypo.icon_down).build(), event -> searchDown());
        this.searchUp.disableProperty().bind(Bindings.not(found));
        this.searchDown.disableProperty().bind(Bindings.not(found));
        this.matchCaseCB = new CheckBox("Учитывать регистр");
        matchCaseCB.setFocusTraversable(false);
        this.matchWord = new CheckBox("Слова");
        matchWord.setFocusTraversable(false);
        this.regExCB = new CheckBox("RegEx");
        regExCB.setFocusTraversable(false);

        this.matchLabel = new JInvLabel();
        matchLabel.visibleProperty().bind(Bindings.isNotEmpty(textField.textProperty()));
        matchLabel.setStyle("-fx-font-weight: bold");
        final Separator separator = new Separator();
        separator.visibleProperty().bind(matchLabel.visibleProperty());

        getItems().addAll(textField, searchUp, searchDown, matchCaseCB, matchWord, regExCB, separator, matchLabel);

        initListener();
    }

    private void initListener() {
        textField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (S.isNotNullOrEmpty(newValue)) {
                searchAndMark_impl(newValue);
            } else {
                notFound();
                clearMark();
            }
        });
        matchCaseCB.selectedProperty().addListener((observable, oldValue, newValue) -> {
            searchFromTextField();
        });
        regExCB.selectedProperty().addListener((observable, oldValue, newValue) -> {
            searchFromTextField();
        });
        matchWord.selectedProperty().addListener((observable, oldValue, newValue) -> {
            searchFromTextField();
        });
        visibleProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue) {
                clearMark();
            } else {
                searchFromTextField();
            }
        });
    }

    /**
     * Берет искомый текст из textField'а, а затем ищет и маркирует
     */
    public void searchFromTextField() {
        final String text = textField.getText();
        if (S.isNotNullOrEmpty(text)) {
            searchAndMark_impl(text);
        }
    }

    protected void found(int count) {
        matchLabel.setText(count + " найдено");
        textField.pseudoClassStateChanged(PseudoClass.getPseudoClass("fail"), false);
        found.set(true);
    }

    protected void notFound() {
        notFound(false);
    }

    protected void notFound(boolean highlightField) {
        matchLabel.setText("Нет совпадений");
        textField.pseudoClassStateChanged(PseudoClass.getPseudoClass("fail"), highlightField);
        found.set(false);
    }

    protected abstract void searchUp();

    protected abstract void searchDown();

    private void searchAndMark_impl(String searchText) {
        final int count = searchAndMark(searchText);
        if (count > 0) {
            found(count);
        } else {
            notFound(true);
        }
    }

    public CustomTextField getTextField() {
        return textField;
    }

    public ButtonBase getSearchUp() {
        return searchUp;
    }

    public ButtonBase getSearchDown() {
        return searchDown;
    }

    public CheckBox getMatchCaseCB() {
        return matchCaseCB;
    }

    public CheckBox getMatchWord() {
        return matchWord;
    }

    public CheckBox getRegExCB() {
        return regExCB;
    }

    public Label getMatchLabel() {
        return matchLabel;
    }

    public boolean isFound() {
        return found.get();
    }

    public BooleanProperty foundProperty() {
        return found;
    }
}
