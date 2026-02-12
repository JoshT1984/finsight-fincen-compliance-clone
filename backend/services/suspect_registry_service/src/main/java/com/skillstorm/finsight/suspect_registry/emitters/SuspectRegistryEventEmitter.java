package com.skillstorm.finsight.suspect_registry.emitters;

import com.skillstorm.finsight.suspect_registry.loggers.SuspectRegistryEventLog;

public interface SuspectRegistryEventEmitter {
    void emit(SuspectRegistryEventLog event);
}
