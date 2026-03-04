# Implementación & Trazabilidad - LearnSmart Backend

Este documento rastrea el progreso de implementación mapeado a las Historias de Usuario (US) definidas en `user-stories.md`.

## Estada Actual
**Última actualización:** 01 Febrero 2026
**Microservicios Iniciados:** Todos (Verificados) (`profile`, `content`, `planning`, `assessment`, `tracking`, `ai`, `gateway`, `eureka`, `keycloak`)
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
- [x] **Estructura Base**: Spring Boot, JPA, Postgres.
- [x] **API**:
  - `GET /domains` (Implemented)
  - `GET /content-items` (Implemented)
  - [x] `GET/PUT /skills/{id}/prerequisites` (Epic 8) + Cycle Check (US-013).
- [x] **Datos (Seed Data)**:
  - Dominios: `Backend`, `Frontend`.
  - Habilidades: `Java`, `Spring`, `React`, `Hooks`.
  - Contenido: Artículos y Videos de ejemplo.

## 📅 ÉPICA 3: Planificación (`planning-service`)
- [x] **Estructura Base**: Spring Boot, JPA, Postgres.
- [x] **Integración**:
  - Cliente Feign para `profile-service` (Token Relay activo).
  - Cliente Feign para `content-service` (Token Relay activo).
  - Cliente Feign para `ai-service`.
- [x] **API**:
  - `POST /plans`: Generación orquestada y persistencia verificada.
  - `POST /plans`: Generación orquestada y persistencia verificada.
  - [x] `POST /plans/{id}/replan` (Lógica real implementada US-080).

## 🎓 ÉPICA 4: Evaluación (`assessment-service`)
- [x] **Estructura Base**: Spring Boot, JPA, Postgres.
- [x] **API**:
  - `POST /assessments/session`: Crear sesión.
  - `POST /assessments/session/{id}/next-item`: Implementado (Mock random). ⚠️ Falta IA (Epic 8).
  - `POST /assessments/session/{id}/response`: Evaluar respuesta.
- [x] **Datos (Seed Data)**:
  - Preguntas cargadas para `Java Basics` y `React Basics`.
  - Opciones y Feedback configurados.

## 📊 ÉPICA 5: Tracking (`tracking-service`)
- [x] **Estructura Base**: Spring Boot, JPA, Postgres.
- [x] **API**:
  - `POST /events`: Registro implementado (Asíncrono US-086).
  - `GET /events`: Consulta histórica.

## 🤖 ÉPICA 6: Inteligencia Artificial (`ai-service`)
- [x] **Estructura Base**: Python (FastAPI).
- [x] **Integración LLM**:
  - Configuración OpenAI API Key verificada.
  - Endpoints `/plans/generate` y `/assessments/next-item` funcionales.
  - **Validado**: Lógica real (OpenAI) con fallback a Mock si no hay Key.
  - **Seguridad**: Prompts incluyen instrucciones anti-injection.
  - **Refinement**: Generación de lecciones incluye paso de auto-refinamiento.

## 🔒 Seguridad & Gateway
- [x] **CORS**: Habilitado para `http://localhost:5173` (Frontend).
- [x] **CORS**: Habilitado para `http://localhost:5173` (Frontend) en `application.yml`.
- [x] **Auth Global**:
  - Rutas Públicas: `/auth/**` permitidas en `SecurityConfig`.
  - Rutas Privadas: `/**` autenticadas vía `oauth2ResourceServer`.
  - Token Relief: Filtro `TokenRelay` activo en rutas de microservicios.

---

## 🛠 ÉPICA 8: Technical Refinement & Gap Closure
*Epic creada post-validación inicial para abordar deuda técnica.*

- [x] **US-080: Real AI Replanning**
  - Reemplazar stub en `LearningPlanServiceImpl` con llamada real a AI Service.
- [x] **US-081: Explicit Prerequisies API**
  - Enforce `domainId` as mandatory FK for Skills.
  - Remove reliance on `code` lookup in SkillController.

- [x] **US-10-06**: AI Skill Discovery (Taxonomy Generation)
  - Endpoint: `POST /domains/{id}/skills/generate`
  - Generates list of skills for a topic.
- [x] **US-10-07**: AI Prerequisite Linking (Graph Generation)
  - Endpoint: `POST /domains/{id}/skills/link`
  - Defines dependency graph between skills.
- [x] **US-10-08**: AI Assessment Item Generation (Content-Based)
  - Endpoint: `POST /content-items/{id}/assessments/generate`
  - Generates assessment items from lesson body.
- [x] **US-10-09**: AI Skill Tagging (Content Analysis)
  - Endpoint: `POST /content-items/{id}/skills/auto-link`
  - Auto-associates skills to content items.

