import { Component, OnInit, OnDestroy } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';

import { Subject } from 'rxjs';

import { Property, PropertyListItem, PropertySearchRequest, SortDirection } from '../../models/property';
import { PropertyService } from '../../services/property.service';
import { UserService } from '../../services/user.service';
import { TokenService } from '../../services/token.service';
import { AuthStateService } from '../../services/auth-state.service';
import { LocationService } from '../../services/location.service';
import { CityService } from '../../services/city.service';
import { Location } from '../../models/location';
import { City } from '../../models/city';

@Component({
  selector: 'app-property-list',
  templateUrl: './property-list.component.html',
  styleUrls: ['./property-list.component.scss']
})
export class PropertyListComponent implements OnInit, OnDestroy {
  // Core data
  properties: Property[] = [];
  propertyType!: string; // Lưu dạng string để dùng trong URL
  loading: boolean = false;
  errorMsg: string = '';
  
  // Wishlist state
  isFavoriteMap: Map<number, boolean> = new Map();
  isLoadingWishlistMap: Map<number, boolean> = new Map();
  isLoggedIn: boolean = false;
  userId: number = 0;
  
  // Convert PropertyType enum to number for legacy compatibility
  private getPropertyTypeNumber(propertyType: any): number {
    if (typeof propertyType === 'number') return propertyType;
    
    switch (propertyType) {
      case 'APARTMENT': return 0;
      case 'HOUSE': return 1;
      case 'VILLA': return 2;
      default: return 0; // Default to apartment
    }
  }

  // Get property type label for display
  getPropertyTypeLabel(propertyType: number): string {
    return this.propertyTypeLabels[propertyType] || 'Apartment';
  }

  // Generate mock reviews count based on rating for demo purposes
  private generateMockReviewsCount(rating: number): number {
    if (rating >= 4.5) return Math.floor(Math.random() * 50) + 20; // 20-70 reviews for excellent properties
    if (rating >= 4.0) return Math.floor(Math.random() * 30) + 15; // 15-45 reviews for very good properties  
    if (rating >= 3.5) return Math.floor(Math.random() * 20) + 10; // 10-30 reviews for good properties
    if (rating >= 3.0) return Math.floor(Math.random() * 15) + 5;  // 5-20 reviews for decent properties
    return Math.floor(Math.random() * 10) + 1; // 1-10 reviews for poor properties
  }

  // For backward compatibility - convert PropertyListItem to Property
  private convertToProperty(item: PropertyListItem): Property {
    // Create mock reviews array based on rating
    const reviewsCount = this.generateMockReviewsCount(item.rating);
    const mockReviews: any[] = Array(reviewsCount).fill(null).map((_, index) => ({
      reviewId: index + 1,
      username: `User${index + 1}`,
      propertyName: item.name,
      propertyId: item.id,
      comment: 'Great stay!',
      rating: item.rating,
      reviewDate: new Date().toISOString()
    }));

    return {
      id: item.id,
      name: item.name,
      rating: item.rating,
      hostName: item.hostName,
      address: '', // Not available in list item
      locationName: item.locationName,
      cityName: item.cityName,
      pricePerNight: item.pricePerNight,
      numberOfBedrooms: item.numberOfBedrooms,
      numberOfBathrooms: item.numberOfBathrooms,
      maxAdults: item.maxAdults,
      maxChildren: 0, // Default values for missing fields
      maxInfants: 0,
      maxPets: item.maxPets,
      propertyType: this.getPropertyTypeNumber(item.propertyType),
      description: '',
      images: item.thumbnailImageUrl ? [{
        imageId: 0,
        imageUrl: item.thumbnailImageUrl,
        description: '',
        createDate: '',
        updateDate: ''
      }] : [],
      reviews: mockReviews, // Mock reviews for demo
      amenities: [],
      facilities: [],
      nameUserFavorites: [],
      available: true,
      guestFavorite: item.guestFavorite
    };
  }
  
  // Filter data
  cities: City[] = [];
  locations: Location[] = [];
  allLocations: Location[] = []; // Cache toàn bộ locations để infer city
  
  // Selection state
  selectedCity: string | null = null;
  selectedLocation: string | null = null;
  
  // Pagination
  currentPage: number = 0;
  pageSize: number = 5;
  totalElements: number = 0;
  totalPages: number = 0;
  Math = Math; // Expose Math to template
  
  // UI state
  showAllCities: boolean = false;
  showAllLocations: boolean = false;
  
