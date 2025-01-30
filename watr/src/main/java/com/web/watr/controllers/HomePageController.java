package com.web.watr.controllers;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

import java.util.concurrent.CompletableFuture;

@Controller
public class HomePageController {

    @Operation(summary = "Get Index Page", description = "Returns the home page view asynchronously.")
    @ApiResponse(responseCode = "200", description = "Home page loaded successfully")
    @GetMapping("/")
    @Async
    public CompletableFuture<String> getIndex(){
        return CompletableFuture.completedFuture("index");
    }
}
