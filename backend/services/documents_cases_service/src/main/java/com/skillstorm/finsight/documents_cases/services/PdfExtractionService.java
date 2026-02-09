package com.skillstorm.finsight.documents_cases.services;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.skillstorm.finsight.documents_cases.dtos.compliance.CreateCtrRequestPayload;
import com.skillstorm.finsight.documents_cases.dtos.compliance.CreateSarRequestPayload;
import com.skillstorm.finsight.documents_cases.models.DocumentType;

/**
 * Extracts text from FinCEN Form 104 (CTR) and Form 109 (SAR) PDFs and builds
 * compliance payloads for creating CTR/SAR records. Uses PDFBox for text extraction
 * and heuristic parsing; stores raw text in form data for audit.
 */
@Service
public class PdfExtractionService {

    private static final Logger log = LoggerFactory.getLogger(PdfExtractionService.class);
    private static final String SOURCE_SYSTEM = "PDF_IMPORT";

    /**
     * Extracts text from the PDF and parses it into a CTR or SAR request payload.
     * If parsing fails or is partial, returns a minimal payload with full raw text in form data.
     *
     * @param inputStream PDF content (caller responsible for closing if needed).
     * @param documentType CTR or SAR.
     * @return CreateCtrRequestPayload or CreateSarRequestPayload (cast by caller based on documentType).
     */
    public Object extractAndParse(InputStream inputStream, DocumentType documentType) throws IOException {
        String rawText = extractText(inputStream);
        if (rawText == null || rawText.isBlank()) {
            log.warn("PDF produced no extractable text (may be image-only or encrypted)");
        }
        String sourceEntityId = "PDF_" + UUID.randomUUID();
        Instant now = Instant.now();

        switch (documentType) {
            case CTR -> { return parseForm104(rawText != null ? rawText : "", sourceEntityId, now); }
            case SAR -> { return parseForm109(rawText != null ? rawText : "", sourceEntityId, now); }
            default -> throw new IllegalArgumentException("PDF extraction only supported for CTR or SAR, got: " + documentType);
        }
    }

    /**
     * Extract raw text from a PDF using PDFBox.
     */
    public String extractText(InputStream inputStream) throws IOException {
        try (PDDocument document = Loader.loadPDF(inputStream.readAllBytes())) {
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(document);
        } catch (IOException e) {
            log.error("PDF text extraction failed: {}", e.getMessage());
            throw new IOException("Failed to extract text from PDF: " + e.getMessage(), e);
        }
    }

