import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { tap, catchError } from 'rxjs/operators';
import { throwError } from 'rxjs';
import { environment } from '../../environments/environment';
import { PaymentWebhookData } from '../models/booking';
import { 
  PaymentInitRequest, 
  PaymentInitResponse, 
  PaymentStatusResponse 
} from '../models/payment-init.dto';

export interface BaseResponse<T> {
  code: number;
  message: string;
  data: T;
}

@Injectable({
  providedIn: 'root'
})
export class PaymentService {
  private baseUrl = (environment.apiBaseUrl || 'http://localhost:8080').replace(/\/$/, '');

  constructor(private http: HttpClient) {}

  /**
   * @deprecated This method uses the OLD payment initialization endpoint.
   * 
   * **MIGRATION REQUIRED:**
   * - OLD ENDPOINT: POST /api/transactions/init (DEPRECATED)
   * - NEW ENDPOINT: POST /bookings/init-payment (CURRENT)
   * 
   * **Please use BookingService.initPayment() instead:**
   * ```typescript
   * // OLD (deprecated):
   * this.paymentService.initPayment(bookingId, promotionCode)
   * 
   * // NEW (correct):
   * this.bookingService.initPayment({
   *   bookingId: bookingId,
   *   promotionCode: promotionCode,
   *   returnUrl: `${window.location.origin}/payment/success`,
   *   cancelUrl: `${window.location.origin}/payment/cancel`
   * })
   * ```
   * 
   * This method is kept for backward compatibility only and may be removed in future versions.
   * 
   * @param bookingId - Booking ID (must be in CONFIRMED status)
   * @param promotionCode - Optional promotion code
   * @returns Observable<PaymentInitResponse> - Contains PayOS URL to redirect to
   */
  initPayment(bookingId: number, promotionCode?: string): Observable<PaymentInitResponse> {
    console.warn('⚠️ ================================================');
    console.warn('⚠️ DEPRECATED: PaymentService.initPayment()');
    console.warn('⚠️ Please migrate to BookingService.initPayment()');
    console.warn('⚠️ Old endpoint: POST /api/transactions/init');
    console.warn('⚠️ New endpoint: POST /bookings/init-payment');
    console.warn('⚠️ ================================================');

    const request: PaymentInitRequest = {
      bookingId,
      paymentMethod: 'PAYOS',
      promotionCode
    };

    console.log('=======================================');
    console.log('💳 PAYMENT SERVICE - Initializing payment (DEPRECATED METHOD)');
    console.log('   Booking ID:', bookingId);
    console.log('   Payment Method:', request.paymentMethod);
    console.log('   Promotion Code:', promotionCode || 'None');
    console.log('   Timestamp:', new Date().toISOString());
    console.log('=======================================');

    return this.http.post<PaymentInitResponse>(`${this.baseUrl}/api/transactions/init`, request).pipe(
      tap((response) => {
        console.log('✅ PAYMENT SERVICE - Init successful (DEPRECATED METHOD)');
        console.log('   Response code:', response.code);
        console.log('   Message:', response.message);
        
        if (response.data) {
          console.log('   Transaction ID:', response.data.transactionId);
          console.log('   Order ID:', response.data.orderId);
          console.log('   Amount:', response.data.amount);
          console.log('   Original Amount:', response.data.originalAmount);
          console.log('   Discount:', response.data.discountAmount);
          console.log('   PayOS URL:', response.data.payUrl);
          console.log('   Expires At:', response.data.expiresAt);
        }
        console.log('=======================================');
      }),
      catchError((error) => {
        console.error('❌ PAYMENT SERVICE - Init error (DEPRECATED METHOD)');
        console.error('   Status:', error.status);
        console.error('   Message:', error.message);
        console.error('   Error:', error);
        console.error('=======================================');
        return throwError(() => error);
      })
    );
  }

  /**
   * Check payment status by booking ID
   * GET /api/payment/status/{bookingId}
   * 
   * Use this to poll payment status after user returns from PayOS
   * 
   * @param bookingId - Booking ID
   * @returns Observable<PaymentStatusResponse>
   */
  checkPaymentStatus(bookingId: number): Observable<PaymentStatusResponse> {
    console.log('=======================================');
    console.log('🔍 PAYMENT SERVICE - Checking payment status');
    console.log('   Booking ID:', bookingId);
    console.log('   Timestamp:', new Date().toISOString());
    console.log('=======================================');

    return this.http.get<PaymentStatusResponse>(`${this.baseUrl}/api/payment/status/${bookingId}`).pipe(
      tap((response) => {
        console.log('✅ PAYMENT SERVICE - Status check response');
        console.log('   Response code:', response.code);
        console.log('   Message:', response.message);
        
        if (response.data) {
          console.log('   Booking Status:', response.data.bookingStatus);
          console.log('   Transaction Status:', response.data.transactionStatus);
          console.log('   Amount:', response.data.amount);
          console.log('   Paid At:', response.data.paidAt || 'Not paid yet');
        }
        console.log('=======================================');
      }),
      catchError((error) => {
        console.error('❌ PAYMENT SERVICE - Status check error');
        console.error('   Status:', error.status);
        console.error('   Message:', error.message);
        console.error('   Error:', error);
        console.error('=======================================');
        return throwError(() => error);
      })
    );
  }

  /**
   * @deprecated Frontend should NOT call webhook directly
   * Webhooks are called by PayOS → Backend only
   * 
   * Send payment webhook notification
   * POST /payment/webhook
   */
  sendWebhookNotification(webhookData: PaymentWebhookData): Observable<any> {
    console.warn('⚠️ WARNING: Frontend should not call webhook directly!');
    console.log('🔵 API Call: POST /payment/webhook', webhookData);
    return this.http.post(`${this.baseUrl}/payment/webhook`, webhookData);
  }

  /**
   * @deprecated Use checkPaymentStatus(bookingId) instead
   * Get payment status by order code
   * GET /api/payment/status/{orderCode}
   */
  getPaymentStatus(orderCode: string): Observable<BaseResponse<any>> {
    console.log(`🔵 API Call: GET /api/payment/status/${orderCode}`);
    return this.http.get<BaseResponse<any>>(`${this.baseUrl}/api/payment/status/${orderCode}`);
  }
}
