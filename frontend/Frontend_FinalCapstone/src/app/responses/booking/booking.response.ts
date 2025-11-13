import { Booking } from '../../models/booking';
import { PageResponse } from '../../models/property';

export interface BookingResponse {
  code: number;
  message: string;
  data: Booking[];
}

export interface BookingPageResponse {
  code: number;
  message: string;
  data: PageResponse<Booking>;
}
