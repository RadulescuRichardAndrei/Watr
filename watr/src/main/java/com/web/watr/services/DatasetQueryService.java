package com.web.watr.services;

import com.web.watr.beans.FilterBean;
import com.web.watr.utils.MethodUtils;
import org.apache.jena.arq.querybuilder.ExprFactory;
import org.apache.jena.arq.querybuilder.SelectBuilder;
import org.apache.jena.atlas.json.JsonArray;
import org.apache.jena.atlas.json.JsonObject;
import org.apache.jena.query.*;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.sparql.expr.ExprVar;
import org.apache.jena.sparql.expr.NodeValue;

import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

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
            Map.entry("http://www.w3.org/ns/oa#", "oa"),
            Map.entry("http://www.w3.org/2001/XMLSchema-instance#", "xsi")
    );


    private String getShortenUri(String uri) {
        if (uri == null)
            return null;

        for (Map.Entry<String, String> entry : NAMESPACE_PREFIXES.entrySet()) {
            if (uri.startsWith(entry.getKey())) {
                String localName = uri.substring(entry.getKey().length());
                return entry.getValue() + ":" + localName;
            }
        }
        return uri;
    }

    private String getLongUri(String uri){
        if (uri == null)
            return null;
        if (!uri.contains(":")){
            var exactMatch= NAMESPACE_PREFIXES.entrySet().stream().filter(entry -> entry.getValue().equals(uri)).findFirst().orElse(null);
            if (exactMatch != null)
                return exactMatch.getKey();

            var result= NAMESPACE_PREFIXES.entrySet().stream().filter(entry -> entry.getValue().startsWith(uri)).findFirst().orElse(null);
            return (result != null) ? result.getKey() : null;
        }

        for (Map.Entry<String, String> entry : NAMESPACE_PREFIXES.entrySet())
            if(uri.startsWith(entry.getValue())){
                String localName= uri.substring(entry.getValue().length()+1);
                return entry.getKey() + localName;
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
        /*String queryString = "SELECT ?subject ?predicate ?object " +
                "WHERE { GRAPH ?g { ?subject ?predicate ?object } } " + // Use GRAPH to account for named graphs
                "LIMIT " + pageSize + " OFFSET " + offset;*/

        SelectBuilder selectBuilder = new SelectBuilder().addVar("subject")
                .addVar("predicate")
                .addVar("object")
                .addWhere("?subject", "?predicate", "?object");

        selectBuilder.addUnion(
                new SelectBuilder().addGraph("?g", "?subject", "?predicate", "?object")
        );
        selectBuilder.setLimit(pageSize).setOffset(offset);

        String queryString = selectBuilder.buildString();
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

        SelectBuilder selectBuilder = new SelectBuilder().setDistinct(true)
                .addVar("subject").addGraph("?g","?subject", "?predicate", "?object");

        selectBuilder.addUnion(
                new SelectBuilder().addWhere("?subject", "?predicate", "?object")
        );

        if (subjectName != null && !subjectName.isEmpty()) {
            ExprFactory exprFactory = new ExprFactory();
            var longUri = getLongUri(subjectName);
            if (longUri == null)
                return Collections.emptyList();
            selectBuilder.addFilter(exprFactory.strstarts(
                    exprFactory.str(new ExprVar("subject")),
                    NodeValue.makeString(longUri)
                    )
            );
        }

        selectBuilder.addOrderBy("subject")
                .setLimit(pageSize).setOffset(offset);

        String queryString = selectBuilder.buildString();
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

        SelectBuilder selectBuilder = new SelectBuilder().setDistinct(true)
                .addVar("predicate").addGraph("?g", "?subject", "?predicate", "?object");

        selectBuilder.addUnion(
                new SelectBuilder().addWhere("?subject", "?predicate", "?object")
        );

        if (predicateName != null && !predicateName.isEmpty()) {
            ExprFactory exprFactory = new ExprFactory();
            var longUri = getLongUri(predicateName);
            if (longUri == null)
                return Collections.emptyList();
            selectBuilder.addFilter(exprFactory.strstarts(
                    exprFactory.str(new ExprVar("predicate")),
                    NodeValue.makeString(longUri)
            ));
        }

        selectBuilder.addOrderBy("predicate")
                .setLimit(pageSize).setOffset(offset);

        String queryString = selectBuilder.buildString();
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

        SelectBuilder selectBuilder = new SelectBuilder().setDistinct(true)
                .addVar("object").addGraph("?g", "?subject", "?predicate", "?object");

        selectBuilder.addUnion(
                new SelectBuilder().addWhere("?subject", "?predicate", "?object")
        );

        if (objectName != null && !objectName.isEmpty()) {
            ExprFactory exprFactory = new ExprFactory();
            var longUri = getLongUri(objectName);
            if (longUri == null)
                return Collections.emptyList();
            selectBuilder.addFilter(exprFactory.strstarts(
                    exprFactory.str(new ExprVar("object")),
                    NodeValue.makeString(longUri)
            ));
        }

        selectBuilder.addOrderBy("object")
                .setLimit(pageSize).setOffset(offset);

        String queryString = selectBuilder.buildString();
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

    public  Map<String, JsonArray> executePagedSelectByFilterQuery(Dataset dataset, FilterBean filters){

        SelectBuilder sb= new SelectBuilder();


        sb.addVar("*").addGraph("?g", new SelectBuilder()
                .addWhere("?subject", "?predicate", "?object"));

        if (MethodUtils.existsAndNotEmpty(filters.getSelectedSubjects())){
            String subjects= filters.getSelectedSubjects().stream().map(this::getLongUri).map(uri -> "<" + uri + ">").collect(Collectors.joining());
            sb.addFilter("?subject IN (" + subjects + ")");
        }
        if (MethodUtils.existsAndNotEmpty(filters.getSelectedPredicates())){
            String predicates= filters.getSelectedPredicates().stream().map(this::getLongUri).map(uri -> "<" + uri + ">").collect(Collectors.joining());
            sb.addFilter("?predicate IN (" + predicates + ")");
        }
        if (MethodUtils.existsAndNotEmpty(filters.getSelectedObjects())){
            String objects= filters.getSelectedObjects().stream().map(this::getLongUri).map(uri -> "<" + uri + ">").collect(Collectors.joining());
            sb.addFilter("?object IN (" + objects + ")");
        }
        Query query= sb.build();

        Map<String, JsonArray> result = new HashMap<>();
        JsonArray nodes= new JsonArray();
        JsonArray edges= new JsonArray();
        Set<String> nodeSet = new HashSet<>();

        try(QueryExecution qexec = QueryExecutionFactory.create(query, dataset)) {
            ResultSet results = qexec.execSelect();

            while (results.hasNext()) {
                QuerySolution solution = results.nextSolution();
                String subject = solution.get("subject").toString();
                String object = solution.get("object").toString();
                String predicate = solution.get("predicate").toString();
                if (!nodeSet.contains(subject)) {
                    JsonObject jsonNode= new JsonObject();
                    jsonNode.put("id",subject);
                    jsonNode.put("label", getShortenUri(subject));
                    nodes.add(jsonNode);
                    nodeSet.add(subject);
                }
                if (!nodeSet.contains(object)) {
                    JsonObject jsonNode= new JsonObject();
                    jsonNode.put("id",object);
                    jsonNode.put("label", getShortenUri(object));
                    nodes.add(jsonNode);
                    if(solution.get("object").isResource())
                        nodeSet.add(object);
                }
                JsonObject jsonNode= new JsonObject();
                jsonNode.put("from",subject);
                jsonNode.put("to",object);
                jsonNode.put("label", getShortenUri(predicate));
                edges.add(jsonNode);
            }
        }
        result.put("nodes", nodes);
        result.put("edges", edges);
        return  result;
    }

}
