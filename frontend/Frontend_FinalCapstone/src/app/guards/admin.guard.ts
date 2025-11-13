import { Injectable } from '@angular/core';
import { ActivatedRouteSnapshot, RouterStateSnapshot, UrlTree, CanActivateFn } from '@angular/router';
import { Router } from '@angular/router';
import { inject } from '@angular/core';
import { AuthService } from '../services/auth.service';

/**
 * NOTE: Updated AdminGuard to use new AuthService
 * - Checks if user is logged in AND has admin role
 * - Redirects to login if not authenticated or not admin
 */

@Injectable({
  providedIn: 'root'
})
export class AdminGuard {
  constructor(
    private authService: AuthService, 
    private router: Router
  ) {}

  canActivate(next: ActivatedRouteSnapshot, state: RouterStateSnapshot): boolean {
    // Check if user is logged in
    if (!this.authService.isLoggedIn()) {
      console.warn('⚠️ AdminGuard: User not authenticated');
      this.router.navigate(['/login']);
      return false;
    }
    
    // Check if user has admin role from token
    const token = this.authService.getToken();
    let isAdmin = false;
    
    if (token) {
      try {
        // NOTE: Decode token to get role (single role, not array)
        const tokenPayload = JSON.parse(atob(token.split('.')[1]));
        const role = tokenPayload.role || '';
        isAdmin = role === 'ROLE_ADMIN' || role === 'admin';
      } catch (e) {
        console.error('❌ Error decoding token:', e);
      }
    }
    
    if (isAdmin) {
      return true;
    } else {
      console.warn('⚠️ AdminGuard: User is not admin');
      this.router.navigate(['/']);
      return false;
    }
  }  
}

export const AdminGuardFn: CanActivateFn = (
  next: ActivatedRouteSnapshot, 
  state: RouterStateSnapshot
): boolean => {
  // debugger
  return inject(AdminGuard).canActivate(next, state);
}
