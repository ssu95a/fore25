package ru.inversion.fore.form.control;

import javafx.beans.value.ObservableValue;
import javafx.scene.control.Control;
import javafx.util.Callback;

import ru.inversion.utils.Checks;

import java.util.function.Predicate;


public final class ValueExtractors
{
   private ValueExtractors()
   {
   }


   /**
    * Получить ObservableValue, представляющий value Control.
    */
   public static ObservableValue<?> observable(Control control)
   {
      Checks.Require.object(control, "control");

      final Callback<Control, ObservableValue<?>> extractor =
              org.controlsfx.tools.ValueExtractor.getObservableValueExtractor(control)
                      .orElseThrow(
                              () -> new IllegalArgumentException(
                                      "Value extractor not found for "
                                              + control.getClass().getName()
                              )
                      );

      return Checks.Require.object( extractor.call(control), "observable" );
   }


   /**
    * Получить текущее value Control.
    */
   public static Object valueOf(Control control)
   {
      return observable(control).getValue();
   }


   /**
    * Зарегистрировать extractor для собственного Control.
    */
   public static void register(
           Predicate<Control> test,
           Callback<Control, ObservableValue<?>> extractor )
   {
      Checks.Require.objects(
              test,      "test",
              extractor, "extractor"
      );

      org.controlsfx.tools.ValueExtractor
              .addObservableValueExtractor(
                      test,
                      extractor
              );
   }
}