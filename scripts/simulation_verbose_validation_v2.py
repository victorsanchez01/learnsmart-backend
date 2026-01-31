#!/usr/bin/env python3
"""
Simulación detallada del 'Student Journey' con validación exhaustiva de respuestas.
Verifica la integridad de los datos en cada salto entre microservicios.
"""
import requests
import json
import time
import sys

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
    
        token = response.json().get("access_token")
        print(f"✅ Token obtenido correctamente (Length: {len(token)})")
        return token
    except Exception as e:
        print(f"❌ Excepción obteniendo token: {e}")
        return None

def validate_response(name, response, expected_status=200):
    print(f"\n--- [VALIDANDO {name}] ---")
    print(f"URL: {response.url}")
    print(f"Status: {response.status_code}")
    
    if response.status_code != expected_status:
        print(f"❌ ERROR: Se esperaba {expected_status}, se recibió {response.status_code}")
        print(f"Body: {response.text}")
        return False
    
    try:
        body = response.json()
        print(f"✅ Respuesta JSON válida.")
        # Pretty print for logs
        # print(json.dumps(body, indent=2))
        return body
    except Exception as e:
        print(f"❌ ERROR: La respuesta no es un JSON válido: {e}")
        return False

def run_verbose_journey():
    print("\n🚀 INICIANDO VALIDACIÓN EXHAUSTIVA DEL BACKEND")
    print("=" * 80)
    
    token = get_token()
    if not token:
        sys.exit(1)
    
    headers = {"Authorization": f"Bearer {token}", "Content-Type": "application/json"}
    
    # 1. PROFILE & PROGRESS
    print("\n[Step 1] Profile Service: /profiles/me/progress")
    res = requests.get(f"{GATEWAY_URL}/profiles/me/progress", headers=headers)
    prog = validate_response("Consolidated Progress", res)
    
    if not prog:
        # Intentar registro si no existe
        print("   ⚠️ Usuario no encontrado, registrando...")
        reg_payload = {
            "email": "user1@example.com",
            "password": "password",
            "displayName": "Student User One",
            "locale": "es"
        }
        requests.post(f"{GATEWAY_URL}/profiles", headers=headers, json=reg_payload)
        res = requests.get(f"{GATEWAY_URL}/profiles/me/progress", headers=headers)
        prog = validate_response("Consolidated Progress (Retry)", res)

    assert prog['profile']['userId'] is not None, "userId missing in profile"
    user_uuid = prog['profile']['userId']
    print(f"   👤 Estudiante: {prog['profile']['displayName']} ({user_uuid})")

    # 2. PLANNING - CREATE PLAN
    print("\n[Step 2] Planning Service: POST /planning/plans")
    create_plan_res = requests.post(f"{GATEWAY_URL}/planning/plans", headers=headers, json={
        "userId": user_uuid,
        "goalId": "fullstack-dev",
        "hoursPerWeek": 20
    })
    plan = validate_response("Create Plan", create_plan_res, 201)
    if not plan: sys.exit(1)
    
    plan_id = plan.get("id")
    print(f"   📋 Plan ID: {plan_id}")
    print(f"   🤖 AI Log: {plan.get('rawPlanAi')[:100]}...")
    assert len(plan.get("modules", [])) > 0, "Plan should have generated modules"

    # 3. PLANNING - LIST MODULES
    print(f"\n[Step 3] Planning Service: GET /planning/plans/{plan_id}/modules")
    modules_res = requests.get(f"{GATEWAY_URL}/planning/plans/{plan_id}/modules", headers=headers)
    modules = validate_response("List Modules", modules_res)
    
    print(f"   📦 Cantidad de módulos: {len(modules)}")
    for m in modules:
        print(f"      - [{m['position']}] {m['title']} (Status: {m['status']})")
        assert m['planId'] == plan_id, f"Incorrect planId in module {m['id']}"

    # 4. CONTENT - CATALOG SEARCH
    print("\n[Step 4] Content Service: GET /content/content-items")
    items_res = requests.get(f"{GATEWAY_URL}/content/content-items?size=5", headers=headers)
    catalog = validate_response("Content Catalog", items_res)
    
    assert len(catalog) > 0, "Catalog should not be empty"
    item_id = catalog[0]['id']
    print(f"   📚 Primer item del catálogo: {catalog[0]['title']} ({item_id})")

    # 5. CONTENT - ITEM DETAIL
    print(f"\n[Step 5] Content Service: GET /content/content-items/{item_id}")
    detail_res = requests.get(f"{GATEWAY_URL}/content/content-items/{item_id}", headers=headers)
    detail = validate_response("Content Detail", detail_res)
    
    assert detail['id'] == item_id, "Detail ID mismatch"
    assert 'description' in detail, "Detail should include description/content"
    print(f"   📄 Contenido: {detail['description'][:100]}...")

    # 6. TRACKING - ANALYTICS
    print(f"\n[Step 6] Tracking Service: /analytics/users/{user_uuid}/stats")
    stats_res = requests.get(f"{GATEWAY_URL}/tracking/analytics/users/{user_uuid}/stats", headers=headers)
    stats = validate_response("User Stats", stats_res)
    
    print(f"   ⏱️ Horas totales: {stats.get('totalHours')}")
    print(f"   🔥 Racha: {stats.get('currentStreak')}")

    # 7. ASSESSMENT - MASTERY
    print(f"\n[Step 7] Assessment Service: /skill-mastery")
    mastery_res = requests.get(f"{GATEWAY_URL}/assessment/users/{user_uuid}/skill-mastery", headers=headers)
    mastery = validate_response("Skill Mastery", mastery_res)
    
    print(f"   🎯 Habilidades evaluadas: {len(mastery)}")
    for sm in mastery:
        print(f"      - Skill: {sm.get('skillName')} ({sm.get('skillId')}) -> mastery: {sm.get('mastery')}")
        assert 'skillName' in sm, "Skill name should be enriched"

    print("\n" + "=" * 80)
    print("✨ VALIDACIÓN FINALIZADA CON ÉXITO: Todos los servicios respondieron correctamente.")
    print("=" * 80)

if __name__ == "__main__":
    run_verbose_journey()
