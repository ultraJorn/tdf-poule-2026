package com.tdfpoule.dto;

import java.util.List;

/** Everything the frontend needs after creating or opening a poule. */
public record PouleBundle(PouleDto poule, List<RiderDto> riders) {
}
