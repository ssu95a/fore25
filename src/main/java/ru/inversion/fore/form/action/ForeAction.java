package ru.inversion.fore.form.action;

import javafx.event.ActionEvent;
import javafx.scene.input.KeyCombination;
import org.controlsfx.control.action.Action;
import ru.inversion.utils.Checks;

import java.util.List;
import java.util.function.Consumer;

/**
 * One mutable runtime operation. Defaults are copied from an immutable prototype.
 * Direct ControlsFX inheritance is intentional for Fore25 v0.1.
 */
public final class ForeAction extends Action
{
   private final StandardAction standardType;
   private IconSpec icon;
   private List<KeyCombination> hotkeys = List.of();

   ForeAction(ActionPrototype prototype, Consumer<ActionEvent> handler)
   {
      super(
              Checks.Require.object(prototype, "prototype").text(),
              Checks.Require.object(handler, "handler")
      );

      standardType = prototype.type();

      setLongText(prototype.tooltip());
      setIcon(prototype.icon());
      setHotkeys(prototype.hotkeys());
   }

   public StandardAction standardType()
   {
      return standardType;
   }

   public IconSpec icon()
   {
      return icon;
   }

   /** Local override; it never modifies the standard prototype. */
   public void setIcon(IconSpec icon)
   {
      this.icon = Checks.Require.object(icon, "icon");
      setGraphic(icon.newGraphic());
   }

   /** All shortcuts, including alternatives beyond the primary accelerator. */
   public List<KeyCombination> hotkeys()
   {
      return hotkeys;
   }

   /** The first shortcut is also ControlsFX's primary menu accelerator. */
   public void setHotkeys(List<? extends KeyCombination> hotkeys)
   {
      Checks.Require.object(hotkeys, "hotkeys");
      this.hotkeys = List.copyOf(hotkeys);
      setAccelerator(this.hotkeys.isEmpty() ? null : this.hotkeys.get(0));
   }
}
