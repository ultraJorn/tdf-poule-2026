package com.tdfpoule.dto;

import com.tdfpoule.entity.SwapLogEntry;

public record SwapLogEntryDto(String outId, String inId, int effectiveFromStage, long ts) {
    public static SwapLogEntryDto from(SwapLogEntry e) {
        return new SwapLogEntryDto(e.getOutId(), e.getInId(), e.getEffectiveFromStage(), e.getTs());
    }
}
