import { Component, OnInit, OnDestroy } from '@angular/core';
import { Router } from '@angular/router';
import { BookingService } from '../../services/booking.service';
import { PromotionService } from '../../services/promotion.service';
import { TokenService } from '../../services/token.service';
import { SimpleModalService } from '../../services/simple-modal.service';
import { Booking, BookingStatus } from '../../models/booking';
import { UserPromotionDTO } from '../../models/user-promotion.dto';
import { PromotionPreviewDTO } from '../../models/promotion-preview.dto';
import { InitPaymentRequest } from '../../models/init-payment.request';

@Component({
  selector: 'app-my-bookings',
  templateUrl: './my-bookings.component.html',
  styleUrls: ['./my-bookings.component.scss']
})
export class MyBookingsComponent implements OnInit, OnDestroy {
  bookings: Booking[] = [];
  filteredBookings: Booking[] = [];
  loading: boolean = false;
  errorMessage: string = '';
  userId: number = 0;
  BookingStatus = BookingStatus; // Expose enum to template
  
  // Pagination
  currentPage: number = 0;
  totalPages: number = 0;
  totalElements: number = 0;
  pageSize: number = 5; // 5 bookings per page
  
  // Filter tabs
  selectedFilter: 'ALL' | 'PENDING' | 'CONFIRMED' | 'PAID' | 'COMPLETED' | 'CANCELLED' | 'REJECTED' = 'ALL';
  bookingCounts: { [key: string]: number } = {
    ALL: 0,
    PENDING: 0,
    CONFIRMED: 0,
    PAID: 0,
    COMPLETED: 0,
    CANCELLED: 0,
    REJECTED: 0
  };
  
  // Payment deadline countdown
  countdownIntervals: { [bookingId: number]: any } = {};
  countdownTexts: { [bookingId: number]: string } = {};

  // ⭐ REMOVED: Promotion logic moved to BookingComponent
  // Promotions are now applied during booking creation, not during payment

  constructor(
    private bookingService: BookingService,
    private promotionService: PromotionService,
    private tokenService: TokenService,
    private router: Router,
    private modalService: SimpleModalService
  ) {}

  ngOnInit(): void {
    // Get userId from token
    this.userId = this.tokenService.getUserId();
    if (this.userId > 0) {
      this.loadBookings();
      // ⭐ REMOVED: loadMyActivePromotions() - no longer needed
    } else {
      this.errorMessage = 'Please login to view bookings';
      this.router.navigate(['/login']);
    }
  }

  ngOnDestroy(): void {
    // Clear all countdown intervals
    Object.keys(this.countdownIntervals).forEach(key => {
      clearInterval(this.countdownIntervals[+key]);
    });
  }

  /**
   * Load bookings with server-side filtering and pagination
   * @param status - Optional status filter (PENDING, CONFIRMED, COMPLETED, CANCELLED, REJECTED, ALL)
   * @param page - Page number (0-indexed)
   */
  loadBookings(status?: string, page: number = 0): void {
    this.loading = true;
    this.errorMessage = '';

    // Map CONFIRMED filter to both CONFIRMED and PAID statuses
    const apiStatus = status === 'CONFIRMED' ? undefined : status;

    console.log('=======================================');
    console.log('📋 LOADING USER BOOKINGS');
    console.log('   User ID:', this.userId);
    console.log('   Filter Status:', status);
    console.log('   API Status:', apiStatus);
    console.log('   Page:', page);
    console.log('   Page Size:', this.pageSize);
    console.log('   Timestamp:', new Date().toISOString());
    console.log('=======================================');

    // Call API with server-side filtering: GET /bookings/user/{userId}?status=xxx&page=0&size=5&sortBy=createdAt&sortDirection=DESC
    this.bookingService.getUserBookings(
      this.userId,
      page,              // page number
      this.pageSize,     // size = 5
      'createdAt',       // sortBy - newest first
      'DESC',            // sortDirection
      apiStatus          // status filter
    ).subscribe({
      next: (response) => {
        console.log('=======================================');
        console.log('📥 BOOKINGS API RESPONSE:');
        console.log('   Status Filter:', apiStatus);
        console.log('   Current Page:', response.data.currentPage);
        console.log('   Total Pages:', response.data.totalPages);
        console.log('   Total Elements:', response.data.totalElements);
        console.log('   Page Size:', response.data.pageSize);
        console.log('=======================================');
        
        this.loading = false;
        if (response.code === 200 && response.data && response.data.content) {
          const allBookings = response.data.content;
          
          // Update pagination info
          this.currentPage = response.data.currentPage;
          this.totalPages = response.data.totalPages;
          this.totalElements = response.data.totalElements;
          
          // If filter is CONFIRMED, manually filter to include both CONFIRMED and PAID
          if (status === 'CONFIRMED') {
            this.filteredBookings = allBookings.filter(b => 
              b.status === 'CONFIRMED' || b.status === 'PAID'
            );
          } else {
            this.filteredBookings = allBookings;
          }

          console.log('=======================================');
          console.log('✅ BOOKINGS LOADED:');
          console.log('   Filtered Count:', this.filteredBookings.length);
          console.log('   Page:', this.currentPage + 1, '/', this.totalPages);
          console.log('   Bookings:', this.filteredBookings);
          console.log('=======================================');
          
          // Load all bookings to get counts (separate API call without pagination)
          this.loadBookingCounts();
          
          // Start countdown timers for CONFIRMED bookings
          this.filteredBookings.forEach(booking => {
            if (booking.status === 'CONFIRMED' && 
                booking.paymentDeadline && 
                !booking.isPaymentExpired) {
              this.startCountdown(booking);
            }
          });
        } else {
          this.errorMessage = response.message || 'Failed to load bookings';
          console.error('❌ Invalid response format:', response);
        }
      },
      error: (error) => {
        this.loading = false;
        console.error('=======================================');
        console.error('❌ ERROR LOADING BOOKINGS:');
        console.error('   Status:', error.status);
        console.error('   Error:', error);
        console.error('=======================================');
        this.errorMessage = 'Failed to load bookings. Please try again.';
      }
    });
  }

