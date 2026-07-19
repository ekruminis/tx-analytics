package com.ekruminis.txanalytics.indexer.kafka;

import java.util.List;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.ekruminis.txanalytics.indexer.elastic.TxResultEsDoc;
import com.ekruminis.txanalytics.indexer.elastic.TxResultEsRepository;
import com.ekruminis.txanalytics.wire.TxResult;

@Component
public class TxResultEsConsumer {

    private final TxResultEsRepository repository;

    public TxResultEsConsumer(TxResultEsRepository repository) {
        this.repository = repository;
    }

    @KafkaListener(topics = "tx.results", containerFactory = "esTxResultListenerFactory")
    public void onTxResults(List<TxResult> results) {
        List<TxResultEsDoc> docs = results.stream().map(TxResultEsDoc::from).toList();
        repository.saveAll(docs);
    }
}
