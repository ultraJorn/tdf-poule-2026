package com.tdfpoule.dto;

import java.time.Instant;

public record AdminPouleSummaryDto(
        String code,
        String name,
        Instant createdAt,
        int currentStage,
        long riderCount,
        long teamCount
) {
}
