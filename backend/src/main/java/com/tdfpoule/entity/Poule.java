package com.tdfpoule.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "poule")
public class Poule {

    @Id
    @Column(length = 8)
    private String id; // the join code, e.g. "ABCDE"

    @Column(nullable = false, length = 120)
    private String name;

    @Column(name = "admin_password_hash", nullable = false, length = 100)
    private String adminPasswordHash;

    @Column(name = "budget_cap", nullable = false)
    private int budgetCap;

    @Column(name = "team_size", nullable = false)
    private int teamSize;

    @Column(name = "total_swaps", nullable = false)
    private int totalSwaps;

    @Column(name = "current_stage", nullable = false)
    private int currentStage;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getAdminPasswordHash() { return adminPasswordHash; }
    public void setAdminPasswordHash(String adminPasswordHash) { this.adminPasswordHash = adminPasswordHash; }

    public int getBudgetCap() { return budgetCap; }
    public void setBudgetCap(int budgetCap) { this.budgetCap = budgetCap; }

    public int getTeamSize() { return teamSize; }
    public void setTeamSize(int teamSize) { this.teamSize = teamSize; }

    public int getTotalSwaps() { return totalSwaps; }
    public void setTotalSwaps(int totalSwaps) { this.totalSwaps = totalSwaps; }

    public int getCurrentStage() { return currentStage; }
    public void setCurrentStage(int currentStage) { this.currentStage = currentStage; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
