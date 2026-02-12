package com.skillstorm.finsight.suspect_registry.loggers;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public record SuspectRegistryEventLog(
        Instant timestamp,
        String entityType, // "ADDRESS", "SUSPECT", etc.
        String entityId,

        String eventType, // "CREATED", "UPDATED"
        String trigger, // "USER", "SYSTEM"
        String rule,
        String idempotencyKey,

        Map<String, Object> metadata) {

    public static SuspectRegistryEventLog of(
            String entityType,
            String entityId,
            String eventType,
            String trigger,
            String rule,
            String idempotencyKey,
            Map<String, Object> metadata) {

        return new SuspectRegistryEventLog(
                Instant.now(),
                entityType,
                entityId,
                eventType,
                trigger,
                rule,
                idempotencyKey,
                metadata);
    }

    // ---- Convenience helpers ----

    public static SuspectRegistryEventLog addressCreated(
            String addressId,
            String trigger,
            Map<String, Object> metadata) {

        return of("ADDRESS", addressId, "CREATED", trigger, null, "ADDRESS_CREATED:" + addressId, metadata);
    }

    public static SuspectRegistryEventLog addressUpdated(
            String addressId,
            String trigger,
            Map<String, Object> metadata) {
        return of("ADDRESS", addressId, "UPDATED", trigger, null, "ADDRESS_UPDATED:" + addressId, metadata);
    }

    public static SuspectRegistryEventLog addressDeleted(
            String addressId,
            String trigger,
            Map<String, Object> metadata) {
        return of("ADDRESS", addressId, "DELETED", trigger, null, "ADDRESS_DELETED:" + addressId, metadata);
    }

    public static SuspectRegistryEventLog aliasCreated(
            String aliasId,
            String trigger,
            Map<String, Object> metadata) {
        return of("ALIAS", aliasId, "CREATED", trigger, null, "ALIAS_CREATED:" + aliasId, metadata);
    }

    public static SuspectRegistryEventLog aliasUpdated(
            String aliasId,
            String trigger,
            Map<String, Object> metadata) {
        return of("ALIAS", aliasId, "UPDATED", trigger, null, "ALIAS_UPDATED:" + aliasId, metadata);
    }

    public static SuspectRegistryEventLog aliasDeleted(
            String aliasId,
            String trigger,
            Map<String, Object> metadata) {
        return of("ALIAS", aliasId, "DELETED", trigger, null, "ALIAS_DELETED:" + aliasId, metadata);
    }

    public static SuspectRegistryEventLog organizationCreated(
            String orgId,
            String trigger,
            Map<String, Object> metadata) {
        return of("ORGANIZATION", orgId, "CREATED", trigger, null, "ORGANIZATION_CREATED:" + orgId, metadata);
    }

    public static SuspectRegistryEventLog organizationUpdated(
            String orgId,
            String trigger,
            Map<String, Object> metadata) {
        return of("ORGANIZATION", orgId, "UPDATED", trigger, null, "ORGANIZATION_UPDATED:" + orgId, metadata);
    }

    public static SuspectRegistryEventLog organizationDeleted(
            String orgId,
            String trigger,
            Map<String, Object> metadata) {
        return of("ORGANIZATION", orgId, "DELETED", trigger, null, "ORGANIZATION_DELETED:" + orgId, metadata);
    }

    public static SuspectRegistryEventLog suspectCreated(
            String suspectId,
            String trigger,
            Map<String, Object> metadata) {
        return of("SUSPECT", suspectId, "CREATED", trigger, null, "SUSPECT_CREATED:" + suspectId, metadata);
    }

    public static SuspectRegistryEventLog suspectUpdated(
            String suspectId,
            String trigger,
            Map<String, Object> metadata) {
        return of("SUSPECT", suspectId, "UPDATED", trigger, null, "SUSPECT_UPDATED:" + suspectId, metadata);
    }

    public static SuspectRegistryEventLog suspectDeleted(
            String suspectId,
            String trigger,
            Map<String, Object> metadata) {
        return of("SUSPECT", suspectId, "DELETED", trigger, null, "SUSPECT_DELETED:" + suspectId, metadata);
    }
}