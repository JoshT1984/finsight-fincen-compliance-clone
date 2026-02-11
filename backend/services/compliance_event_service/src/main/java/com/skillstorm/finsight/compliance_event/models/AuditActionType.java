package com.skillstorm.finsight.compliance_event.models;

public enum AuditActionType {
    CTR_CREATED,
    CTR_FLAGGED_FOR_REVIEW,

    SAR_AUTO_GENERATED,
    SAR_MANUAL_CREATED,
    SAR_ESCALATED,

    SYSTEM_DECISION,
    ANALYST_DECISION
}
