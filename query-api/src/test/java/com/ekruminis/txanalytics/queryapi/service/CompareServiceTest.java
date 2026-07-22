package com.ekruminis.txanalytics.queryapi.service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import com.ekruminis.txanalytics.queryapi.postgres.SimulationRun;
import com.ekruminis.txanalytics.queryapi.postgres.SimulationRunRepository;
import com.ekruminis.txanalytics.queryapi.web.BlockStatsView;
import com.ekruminis.txanalytics.queryapi.web.ComparisonView;
import com.ekruminis.txanalytics.queryapi.web.DistributionView;
import com.ekruminis.txanalytics.queryapi.web.MetricRanking;
import com.ekruminis.txanalytics.queryapi.web.RunSummaryView;
import com.ekruminis.txanalytics.queryapi.web.RunView;
import com.ekruminis.txanalytics.queryapi.web.TxFeeStatsView;

@ExtendWith(MockitoExtension.class)
class CompareServiceTest {

    @Mock
    AnalyticsService analytics;

    @Mock
    SimulationRunRepository runRepo;

    @InjectMocks
    CompareService service;

    @Test
    void rejectsWhenBothExperimentAndRunsGiven() {
        assertBadRequest(() -> service.compare(UUID.randomUUID(), null, "a,b"));
    }

    @Test
    void rejectsWhenNeitherExperimentNorRunsGiven() {
        assertBadRequest(() -> service.compare(null, null, null));
    }

    @Test
    void rejectsTfmFilterWithoutExperiment() {
        assertBadRequest(() -> service.compare(null, "eip1559", null));
    }

    @Test
    void rejectsRunsCsvContainingAnInvalidUuid() {
        assertBadRequest(() -> service.compare(null, null, "not-a-uuid"));
    }

    @Test
    void notFoundWhenExperimentHasNoRuns() {
        UUID experiment = UUID.randomUUID();
        when(runRepo.findByExperimentIdOrderByTfmType(experiment)).thenReturn(List.of());

        assertThatThrownBy(() -> service.compare(experiment, null, null))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void ranksRunsByAveragePayoutHighestFirst() {
        UUID low = UUID.randomUUID();
        UUID high = UUID.randomUUID();
        UUID mid = UUID.randomUUID();

        when(analytics.summary(low)).thenReturn(summaryWithPayoutAvg(low, "first_price", 5.0));
        when(analytics.summary(high)).thenReturn(summaryWithPayoutAvg(high, "eip1559", 15.0));
        when(analytics.summary(mid)).thenReturn(summaryWithPayoutAvg(mid, "second_price", 10.0));

        ComparisonView view = service.compare(null, null, low + "," + high + "," + mid);

        assertThat(view.runs()).hasSize(3);

        MetricRanking avgPayout = view.rankings().stream()
                .filter(r -> r.metric().equals("avg_payout"))
                .findFirst().orElseThrow();

        assertThat(avgPayout.higherIsBetter()).isTrue();
        assertThat(avgPayout.ranked()).extracting("runId").containsExactly(high, mid, low);
        assertThat(avgPayout.ranked()).extracting("rank").containsExactly(1, 2, 3);
    }

    @Test
    void filtersExperimentRunsByRequestedTfm() {
        UUID experiment = UUID.randomUUID();
        UUID eipRunId = UUID.randomUUID();

        SimulationRun eipRun = mock(SimulationRun.class);
        when(eipRun.getTfmType()).thenReturn("eip1559");
        when(eipRun.getId()).thenReturn(eipRunId);

        SimulationRun firstPriceRun = mock(SimulationRun.class);
        when(firstPriceRun.getTfmType()).thenReturn("first_price");

        when(runRepo.findByExperimentIdOrderByTfmType(experiment))
                .thenReturn(List.of(eipRun, firstPriceRun));
        when(analytics.summary(eipRunId)).thenReturn(summaryWithPayoutAvg(eipRunId, "eip1559", 9.0));

        ComparisonView view = service.compare(experiment, "eip1559", null);

        assertThat(view.runs()).hasSize(1);
        assertThat(view.runs().get(0).run().tfm()).isEqualTo("eip1559");
    }

    private static void assertBadRequest(org.assertj.core.api.ThrowableAssert.ThrowingCallable call) {
        assertThatThrownBy(call)
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    private static RunSummaryView summaryWithPayoutAvg(UUID runId, String tfm, double payoutAvg) {
        RunView run = new RunView(runId, tfm, "size_limit=2000000",
                Instant.EPOCH, UUID.randomUUID(), 1L, 4, "dataset");

        DistributionView payout =
                new DistributionView(10, 1.0, 9.0, payoutAvg, payoutAvg * 10, Map.of());
        DistributionView zero =
                new DistributionView(10, 0.0, 0.0, 0.0, 0.0, Map.of());

        BlockStatsView blocks = new BlockStatsView(10, payout, zero, 0.5, zero, zero, zero, zero);
        TxFeeStatsView fees = new TxFeeStatsView(100, 90, 10, zero, zero, 0.0, 0.0);
        return new RunSummaryView(run, blocks, fees);
    }
}
