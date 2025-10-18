import { Component, Input, OnInit } from '@angular/core';
import { Property } from '../../../models/property';
import { getBaseUrl } from '../../../utils/url.util';
import { UserService } from '../../../services/user.service';
import { TokenService } from '../../../services/token.service';
import { AuthStateService } from '../../../services/auth-state.service';

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

  constructor(
    private userService: UserService,
    private tokenService: TokenService,
    private authStateService: AuthStateService
  ) {}

  ngOnInit(): void {
    // Check if user is logged in
    this.checkLoginStatus();
    
    // Subscribe to login state changes
    this.authStateService.loginState$.subscribe(() => {
      this.checkLoginStatus();
    });
  }

  checkLoginStatus(): void {
    const token = this.tokenService.getToken();
    this.isLoggedIn = !!(token && !this.tokenService.isTokenExpired());
    
    if (this.isLoggedIn) {
      this.userId = this.tokenService.getUserId();
      // Check if this property is favorited
      this.checkIfFavorite();
    } else {
      this.isFavorite = false;
    }
  }

  checkIfFavorite(): void {
    if (!this.isLoggedIn || !this.userId || !this.property?.id) return;
    
    const token = this.tokenService.getToken();
    if (!token) return;

    this.userService.checkFavorite(this.userId, this.property.id, token).subscribe({
      next: (response: any) => {
        console.log(`❤️ Favorite check for property ${this.property.id}:`, response);
        // Assume API returns { data: true/false } or { data: { isFavorite: true/false } }
        if (response && response.data !== undefined) {
          this.isFavorite = response.data === true || response.data.isFavorite === true;
        }
      },
      error: (error: any) => {
        console.error(`❌ Error checking favorite for property ${this.property.id}:`, error);
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
      // Nếu không, ghép với baseUrl
      return `${this.baseUrl}${imageUrl}`;
    }
    return '/assets/img/placeholder.svg';
  }

  toggleFavorite(event: Event): void {
    event.stopPropagation();
    event.preventDefault();
    
    // Check if user is logged in
    if (!this.isLoggedIn) {
      alert('Vui lòng đăng nhập để sử dụng tính năng Wishlist!');
      return;
    }

    if (this.isLoading) return; // Prevent multiple clicks
    
    const token = this.tokenService.getToken();
    if (!token || !this.userId) {
      alert('Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại!');
      return;
    }

    this.isLoading = true;

    if (this.isFavorite) {
      // Remove from favorites
      console.log(`💔 Removing property ${this.property.id} from favorites...`);
      this.userService.removeFromFavorites(this.userId, this.property.id, token).subscribe({
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
      this.userService.addToFavorites(this.userId, this.property.id, token).subscribe({
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
