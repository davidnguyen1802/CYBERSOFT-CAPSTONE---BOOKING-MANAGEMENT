import { Component, ViewChild, OnInit } from '@angular/core';
import { 
  FormBuilder, 
  FormGroup, 
  Validators,
  ValidationErrors, 
  ValidatorFn, 
  AbstractControl
} from '@angular/forms';

import { Router, ActivatedRoute } from '@angular/router';
import { UserService } from '../../services/user.service';
import { TokenService } from '../../services/token.service';
import { AuthStateService } from '../../services/auth-state.service';
import { DeviceService } from '../../services/device.service';
import { PromotionService } from '../../services/promotion.service';
import { BookingService } from '../../services/booking.service';
import { ReviewService } from '../../services/review.service';
import { PropertyService } from '../../services/property.service';
import { SimpleModalService } from '../../services/simple-modal.service';
import { UserResponse } from '../../responses/user/user.response';
import { UpdateUserDTO } from '../../dtos/user/update.user.dto';
import { UpdateReviewDTO } from '../../dtos/review/update.review.dto';
import { Property } from '../../models/property';
import { UserPromotionDTO } from '../../models/user-promotion.dto';
import { ApiResponse } from '../../models/api-response';
import { PageContent } from '../../models/api-response';
import { Booking, BookingStatus } from '../../models/booking';
import { Review } from '../../models/review';
import { environment } from '../../../environments/environment';

@Component({
  selector: 'user-profile',
  templateUrl: './user.profile.component.html',
  styleUrls: ['./user.profile.component.scss']
})
export class UserProfileComponent implements OnInit {
  userResponse?: UserResponse;
  userProfileForm: FormGroup;
  token: string = '';
  isEditMode: boolean = false;
  isLoading: boolean = false;
  activeSection: string = 'profile'; // Default section
  avatarUrl: string = '';
  private baseUrl = (environment.apiBaseUrl || 'http://localhost:8080').replace(/\/$/, '');
  
  // Wishlist data
  favoriteProperties: Property[] = [];
  filteredProperties: Property[] = [];
  isLoadingWishlist: boolean = false;
  searchQuery: string = '';
  wishlistCurrentPage: number = 0;
  wishlistTotalPages: number = 0;
  wishlistTotalElements: number = 0;
  wishlistPageSize: number = 9;

  // Promotions data
  promotions: UserPromotionDTO[] = [];
  activePromotions: UserPromotionDTO[] = [];
  usedPromotions: UserPromotionDTO[] = [];
  expiredPromotions: UserPromotionDTO[] = [];
  isLoadingPromotions: boolean = false;
  promotionsError: string = '';
  promotionsCurrentPage: number = 0;
  promotionsTotalPages: number = 0;
  promotionsTotalElements: number = 0;
  promotionsPageSize: number = 9;

  // Bookings data
  bookings: Booking[] = [];
  upcomingBookings: Booking[] = [];
  pastBookings: Booking[] = [];
  isLoadingBookings: boolean = false;
  bookingsError: string = '';
  bookingsCurrentPage: number = 0;
  bookingsTotalPages: number = 0;
  bookingsTotalElements: number = 0;
  bookingsPageSize: number = 5; // 5 bookings per page

  // Reviews data
  reviews: Review[] = [];
  isLoadingReviews: boolean = false;
  reviewsError: string = '';
  editingReviewId: number | null = null;
  editingReviewData: { comment: string; rating: number } | null = null;

  // Host Properties data
  hostProperties: Property[] = [];
  filteredHostProperties: Property[] = [];
  isLoadingHostProperties: boolean = false;
  hostPropertiesError: string = '';
  propertySearchQuery: string = '';
  propertySortBy: string = 'id';
  propertySortDirection: string = 'DESC';
  hostPropertiesCurrentPage: number = 0;
  hostPropertiesTotalPages: number = 0;
  hostPropertiesTotalElements: number = 0;
  hostPropertiesPageSize: number = 9;

  constructor(
    private formBuilder: FormBuilder,
    private activatedRoute: ActivatedRoute,
    private userService: UserService,
    private router: Router,
    private tokenService: TokenService,
    private authStateService: AuthStateService,
    private deviceService: DeviceService,
    private promotionService: PromotionService,
    private bookingService: BookingService,
    private reviewService: ReviewService,
    private propertyService: PropertyService,
    private modalService: SimpleModalService,
  ){        
    this.userProfileForm = this.formBuilder.group({
      fullname: ['', [Validators.required, Validators.minLength(2)]],     
      address: ['', [Validators.minLength(3)]],       
      password: ['', [Validators.minLength(6)]], 
      retype_password: ['', [Validators.minLength(6)]], 
      date_of_birth: [''],      
    }, {
      validators: this.passwordMatchValidator
    });
  }
  
  ngOnInit(): void {
    console.log('👤 UserProfileComponent initialized');
    this.token = this.tokenService.getToken() || '';
    
    // Check if user has token (don't check expiration - interceptor handles refresh)
    if (!this.token) {
      console.warn('⚠️ No token found, redirecting to login');
      this.modalService.showError('Vui lòng đăng nhập để xem profile của bạn');
      this.router.navigate(['/login']);
      return;
    }

    console.log('✅ Token found');
    console.log('   TokenService.isTokenExpired() called');
    console.log('   Result:', this.tokenService.isTokenExpired());
    console.log('   Token preview:', this.token.substring(0, 50) + '...');
    
    // Check for section query param (for browser back button support)
    this.activatedRoute.queryParams.subscribe(params => {
      if (params['section']) {
        const section = params['section'];
        this.activeSection = section;
        console.log('📂 Active section from URL:', this.activeSection);
        
        // Don't load section data here - wait until userResponse is loaded
        // The data will be loaded in loadUserProfile() success callback
      }
    });
    
    // Load user profile from backend API
    // If token is expired, interceptor will automatically refresh before making the request
    this.loadUserProfile();
  }

