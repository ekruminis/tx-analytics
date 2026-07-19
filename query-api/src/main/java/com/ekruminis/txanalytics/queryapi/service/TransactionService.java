package com.ekruminis.txanalytics.queryapi.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.ekruminis.txanalytics.queryapi.mongo.TxResultDoc;
import com.ekruminis.txanalytics.queryapi.mongo.TxResultMongoRepository;
import com.ekruminis.txanalytics.queryapi.postgres.Block;
import com.ekruminis.txanalytics.queryapi.postgres.BlockRepository;
import com.ekruminis.txanalytics.queryapi.postgres.Experiment;
import com.ekruminis.txanalytics.queryapi.postgres.ExperimentRepository;
import com.ekruminis.txanalytics.queryapi.postgres.SimulationRun;
import com.ekruminis.txanalytics.queryapi.postgres.SimulationRunRepository;
import com.ekruminis.txanalytics.queryapi.web.TxLookupView;
import com.ekruminis.txanalytics.queryapi.web.TxOccurrenceView;

@Service
public class TransactionService {

    private final TxResultMongoRepository txRepo;
    private final SimulationRunRepository runRepo;
    private final ExperimentRepository experimentRepo;
    private final BlockRepository blockRepo;

    public TransactionService(TxResultMongoRepository txRepo,
                              SimulationRunRepository runRepo,
                              ExperimentRepository experimentRepo,
                              BlockRepository blockRepo) {
        this.txRepo = txRepo;
        this.runRepo = runRepo;
        this.experimentRepo = experimentRepo;
        this.blockRepo = blockRepo;
    }

    @Cacheable(cacheNames = "txLookup", key = "#txHash", sync = true)
    public TxLookupView lookup(String txHash) {
        List<TxResultDoc> docs = txRepo.findByTxHashOrderByRunIdAscHeightAsc(txHash);
        if (docs.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "tx not found: " + txHash);
        }
        return new TxLookupView(txHash, enrich(docs));
    }

    @Cacheable(cacheNames = "txInRun", key = "#runId + '_' + #txHash", sync = true)
    public TxLookupView inRun(UUID runId, String txHash) {
        List<TxResultDoc> docs = txRepo.findByRunIdAndTxHashOrderByHeightAsc(runId.toString(), txHash);
        if (docs.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "tx " + txHash + " not found in run " + runId);
        }
        return new TxLookupView(txHash, enrich(docs));
    }

    private List<TxOccurrenceView> enrich(List<TxResultDoc> docs) {
        Set<UUID> runIds = docs.stream().map(d -> UUID.fromString(d.getRunId())).collect(Collectors.toSet());
        Map<UUID, SimulationRun> runs = runRepo.findAllById(runIds).stream()
                .collect(Collectors.toMap(SimulationRun::getId, Function.identity()));
        Set<UUID> expIds = runs.values().stream().map(SimulationRun::getExperimentId).collect(Collectors.toSet());
        Map<UUID, Experiment> experiments = experimentRepo.findAllById(expIds).stream()
                .collect(Collectors.toMap(Experiment::getId, Function.identity()));

        List<TxOccurrenceView> out = new ArrayList<>(docs.size());
        for (TxResultDoc d : docs) {
            UUID runId = UUID.fromString(d.getRunId());
            SimulationRun run = runs.get(runId);
            if (run == null) {
                continue;
            }
            Experiment exp = experiments.get(run.getExperimentId());
            String blockHash = blockRepo.findByRunIdAndHeight(runId, d.getHeight())
                    .map(Block::getCurrentHash).orElse(null);
            out.add(new TxOccurrenceView(
                    runId, run.getTfmType(), run.getExperimentId(),
                    exp == null ? 0 : exp.getSeed(),
                    exp == null ? 0 : exp.getNumMiners(),
                    exp == null ? null : exp.getDatasetHash(),
                    run.getMechanismParams(),
                    d.getHeight(), blockHash,
                    d.getOfferedFee(), d.getPaidFee(), d.isConfirmed(), d.getBurned()));
        }
        return out;
    }
}
