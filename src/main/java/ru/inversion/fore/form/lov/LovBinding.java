package ru.inversion.fore.form.lov;

import javafx.application.Platform;
import javafx.beans.property.Property;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.value.ChangeListener;
import javafx.event.EventHandler;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import ru.inversion.fore.form.FormTools;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.function.Supplier;

/** Привязка одного поля к LOV и именованным получателям значений. Владеет экземпляром ForeLov. */
public final class LovBinding implements AutoCloseable
{
   public enum State { UNCHECKED, VALIDATING, VALID, INVALID }

   public record Target<T>(Property<T> property, Function<Object, T> converter)
   {
      public Target { Objects.requireNonNull(property); Objects.requireNonNull(converter); }

      public static <T> Target<T> of(Property<T> property, Class<T> type)
      {
         return new Target<>(property, value -> value == null ? null : type.cast(value));
      }

      public static Target<String> text(Property<String> property)
      {
         return new Target<>(property, LovDefinition.ValueType::text);
      }

      private Assignment<T> prepare(Object value)
      {
         if( property.isBound() ) throw new IllegalStateException("Получатель LOV связан односторонним binding");
         return new Assignment<>(property, property.getValue(), converter.apply(value));
      }
   }

   private record Assignment<T>(Property<T> property, T previous, T value)
   {
      void apply() { property.setValue(value); }
      void restore() { property.setValue(previous); }
   }

   private final ForeLov lov;
   private final TextField field;
   private final Map<String, Target<?>> targets;
   private final Supplier<Map<String, Object>> parameters;
   private final Runnable nextFocus;
   private final ReadOnlyObjectWrapper<State> state = new ReadOnlyObjectWrapper<>(State.UNCHECKED);
   private final ReadOnlyObjectWrapper<Throwable> error = new ReadOnlyObjectWrapper<>();
   private final EventHandler<KeyEvent> keyHandler;
   private final ChangeListener<Boolean> focusListener;
   private final ChangeListener<String> textListener;
   private boolean opening;
   private boolean applying;
   private boolean closed;
   private long revision;
   private CompletableFuture<Boolean> operation;

   public LovBinding(ForeLov lov, TextField field, Map<String, Target<?>> targets)
   {
      this(lov, field, targets, Map::of, null);
   }

   /** nextFocus обязателен при auto-skip; параметры вычисляются заново при каждом вызове. */
   public LovBinding(ForeLov lov, TextField field, Map<String, Target<?>> targets,
                     Supplier<Map<String, Object>> parameters, Runnable nextFocus)
   {
      FormTools.requireFxThread();
      this.lov = Objects.requireNonNull(lov);
      this.field = Objects.requireNonNull(field);
      this.targets = Map.copyOf(targets);
      this.parameters = Objects.requireNonNull(parameters);
      this.nextFocus = nextFocus;
      if( lov.getDefinition().behavior().autoSkip() && nextFocus == null )
         throw new IllegalArgumentException("Для auto-skip требуется действие nextFocus");
      var properties = Collections.newSetFromMap(new IdentityHashMap<Property<?>, Boolean>());
      for( var column : lov.getDefinition().columns() )
      {
         if( column.returnTo().isEmpty() ) continue;
         Target<?> target = this.targets.get(column.returnTo());
         if( target == null ) throw new IllegalArgumentException("Не задан получатель LOV: " + column.returnTo());
         if( !properties.add(target.property()) )
            throw new IllegalArgumentException("Одно свойство нельзя назначить нескольким колонкам LOV");
      }
      if( lov.getDefinition().behavior().validateFromList()
            && this.targets.get(lov.getDefinition().searchColumn().returnTo()).property() != field.textProperty() )
         throw new IllegalArgumentException("Первая видимая колонка должна возвращаться в проверяемое поле");
      KeyCombination key = KeyCombination.valueOf(lov.getDefinition().behavior().key());
      keyHandler = event -> {
         if( key.match(event) ) { show(); event.consume(); }
      };
      textListener = (observable, oldValue, value) -> {
         if( applying ) return;
         revision++;
         state.set(State.UNCHECKED);
         error.set(null);
         if( !opening ) lov.cancelQuery();
      };
      focusListener = (observable, oldValue, focused) -> {
         if( closed || opening || applying ) return;
         if( focused && lov.getDefinition().behavior().autoDisplay() ) show();
         else if( !focused && lov.getDefinition().behavior().validateFromList()
               && state.get() == State.UNCHECKED && field.getScene() != null
               && field.getScene().getWindow() != null && field.getScene().getWindow().isShowing() )
            validate();
      };
      field.addEventFilter(KeyEvent.KEY_PRESSED, keyHandler);
      field.textProperty().addListener(textListener);
      field.focusedProperty().addListener(focusListener);
   }

