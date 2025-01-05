package com.web.watr.services;

import com.web.watr.beans.FilterBean;
import com.web.watr.utils.MethodUtils;
import org.apache.jena.arq.querybuilder.ExprFactory;
import org.apache.jena.arq.querybuilder.SelectBuilder;
import org.apache.jena.arq.querybuilder.WhereBuilder;
import org.apache.jena.atlas.json.JsonArray;
import org.apache.jena.atlas.json.JsonObject;
import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.query.*;
import org.apache.jena.rdf.model.*;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.sparql.expr.ExprVar;
import org.apache.jena.sparql.expr.NodeValue;
import org.apache.jena.sparql.syntax.ElementGroup;
import org.apache.jena.sparql.syntax.ElementService;
import org.apache.jena.vocabulary.RDF;

import java.io.ByteArrayOutputStream;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

public class DatasetQueryService {

    private static final List<Map.Entry<String,String>> NAMESPACE_PREFIXES = new ArrayList<>(List.of(
            Map.entry("http://www.w3.org/1999/02/22-rdf-syntax-ns#", "rdf"),
            Map.entry("http://www.w3.org/2000/01/rdf-schema#", "rdfs"),
            Map.entry("http://www.w3.org/2002/07/owl#", "owl"),
            Map.entry("http://schema.org/", "schema"),
            Map.entry("http://www.w3.org/2001/XMLSchema#", "xsd"),
            Map.entry("http://www.w3.org/ns/foaf#", "foaf"),
            Map.entry("http://xmlns.com/foaf/0.1/", "foaf1"),
            Map.entry("http://www.w3.org/2002/12/cal/ical#", "ical"),
            Map.entry("http://www.w3.org/ns/dc#", "dc"),
            Map.entry("http://purl.org/dc/elements/1.1/", "dc1"),
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
            Map.entry("http://www.w3.org/2001/XMLSchema-instance#", "xsi"),
            Map.entry("http://dbpedia.org/property/", "dbpedia"),
            Map.entry("http://www.wikidata.org/entity/", "wikidata"),
            Map.entry("http://dbpedia.org/ontology/", "dbpedia_ontology"),
            Map.entry("http://www.openlinksw.com/schemas/virtrdf#", "virtrdf"),
            Map.entry("http://www.ontologydesignpatterns.org/ont/dul/DUL.owl#", "dul")

    ));

    public DatasetQueryService() {
        NAMESPACE_PREFIXES.sort(Comparator.comparingInt((Map.Entry<String, String> entry) -> entry.getValue().length()).reversed());
        System.out.println("yes");
    }

