# PC Parts Store API

## Overview

PC Parts Store API is a Spring Boot microservices backend for the PC Parts Store. It provides the core customer, product, inventory, ordering, and authentication capabilities used by the UI.

This application is intended to run alongside the [PC Parts Store UI](https://github.com/craig-fox/pc-parts-store-ui.git). End-to-end tests are maintained in the [PC Parts Store E2E repository](https://github.com/craig-fox/pc-parts-store-e2e.git).

## Architecture

The services run as Docker containers on a shared backend network. Each data-owning active service has its own PostgreSQL database. The order service communicates with the customer, product, and inventory services when processing orders.

## Services

Active modules:

- `customer-service` — manages customer profiles and customer data.
- `order-service` — creates and manages orders, coordinating with customer, product, and inventory services.
- `product-service` — manages the product catalogue and product information.
- `inventory-service` — tracks stock and inventory availability.
- `authentication-service` — authenticates users and issues JWTs for access to protected API endpoints.

`payment-service`, `notification-service`, and `shipping-service` are currently stubs. They are planned for development in a later phase.

## Technology

- Java 21
- Spring Boot
- PostgreSQL
- Flyway
- Testcontainers
- Docker
- Maven

## Getting Started

### Prerequisites

- Java 21
- Maven
- Docker Desktop (or Docker Engine with Docker Compose)
- A value for the `JWT_SECRET` environment variable

### JWT secret

The services require a JWT secret. Do not commit the secret to the repository.

Generate a suitable value with:

```sh
openssl rand -base64 32
```

`openssl` is commonly available on macOS and Linux. If it is not installed on Linux, install the distribution's `openssl` package first. On Windows, run the command from Git Bash or WSL if either is installed; otherwise, generate a random Base64 value with PowerShell:

```powershell
[Convert]::ToBase64String((1..32 | ForEach-Object { Get-Random -Maximum 256 }))
```

Set the generated value for your current shell session:

```powershell
# Windows PowerShell
$env:JWT_SECRET = "paste-the-generated-value-here"
```

```sh
# macOS and Linux
export JWT_SECRET='paste-the-generated-value-here'
```

For Windows Command Prompt:

```cmd
set JWT_SECRET=paste-the-generated-value-here
```

Keep the same secret while the services are running so that tokens issued by the authentication service can be verified by the other services.

## Configuration

The services use Spring profiles to distinguish between environments:

- `dev` — local development and Docker Compose.
- `test` — test execution; integration tests use Testcontainers where applicable.
- `prod` — production configuration, with environment-specific values supplied externally.

The Docker Compose development environment activates the `dev` profile automatically.

Secrets and environment-specific values are supplied through environment variables rather than being committed to the repository. Depending on the service and environment, these include:

- `JWT_SECRET`
- `JWT_EXPIRATION`
- `SPRING_DATASOURCE_URL`
- `SPRING_DATASOURCE_USERNAME`
- `SPRING_DATASOURCE_PASSWORD`
- Service base URLs such as `CUSTOMER_SERVICE_URL`, `PRODUCT_SERVICE_URL`, and `INVENTORY_SERVICE_URL`

Production configuration does not provide local database fallbacks, so the required production values must be supplied by the deployment environment.

## Running and Testing

### Quick start

Start the complete development environment:

```sh
./scripts/start.sh
```

Run the complete Maven verification:

```sh
./scripts/test.sh
```

### Start individual or selected services

Use Docker Compose from the repository root.

Launch one service and the dependencies defined for it:

```sh
docker compose up --build customer-service
```

Launch multiple services by listing their names:

```sh
docker compose up --build customer-service product-service authentication-service
```

Launch the complete stack in the background:

```sh
docker compose up --build -d
```

Stop the stack:

```sh
docker compose down
```

Add `-v` only when you also want to remove the PostgreSQL data volumes.

### End-to-end tests

End-to-end tests are maintained in the [PC Parts Store E2E repository](https://github.com/craig-fox/pc-parts-store-e2e.git).

The E2E environment uses Docker Compose with separate PostgreSQL volumes so that E2E data is isolated from development data. The E2E suite uses Cucumber and is also executed by GitHub Actions.

### CI/CD and code quality

GitHub Actions runs the following checks:

- Maven build and tests
- Checkstyle
- SpotBugs
- JaCoCo coverage checks
- End-to-end tests
- Docker image builds

SonarQube is available for local static analysis.

## Future work

Develop the payment, notification, and shipping services, then integrate them with the active order workflow.
