package com.tdfpoule.dto;

import jakarta.validation.constraints.NotBlank;

public record SwapRequest(@NotBlank String outId, @NotBlank String inId) {
}
