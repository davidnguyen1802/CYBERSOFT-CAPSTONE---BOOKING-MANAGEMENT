import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { getBaseUrl } from '../utils/url.util';
@Injectable({
  providedIn: 'root'
})
export class RoleService {
  private apiGetRoles  = `${getBaseUrl()}/roles`;

  constructor(private http: HttpClient) { }
  getRoles():Observable<any> {
    console.log('🔵 API Call: GET /roles');
    return this.http.get<any[]>(this.apiGetRoles);
  }
}