  loadUserProfile(): void {
    console.log('📥 Loading user profile from backend API...');
    this.isLoading = true;
    
    // Call backend API to get detailed user profile (interceptor handles Authorization header)
    this.userService.getMyDetailedProfile(true).subscribe({
      next: (response: any) => {
        console.log('📥 Profile API response:', response);
        
        // Backend returns: { code: 200, message: "...", data: {...} }
        if (response && response.data) {
          const userData = response.data;
          console.log('👤 Raw user data from backend:', userData);
          
          this.userResponse = {
            id: userData.id,
            fullname: userData.fullname,
            username: userData.username,
            email: userData.email,
            phone: userData.phone,
            address: userData.address,
            avatar: userData.avatar,
            gender: userData.gender,
            dob: userData.dob,
            is_active: userData.status === 'ACTIVE',
            status: userData.status,
            date_of_birth: userData.dob ? new Date(userData.dob) : new Date(),
            create_date: userData.create_date,
            facebook_account_id: userData.facebook_account_id || 0,
            google_account_id: userData.google_account_id || 0,
            role: {
              id: userData.role === 'ADMIN' ? 1 : (userData.role === 'HOST' ? 2 : 3),
              name: userData.role || 'GUEST'
            },
            // Statistics from backend API (exact field names from response)
            total_bookings: userData.total_bookings !== null ? userData.total_bookings : 0,
            total_reviews: userData.total_reviews !== null ? userData.total_reviews : 0,
            favorite_properties_count: userData.favorite_properties_count !== null ? userData.favorite_properties_count : 0,
            active_promotions_count: userData.active_promotions_count !== null ? userData.active_promotions_count : 0,
            hosted_properties_count: userData.hosted_properties_count !== null ? userData.hosted_properties_count : 0,
            total_earnings: userData.total_earnings !== null ? userData.total_earnings : 0,
            average_rating: userData.average_rating !== null ? userData.average_rating : 0,
            total_property_reviews: userData.total_property_reviews !== null ? userData.total_property_reviews : 0
          } as any;

          // Populate form with data
          this.userProfileForm.patchValue({
            fullname: this.userResponse?.fullname || '',
            address: this.userResponse?.address || '',
            date_of_birth: this.userResponse?.date_of_birth ? 
              new Date(this.userResponse.date_of_birth).toISOString().substring(0, 10) : '',
          });

          // Set avatar URL based on user ID
          if (this.userResponse && this.userResponse.id) {
            this.avatarUrl = `${this.baseUrl}/files/avatar_user_${this.userResponse.id}.jpg`;
            console.log('👤 Avatar URL:', this.avatarUrl);
          }

          console.log('✅ User profile loaded successfully:', {
            id: this.userResponse?.id,
            fullname: this.userResponse?.fullname,
            email: this.userResponse?.email,
            role: typeof this.userResponse?.role === 'string' 
              ? this.userResponse?.role 
              : this.userResponse?.role?.name,
            stats: {
              total_bookings: this.userResponse?.total_bookings,
              total_reviews: this.userResponse?.total_reviews,
              favorite_properties: this.userResponse?.favorite_properties_count,
              active_promotions: this.userResponse?.active_promotions_count,
              hosted_properties: this.userResponse?.hosted_properties_count
            }
          });
          this.isLoading = false;
          
          // After user profile is loaded, load section data if activeSection is set
          if (this.activeSection && this.activeSection !== 'profile') {
            console.log('🔄 Loading section data after profile loaded:', this.activeSection);
            this.autoLoadSectionData(this.activeSection);
          }
        }
      },
      error: (error: any) => {
        console.error('❌ Error loading profile:', error);
        console.error('❌ Error details:', {
          status: error.status,
          statusText: error.statusText,
          message: error?.error?.message,
          url: error.url
        });
        this.isLoading = false;
        
        // If API fails, redirect to login (don't use localStorage fallback)
        this.modalService.showError('Không thể tải profile. Vui lòng đăng nhập lại.');
        this.tokenService.removeToken(); // Clear invalid tokens
        this.router.navigate(['/login']);
      }
    });
  }
  passwordMatchValidator(): ValidatorFn {
    return (formGroup: AbstractControl): ValidationErrors | null => {
      const password = formGroup.get('password')?.value;
      const retypedPassword = formGroup.get('retype_password')?.value;
      if (password !== retypedPassword) {
        return { passwordMismatch: true };
      }
  
      return null;
    };
  }
  toggleEditMode(): void {
    this.isEditMode = !this.isEditMode;
    console.log(`✏️ Edit mode ${this.isEditMode ? 'enabled' : 'disabled'}`);
    
    if (this.isEditMode && this.userResponse) {
      // Reset form when entering edit mode
      this.userProfileForm.patchValue({
        fullname: this.userResponse.fullname || '',
        address: this.userResponse.address || '',
        date_of_birth: this.userResponse.date_of_birth ? 
          new Date(this.userResponse.date_of_birth).toISOString().substring(0, 10) : '',
        password: '',
        retype_password: ''
      });
      console.log('📝 Form populated with current user data');
    }
  }

  cancelEdit(): void {
    console.log('❌ Edit cancelled, reloading original data');
    this.isEditMode = false;
    this.userProfileForm.reset();
    // Reload original data from backend
    this.loadUserProfile();
  }

