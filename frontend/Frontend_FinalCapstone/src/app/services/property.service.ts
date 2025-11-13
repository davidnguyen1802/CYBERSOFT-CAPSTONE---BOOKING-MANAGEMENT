import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { environment } from '../../environments/environment';
import { 
  Property, 
  PropertyListItem, 
  PageResponse, 
  BaseResponse, 
  PropertySearchRequest 
} from '../models/property';
import { PropertyRequestDTO } from '../dtos/property/property.request.dto';
import { PropertyCompleteRequestDTO } from '../dtos/property/property-complete.request.dto';

@Injectable({ providedIn: 'root' })
export class PropertyService {
  // Ensure no trailing slash so concatenations like `${baseUrl}/path` are correct
  private baseUrl = (environment.apiBaseUrl || 'http://localhost:8080').replace(/\/$/, '');

  constructor(private http: HttpClient) {}

  /**
   * Lấy top 7 properties nổi bật
   * GET /property/top7
   */
  getTop7Properties(): Observable<BaseResponse<Property[]>> {
    console.log('🔵 API Call: GET /property/top7');
    return this.http.get<BaseResponse<Property[]>>(`${this.baseUrl}/property/top7`);
  }

  /**
   * Lấy top 4 properties theo loại
   * GET /property/top4/type/{type} (Still active endpoint)
   * @param propertyType 0: Apartment, 1: House, 2: Hotel
   */
  getTop4ByType(propertyType: number): Observable<BaseResponse<Property[]>> {
    console.log(`🔵 API Call: GET /property/top4/type/${propertyType}`);
    return this.http.get<BaseResponse<Property[]>>(`${this.baseUrl}/property/top4/type/${propertyType}`);
  }

  /**
   * Lấy tất cả properties theo loại (Updated to use new API with pagination)
   * GET /property/filter?type={type}
   * @param propertyType 0: Apartment, 1: House, 2: Hotel
   * @returns Observable with only content array (for backward compatibility)
   */
  getPropertiesByType(propertyType: number, page: number = 0, size: number = 20): Observable<BaseResponse<PropertyListItem[]>> {
    console.log(`🔵 API Call: GET /property/filter?type=${propertyType}&page=${page}&size=${size}`);
    const params = new HttpParams()
      .set('type', propertyType.toString())
      .set('page', page.toString())
      .set('size', size.toString())
      .set('sortBy', 'priority')
      .set('sortDirection', 'DESC');
    return this.http.get<BaseResponse<PageResponse<PropertyListItem>>>(`${this.baseUrl}/property/filter`, { params })
      .pipe(map(response => ({
        ...response,
        data: response.data.content
      })));
  }

  /**
   * Lấy properties theo loại với full pagination info
   * GET /property/filter?type={type}
   * @param propertyType 0: Apartment, 1: House, 2: Hotel
   * @returns Observable with full PageResponse
   */
  getPropertiesByTypeWithPagination(propertyType: number, page: number = 0, size: number = 20): Observable<BaseResponse<PageResponse<PropertyListItem>>> {
    console.log(`🔵 API Call: GET /property/filter?type=${propertyType}&page=${page}&size=${size} (with pagination)`);
    const params = new HttpParams()
      .set('type', propertyType.toString())
      .set('page', page.toString())
      .set('size', size.toString())
      .set('sortBy', 'priority')
      .set('sortDirection', 'DESC');
    return this.http.get<BaseResponse<PageResponse<PropertyListItem>>>(`${this.baseUrl}/property/filter`, { params });
  }

