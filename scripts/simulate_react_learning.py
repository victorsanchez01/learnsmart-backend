import requests
import json
import time
import os
import uuid
from datetime import datetime

# Configuration
GATEWAY_URL = os.getenv("GATEWAY_URL", "http://localhost:8762")
KEYCLOAK_URL = os.getenv("KEYCLOAK_URL", "http://localhost:8080")
REALM = "learnsmart"
ADMIN_USER = "admin1"
ADMIN_PASS = "password"
CLIENT_ID = "learnsmart-frontend"
CLIENT_SECRET = "" # Public client doesn't need secret

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
            if hasattr(e, 'response') and e.response:
                print(e.response.text)
            raise

    def get_headers(self):
        return {
            "Authorization": f"Bearer {self.token}",
            "Content-Type": "application/json"
        }

    def post(self, path, data):
        response = requests.post(f"{GATEWAY_URL}{path}", headers=self.get_headers(), json=data)
        return self._handle_response(response)

    def get(self, path, params=None):
        response = requests.get(f"{GATEWAY_URL}{path}", headers=self.get_headers(), params=params)
        return self._handle_response(response)

    def _handle_response(self, response):
        # Log response details
        print(f"  → Response {response.status_code} from {response.url}")
        
        if response.status_code in [200, 201, 204]:
            if response.content:
                data = response.json()
                # Print first 200 chars of response for verification
                response_str = str(data)
                if len(response_str) > 200:
                    print(f"  ← Data: {response_str[:200]}...")
                else:
                    print(f"  ← Data: {response_str}")
                return data
            return None
        msg = f"Request failed: {response.status_code} {response.text}"
        print(msg)
        # Write to file for debug
        try:
            with open("error.txt", "w") as f:
                f.write(msg)
        except:
            pass
        response.raise_for_status()


