package ru.inversion.fx.form.search;

import javafx.beans.NamedArg;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import ru.inversion.icons.IconFactory;
import ru.inversion.icons.enums.FontAwesome;
import ru.inversion.icons.enums.IconSize;

import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;

/**
 * Диалог поиска, в зависимости от типа данных
 * сервис
 *  для TreeTable добавить
 *  - искать только в листовых
 *  - искать с начала
 * */
public abstract class BaseSearchDialog<T> extends Dialog< SearchParam<T> > {

    static protected int MAX_HISTORY_SIZE = 10;

    static final protected Map<Object, Map<? extends Object,Integer> > g_searchHistory = new HashMap<>();

    protected static final ResourceBundle foreBundle = ResourceBundle.getBundle("fore");

    protected final GridPane grid;
    protected final Label    label;
    protected final Object   objId;

    /** */
    public BaseSearchDialog() {
        this(null,null);
    }

    /** */
    public BaseSearchDialog( Object objId, @NamedArg("defaultValue") T defaultValue )
    {
        this.objId = objId;
        setResizable(true);

        initModality(Modality.NONE);

        final DialogPane dialogPane = getDialogPane();
        // -- label
        label = createContentLabel(dialogPane.getContentText());
        label.setPrefWidth(Region.USE_COMPUTED_SIZE);
        label.textProperty().bind(dialogPane.contentTextProperty());

        this.grid = new GridPane();
        this.grid.setHgap(10);
        this.grid.setVgap(10);
        this.grid.setMaxWidth ( Double.MAX_VALUE );
        this.grid.setAlignment( Pos.CENTER_LEFT  );

        Label l = IconFactory.getLabel( FontAwesome.fa_search, IconSize.LARGE, Color.LIGHTSKYBLUE );
        l.setStyle( l.getStyle() + ";-fx-font-size:2em;");
        this.setGraphic( l );

        dialogPane.contentTextProperty().addListener(o -> updateGrid());

        setTitle( foreBundle.getString("SEARCH_DIALOG.TITLE") );
        dialogPane.setHeaderText(foreBundle.getString("SEARCH_DIALOG.HEADER"));
        dialogPane.getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        setResultConverter((dialogButton) -> {
            ButtonBar.ButtonData data = dialogButton == null ? null : dialogButton.getButtonData();
            return data == ButtonBar.ButtonData.OK_DONE ? makeParam() : null;
        });
    }

    abstract protected void updateGrid();

    abstract protected SearchParam<T> makeParam();


    /** */
    private Label createContentLabel(String text) {
        Label label = new Label(text);
        label.setMaxWidth (Double.MAX_VALUE);
        label.setMaxHeight(Double.MAX_VALUE);
        label.getStyleClass().add("content");
        label.setWrapText(true);
        label.setPrefWidth(360);
        return label;
    }

}
