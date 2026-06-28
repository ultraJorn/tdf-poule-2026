package com.tdfpoule.controller;

import com.tdfpoule.dto.BulkRidersRequest;
import com.tdfpoule.dto.RiderDto;
import com.tdfpoule.dto.RiderUpsertRequest;
import com.tdfpoule.service.RiderService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/poules/{code}/riders")
public class RiderController {

    private final RiderService riderService;

    public RiderController(RiderService riderService) {
        this.riderService = riderService;
    }

    @GetMapping
    public List<RiderDto> list(@PathVariable String code) {
        return riderService.list(code);
    }

    @PostMapping
    public RiderDto add(@PathVariable String code,
                         @RequestHeader("X-Admin-Password") String adminPassword,
                         @Valid @RequestBody RiderUpsertRequest req) {
        return riderService.add(code, adminPassword, req);
    }

    @PutMapping("/{riderId}")
    public RiderDto update(@PathVariable String code, @PathVariable String riderId,
                            @RequestHeader("X-Admin-Password") String adminPassword,
                            @Valid @RequestBody RiderUpsertRequest req) {
        return riderService.update(code, riderId, adminPassword, req);
    }

    @DeleteMapping("/{riderId}")
    public void delete(@PathVariable String code, @PathVariable String riderId,
                        @RequestHeader("X-Admin-Password") String adminPassword) {
        riderService.delete(code, riderId, adminPassword);
    }

    @PutMapping
    public List<RiderDto> bulkReplace(@PathVariable String code,
                                       @RequestHeader("X-Admin-Password") String adminPassword,
                                       @Valid @RequestBody BulkRidersRequest req) {
        return riderService.bulkReplace(code, adminPassword, req);
    }
}
