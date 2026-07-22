package com.ekruminis.txanalytics.queryapi.service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import com.ekruminis.txanalytics.queryapi.mongo.BlockResultMongoRepository;
import com.ekruminis.txanalytics.queryapi.mongo.TxResultDoc;
import com.ekruminis.txanalytics.queryapi.mongo.TxResultMongoRepository;
import com.ekruminis.txanalytics.queryapi.postgres.Block;
import com.ekruminis.txanalytics.queryapi.postgres.BlockRepository;
import com.ekruminis.txanalytics.queryapi.postgres.SimulationRun;
import com.ekruminis.txanalytics.queryapi.postgres.SimulationRunRepository;
import com.ekruminis.txanalytics.queryapi.web.BlockDetailView;
import com.ekruminis.txanalytics.queryapi.web.BlockTxView;

@ExtendWith(MockitoExtension.class)
class BlockServiceTest {

    @Mock BlockRepository blockRepo;
    @Mock SimulationRunRepository runRepo;
    @Mock BlockResultMongoRepository blockResultRepo;
    @Mock TxResultMongoRepository txRepo;
    @InjectMocks BlockService service;

    @Test
    void detailReturnsNotFoundWhenBlockMissing() {
        UUID runId = UUID.randomUUID();
        when(blockRepo.findByRunIdAndHeight(runId, 5)).thenReturn(Optional.empty());

        assertStatus(() -> service.detail(runId, 5), HttpStatus.NOT_FOUND);
    }

    @Test
    void byHashReturnsNotFoundWhenBlockMissing() {
        when(blockRepo.findByCurrentHash("deadbeef")).thenReturn(Optional.empty());

        assertStatus(() -> service.byHash("deadbeef"), HttpStatus.NOT_FOUND);
    }

    @Test
    void detailReturnsInternalErrorWhenBlockReferencesMissingRun() {
        UUID runId = UUID.randomUUID();
        Block block = mock(Block.class);
        when(block.getRunId()).thenReturn(runId);
        when(blockRepo.findByRunIdAndHeight(runId, 5)).thenReturn(Optional.of(block));
        when(runRepo.findById(runId)).thenReturn(Optional.empty());

        assertStatus(() -> service.detail(runId, 5), HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Test
    void detailAssemblesBlockWhenOutcomesAbsent() {
        UUID runId = UUID.randomUUID();
        Block block = mock(Block.class);
        when(block.getRunId()).thenReturn(runId);
        when(block.getHeight()).thenReturn(5);
        when(blockRepo.findByRunIdAndHeight(runId, 5)).thenReturn(Optional.of(block));
        when(runRepo.findById(runId)).thenReturn(Optional.of(mock(SimulationRun.class)));
        when(blockResultRepo.findById(runId + ":" + 5)).thenReturn(Optional.empty());

        BlockDetailView view = service.detail(runId, 5);

        assertThat(view.height()).isEqualTo(5);
        assertThat(view.baseFee()).isNull();
    }

    @Test
    void transactionsReturnsNotFoundWhenBlockMissing() {
        when(blockRepo.findByCurrentHash("nope")).thenReturn(Optional.empty());

        assertStatus(() -> service.transactions("nope", PageRequest.of(0, 10)), HttpStatus.NOT_FOUND);
    }

    @Test
    void transactionsPagesTxsForTheBlock() {
        UUID runId = UUID.randomUUID();
        Block block = mock(Block.class);
        when(block.getRunId()).thenReturn(runId);
        when(block.getHeight()).thenReturn(7);
        when(blockRepo.findByCurrentHash("hash7")).thenReturn(Optional.of(block));
        Page<TxResultDoc> empty = new PageImpl<>(List.of());
        when(txRepo.findByRunIdAndHeight(eq(runId.toString()), eq(7), any())).thenReturn(empty);

        Page<BlockTxView> result = service.transactions("hash7", PageRequest.of(0, 10));

        assertThat(result.getContent()).isEmpty();
    }

    private static void assertStatus(org.assertj.core.api.ThrowableAssert.ThrowingCallable call, HttpStatus expected) {
        assertThatThrownBy(call)
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(expected);
    }
}
