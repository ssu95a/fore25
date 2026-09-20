/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ru.inversion.fx.service.module;

/**
 *
 * @author antonovdi
 */
public class ModuleContext {

    /**
     * Имя jar файла
     */
    String jarName;

    /**
     * Версия модуля. Implementation-version
     */
    String fullVersion;

    /**
     * Версия модуля менеджеров. Последняя часть в fullVersion.
     */
    String managerVersion;

    /**
     * Версия ядра c которым был собран модуль
     */
    String coreVersion;

    /**
     * Флажок была ли проверка версии модуля в базе данных 
     */
    boolean checkedDb;

    public boolean isCheckedDb() {
        return checkedDb;
    }

    public void setCheckedDb(boolean checkedDb) {
        this.checkedDb = checkedDb;
    }
    
    
    public String getJarName() {
        return jarName;
    }

    public void setJarName(String jarName) {
        this.jarName = jarName;
    }

    public String getFullVersion() {
        return fullVersion;
    }

    public void setFullVersion(String fullVersion) {
        this.fullVersion = fullVersion;
    }

    public String getManagerVersion() {
        return managerVersion;
    }

    public void setManagerVersion(String managerVersion) {
        this.managerVersion = managerVersion;
    }

    public String getCoreVersion() {
        return coreVersion;
    }

    public void setCoreVersion(String coreVersion) {
        this.coreVersion = coreVersion;
    }

}
