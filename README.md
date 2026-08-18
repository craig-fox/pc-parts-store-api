# PC Parts Store API

## Overview

PC Parts Store API is a Spring Boot microservices backend for the PC Parts Store. It provides the core customer, product, inventory, ordering, and authentication capabilities used by the UI.

This application is intended to run alongside the [PC Parts Store UI](https://github.com/craig-fox/pc-parts-store-ui.git). End-to-end tests are available in the [PC Parts Store E2E repository](https://github.com/craig-fox/pc-parts-store-e2e.git).

## Architecture diagram

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

- Docker Desktop (or Docker Engine with Docker Compose)
- A value for the `JWT_SECRET` environment variable

Set `JWT_SECRET` before attempting to launch the containers. Generate a suitable value with:

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

For Windows Command Prompt, use `set JWT_SECRET=paste-the-generated-value-here` instead. Keep the same secret while the services are running so that tokens issued by the authentication service can be verified by the other services.

## Running a service

Use Docker Compose from the repository root. With Docker Compose v2, run `docker compose`; if your installation uses the older standalone command, replace it with `docker-compose` in the examples below.

Launch one service and the dependencies defined for it:

```sh
docker compose up --build customer-service
```

Launch multiple services by listing their names:

```sh
docker compose up --build customer-service product-service authentication-service
```

Launch the complete available stack in the background:

```sh
docker compose up --build -d
```

Stop the stack with `docker compose down`. Add `-v` only when you also want to remove the PostgreSQL data volumes.

## Running all tests

Run the full Maven test suite from the repository root:

```sh
mvn test
```

This command requires Maven to be installed. Individual services also include Maven wrappers if you prefer to run their tests from the corresponding service directory.

## Future work

Develop the payment, notification, and shipping services, then integrate them with the active order workflow.
