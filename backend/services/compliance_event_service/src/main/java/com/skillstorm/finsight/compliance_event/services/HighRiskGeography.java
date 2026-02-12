package com.skillstorm.finsight.compliance_event.services;

import java.util.List;
import java.util.Set;

/**
 * Lightweight high-risk geography heuristic.
 *
 * MVP notes:
 * - Locations currently come from cash_transaction.location (often a branch code or free-text).
 * - We treat this as a keyword match to keep the scoring trigger in place.
 * - Replace/extend with a proper risk rating service or geo enrichment when available.
 */
public final class HighRiskGeography {

    private HighRiskGeography() {}

    // Keep this small and obvious. This is *not* a sanctions list.
    private static final Set<String> KEYWORDS = Set.of(
            "panama",
            "cayman",
            "bvi",
            "british virgin",
            "macau",
            "hong kong",
            "dubai",
            "uae",
            "cyprus",
            "malta"
    );

    public static boolean isHighRisk(List<String> locations) {
        if (locations == null || locations.isEmpty()) {
            return false;
        }

        for (String loc : locations) {
            if (loc == null) {
                continue;
            }
            String s = loc.toLowerCase();
            for (String kw : KEYWORDS) {
                if (s.contains(kw)) {
                    return true;
                }
            }
        }
        return false;
    }
}
