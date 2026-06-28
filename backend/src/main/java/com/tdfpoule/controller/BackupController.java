package com.tdfpoule.controller;

import com.tdfpoule.dto.BackupBundle;
import com.tdfpoule.dto.PouleBundle;
import com.tdfpoule.dto.RestoreRequest;
import com.tdfpoule.service.BackupService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class BackupController {

    private final BackupService backupService;

    public BackupController(BackupService backupService) {
        this.backupService = backupService;
    }

    @GetMapping("/poules/{code}/backup")
    public BackupBundle export(@PathVariable String code, @RequestHeader("X-Admin-Password") String adminPassword) {
        return backupService.export(code, adminPassword);
    }

    @PostMapping("/restore")
    public PouleBundle restore(@Valid @RequestBody RestoreRequest req) {
        return backupService.restore(req);
    }
}
