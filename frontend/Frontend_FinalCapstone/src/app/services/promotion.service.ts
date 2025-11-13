import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { tap, catchError } from 'rxjs/operators';
import { throwError } from 'rxjs';
import { environment } from '../../environments/environment';
import { Promotion } from '../models/promotion';
import { UserPromotionDTO } from '../models/user-promotion.dto';
import { PromotionPreviewDTO } from '../models/promotion-preview.dto';
import { ApiResponse, PageContent } from '../models/api-response';
import { TokenService } from './token.service';

@Injectable({
  providedIn: 'root'
})
export class PromotionService {
  // Ensure no trailing slash so concatenations like `${baseUrl}/path` are correct
  private baseUrl = (environment.apiBaseUrl || 'http://localhost:8080').replace(/\/$/, '');
  private apiUrl = `${this.baseUrl}/promotions`;

  constructor(
    private http: HttpClient,
    private tokenService: TokenService
  ) {}

  /**
   * Get all available public promotions (paginated)
   * GET /promotions?pageIndex=0&pageSize=10
   * 
   * @param pageIndex - Page index (0-based)
   * @param pageSize - Number of items per page
   * @returns Observable<ApiResponse<PagedResponse<Promotion>>>
   */
  getPromotions(pageIndex: number = 0, pageSize: number = 10): Observable<ApiResponse<PageContent<Promotion>>> {
    const url = `${this.apiUrl}`;
    const params = new HttpParams()
      .set('pageIndex', pageIndex.toString())
      .set('pageSize', pageSize.toString());
    
    console.log('=======================================');
    console.log('🎫 PROMOTION SERVICE - Getting public promotions');
    console.log('   URL:', url);
    console.log('   Params:', { pageIndex, pageSize });
    console.log('   Timestamp:', new Date().toISOString());
    console.log('=======================================');
    
    return this.http.get<ApiResponse<PageContent<Promotion>>>(url, { params }).pipe(
      tap((response) => {
        console.log('✅ PROMOTION SERVICE - Public promotions loaded');
        console.log('   Response code:', response.code);
        console.log('   Total promotions:', response.data.totalElements);
        console.log('   Current page:', response.data.pageNumber + 1, '/', response.data.totalPages);
        console.log('=======================================');
      }),
      catchError((error) => {
        console.error('❌ PROMOTION SERVICE - Error loading public promotions');
        console.error('   Status:', error.status);
        console.error('   Error:', error);
        console.error('=======================================');
        return throwError(() => error);
      })
    );
  }

  /**
   * Claim a promotion code
   * POST /promotions/claim
   * Body: { code: "SUMMER2025" }
   * 
   * @param code - Promotion code to claim
   * @returns Observable<ApiResponse<UserPromotionDTO>>
   */
  claimPromotion(code: string): Observable<ApiResponse<UserPromotionDTO>> {
    const url = `${this.apiUrl}/claim`;
    const body = { code };
    
    console.log('=======================================');
    console.log('🎁 PROMOTION SERVICE - Claiming promotion');
    console.log('   Code:', code);
    console.log('   URL:', url);
    console.log('   Timestamp:', new Date().toISOString());
    console.log('=======================================');
    
    return this.http.post<ApiResponse<UserPromotionDTO>>(url, body).pipe(
      tap((response) => {
        console.log('✅ PROMOTION SERVICE - Promotion claimed');
        console.log('   Response code:', response.code);
        console.log('   Message:', response.message);
        if (response.data) {
          console.log('   Promotion:', response.data.promotionName);
          console.log('   Active:', response.data.active);
          console.log('   Expires:', response.data.expiresDate);
        }
        console.log('=======================================');
      }),
      catchError((error) => {
        console.error('❌ PROMOTION SERVICE - Claim error');
        console.error('   Status:', error.status);
        console.error('   Message:', error.error?.message || error.message);
        console.error('=======================================');
        return throwError(() => error);
      })
    );
  }

