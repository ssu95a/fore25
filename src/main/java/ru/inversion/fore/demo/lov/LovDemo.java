package ru.inversion.fore.demo.lov;

import javafx.application.Application;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import ru.inversion.fore.form.lov.LovBinding;
import ru.inversion.fore.form.lov.LovSources;
import ru.inversion.fore.form.lov.LxmlLoader;

import java.util.Map;

/** Демонстрация декларативного LOV, скрытого идентификатора и проверки перед сохранением. */
public final class LovDemo extends Application
{
   @Override public void start(Stage stage) throws Exception
   {
      var id = new SimpleObjectProperty<Long>();
      var code = new TextField();
      code.setPromptText("Код подразделения; F9 — справочник");
      var name = new TextField();
      name.setEditable(false);
      name.setFocusTraversable(false);
      var identity = new Label();
      identity.textProperty().bind(id.asString("Идентификатор: %s"));
      var lov = new LxmlLoader().load(LovDemo.class.getResource("departments.lxml"), new LovSources());
      var binding = new LovBinding(lov, code, Map.of(
            "departmentId", LovBinding.Target.of(id, Long.class),
            "departmentCode", LovBinding.Target.text(code.textProperty()),
            "departmentName", LovBinding.Target.text(name.textProperty())));
      var choose = new Button("…");
      choose.setAccessibleText("Выбрать подразделение");
      choose.setFocusTraversable(false);
      choose.setOnAction(event -> binding.show());
      var save = new Button("Проверить перед сохранением");
      var status = new Label("Введите БУХ или откройте список клавишей F9.");
      status.setWrapText(true);
      save.setOnAction(event -> {
         save.setDisable(true);
         binding.validate().whenComplete((valid, failure) -> {
            save.setDisable(false);
            status.setText(Boolean.TRUE.equals(valid) ? "Проверка пройдена; значения готовы к сохранению." :
                  "Выбор не подтверждён. Сохранение не выполнено.");
         });
      });
      binding.errorProperty().addListener((observable, oldValue, failure) -> {
         if( failure != null ) status.setText("Ошибка проверки: " + failure.getMessage());
      });
      var input = new HBox(8, code, choose);
      HBox.setHgrow(code, Priority.ALWAYS);
      var content = new VBox(12, new Label("Подразделение"), input, name, identity, save, status);
      content.setPadding(new Insets(24));
      stage.setTitle("Fore LOV — прототип LXML");
      stage.setScene(new Scene(content, 620, 300));
      stage.setOnHidden(event -> binding.close());
      stage.show();
   }

   public static void main(String[] args) { launch(args); }
}
