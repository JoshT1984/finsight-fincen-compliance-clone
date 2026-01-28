package com.skillstorm.finsight.suspect_registry.dtos.request;

import jakarta.validation.constraints.Size;

public record PatchAddressRequest(
        @Size(max = 256)
        String line1,

        @Size(max = 256)
        String line2,

        @Size(max = 128)
        String city,

        @Size(max = 64)
        String state,

        @Size(max = 32)
        String postalCode,

        @Size(max = 64)
        String country,

        @Size(max = 64)
        String addressHash
) {
}
