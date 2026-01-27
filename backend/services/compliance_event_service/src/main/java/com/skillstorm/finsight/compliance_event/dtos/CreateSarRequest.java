package com.skillstorm.finsight.compliance_event.dtos;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateSarRequest(

                @NotBlank @Size(max = 64) String sourceSystem,

                @NotBlank @Size(max = 64) String sourceEntityId,

                @Size(max = 128) String externalSubjectKey,

                @NotNull Instant eventTime,

                @DecimalMin(value = "0.00", inclusive = true) BigDecimal totalAmount,

                @Size(max = 50000) String narrative,

                Instant activityStart,
                Instant activityEnd,

                Map<String, Object> formData,

                @Min(0) @Max(100) Integer severityScore) {
}
