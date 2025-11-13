import { Injectable } from '@angular/core';
import { JwtHelperService } from '@auth0/angular-jwt';
import { Observable } from 'rxjs';

@Injectable({
  providedIn: 'root',
})
export class TokenService {
    private readonly TOKEN_KEY = 'access_token';
    private readonly REMEMBER_KEY = 'remember'; // Flag to track remember me preference
    // Refresh token is now stored in HttpOnly cookie by backend, not in localStorage
    private jwtHelperService = new JwtHelperService();
    
    // JWT validation regex: must have 3 parts separated by dots (header.payload.signature)
    private readonly JWT_REGEX = /^[A-Za-z0-9-_]+\.[A-Za-z0-9-_]+\.[A-Za-z0-9-_]+$/;
    
    constructor(){}
    
    /**
     * Validate JWT format
     * @param token - Token to validate
     * @returns true if token matches JWT format (xxx.yyy.zzz)
     */
    private isValidJWT(token: string | null | undefined): boolean {
        if (!token || typeof token !== 'string') {
            return false;
        }
        return this.JWT_REGEX.test(token);
    }
    
    //getter/setter
    getToken(): string | null {
        // CRITICAL: Check BOTH localStorage (remember=ON) and sessionStorage (remember=OFF)
        let token = localStorage.getItem(this.TOKEN_KEY);
        let storageLocation = 'none';
        
        if (token) {
            storageLocation = 'localStorage';
        } else {
            token = sessionStorage.getItem(this.TOKEN_KEY);
            if (token) {
                storageLocation = 'sessionStorage';
            }
        }
        
        // Validate JWT format before returning
        if (!token) {
            // Only log once when no token (avoid spam)
            return null;
        }
        
        // Only log on first retrieval or when storage location changes
        // Removed excessive logging to improve performance
        
        if (!this.isValidJWT(token)) {
            console.error('❌ TokenService.getToken() - Invalid JWT format in storage!');
            console.error('   Token value:', token.substring(0, 20) + '...');
            console.error('   Clearing invalid token...');
            this.removeToken();
            return null;
        }
        
        return token;
    }
    
    /**
     * Get remember me preference from localStorage
     * Used by interceptor to determine storage location for refreshed token
     */
    getRememberMe(): boolean {
        const remember = localStorage.getItem(this.REMEMBER_KEY);
        return remember === '1';
    }

    /**
     * Set token to appropriate storage based on remember me preference
     * @param token - Access token to store (must be valid JWT)
     * @param remember - Optional: explicitly set storage location. If not provided, reads from localStorage flag.
     * 
     * If remember=true → localStorage (persistent across sessions)
     * If remember=false → sessionStorage (cleared when browser closes)
     * If remember=undefined → use existing REMEMBER_KEY flag
     * 
     * @throws Error if token is invalid (null/undefined/empty/not JWT format)
     */
    setToken(token: string, remember?: boolean): void {
        // Validate token before saving
        if (!token || typeof token !== 'string' || token.trim() === '') {
            const error = '❌ TokenService.setToken() - Cannot save null/empty token!';
            console.error(error);
            throw new Error(error);
        }
        
        if (!this.isValidJWT(token)) {
            const error = `❌ TokenService.setToken() - Invalid JWT format! Token: ${token.substring(0, 20)}...`;
            console.error(error);
            console.error('   Expected format: xxx.yyy.zzz (header.payload.signature)');
            throw new Error(error);
        }
        
        console.log('✅ TokenService.setToken() - Valid JWT format');
        console.log('   Token length:', token.length);
        console.log('   Token preview:', token.substring(0, 50) + '...');
        
        // Determine storage location
        let useLocalStorage: boolean;
        
        if (remember !== undefined) {
            // Explicit parameter provided - use it
            useLocalStorage = remember;
        } else {
            // Fallback to existing flag
            const rememberFlag = localStorage.getItem(this.REMEMBER_KEY);
            useLocalStorage = rememberFlag === '1';
        }
        
        if (useLocalStorage) {
            localStorage.setItem(this.TOKEN_KEY, token);
            console.log('✅ Token saved to localStorage (Remember Me = ON)');
        } else {
            sessionStorage.setItem(this.TOKEN_KEY, token);
            console.log('✅ Token saved to sessionStorage (Remember Me = OFF)');
        }
    }
    
    /**
     * Remove token from BOTH storages to ensure clean logout
     */
    removeToken(): void {
        localStorage.removeItem(this.TOKEN_KEY);
        sessionStorage.removeItem(this.TOKEN_KEY);
        localStorage.removeItem(this.REMEMBER_KEY);
        console.log('✅ Token removed from both localStorage and sessionStorage');
    }
    
