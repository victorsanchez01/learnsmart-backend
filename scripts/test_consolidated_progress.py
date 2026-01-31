#!/usr/bin/env python3
"""
Test endpoint de progreso consolidado en profile-service
"""
import requests
import json

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

def test_consolidated_progress():
    print("\n🔍 Test: Consolidated User Progress")
    print("=" * 60)
    
    token = get_token()
    if not token:
        return
    
    headers = {"Authorization": f"Bearer {token}"}
    
    print("\n1. Consultando progreso consolidado de 'me'...")
    progress_url = f"{GATEWAY_URL}/profile/profiles/me/progress"
    response = requests.get(progress_url, headers=headers)
    
    print(f"   Status: {response.status_code}")
    if response.status_code == 200:
        progress = response.json()
        print(f"   ✅ Respuesta de progreso recibida")
        
        profile = progress.get("profile", {})
        print(f"   👤 Usuario: {profile.get('displayName')} ({profile.get('userId')})")
        
        goals = progress.get("goals", [])
        print(f"   🎯 Goles: {len(goals)}")
        for g in goals:
            print(f"      - {g.get('title')}")
            
        plan = progress.get("currentPlan", {})
        if plan:
            print(f"   📅 Plan Actual: {plan.get('status')} ({plan.get('overallPercentage')}% completado)")
            print(f"      Módulos: {plan.get('completedModules')}/{plan.get('totalModules')}")
        else:
            print("   📅 Plan Actual: Ninguno")
            
        skills = progress.get("skillsInProgress", [])
        print(f"   📊 Skills en progreso: {len(skills)}")
        for s in skills:
            print(f"      - {s.get('skillName')}: {s.get('mastery')}")
            
        activity = progress.get("activity", {})
        if activity:
            print(f"   📈 Actividad: {activity.get('totalHours')}h totales, Racha: {activity.get('currentStreak')} días")
            
        print(f"\n   Respuesta completa:")
        print(json.dumps(progress, indent=2))
    else:
        print(f"   ❌ Error: {response.status_code}")
        print(response.text)

if __name__ == "__main__":
    test_consolidated_progress()
