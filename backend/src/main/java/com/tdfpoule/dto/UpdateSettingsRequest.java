package com.tdfpoule.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record UpdateSettingsRequest(
        @NotBlank String name,
        @Min(1) int teamSize,
        @Min(1) int budgetCap,
        @Min(0) int totalSwaps
) {
}
