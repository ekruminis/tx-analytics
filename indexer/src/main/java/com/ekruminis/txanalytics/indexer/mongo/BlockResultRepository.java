package com.ekruminis.txanalytics.indexer.mongo;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface BlockResultRepository extends MongoRepository<BlockResultDoc, String> {
}
