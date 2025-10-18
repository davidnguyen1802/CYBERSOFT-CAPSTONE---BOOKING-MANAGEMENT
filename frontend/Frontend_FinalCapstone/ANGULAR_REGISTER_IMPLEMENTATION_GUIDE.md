# Hướng dẫn Frontend Angular: Triển khai Đăng ký (Register)

## 📋 Mục lục
1. [Backend API Specification](#backend-api-specification)
2. [Request Format từ Frontend](#request-format)
3. [Response Format từ Backend](#response-format)
4. [Implementation Guide](#implementation-guide)
5. [Checklist kiểm tra code](#checklist)
6. [Error Handling](#error-handling)
7. [Testing](#testing)

---

## 1. Backend API Specification

### Endpoint
```
POST http://localhost:8080/auth/signup
```

### Headers
```http
Content-Type: multipart/form-data
```

### CORS Requirements
- Backend đã cấu hình CORS cho phép:
  - Origins: `http://localhost:4200`, `http://127.0.0.1:4200`, etc.
  - Credentials: **true** (bắt buộc vì có cookie)
  - Exposed headers: `Set-Cookie`, `Authorization`

---

## 2. Request Format từ Frontend

### 2.1. Form Data Fields

Frontend **BẮT BUỘC** gửi multipart/form-data với các field sau:

#### **Required Fields (Bắt buộc):**

| Field Name | Type | Validation | Example |
|-----------|------|------------|---------|
| `fullName` | string | 2-100 ký tự | "Nguyễn Văn A" |
| `username` | string | 3-50 ký tự, chỉ a-z, 0-9, .-_ | "nguyenvana" |
| `email` | string | Email hợp lệ | "nguyenvana@example.com" |
| `password` | string | Tối thiểu 6 ký tự | "123456" |

#### **Optional Fields (Tuỳ chọn):**

| Field Name | Type | Format | Example |
|-----------|------|--------|---------|
| `phone` | string | Số điện thoại | "0909123456" |
| `address` | string | Địa chỉ | "123 ABC Street, District 1" |
| `gender` | enum | "MALE" \| "FEMALE" \| "NONE" | "MALE" |
| `dob` | string | ISO Date "YYYY-MM-DD" | "1990-01-15" |
| `avatar` | File | Image file (jpg, png, etc.) | avatar.jpg |

#### **Social Login Fields (Tuỳ chọn - cho OAuth):**

| Field Name | Type | Example |
|-----------|------|---------|
| `facebookAccountId` | string | "1234567890" |
| `googleAccountId` | string | "google_abc123" |

### 2.2. Angular FormData Construction

```typescript
// Ví dụ xây dựng FormData từ Angular
const formData = new FormData();

// Required fields
formData.append('fullName', this.registerForm.value.fullName);
formData.append('username', this.registerForm.value.username);
formData.append('email', this.registerForm.value.email);
formData.append('password', this.registerForm.value.password);

// Optional fields - chỉ append nếu có giá trị
if (this.registerForm.value.phone) {
  formData.append('phone', this.registerForm.value.phone);
}

if (this.registerForm.value.address) {
  formData.append('address', this.registerForm.value.address);
}

if (this.registerForm.value.gender) {
  formData.append('gender', this.registerForm.value.gender); // "MALE" | "FEMALE" | "NONE"
}

if (this.registerForm.value.dob) {
  // Convert Date object to YYYY-MM-DD string
  const dobString = this.formatDateToISO(this.registerForm.value.dob);
  formData.append('dob', dobString);
}

// Avatar file
if (this.selectedAvatarFile) {
  formData.append('avatar', this.selectedAvatarFile, this.selectedAvatarFile.name);
}
```

### 2.3. Date Format Helper

```typescript
// Helper method để format Date thành YYYY-MM-DD
formatDateToISO(date: Date | string): string {
  if (!date) return '';
  
  const d = new Date(date);
  const year = d.getFullYear();
  const month = String(d.getMonth() + 1).padStart(2, '0');
  const day = String(d.getDate()).padStart(2, '0');
  
  return `${year}-${month}-${day}`;
}
```

---

## 3. Response Format từ Backend

### 3.1. Success Response (HTTP 200)

```typescript
interface ResponseObject<T> {
  message: string;
  data: T;
  status: string; // "OK"
}

interface AuthResponse {
  message: string;
  token: string;           // Access token JWT
  tokenType: string;       // "Bearer"
  username: string;
  roles: string[];         // ["ROLE_GUEST"]
  id: number;
  refresh_token?: null;    // Luôn null trong body
}
```

**Example Success Response:**
```json
{
  "message": "Sign up successfully",
  "data": {
    "message": "Sign up Successfully.",
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJ1c2VySWQiOjEyMywic3ViIjoibmd1eWVudmFuYUBleGFtcGxlLmNvbSIsImlzcyI6Im15YXBwIiwiaWF0IjoxNzI5MjQ4MDAwLCJleHAiOjE3MjkyNDg5MDB9.xyz",
    "tokenType": "Bearer",
    "username": "nguyenvana",
    "roles": ["ROLE_GUEST"],
    "id": 123
  },
  "status": "OK"
}
```

**Set-Cookie Header:**
```http
Set-Cookie: refresh_token=eyJhbGc...; Path=/; HttpOnly; SameSite=Lax; Max-Age=1209600
```

⚠️ **LƯU Ý QUAN TRỌNG:**
- **Access token** trả trong response body → Frontend lưu localStorage/memory
- **Refresh token** KHÔNG trả trong body, chỉ có trong HttpOnly cookie
- Frontend **KHÔNG THỂ** đọc refresh token từ JavaScript (bảo mật)
- Browser tự động gửi cookie khi gọi `/auth/refresh` (nếu `withCredentials: true`)

### 3.2. Error Response (HTTP 400/401/500)

```typescript
interface ErrorResponse {
  message: string;
  data?: any;
  status: string; // "BAD_REQUEST" | "UNAUTHORIZED" | etc.
}
```

**Example Error Responses:**

**Username đã tồn tại:**
```json
{
  "message": "Username already exists",
  "status": "BAD_REQUEST"
}
```

**Email đã tồn tại:**
```json
{
  "message": "Your email is existed",
  "status": "BAD_REQUEST"
}
```

**Validation Error:**
```json
{
  "message": "Password must be at least 6 characters",
  "status": "BAD_REQUEST"
}
```

---

## 4. Implementation Guide

### 4.1. Angular Models (TypeScript Interfaces)

Tạo file: `src/app/models/auth.model.ts`

```typescript
export interface SignUpRequest {
  fullName: string;
  username: string;
  email: string;
  password: string;
  phone?: string;
  address?: string;
  gender?: 'MALE' | 'FEMALE' | 'NONE';
  dob?: string; // YYYY-MM-DD
  facebookAccountId?: string;
  googleAccountId?: string;
}

export interface AuthResponse {
  message: string;
  token: string;
  tokenType: string;
  username: string;
  roles: string[];
  id: number;
  refresh_token?: null;
}

export interface ResponseObject<T = any> {
  message: string;
  data: T;
  status: string;
}
```

### 4.2. Auth Service

Tạo file: `src/app/services/auth.service.ts`

```typescript
import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { map, tap } from 'rxjs/operators';
import { AuthResponse, ResponseObject, SignUpRequest } from '../models/auth.model';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly API_URL = 'http://localhost:8080/auth';
  private readonly ACCESS_TOKEN_KEY = 'access_token';

  constructor(private http: HttpClient) {}

  /**
   * Đăng ký user mới
   * @param formData FormData chứa thông tin đăng ký + avatar (nếu có)
   * @returns Observable<AuthResponse>
   */
  register(formData: FormData): Observable<AuthResponse> {
    // ⚠️ QUAN TRỌNG: withCredentials: true để nhận cookie refresh_token
    return this.http.post<ResponseObject<AuthResponse>>(
      `${this.API_URL}/signup`,
      formData,
      { 
        withCredentials: true,
        // ❌ KHÔNG set Content-Type header (browser tự động set cho multipart/form-data)
      }
    ).pipe(
      map(response => response.data),
      tap(authResponse => {
        // Lưu access token vào localStorage
        if (authResponse.token) {
          this.setAccessToken(authResponse.token);
        }
      })
    );
  }

  /**
   * Lưu access token
   */
  setAccessToken(token: string): void {
    localStorage.setItem(this.ACCESS_TOKEN_KEY, token);
  }

  /**
   * Lấy access token
   */
  getAccessToken(): string | null {
    return localStorage.getItem(this.ACCESS_TOKEN_KEY);
  }

  /**
   * Xoá access token (logout)
   */
  clearAccessToken(): void {
    localStorage.removeItem(this.ACCESS_TOKEN_KEY);
  }

  /**
   * Kiểm tra user đã login chưa
   */
  isLoggedIn(): boolean {
    return !!this.getAccessToken();
  }

  /**
   * Refresh access token khi hết hạn
   */
  refreshToken(): Observable<AuthResponse> {
    return this.http.post<ResponseObject<AuthResponse>>(
      `${this.API_URL}/refresh`,
      {},
      { withCredentials: true } // ⚠️ Bắt buộc để gửi refresh_token cookie
    ).pipe(
      map(response => response.data),
      tap(authResponse => {
        if (authResponse.token) {
          this.setAccessToken(authResponse.token);
        }
      })
    );
  }

  /**
   * Logout
   */
  logout(): Observable<any> {
    return this.http.post(
      `${this.API_URL}/logout`,
      {},
      { withCredentials: true }
    ).pipe(
      tap(() => this.clearAccessToken())
    );
  }
}
```

### 4.3. Register Component

Tạo file: `src/app/components/register/register.component.ts`

```typescript
import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-register',
  templateUrl: './register.component.html',
  styleUrls: ['./register.component.css']
})
export class RegisterComponent implements OnInit {
  registerForm!: FormGroup;
  selectedAvatarFile: File | null = null;
  isLoading = false;
  errorMessage = '';

  constructor(
    private fb: FormBuilder,
    private authService: AuthService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.initForm();
  }

  private initForm(): void {
    this.registerForm = this.fb.group({
      fullName: ['', [Validators.required, Validators.minLength(2), Validators.maxLength(100)]],
      username: ['', [
        Validators.required,
        Validators.minLength(3),
        Validators.maxLength(50),
        Validators.pattern(/^[a-zA-Z0-9._-]+$/)
      ]],
      email: ['', [Validators.required, Validators.email]],
      password: ['', [Validators.required, Validators.minLength(6)]],
      phone: [''],
      address: [''],
      gender: ['NONE'], // Default value
      dob: ['']
    });
  }

  onAvatarSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (input.files && input.files.length > 0) {
      const file = input.files[0];
      
      // Validate file type
      if (!file.type.startsWith('image/')) {
        this.errorMessage = 'Avatar must be an image file';
        return;
      }
      
      // Validate file size (ví dụ: max 5MB)
      const maxSizeInBytes = 5 * 1024 * 1024; // 5MB
      if (file.size > maxSizeInBytes) {
        this.errorMessage = 'Avatar size must be less than 5MB';
        return;
      }
      
      this.selectedAvatarFile = file;
      this.errorMessage = '';
    }
  }

  onSubmit(): void {
    if (this.registerForm.invalid) {
      this.markFormGroupTouched(this.registerForm);
      return;
    }

    this.isLoading = true;
    this.errorMessage = '';

    // Build FormData
    const formData = new FormData();
    
    // Required fields
    formData.append('fullName', this.registerForm.value.fullName);
    formData.append('username', this.registerForm.value.username);
    formData.append('email', this.registerForm.value.email);
    formData.append('password', this.registerForm.value.password);

    // Optional fields
    if (this.registerForm.value.phone) {
      formData.append('phone', this.registerForm.value.phone);
    }
    if (this.registerForm.value.address) {
      formData.append('address', this.registerForm.value.address);
    }
    if (this.registerForm.value.gender) {
      formData.append('gender', this.registerForm.value.gender);
    }
    if (this.registerForm.value.dob) {
      const dobString = this.formatDateToISO(this.registerForm.value.dob);
      formData.append('dob', dobString);
    }

    // Avatar file
    if (this.selectedAvatarFile) {
      formData.append('avatar', this.selectedAvatarFile, this.selectedAvatarFile.name);
    }

    // Call API
    this.authService.register(formData).subscribe({
      next: (response) => {
        console.log('Registration successful:', response);
        // Redirect to home or dashboard
        this.router.navigate(['/']);
      },
      error: (error) => {
        console.error('Registration error:', error);
        this.errorMessage = error.error?.message || 'Registration failed. Please try again.';
        this.isLoading = false;
      },
      complete: () => {
        this.isLoading = false;
      }
    });
  }

  private formatDateToISO(date: Date | string): string {
    if (!date) return '';
    const d = new Date(date);
    const year = d.getFullYear();
    const month = String(d.getMonth() + 1).padStart(2, '0');
    const day = String(d.getDate()).padStart(2, '0');
    return `${year}-${month}-${day}`;
  }

  private markFormGroupTouched(formGroup: FormGroup): void {
    Object.keys(formGroup.controls).forEach(key => {
      const control = formGroup.get(key);
      control?.markAsTouched();
      if (control instanceof FormGroup) {
        this.markFormGroupTouched(control);
      }
    });
  }
}
```

### 4.4. Register Template

Tạo file: `src/app/components/register/register.component.html`

```html
<div class="register-container">
  <h2>Đăng ký tài khoản</h2>

  <form [formGroup]="registerForm" (ngSubmit)="onSubmit()">
    <!-- Full Name -->
    <div class="form-group">
      <label for="fullName">Họ và tên *</label>
      <input 
        type="text" 
        id="fullName" 
        formControlName="fullName"
        placeholder="Nguyễn Văn A"
        [class.error]="registerForm.get('fullName')?.invalid && registerForm.get('fullName')?.touched"
      />
      <div class="error-message" *ngIf="registerForm.get('fullName')?.invalid && registerForm.get('fullName')?.touched">
        <span *ngIf="registerForm.get('fullName')?.errors?.['required']">Họ tên là bắt buộc</span>
        <span *ngIf="registerForm.get('fullName')?.errors?.['minlength']">Họ tên phải có ít nhất 2 ký tự</span>
      </div>
    </div>

    <!-- Username -->
    <div class="form-group">
      <label for="username">Tên đăng nhập *</label>
      <input 
        type="text" 
        id="username" 
        formControlName="username"
        placeholder="nguyenvana"
        [class.error]="registerForm.get('username')?.invalid && registerForm.get('username')?.touched"
      />
      <div class="error-message" *ngIf="registerForm.get('username')?.invalid && registerForm.get('username')?.touched">
        <span *ngIf="registerForm.get('username')?.errors?.['required']">Tên đăng nhập là bắt buộc</span>
        <span *ngIf="registerForm.get('username')?.errors?.['pattern']">Chỉ được chứa chữ, số, dấu chấm, gạch dưới và gạch ngang</span>
      </div>
    </div>

    <!-- Email -->
    <div class="form-group">
      <label for="email">Email *</label>
      <input 
        type="email" 
        id="email" 
        formControlName="email"
        placeholder="example@email.com"
        [class.error]="registerForm.get('email')?.invalid && registerForm.get('email')?.touched"
      />
      <div class="error-message" *ngIf="registerForm.get('email')?.invalid && registerForm.get('email')?.touched">
        <span *ngIf="registerForm.get('email')?.errors?.['required']">Email là bắt buộc</span>
        <span *ngIf="registerForm.get('email')?.errors?.['email']">Email không hợp lệ</span>
      </div>
    </div>

    <!-- Password -->
    <div class="form-group">
      <label for="password">Mật khẩu *</label>
      <input 
        type="password" 
        id="password" 
        formControlName="password"
        placeholder="Tối thiểu 6 ký tự"
        [class.error]="registerForm.get('password')?.invalid && registerForm.get('password')?.touched"
      />
      <div class="error-message" *ngIf="registerForm.get('password')?.invalid && registerForm.get('password')?.touched">
        <span *ngIf="registerForm.get('password')?.errors?.['required']">Mật khẩu là bắt buộc</span>
        <span *ngIf="registerForm.get('password')?.errors?.['minlength']">Mật khẩu phải có ít nhất 6 ký tự</span>
      </div>
    </div>

    <!-- Phone (Optional) -->
    <div class="form-group">
      <label for="phone">Số điện thoại</label>
      <input type="tel" id="phone" formControlName="phone" placeholder="0909123456" />
    </div>

    <!-- Address (Optional) -->
    <div class="form-group">
      <label for="address">Địa chỉ</label>
      <input type="text" id="address" formControlName="address" placeholder="123 ABC Street" />
    </div>

    <!-- Gender -->
    <div class="form-group">
      <label for="gender">Giới tính</label>
      <select id="gender" formControlName="gender">
        <option value="NONE">Không xác định</option>
        <option value="MALE">Nam</option>
        <option value="FEMALE">Nữ</option>
      </select>
    </div>

    <!-- Date of Birth -->
    <div class="form-group">
      <label for="dob">Ngày sinh</label>
      <input type="date" id="dob" formControlName="dob" />
    </div>

    <!-- Avatar Upload -->
    <div class="form-group">
      <label for="avatar">Ảnh đ���i diện</label>
      <input type="file" id="avatar" accept="image/*" (change)="onAvatarSelected($event)" />
      <small *ngIf="selectedAvatarFile">Đã chọn: {{ selectedAvatarFile.name }}</small>
    </div>

    <!-- Error Message -->
    <div class="alert alert-danger" *ngIf="errorMessage">
      {{ errorMessage }}
    </div>

    <!-- Submit Button -->
    <button type="submit" [disabled]="isLoading || registerForm.invalid">
      <span *ngIf="!isLoading">Đăng ký</span>
      <span *ngIf="isLoading">Đang xử lý...</span>
    </button>
  </form>
</div>
```

### 4.5. HTTP Interceptor (Auto-refresh khi access token hết hạn)

Tạo file: `src/app/interceptors/auth.interceptor.ts`

```typescript
import { Injectable } from '@angular/core';
import {
  HttpEvent, HttpHandler, HttpInterceptor, HttpRequest, HttpErrorResponse
} from '@angular/common/http';
import { Observable, BehaviorSubject, throwError } from 'rxjs';
import { catchError, filter, switchMap, take } from 'rxjs/operators';
import { AuthService } from '../services/auth.service';
import { Router } from '@angular/router';

@Injectable()
export class AuthInterceptor implements HttpInterceptor {
  private isRefreshing = false;
  private refreshTokenSubject = new BehaviorSubject<string | null>(null);

  constructor(
    private authService: AuthService,
    private router: Router
  ) {}

  intercept(req: HttpRequest<any>, next: HttpHandler): Observable<HttpEvent<any>> {
    // Bỏ qua các endpoint không cần token
    if (this.isPublicEndpoint(req.url)) {
      return next.handle(req.clone({ withCredentials: true }));
    }

    // Thêm access token vào header nếu có
    const token = this.authService.getAccessToken();
    let authReq = req.clone({ withCredentials: true });
    
    if (token) {
      authReq = req.clone({
        setHeaders: { Authorization: `Bearer ${token}` },
        withCredentials: true
      });
    }

    return next.handle(authReq).pipe(
      catchError(error => {
        if (error instanceof HttpErrorResponse && error.status === 401) {
          // Access token hết hạn -> thử refresh
          return this.handle401Error(authReq, next);
        }
        return throwError(() => error);
      })
    );
  }

  private isPublicEndpoint(url: string): boolean {
    const publicEndpoints = [
      '/auth/login',
      '/auth/signup',
      '/auth/refresh',
      '/auth/forgot-password'
    ];
    return publicEndpoints.some(endpoint => url.includes(endpoint));
  }

  private handle401Error(req: HttpRequest<any>, next: HttpHandler): Observable<HttpEvent<any>> {
    if (!this.isRefreshing) {
      this.isRefreshing = true;
      this.refreshTokenSubject.next(null);

      return this.authService.refreshToken().pipe(
        switchMap(authResponse => {
          this.isRefreshing = false;
          const newToken = authResponse.token;
          this.refreshTokenSubject.next(newToken);

          // Retry request với token mới
          const clonedReq = req.clone({
            setHeaders: { Authorization: `Bearer ${newToken}` },
            withCredentials: true
          });
          return next.handle(clonedReq);
        }),
        catchError(err => {
          this.isRefreshing = false;
          // Refresh token cũng fail -> redirect về login
          this.authService.clearAccessToken();
          this.router.navigate(['/login']);
          return throwError(() => err);
        })
      );
    } else {
      // Đang refresh, chờ token mới
      return this.refreshTokenSubject.pipe(
        filter(token => token !== null),
        take(1),
        switchMap(token => {
          const clonedReq = req.clone({
            setHeaders: { Authorization: `Bearer ${token}` },
            withCredentials: true
          });
          return next.handle(clonedReq);
        })
      );
    }
  }
}
```

**Đăng ký interceptor trong `app.module.ts`:**

```typescript
import { HTTP_INTERCEPTORS } from '@angular/common/http';
import { AuthInterceptor } from './interceptors/auth.interceptor';

@NgModule({
  // ...
  providers: [
    { provide: HTTP_INTERCEPTORS, useClass: AuthInterceptor, multi: true }
  ]
})
export class AppModule { }
```

---

## 5. Checklist kiểm tra code Frontend

### ✅ Trước khi gửi request:

- [ ] **FormData được tạo đúng:**
  - [ ] Các field required đã append: `fullName`, `username`, `email`, `password`
  - [ ] Các field optional chỉ append khi có giá trị (không append null/undefined)
  - [ ] Field `gender` gửi đúng enum: "MALE" | "FEMALE" | "NONE"
  - [ ] Field `dob` format đúng "YYYY-MM-DD"
  - [ ] Avatar file append với tên field chính xác: `avatar`

- [ ] **HTTP Request config đúng:**
  - [ ] `withCredentials: true` (bắt buộc để nhận cookie)
  - [ ] **KHÔNG** set header `Content-Type` thủ công (browser tự set cho multipart)
  - [ ] URL endpoint đúng: `POST /auth/signup`

- [ ] **Validation client-side:**
  - [ ] Kiểm tra required fields trước khi submit
  - [ ] Validate email format
  - [ ] Validate username pattern (chỉ a-z, 0-9, .-_)
  - [ ] Password tối thiểu 6 ký tự
  - [ ] Avatar file type (chỉ image)
  - [ ] Avatar file size (ví dụ: max 5MB)

### ✅ Khi nhận response:

- [ ] **Success (200):**
  - [ ] Lưu `response.data.token` vào localStorage/memory
  - [ ] **KHÔNG** cố đọc refresh_token từ cookie (HttpOnly)
  - [ ] Lưu thông tin user: `id`, `username`, `roles` (nếu cần)
  - [ ] Redirect user tới trang home/dashboard
  - [ ] Hiển thị thông báo thành công

- [ ] **Error (400/401/500):**
  - [ ] Parse error message từ `error.error.message`
  - [ ] Hiển thị error message cho user
  - [ ] Clear form hoặc highlight field bị lỗi
  - [ ] Handle các lỗi cụ thể:
    - Username đã tồn tại
    - Email đã tồn tại
    - Validation errors

### ✅ Cookie Handling:

- [ ] **Browser đã nhận cookie refresh_token:**
  - [ ] Mở DevTools → Application/Storage → Cookies
  - [ ] Kiểm tra có cookie `refresh_token` cho domain `localhost:8080`
  - [ ] Kiểm tra cookie có flag `HttpOnly`, `SameSite=Lax`

- [ ] **Gửi cookie trong request tiếp theo:**
  - [ ] Tất cả request có `withCredentials: true`
  - [ ] Browser tự động gửi cookie (không cần code thêm)

### ✅ Interceptor (Auto-refresh):

- [ ] **Interceptor đã được đăng ký trong providers**
- [ ] **Logic xử lý 401:**
  - [ ] Phát hiện 401 → gọi `/auth/refresh`
  - [ ] Nếu refresh thành công → retry request với token mới
  - [ ] Nếu refresh fail → redirect về login
- [ ] **Queue handling:**
  - [ ] Nhiều request 401 đồng thời chỉ gọi refresh 1 lần
  - [ ] Các request khác chờ token mới rồi retry

### ✅ Security Best Practices:

- [ ] **Access token:**
  - [ ] Lưu localStorage (hoặc memory nếu quan trọng hơn)
  - [ ] Gửi qua header `Authorization: Bearer {token}`
  - [ ] Không gửi qua URL query params

- [ ] **Refresh token:**
  - [ ] **KHÔNG BAO GIỜ** lưu vào localStorage/sessionStorage
  - [ ] Chỉ lưu trong HttpOnly cookie (backend xử lý)
  - [ ] Frontend không đọc được giá trị (bảo mật)

- [ ] **CORS:**
  - [ ] Backend allow origin chứa domain frontend
  - [ ] Backend allow credentials = true
  - [ ] Frontend gửi withCredentials = true

---

## 6. Error Handling

### Common Errors và cách xử lý:

| Error | HTTP Status | Message | Giải pháp Frontend |
|-------|-------------|---------|-------------------|
| Username exists | 400 | "Username already exists" | Hiển thị error ở field username, suggest thử username khác |
| Email exists | 400 | "Your email is existed" | Hiển thị error ở field email, suggest login |
| Validation error | 400 | Various | Parse validation errors và hiển thị từng field |
| Network error | 0 | Connection failed | Hiển thị "Không thể kết nối server. Vui lòng thử lại." |
| Server error | 500 | Internal server error | Hiển thị "Lỗi hệ thống. Vui lòng thử lại sau." |

### Error Display Example:

```typescript
// In component
handleError(error: any): void {
  if (error.status === 400) {
    // Bad request - validation or duplicate
    this.errorMessage = error.error?.message || 'Invalid input';
  } else if (error.status === 0) {
    // Network error
    this.errorMessage = 'Không thể kết nối server. Vui lòng kiểm tra kết nối.';
  } else if (error.status === 500) {
    // Server error
    this.errorMessage = 'Lỗi hệ thống. Vui lòng thử lại sau.';
  } else {
    this.errorMessage = 'Đăng ký thất bại. Vui lòng thử lại.';
  }
}
```

---

## 7. Testing

### 7.1. Manual Testing Checklist

#### Test Case 1: Đăng ký thành công (full fields + avatar)
```
Input:
- fullName: "Nguyen Van A"
- username: "nguyenvana"
- email: "nguyenvana@example.com"
- password: "123456"
- phone: "0909123456"
- address: "123 ABC Street"
- gender: "MALE"
- dob: "1990-01-15"
- avatar: avatar.jpg (< 5MB)

Expected:
✅ HTTP 200
✅ Response chứa token và user info
✅ Cookie refresh_token được set
✅ Redirect về home page
✅ User đã login (có access token)
```

#### Test Case 2: Đăng ký thành công (chỉ required fields)
```
Input:
- fullName: "Nguyen Van B"
- username: "nguyenvanb"
- email: "nguyenvanb@example.com"
- password: "123456"

Expected:
✅ HTTP 200
✅ Response chứa token
✅ Cookie refresh_token được set
```

#### Test Case 3: Username đã tồn tại
```
Input:
- username: "nguyenvana" (đã tồn tại)
- Other fields: valid

Expected:
❌ HTTP 400
❌ Message: "Username already exists"
❌ Error hiển thị ở field username
```

#### Test Case 4: Email đã tồn tại
```
Input:
- email: "nguyenvana@example.com" (đã tồn tại)
- Other fields: valid

Expected:
❌ HTTP 400
❌ Message: "Your email is existed"
❌ Error hiển thị ở field email
```

#### Test Case 5: Validation errors
```
Scenarios:
a) Password quá ngắn (< 6 chars)
   ❌ HTTP 400 hoặc client-side validation

b) Email format sai
   ❌ HTTP 400 hoặc client-side validation

c) Username chứa ký tự không hợp lệ (ví dụ: spaces, @)
   ❌ HTTP 400 hoặc client-side validation

d) Missing required field
   ❌ HTTP 400 hoặc client-side validation
```

#### Test Case 6: Avatar validation
```
Scenarios:
a) File không phải image
   ❌ Client-side validation: "Avatar must be an image file"

b) File quá lớn (> 5MB)
   ❌ Client-side validation: "Avatar size must be less than 5MB"

c) Không chọn avatar (optional)
   ✅ HTTP 200, đăng ký thành công không có avatar
```

### 7.2. Browser DevTools Verification

**Kiểm tra Request (Network tab):**
```
1. Mở DevTools → Network tab
2. Submit form đăng ký
3. Tìm request `POST /auth/signup`
4. Kiểm tra:
   ✅ Request Method: POST
   ✅ Content-Type: multipart/form-data; boundary=...
   ✅ Request Payload: có tất cả fields đã gửi
   ✅ Headers: không có Authorization (đăng ký lần đầu)
```

**Kiểm tra Response (Network tab):**
```
1. Click vào request `POST /auth/signup`
2. Tab "Response":
   ✅ Status: 200 OK
   ✅ Body: JSON chứa token, username, roles, id
3. Tab "Headers" → Response Headers:
   ✅ Set-Cookie: refresh_token=...; HttpOnly; SameSite=Lax; Max-Age=1209600
```

**Kiểm tra Cookie (Application/Storage tab):**
```
1. Mở DevTools → Application (Chrome) / Storage (Firefox)
2. Sidebar → Cookies → http://localhost:8080
3. Tìm cookie `refresh_token`:
   ✅ Name: refresh_token
   ✅ Value: eyJhbGc... (JWT token)
   ✅ Domain: localhost
   ✅ Path: /
   ✅ HttpOnly: ✓ (checked)
   ✅ SameSite: Lax
   ✅ Expires: ~14 days from now
```

**Kiểm tra LocalStorage:**
```
1. DevTools → Application/Storage → Local Storage
2. Kiểm tra key `access_token`:
   ✅ Key: access_token
   ✅ Value: eyJhbGc... (JWT token)
```

### 7.3. Automated Testing (Jasmine/Karma)

```typescript
// auth.service.spec.ts
describe('AuthService', () => {
  let service: AuthService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [AuthService]
    });
    service = TestBed.inject(AuthService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should register user successfully', () => {
    const formData = new FormData();
    formData.append('fullName', 'Test User');
    formData.append('username', 'testuser');
    formData.append('email', 'test@example.com');
    formData.append('password', '123456');

    const mockResponse = {
      message: 'Sign up successfully',
      data: {
        token: 'fake-jwt-token',
        username: 'testuser',
        roles: ['ROLE_GUEST'],
        id: 1
      },
      status: 'OK'
    };

    service.register(formData).subscribe(response => {
      expect(response.token).toBe('fake-jwt-token');
      expect(response.username).toBe('testuser');
      expect(localStorage.getItem('access_token')).toBe('fake-jwt-token');
    });

    const req = httpMock.expectOne('http://localhost:8080/auth/signup');
    expect(req.request.method).toBe('POST');
    expect(req.request.withCredentials).toBe(true);
    req.flush(mockResponse);
  });

  it('should handle duplicate username error', () => {
    const formData = new FormData();
    formData.append('username', 'existinguser');

    service.register(formData).subscribe({
      next: () => fail('Should have failed'),
      error: (error) => {
        expect(error.error.message).toBe('Username already exists');
      }
    });

    const req = httpMock.expectOne('http://localhost:8080/auth/signup');
    req.flush({ message: 'Username already exists' }, { status: 400, statusText: 'Bad Request' });
  });
});
```

---

## 8. Troubleshooting

### Vấn đề 1: Cookie không được set

**Triệu chứng:** Response thành công nhưng không thấy cookie `refresh_token` trong DevTools.

**Nguyên nhân & Giải pháp:**
- ❌ Thiếu `withCredentials: true` trong request
  - ✅ Thêm `{ withCredentials: true }` vào HttpClient options
  
- ❌ CORS không cho phép credentials
  - ✅ Kiểm tra backend CorsConfig: `allowCredentials(true)`
  - ✅ Kiểm tra backend expose header `Set-Cookie`

- ❌ Domain/origin không match
  - ✅ Kiểm tra backend allowed origins có chứa `http://localhost:4200`

### Vấn đề 2: Cookie không được gửi trong request tiếp theo

**Triệu chứng:** Request `/auth/refresh` hoặc protected endpoints không có cookie.

**Nguyên nhân & Giải pháp:**
- ❌ Thiếu `withCredentials: true` trong request
  - ✅ Đảm bảo tất cả requests có `withCredentials: true`
  
- ❌ Cookie domain/path không khớp
  - ✅ Kiểm tra cookie `Domain` và `Path` trong DevTools
  - ✅ Cookie `Path=/` sẽ gửi cho tất cả endpoints

### Vấn đề 3: Access token không được lưu

**Triệu chứng:** Sau đăng ký, user vẫn chưa login.

**Nguyên nhân & Giải pháp:**
- ❌ Không gọi `setAccessToken()` sau nhận response
  - ✅ Thêm `tap()` operator lưu token trong service
  
- ❌ LocalStorage bị disable (private mode)
  - ✅ Handle error khi save localStorage
  - ✅ Fallback sang in-memory storage

### Vấn đề 4: CORS error

**Triệu chứng:** Console error "CORS policy blocked..."

**Nguyên nhân & Giải pháp:**
- ❌ Backend CORS config sai
  - ✅ Kiểm tra `CorsConfig` allow origin chứa frontend URL
  - ✅ Kiểm tra `allowCredentials(true)`
  
- ❌ Request không match CORS preflight
  - ✅ Kiểm tra backend allow methods: `POST`, `OPTIONS`
  - ✅ Kiểm tra backend allow headers: `Content-Type`, `Authorization`

---

## 9. Best Practices Summary

### ✅ DO (Nên làm):

1. ✅ **Luôn dùng `withCredentials: true`** cho mọi request cần cookie
2. ✅ **Validate input client-side trước khi gửi** (giảm tải server)
3. ✅ **Handle errors gracefully** (hiển thị message rõ ràng cho user)
4. ✅ **Lưu access token vào localStorage** (hoặc memory nếu cần security cao hơn)
5. ✅ **Sử dụng Interceptor** để tự động refresh token khi 401
6. ✅ **Clear sensitive data** khi logout (access token, user info)
7. ✅ **Validate file upload** (type, size) client-side
8. ✅ **Show loading state** khi submit form (disable button, spinner)
9. ✅ **Log errors** để debug (console.error hoặc error tracking service)

### ❌ DON'T (Không nên làm):

1. ❌ **KHÔNG set header `Content-Type` cho multipart/form-data** (browser tự set boundary)
2. ❌ **KHÔNG cố đọc refresh_token từ JavaScript** (HttpOnly cookie)
3. ❌ **KHÔNG lưu refresh_token vào localStorage/sessionStorage** (security risk)
4. ❌ **KHÔNG gửi password không mã hoá qua URL query params** (luôn dùng request body)
5. ❌ **KHÔNG hardcode sensitive info** (API URL, secrets) → dùng environment variables
6. ❌ **KHÔNG bỏ qua validation errors** từ backend
7. ❌ **KHÔNG submit form nhiều lần** (disable button khi đang xử lý)

---

## 10. Kết luận

### Flow hoàn chỉnh từ Frontend → Backend:

```
1. User điền form đăng ký
   ↓
2. Frontend validate client-side
   ↓
3. Frontend build FormData với tất cả fields
   ↓
4. Frontend gửi POST /auth/signup với withCredentials: true
   ↓
5. Backend validate, hash password, lưu user vào DB
   ↓
6. Backend generate access token + refresh token
   ↓
7. Backend trả response:
   - Body: { token: accessToken, username, roles, id }
   - Cookie: Set-Cookie: refresh_token (HttpOnly)
   ↓
8. Frontend nhận response:
   - Lưu access token vào localStorage
   - Cookie refresh_token tự động được browser lưu
   ↓
9. Frontend redirect user về home page
   ↓
10. User đã login, có thể gọi protected APIs với access token
```

### Khi access token hết hạn (15 phút):

```
1. User gọi protected API
   ↓
2. Backend trả 401 Unauthorized
   ↓
3. Interceptor bắt 401 → gọi POST /auth/refresh (withCredentials: true)
   ↓
4. Backend đọc refresh_token từ cookie, validate
   ↓
5. Backend generate access token mới + refresh token mới (rotation)
   ↓
6. Backend trả:
   - Body: { token: newAccessToken }
   - Cookie: Set-Cookie: refresh_token (new)
   ↓
7. Interceptor lưu access token mới
   ↓
8. Interceptor retry request ban đầu với token mới
   ↓
9. User tiếp tục sử dụng app (không bị logout)
```

---

**File này được tạo để hướng dẫn frontend Angular triển khai đúng flow đăng ký theo backend specification.**

**Liên hệ Backend team nếu có vấn đề về API contract hoặc CORS configuration.**

---

*Cập nhật lần cuối: 18-10-2025*

