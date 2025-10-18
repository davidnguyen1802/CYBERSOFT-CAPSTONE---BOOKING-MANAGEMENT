# 🔐 AUTHENTICATION FLOW DOCUMENTATION

## Tổng quan
Tài liệu này mô tả chi tiết flow xác thực của ứng dụng Airbnb Clone, bao gồm login, register, profile management, và OAuth integration.

---

## 📋 Các vấn đề đã phát hiện và sửa

### 1. **TokenService.isTokenExpired() - Logic không đúng**
**Vấn đề:** 
- Khi không có token (`getToken() == null`), hàm trả về `false` (không expired)
- Điều này sai logic vì không có token = không xác thực được = nên coi như expired

**Đã sửa:**
```typescript
// Before ❌
isTokenExpired(): boolean { 
    if(this.getToken() == null) {
        return false; // WRONG: should return true
    }
    return this.jwtHelperService.isTokenExpired(this.getToken()!);
}

// After ✅
isTokenExpired(): boolean { 
    const token = this.getToken();
    if(!token || token === '') {
        console.log('🔑 No token to check expiration');
        return true; // CORRECT: no token = expired
    }
    try {
        const expired = this.jwtHelperService.isTokenExpired(token);
        console.log(`🔑 Token expired check: ${expired}`);
        return expired;
    } catch (error) {
        console.error('🔑 Error checking token expiration:', error);
        return true; // Invalid token = expired
    }
}
```

### 2. **TokenService.getUserId() - Thiếu error handling**
**Vấn đề:**
- Không handle lỗi khi decode token bị lỗi (token invalid/malformed)

**Đã sửa:**
```typescript
// Added try-catch block
getUserId(): number {
    let token = this.getToken();
    if (!token) {
        console.log('🔑 No token found, cannot get user ID');
        return 0;
    }
    try {
        let userObject = this.jwtHelperService.decodeToken(token);
        const userId = 'userId' in userObject ? parseInt(userObject['userId']) : 0;
        console.log(`🔑 User ID from token: ${userId}`);
        return userId;
    } catch (error) {
        console.error('🔑 Error decoding token:', error);
        return 0;
    }
}
```

### 3. **Login Component - Thiếu validation token trước khi save**
**Vấn đề:**
- Không kiểm tra token có hợp lệ trước khi lưu vào localStorage
- Thiếu log chi tiết để debug

**Đã sửa:**
```typescript
// Added token validation
if (!token || token.trim() === '') {
    console.error('❌ Invalid token received');
    alert('Invalid token received from server');
    return;
}

// Added detailed logging
console.log('🔑 Token received:', token ? 'Yes' : 'No');
console.log('🔑 Refresh token received:', refreshToken ? 'Yes' : 'No');
```

### 4. **Register Component - Thiếu validation trước khi submit**
**Vấn đề:**
- Không validate password match trước khi gửi request
- Không check terms acceptance

**Đã sửa:**
```typescript
// Added validations
if (this.password !== this.retypePassword) {
    console.error('❌ Passwords do not match');
    alert('Passwords do not match!');
    return;
}

if (!this.isAccepted) {
    console.error('❌ Terms not accepted');
    alert('Please accept the terms and conditions');
    return;
}
```

### 5. **Auth Callback - Thiếu error handling cho OAuth tokens**
**Vấn đề:**
- Không validate token từ OAuth trước khi lưu
- Không có try-catch để handle errors

**Đã sửa:**
```typescript
// Added validation
if (!token || token.trim() === '') {
    console.error('❌ Invalid OAuth token received');
    this.errorMessage = 'Invalid token received';
    this.loading = false;
    setTimeout(() => {
        this.router.navigate(['/login']);
    }, 3000);
    return;
}

// Added try-catch
try {
    this.tokenService.setToken(token);
    // ... rest of code
} catch (error) {
    console.error('❌ Error processing OAuth tokens:', error);
    this.errorMessage = 'Failed to process authentication';
    // ... handle error
}
```

### 6. **Header Component - Không xử lý 401/403 errors**
**Vấn đề:**
- Khi API trả về 401 (Unauthorized), không clear token invalid
- User vẫn thấy mình logged in nhưng các API khác sẽ fail

