package com.ekruminis.txanalytics.queryapi.service;

import java.util.UUID;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.ekruminis.txanalytics.queryapi.mongo.BlockResultDoc;
import com.ekruminis.txanalytics.queryapi.mongo.BlockResultMongoRepository;
import com.ekruminis.txanalytics.queryapi.mongo.TxResultMongoRepository;
import com.ekruminis.txanalytics.queryapi.postgres.Block;
import com.ekruminis.txanalytics.queryapi.postgres.BlockRepository;
import com.ekruminis.txanalytics.queryapi.postgres.SimulationRun;
import com.ekruminis.txanalytics.queryapi.postgres.SimulationRunRepository;
import com.ekruminis.txanalytics.queryapi.web.BlockDetailView;
import com.ekruminis.txanalytics.queryapi.web.BlockTxView;

@Service
public class BlockService {

    private final BlockRepository blockRepo;
    private final SimulationRunRepository runRepo;
    private final BlockResultMongoRepository blockResultRepo;
    private final TxResultMongoRepository txRepo;

    public BlockService(BlockRepository blockRepo,
                        SimulationRunRepository runRepo,
                        BlockResultMongoRepository blockResultRepo,
                        TxResultMongoRepository txRepo) {
        this.blockRepo = blockRepo;
        this.runRepo = runRepo;
        this.blockResultRepo = blockResultRepo;
        this.txRepo = txRepo;
    }

    @Cacheable(cacheNames = "blockDetail", key = "#runId + '_' + #height", sync = true)
    public BlockDetailView detail(UUID runId, int height) {
        Block block = blockRepo.findByRunIdAndHeight(runId, height)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "block not found: run " + runId + " height " + height));
        return assemble(block);
    }

    @Cacheable(cacheNames = "blockByHash", key = "#currentHash", sync = true)
    public BlockDetailView byHash(String currentHash) {
        Block block = blockRepo.findByCurrentHash(currentHash)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "block not found: " + currentHash));
        return assemble(block);
    }

    public Page<BlockTxView> transactions(String blockHash, Pageable pageable) {
        Block block = blockRepo.findByCurrentHash(blockHash)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "block not found: " + blockHash));
        return txRepo.findByRunIdAndHeight(block.getRunId().toString(), block.getHeight(), pageable)
                .map(BlockTxView::of);
    }

    private BlockDetailView assemble(Block block) {
        SimulationRun run = runRepo.findById(block.getRunId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                        "block references missing run " + block.getRunId()));
        BlockResultDoc outcomes = blockResultRepo
                .findById(block.getRunId() + ":" + block.getHeight())
                .orElse(null);
        return BlockDetailView.of(block, run, outcomes);
    }
}
