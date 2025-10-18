import { Component, ViewChild, OnInit } from '@angular/core';
import { LoginDTO } from '../../dtos/user/login.dto';
import { UserService } from '../../services/user.service';
import { TokenService } from '../../services/token.service';
import { RoleService } from '../../services/role.service'; // Import RoleService
import { ActivatedRoute, Router } from '@angular/router';
import { NgForm } from '@angular/forms';
import { LoginResponse } from '../../responses/user/login.response';
import { Role } from '../../models/role'; // Đường dẫn đến model Role
import { UserResponse } from '../../responses/user/user.response';
import { CartService } from '../../services/cart.service';
import { HttpClient } from '@angular/common/http';
import { getBaseUrl } from '../../utils/url.util';
import { AuthStateService } from '../../services/auth-state.service';
@Component({
  selector: 'app-login',
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.scss']
})
export class LoginComponent implements OnInit{
  @ViewChild('loginForm') loginForm!: NgForm;

  /*
  //Login user1 (can use phone or email)
  usernameOrEmail: string = '33445566';
  password: string = '123456789';

  //Login user2
  usernameOrEmail: string = '0964896239';
  password: string = '123456789';

  //Login admin
  usernameOrEmail: string = '11223344';
  password: string = '11223344';

  //Login with email
  usernameOrEmail: string = 'user@example.com';
  password: string = '123456789';
  */
  usernameOrEmail: string = '';
  password: string = '';
  showPassword: boolean = false;

  // roles: Role[] = []; // Not needed - roles determined by backend
  rememberMe: boolean = true;
  // selectedRole: Role | undefined; // Not needed - roles determined by backend
  userResponse?: UserResponse

  onUsernameOrEmailChange() {
    console.log(`Username/Email typed: ${this.usernameOrEmail}`);
    //how to validate ? must be at least 6 characters
  }
  constructor(
    private router: Router,
    private activatedRoute: ActivatedRoute,
    private userService: UserService,
    private tokenService: TokenService,
    private roleService: RoleService,
    private cartService: CartService,
    private http: HttpClient,
    private authStateService: AuthStateService,
  ) { }

  ngOnInit() {
    // Role selection not needed for login - roles are determined by the backend based on user account
    // The backend /auth/login endpoint returns user info with their assigned role
  }
  createAccount() {
    // debugger
    // Chuyển hướng người dùng đến trang đăng ký (hoặc trang tạo tài khoản)
    this.router.navigate(['/register']); 
  }
  login() {
    console.log('🔐 Login attempt started');
    const message = `usernameOrEmail: ${this.usernameOrEmail}` +
      `password: ${this.password}`;
    //alert(message);
    // debugger

    const loginDTO: LoginDTO = {
      usernameOrEmail: this.usernameOrEmail,
      password: this.password
    };
    
    console.log('🔐 Sending login request...', { usernameOrEmail: this.usernameOrEmail });
    
    this.userService.login(loginDTO).subscribe({
      next: (response: LoginResponse) => {
        console.log('✅ Login response:', response);
        
        // Check if response and response.data exist
        if (!response || !response.data) {
          console.error('❌ Invalid response structure:', response);
          alert('Invalid response from server');
          return;
        }
        
        const authData = response.data;
        const token = authData.token;
        console.log('Token received:', token);
        console.log('User data:', authData);
        
        if (this.rememberMe) {          
          // Save access token to localStorage
          // Refresh token is automatically stored in HttpOnly cookie by backend
          this.tokenService.setToken(token);
          
          // Notify that user has logged in
          console.log('🔐 Notifying login state change');
          this.authStateService.notifyLogin();
          
          // Refresh cart (cart service will use token to identify user)
          console.log('🛒 Refreshing cart for logged-in user');
          this.cartService.refreshCart();
          
          // Navigate based on role
          const roles = authData.roles || [];
          const isAdmin = roles.includes('ROLE_ADMIN');
          
          console.log('👤 User roles:', roles);
          console.log('👤 Is Admin:', isAdmin);
          
          if (isAdmin) {
            console.log('➡️ Navigating to /admin');
            this.router.navigate(['/admin']);
          } else {
            console.log('➡️ Navigating to /');
            this.router.navigate(['/']);
          }
        } else {
          console.warn('⚠️ Remember me is not checked, tokens not saved');
        }
      },
      complete: () => {
        console.log('✅ Login process complete');
      },
      error: (error: any) => {
        console.error('❌ Login error:', error);
        console.error('❌ Error details:', {
          status: error.status,
          statusText: error.statusText,
          message: error?.error?.message,
          fullError: error?.error
        });
        
        const errorMessage = error?.error?.message || 'Login failed. Please check your credentials.';
        alert(errorMessage);
      }
    });
  }
  togglePassword() {
    this.showPassword = !this.showPassword;
  }

  // Social Login - Google
  loginWithGoogle(): void {
    const loginType = 'google';
    const baseUrl = getBaseUrl();
    const apiUrl = `${baseUrl}/auth/social-login?login_type=${loginType}`;
    
    console.log('Initiating Google login...');
    console.log('Fetching OAuth URL from:', apiUrl);
    
    // Step 1: Make HTTP request to get the OAuth authorization URL from backend
    this.http.get(apiUrl, { responseType: 'text' }).subscribe({
      next: (oauthUrl: string) => {
        console.log('Received OAuth URL:', oauthUrl);
        
        // Step 2: Redirect browser to the Google OAuth login page
        window.location.href = oauthUrl;
      },
      error: (error) => {
        console.error('Error getting OAuth URL:', error);
        alert('Failed to initiate Google login. Please try again.');
      }
    });
  }

  // Social Login - Facebook
  loginWithFacebook(): void {
    const loginType = 'facebook';
    const baseUrl = getBaseUrl();
    const apiUrl = `${baseUrl}/auth/social-login?login_type=${loginType}`;
    
    console.log('Initiating Facebook login...');
    console.log('Fetching OAuth URL from:', apiUrl);
    
    // Step 1: Make HTTP request to get the OAuth authorization URL from backend
    this.http.get(apiUrl, { responseType: 'text' }).subscribe({
      next: (oauthUrl: string) => {
        console.log('Received OAuth URL:', oauthUrl);
        
        // Step 2: Redirect browser to the Facebook OAuth login page
        window.location.href = oauthUrl;
      },
      error: (error) => {
        console.error('Error getting OAuth URL:', error);
        alert('Failed to initiate Facebook login. Please try again.');
      }
    });
  }
}