**Đã sửa:**
```typescript
error: (error) => {
    console.error('❌ Error loading user profile in header:', error);
    console.log('🚪 Clearing login state due to profile load error');
    this.isLoggedIn = false;
    this.userResponse = null;
    
    // Clear invalid token on 401/403
    if (error.status === 401 || error.status === 403) {
        console.log('🔑 Clearing invalid token');
        this.tokenService.removeToken();
    }
}
```

### 7. **Token Interceptor - Không log requests**
**Vấn đề:**
- Khó debug khi không biết request nào có token, request nào không

**Đã sửa:**
```typescript
if (token && token.trim() !== '') {
    console.log(`🔒 Token interceptor: Adding token to ${req.method} ${req.url}`);
    req = req.clone({
        setHeaders: {
            Authorization: `Bearer ${token}`,
        },
    });
} else {
    console.log(`🔓 Token interceptor: No token for ${req.method} ${req.url}`);
}
```

---

## 🔄 AUTHENTICATION FLOW

### **1. NORMAL LOGIN FLOW**

```
┌─────────────┐
│   User      │
│ enters      │
│ credentials │
└──────┬──────┘
       │
       ▼
┌─────────────────────────────────────────────────┐
│ LoginComponent.login()                          │
│ - Validate input                                │
│ - Create LoginDTO                               │
│ - Console: "🔐 Login attempt started"          │
└──────┬──────────────────────────────────────────┘
       │
       ▼
┌─────────────────────────────────────────────────┐
│ UserService.login(loginDTO)                     │
│ - POST /auth/login                              │
│ - Console: "🔵 API Call: POST /auth/login"     │
└──────┬──────────────────────────────────────────┘
       │
       ▼
┌─────────────────────────────────────────────────┐
│ Backend Response:                               │
│ {                                               │
│   code: 200,                                    │
│   data: {                                       │
│     token: "jwt_token",                         │
│     refresh_token: "refresh_token",             │
│     roles: ["ROLE_GUEST"],                      │
│     ...                                         │
│   }                                             │
│ }                                               │
└──────┬──────────────────────────────────────────┘
       │
       ▼
┌─────────────────────────────────────────────────┐
│ LoginComponent - Response Handler               │
│ 1. Validate response structure                  │
│ 2. Validate token not empty                     │
│ 3. Save tokens:                                 │
│    - tokenService.setToken(token)               │
│    - tokenService.setRefreshToken(refresh)      │
│    Console: "🔑 Token saved to localStorage"   │
│ 4. Notify login state:                          │
│    - authStateService.notifyLogin()             │
│    Console: "🔐 Auth State: User logged in"    │
│ 5. Refresh cart:                                │
│    - cartService.refreshCart()                  │
│    Console: "🛒 Cart refreshed..."             │
│ 6. Navigate based on role:                      │
│    - Admin: /admin                              │
│    - Others: /                                  │
└──────┬──────────────────────────────────────────┘
       │
       ▼
┌─────────────────────────────────────────────────┐
│ HeaderComponent - Auto Update                   │
│ - Subscribes to authStateService.loginState$    │
│ - Calls checkLoginStatus()                      │
│ - Loads user profile via API                    │
│ - Console: "🔐 Login state changed in header"  │
└─────────────────────────────────────────────────┘
```

### **2. OAUTH LOGIN FLOW (Google/Facebook)**

```
┌─────────────┐
│   User      │
│   clicks    │
│ "Login with │
│   Google"   │
└──────┬──────┘
       │
       ▼
┌─────────────────────────────────────────────────┐
│ LoginComponent.loginWithGoogle()                │
│ - GET /auth/social-login?login_type=google      │
│ - Console: "Initiating Google login..."        │
└──────┬──────────────────────────────────────────┘
       │
       ▼
┌─────────────────────────────────────────────────┐
│ Backend returns OAuth URL                       │
│ - Redirect to Google OAuth page                 │
│ - window.location.href = oauthUrl               │
└──────┬──────────────────────────────────────────┘
       │
       ▼
┌─────────────────────────────────────────────────┐
│ User logs in on Google                          │
│ Google redirects to callback URL:               │
│ /auth-callback?token=...&refresh_token=...      │
└──────┬──────────────────────────────────────────┘
       │
       ▼
┌─────────────────────────────────────────────────┐
│ AuthCallbackComponent.ngOnInit()                │
│ 1. Parse query parameters                       │
│    Console: "🔐 OAuth Callback Component init" │
│ 2. Check for errors                             │
│ 3. Validate token & id present                  │
└──────┬──────────────────────────────────────────┘
       │
       ▼
┌─────────────────────────────────────────────────┐
│ AuthCallbackComponent.processOAuthTokens()      │
│ 1. Validate token not empty                     │
│ 2. Save tokens to localStorage                  │
│    Console: "✅ OAuth tokens saved"            │
│ 3. Notify login state                           │
│    Console: "🔐 Notifying login state change"  │
│ 4. Refresh cart                                 │
│    Console: "🛒 Refreshing cart..."            │
│ 5. Navigate to /user-profile                    │
│    Console: "➡️ Redirecting to /user-profile" │
└─────────────────────────────────────────────────┘
```

