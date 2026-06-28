package com.tdfpoule.dto;

import jakarta.validation.constraints.NotBlank;

public record CsvImportRequest(@NotBlank String csvText) {
}
