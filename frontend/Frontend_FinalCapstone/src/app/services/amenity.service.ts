import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from 'src/environments/environment';
import { Amenity, AmenityRequest } from '../models/amenity';

@Injectable({
  providedIn: 'root'
})
export class AmenityService {
  private apiUrl = `${environment.apiBaseUrl}/amenities`;

  constructor(private http: HttpClient) {}

  private getHeaders(): HttpHeaders {
    const token = localStorage.getItem('access_token');
    return new HttpHeaders({
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${token}`
    });
  }

  // GET /amenities - Lấy tất cả amenities
  getAllAmenities(): Observable<any> {
    return this.http.get<any>(this.apiUrl, {
      headers: this.getHeaders()
    });
  }

  // POST /amenities/property - Thêm amenities vào property
  addAmenitiesToProperty(request: AmenityRequest): Observable<any> {
    return this.http.post<any>(`${this.apiUrl}/property`, request, {
      headers: this.getHeaders()
    });
  }

  // Helper: Tạo full URL cho icon
  getIconUrl(iconUrl: string): string {
    if (!iconUrl) return '';
    if (iconUrl.startsWith('http')) return iconUrl;
    return `${environment.apiBaseUrl}${iconUrl}`;
  }
}

