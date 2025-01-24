package com.web.watr.services.query;

import org.apache.jena.query.Dataset;
import org.apache.jena.riot.RDFDataMgr;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public class GenericQueryService {
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

    public GenericQueryService() {
        NAMESPACE_PREFIXES.sort(Comparator.comparingInt((Map.Entry<String, String> entry) -> entry.getValue().length()).reversed());
    }
    protected String getShortenUri(String uri) {
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

    protected String getLongUri(String uri){
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
    protected String getPrefix(String uri) {
        for (Map.Entry<String, String> entry : NAMESPACE_PREFIXES) {
            if (uri.startsWith(entry.getKey())) {
                return entry.getValue() + ": " + entry.getKey();
            }
        }
        return null;
    }
    protected String getSuffix(String uri){
        for (Map.Entry<String, String> entry : NAMESPACE_PREFIXES) {
            if (uri.startsWith(entry.getKey())) {
                return uri.substring(entry.getKey().length());
            }
        }
        return uri;
    }
    public List<String> getAllDetails(String uri){
        String longUri = getLongUri(uri);
        if (longUri == null) return  null;
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
}
