package com.tdfpoule.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record CreatePouleRequest(
        @NotBlank String pouleName,
        @NotBlank String username,
        @NotBlank String adminPassword,
        @Min(1) Integer teamSize,
        @Min(1) Integer budgetCap,
        @Min(0) Integer totalSwaps
) {
}
