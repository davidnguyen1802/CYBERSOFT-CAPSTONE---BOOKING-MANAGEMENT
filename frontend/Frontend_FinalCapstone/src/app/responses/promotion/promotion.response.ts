import { UserPromotionDTO } from '../../models/user-promotion.dto';
import { PageResponse } from '../../models/property';

export interface PromotionResponse {
  code: number;
  message: string;
  data: UserPromotionDTO[];
}

export interface PromotionPageResponse {
  code: number;
  message: string;
  data: PageResponse<UserPromotionDTO>;
}
