package com.tdfpoule.dto;

import jakarta.validation.constraints.NotBlank;

public record RiderUpsertRequest(
        @NotBlank String name,
        @NotBlank String team,
        int price,
        @NotBlank String tag,
        Boolean active,
        String nat
) {
}
