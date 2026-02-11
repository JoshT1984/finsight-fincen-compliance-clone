package com.skillstorm.finsight.compliance_event.emitters;

import com.skillstorm.finsight.compliance_event.loggers.ComplianceEventLog;

public interface ComplianceEventEmitter {
    void emit(ComplianceEventLog event);
}
