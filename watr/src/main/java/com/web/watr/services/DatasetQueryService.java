package com.web.watr.services;

import org.apache.jena.atlas.lib.Pair;
import org.apache.jena.atlas.lib.tuple.Tuple;
import org.apache.jena.base.Sys;
import org.apache.jena.query.*;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.riot.RDFDataMgr;

import java.nio.file.Path;
import java.util.*;

public class DatasetQueryService {

    private final int pageSize;

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
                return entry.getKey();
            }
        }
        return uri;
    }
    private String getSufix(String uri){
        for (Map.Entry<String, String> entry : NAMESPACE_PREFIXES.entrySet()) {
            if (uri.startsWith(entry.getKey())) {
                return uri.substring(entry.getKey().length());
            }
        }
        return uri;
    }



    public DatasetQueryService(int pageSize) {
        this.pageSize= pageSize;
    }

    public Dataset loadDataset(Path path) {
        return RDFDataMgr.loadDataset(path.toString());
    }

    public Set<String> getNamespaces(Dataset dataset){
        String queryString =
                "SELECT DISTINCT (STR(?namespace) AS ?namespace) WHERE { " +
                        "  GRAPH ?g { " +
                        "    ?subject ?predicate ?object " +
                        "  } " +
                        "  { " +
                        "    # Extract namespaces from subject URIs " +
                        "    ?subject ?p ?o . " +
                        "    FILTER(ISIRI(?subject)) " +
                        "    BIND(IRI(STR(?subject)) AS ?namespace) " +
                        "  } " +
                        "  UNION " +
                        "  { " +
                        "    # Extract namespaces from predicate URIs " +
                        "    ?s ?predicate ?o . " +
                        "    FILTER(ISIRI(?predicate)) " +
                        "    BIND(IRI(STR(?predicate)) AS ?namespace) " +
                        "  } " +
                        "  UNION " +
                        "  { " +
                        "    # Extract namespaces from object URIs " +
                        "    ?s ?p ?object . " +
                        "    FILTER(ISIRI(?object)) " +
                        "    BIND(IRI(STR(?object)) AS ?namespace) " +
                        "  } " +
                        "}";
        Query query = QueryFactory.create(queryString);
        Set<String> namespaces = new HashSet<>();
        try (QueryExecution qexec = QueryExecutionFactory.create(query, dataset)){
            ResultSet results = qexec.execSelect();
            while (results.hasNext()) {
                QuerySolution solution = results.nextSolution();
                String namespace = solution.getLiteral("namespace").getString();
                namespaces.add(namespace);
            }
        }
        System.out.println(namespaces);
        return namespaces;
    }
    public List<List<String>> executePagedSelectQuery(Dataset dataset, int page) {
        int offset= page* pageSize;
        String queryString = "SELECT ?subject ?predicate ?object " +
                "WHERE { GRAPH ?g { ?subject ?predicate ?object } } " + // Use GRAPH to account for named graphs
                "LIMIT " + pageSize + " OFFSET " + offset;

        Query query = QueryFactory.create(queryString);
        List<List<String>> tableData = new ArrayList<>();
        try (QueryExecution qexec = QueryExecutionFactory.create(query, dataset)) {
            ResultSet results = qexec.execSelect();

            while (results.hasNext()) {
                QuerySolution soln = results.nextSolution();
                List<String> row = new ArrayList<>();

                RDFNode subject= soln.get("subject");
                row.add(subject.toString());
                row.add(getShortenUri(subject.toString()));

                RDFNode predicate= soln.get("predicate");
                row.add(getSufix(predicate.toString()));
                row.add(getShortenUri(predicate.toString()));



                RDFNode object= soln.get("object");
                row.add(getSufix(object.toString()));
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
        return tableData;
    }

}