  // ================ SORT PROPERTIES ================
  currentSort: 'newest' | 'rating' | null = null;
  private destroy$ = new Subject<void>();
  
  propertyTypeLabels: { [key: string]: string } = {
    '0': 'Apartments',
    '1': 'Houses',
    '2': 'Hotels'
  };

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private propertyService: PropertyService,
    private locationService: LocationService,
    private cityService: CityService,
    private userService: UserService,
    private tokenService: TokenService,
    private authStateService: AuthStateService
  ) {}

  // Navigate to enhanced filter page
  goToEnhancedFilter(): void {
    this.router.navigate(['/properties/filter']);
  }

  ngOnInit(): void {
    // Check login status
    this.checkLoginStatus();
    
    // Subscribe to auth state changes
    this.authStateService.loginState$.subscribe((isLoggedIn: boolean) => {
      this.isLoggedIn = isLoggedIn;
      if (isLoggedIn) {
        this.userId = this.tokenService.getUserId();
        this.checkAllFavorites();
      } else {
        this.isFavoriteMap.clear();
      }
    });
    
    // Check if we have type parameter (type-specific list) or query parameters (search)
    this.route.params.subscribe(params => {
      this.propertyType = params['type']; // Will be undefined for /properties route
    });

    // Listen to query parameters for search functionality and pagination
    this.route.queryParams.subscribe(queryParams => {
      const page = queryParams['page'] ? parseInt(queryParams['page']) - 1 : 0; // Convert from 1-based to 0-based
      
      if (queryParams['name']) {
        this.loadSearchResults(queryParams['name'], page);
      } else if (this.propertyType) {
        this.loadInitialData(page); // Load by type
      } else {
        this.loadAllProperties(page); // Load all properties
      }
    });
  }
  
  checkLoginStatus(): void {
    const token = this.tokenService.getToken();
    this.isLoggedIn = !!token && !this.tokenService.isTokenExpired();
    
    if (this.isLoggedIn) {
      this.userId = this.tokenService.getUserId();
    }
  }
  
  checkAllFavorites(): void {
    if (!this.isLoggedIn || this.properties.length === 0) {
      return;
    }
    
    const token = this.tokenService.getToken();
    if (!token) return;
    
    // Check favorite status for each property
    this.properties.forEach(property => {
      this.userService.checkFavorite(this.userId, property.id, token).subscribe({
        next: (response) => {
          this.isFavoriteMap.set(property.id, response.data === true);
        },
        error: (error) => {
          console.error(`❌ Error checking favorite status for property ${property.id}:`, error);
        }
      });
    });
  }
  
  toggleFavorite(event: Event, propertyId: number): void {
    event.preventDefault();
    event.stopPropagation();
    
    // Check if user is logged in
    if (!this.isLoggedIn) {
      alert('Vui lòng đăng nhập để sử dụng tính năng này!');
      this.router.navigate(['/login']);
      return;
    }

    // Prevent multiple clicks
    if (this.isLoadingWishlistMap.get(propertyId)) {
      return;
    }

    this.isLoadingWishlistMap.set(propertyId, true);
    const token = this.tokenService.getToken();
    if (!token) {
      alert('Vui lòng đăng nhập lại!');
      this.isLoadingWishlistMap.set(propertyId, false);
      return;
    }

    const isFavorite = this.isFavoriteMap.get(propertyId) || false;

    if (isFavorite) {
      // Remove from favorites
      console.log('🔵 Removing property from wishlist:', propertyId);
      this.userService.removeFromFavorites(this.userId, propertyId, token).subscribe({
        next: (response) => {
          console.log('✅ Successfully removed from favorites:', response);
          this.isFavoriteMap.set(propertyId, false);
          this.isLoadingWishlistMap.set(propertyId, false);
        },
        error: (error) => {
          console.error('❌ Error removing from favorites:', error);
          alert('Có lỗi xảy ra khi xóa khỏi danh sách yêu thích!');
          this.isLoadingWishlistMap.set(propertyId, false);
        }
      });
    } else {
      // Add to favorites
      console.log('🔵 Adding property to wishlist:', propertyId);
      this.userService.addToFavorites(this.userId, propertyId, token).subscribe({
        next: (response) => {
          console.log('✅ Successfully added to favorites:', response);
          this.isFavoriteMap.set(propertyId, true);
          this.isLoadingWishlistMap.set(propertyId, false);
        },
        error: (error) => {
          console.error('❌ Error adding to favorites:', error);
          alert('Có lỗi xảy ra khi thêm vào danh sách yêu thích!');
          this.isLoadingWishlistMap.set(propertyId, false);
        }
      });
    }
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }



  /**
   * Load dữ liệu ban đầu: cities, locations và properties theo type
   */
  loadInitialData(page: number = 0): void {
    console.log('📍 PropertyList: Loading initial data for type:', this.propertyType);
    this.loading = true;
    this.errorMsg = '';
    
    // Load cities
    this.cityService.getAll().subscribe({
      next: (response: any) => {
        this.cities = response.data || response || [];
        console.log('✅ PropertyList: Loaded cities:', this.cities.length);
      },
      error: (error: any) => {
        console.error('❌ PropertyList: Error loading cities:', error);
        this.errorMsg = 'Không thể tải danh sách thành phố';
      }
    });
    
    // Load all locations (cache để infer city)
    this.locationService.getAll().subscribe({
      next: (response: any) => {
        this.allLocations = response.data || response || [];
        this.locations = [...this.allLocations]; // Clone
        console.log('✅ PropertyList: Loaded locations:', this.locations.length);
      },
      error: (error: any) => {
        console.error('❌ PropertyList: Error loading locations:', error);
        this.errorMsg = 'Không thể tải danh sách địa điểm';
      }
    });
    
    // Load properties by type
    this.reloadDefaultList(page);
  }

  /**
   * Load tất cả properties (không theo type) với pagination
   */
  loadAllProperties(page: number = 0): void {
    console.log('📍 PropertyList: Loading all properties...');
    this.loading = true;
    this.errorMsg = '';
    
    // Load reference data
    this.loadReferenceData();
    
    // Load all properties with pagination
    this.propertyService.getAllPropertiesWithPagination(page, 5).subscribe({
      next: (response) => {
        const pageResponse = response.data;
        const listItems = pageResponse.content || [];
        this.properties = listItems.map(item => this.convertToProperty(item));
        
        // Update pagination info from API response
        this.currentPage = pageResponse.currentPage;
        this.totalElements = pageResponse.totalElements;
        this.totalPages = pageResponse.totalPages;
        
        console.log('✅ PropertyList: Loaded all properties:', this.properties.length, 
                    `(Page ${this.currentPage + 1}/${this.totalPages}, Total: ${this.totalElements})`);
        this.loading = false;
        
        // Check favorites after loading
        this.checkAllFavorites();
      },
      error: (error: any) => {
        console.error('❌ PropertyList: Error loading all properties:', error);
        this.errorMsg = 'Không thể tải danh sách property';
        this.loading = false;
      }
    });
  }

  /**
   * Load search results by name with pagination
   */
  loadSearchResults(searchTerm: string, page: number = 0): void {
    console.log('📍 PropertyList: Searching for:', searchTerm);
    this.loading = true;
    this.errorMsg = '';
    
    // Load reference data
    this.loadReferenceData();
    
    // Search properties by name
    const searchRequest: PropertySearchRequest = {
      name: searchTerm,
      page: page,
      size: 5, // Load more results for search
      sortBy: 'priority',
      sortDirection: SortDirection.DESC
    };

    this.propertyService.searchPropertiesGet(searchRequest).subscribe({
      next: (pageResponse) => {
        const listItems = pageResponse.content || [];
        this.properties = listItems.map(item => this.convertToProperty(item));
        
        // Update pagination info
        this.currentPage = pageResponse.currentPage;
        this.totalElements = pageResponse.totalElements;
        this.totalPages = pageResponse.totalPages;
        
        console.log('✅ PropertyList: Search results:', this.properties.length,
                    `(Page ${this.currentPage + 1}/${this.totalPages}, Total: ${this.totalElements})`);
        this.loading = false;
        
        // Check favorites after loading
        this.checkAllFavorites();
      },
      error: (error: any) => {
        console.error('❌ PropertyList: Error searching properties:', error);
        this.errorMsg = 'Không thể tìm kiếm property';
        this.loading = false;
      }
    });
  }

  /**
   * Reload danh sách properties mặc định theo type với pagination
   */
  reloadDefaultList(page: number = 0): void {
    console.log('📍 PropertyList: Reloading default list for type:', this.propertyType);
    this.loading = true;
    this.errorMsg = '';
    
    this.propertyService.getPropertiesByTypeWithPagination(+this.propertyType, page, 5).subscribe({
      next: (response) => {
        const pageResponse = response.data;
        const listItems = pageResponse.content || [];
        this.properties = listItems.map(item => this.convertToProperty(item));
        
        // Update pagination info
        this.currentPage = pageResponse.currentPage;
        this.totalElements = pageResponse.totalElements;
        this.totalPages = pageResponse.totalPages;
        
        console.log('✅ PropertyList: Loaded properties by type:', this.properties.length,
                    `(Page ${this.currentPage + 1}/${this.totalPages}, Total: ${this.totalElements})`);
        this.loading = false;
        
        // Check favorites after loading
        this.checkAllFavorites();
      },
      error: (error: any) => {
        console.error('❌ PropertyList: Error loading properties:', error);
        this.errorMsg = 'Không thể tải danh sách property';
        this.loading = false;
      }
    });
  }

  /**
   * Handler khi toggle city checkbox
   * Single-select: chỉ chọn 1 city tại 1 thời điểm
   */
  onCityToggle(cityName: string, checked: boolean): void {
    // If clicking on already selected city, deselect it
    if (this.selectedCity === cityName && !checked) {
      this.selectedCity = null;
      this.selectedLocation = null;
      this.locations = [...this.allLocations]; // Restore all locations
      this.showAllCities = false;
      this.showAllLocations = false;
      
      // Reload appropriate list based on context
      this.reloadAppropriateList();
      return;
    }

    if (checked) {
      // Select city
      this.selectedCity = cityName;
      this.selectedLocation = null; // Clear location selection
      
      // Use new unified filter API
      this.filterPropertiesByCriteria();
      
      // Fetch locations of this city
      this.locationService.getByCity(cityName).subscribe({
        next: (response: any) => {
          this.locations = response.data || response || [];
          this.showAllLocations = false; // Reset về hiển thị 5
        },
        error: (error: any) => {
          console.error('Error loading locations by city:', error);
        }
      });
      
    } else {
      // Uncheck city - reset to default and reload appropriate list
      this.selectedCity = null;
      this.selectedLocation = null;
      this.locations = [...this.allLocations]; // Restore all locations
      this.showAllCities = false;
      this.showAllLocations = false;
      
      // Reload appropriate list based on context
      this.reloadAppropriateList();
    }
  }

  /**
   * Handler khi click vào city checkbox
   * Xử lý toggle logic: nếu đã chọn thì bỏ chọn, nếu chưa chọn thì chọn
   */
  onCityClick(cityName: string): void {
    if (this.selectedCity === cityName) {
      // Already selected, so deselect
      this.onCityToggle(cityName, false);
    } else {
      // Not selected, so select
      this.onCityToggle(cityName, true);
    }
  }

  /**
   * Handler khi click vào location checkbox
   * Xử lý toggle logic: nếu đã chọn thì bỏ chọn, nếu chưa chọn thì chọn
   */
  onLocationClick(locationName: string): void {
    if (this.selectedLocation === locationName) {
      // Already selected, so deselect
      this.onLocationToggle(locationName, false);
    } else {
      // Not selected, so select
      this.onLocationToggle(locationName, true);
    }
  }

  /**
   * Handler khi toggle city checkbox
   * Single-select: chỉ chọn 1 city tại 1 thời điểm
   */
  onLocationToggle(locationName: string, checked: boolean): void {
    // If clicking on already selected location, deselect it
    if (this.selectedLocation === locationName && !checked) {
      this.selectedLocation = null;
      this.selectedCity = null;
      
      // Restore all cities and locations
      this.cityService.getAll().subscribe({
        next: (response: any) => {
          this.cities = response.data || response || [];
        }
      });
      
      this.locations = [...this.allLocations];
      this.showAllCities = false;
      this.showAllLocations = false;
      
      // Reload appropriate list based on context
      this.reloadAppropriateList();
      return;
    }

    if (checked) {
      // Select location
      this.selectedLocation = locationName;
      
      // Use new unified filter API
      this.filterPropertiesByCriteria();
      
      // Infer city from location
      const foundLocation = this.allLocations.find(loc => loc.locationName === locationName);
      if (foundLocation) {
        this.selectedCity = foundLocation.cityName;
        
        // Filter cities to only show the inferred city
        const matchedCity = this.cities.find(c => c.cityName === foundLocation.cityName);
        if (matchedCity) {
          this.cities = [matchedCity];
        }
        
        // Keep locations filtered to this city
        this.locations = this.allLocations.filter(loc => loc.cityName === foundLocation.cityName);
        this.showAllCities = false;
        this.showAllLocations = false;
      }
      
    } else {
      // Uncheck location - reset to default and reload appropriate list
      this.selectedLocation = null;
      this.selectedCity = null;
      
      // Restore all cities and locations
      this.cityService.getAll().subscribe({
        next: (response: any) => {
          this.cities = response.data || response || [];
        }
      });
      
      this.locations = [...this.allLocations];
      this.showAllCities = false;
      this.showAllLocations = false;
      
      // Reload appropriate list based on context
      this.reloadAppropriateList();
    }
  }

  /**
   * Filter properties based on current criteria using unified API with pagination
   */
  filterPropertiesByCriteria(page: number = 0): void {
    this.loading = true;
    this.errorMsg = '';

    const searchRequest: PropertySearchRequest = {
      type: this.propertyType ? +this.propertyType : undefined,
      city: this.selectedCity || undefined,
      location: this.selectedLocation || undefined,
      page: page,
      size: 5,
      sortBy: 'priority',
      sortDirection: SortDirection.DESC
    };

    this.propertyService.searchPropertiesGet(searchRequest).subscribe({
      next: (pageResponse) => {
        const listItems = pageResponse.content || [];
        this.properties = listItems.map(item => this.convertToProperty(item));
        
        // Update pagination info
        this.currentPage = pageResponse.currentPage;
        this.totalElements = pageResponse.totalElements;
        this.totalPages = pageResponse.totalPages;
        
        console.log('✅ PropertyList: Loaded properties:', this.properties.length, 
                    `(Page ${this.currentPage + 1}/${this.totalPages}, Total: ${this.totalElements})`);
        this.loading = false;
        
        // Check favorites after loading
        this.checkAllFavorites();
      },
      error: (error: any) => {
        console.error('Error filtering properties:', error);
        this.errorMsg = 'Không thể tải danh sách property';
        this.loading = false;
      }
    });
  }

  /**
   * Reload appropriate list based on current context
   */
  reloadAppropriateList(): void {
    // Check if we have search parameters
    const nameParam = this.route.snapshot.queryParams['name'];
    const page = this.route.snapshot.queryParams['page'] ? parseInt(this.route.snapshot.queryParams['page']) - 1 : 0;
    
    if (nameParam) {
      this.loadSearchResults(nameParam, page);
    } else if (this.propertyType) {
      this.reloadDefaultList(page); // Load by type
    } else {
      this.loadAllProperties(page); // Load all properties
    }
  }

  getPageTitle(): string {
    // Check if we have query params for search
    const nameParam = this.route.snapshot.queryParams['name'];
    if (nameParam) {
      return `Tìm kiếm: "${nameParam}"`;
    }
    
    // Check if we have property type
    if (this.propertyType) {
      return this.propertyTypeLabels[this.propertyType] || 'Properties';
    }
    
    // Default for all properties
    return 'All Properties';
  }

  getPageSubtitle(): string {
    // Check if we have query params for search
    const nameParam = this.route.snapshot.queryParams['name'];
    if (nameParam) {
      return 'Kết quả tìm kiếm theo tên property';
    }
    
    // Check if we have property type
    if (this.propertyType) {
      const subtitles: { [key: string]: string } = {
        '0': 'Khám phá các căn hộ được yêu thích nhất',
        '1': 'Tìm ngôi nhà lý tưởng cho gia đình bạn',
        '2': 'Khách sạn cao cấp với dịch vụ tuyệt vời'
      };
      return subtitles[this.propertyType] || 'Khám phá các địa điểm tuyệt vời';
    }
    
    // Default for all properties
    return 'Khám phá tất cả các địa điểm tuyệt vời';
  }

  getPropertyImage(property: Property): string {
    if (property.images && property.images.length > 0) {
      const imageUrl = property.images[0].imageUrl;
      if (imageUrl.startsWith('http')) {
        return imageUrl;
      }
      return `http://localhost:8080${imageUrl}`;
    }
    return '/assets/img/placeholder.svg';
  }

  getPropertyListItemImage(property: PropertyListItem): string {
    if (property.thumbnailImageUrl) {
      if (property.thumbnailImageUrl.startsWith('http')) {
        return property.thumbnailImageUrl;
      }
      return `http://localhost:8080${property.thumbnailImageUrl}`;
    }
    return '/assets/img/placeholder.svg';
  }

  // Round rating to nearest 0.5
  roundRating(rating: number): number {
    return Math.round(rating * 2) / 2;
  }

  getRatingLabel(rating: number): string {
    const roundedRating = this.roundRating(rating);
    if (roundedRating >= 4.5) return 'Superb';
    if (roundedRating >= 4.0) return 'Very Good';
    if (roundedRating >= 3.5) return 'Good';
    if (roundedRating >= 3.0) return 'Pleasant';
    if (roundedRating >= 2.0) return 'Fair';
    return 'Poor';
  }

  trackByPropertyId(index: number, property: Property): number {
    return property.id;
  }

  trackByCity(index: number, city: City): number {
    return city.id;
  }

  trackByLocation(index: number, location: Location): number {
    return location.id;
  }

  trackByPropertyListItem(index: number, property: PropertyListItem): number {
    return property.id;
  }

  // ================ NEW ENHANCED SEARCH METHODS ================

  /**
   * Load reference data for enhanced search dropdowns
   */
  private loadReferenceData(): void {
    // Load cities
    this.cityService.getAll().subscribe({
      next: (response: any) => {
        this.cities = response.data || response || [];
      },
      error: (error: any) => {
        console.error('Error loading cities:', error);
      }
    });

    // Load all locations
    this.locationService.getAll().subscribe({
      next: (response: any) => {
        this.allLocations = response.data || response || [];
        this.locations = [...this.allLocations];
      },
      error: (error: any) => {
        console.error('Error loading locations:', error);
      }
    });
  }

  // ================ SORT METHODS ================
  
  /**
   * Sort by newest (createdDate)
   */
  sortByNewest(): void {
    this.currentSort = 'newest';
    this.properties.sort((a, b) => {
      // Since we don't have createdDate in Property model, 
      // we'll sort by id descending as a proxy for newest
      return b.id - a.id;
    });
  }

  /**
   * Sort by rating (highest first)
   */
  sortByRating(): void {
    this.currentSort = 'rating';
    this.properties.sort((a, b) => {
      return (b.rating || 0) - (a.rating || 0);
    });
  }

  getDisplayProperties(): Property[] {
    return this.properties;
  }

  // ================ PAGINATION FUNCTIONS ================
  
  /**
   * Go to specific page
   */
  goToPage(page: number): void {
    if (page >= 0 && page < this.totalPages && page !== this.currentPage) {
      // Update URL with page parameter to maintain browser history
      this.updateUrlWithPage(page);
      
      // Check current context and load appropriate page
      const nameParam = this.route.snapshot.queryParams['name'];
      if (nameParam) {
        this.loadSearchResults(nameParam, page);
      } else if (this.selectedCity || this.selectedLocation) {
        this.filterPropertiesByCriteria(page);
      } else if (this.propertyType) {
        this.reloadDefaultList(page);
      } else {
        this.loadAllProperties(page);
      }
      
      // Scroll to top
      window.scrollTo({ top: 0, behavior: 'smooth' });
    }
  }

  /**
   * Go to previous page
   */
  previousPage(): void {
    if (this.currentPage > 0) {
      this.goToPage(this.currentPage - 1);
    }
  }

  /**
   * Go to next page
   */
  nextPage(): void {
    if (this.currentPage < this.totalPages - 1) {
      this.goToPage(this.currentPage + 1);
    }
  }

  /**
   * Get page numbers to display in pagination
   */
  getPageNumbers(): number[] {
    const pages: number[] = [];
    const maxPagesToShow = 5;
    
    let startPage = Math.max(0, this.currentPage - Math.floor(maxPagesToShow / 2));
    let endPage = Math.min(this.totalPages - 1, startPage + maxPagesToShow - 1);
    
    // Adjust start if we're near the end
    if (endPage - startPage < maxPagesToShow - 1) {
      startPage = Math.max(0, endPage - maxPagesToShow + 1);
    }
    
    for (let i = startPage; i <= endPage; i++) {
      pages.push(i);
    }
    
    return pages;
  }

  /**
   * Update URL with page parameter to maintain browser history
   */
  private updateUrlWithPage(page: number): void {
    const queryParams = { ...this.route.snapshot.queryParams };
    
    if (page > 0) {
      queryParams['page'] = (page + 1).toString(); // Convert to 1-based for URL
    } else {
      delete queryParams['page']; // Remove page param if it's the first page
    }
    
    this.router.navigate([], {
      relativeTo: this.route,
      queryParams: queryParams,
      queryParamsHandling: 'merge',
      replaceUrl: false // This creates new history entry
    });
  }
}
