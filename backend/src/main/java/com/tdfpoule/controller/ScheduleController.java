package com.tdfpoule.controller;

import com.tdfpoule.dto.ScheduleInfoDto;
import com.tdfpoule.dto.ScheduleStageDto;
import com.tdfpoule.service.ScheduleService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
public class ScheduleController {

    private final ScheduleService scheduleService;

    public ScheduleController(ScheduleService scheduleService) {
        this.scheduleService = scheduleService;
    }

    @GetMapping("/api/schedule")
    public ScheduleInfoDto get() {
        List<ScheduleStageDto> stages = scheduleService.getStages().stream()
                .map(ScheduleStageDto::from).collect(Collectors.toList());
        return new ScheduleInfoDto(
                stages,
                scheduleService.getStage1Start(),
                scheduleService.getFreeSwapWindowStart(),
                scheduleService.isFreeSwapWindowActive()
        );
    }
}
