/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ru.inversion.fx.form.controls.skin;

import javafx.scene.Node;
import javafx.scene.control.skin.TableHeaderRow;
import javafx.scene.control.skin.TableViewSkinBase;
import javafx.scene.shape.Rectangle;

/**
 *
 * @author perov
 */
public class JInvTableHeaderRow extends TableHeaderRow {

    private Node corner;

    public JInvTableHeaderRow(final TableViewSkinBase<?, ?, ?, ?, ?> skin) {
        super(skin);
    }

    @Override
    protected void layoutChildren() {
        super.layoutChildren();
        updateClipWidth();

        if (corner == null)
            corner = lookup(".show-hide-columns-button");

        if (corner != null) {
            // Ширину кнопки уже вычислил JavaFX с учётом вертикального scrollbar.
            double cornerWidth = corner.getLayoutBounds().getWidth();
            double height = getHeight() - snappedTopInset() - snappedBottomInset();
            corner.resizeRelocate(Math.max(0, getWidth() - cornerWidth),
                    snappedTopInset(), cornerWidth, height);
        }
    }

    @Override
    protected void updateTableWidth() {
        super.updateTableWidth();
        updateClipWidth();
    }

    private void updateClipWidth() {
        // Skin уже выделил заголовку ширину без панели фильтра.
        if (getClip() instanceof Rectangle clip)
            clip.setWidth(Math.max(0, getWidth()));
    }
}
