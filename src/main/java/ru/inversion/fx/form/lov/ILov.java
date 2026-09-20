package ru.inversion.fx.form.lov;

import ru.inversion.fx.form.ViewContext;
import ru.inversion.fx.form.controls.IJInvControl;

import java.util.function.BiConsumer;

/**
 *
 * @author ssu
 * @param <T>
 */
public interface ILov<T> {

    T getValue( );

   boolean checkValue( T value );

   void showChoiceList( ViewContext vc, String filterString, BiConsumer<Boolean,ILov<T>> clb );
   /**
    * Иной раз в специфическом лове требуются знания о контроле к которому оный привязан.
    * @param ctrl
    */
   default void onSetLov( IJInvControl ctrl ) {}
}
