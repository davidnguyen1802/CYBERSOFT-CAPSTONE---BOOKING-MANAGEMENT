# JWT Authentication Frontend Implementation

## Overview

This document describes the frontend changes made to work with the production-grade JWT authentication system that uses:
- **Access tokens** in `Authorization: Bearer <token>` header
- **Refresh tokens** in HttpOnly cookies (managed by backend)
- **Automatic token refresh** when access token expires

## Changes Made

### 1. TokenService (`src/app/services/token.service.ts`)

**Removed:**
- `getRefreshToken()` method
- `setRefreshToken()` method
- `REFRESH_TOKEN_KEY` constant

**Reason:** Refresh tokens are now stored in HttpOnly cookies by the backend and cannot be accessed by JavaScript. This is more secure as it prevents XSS attacks from stealing refresh tokens.

**Remaining methods:**
```typescript
getToken(): string              // Get access token from localStorage
setToken(token: string): void   // Save access token to localStorage
removeToken(): void             // Clear access token
getUserId(): number             // Extract user ID from JWT token
isTokenExpired(): boolean       // Check if access token is expired
```

### 2. UserService (`src/app/services/user.service.ts`)

**Added endpoints:**
```typescript
private apiRefresh = `${this.baseUrl}/auth/refresh`;
private apiLogout = `${this.baseUrl}/auth/logout`;
```

**Added methods:**
```typescript
refreshToken(): Observable<any> {
  return this.http.post(this.apiRefresh, {}, {
    headers: this.httpUtilService.createHeaders(),
    withCredentials: true // Required to send HttpOnly cookie
  });
}

logout(): Observable<any> {
  return this.http.post(this.apiLogout, {}, {
    headers: this.httpUtilService.createHeaders(),
    withCredentials: true // Required to send HttpOnly cookie
  });
}
```

**Updated methods:**
```typescript
login(loginDTO: LoginDTO): Observable<any> {    
  return this.http.post(this.apiLogin, loginDTO, {
    ...this.apiConfig,
    withCredentials: true // Enable cookies for refresh token
  });
}
```

**Key change:** Added `withCredentials: true` to enable sending/receiving cookies.

### 3. TokenInterceptor (`src/app/interceptors/token.interceptor.ts`)

**Complete rewrite** to handle automatic token refresh:

**Features:**
1. **Adds Authorization header** to all requests with current access token
2. **Catches 401 errors** with `TOKEN_EXPIRED` error code
3. **Automatically calls `/auth/refresh`** to get new access token
4. **Retries failed request** with new token
5. **Queues concurrent requests** while refresh is in progress
6. **Redirects to login** if refresh fails

**Flow:**
```
API Request → 401 TOKEN_EXPIRED → Call /auth/refresh → Get new token → Retry request
                                       ↓ (if fails)
                                  Clear tokens → Redirect to /login
```

**Implementation:**
```typescript
intercept(req: HttpRequest<any>, next: HttpHandler): Observable<HttpEvent<any>> {
  // Add access token
  const token = this.tokenService.getToken();
  if (token) {
    req = this.addToken(req, token);
  }

  return next.handle(req).pipe(
    catchError((error: HttpErrorResponse) => {
      // Handle TOKEN_EXPIRED automatically
      if (error.status === 401 && error.error?.error === 'TOKEN_EXPIRED') {
        return this.handle401Error(req, next);
      }
      return throwError(() => error);
    })
  );
}
```

### 4. LoginComponent (`src/app/components/login/login.component.ts`)

**Removed:**
- Handling of `refresh_token` from response body
- `setRefreshToken()` call

**Updated:**
```typescript
// Before:
const token = authData.token;
const refreshToken = authData.refresh_token;
this.tokenService.setToken(token);
if (refreshToken) {
  this.tokenService.setRefreshToken(refreshToken);
}

// After:
const token = authData.token;
this.tokenService.setToken(token);
// Refresh token is automatically stored in HttpOnly cookie by backend
```

### 5. AuthCallbackComponent (`src/app/components/auth-callback/auth-callback.component.ts`)

**Updated OAuth flow:**
1. Removed refresh token handling from URL parameters
2. Added `withCredentials: true` to HTTP calls
3. Simplified to only save access token

**Changes:**
```typescript
// In processOAuthTokens():
this.tokenService.setToken(token);
// Removed: this.tokenService.setRefreshToken(refreshToken);

// In exchangeCodeForTokens():
this.http.get<any>(callbackUrl, { 
  withCredentials: true // Enable cookies for refresh token
}).subscribe({...});
```

