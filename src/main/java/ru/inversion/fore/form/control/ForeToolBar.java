package ru.inversion.fore.form.control;

import javafx.scene.Node;
import javafx.scene.control.ToolBar;

/** Drag-and-drop Fore toolbar for Gluon Scene Builder. */
public class ForeToolBar extends ToolBar
{
   public ForeToolBar()
   {
      getStyleClass().add("fore-toolbar");
   }

   public ForeToolBar(Node... items)
   {
      super(items);
      getStyleClass().add("fore-toolbar");
   }
}
