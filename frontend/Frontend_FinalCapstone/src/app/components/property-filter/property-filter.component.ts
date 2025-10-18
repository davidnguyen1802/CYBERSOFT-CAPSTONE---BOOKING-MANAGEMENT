import { Component, OnInit, OnDestroy } from '@angular/core';
import { Router } from '@angular/router';
import { Subject } from 'rxjs';
import { Property, PropertyListItem, PropertySearchRequest, SortDirection } from '../../models/property';
import { PropertyService } from '../../services/property.service';
import { LocationService } from '../../services/location.service';
import { CityService } from '../../services/city.service';
import { Location } from '../../models/location';
import { City } from '../../models/city';

@Component({
  selector: 'app-property-filter',
  templateUrl: './property-filter.component.html',
  styleUrls: ['./property-filter.component.scss']
})
export class PropertyFilterComponent implements OnInit, OnDestroy {
  // Properties data
  properties: Property[] = [];
  loading: boolean = false;
  errorMsg: string = '';

  // Pagination
  currentPage: number = 0;
  pageSize: number = 9;
  totalElements: number = 0;
  totalPages: number = 0;
  Math = Math; // Expose Math to template
  
  // Filter data
  cities: City[] = [];
  locations: Location[] = [];
  allLocations: Location[] = [];

  // Filter state
  searchKeyword: string = '';
  selectedPropertyType: number | null = null;
  selectedCity: string | null = null;
  selectedLocation: string | null = null;
  minPrice: number | null = null;
  maxPrice: number | null = null;
  bedrooms: number | null = null;
  bathrooms: number | null = null;
  maxAdults: number | null = null;
  maxChildren: number | null = null;
  maxInfants: number | null = null;
  maxPets: number | null = null;

  // UI state
  showAllCities: boolean = false;
  showAllLocations: boolean = false;
  currentSort: 'newest' | 'rating' | 'price-asc' | 'price-desc' | null = null;

  private destroy$ = new Subject<void>();

  propertyTypes = [
    { value: 0, label: 'Apartment' },
    { value: 1, label: 'House' },
    { value: 2, label: 'Hotel' }
  ];

  constructor(
    private router: Router,
    private propertyService: PropertyService,
    private locationService: LocationService,
    private cityService: CityService
  ) {}

