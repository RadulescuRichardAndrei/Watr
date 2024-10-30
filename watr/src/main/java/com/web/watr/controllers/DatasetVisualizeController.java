package com.web.watr.controllers;

import org.apache.jena.fuseki.main.FusekiServer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class DatasetVisualizeController {

    @Autowired
    private FusekiServer fusekiServer;

    @GetMapping("/visualize-table")
    public String getVisualizePage(){
        return "/content/visualize-dataset-table";
    }
}
