# 🚀 Auth Quick Reference - Developer Guide

## 📌 TL;DR - Những điều cần nhớ

### 1. **withCredentials: true** - BẮT BUỘC cho mọi HTTP request
```typescript
// ✅ ĐÚNG
this.http.post(url, data, { withCredentials: true })
this.http.get(url, { withCredentials: true })

// ❌ SAI - Cookie sẽ KHÔNG được gửi
this.http.post(url, data)
```

### 2. **Interceptor tự động xử lý 401** - Không cần manual refresh
```typescript
// ✅ ĐÚNG - Just call API, interceptor handles everything
this.userService.getMyProfile().subscribe(data => {
    // Works even if access token expired
    // Interceptor auto-refreshes and retries
});

// ❌ SAI - Không cần manual refresh
this.userService.getMyProfile().subscribe({
    error: (err) => {
        if (err.status === 401) {
            this.userService.refreshToken().subscribe(...) // DON'T DO THIS
        }
    }
});
```

### 3. **Logout phải gọi backend** - Clear HttpOnly cookie
```typescript
// ✅ ĐÚNG
logout() {
    this.userService.logout().subscribe({
        complete: () => {
            this.tokenService.removeToken();
            this.userService.removeUserFromLocalStorage();
            this.authStateService.notifyLogout();
            this.router.navigate(['/login']);
        }
    });
}

// ❌ SAI - Quên clear backend cookie
logout() {
    this.tokenService.removeToken();
    this.router.navigate(['/login']);
}
```

---

## 🔑 Token Management

### Access Token (localStorage)
```typescript
// Get token
const token = this.tokenService.getToken();

// Set token (done automatically after login/refresh)
this.tokenService.setToken(token);

// Remove token (done automatically on logout)
this.tokenService.removeToken();

// Check if expired
const isExpired = this.tokenService.isTokenExpired();
```

### Refresh Token (HttpOnly Cookie)
```typescript
// Backend tự động quản lý
// Frontend KHÔNG access trực tiếp
// Cookie name: 'refresh_token'
// Expiry: 30 days

// Refresh token automatically (via interceptor)
// Manual refresh (if needed):
this.userService.refreshToken().subscribe(response => {
    const newToken = response.data.token;
    this.tokenService.setToken(newToken);
});
```

---

## 🔐 Authentication Flow

### Login
```typescript
login() {
    const loginDTO: LoginDTO = {
        usernameOrEmail: this.username,
        password: this.password
    };
    
    this.userService.login(loginDTO).subscribe({
        next: (response: LoginResponse) => {
            const token = response.data.token;
            
            // Save access token
            this.tokenService.setToken(token);
            
            // Notify login state
            this.authStateService.notifyLogin();
            
            // Navigate based on role
            const isAdmin = response.data.roles.includes('ROLE_ADMIN');
            this.router.navigate(isAdmin ? ['/admin'] : ['/']);
        },
        error: (error) => {
            console.error('Login failed:', error);
            alert(error?.error?.message || 'Invalid credentials');
        }
    });
}
```

### Logout
```typescript
logout() {
    this.userService.logout().subscribe({
        next: () => console.log('Logged out successfully'),
        error: (error) => console.error('Logout error:', error),
        complete: () => {
            // Clear everything
            this.tokenService.removeToken();
            this.userService.removeUserFromLocalStorage();
            this.authStateService.notifyLogout();
            this.router.navigate(['/login']);
        }
    });
}
```

### Check Login Status
```typescript
// Method 1: Check token existence
const isLoggedIn = this.tokenService.getToken() !== null;

// Method 2: Check token expiry
const isValid = !this.tokenService.isTokenExpired();

// Method 3: Subscribe to auth state
this.authStateService.loginState$.subscribe(isLoggedIn => {
    console.log('Login state:', isLoggedIn);
});
```

---

## 🛡️ API Calls

### Protected Endpoints (Require Authentication)
```typescript
// Interceptor automatically adds Authorization header
// No need to manually add token!

// Example: Get user profile
getUserProfile() {
    this.http.get('http://localhost:8080/users/me').subscribe(
        data => console.log('Profile:', data),
        error => console.error('Error:', error)
    );
}

// Example: Update user profile
updateProfile(data: UpdateUserDTO) {
    this.http.put('http://localhost:8080/users/me', data).subscribe(
        response => console.log('Updated:', response),
        error => console.error('Error:', error)
    );
}

// Example: Get bookings
getBookings() {
    this.http.get('http://localhost:8080/bookings').subscribe(
        data => console.log('Bookings:', data),
        error => console.error('Error:', error)
    );
}
```

