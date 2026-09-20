/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ru.inversion.fx.app.service;

import ru.inversion.fx.app.AppException;
import ru.inversion.utils.S;

import java.util.*;
import java.util.stream.Collectors;

/**
 *
 * @author antonovdi
 */
public class ViewPrefFormService implements IViewPrefSaver {

    private Collection<PPrefComponent> loadPrefList = new ArrayList<>();
    private Set<PPrefComponent> savePrefSet = new HashSet<>();

    @Override
    public void addViewPref(PPrefComponent entry) {

        if(!savePrefSet.add(entry)) {
            savePrefSet.remove(entry);
            savePrefSet.add(entry);
        }
    }

    @Override
    public void save() throws AppException {
        ViewPrefAppService.saveDimensions( new ArrayList(savePrefSet) );
    }

    @Override
    public Collection<PPrefComponent> getInitialPrefs() {
        return loadPrefList;
    }

    public void setInitialPrefs(Collection<PPrefComponent> prefs) {
        this.loadPrefList = prefs;
    }

    /** */
    public Iterator<PPrefComponent> getInitialPrefs( String componentFor ) {

        if( S.isNullOrEmpty(componentFor) )
            return Collections.emptyIterator();

        return loadPrefList.stream( ).filter( c->( componentFor.equals( c.getCOMPONENT() ) && !S.isNullOrEmpty( c.getELEMENT() ) ) ).iterator();
    }

    @Override
    public Iterator<PPrefComponent> getInitialPrefs( String componentFor, String elementFor ) {

        if( S.isNullOrEmpty(elementFor) )
            return getInitialPrefs(componentFor);

        return loadPrefList.stream( ).filter( c->( componentFor.equals( c.getCOMPONENT() ) && elementFor.equals ( c.getELEMENT() ) ) ).iterator();
    }

    /** */
    @Override
    public void addAll(Collection<PPrefComponent> list) {
        if (list != null) {
            list.forEach(this::addViewPref);
        }
    }
}
