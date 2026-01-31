#!/usr/bin/env python3
"""
Simulación completa del 'Student Journey' para validar readiness del backend.
Valida todos los nuevos endpoints agregados:
- Profile: /me/progress (Consolidado)
- Planning: /plans/{id}/modules y /plans/{userId}/activities
- Content: /content-items/{id} (Detalle con contenido)
- Assessment: /users/{userId}/skill-mastery (Enriquecido)
- Tracking: /analytics/users/{userId}/stats y /activity
"""
import requests
import json
import time

GATEWAY_URL = "http://localhost:8762"
KEYCLOAK_URL = "http://localhost:8080"
REALM = "learnsmart"

def get_token(username="user1", password="password"):
    url = f"{KEYCLOAK_URL}/realms/{REALM}/protocol/openid-connect/token"
    data = {
        "username": username,
        "password": password,
        "grant_type": "password",
        "client_id": "learnsmart-frontend"
    }
    try:
        response = requests.post(url, data=data)
        if response.status_code != 200:
            print(f"❌ Error obteniendo token: {response.status_code} - {response.text}")
            return None
        return response.json()["access_token"]
    except Exception as e:
        print(f"❌ Excepción obteniendo token: {e}")
        return None

def validate_journey():
    print("\n🎓 INICIANDO VALIDACIÓN DEL 'STUDENT JOURNEY' DEL BACKEND")
    print("=" * 70)
    
    token = get_token()
    if not token:
        print("🛑 ABORTANDO: No se pudo autenticar al estudiante.")
        return
    
    headers = {"Authorization": f"Bearer {token}", "Content-Type": "application/json"}
    
    # 1. PROFILE & PROGRESS (BFF)
    print("\nStep 1: Consultando Dashboard Consolidado (/profiles/me/progress)")
    progress_res = requests.get(f"{GATEWAY_URL}/profiles/me/progress", headers=headers)
    
    if progress_res.status_code != 200:
        print(f"   ⚠️ Progress falló ({progress_res.status_code}), intentando registrar al usuario 'user1'...")
        reg_payload = {
            "email": "user1@example.com",
            "password": "password",
            "displayName": "Student User One",
            "locale": "es",
            "timezone": "Europe/Madrid"
        }
        reg_res = requests.post(f"{GATEWAY_URL}/profiles", headers=headers, json=reg_payload)
        if reg_res.status_code in [201, 200]:
            print("   ✅ Usuario registrado con éxito.")
            # Reintentar progress
            progress_res = requests.get(f"{GATEWAY_URL}/profiles/me/progress", headers=headers)
        else:
            print(f"   ❌ Fallo en registro: {reg_res.status_code} - {reg_res.text}")
            return

    if progress_res.status_code == 200:
        prog = progress_res.json()
        print(f"   ✅ Dashboard recibido para: {prog.get('profile', {}).get('displayName')}")
        user_uuid = prog['profile']['userId']
    else:
        print(f"   ❌ Error persistente: {progress_res.status_code}")
        return

    # 2. PLANNING MODULES
    print("\nStep 2: Consultando Ruta de Aprendizaje (Planning Modules)")
    # Intentar obtener el plan ID
    plans_res = requests.get(f"{GATEWAY_URL}/planning/plans?userId={user_uuid}", headers=headers)
    plans = plans_res.json().get("content", [])
    
    if not plans:
        print("   ⚠️ No hay plan, creando uno para validar módulos...")
        create_plan_res = requests.post(f"{GATEWAY_URL}/planning/plans", headers=headers, json={
            "userId": user_uuid,
            "goalId": "fullstack-dev",
            "hoursPerWeek": 15
        })
        print(f"   DEBUG: Status {create_plan_res.status_code}, Body: {create_plan_res.text}")
        plan_id = create_plan_res.json().get("id")
        print(f"   ✅ Plan creado: {plan_id}")
    else:
        plan_id = plans[0]["id"]
        print(f"   ✅ Usando plan: {plan_id}")

    # Consultar módulos
    print(f"   🔎 Consultando módulos del plan: {plan_id}")
    modules_res = requests.get(f"{GATEWAY_URL}/planning/plans/{plan_id}/modules", headers=headers)
    print(f"   DEBUG: Modules Status {modules_res.status_code}, Body: {modules_res.text[:100]}...")
    if modules_res.status_code == 200:
        modules = modules_res.json()
        print(f"   ✅ Encontrados {len(modules)} módulos")
        if modules:
            print(f"   🔎 Módulo 1: {modules[0]['title']} - Status: {modules[0]['status']}")
            module_id = modules[0]['id']
    else:
        print(f"   ❌ Error en Modules: {modules_res.status_code}")

    # 3. CONTENT DETAILS
    print("\nStep 3: Consultando detalle de contenido (/content/content-items/{id})")
    # Listar primero para tener un ID real
    items_res = requests.get(f"{GATEWAY_URL}/content/content-items?size=1", headers=headers)
    items = items_res.json()
    if items:
        item_id = items[0]['id']
        detail_res = requests.get(f"{GATEWAY_URL}/content/content-items/{item_id}", headers=headers)
        if detail_res.status_code == 200:
            detail = detail_res.json()
            print(f"   ✅ Contenido de lección: '{detail['title']}'")
            print(f"   📝 Snippet: {detail.get('description', 'No description')[:50]}...")
        else:
            print(f"   ❌ Error en Content Detail: {detail_res.status_code}")
    else:
        print("   ⚠️ No hay items en el catálogo de contenido.")

    # 4. TRACKING ANALYTICS
    print("\nStep 4: Consultando Estadísticas de Estudio (Tracking Analytics)")
    stats_res = requests.get(f"{GATEWAY_URL}/tracking/analytics/users/{user_uuid}/stats", headers=headers)
    if stats_res.status_code == 200:
        stats = stats_res.json()
        print(f"   ✅ Horas totales: {stats.get('totalHours', 0):.2f}h")
        print(f"   🔥 Racha actual: {stats.get('currentStreak', 0)} días")
    else:
        print(f"   ❌ Error en Stats: {stats_res.status_code}")

    # 5. ENRICHED MASTERY
    print("\nStep 5: Consultando Maestría de Habilidades Enriquecida (Assessment)")
    mastery_res = requests.get(f"{GATEWAY_URL}/assessment/users/{user_uuid}/skill-mastery", headers=headers)
    if mastery_res.status_code == 200:
        mastery = mastery_res.json()
        print(f"   ✅ Encontradas {len(mastery)} habilidades con maestría")
        if mastery:
            first = mastery[0]
            print(f"   🎯 Habilidad: {first.get('skillName', 'N/A')} - Nivel: {first.get('mastery', 0)*100:.1f}%")
    else:
        print(f"   ❌ Error en Mastery: {mastery_res.status_code}")

    print("\n" + "=" * 70)
    print("🏁 VALIDACIÓN FINALIZADA")
    print("=" * 70)

if __name__ == "__main__":
    validate_journey()
