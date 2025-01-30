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
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

import java.util.concurrent.CompletableFuture;

@Controller
public class FilterContoller {
    @Autowired
    private FilterBean filters;

    //          ADD
    @Operation(summary = "Add Subject to Filters", description = "Adds a subject to the filters asynchronously.")
    @ApiResponse(responseCode = "200", description = "Subject added successfully")
    @ApiResponse(responseCode = "400", description = "Bad request, missing or incorrect parameters")
    @PostMapping("/add-subject")
    @Async
    public CompletableFuture<String> addSubject(
            @Parameter(description = "Subject name") @RequestParam() String subject,
            Model model){
        filters.addSubject(subject);
        model.addAttribute("removeButtonEndpoint", "/remove-subject?subject="+subject);
        return CompletableFuture.completedFuture("fragments/filters/filter-element");
    }

    @Operation(summary = "Add Predicate to Filters", description = "Adds a predicate to the filters asynchronously.")
    @ApiResponse(responseCode = "200", description = "Predicate added successfully")
    @PostMapping("/add-predicate")
    @Async
    public CompletableFuture<String> addPredicate(
            @Parameter(description = "Predicate name") @RequestParam("predicate") String predicate,
            Model model) {
        filters.addPredicate(predicate);
        model.addAttribute("removeButtonEndpoint", "/remove-predicate?predicate="+predicate);
        return CompletableFuture.completedFuture("fragments/filters/filter-element");
    }

    @Operation(summary = "Add Object to Filters", description = "Adds an object to the filters asynchronously.")
    @ApiResponse(responseCode = "200", description = "Object added successfully")
    @PostMapping("/add-object")
    @Async
    public CompletableFuture<String> addObject(
            @Parameter(description = "Object name") @RequestParam("object") String object,
            Model model) {
        filters.addObject(object);
        model.addAttribute("removeButtonEndpoint", "/remove-object?object="+object);
        return CompletableFuture.completedFuture("fragments/filters/filter-element");
    }

    //          REMOVE
    @Operation(summary = "Remove Subject from Filters", description = "Removes a subject from the filters.")
    @ApiResponse(responseCode = "200", description = "Subject removed successfully")
    @ApiResponse(responseCode = "400", description = "Bad request, subject not found")
    @PostMapping("/remove-subject")
    public ResponseEntity<String> removeSubject(
            @Parameter(description = "Subject name") @RequestParam() String subject,
            Model model){
        if (filters.getSelectedSubjects().contains(subject))
            filters.removeSubject(subject);
        HttpHeaders headers= new HttpHeaders();
        headers.add("HX-Trigger","recheckCheckboxes");
        return ResponseEntity.ok().headers(headers).body("");
    }

    @Operation(summary = "Remove Predicate from Filters", description = "Removes a predicate from the filters.")
    @ApiResponse(responseCode = "200", description = "Predicate removed successfully")
    @ApiResponse(responseCode = "400", description = "Bad request, predicate not found")
    @PostMapping("/remove-predicate")
    public ResponseEntity<String> removePredicate(
            @Parameter(description = "Predicate name") @RequestParam("predicate") String predicate,
            Model model) {
        if (filters.getSelectedPredicates().contains(predicate))
            filters.removePredicate(predicate);
        HttpHeaders headers= new HttpHeaders();
        headers.add("HX-Trigger","recheckCheckboxes");
        return ResponseEntity.ok().headers(headers).body("");
    }

    @Operation(summary = "Remove Object from Filters", description = "Removes an object from the filters.")
    @ApiResponse(responseCode = "200", description = "Object removed successfully")
    @ApiResponse(responseCode = "400", description = "Bad request, object not found")
    @PostMapping("/remove-object")
    public ResponseEntity<String> removeObject(
            @Parameter(description = "Object name") @RequestParam("object") String object,
            Model model) {
        if (filters.getSelectedObjects().contains(object))
            filters.removeObject(object);

        HttpHeaders headers= new HttpHeaders();
        headers.add("HX-Trigger","recheckCheckboxes");
        return ResponseEntity.ok().headers(headers).body("");
    }

