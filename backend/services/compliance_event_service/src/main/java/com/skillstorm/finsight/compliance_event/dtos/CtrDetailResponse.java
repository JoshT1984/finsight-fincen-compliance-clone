package com.skillstorm.finsight.compliance_event.dtos;

import java.util.List;
import java.util.Map;

public record CtrDetailResponse(
        ComplianceEventResponse ctr,
        Map<String, Object> ctrFormData,
        List<Long> contributingTxnIds,
        List<ComplianceEventResponse> priorCtrs
) {}
