package com.web.watr.controllers;

import com.web.watr.services.query.DatasetQueryService;
import com.web.watr.services.query.StatisticQueryService;
import com.web.watr.utils.MethodUtils;
import jakarta.annotation.PostConstruct;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
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

    @Operation(summary = "Visualize Statistics Page", description = "Returns the page for visualizing dataset statistics asynchronously.")
    @ApiResponse(responseCode = "200", description = "Statistics page returned successfully")
    @GetMapping("/visualize-statistic-page")
    @Async
    public CompletableFuture<String> getStatisticPage() {
        return CompletableFuture.completedFuture("/content/statistics-dataset");
    }

    @Operation(summary = "Get Dataset Statistics", description = "Returns statistics for the selected dataset asynchronously.")
    @ApiResponse(responseCode = "200", description = "Statistics for the dataset returned successfully")
    @ApiResponse(responseCode = "400", description = "Bad request, missing or incorrect parameters")
    @GetMapping("/visualize-statistics")
    @Async
    public CompletableFuture<String> getStatistics(
            @Parameter(description = "Dataset filename") @RequestParam() String dataset,
            Model model){
        model.addAttribute("selectedDataset", dataset);
        return CompletableFuture.completedFuture("/fragments/statistics-container");
    }

    @Operation(summary = "Get Count Statistics", description = "Returns count statistics for a dataset based on triple type.")
    @ApiResponse(responseCode = "200", description = "Count statistics for the dataset returned successfully")
    @ApiResponse(responseCode = "400", description = "Invalid dataset or triple type provided")
    @ApiResponse(responseCode = "500", description = "Server error while processing the dataset")
    @GetMapping("/statistic-count")
    @Async
    public CompletableFuture<String> getCounts(
            @Parameter(description = "Dataset filename") @RequestParam() String dataset,
            @Parameter(description = "Type of triples (e.g., subject, predicate, object)") @RequestParam() String tripleType,
                                     Model model){

        var type=MethodUtils.TRIPLE_TYPE.valueOf(tripleType.toUpperCase());
        Path path = Paths.get(datasetPath, dataset);
        Dataset ds= statisticQueryService.loadDataset(path);
        var result = statisticQueryService.executeCountQuery(ds, type);
        model.addAttribute("counts", result);
        return CompletableFuture.completedFuture("/fragments/statistics/counts");
    }

    @Operation(summary = "Get Predicate Object Distribution", description = "Returns the object distribution for a given predicate in the dataset.")
    @ApiResponse(responseCode = "200", description = "Object distribution for predicate returned successfully")
    @ApiResponse(responseCode = "400", description = "Invalid dataset or predicate provided")
    @ApiResponse(responseCode = "500", description = "Server error while processing the dataset")
    @GetMapping("/statistic-predicate")
    @Async
    public CompletableFuture<String> getObjectDistributionForPredicate(
            @Parameter(description = "Dataset filename") @RequestParam() String dataset,
            @Parameter(description = "Predicate name") @RequestParam() String predicate,
                                                    Model model){

        Path path = Paths.get(datasetPath, dataset);
        Dataset ds= statisticQueryService.loadDataset(path);
        var result= statisticQueryService.categorizeObjects(ds, predicate);
        model.addAttribute("resourceObjects", result.getOtherObjects().isEmpty() ? null : result.getOtherObjects());
        model.addAttribute("literalObjects", result.getNumericalLiterals().isEmpty() ? null : result.getNumericalLiterals());
        return CompletableFuture.completedFuture("/fragments/statistics/distribution");
    }


}
