# OAuth Google Login Implementation

## Summary
Successfully implemented OAuth callback handler for Google (and Facebook) social login. The implementation saves authentication tokens to localStorage and redirects users to the user-profile page for profile completion.

## Implementation Details

### 1. **New Component: AuthCallbackComponent**
   - **Location**: `src/app/components/auth-callback/`
   - **Purpose**: Handles OAuth callback from backend after successful Google/Facebook authentication
   - **Files created**:
     - `auth-callback.component.ts` - Main component logic
     - `auth-callback.component.html` - Loading and error UI
     - `auth-callback.component.scss` - Styling

### 2. **Updated Services**

#### TokenService (`src/app/services/token.service.ts`)
   - **Added methods**:
     - `getRefreshToken()` - Retrieves refresh token from localStorage
     - `setRefreshToken(refreshToken: string)` - Saves refresh token to localStorage
   - **Updated methods**:
     - `removeToken()` - Now also removes refresh token

### 3. **Updated Routing**

#### app-routing.module.ts
   - **New route added**: 
     ```typescript
     { path: 'auth/callback', component: AuthCallbackComponent }
     ```
   - This route handles redirects from the backend OAuth flow

#### app.module.ts
   - Declared `AuthCallbackComponent` in the module

## How It Works

### Updated Flow Diagram
```
1. User clicks "Login with Google" button
   ↓
2. Frontend calls backend `/auth/social-login?login_type=google`
   ↓
3. Backend returns Google OAuth URL
   ↓
4. User is redirected to Google login page
   ↓
5. User authenticates with Google
   ↓
6. Google redirects to backend: `http://localhost:8080/auth/social/callback?state=google&code=...`
   ↓
7. Backend processes OAuth code and validates user
   ↓
8. ⚠️ Backend MUST redirect (HTTP 302) to Angular frontend:
   `http://localhost:4200/auth/callback?token=...&refresh_token=...&id=...&username=...&roles=...`
   ↓
9. Angular AuthCallbackComponent receives URL parameters
   ↓
10. Component saves:
    - Access token (via tokenService.setToken())
    - Refresh token (via tokenService.setRefreshToken())
    - User data (via userService.saveUserResponseToLocalStorage())
   ↓
11. Cart is refreshed (cartService.refreshCart())
   ↓
12. User is redirected to /user-profile
```

### Backend Response Format
The backend should NOT return JSON directly. Instead, it should **redirect** to the Angular frontend with URL parameters:

**Redirect URL Format:**
```
http://localhost:4200/auth/callback?token=eyJhbGciOiJIUzI1NiJ9...&refresh_token=3393a01f-3dee-43f1-b2d0-ce1bc3665166&id=25&username=ncduykhang.05_1760523316888&roles=["ROLE_GUEST"]
```

**URL Parameters:**
- `token` - JWT access token
- `refresh_token` - Refresh token for token renewal
- `id` - User ID
- `username` - Username
- `roles` - JSON array of user roles (URL encoded)

### Data Storage
The component stores the following in localStorage:
- **access_token**: JWT access token for API authentication
- **refresh_token**: Token for refreshing the access token
- **user**: User profile object with id, username, email, role

## Backend Configuration Required

**CRITICAL**: The backend must redirect to the Angular frontend after processing the OAuth callback, not return JSON directly.

### Required Backend Changes

After the backend receives the OAuth callback at `http://localhost:8080/auth/social/callback` and processes the authentication, it **MUST redirect** to the Angular frontend with the user data as URL parameters:

```java
// Backend should redirect to Angular frontend with tokens
String frontendCallbackUrl = "http://localhost:4200/auth/callback?" +
    "token=" + URLEncoder.encode(token, "UTF-8") +
    "&refresh_token=" + URLEncoder.encode(refreshToken, "UTF-8") +
    "&id=" + userId +
    "&username=" + URLEncoder.encode(username, "UTF-8") +
    "&roles=" + URLEncoder.encode(rolesJson, "UTF-8");

return ResponseEntity.status(HttpStatus.FOUND)
    .location(URI.create(frontendCallbackUrl))
    .build();
```

### Example Backend Redirect URL

Instead of returning JSON, the backend should redirect to:
```
http://localhost:4200/auth/callback?token=eyJhbGciOiJIUzI1NiJ9...&refresh_token=3393a01f-3dee-43f1-b2d0-ce1bc3665166&id=25&username=ncduykhang.05_1760523316888&roles=["ROLE_GUEST"]
```

The Angular frontend will:
1. Extract these parameters from the URL
2. Save tokens to localStorage
3. Save user data to localStorage
4. Refresh the cart
5. Redirect to `/user-profile`

### Alternative: JSON Response (Not Recommended)

If the backend cannot redirect, you would need to handle the JSON response differently, but this creates CORS and security issues. **Redirection is the standard OAuth flow.**

## Testing Instructions

### Manual Testing Steps

1. **Start the dev server**:
   ```powershell
   npm start
   ```

2. **Navigate to login page**:
   ```
   http://localhost:4200/login
   ```

3. **Click "Login with Google"** button

4. **Verify the flow**:
   - Should redirect to Google login
   - After Google auth, should redirect back to `http://localhost:4200/auth/callback`
   - Should show loading spinner
   - Should redirect to `http://localhost:4200/user-profile`

5. **Verify localStorage**:
   - Open browser DevTools → Application → Local Storage
   - Check for keys:
     - `access_token`
     - `refresh_token`
     - `user`

6. **Verify authentication**:
   - Navigate to protected routes (e.g., `/orders`, `/user-profile`)
   - Should not be redirected to login

### Error Handling

The component handles:
- **Invalid callback parameters**: Shows error and redirects to login
- **Backend errors**: Displays error message and redirects to login after 3 seconds
- **Network errors**: Shows generic error message

## Files Modified

1. ✅ `src/app/components/auth-callback/auth-callback.component.ts` - **NEW**
2. ✅ `src/app/components/auth-callback/auth-callback.component.html` - **NEW**
3. ✅ `src/app/components/auth-callback/auth-callback.component.scss` - **NEW**
4. ✅ `src/app/services/token.service.ts` - **UPDATED**
5. ✅ `src/app/app-routing.module.ts` - **UPDATED**
6. ✅ `src/app/app.module.ts` - **UPDATED**

## Next Steps

1. **Backend Configuration**: Ensure backend redirects OAuth callback to `http://localhost:4200/auth/callback`
2. **Test with Google**: Verify full OAuth flow works end-to-end
3. **Test with Facebook**: Verify Facebook OAuth also works (same component handles both)
4. **User Profile Completion**: Users can now update their profile at `/user-profile`

## Notes

- The component supports both Google and Facebook OAuth (detected via `state` parameter)
- Refresh tokens are now properly stored and can be used for token refresh functionality
- The user is redirected to `/user-profile` (not home) to encourage profile completion
- Cart data is automatically refreshed after login to associate with the user account
