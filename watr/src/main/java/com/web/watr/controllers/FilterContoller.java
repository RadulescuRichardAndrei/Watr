package com.web.watr.controllers;


import com.web.watr.beans.FilterBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.concurrent.CompletableFuture;

@Controller
public class FilterContoller {
    @Autowired
    private FilterBean filters;

    //          ADD
    @PostMapping("/add-subject")
    @Async
    public CompletableFuture<String> addSubject(@RequestParam() String subject, Model model){
        filters.addSubject(subject);
        model.addAttribute("removeButtonEndpoint", "/remove-subject?subject="+subject);
        return CompletableFuture.completedFuture("fragments/filters/filter-element");
    }

    @PostMapping("/add-predicate")
    @Async
    public CompletableFuture<String> addPredicate(@RequestParam("predicate") String predicate, Model model) {
        filters.addPredicate(predicate);
        model.addAttribute("removeButtonEndpoint", "/remove-predicate?predicate="+predicate);
        return CompletableFuture.completedFuture("fragments/filters/filter-element");
    }

    @PostMapping("/add-object")
    @Async
    public CompletableFuture<String> addObject(@RequestParam("object") String object, Model model) {
        filters.addObject(object);
        model.addAttribute("removeButtonEndpoint", "/remove-object?object="+object);
        return CompletableFuture.completedFuture("fragments/filters/filter-element");
    }

    //          REMOVE
    @PostMapping("/remove-subject")
    public ResponseEntity<String> removeSubject(@RequestParam() String subject, Model model){
        if (filters.getSelectedSubjects().contains(subject))
            filters.removeSubject(subject);
        HttpHeaders headers= new HttpHeaders();
        headers.add("HX-Trigger","recheckCheckboxes");
        return ResponseEntity.ok().headers(headers).body("");
    }
    @PostMapping("/remove-predicate")
    public ResponseEntity<String> removePredicate(@RequestParam("predicate") String predicate, Model model) {
        if (filters.getSelectedPredicates().contains(predicate))
            filters.removePredicate(predicate);
        HttpHeaders headers= new HttpHeaders();
        headers.add("HX-Trigger","recheckCheckboxes");
        return ResponseEntity.ok().headers(headers).body("");
    }
    @PostMapping("/remove-object")
    public ResponseEntity<String> removeObject(@RequestParam("object") String object, Model model) {
        if (filters.getSelectedObjects().contains(object))
            filters.removeObject(object);

        HttpHeaders headers= new HttpHeaders();
        headers.add("HX-Trigger","recheckCheckboxes");
        return ResponseEntity.ok().headers(headers).body("");
    }

    //          TOGGLE
    @PostMapping("/toggle-subject")
    @Async
    public CompletableFuture<String> toggleSubject(@RequestParam() String subject, Model model){
        if (filters.getSelectedSubjects().contains(subject))
            filters.removeSubject(subject);
        else
            filters.addSubject(subject);

        model.addAttribute("filters", filters.getSelectedSubjects());
        model.addAttribute("id","active-filter-subjects");
        model.addAttribute("removeButtonEndpoint", "/remove-subject?subject=");
        return CompletableFuture.completedFuture("fragments/filters/filter-element-list");
    }

    @PostMapping("/toggle-predicate")
    @Async
    public CompletableFuture<String> togglePredicate(@RequestParam() String predicate, Model model) {
        if (filters.getSelectedPredicates().contains(predicate))
            filters.removePredicate(predicate);
        else
            filters.addPredicate(predicate);

        model.addAttribute("filters", filters.getSelectedPredicates());
        model.addAttribute("id", "active-filter-predicates");
        model.addAttribute("removeButtonEndpoint", "/remove-predicate?predicate=");
        return CompletableFuture.completedFuture("fragments/filters/filter-element-list");
    }

    @PostMapping("/toggle-object")
    @Async
    public CompletableFuture<String> toggleObject(@RequestParam() String object, Model model) {
        if (filters.getSelectedObjects().contains(object))
            filters.removeObject(object);
        else
            filters.addObject(object);

        model.addAttribute("filters", filters.getSelectedObjects());
        model.addAttribute("id", "active-filter-objects");
        model.addAttribute("removeButtonEndpoint", "/remove-object?object=");
        return CompletableFuture.completedFuture("fragments/filters/filter-element-list");
    }

    //              GET-ALL
    @GetMapping("/get-subjects")
    @Async
    public CompletableFuture<String> getSubjects(Model model){
        model.addAttribute("filters", filters.getSelectedSubjects());
        model.addAttribute("id","active-filter-subjects");
        model.addAttribute("removeButtonEndpoint", "/remove-subject?subject=");
        return CompletableFuture.completedFuture("fragments/filters/filter-element-list");
    }

    @GetMapping("/get-predicates")
    @Async
    public CompletableFuture<String> getPredicates(Model model) {
        model.addAttribute("filters", filters.getSelectedPredicates());
        model.addAttribute("id","active-filter-predicates");
        model.addAttribute("removeButtonEndpoint", "/remove-predicate?predicate=");
        return CompletableFuture.completedFuture("fragments/filters/filter-element-list");
    }

    @GetMapping("/get-objects")
    @Async
    public CompletableFuture<String> getObjects(Model model) {
        model.addAttribute("filters", filters.getSelectedObjects());
        model.addAttribute("id","active-filter-objects");
        model.addAttribute("removeButtonEndpoint", "/remove-object?object=");
        return CompletableFuture.completedFuture("fragments/filters/filter-element-list");
    }

}
