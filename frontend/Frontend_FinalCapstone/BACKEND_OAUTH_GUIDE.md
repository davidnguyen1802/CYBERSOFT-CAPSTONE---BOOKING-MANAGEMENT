# Backend Implementation Guide for OAuth Callback

## Problem
Currently, the backend returns JSON at `http://localhost:8080/auth/social/callback`, which keeps the user on the backend URL. The frontend cannot access this data due to different origins.

## Solution
The backend must **redirect** the user to the Angular frontend with the authentication data as URL parameters.

---

## Backend Implementation (Java/Spring Boot Example)

### Current Backend Response (INCORRECT)
```java
@GetMapping("/auth/social/callback")
public ResponseEntity<?> handleOAuthCallback(
    @RequestParam String state,
    @RequestParam String code
) {
    // Process OAuth code...
    
    // ❌ THIS IS WRONG - Returns JSON but user stays on backend URL
    return ResponseEntity.ok(Map.of(
        "message", "Login successfully",
        "status", "OK",
        "data", Map.of(
            "token", token,
            "refresh_token", refreshToken,
            "id", userId,
            "username", username,
            "roles", roles
        )
    ));
}
```

### Correct Backend Implementation (REDIRECT)
```java
@GetMapping("/auth/social/callback")
public ResponseEntity<?> handleOAuthCallback(
    @RequestParam String state,
    @RequestParam String code
) throws UnsupportedEncodingException {
    
    // Process OAuth code and get user data
    OAuthResponse oauthResponse = processOAuthCode(state, code);
    
    // Build redirect URL to Angular frontend
    String frontendUrl = "http://localhost:4200/auth/callback";
    
    StringBuilder redirectUrl = new StringBuilder(frontendUrl);
    redirectUrl.append("?token=").append(URLEncoder.encode(oauthResponse.getToken(), "UTF-8"));
    redirectUrl.append("&refresh_token=").append(URLEncoder.encode(oauthResponse.getRefreshToken(), "UTF-8"));
    redirectUrl.append("&id=").append(oauthResponse.getUserId());
    redirectUrl.append("&username=").append(URLEncoder.encode(oauthResponse.getUsername(), "UTF-8"));
    
    // Convert roles array to JSON string
    String rolesJson = new ObjectMapper().writeValueAsString(oauthResponse.getRoles());
    redirectUrl.append("&roles=").append(URLEncoder.encode(rolesJson, "UTF-8"));
    
    // Return 302 redirect
    return ResponseEntity
        .status(HttpStatus.FOUND)
        .location(URI.create(redirectUrl.toString()))
        .build();
}
```

### Alternative Using HttpServletResponse
```java
@GetMapping("/auth/social/callback")
public void handleOAuthCallback(
    @RequestParam String state,
    @RequestParam String code,
    HttpServletResponse response
) throws IOException {
    
    // Process OAuth code
    OAuthResponse oauthResponse = processOAuthCode(state, code);
    
    // Build redirect URL
    String redirectUrl = String.format(
        "http://localhost:4200/auth/callback?token=%s&refresh_token=%s&id=%d&username=%s&roles=%s",
        URLEncoder.encode(oauthResponse.getToken(), "UTF-8"),
        URLEncoder.encode(oauthResponse.getRefreshToken(), "UTF-8"),
        oauthResponse.getUserId(),
        URLEncoder.encode(oauthResponse.getUsername(), "UTF-8"),
        URLEncoder.encode(new ObjectMapper().writeValueAsString(oauthResponse.getRoles()), "UTF-8")
    );
    
    // Redirect to Angular frontend
    response.sendRedirect(redirectUrl);
}
```

---

## Example Redirect URL

After processing, the backend redirects to:
```
http://localhost:4200/auth/callback?token=eyJhbGciOiJIUzI1NiJ9.eyJ1c2VySWQiOjI1LCJzdWIiOiJuY2R1eWtoYW5nLjA1QGdtYWlsLmNvbSIsImV4cCI6MTc2NDI3NTc1NH0.d2FUbOda63TEo4n_zYhvzSBkBzFy0ZFXksOwAID56Ck&refresh_token=3393a01f-3dee-43f1-b2d0-ce1bc3665166&id=25&username=ncduykhang.05_1760523316888&roles=%5B%22ROLE_GUEST%22%5D
```