  save(): void {
    console.log('💾 Save button clicked');
    if (this.userProfileForm.valid) {
      this.isLoading = true;
      
      // Prepare the update DTO
      const updateData: any = {
        fullname: this.userProfileForm.get('fullname')?.value,
        address: this.userProfileForm.get('address')?.value || '',
        date_of_birth: this.userProfileForm.get('date_of_birth')?.value
      };

      // Only include password if user entered one
      const password = this.userProfileForm.get('password')?.value;
      if (password && password.trim() !== '') {
        updateData.password = password;
        updateData.retype_password = this.userProfileForm.get('retype_password')?.value;
        console.log('🔒 Password change requested');
      }
  
      console.log('📤 Sending update data to API:', updateData);

      // Call backend API: PUT /users/me
      this.userService.updateMyProfile(this.token, updateData)
        .subscribe({
          next: (response: any) => {
            console.log('✅ Profile update response:', response);
            
            // Check for successful response
            if (response && response.code === 200) {
              console.log('✅ Profile updated successfully');
              this.modalService.showSuccess('Cập nhật profile thành công!');
              this.isEditMode = false;
              
              // Reload profile data from backend
              this.loadUserProfile();
            } else {
              console.warn('⚠️ Unexpected response format:', response);
              this.modalService.showError('Cập nhật profile thành công nhưng phản hồi không đúng định dạng');
              this.isLoading = false;
            }
          },
          error: (error: any) => {
            console.error('❌ Error updating profile:', error);
            console.error('❌ Error details:', {
              status: error.status,
              statusText: error.statusText,
              message: error?.error?.message,
              fullError: error?.error
            });
            this.isLoading = false;
            
            // Show detailed error message
            const errorMessage = error?.error?.message || error?.message || 'Không thể cập nhật profile';
            this.modalService.showError(errorMessage);
          }
        });
    } else {
      console.warn('⚠️ Form validation failed');
      if (this.userProfileForm.hasError('passwordMismatch')) {
        console.warn('⚠️ Password mismatch');
        this.modalService.showError('Mật khẩu không khớp');
      } else {
        console.warn('⚠️ Form errors:', this.userProfileForm.errors);
        this.modalService.showError('Vui lòng điền đầy đủ các trường bắt buộc');
      }
    }
  }

  logout(): void {
    console.log('=======================================');
    console.log('🚪 LOGOUT - Starting comprehensive cleanup');
    console.log('   Timestamp:', new Date().toISOString());
    console.log('=======================================');
    
    // Step 1: Call backend to revoke RT and clear HttpOnly cookie
    this.userService.logout().subscribe({
      next: () => {
        console.log('✅ Backend logout successful - RT revoked, cookie cleared');
      },
      error: (error) => {
        console.error('❌ Backend logout error (will still clear local data):', error);
      },
      complete: () => {
        console.log('🧹 Starting frontend cleanup...');
        
        // Step 2: Backup device_id before clearing (device_id should persist across logouts)
        const deviceId = localStorage.getItem('device_id');
        console.log('💾 Backing up device_id:', deviceId ? deviceId.substring(0, 8) + '...' : 'none');
        
        // Step 3: Clear ALL localStorage data
        console.log('📦 Clearing localStorage...');
        const localStorageKeys = Object.keys(localStorage);
        localStorageKeys.forEach(key => {
          console.log(`   Removing: ${key}`);
          localStorage.removeItem(key);
        });
        console.log(`✅ localStorage cleared (${localStorageKeys.length} keys removed)`);
        
        // Step 4: Restore device_id if it existed
        if (deviceId) {
          localStorage.setItem('device_id', deviceId);
          console.log('✅ device_id restored');
        }
        
        // Step 5: Clear ALL sessionStorage data
        console.log('📦 Clearing sessionStorage...');
        const sessionStorageKeys = Object.keys(sessionStorage);
        sessionStorageKeys.forEach(key => {
          console.log(`   Removing: ${key}`);
          sessionStorage.removeItem(key);
        });
        console.log(`✅ sessionStorage cleared (${sessionStorageKeys.length} keys removed)`);
        
        // Step 6: Notify auth state (updates UI)
        this.authStateService.notifyLogout();
        console.log('✅ Auth state notified - UI will update');
        
        // Step 7: Redirect to Home
        console.log('➡️ Redirecting to Home page...');
        this.router.navigate(['/']);
        console.log('=======================================');
        console.log('✅ LOGOUT COMPLETE');
        console.log('=======================================');
      }
    });
  }

  // Helper methods for template
  getUserAge(): number | null {
    if (!this.userResponse?.date_of_birth) return null;
    
    const birthDate = new Date(this.userResponse.date_of_birth);
    const today = new Date();
    let age = today.getFullYear() - birthDate.getFullYear();
    const monthDiff = today.getMonth() - birthDate.getMonth();
    
    if (monthDiff < 0 || (monthDiff === 0 && today.getDate() < birthDate.getDate())) {
      age--;
    }
    
    return age;
  }

  formatDate(date: Date | string | null | undefined): string {
    if (!date) return 'N/A';
    
    const dateObj = new Date(date);
    if (isNaN(dateObj.getTime())) return 'N/A';
    
    const day = dateObj.getDate().toString().padStart(2, '0');
    const month = (dateObj.getMonth() + 1).toString().padStart(2, '0');
    const year = dateObj.getFullYear();
    
    return `${day}/${month}/${year}`;
  }

  getMemberSinceDate(): string {
    // Use create_date (when user account was created) for "Member Since"
    return this.formatDate(this.userResponse?.create_date);
  }

  getRoleName(): string {
    if (!this.userResponse?.role) return 'Guest';
    
    // If role is a string (from API), return it directly
    if (typeof this.userResponse.role === 'string') {
      return this.userResponse.role;
    }
    
    // If role is an object, return the name property
    return this.userResponse.role.name || 'Guest';
  }

  isHost(): boolean {
    const role = this.getRoleName();
    return role.toLowerCase() === 'host';
  }

  navigateToAddProperty(): void {
    console.log('🏠 Navigating to add property page...');
    this.router.navigate(['/host/add-property']);
  }

  navigateToHostDashboard(): void {
    console.log('📊 Navigating to host dashboard...');
    this.router.navigate(['/host/dashboard']);
  }

  getUserId(): number {
    return this.tokenService.getUserId();
  }

  // Auto-load section data based on URL query param or manual click
  autoLoadSectionData(section: string): void {
    console.log('🔄 Auto-loading data for section:', section);
    
    switch (section) {
      case 'wishlist':
        if (this.userResponse?.id) {
          this.loadWishlist();
        } else {
          console.log('⏳ Waiting for user profile to load before fetching wishlist');
        }
        break;
      case 'promotions':
        console.log('🎫 Loading promotions section...');
        console.log('   User ID (from JWT):', this.tokenService.getUserId());
        console.log('   Username (from JWT):', this.tokenService.getUsername());
        this.loadPromotions();
        break;
      case 'booking':
        console.log('🏨 Loading bookings section...');
        console.log('   User ID (from JWT):', this.tokenService.getUserId());
        this.loadBookings();
        break;
      case 'reviews':
        console.log('⭐ Loading reviews section...');
        console.log('   User ID (from JWT):', this.tokenService.getUserId());
        this.loadReviews();
        break;
      case 'manage-properties':
        console.log('🏠 Loading host properties section...');
        console.log('   User ID (from JWT):', this.tokenService.getUserId());
        this.loadHostProperties();
        break;
      default:
        console.log('ℹ️ Section does not require data loading:', section);
    }
  }

