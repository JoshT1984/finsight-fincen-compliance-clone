package com.skillstorm.finsight.compliance_event.dtos;

public record GenerateCtrResponse(
    int candidatesFound,
    int ctrsCreated,
    int ctrsSkippedAlreadyExists
) {}