### **3. REGISTER FLOW**

```
┌─────────────┐
│   User      │
│   fills     │
│ register    │
│   form      │
└──────┬──────┘
       │
       ▼
┌─────────────────────────────────────────────────┐
│ RegisterComponent.register()                    │
│ 1. Validate passwords match                     │
│    Console: "❌ Passwords do not match" (if no)│
│ 2. Validate terms acceptance                    │
│    Console: "❌ Terms not accepted" (if no)    │
│ 3. Create RegisterDTO                           │
│    Console: "📝 Register attempt started"      │
└──────┬──────────────────────────────────────────┘
       │
       ▼
┌─────────────────────────────────────────────────┐
│ UserService.register(registerDTO)               │
│ - POST /auth/signup                             │
│ - Console: "🔵 API Call: POST /auth/signup"    │
└──────┬──────────────────────────────────────────┘
       │
       ▼
┌─────────────────────────────────────────────────┐
│ Success Response                                │
│ - Show confirmation dialog                      │
│   Console: "✅ Registration successful"        │
│ - Navigate to /login                            │
│   Console: "➡️ Redirecting to login page"     │
└─────────────────────────────────────────────────┘
```

### **4. PROFILE LOAD FLOW**

```
┌─────────────┐
│   User      │
│ navigates   │
│ to profile  │
└──────┬──────┘
       │
       ▼
┌─────────────────────────────────────────────────┐
│ UserProfileComponent.ngOnInit()                 │
│ 1. Get token from tokenService                  │
│    Console: "👤 UserProfileComponent init"     │
│ 2. Check if token valid                         │
│    - If invalid: redirect to /login             │
│    Console: "⚠️ No valid token found"          │
│ 3. If valid: loadUserProfile()                  │
│    Console: "✅ Valid token found"             │
└──────┬──────────────────────────────────────────┘
       │
       ▼
┌─────────────────────────────────────────────────┐
│ UserProfileComponent.loadUserProfile()          │
│ - Call userService.getMyDetailedProfile(token)  │
│ - Console: "📥 Loading user profile from API"  │
└──────┬──────────────────────────────────────────┘
       │
       ▼
┌─────────────────────────────────────────────────┐
│ UserService.getMyDetailedProfile()              │
│ - GET /users/me/details?includeDetails=true     │
│ - Console: "🔵 API Call: GET /users/me/detail" │
└──────┬──────────────────────────────────────────┘
       │
       ▼
┌─────────────────────────────────────────────────┐
│ Token Interceptor                               │
│ - Automatically adds Authorization header       │
│ - Console: "🔒 Token interceptor: Adding..."   │
└──────┬──────────────────────────────────────────┘
       │
       ▼
┌─────────────────────────────────────────────────┐
│ Backend Response:                               │
│ {                                               │
│   code: 200,                                    │
│   data: {                                       │
│     user_info: {                                │
│       id, fullname, email, role,                │
│       total_bookings, hosted_properties, etc.   │
│     }                                           │
│   }                                             │
│ }                                               │
└──────┬──────────────────────────────────────────┘
       │
       ▼
┌─────────────────────────────────────────────────┐
│ UserProfileComponent - Response Handler         │
│ 1. Parse user data                              │
│    Console: "📥 Profile API response"          │
│    Console: "👤 Raw user data from backend"    │
│ 2. Map to UserResponse object                   │
│ 3. Populate form with data                      │
│    Console: "✅ User profile loaded"           │
│ 4. Display in template                          │
└─────────────────────────────────────────────────┘
```

### **5. PROFILE UPDATE FLOW**

