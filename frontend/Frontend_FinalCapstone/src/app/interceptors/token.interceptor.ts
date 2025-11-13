import { Injectable } from '@angular/core';
import { HttpInterceptor, HttpRequest, HttpHandler, HttpEvent, HttpErrorResponse } from '@angular/common/http';
import { Observable, throwError, BehaviorSubject } from 'rxjs';
import { catchError, filter, take, switchMap, tap } from 'rxjs/operators';
import { TokenService } from '../services/token.service';
import { UserService } from '../services/user.service';
import { AuthStateService } from '../services/auth-state.service';
import { SimpleModalService } from '../services/simple-modal.service';
import { DeviceService } from '../services/device.service';
import { Router } from '@angular/router';

@Injectable()
export class TokenInterceptor implements HttpInterceptor {
    private isRefreshing = false;
    private refreshTokenSubject: BehaviorSubject<string | null> = new BehaviorSubject<string | null>(null);

    constructor(
        private tokenService: TokenService,
        private userService: UserService,
        private router: Router,
        private authStateService: AuthStateService,
        private modalService: SimpleModalService,
        private deviceService: DeviceService
    ) { }

    intercept(
        req: HttpRequest<any>,
        next: HttpHandler): Observable<HttpEvent<any>> {
        
        // Check if this is a public endpoint FIRST (no auth needed)
        const isPublicEndpoint = this.isPublicEndpoint(req.url);
        
        if (isPublicEndpoint) {
            // Public endpoint - skip token check, just pass through
            // Removed verbose logging for performance
            return next.handle(req).pipe(
                catchError((error: HttpErrorResponse) => {
                    // Handle 428 Precondition Required - Missing/Invalid X-Device-Id
                    if (error.status === 428) {
                        return this.handle428Error(error, req, next);
                    }
                    
                    // Handle status codes from /auth/refresh endpoint
                    if (error.status === 419) {
                        return this.handle419Error(error, req.url);
                    }
                    
                    if (error.status === 449) {
                        return this.handle449Error(error, req.url);
                    }
                    
                    if (error.status === 498) {
                        return this.handle498Error(error, req.url);
                    }
                    
                    if (error.status === 499) {
                        return this.handle499Error(error, req.url);
                    }
                    
                    return throwError(() => error);
                })
            );
        }
        
        // Protected endpoint - get token ONCE and check expiration
        const token = this.tokenService.getToken();
        
        // PROACTIVE TOKEN REFRESH: Check if token is expired BEFORE making request
        // NOTE: /auth/logout is NOT a public endpoint - requires valid Authorization header
        // If token expired, MUST refresh first to get valid token, otherwise BE will reject with 401
        if (token && this.tokenService.isTokenExpired()) {
            console.warn('⚠️ Token expired - triggering proactive refresh before API call');
            console.warn('   URL:', req.url.split('?')[0]);
            if (req.url.includes('/auth/logout')) {
                console.warn('   NOTE: /auth/logout requires valid Authorization, refreshing token first');
            }
            
            // Trigger refresh FIRST, then retry original request with new token
            return this.performTokenRefresh(req, next);
        }
        
        let authReq = req;
        
        if (token) {
            // Add Authorization header
            authReq = req.clone({
                setHeaders: {
                    Authorization: `Bearer ${token}`
                }
            });
            // Only log for important endpoints or first request
            if (req.url.includes('/users/me/details') || req.url.includes('/auth/')) {
                console.log('🔒 Protected:', req.url.split('?')[0]); // Log without query params
            }
        } else {
            // Protected endpoint but NO TOKEN - this is important to log
            console.warn('⚠️ No token for protected endpoint:', req.url);
        }

        return next.handle(authReq).pipe(
            catchError((error: HttpErrorResponse) => {
                // Handle 428 Precondition Required - Missing/Invalid X-Device-Id
                if (error.status === 428) {
                    return this.handle428Error(error, authReq, next);
                }
                
                // Handle status codes from /auth/refresh endpoint
                if (error.status === 419) {
                    return this.handle419Error(error, req.url);
                }
                
                if (error.status === 449) {
                    return this.handle449Error(error, req.url);
                }
                
                if (error.status === 498) {
                    return this.handle498Error(error, req.url);
                }
                
                if (error.status === 499) {
                    return this.handle499Error(error, req.url);
                }
                
                // Handle 401 Unauthorized - Trigger refresh for protected endpoints
                if (error.status === 401) {
                    return this.handle401Error(authReq, next, error);
                }
                
                return throwError(() => error);
            })
        );
    }
    
