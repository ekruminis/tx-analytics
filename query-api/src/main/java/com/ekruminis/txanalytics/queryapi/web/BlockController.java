package com.ekruminis.txanalytics.queryapi.web;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import com.ekruminis.txanalytics.queryapi.service.BlockService;

@RestController
public class BlockController {

    private final BlockService service;

    public BlockController(BlockService service) {
        this.service = service;
    }

    @GetMapping("/runs/{runId}/blocks/{height}")
    public BlockDetailView detail(@PathVariable UUID runId, @PathVariable int height) {
        return service.detail(runId, height);
    }

    @GetMapping("/blocks/{hash}")
    public BlockDetailView byHash(@PathVariable String hash) {
        return service.byHash(hash);
    }

    @GetMapping("/blocks/{hash}/transactions")
    public Page<BlockTxView> transactions(
            @PathVariable String hash,
            @PageableDefault(size = 100) Pageable pageable) {
        return service.transactions(hash, pageable);
    }
}
