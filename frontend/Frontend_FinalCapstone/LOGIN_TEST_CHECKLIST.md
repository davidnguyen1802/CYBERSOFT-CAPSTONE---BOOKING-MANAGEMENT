# Login Test Checklist - JWT Authentication with HttpOnly Cookies

## Pre-Test Setup

1. **Backend is running:** `http://localhost:8080`
2. **Frontend is running:** `http://localhost:4200`
3. **CORS is configured** (as per CORS_FIX_GUIDE.md)
4. **Test user exists** in database

## Test 1: Traditional Login Flow

### Steps:
1. Open browser: `http://localhost:4200/login`
2. Open DevTools (F12):
   - Network tab (record all requests)
   - Application tab → Cookies
   - Application tab → Local Storage
   - Console tab (for logs)

3. Enter credentials and click Login

### Expected Results:

#### ✅ Network Tab - Login Request
```
POST http://localhost:8080/auth/login
Status: 200 OK

Request Headers:
- Content-Type: application/json
- (No Authorization header - this is login)

Request Payload:
{
  "usernameOrEmail": "...",
  "password": "..."
}

Response Headers:
- Access-Control-Allow-Origin: http://localhost:4200
- Access-Control-Allow-Credentials: true
- Set-Cookie: refresh_token=...; Path=/; HttpOnly; SameSite=Lax

Response Body:
{
  "message": "Login successfully",
  "status": "OK",
  "data": {
    "token": "eyJhbGc...",
    "tokenType": "Bearer",
    "username": "...",
    "roles": ["ROLE_USER"],
    "id": 1
  }
}
```

**NOTE:** `refresh_token` should NOT be in the response body, only in Set-Cookie header!

#### ✅ Application Tab - Cookies
Look for cookie under `http://localhost:8080`:
```
Name: refresh_token
Value: eyJhbGc... (long JWT string)
Domain: localhost
Path: /
Expires: (14 days from now)
HttpOnly: ✓ (checked)
Secure: (empty for localhost HTTP)
SameSite: Lax
```

#### ✅ Application Tab - Local Storage
Look for entry under `http://localhost:4200`:
```
Key: access_token
Value: eyJhbGc... (JWT token)
```

**IMPORTANT:** Should NOT see `refresh_token` in localStorage!

#### ✅ Console Tab
```
Login response: {message: "Login successfully", status: "OK", data: {...}}
Token received: eyJhbGc...
User data: {token: "...", tokenType: "Bearer", username: "...", ...}
User roles: ["ROLE_USER"]
Navigating to /
Login complete
```

#### ✅ Browser Behavior
- Redirects to home page (`/`) or admin (`/admin`) based on role
- Header shows username (not "Đăng nhập" button)

---

## Test 2: API Request with Access Token

### Steps:
1. After successful login, navigate to user profile or make any API call
2. Check Network tab for the API request

### Expected Results:

#### ✅ Network Tab - API Request
```
GET http://localhost:8080/users/me
Status: 200 OK

Request Headers:
- Authorization: Bearer eyJhbGc...
- Cookie: refresh_token=... (automatically sent by browser)

Response:
{
  "message": "...",
  "status": "OK",
  "data": { user profile data }
}
```