```
┌─────────────┐
│   User      │
│   clicks    │
│ "Edit" then │
│   "Save"    │
└──────┬──────┘
       │
       ▼
┌─────────────────────────────────────────────────┐
│ UserProfileComponent.save()                     │
│ 1. Validate form                                │
│    Console: "💾 Save button clicked"           │
│ 2. Check password match (if changing)           │
│    Console: "🔒 Password change requested"     │
│ 3. Create UpdateUserDTO                         │
│    Console: "📤 Sending update data to API"    │
└──────┬──────────────────────────────────────────┘
       │
       ▼
┌─────────────────────────────────────────────────┐
│ UserService.updateMyProfile(token, dto)         │
│ - PUT /users/me                                 │
│ - Console: "🔵 API Call: PUT /users/me"        │
└──────┬──────────────────────────────────────────┘
       │
       ▼
┌─────────────────────────────────────────────────┐
│ Token Interceptor                               │
│ - Adds Authorization header                     │
│ - Console: "🔒 Token interceptor: Adding..."   │
└──────┬──────────────────────────────────────────┘
       │
       ▼
┌─────────────────────────────────────────────────┐
│ Success Response                                │
│ - Show success alert                            │
│   Console: "✅ Profile updated successfully"   │
│ - Reload profile data                           │
│ - Exit edit mode                                │
└─────────────────────────────────────────────────┘
```

### **6. LOGOUT FLOW**

```
┌─────────────┐
│   User      │
│   clicks    │
│  "Logout"   │
└──────┬──────┘
       │
       ▼
┌─────────────────────────────────────────────────┐
│ HeaderComponent.handleItemClick(2) OR           │
│ UserProfileComponent.logout()                   │
│ Console: "🚪 Logging out user"                 │
└──────┬──────────────────────────────────────────┘
       │
       ▼
┌─────────────────────────────────────────────────┐
│ 1. tokenService.removeToken()                   │
│    Console: "🔑 Tokens removed from localStorage"│
│ 2. userService.removeUserFromLocalStorage()     │
│    Console: "💾 User data removed from localStorage"│
│ 3. authStateService.notifyLogout()              │
│    Console: "🔐 Auth State: User logged out"   │
│ 4. Clear local state                            │
│    - userResponse = null                        │
│    - isLoggedIn = false                         │
│ 5. Navigate to /login                           │
│    Console: "✅ User data cleared, redirecting"│
└──────┬──────────────────────────────────────────┘
       │
       ▼
┌─────────────────────────────────────────────────┐
│ HeaderComponent - Auto Update                   │
│ - Subscribes to authStateService.loginState$    │
│ - Calls checkLoginStatus()                      │
│ - Clears user profile display                   │
│ - Console: "🔐 Login state changed in header"  │
└─────────────────────────────────────────────────┘
```

---

## 🔒 TOKEN MANAGEMENT

### **Token Storage**
- **Access Token**: Stored in `localStorage` with key `access_token`
- **Refresh Token**: Stored in `localStorage` with key `refresh_token`
- **User Data**: NO longer stored in localStorage (always fetched from API)

### **Token Usage**
1. **Automatic Injection**: `TokenInterceptor` automatically adds token to all HTTP requests
2. **Validation**: Token is validated before being saved
3. **Expiration Check**: `isTokenExpired()` checks if token is still valid
4. **Error Handling**: Invalid/expired tokens are cleared on 401/403 errors

### **Token Lifecycle**
```
Login/OAuth → Token Saved → Token Used in Requests → Token Expires/Invalid → Clear Token → Redirect to Login
```

---

## 🛡️ SECURITY IMPROVEMENTS

### **1. No User Data in localStorage**
- ❌ Before: User data stored in localStorage (security risk)
- ✅ After: Only tokens stored, user data fetched from API

### **2. Token Validation**
- ✅ Validate token not empty before saving
- ✅ Try-catch around token decode operations
- ✅ Clear invalid tokens on 401/403 errors

### **3. Error Handling**
- ✅ All API calls have error handlers
- ✅ Detailed error logging
- ✅ User-friendly error messages

### **4. State Management**
- ✅ AuthStateService notifies components of login/logout
- ✅ Header component auto-updates on state change
- ✅ Cart refreshes on login

---

## 📝 CONSOLE LOG PATTERNS

