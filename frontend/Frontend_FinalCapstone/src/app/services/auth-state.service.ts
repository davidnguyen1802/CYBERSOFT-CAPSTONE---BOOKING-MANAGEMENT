import { Injectable } from '@angular/core';
import { BehaviorSubject, Observable } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class AuthStateService {
  // Observable to track login state changes
  private loginStateSubject = new BehaviorSubject<boolean>(false);
  public loginState$: Observable<boolean> = this.loginStateSubject.asObservable();

  // Observable to track token refresh state
  private isRefreshingSubject = new BehaviorSubject<boolean>(false);
  public isRefreshing$: Observable<boolean> = this.isRefreshingSubject.asObservable();

  // Observable for new token after refresh
  private refreshTokenSubject = new BehaviorSubject<string | null>(null);
  public refreshToken$: Observable<string | null> = this.refreshTokenSubject.asObservable();

  constructor() { }

  // Notify that user has logged in
  notifyLogin(): void {
    console.log('🔐 Auth State: User logged in');
    this.loginStateSubject.next(true);
  }

  // Notify that user has logged out
  notifyLogout(): void {
    console.log('🔐 Auth State: User logged out');
    this.loginStateSubject.next(false);
    this.isRefreshingSubject.next(false);
    this.refreshTokenSubject.next(null);
  }

  // Get current login state
  isLoggedIn(): boolean {
    return this.loginStateSubject.value;
  }

  // Set refresh state
  setRefreshing(isRefreshing: boolean): void {
    console.log(`🔄 Auth State: Refreshing = ${isRefreshing}`);
    this.isRefreshingSubject.next(isRefreshing);
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
