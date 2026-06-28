package com.tdfpoule.service;

import com.tdfpoule.dto.*;
import com.tdfpoule.entity.Poule;
import com.tdfpoule.entity.Stage;
import com.tdfpoule.exception.ApiException;
import com.tdfpoule.repository.PouleRepository;
import com.tdfpoule.repository.RiderRepository;
import com.tdfpoule.repository.StageRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class StageService {

    private final StageRepository stageRepository;
    private final PouleRepository pouleRepository;
    private final RiderRepository riderRepository;
    private final PouleService pouleService;
    private final ScoringService scoringService;
    private final StageCsvImportService csvImportService;

    public StageService(StageRepository stageRepository, PouleRepository pouleRepository,
                         RiderRepository riderRepository, PouleService pouleService,
                         ScoringService scoringService, StageCsvImportService csvImportService) {
        this.stageRepository = stageRepository;
        this.pouleRepository = pouleRepository;
        this.riderRepository = riderRepository;
        this.pouleService = pouleService;
        this.scoringService = scoringService;
        this.csvImportService = csvImportService;
    }

    @Transactional
    public PouleBundle saveResult(String code, int stageNumber, String adminPassword, StageResultRequest req) {
        String id = pouleService.normalizeCode(code);
        pouleService.assertAdmin(id, adminPassword);
        Stage stage = stageRepository.findByPouleIdAndStageNumber(id, stageNumber)
                .orElseThrow(() -> ApiException.notFound("Stage " + stageNumber + " not found"));

        List<String> finishOrder = padOrTrim(req.finishOrder(), 15);
        List<String> gcOrder = padOrTrim(req.gcOrder(), 10);
        Map<String, String> jerseys = req.jerseys() != null ? new LinkedHashMap<>(req.jerseys()) : new LinkedHashMap<>();

        stage.setTopRiderIds(finishOrder);
        stage.setGcRiderIds(gcOrder);
        stage.setJerseys(jerseys);
        stage.setPointsByRider(scoringService.computeStagePoints(finishOrder, jerseys));
        stage.setLocked(true);
        stageRepository.save(stage);

        Poule poule = pouleService.getOrThrow(id);
        poule.setCurrentStage(Math.max(poule.getCurrentStage(), stageNumber));
        pouleRepository.save(poule);

        return pouleService.getBundle(id);
    }

    @Transactional
    public CsvImportResult importCsv(String code, int stageNumber, String adminPassword, CsvImportRequest req) {
        String id = pouleService.normalizeCode(code);
        pouleService.assertAdmin(id, adminPassword);

        List<RiderDto> riders = riderRepository.findByPouleIdOrderByPriceDescNameAsc(id)
                .stream().map(RiderDto::from).collect(Collectors.toList());
        StageCsvImportService.ParsedCsv parsed = csvImportService.parse(riders, req.csvText());

        int matched = parsed.rows() - parsed.unmatchedNames().size();
        if (parsed.rows() == 0) {
            return new CsvImportResult(0, 0, parsed.unmatchedNames(), false);
        }
        if (!parsed.unmatchedNames().isEmpty()) {
            return new CsvImportResult(parsed.rows(), matched, parsed.unmatchedNames(), false);
        }

        saveResult(id, stageNumber, adminPassword,
                new StageResultRequest(parsed.finishOrder(), parsed.gcOrder(), parsed.jerseys()));
        return new CsvImportResult(parsed.rows(), matched, List.of(), true);
    }

    private List<String> padOrTrim(List<String> in, int size) {
        List<String> out = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            String v = (in != null && i < in.size()) ? in.get(i) : null;
            out.add((v == null || v.isBlank()) ? null : v);
        }
        return out;
    }
}
