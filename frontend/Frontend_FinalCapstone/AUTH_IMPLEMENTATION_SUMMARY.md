# ✅ Auth Implementation Summary - Token Refresh với HttpOnly Cookie

## 📋 Tổng quan

Đã implement **Auto Refresh Token** mechanism theo đúng backend specification:
- **Access Token**: Lưu trong `localStorage` (12 giờ)
- **Refresh Token**: Lưu trong **HttpOnly Cookie** `refresh_token` (30 ngày)
- **Auto Refresh**: Tự động refresh khi 401, user không bị logout đột ngột

---

## ✅ Các file đã được cập nhật

### 1. **TokenInterceptor** (`src/app/interceptors/token.interceptor.ts`)

#### Thay đổi chính:
✅ **Luôn thêm `withCredentials: true`** vào mọi request (bắt buộc để gửi/nhận cookies)  
✅ **Tự động refresh token** khi gặp 401 Unauthorized  
✅ **Xử lý race condition** - chỉ 1 request refresh, các request khác đợi  
✅ **Skip auto-refresh** cho các endpoint auth (`/auth/login`, `/auth/signup`, `/auth/refresh`)  
✅ **Sử dụng Angular Router** thay vì `window.location.href` để navigate  
✅ **Clear user data** khi refresh token hết hạn  

#### Flow hoạt động:
```
1. Request API → 401 Unauthorized
2. Interceptor kiểm tra: Có đang refresh không?
3. Nếu CHƯA refresh:
   - Đánh dấu đang refresh
   - Gọi /auth/refresh (backend đọc refresh_token từ cookie)
   - Backend trả về access token mới
   - Lưu access token mới vào localStorage
   - Retry request ban đầu với token mới
4. Nếu ĐÃ có request khác đang refresh:
   - Đợi request đó hoàn thành
   - Lấy token mới
   - Retry request
5. Nếu refresh THẤT BẠI:
   - Clear tokens
   - Navigate to /login với queryParams sessionExpired
```

#### Key Features:
```typescript
// ✅ Luôn thêm withCredentials cho mọi request
if (token) {
    authReq = req.clone({
        setHeaders: { Authorization: `Bearer ${token}` },
        withCredentials: true // 🔥 CRITICAL
    });
} else {
    authReq = req.clone({
        withCredentials: true // 🔥 Even for login/signup
    });
}

// ✅ Skip auto-refresh cho auth endpoints
private shouldSkipRefresh(url: string): boolean {
    return url.includes('/auth/login') || 
           url.includes('/auth/signup') ||
           url.includes('/auth/refresh');
}

// ✅ Race condition prevention
if (!this.isRefreshing) {
    this.isRefreshing = true;
    // ... perform refresh
} else {
    // Wait for ongoing refresh to complete
    return this.refreshTokenSubject.pipe(...);
}
```

---

### 2. **AuthStateService** (`src/app/services/auth-state.service.ts`)

#### Thay đổi chính:
✅ Thêm `isRefreshingSubject` để track refresh state  
✅ Thêm `refreshTokenSubject` để share token mới giữa các requests  
✅ Thêm các methods: `setRefreshing()`, `isRefreshing()`, `notifyTokenRefreshed()`  
✅ Clear refresh state khi logout  

#### Sử dụng:
```typescript
// Check if user is logged in
authStateService.isLoggedIn()

// Subscribe to login state changes
authStateService.loginState$.subscribe(isLoggedIn => {
    console.log('Login state:', isLoggedIn);
});

// Notify login/logout
authStateService.notifyLogin();
authStateService.notifyLogout();

// Track refresh state
authStateService.isRefreshing$.subscribe(isRefreshing => {
    // Show loading spinner if needed
});
```

---

## 🔧 Các file ĐÚNG và KHÔNG CẦN thay đổi

### ✅ UserService (`src/app/services/user.service.ts`)
- ✅ `login()` đã có `withCredentials: true`
- ✅ `refreshToken()` đã có `withCredentials: true`
- ✅ `logout()` đã có `withCredentials: true`
- ✅ Đã có methods `saveUserResponseToLocalStorage()`, `removeUserFromLocalStorage()`

### ✅ TokenService (`src/app/services/token.service.ts`)
- ✅ `getToken()`, `setToken()`, `removeToken()` hoạt động đúng
- ✅ Lưu access token vào `localStorage` với key `access_token`
- ✅ Comment rõ ràng: "Refresh token is now stored in HttpOnly cookie by backend"

### ✅ LoginComponent (`src/app/components/login/login.component.ts`)
- ✅ Gọi `userService.login()` đúng cách
- ✅ Lưu token vào localStorage sau khi login thành công
- ✅ Gọi `authStateService.notifyLogin()` để notify login state
- ✅ Refresh cart sau khi login
- ✅ Navigate based on role (ADMIN → /admin, USER → /)

