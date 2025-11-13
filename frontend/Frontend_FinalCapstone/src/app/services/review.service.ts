import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { tap, catchError } from 'rxjs/operators';
import { throwError } from 'rxjs';
import { environment } from '../../environments/environment';
import { ReviewResponse } from '../responses/review/review.response';
import { UpdateReviewDTO } from '../dtos/review/update.review.dto';

@Injectable({
  providedIn: 'root'
})
export class ReviewService {
  // Ensure no trailing slash so concatenations like `${baseUrl}/path` are correct
  private baseUrl = (environment.apiBaseUrl || 'http://localhost:8080').replace(/\/$/, '');
  private apiUrl = `${this.baseUrl}/reviews`;

  constructor(private http: HttpClient) {}

  /**
   * Get all reviews for a specific user
   * @param userId - The ID of the user
   * @returns Observable<ReviewResponse>
   */
  getUserReviews(userId: number): Observable<ReviewResponse> {
    const url = `${this.apiUrl}/user/${userId}`;
    console.log('=======================================');
    console.log('⭐ REVIEW SERVICE - Getting user reviews');
    console.log('   User ID:', userId);
    console.log('   URL:', url);
    console.log('   Timestamp:', new Date().toISOString());
    console.log('=======================================');
    
    return this.http.get<ReviewResponse>(url).pipe(
      tap((response) => {
        console.log('✅ REVIEW SERVICE - Success');
        console.log('   Response code:', response.code);
        console.log('   Message:', response.message);
        const reviews = Array.isArray(response.data) ? response.data : [];
        console.log('   Total reviews:', reviews.length);
        console.log('=======================================');
      }),
      catchError((error) => {
        console.error('❌ REVIEW SERVICE - Error');
        console.error('   Status:', error.status);
        console.error('   Message:', error.message);
        console.error('   Error:', error);
        console.error('=======================================');
        return throwError(() => error);
      })
    );
  }

  /**
   * Update a review
   * @param reviewId - The ID of the review
   * @param updateData - The review data to update
   * @returns Observable<ReviewResponse>
   */
  updateReview(reviewId: number, updateData: UpdateReviewDTO): Observable<ReviewResponse> {
    const url = `${this.apiUrl}/${reviewId}`;
    console.log('=======================================');
    console.log('✏️ REVIEW SERVICE - Updating review');
    console.log('   Review ID:', reviewId);
    console.log('   URL:', url);
    console.log('   Update data:', updateData);
    console.log('   Timestamp:', new Date().toISOString());
    console.log('=======================================');
    
    return this.http.put<ReviewResponse>(url, updateData).pipe(
      tap((response) => {
        console.log('✅ REVIEW SERVICE - Updated successfully');
        console.log('   Response:', response);
        console.log('=======================================');
      }),
      catchError((error) => {
        console.error('❌ REVIEW SERVICE - Update error');
        console.error('   Status:', error.status);
        console.error('   Message:', error.message);
        console.error('   Error:', error);
        console.error('=======================================');
        return throwError(() => error);
      })
    );
  }

  /**
   * Delete a review
   * @param reviewId - The ID of the review
   * @param userId - The ID of the user
   * @returns Observable<any>
   */
  deleteReview(reviewId: number, userId: number): Observable<any> {
    const url = `${this.apiUrl}/${reviewId}/user/${userId}`;
    console.log('=======================================');
    console.log('🗑️ REVIEW SERVICE - Deleting review');
    console.log('   Review ID:', reviewId);
    console.log('   User ID:', userId);
    console.log('   URL:', url);
    console.log('   Timestamp:', new Date().toISOString());
    console.log('=======================================');
    
    return this.http.delete<any>(url).pipe(
      tap((response) => {
        console.log('✅ REVIEW SERVICE - Deleted successfully');
        console.log('   Response:', response);
        console.log('=======================================');
      }),
      catchError((error) => {
        console.error('❌ REVIEW SERVICE - Delete error');
        console.error('   Status:', error.status);
        console.error('   Message:', error.message);
        console.error('   Error:', error);
        console.error('=======================================');
        return throwError(() => error);
      })
    );
  }
}
