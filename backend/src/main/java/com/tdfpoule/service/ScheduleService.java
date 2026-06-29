package com.tdfpoule.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

/**
 * Loads schedule/tour_de_france_2026_schedule.json (confirmed/estimated 2026 route dates and
 * CEST start times) and answers "is the pre-race free-swap window open right now?" using the
 * real Europe/Paris clock -- not server-local time and not each player's browser timezone --
 * so the cutoff actually lines up with when the race starts in reality.
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

    @Value("${app.free-swap-window-hours:24}")
    private int freeSwapWindowHours;

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

    public Instant getFreeSwapWindowStart() {
        return getStage1Start().minus(Duration.ofHours(freeSwapWindowHours));
    }

    /** True from {@code freeSwapWindowHours} before stage 1 starts, until it actually starts. */
    public boolean isFreeSwapWindowActive() {
        Instant now = Instant.now();
        return !now.isBefore(getFreeSwapWindowStart()) && now.isBefore(getStage1Start());
    }
}
