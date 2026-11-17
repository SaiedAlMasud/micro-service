#!/usr/bin/env bash
# ==============================================================================
# Microservice User-Service: Automated A-Z Endpoint Smoke Test
# ==============================================================================

BASE_URL="http://localhost:8081"
RAND_ID=$((RANDOM % 9000 + 1000))
TEST_EMAIL="user_${RAND_ID}@example.com"
TEST_PASS="Secret@123"
ADMIN_EMAIL="admin@example.com"
ADMIN_PASS="Admin@123"

GREEN='\033[0;32m'
RED='\033[0;31m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

pass_count=0
fail_count=0

assert_status() {
    local label="$1"
    local expected="$2"
    local actual="$3"
    local response="$4"

    if [ "$actual" -eq "$expected" ]; then
        echo -e "${GREEN}✔ PASS${NC} [HTTP $actual] $label"
        pass_count=$((pass_count + 1))
    else
        echo -e "${RED}✘ FAIL${NC} [Expected $expected, got $actual] $label"
        echo -e "   Response: $response"
        fail_count=$((fail_count + 1))
    fi
}

echo -e "${BLUE}======================================================${NC}"
echo -e "${BLUE}  USER SERVICE - COMPLETE A to Z ENDPOINT VERIFICATION ${NC}"
echo -e "${BLUE}======================================================${NC}"

# Check connection
if ! curl -s "$BASE_URL/actuator/health" > /dev/null; then
    echo -e "${RED}Error: Service is not running on $BASE_URL!${NC}"
    echo "Please start the service with: ./gradlew bootRun"
    exit 1
fi

# 1. Health check
echo -e "\n${YELLOW}1. Health Check${NC}"
RESP=$(curl -s -w "\n%{http_code}" "$BASE_URL/actuator/health")
STATUS=$(echo "$RESP" | tail -n1)
BODY=$(echo "$RESP" | sed '$d')
assert_status "Service and DB Health UP" 200 "$STATUS" "$BODY"

# 2. Register New User
echo -e "\n${YELLOW}2. Register New User (${TEST_EMAIL})${NC}"
RESP=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/api/v1/auth/register" \
    -H "Content-Type: application/json" \
    -d "{\"fullName\":\"John Doe\",\"email\":\"$TEST_EMAIL\",\"password\":\"$TEST_PASS\",\"phone\":\"01711223344\"}")
STATUS=$(echo "$RESP" | tail -n1)
BODY=$(echo "$RESP" | sed '$d')
assert_status "Register new user returns 201 Created" 201 "$STATUS" "$BODY"
USER_ID=$(echo "$BODY" | python3 -c "import sys,json; print(json.load(sys.stdin).get('id',''))" 2>/dev/null)

# 3. Duplicate Registration Conflict
echo -e "\n${YELLOW}3. Duplicate Email Check${NC}"
RESP=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/api/v1/auth/register" \
    -H "Content-Type: application/json" \
    -d "{\"fullName\":\"John Duplicate\",\"email\":\"$TEST_EMAIL\",\"password\":\"$TEST_PASS\"}")
STATUS=$(echo "$RESP" | tail -n1)
BODY=$(echo "$RESP" | sed '$d')
assert_status "Duplicate email returns 409 Conflict" 409 "$STATUS" "$BODY"

# 4. Validation Error Check
echo -e "\n${YELLOW}4. Request Validation Handling${NC}"
RESP=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/api/v1/auth/register" \
    -H "Content-Type: application/json" \
    -d "{\"fullName\":\"\",\"email\":\"invalid-email\",\"password\":\"123\"}")
STATUS=$(echo "$RESP" | tail -n1)
BODY=$(echo "$RESP" | sed '$d')
assert_status "Invalid body returns 400 Bad Request" 400 "$STATUS" "$BODY"

# 5. Login
echo -e "\n${YELLOW}5. User Login & Token Generation${NC}"
RESP=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/api/v1/auth/login" \
    -H "Content-Type: application/json" \
    -d "{\"email\":\"$TEST_EMAIL\",\"password\":\"$TEST_PASS\"}")
STATUS=$(echo "$RESP" | tail -n1)
BODY=$(echo "$RESP" | sed '$d')
assert_status "Valid login returns 200 OK with tokens" 200 "$STATUS" "$BODY"
ACCESS_TOKEN=$(echo "$BODY" | python3 -c "import sys,json; print(json.load(sys.stdin).get('accessToken',''))" 2>/dev/null)
REFRESH_TOKEN=$(echo "$BODY" | python3 -c "import sys,json; print(json.load(sys.stdin).get('refreshToken',''))" 2>/dev/null)

# 6. Wrong Password Login
echo -e "\n${YELLOW}6. Invalid Credentials Check${NC}"
RESP=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/api/v1/auth/login" \
    -H "Content-Type: application/json" \
    -d "{\"email\":\"$TEST_EMAIL\",\"password\":\"WrongPassword\"}")
STATUS=$(echo "$RESP" | tail -n1)
BODY=$(echo "$RESP" | sed '$d')
assert_status "Wrong password returns 401 Unauthorized" 401 "$STATUS" "$BODY"

# 7. Get Profile (/users/me)
echo -e "\n${YELLOW}7. Fetch Profile (GET /api/v1/users/me)${NC}"
RESP=$(curl -s -w "\n%{http_code}" "$BASE_URL/api/v1/users/me" \
    -H "Authorization: Bearer $ACCESS_TOKEN")
STATUS=$(echo "$RESP" | tail -n1)
BODY=$(echo "$RESP" | sed '$d')
assert_status "Authenticated GET /me returns 200 OK" 200 "$STATUS" "$BODY"

