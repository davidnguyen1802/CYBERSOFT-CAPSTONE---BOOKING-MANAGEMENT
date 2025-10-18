import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { TokenService } from '../../services/token.service';
import { UserService } from '../../services/user.service';
import { CartService } from '../../services/cart.service';
import { getBaseUrl } from '../../utils/url.util';
import { AuthStateService } from '../../services/auth-state.service';

@Component({
  selector: 'app-auth-callback',
  templateUrl: './auth-callback.component.html',
  styleUrls: ['./auth-callback.component.scss']
})
export class AuthCallbackComponent implements OnInit {
  loading: boolean = true;
  errorMessage: string = '';

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private http: HttpClient,
    private tokenService: TokenService,
    private userService: UserService,
    private cartService: CartService,
    private authStateService: AuthStateService,
  ) { }

  ngOnInit(): void {
    // Get the query parameters from the URL
    this.route.queryParams.subscribe(params => {
      const token = params['token'];
      const refreshToken = params['refresh_token'];
      const id = params['id'];
      const username = params['username'];
      const roles = params['roles'];
      const error = params['error'];

      console.log('OAuth callback params:', { token, refreshToken, id, username, roles, error });

      if (error) {
        this.errorMessage = `Authentication failed: ${error}`;
        this.loading = false;
        setTimeout(() => {
          this.router.navigate(['/login']);
        }, 3000);
        return;
      }

      if (token && id) {
        // Process the tokens and user data directly from URL parameters
        this.processOAuthTokens(token, refreshToken, id, username, roles);
      } else {
        this.errorMessage = 'Invalid callback parameters';
        this.loading = false;
        setTimeout(() => {
          this.router.navigate(['/login']);
        }, 3000);
      }
    });
  }

  private processOAuthTokens(token: string, refreshToken: string, id: string, username: string, roles: string): void {
    console.log('Processing OAuth tokens...');

    // Save access token to localStorage
    // Refresh token is automatically stored in HttpOnly cookie by backend
    this.tokenService.setToken(token);

    console.log('Token saved to localStorage');

    // Notify that user has logged in
    this.authStateService.notifyLogin();

    // Refresh cart (cart service will use token to identify user)
    this.cartService.refreshCart();

    this.loading = false;

    // Redirect to user-profile to fetch and display user data from API
    console.log('Redirecting to /user-profile');
    this.router.navigate(['/user-profile']);
  }

  private exchangeCodeForTokens(loginType: string, code: string): void {
    const baseUrl = getBaseUrl();
    const callbackUrl = `${baseUrl}/auth/social/callback?state=${loginType}&code=${encodeURIComponent(code)}`;
    
    console.log('Calling backend callback URL:', callbackUrl);

    this.http.get<any>(callbackUrl, { 
      withCredentials: true // Enable cookies for refresh token
    }).subscribe({
      next: (response) => {
        console.log('OAuth callback response:', response);

        if (response && response.data) {
          const authData = response.data;
          const token = authData.token;

          // Save access token to localStorage
          // Refresh token is automatically stored in HttpOnly cookie by backend
          this.tokenService.setToken(token);

          console.log('Token saved to localStorage');

          // Notify that user has logged in
          this.authStateService.notifyLogin();

          // Refresh cart (cart service will use token to identify user)
          this.cartService.refreshCart();

          this.loading = false;

          // Redirect to user-profile to fetch and display user data from API
          console.log('Redirecting to /user-profile');
          this.router.navigate(['/user-profile']);
        } else {
          this.errorMessage = 'Invalid response from server';
          this.loading = false;
          setTimeout(() => {
            this.router.navigate(['/login']);
          }, 3000);
        }
      },
      error: (error) => {
        console.error('OAuth callback error:', error);
        this.errorMessage = error?.error?.message || 'Authentication failed. Please try again.';
        this.loading = false;
        setTimeout(() => {
          this.router.navigate(['/login']);
        }, 3000);
      }
    });
  }
}