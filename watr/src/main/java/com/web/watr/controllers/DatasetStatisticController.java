package com.web.watr.controllers;

import com.web.watr.services.query.DatasetQueryService;
import com.web.watr.services.query.StatisticQueryService;
import com.web.watr.utils.MethodUtils;
import jakarta.annotation.PostConstruct;
import org.apache.jena.query.Dataset;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.CompletableFuture;

@Controller
public class DatasetStatisticController {

    @Value("${fuseki.dataset.path}")
    private String datasetPath;

    private static DatasetQueryService datasetQueryService;
    private static StatisticQueryService statisticQueryService;

    @PostConstruct
    public void init(){
        datasetQueryService=new DatasetQueryService();
        statisticQueryService= new StatisticQueryService();
    }

    @GetMapping("/visualize-statistic-page")
    @Async
    public CompletableFuture<String> getStatisticPage() {
        return CompletableFuture.completedFuture("/content/statistics-dataset");
    }

    @GetMapping("/visualize-statistics")
    @Async
    public CompletableFuture<String> getStatistics(@RequestParam() String dataset, Model model){
        model.addAttribute("selectedDataset", dataset);
        return CompletableFuture.completedFuture("/fragments/statistics-container");
    }

    @GetMapping("/statistic-count")
    @Async
    public CompletableFuture<String> getCounts(@RequestParam() String dataset,
                            @RequestParam() String tripleType,
                                     Model model){

        var type=MethodUtils.TRIPLE_TYPE.valueOf(tripleType.toUpperCase());
        Path path = Paths.get(datasetPath, dataset);
        Dataset ds= statisticQueryService.loadDataset(path);
        var result = statisticQueryService.executeCountQuery(ds, type);
        model.addAttribute("counts", result);
        return CompletableFuture.completedFuture("/fragments/statistics/counts");
    }

    @GetMapping("/statistic-predicate")
    @Async
    public CompletableFuture<String> getObjectDistributionForPredicate(@RequestParam() String dataset,
                                                    @RequestParam() String predicate,
                                                    Model model){

        Path path = Paths.get(datasetPath, dataset);
        Dataset ds= statisticQueryService.loadDataset(path);
        var result= statisticQueryService.categorizeObjects(ds, predicate);
        model.addAttribute("resourceObjects", result.getOtherObjects().isEmpty() ? null : result.getOtherObjects());
        model.addAttribute("literalObjects", result.getNumericalLiterals().isEmpty() ? null : result.getNumericalLiterals());
        return CompletableFuture.completedFuture("/fragments/statistics/distribution");
    }


}
