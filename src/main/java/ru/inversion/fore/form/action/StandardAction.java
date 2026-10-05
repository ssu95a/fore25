package ru.inversion.fore.form.action;

/**
 * Semantics of a standard Fore operation, not the identity of an action instance.
 * Two controls/forms may have different actions with the same standard type.
 */
public enum StandardAction
{
   CREATE,
   UPDATE,
   DELETE,
   VIEW,

   REFRESH,

   IMPORT,
   EXPORT,

   PRINT,

   STATUS
}
