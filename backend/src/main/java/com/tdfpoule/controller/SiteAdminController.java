package com.tdfpoule.controller;

import com.tdfpoule.dto.AdminPouleSummaryDto;
import com.tdfpoule.service.SiteAdminService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
public class SiteAdminController {

    private final SiteAdminService siteAdminService;

    public SiteAdminController(SiteAdminService siteAdminService) {
        this.siteAdminService = siteAdminService;
    }

    @GetMapping("/poules")
    public List<AdminPouleSummaryDto> list(@RequestHeader(value = "X-Site-Admin-Secret", required = false) String secret) {
        return siteAdminService.listAll(secret);
    }

    @DeleteMapping("/poules/{code}")
    public void delete(@PathVariable String code, @RequestHeader(value = "X-Site-Admin-Secret", required = false) String secret) {
        siteAdminService.delete(secret, code);
    }
}