### **Icons Used**
- 🔵 - API Calls
- 🔑 - Token Operations
- 🔐 - Authentication State
- 💾 - LocalStorage Operations
- 🛒 - Cart Operations
- 👤 - User Profile Operations
- ✅ - Success
- ❌ - Errors
- ⚠️ - Warnings
- 📥 - Loading/Receiving Data
- 📤 - Sending Data
- ➡️ - Navigation
- 🚪 - Logout
- 🔒 - Token Interceptor (adding token)
- 🔓 - Token Interceptor (no token)
- 🎯 - Component Initialization
- 🖱️ - User Interactions

### **Example Logs**
```
🔐 Login attempt started
🔵 API Call: POST /auth/login
🔑 Token saved to localStorage
🔐 Auth State: User logged in
🛒 Cart refreshed from localStorage: 2 items
➡️ Navigating to /
🎯 Header Component initialized
🔐 Login state changed in header: true
📥 Loading user profile for header...
🔒 Token interceptor: Adding token to GET http://localhost:8080/users/me
✅ Header profile loaded
👤 User info in header: { fullname: "John Doe", role: "GUEST" }
```

---

## 🐛 POTENTIAL BUGS FIXED

1. ✅ **Token expiration logic inverted** - Fixed in TokenService
2. ✅ **No error handling for token decode** - Added try-catch
3. ✅ **Token not validated before saving** - Added validation in login/OAuth
4. ✅ **Invalid tokens not cleared on 401/403** - Added in header component
5. ✅ **Password validation missing in register** - Added client-side validation
6. ✅ **OAuth tokens not validated** - Added validation and error handling
7. ✅ **No logging for debugging** - Added comprehensive console logs

---

## 🚀 TESTING CHECKLIST

### **Normal Login**
- [ ] Login with valid credentials
- [ ] Login with invalid credentials (should show error)
- [ ] Login with empty token response (should show error)
- [ ] Token should be saved to localStorage
- [ ] Cart should refresh after login
- [ ] Header should update with user info
- [ ] Check console logs for flow

### **OAuth Login**
- [ ] Click "Login with Google"
- [ ] Complete Google authentication
- [ ] Should redirect to /user-profile
- [ ] Token should be saved
- [ ] Check console logs for OAuth flow

### **Register**
- [ ] Register with valid data
- [ ] Register with mismatched passwords (should show error)
- [ ] Register without accepting terms (should show error)
- [ ] Should redirect to login after success

### **Profile**
- [ ] Load profile page (should fetch from API)
- [ ] Edit and save profile
- [ ] Change password
- [ ] Check validation errors
- [ ] Check console logs

### **Logout**
- [ ] Logout from header menu
- [ ] Logout from profile page
- [ ] Tokens should be cleared
- [ ] Header should update (show login button)
- [ ] Should redirect to login

### **Token Expiration**
- [ ] Use expired token
- [ ] Should redirect to login
- [ ] Token should be cleared

---

## 📊 FILES MODIFIED

1. ✅ `token.service.ts` - Fixed token validation logic
2. ✅ `login.component.ts` - Added validation and detailed logging
3. ✅ `register.component.ts` - Added validation and logging
4. ✅ `user.profile.component.ts` - Added comprehensive logging
5. ✅ `auth-callback.component.ts` - Added OAuth token validation
6. ✅ `header.component.ts` - Added error handling for invalid tokens
7. ✅ `token.interceptor.ts` - Added logging for debugging

---

## 🎯 BEST PRACTICES IMPLEMENTED

1. ✅ **Never store sensitive user data in localStorage** (only tokens)
2. ✅ **Always validate API responses** before using data
3. ✅ **Clear invalid tokens** on authentication errors
4. ✅ **Use reactive state management** (AuthStateService)
5. ✅ **Comprehensive error handling** with user-friendly messages
6. ✅ **Detailed console logging** for debugging
7. ✅ **Token validation** before operations
8. ✅ **Try-catch blocks** around risky operations

---

## 📖 USAGE GUIDE

### **For Developers**
1. Open browser DevTools (F12)
2. Go to Console tab
3. Watch for emoji logs during authentication flow
4. Check localStorage for tokens (not user data)
5. Use network tab to see API calls with tokens

### **For Testing**
1. Clear localStorage before testing
2. Follow console logs to track flow
3. Check for errors in console
4. Verify tokens are saved/cleared correctly
5. Test all flows (login, register, profile, logout)

---

**Last Updated**: October 18, 2025  
**Author**: AI Assistant  
**Version**: 2.0 (After Bug Fixes)
