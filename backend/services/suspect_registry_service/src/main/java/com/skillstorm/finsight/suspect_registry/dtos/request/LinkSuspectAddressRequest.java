package com.skillstorm.finsight.suspect_registry.dtos.request;

import com.skillstorm.finsight.suspect_registry.models.AddressType;

import jakarta.validation.constraints.NotNull;

public record LinkSuspectAddressRequest(
        @NotNull(message = "Address ID is required")
        Long addressId,

        AddressType addressType,

        Boolean isCurrent
) {
}
