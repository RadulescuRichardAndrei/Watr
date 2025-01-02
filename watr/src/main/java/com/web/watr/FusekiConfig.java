package com.web.watr;

import org.apache.jena.fuseki.main.FusekiServer;
import org.apache.jena.query.Dataset;
import org.apache.jena.tdb2.TDB2Factory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FusekiConfig {

    @Value("${fuseki.dataset.path}")
    private String directory;
    @Bean
    public FusekiServer fusekiServer() {

        Dataset dataset = TDB2Factory.connectDataset(directory);
        //TDB2.getContext().set(TDB2.symUnionDefaultGraph, true);

        // Start Fuseki server on a specific port and bind the dataset to a SPARQL endpoint
        FusekiServer server = FusekiServer.create()
                .add("/dataset", dataset) // SPARQL endpoint
                .port(3030)               // Change port if necessary
                .build();

        // Start the server
        server.start();
        return server;
    }
}
