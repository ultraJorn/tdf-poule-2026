package com.tdfpoule.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record RestoreRequest(
        @NotNull @Valid BackupBundle data,
        @NotBlank String newAdminPassword
) {
}