  /**
   * Lấy tất cả properties không theo loại
   * GET /property/filter (without type filter) with pagination
   * @returns Observable with only content array (for backward compatibility)
   */
  getAllProperties(page: number = 0, size: number = 20): Observable<BaseResponse<PropertyListItem[]>> {
    console.log(`🔵 API Call: GET /property/filter?page=${page}&size=${size} (All properties)`);
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString())
      .set('sortBy', 'priority')
      .set('sortDirection', 'DESC');
    return this.http.get<BaseResponse<PageResponse<PropertyListItem>>>(`${this.baseUrl}/property/filter`, { params })
      .pipe(map(response => ({
        ...response,
        data: response.data.content
      })));
  }

  /**
   * Lấy tất cả properties với full pagination info
   * GET /property/filter
   * @returns Observable with full PageResponse
   */
  getAllPropertiesWithPagination(page: number = 0, size: number = 20): Observable<BaseResponse<PageResponse<PropertyListItem>>> {
    console.log(`🔵 API Call: GET /property/filter?page=${page}&size=${size} (All properties with pagination)`);
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString())
      .set('sortBy', 'priority')
      .set('sortDirection', 'DESC');
    return this.http.get<BaseResponse<PageResponse<PropertyListItem>>>(`${this.baseUrl}/property/filter`, { params });
  }

  /**
   * Tìm kiếm property suggestions theo tên (debounced search)
   * GET /property/filter?name={searchTerm}&size=5
   */
  searchPropertySuggestions(searchTerm: string): Observable<PropertyListItem[]> {
    if (!searchTerm.trim()) {
      return new Observable(observer => {
        observer.next([]);
        observer.complete();
      });
    }

    console.log(`🔵 API Call: GET /property/filter?name=${searchTerm}&size=5 (Search suggestions)`);
    const params = new HttpParams()
      .set('name', searchTerm.trim())
      .set('size', '5')
      .set('page', '0');
    
    return this.http.get<BaseResponse<PageResponse<PropertyListItem>>>(`${this.baseUrl}/property/filter`, { params })
      .pipe(map(response => response.data.content || []));
  }

  /**
   * Lấy chi tiết property
   * GET /property/{id}
   */
  getPropertyDetail(id: number): Observable<BaseResponse<Property>> {
    console.log(`🔵 API Call: GET /property/${id}`);
    return this.http.get<BaseResponse<Property>>(`${this.baseUrl}/property/${id}`);
  }

  /**
   * Lấy reviews của property
   * GET /property/{id}/reviews
   */
  getPropertyReviews(propertyId: number): Observable<BaseResponse<any[]>> {
    console.log(`🔵 API Call: GET /property/${propertyId}/reviews`);
    return this.http.get<BaseResponse<any[]>>(`${this.baseUrl}/property/${propertyId}/reviews`);
  }

  /**
   * Lấy properties theo type và city (Updated to use new API with pagination)
   * GET /property/filter?type={type}&city={cityName}&page={page}&size={size}
   * @param type Property type (0: Apartment, 1: House, 2: Hotel)
   * @param cityName Tên thành phố
   * @param page Page number (default: 0)
   * @param size Page size (default: 20)
   */
  getByTypeAndCity(type: string, cityName: string, page: number = 0, size: number = 20): Observable<BaseResponse<PropertyListItem[]>> {
    console.log(`🔵 API Call: GET /property/filter?type=${type}&city=${cityName}&page=${page}&size=${size}`);
    let params = new HttpParams()
      .set('type', type)
      .set('city', cityName)
      .set('page', page.toString())
      .set('size', size.toString())
      .set('sortBy', 'priority')
      .set('sortDirection', 'DESC');
    
    return this.http.get<BaseResponse<PageResponse<PropertyListItem>>>(`${this.baseUrl}/property/filter`, { params })
      .pipe(map(response => ({
        ...response,
        data: response.data.content
      })));
  }

  /**
   * Lấy properties theo type và location (Updated to use new API with pagination)  
   * GET /property/filter?type={type}&location={locationName}&page={page}&size={size}
   * @param type Property type (0: Apartment, 1: House, 2: Hotel)
   * @param locationName Tên địa điểm
   * @param page Page number (default: 0)
   * @param size Page size (default: 20)
   */
  getByTypeAndLocation(type: string, locationName: string, page: number = 0, size: number = 20): Observable<BaseResponse<PropertyListItem[]>> {
    console.log(`🔵 API Call: GET /property/filter?type=${type}&location=${locationName}&page=${page}&size=${size}`);
    let params = new HttpParams()
      .set('type', type)
      .set('location', locationName)
      .set('page', page.toString())
      .set('size', size.toString())
      .set('sortBy', 'priority')
      .set('sortDirection', 'DESC');
      
    return this.http.get<BaseResponse<PageResponse<PropertyListItem>>>(`${this.baseUrl}/property/filter`, { params })
      .pipe(map(response => ({
        ...response,
        data: response.data.content
      })));
  }

  // ================ NEW SEARCH & FILTER METHODS ================

  /**
   * Search properties using GET with query parameters.
   * Recommended for most use cases - supports shareable URLs.
   */
  searchPropertiesGet(filters: PropertySearchRequest): Observable<PageResponse<PropertyListItem>> {
    let params = this.buildHttpParams(filters);
    console.log(`🔵 API Call: GET /property/filter with filters:`, filters);
    
    return this.http.get<BaseResponse<PageResponse<PropertyListItem>>>(`${this.baseUrl}/property/filter`, { params })
      .pipe(map(response => response.data));
  }

  /**
   * Search properties using POST with request body.
   * Use for complex filter combinations or when URL length is a concern.
   */
  searchPropertiesPost(filters: PropertySearchRequest): Observable<PageResponse<PropertyListItem>> {
    console.log(`🔵 API Call: POST /property/search with filters:`, filters);
    return this.http.post<BaseResponse<PageResponse<PropertyListItem>>>(`${this.baseUrl}/property/search`, filters)
      .pipe(map(response => response.data));
  }

  /**
   * Helper: Build HttpParams from filter object.
   * Only includes non-null/non-undefined values to avoid polluting URL.
   */
  private buildHttpParams(filters: PropertySearchRequest): HttpParams {
    let params = new HttpParams();

    // Simple scalar parameters
    if (filters.type !== undefined && filters.type !== null) {
      params = params.append('type', filters.type.toString());
    }
    if (filters.city) {
      params = params.append('city', filters.city);
    }
    if (filters.location) {
      params = params.append('location', filters.location);
    }
    if (filters.minPrice !== undefined && filters.minPrice !== null) {
      params = params.append('minPrice', filters.minPrice.toString());
    }
    if (filters.maxPrice !== undefined && filters.maxPrice !== null) {
      params = params.append('maxPrice', filters.maxPrice.toString());
    }
    if (filters.bedrooms !== undefined && filters.bedrooms !== null) {
      params = params.append('bedrooms', filters.bedrooms.toString());
    }
    if (filters.bathrooms !== undefined && filters.bathrooms !== null) {
      params = params.append('bathrooms', filters.bathrooms.toString());
    }
    if (filters.maxAdults !== undefined && filters.maxAdults !== null) {
      params = params.append('maxAdults', filters.maxAdults.toString());
    }
    if (filters.maxChildren !== undefined && filters.maxChildren !== null) {
      params = params.append('maxChildren', filters.maxChildren.toString());
    }
    if (filters.maxInfants !== undefined && filters.maxInfants !== null) {
      params = params.append('maxInfants', filters.maxInfants.toString());
    }
    if (filters.maxPets !== undefined && filters.maxPets !== null) {
      params = params.append('maxPets', filters.maxPets.toString());
    }
    if (filters.name) {
      params = params.append('name', filters.name);
    }

    // Array parameters (repeat key for each value)
    if (filters.amenities && filters.amenities.length > 0) {
      filters.amenities.forEach(id => {
        params = params.append('amenities', id.toString());
      });
    }
    if (filters.facilities && filters.facilities.length > 0) {
      filters.facilities.forEach(id => {
        params = params.append('facilities', id.toString());
      });
    }

    // Pagination & sorting
    if (filters.page !== undefined && filters.page !== null) {
      params = params.append('page', filters.page.toString());
    }
    if (filters.size !== undefined && filters.size !== null) {
      params = params.append('size', filters.size.toString());
    }
    if (filters.sortBy) {
      params = params.append('sortBy', filters.sortBy);
    }
    if (filters.sortDirection) {
      params = params.append('sortDirection', filters.sortDirection);
    }

    return params;
  }

  /**
   * Create a new property (OLD METHOD - still works)
   * POST /property
   */
  createProperty(propertyRequest: PropertyRequestDTO): Observable<BaseResponse<any>> {
    console.log('🔵 API Call: POST /property', propertyRequest);
    return this.http.post<BaseResponse<any>>(`${this.baseUrl}/property`, propertyRequest);
  }

  /**
   * Create complete property in one request (NEW METHOD - RECOMMENDED)
   * POST /property/complete
   * Includes: property info + images + amenities + facilities
   */
  createCompleteProperty(request: PropertyCompleteRequestDTO): Observable<BaseResponse<any>> {
    console.log('🔵 API CALL: POST /property/complete');
    console.log('📝 Property data:', request.property);
    console.log('🖼️  Images count:', request.images?.length || 0);
    console.log('📋 Image descriptions:', request.imageDescriptions);
    console.log('✨ Amenities:', request.amenityIds);
    console.log('🏢 Facilities:', request.facilityIds);

    const formData = new FormData();
    
    // Add property data as JSON Blob
    formData.append('property', new Blob(
      [JSON.stringify(request.property)], 
      { type: 'application/json' }
    ));
    console.log('✅ Added property JSON to FormData');
    
    // Add images (if provided)
    if (request.images && request.images.length > 0) {
      request.images.forEach((image, index) => {
        formData.append('images', image);
        console.log(`✅ Added image ${index + 1}: ${image.name} (${(image.size / 1024).toFixed(2)} KB)`);
      });
    }
    
    // Add image descriptions (if provided)
    if (request.imageDescriptions && request.imageDescriptions.length > 0) {
      formData.append('imageDescriptions', new Blob(
        [JSON.stringify(request.imageDescriptions)],
        { type: 'application/json' }
      ));
      console.log('✅ Added image descriptions to FormData');
    }
    
    // Add amenity IDs (if provided)
    if (request.amenityIds && request.amenityIds.length > 0) {
      formData.append('amenityIds', new Blob(
        [JSON.stringify(request.amenityIds)],
        { type: 'application/json' }
      ));
      console.log('✅ Added amenity IDs to FormData');
    }
    
    // Add facility IDs (if provided)
    if (request.facilityIds && request.facilityIds.length > 0) {
      formData.append('facilityIds', new Blob(
        [JSON.stringify(request.facilityIds)],
        { type: 'application/json' }
      ));
      console.log('✅ Added facility IDs to FormData');
    }
    
    console.log('🚀 Sending FormData to API...');
    return this.http.post<BaseResponse<any>>(`${this.baseUrl}/property/complete`, formData);
  }

    /**
   * Get all properties by host ID (with pagination and sorting)
   * GET /property/host/{hostId}
   * @param hostId ID của host
   * @param page Page number (default: 0)
   * @param size Page size (default: 9)
   * @param name Optional search query for property name
   * @param sortBy Optional sort field (default: id)
   * @param sortDirection Optional sort direction (default: DESC)
   */
  getPropertiesByHost(
    hostId: number,
    page: number = 0,
    size: number = 9,
    name?: string,
    sortBy: string = 'id',
    sortDirection: string = 'DESC'
  ): Observable<BaseResponse<PageResponse<Property>>> {
    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString())
      .set('sortBy', sortBy)
      .set('sortDirection', sortDirection);
    
    if (name && name.trim()) {
      params = params.set('name', name.trim());
    }
    
    const fullUrl = `${this.baseUrl}/property/host/${hostId}`;
    console.log(`🔵 API Call: GET ${fullUrl}`);
    console.log(`   📋 Params:`, { page, size, sortBy, sortDirection, name: name || 'none' });
    
    return this.http.get<BaseResponse<PageResponse<Property>>>(fullUrl, { params });
  }

  /**
   * Delete a property by ID
   * DELETE /property/{id}
   * @param propertyId ID của property cần xóa
   */
  deleteProperty(propertyId: number): Observable<BaseResponse<any>> {
    console.log(`🔵 API Call: DELETE /property/${propertyId}`);
    return this.http.delete<BaseResponse<any>>(`${this.baseUrl}/property/${propertyId}`);
  }
}
