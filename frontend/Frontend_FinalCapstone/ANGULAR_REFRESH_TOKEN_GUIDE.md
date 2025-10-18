# Hướng dẫn Angular - Auto Refresh Token khi Access Token hết hạn

## 📋 Tổng quan cơ chế

### Backend đã implement:
- **Access Token**: Lưu ở localStorage/memory, gửi qua `Authorization: Bearer` header
- **Refresh Token**: Lưu trong **HttpOnly Cookie** tên `refresh_token`, tự động gửi kèm request
- **Endpoint `/auth/refresh`**: POST endpoint để lấy access token mới
- **Thời gian hết hạn**:
  - Access Token: 12 giờ (43,200,000 ms)
  - Refresh Token: 30 ngày (2,592,000,000 ms)

### Flow hoạt động:
1. User login → Backend trả về access token (trong response body) + refresh token (trong cookie)
2. Frontend lưu access token vào localStorage
3. Mỗi API call gửi access token qua `Authorization: Bearer` header
4. Khi access token hết hạn (401) → Tự động gọi `/auth/refresh`
5. Backend đọc refresh token từ cookie, validate và trả về access token mới
6. Retry request ban đầu với access token mới

---

## 🛠️ Implementation cho Angular

### 1. Tạo Auth Service (`auth.service.ts`)

```typescript
import { Injectable } from '@angular/core';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { BehaviorSubject, Observable, throwError } from 'rxjs';
import { tap, catchError, switchMap } from 'rxjs/operators';
import { Router } from '@angular/router';

export interface AuthResponse {
  message: string;
  token: string;
  tokenType: string;
  username: string;
  roles: string[];
  id: number;
}

export interface ResponseObject {
  message: string;
  data: AuthResponse;
  status: number;
}

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private readonly API_URL = 'http://localhost:8080/auth';
  private readonly ACCESS_TOKEN_KEY = 'access_token';
  
  private isRefreshing = false;
  private refreshTokenSubject: BehaviorSubject<string | null> = new BehaviorSubject<string | null>(null);

  constructor(
    private http: HttpClient,
    private router: Router
  ) {}

  /**
   * Login method
   */
  login(username: string, password: string): Observable<ResponseObject> {
    return this.http.post<ResponseObject>(`${this.API_URL}/login`, {
      username,
      password
    }, {
      withCredentials: true // Important: cho phép gửi/nhận cookies
    }).pipe(
      tap(response => {
        if (response.data?.token) {
          this.setAccessToken(response.data.token);
        }
      }),
      catchError(this.handleError)
    );
  }

  /**
   * Refresh access token using refresh token from cookie
   * Backend tự động đọc refresh_token từ HttpOnly cookie
   */
  refreshToken(): Observable<ResponseObject> {
    return this.http.post<ResponseObject>(`${this.API_URL}/refresh`, {}, {
      withCredentials: true // Important: gửi cookie refresh_token
    }).pipe(
      tap(response => {
        if (response.data?.token) {
          this.setAccessToken(response.data.token);
          this.refreshTokenSubject.next(response.data.token);
        }
      }),
      catchError(error => {
        // Nếu refresh token cũng hết hạn hoặc invalid
        this.logout();
        return throwError(() => error);
      })
    );
  }

  /**
   * Logout - revoke tokens and clear storage
   */
  logout(): Observable<any> {
    return this.http.post(`${this.API_URL}/logout`, {}, {
      withCredentials: true
    }).pipe(
      tap(() => {
        this.clearTokens();
        this.router.navigate(['/login']);
      }),
      catchError(error => {
        // Clear tokens anyway
        this.clearTokens();
        this.router.navigate(['/login']);
        return throwError(() => error);
      })
    );
  }

  /**
   * Get access token from localStorage
   */
  getAccessToken(): string | null {
    return localStorage.getItem(this.ACCESS_TOKEN_KEY);
  }

  /**
   * Save access token to localStorage
   */
  setAccessToken(token: string): void {
    localStorage.setItem(this.ACCESS_TOKEN_KEY, token);
  }

  /**
   * Clear all tokens
   */
  clearTokens(): void {
    localStorage.removeItem(this.ACCESS_TOKEN_KEY);
    this.isRefreshing = false;
    this.refreshTokenSubject.next(null);
  }

  /**
   * Check if user is authenticated
   */
  isAuthenticated(): boolean {
    return !!this.getAccessToken();
  }

  /**
   * Get refresh status
   */
  getIsRefreshing(): boolean {
    return this.isRefreshing;
  }

  /**
   * Set refresh status
   */
  setIsRefreshing(value: boolean): void {
    this.isRefreshing = value;
  }

  /**
   * Get refresh token subject
   */
  getRefreshTokenSubject(): BehaviorSubject<string | null> {
    return this.refreshTokenSubject;
  }

  private handleError(error: HttpErrorResponse) {
    let errorMessage = 'An error occurred';
    if (error.error instanceof ErrorEvent) {
      errorMessage = `Error: ${error.error.message}`;
    } else {
      errorMessage = `Error Code: ${error.status}\nMessage: ${error.message}`;
    }
    return throwError(() => new Error(errorMessage));
  }
}
```

