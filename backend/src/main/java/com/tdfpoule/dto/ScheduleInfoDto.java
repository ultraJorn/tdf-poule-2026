package com.tdfpoule.dto;

import java.time.Instant;
import java.util.List;

public record ScheduleInfoDto(
        List<ScheduleStageDto> stages,
        Instant stage1Start
) {
}