    /**
     * Check if URL is a public endpoint (no auth required)
     */
    private isPublicEndpoint(url: string): boolean {
        const publicPatterns = [
            '/auth/login',
            '/auth/signup',
            '/auth/refresh',
            '/auth/social-login',
            '/auth/social/callback',
            '/auth/forgot-password',
            '/auth/reset-password',
            '/auth/validate-reset-token',
            '/locations',
            '/cities'
            // NOTE: Removed '/images' - POST /images/property REQUIRES auth
            // NOTE: Removed '/files' - may require auth for upload
        ];
        
        // Check exact match for property endpoints (avoid matching /user/favorites/*/property/*)
        const propertyPublicEndpoints = [
            '/properties',  // GET all properties
            '/property/'    // GET single property (e.g., /property/123)
        ];
        
        // First check if it's a protected user endpoint with "property" in path
        if (url.includes('/user/') || url.includes('/users/')) {
            return false; // All /user/* and /users/* endpoints are protected
        }
        
        // Check property endpoints - only public if it starts with /properties or /property/
        const hasPropertyInPath = propertyPublicEndpoints.some(pattern => {
            if (pattern === '/properties') {
                // Match /properties, /properties?query, /properties/search
                return url.includes('/properties');
            } else if (pattern === '/property/') {
                // Match /property/123 but NOT /user/favorites/21/property/44
                const propertyPattern = /\/property\/\d+(?:\/|$|\?)/;
                return propertyPattern.test(url);
            }
            return false;
        });
        
        if (hasPropertyInPath) return true;
        
        // Check other public patterns
        return publicPatterns.some(pattern => url.includes(pattern));
    }

    /**
     * Check if URL should skip automatic token refresh
     * Only auth endpoints should skip (they're already in auth flow)
     */
    private shouldSkipRefresh(url: string): boolean {
        return url.includes('/auth/login') || 
               url.includes('/auth/signup') ||
               url.includes('/auth/register') ||
               url.includes('/auth/refresh') ||
               url.includes('/auth/social-login') ||
               url.includes('/auth/social/callback');
    }

    /**
     * Add access token to request header
     */
    private addTokenAndCredentials(request: HttpRequest<any>, token: string): HttpRequest<any> {
        // Preserve all existing headers and update Authorization
        // This ensures X-Device-Id, Content-Type, etc. are not lost
        const headers = request.headers.set('Authorization', `Bearer ${token}`);
        
        return request.clone({
            headers: headers
        });
    }

    /**
     * Handle 401 UNAUTHORIZED error
     * 
     * NEW LOGIC (after backend change):
     * - Backend now returns specific status codes (419, 449, 498, 499) for /auth/refresh errors
     * - 401 is now ONLY for "access token expired/invalid" from protected endpoints
     * 
     * Logic:
     * 1. If from /auth/refresh → Don't retry refresh (avoid infinite loop)
     * 2. If from auth endpoints (login, signup) → Skip refresh (already in auth flow)
     * 3. Otherwise → Trigger refresh (access token may be expired)
     */
    private handle401Error(
        request: HttpRequest<any>, 
        next: HttpHandler,
        error: HttpErrorResponse
    ): Observable<HttpEvent<any>> {
        const url = request.url;
        const errorMessage = error?.error?.message || error?.message || error?.statusText || '';
        
        // Log all 401 errors
        console.error('🔴 401 UNAUTHORIZED Error:');
        console.error('   URL:', url);
        console.error('   Message:', errorMessage);
        
        // Case 1: Error from /auth/refresh → Don't retry (avoid infinite loop)
        if (url.includes('/auth/refresh')) {
            console.error('   → 401 from /auth/refresh endpoint');
            console.error('   → NOT retrying refresh (would cause infinite loop)');
            
            // Clear tokens and show session expired modal
            this.tokenService.removeToken();
            this.authStateService.notifyLogout();
            this.modalService.showSessionExpired();
            
            return throwError(() => error);
        }
        
        // Case 2: Skip refresh for auth endpoints (login, signup, etc.)
        if (this.shouldSkipRefresh(url)) {
            console.warn('   → Skipping refresh for auth endpoint');
            console.warn('   → Endpoint:', url);
            return throwError(() => error);
        }
        
        // Case 3: 401 from protected endpoint (like /users/me/details, /properties/favorites)
        // → Access token expired/invalid → Trigger refresh
        console.warn('   → 401 from protected endpoint');
        console.warn('   → Access token may be expired/invalid');
        console.warn('   → Attempting token refresh...');
        return this.performTokenRefresh(request, next);
    }

