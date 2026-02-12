# MVP: What You Need to Do (Suspect Registry + Documents-Cases Only)

Scope: **suspect_registry_service** and **documents_cases_service** only. Other MVP items (CTR auto-generation, rule-based SAR detection) live in other microservices and are out of scope here.

---

## 1. CTRs auto-generated from cash transactions

**Not your scope.** This is owned by the compliance/transactions service (e.g. `CtrGenerationService`, scheduled jobs).

**Your action:** None.

---

## 2. Rule-based detection generates SARs

**Not your scope.** Detection rules and automatic SAR creation live in another service.

**Your action:** None.

---

## 3. SARs create cases

**Your scope: documents_cases_service.**

**Already in place:**

- When a **SAR document** is uploaded (without `caseId`), [DocumentService](backend/services/documents_cases_service/src/main/java/com/skillstorm/finsight/documents_cases/services/DocumentService.java) auto-creates a case for that SAR and links the document to it.
- [CaseFileController](backend/services/documents_cases_service/src/main/java/com/skillstorm/finsight/documents_cases/controllers/CaseFileController.java) exposes `POST /api/cases` with body `{ "sarId", "status", "referredToAgency" }` so a case can be created when a SAR exists elsewhere.

**What you may need to do:**

- If **another service** creates SARs (e.g. via API) and expects a case to exist:
  - **Option A:** That service calls your `POST /api/cases` with the new `sarId` after creating the SAR. Document this contract (request body, idempotency if any).
  - **Option B:** You consume a “SAR created” event (e.g. from RabbitMQ), then create the case in documents-cases. This requires adding a listener in documents_cases_service and calling your existing `CaseFileService.create(...)`.
- If all SARs today are created only via your document upload flow, no change needed; just document that “uploading a SAR document creates a case when one doesn’t exist.”

---

## 4. Analysts can review, refer, and close cases

**Your scope: documents_cases_service** (and frontend that calls it).

**Already in place:**

- Backend: `GET /api/cases`, `GET /api/cases/{id}`, `PATCH`, `POST /api/cases/{id}/refer`, `POST /api/cases/{id}/close`; analysts see all cases via role `ANALYST`.
- Frontend: case list, case detail, Refer and Close buttons with confirmation.

**What you need to do:**

- **Verify end-to-end:** Confirm analyst role can list cases, open a case, add notes, refer (with agency), and close. Fix any bugs (e.g. validation, error messages, or UI state).
- No new feature work unless you find gaps.

---

## 5. CTR/SAR PDF upload and extraction

**Your scope: documents_cases_service.**

**Upload (done):**

- Multipart upload, S3 storage, DB metadata (type, ctrId/sarId/caseId), and [DocumentUploadEvent](backend/services/documents_cases_service/src/main/java/com/skillstorm/finsight/documents_cases/dtos/DocumentUploadEvent.java) published to RabbitMQ.

**What you need to do:**

- **If “create new CTR/SAR” on upload must work:**  
  [ComplianceEventServiceClient](backend/services/documents_cases_service/src/main/java/com/skillstorm/finsight/documents_cases/services/ComplianceEventServiceClient.java) currently throws for `createCtrRecord()` and `createSarRecord()`. Implement a REST client to the compliance service’s create-CTR and create-SAR endpoints (or equivalent), so uploads without an existing `ctrId`/`sarId` can create a record and then link the document. Document URL and contract.

- **If PDF extraction is your responsibility:**  
  - Add a consumer (in documents_cases_service or a small worker) that subscribes to your document-upload queue.
  - On event: download PDF from S3 (e.g. using the presigned URL or bucket/key), extract text (e.g. Apache PDFBox or similar), then either:
    - Store extracted fields in your DB (if you have a schema for that), or
    - Call the compliance service to create/update CTR or SAR with extracted data (requires the client above and an API that accepts extracted fields).
  - If extraction is owned by another team, document that the event is published and they must consume it; no extraction code in your services.

---

## 6. Law enforcement can view referred cases

**Your scope: documents_cases_service.**

**Already in place:**

- [CaseFileRepository.findVisibleToLawEnforcement()](backend/services/documents_cases_service/src/main/java/com/skillstorm/finsight/documents_cases/repositories/CaseFileRepository.java): cases with `status = REFERRED` or `(status = CLOSED` and `referred_at IS NOT NULL)`.
- [CaseFileService.findAll()](backend/services/documents_cases_service/src/main/java/com/skillstorm/finsight/documents_cases/services/CaseFileService.java) uses this when the user has role `LAW_ENFORCEMENT_USER`; same filter is used for case notes and document visibility.

**What you need to do:**

- **Verify:** Confirm that users with `LAW_ENFORCEMENT_USER` only see referred (or previously referred) cases and related notes/documents. Ensure the identity/auth service can assign this role and that the frontend sends the same JWT so documents-cases sees the correct role.
- No backend change needed unless you find a bug.

---

## Suspect Registry and the MVP

Today, no MVP criterion explicitly requires suspect registry. If the product needs **case–suspect linkage** (e.g. show suspect(s) on a case for analysts or law enforcement):

- **Option A:** Documents-cases or the frontend calls **suspect_registry_service** (e.g. “list suspects” or “get suspect by id”) and enriches the case view. Suspect registry only needs stable APIs (e.g. `GET /api/suspects`, `GET /api/suspects/{id}`); no case/SAR concepts inside it.
- **Option B:** If cases must be “linked” to suspects in data (e.g. case has `suspect_id` or a link table), then either:
  - Add a `case_id` (or similar) in suspect_registry and APIs to link/list by case, or
  - Add a reference (e.g. `suspect_id`) in documents_cases and resolve suspect details via suspect registry when displaying.

**What you need to do:**

- If case–suspect linkage is in scope for MVP: add the integration (documents-cases or frontend calling suspect registry, and any linking data you agree on). Otherwise, ensure suspect registry APIs are stable and documented for future use.

---

## Checklist (your two services only)

| Item | Service | Action |
|------|---------|--------|
| SARs create cases | documents_cases | Document behavior; add event consumer or document `POST /api/cases` for others if SARs are created elsewhere. |
| Analysts review, refer, close | documents_cases | Verify E2E; fix bugs. |
| CTR/SAR upload | documents_cases | Done. Optional: implement ComplianceEventServiceClient if “create new” on upload is required. |
| CTR/SAR extraction | documents_cases | If yours: add consumer + PDF extraction + compliance API or DB. If not: document event contract. |
| Law enforcement view referred | documents_cases | Verify role and visibility; fix if needed. |
| Case–suspect linkage | suspect_registry + documents_cases | If in MVP: add API usage and/or linking; else keep registry APIs stable. |
