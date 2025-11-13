import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';

@Component({
  selector: 'app-payment-cancel',
  templateUrl: './payment-cancel.component.html',
  styleUrls: ['./payment-cancel.component.scss']
})
export class PaymentCancelComponent implements OnInit {
  bookingId: number = 0;
  status: string = '';
  redirectCountdown: number = 3;
  private countdownInterval: any;

  constructor(
    private route: ActivatedRoute,
    private router: Router
  ) {}

  ngOnInit(): void {
    // Get payment info from URL parameters
    this.route.queryParams.subscribe(params => {
      console.log('=======================================');
      console.log('🔙 PAYMENT CANCEL - User returned from PayOS');
      console.log('   All URL Params:', params);
      console.log('=======================================');
      
      // ⭐ PayOS returns: ?orderCode=29&cancel=true&status=CANCELLED
      const orderCodeParam = params['orderCode'];
      this.bookingId = orderCodeParam ? +orderCodeParam : 0;
      this.status = params['status'] || 'cancelled';

      console.log('=======================================');
      console.log('� PARSED PARAMS:');
      console.log('   orderCode (bookingId):', orderCodeParam);
      console.log('   Booking ID:', this.bookingId);
      console.log('   Status:', this.status);
      console.log('   ⚠️ Payment was cancelled by user');
      console.log('   Timestamp:', new Date().toISOString());
      console.log('=======================================');

      // Start countdown redirect
      this.startCountdown();
    });
  }

  ngOnDestroy(): void {
    if (this.countdownInterval) {
      clearInterval(this.countdownInterval);
    }
  }

  startCountdown(): void {
    this.countdownInterval = setInterval(() => {
      this.redirectCountdown--;
      
      if (this.redirectCountdown <= 0) {
        clearInterval(this.countdownInterval);
        this.goToBookings();
      }
    }, 1000);
  }

  goToBookings(): void {
    if (this.countdownInterval) {
      clearInterval(this.countdownInterval);
    }
    this.router.navigate(['/my-bookings']);
  }

  goHome(): void {
    if (this.countdownInterval) {
      clearInterval(this.countdownInterval);
    }
    this.router.navigate(['/']);
  }
}
