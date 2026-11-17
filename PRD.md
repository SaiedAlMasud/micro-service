Microservice Project Documentation
Services: User Service, Product Service, Cart Service Communication: Synchronous REST (HTTP + JSON) only. No message broker or async communication in this version. Team: 2 collaborators working in parallel, integrating later.
1. Overview
This project is an e-commerce style backend split into three independent services. Each service owns its own code, its own database and its own API. Services never read each other's database; they only talk through REST APIs.
Service
Responsibility
Port (suggested)
Database
User Service
Register, login, user profile, issue JWT token
8081
user_db
Product Service
Product catalog: name, category, price, stock, etc.
8082
product_db
Cart Service
A user's cart and cart items
8083
cart_db

Goals
Each service can be developed, run, tested and deployed independently.
Clear API contracts so two developers can work without blocking each other.
Simple synchronous communication that is easy to debug.
Non-goals (for now)
Message queues, events, Kafka/RabbitMQ (async communication).
Orders, payments, notifications (can be added later as new services).
2. Architecture
                    Client (Web / Mobile / Postman)
                                |
                                | HTTP (JSON)
          ----------------------------------------------
          |                     |                      |
   +--------------+      +---------------+      +--------------+
   | User Service |      |Product Service|      | Cart Service |
   |   :8081      |      |    :8082      |      |    :8083     |
   +--------------+      +---------------+      +--------------+
          |                     ^                      |
       user_db             product_db              cart_db
                                |                      |
                                +---- REST call -------+
                              (Cart -> Product: GET /products/{id})

   JWT issued by User Service, validated locally by Product and Cart.
Key principles
Database per service. No shared tables, no cross-database foreign keys.
Reference by ID only. Cart stores userId and productId as plain values, not as foreign keys.
API contract first. Agree on endpoints and JSON shapes before coding (see section 8).
Stateless services. Authentication uses a JWT sent in each request.
Optional later: an API Gateway as the single entry point for clients. Not required in v1; clients can call each service directly.
3. Entities and Fields
All IDs can be UUID (recommended for microservices) or auto-increment BIGINT. Pick one and use it in all services.
3.1 User Service (user_db)
Entity: User
Field
Type
Constraints
Notes
id
UUID
PK


fullName
String
not null


email
String
unique, not null
used for login
passwordHash
String
not null
BCrypt hash, never plain text
phone
String
nullable


role
Enum
not null, default USER
USER, ADMIN
status
Enum
default ACTIVE
ACTIVE, BLOCKED
createdAt
Timestamp
not null


updatedAt
Timestamp
not null



Entity: Address (optional, can be added later)
Field
Type
Notes
id
UUID
PK
userId
UUID
FK to User (same DB, so allowed)
line1, line2, city, postalCode, country
String


isDefault
Boolean



3.2 Product Service (product_db)
Entity: Category
Field
Type
Constraints
id
UUID
PK
name
String
unique, not null
description
String
nullable

Entity: Product
Field
Type
Constraints
Notes
id
UUID
PK


name
String
not null


description
Text
nullable


categoryId
UUID
FK to Category
same DB
brand
String
nullable


price
Decimal(10,2)
not null, > 0


currency
String
default BDT


stockQuantity
Integer
>= 0


imageUrl
String
nullable


status
Enum
ACTIVE, INACTIVE
hide product without deleting
createdAt / updatedAt
Timestamp





3.3 Cart Service (cart_db)
Entity: Cart
Field
Type
Constraints
Notes
id
UUID
PK


userId
UUID
not null; unique among ACTIVE carts
plain value. At most one ACTIVE cart per user (partial unique index, see 13.5)
status
Enum
ACTIVE, CHECKED_OUT


createdAt / updatedAt
Timestamp





Entity: CartItem
Field
Type
Constraints
Notes
id
UUID
PK


cartId
UUID
FK to Cart
same DB
productId
UUID
not null
plain value, from Product Service
productName
String


snapshot at time of adding
unitPrice
Decimal(10,2)


snapshot at time of adding
quantity
Integer
>= 1


addedAt
Timestamp





