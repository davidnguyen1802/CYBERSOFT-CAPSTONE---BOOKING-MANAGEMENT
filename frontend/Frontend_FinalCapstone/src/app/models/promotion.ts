/**
 * Promotion Entity Model
 * Represents a promotion available in the system
 */
export interface Promotion {
  id: number;
  code: string;
  name: string;
  description?: string;
  discountType: 'PERCENTAGE' | 'FIXED_AMOUNT';
  discountValue: number;
  minPurchaseLimit?: number;
  maxDiscountAmount?: number;
  usageLimit?: number;
  timesUsed: number;
  startDate?: string;  // ISO DateTime
  endDate?: string;    // ISO DateTime
  active: boolean;
  createdAt?: string;  // ISO DateTime
  updatedAt?: string;  // ISO DateTime
}
