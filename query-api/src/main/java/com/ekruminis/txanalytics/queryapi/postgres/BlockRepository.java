package com.ekruminis.txanalytics.queryapi.postgres;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BlockRepository extends JpaRepository<Block, Long> {

    Page<Block> findByRunIdOrderByHeightAsc(UUID runId, Pageable pageable);

    Optional<Block> findByCurrentHash(String currentHash);

    Optional<Block> findByRunIdAndHeight(UUID runId, int height);
}
