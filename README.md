# Checkout API (Sandbox) — Spring Boot + Stripe

API REST para simular un flujo de checkout usando una pasarela de pagos en modo sandbox (Stripe). Incluye creación de órdenes, inicio de pago (Payment Intent), procesamiento de webhooks y pruebas automatizadas con Testcontainers. Listo para correr con Docker Compose y CI en GitHub Actions.

## Características
- Creación de órdenes con items y cálculo de totales en “minor units” (centavos).
- Inicio de pago vía Stripe (sandbox) y retorno de `client_secret`.
- Webhook para actualizar estado de la orden (`PAID`, `FAILED`) verificando firma.
- Idempotencia en el endpoint de checkout mediante header `Idempotency-Key`.
- Pruebas unitarias e integración (PostgreSQL con Testcontainers).
- Dockerfile multi-stage + `docker-compose.yml` para entorno local.
- CI de ejemplo con build, test y artefacto de cobertura (JaCoCo).
- OpenAPI/Swagger habilitado para explorar la API.

## Stack
- Java 17, Spring Boot 3 (Web, Data JPA, Validation, Actuator)
- PostgreSQL
- Stripe SDK (modo test)
- JUnit 5, Mockito, Spring Boot Test, Testcontainers, JaCoCo
- Docker, Docker Compose
- GitHub Actions (CI)

## Arquitectura (resumen)
- Capas:
  - Controller (REST) → recibe requests y valida datos
  - Service (negocio) → orquesta checkout, totales, estados
  - Repository (JPA) → persistencia de órdenes, items y pagos
  - Integration (Stripe) → cliente hacia la pasarela
- Estados:
  - Order: `CREATED` → `PAYMENT_PENDING` → `PAID` | `FAILED` | `CANCELED`
  - Payment: `INITIATED` → `SUCCEEDED` | `FAILED` | `CANCELED`

## Endpoints
- POST `/api/orders` — crea una orden local
- POST `/api/checkout` — inicia el pago en Stripe (requiere `Idempotency-Key`)
- POST `/api/webhooks/stripe` — recibe eventos de Stripe (firma requerida)
- GET `/api/orders/{orderId}` — consulta estado y detalles de una orden

## Modelos (JSON)
Orden (request para crear):
```json
{
  "currency": "USD",
  "items": [
    { "name": "T-shirt", "unitPriceMinor": 1999, "quantity": 2 },
    { "name": "Cap", "unitPriceMinor": 1299, "quantity": 1 }
  ]
}
```

Orden (respuesta):
```json
{
  "orderId": "f0c68e4e-0e0d-4fd4-8f77-abc123456789",
  "status": "CREATED",
  "currency": "USD",
  "totalAmountMinor": 5297,
  "items": [
    { "name": "T-shirt", "unitPriceMinor": 1999, "quantity": 2, "subtotalMinor": 3998 },
    { "name": "Cap", "unitPriceMinor": 1299, "quantity": 1, "subtotalMinor": 1299 }
  ]
}
```

Checkout (request):
```json
{
  "orderId": "f0c68e4e-0e0d-4fd4-8f77-abc123456789",
  "provider": "STRIPE"
}
```

Checkout (respuesta):
```json
{
  "orderId": "f0c68e4e-0e0d-4fd4-8f77-abc123456789",
  "paymentId": "2a0f...123",
  "provider": "STRIPE",
  "clientSecret": "pi_3Nx...secret_123",
  "status": "PAYMENT_PENDING"
}
```

## Variables de entorno
- `SPRING_DATASOURCE_URL` — ej.: `jdbc:postgresql://localhost:5432/app`
- `SPRING_DATASOURCE_USERNAME` — ej.: `app`
- `SPRING_DATASOURCE_PASSWORD` — ej.: `app`
- `STRIPE_API_KEY` — clave secreta de Stripe en modo test (pk_live no usar aquí)
- `STRIPE_WEBHOOK_SECRET` — secret del endpoint de webhook (Stripe CLI / Dashboard)
- `SERVER_PORT` — opcional (default 8080)

Ejemplo `.env` (local):
```env
SPRING_DATASOURCE_URL=jdbc:postgresql://db:5432/app
SPRING_DATASOURCE_USERNAME=app
SPRING_DATASOURCE_PASSWORD=app
STRIPE_API_KEY=sk_test_************************
STRIPE_WEBHOOK_SECRET=whsec_************************
SERVER_PORT=8080
```

## Arranque rápido (Docker Compose)
1) Asegúrate de tener Docker y Docker Compose instalados.
2) Coloca tu `.env` en el raíz del proyecto.
3) Ejecuta:
```bash
docker compose up --build
```
4) API disponible en `http://localhost:8080`