    /**
     * Parse extracted text as FinCEN Form 104 (CTR). Matches labels from the official form:
     * Item 28 Date of transaction; Item 26/27 Total cash in/out; Items 2-4 Individual/entity name.
     */
    private CreateCtrRequestPayload parseForm104(String rawText, String sourceEntityId, Instant fallbackTime) {
        Map<String, Object> ctrFormData = new HashMap<>();
        ctrFormData.put("_rawText", rawText);

        String customerName = "(Extracted from PDF)";
        Instant transactionTime = fallbackTime;

        // Form 104 Item 28: "28 Date of transaction" or "Date of transaction" followed by MM/DD/YYYY
        Pattern datePattern = Pattern.compile(
            "(?:28\\s+)?(?:date\\s+of\\s+transaction|transaction\\s+date)\\s*:?\\s*(\\d{1,2}[-/]\\d{1,2}[-/]\\d{2,4})",
            Pattern.CASE_INSENSITIVE);
        Matcher dateMatcher = datePattern.matcher(rawText);
        if (dateMatcher.find()) {
            try {
                String dateStr = dateMatcher.group(1).replace('-', '/');
                LocalDate d = LocalDate.parse(dateStr, DateTimeFormatter.ofPattern("M/d/yyyy"));
                transactionTime = d.atStartOfDay(ZoneOffset.UTC).toInstant();
                ctrFormData.put("_parsedTransactionDate", dateStr);
            } catch (DateTimeParseException e) {
                try {
                    LocalDate d = LocalDate.parse(dateMatcher.group(1), DateTimeFormatter.ofPattern("MM/dd/yy"));
                    transactionTime = d.atStartOfDay(ZoneOffset.UTC).toInstant();
                    ctrFormData.put("_parsedTransactionDate", dateMatcher.group(1));
                } catch (DateTimeParseException e2) {
                    log.debug("Could not parse CTR transaction date: {}", dateMatcher.group(1));
                }
            }
        }

        // Form 104 Items 26/27: "Total cash in $", "Total cash out $", or "26 Total cash in", "27 Total cash out"
        Pattern amountPattern = Pattern.compile(
            "(?:26\\s+)?(?:total\\s+cash\\s+in|total\\s+currency|total\\s+amount)\\s*\\$?\\s*([\\d,]+(?:\\.\\d{2})?)" +
            "|(?:27\\s+)?(?:total\\s+cash\\s+out|total\\s+amount)\\s*\\$?\\s*([\\d,]+(?:\\.\\d{2})?)",
            Pattern.CASE_INSENSITIVE);
        Matcher amountMatcher = amountPattern.matcher(rawText);
        BigDecimal cashIn = BigDecimal.ZERO;
        BigDecimal cashOut = BigDecimal.ZERO;
        while (amountMatcher.find()) {
            try {
                String g1 = amountMatcher.group(1);
                String g2 = amountMatcher.group(2);
                String amtStr = (g1 != null && !g1.isEmpty()) ? g1 : (g2 != null ? g2 : null);
                if (amtStr != null) {
                    BigDecimal amt = new BigDecimal(amtStr.replace(",", ""));
                    if (g1 != null && !g1.isEmpty()) cashIn = cashIn.max(amt);
                    else cashOut = cashOut.max(amt);
                }
            } catch (NumberFormatException ignored) { }
        }
        BigDecimal totalAmount = cashIn.max(cashOut);
        if (totalAmount.compareTo(BigDecimal.ZERO) > 0) {
            ctrFormData.put("_parsedTotalAmount", totalAmount.toPlainString());
        }

        // Form 104 Items 2–4: "Individual's last name or entity's name", "First name", "3 First name", "2 Individual's last name"
        Pattern namePattern = Pattern.compile(
            "(?:2\\s+)?(?:individual'?s?\\s+last\\s+name\\s+or\\s+entity'?s?\\s+name|individual'?s?\\s+last\\s+name|entity'?s?\\s+name)" +
            "\\s*:?\\s*([A-Za-z][A-Za-z\\s.,'-]{2,100})" +
            "|(?:3\\s+)?first\\s+name\\s*:?\\s*([A-Za-z][A-Za-z\\s.,'-]{2,100})",
            Pattern.CASE_INSENSITIVE);
        Matcher nameMatcher = namePattern.matcher(rawText);
        String lastOrEntity = null;
        String first = null;
        while (nameMatcher.find()) {
            String last = nameMatcher.group(1);
            String firstCap = nameMatcher.group(2);
            if (last != null && !last.trim().isEmpty() && last.length() <= 128) lastOrEntity = last.trim();
            if (firstCap != null && !firstCap.trim().isEmpty() && firstCap.length() <= 128) first = firstCap.trim();
        }
        if (lastOrEntity != null || first != null) {
            customerName = (first != null && lastOrEntity != null) ? (first + " " + lastOrEntity) : (first != null ? first : lastOrEntity);
            ctrFormData.put("_parsedCustomerName", customerName);
        }

        return new CreateCtrRequestPayload(
                SOURCE_SYSTEM,
                sourceEntityId,
                null,
                fallbackTime,
                totalAmount,
                customerName,
                transactionTime,
                ctrFormData);
    }

