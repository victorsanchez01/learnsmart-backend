#!/usr/bin/env python3
"""
Test endpoints de módulos y actividades en planning-service
"""
import requests
import json
import time

GATEWAY_URL = "http://localhost:8762"
KEYCLOAK_URL = "http://localhost:8080"
REALM = "learnsmart"

def get_token(username="testuser", password="testpass"):
    url = f"{KEYCLOAK_URL}/realms/{REALM}/protocol/openid-connect/token"
    data = {
        "username": username,
        "password": password,
        "grant_type": "password",
        "client_id": "learnsmart-client"
    }
    try:
        response = requests.post(url, data=data)
        response.raise_for_status()
        return response.json()["access_token"]
    except Exception as e:
        print(f"❌ Error obteniendo token: {e}")
        return None

def test_plan_modules():
    print("\n🔍 Test: Planning Modules & Activities")
    print("=" * 60)
    
    token = get_token()
    if not token:
        return
    
    headers = {"Authorization": f"Bearer {token}", "Content-Type": "application/json"}
    
    # 1. Obtener o crear un plan
    print("\n1. Obteniendo planes del usuario...")
    plans_res = requests.get(f"{GATEWAY_URL}/planning/plans?userId=testuser", headers=headers)
    plans = plans_res.json().get("content", [])
    
    plan_id = None
    if not plans:
        print("   No hay planes, creando uno nuevo...")
        new_plan = {
            "userId": "testuser",
            "goalId": "backend-developer",
            "hoursPerWeek": 10
        }
        create_res = requests.post(f"{GATEWAY_URL}/planning/plans", json=new_plan, headers=headers)
        if create_res.status_code == 201:
            plan_id = create_res.json()["id"]
            print(f"   ✅ Plan creado: {plan_id}")
            # Esperar un poco para la generación de la IA
            print("   ⏳ Esperando 5s para generación de módulos por IA...")
            time.sleep(5)
        else:
            print(f"   ❌ Error creando plan: {create_res.status_code}")
            return
    else:
        plan_id = plans[0]["id"]
        print(f"   ✅ Usando plan existente: {plan_id}")

    # 2. Listar módulos ⭐ NUEVO
    print(f"\n2. Listando módulos del plan {plan_id}...")
    modules_res = requests.get(f"{GATEWAY_URL}/planning/plans/{plan_id}/modules", headers=headers)
    print(f"   Status: {modules_res.status_code}")
    
    if modules_res.status_code == 200:
        modules = modules_res.json()
        print(f"   ✅ Encontrados {len(modules)} módulos")
        for m in modules:
            print(f"   - [{m['status']}] {m['title']} (ID: {m['id']})")
        
        if modules:
            module_id = modules[0]['id']
            
            # 3. Actualizar estado de módulo ⭐ NUEVO
            print(f"\n3. Actualizando estado del módulo {module_id} a 'in_progress'...")
            patch_res = requests.patch(
                f"{GATEWAY_URL}/planning/plans/{plan_id}/modules/{module_id}",
                json={"status": "in_progress"},
                headers=headers
            )
            print(f"   Status: {patch_res.status_code}")
            if patch_res.status_code == 200:
                print(f"   ✅ Módulo actualizado: {patch_res.json()['status']}")

            # 4. Listar actividades del módulo ⭐ NUEVO
            print(f"\n4. Listando actividades del módulo {module_id}...")
            activities_res = requests.get(
                f"{GATEWAY_URL}/planning/plans/{plan_id}/activities?moduleId={module_id}",
                headers=headers
            )
            print(f"   Status: {activities_res.status_code}")
            if activities_res.status_code == 200:
                acts = activities_res.json()
                print(f"   ✅ Encontradas {len(acts)} actividades")
                for a in acts:
                    print(f"   - [{a['status']}] {a['activityType']} (Ref: {a['content_ref'] if 'content_ref' in a else a.get('contentRef', 'N/A')})")
                
                if acts:
                    act_id = acts[0]['id']
                    # 5. Actualizar actividad
                    print(f"\n5. Marcando actividad {act_id} como 'completed'...")
                    act_patch = requests.patch(
                        f"{GATEWAY_URL}/planning/plans/{plan_id}/activities/{act_id}",
                        json={"status": "completed", "overrideEstimatedMinutes": 25},
                        headers=headers
                    )
                    if act_patch.status_code == 200:
                        print(f"   ✅ Actividad actualizada!")

if __name__ == "__main__":
    test_plan_modules()
