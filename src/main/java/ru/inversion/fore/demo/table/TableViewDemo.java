package ru.inversion.fore.demo.table;

import javafx.application.Application;
import javafx.stage.Stage;
import ru.inversion.fore.form.FormLauncher;

/** Точка запуска примера из IDE; отдельный основной класс поддерживает запуск JavaFX из classpath. */
public final class TableViewDemo
{
   private TableViewDemo() {}

   public static void main(String[] args)
   {
      Application.launch(DemoApplication.class, args);
   }

   public static final class DemoApplication extends Application
   {
      @Override
      public void start(Stage primaryStage)
      {
         new FormLauncher<RecordStore, RecordTableController>(null, null, RecordTableController.class)
                 .fxml("ru/inversion/fore/demo/table/record-table.fxml")
                 .bundle("ru.inversion.fore.demo.table.messages")
                 .dataObject(RecordStore.sample())
                 .runForm();
      }
   }
}
