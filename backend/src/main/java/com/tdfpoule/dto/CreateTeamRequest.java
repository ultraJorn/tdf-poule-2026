package com.tdfpoule.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record CreateTeamRequest(
        @NotBlank String username,
        @NotEmpty List<String> riderIds
) {
}
