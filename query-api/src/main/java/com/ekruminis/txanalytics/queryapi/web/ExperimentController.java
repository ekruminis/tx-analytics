package com.ekruminis.txanalytics.queryapi.web;

import java.util.List;
import java.util.UUID;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ekruminis.txanalytics.queryapi.service.ExperimentService;

@RestController
@RequestMapping("/experiments")
public class ExperimentController {

    private final ExperimentService service;

    public ExperimentController(ExperimentService service) {
        this.service = service;
    }

    @GetMapping
    public List<ExperimentView> list() {
        return service.list();
    }

    @GetMapping("/{id}")
    public ExperimentDetailView get(@PathVariable UUID id) {
        return service.get(id);
    }

    @GetMapping("/{id}/miners")
    public List<MinerView> miners(@PathVariable UUID id) {
        return service.miners(id);
    }
}
