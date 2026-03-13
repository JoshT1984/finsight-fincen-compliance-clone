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
import org.apache.pdfbox.pdmodel.interactive.form.PDAcroForm;
import org.apache.pdfbox.pdmodel.interactive.form.PDField;
import org.apache.pdfbox.text.PDFTextStripper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.skillstorm.finsight.documents_cases.dtos.compliance.CreateCtrRequestPayload;
import com.skillstorm.finsight.documents_cases.dtos.compliance.CreateSarRequestPayload;
import com.skillstorm.finsight.documents_cases.exceptions.DocumentValidationException;
import com.skillstorm.finsight.documents_cases.models.DocumentType;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

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
        String text = rawText != null ? rawText : "";
        assertPdfMatchesFinCENForm(text, documentType);
        String sourceEntityId = "PDF_" + UUID.randomUUID();
        Instant now = Instant.now();

        switch (documentType) {
            case CTR -> { return parseForm104(text, sourceEntityId, now); }
            case SAR -> { return parseForm109(text, sourceEntityId, now); }
            default -> throw new IllegalArgumentException("PDF extraction only supported for CTR or SAR, got: " + documentType);
        }
    }

    /**
     * Verifies that the extracted PDF text matches the expected FinCEN form (104 for CTR, 109 for SAR).
     * Rejects uploads that are not the correct form type so we do not parse or store non‑conforming documents.
     *
     * @param rawText Combined text extracted from the PDF (page text + form field values).
     * @param documentType CTR or SAR (expects Form 104 or Form 109 respectively).
     * @throws DocumentValidationException if the document does not appear to be the expected form.
     */
    public void assertPdfMatchesFinCENForm(String rawText, DocumentType documentType) {
        if (rawText == null) rawText = "";
        String lower = rawText.toLowerCase();
        List<String> errors = new ArrayList<>();
        if (documentType == DocumentType.CTR) {
            // FinCEN Form 104 (Currency Transaction Report) identifiers
            boolean hasForm104 = lower.contains("fincen form 104") || lower.contains("form 104")
                    || lower.contains("currency transaction report") || (lower.contains("ctr") && lower.contains("fincen"));
            boolean hasCtrIndicators = lower.contains("item 26") || lower.contains("item 27") || lower.contains("item 28")
                    || lower.contains("total cash in") || lower.contains("total cash out") || lower.contains("date of transaction");
            if (!hasForm104 && !hasCtrIndicators) {
                errors.add("This document does not appear to be a FinCEN Form 104 (Currency Transaction Report). " +
                        "Please upload a valid CTR form. Expected to see form title 'FinCEN Form 104' or 'Currency Transaction Report' and form items (e.g. Item 26, 27, 28).");
            }
        } else if (documentType == DocumentType.SAR) {
            // FinCEN Form 109 (SAR-MSB) identifiers
            boolean hasForm109 = lower.contains("fincen form 109") || lower.contains("form 109")
                    || (lower.contains("suspicious activity report") && (lower.contains("fincen") || lower.contains("msb")));
            boolean hasSarIndicators = lower.contains("part vi") || lower.contains("suspicious activity")
                    || lower.contains("sar") && (lower.contains("narrative") || lower.contains("suspicious"));
            if (!hasForm109 && !hasSarIndicators) {
                errors.add("This document does not appear to be a FinCEN Form 109 (Suspicious Activity Report). " +
                        "Please upload a valid SAR form. Expected to see form title 'FinCEN Form 109' or 'Suspicious Activity Report' and sections such as Part VI (narrative).");
            }
        }
        if (!errors.isEmpty()) {
            throw new DocumentValidationException(errors);
        }
    }

    /**
     * Extract raw text from a PDF using PDFBox: page text plus any AcroForm field values
     * (fillable form data like name, amount, date), so parsing can find them even when
     * they are not rendered in the page content.
     */
    public String extractText(InputStream inputStream) throws IOException {
        byte[] bytes = inputStream.readAllBytes();
        try (PDDocument document = Loader.loadPDF(bytes)) {
            PDFTextStripper stripper = new PDFTextStripper();
            String pageText = stripper.getText(document);
            StringBuilder combined = new StringBuilder(pageText != null ? pageText : "");
            PDAcroForm acroForm = document.getDocumentCatalog().getAcroForm();
            if (acroForm != null) {
                combined.append(" \u200BFORM_VALUES "); // sentinel so parsing can restrict name/amount search to filled values
                StringBuilder fieldBlock = new StringBuilder();
                Pattern itemNumberInName = Pattern.compile("\\b(\\d{1,2})\\b");
                for (PDField field : acroForm.getFieldTree()) {
                    try {
                        String value = field.getValueAsString();
                        if (value != null && !value.isBlank()) {
                            combined.append(" ").append(value.trim());
                        }
                        // Build FIELD_N=value block for fixed-form mapping (skip "Off" so only real values get keys)
                        String name = field.getFullyQualifiedName();
                        if (name != null && value != null && !value.isBlank() && !"Off".equalsIgnoreCase(value.trim())) {
                            Matcher numMatcher = itemNumberInName.matcher(name);
                            if (numMatcher.find()) {
                                int itemNum = Integer.parseInt(numMatcher.group(1));
                                if (itemNum >= 1 && itemNum <= 50) {
                                    // Do not put date-like or numeric values into name fields (2,3,4 CTR; 4,5,6 SAR)
                                    if (isNameFieldItem(itemNum) && looksLikeDateOrNumber(value.trim())) {
                                        continue;
                                    }
                                    fieldBlock.append("\u200BFIELD_").append(itemNum).append("=").append(value.trim()).append("\u200B");
                                }
                            }
                        }
                    } catch (Exception e) {
                        log.trace("Could not read form field {}: {}", field.getFullyQualifiedName(), e.getMessage());
                    }
                }
                if (fieldBlock.length() > 0) {
                    combined.append(" \u200BFIELD_VALUES ").append(fieldBlock);
                }
            }
            return combined.toString();
        } catch (IOException e) {
            log.error("PDF text extraction failed: {}", e.getMessage());
            throw new IOException("Failed to extract text from PDF: " + e.getMessage(), e);
        }
    }

    /**
     * Parse extracted text as FinCEN Form 104 (CTR). Matches labels from the official form:
     * Item 28 Date of transaction; Item 26/27 Total cash in/out; Items 2-4 Individual/entity name.
     */
    private static final int MAX_RAW_TEXT_LENGTH = 100_000;

    /**
     * Prefix for externalSubjectKey when we have a parsed SSN.
     * Stored as "SSN:123456789" in compliance_event.external_subject_key so the compliance service can
     * identify it as an SSN and link the event to a suspect by SSN (see SuspectRegistryClient.extractSsnFromSubjectKey).
     * If you need only the 9-digit value in the DB, downstream can strip this prefix.
     */
    private static final String SSN_SUBJECT_PREFIX = "SSN:";

    /** Normalizes SSN to 9 digits (strips dashes/spaces). Returns null if not exactly 9 digits. */
    private static String normalizeSsn(String s) {
        if (s == null || s.isBlank()) return null;
        String digits = s.replaceAll("[^0-9]", "");
        return digits.length() == 9 ? digits : null;
    }

    /**
     * Parses SSN from form text. Looks for Item 5 / "Social security number" / "SSN" and
     * xxx-xx-xxxx or 9 consecutive digits. Returns normalized 9-digit string or null.
     */
    private static String parseSsnFromText(String rawText) {
        if (rawText == null || rawText.isBlank()) return null;
        // Match SSN-like patterns: 123-45-6789 or 123 45 6789 or 123456789 (with word boundaries so we don't grab 10+ digits)
        Pattern ssnPattern = Pattern.compile(
            "(?:5\\s+)?(?:social\\s+security\\s+number|ssn|employer\\s+identification\\s+number)\\s*:?\\s*([0-9]{3}[-\\s]?[0-9]{2}[-\\s]?[0-9]{4})" +
            "|\\b([0-9]{3}[-\\s][0-9]{2}[-\\s][0-9]{4})\\b" +
            "|\\b([0-9]{9})\\b(?!\\d)");
        Matcher m = ssnPattern.matcher(rawText);
        while (m.find()) {
            String g1 = m.group(1);
            String g2 = m.group(2);
            String g3 = m.group(3);
            String candidate = g1 != null ? g1 : (g2 != null ? g2 : g3);
            if (candidate != null) {
                String normalized = normalizeSsn(candidate);
                if (normalized != null) return normalized;
            }
        }
        return null;
    }

    /**
     * Parses Form 104 address (Items 8–10: street, city/state/ZIP, country) and puts
     * _parsedAddressLine1, _parsedAddressCity, _parsedAddressState, _parsedAddressPostalCode,
     * _parsedAddressCountry into ctrFormData when found.
     */
    private static void parseCtrAddress(String rawText, Map<String, Object> ctrFormData) {
        if (rawText == null || rawText.isBlank() || ctrFormData == null) return;
        // Skip if already set from FIELD_VALUES (fixed template)
        if (ctrFormData.containsKey("_parsedAddressLine1") && ctrFormData.containsKey("_parsedAddressCity")) {
            return;
        }
        // Item 8: Permanent address (Number and Street)
        Pattern streetPattern = Pattern.compile(
            "(?:8\\s+)?(?:permanent\\s+address|number\\s+and\\s+street|street\\s+address)\\s*:?\\s*([A-Za-z0-9\\s.,#'-]{5,120}?)(?=\\s*[\\r\\n]|\\s*9\\s|\\s*$)",
            Pattern.CASE_INSENSITIVE);
        Matcher streetMatcher = streetPattern.matcher(rawText);
        if (streetMatcher.find()) {
            String line1 = streetMatcher.group(1).trim();
            if (line1.length() >= 5 && line1.length() <= 256 && !line1.matches("(?i).*individual.*|.*entity.*|.*organization.*")) {
                ctrFormData.put("_parsedAddressLine1", line1);
            }
        }
        // Item 9: City or town, State, and ZIP
        Pattern cityStateZip = Pattern.compile(
            "(?:9\\s+)?(?:city\\s+or\\s+town|city)\\s*,?\\s*([A-Za-z\\s.'-]{2,64}?)\\s*,?\\s*([A-Za-z]{2})\\s*,?\\s*([0-9]{5}(?:-[0-9]{4})?)",
            Pattern.CASE_INSENSITIVE);
        Matcher cszMatcher = cityStateZip.matcher(rawText);
        if (cszMatcher.find()) {
            ctrFormData.put("_parsedAddressCity", cszMatcher.group(1).trim());
            ctrFormData.put("_parsedAddressState", cszMatcher.group(2).trim());
            ctrFormData.put("_parsedAddressPostalCode", cszMatcher.group(3).trim());
        } else {
            // Fallback: "9" followed by city-like and state-like and zip
            Pattern fallback = Pattern.compile(
                "\\b9\\s+([A-Za-z][A-Za-z\\s.'-]{1,48}?)\\s+([A-Z][A-Z])\\s+([0-9]{5}(?:-[0-9]{4})?)\\b",
                Pattern.CASE_INSENSITIVE);
            Matcher fb = fallback.matcher(rawText);
            if (fb.find()) {
                ctrFormData.put("_parsedAddressCity", fb.group(1).trim());
                ctrFormData.put("_parsedAddressState", fb.group(2).trim().toUpperCase());
                ctrFormData.put("_parsedAddressPostalCode", fb.group(3).trim());
            }
        }
        // Item 10: Country
        Pattern countryPattern = Pattern.compile(
            "(?:10\\s+)?(?:country)\\s*:?\\s*([A-Za-z][A-Za-z\\s.'-]{2,60}?)(?=\\s*[\\r\\n]|\\s*\\d|\\s*$)",
            Pattern.CASE_INSENSITIVE);
        Matcher countryMatcher = countryPattern.matcher(rawText);
        if (countryMatcher.find()) {
            String country = countryMatcher.group(1).trim();
            if (country.length() <= 64 && !country.matches("(?i).*enter.*|.*see.*")) {
                ctrFormData.put("_parsedAddressCountry", country);
            }
        }
        // If we have form values section, also look for address-like line (number + street name)
        if (!ctrFormData.containsKey("_parsedAddressLine1")) {
            int formValues = rawText.indexOf("FORM_VALUES");
            if (formValues >= 0 && formValues < rawText.length() - 30) {
                String window = rawText.substring(Math.max(0, formValues - 200), rawText.length());
                Pattern anyStreet = Pattern.compile("\\b(\\d+\\s+[A-Za-z0-9][A-Za-z0-9\\s.,#'-]{4,80}?)(?=\\s+[A-Z][A-Z]\\s+[0-9]{5}|\\s*[\\r\\n])");
                Matcher am = anyStreet.matcher(window);
                if (am.find()) {
                    String line1 = am.group(1).trim();
                    if (line1.length() <= 256) ctrFormData.put("_parsedAddressLine1", line1);
                }
            }
        }
        // Default country if we have other address parts
        if (ctrFormData.containsKey("_parsedAddressCity") && !ctrFormData.containsKey("_parsedAddressCountry")) {
            ctrFormData.put("_parsedAddressCountry", "USA");
        }
    }

    /**
     * Parses Form 109 (SAR) subject/individual address and puts _parsedAddressLine1, _parsedAddressCity,
     * _parsedAddressState, _parsedAddressPostalCode, _parsedAddressCountry into formData when found.
     * Uses same keys as CTR so the compliance service can push to suspect addresses.
     */
    private static void parseSarAddress(String rawText, Map<String, Object> formData) {
        if (rawText == null || rawText.isBlank() || formData == null) return;
        // Prefer address from FORM_VALUES (fixed template) so we don't match instruction text
        int formValuesIdx = rawText.indexOf("FORM_VALUES");
        if (formValuesIdx >= 0 && formValuesIdx < rawText.length() - 20) {
            String formSection = rawText.substring(formValuesIdx);
            Pattern addrInForm = Pattern.compile(
                "\\b(\\d+\\s+[A-Za-z0-9][A-Za-z0-9\\s.,#'-]{4,80}?)\\s+([A-Za-z][A-Za-z\\s.'-]{2,48}?)\\s+([A-Z]{2})\\s+([0-9]{5}(?:-[0-9]{4})?)\\b");
            Matcher am = addrInForm.matcher(formSection);
            if (am.find()) {
                formData.put("_parsedAddressLine1", am.group(1).trim());
                formData.put("_parsedAddressCity", am.group(2).trim());
                formData.put("_parsedAddressState", am.group(3).trim().toUpperCase());
                formData.put("_parsedAddressPostalCode", am.group(4).trim());
                formData.put("_parsedAddressCountry", "USA");
                return;
            }
        }
        // SAR forms often have "Address", "Street", "Subject address" or similar
        Pattern streetPattern = Pattern.compile(
            "(?:address|street\\s+address|number\\s+and\\s+street|subject'?s?\\s+address)\\s*:?\\s*([A-Za-z0-9\\s.,#'-]{5,120}?)(?=\\s*[\\r\\n]|\\s*(?:city|state|zip|country)|\\s*$)",
            Pattern.CASE_INSENSITIVE);
        Matcher streetMatcher = streetPattern.matcher(rawText);
        if (streetMatcher.find()) {
            String line1 = streetMatcher.group(1).trim();
            if (line1.length() >= 5 && line1.length() <= 256 && !line1.matches("(?i).*narrative.*|.*part\\s+vi.*")) {
                formData.put("_parsedAddressLine1", line1);
            }
        }
        // City, State, ZIP — "City", "State", "ZIP" or "City or town, State, and ZIP"
        Pattern cityStateZip = Pattern.compile(
            "(?:city\\s+(?:or\\s+town)?\\s*,?\\s*)?([A-Za-z][A-Za-z\\s.'-]{2,64}?)\\s*,?\\s*([A-Za-z]{2})\\s*,?\\s*([0-9]{5}(?:-[0-9]{4})?)",
            Pattern.CASE_INSENSITIVE);
        Matcher cszMatcher = cityStateZip.matcher(rawText);
        if (cszMatcher.find()) {
            formData.put("_parsedAddressCity", cszMatcher.group(1).trim());
            formData.put("_parsedAddressState", cszMatcher.group(2).trim().toUpperCase());
            formData.put("_parsedAddressPostalCode", cszMatcher.group(3).trim());
        } else {
            Pattern fallback = Pattern.compile(
                "\\b([A-Za-z][A-Za-z\\s.'-]{1,48}?)\\s+([A-Z][A-Z])\\s+([0-9]{5}(?:-[0-9]{4})?)\\b",
                Pattern.CASE_INSENSITIVE);
            Matcher fb = fallback.matcher(rawText);
            if (fb.find()) {
                formData.put("_parsedAddressCity", fb.group(1).trim());
                formData.put("_parsedAddressState", fb.group(2).trim().toUpperCase());
                formData.put("_parsedAddressPostalCode", fb.group(3).trim());
            }
        }
        // Country
        Pattern countryPattern = Pattern.compile(
            "(?:country)\\s*:?\\s*([A-Za-z][A-Za-z\\s.'-]{2,60}?)(?=\\s*[\\r\\n]|\\s*\\d|\\s*$)",
            Pattern.CASE_INSENSITIVE);
        Matcher countryMatcher = countryPattern.matcher(rawText);
        if (countryMatcher.find()) {
            String country = countryMatcher.group(1).trim();
            if (country.length() <= 64 && !country.matches("(?i).*enter.*|.*see.*")) {
                formData.put("_parsedAddressCountry", country);
            }
        }
        // Fallback: address-like line (number + street) before city/state/zip in FORM_VALUES or nearby
        if (!formData.containsKey("_parsedAddressLine1")) {
            int formValues = rawText.indexOf("FORM_VALUES");
            if (formValues >= 0 && formValues < rawText.length() - 30) {
                String window = rawText.substring(Math.max(0, formValues - 300), rawText.length());
                Pattern anyStreet = Pattern.compile("\\b(\\d+\\s+[A-Za-z0-9][A-Za-z0-9\\s.,#'-]{4,80}?)(?=\\s+[A-Z][A-Z]\\s+[0-9]{5}|\\s*[\\r\\n])");
                Matcher am = anyStreet.matcher(window);
                if (am.find()) {
                    String line1 = am.group(1).trim();
                    if (line1.length() <= 256) formData.put("_parsedAddressLine1", line1);
                }
            }
        }
        if (formData.containsKey("_parsedAddressCity") && !formData.containsKey("_parsedAddressCountry")) {
            formData.put("_parsedAddressCountry", "USA");
        }
    }

    /** Excludes year-like values (1900–2099) so we don't use a date year as the transaction amount. */
    private static boolean isLikelyYear(BigDecimal v) {
        if (v == null || v.scale() > 0) return false;
        int i = v.intValue();
        return i >= 1900 && i <= 2099;
    }

    /** Common street suffixes so we don't use "St", "Ave", etc. as first/last name. */
    private static boolean isStreetSuffix(String word) {
        if (word == null || word.isBlank()) return false;
        String t = word.trim().toLowerCase();
        return t.equals("st") || t.equals("street") || t.equals("ave") || t.equals("avenue")
                || t.equals("blvd") || t.equals("boulevard") || t.equals("rd") || t.equals("road")
                || t.equals("ln") || t.equals("lane") || t.equals("dr") || t.equals("drive");
    }

    /** US state/territory 2-letter codes so we don't use "CT", "NY", etc. as first/last name. */
    private static final Set<String> STATE_ABBREVIATIONS = Set.of(
            "AL", "AK", "AZ", "AR", "CA", "CO", "CT", "DE", "FL", "GA", "HI", "ID", "IL", "IN", "IA",
            "KS", "KY", "LA", "ME", "MD", "MA", "MI", "MN", "MS", "MO", "MT", "NE", "NV", "NH", "NJ",
            "NM", "NY", "NC", "ND", "OH", "OK", "OR", "PA", "RI", "SC", "SD", "TN", "TX", "UT", "VT",
            "VA", "WA", "WV", "WI", "WY", "DC");

    private static boolean isStateAbbreviation(String word) {
        if (word == null || word.length() != 2) return false;
        return STATE_ABBREVIATIONS.contains(word.trim().toUpperCase());
    }

    /** Rejects captured "names" that are form boilerplate, street suffixes, or state abbreviations. */
    private static boolean isValidName(String s) {
        if (s == null || s.trim().isEmpty() || s.length() > 128) return false;
        if (isStreetSuffix(s) || isStateAbbreviation(s)) return false;
        String t = s.trim().toLowerCase();
        if (t.contains("item") || t.contains("section") || t.contains("transaction") || t.contains("person(")
                || t.contains("complete") || t.contains("check") || t.contains("instructions")
                || t.contains("behalf") || t.contains("whose") || t.startsWith("in ") || t.contains(" or ")
                || t.contains("entity's") || t.contains("organization")
                || t.equals("report") || t.equals("omb") || t.equals("form") || t.equals("currency")
                || t.equals("treasury") || t.equals("fincen") || t.equals("department") || t.equals("march")
                || t.equals("rev") || t.equals("previous") || t.equals("cat") || t.equals("no")
                || t.equals("doing") || t.equals("business") || t.equals("name") || t.equals("middle")
                || t.equals("initial") || t.equals("first") || t.equals("last") || t.equals("individual")
                || t.equals("entity") || t.equals("organization") || t.equals("individuals")
                || t.equals("enter") || t.equals("regulator") || t.equals("examiner") || t.equals("bsa")
                || t.equals("off") || t.equals("yes") || t.equals("no")) {
            return false;
        }
        return true;
    }

    /**
     * Extracts the SAR narrative from Part VI, skipping the form title and instructions
     * so that only the user-entered narrative (e.g. "test narrative text") is returned.
     */
    private static String extractSarNarrative(String rawText) {
        if (rawText == null || rawText.isBlank()) return null;
        String lower = rawText.toLowerCase();
        int part6 = lower.indexOf("part vi");
        int narrativeLabel = lower.indexOf("suspicious activity information - narrative");
        int sectionStart = -1;
        if (part6 >= 0) sectionStart = part6;
        if (narrativeLabel >= 0 && (sectionStart < 0 || narrativeLabel < sectionStart)) sectionStart = narrativeLabel;
        if (sectionStart < 0) return rawText.length() > 49999 ? rawText.substring(0, 49999) : rawText.trim();

        String section = rawText.substring(sectionStart, Math.min(rawText.length(), sectionStart + 55000));
        String sectionLower = section.toLowerCase();

        // Skip past the form header and instructions to find where user content starts.
        // Common instruction text on Form 109: "Explanation/description of suspicious activity(ies). This section of the report is critical..."
        int contentStart = 0;
        String[] instructionMarkers = {
            "explanation/description of suspicious activity",
            "this section of the report is critical",
            "the care with which it is completed may determine",
            "please type or print",
            "always complete entire report",
            "items marked with"
        };
        int lastInstructionEnd = -1;
        for (String marker : instructionMarkers) {
            int idx = sectionLower.indexOf(marker);
            if (idx >= 0) {
                // Find end of this phrase (next period + newline or next double newline)
                int after = idx + marker.length();
                int period = section.indexOf('.', after);
                if (period >= 0 && period - after < 300) {
                    int lineEnd = section.indexOf('\n', period);
                    if (lineEnd >= 0) lastInstructionEnd = Math.max(lastInstructionEnd, lineEnd + 1);
                    else lastInstructionEnd = Math.max(lastInstructionEnd, period + 1);
                } else {
                    int lineEnd = section.indexOf('\n', after);
                    if (lineEnd >= 0) lastInstructionEnd = Math.max(lastInstructionEnd, lineEnd + 1);
                }
            }
        }
        if (lastInstructionEnd >= 0) {
            contentStart = lastInstructionEnd;
            // Advance to next double newline so we start at the user's narrative paragraph
            int dbl = section.indexOf("\n\n", contentStart);
            if (dbl >= 0 && dbl - contentStart < 500) contentStart = dbl + 2;
            else {
                int dblCr = section.indexOf("\r\n\r\n", contentStart);
                if (dblCr >= 0 && dblCr - contentStart < 500) contentStart = dblCr + 4;
            }
        } else {
            // No known instructions found; skip first "paragraph" (up to double newline) after section title
            int firstNewline = section.indexOf('\n');
            if (firstNewline >= 0) {
                int secondNewline = section.indexOf('\n', firstNewline + 1);
                if (secondNewline >= 0 && secondNewline - firstNewline <= 2) {
                    contentStart = secondNewline + 1; // skip blank line
                } else {
                    // Skip first line (title) and take from there
                    contentStart = firstNewline + 1;
                }
            }
        }

        String candidate = section.substring(contentStart).trim();
        // If what remains looks like more instructions (starts with number, "part", etc.), skip that line too
        while (candidate.length() > 0) {
            int firstLineEnd = candidate.indexOf('\n');
            String firstLine = firstLineEnd >= 0 ? candidate.substring(0, firstLineEnd).trim() : candidate;
            if (firstLine.isEmpty()) {
                candidate = firstLineEnd >= 0 ? candidate.substring(firstLineEnd + 1).trim() : "";
                continue;
            }
            String fl = firstLine.toLowerCase();
            if (fl.startsWith("part ") || fl.matches("^\\d+\\s.*") && firstLine.length() < 80
                    || fl.contains("explanation/description") || fl.contains("this section of the report")) {
                candidate = firstLineEnd >= 0 ? candidate.substring(firstLineEnd + 1).trim() : "";
                continue;
            }
            break;
        }
        candidate = candidate.trim();
        candidate = candidate.trim();
        if (candidate.isEmpty()) return null;
        // Reject if result is clearly form instructions (numbered items, "General Instruction", etc.)
        String candLower = candidate.toLowerCase();
        if (candidate.length() > 500 && (candLower.contains("item 1.") || candLower.contains("general instruction")
                || candLower.startsWith("4.") || candLower.contains("check the box"))) {
            return null;
        }
        return candidate.length() > 50000 ? candidate.substring(0, 49999) : candidate;
    }

    /**
     * True if segment looks like subject name + address (e.g. "Johnson Sarah 453 Slate St Hartford CT 06119"),
     * not narrative prose. Such segments should be excluded when picking the narrative.
     */
    private static boolean looksLikeNameAndAddress(String segment) {
        if (segment == null || segment.length() < 15) return false;
        // Has 5-digit ZIP and street suffix (e.g. " St ") or 2-letter state + ZIP
        boolean hasZip = segment.matches(".*\\b[0-9]{5}(?:-[0-9]{4})?\\b.*");
        boolean hasStreetSuffix = segment.matches(".*\\s+St\\s+.*") || segment.matches(".*\\s+Street\\s+.*")
                || segment.matches(".*\\s+Ave\\s+.*") || segment.matches(".*\\s+Blvd\\s+.*");
        boolean hasStateAndZip = segment.matches(".*\\s+[A-Z]{2}\\s+[0-9]{5}\\b.*");
        return hasZip && (hasStreetSuffix || hasStateAndZip);
    }

    /**
     * From the FORM_VALUES section (concatenated form field values), extract the segment that
     * best represents the narrative. Fillable SAR often has many "Off" checkbox values and one
     * narrative field (e.g. "Suspicious activity test text"). We take the longest run that looks
     * like prose (not name+address, not a date, not a single number).
     */
    private static String extractNarrativeFromFormValues(String formValuesSection) {
        if (formValuesSection == null || formValuesSection.isBlank()) return null;
        // Split on "Off" (checkbox value) so we get segments like "Johnson Sarah", "02/10/2026", "Suspicious activity test text"
        String[] segments = formValuesSection.trim().split("\\s+Off\\s+");
        Pattern dateOnly = Pattern.compile("^\\d{1,2}[-/]\\d{1,2}[-/]\\d{2,4}$");
        Pattern numberOnly = Pattern.compile("^[\\d,.]+$");
        String best = null;
        int bestLen = 0;
        for (String seg : segments) {
            String s = seg.trim().replaceFirst("^(?i)Off\\s+", "").replaceFirst("\\s+(?i)Off$", "").trim();
            if (s.isEmpty() || s.length() < 10) continue;
            if (dateOnly.matcher(s).matches() || numberOnly.matcher(s).matches()) continue;
            if ("Off".equalsIgnoreCase(s)) continue;
            if (looksLikeNameAndAddress(s)) continue; // skip subject name + address block
            if (s.length() > bestLen && s.length() <= 50000) {
                bestLen = s.length();
                best = s;
            }
        }
        return best;
    }

    /** Tries multiple patterns to find Item 28 / Date of transaction; returns the date string or null. */
    private static String parseCtrTransactionDate(String rawText) {
        if (rawText == null || rawText.isBlank()) return null;
        // Pattern 1: "28 Date of transaction" or "Date of transaction" with date on same line (allow spaces/newlines before date)
        Pattern p1 = Pattern.compile(
            "(?:28\\s+)?(?:date\\s+of\\s+transaction|transaction\\s+date)\\s*:?[\\s\\r\\n]*(\\d{1,2}[-/]\\d{1,2}[-/]\\d{2,4})",
            Pattern.CASE_INSENSITIVE);
        Matcher m1 = p1.matcher(rawText);
        if (m1.find()) return m1.group(1);
        // Pattern 2: "28" then optional label then date (e.g. "28  01/15/2024" or "28  Date of transaction 01/15/2024")
        Pattern p2 = Pattern.compile("\\b28\\s+(?:date\\s+of\\s+transaction\\s*)?(\\d{1,2}[-/]\\d{1,2}[-/]\\d{2,4})", Pattern.CASE_INSENSITIVE);
        Matcher m2 = p2.matcher(rawText);
        if (m2.find()) return m2.group(1);
        // Pattern 3: Standalone date near "28" (e.g. line with "28" and next token is date)
        Pattern p3 = Pattern.compile("\\b28\\b[\\s\\r\\n]+(\\d{1,2}[-/]\\d{1,2}[-/]\\d{2,4})");
        Matcher m3 = p3.matcher(rawText);
        if (m3.find()) return m3.group(1);
        // Pattern 4: "28" then any whitespace then date (fillable PDFs often output "28  02/10/2026")
        Pattern p4 = Pattern.compile("\\b28\\s+(\\d{1,2}[-/]\\d{1,2}[-/]\\d{2,4})\\b");
        Matcher m4 = p4.matcher(rawText);
        if (m4.find()) return m4.group(1);
        // Pattern 5: Any valid-looking date in document (fallback when label order varies)
        Pattern p5 = Pattern.compile("\\b(\\d{1,2})/(\\d{1,2})/(\\d{4})\\b");
        Matcher m5 = p5.matcher(rawText);
        while (m5.find()) {
            try {
                int month = Integer.parseInt(m5.group(1));
                int day = Integer.parseInt(m5.group(2));
                int year = Integer.parseInt(m5.group(3));
                if (year >= 1990 && year <= 2030 && month >= 1 && month <= 12 && day >= 1 && day <= 31) {
                    return m5.group(0);
                }
            } catch (NumberFormatException ignored) { }
        }
        // Pattern 6: Date with spaces around slashes (e.g. "02 / 10 / 2026" from some PDFs)
        Pattern p6 = Pattern.compile("\\b(\\d{1,2})\\s*/\\s*(\\d{1,2})\\s*/\\s*(\\d{4})\\b");
        Matcher m6 = p6.matcher(rawText);
        while (m6.find()) {
            try {
                int month = Integer.parseInt(m6.group(1));
                int day = Integer.parseInt(m6.group(2));
                int year = Integer.parseInt(m6.group(3));
                if (year >= 1990 && year <= 2030 && month >= 1 && month <= 12 && day >= 1 && day <= 31) {
                    return m6.group(1) + "/" + m6.group(2) + "/" + m6.group(3);
                }
            } catch (NumberFormatException ignored) { }
        }
        return null;
    }

    /** Form 104 name items 2,3,4; Form 109 name items 4,5,6. */
    private static boolean isNameFieldItem(int itemNum) {
        return itemNum == 2 || itemNum == 3 || itemNum == 4 || itemNum == 5 || itemNum == 6;
    }

    /** True if value looks like a date (MM/DD/YYYY) or is mostly digits (e.g. SSN, amount). */
    private static boolean looksLikeDateOrNumber(String value) {
        if (value == null || value.isBlank()) return false;
        String v = value.trim();
        if (v.matches("\\d{1,2}[-/]\\d{1,2}[-/]\\d{2,4}")) return true; // date
        if (v.matches("[\\d,.]+")) return true; // number only
        if (v.length() <= 4 && v.matches("\\d+")) return true; // short number
        return false;
    }

    /** From FIELD_VALUES block (e.g. \u200BFIELD_2=Johnson\u200BFIELD_3=Sarah\u200B), return value for given item number or null. */
    private static String getFieldValueFromBlock(String rawText, int fieldNum) {
        return getFieldValueFromBlock(rawText, fieldNum, false);
    }

    /**
     * From FIELD_VALUES block, return a value for the given field number.
     * When skipDateLike is true, returns the first value that does not look like a date/number
     * (so when FIELD_1 appears as both "Johnson" and "01/01/1970", we pick "Johnson" for name).
     */
    private static String getFieldValueFromBlock(String rawText, int fieldNum, boolean skipDateLike) {
        if (rawText == null || rawText.isEmpty()) return null;
        int start = rawText.indexOf("FIELD_VALUES");
        if (start < 0) return null;
        String block = rawText.substring(start);
        String prefix = "\u200BFIELD_" + fieldNum + "=";
        int idx = 0;
        while (true) {
            idx = block.indexOf(prefix, idx);
            if (idx < 0) return null;
            int valueStart = idx + prefix.length();
            int valueEnd = block.indexOf("\u200B", valueStart);
            String value = valueEnd < 0 ? block.substring(valueStart).trim() : block.substring(valueStart, valueEnd).trim();
            if (!skipDateLike || !looksLikeDateOrNumber(value)) {
                return value;
            }
            idx = valueEnd < 0 ? block.length() : valueEnd + 1;
        }
    }

    /** Build "First [Middle] Last" from form items (first name, middle, last name). */
    private static String buildDisplayName(String first, String middle, String last) {
        if (first != null && !first.isBlank() && last != null && !last.isBlank()) {
            return first + (middle != null && !middle.isBlank() ? " " + middle : "") + " " + last;
        }
        if (first != null && !first.isBlank()) return first;
        if (last != null && !last.isBlank()) return last;
        return null;
    }

    /**
     * SAR fallback: parse subject name from FORM_VALUES using first two valid title-case words
     * (Form 109 order: Item 4 last, Item 5 first). Skips street suffixes and state abbreviations.
     */
    private static void parseSarSubjectNameFromFormValues(String rawText, Map<String, Object> formData) {
        if (rawText == null || formData == null) return;
        int formValues = rawText.indexOf("FORM_VALUES");
        if (formValues < 0) return;
        String nameSearchArea = rawText.substring(formValues);
        Pattern twoNames = Pattern.compile("\\b([A-Z][a-zA-Z]{1,24})\\s+([A-Z][a-zA-Z]{1,24})\\b");
        Matcher m = twoNames.matcher(nameSearchArea);
        while (m.find()) {
            String firstWord = m.group(1);
            String secondWord = m.group(2);
            if (isStreetSuffix(secondWord) || isStateAbbreviation(firstWord) || isStateAbbreviation(secondWord)) {
                continue;
            }
            if (isValidName(firstWord) && isValidName(secondWord) && !firstWord.equalsIgnoreCase(secondWord)) {
                String customerName = secondWord + " " + firstWord; // Form 109: firstWord=last, secondWord=first
                if (customerName.length() <= 128) {
                    formData.put("_parsedCustomerName", customerName);
                }
                return;
            }
        }
        // Strategy 6 style: first 2–3 valid words in FORM_VALUES
        Pattern word = Pattern.compile("\\b([A-Z][a-zA-Z]{1,24})\\b");
        Matcher wm = word.matcher(nameSearchArea);
        String firstCandidate = null;
        String secondCandidate = null;
        while (wm.find()) {
            String token = wm.group(1);
            if (!isValidName(token)) continue;
            if (firstCandidate == null) {
                firstCandidate = token;
            } else if (secondCandidate == null && !token.equalsIgnoreCase(firstCandidate)) {
                secondCandidate = token;
                break;
            }
        }
        if (firstCandidate != null && secondCandidate != null) {
            String customerName = secondCandidate + " " + firstCandidate;
            if (customerName.length() <= 128) {
                formData.put("_parsedCustomerName", customerName);
            }
        }
    }

    private CreateCtrRequestPayload parseForm104(String rawText, String sourceEntityId, Instant fallbackTime) {
        // Normalize Unicode apostrophes/quotes so "Individual's" matches (e.g. \u2019 -> ')
        if (rawText != null && !rawText.isEmpty()) {
            rawText = rawText.replace('\u2019', '\'').replace('\u2018', '\'');
        }
        Map<String, Object> ctrFormData = new HashMap<>();
        String rawForStorage = rawText == null ? "" : (rawText.length() <= MAX_RAW_TEXT_LENGTH ? rawText : rawText.substring(0, MAX_RAW_TEXT_LENGTH));
        rawForStorage = rawForStorage.replace("\u200BFORM_VALUES ", " "); // don't persist internal sentinel
        ctrFormData.put("_rawText", rawForStorage);

        String customerName = "(Extracted from PDF)";
        Instant transactionTime = fallbackTime;

        // Prefer FIELD_VALUES block (fixed template) for date, then fall back to heuristics
        String dateStr = getFieldValueFromBlock(rawText, 28);
        if (dateStr == null || dateStr.isBlank()) {
            dateStr = parseCtrTransactionDate(rawText);
        }
        if (dateStr != null) {
            try {
                String normalized = dateStr.replace('-', '/').trim();
                LocalDate d = LocalDate.parse(normalized, DateTimeFormatter.ofPattern("M/d/yyyy"));
                transactionTime = d.atStartOfDay(ZoneOffset.UTC).toInstant();
                ctrFormData.put("_parsedTransactionDate", normalized);
            } catch (DateTimeParseException e) {
                try {
                    LocalDate d = LocalDate.parse(dateStr.replace('-', '/').trim(), DateTimeFormatter.ofPattern("MM/dd/yy"));
                    transactionTime = d.atStartOfDay(ZoneOffset.UTC).toInstant();
                    ctrFormData.put("_parsedTransactionDate", dateStr.trim());
                } catch (DateTimeParseException e2) {
                    log.debug("Could not parse CTR transaction date: {}", dateStr);
                }
            }
        }

        // Form 104 Item 26 = Total cash IN, Item 27 = Total cash OUT. Prefer FIELD_VALUES then heuristics.
        BigDecimal cashIn = BigDecimal.ZERO;
        BigDecimal cashOut = BigDecimal.ZERO;
        String field26 = getFieldValueFromBlock(rawText, 26);
        String field27 = getFieldValueFromBlock(rawText, 27);
        if (field26 != null && !field26.isBlank()) {
            try {
                cashIn = new BigDecimal(field26.replace(",", ""));
            } catch (NumberFormatException ignored) { }
        }
        if (field27 != null && !field27.isBlank()) {
            try {
                cashOut = new BigDecimal(field27.replace(",", ""));
            } catch (NumberFormatException ignored) { }
        }
        Pattern cashInPattern = Pattern.compile(
            "(?:26\\s+)?(?:total\\s+cash\\s+in|total\\s+currency\\s+received|total\\s+amount\\s+in)\\s*\\$?\\s*([\\d,]+(?:\\.\\d{2})?)",
            Pattern.CASE_INSENSITIVE);
        Pattern cashOutPattern = Pattern.compile(
            "(?:27\\s+)?(?:total\\s+cash\\s+out|total\\s+currency\\s+paid|total\\s+amount\\s+out)\\s*\\$?\\s*([\\d,]+(?:\\.\\d{2})?)",
            Pattern.CASE_INSENSITIVE);
        Matcher inMatcher = cashInPattern.matcher(rawText);
        while (inMatcher.find()) {
            try {
                String amt = inMatcher.group(1);
                if (amt != null && !amt.isEmpty()) {
                    BigDecimal val = new BigDecimal(amt.replace(",", ""));
                    if (val.compareTo(cashIn) > 0) cashIn = val;
                }
            } catch (NumberFormatException ignored) { }
        }
        Matcher outMatcher = cashOutPattern.matcher(rawText);
        while (outMatcher.find()) {
            try {
                String amt = outMatcher.group(1);
                if (amt != null && !amt.isEmpty()) {
                    BigDecimal val = new BigDecimal(amt.replace(",", ""));
                    if (val.compareTo(cashOut) > 0) cashOut = val;
                }
            } catch (NumberFormatException ignored) { }
        }
        // Fallback: some forms use "26 Total amount" / "27 Total amount" without "in"/"out"
        if (cashIn.compareTo(BigDecimal.ZERO) <= 0 && cashOut.compareTo(BigDecimal.ZERO) <= 0) {
            Pattern item26Amount = Pattern.compile("\\b26\\s+(?:total\\s+)?(?:cash|amount|currency)\\s*\\$?\\s*([\\d,]+(?:\\.\\d{2})?)", Pattern.CASE_INSENSITIVE);
            Pattern item27Amount = Pattern.compile("\\b27\\s+(?:total\\s+)?(?:cash|amount|currency)\\s*\\$?\\s*([\\d,]+(?:\\.\\d{2})?)", Pattern.CASE_INSENSITIVE);
            Matcher m26 = item26Amount.matcher(rawText);
            if (m26.find()) {
                try {
                    cashIn = new BigDecimal(m26.group(1).replace(",", ""));
                } catch (NumberFormatException ignored) { }
            }
            Matcher m27 = item27Amount.matcher(rawText);
            if (m27.find()) {
                try {
                    cashOut = new BigDecimal(m27.group(1).replace(",", ""));
                } catch (NumberFormatException ignored) { }
            }
        }
        // Fallback: item number then value only (e.g. "26  10000" or "26 10000.00" — fillable PDF output)
        if (cashIn.compareTo(BigDecimal.ZERO) <= 0 && cashOut.compareTo(BigDecimal.ZERO) <= 0) {
            Pattern num26 = Pattern.compile("\\b26\\s+([\\d,]+(?:\\.\\d{2})?)\\b");
            Pattern num27 = Pattern.compile("\\b27\\s+([\\d,]+(?:\\.\\d{2})?)\\b");
            Matcher n26 = num26.matcher(rawText);
            if (n26.find()) {
                try {
                    cashIn = new BigDecimal(n26.group(1).replace(",", ""));
                } catch (NumberFormatException ignored) { }
            }
            Matcher n27 = num27.matcher(rawText);
            if (n27.find()) {
                try {
                    cashOut = new BigDecimal(n27.group(1).replace(",", ""));
                } catch (NumberFormatException ignored) { }
            }
        }
        // Fallback: 26 or 27 followed by amount within ~60 chars (layout with line breaks between label and value)
        if (cashIn.compareTo(BigDecimal.ZERO) <= 0 && cashOut.compareTo(BigDecimal.ZERO) <= 0) {
            for (String marker : new String[] { "26", "27" }) {
                int idx = rawText.indexOf(marker);
                if (idx < 0) continue;
                int end = Math.min(rawText.length(), idx + marker.length() + 60);
                String window = rawText.substring(idx, end);
                Pattern p = Pattern.compile("\\b([\\d]{1,3}(?:,[\\d]{3})*(?:\\.[0-9]{2})?)\\b|\\b([\\d]+(?:\\.[0-9]{2})?)\\b");
                Matcher m = p.matcher(window);
                if (m.find()) {
                    String g = m.group(1) != null ? m.group(1) : m.group(2);
                    if (g != null) {
                        try {
                            BigDecimal v = new BigDecimal(g.replace(",", ""));
                            if (v.compareTo(BigDecimal.valueOf(100)) >= 0 && v.compareTo(new BigDecimal("99999999")) <= 0 && !isLikelyYear(v)) {
                                if (cashIn.compareTo(v) < 0) cashIn = v;
                                if (cashOut.compareTo(v) < 0) cashOut = v;
                                break;
                            }
                        } catch (NumberFormatException ignored) { }
                    }
                }
            }
        }
        // Last resort: only in FORM_VALUES section (fillable field values), take max and exclude years.
        if (cashIn.compareTo(BigDecimal.ZERO) <= 0 && cashOut.compareTo(BigDecimal.ZERO) <= 0) {
            int formValues = rawText.indexOf("\u200BFORM_VALUES ");
            if (formValues >= 0 && formValues < rawText.length() - 20) {
                String searchArea = rawText.substring(formValues);
                Pattern anyAmount = Pattern.compile("\\b([\\d]{1,3}(?:,[\\d]{3})*(?:\\.[0-9]{2})?)\\b|\\b([\\d]+(?:\\.[0-9]{2})?)\\b");
                Matcher am = anyAmount.matcher(searchArea);
                BigDecimal maxAmount = BigDecimal.ZERO;
                while (am.find()) {
                    String g = am.group(1) != null ? am.group(1) : am.group(2);
                    if (g == null) continue;
                    try {
                        BigDecimal v = new BigDecimal(g.replace(",", ""));
                        if (v.compareTo(BigDecimal.ZERO) > 0 && v.compareTo(new BigDecimal("99999999")) <= 0
                                && (v.scale() >= 2 || v.compareTo(BigDecimal.valueOf(100)) >= 0)
                                && !isLikelyYear(v)
                                && v.compareTo(maxAmount) > 0) {
                            maxAmount = v;
                        }
                    } catch (NumberFormatException ignored) { }
                }
                if (maxAmount.compareTo(BigDecimal.ZERO) > 0) {
                    cashIn = maxAmount;
                }
            }
        }
        // Final fallback: in lines that mention 26, 27, total, or cash, take the max amount (exclude years).
        if (cashIn.compareTo(BigDecimal.ZERO) <= 0 && cashOut.compareTo(BigDecimal.ZERO) <= 0) {
            Pattern lineAmount = Pattern.compile("\\b([\\d]{1,3}(?:,[\\d]{3})*(?:\\.[0-9]{2})?)\\b|\\b([\\d]+(?:\\.[0-9]{2})?)\\b");
            Pattern context = Pattern.compile("\\b(26|27|total|cash)\\b", Pattern.CASE_INSENSITIVE);
            BigDecimal maxInContext = BigDecimal.ZERO;
            for (String line : rawText.split("[\\r\\n]+")) {
                if (!context.matcher(line).find()) continue;
                Matcher lm = lineAmount.matcher(line);
                while (lm.find()) {
                    String g = lm.group(1) != null ? lm.group(1) : lm.group(2);
                    if (g == null) continue;
                    try {
                        BigDecimal v = new BigDecimal(g.replace(",", ""));
                        if (v.compareTo(BigDecimal.valueOf(100)) >= 0 && v.compareTo(new BigDecimal("99999999")) <= 0 && !isLikelyYear(v)
                                && v.compareTo(maxInContext) > 0) {
                            maxInContext = v;
                        }
                    } catch (NumberFormatException ignored) { }
                }
            }
            if (maxInContext.compareTo(BigDecimal.ZERO) > 0) {
                cashIn = maxInContext;
            }
        }
        BigDecimal totalAmount = cashIn.max(cashOut);
        if (totalAmount.compareTo(BigDecimal.ZERO) > 0) {
            ctrFormData.put("_parsedTotalAmount", totalAmount.toPlainString());
            if (cashIn.compareTo(BigDecimal.ZERO) > 0) ctrFormData.put("_parsedCashIn", cashIn.toPlainString());
            if (cashOut.compareTo(BigDecimal.ZERO) > 0) ctrFormData.put("_parsedCashOut", cashOut.toPlainString());
            String direction = (cashIn.compareTo(BigDecimal.ZERO) > 0 && cashOut.compareTo(BigDecimal.ZERO) > 0) ? "BOTH"
                : (cashIn.compareTo(BigDecimal.ZERO) > 0 ? "IN" : "OUT");
            ctrFormData.put("_parsedDirection", direction);
        }

        // Form 104 Items 2–4: Item 2 = last name (or organization), Item 3 = first name, Item 4 = middle initial/name.
        // Prefer FIELD_VALUES block (fixed template). Some PDFs use 1-based field numbers (1=last, 2=first, 3=date).
        String lastOrEntity = getFieldValueFromBlock(rawText, 2);
        String first = getFieldValueFromBlock(rawText, 3);
        String middle = getFieldValueFromBlock(rawText, 4);
        if (middle != null && middle.isEmpty()) middle = null;
        // If we got a date as "first" or "last", or we have only one name part, the PDF may use 1-based fields (1=last, 2=first).
        // FIELD_1 can appear twice (e.g. Johnson and 01/01/1970); prefer the value that looks like a name.
        boolean tryAlt = looksLikeDateOrNumber(first) || looksLikeDateOrNumber(lastOrEntity)
                || (first == null && lastOrEntity != null) || (first != null && lastOrEntity == null);
        if (tryAlt) {
            String altLast = getFieldValueFromBlock(rawText, 1, true);  // skip date-like so we get "Johnson" not "01/01/1970"
            String altFirst = getFieldValueFromBlock(rawText, 2);
            if (!looksLikeDateOrNumber(altFirst) && !looksLikeDateOrNumber(altLast)
                    && (altLast != null && !altLast.isBlank() || altFirst != null && !altFirst.isBlank())) {
                if (altLast != null && !altLast.isBlank()) lastOrEntity = altLast;
                if (altFirst != null && !altFirst.isBlank()) first = altFirst;
            } else if (looksLikeDateOrNumber(first) || looksLikeDateOrNumber(lastOrEntity)) {
                // Discard date so heuristics can fill from FORM_VALUES
                if (looksLikeDateOrNumber(first)) first = null;
                if (looksLikeDateOrNumber(lastOrEntity)) lastOrEntity = null;
            }
        }

        // Strategy 1: Value on same line after "2 ... name", "3 First name", or "4 Middle initial" (capture must not be boilerplate)
        Pattern nameAfterLabel = Pattern.compile(
            "(?:2\\s+)?(?:individual'?s?\\s+last\\s+name\\s+or\\s+(?:entity'?s?|organization)\\s+name|individual'?s?\\s+last\\s+name|entity'?s?\\s+name)" +
            "\\s*:?\\s*([A-Za-z][A-Za-z\\s.,'-]{2,100}?)(?=\\s*[\\r\\n]|\\s*$|\\s*\\d)" +
            "|(?:3\\s+)?first\\s+name\\s*:?\\s*([A-Za-z][A-Za-z\\s.,'-]{2,100}?)(?=\\s*[\\r\\n]|\\s*$|\\s*\\d)" +
            "|(?:4\\s+)?middle\\s+initial\\s*:?\\s*([A-Za-z][A-Za-z\\s.,'-]{0,100}?)(?=\\s*[\\r\\n]|\\s*$|\\s*\\d)",
            Pattern.CASE_INSENSITIVE);
        Matcher m1 = nameAfterLabel.matcher(rawText);
        while (m1.find()) {
            String last = m1.group(1);
            String firstCap = m1.group(2);
            String mid = m1.group(3);
            if (lastOrEntity == null && isValidName(last)) lastOrEntity = last.trim();
            if (first == null && isValidName(firstCap)) first = firstCap != null ? firstCap.trim() : null;
            if (middle == null && mid != null && !mid.trim().isEmpty() && isValidName(mid.trim())) middle = mid.trim();
        }

        // Strategy 2: Value on next line after label (common in PDFs)
        Pattern nameOnNextLine = Pattern.compile(
            "(?:2\\s+)?(?:individual'?s?\\s+last\\s+name\\s+or\\s+(?:entity'?s?|organization)\\s+name|individual'?s?\\s+last\\s+name)[\\s\\r\\n]*([A-Za-z][A-Za-z\\s.,'-]{2,50}?)(?=\\s*[\\r\\n]|\\s{2,}|\\s*3\\s)" +
            "|(?:3\\s+)?first\\s+name[\\s\\r\\n]*([A-Za-z][A-Za-z\\s.,'-]{2,50}?)(?=\\s*[\\r\\n]|\\s{2,}|\\s*4\\s)" +
            "|(?:4\\s+)?middle\\s+initial[\\s\\r\\n]*([A-Za-z][A-Za-z\\s.,'-]{0,50}?)(?=\\s*[\\r\\n]|\\s{2,}|\\s*5\\s)",
            Pattern.CASE_INSENSITIVE);
        Matcher m2 = nameOnNextLine.matcher(rawText);
        while (m2.find()) {
            String g1 = m2.group(1);
            String g2 = m2.group(2);
            String g3 = m2.group(3);
            if (lastOrEntity == null && isValidName(g1)) lastOrEntity = g1.trim();
            if (first == null && isValidName(g2)) first = g2 != null ? g2.trim() : null;
            if (middle == null && g3 != null && !g3.trim().isEmpty() && isValidName(g3.trim())) middle = g3.trim();
        }

        // Strategy 3: Item number then value then label (e.g. "2  Johnson  Individual's last name" or "3  Sarah  First name")
        Pattern valueBeforeLabel = Pattern.compile(
            "\\b2\\s+([A-Za-z][A-Za-z\\s.,'-]{2,50}?)\\s+(?:individual'?s?|organization|entity'?s?)" +
            "|\\b3\\s+([A-Za-z][A-Za-z\\s.,'-]{2,50}?)\\s+first",
            Pattern.CASE_INSENSITIVE);
        Matcher m3 = valueBeforeLabel.matcher(rawText);
        while (m3.find()) {
            String g1 = m3.group(1);
            String g2 = m3.group(2);
            if (lastOrEntity == null && isValidName(g1)) lastOrEntity = g1.trim();
            if (first == null && isValidName(g2)) first = g2 != null ? g2.trim() : null;
        }

        // Strategy 4: Line starting with "2 " or "3 " followed only by a name (no label on same line)
        Pattern lineWithJustNumberAndName = Pattern.compile(
            "(?:^|[\\r\\n])\\s*2\\s+([A-Za-z][A-Za-z\\s.,'-]{2,50}?)\\s*[\\r\\n]" +
            "|(?:^|[\\r\\n])\\s*3\\s+([A-Za-z][A-Za-z\\s.,'-]{2,50}?)\\s*[\\r\\n]",
            Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);
        Matcher m4 = lineWithJustNumberAndName.matcher(rawText);
        while (m4.find()) {
            String g1 = m4.group(1);
            String g2 = m4.group(2);
            if (lastOrEntity == null && isValidName(g1)) lastOrEntity = g1.trim();
            if (first == null && isValidName(g2)) first = g2 != null ? g2.trim() : null;
        }

        // When we have only first or only last (e.g. FIELD_1 had no name value), fill the missing part from FORM_VALUES "Last First" text.
        if (rawText != null && (lastOrEntity == null || first == null)) {
            int fv = rawText.indexOf("FORM_VALUES");
            if (fv >= 0) {
                String formSection = rawText.substring(fv);
                Pattern twoNames = Pattern.compile("\\b([A-Z][a-zA-Z]{1,24})\\s+([A-Z][a-zA-Z]{1,24})\\b");
                Matcher m = twoNames.matcher(formSection);
                while (m.find()) {
                    String w1 = m.group(1);
                    String w2 = m.group(2);
                    if (isStreetSuffix(w2) || isStateAbbreviation(w1) || isStateAbbreviation(w2) || !isValidName(w1) || !isValidName(w2) || w1.equalsIgnoreCase(w2)) {
                        continue;
                    }
                    // Form order in values is typically last then first: "Johnson Sarah"
                    if (lastOrEntity == null && first != null && first.equalsIgnoreCase(w2)) {
                        lastOrEntity = w1;
                        break;
                    }
                    if (first == null && lastOrEntity != null && lastOrEntity.equalsIgnoreCase(w1)) {
                        first = w2;
                        break;
                    }
                }
            }
        }

        // Strategy 5: Two consecutive title-case words (e.g. "Sarah Johnson") — prefer form field values to avoid template text
        if (lastOrEntity == null && first == null) {
            String nameSearchArea = rawText;
            int formValues = rawText.indexOf("FORM_VALUES");
            if (formValues >= 0 && formValues < rawText.length() - 15) {
                nameSearchArea = rawText.substring(formValues);
            } else {
                int sectionA = Math.max(rawText.indexOf("Section A"), Math.max(rawText.indexOf("Person(s) on Whose"), rawText.indexOf("Individual")));
                if (sectionA >= 0 && sectionA < rawText.length() - 20) {
                    nameSearchArea = rawText.substring(sectionA);
                }
            }
            Pattern twoNames = Pattern.compile("\\b([A-Z][a-zA-Z]{1,24})\\s+([A-Z][a-zA-Z]{1,24})\\b");
            Matcher m5 = twoNames.matcher(nameSearchArea);
            while (m5.find()) {
                String firstWord = m5.group(1);
                String secondWord = m5.group(2);
                // Skip address-like pairs: second word is street suffix (e.g. "Main St") or either is state abbrev (e.g. "Vernon CT")
                if (isStreetSuffix(secondWord) || isStateAbbreviation(firstWord) || isStateAbbreviation(secondWord)) {
                    continue;
                }
                if (isValidName(firstWord) && isValidName(secondWord) && !firstWord.equalsIgnoreCase(secondWord)) {
                    // Form order is typically Item 2 (last) then Item 3 (first), so firstWord=last, secondWord=first
                    first = secondWord;
                    lastOrEntity = firstWord;
                    break;
                }
            }
        }

        // Strategy 6: In FORM_VALUES section, take first 2–3 valid name-like words. Form order: Item 2 (last), Item 3 (first), Item 4 (middle).
        if (lastOrEntity == null && first == null) {
            int formValues = rawText.indexOf("FORM_VALUES");
            if (formValues >= 0) {
                String formSection = rawText.substring(formValues);
                Pattern word = Pattern.compile("\\b([A-Z][a-zA-Z]{1,24})\\b");
                Matcher wm = word.matcher(formSection);
                String firstCandidate = null;  // last name (Item 2)
                String secondCandidate = null; // first name (Item 3)
                String thirdCandidate = null;  // middle (Item 4), optional
                while (wm.find()) {
                    String token = wm.group(1);
                    if (!isValidName(token)) continue;
                    if (firstCandidate == null) {
                        firstCandidate = token;
                    } else if (secondCandidate == null && !token.equalsIgnoreCase(firstCandidate)) {
                        secondCandidate = token;
                    } else if (thirdCandidate == null && !token.equalsIgnoreCase(firstCandidate) && !token.equalsIgnoreCase(secondCandidate)) {
                        thirdCandidate = token;
                        break;
                    }
                }
                if (firstCandidate != null && secondCandidate != null) {
                    first = secondCandidate;   // first name
                    lastOrEntity = firstCandidate; // last name
                    if (thirdCandidate != null) {
                        middle = thirdCandidate;  // middle name/initial
                    }
                }
            }
        }

        if (lastOrEntity != null || first != null) {
            // Display: First Name [Middle Name] Last Name
            if (first != null && lastOrEntity != null) {
                customerName = first + (middle != null && !middle.isEmpty() ? " " + middle : "") + " " + lastOrEntity;
            } else {
                customerName = first != null ? first : lastOrEntity;
                if (middle != null && !middle.isEmpty()) customerName = customerName + " " + middle;
            }
            if (customerName != null && customerName.length() <= 128) {
                ctrFormData.put("_parsedCustomerName", customerName);
            }
        }

        // Form 104 Item 5: Social security number (for suspect matching)
        String parsedSsn = parseSsnFromText(rawText);
        String externalSubjectKey = null;
        if (parsedSsn != null) {
            ctrFormData.put("_parsedSsn", parsedSsn);
            externalSubjectKey = SSN_SUBJECT_PREFIX + parsedSsn;
        }

        // Form 104 Items 7–11: Permanent address. Prefer FIELD_VALUES then parseCtrAddress heuristics.
        String field7 = getFieldValueFromBlock(rawText, 7);
        String field9 = getFieldValueFromBlock(rawText, 9);
        String field10 = getFieldValueFromBlock(rawText, 10);
        String field11 = getFieldValueFromBlock(rawText, 11);
        if (field7 != null && !field7.isBlank()) ctrFormData.put("_parsedAddressLine1", field7.trim());
        if (field9 != null && !field9.isBlank()) ctrFormData.put("_parsedAddressCity", field9.trim());
        if (field10 != null && !field10.isBlank()) ctrFormData.put("_parsedAddressState", field10.trim().toUpperCase());
        if (field11 != null && !field11.isBlank()) ctrFormData.put("_parsedAddressPostalCode", field11.trim());
        parseCtrAddress(rawText, ctrFormData);

        return new CreateCtrRequestPayload(
                SOURCE_SYSTEM,
                sourceEntityId,
                externalSubjectKey,
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
        if (rawText != null && !rawText.isEmpty()) {
            rawText = rawText.replace('\u2019', '\'').replace('\u2018', '\'');
        }
        Map<String, Object> formData = new HashMap<>();
        String rawForStorage = rawText == null ? "" : (rawText.length() <= MAX_RAW_TEXT_LENGTH ? rawText : rawText.substring(0, MAX_RAW_TEXT_LENGTH));
        rawForStorage = rawForStorage.replace("\u200BFORM_VALUES ", " ");
        formData.put("_rawText", rawForStorage);

        // Form 109 Items 4, 5, 6: Subject name (last, first, middle). Prefer FIELD_VALUES block, then fallback heuristics.
        String sarLast = getFieldValueFromBlock(rawText, 4);
        String sarFirst = getFieldValueFromBlock(rawText, 5);
        String sarMiddle = getFieldValueFromBlock(rawText, 6);
        if ((sarLast != null && !sarLast.isBlank()) || (sarFirst != null && !sarFirst.isBlank())) {
            String customerName = buildDisplayName(sarFirst, sarMiddle, sarLast);
            if (customerName != null && customerName.length() <= 128) {
                formData.put("_parsedCustomerName", customerName);
            }
        }
        if (!formData.containsKey("_parsedCustomerName")) {
            parseSarSubjectNameFromFormValues(rawText != null ? rawText : "", formData);
        }

        String narrative = null;
        if (rawText != null && !rawText.isBlank()) {
            // Prefer narrative from fillable form fields (FORM_VALUES): narrative field is often the only prose among "Off" checkboxes
            int formValues = rawText.indexOf("FORM_VALUES");
            if (formValues >= 0 && formValues < rawText.length() - 15) {
                String fromForm = rawText.substring(formValues + "FORM_VALUES".length()).trim();
                narrative = extractNarrativeFromFormValues(fromForm);
                if (narrative != null && !narrative.isBlank()) {
                    formData.put("_narrativeSection", "Form fields");
                }
            }
            // Fall back to Part VI page text if we didn't get narrative from form fields
            if (narrative == null || narrative.isBlank()) {
                narrative = extractSarNarrative(rawText);
                if (narrative != null && !narrative.isBlank()) {
                    if (!formData.containsKey("_narrativeSection")) formData.put("_narrativeSection", "Part VI");
                }
            }
            if (narrative == null || narrative.isBlank()) {
                narrative = "(No text extracted from PDF)";
            }
        } else {
            narrative = "(No text extracted from PDF)";
        }

        Instant activityStart = null;
        Instant activityEnd = null;
        String raw = rawText != null ? rawText : "";
        // Form 109 *16: "Date or date range of suspicious activity" — "From ___/___/___" and "To ___/___/___"
        Pattern fromToPattern = Pattern.compile(
            "from\\s+(\\d{1,2}[-/]\\d{1,2}[-/]\\d{2,4})\\s*(?:\\s+to\\s+(\\d{1,2}[-/]\\d{1,2}[-/]\\d{2,4}))?",
            Pattern.CASE_INSENSITIVE);
        Matcher fromToMatcher = fromToPattern.matcher(raw);
        if (fromToMatcher.find()) {
            try {
                String fromStr = fromToMatcher.group(1).replace('-', '/').trim();
                LocalDate fromDate = LocalDate.parse(fromStr, DateTimeFormatter.ofPattern("M/d/yyyy"));
                activityStart = fromDate.atStartOfDay(ZoneOffset.UTC).toInstant();
                formData.put("_parsedActivityStart", fromStr);
                String toStr = fromToMatcher.group(2);
                if (toStr != null && !toStr.isEmpty()) {
                    toStr = toStr.replace('-', '/').trim();
                    LocalDate toDate = LocalDate.parse(toStr, DateTimeFormatter.ofPattern("M/d/yyyy"));
                    activityEnd = toDate.atStartOfDay(ZoneOffset.UTC).toInstant();
                    formData.put("_parsedActivityEnd", toStr);
                }
            } catch (DateTimeParseException e) {
                try {
                    String fromStr = fromToMatcher.group(1).replace('-', '/').trim();
                    LocalDate fromDate = LocalDate.parse(fromStr, DateTimeFormatter.ofPattern("MM/dd/yy"));
                    activityStart = fromDate.atStartOfDay(ZoneOffset.UTC).toInstant();
                    formData.put("_parsedActivityStart", fromStr);
                } catch (DateTimeParseException ignored) { }
            }
        }
        // Alternative: "Date of suspicious activity" or "*16" with date(s) on same or next line
        if (activityStart == null) {
            Pattern dateOfActivity = Pattern.compile(
                "(?:\\*?16\\s+)?(?:date\\s+(?:or\\s+date\\s+range\\s+)?of\\s+suspicious\\s+activity|suspicious\\s+activity\\s+date)[\\s\\r\\n]*([\\d]{1,2}[-/][\\d]{1,2}[-/][\\d]{2,4})",
                Pattern.CASE_INSENSITIVE);
            Matcher dm = dateOfActivity.matcher(raw);
            if (dm.find()) {
                try {
                    String dateStr = dm.group(1).replace('-', '/').trim();
                    LocalDate d = LocalDate.parse(dateStr, DateTimeFormatter.ofPattern("M/d/yyyy"));
                    activityStart = d.atStartOfDay(ZoneOffset.UTC).toInstant();
                    formData.put("_parsedActivityStart", dateStr);
                } catch (DateTimeParseException e) {
                    try {
                        LocalDate d = LocalDate.parse(dm.group(1).replace('-', '/').trim(), DateTimeFormatter.ofPattern("MM/dd/yy"));
                        activityStart = d.atStartOfDay(ZoneOffset.UTC).toInstant();
                        formData.put("_parsedActivityStart", dm.group(1).trim());
                    } catch (DateTimeParseException ignored) { }
                }
            }
        }
        if (activityStart == null) {
            Pattern anyDate = Pattern.compile("(\\d{1,2}[-/]\\d{1,2}[-/]\\d{2,4})");
            Matcher m = anyDate.matcher(raw);
            if (m.find()) {
                try {
                    String dateStr = m.group(1).replace('-', '/').trim();
                    LocalDate d = LocalDate.parse(dateStr, DateTimeFormatter.ofPattern("M/d/yyyy"));
                    activityStart = d.atStartOfDay(ZoneOffset.UTC).toInstant();
                    formData.put("_parsedActivityStart", dateStr);
                } catch (DateTimeParseException e) {
                    try {
                        LocalDate d = LocalDate.parse(m.group(1).replace('-', '/').trim(), DateTimeFormatter.ofPattern("MM/dd/yy"));
                        activityStart = d.atStartOfDay(ZoneOffset.UTC).toInstant();
                        formData.put("_parsedActivityStart", m.group(1).trim());
                    } catch (DateTimeParseException ignored) { }
                }
            }
        }
        // Fallback: valid date in FORM_VALUES (fillable fields) so user-entered date is used
        if (activityStart == null && raw != null) {
            int formValues = raw.indexOf("FORM_VALUES");
            if (formValues >= 0) {
                String formSection = raw.substring(formValues);
                Pattern p = Pattern.compile("\\b(\\d{1,2})/(\\d{1,2})/(\\d{4})\\b");
                Matcher pm = p.matcher(formSection);
                while (pm.find()) {
                    try {
                        int month = Integer.parseInt(pm.group(1));
                        int day = Integer.parseInt(pm.group(2));
                        int year = Integer.parseInt(pm.group(3));
                        if (year >= 1990 && year <= 2030 && month >= 1 && month <= 12 && day >= 1 && day <= 31) {
                            String dateStr = pm.group(0);
                            LocalDate d = LocalDate.parse(dateStr, DateTimeFormatter.ofPattern("M/d/yyyy"));
                            activityStart = d.atStartOfDay(ZoneOffset.UTC).toInstant();
                            formData.put("_parsedActivityStart", dateStr);
                            break;
                        }
                    } catch (DateTimeParseException | NumberFormatException ignored) { }
                }
            }
        }

        // Form 109 *17: "Total amount involved" — multiple patterns to match varied text
        BigDecimal totalAmount = null;
        Pattern amountPattern = Pattern.compile(
            "(?:\\*?17\\s+)?(?:total\\s+amount\\s+involved|total\\s+amount)\\s*\\$?\\s*([\\d,]+(?:\\.\\d{2})?)",
            Pattern.CASE_INSENSITIVE);
        Matcher amountMatcher = amountPattern.matcher(raw);
        if (amountMatcher.find()) {
            try {
                String amtStr = amountMatcher.group(1);
                if (amtStr != null && !amtStr.isEmpty()) {
                    totalAmount = new BigDecimal(amtStr.replace(",", ""));
                    formData.put("_parsedTotalAmount", totalAmount.toPlainString());
                }
            } catch (NumberFormatException ignored) { }
        }
        if ((totalAmount == null || totalAmount.compareTo(BigDecimal.ZERO) <= 0) && !formData.containsKey("_parsedTotalAmount")) {
            Pattern item17Amount = Pattern.compile("\\b17\\s+(?:total\\s+)?(?:amount|dollar)[\\s\\$]*([\\d,]+(?:\\.\\d{2})?)", Pattern.CASE_INSENSITIVE);
            Matcher m17 = item17Amount.matcher(raw);
            if (m17.find()) {
                try {
                    BigDecimal amt = new BigDecimal(m17.group(1).replace(",", ""));
                    totalAmount = amt;
                    formData.put("_parsedTotalAmount", amt.toPlainString());
                } catch (NumberFormatException ignored) { }
            }
        }
        // Fallback: *17 or 17 followed by number only (fillable form output)
        if ((totalAmount == null || totalAmount.compareTo(BigDecimal.ZERO) <= 0) && raw != null) {
            Pattern num17 = Pattern.compile("\\b17\\s+([\\d,]+(?:\\.\\d{2})?)\\b");
            Matcher n17 = num17.matcher(raw);
            if (n17.find()) {
                try {
                    totalAmount = new BigDecimal(n17.group(1).replace(",", ""));
                    formData.put("_parsedTotalAmount", totalAmount.toPlainString());
                } catch (NumberFormatException ignored) { }
            }
        }
        // Last resort: dollar-like amount — prefer FORM_VALUES section and take the *maximum* valid amount so we don't
        // pick form number 109 or other small numbers instead of the real total amount (e.g. 10000).
        if ((totalAmount == null || totalAmount.compareTo(BigDecimal.ZERO) <= 0) && raw != null) {
            Pattern anyAmount = Pattern.compile("\\b([\\d]{1,3}(?:,[\\d]{3})*(?:\\.[0-9]{2})?)\\b|\\b([\\d]+(?:\\.[0-9]{2})?)\\b");
            BigDecimal maxAmount = null;
            String searchArea = raw;
            int formValues = raw.indexOf("\u200BFORM_VALUES ");
            if (formValues >= 0 && formValues < raw.length() - 20) {
                searchArea = raw.substring(formValues);
            }
            Matcher am = anyAmount.matcher(searchArea);
            while (am.find()) {
                String g = am.group(1) != null ? am.group(1) : am.group(2);
                if (g == null) continue;
                try {
                    BigDecimal v = new BigDecimal(g.replace(",", ""));
                    if (v.compareTo(BigDecimal.ZERO) > 0 && v.compareTo(new BigDecimal("99999999")) <= 0
                            && (v.scale() >= 2 || v.compareTo(BigDecimal.valueOf(100)) >= 0)
                            && !isLikelyYear(v)) {
                        if (maxAmount == null || v.compareTo(maxAmount) > 0) maxAmount = v;
                    }
                } catch (NumberFormatException ignored) { }
            }
            if (maxAmount != null) {
                totalAmount = maxAmount;
                formData.put("_parsedTotalAmount", maxAmount.toPlainString());
            }
        }

        Integer severityScore = 50; // default when not derivable

        // Form 109: Subject/individual address (for suspect enrichment — same keys as CTR so compliance service can push to suspect)
        parseSarAddress(raw, formData);

        // SSN if present (for suspect matching) — Form 109 may have SSN in subject/individual section
        String parsedSsn = parseSsnFromText(raw);
        String externalSubjectKey = null;
        if (parsedSsn != null) {
            formData.put("_parsedSsn", parsedSsn);
            externalSubjectKey = SSN_SUBJECT_PREFIX + parsedSsn;
        }

        return new CreateSarRequestPayload(
                SOURCE_SYSTEM,
                sourceEntityId,
                externalSubjectKey,
                fallbackTime,
                totalAmount,
                narrative,
                activityStart,
                activityEnd,
                formData,
                severityScore);
    }

    /**
     * Validates CTR payload after extraction. Errors are based only on what was actually parsed
     * from the PDF text; messages tell the user which form items to check.
     */
    public void validateCtrPayload(CreateCtrRequestPayload payload) {
        List<String> errors = new ArrayList<>();
        boolean hasName = payload.ctrFormData() != null && payload.ctrFormData().containsKey("_parsedCustomerName");
        if (!hasName) {
            errors.add("Based on the extracted text, customer name was not found. Please ensure the CTR form has Items 2 and 3 (Individual's last name or Organization name, First name) clearly filled.");
        }
        boolean hasAmount = payload.ctrFormData() != null && payload.ctrFormData().containsKey("_parsedTotalAmount")
                && payload.totalAmount() != null && payload.totalAmount().compareTo(BigDecimal.ZERO) > 0;
        if (!hasAmount) {
            errors.add("Based on the extracted text, transaction amount was not found. Please ensure the CTR form has Item 26 (Total cash in) and/or Item 27 (Total cash out) with a dollar amount.");
        }
        boolean hasDate = payload.ctrFormData() != null && payload.ctrFormData().containsKey("_parsedTransactionDate");
        if (!hasDate) {
            errors.add("Based on the extracted text, transaction date was not found. Please ensure the CTR form has Item 28 (Date of transaction) in a format like MM/DD/YYYY.");
        }
        if (!errors.isEmpty()) {
            throw new DocumentValidationException(errors);
        }
    }

    /**
     * Validates SAR payload after extraction. Errors are based only on what was actually parsed
     * from the PDF text; messages tell the user which form parts to check.
     */
    public void validateSarPayload(CreateSarRequestPayload payload) {
        List<String> errors = new ArrayList<>();
        String narrative = payload.narrative();
        if (narrative == null || narrative.isBlank() || "(No text extracted from PDF)".equals(narrative)) {
            errors.add("Based on the extracted text, the suspicious activity narrative was not found. Please ensure the SAR form has Part VI (Suspicious Activity Information - Narrative) filled.");
        }
        boolean hasDate = payload.formData() != null
                && (payload.formData().containsKey("_parsedActivityStart") || payload.formData().containsKey("_parsedActivityEnd"));
        if (!hasDate) {
            errors.add("Based on the extracted text, activity date or date range was not found. Please ensure the SAR form has the date range of suspicious activity (From/To) in a format like MM/DD/YYYY.");
        }
        boolean hasAmount = payload.formData() != null && payload.formData().containsKey("_parsedTotalAmount")
                && payload.totalAmount() != null && payload.totalAmount().compareTo(BigDecimal.ZERO) > 0;
        if (!hasAmount) {
            errors.add("Based on the extracted text, total amount was not found. Please ensure the SAR form has *17 (Total amount involved) with a dollar amount.");
        }
        if (!errors.isEmpty()) {
            throw new DocumentValidationException(errors);
        }
    }
}
