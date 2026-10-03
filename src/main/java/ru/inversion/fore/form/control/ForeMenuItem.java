package ru.inversion.fore.form.control;

import javafx.scene.Node;
import javafx.scene.control.MenuItem;
import javafx.scene.input.KeyCombination;
import org.controlsfx.control.action.ActionUtils;
import ru.inversion.fore.form.action.ActionPrototype;
import ru.inversion.fore.form.action.ForeAction;
import ru.inversion.fore.form.action.ForeActions;
import ru.inversion.fore.form.action.StandardAction;

import java.util.Objects;

/** FXML MenuItem with standard appearance or a shared runtime ForeAction. */
public class ForeMenuItem extends MenuItem
{
   private ForeAction action;
   private StandardAction standardAction;
   private String defaultText;
   private Node defaultGraphic;
   private KeyCombination defaultAccelerator;

   public ForeMenuItem()
   {
   }

   public ForeMenuItem(ForeAction action)
   {
      setAction(action);
   }

   public StandardAction getStandardAction()
   {
      return standardAction;
   }

   /** Applies only standard presentation; does not replace FXML onAction. */
   public void setStandardAction(StandardAction type)
   {
      if( action != null )
         setAction(null);
      final boolean replaceText = getText() == null || getText().isEmpty()
              || Objects.equals(getText(), defaultText);
      final boolean replaceGraphic = getGraphic() == null || getGraphic() == defaultGraphic;
      final boolean replaceAccelerator = getAccelerator() == null
              || Objects.equals(getAccelerator(), defaultAccelerator);

      standardAction = type;
      if( type == null )
      {
         if( replaceText ) setText(null);
         if( replaceGraphic ) setGraphic(null);
         if( replaceAccelerator ) setAccelerator(null);
         defaultText = null;
         defaultGraphic = null;
         defaultAccelerator = null;
         return;
      }

      final ActionPrototype prototype = ForeActions.prototype(type);
      defaultText = prototype.text();
      defaultGraphic = prototype.icon().newGraphic();
      defaultAccelerator = prototype.hotkeys().isEmpty()
              ? null : prototype.hotkeys().get(0);
      if( replaceText ) setText(defaultText);
      if( replaceGraphic ) setGraphic(defaultGraphic);
      if( replaceAccelerator ) setAccelerator(defaultAccelerator);
   }

   @java.beans.Transient
   public ForeAction getAction()
   {
      return action;
   }

   /** Live ControlsFX binding to the same ForeAction used by ForeButton. */
   public void setAction(ForeAction next)
   {
      if( action == next )
         return;
      if( action != null )
         ActionUtils.unconfigureMenuItem(this);
      action = next;
      if( next != null )
      {
         standardAction = next.standardType();
         ActionUtils.configureMenuItem(next, this);
      }
      else
      {
         final StandardAction oldType = standardAction;
         standardAction = null;
         setText(null);
         setGraphic(null);
         setAccelerator(null);
         setDisable(false);
         defaultText = null;
         defaultGraphic = null;
         defaultAccelerator = null;
         if( oldType != null )
            setStandardAction(oldType);
      }
   }
}
