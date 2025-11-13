import { Component, OnInit } from '@angular/core';
import { Router, ActivatedRoute } from '@angular/router';
import { Property, PropertyReview } from '../../models/property';
import { PropertyService } from '../../services/property.service';
import { UserService } from '../../services/user.service';
import { TokenService } from '../../services/token.service';
import { AuthStateService } from '../../services/auth-state.service';
import { SimpleModalService } from '../../services/simple-modal.service';
import { environment } from '../../../environments/environment';
import { DateRange } from '../shared/date-range-picker/date-range-picker.component';

@Component({
  selector: 'app-detail-property',
  templateUrl: './detail-property.component.html',
  styleUrls: ['./detail-property.component.scss']
})
export class DetailPropertyComponent implements OnInit {
  property?: Property;
  propertyId: number = 0;
  currentImageIndex: number = 0;
  loading: boolean = false;
  errorMsg: string = '';
  
  // Wishlist state
  isFavorite: boolean = false;
  isLoggedIn: boolean = false;
  userId: number = 0;
  isLoadingWishlist: boolean = false;
  
  // Booking form data
  guestCounts = {
    adults: 1,
    children: 0,
    infants: 0,
    pets: 0
  };
  isPetChecked: boolean = false;
  
  // Date range picker data
  today: Date;
  oneYearLater: Date;
  blockedDates: (Date | {start: Date; end: Date})[] = [];
  initialCheckIn?: Date;
  initialCheckOut?: Date;
  selectedDateRange: DateRange | null = null;
  
  // Reviews
  displayedReviews: PropertyReview[] = [];
  showAllReviews: boolean = false;
  reviewsToShow: number = 5;
  
  // Review form
  reviewForm = {
    name: '',
    email: '',
    rating: 0,
    comment: ''
  };
  
  // Star rating interactive state
  hoverRating: number = 0;
  tempRating: number = 0;
  
  // Image modal state
  isImageModalOpen: boolean = false;
  
  constructor(
    private propertyService: PropertyService,
    private activatedRoute: ActivatedRoute,
    private router: Router,
    private userService: UserService,
    private tokenService: TokenService,
    private authStateService: AuthStateService,
    private modalService: SimpleModalService
  ) {
    const today = new Date();
    today.setHours(0, 0, 0, 0);
    this.today = today;
    
    this.oneYearLater = new Date(today);
    this.oneYearLater.setFullYear(this.oneYearLater.getFullYear() + 1);
  }
    
  ngOnInit() {
    // Get propertyId from URL
    const idParam = this.activatedRoute.snapshot.paramMap.get('id');
    if (idParam !== null) {
      this.propertyId = +idParam;
    }
    if (!isNaN(this.propertyId)) {
      this.loadPropertyDetail();
      // Load blocked dates for this property
      this.loadBlockedDates();
    }
    
    // Check login status and favorite state
    this.checkLoginAndFavoriteStatus();
    
    // Subscribe to auth state changes
    this.authStateService.loginState$.subscribe((isLoggedIn: boolean) => {
      this.isLoggedIn = isLoggedIn;
      if (isLoggedIn && this.propertyId) {
        this.checkIfFavorite();
      } else {
        this.isFavorite = false;
      }
    });
    
    // Scroll to top when component loads
    window.scrollTo(0, 0);
  }
  
  checkLoginAndFavoriteStatus(): void {
    const token = this.tokenService.getToken();
    // Don't check expiration - interceptor handles token refresh automatically
    this.isLoggedIn = !!token;
    
    if (this.isLoggedIn && this.propertyId) {
      this.userId = this.tokenService.getUserId();
      this.checkIfFavorite();
    }
  }
  
  checkIfFavorite(): void {
    if (!this.isLoggedIn || !this.propertyId) {
      return;
    }

    this.userService.checkFavorite(this.userId, this.propertyId).subscribe({
      next: (response) => {
        console.log('🔵 Check favorite status response:', response);
        this.isFavorite = response.data === true;
      },
      error: (error) => {
        console.error('❌ Error checking favorite status:', error);
      }
    });
  }

