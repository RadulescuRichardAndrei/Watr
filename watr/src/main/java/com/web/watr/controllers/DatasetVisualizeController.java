package com.web.watr.controllers;

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

@Controller
public class DatasetVisualizeController {

    @Value("${fuseki.dataset.path}")
    private String datasetPath;
    @Autowired
    private FusekiServer fusekiServer;

    private static int pageSize=20;

    @GetMapping("/visualize-table-page")
    public String getVisualizePage(){
        return "/content/visualize-dataset-table";
    }
    @GetMapping("/visualize-table-data")
    public  String getTableData(@RequestParam(defaultValue = "0") int page,
                                @RequestParam(required = true) String dataset,
                                Model model){
        Path path = Paths.get(datasetPath, dataset);
        Dataset ds= RDFDataMgr.loadDataset(path.toString());

        int offset= page* pageSize;
        String queryString = "SELECT ?subject ?predicate ?object " +
                "WHERE { GRAPH ?g { ?subject ?predicate ?object } } " + // Use GRAPH to account for named graphs
                "LIMIT " + pageSize + " OFFSET " + offset;

        Query query = QueryFactory.create(queryString);
        List<List<String>> tableData = new ArrayList<>();
        try (QueryExecution qexec = QueryExecutionFactory.create(query, ds)) {
            ResultSet results = qexec.execSelect();

            while (results.hasNext()) {
                QuerySolution soln = results.nextSolution();
                List<String> row = new ArrayList<>();
                row.add(soln.get("subject").toString());
                row.add(soln.get("predicate").toString());
                row.add(soln.get("object").toString());
                tableData.add(row);
            }
        }
        model.addAttribute("tableName", dataset.split("\\.")[0]);
        model.addAttribute("nextPage", (tableData.size() == pageSize) ? "/visualize-table-data?page=" + (page + 1) + "&dataset=" + dataset : null);
        model.addAttribute("previousPage", (page>0) ? "/visualize-table-data?page=" + (page - 1) + "&dataset=" + dataset : null);
        model.addAttribute("tableData", tableData);
        model.addAttribute("currentPage", page);

    return "/table/rdftable";
    }

}
