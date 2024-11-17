package com.web.watr.beans;

import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

@RequestScope
@Component
public class FilterBean {
    private String selectedDataset;
    private List<String> selectedSubjects;
    private List<String> selectedPredicates;
    private List<String> selectedObjects;

    public String getSelectedDataset() {
        return selectedDataset;
    }

    public void setSelectedDataset(String selectedDataset) {
        this.selectedDataset = selectedDataset;
    }

    public List<String> getSelectedSubjects() {
        return selectedSubjects;
    }

    public void setSelectedSubjects(List<String> selectedSubjects) {
        this.selectedSubjects = selectedSubjects;
    }

    public List<String> getSelectedPredicates() {
        return selectedPredicates;
    }

    public void setSelectedPredicates(List<String> selectedPredicates) {
        this.selectedPredicates = selectedPredicates;
    }

    public List<String> getSelectedObjects() {
        return selectedObjects;
    }

    public void setSelectedObjects(List<String> selectedObjects) {
        this.selectedObjects = selectedObjects;
    }
    public void addSubject(String subject){
        this.selectedSubjects.add(subject);
    }

    public void addPredicate(String predicate){
        this.selectedPredicates.add(predicate);
    }
    public void addObject(String object){
        this.selectedObjects.add(object);
    }

    public void removeDuplicates() {
        // Remove duplicates by converting lists to Sets and then back to Lists
        if (selectedSubjects != null) {
            selectedSubjects = new ArrayList<>(new HashSet<>(selectedSubjects));
        }
        if (selectedPredicates != null) {
            selectedPredicates = new ArrayList<>(new HashSet<>(selectedPredicates));
        }
        if (selectedObjects != null) {
            selectedObjects = new ArrayList<>(new HashSet<>(selectedObjects));
        }
    }
}
