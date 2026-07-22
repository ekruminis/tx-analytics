package com.ekruminis.txanalytics.queryapi.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import com.ekruminis.txanalytics.queryapi.postgres.Experiment;
import com.ekruminis.txanalytics.queryapi.postgres.ExperimentRepository;
import com.ekruminis.txanalytics.queryapi.postgres.Miner;
import com.ekruminis.txanalytics.queryapi.postgres.MinerRepository;
import com.ekruminis.txanalytics.queryapi.postgres.SimulationRun;
import com.ekruminis.txanalytics.queryapi.postgres.SimulationRunRepository;
import com.ekruminis.txanalytics.queryapi.web.ExperimentDetailView;
import com.ekruminis.txanalytics.queryapi.web.ExperimentView;
import com.ekruminis.txanalytics.queryapi.web.MinerView;

@ExtendWith(MockitoExtension.class)
class ExperimentServiceTest {

    @Mock ExperimentRepository experimentRepo;
    @Mock SimulationRunRepository runRepo;
    @Mock MinerRepository minerRepo;
    @InjectMocks ExperimentService service;

    @Test
    void getReturnsNotFoundForUnknownExperiment() {
        UUID id = UUID.randomUUID();
        when(experimentRepo.findById(id)).thenReturn(Optional.empty());

        assertStatus(() -> service.get(id), HttpStatus.NOT_FOUND);
    }

    @Test
    void getAssemblesExperimentWithRunsAndMiners() {
        UUID id = UUID.randomUUID();
        when(experimentRepo.findById(id)).thenReturn(Optional.of(mock(Experiment.class)));
        when(runRepo.findByExperimentIdOrderByTfmType(id)).thenReturn(List.of(mock(SimulationRun.class)));
        when(minerRepo.findByExperimentIdOrderByMinerIdAsc(id)).thenReturn(List.of(mock(Miner.class)));

        ExperimentDetailView view = service.get(id);

        assertThat(view.runs()).hasSize(1);
        assertThat(view.miners()).hasSize(1);
    }

    @Test
    void minersReturnsNotFoundWhenExperimentAbsent() {
        UUID id = UUID.randomUUID();
        when(experimentRepo.existsById(id)).thenReturn(false);

        assertStatus(() -> service.miners(id), HttpStatus.NOT_FOUND);
    }

    @Test
    void minersListsRosterForKnownExperiment() {
        UUID id = UUID.randomUUID();
        when(experimentRepo.existsById(id)).thenReturn(true);
        when(minerRepo.findByExperimentIdOrderByMinerIdAsc(id)).thenReturn(List.of(mock(Miner.class), mock(Miner.class)));

        List<MinerView> miners = service.miners(id);

        assertThat(miners).hasSize(2);
    }

    @Test
    void listAttachesRunCountPerExperiment() {
        UUID id = UUID.randomUUID();
        Experiment exp = mock(Experiment.class);
        when(exp.getId()).thenReturn(id);
        when(experimentRepo.findAllByOrderByStartedAtDesc()).thenReturn(List.of(exp));
        when(runRepo.countByExperimentId(id)).thenReturn(3L);

        List<ExperimentView> list = service.list();

        assertThat(list).hasSize(1);
        assertThat(list.get(0).runCount()).isEqualTo(3L);
    }

    private static void assertStatus(org.assertj.core.api.ThrowableAssert.ThrowingCallable call, HttpStatus expected) {
        assertThatThrownBy(call)
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(expected);
    }
}
