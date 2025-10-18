import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { City } from '../models/city';

@Injectable({
  providedIn: 'root'
})
export class CityService {
  private baseUrl = (environment.apiBaseUrl || 'http://localhost:8080').replace(/\/$/, '');

  constructor(private http: HttpClient) {}

  /**
   * Lấy danh sách tất cả cities
   * GET /cities
   */
  getAll(): Observable<City[]> {
    console.log('🔵 API Call: GET /cities');
    return this.http.get<City[]>(`${this.baseUrl}/cities`);
  }
}
