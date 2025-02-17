package com.web.watr.controllers;

import com.web.watr.beans.FilterBean;
import com.web.watr.services.TripletsMatcher;
import com.web.watr.services.query.DatasetQueryService;
import com.web.watr.services.query.StatisticQueryService;
import com.web.watr.utils.FileUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.annotation.PostConstruct;
import org.apache.jena.query.Dataset;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Controller
public class DatasetVisualizeController {

    @Autowired
    private FilterBean filters;
    @Value("${fuseki.dataset.path}")
    private String datasetPath;

    private static int pageSize;
    private static int pageSizeSmall;
    private static DatasetQueryService datasetQueryService;
    private static StatisticQueryService statisticQueryService;

    @PostConstruct
    public void init(){
        pageSize=20;
        pageSizeSmall=8;
        datasetQueryService=new DatasetQueryService();
        statisticQueryService= new StatisticQueryService();
    }

    @Operation(summary = "Get Visualize Table Page", description = "Returns the template for visualizing dataset in a table.")
    @ApiResponse(responseCode = "200", description = "Visualize dataset table page returned successfully")
    @GetMapping("/visualize-table-page")
    @Async
    public CompletableFuture<String> getVisualizePage(){
        return CompletableFuture.completedFuture("/content/visualize-dataset-table");
    }

    @Operation(summary = "Get Visualize RDF Graph Page", description = "Returns the template for visualizing dataset as an RDF graph.")
    @ApiResponse(responseCode = "200", description = "Visualize RDF graph page returned successfully")
    @GetMapping("/visualize-rdf-graph-page")
    @Async
    public CompletableFuture<String> getVisualizeRDFPage(){
        return CompletableFuture.completedFuture("/content/visualize-dataset-rdf");
    }

    @Operation(summary = "Get Table Data", description = "Retrieves paginated table data from a dataset.")
    @ApiResponse(responseCode = "200", description = "Table data retrieved successfully")
    @GetMapping("/visualize-table-data")
    @Async
    public  CompletableFuture<String> getTableData(
            @Parameter(description = "Page number for pagination") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Dataset filename") @RequestParam() String dataset,
                                Model model){
        Path path = Paths.get(datasetPath, dataset);
        Dataset ds= datasetQueryService.loadDataset(path);

        List<List<String>> tableData = datasetQueryService.executePagedSelectQuery(ds, page, pageSize);
        List<String> namespaces= tableData.getLast();
        tableData.remove(tableData.getLast());


        model.addAttribute("namespaces",  String.join(", ", namespaces));
        model.addAttribute("tableName", dataset.split("\\.")[0]);
        model.addAttribute("nextPage", (tableData.size() == pageSize) ? "/visualize-table-data?page=" + (page + 1) + "&dataset=" + dataset : null);
        model.addAttribute("previousPage", (page>0) ? "/visualize-table-data?page=" + (page - 1) + "&dataset=" + dataset : null);
        model.addAttribute("tableData", tableData);
        model.addAttribute("currentPage", page);

    return CompletableFuture.completedFuture("/table/rdftable");
    }

    @Operation(summary = "Get RDF Data", description = "Retrieves RDF data for visualization.")
    @ApiResponse(responseCode = "200", description = "RDF data retrieved successfully")
    @GetMapping("/visualize-rdf-data")
    @Async
    public CompletableFuture<String> getRDFData(
            @Parameter(description = "Dataset filename") @RequestParam() String dataset,
            Model model) {
        filters.resetFilter();
        model.addAttribute("selectedDataset", dataset);
        return CompletableFuture.completedFuture("/search/dropdown-search");
    }