  // Set active section in sidebar
  setActiveSection(section: string): void {
    console.log(`📂 Switching to section: ${section}`);
    
    this.activeSection = section;
    
    // Update URL without navigation (for browser back button support)
    const currentUrl = this.router.url.split('?')[0];
    this.router.navigate([], {
      relativeTo: this.activatedRoute,
      queryParams: { section: section },
      queryParamsHandling: 'merge',
      replaceUrl: false // Don't replace history, add new entry
    });
    
    // Exit edit mode when switching sections
    if (this.isEditMode) {
      this.isEditMode = false;
      this.userProfileForm.reset();
    }
    
    // Load data based on section
    this.autoLoadSectionData(section);
  }
  
  // Load user's favorite properties (with pagination)
  loadWishlist(page: number = 0): void {
    if (!this.userResponse?.id) {
      console.warn('⚠️ User ID not available, cannot load wishlist');
      return;
    }
    
    console.log('🔵 Loading wishlist for user:', this.userResponse.id, 'page:', page);
    this.isLoadingWishlist = true;
    this.favoriteProperties = [];
    
    this.userService.getFavoriteProperties(this.userResponse.id, page, this.wishlistPageSize).subscribe({
      next: (response) => {
        console.log('✅ Wishlist API response:', response);
        if (response && response.data) {
          this.favoriteProperties = response.data.content;
          this.filteredProperties = response.data.content;
          this.wishlistCurrentPage = response.data.currentPage;
          this.wishlistTotalPages = response.data.totalPages;
          this.wishlistTotalElements = response.data.totalElements;
          console.log(`✅ Loaded ${this.favoriteProperties.length} favorite properties (page ${this.wishlistCurrentPage + 1}/${this.wishlistTotalPages})`);
        }
        this.isLoadingWishlist = false;
      },
      error: (error) => {
        console.error('❌ Error loading wishlist:', error);
        this.favoriteProperties = [];
        this.filteredProperties = [];
        this.isLoadingWishlist = false;
      }
    });
  }

  // Handle wishlist page change
  onWishlistPageChange(page: number): void {
    this.loadWishlist(page);
  }
  
  // Filter properties by search query
  filterProperties(): void {
    if (!this.searchQuery.trim()) {
      this.filteredProperties = this.favoriteProperties;
      return;
    }
    
    const query = this.searchQuery.toLowerCase().trim();
    this.filteredProperties = this.favoriteProperties.filter(property => 
      property.name.toLowerCase().includes(query) ||
      property.locationName?.toLowerCase().includes(query) ||
      property.cityName?.toLowerCase().includes(query) ||
      property.hostName?.toLowerCase().includes(query)
    );
    
    console.log(`🔍 Search: "${this.searchQuery}" - Found ${this.filteredProperties.length} properties`);
  }

  // ==================== PROMOTIONS METHODS ====================
  
  loadPromotions(page: number = 0): void {
    console.log('🔄 Loading promotions... page:', page);

    this.isLoadingPromotions = true;
    this.promotionsError = '';
    console.log('📡 Calling PromotionService.getMyPromotions()...');
    
    this.promotionService.getMyPromotions(page, this.promotionsPageSize).subscribe({
      next: (response: ApiResponse<PageContent<UserPromotionDTO>>) => {
        console.log('📥 Received response from PromotionService');
        console.log('   Code:', response.code);
        console.log('   Message:', response.message);
        console.log('   Response data:', response.data);
        
        if (response.code === 200) {
          // Check if data is null (backend error)
          if (!response.data) {
            console.error('❌ Response data is null - backend error');
            this.promotionsError = response.message || 'Backend error: Invalid promotion data';
            this.promotions = [];
            this.activePromotions = [];
            this.usedPromotions = [];
            this.expiredPromotions = [];
            this.promotionsTotalElements = 0;
            this.promotionsTotalPages = 0;
            this.promotionsCurrentPage = 0;
          } else {
            // Data is valid
            this.promotions = response.data.content || [];
            this.promotionsCurrentPage = response.data.pageNumber || 0;
            this.promotionsTotalPages = response.data.totalPages || 0;
            this.promotionsTotalElements = response.data.totalElements || 0;
            console.log('✅ Promotions loaded successfully');
            console.log(`   Total promotions: ${this.promotionsTotalElements} (page ${this.promotionsCurrentPage + 1}/${this.promotionsTotalPages})`);
            this.categorizePromotions();
          }
        } else {
          console.error('❌ Unexpected response code:', response.code);
          this.promotionsError = response.message || 'Không thể tải mã giảm giá';
        }
        this.isLoadingPromotions = false;
      },
      error: (err) => {
        console.error('❌ Error loading promotions');
        console.error('   Status:', err.status);
        console.error('   Message:', err.message);
        console.error('   Full error:', err);
        this.promotionsError = 'Đã có lỗi xảy ra khi tải mã giảm giá';
        this.isLoadingPromotions = false;
      }
    });
  }

  // Handle promotions page change
  onPromotionsPageChange(page: number): void {
    this.loadPromotions(page);
  }

  categorizePromotions(): void {
    console.log('📊 Categorizing promotions...');
    
    // ✅ FIXED: Use new API response structure with `active` (boolean) and `promotionUsages` array
    // - active: true/false
    // - promotionUsages: array of usage records
    // - expiresDate: ISO DateTime string
    
    const now = new Date();
    
    // Active: active=true, not expired, not used yet
    this.activePromotions = this.promotions.filter(p => {
      const expiresDate = new Date(p.expiresDate);
      const isNotExpired = expiresDate > now;
      const isNotUsed = !p.promotionUsages || p.promotionUsages.length === 0;
      return p.active && isNotExpired && isNotUsed;
    });

    // Used: has promotionUsages records
    this.usedPromotions = this.promotions.filter(p => {
      return p.promotionUsages && p.promotionUsages.length > 0;
    });

    // Expired: past expiration date OR active=false (but not used)
    this.expiredPromotions = this.promotions.filter(p => {
      const expiresDate = new Date(p.expiresDate);
      const isExpired = expiresDate <= now;
      const isNotUsed = !p.promotionUsages || p.promotionUsages.length === 0;
      return (!p.active || isExpired) && isNotUsed;
    });
    
    console.log('   Active:', this.activePromotions.length);
    console.log('   Used:', this.usedPromotions.length);
    console.log('   Expired:', this.expiredPromotions.length);
    console.log('=======================================');
  }

