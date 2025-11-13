import { Promotion } from './promotion';

/**
 * PromotionUsage - Represents a single usage of a promotion
 */
export interface PromotionUsage {
  id: number;
  idBooking: number;
  idUserPromotion: number;
  discountAmount: number;
  usedDate: string; // ISO DateTime
}

/**
 * UserPromotionDTO - Represents a promotion assigned to a user
 * 
 * ✅ UPDATED to match ACTUAL backend API response format from GET /promotions/{userId}:
 * - Uses assignedDate (not claimedAt)
 * - Uses expiresDate (not expiredAt)
 * - Has promotionUsages array
 * - Has active boolean (not status enum)
 * - Includes userName
 * 
 * API Endpoint: GET /promotions/{userId}
 */
export interface UserPromotionDTO {
  id: number;
  assignedDate: string;        // ISO DateTime - when promotion was assigned
  expiresDate: string;         // ISO DateTime - expiration date
  promotionName: string;
  promotionCode: string;
  userId: number;
  userName: string;
  promotionUsages: PromotionUsage[]; // Array of usage records
  active: boolean;             // true if promotion is still valid
  
  // ✅ Optional fields (may be returned by some endpoints)
  discountPercentage?: number;
  maxDiscountAmount?: number;
  minBookingAmount?: number;
  description?: string;
}

/**
 * Helper function to compute promotion status based on API data
 * 
 * Logic:
 * - ACTIVE: active=true + not expired + not used yet
 * - USED: has promotionUsages records
 * - EXPIRED: active=false OR past expiresDate (and not used)
 */
export function getPromotionStatus(promo: UserPromotionDTO): 'ACTIVE' | 'USED' | 'EXPIRED' {
  // Check if used (has usage records)
  if (promo.promotionUsages && promo.promotionUsages.length > 0) {
    return 'USED';
  }
  
  // Check if expired
  const now = new Date();
  const expiresDate = new Date(promo.expiresDate);
  const isExpired = expiresDate <= now;
  
  if (!promo.active || isExpired) {
    return 'EXPIRED';
  }
  
  // Otherwise, it's active
  return 'ACTIVE';
}

/**
 * Legacy interface for backward compatibility
 * @deprecated Use UserPromotionDTO instead
 */
export interface UserPromotionDTOLegacy {
  id: number;
  userId: number;
  promotion: Promotion;  // Nested promotion object (OLD FORMAT)
  assignedAt: string;    // ISO DateTime (was assignedDate)
  expiresAt?: string;    // ISO DateTime (was expiresDate)
  status: 'ACTIVE' | 'INACTIVE';  // Old status values
  isLocked: boolean;     // New field - locked during payment
}
