# 🧪 Auth Testing Checklist - Token Refresh Implementation

## 📋 Pre-Testing Setup

### Backend Requirements
- [ ] Backend server đang chạy trên `http://localhost:8080`
- [ ] Backend đã implement `/auth/refresh` endpoint
- [ ] Backend sử dụng HttpOnly cookie tên `refresh_token`
- [ ] Backend CORS config:
  - `allowedOrigins`: `http://localhost:4200` (NOT wildcard `*`)
  - `allowCredentials`: `true`
  - `exposedHeaders`: `["Set-Cookie", "Authorization"]`
- [ ] JWT Filter bypass `/auth/login`, `/auth/signup`, `/auth/refresh`

### Frontend Setup
- [ ] Run `npm install` (if needed)
- [ ] Run `ng serve` → App chạy trên `http://localhost:4200`
- [ ] Clear browser localStorage: `localStorage.clear()`
- [ ] Clear browser cookies for localhost
- [ ] Open DevTools (F12) → Network tab

---

## ✅ Test Cases

### Test 1: Login Flow
**Steps:**
1. Navigate to `http://localhost:4200/login`
2. Open DevTools → Network tab
3. Enter credentials and click "Đăng nhập"

**Expected Results:**
- [ ] Network tab shows `POST http://localhost:8080/auth/login`
- [ ] Request Headers include:
  ```
  Content-Type: application/json
  Origin: http://localhost:4200
  (NO Authorization header - this is login!)
  ```
- [ ] Request Payload:
  ```json
  {
    "usernameOrEmail": "your_username",
    "password": "your_password"
  }
  ```
- [ ] Response status: `200 OK`
- [ ] Response Headers include:
  ```
  Access-Control-Allow-Origin: http://localhost:4200
  Access-Control-Allow-Credentials: true
  Set-Cookie: refresh_token=...; HttpOnly; Secure; SameSite=Strict; Max-Age=2592000
  ```
- [ ] Response Body:
  ```json
  {
    "message": "Login successfully",
    "status": "OK",
    "data": {
      "token": "eyJhbGc...",
      "username": "...",
      "roles": ["ROLE_USER"],
      "id": 123
    }
  }
  ```
- [ ] Console logs:
  ```
  🔐 Login attempt started
  🔐 Sending login request...
  ✅ Login response: ...
  🔑 Token saved to localStorage
  🔐 Auth State: User logged in
  🛒 Refreshing cart for logged-in user
  ```
- [ ] localStorage['access_token'] = "eyJhbGc..."
- [ ] localStorage['user'] = user info JSON
- [ ] Browser Cookies → `refresh_token` exists (HttpOnly)
- [ ] Redirect to home `/` or admin `/admin`

**If Failed:**
- [ ] Check backend console for errors
- [ ] Check CORS configuration
- [ ] Verify JWT filter is not blocking `/auth/login`
- [ ] See `401_POSTMAN_WORKS_BROWSER_FAILS.md` for troubleshooting

---

### Test 2: API Call with Valid Access Token
**Steps:**
1. After login, navigate to `/user-profile`
2. Open DevTools → Network tab

**Expected Results:**
- [ ] Network tab shows `GET http://localhost:8080/users/me`
- [ ] Request Headers include:
  ```
  Authorization: Bearer eyJhbGc...
  Cookie: refresh_token=...
  ```
- [ ] Response status: `200 OK`
- [ ] User profile data displayed correctly
- [ ] Console logs:
  ```
  🔵 API Call: GET /users/me
  💾 User response retrieved from localStorage: ...
  ```

**If Failed:**
- [ ] Check if access_token exists in localStorage
- [ ] Check if interceptor is adding Authorization header
- [ ] Check backend JWT filter is validating token correctly

---

### Test 3: Access Token Expired - Auto Refresh
**How to Test:**
Option A: Wait 12 hours (not practical)
Option B: Manually expire token in localStorage
Option C: Modify backend to use 1-minute access token expiry for testing

**Steps:**
1. Login successfully
2. Wait for access token to expire (or manually expire it)
3. Make an API call (e.g., navigate to `/user-profile`)
4. Open DevTools → Network tab

**Expected Results:**
- [ ] Network tab shows:
  ```
  1. GET /users/me → 401 Unauthorized
  2. POST /auth/refresh → 200 OK
  3. GET /users/me → 200 OK (retry with new token)
  ```
- [ ] Console logs:
  ```
  ⚠️ 401 Unauthorized - Attempting token refresh...
  🔄 Refreshing access token...
  ✅ Token refreshed successfully
  ```
- [ ] localStorage['access_token'] updated with new token
- [ ] User profile data displayed (NOT redirected to login)
- [ ] User did NOT get logged out
- [ ] UI did NOT show any error message

