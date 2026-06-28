package com.tdfpoule.dto;

import com.tdfpoule.entity.Poule;

import java.util.List;

public record PouleDto(
        String code,
        String name,
        int budgetCap,
        int teamSize,
        int totalSwaps,
        int currentStage,
        List<StageDto> stages
) {
    public static PouleDto from(Poule p, List<StageDto> stages) {
        return new PouleDto(p.getId(), p.getName(), p.getBudgetCap(), p.getTeamSize(), p.getTotalSwaps(), p.getCurrentStage(), stages);
    }
}
