package com.ekruminis.txanalytics.queryapi.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
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

import com.ekruminis.txanalytics.queryapi.mongo.TxResultDoc;
import com.ekruminis.txanalytics.queryapi.mongo.TxResultMongoRepository;
import com.ekruminis.txanalytics.queryapi.postgres.Block;
import com.ekruminis.txanalytics.queryapi.postgres.BlockRepository;
import com.ekruminis.txanalytics.queryapi.postgres.Experiment;
import com.ekruminis.txanalytics.queryapi.postgres.ExperimentRepository;
import com.ekruminis.txanalytics.queryapi.postgres.SimulationRun;
import com.ekruminis.txanalytics.queryapi.postgres.SimulationRunRepository;
import com.ekruminis.txanalytics.queryapi.web.TxLookupView;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock TxResultMongoRepository txRepo;
    @Mock SimulationRunRepository runRepo;
    @Mock ExperimentRepository experimentRepo;
    @Mock BlockRepository blockRepo;
    @InjectMocks TransactionService service;

    @Test
    void lookupReturnsNotFoundWhenHashUnknown() {
        when(txRepo.findByTxHashOrderByRunIdAscHeightAsc("abc")).thenReturn(List.of());

        assertStatus(() -> service.lookup("abc"), HttpStatus.NOT_FOUND);
    }

    @Test
    void inRunReturnsNotFoundWhenHashMissingInRun() {
        UUID runId = UUID.randomUUID();
        when(txRepo.findByRunIdAndTxHashOrderByHeightAsc(runId.toString(), "abc")).thenReturn(List.of());

        assertStatus(() -> service.inRun(runId, "abc"), HttpStatus.NOT_FOUND);
    }

    @Test
    void lookupEnrichesEachOccurrenceWithRunAndBlockContext() {
        UUID runId = UUID.randomUUID();
        UUID expId = UUID.randomUUID();

        TxResultDoc doc = mock(TxResultDoc.class);
        when(doc.getRunId()).thenReturn(runId.toString());
        when(doc.getHeight()).thenReturn(3);

        SimulationRun run = mock(SimulationRun.class);
        when(run.getId()).thenReturn(runId);
        when(run.getExperimentId()).thenReturn(expId);
        when(run.getTfmType()).thenReturn("eip1559");
        when(run.getMechanismParams()).thenReturn("size_limit=4000000");

        Experiment exp = mock(Experiment.class);
        when(exp.getId()).thenReturn(expId);

        Block block = mock(Block.class);
        when(block.getCurrentHash()).thenReturn("blockhash");

        when(txRepo.findByTxHashOrderByRunIdAscHeightAsc("tx1")).thenReturn(List.of(doc));
        when(runRepo.findAllById(any())).thenReturn(List.of(run));
        when(experimentRepo.findAllById(any())).thenReturn(List.of(exp));
        when(blockRepo.findByRunIdAndHeight(runId, 3)).thenReturn(Optional.of(block));

        TxLookupView view = service.lookup("tx1");

        assertThat(view.txHash()).isEqualTo("tx1");
        assertThat(view.occurrences()).hasSize(1);
        assertThat(view.occurrences().get(0).tfm()).isEqualTo("eip1559");
    }

    private static void assertStatus(org.assertj.core.api.ThrowableAssert.ThrowingCallable call, HttpStatus expected) {
        assertThatThrownBy(call)
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(expected);
    }
}