  // Smooth scroll to section
  scrollToSection(sectionId: string, event?: Event): void {
    if (event) {
      event.preventDefault();
    }
    const element = document.getElementById(sectionId);
    if (element) {
      const offset = 100; // Offset for sticky header
      const elementPosition = element.getBoundingClientRect().top;
      const offsetPosition = elementPosition + window.pageYOffset - offset;

      window.scrollTo({
        top: offsetPosition,
        behavior: 'smooth'
      });
    }
  }

  loadPropertyDetail() {
    this.loading = true;
    this.errorMsg = '';
    
    console.log('🔵 [DETAIL-PROPERTY] Loading property detail, ID:', this.propertyId);
    
    this.propertyService.getPropertyDetail(this.propertyId).subscribe({
      next: (response) => {
        console.log('✅ [DETAIL-PROPERTY] Property loaded successfully:', response.data);
        console.log('🖼️ [DETAIL-PROPERTY] Images array:', response.data?.images);
        if (response.data?.images && response.data.images.length > 0) {
          console.log('📸 [DETAIL-PROPERTY] First image URL:', response.data.images[0].imageUrl);
        }
        this.property = response.data;
        this.loading = false;
        this.updateDisplayedReviews();
      },
      error: (error: any) => {
        console.error('❌ [DETAIL-PROPERTY] Error fetching property detail:', error);
        this.errorMsg = 'Không thể tải thông tin chi tiết property';
        this.loading = false;
      }
    });
  }

  // ================ IMAGE GALLERY METHODS ================
  showImage(index: number): void {
    if (this.property && this.property.images && this.property.images.length > 0) {
      if (index < 0) {
        index = 0;
      } else if (index >= this.property.images.length) {
        index = this.property.images.length - 1;
      }        
      this.currentImageIndex = index;
    }
  }

  thumbnailClick(index: number) {
    this.currentImageIndex = index;
  }  

  nextImage(): void {
    this.showImage(this.currentImageIndex + 1);
  }

  previousImage(): void {
    this.showImage(this.currentImageIndex - 1);
  }

  getPropertyImage(index: number = 0): string {
    if (this.property && this.property.images && this.property.images.length > index) {
      const imageUrl = this.property.images[index].imageUrl;
      console.log(`🖼️ [DETAIL-PROPERTY] Getting image [${index}]:`, imageUrl);
      
      if (imageUrl.startsWith('http')) {
        console.log('✅ [DETAIL-PROPERTY] Using absolute URL:', imageUrl);
        return imageUrl;
      }
      // Ensure we have a leading slash for relative paths
      const cleanPath = imageUrl.startsWith('/') ? imageUrl : `/${imageUrl}`;
      const fullUrl = `${environment.apiBaseUrl || 'http://localhost:8080'}${cleanPath}`;
      console.log('✅ [DETAIL-PROPERTY] Generated URL:', fullUrl);
      return fullUrl;
    }
    console.log('⚠️ [DETAIL-PROPERTY] No image found at index', index, '- using placeholder');
    return '/assets/img/placeholder.svg';
  }

  getCurrentImage(): string {
    console.log(`🔍 [DETAIL-PROPERTY] getCurrentImage() called, current index: ${this.currentImageIndex}`);
    return this.getPropertyImage(this.currentImageIndex);
  }

  // Get icon URL from API
  getIconUrl(iconPath: string): string {
    if (!iconPath) return '';
    if (iconPath.startsWith('http')) {
      return iconPath;
    }
    // Ensure we have a leading slash for relative paths
    const cleanPath = iconPath.startsWith('/') ? iconPath : `/${iconPath}`;
    return `${environment.apiBaseUrl || 'http://localhost:8080'}${cleanPath}`;
  }

