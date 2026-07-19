package com.ekruminis.txanalytics.queryapi.web;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ekruminis.txanalytics.queryapi.service.RunService;

@RestController
@RequestMapping("/runs")
public class RunController {

    private final RunService service;

    public RunController(RunService service) {
        this.service = service;
    }

    @GetMapping
    public Page<RunView> list(
            @RequestParam(required = false) UUID experiment,
            @RequestParam(required = false) String tfm,
            @PageableDefault(size = 50) Pageable pageable) {
        return service.list(experiment, tfm, pageable);
    }

    @GetMapping("/{id}")
    public RunView get(@PathVariable UUID id) {
        return service.get(id);
    }

    @GetMapping("/{id}/blocks")
    public Page<BlockView> blocks(
            @PathVariable UUID id,
            @PageableDefault(size = 100) Pageable pageable) {
        return service.blocks(id, pageable);
    }
}
