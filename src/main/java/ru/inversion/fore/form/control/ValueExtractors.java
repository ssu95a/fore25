package ru.inversion.fore.form.control;

import javafx.scene.control.*;

import ru.inversion.utils.Checks;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;


public final class ValueExtractors
{
   private static final List<Entry> extractors = new ArrayList<>();

   static {
      register( TextInputControl.class, TextInputControl::getText );
      register( ComboBoxBase.class, ComboBoxBase::getValue );
      register( ChoiceBox.class, ChoiceBox::getValue );
      register( CheckBox.class, CheckBox::isSelected );
   }


   private ValueExtractors()
   { }


   /**
    * Зарегистрировать extractor для Control данного типа.
    *
    * Более поздняя регистрация имеет больший приоритет.
    * Это позволяет переопределять стандартное поведение
    * для собственных Control.
    */
   public static synchronized <C extends Control, T> void register( Class<C> controlClass, Function<? super C, ? extends T> extractor )
   {
      Checks.Require.objects( controlClass, "controlClass", extractor, "extractor");
      extractors.add( 0, new Entry( controlClass, adapt(controlClass, extractor) ) );
   }


   /**
    * Найти extractor для конкретного Control.
    */
   public static synchronized Function<Control, Object> extractor( Control control )
   {
      //Checks.Require.object( control, "control" );

      if( control == null )
          return c->null;

      final Class<?> controlClass = control.getClass();

      for( Entry entry : extractors )
      {
         if( entry.controlClass().isAssignableFrom(controlClass) )
             return entry.extractor();
      }

      if( control.getClass().isAssignableFrom(Labeled.class) )
          return c -> ((Labeled) c).getText();

      throw new IllegalArgumentException( "Value extractor not found for " + controlClass.getName() );
   }


   /**
    * Сразу получить текущее value Control.
    *
    * Удобно не только для validation.
    */
   public static Object valueOf(Control control)
   {
      return extractor(control).apply(control);
   }


   /**
    * Единственное место, где typed Function превращается
    * во внутренний универсальный extractor.
    */
   private static <C extends Control, T>
   Function<Control, Object> adapt( Class<C> controlClass, Function<? super C, ? extends T> extractor )
   {
      return control ->extractor.apply(controlClass.cast(control));
   }

   /** */
   private record Entry( Class<? extends Control> controlClass, Function<Control, Object> extractor ) { }
}