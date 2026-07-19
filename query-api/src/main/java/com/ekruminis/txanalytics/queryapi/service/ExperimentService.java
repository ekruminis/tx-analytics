package com.ekruminis.txanalytics.queryapi.service;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.ekruminis.txanalytics.queryapi.postgres.Experiment;
import com.ekruminis.txanalytics.queryapi.postgres.ExperimentRepository;
import com.ekruminis.txanalytics.queryapi.postgres.MinerRepository;
import com.ekruminis.txanalytics.queryapi.postgres.SimulationRunRepository;
import com.ekruminis.txanalytics.queryapi.web.ExperimentDetailView;
import com.ekruminis.txanalytics.queryapi.web.ExperimentView;
import com.ekruminis.txanalytics.queryapi.web.MinerView;
import com.ekruminis.txanalytics.queryapi.web.RunView;

@Service
public class ExperimentService {

    private final ExperimentRepository experimentRepo;
    private final SimulationRunRepository runRepo;
    private final MinerRepository minerRepo;

    public ExperimentService(ExperimentRepository experimentRepo,
                             SimulationRunRepository runRepo,
                             MinerRepository minerRepo) {
        this.experimentRepo = experimentRepo;
        this.runRepo = runRepo;
        this.minerRepo = minerRepo;
    }

    public List<ExperimentView> list() {
        return experimentRepo.findAllByOrderByStartedAtDesc().stream()
                .map(e -> ExperimentView.of(e, runRepo.countByExperimentId(e.getId())))
                .toList();
    }

    public ExperimentDetailView get(UUID id) {
        Experiment experiment = experimentRepo.findById(id)
                .orElseThrow(() -> notFound(id));
        List<RunView> runs = runRepo.findByExperimentIdOrderByTfmType(id).stream()
                .map(r -> RunView.of(r, experiment))
                .toList();
        List<MinerView> miners = minerRepo.findByExperimentIdOrderByMinerIdAsc(id).stream()
                .map(MinerView::of)
                .toList();
        ExperimentView view = ExperimentView.of(experiment, runs.size());
        return new ExperimentDetailView(view, runs, miners);
    }

    public List<MinerView> miners(UUID experimentId) {
        if (!experimentRepo.existsById(experimentId)) {
            throw notFound(experimentId);
        }
        return minerRepo.findByExperimentIdOrderByMinerIdAsc(experimentId).stream()
                .map(MinerView::of)
                .toList();
    }

    Experiment requireExperiment(UUID id) {
        return experimentRepo.findById(id).orElseThrow(() -> notFound(id));
    }

    private static ResponseStatusException notFound(UUID id) {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "experiment not found: " + id);
    }
}