---

### 2. Tạo HTTP Interceptor (`auth.interceptor.ts`)

```typescript
import { Injectable } from '@angular/core';
import {
  HttpRequest,
  HttpHandler,
  HttpEvent,
  HttpInterceptor,
  HttpErrorResponse
} from '@angular/common/http';
import { Observable, throwError, BehaviorSubject } from 'rxjs';
import { catchError, filter, take, switchMap } from 'rxjs/operators';
import { AuthService } from './auth.service';

@Injectable()
export class AuthInterceptor implements HttpInterceptor {
  constructor(private authService: AuthService) {}

  intercept(request: HttpRequest<any>, next: HttpHandler): Observable<HttpEvent<any>> {
    // Add access token to request header
    const accessToken = this.authService.getAccessToken();
    
    if (accessToken) {
      request = this.addToken(request, accessToken);
    }

    // Always include credentials for cookie handling
    request = request.clone({
      withCredentials: true
    });

    return next.handle(request).pipe(
      catchError(error => {
        if (error instanceof HttpErrorResponse && error.status === 401) {
          return this.handle401Error(request, next);
        }
        return throwError(() => error);
      })
    );
  }

  /**
   * Add access token to request header
   */
  private addToken(request: HttpRequest<any>, token: string): HttpRequest<any> {
    return request.clone({
      setHeaders: {
        Authorization: `Bearer ${token}`
      }
    });
  }

  /**
   * Handle 401 Unauthorized error - Auto refresh token
   */
  private handle401Error(request: HttpRequest<any>, next: HttpHandler): Observable<HttpEvent<any>> {
    // Bỏ qua refresh nếu đang gọi endpoint login, signup, hoặc refresh
    if (request.url.includes('/auth/login') || 
        request.url.includes('/auth/signup') ||
        request.url.includes('/auth/refresh')) {
      return throwError(() => new Error('Authentication failed'));
    }

    if (!this.authService.getIsRefreshing()) {
      this.authService.setIsRefreshing(true);
      this.authService.getRefreshTokenSubject().next(null);

      return this.authService.refreshToken().pipe(
        switchMap((response: any) => {
          this.authService.setIsRefreshing(false);
          const newToken = response.data.token;
          this.authService.getRefreshTokenSubject().next(newToken);
          
          // Retry original request with new token
          return next.handle(this.addToken(request, newToken));
        }),
        catchError((err) => {
          this.authService.setIsRefreshing(false);
          this.authService.clearTokens();
          return throwError(() => err);
        })
      );
    } else {
      // Nếu đang refresh, đợi token mới rồi retry
      return this.authService.getRefreshTokenSubject().pipe(
        filter(token => token != null),
        take(1),
        switchMap(token => {
          return next.handle(this.addToken(request, token!));
        })
      );
    }
  }
}
```

---

### 3. Đăng ký Interceptor trong `app.module.ts` hoặc `app.config.ts`

#### Với NgModule (Angular < 17):

