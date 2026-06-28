package com.tdfpoule.service;

import com.tdfpoule.entity.Stage;
import com.tdfpoule.repository.StageRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Runs once per backend startup so poules created before GPX support existed (e.g. the
 * already-live production poule) get the real km/elev/elevationProfile too, not just poules
 * created from now on. This is purely derived data (organizers never edit km/elev/profile
 * themselves), so unconditionally recomputing from the bundled GPX on every startup is safe
 * and keeps every poule in sync if the GPX files or parsing logic ever change.
 */
@Component
public class GpxBackfillRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(GpxBackfillRunner.class);

    private final StageRepository stageRepository;
    private final GpxRouteParser gpxRouteParser;

    public GpxBackfillRunner(StageRepository stageRepository, GpxRouteParser gpxRouteParser) {
        this.stageRepository = stageRepository;
        this.gpxRouteParser = gpxRouteParser;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        List<Stage> stages = stageRepository.findAll();
        int updated = 0;
        for (Stage stage : stages) {
            gpxRouteParser.applyToStage(stage);
            updated++;
        }
        if (updated > 0) {
            stageRepository.saveAll(stages);
            log.info("GPX backfill: refreshed route data for {} stage rows across all poules", updated);
        }
    }
}
