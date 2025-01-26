package com.web.watr.services.query;

import com.web.watr.utils.MethodUtils;
import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.query.*;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.vocabulary.RDF;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

public class StatisticQueryService extends GenericQueryService{


    public StatisticQueryService() {
        super();
    }
    public List<List<String>> executeCountQuery(Dataset dataset, MethodUtils.TRIPLE_TYPE countType) {
        List<List<String>> result = new ArrayList<>();

        String selectField = "?s";

        if (countType == MethodUtils.TRIPLE_TYPE.PREDICATE) {
            selectField = "?p";
        } else if (countType == MethodUtils.TRIPLE_TYPE.OBJECT) {
            selectField = "?o";
        }

        var queryString = new ParameterizedSparqlString();

        if (countType == MethodUtils.TRIPLE_TYPE.OBJECT) {
            queryString.setCommandText(
                    "SELECT (STR(?o2) AS ?o) (COUNT(*) AS ?count) " +
                            "WHERE { { ?s ?p ?o2 . } " +
                            " UNION " +
                            " { GRAPH ?g { ?s ?p ?o2 . } } }" +
                            "GROUP BY ?o2 " +
                            "ORDER BY DESC(?count) " +
                            "LIMIT 10"
            );
        }else {
            queryString.setCommandText(
                    "SELECT " + selectField + " (COUNT(*) AS ?count) " +
                            "WHERE { { ?s ?p ?o . } " +
                            " UNION " +
                            " { GRAPH ?g { ?s ?p ?o . } } }" +
                            "GROUP BY " + selectField + " " +
                            "ORDER BY DESC(?count) " +
                            "LIMIT 10"
            );
        }

        Query query = queryString.asQuery();

        try (QueryExecution qe = QueryExecutionFactory.create(query, dataset)) {
            ResultSet results = qe.execSelect();
            while (results.hasNext()) {
                var list= new ArrayList<String>();
                QuerySolution solution = results.next();
                if (countType != MethodUtils.TRIPLE_TYPE.OBJECT)
                    list.add(getShortenUri(solution.getResource(selectField).getURI()));
                else
                    list.add(getShortenUri(solution.get(selectField).toString()));

                list.add(solution.getLiteral("count").getString());

                result.add(list);
            }
        } catch (Exception e) {
            return null;
        }
        return result;
    }

