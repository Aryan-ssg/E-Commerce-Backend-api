# E-Commerce Backend API

A production-oriented REST API for an e-commerce platform built with **Spring Boot 4.1** (Java 17), **PostgreSQL**, and **React 18 + TypeScript** frontend. Focused on concurrency safety under real-world checkout pressure — not just CRUD.

## Tech Stack

### Backend
- **Spring Boot 4.1** — Spring MVC, Spring Data JPA, Spring Security
- **PostgreSQL** — Hibernate ORM with `ddl-auto` schema management
- **JWT Authentication** — access tokens (15 min) + rotating refresh tokens (7 days)
- **Razorpay** — payment order creation, signature verification, webhook handling
- **Cloudinary** — product image uploads with auto-optimization
- **Lombok** — boilerplate reduction
- **Testcontainers** — integration tests with real PostgreSQL in Docker

### Frontend
- **React 18 + TypeScript**
- **Vite** — fast dev server with API proxy
- **Tailwind CSS 4** — utility-first styling
- **React Router 6** — client-side routing
- **Axios** — HTTP client with interceptor-based token refresh

## Features

### Authentication & Authorization
- Register, login, logout
- JWT access + refresh token rotation with reuse detection
- Role-based access control: `USER` and `ADMIN`
- Reusing a revoked refresh token invalidates the entire token family

### Product Catalog
- Public product listing with search and filter (JPA Specifications)
- Category browsing
- Admin CRUD for products and categories
- Cloudinary-powered image upload with auto-optimization (`q_auto,f_auto,w_800`)

### Shopping Cart
- Add, update quantity, remove items
- Stock-validated — can't add more than available
- Duplicate detection — adding the same product increments quantity

### Orders & Payments
- Razorpay payment flow: create order → client confirm → server verify
- Webhook handler for asynchronous payment confirmation
- Order lifecycle with enforced state machine:

```
PENDING → PAID → PROCESSING → SHIPPED → DELIVERED
  ↓         ↓
CANCELLED CANCELLED
```

- Auto-expiry job: cancels stale `PENDING` orders older than 15 minutes and releases stock
- Admin status management via expandable orders dashboard

### Inventory
- Atomic stock reservation (`UPDATE ... WHERE stock >= quantity`) — no overselling under concurrent checkout
- Stock release on cancellation or expiry
- 20-thread concurrency test (`StockConcurrencyIT`) verifies safety

## Notable Design Decisions

- **No overselling under concurrent checkout** — stock is decremented via a single atomic SQL `UPDATE`, not read-then-write. Verified by a 20-thread Testcontainers integration test.
- **Refresh-token rotation with reuse detection** — each refresh invalidates its predecessor. Reusing a revoked token revokes the entire family.
- **Idempotent payment confirmation** — Razorpay signature is verified on both the client-confirm call and the webhook, so a retried webhook can't double-mark an order as paid.
- **Race-safe order cancellation** — user cancel, admin cancel, and the expiry job all race on the same conditional UPDATE claim. Stock is never double-released.
- **Separate prod profile** — Swagger, show-sql, and SQL init are disabled in production. `ddl-auto=validate` enforces schema correctness.

## Prerequisites

- **Java 17+**
- **Maven 3.9+** (or use the included `mvnw` wrapper)
- **Node.js 18+**
- **PostgreSQL 12+**
- **Docker** — required for integration tests (Testcontainers spins up PostgreSQL)
- **Razorpay test keys** — for payment features
- **Cloudinary account** — for image uploads

## Local Setup

### 1. Create the database

```sql
CREATE DATABASE ecommerce;
```

### 2. Configure environment variables

```bash
cp .env.example .env
```

Edit `.env` with your values:

| Variable | Description |
|---|---|
| `DB_URL` | JDBC URL (e.g. `jdbc:postgresql://localhost:5432/ecommerce`) |
| `DB_USERNAME` | Database user |
| `DB_PASSWORD` | Database password |
| `JWT_SECRET_KEY` | High-entropy string for JWT signing (min 32 chars) |
| `JWT_EXPIRATION_MS` | Access token lifetime (default: 15 min) |
| `JWT_REFRESH_EXPIRATION_MS` | Refresh token lifetime (default: 7 days) |
| `ADMIN_PASSWORD` | Password for the auto-created `admin` user |
| `CLOUDINARY_CLOUD_NAME` | Cloudinary cloud name |
| `CLOUDINARY_API_KEY` | Cloudinary API key |
| `CLOUDINARY_API_SECRET` | Cloudinary API secret |
| `RAZORPAY_KEY_ID` | Razorpay test key |
| `RAZORPAY_KEY_SECRET` | Razorpay test secret |
| `RAZORPAY_WEBHOOK_SECRET` | Razorpay webhook signing secret |
| `APP_CORS_ALLOWED_ORIGINS` | Comma-separated allowed origins (default: `localhost:5173,localhost:3000`) |

### 3. Start the backend

Using the included script (loads `.env` automatically):

```bash
./start.sh
```

