package com.tdfpoule.controller;

import com.tdfpoule.dto.CsvImportRequest;
import com.tdfpoule.dto.CsvImportResult;
import com.tdfpoule.dto.PouleBundle;
import com.tdfpoule.dto.StageResultRequest;
import com.tdfpoule.service.StageService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/poules/{code}/stages/{stageNumber}")
public class StageController {

    private final StageService stageService;

    public StageController(StageService stageService) {
        this.stageService = stageService;
    }

    @PutMapping
    public PouleBundle saveResult(@PathVariable String code, @PathVariable int stageNumber,
                                   @RequestHeader("X-Admin-Password") String adminPassword,
                                   @Valid @RequestBody StageResultRequest req) {
        return stageService.saveResult(code, stageNumber, adminPassword, req);
    }

    @PostMapping("/import")
    public CsvImportResult importCsv(@PathVariable String code, @PathVariable int stageNumber,
                                      @RequestHeader("X-Admin-Password") String adminPassword,
                                      @Valid @RequestBody CsvImportRequest req) {
        return stageService.importCsv(code, stageNumber, adminPassword, req);
    }
}
