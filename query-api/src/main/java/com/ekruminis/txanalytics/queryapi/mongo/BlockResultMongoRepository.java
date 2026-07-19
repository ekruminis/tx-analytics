package com.ekruminis.txanalytics.queryapi.mongo;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface BlockResultMongoRepository extends MongoRepository<BlockResultDoc, String> {
}
