package com.tdfpoule.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

/**
 * Loads schedule/tour_de_france_2026_schedule.json (confirmed/estimated 2026 route dates and
 * CEST start times) so the app can show each stage's real calendar date and a countdown to
 * stage 1, computed against the real Europe/Paris clock rather than server-local time.
 *
 * Free/uncapped pre-race swaps are gated on the poule's own currentStage (see
 * TeamService.swap()), not on wall-clock time -- this service is purely informational now.
 */
@Service
public class ScheduleService {

    private static final ZoneId RACE_ZONE = ZoneId.of("Europe/Paris");

    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record ScheduleStage(int stage, String date, String day, String start, String finish, String type,
                                 String startTimeCest, String startTimeNote) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    record ScheduleFile(List<ScheduleStage> stages) {}

    private final List<ScheduleStage> stages;

    public ScheduleService(ObjectMapper objectMapper) {
        try (InputStream in = new ClassPathResource("schedule/tour_de_france_2026_schedule.json").getInputStream()) {
            this.stages = objectMapper.readValue(in, ScheduleFile.class).stages();
        } catch (Exception e) {
            throw new IllegalStateException("Could not load tour_de_france_2026_schedule.json", e);
        }
    }

    public List<ScheduleStage> getStages() {
        return stages;
    }

    public ScheduleStage getStage(int stageNumber) {
        return stages.stream().filter(s -> s.stage() == stageNumber).findFirst().orElse(null);
    }

    /** When stage 1 actually starts in real time, parsed from its confirmed CEST date + time. */
    public Instant getStage1Start() {
        ScheduleStage s1 = getStage(1);
        return ZonedDateTime.of(LocalDate.parse(s1.date()), LocalTime.parse(s1.startTimeCest()), RACE_ZONE).toInstant();
    }
}
