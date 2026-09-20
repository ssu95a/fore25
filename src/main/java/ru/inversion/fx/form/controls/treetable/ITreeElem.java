/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ru.inversion.fx.form.controls.treetable;

/**
 * Интерфейс для работы с JInvTreeTable. 
 * Entity должен реализвывать данный интерфейс.
 * Методы пометить анотацией @Transient
 * @author perov
 */
public interface ITreeElem<T extends Comparable<T>> {
  T getParentId();
  T getId();
}
