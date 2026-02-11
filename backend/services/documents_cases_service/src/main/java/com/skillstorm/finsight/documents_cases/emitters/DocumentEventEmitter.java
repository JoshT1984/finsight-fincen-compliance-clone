package com.skillstorm.finsight.documents_cases.emitters;

import com.skillstorm.finsight.documents_cases.loggers.DocumentEventLog;

public interface DocumentEventEmitter {
    void emit(DocumentEventLog event);
}
