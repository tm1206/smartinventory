# SmartInventory — Inventory Management System

Spring Boot 3.5 · Java 21 · React 18 · AWS (S3, SQS, SES, CloudWatch) · H2 (local) · PostgreSQL (Railway)

**Frontend:** https://smartinventory-frontend.vercel.app  
**API / Swagger:** https://smartinventory-production-2890.up.railway.app/swagger-ui/index.html

---

## API Endpoints

### Auth  `/api/auth`
| Method | Path | Description |
|--------|------|-------------|
| POST | `/api/auth/register` | Register new user |
| POST | `/api/auth/login` | Login, get JWT tokens |
| POST | `/api/auth/refresh` | Refresh access token |
| POST | `/api/auth/logout` | Logout (invalidate refresh token) |

### Products  `/api/products`
| Method | Path | Auth | Description |
|--------|------|------|-------------|
| GET | `/api/products` | Any | List all (filterable: ?name=&category=) |
| GET | `/api/products/{id}` | Any | Get by ID |
| GET | `/api/products/sku/{sku}` | Any | Get by SKU |
| GET | `/api/products/categories` | Any | All categories |
| GET | `/api/products/low-stock` | ADMIN/MANAGER | Products with qty < 10 |
| POST | `/api/products` | ADMIN/MANAGER | Create product |
| PUT | `/api/products/{id}` | ADMIN/MANAGER | Update product |
| POST | `/api/products/{id}/image` | ADMIN/MANAGER | Upload image to S3 |
| DELETE | `/api/products/{id}` | ADMIN | Delete product |

### Orders  `/api/orders`
| Method | Path | Auth | Description |
|--------|------|------|-------------|
| POST | `/api/orders` | Any | Place order (deducts stock) |
| GET | `/api/orders` | ADMIN/MANAGER | All orders |
| GET | `/api/orders/my-orders` | Any | Current user's orders |
| GET | `/api/orders/{id}` | Any | Order by ID |
| GET | `/api/orders/number/{orderNumber}` | Any | Order by number |
| PUT | `/api/orders/{id}/status?status=` | ADMIN/MANAGER | Update order status |

### Reports  `/api/reports`  (ADMIN/MANAGER only)
| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/reports/inventory` | Stock summary + low stock |
| GET | `/api/reports/orders?start=&end=` | Orders by date range |
| GET | `/api/reports/inventory/export` | Export CSV → S3 → presigned URL |
| GET | `/api/reports/orders/export?start=&end=` | Export orders CSV → S3 → presigned URL |

### Admin  `/api/admin`  (ADMIN only)
| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/admin/users` | All users |
| GET | `/api/admin/users/{id}` | User by ID |
| PUT | `/api/admin/users/{id}/role?role=` | Update user role |
| PUT | `/api/admin/users/{id}/status?active=` | Activate/deactivate user |
| PUT | `/api/admin/users/{id}/password?password=` | Reset user password (BCrypt encoded) |
| GET | `/api/admin/audit-logs` | Paginated audit logs |

### Other
| Path | Description |
|------|-------------|
| GET `/actuator/health` | Health check (public) |
| GET `/swagger-ui/index.html` | Swagger UI |
| GET `/v3/api-docs` | OpenAPI JSON |
| GET `/h2-console` | H2 browser console (local only) |

---

## Roles
- **ADMIN** — full access
- **MANAGER** — products + orders + reports
- **STAFF** — place orders, view products

---

## Local Dev

```bash
mvn spring-boot:run
# App: http://localhost:8080
# Swagger: http://localhost:8080/swagger-ui/index.html
# H2 Console: http://localhost:8080/h2-console
```

No AWS credentials needed locally — all AWS calls fail gracefully.

---

## Environment Variables (Production)

```
AWS_ACCESS_KEY=<your IAM access key>
AWS_SECRET_KEY=<your IAM secret key>
AWS_REGION=ap-south-1
S3_BUCKET_NAME=smartinventory-uploads
SQS_ORDER_QUEUE_URL=https://sqs.ap-south-1.amazonaws.com/<account>/order-events
SQS_STOCK_QUEUE_URL=https://sqs.ap-south-1.amazonaws.com/<account>/stock-alerts
SES_FROM_EMAIL=noreply@yourdomain.com
SES_SMTP_USERNAME=<SES SMTP username>
SES_SMTP_PASSWORD=<SES SMTP password>
CLOUDWATCH_LOG_GROUP=smartinventory-logs
JWT_SECRET=<base64 encoded secret, min 64 chars>
SPRING_PROFILES_ACTIVE=prod
DATABASE_URL=postgresql://<host>:<port>/<db>
```

---

## AWS Console Setup

### 1. S3 Bucket
1. Go to S3 → Create bucket
2. Name: `smartinventory-uploads`
3. Region: `ap-south-1`
4. Block all public access: ON (presigned URLs handle access)
5. Versioning: optional

### 2. SQS Queues
Create two Standard queues:
1. `order-events` — Visibility timeout: 30s, Message retention: 4 days
2. `stock-alerts` — Visibility timeout: 30s, Message retention: 4 days

Copy the Queue URLs and set as `SQS_ORDER_QUEUE_URL` and `SQS_STOCK_QUEUE_URL`.

### 3. SES Email Setup
1. Go to SES → Verified identities → Create identity
2. Choose **Email address** and verify `noreply@yourdomain.com`
3. Click the verification link in your inbox
4. For SMTP credentials: SES → SMTP settings → Create SMTP credentials
5. Copy the SMTP username and password for env vars

### 4. IAM User
1. IAM → Users → Create user (name: `smartinventory-app`)
2. Attach these inline policies:

**S3 Policy:**
```json
{
  "Effect": "Allow",
  "Action": ["s3:PutObject","s3:GetObject","s3:DeleteObject","s3:GetObjectAttributes"],
  "Resource": "arn:aws:s3:::smartinventory-uploads/*"
}
```

**SQS Policy:**
```json
{
  "Effect": "Allow",
  "Action": ["sqs:SendMessage","sqs:ReceiveMessage","sqs:DeleteMessage","sqs:GetQueueAttributes"],
  "Resource": [
    "arn:aws:sqs:ap-south-1:<account>:order-events",
    "arn:aws:sqs:ap-south-1:<account>:stock-alerts"
  ]
}
```

**CloudWatch Policy:**
```json
{
  "Effect": "Allow",
  "Action": ["cloudwatch:PutMetricData","logs:CreateLogGroup","logs:CreateLogStream","logs:PutLogEvents"],
  "Resource": "*"
}
```

3. Create Access Key → save `AWS_ACCESS_KEY` and `AWS_SECRET_KEY`

### 5. Billing Alert ($1 Budget)
1. Billing → Budgets → Create budget
2. Budget type: Cost
3. Amount: $1.00
4. Alert: 80% of actual cost → email notification

---

## Railway Deployment

```bash
# 1. Build the JAR
mvn clean package -DskipTests

# 2. Install Railway CLI
brew install railway

# 3. Login and init
railway login
railway init

# 4. Add all environment variables in Railway dashboard
# (Settings → Variables → paste all env vars from above)

# 5. Deploy
railway up

# Or connect GitHub for auto-deploy on push
```

The `Dockerfile` is already configured:
```dockerfile
FROM eclipse-temurin:21-jdk-alpine
COPY target/smartinventory.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","/app.jar"]
```

Railway auto-detects the Dockerfile and builds + deploys automatically.
Set `SPRING_PROFILES_ACTIVE=prod` in Railway variables to activate PostgreSQL + AWS config.
