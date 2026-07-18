package ZBRA.persistence;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SimulationRunRepository extends JpaRepository<SimulationRun, UUID> {
}
