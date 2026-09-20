package ru.inversion.xxi.impl;

import ru.inversion.fx.app.frame.menu.IMenuItemData;
import ru.inversion.utils.U;
import ru.inversion.utils.converter.TypeConverter;

/**
 * @author fomishkin
 * @since 25.04.2022
 */
class MenuItemData implements IMenuItemData {

    private final Integer ID;
    private final Integer groupNum, parentID, ordBY;
    private final String name, toolTip;
    private final String java_class, java_method, param;
    private final String mnbItem;

    /**
     *
     */
    MenuItemData(Object[] a) {
        int i = 0;
        ID = TypeConverter.convert(a[i++], Integer.class);
        name = (String) a[i++];
        toolTip = (String) a[i++];
        groupNum = TypeConverter.convert(a[i++], Integer.class);//a[i] == null ? null : ((Number)a[i]).intValue();
        //i++;
        ordBY = TypeConverter.convert(a[i++], Integer.class);//a[i] == null ? null : ((Number)a[i]).intValue();
        //i++;
        java_class = (String) a[i++];
        java_method = (String) a[i++];
        param = (String) a[i++];
        parentID = TypeConverter.convert(a[i++], Integer.class);//a[i] == null ? null : ((Number)a[i]).intValue();
        mnbItem = (String) a[i++];
    }

    /**
     *
     */
    @Override
    public Integer getID() {
        return ID;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Integer getGroup() {
        return U.nvl(groupNum, Integer.valueOf(0));
    }

    @Override
    public Integer getOrder() {
        return U.nvl(ordBY, Integer.valueOf(0));
    }

    @Override
    public String getTooltip() {
        return toolTip;
    }

    @Override
    public String getJavaClass() {
        return java_class;
    }

    @Override
    public String getJavaMethod() {
        return java_method;
    }

    @Override
    public String getParameters() {
        return param;
    }

    @Override
    public Integer getParentID() {
        return parentID;
    }

    @Override
    public String getMnbItem() {
        return mnbItem;
    }
}
