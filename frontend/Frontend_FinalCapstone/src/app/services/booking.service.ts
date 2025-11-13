import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, of } from 'rxjs';
import { tap, catchError, switchMap, map } from 'rxjs/operators';
import { throwError } from 'rxjs';
import { environment } from '../../environments/environment';
import { Booking, BookingRequest, BookingResponse, ApprovalPreviewDTO, HostStatistics } from '../models/booking';
import { BookingPageResponse } from '../responses/booking/booking.response';
import { InitPaymentRequest } from '../models/init-payment.request';
import { InitPaymentResponse } from '../models/init-payment.response';
import { ApiResponse } from '../models/api-response';

export interface BaseResponse<T> {
  code: number;
  message: string;
  data: T;
}

@Injectable({
  providedIn: 'root'
})
export class BookingService {
  // Ensure no trailing slash so concatenations like `${baseUrl}/path` are correct
  private baseUrl = (environment.apiBaseUrl || 'http://localhost:8080').replace(/\/$/, '');

  // Store pending booking data for promotion selection flow
  private pendingBookingData: any = null;

  constructor(private http: HttpClient) {}

  /**
   * Store booking form data before navigating to promotion selection page
   */
  setPendingBookingData(data: any): void {
    this.pendingBookingData = data;
    console.log('💾 Booking data stored for promotion selection:', data);
  }

  /**
   * Get pending booking data
   */
  getPendingBookingData(): any {
    return this.pendingBookingData;
  }

  /**
   * Clear pending booking data after booking creation
   */
  clearPendingBookingData(): void {
    this.pendingBookingData = null;
    console.log('🗑️ Booking data cleared');
  }

  /**
   * Get all bookings for a user with pagination and filtering
   * GET /bookings/user/{userId}
   * @param userId - User ID
   * @param page - Page number (default: 0)
   * @param size - Page size (default: 9)
   * @param sortBy - Sort field (default: 'createdAt')
   * @param sortDirection - Sort direction (default: 'DESC')
   * @param status - Filter by status (optional)
   * @returns Observable<BookingPageResponse>
   */
  getUserBookings(
    userId: number,
    page: number = 0,
    size: number = 9,
    sortBy: string = 'createdAt',
    sortDirection: string = 'DESC',
    status?: string
  ): Observable<BookingPageResponse> {
    const url = `${this.baseUrl}/bookings/user/${userId}`;
    
    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString())
      .set('sortBy', sortBy)
      .set('sortDirection', sortDirection);
    
    // Add status filter if provided
    if (status && status !== 'ALL') {
      params = params.set('status', status);
    }
    
