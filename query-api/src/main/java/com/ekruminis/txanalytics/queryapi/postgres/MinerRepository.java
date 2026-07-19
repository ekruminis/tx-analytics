package com.ekruminis.txanalytics.queryapi.postgres;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface MinerRepository extends JpaRepository<Miner, Long> {

    List<Miner> findByExperimentIdOrderByMinerIdAsc(UUID experimentId);
}
