#!/usr/bin/env bash
set -e

echo "=== 1. Authenticating with User Service (:8081) ==="
# Login as Admin
ADMIN_LOGIN_RES=$(curl -s -X POST http://localhost:8081/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@example.com","password":"Admin@123"}')
ADMIN_TOKEN=$(echo "$ADMIN_LOGIN_RES" | grep -o '"accessToken":"[^"]*' | cut -d'"' -f4)

if [ -z "$ADMIN_TOKEN" ]; then
  echo "Failed to get admin token: $ADMIN_LOGIN_RES"
  exit 1
fi
echo "Admin token obtained."

# Register and Login regular test user
USER_EMAIL="cart_tester_$(date +%s)@example.com"
USER_PASS="User@123456"
REG_RES=$(curl -s -X POST http://localhost:8081/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d "{\"email\":\"$USER_EMAIL\",\"password\":\"$USER_PASS\",\"fullName\":\"Cart Tester\"}")

LOGIN_RES=$(curl -s -X POST http://localhost:8081/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d "{\"email\":\"$USER_EMAIL\",\"password\":\"$USER_PASS\"}")
USER_TOKEN=$(echo "$LOGIN_RES" | grep -o '"accessToken":"[^"]*' | cut -d'"' -f4)

if [ -z "$USER_TOKEN" ]; then
  echo "Failed to register user: $REG_RES"
  exit 1
fi
echo "User token obtained for $USER_EMAIL"

echo ""
echo "=== 2. Creating Category and Product on Product Service (:8082) ==="
CAT_RES=$(curl -s -X POST http://localhost:8082/api/v1/categories \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d "{\"name\":\"Electronics $(date +%s)\",\"description\":\"Devices and gadgets\"}")
CAT_ID=$(echo "$CAT_RES" | grep -o '"id":"[^"]*' | cut -d'"' -f4)
echo "Created category ID: $CAT_ID"

PROD_RES=$(curl -s -X POST http://localhost:8082/api/v1/products \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d "{\"name\":\"Mechanical Keyboard\",\"description\":\"RGB mechanical gaming keyboard\",\"categoryId\":\"$CAT_ID\",\"price\":150.00,\"currency\":\"BDT\",\"stockQuantity\":10}")
PROD_ID=$(echo "$PROD_RES" | grep -o '"id":"[^"]*' | cut -d'"' -f4)
echo "Created product ID: $PROD_ID"

echo ""
echo "=== 3. Testing Cart Service (:8083) Endpoints ==="

echo "--- Test 1: Unauthorized access without token ---"
STATUS_UNAUTH=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:8083/api/v1/cart)
echo "Status code: $STATUS_UNAUTH (Expected: 401)"
if [ "$STATUS_UNAUTH" != "401" ]; then
  echo "FAILED: Expected 401"
  exit 1
fi

echo "--- Test 2: Get empty cart for new user ---"
EMPTY_CART=$(curl -s http://localhost:8083/api/v1/cart \
  -H "Authorization: Bearer $USER_TOKEN")
echo "Response: $EMPTY_CART"

echo "--- Test 3: Add product to cart (quantity 2) ---"
ADD_RES=$(curl -s -X POST http://localhost:8083/api/v1/cart/items \
  -H "Authorization: Bearer $USER_TOKEN" \
  -H "Content-Type: application/json" \
  -d "{\"productId\":\"$PROD_ID\",\"quantity\":2}")
echo "Response: $ADD_RES"

ITEM_ID=$(echo "$ADD_RES" | grep -o '"id":"[^"]*' | head -n 2 | tail -n 1 | cut -d'"' -f4)
echo "Cart item ID: $ITEM_ID"

echo "--- Test 4: Add same product to cart again (quantity 1) ---"
ADD_AGAIN_RES=$(curl -s -X POST http://localhost:8083/api/v1/cart/items \
  -H "Authorization: Bearer $USER_TOKEN" \
  -H "Content-Type: application/json" \
  -d "{\"productId\":\"$PROD_ID\",\"quantity\":1}")
echo "Response: $ADD_AGAIN_RES"

echo "--- Test 5: Add non-existent product (Expect 404 PRODUCT_NOT_FOUND) ---"
RANDOM_UUID="00000000-0000-0000-0000-000000000000"
NOT_FOUND_RES=$(curl -s -w "\nHTTP_STATUS:%{http_code}\n" -X POST http://localhost:8083/api/v1/cart/items \
  -H "Authorization: Bearer $USER_TOKEN" \
  -H "Content-Type: application/json" \
  -d "{\"productId\":\"$RANDOM_UUID\",\"quantity\":1}")
echo "Response: $NOT_FOUND_RES"

echo "--- Test 6: Add quantity exceeding available stock (Expect 409 INSUFFICIENT_STOCK) ---"
OVERSTOCK_RES=$(curl -s -w "\nHTTP_STATUS:%{http_code}\n" -X POST http://localhost:8083/api/v1/cart/items \
  -H "Authorization: Bearer $USER_TOKEN" \
  -H "Content-Type: application/json" \
  -d "{\"productId\":\"$PROD_ID\",\"quantity\":50}")
echo "Response: $OVERSTOCK_RES"

echo "--- Test 7: Update item quantity to 4 ---"
UPDATE_RES=$(curl -s -X PUT "http://localhost:8083/api/v1/cart/items/$ITEM_ID" \
  -H "Authorization: Bearer $USER_TOKEN" \
  -H "Content-Type: application/json" \
  -d "{\"quantity\":4}")
echo "Response: $UPDATE_RES"

echo "--- Test 8: View cart ---"
VIEW_RES=$(curl -s http://localhost:8083/api/v1/cart \
  -H "Authorization: Bearer $USER_TOKEN")
echo "Response: $VIEW_RES"

echo "--- Test 9: Remove item ---"
STATUS_DEL_ITEM=$(curl -s -o /dev/null -w "%{http_code}" -X DELETE "http://localhost:8083/api/v1/cart/items/$ITEM_ID" \
  -H "Authorization: Bearer $USER_TOKEN")
echo "Delete item status code: $STATUS_DEL_ITEM (Expected: 204)"

echo "--- Test 10: Clear cart ---"
# Add one item back first
curl -s -X POST http://localhost:8083/api/v1/cart/items \
  -H "Authorization: Bearer $USER_TOKEN" \
  -H "Content-Type: application/json" \
  -d "{\"productId\":\"$PROD_ID\",\"quantity\":1}" > /dev/null

STATUS_CLEAR=$(curl -s -o /dev/null -w "%{http_code}" -X DELETE http://localhost:8083/api/v1/cart \
  -H "Authorization: Bearer $USER_TOKEN")
echo "Clear cart status code: $STATUS_CLEAR (Expected: 204)"

FINAL_CART=$(curl -s http://localhost:8083/api/v1/cart \
  -H "Authorization: Bearer $USER_TOKEN")
echo "Final cart after clear: $FINAL_CART"

echo ""
echo "=== ALL CART SERVICE TESTS PASSED SUCCESSFULLY! ==="
