package ru.inversion.fore.form.action;

import javafx.event.ActionEvent;
import javafx.scene.input.KeyCombination;
import org.controlsfx.control.action.Action;

import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * One mutable runtime operation. Defaults are copied from StandardAction.
 * Direct ControlsFX inheritance is intentional for Fore25 v0.2.
 */
public final class ForeAction extends Action
{
   private final StandardAction standardType;
   private IconSpec icon;
   private List<KeyCombination> hotkeys = List.of();

   ForeAction(StandardAction type, Consumer<ActionEvent> handler)
   {
      super(
              Objects.requireNonNull(type, "type").text(),
              Objects.requireNonNull(handler, "handler")
      );

      standardType = type;

      setLongText(type.tooltip());
      setIcon(type.icon());
      setHotkeys(type.hotkeys());
   }

   public StandardAction standardType()
   {
      return standardType;
   }

   public IconSpec icon()
   {
      return icon;
   }

   /** Local override; it never modifies the standard descriptor. */
   public void setIcon(IconSpec icon)
   {
      this.icon = Objects.requireNonNull(icon, "icon");
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
      Objects.requireNonNull(hotkeys, "hotkeys");
      this.hotkeys = List.copyOf(hotkeys);
      setAccelerator(this.hotkeys.isEmpty() ? null : this.hotkeys.get(0));
   }
}