  // ================ PROPERTY INFO METHODS ================
  getPropertyTypeLabel(): string {
    const types: { [key: number]: string } = {
      0: 'Apartment',
      1: 'House', 
      2: 'Hotel'
    };
    return types[this.property?.propertyType || 0] || 'Property';
  }

  getRatingLabel(rating: number): string {
    if (rating >= 4.5) return 'Superb';
    if (rating >= 4) return 'Very Good';
    if (rating >= 3.5) return 'Good';
    if (rating >= 3) return 'Pleasant';
    return 'Fair';
  }

  getRatingClass(rating: number): string {
    if (rating >= 4.5) return 'badge-superb';
    if (rating >= 4) return 'badge-very-good';
    if (rating >= 3.5) return 'badge-good';
    if (rating >= 3) return 'badge-pleasant';
    return 'badge-fair';
  }

  getAverageRating(): number {
    if (!this.property?.reviews || this.property.reviews.length === 0) {
      return 0;
    }
    const sum = this.property.reviews.reduce((acc, review) => acc + review.rating, 0);
    return sum / this.property.reviews.length;
  }

  // Rating already on 5-point scale, no need to divide
  getAverageRatingOutOf5(): number {
    return this.getAverageRating();
  }

  // ================ REVIEWS METHODS ================
  updateDisplayedReviews(): void {
    if (!this.property?.reviews) {
      this.displayedReviews = [];
      return;
    }
    
    if (this.showAllReviews) {
      this.displayedReviews = this.property.reviews;
    } else {
      this.displayedReviews = this.property.reviews.slice(0, this.reviewsToShow);
    }
  }

  toggleShowAllReviews(): void {
    this.showAllReviews = !this.showAllReviews;
    this.updateDisplayedReviews();
  }

  getRatingDistribution(stars: number): number {
    if (!this.property?.reviews) return 0;
    // Rating is already on 5-point scale
    const count = this.property.reviews.filter(r => Math.round(r.rating) === stars).length;
    return (count / this.property.reviews.length) * 100;
  }

  getStarArray(rating: number): number[] {
    // Custom rounding logic: 0-0.3 → 0, 0.4-0.7 → 0.5, 0.8+ → 1
    const roundedRating = this.roundRating(rating);
    const fullStars = Math.floor(roundedRating);
    const hasHalfStar = (roundedRating % 1) >= 0.5;
    
    return Array(5).fill(0).map((_, i) => {
      if (i < fullStars) return 1; // Full star
      if (i === fullStars && hasHalfStar) return 0.5; // Half star
      return 0; // Empty star
    });
  }

  // Custom rounding for each star
  roundRating(rating: number): number {
    const fullStars = Math.floor(rating);
    const decimal = rating - fullStars;
    
    let roundedDecimal = 0;
    if (decimal >= 0 && decimal <= 0.3) {
      roundedDecimal = 0;
    } else if (decimal > 0.3 && decimal <= 0.7) {
      roundedDecimal = 0.5;
    } else if (decimal > 0.7) {
      roundedDecimal = 1;
    }
    
    return fullStars + roundedDecimal;
  }

  setReviewRating(rating: number): void {
    // Keep rating on 1-5 scale
    this.reviewForm.rating = rating;
  }

  // Interactive star rating methods
  onStarHover(event: MouseEvent, starNumber: number): void {
    const target = event.currentTarget as HTMLElement;
    const rect = target.getBoundingClientRect();
    const x = event.clientX - rect.left;
    const width = rect.width;
    const percentage = (x / width) * 100;
    
    // Calculate rating based on mouse position within the star
    // starNumber is now 1, 2, 3, 4, 5 (left to right)
    // Left half (0-50%) = X-0.5, Right half (50-100%) = X
    
    if (percentage < 50) {
      // Mouse on left half = show half star (e.g., star 1 left half = 0.5)
      this.hoverRating = starNumber - 0.5;
    } else {
      // Mouse on right half = show full star (e.g., star 1 right half = 1.0)
      this.hoverRating = starNumber;
    }
  }

