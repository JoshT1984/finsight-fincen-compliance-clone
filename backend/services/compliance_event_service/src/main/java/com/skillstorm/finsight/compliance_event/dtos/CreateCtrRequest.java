package com.skillstorm.finsight.compliance_event.dtos;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Map;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateCtrRequest(

        @NotBlank String sourceSystem,

        @NotBlank String sourceEntityId,

        @Size(max = 128) String externalSubjectKey,

        @NotNull OffsetDateTime eventTime,

        @NotNull @DecimalMin(value = "0.00") BigDecimal totalAmount,

        @NotBlank @Size(max = 128) String customerName,

        // Matches schema: compliance_event_ctr_detail.transaction_time TIMESTAMPTZ NOT
        // NULL
        @NotNull OffsetDateTime transactionTime,

        // Recommend allowing null and defaulting to {} in service
        Map<String, Object> ctrFormData) {
}
