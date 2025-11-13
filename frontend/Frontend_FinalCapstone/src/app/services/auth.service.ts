import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable, throwError, timer, Subscription } from 'rxjs';
import { tap, catchError } from 'rxjs/operators';
import { Router } from '@angular/router';
import { getBaseUrl } from '../utils/url.util';
import { LoginDTO } from '../dtos/user/login.dto';
import { JwtHelperService } from '@auth0/angular-jwt';

/**
 * NOTE: Centralized Auth Service following AT/RT best practices
 * - Access Token (AT) stored in localStorage (remember=ON) or sessionStorage (remember=OFF)
 * - Refresh Token (RT) stored in HttpOnly cookie (managed by backend)
 * - Silent refresh triggered ~60s before AT expiry
 * - CSRF protection via X-CSRF-Check header on refresh/logout
 */

interface AuthResponse {
  message: string;
  status: string;
  data: {
    token: string;
    refresh_token: string; // Not used by FE (HttpOnly cookie)
    tokenType: string;
    id: number;
    username: string;
    role: string;
    expiresAt?: number; // Epoch milliseconds
  };
}

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private readonly TOKEN_KEY = 'access_token';
  private readonly EXPIRES_KEY = 'access_expires';
  private readonly REMEMBER_KEY = 'remember';
  private readonly REFRESH_BUFFER_MS = 60 * 1000; // Refresh 60s before expiry
  
  private baseUrl = getBaseUrl();
  private apiLogin = `${this.baseUrl}/auth/login`;
  private apiRefresh = `${this.baseUrl}/auth/refresh`;
  private apiLogout = `${this.baseUrl}/auth/logout`;
  
  private jwtHelper = new JwtHelperService();
  private refreshTimerSub?: Subscription;

  constructor(
    private http: HttpClient,
    private router: Router
  ) {}

  /**
   * NOTE: Get appropriate storage based on remember flag
   * - remember=1 → localStorage (persistent across browser sessions)
   * - remember=0 or missing → sessionStorage (cleared when browser closes)
   */
  private getStorage(): Storage {
    try {
      const remember = localStorage.getItem(this.REMEMBER_KEY);
      return remember === '1' ? localStorage : sessionStorage;
    } catch (e) {
      console.error('⚠️ Storage access error (Safari private mode?):', e);
      // Fallback to in-memory if storage is unavailable
      return sessionStorage;
    }
  }

  /**
   * NOTE: Login with rememberMe support
   * @param loginDTO - email/phone, password
   * @param rememberMe - if true, stores AT in localStorage; if false, uses sessionStorage
   */
  login(loginDTO: LoginDTO, rememberMe: boolean): Observable<AuthResponse> {
    console.log('🔐 AuthService.login() called', { rememberMe });
    
    // NOTE: Attach rememberMe to request body (backend uses this for RT cookie MaxAge)
    const payload = {
      ...loginDTO,
      rememberMe
    };
    
    return this.http.post<AuthResponse>(this.apiLogin, payload, {
      withCredentials: true, // NOTE: Required to receive HttpOnly RT cookie
      headers: new HttpHeaders({ 'Content-Type': 'application/json' })
    }).pipe(
      tap(response => {
        if (response?.data?.token) {
          this.storeAuthData(response.data, rememberMe);
        }
      }),
      catchError(error => {
        console.error('❌ Login failed:', error);
        return throwError(() => error);
      })
    );
  }

  /**
   * NOTE: Store auth data after successful login/refresh
   * - Saves AT + expiresAt to appropriate storage
   * - Sets remember flag if rememberMe=true
   * - Schedules silent refresh timer
   */
  private storeAuthData(authData: AuthResponse['data'], rememberMe: boolean): void {
    try {
      const token = authData.token;
      const expiresAt = authData.expiresAt || this.calculateExpiresAt(token);
      
      // NOTE: Set remember flag FIRST to determine storage location
      if (rememberMe) {
        localStorage.setItem(this.REMEMBER_KEY, '1');
      } else {
        localStorage.removeItem(this.REMEMBER_KEY);
      }
      
      const storage = this.getStorage();
      storage.setItem(this.TOKEN_KEY, token);
      storage.setItem(this.EXPIRES_KEY, expiresAt.toString());
      
      console.log('✅ Auth data stored:', {
        storage: rememberMe ? 'localStorage' : 'sessionStorage',
        expiresAt: new Date(expiresAt).toISOString()
      });
      
      // Schedule silent refresh
      this.scheduleSilentRefresh(expiresAt);
    } catch (e) {
      console.error('❌ Failed to store auth data:', e);
    }
  }

  /**
   * NOTE: Calculate token expiry from JWT payload
   * Falls back to conservative 15min if 'exp' claim missing
   */
  private calculateExpiresAt(token: string): number {
    try {
      const decoded = this.jwtHelper.decodeToken(token);
      if (decoded?.exp) {
        return decoded.exp * 1000; // Convert seconds to milliseconds
      }
    } catch (e) {
      console.error('⚠️ Failed to decode token:', e);
    }
    // Fallback: 15 minutes from now
    return Date.now() + (15 * 60 * 1000);
  }

  /**
   * NOTE: Schedule silent refresh ~60s before token expiry
   * - Clears existing timer before scheduling new one
   * - Only schedules if expiry is in the future
   */
  private scheduleSilentRefresh(expiresAt: number): void {
    // Clear existing timer
    this.clearRefreshTimer();
    
    const now = Date.now();
    const timeUntilRefresh = expiresAt - now - this.REFRESH_BUFFER_MS;
    
    if (timeUntilRefresh <= 0) {
      console.warn('⚠️ Token already expired or expiring soon, refresh immediately');
      this.refresh().subscribe();
      return;
    }
    
    console.log(`⏰ Silent refresh scheduled in ${Math.round(timeUntilRefresh / 1000)}s`);
    
    this.refreshTimerSub = timer(timeUntilRefresh).subscribe(() => {
      console.log('🔄 Triggering silent refresh...');
      this.refresh().subscribe({
        next: () => console.log('✅ Silent refresh successful'),
        error: (err) => {
          console.error('❌ Silent refresh failed:', err);
          this.logout();
        }
      });
    });
  }

  /**
   * NOTE: Clear refresh timer (called on logout or before rescheduling)
   */
  private clearRefreshTimer(): void {
    if (this.refreshTimerSub) {
      this.refreshTimerSub.unsubscribe();
      this.refreshTimerSub = undefined;
    }
  }

  /**
   * NOTE: Refresh access token using HttpOnly RT cookie
   * CRITICAL: Must send X-CSRF-Check header for CSRF protection
   * - Updates AT + expiresAt in storage
   * - Reschedules next silent refresh
   * - Returns observable for interceptor/manual calls
   */
  refresh(): Observable<AuthResponse> {
    console.log('🔄 AuthService.refresh() called');
    
    return this.http.post<AuthResponse>(this.apiRefresh, {}, {
      withCredentials: true, // NOTE: Required to send HttpOnly RT cookie
      headers: new HttpHeaders({
        'Content-Type': 'application/json',
        'X-CSRF-Check': '1' // NOTE: CSRF protection header
      })
    }).pipe(
      tap(response => {
        if (response?.data?.token) {
          // NOTE: Preserve remember flag across refresh
          const rememberMe = localStorage.getItem(this.REMEMBER_KEY) === '1';
          this.storeAuthData(response.data, rememberMe);
        }
      }),
      catchError(error => {
        console.error('❌ Refresh failed:', error);
        // NOTE: If refresh fails, logout to clear invalid state
        if (error.status === 401 || error.status === 403) {
          this.logout();
        }
        return throwError(() => error);
      })
    );
  }

  /**
   * NOTE: Logout - clears all auth data and HttpOnly cookie
   * - Calls backend /auth/logout to clear RT cookie
   * - Removes AT + expires from both storages
   * - Clears remember flag
   * - Cancels refresh timer
   * - Redirects to login page
   */
  logout(): void {
    console.log('🚪 AuthService.logout() called');
    
    // Call backend to clear HttpOnly cookie
    this.http.post(this.apiLogout, {}, {
      withCredentials: true,
      headers: new HttpHeaders({
        'Content-Type': 'application/json',
        'X-CSRF-Check': '1' // NOTE: CSRF protection
      })
    }).subscribe({
      next: () => console.log('✅ Backend logout successful'),
      error: (err) => console.warn('⚠️ Backend logout failed:', err)
    });
    
    // Clear all local state
    this.clearAuthData();
    
    // Redirect to login
    this.router.navigate(['/login']);
  }

  /**
   * NOTE: Clear all auth data from storages
   * - Clears both localStorage and sessionStorage
   * - Cancels refresh timer
   */
  private clearAuthData(): void {
    try {
      // Clear from both storages to be safe
      [localStorage, sessionStorage].forEach(storage => {
        storage.removeItem(this.TOKEN_KEY);
        storage.removeItem(this.EXPIRES_KEY);
      });
      localStorage.removeItem(this.REMEMBER_KEY);
      
      this.clearRefreshTimer();
      
      console.log('✅ All auth data cleared');
    } catch (e) {
      console.error('❌ Failed to clear auth data:', e);
    }
  }

  /**
   * NOTE: Get current access token from appropriate storage
   */
  getToken(): string | null {
    try {
      const storage = this.getStorage();
      return storage.getItem(this.TOKEN_KEY);
    } catch (e) {
      console.error('❌ Failed to get token:', e);
      return null;
    }
  }

  /**
   * NOTE: Check if token is expired or about to expire
   */
  isTokenExpired(): boolean {
    const token = this.getToken();
    if (!token) return true;
    
    try {
      return this.jwtHelper.isTokenExpired(token);
    } catch (e) {
      console.error('❌ Failed to check token expiry:', e);
      return true;
    }
  }

  /**
   * NOTE: Check if user is logged in (has valid token)
   */
  isLoggedIn(): boolean {
    return !!this.getToken() && !this.isTokenExpired();
  }

  /**
   * NOTE: Bootstrap logic - called on app init
   * - If remember=1 and no valid AT, attempts silent refresh
   * - This handles case where user returns after browser restart
   */
  initializeAuth(): Observable<boolean> {
    console.log('🚀 AuthService.initializeAuth() called');
    
    const rememberMe = localStorage.getItem(this.REMEMBER_KEY) === '1';
    const hasValidToken = this.isLoggedIn();
    
    if (rememberMe && !hasValidToken) {
      console.log('🔄 Remember=ON but no valid token, attempting refresh...');
      return new Observable(observer => {
        this.refresh().subscribe({
          next: () => {
            observer.next(true);
            observer.complete();
          },
          error: () => {
            observer.next(false);
            observer.complete();
          }
        });
      });
    }
    
    if (hasValidToken) {
      // Schedule refresh for existing token
      const storage = this.getStorage();
      const expiresAt = parseInt(storage.getItem(this.EXPIRES_KEY) || '0');
      if (expiresAt > Date.now()) {
        this.scheduleSilentRefresh(expiresAt);
      }
      return new Observable(observer => {
        observer.next(true);
        observer.complete();
      });
    }
    
    return new Observable(observer => {
      observer.next(false);
      observer.complete();
    });
  }
}
