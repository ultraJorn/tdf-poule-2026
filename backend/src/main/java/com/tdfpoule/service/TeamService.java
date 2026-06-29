package com.tdfpoule.service;

import com.tdfpoule.dto.*;
import com.tdfpoule.entity.Poule;
import com.tdfpoule.entity.PlayerTeam;
import com.tdfpoule.entity.Rider;
import com.tdfpoule.entity.Stage;
import com.tdfpoule.entity.SwapLogEntry;
import com.tdfpoule.exception.ApiException;
import com.tdfpoule.repository.PlayerTeamRepository;
import com.tdfpoule.repository.RiderRepository;
import com.tdfpoule.repository.StageRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class TeamService {

    private final PlayerTeamRepository playerTeamRepository;
    private final RiderRepository riderRepository;
    private final StageRepository stageRepository;
    private final PouleService pouleService;
    private final ScoringService scoringService;

    public TeamService(PlayerTeamRepository playerTeamRepository, RiderRepository riderRepository,
                        StageRepository stageRepository, PouleService pouleService, ScoringService scoringService) {
        this.playerTeamRepository = playerTeamRepository;
        this.riderRepository = riderRepository;
        this.stageRepository = stageRepository;
        this.pouleService = pouleService;
        this.scoringService = scoringService;
    }

    public Optional<TeamDto> find(String code, String username) {
        String id = pouleService.normalizeCode(code);
        return playerTeamRepository.findByPouleIdAndUsernameIgnoreCase(id, username).map(t -> toDto(id, t));
    }

    @Transactional
    public TeamDto create(String code, CreateTeamRequest req) {
        String id = pouleService.normalizeCode(code);
        Poule poule = pouleService.getOrThrow(id);

        if (req.riderIds().size() != poule.getTeamSize()) {
            throw ApiException.badRequest("A team needs exactly " + poule.getTeamSize() + " riders.");
        }
        if (req.riderIds().stream().distinct().count() != req.riderIds().size()) {
            throw ApiException.badRequest("The same rider can't be picked twice.");
        }

        Map<String, Rider> riders = riderRepository.findAllById(req.riderIds()).stream()
                .filter(r -> r.getPouleId().equals(id))
                .collect(Collectors.toMap(Rider::getId, r -> r));
        if (riders.size() != req.riderIds().size()) {
            throw ApiException.badRequest("One or more selected riders couldn't be found in this poule.");
        }
        int used = req.riderIds().stream().mapToInt(rid -> riders.get(rid).getPrice()).sum();
        if (used > poule.getBudgetCap()) {
            throw ApiException.badRequest("That team costs " + used + ", which is over the budget cap of " + poule.getBudgetCap() + ".");
        }

        PlayerTeam team = new PlayerTeam();
        team.setPouleId(id);
        team.setUsername(req.username().trim());
        team.setRiderIds(req.riderIds());
        team.setSwapsUsed(0);
        team.setJoinedAt(Instant.now());
        try {
            playerTeamRepository.save(team);
        } catch (DataIntegrityViolationException e) {
            throw ApiException.conflict("That name has already joined this poule.");
        }
        return toDto(id, team);
    }

    @Transactional
    public TeamDto swap(String code, String username, SwapRequest req) {
        String id = pouleService.normalizeCode(code);
        Poule poule = pouleService.getOrThrow(id);
        PlayerTeam team = playerTeamRepository.findByPouleIdAndUsernameIgnoreCase(id, username)
                .orElseThrow(() -> ApiException.notFound("No team found for " + username));

        // Free, uncapped swaps for the whole pre-race period -- real startlists can still
        // change right up to the gun, so players shouldn't burn a limited swap reacting to
        // that. The limited totalSwaps budget only starts counting once stage 1 is actually
        // locked in by the organizer (currentStage > 0), not tied to a specific clock time.
        boolean freeSwap = poule.getCurrentStage() == 0;
        if (!freeSwap && team.getSwapsUsed() >= poule.getTotalSwaps()) {
            throw ApiException.badRequest("No swaps left.");
        }
        List<String> currentRoster = scoringService.rosterAtStage(team.getRiderIds(), team.getSwapLog(), poule.getCurrentStage() + 1);
        if (!currentRoster.contains(req.outId())) {
            throw ApiException.badRequest("That rider isn't on the current roster.");
        }
        if (currentRoster.contains(req.inId())) {
            throw ApiException.badRequest("That rider is already on the roster.");
        }

        Map<String, Rider> riderMap = riderRepository.findAllById(currentRoster).stream()
                .collect(Collectors.toMap(Rider::getId, r -> r));
        Rider incoming = riderRepository.findById(req.inId())
                .filter(r -> r.getPouleId().equals(id))
                .orElseThrow(() -> ApiException.notFound("Replacement rider not found"));

        int usedExcludingOut = currentRoster.stream()
                .filter(rid -> !rid.equals(req.outId()))
                .mapToInt(rid -> riderMap.get(rid).getPrice())
                .sum();
        if (usedExcludingOut + incoming.getPrice() > poule.getBudgetCap()) {
            throw ApiException.badRequest("That swap would go over the budget cap of " + poule.getBudgetCap() + ".");
        }

        team.getSwapLog().add(new SwapLogEntry(req.outId(), req.inId(), poule.getCurrentStage() + 1, Instant.now().toEpochMilli()));
        if (!freeSwap) {
            team.setSwapsUsed(team.getSwapsUsed() + 1);
        }
        playerTeamRepository.save(team);
        return toDto(id, team);
    }

    public List<LeaderboardRowDto> leaderboard(String code) {
        String id = pouleService.normalizeCode(code);
        pouleService.getOrThrow(id);
        List<Stage> stages = stageRepository.findByPouleIdOrderByStageNumberAsc(id);
        return playerTeamRepository.findByPouleId(id).stream()
                .map(team -> new LeaderboardRowDto(team.getUsername(), scoringService.teamTotal(team, stages).total()))
                .sorted((a, b) -> Integer.compare(b.total(), a.total()))
                .collect(Collectors.toList());
    }

    private TeamDto toDto(String pouleId, PlayerTeam team) {
        List<Stage> stages = stageRepository.findByPouleIdOrderByStageNumberAsc(pouleId);
        return TeamDto.from(team, scoringService.teamTotal(team, stages));
    }
}
