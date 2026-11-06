package ru.inversion.fx.form.controls.dsbar;

import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBase;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import ru.inversion.dataset.fx.DSFXAdapter;
import ru.inversion.fx.form.ActionFactory;
import ru.inversion.icons.RowIconDescriptorBuilder;
import ru.inversion.icons.enums.FontAwesome;
import ru.inversion.icons.utils.EnumUtils;

import java.util.ResourceBundle;

/**
 * @author ssu
 */
public class Part_MarkButtons extends AbstractPartBase {

    /** */
    final private DSFXAdapter dsAdapter;

    /** */
    final private HBox pane = new HBox(1.0d);

    /** */
    public Part_MarkButtons( DSFXAdapter adapter, ResourceBundle bundle) {

        super(bundle);

        if (adapter == null)
            throw new IllegalArgumentException("Adapter is null");

        pane.setAlignment(Pos.CENTER_LEFT);

        this.dsAdapter = adapter;

        ButtonBase[] b = new ButtonBase[3];


        b[0] = ActionFactory.createButton(FontAwesome.fa_check_square_o, null, (a) -> markRow(), g_bundle.getString("MARK"));


        b[1] = ActionFactory.createButton(new RowIconDescriptorBuilder().add(EnumUtils.getEnumByCode(FontAwesome.class, "\uf046")).
                        add(EnumUtils.getEnumByCode(FontAwesome.class, "\uf046")).
                        add(EnumUtils.getEnumByCode(FontAwesome.class, "\uf046")).build()
                ,
                 (a) -> markAllRow(),
                g_bundle.getString("MARK_ALL")
        );

        b[2] = ActionFactory.createButton(new RowIconDescriptorBuilder().
                        add(EnumUtils.getEnumByCode(FontAwesome.class, "\uf096")).
                        add(EnumUtils.getEnumByCode(FontAwesome.class, "\uf096")).
                        add(EnumUtils.getEnumByCode(FontAwesome.class, "\uf096")).build()
                ,
                (a) -> unmarkAllRow(),
                g_bundle.getString("UNMARK_ALL")
        );

//
//
//        /createButton( "\uf046 \uf046 \uf046", (a)->markAllRow(),   g_bundle.getString("MARK_ALL")  );
//        b[2] = ActionFactory.createButton( "\uf096 \uf096 \uf096", (a)->unmarkAllRow(), g_bundle.getString("UNMARK_ALL"));


//        b[1] = ActionFactory.createButton( "\uf046 \uf046 \uf046", (a)->markAllRow(),   g_bundle.getString("MARK_ALL")  );
//        b[2] = ActionFactory.createButton( "\uf096 \uf096 \uf096", (a)->unmarkAllRow(), g_bundle.getString("UNMARK_ALL"));

        b[0].prefWidthProperty().bind(b[1].widthProperty());

        pane.getProperties().put(PART, getType());
        pane.getChildren().addAll(b);
    }

    @Override
    public DSInfoBar.PartEnum getType() {
        return DSInfoBar.PartEnum.MarkButtons;
    }

    @Override
    public Pane createControlPane() {
        return pane;
    }

    public void markRow() {
        if (dsAdapter.isEnableMark()) {
            dsAdapter.revertMarkCurrentRow();
        }
    }

    public void markAllRow() {
        dsAdapter.markAll();
    }

    public void unmarkAllRow() {
        dsAdapter.unMarkAll();
    }

    @Override
    public Pane getControlPane() {
        return pane;
    }

}
