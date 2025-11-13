import { Review } from '../../models/review';

export interface ReviewResponse {
  code: number;
  message: string;
  data: Review[] | Review;
}
