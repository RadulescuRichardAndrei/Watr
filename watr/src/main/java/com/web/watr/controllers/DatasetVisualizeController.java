package com.web.watr.controllers;

import com.web.watr.beans.FilterBean;
import com.web.watr.services.DatasetQueryService;
import com.web.watr.utils.MethodUtils;
import jakarta.annotation.PostConstruct;
import org.apache.jena.assembler.Mode;
import org.apache.jena.fuseki.main.FusekiServer;
import org.apache.jena.query.*;
import org.apache.jena.riot.RDFDataMgr;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

@Controller
public class DatasetVisualizeController {

    @Autowired
    private FilterBean filters;
    @Value("${fuseki.dataset.path}")
    private String datasetPath;
    @Autowired
    private FusekiServer fusekiServer;

    private static int pageSize;
    private static int pageSizeSmall;
    private static DatasetQueryService datasetQueryService;

    @PostConstruct
    public void init(){
        pageSize=20;
        pageSizeSmall=8;
        datasetQueryService=new DatasetQueryService();
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
                                @RequestParam(required = true) String dataset,
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
    public String getRDFData(@RequestParam(required = true) String dataset, Model model) {

        model.addAttribute("selectedDataset", dataset);
        return "/search/dropdown-search";
    }
    @GetMapping("/filter-subjects")
    public String getRDFDataSubjects(@RequestParam String dataset,
                                     @RequestParam(defaultValue = "0") int page,
                                     @RequestParam(required = false) String search,
                             Model model){

        Path path = Paths.get(datasetPath, dataset);
        Dataset ds= datasetQueryService.loadDataset(path);

        List<String> subjects= datasetQueryService.executePagedSelectSubjects(ds,page, pageSizeSmall, search);

        String searchParam = search != null && !search.isEmpty() ? "&search=" + search : "";

        model.addAttribute("filters", filters);

        model.addAttribute("paginatedList", subjects);
        model.addAttribute("label","subject");
        model.addAttribute("endpoint","/filter-subject");
        model.addAttribute("currentPage",page);
        model.addAttribute("nextPage", (subjects.size() == pageSizeSmall) ? "/filter-subjects?dataset=" +
                                                    dataset + "&page=" + (page + 1) + searchParam : null);
        model.addAttribute("previousPage", (page > 0) ? "/filter-subjects?dataset=" +
                                                    dataset + "&page=" + (page - 1) + searchParam : null);
        return (search==null || page>0) ? "/fragments/filter-dropdown" : "/fragments/filter-dropdown-list" ;
    }
    @GetMapping("/filter-predicates")
    public String getRDFDataPredicates(@RequestParam String dataset,
                                       @RequestParam(defaultValue = "0") int page,
                                       @RequestParam(required = false) String search,
                                       Model model){
        Path path = Paths.get(datasetPath, dataset);
        Dataset ds= datasetQueryService.loadDataset(path);

        List<String> subjects= datasetQueryService.executePagedSelectPredicates(ds,page, pageSizeSmall, search);

        String searchParam = search != null && !search.isEmpty() ? "&search=" + search : "";

        model.addAttribute("filters", filters);
        model.addAttribute("paginatedList", subjects);
        model.addAttribute("label","predicate");
        model.addAttribute("endpoint","/filter-subject");
        model.addAttribute("currentPage",page);
        model.addAttribute("nextPage", (subjects.size() == pageSizeSmall) ? "/filter-subjects?dataset=" +
                dataset + "&page=" + (page + 1) + searchParam : null);
        model.addAttribute("previousPage", (page > 0) ? "/filter-subjects?dataset=" +
                dataset + "&page=" + (page - 1) + searchParam : null);
        return (search==null || page>0) ? "/fragments/filter-dropdown" : "/fragments/filter-dropdown-list" ;
    }
    @GetMapping("/filter-objects")
    public String getRDFDataObjects(@RequestParam String dataset,
                                       @RequestParam(defaultValue = "0") int page,
                                       @RequestParam(required = false) String search,
                                       Model model){

        Path path = Paths.get(datasetPath, dataset);
        Dataset ds= datasetQueryService.loadDataset(path);

        List<String> subjects= datasetQueryService.executePagedSelectObjects(ds,page, pageSizeSmall, search);

        String searchParam = search != null && !search.isEmpty() ? "&search=" + search : "";

        model.addAttribute("filters", filters);
        model.addAttribute("paginatedList", subjects);
        model.addAttribute("label","object");
        model.addAttribute("endpoint","/filter-subject");
        model.addAttribute("currentPage",page);
        model.addAttribute("nextPage", (subjects.size() == pageSizeSmall) ? "/filter-subjects?dataset=" +
                dataset + "&page=" + (page + 1) + searchParam : null);
        model.addAttribute("previousPage", (page > 0) ? "/filter-subjects?dataset=" +
                dataset + "&page=" + (page - 1) + searchParam : null);
        return (search==null || page>0) ? "/fragments/filter-dropdown" : "/fragments/filter-dropdown-list" ;
    }
}
