# Implementación & Trazabilidad - LearnSmart Backend

Este documento rastrea el progreso de implementación mapeado a las Historias de Usuario (US) definidas en `user-stories.md`.

## Estada Actual
**Última actualización:** 18 Enero 2026
**Microservicios Iniciados:** `profile-service`
**Infraestructura:** `back-end-eureka`, `gateway`

---

## 🏗 Infraestructura Base

- [x] **Service Discovery (Eureka)**
  - Configuración Docker-friendly (`application.yml`)
  - Puerto: 8761
- [x] **API Gateway**
  - Mapeo de rutas a microservicios (`lb://*`)
  - Configuración CORS global
  - Puerto: 8762

---

## 👤 ÉPICA 1: Gestión de Usuario y Perfil (`profile-service`)

### Feature 1.1 — Registro y autenticación
- **US-001 – Registro de usuario**
  - [x] **Endpoint**: `POST /auth/register` (AuthController)
  - [x] **Lógica**: Simulación de ID externo (Keycloak), creación de registro en `user_profiles`.
  - [x] **Validación**: `@Valid`, `@NotBlank`, `@Email`.

### Feature 1.2 — Gestión del perfil
- **US-002 – Consultar mi perfil**
  - [x] **Endpoint**: `GET /profiles/me`
  - [x] **Header**: Uso de `X-User-Id` para contexto usuario.
- **US-003 – Actualizar mi perfil**
  - [x] **Endpoint**: `PUT /profiles/me`
  - [x] **Datos**: Nombre, Locale, Timezone, Año nacimiento.

### Feature 1.3 — Gestión de objetivos
- **US-004 – Definir objetivos educativos**
  - [x] **Endpoint**: `POST /profiles/me/goals`
  - [x] **Entidad**: `UserGoal` con campos (domain, targetLevel, intensity...)
- **US-005 – Actualizar o desactivar objetivos**
  - [x] **Endpoint**: `PUT /profiles/me/goals/{id}`
  - [x] **Endpoint**: `DELETE /profiles/me/goals/{id}`

### Checklist Técnico (Profile Service)
- [x] **Estructura Proyecto**: Spring Boot 3.4.5, Java 21. `pom.xml` independiente.
- [x] **Base de Datos**: PostgreSQL driver.
- [x] **Schema**: `schema.sql` (ddl) idempotente.
- [x] **JPA**: Entidades `UserProfile`, `UserGoal` + Repositorios.
- [x] **Observabilidad**: Cliente Eureka configurado.
- [x] **Tests**: Tests de integración (Ejecutados y Verificados en Docker).
  - Verificado flujo end-to-end: Register -> Get Profile via Gateway.


---

## 📚 ÉPICA 2: Contenido (`content-service`)
*Pendiente de inicio.*

## 📅 ÉPICA 3: Planificación (`planning-service`)
*Pendiente de inicio.*

## 🎓 ÉPICA 4: Evaluación (`assessment-service`)
*Pendiente de inicio.*

## 📊 ÉPICA 5: Tracking (`tracking-service`)
*Pendiente de inicio.*

## 🤖 ÉPICA 6: Inteligencia Artificial (`ai-service`)
*Pendiente de inicio.*
