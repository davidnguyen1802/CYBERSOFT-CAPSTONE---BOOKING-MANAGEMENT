import { Component, OnInit } from '@angular/core';
import { UserService } from '../../services/user.service';

import { ActivatedRoute, Router } from '@angular/router';
import { TokenService } from '../../services/token.service';
import { UserResponse } from '../../responses/user/user.response';
import { AuthStateService } from '../../services/auth-state.service';
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
  avatarUrl: string = '';
  // Remove trailing slash to avoid double slash when building URLs
  private baseUrl = (environment.apiBaseUrl || 'http://localhost:8080').replace(/\/$/, '');

  constructor(
    private userService: UserService,       
    private tokenService: TokenService,    
    private router: Router,
    private authStateService: AuthStateService,
  ) {
    
   }
  ngOnInit() {
    console.log('🎯 Header Component initialized');
    // Check if user is logged in by checking token
    this.checkLoginStatus();
    
    // Subscribe to login state changes
    this.authStateService.loginState$.subscribe((isLoggedIn) => {
      console.log('🔐 Login state changed in header:', isLoggedIn);
      this.checkLoginStatus();
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
    console.log('🗺️ Current route:', currentRoute);
    
    if (currentRoute === '/' || currentRoute.startsWith('/home')) {
      this.activeNavItem = 0;
    } else if (currentRoute.startsWith('/bookings')) {
      this.activeNavItem = 1;
    } else if (currentRoute.startsWith('/orders') || currentRoute.startsWith('/wishlist')) {
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
    const isTokenExpired = this.tokenService.isTokenExpired();
    this.isLoggedIn = !!(token && !isTokenExpired);
    
    console.log('🔐 Checking login status:', {
      hasToken: !!token,
      isExpired: isTokenExpired,
      isLoggedIn: this.isLoggedIn
    });
    
    // If logged in, fetch user data from API
    if (this.isLoggedIn) {
      this.loadUserProfile();
    } else {
      console.log('👤 User not logged in, clearing profile data');
      this.userResponse = null;
    }
  }

  loadUserProfile(): void {
    const token = this.tokenService.getToken();
    if (!token) {
      console.warn('⚠️ No token available to load profile');
      return;
    }

    console.log('📥 Loading user profile for header...');
    // Fetch user profile from API
    this.userService.getMyProfile(token).subscribe({
      next: (response: any) => {
        console.log('✅ Header profile loaded:', response);
        if (response && response.data) {
          this.userResponse = {
            id: response.data.id,
            fullname: response.data.fullname || response.data.username,
            username: response.data.username,
            email: response.data.email,
            role: {
              id: response.data.role === 'ADMIN' ? 1 : (response.data.role === 'HOST' ? 2 : 3),
              name: response.data.role || 'GUEST'
            }
          } as any;
          
          // Set avatar URL based on user ID
          if (this.userResponse && this.userResponse.id) {
            this.avatarUrl = `${this.baseUrl}/files/avatar_user_${this.userResponse.id}.jpg`;
            console.log('👤 Avatar URL:', this.avatarUrl);
          }
          
          console.log('👤 User info in header:', {
            fullname: this.userResponse?.fullname,
            role: typeof this.userResponse?.role === 'string' 
              ? this.userResponse?.role 
              : this.userResponse?.role?.name
          });
        }
      },
      error: (error) => {
        console.error('❌ Error loading user profile in header:', error);
        // If error, user might be logged out or token is invalid
        console.log('🚪 Clearing login state due to profile load error');
        this.isLoggedIn = false;
        this.userResponse = null;
        this.avatarUrl = '';
        // Optionally clear invalid token
        if (error.status === 401 || error.status === 403) {
          console.log('🔑 Clearing invalid token');
          this.tokenService.removeToken();
        }
      }
    });
  }

  // Navigate to user profile
  navigateToProfile(): void {
    console.log('➡️ Navigating to user profile');
    this.activeNavItem = 3;
    setTimeout(() => {
      this.router.navigate(['/user-profile']);
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
    //alert(`Clicked on "${index}"`);
    console.log(`🖱️ Header menu item clicked: ${index}`);
    
    if(index === 0) {
      // Navigate to user profile
      console.log('➡️ Navigating to user profile');
      this.router.navigate(['/user-profile']);                      
    } else if (index === 2) {
      // Logout: call backend to clear HttpOnly cookie, then clear local token
      this.userService.logout().subscribe({
        next: () => {
          console.log('Logged out successfully');
        },
        error: (error) => {
          console.error('Logout error (will still clear local data):', error);
        },
        complete: () => {
          // Clear local token and user data regardless of backend response
          this.tokenService.removeToken();
          this.userService.removeUserFromLocalStorage(); // Clean up any residual data
          this.userResponse = null;
          this.isLoggedIn = false;
          this.authStateService.notifyLogout(); // Notify logout
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