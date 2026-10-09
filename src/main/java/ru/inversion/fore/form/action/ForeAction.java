package ru.inversion.fore.form.action;

import javafx.event.ActionEvent;
import javafx.scene.input.KeyCombination;
import org.controlsfx.control.action.Action;

import java.util.ArrayList;
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

   private final List<Consumer<List<KeyCombination>>> hotkeyValidators = new ArrayList<>();

   ForeAction( StandardAction type, Consumer<ActionEvent> handler )
   {
      super( Objects.requireNonNull(type, "type").text(), Objects.requireNonNull(handler, "handler") );

      standardType = type;

      setLongText ( type.tooltip());
      setIcon     ( type.icon()   );
      setHotkeys  ( type.hotkeys());
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
      final List<KeyCombination> next = List.copyOf(Objects.requireNonNull(hotkeys, "hotkeys"));
      for( var validator : List.copyOf(hotkeyValidators) )
         validator.accept(next);

      // Validate all attached scopes before changing either the keys or the menu accelerator.
      final List<KeyCombination> previous = this.hotkeys;
      this.hotkeys = next;
      try
      {
         setAccelerator(next.isEmpty() ? null : next.get(0));
      }
      catch( RuntimeException | Error ex )
      {
         this.hotkeys = previous;
         throw ex;
      }
   }

   void addHotkeyValidator(Consumer<List<KeyCombination>> validator)
   {
      hotkeyValidators.add(validator);
   }

   void removeHotkeyValidator(Consumer<List<KeyCombination>> validator)
   {
      hotkeyValidators.remove(validator);
   }
}
