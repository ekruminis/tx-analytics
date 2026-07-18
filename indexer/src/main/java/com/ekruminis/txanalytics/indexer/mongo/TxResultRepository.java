package com.ekruminis.txanalytics.indexer.mongo;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface TxResultRepository extends MongoRepository<TxResultDoc, String> {
}
