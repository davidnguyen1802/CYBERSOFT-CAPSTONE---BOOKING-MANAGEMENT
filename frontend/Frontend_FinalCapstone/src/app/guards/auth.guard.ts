import { Injectable } from '@angular/core';
import { 
  ActivatedRouteSnapshot, 
  RouterStateSnapshot, 
  CanActivateFn 
} from '@angular/router';
import { TokenService } from '../services/token.service';
import { SimpleModalService } from '../services/simple-modal.service';
import { Router } from '@angular/router';
import { inject } from '@angular/core';

/**
 * AuthGuard - Protect routes that require authentication
 * 
 * Behavior:
 * - If user has token → allow access
 * - If user is anonymous (no token) → show "Login Required" modal
 *   Modal will redirect to /login when user clicks button
 */

@Injectable({
  providedIn: 'root'
})
export class AuthGuard {  
  constructor(
    private tokenService: TokenService,
    private modalService: SimpleModalService,
    private router: Router,    
  ) {}

  canActivate(next: ActivatedRouteSnapshot, state: RouterStateSnapshot): boolean {
    const token = this.tokenService.getToken();
    
    if (token) {
      // User has token - allow access
      // Note: If token is invalid, backend will return 401 and interceptor will handle refresh
      return true;
    } else {
      // Anonymous user trying to access protected route
      console.warn('⚠️ AuthGuard: Anonymous user blocked from protected route');
      console.warn('   Target URL:', state.url);
      
      // Show login required modal
      this.modalService.showLoginRequired();
      
      // Block navigation
      return false;
    }
  }
}

// Sử dụng functional guard như sau:
export const AuthGuardFn: CanActivateFn = (next: ActivatedRouteSnapshot, state: RouterStateSnapshot): boolean => {
  // debugger
  return inject(AuthGuard).canActivate(next, state);
}
