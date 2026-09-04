# 🛒 Ecommerce Platform

A production-oriented ecommerce platform built with **Spring Boot 4.1** (Java 17) and **React 18 + TypeScript**, focused on concurrency safety under real-world checkout pressure — not just CRUD.

## Tech Stack

### Backend
- **Spring Boot 4.1** — Spring MVC, Spring Data JPA, Spring Security
- **PostgreSQL** — JPA with Hibernate
- **JWT Authentication** — access + rotating refresh tokens
- **Razorpay** — payment orders, signature verification, webhooks
- **Lombok**, **Testcontainers** integration testing (75 test methods)

### Frontend
- **React 18 + TypeScript**
- **Vite**, **Tailwind CSS 4**
- **React Router**, **Axios**

## Features

- **Auth** — register, login, refresh-token rotation with reuse detection
- **Role-based access** — user vs. admin endpoints
- **Catalog** — products, categories, search/filter via JPA Specifications
- **Cart** — add/update/remove, stock-validated checkout
- **Orders** — lifecycle (PENDING → PAID/CANCELLED), auto-expiry job
- **Payments** — Razorpay order creation, client-confirm + webhook verification
- **Inventory** — atomic stock reservation and release

## Notable Design Decisions

- **No overselling under concurrent checkout** — stock is decremented via a single atomic `UPDATE ... WHERE stock >= quantity` (not read-then-write), verified by a 20-thread Testcontainers test (`StockConcurrencyIT`).
- **Refresh-token rotation with reuse detection** — each refresh invalidates its predecessor; reusing a revoked/replaced token revokes the entire token family.
- **Idempotent payment confirmation** — Razorpay signature is verified on both the client-confirm call and the webhook, so a retried webhook can't double-mark an order as paid (single atomic `PENDING → PAID` status flip).
- **Race-safe order cancellation** — cancellation is a conditional UPDATE; user cancel, admin cancel, and the expiry job race on the same claim, so stock can never be double-released.

## Prerequisites

- **Java 17+**
- **Node.js 18+**
- **PostgreSQL**
- **Docker** (for integration tests via Testcontainers)
- **Razorpay test keys** (for payment features)

## Setup

### 1. Configure environment

```bash
cp .env.example .env
```

Edit `.env` with your values:

| Variable | Description |
|---|---|
| `DB_URL` | JDBC URL (e.g. `jdbc:postgresql://localhost:5432/ecommerce`) |
| `DB_USERNAME` | Database user |
| `DB_PASSWORD` | Database password |
| `JWT_SECRET_KEY` | High-entropy string for JWT signing |
| `ADMIN_PASSWORD` | Password for the auto-created `admin` user |
| `RAZORPAY_KEY_ID` | Razorpay test key |
| `RAZORPAY_KEY_SECRET` | Razorpay test secret |
| `RAZORPAY_WEBHOOK_SECRET` | Webhook signing secret |

### 2. Start the backend

```bash
./start.sh    # loads .env and runs ./mvnw spring-boot:run
```

Or manually:

```bash
export $(grep -v '^#' .env | xargs)
./mvnw spring-boot:run
```

The API runs at **`http://localhost:8080`**.

On first boot, `DataInitializer` creates an **admin user** (`admin` / your `${ADMIN_PASSWORD}`) if none exists.

### 3. Start the frontend

```bash
cd frontend
npm install
npm run dev
```

Runs at **`http://localhost:5173`**.

## Testing

```bash
./mvnw verify
```

- **75 test methods** across service, controller, and integration layers
- Includes a **20-thread concurrency test** (`Order/StockConcurrencyIT`) that verifies no overselling
- **Requires Docker** — tests spin up PostgreSQL via Testcontainers

## API Docs

- Swagger UI: `http://localhost:8080/swagger-ui.html` (requires backend running)
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`

## Key Settings

- **JWT access token**: 15-min expiry
- **Refresh token**: 7-day expiry, single-use (rotation)
- **CORS**: `http://localhost:5173`, `http://localhost:3000`
- **Uploads**: product images → `uploads/products` (5MB max)

## Project Layout

```
src/main/java/com/example/Ecommerce/
├── AppUser/        # users, roles, password change
├── Cart/          # cart operations
├── Category/      # categories
├── Common/        # security, JWT, refresh tokens, file storage
├── Inventory/     # stock reservation/release
├── Login/         # auth endpoints
├── Order/         # order lifecycle, expiry job, transactional executor
├── Payment/       # Razorpay client, signature verification
└── Product/      # product CRUD, search specs

frontend/
├── src/          # React components and pages
└── package.json
```

## License

Not licensed — all rights reserved.