  onStarLeave(): void {
    this.hoverRating = 0;
  }

  onStarClick(event: MouseEvent, starNumber: number): void {
    const target = event.currentTarget as HTMLElement;
    const rect = target.getBoundingClientRect();
    const x = event.clientX - rect.left;
    const width = rect.width;
    const percentage = (x / width) * 100;
    
    // Set the permanent rating based on click position
    if (percentage < 50) {
      this.reviewForm.rating = starNumber - 0.5;
    } else {
      this.reviewForm.rating = starNumber;
    }
    this.tempRating = this.reviewForm.rating;
  }

  getStarFillPercentage(starNumber: number): number {
    const rating = this.hoverRating > 0 ? this.hoverRating : this.reviewForm.rating;
    
    // starNumber is 1, 2, 3, 4, 5
    if (rating >= starNumber) {
      return 100; // Full star
    } else if (rating === starNumber - 0.5) {
      return 50; // Half star (only this star shows half)
    } else {
      return 0; // Empty star
    }
  }

  submitReview(): void {
    // Validate form
    if (!this.reviewForm.name || !this.reviewForm.email || !this.reviewForm.rating || !this.reviewForm.comment) {
      this.modalService.showError('Vui lòng điền đầy đủ thông tin đánh giá');
      return;
    }

    // TODO: Implement API call to submit review
    console.log('Submitting review:', this.reviewForm);
    
    // Reset form
    this.reviewForm = {
      name: '',
      email: '',
      rating: 0,
      comment: ''
    };
  }

  // ================ BOOKING METHODS ================
  getTotalGuests(): number {
    return this.guestCounts.adults + 
           this.guestCounts.children + 
           this.guestCounts.infants;
  }

  incrementGuest(type: 'adults' | 'children' | 'infants' | 'pets'): void {
    const maxLimits = {
      adults: this.property?.maxAdults || 10,
      children: this.property?.maxChildren || 10,
      infants: this.property?.maxInfants || 10,
      pets: this.property?.maxPets || 0
    };

    if (this.guestCounts[type] < maxLimits[type]) {
      this.guestCounts[type]++;
    }
  }

  decrementGuest(type: 'adults' | 'children' | 'infants' | 'pets'): void {
    const minValue = type === 'adults' ? 1 : 0;
    if (this.guestCounts[type] > minValue) {
      this.guestCounts[type]--;
    }
  }

  canDecrement(type: 'adults' | 'children' | 'infants' | 'pets'): boolean {
    const minValue = type === 'adults' ? 1 : 0;
    return this.guestCounts[type] > minValue;
  }

  canIncrement(type: 'adults' | 'children' | 'infants' | 'pets'): boolean {
    const maxLimits = {
      adults: this.property?.maxAdults || 10,
      children: this.property?.maxChildren || 10,
      infants: this.property?.maxInfants || 10,
      pets: this.property?.maxPets || 0
    };
    return this.guestCounts[type] < maxLimits[type];
  }

  togglePetCheckbox(): void {
    this.isPetChecked = !this.isPetChecked;
    if (!this.isPetChecked) {
      this.guestCounts.pets = 0;
    } else {
      this.guestCounts.pets = 1;
    }
  }

  // ================ DATE RANGE PICKER METHODS ================
  loadBlockedDates(): void {
    // Optional: Load blocked dates from API
    // For now, using empty array - can be extended to call getPropertyBookings API
    // and map confirmed/booked dates to blockedDates array
    this.blockedDates = [];
  }

  onRangeSelected(range: DateRange): void {
    if (range && range.checkIn && range.checkOut) {
      this.selectedDateRange = range;
      // Update initial dates for picker
      this.initialCheckIn = range.checkIn;
      this.initialCheckOut = range.checkOut;
    }
  }

  // Helper to format Date to YYYY-MM-DD format
  private formatDateToYYYYMMDD(date: Date | null): string {
    if (!date) return '';
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    return `${year}-${month}-${day}`;
  }

