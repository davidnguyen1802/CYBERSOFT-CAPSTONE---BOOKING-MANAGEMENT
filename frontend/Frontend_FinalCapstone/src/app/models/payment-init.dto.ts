/**
 * Payment Initialization Request
 * Backend: POST /api/transactions/init
 */
export interface PaymentInitRequest {
  bookingId: number;
  paymentMethod: 'PAYOS';
  promotionCode?: string;
}

/**
 * Payment Initialization Response
 * Contains PayOS URL and payment details
 */
export interface PaymentInitResponse {
  code: number;
  message: string;
  data: PaymentInitData;
}

export interface PaymentInitData {
  transactionId: number;
  orderId: string;
  payUrl: string;
  amount: number;
  originalAmount: number;
  discountAmount: number;
  paymentMethod: string;
  expiresAt: string; // ISO DateTime
  promotionCode?: string;
}

/**
 * Payment Status Response
 * Backend: GET /api/payment/status/{bookingId}
 */
export interface PaymentStatusResponse {
  code: number;
  message: string;
  data: PaymentStatusData;
}

export interface PaymentStatusData {
  bookingId: number;
  bookingStatus: string;
  transactionId: number;
  transactionStatus: string;
  amount: number;
  paymentMethod: string;
  paidAt: string | null; // ISO DateTime
  reference: string | null;
  
  // Optional fields for promotion display
  originalAmount?: number;      // Price before discount
  discountAmount?: number;       // Discount from promotion
  orderCode?: string;            // Alternative to transactionId
}











