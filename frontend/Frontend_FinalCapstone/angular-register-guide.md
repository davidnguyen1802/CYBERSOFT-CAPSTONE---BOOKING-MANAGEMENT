# Hướng dẫn Frontend Angular: Đăng ký (Register / Signup)

Tài liệu này mô tả cách frontend Angular gọi API đăng ký (`POST /auth/signup`) của backend trong project, xử lý access token và refresh token cookie (HttpOnly), và cung cấp ví dụ mã TypeScript: model, AuthService, HttpInterceptor, và snippet component.

---
Tóm tắt nhanh (developer view)
- Endpoint đăng ký: `POST /auth/signup`
  - Content-Type: multipart/form-data
  - Trường form data (camelCase): `fullName`, `username`, `email`, `password`, `phone` (opt), `address` (opt), `gender` ("MALE"|"FEMALE"|"NONE"), `dob` ("YYYY-MM-DD"), `facebookAccountId`, `googleAccountId`, file `avatar` (opt)
  - Server trả `ResponseObject` chứa `AuthResponse` trong `data`. `data.token` = access token (JWT). Refresh token được đặt vào cookie HttpOnly tên `refresh_token` (không trả trong body).
- Khi access token hết hạn: frontend gọi `POST /auth/refresh` (empty body) với `withCredentials: true`. Backend đọc cookie `refresh_token` và trả access token mới trong response body + set-cookie refresh_token mới (rotation).
- CORS: backend đã cấu hình allowCredentials=true và cho origin `http://localhost:4200`. Mọi request cần cookie (signup, refresh, logout) phải dùng `withCredentials: true`.

---
Chi tiết dữ liệu và format
- `gender`: gửi text "MALE", "FEMALE" hoặc "NONE".
- `dob`: gửi string ISO date `YYYY-MM-DD` (mapping vào `LocalDate` ở backend).
- `avatar`: field name phải là `avatar` (multipart file).
- Không cố gắng đọc cookie `refresh_token` từ JS — cookie là HttpOnly. Browser sẽ gửi cookie tự động nếu `withCredentials: true`.

---
TypeScript models (ví dụ)
Tạo `src/app/models/auth.model.ts`:

```ts
export interface AuthResponseDTO {
  message: string;
  token: string; // access token
  refresh_token?: string | null;
  tokenType?: string;
  id?: number;
  username?: string;
  roles?: string[];
}

export interface ResponseObject<T=any> {
  message: string;
  data: T;
  status: number;
}
```

---
`AuthService` (gửi form-data, lưu access token, refresh)
Tạo `src/app/services/auth.service.ts` (ví dụ):

```ts
import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { ResponseObject, AuthResponseDTO } from '../models/auth.model';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private baseUrl = 'http://localhost:8080/auth';

  constructor(private http: HttpClient) {}

  register(form: {
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
  }, avatarFile?: File): Observable<AuthResponseDTO> {
    const fd = new FormData();
    fd.append('fullName', form.fullName);
    fd.append('username', form.username);
    fd.append('email', form.email);
    fd.append('password', form.password);
    if (form.phone) fd.append('phone', form.phone);
    if (form.address) fd.append('address', form.address);
    if (form.gender) fd.append('gender', form.gender);
    if (form.dob) fd.append('dob', form.dob);
    if (form.facebookAccountId) fd.append('facebookAccountId', form.facebookAccountId);
    if (form.googleAccountId) fd.append('googleAccountId', form.googleAccountId);
    if (avatarFile) fd.append('avatar', avatarFile, avatarFile.name);

    return this.http.post<ResponseObject<AuthResponseDTO>>(
      `${this.baseUrl}/signup`,
      fd,
      { withCredentials: true }
    ).pipe(map(resp => {
      const auth = resp.data;
      if (auth && auth.token) {
        localStorage.setItem('access_token', auth.token);
      }
      return auth;
    }));
  }

  refreshToken(): Observable<AuthResponseDTO> {
    return this.http.post<ResponseObject<AuthResponseDTO>>(
      `${this.baseUrl}/refresh`,
      {},
      { withCredentials: true }
    ).pipe(map(r => {
      const auth = r.data;
      if (auth && auth.token) localStorage.setItem('access_token', auth.token);
      return auth;
    }));
  }

  getAccessToken(): string | null {
    return localStorage.getItem('access_token');
  }

  signOut() {
    return this.http.post(`${this.baseUrl}/logout`, {}, { withCredentials: true });
  }
}
```

---
`HttpInterceptor` (tự động thêm Authorization, refresh khi 401)
Tạo `src/app/interceptors/auth.interceptor.ts`:

