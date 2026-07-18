package ZBRA.persistence;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface BlockRepository extends JpaRepository<BlockEntity, Long> {

    boolean existsByRunIdAndHeight(UUID runId, int height);
}
