import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';
import { RegisterDTO } from '../dtos/user/register.dto';
import { LoginDTO } from '../dtos/user/login.dto';
import { environment } from '../../environments/environment';
import { getBaseUrl } from '../utils/url.util';
import { HttpUtilService } from './http.util.service';
import { UserResponse } from '../responses/user/user.response';
import { UpdateUserDTO } from '../dtos/user/update.user.dto';

@Injectable({
  providedIn: 'root'
})
export class UserService {
  private baseUrl = getBaseUrl();
  private apiRegister = `${this.baseUrl}/auth/signup`;
  private apiLogin = `${this.baseUrl}/auth/login`;
  private apiRefresh = `${this.baseUrl}/auth/refresh`;
  private apiLogout = `${this.baseUrl}/auth/logout`;
  private apiUserProfile = `${this.baseUrl}/users/me`;
  private apiUserDetailedProfile = `${this.baseUrl}/users/me/details`;

  private apiConfig = {
    headers: this.httpUtilService.createHeaders(),
  }

  constructor(
    private http: HttpClient,
    private httpUtilService: HttpUtilService
  ) { }

  register(registerData: RegisterDTO | FormData): Observable<any> {
    console.log('🔵 API Call: POST /auth/signup');
    
    // If FormData, don't set Content-Type (browser will set it with boundary)
    const headers = registerData instanceof FormData 
      ? new HttpHeaders() // Let browser set Content-Type for multipart
      : this.httpUtilService.createHeaders();
    
    return this.http.post(this.apiRegister, registerData, {
      headers,
      withCredentials: true // Enable cookies for refresh token
    });
  }

  login(loginDTO: LoginDTO): Observable<any> {    
    return this.http.post(this.apiLogin, loginDTO, {
      ...this.apiConfig,
      withCredentials: true // Enable cookies for refresh token
    });
  }

  // Refresh access token using HttpOnly cookie
  refreshToken(): Observable<any> {
    return this.http.post(this.apiRefresh, {}, {
      headers: this.httpUtilService.createHeaders(),
      withCredentials: true // Required to send HttpOnly cookie
    });
  }

  // Logout - clears HttpOnly cookie on backend
  logout(): Observable<any> {
    return this.http.post(this.apiLogout, {}, {
      headers: this.httpUtilService.createHeaders(),
      withCredentials: true // Required to send HttpOnly cookie
    });
  }

  // Get basic user profile with statistics
  getMyProfile(token: string): Observable<any> {
    console.log('🔵 API Call: GET /users/me');
    return this.http.get(this.apiUserProfile, {
      headers: new HttpHeaders({
        'Content-Type': 'application/json',
        Authorization: `Bearer ${token}`
      })
    });
  }

  // Get detailed user profile with bookings, properties, etc.
  getMyDetailedProfile(token: string, includeDetails: boolean = true): Observable<any> {
    console.log(`🔵 API Call: GET ${this.apiUserDetailedProfile}`);
    return this.http.get(this.apiUserDetailedProfile, {
      headers: new HttpHeaders({
        'Content-Type': 'application/json',
        Authorization: `Bearer ${token}`
      })
    });
  }

  // Update user profile
  updateMyProfile(token: string, updateUserDTO: UpdateUserDTO): Observable<any> {
    console.log('🔵 API Call: PUT /users/me', updateUserDTO);
    return this.http.put(this.apiUserProfile, updateUserDTO, {
      headers: new HttpHeaders({
        'Content-Type': 'application/json',
        Authorization: `Bearer ${token}`
      })
    });
  }

