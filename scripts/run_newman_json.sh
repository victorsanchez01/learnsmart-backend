#!/bin/bash
# run_newman_json.sh
# Runs the LearnSmart Newman collection and exports a full JSON report.

COLLECTION_FILE="docs/LearnSmart_Newman.postman_collection.json"

if [ ! -f "$COLLECTION_FILE" ]; then
    echo "Error: Collection file $COLLECTION_FILE not found."
    exit 1
fi

echo "Waiting for Keycloak to be fully ready at http://localhost:8080..."
while ! curl -s http://localhost:8080/realms/master > /dev/null; do
    echo "Keycloak not ready yet... sleeping for 5 seconds."
    sleep 5
done
echo "Keycloak is accepting requests! Waiting an additional 10 seconds for internal startup to settle..."
sleep 10
echo "Keycloak is up!"

echo "Waiting for backend services to register with Eureka API Gateway..."
while curl -s -o /dev/null -w "%{http_code}" http://localhost:8762/content/v3/api-docs | grep -q "503"; do
    echo "Gateway returning 503 (Microservices still booting). Sleeping for 10 seconds..."
    sleep 10
done
echo "Content Service is reachable via Gateway! Waiting 10 more seconds for remaining apps..."
sleep 10

echo "Running Integration Tests via Newman..."
newman run "$COLLECTION_FILE" \
  --env-var "gateway_url=http://localhost:8762" \
  --env-var "keycloak_url=http://localhost:8080" \
  --env-var "ai_service_url=http://localhost:8000" \
  --env-var "realm=learnsmart" \
  --env-var "client_id=learnsmart-frontend" \
  --env-var "admin_user=admin1" \
  --env-var "admin_pass=password" \
  --reporters cli,json \
  --reporter-json-export "newman-full-report.json"

echo "Newman execution complete. Check newman-full-report.json for full JSON output."
