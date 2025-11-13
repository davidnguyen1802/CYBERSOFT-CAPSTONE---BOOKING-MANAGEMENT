import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { UserService } from '../../services/user.service';
import { ActivatedRoute, Router } from '@angular/router';
import { TokenService } from '../../services/token.service';
import { UserResponse } from '../../responses/user/user.response';
import { AuthStateService } from '../../services/auth-state.service';
import { SilentAuthService } from '../../services/silent-auth.service';
import { environment } from '../../../environments/environment';

@Component({
  selector: 'app-header',
  templateUrl: './header.component.html',
  styleUrls: ['./header.component.scss']
})
export class HeaderComponent implements OnInit{
  userResponse?:UserResponse | null;
  isPopoverOpen = false;
  activeNavItem: number = 0;
  isLoggedIn: boolean = false;
  isLoadingAuth: boolean = true; // Add loading state to prevent flickering
  avatarUrl: string = '';
  // Remove trailing slash to avoid double slash when building URLs
  private baseUrl = (environment.apiBaseUrl || 'http://localhost:8080').replace(/\/$/, '');

  constructor(
    private userService: UserService,       
    private tokenService: TokenService,    
    private router: Router,
    private authStateService: AuthStateService,
    private silentAuthService: SilentAuthService,
    private cdr: ChangeDetectorRef
  ) {
    
   }
  ngOnInit() {
    console.log('🎯 Header Component initialized');
    
    // Subscribe to auth ready state FIRST - listen for login/logout events
    this.authStateService.authReady$.subscribe((isReady) => {
      console.log('🔐 Auth ready state changed in header:', isReady);
      if (isReady) {
        // User just logged in - update state
        console.log('✅ User logged in - updating UI');
        this.isLoggedIn = true;
        this.isLoadingAuth = false;
        // NOTE: Don't call loadUserProfile() here - it will be called by silent auth callback
      } else {
        // User logged out - clear state
        // IMPORTANT: Only clear if we were previously logged in
        // Don't clear during initial load (silent auth not done yet)
        if (this.userResponse) {
          console.log('🚪 User logged out - clearing UI');
          this.isLoggedIn = false;
          this.isLoadingAuth = false;
          this.userResponse = null;
          this.avatarUrl = '';
        }
      }
    });
    
    // SILENT AUTHENTICATION FLOW:
    // Try to restore session from RT cookie if no AT in storage
    this.isLoadingAuth = true; // Start loading
    
    this.silentAuthService.attemptSilentLogin().subscribe({
      next: (isAuthenticated) => {
        console.log('=======================================');
        console.log('🔐 SILENT AUTH RESULT:', isAuthenticated ? 'AUTHENTICATED' : 'ANONYMOUS');
        console.log('   Setting isLoggedIn to:', isAuthenticated);
        console.log('=======================================');
        
        this.isLoggedIn = isAuthenticated;
        
        if (isAuthenticated) {
          console.log('✅ User authenticated - loading profile...');
          this.loadUserProfile(); // Only load profile ONCE here
        } else {
          console.log('ℹ️ User is anonymous - showing login button');
          this.isLoadingAuth = false; // Done loading
        }
      },
      error: (error) => {
        console.error('❌ Silent auth error:', error);
        this.isLoggedIn = false;
        this.isLoadingAuth = false; // Done loading even on error
      }
    });
    
    // Set active nav item based on current route
    this.setActiveNavFromRoute();
    
    // Subscribe to route changes
    this.router.events.subscribe(() => {
      this.setActiveNavFromRoute();
    });
  }
  
  setActiveNavFromRoute(): void {
    const currentRoute = this.router.url;
    // Remove verbose logging - only log on actual nav changes
    
    if (currentRoute === '/' || currentRoute.startsWith('/home')) {
      this.activeNavItem = 0;
    } else if (currentRoute.startsWith('/bookings')) {
      this.activeNavItem = 1;
    } else if (currentRoute.startsWith('/my-bookings') || currentRoute.startsWith('/wishlist')) {
      this.activeNavItem = 2;
    } else if (currentRoute.startsWith('/user-profile')) {
      this.activeNavItem = 3;
    } else {
      // Default to -1 if no match
      this.activeNavItem = -1;
    }
  }

  checkLoginStatus(): void {
    const token = this.tokenService.getToken();
    
    // NOTE: Only check if token exists, not if it's expired
    // Backend will handle token validation
    this.isLoggedIn = !!token;
    
    // If logged in, fetch user data from API
    if (this.isLoggedIn) {
      this.loadUserProfile();
    } else {
      console.log('👤 User not logged in, clearing profile data');
      this.userResponse = null;
    }
  }