   public State getState() { return state.get(); }
   public ReadOnlyObjectProperty<State> stateProperty() { return state.getReadOnlyProperty(); }
   public Throwable getError() { return error.get(); }
   public ReadOnlyObjectProperty<Throwable> errorProperty() { return error.getReadOnlyProperty(); }

   public CompletableFuture<Boolean> show()
   {
      checkOpen();
      if( operation != null && !operation.isDone() ) return operation;
      return begin(false);
   }

   /**
    * Асинхронный барьер перед сохранением формы. Потеря фокуса не может остановить навигацию JavaFX.
    * Пустое необязательное поле очищает получателей; обязательность проверяет валидатор формы.
    */
   public CompletableFuture<Boolean> validate()
   {
      checkOpen();
      if( operation != null && !operation.isDone() ) return operation;
      return begin(true);
   }

   private CompletableFuture<Boolean> begin(boolean validation)
   {
      long ticket = revision;
      var result = new CompletableFuture<Boolean>();
      operation = result;
      state.set(State.VALIDATING);
      error.set(null);
      try
      {
         Map<String, Object> values = new LovDataSource.Request(lov.getDefinition(), "",
               lov.getDefinition().search().mode(), parameters.get()).parameters();
         if( validation && field.getText().isEmpty() )
         {
            var empty = new LinkedHashMap<String, Object>();
            lov.getDefinition().columns().stream().filter(column -> !column.returnTo().isEmpty())
                  .forEach(column -> empty.put(column.returnTo(), null));
            apply(empty);
            finish(result, ticket, true, null);
         }
         else if( validation )
            lov.query(field.getText(), LovDefinition.MatchMode.EXACT, values, false).whenComplete((loaded, failure) -> {
               if( !current(ticket) ) { result.complete(false); return; }
               if( failure != null ) { finish(result, ticket, false, failure); return; }
               if( loaded.complete() && loaded.rows().size() == 1 )
                  accept(loaded.rows().getFirst(), values, result, ticket, false);
               else choose(values, result, ticket);
            });
         else choose(values, result, ticket);
      }
      catch( RuntimeException ex ) { finish(result, ticket, false, ex); }
      return result;
   }

   private void choose(Map<String, Object> values, CompletableFuture<Boolean> result, long ticket)
   {
      opening = true;
      try
      {
         lov.show(field, values, field.getText()).whenComplete((selected, failure) -> {
            // Фокус владельца восстанавливается при закрытии диалога; это не новый вход в LOV.
            Platform.runLater(() -> opening = false);
            if( failure != null ) { finish(result, ticket, false, failure); return; }
            if( selected.isPresent() && current(ticket) ) accept(selected.get(), values, result, ticket, true);
            else finish(result, ticket, false, null);
         });
      }
      catch( RuntimeException ex )
      {
         opening = false;
         finish(result, ticket, false, ex);
      }
   }

   private void accept(LovRow row, Map<String, Object> queryParameters,
                       CompletableFuture<Boolean> result, long ticket, boolean selected)
   {
      try
      {
         if( !queryParameters.equals(parameters.get()) )
            throw new IllegalStateException("Параметры LOV изменились во время выбора; повторите проверку");
         apply(row.returns(lov.getDefinition()));
         finish(result, ticket, true, null);
         if( selected && lov.getDefinition().behavior().autoSkip() ) nextFocus.run();
      }
      catch( RuntimeException ex ) { finish(result, ticket, false, ex); }
   }

   private void apply(Map<String, Object> values)
   {
      var changes = new ArrayList<Assignment<?>>();
      values.forEach((name, value) -> changes.add(targets.get(name).prepare(value)));
      applying = true;
      try
      {
         for( var change : changes ) change.apply();
      }
      catch( RuntimeException ex )
      {
         for( var change : changes.reversed() )
            try { change.restore(); }
            catch( RuntimeException rollback ) { ex.addSuppressed(rollback); }
         throw ex;
      }
      finally { applying = false; }
   }

   private boolean current(long ticket) { return !closed && ticket == revision; }

   private void finish(CompletableFuture<Boolean> result, long ticket, boolean valid, Throwable failure)
   {
      if( current(ticket) )
      {
         state.set(valid ? State.VALID : State.INVALID);
         error.set(failure);
      }
      result.complete(current(ticket) && valid);
   }

   private void checkOpen()
   {
      FormTools.requireFxThread();
      if( closed ) throw new IllegalStateException("Привязка LOV закрыта");
   }

   @Override public void close()
   {
      FormTools.requireFxThread();
      if( closed ) return;
      closed = true;
      revision++;
      field.removeEventFilter(KeyEvent.KEY_PRESSED, keyHandler);
      field.textProperty().removeListener(textListener);
      field.focusedProperty().removeListener(focusListener);
      lov.close();
      if( operation != null ) operation.complete(false);
   }
}