    @Operation(summary = "Filter RDF Subjects", description = "Filters RDF subjects in the dataset based on search criteria.")
    @ApiResponse(responseCode = "200", description = "Filtered RDF subjects retrieved successfully")
    @GetMapping("/filter-subjects")
    @Async
    public CompletableFuture<String> getRDFDataSubjects(
            @Parameter(description = "Dataset filename") @RequestParam String dataset,
            @Parameter(description = "Page number for pagination") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Search filter") @RequestParam(required = false) String search,
            @Parameter(description = "Indicates if this is the first request") @RequestParam(name = "first-request", required = false) Boolean firstRequest,
                             Model model){
        Path path = Paths.get(datasetPath, dataset);
        Dataset ds= datasetQueryService.loadDataset(path);

        List<String> subjects= datasetQueryService.executePagedSelectSubjects(ds,page, pageSizeSmall, search);

        String searchParam = search != null && !search.isEmpty() ? "&search=" + search : "";

        model.addAttribute("filters", filters);

        model.addAttribute("paginatedList", subjects);
        model.addAttribute("label","subject");
        model.addAttribute("endpoint","/filter-subjects?dataset=" + dataset);
        model.addAttribute("currentPage",page);
        model.addAttribute("nextPage", (subjects.size() == pageSizeSmall) ? "/filter-subjects?dataset=" +
                                                    dataset + "&page=" + (page + 1) + searchParam : null);
        model.addAttribute("previousPage", (page > 0) ? "/filter-subjects?dataset=" +
                                                    dataset + "&page=" + (page - 1) + searchParam : null);
        return (firstRequest!=null && firstRequest) ? CompletableFuture.completedFuture("/fragments/filters/filter-dropdown")
                : CompletableFuture.completedFuture("/fragments/filters/filter-dropdown-list");
    }

    @Operation(summary = "Filter RDF Predicates", description = "Filters RDF predicates in the dataset based on search criteria.")
    @ApiResponse(responseCode = "200", description = "Filtered RDF predicates retrieved successfully")
    @GetMapping("/filter-predicates")
    @Async
    public CompletableFuture<String> getRDFDataPredicates(
            @Parameter(description = "Dataset filename") @RequestParam String dataset,
            @Parameter(description = "Page number for pagination") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Search filter") @RequestParam(required = false) String search,
            @Parameter(description = "Indicates if this is the first request") @RequestParam(name = "first-request", required = false) Boolean firstRequest,
                                       Model model){
        Path path = Paths.get(datasetPath, dataset);
        Dataset ds= datasetQueryService.loadDataset(path);

        List<String> subjects= datasetQueryService.executePagedSelectPredicates(ds,page, pageSizeSmall, search);

        String searchParam = search != null && !search.isEmpty() ? "&search=" + search : "";

        model.addAttribute("filters", filters);
        model.addAttribute("paginatedList", subjects);
        model.addAttribute("label","predicate");
        model.addAttribute("endpoint","/filter-predicates?dataset=" + dataset);
        model.addAttribute("currentPage",page);
        model.addAttribute("nextPage", (subjects.size() == pageSizeSmall) ? "/filter-predicates?dataset=" +
                dataset + "&page=" + (page + 1) + searchParam : null);
        model.addAttribute("previousPage", (page > 0) ? "/filter-predicates?dataset=" +
                dataset + "&page=" + (page - 1) + searchParam : null);
        return (firstRequest!=null && firstRequest) ?
                CompletableFuture.completedFuture("/fragments/filters/filter-dropdown")
                : CompletableFuture.completedFuture("/fragments/filters/filter-dropdown-list") ;
    }

    @Operation(summary = "Select RDF Predicates", description = "Retrieves a paginated list of predicates for selection in the dataset.")
    @ApiResponse(responseCode = "200", description = "RDF predicates retrieved successfully")
    @GetMapping("/select-predicate")
    @Async
    public CompletableFuture<String> getRDFDataPredicates2(
            @Parameter(description = "Dataset filename") @RequestParam String dataset,
            @Parameter(description = "Page number for pagination") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Search filter") @RequestParam(required = false) String search,
            @Parameter(description = "Indicates if this is the first request") @RequestParam(name = "first-request", required = false) Boolean firstRequest,
                                       Model model){
        Path path = Paths.get(datasetPath, dataset);
        Dataset ds= datasetQueryService.loadDataset(path);

        List<String> subjects= datasetQueryService.executePagedSelectPredicates(ds,page, pageSizeSmall, search);

        String searchParam = search != null && !search.isEmpty() ? "&search=" + search : "";

        model.addAttribute("paginatedList", subjects);
        model.addAttribute("endpoint","/statistic-predicate?dataset=" + dataset);
        model.addAttribute("currentPage",page);
        model.addAttribute("nextPage", (subjects.size() == pageSizeSmall) ? "/filter-predicates?dataset=" +
                dataset + "&page=" + (page + 1) + searchParam : null);
        model.addAttribute("previousPage", (page > 0) ? "/filter-predicates?dataset=" +
                dataset + "&page=" + (page - 1) + searchParam : null);
        return (firstRequest!=null && firstRequest) ?
                CompletableFuture.completedFuture("/fragments/filters/select-dropdown")
                : CompletableFuture.completedFuture("/fragments/filters/select-dropdown-list") ;
    }

