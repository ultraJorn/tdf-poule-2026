package com.tdfpoule.dto;

import java.util.List;
import java.util.Map;

/**
 * finishOrder: up to 15 entries, index 0 = stage winner; null/blank entries mean "not entered".
 * gcOrder: up to 10 entries, the overall standings after this stage (optional).
 * jerseys: keys among yellow/green/polka/white -> riderId.
 */
public record StageResultRequest(
        List<String> finishOrder,
        List<String> gcOrder,
        Map<String, String> jerseys
) {
}