  /**
   * Load all bookings to calculate counts for filter tabs
   */
  loadBookingCounts(): void {
    // Call API without filter to get all bookings for counts
    this.bookingService.getUserBookings(this.userId, 0, 1000, 'createdAt', 'DESC', undefined).subscribe({
      next: (response) => {
        if (response.code === 200 && response.data && response.data.content) {
          const allBookings = response.data.content;
          this.bookingCounts['ALL'] = allBookings.length;
          
          // Backend returns status in UPPERCASE (PENDING, CONFIRMED, PAID, COMPLETED, CANCELLED, REJECTED)
          this.bookingCounts['PENDING'] = allBookings.filter(b => b.status === 'PENDING').length;
          this.bookingCounts['CONFIRMED'] = allBookings.filter(b => b.status === 'CONFIRMED').length;
          this.bookingCounts['PAID'] = allBookings.filter(b => b.status === 'PAID').length;
          this.bookingCounts['COMPLETED'] = allBookings.filter(b => b.status === 'COMPLETED').length;
          this.bookingCounts['CANCELLED'] = allBookings.filter(b => b.status === 'CANCELLED').length;
          this.bookingCounts['REJECTED'] = allBookings.filter(b => b.status === 'REJECTED').length;

          console.log('📊 Booking counts updated:', this.bookingCounts);
          console.log('📊 Sample statuses from API:', allBookings.slice(0, 3).map(b => ({ 
            id: b.id, 
            status: b.status
          })));
        }
      },
      error: (error) => {
        console.error('❌ Error loading booking counts:', error);
      }
    });
  }

  /**
   * Filter bookings based on selected tab (calls API with filter)
   */
  filterBookings(filter: 'ALL' | 'PENDING' | 'CONFIRMED' | 'PAID' | 'COMPLETED' | 'CANCELLED' | 'REJECTED'): void {
    this.selectedFilter = filter;
    this.currentPage = 0; // Reset to first page when filter changes
    const statusFilter = filter === 'ALL' ? undefined : filter;
    this.loadBookings(statusFilter, 0);
  }

  /**
   * Handle page change
   */
  onPageChange(page: number): void {
    const statusFilter = this.selectedFilter === 'ALL' ? undefined : this.selectedFilter;
    this.loadBookings(statusFilter, page);
    
    // Scroll to top of bookings list
    window.scrollTo({ top: 0, behavior: 'smooth' });
  }

  /**
   * Calculate number of nights
   */
  calculateNights(checkIn: string, checkOut: string): number {
    const start = new Date(checkIn);
    const end = new Date(checkOut);
    const diffTime = Math.abs(end.getTime() - start.getTime());
    const diffDays = Math.ceil(diffTime / (1000 * 60 * 60 * 24));
    return diffDays;
  }

  // ⭐ REMOVED: Promotion methods - no longer needed
  // Promotions are now applied during booking creation in BookingComponent
  // Methods removed: loadMyActivePromotions(), togglePromotionSelector(), 
  // selectPromotion(), validatePromotion(), clearPromotion()

  // ========== Navigate to My Promotions Page (KEPT) ==========
  goToMyPromotions(): void {
    this.router.navigate(['/my-promotions']);
  }

