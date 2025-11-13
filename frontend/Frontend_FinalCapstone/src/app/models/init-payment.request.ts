/**
 * InitPaymentRequest - Request to initialize payment
 * ⭐ ENDPOINT: POST /transactions/init
 * ⭐ Promotion discount is already applied in booking.totalPrice during booking creation
 * Backend will use booking.totalPrice directly (no promotion logic here)
 */
export interface InitPaymentRequest {
  bookingId: number;
  paymentMethod: string;   // "PAYOS" - Payment method
  // promotionCode removed - not needed anymore
}