  /**
   * Get user's claimed promotions (My Promotions)
   * GET /promotions/{userId}?page=0&size=10
   * 
   * Uses the CORRECT endpoint as per API documentation:
   * - Endpoint: GET /promotions/{userId}
   * - Query params: page, size, sortBy, sortDirection
   * 
   * @param pageIndex - Page index (0-based) - maps to 'page' query param
   * @param pageSize - Number of items per page - maps to 'size' query param
   * @returns Observable<ApiResponse<PagedResponse<UserPromotionDTO>>>
   */
  getMyPromotions(pageIndex: number = 0, pageSize: number = 10): Observable<ApiResponse<PageContent<UserPromotionDTO>>> {
    // ✅ Get userId from JWT token
    const userId = this.tokenService.getUserId();
    
    if (userId === 0) {
      console.error('❌ PROMOTION SERVICE - Cannot get promotions: User ID is 0 (not logged in)');
      return throwError(() => new Error('User not logged in'));
    }
    
    // ✅ FIXED: Use correct endpoint format /promotions/{userId}
    const url = `${this.apiUrl}/${userId}`;
    
    // ✅ FIXED: Use 'page' and 'size' (not pageIndex/pageSize) as per API doc
    const params = new HttpParams()
      .set('page', pageIndex.toString())
      .set('size', pageSize.toString())
      .set('sortBy', 'id')
      .set('sortDirection', 'DESC');
    
    console.log('=======================================');
    console.log('🎫 PROMOTION SERVICE - Getting my promotions');
    console.log('   User ID:', userId);
    console.log('   URL:', url);
    console.log('   Params:', { page: pageIndex, size: pageSize, sortBy: 'id', sortDirection: 'DESC' });
    console.log('   Timestamp:', new Date().toISOString());
    console.log('=======================================');
    
    return this.http.get<ApiResponse<PageContent<UserPromotionDTO>>>(url, { params }).pipe(
      tap((response) => {
        console.log('✅ PROMOTION SERVICE - My promotions loaded');
        console.log('   Response code:', response.code);
        console.log('   Response data:', response.data);
        
        if (response.data && response.data.totalElements !== null && response.data.totalElements !== undefined) {
          console.log('   Total promotions:', response.data.totalElements);
          console.log('   Current page:', response.data.pageNumber + 1, '/', response.data.totalPages);
          if (response.data.content && response.data.content.length > 0) {
            // ✅ FIXED: Use p.active (boolean) instead of p.status (enum)
            const active = response.data.content.filter(p => p.active === true).length;
            const inactive = response.data.content.filter(p => p.active === false).length;
            const used = response.data.content.filter(p => p.promotionUsages && p.promotionUsages.length > 0).length;
            console.log('   - Active:', active);
            console.log('   - Inactive/Expired:', inactive);
            console.log('   - Used:', used);
          }
        } else {
          console.warn('⚠️ Response data is null or missing totalElements');
          console.warn('   This may indicate a backend error or empty response');
        }
        console.log('=======================================');
      }),
      catchError((error) => {
        console.error('❌ PROMOTION SERVICE - Error loading my promotions');
        console.error('   Status:', error.status);
        console.error('   Error:', error);
        console.error('=======================================');
        return throwError(() => error);
      })
    );
  }