### Public Endpoints (No Authentication)
```typescript
// These work without login
// Interceptor still adds withCredentials: true

// Example: Get categories
getCategories() {
    this.http.get('http://localhost:8080/api/categories').subscribe(
        data => console.log('Categories:', data)
    );
}

// Example: Get properties
getProperties() {
    this.http.get('http://localhost:8080/properties').subscribe(
        data => console.log('Properties:', data)
    );
}
```

---

## 🎯 Common Scenarios

### Scenario 1: User opens app
```typescript
ngOnInit() {
    // Check if user is logged in
    const token = this.tokenService.getToken();
    
    if (token && !this.tokenService.isTokenExpired()) {
        // User is logged in, load user data
        this.loadUserProfile();
    } else {
        // Not logged in or token expired
        this.router.navigate(['/login']);
    }
}
```

### Scenario 2: Access token expires while using app
```
1. User is browsing properties
2. Access token expires (after 12 hours)
3. User clicks on a property to view details
4. API call returns 401 Unauthorized
5. Interceptor catches 401
6. Interceptor calls /auth/refresh automatically
7. Backend validates refresh_token from cookie
8. Backend returns new access token
9. Interceptor saves new token
10. Interceptor retries original API call
11. Property details display successfully
✅ User doesn't notice anything!
```

### Scenario 3: Refresh token expires
```
1. User hasn't used app for 30 days
2. Refresh token expires
3. User opens app and tries to view profile
4. API call returns 401
5. Interceptor calls /auth/refresh
6. Backend returns 401 (refresh token expired)
7. Interceptor redirects to /login?sessionExpired=true
8. User sees "Session expired, please login again"
✅ User needs to login again
```

### Scenario 4: Multiple tabs open
```
1. User has 2 tabs open
2. User logs out in Tab 1
3. Backend clears refresh_token cookie
4. Tab 2 tries to make API call
5. Access token might still be valid temporarily
6. When access token expires in Tab 2:
7. Interceptor calls /auth/refresh
8. No refresh_token cookie → 401
9. Tab 2 redirects to login
✅ Both tabs stay in sync via cookie
```

---

## 🧩 Integration with Components

### Login Component
```typescript
import { UserService } from '../../services/user.service';
import { TokenService } from '../../services/token.service';
import { AuthStateService } from '../../services/auth-state.service';

export class LoginComponent {
    constructor(
        private userService: UserService,
        private tokenService: TokenService,
        private authStateService: AuthStateService,
        private router: Router
    ) {}
    
    login() {
        this.userService.login(this.loginDTO).subscribe({
            next: (response) => {
                this.tokenService.setToken(response.data.token);
                this.authStateService.notifyLogin();
                this.router.navigate(['/']);
            }
        });
    }
}
```

### Header Component
```typescript
import { AuthStateService } from '../../services/auth-state.service';
import { UserService } from '../../services/user.service';
import { TokenService } from '../../services/token.service';

export class HeaderComponent implements OnInit {
    isLoggedIn = false;
    
    constructor(
        private authStateService: AuthStateService,
        private userService: UserService,
        private tokenService: TokenService,
        private router: Router
    ) {}
    
    ngOnInit() {
        // Subscribe to login state changes
        this.authStateService.loginState$.subscribe(isLoggedIn => {
            this.isLoggedIn = isLoggedIn;
        });
        
        // Initialize login state
        this.isLoggedIn = !!this.tokenService.getToken();
    }
    
    logout() {
        this.userService.logout().subscribe({
            complete: () => {
                this.tokenService.removeToken();
                this.userService.removeUserFromLocalStorage();
                this.authStateService.notifyLogout();
                this.router.navigate(['/login']);
            }
        });
    }
}
```

### User Profile Component
```typescript
import { TokenService } from '../../services/token.service';
import { UserService } from '../../services/user.service';

export class UserProfileComponent implements OnInit {
    userProfile: any;
    
    constructor(
        private userService: UserService,
        private tokenService: TokenService
    ) {}
    
    ngOnInit() {
        this.loadProfile();
    }
    
    loadProfile() {
        // Interceptor automatically adds token
        // No need to pass token manually!
        this.http.get('http://localhost:8080/users/me').subscribe({
            next: (profile) => {
                this.userProfile = profile;
            },
            error: (error) => {
                // If 401, interceptor handles auto-refresh
                // If refresh fails, user redirected to login
                console.error('Error loading profile:', error);
            }
        });
    }
}
```

---

## 🔧 Services Quick Reference