    @Operation(summary = "Filter RDF Objects", description = "Filters RDF objects in the dataset based on search criteria.")
    @ApiResponse(responseCode = "200", description = "Filtered RDF objects retrieved successfully")
    @GetMapping("/filter-objects")
    @Async
    public CompletableFuture<String> getRDFDataObjects(
            @Parameter(description = "Dataset filename") @RequestParam String dataset,
            @Parameter(description = "Page number for pagination") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Search filter") @RequestParam(required = false) String search,
            @Parameter(description = "Indicates if this is the first request") @RequestParam(name = "first-request", required = false) Boolean firstRequest,
                                       Model model){

        Path path = Paths.get(datasetPath, dataset);
        Dataset ds= datasetQueryService.loadDataset(path);

        List<String> subjects= datasetQueryService.executePagedSelectObjects(ds,page, pageSizeSmall, search);

        String searchParam = search != null && !search.isEmpty() ? "&search=" + search : "";

        model.addAttribute("filters", filters);
        model.addAttribute("paginatedList", subjects);
        model.addAttribute("label","object");
        model.addAttribute("endpoint","/filter-objects?dataset=" + dataset);
        model.addAttribute("currentPage",page);
        model.addAttribute("nextPage", (subjects.size() == pageSizeSmall) ? "/filter-objects?dataset=" +
                dataset + "&page=" + (page + 1) + searchParam : null);
        model.addAttribute("previousPage", (page > 0) ? "/filter-objects?dataset=" +
                dataset + "&page=" + (page - 1) + searchParam : null);
        return (firstRequest!=null && firstRequest) ?
                CompletableFuture.completedFuture("/fragments/filters/filter-dropdown")
                : CompletableFuture.completedFuture("/fragments/filters/filter-dropdown-list");
    }

    @Operation(summary = "Generate Graph Representation", description = "Generates a visual representation of the dataset as a graph.")
    @ApiResponse(responseCode = "200", description = "Graph representation generated successfully")
    @GetMapping("/generate-graph")
    @Async
    public CompletableFuture<String> getGraphRepresentation(
            @Parameter(description = "Dataset filename") @RequestParam String dataset,
            Model model){

        Path path = Paths.get(datasetPath, dataset);
        Dataset ds= datasetQueryService.loadDataset(path);
        var result = datasetQueryService.executePagedSelectByFilterQuery(ds, filters);
        model.addAttribute("selectedDataset", dataset);
        model.addAttribute("nodes",result.get("nodes").toString());
        model.addAttribute("edges",result.get("edges").toString());

        return CompletableFuture.completedFuture("/fragments/graph-container");
    }