    /**
     * Handle 428 PRECONDITION_REQUIRED - Missing or invalid X-Device-Id
     * Backend message: "X-Device-Id header is required" or "Invalid X-Device-Id format"
     * From: Auth endpoints (/auth/login, /auth/signup, /auth/refresh, etc.)
     * Action: Clear invalid deviceId, generate new one, and retry request
     * 
     * This is a FALLBACK handler - should rarely trigger because:
     * - DeviceService.getDeviceId() auto-validates and auto-generates
     * - DeviceIdInterceptor always sends valid UUID v4
     * 
     * When this triggers:
     * - Edge case: localStorage corrupted AFTER validation
     * - Bug in validation logic
     * - Backend changed validation rules
     */
    private handle428Error(
        error: HttpErrorResponse,
        request: HttpRequest<any>,
        next: HttpHandler
    ): Observable<HttpEvent<any>> {
        const errorMessage = error?.error?.message || error?.message || error?.statusText || '';
        
        console.error('❌ 428 PRECONDITION_REQUIRED Error:');
        console.error('   URL:', request.url);
        console.error('   Message:', errorMessage);
        console.error('   Cause: X-Device-Id header missing or invalid format');
        console.error('   ⚠️ This should rarely happen (frontend validates before sending)');
        
        // Clear potentially invalid deviceId from localStorage
        this.deviceService.clearDeviceId();
        console.log('🗑️ Cleared old deviceId from localStorage');
        
        // Generate new deviceId (getDeviceId() will auto-generate and validate)
        const newDeviceId = this.deviceService.getDeviceId();
        console.log('🆕 Generated new deviceId:', newDeviceId.substring(0, 8) + '...');
        
        // Clone request with new X-Device-Id header
        const clonedReq = request.clone({
            setHeaders: {
                'X-Device-Id': newDeviceId
            }
        });
        
        console.log('🔄 Retrying request with new deviceId...');
        
        // Retry request ONCE with new deviceId
        return next.handle(clonedReq).pipe(
            catchError((retryError: HttpErrorResponse) => {
                // If retry also fails, log and throw
                console.error('❌ Retry after 428 failed:');
                console.error('   Status:', retryError.status);
                console.error('   Message:', retryError?.error?.message || retryError?.message);
                console.error('   ⚠️ Backend may have changed validation rules or there is a bug');
                
                // Don't retry again - throw error to component
                return throwError(() => retryError);
            })
        );
    }

    /**
     * Handle 419 AUTHENTICATION_TIMEOUT - Token expired
     * Backend message: "Token expired"
     * From: /auth/refresh endpoint
     * Action: Clear tokens and show session expired modal
     */
    private handle419Error(error: HttpErrorResponse, url: string): Observable<never> {
        const errorMessage = error?.error?.message || error?.message || error?.statusText || '';
        
        console.error('🔴 419 AUTHENTICATION_TIMEOUT Error:');
        console.error('   URL:', url);
        console.error('   Message:', errorMessage);
        console.error('   Cause: Refresh token expired (need to re-login)');
        
        // Only handle if from /auth/refresh
        if (url.includes('/auth/refresh')) {
            this.tokenService.removeToken();
            this.authStateService.notifyLogout();
            this.modalService.showSessionExpired(); // SHOW MODAL
        }
        
        return throwError(() => error);
    }

    /**
     * Handle 449 RETRY_WITH - Token missing
     * Backend message: "Token missing"
     * From: /auth/refresh endpoint
     * Action: Clear tokens, set anonymous mode, NO modal (user can browse public pages)
     */
    private handle449Error(error: HttpErrorResponse, url: string): Observable<never> {
        const errorMessage = error?.error?.message || error?.message || error?.statusText || '';
        
        console.warn('⚠️ 449 RETRY_WITH Error:');
        console.warn('   URL:', url);
        console.warn('   Message:', errorMessage);
        console.warn('   Cause: Refresh token missing (anonymous mode)');
        
        // Only handle if from /auth/refresh
        if (url.includes('/auth/refresh')) {
            console.warn('   → Setting anonymous mode (no modal, no protected API calls)');
            this.tokenService.removeToken();
            this.authStateService.notifyLogout();
            // NO MODAL - let UI handle gracefully
        }
        
        return throwError(() => error);
    }

