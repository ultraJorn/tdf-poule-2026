package com.tdfpoule.dto;

import com.tdfpoule.entity.ElevationPoint;

public record ElevationPointDto(double km, double elev) {
    public static ElevationPointDto from(ElevationPoint p) {
        return new ElevationPointDto(p.getKm(), p.getElev());
    }
}