**If Failed:**
- [ ] Check if refresh_token cookie exists in browser
- [ ] Check if backend `/auth/refresh` endpoint works
- [ ] Check if interceptor is catching 401 errors
- [ ] Check console logs for errors

---

### Test 4: Refresh Token Expired - Logout
**How to Test:**
Option A: Wait 30 days (not practical)
Option B: Manually delete refresh_token cookie
Option C: Modify backend to use 5-minute refresh token expiry for testing

**Steps:**
1. Login successfully
2. Delete `refresh_token` cookie from browser:
   - DevTools → Application → Cookies → localhost → Delete `refresh_token`
3. Make an API call (e.g., navigate to `/user-profile`)

**Expected Results:**
- [ ] Network tab shows:
  ```
  1. GET /users/me → 401 Unauthorized
  2. POST /auth/refresh → 401 Unauthorized (no refresh token)
  ```
- [ ] Console logs:
  ```
  ⚠️ 401 Unauthorized - Attempting token refresh...
  🔄 Refreshing access token...
  ❌ Token refresh failed: ...
  🔑 Tokens removed from localStorage
  ```
- [ ] localStorage['access_token'] = null
- [ ] localStorage['user'] = null
- [ ] Redirect to `/login?sessionExpired=true`
- [ ] User logged out

**If Failed:**
- [ ] Check interceptor error handling
- [ ] Check if handleRefreshFailure() is called
- [ ] Check router navigation

---

### Test 5: Multiple Concurrent Requests (Race Condition Test)
**Steps:**
1. Login successfully
2. Expire access token (delete or wait)
3. Open DevTools → Network tab
4. Navigate to a page that makes multiple API calls simultaneously
   - Example: Dashboard with profile + bookings + properties
5. Observe Network tab

**Expected Results:**
- [ ] Network tab shows:
  ```
  1. GET /users/me → 401
  2. GET /bookings → 401
  3. GET /properties → 401
  4. POST /auth/refresh → 200 OK (ONLY ONE request!)
  5. GET /users/me → 200 OK (retry)
  6. GET /bookings → 200 OK (retry)
  7. GET /properties → 200 OK (retry)
  ```
- [ ] Console logs:
  ```
  ⚠️ 401 Unauthorized - Attempting token refresh...
  🔄 Refreshing access token...
  ⏳ Waiting for token refresh to complete... (for subsequent 401s)
  ⏳ Waiting for token refresh to complete...
  ✅ Token refreshed successfully
  ✅ Using refreshed token for queued request
  ✅ Using refreshed token for queued request
  ```
- [ ] **ONLY 1** `/auth/refresh` request (not 3!)
- [ ] All original requests retried successfully
- [ ] No duplicate refresh requests

**If Failed:**
- [ ] Check isRefreshing flag in interceptor
- [ ] Check refreshTokenSubject logic
- [ ] See TokenInterceptor implementation

---

### Test 6: Logout Flow
**Steps:**
1. Login successfully
2. Open DevTools → Network tab
3. Click user avatar → "Đăng xuất"

**Expected Results:**
- [ ] Network tab shows `POST http://localhost:8080/auth/logout`
- [ ] Request Headers include:
  ```
  Authorization: Bearer eyJhbGc...
  Cookie: refresh_token=...
  ```
- [ ] Response status: `200 OK`
- [ ] Console logs:
  ```
  Logged out successfully
  🔑 Tokens removed from localStorage
  💾 User data removed from localStorage
  🔐 Auth State: User logged out
  ```
- [ ] localStorage['access_token'] = null
- [ ] localStorage['user'] = null
- [ ] Browser Cookies → `refresh_token` deleted (by backend)
- [ ] Redirect to `/login`

**If Failed:**
- [ ] Check if logout() method calls backend endpoint
- [ ] Check if all cleanup methods are called
- [ ] Check backend /auth/logout implementation

---

### Test 7: Login → Logout → Login Again
**Steps:**
1. Login successfully
2. Logout
3. Login again with same credentials

**Expected Results:**
- [ ] First login successful
- [ ] Logout successful (tokens cleared)
- [ ] Second login successful
- [ ] New access_token in localStorage
- [ ] New refresh_token cookie
- [ ] No errors in console
- [ ] Can access protected routes

**If Failed:**
- [ ] Check if logout properly clears all data
- [ ] Check if backend creates new session on login

---

### Test 8: Direct Navigation to Protected Route (Not Logged In)
**Steps:**
1. Logout (or clear localStorage + cookies)
2. Navigate directly to `http://localhost:4200/user-profile`

**Expected Results:**
- [ ] AuthGuard blocks access
- [ ] Redirect to `/login`
- [ ] Console log: "Access denied - no token"

**If Failed:**
- [ ] Check if AuthGuard is implemented
- [ ] Check if routes are protected with canActivate

---