  loadUserProfile(): void {
    console.log('=======================================');
    console.log('📥 LOADING USER PROFILE FOR HEADER');
    console.log('   Method: Decode JWT (NO API CALL)');
    console.log('   Timestamp:', new Date().toISOString());
    console.log('=======================================');
    
    // Decode JWT to get user info
    console.log('🔍 Decoding JWT token...');
    const startTime = performance.now();
    const userInfo = this.tokenService.getUserInfo();
    const decodeTime = performance.now() - startTime;
    
    if (!userInfo) {
      console.warn('⚠️ FAILED: No valid token or unable to decode JWT');
      console.log('   → Setting isLoggedIn = false');
      console.log('   → Clearing userResponse');
      this.isLoggedIn = false;
      this.userResponse = null;
      this.avatarUrl = '';
      this.isLoadingAuth = false;
      console.log('=======================================');
      return;
    }

    console.log(`✅ SUCCESS: JWT decoded in ${decodeTime.toFixed(2)}ms`);
    console.log('📦 Decoded user info from JWT:');
    console.log('   userId:', userInfo.userId);
    console.log('   username:', userInfo.username);
    console.log('   role:', userInfo.role);
    console.log('   email:', userInfo.email);
    
    // Map to UserResponse format for header display
    console.log('🔍 Mapping to UserResponse format...');
    this.userResponse = {
      id: userInfo.userId,
      username: userInfo.username,
      role: {
        id: userInfo.role === 'ADMIN' ? 1 : (userInfo.role === 'HOST' ? 2 : 3),
        name: userInfo.role
      }
    } as any;
    console.log('✅ UserResponse mapped');
    
    // Set avatar URL based on user ID
    console.log('🔍 Setting avatar URL...');
    this.avatarUrl = `${this.baseUrl}/files/avatar_user_${userInfo.userId}.jpg`;
    console.log('✅ Avatar URL set:', this.avatarUrl);
    
    console.log('=======================================');
    console.log('✅ HEADER PROFILE LOADED SUCCESSFULLY');
    console.log('👤 USER INFO:');
    console.log('   id:', userInfo.userId);
    console.log('   username:', userInfo.username);
    console.log('   role:', userInfo.role);
    console.log('   avatarUrl:', this.avatarUrl);
    console.log('=======================================');
    
    this.isLoadingAuth = false; // Done loading
    console.log('✅ isLoadingAuth set to false - UI should update now');
    
    // Force Angular to detect changes
    this.cdr.detectChanges();
    console.log('✅ Change detection triggered manually');
  }

  // Navigate to user profile
  navigateToProfile(): void {
    console.log('➡️ Navigating to user profile with section=profile');
    
    // LAZY LOAD: If userResponse not loaded yet, load it now
    if (this.isLoggedIn && !this.userResponse) {
      console.log('📥 Profile not loaded yet - loading now...');
      this.loadUserProfile();
    }
    
    this.activeNavItem = 3;
    setTimeout(() => {
      this.router.navigate(['/user-profile'], { 
        queryParams: { section: 'profile' }
      });
    }, 100);
  }

  // Handle avatar image error (fallback to default)
  onAvatarError(): void {
    console.warn('⚠️ Avatar image failed to load, using default');
    this.avatarUrl = 'assets/img/default-avatar.svg'; // Use default avatar SVG
  }  

  togglePopover(event: Event): void {
    event.preventDefault();
    this.isPopoverOpen = !this.isPopoverOpen;
  }

  handleItemClick(index: number): void {
    console.log('=======================================');
    console.log('🖱️ HeaderComponent.handleItemClick() CALLED');
    console.log('   Index:', index);
    console.log('   Timestamp:', new Date().toISOString());
    console.log('=======================================');
    
    if(index === 0) {
      // Navigate to user profile
      console.log('➡️ Navigating to user profile');
      this.router.navigate(['/user-profile']);                      
    } else if (index === 2) {
      console.log('🚪 LOGOUT ACTION - Starting logout process...');
      // Logout: call backend to clear HttpOnly cookie, then clear local token
      this.userService.logout().subscribe({
        next: () => {
          console.log('✅ Backend logout successful');
        },
        error: (error) => {
          console.error('❌ Logout error (will still clear local data):', error);
        },
        complete: () => {
          console.log('🧹 Clearing local tokens and user data...');
          
          // IMPORTANT: Notify logout FIRST to trigger UI updates
          this.authStateService.notifyLogout(); // ← Triggers header to show "Đăng nhập" button
          
          // Then clear local token and user data
          this.tokenService.removeToken();
          this.userService.removeUserFromLocalStorage();
          this.userResponse = null;
          this.isLoggedIn = false;
          
          console.log('✅ Logout complete - Login button should now be visible');
          console.log('➡️ Redirecting to login page...');
          this.router.navigate(['/login']);
        }
      });
    }
    this.isPopoverOpen = false; // Close the popover after clicking an item    
  }

  
  setActiveNavItem(index: number, route: string) {
    console.log(`🎯 Setting active nav item: ${index}, navigating to: ${route}`);
    this.activeNavItem = index;
    
    // Navigate after a short delay to allow animation to start
    setTimeout(() => {
      this.router.navigate([route]);
    }, 100);
  }  
}