    /**
     * Handle 498 INVALID_TOKEN - Invalid token or token not found
     * Backend message: "Invalid token" or "Token not found"
     * From: /auth/refresh endpoint
     * Action: Same as 449 (anonymous mode, no modal) but LOG ERROR for monitoring
     */
    private handle498Error(error: HttpErrorResponse, url: string): Observable<never> {
        const errorMessage = error?.error?.message || error?.message || error?.statusText || '';
        
        // LOG ERROR for monitoring (invalid JWT signature, token not in DB)
        console.error('❌ 498 INVALID_TOKEN Error:');
        console.error('   URL:', url);
        console.error('   Message:', errorMessage);
        console.error('   Cause: Invalid JWT signature or token not found in database');
        console.error('   ⚠️ This may indicate security issue or data inconsistency');
        
        // Only handle if from /auth/refresh
        if (url.includes('/auth/refresh')) {
            console.warn('   → Treating as anonymous mode (like 449)');
            console.warn('   → No modal, user can browse public pages');
            this.tokenService.removeToken();
            this.authStateService.notifyLogout();
            // NO MODAL - same behavior as 449
        }
        
        return throwError(() => error);
    }

    /**
     * Handle 499 TOKEN_REVOKED - Token revoked
     * Backend message: "Token revoked"
     * From: /auth/refresh endpoint
     * Action: Same as 449 (anonymous mode, no modal) but LOG ERROR for monitoring
     */
    private handle499Error(error: HttpErrorResponse, url: string): Observable<never> {
        const errorMessage = error?.error?.message || error?.message || error?.statusText || '';
        
        // LOG ERROR for monitoring (manual revoke or reuse attack)
        console.error('❌ 499 TOKEN_REVOKED Error:');
        console.error('   URL:', url);
        console.error('   Message:', errorMessage);
        console.error('   Cause: Token revoked (logout or reuse attack detected)');
        console.error('   ⚠️ This may indicate security breach attempt');
        
        // Only handle if from /auth/refresh
        if (url.includes('/auth/refresh')) {
            console.warn('   → Treating as anonymous mode (like 449)');
            console.warn('   → No modal, user can browse public pages');
            this.tokenService.removeToken();
            this.authStateService.notifyLogout();
            // NO MODAL - same behavior as 449
        }
        
        return throwError(() => error);
    }

