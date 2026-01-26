package com.skillstorm.finsight.compliance_event.dtos;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Map;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateSarRequest(

        @NotBlank String sourceSystem,

        @NotBlank String sourceEntityId,

        @Size(max = 128) String externalSubjectKey,

        @NotNull OffsetDateTime eventTime,

        @NotNull @DecimalMin(value = "0.00") BigDecimal totalAmount,

        @Size(max = 50000) String narrative,

        OffsetDateTime activityStart,
        OffsetDateTime activityEnd,

        // You can keep @NotNull if you truly require it.
        // If you want to allow omitted formData, remove @NotNull and default it in
        // service.
        @NotNull Map<String, Object> formData,

        @Min(0) @Max(100) Integer severityScore) {
}
