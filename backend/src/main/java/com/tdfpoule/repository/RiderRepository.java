package com.tdfpoule.repository;

import com.tdfpoule.entity.Rider;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RiderRepository extends JpaRepository<Rider, String> {
    List<Rider> findByPouleIdOrderByPriceDescNameAsc(String pouleId);
    void deleteByPouleId(String pouleId);
    long countByPouleId(String pouleId);
}
