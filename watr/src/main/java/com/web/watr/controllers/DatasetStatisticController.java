package com.web.watr.controllers;

import com.web.watr.beans.FilterBean;
import com.web.watr.services.DatasetQueryService;
import com.web.watr.utils.MethodUtils;
import jakarta.annotation.PostConstruct;
import org.apache.jena.query.Dataset;
import org.springframework.beans.factory.annotation.Autowired;
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
    @Autowired
    private FilterBean filters;
    private static DatasetQueryService datasetQueryService;

    @PostConstruct
    public void init(){
        datasetQueryService=new DatasetQueryService();
    }

    @GetMapping("/visualize-statistic-page")
    public String getStatisticPage() {return "/content/statistics-dataset";}
    @GetMapping("/visualize-statistics")
    public String getStatistics(@RequestParam() String dataset, Model model){
        model.addAttribute("selectedDataset", dataset);
        return "/fragments/statistics-container";
    }
    @GetMapping("/statistic-count")
    public String getCounts(@RequestParam() String dataset,
                            @RequestParam() String tripleType,
                                     Model model){

        var type=MethodUtils.TRIPLE_TYPE.valueOf(tripleType.toUpperCase());
        Path path = Paths.get(datasetPath, dataset);
        Dataset ds= datasetQueryService.loadDataset(path);
        var result = datasetQueryService.executeCountQuery(ds, type);
        model.addAttribute("counts", result);
        return "/fragments/statistics/counts";
    }
    @GetMapping("/general-statistics")
    public String getGeneralStatistics(@RequestParam() String dataset,
                                       Model model){

        
        return "";
    }


}
