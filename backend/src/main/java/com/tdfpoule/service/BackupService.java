package com.tdfpoule.service;

import com.tdfpoule.dto.*;
import com.tdfpoule.entity.*;
import com.tdfpoule.repository.PlayerTeamRepository;
import com.tdfpoule.repository.PouleRepository;
import com.tdfpoule.repository.RiderRepository;
import com.tdfpoule.repository.StageRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Export/import a whole poule as one JSON blob -- a safety net for organizers, the same
 * "copy this code somewhere safe" idea the original single-file app used, now backed by a
 * real database instead of being the *only* persistence mechanism.
 */
@Service
public class BackupService {

    private final PouleRepository pouleRepository;
    private final RiderRepository riderRepository;
    private final StageRepository stageRepository;
    private final PlayerTeamRepository playerTeamRepository;
    private final PouleService pouleService;
    private final ScoringService scoringService;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public BackupService(PouleRepository pouleRepository, RiderRepository riderRepository,
                          StageRepository stageRepository, PlayerTeamRepository playerTeamRepository,
                          PouleService pouleService, ScoringService scoringService) {
        this.pouleRepository = pouleRepository;
        this.riderRepository = riderRepository;
        this.stageRepository = stageRepository;
        this.playerTeamRepository = playerTeamRepository;
        this.pouleService = pouleService;
        this.scoringService = scoringService;
    }

    public BackupBundle export(String code, String adminPassword) {
        String id = pouleService.normalizeCode(code);
        pouleService.assertAdmin(id, adminPassword);
        Poule poule = pouleService.getOrThrow(id);

        List<Stage> stages = stageRepository.findByPouleIdOrderByStageNumberAsc(id);
        List<StageDto> stageDtos = stages.stream().map(StageDto::from).collect(Collectors.toList());
        List<RiderDto> riders = riderRepository.findByPouleIdOrderByPriceDescNameAsc(id)
                .stream().map(RiderDto::from).collect(Collectors.toList());
        List<TeamDto> teams = playerTeamRepository.findByPouleId(id).stream()
                .map(t -> TeamDto.from(t, scoringService.teamTotal(t, stages)))
                .collect(Collectors.toList());

        return new BackupBundle(id, poule.getName(), poule.getBudgetCap(), poule.getTeamSize(),
                poule.getTotalSwaps(), poule.getCurrentStage(), stageDtos, riders, teams);
    }

    @Transactional
    public PouleBundle restore(RestoreRequest req) {
        BackupBundle data = req.data();
        String id = pouleService.normalizeCode(data.code());

        playerTeamRepository.deleteAll(playerTeamRepository.findByPouleId(id));
        riderRepository.deleteByPouleId(id);
        stageRepository.deleteAll(stageRepository.findByPouleIdOrderByStageNumberAsc(id));
        pouleRepository.findById(id).ifPresent(pouleRepository::delete);

        Poule poule = new Poule();
        poule.setId(id);
        poule.setName(data.name());
        poule.setAdminPasswordHash(passwordEncoder.encode(req.newAdminPassword()));
        poule.setBudgetCap(data.budgetCap());
        poule.setTeamSize(data.teamSize());
        poule.setTotalSwaps(data.totalSwaps());
        poule.setCurrentStage(data.currentStage());
        pouleRepository.save(poule);

        riderRepository.saveAll(data.riders().stream().map(rd -> {
            Rider r = new Rider();
            r.setId(rd.id());
            r.setPouleId(id);
            r.setName(rd.name());
            r.setTeam(rd.team());
            r.setPrice(rd.price());
            r.setTag(rd.tag());
            r.setActive(rd.active());
            return r;
        }).collect(Collectors.toList()));

        stageRepository.saveAll(data.stages().stream().map(sd -> {
            Stage s = new Stage();
            s.setPouleId(id);
            s.setStageNumber(sd.n());
            s.setLabel(sd.label());
            s.setStageTag(sd.tag());
            s.setKm(sd.km());
            s.setElev(sd.elev());
            s.setNoteEn(sd.note() != null ? sd.note().en() : null);
            s.setNoteNl(sd.note() != null ? sd.note().nl() : null);
            s.setFavorites(sd.favorites() != null ? sd.favorites() : List.of());
            s.setElevationProfile(sd.elevationProfile() != null
                    ? sd.elevationProfile().stream().map(p -> new ElevationPoint(p.km(), p.elev())).collect(Collectors.toList())
                    : List.of());
            s.setLocked(sd.locked());
            s.setTopRiderIds(sd.top() != null ? sd.top() : List.of());
            s.setGcRiderIds(sd.gc() != null ? sd.gc() : List.of());
            s.setJerseys(sd.jerseys() != null ? sd.jerseys() : Map.of());
            s.setPointsByRider(sd.pointsByRider() != null ? sd.pointsByRider() : Map.of());
            return s;
        }).collect(Collectors.toList()));

        playerTeamRepository.saveAll(data.teams().stream().map(td -> {
            PlayerTeam t = new PlayerTeam();
            t.setPouleId(id);
            t.setUsername(td.username());
            t.setRiderIds(td.riderIds());
            t.setSwapsUsed(td.swapsUsed());
            t.setJoinedAt(Instant.now());
            t.setSwapLog(td.swapLog().stream()
                    .map(sl -> new SwapLogEntry(sl.outId(), sl.inId(), sl.effectiveFromStage(), sl.ts()))
                    .collect(Collectors.toList()));
            return t;
        }).collect(Collectors.toList()));

        return pouleService.getBundle(id);
    }
}
