import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { RegisterDTO } from '../dtos/user/register.dto';
import { LoginDTO } from '../dtos/user/login.dto';
import { environment } from '../../environments/environment';
import { getBaseUrl } from '../utils/url.util';
import { HttpUtilService } from './http.util.service';
import { TokenService } from './token.service';
import { DeviceService } from './device.service';
import { UserResponse } from '../responses/user/user.response';
import { UpdateUserDTO } from '../dtos/user/update.user.dto';
import { BaseResponse, PageResponse } from '../models/property';
import { Property } from '../models/property';

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
    private httpUtilService: HttpUtilService,
    private tokenService: TokenService,
    private deviceService: DeviceService
  ) { }

  register(registerData: RegisterDTO | FormData): Observable<any> {
    console.log('=======================================');
    console.log('🔵 API Call: POST /auth/signup');
    console.log('   Data Type:', registerData instanceof FormData ? 'FormData' : 'RegisterDTO');
    console.log('   Timestamp:', new Date().toISOString());
    console.log('=======================================');
    
    // If FormData, don't set Content-Type (browser will set it with boundary)
    const headers = registerData instanceof FormData 
      ? new HttpHeaders() // Let browser set Content-Type for multipart
      : this.httpUtilService.createHeaders();
    
    return this.http.post(this.apiRegister, registerData, {
      headers
      // NOTE: withCredentials NOT needed - user doesn't have RT cookie yet
    });
  }

  login(loginDTO: LoginDTO): Observable<any> {
    console.log('=======================================');
    console.log('🔵 API Call: POST /auth/login');
    console.log('   Username/Email:', loginDTO.usernameOrEmail);
    console.log('   Remember Me:', loginDTO.rememberMe);
    console.log('   Timestamp:', new Date().toISOString());
    console.log('=======================================');
    
    return this.http.post(this.apiLogin, loginDTO, {
      ...this.apiConfig,
      withCredentials: true // Enable cookies for refresh token
    });
  }

  // Refresh access token using HttpOnly cookie
  refreshToken(): Observable<any> {
    console.log('=======================================');
    console.log('🔵 API Call: POST /auth/refresh');
    console.log('   Purpose: Get new Access Token');
    console.log('   With Credentials: true (sends HttpOnly cookie)');
    console.log('   Timestamp:', new Date().toISOString());
    console.log('=======================================');
    
    return this.http.post(this.apiRefresh, {}, {
      headers: this.httpUtilService.createHeaders(),
      withCredentials: true // Required to send HttpOnly cookie
    });
  }

  // Logout - clears HttpOnly cookie on backend
  // NOTE: Must send Authorization header and X-Device-Id (like auth/refresh)
  logout(): Observable<any> {
    console.log('=======================================');
    console.log('🚪 UserService.logout() CALLED');
    console.log('   Timestamp:', new Date().toISOString());
    console.log('   Stack trace:', new Error().stack);
    console.log('=======================================');
    
    // Get token for Authorization header
    const token = this.tokenService.getToken();
    
    // Get device_id from localStorage directly (không dùng getDeviceId() để tránh auto-generate)
    // DeviceIdInterceptor sẽ tự động thêm X-Device-Id nếu có trong LS
    // Nhưng chúng ta cũng thêm ở đây để đảm bảo
    const deviceIdFromLS = localStorage.getItem('device_id');
    
    // Build headers with Authorization and X-Device-Id
    const headers: { [key: string]: string } = {
      'Content-Type': 'application/json'
    };
    
    // Add Authorization header if token exists
    if (token) {
      headers['Authorization'] = `Bearer ${token}`;
      console.log('   Authorization header: Added');
    } else {
      console.warn('   ⚠️ Authorization header: No token found');
    }
    
    // Add X-Device-Id header if exists in localStorage
    // NOTE: Không tự động tạo device_id mới khi logout
    // Nếu không có device_id, backend vẫn có thể xử lý logout dựa trên Authorization token và RT cookie
    if (deviceIdFromLS) {
      // Validate format before sending
      const isValidUUID = /^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i.test(deviceIdFromLS);
      if (isValidUUID) {
        headers['X-Device-Id'] = deviceIdFromLS;
        console.log('   X-Device-Id header: Added', deviceIdFromLS.substring(0, 8) + '...');
      } else {
        console.warn('   ⚠️ X-Device-Id header: Invalid format, skipping');
      }
    } else {
      console.warn('   ⚠️ X-Device-Id header: Not found in localStorage (backend may still process logout via Authorization token)');
    }
    
    console.log('=======================================');
    
    return this.http.post(this.apiLogout, {}, {
      headers: new HttpHeaders(headers),
      withCredentials: true // REQUIRED: Send RT cookie to backend for deletion
    });
  }

  /**
   * Get basic user profile (id, username, role)
   * 
   * @deprecated Use tokenService.getUserInfo() instead to decode JWT.
   * This eliminates unnecessary API call and improves performance by 200-500ms.
   * 
   * Only use getMyDetailedProfile() if you need full profile with statistics.
   */
  getMyProfile(): Observable<any> {
    console.log('=======================================');
    console.log('⚠️ DEPRECATED: UserService.getMyProfile() CALLED');
    console.log('   → Consider using tokenService.getUserInfo() instead');
    console.log('   Method: GET');
    console.log('   Endpoint:', this.apiUserProfile);
    console.log('   Authorization: Handled by TokenInterceptor');
    console.log('   Timestamp:', new Date().toISOString());
    console.log('=======================================');
    // Interceptor will automatically add Authorization header
    return this.http.get(this.apiUserProfile);
  }

  // Get detailed user profile with bookings, properties, etc.
  getMyDetailedProfile(includeDetails: boolean = true): Observable<any> {
    console.log('=======================================');
    console.log(`🔵 API Call: GET ${this.apiUserDetailedProfile}`);
    console.log('   Authorization: Handled by TokenInterceptor');
    console.log('   Timestamp:', new Date().toISOString());
    console.log('=======================================');
    // Interceptor will automatically add Authorization header
    return this.http.get(this.apiUserDetailedProfile);
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
      console.log('=======================================');
      console.log('🗑️ UserService.removeUserFromLocalStorage() CALLED');
      console.log('   Timestamp:', new Date().toISOString());
      console.log('   Stack trace:', new Error().stack);
      console.log('=======================================');
      // Remove the user data from local storage using the key
      localStorage.removeItem('user');
      console.log('✅ User data removed from localStorage');
    } catch (error) {
      console.error('❌ Error removing user data from localStorage:', error);
      // Handle the error as needed
    }
  }

  // ============== FAVORITES API ==============
  
  /**
   * Add property to user's favorites
   * POST /user/favorites/{userId}/property/{propertyId}
   * Authorization header added by TokenInterceptor
   */
  addToFavorites(userId: number, propertyId: number): Observable<any> {
    const url = `${this.baseUrl}/user/favorites/${userId}/property/${propertyId}`;
    console.log('=======================================');
    console.log(`🔵 API Call: POST ${url}`);
    console.log('   Authorization: Handled by TokenInterceptor');
    console.log('=======================================');
    
    // TokenInterceptor will add Authorization header automatically
    return this.http.post(url, {});
  }

  /**
   * Remove property from user's favorites
   * DELETE /user/favorites/{userId}/property/{propertyId}
   * Authorization header added by TokenInterceptor
   */
  removeFromFavorites(userId: number, propertyId: number): Observable<any> {
    const url = `${this.baseUrl}/user/favorites/${userId}/property/${propertyId}`;
    console.log('=======================================');
    console.log(`🔵 API Call: DELETE ${url}`);
    console.log('   Authorization: Handled by TokenInterceptor');
    console.log('=======================================');
    
    // TokenInterceptor will add Authorization header automatically
    return this.http.delete(url);
  }

  /**
   * Check if property is in user's favorites
   * GET /user/favorites/{userId}/property/{propertyId}/check
   * Authorization header added by TokenInterceptor
   */
  checkFavorite(userId: number, propertyId: number): Observable<any> {
    const url = `${this.baseUrl}/user/favorites/${userId}/property/${propertyId}/check`;
    console.log(`🔵 API Call: GET ${url}`);
    console.log('   Authorization: Handled by TokenInterceptor');
    
    // TokenInterceptor will add Authorization header automatically
    return this.http.get(url);
  }

  /**
   * Get all available favorite properties for user (with pagination)
   * GET /user/favorites/{userId}/available
   * Authorization header added by TokenInterceptor
   * @param userId User ID
   * @param page Page number (0-based, default: 0)
   * @param size Page size (default: 9)
   * @param sortBy Sort field (default: id)
   * @param sortDirection Sort direction (default: DESC)
   */
  getFavoriteProperties(
    userId: number,
    page: number = 0,
    size: number = 9,
    sortBy: string = 'id',
    sortDirection: string = 'DESC'
  ): Observable<BaseResponse<PageResponse<Property>>> {
    const url = `${this.baseUrl}/user/favorites/${userId}/available`;
    
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString())
      .set('sortBy', sortBy)
      .set('sortDirection', sortDirection);
    
    console.log(`🔵 API Call: GET ${url}`);
    console.log(`   📋 Params:`, { page, size, sortBy, sortDirection });
    console.log('   Authorization: Handled by TokenInterceptor');
    
    // TokenInterceptor will add Authorization header automatically
    return this.http.get<BaseResponse<PageResponse<Property>>>(url, { params });
  }
  
}
