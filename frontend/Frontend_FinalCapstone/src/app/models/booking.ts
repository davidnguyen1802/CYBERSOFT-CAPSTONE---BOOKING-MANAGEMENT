export interface Booking {
  id?: number;
  userId?: number;
  userName?: string;
  propertyId: number;
  propertyName?: string;
  propertyPricePerNight?: number;
  checkIn: string; // ISO DateTime format: "2025-11-01T14:00:00"
  checkOut: string; // ISO DateTime format
  totalPrice?: number; // ⭐ NEW FLOW: This is DISCOUNTED price if promotion was applied
  numAdults: number;
  numChildren: number;
  num_teenager: number; // Changed from numTeenager to match backend
  num_infant: number; // Changed from numInfant to match backend
  numPet?: number; // Added missing field
  notes?: string;
  status?: BookingStatus;
  
  // ⭐ NEW: Promotion-related fields (discount at booking creation flow)
  promotionCode?: string;      // Code of promotion applied during booking creation
  originalAmount?: number;      // Original price before discount
  discountApplied?: number;     // Discount amount applied
  
  // Added fields from Backend
  confirmedAt?: string;         // ISO DateTime
  cancelledAt?: string;          // ISO DateTime
  cancelledBy?: 'guest' | 'host' | 'system';
  cancelReason?: string;
  paymentDeadline?: string;     // ISO DateTime (confirmedAt + 24h)
  isPaymentExpired?: boolean;
  
  createdAt?: string;
  updatedAt?: string;
}

export enum BookingStatus {
  PENDING = 'PENDING',
  CONFIRMED = 'CONFIRMED',
  PAID = 'PAID',
  REJECTED = 'REJECTED',
  CANCELLED = 'CANCELLED',
  COMPLETED = 'COMPLETED'
}

// BookingRequest - Input to create booking (matches backend request exactly)
export interface BookingRequest {
  userId?: number; // Added - will be populated from token service
  propertyId: number; // Required
  checkIn: string; // Required, ISO DateTime format
  checkOut: string; // Required, ISO DateTime format
  numAdults: number; // Required, minimum 1
  numChildren?: number; // Optional, minimum 0
  num_teenager?: number; // Optional, backend uses underscore
  num_infant?: number; // Optional, backend uses underscore
  numPet?: number; // Added missing field
  notes?: string; // Optional, max 1000 characters
  
  // ⭐ NEW FLOW: Promotion applied at booking creation (not during payment)
  promotionCode?: string; // Optional - Code of promotion to apply
  originalAmount?: number; // Required if promotionCode provided - For price verification
}

// Create Booking Response
export interface BookingResponse {
  booking: Booking;
  payment?: PayOSPaymentData; // Changed from checkoutUrl/orderCode to full payment object
}

// PayOS Payment Data (from backend documentation)
export interface PayOSPaymentData {
  bin: string;
  accountNumber: string;
  accountName: string;
  amount: number;
  description: string;
  orderCode: number; // Same as booking ID
  currency: string; // "VND"
  paymentLinkId: string;
  status: string; // "PENDING", "PAID", "CANCELLED"
  checkoutUrl: string; // Redirect user to this URL
  qrCode: string; // Base64 QR code image
}

export interface PaymentWebhookData {
  orderCode: string;
  status: string;
  success: boolean;
}

// Approval Preview DTOs for Host flow
export interface ApprovalPreviewDTO {
  bookingToApprove: Booking;
  willBeAutoRejected: ConflictingBookingDTO[];
  totalConflicts: number;
  warning?: string;
}

export interface ConflictingBookingDTO {
  id: number;
  guestName: string;
  guestEmail: string;
  checkIn: string;
  checkOut: string;
  reason: string;
}

// Host Statistics
export interface HostStatistics {
  totalProperties: number;
  pendingBookings: number;
  confirmedBookings: number;
  completedBookings: number;
  totalRevenue: number;
  averageRating: number;
}
