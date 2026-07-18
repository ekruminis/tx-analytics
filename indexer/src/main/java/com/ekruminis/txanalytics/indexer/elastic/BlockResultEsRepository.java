package com.ekruminis.txanalytics.indexer.elastic;

import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface BlockResultEsRepository extends ElasticsearchRepository<BlockResultEsDoc, String> {
}