  // ==================== BOOKINGS METHODS ====================
  
  loadBookings(page: number = 0): void {
    console.log('🔄 Loading bookings... page:', page);
    const userId = this.tokenService.getUserId();
    console.log('   User ID from JWT:', userId);
    
    if (!userId) {
      console.warn('⚠️ No user ID found in cache - user not logged in or cache not initialized');
      this.bookingsError = 'Vui lòng đăng nhập để xem đặt phòng';
      this.isLoadingBookings = false;
      return;
    }

    this.isLoadingBookings = true;
    this.bookingsError = '';
    console.log('📡 Calling BookingService.getUserBookings()...');
    
    // Load bookings sorted by createdAt DESC (newest first)
    this.bookingService.getUserBookings(
      userId, 
      page, 
      this.bookingsPageSize,
      'createdAt',  // Sort by creation date
      'DESC',       // Newest first
      undefined     // No status filter - load all bookings
    ).subscribe({
      next: (response) => {
        console.log('📥 Received response from BookingService');
        console.log('   Code:', response.code);
        console.log('   Message:', response.message);
        
        if (response.code === 200) {
          this.bookings = response.data.content;
          this.bookingsCurrentPage = response.data.currentPage;
          this.bookingsTotalPages = response.data.totalPages;
          this.bookingsTotalElements = response.data.totalElements;
          console.log('✅ Bookings loaded successfully');
          console.log(`   Total bookings: ${this.bookingsTotalElements} (page ${this.bookingsCurrentPage + 1}/${this.bookingsTotalPages})`);
          console.log('🔍 BOOKING ORDER CHECK:');
          this.bookings.forEach((b, idx) => {
            console.log(`   [${idx}] ID: ${b.id}, Created: ${b.createdAt}, Status: ${b.status}`);
          });
          this.categorizeBookings();
        } else {
          console.error('❌ Unexpected response code:', response.code);
          this.bookingsError = response.message || 'Không thể tải danh sách đặt phòng';
        }
        this.isLoadingBookings = false;
      },
      error: (err) => {
        console.error('❌ Error loading bookings');
        console.error('   Status:', err.status);
        console.error('   Message:', err.message);
        console.error('   Full error:', err);
        this.bookingsError = 'Đã có lỗi xảy ra khi tải danh sách đặt phòng';
        this.isLoadingBookings = false;
      }
    });
  }

  // Handle bookings page change
  onBookingsPageChange(page: number): void {
    this.loadBookings(page);
  }

  categorizeBookings(): void {
    console.log('📊 Categorizing bookings...');
    const now = new Date();
    
    // Upcoming: CONFIRMED bookings with future check-in date, sorted by check-in date (earliest first)
    this.upcomingBookings = this.bookings.filter(b => {
      const checkIn = new Date(b.checkIn);
      return checkIn > now && b.status === 'CONFIRMED';
    }).sort((a, b) => new Date(a.checkIn).getTime() - new Date(b.checkIn).getTime());

    // Past: All other bookings, sorted by createdAt DESC (newest first)
    this.pastBookings = this.bookings.filter(b => {
      const checkOut = new Date(b.checkOut);
      return checkOut <= now || b.status !== 'CONFIRMED';
    }).sort((a, b) => {
      // Sort by createdAt DESC (newest first)
      const dateA = a.createdAt ? new Date(a.createdAt).getTime() : 0;
      const dateB = b.createdAt ? new Date(b.createdAt).getTime() : 0;
      return dateB - dateA;
    });
    
    console.log('   Upcoming:', this.upcomingBookings.length);
    console.log('   Past:', this.pastBookings.length);
    console.log('   Past bookings order:');
    this.pastBookings.slice(0, 3).forEach((b, idx) => {
      console.log(`      [${idx}] ID: ${b.id}, Created: ${b.createdAt}, Status: ${b.status}`);
    });
    console.log('=======================================');
  }

  getBookingStatusBadgeClass(status: string | BookingStatus | undefined): string {
    if (!status) return 'badge bg-secondary';
    
    const statusStr = status.toString();
    
    switch (statusStr) {
      case 'CONFIRMED':
      case 'ACTIVE':
        return 'badge bg-success';
      case 'PAID':
        return 'badge bg-info';
      case 'COMPLETED':
        return 'badge bg-primary';
      case 'CANCELLED':
        return 'badge bg-danger';
      case 'REJECTED':
        return 'badge bg-danger';
      case 'PENDING':
        return 'badge bg-warning';
      default:
        return 'badge bg-secondary';
    }
  }

  getBookingStatusLabel(status: string | BookingStatus | undefined): string {
    if (!status) return 'Unknown';
    
    const statusStr = status.toString();
    
    switch (statusStr) {
      case 'CONFIRMED':
      case 'ACTIVE':
        return 'Đang hoạt động';
      case 'PAID':
        return 'Đã thanh toán';
      case 'COMPLETED':
        return 'Đã hoàn thành';
      case 'CANCELLED':
        return 'Đã hủy';
      case 'REJECTED':
        return 'Đã từ chối';
      case 'PENDING':
        return 'Chờ xác nhận';
      default:
        return statusStr;
    }
  }

  getBookingStatusClass(status: string | BookingStatus | undefined): string {
    if (!status) return 'badge bg-secondary';
    
    const statusStr = status.toString();
    
    switch (statusStr) {
      case 'CONFIRMED':
      case 'ACTIVE':
        return 'badge bg-success';
      case 'PAID':
        return 'badge bg-info';
      case 'COMPLETED':
        return 'badge bg-primary';
      case 'CANCELLED':
        return 'badge bg-danger';
      case 'REJECTED':
        return 'badge bg-danger';
      case 'PENDING':
        return 'badge bg-warning';
      default:
        return 'badge bg-secondary';
    }
  }

