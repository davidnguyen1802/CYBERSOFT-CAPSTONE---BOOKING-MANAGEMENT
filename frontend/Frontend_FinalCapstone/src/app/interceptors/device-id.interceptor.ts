import { Injectable } from '@angular/core';
import { HttpInterceptor, HttpRequest, HttpHandler, HttpEvent } from '@angular/common/http';
import { Observable } from 'rxjs';
import { DeviceService } from '../services/device.service';

/**
 * Device ID Interceptor (Class-based for NgModule)
 * 
 * NGUYÊN TẮC:
 * - CHỈ gắn X-Device-Id header NẾU device ID đã tồn tại
 * - CHỈ gắn cho AUTH-RELATED endpoints (refresh, logout, me)
 * - KHÔNG gắn cho public endpoints (property/top7, search, etc.)
 * - KHÔNG tự tạo device ID trong interceptor
 * - Device ID được tạo SAU KHI login/signup thành công
 * 
 * Header format:
 * X-Device-Id: 550e8400-e29b-41d4-a716-446655440000 (UUID v4)
 * 
 * Behavior:
 * - User chưa login → không có device ID → KHÔNG gắn header
 * - User đã login + public endpoint → KHÔNG gắn header
 * - User đã login + auth endpoint → TỰ ĐỘNG gắn header
 * 
 * Auth endpoints that NEED X-Device-Id:
 * - GET /auth/refresh - Backend validates device
 * - POST /auth/logout - Track which device to logout
 * - POST /auth/logout-all - Logout all devices for user
 * 
 * Auth endpoints that DON'T NEED X-Device-Id:
 * - GET /users/me - Only needs Access Token, no device tracking
 * 
 * Public endpoints that DON'T NEED X-Device-Id:
 * - GET /property/top7
 * - GET /property/search
 * - GET /city/list
 * - Any endpoint accessible without login
 * 
 * Backend usage:
 * - Login/Signup: KHÔNG cần X-Device-Id (device ID chưa tồn tại)
 * - Refresh: CẦN X-Device-Id để validate device
 * - Logout/Logout-all/Me: CẦN X-Device-Id để track device
 * 
 * Security:
 * - Device ID is NOT sensitive (just a random UUID)
 * - Device ID !== Access Token
 * - Used for tracking only, not authentication
 * 
 * CORS:
 * - Backend MUST allow X-Device-Id in CORS config for auth endpoints
 * - Public endpoints KHÔNG cần CORS config cho header này
 */
@Injectable()
export class DeviceIdInterceptor implements HttpInterceptor {
  constructor(private deviceService: DeviceService) {}

  intercept(req: HttpRequest<any>, next: HttpHandler): Observable<HttpEvent<any>> {
    // CHỈ gắn X-Device-Id header cho auth endpoints
    // Backend YÊU CẦU X-Device-Id cho: login, signup, refresh, logout, logout-all
    const isAuthRequest = 
      req.url.includes('/auth/login') ||
      req.url.includes('/auth/signup') ||
      req.url.includes('/auth/refresh') ||
      req.url.includes('/auth/logout') ||
      req.url.includes('/auth/logout-all') ||
      req.url.includes('/auth/social-login') ||
      req.url.includes('/auth/social/callback');

    // Nếu KHÔNG phải auth request → skip, không gắn header
    if (!isAuthRequest) {
      return next.handle(req);
    }

    // CHECK: Nếu request đã có X-Device-Id header → skip (không override)
    // Điều này đảm bảo UserService.logout() có thể set device_id chính xác
    if (req.headers.has('X-Device-Id')) {
      console.log('📤 Auth request already has X-Device-Id header, skipping interceptor');
      return next.handle(req);
    }

    // SPECIAL CASE: /auth/logout - chỉ thêm header NẾU device_id đã tồn tại trong LS
    // KHÔNG tự động tạo device_id mới khi logout (vì device_id mới không match với backend)
    if (req.url.includes('/auth/logout')) {
      const deviceIdFromLS = localStorage.getItem('device_id');
      if (!deviceIdFromLS) {
        console.warn('⚠️ /auth/logout: No device_id in localStorage, skipping X-Device-Id header');
        console.warn('   Backend may still process logout via Authorization token and RT cookie');
        return next.handle(req);
      }
      
      // Validate format
      const isValidUUID = /^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i.test(deviceIdFromLS);
      if (!isValidUUID) {
        console.warn('⚠️ /auth/logout: Invalid device_id format, skipping X-Device-Id header');
        return next.handle(req);
      }
      
      // Add header với device_id từ LS (không dùng getDeviceId() để tránh auto-generate)
      const clonedReq = req.clone({
        setHeaders: {
          'X-Device-Id': deviceIdFromLS
        }
      });

      console.log('📤 Auth logout request with device ID:', {
        url: req.url,
        deviceId: deviceIdFromLS.substring(0, 8) + '...'
      });

      return next.handle(clonedReq);
    }

    // OTHER AUTH ENDPOINTS: Get device ID (auto-generate nếu chưa có hoặc invalid)
    // getDeviceId() LUÔN trả về valid UUID v4 (không bao giờ null)
    const deviceId = this.deviceService.getDeviceId();

    // Clone request và thêm header X-Device-Id
    const clonedReq = req.clone({
      setHeaders: {
        'X-Device-Id': deviceId
      }
    });

    // Log for debugging
    console.log('📤 Auth request with device ID:', {
      url: req.url,
      deviceId: deviceId.substring(0, 8) + '...' // Log first 8 chars only
    });

    return next.handle(clonedReq);
  }
}

