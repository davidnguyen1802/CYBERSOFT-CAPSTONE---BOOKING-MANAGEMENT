export interface Review {
  reviewId: number;
  propertyName: string;
  propertyId: number;
  comment: string;
  rating: number;
  reviewDate: string;
  updatedDate?: string; // For tracking edits
}
