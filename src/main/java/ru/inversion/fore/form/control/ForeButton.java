package ru.inversion.fore.form.control;

import javafx.scene.control.Button;
import org.controlsfx.control.action.ActionUtils;
import ru.inversion.fore.form.action.ForeAction;

/** A standard Fore button. Its visual properties follow the assigned action. */
public class ForeButton extends Button
{
   private ForeAction action;

   public ForeButton()
   {
      getStyleClass().add("fore-action-button");
   }

   public ForeButton(ForeAction action)
   {
      this();
      setAction(action);
   }

   public ForeAction getAction()
   {
      return action;
   }

   /** Supports replacing/unbinding an action without accumulating listeners. */
   public void setAction(ForeAction action)
   {
      if( this.action == action )
         return;

      if( this.action != null )
         ActionUtils.unconfigureButton(this);

      this.action = action;
      if( action == null )
      {
         setText(null);
         setGraphic(null);
         setTooltip(null);
         setDisable(false);
      }
      else
         ActionUtils.configureButton(action, this);
   }
}
