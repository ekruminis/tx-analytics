package com.ekruminis.txanalytics.queryapi.service;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.ekruminis.txanalytics.queryapi.postgres.BlockRepository;
import com.ekruminis.txanalytics.queryapi.postgres.Experiment;
import com.ekruminis.txanalytics.queryapi.postgres.ExperimentRepository;
import com.ekruminis.txanalytics.queryapi.postgres.SimulationRun;
import com.ekruminis.txanalytics.queryapi.postgres.SimulationRunRepository;
import com.ekruminis.txanalytics.queryapi.web.BlockView;
import com.ekruminis.txanalytics.queryapi.web.RunView;

@Service
public class RunService {

    private final SimulationRunRepository runRepo;
    private final ExperimentRepository experimentRepo;
    private final BlockRepository blockRepo;

    public RunService(SimulationRunRepository runRepo,
                      ExperimentRepository experimentRepo,
                      BlockRepository blockRepo) {
        this.runRepo = runRepo;
        this.experimentRepo = experimentRepo;
        this.blockRepo = blockRepo;
    }

    public Page<RunView> list(UUID experimentId, String tfm, Pageable pageable) {
        Page<SimulationRun> runs = runRepo.search(experimentId, tfm, pageable);
        List<UUID> expIds = runs.stream().map(SimulationRun::getExperimentId).distinct().toList();
        Map<UUID, Experiment> experiments = experimentRepo.findAllById(expIds).stream()
                .collect(Collectors.toMap(Experiment::getId, Function.identity()));
        return runs.map(r -> RunView.of(r, experiments.get(r.getExperimentId())));
    }

    public RunView get(UUID id) {
        SimulationRun run = runRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "run not found: " + id));
        Experiment experiment = experimentRepo.findById(run.getExperimentId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                        "run " + id + " references missing experiment " + run.getExperimentId()));
        return RunView.of(run, experiment);
    }

    public Page<BlockView> blocks(UUID runId, Pageable pageable) {
        if (!runRepo.existsById(runId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "run not found: " + runId);
        }
        return blockRepo.findByRunIdOrderByHeightAsc(runId, pageable).map(BlockView::of);
    }
}