  ngOnInit(): void {
    this.loadReferenceData();
    this.loadAllProperties();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  /**
   * Load reference data (cities, locations)
   */
  loadReferenceData(): void {
    console.log('📍 PropertyFilter: Loading reference data...');
    // Load cities
    this.cityService.getAll().subscribe({
      next: (response: any) => {
        this.cities = response.data || response || [];
        console.log('✅ PropertyFilter: Loaded cities:', this.cities.length);
      },
      error: (error: any) => {
        console.error('❌ PropertyFilter: Error loading cities:', error);
      }
    });

    // Load all locations
    this.locationService.getAll().subscribe({
      next: (response: any) => {
        this.allLocations = response.data || response || [];
        this.locations = [...this.allLocations];
        console.log('✅ PropertyFilter: Loaded locations:', this.locations.length);
      },
      error: (error: any) => {
        console.error('❌ PropertyFilter: Error loading locations:', error);
      }
    });
  }

  /**
   * Load all properties initially
   */
  loadAllProperties(page: number = 0): void {
    console.log(`📍 PropertyFilter: Loading all properties (page ${page})...`);
    this.loading = true;
    this.errorMsg = '';

    this.propertyService.getAllPropertiesWithPagination(page, this.pageSize).subscribe({
      next: (response) => {
        const pageData = response.data;
        const listItems = pageData.content || [];
        this.properties = listItems.map(item => this.convertToProperty(item));
        
        // Update pagination info
        this.currentPage = pageData.currentPage;
        this.totalElements = pageData.totalElements;
        this.totalPages = pageData.totalPages;
        
        console.log('✅ PropertyFilter: Loaded all properties:', this.properties.length,
                    `(Page ${this.currentPage + 1}/${this.totalPages}, Total: ${this.totalElements})`);
        this.loading = false;
      },
      error: (error: any) => {
        console.error('❌ PropertyFilter: Error loading properties:', error);
        this.errorMsg = 'Không thể tải danh sách property';
        this.loading = false;
      }
    });
  }

  /**
   * Apply filters
   */
  applyFilters(page: number = 0): void {
    this.loading = true;
    this.errorMsg = '';

    const searchRequest: PropertySearchRequest = {
      name: this.searchKeyword || undefined,
      type: this.selectedPropertyType !== null ? this.selectedPropertyType : undefined,
      city: this.selectedCity || undefined,
      location: this.selectedLocation || undefined,
      minPrice: this.minPrice || undefined,
      maxPrice: this.maxPrice || undefined,
      bedrooms: this.bedrooms || undefined,
      bathrooms: this.bathrooms || undefined,
      maxAdults: this.maxAdults || undefined,
      maxChildren: this.maxChildren || undefined,
      maxInfants: this.maxInfants || undefined,
      maxPets: this.maxPets || undefined,
      page: page,
      size: this.pageSize,
      sortBy: 'priority',
      sortDirection: SortDirection.DESC
    };

    console.log('📍 PropertyFilter: Applying filters...', searchRequest);
    this.propertyService.searchPropertiesGet(searchRequest).subscribe({
      next: (pageResponse) => {
        const listItems = pageResponse.content || [];
        this.properties = listItems.map(item => this.convertToProperty(item));
        
        // Update pagination info
        this.currentPage = pageResponse.currentPage;
        this.totalElements = pageResponse.totalElements;
        this.totalPages = pageResponse.totalPages;
        
        console.log('✅ PropertyFilter: Loaded properties:', this.properties.length, 
                    `(Page ${this.currentPage + 1}/${this.totalPages}, Total: ${this.totalElements})`);
        this.loading = false;
      },
      error: (error: any) => {
        console.error('❌ PropertyFilter: Error filtering properties:', error);
        this.errorMsg = 'Không thể lọc property';
        this.loading = false;
      }
    });
  }

  /**
   * Clear all filters
   */
  clearFilters(): void {
    this.searchKeyword = '';
    this.selectedPropertyType = null;
    this.selectedCity = null;
    this.selectedLocation = null;
    this.minPrice = null;
    this.maxPrice = null;
    this.bedrooms = null;
    this.bathrooms = null;
    this.maxAdults = null;
    this.maxChildren = null;
    this.maxInfants = null;
    this.maxPets = null;
    this.loadAllProperties();
  }

  /**
   * Handle city change
   */
  onCityChange(): void {
    if (this.selectedCity) {
      // Filter locations by selected city
      this.locationService.getByCity(this.selectedCity).subscribe({
        next: (response: any) => {
          this.locations = response.data || response || [];
        },
        error: (error: any) => {
          console.error('Error loading locations by city:', error);
        }
      });
    } else {
      // Reset to all locations
      this.locations = [...this.allLocations];
    }
    this.selectedLocation = null;
  }

  /**
   * Sort methods
   */
  sortByNewest(): void {
    this.currentSort = 'newest';
    this.properties.sort((a, b) => b.id - a.id);
  }

  sortByRating(): void {
    this.currentSort = 'rating';
    this.properties.sort((a, b) => (b.rating || 0) - (a.rating || 0));
  }

  sortByPriceAsc(): void {
    this.currentSort = 'price-asc';
    this.properties.sort((a, b) => a.pricePerNight - b.pricePerNight);
  }

  sortByPriceDesc(): void {
    this.currentSort = 'price-desc';
    this.properties.sort((a, b) => b.pricePerNight - a.pricePerNight);
  }

  /**
   * Convert PropertyListItem to Property
   */
  private convertToProperty(item: PropertyListItem): Property {
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
      address: '',
      locationName: item.locationName,
      cityName: item.cityName,
      pricePerNight: item.pricePerNight,
      numberOfBedrooms: item.numberOfBedrooms,
      numberOfBathrooms: item.numberOfBathrooms,
      maxAdults: item.maxAdults,
      maxChildren: 0,
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
      reviews: mockReviews,
      amenities: [],
      facilities: [],
      nameUserFavorites: [],
      available: true,
      guestFavorite: item.guestFavorite
    };
  }

  private getPropertyTypeNumber(propertyType: any): number {
    if (typeof propertyType === 'number') return propertyType;
    switch (propertyType) {
      case 'APARTMENT': return 0;
      case 'HOUSE': return 1;
      case 'VILLA': return 2;
      default: return 0;
    }
  }

  private generateMockReviewsCount(rating: number): number {
    if (rating >= 4.5) return Math.floor(Math.random() * 50) + 20;
    if (rating >= 4.0) return Math.floor(Math.random() * 30) + 15;
    if (rating >= 3.5) return Math.floor(Math.random() * 20) + 10;
    if (rating >= 3.0) return Math.floor(Math.random() * 15) + 5;
    return Math.floor(Math.random() * 10) + 1;
  }

  getPropertyImage(property: Property): string {
    if (property.images && property.images.length > 0) {
      return `http://localhost:8080${property.images[0].imageUrl}`;
    }
    return '/assets/img/default-property.jpg';
  }

  getRatingText(rating: number): string {
    const roundedRating = this.roundRating(rating);
    if (roundedRating >= 4.5) return 'Superb';
    if (roundedRating >= 4.0) return 'Very Good';
    if (roundedRating >= 3.5) return 'Good';
    if (roundedRating >= 3.0) return 'Pleasant';
    if (roundedRating >= 2.0) return 'Fair';
    return 'Poor';
  }

  roundRating(rating: number): number {
    return Math.round(rating * 10) / 10;
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

  // ================ PAGINATION METHODS ================
  
  /**
   * Go to specific page
   */
  goToPage(page: number): void {
    if (page >= 0 && page < this.totalPages && page !== this.currentPage) {
      this.applyFilters(page);
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
}
