/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ru.inversion.fx.help.store;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.List;

/**
 *
 * @author perov
 */
@XmlRootElement(name = "controllers")
public class PHelpWrapper {
    
    private List<AdapterHelp> controllers;
    
    @XmlElement(name = "controller")
    public List<AdapterHelp> getControllers() {
        return controllers;
    }

    public void setControllers(List<AdapterHelp> controllers) {
        this.controllers = controllers;
    }
    
}