```typescript
import { NgModule } from '@angular/core';
import { BrowserModule } from '@angular/platform-browser';
import { HttpClientModule, HTTP_INTERCEPTORS } from '@angular/common/http';

import { AppComponent } from './app.component';
import { AuthInterceptor } from './interceptors/auth.interceptor';
import { AuthService } from './services/auth.service';

@NgModule({
  declarations: [
    AppComponent
  ],
  imports: [
    BrowserModule,
    HttpClientModule
  ],
  providers: [
    AuthService,
    {
      provide: HTTP_INTERCEPTORS,
      useClass: AuthInterceptor,
      multi: true
    }
  ],
  bootstrap: [AppComponent]
})
export class AppModule { }
```

#### Với Standalone (Angular >= 17):

```typescript
import { ApplicationConfig } from '@angular/core';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { authInterceptorFn } from './interceptors/auth.interceptor';

export const appConfig: ApplicationConfig = {
  providers: [
    provideHttpClient(
      withInterceptors([authInterceptorFn])
    )
  ]
};

// auth.interceptor.ts (functional interceptor cho Angular 17+)
import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { AuthService } from '../services/auth.service';
import { catchError, switchMap, filter, take } from 'rxjs/operators';
import { throwError } from 'rxjs';

export const authInterceptorFn: HttpInterceptorFn = (req, next) => {
  const authService = inject(AuthService);
  const accessToken = authService.getAccessToken();

  let authReq = req;
  
  if (accessToken) {
    authReq = req.clone({
      setHeaders: {
        Authorization: `Bearer ${accessToken}`
      },
      withCredentials: true
    });
  } else {
    authReq = req.clone({
      withCredentials: true
    });
  }

  return next(authReq).pipe(
    catchError((error) => {
      if (error.status === 401 && 
          !req.url.includes('/auth/login') && 
          !req.url.includes('/auth/refresh')) {
        
        if (!authService.getIsRefreshing()) {
          authService.setIsRefreshing(true);
          authService.getRefreshTokenSubject().next(null);

          return authService.refreshToken().pipe(
            switchMap((response: any) => {
              authService.setIsRefreshing(false);
              const newToken = response.data.token;
              authService.getRefreshTokenSubject().next(newToken);

              const retryReq = req.clone({
                setHeaders: {
                  Authorization: `Bearer ${newToken}`
                },
                withCredentials: true
              });
              return next(retryReq);
            }),
            catchError((err) => {
              authService.setIsRefreshing(false);
              authService.clearTokens();
              return throwError(() => err);
            })
          );
        } else {
          return authService.getRefreshTokenSubject().pipe(
            filter(token => token != null),
            take(1),
            switchMap(token => {
              const retryReq = req.clone({
                setHeaders: {
                  Authorization: `Bearer ${token}`
                },
                withCredentials: true
              });
              return next(retryReq);
            })
          );
        }
      }
      return throwError(() => error);
    })
  );
};
```

---

### 4. Example Component sử dụng

```typescript
import { Component, OnInit } from '@angular/core';
import { AuthService } from './services/auth.service';
import { HttpClient } from '@angular/common/http';

@Component({
  selector: 'app-dashboard',
  template: `
    <div>
      <h1>Dashboard</h1>
      <button (click)="loadUserProfile()">Load Profile</button>
      <button (click)="logout()">Logout</button>
      <div *ngIf="userProfile">
        <pre>{{ userProfile | json }}</pre>
      </div>
    </div>
  `
})
export class DashboardComponent implements OnInit {
  userProfile: any;

  constructor(
    private authService: AuthService,
    private http: HttpClient
  ) {}

  ngOnInit() {
    this.loadUserProfile();
  }

  loadUserProfile() {
    // Gọi API bình thường - interceptor sẽ tự động xử lý refresh nếu cần
    this.http.get('http://localhost:8080/users/me', {
      withCredentials: true // Luôn set withCredentials: true
    }).subscribe({
      next: (response) => {
        this.userProfile = response;
        console.log('Profile loaded:', response);
      },
      error: (error) => {
        console.error('Error loading profile:', error);
      }
    });
  }

  logout() {
    this.authService.logout().subscribe({
      next: () => {
        console.log('Logged out successfully');
      },
      error: (error) => {
        console.error('Logout error:', error);
      }
    });
  }
}
```