def run_simulation():
    print("=== STARTING REACT LEARNING SIMULATION ===")

    # 1. Admin Setup: Populate Content
    admin = LearnSmartClient("ADMIN")
    admin.login(ADMIN_USER, ADMIN_PASS)

    # 1.1 Create Domain
    print("\n[Admin] Check/Create Domain 'web-dev'...")
    domains = admin.get("/content/domains", params={"code": "web-dev"})
    # Handle list or paged
    domain_list = domains if isinstance(domains, list) else domains.get('content', [])
    
    if domain_list:
        domain_id = domain_list[0]['id']
        print(f"Domain exists: {domain_id}")
    else:
        domain_data = {
            "code": "web-dev",
            "name": "Web Development",
            "description": "Frontend, Backend and DevOps"
        }
        res = admin.post("/content/domains", domain_data)
        domain_id = res['id']
        print(f"Domain created: {domain_id}")

    # 1.2 Create Skills
    skills_data = [
        {"code": "html-css", "name": "HTML & CSS Basics", "level": "BEGINNER"},
        {"code": "js-es6", "name": "JavaScript ES6", "level": "INTERMEDIATE"},
        {"code": "react-basics", "name": "React Basics", "level": "INTERMEDIATE"},
    ]
    
    skill_ids = {} # code -> id

    for s in skills_data:
        # Check if exists
        existing = admin.get("/content/skills", params={"code": s["code"]})
        existing_list = existing if isinstance(existing, list) else existing.get('content', [])

        if existing_list:
            skill_id = existing_list[0]['id']
            skill_ids[s['code']] = skill_id
            print(f"Skill {s['code']} exists: {skill_id}")
        else:
            s_payload = {
                "domainId": domain_id,
                "code": s["code"],
                "name": s["name"],
                "description": f"Learn {s['name']}",
                "level": s["level"],
                "tags": ["frontend", s["code"]]
            }
            res = admin.post("/content/skills", s_payload)
            skill_ids[s['code']] = res['id']
            print(f"Skill {s['code']} created: {res['id']}")

    # 1.3 Create Content Items (Lessons)
    # create one lesson for React
    react_lesson_payload = {
        "domainId": domain_id,
        "type": "lesson",
        "title": "Introduction to Components",
        "description": "Learn what React components are",
        "estimatedMinutes": 15,
        "difficulty": 0.3,
        "metadata": {"format": "markdown", "body": "# Components\nReact is about components..."},
        "isActive": True
    }
    # Check dupes? simplified: just create
    print("\n[Admin] Creating React Lesson...")
    lesson = admin.post("/content/content-items", react_lesson_payload)
    lesson_id = lesson['id']
    # Link to skill
    admin.post(f"/content/content-items/{lesson_id}/skills", [{"skillId": skill_ids["react-basics"], "weight": 1.0}])

    # 1.4 Create Assessment Items (Questions)
    print("[Admin] Creating React Quiz Question...")
    question_payload = {
        "domainId": domain_id,
        "type": "multiple_choice",
        "stem": "What is the hook to manage state in functional components?",
        "origin": "static",
        "difficulty": 0.4,
        "metadata": "{}",
        "skills": [{"skillId": skill_ids["react-basics"], "weight": 1.0}],
        "options": [
            {"text": "useEffect", "isCorrect": False},
            {"text": "useState", "isCorrect": True, "feedback": "Correct! useState returns a pair..."},
            {"text": "useReducer", "isCorrect": False},
            {"text": "this.setState", "isCorrect": False}
        ]
    }
    # For simplicity, we just create one question.
    question = admin.post("/assessment/assessment-items", question_payload)
    question_id = question['id']
    print(f"Question created: {question_id}")


    # 2. User Journey
    # 2.1 Register
    timestamp = int(time.time())
    username = f"student_{timestamp}"
    email = f"{username}@example.com"
    password = "password123"
    
    print(f"\n[User] Registering {username}...")
    # Registration is an open endpoint or done via Keycloak + profile. 
    # API: POST /profile-service/auth/register (via gateway: /profiles/auth/register ?)
    # Oas says /auth/register in profile-service. Gateway route: - id: profile-auth uri: lb://profile-service predicates: - Path=/auth/**
    # Let's try POST {GATEWAY_URL}/auth/register
    
    register_payload = {
        "email": email,
        "password": password,
        "displayName": "React Student"
    }
    # Manually create user in Keycloak using MASTER credentials (to ensure permissions)
    print(f"\n[System] Creating Keycloak user {username}...")
    
    # Get Master Token
    master_token_url = f"{KEYCLOAK_URL}/realms/master/protocol/openid-connect/token"
    master_data = {
        "username": "admin",
        "password": "admin",
        "grant_type": "password",
        "client_id": "admin-cli"
    }
    try:
        mt_res = requests.post(master_token_url, data=master_data)
        mt_res.raise_for_status()
        master_token = mt_res.json()['access_token']
        
        kc_url = f"{KEYCLOAK_URL}/admin/realms/{REALM}/users"
        kc_payload = {
            "username": username,
            "email": email,
            "enabled": True,
            "emailVerified": True,
            "firstName": "React",
            "lastName": "Student",
            "credentials": [{
                "type": "password",
                "value": password,
                "temporary": False
            }]
        }
        
        kc_res = requests.post(kc_url, json=kc_payload, headers={"Authorization": f"Bearer {master_token}"})
        kc_user_id = None
        if kc_res.status_code == 201:
             print(f"Keycloak user created.")
             # Get the user ID from Location header
             location = kc_res.headers.get('Location', '')
             if location:
                 kc_user_id = location.split('/')[-1]
                 print(f"Keycloak User ID: {kc_user_id}")
        elif kc_res.status_code == 409:
             print("Keycloak user already exists.")
             # Look up the user to get ID
             users_res = requests.get(f"{kc_url}?username={username}", headers={"Authorization": f"Bearer {master_token}"})
             if users_res.status_code == 200 and users_res.json():
                 kc_user_id = users_res.json()[0]['id']
                 print(f"Retrieved existing Keycloak User ID: {kc_user_id}")
        else:
             print(f"Keycloak creation failed: {kc_res.text}")
             kc_res.raise_for_status()
    except Exception as e:
        print(f"Keycloak user creation exception: {e}")
        # If this fails, login below will likely fail too, but we proceed to try.

    # Register profile as admin (with JWT containing the Keycloak user ID)
    # We need to login as the student first to get their JWT
    print(f"[STUDENT] Logging in to create profile with correct authUserId...")
    student_temp = LearnSmartClient("STUDENT_TEMP")
    student_temp.login(username, password)  # This gets their JWT
    try:
        profile_res = student_temp.post("/profiles", register_payload)
        print(f"Profile created successfully")
    except Exception as e:
        print(f"Profile creation failed: {e}")




    # 2.2 Login
    student = LearnSmartClient("STUDENT")
    student.login(username, password)
    
    # 2.3 Create Goal
    print("\n[User] Creating Learning Goal 'Master React'...")
    goal_payload = {
        "title": "Master React",
        "description": "I want to be a frontend dev",
        "domain": "web-dev",
        "targetLevel": "INTERMEDIATE",
        "intensity": "standard"
    }
    goal = student.post("/profiles/me/goals", goal_payload)
    goal_id = goal['id']
    print(f"Goal created: {goal_id}")

    # 2.4 Generate Plan
    print("[User] Generating Learning Plan...")
    plan_req = {
        "userId": student.get("/profiles/me")['userId'],
        "goalId": goal_id,
        "hoursPerWeek": 5
    }
    plan = student.post("/planning/plans", plan_req)
    print(f"DEBUG: Plan response: {plan}")
    plan_id = plan['id'] if plan and 'id' in plan else None
    modules = plan.get('modules', []) if plan else []
    print(f"Plan generated: {plan_id} with {len(modules) if modules else 0} modules")



    # 2.5 Study Phase: Track an event
    print("[User] Reading Lesson (Tracking Event)...")
    event_payload = {
        "userId": student.get("/profiles/me")['userId'],
        "eventType": "content_view",
        "entityType": "content_item",
        "entityId": lesson_id,
        "payload": json.dumps({"durationSeconds": 120})
    }
    student.post("/tracking/events", event_payload)


    # 2.6 Assessment Phase
    print("[User] Taking Assessment Session...")
    session_payload = {
        "userId": student.get("/profiles/me")['userId'], # Self ID
        "type": "practice",
        "config": json.dumps({"domainId": domain_id}) # Focus on domain
    }
    session = student.post("/assessment/assessments/sessions", session_payload)
    session_id = session['id']
    print(f"Session started: {session_id}")

    # Fail-safe loop to find our question or just verify flow
    # In a real adaptive system, we might not get the exact question we created if there are others.
    # But we seeded only one or few.
    
    # Get Next Item
    next_item_res =student.get(f"/assessment/assessments/sessions/{session_id}/next-item")
    if next_item_res is None or next_item_res.get('text') is None:
        print("Session already done (no items?).")
    else:
        item = next_item_res  # Response IS the AssessmentItem
        print(f"Got item: {item['text']}")
        
        # Submit Answer (Find correct option)
        correct_opt = next((o for o in item['options'] if o['statement'] == "useState" or o['isCorrect']), None)
        # Note: 'isCorrect' is NOT hidden in the OAS schema provided? 
        # Wait, the OAS for AssessmentItem shows 'isCorrect' in 'options'. 
        # Typically this should be hidden from student, but for simulation/MVP it might be exposed or we are admin-like?
        # Actually in mapped DTOs for student 'isCorrect' should be hidden. 
        # If it's hidden, we select by label "B" which we know is right.
        
        selected_opt_id = None
        if 'isCorrect' in item['options'][0]:
             # It seems exposed in current DTO/Serializer - Valid for MVP debugging
             selected_opt_id = correct_opt['id']
             print("Found correct option via payload inspection.")
        else:
             print("isCorrect hidden. Picking option with 'useState' text.")
             selected_opt_id = next((o['id'] for o in item['options'] if "useState" in o['statement']), None)

        submit_payload = {
            "assessmentItemId": item['id'],
            "selectedOptionId": selected_opt_id,
            "responseTimeMs": 5000
        }
        res_feedback = student.post(f"/assessment/assessments/sessions/{session_id}/responses", submit_payload)
        print(f"Response submitted. Correct? {res_feedback['isCorrect']}")

    # 2.7 Verify Mastery
    # Mastery updates might be async or sync. In this MVP it is likely sync or near-sync.
    print("[User] Verifying Mastery...")
    user_id = session['userId']
    mastery_list = student.get(f"/assessment/users/{user_id}/skill-mastery")
    
    react_mastery = next((m for m in mastery_list if m['skillId'] == skill_ids['react-basics']), None)
    
    if react_mastery:
        print(f"Mastery for React Basics: {react_mastery['mastery']}")
        if react_mastery['mastery'] > 0:
            print("SUCCESS: Mastery increased!")
        else:
            print("WARNING: Mastery is 0 (maybe logic requires more attempts or config adjustment).")
    else:
        print("WARNING: No mastery record found for React Basics.")

    print("\n=== SIMULATION COMPLETED ===")

if __name__ == "__main__":
    run_simulation()