    /**
     * Parse extracted text as FinCEN Form 109 (SAR-MSB). Narrative is in Part VI;
     * Item *16 date range of suspicious activity; *17 total amount involved.
     */
    private CreateSarRequestPayload parseForm109(String rawText, String sourceEntityId, Instant fallbackTime) {
        Map<String, Object> formData = new HashMap<>();
        formData.put("_rawText", rawText);

        String narrative;
        if (rawText != null && !rawText.isBlank()) {
            // Form 109 narrative is in Part VI "Suspicious Activity Information - Narrative"
            int part6 = rawText.toLowerCase().indexOf("part vi");
            int narrativeLabel = rawText.toLowerCase().indexOf("suspicious activity information - narrative");
            int start = -1;
            if (part6 >= 0) start = part6;
            if (narrativeLabel >= 0 && (start < 0 || narrativeLabel < start)) start = narrativeLabel;
            if (start >= 0) {
                String section = rawText.substring(start, Math.min(rawText.length(), start + 50000));
                narrative = section.length() > 50000 ? section.substring(0, 49999) : section.trim();
                formData.put("_narrativeSection", "Part VI");
            } else {
                narrative = rawText.length() > 50000 ? rawText.substring(0, 49999) : rawText;
            }
        } else {
            narrative = "(No text extracted from PDF)";
        }

        Instant activityStart = null;
        Instant activityEnd = null;
        // Form 109 *16: "Date or date range of suspicious activity" — "From ___/___/___" and "To ___/___/___"
        Pattern fromToPattern = Pattern.compile(
            "from\\s+(\\d{1,2}[-/]\\d{1,2}[-/]\\d{2,4})\\s*(?:\\s+to\\s+(\\d{1,2}[-/]\\d{1,2}[-/]\\d{2,4}))?",
            Pattern.CASE_INSENSITIVE);
        Matcher fromToMatcher = fromToPattern.matcher(rawText != null ? rawText : "");
        if (fromToMatcher.find()) {
            try {
                String fromStr = fromToMatcher.group(1).replace('-', '/');
                LocalDate fromDate = LocalDate.parse(fromStr, DateTimeFormatter.ofPattern("M/d/yyyy"));
                activityStart = fromDate.atStartOfDay(ZoneOffset.UTC).toInstant();
                formData.put("_parsedActivityStart", fromStr);
                String toStr = fromToMatcher.group(2);
                if (toStr != null && !toStr.isEmpty()) {
                    toStr = toStr.replace('-', '/');
                    LocalDate toDate = LocalDate.parse(toStr, DateTimeFormatter.ofPattern("M/d/yyyy"));
                    activityEnd = toDate.atStartOfDay(ZoneOffset.UTC).toInstant();
                    formData.put("_parsedActivityEnd", toStr);
                }
            } catch (DateTimeParseException e) {
                try {
                    String fromStr = fromToMatcher.group(1).replace('-', '/');
                    LocalDate fromDate = LocalDate.parse(fromStr, DateTimeFormatter.ofPattern("MM/dd/yy"));
                    activityStart = fromDate.atStartOfDay(ZoneOffset.UTC).toInstant();
                    formData.put("_parsedActivityStart", fromStr);
                } catch (DateTimeParseException ignored) { }
            }
        }
        if (activityStart == null) {
            Pattern anyDate = Pattern.compile("(\\d{1,2}[-/]\\d{1,2}[-/]\\d{2,4})");
            Matcher m = anyDate.matcher(rawText != null ? rawText : "");
            if (m.find()) {
                try {
                    String dateStr = m.group(1).replace('-', '/');
                    LocalDate d = LocalDate.parse(dateStr, DateTimeFormatter.ofPattern("M/d/yyyy"));
                    activityStart = d.atStartOfDay(ZoneOffset.UTC).toInstant();
                    formData.put("_parsedActivityStart", dateStr);
                } catch (DateTimeParseException e) {
                    try {
                        LocalDate d = LocalDate.parse(m.group(1), DateTimeFormatter.ofPattern("MM/dd/yy"));
                        activityStart = d.atStartOfDay(ZoneOffset.UTC).toInstant();
                    } catch (DateTimeParseException ignored) { }
                }
            }
        }

        Integer severityScore = 50; // default when not derivable

        return new CreateSarRequestPayload(
                SOURCE_SYSTEM,
                sourceEntityId,
                null,
                fallbackTime,
                null,
                narrative,
                activityStart,
                activityEnd,
                formData,
                severityScore);
    }
}