---

## Frontend Flow (Already Implemented)

1. User lands at `http://localhost:4200/auth/callback?token=...&refresh_token=...&id=...&username=...&roles=...`
2. `AuthCallbackComponent` extracts URL parameters
3. Saves `token` to localStorage via `tokenService.setToken()`
4. Saves `refresh_token` to localStorage via `tokenService.setRefreshToken()`
5. Creates user object and saves via `userService.saveUserResponseToLocalStorage()`
6. Refreshes cart via `cartService.refreshCart()`
7. Redirects to `/user-profile`

---

## Security Considerations

### ⚠️ Token in URL Parameters
Passing tokens in URL parameters is not the most secure method because:
- URLs can be logged in browser history
- URLs can be logged in server logs
- URLs can be leaked via Referer headers

### Better Alternative: Use Session/Cookie
```java
@GetMapping("/auth/social/callback")
public void handleOAuthCallback(
    @RequestParam String state,
    @RequestParam String code,
    HttpServletResponse response,
    HttpSession session
) throws IOException {
    
    OAuthResponse oauthResponse = processOAuthCode(state, code);
    
    // Store tokens in HTTP-only cookies (more secure)
    Cookie tokenCookie = new Cookie("access_token", oauthResponse.getToken());
    tokenCookie.setHttpOnly(true);
    tokenCookie.setSecure(true); // Only over HTTPS
    tokenCookie.setPath("/");
    tokenCookie.setMaxAge(3600); // 1 hour
    response.addCookie(tokenCookie);
    
    Cookie refreshTokenCookie = new Cookie("refresh_token", oauthResponse.getRefreshToken());
    refreshTokenCookie.setHttpOnly(true);
    refreshTokenCookie.setSecure(true);
    refreshTokenCookie.setPath("/");
    refreshTokenCookie.setMaxAge(604800); // 7 days
    response.addCookie(refreshTokenCookie);
    
    // Redirect with only user info
    String redirectUrl = String.format(
        "http://localhost:4200/auth/callback?id=%d&username=%s&roles=%s",
        oauthResponse.getUserId(),
        URLEncoder.encode(oauthResponse.getUsername(), "UTF-8"),
        URLEncoder.encode(new ObjectMapper().writeValueAsString(oauthResponse.getRoles()), "UTF-8")
    );
    
    response.sendRedirect(redirectUrl);
}
```

But this requires frontend changes to read cookies instead of localStorage.

---

## Testing

### 1. Test the Redirect
After implementing, test by clicking "Login with Google":
- Backend should redirect to `http://localhost:4200/auth/callback?token=...`
- Frontend should show loading spinner
- Frontend should redirect to `/user-profile`

### 2. Verify localStorage
Open DevTools → Application → Local Storage:
- `access_token` should be set
- `refresh_token` should be set
- `user` should contain user data

### 3. Verify Authentication
- Navigate to protected routes (e.g., `/orders`)
- Should NOT be redirected to login
- API calls should include `Authorization: Bearer <token>` header

---

## Configuration for Production

For production, update the redirect URL:

```java
// In application.properties or application.yml
frontend.oauth.callback.url=https://yourdomain.com/auth/callback

// In your controller
@Value("${frontend.oauth.callback.url}")
private String frontendCallbackUrl;

@GetMapping("/auth/social/callback")
public void handleOAuthCallback(...) {
    String redirectUrl = frontendCallbackUrl + "?token=...";
    response.sendRedirect(redirectUrl);
}
```

---

## Summary

✅ **Backend must redirect to Angular frontend** (not return JSON)
✅ **Frontend extracts tokens from URL parameters**
✅ **Frontend saves tokens to localStorage**
✅ **Frontend redirects to /user-profile**

This is the standard OAuth callback flow used by most web applications.
