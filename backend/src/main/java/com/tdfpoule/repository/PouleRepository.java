package com.tdfpoule.repository;

import com.tdfpoule.entity.Poule;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PouleRepository extends JpaRepository<Poule, String> {
}
