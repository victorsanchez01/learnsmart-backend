import requests
import sys

BASE_URL = "http://localhost:8762"

def verify():
    print("1. Registering User...")
    try:
        reg_response = requests.post(
            f"{BASE_URL}/auth/register",
            json={
                "email": "test.docker@learnsmart.com",
                "password": "password123",
                "displayName": "Docker User",
                "locale": "es-ES"
            }
        )
        print(f"Register Status: {reg_response.status_code}")
        print(f"Register Body: {reg_response.text}")
        
        if reg_response.status_code != 201:
            print("FAILED to register")
            sys.exit(1)
            
        user_data = reg_response.json()
        user_id = user_data.get('userId')
        print(f"User ID: {user_id}")

        print("\n2. Getting Profile...")
        profile_response = requests.get(
            f"{BASE_URL}/profiles/me",
            headers={"X-User-Id": str(user_id)}
        )
        print(f"Profile Status: {profile_response.status_code}")
        print(f"Profile Body: {profile_response.text}")
        
        if profile_response.status_code == 200 and profile_response.json()['displayName'] == "Docker User":
            print("\nSUCCESS: Deployment Verified!")
        else:
            print("\nFAILED: Profile retrieval mismatch")
            sys.exit(1)

    except Exception as e:
        print(f"EXCEPTION: {e}")
        sys.exit(1)

if __name__ == "__main__":
    verify()
