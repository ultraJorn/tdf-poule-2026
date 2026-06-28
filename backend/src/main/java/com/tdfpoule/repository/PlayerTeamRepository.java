package com.tdfpoule.repository;

import com.tdfpoule.entity.PlayerTeam;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PlayerTeamRepository extends JpaRepository<PlayerTeam, Long> {
    List<PlayerTeam> findByPouleId(String pouleId);
    Optional<PlayerTeam> findByPouleIdAndUsernameIgnoreCase(String pouleId, String username);
    long countByPouleId(String pouleId);
}