  // ⭐ SIMPLIFIED: Handle Payment (No Promotion Logic)
  handlePayment(bookingId: number): void {
    console.log('=======================================');
    console.log('🔵 handlePayment called with bookingId:', bookingId);
    console.log('=======================================');
    
    if (!bookingId) {
      console.error('❌ No bookingId provided');
      return;
    }

    // ⚠️ FIX: Tìm trong filteredBookings thay vì bookings
    const booking = this.filteredBookings.find(b => b.id === bookingId);
    if (!booking) {
      console.error('❌ Booking not found in filteredBookings:', bookingId);
      console.log('Available bookings:', this.filteredBookings.map(b => ({ id: b.id, status: b.status })));
      this.modalService.showError('Booking not found');
      return;
    }

    console.log('✅ Booking found:', {
      id: booking.id,
      status: booking.status,
      totalPrice: booking.totalPrice,
      isPaymentExpired: booking.isPaymentExpired,
      paymentDeadline: booking.paymentDeadline,
      promotionCode: booking.promotionCode
    });

    // Check if expired
    if (booking.isPaymentExpired) {
      console.warn('⚠️ Payment deadline expired');
      this.modalService.showError('Payment deadline has expired. This booking will be cancelled.');
      return;
    }

    // Show loading
    this.loading = true;

    // ⭐ NEW FLOW: NO promotionCode - discount already applied during booking creation
    const request: InitPaymentRequest = {
      bookingId: bookingId,
      paymentMethod: 'PAYOS'
    };

    console.log('=======================================');
    console.log('📤 SENDING PAYMENT INIT REQUEST:');
    console.log('   API: POST /transactions/init');
    console.log('   Request Body:', JSON.stringify(request, null, 2));
    console.log('   Booking total:', booking.totalPrice, 'VND (already discounted if promotion was used)');
    console.log('=======================================');

    this.bookingService.initPayment(request).subscribe({
      next: (response) => {
        this.loading = false;
        console.log('=======================================');
        console.log('📥 PAYMENT INIT RESPONSE RECEIVED:');
        console.log('   Full Response:', JSON.stringify(response, null, 2));
        console.log('   Response Code:', response.code);
        console.log('   Response Message:', response.message);
        console.log('   Response Data:', response.data);
        console.log('=======================================');
        
        // ⭐ FIXED: Backend returns "payUrl" not "paymentUrl"
        const paymentUrl = response.data?.payUrl || response.data?.paymentUrl;
        
        if (response.code === 200 && paymentUrl) {
          console.log('✅ Payment URL received:', paymentUrl);
          console.log('🔗 Redirecting to PayOS in 500ms...');
          
          // Redirect to PayOS
          setTimeout(() => {
            console.log('🌐 Redirecting NOW to:', paymentUrl);
            window.location.href = paymentUrl;
          }, 500);
        } else {
          console.error('=======================================');
          console.error('❌ Invalid response or missing payUrl');
          console.error('   Response code:', response.code);
          console.error('   Has data?:', !!response.data);
          console.error('   Has payUrl?:', !!response.data?.payUrl);
          console.error('   Has paymentUrl?:', !!response.data?.paymentUrl);
          console.error('   payUrl value:', response.data?.payUrl);
          console.error('=======================================');
          this.modalService.showError(response.message || 'Failed to initialize payment');
        }
      },
      error: (error) => {
        this.loading = false;
        console.error('=======================================');
        console.error('❌ PAYMENT INIT ERROR:');
        console.error('   Full Error:', JSON.stringify(error, null, 2));
        console.error('   Status:', error.status);
        console.error('   Status Text:', error.statusText);
        console.error('   Error Message:', error.error?.message || error.message);
        console.error('   Error Body:', error.error);
        console.error('   URL:', error.url);
        console.error('=======================================');
        this.modalService.showError(
          error.error?.message || 'Failed to initialize payment. Please try again.'
        );
      }
    });
  }

  /**
   * View booking details page
   */
  viewBookingDetails(bookingId: number): void {
    this.router.navigate(['/my-bookings', bookingId]);
  }