Why snapshot name and price? If the Product Service is temporarily down, the cart can still be displayed. The price is re-verified from the Product Service when the cart is viewed or at checkout.
Unique constraint: (cartId, productId) so the same product is one row with a quantity.
4. REST API Specification
Base path pattern: /api/v1/.... All requests and responses are JSON.
4.1 User Service
Method
Endpoint
Auth
Description
POST
/api/v1/auth/register
public
Create account
POST
/api/v1/auth/login
public
Returns JWT access token
GET
/api/v1/users/me
JWT
Current user profile
PUT
/api/v1/users/me
JWT
Update profile
GET
/api/v1/users/{id}
JWT (ADMIN only)
Get user by ID (admin tool; no service calls it in v1)

Register request
{ "fullName": "Rahim Uddin", "email": "rahim@mail.com", "password": "Secret@123", "phone": "017XXXXXXXX" }
Login response
{ "accessToken": "<jwt>", "tokenType": "Bearer", "expiresIn": 3600,
  "user": { "id": "uuid", "fullName": "Rahim Uddin", "role": "USER" } }
4.2 Product Service
Method
Endpoint
Auth
Description
GET
/api/v1/products
public
List with pagination, filter by category, search
GET
/api/v1/products/{id}
public
Product details (used by Cart Service)
POST
/api/v1/products/bulk
public
Get many products by ID list { "ids": [...] } (max 50). Used by Cart Service
POST
/api/v1/products
ADMIN
Create product
PUT
/api/v1/products/{id}
ADMIN
Update product
DELETE
/api/v1/products/{id}
ADMIN
Soft delete (status INACTIVE)
GET
/api/v1/categories
public
List categories
POST
/api/v1/categories
ADMIN
Create category

Query example: GET /api/v1/products?categoryId=...&q=phone&page=0&size=20
Visibility rules. GET /products (list) returns only ACTIVE products. GET /products/{id} and POST /products/bulk return a product with its status even when INACTIVE, so Cart Service can show it as unavailable. Both are public and read-only, so Cart Service does not need to forward a token. The bulk endpoint uses POST only because the list of IDs goes in the body.
Bulk request and response (part of the contract Cart depends on)
POST /api/v1/products/bulk
{ "ids": ["uuid-1", "uuid-2"] }

200 OK
{ "items": [ { "id": "uuid-1", "name": "Phone X", "price": 25000.00,
               "currency": "BDT", "stockQuantity": 40, "status": "ACTIVE" } ],
  "missingIds": ["uuid-2"] }
Product response
{ "id": "uuid", "name": "Phone X", "categoryId": "uuid", "price": 25000.00,
  "currency": "BDT", "stockQuantity": 40, "status": "ACTIVE" }
4.3 Cart Service
Method
Endpoint
Auth
Description
GET
/api/v1/cart
JWT
Get my cart with items and total
POST
/api/v1/cart/items
JWT
Add item { productId, quantity }
PUT
/api/v1/cart/items/{itemId}
JWT
Change quantity
DELETE
/api/v1/cart/items/{itemId}
JWT
Remove item
DELETE
/api/v1/cart
JWT
Clear cart

The user is identified from the JWT (sub claim = userId). Never accept userId from the request body.
5. Inter-Service Communication
5.1 Who calls whom
Caller
Callee
Endpoint
Why
Cart Service
Product Service
GET /api/v1/products/{id} and POST /api/v1/products/bulk
Validate product exists, is ACTIVE, has stock; fetch current name and price
Cart Service
User Service
(not needed in v1)
User identity comes from the JWT, no call required
Product Service
none




User Service
none





