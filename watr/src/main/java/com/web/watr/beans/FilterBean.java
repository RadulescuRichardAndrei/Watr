package com.web.watr.beans;

import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.SessionScope;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@SessionScope
@Component
public class FilterBean implements Serializable {
    private List<String> selectedSubjects = new ArrayList<>();
    private List<String> selectedPredicates = new ArrayList<>();
    private List<String> selectedObjects = new ArrayList<>();


    @PostConstruct
    public void init(){
        selectedSubjects= new ArrayList<>();
        selectedPredicates= new ArrayList<>();
        selectedObjects=new ArrayList<>();
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

    // Methods to add/remove selections
    public void addSubject(String subject) {
        selectedSubjects.add(subject);
    }

    public void removeSubject(String subject) {
        selectedSubjects.remove(subject);
    }

    public void addPredicate(String predicate) {
        selectedPredicates.add(predicate);
    }

    public void removePredicate(String predicate) {
        selectedPredicates.remove(predicate);
    }

    public void addObject(String object) {
        selectedObjects.add(object);
    }

    public void removeObject(String object) {
        selectedObjects.remove(object);
    }
    public boolean containsInList(String label, String smth){
        if (label.equals("subject"))
            return selectedSubjects.contains(smth);
        if (label.equals("predicate"))
            return selectedPredicates.contains(smth);
        if (label.equals("object"))
            return selectedObjects.contains(smth);
        return false;
    }
}
