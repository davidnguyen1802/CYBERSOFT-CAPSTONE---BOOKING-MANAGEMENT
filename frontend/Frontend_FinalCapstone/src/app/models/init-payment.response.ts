/**
 * InitPaymentResponse - Response from payment initialization
 * Based on backend API: POST /transactions/init (Simplified - No Promotion Fields)
 * Note: Promotion discount is already included in booking.totalPrice
 */
export interface InitPaymentResponse {
  transactionId?: string;     // Transaction ID
  orderId?: string;           // Order ID from response
  orderCode?: number;         // PayOS order code
  amount: number;             // Amount to pay (already includes any promotion discount from booking)
  paymentMethod?: string;     // "PAYOS"
  expiresAt?: string;         // Payment link expiration time
  
  // ⭐ Backend returns "payUrl" not "paymentUrl"
  payUrl: string;             // PayOS payment URL to redirect (BE returns this field)
  paymentUrl?: string;        // Alias for payUrl (for compatibility)
}
