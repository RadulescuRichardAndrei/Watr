package com.web.watr.controllers;

import com.web.watr.services.DatasetQueryService;
import jakarta.annotation.PostConstruct;
import org.apache.jena.query.Dataset;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.nio.file.Path;
import java.nio.file.Paths;

@Controller
public class DatasetStatisticController {

    @Value("${fuseki.dataset.path}")
    private String datasetPath;

    private static DatasetQueryService datasetQueryService;

    @PostConstruct
    public void init(){
        datasetQueryService=new DatasetQueryService();
    }

    @GetMapping("/predicate-counts")
    public String getPredicateCounts(@RequestParam() String dataset,
                                     Model model){

        Path path = Paths.get(datasetPath, dataset);
        Dataset ds= datasetQueryService.loadDataset(path);
        var result = datasetQueryService.executeCountPredicatesQuery(ds);
        model.addAttribute("predicateCounts", result);
        return "";
    }
    @GetMapping("/general-statistics")
    public String getGeneralStatistics(@RequestParam() String dataset,
                                       Model model){

        
        return "";
    }


}
