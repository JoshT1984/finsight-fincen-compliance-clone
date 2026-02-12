# SSN Matching and Microservice Communication

## SSN matching when SSN is encrypted

**Yes, SSN matching works with encryption.**

In the **suspect_registry_service**, SSN is handled in two ways:

1. **Stored value (`ssn` column)**  
   - Persisted **encrypted** (AES-256-GCM) via `SsnEncryptor`.  
   - Encryption is **non-deterministic** (random IV), so the same SSN produces different ciphertext each time.  
   - Used for display/decryption when needed; **not** used for lookups.

2. **Lookup value (`ssn_hash` column)**  
   - **Deterministic SHA-256 hash** of the normalized 9-digit SSN (see `SsnHashUtil`).  
   - Same SSN always yields the same hash (e.g. `123-45-6789` and `123456789` hash the same).  
   - All matching is done by **hash**: `findBySsnHash(hash)`.

**Flow:**

- **documents_cases_service** extracts plaintext SSN from the PDF and sends it in the CTR/SAR payload (`externalSubjectKey = "SSN:123456789"`).
- **compliance_event_service** sends that **plaintext SSN** to suspect_registry in `POST /api/suspects/find-or-create` (over HTTPS).
- **suspect_registry_service** normalizes the SSN to digits, hashes it with SHA-256, and looks up by `ssn_hash`. It never stores the plaintext SSN in a queryable form; the encrypted `ssn` is for at-rest protection only.

So: **matching is by hash; encryption is for storage.** No need to decrypt to match.

---

## How the microservices communicate

### 1. documents_cases_service

- **Outbound REST (synchronous)**  
  - **→ compliance_event_service**  
  - When a CTR or SAR PDF is uploaded without an existing `ctrId`/`sarId`, the service extracts the PDF, builds `CreateCtrRequestPayload` or `CreateSarRequestPayload`, and calls:
    - `ComplianceEventServiceClient.createCtr(request)` or `createSar(request)`.
  - Uses `RestTemplate` (`complianceEventRestTemplate`), base URL from `compliance-event-service.url` (e.g. `http://localhost:8085`).

- **Outbound messaging (asynchronous)**  
  - **→ RabbitMQ**  
  - After saving a document record (and after creating the CTR/SAR via REST when applicable), it publishes a `DocumentUploadEvent` to the appropriate queue (CTR or SAR).  
  - Used for any downstream consumers (e.g. analytics, audit); the actual creation of the CTR/SAR record is done synchronously via REST above.

### 2. compliance_event_service

- **Inbound REST**  
  - Receives `POST /api/compliance-events/ctr` and `POST /api/compliance-events/sar` from documents_cases_service (and any other allowed callers).

- **Outbound REST (synchronous)**  
  - **→ suspect_registry_service**  
  - When creating a CTR or SAR, if `externalSubjectKey` is `SSN:xxxxxxxxx`, it:
    1. Calls `POST /api/suspects/find-or-create` with SSN and name (and adds alias when the name differs).
    2. If the form data contains parsed address (`_parsedAddressLine1`, etc.), calls `POST /api/suspects/{id}/addresses/from-form` to attach the address to the suspect.
  - Uses `SuspectRegistryClient` with `RestTemplate`; base URL from `finsight.suspect-registry.url`. If that property is not set, the client is not created and suspect linking is skipped.

### 3. suspect_registry_service

- **Inbound REST only**  
  - Exposes suspect, alias, and address APIs, including:
    - `GET /api/suspects/by-ssn?ssn=...`
    - `POST /api/suspects/find-or-create`
    - `POST /api/suspects/{id}/addresses/from-form`

### Summary

| From                    | To                       | Mechanism   | When / purpose                                      |
|-------------------------|--------------------------|------------|-----------------------------------------------------|
| documents_cases_service | compliance_event_service | REST       | Create CTR/SAR from extracted PDF (sync)            |
| documents_cases_service | RabbitMQ                 | AMQP       | Publish document upload event (async)              |
| compliance_event_service | suspect_registry_service | REST       | Find-or-create suspect by SSN, add address (sync)  |

All inter-service REST calls are synchronous (request/response). RabbitMQ is used only for the document-upload event after the document (and, when applicable, the CTR/SAR) is already created.

---

## SAR address parsing

SAR (Form 109) PDFs are now parsed for subject/individual address in the same way as CTR (Form 104). Extracted fields (`_parsedAddressLine1`, `_parsedAddressCity`, `_parsedAddressState`, `_parsedAddressPostalCode`, `_parsedAddressCountry`) are stored in the SAR `formData`. When the compliance service creates the SAR and links a suspect by SSN, it pushes any parsed address to the suspect via `POST /api/suspects/{id}/addresses/from-form`, so SAR uploads with an address also add that address to the suspect.
