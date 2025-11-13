import { Injectable } from '@angular/core';
import { HttpInterceptor, HttpRequest, HttpHandler, HttpEvent, HttpErrorResponse } from '@angular/common/http';
import { Observable, throwError, BehaviorSubject } from 'rxjs';
import { catchError, filter, take, switchMap } from 'rxjs/operators';
import { AuthService } from '../services/auth.service';

/**
 * NOTE: Enhanced HTTP Interceptor with CSRF protection and debounced refresh
 * - Automatically adds Authorization header with AT for protected requests
 * - Handles 401/403 errors with automatic token refresh
 * - Prevents refresh request storms with debouncing
 * - Adds X-CSRF-Check header for refresh/logout endpoints
 */

@Injectable()
export class TokenInterceptor implements HttpInterceptor {
    private isRefreshing = false;
    private refreshTokenSubject: BehaviorSubject<string | null> = new BehaviorSubject<string | null>(null);

    constructor(private authService: AuthService) {}

    intercept(req: HttpRequest<any>, next: HttpHandler): Observable<HttpEvent<any>> {
        // NOTE: Add AT and credentials to request
        const authReq = this.addAuthHeader(req);

        return next.handle(authReq).pipe(
            catchError((error: HttpErrorResponse) => {
                // NOTE: Handle 401/403 with automatic refresh
                if ((error.status === 401 || error.status === 403) && !this.shouldSkipRefresh(req.url)) {
                    return this.handle401Error(authReq, next);
                }
                return throwError(() => error);
            })
        );
    }

    /**
     * NOTE: Add Authorization header and enable credentials
     * - Skips adding AT for auth endpoints (login, refresh, logout)
     * - Always enables withCredentials for HttpOnly RT cookie
     * - Adds X-CSRF-Check header for refresh/logout
     */
    private addAuthHeader(req: HttpRequest<any>): HttpRequest<any> {
        const token = this.authService.getToken();
        const isCsrfRequired = this.requiresCsrfHeader(req.url);
        
        const headers: { [key: string]: string } = {};
        
        // Add Authorization header for non-auth endpoints
        if (token && !this.shouldSkipRefresh(req.url)) {
            headers['Authorization'] = `Bearer ${token}`;
        }
        
        // NOTE: Add CSRF protection header for refresh/logout
        if (isCsrfRequired) {
            headers['X-CSRF-Check'] = '1';
        }
        
        return req.clone({
            setHeaders: headers,
            withCredentials: true // NOTE: Always send cookies (for RT)
        });
    }

    /**
     * NOTE: Check if URL requires CSRF header
     * - Refresh and logout endpoints need CSRF protection
     */
    private requiresCsrfHeader(url: string): boolean {
        return url.includes('/auth/refresh') || url.includes('/auth/logout');
    }

    /**
     * NOTE: Check if URL should skip automatic refresh
     * - Auth endpoints don't need refresh retry
     */
    private shouldSkipRefresh(url: string): boolean {
        return url.includes('/auth/login') || 
               url.includes('/auth/signup') ||
               url.includes('/auth/register') ||
               url.includes('/auth/refresh') ||
               url.includes('/auth/logout') ||
               url.includes('/auth/social-login') ||
               url.includes('/auth/social/callback');
    }

    /**
     * NOTE: Handle 401/403 errors with debounced refresh
     * - Prevents multiple simultaneous refresh requests
     * - Queues subsequent requests until refresh completes
     * - Retries original request with new token
     * - Logs out if refresh fails
     */
    private handle401Error(request: HttpRequest<any>, next: HttpHandler): Observable<HttpEvent<any>> {
        if (this.isRefreshing) {
            // NOTE: Queue this request until refresh completes
            console.log('⏳ Request queued - waiting for ongoing refresh...');
            
            return this.refreshTokenSubject.pipe(
                filter(token => token !== null),
                take(1),
                switchMap(token => {
                    console.log('✅ Using refreshed token for queued request');
                    return next.handle(this.addAuthHeader(request));
                }),
                catchError(error => {
                    // If queued request still fails, pass error through
                    console.warn('⚠️ Queued request failed after refresh:', error);
                    return throwError(() => error);
                })
            );
        }

        // NOTE: Start refresh process
        this.isRefreshing = true;
        this.refreshTokenSubject.next(null);

        console.log('🔄 Starting token refresh due to 401/403...');

        return this.authService.refresh().pipe(
            switchMap(() => {
                this.isRefreshing = false;
                const newToken = this.authService.getToken();
                this.refreshTokenSubject.next(newToken);
                
                console.log('✅ Token refreshed, retrying original request');
                
                // Retry original request with new token
                return next.handle(this.addAuthHeader(request));
            }),
            catchError(error => {
                this.isRefreshing = false;
                this.refreshTokenSubject.next(null);
                
                console.error('❌ Token refresh failed:', error);
                
                // NOTE: If refresh fails with 401/403, it means RT is invalid
                // Clear auth state and logout
                if (error.status === 401 || error.status === 403) {
                    console.error('❌ Refresh token invalid, logging out...');
                    this.authService.logout();
                }
                
                return throwError(() => error);
            })
        );
    }
}
