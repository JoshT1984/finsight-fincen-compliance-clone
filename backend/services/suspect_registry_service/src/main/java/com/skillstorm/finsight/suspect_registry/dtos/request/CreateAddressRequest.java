package com.skillstorm.finsight.suspect_registry.dtos.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateAddressRequest(
        @NotBlank(message = "Line 1 is required")
        @Size(max = 256)
        String line1,

        @Size(max = 256)
        String line2,

        @NotBlank(message = "City is required")
        @Size(max = 128)
        String city,

        @Size(max = 64)
        String state,

        @Size(max = 32)
        String postalCode,

        @NotBlank(message = "Country is required")
        @Size(max = 64)
        String country
) {
}
