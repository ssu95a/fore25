package ru.inversion.fore.form.action;

import org.junit.jupiter.api.Test;
import ru.inversion.fore.form.control.ForeButton;
import ru.inversion.fore.form.control.ForeMenuItem;
import ru.inversion.fore.form.control.ForeToolBar;

import java.beans.Introspector;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

/** Structural JavaBean/Scene Builder contract, independent from a GUI smoke test. */
class ForeSceneBuilderContractTest
{
   @Test
   void controlsExposeZeroArgConstructors() throws Exception
   {
      assertNotNull(ForeButton.class.getConstructor());
      assertNotNull(ForeMenuItem.class.getConstructor());
      assertNotNull(ForeToolBar.class.getConstructor());
   }

   @Test
   void buttonAndMenuExposeStandardActionProperty() throws Exception
   {
      for( Class<?> type : new Class<?>[]{ ForeButton.class, ForeMenuItem.class } )
      {
         var info = Introspector.getBeanInfo(type);
         var p = Arrays.stream(info.getPropertyDescriptors())
                 .filter(prop -> prop.getName().equals("standardAction"))
                 .findFirst().orElseThrow();
         assertEquals(StandardAction.class, p.getPropertyType());
         assertNotNull(p.getReadMethod());
         assertNotNull(p.getWriteMethod());
      }
   }
}
