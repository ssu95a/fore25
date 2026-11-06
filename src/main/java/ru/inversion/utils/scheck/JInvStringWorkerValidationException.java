/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ru.inversion.utils.scheck;

public class JInvStringWorkerValidationException extends Exception {

    public JInvStringWorkerValidationException(String message, Throwable cause) {
        super(message, cause);
    }

    public JInvStringWorkerValidationException(String desc) {
        super(desc);
    }
}