  // Get user by ID (for viewing other users)
  getUserById(userId: number, token: string): Observable<any> {
    console.log(`🔵 API Call: GET /users/${userId}`);
    return this.http.get(`${this.baseUrl}/users/${userId}`, {
      headers: new HttpHeaders({
        'Content-Type': 'application/json',
        Authorization: `Bearer ${token}`
      })
    });
  }
  saveUserResponseToLocalStorage(userResponse?: UserResponse) {
    try {
      if(userResponse == null || !userResponse) {
        console.log('💾 No user response to save');
        return;
      }
      // Convert the userResponse object to a JSON string
      const userResponseJSON = JSON.stringify(userResponse);  
      // Save the JSON string to local storage with a key (e.g., "userResponse")
      localStorage.setItem('user', userResponseJSON);  
      console.log('💾 User response saved to localStorage:', {
        id: userResponse.id,
        fullname: userResponse.fullname,
        email: userResponse.email,
        role: typeof userResponse.role === 'string' 
          ? userResponse.role 
          : userResponse.role?.name
      });
    } catch (error) {
      console.error('❌ Error saving user response to localStorage:', error);
    }
  }
  getUserResponseFromLocalStorage():UserResponse | null {
    try {
      // Retrieve the JSON string from local storage using the key
      const userResponseJSON = localStorage.getItem('user'); 
      if(userResponseJSON == null || userResponseJSON == undefined) {
        console.log('💾 No user data found in localStorage');
        return null;
      }
      // Parse the JSON string back to an object
      const userResponse = JSON.parse(userResponseJSON!);  
      console.log('💾 User response retrieved from localStorage:', {
        id: userResponse.id,
        fullname: userResponse.fullname,
        email: userResponse.email,
        role: userResponse.role?.name
      });
      return userResponse;
    } catch (error) {
      console.error('❌ Error retrieving user response from localStorage:', error);
      return null; // Return null or handle the error as needed
    }
  }
  removeUserFromLocalStorage():void {
    try {
      // Remove the user data from local storage using the key
      localStorage.removeItem('user');
      console.log('💾 User data removed from localStorage');
    } catch (error) {
      console.error('❌ Error removing user data from localStorage:', error);
      // Handle the error as needed
    }
  }

  // ============== FAVORITES API ==============
  
  /**
   * Add property to user's favorites
   * POST /user/favorites/{userId}/property/{propertyId}
   */
  addToFavorites(userId: number, propertyId: number, token: string): Observable<any> {
    const url = `${this.baseUrl}/user/favorites/${userId}/property/${propertyId}`;
    console.log(`🔵 API Call: POST ${url}`);
    
    const headers = new HttpHeaders({
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${token}`
    });
    
    return this.http.post(url, {}, { headers, withCredentials: true });
  }

  /**
   * Remove property from user's favorites
   * DELETE /user/favorites/{userId}/property/{propertyId}
   */
  removeFromFavorites(userId: number, propertyId: number, token: string): Observable<any> {
    const url = `${this.baseUrl}/user/favorites/${userId}/property/${propertyId}`;
    console.log(`🔵 API Call: DELETE ${url}`);
    
    const headers = new HttpHeaders({
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${token}`
    });
    
    return this.http.delete(url, { headers, withCredentials: true });
  }

  /**
   * Check if property is in user's favorites
   * GET /user/favorites/{userId}/property/{propertyId}/check
   */
  checkFavorite(userId: number, propertyId: number, token: string): Observable<any> {
    const url = `${this.baseUrl}/user/favorites/${userId}/property/${propertyId}/check`;
    console.log(`🔵 API Call: GET ${url}`);
    
    const headers = new HttpHeaders({
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${token}`
    });
    
    return this.http.get(url, { headers, withCredentials: true });
  }

  /**
   * Get all available favorite properties for user
   * GET /user/favorites/{userId}/available
   */
  getFavoriteProperties(userId: number, token: string): Observable<any> {
    const url = `${this.baseUrl}/user/favorites/${userId}/available`;
    console.log(`🔵 API Call: GET ${url}`);
    
    const headers = new HttpHeaders({
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${token}`
    });
    
    return this.http.get(url, { headers, withCredentials: true });
  }
  
}
