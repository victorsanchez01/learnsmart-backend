# 1. Visión general del sistema

Plataforma educativa basada en IA con aprendizaje adaptativo.
Tecnologías principales:

* **Frontend**: SPA web/móvil (cliente JS).
* **Auth**: Keycloak (OIDC), JWT.
* **Backend**: microservicios Java/Spring Boot.
* **Service discovery**: Netflix Eureka.
* **Gateway**: Spring Cloud Gateway.
* **IA**: microservicio Python (“ai-service”).
* **Persistencia**: PostgreSQL (una BD por microservicio de dominio).

Objetivo:
Ofrecer rutas de aprendizaje personalizadas, evaluación adaptativa y replanificación continua usando IA, manteniendo consistencia fuerte del plan y del estado del alumno en Postgres.

---

# 2. Componentes y responsabilidades

## 2.1. Frontend

* Interfaz de usuario (onboarding, panel de estudio, actividades, evaluaciones).
* Obtiene JWT de Keycloak vía flujo OIDC.
* Llama exclusivamente al **API Gateway**, nunca directamente a microservicios ni a ai-service.

## 2.2. Keycloak

* Proveedor de identidad (IdP).
* Gestiona usuarios, roles, sesiones y emisión de JWT.
* El claim `sub` del JWT se usa como `user_id` en todos los servicios.

## 2.3. API Gateway (Spring Cloud Gateway)

* Entry point de todo el tráfico HTTP del frontend.
* Valida JWT (firma y expiración).
* Enruta peticiones a microservicios usando Eureka.
* Puede aplicar:

  * rate limiting
  * logging / tracing
  * normalización de cabeceras (por ejemplo, propagar `X-User-Id` extraído del JWT).

## 2.4. Eureka

* Registro de microservicios (service discovery).
* Los microservicios se registran y se resuelven por nombre lógico.
* El Gateway y otros servicios internos consultan Eureka para descubrir instancias.

## 2.5. Microservicios de dominio (Spring Boot + Postgres)

Cada servicio tiene su propia base de datos Postgres (no hay FKs cross-service; solo IDs lógicos).

### 2.5.1. `profile-service` (+ `profile-db`)

Responsabilidades:

* Perfil estable del usuario:

  * datos básicos de perfil (no sensibles)
  * preferencias de estudio
  * objetivos de aprendizaje
* API para leer/actualizar perfil, objetivos y preferencias.

Datos (resumen):

* `user_profiles`
* `user_goals`
* `user_study_preferences`

### 2.5.2. `content-service` (+ `content-db`)

Responsabilidades:

* Catálogo de **skills** y contenido educativo (lecciones, prácticas, etc.).
* Define la taxonomía de habilidades y prerequisitos.

Datos:

* `domains`
* `skills`, `skill_prerequisites`
* `content_items`, `content_item_skills`

### 2.5.3. `planning-service` (+ `planning-db`)

Responsabilidades:

* **Fuente de verdad** de la ruta de aprendizaje del usuario.
* Gestiona:

  * planes de aprendizaje
  * módulos del plan
  * actividades del plan
* Consume:

  * Perfil y objetivos de `profile-service`.
  * Catálogo y skills de `content-service`.
  * Estado de dominio de `assessment-service`.
  * Motor de IA (`ai-service`) para generar o ajustar planes.

Datos:

* `learning_plans`
* `plan_modules`
* `plan_activities`
* Campo `raw_plan_ai` (JSON) para guardar la respuesta original de IA.

### 2.5.4. `assessment-service` (+ `assessment-db`)

Responsabilidades:

* Evaluación adaptativa.
* Banco de ítems (siempre o parcialmente generados por IA).
* Seguimiento de sesiones de evaluación.
* Estado de dominio por skill (`user_skill_mastery`).

Datos:

* `assessment_items`, `assessment_item_skills`, `assessment_item_options`
* `user_assessment_sessions`
* `user_item_responses`
* `user_skill_mastery`

Expose:

* API para que el frontend obtenga la **siguiente pregunta**.
* API para recibir respuestas del usuario, calcular corrección y feedback.
* API para exponer estado de dominio a `planning-service`.

### 2.5.5. `tracking-service` (+ `tracking-db`) (opcional)

Responsabilidades:

* Registro genérico de eventos de aprendizaje y uso.
* No interviene en la lógica online del plan, pero sirve para analytics y training futuros.

Datos:

* `learning_events`

---

## 2.6. `ai-service` (Python)

Responsabilidades:

* Motor IA stateless.
* No expone API al frontend; solo a otros servicios backend.
* Implementa lógica basada en LLM (u otros modelos) para:

1. **Generación / ajuste de plan** (llamado por `planning-service`).

   * Entrada: perfil, objetivos, constraints (tiempo/fecha), skills y contenidos disponibles, estado dominante.
   * Salida: estructura de plan (módulos y actividades) en JSON.

2. **Evaluación adaptativa** (llamado por `assessment-service`).

   * Entrada: skill objetivo, historial de respuestas, dificultad deseada, etc.
   * Salida: ítem(s) de evaluación (enunciado, opciones, tags de error, feedback sugerido).
   * Opcional: recomendación de actualización de `mastery`.

Persistencia:

* No es dueño de datos de dominio.
* Puede tener almacenamiento propio solo para logs técnicos, cachés o modelos, sin exponerlo como fuente de verdad.

---

# 3. Propiedad de datos y consistencia