### Test 9: Token in Request Headers (All Requests)
**Steps:**
1. Login successfully
2. Navigate to any page (home, properties, etc.)
3. Open DevTools → Network tab
4. Make any API call
5. Click on the request → Headers tab

**Expected Results:**
- [ ] Request Headers include:
  ```
  Authorization: Bearer eyJhbGc...
  Cookie: refresh_token=...
  ```
- [ ] `Authorization` header added by interceptor (if token exists)
- [ ] `Cookie` header sent automatically by browser (withCredentials: true)

**If Failed:**
- [ ] Check TokenInterceptor addTokenAndCredentials() method
- [ ] Check if withCredentials: true is set

---

### Test 10: CORS Preflight (OPTIONS Request)
**Steps:**
1. Logout
2. Open DevTools → Network tab
3. Login
4. Look for OPTIONS request before POST /auth/login

**Expected Results:**
- [ ] Network tab shows:
  ```
  1. OPTIONS /auth/login → 200 OK
  2. POST /auth/login → 200 OK
  ```
- [ ] OPTIONS Response Headers:
  ```
  Access-Control-Allow-Origin: http://localhost:4200
  Access-Control-Allow-Credentials: true
  Access-Control-Allow-Methods: GET, POST, PUT, DELETE, OPTIONS
  Access-Control-Allow-Headers: Content-Type, Authorization
  ```

**If Failed:**
- [ ] Backend CORS configuration is wrong
- [ ] See `401_POSTMAN_WORKS_BROWSER_FAILS.md`
- [ ] Check if backend allows OPTIONS requests

---

## 🐛 Common Issues & Solutions

### Issue 1: Login returns 401
**Possible causes:**
- CORS preflight failing
- JWT filter blocking `/auth/login`
- Wrong credentials
- Backend not running

**Debug steps:**
1. Check Network tab → OPTIONS request
2. Check backend console logs
3. Test with Postman (if Postman works, it's CORS)
4. See `401_POSTMAN_WORKS_BROWSER_FAILS.md`

---

### Issue 2: Refresh token not working
**Possible causes:**
- refresh_token cookie not sent
- withCredentials: false
- Backend not reading cookie correctly
- Refresh token expired

**Debug steps:**
1. Check browser cookies → refresh_token exists?
2. Check Network tab → Cookie header present?
3. Check backend logs → Is it receiving the cookie?
4. Check UserService.refreshToken() has withCredentials: true

---

### Issue 3: User logged out randomly
**Possible causes:**
- Refresh token expired (after 30 days)
- Backend revoked tokens
- Manual cookie deletion
- Browser privacy mode

**Debug steps:**
1. Check refresh_token cookie expiry
2. Check backend token revocation logic
3. Check if cookies are allowed in browser

---

### Issue 4: Multiple refresh requests
**Possible causes:**
- Race condition not handled
- isRefreshing flag not working
- Multiple interceptors

**Debug steps:**
1. Check TokenInterceptor isRefreshing logic
2. Check if only ONE interceptor is registered
3. Add console logs to track refresh calls

---

## 📊 Success Criteria

### ✅ All tests should pass:
- [ ] Login successful
- [ ] Tokens saved correctly
- [ ] API calls include Authorization header
- [ ] Auto refresh works when access token expires
- [ ] User NOT logged out when access token expires
- [ ] User logged out when refresh token expires
- [ ] Race condition handled (only 1 refresh request)
- [ ] Logout clears all tokens
- [ ] CORS working (OPTIONS + POST)
- [ ] withCredentials: true on all requests

### 🎉 If all tests pass:
✅ **Auth implementation is production-ready!**

---

## 📝 Test Results Log

### Test Run: _______________
Date: _______________
Tester: _______________

| Test # | Test Name | Status | Notes |
|--------|-----------|--------|-------|
| 1 | Login Flow | ⬜ Pass ⬜ Fail | |
| 2 | API Call with Valid Token | ⬜ Pass ⬜ Fail | |
| 3 | Auto Refresh | ⬜ Pass ⬜ Fail | |
| 4 | Refresh Token Expired | ⬜ Pass ⬜ Fail | |
| 5 | Race Condition | ⬜ Pass ⬜ Fail | |
| 6 | Logout Flow | ⬜ Pass ⬜ Fail | |
| 7 | Login Again | ⬜ Pass ⬜ Fail | |
| 8 | Protected Route | ⬜ Pass ⬜ Fail | |
| 9 | Request Headers | ⬜ Pass ⬜ Fail | |
| 10 | CORS Preflight | ⬜ Pass ⬜ Fail | |

**Overall Result:** ⬜ All Pass ⬜ Some Failed

**Issues Found:**
1. _________________________________
2. _________________________________
3. _________________________________

**Action Items:**
1. _________________________________
2. _________________________________
3. _________________________________

---

**Last Updated:** 2025-01-18  
**Document Version:** 1.0  
**Status:** Ready for Testing
