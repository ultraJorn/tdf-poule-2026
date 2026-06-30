package com.tdfpoule.controller;

import com.tdfpoule.dto.CreateTeamRequest;
import com.tdfpoule.dto.LeaderboardRowDto;
import com.tdfpoule.dto.SwapRequest;
import com.tdfpoule.dto.TeamDto;
import com.tdfpoule.exception.ApiException;
import com.tdfpoule.service.TeamService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/poules/{code}")
public class TeamController {

    private final TeamService teamService;

    public TeamController(TeamService teamService) {
        this.teamService = teamService;
    }

    @GetMapping("/teams/{username}")
    public TeamDto get(@PathVariable String code, @PathVariable String username) {
        return teamService.find(code, username)
                .orElseThrow(() -> ApiException.notFound("No team found for " + username));
    }

    @PostMapping("/teams")
    @ResponseStatus(HttpStatus.CREATED)
    public TeamDto create(@PathVariable String code, @Valid @RequestBody CreateTeamRequest req) {
        return teamService.create(code, req);
    }

    @PostMapping("/teams/{username}/swap")
    public TeamDto swap(@PathVariable String code, @PathVariable String username, @Valid @RequestBody SwapRequest req) {
        return teamService.swap(code, username, req);
    }

    @DeleteMapping("/teams/{username}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable String code, @PathVariable String username,
                       @RequestHeader("X-Admin-Password") String adminPassword) {
        teamService.delete(code, username, adminPassword);
    }

    @GetMapping("/leaderboard")
    public List<LeaderboardRowDto> leaderboard(@PathVariable String code) {
        return teamService.leaderboard(code);
    }
}
