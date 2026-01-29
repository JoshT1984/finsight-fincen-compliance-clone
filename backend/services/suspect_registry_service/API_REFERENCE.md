# Suspect Registry Service – API Reference

Base URL: `http://localhost:8080` (or your configured port)

---

## 1. Create Organization

**POST** `/api/organizations`

**Request body:**
```json
{
  "name": "Acme Cartel",
  "type": "CARTEL"
}
```

| Field | Required | Description |
|-------|----------|-------------|
| name  | Yes      | Organization name (max 256 chars) |
| type  | No       | One of: `CARTEL`, `GANG`, `TERRORIST`, `FRAUD_RING`, `MONEY_LAUNDERING`, `OTHER` (default: `OTHER`) |

---

## 2. Create Alias (attaches alias to suspect)

**POST** `/api/aliases`

**Request body:**
```json
{
  "suspectId": 1,
  "aliasName": "Johnny Doe",
  "aliasType": "AKA",
  "isPrimary": false
}
```

| Field     | Required | Description |
|-----------|----------|-------------|
| suspectId | Yes      | ID of the suspect to attach this alias to |
| aliasName | Yes      | Alias name (max 256 chars) |
| aliasType | No      | One of: `AKA`, `LEGAL`, `NICKNAME`, `BUSINESS` (default: `AKA`) |
| isPrimary | No      | Whether this is the primary alias (default: false) |

---

## 3. Create Suspect

**POST** `/api/suspects`

**Request body:**
```json
{
  "primaryName": "John Doe",
  "dob": "1985-03-15",
  "ssn": "123-45-6789",
  "riskLevel": "MEDIUM"
}
```

| Field      | Required | Description |
|------------|----------|-------------|
| primaryName | Yes    | Primary name (max 256 chars) |
| dob        | No      | Date of birth (YYYY-MM-DD) |
| ssn        | No      | SSN, 9 digits (e.g. `123-45-6789` or `123456789`) |
| riskLevel  | No      | One of: `UNKNOWN`, `LOW`, `MEDIUM`, `HIGH` (default: `UNKNOWN`) |

---

## 4. Create Address

**POST** `/api/addresses`

**Request body:**
```json
{
  "line1": "123 Main St",
  "line2": "Apt 4B",
  "city": "New York",
  "state": "NY",
  "postalCode": "10001",
  "country": "US"
}
```

| Field     | Required | Description |
|-----------|----------|-------------|
| line1     | Yes      | Address line 1 (max 256 chars) |
| line2     | No       | Address line 2 (max 256 chars) |
| city      | Yes      | City (max 128 chars) |
| state     | No       | State (max 64 chars) |
| postalCode| No       | Postal/ZIP code (max 32 chars) |
| country   | Yes      | Country (max 64 chars) |

---

## 5. Attach Suspect to Organization

**POST** `/api/suspects/{suspectId}/organizations`

**Example:** `POST` `/api/suspects/1/organizations`

**Request body:**
```json
{
  "orgId": 1,
  "role": "member"
}
```

| Field | Required | Description |
|-------|----------|-------------|
| orgId | Yes      | Organization ID to link |
| role  | No       | Suspect’s role in the organization (max 64 chars) |

---

## 6. Attach Address to Suspect

**POST** `/api/suspects/{suspectId}/addresses`

**Example:** `POST` `/api/suspects/1/addresses`

**Request body:**
```json
{
  "addressId": 1,
  "addressType": "HOME",
  "isCurrent": true
}
```

| Field      | Required | Description |
|------------|----------|-------------|
| addressId  | Yes      | Address ID to link |
| addressType| No       | One of: `HOME`, `WORK`, `MAILING`, `UNKNOWN` (default: `UNKNOWN`) |
| isCurrent  | No       | Whether this is the current address (default: true) |

---

## 7. Custom Search / Query Endpoints

### Suspects by organization

**GET** `/api/suspects/by-organization/{orgId}`

