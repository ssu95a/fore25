package ru.inversion.fx.form.controls.sheet;
import java.util.Objects;
import javafx.scene.control.Label;
import ru.inversion.icons.IconDescriptor;
import ru.inversion.icons.IconDescriptorBuilder;
import ru.inversion.icons.IconFactory;
import ru.inversion.icons.enums.FontAwesome;

/** Содержит название страницы и векторную картинку
 @author fomishkin on 30.07.2018. */
public class SettingsPage {
    public static SettingsPage NO_ACCESS = new SettingsPage( "NO_ACCESS" );

    private final String name;
    private final IconDescriptor graphic;

    public SettingsPage( final String name, final IconDescriptor graphic ) {
        this.name = name;
        this.graphic = graphic;
    }

    public SettingsPage( final String name ) {
        this(name, IconDescriptorBuilder.of( FontAwesome.empty ) );
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return getName();
    }

    public Label getGraphic() {
        return IconFactory.getLabel(graphic);
    }

    @Override
    public boolean equals( final Object o ) {
        if ( this == o ) {
            return true;
        }
        if ( o == null || getClass() != o.getClass() ) {
            return false;
        }
        final SettingsPage that = (SettingsPage) o;
        return Objects.equals( name, that.name ) && Objects.equals( graphic, that.graphic );
    }

    @Override
    public int hashCode() {
        return Objects.hash( name, graphic );
    }
}
