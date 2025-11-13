/**
 * PromotionPreviewDTO - Preview of discount calculation
 * Used for validation before payment
 * 
 * ⚠️ IMPORTANT: Backend Java field "isValid" becomes "valid" in JSON
 * due to Jackson's boolean serialization (removes "is" prefix)
 */
export interface PromotionPreviewDTO {
  valid: boolean;              // ✅ Backend sends "valid", not "isValid"
  errorMessage?: string | null;
  originalAmount: number;
  discountAmount: number;
  finalAmount: number;
  promotionCode?: string;
  promotionName?: string;
  description?: string | null;
  discountType?: string | null;    // "PERCENTAGE" or "FIXED"
  discountValue?: number | null;
}
