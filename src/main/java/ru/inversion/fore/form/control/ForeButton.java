package ru.inversion.fore.form.control;

import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import org.controlsfx.control.action.ActionUtils;
import ru.inversion.fore.form.action.ActionPrototype;
import ru.inversion.fore.form.action.ForeAction;
import ru.inversion.fore.form.action.ForeActions;
import ru.inversion.fore.form.action.StandardAction;

import java.util.Objects;

/**
 * JavaFX/Scene Builder-friendly standard button. The FXML bean property
 * standardAction applies visual defaults without replacing onAction.
 * setAction(ForeAction) activates the independent, live runtime binding mode.
 */
public class ForeButton extends Button implements IForeControl
{
   private StandardAction standardAction;
   private ForeAction action;

   private String defaultText;

   private Tooltip defaultTooltip;
   private String defaultTooltipText;

   private Node defaultGraphic;

   public ForeButton()
   {
      getStyleClass().add("fore-action-button");
      setFocusTraversable(false);
   }

   public ForeButton(ForeAction action)
   {
      this();
      setAction(action);
   }

   // FXMLLoader ищет JavaBean-методы в классах, а не default-методы интерфейсов.
   @Override
   public String getFieldName()
   {
      return IForeControl.super.getFieldName();
   }

   @Override
   public void setFieldName(String fieldName)
   {
      IForeControl.super.setFieldName(fieldName);
   }

   @Override
   public Label getLabel()
   {
      return IForeControl.super.getLabel();
   }

   @Override
   public void setLabel(Label label)
   {
      IForeControl.super.setLabel(label);
   }

   public StandardAction getStandardAction()
   {
      return standardAction;
   }

   /** Does not touch onAction, regardless of FXML setter order. */
   public void setStandardAction(StandardAction type)
   {
      if( action != null )
          setAction(null);

      // Only overwrite values previously installed by Fore, not FXML overrides.
      final boolean replaceText = getText() == null || getText().isEmpty() || Objects.equals(getText(), defaultText);
      final boolean replaceTooltip = getTooltip() == null || (getTooltip() == defaultTooltip && Objects.equals(getTooltip().getText(), defaultTooltipText));
      final boolean replaceGraphic = getGraphic() == null || getGraphic() == defaultGraphic;

      standardAction = type;

      if( type == null )
      {
         if( replaceText )
             setText(null);

         if( replaceTooltip )
             setTooltip(null);

         if( replaceGraphic )
             setGraphic(null);

         defaultText    = null;
         defaultTooltip = null;
         defaultTooltipText = null;
         defaultGraphic = null;

         return;
      }

      final ActionPrototype prototype = ForeActions.prototype(type);

      defaultText        = prototype.text();
      defaultTooltipText = prototype.tooltip();
      defaultTooltip     = new Tooltip(defaultTooltipText);
      defaultGraphic     = prototype.icon().newGraphic();

      if( replaceText )
          setText(defaultText);

      if( replaceTooltip )
          setTooltip(defaultTooltip);

      if( replaceGraphic )
          setGraphic(defaultGraphic);
   }

   @java.beans.Transient // Runtime binding, not a serialized design property.
   public ForeAction getAction()
   {
      return action;
   }

   /** Programmatic shared-action mode: do not combine with FXML onAction. */
   public void setAction(ForeAction a)
   {
      if( action == a)
         return;
      if( action != null )
          ActionUtils.unconfigureButton(this);

      action = a;

      if( a != null )
      {
         standardAction = a.standardType();
         ActionUtils.configureButton(a, this);
      }
      else
      {
         final StandardAction oldType = standardAction;
         standardAction = null;
         setText(null);
         setGraphic(null);
         setTooltip(null);
         setDisable(false);

         defaultText = null;
         defaultTooltip = null;
         defaultTooltipText = null;
         defaultGraphic = null;

         if( oldType != null )
             setStandardAction(oldType);
      }
   }
}
