package com.ekruminis.txanalytics.queryapi.mongo;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface TxResultMongoRepository extends MongoRepository<TxResultDoc, String> {

    List<TxResultDoc> findByTxHashOrderByRunIdAscHeightAsc(String txHash);

    List<TxResultDoc> findByRunIdAndTxHashOrderByHeightAsc(String runId, String txHash);

    Page<TxResultDoc> findByRunIdAndHeight(String runId, int height, Pageable pageable);
}
