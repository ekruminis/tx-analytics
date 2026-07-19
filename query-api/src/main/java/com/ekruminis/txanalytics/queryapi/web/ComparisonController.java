package com.ekruminis.txanalytics.queryapi.web;

import java.util.UUID;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ekruminis.txanalytics.queryapi.service.CompareService;

@RestController
public class ComparisonController {

    private final CompareService service;

    public ComparisonController(CompareService service) {
        this.service = service;
    }

    @GetMapping("/runs/compare")
    public ComparisonView compare(
            @RequestParam(required = false) UUID experiment,
            @RequestParam(required = false) String tfm,
            @RequestParam(required = false) String runs) {
        return service.compare(experiment, tfm, runs);
    }
}
