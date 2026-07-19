package com.ekruminis.txanalytics.queryapi.postgres;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SimulationRunRepository extends JpaRepository<SimulationRun, UUID> {

    List<SimulationRun> findByExperimentIdOrderByTfmType(UUID experimentId);

    long countByExperimentId(UUID experimentId);

    @Query("""
            select r from SimulationRun r
            where (:experimentId is null or r.experimentId = :experimentId)
              and (:tfm is null or r.tfmType = :tfm)
            """)
    Page<SimulationRun> search(@Param("experimentId") UUID experimentId,
                               @Param("tfm") String tfm,
                               Pageable pageable);
}
