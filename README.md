# E-commerce

Demo marketplace API written in Java Spring Boot (SQLite, JWT authentication).

There are no administrators: every registered account is a regular user and can
publish products. A user can edit or delete only the products they created,
while browsing and filtering products is public.

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
including old databases that still contain the former `users.role` column or a
global unique constraint on product names.

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
| GET    | `/api/users/me` | Current user's id, email, joined |

### Products

| Method | Path                    | Auth       | Description                              |
|--------|-------------------------|------------|------------------------------------------|
| GET    | `/api/products`         | public     | List all products                        |
| GET    | `/api/products/{id}`    | public     | Get one product                          |
| POST   | `/api/products/filter`  | public     | Filter products (body may be empty)      |
| POST   | `/api/products`         | user       | Publish a product                        |
| PUT    | `/api/products/{id}`    | owner      | Partially update own product             |
| DELETE | `/api/products/{id}`    | owner      | Delete own product                       |

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

| Method | Path              | Description          |
|--------|-------------------|----------------------|
| GET    | `/api/categories` | List all categories  |