    @Operation(summary = "Generate JSON-LD Representation", description = "Generates and downloads a JSON-LD representation of the dataset.")
    @ApiResponse(responseCode = "200", description = "JSON-LD generated successfully")
    @ApiResponse(responseCode = "500", description = "Error in processing the data")
    @GetMapping("/generate-jsonld")
    public ResponseEntity<Object> getJsonLDRepresentation(
            @Parameter(description = "Dataset filename") @RequestParam String dataset,
            Model model){

        Path path = Paths.get(datasetPath, dataset);
        Dataset ds= datasetQueryService.loadDataset(path);
        var result = datasetQueryService.executePagedSelectByFilterQueryAndReturnJSONLD(ds, filters);
        if (result == null)
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error in processing the data");

        return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"data.jsonld\"")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new ByteArrayResource(result));

    }

    @Operation(summary = "Get Subject Node Details", description = "Retrieves details about a subject node.")
    @ApiResponse(responseCode = "200", description = "Subject node details retrieved successfully")
    @GetMapping("/details-subject")
    @Async
    public CompletableFuture<String> getSubjectNodeDetails(
            @Parameter(description = "Dataset filename") @RequestParam String dataset,
            @Parameter(description = "Subject name") @RequestParam String name,
            Model model){
        Path path = Paths.get(datasetPath, dataset);
        Dataset ds1= datasetQueryService.loadDataset(path);
        Dataset ds2= statisticQueryService.loadDataset(path);

        var vc= datasetQueryService.getAllDetails(name);
        var vcContent=List.of(
                new AbstractMap.SimpleEntry<>("Prefix", vc.get(0)),
                new AbstractMap.SimpleEntry<>("Long URI", vc.get(1)),
                new AbstractMap.SimpleEntry<>("Suffix", vc.get(2))
        );
        var outDegree= statisticQueryService.getOutDegreeSubject(ds2, name);
        var statisticCount= statisticQueryService.getPredicateObjectCountsForSubject(ds2, name);
        var predicatesAvg= statisticQueryService.getPredicatesAverageCountPerSubject(ds2);

        model.addAttribute("relationDetails", null);
        model.addAttribute("statisticType","subject");
        model.addAttribute("outDegree", outDegree);
        model.addAttribute("statisticCount", statisticCount);
        model.addAttribute("vocabDetails", vcContent);
        model.addAttribute("dataset",dataset);
        model.addAttribute("predicatesAvg", predicatesAvg);
        return CompletableFuture.completedFuture("fragments/graph-details");
    }

    @Operation(summary = "Get Object Node Details", description = "Retrieves details about an object node.")
    @ApiResponse(responseCode = "200", description = "Object node details retrieved successfully")
    @GetMapping("/details-object")
    @Async
    public CompletableFuture<String> getObjectNodeDetails(
            @Parameter(description = "Dataset filename") @RequestParam String dataset,
            @Parameter(description = "Object name") @RequestParam String name,
            @Parameter(description = "Type of the object (resource or literal)") @RequestParam(name = "object-type") String objectType,
                                       Model model){
        Path path = Paths.get(datasetPath, dataset);
        Dataset ds1= datasetQueryService.loadDataset(path);
        Dataset ds2= statisticQueryService.loadDataset(path);

        var vc= datasetQueryService.getAllDetails(name);
        var vcContent= new ArrayList<AbstractMap.SimpleEntry<String, String>>();
        if (vc == null){
            vcContent.add(new AbstractMap.SimpleEntry<>("Prefix", null));
            vcContent.add(new AbstractMap.SimpleEntry<>("Long URI", null));
            vcContent.add(new AbstractMap.SimpleEntry<>("Suffix", null));
        }else {
            vcContent.add(new AbstractMap.SimpleEntry<>("Prefix", vc.get(0)));
            vcContent.add(new AbstractMap.SimpleEntry<>("Long URI", vc.get(1)));
            vcContent.add(new AbstractMap.SimpleEntry<>("Suffix", vc.get(2)));
        }

        int outDegree= 0;
        if (objectType.equals("resource"))
            outDegree= statisticQueryService.getOutDegreeSubject(ds2, name);
        var inDegree= statisticQueryService.getInDegreeObject(ds2, name, objectType);
        var statisticObject= statisticQueryService.extractInfoForObject(ds2, name, objectType);

        model.addAttribute("relationDetails", null);
        model.addAttribute("statisticType","object");
        model.addAttribute("outDegree", outDegree);
        model.addAttribute("inDegree", inDegree);
        model.addAttribute("statisticObject", statisticObject);
        model.addAttribute("vocabDetails", vcContent);
        model.addAttribute("dataset",dataset);
        return CompletableFuture.completedFuture("fragments/graph-details");
    }

    @Operation(summary = "Get Edge Details", description = "Retrieves details about an edge (predicate) in the dataset graph.")
    @ApiResponse(responseCode = "200", description = "Edge details retrieved successfully")
    @GetMapping("/details-edge")
    @Async
    public CompletableFuture<String> getEdgeDetails(
            @Parameter(description = "Dataset filename") @RequestParam String dataset,
            @Parameter(description = "Predicate name") @RequestParam String name,
            Model model){
        Path path = Paths.get(datasetPath, dataset);
        Dataset ds1= datasetQueryService.loadDataset(path);
        Dataset ds2= statisticQueryService.loadDataset(path);

        var result = datasetQueryService.getDetailsAboutPredicate(ds1, name);
        var vc= datasetQueryService.getAllDetails(name);

        var vcContent=List.of(
                new AbstractMap.SimpleEntry<>("Prefix", vc.get(0)),
                new AbstractMap.SimpleEntry<>("Long URI", vc.get(1)),
                new AbstractMap.SimpleEntry<>("Suffix", vc.get(2))
        );
        var statisticCount= statisticQueryService.executePredicateCountQuery(ds2, name);
        var statisticCoOc= statisticQueryService.executeCoOccurringPredicatesQuery(ds2, name);

        model.addAttribute("relationDetails", result);
        model.addAttribute("vocabDetails", vcContent);
        model.addAttribute("statisticDetailsCount", statisticCount);
        model.addAttribute("statisticDetailsCoOc", statisticCoOc);
        model.addAttribute("statisticType","predicate");
        model.addAttribute("dataset",dataset);

        return CompletableFuture.completedFuture("fragments/graph-details");
    }

    @Operation(summary = "Compare Two Datasets", description = "Performs a comparison between two datasets.")
    @ApiResponse(responseCode = "200", description = "Comparison completed successfully")
    @GetMapping("/dataset-matcher")
    @Async
    public CompletableFuture<String> getMatches(
            @Parameter(description = "First dataset filename") @RequestParam(name= "first-dataset") String firstDataset,
            @Parameter(description = "Second dataset filename") @RequestParam(name="second-dataset") String secondDataset,
                             Model model){

        Path path1 = Paths.get(datasetPath, firstDataset);
        Path path2 = Paths.get(datasetPath, secondDataset);
        Dataset ds1= datasetQueryService.loadDataset(path1);
        var triplets1= datasetQueryService.getDistinctElementsFromTriplets(ds1);

        Dataset ds2= datasetQueryService.loadDataset(path2);
        var triplets2= datasetQueryService.getDistinctElementsFromTriplets(ds2);

        var stringMatcherS= TripletsMatcher.stringMatching(triplets1.get(0), triplets2.get(0),3);
        var stringMatcherP= TripletsMatcher.stringMatching(triplets1.get(1), triplets2.get(1),3);
        var stringMatcherO= TripletsMatcher.stringMatching(triplets1.get(2), triplets2.get(2),3);

        var dbpediaMatcherS= datasetQueryService.findEquivalentPropertiesDBpedia(triplets1.get(0), triplets2.get(0));
        var dbpediaMatcherP= datasetQueryService.findEquivalentPropertiesDBpedia(triplets1.get(1), triplets2.get(1));
        var dbpediaMatcherO= datasetQueryService.findEquivalentPropertiesDBpedia(triplets1.get(2), triplets2.get(2));

        var wikidataMatcherS= datasetQueryService.findEquivalentPropertiesWikidata(triplets1.get(0), triplets2.get(0));
        var wikidataMatcherP= datasetQueryService.findEquivalentPropertiesWikidata(triplets1.get(1), triplets2.get(1));
        var wikidataMatcherO= datasetQueryService.findEquivalentPropertiesWikidata(triplets1.get(2), triplets2.get(2));

        model.addAttribute("stringMatcherS", stringMatcherS);
        model.addAttribute("stringMatcherP", stringMatcherP);
        model.addAttribute("stringMatcherO", stringMatcherO);

        model.addAttribute("dbpediaMatcherS", dbpediaMatcherS);
        model.addAttribute("dbpediaMatcherP", dbpediaMatcherP);
        model.addAttribute("dbpediaMatcherO", dbpediaMatcherO);

        model.addAttribute("wikidataMatcherS", wikidataMatcherS);
        model.addAttribute("wikidataMatcherP", wikidataMatcherP);
        model.addAttribute("wikidataMatcherO", wikidataMatcherO);

        model.addAttribute("firstDataset", firstDataset);
        model.addAttribute("secondDataset", secondDataset);
        return CompletableFuture.supplyAsync(()-> {return "/fragments/matches";});
    }

    @Operation(summary = "Get Dataset Comparison Page", description = "Returns the template for comparing two datasets.")
    @ApiResponse(responseCode = "200", description = "Comparison page returned successfully")
    @GetMapping("/visualize-compare-page")
    @Async
    public CompletableFuture<String> getComparePage(Model model){
        File datasetDirectory = new File(datasetPath);
        File[] files = datasetDirectory.listFiles();
        if (files != null) {
            List<String> validFileNames = new ArrayList<>();
            for (File file : files) {
                String fileName = file.getName();
                if (FileUtils.isValidFileType(FileUtils.getFileExtension(fileName))) {
                    validFileNames.add(fileName);
                }
            }
            model.addAttribute("datasets", validFileNames);
        }
        else {
            model.addAttribute("datasets", new ArrayList<>());
        }
        return CompletableFuture.completedFuture( "/content/compare-dataset");
    }
}