    /**
     * Perform token refresh with debounce protection
     * Only one refresh at a time - other requests queue and wait
     */
    private performTokenRefresh(request: HttpRequest<any>, next: HttpHandler): Observable<HttpEvent<any>> {
        // Check if already refreshing to prevent multiple refresh calls
        if (this.isRefreshing) {
            // Another request is already refreshing the token
            // Wait for the new token, then retry this request
            console.log('⏳ Request queued - waiting for ongoing token refresh...');
            
            return this.refreshTokenSubject.pipe(
                filter(token => token !== null),
                take(1),
                switchMap(token => {
                    console.log('✅ Using refreshed token for queued request');
                    return next.handle(this.addTokenAndCredentials(request, token!));
                }),
                catchError((error: HttpErrorResponse) => {
                    return throwError(() => error);
                })
            );
        }
        
        // Start refresh process
        this.isRefreshing = true;
        this.refreshTokenSubject.next(null);
        this.authStateService.setRefreshing(true);

        console.log('🔄 Starting token refresh...');
        
        return this.userService.refreshToken().pipe(
            switchMap((response: any) => {
                console.log('📥 REFRESH TOKEN RESPONSE RECEIVED:');
                console.log('   Full response:', response);
                console.log('   response.status:', response?.status);
                console.log('   response.message:', response?.message);
                console.log('   response.data type:', typeof response?.data);
                console.log('   response.data preview:', response?.data?.substring(0, 50) + '...');
                
                this.isRefreshing = false;
                this.authStateService.setRefreshing(false);
                
                // ⚠️ CRITICAL: Backend returns JWT string directly in response.data
                // Same format as login/signup: { status: "OK", message: "...", data: "JWT_STRING" }
                const newToken = response?.data;
                
                if (newToken && typeof newToken === 'string') {
                    console.log('✅ Token refreshed successfully');
                    console.log('   New token length:', newToken.length);
                    console.log('   New token preview:', newToken.substring(0, 50) + '...');
                    
                    // Save token with remember flag (will throw if invalid JWT format)
                    const remember = this.tokenService.getRememberMe();
                    try {
                        this.tokenService.setToken(newToken, remember);
                        console.log('✅ New token saved to storage');
                    } catch (error) {
                        console.error('❌ Failed to save refreshed token:', error);
                        this.tokenService.removeToken();
                        this.authStateService.notifyLogout();
                        return throwError(() => new Error('Invalid token format from refresh'));
                    }
                    
                    this.refreshTokenSubject.next(newToken);
                    this.authStateService.notifyTokenRefreshed(newToken);
                    
                    // Retry the original request with new token
                    console.log('🔄 Retrying original request with new token');
                    console.log('   Original URL:', request.url);
                    console.log('   Original method:', request.method);
                    console.log('   New token length:', newToken.length);
                    
                    const retryRequest = this.addTokenAndCredentials(request, newToken);
                    
                    // Log headers being sent (for debugging)
                    console.log('📤 RETRY Request headers (NOT logout!):');
                    console.log('   URL:', retryRequest.url);
                    console.log('   Method:', retryRequest.method);
                    console.log('   Authorization:', retryRequest.headers.has('Authorization') ? '✅ Present' : '❌ Missing');
                    console.log('   X-Device-Id:', retryRequest.headers.has('X-Device-Id') ? '✅ Present' : '❌ Missing');
                    console.log('   Content-Type:', retryRequest.headers.get('Content-Type') || 'not set');
                    
                    return next.handle(retryRequest).pipe(
                        tap((event: any) => {
                            // Log different event types
                            if (event.type === 0) { // HttpSentEvent
                                console.log('✅ Retry request sent successfully');
                                console.log('   URL:', retryRequest.url);
                            } else if (event.type === 4) { // HttpResponse
                                console.log('✅ Retry request completed');
                                console.log('   URL:', retryRequest.url);
                                console.log('   Status:', event.status);
                                console.log('   Response body preview:', JSON.stringify(event.body).substring(0, 200));
                            }
                        }),
                        catchError((err: HttpErrorResponse) => {
                            console.error('❌ Retry request failed after refresh:');
                            console.error('   URL:', err.url);
                            console.error('   Method:', request.method);
                            console.error('   Status:', err.status);
                            console.error('   Status Text:', err.statusText);
                            if (err.error) {
                                console.error('   Error body:', err.error);
                            }
                            return throwError(() => err);
                        })
                    );
                }
                
                // If no token in response or invalid format, handle as error
                console.error('❌ Invalid token in refresh response');
                console.error('   Expected: response.data = "JWT_STRING"');
                console.error('   Received:', response);
                return throwError(() => new Error('Invalid token format from refresh endpoint'));
            }),
            catchError((err: HttpErrorResponse) => {
                this.isRefreshing = false;
                this.authStateService.setRefreshing(false);
                
                const errorMessage = err?.error?.message || err?.message || '';
                
                console.error('❌ Token refresh request failed:');
                console.error('   Status:', err.status);
                console.error('   Message:', errorMessage);
                
                // Case 1: 449 RETRY_WITH - "Token missing"
                // → Anonymous mode (user hasn't logged in yet or RT cookie expired)
                // → NO MODAL, allow browsing public pages
                if (err.status === 449) {
                    console.warn('   → 449: Refresh token missing - anonymous mode');
                    this.tokenService.removeToken();
                    this.authStateService.notifyLogout();
                    return throwError(() => err); // NO MODAL
                }
                
                // Case 2: 419 AUTHENTICATION_TIMEOUT - "Token expired"
                // → Session expired (token reached max age)
                // → SHOW MODAL, require re-login
                if (err.status === 419) {
                    console.error('   → 419: Token expired - session expired');
                    this.tokenService.removeToken();
                    this.authStateService.notifyLogout();
                    this.modalService.showSessionExpired(); // SHOW MODAL
                    return throwError(() => err);
                }
                
                // Case 3: 498 INVALID_TOKEN - "Invalid token" or "Token not found"
                // → Treat as anonymous mode (like 449) but LOG for monitoring
                // → This indicates potential security issue or data inconsistency
                if (err.status === 498) {
                    console.error('   → 498: Invalid token or not found in database');
                    console.error('   → ⚠️ SECURITY ALERT: Log this for monitoring!');
                    console.warn('   → Treating as anonymous mode (no modal)');
                    this.tokenService.removeToken();
                    this.authStateService.notifyLogout();
                    return throwError(() => err); // NO MODAL
                }
                
                // Case 4: 499 TOKEN_REVOKED - "Token revoked"
                // → Treat as anonymous mode (like 449) but LOG for monitoring
                // → This may indicate reuse attack or manual revocation
                if (err.status === 499) {
                    console.error('   → 499: Token revoked');
                    console.error('   → ⚠️ SECURITY ALERT: Possible reuse attack! Log this for monitoring!');
                    console.warn('   → Treating as anonymous mode (no modal)');
                    this.tokenService.removeToken();
                    this.authStateService.notifyLogout();
                    return throwError(() => err); // NO MODAL
                }
                
                // Other errors (503 SERVICE_UNAVAILABLE, network issues, etc.) 
                // → Let them bubble up to component
                return throwError(() => err);
            })
        );
    }
}