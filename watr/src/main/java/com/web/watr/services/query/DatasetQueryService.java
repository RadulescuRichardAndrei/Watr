package com.web.watr.services.query;

import com.web.watr.beans.FilterBean;
import com.web.watr.utils.MethodUtils;
import org.apache.jena.arq.querybuilder.ExprFactory;
import org.apache.jena.arq.querybuilder.SelectBuilder;
import org.apache.jena.arq.querybuilder.WhereBuilder;
import org.apache.jena.atlas.json.JsonArray;
import org.apache.jena.atlas.json.JsonObject;
import org.apache.jena.query.*;
import org.apache.jena.rdf.model.*;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.sparql.expr.ExprVar;
import org.apache.jena.sparql.expr.NodeValue;
import org.apache.jena.sparql.syntax.ElementGroup;
import org.apache.jena.sparql.syntax.ElementService;

import java.io.ByteArrayOutputStream;
import java.util.*;
import java.util.stream.Collectors;

public class DatasetQueryService extends GenericQueryService {

    private static String dbpediaEndpoint = "http://dbpedia.org/sparql";
    private static String wikidataEndpoint = "http://query.wikidata.org/sparql";


    public DatasetQueryService() {
        super();
    }
    public List<List<String>> executePagedSelectQuery(Dataset dataset, int page, int pageSize) {
        int offset= page* pageSize;

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
                    jsonNode.put("type","subject");
                    nodes.add(jsonNode);
                    nodeSet.add(subject);
                }
                if (!nodeSet.contains(object)) {
                    JsonObject jsonNode= new JsonObject();
                    jsonNode.put("id",object);
                    jsonNode.put("label", getShortenUri(object));
                    jsonNode.put("type","object");
                    if (solution.get("object").isResource())
                        jsonNode.put("type_object","resource");
                    else if (solution.get("object").isLiteral())
                        jsonNode.put("type_object","literal");
                    else
                        jsonNode.put("type_object","null");
                    nodes.add(jsonNode);
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
        body.addElement(new ElementService(dbpediaEndpoint, query.getQueryPattern()));
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

    public List<List<String>> getDistinctElementsFromTriplets(Dataset dataset) {
        var resultLists = new ArrayList<List<String>>();
        var subjects = new HashSet<String>();
        var predicates = new HashSet<String>();
        var objects = new HashSet<String>();

        var queryString = new ParameterizedSparqlString();

        queryString.setCommandText(
                "SELECT DISTINCT ?s ?p ?o " +
                        "WHERE { " +
                        "  { ?s ?p ?o } " +
                        "  UNION " +
                        "  { GRAPH ?g { ?s ?p ?o } } " +
                        "}"
        );

        Query query = queryString.asQuery();

        try (QueryExecution qe = QueryExecutionFactory.create(query, dataset)) {
            ResultSet resultSet = qe.execSelect();

            while (resultSet.hasNext()) {
                QuerySolution sol = resultSet.next();
                var sub= sol.get("?s");
                var pred= sol.get("?p");
                var obj= sol.get("?o");

                if (sub.isResource() && !sub.isAnon() && !subjects.contains(getShortenUri(sub.asResource().getURI())) ) {
                    subjects.add(getShortenUri(sub.asResource().getURI()));
                }

                if (pred.isResource() && !predicates.contains(getShortenUri(pred.asResource().getURI())) ) {
                    predicates.add(getShortenUri(pred.asResource().getURI()));
                }

                if (obj.isResource() && !obj.isAnon() && !objects.contains(getShortenUri(obj.asResource().getURI())) ) {
                    objects.add(getShortenUri(obj.asResource().getURI()));
                }
            }
        } catch (Exception e) {
            return null;
        }


        resultLists.add(new ArrayList<>(subjects));
        resultLists.add(new ArrayList<>(predicates));
        resultLists.add(new ArrayList<>(objects));

        return resultLists;
    }

    public List<AbstractMap.SimpleEntry<String, String>> findEquivalentPropertiesDBpedia(List<String> firstList, List<String> secondList) {
        var matches = new ArrayList<AbstractMap.SimpleEntry<String,String>>();

        var firstString= firstList.stream().map(this::getLongUri).map(uri -> "<" + uri + ">").collect(Collectors.joining(" "));
        var secondString= secondList.stream().map(this::getLongUri).map(uri -> "<" + uri + ">").collect(Collectors.joining(" "));

        String queryString = "PREFIX owl: <http://www.w3.org/2002/07/owl#> " +
                "SELECT DISTINCT ?prop1 ?prop2 " +
                "WHERE { " +
                "  SERVICE <" + dbpediaEndpoint + "> { " +
                "    VALUES ?prop1 { " + firstString + " } " +
                "    VALUES ?prop2 { " + secondString + " } " +
                "    { ?prop1 owl:equivalentProperty ?prop2 } " +
                "    UNION " +
                "    { ?prop2 owl:equivalentProperty ?prop1 } " +
                "  } " +
                "}";

        Query query= QueryFactory.create(queryString);
        Model model= ModelFactory.createDefaultModel();
        try (QueryExecution qexec = QueryExecutionFactory.create(query, model)) {
            ResultSet results = qexec.execSelect();
            while (results.hasNext()) {
                QuerySolution sol = results.next();
                String prop1 = sol.getResource("prop1").getURI();
                String prop2 = sol.getResource("prop2").getURI();
                matches.add(new AbstractMap.SimpleEntry<>(prop1, prop2){});
            }
        }catch (Exception e){
            return null;
        }

        return matches;
    }

    public List<AbstractMap.SimpleEntry<String, String>> findEquivalentPropertiesWikidata(List<String> firstList, List<String> secondList) {
        var matches = new ArrayList<AbstractMap.SimpleEntry<String, String>>();

        // Convert lists into SPARQL-friendly format
        var firstString = firstList.stream().map(this::getLongUri).map(uri -> "<" + uri + ">").collect(Collectors.joining(" "));
        var secondString = secondList.stream().map(this::getLongUri).map(uri -> "<" + uri + ">").collect(Collectors.joining(" "));

        // SPARQL query with SERVICE and rdf prefix
        String queryString = "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> " +
                "SELECT DISTINCT ?prop1 ?prop2 " +
                "WHERE { " +
                "  SERVICE <" + wikidataEndpoint + "> { " +
                "    VALUES ?prop1 { " + firstString + " } " +
                "    VALUES ?prop2 { " + secondString + " } " +
                "    { ?prop1 rdf:equivalentProperty ?prop2 } " +
                "    UNION " +
                "    { ?prop2 rdf:equivalentProperty ?prop1 } " +
                "  } " +
                "}";

        Query query = QueryFactory.create(queryString);
        Model model= ModelFactory.createDefaultModel();
        try (QueryExecution qexec = QueryExecutionFactory.create(query,model)) {
            ResultSet results = qexec.execSelect();
            while (results.hasNext()) {
                QuerySolution sol = results.next();
                String prop1 = sol.getResource("prop1").getURI();
                String prop2 = sol.getResource("prop2").getURI();
                matches.add(new AbstractMap.SimpleEntry<>(prop1, prop2));
            }
        } catch (Exception e) {
            return null;
        }

        return matches;
    }

}
