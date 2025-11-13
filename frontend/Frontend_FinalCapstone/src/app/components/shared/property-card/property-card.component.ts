import { Component, Input, OnInit } from '@angular/core';
import { Property } from '../../../models/property';
import { getBaseUrl } from '../../../utils/url.util';
import { UserService } from '../../../services/user.service';
import { TokenService } from '../../../services/token.service';
import { AuthStateService } from '../../../services/auth-state.service';
import { SimpleModalService } from '../../../services/simple-modal.service';

@Component({
  selector: 'app-property-card',
  templateUrl: './property-card.component.html',
  styleUrls: ['./property-card.component.scss']
})
export class PropertyCardComponent implements OnInit {
  @Input() property!: Property;

  isFavorite: boolean = false;
  baseUrl: string = getBaseUrl();
  isLoggedIn: boolean = false;
  userId: number = 0;
  isLoading: boolean = false;
  
  private loginSubscription?: any; // Store subscription for cleanup

  constructor(
    private userService: UserService,
    private tokenService: TokenService,
    private authStateService: AuthStateService,
    private modalService: SimpleModalService
  ) {}

  ngOnInit(): void {
    // Subscribe to login state changes (store subscription for cleanup)
    // This will fire ONCE on subscribe if loginState$ is a BehaviorSubject
    this.loginSubscription = this.authStateService.loginState$.subscribe(() => {
      this.checkLoginStatus();
    });
    
    // NOTE: Don't call checkLoginStatus() manually here - subscription will handle it
  }
  
  ngOnDestroy(): void {
    // CRITICAL: Unsubscribe to prevent memory leaks
    if (this.loginSubscription) {
      this.loginSubscription.unsubscribe();
    }
  }

  checkLoginStatus(): void {
    const token = this.tokenService.getToken();
    
    // NOTE: We only check if token EXISTS, not if it's expired
    // Backend will return 401 if token is invalid/expired
    this.isLoggedIn = !!token;
    console.log('🔐 PROPERTY-CARD: Checking login status for property ID:', this.property.id);
    console.log('   Has token:', !!token);
    console.log('   isLoggedIn:', this.isLoggedIn);
    
    if (this.isLoggedIn) {
      // Decode JWT to get userId
      console.log('🔍 Decoding JWT to get userId...');
      const startTime = performance.now();
      this.userId = this.tokenService.getUserId();
      const decodeTime = performance.now() - startTime;
      console.log(`✅ User ID decoded in ${decodeTime.toFixed(2)}ms:`, this.userId);
      
      // Only check favorite if we got a valid userId from cache
      if (this.userId > 0) {
        console.log('🔍 Checking if property is favorite...');
        this.checkIfFavorite();
      } else {
        console.log('❌ Invalid user ID → Setting isFavorite = false');
        this.isFavorite = false;
      }
    } else {
      console.log('❌ User not logged in → Setting isFavorite = false');
      this.isFavorite = false;
      this.userId = 0;
    }
  }

  checkIfFavorite(): void {
    if (!this.isLoggedIn || !this.userId || !this.property?.id) return;

    this.userService.checkFavorite(this.userId, this.property.id).subscribe({
      next: (response: any) => {
        console.log(`❤️ Favorite check for property ${this.property.id}:`, response);
        // Assume API returns { data: true/false } or { data: { isFavorite: true/false } }
        if (response && response.data !== undefined) {
          this.isFavorite = response.data === true || response.data.isFavorite === true;
        }
      },
      error: (error: any) => {
        // IMPORTANT: Favorite check is OPTIONAL - 401 just means not logged in
        // Don't trigger auto-refresh or logout
        if (error.status === 401) {
          console.log(`ℹ️ Token invalid for favorite check - property ${this.property.id}`);
        } else {
          console.error(`❌ Error checking favorite for property ${this.property.id}:`, error);
        }
        this.isFavorite = false;
      }
    });
  }

  getImageUrl(): string {
    if (this.property.images && this.property.images.length > 0) {
      const imageUrl = this.property.images[0].imageUrl;
      // Nếu imageUrl đã là full URL (bắt đầu với http), dùng trực tiếp
      if (imageUrl.startsWith('http')) {
        return imageUrl;
      }
      // Nếu không, ghép với baseUrl - ensure leading slash
      const cleanPath = imageUrl.startsWith('/') ? imageUrl : `/${imageUrl}`;
      return `${this.baseUrl}${cleanPath}`;
    }
    return '/assets/img/placeholder.svg';
  }

  toggleFavorite(event: Event): void {
    event.stopPropagation();
    event.preventDefault();
    
    // Check if user is logged in (anonymous mode)
    if (!this.isLoggedIn) {
      console.warn('⚠️ PropertyCard: Anonymous user trying to toggle favorite');
      // Show login required modal
      this.modalService.showLoginRequired();
      return;
    }

    if (this.isLoading) return; // Prevent multiple clicks
    
    // Note: Token expiration is handled by TokenInterceptor automatically
    // If token is expired/invalid, interceptor will try to refresh or show session expired modal
    const userId = this.userId;

    this.isLoading = true;

    if (this.isFavorite) {
      // Remove from favorites
      console.log(`💔 Removing property ${this.property.id} from favorites...`);
      this.userService.removeFromFavorites(userId, this.property.id).subscribe({
        next: (response: any) => {
          console.log('✅ Removed from favorites:', response);
          this.isFavorite = false;
          // Show success message (optional)
          // You can use a toast/snackbar service here
          console.log('✅ Đã xóa khỏi danh sách yêu thích');
          this.isLoading = false;
        },
        error: (error: any) => {
          console.error('❌ Error removing from favorites:', error);
          alert('Có lỗi xảy ra khi xóa khỏi danh sách yêu thích. Vui lòng thử lại!');
          this.isLoading = false;
        }
      });
    } else {
      // Add to favorites
      console.log(`💖 Adding property ${this.property.id} to favorites...`);
      this.userService.addToFavorites(userId, this.property.id).subscribe({
        next: (response: any) => {
          console.log('✅ Added to favorites:', response);
          if (response && response.code === 200) {
            this.isFavorite = true;
            // Show success message
            console.log('✅ Đã thêm vào danh sách yêu thích');
          } else {
            alert('Có lỗi xảy ra. Vui lòng thử lại!');
          }
          this.isLoading = false;
        },
        error: (error: any) => {
          console.error('❌ Error adding to favorites:', error);
          alert('Có lỗi xảy ra khi thêm vào danh sách yêu thích. Vui lòng thử lại!');
          this.isLoading = false;
        }
      });
    }
  }

  getPropertyTypeLabel(): string {
    const labels: { [key: number]: string } = {
      0: 'Apartment',
      1: 'House', 
      2: 'Hotel'
    };
    return labels[this.property.propertyType] || 'Property';
  }

  getRatingLabel(): string {
    if (this.property.rating >= 4.5) return 'Excellent';
    if (this.property.rating >= 4.0) return 'Very Good';
    if (this.property.rating >= 3.5) return 'Good';
    if (this.property.rating >= 3.0) return 'Fair';
    return 'Average';
  }

  onImageError(event: any): void {
    event.target.src = '/assets/img/placeholder.svg';
  }
}
