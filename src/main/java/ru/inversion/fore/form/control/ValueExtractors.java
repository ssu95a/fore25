package ru.inversion.fore.form.control;

import javafx.beans.value.ObservableValue;
import javafx.scene.control.Control;
import javafx.util.Callback;

import ru.inversion.utils.Checks;

import java.util.Optional;
import java.util.function.Predicate;


public final class ValueExtractors
{
   /** */
   private ValueExtractors()
   { }


   /**
    * Найти ObservableValue, представляющий value Control.
    * <>
    * Отсутствие extractor является допустимым результатом.
    */
   public static Optional<ObservableValue<?>> findObservable(
           Control control )
   {
      Checks.Require.object(control, "control");

      final Optional<Callback<Control, ObservableValue<?>>> extractor =
              org.controlsfx.tools.ValueExtractor
                      .getObservableValueExtractor(control);

      if( extractor.isEmpty() )
         return Optional.empty();

      return Optional.of(
              Checks.Require.object(
                      extractor.get().call(control),
                      "observable"
              )
      );
   }


   /**
    * Получить ObservableValue, представляющий value Control.
    *
    * Если extractor не зарегистрирован, это ошибка.
    */
   public static ObservableValue<?> observable(Control control)
   {
      Checks.Require.object(control, "control");

      return findObservable(control)
              .orElseThrow(
                      () -> new IllegalArgumentException(
                              "Value extractor not found for "
                                      + control.getClass().getName()
                      )
              );
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