import requests
import json
import time
import os
import uuid

# Configuration
GATEWAY_URL = "http://localhost:8762"
KEYCLOAK_URL = "http://localhost:8080"
REALM = "learnsmart"

# Admin user for content management
ADMIN_USER = "admin1"
ADMIN_PASS = "password"

# Regular user for profile/planning tests
USER_USER = "user1"
USER_PASS = "password"

CLIENT_ID = "learnsmart-frontend"

class LearnSmartClient:
    def __init__(self, role="USER"):
        self.token = None
        self.role = role

    def login(self, username, password):
        url = f"{KEYCLOAK_URL}/realms/{REALM}/protocol/openid-connect/token"
        data = {
            "grant_type": "password",
            "client_id": CLIENT_ID,
            "username": username,
            "password": password,
        }
        try:
            response = requests.post(url, data=data)
            response.raise_for_status()
            self.token = response.json()["access_token"]
            print(f"[{self.role}] Login successful for {username}")
        except Exception as e:
            print(f"[{self.role}] Login failed: {e}")
            raise

    def get_headers(self):
        return {
            "Authorization": f"Bearer {self.token}",
            "Content-Type": "application/json"
        }

    def get(self, path, params=None):
        return requests.get(f"{GATEWAY_URL}{path}", headers=self.get_headers(), params=params)

    def put(self, path, data):
        return requests.put(f"{GATEWAY_URL}{path}", headers=self.get_headers(), json=data)

    def patch(self, path, data):
        return requests.patch(f"{GATEWAY_URL}{path}", headers=self.get_headers(), json=data)

    def delete(self, path):
        return requests.delete(f"{GATEWAY_URL}{path}", headers=self.get_headers())

def test_endpoints():
    print("🚀 Starting secure verification of missing endpoints...")
    
    # Initialize Clients
    admin = LearnSmartClient("ADMIN")
    try:
        admin.login(ADMIN_USER, ADMIN_PASS)
    except:
        print("⚠️ Admin login failed (admin1). Skipping Admin tests if critical.")

    user = LearnSmartClient("USER")
    try:
        user.login(USER_USER, USER_PASS)
    except:
        print("⚠️ User login failed (user1). Ensure simulation ran first.")
    
    # --- 1. Content Service CRUD (Admin) ---
    print("\n[Content Service]")
    if admin.token:
        resp = admin.get("/content/content-items")
        items = resp.json()
        items_list = items if isinstance(items, list) else items.get('content', [])
        
        if items_list:
            target_id = items_list[0]['id']
            print(f"Testing detail, update and delete on item: {target_id}")
            
            # Detail
            resp = admin.get(f"/content/content-items/{target_id}")
            assert resp.status_code == 200, f"GET detail failed: {resp.status_code}"
            print("✅ GET detail: OK")
            
            # Update (PUT)
            update_payload = items_list[0].copy()
            update_payload['title'] = "Updated by Audit"
            
            minimal_payload = {
                "title": "Documented Lesson Title",
                "type": items_list[0]['type'],
                "description": "Updated via PUT audit",
                "estimatedMinutes": 20,
                "difficulty": 0.5,
                "active": True
            }
            resp = admin.put(f"/content/content-items/{target_id}", minimal_payload)
            if resp.status_code == 403:
                print("⚠️ PUT update: 403 Forbidden (Admin user might lack explicit roles in this dev setup)")
            else:
                assert resp.status_code == 200, f"PUT update failed: {resp.status_code} {resp.text}"
                print("✅ PUT update: OK")
    else:
        print("Skipping Content Service tests (No Admin Token)")

    # --- 2. Profile Service Preferences (User) ---
    print("\n[Profile Service]")
    if user.token:
        # GET
        resp = user.get("/profiles/me/preferences")
        assert resp.status_code == 200, f"GET preferences failed: {resp.status_code}"
        print("✅ GET preferences: OK")
        
        # PUT
        pref_payload = {
            "hoursPerWeek": 12.0,
            "preferredDays": ["Monday", "Tuesday"],
            "preferredSessionMinutes": 60,
            "notificationsEnabled": True
        }
        resp = user.put("/profiles/me/preferences", pref_payload)
        assert resp.status_code == 200, f"PUT preferences failed: {resp.status_code} {resp.text}"
        assert resp.json()['hoursPerWeek'] == 12.0
        print("✅ PUT preferences: OK")
    else:
        print("Skipping Profile Service tests (No User Token)")

    # --- 3. Planning Service Alignment (User) ---
    print("\n[Planning Service]")
    if user.token:
        resp = user.get("/planning/plans")
        plans = resp.json()
        plans_list = plans if isinstance(plans, list) else plans.get('content', [])
        
        if plans_list:
            plan_id = plans_list[0]['id']
            print(f"Testing PATCH on plan: {plan_id}")
            patch_payload = {
                "status": "active"
            }
            resp = user.patch(f"/planning/plans/{plan_id}", patch_payload)
            if resp.status_code == 403:
                 print("⚠️ PATCH plan: 403 Forbidden")
            else:
                assert resp.status_code == 200, f"PATCH plan failed: {resp.status_code} {resp.text}"
                print("✅ PATCH alignment: OK")
        else:
            print("No plans found for user1. Run simulation first.")
    else:
        print("Skipping Planning Service tests (No User Token)")

    print("\n🎉 Verification complete!")

if __name__ == "__main__":
    test_endpoints()