  getBookingStatusText(status: string | BookingStatus | undefined): string {
    if (!status) return 'Unknown';
    
    const statusStr = status.toString();
    
    switch (statusStr) {
      case 'CONFIRMED':
      case 'ACTIVE':
        return 'Đang hoạt động';
      case 'PAID':
        return 'Đã thanh toán';
      case 'COMPLETED':
        return 'Đã hoàn thành';
      case 'CANCELLED':
        return 'Đã hủy';
      case 'REJECTED':
        return 'Đã từ chối';
      case 'PENDING':
        return 'Chờ xác nhận';
      default:
        return statusStr;
    }
  }

  getTotalGuests(booking: Booking): number {
    return booking.numAdults + booking.numChildren + booking.num_infant;
  }

  getGuestsText(booking: Booking): string {
    const parts: string[] = [];
    if (booking.numAdults > 0) parts.push(`${booking.numAdults} người lớn`);
    if (booking.numChildren > 0) parts.push(`${booking.numChildren} trẻ em`);
    if (booking.num_infant > 0) parts.push(`${booking.num_infant} trẻ sơ sinh`);
    return parts.join(', ') || '0 khách';
  }

  getNumberOfNights(checkIn: string, checkOut: string): number {
    const checkInDate = new Date(checkIn);
    const checkOutDate = new Date(checkOut);
    const diffTime = Math.abs(checkOutDate.getTime() - checkInDate.getTime());
    const diffDays = Math.ceil(diffTime / (1000 * 60 * 60 * 24));
    return diffDays;
  }

  navigateToProperty(propertyId: number): void {
    console.log('🏠 Navigating to property detail:', propertyId);
    this.router.navigate(['/properties', propertyId]);
  }

  /**
   * Check if booking can be cancelled
   * All bookings EXCEPT COMPLETED and CANCELLED can be cancelled
   */
  canCancel(booking: Booking): boolean {
    return booking.status !== 'COMPLETED' && booking.status !== 'CANCELLED';
  }

  /**
   * Check if booking can be deleted
   * Only CANCELLED bookings can be deleted
   */
  canDelete(booking: Booking): boolean {
    return booking.status === 'CANCELLED';
  }

  /**
   * Cancel a booking
   */
  async cancelBooking(bookingId: number | undefined): Promise<void> {
    if (!bookingId) return;

    // Show beautiful modal with input field for cancellation reason
    const reason = await this.modalService.showConfirm(
      'Xác nhận hủy booking',
      'Bạn có chắc chắn muốn hủy booking này? Hành động này không thể hoàn tác.',
      {
        primaryButton: 'Xác nhận hủy',
        cancelButton: 'Không, giữ lại',
        showInput: true,
        inputLabel: 'Lý do hủy (tùy chọn)',
        inputPlaceholder: 'Nhập lý do hủy booking...',
        isDanger: true
      }
    );

    // If user cancelled the modal
    if (reason === null) {
      console.log('❌ User cancelled booking cancellation');
      return;
    }

    console.log(`🚫 Cancelling booking #${bookingId} with reason:`, reason || '(no reason provided)');

    // Pass reason to service
    this.bookingService.cancelBooking(bookingId, reason || undefined).subscribe({
      next: (response) => {
        if (response.code === 200) {
          console.log('✅ Booking cancelled');
          this.modalService.showSuccess('Đã hủy booking thành công');
          this.loadBookings(this.bookingsCurrentPage); // Reload current page
        } else {
          this.modalService.showError(response.message || 'Không thể hủy booking');
        }
      },
      error: (error) => {
        console.error('❌ Error cancelling booking:', error);
        this.modalService.showError('Không thể hủy booking. Vui lòng thử lại.');
      }
    });
  }

  /**
   * Delete a cancelled booking
   */
  async deleteBooking(bookingId: number | undefined): Promise<void> {
    if (!bookingId) return;

    // Show confirmation modal
    const confirmed = await this.modalService.showConfirm(
      'Xác nhận xóa booking',
      'Bạn có chắc chắn muốn xóa booking này? Hành động này không thể hoàn tác.',
      {
        primaryButton: 'Xóa booking',
        cancelButton: 'Hủy',
        isDanger: true
      }
    );

    // If user cancelled
    if (confirmed === null) {
      console.log('❌ User cancelled booking deletion');
      return;
    }

    console.log(`🗑️ Deleting booking #${bookingId}`);
    this.bookingService.deleteBooking(bookingId).subscribe({
      next: (response) => {
        if (response.code === 200) {
          console.log('✅ Booking deleted successfully');
          this.modalService.showSuccess('Đã xóa booking thành công');
          this.loadBookings(this.bookingsCurrentPage); // Reload current page
        } else {
          this.modalService.showError(response.message || 'Không thể xóa booking');
        }
      },
      error: (error) => {
        console.error('❌ Error deleting booking:', error);
        this.modalService.showError('Không thể xóa booking. Vui lòng thử lại.');
      }
    });
  }

  formatCurrency(amount: number | undefined): string {
    if (amount === undefined || amount === null) {
      return '0đ';
    }
    return amount.toLocaleString('vi-VN') + 'đ';
  }

  isExpiringSoon(expiresDate: string): boolean {
    const expires = new Date(expiresDate);
    const now = new Date();
    const daysUntilExpiry = Math.floor((expires.getTime() - now.getTime()) / (1000 * 60 * 60 * 24));
    return daysUntilExpiry <= 7 && daysUntilExpiry > 0;
  }

  copyPromotionCode(code: string): void {
    navigator.clipboard.writeText(code).then(() => {
      this.modalService.showSuccess(`Đã sao chép mã: ${code}`);
    }).catch(err => {
      console.error('Failed to copy code:', err);
      this.modalService.showError('Không thể sao chép mã');
    });
  }

  // NOTE: Delete promotion feature removed - promotions are managed by backend
  // User cannot delete claimed promotions, they expire or get used automatically