    public byte[] executeCountQueryAndReturnByteArray(Dataset dataset, MethodUtils.TRIPLE_TYPE countType) {
        List<List<String>> result = new ArrayList<>();

        String selectField = "?s";
        if (countType == MethodUtils.TRIPLE_TYPE.PREDICATE) {
            selectField = "?p";
        } else if (countType == MethodUtils.TRIPLE_TYPE.OBJECT) {
            selectField = "?o";
        }

        ParameterizedSparqlString queryString = new ParameterizedSparqlString();
        queryString.setCommandText(
                "SELECT " + selectField + " (COUNT(*) AS ?count) " +
                        "WHERE { { ?s ?p ?o . } " +
                        " UNION " +
                        " { GRAPH ?g { ?s ?p ?o . } } }" +
                        "GROUP BY " + selectField + " " +
                        "ORDER BY DESC(?count) " +
                        "LIMIT 10"
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

    public List<List<String>> executeCoOccurringPredicatesQuery(Dataset dataset, String predicate) {
        List<List<String>> result = new ArrayList<>();

        predicate= new StringBuilder().append("<").append(getLongUri(predicate)).append(">").toString();

        ParameterizedSparqlString queryString = new ParameterizedSparqlString();
        queryString.setCommandText(
                "SELECT ?p (COUNT(*) AS ?count) " +
                        "WHERE { " +
                        "  { " +
                        "    ?s " + predicate + " ?o . " +
                        "    ?s ?p ?o2 . " +
                        "    FILTER(?p != " + predicate + ") " + // Exclude the same predicate
                        "  } " +
                        "  UNION { " +
                        "    GRAPH ?g { " +
                        "      ?s " + predicate + " ?o . " +
                        "      ?s ?p ?o2 . " +
                        "      FILTER(?p != " + predicate + ") " + // Exclude the same predicate
                        "    } } } " +
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

    public List<List<String>> getPredicateObjectCountsForSubject(Dataset dataset, String subject) {
        List<List<String>> predicateCounts = new ArrayList<>();

        subject = new StringBuilder().append("<").append(getLongUri(subject)).append(">").toString();
        var queryString = new ParameterizedSparqlString();

        queryString.setCommandText("SELECT ?predicate (COUNT(?object) AS ?count) " +
                "WHERE { " +
                subject + " ?predicate ?object . " +
                "} GROUP BY ?predicate");

        Query query = queryString.asQuery();

        try (QueryExecution qe = QueryExecutionFactory.create(query, dataset)) {
            ResultSet result = qe.execSelect();

            while (result.hasNext()) {
                QuerySolution sol = result.next();
                String predicate = sol.getResource("predicate").toString();
                int count = sol.getLiteral("count").getInt();

                List<String> entry = new ArrayList<>();
                entry.add(predicate);
                entry.add(String.valueOf(count));

                predicateCounts.add(entry);
            }
        } catch (Exception e) {
            return null;
        }

        return predicateCounts;
    }

    public List<List<String>> getPredicateSubjectCountsForObject(Dataset dataset, String object) {
        List<List<String>> predicateCounts = new ArrayList<>();

        object = new StringBuilder().append("<").append(getLongUri(object)).append(">").toString();
        var queryString = new ParameterizedSparqlString();

        queryString.setCommandText("SELECT ?predicate (COUNT(?subject) AS ?count) " +
                "WHERE { " +
                "?subject ?predicate " + object + " . " +
                "} GROUP BY ?predicate");

        Query query = queryString.asQuery();

        try (QueryExecution qe = QueryExecutionFactory.create(query, dataset)) {
            ResultSet result = qe.execSelect();

            while (result.hasNext()) {
                QuerySolution sol = result.next();
                String predicate = sol.getResource("predicate").toString();
                int count = sol.getLiteral("count").getInt();

                List<String> entry = new ArrayList<>();
                entry.add(predicate);
                entry.add(String.valueOf(count));

                predicateCounts.add(entry);
            }
        } catch (Exception e) {
            return null;
        }

        return predicateCounts;
    }

    public List<List<String>> extractInfoForObject(Dataset dataset, String object, String objectType) {
        if (objectType.equals("resource"))
            object = new StringBuilder().append("<").append(getLongUri(object)).append(">").toString();
        if (object.contains("^^") && objectType.equals("literal")) {
            String[] parts = object.split("\\^\\^");
            object = parts[0] + "^^" + '<' + getLongUri(parts[1]) + '>';
            }
        else if (objectType.equals("literal"))
            object = "\"" + object + "\"";
        List<List<String>> literalInfo = new ArrayList<>();

        String queryText= null;
        if(objectType.equals("resource")){
            queryText= "SELECT ?p (COUNT(?o) as ?count) ?lang "+
                    "WHERE { { ?s ?p ?o .} " +
                    "UNION { GRAPH ?g {?s ?p ?o} }" +
                    "FILTER(?o = " + object + ") " +
                    "OPTIONAL { BIND(LANG(?o) AS ?lang)} }" +
                    "GROUP BY ?p ?lang";
        }else if (objectType.equals("literal")){
            queryText = "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>" +
                    "SELECT ?p ?datatype " +
                    //"(IF(DATATYPE(?o) IN (xsd:decimal, xsd:float, xsd:integer), MIN(?o), '') AS ?min) " +
                    //"(IF(DATATYPE(?o) IN (xsd:decimal, xsd:float, xsd:integer), MAX(?o), '') AS ?max) " +
                    //"(IF(DATATYPE(?o) IN (xsd:decimal, xsd:float, xsd:integer), AVG(?o), '') AS ?avg) " +
                    "WHERE { " +
                    "{ ?s ?p ?o . } " +
                    "UNION { GRAPH ?g { ?s ?p ?o } } " +
                    "FILTER(?o = " + object + ") " +
                    "} " +
                    "GROUP BY ?p ?datatype ?o";
        }
        var queryString = new ParameterizedSparqlString(queryText);

        Query query = queryString.asQuery();

        try (QueryExecution qe = QueryExecutionFactory.create(query, dataset)) {
            ResultSet result = qe.execSelect();

            while (result.hasNext()) {
                QuerySolution sol = result.next();
                List<String> entry = new ArrayList<>();

                String predicate = getShortenUri(sol.getResource("p").toString());
                if (objectType.equals("resource")){
                    entry.add("Resource");
                    entry.add(predicate);

                    var count= sol.getLiteral("count").getInt();
                    String languageTag = sol.contains("lang") ? sol.getLiteral("lang").getString() : "null";
                    entry.add(String.valueOf(count));
                    entry.add(languageTag);

                } else if (objectType.equals("literal")){
                    entry.add("Literal");
                    entry.add(predicate);

                    String datatype = sol.contains("datatype") ? sol.getResource("datatype").toString() : "null";
                    String min = sol.contains("min") && !sol.getLiteral("min").getString().isEmpty() ? sol.getLiteral("min").getString() : "null";
                    String max = sol.contains("max") && !sol.getLiteral("max").getString().isEmpty()  ? sol.getLiteral("max").getString() : "null";
                    String avg = sol.contains("avg") && !sol.getLiteral("avg").getString().isEmpty()  ? sol.getLiteral("avg").getString() : "null";

                    entry.add(datatype);
                    entry.add(min);
                    entry.add(max);
                    entry.add(avg);
                }
                literalInfo.add(entry);
            }
        } catch (Exception e) {
            return null;
        }

        return literalInfo;
    }


    public Integer getOutDegreeSubject(Dataset dataset, String subject) {
        int outDegree = 0;

        subject = new StringBuilder().append("<").append(getLongUri(subject)).append(">").toString();
        var queryString = new ParameterizedSparqlString();

        queryString.setCommandText("SELECT (COUNT(*) AS ?count) " +
                "WHERE { {" +
                subject + " ?predicate ?object . }" +
                " UNION " +
                " { GRAPH ?g { " + subject + " ?predicate ?object . } }" +
                "}");

        Query query = queryString.asQuery();

        try (QueryExecution qe = QueryExecutionFactory.create(query, dataset)) {
            ResultSet result = qe.execSelect();

            if (result.hasNext()) {
                QuerySolution sol = result.next();
                outDegree = sol.getLiteral("count").getInt();
            }
        } catch (Exception e) {
            return null;
        }

        return outDegree;
    }

    public Integer getInDegreeObject(Dataset dataset, String object, String objectType) {
        int inDegree = 0;
        if (objectType.equals("resource"))
            object = new StringBuilder().append("<").append(getLongUri(object)).append(">").toString();
        if (object.contains("^^") && objectType.equals("literal")) {
            String[] parts = object.split("\\^\\^");
            object = parts[0] + "^^" + '<' + getLongUri(parts[1]) + '>';
        } else if (objectType.equals("literal"))
            object = "\"" + object + "\"";

        var queryString = new ParameterizedSparqlString();

        queryString.setCommandText("SELECT (COUNT(*) AS ?count) " +
                "WHERE { { ?subject ?predicate ?object . }" +
                " UNION " +
                " { GRAPH ?g { ?subject ?predicate ?object . } } "+
                "FILTER(?object = " + object + ")"+
                "}");

        Query query = queryString.asQuery();

        try (QueryExecution qe = QueryExecutionFactory.create(query, dataset)) {
            ResultSet result = qe.execSelect();

            if (result.hasNext()) {
                QuerySolution sol = result.next();
                inDegree = sol.getLiteral("count").getInt();
            }
        } catch (Exception e) {
            return null;
        }

        return inDegree;
    }



}