### ✅ HeaderComponent (`src/app/components/header/header.component.ts`)
- ✅ Logout gọi `userService.logout()` để clear backend cookie
- ✅ Clear local tokens: `tokenService.removeToken()`
- ✅ Clear user data: `userService.removeUserFromLocalStorage()`
- ✅ Notify logout: `authStateService.notifyLogout()`
- ✅ Navigate to `/login`

### ✅ AppModule (`src/app/app.module.ts`)
- ✅ TokenInterceptor đã được register với `HTTP_INTERCEPTORS`
- ✅ `multi: true` để cho phép multiple interceptors

---

## 🎯 Testing Instructions

### Test 1: Login thành công
```typescript
// Expected behavior:
1. User nhập username/password → click Login
2. POST /auth/login được gọi với withCredentials: true
3. Backend trả về:
   - Response body: { token: "...", username: "...", roles: [...] }
   - Set-Cookie header: refresh_token=...; HttpOnly; Secure; SameSite=Strict
4. Frontend lưu access_token vào localStorage
5. Frontend lưu user info vào localStorage
6. authStateService.notifyLogin() được gọi
7. Navigate to home (/) hoặc admin (/admin)

✅ Kiểm tra:
- localStorage['access_token'] có giá trị
- localStorage['user'] có user info
- Browser DevTools → Application → Cookies có refresh_token
- Console log: "🔐 Auth State: User logged in"
```

### Test 2: API call với access token hợp lệ
```typescript
// Example: Get user profile
this.http.get('http://localhost:8080/users/me').subscribe(...)

// Expected behavior:
1. Interceptor thêm Authorization: Bearer <token>
2. Interceptor thêm withCredentials: true
3. Request thành công → 200 OK

✅ Kiểm tra Network tab:
Request Headers:
- Authorization: Bearer eyJhbGc...
- Cookie: refresh_token=...
```

### Test 3: Access token hết hạn - Auto refresh
```typescript
// Scenario: Access token expired, refresh token still valid
// Expected behavior:
1. API call → Backend trả về 401 Unauthorized
2. Interceptor catch 401 error
3. Interceptor gọi POST /auth/refresh với withCredentials: true
4. Backend đọc refresh_token từ cookie
5. Backend validate refresh token
6. Backend trả về access token mới
7. Interceptor lưu token mới vào localStorage
8. Interceptor retry request ban đầu với token mới
9. Request thành công → 200 OK

✅ User KHÔNG bị logout, KHÔNG cần làm gì
✅ Console logs:
- "⚠️ 401 Unauthorized - Attempting token refresh..."
- "🔄 Refreshing access token..."
- "✅ Token refreshed successfully"
- Original request succeeds
```

### Test 4: Refresh token hết hạn
```typescript
// Scenario: Both access token and refresh token expired
// Expected behavior:
1. API call → 401 Unauthorized
2. Interceptor gọi /auth/refresh
3. Backend trả về 401 (refresh token hết hạn)
4. Interceptor clear localStorage
5. Interceptor navigate to /login?sessionExpired=true

✅ User bị logout
✅ Navigate to login page
✅ localStorage cleared
✅ authStateService.notifyLogout() called
```

### Test 5: Multiple concurrent requests với 401
```typescript
// Scenario: 5 API calls cùng lúc, tất cả 401
// Expected behavior:
1. Request 1 → 401 → Trigger refresh (isRefreshing = true)
2. Request 2 → 401 → Wait for refresh
3. Request 3 → 401 → Wait for refresh
4. Request 4 → 401 → Wait for refresh
5. Request 5 → 401 → Wait for refresh
6. Refresh completes → new token received
7. All 5 requests retry với token mới
8. All 5 requests succeed

✅ Chỉ 1 request /auth/refresh (không phải 5 requests)
✅ Console log: "⏳ Waiting for token refresh to complete..."
```

### Test 6: Logout
```typescript
// Expected behavior:
1. User click Logout
2. POST /auth/logout với withCredentials: true
3. Backend clear refresh_token cookie
4. Frontend clear localStorage
5. authStateService.notifyLogout()
6. Navigate to /login

✅ localStorage['access_token'] = null
✅ localStorage['user'] = null
✅ Browser cookies: refresh_token cleared
✅ Console: "🔐 Auth State: User logged out"
```

---

## 🔒 Security Features (Implemented)

✅ **HttpOnly Cookie for Refresh Token**  
→ Không thể bị XSS attacks đánh cắp  
→ Backend quản lý, frontend không access được  

✅ **Access Token in localStorage**  
→ Ngắn hạn (12 giờ), ít rủi ro  
→ Dễ dàng gửi qua Authorization header  

✅ **Token Rotation**  
→ Mỗi lần refresh, backend issue refresh token mới  
→ Old refresh token bị revoke  

✅ **Automatic Session Management**  
→ User không bị logout đột ngột  
→ Seamless UX khi access token hết hạn  