  /**
   * ⭐ NEW FLOW: Validate promotion for booking
   * GET /promotions/validate?code=X&propertyId=Y&checkIn=Z&checkOut=W
   * 
   * Backend signature:
   * @GetMapping("/validate")
   * validatePromotion(
   *   @RequestParam String code,
   *   @RequestParam Integer propertyId,
   *   @RequestParam @DateTimeFormat(iso = ISO.DATE_TIME) LocalDateTime checkIn,
   *   @RequestParam @DateTimeFormat(iso = ISO.DATE_TIME) LocalDateTime checkOut
   * )
   * 
   * @param code - Promotion code (string)
   * @param propertyId - Property ID
   * @param checkIn - Check-in datetime (ISO-8601 format)
   * @param checkOut - Check-out datetime (ISO-8601 format)
   * @returns Observable<ApiResponse<PromotionPreviewDTO>>
   */
  validatePromotionForBooking(
    code: string,
    propertyId: number,
    checkIn: string,
    checkOut: string
  ): Observable<ApiResponse<PromotionPreviewDTO>> {
    const url = `${this.apiUrl}/validate`;
    
    // ✅ Match backend @RequestParam names exactly
    const params = new HttpParams()
      .set('code', code)
      .set('propertyId', propertyId.toString())
      .set('checkIn', checkIn)    // ISO-8601: "2025-12-01T14:00:00"
      .set('checkOut', checkOut); // ISO-8601: "2025-12-05T11:00:00"
    
    // 🔍 DEBUG: Check token before making request
    const token = this.tokenService.getToken();
    const userId = this.tokenService.getUserId();
    
    console.log('=======================================');
    console.log('🎟️ PROMOTION SERVICE - Validating promotion for booking');
    console.log('   Promotion Code:', code);
    console.log('   Property ID:', propertyId);
    console.log('   Check-in:', checkIn);
    console.log('   Check-out:', checkOut);
    console.log('   URL:', url);
    console.log('   🔑 Token exists:', !!token);
    console.log('   👤 User ID:', userId);
    if (token) {
      console.log('   🔑 Token preview:', token.substring(0, 20) + '...');
      console.log('   ⏰ Token expired:', this.tokenService.isTokenExpired());
    }
    console.log('   Timestamp:', new Date().toISOString());
    console.log('=======================================');
    
    return this.http.get<ApiResponse<PromotionPreviewDTO>>(url, { params }).pipe(
      tap((response) => {
        console.log('✅ PROMOTION SERVICE - Validation response');
        console.log('   Response code:', response.code);
        console.log('   Message:', response.message);
        console.log('   📦 FULL RESPONSE:', JSON.stringify(response, null, 2));
        console.log('   📦 RESPONSE.DATA:', JSON.stringify(response.data, null, 2));
        
        if (response.data) {
          console.log('   Is Valid:', response.data.valid);  // ✅ Changed from isValid to valid
          console.log('   Is Valid type:', typeof response.data.valid);
          console.log('   Original Amount:', response.data.originalAmount);
          console.log('   Discount Amount:', response.data.discountAmount);
          console.log('   Final Amount:', response.data.finalAmount);
          if (response.data.valid === true) {  // ✅ Changed from isValid to valid
            console.log('   💰 You save:', response.data.discountAmount, 'VND');
          } else if (response.data.valid === false) {  // ✅ Changed from isValid to valid
            console.log('   ❌ Error:', response.data.errorMessage);
          } else {
            console.log('   ⚠️ WARNING: valid field is null or undefined!');
          }
        }
        console.log('=======================================');
      }),
      catchError((error) => {
        console.error('❌ PROMOTION SERVICE - Validation error');
        console.error('   Status:', error.status);
        console.error('   Status Text:', error.statusText);
        console.error('   Error Message:', error.error?.message || error.message);
        console.error('   Full Error:', error);
        if (error.status === 401) {
          console.error('   🔒 AUTHENTICATION ERROR - Token may be invalid or expired');
          console.error('   🔍 Check if token exists:', !!token);
          console.error('   🔍 Check if token expired:', token ? this.tokenService.isTokenExpired() : 'No token');
        }
        console.error('=======================================');
        return throwError(() => error);
      })
    );
  }