  // ==================== REVIEWS METHODS ====================
  
  loadReviews(): void {
    console.log('🔄 Loading reviews...');
    const userId = this.tokenService.getUserId();
    console.log('   User ID from JWT:', userId);
    
    if (!userId) {
      console.warn('⚠️ No user ID found in cache - user not logged in or cache not initialized');
      this.reviewsError = 'Vui lòng đăng nhập để xem đánh giá';
      this.isLoadingReviews = false;
      return;
    }

    this.isLoadingReviews = true;
    this.reviewsError = '';
    console.log('📡 Calling ReviewService.getUserReviews()...');
    
    this.reviewService.getUserReviews(userId).subscribe({
      next: (response) => {
        console.log('📥 Received response from ReviewService');
        console.log('   Code:', response.code);
        console.log('   Message:', response.message);
        
        if (response.code === 200) {
          this.reviews = Array.isArray(response.data) ? response.data : [];
          console.log('✅ Reviews loaded successfully');
          console.log('   Total reviews:', this.reviews.length);
        } else {
          console.error('❌ Unexpected response code:', response.code);
          this.reviewsError = response.message || 'Không thể tải danh sách đánh giá';
        }
        this.isLoadingReviews = false;
      },
      error: (err) => {
        console.error('❌ Error loading reviews');
        console.error('   Status:', err.status);
        console.error('   Message:', err.message);
        console.error('   Full error:', err);
        this.reviewsError = 'Đã có lỗi xảy ra khi tải danh sách đánh giá';
        this.isLoadingReviews = false;
      }
    });
  }

  startEditReview(review: Review): void {
    console.log('✏️ Starting edit for review:', review.reviewId);
    this.editingReviewId = review.reviewId;
    this.editingReviewData = {
      comment: review.comment,
      rating: review.rating
    };
  }

  cancelEditReview(): void {
    console.log('❌ Cancelled edit');
    this.editingReviewId = null;
    this.editingReviewData = null;
  }

  saveEditReview(review: Review): void {
    if (!this.editingReviewData) {
      console.error('❌ No editing data found');
      return;
    }

    const userId = this.tokenService.getUserId();
    if (!userId) {
      alert('Không thể xác định user ID');
      return;
    }

    const updateData: UpdateReviewDTO = {
      userId: userId,
      propertyId: review.propertyId,
      comment: this.editingReviewData.comment,
      rating: this.editingReviewData.rating
    };

    console.log('💾 Saving review:', review.reviewId, updateData);

    this.reviewService.updateReview(review.reviewId, updateData).subscribe({
      next: (response) => {
        console.log('✅ Review updated successfully');
        this.modalService.showSuccess('Đã cập nhật đánh giá thành công!');
        this.editingReviewId = null;
        this.editingReviewData = null;
        // Reload reviews list
        this.loadReviews();
      },
      error: (err) => {
        console.error('❌ Error updating review:', err);
        this.modalService.showError('Không thể cập nhật đánh giá. Vui lòng thử lại!');
      }
    });
  }

  deleteReview(reviewId: number): void {
    const confirmDelete = confirm('Bạn có chắc chắn muốn xóa đánh giá này không?');
    
    if (!confirmDelete) {
      console.log('❌ User cancelled review deletion');
      return;
    }

    const userId = this.tokenService.getUserId();
    if (!userId) {
      this.modalService.showError('Không thể xác định user ID');
      return;
    }

    console.log('🗑️ Deleting review:', reviewId);
    this.reviewService.deleteReview(reviewId, userId).subscribe({
      next: (response) => {
        console.log('✅ Review deleted successfully');
        this.modalService.showSuccess('Đã xóa đánh giá thành công!');
        // Reload reviews list
        this.loadReviews();
      },
      error: (err) => {
        console.error('❌ Error deleting review:', err);
        this.modalService.showError('Không thể xóa đánh giá. Vui lòng thử lại!');
      }
    });
  }

  getStarsArray(rating: number): number[] {
    return Array(5).fill(0).map((_, i) => i + 1);
  }

  setRating(rating: number): void {
    if (this.editingReviewData) {
      this.editingReviewData.rating = rating;
    }
  }

  shouldShowUpdatedDate(review: Review): boolean {
    if (!review.updatedDate) return false;
    const created = new Date(review.reviewDate);
    const updated = new Date(review.updatedDate);
    // Compare dates (ignore milliseconds)
    return Math.abs(updated.getTime() - created.getTime()) > 1000;
  }

  // Handle avatar image error (fallback to default)
  onAvatarError(): void {
    console.warn('⚠️ Avatar image failed to load, using default');
    this.avatarUrl = 'assets/img/default-avatar.svg';
  }

  // Map property type string to number
  mapPropertyTypeToNumber(propertyType: string | number): number {
    if (typeof propertyType === 'number') {
      return propertyType;
    }
    
    const typeMap: { [key: string]: number } = {
      'APARTMENT': 0,
      'HOUSE': 1,
      'HOTEL': 2,
      'VILLA': 1, // Map VILLA to House
      'CONDO': 0, // Map CONDO to Apartment
      'RESORT': 2 // Map RESORT to Hotel
    };
    
    return typeMap[propertyType.toUpperCase()] || 0;
  }

  // ==================== HOST PROPERTIES METHODS ====================
  
