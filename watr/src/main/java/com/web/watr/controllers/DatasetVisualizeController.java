package com.web.watr.controllers;

import com.web.watr.services.DatasetQueryService;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Controller
public class DatasetVisualizeController {

    @Value("${fuseki.dataset.path}")
    private String datasetPath;
    @Autowired
    private FusekiServer fusekiServer;

    private static int pageSize;
    private static DatasetQueryService datasetQueryService;

    @PostConstruct
    public void init(){
        pageSize=20;
        datasetQueryService=new DatasetQueryService(pageSize);
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

        List<List<String>> tableData = datasetQueryService.executePagedSelectQuery(ds, page);
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
    public String getRDFData(Model model) {
        return "yes";
    }
}
