# FinSight (SkillStorm Project 3) — Microservices Scaffold

FinSight is a FINCEN-inspired MVP demo for modeling real-world AML workflows:
- CTR ingestion/aggregation
- SAR generation + detection rule hits
- Case creation/notes + referrals
- Document upload to S3 (metadata stored in DB)
- Event-driven integration (SQS/EventBridge)

This repository is a **starting foundation/scaffold** intended to be expanded into a full working demo.

---

## Repository Layout

skillstorm-project3/
frontend/ # Angular app (UI)
backend/
services/ # Spring Boot microservices (one per bounded context)
identity-auth-service/
transactions-ctr-service/
sar-detection-service/
documents-cases-service/
platform/ # Shared platform/infrastructure assets (deploy + local dev)
local/ # Local dev configs (docker compose, local scripts)
ecs/ # ECS task/service/ALB templates (if used)
docs/ # Optional: diagrams, screenshots, writeups

<img src="https://github.com/JoshT1984/finsight-fincen-compliance-clone/blob/main/skillstorm-project3/frontend/public/images/landing-page.png?raw=true" alt="Login Page" width="600">

<img src="https://github.com/JoshT1984/finsight-fincen-compliance-clone/blob/main/skillstorm-project3/frontend/public/images/Suspicion_Score.png?raw=true" alt="Dashboard" width="600">

<img src="https://github.com/JoshT1984/finsight-fincen-compliance-clone/blob/main/skillstorm-project3/frontend/public/images/cases.png?raw=true" alt="Case Details" width="600">

<img src="https://github.com/JoshT1984/finsight-fincen-compliance-clone/blob/main/skillstorm-project3/frontend/public/images/doc_upload.png?raw=true" alt="Document Upload" width="600">


### Why this structure?
- **frontend/** is isolated from backend runtime dependencies
- **backend/services/** maps 1:1 to microservice bounded contexts
- **backend/platform/** keeps platform concerns (infra, local tooling, ECS manifests) out of service code

---

## Architecture Overview (Target Deployment)

Planned AWS deployment pattern:

- **ALB** as a single entrypoint
- **ECS Cluster** (Fargate) for compute
- **4 ECS Services** (one per microservice)
- **Target Groups + ALB path routing** to each service
- **Postgres (RDS)** for structured data
- **DynamoDB** for non-structured data (timelines/signals/audit-like streams)
- **S3** for documents (presigned URL upload/download)
- **SQS/EventBridge** for events (CTR→SAR, SAR→Case, Document signals)
- **CloudWatch Logs + X-Ray / OpenTelemetry** for observability

---

## Microservices (Bounded Contexts)

### 1) identity-auth-service
Owns:
- users, roles, oauth identities
Stores:
- structured user data (Postgres)

### 2) transactions-ctr-service
Owns:
- cash transactions
- CTR rollups/aggregations
Stores:
- structured transaction + CTR data (Postgres)

### 3) sar-detection-service
Owns:
- SAR records
- detection rules + rule hits
- SAR ↔ CTR mapping (by ID)
Stores:
- structured SAR data (Postgres)
- optional non-structured detection signals (DynamoDB)

### 4) documents-cases-service
Owns:
- case files (1 Case per SAR)
- case notes
- document metadata (S3 paths)
- audit events
Stores:
- structured case/document/audit data (Postgres)
- optional non-structured timelines (DynamoDB)

> Note: Cross-service relationships use **external IDs** (no cross-service foreign keys).

---

## Database Initialization

Each service contains a database schema file at:


Each service is configured with:
- `spring.jpa.hibernate.ddl-auto: none`
- `spring.sql.init.mode: always` (so `schema.sql` runs on startup)

---

## Local Development (High Level)

### Message Queues (RabbitMQ)

Start RabbitMQ for document upload events:

```bash
docker compose up -d
```

See [docker/README.md](docker/README.md) for details.

### Frontend (Angular)
From `frontend/`:
- Install: `npm install`
- Run: `ng serve`
- App: `http://localhost:4200`

### Backend Services (Spring Boot)
From each service folder:
- Run: `mvn spring-boot:run`

Each service uses environment variables for DB/AWS:
- `PORT`
- `DB_URL`, `DB_USER`, `DB_PASS`
- `AWS_REGION`
- `DDB_ENDPOINT` (local DynamoDB optional)
- `RABBITMQ_HOST`, `RABBITMQ_PORT` (default localhost:5672)

---

## Frontend UI Template (FINCEN-inspired)

The UI foundation is based on two templates:
- `index.html` (layout reference)
- `styles.css` (global styling + world background + sticky footer)

Angular requires `src/index.html` to remain minimal and contain `<app-root></app-root>`.
The template markup from `index.html` is moved into Angular components:

- Header/Nav → `src/app/layout/header`
- Footer → `src/app/layout/footer`
- Page wrapper + router outlet → `src/app/layout/shell`
- Home page content → `src/app/pages/home`

The template CSS from `styles.css` is used as the Angular global stylesheet:
- `frontend/src/styles.css`

---

## What’s Next
- Implement minimal endpoints per service (`/health`, `/api/...`)
- Add event contracts (SQS/EventBridge messages)
- Add presigned S3 upload/download in documents-cases-service
- Build Angular routes for Cases/CTRs/SARs/Upload
- Add OpenSearch (optional) for investigator search


