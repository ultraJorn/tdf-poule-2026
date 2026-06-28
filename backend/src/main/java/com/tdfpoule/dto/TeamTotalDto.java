package com.tdfpoule.dto;

import java.util.List;

public record TeamTotalDto(int total, List<StageBreakdownDto> breakdown) {
}
