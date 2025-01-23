package com.web.watr.controllers;

import com.web.watr.beans.FilterBean;
import com.web.watr.services.query.DatasetQueryService;
import com.web.watr.services.query.StatisticQueryService;
import jakarta.annotation.PostConstruct;
import org.apache.jena.query.Dataset;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.AbstractMap;
import java.util.List;

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

    @GetMapping("/visualize-table-page")
    public String getVisualizePage(){
        return "/content/visualize-dataset-table";
    }


    @GetMapping("/visualize-rdf-graph-page")
    public String getVisualizeRDFPage(){
        return "/content/visualize-dataset-rdf";
    }
    @GetMapping("/visualize-table-data")
    public  String getTableData(@RequestParam(defaultValue = "0") int page,
                                @RequestParam() String dataset,
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

    return "/table/rdftable";
    }

    @GetMapping("/visualize-rdf-data")
    public String getRDFData(@RequestParam() String dataset, Model model) {
        filters.resetFilter();
        model.addAttribute("selectedDataset", dataset);
        return "/search/dropdown-search";
    }
    @GetMapping("/filter-subjects")
    public String getRDFDataSubjects(@RequestParam String dataset,
                                     @RequestParam(defaultValue = "0") int page,
                                     @RequestParam(required = false) String search,
                                     @RequestParam(name = "first-request", required = false) Boolean firstRequest,
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
        return (firstRequest!=null && firstRequest) ? "/fragments/filter-dropdown" : "/fragments/filter-dropdown-list" ;
    }

    @GetMapping("/filter-predicates")
    public String getRDFDataPredicates(@RequestParam String dataset,
                                       @RequestParam(defaultValue = "0") int page,
                                       @RequestParam(required = false) String search,
                                       @RequestParam(name = "first-request", required = false) Boolean firstRequest,
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
        return (firstRequest!=null && firstRequest) ? "/fragments/filter-dropdown" : "/fragments/filter-dropdown-list" ;
    }
    @GetMapping("/filter-objects")
    public String getRDFDataObjects(@RequestParam String dataset,
                                       @RequestParam(defaultValue = "0") int page,
                                       @RequestParam(required = false) String search,
                                    @RequestParam(name = "first-request", required = false) Boolean firstRequest,
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
        return (firstRequest!=null && firstRequest) ? "/fragments/filter-dropdown" : "/fragments/filter-dropdown-list" ;
    }
    @GetMapping("/generate-graph")
    public String getGraphRepresentation(@RequestParam String dataset, Model model){

        Path path = Paths.get(datasetPath, dataset);
        Dataset ds= datasetQueryService.loadDataset(path);
        var result = datasetQueryService.executePagedSelectByFilterQuery(ds, filters);
        model.addAttribute("selectedDataset", dataset);
        model.addAttribute("nodes",result.get("nodes").toString());
        model.addAttribute("edges",result.get("edges").toString());

        return "/fragments/graph-container";
    }
    @GetMapping("/generate-jsonld")
    public ResponseEntity<Object> getJsonLDRepresentation(@RequestParam String dataset, Model model){

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
    @GetMapping("/details-subject")
    public String getNodeDetails(@RequestParam String dataset, @RequestParam String name, Model model){
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

        model.addAttribute("relationDetails", null);
        model.addAttribute("statisticType","subject");
        model.addAttribute("outDegree", outDegree);
        model.addAttribute("statisticCount", statisticCount);
        model.addAttribute("vocabDetails", vcContent);
        model.addAttribute("dataset",dataset);
        return "fragments/graph-details";
    }
    /*
    * Object

Is Resource / Is Literal
In-Degree
Out-Degree (r)
Predicate Count
Datatype (l)
Range [min max] (l)
Average (l)
Language tag

TO DO TOMORRRRROW details-object
    *
    * */


    @GetMapping("/details-edge")
    public String getEdgeDetails(@RequestParam String dataset, @RequestParam String name, Model model){
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

        return "fragments/graph-details";
    }


}
