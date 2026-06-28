package com.tdfpoule.service;

import com.tdfpoule.dto.BulkRidersRequest;
import com.tdfpoule.dto.RiderDto;
import com.tdfpoule.dto.RiderUpsertRequest;
import com.tdfpoule.entity.Rider;
import com.tdfpoule.exception.ApiException;
import com.tdfpoule.repository.RiderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class RiderService {

    private final RiderRepository riderRepository;
    private final PouleService pouleService;

    public RiderService(RiderRepository riderRepository, PouleService pouleService) {
        this.riderRepository = riderRepository;
        this.pouleService = pouleService;
    }

    public List<RiderDto> list(String code) {
        String id = pouleService.normalizeCode(code);
        pouleService.getOrThrow(id);
        return riderRepository.findByPouleIdOrderByPriceDescNameAsc(id)
                .stream().map(RiderDto::from).collect(Collectors.toList());
    }

    @Transactional
    public RiderDto add(String code, String adminPassword, RiderUpsertRequest req) {
        String id = pouleService.normalizeCode(code);
        pouleService.assertAdmin(id, adminPassword);
        Rider r = new Rider();
        r.setId(id + "-r-" + UUID.randomUUID());
        r.setPouleId(id);
        applyUpsert(r, req);
        riderRepository.save(r);
        return RiderDto.from(r);
    }

    @Transactional
    public RiderDto update(String code, String riderId, String adminPassword, RiderUpsertRequest req) {
        String id = pouleService.normalizeCode(code);
        pouleService.assertAdmin(id, adminPassword);
        Rider r = riderRepository.findById(riderId)
                .filter(x -> x.getPouleId().equals(id))
                .orElseThrow(() -> ApiException.notFound("Rider not found"));
        applyUpsert(r, req);
        riderRepository.save(r);
        return RiderDto.from(r);
    }

    @Transactional
    public void delete(String code, String riderId, String adminPassword) {
        String id = pouleService.normalizeCode(code);
        pouleService.assertAdmin(id, adminPassword);
        Rider r = riderRepository.findById(riderId)
                .filter(x -> x.getPouleId().equals(id))
                .orElseThrow(() -> ApiException.notFound("Rider not found"));
        riderRepository.delete(r);
    }

    @Transactional
    public List<RiderDto> bulkReplace(String code, String adminPassword, BulkRidersRequest req) {
        String id = pouleService.normalizeCode(code);
        pouleService.assertAdmin(id, adminPassword);
        riderRepository.deleteByPouleId(id);
        List<Rider> riders = req.riders().stream().map(line -> {
            Rider r = new Rider();
            r.setId(id + "-r-" + UUID.randomUUID());
            r.setPouleId(id);
            applyUpsert(r, line);
            return r;
        }).collect(Collectors.toList());
        riderRepository.saveAll(riders);
        return riders.stream().map(RiderDto::from).collect(Collectors.toList());
    }

    private void applyUpsert(Rider r, RiderUpsertRequest req) {
        r.setName(req.name().trim());
        r.setTeam(req.team().trim());
        r.setPrice(req.price());
        r.setTag(req.tag().trim());
        r.setActive(req.active() == null || req.active());
    }
}