  loadHostProperties(page: number = 0): void {
    if (!this.userResponse?.id) {
      console.warn('⚠️ User ID not available, cannot load host properties');
      return;
    }

    if (!this.isHost()) {
      console.warn('⚠️ User is not a host, cannot load properties');
      this.hostPropertiesError = 'Chỉ host mới có thể quản lý chỗ ở';
      return;
    }
    
    console.log('🔄 Loading host properties... page:', page);
    console.log('   🌐 API Call: PropertyService.getPropertiesByHost()');
    console.log('   Host ID:', this.userResponse.id);
    console.log('   Search query:', this.propertySearchQuery);
    console.log('   Sort by:', this.propertySortBy, this.propertySortDirection);
    
    this.isLoadingHostProperties = true;
    this.hostPropertiesError = '';
    
    this.propertyService.getPropertiesByHost(
      this.userResponse.id,
      page,
      this.hostPropertiesPageSize,
      this.propertySearchQuery,
      this.propertySortBy,
      this.propertySortDirection
    ).subscribe({
      next: (response) => {
        console.log('✅ Host properties loaded successfully');
        console.log('   Response:', response);
        
        if (response.code === 200 && response.data) {
          const data = response.data;
          
          // Map API response to Property model (handle field name differences)
          this.hostProperties = (data.content || []).map((item: any) => ({
            id: item.id,
            name: item.propertyName || item.name,
            rating: item.averageRating || item.rating || 0,
            hostName: item.hostName,
            address: item.address,
            locationName: item.wardCommune || item.locationName || '',
            cityName: item.cityProvince || item.cityName || '',
            pricePerNight: item.pricePerNight,
            numberOfBedrooms: item.numBedrooms || item.numberOfBedrooms || 0,
            numberOfBathrooms: item.numBathrooms || item.numberOfBathrooms || 0,
            maxAdults: item.maxGuests || item.maxAdults || 0,
            maxChildren: item.maxChildren || 0,
            maxInfants: item.maxInfants || 0,
            maxPets: item.maxPets || 0,
            propertyType: this.mapPropertyTypeToNumber(item.propertyType),
            description: item.description || '',
            images: (item.propertyImages || item.images || []).map((img: any) => ({
              imageId: img.id || img.imageId,
              imageUrl: img.imageUrl,
              description: img.description || '',
              createDate: img.createdAt || img.createDate || '',
              updateDate: img.updatedAt || img.updateDate || ''
            })),
            reviews: [],
            amenities: (item.propertyAmenities || item.amenities || []).map((amenity: any) => ({
              id: amenity.id,
              iconUrl: amenity.iconUrl || '',
              amenityName: amenity.name || amenity.amenityName || '',
              description: amenity.description || ''
            })),
            facilities: [],
            nameUserFavorites: [],
            available: item.isAvailable !== undefined ? item.isAvailable : true,
            guestFavorite: false,
            createdAt: item.createdAt,
            updatedAt: item.updatedAt,
            createDate: item.createdAt
          }));
          
          this.filteredHostProperties = this.hostProperties;
          this.hostPropertiesCurrentPage = (data as any).pageNumber || data.currentPage || 0;
          this.hostPropertiesTotalPages = data.totalPages || 0;
          this.hostPropertiesTotalElements = data.totalElements || 0;
          
          console.log(`   Total properties: ${this.hostPropertiesTotalElements} (page ${this.hostPropertiesCurrentPage + 1}/${this.hostPropertiesTotalPages})`);
          console.log(`   Loaded ${this.hostProperties.length} properties on this page`);
          
          if (this.hostProperties.length > 0) {
            console.log('   📋 Sample property:', this.hostProperties[0]);
          }
        } else {
          console.error('❌ Unexpected response code:', response.code);
          this.hostPropertiesError = response.message || 'Không thể tải danh sách chỗ ở';
        }
        this.isLoadingHostProperties = false;
      },
      error: (err) => {
        console.error('❌ Error loading host properties');
        console.error('   Status:', err.status);
        console.error('   Message:', err.message);
        console.error('   Full error:', err);
        this.hostPropertiesError = 'Đã có lỗi xảy ra khi tải danh sách chỗ ở';
        this.isLoadingHostProperties = false;
      }
    });
  }

  // Handle host properties page change
  onHostPropertiesPageChange(page: number): void {
    this.loadHostProperties(page);
  }

  filterHostProperties(): void {
    console.log('🔍 Filtering host properties with query:', this.propertySearchQuery);
    this.loadHostProperties(0); // Reset to first page when filtering
  }

  sortHostProperties(sortBy: string): void {
    console.log('📊 Sorting host properties by:', sortBy);
    
    // Toggle sort direction if clicking same field
    if (this.propertySortBy === sortBy) {
      this.propertySortDirection = this.propertySortDirection === 'ASC' ? 'DESC' : 'ASC';
    } else {
      this.propertySortBy = sortBy;
      this.propertySortDirection = 'DESC';
    }
    
    this.loadHostProperties();
  }

  togglePropertySort(): void {
    console.log('🔄 Toggling property sort direction');
    this.propertySortDirection = this.propertySortDirection === 'DESC' ? 'ASC' : 'DESC';
    this.loadHostProperties();
  }

  deleteHostProperty(propertyId: number, propertyName: string): void {
    const confirmDelete = confirm(`Bạn có chắc chắn muốn xóa chỗ ở "${propertyName}" không?\n\nHành động này không thể hoàn tác!`);
    
    if (!confirmDelete) {
      console.log('❌ User cancelled property deletion');
      return;
    }

    console.log('🗑️ Deleting property:', propertyId);
    this.propertyService.deleteProperty(propertyId).subscribe({
      next: (response) => {
        console.log('✅ Property deleted successfully');
        this.modalService.showSuccess('Đã xóa chỗ ở thành công!');
        // Reload properties list
        this.loadHostProperties();
      },
      error: (err) => {
        console.error('❌ Error deleting property:', err);
        const errorMessage = err?.error?.message || 'Không thể xóa chỗ ở. Vui lòng thử lại!';
        this.modalService.showError(errorMessage);
      }
    });
  }

  editHostProperty(propertyId: number): void {
    console.log('✏️ Navigating to edit property:', propertyId);
    this.router.navigate(['/host/edit-property', propertyId]);
  }

  getPropertyImage(property: Property): string {
    if (property.images && property.images.length > 0) {
      const imageUrl = property.images[0].imageUrl;
      if (imageUrl.startsWith('http')) {
        return imageUrl;
      }
      const cleanPath = imageUrl.startsWith('/') ? imageUrl : `/${imageUrl}`;
      return `${this.baseUrl}${cleanPath}`;
    }
    return '/assets/img/placeholder.svg';
  }

  getPropertyTypeText(propertyType: number): string {
    const types: { [key: number]: string } = {
      0: 'Apartment',
      1: 'House',
      2: 'Hotel'
    };
    return types[propertyType] || 'Property';
  }
}