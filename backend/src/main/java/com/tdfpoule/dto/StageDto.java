package com.tdfpoule.dto;

import com.tdfpoule.entity.Stage;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public record StageDto(
        int n,
        String label,
        String tag,
        Integer km,
        Integer elev,
        StageNoteDto note,
        List<String> favorites,
        boolean locked,
        List<String> top,
        List<String> gc,
        Map<String, String> jerseys,
        Map<String, Integer> pointsByRider
) {
    public static StageDto from(Stage s) {
        return new StageDto(
                s.getStageNumber(),
                s.getLabel(),
                s.getStageTag(),
                s.getKm(),
                s.getElev(),
                new StageNoteDto(s.getNoteEn(), s.getNoteNl()),
                s.getFavorites(),
                s.isLocked(),
                s.getTopRiderIds().stream().filter(id -> id != null && !id.isBlank()).collect(Collectors.toList()),
                s.getGcRiderIds().stream().filter(id -> id != null && !id.isBlank()).collect(Collectors.toList()),
                s.getJerseys(),
                s.getPointsByRider()
        );
    }
}
