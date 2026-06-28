package com.tdfpoule.service;

import com.tdfpoule.dto.AdminPouleSummaryDto;
import com.tdfpoule.exception.ApiException;
import com.tdfpoule.repository.PlayerTeamRepository;
import com.tdfpoule.repository.RiderRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * A single shared secret that can see/delete EVERY poule on this deployment, bypassing each
 * poule's own organizer passphrase entirely -- the recovery tool for "I lost the code to a
 * test poule and just want it gone." Deliberately separate from PouleService.assertAdmin,
 * which only ever grants access to one poule at a time.
 */
@Service
public class SiteAdminService {

    private final RiderRepository riderRepository;
    private final PlayerTeamRepository playerTeamRepository;
    private final PouleService pouleService;

    @Value("${app.site-admin-secret}")
    private String siteAdminSecret;

    public SiteAdminService(RiderRepository riderRepository, PlayerTeamRepository playerTeamRepository,
                             PouleService pouleService) {
        this.riderRepository = riderRepository;
        this.playerTeamRepository = playerTeamRepository;
        this.pouleService = pouleService;
    }

    public void assertSiteAdmin(String providedSecret) {
        if (siteAdminSecret == null || siteAdminSecret.isBlank()) {
            throw ApiException.forbidden("Site admin access isn't configured on this deployment (SITE_ADMIN_SECRET isn't set).");
        }
        if (providedSecret == null || !constantTimeEquals(siteAdminSecret, providedSecret)) {
            throw ApiException.forbidden("That site admin secret doesn't match.");
        }
    }

    public List<AdminPouleSummaryDto> listAll(String providedSecret) {
        assertSiteAdmin(providedSecret);
        return pouleService.listAll().stream()
                .map(p -> new AdminPouleSummaryDto(
                        p.getId(),
                        p.getName(),
                        p.getCreatedAt(),
                        p.getCurrentStage(),
                        riderRepository.countByPouleId(p.getId()),
                        playerTeamRepository.countByPouleId(p.getId())
                ))
                .sorted(Comparator.comparing(AdminPouleSummaryDto::createdAt).reversed())
                .collect(Collectors.toList());
    }

    public void delete(String providedSecret, String code) {
        assertSiteAdmin(providedSecret);
        pouleService.deleteAsSiteAdmin(code);
    }

    private boolean constantTimeEquals(String a, String b) {
        return MessageDigest.isEqual(a.getBytes(StandardCharsets.UTF_8), b.getBytes(StandardCharsets.UTF_8));
    }
}
