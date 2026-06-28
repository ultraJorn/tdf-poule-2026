package com.tdfpoule.dto;

import java.util.List;

/** Replaces the entire rider list for a poule, e.g. once the final startlist is known. */
public record BulkRidersRequest(List<RiderUpsertRequest> riders) {
}
