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
import { SimpleModalService } from '../../services/simple-modal.service';
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
  rememberMe: boolean = false; // Default is false - only save token if user checks this

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
    private modalService: SimpleModalService
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

    // CRITICAL: Include rememberMe in request body (backend needs this!)
    const loginDTO: LoginDTO = {
      usernameOrEmail: this.usernameOrEmail,
      password: this.password,
      rememberMe: this.rememberMe // ← NEW: Send to backend
    };
    
    console.log('🔐 Sending login request...', { 
      usernameOrEmail: this.usernameOrEmail,
      rememberMe: this.rememberMe 
    });
    
    this.userService.login(loginDTO).subscribe({
      next: (response: LoginResponse) => {
        console.log('✅ Login response:', response);
        
        // Check if response and response.data exist
        if (!response || !response.data) {
          console.error('❌ Invalid response structure:', response);
          this.modalService.showError('Phản hồi không hợp lệ từ server');
          return;
        }
        
        // CRITICAL: Extract token from response
        // Backend returns: { message, status, data: "<JWT_STRING>" }
        // NOT: { message, status, data: { token: "..." } }
        let token: string;
        
        if (typeof response.data === 'string') {
          // Case 1: data IS the JWT string directly
          token = response.data;
          console.log('✅ Token extracted from response.data (string)');
        } else if (response.data && typeof response.data === 'object' && 'token' in response.data) {
          // Case 2: data is object with token property (backward compatibility)
          token = (response.data as any).token;
          console.log('✅ Token extracted from response.data.token (object)');
        } else {
          console.error('❌ Cannot find token in response:', response);
          this.modalService.showError('Không tìm thấy token trong phản hồi');
          return;
        }
        
        // Validate token exists and is truthy
        if (!token || typeof token !== 'string' || token.trim() === '') {
          console.error('❌ Token is null/empty/invalid:', { 
            token, 
            type: typeof token,
            truthy: !!token,
            length: token?.length 
          });
          this.modalService.showError('Token không hợp lệ');
          return;
        }
        
        console.log('🔐 Token validation:');
        console.log('   Truthy:', !!token);
        console.log('   Type:', typeof token);
        console.log('   Length:', token.length);
        console.log('   Preview:', token.substring(0, 50) + '...');
        
        // CRITICAL: Save token using TokenService (handles storage location + JWT validation)
        // rememberMe determines storage: localStorage (true) vs sessionStorage (false)
        console.log(`💾 Saving token with rememberMe=${this.rememberMe}...`);
        
        // Save remember preference first
        if (this.rememberMe) {
          localStorage.setItem('remember', '1');
        } else {
          localStorage.removeItem('remember');
        }
        
        // Then save token (TokenService will validate JWT format and throw if invalid)
        try {
          this.tokenService.setToken(token, this.rememberMe);
          console.log('✅ Token saved to storage successfully');
        } catch (error) {
          console.error('❌ Failed to save token:', error);
          this.modalService.showError('Lỗi lưu token: ' + (error as Error).message);
          return;
        }
        
        // NOTE: Device ID is auto-generated by DeviceService.getDeviceId() when needed
        // DeviceIdInterceptor will trigger auto-generation on first auth request
        // No need to manually generate here
        
        // CRITICAL: Notify auth ready AFTER token is saved
        // Use setTimeout(0) to ensure storage operations complete
        console.log('🔐 Notifying auth ready state (next tick)...');
        setTimeout(() => {
          this.authStateService.notifyLogin(); // This sets authReady = true
          
          // Refresh cart (cart service will use token to identify user)
          console.log('🛒 Refreshing cart for logged-in user');
          this.cartService.refreshCart();
          
          // Navigate based on role from TOKEN PAYLOAD (not response data)
          // Decode token to get role
          const decodedToken = this.tokenService.decodeToken(token);
          const role = decodedToken?.role || '';
          const isAdmin = role === 'ROLE_ADMIN';
          
          console.log('👤 User role (from token):', role);
          console.log('👤 Is Admin:', isAdmin);
          
          if (isAdmin) {
            console.log('➡️ Navigating to /admin');
            this.router.navigate(['/admin']);
          } else {
            console.log('➡️ Navigating to /');
            this.router.navigate(['/']);
          }
        }, 100); // 100ms delay to ensure auth ready propagates
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
        
        const errorMessage = error?.error?.message || 'Đăng nhập thất bại. Vui lòng kiểm tra lại thông tin đăng nhập.';
        this.modalService.showError(errorMessage);
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
        this.modalService.showError('Không thể khởi tạo đăng nhập Google. Vui lòng thử lại.');
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
        this.modalService.showError('Không thể khởi tạo đăng nhập Facebook. Vui lòng thử lại.');
      }
    });
  }
}
