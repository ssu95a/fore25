package ru.inversion.fore.form.control;

import javafx.scene.control.MenuItem;
import org.controlsfx.control.action.ActionUtils;
import ru.inversion.fore.form.action.ForeAction;

/** A menu representation of the same runtime action used by ForeButton. */
public class ForeMenuItem extends MenuItem
{
   private ForeAction action;

   public ForeMenuItem()
   {
   }

   public ForeMenuItem(ForeAction action)
   {
      setAction(action);
   }

   public ForeAction getAction()
   {
      return action;
   }

   public void setAction(ForeAction action)
   {
      if( this.action == action )
         return;

      if( this.action != null )
         ActionUtils.unconfigureMenuItem(this);

      this.action = action;
      if( action == null )
      {
         setText(null);
         setGraphic(null);
         setAccelerator(null);
         setDisable(false);
      }
      else
         ActionUtils.configureMenuItem(action, this);
   }
}
