import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { UserPromotionDTO, getPromotionStatus } from '../../models/user-promotion.dto';
import { PromotionService } from '../../services/promotion.service';
import { BookingService } from '../../services/booking.service';
import { TokenService } from '../../services/token.service';

@Component({
  selector: 'app-promotion-selection',
  templateUrl: './promotion-selection.component.html',
  styleUrls: ['./promotion-selection.component.scss']
})
export class PromotionSelectionComponent implements OnInit {
  // Expose Math to template
  Math = Math;
  
  // Claim section
  promotionCode: string = '';
  isClaimingPromotion: boolean = false;
  claimErrorMessage: string = '';

  // List section
  promotions: UserPromotionDTO[] = [];
  activePromotions: UserPromotionDTO[] = [];
  isLoadingPromotions: boolean = false;

  // Pagination
  currentPage: number = 0;
  pageSize: number = 5;
  totalPages: number = 0;
  paginatedPromotions: UserPromotionDTO[] = [];

  // Selected promotion
  selectedPromotion: UserPromotionDTO | null = null;

  // Preview
  previewData: any = null;
  isValidatingPromotion: boolean = false;

  // Success modal
  showSuccessModal: boolean = false;

  // Booking data (from previous step)
  bookingData: any = null;

  constructor(
    private promotionService: PromotionService,
    private bookingService: BookingService,
    private tokenService: TokenService,
    private router: Router
  ) {}

  ngOnInit(): void {
    // Get booking data from service
    this.bookingData = this.bookingService.getPendingBookingData();
    
    if (!this.bookingData) {
      alert('No booking data found. Please start from booking form.');
      this.router.navigate(['/']);
      return;
    }

    this.loadMyPromotions();
  }

  /**
   * Load user's promotions and filter ACTIVE ones
   */
  loadMyPromotions(): void {
    this.isLoadingPromotions = true;
    const userId = this.tokenService.getUserId();

    if (!userId) {
      alert('User not found. Please login again.');
      this.router.navigate(['/login']);
      return;
    }

    this.promotionService.getMyPromotions(0, 100).subscribe({
      next: (response: any) => {
        if (response.data && response.data.content) {
          this.promotions = response.data.content || [];
        } else {
          this.promotions = response.data || [];
        }
        
        // Filter only ACTIVE promotions
        this.activePromotions = this.promotions.filter(
          promo => getPromotionStatus(promo) === 'ACTIVE'
        );

        // Calculate pagination
        this.totalPages = Math.ceil(this.activePromotions.length / this.pageSize);
        this.currentPage = 0;
        this.updatePaginatedPromotions();

        this.isLoadingPromotions = false;
      },
      error: (error: any) => {
        console.error('Error loading promotions:', error);
        alert('Failed to load promotions. Please try again.');
        this.isLoadingPromotions = false;
      }
    });
  }

  /**
   * Update paginated promotions based on current page
   */
  updatePaginatedPromotions(): void {
    const startIndex = this.currentPage * this.pageSize;
    const endIndex = startIndex + this.pageSize;
    this.paginatedPromotions = this.activePromotions.slice(startIndex, endIndex);
  }

  /**
   * Go to previous page
   */
  previousPage(): void {
    if (this.currentPage > 0) {
      this.currentPage--;
      this.updatePaginatedPromotions();
    }
  }

  /**
   * Go to next page
   */
  nextPage(): void {
    if (this.currentPage < this.totalPages - 1) {
      this.currentPage++;
      this.updatePaginatedPromotions();
    }
  }

  /**
   * Claim new promotion by code
   */
  claimPromotion(): void {
    if (!this.promotionCode.trim()) {
      this.claimErrorMessage = 'Please enter promotion code';
      return;
    }

    this.isClaimingPromotion = true;
    this.claimErrorMessage = '';

    this.promotionService.claimPromotion(this.promotionCode.trim()).subscribe({
      next: (response: any) => {
        // Success - promotion claimed
        alert(`Promotion "${this.promotionCode}" claimed successfully!`);

        // Clear input
        this.promotionCode = '';
        
        // Reload promotions to show newly claimed one
        this.loadMyPromotions();
        
        this.isClaimingPromotion = false;
      },
      error: (error: any) => {
        console.error('Error claiming promotion:', error);
        
        // Parse error message from backend
        let errorMsg = 'Failed to claim promotion';
        if (error.error?.message) {
          errorMsg = error.error.message;
        } else if (error.status === 400) {
          errorMsg = 'Invalid promotion code or already claimed';
        } else if (error.status === 404) {
          errorMsg = 'Promotion not found or inactive';
        } else if (error.status === 401) {
          errorMsg = 'Please login to claim promotion';
        }

        this.claimErrorMessage = errorMsg;
        this.isClaimingPromotion = false;
      }
    });
  }

  /**
   * Select promotion from list
   */
  selectPromotion(promotion: UserPromotionDTO): void {
    this.selectedPromotion = promotion;
    this.previewData = null; // Reset preview
    
    // Validate promotion
    this.validatePromotion(promotion);
  }

