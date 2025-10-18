import { Injectable } from '@angular/core';
import { ActivatedRouteSnapshot, RouterStateSnapshot, UrlTree, CanActivateFn } from '@angular/router';
import { Router } from '@angular/router';
import { inject } from '@angular/core';
import { TokenService } from '../services/token.service';

@Injectable({
  providedIn: 'root'
})
export class AdminGuard {
  constructor(
    private tokenService: TokenService, 
    private router: Router
  ) {}

  canActivate(next: ActivatedRouteSnapshot, state: RouterStateSnapshot): boolean {
    const isTokenExpired = this.tokenService.isTokenExpired();
    const isUserIdValid = this.tokenService.getUserId() > 0;
    
    // Check if user has admin role from token
    const token = this.tokenService.getToken();
    let isAdmin = false;
    
    if (token) {
      try {
        // Decode token to get roles
        const tokenPayload = JSON.parse(atob(token.split('.')[1]));
        const roles = tokenPayload.roles || [];
        isAdmin = roles.includes('ROLE_ADMIN') || roles.includes('admin');
      } catch (e) {
        console.error('Error decoding token:', e);
      }
    }
    
    // debugger
    if (!isTokenExpired && isUserIdValid && isAdmin) {
      return true;
    } else {
      // Nếu không authenticated, bạn có thể redirect hoặc trả về một UrlTree khác.
      // Ví dụ trả về trang login:
      this.router.navigate(['/login']);
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