Compose típico:
```yaml
version: "3.9"
services:
  app:
    build: .
    ports:
      - "8080:8080"
    env_file:
      - .env
    depends_on:
      - db

  db:
    image: postgres:16
    environment:
      POSTGRES_DB: app
      POSTGRES_USER: app
      POSTGRES_PASSWORD: app
    ports:
      - "5432:5432"
```

## Desarrollo local (sin Docker)
1) PostgreSQL corriendo localmente (puedes usar Testcontainers en tests).
2) Exporta variables de entorno (ver `.env`).
3) Ejecuta:
```bash
./mvnw spring-boot:run
```

## Stripe (Sandbox) y Webhooks
- Tarjeta de prueba: `4242 4242 4242 4242` con cualquier fecha futura y CVC `123`.
- No almacenes datos de tarjeta en el backend: el front confirmará el pago con `client_secret`.
- Stripe CLI para webhooks:
```bash
# 1) Autenticación
stripe login

# 2) Escuchar y reenviar webhooks al backend local
stripe listen --forward-to localhost:8080/api/webhooks/stripe

# 3) Simular un evento de éxito
stripe trigger payment_intent.succeeded
```
- Usa el valor de `WEBHOOK_SECRET` que te da el comando `listen` como `STRIPE_WEBHOOK_SECRET`.

## Idempotencia
- Envía `Idempotency-Key` en `POST /api/checkout`.
- El backend persistirá la clave y el resultado para devolver la misma respuesta si el cliente reintenta con el mismo payload.
- Si la clave se reutiliza con payload distinto, el backend debe responder 409 Conflict.

## OpenAPI/Swagger
- UI: `http://localhost:8080/swagger-ui/index.html`
- Spec: `http://localhost:8080/v3/api-docs`

## Ejemplos (curl)
Crear orden:
```bash
curl -X POST http://localhost:8080/api/orders \
  -H "Content-Type: application/json" \
  -d '{
        "currency":"USD",
        "items":[
          {"name":"T-shirt","unitPriceMinor":1999,"quantity":2},
          {"name":"Cap","unitPriceMinor":1299,"quantity":1}
        ]
      }'
```

Iniciar checkout (Stripe):
```bash
curl -X POST http://localhost:8080/api/checkout \
  -H "Content-Type: application/json" \
  -H "Idempotency-Key: 7f0f3f6b-2bce-4a2f-bb36-1234567890ab" \
  -d '{
        "orderId":"f0c68e4e-0e0d-4fd4-8f77-abc123456789",
        "provider":"STRIPE"
      }'
```

Webhook (Stripe) — simulación:
```bash
curl -X POST http://localhost:8080/api/webhooks/stripe \
  -H "Content-Type: application/json" \
  -H "Stripe-Signature: t=...,v1=..." \
  -d '{ "type":"payment_intent.succeeded", "data": { "object": { "id":"pi_3Nx...", "metadata": {"orderId":"f0c68e4e-0e0d-4fd4-8f77-abc123456789"} } } }'
```

## Pruebas
- Unitarias: reglas de negocio (totales, validaciones, idempotencia).
- Integración: Spring Boot Test + Testcontainers (PostgreSQL).
- Cobertura: JaCoCo (objetivo ≥ 80%).
```bash
./mvnw verify
# Reporte: target/site/jacoco/index.html
```

## CI (GitHub Actions)
Workflow ejemplo:
```yaml
name: CI

on:
  push:
    branches: [ "main" ]
  pull_request:
    branches: [ "main" ]

jobs:
  build-test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: "17"
          cache: maven
      - run: mvn -B verify
      - uses: actions/upload-artifact@v4
        if: always()
        with:
          name: jacoco-report
          path: target/site/jacoco

  docker-build:
    runs-on: ubuntu-latest
    needs: build-test
    steps:
      - uses: actions/checkout@v4
      - uses: docker/setup-qemu-action@v3
      - uses: docker/setup-buildx-action@v3
      - uses: docker/build-push-action@v5
        with:
          context: .
          platforms: linux/amd64
          push: false
          tags: localhost/checkout-api:ci
```

## Buenas prácticas
- Montos en `minor units` (evita errores de punto flotante).
- Validar firma de webhooks antes de procesar el contenido.
- No registrar datos sensibles en logs.
- Estados terminales (PAID/FAILED) no deben revertirse por eventos fuera de orden.
- Documenta claramente las transiciones de estado.

## Estructura del proyecto (sugerida)
```
.
├─ src/main/java/.../controller
├─ src/main/java/.../service
├─ src/main/java/.../repository
├─ src/main/java/.../integration/stripe
├─ src/main/java/.../model
├─ src/test/java/... (unit e integración)
├─ Dockerfile
├─ docker-compose.yml
├─ README.md
└─ pom.xml
```

Autor: AlsovCR2 — Proyecto personal para reforzar integración de pagos, pruebas, Docker y CI/CD en Java Backend.