**Example:** `GET` `/api/suspects/by-organization/1`

Returns all suspects linked to the given organization.

---

### Aliases by suspect

**GET** `/api/suspects/{suspectId}/aliases`

**Example:** `GET` `/api/suspects/1/aliases`

Returns all aliases for the given suspect.

---

### Addresses by suspect

**GET** `/api/suspects/{suspectId}/addresses`

**Example:** `GET` `/api/suspects/1/addresses`

Returns all addresses linked to the given suspect.

---

## 8. Standard CRUD Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET    | `/api/organizations` | List all organizations |
| GET    | `/api/organizations/{id}` | Get organization by ID |
| PATCH  | `/api/organizations/{id}` | Update organization |
| DELETE | `/api/organizations/{id}` | Delete organization |
| GET    | `/api/aliases` | List all aliases |
| GET    | `/api/aliases/{id}` | Get alias by ID |
| PATCH  | `/api/aliases/{id}` | Update alias |
| DELETE | `/api/aliases/{id}` | Delete alias |
| GET    | `/api/suspects` | List all suspects |
| GET    | `/api/suspects/{id}` | Get suspect by ID |
| PATCH  | `/api/suspects/{id}` | Update suspect |
| DELETE | `/api/suspects/{id}` | Delete suspect |
| GET    | `/api/addresses` | List all addresses |
| GET    | `/api/addresses/{id}` | Get address by ID |
| PATCH  | `/api/addresses/{id}` | Update address |
| DELETE | `/api/addresses/{id}` | Delete address |

---

## 9. Example Workflow (cURL)

```bash
# 1. Create organization
curl -X POST http://localhost:8080/api/organizations \
  -H "Content-Type: application/json" \
  -d '{"name": "Acme Cartel", "type": "CARTEL"}'

# 2. Create suspect
curl -X POST http://localhost:8080/api/suspects \
  -H "Content-Type: application/json" \
  -d '{"primaryName": "John Doe", "dob": "1985-03-15", "riskLevel": "MEDIUM"}'

# 3. Create alias (attaches to suspect 1)
curl -X POST http://localhost:8080/api/aliases \
  -H "Content-Type: application/json" \
  -d '{"suspectId": 1, "aliasName": "Johnny Doe", "aliasType": "AKA"}'

# 4. Create address
curl -X POST http://localhost:8080/api/addresses \
  -H "Content-Type: application/json" \
  -d '{"line1": "123 Main St", "city": "New York", "state": "NY", "postalCode": "10001", "country": "US"}'

# 5. Attach suspect 1 to organization 1
curl -X POST http://localhost:8080/api/suspects/1/organizations \
  -H "Content-Type: application/json" \
  -d '{"orgId": 1, "role": "member"}'

# 6. Attach address 1 to suspect 1
curl -X POST http://localhost:8080/api/suspects/1/addresses \
  -H "Content-Type: application/json" \
  -d '{"addressId": 1, "addressType": "HOME", "isCurrent": true}'

# 7. Get suspects by organization
curl http://localhost:8080/api/suspects/by-organization/1

# 8. Get aliases for suspect 1
curl http://localhost:8080/api/suspects/1/aliases

# 9. Get addresses for suspect 1
curl http://localhost:8080/api/suspects/1/addresses
```

---

## 10. Patch Request Bodies (partial updates)

**PATCH** requests accept only the fields you want to change. Omit fields to leave them unchanged.

**PATCH** `/api/organizations/{id}`:
```json
{"name": "New Name", "type": "GANG"}
```

**PATCH** `/api/aliases/{id}`:
```json
{"aliasName": "New Alias", "aliasType": "LEGAL", "isPrimary": true}
```

**PATCH** `/api/suspects/{id}`:
```json
{"primaryName": "Updated Name", "riskLevel": "HIGH"}
```

**PATCH** `/api/addresses/{id}`:
```json
{"line1": "456 Oak Ave", "city": "Boston"}
```
