import { Injectable } from '@angular/core';
import { Subject } from 'rxjs';

/**
 * Cross-Tab Synchronization Service
 * 
 * Use BroadcastChannel API to sync logout/login events across browser tabs
 * 
 * Scenarios:
 * - User logs out in Tab A → Tab B automatically logs out
 * - User logs in in Tab A → Tab B automatically updates UI
 * 
 * Browser Support:
 * - Chrome 54+, Firefox 38+, Edge 79+
 * - Safari 15.4+ (April 2022)
 * - For older browsers, falls back to localStorage events (slower but works)
 */
@Injectable({
  providedIn: 'root'
})
export class CrossTabSyncService {
  private readonly CHANNEL_NAME = 'web_auth_sync';
  private readonly STORAGE_KEY = 'web_event';
  
  private channel: BroadcastChannel | null = null;
  private logoutEvent$ = new Subject<void>();
  private loginEvent$ = new Subject<void>();

  constructor() {
    this.initializeSync();
  }

  /**
   * Initialize cross-tab sync using BroadcastChannel or localStorage fallback
   */
  private initializeSync(): void {
    // Try BroadcastChannel first (modern browsers)
    if ('BroadcastChannel' in window) {
      console.log('✅ Using BroadcastChannel for cross-tab sync');
      this.initBroadcastChannel();
    } else {
      console.warn('⚠️ BroadcastChannel not supported, using localStorage fallback');
      this.initLocalStorageSync();
    }
  }

  /**
   * Initialize BroadcastChannel for modern browsers
   */
  private initBroadcastChannel(): void {
    try {
      this.channel = new BroadcastChannel(this.CHANNEL_NAME);
      
      this.channel.onmessage = (event) => {
        console.log('📡 Received cross-tab message:', event.data);
        
        if (event.data.type === 'LOGOUT') {
          console.log('🚪 Other tab logged out → Triggering logout in this tab');
          this.logoutEvent$.next();
        } else if (event.data.type === 'LOGIN') {
          console.log('🔐 Other tab logged in → Triggering login in this tab');
          this.loginEvent$.next();
        }
      };

      this.channel.onmessageerror = (error) => {
        console.error('❌ BroadcastChannel error:', error);
      };
      
      console.log('✅ BroadcastChannel initialized');
    } catch (error) {
      console.error('❌ Failed to initialize BroadcastChannel:', error);
      this.initLocalStorageSync();
    }
  }

  /**
   * Initialize localStorage event listener as fallback
   * Works in older browsers but has slight delay
   */
  private initLocalStorageSync(): void {
    window.addEventListener('storage', (event) => {
      // Only listen to our specific key
      if (event.key !== this.STORAGE_KEY) return;
      
      // Only trigger if value actually changed
      if (!event.newValue) return;
      
      try {
        const data = JSON.parse(event.newValue);
        console.log('📡 Received localStorage event:', data);
        
        if (data.type === 'LOGOUT') {
          console.log('🚪 Other tab logged out → Triggering logout in this tab');
          this.logoutEvent$.next();
          
          // Clean up the storage key
          localStorage.removeItem(this.STORAGE_KEY);
        } else if (data.type === 'LOGIN') {
          console.log('🔐 Other tab logged in → Triggering login in this tab');
          this.loginEvent$.next();
          
          // Clean up the storage key
          localStorage.removeItem(this.STORAGE_KEY);
        }
      } catch (error) {
        console.error('❌ Failed to parse storage event:', error);
      }
    });
    
    console.log('✅ localStorage sync initialized');
  }

  /**
   * Broadcast logout event to all other tabs
   * This tab will clear its own data separately
   */
  broadcastLogout(): void {
    console.log('📢 Broadcasting LOGOUT to other tabs...');
    
    const message = { type: 'LOGOUT', timestamp: Date.now() };
    
    if (this.channel) {
      // Use BroadcastChannel if available
      this.channel.postMessage(message);
      console.log('✅ Logout broadcasted via BroadcastChannel');
    } else {
      // Fallback to localStorage
      localStorage.setItem(this.STORAGE_KEY, JSON.stringify(message));
      console.log('✅ Logout broadcasted via localStorage');
      
      // Clean up after short delay to allow other tabs to read
      setTimeout(() => {
        localStorage.removeItem(this.STORAGE_KEY);
      }, 100);
    }
  }

  /**
   * Broadcast login event to all other tabs
   */
  broadcastLogin(): void {
    console.log('📢 Broadcasting LOGIN to other tabs...');
    
    const message = { type: 'LOGIN', timestamp: Date.now() };
    
    if (this.channel) {
      this.channel.postMessage(message);
      console.log('✅ Login broadcasted via BroadcastChannel');
    } else {
      localStorage.setItem(this.STORAGE_KEY, JSON.stringify(message));
      console.log('✅ Login broadcasted via localStorage');
      
      setTimeout(() => {
        localStorage.removeItem(this.STORAGE_KEY);
      }, 100);
    }
  }

  /**
   * Subscribe to logout events from other tabs
   * @returns Observable that emits when another tab logs out
   */
  onLogoutFromOtherTab() {
    return this.logoutEvent$.asObservable();
  }

  /**
   * Subscribe to login events from other tabs
   * @returns Observable that emits when another tab logs in
   */
  onLoginFromOtherTab() {
    return this.loginEvent$.asObservable();
  }

  /**
   * Clean up resources
   */
  destroy(): void {
    if (this.channel) {
      this.channel.close();
      console.log('✅ BroadcastChannel closed');
    }
    this.logoutEvent$.complete();
    this.loginEvent$.complete();
  }
}