Or manually:

```bash
export $(grep -v '^#' .env | xargs)
./mvnw spring-boot:run
```

The API runs at **`http://localhost:8080`**.

On first boot, `DataInitializer` creates an `admin` user (`admin` / your `${ADMIN_PASSWORD}`) if none exists.

### 4. Start the frontend

```bash
cd frontend
npm install
npm run dev
```

Runs at **`http://localhost:5173`**. The Vite dev server proxies all `/api` requests to the backend on port 8080.

## Testing

```bash
./mvnw verify
```

- **114 test methods** across unit, controller, and integration layers
- Unit tests: service logic, validation, DTOs, order expiry, concurrency executor
- Controller tests: MockMvc-based endpoint verification
- Integration tests: full Spring context with real PostgreSQL via Testcontainers
- Includes a **20-thread concurrency test** (`StockConcurrencyIT`) that verifies no overselling
- **Requires Docker** — tests spin up PostgreSQL via Testcontainers

## API Endpoints

### Public (no auth)
| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/api/public/register` | Register a new user |
| `POST` | `/api/public/login` | Login, returns access + refresh tokens |
| `POST` | `/api/public/refresh` | Refresh access token |
| `GET` | `/api/public/health` | Health check (returns `200 OK`) |
| `GET` | `/api/public/products` | List products (search/filter) |
| `GET` | `/api/public/products/{id}` | Get product by ID |
| `GET` | `/api/public/categories` | List categories |

### User (requires `USER` or `ADMIN` role)
| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/api/order/my-orders` | List current user's orders |
| `POST` | `/api/order/place` | Place an order |
| `POST` | `/api/order/{id}/verify-payment` | Confirm payment |
| `PUT` | `/api/order/{id}/changeShippingAddress` | Update shipping address |
| `PUT` | `/api/order/{id}/cancel` | Cancel an order |
| `POST` | `/api/items` | Add item to cart |
| `PUT` | `/api/items/{id}` | Update cart item quantity |
| `DELETE` | `/api/items/{id}` | Remove cart item |
| `POST` | `/api/user/change-password` | Change password |
| `POST` | `/api/verify` | Verify email (placeholder) |

### Admin (requires `ADMIN` role)
| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/api/admin/users` | List users (paginated, filterable) |
| `GET` | `/api/admin/orders` | List all orders (paginated) |
| `GET` | `/api/order/{id}` | Get order details |
| `PUT` | `/api/order/{id}/updateOrderStatus` | Update order status |
| `POST` | `/api/admin/categories` | Create category |
| `PUT` | `/api/admin/categories/{id}` | Update category |
| `DELETE` | `/api/admin/categories/{id}` | Delete category |
| `POST` | `/api/admin/categories/{id}/products` | Create product in category |
| `PUT` | `/api/admin/products/{id}` | Update product |
| `DELETE` | `/api/admin/products/{id}` | Delete product |
| `POST` | `/api/admin/products/{id}/image` | Upload product image |
| `PATCH` | `/api/admin/products/{id}/restock` | Restock product |

### Payment Webhook
| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/api/payment/webhook` | Razorpay webhook (no auth) |

## API Documentation

Swagger UI and OpenAPI docs are available when the backend is running:

- **Swagger UI**: `http://localhost:8080/swagger-ui.html`
- **OpenAPI JSON**: `http://localhost:8080/v3/api-docs`

Disabled in production via `application-prod.properties`.

## Configuration Profiles

| Profile | `ddl-auto` | Swagger | show-sql | SQL init |
|---|---|---|---|---|
| `default` (dev) | `update` | enabled | enabled | `always` |
| `prod` | `update` (first deploy) / `validate` (after) | disabled | disabled | `never` |

## Project Layout

```
src/main/java/com/example/Ecommerce/
├── AppUser/          # user registration, roles, password management
├── Cart/             # cart CRUD, stock validation
├── Category/         # category management
├── Common/           # security config, JWT, refresh tokens, Cloudinary, health check
├── Inventory/        # atomic stock reservation and release
├── Login/            # authentication endpoints
├── Order/            # order lifecycle, expiry job, transactional executor
├── Payment/          # Razorpay integration, signature verification, webhooks
└── Product/          # product CRUD, image upload, search specifications

frontend/
├── src/
│   ├── api/          # axios client, admin API functions
│   ├── auth/         # AuthContext, token storage
│   ├── components/   # Navbar, ProductCard, PrivateRoute, Toast
│   ├── pages/        # Catalog, Cart, Checkout, Orders, Login, Register
│   └── pages/admin/  # Admin dashboard: Users, Categories, Products, Orders
└── package.json
```

## Deployment (Render)

### Backend
1. Create a managed PostgreSQL instance on Render
2. Create a Web Service connected to this repo
3. Set environment variables (see table above)
4. Set Health Check Path to `/api/public/health`
5. Deploy — Hibernate creates tables on first boot (`ddl-auto=update`)

### Frontend
Coming soon.

## License

Not licensed — all rights reserved.
