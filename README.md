# MNP Portability

A Mobile Number Portability (MNP) service. A **recipient** operator submits a porting request for a phone number, and the **donor** operator (the one currently holding the number) accepts or rejects it. Requests the donor never answers are canceled automatically after 2 minutes.

- **Backend:** Java 17, Spring Boot 4.1, Spring Data JPA, Flyway, MySQL 8.4
- **Frontend:** Angular 20, served by nginx
- **Everything runs with one command:** `docker compose up --build`

## Contents

- [Quick start](#quick-start)
- [Try the flow](#try-the-flow)
- [Configuration](#configuration)
- [Running the tests](#running-the-tests)
- [Local development without Docker](#local-development-without-docker)
- [Business rules](#business-rules)
- [API reference](#api-reference)
- [Architecture and design decisions](#architecture-and-design-decisions)
- [Database](#database)
- [Assumptions](#assumptions)
- [Git workflow and versioning](#git-workflow-and-versioning)

## Quick start

**Requirements:** Docker with Compose v2. Nothing else (no Java, Node or MySQL) is needed on the host.

```bash
git clone git@github.com:Ramadan-Lotfy/mnp-portability.git
cd mnp-portability
docker compose up --build
```

The first build downloads Maven and npm dependencies and can take several minutes; later builds reuse the cache. Compose starts the services in order (MySQL, then the backend once MySQL is healthy, then the frontend once the backend is healthy). Flyway creates the schema and seeds the operators on the first backend start.

| What | URL |
|---|---|
| Web UI | http://localhost:8081 |
| REST API | http://localhost:8080/api/v1 |
| Swagger UI | http://localhost:8080/swagger-ui.html |
| Health check | http://localhost:8080/actuator/health |
| MySQL (for DB clients) | `localhost:3307`, database `mnp`, user `mnp`, password `mnp` |

Check that everything is up (all three services should show `healthy`):

```bash
docker compose ps
curl -s http://localhost:8080/actuator/health
```

Stop and reset:

```bash
docker compose down        # stop, keep the data
docker compose down -v     # stop and delete the database volume (a completely fresh start)
```

## Try the flow

In the web UI, use the **Acting as** selector in the header to switch between Vodafone, Orange and Etisalat. The same flow with `curl`:

```bash
# 1. Orange asks for a Vodafone number -> 201, status PENDING, donor "vodafone"
curl -s -X POST localhost:8080/api/v1/porting-requests \
  -H "organization: orange" -H "Content-Type: application/json" \
  -d '{"phoneNumber":"01012345678"}'

# 2. Nobody else can request the same number while it is pending -> 409
curl -s -X POST localhost:8080/api/v1/porting-requests \
  -H "organization: etisalat" -H "Content-Type: application/json" \
  -d '{"phoneNumber":"01012345678"}'

# 3. Only the donor can decide: Orange (the recipient) gets 403, Vodafone gets 200
curl -s -X POST localhost:8080/api/v1/porting-requests/1/accept -H "organization: orange"
curl -s -X POST localhost:8080/api/v1/porting-requests/1/accept -H "organization: vodafone"

# 4. The number now belongs to Orange
curl -s localhost:8080/api/v1/phone-numbers/01012345678 -H "organization: etisalat"
```

To watch the timeout without waiting 2 minutes, copy `.env.example` to `.env`, set `PORTING_REQUEST_TIMEOUT=20s` and `PORTING_TIMEOUT_CHECK_INTERVAL=5s`, and run `docker compose up -d`.

## Configuration

Every value has a default, so `docker compose up` works without any configuration. To override, copy `.env.example` to `.env`.

| Variable | Default | Meaning |
|---|---|---|
| `MYSQL_DATABASE` / `MYSQL_USER` / `MYSQL_PASSWORD` | `mnp` | Database name and credentials |
| `MYSQL_ROOT_PASSWORD` | `root` | MySQL root password |
| `FRONTEND_PORT` | `8081` | Host port of the web UI |
| `APP_PORT` | `8080` | Host port of the API |
| `MYSQL_HOST_PORT` | `3307` | Host port of MySQL |
| `PORTING_REQUEST_TIMEOUT` | `2m` | How long a request may stay PENDING |
| `PORTING_TIMEOUT_CHECK_INTERVAL` | `15s` | How often the cleanup job runs |

## Running the tests

From `backend/`:

```bash
./mvnw test      # unit tests + web-layer tests; fast, no Docker needed
./mvnw verify    # the above plus the integration test on a real MySQL; Docker must be running
```

| Layer | Covers |
|---|---|
| Unit (JUnit 5, Mockito) | State transitions, visibility rules, donor/recipient rules, the 2-minute timeout boundary (using a fixed clock), number status and current-holder logic, phone number format validation |
| Web layer (`@WebMvcTest`) | The HTTP contract: 401, 400, 403, 404, 409, 422, response shapes, the `organization` header |
| Integration (`*IT`, Testcontainers MySQL 8.4) | The full lifecycle against the real schema: Flyway migrations, JPA mappings, the visibility query, the database-level unique guard, and the timeout sweep, with time advanced by a controllable clock instead of waiting |

The integration test starts its own MySQL container on a random port, so it does not interfere with the Compose stack.

## Local development without Docker

```bash
# 1. MySQL
docker run --name mnp-mysql -e MYSQL_ROOT_PASSWORD=root \
  -e MYSQL_DATABASE=mnp -e MYSQL_USER=mnp -e MYSQL_PASSWORD=mnp \
  -p 3306:3306 -d mysql:8.4

# 2. Backend (from backend/), http://localhost:8080
./mvnw spring-boot:run

# 3. Frontend (from frontend/, Node 20.19+ or 22.12+), http://localhost:4200
npm install
npx ng serve --proxy-config proxy.conf.json
```

Stop the Compose stack first if it is running, because both want port 8080. The Angular dev server proxies `/api` to the backend, so the browser only ever talks to one origin and no CORS configuration is needed.

## Business rules

| Operator | Header value | Number range |
|---|---|---|
| Vodafone | `vodafone` | 01000000000 – 01099999999 |
| Etisalat | `etisalat` | 01100000000 – 01199999999 |
| Orange | `orange` | 01200000000 – 01299999999 |

```
Start --invalid--> (rejected with HTTP 400, never stored)
  |
  +--valid--> PENDING --donor rejects--> REJECTED
                 |    --donor accepts--> ACCEPTED
                 +--- 2 min timeout ---> CANCELED
```

- A request contains **exactly one** phone number.
- A number with a **pending request** cannot be requested again.
- **Only the donor** can accept or reject.
- **Visibility:** the donor and recipient of a request see it in any status; every other operator sees it only once it is `ACCEPTED`.
- A **background job** cancels pending requests older than the timeout (2 minutes).
- Requests are **schema-validated**; invalid ones are rejected.
- Operators can query a number's **status and current holder**.
- **Authentication and authorization are mocked** with the `organization` request header.

## API reference

All endpoints are under `/api/v1` and require the `organization` header (`vodafone`, `orange` or `etisalat`, case-insensitive). Interactive documentation is in Swagger UI.

| Method | Path | Description |
|---|---|---|
| `POST` | `/porting-requests` | Submit a request. The caller is the recipient |
| `GET` | `/porting-requests` | List what the caller may see. Query: `status`, `page`, `size` (max 100) |
| `GET` | `/porting-requests/{id}` | One request |
| `POST` | `/porting-requests/{id}/accept` | Donor accepts |
| `POST` | `/porting-requests/{id}/reject` | Donor rejects |
| `GET` | `/phone-numbers/{phoneNumber}` | Number status and current holder |

Lists are always newest first. The sort order is fixed.

**Submit**

```json
POST /api/v1/porting-requests
{ "phoneNumber": "01012345678" }
```

```json
201 Created
{
  "id": 1,
  "phoneNumber": "01012345678",
  "donor": "vodafone",
  "recipient": "orange",
  "status": "PENDING",
  "createdAt": "2026-10-08T10:00:00Z",
  "updatedAt": "2026-10-08T10:00:00Z"
}
```

**Number status**

```json
GET /api/v1/phone-numbers/01012345678
{
  "phoneNumber": "01012345678",
  "status": "PORTED",
  "currentOperator": "orange",
  "originalOperator": "vodafone"
}
```

`status` is `NOT_PORTED`, `PORTED`, or `PORTING_PENDING`. A pending request is only reported to its own donor and recipient, so other operators cannot learn about it.

**Errors** always have one shape:

```json
{ "timestamp": "...", "status": 409, "error": "Conflict", "message": "Phone number 01012345678 already has a pending porting request", "path": "/api/v1/porting-requests" }
```

| Status | When |
|---|---|
| 400 | Malformed body or parameter, or not an 11-digit number starting with `01` |
| 401 | Missing or unknown `organization` header |
| 403 | The caller is not the donor of the request it tries to decide |
| 404 | Unknown id, a request the caller may not see, or (on the status endpoint) a number in no operator range |
| 409 | The number already has a pending request; the request is already decided or timed out; or a concurrent change won |
| 422 | The number belongs to no operator range, or the recipient already holds it |

## Architecture and design decisions

```
mnp-portability/
├── docker-compose.yml
├── .env.example
├── backend/                         Spring Boot application
│   └── src/main/
│       ├── java/com/mnp/portability/
│       │   ├── common/              config, mocked security, error handling
│       │   ├── operator/            operators and their number ranges
│       │   ├── portingrequest/      the core feature: entity, service, controller, timeout job
│       │   └── phonenumber/         number status and phone number validation
│       └── resources/db/migration/  Flyway: schema and seed data
└── frontend/                        Angular app + nginx
```
