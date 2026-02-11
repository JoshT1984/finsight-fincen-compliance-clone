package com.skillstorm.finsight.compliance_event.services;

import java.util.List;
import java.util.Set;

public final class AddressAnomaly {

    private AddressAnomaly() {
    }

    private static final Set<String> KEYWORDS = Set.of(
            "no known address",
            "no home address");

    public static boolean isUnknownOrMissing(List<String> locations) {
        if (locations == null || locations.isEmpty())
            return false;

        for (String loc : locations) {
            if (loc == null)
                continue;
            String s = loc.toLowerCase();
            for (String kw : KEYWORDS) {
                if (s.contains(kw))
                    return true;
            }
        }
        return false;
    }
}
