package com.ekruminis.txanalytics.indexer.elastic;

import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface MinerRosterEsRepository extends ElasticsearchRepository<MinerRosterEsDoc, String> {
}