* Cada microservicio es **dueño exclusivo** de su modelo de datos.
* No hay joins entre bases de datos; la integración es vía APIs.
* `planning-service` y `assessment-service` son los responsables principales del **estado dinámico** del aprendizaje.

Reglas clave:

* La ruta de aprendizaje en curso está **persistida** en `planning-db`.
* El estado de dominio (`user_skill_mastery`) está persistido en `assessment-db`.
* El ai-service puede proponer cambios, pero **nunca escribe directamente en Postgres**:

  * siempre a través de los servicios de dominio que validan y persisten.

---

# 4. Flujos principales

## 4.1. Onboarding + generación de plan inicial

1. Usuario se registra / loguea en el Frontend (Keycloak).
2. Frontend llama al Gateway con JWT.
3. Gateway enruta a `profile-service`:

   * alta/actualización de perfil, objetivos y preferencias.
4. `planning-service` recibe una petición de “generar plan inicial”:

   * Lee perfil/objetivos de `profile-service`.
   * Lee skills y contenidos relevantes de `content-service`.
   * (Opcional) lee estado de dominio inicial de `assessment-service`.
   * Llama a `ai-service` con toda la información.
5. `ai-service` devuelve JSON con el plan propuesto.
6. `planning-service` valida, normaliza y persiste en `planning-db`.
7. El Frontend consulta el plan a `planning-service` vía Gateway.

## 4.2. Sesión de estudio / evaluación

1. Frontend muestra la actividad actual del plan.
2. Si la actividad es evaluativa:

   * Frontend pide la **siguiente pregunta** a `assessment-service`.
   * `assessment-service` decide si:

     * reutiliza ítems existentes, o
     * pide un ítem nuevo a `ai-service`.
   * El ítem se devuelve al Frontend.
3. Usuario responde:

   * Frontend envía respuesta a `assessment-service`.
   * `assessment-service` corrige, actualiza `user_item_responses` y `user_skill_mastery`.
   * Devuelve feedback al Frontend.
4. Cuando se completa un bloque/módulo:

   * `assessment-service` expone estado de dominio a `planning-service`.
   * `planning-service` puede marcar módulo como completado o solicitar replanificación a `ai-service`.

## 4.3. Replanificación del aprendizaje

1. Trigger (manual del usuario o automático) → “ajustar plan”.
2. `planning-service` lee:

   * Plan actual (`planning-db`).
   * Perfil/objetivos (`profile-service`).
   * Estado de dominio (`assessment-service`).
3. Llama a `ai-service` con el contexto.
4. `ai-service` devuelve modificaciones:

   * nuevos módulos/actividades
   * cambios de orden
   * marcados de refuerzo
5. `planning-service` aplica las modificaciones, actualiza `planning-db` y mantiene un registro de versiones si es necesario.

---

# 5. Seguridad

* Autenticación:

  * Keycloak como IdP.
  * JWT firmado; verificación en Gateway y/o microservicios.
* Autorización:

  * Basada en roles / claims del JWT.
  * Cada microservicio aplica reglas de autorización para sus endpoints.
* Comunicación:

  * Frontend ↔ Gateway: HTTPS.
  * Gateway ↔ microservicios / microservicios entre sí: típicamente HTTP interno/HTTPS dependiendo del entorno.

---

# 6. Integración entre servicios

## 6.1. Estilo de comunicación

* **Sync** (principal): REST/HTTP+JSON.
* Descubrimiento: Eureka.
* Contratos:

  * Cada microservicio expone su OpenAPI (OAS) ya definida.
  * El generador de código debe usar esos contratos como fuente de verdad para clientes.

## 6.2. Ejemplos de relaciones principales

* `planning-service` → `profile-service`

  * Lectura de perfil y objetivos (GET).
* `planning-service` → `content-service`

  * Lectura de skills y contenidos (GET).
* `planning-service` → `assessment-service`

  * Lectura de `user_skill_mastery` y/o métricas agregadas.
* `planning-service` → `ai-service`

  * Generación / ajuste de plan (POST JSON).
* `assessment-service` → `ai-service`

  * Generación de ítems y feedback (POST JSON).
* `assessment-service` → `tracking-service`

  * Emisión de eventos de evaluación (POST).

---

# 7. Instrucciones específicas para el agente generador de código

1. **Respetar bounded contexts**

   * No generar código que acceda directamente a datos de otro servicio.
   * Toda comunicación cross-service debe usar las APIs definidas en su OAS.

2. **Uso de OAS**

   * Las definiciones OpenAPI existentes son contractuales.
   * El generador de código debe:

     * Crear controladores/handlers que cumplan esos contratos.
     * Generar clientes HTTP para consumir otros servicios basándose en sus OAS.

3. **Persistencia**

   * Cada microservicio usa su propia conexión a Postgres y su propio esquema.
   * No crear FKs a tablas de otros servicios.

4. **Auth**

   * Suponer que las peticiones entrantes ya traen un JWT validado en el Gateway.
   * Extraer `user_id` del token (o cabecera normalizada) según la configuración que se defina.
   * No interactuar directamente con Keycloak salvo en servicios específicamente de administración.

5. **ai-service**

   * Tratarlo como servicio HTTP externo interno:

     * No acceder a su estado interno.
     * Usar endpoints de alto nivel (`/plan/generate`, `/assessment/generate-item`, etc.) según su OAS.

6. **Observabilidad / logging**

   * Incluir trazas mínimas: correlation ID, user_id, servicio llamante.
   * No mezclar lógica de dominio con logging.