    private String getShortenUri(String uri) {
        if (uri == null)
            return null;

        for (Map.Entry<String, String> entry : NAMESPACE_PREFIXES) {
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
            var exactMatch= NAMESPACE_PREFIXES.stream().filter(entry -> entry.getValue().equals(uri)).findFirst().orElse(null);
            if (exactMatch != null)
                return exactMatch.getKey();

            var result= NAMESPACE_PREFIXES.stream().filter(entry -> entry.getValue().startsWith(uri)).findFirst().orElse(null);
            return (result != null) ? result.getKey() : null;
        }

        for (Map.Entry<String, String> entry : NAMESPACE_PREFIXES)
            if(uri.startsWith(entry.getValue())){
                String localName= uri.substring(entry.getValue().length()+1);
                return entry.getKey() + localName;
            }
        return uri;
    }
    private String getPrefix(String uri) {
        for (Map.Entry<String, String> entry : NAMESPACE_PREFIXES) {
            if (uri.startsWith(entry.getKey())) {
                return entry.getValue() + ": " + entry.getKey();
            }
        }
        return null;
    }
    private String getSuffix(String uri){
        for (Map.Entry<String, String> entry : NAMESPACE_PREFIXES) {
            if (uri.startsWith(entry.getKey())) {
                return uri.substring(entry.getKey().length());
            }
        }
        return uri;
    }
    public List<String> getAllDetails(String uri){
        String longUri = getLongUri(uri);
        List<String> details = new ArrayList<>();
        NAMESPACE_PREFIXES.stream()
                .filter(entry-> longUri.startsWith(entry.getKey()))
                .findFirst().ifPresentOrElse(
                    entry -> details.add(entry.getKey()),
                    () -> details.add(null)
                );
        details.add(longUri);
        int index= Math.max(uri.lastIndexOf(':'), uri.lastIndexOf('/'));
        details.add(index > -1 ? uri.substring(index+1) : null);
        return details;
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
                row.add(getSuffix(predicate.toString()));
                row.add(getShortenUri(predicate.toString()));

                if (getPrefix(predicate.toString())!=null && !namespaces.contains(getPrefix(predicate.toString())))
                    namespaces.add(getPrefix(predicate.toString()));

                RDFNode object= soln.get("object");
                row.add(getSuffix(object.toString()));

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

        SelectBuilder sbDeafultGraph= new SelectBuilder().addWhere("?subject", "?predicate", "?object");

        if (MethodUtils.existsAndNotEmpty(filters.getSelectedSubjects())){
            String subjects= filters.getSelectedSubjects().stream().map(this::getLongUri).map(uri -> "<" + uri + ">").collect(Collectors.joining(", "));
            sb.addFilter("?subject IN (" + subjects + ")");
            sbDeafultGraph.addFilter("?subject IN (" + subjects + ")");

        }
        if (MethodUtils.existsAndNotEmpty(filters.getSelectedPredicates())){
            String predicates= filters.getSelectedPredicates().stream().map(this::getLongUri).map(uri -> "<" + uri + ">").collect(Collectors.joining(", "));
            sb.addFilter("?predicate IN (" + predicates + ")");
            sbDeafultGraph.addFilter("?predicate IN (" + predicates + ")");
        }
        if (MethodUtils.existsAndNotEmpty(filters.getSelectedObjects())){
            String objects= filters.getSelectedObjects().stream().map(
                    obj ->{
                        if (!obj.contains(":") || obj.contains("^^"))//for literals
                            return '"' + obj + '"';
                        else
                            return "<" + getLongUri(obj) + ">";//for resources
                    }).collect(Collectors.joining(", "));
            sb.addFilter("?object IN (" + objects + ")");
            sbDeafultGraph.addFilter("?object IN (" + objects + ")");
        }

        sb.addUnion(sbDeafultGraph);

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
                    //if(solution.get("object").isResource())
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

    public byte[] executePagedSelectByFilterQueryAndReturnJSONLD(Dataset dataset, FilterBean filters){
        SelectBuilder sb= new SelectBuilder();

        sb.addVar("*").addGraph("?g", new SelectBuilder()
                .addWhere("?subject", "?predicate", "?object"));

        SelectBuilder sbDeafultGraph= new SelectBuilder().addWhere("?subject", "?predicate", "?object");

        if (MethodUtils.existsAndNotEmpty(filters.getSelectedSubjects())){
            String subjects= filters.getSelectedSubjects().stream().map(this::getLongUri).map(uri -> "<" + uri + ">").collect(Collectors.joining(", "));
            sb.addFilter("?subject IN (" + subjects + ")");
            sbDeafultGraph.addFilter("?subject IN (" + subjects + ")");

        }
        if (MethodUtils.existsAndNotEmpty(filters.getSelectedPredicates())){
            String predicates= filters.getSelectedPredicates().stream().map(this::getLongUri).map(uri -> "<" + uri + ">").collect(Collectors.joining(", "));
            sb.addFilter("?predicate IN (" + predicates + ")");
            sbDeafultGraph.addFilter("?predicate IN (" + predicates + ")");
        }
        if (MethodUtils.existsAndNotEmpty(filters.getSelectedObjects())){
            String objects= filters.getSelectedObjects().stream().map(
                    obj ->{
                        if (!obj.contains(":") || obj.contains("^^"))//for literals
                            return '"' + obj + '"';
                        else
                            return "<" + getLongUri(obj) + ">";//for resources
                    }).collect(Collectors.joining(", "));
            sb.addFilter("?object IN (" + objects + ")");
            sbDeafultGraph.addFilter("?object IN (" + objects + ")");
        }

        sb.addUnion(sbDeafultGraph);

        Query query= sb.build();
        try(QueryExecution qexec = QueryExecutionFactory.create(query, dataset)) {
            ResultSet results = qexec.execSelect();

            Model model = ModelFactory.createDefaultModel();
            while (results.hasNext()){
                QuerySolution solution = results.nextSolution();
                Resource subject = solution.getResource("subject");
                Property predicate = model.createProperty(solution.getResource("predicate").getURI());
                RDFNode object = solution.get("object");

                model.add(subject, predicate, object);
            }

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

            RDFDataMgr.write(outputStream, model, Lang.JSONLD);

            return outputStream.toByteArray();
        } catch (Exception e){
            return null;
        }

    }
    public List<List<String>> getDetailsAboutPredicate(Dataset dataset, String predicate){
        SelectBuilder sb= new SelectBuilder();

        predicate= new StringBuilder().append("<").append(getLongUri(predicate)).append(">").toString();

        sb.addPrefix("owl", "http://www.w3.org/2002/07/owl#")
                .addPrefix("rdfs", "http://www.w3.org/2000/01/rdf-schema#");



        sb.addVar("domain").addVar("range")
                .addVar("equivalentProperty").addVar("subProperty").addVar("inverseProperty");

        sb.addWhere(new WhereBuilder()
                .addPrefix("rdfs", "http://www.w3.org/2000/01/rdf-schema#")
                .addWhere(predicate, "rdfs:domain", "?domain")
                .addUnion(new WhereBuilder()
                        .addPrefix("rdfs", "http://www.w3.org/2000/01/rdf-schema#")
                        .addWhere(predicate, "rdfs:range", "?range"))
                .addUnion(new WhereBuilder()
                        .addPrefix("owl", "http://www.w3.org/2002/07/owl#")
                        .addWhere(predicate, "owl:equivalentProperty", "?equivalentProperty"))
                .addUnion(new WhereBuilder()
                        .addPrefix("rdfs", "http://www.w3.org/2000/01/rdf-schema#")
                        .addWhere(predicate, "rdfs:subPropertyOf", "?subProperty"))
                .addUnion(new WhereBuilder()
                        .addPrefix("owl", "http://www.w3.org/2002/07/owl#")
                        .addWhere(predicate, "owl:inverseOf", "?inverseProperty")));


        Query query = sb.build();
        ElementGroup body = new ElementGroup();
        body.addElement(new ElementService("http://dbpedia.org/sparql", query.getQueryPattern()));
        //body.addElement(new ElementService("https://query.wikidata.org/sparql", query.getQueryPattern()));

        query.setQueryPattern(body);

        List<String> equivalentProperties = new ArrayList<>();
        List<String> subProperties = new ArrayList<>();
        List<String> inverseProperties = new ArrayList<>();
        List<String> domains = new ArrayList<>();
        List<String> ranges = new ArrayList<>();

        try (QueryExecution qexec = QueryExecutionFactory.create(query, dataset)) {
            ResultSet results = qexec.execSelect();

            // Output results
            while (results.hasNext()) {
                QuerySolution sol = results.next();
                String domain = sol.contains("domain") ? getShortenUri(sol.get("domain").toString()) : null;
                String range = sol.contains("range") ? getShortenUri(sol.get("range").toString()) : null;
                String equivalentProperty = sol.contains("equivalentProperty") ? getShortenUri(sol.get("equivalentProperty").toString()) : null;
                String subProperty = sol.contains("subProperty") ? getShortenUri(sol.get("subProperty").toString()) : null;
                String inverseProperty = sol.contains("inverseProperty") ? getShortenUri(sol.get("inverseProperty").toString()) : null;

                if( domain!=null && !domains.contains(domain))
                    domains.add(domain);
                if( range!=null && !ranges.contains(range))
                    ranges.add(range);
                if( equivalentProperty!=null && !equivalentProperties.contains(equivalentProperty))
                    equivalentProperties.add(equivalentProperty);
                if( subProperty!=null && !subProperties.contains(subProperty))
                    subProperties.add(subProperty);
                if( inverseProperty!=null && !inverseProperties.contains(inverseProperty))
                    inverseProperties.add(inverseProperty);
            }
        } catch (Exception e) {
            return null;
        }


        return Arrays.asList(equivalentProperties, subProperties, inverseProperties, domains, ranges);
    }

    public List<Integer> executePredicateCountQuery(Dataset dataset, String predicate) {
        List<Integer> statistic = new ArrayList<>();

        predicate= new StringBuilder().append("<").append(getLongUri(predicate)).append(">").toString();
        var queryString= new ParameterizedSparqlString();
        queryString.setCommandText("SELECT (COUNT(*) AS ?count)" +
                "WHERE { " +
                "?subject ?predicate ?object . " +
                "FILTER (?predicate = " + predicate + ") " +  // Filter for the specific predicate
                "}");

        Query query = queryString.asQuery();

        try (QueryExecution qe = QueryExecutionFactory.create(query, dataset)) {
            ResultSet result = qe.execSelect();
            if (result.hasNext()) {
                QuerySolution sol = result.next();
                statistic.add(sol.getLiteral("count").getInt());
            }
        } catch (Exception e) {
            return null;
        }
        queryString= new ParameterizedSparqlString();
        queryString.setCommandText("SELECT (COUNT(*) AS ?count)" +
                "WHERE { " +
                "?subject ?predicate ?object . " +
                "}");

        query = queryString.asQuery();

        try (QueryExecution qe = QueryExecutionFactory.create(query, dataset)) {
            ResultSet result = qe.execSelect();
            if (result.hasNext()) {
                QuerySolution sol = result.next();
                statistic.add(sol.getLiteral("count").getInt());
            }
        } catch (Exception e) {
            return null;
        }

        return statistic;
    }

    public List<List<String>> executeCoOccurringPredicatesQuery(Dataset dataset, String predicate) {
        List<List<String>> result = new ArrayList<>();

        predicate= new StringBuilder().append("<").append(getLongUri(predicate)).append(">").toString();

        ParameterizedSparqlString queryString = new ParameterizedSparqlString();
        queryString.setCommandText(
                "SELECT ?p (COUNT(*) AS ?count) " +
                        "WHERE { " +
                        " ?s "+ predicate +" ?o ." +
                        " ?s ?p ?o2 . " +
                        "  FILTER(?p != " + predicate + ") " +       // Exclude the same predicate
                        "} " +
                        "GROUP BY ?p " +
                        "ORDER BY DESC(?count)"
        );

        Query query = queryString.asQuery();

        try (QueryExecution qe = QueryExecutionFactory.create(query, dataset)) {
            ResultSet results = qe.execSelect();
            while (results.hasNext()) {
                var list= new ArrayList<String>();
                QuerySolution solution = results.next();
                list.add(getShortenUri(solution.getResource("p").getURI()));
                list.add(solution.getLiteral("count").getString());

                result.add(list);
            }
        } catch (Exception e) {
            return null;
        }
        return result;
    }

    public List<List<String>> executeCountPredicatesQuery(Dataset dataset) {
        List<List<String>> result = new ArrayList<>();

        ParameterizedSparqlString queryString = new ParameterizedSparqlString();
        queryString.setCommandText(
                "SELECT ?p (COUNT(*) AS ?count) " +
                        "WHERE { ?s ?p ?o . } " +
                        "GROUP BY ?p " +
                        "ORDER BY DESC(?count)"
        );

        Query query = queryString.asQuery();

        try (QueryExecution qe = QueryExecutionFactory.create(query, dataset)) {
            ResultSet results = qe.execSelect();
            while (results.hasNext()) {
                var list= new ArrayList<String>();
                QuerySolution solution = results.next();
                list.add(getShortenUri(solution.getResource("p").getURI()));
                list.add(solution.getLiteral("count").getString());

                result.add(list);
            }
        } catch (Exception e) {
            return null;
        }
        return result;
    }

    public byte[] executeCountPredicatesQueryAndReturnByteArray(Dataset dataset) {
        List<List<String>> result = new ArrayList<>();

        ParameterizedSparqlString queryString = new ParameterizedSparqlString();
        queryString.setCommandText(
                "SELECT ?p (COUNT(*) AS ?count) " +
                        "WHERE { ?s ?p ?o . } " +
                        "GROUP BY ?p " +
                        "ORDER BY DESC(?count)"
        );

        // Prepare the query
        Query query = queryString.asQuery();

        // Create a new model to hold the RDF Data Cube data
        Model model = ModelFactory.createDefaultModel();
        Resource observationClass = model.createResource("http://www.w3.org/ns/sd#Observation");
        Property dimensionProperty = model.createProperty("http://purl.org/qb#dimension");
        Property measureProperty = model.createProperty("http://purl.org/qb#measure");
        Property typeProperty = RDF.type;

        try (QueryExecution qe = QueryExecutionFactory.create(query, dataset)) {
            ResultSet results = qe.execSelect();

            // Iterate through the results and add them to the model
            while (results.hasNext()) {
                QuerySolution solution = results.next();
                String predicateUri = solution.getResource("p").getURI();
                String count = solution.getLiteral("count").getString();

                Resource observation = model.createResource();

                observation.addProperty(typeProperty, observationClass);

                observation.addProperty(dimensionProperty, model.createResource(predicateUri));

                observation.addProperty(measureProperty, model.createTypedLiteral(count, XSDDatatype.XSDint));
            }
        } catch (Exception e) {
            return null;
        }

        // Serialize the model to JSON-LD format
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        RDFDataMgr.write(outputStream, model, Lang.JSONLD);

        // Return the byte array
        return outputStream.toByteArray();
    }

}
