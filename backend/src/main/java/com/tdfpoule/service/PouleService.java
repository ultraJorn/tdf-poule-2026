package com.tdfpoule.service;

import com.tdfpoule.dto.*;
import com.tdfpoule.entity.Poule;
import com.tdfpoule.entity.Rider;
import com.tdfpoule.entity.Stage;
import com.tdfpoule.exception.ApiException;
import com.tdfpoule.repository.PlayerTeamRepository;
import com.tdfpoule.repository.PouleRepository;
import com.tdfpoule.repository.RiderRepository;
import com.tdfpoule.repository.StageRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class PouleService {

    private static final String CODE_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final PouleRepository pouleRepository;
    private final RiderRepository riderRepository;
    private final StageRepository stageRepository;
    private final PlayerTeamRepository playerTeamRepository;
    private final SeedDataService seedDataService;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public PouleService(PouleRepository pouleRepository, RiderRepository riderRepository,
                         StageRepository stageRepository, PlayerTeamRepository playerTeamRepository,
                         SeedDataService seedDataService) {
        this.pouleRepository = pouleRepository;
        this.riderRepository = riderRepository;
        this.stageRepository = stageRepository;
        this.playerTeamRepository = playerTeamRepository;
        this.seedDataService = seedDataService;
    }

    @Transactional
    public PouleBundle create(CreatePouleRequest req) {
        String code = generateUniqueCode();

        Poule poule = new Poule();
        poule.setId(code);
        poule.setName(req.pouleName().trim());
        poule.setAdminPasswordHash(passwordEncoder.encode(req.adminPassword()));
        poule.setBudgetCap(req.budgetCap() != null ? req.budgetCap() : 100);
        poule.setTeamSize(req.teamSize() != null ? req.teamSize() : 9);
        poule.setTotalSwaps(req.totalSwaps() != null ? req.totalSwaps() : 3);
        poule.setCurrentStage(0);
        pouleRepository.save(poule);

        List<Rider> riders = new ArrayList<>();
        List<SeedDataService.SeedRider> seedRiders = seedDataService.loadRiders();
        for (int i = 0; i < seedRiders.size(); i++) {
            SeedDataService.SeedRider sr = seedRiders.get(i);
            Rider r = new Rider();
            r.setId(code + "-r" + i);
            r.setPouleId(code);
            r.setName(sr.name());
            r.setTeam(sr.team());
            r.setPrice(sr.price());
            r.setTag(sr.tag());
            r.setActive(true);
            riders.add(r);
        }
        riderRepository.saveAll(riders);

        List<Stage> stages = new ArrayList<>();
        for (SeedDataService.SeedStage ss : seedDataService.loadStages()) {
            Stage s = new Stage();
            s.setPouleId(code);
            s.setStageNumber(ss.number());
            s.setLabel(ss.label());
            s.setStageTag(ss.tag());
            s.setKm(ss.km());
            s.setElev(ss.elev());
            s.setNoteEn(ss.noteEn());
            s.setNoteNl(ss.noteNl());
            s.setFavorites(ss.favorites() != null ? ss.favorites() : List.of());
            stages.add(s);
        }
        stageRepository.saveAll(stages);

        return getBundle(code);
    }

    public PouleBundle getBundle(String code) {
        Poule poule = getOrThrow(code);
        List<StageDto> stages = stageRepository.findByPouleIdOrderByStageNumberAsc(code)
                .stream().map(StageDto::from).collect(Collectors.toList());
        List<RiderDto> riders = riderRepository.findByPouleIdOrderByPriceDescNameAsc(code)
                .stream().map(RiderDto::from).collect(Collectors.toList());
        return new PouleBundle(PouleDto.from(poule, stages), riders);
    }

    public Poule getOrThrow(String code) {
        return pouleRepository.findById(normalizeCode(code))
                .orElseThrow(() -> ApiException.notFound("Couldn't find a poule with that code. Double-check it with your organizer."));
    }

    public void assertAdmin(String code, String rawPassword) {
        Poule poule = getOrThrow(code);
        if (rawPassword == null || !passwordEncoder.matches(rawPassword, poule.getAdminPasswordHash())) {
            throw ApiException.forbidden("That passphrase doesn't match.");
        }
    }

    @Transactional
    public PouleBundle updateSettings(String code, String adminPassword, UpdateSettingsRequest req) {
        assertAdmin(code, adminPassword);
        Poule poule = getOrThrow(code);
        poule.setName(req.name().trim());
        poule.setTeamSize(req.teamSize());
        poule.setBudgetCap(req.budgetCap());
        poule.setTotalSwaps(req.totalSwaps());
        pouleRepository.save(poule);
        return getBundle(code);
    }

    @Transactional
    public void delete(String code, String adminPassword) {
        assertAdmin(code, adminPassword);
        deleteInternal(normalizeCode(code));
    }

    /** Used only by SiteAdminService, which checks its own site-wide secret instead. */
    @Transactional
    public void deleteAsSiteAdmin(String code) {
        deleteInternal(normalizeCode(code));
    }

    public List<Poule> listAll() {
        return pouleRepository.findAll();
    }

    private void deleteInternal(String id) {
        playerTeamRepository.deleteAll(playerTeamRepository.findByPouleId(id));
        riderRepository.deleteByPouleId(id);
        stageRepository.deleteAll(stageRepository.findByPouleIdOrderByStageNumberAsc(id));
        pouleRepository.deleteById(id);
    }

    public String normalizeCode(String code) {
        return code == null ? null : code.trim().toUpperCase();
    }

    private String generateUniqueCode() {
        for (int attempt = 0; attempt < 20; attempt++) {
            String candidate = randomCode();
            if (!pouleRepository.existsById(candidate)) return candidate;
        }
        throw new IllegalStateException("Could not generate a unique poule code, please retry");
    }

    private String randomCode() {
        StringBuilder sb = new StringBuilder(5);
        for (int i = 0; i < 5; i++) sb.append(CODE_CHARS.charAt(RANDOM.nextInt(CODE_CHARS.length())));
        return sb.toString();
    }
}