### 6. HeaderComponent (`src/app/components/header\header.component.ts`)

**Updated logout flow:**
```typescript
// Before:
this.tokenService.removeToken();
this.router.navigate(['/login']);

// After:
this.userService.logout().subscribe({
  next: () => console.log('Logged out successfully'),
  error: (error) => console.error('Logout error:', error),
  complete: () => {
    // Clear local token regardless of backend response
    this.tokenService.removeToken();
    this.userService.removeUserFromLocalStorage();
    this.userResponse = null;
    this.isLoggedIn = false;
    this.authStateService.notifyLogout();
    this.router.navigate(['/login']);
  }
});
```

**Why:** Calling backend `/auth/logout` ensures the HttpOnly refresh token cookie is cleared on the server side.

## Authentication Flow

### Login Flow

```
1. User submits credentials
   ↓
2. Frontend: POST /auth/login with withCredentials: true
   ↓
3. Backend: Returns { token: "..." } + Set-Cookie: refresh_token=...; HttpOnly
   ↓
4. Frontend: Saves access token to localStorage
   ↓
5. Browser: Automatically stores HttpOnly cookie
   ↓
6. Frontend: Redirects to home/admin page
```

### API Request Flow

```
1. Component makes API call
   ↓
2. TokenInterceptor adds Authorization: Bearer <access_token>
   ↓
3. Backend validates token
   ↓
   ├─ Valid → Returns data
   │
   └─ Expired (401 TOKEN_EXPIRED) → Interceptor catches error
                                      ↓
                                  POST /auth/refresh (with cookie)
                                      ↓
                                  Backend returns new access token
                                      ↓
                                  Interceptor saves new token
                                      ↓
                                  Retry original request with new token
```

### Token Refresh Flow

```
1. Access token expires (15 min)
   ↓
2. API request returns 401 TOKEN_EXPIRED
   ↓
3. TokenInterceptor automatically calls POST /auth/refresh
   ↓
4. Browser sends HttpOnly refresh_token cookie automatically
   ↓
5. Backend validates refresh token
   ↓
   ├─ Valid → Returns new access token + new refresh token cookie
   │          ↓
   │      Save new access token
   │          ↓
   │      Retry failed request
   │
   └─ Invalid/Expired → Clear tokens
                         ↓
                     Redirect to /login
```

### Logout Flow

```
1. User clicks logout
   ↓
2. Frontend: POST /auth/logout with withCredentials: true
   ↓
3. Backend: Revokes refresh token + clears cookie
   ↓
4. Frontend: Removes access token from localStorage
   ↓
5. Redirects to /login
```

## Security Improvements

### Before (Less Secure)
- ❌ Refresh tokens stored in localStorage (vulnerable to XSS)
- ❌ Manual token refresh implementation needed in each component
- ❌ No automatic token rotation
- ❌ Refresh tokens could be stolen via JavaScript

### After (More Secure)
- ✅ Refresh tokens in HttpOnly cookies (protected from XSS)
- ✅ Automatic token refresh via interceptor (transparent to components)
- ✅ Token rotation on every refresh (detects token theft)
- ✅ Refresh tokens cannot be accessed by JavaScript
- ✅ Short-lived access tokens (15 min) reduce attack window
- ✅ Long-lived refresh tokens (14 days) reduce login frequency

## Browser Compatibility

**Requirements:**
- Cookies must be enabled
- CORS must allow credentials: `Access-Control-Allow-Credentials: true`
- Backend must use `SameSite=Strict` or `SameSite=Lax` for cookies

**HTTPS Required in Production:**
- HttpOnly cookies with `Secure` flag only work over HTTPS
- Development (localhost) works with HTTP

## Testing

### Manual Testing Steps

**1. Test Login:**
```bash
# Open browser DevTools → Network tab
# Login with credentials
# Check:
- Response has "token" field
- Response does NOT have "refresh_token" field
- Set-Cookie header includes "refresh_token; HttpOnly"
- localStorage has "access_token"
```

**2. Test Automatic Refresh:**
```bash
# Option A: Wait 15 minutes for token to expire naturally
# Option B: Manually expire token by changing expiration in backend to 1 minute

# After token expires:
- Make any API request
- Check Network tab:
  - First request fails with 401 TOKEN_EXPIRED
  - Automatic POST /auth/refresh is triggered
  - Original request is retried with new token
  - Request succeeds
```

