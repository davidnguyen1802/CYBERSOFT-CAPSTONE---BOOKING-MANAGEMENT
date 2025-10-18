# 401 Unauthorized - Frontend Login Fails (Postman Works)

## Problem
- ✅ Postman login works successfully
- ❌ Frontend login returns `401 Unauthorized`
- Error: `HttpErrorResponse { status: 401, statusText: 'OK' }`

## Root Cause
When Postman works but browser fails with 401, it's **always** one of these issues:

### 1. **CORS Preflight Failure** (Most Common)
Browsers send an OPTIONS request before POST. If CORS headers are wrong, the POST will fail with 401.

### 2. **Backend JWT Filter Intercepting Login**
The JWT authentication filter might be checking `/auth/login` when it should be bypassed.

### 3. **HttpOnly Cookie Issue**
Frontend is sending `withCredentials: true`, backend might not be configured to accept it.

---

## Diagnostic Steps

### Step 1: Check Browser Network Tab

Open DevTools (F12) → Network tab, then try to login again.

#### Look for TWO requests:

**Request 1: OPTIONS (Preflight)**
```
OPTIONS http://localhost:8080/auth/login
Status: ??? (Check this first!)
```

**Request 2: POST (Actual login)**
```
POST http://localhost:8080/auth/login
Status: 401
```

#### ✅ If OPTIONS shows **200 OK**:
Check the response headers of OPTIONS request:
```
Access-Control-Allow-Origin: http://localhost:4200  (NOT *)
Access-Control-Allow-Credentials: true
Access-Control-Allow-Methods: POST, GET, PUT, DELETE, OPTIONS
```

#### ❌ If OPTIONS shows **401**, **403**, or **Failed**:
The CORS configuration is wrong. Backend is not allowing the preflight.

---

### Step 2: Check What Frontend is Sending

In Network tab, click the **POST** request → **Headers** tab

#### Request Headers should include:
```
Origin: http://localhost:4200
Content-Type: application/json
(NO Authorization header - this is a login request!)
```

#### Request Payload should be:
```json
{
  "usernameOrEmail": "your_username",
  "password": "your_password"
}
```

#### ❌ If you see `Authorization: Bearer ...` header:
The JWT interceptor is adding a token to the login request (bug).

---

### Step 3: Check Backend Response

In Network tab, click the **POST** request → **Response** tab

#### ✅ If you see JSON response with error message:
```json
{
  "message": "Bad credentials",
  "status": "UNAUTHORIZED"
}
```
This means credentials are wrong, but CORS is working.

#### ❌ If you see HTML or no response body:
CORS is blocking the response. Browser won't show the body due to CORS policy.

---

## Backend Fixes Required

Based on the attached `GOOGLE_OAUTH_FIX.md`, your backend might have JWT filter issues.

### Fix 1: Bypass JWT Filter for `/auth/login`

In your `JwtTokenFilter.java`, ensure `/auth/login` is bypassed:

```java
@Override
protected void doFilterInternal(...) throws ServletException, IOException {
    // CHECK BYPASS FIRST - before any authentication logic
    if (isBypassToken(request)) {
        filterChain.doFilter(request, response);
        return;  // Exit immediately
    }
    
    try {
        // JWT authentication logic only for non-bypassed endpoints
        final String authHeader = request.getHeader("Authorization");
        // ... rest of authentication
    } catch (Exception e) {
        // Error handling
    }
}

private boolean isBypassToken(HttpServletRequest request) {
    final String requestPath = request.getServletPath();
    
    // ALL these should bypass JWT validation:
    return requestPath.startsWith("/auth/login") ||
           requestPath.startsWith("/auth/signup") ||
           requestPath.startsWith("/auth/register") ||
           requestPath.startsWith("/auth/social-login") ||
           requestPath.startsWith("/auth/social/callback") ||
           requestPath.equals("/auth/refresh") ||  // Refresh validates refresh token, not access token
           requestPath.startsWith("/api/categories");
}
```

**Critical:** The `isBypassToken()` check must be **BEFORE** the try-catch block!

### Fix 2: Verify CORS Configuration

Your `CorsConfig.java` must have:

```java
@Configuration
public class CorsConfig implements WebMvcConfigurer {
    
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins(
                    "http://localhost:4200",
                    "http://127.0.0.1:4200"
                )  // ✅ Exact origins (NOT wildcards!)
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .exposedHeaders("Set-Cookie", "Authorization")
                .allowCredentials(true)  // ✅ Required for cookies
                .maxAge(3600);
    }
}
```

