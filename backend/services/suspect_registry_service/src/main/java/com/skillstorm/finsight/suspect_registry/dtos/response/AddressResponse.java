package com.skillstorm.finsight.suspect_registry.dtos.response;

import java.time.Instant;

public record AddressResponse(
        Long addressId,
        String line1,
        String line2,
        String city,
        String state,
        String postalCode,
        String country,
        String addressHash,
        Instant createdAt
) {
}
