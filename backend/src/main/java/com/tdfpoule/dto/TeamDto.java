package com.tdfpoule.dto;

import com.tdfpoule.entity.PlayerTeam;

import java.util.List;
import java.util.stream.Collectors;

public record TeamDto(
        String username,
        List<String> riderIds,
        int swapsUsed,
        List<SwapLogEntryDto> swapLog,
        TeamTotalDto total
) {
    public static TeamDto from(PlayerTeam team, TeamTotalDto total) {
        return new TeamDto(
                team.getUsername(),
                team.getRiderIds(),
                team.getSwapsUsed(),
                team.getSwapLog().stream().map(SwapLogEntryDto::from).collect(Collectors.toList()),
                total
        );
    }
}
