package com.tdfpoule.dto;

import java.util.List;

public record CsvImportResult(int totalRows, int matchedRows, List<String> unmatchedNames, boolean saved) {
}