  /**
   * OLD FLOW: Validate promotion for booking form by code
   * GET /promotions/validate?code=X&propertyId=Y&checkIn=...&checkOut=...
   * 
   * This DOES NOT consume the promotion, only validates and calculates discount
   * User will see the discount preview before creating booking
   * Promotion will be consumed when booking is created
   * 
   * @param code - Promotion code
   * @param propertyId - Property ID
   * @param checkIn - Check-in date (ISO format)
   * @param checkOut - Check-out date (ISO format)
   * @returns Observable<ApiResponse<PromotionPreviewDTO>>
   */
  validatePromotionByCode(
    code: string, 
    propertyId: number,
    checkIn: string,
    checkOut: string
  ): Observable<ApiResponse<PromotionPreviewDTO>> {
    const url = `${this.apiUrl}/validate`;
    const params = new HttpParams()
      .set('code', code)
      .set('propertyId', propertyId.toString())
      .set('checkIn', checkIn)
      .set('checkOut', checkOut);
    
    console.log('=======================================');
    console.log('🎟️ PROMOTION SERVICE - Validating promotion by code');
    console.log('   Code:', code);
    console.log('   Property ID:', propertyId);
    console.log('   Check-in:', checkIn);
    console.log('   Check-out:', checkOut);
    console.log('   URL:', url);
    console.log('   Timestamp:', new Date().toISOString());
    console.log('=======================================');
    
    return this.http.get<ApiResponse<PromotionPreviewDTO>>(url, { params }).pipe(
      tap((response) => {
        console.log('✅ PROMOTION SERVICE - Validation response');
        console.log('   Response code:', response.code);
        console.log('   Message:', response.message);
        
        if (response.data) {
          console.log('   Valid:', response.data.valid);  // ✅ Changed from isValid to valid
          if (response.data.valid === true) {  // ✅ Changed from isValid to valid
            console.log('   Original Amount:', response.data.originalAmount);
            console.log('   Discount Amount:', response.data.discountAmount);
            console.log('   Final Amount:', response.data.finalAmount);
            console.log('   💰 You save:', response.data.discountAmount, 'VND');
          } else {
            console.log('   ❌ Error:', response.data.errorMessage);
          }
        }
        console.log('=======================================');
      }),
      catchError((error) => {
        console.error('❌ PROMOTION SERVICE - Validation error');
        console.error('   Status:', error.status);
        console.error('   Message:', error.error?.message || error.message);
        console.error('   Error:', error);
        console.error('=======================================');
        return throwError(() => error);
      })
    );
  }

  /**
   * OLD FLOW (Deprecated): Validate promotion for existing booking
   * GET /promotions/validate?code=X&bookingId=Y
   * 
   * ⚠️ This method is DEPRECATED in new flow (discount at booking creation)
   * Kept for backward compatibility only
   * 
   * @param code - Promotion code
   * @param bookingId - Booking ID to apply promotion to
   * @returns Observable<ApiResponse<PromotionPreviewDTO>>
   */
  validatePromotion(code: string, bookingId: number): Observable<ApiResponse<PromotionPreviewDTO>> {
    const url = `${this.apiUrl}/validate`;
    const params = new HttpParams()
      .set('code', code)
      .set('bookingId', bookingId.toString());
    
    console.log('=======================================');
    console.log('🎟️ PROMOTION SERVICE - Validating promotion (OLD FLOW)');
    console.log('   ⚠️ DEPRECATED: Use validatePromotionForBooking() instead');
    console.log('   Code:', code);
    console.log('   Booking ID:', bookingId);
    console.log('   URL:', url);
    console.log('   Timestamp:', new Date().toISOString());
    console.log('=======================================');
    
    return this.http.get<ApiResponse<PromotionPreviewDTO>>(url, { params }).pipe(
      tap((response) => {
        console.log('✅ PROMOTION SERVICE - Validation response');
        console.log('   Response code:', response.code);
        console.log('   Message:', response.message);
        
        if (response.data) {
          console.log('   Valid:', response.data.valid);  // ✅ Changed from isValid to valid
          if (response.data.valid === true) {  // ✅ Changed from isValid to valid
            console.log('   Original Amount:', response.data.originalAmount);
            console.log('   Discount:', response.data.discountAmount);
            console.log('   Final Amount:', response.data.finalAmount);
          } else {
            console.log('   Error:', response.data.errorMessage);
          }
        }
        console.log('=======================================');
      }),
      catchError((error) => {
        console.error('❌ PROMOTION SERVICE - Validation error');
        console.error('   Status:', error.status);
        console.error('   Message:', error.message);
        console.error('   Error:', error);
        console.error('=======================================');
        return throwError(() => error);
      })
    );
  }
}
