/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ru.inversion.utils.scheck;

/**
 *
 * @author antonovdi
 */
public class JInvStringWorkerException extends Exception {

    public JInvStringWorkerException(String message, Throwable cause) {
        super(message, cause);
    }

    public JInvStringWorkerException(String desc) {
        super(desc);
    }
}
