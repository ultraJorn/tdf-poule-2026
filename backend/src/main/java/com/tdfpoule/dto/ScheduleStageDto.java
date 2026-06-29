package com.tdfpoule.dto;

import com.tdfpoule.service.ScheduleService;

public record ScheduleStageDto(
        int stage, String date, String day, String start, String finish,
        String type, String startTimeCest, String startTimeNote
) {
    public static ScheduleStageDto from(ScheduleService.ScheduleStage s) {
        return new ScheduleStageDto(s.stage(), s.date(), s.day(), s.start(), s.finish(), s.type(), s.startTimeCest(), s.startTimeNote());
    }
}
