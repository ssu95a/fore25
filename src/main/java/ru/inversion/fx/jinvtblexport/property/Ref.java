/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ru.inversion.fx.jinvtblexport.property;

/**
 *
 * @author polyatykina
 */
public class Ref<T>{
          
       private T val;
       public void set(T value){
           val = value;
       }
       public Ref(T value){
           val = value;
       }
       public T get(){
           return val;
       }
}
