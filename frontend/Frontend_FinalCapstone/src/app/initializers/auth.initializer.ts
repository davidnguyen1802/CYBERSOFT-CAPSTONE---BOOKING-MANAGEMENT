import { APP_INITIALIZER, Provider } from '@angular/core';
import { AuthService } from '../services/auth.service';

/**
 * NOTE: App initializer factory for auth bootstrap
 * - Runs before app fully initializes
 * - Checks if user has remember=1 flag
 * - If yes and no valid AT, attempts silent refresh
 * - This enables "remember me" to work across browser restarts
 */
export function initializeAuth(authService: AuthService): () => Promise<boolean> {
  return () => {
    return new Promise((resolve) => {
      console.log('🚀 App initializing - checking auth state...');
      
      authService.initializeAuth().subscribe({
        next: (isAuthenticated) => {
          if (isAuthenticated) {
            console.log('✅ User authenticated on app init');
          } else {
            console.log('ℹ️ User not authenticated on app init');
          }
          resolve(true);
        },
        error: (error) => {
          console.error('❌ Auth initialization failed:', error);
          resolve(true); // Resolve anyway to not block app startup
        }
      });
    });
  };
}

/**
 * NOTE: Provider for APP_INITIALIZER
 * Add this to app.module.ts providers array
 */
export const authInitializerProvider: Provider = {
  provide: APP_INITIALIZER,
  useFactory: initializeAuth,
  deps: [AuthService],
  multi: true
};