✅ **CORS Strict Origin**  
→ Backend chỉ allow `http://localhost:4200`  
→ `allowCredentials: true` với exact origin  

✅ **Revoke All Tokens on Logout**  
→ Clear session đúng cách  
→ Backend revoke refresh token  

---

## 📝 Best Practices đã implement

### ✅ withCredentials: true ở MỌI request
```typescript
// ✅ ĐÚNG
this.http.post(url, data, { withCredentials: true })
this.http.get(url, { withCredentials: true })

// ❌ SAI
this.http.post(url, data) // Missing withCredentials
```

### ✅ Interceptor handle tất cả 401
```typescript
// ✅ ĐÚNG - Interceptor tự động xử lý
this.http.get('/users/me').subscribe(...)
// → 401 → Auto refresh → Retry → Success

// ❌ SAI - Manual refresh trong component
this.http.get('/users/me').subscribe({
    error: (err) => {
        if (err.status === 401) {
            this.refreshToken(); // DON'T DO THIS
        }
    }
});
```

### ✅ Race condition prevention
```typescript
// ✅ ĐÚNG - Chỉ 1 refresh request
if (!this.isRefreshing) {
    this.isRefreshing = true;
    // perform refresh
} else {
    // wait for ongoing refresh
}

// ❌ SAI - Multiple refresh requests
this.userService.refreshToken().subscribe(...)
```

### ✅ Clear ALL data on logout
```typescript
// ✅ ĐÚNG
logout() {
    this.userService.logout().subscribe({ // Clear backend cookie
        complete: () => {
            this.tokenService.removeToken(); // Clear localStorage
            this.userService.removeUserFromLocalStorage(); // Clear user data
            this.authStateService.notifyLogout(); // Notify state change
            this.router.navigate(['/login']);
        }
    });
}

// ❌ SAI - Quên clear user data
logout() {
    this.tokenService.removeToken();
    this.router.navigate(['/login']);
    // Missing: backend logout call, user data cleanup, state notification
}
```

---

## 🚨 Common Mistakes to Avoid

### ❌ Quên `withCredentials: true`
```typescript
// ❌ SAI - Cookie sẽ KHÔNG được gửi
this.http.post('/auth/login', data)

// ✅ ĐÚNG
this.http.post('/auth/login', data, { withCredentials: true })
```

### ❌ Manual refresh trong component
```typescript
// ❌ SAI - Không cần manual refresh
if (error.status === 401) {
    this.userService.refreshToken().subscribe(...)
}

// ✅ ĐÚNG - Interceptor tự động xử lý
// Just make the API call, interceptor handles 401
this.http.get('/users/me').subscribe(...)
```

### ❌ Gọi `/auth/refresh` nhiều lần khi nhiều requests 401
```typescript
// ❌ SAI - Race condition
if (error.status === 401) {
    this.userService.refreshToken().subscribe(...) // Called multiple times!
}

// ✅ ĐÚNG - Interceptor có race condition prevention
// See TokenInterceptor implementation
```

### ❌ Không clear backend cookie khi logout
```typescript
// ❌ SAI - Cookie vẫn còn trên browser
logout() {
    this.tokenService.removeToken();
    this.router.navigate(['/login']);
}

// ✅ ĐÚNG - Gọi backend /auth/logout để clear cookie
logout() {
    this.userService.logout().subscribe({ // Clear backend cookie
        complete: () => {
            this.tokenService.removeToken();
            this.router.navigate(['/login']);
        }
    });
}
```

---

## 🎉 Kết luận

### ✅ Đã implement đầy đủ:
1. ✅ Auto refresh token khi 401
2. ✅ HttpOnly cookie cho refresh token (security)
3. ✅ Race condition prevention
4. ✅ Seamless UX (user không bị logout đột ngột)
5. ✅ withCredentials: true cho mọi request
6. ✅ Proper logout (clear backend + frontend)
7. ✅ Auth state management (AuthStateService)
8. ✅ Angular Router navigation (không dùng window.location)

### 🔥 Key Points:
- **User experience**: Mượt mà, không bị logout đột ngột
- **Security**: Refresh token trong HttpOnly cookie không thể bị XSS
- **Automatic**: Interceptor tự động xử lý, component không cần quan tâm
- **Production-ready**: Handle race conditions, errors, edge cases

### 📚 Tài liệu tham khảo:
- `ANGULAR_REFRESH_TOKEN_GUIDE.md` - Hướng dẫn chi tiết implementation
- `401_POSTMAN_WORKS_BROWSER_FAILS.md` - Troubleshooting CORS và 401 issues
- `BACKEND_OAUTH_GUIDE.md` - Backend OAuth configuration

---

**Date:** 2025-01-18  
**Status:** ✅ Implementation Complete  
**Framework:** Angular 17+  
**Backend:** Spring Boot with JWT + HttpOnly Cookie
