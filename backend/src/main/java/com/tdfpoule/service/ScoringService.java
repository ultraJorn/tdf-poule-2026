package com.tdfpoule.service;

import com.tdfpoule.dto.StageBreakdownDto;
import com.tdfpoule.dto.TeamTotalDto;
import com.tdfpoule.entity.PlayerTeam;
import com.tdfpoule.entity.Stage;
import com.tdfpoule.entity.SwapLogEntry;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Direct port of the scoring rules from the original single-file app:
 * finish-order points, jersey bonuses, and how a player's roster evolves
 * stage-by-stage as swaps take effect.
 */
@Service
public class ScoringService {

    public static final int[] FINISH_POINTS = {25, 20, 16, 14, 12, 10, 8, 7, 6, 5, 4, 3, 2, 1, 1};

    public static final Map<String, Integer> JERSEY_BONUS = Map.of(
            "yellow", 8,
            "green", 5,
            "polka", 5,
            "white", 3
    );

    /** What a player's roster looked like once swaps effective through {@code stageNum} are applied. */
    public List<String> rosterAtStage(List<String> baseRiders, List<SwapLogEntry> swapLog, int stageNum) {
        List<String> roster = new ArrayList<>(baseRiders);
        List<SwapLogEntry> sorted = new ArrayList<>(swapLog == null ? List.of() : swapLog);
        sorted.sort((a, b) -> Integer.compare(a.getEffectiveFromStage(), b.getEffectiveFromStage()));
        for (SwapLogEntry sw : sorted) {
            if (sw.getEffectiveFromStage() > stageNum) continue;
            int idx = roster.indexOf(sw.getOutId());
            if (idx != -1) {
                roster.set(idx, sw.getInId());
            } else if (!roster.contains(sw.getInId())) {
                roster.add(sw.getInId());
            }
        }
        return roster;
    }

    /** Points earned by each rider in one stage: finish-order points plus jersey bonuses. */
    public Map<String, Integer> computeStagePoints(List<String> finishOrder, Map<String, String> jerseys) {
        Map<String, Integer> pts = new LinkedHashMap<>();
        if (finishOrder != null) {
            for (int i = 0; i < finishOrder.size(); i++) {
                String riderId = finishOrder.get(i);
                if (riderId == null || riderId.isBlank()) continue;
                int gained = i < FINISH_POINTS.length ? FINISH_POINTS[i] : 0;
                pts.merge(riderId, gained, Integer::sum);
            }
        }
        if (jerseys != null) {
            for (Map.Entry<String, String> e : jerseys.entrySet()) {
                String riderId = e.getValue();
                if (riderId == null || riderId.isBlank()) continue;
                int bonus = JERSEY_BONUS.getOrDefault(e.getKey(), 0);
                pts.merge(riderId, bonus, Integer::sum);
            }
        }
        return pts;
    }

    /** A team's running total and per-stage breakdown across every locked stage. */
    public TeamTotalDto teamTotal(PlayerTeam team, List<Stage> stages) {
        int total = 0;
        List<StageBreakdownDto> breakdown = new ArrayList<>();
        for (Stage stage : stages) {
            if (!stage.isLocked()) continue;
            List<String> roster = rosterAtStage(team.getRiderIds(), team.getSwapLog(), stage.getStageNumber());
            int stagePts = 0;
            for (String riderId : roster) {
                Integer p = stage.getPointsByRider().get(riderId);
                if (p != null) stagePts += p;
            }
            breakdown.add(new StageBreakdownDto(stage.getStageNumber(), stagePts));
            total += stagePts;
        }
        return new TeamTotalDto(total, breakdown);
    }
}
