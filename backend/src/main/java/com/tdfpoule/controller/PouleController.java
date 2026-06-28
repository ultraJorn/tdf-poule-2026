package com.tdfpoule.controller;

import com.tdfpoule.dto.CreatePouleRequest;
import com.tdfpoule.dto.PouleBundle;
import com.tdfpoule.dto.UpdateSettingsRequest;
import com.tdfpoule.service.PouleService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/poules")
public class PouleController {

    private final PouleService pouleService;

    public PouleController(PouleService pouleService) {
        this.pouleService = pouleService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PouleBundle create(@Valid @RequestBody CreatePouleRequest req) {
        return pouleService.create(req);
    }

    @GetMapping("/{code}")
    public PouleBundle get(@PathVariable String code) {
        return pouleService.getBundle(code);
    }

    @PutMapping("/{code}/settings")
    public PouleBundle updateSettings(@PathVariable String code,
                                       @RequestHeader("X-Admin-Password") String adminPassword,
                                       @Valid @RequestBody UpdateSettingsRequest req) {
        return pouleService.updateSettings(code, adminPassword, req);
    }

    @DeleteMapping("/{code}")
    public void delete(@PathVariable String code, @RequestHeader("X-Admin-Password") String adminPassword) {
        pouleService.delete(code, adminPassword);
    }

    @PostMapping("/{code}/auth")
    public void checkAdminPassword(@PathVariable String code, @RequestHeader("X-Admin-Password") String adminPassword) {
        pouleService.assertAdmin(code, adminPassword);
    }
}
