package com.ekruminis.txanalytics.queryapi.service;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import com.ekruminis.txanalytics.queryapi.postgres.ExperimentRepository;
import com.ekruminis.txanalytics.queryapi.postgres.MinerRepository;
import com.ekruminis.txanalytics.queryapi.postgres.SimulationRun;
import com.ekruminis.txanalytics.queryapi.postgres.SimulationRunRepository;

@ExtendWith(MockitoExtension.class)
class AnalyticsServiceTest {

    @Mock SimulationRunRepository runRepo;
    @Mock ExperimentRepository experimentRepo;
    @Mock MinerRepository minerRepo;

    AnalyticsService service;

    @BeforeEach
    void setUp() {
        service = new AnalyticsService(null, runRepo, experimentRepo, minerRepo);
    }

    @Test
    void summaryReturnsNotFoundForUnknownRun() {
        UUID runId = UUID.randomUUID();
        when(runRepo.findById(runId)).thenReturn(Optional.empty());

        assertStatus(() -> service.summary(runId), HttpStatus.NOT_FOUND);
    }

    @Test
    void feeDistributionReturnsNotFoundForUnknownRun() {
        UUID runId = UUID.randomUUID();
        when(runRepo.findById(runId)).thenReturn(Optional.empty());

        assertStatus(() -> service.feeDistribution(runId), HttpStatus.NOT_FOUND);
    }

    @Test
    void timeseriesRejectsUnknownMetric() {
        UUID runId = UUID.randomUUID();
        when(runRepo.findById(runId)).thenReturn(Optional.of(mock(SimulationRun.class)));

        assertStatus(() -> service.timeseries(runId, "bogus", "day"), HttpStatus.BAD_REQUEST);
    }

    @Test
    void timeseriesRejectsUnknownInterval() {
        UUID runId = UUID.randomUUID();
        when(runRepo.findById(runId)).thenReturn(Optional.of(mock(SimulationRun.class)));

        assertStatus(() -> service.timeseries(runId, "payout", "decade"), HttpStatus.BAD_REQUEST);
    }

    private static void assertStatus(org.assertj.core.api.ThrowableAssert.ThrowingCallable call, HttpStatus expected) {
        assertThatThrownBy(call)
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(expected);
    }
}
