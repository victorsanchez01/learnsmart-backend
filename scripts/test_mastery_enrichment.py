#!/usr/bin/env python3
"""
Test endpoint de mastery enriquecido en assessment-service
"""
import requests
import json
import uuid

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

def test_enriched_mastery():
    print("\n🔍 Test: Enriched Skill Mastery")
    print("=" * 60)
    
    token = get_token()
    if not token:
        return
    
    headers = {"Authorization": f"Bearer {token}"}
    
    # 1. Obtener ID de usuario (testuser)
    # Por sencillez usamos un UUID aleatorio o si ya conocemos uno
    user_id = "testuser_id" # Esto debería ser el UUID de Keycloak
    
    # En la simulación real usamos el ID devuelto por Keycloak.
    # Para el test, intentamos obtener el mastery de 'me' si existiera o listamos.
    
    print("\n1. Consultando mastery de 'me' (si el endpoint existiera) o de un ID conocido...")
    # El endpoint es /assessment/users/{userId}/skill-mastery
    # Primero intentamos obtener el perfil para tener el ID real
    profile_res = requests.get(f"{GATEWAY_URL}/profile/profiles/me", headers=headers)
    if profile_res.status_code == 200:
        actual_user_id = profile_res.json()["userId"]
        print(f"   ✅ Usuario ID: {actual_user_id}")
        
        print(f"\n2. Consultando mastery enriquecido para {actual_user_id}...")
        mastery_url = f"{GATEWAY_URL}/assessment/users/{actual_user_id}/skill-mastery"
        response = requests.get(mastery_url, headers=headers)
        
        print(f"   Status: {response.status_code}")
        if response.status_code == 200:
            mastery = response.json()
            print(f"   ✅ Encontrados {len(mastery)} registros de mastery")
            if mastery:
                for m in mastery:
                    print(f"   🎯 Skill: {m.get('skillName', 'N/A')} ({m.get('skillId')})")
                    print(f"      Dominio: {m.get('domainName', 'N/A')}")
                    print(f"      Mastery: {m.get('mastery')}")
                
                print(f"\n   Respuesta completa (primera entrada):")
                print(json.dumps(mastery[0], indent=2))
            else:
                print("   ⚠️  No hay datos de mastery para este usuario aún.")
        else:
            print(f"   ❌ Error: {response.status_code}")
            print(response.text)
    else:
        print(f"   ❌ No se pudo obtener el perfil: {profile_res.status_code}")

if __name__ == "__main__":
    test_enriched_mastery()