**3. Test Logout:**
```bash
# After logging in:
- Click logout button
- Check Network tab:
  - POST /auth/logout is called
  - Set-Cookie header clears refresh_token
- Check Application → Cookies:
  - refresh_token cookie is removed
- Check localStorage:
  - access_token is removed
```

### Using Browser DevTools

**Check Cookies:**
```
DevTools → Application → Cookies → http://localhost:8080
- Look for "refresh_token" cookie
- Verify HttpOnly, Secure, SameSite flags
```

**Check localStorage:**
```
DevTools → Application → Local Storage → http://localhost:4200
- Look for "access_token"
- Should NOT contain "refresh_token"
```

**Check Network Requests:**
```
DevTools → Network → Filter by XHR/Fetch
- Check request headers for "Authorization: Bearer ..."
- Check response Set-Cookie headers
- Check if refresh is triggered on 401 errors
```

## Common Issues & Troubleshooting

### Issue: "Token refresh failed immediately after login"
**Cause:** Backend might not be setting HttpOnly cookie correctly  
**Solution:** 
- Check backend `ResponseCookie` configuration
- Verify `httpOnly(true)`, `secure(false)` for localhost
- Check CORS configuration allows credentials

### Issue: "Cookies not being sent with requests"
**Cause:** Missing `withCredentials: true` in HTTP calls  
**Solution:** 
- Ensure all auth-related requests have `withCredentials: true`
- Check CORS `Access-Control-Allow-Credentials: true`

### Issue: "Infinite refresh loop"
**Cause:** Refresh endpoint also returns 401 TOKEN_EXPIRED  
**Solution:** 
- Backend must accept expired access tokens on `/auth/refresh`
- Only refresh token validation should matter

### Issue: "Redirect to login on every page refresh"
**Cause:** Access token not being saved to localStorage  
**Solution:** 
- Check `tokenService.setToken()` is called after login
- Verify localStorage quota not exceeded

### Issue: "CORS errors with credentials"
**Cause:** Backend CORS misconfiguration  
**Solution:**
```java
// Backend CORS config
.allowedOrigins("http://localhost:4200")
.allowCredentials(true)
.allowedHeaders("*")
.allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
```

## Migration Checklist

- [x] Update TokenService to remove refresh token methods
- [x] Add `withCredentials: true` to login endpoint
- [x] Add `refreshToken()` method to UserService
- [x] Add `logout()` method to UserService
- [x] Update TokenInterceptor with automatic refresh logic
- [x] Update LoginComponent to not handle refresh_token from response
- [x] Update AuthCallbackComponent for OAuth flow
- [x] Update HeaderComponent logout to call backend endpoint
- [ ] Test login flow end-to-end
- [ ] Test automatic token refresh
- [ ] Test logout clears cookies
- [ ] Test OAuth login flow
- [ ] Verify no refresh_token in localStorage
- [ ] Verify refresh_token cookie is HttpOnly

## Next Steps

1. **Start development server:**
   ```bash
   npm start
   ```

2. **Test login:**
   - Navigate to http://localhost:4200/login
   - Login with valid credentials
   - Check DevTools for token and cookie

3. **Test token expiration:**
   - Wait 15 minutes OR
   - Temporarily change backend expiration to 1 minute
   - Make any API request
   - Verify automatic refresh happens

4. **Test logout:**
   - Click logout button
   - Verify cookie is cleared
   - Verify redirect to login page

5. **Test OAuth:**
   - Click "Login with Google"
   - Complete OAuth flow
   - Verify tokens are saved
   - Verify redirect to user-profile

## Production Deployment Notes

**Backend Requirements:**
- Set `jwt.secret` environment variable (256+ bits)
- Enable HTTPS/TLS
- Configure CORS for production domain
- Set cookie `secure: true` in production
- Set `jwt.access-expiration-ms` to appropriate value (default: 900000 = 15 min)
- Set `jwt.refresh-expiration-ms` to appropriate value (default: 1209600000 = 14 days)

**Frontend Requirements:**
- Deploy to HTTPS domain (required for Secure cookies)
- Update CORS origin in backend config
- Test token refresh works in production
- Verify cookies are set with Secure flag

---

**Last Updated:** 2025-01-18  
**Version:** 1.0  
**Compatible with:** Backend JWT Authentication Production Guide v1.0