    //          TOGGLE
    @Operation(summary = "Toggle Subject in Filters", description = "Toggles a subject in the filters (add or remove).")
    @ApiResponse(responseCode = "200", description = "Subject toggled successfully")
    @PostMapping("/toggle-subject")
    @Async
    public CompletableFuture<String> toggleSubject(
            @Parameter(description = "Subject name") @RequestParam() String subject,
            Model model){
        if (filters.getSelectedSubjects().contains(subject))
            filters.removeSubject(subject);
        else
            filters.addSubject(subject);

        model.addAttribute("filters", filters.getSelectedSubjects());
        model.addAttribute("id","active-filter-subjects");
        model.addAttribute("removeButtonEndpoint", "/remove-subject?subject=");
        return CompletableFuture.completedFuture("fragments/filters/filter-element-list");
    }

    @Operation(summary = "Toggle Predicate in Filters", description = "Toggles a predicate in the filters (add or remove).")
    @ApiResponse(responseCode = "200", description = "Predicate toggled successfully")
    @PostMapping("/toggle-predicate")
    @Async
    public CompletableFuture<String> togglePredicate(
            @Parameter(description = "Predicate name") @RequestParam() String predicate,
            Model model) {
        if (filters.getSelectedPredicates().contains(predicate))
            filters.removePredicate(predicate);
        else
            filters.addPredicate(predicate);

        model.addAttribute("filters", filters.getSelectedPredicates());
        model.addAttribute("id", "active-filter-predicates");
        model.addAttribute("removeButtonEndpoint", "/remove-predicate?predicate=");
        return CompletableFuture.completedFuture("fragments/filters/filter-element-list");
    }

    @Operation(summary = "Toggle Object in Filters", description = "Toggles an object in the filters (add or remove).")
    @ApiResponse(responseCode = "200", description = "Object toggled successfully")
    @PostMapping("/toggle-object")
    @Async
    public CompletableFuture<String> toggleObject(
            @Parameter(description = "Object name") @RequestParam() String object,
            Model model) {
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
    @Operation(summary = "Get All Subjects in Filters", description = "Returns a list of all subjects currently in the filters.")
    @ApiResponse(responseCode = "200", description = "List of subjects returned successfully")
    @GetMapping("/get-subjects")
    @Async
    public CompletableFuture<String> getSubjects(Model model){
        model.addAttribute("filters", filters.getSelectedSubjects());
        model.addAttribute("id","active-filter-subjects");
        model.addAttribute("removeButtonEndpoint", "/remove-subject?subject=");
        return CompletableFuture.completedFuture("fragments/filters/filter-element-list");
    }

    @Operation(summary = "Get All Predicates in Filters", description = "Returns a list of all predicates currently in the filters.")
    @ApiResponse(responseCode = "200", description = "List of predicates returned successfully")
    @GetMapping("/get-predicates")
    @Async
    public CompletableFuture<String> getPredicates(Model model) {
        model.addAttribute("filters", filters.getSelectedPredicates());
        model.addAttribute("id","active-filter-predicates");
        model.addAttribute("removeButtonEndpoint", "/remove-predicate?predicate=");
        return CompletableFuture.completedFuture("fragments/filters/filter-element-list");
    }

    @Operation(summary = "Get All Objects in Filters", description = "Returns a list of all objects currently in the filters.")
    @ApiResponse(responseCode = "200", description = "List of objects returned successfully")
    @GetMapping("/get-objects")
    @Async
    public CompletableFuture<String> getObjects(Model model) {
        model.addAttribute("filters", filters.getSelectedObjects());
        model.addAttribute("id","active-filter-objects");
        model.addAttribute("removeButtonEndpoint", "/remove-object?object=");
        return CompletableFuture.completedFuture("fragments/filters/filter-element-list");
    }

}
