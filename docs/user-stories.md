# ✅ **BACKLOG DEL SISTEMA DE APRENDIZAJE ADAPTATIVO (LearnSmart)**

*(Derivado directamente de los microservicios reales)*

---

# **ÉPICA 1 — Gestión de Usuario y Perfil Personalizado**

Basado en *profile-service* 

## **Feature 1.1 — Registro y autenticación**

**US-001 – Registro de usuario**
*Como usuario nuevo, quiero registrarme en el sistema para acceder a mi perfil y comenzar un plan de aprendizaje.*
**Criterios de aceptación:**

* El registro debe crear usuario en Keycloak.
* Debe generarse un perfil interno asociado.
  **Endpoints:**
* POST `/auth/register` 

---

## **Feature 1.2 — Gestión del perfil**

**US-002 – Consultar mi perfil**
*Como usuario autenticado, quiero ver mi información personal para validar mis datos generales.*
**Endpoint:** GET `/profiles/me` 

**US-003 – Actualizar mi perfil**
*Como usuario, quiero actualizar mis preferencias, idioma o datos demográficos para mejorar la personalización.*
**Endpoint:** PUT `/profiles/me` 

---

## **Feature 1.3 — Gestión de objetivos de aprendizaje**

**US-004 – Definir objetivos educativos**
*Como usuario, quiero crear uno o más objetivos de aprendizaje para que el sistema ajuste mis planes y actividades.*
**Endpoint:** POST `/profiles/me/goals` 

**US-005 – Actualizar o desactivar objetivos**
*Como usuario, quiero modificar mis metas o pausarlas según mi progreso.*
**Endpoint:** PUT `/profiles/me/goals/{goalId}` 

---

# **ÉPICA 2 — Catalogación y Gestión del Contenido Formativo**

Basado en *content-service* 

## **Feature 2.1 — Gestión de dominios**

**US-010 – Listar dominios disponibles**
*Como usuario, quiero ver los dominios académicos disponibles para navegar el catálogo.*
**Endpoint:** GET `/domains`

**US-011 – Crear dominio (admin)**
*Como administrador, quiero crear dominios para organizar las áreas de conocimiento.*
**Endpoint:** POST `/domains`

---

## **Feature 2.2 — Gestión de skills**

**US-012 – Ver skills de un dominio**
*Como usuario, quiero ver las habilidades asociadas a un dominio para entender la estructura del aprendizaje.*
**Endpoint:** GET `/skills`

**US-013 – Gestionar prerequisitos**
*Como diseñador instruccional, quiero definir prerequisitos para controlar la progresión.*
**Endpoint:** GET/PUT `/skills/{id}/prerequisites`

---

## **Feature 2.3 — Contenidos educativos**

**US-014 – Listar materiales de aprendizaje**
*Como usuario, quiero acceder a las actividades, lecciones o quizzes asociados a mis skills.*
**Endpoint:** GET `/content-items`

**US-015 – Crear y editar contenido (admin/IA)**
*Como creador/IA, quiero generar contenido asociado a una skill.*
**Endpoints:**

* POST `/content-items`
* PUT `/content-items/{id}`

---

# **ÉPICA 3 — Planificación Adaptativa del Aprendizaje**

Basado en *planning-service*  y *ai-service* 

## **Feature 3.1 — Generación del plan**

**US-020 – Generar plan inicial personalizado**
*Como usuario, quiero que el sistema cree un plan adaptado a mis datos, perfil y objetivos.*
**Endpoints:**

* POST `/plans` (planning-service)
* POST `/v1/plans/generate` (ai-service) 

---

## **Feature 3.2 — Replanificación dinámica**

**US-021 – Ajustar plan según nuevos eventos**
*Como usuario, quiero que el sistema reajuste mi plan si avanzo más rápido, fallo actividades o cambio mis objetivos.*
**Endpoints:**

* POST `/plans/{planId}/replan`
* POST `/v1/plans/replan` (IA) 

---

## **Feature 3.3 — Navegación detallada del plan**

**US-022 – Ver módulos del plan**
**Endpoint:** GET `/plans/{planId}/modules`

**US-023 – Actualizar estado de actividades**
**Endpoint:** PATCH `/plans/{planId}/activities/{activityId}`

---

# **ÉPICA 4 — Evaluación Adaptativa y Feedback Instantáneo**

Basado en *assessment-service*  y *ai-service* 

## **Feature 4.1 — Sesiones adaptativas**

**US-030 – Crear sesión de evaluación**
*Como usuario, quiero iniciar una sesión de evaluación que se adapte progresivamente a mi nivel.*
**Endpoint:** POST `/assessment-sessions`

**US-031 – Obtener siguiente ítem adaptativo**
*Como usuario, quiero que la evaluación me presente el siguiente ítem según mi desempeño previo.*
**Endpoints:**

* POST `/assessment-sessions/{sessionId}/next-item`
* POST `/v1/assessments/next-item` (IA) 

---

## **Feature 4.2 — Envío de respuestas y retroalimentación**

**US-032 – Enviar respuesta a un ítem**
**Endpoint:** POST `/assessment-sessions/{sessionId}/responses`

**US-033 – Recibir feedback personalizado inmediato**
*Como usuario, quiero recibir explicaciones y pistas generadas por IA según mi respuesta.*
**Endpoints:**

* POST `/v1/assessments/feedback` (IA) 

---

## **Feature 4.3 — Consultar dominio de habilidades**

**US-034 – Ver dominio por skill**
**Endpoint:** GET `/users/{userId}/skill-mastery/{skillId}`

---

# **ÉPICA 5 — Tracking de Actividad y Analítica de Aprendizaje**

Basado en *tracking-service* 

## **Feature 5.1 — Registro de eventos**

**US-040 – Registrar eventos de aprendizaje**
*Como sistema, necesito guardar eventos significativos para mejorar la adaptatividad y analítica.*
**Endpoint:** POST `/events`

## **Feature 5.2 — Consulta de eventos**

**US-041 – Consultar eventos por usuario, tipo o entidad**
**Endpoint:** GET `/events`

---

# **ÉPICA 6 — Motor de Inteligencia Artificial**

La IA no es un microservicio aislado, sino un **coprocesador** utilizado por planning y assessment.
Basado en *ai-service* 

## **Feature 6.1 — Generación de planes**

US ya definida arriba (US-020)

## **Feature 6.2 — Reajuste del plan**

US ya definida arriba (US-021)

## **Feature 6.3 — Generación de feedback pedagógico**

US ya definida (US-033)


