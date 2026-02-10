package com.skillstorm.finsight.suspect_registry.dtos.response;

import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.skillstorm.finsight.suspect_registry.models.AddressType;

public record LinkedAddressResponse(
        Long addressId,
        String line1,
        String line2,
        String city,
        String state,
        String postalCode,
        String country,
        Instant createdAt,
        @JsonProperty("addressType") AddressType addressType,
        @JsonProperty("isCurrent") boolean isCurrent
) {
}
