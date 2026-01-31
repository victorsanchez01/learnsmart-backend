#!/usr/bin/env python3
"""
Test nuevo endpoint GET /content-items/{id} en content-service
"""
import requests
import json

GATEWAY_URL = "http://localhost:8762"
KEYCLOAK_URL = "http://localhost:8080"
REALM = "learnsmart"

def get_token(username="testuser", password="testpass"):
    """Obtener token JWT"""
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

def test_get_content_item_by_id():
    """Prueba GET /content-items/{id}"""
    print("\n🔍 Test: GET /content-items/{id}")
    print("=" * 60)
    
    # Obtener token
    token = get_token()
    if not token:
        print("❌ Sin token, abortando")
        return
    
    headers = {"Authorization": f"Bearer {token}"}
    
    # 1. Listar content items para obtener un ID
    print("\n1. Listando content items...")
    list_url = f"{GATEWAY_URL}/content/content-items?page=0&size=1"
    response = requests.get(list_url, headers=headers)
    
    if response.status_code != 200:
        print(f"❌ Error listando: {response.status_code}")
        print(response.text)
        return
    
    items = response.json()
    if not items:
        print("⚠️  No hay content items disponibles")
        return
    
    content_id = items[0]["id"]
    print(f"✅ Encontrado content item: {content_id}")
    print(f"   Título: {items[0].get('title', 'N/A')}")
    
    # 2. Obtener detalles del item individual ⭐ NUEVO ENDPOINT
    print(f"\n2. Obteniendo detalles de {content_id}...")
    detail_url = f"{GATEWAY_URL}/content/content-items/{content_id}"
    response = requests.get(detail_url, headers=headers)
    
    print(f"   Status: {response.status_code}")
    
    if response.status_code == 200:
        details = response.json()
        print(f"✅ Detalles obtenidos exitosamente!")
        print(f"\n   📄 ID: {details.get('id')}")
        print(f"   📝 Título: {details.get('title')}")
        print(f"   📚 Tipo: {details.get('type')}")
        print(f"   📖 Descripción: {details.get('description', 'N/A')[:50]}...")
        print(f"   ⏱️  Tiempo estimado: {details.get('estimatedMinutes', 'N/A')} min")
        print(f"   🎯 Dificultad: {details.get('difficulty', 'N/A')}")
        if details.get('domain'):
            print(f"   🌐 Dominio: {details['domain'].get('name', 'N/A')}")
        
        print(f"\n   Respuesta completa:")
        print(json.dumps(details, indent=2))
        
    elif response.status_code == 404:
        print(f"❌ Content item no encontrado (404)")
    else:
        print(f"❌ Error: {response.status_code}")
        print(response.text)
    
    # 3. Verificar que usuarios sin autenticación NO puedan acceder
    print(f"\n3. Verificando seguridad (sin token)...")
    response = requests.get(detail_url)  # Sin headers de auth
    print(f"   Status sin auth: {response.status_code}")
    if response.status_code in [401, 403]:
        print(f"✅ Seguridad OK - acceso denegado sin autenticación")
    else:
        print(f"⚠️  Endpoint accesible sin autenticación!")

if __name__ == "__main__":
    test_get_content_item_by_id()
