# PC Parts Store API

## Overview

PC Parts Store API is a Spring Boot microservices backend for the PC Parts Store. It provides the customer, product, inventory, ordering, authentication, shipping, and payment capabilities used by the UI.

The application is intended to run alongside the [PC Parts Store UI](https://github.com/craig-fox/pc-parts-store-ui.git). End-to-end tests are maintained in the [PC Parts Store E2E repository](https://github.com/craig-fox/pc-parts-store-e2e.git).

The project is currently at **version 1.2**, representing the first version with the core platform deployed into an AWS production-shaped environment.

## Architecture

The application is implemented as a set of independently deployable Spring Boot services. Each data-owning service has its own PostgreSQL database. The order service coordinates calls to the other services required to create and process an order.

### Local development architecture

```text
                         +-------------------+
                         |   React UI        |
                         |   localhost:5173  |
                         +---------+---------+
                                   |
                                   v
                         +-------------------+
                         |   API Gateway     |
                         |   localhost:8080  |
                         +---------+---------+
                                   |
          +----------------+-------+-------+----------------+
          |                |               |                |
          v                v               v                v
   Authentication     Customer        Product          Order
      :8085            :8081          :8083           :8082
                                                        |
                                         +--------------+--------------+
                                         |              |              |
                                         v              v              v
                                    Inventory       Shipping        Payment
                                      :8084           :8087           :8086

Each data-owning service has its own PostgreSQL database.
```

All services run as Docker containers on a shared Docker Compose network during local development.

### AWS architecture

```text
                         +-------------------+
                         |   React UI        |
                         | S3 + CloudFront   |
                         +---------+---------+
                                   |
                                   v
                         +-------------------+
                         |       ALB         |
                         +---------+---------+
                                   |
                                   v
                         +-------------------+
                         |   API Gateway     |
                         |    ECS/Fargate    |
                         +---------+---------+
                                   |
              +--------------------+--------------------+
              |          |          |          |         |
              v          v          v          v         v
          Auth       Customer    Product    Order    Inventory
          ECS         ECS         ECS       ECS        ECS
                                               |
                                      +--------+--------+
                                      |        |        |
                                      v        v        v
                                   Shipping Payment  Inventory
                                     ECS      ECS       ECS

                       AWS Cloud Map provides
                       service discovery between services.

                       RDS PostgreSQL provides
                       persistent application data.
```

The AWS deployment uses ECS/Fargate, ECR, Cloud Map, an Application Load Balancer, RDS PostgreSQL, Secrets Manager, CloudWatch Logs, S3, and CloudFront.

AWS resources are deployed in `ap-southeast-2`.

## Services

Active modules:

* `authentication-service` — authenticates users and issues JWTs for protected API endpoints.
* `customer-service` — manages customer profiles and customer data.
* `product-service` — manages the product catalogue and product information.
* `inventory-service` — tracks stock and inventory availability.
* `order-service` — creates and manages orders and coordinates the downstream services required to process them.
* `shipping-service` — calculates shipping costs based on order weight, price, and shipping method.
* `payment-service` — handles payment processing for orders by calculating and recording the payment amount.

`notification-service` remains deferred and is planned for a later version.

The payment and shipping services are now implemented and integrated into the order workflow. External providers/APIs are still future work.

## Technology Stack

* Java 21
* Spring Boot 3.5.16
* Spring Data JPA / JDBC where appropriate
* PostgreSQL
* Flyway
* Testcontainers
* Docker
* Docker Compose
* Maven
* Spring Cloud Gateway
* JWT authentication
* JUnit Jupiter
* Mockito
* WireMock
* JaCoCo
* Checkstyle
* SpotBugs
* SonarQube
* Terraform
* AWS ECS/Fargate
* AWS ECR
* AWS RDS
* AWS Secrets Manager
* AWS Cloud Map
* AWS CloudWatch
* AWS S3
* AWS CloudFront
* AWS Application Load Balancer

## Environments

The application has separate local development and AWS production-shaped environments.

| Environment          | Local development       | AWS production-shaped |
| -------------------- | ----------------------- | --------------------- |
| Profile              | `dev`                   | `prod`                |
| Compute              | Docker Compose          | ECS/Fargate           |
| Databases            | PostgreSQL containers   | Amazon RDS PostgreSQL |
| Service discovery    | Docker Compose DNS      | AWS Cloud Map         |
| Secrets              | Environment variables   | AWS Secrets Manager   |
| Logs                 | Docker logs             | CloudWatch Logs       |
| Frontend             | Vite development server | S3 + CloudFront       |
| External entry point | Local API Gateway       | ALB + API Gateway     |
| Region               | Local                   | `ap-southeast-2`      |

Tests use the `test` profile and Testcontainers where integration tests require real PostgreSQL infrastructure.

## Local Service Ports

| Service                | Local port |
| ---------------------- | ---------: |
| API Gateway            |     `8080` |
| Customer Service       |     `8081` |
| Order Service          |     `8082` |
| Product Service        |     `8083` |
| Inventory Service      |     `8084` |
| Authentication Service |     `8085` |
| Payment Service        |     `8086` |
| Shipping Service       |     `8087` |
| SonarQube              |     `9000` |

Inside the Docker Compose network, services communicate using their service names.


## Database Configuration

Each data-owning service has its own PostgreSQL database.

| Service           | Database      |
| ----------------- | ------------- |
| Customer Service  | `customerdb`  |
| Product Service   | `productdb`   |
| Order Service     | `orderdb`     |
| Inventory Service | `inventorydb` |
| Payment Service   | `paymentdb`   |
| Shipping Service  | `shippingdb`  |

Flyway manages schema creation and migrations.

The production deployment uses a PostgreSQL RDS instance. The application databases are separated logically within the RDS deployment.

### Hikari connection pools

Because the initial RDS deployment is intentionally small, the database connection pools are constrained to avoid exhausting the available PostgreSQL connections.

The DB-backed services use:

```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 5
      minimum-idle: 1
```

This gives each service a bounded pool while retaining a small number of idle connections.

## Getting Started

### Prerequisites

* Java 21
* Maven
* Docker Desktop, or Docker Engine with Docker Compose
* A value for the `JWT_SECRET` environment variable

### JWT secret

The services require a JWT secret. Do not commit the secret to the repository.

Generate a suitable value with:

```sh
openssl rand -base64 32
```

Set the generated value for the current shell session:

```sh
export JWT_SECRET='paste-the-generated-value-here'
```

For Windows PowerShell:

```powershell
$env:JWT_SECRET = "paste-the-generated-value-here"
```

Keep the same secret while the services are running so that tokens issued by the authentication service can be verified by the other services.

## Configuration

The services use Spring profiles to distinguish between environments:

* `dev` — local development and Docker Compose.
* `test` — test execution.
* `prod` — AWS production configuration.

The Docker Compose development environment activates the `dev` profile automatically.

Secrets and environment-specific values are supplied through environment variables rather than committed to the repository.

Common configuration includes:

* `JWT_SECRET`
* `JWT_EXPIRATION`
* `SPRING_DATASOURCE_URL`
* `SPRING_DATASOURCE_USERNAME`
* `SPRING_DATASOURCE_PASSWORD`
* `CUSTOMER_SERVICE_URL`
* `PRODUCT_SERVICE_URL`
* `INVENTORY_SERVICE_URL`
* `PAYMENT_SERVICE_URL`
* `SHIPPING_SERVICE_URL`

Production configuration does not provide local database fallbacks. Required production values must therefore be supplied by the deployment environment.


In AWS, service-to-service URLs use AWS Cloud Map service discovery rather than `localhost`.

For example:

```text
http://payment-service.pc-parts-store.dev:8080
http://shipping-service.pc-parts-store.dev:8080
```

## API Gateway

The API Gateway is implemented using Spring Cloud Gateway WebFlux.

The gateway provides a single external API entry point and routes requests to the appropriate service.

Current routes include:

* `/api/auth/**` → Authentication Service
* `/api/customers/**` → Customer Service
* `/api/products/**` → Product Service
* `/api/orders/**` → Order Service
* `/images/**` → Product image resources

The gateway also participates in JWT propagation, correlation ID propagation, and downstream retry handling.

## Security

Authentication is handled by the Authentication Service.

The service issues JWTs after successful login. Protected services validate the JWT through the shared security configuration.

JWTs are propagated between services where downstream calls require the authenticated user's context.

Secrets are supplied through environment variables locally and AWS Secrets Manager in the AWS deployment.

## Order Processing

The order service coordinates several downstream services when creating an order.

A typical order flow is:

```text
Client
  |
  v
API Gateway
  |
  v
Order Service
  |
  +--> Customer Service
  |
  +--> Product Service
  |
  +--> Inventory Service
  |
  +--> Shipping Service
  |
  +--> Payment Service
  |
  v
Order persistence
```

The order workflow includes:

1. Authentication of the request.
2. Customer validation.
3. Product validation/lookups.
4. Inventory reservation.
5. Shipping cost calculation.
6. Payment processing.
7. Order persistence.

Inventory reservation failures and downstream failures are handled with compensation where appropriate. For example, inventory reserved before a later failure can be released.

The order API also supports idempotency using the `Idempotency-Key` request header.

## Reliability

Reliability features currently include:

* Explicit downstream connection and read timeouts.
* Bounded Hikari connection pools.
* Gateway retry handling.
* Order idempotency.
* Inventory reservation compensation.
* Downstream error handling.
* Correlation IDs for tracing requests across services.
* Separate production configuration with no local database fallback.
* Health and operational logging through the AWS deployment.

Current RestClient timeout configuration is approximately:

```text
Connect timeout: 2 seconds
Read timeout:    5 seconds
```


Retries are applied at the gateway for appropriate transient failures. Care is taken not to blindly retry operations where doing so could create duplicate side effects.

## Observability

The application uses correlation IDs to make requests traceable across service boundaries.

A correlation ID is propagated using the `X-Correlation-ID` header and stored in the logging MDC.

The application uses:

* Docker logs locally.
* CloudWatch Logs in AWS.
* Correlation IDs for request tracing.
* Dynatrace and Splunk are available as broader observability tooling.

The correlation ID is especially useful when diagnosing a request that crosses the API Gateway and multiple downstream services.

## Testing

The project uses several levels of automated testing.

### Unit tests

JUnit Jupiter and Mockito are used for service and component tests.

### MVC/controller tests

Spring Boot test slices such as `@WebMvcTest` are used where appropriate.

### Repository/integration tests

Testcontainers provides real PostgreSQL instances for tests that require database integration.

### HTTP integration tests

WireMock is used where downstream HTTP behaviour needs to be controlled and verified.

### End-to-end tests

End-to-end tests are maintained in the [PC Parts Store E2E repository](https://github.com/craig-fox/pc-parts-store-e2e.git).

The E2E suite uses Cucumber and Docker Compose and is also executed by GitHub Actions.

## Code Quality

The project uses:

* Checkstyle
* SpotBugs
* JaCoCo
* SonarQube

GitHub Actions runs build, test, static analysis, coverage, Docker image, and E2E checks.

SonarQube can also be run locally for development-time analysis.

## Local Development

### Quick start

Start the complete development environment:

```sh
./scripts/start.sh
```

Run the complete Maven verification:

```sh
./scripts/test.sh
```

### Start selected services

Launch one service and its defined dependencies:

```sh
docker compose up --build customer-service
```

Launch multiple services:

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


## AWS Deployment

AWS infrastructure is managed through Terraform.

The infrastructure is separated into two main areas:

### Persistent infrastructure

`terraform/persistent` contains resources intended to survive application redeployments, including infrastructure such as:

* VPC/networking
* Subnets
* Security groups
* RDS
* Persistent secrets and supporting resources

### Application infrastructure

`terraform/application` contains application-level resources such as:

* ECR repositories
* ECS cluster
* ECS services
* ECS task definitions
* Cloud Map service discovery
* Application Load Balancer integration
* CloudWatch log groups
* Frontend S3 bucket and CloudFront distribution
* IAM roles required by the application

The deployment region is:

```text
ap-southeast-2
```

### ECR

Each deployable service has an ECR repository.

Images are built for the AWS ECS/Fargate ARM64 environment and pushed to ECR before the corresponding ECS service is updated.

ECR repositories are intentionally retained across application destroy/redeploy operations.

This allows images and repositories to survive an application infrastructure teardown and be reused when the application infrastructure is recreated.

### ECS/Fargate

The backend services run on ECS/Fargate.

The current deployment intentionally uses small task sizes suitable for a development/portfolio environment rather than production-scale workloads.

Services communicate privately through AWS Cloud Map.

### RDS

The deployment uses Amazon RDS PostgreSQL.

The current RDS configuration is intentionally small:

* PostgreSQL 17
* `t3g.micro`
* 20 GB allocated storage
* Up to 100 GB maximum storage
* gp3 storage
* Storage encryption enabled
* Database name `pcparts`

### Secrets Manager

Database credentials and other sensitive production values are stored in AWS Secrets Manager rather than committed to Terraform or application configuration.


### CloudWatch

ECS services send application logs to CloudWatch Logs.

The logs can be used to diagnose:

* startup failures
* downstream connection errors
* authentication failures
* gateway routing failures
* database connectivity problems
* correlation IDs across requests

### Frontend

The React frontend is deployed separately using:

* Amazon S3
* Amazon CloudFront

The frontend communicates with the public API endpoint through the API Gateway.

## AWS Deployment Scripts

The infrastructure repository contains scripts for the application deployment lifecycle.

The general workflow is:

```text
Build service images
       |
       v
Push images to ECR
       |
       v
Terraform plan
       |
       v
Terraform apply
       |
       v
ECS services updated
       |
       v
Verify CloudWatch logs / health
       |
       v
Run E2E verification
```

When application infrastructure is destroyed, ECR repositories/images are deliberately retained so that they can be reused by a subsequent deployment.

The persistent infrastructure is managed separately so that destroying application resources does not unnecessarily destroy the database and network foundation.

## Current AWS Architecture

The current AWS deployment includes:

* VPC
* Public/private subnets
* Security groups
* RDS PostgreSQL
* Secrets Manager
* ECS cluster
* ECS/Fargate services
* ECR repositories
* AWS Cloud Map
* Application Load Balancer
* CloudWatch Logs
* S3
* CloudFront
* IAM roles

The public application endpoints are:

```text
https://pcparts.craigfox.dev
https://api.pcparts.craigfox.dev
```

The frontend is served through CloudFront and the API is exposed through the AWS load-balancing/gateway path.

## Capacity and Scaling Considerations

The current deployment is intentionally small, but the application has been designed with several scaling concerns in mind.

### Service-level scaling

Services can be scaled independently because they are separately deployed ECS tasks.

The main services involved in order processing are:

* Order
* Inventory
* Product
* Customer
* Shipping
* Payment


The order service is the main orchestration point and therefore tends to generate the most downstream traffic.

### Database connections

Each DB-backed service currently limits its Hikari pool to five connections.

This is appropriate for the current small RDS instance but would need to be reconsidered when increasing ECS task counts.

Scaling the number of ECS tasks without considering database connection limits could exhaust the available PostgreSQL connections.

### Order workload

For an order containing `N` items, a typical successful workflow involves approximately:

* 1 customer validation
* `N` inventory reservations
* `N` product lookups
* 1 shipping quote
* 1 payment operation
* 2 order persistence operations

A failed workflow can add inventory release operations.

An idempotent retry can avoid repeating the complete workflow where the original request has already been recorded.

This provides a useful starting point for load and capacity testing.

## Frontend

The separate React frontend is maintained in:

[PC Parts Store UI](https://github.com/craig-fox/pc-parts-store-ui.git)

The frontend uses:

* React
* TypeScript
* Vite
* Tailwind CSS
* React Router
* Vitest

The UI communicates with the API Gateway rather than directly calling each backend service.

The current UI version is **1.1**.

Shipping calculations are integrated into checkout and use the shipping service rather than duplicating backend order logic in the frontend.

## Version 1.2

Version 1.2 represents the major transition from a locally running microservices application to a production-shaped AWS deployment.

Major additions include:

* Payment Service implementation.
* Shipping Service implementation.
* Payment and shipping integration with the order workflow.
* Downstream timeout configuration.
* Gateway retry behaviour.
* Inventory reservation compensation.
* Order idempotency.
* API Gateway.
* JWT propagation.
* Correlation ID propagation.
* Observability improvements.
* AWS ECR deployment.
* ECS/Fargate deployment.
* AWS Cloud Map service discovery.
* Application Load Balancer integration.
* RDS PostgreSQL.
* AWS Secrets Manager.
* CloudWatch logging.
* S3 and CloudFront frontend deployment.
* Terraform separation between persistent and application infrastructure.
* AWS end-to-end order verification.

`notification-service` remains intentionally deferred.

## Future Work

Potential future work includes:

1. **Notification Service**

   * Implement email/order notification workflows.

2. **External payment provider**

   * Replace the current payment implementation with an external payment provider integration.

3. **External shipping provider**

   * Integrate real shipping/rate APIs.

4. **Event-driven workflows**

   * Move suitable service interactions from synchronous HTTP calls toward events using AWS messaging services.

5. **ECS autoscaling**

   * Add CPU/request-based scaling policies once realistic workload measurements are available.

6. **Load and capacity testing**

   * Measure service throughput, latency, database connection usage, and downstream call volume under realistic workloads.

7. **Database scaling**

   * Evaluate larger RDS instances, read replicas, connection pooling improvements, or RDS Proxy as workload increases.

8. **Caching**

   * Evaluate caching for read-heavy product/catalogue workloads.

9. **Operational hardening**

   * Expand alerting, dashboards, incident procedures, backup/recovery testing, and production operational practices.

10. **Deployment automation**

    * Further automate image promotion, infrastructure deployment, smoke tests, and rollback.

11. **Technology upgrades**

    * Future upgrade path includes Spring Boot 4 and Java 25 once the application's dependencies and deployment environment are ready.

## Project Structure

The repository is a Maven multi-module project.

A simplified structure is:

```text
pc-parts-store-api/
├── authentication-service/
├── customer-service/
├── product-service/
├── inventory-service/
├── order-service/
├── payment-service/
├── shipping-service/
├── api-gateway/
├── security-common/
├── observability-common/
├── scripts/
├── docker-compose.yml
├── pom.xml
└── README.md
```

The exact module structure may evolve as additional shared functionality and services are introduced.

## Status

The core PC Parts Store backend is implemented and deployed in a production-shaped AWS environment.

### Implemented

* Authentication
* Customer management
* Product catalogue
* Inventory management
* Order processing
* Payment processing
* Shipping calculation
* JWT security
* API Gateway
* Service-to-service communication
* Timeouts and downstream error handling
* Retry handling
* Idempotency
* Inventory compensation
* Correlation IDs
* Automated testing
* Code quality checks
* Docker-based local development
* Terraform infrastructure
* ECR
* ECS/Fargate
* Cloud Map
* ALB
* RDS PostgreSQL
* Secrets Manager
* CloudWatch Logs
* S3
* CloudFront
* AWS end-to-end verification

### Deferred

* Notification Service
* External payment provider
* External shipping provider
* Event-driven service workflows
* Production-scale autoscaling and capacity tuning

