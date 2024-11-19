package com.web.watr.services;

import com.web.watr.utils.MethodUtils;
import org.apache.jena.arq.querybuilder.SelectBuilder;
import org.apache.jena.query.*;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.riot.RDFDataMgr;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class DatasetQueryService {

    private static final Map<String, String> NAMESPACE_PREFIXES = Map.ofEntries(
            Map.entry("http://www.w3.org/1999/02/22-rdf-syntax-ns#", "rdf"),
            Map.entry("http://www.w3.org/2000/01/rdf-schema#", "rdfs"),
            Map.entry("http://www.w3.org/2002/07/owl#", "owl"),
            Map.entry("http://schema.org/", "schema"),
            Map.entry("http://www.w3.org/2001/XMLSchema#", "xsd"),
            Map.entry("http://www.w3.org/ns/foaf#", "foaf"),
            Map.entry("http://xmlns.com/foaf/0.1/", "foaf"),
            Map.entry("http://www.w3.org/2002/12/cal/ical#", "ical"),
            Map.entry("http://www.w3.org/ns/dc#", "dc"),
            Map.entry("http://purl.org/dc/elements/1.1/", "dc"),
            Map.entry("http://purl.org/dc/terms/", "dcterms"),
            Map.entry("http://www.w3.org/ns/prov#", "prov"),
            Map.entry("http://www.w3.org/ns/skos#", "skos"),
            Map.entry("http://www.w3.org/2004/02/skos/core#", "skos"),
            Map.entry("http://www.opengis.net/ont/geosparql#", "geo"),
            Map.entry("http://www.w3.org/ns/ldp#", "ldp"),
            Map.entry("http://www.w3.org/ns/shacl#", "shacl"),
            Map.entry("http://www.w3.org/ns/solid/terms#", "solid"),
            Map.entry("https://www.w3.org/ns/activitystreams#", "as"),
            Map.entry("https://www.w3.org/ns/webvtt#", "vtt"),
            Map.entry("http://www.w3.org/1999/02/22-rdf-syntax-ns#Bag", "rdf:Bag"),
            Map.entry("http://www.w3.org/1999/02/22-rdf-syntax-ns#Alt", "rdf:Alt"),
            Map.entry("http://www.w3.org/1999/02/22-rdf-syntax-ns#Seq", "rdf:Seq"),
            Map.entry("http://www.w3.org/ns/oa#", "oa"),
            Map.entry("http://www.w3.org/2001/XMLSchema-instance#", "xsi")
    );


    private String getShortenUri(String uri) {
        for (Map.Entry<String, String> entry : NAMESPACE_PREFIXES.entrySet()) {
            if (uri.startsWith(entry.getKey())) {
                String localName = uri.substring(entry.getKey().length());
                return entry.getValue() + ":" + localName;
            }
        }
        return uri;
    }
    private String getPrefix(String uri) {
        for (Map.Entry<String, String> entry : NAMESPACE_PREFIXES.entrySet()) {
            if (uri.startsWith(entry.getKey())) {
                return entry.getValue() + ": " + entry.getKey();
            }
        }
        return null;
    }
    private String getSufix(String uri){
        for (Map.Entry<String, String> entry : NAMESPACE_PREFIXES.entrySet()) {
            if (uri.startsWith(entry.getKey())) {
                return uri.substring(entry.getKey().length());
            }
        }
        return uri;
    }

    public Dataset loadDataset(Path path) {
        return RDFDataMgr.loadDataset(path.toString());
    }

    public List<List<String>> executePagedSelectQuery(Dataset dataset, int page, int pageSize) {
        int offset= page* pageSize;
        String queryString = "SELECT ?subject ?predicate ?object " +
                "WHERE { GRAPH ?g { ?subject ?predicate ?object } } " + // Use GRAPH to account for named graphs
                "LIMIT " + pageSize + " OFFSET " + offset;

        Query query = QueryFactory.create(queryString);
        List<List<String>> tableData = new ArrayList<>();
        List<String> namespaces= new ArrayList<>();
        try (QueryExecution qexec = QueryExecutionFactory.create(query, dataset)) {
            ResultSet results = qexec.execSelect();

            while (results.hasNext()) {
                QuerySolution soln = results.nextSolution();
                List<String> row = new ArrayList<>();

                RDFNode subject= soln.get("subject");
                row.add(subject.toString());
                row.add(getShortenUri(subject.toString()));
                if (getPrefix(subject.toString()) != null && !namespaces.contains(getPrefix(subject.toString())))
                    namespaces.add(getPrefix(subject.toString()));

                RDFNode predicate= soln.get("predicate");
                row.add(getSufix(predicate.toString()));
                row.add(getShortenUri(predicate.toString()));

                if (getPrefix(predicate.toString())!=null && !namespaces.contains(getPrefix(predicate.toString())))
                    namespaces.add(getPrefix(predicate.toString()));

                RDFNode object= soln.get("object");
                row.add(getSufix(object.toString()));

                if (getPrefix(object.toString())!=null && !namespaces.contains(getPrefix(object.toString())))
                    namespaces.add(getPrefix(object.toString()));

                StringBuilder attributesConstruct= new StringBuilder();

                if (object.isResource()) {
                    attributesConstruct.append("resource='").append(getShortenUri(object.toString())).append("'").append(",");
                } else if (object.isLiteral()) {
                    attributesConstruct.append("datatype='").append(object.asLiteral().getDatatype()).append("'").append(",");
                    if (object.asLiteral().getLanguage()!= null && !object.asLiteral().getLanguage().isEmpty())
                        attributesConstruct.append("lang='").append(object.asLiteral().getLanguage()).append("'");
                }
                row.add(attributesConstruct.toString());
                tableData.add(row);
            }
        }
        tableData.add(namespaces);
        return tableData;
    }

    public List<String> executePagedSelectSubjects(Dataset dataset, int page, int pageSize, String subjectName) {
        int offset = page * pageSize;
        String queryString = "SELECT DISTINCT ?subject " +
                "WHERE { GRAPH ?g { ?subject ?predicate ?object } " +
                (subjectName != null && !subjectName.isEmpty() ?
                        "FILTER STRSTARTS(STR(?subject), '" + subjectName + "'). " : "") +
                "} " +
                "LIMIT " + pageSize + " OFFSET " + offset;

        Query query = QueryFactory.create(queryString);
        List<String> subjects = new ArrayList<>();
        try (QueryExecution qexec = QueryExecutionFactory.create(query, dataset)) {
            ResultSet results = qexec.execSelect();

            while (results.hasNext()) {
                QuerySolution soln = results.nextSolution();
                subjects.add(getShortenUri(soln.get("subject").toString()));
            }
        }
        return subjects;
    }

    public List<String> executePagedSelectPredicates(Dataset dataset, int page, int pageSize, String predicateName) {
        int offset = page * pageSize;
        String queryString = "SELECT DISTINCT ?predicate " +
                "WHERE { GRAPH ?g { ?subject ?predicate ?object } " +
                (predicateName != null && !predicateName.isEmpty() ?
                        "FILTER STRSTARTS(STR(?predicate), '" + predicateName + "'). " : "") +
                "} " +
                "LIMIT " + pageSize + " OFFSET " + offset;

        Query query = QueryFactory.create(queryString);
        List<String> predicates = new ArrayList<>();

        try (QueryExecution qexec = QueryExecutionFactory.create(query, dataset)) {
            ResultSet results = qexec.execSelect();

            while (results.hasNext()) {
                QuerySolution soln = results.nextSolution();
                predicates.add(getShortenUri(soln.get("predicate").toString()));
            }
        }

        return predicates;
    }


    public List<String> executePagedSelectObjects(Dataset dataset, int page, int pageSize, String objectName) {
        int offset = page * pageSize;
        String queryString = "SELECT DISTINCT ?object " +
                "WHERE { GRAPH ?g { ?subject ?predicate ?object } " +
                (objectName != null && !objectName.isEmpty() ?
                        "FILTER STRSTARTS(STR(?object), '" + objectName + "'). " : "") +
                "} " +
                "LIMIT " + pageSize + " OFFSET " + offset;

        Query query = QueryFactory.create(queryString);
        List<String> objects = new ArrayList<>();

        try (QueryExecution qexec = QueryExecutionFactory.create(query, dataset)) {
            ResultSet results = qexec.execSelect();

            while (results.hasNext()) {
                QuerySolution soln = results.nextSolution();
                objects.add(getShortenUri(soln.get("object").toString()));
            }
        }

        return objects;
    }
    public List<List<String>> executePagedSelectByFilterQuery(Dataset dataset, List<List<String>> filters){

        SelectBuilder sb= new SelectBuilder();
        sb.addVar("*").addGraph("?g", new SelectBuilder()
                .addWhere("?subject", "?predicate", "?object"));

        if (MethodUtils.existsAndNotEmpty(filters.get(0))){
            sb.addFilter("?subject IN (" + filters.get(0) + ")");
        }
        if (MethodUtils.existsAndNotEmpty(filters.get(1))){
            sb.addFilter("?predicate IN (" + filters.get(1) + ")");
        }
        if (MethodUtils.existsAndNotEmpty(filters.get(2))){
            sb.addFilter("?object IN (" + filters.get(2) + ")");
        }
        Query query= sb.build();

        List<List<String>> resultList= List.of(new ArrayList<>(),new ArrayList<>(), new ArrayList<>());

        try(QueryExecution qexec = QueryExecutionFactory.create(query, dataset)) {
            ResultSet results = qexec.execSelect();

            while (results.hasNext()) {
                QuerySolution solution = results.nextSolution();
                // Collect values based on the variables selected
                if (solution.contains("subject")) {
                    resultList.get(0).add(solution.get("subject").toString());
                } else if (solution.contains("predicate")) {
                    resultList.get(1).add(solution.get("predicate").toString());
                } else if (solution.contains("object")) {
                    resultList.get(2).add(solution.get("object").toString());
                }
            }
        }
        return  resultList;
    }

}
