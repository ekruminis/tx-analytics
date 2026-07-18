package ZBRA.persistence;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ExperimentRepository extends JpaRepository<Experiment, UUID> {
}
