# SmartInventory — Frontend API Reference

**Base URL:** `https://smartinventory-production-2890.up.railway.app`

**Interactive Docs:** `https://smartinventory-production-2890.up.railway.app/swagger-ui/index.html`

---

## Table of Contents

1. [Authentication Flow](#authentication-flow)
2. [Request Conventions](#request-conventions)
3. [Response Envelope](#response-envelope)
4. [Error Responses](#error-responses)
5. [Enums & Constants](#enums--constants)
6. [Data Models](#data-models)
7. [API Endpoints](#api-endpoints)
   - [Auth](#auth-endpoints)
   - [Products](#product-endpoints)
   - [Orders](#order-endpoints)
   - [Reports](#report-endpoints)
   - [Admin](#admin-endpoints)

---

## Authentication Flow

### 1. Register

```http
POST /api/auth/register
```

```json
{
  "username": "johndoe",
  "email": "john@example.com",
  "password": "secret123",
  "fullName": "John Doe",
  "role": "STAFF"
}
```

Returns an `accessToken` and `refreshToken` immediately — no separate login step needed after registration.

### 2. Login

```http
POST /api/auth/login
```

```json
{
  "username": "johndoe",
  "password": "secret123"
}
```

Store both `accessToken` and `refreshToken` from the response.

### 3. Use the Token

Send the access token in the `Authorization` header on every protected request:

```
Authorization: Bearer <accessToken>
```

### 4. Refresh the Token

Access tokens expire in **24 hours** (`expiresIn` is in milliseconds). When a request returns `401`, refresh:

```http
POST /api/auth/refresh
```

```json
{
  "refreshToken": "<refreshToken>"
}
```

Returns a new `accessToken` and `refreshToken`. Discard the old pair and store the new one.

### 5. Logout

```http
POST /api/auth/logout
Authorization: Bearer <accessToken>
```

Invalidates the refresh token server-side. No request body required.

---

## Request Conventions

| Header | Value |
|---|---|
| `Content-Type` | `application/json` (all POST/PUT with a body) |
| `Authorization` | `Bearer <accessToken>` (all protected endpoints) |

**Pagination** — endpoints that return lists accept these query params:

| Param | Default | Description |
|---|---|---|
| `page` | `0` | Zero-based page number |
| `size` | `20` | Items per page |
| `sort` | — | Field and direction, e.g. `createdAt,desc` |

---

## Response Envelope

Every response wraps data in this envelope:

```json
{
  "success": true,
  "message": "Optional human-readable message",
  "data": { ... },
  "timestamp": "2026-06-07T10:00:00"
}
```

| Field | Type | Always present |
|---|---|---|
| `success` | `boolean` | Yes |
| `message` | `string` | No — omitted on simple success responses |
| `data` | any | No — omitted on errors |
| `timestamp` | `string (ISO-8601)` | Yes |

---

## Error Responses

All errors follow the same envelope. `data` is omitted.

### Validation error (400)

```json
{
  "success": false,
  "message": "Validation failed",
  "errors": {
    "username": "Username is required",
    "email": "Invalid email format"
  },
  "timestamp": "2026-06-07T10:00:00"
}
```

### Other errors

```json
{
  "success": false,
  "message": "Human-readable error description",
  "timestamp": "2026-06-07T10:00:00"
}
```

| HTTP Status | Meaning |
|---|---|
| `400` | Validation failed or bad request (e.g. duplicate SKU) |
| `401` | Missing, invalid, or expired token |
| `403` | Authenticated but insufficient role |
| `404` | Resource not found |
| `409` | Conflict (e.g. insufficient stock, FK constraint) |
| `413` | File upload too large |
| `500` | Unexpected server error |

---

## Enums & Constants

### Role

Assigned at registration. Invalid values fall back to `STAFF`.

| Value | Description |
|---|---|
| `ADMIN` | Full access — all endpoints including admin panel |
| `MANAGER` | Can manage products and orders; no admin panel |
| `STAFF` | Can read products, place orders; no management access |

### OrderStatus

| Value | Description |
|---|---|
| `PENDING` | Newly placed, awaiting processing |
| `PROCESSING` | Being fulfilled |
| `COMPLETED` | Fulfilled and closed |
| `CANCELLED` | Cancelled |

---

## Data Models

### AuthResponse

Returned by register, login, and refresh.

```json
{
  "accessToken": "eyJhbGci...",
  "refreshToken": "eyJhbGci...",
  "tokenType": "Bearer",
  "expiresIn": 86400000,
  "username": "johndoe",
  "email": "john@example.com",
  "role": "STAFF"
}
```

| Field | Type | Notes |
|---|---|---|
| `accessToken` | `string` | JWT — include as `Bearer` in `Authorization` header |
| `refreshToken` | `string` | JWT — store securely; used to get new access tokens |
| `tokenType` | `string` | Always `"Bearer"` |
| `expiresIn` | `number` | Access token TTL in **milliseconds** (86400000 = 24 h) |
| `username` | `string` | |
| `email` | `string` | |
| `role` | `string` | `ADMIN` \| `MANAGER` \| `STAFF` |

### User

```json
{
  "id": 1,
  "username": "johndoe",
  "email": "john@example.com",
  "fullName": "John Doe",
  "role": "STAFF",
  "active": true,
  "createdAt": "2026-06-07T10:00:00"
}
```

| Field | Type | Notes |
|---|---|---|
| `id` | `number` | |
| `username` | `string` | Unique |
| `email` | `string` | Unique |
| `fullName` | `string` | |
| `role` | `string` | See [Role](#role) |
| `active` | `boolean` | `false` = deactivated account |
| `createdAt` | `string (ISO-8601)` | |

### Product

```json
{
  "id": 1,
  "name": "Laptop Pro X",
  "sku": "LAP-001",
  "category": "Electronics",
  "quantity": 25,
  "price": 1299.99,
  "imageUrl": "https://s3.amazonaws.com/...",
  "description": "High-performance laptop",
  "lowStock": false,
  "createdAt": "2026-06-07T10:00:00",
  "updatedAt": "2026-06-07T12:00:00"
}
```

| Field | Type | Notes |
|---|---|---|
| `id` | `number` | |
| `name` | `string` | |
| `sku` | `string` | Unique stock-keeping unit |
| `category` | `string` | |
| `quantity` | `number` | Current stock level |
| `price` | `number` | Decimal, e.g. `1299.99` |
| `imageUrl` | `string \| null` | S3 URL if image uploaded |
| `description` | `string \| null` | |
| `lowStock` | `boolean` | `true` when `quantity < 10` |
| `createdAt` | `string (ISO-8601)` | |
| `updatedAt` | `string (ISO-8601)` | |

### Order

```json
{
  "id": 1,
  "orderNumber": "ORD-20260607-001",
  "username": "johndoe",
  "status": "PENDING",
  "totalAmount": 2599.98,
  "shippingAddress": "123 Main St",
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
    }
  ],
  "createdAt": "2026-06-07T10:00:00",
  "updatedAt": "2026-06-07T10:00:00"
}
```

**Order fields:**

| Field | Type | Notes |
|---|---|---|
| `id` | `number` | |
| `orderNumber` | `string` | Human-readable unique identifier |
| `username` | `string` | The user who placed the order |
| `status` | `string` | See [OrderStatus](#orderstatus) |
| `totalAmount` | `number` | Sum of all item totals |
| `shippingAddress` | `string \| null` | |
| `notes` | `string \| null` | |
| `items` | `OrderItem[]` | |
| `createdAt` | `string (ISO-8601)` | |
| `updatedAt` | `string (ISO-8601)` | |

**OrderItem fields:**

| Field | Type | Notes |
|---|---|---|
| `id` | `number` | |
| `productId` | `number` | |
| `productName` | `string` | Snapshot at time of order |
| `productSku` | `string` | Snapshot at time of order |
| `quantity` | `number` | |
| `unitPrice` | `number` | Snapshot at time of order |
| `totalPrice` | `number` | `unitPrice × quantity` |

### AuditLog

```json
{
  "id": 1,
  "username": "johndoe",
  "actionType": "PRODUCT_CREATED",
  "entityType": "Product",
  "entityId": "42",
  "details": "Product created: Laptop Pro X",
  "ipAddress": "1.2.3.4",
  "timestamp": "2026-06-07T10:00:00"
}
```

| Field | Type | Notes |
|---|---|---|
| `id` | `number` | |
| `username` | `string` | Actor |
| `actionType` | `string` | e.g. `USER_LOGIN`, `PRODUCT_CREATED`, `ORDER_STATUS_UPDATED` |
| `entityType` | `string` | e.g. `Product`, `Order`, `User` |
| `entityId` | `string` | ID of affected entity |
| `details` | `string` | Human-readable description |
| `ipAddress` | `string \| null` | |
| `timestamp` | `string (ISO-8601)` | |

---

## API Endpoints

### Auth Endpoints

All auth endpoints are **public** — no token required.

---

#### POST /api/auth/register

Register a new user. Returns tokens immediately.

**Request body:**

```json
{
  "username": "johndoe",
  "email": "john@example.com",
  "password": "secret123",
  "fullName": "John Doe",
  "role": "STAFF"
}
```

| Field | Type | Required | Constraints |
|---|---|---|---|
| `username` | `string` | Yes | 3–50 characters, unique |
| `email` | `string` | Yes | Valid email format, unique |
| `password` | `string` | Yes | Min 6 characters |
| `fullName` | `string` | Yes | |
| `role` | `string` | No | `ADMIN` \| `MANAGER` \| `STAFF`; defaults to `STAFF` if omitted or invalid |

**Response — 201 Created:**

```json
{
  "success": true,
  "message": "User registered successfully",
  "data": { "<AuthResponse>" }
}
```

---

#### POST /api/auth/login

Authenticate and receive tokens.

**Request body:**

```json
{
  "username": "johndoe",
  "password": "secret123"
}
```

**Response — 200 OK:**

```json
{
  "success": true,
  "message": "Login successful",
  "data": { "<AuthResponse>" }
}
```

**Error — 401:**

```json
{
  "success": false,
  "message": "Invalid username or password"
}
```

---

#### POST /api/auth/refresh

Exchange a refresh token for a new access token + refresh token pair. Both old tokens become invalid.

**Request body:**

```json
{
  "refreshToken": "eyJhbGci..."
}
```

**Response — 200 OK:**

```json
{
  "success": true,
  "message": "Token refreshed",
  "data": { "<AuthResponse>" }
}
```

---

#### POST /api/auth/logout

Invalidates the current user's refresh token.

| | |
|---|---|
| **Auth required** | Yes |
| **Request body** | None |

**Response — 200 OK:**

```json
{
  "success": true,
  "message": "Logged out successfully"
}
```

---

### Product Endpoints

| | |
|---|---|
| **Auth required** | Yes (except where noted) |

---

#### GET /api/products

List all products with optional search and pagination.

| | |
|---|---|
| **Auth required** | Yes |
| **Role required** | Any authenticated user |

**Query parameters:**

| Param | Type | Description |
|---|---|---|
| `name` | `string` | Case-insensitive partial match on product name |
| `category` | `string` | Exact match on category |
| `page` | `number` | Page number (0-based, default `0`) |
| `size` | `number` | Page size (default `20`) |
| `sort` | `string` | e.g. `price,desc` or `name,asc` |

**Response — 200 OK:**

```json
{
  "success": true,
  "data": {
    "content": [ { "<Product>" } ],
    "totalElements": 42,
    "totalPages": 3,
    "size": 20,
    "number": 0,
    "first": true,
    "last": false
  }
}
```

---

#### GET /api/products/{id}

Get a single product by its database ID.

| | |
|---|---|
| **Auth required** | Yes |
| **Role required** | Any authenticated user |

**Response — 200 OK:**

```json
{
  "success": true,
  "data": { "<Product>" }
}
```

**Error — 404:**

```json
{
  "success": false,
  "message": "Product not found with id: 99"
}
```

---

#### GET /api/products/sku/{sku}

Get a single product by its SKU.

| | |
|---|---|
| **Auth required** | Yes |
| **Role required** | Any authenticated user |

**Response — 200 OK:**

```json
{
  "success": true,
  "data": { "<Product>" }
}
```

---

#### GET /api/products/categories

Get the list of all distinct categories in the system.

| | |
|---|---|
| **Auth required** | Yes |
| **Role required** | Any authenticated user |

**Response — 200 OK:**

```json
{
  "success": true,
  "data": ["Electronics", "Furniture", "Office Supplies"]
}
```

---

#### GET /api/products/low-stock

Get all products with quantity below 10.

| | |
|---|---|
| **Auth required** | Yes |
| **Role required** | `ADMIN`, `MANAGER` |

**Response — 200 OK:**

```json
{
  "success": true,
  "data": [ { "<Product>" } ]
}
```

---

#### POST /api/products

Create a new product.

| | |
|---|---|
| **Auth required** | Yes |
| **Role required** | `ADMIN`, `MANAGER` |

**Request body:**

```json
{
  "name": "Laptop Pro X",
  "sku": "LAP-001",
  "category": "Electronics",
  "quantity": 50,
  "price": 1299.99,
  "imageUrl": "https://example.com/image.jpg",
  "description": "High-performance laptop"
}
```

| Field | Type | Required | Constraints |
|---|---|---|---|
| `name` | `string` | Yes | |
| `sku` | `string` | Yes | Unique |
| `category` | `string` | Yes | |
| `quantity` | `number` | Yes | Integer ≥ 0 |
| `price` | `number` | Yes | Decimal > 0 |
| `imageUrl` | `string` | No | |
| `description` | `string` | No | |

**Response — 201 Created:**

```json
{
  "success": true,
  "message": "Product created successfully",
  "data": { "<Product>" }
}
```

---

#### PUT /api/products/{id}

Update an existing product. All fields are replaced (not partial).

| | |
|---|---|
| **Auth required** | Yes |
| **Role required** | `ADMIN`, `MANAGER` |

**Request body:** Same structure as POST.

**Response — 200 OK:**

```json
{
  "success": true,
  "message": "Product updated successfully",
  "data": { "<Product>" }
}
```

---

#### POST /api/products/{id}/image

Upload a product image. Stores in S3 and updates `imageUrl` on the product.

| | |
|---|---|
| **Auth required** | Yes |
| **Role required** | `ADMIN`, `MANAGER` |
| **Content-Type** | `multipart/form-data` |

**Form field:**

| Field | Type | Description |
|---|---|---|
| `file` | `File` | Image file to upload |

**Response — 200 OK:**

```json
{
  "success": true,
  "message": "Image uploaded successfully",
  "data": "https://s3.amazonaws.com/smartinventory/products/uuid.jpg"
}
```

---

#### DELETE /api/products/{id}

Delete a product. Fails if the product is referenced by any existing orders.

| | |
|---|---|
| **Auth required** | Yes |
| **Role required** | `ADMIN` only |

**Response — 200 OK:**

```json
{
  "success": true,
  "message": "Product deleted successfully"
}
```

**Error — 400 (product has orders):**

```json
{
  "success": false,
  "message": "Cannot delete 'Laptop Pro X' — it is referenced by existing orders"
}
```

---

### Order Endpoints

| | |
|---|---|
| **Auth required** | Yes |

---

#### POST /api/orders

Place a new order. Deducts stock from each product immediately.

| | |
|---|---|
| **Auth required** | Yes |
| **Role required** | Any authenticated user |

**Request body:**

```json
{
  "items": [
    { "productId": 1, "quantity": 2 },
    { "productId": 3, "quantity": 1 }
  ],
  "shippingAddress": "123 Main St, Springfield",
  "notes": "Leave at the door"
}
```

| Field | Type | Required | Notes |
|---|---|---|---|
| `items` | `array` | Yes | At least one item |
| `items[].productId` | `number` | Yes | Must exist |
| `items[].quantity` | `number` | Yes | Must be > 0 and ≤ available stock |
| `shippingAddress` | `string` | No | |
| `notes` | `string` | No | |

**Response — 201 Created:**

```json
{
  "success": true,
  "message": "Order placed successfully",
  "data": { "<Order>" }
}
```

**Error — 409 (insufficient stock):**

```json
{
  "success": false,
  "message": "Insufficient stock for Laptop Pro X. Requested: 10, Available: 3"
}
```

---

#### GET /api/orders

Get all orders in the system (paginated).

| | |
|---|---|
| **Auth required** | Yes |
| **Role required** | `ADMIN`, `MANAGER` |

**Query parameters:** Standard pagination params (`page`, `size`, `sort`).

**Response — 200 OK:**

```json
{
  "success": true,
  "data": {
    "content": [ { "<Order>" } ],
    "totalElements": 100,
    "totalPages": 5,
    "size": 20,
    "number": 0
  }
}
```

---

#### GET /api/orders/my-orders

Get the orders placed by the currently logged-in user.

| | |
|---|---|
| **Auth required** | Yes |
| **Role required** | Any authenticated user |

**Query parameters:** Standard pagination params.

**Response — 200 OK:** Same paginated structure as GET /api/orders.

---

#### GET /api/orders/{id}

Get a single order by its database ID.

| | |
|---|---|
| **Auth required** | Yes |
| **Role required** | Any authenticated user |

**Response — 200 OK:**

```json
{
  "success": true,
  "data": { "<Order>" }
}
```

---

#### GET /api/orders/number/{orderNumber}

Get a single order by its human-readable order number (e.g. `ORD-20260607-001`).

| | |
|---|---|
| **Auth required** | Yes |
| **Role required** | Any authenticated user |

**Response — 200 OK:**

```json
{
  "success": true,
  "data": { "<Order>" }
}
```

---

#### PUT /api/orders/{id}/status

Update the status of an order.

| | |
|---|---|
| **Auth required** | Yes |
| **Role required** | `ADMIN`, `MANAGER` |

**Query parameter:**

| Param | Type | Required | Values |
|---|---|---|---|
| `status` | `string` | Yes | `PENDING` \| `PROCESSING` \| `COMPLETED` \| `CANCELLED` |

**Example request:**

```
PUT /api/orders/1/status?status=PROCESSING
Authorization: Bearer <token>
```

**Response — 200 OK:**

```json
{
  "success": true,
  "message": "Order status updated",
  "data": { "<Order>" }
}
```

---

### Report Endpoints

All report endpoints require `ADMIN` or `MANAGER` role.

---

#### GET /api/reports/inventory

Get a summary of the current inventory state.

| | |
|---|---|
| **Auth required** | Yes |
| **Role required** | `ADMIN`, `MANAGER` |

**Response — 200 OK:**

```json
{
  "success": true,
  "data": {
    "totalProducts": 42,
    "lowStockCount": 3,
    "totalInventoryValue": 125430.50,
    "categories": ["Electronics", "Furniture"],
    "lowStockProducts": [
      { "id": 5, "name": "Keyboard", "sku": "KEY-001", "quantity": 2 }
    ],
    "generatedAt": "2026-06-07T10:00:00"
  }
}
```

---

#### GET /api/reports/orders

Get an order summary for a date range.

| | |
|---|---|
| **Auth required** | Yes |
| **Role required** | `ADMIN`, `MANAGER` |

**Query parameters:**

| Param | Type | Required | Format |
|---|---|---|---|
| `start` | `string` | Yes | ISO-8601 datetime, e.g. `2026-01-01T00:00:00` |
| `end` | `string` | Yes | ISO-8601 datetime, e.g. `2026-06-30T23:59:59` |

**Example request:**

```
GET /api/reports/orders?start=2026-01-01T00:00:00&end=2026-06-30T23:59:59
```

**Response — 200 OK:**

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
    "dateRange": {
      "start": "2026-01-01T00:00:00",
      "end": "2026-06-30T23:59:59"
    },
    "generatedAt": "2026-06-07T10:00:00"
  }
}
```

---

#### GET /api/reports/inventory/export

Export all inventory data as a CSV file. The file is uploaded to S3 and a pre-signed download URL is returned. The URL expires after a short period.

| | |
|---|---|
| **Auth required** | Yes |
| **Role required** | `ADMIN`, `MANAGER` |

**Response — 200 OK:**

```json
{
  "success": true,
  "message": "Inventory exported successfully",
  "data": "https://s3.amazonaws.com/smartinventory/reports/inventory-20260607-100000.csv?..."
}
```

---

#### GET /api/reports/orders/export

Export orders for a date range as CSV. Returns a pre-signed S3 URL.

| | |
|---|---|
| **Auth required** | Yes |
| **Role required** | `ADMIN`, `MANAGER` |

**Query parameters:** Same `start` and `end` as GET /api/reports/orders.

**Response — 200 OK:**

```json
{
  "success": true,
  "message": "Orders exported successfully",
  "data": "https://s3.amazonaws.com/smartinventory/reports/orders-20260607-100000.csv?..."
}
```

---

### Admin Endpoints

All admin endpoints require the `ADMIN` role.

---

#### GET /api/admin/users

Get all registered users.

| | |
|---|---|
| **Auth required** | Yes |
| **Role required** | `ADMIN` |

**Response — 200 OK:**

```json
{
  "success": true,
  "data": [ { "<User>" } ]
}
```

---

#### GET /api/admin/users/{id}

Get a single user by ID.

| | |
|---|---|
| **Auth required** | Yes |
| **Role required** | `ADMIN` |

**Response — 200 OK:**

```json
{
  "success": true,
  "data": { "<User>" }
}
```

---

#### PUT /api/admin/users/{id}/role

Change a user's role.

| | |
|---|---|
| **Auth required** | Yes |
| **Role required** | `ADMIN` |

**Query parameter:**

| Param | Type | Required | Values |
|---|---|---|---|
| `role` | `string` | Yes | `ADMIN` \| `MANAGER` \| `STAFF` |

**Example request:**

```
PUT /api/admin/users/5/role?role=MANAGER
Authorization: Bearer <token>
```

**Response — 200 OK:**

```json
{
  "success": true,
  "message": "Role updated successfully",
  "data": { "<User>" }
}
```

---

#### PUT /api/admin/users/{id}/status

Activate or deactivate a user account.

| | |
|---|---|
| **Auth required** | Yes |
| **Role required** | `ADMIN` |

**Query parameter:**

| Param | Type | Required | Values |
|---|---|---|---|
| `active` | `boolean` | Yes | `true` \| `false` |

**Example request:**

```
PUT /api/admin/users/5/status?active=false
Authorization: Bearer <token>
```

**Response — 200 OK:**

```json
{
  "success": true,
  "message": "User status updated",
  "data": { "<User>" }
}
```

---

#### GET /api/admin/audit-logs

Get paginated audit logs for all system actions.

| | |
|---|---|
| **Auth required** | Yes |
| **Role required** | `ADMIN` |

**Query parameters:** Standard pagination params (`page`, `size`, `sort`).

**Response — 200 OK:**

```json
{
  "success": true,
  "data": {
    "content": [ { "<AuditLog>" } ],
    "totalElements": 500,
    "totalPages": 25,
    "size": 20,
    "number": 0
  }
}
```

---

## Public Endpoints (No Auth)

| Endpoint | Description |
|---|---|
| `GET /actuator/health` | App health check — returns `{"status":"UP"}` |
| `GET /swagger-ui/index.html` | Interactive API explorer |
| `GET /v3/api-docs` | OpenAPI 3.0 JSON spec |
| `POST /api/auth/register` | Register a new user |
| `POST /api/auth/login` | Login |
| `POST /api/auth/refresh` | Refresh access token |