**Key Points:**
- Authorization header contains access token
- Cookie header automatically includes refresh_token (you won't see this in Angular code)

---

## Test 3: Token Refresh (Manual - Optional)

### Steps:
1. To test refresh without waiting 15 minutes:
   - Open DevTools → Application → Local Storage
   - Manually change `access_token` to an expired/invalid token: `expired_token`
2. Navigate to user profile or make any API call
3. Watch Network tab closely

### Expected Results:

#### ✅ Network Tab - Sequence
```
1. GET /users/me
   Status: 401 Unauthorized
   Response: { "error": "TOKEN_EXPIRED", "message": "..." }

2. POST /auth/refresh (triggered automatically by interceptor)
   Status: 200 OK
   Request: (empty body)
   Request Cookie: refresh_token=...
   Response Headers: Set-Cookie: refresh_token=... (NEW token)
   Response Body: {
     "data": {
       "token": "new_access_token...",
       "username": "...",
       ...
     }
   }

3. GET /users/me (retry with new token)
   Status: 200 OK
   Request Headers: Authorization: Bearer new_access_token...
   Response: { user profile data }
```

#### ✅ Local Storage
- `access_token` updated to new token value

#### ✅ Cookies
- `refresh_token` cookie updated (new expiration time)

**No redirect to login page!** User stays on current page.

---

## Test 4: Logout

### Steps:
1. Click on username in header
2. Click "Đăng xuất" (Logout)
3. Watch Network and Application tabs

### Expected Results:

#### ✅ Network Tab
```
POST http://localhost:8080/auth/logout
Status: 200 OK

Request Headers:
- Authorization: Bearer ...
- Cookie: refresh_token=...

Response Headers:
- Set-Cookie: refresh_token=; Path=/; Max-Age=0; Expires=Thu, 01 Jan 1970...
  (This clears the cookie)

Response Body:
{
  "message": "Signed out successfully",
  "status": "OK"
}
```

#### ✅ Application Tab - Cookies
- `refresh_token` cookie is **removed** from `http://localhost:8080`

#### ✅ Application Tab - Local Storage
- `access_token` is **removed** from `http://localhost:4200`

#### ✅ Browser Behavior
- Redirects to `/login`
- Header shows "Đăng nhập" button (not username)

---

## Test 5: OAuth Login (Google/Facebook)

### Steps:
1. Click "Login with Google" button
2. Complete OAuth flow
3. You'll be redirected back to frontend

### Expected Results:

Similar to Test 1, but tokens come from URL parameters:
```
http://localhost:4200/auth/callback?token=...&refresh_token=...&id=...&username=...
```

Then same cookie/localStorage behavior as traditional login.

---

## Common Issues & Fixes

### ❌ Issue: CORS error "wildcard '*' when credentials mode is 'include'"
**Fix:** Backend must use exact origins, not wildcards (already fixed per CORS_FIX_GUIDE.md)

### ❌ Issue: No `Set-Cookie` header in response
**Causes:**
1. Backend not setting cookie correctly
2. CORS not allowing credentials

**Fix:**
- Check backend `ResponseCookie` configuration
- Verify CORS config has `allowCredentials(true)` and exact origins

### ❌ Issue: Cookie not showing in Application tab
**Check:**
- Look under `http://localhost:8080` domain (not 4200)
- Cookie might be under "localhost" or "127.0.0.1" depending on URL used

### ❌ Issue: `refresh_token` in localStorage
**Problem:** Old code is still setting it

**Fix:** Make sure you're using updated code (refresh_token should NOT be in localStorage)

### ❌ Issue: 401 error immediately after login
**Causes:**
1. Access token not being saved
2. Token format incorrect
3. Backend validation issue

**Debug:**
- Check Console for "Token received: ..."
- Verify token starts with `eyJhbGc...`
- Decode token at jwt.io to check claims

### ❌ Issue: Infinite refresh loop
**Cause:** Refresh endpoint also returns 401 TOKEN_EXPIRED

**Fix:** Backend `/auth/refresh` must accept expired access tokens (validate only refresh token from cookie)

---

## Success Criteria

All tests pass if:

1. ✅ Login saves access token in localStorage
2. ✅ Login sets refresh token in HttpOnly cookie
3. ✅ NO refresh_token in localStorage
4. ✅ API requests include Authorization header
5. ✅ Token refresh happens automatically on 401 TOKEN_EXPIRED
6. ✅ Logout clears both localStorage and cookie
7. ✅ No CORS errors
8. ✅ User stays logged in across page refreshes (until access token expires)
9. ✅ After access token expires, automatic refresh works without user noticing

---

## Quick Test Commands

### Check if cookie is set (using curl):
```bash
curl -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{"usernameOrEmail":"user@example.com","password":"password123"}' \
  -c cookies.txt \
  -v

# Look for Set-Cookie header in output
# Then check cookies.txt file
cat cookies.txt
```

### Decode JWT token (copy from localStorage):
```bash
# Visit: https://jwt.io
# Paste token from localStorage
# Verify claims: userId, sub, roles, exp, etc.
```

---

**Last Updated:** 2025-01-18  
**Status:** Ready for testing  
**Prerequisites:** Backend CORS fixed, frontend updated with withCredentials