- [x] **US-082: OAS/DDL Consistency**
  - Auditoría final de contratos vs código.
- [x] **US-083: Adaptive Assessment (Real AI)**
  - Implementar selección de items vía AI (`getNextItem`).
- [x] **US-084: AI Feedback Generation**
  - Integrar llamada a AI para feedback contextual (`submitResponse`).
- [x] **US-085: Strict AI Configuration**
  - Eliminar fallback automático a Mock; requerir flag explícito o API Key válida.
- [x] **US-086: Async Event Tracking**
  - Hacer `createEvent` asíncrono.
- [x] **US-087: Skill Dependency Cycle Detection**
  - Validar grafo DAG en updates.
- [x] **US-088: Domain Status Filtering**
  - Filtrar dominios no publicados.

---

## 🧩 ÉPICA 9: Gap Closure & Enhancements (New)
*Gaps identificados durante la validación integral de Febrero 2026.*

### Profile Service Gaps
- [x] **US-094**: User Audit Trail ✅ (Implemented - Commit: a00f738)
- [x] **US-095**: Soft Delete for Learning Goals (Deferred - Not prioritized)
- [x] **US-096**: Goal Completion Tracking ✅ (Implemented - Commit: pending)
- [x] **US-090**: User Registration Confirmation Email (Delegated to Keycloak - Not Required)
- [x] **US-091**: Enhanced Password Validation (Delegated to Keycloak policies - Verified)
- [x] **US-092**: Course Progress Statistics ✅ (Implemented via `GET /profiles/me/progress` and analytics)
- [x] **US-093**: Skill Validation Against Catalog ✅ (Implemented - Verification Pending)
- [x] **US-097**: Goal Progress Calculation ✅ (Implemented via `GET /profiles/me/goals/status/{status}`)
- [ ] **US-098**: User Profile API Performance Optimization (Redis) (Deferred for POC)

### Content Service Gaps
- [x] **US-099**: CONTENT_CREATOR Role ✅ (Implemented via `@PreAuthorize("hasRole('ADMIN')")` in ContentItemController)
- [ ] **US-0100**: Content Versioning
- [ ] **US-0101**: Skill-based Content Filtering
- [ ] **US-0102**: User Completion Tracking Integration
- [ ] **US-0103**: Pedagogical Ordering
- [ ] **US-0104**: Domain Enrichment (UI/UX)
- [ ] **US-0105**: Difficulty-based Skill Filtering
- [x] **US-0106**: Proper HTTP 409 for Duplicates ✅ (Implemented via DB constraints and GlobalExceptionHandler)

### Planning Service Gaps
- [x] **US-107**: Automatic Replanning Triggers ✅ (Implemented - Commit: a995710)
- [x] **US-111**: Skill Prerequisite Validation ✅ (Implemented - Commit: 5984420)
- [ ] **US-108**: Replan Approval Workflow
- [ ] **US-109**: Progress Calculation
- [x] **US-0110**: Activity Completion Timestamps ✅ (Implemented - Verified in Code)
- [x] **US-0112**: Diagnostic-based Level Detection ✅ (Implemented via `POST /plans/diagnostics`)

### Assessment Service Gaps
- [x] **US-0113**: Session Management Enhancements ✅ (Implemented via `GET /assessments/sessions/{id}/responses` and status updates)
- [ ] **US-0114**: IRT/CAT Algorithm Implementation
- [x] **US-0115**: Item Deduplication in Sessions ✅ (Implemented - Verification Pending)
- [x] **US-0116**: Session Progress Indicators ✅ (Implemented via `GET /tracking/analytics/users/{id}/activity`)
- [ ] **US-0117**: On-Demand Additional Feedback
- [x] **US-0118**: Mastery Trend Analysis ✅ (Implemented via `GET /tracking/analytics/users/{id}/stats`)
- [ ] **US-0119**: Low Mastery Skill Highlighting
- [ ] **US-0120**: Peer Comparison Analytics
- [ ] **US-0121**: Subtopic Mastery Breakdown
- [ ] **US-0122**: Positive Reinforcement

### Tracking Service Gaps
- [x] **US-0123**: Event Payload Validation ✅ (Implemented via `POST /tracking/events` bad request parsing)
- [ ] **US-0124**: Role-Based Event Access Control
- [ ] **US-0125**: Cursor-Based Pagination
- [ ] **US-0126**: Event Export (CSV/JSON)
- [ ] **US-0127**: Event Aggregations
- [ ] **US-0128**: Query Result Limits
- [ ] **US-0129**: Performance Monitoring

### AI Service Gaps
- [ ] **US-0130**: AI Performance Monitoring
- [ ] **US-0131**: Follow-Up Feedback Requests
