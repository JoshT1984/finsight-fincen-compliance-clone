package com.skillstorm.finsight.compliance_event.dtos;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateCtrRequest(

        @NotBlank @Size(max = 64) String sourceSystem,

        @NotBlank @Size(max = 64) String sourceEntityId,

        @Size(max = 128) String externalSubjectKey,

        @NotNull Instant eventTime,

        @NotNull @DecimalMin(value = "0.00", inclusive = true) BigDecimal totalAmount,

        @NotBlank @Size(max = 128) String customerName,

        // Matches schema: compliance_event_ctr_detail.transaction_time TIMESTAMP(3) NOT
        @NotNull Instant transactionTime,

        // Allow null and default to {} in service
        Map<String, Object> ctrFormData) {
}
