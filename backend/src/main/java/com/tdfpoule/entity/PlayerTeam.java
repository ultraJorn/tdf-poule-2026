package com.tdfpoule.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "player_team")
public class PlayerTeam {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "poule_id", nullable = false, length = 8)
    private String pouleId;

    @Column(nullable = false, length = 60)
    private String username;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "rider_ids", nullable = false, columnDefinition = "jsonb")
    private List<String> riderIds = new ArrayList<>();

    @Column(name = "swaps_used", nullable = false)
    private int swapsUsed = 0;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "swap_log", nullable = false, columnDefinition = "jsonb")
    private List<SwapLogEntry> swapLog = new ArrayList<>();

    @Column(name = "joined_at", nullable = false)
    private Instant joinedAt = Instant.now();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getPouleId() { return pouleId; }
    public void setPouleId(String pouleId) { this.pouleId = pouleId; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public List<String> getRiderIds() { return riderIds; }
    public void setRiderIds(List<String> riderIds) { this.riderIds = riderIds; }

    public int getSwapsUsed() { return swapsUsed; }
    public void setSwapsUsed(int swapsUsed) { this.swapsUsed = swapsUsed; }

    public List<SwapLogEntry> getSwapLog() { return swapLog; }
    public void setSwapLog(List<SwapLogEntry> swapLog) { this.swapLog = swapLog; }

    public Instant getJoinedAt() { return joinedAt; }
    public void setJoinedAt(Instant joinedAt) { this.joinedAt = joinedAt; }
}
