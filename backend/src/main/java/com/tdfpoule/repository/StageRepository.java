package com.tdfpoule.repository;

import com.tdfpoule.entity.Stage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StageRepository extends JpaRepository<Stage, Long> {
    List<Stage> findByPouleIdOrderByStageNumberAsc(String pouleId);
    Optional<Stage> findByPouleIdAndStageNumber(String pouleId, int stageNumber);
}
