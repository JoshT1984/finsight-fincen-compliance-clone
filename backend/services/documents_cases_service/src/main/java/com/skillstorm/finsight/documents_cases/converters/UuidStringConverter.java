package com.skillstorm.finsight.documents_cases.converters;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.UUID;

/**
 * JPA converter so that Java {@link UUID} is persisted as a string (e.g. "1657cc58-05be-11f1-94f0-02fa4003976b")
 * to VARCHAR columns. Without this, Hibernate sends the UUID as binary bytes, which MySQL rejects with
 * "Incorrect string value" when the column charset is utf8/utf8mb4.
 */
@Converter(autoApply = false)
public class UuidStringConverter implements AttributeConverter<UUID, String> {

    @Override
    public String convertToDatabaseColumn(UUID attribute) {
        return attribute == null ? null : attribute.toString();
    }

    @Override
    public UUID convertToEntityAttribute(String dbData) {
        return dbData == null ? null : UUID.fromString(dbData);
    }
}