Keeping the dependency one-directional (Cart depends on Product, nobody depends on Cart) avoids circular calls.
5.2 Flow: Add item to cart
Client sends POST /cart/items with Authorization: Bearer <jwt> and { productId, quantity } to Cart Service.
Cart Service validates the JWT locally and extracts userId.
Cart Service calls GET {PRODUCT_SERVICE_URL}/api/v1/products/{productId}.
If 404 or status INACTIVE: return 404 PRODUCT_NOT_FOUND. If stockQuantity < quantity: return 409 INSUFFICIENT_STOCK.
Cart Service finds or creates the user's Cart, adds or updates the CartItem with the name and price snapshot.
Returns the updated cart.
5.3 Flow: View cart
Load cart items from cart_db.
Optionally call Product Service for each productId to refresh prices (or a bulk endpoint POST /api/v1/products/bulk with a list of IDs, which is better than N calls).
Return items with a priceChanged flag if the price differs from the snapshot.
5.4 Authentication across services
User Service signs the JWT on login. Claims: sub (userId), role, iat, exp.
Recommended: sign with RS256. User Service keeps the private key; Product and Cart only need the public key to verify. Simple alternative for learning: HS256 with one shared secret stored in environment variables.
Each service validates the token itself, so no call to User Service per request.
The only service-to-service call in v1 (Cart to Product) uses public, read-only endpoints, so no token is forwarded. If you later add protected internal endpoints, use an internal API key header instead of forwarding the user's token.
Role claim. Put the role in a claim named role (USER or ADMIN). Spring Security reads scope by default, not role, so each service needs a small JWT converter that maps role to the authority ROLE_<role>. Agree on this in Step 0, otherwise ADMIN endpoints will return 403 even with a valid token.
Blocked users. Tokens are validated locally, so a token issued before a user was blocked stays valid until it expires. Keep the expiry short (15 to 60 minutes). This is an accepted limitation in v1.
5.5 Failure handling (very important in synchronous calls)
Problem
Handling
Product Service slow
Set connect timeout (~2s) and read timeout (~3s) on the HTTP client
Product Service down
Return 503 SERVICE_UNAVAILABLE with a clear message; do not crash
Temporary network error
Retry GET calls 2 times with short backoff (never blindly retry POST)
Repeated failures
Add a circuit breaker later (e.g. Resilience4j in Java)

5.6 Standard error format (same in all services)
{ "timestamp": "2026-11-17T10:00:00Z", "status": 404, "code": "PRODUCT_NOT_FOUND",
  "message": "Product with id ... not found", "path": "/api/v1/cart/items" }
Common codes: VALIDATION_ERROR (400), UNAUTHORIZED (401), FORBIDDEN (403), NOT_FOUND (404), CONFLICT (409), SERVICE_UNAVAILABLE (503). Business codes used by Cart Service: PRODUCT_NOT_FOUND (404), INSUFFICIENT_STOCK (409).
Each service implements this format itself (one small @RestControllerAdvice class, copied into each service). Do not create a shared library: a shared jar ties the services' releases together, which defeats the purpose of independent services.
6. Configuration and Environment
Never hardcode other services' URLs. Use environment variables.
Service
Variable
Example
All
SERVER_PORT
8081 / 8082 / 8083
All
DB_URL, DB_USER, DB_PASSWORD
own database only
User
JWT_PRIVATE_KEY, JWT_EXPIRY


Product, Cart
JWT_PUBLIC_KEY (or JWT_SECRET)


Cart
PRODUCT_SERVICE_URL
http://localhost:8082 (local) or http://product-service:8082 (Docker)
Cart
PRODUCT_CONNECT_TIMEOUT_MS, PRODUCT_READ_TIMEOUT_MS
2000 / 3000 (see 5.5)

7. Suggested Repository Structure
Option A (recommended for two people): one Git repository per service, plus one small shared repository for docs and infrastructure.
user-service/       (own repo)
product-service/    (own repo)
cart-service/       (own repo)
platform-docs/      (own repo: this document, OpenAPI files, docker-compose.yml)
Option B: one mono-repo with folders user-service/, product-service/, cart-service/, docs/ and docker-compose.yml. Easier to start (one clone, one compose file). If each person edits only their own folders, conflicts stay rare; the risk is the shared files (compose file, docs) and accidental edits in the other person's folder. Section 14 explains the ownership rules that work for either option.
Whichever you choose, each service keeps its own README with how to run, env variables and API list.
8. How Two Collaborators Work Separately, Then Connect
Start here: the Day-0 checklist and the GitHub rules that prevent conflicts are in section 14. The libraries each service needs are in section 15.
8.1 Split of ownership (suggested)
Person
Owns
Reason
Developer A
User Service, then Cart Service
Auth is needed first; Cart is the consumer of Product
Developer B
Product Service, plus platform repo (docker-compose, OpenAPI, shared conventions)
Product is the provider that Cart calls

