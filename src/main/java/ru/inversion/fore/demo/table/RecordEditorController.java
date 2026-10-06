package ru.inversion.fore.demo.table;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import ru.inversion.fore.form.FormController;
import ru.inversion.fore.form.FormMode;
import ru.inversion.fore.form.control.ForeTextField;
import ru.inversion.fore.form.validation.ValidationResult;
import ru.inversion.fore.form.validation.Validators;
import ru.inversion.fore.form.validation.ValueValidator;

import java.util.Objects;

/** Редактор изменяет черновик только после успешной валидации и подтверждения. */
public final class RecordEditorController extends FormController<RecordStore.Draft>
{
   @FXML private Label idValue;
   @FXML private ForeTextField nameField;
   @FXML private Button saveButton;
   @FXML private Button cancelButton;

   @Override
   protected void init()
   {
      final var draft = Objects.requireNonNull(getDataObject(), "draft");
      final String titleKey = switch( getFormMode() )
      {
         case INSERT -> "editor.insert.title";
         case EDIT -> "editor.edit.title";
         case VIEW -> "editor.view.title";
         default -> throw new IllegalArgumentException("Режим редактора не поддерживается: " + getFormMode());
      };
      setTitle(getBundle().getString(titleKey));
      idValue.setText(Long.toString(draft.id()));
      nameField.setText(draft.name());

      final boolean readOnly = getFormMode() == FormMode.VIEW;
      nameField.setEditable(!readOnly);
      saveButton.setVisible(!readOnly);
      saveButton.managedProperty().bind(saveButton.visibleProperty());
      cancelButton.setText(getBundle().getString(readOnly ? "editor.close" : "editor.cancel"));
      cancelButton.setDefaultButton(readOnly);

      final ValueValidator<String> required = value -> value == null || value.isBlank()
              ? ValidationResult.failure(getBundle().getString("editor.name.required"))
              : ValidationResult.ok();
      validation().add(nameField, required);
      validation().add(nameField, Validators.maxLength(120, getBundle().getString("editor.name.tooLong")));
   }

   @Override
   protected void guiInit()
   {
      Platform.runLater(nameField::requestFocus);
   }

   @FXML private void handleSave() { ok(); }
   @FXML private void handleCancel() { cancel(); }

   @Override
   protected boolean onOK()
   {
      if( getFormMode() == FormMode.VIEW )
         return false;

      getDataObject().setName(nameField.getText().strip());
      return true;
   }

   @Override
   protected void closeGuiResources()
   {
      if( saveButton != null ) saveButton.managedProperty().unbind();
   }
}
