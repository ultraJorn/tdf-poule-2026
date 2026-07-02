package com.tdfpoule.dto;

import com.tdfpoule.entity.Rider;

public record RiderDto(String id, String name, String team, int price, String tag, boolean active, String nat) {
    public static RiderDto from(Rider r) {
        return new RiderDto(r.getId(), r.getName(), r.getTeam(), r.getPrice(), r.getTag(), r.isActive(), r.getNat());
    }
}
