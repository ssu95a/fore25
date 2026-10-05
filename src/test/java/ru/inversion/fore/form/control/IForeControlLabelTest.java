package ru.inversion.fore.form.control;

import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Label;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class IForeControlLabelTest
{
   @BeforeAll
   static void startToolkit() throws Exception
   {
      final CountDownLatch started = new CountDownLatch(1);
      try
      {
         Platform.startup(started::countDown);
      }
      catch( IllegalStateException alreadyStarted )
      {
         Platform.runLater(started::countDown);
      }
      assertTrue(started.await(10, TimeUnit.SECONDS), "JavaFX toolkit did not start");
   }

   @Test
   void labelCanBeReplacedAndCleared() throws Exception
   {
      onFxThread(() -> {
         final ForeTextField control = new ForeTextField();
         final Label first = new Label("First");
         final Label second = new Label("Second");

         assertNull(control.getLabel());
         control.setLabel(first);
         assertSame(first, control.getLabel());
         assertSame(control, first.getLabelFor());

         control.setLabel(second);
         assertNull(first.getLabelFor());
         assertSame(second, control.getLabel());
         assertSame(control, second.getLabelFor());

         control.setLabel(second);
         assertSame(second, control.getLabel());
         control.setLabel(null);
         assertNull(second.getLabelFor());
         assertNull(control.getLabel());
         control.setLabel(null);
         assertNull(control.getLabel());
      });
   }

   @Test
   void externalLabelForAssociationCanBeDiscoveredAndReplaced() throws Exception
   {
      onFxThread(() -> {
         final ForeTextField control = new ForeTextField();
         final Label external = new Label("External");
         final Label replacement = new Label("Replacement");
         external.setLabelFor(control);

         assertSame(external, control.getLabel());
         control.setLabel(replacement);
         assertNull(external.getLabelFor());
         assertSame(replacement, control.getLabel());

         replacement.setLabelFor(null);
         assertNull(control.getLabel());
      });
   }

   @Test
   void movingLabelBetweenForeControlsDoesNotDetachItsNewTarget() throws Exception
   {
      onFxThread(() -> {
         final ForeTextField first = new ForeTextField();
         final ForeTextField second = new ForeTextField();
         final Label label = new Label("Shared");
         first.setLabel(label);
         second.setLabel(label);

         assertNull(first.getLabel());
         assertSame(label, second.getLabel());
         first.setLabel(null);
         assertSame(second, label.getLabelFor());
         assertSame(label, second.getLabel());
      });
   }

   @Test
   void externalRelocationIsRespectedWhenClearingOldControl() throws Exception
   {
      onFxThread(() -> {
         final ForeTextField first = new ForeTextField();
         final ForeTextField second = new ForeTextField();
         final Label label = new Label("Moved");
         first.setLabel(label);
         label.setLabelFor(second);

         assertNull(first.getLabel());
         first.setLabel(null);
         assertSame(second, label.getLabelFor());
         assertSame(label, second.getLabel());
      });
   }

   @Test
   void boundReplacementDoesNotDetachPreviousLabel() throws Exception
   {
      onFxThread(() -> {
         final ForeTextField control = new ForeTextField();
         final ForeTextField other = new ForeTextField();
         final Label previous = new Label("Previous");
         final Label replacement = new Label("Bound");
         final SimpleObjectProperty<Node> target = new SimpleObjectProperty<>(other);
         control.setLabel(previous);
         replacement.labelForProperty().bind(target);

         assertThrows(RuntimeException.class, () -> control.setLabel(replacement));
         assertSame(previous, control.getLabel());
         assertSame(control, previous.getLabelFor());
         assertSame(other, replacement.getLabelFor());
         assertTrue(replacement.labelForProperty().isBound());
      });
   }

   @Test
   void boundCurrentLabelRejectsChangesButAllowsIdempotentSetter() throws Exception
   {
      onFxThread(() -> {
         final ForeTextField control = new ForeTextField();
         final Label current = new Label("Bound current");
         final Label replacement = new Label("Replacement");
         final SimpleObjectProperty<Node> target = new SimpleObjectProperty<>(control);
         current.labelForProperty().bind(target);

         assertSame(current, control.getLabel());
         control.setLabel(current);
         assertThrows(IllegalStateException.class, () -> control.setLabel(null));
         assertThrows(IllegalStateException.class, () -> control.setLabel(replacement));
         assertSame(current, control.getLabel());
         assertSame(control, current.getLabelFor());
         assertTrue(current.labelForProperty().isBound());
         assertNull(replacement.getLabelFor());
      });
   }

   @Test
   void fxmlLoadsInheritedMetadataProperties() throws Exception
   {
      onFxThread(() -> {
         final FXMLLoader loader = load("""
                 <?import javafx.scene.control.Label?>
                 <?import javafx.scene.layout.VBox?>
                 <?import ru.inversion.fore.form.control.ForeTextField?>
                 <VBox xmlns:fx="http://javafx.com/fxml/1">
                    <Label fx:id="caption" text="Employee"/>
                    <ForeTextField fx:id="control" fieldName="  EMP_NAME  " label="$caption"/>
                 </VBox>
                 """);
         final ForeTextField control = (ForeTextField) loader.getNamespace().get("control");
         final Label caption = (Label) loader.getNamespace().get("caption");

         assertEquals("  EMP_NAME  ", control.getFieldName());
         assertEquals("  EMP_NAME  ", control.getProperties().get("ru.inversion.field_name"));
         assertSame(caption, control.getLabel());
         assertSame(control, caption.getLabelFor());
      });
   }

   @Test
   void fxmlNativeLabelForAssociationIsVisibleToContract() throws Exception
   {
      onFxThread(() -> {
         final FXMLLoader loader = load("""
                 <?import javafx.scene.control.Label?>
                 <?import javafx.scene.layout.VBox?>
                 <?import ru.inversion.fore.form.control.ForeTextField?>
                 <VBox xmlns:fx="http://javafx.com/fxml/1">
                    <ForeTextField fx:id="control"/>
                    <Label fx:id="caption" labelFor="$control" text="Employee"/>
                 </VBox>
                 """);
         final ForeTextField control = (ForeTextField) loader.getNamespace().get("control");
         final Label caption = (Label) loader.getNamespace().get("caption");

         assertSame(caption, control.getLabel());
         control.setLabel(null);
         assertNull(caption.getLabelFor());
         assertNull(control.getLabel());
      });
   }

   @Test
   void fxmlLoadsTextFieldMetadataAndKeepsTextVerbatim() throws Exception
   {
      onFxThread(() -> {
         final FXMLLoader loader = load("""
                 <?import javafx.scene.control.Label?>
                 <?import javafx.scene.layout.VBox?>
                 <?import ru.inversion.fore.form.control.ForeTextField?>
                 <VBox xmlns:fx="http://javafx.com/fxml/1">
                    <Label fx:id="caption" text="Employee"/>
                    <ForeTextField fx:id="control" fieldName="  EMP_NAME  " label="$caption" text="  Alice  "/>
                 </VBox>
                 """);
         final ForeTextField control = (ForeTextField) loader.getNamespace().get("control");
         final Label caption = (Label) loader.getNamespace().get("caption");

         assertEquals("  EMP_NAME  ", control.getFieldName());
         assertEquals("  Alice  ", control.getText());
         assertSame(caption, control.getLabel());
         assertSame(control, caption.getLabelFor());
      });
   }

   @Test
   void controlsFxExtractsTextFieldValueWithoutCustomRegistration() throws Exception
   {
      onFxThread(() -> {
         final ForeTextField control = new ForeTextField("  Alice  ");
         final var observable = org.controlsfx.tools.ValueExtractor
                 .getObservableValueExtractor(control).orElseThrow().call(control);

         assertSame(control.textProperty(), observable);
         assertEquals("  Alice  ", observable.getValue());
         control.setText("  Bob  ");
         assertEquals("  Bob  ", observable.getValue());
      });
   }

   private static FXMLLoader load(String fxml) throws Exception
   {
      final FXMLLoader loader = new FXMLLoader();
      loader.load(new ByteArrayInputStream(fxml.getBytes(StandardCharsets.UTF_8)));
      return loader;
   }

   // Все операции с Control и FXMLLoader выполняются в FX Application Thread.
   private static void onFxThread(FxTask task) throws Exception
   {
      final FutureTask<Void> future = new FutureTask<>(() -> {
         task.run();
         return null;
      });
      Platform.runLater(future);
      future.get(10, TimeUnit.SECONDS);
   }

   @FunctionalInterface
   private interface FxTask
   {
      void run() throws Exception;
   }
}