  async cancelBooking(bookingId: number): Promise<void> {
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

    // If user cancelled the modal (clicked "Không, giữ lại" or closed modal)
    if (reason === null) {
      console.log('❌ User cancelled booking cancellation');
      return;
    }

    console.log('🔄 Cancelling booking with reason:', reason || '(no reason provided)');

    // Pass reason to service (empty string if no reason provided)
    this.bookingService.cancelBooking(bookingId, reason || undefined).subscribe({
      next: (response) => {
        if (response.code === 200) {
          console.log('✅ Booking cancelled');
          this.modalService.showSuccess('Đã hủy booking thành công');
          this.loadBookings(this.selectedFilter === 'ALL' ? undefined : this.selectedFilter, this.currentPage); // Reload current page with filter
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

  async deleteBooking(bookingId: number): Promise<void> {
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
          this.loadBookings(this.selectedFilter === 'ALL' ? undefined : this.selectedFilter, this.currentPage); // Reload current page with filter
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

  getStatusClass(status: string | BookingStatus | undefined): string {
    if (!status) return '';
    
    const statusStr = status.toString();
    
    switch (statusStr) {
      case 'CONFIRMED':
        return 'status-confirmed';
      case 'PENDING':
        return 'status-pending';
      case 'PAID':
        return 'status-paid';
      case 'REJECTED':
        return 'status-rejected';
      case 'CANCELLED':
        return 'status-cancelled';
      case 'COMPLETED':
        return 'status-completed';
      default:
        return '';
    }
  }

  /**
   * Check if booking status matches (handles format inconsistency)
   */
  isStatus(booking: Booking, status: BookingStatus): boolean {
    if (!booking.status) return false;
    const normalized = this.getStatusClass(booking.status);
    return normalized.includes(status.toLowerCase());
  }

  createNewBooking(): void {
    this.router.navigate(['/booking']);
  }

  viewProperty(propertyId: number): void {
    this.router.navigate(['/properties', propertyId]);
  }

  formatDate(dateString: string): string {
    const date = new Date(dateString);
    return date.toLocaleDateString('vi-VN', {
      year: 'numeric',
      month: 'long',
      day: 'numeric'
    });
  }

  formatCurrency(amount: number | undefined): string {
    if (amount === undefined || amount === null) return '0 VND';
    return amount.toLocaleString('vi-VN') + ' VND';
  }

  /**
   * Start countdown timer for payment deadline
   */
  startCountdown(booking: Booking): void {
    if (!booking.id || !booking.paymentDeadline) return;

    // Clear existing interval if any
    if (this.countdownIntervals[booking.id]) {
      clearInterval(this.countdownIntervals[booking.id]);
    }

    // Update immediately
    this.updateCountdown(booking);

    // Update every second
    this.countdownIntervals[booking.id] = setInterval(() => {
      this.updateCountdown(booking);
    }, 1000);
  }

  /**
   * Update countdown text for a booking
   */
  updateCountdown(booking: Booking): void {
    if (!booking.id || !booking.paymentDeadline) return;

    const now = new Date();
    const deadline = new Date(booking.paymentDeadline);
    const diff = deadline.getTime() - now.getTime();

    if (diff <= 0) {
      this.countdownTexts[booking.id] = 'Expired';
      if (this.countdownIntervals[booking.id]) {
        clearInterval(this.countdownIntervals[booking.id]);
      }
    } else {
      const hours = Math.floor(diff / (1000 * 60 * 60));
      const minutes = Math.floor((diff % (1000 * 60 * 60)) / (1000 * 60));
      const seconds = Math.floor((diff % (1000 * 60)) / 1000);
      this.countdownTexts[booking.id] = `${hours}h ${minutes}m ${seconds}s`;
    }
  }

  /**
   * Get countdown text for display
   */
  getCountdown(bookingId: number | undefined): string {
    if (!bookingId) return '';
    return this.countdownTexts[bookingId] || '';
  }

  /**
   * Get status display info (icon + text)
   */
  getStatusInfo(status: BookingStatus | string | undefined): { icon: string; text: string; color: string } {
    const statusMap: { [key: string]: { icon: string; text: string; color: string } } = {
      'PENDING': { icon: '⏳', text: 'Pending Approval', color: 'pending' },
      'CONFIRMED': { icon: '✅', text: 'Approved - Awaiting Payment', color: 'confirmed' },
      'PAID': { icon: '💳', text: 'Paid', color: 'paid' },
      'COMPLETED': { icon: '✔️', text: 'Completed', color: 'completed' },
      'CANCELLED': { icon: '❌', text: 'Cancelled', color: 'cancelled' },
      'REJECTED': { icon: '❌', text: 'Rejected by Host', color: 'rejected' }
    };

    const statusStr = status?.toString() || '';
    return statusMap[statusStr] || { icon: '❓', text: statusStr, color: '' };
  }

  /**
   * Check if booking can be cancelled
   * All bookings EXCEPT COMPLETED, CANCELLED, and REJECTED can be cancelled
   */
  canCancel(booking: Booking): boolean {
    return booking.status !== 'COMPLETED' && 
           booking.status !== 'CANCELLED' && 
           booking.status !== 'REJECTED';
  }

  /**
   * Check if booking can be deleted
   * Only CANCELLED and REJECTED bookings can be deleted
   */
  canDelete(booking: Booking): boolean {
    return booking.status === 'CANCELLED' || booking.status === 'REJECTED';
  }

  /**
   * Check if booking can be paid
   * Only CONFIRMED bookings (not yet paid) can proceed to payment
   */
  canPay(booking: Booking): boolean {
    return booking.status === 'CONFIRMED' && !booking.isPaymentExpired;
  }
}