  /**
   * Validate selected promotion for current booking
   */
  validatePromotion(promotion: UserPromotionDTO): void {
    if (!this.bookingData) {
      alert('Booking data not found');
      return;
    }

    // 🔍 DEBUG: Check authentication before validation
    const token = this.tokenService.getToken();
    if (!token) {
      alert('Authentication required. Please login again.');
      this.router.navigate(['/login']);
      return;
    }

    this.isValidatingPromotion = true;

    // ✅ Extract promotion code from UserPromotionDTO
    // UserPromotionDTO has: { promotionCode: string, ... }
    const promotionCode = promotion.promotionCode;
    
    if (!promotionCode) {
      alert('Promotion code not found');
      this.isValidatingPromotion = false;
      return;
    }

    // ✅ Backend needs: code, propertyId, checkIn, checkOut (ISO-8601)
    console.log('🔍 Starting promotion validation...');
    console.log('   Token exists:', !!token);
    console.log('   Promotion Code:', promotionCode);
    console.log('   Property ID:', this.bookingData.propertyId);
    console.log('   Check-in:', this.bookingData.checkInDate);
    console.log('   Check-out:', this.bookingData.checkOutDate);

    this.promotionService.validatePromotionForBooking(
      promotionCode,
      this.bookingData.propertyId,
      this.bookingData.checkInDate,
      this.bookingData.checkOutDate
    ).subscribe({
      next: (response: any) => {
        this.previewData = response.data;
        this.isValidatingPromotion = false;
        
        if (this.previewData.valid === true) {  // ✅ Changed from isValid to valid
          console.log('✅ Validation successful:', this.previewData);
        } else {
          console.log('⚠️ Validation failed:', this.previewData.errorMessage);
          alert(`Promotion validation failed: ${this.previewData.errorMessage}`);
          this.selectedPromotion = null;
          this.previewData = null;
        }
      },
      error: (error: any) => {
        console.error('❌ Validation error:', error);
        
        let errorMsg = 'Promotion validation failed';
        
        if (error.status === 401) {
          errorMsg = 'Authentication failed. Please login again.';
          // Optionally redirect to login
          setTimeout(() => {
            this.router.navigate(['/login']);
          }, 2000);
        } else if (error.error?.message) {
          errorMsg = error.error.message;
        }

        alert(errorMsg);

        this.selectedPromotion = null;
        this.previewData = null;
        this.isValidatingPromotion = false;
      }
    });
  }

  /**
   * Skip promotion and create booking without discount
   */
  skipAndCreateBooking(): void {
    if (confirm('Create booking without promotion?')) {
      this.createBooking(null);
    }
  }

  /**
   * Apply selected promotion and create booking
   */
  applyAndCreateBooking(): void {
    if (!this.selectedPromotion) {
      alert('Please select a promotion first');
      return;
    }

    if (!this.previewData) {
      alert('Promotion validation pending. Please wait.');
      return;
    }

    // Create booking with promotion
    this.createBooking(this.selectedPromotion.id!);
  }

  /**
   * Create booking with or without promotion
   */
  private createBooking(promotionId: number | null): void {
    if (!this.bookingData) {
      alert('Booking data not found');
      return;
    }

    // Match backend BookingRequest structure exactly
    const bookingRequest = {
      userId: this.tokenService.getUserId(),
      propertyId: this.bookingData.propertyId,
      checkIn: this.bookingData.checkIn, // ISO DateTime
      checkOut: this.bookingData.checkOut, // ISO DateTime
      numAdults: this.bookingData.numAdults,
      numChildren: this.bookingData.numChildren || 0,
      num_infant: this.bookingData.num_infant || 0,
      num_pet: this.bookingData.num_pet || 0,
      notes: this.bookingData.notes || '',
      promotionCode: promotionId ? this.selectedPromotion?.promotionCode : undefined,
      originalAmount: promotionId ? this.bookingData.originalAmount : undefined
    };

    console.log('📤 Creating booking:', bookingRequest);

    this.bookingService.createBooking(bookingRequest).subscribe({
      next: (response: any) => {
        // Clear pending booking data
        this.bookingService.clearPendingBookingData();

        // Show success message with custom styling
        this.showSuccessDialog();
      },
      error: (error: any) => {
        console.error('Error creating booking:', error);
        
        let errorMsg = 'Failed to create booking';
        if (error.error?.message) {
          errorMsg = error.error.message;
        }

        alert(errorMsg);
      }
    });
  }

  /**
   * Show success dialog and navigate to My Bookings
   */
  private showSuccessDialog(): void {
    this.showSuccessModal = true;
  }

  /**
   * Close success modal and navigate to My Bookings
   */
  closeSuccessModal(): void {
    this.showSuccessModal = false;
    this.router.navigate(['/my-bookings']);
  }

  /**
   * Go back to booking form
   */
  goBack(): void {
    this.router.navigate(['/booking'], {
      queryParams: this.bookingData.queryParams || {}
    });
  }

  /**
   * Format currency (VND)
   */
  formatCurrency(amount: number): string {
    return new Intl.NumberFormat('vi-VN', {
      style: 'currency',
      currency: 'VND'
    }).format(amount);
  }

  /**
   * Format date
   */
  formatDate(date: string): string {
    return new Date(date).toLocaleDateString('vi-VN', {
      year: 'numeric',
      month: 'long',
      day: 'numeric'
    });
  }

  /**
   * Get status badge class
   */
  getStatusBadgeClass(promotion: UserPromotionDTO): string {
    const status = getPromotionStatus(promotion);
    switch (status) {
      case 'ACTIVE':
        return 'badge-success';
      case 'USED':
        return 'badge-secondary';
      case 'EXPIRED':
        return 'badge-danger';
      default:
        return 'badge-secondary';
    }
  }

  /**
   * Check if promotion is selected
   */
  isPromotionSelected(promotion: UserPromotionDTO): boolean {
    return this.selectedPromotion?.id === promotion.id;
  }
}