    console.log('=======================================');
    console.log('🏨 BOOKING SERVICE - Getting user bookings');
    console.log('   User ID:', userId);
    console.log('   URL:', url);
    console.log('   Params:', { page, size, sortBy, sortDirection, status });
    console.log('   Timestamp:', new Date().toISOString());
    console.log('=======================================');    return this.http.get<BookingPageResponse>(url, { params }).pipe(
      tap((response) => {
        console.log('✅ BOOKING SERVICE - Success');
        console.log('   Response code:', response.code);
        console.log('   Message:', response.message);
        
        if (response.data && response.data.content) {
          console.log('   Total bookings:', response.data.totalElements);
          console.log('   Current page:', response.data.currentPage + 1, '/', response.data.totalPages);
          console.log('   Page size:', response.data.pageSize);
          console.log('   Items in this page:', response.data.content.length);
          
          if (response.data.content.length > 0) {
            const pending = response.data.content.filter(b => {
              const s = b.status?.toString().toUpperCase();
              return s === 'PENDING';
            }).length;
            const confirmed = response.data.content.filter(b => {
              const s = b.status?.toString().toUpperCase();
              return s === 'CONFIRMED';
            }).length;
            const completed = response.data.content.filter(b => {
              const s = b.status?.toString().toUpperCase();
              return s === 'COMPLETED';
            }).length;
            const cancelled = response.data.content.filter(b => {
              const s = b.status?.toString().toUpperCase();
              return s === 'CANCELLED';
            }).length;
            console.log('   - Pending:', pending);
            console.log('   - Confirmed:', confirmed);
            console.log('   - Completed:', completed);
            console.log('   - Cancelled:', cancelled);
          }
        }
        console.log('=======================================');
      }),
      catchError((error) => {
        console.error('❌ BOOKING SERVICE - Error');
        console.error('   Status:', error.status);
        console.error('   Message:', error.message);
        console.error('   Error:', error);
        console.error('=======================================');
        return throwError(() => error);
      })
    );
  }

  /**
   * Create booking
   * POST /bookings/create
   * Body: BookingRequest (includes promotionCode and originalAmount if promotion used)
   * 
   * ⭐ NEW FLOW: This is where promotions are applied!
   * If promotionCode provided:
   * 1. Backend validates promotion and user ownership
   * 2. Verifies originalAmount matches calculation
   * 3. Calculates discount
   * 4. CONSUMES promotion (status → INACTIVE)
   * 5. Saves booking with DISCOUNTED totalPrice
   * 
   * Returns: Booking with totalPrice = final discounted price
   */
  createBooking(
    bookingData: BookingRequest
  ): Observable<BaseResponse<BookingResponse>> {
    console.log('🔵 API Call: POST /bookings/create', bookingData);
    return this.http.post<BaseResponse<BookingResponse>>(
      `${this.baseUrl}/bookings/create`,
      bookingData
    ).pipe(
      map(response => {
        console.log('✅ POST /bookings/create - Response received:');
        console.log('   📦 Full response:', JSON.stringify(response, null, 2));
        console.log('   🎯 Response code:', response.code);
        console.log('   💬 Response message:', response.message);
        if (response.data) {
          const data = response.data as any;
          console.log('   📋 Booking data:', data);
          console.log('      - Booking ID:', data.id);
          console.log('      - Status:', data.status);
          console.log('      - Total Price:', data.totalPrice);
          console.log('      - Original Amount:', data.originalAmount);
          console.log('      - Discount Amount:', data.discountAmount);
          console.log('      - Promotion Code:', data.promotionCode);
          console.log('      - Payment Deadline:', data.paymentDeadline);
          console.log('      - Confirmed At:', data.confirmedAt);
        }
        return response;
      })
    );
  }

  /**
   * Initialize payment for a booking
   * POST /transactions/init (NEW ENDPOINT)
   * Body: { bookingId, paymentMethod }
   * 
   * ⭐ NEW FLOW: NO promotionCode needed!
   * Promotion was already applied during booking creation (createBooking)
   * 
   * Backend will:
   * 1. Get booking with totalPrice (already discounted)
   * 2. Create PayOS payment link with that amount
   * 3. Return payment URL
   * 
   * @param request - Payment initialization request (no promotion code)
   * @returns Observable<ApiResponse<InitPaymentResponse>>
   */
  initPayment(request: InitPaymentRequest): Observable<ApiResponse<InitPaymentResponse>> {
    const url = `${this.baseUrl}/transactions/init`;
    
    console.log('=======================================');
    console.log('💳 BOOKING SERVICE - Initializing payment');
    console.log('   Booking ID:', request.bookingId);
    console.log('   Payment Method:', request.paymentMethod);
    console.log('   ⭐ NO Promotion Code (applied during booking creation)');
    console.log('   URL:', url);
    console.log('   Timestamp:', new Date().toISOString());
    console.log('=======================================');
    
    return this.http.post<ApiResponse<InitPaymentResponse>>(url, request).pipe(
      tap((response) => {
        console.log('=======================================');
        console.log('✅ BOOKING SERVICE - Payment initialized');
        console.log('📦 FULL RESPONSE:', JSON.stringify(response, null, 2));
        console.log('   Response code:', response.code);
        console.log('   Message:', response.message);
        console.log('   Response.data:', response.data);
        console.log('   Response.data type:', typeof response.data);
        
        if (response.data) {
          console.log('   ⭐ Payment URL (payUrl):', response.data.payUrl);
          console.log('   Order Code:', response.data.orderCode);
          console.log('   Order ID:', response.data.orderId);
          console.log('   Amount:', response.data.amount);
          console.log('   Transaction ID:', response.data.transactionId);
          console.log('   Expires At:', response.data.expiresAt);
          console.log('   Payment Method:', response.data.paymentMethod);
        } else {
          console.error('⚠️ response.data is NULL or UNDEFINED!');
        }
        console.log('=======================================');
      }),
      catchError((error) => {
        console.error('=======================================');
        console.error('❌ BOOKING SERVICE - Payment init error');
        console.error('📦 FULL ERROR:', JSON.stringify(error, null, 2));
        console.error('   Status:', error.status);
        console.error('   Status Text:', error.statusText);
        console.error('   Error Message:', error.error?.message || error.message);
        console.error('   Error Body:', error.error);
        console.error('   URL:', error.url);
        console.error('=======================================');
        return throwError(() => error);
      })
    );
  }

  /**
   * Get all bookings for current user
   * GET /bookings/user/{userId}
   * Note: Backend uses /user/{userId} not /my-bookings
   */
  getMyBookings(userId: number): Observable<BaseResponse<Booking[]>> {
    console.log(`🔵 API Call: GET /bookings/user/${userId}`);
    return this.http.get<BaseResponse<Booking[]>>(`${this.baseUrl}/bookings/user/${userId}`);
  }

  /**
   * Get all bookings for a property
   * GET /bookings/property/{propertyId}
   */
  getPropertyBookings(propertyId: number): Observable<BaseResponse<Booking[]>> {
    console.log(`🔵 API Call: GET /bookings/property/${propertyId}`);
    return this.http.get<BaseResponse<Booking[]>>(`${this.baseUrl}/bookings/property/${propertyId}`);
  }

  /**
   * Get all bookings (Admin only)
   * GET /bookings/all
   */
  getAllBookings(): Observable<BaseResponse<Booking[]>> {
    console.log('🔵 API Call: GET /bookings/all');
    return this.http.get<BaseResponse<Booking[]>>(`${this.baseUrl}/bookings/all`);
  }

  /**
   * Update booking status
   * PUT /bookings/{id}/status?status={status}
   */
  updateBookingStatus(id: number, status: string): Observable<BaseResponse<Booking>> {
    console.log(`🔵 API Call: PUT /bookings/${id}/status?status=${status}`);
    return this.http.put<BaseResponse<Booking>>(`${this.baseUrl}/bookings/${id}/status?status=${status}`, {});
  }

  /**
   * Check property availability
   * GET /bookings/check-availability
   */
  checkAvailability(propertyId: number, checkIn: string, checkOut: string): Observable<BaseResponse<boolean>> {
    console.log(`🔵 API Call: GET /bookings/check-availability?propertyId=${propertyId}`);
    return this.http.get<BaseResponse<boolean>>(
      `${this.baseUrl}/bookings/check-availability`,
      {
        params: {
          propertyId: propertyId.toString(),
          checkIn: checkIn,
          checkOut: checkOut
        }
      }
    );
  }

  /**
   * Delete booking (Admin only)
   * DELETE /bookings/{id}
   */
  deleteBooking(id: number): Observable<BaseResponse<null>> {
    console.log(`🔵 API Call: DELETE /bookings/${id}`);
    return this.http.delete<BaseResponse<null>>(`${this.baseUrl}/bookings/${id}`);
  }

  /**
   * Get booking by ID
   * GET /bookings/{id}
   */
  getBookingById(id: number): Observable<BaseResponse<Booking>> {
    console.log(`🔵 API Call: GET /bookings/${id}`);
    return this.http.get<BaseResponse<Booking>>(`${this.baseUrl}/bookings/${id}`);
  }

  /**
   * Cancel a booking
   * PUT /bookings/{id}/cancel?reason={reason}
   */
  cancelBooking(id: number, reason?: string): Observable<BaseResponse<Booking>> {
    let params = new HttpParams();
    if (reason) {
      params = params.set('reason', reason);
    }
    console.log(`🔵 API Call: PUT /bookings/${id}/cancel?reason=${reason || ''}`);
    return this.http.put<BaseResponse<Booking>>(
      `${this.baseUrl}/bookings/${id}/cancel`,
      {},
      { params }
    );
  }

  // ========== HOST METHODS ==========

  /**
   * Preview approval conflicts (Host)
   * GET /bookings/{bookingId}/approve/preview?hostId={hostId}
   */
  previewApproval(
    bookingId: number,
    hostId: number
  ): Observable<BaseResponse<ApprovalPreviewDTO>> {
    const params = new HttpParams().set('hostId', hostId.toString());
    console.log(`🔵 API Call: GET /bookings/${bookingId}/approve/preview?hostId=${hostId}`);
    return this.http.get<BaseResponse<ApprovalPreviewDTO>>(
      `${this.baseUrl}/bookings/${bookingId}/approve/preview`,
      { params }
    );
  }

  /**
   * Approve booking (Host)
   * PUT /bookings/{bookingId}/approve?hostId={hostId}
   */
  approveBooking(
    bookingId: number,
    hostId: number
  ): Observable<BaseResponse<Booking>> {
    const params = new HttpParams().set('hostId', hostId.toString());
    console.log(`🔵 API Call: PUT /bookings/${bookingId}/approve?hostId=${hostId}`);
    return this.http.put<BaseResponse<Booking>>(
      `${this.baseUrl}/bookings/${bookingId}/approve`,
      {},
      { params }
    );
  }

  /**
   * Reject booking (Host)
   * PUT /bookings/{bookingId}/reject?hostId={hostId}&reason={reason}
   */
  rejectBooking(
    bookingId: number,
    hostId: number,
    reason?: string
  ): Observable<BaseResponse<Booking>> {
    let params = new HttpParams().set('hostId', hostId.toString());
    if (reason) {
      params = params.set('reason', reason);
    }
    console.log(`🔵 API Call: PUT /bookings/${bookingId}/reject?hostId=${hostId}&reason=${reason || ''}`);
    return this.http.put<BaseResponse<Booking>>(
      `${this.baseUrl}/bookings/${bookingId}/reject`,
      {},
      { params }
    );
  }

  /**
   * Filter host bookings by status
   * GET /bookings/host/{hostId}/filter?status=PENDING&status=CONFIRMED&page=0&size=20
   */
  filterHostBookings(
    hostId: number,
    statuses: string[] = [],
    page: number = 0,
    size: number = 20,
    sortBy: string = 'createdAt',
    sortDirection: 'ASC' | 'DESC' = 'DESC'
  ): Observable<BookingPageResponse> {
    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString())
      .set('sortBy', sortBy)
      .set('sortDirection', sortDirection);

    // Add multiple status filters
    statuses.forEach(status => {
      params = params.append('status', status);
    });

    console.log(`🔵 API Call: GET /bookings/host/${hostId}/filter`, { statuses, page, size });
    return this.http.get<BookingPageResponse>(
      `${this.baseUrl}/bookings/host/${hostId}/filter`,
      { params }
    );
  }

  /**
   * Get all bookings for a host (paginated)
   * GET /bookings/host/{hostId}/filter?page=0&size=10&sortBy=createdAt&sortDirection=DESC
   * 
   * NOTE: Backend uses /filter endpoint with optional status filtering
   * When called without status param, returns ALL bookings for the host
   */
  getBookingsByHost(
    hostId: number,
    page: number = 0,
    size: number = 10,
    sortBy: string = 'createdAt',
    sortDirection: 'ASC' | 'DESC' = 'DESC'
  ): Observable<BookingPageResponse> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString())
      .set('sortBy', sortBy)
      .set('sortDirection', sortDirection);

    const url = `${this.baseUrl}/bookings/host/${hostId}/filter`;

    console.log('=======================================');
    console.log('🏨 BOOKING SERVICE - Getting host bookings');
    console.log('   Host ID:', hostId);
    console.log('   URL:', url);
    console.log('   Params:', { page, size, sortBy, sortDirection });
    console.log('   Full URL:', `${url}?${params.toString()}`);
    console.log('   Timestamp:', new Date().toISOString());
    console.log('=======================================');

    return this.http.get<BookingPageResponse>(url, { params }).pipe(
      tap((response) => {
        console.log('✅ BOOKING SERVICE - Host bookings loaded successfully');
        console.log('   Response code:', response.code);
        console.log('   Message:', response.message);
        if (response.data) {
          console.log('   Total bookings:', response.data.totalElements);
          console.log('   Current page:', response.data.currentPage);
          console.log('   Page size:', response.data.pageSize);
          console.log('   Total pages:', response.data.totalPages);
          console.log('   Bookings count:', response.data.content?.length || 0);
        }
        console.log('=======================================');
      }),
      catchError((error) => {
        console.error('=======================================');
        console.error('❌ BOOKING SERVICE - Error loading host bookings');
        console.error('   Host ID:', hostId);
        console.error('   URL:', url);
        console.error('   Status:', error.status);
        console.error('   Status Text:', error.statusText);
        console.error('   Error message:', error.message);
        console.error('   Error body:', error.error);
        console.error('   Full error object:', error);
        console.error('=======================================');
        return throwError(() => error);
      })
    );
  }

  /**
   * Get property bookings in date range (Host calendar view)
   * GET /bookings/property/{propertyId}/date-range?hostId={hostId}&startDate={date}&endDate={date}
   */
  getPropertyBookingsInDateRange(
    propertyId: number,
    hostId: number,
    startDate: string,
    endDate: string,
    page: number = 0,
    size: number = 100
  ): Observable<BookingPageResponse> {
    const params = new HttpParams()
      .set('hostId', hostId.toString())
      .set('startDate', startDate)
      .set('endDate', endDate)
      .set('page', page.toString())
      .set('size', size.toString());

    console.log(`🔵 API Call: GET /bookings/property/${propertyId}/date-range`, { hostId, startDate, endDate });
    return this.http.get<BookingPageResponse>(
      `${this.baseUrl}/bookings/property/${propertyId}/date-range`,
      { params }
    );
  }

  /**
   * Get host statistics
   * GET /host/{hostId}/statistics
   * 
   * NOTE: This is a mock implementation since backend doesn't have this endpoint yet.
   * Replace with real API call when backend implements it.
   */
  getHostStatistics(hostId: number): Observable<BaseResponse<HostStatistics>> {
    console.log(`🔵 API Call: GET /host/${hostId}/statistics`);
    
    // Mock implementation - calculate from bookings
    return this.getBookingsByHost(hostId, 0, 1000).pipe(
      tap(response => console.log('📊 Calculating statistics from bookings:', response)),
      switchMap((response) => {
        const mockStats: HostStatistics = {
          totalProperties: 5,
          pendingBookings: 0,
          confirmedBookings: 0,
          completedBookings: 0,
          totalRevenue: 0,
          averageRating: 4.7
        };

        if (response.code === 200 && response.data) {
          const bookings = response.data.content;
          
          mockStats.pendingBookings = bookings.filter((b: Booking) => {
            const status = b.status?.toString().toUpperCase();
            return status === 'PENDING';
          }).length;
          
          mockStats.confirmedBookings = bookings.filter((b: Booking) => {
            const status = b.status?.toString().toUpperCase();
            return status === 'CONFIRMED';
          }).length;
          
          mockStats.completedBookings = bookings.filter((b: Booking) => {
            const status = b.status?.toString().toUpperCase();
            return status === 'COMPLETED';
          }).length;
          
          mockStats.totalRevenue = bookings
            .filter((b: Booking) => {
              const status = b.status?.toString().toUpperCase();
              return status === 'COMPLETED' || status === 'PAID';
            })
            .reduce((sum: number, b: Booking) => sum + (b.totalPrice || 0), 0);
        }

        console.log('📊 Calculated statistics:', mockStats);

        return of({
          code: 200,
          message: 'Statistics retrieved successfully',
          data: mockStats
        });
      }),
      catchError(error => {
        console.error('❌ Error fetching bookings for statistics:', error);
        // Return mock data on error
        return of({
          code: 200,
          message: 'Mock statistics (fallback)',
          data: {
            totalProperties: 5,
            pendingBookings: 8,
            confirmedBookings: 12,
            completedBookings: 120,
            totalRevenue: 45000000,
            averageRating: 4.7
          }
        });
      })
    );
  }
}