Because Cart (A) calls Product (B), the two of you must agree on the Product API contract early. That is the main integration point.
Rule: nobody commits directly to a repository owned by the other person. Changes go through a pull request that the owner reviews. After the first version works, swap owners for the next features (for example B adds a Cart feature, A adds a Product feature) so both of you learn both sides.
8.2 Step 0: Agree on the contract (together, about 1 to 2 hours)
Do this before anyone writes code:
Decide the ID type (UUID), date format (ISO-8601 UTC), money type (Decimal), base path (/api/v1).
Write the OpenAPI (Swagger) YAML for each service, at least for the endpoints Cart uses: GET /products/{id} and the bulk endpoint.
Decide the JWT claims and signing method, and share the public key or secret via a private channel (never commit secrets to Git).
Decide the standard error format and ports.
Commit this to the platform-docs repo. Contract changes need a pull request and approval from the other person.
8.3 Step 1: Build in isolation
Developer A (Cart, phase 3) does not wait for the real Product Service. Use a mock: WireMock, Mockoon, or a tiny Node/JSON-server that returns the agreed JSON for GET /products/{id} and POST /products/bulk. Point PRODUCT_SERVICE_URL to the mock.
Developer B (Product) tests with Postman or automated tests, and does not need the Cart Service at all.
For JWT during development, generate a test token with the agreed claims so Product and Cart can be tested without the real User Service.
Each person runs their own local database (Docker container for PostgreSQL or MySQL).
8.4 Step 2: Git workflow
Branches: main (protected, always working) and short-lived feature/<short-name> branches, one per task. A separate develop branch is not needed for a team of two.
Pull requests with at least one review from the other person.
Commit messages like feat(cart): add item to cart.
Each service has its own Dockerfile so it can be built on any machine.
Set up branch protection, .gitignore and .env.example before the first feature commit (details in section 14).
8.5 Step 3: Integration (first real connection)
Start everything with the shared docker-compose.yml in platform-docs. It builds each service from its own repository (cloned side by side, see 14.4) and starts three services, three databases and one shared network.
Inside Docker Compose, services call each other by service name (e.g. http://product-service:8082), not localhost.
Replace the mock URL with the real Product Service URL.
Run the end-to-end scenario below.
Fix mismatches (field names, status codes) by updating the OpenAPI file first, then code.
8.6 End-to-end test scenario
Register a user, then log in and copy the JWT.
As ADMIN, create a category and a product.
As the user, add the product to the cart (quantity 2). Expect 200 with a cart total.
Try adding a non-existent product. Expect 404.
Try a quantity larger than stock. Expect 409.
Stop Product Service and add an item. Expect 503, not a crash.
Call Cart with no or an expired token. Expect 401.
8.7 Suggested timeline
Phase
Work
Both / Who
1
Contract, OpenAPI, ports, JWT decision, repos
Both
2
A: User Service (register, login, JWT). B: Product Service (CRUD, list, search)
Separate
3
A: Cart Service against Product mock. B: bulk endpoint, docker-compose, tests
Separate
4
Integration through Docker Compose, end-to-end tests
Both
5
Cleanup: README, Postman collection, error handling, resilience
Both

9. Testing Strategy
Unit tests for business logic in each service.
API tests with Postman collections stored in platform-docs.
Contract check: both sides validate against the same OpenAPI file.
Integration test for Cart calling Product (use the mock in CI, the real service in Compose).
10. Security Checklist
Hash passwords with BCrypt (never store plain text).
Validate all input (email format, quantity >= 1, price > 0).
Do not return passwordHash in any response.
Check ownership: a user can only access their own cart.
Restrict create, update and delete product endpoints to ADMIN.
Keep secrets in environment variables, not in Git.
Enable CORS only for your frontend origin.
11. Future Improvements
API Gateway (single entry point, routing, central JWT check).
Service discovery (instead of fixed URLs).
Order Service (checkout converts cart into an order).
Async communication (events such as ProductPriceChanged, UserDeleted) when you are ready.
Centralized logging and tracing, with a correlation ID header passed between services.
Circuit breaker, rate limiting, caching of product data.
12. Quick Reference
Question
Answer
How do services talk?
REST over HTTP, JSON
Who calls whom?
Cart calls Product. Nobody else calls anybody
How is the user identified?
JWT from User Service, verified locally in each service
Where is data stored?
Each service has its own database
How do we work separately?
Agreed OpenAPI contract, mock servers, separate repos
How do we connect later?
Docker Compose with real service URLs, then end-to-end tests
Where do we start?
Section 14: Day-0 checklist and GitHub rules
Which libraries does each service need?
Section 15

13. Database Design
This section adds the database details: schema, relationships, indexes, migrations and local setup. The SQL below uses PostgreSQL syntax; MySQL needs only small changes (for example UUID becomes CHAR(36)).
13.1 Database rules
One database per service: user_db, product_db, cart_db. Each service has its own database user, and that user can access only its own database.
Foreign keys are allowed only inside the same database (for example cart_items.cart_id to carts.id).
Across services there are no foreign keys. carts.user_id and cart_items.product_id are plain UUID columns, and the application checks them through REST calls.
Use soft delete (a status column) for users and products, so old carts do not point to rows that vanished.
All tables carry created_at and updated_at.
13.2 Relationships
user_db                      product_db                   cart_db
-------                      ----------                   -------
users 1 --- N addresses      categories 1 --- N products  carts 1 --- N cart_items

Cross-service (logical only, NOT enforced by the database):
  carts.user_id        -> users.id           (User Service)
  cart_items.product_id -> products.id       (Product Service)
13.3 user_db schema
CREATE TABLE users (
    id            UUID PRIMARY KEY,
    full_name     VARCHAR(120) NOT NULL,
    email         VARCHAR(160) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    phone         VARCHAR(30),
    role          VARCHAR(20)  NOT NULL DEFAULT 'USER',    -- USER, ADMIN
    status        VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',  -- ACTIVE, BLOCKED
    created_at    TIMESTAMP    NOT NULL DEFAULT now(),
    updated_at    TIMESTAMP    NOT NULL DEFAULT now()
);

CREATE TABLE addresses (
    id          UUID PRIMARY KEY,
    user_id     UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    line1       VARCHAR(200) NOT NULL,
    line2       VARCHAR(200),
    city        VARCHAR(80)  NOT NULL,
    postal_code VARCHAR(20),
    country     VARCHAR(80)  NOT NULL,
    is_default  BOOLEAN NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_addresses_user_id ON addresses(user_id);
13.4 product_db schema
CREATE TABLE categories (
    id          UUID PRIMARY KEY,
    name        VARCHAR(100) NOT NULL UNIQUE,
    description VARCHAR(255),
    created_at  TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE products (
    id             UUID PRIMARY KEY,
    name           VARCHAR(200) NOT NULL,
    description    TEXT,
    category_id    UUID NOT NULL REFERENCES categories(id),
    brand          VARCHAR(100),
    price          NUMERIC(10,2) NOT NULL CHECK (price > 0),
    currency       VARCHAR(3) NOT NULL DEFAULT 'BDT',
    stock_quantity INTEGER NOT NULL DEFAULT 0 CHECK (stock_quantity >= 0),
    image_url      VARCHAR(500),
    status         VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',  -- ACTIVE, INACTIVE
    created_at     TIMESTAMP NOT NULL DEFAULT now(),
    updated_at     TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_products_category ON products(category_id);
CREATE INDEX idx_products_status   ON products(status);
CREATE INDEX idx_products_name     ON products(name);
13.5 cart_db schema
CREATE TABLE carts (
    id         UUID PRIMARY KEY,
    user_id    UUID NOT NULL,                       -- logical link to User Service, no FK
    status     VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',  -- ACTIVE, CHECKED_OUT
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now()
);

-- only one ACTIVE cart per user
CREATE UNIQUE INDEX uq_active_cart_per_user ON carts(user_id) WHERE status = 'ACTIVE';

CREATE TABLE cart_items (
    id           UUID PRIMARY KEY,
    cart_id      UUID NOT NULL REFERENCES carts(id) ON DELETE CASCADE,
    product_id   UUID NOT NULL,                     -- logical link to Product Service, no FK
    product_name VARCHAR(200) NOT NULL,             -- snapshot
    unit_price   NUMERIC(10,2) NOT NULL,            -- snapshot
    quantity     INTEGER NOT NULL CHECK (quantity >= 1),
    added_at     TIMESTAMP NOT NULL DEFAULT now(),
    UNIQUE (cart_id, product_id)
);

CREATE INDEX idx_cart_items_cart_id ON cart_items(cart_id);
The cart total is not stored. Calculate it as the sum of unit_price * quantity when the cart is read, so it can never go out of sync.
13.6 Keeping data consistent without cross-database foreign keys
Situation
What to do
Product is deleted or set INACTIVE
Product Service never hard-deletes. Cart shows the item as unavailable when it re-checks the product.
User is deleted or BLOCKED
User Service never hard-deletes in v1. A blocked user cannot log in again. Tokens already issued stay valid until they expire (see 5.4), so keep the expiry short.
Product price changes
Cart keeps the snapshot price but re-checks on view and at checkout, and returns a priceChanged flag.
Stock changes after adding to cart
Stock is checked again when the item is added and again at checkout. Cart does not reserve stock in v1.
Orphan rows (bad productId)
Prevented by validating the product through the Product API before inserting.

13.7 Migrations and seed data
Do not let the framework auto-create tables in real use. Use a migration tool such as Flyway or Liquibase, with versioned files like V1__create_users.sql, V2__create_addresses.sql.
Each service keeps its own migration folder inside its own repository. Never edit an old migration after it is merged; add a new one.
Seed data (development only): one ADMIN user in User Service, 3 to 5 categories and 10 to 20 products in Product Service. Keep these in a separate seed script so they do not run in production.
13.8 Local database setup with Docker Compose
Three separate database containers keep the services truly independent (one container with three databases also works if your laptop is limited).
services:
  user-db:
    image: postgres:16
    environment:
      POSTGRES_DB: user_db
      POSTGRES_USER: user_svc
      POSTGRES_PASSWORD: ${USER_DB_PASSWORD}
    ports: ["5433:5432"]
    volumes: ["user_data:/var/lib/postgresql/data"]

  product-db:
    image: postgres:16
    environment:
      POSTGRES_DB: product_db
      POSTGRES_USER: product_svc
      POSTGRES_PASSWORD: ${PRODUCT_DB_PASSWORD}
    ports: ["5434:5432"]
    volumes: ["product_data:/var/lib/postgresql/data"]

  cart-db:
    image: postgres:16
    environment:
      POSTGRES_DB: cart_db
      POSTGRES_USER: cart_svc
      POSTGRES_PASSWORD: ${CART_DB_PASSWORD}
    ports: ["5435:5432"]
    volumes: ["cart_data:/var/lib/postgresql/data"]

volumes:
  user_data:
  product_data:
  cart_data:
Each service connects to its own database only, for example the Cart Service uses jdbc:postgresql://cart-db:5432/cart_db inside Docker, or localhost:5435 from your own machine. Passwords come from a .env file that is not committed to Git.
Each developer runs only the database for the service they own during separate development. When you integrate, the shared compose file starts all three.
The services themselves are added to this file in the integration phase. A ready snippet is in section 14.4.
14. Getting Started and GitHub Setup
This section answers three questions: what to do on day 0, how to split the work, and how to use GitHub so that the two of you never edit the same file at the same time.
14.1 Day-0 checklist (do together, about half a day)
Create a free GitHub organization (for example shop-microservices) and make both of you Owners. Equal admin rights, and all repositories live in one place. (A personal account with the other person as collaborator also works.)
Create four repositories: platform-docs, user-service, product-service, cart-service. Public repositories are the easiest for a practice project with no secrets: branch protection is free on public repositories, while on private repositories it may need a paid plan (check Settings, Branches).
In platform-docs, commit this document and do Step 0 from 8.2: ID type, dates, money, JWT claims (including the role claim), error format, ports, and the first OpenAPI files.
Generate each service skeleton at start.spring.io with the dependencies from section 15 (same Java and Spring Boot version in all three). Commit the skeleton, a README.md, .gitignore, .gitattributes (* text=auto), .env.example and the Maven wrapper directly to main as the first commit.
Then turn on branch protection for main in every repository: require a pull request, require 1 approval, block force pushes, delete branches after merge.
Create a GitHub Project (Kanban board) and one issue per task from the timeline in 8.7. Assign every issue to exactly one person.
Clone all four repositories side by side (see 14.4) and confirm that each person can run docker compose up for the databases.
14.2 Rules that prevent conflicts
One owner per repository. You push feature branches to your own repositories only. In the other person's repository you open an issue or a pull request, nothing else.
Split shared files by owner. In platform-docs each OpenAPI file and each Postman collection has one owner (table below), and the compose file has one owner, so two people almost never touch the same file.
Contract changes are always a pull request to platform-docs, approved by the other person. If Cart needs a new field from Product, A opens an issue or PR for B's OpenAPI file; B decides and merges.
Short-lived branches. One task per branch, merged within a day or two. Before opening a PR, merge or rebase the latest main into your branch and run the tests.
Small pull requests, reviewed by the other person within a day. Commit messages like feat(cart): add item to cart (conventional commits).
Never commit secrets or build output. .env, private keys, target/, .idea/ stay in .gitignore. Commit .env.example with placeholder values. If a secret is committed by mistake, change the secret; deleting the commit is not enough.
Never edit a merged Flyway migration (see 13.7). Add a new V file instead.
CODEOWNERS is not needed for two people. With "require 1 approval", the other person always reviews. (A code owner cannot approve their own pull request, so a CODEOWNERS file listing only the author would block the PR.)
14.3 Ownership map
Repository or path
Owner
Reviews
user-service (whole repo)
Developer A
Developer B
cart-service (whole repo)
Developer A
Developer B
product-service (whole repo)
Developer B
Developer A
platform-docs/openapi/user-service.yaml, cart-service.yaml
Developer A
Developer B
platform-docs/openapi/product-service.yaml
Developer B
Developer A (Cart depends on it)
platform-docs/docker-compose.yml, .env.example
Developer B
Developer A
platform-docs/postman/<service>.json
Owner of that service
The other developer
platform-docs/docs/ (this document)
Whoever edits it
The other developer

14.4 Folder layout and compose file for the services
Clone the four repositories as sibling folders. The compose file in platform-docs then builds each service from its neighbour folder:
microservices-practice/
  platform-docs/      docker-compose.yml, openapi/, postman/, docs/
  user-service/
  product-service/
  cart-service/
Add these services to the compose file from 13.8 (the databases stay as they are):
  user-service:
    build: ../user-service
    environment:
      SERVER_PORT: 8081
      DB_URL: jdbc:postgresql://user-db:5432/user_db
      DB_USER: user_svc
      DB_PASSWORD: ${USER_DB_PASSWORD}
      JWT_SECRET: ${JWT_SECRET}
    ports: ["8081:8081"]
    depends_on: [user-db]

  product-service:
    build: ../product-service
    environment:
      SERVER_PORT: 8082
      DB_URL: jdbc:postgresql://product-db:5432/product_db
      DB_USER: product_svc
      DB_PASSWORD: ${PRODUCT_DB_PASSWORD}
      JWT_SECRET: ${JWT_SECRET}
    ports: ["8082:8082"]
    depends_on: [product-db]

  cart-service:
    build: ../cart-service
    environment:
      SERVER_PORT: 8083
      DB_URL: jdbc:postgresql://cart-db:5432/cart_db
      DB_USER: cart_svc
      DB_PASSWORD: ${CART_DB_PASSWORD}
      JWT_SECRET: ${JWT_SECRET}
      PRODUCT_SERVICE_URL: http://product-service:8082
    ports: ["8083:8083"]
    depends_on: [cart-db, product-service]
This snippet uses the simple HS256 shared secret from 5.4 (JWT_SECRET), because an RS256 key is multi-line and awkward in an environment variable. If you choose RS256, mount the key files as volumes instead and keep them out of Git.
14.5 First tasks for each developer
Developer A: User Service, then Cart Service
Skeleton, /actuator/health, Flyway V1__create_users.sql.
Register (BCrypt) and login that returns a JWT with the agreed claims.
GET and PUT /users/me, validation, standard error format, seed ADMIN user (dev only).
Tests, README, Dockerfile. Then Cart Service against the WireMock mock of Product Service (phase 3).
Developer B: Product Service and the platform repository
Skeleton, /actuator/health, Flyway migrations for categories and products.
Product and category CRUD, pagination and search, soft delete.
POST /products/bulk, JWT validation with the role converter, tested with a hand-made test token.
Dockerfile, the compose file with the three databases and the three services, Postman collection, seed data.
15. Tech Stack and Dependencies per Service
The document so far does not name a stack, but the examples (Flyway, Resilience4j, JDBC URLs, WireMock) point to Java with Spring Boot, so this section assumes it: Java 21, Spring Boot 4.x, Maven (commit the Maven wrapper mvnw), PostgreSQL 16 and Docker. Use the same Java and Spring Boot version in all three services.
Version note. Spring Boot 4 renamed several starters (for example spring-boot-starter-web became spring-boot-starter-webmvc). Names below are Boot 4 names with the Boot 3 name in brackets where it differs. Generate the project at start.spring.io so the artifact names match your version, and check that third-party libraries (springdoc, Resilience4j, WireMock integration) support your Boot version before adding them.
15.1 Dependencies for all three services
Dependency
Why
spring-boot-starter-webmvc (Boot 3: spring-boot-starter-web)
REST controllers, embedded Tomcat, JSON
spring-boot-starter-validation
@Valid, @NotBlank, @Min on request DTOs (section 10)
spring-boot-starter-data-jpa
Entities and repositories, pagination
org.postgresql:postgresql (runtime)
PostgreSQL JDBC driver
Flyway: spring-boot-starter-flyway (Boot 3: flyway-core) + org.flywaydb:flyway-database-postgresql
Versioned migrations (13.7). Set spring.jpa.hibernate.ddl-auto=validate
spring-boot-starter-security-oauth2-resource-server (Boot 3: spring-boot-starter-oauth2-resource-server)
Validates the JWT locally in every service (5.4). Also brings Spring Security
spring-boot-starter-actuator
/actuator/health for Docker health checks
springdoc-openapi-starter-webmvc-ui
Swagger UI, to compare your code with the OpenAPI contract
Test: spring-boot-starter-test, spring-boot-starter-webmvc-test, spring-boot-starter-security-test (Boot 3: spring-security-test)
JUnit 5, Mockito, MockMvc, @WithMockUser
Test: spring-boot-testcontainers, testcontainers-junit-jupiter, testcontainers-postgresql (Testcontainers 1.x: junit-jupiter, postgresql)
Run repository tests against a real PostgreSQL in Docker, not H2
lombok (optional)
Less boilerplate. Skip it if you want to see all the code while learning

15.2 Extra dependencies per service
Service
Extra dependencies
Notes
User Service
None beyond 15.1.
Passwords: BCryptPasswordEncoder is already included with Spring Security.
Signing the JWT: use NimbusJwtEncoder from the resource-server starter (HS256 secret now, RSA key pair later). Avoid adding jjwt next to it: one JWT library per project is enough.
Product Service
None beyond 15.1.
Public endpoints are opened in a SecurityFilterChain; create, update, delete need ADMIN.
Pagination and search with Spring Data Pageable and JPA Specifications.
Later (optional): spring-boot-starter-cache with Caffeine for product data.
Cart Service
None for the HTTP client: RestClient is part of Spring Framework (RestClient.builder()), with connect and read timeouts set on the request factory.
Mock for development: Docker image wiremock/wiremock (no Maven dependency).
Mock in tests: org.wiremock.integrations:wiremock-spring-boot.
Phase 5 (optional): Resilience4j for circuit breaker and retry.
Keep the WireMock mapping files in cart-service/mocks/product-service/mappings/ and run docker run -p 8082:8080 -v ./mocks/product-service/mappings:/home/wiremock/mappings wiremock/wiremock. Mockoon is an alternative with a GUI.
A hand-written retry (2 retries, short backoff, GET only) is enough for learning; add Resilience4j only after the basics work.
Cart does not call User Service in v1, so it needs no client for it.
platform-docs
No Java code.
Docker and Docker Compose, Postman, OpenAPI YAML (edit with VS Code or Swagger Editor).

15.3 Do not add in v1
No shared library between services (a common -commons jar), see 5.6.
No Spring Cloud (Eureka, Gateway, OpenFeign, Config Server) and no Kafka or RabbitMQ. They are listed under Future Improvements (section 11).
No second database driver or any connection to another service's database.
15.4 Tools on each developer machine
JDK 21, Git, Docker Desktop (with Docker Compose), IntelliJ IDEA or VS Code with Java extensions, Postman, a GitHub account, and the Maven wrapper from each repository (no global Maven needed).
