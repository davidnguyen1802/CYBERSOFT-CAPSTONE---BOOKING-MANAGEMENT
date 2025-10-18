import { Injectable } from '@angular/core';
import { JwtHelperService } from '@auth0/angular-jwt';

@Injectable({
  providedIn: 'root',
})
export class TokenService {
    private readonly TOKEN_KEY = 'access_token';
    // Refresh token is now stored in HttpOnly cookie by backend, not in localStorage
    private jwtHelperService = new JwtHelperService();
    constructor(){}
    //getter/setter
    getToken():string {
        const token = localStorage.getItem(this.TOKEN_KEY) ?? '';
        // Only log if token exists to avoid spam
        return token;
    }
    setToken(token: string): void {
        console.log('🔑 Token saved to localStorage');
        localStorage.setItem(this.TOKEN_KEY, token);             
    }
    
    getUserId(): number {
        let token = this.getToken();
        if (!token) {
            console.log('🔑 No token found, cannot get user ID');
            return 0;
        }
        try {
            let userObject = this.jwtHelperService.decodeToken(token);
            const userId = 'userId' in userObject ? parseInt(userObject['userId']) : 0;
            console.log(`🔑 User ID from token: ${userId}`);
            return userId;
        } catch (error) {
            console.error('🔑 Error decoding token:', error);
            return 0;
        }
    }
    
      
    removeToken(): void {
        console.log('🔑 Tokens removed from localStorage');
        localStorage.removeItem(this.TOKEN_KEY);
        // Note: HttpOnly cookie (refresh_token) will be cleared by calling /auth/logout endpoint
    }              
    isTokenExpired(): boolean { 
        const token = this.getToken();
        if(!token || token === '') {
            console.log('🔑 No token to check expiration');
            return true; // Changed: treat no token as expired
        }
        try {
            const expired = this.jwtHelperService.isTokenExpired(token);
            console.log(`🔑 Token expired check: ${expired}`);
            return expired;
        } catch (error) {
            console.error('🔑 Error checking token expiration:', error);
            return true; // Treat invalid token as expired
        }
    }
}
