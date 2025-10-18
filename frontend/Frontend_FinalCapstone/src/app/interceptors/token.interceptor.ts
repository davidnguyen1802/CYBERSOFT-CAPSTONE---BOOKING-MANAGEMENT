import { Injectable } from '@angular/core';
import { HttpInterceptor, HttpRequest, HttpHandler, HttpEvent, HttpErrorResponse } from '@angular/common/http';
import { Observable, throwError, BehaviorSubject } from 'rxjs';
import { catchError, filter, take, switchMap } from 'rxjs/operators';
import { TokenService } from '../services/token.service';
import { UserService } from '../services/user.service';
import { Router } from '@angular/router';

@Injectable()
export class TokenInterceptor implements HttpInterceptor {
    private isRefreshing = false;
    private refreshTokenSubject: BehaviorSubject<string | null> = new BehaviorSubject<string | null>(null);

    constructor(
        private tokenService: TokenService,
        private userService: UserService,
        private router: Router
    ) { }

    intercept(
        req: HttpRequest<any>,
        next: HttpHandler): Observable<HttpEvent<any>> {
        
        // Clone request and add access token + withCredentials
        let authReq = req;
        const token = this.tokenService.getToken();
        
        if (token) {
            authReq = req.clone({
                setHeaders: {
                    Authorization: `Bearer ${token}`
                },
                withCredentials: true // CRITICAL: Always send cookies
            });
        } else {
            // Even without token, enable credentials for endpoints like /auth/login
            authReq = req.clone({
                withCredentials: true
            });
        }

        return next.handle(authReq).pipe(
            catchError((error: HttpErrorResponse) => {
                // Handle 401 Unauthorized - Auto refresh token
                if (error.status === 401) {
                    // Skip refresh for login, signup, and refresh endpoints
                    if (this.shouldSkipRefresh(req.url)) {
                        console.error('❌ Authentication failed on auth endpoint:', req.url);
                        return throwError(() => error);
                    }
                    
                    console.warn('⚠️ 401 Unauthorized - Attempting token refresh...');
                    return this.handle401Error(authReq, next);
                }
                return throwError(() => error);
            })
        );
    }

    /**
     * Check if URL should skip automatic token refresh
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
     * Add access token to request header and enable credentials
     */
    private addTokenAndCredentials(request: HttpRequest<any>, token: string): HttpRequest<any> {
        return request.clone({
            setHeaders: {
                Authorization: `Bearer ${token}`
            },
            withCredentials: true
        });
    }

    /**
     * Handle 401 Unauthorized error - Auto refresh token
     */
    private handle401Error(request: HttpRequest<any>, next: HttpHandler): Observable<HttpEvent<any>> {
        if (!this.isRefreshing) {
            this.isRefreshing = true;
            this.refreshTokenSubject.next(null);

            console.log('🔄 Refreshing access token...');
            
            return this.userService.refreshToken().pipe(
                switchMap((response: any) => {
                    this.isRefreshing = false;
                    
                    // Extract new access token from response
                    const newToken = response?.data?.token;
                    if (newToken) {
                        console.log('✅ Token refreshed successfully');
                        this.tokenService.setToken(newToken);
                        this.refreshTokenSubject.next(newToken);
                        
                        // Retry the original request with new token
                        return next.handle(this.addTokenAndCredentials(request, newToken));
                    }
                    
                    // If no token in response, logout
                    console.error('❌ No token in refresh response');
                    this.handleRefreshFailure();
                    return throwError(() => new Error('Token refresh failed - no token in response'));
                }),
                catchError((err) => {
                    this.isRefreshing = false;
                    
                    // Refresh failed - clear tokens and redirect to login
                    console.error('❌ Token refresh failed:', err);
                    this.handleRefreshFailure();
                    return throwError(() => err);
                })
            );
        } else {
            // Another request is already refreshing the token
            // Wait for the new token, then retry this request
            console.log('⏳ Waiting for token refresh to complete...');
            
            return this.refreshTokenSubject.pipe(
                filter(token => token !== null),
                take(1),
                switchMap(token => {
                    console.log('✅ Using refreshed token for queued request');
                    return next.handle(this.addTokenAndCredentials(request, token!));
                })
            );
        }
    }

    /**
     * Handle refresh token failure - clear tokens and redirect to login
     */
    private handleRefreshFailure(): void {
        this.tokenService.removeToken();
        this.userService.removeUserFromLocalStorage();
        
        // Use Angular Router for better navigation
        this.router.navigate(['/login'], {
            queryParams: { sessionExpired: 'true' }
        });
    }
}