---

## 🎯 Testing Flow

### Test 1: Login và lưu token
```typescript
this.authService.login('user@example.com', 'password').subscribe({
  next: (response) => {
    console.log('Login successful, token saved');
    // Access token đã được lưu vào localStorage
    // Refresh token đã được lưu vào HttpOnly cookie (tự động)
  }
});
```

### Test 2: Call API với access token hợp lệ
```typescript
// Access token tự động được thêm vào header bởi interceptor
this.http.get('http://localhost:8080/properties').subscribe({
  next: (data) => console.log('Properties:', data)
});
```

### Test 3: Call API khi access token hết hạn
```typescript
// Khi access token hết hạn:
// 1. API trả về 401
// 2. Interceptor tự động gọi /auth/refresh
// 3. Backend đọc refresh token từ cookie
// 4. Backend trả về access token mới
// 5. Interceptor retry request ban đầu với token mới
// 6. User không bị logout, không cần làm gì cả

this.http.get('http://localhost:8080/bookings').subscribe({
  next: (data) => console.log('Bookings:', data),
  error: (error) => {
    // Chỉ vào đây nếu refresh token cũng hết hạn
    console.error('Session expired, please login again');
  }
});
```

---

## ⚠️ Lưu ý quan trọng

### 1. **withCredentials: true** - BẮT BUỘC
```typescript
// Phải set ở mọi request để gửi/nhận cookies
this.http.post(url, data, { withCredentials: true })
this.http.get(url, { withCredentials: true })
```

### 2. **CORS Configuration**
Backend đã config đúng:
- `allowCredentials: true`
- `exposedHeaders: ["Set-Cookie", "Authorization"]`
- Không dùng wildcard `*` khi `allowCredentials: true`

### 3. **Development vs Production**
```typescript
// Development (localhost)
secure: false
sameSite: "Lax"

// Production (HTTPS)
secure: true
sameSite: "Strict"
```

### 4. **Token Expiration Times**
- Access Token: 12 giờ (đủ dài để UX tốt)
- Refresh Token: 30 ngày (user chỉ cần login 1 lần/tháng)

### 5. **Race Condition Prevention**
Interceptor đã xử lý race condition khi nhiều request 401 cùng lúc:
- Chỉ 1 request refresh được thực hiện
- Các request khác đợi token mới rồi retry

---

## 🔒 Security Best Practices (Đã implement)

✅ **Refresh token trong HttpOnly cookie** - Không thể bị XSS đánh cắp  
✅ **Access token trong localStorage** - Ngắn hạn, ít rủi ro  
✅ **Token rotation** - Refresh token mới được issue mỗi lần refresh  
✅ **Revoke all tokens on logout** - Clear session đúng cách  
✅ **SameSite cookie** - Chống CSRF attacks  
✅ **CORS strict origin** - Chỉ allow specific domains  

---

## 📝 Summary

**Backend endpoints:**
- `POST /auth/login` - Login, trả về access token + set refresh token cookie
- `POST /auth/refresh` - Đọc refresh token từ cookie, trả về access token mới
- `POST /auth/logout` - Revoke tokens và clear cookie
- `GET /auth/me` - Get user info (requires valid access token)

**Frontend responsibilities:**
- Lưu access token vào localStorage
- Gửi access token qua `Authorization: Bearer` header
- **Luôn set `withCredentials: true`** để gửi/nhận cookies
- Tự động refresh khi 401 (via interceptor)
- Clear tokens khi logout

**Flow tự động:**
1. User không cần làm gì
2. Access token hết hạn → 401
3. Interceptor tự động gọi `/auth/refresh`
4. Backend validate refresh token (từ cookie)
5. Backend trả access token mới
6. Retry request ban đầu
7. Thành công ✅

---

## 🚀 Kết luận

Với implementation này:
- ✅ User experience mượt mà (không bị logout đột ngột)
- ✅ Bảo mật cao (HttpOnly cookie cho refresh token)
- ✅ Tự động xử lý expired tokens
- ✅ Không cần manual intervention
- ✅ Production-ready

**Chỉ cần nhớ**: `withCredentials: true` ở mọi HTTP request! 🎯

