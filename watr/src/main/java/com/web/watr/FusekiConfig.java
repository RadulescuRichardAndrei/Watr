package com.web.watr;

import org.apache.jena.fuseki.main.FusekiServer;
import org.apache.jena.query.Dataset;
import org.apache.jena.tdb2.TDB2Factory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FusekiConfig {

    @Bean
    public FusekiServer fusekiServer() {
        // Create an in-memory or persistent dataset (TDB2 in this case)
        String directory="D:/facultateM/Web/Proiect_Watr/Fuseki";
        Dataset dataset = TDB2Factory.connectDataset(directory);

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