**Remove ALL `@CrossOrigin` annotations** from controllers!

### Fix 3: Spring Security CORS Configuration

If you have `WebSecurityConfig.java`, add:

```java
@Bean
public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http
        .cors(withDefaults())  // Enable CORS
        .csrf(csrf -> csrf.disable())
        .authorizeHttpRequests(auth -> auth
            .requestMatchers("/auth/**").permitAll()  // Allow auth endpoints
            .requestMatchers("/api/categories/**").permitAll()
            .anyRequest().authenticated()
        )
        .addFilterBefore(jwtTokenFilter, UsernamePasswordAuthenticationFilter.class);
        
    return http.build();
}
```

---

## Quick Backend Test

Run this curl command to test if backend accepts OPTIONS:

```bash
# Test preflight
curl -X OPTIONS http://localhost:8080/auth/login \
  -H "Origin: http://localhost:4200" \
  -H "Access-Control-Request-Method: POST" \
  -H "Access-Control-Request-Headers: Content-Type" \
  -v

# Expected output:
< HTTP/1.1 200 OK
< Access-Control-Allow-Origin: http://localhost:4200
< Access-Control-Allow-Credentials: true
< Access-Control-Allow-Methods: GET, POST, PUT, DELETE, OPTIONS
```

If you see `401` or `403` here, the CORS is definitely wrong.

---

## Frontend Temporary Workaround

**FOR TESTING ONLY** - disable CORS in Chrome:

### Windows:
```powershell
# Close ALL Chrome windows first
# Then run:
"C:\Program Files\Google\Chrome\Application\chrome.exe" --disable-web-security --user-data-dir="C:\tmp\chrome-test"
```

### Mac:
```bash
open -na "Google Chrome" --args --disable-web-security --user-data-dir=/tmp/chrome-test
```

### Linux:
```bash
google-chrome --disable-web-security --user-data-dir=/tmp/chrome-test
```

Then try login again. If it works, the problem is 100% CORS configuration.

**⚠️ Don't use this Chrome instance for regular browsing! Only for testing.**

---

## Backend Checklist

- [ ] `JwtTokenFilter.java` - `isBypassToken()` check is **FIRST** in `doFilterInternal()`
- [ ] `JwtTokenFilter.java` - `/auth/login` is in the bypass list
- [ ] `CorsConfig.java` - Uses exact origins (no wildcards)
- [ ] `CorsConfig.java` - `allowCredentials(true)` is set
- [ ] Remove ALL `@CrossOrigin` annotations from controllers
- [ ] `WebSecurityConfig.java` - `.cors(withDefaults())` is enabled
- [ ] `WebSecurityConfig.java` - `.requestMatchers("/auth/**").permitAll()`
- [ ] **Restart backend server** after changes
- [ ] Test OPTIONS request with curl (should return 200)
- [ ] Check backend console logs for CORS errors

---

## Expected Behavior

### When Working Correctly:

**Browser Network Tab:**
```
1. OPTIONS /auth/login → 200 OK
   Response Headers:
   - Access-Control-Allow-Origin: http://localhost:4200
   - Access-Control-Allow-Credentials: true

2. POST /auth/login → 200 OK
   Request Headers:
   - Origin: http://localhost:4200
   - Content-Type: application/json
   Response Headers:
   - Access-Control-Allow-Origin: http://localhost:4200
   - Set-Cookie: refresh_token=...; HttpOnly
   Response Body:
   {
     "message": "Login successfully",
     "status": "OK",
     "data": {
       "token": "eyJhbGc...",
       "username": "...",
       "roles": ["ROLE_USER"]
     }
   }
```

---

## Summary

**Problem:** Frontend 401, Postman works  
**Cause:** CORS preflight failing OR JWT filter blocking login endpoint  
**Solution:**  
1. Verify backend JWT filter bypasses `/auth/login`
2. Verify backend CORS config uses exact origins with credentials
3. Remove `@CrossOrigin` annotations
4. Restart backend
5. Test with curl OPTIONS request

**Next Step:** Check Network tab in browser to see if OPTIONS request succeeds or fails.

---

**Date:** 2025-01-18  
**Status:** Awaiting backend CORS/Filter fix