```ts
import { Injectable } from '@angular/core';
import { HttpEvent, HttpHandler, HttpInterceptor, HttpRequest, HttpErrorResponse } from '@angular/common/http';
import { Observable, BehaviorSubject, throwError } from 'rxjs';
import { catchError, filter, switchMap, take } from 'rxjs/operators';
import { AuthService } from '../services/auth.service';
import { Router } from '@angular/router';

@Injectable()
export class AuthInterceptor implements HttpInterceptor {
  private isRefreshing = false;
  private refreshTokenSubject = new BehaviorSubject<string | null>(null);

  constructor(private authService: AuthService, private router: Router) {}

  intercept(req: HttpRequest<any>, next: HttpHandler): Observable<HttpEvent<any>> {
    if (req.url.endsWith('/auth/refresh') || req.url.endsWith('/auth/login') || req.url.endsWith('/auth/signup')) {
      return next.handle(req.clone({ withCredentials: true }));
    }

    const token = this.authService.getAccessToken();
    let authReq = req.clone({ withCredentials: true });
    if (token) {
      authReq = req.clone({ setHeaders: { Authorization: `Bearer ${token}` }, withCredentials: true });
    }

    return next.handle(authReq).pipe(
      catchError(err => {
        if (err instanceof HttpErrorResponse && err.status === 401) {
          return this.handle401Error(authReq, next);
        }
        return throwError(() => err);
      })
    );
  }

  private handle401Error(req: HttpRequest<any>, next: HttpHandler) {
    if (!this.isRefreshing) {
      this.isRefreshing = true;
      this.refreshTokenSubject.next(null);

      return this.authService.refreshToken().pipe(
        switchMap(auth => {
          this.isRefreshing = false;
          const newToken = auth?.token ?? null;
          this.refreshTokenSubject.next(newToken);
          if (newToken) {
            const clone = req.clone({ setHeaders: { Authorization: `Bearer ${newToken}` }, withCredentials: true });
            return next.handle(clone);
          }
          this.router.navigate(['/login']);
          return throwError(() => new Error('No new token'));
        }),
        catchError(err => {
          this.isRefreshing = false;
          this.router.navigate(['/login']);
          return throwError(() => err);
        })
      );
    } else {
      return this.refreshTokenSubject.pipe(
        filter(t => t !== null),
        take(1),
        switchMap(token => {
          const clone = req.clone({ setHeaders: { Authorization: `Bearer ${token}`! }, withCredentials: true });
          return next.handle(clone);
        })
      );
    }
  }
}
```

Đăng ký interceptor trong `AppModule`:
```ts
providers: [
  { provide: HTTP_INTERCEPTORS, useClass: AuthInterceptor, multi: true }
]
```

---
Snippet component (đăng ký)
```ts
// register.component.ts (example)
onSubmit() {
  const payload = {
    fullName: this.form.value.fullName,
    username: this.form.value.username,
    email: this.form.value.email,
    password: this.form.value.password,
    phone: this.form.value.phone,
    address: this.form.value.address,
    gender: this.form.value.gender, // MALE/FEMALE/NONE
    dob: this.form.value.dob // 'YYYY-MM-DD'
  };
  this.authService.register(payload, this.selectedFile).subscribe({
    next: auth => {
      // access token saved by service
      this.router.navigate(['/']);
    },
    error: err => {
      // show error
      console.error(err);
    }
  });
}
```

---
Các lưu ý quan trọng & debugging
- Luôn dùng `withCredentials: true` cho các request liên quan đến cookie (signup, refresh, logout).
- Nếu browser không nhận/ gửi cookie:
  - Kiểm tra response header `Set-Cookie` từ backend (DevTools -> Network -> Response headers).
  - Kiểm tra `Access-Control-Allow-Credentials: true` và `Access-Control-Allow-Origin` không phải `*`.
- Trong production: set `secure=true` cho cookie và dùng HTTPS.
- Refresh logic: interceptor chỉ gọi `/auth/refresh` khi nhận 401; backend sẽ trả 401 nếu refresh token mất hiệu lực.
- Không lưu refresh token vào localStorage/sessionStorage (server dùng HttpOnly cookie đúng là cách an toàn hơn).

---
Các câu hỏi thường gặp
1) Backend trả `refresh_token` cả trong body và cookie?  
   - Hiện backend chỉ trả access token trong body và đặt refresh token vào HttpOnly cookie.
2) Tôi có thể test bằng Postman?  
   - Postman không lưu cookie between calls trừ khi bật "Automatically follow redirects" và enable cookie jar. Bật cookie jar hoặc test qua browser client.
3) Access token hết hạn khi đang dùng app -> UI có bị logout?  
   - Nếu interceptor cấu hình đúng, khi nhận 401 sẽ tự gọi `/auth/refresh`, nhận access token mới và retry request. Nếu refresh fail, redirect tới login.

---
Tài liệu liên quan (code backend tham khảo)
- `AuthenticationController` (các endpoint `/auth/login`, `/auth/signup`, `/auth/refresh`) 
- `JwtTokenUtil` (config keys: `jwt.access-expiration-ms`, `jwt.refresh-expiration-ms`)
- `CorsConfig` (đã bật allowCredentials và expose Set-Cookie)

---
Muốn tôi làm tiếp gì?
- Tạo file ví dụ TypeScript hoàn chỉnh trong repo (src/... trên frontend) để frontend dev copy/paste?  
- Hoặc chỉnh backend để `/auth/me` trả profile chi tiết (hoặc đổi tên endpoint)?

Hết.

