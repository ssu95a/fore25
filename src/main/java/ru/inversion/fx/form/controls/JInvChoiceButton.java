/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ru.inversion.fx.form.controls;
import ru.inversion.icons.IBaseIconDescriptor;
import ru.inversion.icons.IconDescriptor;
import ru.inversion.icons.IconFactory;
import ru.inversion.icons.enums.FontAwesome;

/**
 *
 * @author antonovdi
 */
public class JInvChoiceButton extends JInvButton {

    public static final String PROPERTY_CUSTOM_ICON = "ru.inversion.custom_choice_icon";

    public JInvChoiceButton() {
        FontAwesome icon = getProperty( PROPERTY_CUSTOM_ICON, FontAwesome.fa_table );
        init(icon);
    }
    public JInvChoiceButton(FontAwesome icon) {
        init(icon);
    }
    public JInvChoiceButton( IBaseIconDescriptor complexIcon) {
        init(complexIcon);
    }

    private void init( final FontAwesome icon ) {
        init(IconDescriptor.of( icon ));
    }
    private void init( final IBaseIconDescriptor complexIcon ) {
        setGraphic( IconFactory.getLabel( complexIcon ));
    }

    public JInvChoiceButton(String text) {
        super(text);
    }
}
