import { Injectable } from '@angular/core';
import { Observable, of, throwError } from 'rxjs';
import { catchError, map, tap } from 'rxjs/operators';
import { TokenService } from './token.service';
import { UserService } from './user.service';
import { AuthStateService } from './auth-state.service';

@Injectable({
  providedIn: 'root'
})
export class SilentAuthService {
  private isAttemptingRefresh = false; // Prevent concurrent refresh attempts

  constructor(
    private tokenService: TokenService,
    private userService: UserService,
    private authStateService: AuthStateService
  ) {}

  /**
   * SILENT AUTHENTICATION FLOW:
   * 1. Check if AT exists in storage → if yes, return true (already authenticated)
   * 2. If no AT → Check if RT cookie might exist (check remember preference)
   * 3. If no indication of RT → Return false immediately (anonymous mode)
   * 4. If RT might exist → Try to refresh from RT cookie
   * 5. If refresh succeeds → Save new AT, return true
   * 6. If refresh fails → User is anonymous, return false
   * 
   * @returns Observable<boolean> - true if authenticated, false if anonymous
   */
  attemptSilentLogin(): Observable<boolean> {
    console.log('=======================================');
    console.log('🔐 SILENT AUTH - Attempting silent login...');
    console.log('   Timestamp:', new Date().toISOString());
    console.log('=======================================');

    // Step 1: Check if AT already exists in storage
    const hasToken = this.tokenService.hasValidToken();
    
    if (hasToken) {
      console.log('✅ Access Token found in storage - already authenticated');
      console.log('   Skipping refresh attempt');
      
      // IMPORTANT: Notify auth state even if token already exists
      // This ensures authReady$ emits true and UI updates correctly
      // Pass false to prevent broadcasting (avoid infinite reload loop)
      this.authStateService.notifyLogin(false);
      console.log('✅ Auth state notified - user is authenticated');
      
      return of(true);
    }

    console.log('❌ No Access Token in storage');
    
    // Step 2: Check if user was ever logged in (has remember preference)
    const rememberMe = this.tokenService.getRememberMe();
    
    console.log('   Checking login indicator:');
    console.log('   - Remember preference:', rememberMe);
    
    // If NO remember preference → User never logged in or has logged out → Skip refresh
    if (!rememberMe) {
      console.log('ℹ️ No remember preference found');
      console.log('   → User never logged in or has logged out');
      console.log('   → ANONYMOUS MODE - Skipping refresh attempt');
      console.log('   → User can browse public pages without login');
      console.log('   → No protected API calls will be made');
      console.log('=======================================');
      return of(false);
    }
    
    console.log('🔄 Remember preference found - attempting refresh...');
    console.log('   Will try to restore session from HttpOnly RT cookie');

    // Step 3: Prevent concurrent refresh attempts
    if (this.isAttemptingRefresh) {
      console.log('⏳ Refresh already in progress - waiting...');
      return of(false);
    }

    this.isAttemptingRefresh = true;

    // Step 3: Try to refresh token from RT cookie
    return this.userService.refreshToken().pipe(
      tap((response: any) => {
        console.log('✅ Silent refresh successful!');
        console.log('   Response:', response);
      }),
      map((response: any) => {
        // ⚠️ CRITICAL: Backend returns JWT string directly in response.data
        // Same format as login/signup: { status: "OK", message: "...", data: "JWT_STRING" }
        const newToken = response.data;

        if (!newToken || typeof newToken !== 'string') {
          console.error('❌ Invalid token in refresh response');
          console.error('   Expected: response.data = "JWT_STRING"');
          console.error('   Received:', response);
          throw new Error('Invalid token format from refresh endpoint');
        }

        console.log('✅ New Access Token extracted from response');
        console.log('   Token length:', newToken.length);

        // Step 4: Save token to appropriate storage
        // Check if user had "remember me" preference
        const rememberMe = this.tokenService.getRememberMe();
        console.log('💾 Saving token with RememberMe =', rememberMe);
        
        // If no preference found, default to true (assume user wanted to stay logged in)
        const shouldRemember = rememberMe !== false; // true if undefined or true
        console.log('   Effective RememberMe:', shouldRemember);

        try {
          this.tokenService.setToken(newToken, shouldRemember);
          console.log('✅ Token saved to storage successfully');
          
          // Also save the remember preference for future refreshes
          if (shouldRemember) {
            localStorage.setItem('remember', '1');
          }
          
          // Step 5: Notify auth state that user is authenticated
          // Pass false to prevent broadcasting (silent refresh, not user login)
          this.authStateService.notifyLogin(false);
          console.log('✅ Auth state notified - user is now authenticated');

          this.isAttemptingRefresh = false;
          return true; // Successfully authenticated

        } catch (error: any) {
          console.error('❌ Failed to save token:', error.message);
          this.isAttemptingRefresh = false;
          throw error;
        }
      }),
      catchError((error) => {
        console.log('❌ Silent refresh failed');
        console.log('   Error:', error.status || error.message);
        
        // Analyze error
        if (error.status === 449) {
          console.log('   Reason: 449 - No RT cookie found (user never logged in or already logged out)');
        } else if (error.status === 419) {
          console.log('   Reason: 419 - RT expired (session timed out)');
        } else if (error.status === 498) {
          console.log('   Reason: 498 - RT invalid (corrupted cookie)');
        } else if (error.status === 499) {
          console.log('   Reason: 499 - RT revoked (user logged out from another device)');
        } else {
          console.log('   Reason: Network or server error');
        }

        console.log('ℹ️ User will browse as ANONYMOUS (no login required)');
        this.isAttemptingRefresh = false;
        
        // Return false instead of throwing error (anonymous mode is valid)
        return of(false);
      })
    );
  }

  /**
   * Quick check if user is authenticated (synchronous)
   * Only checks if AT exists in storage, doesn't validate or refresh
   */
  isAuthenticated(): boolean {
    return this.tokenService.hasValidToken();
  }
}
