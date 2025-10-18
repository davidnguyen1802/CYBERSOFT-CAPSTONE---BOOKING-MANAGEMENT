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
import { UserResponse } from '../../responses/user/user.response';
import { UpdateUserDTO } from '../../dtos/user/update.user.dto';
import { Property } from '../../models/property';
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

  constructor(
    private formBuilder: FormBuilder,
    private activatedRoute: ActivatedRoute,
    private userService: UserService,
    private router: Router,
    private tokenService: TokenService,
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
    this.token = this.tokenService.getToken();
    
    // Check if user is logged in
    if (!this.token || this.tokenService.isTokenExpired()) {
      console.warn('⚠️ No valid token found, redirecting to login');
      alert('Please login to view your profile');
      this.router.navigate(['/login']);
      return;
    }

    console.log('✅ Valid token found, loading user profile');
    // Load user profile from backend API
    this.loadUserProfile();
  }

  loadUserProfile(): void {
    console.log('📥 Loading user profile from backend API...');
    this.isLoading = true;
    
    // Call backend API to get detailed user profile
    this.userService.getMyDetailedProfile(this.token, true).subscribe({
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
        alert('Failed to load profile. Please login again.');
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
              alert('Profile updated successfully!');
              this.isEditMode = false;
              
              // Reload profile data from backend
              this.loadUserProfile();
            } else {
              console.warn('⚠️ Unexpected response format:', response);
              alert('Profile updated, but response was unexpected');
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
            const errorMessage = error?.error?.message || error?.message || 'Failed to update profile';
            alert(`Error: ${errorMessage}`);
          }
        });
    } else {
      console.warn('⚠️ Form validation failed');
      if (this.userProfileForm.hasError('passwordMismatch')) {
        console.warn('⚠️ Password mismatch');
        alert('Passwords do not match');
      } else {
        console.warn('⚠️ Form errors:', this.userProfileForm.errors);
        alert('Please fill in all required fields correctly');
      }
    }
  }

  logout(): void {
    console.log('🚪 Logging out user');
    // Clear all tokens and user data
    this.tokenService.removeToken();
    this.userService.removeUserFromLocalStorage(); // Clean up any residual user data
    console.log('✅ User data cleared, redirecting to login');
    this.router.navigate(['/login']);
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
    if (!date) return 'Not specified';
    return new Date(date).toLocaleDateString();
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

  getUserId(): number {
    return this.tokenService.getUserId();
  }

  // Set active section in sidebar
  setActiveSection(section: string): void {
    console.log(`📂 Switching to section: ${section}`);
    this.activeSection = section;
    // Exit edit mode when switching sections
    if (this.isEditMode) {
      this.isEditMode = false;
      this.userProfileForm.reset();
    }
    
    // Load wishlist when switching to wishlist section
    if (section === 'wishlist') {
      this.loadWishlist();
    }
  }
  
  // Load user's favorite properties
  loadWishlist(): void {
    if (!this.userResponse?.id) {
      console.warn('⚠️ User ID not available, cannot load wishlist');
      return;
    }
    
    console.log('🔵 Loading wishlist for user:', this.userResponse.id);
    this.isLoadingWishlist = true;
    this.favoriteProperties = [];
    
    this.userService.getFavoriteProperties(this.userResponse.id, this.token).subscribe({
      next: (response) => {
        console.log('✅ Wishlist API response:', response);
        if (response && response.data) {
          this.favoriteProperties = response.data;
          this.filteredProperties = response.data; // Initialize filtered list
          console.log(`✅ Loaded ${this.favoriteProperties.length} favorite properties`);
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

  // Handle avatar image error (fallback to default)
  onAvatarError(): void {
    console.warn('⚠️ Avatar image failed to load, using default');
    this.avatarUrl = 'assets/img/default-avatar.svg';
  }
}

