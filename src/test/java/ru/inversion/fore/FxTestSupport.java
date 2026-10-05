package ru.inversion.fore;

import javafx.application.Platform;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertTrue;

/** Shared FX-thread support for headless tests and Stage lifecycle checks. */
public final class FxTestSupport
{
   private FxTestSupport() {}

   public static void start() throws Exception
   {
      final CountDownLatch started = new CountDownLatch(1);
      final Runnable ready = () -> {
         Platform.setImplicitExit(false);
         started.countDown();
      };
      try
      {
         Platform.startup(ready);
      }
      catch( IllegalStateException alreadyStarted )
      {
         Platform.runLater(ready);
      }
      assertTrue(started.await(10, TimeUnit.SECONDS), "JavaFX toolkit did not start");
   }

   public static void run(Task task) throws Exception
   {
      if( Platform.isFxApplicationThread() )
      {
         task.run();
         return;
      }
      final FutureTask<Void> future = new FutureTask<>(() -> {
         task.run();
         return null;
      });
      Platform.runLater(future);
      try
      {
         future.get(10, TimeUnit.SECONDS);
      }
      catch( ExecutionException ex )
      {
         if( ex.getCause() instanceof Exception cause )
            throw cause;
         if( ex.getCause() instanceof Error cause )
            throw cause;
         throw ex;
      }
   }

   @FunctionalInterface
   public interface Task
   {
      void run() throws Exception;
   }
}
