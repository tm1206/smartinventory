# SmartInventory — Complete Project Documentation

> Everything needed to understand, explain, rebuild, extend, deploy, and demo this project.

---

## Table of Contents

1. [Project Overview](#1-project-overview)
2. [Architecture](#2-architecture)
3. [AWS Setup](#3-aws-setup)
4. [All 47 Java Files](#4-all-47-java-files)
5. [All 25 API Endpoints](#5-all-25-api-endpoints)
6. [Database Schema](#6-database-schema)
7. [pom.xml Dependencies](#7-pomxml-dependencies)
8. [Config Files](#8-config-files)
9. [Railway Deployment](#9-railway-deployment)
10. [Spring Boot Concepts](#10-spring-boot-concepts)
11. [Interview Preparation](#11-interview-preparation)
12. [Frontend Guide](#12-frontend-guide)
13. [Troubleshooting](#13-troubleshooting)
14. [Future Improvements](#14-future-improvements)

---

## 1. Project Overview

### What It Is

SmartInventory is a production-grade REST API backend for managing business inventory. It handles products, orders, users, and reporting — all backed by a PostgreSQL database on Railway and integrated with four AWS services.

### Why It Was Built

Built as a portfolio project to demonstrate:
- Real-world Spring Boot 3.5 backend architecture
- JWT-based authentication with role-based access control
- AWS cloud services integration (S3, SQS, SES, CloudWatch)
- Production deployment on Railway with PostgreSQL
- Clean REST API design consumable by any frontend

### Live URLs

| Resource | URL |
|---|---|
| API Base | `https://smartinventory-production-2890.up.railway.app` |
| Swagger UI | `https://smartinventory-production-2890.up.railway.app/swagger-ui/index.html` |
| API Docs (JSON) | `https://smartinventory-production-2890.up.railway.app/v3/api-docs` |
| Health Check | `https://smartinventory-production-2890.up.railway.app/actuator/health` |
| GitHub | `https://github.com/tm1206/smartinventory` |

### Tech Stack

| Layer | Technology | Version |
|---|---|---|
| Language | Java | 21 |
| Framework | Spring Boot | 3.5.0 |
| Security | Spring Security | 6.5.0 |
| ORM | Spring Data JPA / Hibernate | 6.6.x |
| Auth | JJWT | 0.12.5 |
| API Docs | springdoc-openapi | 2.8.6 |
| AWS SDK | Spring Cloud AWS | 3.2.0 |
| AWS CloudWatch | AWS SDK v2 | (managed by Spring Cloud AWS BOM) |
| Database (local) | H2 (in-memory) | — |
| Database (prod) | PostgreSQL | 18.4 (Railway) |
| Build Tool | Maven | 3.9 |
| Deployment | Railway | — |
| Container | Docker (multi-stage) | — |
| Code Generation | Lombok | — |

---

## 2. Architecture

### ASCII Architecture Diagram

```
                          ┌─────────────────────────────────────────────┐
                          │              CLIENTS                         │
                          │  React App / Mobile / curl / Postman         │
                          └──────────────────┬──────────────────────────┘
                                             │ HTTPS
                                             ▼
                          ┌─────────────────────────────────────────────┐
                          │            RAILWAY CLOUD                     │
                          │                                              │
                          │  ┌──────────────────────────────────────┐   │
                          │  │      Spring Boot App (Port 8080)      │   │
                          │  │                                        │   │
                          │  │  ┌─────────┐  ┌──────────────────┐   │   │
                          │  │  │  CORS   │  │   JwtFilter       │   │   │
                          │  │  │ Filter  │  │ (OncePerRequest)  │   │   │
                          │  │  └────┬────┘  └────────┬─────────┘   │   │
                          │  │       │                 │              │   │
                          │  │  ┌────▼─────────────────▼──────────┐ │   │
                          │  │  │         Spring Security           │ │   │
                          │  │  │  (Role-based, Stateless JWT)     │ │   │
                          │  │  └────────────────┬────────────────┘ │   │
                          │  │                   │                   │   │
                          │  │  ┌────────────────▼──────────────┐   │   │
                          │  │  │           Controllers          │   │   │
                          │  │  │  Auth | Product | Order |      │   │   │
                          │  │  │  Report | Admin               │   │   │
                          │  │  └────────────────┬──────────────┘   │   │
                          │  │                   │                   │   │
                          │  │  ┌────────────────▼──────────────┐   │   │
                          │  │  │            Services            │   │   │
                          │  │  │  Auth | Product | Order |      │   │   │
                          │  │  │  S3 | SQS | SES | CW | Audit  │   │   │
                          │  │  └──┬──────────────────────────┬─┘   │   │
                          │  │     │                          │      │   │
                          │  │  ┌──▼──────────┐   ┌──────────▼───┐  │   │
                          │  │  │ Repositories│   │  AWS Clients │  │   │
                          │  │  │  (JPA)      │   │  (SDK v2)    │  │   │
                          │  │  └──────┬──────┘   └──────────────┘  │   │
                          │  │         │                              │   │
                          │  └─────────┼──────────────────────────── ┘   │
                          │            │                                  │
                          │  ┌─────────▼──────────┐                      │
                          │  │  PostgreSQL 18.4    │                      │
                          │  │  (Railway Managed)  │                      │
                          │  └─────────────────────┘                      │
                          └──────────────────┬──────────────────────────┘
                                             │
                    ┌────────────────────────┼───────────────────────────┐
                    │         AWS (ap-south-1)│                           │
                    │                         │                           │
              ┌─────▼──────┐  ┌──────────────▼──┐  ┌────────┐  ┌──────┐│
              │  S3 Bucket │  │   SQS Queues      │  │  SES   │  │  CW ││
              │  (images + │  │  order-events     │  │ Emails │  │Metrics
              │  reports)  │  │  stock-alerts     │  │        │  │      ││
              └────────────┘  └─────────────────-─┘  └────────┘  └──────┘│
                    └────────────────────────────────────────────────────┘
```

### Component Connections

```
HTTP Request
    │
    ├─► CorsFilter          → adds Access-Control-* headers to all responses
    ├─► JwtFilter           → extracts Bearer token, validates, sets SecurityContext
    ├─► SecurityFilterChain → checks role vs endpoint rules
    │
    ├─► AuthController      → POST /api/auth/**
    │       └─► AuthService → UserRepository, JwtUtil, SESService, CloudWatchService
    │
    ├─► ProductController   → /api/products/**
    │       └─► ProductService → ProductRepository, S3Service, SQSService, AuditLogService
    │
    ├─► OrderController     → /api/orders/**
    │       └─► OrderService → OrderRepository, ProductService, SQSService, SESService, AuditLogService
    │
    ├─► ReportController    → /api/reports/**
    │       └─► ReportService → ProductRepository, OrderRepository, S3Service
    │
    └─► AdminController     → /api/admin/**
            └─► UserRepository, AuditLogService
```

### Request Data Flow

```
1. Browser sends:  POST /api/orders
                   Authorization: Bearer eyJhbGci...
                   Content-Type: application/json
                   Body: { "items": [...] }

2. CorsFilter:     Adds Access-Control-Allow-Origin: * to response headers

3. JwtFilter:      Parses token → extracts username "johndoe"
                   Loads UserDetails from DB
                   Validates token signature + expiry
                   Sets SecurityContext authentication

4. SecurityConfig: Checks: anyRequest().authenticated() → pass (has auth)
                   @PreAuthorize on method → pass (has ADMIN/MANAGER/STAFF)

5. OrderController: Calls orderService.placeOrder(request, "johndoe")

6. OrderService:    Looks up User from UserRepository
                    For each item:
                        → productService.deductStock() → ProductRepository.save()
                    Builds Order + OrderItems
                    → orderRepository.save()
                    → sqsService.publishOrderEvent() → SQS order-events queue
                    → sesService.sendOrderConfirmation() → SES email
                    → auditLogService.log() [async] → AuditLogRepository.save()

7. Response:        201 Created
                    { "success": true, "data": { <OrderResponse> } }
```

---

## 3. AWS Setup

### IAM User

**User name:** `smartinventory-user`
**Account ID:** `454681118572`
**Region:** `ap-south-1` (Mumbai)

The IAM user has **no console access** — programmatic access only (access key + secret key).

#### Policies Attached

```
SmartInventoryS3Access:
  - s3:PutObject
  - s3:GetObject
  - s3:DeleteObject
  - s3:ListBucket
  Resource: arn:aws:s3:::smartinventory-uploads
            arn:aws:s3:::smartinventory-uploads/*

SmartInventorySQSAccess:
  - sqs:SendMessage
  - sqs:ReceiveMessage
  - sqs:DeleteMessage
  - sqs:GetQueueAttributes
  Resource: arn:aws:sqs:ap-south-1:454681118572:order-events
            arn:aws:sqs:ap-south-1:454681118572:stock-alerts

SmartInventorySESAccess:
  - ses:SendEmail
  - ses:SendRawEmail
  Resource: *

SmartInventoryCloudWatchAccess:
  - cloudwatch:PutMetricData
  Resource: *
```

#### Step-by-Step IAM Setup

1. AWS Console → IAM → Users → Create user
2. User name: `smartinventory-user`
3. Access type: **Programmatic access only**
4. Skip adding to groups
5. Create user → **Download credentials CSV** (only chance)
6. Create each policy above: IAM → Policies → Create policy → JSON tab
7. Attach policies: IAM → Users → smartinventory-user → Add permissions → Attach policies

### S3 Bucket Setup

1. AWS Console → S3 → Create bucket
2. Bucket name: `smartinventory-uploads`
3. Region: `ap-south-1`
4. Block all public access: **ON** (files accessed via pre-signed URLs)
5. Versioning: disabled
6. No bucket policy needed (IAM user policy handles access)

**Folder structure inside bucket:**
```
smartinventory-uploads/
├── products/          ← product images (UUID filenames)
└── reports/           ← exported CSV files
```

### SQS Queues Setup

1. AWS Console → SQS → Create queue
2. Type: **Standard queue** (not FIFO)
3. Queue 1: `order-events`
   - Visibility timeout: 30 seconds
   - Message retention: 4 days
4. Queue 2: `stock-alerts`
   - Same settings
5. Copy both queue URLs for env variables

**Queue URLs:**
```
https://sqs.ap-south-1.amazonaws.com/454681118572/order-events
https://sqs.ap-south-1.amazonaws.com/454681118572/stock-alerts
```

### SES Email Setup

SES operates in **sandbox mode** by default — only verified email addresses can receive mail.

1. AWS Console → SES → Verified identities → Create identity
2. Identity type: **Email address**
3. Enter: `taranghq@gmail.com` (or your email)
4. Click verification link in the email
5. Status must show **Verified**

**SMTP Credentials** (for JavaMailSender):
1. SES → SMTP settings → Create SMTP credentials
2. This creates a new IAM user specifically for SMTP
3. Download credentials (username/password for SMTP, NOT the IAM access key)

**To exit sandbox:** Submit a production access request via SES console (allows sending to unverified addresses).

### CloudWatch Setup

No manual setup needed. The IAM policy `cloudwatch:PutMetricData` is sufficient. The app creates the namespace and metrics automatically on first write.

**Namespace:** `smartinventory-logs` (set via `CLOUDWATCH_LOG_GROUP` env var)

**Metrics written:**
- `LoginSuccess` — incremented on every successful login
- `LoginFailure` — incremented on every failed login attempt
- `ApiRequest` — incremented on API calls (when manually invoked)
- `ApplicationException` — incremented on unhandled exceptions

**To view in console:** CloudWatch → Metrics → Custom namespaces → `smartinventory-logs`

### All Environment Variables

| Variable | Description | Example |
|---|---|---|
| `AWS_ACCESS_KEY` | IAM user access key ID | `AKIA...` |
| `AWS_SECRET_KEY` | IAM user secret access key | `wJalr...` |
| `AWS_REGION` | AWS region | `ap-south-1` |
| `S3_BUCKET_NAME` | S3 bucket name | `smartinventory-uploads` |
| `SQS_ORDER_QUEUE_URL` | Full SQS order-events URL | `https://sqs.ap-south-1...` |
| `SQS_STOCK_QUEUE_URL` | Full SQS stock-alerts URL | `https://sqs.ap-south-1...` |
| `AWS_SQS_ENABLED` | Enable SQS listeners | `true` or `false` |
| `SES_FROM_EMAIL` | Sender email (must be verified) | `noreply@yourdomain.com` |
| `SES_SMTP_USERNAME` | SES SMTP username | `AKIA...` (different from AWS key) |
| `SES_SMTP_PASSWORD` | SES SMTP password | `BNfK...` |
| `CLOUDWATCH_LOG_GROUP` | CloudWatch namespace | `smartinventory-logs` |
| `JWT_SECRET` | JWT signing secret (min 64 chars) | `your-very-long-secret-key-...` |
| `SPRING_PROFILES_ACTIVE` | Active profile | `prod` |
| `PGHOST` | PostgreSQL host (Railway auto-sets) | `postgres.railway.internal` |
| `PGPORT` | PostgreSQL port | `5432` |
| `PGDATABASE` | Database name | `railway` |
| `PGUSER` | Database user | `postgres` |
| `PGPASSWORD` | Database password | `...` |

---

## 4. All 47 Java Files

### Entry Point

#### `SmartInventoryApplication.java`
**Package:** `com.smartinventory`
**Purpose:** Spring Boot entry point. `@EnableScheduling` enables `@Async` and scheduled tasks.
```java
@SpringBootApplication
@EnableScheduling
public class SmartInventoryApplication {
    public static void main(String[] args) { SpringApplication.run(...); }
}
```

---

### config/

#### `AWSConfig.java`
**Package:** `com.smartinventory.config`
**Purpose:** Manually creates all AWS SDK clients (CloudWatchClient, SqsAsyncClient, S3Client) using credentials from properties. Falls back to `AnonymousCredentialsProvider` if credentials are blank (for local dev).

**Key methods:**
- `credentialsProvider()` — returns `StaticCredentialsProvider` if keys exist, else `AnonymousCredentialsProvider`
- `cloudWatchClient()` — `@Bean` CloudWatch SDK v2 client
- `sqsAsyncClient()` — `@Bean @Primary` async SQS client
- `sqsTemplate()` — `@Bean` Spring Cloud AWS SQS template (wraps async client)
- `defaultSqsListenerContainerFactory()` — configures SQS listener containers, auto-starts only if credentials present
- `s3Client()` — `@Bean @Primary` S3 SDK client

#### `JpaConfig.java`
**Package:** `com.smartinventory.config`
**Purpose:** Enables JPA auditing (`@CreatedDate`, `@LastModifiedDate`) and specifies repository scan package.
```java
@Configuration
@EnableJpaAuditing
@EnableJpaRepositories(basePackages = "com.smartinventory.repository")
```

#### `OpenApiConfig.java`
**Package:** `com.smartinventory.config`
**Purpose:** Configures Swagger UI with API title, version, and the `bearerAuth` JWT security scheme so that the "Authorize" button in Swagger UI works.

---

### controller/

#### `AuthController.java`
**Package:** `com.smartinventory.controller`
**Base path:** `/api/auth`
**Auth required:** None (all public)
**Purpose:** Handles user registration, login, token refresh, and logout.

| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/register` | Create account + return tokens |
| `POST` | `/login` | Authenticate + return tokens |
| `POST` | `/refresh` | Exchange refresh token for new pair |
| `POST` | `/logout` | Invalidate refresh token server-side |

#### `ProductController.java`
**Package:** `com.smartinventory.controller`
**Base path:** `/api/products`
**Auth required:** Yes (all endpoints)
**Purpose:** Full CRUD for products plus image upload.

| Method | Endpoint | Role |
|---|---|---|
| `POST` | `/` | ADMIN, MANAGER |
| `GET` | `/` | Any |
| `GET` | `/{id}` | Any |
| `GET` | `/sku/{sku}` | Any |
| `PUT` | `/{id}` | ADMIN, MANAGER |
| `POST` | `/{id}/image` | ADMIN, MANAGER |
| `DELETE` | `/{id}` | ADMIN only |
| `GET` | `/categories` | Any |
| `GET` | `/low-stock` | ADMIN, MANAGER |

#### `OrderController.java`
**Package:** `com.smartinventory.controller`
**Base path:** `/api/orders`
**Auth required:** Yes (all endpoints)

| Method | Endpoint | Role |
|---|---|---|
| `POST` | `/` | Any authenticated |
| `GET` | `/` | ADMIN, MANAGER |
| `GET` | `/my-orders` | Any authenticated |
| `GET` | `/{id}` | Any authenticated |
| `GET` | `/number/{orderNumber}` | Any authenticated |
| `PUT` | `/{id}/status` | ADMIN, MANAGER |

#### `ReportController.java`
**Package:** `com.smartinventory.controller`
**Base path:** `/api/reports`
**Auth required:** Yes — ADMIN or MANAGER

| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/inventory` | Inventory summary report |
| `GET` | `/orders` | Orders report by date range |
| `GET` | `/inventory/export` | Export inventory CSV → S3 → presigned URL |
| `GET` | `/orders/export` | Export orders CSV → S3 → presigned URL |

#### `AdminController.java`
**Package:** `com.smartinventory.controller`
**Base path:** `/api/admin`
**Auth required:** Yes — ADMIN only (set at class level with `@PreAuthorize("hasRole('ADMIN')")`)

| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/users` | List all users |
| `GET` | `/users/{id}` | Get user by ID |
| `PUT` | `/users/{id}/role` | Change user role |
| `PUT` | `/users/{id}/status` | Activate/deactivate user |
| `GET` | `/audit-logs` | Paginated audit log |

---

### dto/

#### `ApiResponse.java`
**Package:** `com.smartinventory.dto`
**Purpose:** Universal response wrapper. `@JsonInclude(NON_NULL)` omits null fields. All controllers return `ResponseEntity<ApiResponse<T>>`.

Fields: `success` (boolean), `message` (String), `data` (T), `errors` (Object), `timestamp` (LocalDateTime)

Static factories: `ApiResponse.success(data)`, `ApiResponse.success(message, data)`, `ApiResponse.error(message)`, `ApiResponse.error(message, errors)`

#### `AuthResponse.java`
**Purpose:** JWT token pair + user info. Returned by register, login, refresh.

Fields: `accessToken`, `refreshToken`, `tokenType` ("Bearer"), `expiresIn` (ms), `username`, `email`, `role`

#### `LoginRequest.java`
**Purpose:** Login credentials input.
Fields: `username` (required), `password` (required)

#### `RegisterRequest.java`
**Purpose:** New user registration input.
Fields: `username` (3-50 chars, required), `email` (valid email, required), `password` (min 6, required), `fullName` (required), `role` (optional string — defaults to STAFF if invalid)

#### `RefreshTokenRequest.java`
**Purpose:** Token refresh input.
Fields: `refreshToken` (required)

#### `ProductRequest.java`
**Purpose:** Create/update product input.
Fields: `name` (required), `sku` (required, unique), `category` (required), `quantity` (integer ≥ 0, required), `price` (decimal > 0, required), `imageUrl` (optional), `description` (optional)

#### `ProductResponse.java`
**Purpose:** Product read model returned to clients.
Fields: `id`, `name`, `sku`, `category`, `quantity`, `price`, `imageUrl`, `description`, `lowStock` (boolean, computed: quantity < 10), `createdAt`, `updatedAt`

#### `OrderRequest.java`
**Purpose:** Place order input. Contains nested `OrderItemRequest`.
Fields: `items` (List, at least 1, required), `shippingAddress` (optional), `notes` (optional)
Nested `OrderItemRequest`: `productId` (required), `quantity` (required)

#### `OrderResponse.java`
**Purpose:** Order read model returned to clients. Contains nested `OrderItemResponse`.
Fields: `id`, `orderNumber`, `username`, `items` (List), `status`, `totalAmount`, `shippingAddress`, `notes`, `createdAt`, `updatedAt`
Nested `OrderItemResponse`: `id`, `productId`, `productName`, `productSku`, `quantity`, `unitPrice`, `totalPrice`

#### `UserResponse.java`
**Purpose:** User read model (excludes password and refresh token).
Fields: `id`, `username`, `email`, `fullName`, `role`, `active`, `createdAt`

---

### event/

#### `OrderStatusListener.java`
**Package:** `com.smartinventory.event`
**Purpose:** Listens to the `order-events` SQS queue. When an order event arrives, it calls `orderService.updateOrderStatus()` to apply the status from the message.
**Conditional:** `@ConditionalOnProperty(name = "aws.sqs.enabled", havingValue = "true")` — only created in Spring context when SQS is enabled.

#### `StockAlertListener.java`
**Package:** `com.smartinventory.event`
**Purpose:** Listens to `stock-alerts` SQS queue. When a low-stock alert arrives, sends a low-stock email via `SESService`.
**Conditional:** Same `@ConditionalOnProperty` as above.

---

### exception/

#### `GlobalExceptionHandler.java`
**Package:** `com.smartinventory.exception`
**Purpose:** `@RestControllerAdvice` — catches all exceptions and converts them to `ApiResponse` error responses with the correct HTTP status.

| Exception | HTTP Status | Message |
|---|---|---|
| `ResourceNotFoundException` | 404 | Resource not found |
| `UnauthorizedException` | 401 | Unauthorized message |
| `InsufficientStockException` | 409 | Stock shortage message |
| `BadCredentialsException` | 401 | "Invalid username or password" |
| `AccessDeniedException` | 403 | "Access denied: insufficient permissions" |
| `MethodArgumentNotValidException` | 400 | "Validation failed" + field errors map |
| `MaxUploadSizeExceededException` | 413 | "File size exceeds maximum" |
| `DataIntegrityViolationException` | 409 | FK constraint message |
| `IllegalArgumentException` | 400 | The exception message |
| `Exception` (catch-all) | 500 | "An unexpected error occurred: ..." |

#### `ResourceNotFoundException.java`
**Purpose:** Thrown when a requested entity doesn't exist. Two constructors: free-form message or `(resource, field, value)` which produces `"Product not found with id: '42'"`.

#### `InsufficientStockException.java`
**Purpose:** Thrown when an order requests more stock than available. Constructor `(productName, requested, available)` produces descriptive message.

#### `UnauthorizedException.java`
**Purpose:** Thrown for auth-related logic errors (invalid refresh token, etc.). Maps to 401.

---

### model/

#### `User.java`
**Package:** `com.smartinventory.model`
**Table:** `users`
**Purpose:** JPA entity for system users. Uses `@EntityListeners(AuditingEntityListener.class)` for auto-populated timestamps.

Fields: `id`, `username` (unique), `email` (unique), `password` (bcrypt), `fullName`, `role` (enum), `refreshToken`, `active` (default true), `createdAt`, `updatedAt`

Enum `Role`: `ADMIN`, `MANAGER`, `STAFF`

#### `Product.java`
**Package:** `com.smartinventory.model`
**Table:** `products`
**Purpose:** JPA entity for inventory products.

Fields: `id`, `name`, `sku` (unique), `category`, `quantity`, `price` (BigDecimal, scale 2), `imageUrl`, `description`, `createdAt`, `updatedAt`

#### `Order.java`
**Package:** `com.smartinventory.model`
**Table:** `orders`
**Purpose:** JPA entity for customer orders. Has one-to-many relationship with `OrderItem` (cascades ALL, orphanRemoval).

Fields: `id`, `orderNumber` (unique), `user` (ManyToOne → users), `items` (OneToMany → order_items), `status` (enum, default PENDING), `totalAmount`, `shippingAddress`, `notes`, `createdAt`, `updatedAt`

Enum `OrderStatus`: `PENDING`, `PROCESSING`, `COMPLETED`, `CANCELLED`

#### `OrderItem.java`
**Package:** `com.smartinventory.model`
**Table:** `order_items`
**Purpose:** Junction entity linking orders to products, with price snapshot at time of order.

Fields: `id`, `order` (ManyToOne → orders), `product` (ManyToOne → products), `quantity`, `unitPrice` (snapshot), `totalPrice` (snapshot)

#### `AuditLog.java`
**Package:** `com.smartinventory.model`
**Table:** `audit_logs`
**Purpose:** Immutable record of every significant action in the system.

Fields: `id`, `username`, `actionType`, `entityType`, `entityId`, `details`, `ipAddress`, `timestamp`

---

### repository/

#### `UserRepository.java`
**Purpose:** JPA repository for User.
**Custom methods:** `findByUsername`, `findByEmail`, `findByRefreshToken`, `existsByUsername`, `existsByEmail`

#### `ProductRepository.java`
**Purpose:** JPA repository for Product.
**Custom methods:** `findBySku`, `existsBySku`, `findByCategory`, `findByQuantityLessThan`
**JPQL queries:**
- `searchProducts(@Param name, @Param category, Pageable)` — case-insensitive name LIKE with empty-string guard (avoids PostgreSQL bytea null inference bug)
- `findAllCategories()` — `SELECT DISTINCT p.category FROM Product p ORDER BY p.category`

#### `OrderRepository.java`
**Purpose:** JPA repository for Order.
**Custom methods:** `findByOrderNumber`, `findByUserId` (pageable), `findByStatus`
**JPQL queries:**
- `findByDateRange(start, end)` — orders within a LocalDateTime range
- `findByUserIdAndStatus(userId, status)` — filtered user orders

#### `AuditLogRepository.java`
**Purpose:** JPA repository for AuditLog.
**Custom methods:** `findByUsername`, `findByActionType`, `findByTimestampBetween` (pageable)

---

### security/

#### `JwtUtil.java`
**Package:** `com.smartinventory.security`
**Purpose:** JWT creation and validation. Uses JJWT 0.12.5 with HS512 algorithm.

**Key methods:**
- `generateAccessToken(UserDetails)` — creates token with `type=access`, 24h TTL
- `generateRefreshToken(UserDetails)` — creates token with `type=refresh`, 7d TTL
- `extractUsername(token)` — reads subject claim
- `isTokenValid(token, UserDetails)` — checks username match + not expired
- `isTokenExpired(token)` — checks expiration claim
- `getSigningKey()` — tries Base64 decode of secret first, falls back to raw bytes, pads to 64 bytes minimum for HS512

#### `JwtFilter.java`
**Package:** `com.smartinventory.security`
**Purpose:** `OncePerRequestFilter` that intercepts every request. Extracts the Bearer token, validates it, and sets the `SecurityContextHolder` authentication — making the user available throughout the request lifecycle.

**Flow:**
1. Read `Authorization` header
2. If missing or not `Bearer `, pass through
3. Extract username from token
4. Load `UserDetails` from DB
5. Validate token (signature + expiry + username match)
6. Set `UsernamePasswordAuthenticationToken` in `SecurityContextHolder`
7. Continue filter chain

#### `SecurityConfig.java`
**Package:** `com.smartinventory.security`
**Purpose:** Spring Security configuration. Stateless session, JWT filter, CORS, and URL-level access rules.

Key configurations:
- CSRF disabled (stateless JWT needs no CSRF)
- Session: `STATELESS` (no server-side sessions)
- CORS: `corsConfigurationSource()` bean, allows all origins with credentials
- Public paths: `/api/auth/**`, `/v3/api-docs/**`, `/swagger-ui/**`, `/actuator/health`
- `/api/admin/**` → ADMIN only
- `/api/reports/**` → ADMIN or MANAGER
- All other requests → any authenticated user
- Method-level security enabled via `@EnableMethodSecurity`

#### `UserDetailsServiceImpl.java`
**Package:** `com.smartinventory.security`
**Purpose:** Implements `UserDetailsService` to load users from the database for Spring Security. Converts `User.Role` to `ROLE_ADMIN` / `ROLE_MANAGER` / `ROLE_STAFF` granted authority strings. Respects `isActive` flag (locked accounts cannot authenticate).

---

### service/

#### `AuthService.java`
**Purpose:** Handles registration, login, token refresh, and logout.

**Key logic:**
- `register()` — validates uniqueness, encodes password with BCrypt, creates user, generates both tokens, sends welcome email (SES), writes audit log
- `login()` — authenticates via `AuthenticationManager`, generates new tokens, rotates refresh token in DB, writes audit log, sends CloudWatch metric
- `refreshToken()` — finds user by refresh token, checks expiry, issues new pair (rotation)
- `logout()` — sets `refreshToken = null` in DB

#### `ProductService.java`
**Purpose:** Product CRUD, image upload, stock management.

**Key logic:**
- `createProduct()` — checks SKU uniqueness, saves, publishes SQS stock alert if low stock
- `getAllProducts()` — converts null search params to empty string (PostgreSQL bytea bug workaround), calls `searchProducts` JPQL
- `deleteProduct()` — catches `DataIntegrityViolationException` from FK constraint (order_items → products) and throws user-friendly `IllegalArgumentException`
- `deductStock()` — called by OrderService; throws `InsufficientStockException` if insufficient quantity
- `uploadProductImage()` — delegates to `S3Service.uploadFile()`, updates product.imageUrl
- `getLowStockProducts()` — returns products with quantity < 10

#### `OrderService.java`
**Purpose:** Order lifecycle management.

**Key logic:**
- `placeOrder()` — creates order with unique `ORD-{UUID}` number, iterates items deducting stock for each, calculates total, saves order + items in one transaction, publishes to SQS `order-events`, sends SES confirmation email, writes audit log
- `updateOrderStatus()` — updates status field, saves, writes audit log
- `getUserOrders()` — fetches orders by user's DB id
- `getOrdersByDateRange()` — used by ReportService

#### `S3Service.java`
**Purpose:** AWS S3 file operations.

**Key methods:**
- `uploadFile(MultipartFile, folder)` — generates UUID filename, uploads via `S3Template`, returns public URL `https://{bucket}.s3.{region}.amazonaws.com/{key}`
- `uploadCsvReport(csvContent, filename)` — uploads CSV bytes, returns 1-hour pre-signed URL
- `generatePresignedUrl(key, Duration)` — creates time-limited download URL via `S3Presigner`
- `deleteFile(fileUrl)` — extracts key from URL and deletes from S3

#### `SESService.java`
**Purpose:** AWS SES email sending via JavaMailSender (SMTP).

**Email types:**
- `sendWelcomeEmail(toEmail, username)` — sent on registration
- `sendOrderConfirmationEmail(toEmail, orderNumber, totalAmount)` — sent on order placement
- `sendLowStockAlertEmail(toEmail, productName, currentStock)` — sent by StockAlertListener
- `sendPasswordResetEmail(toEmail, resetToken)` — available but not wired to an endpoint

All emails are HTML. Errors are caught and logged (non-fatal).

#### `SQSService.java`
**Purpose:** Publishes messages to AWS SQS queues.

**Key methods:**
- `publishOrderEvent(Map<String,Object>)` — serializes to JSON, sends to `order-events` queue. Payload: `{ orderId, orderNumber, userId, username, totalAmount, status, timestamp }`
- `publishStockAlert(Map<String,Object>)` — serializes to JSON, sends to `stock-alerts` queue. Payload: `{ productId, productName, sku, currentQuantity, threshold }`

#### `CloudWatchService.java`
**Purpose:** Publishes custom metrics to AWS CloudWatch.

**Key methods:**
- `logLoginAttempt(username, success)` — increments `LoginSuccess` or `LoginFailure` metric
- `logApiRequest(endpoint, username, httpStatus)` — increments `ApiRequest` metric
- `logException(exceptionType, endpoint, message)` — increments `ApplicationException` metric

**Namespace:** Value of `cloudwatch.log-group` property (`smartinventory-logs` in prod)

#### `AuditLogService.java`
**Purpose:** Async audit trail persistence.

**Key methods:**
- `log(username, actionType, entityType, entityId, details, ipAddress)` — `@Async`, saves `AuditLog` entity. Called from AuthService, ProductService, OrderService, AdminController.
- `getAuditLogs(Pageable)` — paginated retrieval
- `getAuditLogsByDateRange(start, end, Pageable)` — date-filtered retrieval

**Action types in use:** `USER_REGISTERED`, `USER_LOGIN`, `USER_LOGOUT`, `PRODUCT_CREATED`, `PRODUCT_UPDATED`, `PRODUCT_DELETED`, `PRODUCT_IMAGE_UPLOADED`, `ORDER_PLACED`, `ORDER_STATUS_UPDATED`

#### `ReportService.java`
**Purpose:** Business intelligence reports and CSV exports.

**Key methods:**
- `getInventorySummary()` — returns Map with totalProducts, lowStockCount, totalInventoryValue, categories list, lowStockProducts list
- `getOrdersReport(start, end)` — returns Map with totalOrders, totalRevenue, counts by status, dateRange
- `exportInventoryCsv()` — builds CSV string, uploads to S3 `reports/` folder, returns 1-hour pre-signed URL
- `exportOrdersCsv(start, end)` — same for orders in date range

---

## 5. All 25 API Endpoints

**Base URL:** `https://smartinventory-production-2890.up.railway.app`

### Auth Endpoints (Public)

---

#### POST /api/auth/register

```bash
curl -X POST https://smartinventory-production-2890.up.railway.app/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "johndoe",
    "email": "john@example.com",
    "password": "secret123",
    "fullName": "John Doe",
    "role": "ADMIN"
  }'
```

**Request:**
```json
{
  "username": "johndoe",
  "email": "john@example.com",
  "password": "secret123",
  "fullName": "John Doe",
  "role": "ADMIN"
}
```

**Response 201:**
```json
{
  "success": true,
  "message": "User registered successfully",
  "data": {
    "accessToken": "eyJhbGci...",
    "refreshToken": "eyJhbGci...",
    "tokenType": "Bearer",
    "expiresIn": 86400000,
    "username": "johndoe",
    "email": "john@example.com",
    "role": "ADMIN"
  },
  "timestamp": "2026-06-07T10:00:00"
}
```

---

#### POST /api/auth/login

```bash
curl -X POST https://smartinventory-production-2890.up.railway.app/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username": "johndoe", "password": "secret123"}'
```

**Request:** `{ "username": "johndoe", "password": "secret123" }`

**Response 200:** Same structure as register `data`.

**Error 401:** `{ "success": false, "message": "Invalid username or password" }`

---

#### POST /api/auth/refresh

```bash
curl -X POST https://smartinventory-production-2890.up.railway.app/api/auth/refresh \
  -H "Content-Type: application/json" \
  -d '{"refreshToken": "eyJhbGci..."}'
```

**Response 200:** Same structure as register `data` (new token pair).

---

#### POST /api/auth/logout

```bash
curl -X POST https://smartinventory-production-2890.up.railway.app/api/auth/logout \
  -H "Authorization: Bearer eyJhbGci..."
```

**Response 200:** `{ "success": true, "message": "Logged out successfully" }`

---

### Product Endpoints

---

#### GET /api/products

```bash
curl "https://smartinventory-production-2890.up.railway.app/api/products?name=laptop&category=Electronics&page=0&size=10&sort=price,desc" \
  -H "Authorization: Bearer eyJhbGci..."
```

**Auth:** Yes | **Role:** Any

**Query params:** `name` (partial match), `category` (exact), `page`, `size`, `sort`

**Response 200:**
```json
{
  "success": true,
  "data": {
    "content": [
      {
        "id": 1,
        "name": "Laptop Pro X",
        "sku": "LAP-001",
        "category": "Electronics",
        "quantity": 25,
        "price": 1299.99,
        "imageUrl": null,
        "description": "High-performance laptop",
        "lowStock": false,
        "createdAt": "2026-06-07T10:00:00",
        "updatedAt": "2026-06-07T10:00:00"
      }
    ],
    "totalElements": 1,
    "totalPages": 1,
    "size": 10,
    "number": 0,
    "first": true,
    "last": true
  }
}
```

---

#### GET /api/products/{id}

```bash
curl https://smartinventory-production-2890.up.railway.app/api/products/1 \
  -H "Authorization: Bearer eyJhbGci..."
```

**Auth:** Yes | **Role:** Any

**Response 200:** `{ "success": true, "data": { <Product> } }`

**Error 404:** `{ "success": false, "message": "Product not found with id: '99'" }`

---

#### GET /api/products/sku/{sku}

```bash
curl https://smartinventory-production-2890.up.railway.app/api/products/sku/LAP-001 \
  -H "Authorization: Bearer eyJhbGci..."
```

**Auth:** Yes | **Role:** Any | **Response 200:** `{ "success": true, "data": { <Product> } }`

---

#### GET /api/products/categories

```bash
curl https://smartinventory-production-2890.up.railway.app/api/products/categories \
  -H "Authorization: Bearer eyJhbGci..."
```

**Auth:** Yes | **Role:** Any

**Response 200:** `{ "success": true, "data": ["Electronics", "Furniture", "Office Supplies"] }`

---

#### GET /api/products/low-stock

```bash
curl https://smartinventory-production-2890.up.railway.app/api/products/low-stock \
  -H "Authorization: Bearer eyJhbGci..."
```

**Auth:** Yes | **Role:** ADMIN, MANAGER

**Response 200:** `{ "success": true, "data": [ { <Product> } ] }` (only products with quantity < 10)

---

#### POST /api/products

```bash
curl -X POST https://smartinventory-production-2890.up.railway.app/api/products \
  -H "Authorization: Bearer eyJhbGci..." \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Wireless Mouse",
    "sku": "MOUSE-001",
    "category": "Electronics",
    "quantity": 100,
    "price": 29.99,
    "description": "Ergonomic wireless mouse"
  }'
```

**Auth:** Yes | **Role:** ADMIN, MANAGER

**Response 201:** `{ "success": true, "message": "Product created successfully", "data": { <Product> } }`

**Error 400 (duplicate SKU):** `{ "success": false, "message": "SKU already exists: MOUSE-001" }`

---

#### PUT /api/products/{id}

```bash
curl -X PUT https://smartinventory-production-2890.up.railway.app/api/products/1 \
  -H "Authorization: Bearer eyJhbGci..." \
  -H "Content-Type: application/json" \
  -d '{"name": "Wireless Mouse Pro", "sku": "MOUSE-001", "category": "Electronics", "quantity": 95, "price": 34.99}'
```

**Auth:** Yes | **Role:** ADMIN, MANAGER

**Response 200:** `{ "success": true, "message": "Product updated successfully", "data": { <Product> } }`

---

#### POST /api/products/{id}/image

```bash
curl -X POST https://smartinventory-production-2890.up.railway.app/api/products/1/image \
  -H "Authorization: Bearer eyJhbGci..." \
  -F "file=@/path/to/image.jpg"
```

**Auth:** Yes | **Role:** ADMIN, MANAGER | **Content-Type:** `multipart/form-data`

**Response 200:** `{ "success": true, "message": "Image uploaded successfully", "data": "https://s3.amazonaws.com/..." }`

---

#### DELETE /api/products/{id}

```bash
curl -X DELETE https://smartinventory-production-2890.up.railway.app/api/products/1 \
  -H "Authorization: Bearer eyJhbGci..."
```

**Auth:** Yes | **Role:** ADMIN only

**Response 200:** `{ "success": true, "message": "Product deleted successfully" }`

**Error 400 (has orders):** `{ "success": false, "message": "Cannot delete 'Laptop Pro X' — it is referenced by existing orders" }`

---

### Order Endpoints

---

#### POST /api/orders

```bash
curl -X POST https://smartinventory-production-2890.up.railway.app/api/orders \
  -H "Authorization: Bearer eyJhbGci..." \
  -H "Content-Type: application/json" \
  -d '{
    "items": [
      {"productId": 1, "quantity": 2},
      {"productId": 3, "quantity": 1}
    ],
    "shippingAddress": "123 Main St, Springfield",
    "notes": "Leave at door"
  }'
```

**Auth:** Yes | **Role:** Any authenticated

**Response 201:**
```json
{
  "success": true,
  "message": "Order placed successfully",
  "data": {
    "id": 1,
    "orderNumber": "ORD-A1B2C3D4",
    "username": "johndoe",
    "status": "PENDING",
    "totalAmount": 2629.97,
    "shippingAddress": "123 Main St, Springfield",
    "notes": "Leave at door",
    "items": [
      {
        "id": 1,
        "productId": 1,
        "productName": "Laptop Pro X",
        "productSku": "LAP-001",
        "quantity": 2,
        "unitPrice": 1299.99,
        "totalPrice": 2599.98
      },
      {
        "id": 2,
        "productId": 3,
        "productName": "Wireless Mouse",
        "productSku": "MOUSE-001",
        "quantity": 1,
        "unitPrice": 29.99,
        "totalPrice": 29.99
      }
    ],
    "createdAt": "2026-06-07T10:00:00",
    "updatedAt": "2026-06-07T10:00:00"
  }
}
```

**Error 409:** `{ "success": false, "message": "Insufficient stock for 'Laptop Pro X': requested 10, available 3" }`

---

#### GET /api/orders

```bash
curl "https://smartinventory-production-2890.up.railway.app/api/orders?page=0&size=20&sort=createdAt,desc" \
  -H "Authorization: Bearer eyJhbGci..."
```

**Auth:** Yes | **Role:** ADMIN, MANAGER

**Response 200:** Paginated `{ "success": true, "data": { "content": [ <Order> ], "totalElements": ..., ... } }`

---

#### GET /api/orders/my-orders

```bash
curl https://smartinventory-production-2890.up.railway.app/api/orders/my-orders \
  -H "Authorization: Bearer eyJhbGci..."
```

**Auth:** Yes | **Role:** Any authenticated | **Response 200:** Paginated orders for logged-in user.

---

#### GET /api/orders/{id}

```bash
curl https://smartinventory-production-2890.up.railway.app/api/orders/1 \
  -H "Authorization: Bearer eyJhbGci..."
```

**Auth:** Yes | **Role:** Any authenticated | **Response 200:** `{ "success": true, "data": { <Order> } }`

---

#### GET /api/orders/number/{orderNumber}

```bash
curl https://smartinventory-production-2890.up.railway.app/api/orders/number/ORD-A1B2C3D4 \
  -H "Authorization: Bearer eyJhbGci..."
```

**Auth:** Yes | **Role:** Any authenticated | **Response 200:** `{ "success": true, "data": { <Order> } }`

---

#### PUT /api/orders/{id}/status

```bash
curl -X PUT "https://smartinventory-production-2890.up.railway.app/api/orders/1/status?status=PROCESSING" \
  -H "Authorization: Bearer eyJhbGci..."
```

**Auth:** Yes | **Role:** ADMIN, MANAGER
**Query param:** `status` = `PENDING` | `PROCESSING` | `COMPLETED` | `CANCELLED`

**Response 200:** `{ "success": true, "message": "Order status updated", "data": { <Order> } }`

---

### Report Endpoints (ADMIN, MANAGER only)

---

#### GET /api/reports/inventory

```bash
curl https://smartinventory-production-2890.up.railway.app/api/reports/inventory \
  -H "Authorization: Bearer eyJhbGci..."
```

**Response 200:**
```json
{
  "success": true,
  "data": {
    "totalProducts": 42,
    "lowStockCount": 3,
    "totalInventoryValue": 125430.50,
    "categories": ["Electronics", "Furniture"],
    "lowStockProducts": [
      {"id": 5, "name": "Keyboard", "sku": "KEY-001", "quantity": 2}
    ],
    "generatedAt": "2026-06-07T10:00:00"
  }
}
```

---

#### GET /api/reports/orders

```bash
curl "https://smartinventory-production-2890.up.railway.app/api/reports/orders?start=2026-01-01T00:00:00&end=2026-12-31T23:59:59" \
  -H "Authorization: Bearer eyJhbGci..."
```

**Query params:** `start` and `end` (ISO-8601 datetime)

**Response 200:**
```json
{
  "success": true,
  "data": {
    "totalOrders": 150,
    "totalRevenue": 98432.00,
    "pendingOrders": 10,
    "processingOrders": 25,
    "completedOrders": 110,
    "cancelledOrders": 5,
    "dateRange": {"start": "2026-01-01T00:00:00", "end": "2026-12-31T23:59:59"},
    "generatedAt": "2026-06-07T10:00:00"
  }
}
```

---

#### GET /api/reports/inventory/export

```bash
curl https://smartinventory-production-2890.up.railway.app/api/reports/inventory/export \
  -H "Authorization: Bearer eyJhbGci..."
```

**Response 200:** `{ "success": true, "message": "Inventory exported successfully", "data": "https://s3.amazonaws.com/...?X-Amz-Expires=3600..." }`

---

#### GET /api/reports/orders/export

```bash
curl "https://smartinventory-production-2890.up.railway.app/api/reports/orders/export?start=2026-01-01T00:00:00&end=2026-12-31T23:59:59" \
  -H "Authorization: Bearer eyJhbGci..."
```

**Response 200:** `{ "success": true, "message": "Orders exported successfully", "data": "<presigned-url>" }`

---

### Admin Endpoints (ADMIN only)

---

#### GET /api/admin/users

```bash
curl https://smartinventory-production-2890.up.railway.app/api/admin/users \
  -H "Authorization: Bearer eyJhbGci..."
```

**Response 200:** `{ "success": true, "data": [ { <User> } ] }`

---

#### GET /api/admin/users/{id}

```bash
curl https://smartinventory-production-2890.up.railway.app/api/admin/users/1 \
  -H "Authorization: Bearer eyJhbGci..."
```

**Response 200:** `{ "success": true, "data": { <User> } }`

---

#### PUT /api/admin/users/{id}/role

```bash
curl -X PUT "https://smartinventory-production-2890.up.railway.app/api/admin/users/5/role?role=MANAGER" \
  -H "Authorization: Bearer eyJhbGci..."
```

**Query param:** `role` = `ADMIN` | `MANAGER` | `STAFF`

**Response 200:** `{ "success": true, "message": "Role updated successfully", "data": { <User> } }`

---

#### PUT /api/admin/users/{id}/status

```bash
curl -X PUT "https://smartinventory-production-2890.up.railway.app/api/admin/users/5/status?active=false" \
  -H "Authorization: Bearer eyJhbGci..."
```

**Query param:** `active` = `true` | `false`

**Response 200:** `{ "success": true, "message": "User status updated", "data": { <User> } }`

---

#### GET /api/admin/audit-logs

```bash
curl "https://smartinventory-production-2890.up.railway.app/api/admin/audit-logs?page=0&size=20&sort=timestamp,desc" \
  -H "Authorization: Bearer eyJhbGci..."
```

**Response 200:** Paginated `{ "success": true, "data": { "content": [ { <AuditLog> } ], ... } }`

---

## 6. Database Schema

### Tables

#### `users`

| Column | Type | Constraints |
|---|---|---|
| `id` | BIGSERIAL | PRIMARY KEY |
| `username` | VARCHAR | NOT NULL, UNIQUE |
| `email` | VARCHAR | NOT NULL, UNIQUE |
| `password` | VARCHAR | NOT NULL (bcrypt) |
| `full_name` | VARCHAR | |
| `role` | VARCHAR | NOT NULL (ADMIN/MANAGER/STAFF) |
| `refresh_token` | TEXT | NULLABLE |
| `is_active` | BOOLEAN | DEFAULT true |
| `created_at` | TIMESTAMP | NOT NULL |
| `updated_at` | TIMESTAMP | |

#### `products`

| Column | Type | Constraints |
|---|---|---|
| `id` | BIGSERIAL | PRIMARY KEY |
| `name` | VARCHAR | NOT NULL |
| `sku` | VARCHAR | NOT NULL, UNIQUE |
| `category` | VARCHAR | NOT NULL |
| `quantity` | INTEGER | NOT NULL |
| `price` | NUMERIC(10,2) | NOT NULL |
| `image_url` | VARCHAR | |
| `description` | TEXT | |
| `created_at` | TIMESTAMP | NOT NULL |
| `updated_at` | TIMESTAMP | |

#### `orders`

| Column | Type | Constraints |
|---|---|---|
| `id` | BIGSERIAL | PRIMARY KEY |
| `order_number` | VARCHAR | NOT NULL, UNIQUE |
| `user_id` | BIGINT | NOT NULL, FK → users.id |
| `status` | VARCHAR | NOT NULL (PENDING/PROCESSING/COMPLETED/CANCELLED) |
| `total_amount` | NUMERIC(10,2) | NOT NULL |
| `shipping_address` | VARCHAR | |
| `notes` | TEXT | |
| `created_at` | TIMESTAMP | NOT NULL |
| `updated_at` | TIMESTAMP | |

#### `order_items`

| Column | Type | Constraints |
|---|---|---|
| `id` | BIGSERIAL | PRIMARY KEY |
| `order_id` | BIGINT | NOT NULL, FK → orders.id (CASCADE DELETE) |
| `product_id` | BIGINT | NOT NULL, FK → products.id |
| `quantity` | INTEGER | NOT NULL |
| `unit_price` | NUMERIC(10,2) | NOT NULL |
| `total_price` | NUMERIC(10,2) | NOT NULL |

#### `audit_logs`

| Column | Type | Constraints |
|---|---|---|
| `id` | BIGSERIAL | PRIMARY KEY |
| `username` | VARCHAR | |
| `action_type` | VARCHAR | NOT NULL |
| `entity_type` | VARCHAR | |
| `entity_id` | VARCHAR | |
| `details` | TEXT | |
| `ip_address` | VARCHAR | |
| `timestamp` | TIMESTAMP | NOT NULL |

### Entity Relationships

```
users (1) ──────────< orders (many)
                          │
                          │ (1)
                          ▼
                      order_items (many) >──── products (1)
                          │
                          └─ CASCADE ALL from orders
                             (delete order → delete its items)
                             (order_items.product_id has NO CASCADE)
                             (cannot delete product if it has order_items)
```

**DDL is auto-generated by Hibernate** using `spring.jpa.hibernate.ddl-auto=update` in production, which creates tables on first run and adds missing columns on subsequent runs (never drops data).

---

## 7. pom.xml Dependencies

```xml
<parent>spring-boot-starter-parent:3.5.0</parent>
```

The parent BOM manages versions for all Spring Boot, Spring Framework, and Hibernate dependencies.

| Dependency | Version | Purpose |
|---|---|---|
| `spring-boot-starter-web` | (managed) | Embedded Tomcat, Spring MVC, REST controllers |
| `spring-boot-starter-security` | (managed) | Spring Security — auth filters, method security |
| `spring-boot-starter-data-jpa` | (managed) | Hibernate ORM, Spring Data repositories |
| `spring-boot-starter-mail` | (managed) | JavaMailSender for SMTP → SES |
| `spring-boot-starter-validation` | (managed) | Bean Validation (`@NotBlank`, `@Valid`, etc.) |
| `spring-boot-starter-actuator` | (managed) | `/actuator/health` endpoint |
| `springdoc-openapi-starter-webmvc-ui` | 2.8.6 | Swagger UI + `/v3/api-docs` |
| `jjwt-api` | 0.12.5 | JWT creation/parsing API |
| `jjwt-impl` | 0.12.5 | JJWT implementation (runtime) |
| `jjwt-jackson` | 0.12.5 | JJWT JSON serialization via Jackson (runtime) |
| `spring-cloud-aws-starter-s3` | 3.2.0 | `S3Template` + AWS SDK S3 autoconfiguration |
| `spring-cloud-aws-starter-sqs` | 3.2.0 | `SqsTemplate`, `@SqsListener` + SQS autoconfiguration |
| `spring-cloud-aws-starter-ses` | 3.2.0 | SES SMTP autoconfiguration |
| `software.amazon.awssdk:cloudwatch` | (AWS BOM) | CloudWatch SDK v2 client |
| `h2` | (managed) | In-memory database for local development |
| `postgresql` | (managed) | PostgreSQL JDBC driver for production |
| `lombok` | (managed) | Annotation-based code generation (`@Data`, `@Builder`, etc.) |
| `spring-boot-starter-test` | (managed) | JUnit 5, Mockito, AssertJ for tests |

**Why springdoc 2.8.6 specifically:** springdoc 2.5.0 (the previous version) called `ControllerAdviceBean(Object)` which was removed in Spring Framework 6.2.x (bundled with Spring Boot 3.5.0), causing a `NoSuchMethodError` on every `/v3/api-docs` request. 2.8.6 is the latest release and is compatible.

---

## 8. Config Files

### application.properties (local/default)

```properties
# Application
spring.application.name=SmartInventory
server.port=8080

# H2 Database (Local)
spring.datasource.url=jdbc:h2:mem:smartinventory;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE
spring.datasource.driver-class-name=org.h2.Driver
spring.datasource.username=sa
spring.datasource.password=
spring.h2.console.enabled=true
spring.h2.console.path=/h2-console

# JPA
spring.jpa.database-platform=org.hibernate.dialect.H2Dialect
spring.jpa.hibernate.ddl-auto=create-drop
spring.jpa.show-sql=false
spring.jpa.properties.hibernate.format_sql=true

# JWT
jwt.secret=U21hcnRJbnZlbnRvcnktU3VwZXItU2VjcmV0LUtleS0yMDI0LVZlcnktTG9uZy1LZXktRm9yLUpXVC1Ub2tlbi1HZW5lcmF0aW9u
jwt.expiration=86400000
jwt.refresh-expiration=604800000

# AWS (local defaults - override via env vars in prod)
aws.access-key=${AWS_ACCESS_KEY:}
aws.secret-key=${AWS_SECRET_KEY:}
aws.region=${AWS_REGION:ap-south-1}
aws.s3.bucket-name=${S3_BUCKET_NAME:smartinventory-uploads}
aws.sqs.order-queue-url=${SQS_ORDER_QUEUE_URL:order-events}
aws.sqs.stock-queue-url=${SQS_STOCK_QUEUE_URL:stock-alerts}
aws.ses.from-email=${SES_FROM_EMAIL:noreply@smartinventory.com}
aws.ses.alert-email=${SES_FROM_EMAIL:admin@smartinventory.com}
cloudwatch.log-group=${CLOUDWATCH_LOG_GROUP:smartinventory-logs}

# Spring Cloud AWS - disable auto-configuration locally
spring.cloud.aws.credentials.access-key=${AWS_ACCESS_KEY:dummy}
spring.cloud.aws.credentials.secret-key=${AWS_SECRET_KEY:dummy}
spring.cloud.aws.region.static=${AWS_REGION:ap-south-1}

# Disable SQS listeners locally (no real queues)
spring.cloud.aws.sqs.listener.auto-startup=false

# Mail (SES via SMTP) - dummy for local
spring.mail.host=${MAIL_HOST:email-smtp.ap-south-1.amazonaws.com}
spring.mail.port=${MAIL_PORT:587}
spring.mail.username=${MAIL_USERNAME:dummy}
spring.mail.password=${MAIL_PASSWORD:dummy}
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.starttls.enable=true

# Actuator
management.endpoints.web.exposure.include=health,info,metrics
management.endpoint.health.show-details=always

# File upload
spring.servlet.multipart.max-file-size=10MB
spring.servlet.multipart.max-request-size=10MB

# Logging
logging.level.com.smartinventory=INFO
logging.level.org.springframework.security=WARN
logging.level.io.awspring=WARN
```

### application-prod.properties (Railway/Production)

```properties
# PostgreSQL (Railway)
spring.datasource.url=${SPRING_DATASOURCE_URL:jdbc:postgresql://${PGHOST}:${PGPORT:5432}/${PGDATABASE}}
spring.datasource.driver-class-name=org.postgresql.Driver
spring.datasource.username=${PGUSER}
spring.datasource.password=${PGPASSWORD}

# JPA
spring.jpa.database-platform=org.hibernate.dialect.PostgreSQLDialect
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=false

# AWS Credentials
spring.cloud.aws.credentials.access-key=${AWS_ACCESS_KEY}
spring.cloud.aws.credentials.secret-key=${AWS_SECRET_KEY}
spring.cloud.aws.region.static=${AWS_REGION:ap-south-1}

# SQS
aws.sqs.order-queue-url=${SQS_ORDER_QUEUE_URL}
aws.sqs.stock-queue-url=${SQS_STOCK_QUEUE_URL}
aws.sqs.enabled=${AWS_SQS_ENABLED:false}
spring.cloud.aws.sqs.enabled=true

# SES (SMTP)
aws.ses.from-email=${SES_FROM_EMAIL:noreply@smartinventory.com}
spring.mail.host=email-smtp.${AWS_REGION:ap-south-1}.amazonaws.com
spring.mail.port=587
spring.mail.username=${SES_SMTP_USERNAME:dummy}
spring.mail.password=${SES_SMTP_PASSWORD:dummy}
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.starttls.enable=true

# S3
aws.s3.bucket-name=${S3_BUCKET_NAME:smartinventory-uploads}

# CloudWatch
cloudwatch.log-group=${CLOUDWATCH_LOG_GROUP:smartinventory-logs}

# JWT
jwt.secret=${JWT_SECRET}
jwt.expiration=86400000
jwt.refresh-expiration=604800000

# H2 console disabled in prod
spring.h2.console.enabled=false

# Logging
logging.level.com.smartinventory=INFO
logging.level.org.springframework.security=WARN
```

### Dockerfile

```dockerfile
# Stage 1: Build the JAR inside Docker
FROM maven:3.9-eclipse-temurin-21-alpine AS builder
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -q
COPY src ./src
RUN mvn clean package -DskipTests -q

# Stage 2: Minimal JRE runtime image
FROM eclipse-temurin:21-jre-alpine
COPY --from=builder /app/target/smartinventory.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","/app.jar"]
```

**Why multi-stage:** Railway respects `.gitignore`, which excludes `target/` and `*.jar`. A single-stage Dockerfile that copies `target/smartinventory.jar` would fail because the JAR is never sent to the build context. The multi-stage approach builds the JAR inside Docker from the source files, which are tracked by git.

---

## 9. Railway Deployment

### Initial Setup

1. Sign up at `railway.app`
2. New project → Deploy from GitHub repo
3. Connect `github.com/tm1206/smartinventory`
4. Railway detects Dockerfile automatically

### Add PostgreSQL

1. Railway dashboard → New → Database → PostgreSQL
2. Railway auto-sets these env vars on the app service:
   - `PGHOST`, `PGPORT`, `PGDATABASE`, `PGUSER`, `PGPASSWORD`
3. No code change needed — `application-prod.properties` reads them automatically

### Set Environment Variables

In Railway dashboard → Your service → Variables, add:

```
SPRING_PROFILES_ACTIVE = prod
AWS_ACCESS_KEY         = <IAM access key>
AWS_SECRET_KEY         = <IAM secret key>
AWS_REGION             = ap-south-1
S3_BUCKET_NAME         = smartinventory-uploads
SQS_ORDER_QUEUE_URL    = https://sqs.ap-south-1.amazonaws.com/454681118572/order-events
SQS_STOCK_QUEUE_URL    = https://sqs.ap-south-1.amazonaws.com/454681118572/stock-alerts
AWS_SQS_ENABLED        = true
SES_FROM_EMAIL         = noreply@yourdomain.com
SES_SMTP_USERNAME      = <SES SMTP username>
SES_SMTP_PASSWORD      = <SES SMTP password>
CLOUDWATCH_LOG_GROUP   = smartinventory-logs
JWT_SECRET             = <min 64 character random string>
```

### How to Redeploy

**Option 1 — Push to GitHub** (Railway auto-deploys on push to main):
```bash
git add .
git commit -m "your message"
git push origin main
```

**Option 2 — Railway CLI:**
```bash
npm install -g @railway/cli
railway login
railway link  # link to project
railway up    # deploy current directory
```

**Option 3 — Railway dashboard:**
Dashboard → Deployments → Redeploy last build.

### Railway CLI Reference

```bash
railway logs          # tail recent logs
railway logs --tail N # last N lines
railway variables     # list all env vars
railway service list  # list services in project
railway status        # check deployment status
railway up --detach   # deploy without streaming logs
```

### Monitoring the Deployment

After `railway up`, watch logs for:
```
Started SmartInventoryApplication in X seconds
```

If you see `HikariPool` errors, the database connection failed — check `PGHOST`/`PGUSER`/`PGPASSWORD`.

---

## 10. Spring Boot Concepts

### JWT Authentication Flow

```
1. CLIENT REGISTERS/LOGS IN:
   POST /api/auth/login { username, password }

2. SERVER:
   AuthenticationManager.authenticate()  → BadCredentialsException if wrong
   Load user from DB
   JwtUtil.generateAccessToken(userDetails)
     → Jwts.builder()
          .subject(username)
          .claims({ "type": "access" })
          .issuedAt(now)
          .expiration(now + 24h)
          .signWith(HS512 key derived from JWT_SECRET)
          .compact()
   JwtUtil.generateRefreshToken(userDetails)
     → Same but expiration = now + 7d, type = "refresh"
   Store refreshToken in users.refresh_token column
   Return { accessToken, refreshToken, expiresIn: 86400000 }

3. CLIENT MAKES PROTECTED REQUEST:
   Authorization: Bearer <accessToken>

4. JwtFilter (runs on every request):
   Extract token after "Bearer "
   JwtUtil.extractUsername(token)      → parse subject claim
   UserDetailsService.loadByUsername() → DB lookup
   JwtUtil.isTokenValid(token, user)   → username match + not expired
   SecurityContextHolder.setAuthentication(token)
   Continue filter chain

5. METHOD-LEVEL SECURITY:
   @PreAuthorize("hasRole('ADMIN')") checks ROLE_ADMIN in SecurityContext

6. TOKEN REFRESH (before 24h expiry):
   POST /api/auth/refresh { refreshToken }
   Find user by refreshToken column
   Verify not expired (up to 7 days)
   Issue new accessToken + refreshToken
   Rotate: update refreshToken in DB, return new pair

7. LOGOUT:
   POST /api/auth/logout (with valid access token)
   Set users.refresh_token = null
   Old refresh token is now invalid
```

### Spring Security Configuration

The filter chain processes each request in this order:
1. **CORS filter** — adds `Access-Control-*` headers, handles `OPTIONS` preflight (returns 200 with headers, no auth needed)
2. **JwtFilter** — extracts and validates Bearer token, sets SecurityContext
3. **ExceptionTranslationFilter** — converts security exceptions to 401/403 responses
4. **FilterSecurityInterceptor** — enforces URL-level rules from `SecurityConfig`
5. **Method interceptor** — enforces `@PreAuthorize` on controller methods

Key design decisions:
- **CSRF disabled** — JWT is stateless, CSRF tokens are only needed for cookie-based sessions
- **Stateless session** — `SessionCreationPolicy.STATELESS` means no `HttpSession` is created
- **`@EnableMethodSecurity`** — enables `@PreAuthorize`, `@PostAuthorize` on methods

### SQS Listener Setup

```
@ConditionalOnProperty(name = "aws.sqs.enabled", havingValue = "true")
```
This prevents the listener beans from being created at all when SQS is not enabled (local dev or when `AWS_SQS_ENABLED=false`). Without this guard, Spring Cloud AWS would try to connect to SQS on startup and fail with 403 errors.

```
@SqsListener("${aws.sqs.order-queue-url}")
public void handleOrderEvent(String message) { ... }
```
Spring Cloud AWS polls the queue with long polling, deserializes the message, and calls this method. On exception, the message is not deleted and becomes visible again after the visibility timeout.

### S3 Upload Flow

```
Client sends: POST /api/products/1/image (multipart/form-data, file=image.jpg)
     │
ProductController.uploadImage()
     │
ProductService.uploadProductImage()
     │
S3Service.uploadFile(MultipartFile, "products")
     ├── Generate key: "products/{UUID}.jpg"
     ├── s3Template.upload(bucketName, key, inputStream)
     │   └── Uses Spring Cloud AWS S3Template (wraps AWS SDK v2)
     └── Return URL: "https://smartinventory-uploads.s3.ap-south-1.amazonaws.com/products/{UUID}.jpg"
     │
ProductService: product.setImageUrl(url) → productRepository.save(product)
     │
Response: { "data": "https://smartinventory-uploads.s3..." }
```

For reports, `S3Service.uploadCsvReport()` then calls `generatePresignedUrl()` using `S3Presigner` — this creates a time-limited URL (1 hour) that allows downloading without needing AWS credentials.

### SES Email Flow

```
JavaMailSender (Spring Boot mail starter)
     │
     └── Connects to: email-smtp.ap-south-1.amazonaws.com:587
         Using: SES_SMTP_USERNAME + SES_SMTP_PASSWORD (STARTTLS)

SESService.sendEmail(toEmail, subject, htmlBody):
     MimeMessage message = mailSender.createMimeMessage()
     MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8")
     helper.setFrom(SES_FROM_EMAIL)
     helper.setTo(toEmail)
     helper.setText(htmlBody, true)   ← true = HTML
     mailSender.send(message)

NOTE: SES sandbox only sends to verified email addresses.
      The `toEmail` is user.getEmail() — only works for registered users
      whose email is verified in SES.
```

### CloudWatch Logging Flow

```
CloudWatchService.logLoginAttempt("johndoe", true)
     │
     └── MetricDatum.builder()
              .metricName("LoginSuccess")
              .value(1.0)
              .unit(StandardUnit.COUNT)
              .timestamp(Instant.now())
              .build()
     │
     └── PutMetricDataRequest.builder()
              .namespace("smartinventory-logs")    ← CLOUDWATCH_LOG_GROUP env var
              .metricData(datum)
              .build()
     │
     └── cloudWatchClient.putMetricData(request)
         → AWS CloudWatch API: PUT /metricData

Visible in: CloudWatch Console → Metrics → Custom Namespaces → smartinventory-logs
```

### CORS Configuration

```java
// In SecurityConfig.java
.cors(cors -> cors.configurationSource(corsConfigurationSource()))

@Bean
public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration config = new CorsConfiguration();
    config.setAllowedOriginPatterns(List.of("*"));  // NOT allowedOrigins("*")
    config.setAllowedMethods(List.of("GET","POST","PUT","DELETE","OPTIONS"));
    config.setAllowedHeaders(List.of("Authorization","Content-Type"));
    config.setAllowCredentials(true);
    
    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", config);
    return source;
}
```

**Why `allowedOriginPatterns("*")` not `allowedOrigins("*")`:**
The CORS spec prohibits `Access-Control-Allow-Origin: *` when `Access-Control-Allow-Credentials: true`. Browsers reject such responses. `allowedOriginPatterns("*")` is Spring's workaround: it reflects the actual request origin back in the response header, satisfying the spec while effectively allowing all origins.

**Why CORS is in Spring Security and not `@CrossOrigin`:**
The Spring Security filter chain runs before the MVC layer. If CORS is only configured in MVC, Spring Security's filters reject `OPTIONS` preflight requests with 401 before they ever reach the CORS configuration. Wiring it into the security filter chain via `cors()` ensures preflight requests pass through first.

---

## 11. Interview Preparation

### 30 Questions + Detailed Answers

#### Authentication & Security

**Q1: How does JWT authentication work in your application?**

On login, the server authenticates credentials via `DaoAuthenticationProvider`, generates an access token (24h, HS512-signed) and refresh token (7d) using JJWT, stores the refresh token in the database, and returns both to the client. On subsequent requests, the `JwtFilter` (a `OncePerRequestFilter`) extracts the Bearer token from the `Authorization` header, validates its signature and expiry using `JwtUtil`, loads the user from the database, and sets the authentication in `SecurityContextHolder`. No server-side session is maintained — the token itself is the session.

**Q2: Why do you use both an access token and a refresh token?**

Short-lived access tokens (24h) limit the window of exposure if a token is stolen — an attacker can only use it for 24 hours. Long-lived refresh tokens (7d) allow users to stay logged in without re-entering credentials. Refresh tokens are stored in the database, so they can be explicitly invalidated on logout. Access tokens are stateless and cannot be invalidated before expiry, which is why they're kept short-lived.

**Q3: What does `@PreAuthorize("hasRole('ADMIN')")` do and how does it work?**

`@PreAuthorize` is processed by a Spring Security AOP proxy (enabled by `@EnableMethodSecurity`). When a method is called, the proxy evaluates the SpEL expression against the current `SecurityContextHolder.getAuthentication()`. `hasRole('ADMIN')` checks for the granted authority `ROLE_ADMIN` (Spring Security automatically prepends `ROLE_`). `hasAnyRole('ADMIN','MANAGER')` checks for either. If the check fails, Spring throws `AccessDeniedException`, which `GlobalExceptionHandler` catches and returns as HTTP 403.

**Q4: What is CSRF and why is it disabled here?**

CSRF (Cross-Site Request Forgery) attacks exploit browser behavior: a malicious site tricks a logged-in user's browser into making state-changing requests to your site using their cookies. The defense is a CSRF token in the request body or header. In this app, authentication is done via a JWT in the `Authorization` header, not cookies. Browsers don't automatically attach custom headers cross-origin, so CSRF attacks can't include the token. Therefore CSRF protection is unnecessary and safely disabled.

**Q5: How does the password get stored and verified?**

Passwords are hashed using `BCryptPasswordEncoder` (work factor 10 by default). BCrypt is a one-way hash that includes a random salt — the same password produces different hashes each time. The encoded hash is stored in the database. On login, `DaoAuthenticationProvider` calls `passwordEncoder.matches(rawPassword, encodedHash)`, which re-computes BCrypt with the embedded salt and compares — no plaintext ever stored.

**Q6: What happens when a JWT token expires?**

The `JwtFilter` calls `JwtUtil.isTokenExpired()` which checks the `exp` claim against the current time. If expired, `isTokenValid()` returns false, no authentication is set in `SecurityContextHolder`, and the request proceeds as unauthenticated. The endpoint's security check then returns 401 Unauthorized. The client should detect 401, call `POST /api/auth/refresh` with the refresh token to get a new access token, and retry the original request.

**Q7: How do you prevent a deactivated user from logging in?**

`UserDetailsServiceImpl.loadUserByUsername()` maps `user.isActive()` to Spring Security's `accountLocked` and `disabled` flags. The `DaoAuthenticationProvider` checks these flags and throws `LockedException` / `DisabledException` if the account is inactive — preventing login. If a token is already held by a deactivated user, the next request will still load the user from the database (JwtFilter calls `loadByUsername`), and Spring Security will reject the authentication because the user is disabled.

---

#### Spring Boot & JPA

**Q8: What is the purpose of `@Transactional` and where did you use it?**

`@Transactional` wraps a method in a database transaction — all database operations within it either all commit or all roll back. I used it on:
- `placeOrder()` — stock deduction + order creation must be atomic; if saving the order fails after deducting stock, the stock is restored
- `createProduct()`, `updateProduct()`, `deleteProduct()` — any partial failure rolls back
- `register()`, `login()`, `refreshToken()` — token generation + DB update must be atomic

**Q9: Explain the Hibernate null parameter bug you hit with PostgreSQL.**

When a JPQL query has a nullable `String` parameter and the value is `null`, Hibernate 6 passes it to JDBC as an untyped null. PostgreSQL cannot infer the column type for a null parameter and defaults to `bytea` (binary). When the query tries to use this `bytea` value in a `LOWER()` function expecting `text`, it fails with `function lower(bytea) does not exist`. The fix: convert null parameters to empty strings in the service layer, then use `= ''` instead of `IS NULL` in the JPQL query.

**Q10: What is the difference between `ddl-auto=create-drop` and `update`?**

`create-drop` creates all tables on startup and drops them on shutdown — used locally where the H2 in-memory database resets anyway. `update` reads the current schema and applies any missing columns or tables — used in production so data is never lost between deployments. Neither should run `validate` in production without careful schema migration tooling (Flyway/Liquibase).

**Q11: How does `@Async` work in `AuditLogService`?**

`@EnableScheduling` on the main class enables Spring's async executor. `@Async` on `AuditLogService.log()` causes Spring to intercept the method call, submit it to a thread pool, and return immediately to the caller. The audit log write happens in a background thread — the API response is sent before the log is persisted. This keeps response times fast even if audit logging is slow (e.g., DB write backpressure). Errors are caught and logged but never propagate to the caller.

**Q12: What is `@ConditionalOnProperty` and why did you use it on SQS listeners?**

`@ConditionalOnProperty` tells Spring to only create the annotated bean if a specific property has a specific value. Without it, Spring Cloud AWS creates the SQS listener beans and immediately tries to connect to SQS. In local dev without real AWS credentials, this causes 403/connection errors on startup. With `@ConditionalOnProperty(name = "aws.sqs.enabled", havingValue = "true")`, the listener beans are not instantiated at all unless explicitly enabled. This decouples AWS dependency from local development.

---

#### AWS Services

**Q13: How does S3 upload work in your app?**

`ProductController` receives the multipart file, passes it to `ProductService`, which delegates to `S3Service.uploadFile()`. The service generates a UUID-based key (`products/{UUID}.jpg`), opens an `InputStream` from the `MultipartFile`, and calls `S3Template.upload(bucket, key, stream)` — a Spring Cloud AWS helper that wraps the AWS SDK v2 `PutObjectRequest`. The response is a public URL `https://{bucket}.s3.{region}.amazonaws.com/{key}` which is stored in `product.imageUrl`.

**Q14: What is a pre-signed URL and why is it used for CSV exports?**

A pre-signed URL is a time-limited URL that grants temporary access to a private S3 object without requiring AWS credentials. `S3Presigner.presignGetObject()` embeds the authorization signature in the URL query string. The recipient can download the file directly from S3 without going through the API server. This is better than streaming the file through the server (saves bandwidth and processing) and better than making the bucket public (preserves security). The reports use 1-hour expiry.

**Q15: Explain your SQS message flow for order events.**

When an order is placed: `OrderService.placeOrder()` calls `SQSService.publishOrderEvent()` which serializes a map `{orderId, orderNumber, userId, username, totalAmount, status=PROCESSING, timestamp}` to JSON and sends it to the `order-events` SQS queue via `SqsTemplate.send()`. The `OrderStatusListener` (when `AWS_SQS_ENABLED=true`) polls the queue, deserializes the message, and calls `orderService.updateOrderStatus(orderId, PROCESSING, "SYSTEM")` — automatically advancing the order state. This creates an event-driven status update without tight coupling.

**Q16: Why does SES require verified email addresses?**

AWS SES operates in "sandbox mode" by default to prevent spam. In sandbox mode, both the sender AND recipient must be verified email addresses in SES. Production access requires submitting a request to AWS explaining your use case and sending practices. Once approved, you can send to any address (as long as your bounce/complaint rates stay low). In this app, confirmation emails go to the logged-in user's email — testing requires that email to be SES-verified.

**Q17: What CloudWatch metrics does your app publish and what triggers them?**

- `LoginSuccess` — incremented on every successful `POST /api/auth/login`
- `LoginFailure` — incremented on every failed login attempt (wrong credentials)
- `ApiRequest` — available in `CloudWatchService.logApiRequest()` but not auto-wired to all endpoints (requires explicit call)
- `ApplicationException` — available in `logException()` for tracking uncaught errors

All metrics go to the `smartinventory-logs` namespace (configured via `CLOUDWATCH_LOG_GROUP`).

---

#### Architecture & Design

**Q18: Why did you choose a multi-stage Dockerfile?**

Railway respects `.gitignore`, which excludes `target/` and `*.jar` files. A single-stage Dockerfile `COPY target/smartinventory.jar` would fail because the JAR is never uploaded to the build context. The multi-stage build uses `maven:3.9-eclipse-temurin-21-alpine` as a builder to compile the JAR inside Docker from the source files (which ARE in git), then copies only the JAR into a minimal `eclipse-temurin:21-jre-alpine` runtime image (much smaller — no Maven or JDK in the final image).

**Q19: How is the database connection configured differently for local vs production?**

Spring Boot's profile system: `application.properties` defines H2 in-memory configuration for local development. `application-prod.properties` overrides with PostgreSQL when `SPRING_PROFILES_ACTIVE=prod`. Railway sets this env var, so `prod` activates automatically. The production config reads PostgreSQL connection details from `PGHOST`, `PGPORT`, `PGDATABASE`, `PGUSER`, `PGPASSWORD` — environment variables Railway auto-injects from the attached PostgreSQL service.

**Q20: Why does deleting a product with orders return a friendly message instead of a 500?**

A `@ManyToOne` FK from `order_items.product_id → products.id` has no `ON DELETE CASCADE`. Deleting a referenced product causes PostgreSQL to throw a FK violation, which Hibernate wraps as `DataIntegrityViolationException`. The `GlobalExceptionHandler` catches this and returns 409 Conflict. In `ProductService.deleteProduct()`, we additionally catch it and throw `IllegalArgumentException` with a message like `"Cannot delete 'Laptop Pro X' — it is referenced by existing orders"`. `productRepository.flush()` forces the SQL to execute within the transaction so the exception fires before the method returns.

**Q21: How do you handle errors consistently across all endpoints?**

`GlobalExceptionHandler` (`@RestControllerAdvice`) is a single class that handles all exceptions from any controller. Each `@ExceptionHandler` method catches a specific exception type and returns an `ApiResponse.error()` with the appropriate HTTP status. The `ApiResponse<T>` wrapper ensures every response — success or failure — has the same structure: `{success, message, data, errors, timestamp}`. `@JsonInclude(NON_NULL)` omits null fields, so success responses don't include `errors` and error responses don't include `data`.

---

#### Infrastructure & Deployment

**Q22: How do you deploy to Railway?**

Push to GitHub (Railway auto-deploys on push) or run `railway up` from the CLI. Railway detects the Dockerfile, builds the image, and deploys to a container with health checks. Environment variables are managed in the Railway dashboard and injected at runtime. The PostgreSQL service is managed by Railway and auto-connects via injected `PG*` environment variables.

**Q23: What caused the springdoc 500 error and how did you fix it?**

`springdoc-openapi 2.5.0` called `ControllerAdviceBean(Object)`, a constructor that was removed in Spring Framework 6.2.x (included in Spring Boot 3.5.0). This caused a `java.lang.NoSuchMethodError` every time `/v3/api-docs` was loaded — making Swagger UI show a 500. The fix was upgrading `springdoc.version` from `2.5.0` to `2.8.6` in `pom.xml`.

**Q24: How does CORS work in your app and why is it in Spring Security?**

CORS is configured as a `CorsConfigurationSource` bean wired into `HttpSecurity.cors()`. This ensures CORS headers are processed by the Spring Security filter chain before any authentication checks. Browser `OPTIONS` preflight requests would get 401 if CORS was only configured at the MVC level (Spring Security filters run first). With `allowedOriginPatterns("*")` + `allowCredentials(true)`, any origin can make credentialed requests — `allowedOrigins("*")` cannot be used with credentials per the CORS spec.

---

#### Code Quality

**Q25: What is Lombok and why did you use it?**

Lombok is an annotation processor that generates boilerplate Java code at compile time. `@Data` generates getters, setters, `equals`, `hashCode`, and `toString`. `@Builder` generates the builder pattern. `@RequiredArgsConstructor` generates a constructor for all `final` fields (used with Spring's constructor injection). `@Slf4j` injects a `private static final Logger log`. This dramatically reduces code size — the `Order` entity would be 3x longer without Lombok.

**Q26: Why use `BigDecimal` for prices instead of `double`?**

`double` and `float` are floating-point types that cannot represent most decimal fractions exactly (e.g., `0.1 + 0.2 = 0.30000000000000004`). For financial calculations, rounding errors accumulate and lead to incorrect totals. `BigDecimal` uses arbitrary-precision decimal arithmetic and is exact. The JPA mapping `precision=10, scale=2` maps to `NUMERIC(10,2)` in PostgreSQL, which also stores exact decimal values.

**Q27: What is the role of `@JsonInclude(NON_NULL)` on `ApiResponse`?**

Without this annotation, Jackson serializes all fields including those with `null` values. A successful response would include `"errors": null` and an error response would include `"data": null`, adding noise to the API output. `@JsonInclude(Include.NON_NULL)` tells Jackson to omit any field whose value is `null`. This keeps response payloads clean and smaller.

---

#### Tricky Questions

**Q28: If two requests simultaneously try to place an order for the last item in stock, what happens?**

Currently there is a potential race condition. Both requests read `quantity=1`, both see sufficient stock, both call `deductStock()` — the first commits `quantity=0`, the second also commits `quantity=0` (or -1), resulting in overselling. The proper fix is optimistic locking (`@Version` on `Product`) which adds a `version` column; the second update fails with `OptimisticLockException` if the version changed since the read. Alternatively, `SELECT ... FOR UPDATE` (pessimistic locking) holds a row-level lock during the transaction.

**Q29: What would happen if the SQS publish fails after the order is saved?**

The order is already committed to the database. The `SQSService.publishOrderEvent()` call is not in the same transaction and is not transactional itself. If it fails, the order exists but no SQS event is sent. The `OrderStatusListener` never fires, so the order stays `PENDING` instead of advancing to `PROCESSING`. The fix: use a transactional outbox pattern — write the event to a DB table in the same transaction as the order, then have a separate job publish events from the outbox table to SQS and delete them on success.

**Q30: Why does `@Async` on `AuditLogService` require `@EnableScheduling` on the main class?**

It actually doesn't require `@EnableScheduling` specifically — `@Async` requires `@EnableAsync`. In this project, `@EnableScheduling` is on the main class, which enables scheduled tasks but not async. The `@Async` works because Spring Boot auto-configures an `AsyncTaskExecutor` when it detects `@EnableAsync` or when the `TaskExecutionAutoConfiguration` is active (which it is by default in Spring Boot). `@EnableScheduling` enables `@Scheduled` methods. Both features share thread pool infrastructure, but `@Async` doesn't strictly need `@EnableScheduling`.

---

### How to Explain the Project

#### 30-Second Version

"SmartInventory is a production REST API I built with Spring Boot 3.5 and Java 21, deployed on Railway with PostgreSQL. It manages inventory products, orders, and users with role-based access control using JWT authentication. It integrates four AWS services: S3 for file storage, SQS for async event messaging, SES for transactional emails, and CloudWatch for custom metrics."

#### 2-Minute Version

"SmartInventory is a full-featured inventory management backend built with Spring Boot 3.5, Java 21, and PostgreSQL — live in production on Railway. The core features are product management with stock tracking, order placement with automatic stock deduction, and a role-based access system with three roles: Admin, Manager, and Staff.

Authentication uses JWT with both short-lived access tokens (24 hours) and refresh tokens (7 days). Spring Security enforces roles at the endpoint and method level. Every significant action is captured in an async audit log.

The app integrates four AWS services: product images upload to S3 with UUID filenames; when orders are placed, an event is published to SQS and a listener advances the order status asynchronously; SES sends welcome emails on registration and confirmation emails on order placement; CloudWatch receives custom metrics for login events.

I deployed it using a multi-stage Dockerfile — necessary because Railway respects .gitignore and excludes the compiled JAR, so I build the JAR inside Docker from source. The API is fully documented with Swagger UI."

#### 5-Minute Version

*(Use 2-minute version as base, then expand)*

"...Beyond the core features, I made several interesting engineering decisions.

For the database layer, I use Spring Data JPA with Hibernate 6. I hit a real bug specific to Hibernate 6 + PostgreSQL: when a JPQL query has nullable String parameters, Hibernate passes nulls as untyped binary data. PostgreSQL then fails because it can't apply text functions to binary. I fixed this by converting null search parameters to empty strings in the service layer and using empty-string equality checks instead of IS NULL in the JPQL query.

For security, I made a deliberate choice to keep CORS configuration inside Spring Security's filter chain rather than using Spring MVC's `@CrossOrigin`. The reason: Spring Security filters run before MVC. If CORS is only in MVC, OPTIONS preflight requests get 401 before they reach the CORS config. Wiring it into HttpSecurity ensures preflight requests are handled correctly.

The delete product feature has an interesting constraint: products referenced by orders can't be deleted due to a foreign key constraint. Instead of letting the database exception bubble up as a confusing 500, I catch `DataIntegrityViolationException` in the service layer, call `flush()` to force immediate SQL execution within the transaction, and throw a user-friendly `IllegalArgumentException` that the global exception handler converts to a 400 with a descriptive message.

I also spent time on the production deployment. The multi-stage Dockerfile was necessary because Railway gitignores the target/ directory. I needed to build the JAR inside Docker from source. And when I upgraded to Spring Boot 3.5, I discovered springdoc-openapi 2.5.0 was incompatible — it called a constructor removed in Spring Framework 6.2 — so I upgraded to 2.8.6."

### Key Technical Points to Highlight

1. **JWT with dual-token strategy** — access token (stateless, 24h) + refresh token (stored in DB, rotated on use, 7d)
2. **Spring Security filter chain** — CORS → JWT filter → auth checks → method security
3. **PostgreSQL null inference bug** — real production bug in Hibernate 6, non-obvious fix
4. **Async audit logging** — `@Async` ensures audit writes don't slow down API responses
5. **Multi-stage Dockerfile** — why it was necessary (Railway + .gitignore)
6. **`@ConditionalOnProperty`** — elegant way to feature-flag SQS listeners per environment
7. **Friendly FK violation handling** — `flush()` trick to catch DB constraint errors in-transaction
8. **springdoc incompatibility** — shows ability to diagnose library version conflicts
9. **Pre-signed S3 URLs** — for CSV report downloads (1-hour expiry, no credentials needed)
10. **SQS event-driven status updates** — orders flow asynchronously from PENDING → PROCESSING

---

## 12. Frontend Guide

### Base URL & Auth

```
Base URL: https://smartinventory-production-2890.up.railway.app
Swagger:  https://smartinventory-production-2890.up.railway.app/swagger-ui/index.html
```

**Auth header for all protected requests:**
```
Authorization: Bearer <accessToken>
```

### Authentication Flow

```
1. Register:  POST /api/auth/register   → get accessToken + refreshToken
2. Login:     POST /api/auth/login      → get accessToken + refreshToken
3. Use:       Add Authorization: Bearer <accessToken> to all requests
4. Refresh:   POST /api/auth/refresh when you get 401
5. Logout:    POST /api/auth/logout to invalidate refresh token
```

Access tokens expire in **86400000 ms (24 hours)**. When expired, the server returns 401. Refresh immediately using the refresh token (valid 7 days) before retrying.

### Response Envelope

Every response:
```json
{
  "success": true | false,
  "message": "optional string",
  "data": { ... } | [ ... ] | null,
  "errors": { "field": "error message" },
  "timestamp": "2026-06-07T10:00:00"
}
```

Null fields are omitted. `errors` only appears on 400 validation errors.

### Pagination

GET endpoints that return lists accept:

| Param | Default | Example |
|---|---|---|
| `page` | `0` | `?page=1` |
| `size` | `20` | `?size=10` |
| `sort` | none | `?sort=createdAt,desc` |

Response shape:
```json
{
  "content": [ ... ],
  "totalElements": 100,
  "totalPages": 5,
  "size": 20,
  "number": 0,
  "first": true,
  "last": false
}
```

### Roles

| Role | What they can do |
|---|---|
| `ADMIN` | Everything — all endpoints including admin panel, delete products |
| `MANAGER` | Manage products and orders, view reports; no admin panel |
| `STAFF` | Browse products, place orders, view own orders |

### Enums

**OrderStatus:** `PENDING` | `PROCESSING` | `COMPLETED` | `CANCELLED`

**Role:** `ADMIN` | `MANAGER` | `STAFF`

### All Models

**AuthResponse:**
```json
{ "accessToken": "", "refreshToken": "", "tokenType": "Bearer", "expiresIn": 86400000, "username": "", "email": "", "role": "" }
```

**Product:**
```json
{ "id": 1, "name": "", "sku": "", "category": "", "quantity": 0, "price": 0.00, "imageUrl": null, "description": null, "lowStock": false, "createdAt": "", "updatedAt": "" }
```

**Order:**
```json
{ "id": 1, "orderNumber": "ORD-...", "username": "", "status": "PENDING", "totalAmount": 0.00, "shippingAddress": null, "notes": null, "items": [ { "id": 1, "productId": 1, "productName": "", "productSku": "", "quantity": 1, "unitPrice": 0.00, "totalPrice": 0.00 } ], "createdAt": "", "updatedAt": "" }
```

**User (admin view):**
```json
{ "id": 1, "username": "", "email": "", "fullName": "", "role": "STAFF", "active": true, "createdAt": "" }
```

**AuditLog:**
```json
{ "id": 1, "username": "", "actionType": "", "entityType": "", "entityId": "", "details": "", "ipAddress": null, "timestamp": "" }
```

### Error Response Formats

**Validation error (400):**
```json
{ "success": false, "message": "Validation failed", "errors": { "username": "Username is required", "email": "Invalid email format" } }
```

**Single error:**
```json
{ "success": false, "message": "Product not found with id: '99'" }
```

**HTTP Status meanings:**
- `400` — Validation failed or bad input
- `401` — Not authenticated (missing/expired token)
- `403` — Authenticated but wrong role
- `404` — Resource doesn't exist
- `409` — Conflict (insufficient stock, FK violation)
- `413` — File too large (>10MB)
- `500` — Server error

---

## 13. Troubleshooting

### Error: `function lower(bytea) does not exist` on product search

**Root cause:** Hibernate 6 passes null String parameters to PostgreSQL as untyped binary (`bytea`). PostgreSQL can't run text functions on binary types.

**Fix:** In `ProductService.getAllProducts()`, convert null to empty string:
```java
String searchName = (name != null && !name.isBlank()) ? name : "";
String searchCategory = (category != null && !category.isBlank()) ? category : "";
```
And in `ProductRepository.searchProducts()`, use `= ''` instead of `IS NULL`:
```sql
(:name = '' OR LOWER(p.name) LIKE LOWER(CONCAT('%', :name, '%')))
```

### Error: `NoSuchMethodError: ControllerAdviceBean.<init>(Object)` — Swagger 500

**Root cause:** `springdoc-openapi 2.5.0` calls a `ControllerAdviceBean` constructor removed in Spring Framework 6.2.x (Spring Boot 3.5.0).

**Fix:** Upgrade in `pom.xml`:
```xml
<springdoc.version>2.8.6</springdoc.version>
```

### Error: SQS 403 `sqs:ReceiveMessage` on startup

**Root cause:** The IAM user lacks SQS permissions.

**Fix options:**
1. Add `SmartInventorySQSAccess` policy to the IAM user (permanent)
2. Disable SQS until permissions are granted: `AWS_SQS_ENABLED=false`

### Error: `DataIntegrityViolationException` not caught by `GlobalExceptionHandler`

**Root cause:** Spring Security's `ExceptionTranslationFilter` intercepts some exceptions before `@RestControllerAdvice` can. Also, Hibernate only sends SQL at transaction commit — after the method returns — so the exception may escape.

**Fix:** In `ProductService.deleteProduct()`, catch the exception directly and call `flush()` to force immediate SQL:
```java
try {
    productRepository.delete(product);
    productRepository.flush();
} catch (DataIntegrityViolationException e) {
    throw new IllegalArgumentException("Cannot delete '" + product.getName() + "' — it is referenced by existing orders");
}
```

### Error: `compile: not a repeatable annotation interface` for `@Transactional`

**Root cause:** Method already had `@Transactional` and another was added.

**Fix:** Remove the duplicate. Java annotations are not repeatable by default.

### Error: Railway deploy succeeds but app crashes on startup with `HikariPool` errors

**Root cause:** Database connection failed — wrong credentials or host.

**Fix:** Check Railway Variables for the Postgres service. Verify `PGHOST`, `PGPORT`, `PGDATABASE`, `PGUSER`, `PGPASSWORD` are set on the app service (not just the Postgres service). Railway auto-injects these when services are linked, but confirm the app service shows them in Variables.

### Error: Multi-stage Dockerfile needed for Railway

**Root cause:** Railway builds from git context (respects `.gitignore`). `target/` and `*.jar` are gitignored, so `COPY target/smartinventory.jar` fails in a single-stage build.

**Fix:** Multi-stage Dockerfile — build the JAR inside Docker:
```dockerfile
FROM maven:3.9-eclipse-temurin-21-alpine AS builder
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -q
COPY src ./src
RUN mvn clean package -DskipTests -q

FROM eclipse-temurin:21-jre-alpine
COPY --from=builder /app/target/smartinventory.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","/app.jar"]
```

### Error: SES email fails — address not verified

**Root cause:** SES sandbox mode requires both sender and recipient to be verified.

**Fix:** Verify the recipient's email in AWS Console → SES → Verified identities. Or request production access to send to any address.

### Error: `countByProductId` JPQL returning boolean type fails

**Root cause:** `SELECT COUNT(oi) > 0` is not valid JPQL syntax for a boolean return type.

**Fix:** Return `long` and use `SELECT COUNT(oi)`:
```java
@Query("SELECT COUNT(oi) FROM OrderItem oi WHERE oi.product.id = :productId")
long countByProductId(@Param("productId") Long productId);
```

### Error: CloudWatch `AccessDenied` after adding policy

**Root cause:** IAM policy changes have propagation delay (usually 30-60 seconds).

**Fix:** Wait 1-2 minutes after attaching the policy before testing.

### Error: `PUT /api/orders/{id}/status` returns "Required request parameter 'status' not present"

**Root cause:** Passing status as a JSON body (`-d '"CONFIRMED"'`) instead of a query parameter.

**Fix:** Pass as query parameter:
```bash
curl -X PUT "https://.../api/orders/1/status?status=PROCESSING"
```

### Error: `OrderStatus` enum conversion fails with "CONFIRMED"

**Root cause:** `CONFIRMED` is not a valid `OrderStatus`. Valid values are `PENDING`, `PROCESSING`, `COMPLETED`, `CANCELLED`.

---

## 14. Future Improvements

### High Priority

**Optimistic Locking for Stock**
Add `@Version private Integer version` to `Product`. Hibernate will add a `version` column and use it in `UPDATE WHERE version = ?`. Concurrent orders for the same product will get `OptimisticLockException` instead of silently overselling. Retry with backoff or return 409 to the client.

**Transactional Outbox for SQS**
Currently, if SQS publish fails after order commit, the order is stuck in PENDING. Replace direct SQS publish with: write to an `outbox_events` table in the same transaction as the order, then run a scheduled job that reads unprocessed events, publishes to SQS, and marks them done. This guarantees at-least-once delivery.

**Password Reset Flow**
`SESService.sendPasswordResetEmail()` exists but is not wired to any endpoint. Add `POST /api/auth/forgot-password` and `POST /api/auth/reset-password` with secure token generation and expiry.

**Refresh Token Rotation Validation**
Currently any valid refresh token works. Add refresh token family tracking to detect token reuse (if a stolen refresh token is used, invalidate the entire token family).

### Medium Priority

**Flyway/Liquibase Migrations**
Replace `ddl-auto=update` with proper schema migrations. `update` is fragile in production — it won't remove columns or constraints, and the evolution history is invisible.

**Pagination on Audit Logs with Filters**
Currently `GET /api/admin/audit-logs` only supports generic pagination. Add query params: `?username=johndoe&actionType=ORDER_PLACED&startDate=2026-01-01&endDate=2026-12-31`.

**Product Quantity History**
Track every quantity change (reason, amount, actor) in a `stock_movements` table. Enables inventory audit trails and demand forecasting.

**Rate Limiting**
Add `bucket4j` or Spring Cloud Gateway rate limiting on auth endpoints to prevent brute-force login attacks. Especially important for `/api/auth/login`.

**Email Templates with Thymeleaf**
Replace string-concatenated HTML emails in `SESService` with Thymeleaf templates. Easier to maintain and supports proper styling.

### Lower Priority

**Unit and Integration Tests**
Add JUnit 5 tests with Mockito for services, and Spring Boot integration tests with Testcontainers (PostgreSQL) for repositories and controllers. Current project has no tests.

**Docker Compose for Local Dev**
Add `docker-compose.yml` to spin up PostgreSQL + the app locally, eliminating the need for H2 and making local behavior match production.

**Frontend Application**
Build a React (TypeScript) frontend consuming this API. The `FRONTEND_GUIDE.md` in this repo documents all contracts. Priority screens: Login, Product List, Add Product, Orders, Reports dashboard.

**Admin Dashboard**
Dedicated admin UI for user management and audit log viewing. Could be a separate service or embedded in the React app behind role checks.

### How to Scale

**Horizontal Scaling (multiple instances)**

JWT is stateless — any instance can validate any token. PostgreSQL handles concurrent connections. The main concern: SQS listeners would each receive and process messages independently — ensure SQS queue visibility timeout > processing time and implement idempotency on the listener side.

**Caching**

Add Redis with Spring Cache (`@Cacheable`) on frequently-read, rarely-changed data: product categories, product listings. Invalidate cache on product create/update.

**Database Scaling**

Railway supports read replicas for PostgreSQL (on paid plans). Configure a read-only DataSource for report queries (JPA supports multiple data sources).

**File Storage Optimization**

Add image resizing on upload (Thumbnailator library) to reduce S3 storage costs and speed up image loading. Or use CloudFront CDN in front of S3.

**Observability**

Add distributed tracing with OpenTelemetry + Jaeger. Add Prometheus metrics endpoint and Grafana dashboard. Currently only 4 manual CloudWatch metrics — Spring Boot Actuator exposes JVM, HTTP, and connection pool metrics automatically.

---

*This document covers the entire SmartInventory backend as of June 2026. For the latest code, see [github.com/tm1206/smartinventory](https://github.com/tm1206/smartinventory).*
