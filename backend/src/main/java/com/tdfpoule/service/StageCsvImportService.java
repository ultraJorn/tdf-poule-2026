package com.tdfpoule.service;

import com.tdfpoule.dto.RiderDto;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Port of the original parseStageCSV/normalizeName/findRiderByName JS helpers: lets an
 * organizer paste "place,rider name" / "yellow,rider name" / "gc3,rider name" lines instead
 * of using the dropdowns one at a time.
 */
@Service
public class StageCsvImportService {

    private static final Set<String> HEADER_WORDS = Set.of("place", "plaats", "positie", "rider", "renner", "naam", "name");
    private static final List<String> JERSEY_KEYS = List.of("yellow", "green", "polka", "white");
    private static final Pattern GC_PATTERN = Pattern.compile("^gc\\s*(\\d{1,2})$");

    public record ParsedCsv(List<String> finishOrder, List<String> gcOrder, Map<String, String> jerseys,
                             int rows, List<String> unmatchedNames) {}

    public ParsedCsv parse(List<RiderDto> riders, String text) {
        List<String> finishOrder = new ArrayList<>(Collections.nCopies(15, null));
        List<String> gcOrder = new ArrayList<>(Collections.nCopies(10, null));
        Map<String, String> jerseys = new LinkedHashMap<>();
        List<String> unmatched = new ArrayList<>();
        int rows = 0;

        for (String rawLine : text.split("\n")) {
            String line = rawLine.trim();
            if (line.isEmpty()) continue;
            int idx = line.indexOf(',');
            if (idx == -1) continue;
            String rawKey = line.substring(0, idx).trim().toLowerCase();
            String rawName = line.substring(idx + 1).trim();
            if (HEADER_WORDS.contains(rawKey) || rawName.isEmpty()) continue;

            String slotType = null;
            int slotIndex = -1;
            String jerseyKey = null;

            Matcher gc = GC_PATTERN.matcher(rawKey);
            if (JERSEY_KEYS.contains(rawKey)) {
                slotType = "jersey";
                jerseyKey = rawKey;
            } else if (gc.matches()) {
                int g = Integer.parseInt(gc.group(1));
                if (g >= 1 && g <= 10) { slotType = "gc"; slotIndex = g - 1; }
            } else {
                try {
                    int n = Integer.parseInt(rawKey);
                    if (n >= 1 && n <= 15) { slotType = "place"; slotIndex = n - 1; }
                } catch (NumberFormatException ignored) { /* unrecognized key, skip */ }
            }
            if (slotType == null) continue;

            rows++;
            Optional<RiderDto> rider = findRiderByName(riders, rawName);
            if (rider.isEmpty()) { unmatched.add(rawName); continue; }

            switch (slotType) {
                case "place" -> finishOrder.set(slotIndex, rider.get().id());
                case "gc" -> gcOrder.set(slotIndex, rider.get().id());
                case "jersey" -> jerseys.put(jerseyKey, rider.get().id());
            }
        }
        return new ParsedCsv(finishOrder, gcOrder, jerseys, rows, unmatched);
    }

    private String normalizeName(String s) {
        if (s == null) return "";
        String stripped = Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("[\\p{M}]", "");
        return stripped.toLowerCase().trim().replaceAll("\\s+", " ");
    }

    private Optional<RiderDto> findRiderByName(List<RiderDto> riders, String name) {
        String norm = normalizeName(name);
        if (norm.isEmpty()) return Optional.empty();

        Optional<RiderDto> exact = riders.stream().filter(r -> normalizeName(r.name()).equals(norm)).findFirst();
        if (exact.isPresent()) return exact;

        // forgiving fallback: match on surname/single word if it's unambiguous
        List<RiderDto> matches = riders.stream().filter(r -> {
            List<String> parts = List.of(normalizeName(r.name()).split(" "));
            return (!parts.isEmpty() && parts.get(parts.size() - 1).equals(norm)) || parts.contains(norm);
        }).toList();
        return matches.size() == 1 ? Optional.of(matches.get(0)) : Optional.empty();
    }
}
