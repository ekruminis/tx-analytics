package com.ekruminis.txanalytics.queryapi.web;

import java.util.UUID;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import com.ekruminis.txanalytics.queryapi.service.TransactionService;

@RestController
public class TransactionController {

    private final TransactionService service;

    public TransactionController(TransactionService service) {
        this.service = service;
    }

    @GetMapping("/tx/{hash}")
    public TxLookupView lookup(@PathVariable String hash) {
        return service.lookup(hash);
    }

    @GetMapping("/runs/{runId}/tx/{hash}")
    public TxLookupView inRun(@PathVariable UUID runId, @PathVariable String hash) {
        return service.inRun(runId, hash);
    }
}
