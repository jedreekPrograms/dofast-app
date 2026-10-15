# doFast — Local Task Marketplace

A full-stack application for publishing local paid tasks, finding nearby work and coordinating a task from acceptance through completion and settlement.

**Status: actively in development.** Core workflows and supporting tests are implemented; product polish, external integrations and deployment validation are ongoing. Use provider test environments for local payment development.

## Product workflow

A requester publishes a task with its location and funding requirements. Another user discovers the task and accepts it or submits a proposal. The application coordinates progress, communication, completion and settlement, with supporting review and dispute workflows.

| Area | Implementation in this repository |
| --- | --- |
| Accounts and access | Account management, browser-session security and authorization policies |
| Tasks | Publication, discovery, assignment, cancellation and lifecycle rules |
| Location | PostGIS nearby queries, route and tracking features, exact-location privacy |
| Payments | Stripe integration, escrow transitions, wallet ledger and reconciliation logic |
| Communication and trust | Chat, notifications, reviews, verification and disputes |

Feature coverage and operational readiness continue to evolve. The domain documents linked below describe the current contracts and limitations.

## Backend engineering

- **Modular monolith:** domain boundaries remain explicit while related state changes can share transaction boundaries.
- **PostgreSQL as the source of truth:** job state, financial records and ownership rules remain in the transactional database.
- **PostGIS discovery:** geographic matching is performed in indexed database queries.
- **Versioned schema changes:** Flyway owns migrations rather than Hibernate schema auto-update.
- **External integration boundaries:** payment and provider behavior is separated from application use cases.
- **Privacy and abuse controls:** access policies, location retention, attachment handling and Redis-backed request budgets are documented and tested.

## Technology

| Layer | Stack |
| --- | --- |
| API | Java 21, Spring Boot 4.1, Spring Data JPA/Hibernate, Spring Security |
| Data | PostgreSQL 18 / PostGIS, Flyway, Redis 7.4 |
| Web | React 19, Vite |
| Runtime | Docker Compose, Nginx |
| Validation | Backend and frontend tests, runtime smoke scripts, GitHub Actions configuration |

## Run locally

Requirements: Docker Desktop or Docker Engine with Compose v2.

From the repository root:

```shell
cp .env.example .env
docker compose up --build
```

Windows PowerShell:

```powershell
Copy-Item .env.example .env
docker compose up --build
```

Review `.env` before starting. Its provider keys are placeholders; exercising Stripe or other external integrations requires the corresponding test configuration. A running local stack does not by itself demonstrate a configured payment-provider flow.

| Service | Local address |
| --- | --- |
| Web | http://localhost:5173 |
| API | http://localhost:8080 |
| API health | http://localhost:8080/actuator/health |
| PostgreSQL / PostGIS | `localhost:5434` |

For backend development outside Docker, use Java 21 and follow the [development guide](docs/DEVELOPMENT.md). Web development outside Docker uses the Node.js version documented there.

## Repository map

| Path | Purpose |
| --- | --- |
| [apps/api](apps/api/) | Spring Boot API and backend tests |
| [apps/web](apps/web/) | Customer and administration interface |
| [infra](infra/) | Gateway and deployment configuration |
| [docs](docs/) | Architecture, domain contracts and operating guides |
| [scripts](scripts/) | Development helpers |
| [compose.yaml](compose.yaml) | Local services |
| [.env.example](.env.example) | Example configuration |

## Checks

```shell
mvn -B -f apps/api/pom.xml verify
npm --prefix apps/web ci
npm --prefix apps/web test
npm --prefix apps/web run lint
npm --prefix apps/web run build
```

Some integration checks need additional services. Redis integration tests and the container-based smoke scenarios have their prerequisites configured in the [CI workflow](.github/workflows/ci.yml) and [.github/scripts](.github/scripts/).

**Current CI configuration note:** the default branch is `main`, while the primary workflow still filters `master` for its default-branch push/PR triggers. Updating those filters remains necessary for automatic coverage of `main`.

## Explore the design

| Topic | Documentation |
| --- | --- |
| System boundaries | [Architecture](docs/ARCHITECTURE.md) |
| Ownership and task transitions | [Job lifecycle](docs/JOB_LIFECYCLE.md) |
| Geographic discovery | [Location and matching](docs/LOCATION_AND_MATCHING.md) |
| Money and auditability | [Payments and ledger](docs/PAYMENTS_AND_LEDGER.md) |
| Access and security | [Accounts](docs/ACCOUNTS_AND_ACCESS.md), [security](docs/SECURITY.md) |
| Distributed abuse budgets | [Rate limiting](docs/DISTRIBUTED_RATE_LIMITING.md) |
| Operations | [Deployment](docs/PRODUCTION_DEPLOYMENT.md), [backup and restore](docs/BACKUP_AND_RESTORE.md) |

The repository uses the Carlisle technical baseline.
