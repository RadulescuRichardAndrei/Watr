package com.web.watr.controllers;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.concurrent.CompletableFuture;

@Controller
public class HomePageController {

    @GetMapping("/")
    @Async
    public CompletableFuture<String> getIndex(){
        return CompletableFuture.completedFuture("index");
    }
}
