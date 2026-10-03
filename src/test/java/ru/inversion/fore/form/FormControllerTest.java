package ru.inversion.fore.form;

import javafx.stage.Window;
import org.junit.jupiter.api.Test;
import ru.inversion.fore.form.validation.ValidationResult;
import ru.inversion.tc.TaskContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Controller decision/lifecycle tests without a Stage or FX event loop.
 * Graphical decoration and actual Stage close events require an FX smoke test.
 */
class FormControllerTest
{
   @Test
   void preInitPrecedesFxmlInitialize() throws Exception
   {
      var controller = new ProbeController();
      prepare(controller, FormMode.DEFAULT, new ArrayList<>(), new ArrayList<>());
      assertEquals(List.of("preInit"), controller.lifecycle);

      controller.initialize(null, null);
      assertEquals(List.of("preInit", "init"), controller.lifecycle);
   }

   @Test
   void initializationExceptionIsWrapped() throws Exception
   {
      var controller = new ProbeController();
      controller.failInit = true;
      prepare(controller, FormMode.DEFAULT, new ArrayList<>(), new ArrayList<>());

      var error = assertThrows(FormException.class, () -> controller.initialize(null, null));
      assertInstanceOf(IllegalStateException.class, error.getCause());
   }

   @Test
   void untargetedFormFailureIsShownAndPreventsOk() throws Exception
   {
      var controller = new ProbeController();
      prepare(controller, FormMode.EDIT, new ArrayList<>(), new ArrayList<>());
      controller.validation().add(() -> ValidationResult.failure("End date must be after start date"));

      assertFalse(controller.processCloseRequest(FormResultType.OK));
      assertEquals(List.of("End date must be after start date"), controller.messages);
      assertEquals(0, controller.okCalls);
   }

   @Test
   void okValidatesAgainAndCallbackReceivesAcceptedResultOnce() throws Exception
   {
      var controller = new ProbeController();
      var results = new ArrayList<FormResult<String>>();
      prepare(controller, FormMode.DEFAULT, results, new ArrayList<>());
      var invalid = new boolean[]{true};
      controller.validation().add(() -> invalid[0]
              ? ValidationResult.failure("Fix the dates")
              : ValidationResult.ok());

      assertFalse(controller.processCloseRequest(FormResultType.OK));
      invalid[0] = false;
      assertTrue(controller.processCloseRequest(FormResultType.OK));
      assertEquals(1, controller.okCalls);

      controller.completeController();
      controller.completeController();
      assertEquals(1, results.size());
      assertEquals(FormResultType.OK, results.getFirst().result());
      assertEquals("record", results.getFirst().dataObject());
   }

   @Test
   void rejectedOnOkDoesNotChangeResultToOk() throws Exception
   {
      var controller = new ProbeController();
      var results = new ArrayList<FormResult<String>>();
      prepare(controller, FormMode.DEFAULT, results, new ArrayList<>());
      controller.okAllowed = false;

      assertFalse(controller.processCloseRequest(FormResultType.OK));
      assertEquals(1, controller.okCalls);
      controller.completeController();
      assertEquals(FormResultType.CANCEL, results.getFirst().result());
   }

   @Test
   void cancelSkipsValidationAndHonorsVeto() throws Exception
   {
      var controller = new ProbeController();
      prepare(controller, FormMode.DEFAULT, new ArrayList<>(), new ArrayList<>());
      var validationCalls = new AtomicInteger();
      controller.validation().add(() -> {
         validationCalls.incrementAndGet();
         return ValidationResult.failure("Must not be called on CANCEL");
      });
      controller.cancelAllowed = false;

      assertFalse(controller.processCloseRequest(FormResultType.CANCEL));
      controller.cancelAllowed = true;
      assertTrue(controller.processCloseRequest(FormResultType.CANCEL));
      assertEquals(0, validationCalls.get());
      assertEquals(2, controller.cancelCalls);
      assertTrue(controller.messages.isEmpty());
   }

   @Test
   void nonEditingModesSkipOkValidation() throws Exception
   {
      for( var mode : List.of(FormMode.VIEW, FormMode.DELETE) )
      {
         var controller = new ProbeController();
         prepare(controller, mode, new ArrayList<>(), new ArrayList<>());
         var validationCalls = new AtomicInteger();
         controller.validation().add(() -> {
            validationCalls.incrementAndGet();
            return ValidationResult.failure("Must not be called");
         });

         assertTrue(controller.processCloseRequest(FormResultType.OK), mode.name());
         assertEquals(0, validationCalls.get(), mode.name());
         assertEquals(1, controller.okCalls, mode.name());
      }
   }

   @Test
   void interactiveErrorGoesToLauncherSinkWithoutRethrowingInListener() throws Exception
   {
      var controller = new ProbeController();
      var errors = new ArrayList<Throwable>();
      prepare(controller, FormMode.DEFAULT, new ArrayList<>(), errors);
      var cause = new Exception("Value validator failed");

      assertDoesNotThrow(() -> controller.handleInteractiveValidationError(cause));
      assertEquals(1, errors.size());
      assertInstanceOf(FormException.class, errors.getFirst());
      assertSame(cause, errors.getFirst().getCause());
   }

   @Test
   void resourcesAreReleasedOnlyOnce() throws Exception
   {
      var controller = new ProbeController();
      prepare(controller, FormMode.DEFAULT, new ArrayList<>(), new ArrayList<>());

      controller.releaseController();
      controller.releaseController();
      assertEquals(1, controller.releaseCalls);
   }

   private static void prepare(
           ProbeController controller,
           FormMode mode,
           List<FormResult<String>> results,
           List<Throwable> errors ) throws Exception
   {
      assertTrue(controller.preInitController(new StubContext(mode), results::add, errors::add));
   }

   private static final class ProbeController extends FormController<String>
   {
      final List<String> lifecycle = new ArrayList<>();
      final List<String> messages = new ArrayList<>();
      boolean failInit;
      boolean okAllowed = true;
      boolean cancelAllowed = true;
      int okCalls;
      int cancelCalls;
      int releaseCalls;

      @Override protected boolean preInit()
      {
         lifecycle.add("preInit");
         return true;
      }

      @Override protected void init()
      {
         lifecycle.add("init");
         if( failInit )
            throw new IllegalStateException("Init failed");
      }

      @Override protected void showValidationMessage(String message)
      {
         messages.add(message);
      }

      @Override protected boolean onOK()
      {
         okCalls++;
         return okAllowed;
      }

      @Override protected boolean onCancel()
      {
         cancelCalls++;
         return cancelAllowed;
      }

      @Override protected void closeResources()
      {
         releaseCalls++;
      }
   }

   /** A context that does not create a JavaFX Window or a real TaskContext. */
   private record StubContext(FormMode mode) implements FormContext<String>
   {
      @Override public TaskContext taskContext() { return null; }
      @Override public Window owner() { return null; }
      @Override public String dataObject() { return "record"; }
      @Override public Map<String, Object> parameters() { return Map.of(); }
      @Override public ResourceBundle bundle() { return null; }
      @Override public FormController<?> parentController() { return null; }
      @Override public Window window() { return null; }
   }
}
