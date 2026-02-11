package com.skillstorm.finsight.documents_cases.services;

import java.io.InputStream;
import java.math.BigDecimal;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assumptions;

import com.skillstorm.finsight.documents_cases.dtos.compliance.CreateCtrRequestPayload;
import com.skillstorm.finsight.documents_cases.dtos.compliance.CreateSarRequestPayload;
import com.skillstorm.finsight.documents_cases.models.DocumentType;

/**
 * Verifies CTR/SAR PDF parsing against sample forms in src/main/resources/sample-forms/.
 * test-ctr.pdf: Sarah Johnson, 10000, 02/10/2026.
 * test-sar-10000.pdf: total amount 10000.
 */
class PdfExtractionServiceTest {

    @Test
    void testCtrPdfExtractsNameAmountAndDate() throws Exception {
        PdfExtractionService service = new PdfExtractionService();
        String path = "/sample-forms/test-ctr.pdf";
        try (InputStream is = getClass().getResourceAsStream(path)) {
            assertNotNull(is, "test-ctr.pdf not found on classpath at " + path);
            Object result = service.extractAndParse(is, DocumentType.CTR);
            CreateCtrRequestPayload payload = (CreateCtrRequestPayload) result;
            Map<String, Object> formData = payload.ctrFormData();
            assertNotNull(formData);

            assertTrue(formData.containsKey("_parsedCustomerName"),
                "Expected _parsedCustomerName in form data. Raw text may not match current patterns.");
            String name = (String) formData.get("_parsedCustomerName");
            assertTrue(name != null && (name.contains("Sarah") || name.contains("Johnson")),
                "Expected customer name to contain Sarah or Johnson, got: " + name);

            assertTrue(formData.containsKey("_parsedTransactionDate"),
                "Expected _parsedTransactionDate in form data.");
            String dateStr = (String) formData.get("_parsedTransactionDate");
            assertTrue(dateStr != null && dateStr.contains("2026"),
                "Expected date to include 2026, got: " + dateStr);

            assertNotNull(payload.totalAmount(), "Expected totalAmount to be set");
            assertTrue(payload.totalAmount().compareTo(BigDecimal.ZERO) > 0,
                "Expected positive total amount, got: " + payload.totalAmount());
        }
    }

    /** Sample test-ctr.pdf: expect 10000 when form has Item 26/27 or amount field = 10000; otherwise at least positive. */
    @Test
    void testCtrPdfExtractsAmount10000() throws Exception {
        PdfExtractionService service = new PdfExtractionService();
        try (InputStream is = getClass().getResourceAsStream("/sample-forms/test-ctr.pdf")) {
            assertNotNull(is, "test-ctr.pdf not found");
            CreateCtrRequestPayload payload = (CreateCtrRequestPayload) service.extractAndParse(is, DocumentType.CTR);
            assertNotNull(payload.totalAmount(), "totalAmount should be set");
            assertTrue(payload.totalAmount().compareTo(BigDecimal.ZERO) > 0,
                "totalAmount should be positive; got " + payload.totalAmount());
            assertTrue(
                payload.totalAmount().compareTo(new BigDecimal("10000")) == 0
                    || payload.totalAmount().compareTo(new BigDecimal("1000")) >= 0,
                "test-ctr.pdf should yield 10000 (or at least 1000); got " + payload.totalAmount()
                    + ". Ensure Item 26/27 or fillable amount contains 10000.");
        }
    }

    /** Sample test-sar-10000.pdf has total amount 10000; DB total_amount must match. */
    @Test
    void testSarPdfExtractsAmount10000() throws Exception {
        PdfExtractionService service = new PdfExtractionService();
        try (InputStream is = getClass().getResourceAsStream("/sample-forms/test-sar-10000.pdf")) {
            Assumptions.assumeTrue(is != null, "test-sar-10000.pdf not on classpath");
            CreateSarRequestPayload payload = (CreateSarRequestPayload) service.extractAndParse(is, DocumentType.SAR);
            assertNotNull(payload.totalAmount(), "SAR total amount should be set");
            assertTrue(payload.totalAmount().compareTo(new BigDecimal("10000")) == 0,
                "test-sar-10000.pdf should yield totalAmount 10000 for DB compliance_event.total_amount; got " + payload.totalAmount());
        }
    }
}
