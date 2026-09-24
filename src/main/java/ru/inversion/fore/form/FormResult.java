package ru.inversion.fore.form;

import javafx.stage.Window;

/** */
public record FormResult<T>(
  FormResultType result,
  T dataObject,
  Window window
)
{}