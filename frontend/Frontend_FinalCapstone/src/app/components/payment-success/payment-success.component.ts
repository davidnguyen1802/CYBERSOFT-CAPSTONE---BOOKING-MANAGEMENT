import { Component, OnInit, OnDestroy } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { PaymentService } from '../../services/payment.service';

@Component({
  selector: 'app-payment-success',
  templateUrl: './payment-success.component.html',
  styleUrls: ['./payment-success.component.scss']
})
export class PaymentSuccessComponent implements OnInit, OnDestroy {
  bookingId: number = 0;
  orderCode: string = '';
  status: string = 'PENDING';
  paymentDate: string = '';
  
  // Polling state
  isPolling: boolean = true;
  pollingAttempts: number = 0;
  maxPollingAttempts: number = 10;
  pollingInterval: any;
  
  // Display state
  showSuccess: boolean = false;
  showError: boolean = false;
  errorMessage: string = '';
  
  // Payment details (from backend response)
  amount: number = 0;
  originalAmount: number = 0;
  discountAmount: number = 0;
  transactionId: string = '';
  promotionApplied: boolean = false;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private paymentService: PaymentService
  ) {}

  ngOnInit(): void {
    // Get payment info from URL parameters
    this.route.queryParams.subscribe(params => {
      console.log('=======================================');
      console.log('🔙 PAYMENT RETURN - User returned from PayOS');
      console.log('   All URL Params:', params);
      console.log('=======================================');
      
      // ⭐ PayOS returns: ?code=00&id=...&cancel=false&status=PAID&orderCode=29
      // orderCode = bookingId
      const orderCodeParam = params['orderCode'];
      const statusParam = params['status'];
      const cancelParam = params['cancel'];
      const codeParam = params['code'];
      
      // Get bookingId from orderCode (PayOS uses orderCode for bookingId)
      this.bookingId = orderCodeParam ? +orderCodeParam : 0;
      this.orderCode = orderCodeParam || 'N/A';
      
      console.log('=======================================');
      console.log('� PARSED PARAMS:');
      console.log('   orderCode (bookingId):', orderCodeParam);
      console.log('   Booking ID:', this.bookingId);
      console.log('   Status:', statusParam);
      console.log('   Cancel:', cancelParam);
      console.log('   Code:', codeParam);
      console.log('=======================================');
      
      // Check if cancelled
      if (cancelParam === 'true' || statusParam === 'CANCELLED') {
        // User cancelled payment
        this.isPolling = false;
        this.showError = true;
        this.errorMessage = 'Payment was cancelled. Please try again.';
        console.log('❌ Payment cancelled by user');
        
        // Redirect back to bookings after 3 seconds
        setTimeout(() => {
          this.router.navigate(['/my-bookings']);
        }, 3000);
        return;
      }
      
      if (this.bookingId > 0) {
        // Start polling payment status
        this.startPolling();
      } else {
        this.isPolling = false;
        this.showError = true;
        this.errorMessage = 'Invalid booking ID. Please check your bookings.';
        console.error('❌ Invalid bookingId from URL params');
      }
    });
  }

  ngOnDestroy(): void {
    // Clear polling interval
    if (this.pollingInterval) {
      clearInterval(this.pollingInterval);
    }
  }

  /**
   * Start polling payment status from backend
   */
  startPolling(): void {
    console.log('🔄 Starting payment status polling...');
    
    // Check immediately
    this.checkPaymentStatus();
    
    // Then check every 2 seconds
    this.pollingInterval = setInterval(() => {
      this.checkPaymentStatus();
    }, 2000);
  }

  /**
   * Check payment status from backend
   */
  checkPaymentStatus(): void {
    this.pollingAttempts++;
    
    console.log(`🔍 Polling attempt ${this.pollingAttempts}/${this.maxPollingAttempts}`);
    
    this.paymentService.checkPaymentStatus(this.bookingId).subscribe({
      next: (response) => {
        console.log('✅ Payment status response:', response);
        
        if (response.code === 200 && response.data) {
          const data = response.data;
          
          if (data.bookingStatus === 'PAID' || data.bookingStatus === 'Paid') {
            // Payment successful!
            this.isPolling = false;
            this.showSuccess = true;
            this.status = 'PAID';
            this.orderCode = data.bookingId.toString();
            
            // Set payment details
            this.amount = data.amount || 0;
            this.originalAmount = data.originalAmount || data.amount || 0;
            this.discountAmount = this.originalAmount - this.amount;
            this.promotionApplied = this.discountAmount > 0;
            this.transactionId = data.transactionId?.toString() || data.orderCode || '';
            
            console.log('💰 Payment Details:');
            console.log('   Amount:', this.amount);
            console.log('   Original Amount:', this.originalAmount);
            console.log('   Discount:', this.discountAmount);
            console.log('   Promotion Applied:', this.promotionApplied);
            console.log('   Transaction ID:', this.transactionId);
            
            // Set payment date
            if (data.paidAt) {
              const paidDate = new Date(data.paidAt);
              this.paymentDate = paidDate.toLocaleString('vi-VN', {
                year: 'numeric',
                month: 'long',
                day: 'numeric',
                hour: '2-digit',
                minute: '2-digit'
              });
            } else {
              const now = new Date();
              this.paymentDate = now.toLocaleString('vi-VN', {
                year: 'numeric',
                month: 'long',
                day: 'numeric',
                hour: '2-digit',
                minute: '2-digit'
              });
            }
            
            // Stop polling
            clearInterval(this.pollingInterval);
            
            console.log('🎉 Payment confirmed! Booking status: PAID');
          } else if (this.pollingAttempts >= this.maxPollingAttempts) {
            // Timeout - stop polling
            this.isPolling = false;
            this.showError = true;
            this.errorMessage = 'Payment is being processed. Please check your bookings in a few minutes.';
            clearInterval(this.pollingInterval);
            
            console.log('⏱️ Polling timeout - payment still processing');
            
            // Redirect to bookings after 5 seconds
            setTimeout(() => {
              this.router.navigate(['/my-bookings']);
            }, 5000);
          }
        }
      },
      error: (error) => {
        console.error('❌ Error checking payment status:', error);
        
        if (this.pollingAttempts >= this.maxPollingAttempts) {
          this.isPolling = false;
          this.showError = true;
          this.errorMessage = 'Unable to verify payment status. Please check your bookings.';
          clearInterval(this.pollingInterval);
          
          // Redirect to bookings after 5 seconds
          setTimeout(() => {
            this.router.navigate(['/my-bookings']);
          }, 5000);
        }
      }
    });
  }

  goToBookings(): void {
    this.router.navigate(['/my-bookings']);
  }

  createNewBooking(): void {
    this.router.navigate(['/booking']);
  }
}