# 8. Unauthorized /users/me without token
echo -e "\n${YELLOW}8. Missing Authorization Check${NC}"
RESP=$(curl -s -w "\n%{http_code}" "$BASE_URL/api/v1/users/me")
STATUS=$(echo "$RESP" | tail -n1)
BODY=$(echo "$RESP" | sed '$d')
assert_status "Missing token returns 401 Unauthorized" 401 "$STATUS" "$BODY"

# 9. Update Profile (PUT /api/v1/users/me)
echo -e "\n${YELLOW}9. Update Profile (PUT /api/v1/users/me)${NC}"
RESP=$(curl -s -w "\n%{http_code}" -X PUT "$BASE_URL/api/v1/users/me" \
    -H "Authorization: Bearer $ACCESS_TOKEN" \
    -H "Content-Type: application/json" \
    -d "{\"fullName\":\"John Updated\",\"phone\":\"01899999999\"}")
STATUS=$(echo "$RESP" | tail -n1)
BODY=$(echo "$RESP" | sed '$d')
assert_status "Update profile returns 200 OK" 200 "$STATUS" "$BODY"

# 10. Refresh Token Rotation
echo -e "\n${YELLOW}10. Token Refresh (POST /api/v1/auth/refresh)${NC}"
RESP=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/api/v1/auth/refresh" \
    -H "Content-Type: application/json" \
    -d "{\"refreshToken\":\"$REFRESH_TOKEN\"}")
STATUS=$(echo "$RESP" | tail -n1)
BODY=$(echo "$RESP" | sed '$d')
assert_status "Token refresh returns 200 OK with new tokens" 200 "$STATUS" "$BODY"
NEW_ACCESS_TOKEN=$(echo "$BODY" | python3 -c "import sys,json; print(json.load(sys.stdin).get('accessToken',''))" 2>/dev/null)
NEW_REFRESH_TOKEN=$(echo "$BODY" | python3 -c "import sys,json; print(json.load(sys.stdin).get('refreshToken',''))" 2>/dev/null)

# 11. Old Refresh Token Re-use Prevention
echo -e "\n${YELLOW}11. Refresh Token Invalidation / Rotation Verification${NC}"
RESP=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/api/v1/auth/refresh" \
    -H "Content-Type: application/json" \
    -d "{\"refreshToken\":\"$REFRESH_TOKEN\"}")
STATUS=$(echo "$RESP" | tail -n1)
BODY=$(echo "$RESP" | sed '$d')
assert_status "Re-using old refresh token returns 401 Unauthorized" 401 "$STATUS" "$BODY"

# 12. Admin Login
echo -e "\n${YELLOW}12. Seed Admin Login${NC}"
RESP=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/api/v1/auth/login" \
    -H "Content-Type: application/json" \
    -d "{\"email\":\"$ADMIN_EMAIL\",\"password\":\"$ADMIN_PASS\"}")
STATUS=$(echo "$RESP" | tail -n1)
BODY=$(echo "$RESP" | sed '$d')
assert_status "Admin login returns 200 OK" 200 "$STATUS" "$BODY"
ADMIN_TOKEN=$(echo "$BODY" | python3 -c "import sys,json; print(json.load(sys.stdin).get('accessToken',''))" 2>/dev/null)

# 13. Admin Access: GET /api/v1/users/{id}
echo -e "\n${YELLOW}13. Admin Access: GET /api/v1/users/{id}${NC}"
RESP=$(curl -s -w "\n%{http_code}" "$BASE_URL/api/v1/users/$USER_ID" \
    -H "Authorization: Bearer $ADMIN_TOKEN")
STATUS=$(echo "$RESP" | tail -n1)
BODY=$(echo "$RESP" | sed '$d')
assert_status "Admin can query user by ID (200 OK)" 200 "$STATUS" "$BODY"

# 14. Forbidden: Regular User calling GET /api/v1/users/{id}
echo -e "\n${YELLOW}14. Regular User Forbidden on Admin Endpoint${NC}"
RESP=$(curl -s -w "\n%{http_code}" "$BASE_URL/api/v1/users/$USER_ID" \
    -H "Authorization: Bearer $NEW_ACCESS_TOKEN")
STATUS=$(echo "$RESP" | tail -n1)
BODY=$(echo "$RESP" | sed '$d')
assert_status "Regular user gets 403 Forbidden on admin endpoint" 403 "$STATUS" "$BODY"

# 15. Logout
echo -e "\n${YELLOW}15. Logout (POST /api/v1/auth/logout)${NC}"
RESP=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/api/v1/auth/logout" \
    -H "Authorization: Bearer $NEW_ACCESS_TOKEN")
STATUS=$(echo "$RESP" | tail -n1)
BODY=$(echo "$RESP" | sed '$d')
assert_status "Logout returns 204 No Content" 204 "$STATUS" "$BODY"

# 16. Refresh after Logout should fail
echo -e "\n${YELLOW}16. Refresh Token after Logout Check${NC}"
RESP=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/api/v1/auth/refresh" \
    -H "Content-Type: application/json" \
    -d "{\"refreshToken\":\"$NEW_REFRESH_TOKEN\"}")
STATUS=$(echo "$RESP" | tail -n1)
BODY=$(echo "$RESP" | sed '$d')
assert_status "Revoked refresh token after logout returns 401 Unauthorized" 401 "$STATUS" "$BODY"

echo -e "\n${BLUE}======================================================${NC}"
echo -e "Tests Completed: ${GREEN}$pass_count passed${NC}, ${RED}$fail_count failed${NC}"
echo -e "${BLUE}======================================================${NC}"

if [ "$fail_count" -eq 0 ]; then
    echo -e "${GREEN}🎉 ALL ENDPOINTS VERIFIED & WORKING PERFECTLY!${NC}"
    exit 0
else
    exit 1
fi

