import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { Location } from '../models/location';

@Injectable({
  providedIn: 'root'
})
export class LocationService {
  private baseUrl = (environment.apiBaseUrl || 'http://localhost:8080').replace(/\/$/, '');

  constructor(private http: HttpClient) {}

  /**
   * Lấy tất cả locations
   * GET /locations
   */
  getAll(): Observable<Location[]> {
    console.log('🔵 API Call: GET /locations');
    return this.http.get<Location[]>(`${this.baseUrl}/locations`);
  }

  /**
   * Lấy locations theo city
   * GET /locations/city/{cityName}
   */
  getByCity(cityName: string): Observable<Location[]> {
    console.log(`🔵 API Call: GET /locations/city/${cityName}`);
    return this.http.get<Location[]>(`${this.baseUrl}/locations/city/${encodeURIComponent(cityName)}`);
  }

  // Legacy method for backward compatibility
  getAllLocations(): Observable<Location[]> {
    return this.getAll();
  }

  // Legacy method for backward compatibility
  getLocationsByCity(cityName: string): Observable<Location[]> {
    return this.getByCity(cityName);
  }
}
