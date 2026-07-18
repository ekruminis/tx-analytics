package com.ekruminis.txanalytics.indexer.kafka;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.mongodb.core.BulkOperations;
import org.springframework.data.mongodb.core.BulkOperations.BulkMode;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.ekruminis.txanalytics.indexer.mongo.TxResultDoc;
import com.ekruminis.txanalytics.wire.TxResult;

@Component
public class TxResultConsumer {

    private static final Logger log = LoggerFactory.getLogger(TxResultConsumer.class);

    private final MongoTemplate mongoTemplate;

    public TxResultConsumer(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @KafkaListener(topics = "tx.results", containerFactory = "txResultListenerFactory")
    public void onTxResults(List<TxResult> results) {
        List<TxResultDoc> docs = results.stream().map(TxResultDoc::from).toList();
        BulkOperations bulk = mongoTemplate.bulkOps(BulkMode.UNORDERED, TxResultDoc.class);
        docs.forEach(bulk::insert);
        try {
            bulk.execute();
        } catch (DuplicateKeyException e) {
            log.debug("skipping {} duplicate tx docs in batch", docs.size(), e);
        }
    }
}
