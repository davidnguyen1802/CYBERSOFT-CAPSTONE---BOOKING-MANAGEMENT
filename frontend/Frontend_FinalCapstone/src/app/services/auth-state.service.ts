import { Injectable } from '@angular/core';
import { BehaviorSubject, Observable } from 'rxjs';
import { CrossTabSyncService } from './cross-tab-sync.service';

@Injectable({
  providedIn: 'root'
})
export class AuthStateService {
  // Observable to track login state changes
  private loginStateSubject = new BehaviorSubject<boolean>(false);
  public loginState$: Observable<boolean> = this.loginStateSubject.asObservable();

  // Observable to track auth ready state (token saved and ready)
  private authReadySubject = new BehaviorSubject<boolean>(false);
  public authReady$: Observable<boolean> = this.authReadySubject.asObservable();

  // Observable to track token refresh state
  private isRefreshingSubject = new BehaviorSubject<boolean>(false);
  public isRefreshing$: Observable<boolean> = this.isRefreshingSubject.asObservable();

  // Observable for new token after refresh
  private refreshTokenSubject = new BehaviorSubject<string | null>(null);
  public refreshToken$: Observable<string | null> = this.refreshTokenSubject.asObservable();

  constructor(
    private crossTabSync: CrossTabSyncService
  ) {
    // Listen to logout events from other tabs
    this.crossTabSync.onLogoutFromOtherTab().subscribe(() => {
      console.log('=======================================');
      console.log('📡 CROSS-TAB LOGOUT detected');
      console.log('   Another tab logged out');
      console.log('   Clearing this tab\'s data...');
      console.log('=======================================');
      
      // Perform local cleanup WITHOUT broadcasting again (avoid infinite loop)
      this.performLocalLogout();
    });

    // Listen to login events from other tabs
    this.crossTabSync.onLoginFromOtherTab().subscribe(() => {
      console.log('=======================================');
      console.log('📡 CROSS-TAB LOGIN detected');
      console.log('   Another tab logged in');
      console.log('   Updating this tab\'s state...');
      console.log('=======================================');
      
      // Update login state WITHOUT broadcasting again (avoid infinite loop)
      this.loginStateSubject.next(true);
      this.authReadySubject.next(true);
      
      console.log('✅ Login state synced from other tab');
      console.log('ℹ️ User may need to refresh to see full profile');
    });
  }

  // Notify that user has logged in
  // @param broadcastToOtherTabs - Set to false for silent auth to prevent infinite reload loop
  notifyLogin(broadcastToOtherTabs: boolean = true): void {
    console.log('=======================================');
    console.log('🔐 AuthStateService.notifyLogin() CALLED');
    console.log('   Timestamp:', new Date().toISOString());
    console.log('   Previous state:', this.loginStateSubject.value);
    console.log('   Broadcast to tabs:', broadcastToOtherTabs);
    console.log('   Stack trace:', new Error().stack);
    console.log('=======================================');
    this.loginStateSubject.next(true);
    this.authReadySubject.next(true); // Token is ready
    console.log('✅ Login state set to TRUE');
    console.log('✅ Auth ready state set to TRUE');
    
    // Only broadcast if this is a real user login (not silent auth)
    if (broadcastToOtherTabs) {
      this.crossTabSync.broadcastLogin();
      console.log('📢 Login broadcasted to other tabs');
    } else {
      console.log('⏭️ Skipping broadcast (silent auth)');
    }
  }

  // Notify that user has logged out
  notifyLogout(): void {
    console.log('=======================================');
    console.log('🚪 AuthStateService.notifyLogout() CALLED');
    console.log('   Timestamp:', new Date().toISOString());
    console.log('   Previous state:', this.loginStateSubject.value);
    console.log('=======================================');
    
    // Broadcast logout to other tabs FIRST (before clearing local data)
    this.crossTabSync.broadcastLogout();
    console.log('📢 Logout broadcasted to other tabs');
    
    // Then clear this tab's data
    this.performLocalLogout();
  }

  /**
   * Perform local logout cleanup WITHOUT broadcasting
   * Used when receiving logout event from another tab
   */
  private performLocalLogout(): void {
    // Update auth state
    this.loginStateSubject.next(false);
    this.authReadySubject.next(false);
    this.isRefreshingSubject.next(false);
    this.refreshTokenSubject.next(null);
    
    // Clear ALL storage (complete cleanup on logout)
    console.log('🧹 Clearing ALL storage...');
    
    // Backup device_id before clearing (device_id should persist across logouts)
    const deviceId = localStorage.getItem('device_id');
    console.log('💾 Backing up device_id:', deviceId ? deviceId.substring(0, 8) + '...' : 'none');
    
    // Clear localStorage (tokens, remember preference, etc.)
    // NOTE: device_id will be restored after clearing
    const localKeys = Object.keys(localStorage);
    console.log(`📦 localStorage: ${localKeys.length} keys to clear`);
    localStorage.clear();
    
    // Restore device_id if it existed
    if (deviceId) {
      localStorage.setItem('device_id', deviceId);
      console.log('✅ device_id restored');
    }
    
    // Clear sessionStorage
    const sessionKeys = Object.keys(sessionStorage);
    console.log(`📦 sessionStorage: ${sessionKeys.length} keys to clear`);
    sessionStorage.clear();
    
    console.log('✅ Login state set to FALSE');
    console.log('✅ Auth ready state set to FALSE');
    console.log('✅ ALL storage cleared (localStorage + sessionStorage)');
    console.log('=======================================');
  }

  // Get current login state
  isLoggedIn(): boolean {
    return this.loginStateSubject.value;
  }

  // Check if auth is ready (token saved and ready for API calls)
  isAuthReady(): boolean {
    return this.authReadySubject.value;
  }

  // Wait for auth to be ready before making protected API calls
  waitForAuthReady(): Observable<boolean> {
    return this.authReady$;
  }

  // Set refresh state
  setRefreshing(isRefreshing: boolean): void {
    console.log(`🔄 Auth State: Refreshing = ${isRefreshing}`);
    this.isRefreshingSubject.next(isRefreshing);
  }

  // Alias methods for better readability
  startRefreshing(): void {
    this.setRefreshing(true);
  }

  stopRefreshing(): void {
    this.setRefreshing(false);
  }

  // Get refresh state
  isRefreshing(): boolean {
    return this.isRefreshingSubject.value;
  }

  // Notify new token after refresh
  notifyTokenRefreshed(token: string | null): void {
    console.log('🔄 Auth State: Token refreshed');
    this.refreshTokenSubject.next(token);
  }

  // Get refresh token subject
  getRefreshTokenSubject(): BehaviorSubject<string | null> {
    return this.refreshTokenSubject;
  }
}
