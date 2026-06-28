package com.tdfpoule.dto;

import java.util.List;

/**
 * A full export of one poule: its settings, the rider pool, every stage result, and every
 * player's team. Deliberately leaves out the admin password hash -- restoring a poule means
 * setting a fresh organizer passphrase rather than carrying the old hash across.
 */
public record BackupBundle(
        String code,
        String name,
        int budgetCap,
        int teamSize,
        int totalSwaps,
        int currentStage,
        List<StageDto> stages,
        List<RiderDto> riders,
        List<TeamDto> teams
) {
}