    /**
     * Extract userId from JWT token payload
     * @returns userId or 0 if not found/invalid token
     */
    getUserId(): number {
        const token = this.getToken();
        
        // Return 0 if no token or invalid JWT (getToken already validated format)
        if (!token) {
            return 0;
        }
        
        // Double-check JWT format before decoding
        if (!this.isValidJWT(token)) {
            console.warn('⚠️ TokenService.getUserId() - Invalid JWT format, returning 0');
            return 0;
        }
        
        try {
            const userObject = this.jwtHelperService.decodeToken(token);
            const userId = 'userId' in userObject ? parseInt(userObject['userId']) : 0;
            return userId;
        } catch (error) {
            // Silently return 0 if decode fails (token format invalid)
            console.warn('⚠️ TokenService.getUserId() - Failed to decode token, returning 0');
            return 0;
        }
    }
    
    /**
     * Extract username from JWT token payload
     * @returns username or empty string if not found/invalid token
     */
    getUsername(): string {
        const token = this.getToken();
        
        if (!token || !this.isValidJWT(token)) {
            return '';
        }
        
        try {
            const payload = this.jwtHelperService.decodeToken(token);
            return payload?.username || '';
        } catch (error) {
            console.warn('⚠️ TokenService.getUsername() - Failed to decode token');
            return '';
        }
    }
    
    /**
     * Extract role from JWT token payload
     * @returns role (e.g., 'ADMIN', 'HOST', 'GUEST') or empty string if not found/invalid token
     */
    getRole(): string {
        const token = this.getToken();
        
        if (!token || !this.isValidJWT(token)) {
            return '';
        }
        
        try {
            const payload = this.jwtHelperService.decodeToken(token);
            return payload?.role || '';
        } catch (error) {
            console.warn('⚠️ TokenService.getRole() - Failed to decode token');
            return '';
        }
    }
    
    /**
     * Extract email from JWT token payload
     * @returns email or empty string if not found/invalid token
     */
    getEmail(): string {
        const token = this.getToken();
        
        if (!token || !this.isValidJWT(token)) {
            return '';
        }
        
        try {
            const payload = this.jwtHelperService.decodeToken(token);
            return payload?.email || '';
        } catch (error) {
            console.warn('⚠️ TokenService.getEmail() - Failed to decode token');
            return '';
        }
    }
    
    /**
     * Extract all basic user info from JWT token payload
     * @returns Object with userId, username, role, email or null if invalid token
     */
    getUserInfo(): { userId: number; username: string; role: string; email: string } | null {
        const token = this.getToken();
        
        if (!token || !this.isValidJWT(token)) {
            return null;
        }
        
        try {
            const payload = this.jwtHelperService.decodeToken(token);
            return {
                userId: payload?.userId ? parseInt(payload.userId) : 0,
                username: payload?.username || '',
                role: payload?.role || '',
                email: payload?.email || ''
            };
        } catch (error) {
            console.warn('⚠️ TokenService.getUserInfo() - Failed to decode token');
            return null;
        }
    }
    
    /**
     * Decode JWT token to get payload data
     * Returns null if token is invalid or doesn't match JWT format
     */
    decodeToken(token?: string): any {
        const tokenToUse = token || this.getToken();
        
        if (!tokenToUse) {
            return null;
        }
        
        // Validate JWT format before decoding
        if (!this.isValidJWT(tokenToUse)) {
            console.warn('⚠️ TokenService.decodeToken() - Invalid JWT format');
            return null;
        }
        
        try {
            return this.jwtHelperService.decodeToken(tokenToUse);
        } catch (error) {
            console.warn('⚠️ TokenService.decodeToken() - Failed to decode token');
            return null;
        }
    }
    
    isTokenExpired(): boolean { 
        const token = this.getToken();
        console.log('🔍 TokenService.isTokenExpired() called');
        if(!token || token === '') {
            console.log('   Result: TRUE (no token)');
            return true; // Changed: treat no token as expired
        }
        try {
            const expired = this.jwtHelperService.isTokenExpired(token);
            console.log('   Result:', expired);
            console.log('   Token preview:', token.substring(0, 50) + '...');
            return expired;
        } catch (error) {
            console.error('   Error checking token expiration:', error);
            return true; // Treat invalid token as expired
        }
    }
    
    /**
     * Check if user has valid token in storage
     * @returns true if token exists and is valid JWT format (doesn't check expiration)
     */
    hasValidToken(): boolean {
        const token = this.getToken();
        return !!token && this.isValidJWT(token);
    }
}