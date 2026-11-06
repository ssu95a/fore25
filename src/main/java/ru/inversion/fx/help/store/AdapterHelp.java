/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ru.inversion.fx.help.store;

import ru.inversion.fx.help.entity.PHelp;
import ru.inversion.fx.help.entity.PHelpBundle;

import javax.xml.bind.annotation.XmlElement;
import java.util.List;

/**
 *
 * @author perov
 */
public class AdapterHelp {
    private PHelp pHelp;
    private List<PHelpBundle> components;

    public AdapterHelp( final PHelp pHelp ) {
        this.pHelp = pHelp;
    }
    public AdapterHelp() {}

    public PHelp getpHelp() {
        return pHelp;
    }

    public void setpHelp(PHelp pHelp) {
        this.pHelp = pHelp;
    }
    @XmlElement(name = "component")
    public List<PHelpBundle> getComponents() {
        return components;
    }

    public void setComponents(List<PHelpBundle> components) {
        this.components = components;
    }

}