### TokenService
```typescript
// Get token from localStorage
getToken(): string

// Save token to localStorage
setToken(token: string): void

// Remove token from localStorage
removeToken(): void

// Get user ID from token
getUserId(): number

// Check if token is expired
isTokenExpired(): boolean
```

### UserService
```typescript
// Login - returns access token in response, sets refresh token cookie
login(loginDTO: LoginDTO): Observable<LoginResponse>

// Refresh - uses refresh_token cookie, returns new access token
refreshToken(): Observable<any>

// Logout - clears refresh_token cookie on backend
logout(): Observable<any>

// Get user profile
getMyProfile(token: string): Observable<any>

// Update user profile
updateMyProfile(token: string, data: UpdateUserDTO): Observable<any>

// Save user to localStorage
saveUserResponseToLocalStorage(user: UserResponse): void

// Get user from localStorage
getUserResponseFromLocalStorage(): UserResponse | null

// Remove user from localStorage
removeUserFromLocalStorage(): void
```

### AuthStateService
```typescript
// Notify login
notifyLogin(): void

// Notify logout
notifyLogout(): void

// Check if logged in
isLoggedIn(): boolean

// Subscribe to login state changes
loginState$: Observable<boolean>

// Set refresh state
setRefreshing(isRefreshing: boolean): void

// Check if refreshing
isRefreshing(): boolean

// Notify token refreshed
notifyTokenRefreshed(token: string | null): void
```

---

## ⚠️ Important Notes

### 1. Never store sensitive data in localStorage
```typescript
// ✅ OK to store in localStorage:
- Access token (short-lived, 12 hours)
- User ID
- User preferences (theme, language)

// ❌ NEVER store in localStorage:
- Refresh token (use HttpOnly cookie)
- Passwords
- Credit card info
- Social security numbers
```

### 2. Always use HTTPS in production
```typescript
// Development (localhost)
http://localhost:4200 ← OK
http://localhost:8080 ← OK

// Production
https://yourdomain.com ← REQUIRED
https://api.yourdomain.com ← REQUIRED
```

### 3. Token expiry times
```typescript
// Access Token: 12 hours (43,200,000 ms)
// - Long enough for good UX
// - Short enough for security

// Refresh Token: 30 days (2,592,000,000 ms)
// - User only needs to login once per month
// - Stored in HttpOnly cookie (secure)
```

### 4. CORS Configuration
```typescript
// Backend must allow:
allowedOrigins: ["http://localhost:4200"] // NOT "*"
allowCredentials: true // Required for cookies
allowedMethods: ["GET", "POST", "PUT", "DELETE", "OPTIONS"]
allowedHeaders: ["*"]
exposedHeaders: ["Set-Cookie", "Authorization"]
```

---

## 🐛 Debugging Tips

### Check if token exists
```typescript
console.log('Token:', localStorage.getItem('access_token'));
console.log('User:', localStorage.getItem('user'));
```

### Check if refresh token cookie exists
```
1. Open DevTools (F12)
2. Application tab
3. Cookies
4. http://localhost:4200
5. Look for 'refresh_token' cookie
```

### Check request headers
```
1. Open DevTools (F12)
2. Network tab
3. Click on any API request
4. Headers tab
5. Check:
   - Authorization: Bearer ...
   - Cookie: refresh_token=...
```

### Check interceptor is working
```typescript
// In TokenInterceptor, add console.log:
intercept(req: HttpRequest<any>, next: HttpHandler): Observable<HttpEvent<any>> {
    console.log('🔍 Interceptor:', req.url);
    console.log('🔍 Token:', this.tokenService.getToken());
    // ...
}
```

### Check auth state
```typescript
// In component:
this.authStateService.loginState$.subscribe(state => {
    console.log('🔐 Login state:', state);
});

this.authStateService.isRefreshing$.subscribe(state => {
    console.log('🔄 Refreshing:', state);
});
```

---

## 📚 Related Files

- `AUTH_IMPLEMENTATION_SUMMARY.md` - Detailed implementation guide
- `AUTH_TESTING_CHECKLIST.md` - Complete testing guide
- `ANGULAR_REFRESH_TOKEN_GUIDE.md` - Original design document
- `401_POSTMAN_WORKS_BROWSER_FAILS.md` - CORS troubleshooting
- `src/app/interceptors/token.interceptor.ts` - Auto-refresh logic
- `src/app/services/auth-state.service.ts` - Auth state management
- `src/app/services/token.service.ts` - Token storage
- `src/app/services/user.service.ts` - Auth API calls

---

**Last Updated:** 2025-01-18  
**Version:** 1.0  
**Status:** Production Ready ✅
