# E-commerce

Demo marketplace API written in Java Spring Boot (SQLite, JWT authentication).

The API has two roles:

- `USER` — assigned automatically on registration. Can browse products and
  categories, but cannot change anything.
- `ADMIN` — the built-in administrator account. Can create, update, and delete
  products and categories.

Browsing and filtering products is public. Product changes require an admin
token.

## Run

Requirements: JDK 17 or newer.

Windows (PowerShell or cmd):

```bat
mvnw.cmd spring-boot:run
```

Linux/macOS:

```bash
./mvnw spring-boot:run
```

On Windows the wrapper automatically picks an installed JDK 17+ even when
`JAVA_HOME` points to an older Java version. If you start `ServerApplication`
from an IDE, make sure the project SDK is set to JDK 17+ as well.

The server listens on `http://localhost:8080` and stores data in
`./e-commerce.db`. On startup the database schema is updated automatically,
including old databases that still have a global unique constraint on product
names.

### Port 8080 is already in use

This means another copy of the application is still running, usually a previous
run that was not stopped. Stop it instead of starting a second one. On Windows:

```powershell
Get-NetTCPConnection -LocalPort 8080 -State Listen | Select-Object OwningProcess
Stop-Process -Id <PID> -Force
```

To run on a different port:

```bat
mvnw.cmd spring-boot:run -Dspring-boot.run.arguments=--server.port=8081
```

When starting from IntelliJ, enable **Single instance only** in the run
configuration so pressing Run again reuses the running application instead of
launching a duplicate.

## Roles and the administrator account

Registration (`POST /api/auth/register`) always creates a `USER`, even if a role
is sent in the request body.

On startup the application ensures that this administrator exists:

| Email                        | Password      | Role    |
|------------------------------|---------------|---------|
| `lagcompensation@admin.test` | `123456789ww` | `ADMIN` |

The credentials can be changed with `app.admin.email` and `app.admin.password`
in `src/main/resources/application.properties`. The seeded account is not
recreated if it already exists; its password is reset only when it no longer
matches the configured value.

The role is returned by `/api/auth/login`, `/api/auth/register`, and
`/api/users/me` as `USER` or `ADMIN`.

## Endpoints

### Auth (public)

| Method | Path                 | Description                     |
|--------|----------------------|---------------------------------|
| POST   | `/api/auth/register` | Register `{ email, password }`  |
| POST   | `/api/auth/login`    | Log in and receive a JWT token  |

Send the token as `Authorization: Bearer <token>`.

### Users

| Method | Path            | Description                      |
|--------|-----------------|----------------------------------|
| GET    | `/api/users/me` | Current user's id, email, role, joined |

### Products

| Method | Path                    | Auth       | Description                              |
|--------|-------------------------|------------|------------------------------------------|
| GET    | `/api/products`         | public     | List all products                        |
| GET    | `/api/products/{id}`    | public     | Get one product                          |
| POST   | `/api/products/filter`  | public     | Filter products (body may be empty)      |
| POST   | `/api/products`         | admin      | Publish a product                        |
| PUT    | `/api/products/{id}`    | admin      | Partially update a product               |
| DELETE | `/api/products/{id}`    | admin      | Delete a product                         |

Create product body:

```json
{
  "name": "Coffee beans",
  "description": "1 kg, medium roast",
  "price": 12.50,
  "stock": 20,
  "categoryId": null
}
```

Update body accepts any subset of `name`, `description`, `price`, `stock`,
`categoryId`. Missing fields stay unchanged.

Filter body:

```json
{
  "name": "coffee",
  "categoryId": 1,
  "sellerId": 2,
  "minPrice": 5,
  "maxPrice": 20,
  "minStock": 1,
  "maxStock": 100
}
```

Product names must be unique per seller; different users may publish products
with the same name.

### Categories

| Method | Path                   | Auth    | Description                          |
|--------|------------------------|---------|--------------------------------------|
| GET    | `/api/categories`      | public  | List all categories                  |
| POST   | `/api/categories`      | admin   | Create a category `{ name }`         |
| PUT    | `/api/categories/{id}` | admin   | Rename a category `{ name }`         |
| DELETE | `/api/categories/{id}` | admin   | Delete a category unused by products |

Deleting a category that still has products returns `409 Conflict`.