  bookNow(): void {
    // Check if user is logged in
    if (!this.isLoggedIn) {
      this.modalService.showError('Vui lòng đăng nhập để đặt phòng!');
      this.router.navigate(['/login']);
      return;
    }

    // Validate dates
    if (!this.selectedDateRange || !this.selectedDateRange.checkIn || !this.selectedDateRange.checkOut) {
      this.modalService.showError('Vui lòng chọn ngày check-in và check-out');
      return;
    }

    // Convert dates to YYYY-MM-DD format
    const checkInDate = this.formatDateToYYYYMMDD(this.selectedDateRange.checkIn);
    const checkOutDate = this.formatDateToYYYYMMDD(this.selectedDateRange.checkOut);

    // Navigate to booking page with query params (all data auto-filled)
    this.router.navigate(['/booking'], { 
      queryParams: { 
        propertyId: this.propertyId,
        checkIn: checkInDate,
        checkOut: checkOutDate,
        adults: this.guestCounts.adults,
        children: this.guestCounts.children,
        infants: this.guestCounts.infants,
        pets: this.guestCounts.pets
      }
    });
  }

  toggleWishlist(): void {
    // Check if user is logged in
    if (!this.isLoggedIn) {
      this.modalService.showError('Vui lòng đăng nhập để sử dụng tính năng này!');
      this.router.navigate(['/login']);
      return;
    }

    // Prevent multiple clicks
    if (this.isLoadingWishlist) {
      return;
    }

    this.isLoadingWishlist = true;
    const token = this.tokenService.getToken();
    if (!token) {
      this.modalService.showError('Vui lòng đăng nhập lại!');
      this.isLoadingWishlist = false;
      return;
    }

    if (this.isFavorite) {
      // Remove from favorites (interceptor handles Authorization header)
      console.log('🔵 Removing property from wishlist:', this.propertyId);
      this.userService.removeFromFavorites(this.userId, this.propertyId).subscribe({
        next: (response) => {
          console.log('✅ Successfully removed from favorites:', response);
          this.isFavorite = false;
          this.isLoadingWishlist = false;
        },
        error: (error) => {
          console.error('❌ Error removing from favorites:', error);
          this.modalService.showError('Có lỗi xảy ra khi xóa khỏi danh sách yêu thích!');
          this.isLoadingWishlist = false;
        }
      });
    } else {
      // Add to favorites (interceptor handles Authorization header)
      console.log('🔵 Adding property to wishlist:', this.propertyId);
      this.userService.addToFavorites(this.userId, this.propertyId).subscribe({
        next: (response) => {
          console.log('✅ Successfully added to favorites:', response);
          this.isFavorite = true;
          this.isLoadingWishlist = false;
        },
        error: (error) => {
          console.error('❌ Error adding to favorites:', error);
          this.modalService.showError('Có lỗi xảy ra khi thêm vào danh sách yêu thích!');
          this.isLoadingWishlist = false;
        }
      });
    }
  }

  goBack(): void {
    this.router.navigate(['/properties/type', this.property?.propertyType || 0]);
  }

  // ================ UTILITY METHODS ================
  formatDate(dateString: string): string {
    const date = new Date(dateString);
    return date.toLocaleDateString('vi-VN', { 
      year: 'numeric', 
      month: 'long', 
      day: 'numeric' 
    });
  }

  formatPrice(price: number): string {
    return new Intl.NumberFormat('vi-VN').format(price);
  }

  // ================ IMAGE MODAL METHODS ================
  openImageModal(index: number): void {
    this.currentImageIndex = index;
    this.isImageModalOpen = true;
    // Prevent body scroll when modal is open
    document.body.style.overflow = 'hidden';
  }

  closeImageModal(): void {
    this.isImageModalOpen = false;
    // Restore body scroll
    document.body.style.overflow = 'auto';
  }

  selectImage(index: number): void {
    this.currentImageIndex = index;
  }
}
