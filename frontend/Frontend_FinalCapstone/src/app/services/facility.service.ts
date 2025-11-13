import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { Facility, FacilityRequest } from '../models/facility';

@Injectable({
  providedIn: 'root'
})
export class FacilityService {
  private apiUrl = `${environment.apiBaseUrl}/facilities`;

  constructor(private http: HttpClient) { }

  getAllFacilities(): Observable<any> {
    return this.http.get<any>(this.apiUrl);
  }

  addFacilitiesToProperty(request: FacilityRequest): Observable<any> {
    const token = localStorage.getItem('access_token');
    const headers = new HttpHeaders({
      'Authorization': `Bearer ${token}`
    });
    
    return this.http.post<any>(`${this.apiUrl}/property`, request, { headers });
  }

  getIconUrl(iconUrl: string): string {
    if (!iconUrl) {
      return 'assets/default-facility-icon.png';
    }
    if (iconUrl.startsWith('http://') || iconUrl.startsWith('https://')) {
      return iconUrl;
    }
    return `${environment.apiBaseUrl}${iconUrl}`;
  }
}

