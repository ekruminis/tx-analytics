package com.ekruminis.txanalytics.indexer.kafka;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.ekruminis.txanalytics.indexer.elastic.MinerRosterEsDoc;
import com.ekruminis.txanalytics.indexer.elastic.MinerRosterEsRepository;
import com.ekruminis.txanalytics.wire.MinerRoster;

@Component
public class MinerRosterEsConsumer {

    private static final Logger log = LoggerFactory.getLogger(MinerRosterEsConsumer.class);

    private final MinerRosterEsRepository repository;

    public MinerRosterEsConsumer(MinerRosterEsRepository repository) {
        this.repository = repository;
    }

    @KafkaListener(topics = "miners.roster", containerFactory = "esMinerRosterListenerFactory")
    public void onRoster(MinerRoster roster) {
        if (roster.miners() == null || roster.miners().isEmpty()) {
            log.warn("ignoring empty roster for experiment {}", roster.experimentId());
            return;
        }
        List<MinerRosterEsDoc> docs = new ArrayList<>(roster.miners().size());
        for (MinerRoster.Entry entry : roster.miners()) {
            docs.add(MinerRosterEsDoc.from(roster, entry));
        }
        repository.saveAll(docs);
        log.info("indexed roster of {} miners for experiment {}",
                docs.size(), roster.experimentId());
    }
}
