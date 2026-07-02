package com.tdfpoule.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/** Loads the seed rider pool and the 21-stage route from bundled JSON resources. */
@Service
public class SeedDataService {

    public record SeedRider(String name, String team, int price, String tag, String nat) {}

    public record SeedStage(int number, String label, String tag, Integer km, Integer elev,
                             String noteEn, String noteNl, List<String> favorites) {}

    private final ObjectMapper objectMapper;

    @Autowired
    public SeedDataService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public List<SeedRider> loadRiders() {
        return readJson("seed/riders.json", SeedRider[].class);
    }

    public List<SeedStage> loadStages() {
        return readJson("seed/stages.json", SeedStage[].class);
    }

    private <T> List<T> readJson(String classpathLocation, Class<T[]> arrayType) {
        try (InputStream in = new ClassPathResource(classpathLocation).getInputStream()) {
            T[] arr = objectMapper.readValue(in, arrayType);
            return List.of(arr);
        } catch (IOException e) {
            throw new IllegalStateException("Could not load seed data from " + classpathLocation, e);
        }
    }
}
