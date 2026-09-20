/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ru.inversion.dataset.fx;

import java.util.Objects;

/**
 * Класс группы для {@link F7FilterItem}
 *
 * @author antonovdi
 */
public class F7FilterGroup implements Comparable<F7FilterGroup> {

    public F7FilterGroup(String id, String title) {
        this.id = id;
        this.title = title;
    }

    public F7FilterGroup(String id, String title, int order) {
        this.id = id;
        this.title = title;
        this.order = order;
    }

    public F7FilterGroup(String id, String title, int order, boolean collapsed) {
        this.id = id;
        this.title = title;
        this.order = order;
        this.collapsed = collapsed;
    }

    /**
     * Идентификатор группы
     */
    private String id;

    /**
     * Название группы
     */
    private String title;
    /**
     * Порядковый номер группы
     */
    private int order;

    /**
     * Признак свернутости группы
     */
    private boolean collapsed;

    /**
     */
    public boolean isCollapsed() {
        return collapsed;
    }

    /**
     */
    public void setCollapsed(boolean collapsed) {
        this.collapsed = collapsed;
    }

    /**
     */
    public int getOrder() {
        return order;
    }

    /**
     */
    public void setOrder(int order) {
        this.order = order;
    }

    /**
     */
    public String getTitle() {
        return title;
    }

    /**
     */
    public void setTitle(String title) {
        this.title = title;
    }

    /**
     */
    public String getId() {
        return id;
    }

    /**
     */
    public void setId(String id) {
        this.id = id;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 37 * hash + Objects.hashCode(this.id);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final F7FilterGroup other = (F7FilterGroup) obj;
        if (!Objects.equals(this.id, other.id)) {
            return false;
        }
        return true;
    }

    @Override
    public int compareTo(F7FilterGroup o) {

        int result = Integer.compare(getOrder(), o.getOrder());
        if (result == 0) {
            return getId().compareToIgnoreCase(o.getId());
        } else {
            return result;
        }
    }

}
