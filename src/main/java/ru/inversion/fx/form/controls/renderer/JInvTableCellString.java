/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ru.inversion.fx.form.controls.renderer;

/**
 *
 * @author antonovdi
 */
public class JInvTableCellString extends JInvTableCell{
    
    public JInvTableCellString(String mask) {
        super(mask);
    }

    @Override
    protected void updateItem(Object item, boolean empty) {
        super.updateItem(item, empty); //To change body of generated methods, choose Tools | Templates.
        applyRenderer(item, empty);
    }
}
