package com.ekruminis.txanalytics.queryapi.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import com.ekruminis.txanalytics.queryapi.postgres.Block;
import com.ekruminis.txanalytics.queryapi.postgres.BlockRepository;
import com.ekruminis.txanalytics.queryapi.postgres.Experiment;
import com.ekruminis.txanalytics.queryapi.postgres.ExperimentRepository;
import com.ekruminis.txanalytics.queryapi.postgres.SimulationRun;
import com.ekruminis.txanalytics.queryapi.postgres.SimulationRunRepository;
import com.ekruminis.txanalytics.queryapi.web.BlockView;
import com.ekruminis.txanalytics.queryapi.web.RunView;

@ExtendWith(MockitoExtension.class)
class RunServiceTest {

    @Mock SimulationRunRepository runRepo;
    @Mock ExperimentRepository experimentRepo;
    @Mock BlockRepository blockRepo;
    @InjectMocks RunService service;

    @Test
    void getReturnsNotFoundWhenRunMissing() {
        UUID id = UUID.randomUUID();
        when(runRepo.findById(id)).thenReturn(Optional.empty());

        assertStatus(() -> service.get(id), HttpStatus.NOT_FOUND);
    }

    @Test
    void getReturnsInternalErrorWhenRunReferencesMissingExperiment() {
        UUID id = UUID.randomUUID();
        UUID expId = UUID.randomUUID();
        SimulationRun run = mock(SimulationRun.class);
        when(run.getExperimentId()).thenReturn(expId);
        when(runRepo.findById(id)).thenReturn(Optional.of(run));
        when(experimentRepo.findById(expId)).thenReturn(Optional.empty());

        assertStatus(() -> service.get(id), HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Test
    void getMapsRunAndExperimentToView() {
        UUID id = UUID.randomUUID();
        UUID expId = UUID.randomUUID();
        SimulationRun run = mock(SimulationRun.class);
        when(run.getId()).thenReturn(id);
        when(run.getExperimentId()).thenReturn(expId);
        when(run.getTfmType()).thenReturn("eip1559");
        when(runRepo.findById(id)).thenReturn(Optional.of(run));
        when(experimentRepo.findById(expId)).thenReturn(Optional.of(mock(Experiment.class)));

        RunView view = service.get(id);

        assertThat(view.id()).isEqualTo(id);
        assertThat(view.tfm()).isEqualTo("eip1559");
    }

    @Test
    void blocksReturnsNotFoundWhenRunMissing() {
        UUID runId = UUID.randomUUID();
        when(runRepo.existsById(runId)).thenReturn(false);

        assertStatus(() -> service.blocks(runId, PageRequest.of(0, 10)), HttpStatus.NOT_FOUND);
    }

    @Test
    void blocksPagesThroughTheRunsBlocks() {
        UUID runId = UUID.randomUUID();
        when(runRepo.existsById(runId)).thenReturn(true);
        Page<Block> page = new PageImpl<>(List.of(mock(Block.class)));
        when(blockRepo.findByRunIdOrderByHeightAsc(eq(runId), any())).thenReturn(page);

        Page<BlockView> result = service.blocks(runId, PageRequest.of(0, 10));

        assertThat(result.getContent()).hasSize(1);
    }

    private static void assertStatus(org.assertj.core.api.ThrowableAssert.ThrowingCallable call, HttpStatus expected) {
        assertThatThrownBy(call)
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(expected);
    }
}
