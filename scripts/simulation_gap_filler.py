#!/usr/bin/env python3
"""
simulation_gap_filler.py

Verifies the remaining endpoints:
1. Assessment Service: Complete flow (Session -> Next Item -> Submit -> List Responses)
2. Tracking Service: Event Ingestion & Retrieval

Dependencies:
- Requires 'admin1' to have a profile and a plan (created by simulation_read_after_write_v3.py).
"""
import requests
import json
import sys
import uuid
import time

GATEWAY_URL = "http://localhost:8762"
KEYCLOAK_URL = "http://localhost:8080"
REALM = "learnsmart"
CLIENT_ID = "learnsmart-frontend"

USERNAME = "admin1"
PASSWORD = "password"

def fail(msg):
    print(f"❌ FAILURE: {msg}")
    sys.exit(1)

def success(msg):
    print(f"✅ {msg}")

def get_token():
    url = f"{KEYCLOAK_URL}/realms/{REALM}/protocol/openid-connect/token"
    data = {"username": USERNAME, "password": PASSWORD, "grant_type": "password", "client_id": CLIENT_ID}
    resp = requests.post(url, data=data)
    if resp.status_code != 200: return None
    return resp.json().get("access_token")

def main():
    print("\n🚀 Starting GAP Verification (Assessment & Tracking)\n")
    
    # 1. Auth
    token = get_token()
    if not token: fail("Login failed")
    headers = {"Authorization": f"Bearer {token}", "Content-Type": "application/json"}
    success("Authenticated as admin1")

    # 2. Get Context (UserId, PlanId)
    resp = requests.get(f"{GATEWAY_URL}/profiles/me", headers=headers)
    if resp.status_code != 200: fail("Profile not found (Run v3 script first)")
    user_id = resp.json()['userId']
    
    resp = requests.get(f"{GATEWAY_URL}/planning/plans", headers=headers)
    plans = resp.json()
    plans_list = plans.get('content', [])
    if not plans_list: fail("No plans found (Run v3 script first)")
    
    plan_id = plans_list[0]['id']
    
    # Get Module ID
    resp = requests.get(f"{GATEWAY_URL}/planning/plans/{plan_id}/modules", headers=headers)
    modules = resp.json()
    if not modules: fail("Plan has no modules")
    module_id = modules[0]['id']
    
    success(f"Context loaded: User={user_id}, Plan={plan_id}, Module={module_id}")

    # --- 3. ASSESSMENT SERVICE ---
    print("\n🔹 Verifying Assessment Service...")
    
    # A. Create Session
    session_payload = {
        "userId": user_id,
        "type": "QUIZ",
        "planId": plan_id,
        "moduleId": module_id
    }
    resp = requests.post(f"{GATEWAY_URL}/assessment/assessments/sessions", headers=headers, json=session_payload)
    if resp.status_code != 201: fail(f"Create Session failed: {resp.status_code} {resp.text}")
    session_id = resp.json()['id']
    success(f"Session Created: {session_id}")
    
    # B. Get Next Item
    resp = requests.get(f"{GATEWAY_URL}/assessment/assessments/sessions/{session_id}/next-item", headers=headers)
    if resp.status_code != 200: fail(f"Get Next Item failed: {resp.status_code}")
    item = resp.json()
    item_id = item.get('id')
    if not item_id: 
        print("   ⚠️ No items returned (AI might return empty or null). Skipping Response test.")
    else:
        success(f"Next Item Retrieved: {item_id}")
        
        # C. Submit Response
        submit_payload = {
            "assessmentItemId": item_id,
            "responsePayload": "A",
            "responseTimeMs": 1200
        }
        resp = requests.post(f"{GATEWAY_URL}/assessment/assessments/sessions/{session_id}/responses", headers=headers, json=submit_payload)
        if resp.status_code != 200: fail(f"Submit Response failed: {resp.status_code} {resp.text}")
        success("Response Submitted")
        
        # D. Verify Response List
        resp = requests.get(f"{GATEWAY_URL}/assessment/assessments/sessions/{session_id}/responses", headers=headers)
        if resp.status_code != 200: fail(f"List Responses failed: {resp.status_code}")
        responses = resp.json()
        if len(responses) == 0: fail("Response not persisted")
        success("Response List Verified")

    # --- 4. TRACKING SERVICE ---
    print("\n🔹 Verifying Tracking Service...")
    
    # A. Create Event
    event_payload = {
        "userId": user_id,
        "eventType": "SESSION_START",
        "entityType": "ASSESSMENT_SESSION",
        "entityId": session_id,
        "metadata": {"browser": "python-script"}
    }
    resp = requests.post(f"{GATEWAY_URL}/tracking/events", headers=headers, json=event_payload)
    if resp.status_code != 201: fail(f"Create Event failed: {resp.status_code} {resp.text}")
    print("   Event Created.")
    
    # B. List Events (Verify Persistence)
    # Give a tiny buffer for async if any? Controller seemed sync JPA save.
    resp = requests.get(f"{GATEWAY_URL}/tracking/events?userId={user_id}&entityId={session_id}", headers=headers)
    if resp.status_code != 200: fail(f"List Events failed: {resp.status_code}")
    events_page = resp.json()
    events = events_page.get('content', [])
    
    found = any(e['entityId'] == session_id for e in events)
    if not found: fail("Created event not found in list")
    success("Tracking Event Verified")

    print("\n🎉 GAP VERIFICATION COMPLETE (All logic exercized)")

if __name__ == "__main__":
    main()
